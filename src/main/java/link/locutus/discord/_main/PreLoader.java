package link.locutus.discord._main;

import link.locutus.discord.Locutus;
import link.locutus.discord.Logg;
import link.locutus.discord.api.ApiKeyPool;
import link.locutus.discord.api.endpoints.DnsApi;
import link.locutus.discord.commands.manager.CommandManager;
import link.locutus.discord.commands.manager.v2.impl.SlashCommandManager;
import link.locutus.discord.commands.manager.v2.impl.pw.CommandManager2;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.*;
import link.locutus.discord.pnw.RegisteredUser;
import link.locutus.discord.util.FileUtil;
import link.locutus.discord.util.MathMan;
import link.locutus.discord.util.offshore.Auth;
import link.locutus.discord.util.scheduler.ThrowingSupplier;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PreLoader implements ILoader {
    private final ThreadPoolExecutor executor;
    private final ScheduledThreadPoolExecutor scheduler;
    private final Semaphore semaphore;
    private final Locutus locutus;
    // futures
    private final Map<String, Future<?>> resolvers;
    private final Map<String, Thread> resolverThreads;
    private final AtomicInteger numTasks = new AtomicInteger(0);
    private final boolean awaitBackup;

    private volatile FinalizedLoader finalized;
    // fields
    private final Future<SlashCommandManager> slashCommandManager;
    private final Future<JDA> jda;

    private final Future<DiscordDB> discordDB;
    private final Future<NationDB> nationDB;
    private final Future<WarDB> warDb;
    private final Future<BankDB> bankDb;
    private final Future<CommandManager> commandManager;
    private final Future<Supplier<Integer>> getNationId;
    private final Future<Supplier<Long>> adminUserId;
    private final Future<DnsApi> apiV3;
    private final Future<Boolean> backup;

    public PreLoader(Locutus locutus, ThreadPoolExecutor executor, ScheduledThreadPoolExecutor scheduler) {
        this.executor = executor;
        this.scheduler = scheduler;
        this.semaphore = new Semaphore(0);

        this.resolvers = new ConcurrentHashMap<>();
        this.resolverThreads = new ConcurrentHashMap<>();
        this.locutus = locutus;

        this.slashCommandManager = add("Slash Command Manager", new ThrowingSupplier<SlashCommandManager>() {
            @Override
            public SlashCommandManager getThrows() throws Exception {
                return new SlashCommandManager(Settings.INSTANCE.ENABLED_COMPONENTS.REGISTER_ADMIN_SLASH_COMMANDS, () -> Locutus.cmd().getV2());
            }
        }, false);
        this.jda = add("Discord Hook", this::buildJDA, false);
        this.awaitBackup = !Settings.INSTANCE.BACKUP.SCRIPT.isEmpty();
        if (awaitBackup) {
            backup = add("Backup", () -> {
                Backup.backup();
                return true;
            }, false);
        } else {
            backup = CompletableFuture.completedFuture(false);
        }

        this.discordDB = add("Discord Database", () -> new DiscordDB());
        this.nationDB = add("Nation Database", () -> new NationDB().load());

        this.warDb = add("War Database", () -> new WarDB().load());
        this.bankDb = add("Bank Database", () -> new BankDB());
        this.commandManager = add("Command Handler", () -> new CommandManager(scheduler));
        if (Settings.INSTANCE.NATION_ID <= 0) {
            throw new IllegalArgumentException("Please set `nation-id` in the config.yaml");
        } else {
            this.getNationId = CompletableFuture.completedFuture(() -> Settings.INSTANCE.NATION_ID);
        }
        if (Settings.INSTANCE.ADMIN_USER_ID <= 0) {
            this.adminUserId = add("Discord Admin User ID", new ThrowingSupplier<Supplier<Long>>() {
                @Override
                public Supplier<Long> getThrows() throws Exception {
                    int nationId = getNationId();
                    RegisteredUser adminPnwUser = getDiscordDB().getUserFromNationId(nationId);
                    if (adminPnwUser != null) {
                        Settings.INSTANCE.ADMIN_USER_ID = adminPnwUser.getDiscordId();
                    }
                    return () -> Settings.INSTANCE.ADMIN_USER_ID;
                }
            });
        } else {
            this.adminUserId = CompletableFuture.completedFuture(() -> Settings.INSTANCE.ADMIN_USER_ID);
        }
        this.apiV3 = add("PW-API V3", () -> {
            ApiKeyPool.SimpleBuilder builder = ApiKeyPool.builder();
            Settings.INSTANCE.API_KEY_POOL.forEach(builder::addKey);
            ApiKeyPool v3Pool = builder.build();
            return new DnsApi(v3Pool);
        });

        add("Register Discord Commands", () -> {
            CommandManager cmdMan = getCommandManager();
            CommandManager2 v2 = cmdMan.getV2();
            v2.registerDefaults();
            return null;
        });
        setupMonitor();
    }

    private void setupMonitor() {
        Logg.text("Initializing Startup Monitor");
        scheduler.schedule(() -> {
            List<String> deadlocks = detectDeadlock(resolverThreads.values().stream().map(Thread::getId).collect(Collectors.toSet()));
            if (!deadlocks.isEmpty()) {
                Logg.text("\n[Startup Monitor] " + deadlocks.size() + " Deadlocks detected\n" + deadlocks + "\n");
            } else {
                String stacktrace = printStacktrace();
                if (!stacktrace.isEmpty()) {
                    Logg.text("\n[Startup Monitor] Initializing the bot taking longer than expected, but is not hung (120s):\n" + stacktrace + "\n");
                } else {
                    Logg.text("\n[Startup Monitor] Detected no hung or incomplete startup threads (they either completed successfully or completed with errors)");
                }
            }
        }, 120, TimeUnit.SECONDS);
    }

    public static List<String> detectDeadlock(Set<Long> threadIds) {
        List<String> results = new ArrayList<>();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreadIds = threadMXBean.findDeadlockedThreads();
        if (deadlockedThreadIds != null) {
            ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(deadlockedThreadIds, true, true);
            List<ThreadInfo> filteredThreadInfos = filterThreadInfos(threadInfos, threadIds);
            for (ThreadInfo threadInfo : filteredThreadInfos) {
                StringBuilder info = new StringBuilder("Deadlocked thread: " + threadInfo.getThreadName() + ": " + threadInfo.getThreadState() + "\n");
                info.append("Blocked: ").append(threadInfo.getBlockedTime()).append("/").append(threadInfo.getBlockedCount()).append("\n");
                info.append("Waited: ").append(threadInfo.getWaitedTime()).append("/").append(threadInfo.getWaitedCount()).append("\n");
                info.append("Lock: ").append(threadInfo.getLockName()).append("/").append(threadInfo.getLockOwnerId()).append("/").append(threadInfo.getLockOwnerName()).append("\n");
                for (StackTraceElement ste : threadInfo.getStackTrace()) {
                    info.append("\t").append(ste).append("\n");
                }
                results.add(info.toString());
            }
        }
        return results;
    }

    private static List<ThreadInfo> filterThreadInfos(ThreadInfo[] threadInfos, Set<Long> threadIds) {
        return List.of(threadInfos).stream()
                .filter(threadInfo -> threadIds.contains(threadInfo.getThreadId()))
                .collect(Collectors.toList());
    }

    @Override
    public int getNationId() {
        return FileUtil.get(getNationId).get();
    }
    @Override
    public long getAdminUserId() {
        return FileUtil.get(adminUserId).get();
    }

    @Override
    public ILoader resolveFully(long timeout) {
        Set<Map.Entry<String, Future<?>>> tmp = resolvers.entrySet();
        if (finalized != null) return finalized;
        synchronized (this) {
            if (finalized != null) {
                return  finalized;
            }
            for (Map.Entry<String, Future<?>> resolver : tmp) {
                String taskName = resolver.getKey();
                Future<?> future = resolver.getValue();
                try {
                    future.get(timeout, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    Logg.text("Failed to resolve `TASK:" + taskName + "`: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }
            resolvers.clear();
            this.finalized = new FinalizedLoader(this);
            locutus.setLoader(finalized);
            return finalized;
        }
    }

    private <T> Future<T> add(String taskName, ThrowingSupplier<T> supplier) {
        return add(taskName, supplier, true);
    }

    private <T> Future<T> add(String taskName, ThrowingSupplier<T> supplier, boolean wait) {
        if (resolvers.containsKey(taskName)) {
            throw new IllegalArgumentException("Duplicate task: " + taskName);
        }
        Future<T> future = executor.submit(() -> {
            try {
                Thread thread = Thread.currentThread();
                thread.setName("Load-" + taskName);
                resolverThreads.put(taskName, thread);
                numTasks.incrementAndGet();

                if (wait) semaphore.acquire();
                Logg.text("Loading `" + taskName + "`");
                long start = System.currentTimeMillis();
                T result = supplier.get();
                long end = System.currentTimeMillis();
                if (end - start > 15 || true) {
                    int completed = numTasks.get() - resolverThreads.size() + 1;
                    Logg.text("Completed " + completed + "/" + numTasks + ": `" + taskName + "` in " + MathMan.format((end - start) / 1000d) + "s");
                }
                return result;
            } catch (Throwable e) {
                e.printStackTrace();
                Logg.text("Failed to load `TASK:" + taskName + "`: " + e.getMessage());
                throw e;
            } finally {
                resolverThreads.remove(taskName);
            }
        });
        resolvers.put(taskName, future);
        return future;
    }

    @Override
    public void initialize() {
        if (awaitBackup) {
            FileUtil.get(backup);
        }
        semaphore.release(Integer.MAX_VALUE);
    }

    @Override
    public String printStacktrace() {
        if (resolverThreads.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Thread> entry : resolverThreads.entrySet()) {
            builder.append(printStacktrace(entry.getValue()));
        }
        return builder.toString();
    }

    private String printStacktrace(Thread thread) {
        StringBuilder builder = new StringBuilder();
        builder.append("Thread ").append(thread.getName()).append("/").append(thread.getState()).append("\n");
        for (StackTraceElement element : thread.getStackTrace()) {
            builder.append("\tat ").append(element).append("\n");
        }
        return builder.toString();
    }

    private JDA buildJDA() throws ExecutionException, InterruptedException {
        JDABuilder builder = JDABuilder.createLight(Settings.INSTANCE.BOT_TOKEN, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES);
        if (Settings.INSTANCE.ENABLED_COMPONENTS.SLASH_COMMANDS) {
            SlashCommandManager slash = getSlashCommandManager();
            if (slash != null) {
                builder.addEventListeners(slash);
            }
        }
        if (Settings.INSTANCE.ENABLED_COMPONENTS.SLASH_COMMANDS) {
            builder.addEventListeners(locutus);
        }
        builder
                .setChunkingFilter(ChunkingFilter.NONE)
                .setBulkDeleteSplittingEnabled(false)
                .setCompression(Compression.ZLIB)
                .setLargeThreshold(250)
                .setMemberCachePolicy(MemberCachePolicy.ALL);
        if (Settings.INSTANCE.DISCORD.INTENTS.GUILD_MEMBERS) {
            builder.enableIntents(GatewayIntent.GUILD_MEMBERS);
        }
        if (Settings.INSTANCE.DISCORD.INTENTS.MESSAGE_CONTENT) {
            builder.enableIntents(GatewayIntent.MESSAGE_CONTENT);
        }
        if (Settings.INSTANCE.DISCORD.INTENTS.GUILD_PRESENCES) {
            builder.enableIntents(GatewayIntent.GUILD_PRESENCES);
        }
        if (Settings.INSTANCE.DISCORD.INTENTS.GUILD_MESSAGES) {
            builder.enableIntents(GatewayIntent.GUILD_MESSAGES);
        }
        if (Settings.INSTANCE.DISCORD.INTENTS.GUILD_MESSAGE_REACTIONS) {
            builder.enableIntents(GatewayIntent.GUILD_MESSAGE_REACTIONS);
        }
        if (Settings.INSTANCE.DISCORD.INTENTS.DIRECT_MESSAGES) {
            builder.enableIntents(GatewayIntent.DIRECT_MESSAGES);
        }
        if (Settings.INSTANCE.DISCORD.INTENTS.EMOJI) {
            builder.enableIntents(GatewayIntent.GUILD_EMOJIS_AND_STICKERS);
        }
        if (Settings.INSTANCE.DISCORD.CACHE.MEMBER_OVERRIDES) {
            builder.enableCache(CacheFlag.MEMBER_OVERRIDES);
        }
        if (Settings.INSTANCE.DISCORD.CACHE.ONLINE_STATUS) {
            builder.enableCache(CacheFlag.ONLINE_STATUS);
        }
        if (Settings.INSTANCE.DISCORD.CACHE.EMOTE) {
            builder.enableCache(CacheFlag.EMOJI);
        }
        return builder.build();
    }

    @Override
    public SlashCommandManager getSlashCommandManager() {
        return FileUtil.get(slashCommandManager);
    }

    @Override
    public JDA getJda() {
        return FileUtil.get(jda);
    }

    @Override
    public DiscordDB getDiscordDB() {
        return FileUtil.get(discordDB);
    }

    @Override
    public NationDB getNationDB() {
        return FileUtil.get(nationDB);
    }

    @Override
    public WarDB getWarDB() {
        return FileUtil.get(warDb);
    }

    @Override
    public BankDB getBankDB() {
        return FileUtil.get(bankDb);
    }

    @Override
    public CommandManager getCommandManager() {
        return FileUtil.get(commandManager);
    }

    @Override
    public DnsApi getApiV3() {
        return FileUtil.get(apiV3);
    }
}

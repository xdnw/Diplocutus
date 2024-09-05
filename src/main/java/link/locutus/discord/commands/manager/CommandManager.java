package link.locutus.discord.commands.manager;

import it.unimi.dsi.fastutil.chars.CharOpenHashSet;
import link.locutus.discord.Locutus;
import link.locutus.discord.Logg;
import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.commands.WarCategory;
import link.locutus.discord.commands.manager.v2.command.CommandRef;
import link.locutus.discord.commands.manager.v2.command.IMessageIO;
import link.locutus.discord.commands.manager.v2.impl.discord.DiscordChannelIO;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.commands.manager.v2.impl.pw.filter.NationPlaceholders;
import link.locutus.discord.commands.manager.v2.impl.pw.CommandManager2;
import link.locutus.discord.config.Messages;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.DiscordDB;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.db.entities.DiscordMeta;
import link.locutus.discord.db.entities.NationMeta;
import link.locutus.discord.db.guild.GuildKey;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.*;
import link.locutus.discord.util.discord.DiscordUtil;
import link.locutus.discord.util.scheduler.CaughtRunnable;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.apache.commons.lang3.StringUtils;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class CommandManager {
    private final char prefix1;
    private final CommandManager2 modernized;
    private final ScheduledThreadPoolExecutor executor;

    public CommandManager(ScheduledThreadPoolExecutor executor) {
        this.executor = executor;
        this.prefix1 = Settings.commandPrefix().charAt(0);
        modernized = new CommandManager2();
    }

    public Set<Character> getAllPrefixes() {
        return Set.of(prefix1);
    }

    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    public CommandManager2 getV2() {
        return modernized;
    }

    public boolean run(Guild guild, IMessageIO channel, final User msgUser, String command, final boolean async, final boolean noPermMsg) {
        if (msgUser.isSystem() || msgUser.isBot()) {
            return false;
        }

        String content = DiscordUtil.trimContent(command.trim());
        if (content.length() == 0) {
            return false;
        }

        boolean returnNotFound = (!(channel instanceof DiscordChannelIO)) || command.contains(Settings.INSTANCE.APPLICATION_ID + "");

        if (content.equalsIgnoreCase(Settings.commandPrefix() + "threads")) {
            threadDump();
            return true;
        }

        boolean jsonCommand = (content.startsWith("{") && content.endsWith("}"));
        char char0 = content.charAt(0);
        if (char0 != (prefix1) && !jsonCommand) {
            handleWarRoomSync(guild, msgUser, channel, content);
            return false;
        }

        // Channel blacklisting / whitelisting
        if (char0 == prefix1 || jsonCommand) {
            try {
                modernized.run(guild, channel, msgUser, content, async, returnNotFound);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    private void handleWarRoomSync(Guild guild, User msgUser, IMessageIO io, String command) {
        if (guild == null) return;
        GuildChannel channel = guild.getGuildChannelById(io.getIdLong());
        if (!(channel instanceof ICategorizableChannel GuildMessageChannel)) return;
        if (!(channel instanceof MessageChannel)) return;

        Category category = GuildMessageChannel.getParentCategory();
        if (category == null) return;
        if (!category.getName().startsWith("warcat")) return;
        GuildDB db = Locutus.imp().getGuildDB(guild);
        if (db == null) return;
        if (!db.isWhitelisted() && db.getOrNull(GuildKey.ENABLE_WAR_ROOMS) != Boolean.TRUE) return;

        if (!db.hasAlliance()) return;

        WarCategory.WarRoom room = WarCategory.getGlobalWarRoom((MessageChannel) channel);
        if (room == null || room.target == null) return;

        Set<WarCategory.WarRoom> rooms = WarCategory.getGlobalWarRooms(room.target);
        if (rooms == null) return;
        ByteBuffer optOut = DiscordMeta.OPT_OUT.get(msgUser.getIdLong());
        if (optOut != null && optOut.get() != 0) return;

        for (WarCategory.WarRoom other : rooms) {
            if (other == room || other.channel == null) continue;

            String userPrefix = DiscordUtil.getFullUsername(msgUser);
            DBNation authorNation = DiscordUtil.getNation(msgUser);
            if (authorNation != null) {
                userPrefix = guild.getName() + "|" + authorNation.getNation() + "/`" + userPrefix + "`";
            }
            String msg = userPrefix + ": " + DiscordUtil.trimContent(command);
            msg = msg.replaceAll("@everyone", "@ everyone");
            msg = msg.replaceAll("@here", "@ here");
            msg = msg.replaceAll("<@&", "<@ &");
            RateLimitUtil.queueWhenFree(other.channel.sendMessage(msg));
        }
    }

    private void threadDump() {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (Thread thread : threadSet) {
            System.err.println(Arrays.toString(thread.getStackTrace()));
        }
        System.err.print("\n\nQueue: " + executor.getQueue().size() + " | Active: " + executor.getActiveCount() + " | task count: " + executor.getTaskCount());
        executor.submit(() -> System.err.println("- COMMAND EXECUTOR RAN SUCCESSFULLY!!!"));
    }

    public String getPrefix() {
        return prefix1 + "";
    }
}

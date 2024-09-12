package link.locutus.discord.db;

import com.google.common.eventbus.AsyncEventBus;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import link.locutus.discord.Locutus;
import link.locutus.discord.Logg;
import link.locutus.discord.api.ApiKeyPool;
import link.locutus.discord.api.generated.BankType;
import link.locutus.discord.api.generated.TreatyType;
import link.locutus.discord.api.types.Building;
import link.locutus.discord.api.types.Rank;
import link.locutus.discord.api.types.tx.BankTransfer;
import link.locutus.discord.api.types.tx.Transaction2;
import link.locutus.discord.commands.WarCategory;
import link.locutus.discord.commands.manager.v2.binding.annotation.Command;
import link.locutus.discord.commands.manager.v2.impl.pw.NationFilter;
import link.locutus.discord.commands.manager.v2.builder.RankBuilder;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.entities.*;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.announce.AnnounceType;
import link.locutus.discord.db.entities.announce.Announcement;
import link.locutus.discord.db.entities.sheet.CustomSheetManager;
import link.locutus.discord.db.guild.GuildSetting;
import link.locutus.discord.db.guild.GuildKey;
import link.locutus.discord.db.guild.SheetKey;
import link.locutus.discord.db.handlers.SyncableDatabase;
import link.locutus.discord.gpt.pw.PWGPTHandler;
import link.locutus.discord.pnw.AllianceList;
import link.locutus.discord.pnw.GuildOrAlliance;
import link.locutus.discord.pnw.NationOrAlliance;
import link.locutus.discord.pnw.NationOrAllianceOrGuild;
import link.locutus.discord.util.*;
import link.locutus.discord.util.scheduler.ThrowingBiConsumer;
import link.locutus.discord.util.scheduler.ThrowingConsumer;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.discord.DiscordUtil;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.offshore.test.IACategory;
import link.locutus.discord.util.scheduler.ThrowingFunction;
import link.locutus.discord.util.task.roles.AutoRoleTask;
import link.locutus.discord.util.task.roles.IAutoRoleTask;
import com.google.common.eventbus.EventBus;
import com.google.gson.JsonObject;
import link.locutus.discord.api.generated.ResourceType;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.DefaultGuildChannelUnion;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The GuildDB class represents a Discord guild database for the Locutus Discord bot written in Java.
 * It is specifically designed for the web browser nation simulation game Politics And War.
 *
 * Responsibilities:
 * - Managing guild-specific settings and configurations.
 * - Handling various transactions and balances related to nations, alliances, and banks.
 * - Handling announcements, interviews, copy pastas, and permissions within the guild.
 * - Managing war channels and coalitions.
 *
 * Relationship to other classes:
 * The GuildDB class interacts with other classes such as Locutus, ApiKeyPool, GrantTemplateManager, AllianceList,
 * DBAlliance, DBNation, CityBuildRange, Transaction2, OffshoreInstance, and more, to perform its functionalities
 * within the Discord bot and the Politics And War game.
 *
 * Constructor Summary:
 * - GuildDB(Guild guild): Constructs a GuildDB object for the specified Discord guild.
 *
 * Field Summary:
 * - private final Guild guild: The Discord guild associated with the GuildDB.
 * - private volatile IAutoRoleTask autoRoleTask: The automatic role task associated with the GuildDB.
 * - private GuildHandler handler: The guild handler associated with the GuildDB.
 * - private IACategory iaCat: The IACategory associated with the GuildDB.
 * - private GrantTemplateManager grantTemplateManager: The grant template manager associated with the GuildDB.
 * - private boolean cachedRoleAliases: Flag indicating if role aliases are cached.
 * - private final Map<Roles,Map<Long,Long>> roleToAccountToDiscord: Mapping of roles to Discord account IDs.
 * - private EventBus eventBus: The event bus associated with the GuildDB.
 * - private WarCategory warChannel: The war channel associated with the GuildDB.
 * - public boolean warChannelInit: Flag indicating if the war channel is initialized.
 * - private Throwable warCatError: Error associated with the war category.
 * - private final String description: Description of the GuildDB class.
 * - private final Map<GuildSetting,Object> infoParsed: Parsed information associated with the GuildDB.
 * - private final Object nullInstance: Null instance object.
 * - private Map<Class,Integer> permissions: Permissions associated with the GuildDB.
 * - private Map<String,Set<Long>> coalitions: Coalitions associated with the GuildDB.
 *
 * Example Usage:
 * Guild guild = event.getGuild();
 * GuildDB guildDB = new GuildDB(guild);
 * guildDB.addBalance(tx_datetime, account, banker, "Transaction note", amount);
 *
 * Most Related:
 * {@link Locutus} - The main Discord bot class that utilizes the GuildDB for guild-specific operations.
 */
public class GuildDB extends DBMainV2 implements NationOrAllianceOrGuild, GuildOrAlliance, SyncableDatabase {
    private final Guild guild;
    private volatile IAutoRoleTask autoRoleTask;
    private GuildHandler handler;
    private IACategory iaCat;
    private CustomSheetManager sheetManager;
    private volatile boolean cachedRoleAliases = false;
    private final Map<Roles, Map<Long, Long>> roleToAccountToDiscord;

    public GuildDB(Guild guild) throws SQLException, ClassNotFoundException {
        super("guilds/" + guild.getId());
        this.roleToAccountToDiscord  = new ConcurrentHashMap<>();
        this.guild = guild;
        Logg.text(guild + " | AA:" + StringMan.getString(getInfoRaw(GuildKey.ALLIANCE_ID, false)));
        importLegacyRoles();
        PWGPTHandler gpt = Locutus.imp().getCommandManager().getV2().getPwgptHandler();
        if (gpt != null) {
            gpt.getConverter().initDocumentConversion(this);
        }
    }

    @Override
    public Set<String> getTablesAllowingDeletion() {
        Set<String> tables = new HashSet<>();
        tables.add("NATION_META");
        tables.add("ROLES2");
        tables.add("INFO");
        tables.add("COALITIONS");
        return tables;
    }

    @Override
    public Map<String, String> getTablesToSync() {
        Map<String, String> tableToColumn = new LinkedHashMap<>();
        tableToColumn.put("INTERNAL_TRANSACTIONS2", "tx_datetime");
        tableToColumn.put("NATION_META", "date_updated");
        tableToColumn.put("ROLES2", "date_updated");
        tableToColumn.put("INFO", "date_updated");
        tableToColumn.put("COALITIONS", "date_updated");
        return tableToColumn;
    }

    public CustomSheetManager getSheetManager() {
        if (sheetManager == null) {
            synchronized (this) {
                if (sheetManager == null) {
                    sheetManager = new CustomSheetManager(this);
                }
            }
        }
        return sheetManager;
    }

    public long getLastModified() {
        return getFile().lastModified();
    }

    private void importLegacyRoles() {

        try {
            if (tableExists("ROLES")) {
                // get records from ROLES
                try (PreparedStatement stmt = prepareQuery("SELECT * FROM ROLES")) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            String roleName = rs.getString("role");
                            long alias = rs.getLong("alias");
//                            long alliance = rs.getLong("alliance");

                            Roles role = Roles.getRoleByNameLegacy(roleName);
                            if (role == null) {
                                switch (roleName.toLowerCase(Locale.ROOT)) {
                                    case "distributor":
                                    case "ambassador":
                                    case "active":
                                    case "war_alert":
                                    case "map_full_alert":
                                    case "beige_alert_30m":
                                        continue;
                                }
                                throw new IllegalArgumentException("Unknown legacy role: " + roleName);
                            }
                            addRole(role, alias, 0);
                        }
                    }
                }
                // drop tables ROLES
                executeStmt("DROP TABLE ROLES");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setAutoRoleTask(IAutoRoleTask autoRoleTask) {
        this.autoRoleTask = autoRoleTask;
    }

    public synchronized IACategory getExistingIACategory() {
        return iaCat;
    }

    public synchronized IACategory getIACategory() {
        return getIACategory(false, true, false);
    }
    public synchronized IACategory getIACategory(boolean create, boolean allowDelegate, boolean throwError) {
        GuildDB delegate = allowDelegate ? getDelegateServer() : null;
        if (delegate != null && delegate.iaCat != null) {
            return delegate.iaCat;
        }
        if (this.iaCat == null) {
            boolean hasInterview = false;
            for (Category category : guild.getCategories()) {
                if (category.getName().toLowerCase().startsWith("interview")) {
                    hasInterview = true;
                }
            }

            if (hasInterview) {
                this.iaCat = new IACategory(this);
                this.iaCat.load();
            }
        }
        if (iaCat == null && delegate != null) {
            iaCat = delegate.getIACategory(false, false, throwError);
        }
        if (iaCat == null && create) {
            Category category = RateLimitUtil.complete(guild.createCategory("interview"));
            this.iaCat = new IACategory(this);
            this.iaCat.load();
        }
        if (iaCat == null && throwError) {
            throw new IllegalStateException("No `interview` category found");
        }
        return this.iaCat;
    }

    private EventBus eventBus = null;

    public void postEvent(Object event) {
        EventBus bus = getEventBus();
        if (bus != null) bus.post(event);
    }

    public EventBus getEventBus() {
        if (this.eventBus == null) {
            if (hasAlliance()) {
                synchronized (this) {
                    if (this.eventBus == null) {
                        this.eventBus = new AsyncEventBus(getGuild().toString(), Runnable::run);
                        eventBus.register(getHandler());
                    }
                }
            }
        }
        return eventBus;
    }

    public synchronized void setHandler(GuildHandler handler) {
        if (eventBus == null) {
            this.eventBus = new AsyncEventBus(getGuild().toString(), Runnable::run);
        } else if (this.handler != null) {
            eventBus.unregister(this.handler);
        }
        this.handler = handler;
        this.eventBus.register(this.handler);
    }

    public synchronized GuildHandler getHandler() {
        if (handler == null) {
            this.handler = new GuildHandler(guild, this);
        }
        return handler;
    }

    public Guild getGuild() {
        return guild;
    }

    public ApiKeyPool getApiKey(int allianceId, boolean requireLeader) {
        List<ApiKeyPool.ApiKey> keys = getOrNull(GuildKey.API_KEY);
        keys.removeIf(f -> {
            DBNation nation = f.getNation();
            if (nation == null) return true;
            return nation.getAlliance_id() != allianceId || (requireLeader && nation.getPositionEnum().id < Rank.LEADER.id);
        });
        if (keys.isEmpty()) return null;
        ApiKeyPool.SimpleBuilder builder = ApiKeyPool.builder();
        for (ApiKeyPool.ApiKey key : keys) {
            builder.addKey(key);
        }
        return builder.build();
    }

    public boolean hasCoalitionPermsOnRoot(Coalition coalition) {
        return hasCoalitionPermsOnRoot(coalition.name().toLowerCase());
    }

    public boolean hasCoalitionPermsOnRoot(String coalition) {
        return hasCoalitionPermsOnRoot(coalition, true);
    }
    public boolean hasCoalitionPermsOnRoot(String coalition, boolean allowDelegate) {
        Set<Integer> aaids = getAllianceIds();
        GuildDB rootDB = Locutus.imp().getRootDb();
        if (this == rootDB) return true;
        if (rootDB == null) return false;
        Set<Long> coalMembers = rootDB.getCoalitionRaw(coalition);
        if (coalMembers.contains(getIdLong())) {
            return true;
        }
        for (Integer id : aaids) {
            if (coalMembers.contains(id.longValue())) {
                return true;
            }
        }
        if (allowDelegate) {
            GuildDB delegate = getDelegateServer();
            if (delegate != null) {
                return delegate.hasCoalitionPermsOnRoot(coalition);
            }
        }
        return false;
    }

    public String getOrThrow(SheetKey key) {
        String value = getInfo(key, true);
        if (value == null) {
            throw new UnsupportedOperationException("No `" + key.name() + "` has been set.");
        }
        return value;
    }

    public <T> T getOrThrow(GuildSetting<T> key) {
        return getOrThrow(key, true);
    }

    public <T> T getOrThrow(GuildSetting<T> key, boolean allowDelegate) {
        T value = getOrNull(key, allowDelegate);
        if (value == null) {
            throw new UnsupportedOperationException("No " + key.name() + " registered. Use " + key.getCommandMention());
        }
        return value;
    }

    public <T> T getOrNull(GuildSetting<T> key, boolean allowDelegate) {
        Object parsed;
        synchronized (infoParsed) {
            parsed = infoParsed.getOrDefault(key, nullInstance);
        }
        if (parsed != nullInstance) return (T) parsed;

        boolean isDelegate = false;
        String value = getInfoRaw(key, false);
        if (value == null) {
            isDelegate = true;
            if (allowDelegate) {
                value = getInfoRaw(key, true);
            }
        }
        if (value == null) return null;
        try {
            parsed =  (T) key.parse(this, value);
            if (!isDelegate) {
                synchronized (infoParsed) {
                    infoParsed.put(key, parsed);
                }
            }
            return (T) parsed;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public <T> T getOrNull(GuildSetting<T> key) {
        return getOrNull(key, true);
    }

    public Map<String,String> getKeys() {
        return Collections.unmodifiableMap(info);
    }

    public void setMeta(long userId, NationMeta key, byte[] value) {
        GuildDB delegate = getDelegateServer();
        if (delegate != null) {
            delegate.setMeta(userId, key, value);
            return;
        }
        checkNotNull(key);
        checkNotNull(value);
        update("INSERT OR REPLACE INTO `NATION_META`(`id`, `key`, `meta`, `date_updated`) VALUES(?, ?, ?, ?)", (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setLong(1, userId);
            stmt.setInt(2, key.ordinal());
            stmt.setBytes(3, value);
            stmt.setLong(4, System.currentTimeMillis());
        });
    }

    public Map<DBNation, byte[]> getNationMetaMap(NationMeta key) {
        GuildDB delegate = getDelegateServer();
        if (delegate != null) {
            return delegate.getNationMetaMap(key);
        }
        Map<DBNation, byte[]> result = new HashMap<>();
        try (PreparedStatement stmt = prepareQuery("select * FROM NATION_META where key = ?")) {
            stmt.setInt(1, key.ordinal());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long nationId = rs.getInt("id");
                    if (nationId < Integer.MAX_VALUE) {
                        DBNation nation = Locutus.imp().getNationDB().getNation((int) nationId);
                        byte[] data = rs.getBytes("meta");
                        result.put(nation, data);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public ByteBuffer getNationMeta(int nationId, NationMeta key) {
        return getMeta((long) nationId, key);
    }

    public Map<Long, ByteBuffer> getAllMeta(NationMeta key) {
        GuildDB delegate = getDelegateServer();
        if (delegate != null) {
            return delegate.getAllMeta(key);
        }
        Map<Long, ByteBuffer> results = new HashMap<>();
        try (PreparedStatement stmt = prepareQuery("select * FROM NATION_META where AND key = ?")) {
            stmt.setInt(1, key.ordinal());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    ByteBuffer buf = ByteBuffer.wrap(rs.getBytes("meta"));
                    results.put(id, buf);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    public void deleteAllMeta(NationMeta meta) {
        update("DELETE FROM NATION_META where key = ?", (ThrowingConsumer<PreparedStatement>) stmt -> stmt.setInt(1, meta.ordinal()));
    }

    public ByteBuffer getMeta(long userId, NationMeta key) {
        GuildDB delegate = getDelegateServer();
        if (delegate != null) {
            return delegate.getMeta(userId, key);
        }
        try (PreparedStatement stmt = prepareQuery("select * FROM NATION_META where id = ? AND key = ?")) {
            stmt.setLong(1, userId);
            stmt.setInt(2, key.ordinal());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    return ByteBuffer.wrap(rs.getBytes("meta"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deleteMeta(long userId, NationMeta key) {
        GuildDB delegate = getDelegateServer();
        if (delegate != null) {
            delegate.deleteMeta(userId, key);
            return;
        }
        synchronized (this) {
            logDeletion("NATION_META", System.currentTimeMillis(), new String[]{"id", "key"}, userId, key.ordinal());
            update("DELETE FROM NATION_META where id = ? AND key = ?", new ThrowingConsumer<PreparedStatement>() {
                @Override
                public void acceptThrows(PreparedStatement stmt) throws Exception {
                    stmt.setLong(1, userId);
                    stmt.setInt(2, key.ordinal());
                }
            });
        }
    }

    @Override
    public long getIdLong() {
        return guild.getIdLong();
    }

    @Override
    public boolean isAlliance() {
        return false;
    }

    @Override
    public int getAlliance_id() {
        throw new UnsupportedOperationException("Not an alliance");
    }

    public boolean hasAlliance() {
        return getOrNull(GuildKey.ALLIANCE_ID) != null;
    }

    /**
     * @return the alliances registered, or null
     */
    public AllianceList getAllianceList() {
        Set<Integer> ids = getAllianceIds();
        if (ids.isEmpty()) return null;
        return new AllianceList(ids);
    }

    @Override
    public String getName() {
        return guild.getName() + "/" + guild.getIdLong();
    }

    public String getUrl() {
        Role botRole = guild.getBotRole();
        if (botRole == null || !botRole.hasPermission(Permission.MANAGE_SERVER)) {
            return null;
        }
        try {
            Invite invite = getInvite(false);
            if (invite != null) return invite.getUrl();
        } catch (RuntimeException ignore) {
            ignore.printStackTrace();
        }
        return null;
    }

    public Invite getInvite(boolean create) {
        List<Invite> invites = RateLimitUtil.complete(guild.retrieveInvites());
        for (Invite invite : invites) {
            if (invite.getMaxUses() == 0) {
                return invite;
            }
        }
        if (create) {
            DefaultGuildChannelUnion defChannel = guild.getDefaultChannel();
            if (defChannel == null) return null;
            return RateLimitUtil.complete(defChannel.createInvite().setUnique(false).setMaxAge(Integer.MAX_VALUE).setMaxUses(0));
        }
        return null;
    }

    public void addTransactions(List<BankTransfer> transactions) {
        GuildDB delegate = getDelegateServer();
        if (delegate != null) {
            delegate.addTransactions(transactions);
            return;
        }
        if (transactions.isEmpty()) return;
        save(transactions);
    }

    public void addTransaction(BankTransfer tx) {
        GuildDB delegate = getDelegateServer();
        if (delegate != null) {
            delegate.addTransaction(tx);
            return;
        }
        save(List.of(tx));
        MessageChannel output = getOrNull(GuildKey.ADDBALANCE_ALERT_CHANNEL);
        if (output != null) {
            try {
                RateLimitUtil.queueWhenFree(output.sendMessage(tx.toString()));
            } catch (Throwable ignore) {
                ignore.printStackTrace();
            }
        }
    }

    public List<GuildSetting> listInaccessibleChannelKeys() {
        List<GuildSetting> inaccessible = new ArrayList<>();
        for (GuildSetting key : GuildKey.values()) {
            String valueStr = getInfoRaw(key, false);
            if (valueStr == null) continue;
            Object value = key.parse(this, valueStr);
            if (value == null) {
                inaccessible.add(key);
                continue;
            }
            if (value instanceof GuildMessageChannel) {
                GuildMessageChannel channel = (GuildMessageChannel) value;
                if (!channel.canTalk()) {
                    inaccessible.add(key);
                }
            }
        }
        return inaccessible;
    }
    public void unsetInaccessibleChannels() {
        for (GuildSetting key : listInaccessibleChannelKeys()) {
            deleteInfo(key);
        }
    }
    public void deleteExpire_bugFix() {
        GuildDB delegate = getDelegateServer();
        if (delegate != null) {
            delegate.deleteExpire_bugFix();
            return;
        }
        String query = "DELETE FROM INTERNAL_TRANSACTIONS2 WHERE lower(note) like \"%timestamp:%\"";
        executeStmt(query);
    }

    public void updateNoteDate_bugFix() {
        GuildDB delegate = getDelegateServer();
        if (delegate != null) {
            delegate.updateNoteDate_bugFix();
            return;
        }
        long date = System.currentTimeMillis();
        String query = "UPDATE INTERNAL_TRANSACTIONS2 set tx_datetime = " + date + " where lower(note) like \"%timestamp:%\"";
        executeStmt(query);
    }

    public List<BankTransfer> getDepositOffsetTransactions(long id) {
        long sender_id = Math.abs(id);
        int sender_type;
        if (sender_id > Integer.MAX_VALUE) {
            sender_type = id > 0 ? 3 : 4;
        } else {
            sender_type = id >= 0 ? 1 : 2;
        }
        return getDepositOffsetTransactions(sender_id, sender_type);
    }

    public List<BankTransfer> getDepositOffsetTransactions(long sender_id, int sender_type) {
        List<BankTransfer> transfers = getTransactionsById(sender_id, sender_type);
        for (Transaction2 transfer : transfers) {
            transfer.tx_id = -1;
        }
        return transfers;
    }

    public List<BankTransfer> getTransactionsById(long senderOrReceiverId, int type) {
        String query = "select * FROM INTERNAL_TRANSACTIONS2 WHERE ((sender_id = ? AND sender_TYPE = ?) OR (receiver_id = ? AND receiver_type = ?))";
        return getBankTransfer(query, stmt -> {
            stmt.setLong(1, senderOrReceiverId);
            stmt.setInt(2, type);
            stmt.setLong(3, senderOrReceiverId);
            stmt.setInt(4, type);
        });
    }

    private List<BankTransfer> getBankTransfer(String query, ThrowingConsumer<PreparedStatement> accept) {
        GuildDB delegate = getDelegateServer();
        if (delegate != null) {
            return delegate.getBankTransfer(query, accept);
        }
        return select(new BankTransfer(), query, accept);
    }

    public List<BankTransfer> getTransactions(long minDateMs, boolean desc) {
        String query = "select * FROM INTERNAL_TRANSACTIONS2 WHERE tx_datetime > ? ORDER BY tx_id " + (desc ? "DESC" : "ASC");
        return getBankTransfer(query, stmt -> stmt.setLong(1, minDateMs));
    }

    @Override
    public void createTables() {
        {
            // TODO deposits
            // TODO equipment

            StringBuilder query = new StringBuilder("CREATE TABLE IF NOT EXISTS `INTERNAL_TRANSACTIONS2` (" +
                    "`tx_id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "tx_datetime BIGINT NOT NULL, " +
                    "sender_id BIGINT NOT NULL, " +
                    "sender_type INT NOT NULL, " +
                    "receiver_id BIGINT NOT NULL, " +
                    "receiver_type INT NOT NULL, " +
                    "banker_nation_id INT NOT NULL, " +
                    "note varchar");

            for (ResourceType type : ResourceType.values) {
                query.append(", " + type.name() + " BIGINT NOT NULL");
            }
            query.append(")");

            executeStmt(query.toString());
        }
        {
            String query = "CREATE TABLE IF NOT EXISTS `ANNOUNCEMENTS2` (`ann_id` INTEGER PRIMARY KEY AUTOINCREMENT, `sender` BIGINT NOT NULL, `active` BOOLEAN NOT NULL, `title` VARCHAR NOT NULL, `content` VARCHAR NOT NULL, `replacements` VARCHAR NOT NULL, `filter` VARCHAR NOT NULL, `date` BIGINT NOT NULL, `allow_creation` BOOLEAN NOT NULL)";
            executeStmt(query);

            String query2 = "CREATE TABLE IF NOT EXISTS `ANNOUNCEMENTS_PLAYER2` (`receiver` INT NOT NULL, `ann_id` INT NOT NULL, `active` BOOLEAN NOT NULL, `diff` BLOB NOT NULL, PRIMARY KEY(receiver, ann_id), FOREIGN KEY(ann_id) REFERENCES ANNOUNCEMENTS2(ann_id))";
            executeStmt(query2);
        }
        {
            String query = "CREATE TABLE IF NOT EXISTS `INTERVIEW_MESSAGES2` (`message_id` BIGINT NOT NULL PRIMARY KEY, `channel_id` BIGINT NOT NULL, `sender` BIGINT NOT NULL, `date_created` BIGINT NOT NULL, `date_updated` BIGINT NOT NULL, `message` VARCHAR NOT NULL)";
            executeStmt(query);
            purgeOldInterviews(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(14));
        }
        {
            String nations = "CREATE TABLE IF NOT EXISTS `NATION_META` (`id` BIGINT NOT NULL, `key` BIGINT NOT NULL, `meta` BLOB NOT NULL, date_updated BIGINT NOT NULL, PRIMARY KEY(id, key))";
            executeStmt(nations);
        };

        {
            String create = "CREATE TABLE IF NOT EXISTS `ROLES2` (`role` BIGINT NOT NULL, `alias` BIGINT NOT NULL, `alliance` BIGINT NOT NULL, `date_updated` BIGINT NOT NULL, PRIMARY KEY(role, alliance))";
            executeStmt(create);
        };
        {
            String create = "CREATE TABLE IF NOT EXISTS `COALITIONS` (`alliance_id` BIGINT NOT NULL, `coalition` VARCHAR NOT NULL, `date_updated` BIGINT NOT NULL, PRIMARY KEY(alliance_id, coalition))";
            executeStmt(create);
        };
        {
            String create = "CREATE TABLE IF NOT EXISTS `INFO` (`key` VARCHAR NOT NULL PRIMARY KEY, `value` VARCHAR NOT NULL, `date_updated` BIGINT NOT NULL)";
            executeStmt(create);
        };
        createDeletionsTables();
    }


    public void purgeOldInterviews(long cutoff) {
        update("DELETE FROM INTERVIEW_MESSAGES2 WHERE date_updated < ?", (ThrowingConsumer<PreparedStatement>) stmt -> stmt.setLong(1, cutoff));
    }

    public void addInterviewMessage(Message message, boolean createMessageOnFail) {
        User author = message.getAuthor();
        GuildMessageChannel channel = message.getGuildChannel();
        long channelId = channel.getIdLong();
        long date = message.getTimeCreated().toInstant().toEpochMilli();
        long now = System.currentTimeMillis();

        if (author.isBot() || author.isSystem() || message.isWebhookMessage()) {
            if (createMessageOnFail) {
                addInterviewMessage(channelId, author.getIdLong(), message.getIdLong(), date, now, DiscordUtil.trimContent(message.getContentRaw()));
            } else {
                updateInterviewMessageDate(channelId);
            }
        } else {
            addInterviewMessage(channelId, author.getIdLong(), message.getIdLong(), date, now, DiscordUtil.trimContent(message.getContentRaw()));
        }

    }

    public void addInterviewMessage(long channelId, long sender, long message_id, long dateCreated, long dateUpdated, String message) {
        ByteBuffer optOut = DiscordMeta.OPT_OUT.get(sender);
        if (optOut != null && optOut.get() != 0) return;

        // "CREATE TABLE IF NOT EXISTS `INTERVIEW_MESSAGES2` (`channel_id` INTEGER NOT NULL PRIMARY KEY, `sender` INT NOT NULL, `date` INT NOT NULL, `message` VARCHAR NOT NULL)";
        String query = "INSERT OR IGNORE INTO INTERVIEW_MESSAGES2(`channel_id`, `sender`, `message_id`, `date_created`, `date_updated`, `message`) VALUES(?, ?, ?, ?, ?, ?)";
        update(query , (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setLong(1, channelId);
            stmt.setLong(2, sender);
            stmt.setLong(3, message_id);
            stmt.setLong(4, dateCreated);
            stmt.setLong(5, dateUpdated);
            stmt.setString(6, message);
        });
    }

    public void updateInterviewMessageDate(long channelId) {
        String query = "UPDATE INTERVIEW_MESSAGES2 SET `date_updated` = ? WHERE `channel_id` = ? AND date_updated = (SELECT MAX(date_updated) FROM INTERVIEW_MESSAGES2 WHERE `channel_id` = ?)";
        update(query, (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setLong(1, System.currentTimeMillis());
            stmt.setLong(2, channelId);
            stmt.setLong(3, channelId);
        });
    }

    public Map<Long, InterviewMessage> getLatestInterviewMessages() {
        IACategory iaCat = getIACategory();
        if (iaCat == null) return null;
        iaCat.load();

        Map<Long, InterviewMessage> result = new LinkedHashMap<>();
        query("select * FROM INTERVIEW_MESSAGES2 ORDER BY date_created desc",
                f -> {},
                (ThrowingConsumer<ResultSet>) rs -> {
                    while (rs.next()) {
                        InterviewMessage msg = new InterviewMessage(rs);
                        InterviewMessage existing = result.get(msg.channelId);
                        if (existing == null || existing.date_created < msg.date_created) {
                            result.put(msg.channelId, msg);
                        }
                    }
                });
        return result;
    }

    public void deleteInterviewMessages(long userId) {
        update("DELETE FROM INTERVIEW_MESSAGES2 WHERE sender = ?", (ThrowingConsumer<PreparedStatement>) stmt -> stmt.setLong(1, userId));
    }

    public Map<Long, List<InterviewMessage>> getInterviewMessages() {
        IACategory iaCat = getIACategory();
        if (iaCat == null) return null;
        iaCat.load();

        Map<Long, List<InterviewMessage>> result = new LinkedHashMap<>();
        query("select * FROM INTERVIEW_MESSAGES2 ORDER BY date_created desc",
                f -> {},
                (ThrowingConsumer<ResultSet>) rs -> {
                    while (rs.next()) {
                        InterviewMessage msg = new InterviewMessage(rs);
                        result.computeIfAbsent(msg.channelId, f -> new ArrayList<>()).add(msg);
                    }
                });
        return result;
    }

    public int addAnnouncement(User sender, String title, String content, String replacements, String filter, boolean allowCreation) {
        String query = "INSERT INTO `ANNOUNCEMENTS2`(`sender`, `active`, `title`, `content`, `replacements`, `filter`, `date`, `allow_creation`) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, sender.getIdLong());
            stmt.setBoolean(2, true);
            stmt.setString(3, title);
            stmt.setString(4, content);
            stmt.setString(5, replacements);
            stmt.setString(6, filter);
            stmt.setLong(7, System.currentTimeMillis());
            stmt.setBoolean(8, allowCreation);
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    return id;
                }
            }
            throw new IllegalArgumentException("Error creating announcement");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void addPlayerAnnouncement(DBNation receiver, int annId, byte[] diff) {
        String query = "INSERT OR REPLACE INTO ANNOUNCEMENTS_PLAYER2(`receiver`, `ann_id`, `active`, `diff`) VALUES(?, ?, ?, ?)";
        update(query , (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setInt(1, receiver.getNation_id());
            stmt.setInt(2, annId);
            stmt.setBoolean(3, true);
            stmt.setBytes(4, diff);
        });
    }

    public List<Announcement> getAnnouncements() {
        List<Announcement> result = new ArrayList<>();
        try (PreparedStatement stmt = prepareQuery("select * FROM ANNOUNCEMENTS2 ORDER BY date desc")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new Announcement(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Announcement getAnnouncement(int ann_id) {
        List<Announcement> result = new ArrayList<>();
        query("select * FROM ANNOUNCEMENTS2 WHERE ann_id = ? ORDER BY date desc",
                (ThrowingConsumer<PreparedStatement>) stmt -> stmt.setInt(1, ann_id),
                (ThrowingConsumer<ResultSet>) rs -> {
                    while (rs.next()) {
                        result.add(new Announcement(rs));
                    }
                });
        return result.isEmpty() ? null : result.get(0);
    }

    public Map<Integer, Announcement> getAnnouncementsByIds(Set<Integer> ids) {
        Map<Integer, Announcement> result = new LinkedHashMap<>();
        query("select * FROM ANNOUNCEMENTS2 WHERE ann_id in " + StringMan.getString(ids) + " ORDER BY date desc",
                f -> {},
                (ThrowingConsumer<ResultSet>) rs -> {
                    while (rs.next()) {
                        Announcement ann = new Announcement(rs);
                        result.put(ann.id, ann);
                    }
                });
        return result;
    }

    public void setAnnouncementActive(int ann_id, boolean value) {
        update("UPDATE ANNOUNCEMENTS2 SET active = ? WHERE ann_id = ?", new ThrowingConsumer<PreparedStatement>() {
            @Override
            public void acceptThrows(PreparedStatement stmt) throws Exception {
                stmt.setBoolean(1, value);
                stmt.setInt(2, ann_id);
            }
        });
    }

    public void setAnnouncementActive(int ann_id, int nation_id, boolean active) {
        update("UPDATE ANNOUNCEMENTS_PLAYER2 SET active = ? WHERE ann_id = ? AND receiver = ?", new ThrowingConsumer<PreparedStatement>() {
            @Override
            public void acceptThrows(PreparedStatement stmt) throws Exception {
                stmt.setBoolean(1, active);
                stmt.setInt(2, ann_id);
                stmt.setInt(3, nation_id);
            }
        });
    }

    public Map<Announcement, List<Announcement.PlayerAnnouncement>> getAllPlayerAnnouncements(boolean allowArchived) {
        List<Announcement> announcements = getAnnouncements();
        Map<Integer, Announcement> announcementsById = new LinkedHashMap<>();
        for (Announcement announcement : announcements) {
            if (!announcement.active && !allowArchived) continue;
            announcementsById.put(announcement.id, announcement);
        }

        Map<Announcement, List<Announcement.PlayerAnnouncement>> result = new LinkedHashMap<>();
        query("select * FROM ANNOUNCEMENTS_PLAYER2 ORDER BY ann_id desc",
                f -> {},
                (ThrowingConsumer<ResultSet>) rs -> {
                    while (rs.next()) {
                        int annId = rs.getInt("ann_id");
                        Announcement announcement = announcementsById.get(annId);
                        if (announcement == null) continue;
                        Announcement.PlayerAnnouncement plrAnn = new Announcement.PlayerAnnouncement(this, announcement, rs);
                        if (!allowArchived && !plrAnn.active) continue;
                        result.computeIfAbsent(announcement, f -> new ArrayList<>()).add(plrAnn);
                    }
                });
        return result;
    }

    public List<Announcement.PlayerAnnouncement> getPlayerAnnouncementsContaining(String invite) {
        List<Announcement.PlayerAnnouncement> result = new ArrayList<>();
        query("select * FROM ANNOUNCEMENTS_PLAYER2 WHERE diff LIKE ? ORDER BY ann_id desc",
                (ThrowingConsumer<PreparedStatement>) stmt -> stmt.setString(1, "%" + invite + "%"),
                (ThrowingConsumer<ResultSet>) rs -> {
                    while (rs.next()) {
                        result.add(new Announcement.PlayerAnnouncement(this, null, rs));
                    }
                });
        return result;
    }

    public List<Announcement.PlayerAnnouncement> getPlayerAnnouncementsByAnnId(int ann_id) {
        Announcement announcement = getAnnouncement(ann_id);
        if (announcement == null) return null;
        List<Announcement.PlayerAnnouncement> result = new ArrayList<>();
        query("select * FROM ANNOUNCEMENTS_PLAYER2 WHERE ann_id = ? ORDER BY ann_id desc",
                (ThrowingConsumer<PreparedStatement>) stmt -> stmt.setInt(1, ann_id),
                (ThrowingConsumer<ResultSet>) rs -> {
                    while (rs.next()) {
                        result.add(new Announcement.PlayerAnnouncement(this, announcement, rs));
                    }
                });
        return result;
    }

    public Announcement.PlayerAnnouncement getOrCreatePlayerAnnouncement(int ann_id, DBNation nation, AnnounceType create) throws IOException {
        Announcement.PlayerAnnouncement existing = getPlayerAnnouncement(ann_id, nation.getId());
        if (existing != null && !create.isValid(this, existing.getParent(), existing)) {
            existing = null;
        }
        if (existing == null) {
            Announcement announcement = getAnnouncement(ann_id);
            if (announcement == null) return null;
            Predicate<DBNation> filter = announcement.getFilter(this);
            if (!filter.test(nation)) return null;

            int currentAnnouncements = countAnnouncements(ann_id);
            String message = create.create(this, announcement, currentAnnouncements);

            byte[] diff = StringMan.getDiffBytes(announcement.body, message);
            Announcement.PlayerAnnouncement newAnnouncement = new Announcement.PlayerAnnouncement(this, announcement, diff, nation.getId(), true);
            addPlayerAnnouncement(nation, ann_id, diff);
            return newAnnouncement;
        } else {
            return existing;
        }
    }

    public Announcement.PlayerAnnouncement getPlayerAnnouncement(int ann_id, int nationId) {
        List<Announcement.PlayerAnnouncement> result = new ArrayList<>();
        query("select * FROM ANNOUNCEMENTS_PLAYER2 WHERE ann_id = ? AND receiver = ? ORDER BY ann_id desc",
                (ThrowingConsumer<PreparedStatement>) stmt -> {
                    stmt.setInt(1, ann_id);
                    stmt.setInt(2, nationId);
                },
                (ThrowingConsumer<ResultSet>) rs -> {
                    while (rs.next()) {
                        result.add(new Announcement.PlayerAnnouncement(this, rs));
                    }
                });
        return result.isEmpty() ? null : result.get(0);
    }

    public int countAnnouncements(int announcementId) {
        AtomicInteger result = new AtomicInteger();
            query("select count(*) FROM ANNOUNCEMENTS_PLAYER2 WHERE ann_id = ?",
                (ThrowingConsumer<PreparedStatement>) stmt -> stmt.setInt(1, announcementId),
                (ThrowingConsumer<ResultSet>) rs -> {
                    if (rs.next()) {
                        result.set(rs.getInt(1));
                    }
                });
        return result.get();
    }

    public List<Announcement.PlayerAnnouncement> getPlayerAnnouncementsByNation(int nationId, boolean requireActive) {
        List<Announcement.PlayerAnnouncement> result = new ArrayList<>();
        query("select * FROM ANNOUNCEMENTS_PLAYER2 WHERE receiver = ?" + (requireActive ? " AND `active` = true" : "") + " ORDER BY ann_id desc",
                (ThrowingConsumer<PreparedStatement>) stmt -> stmt.setInt(1, nationId),
                (ThrowingConsumer<ResultSet>) rs -> {
                    while (rs.next()) {
                        result.add(new Announcement.PlayerAnnouncement(this, rs));
                    }
                });
        Set<Integer> annIds = result.stream().map(f -> f.ann_id).collect(Collectors.toSet());
        Map<Integer, Announcement> announcements = getAnnouncementsByIds(annIds);
        Iterator<Announcement.PlayerAnnouncement> iter = result.iterator();
        while (iter.hasNext()) {
            Announcement.PlayerAnnouncement plrAnn = iter.next();
            Announcement announcement = announcements.get(plrAnn.ann_id);
            if (announcement != null && (announcement.active || !requireActive)) {
                plrAnn.setParent(announcement);
            } else if (requireActive) {
                iter.remove();
            }
        }
        return result;
    }

    public IAutoRoleTask getAutoRoleTask() {
        if (this.autoRoleTask == null) {
            synchronized (this) {
                if (this.autoRoleTask == null) {
                    this.autoRoleTask = new AutoRoleTask(getGuild(), this);
                }
            }
        }
        return autoRoleTask;
    }

    public void subBalanceMulti(Map<NationOrAllianceOrGuild, double[]> amounts, long dateTime, int banker, String offshoreNote) {
        for (Map.Entry<NationOrAllianceOrGuild, double[]> entry : amounts.entrySet()) {
            NationOrAllianceOrGuild account = entry.getKey();
            double[] amount = entry.getValue();
            double[] amountNegative = ResourceType.negative(amount.clone());
            if (account.isGuild()) {
                addTransfer(dateTime, 0, 0, account.getIdLong(), account.getReceiverType(), banker, offshoreNote, amountNegative);
            } else {
                addTransfer(dateTime, 0, 0, (NationOrAlliance) account, banker, offshoreNote, amountNegative);
            }
        }
    }

    public Map<NationOrAllianceOrGuild, double[]> subBalanceMulti(Map<NationOrAllianceOrGuild, double[]> depositsByAA, long dateTime, double[] amount, int banker, String offshoreNote) {
        for (int i = 0; i < amount.length; i++) {
            if (amount[i] < 0) throw new IllegalArgumentException("Amount must be positive: " + ResourceType.resourcesToString(amount));
        }

        double[] amountLeft = amount.clone();

        Map<NationOrAllianceOrGuild, double[]> ammountEach = new LinkedHashMap<>();
        for (Map.Entry<NationOrAllianceOrGuild, double[]> entry : depositsByAA.entrySet()) {
            NationOrAllianceOrGuild account = entry.getKey();
            double[] subDeposits = entry.getValue();

            double[] toSubtract = null;
            for (int i = 0; i < amountLeft.length; i++) {
                double subDepositsI = subDeposits[i];
                double amountI = amountLeft[i];
                if (Math.round(subDepositsI * 100) > 0 && Math.round(amountI * 100) > 0) {
                    double subtract = Math.min(subDepositsI, amountI);
                    if (Math.round(subtract * 100) == 0) continue;
                    if (toSubtract == null) toSubtract = ResourceType.getBuffer();
                    toSubtract[i] = subtract;
                    amountLeft[i] -= subtract;
                }
            }
            if (toSubtract != null) {
                ammountEach.put(account, toSubtract.clone());
                addTransfer(dateTime, 0, 0, account.getIdLong(), account.getReceiverType(), banker, offshoreNote, toSubtract);
            }
        }
        if (!ResourceType.isZero(amountLeft)) {
            throw new IllegalArgumentException("Could not add balance to all accounts. Amount left: " + ResourceType.resourcesToString(amountLeft));
        }
        return ammountEach;
    }

    public void addBalanceTaxId(long tx_datetime, int taxId, int banker, String note, double[] amount) {
        addTransfer(tx_datetime, taxId, 4, 0, 0, banker, note, amount);
    }

    public void addBalanceTaxId(long tx_datetime, int taxId, int nation, int banker, String note, double[] amount) {
        addTransfer(tx_datetime, taxId, 4, nation, 1, banker, note, amount);
    }

    public void subBalance(long tx_datetime, NationOrAlliance account, int banker, String note, double[] amount) {
        double[] copy = ResourceType.getBuffer();
        for (int i = 0; i < copy.length; i++) copy[i] = -amount[i];
        addBalance(tx_datetime, account, banker, note, copy);
    }

    public void addBalance(long tx_datetime, NationOrAlliance account, int banker, String note, double[] amount) {
        addTransfer(tx_datetime, account, 0, 0, banker, note, amount);
    }

    public void subtractBalance(long tx_datetime, NationOrAlliance account, int banker, String note, double[] amount) {
        addTransfer(tx_datetime, 0, 0, account, banker, note, amount);
    }

    public void addBalance(long tx_datetime, long accountId, int type, int banker, String note, double[] amount) {
        addTransfer(tx_datetime, accountId, type, 0, 0, banker, note, amount);
    }

    public void subtractBalance(long tx_datetime, long accountId, int type, int banker, String note, double[] amount) {
        addTransfer(tx_datetime, 0, 0, accountId, type, banker, note, amount);
    }

    public void addTransfer(long tx_datetime, NationOrAlliance sender, long receiver_id, int receiver_type, int banker, String note, double[] amount) {
        Map.Entry<Long, Integer> idType = sender.getTransferIdAndType();
        addTransfer(tx_datetime, idType.getKey(), idType.getValue(), receiver_id, receiver_type, banker, note, amount);
    }

    public void addTransfer(long tx_datetime, NationOrAlliance sender, NationOrAlliance receiver2, int banker, String note, double[] amount) {
        Map.Entry<Long, Integer> idType = sender.getTransferIdAndType();
        addTransfer(tx_datetime, idType.getKey(), idType.getValue(), receiver2, banker, note, amount);
    }

    public void addTransfer(long tx_datetime, long sender_id, int sender_type, NationOrAlliance receiver, int banker, String note, double[] amount) {
        Map.Entry<Long, Integer> idType = receiver.getTransferIdAndType();
        addTransfer(tx_datetime, sender_id, sender_type, idType.getKey(), idType.getValue(), banker, note, amount);
    }

    public void addTransfer(long tx_datetime, long sender_id, int sender_type, long receiver_id, int receiver_type, int banker, String note, double[] amount) {
        BankTransfer transfer = new BankTransfer();
        transfer.tx_datetime = tx_datetime;
        transfer.sender_id = sender_id;
        transfer.sender_type = sender_type;
        transfer.receiver_id = receiver_id;
        transfer.receiver_type = receiver_type;
        transfer.banker_nation = banker;
        transfer.note = note;
        transfer.resources = amount;
        transfer.generateId();
        transfer.TypeTxt = BankType.ALLIANCE_BANK_DEPOSIT;
        addTransaction(transfer);
    }

    private WarCategory warChannel;
    public boolean warChannelInit = false;

    public boolean isAllyOfRoot() {
        return isAllyOfRoot(true);
    }

    public boolean isAllyOfRoot(boolean checkWhitelist) {
        return isAllyOfRoot(type -> {
            if (type == null) return false;
            return type.isDefensive();
        }, checkWhitelist);
    }

    public boolean isAllyOfRoot(Function<TreatyType, Boolean> checkType) {
        return isAllyOfRoot(checkType, true);
    }

    public boolean isAllyOfRoot(Function<TreatyType, Boolean> checkType, boolean allowWhitelist) {
        if (allowWhitelist && isWhitelisted()) return true;
        return false;
    }

    public void disableWarChannel() {
        this.warChannelInit = true;
        this.warChannel = null;
    }

    public WarCategory getWarChannel() {
        return getWarChannel(false);
    }

    private Throwable warCatError = null;
    private String warCatErrorMsg = null;

    public WarCategory getWarChannel(boolean throwException) {
        return getWarChannel(throwException, false);
    }

    public WarCategory getWarChannel(boolean throwException, boolean isWarServer) {
        Boolean enabled = getOrNull(GuildKey.ENABLE_WAR_ROOMS, false);
        if (enabled == Boolean.FALSE || enabled == null) {
            if (throwException) {
                String msg = "War rooms are not enabled " + GuildKey.ENABLE_WAR_ROOMS.getCommandObj(this, true) + " in guild " + getGuild();
                if (warCatError != null) {
                        msg += msg + "\nPreviously disabled due to error: " + warCatError.getMessage();
                }
                throw new IllegalArgumentException(msg);
            }
            return null;
        }
        if (!isWhitelisted() && !isValidAlliance()) {
            if (throwException) {
                String msg = "Ensure there are members in this alliance, " + CM.who.cmd.toSlashMention() + " and that " + CM.settings_default.registerAlliance.cmd.toSlashMention() + " is set in guild " + getGuild();
                if (warCatError != null) {
                    msg += msg + "\nPreviously disabled due to error: " + warCatError.getMessage();
                }
                throw new IllegalArgumentException(msg);
            }
            return null;
        }
        Guild warServer = getOrNull(GuildKey.WAR_SERVER, false);
        if (warServer != null && warServer.getIdLong() != guild.getIdLong()) {
            GuildDB db = Locutus.imp().getGuildDB(warServer);
            if (db == null) {
                if (throwException) throw new IllegalArgumentException("There is a null war server set (or delegated to) " + GuildKey.WAR_SERVER.getCommandMention() + " in guild " + getGuild());
                return null;
            }
            if (db.getOrNull(GuildKey.WAR_SERVER, false) != null) {
                if (throwException) throw new IllegalArgumentException("There is a null war server set " + GuildKey.WAR_SERVER.getCommandMention() + " in guild " + getGuild());
                return null;
            }
            return db.getWarChannel(throwException, true);
        }

        if (hasAlliance() || isWarServer) {
            if (warChannel == null) {
                if (!warChannelInit || throwException) {
                    warChannelInit = true;
                    boolean allowed = Boolean.TRUE.equals(enabled);
                    if (allowed) {
                        try {
                            warChannel = new WarCategory(guild, "warcat");
                            warCatError = null;
                            warCatErrorMsg = null;
                        } catch (Throwable e) {
                            warCatError = e;
                            warCatErrorMsg = StringMan.stripApiKey(e.getMessage());
                            if (e instanceof  InsufficientPermissionException pe) {
                                warCatErrorMsg += " in <#" + pe.getChannelId() + ">";
                            }
                            if (throwException) throw new IllegalArgumentException("There was an error creating war channels: " + warCatErrorMsg + "\n```" + StringMan.stacktraceToString(e) + "```\n" +
                                    "\nTry setting " + GuildKey.ENABLE_WAR_ROOMS.getCommandObj(this, true) + " and attempting this command again once the issue has been resolved.");
                            return null;
                        }
                    } else {
                        if (throwException) throw new IllegalArgumentException("War rooms are not enabled, see: " + CM.settings_war_room.ENABLE_WAR_ROOMS.cmd.toSlashMention());
                    }
                }
            }
//            } else if (isWhitelisted()) {
//                warChannel = new DebugWarChannel(guild, "warcat", "");
        } else if (warChannel == null) {
            if (throwException) throw new IllegalArgumentException("Please set " + CM.settings_default.registerAlliance.cmd.toSlashMention() + " in " + guild);
        }
        return warChannel;
    }

    public boolean isWhitelisted() {
        if (getIdLong() == Settings.INSTANCE.ROOT_SERVER) return true;
        if (getIdLong() == Settings.INSTANCE.ROOT_COALITION_SERVER) return true;
        if (hasCoalitionPermsOnRoot(Coalition.WHITELISTED)) return true;
        // other stuff?
        return false;
    }

    public boolean violatesDNR(DBNation defender) {
        Function<DBNation, Boolean> canRaid = getCanRaid();
        return !canRaid.apply(defender);
    }

    public GuildDB getDelegateServer() {
        Map.Entry<Integer, Long> delegate = getOrNull(GuildKey.DELEGATE_SERVER, false);
        if (delegate != null && delegate.getValue() != getIdLong()) {
            return Locutus.imp().getGuildDB(delegate.getValue());
        }
        return null;
    }

    public boolean isDelegateServer() {
        return getDelegateServer() != null;
    }

    public boolean isValidAlliance() {
        Set<Integer> aaIds = getOrNull(GuildKey.ALLIANCE_ID);
        if (aaIds == null || aaIds.isEmpty()) return false;
        for (int aaId : aaIds) {
            if (DBAlliance.get(aaId) != null) return true;
        }
        return false;
    }

    public boolean hasRequiredMMR(DBNation nation, long updateMs) {
        if (getOrNull(GuildKey.REQUIRED_MMR) == null) return true;
        return getRequiredMMR(nation, updateMs).values().stream().anyMatch(f -> f);
    }

    public Map<String, Boolean> getRequiredMMR(DBNation nation, long updateMs) {
        Map<NationFilter, MMRMatcher> requiredMmrMap = getOrNull(GuildKey.REQUIRED_MMR);
        if (requiredMmrMap == null) return null;
        Map<String, Boolean> allowedMMr = new LinkedHashMap<>();
        String myMMR = null;
        for (Map.Entry<NationFilter, MMRMatcher> entry : requiredMmrMap.entrySet()) {
            NationFilter nationMatcher = entry.getKey().recalculate(60_000);
            if (nationMatcher.test(nation)) {
                if (myMMR == null) {
                    Map<Building, Integer> buildings = nation.getPrivateData().getBuildings(updateMs, false);
                    myMMR = buildings.get(Building.ARMY_BASES) + "/" + buildings.get(Building.AIR_BASES) + "/" + buildings.get(Building.NAVAL_BASES);
                }
                MMRMatcher required = entry.getValue();
                allowedMMr.put(required.getRequired(), required.test(myMMR));
            }
        }
        return allowedMMr;
    }

    public boolean isOwnerActive() {
        if (guild == null) return false;
        Member owner = guild.getOwner();
        if (owner == null) return false;
        User user = owner.getUser();
        if (user == null) return false;
        DBNation nation = DiscordUtil.getNation(user);
        return nation != null && nation.active_m() < 10000;
    }

    public Set<String> findCoalitions(int aaId) {
        loadCoalitions();
        Set<String> coalitions = new LinkedHashSet<>();
        for (Map.Entry<Long, Set<Long>> entry : coalitions2.entrySet()) {
            if (entry.getValue().contains((long) aaId)) {
                String name = coalitionId2Name.get(entry.getKey());
                if (name != null) {
                    coalitions.add(name);
                }
            }
        }
        return coalitions;
    }

    public AddBalanceBuilder addBalanceBuilder() {
        return new AddBalanceBuilder(this);
    }

    public boolean isEnemyAlliance(int allianceId) {
        return getCoalitionRaw(Coalition.ENEMIES).contains((long) allianceId);
    }

    public void setWarCatError(InsufficientPermissionException e) {
        this.warCatError = e;
    }

    public Throwable getWarCatError() {
        return warCatError;
    }

    public Set<String> getCoalitionNames() {
        GuildDB faServer = getOrNull(GuildKey.FA_SERVER);
        if (faServer != null && faServer.getIdLong() != getIdLong()) return faServer.getCoalitionNames();
        loadCoalitions();
        return new LinkedHashSet<>(coalitionName2Id.keySet());
    }

    public enum AutoNickOption {
        FALSE("No nickname given"),
        LEADER("Set to leader name"),
        NATION("Set to nation name"),
        DISCORD("Set to discord name"),
        NICKNAME("Set to discord nickname")
        ;

        private final String description;

        AutoNickOption(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return name() + ": `" + description + "`";
        }
    }

    public enum AutoRoleOption {
        FALSE("No roles given"),
        ALL("Alliance roles created for all (see: `AUTOROLE_TOP_X`)"),
        ALLIES("Alliance roles created for allies (see: `allies` coalition)"),
        ;

        private final String description;

        AutoRoleOption(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return name() + ": `" + description + "`";
        }

        public String getDescription() {
            return description;
        }
    }

    public Function<DBNation, Boolean> getCanRaid() {
        Integer topX = getOrNull(GuildKey.DO_NOT_RAID_TOP_X);
        if (topX == null) topX = 0;
        return getCanRaid(topX, true);
    }

    public Function<DBNation, Boolean> getCanRaid(int topX, boolean checkTreaties) {
        GuildDB faServer = getOrNull(GuildKey.FA_SERVER);
        if (faServer != null && faServer.getIdLong() != getIdLong()) return faServer.getCanRaid(topX, checkTreaties);
        loadCoalitions();
        Set<Integer> dnr = new HashSet<>(getCoalition(Coalition.ALLIES));
        dnr.addAll(getCoalition("dnr"));
        Set<Integer> dnr_active = new HashSet<>(getCoalition("dnr_active"));
        Set<Integer> dnr_member = new HashSet<>(getCoalition("dnr_member"));
        Set<Integer> can_raid = new HashSet<>(getCoalition(Coalition.CAN_RAID));
        can_raid.addAll(getCoalition(Coalition.ENEMIES));
        Set<Integer> can_raid_inactive = new HashSet<>(getCoalition("can_raid_inactive"));
        Map<Integer, Long> dnr_timediff_member = new HashMap<>();
        Map<Integer, Long> dnr_timediff_app = new HashMap<>();

        if (checkTreaties) {
            for (int allianceId : getAllianceIds()) {
                Map<Integer, Treaty> treaties = Locutus.imp().getNationDB().getTreaties(allianceId);
                dnr.addAll(treaties.keySet());
                dnr.add(allianceId);
            }
        }

        if (topX > 0) {
            Map<Integer, Double> aas = new RankBuilder<>(Locutus.imp().getNationDB().getNations().values()).group(DBNation::getAlliance_id).sumValues(DBNation::getScore).sort().get();
            for (Map.Entry<Integer, Double> entry : aas.entrySet()) {
                if (entry.getKey() == 0) continue;
                if (topX-- <= 0) break;
                int allianceId = entry.getKey();
                Map<Integer, Treaty> treaties = Locutus.imp().getNationDB().getTreaties(allianceId);
                for (Map.Entry<Integer, Treaty> aaTreatyEntry : treaties.entrySet()) {
                    if (aaTreatyEntry.getValue().getType().isMandatoryDefensive()) {
                        dnr_member.add(aaTreatyEntry.getKey());
                    }
                }
                dnr_member.add(allianceId);
            }
        }

        for (Map.Entry<String, Long> entry : coalitionName2Id.entrySet()) {
            String name = entry.getKey();
            if (!name.startsWith("dnr_")) continue;

            String[] split = name.split("_");
            if (split.length < 2 || split[split.length - 1].isEmpty()) continue;
            String timeStr = split[split.length - 1];
            if (!Character.isDigit(timeStr.charAt(0))) continue;

            long time = TimeUtil.timeToSec(timeStr) * 1000L;

            Set<Long> ids = coalitions2.get(entry.getValue());
            if (ids == null || ids.isEmpty()) continue;

            for (long id : ids) {
                if (id > Integer.MAX_VALUE) continue;
                int aaId = (int) id;
                if (split.length == 3 && split[1].equalsIgnoreCase("member")) {
                    dnr_timediff_member.put(aaId, time);
                } else if (true || split[1].equalsIgnoreCase("applicant")) {
                    dnr_timediff_app.put(aaId, time);
                }

            }
        }
        Set<Integer> enemies = getCoalition("enemies");
        enemies.addAll(getCoalition(Coalition.CAN_RAID));

        Function<DBNation, Boolean> canRaid = new Function<DBNation, Boolean>() {
            @Override
            public Boolean apply(DBNation enemy) {
                if (enemy.getAlliance_id() == 0) return true;
                if (can_raid.contains(enemy.getAlliance_id())) return true;
                if (can_raid_inactive.contains(enemy.getAlliance_id()) && enemy.active_m() > 10000) return true;
                if (enemies.contains(enemy.getAlliance_id())) return true;
                if (dnr.contains(enemy.getAlliance_id())) return false;
                if (enemy.active_m() < 10000 && dnr_active.contains(enemy.getAlliance_id())) return false;
                if ((enemy.active_m() < 10000 || enemy.getPosition() > 1) && dnr_member.contains(enemy.getAlliance_id())) return false;

                long requiredInactive = -1;

                {
                    Long timeDiff = dnr_timediff_app.get(enemy.getAlliance_id());
                    if (timeDiff != null) {
                        requiredInactive = enemy.getPosition() > 1 ? Long.MAX_VALUE : timeDiff;
                    }
                }
                if (enemy.getPosition() > 1) {
                    Long timeDiff = dnr_timediff_member.get(enemy.getAlliance_id());
                    if (timeDiff != null) {
                        requiredInactive = timeDiff;
                    }
                }

                long msInactive = enemy.active_m() * 60 * 1000L;

                return (msInactive > requiredInactive);
            }
        };

        return canRaid;
    }

    public Map<String, String> getCopyPastas(@Nullable Member memberOrNull) {
        Map<String, String> options = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : getInfoMap().entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("copypasta.")) continue;
            if (memberOrNull != null && !getMissingCopypastaPerms(key, memberOrNull).isEmpty()) continue;
            options.put(key.substring("copypasta.".length()), entry.getValue());
        }
        return options;
    }

    /**
     * Returns the missing copypasta permissions for the given member.
     * @param key
     * @param member
     * @return
     */
    public Set<String> getMissingCopypastaPerms(String key, Member member) {
        String[] split = key.split("\\.");
        Set<String> noRoles = new HashSet<>();
        if (split.length > 1) {
            for (int i = 0; i < split.length - 1; i++) {
                String roleName = split[i];
                if (roleName.equalsIgnoreCase("copypasta")) continue;

                Roles role = Roles.parse(roleName);
                Role discRole = DiscordUtil.getRole(member.getGuild(), roleName);
                if ((role != null && role.has(member)) || (discRole != null && member.getRoles().contains(discRole))) {
                    return Collections.emptySet();
                }
                if (role != null) noRoles.add(role.toString());
                if (discRole != null) noRoles.add(discRole.getName());
            }
        }
        return noRoles;
    }

    public Map<String, String> getInfoMap() {
        initInfo();
        return info;
    }

    public String getInfo(SheetKey key, boolean allowDelegate) {
        return getInfo(key.name(), allowDelegate);
    }

    public String getInfoRaw(GuildSetting key, boolean allowDelegate) {
        if (key == GuildKey.ALLIANCE_ID) {
            String result = getInfo(key.name(), false);
            if (result != null || !allowDelegate) return result;
        }
        return getInfo(key.name(), allowDelegate);
    }

    public <T> void setInfo(GuildSetting<T> key, User user, T value) {
        checkNotNull(key);
        checkNotNull(value);
        value = key.validate(this, user, value);
        setInfoRaw(key, value);
    }

    public <T> void setInfoRaw(GuildSetting<T> key, T value) {
        String toSave = key.toString(value);
        synchronized (infoParsed) {
            setInfo(key.name(), toSave);
            infoParsed.put(key, value);
        }
    }

    private Map<String, String> info;
    private final Map<GuildSetting, Object> infoParsed = new HashMap<>();
    private final Object nullInstance = new Object();

    public String getCopyPasta(String key, boolean allowDelegate) {
        return getInfo("copypasta." + key, allowDelegate);
    }

    private String getInfo(String key, boolean allowDelegate) {
        if (info == null) {
            initInfo();
        }
        String value = info.get(key.toLowerCase());
        if (value == null && allowDelegate) {
            Map.Entry<Integer, Long> delegate = getOrNull(GuildKey.DELEGATE_SERVER, false);
            if (delegate != null && delegate.getValue() != getIdLong()) {
                GuildDB delegateDb = Locutus.imp().getGuildDB(delegate.getValue());
                if (delegateDb != null) {
                    value = delegateDb.getInfo(key, false);
                }
            }
        }
        return value;
    }

    public void deleteInfo(GuildSetting key) {
        synchronized (infoParsed) {
            deleteInfo(key.name());
            infoParsed.remove(key);
        }
    }

    public void deleteCopyPasta(String key) {
        deleteInfo("copypasta." + key);
    }

    private void deleteInfo(String key) {
        String keyLower = key.toLowerCase(Locale.ROOT);
        info.remove(keyLower);
        synchronized (this) {
            logDeletion("INFO", System.currentTimeMillis(), "key", keyLower);
            update("DELETE FROM `INFO` where `key` = ?", (ThrowingConsumer<PreparedStatement>) stmt -> {
                stmt.setString(1, keyLower);
            });
        }
    }

    public void setCopyPasta(String key, String value) {
        setInfo("copypasta." + key, value);
    }

    public void setInfo(SheetKey key, String value) {
        setInfo(key.name(), value);
    }

    public void setInfo(String key, String value) {
        checkNotNull(key);
        checkNotNull(value);
        initInfo();
        synchronized (this) {
            update("INSERT OR REPLACE INTO `INFO`(`key`, `value`, `date_updated`) VALUES(?, ?, ?)", (ThrowingConsumer<PreparedStatement>) stmt -> {
                stmt.setString(1, key.toLowerCase());
                stmt.setString(2, value);
                stmt.setLong(3, System.currentTimeMillis());
            });
            info.put(key.toLowerCase(), value);
        }
    }

    private synchronized void initInfo() {
        if (info == null) {
            ConcurrentHashMap<String, String> tmp = new ConcurrentHashMap<>();
            try (PreparedStatement stmt = prepareQuery("select * FROM INFO")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String key = rs.getString("key");
                        String value = rs.getString("value");
                        tmp.put(key, value);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            this.info = tmp;
        }
    }

    public Set<Integer> getAllies() {
        return getAllies(false);
    }

    public enum UnmaskedReason {
        NOT_REGISTERED,
        NOT_IN_ALLIANCE,
        APPLICANT,
        INACTIVE
    }

    public Map<Member, UnmaskedReason> getMaskedNonMembers() {
        if (!hasAlliance()) return Collections.emptyMap();

        List<Role> roles = new ArrayList<>();
        roles.add(Roles.MEMBER.toRole(this));
        roles.removeIf(Objects::isNull);

        Map<Member, UnmaskedReason> result = new HashMap<>();

        Set<Integer> allowedAAs = new HashSet<>(getAllianceIds());
        for (Role role : roles) {
            List<Member> members = guild.getMembersWithRoles(role);
            for (Member member : members) {
                DBNation nation = DiscordUtil.getNation(member.getUser());
                if (nation == null) result.put(member, UnmaskedReason.NOT_REGISTERED);
                else if (!allowedAAs.contains(nation.getAlliance_id())) result.put(member, UnmaskedReason.NOT_IN_ALLIANCE);
                else if (nation.getPosition() <= 1) result.put(member, UnmaskedReason.APPLICANT);
                else if (nation.active_m() > 20000) result.put(member, UnmaskedReason.INACTIVE);
            }
        }
        return result;
    }

    public Set<Integer> getAllies(boolean fetchTreaties) {
        Set<Integer> allies = new HashSet<>(getCoalition(Coalition.ALLIES));
        Set<Integer> aaIds = getAllianceIds();
        if (!aaIds.isEmpty()) {
            allies.addAll(aaIds);
            if (fetchTreaties) {
                for (int allianceId : aaIds) {
                    Map<Integer, Treaty> treaties = Locutus.imp().getNationDB().getTreaties(allianceId);
                    for (Map.Entry<Integer, Treaty> entry : treaties.entrySet()) {
                        if (entry.getValue().getType().isDefensive()) {
                            allies.add(entry.getKey());
                        }
                    }
                }
            }
        }
        return allies;
    }

    private Map<String, Set<Integer>> coalitionToAlliances(Map<String, Set<Long>> input) {
        Map<String, Set<Integer>> result = new HashMap<>();
        for (Map.Entry<String, Set<Long>> entry : input.entrySet()) {
            Set<Integer> aaIds = new LinkedHashSet<>();
            for (Long id : entry.getValue()) {
                if (id > Integer.MAX_VALUE) {
                    GuildDB db = Locutus.imp().getGuildDB(id);
                    if (db != null) {
                        aaIds.addAll(db.getAllianceIds());
                    }
                } else {
                    aaIds.add(id.intValue());
                }
            }
            result.put(entry.getKey(), aaIds);
        }
        return result;
    }

    private Map<Long, Set<Long>> coalitions2 = null;
    private Map<Long, String> coalitionId2Name = null;
    private Map<String, Long> coalitionName2Id = null;
    private void loadCoalitions() {
        if (coalitions2 == null) {
            synchronized (this) {
                if (coalitions2 == null) {
                    coalitions2 = new ConcurrentHashMap<>();
                    coalitionId2Name = new ConcurrentHashMap<>();
                    coalitionName2Id = new ConcurrentHashMap<>();

                    try (PreparedStatement stmt = prepareQuery("select * FROM COALITIONS")) {
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                long allianceId = rs.getLong("alliance_id");
                                String coalition = rs.getString("coalition").toLowerCase(Locale.ROOT);
                                long id = StringMan.hash(coalition);

                                coalitionId2Name.put(id, coalition);
                                coalitionName2Id.put(coalition, id);
                                coalitions2.computeIfAbsent(id, k -> ConcurrentHashMap.newKeySet()).add(allianceId);
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void addCoalition(long allianceId, Coalition coalition) {
        addCoalition(allianceId, coalition.getNameLower(), coalition);
    }

    public void addCoalition(long allianceId, String coalition) {
        addCoalition(allianceId, coalition.toLowerCase(Locale.ROOT), null);
    }

    private void addCoalition(long allianceId, String coalition, Coalition coalitionParsed) {
        GuildDB faServer = getOrNull(GuildKey.FA_SERVER);
        if (faServer != null && faServer.getIdLong() != getIdLong()) {
            faServer.addCoalition(allianceId, coalition, coalitionParsed);
            return;
        }
        loadCoalitions();
        if (coalitionParsed == null) coalitionParsed = getCoalitionEnumOrNull(coalition);
        Object lock = getLock(coalitionParsed);
        synchronized (lock) {
            long hash = coalitionName2Id.computeIfAbsent(coalition, StringMan::hash);
            coalitions2.computeIfAbsent(hash, k -> ConcurrentHashMap.newKeySet()).add(allianceId);
            coalitionId2Name.put(hash, coalition);

            update("INSERT OR IGNORE INTO `COALITIONS`(`alliance_id`, `coalition`, `date_updated`) VALUES(?, ?, ?)", (ThrowingConsumer<PreparedStatement>) stmt -> {
                stmt.setLong(1, allianceId);
                stmt.setString(2, coalition);
                stmt.setLong(3, System.currentTimeMillis());
            });
        }
    }

    public void removeCoalition(long allianceId, Coalition coalition) {
        removeCoalition(allianceId, coalition.getNameLower(), coalition);
    }

    public void removeCoalition(long allianceId, String coalition) {
        removeCoalition(allianceId, coalition.toLowerCase(Locale.ROOT), null);
    }

    private void removeCoalition(long allianceId, String coalition, Coalition coalitionParsed) {
        GuildDB faServer = getOrNull(GuildKey.FA_SERVER);
        if (faServer != null && faServer.getIdLong() != getIdLong()) {
            faServer.removeCoalition(allianceId, coalition);
            return;
        }
        loadCoalitions();
        if (coalitionParsed == null) coalitionParsed = getCoalitionEnumOrNull(coalition);
        Object lock = getLock(coalitionParsed);
        synchronized (lock) {
            Long hash = coalitionName2Id.get(coalition);
            if (hash != null) {
                Set<Long> set = coalitions2.get(hash);
                if (set != null) {
                    set.remove(allianceId);
                    update("DELETE FROM `COALITIONS` WHERE `alliance_id` = ? AND LOWER(coalition) = LOWER(?)", (ThrowingConsumer<PreparedStatement>) stmt -> {
                        stmt.setLong(1, allianceId);
                        stmt.setString(2, coalition.toLowerCase());
                    });
                }
            }
        }
    }

    public Set<Long> removeCoalition(String coalition) {
        GuildDB faServer = getOrNull(GuildKey.FA_SERVER);
        if (faServer != null && faServer.getIdLong() != getIdLong()) {
            return faServer.removeCoalition(coalition);
        }
        String coalitionLower = coalition.toLowerCase(Locale.ROOT);
        loadCoalitions();
        Coalition coalitionParsed = getCoalitionEnumOrNull(coalitionLower);
        Object lock = getLock(coalitionParsed);

        synchronized (lock) {
            Long hash = coalitionName2Id.remove(coalitionLower);
            if (hash != null) {
                coalitionId2Name.remove(hash);
                logDeletion("COALITIONS", System.currentTimeMillis(), "coalition", coalitionLower);
                update("DELETE FROM `COALITIONS` WHERE LOWER(coalition) = LOWER(?)", (ThrowingConsumer<PreparedStatement>) stmt -> {
                    stmt.setString(1, coalitionLower);
                });
                return coalitions2.remove(hash);
            }
            return Collections.emptySet();
        }
    }

    private Coalition getCoalitionEnumOrNull(String coalition) {
        try {
            return Coalition.valueOf(coalition.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {}
        return null;
    }

    private Object getLock(Coalition coalitionParsed) {
        return coalitions2;
    }

    public Set<Integer> getCoalition(Coalition coalition) {
        Set<Long> longs = getCoalitionRaw(coalition);
        if (longs.isEmpty()) return Collections.emptySet();
        IntArraySet copy = new IntArraySet(longs.size());
        for (Long l : longs) {
            copy.add(l.intValue());
        }
        return copy;
    }

    public Set<Integer> getCoalition(String coalition) {
        Set<Long> longs = getCoalitionRaw(coalition);
        if (longs.isEmpty()) return new IntArraySet();
        IntArraySet copy = new IntArraySet(longs.size());
        for (Long l : longs) {
            copy.add(l.intValue());
        }
        return copy;
    }

    public Set<Long> getCoalitionById(long id) {
        GuildDB faServer = getOrNull(GuildKey.FA_SERVER);
        if (faServer != null && faServer.getIdLong() != getIdLong()) return faServer.getCoalitionById(id);
        loadCoalitions();
        return coalitions2.getOrDefault(id, Collections.emptySet());
    }

    public Set<Long> getCoalitionRaw(Coalition coalition) {
        GuildDB faServer = getOrNull(GuildKey.FA_SERVER);
        if (faServer != null && faServer.getIdLong() != getIdLong()) return faServer.getCoalitionRaw(coalition);
        loadCoalitions();
        Long hash = coalitionName2Id.get(coalition.getNameLower());
        if (hash == null) {
            return Collections.emptySet();
        }
        return coalitions2.getOrDefault(hash, Collections.emptySet());
    }

    public Set<Long> getCoalitionRaw(String coalition) {
        GuildDB faServer = getOrNull(GuildKey.FA_SERVER);
        if (faServer != null && faServer.getIdLong() != getIdLong()) return faServer.getCoalitionRaw(coalition);
        loadCoalitions();
        Long hash = coalitionName2Id.get(coalition.toLowerCase(Locale.ROOT));
        if (hash == null) {
            return Collections.emptySet();
        }
        return coalitions2.getOrDefault(hash, Collections.emptySet());
    }

    public Long getCoalitionId(String coalition, boolean create) {
        loadCoalitions();
        if (coalition.startsWith("~")) coalition = coalition.substring(1);
        return coalitionName2Id.getOrDefault(coalition.toLowerCase(Locale.ROOT), create ? StringMan.hash(coalition) : null);
    }

    public Set<Long> getTrackedBanks() {
        Set<Long> tracked = new LinkedHashSet<>();
        tracked.add(getGuild().getIdLong());
        tracked.addAll(getCoalitionRaw(Coalition.TRACK_DEPOSITS));
        for (Integer id : getAllianceIds()) tracked.add(id.longValue());
        tracked = DNS.expandCoalition(tracked);
        return tracked;
    }

    public Set<Integer> getAllianceIds() {
        return getAllianceIds(true);
    }

    public boolean isAllianceId(int id) {
        Set<Integer> aaIds = getOrNull(GuildKey.ALLIANCE_ID);
        return aaIds != null && aaIds.contains(id);
    }

    /**
     * @param onlyVerified - If only verified alliances are returned
     * @return the alliance ids associated with the guild
     */
    public Set<Integer> getAllianceIds(boolean onlyVerified) {
        Set<Integer> aaIds = getOrNull(GuildKey.ALLIANCE_ID);
        if (onlyVerified) {
            if (aaIds == null) return Collections.emptySet();
            return aaIds;
        }
        if (aaIds == null) return Collections.emptySet();
        return aaIds;
    }

    public void addRole(Roles locutusRole, Role discordRole, long allianceId) {
        addRole(locutusRole, discordRole.getIdLong(), allianceId);
    }

    public void addRole(Roles locutusRole, long discordRole, long allianceId) {
        loadRoles();
        deleteRole(locutusRole, allianceId, false);
        roleToAccountToDiscord.computeIfAbsent(locutusRole, f -> new ConcurrentHashMap<>()).put(allianceId, discordRole);
        update("INSERT OR REPLACE INTO `ROLES2`(`role`, `alias`, `alliance`, `date_updated`) VALUES(?, ?, ?, ?)", (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setLong(1, locutusRole.getId());
            stmt.setLong(2, discordRole);
            stmt.setLong(3, allianceId);
            stmt.setLong(4, System.currentTimeMillis());
        });
    }
    public void deleteRole(Roles role, long alliance) {
        deleteRole(role, alliance, true);
    }
    public void deleteRole(Roles role, long alliance, boolean updateCache) {
        if (updateCache) {
            loadRoles();
            Map<Long, Long> existing = roleToAccountToDiscord.get(role);
            if (existing != null) {
                existing.remove(alliance);
            }
        }
        synchronized (this) {
            logDeletion("ROLES2", System.currentTimeMillis(), new String[]{"role", "alliance"}, role.getId(), alliance);
            update("DELETE FROM `ROLES2` WHERE `role` = ? AND `alliance` = ?", (ThrowingConsumer<PreparedStatement>) stmt -> {
                stmt.setLong(1, role.getId());
                stmt.setLong(2, alliance);
            });
        }
    }

    public void deleteRole(Roles role) {
        loadRoles();
        Map<Long, Long> removed = roleToAccountToDiscord.remove(role);
        synchronized (this) {
            for (Map.Entry<Long, Long> entry : removed.entrySet()) {
                long allianceId = entry.getKey();
                logDeletion("ROLES2", System.currentTimeMillis(), new String[]{"role", "alliance"}, role.getId(), allianceId);
            }
            update("DELETE FROM `ROLES2` WHERE `role` = ?", (ThrowingConsumer<PreparedStatement>) stmt -> {
                stmt.setLong(1, role.getId());
            });
        }
    }

    public  Map<Roles, Map<Long, Long>> getMappingRaw() {
        loadRoles();
        return Collections.unmodifiableMap(roleToAccountToDiscord);
    }

    public Map<Long, Role> getAccountMapping(Roles role) {
        loadRoles();
        Map<Long, Long> existing = roleToAccountToDiscord.get(role);
        if (existing == null || existing.isEmpty()) return Collections.emptyMap();

        Map<Long, Role> result = new HashMap<>();
        for (Map.Entry<Long, Long> entry : existing.entrySet()) {
            Role discordRole = guild.getRoleById(entry.getValue());
            if (discordRole != null) {
                result.put(entry.getKey(), discordRole);
            }
        }
        return result;
    }

    private void loadRoles() {
        if (cachedRoleAliases) return;
        synchronized (roleToAccountToDiscord) {
            if (cachedRoleAliases) return;
            cachedRoleAliases = true;
            try (PreparedStatement stmt = prepareQuery("select * FROM ROLES2")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        try {
                            Roles role = Roles.getRoleById(rs.getInt("role"));
                            if (role == null) continue;
                            long alias = rs.getLong("alias");
                            long alliance = rs.getLong("alliance");
                            roleToAccountToDiscord.computeIfAbsent(role, f -> new ConcurrentHashMap<>()).put(alliance, alias);
                        } catch (IllegalArgumentException ignore) {
                            ignore.printStackTrace();
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public Map<Long, Role> getRoleMap(Roles role) {
        loadRoles();
        Map<Long, Long> roleIds = roleToAccountToDiscord.get(role);
        if (roleIds == null) return Collections.emptyMap();
        Map<Long, Role> result = new HashMap<>();
        for (Map.Entry<Long, Long> entry : roleIds.entrySet()) {
            Role discordRole = guild.getRoleById(entry.getValue());
            if (discordRole != null) {
                result.put(entry.getKey(), discordRole);
            }
        }
        return result;
    }

    public Role getRole(Roles role, Long allianceOrNull) {
        loadRoles();
        Map<Long, Long> roleIds = roleToAccountToDiscord.get(role);
        if (roleIds == null) return null;
        Long mapping = null;
        if (allianceOrNull != null) {
            mapping = roleIds.get(allianceOrNull);
        }
        if (mapping == null) {
            mapping = roleIds.get(0L);
        }
        if (mapping != null) {
            return guild.getRoleById(mapping);
        }
        return null;
    }

    ///

    @Command
    public TextChannel getNotifcationChannel() {
        TextChannel sendTo = guild.getSystemChannel();
        if (sendTo != null && !sendTo.canTalk()) sendTo = null;
        if (sendTo == null) sendTo = guild.getCommunityUpdatesChannel();
        if (sendTo != null && !sendTo.canTalk()) sendTo = null;
        if (sendTo == null) sendTo = guild.getRulesChannel();
        if (sendTo != null && !sendTo.canTalk()) sendTo = null;
        if (sendTo == null) {
            DefaultGuildChannelUnion df = guild.getDefaultChannel();
            if (df != null && df instanceof TextChannel tc && tc.canTalk()) {
                sendTo = tc;
            }
        }
        return sendTo == null || !sendTo.canTalk() ? null : sendTo;
    }
}
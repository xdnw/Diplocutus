package link.locutus.discord.db;

import link.locutus.discord.Locutus;
import link.locutus.discord.api.ApiKeyPool;
import link.locutus.discord.api.endpoints.DnsApi;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.db.entities.DiscordBan;
import link.locutus.discord.db.entities.DiscordMeta;
import link.locutus.discord.db.handlers.SyncableDatabase;
import link.locutus.discord.event.Event;
import link.locutus.discord.event.nation.NationRegisterEvent;
import link.locutus.discord.pnw.RegisteredUser;
import link.locutus.discord.util.StringMan;
import link.locutus.discord.util.discord.DiscordUtil;
import link.locutus.discord.util.scheduler.ThrowingBiConsumer;
import link.locutus.discord.util.scheduler.ThrowingConsumer;
import link.locutus.discord.util.offshore.EncryptionUtil;
import com.google.api.client.util.Base64;
import com.google.api.client.util.Sets;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.dv8tion.jda.api.entities.User;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class DiscordDB extends DBMainV2 implements SyncableDatabase {

    public DiscordDB() throws SQLException, ClassNotFoundException {
        this("locutus");
    }
    public DiscordDB(String name) throws SQLException, ClassNotFoundException {
        super(name);
        if (tableExists("credentials")) migrateCredentials();
    }

    @Override
    public Set<String> getTablesAllowingDeletion() {
        // all tables in getTablesToSync
        return Set.of("USERS", "CREDENTIALS2", "API_KEYS3");
    }

    @Override
    public Map<String, String> getTablesToSync() {
        return Map.of(
                "USERS", "date_updated",
                "CREDENTIALS2", "date_updated",
                "API_KEYS3", "date_updated"
        );
    }

    @Override
    public void createTables() {
        executeStmt("CREATE TABLE IF NOT EXISTS `USERS` (`nation_id` INT NOT NULL, `discord_id` BIGINT NOT NULL, `discord_name` VARCHAR, `date_updated` BIGINT NOT NULL, PRIMARY KEY(discord_id))");
        executeStmt("CREATE TABLE IF NOT EXISTS `CREDENTIALS2` (`discordid` BIGINT NOT NULL PRIMARY KEY, `user` VARCHAR NOT NULL, `password` VARCHAR NOT NULL, `salt` VARCHAR NOT NULL, `date_updated` BIGINT NOT NULL)");
        executeStmt("CREATE TABLE IF NOT EXISTS `VERIFIED` (`nation_id` INT NOT NULL PRIMARY KEY)");
        executeStmt("CREATE TABLE IF NOT EXISTS `DISCORD_META` (`key` BIGINT NOT NULL, `id` BIGINT NOT NULL, `value` BLOB NOT NULL, PRIMARY KEY(`key`, `id`))");
        executeStmt("CREATE TABLE IF NOT EXISTS `API_KEYS3`(`nation_id` INT NOT NULL PRIMARY KEY, `api_key` BLOB, `date_updated` BIGINT NOT NULL)");
        executeStmt("CREATE TABLE IF NOT EXISTS `DISCORD_BANS`(`user` BIGINT NOT NULL, `server` BIGINT NOT NULL, `date` BIGINT NOT NULL, `reason` VARCHAR, PRIMARY KEY(`user`, `server`))");

        for (String table : new String[]{"USERS", "CREDENTIALS2", "API_KEYS3"}) {
            if (getTableColumns(table).stream().noneMatch(c -> c.equalsIgnoreCase("date_updated"))) {
                executeStmt("ALTER TABLE " + table + " ADD COLUMN date_updated BIGINT NOT NULL DEFAULT " + System.currentTimeMillis(), true);
            }
        }

        setupApiKeys();

        createDeletionsTables();
    }

    public List<DiscordBan> getBans(long userId) {
        try (PreparedStatement stmt = prepareQuery("SELECT * FROM `DISCORD_BANS` WHERE `user` = ?")) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<DiscordBan> bans = new ArrayList<>();
                while (rs.next()) {
                    bans.add(new DiscordBan(rs));
                }
                return bans;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<DiscordBan> getBans() {
        try (PreparedStatement stmt = prepareQuery("SELECT * FROM `DISCORD_BANS`")) {
            try (ResultSet rs = stmt.executeQuery()) {
                List<DiscordBan> bans = new ArrayList<>();
                while (rs.next()) {
                    bans.add(new DiscordBan(rs));
                }
                return bans;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addBans(List<DiscordBan> bans) {
        String query = "INSERT OR REPLACE INTO `DISCORD_BANS`(`user`, `server`, `date`, `reason`) VALUES (?, ?, ?, ?)";
        executeBatch(bans, query, new ThrowingBiConsumer<DiscordBan, PreparedStatement>() {
            @Override
            public void acceptThrows(DiscordBan ban, PreparedStatement stmt) throws Exception {
                stmt.setLong(1, ban.user);
                stmt.setLong(2, ban.server);
                stmt.setLong(3, ban.date);
                stmt.setString(4, ban.reason);
            }
        });
    }

    private void setupApiKeys() {
        initInfo();
    }

    public void addApiKey(int nationId, String key) {
        byte[] keyId = new BigInteger(key.toLowerCase(Locale.ROOT), 16).toByteArray();
        update("INSERT OR REPLACE INTO `API_KEYS3`(`nation_id`, `api_key`, `date_updated`) VALUES(?, ?, ?)", (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setInt(1, nationId);
            stmt.setBytes(2, keyId);
            stmt.setLong(3, System.currentTimeMillis());
        });
    }

    public ApiKeyPool.ApiKey getApiKey(int nationId) {
        String existing = Settings.INSTANCE.API_KEY_POOL.get(nationId);
        if (existing != null) {
            return new ApiKeyPool.ApiKey(nationId, existing);
        }
        try (PreparedStatement stmt = prepareQuery("select * FROM API_KEYS3 WHERE nation_id = ?")) {
            stmt.setInt(1, nationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    byte[] keyId = getBytes(rs, "api_key");
                    if (keyId == null) return null;

                    String key = new BigInteger(keyId).toString(16).toLowerCase(Locale.ROOT);
                    return new ApiKeyPool.ApiKey(nationId, key);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deleteApiKeyPairByNation(int nationId) {
        synchronized (this) {
            logDeletion("API_KEYS3", System.currentTimeMillis(), "nation_id", nationId);
            update("DELETE FROM `API_KEYS3` WHERE nation_id = ? ", (ThrowingConsumer<PreparedStatement>) stmt -> {
                stmt.setLong(1, nationId);
            });
        }
    }

    public void deleteApiKey(String key) {
        update("UPDATE API_KEYS3 SET api_key = NULL, `date_updated` = ? WHERE api_key = ?", (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setLong(1, System.currentTimeMillis());
            stmt.setBytes(2, new BigInteger(key.toLowerCase(Locale.ROOT), 16).toByteArray());

        });
    }

    public Integer getNationFromApiKey(String key) {
        for (Map.Entry<Integer, String> entry : Settings.INSTANCE.API_KEY_POOL.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(key)) {
                return entry.getKey();
            }
        }
        try (PreparedStatement stmt = prepareQuery("select * FROM API_KEYS3 WHERE api_key = ?")) {
            byte[] keyId = new BigInteger(key.toLowerCase(Locale.ROOT), 16).toByteArray();
            stmt.setBytes(1, keyId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("nation_id");
                    if (id > 0) return id;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(StringMan.stripApiKey(e.getMessage()).toLowerCase().replace(key.toLowerCase(), "<redacted>"));
        }
        return null;
    }

    private Map<DiscordMeta, Map<Long, byte[]>> info;



    public ByteBuffer getInfo(DiscordMeta meta, long id) {
        if (info == null) {
            initInfo();
        }
        byte[] bytes = info.getOrDefault(meta, Collections.emptyMap()).get(id);
        return bytes == null ? null : ByteBuffer.wrap(bytes);
    }

    public void setInfo(DiscordMeta meta, long id, byte[] value) {
        checkNotNull(meta);
        checkNotNull(value);
        initInfo();
        synchronized (this) {
            update("INSERT OR REPLACE INTO `DISCORD_META`(`key`, `id`, `value`) VALUES(?, ?, ?)", (ThrowingConsumer<PreparedStatement>) stmt -> {
                stmt.setInt(1, meta.ordinal());
                stmt.setLong(2, id);
                stmt.setBytes(3, value);

            });
            info.computeIfAbsent(meta, f -> new Long2ObjectOpenHashMap<>()).put(id, value);
        }
    }

    public void deleteInfo(DiscordMeta meta) {
        update("DELETE FROM DISCORD_META WHERE key = ?",
                (ThrowingConsumer<PreparedStatement>) stmt -> stmt.setInt(1, meta.ordinal()));
    }

    private synchronized void initInfo() {
//        for (DiscordMeta meta : DiscordMeta.values()) {
//            if (meta.getDeleteBefore() > 0) {
//                String update = "DELETE FROM DISCORD_META WHERE key = ? and id < ?";
//                update(update, new ThrowingConsumer<PreparedStatement>() {
//                    @Override
//                    public void acceptThrows(PreparedStatement stmt) throws Exception {
//                        stmt.setInt(1, meta.ordinal());
//                        stmt.setLong(2, meta.getDeleteBefore());
//                    }
//                });
//            }
//        }

        if (info == null) {
            synchronized (this) {
                if (info == null) {
                    info = new ConcurrentHashMap<>();

                    try (PreparedStatement stmt = prepareQuery("select * FROM DISCORD_META")) {
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                int key = rs.getInt("key");
                                DiscordMeta meta = DiscordMeta.values[key];
                                long id = rs.getLong("id");
                                byte[] data = rs.getBytes("value");

                                info.computeIfAbsent(meta, f -> new Long2ObjectOpenHashMap<>()).put(id, data);
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void addVerified(int nationId) {
        update("INSERT OR IGNORE INTO `VERIFIED` (`nation_id`) VALUES(?)", (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setInt(1, nationId);
        });
    }

    public void logout(long nationId) {
        synchronized (this) {
            logDeletion("CREDENTIALS2", System.currentTimeMillis(), "discordid", nationId);
            update("DELETE FROM `CREDENTIALS2` where `discordid` = ?", (ThrowingConsumer<PreparedStatement>) stmt -> {
                stmt.setLong(1, nationId);
            });
        }
    }

    private void migrateCredentials() {
        Set<Long> ids = new HashSet<>();
        String query = getDb().selectBuilder("credentials").select("discordId").buildQuery();
        query(query, stmt -> {},
                (ThrowingConsumer<ResultSet>) r -> ids.add(r.getLong(1)));
        for (long discordId : ids) {
            Map.Entry<String, String> userPass = getUserPass2(discordId, "credentials", EncryptionUtil.Algorithm.LEGACY);
            addUserPass2(discordId, userPass.getKey(), userPass.getValue());
        }

        executeStmt("DROP TABLE CREDENTIALS");
    }

    public void addUserPass2(long nationId, String user, String password) {
        try {
            String secretStr = Settings.INSTANCE.CLIENT_SECRET;
            if (secretStr == null || secretStr.isEmpty()) secretStr = Settings.INSTANCE.BOT_TOKEN;
            byte[] secret = secretStr.getBytes(StandardCharsets.ISO_8859_1);
            byte[] salt = EncryptionUtil.generateKey();

            byte[] userEnc = EncryptionUtil.encrypt2(EncryptionUtil.encrypt2(user.getBytes(StandardCharsets.ISO_8859_1), secret), salt);
            byte[] passEnc = EncryptionUtil.encrypt2(EncryptionUtil.encrypt2(password.getBytes(StandardCharsets.ISO_8859_1), secret), salt);

            update("INSERT OR REPLACE INTO `CREDENTIALS2` (`discordid`, `user`, `password`, `salt`, `date_updated`) VALUES(?, ?, ?, ?, ?)", (ThrowingConsumer<PreparedStatement>) stmt -> {
                stmt.setLong(1, nationId);
                stmt.setString(2, Base64.encodeBase64String(userEnc));
                stmt.setString(3, Base64.encodeBase64String(passEnc));
                stmt.setString(4, Base64.encodeBase64String(salt));
                stmt.setLong(5, System.currentTimeMillis());
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map.Entry<String, String> getUserPass2(long nationId) {
        return getUserPass2(nationId, "credentials2", EncryptionUtil.Algorithm.DEFAULT);
    }

    public Map.Entry<String, String> getUserPass2(long nationId, String table, EncryptionUtil.Algorithm algorithm) {
        if ((nationId == Settings.INSTANCE.ADMIN_USER_ID || nationId == Settings.INSTANCE.NATION_ID) && nationId > 0 && !Settings.INSTANCE.USERNAME.isEmpty() && !Settings.INSTANCE.PASSWORD.isEmpty()) {
            return Map.entry(Settings.INSTANCE.USERNAME, Settings.INSTANCE.PASSWORD);
        }
        try (PreparedStatement stmt = prepareQuery("select * FROM " + table + " WHERE `discordid` = ?")) {
            stmt.setLong(1, nationId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String secretStr = Settings.INSTANCE.CLIENT_SECRET;
                    if (secretStr == null || secretStr.isEmpty()) secretStr = Settings.INSTANCE.BOT_TOKEN;
                    byte[] secret = secretStr.getBytes(StandardCharsets.ISO_8859_1);
                    byte[] salt = Base64.decodeBase64(rs.getString("salt"));
                    byte[] userEnc = Base64.decodeBase64(rs.getString("user"));
                    byte[] passEnc = Base64.decodeBase64(rs.getString("password"));

                    byte[] userBytes = EncryptionUtil.decrypt2(EncryptionUtil.decrypt2(userEnc, salt, algorithm), secret, algorithm);
                    byte[] passBytes = EncryptionUtil.decrypt2(EncryptionUtil.decrypt2(passEnc, salt, algorithm), secret, algorithm);
                    String user = new String(userBytes, StandardCharsets.ISO_8859_1);
                    String pass = new String(passBytes, StandardCharsets.ISO_8859_1);

                    return new AbstractMap.SimpleEntry<>(user, pass);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Set<Integer> getVerified() {
            HashSet<Integer> set = new HashSet<>();
        try (PreparedStatement stmt = prepareQuery("select * FROM VERIFIED")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int nationId = rs.getInt("nation_id");
                    set.add(nationId);
                }
            }
            return set;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isVerified(int nationId) {
            HashSet<Integer> set = new HashSet<>();
        try (PreparedStatement stmt = prepareQuery("select * FROM VERIFIED WHERE nation_id = ?")) {
            stmt.setInt(1, nationId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Map<Long, RegisteredUser> getRegisteredUsers() {
        updateUserCache();
        return userCache;
    }

    public void addUser(RegisteredUser user) {
        if (user.getDiscordId() == Settings.INSTANCE.ADMIN_USER_ID || user.getNationId() == Settings.INSTANCE.NATION_ID) return;
        updateUserCache();
        userCache.put(user.getDiscordId(), user);
        RegisteredUser existing = userNationCache.put(user.getNationId(), user);
        if (existing != null && existing.getDiscordId() == user.getDiscordId() && user.getNationId() == existing.getNationId()) return;

        update("INSERT OR REPLACE INTO `USERS`(`nation_id`, `discord_id`, `discord_name`, `date_updated`) VALUES(?, ?, ?, ?)", (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setInt(1, user.getNationId());
            stmt.setLong(2, user.getDiscordId());
            stmt.setString(3, user.getDiscordName());
            stmt.setLong(4, System.currentTimeMillis());
        });
    }

    public Map<Long, RegisteredUser> getCachedUsers() {
        updateUserCache();
        return Collections.unmodifiableMap(userCache);
    }

    private List<RegisteredUser> getUsersRaw() {
        Set<Integer> nationsToDelete = new HashSet<>();
        ArrayList<RegisteredUser> list = new ArrayList<>();
        try (PreparedStatement stmt = prepareQuery("select * FROM USERS")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int nationId = rs.getInt("nation_id");
                    Long discordId = getLong(rs, "discord_id");
                    if (discordId == null) {
                        nationsToDelete.add(nationId);
                        continue;
                    }
                    String name = rs.getString("discord_name");
                    list.add(new RegisteredUser(nationId, discordId, name));
                }
            }
            if (!nationsToDelete.isEmpty()) {
                for (int id : nationsToDelete) {
                    unregister(id, null);
                }
            }
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public RegisteredUser getUserFromNationId(int nationId) {
        if (nationId == Settings.INSTANCE.NATION_ID && nationId > 0 && Settings.INSTANCE.ADMIN_USER_ID > 0) {
            long userId = Settings.INSTANCE.ADMIN_USER_ID;
            User user = Locutus.imp().getDiscordApi().getUserById(userId);
            return new RegisteredUser(nationId, Settings.INSTANCE.ADMIN_USER_ID, user == null ? null : DiscordUtil.getFullUsername(user));
        }
        updateUserCache();
        return userNationCache.get(nationId);
    }

    public RegisteredUser getUserFromDiscordId(long discordId) {
        if (discordId == Settings.INSTANCE.ADMIN_USER_ID && Settings.INSTANCE.NATION_ID > 0 && discordId > 0) {
            User user = Locutus.imp().getDiscordApi().getUserById(discordId);
            return new RegisteredUser(Settings.INSTANCE.NATION_ID, Settings.INSTANCE.ADMIN_USER_ID, user == null ? null : DiscordUtil.getFullUsername(user));
        }
        updateUserCache();
        return userCache.get(discordId);
    }

    private ConcurrentHashMap<Long, RegisteredUser> userCache = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, RegisteredUser> userNationCache = new ConcurrentHashMap<>();

    public void updateUserCache() {
        if (!userCache.isEmpty()) return;
        synchronized (this) {
            if (!userCache.isEmpty()) return;
            List<RegisteredUser> users = getUsersRaw();
            for (RegisteredUser user : users) {
                long id = user.getDiscordId();
                userCache.put(user.getDiscordId(), user);
                RegisteredUser existing = userNationCache.put(user.getNationId(), user);
                if (existing != null && existing.getDiscordId() != id) {
                    if (existing.getDiscordId() > id) {
                        userNationCache.put(user.getNationId(), existing);
//                        unregister(null, id);
                    } else {
//                        unregister(null, existing.getDiscordId());
                    }
                }
            }
        }
    }

    public RegisteredUser getUser(User user) {
        return getCachedUsers().get(user.getIdLong());
//        return getUser(user.getIdLong(), user.getName(), DiscordUtil.getFullUsername(user));
//        return getUser(user.getIdLong(), null, null);
    }

    public RegisteredUser getUser(Long discordId, String name, String nameWithDesc) {
        Map<Long, RegisteredUser> cached = getCachedUsers();
        if (discordId != null) {
            return cached.get(discordId);
        }
        List<RegisteredUser> secondary = null;
        for (Map.Entry<Long, RegisteredUser> entry : cached.entrySet()) {
            RegisteredUser user = entry.getValue();

            if (nameWithDesc != null && nameWithDesc.equalsIgnoreCase(user.getDiscordName())) {
                user.setDiscordId(user.getDiscordId());
                return user;
            }
            if (name != null && user.getDiscordName() != null) {
                if (user.getDiscordName().contains("#")) {
                    if (user.getDiscordName().startsWith(name + "#")) {
                        if (secondary == null) secondary = new ArrayList<>();
                        secondary.add(user);
                    }
                } else if (name.equalsIgnoreCase(user.getDiscordName())) {
                    if (secondary == null) secondary = new ArrayList<>();
                    secondary.add(user);
                }
            }
        }
        if (secondary != null && secondary.size() == 1) {
            return secondary.get(0);
        }
        return null;
    }

    public void unregister(Integer nationId, Long discordId) {
        if (nationId == null && discordId == null) throw new IllegalArgumentException("A nation id or discord id must be provided");
        if (discordId != null) userCache.remove(discordId);
        if (nationId != null) {
            RegisteredUser user = userNationCache.remove(nationId);
            if (user != null) {
                userCache.remove(discordId = user.getDiscordId());
            }
        }
        if (discordId != null) {
            synchronized (this) {
                logDeletion("USERS", System.currentTimeMillis(), "discord_id", discordId);
                Long finalDiscordId = discordId;
                update("DELETE FROM `USERS` WHERE discord_id = ?", (ThrowingConsumer<PreparedStatement>) stmt -> {
                    stmt.setLong(1, finalDiscordId == null ? -1 : finalDiscordId);
                });
            }
        }

    }
}

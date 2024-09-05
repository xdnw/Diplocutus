package link.locutus.discord.db;

import com.ptsmods.mysqlw.Database;
import link.locutus.discord.Locutus;
import link.locutus.discord.api.endpoints.DnsApi;
import link.locutus.discord.api.endpoints.DnsQuery;
import link.locutus.discord.api.types.tx.BankTransfer;
import link.locutus.discord.api.types.tx.Transaction2;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.DBEntity;
import link.locutus.discord.util.scheduler.ThrowingBiConsumer;
import link.locutus.discord.util.scheduler.ThrowingConsumer;
import link.locutus.discord.util.scheduler.ThrowingFunction;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class DBMainV2 implements Closeable {
    private final File file;
    private boolean isDelegate;
    private final Database db;

    public DBMainV2(String name) throws SQLException {
        this(Settings.INSTANCE.DATABASE, name);
    }

    public DBMainV2(Settings.DATABASE config, String name) throws SQLException {
        if (config.SQLITE.USE) {
            this.file = new File(config.SQLITE.DIRECTORY + File.separator + name + ".db");
            // create file directory if not exist
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            this.db = Database.connect(file);
        } else {
            throw new IllegalArgumentException("Either SQLite OR MySQL must be enabled. (not both, or none)");
        }
        init();
    }

    public <U, T extends DBEntity<U, T>> void updateRecords(T dummy, DBAlliance alliance, Function<DnsApi, DnsQuery<U>> query) {
        DnsApi api = alliance.getApi(true);
        if (api == null) return;
        List<U> fetched = query.apply(api).call();
        List<T> toSave = new ArrayList<>();
        for (U elem : fetched) {
            T record = dummy.emptyInstance();
            record.update(elem, null);
            toSave.add(record);
        }
        save(toSave);
    }

    public <U, T extends DBEntity<U, T>> void updateRecords(T dummy, DBAlliance alliance, BiFunction<DnsApi, Integer, DnsQuery<U>> query, int nationId) {
        updateRecords(dummy, alliance, dnsApi -> query.apply(dnsApi, nationId));
    }

    public <T extends DBEntity<?, T>> List<T> select(T dummy, String whereClause, ThrowingConsumer<PreparedStatement> accept) {
        String tableName = dummy.getTableName();
        String query = "SELECT * FROM " + tableName + (whereClause.isEmpty() ? "" : " WHERE " + whereClause);
        List<T> list = new ArrayList<>();
        ThrowingFunction<ResultSet, T> loader = dummy.loader();
        query(query, accept, (ThrowingConsumer<ResultSet>) rs -> {
            while (rs.next()) {
                list.add(loader.apply(rs));
            }
        });
        return list;
    }

    public <T extends DBEntity<?, T>> List<T> loadAll(T dummyInstance) {
        T empty = dummyInstance.emptyInstance();
        String query = "SELECT * FROM " + empty.getTableName();
        List<T> list = new ArrayList<>();
        ThrowingFunction<ResultSet, T> loader = empty.loader();
        query(query, f -> {}, (ThrowingConsumer<ResultSet>) rs -> {
            while (rs.next()) {
                list.add(loader.apply(rs));
            }
        });
        return list;
    }

    public <T extends DBEntity<?, T>> void save(List<T> entities) {
        if (entities.isEmpty()) return;
        T first = entities.get(0);
        String insert = first.insert(true);
        executeBatch(entities, insert,
                (ThrowingBiConsumer<T, PreparedStatement>) (entity, stmt) -> {
                    Object[] data = entity.write();
                    for (int i = 0; i < data.length; i++) {
                        SQLUtil.set(stmt, i + 1, data[i]);
                    }
                });
    }

    public File getFile() {
        return file;
    }

    public long getLastModified() {
        File file = this.getFile();
        return file == null ? 0 : file.lastModified();
    }

    public DBMainV2(File file) throws SQLException {
        this.file = file;
        this.db = Database.connect(file);
        init();
    }

//    public DBMainV2(String host, int port, String name, String username, String password) throws SQLException {
//        this.db = Database.connect(host, port, name, username, password);
//        init();
//    }

    public DBMainV2(Database other) {
        this.file = null;
        this.db = other;
        this.isDelegate = true;
        init();
    }

    private void init() {
        // Do any initialization here

        createTables();
    }

    public boolean tableExists(String tableName) throws SQLException {
        DatabaseMetaData meta = getConnection().getMetaData();
        try (ResultSet resultSet = meta.getTables(null, null, tableName, new String[] {"TABLE"})) {

            return resultSet.next();
        }
    }

    protected void createTables() {

    }

    public Database getDb() {
        return db;
    }

    @Override
    public void close() throws IOException {
        try {
            db.getConnection().close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * deprecated
     */


    protected int getIntDef0(ResultSet rs, String id) throws SQLException {
        return rs.getInt(id);
    }

    public static int getIntDef0(ResultSet rs, int id) throws SQLException {
        return rs.getInt(id);
    }

    protected long getLongDef0(ResultSet rs, String id) throws SQLException {
        return rs.getLong(id);
    }

    public static long getLongDef0(ResultSet rs, int id) throws SQLException {
        return rs.getLong(id);
    }

    protected Integer getInt(ResultSet rs, String id) throws SQLException {
        int val = rs.getInt(id);
        if (rs.wasNull()) {
            return null;
        }
        return val;
    }

    protected Integer getInt(ResultSet rs, int id) throws SQLException {
        int val = rs.getInt(id);
        if (rs.wasNull()) {
            return null;
        }
        return val;
    }

    protected Long getLong(ResultSet rs, int id) throws SQLException {
        long val = rs.getLong(id);
        if (rs.wasNull()) {
            return null;
        }
        return val;
    }

    protected Long getLong(ResultSet rs, String id) throws SQLException {
        long val = rs.getLong(id);
        if (rs.wasNull()) {
            return null;
        }
        return val;
    }

    protected byte[] getBytes(ResultSet rs, String id) throws SQLException {
        byte[] val = rs.getBytes(id);
        if (rs.wasNull()) {
            return null;
        }
        return val;
    }
    public static byte[] getBytes(ResultSet rs, int id) throws SQLException {
        byte[] val = rs.getBytes(id);
        if (rs.wasNull()) {
            return null;
        }
        return val;
    }


    protected synchronized PreparedStatement prepareStatement(String sql) throws SQLException {
        PreparedStatement stmt = getConnection().prepareStatement(sql);
        stmt.setFetchSize(10000);
        return stmt;
    }

    public PreparedStatement prepareQuery(String sql) throws SQLException {
        PreparedStatement stmt = getConnection().prepareStatement(sql);
        stmt.setFetchSize(10000);
        return stmt;
    }

    public synchronized void executeStmt(String query) {
        System.out.println("Executing " + query);
        executeStmt(query, false);
    }

    public synchronized void executeStmt(String query, boolean ignoreError) {
        try (Statement stmt = getConnection().createStatement()) {
            stmt.addBatch(query);
            stmt.executeBatch();
            stmt.clearBatch();
        } catch (SQLException e) {
            if (!ignoreError) e.printStackTrace();
        }
    }

    public <T> int[] executeBatch(Collection<T> objects, String query, BiConsumer<T, PreparedStatement> consumer) {
        if (objects.isEmpty()) return new int[0];
        synchronized (this) {
            return SQLUtil.executeBatch(getConnection(), objects, query, consumer);
        }
    }

    public <T> T select(String sql, Consumer<PreparedStatement> withStmt, Function<ResultSet, T> rsq) {
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setFetchSize(10000);
            withStmt.accept(stmt);
            ResultSet rs = stmt.executeQuery();
            return rsq.apply(rs);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public boolean query(String sql, Consumer<PreparedStatement> withStmt, Consumer<ResultSet> rsq) {
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setFetchSize(10000);
            withStmt.accept(stmt);
            ResultSet rs = stmt.executeQuery();
            rsq.accept(rs);
            return rs != null;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    protected boolean prepareStatement(String sql, Consumer<PreparedStatement> withStmt) {
        synchronized (this) {
            try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
                stmt.setFetchSize(10000);
                withStmt.accept(stmt);
                return stmt.execute();
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    protected int update(String sql, Object... values) {
        synchronized (this) {
            try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
                for (int i = 0; i < values.length; i++) {
                    stmt.setObject(i + 1, values[i]);
                }
                return stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }
    public int update(String sql, Consumer<PreparedStatement> withStmt) {
        synchronized (this) {
            try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
                withStmt.accept(stmt);
                return stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Gets the connection with the database
     *
     * @return Connection with the database, null if none
     */
    public Connection getConnection() {
        return getDb().getConnection();
    }
}

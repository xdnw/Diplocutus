package link.locutus.discord.db.conflict;

import com.google.common.eventbus.Subscribe;
import com.ptsmods.mysqlw.Database;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import link.locutus.discord.Locutus;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.WarDB;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.db.entities.DBWar;
import link.locutus.discord.db.entities.conflict.ConflictCategory;
import link.locutus.discord.db.entities.conflict.ConflictMetric;
import link.locutus.discord.db.handlers.AttackQuery;
import link.locutus.discord.event.game.HourChangeTask;
import link.locutus.discord.util.MathMan;
import link.locutus.discord.util.StringMan;
import link.locutus.discord.util.TimeUtil;
import link.locutus.discord.util.scheduler.ThrowingBiConsumer;
import link.locutus.discord.util.scheduler.ThrowingConsumer;
import link.locutus.discord.web.AwsManager;
import link.locutus.discord.web.JteUtil;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ConflictManager {
    private final WarDB db;
    private final AwsManager aws;
    private boolean conflictsLoaded = false;

    private final Map<Integer, Conflict> conflictById = new Int2ObjectOpenHashMap<>();
    private Conflict[] conflictArr;
    private final Map<Integer, String> legacyNames2 = new Int2ObjectOpenHashMap<>();
    private final Map<String, Map<Long, Integer>> legacyIdsByDate = new ConcurrentHashMap<>();
    private final Set<Integer> activeConflictsOrd = new IntOpenHashSet();
    private long lastTurn = 0;
    private final Map<Integer, Set<Integer>> activeConflictOrdByAllianceId = new Int2ObjectOpenHashMap<>();
    private final Map<Long, Map<Integer, int[]>> mapTurnAllianceConflictOrd = new Long2ObjectOpenHashMap<>();

    public ConflictManager(WarDB db) {
        this.db = db;
        this.aws = setupAws();
    }

    private AwsManager setupAws() {
        String key = Settings.INSTANCE.WEB.S3.ACCESS_KEY;
        String secret = Settings.INSTANCE.WEB.S3.SECRET_ACCESS_KEY;
        String region = Settings.INSTANCE.WEB.S3.REGION;
        String bucket = Settings.INSTANCE.WEB.S3.BUCKET;
        if (!key.isEmpty() && !secret.isEmpty() && !region.isEmpty() && !bucket.isEmpty()) {
            return new AwsManager(key, secret, bucket, region);
        }
        return null;
    }

    public void createTables() {
        db.executeStmt("CREATE TABLE IF NOT EXISTS conflicts (id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR NOT NULL, start BIGINT NOT NULL, end BIGINT NOT NULL, col1 VARCHAR NOT NULL, col2 VARCHAR NOT NULL, wiki VARCHAR NOT NULL, cb VARCHAR NOT NULL, status VARCHAR NOT NULL, category INTEGER NOT NULL, creator BIGINT NOT NULL)");
        db.executeStmt("ALTER TABLE conflicts ADD COLUMN creator BIGINT DEFAULT 0", true);
        db.executeStmt("ALTER TABLE conflicts ADD COLUMN wiki VARCHAR DEFAULT ''", true);
        db.executeStmt("ALTER TABLE conflicts ADD COLUMN cb VARCHAR DEFAULT ''", true);
        db.executeStmt("ALTER TABLE conflicts ADD COLUMN status VARCHAR DEFAULT ''", true);
        db.executeStmt("ALTER TABLE conflicts ADD COLUMN category INTEGER DEFAULT 0", true);

        db.executeStmt("CREATE TABLE IF NOT EXISTS conflict_participant (conflict_id INTEGER NOT NULL, alliance_id INTEGER NOT NULL, side BOOLEAN, start BIGINT NOT NULL, end BIGINT NOT NULL, PRIMARY KEY (conflict_id, alliance_id), FOREIGN KEY(conflict_id) REFERENCES conflicts(id))");
        db.executeStmt("CREATE TABLE IF NOT EXISTS legacy_names2 (id INTEGER NOT NULL, name VARCHAR NOT NULL, date BIGINT DEFAULT 0, PRIMARY KEY (id, name, date))");

        db.executeStmt("CREATE TABLE IF NOT EXISTS conflict_graphs2 (conflict_id INTEGER NOT NULL, side BOOLEAN NOT NULL, alliance_id INT NOT NULL, metric INTEGER NOT NULL, turn BIGINT NOT NULL, city INTEGER NOT NULL, value INTEGER NOT NULL, PRIMARY KEY (conflict_id, alliance_id, metric, turn, city), FOREIGN KEY(conflict_id) REFERENCES conflicts(id))");

        db.executeStmt("CREATE TABLE IF NOT EXISTS source_sets (guild BIGINT NOT NULL, source_id BIGINT NOT NULL, source_type INT NOT NULL, PRIMARY KEY (guild, source_id, source_type))");
    }

    private synchronized void importData(Database sourceDb, Database targetDb, String tableName) throws SQLException {
        Connection sourceConnection = sourceDb.getConnection();
        Connection targetConnection = targetDb.getConnection();

        targetConnection.setAutoCommit(false);

        try (Statement sourceStatement = sourceConnection.createStatement();
             ResultSet resultSet = sourceStatement.executeQuery("SELECT * FROM " + tableName)) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            String placeholders = String.join(", ", Collections.nCopies(columnCount, "?"));
            String targetSql = "INSERT INTO " + tableName + " VALUES (" + placeholders + ")";

            try (PreparedStatement targetStatement = targetConnection.prepareStatement(targetSql)) {
                while (resultSet.next()) {
                    for (int i = 1; i <= columnCount; i++) {
                        targetStatement.setObject(i, resultSet.getObject(i));
                    }
                    targetStatement.executeUpdate();
                }
            }
            targetConnection.commit();
        } catch (SQLException e) {
            targetConnection.rollback();
            throw e;
        } finally {
            targetConnection.setAutoCommit(true);
        }
    }

    public void importFromExternal(File file) throws SQLException {
        Database otherDb = Database.connect(file);
        List<String> tables = Arrays.asList(
            "conflicts",
                "conflict_participant",
//                "conflict_graphs2",
                "legacy_names2",
                "source_sets"
//                "attack_subtypes"
        );
        // clear conflict_graphs2 and attack_subtypes
        db.executeStmt("DELETE FROM conflict_graphs2");
        db.executeStmt("DELETE FROM attack_subtypes");
        for (String table : tables) {
            // clear all rows of table
            db.executeStmt("DELETE FROM " + table);
            importData(otherDb, db.getDb(), table);
        }
    }

    public String pushIndex() {
        String key = "conflicts/index.gzip";
        byte[] value = getPsonGzip();
        aws.putObject(key, value,  60);
        return aws.getLink(key);
    }

    public boolean pushDirtyConflicts() {
        boolean hasDirty = false;
        for (Conflict conflict : getActiveConflicts()) {
            if (conflict.isDirty()) {
                conflict.push(this, null, false);
                hasDirty = true;
            }
        }
        if (hasDirty) {
            pushIndex();
            return true;
        }
        return false;
    }

    private synchronized void initTurn() {
        long currTurn = TimeUtil.getHour();
        if (lastTurn != currTurn) {
            Iterator<Integer> iter = activeConflictsOrd.iterator();
            activeConflictsOrd.removeIf(f -> {
                Conflict conflict = conflictArr[f];
                return (conflict == null || conflict.getEndHour() <= currTurn);
            });
            recreateConflictsByAlliance();
            for (Conflict conflict : conflictArr) {
                long startTurn = Math.max(lastTurn + 1, conflict.getStartHour());
                long endTurn = Math.min(currTurn + 1, conflict.getEndHour());
                addAllianceTurn(conflict, startTurn, endTurn);
            }
            lastTurn = currTurn;
        }
    }

    private void addAllianceTurn(Conflict conflict, long startTurn, long endTurn) {
        if (startTurn >= endTurn) return;
        synchronized (mapTurnAllianceConflictOrd) {
            Set<Integer> aaIds = conflict.getAllianceIds();
            for (long turn = startTurn; turn < endTurn; turn++) {
                Map<Integer, int[]> conflictIdsByAA = mapTurnAllianceConflictOrd.computeIfAbsent(turn, k -> new Int2ObjectOpenHashMap<>());
                for (int aaId : aaIds) {
                    addAllianceTurn(conflict, aaId, turn, conflictIdsByAA);
                }
            }
        }
    }

    private void addAllianceTurn(Conflict conflict, int aaId, long turn,  Map<Integer, int[]> conflictIdsByAA) {
        if (conflict.getStartHour(aaId) > turn) return;
        if (conflict.getEndHour(aaId) <= turn) return;
        int[] currIds = conflictIdsByAA.get(aaId);
        if (currIds == null) {
            currIds = new int[]{conflict.getOrdinal()};
        } else {
            if (Arrays.binarySearch(currIds, conflict.getOrdinal()) >= 0) return;
            int[] newIds = new int[currIds.length + 1];
            System.arraycopy(currIds, 0, newIds, 0, currIds.length);
            newIds[currIds.length] = conflict.getOrdinal();
            Arrays.sort(newIds);
            currIds = newIds;
        }
        conflictIdsByAA.put(aaId, currIds);
    }

    private void addAllianceTurn(Conflict conflict, int aaId, long turn) {
        Map<Integer, int[]> conflictIdsByAA = mapTurnAllianceConflictOrd.computeIfAbsent(turn, k -> new Int2ObjectOpenHashMap<>());
        addAllianceTurn(conflict, aaId, turn, conflictIdsByAA);
    }

    public void clearAllianceCache() {
        synchronized (mapTurnAllianceConflictOrd) {
            mapTurnAllianceConflictOrd.clear();
            lastTurn = 0;
            recreateConflictsByAlliance();
        }
    }


    private boolean applyConflicts(Predicate<Integer> allowed, long turn, int allianceId1, int allianceId2, Consumer<Conflict> conflictConsumer) {
        if (allianceId1 == 0 || allianceId2 == 0) return false;
        synchronized (mapTurnAllianceConflictOrd)
        {
            Map<Integer, int[]> conflictOrdsByAA = mapTurnAllianceConflictOrd.get(turn);
            if (conflictOrdsByAA == null) return false;
            int[] conflictIds1 = conflictOrdsByAA.get(allianceId1);
            int[] conflictIds2 = conflictOrdsByAA.get(allianceId2);
            if (conflictIds1 != null && conflictIds2 != null) {
                if (conflictIds1.length == 1) {
                    int conflictId1 = conflictIds1[0];
                    if (conflictIds2.length == 1) {
                        if (conflictId1 == conflictIds2[0]) {
                            applyConflictConsumer(allowed, conflictIds1[0], conflictConsumer);
                            return true;
                        }
                    } else {
                        boolean result = false;
                        for (int conflictId : conflictIds2) {
                            if (conflictId == conflictId1) {
                                applyConflictConsumer(allowed, conflictId, conflictConsumer);
                                result = true;
                            }
                        }
                        return result;
                    }
                    return false;
                } else if (conflictIds2.length == 1) {
                    int conflictId2 = conflictIds2[0];
                    boolean result = false;
                    for (int conflictId : conflictIds1) {
                        if (conflictId == conflictId2) {
                            applyConflictConsumer(allowed, conflictId, conflictConsumer);
                            result = true;
                        }
                    }
                    return result;
                } else {
                    int i = 0, j = 0;
                    while (i < conflictIds1.length && j < conflictIds2.length) {
                        int id1 = conflictIds1[i];
                        int id2 = conflictIds2[j];
                        if (id1 < id2) {
                            i++;
                        } else if (id1 > id2) {
                            j++;
                        } else {
                            applyConflictConsumer(allowed, id1, conflictConsumer);
                            i++;
                            j++;
                        }
                    }
                }
            }
            return true;
        }
    }

    private void applyConflictConsumer(Predicate<Integer> allowedOrd, int conflictOrd, Consumer<Conflict> conflictConsumer) {
        if (allowedOrd.test(conflictOrd)) {
            Conflict conflict = conflictArr[conflictOrd];
            conflictConsumer.accept(conflict);
        }
    }

    public boolean updateWar(DBWar previous, DBWar current, Predicate<Integer> allowedConflictords) {
        long turn = TimeUtil.getHour(current.getDate());
        if (turn > lastTurn) initTurn();
        return applyConflicts(allowedConflictords, turn, current.getAttacker_aa(), current.getDefender_aa(), f -> f.updateWar(previous, current, turn));
    }

    private void recreateConflictsByAlliance() {
        synchronized (activeConflictOrdByAllianceId) {
            activeConflictOrdByAllianceId.clear();
            for (int ord : activeConflictsOrd) {
                addConflictsByAlliance(conflictArr[ord], false);
            }
        }
    }

    private void addConflictsByAlliance(Conflict conflict, boolean removeOld) {
        if (conflict == null) return;
        synchronized (activeConflictOrdByAllianceId) {
            if (removeOld) {
                Iterator<Map.Entry<Integer, Set<Integer>>> iter = activeConflictOrdByAllianceId.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<Integer, Set<Integer>> entry = iter.next();
                    entry.getValue().remove(conflict.getOrdinal());
                    if (entry.getValue().isEmpty()) {
                        iter.remove();
                    }
                }
                synchronized (mapTurnAllianceConflictOrd) {
                    Iterator<Map.Entry<Long, Map<Integer, int[]>>> iter2 = mapTurnAllianceConflictOrd.entrySet().iterator();
                    while (iter2.hasNext()) {
                        Map.Entry<Long, Map<Integer, int[]>> entry = iter2.next();
                        Iterator<Map.Entry<Integer, int[]>> iter3 = entry.getValue().entrySet().iterator();
                        while (iter3.hasNext()) {
                            Map.Entry<Integer, int[]> entry2 = iter3.next();
                            int[] value = entry2.getValue();
                            if (Arrays.binarySearch(value, conflict.getOrdinal()) >= 0) {
                                int[] newIds = new int[value.length - 1];
                                if (newIds.length == 0) {
                                    iter3.remove();
                                } else {
                                    for (int i = 0, j = 0; i < value.length; i++) {
                                        if (value[i] != conflict.getOrdinal()) {
                                            newIds[j++] = value[i];
                                        }
                                    }
                                    entry2.setValue(newIds);
                                }
                            }
                        }
                        if (entry.getValue().isEmpty()) {
                            iter2.remove();
                        }
                    }
                }
            }
            if (conflict.isActive()) {
                for (int aaId : conflict.getAllianceIds()) {
                    activeConflictOrdByAllianceId.computeIfAbsent(aaId, k -> new IntArraySet()).add(conflict.getOrdinal());
                }
            }
            addAllianceTurn(conflict, conflict.getStartHour(), Math.min(TimeUtil.getHour(), conflict.getEndHour()));
        }
    }

    public void loadConflicts() {
        List<Conflict> conflicts = new ArrayList<>();
        conflictById.clear();
        db.query("SELECT * FROM conflicts", stmt -> {
        }, (ThrowingConsumer<ResultSet>) rs -> {
            int ordinal = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                long startTurn = rs.getLong("start");
                long endTurn = rs.getLong("end");
                long createdByGuild = rs.getLong("creator");
                String wiki = rs.getString("wiki");
                String col1 = rs.getString("col1");
                String col2 = rs.getString("col2");
                ConflictCategory category = ConflictCategory.values[rs.getInt("category")];
                if (col1.isEmpty()) col1 = "Coalition 1";
                if (col2.isEmpty()) col2 = "Coalition 2";
                String cb = rs.getString("cb");
                String status = rs.getString("status");
                Conflict conflict = new Conflict(id, ordinal++, createdByGuild, category, name, col1, col2, wiki, cb, status, startTurn, endTurn);
                conflicts.add(conflict);
                conflictById.put(id, conflict);
            }
        });
        this.conflictArr = conflicts.toArray(new Conflict[0]);

//        db.update("DELETE FROM conflict_participant WHERE alliance_id = 0");
        db.query("SELECT * FROM conflict_participant", stmt -> {
        }, (ThrowingConsumer<ResultSet>) rs -> {
            while (rs.next()) {
                int conflictId = rs.getInt("conflict_id");
                int allianceId = rs.getInt("alliance_id");
                boolean side = rs.getBoolean("side");
                long startTurn = rs.getLong("start");
                long endTurn = rs.getLong("end");
                Conflict conflict = conflictById.get(conflictId);
                if (conflict != null) {
                    conflict.addParticipant(allianceId, side, false, startTurn, endTurn);
                }
            }
        });
        // load legacyNames
        db.query("SELECT * FROM legacy_names2", stmt -> {
        }, (ThrowingConsumer<ResultSet>) rs -> {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                long date = rs.getLong("date");
                legacyNames2.put(id, name);
                String nameLower = name.toLowerCase(Locale.ROOT);
                legacyIdsByDate.computeIfAbsent(nameLower, k -> new Long2IntOpenHashMap()).put(date, id);
            }
        });

        Locutus.imp().getExecutor().submit(() -> {
            loadConflictWars(null, false);
            Locutus.imp().getRepeatingTasks().addTask("Conflict Website", () -> {
                if (!conflictsLoaded) return;
                pushDirtyConflicts();
            }, 1, TimeUnit.MINUTES);
        });
    }

    public void loadVirtualConflict(Conflict conflict, boolean clearBeforeUpdate) {
        if (clearBeforeUpdate) {
            conflict.clearWarData();
        }
        long start = TimeUtil.getTimeFromHour(conflict.getStartHour());
        long end = conflict.getEndHour() == Long.MAX_VALUE ? Long.MAX_VALUE : TimeUtil.getTimeFromHour(conflict.getEndHour() + 96);

        Set<Integer> aaIds = conflict.getAllianceIds();

        AttackQuery query = Locutus.imp().getWarDb().queryAttacks()
                .withWarsForNationOrAlliance(null, aaIds::contains, f -> f.getDate() >= start && f.getDate() <= end);
        Set<DBWar> wars = new ObjectOpenHashSet<>();
        for (DBWar war : query.wars) {
            if (conflict.updateWar(null, war, TimeUtil.getHour(war.getDate()))) {
                wars.add(war);
            }
        }
    }

    public void loadConflictWars(Collection<Conflict> conflicts, boolean clearBeforeUpdate) {
        try {
            initTurn();
            if (clearBeforeUpdate) {
                Collection<Conflict> tmp = conflicts == null ? Arrays.asList(conflictArr) : conflicts;
                for (Conflict conflict : tmp) {
                    conflict.clearWarData();
                }
            }

            long startMs, endMs;
            Predicate<Integer> allowedConflicts;
            if (conflicts != null) {
                long startTurn = Long.MAX_VALUE;
                long endTurn = 0;
                for (Conflict conflict : conflicts) {
                    startTurn = Math.min(startTurn, conflict.getStartHour());
                    endTurn = Math.max(endTurn, conflict.getEndHour());
                }
                if (endTurn == 0) return;
                startMs = TimeUtil.getTimeFromHour(startTurn);
                endMs = endTurn == Long.MAX_VALUE ? Long.MAX_VALUE : TimeUtil.getTimeFromHour(endTurn + 96);

                boolean[] allowedConflictOrdsArr = new boolean[conflictArr.length];
                for (Conflict conflict : conflicts) {
                    allowedConflictOrdsArr[conflict.getOrdinal()] = true;
                }
                allowedConflicts = f -> allowedConflictOrdsArr[f];
            } else {
                startMs = 0;
                endMs = Long.MAX_VALUE;
                allowedConflicts = f -> true;
            }

            Set<DBWar> wars = new ObjectOpenHashSet<>();
            for (DBWar war : this.db.getWars()) {
                if (war.getDate() >= startMs && war.getDate() <= endMs) {
                    if (updateWar(null, war, allowedConflicts)) {
//                        if (war.isActive() && TimeUtil.getTurn(war.getDate()) + 61 < currentTurn) {
//                            System.out.println("INVALID WAR EXPIRED " + war.getWarId() + " | " + war.getDate() + " | " + war.getStatus());
//                        }
                        wars.add(war);
                    }
                }
            }

            if (!wars.isEmpty()) {
                Map<Integer, Byte> subTypes = loadSubTypes();
                Map<Integer, Byte> newSubTypes = new Int2ByteOpenHashMap();
                BiFunction<DBNation, Long, Integer> activityCache = new BiFunction<>() {
                    private Map<Integer, Set<Long>> activity;
                    @Override
                    public Integer apply(DBNation nation, Long dateMs) {
                        if (activity == null) {
                            activity = Locutus.imp().getNationDB().getActivityByDay(startMs - TimeUnit.DAYS.toMillis(10), endMs);
                        }
                        Set<Long> natAct = activity.get(nation.getId());
                        if (natAct == null) return Integer.MAX_VALUE;
                        long currDay = TimeUtil.getDay(dateMs);
                        for (long day = currDay; day >= currDay - 10; day--) {
                            if (natAct.contains(day)) {
                                return (int) (TimeUnit.DAYS.toMinutes((int) (currDay - day)));
                            }
                        }
                        return 20000;
                    }
                };
                if (!newSubTypes.isEmpty()) {
                    saveSubTypes(newSubTypes);
                }
            }
            conflictsLoaded = true;
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

//    private void saveDefaultNames() {
//        Map<String, Integer> legacyIds = getDefaultNames();
//        for (Map.Entry<String, Integer> entry : legacyIds.entrySet()) {
//            addLegacyName(entry.getValue(), entry.getKey());
//        }
//    }

    public void setStatus(int conflictId, String status) {
        db.update("UPDATE conflicts SET status = ? WHERE id = ?", (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setString(1, status);
            stmt.setInt(2, conflictId);
        });
    }

    public void setCb(int conflictId, String cb) {
        db.update("UPDATE conflicts SET cb = ? WHERE id = ?", (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setString(1, cb);
            stmt.setInt(2, conflictId);
        });
    }

    private Map<Integer, Byte> loadSubTypes() {
        Map<Integer, Byte> subTypes = new Int2ByteOpenHashMap();
        db.query("SELECT * FROM attack_subtypes", stmt -> {
        }, (ThrowingConsumer<ResultSet>) rs -> {
            while (rs.next()) {
                subTypes.put(rs.getInt("attack_id"), rs.getByte("subtype"));
            }
        });
        return subTypes;
    }

    public void saveSubTypes(Map<Integer, Byte> subTypes) {
        String query = "INSERT OR REPLACE INTO attack_subtypes (attack_id, subtype) VALUES (?, ?)";
        db.executeBatch(subTypes.entrySet(), query, (ThrowingBiConsumer<Map.Entry<Integer, Byte>, PreparedStatement>) (entry, stmt) -> {
            stmt.setInt(1, entry.getKey());
            stmt.setByte(2, entry.getValue());
        });
    }

    public Map<Long, List<Long>> getSourceSets() {
        Map<Long, List<Long>> sourceSets = new Long2ObjectOpenHashMap<>();
        db.query("SELECT * FROM source_sets", stmt -> {
        }, (ThrowingConsumer<ResultSet>) rs -> {
            while (rs.next()) {
                long guildId = rs.getLong("guild");
                long sourceId = rs.getLong("source_id");
                sourceSets.computeIfAbsent(guildId, f -> new LongArrayList()).add(sourceId);
            }
        });
        return sourceSets;
    }

    private Map<String, List<Object>> getSourceSetStrings(Map<Long, List<Long>> sourceSets) {
        Map<String, List<Object>> sourceSetStrings = new LinkedHashMap<>();
        for (Map.Entry<Long, List<Long>> entry : sourceSets.entrySet()) {
            List<Object> sourceIdList = new ArrayList<>();
            for (Long sourceId : entry.getValue()) {
                if (sourceId > Integer.MAX_VALUE) {
                    sourceIdList.add(String.valueOf(sourceId));
                } else {
                    sourceIdList.add(sourceId);
                }
            }
            sourceSetStrings.put(String.valueOf(entry.getKey()), sourceIdList);
        }
        return sourceSetStrings;
    }

    private Map<String, String> getSourceNames(Set<Long> sourceIds) {
        Map<String, String> sourceNames = new LinkedHashMap<>();
        for (long id : sourceIds) {
            GuildDB guild = Locutus.imp().getGuildDB(id);
            if (guild != null) {
                sourceNames.put(String.valueOf(id), guild.getName());
            }
        }
        return sourceNames;
    }

    /**
     * type 0 = conflict
     * type 1 = guild
     * @param guild
     * @param sourceId
     * @param sourceType
     */
    public void addSource(long guild, long sourceId, int sourceType) {
        db.update("INSERT OR IGNORE INTO source_sets (guild, source_id, source_type) VALUES (?, ?, ?)", (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setLong(1, guild);
            stmt.setLong(2, sourceId);
            stmt.setInt(3, sourceType);
        });
    }

    public void removeSource(long guild, long sourceId, int sourceType) {
        db.update("DELETE FROM source_sets WHERE guild = ? AND source_id = ? AND source_type = ?", (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setLong(1, guild);
            stmt.setLong(2, sourceId);
            stmt.setInt(3, sourceType);
        });
    }

    public void deleteAllSources(long guild) {
        db.update("DELETE FROM source_sets WHERE guild = ?", (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setLong(1, guild);
        });
    }

    public void clearGraphData(ConflictMetric metric, int conflictId, boolean side, long turn) {
        db.update("DELETE FROM conflict_graphs2 WHERE conflict_id = ? AND side = ? AND metric = ? AND turn = ?", (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setInt(1, conflictId);
            stmt.setBoolean(2, side);
            stmt.setInt(3, metric.ordinal());
            stmt.setLong(4, turn);
        });
    }

    public void clearGraphData(Collection<ConflictMetric> metric, int conflictId, boolean side, long turn) {
        if (metric.isEmpty()) return;
        if (metric.size() == 1) {
            clearGraphData(metric.iterator().next(), conflictId, side, turn);
            return;
        }
        db.update("DELETE FROM conflict_graphs2 WHERE conflict_id = ? AND side = ? AND metric IN (" + metric.stream().map(Enum::ordinal).map(String::valueOf).collect(Collectors.joining(",")) + ") AND turn = ?", (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setInt(1, conflictId);
            stmt.setBoolean(2, side);
            stmt.setLong(3, turn);
        });
    }

    public void deleteGraphData(int conflictId) {
        db.update("DELETE FROM conflict_graphs2 WHERE conflict_id = ?", (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setInt(1, conflictId);
        });
    }

    public void addGraphData(List<ConflictMetric.Entry> metrics) {
        String query = "INSERT OR REPLACE INTO conflict_graphs2 (conflict_id, side, alliance_id, metric, turn, city, value) VALUES (?, ?, ?, ?, ?, ?, ?)";
        db.executeBatch(metrics, query, (ThrowingBiConsumer<ConflictMetric.Entry, PreparedStatement>) (entry, stmt) -> {
            stmt.setInt(1, entry.conflictId());
            stmt.setBoolean(2, entry.side());
            stmt.setInt(3, entry.allianceId());
            stmt.setInt(4, entry.metric().ordinal());
            stmt.setLong(5, entry.turnOrDay());
            stmt.setInt(6, entry.city());
            stmt.setInt(7, entry.value());
        });
    }

    public void addLegacyName(int id, String name, long date) {
        String nameLower = name.toLowerCase(Locale.ROOT);
        Map<Long, Integer> byDate = legacyIdsByDate.computeIfAbsent(nameLower, f -> new Long2IntOpenHashMap());
        Integer otherId = null;
        Long lastDate = null;
        for (Map.Entry<Long, Integer> entry : byDate.entrySet()) {
            long otherDate = entry.getKey();
            if (otherDate <= date && (lastDate == null || otherDate > lastDate)) {
                lastDate = otherDate;
                otherId = entry.getValue();
            }
        }
        if (otherId == null || otherId != id) {
            byDate.put(date, id);
            synchronized (legacyNames2) {
                legacyNames2.putIfAbsent(id, name);
            }
            db.update("INSERT OR IGNORE INTO legacy_names2 (id, name, date) VALUES (?, ?, ?)", (ThrowingConsumer<PreparedStatement>) stmt -> {
                stmt.setInt(1, id);
                stmt.setString(2, name);
                stmt.setLong(3, date);
            });
        }
    }

    public Conflict addConflict(String name, long creator, ConflictCategory category, String col1, String col2, String wiki, String cb, String status, long turnStart, long turnEnd) {
        String query = "INSERT INTO conflicts (name, col1, col2, wiki, start, end, category, cb, status, creator) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setString(2, col1);
            stmt.setString(3, col2);
            stmt.setString(4, wiki);
            stmt.setLong(5, turnStart);
            stmt.setLong(6, turnEnd);
            stmt.setInt(7, category.ordinal());
            stmt.setString(8, cb);
            stmt.setString(9, status);
            stmt.setLong(10, creator);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                Conflict conflict = new Conflict(id, conflictArr.length, creator, category, name, col1, col2, wiki, cb, status, turnStart, turnEnd);
                conflictById.put(id, conflict);
                conflictArr = Arrays.copyOf(conflictArr, conflictArr.length + 1);
                conflictArr[conflictArr.length - 1] = conflict;

                synchronized (activeConflictsOrd) {
                    long turn = TimeUtil.getHour();
                    if (turnEnd > turn) {
                        activeConflictsOrd.add(conflict.getOrdinal());
                        addConflictsByAlliance(conflict, false);
                    }
                }

                return conflict;
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected void updateConflict(Conflict conflict, long start, long end) {
        int conflictOrd = conflict.getOrdinal();
        synchronized (activeConflictsOrd) {
            if (activeConflictsOrd.contains(conflictOrd)) {
                if (end <= TimeUtil.getHour()) {
                    activeConflictsOrd.remove(conflictOrd);
                }
            } else if (!activeConflictsOrd.contains(conflictOrd) && end == Long.MAX_VALUE || end > TimeUtil.getHour()) {
                activeConflictsOrd.add(conflictOrd);
            }
            addConflictsByAlliance(conflict, true);
        }
        db.update("UPDATE conflicts SET start = ?, end = ? WHERE id = ?", (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setLong(1, start);
            stmt.setLong(2, end);
            stmt.setInt(3, conflict.getId());
        });
    }

    public void updateConflictName(int conflictId, String name) {
        db.update("UPDATE conflicts SET name = ? WHERE id = ?", (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setString(1, name);
            stmt.setInt(2, conflictId);
        });
    }

    public void updateConflictName(int conflictId, String name, boolean isPrimary) {
        db.update("UPDATE conflicts SET `col" + (isPrimary ? "1" : "2") + "` = ? WHERE id = ?", (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setString(1, name);
            stmt.setInt(2, conflictId);
        });
    }


    public void updateConflictWiki(int conflictId, String wiki) {
        db.update("UPDATE conflicts SET `wiki` = ? WHERE id = ?", (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setString(1, wiki);
            stmt.setInt(2, conflictId);
        });
    }

    protected void addParticipant(Conflict conflict, int allianceId, boolean side, long start, long end) {
        db.update("INSERT OR REPLACE INTO conflict_participant (conflict_id, alliance_id, side, start, end) VALUES (?, ?, ?, ?, ?)", (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setInt(1, conflict.getId());
            stmt.setInt(2, allianceId);
            stmt.setBoolean(3, side);
            stmt.setLong(4, start);
            stmt.setLong(5, end);
        });
        DBAlliance aa = DBAlliance.get(allianceId);
        if (aa != null) addLegacyName(allianceId, aa.getName(), System.currentTimeMillis());
        addConflictsByAlliance(conflict, true);
    }

    protected void removeParticipant(Conflict conflict, int allianceId) {
        db.update("DELETE FROM conflict_participant WHERE alliance_id = ? AND conflict_id = ?", (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setInt(1, allianceId);
            stmt.setInt(2, conflict.getId());
        });
        addConflictsByAlliance(conflict, true);
    }

    public Map<Integer, Conflict> getConflictMap() {
        synchronized (conflictById) {
            return new Int2ObjectOpenHashMap<>(conflictById);
        }
    }

    public List<Conflict> getActiveConflicts() {
        return conflictById.values().stream().filter(conflict -> conflict.getEndHour() == Long.MAX_VALUE).toList();
    }

    public Conflict getConflict(String conflictName) {
        for (Conflict conflict : getConflictMap().values()) {
            if (conflict.getName().equalsIgnoreCase(conflictName)) {
                return conflict;
            }
        }
        return null;
    }

    public Integer getAllianceId(String name, long date, boolean parseInt) {
        Integer id = getAllianceId(name, date);
        if (id == null && parseInt && MathMan.isInteger(name)) {
            id = Integer.parseInt(name);
        }
        return id;
    }

    public Integer getAllianceId(String name, long date) {
        String nameLower = name.toLowerCase(Locale.ROOT);
        synchronized (legacyIdsByDate) {
            Map<Long, Integer> idsByDate = legacyIdsByDate.get(nameLower);
            if (idsByDate != null && !idsByDate.isEmpty()) {
                if (idsByDate.size() == 1) {
                    return idsByDate.values().iterator().next();
                }
                Long lastDate = null;
                Integer lastId = null;
                for (Map.Entry<Long, Integer> entry : idsByDate.entrySet()) {
                    long otherDate = entry.getKey();
                    if (lastDate == null || (otherDate <= date && otherDate > lastDate)) {
                        lastDate = otherDate;
                        lastId = entry.getValue();
                    }
                }
                return lastId;
            }
        }
        DBAlliance alliance = DBAlliance.parse(name, false);
        if (alliance != null) {
            addLegacyName(alliance.getId(), name, 0);
            return alliance.getId();
        }
        return null;
    }

    public String getAllianceName(int id) {
        String name = getAllianceNameOrNull(id);
        if (name == null) name = "AA:" + id;
        return name;
    }

    public String getAllianceNameOrNull(int id) {
        DBAlliance alliance = DBAlliance.get(id);
        if (alliance != null) return alliance.getName();
        String name;
        synchronized (legacyNames2) {
            name = legacyNames2.get(id);
        }
        return name;
    }

    public void deleteConflict(Conflict conflict) {
        synchronized (activeConflictsOrd) {
            synchronized (conflictById) {
                if (conflictById.remove(conflict.getId()) != null) {
                    ArrayList<Conflict> conflictList = new ArrayList<>(Arrays.asList(conflictArr));
                    conflictList.remove(conflict);
                    conflictArr = conflictList.toArray(new Conflict[0]);

                    activeConflictsOrd.remove(conflict.getOrdinal());

                    recreateConflictsByAlliance();
                }
            }
        }
        db.update("DELETE FROM conflicts WHERE id = ?", (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setInt(1, conflict.getId());
        });
        db.update("DELETE FROM conflict_participant WHERE conflict_id = ?", (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setInt(1, conflict.getId());
        });
    }

    public Conflict getConflictById(int id) {
        synchronized (conflictById) {
            return conflictById.get(id);
        }
    }

    public Set<String> getConflictNames() {
        Set<String> names = new ObjectOpenHashSet<>();
        for (Conflict conflict : conflictArr) {
            names.add(conflict.getName());
        }
        return names;
    }

    public byte[] getPsonGzip() {
        Map<String, Object> root = new Object2ObjectLinkedOpenHashMap<>();
        Map<Integer, Conflict> map = getConflictMap();
        Map<Integer, String> aaNameById = new HashMap<>();

        Map<String, Function<Conflict, Object>> headerFuncs = new LinkedHashMap<>();
        headerFuncs.put("id", Conflict::getId);
        headerFuncs.put("name", Conflict::getName);
        headerFuncs.put("c1_name", f -> f.getSide(true).getName());
        headerFuncs.put("c2_name", f -> f.getSide(false).getName());
        headerFuncs.put("start", f -> TimeUtil.getTimeFromHour(f.getStartHour()));
        headerFuncs.put("end", f -> f.getEndHour() == Long.MAX_VALUE ? -1 : TimeUtil.getTimeFromHour(f.getEndHour()));
        headerFuncs.put("wars", Conflict::getTotalWars);
        headerFuncs.put("active_wars", Conflict::getActiveWars);
        headerFuncs.put("c1_dealt", f -> (long) f.getDamageConverted(true));
        headerFuncs.put("c2_dealt", f -> (long) f.getDamageConverted(false));
        headerFuncs.put("c1", f -> new IntArrayList(f.getCoalition1()));
        headerFuncs.put("c2", f -> new IntArrayList(f.getCoalition2()));
        headerFuncs.put("wiki", Conflict::getWiki);
        headerFuncs.put("status", Conflict::getStatusDesc);
        headerFuncs.put("cb", Conflict::getCasusBelli);
        headerFuncs.put("source", Conflict::getGuildId);

        List<String> headers = new ObjectArrayList<>();
        List<Function<Conflict, Object>> funcs = new ObjectArrayList<>();
        for (Map.Entry<String, Function<Conflict, Object>> entry : headerFuncs.entrySet()) {
            headers.add(entry.getKey());
            funcs.add(entry.getValue());
        }
        root.put("headers", headers);

        List<List<Object>> rows = new ObjectArrayList<>();
        JteUtil.writeArray(rows, funcs, map.values());
        root.put("conflicts", rows);

        for (Conflict conflict : map.values()) {
            for (int id : conflict.getAllianceIds()) {
                if (!aaNameById.containsKey(id)) {
                    String name = getAllianceNameOrNull(id);
                    aaNameById.put(id, name == null ? "" : name);
                }
            }
        }
        List<Integer> allianceIds = new ArrayList<>(aaNameById.keySet());
        Collections.sort(allianceIds);
        List<String> aaNames = allianceIds.stream().map(aaNameById::get).toList();
        root.put("alliance_ids", allianceIds);
        root.put("alliance_names", aaNames);

        Map<Long, List<Long>> sourceSets = getSourceSets();
        root.put("source_sets", getSourceSetStrings(sourceSets));
        root.put("source_names", getSourceNames(sourceSets.keySet()));

        return JteUtil.compress(JteUtil.toBinary(root));
    }

    public Map.Entry<String, Double> getMostSimilar(String name) {
        double distance = Integer.MAX_VALUE;
        String similar = null;
        for (Map.Entry<Integer, String> entry : legacyNames2.entrySet()) {
            double d = StringMan.distanceWeightedQwertSift4(name, entry.getValue());
            if (d < distance) {
                distance = d;
                similar = entry.getValue();
            }
        }
        return distance == Integer.MAX_VALUE ? null : Map.entry(similar, distance);
    }

    public void updateConflictCategory(int conflictId, ConflictCategory category) {
        db.update("UPDATE conflicts SET category = ? WHERE id = ?", (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setInt(1, category.ordinal());
            stmt.setInt(2, conflictId);
        });
    }

    public AwsManager getAws() {
        return aws;
    }
}

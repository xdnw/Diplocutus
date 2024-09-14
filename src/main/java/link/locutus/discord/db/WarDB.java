package link.locutus.discord.db;

import com.google.common.collect.Lists;
import com.ptsmods.mysqlw.table.ColumnType;
import com.ptsmods.mysqlw.table.TablePreset;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.io.FastByteArrayOutputStream;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import link.locutus.discord.Locutus;
import link.locutus.discord.Logg;
import link.locutus.discord.api.endpoints.DnsApi;
import link.locutus.discord.api.endpoints.DnsQuery;
import link.locutus.discord.api.generated.AllianceTreaties;
import link.locutus.discord.api.generated.WarHistory;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.conflict.ConflictManager;
import link.locutus.discord.db.entities.*;
import link.locutus.discord.db.entities.Treaty;
import link.locutus.discord.db.handlers.ActiveWarHandler;
import link.locutus.discord.db.handlers.AttackQuery;
import link.locutus.discord.event.Event;
import link.locutus.discord.event.war.WarStatusChangeEvent;
import link.locutus.discord.util.*;
import link.locutus.discord.util.io.PagePriority;
import link.locutus.discord.util.scheduler.ThrowingBiConsumer;
import link.locutus.discord.util.scheduler.ThrowingConsumer;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.math.ArrayUtil;
import link.locutus.discord.util.scheduler.ThrowingFunction;
import link.locutus.discord.util.scheduler.TriConsumer;
import link.locutus.discord.util.update.WarUpdateProcessor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WarDB extends DBMainV2 {


    private final  ActiveWarHandler activeWars = new ActiveWarHandler(this);
    private final ObjectOpenHashSet<DBWar> warsById = new ObjectOpenHashSet<>();
    private final Int2ObjectOpenHashMap<Object> warsByAllianceId = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<Object> warsByNationId = new Int2ObjectOpenHashMap<>();
    private final Object warsByNationLock = new Object();
    private ConflictManager conflictManager;

    public WarDB() throws SQLException {
        this("war");
    }

    public WarDB(String name) throws SQLException {
        super(Settings.INSTANCE.DATABASE, name);
    }

    private void setWar(DBWar war) {
        synchronized (warsByAllianceId) {
            if (war.getAttacker_aa() != 0)
                ArrayUtil.addElement(DBWar.class, warsByAllianceId, war.getAttacker_aa(), war);
            if (war.getDefender_aa() != 0)
                ArrayUtil.addElement(DBWar.class, warsByAllianceId, war.getDefender_aa(), war);
        }
        synchronized (warsByNationLock) {
            ArrayUtil.addElement(DBWar.class, warsByNationId, war.getAttacker_id(), war);
            ArrayUtil.addElement(DBWar.class, warsByNationId, war.getDefender_id(), war);
        }
        synchronized (warsById) {
            warsById.add(war);
        }
    }

    private void setWars(List<DBWar> allWars, boolean clear, boolean sync) {
        if (clear) {
            synchronized (warsById) {
                warsById.clear();
            }
            synchronized (warsByAllianceId) {
                warsByAllianceId.clear();
            }
            synchronized (warsByNationLock) {
                warsByNationId.clear();
            }
        }
        Int2IntOpenHashMap numWarsByAlliance = new Int2IntOpenHashMap();
        Int2IntOpenHashMap numWarsByNation = new Int2IntOpenHashMap();
        for (DBWar war : allWars) {
            if (war.getAttacker_aa() != 0) numWarsByAlliance.addTo(war.getAttacker_aa(), 1);
            if (war.getDefender_aa() != 0) numWarsByAlliance.addTo(war.getDefender_aa(), 1);
            numWarsByNation.addTo(war.getAttacker_id(), 1);
            numWarsByNation.addTo(war.getDefender_id(), 1);
        }
        synchronized (warsById) {
            warsById.addAll(allWars);
        }
        if (sync) {
            synchronized (warsByNationLock) {
                for (DBWar war : allWars) {
                    setWar(war, war.getAttacker_id(), numWarsByNation.get(war.getAttacker_id()), this.warsByNationId);
                    setWar(war, war.getDefender_id(), numWarsByNation.get(war.getDefender_id()), this.warsByNationId);
                }
            }
            synchronized (warsByAllianceId) {
                for (DBWar war : allWars) {
                    if (war.getAttacker_aa() != 0) setWar(war, war.getAttacker_aa(), numWarsByAlliance.get(war.getAttacker_aa()), this.warsByAllianceId);
                    if (war.getDefender_aa() != 0) setWar(war, war.getDefender_aa(), numWarsByAlliance.get(war.getDefender_aa()), this.warsByAllianceId);
                }
            }
        } else {
            for (DBWar war : allWars) {
                if (war.getAttacker_aa() != 0) setWar(war, war.getAttacker_aa(), numWarsByAlliance.get(war.getAttacker_aa()), this.warsByAllianceId);
                if (war.getDefender_aa() != 0) setWar(war, war.getDefender_aa(), numWarsByAlliance.get(war.getDefender_aa()), this.warsByAllianceId);
                setWar(war, war.getAttacker_id(), numWarsByNation.get(war.getAttacker_id()), this.warsByNationId);
                setWar(war, war.getDefender_id(), numWarsByNation.get(war.getDefender_id()), this.warsByNationId);
            }
        }
    }

    private void setWar(DBWar war, int id, int size, Int2ObjectOpenHashMap<Object> map) {
        if (size == 1) {
            map.put(id, war);
        } else {
            Object o = map.get(id);
            if (o instanceof ObjectOpenHashSet set) {
                set.add(war);
            } else if (o == null) {
                ObjectOpenHashSet<Object> set = new ObjectOpenHashSet<>(size);
                set.add(war);
                map.put(id, set);
            } else if (o instanceof DBWar oldWar) {
                throw new IllegalStateException("Multiple wars for " + id + ": " + oldWar + " and " + war);
            } else {
                throw new IllegalStateException("Unknown object for " + id + ": " + o);
            }
        }
    }

    public WarDB load() {
        loadWars();
        if (conflictManager != null) {
            conflictManager.loadConflicts();
        }



        return this;
    }

    public void loadWars() {
        List<DBWar> wars = loadAll(new DBWar());
        for (DBWar war : wars) {
            if (war.isActive()) {
                activeWars.addActiveWar(war);
            }
        }
        if (!wars.isEmpty()) {
            setWars(wars, false, false);
        }
    }

    public Set<DBWar> getWarsForNationOrAlliance(Set<Integer> nations, Set<Integer> alliances) {
        Set<DBWar> result = new ObjectOpenHashSet<>();
        if (alliances != null && !alliances.isEmpty()) {
            synchronized (warsByAllianceId) {
                for (int id : alliances) {
                    Object wars = warsByAllianceId.get(id);
                    if (wars != null) {
                        ArrayUtil.iterateElements(DBWar.class, wars, result::add);
                    }
                }
            }
        }
        if (nations != null && !nations.isEmpty()) {
            synchronized (warsByNationLock) {
                for (int id : nations) {
                    Object wars = warsByNationId.get(id);
                    if (wars != null) {
                        ArrayUtil.iterateElements(DBWar.class, wars, result::add);
                    }
                }
            }
        }
        return result;
    }

    public Map<Integer, DBWar> getWarsForNationOrAlliance(Predicate<Integer> nations, Predicate<Integer> alliances, Predicate<DBWar> warFilter) {
        Map<Integer, DBWar> result = new Int2ObjectOpenHashMap<>();
        if (alliances != null) {
            synchronized (warsByAllianceId) {
                for (Map.Entry<Integer, Object> entry : warsByAllianceId.entrySet()) {
                    if (alliances.test(entry.getKey())) {
                        if (warFilter != null) {
                            ArrayUtil.iterateElements(DBWar.class, entry.getValue(), war -> {
                                if (warFilter.test(war)) {
                                    result.put(war.getWarId(), war);
                                }
                            });
                        } else {
                            ArrayUtil.iterateElements(DBWar.class, entry.getValue(), war -> {
                                result.put(war.getWarId(), war);
                            });
                        }
                    }
                }
            }
        }
        if (nations != null) {
            synchronized (warsByNationLock) {
                for (Map.Entry<Integer, Object> entry : warsByNationId.entrySet()) {
                    if (nations.test(entry.getKey())) {
                        if (warFilter != null) {
                            ArrayUtil.iterateElements(DBWar.class, entry.getValue(), war -> {
                                if (warFilter.test(war)) {
                                    result.put(war.getWarId(), war);
                                }
                            });
                        } else {
                            ArrayUtil.iterateElements(DBWar.class, entry.getValue(), war -> {
                                result.put(war.getWarId(), war);
                            });
                        }
                    }
                }
            }
        }
        else if (alliances == null) {
            synchronized (warsById) {
                if (warFilter == null) {
                    warsById.forEach((war) -> result.put(war.getWarId(), war));
                } else {
                    for (DBWar war : warsById) {
                        if (warFilter.test(war)) {
                            result.put(war.getWarId(), war);
                        }
                    }
                }
            }
        }
        return result;
    }

    public Map<Integer, DBWar> getWars(Predicate<DBWar> filter) {
        return getWarsForNationOrAlliance(null, null, filter);
    }

    @Override
    public void createTables() {
        executeStmt(SQLUtil.createTable(new DBWar()));
        {
            String create = "CREATE TABLE IF NOT EXISTS `COUNTER_STATS` (`id` INT NOT NULL PRIMARY KEY, `type` INT NOT NULL, `active` INT NOT NULL)";
            try (Statement stmt = getConnection().createStatement()) {
                stmt.addBatch(create);
                stmt.executeBatch();
                stmt.clearBatch();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };
        boolean enableConflicts = !Settings.INSTANCE.WEB.S3.ACCESS_KEY.isEmpty() &&
                !Settings.INSTANCE.WEB.S3.SECRET_ACCESS_KEY.isEmpty() &&
                !Settings.INSTANCE.WEB.S3.REGION.isEmpty() &&
                !Settings.INSTANCE.WEB.S3.BUCKET.isEmpty();
        conflictManager = enableConflicts ? new ConflictManager(this) : null;
        if (conflictManager != null) {
            this.conflictManager.createTables();
        }
    }

    public ConflictManager getConflicts() {
        return conflictManager;
    }

    public ObjectOpenHashSet<DBWar> getActiveWars() {
        return activeWars.getActiveWars();
    }

    public Set<DBWar> getActiveWars(int nationId) {
        return activeWars.getActiveWars(nationId);
    }

    public ObjectOpenHashSet<DBWar> getActiveWars(Predicate<Integer> nationId, Predicate<DBWar> warPredicate) {
        return activeWars.getActiveWars(nationId, warPredicate);
    }

    public Map.Entry<Double, Double> getAACounterStats(int allianceId) {
        List<Map.Entry<DBWar, CounterStat>> counters = Locutus.imp().getWarDb().getCounters(Collections.singleton(allianceId));
        if (counters.isEmpty()) {
            for (Map.Entry<Integer, Treaty> entry : Locutus.imp().getNationDB().getTreaties(allianceId).entrySet()) {
                Treaty treaty = entry.getValue();
                if (treaty.getType().isDefensive()) {
                    int other = treaty.getFromId() == allianceId ? treaty.getToId() : treaty.getFromId();
                    counters.addAll(Locutus.imp().getWarDb().getCounters(Collections.singleton(other)));
                }
            }
            if (counters.isEmpty()) return null;
        }

        int[] uncontested = new int[2];
        int[] countered = new int[2];
        int[] counter = new int[2];
        for (Map.Entry<DBWar, CounterStat> entry : counters) {
            CounterStat stat = entry.getValue();
            DBWar war = entry.getKey();
            switch (stat.type) {
                case ESCALATION:
                case IS_COUNTER:
                    countered[stat.isActive ? 1 : 0]++;
                    continue;
                case UNCONTESTED:
                    if (war.getStatus() == WarStatus.ATTACKER_VICTORY) {
                        uncontested[stat.isActive ? 1 : 0]++;
                    } else {
                        counter[stat.isActive ? 1 : 0]++;
                    }
                    break;
                case GETS_COUNTERED:
                    counter[stat.isActive ? 1 : 0]++;
                    break;
            }
        }

        int totalActive = counter[1] + uncontested[1];
        int totalInactive = counter[0] + uncontested[0];

        double chanceActive = ((double) counter[1] + 1) / (totalActive + 1);
        double chanceInactive = ((double) counter[0] + 1) / (totalInactive + 1);

        if (!Double.isFinite(chanceActive)) chanceActive = 0.5;
        if (!Double.isFinite(chanceInactive)) chanceInactive = 0.5;

        return new AbstractMap.SimpleEntry<>(chanceActive, chanceInactive);
    }

    public List<Map.Entry<DBWar, CounterStat>> getCounters(Collection<Integer> alliances) {
        Map<Integer, DBWar> wars = getWarsForNationOrAlliance(null, alliances::contains, f -> alliances.contains(f.getDefender_aa()));
        String queryStr = "SELECT * FROM COUNTER_STATS WHERE id IN " + StringMan.getString(wars.values().stream().map(f -> f.getWarId()).collect(Collectors.toList()));
        try (PreparedStatement stmt= getConnection().prepareStatement(queryStr)) {
            try (ResultSet rs = stmt.executeQuery()) {
                List<Map.Entry<DBWar, CounterStat>> result = new ArrayList<>();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    CounterStat stat = new CounterStat();
                    stat.isActive = rs.getBoolean("active");
                    stat.type = CounterType.values[rs.getInt("type")];
                    DBWar war = getWar(id);
                    AbstractMap.SimpleEntry<DBWar, CounterStat> entry = new AbstractMap.SimpleEntry<>(war, stat);
                    result.add(entry);
                }
                return result;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public CounterStat getCounterStat(DBWar war) {
        try (PreparedStatement stmt= prepareQuery("SELECT * FROM COUNTER_STATS WHERE id = ?")) {
            stmt.setInt(1, war.getWarId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    CounterStat stat = new CounterStat();
                    stat.isActive = rs.getBoolean("active");
                    stat.type = CounterType.values[rs.getInt("type")];
                    return stat;
                }
            }
            return updateCounter(war, f -> Locutus.imp().runEventsAsync(List.of(f)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public CounterStat updateCounter(DBWar war, Consumer<Event> eventConsumer) {
        DBNation attacker = Locutus.imp().getNationDB().getNation(war.getAttacker_id());
        DBNation defender = Locutus.imp().getNationDB().getNation(war.getDefender_id());
        if (war.getAttacker_aa() == 0 || war.getDefender_aa() == 0) {
            CounterStat stat = new CounterStat();
            stat.type = CounterType.UNCONTESTED;
            stat.isActive = defender != null && defender.active_m() < 2880;
            return stat;
        }
        int warId = war.getWarId();

        long startDate = war.getDate();
        long endDate = war.possibleEndDate();

        boolean isOngoing = war.getStatus() == WarStatus.ACTIVE || war.getStatus() == WarStatus.DEFENDER_OFFERED_PEACE || war.getStatus() == WarStatus.ATTACKER_OFFERED_PEACE;
        boolean isActive = war.getStatus() == WarStatus.DEFENDER_OFFERED_PEACE || war.getStatus() == WarStatus.DEFENDER_VICTORY || war.getStatus() == WarStatus.ATTACKER_OFFERED_PEACE;

        Set<Integer> attAA = new HashSet<>(Collections.singleton(war.getAttacker_aa()));
        for (Map.Entry<Integer, Treaty> entry : Locutus.imp().getNationDB().getTreaties(war.getAttacker_aa()).entrySet()) {
            if (entry.getValue().getType().isDefensive()) {
                attAA.add(entry.getKey());
            }
        }

        Set<Integer> defAA = new HashSet<>(Collections.singleton(war.getDefender_aa()));
        for (Map.Entry<Integer, Treaty> entry : Locutus.imp().getNationDB().getTreaties(war.getDefender_aa()).entrySet()) {
            if (entry.getValue().getType().isDefensive()) {
                defAA.add(entry.getKey());
            }
        }

        Set<Integer> counters = new HashSet<>();
        Set<Integer> isCounter = new HashSet<>();

        Set<Integer> nationIds = new HashSet<>(Arrays.asList(war.getAttacker_id(), war.getDefender_id()));
        long finalEndDate = endDate;
        Collection<DBWar> possibleCounters = getWarsForNationOrAlliance(nationIds::contains, null,
                f -> f.getDate() >= startDate - TimeUnit.DAYS.toMillis(5) && f.getDate() <= finalEndDate).values();
        for (DBWar other : possibleCounters) {
            if (other.getWarId() == war.getWarId()) continue;
            if (attAA.contains(other.getAttacker_aa()) || !(defAA.contains(other.getAttacker_aa()))) continue;
            if (other.getDate() < war.getDate()) {
                if (other.getAttacker_id() == war.getDefender_id() && attAA.contains(other.getDefender_aa())) {
                    isCounter.add(other.getWarId());
                }
            } else if (other.getDefender_id() == war.getAttacker_id()) {
                counters.add(other.getWarId());
            }
        }

        boolean isEscalated = !counters.isEmpty() && !isCounter.isEmpty();

        CounterType type;
        if (isEscalated) {
            type = CounterType.ESCALATION;
        } else if (!counters.isEmpty()) {
            type = CounterType.GETS_COUNTERED;
        } else if (!isCounter.isEmpty()) {
            type = CounterType.IS_COUNTER;
        } else {
            type = CounterType.UNCONTESTED;
        }

        boolean finalIsActive = isActive;
        if (!isOngoing) {
            update("INSERT OR REPLACE INTO `COUNTER_STATS`(`id`, `type`, `active`) VALUES(?, ?, ?)", new ThrowingConsumer<PreparedStatement>() {
                @Override
                public void acceptThrows(PreparedStatement stmt) throws Exception {
                    stmt.setInt(1, warId);
                    stmt.setInt(2, type.ordinal());
                    stmt.setBoolean(3, finalIsActive);
                }
            });
        }

        CounterStat stat = new CounterStat();
        stat.type = type;
        stat.isActive = isActive;
        return stat;
    }

    public void updateActiveWars(Consumer<Event> eventConsumer) {
        updateWars(false, eventConsumer);
    }

    private final Map<Integer, Integer> activeWarCache = new ConcurrentHashMap<>();
    private volatile boolean loadedWarCounts = false;

    public boolean loadWarCounts() {
        for (DBAlliance alliance : Locutus.imp().getNationDB().getAlliances()) {
            activeWarCache.put(alliance.getId(), alliance.getNumWars());
        }
        loadedWarCounts = true;
        return true;
    }

    public void updateWars(boolean pullAll, Consumer<Event> eventConsumer) {
        for (DBAlliance alliance : Locutus.imp().getNationDB().getAlliances()) {
            Integer expected = activeWarCache.get(alliance.getId());
            boolean fetch = false;
            if (expected == null) {
                expected = alliance.getNumWars();
                activeWarCache.put(alliance.getId(), expected);
                if (expected != 0 && loadedWarCounts) {
                    fetch = true;
                }
            }
            int currWars = alliance.getNumWars();
            if (expected != currWars || fetch) {
                activeWarCache.put(alliance.getId(), currWars);
                if (expected > 0) {
                    fetchWars(pullAll, alliance, eventConsumer);
                }
            }
        }
    }

    public void fetchWars(boolean pullAll, DBAlliance alliance, Consumer<Event> eventConsumer) {
        DnsApi v3 = Locutus.imp().getV3();
        DnsQuery<WarHistory> fetched = v3.allianceWarHistory(alliance.getId(), pullAll ? true : null, null, null, null, null);
        List<WarHistory> wars = new ArrayList<>(fetched.call());
        System.out.println("Fetched " + wars.size() + " wars for " + alliance.getName());

        List<DBWar> dbWars = wars.stream().map(f -> {
            DBWar war = new DBWar();
            war.update(f, null);
            return war;
        }).toList();

        Set<Integer> activeWarIds = getActiveWars().stream()
                .filter(f -> f.getAttacker_aa() == alliance.getId() || f.getDefender_aa() == alliance.getId())
                .map(DBWar::getWarId).collect(Collectors.toSet());

        System.out.println("Update wars " + dbWars.size() + " for " + alliance.getName() + " | " + activeWarIds.size() + " active wars");

        updateWars(dbWars, activeWarIds, eventConsumer, eventConsumer != null);
    }

    public boolean updateWars(List<DBWar> dbWars, Collection<Integer> expectedIds, Consumer<Event> eventConsumer, boolean handleNationStatus) {
        List<DBWar> prevWars = new ArrayList<>();
        List<DBWar> newWars = new ArrayList<>();
        Set<Integer> expectedIdsSet = expectedIds == null ? null : expectedIds instanceof Set ? (Set<Integer>) expectedIds : new ObjectOpenHashSet<>(expectedIds);
        Set<Integer> idsFetched = dbWars.stream().map(DBWar::getWarId).collect(Collectors.toSet());

        for (DBWar war : dbWars) {
            DBWar existing = warsById.get(war);
            if (existing != null && existing.equalsDeep(war)) {
                continue;
            }
            prevWars.add(existing == null ? null : existing.copy());
            newWars.add(war);

            if (handleNationStatus && existing == null && war.getDate() > System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(15) && war.getStatus() == WarStatus.ACTIVE) {
                Locutus.imp().getNationDB().setNationActive(war.getAttacker_id(), war.getDate(), eventConsumer);
            }
        }

        long cannotExpireWithin5m = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5);
        for (DBWar war : activeWars.getActiveWars()) {
            if (war.getDate() >= cannotExpireWithin5m) continue;
            if (expectedIdsSet != null && expectedIdsSet.contains(war.getWarId()) && !idsFetched.contains(war.getWarId())) {
                prevWars.add(war.copy());
                newWars.add(war);
                continue;
            }
        }

        saveWars(newWars, true);

        List<Map.Entry<DBWar, DBWar>> warUpdatePreviousNow = new ArrayList<>();

        for (int i = 0 ; i < prevWars.size(); i++) {
            DBWar previous = prevWars.get(i);
            DBWar newWar = newWars.get(i);
            if (newWar.isActive()) {
                activeWars.addActiveWar(newWar);
            } else {
                if (handleNationStatus && previous != null && previous.isActive() && (newWar.getStatus() == WarStatus.DEFENDER_VICTORY || newWar.getStatus() == WarStatus.ATTACKER_VICTORY)) {
                    boolean isAttacker = newWar.getStatus() == WarStatus.ATTACKER_VICTORY;
                    DBNation defender = newWar.getNation(!isAttacker);
                    // TODO FIXME :||remove possible protection?
                }
                activeWars.makeWarInactive(newWar);
            }

            warUpdatePreviousNow.add(new AbstractMap.SimpleEntry<>(previous, newWar));
        }

        if (!warUpdatePreviousNow.isEmpty() && eventConsumer != null) {
            try {
                WarUpdateProcessor.processWars(warUpdatePreviousNow, eventConsumer);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    public void saveWars(Collection<DBWar> values, boolean addToMap) {
        if (values.isEmpty()) return;
        if (addToMap) {
            for (DBWar war : values) {
                setWar(war);
            }
        }
        save(new ArrayList<>(values));
    }

    public Map<Integer, DBWar> getWars(WarStatus status) {
        return getWars(f -> f.getStatus() == status);
    }

    public Map<Integer, DBWar> getWarsSince(long date) {
        return getWars(f -> f.getDate() > date);
    }

    public ObjectOpenHashSet<DBWar> getWars() {
        synchronized (warsById) {
            return new ObjectOpenHashSet<>(warsById);
        }
    }

    public Map<Integer, List<DBWar>> getActiveWarsByAttacker(Set<Integer> attackers, Set<Integer> defenders, WarStatus... statuses) {
        Set<Integer> all = new HashSet<>();

        Map<Integer, List<DBWar>> map = new Int2ObjectOpenHashMap<>();
        activeWars.getActiveWars(f -> all.contains(f), new Predicate<DBWar>() {
            @Override
            public boolean test(DBWar war) {
                if (attackers.contains(war.getAttacker_id()) || defenders.contains(war.getDefender_id())) {
                    List<DBWar> list = map.computeIfAbsent(war.getAttacker_id(), k -> new ArrayList<>());
                    list.add(war);
                }
                return false;
            }
        });
        return map;
    }

    public DBWar getWar(int warId) {
        return warsById.get(new DBWar.DBWarKey(warId));
    }

    public List<DBWar> getWars(int nation1, int nation2, long start, long end) {
        List<DBWar> list = new ArrayList<>();
        synchronized (warsByNationLock) {
            Object wars = warsByNationId.get(nation1);
            if (wars != null) {
                ArrayUtil.iterateElements(DBWar.class, wars, dbWar -> {
                    if ((dbWar.getDefender_id() == nation2 || dbWar.getAttacker_id() == nation1) && dbWar.getDate() > start && dbWar.getDate() < end) {
                        list.add(dbWar);
                    }
                });
            }
        }
        return list;
    }

    public DBWar getActiveWarByNation(int attacker, int defender) {
        for (DBWar war : activeWars.getActiveWars(attacker)) {
            if (war.getAttacker_id() == attacker && war.getDefender_id() == defender) {
                return war;
            }
        }
        return null;
    }

    public Set<DBWar> getWarsByNation(int nation, WarStatus status) {
        if (status == WarStatus.ACTIVE || status == WarStatus.ATTACKER_OFFERED_PEACE || status == WarStatus.DEFENDER_OFFERED_PEACE) {
            Set<DBWar> wars = new ObjectOpenHashSet<>();
            for (DBWar war : activeWars.getActiveWars(nation)) {
                if (war.getStatus() == status) {
                    wars.add(war);
                }
            }
            return wars;
        }
        Set<DBWar> list;
        synchronized (warsByNationLock) {
            Object wars = warsByNationId.get(nation);
            if (wars == null) return Collections.emptySet();
            list = new ObjectOpenHashSet<>();
            ArrayUtil.iterateElements(DBWar.class, wars, dbWar -> {
                if (dbWar.getStatus() == status) {
                    list.add(dbWar);
                }
            });
        }
        return list;
    }

    public Set<DBWar> getActiveWarsByAlliance(Set<Integer> attackerAA, Set<Integer> defenderAA) {
        return activeWars.getActiveWars(f -> true, f -> (attackerAA == null || attackerAA.contains(f.getAttacker_aa())) && (defenderAA == null || defenderAA.contains(f.getDefender_aa())));
    }

    public Set<DBWar> getWarsByAlliance(int attacker) {
        synchronized (warsByAllianceId) {
            Object wars = warsByAllianceId.get(attacker);
            if (wars == null) return Collections.emptySet();
            return ArrayUtil.toSet(DBWar.class, wars);
        }
    }

    public Set<DBWar> getWarsByNation(int nationId) {
        Set<DBWar> result;
        synchronized (warsByNationLock) {
            Object amt = warsByNationId.get(nationId);
            result = ArrayUtil.toSet(DBWar.class, amt);
        }
        return result;
    }

    public Set<DBWar> getWarsByNationMatching(int nationId, Predicate<DBWar> filter) {
        Set<DBWar> list;
        synchronized (warsByNationLock) {
            Object wars = warsByNationId.get(nationId);
            if (wars == null) return Collections.emptySet();
            list = new ObjectOpenHashSet<>();
            ArrayUtil.iterateElements(DBWar.class, wars, dbWar -> {
                if (filter.test(dbWar)) {
                    list.add(dbWar);
                }
            });
        }
        return list;
    }

    public DBWar getLastOffensiveWar(int nation) {
        return getWarsByNation(nation).stream().filter(f -> f.getAttacker_id() == nation).max(Comparator.comparingInt(o -> o.getWarId())).orElse(null);
    }

    public DBWar getLastOffensiveWar(int nation, Long beforeDate) {
        Stream<DBWar> filter = getWarsByNation(nation).stream().filter(f -> f.getAttacker_id() == nation);
        if (beforeDate != null) filter = filter.filter(f -> f.getDate() < beforeDate);
        return filter.max(Comparator.comparingInt(o -> o.getWarId())).orElse(null);
    }

    public DBWar getLastDefensiveWar(int nation) {
        return getWarsByNation(nation).stream().filter(f -> f.getDefender_id() == nation).max(Comparator.comparingInt(o -> o.getWarId())).orElse(null);
    }

    public DBWar getLastDefensiveWar(int nation, Long beforeDate) {
        Stream<DBWar> filter = getWarsByNation(nation).stream().filter(f -> f.getDefender_id() == nation);
        if (beforeDate != null) filter = filter.filter(f -> f.getDate() < beforeDate);
        return filter.max(Comparator.comparingInt(o -> o.getWarId())).orElse(null);

    }

    public DBWar getLastWar(int nationId, Long snapshot) {
        Stream<DBWar> filter = getWarsByNation(nationId).stream();
        if (snapshot != null) filter = filter.filter(f -> f.getDate() < snapshot);
        return filter.max(Comparator.comparingInt(o -> o.getWarId())).orElse(null);
    }

    public Set<DBWar> getWarsByNation(int nation, WarStatus... statuses) {
        if (statuses.length == 0) return getWarsByNation(nation);
        if (statuses.length == 1) return getWarsByNation(nation, statuses[0]);
        Set<WarStatus> statusSet = new HashSet<>(Arrays.asList(statuses));

        Set<DBWar> set;
        synchronized (warsByNationLock) {
            Object wars = warsByNationId.get(nation);
            set = new ObjectOpenHashSet<>();
            ArrayUtil.iterateElements(DBWar.class, wars, dbWar -> {
                if (statusSet.contains(dbWar.getStatus())) {
                    set.add(dbWar);
                }
            });
        }
        return set;
    }

    public Set<DBWar> getActiveWars(Set<Integer> alliances, WarStatus... statuses) {
        if (statuses.length == 0) {
            return Collections.emptySet();
        }
        // ordinal to boolean array
        boolean[] warStatuses = WarStatus.toArray(statuses);
        // enum set?
        return activeWars.getActiveWars(f -> true, f -> (alliances.contains(f.getAttacker_aa()) || alliances.contains(f.getDefender_aa())) && warStatuses[f.getStatus().ordinal()]);
    }

    public Set<DBWar> getWarByStatus(WarStatus... statuses) {
        boolean[] warStatuses = WarStatus.toArray(statuses);
        return new ObjectOpenHashSet<>(getWars(f -> warStatuses[f.getStatus().ordinal()]).values());
    }

    public Set<DBWar> getWars(Set<Integer> alliances, long start) {
        return getWars(alliances, start, Long.MAX_VALUE);
    }

    public Set<DBWar> getWars(Set<Integer> alliances, long start, long end) {
        Map<Integer, DBWar> wars = getWarsForNationOrAlliance(null, alliances::contains, f -> f.getDate() > start && f.getDate() < end);
        return new ObjectOpenHashSet<>(wars.values());
    }

    public Set<DBWar> getWarsById(Set<Integer> warIds) {
        Set<DBWar> result = new ObjectOpenHashSet<>();
        synchronized (warsById) {
            for (Integer id : warIds) {
                DBWar war = warsById.get(new DBWar.DBWarKey(id));
                if (war != null) result.add(war);
            }
        }
        return result;
    }
    public Map<Integer, DBWar> getWars(Collection<Integer> coal1Alliances, Collection<Integer> coal1Nations, Collection<Integer> coal2Alliances, Collection<Integer> coal2Nations, long start, long end) {
        if (coal1Alliances.isEmpty() && coal1Nations.isEmpty() && coal2Alliances.isEmpty() && coal2Nations.isEmpty()) return Collections.emptyMap();

        Set<Integer> alliances = new IntOpenHashSet();
        alliances.addAll(coal1Alliances);
        alliances.addAll(coal2Alliances);
        Set<Integer> nations = new IntOpenHashSet();
        nations.addAll(coal1Nations);
        nations.addAll(coal2Nations);

        Predicate<DBWar> isAllowed;
        if (coal1Alliances.isEmpty() && coal1Nations.isEmpty()) {
            isAllowed = new Predicate<DBWar>() {
                @Override
                public boolean test(DBWar war) {
                    if (war.getDate() < start || war.getDate() > end) return false;
                    return coal2Alliances.contains(war.getAttacker_aa()) || coal2Nations.contains(war.getAttacker_id()) || coal2Alliances.contains(war.getDefender_aa()) || coal2Nations.contains(war.getDefender_id());
                }
            };
        } else if (coal2Alliances.isEmpty() && coal2Nations.isEmpty()) {
            isAllowed = new Predicate<DBWar>() {
                @Override
                public boolean test(DBWar war) {
                    if (war.getDate() < start || war.getDate() > end) return false;
                    return coal1Alliances.contains(war.getAttacker_aa()) || coal1Nations.contains(war.getAttacker_id()) || coal1Alliances.contains(war.getDefender_aa()) || coal1Nations.contains(war.getDefender_id());
                }
            };
        } else {
            isAllowed = new Predicate<DBWar>() {
                @Override
                public boolean test(DBWar war) {
                    if (war.getDate() < start || war.getDate() > end) return false;
                    return ((coal1Alliances.contains(war.getAttacker_aa()) || coal1Nations.contains(war.getAttacker_id())) && (coal2Alliances.contains(war.getDefender_aa()) || coal2Nations.contains(war.getDefender_id()))) ||
                            ((coal1Alliances.contains(war.getDefender_aa()) || coal1Nations.contains(war.getDefender_id())) && (coal2Alliances.contains(war.getAttacker_aa()) || coal2Nations.contains(war.getAttacker_id())));
                }
            };
        }

        return getWarsForNationOrAlliance(nations.isEmpty() ? null : nations::contains, alliances.isEmpty() ? null : alliances::contains, isAllowed);
    }

    public int countWarsByNation(int nation_id, long date, Long endDate) {
        if (endDate == null || endDate == Long.MAX_VALUE) return countWarsByNation(nation_id, date);
        int result;
        synchronized (warsByNationLock) {
            Object wars = warsByNationId.get(nation_id);
            result = ArrayUtil.countElements(DBWar.class, wars, f -> f.getDate() >= date && f.getDate() <= endDate);
        }
        return result;
    }

    public int countWarsByNation(int nation_id, long date) {
        if (date == 0) {
            int result;
            synchronized (warsByNationLock) {
                Object wars = warsByNationId.get(nation_id);
                result = ArrayUtil.countElements(DBWar.class, wars);
            }
            return result;
        }
        int result;
        synchronized (warsByNationLock) {
            Object wars = warsByNationId.get(nation_id);
            if (wars == null) return 0;
            result = ArrayUtil.countElements(DBWar.class, wars, war -> war.getDate() > date);
        }
        return result;
    }

    public int countOffWarsByNation(int nation_id, long startDate, long endDate) {
        int result;
        synchronized (warsByNationLock) {
            Object wars = warsByNationId.get(nation_id);
            if (wars == null) return 0;
            result = ArrayUtil.countElements(DBWar.class, wars, war -> war.getAttacker_id() == nation_id && war.getDate() > startDate && war.getDate() < endDate);
        }
        return result;
    }

    public int countDefWarsByNation(int nation_id, long startDate, long endDate) {
        int result;
        synchronized (warsByNationLock) {
            Object wars = warsByNationId.get(nation_id);
            if (wars == null) return 0;
            result = ArrayUtil.countElements(DBWar.class, wars, war -> war.getDefender_id() == nation_id && war.getDate() > startDate && war.getDate() < endDate);
        }
        return result;
    }

    public int countWarsByAlliance(int alliance_id, long date) {
        if (date == 0) {
            synchronized (warsByAllianceId) {
                Object wars = warsByAllianceId.get(alliance_id);
                return ArrayUtil.countElements(DBWar.class, wars);
            }
        }
        synchronized (warsByAllianceId) {
            Object wars = warsByAllianceId.get(alliance_id);
            if (wars == null) return 0;
            return ArrayUtil.countElements(DBWar.class, wars, war -> war.getDate() > date);
        }
    }

    public AttackQuery queryAttacks() {
        return new AttackQuery(this);
    }
}

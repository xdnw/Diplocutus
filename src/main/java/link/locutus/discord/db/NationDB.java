package link.locutus.discord.db;

import com.ptsmods.mysqlw.query.builder.SelectBuilder;
import com.ptsmods.mysqlw.table.ColumnType;
import com.ptsmods.mysqlw.table.TablePreset;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import link.locutus.discord.Locutus;
import link.locutus.discord.Logg;
import link.locutus.discord.api.endpoints.DnsQuery;
import link.locutus.discord.api.generated.Alliance;
import link.locutus.discord.api.generated.AllianceTreaties;
import link.locutus.discord.api.generated.Nation;
import link.locutus.discord.api.generated.TreatyType;
import link.locutus.discord.commands.manager.v2.builder.SummedMapRankBuilder;
import link.locutus.discord.config.Settings;
import link.locutus.discord.api.endpoints.DnsApi;
import link.locutus.discord.db.entities.*;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.Treaty;
import link.locutus.discord.db.entities.components.AlliancePrivate;
import link.locutus.discord.db.entities.components.NationPrivate;
import link.locutus.discord.db.entities.metric.AllianceMetric;
import link.locutus.discord.db.entities.metric.DnsMetric;
import link.locutus.discord.db.handlers.SyncableDatabase;
import link.locutus.discord.event.Event;
import link.locutus.discord.event.alliance.AllianceCreateEvent;
import link.locutus.discord.event.alliance.AllianceDeleteEvent;
import link.locutus.discord.event.nation.*;
import link.locutus.discord.event.treaty.*;
import link.locutus.discord.util.io.PagePriority;
import link.locutus.discord.util.scheduler.ThrowingBiConsumer;
import link.locutus.discord.util.scheduler.ThrowingConsumer;
import link.locutus.discord.util.*;
import link.locutus.discord.util.math.ArrayUtil;
import link.locutus.discord.api.types.Rank;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class NationDB extends DBMainV2 implements SyncableDatabase {
    private final Map<Integer, DBNation> nationsById = new Int2ObjectOpenHashMap<>();
    private final Map<Integer, Map<Integer, DBNation>> nationsByAlliance = new Int2ObjectOpenHashMap<>();
    private final Map<Integer, DBAlliance> alliancesById = new Int2ObjectOpenHashMap<>();
    private final Map<Integer, Map<Integer, Treaty>> treatiesByAlliance = new Int2ObjectOpenHashMap<>();
    private ReportManager reportManager;

    public NationDB() throws SQLException, ClassNotFoundException {
        super("nations");
    }

    public ReportManager getReportManager() {
        return reportManager;
    }

    public void createTables() {
        executeStmt(SQLUtil.createTable(new DBNation()));
        executeStmt(SQLUtil.createTable(new DBAlliance()));
        executeStmt(SQLUtil.createTable(new Treaty()));
        executeStmt(SQLUtil.createTable(new NationPrivate()));
        {
            executeStmt(SQLUtil.addColumn(new NationPrivate(), "totalSlots", int.class, "0"), true);
            executeStmt(SQLUtil.addColumn(new NationPrivate(), "usedSlots", int.class, "0"), true);
            executeStmt(SQLUtil.addColumn(new NationPrivate(), "effectBuildings", byte[].class, null), true);
            executeStmt(SQLUtil.addColumn(new NationPrivate(), "outdatedEffects", long.class, "0"), true);
            executeStmt(SQLUtil.addColumn(new NationPrivate(), "developmentCostPercent", double.class, "0"), true);
            executeStmt(SQLUtil.addColumn(new NationPrivate(), "landCostPercent", double.class, "0"), true);
            executeStmt(SQLUtil.addColumn(new NationPrivate(), "buildingCostPercent", double.class, "0"), true);
            executeStmt(SQLUtil.addColumn(new NationPrivate(), "projectCostPercent", double.class, "0"), true);
            executeStmt(SQLUtil.addColumn(new NationPrivate(), "repairCostPercent", double.class, "0"), true);
            executeStmt(SQLUtil.addColumn(new NationPrivate(), "politicalSupportCostPercent", double.class, "0"), true);
            executeStmt(SQLUtil.addColumn(new NationPrivate(), "techCostPercent", double.class, "0"), true);
            executeStmt(SQLUtil.addColumn(new NationPrivate(), "liftCostPercent", double.class, "0"), true);

        }
        executeStmt(SQLUtil.createTable(new AlliancePrivate()));
        {

        }
        {
            String query = "CREATE TABLE IF NOT EXISTS `BEIGE_REMINDERS` (`target` INT NOT NULL, `attacker` INT NOT NULL, `turn` BIGINT NOT NULL, PRIMARY KEY(target, attacker))";
            executeStmt(query);
        }
        {
            String nationMetaTable = "CREATE TABLE IF NOT EXISTS `NATION_META` (`id` BIGINT NOT NULL, `key` BIGINT NOT NULL, `meta` BLOB NOT NULL, `date_updated` BIGINT NOT NULL, PRIMARY KEY(id, key))";
            executeStmt(nationMetaTable);
        }
        createKicksTable();
        String activity = "CREATE TABLE IF NOT EXISTS `activity` (`nation` INT NOT NULL, `turn` BIGINT NOT NULL, PRIMARY KEY(nation, turn))";
        executeStmt(activity);

        executeStmt("CREATE TABLE IF NOT EXISTS ALLIANCE_METRICS (alliance_id INT NOT NULL, metric INT NOT NULL, turn BIGINT NOT NULL, value DOUBLE NOT NULL, PRIMARY KEY(alliance_id, metric, turn))");
        executeStmt("CREATE TABLE IF NOT EXISTS GAME_METRICS_DAY (metric INT NOT NULL, day BIGINT NOT NULL, value DOUBLE NOT NULL, PRIMARY KEY(metric, day))");
        executeStmt("CREATE TABLE IF NOT EXISTS GAME_METRICS_TURN (metric INT NOT NULL, turn BIGINT NOT NULL, value DOUBLE NOT NULL, PRIMARY KEY(metric, turn))");

        purgeOldBeigeReminders();

        this.reportManager = new ReportManager(this);

        createDeletionsTables();
    }

    private void loadNationPrivate() {
        List<NationPrivate> nationPrivate = loadAll(new NationPrivate());
        Set<Integer> toDelete = new LinkedHashSet<>();
        for (NationPrivate np : nationPrivate) {
            DBNation nation = getNation(np.getNationId());
            if (nation != null) {
                nation.setNationPrivate(np);
            } else {
                toDelete.add(np.getNationId());
            }
        }
        if (!toDelete.isEmpty()) {
            deleteNationPrivateInDb(toDelete);
        }
    }

    private void loadAlliancePrivate() {
        List<AlliancePrivate> alliancePrivate = loadAll(new AlliancePrivate());
        Set<Integer> toDelete = new LinkedHashSet<>();
        for (AlliancePrivate ap : alliancePrivate) {
            DBAlliance alliance = getAlliance(ap.getAllianceId());
            if (alliance != null) {
                alliance.setAlliancePrivate(ap);
            } else {
                toDelete.add(ap.getAllianceId());
            }
        }
        if (!toDelete.isEmpty()) {
            deleteAlliancePrivateInDb(toDelete);
        }
    }

    public NationDB load() throws SQLException {
        loadAlliances();
        loadAlliancePrivate();
        Logg.text("Loaded " + alliancesById.size() + " alliances");
        loadNations();
        loadNationPrivate();
        Logg.text("Loaded " + nationsById.size() + " nations");
        int treatiesLoaded = loadTreaties();
        Logg.text("Loaded " + treatiesLoaded + " treaties");

        loadAndPurgeMeta();

        // Load missing data
        if (Settings.INSTANCE.ENABLED_COMPONENTS.REPEATING_TASKS) {
            if (nationsById.isEmpty() && (Settings.INSTANCE.TASKS.ALL_NATION_SECONDS > 0)) {
                Logg.text("No nations loaded, fetching all");
                updateAllNations(null);
                Logg.text("Done fetching all nations");
            }
            if (alliancesById.isEmpty() && (Settings.INSTANCE.TASKS.ALL_NATION_SECONDS > 0)) {
                Logg.text("No alliances loaded, fetching all");
                updateAllAlliances(null);
                Logg.text("Done fetching all alliances");
            }
            if (treatiesByAlliance.isEmpty() && (Settings.INSTANCE.TASKS.TREATY_UPDATE_SECONDS > 0)) {
                Logg.text("No treaties loaded, fetching all");
                updateActiveTreaties(null);
                Logg.text("Done fetching all treaties");
            }
        }
        return this;
    }

    public void deleteExpiredTreaties(Consumer<Event> eventConsumer) {
        Set<Treaty> expiredTreaties = new HashSet<>();
        long currentTime = System.currentTimeMillis();
        synchronized (treatiesByAlliance) {
            for (Map<Integer, Treaty> allianceTreaties : treatiesByAlliance.values()) {
                for (Treaty treaty : allianceTreaties.values()) {
                    if (treaty.getEndTime() > 0 && treaty.getEndTime() < currentTime) {
                        expiredTreaties.add(treaty);
                    }
                }
            }
        }

        if (!expiredTreaties.isEmpty()) {
            deleteTreaties(expiredTreaties, eventConsumer);
        }
    }

    public boolean setNationActive(int nationId, long active, Consumer<Event> eventConsumer) {
        DBNation nation = getNation(nationId);
        if (nation != null) {
            if (nation.lastActiveMs() < active) {
                DBNation previous = eventConsumer == null ? null : nation.copy();
                long previousLastActive = nation.lastActiveMs();
                nation.setLastActive(active);

                // only call a new event if it's > 1 minute difference
                if (previousLastActive < active - TimeUnit.MINUTES.toMillis(1)) {
                    if (eventConsumer != null) eventConsumer.accept(new NationChangeActiveEvent(previous, nation));
                }
                return true;
            }
        }
        return false;
    }

    private void deleteAlliancePrivateInDb(Set<Integer> ids) {
        if (ids.isEmpty()) return;
        if (ids.size() == 1) {
            int id = ids.iterator().next();
            executeStmt("DELETE FROM " + new AlliancePrivate().getTableName() + " WHERE alliance_id = " + id);
        } else {
            executeStmt("DELETE FROM " + new AlliancePrivate().getTableName() + " WHERE alliance_id in " + StringMan.getString(ids));
        }
    }

    public void deleteAlliances(Set<Integer> ids, Consumer<Event> eventConsumer) {
        Set<Treaty> treatiesToDelete = new LinkedHashSet<>();
        Set<DBNation> dirtyNations = new HashSet<>();

        for (int id : ids) {
            DBAlliance alliance = getAlliance(id);
            if (alliance != null && eventConsumer != null) {
                eventConsumer.accept(new AllianceDeleteEvent(alliance));
            }
            synchronized (nationsByAlliance) {
                Map<Integer, DBNation> nations = nationsByAlliance.remove(id);
                if (nations != null) {
                    for (DBNation nation : nations.values()) {
                        if (nation.getAlliance_id() == id) {
                            DBNation copy = eventConsumer == null ? null : nation.copy();

                            nation.setAlliance_id(0);

                            if (copy != null) eventConsumer.accept(new NationChangeAllianceEvent(copy, nation));
                            dirtyNations.add(nation);
                        }
                    }
                }
            }
            synchronized (alliancesById) {
                alliancesById.remove(id);
            }
            synchronized (treatiesByAlliance) {
                Map<Integer, Treaty> removedTreaties = treatiesByAlliance.remove(id);
                if (removedTreaties != null && !removedTreaties.isEmpty()) {
                    treatiesToDelete.addAll(removedTreaties.values());
                }
            }
        }

        if (!treatiesToDelete.isEmpty()) deleteTreaties(treatiesToDelete, eventConsumer);
        if (!dirtyNations.isEmpty()) save(new ArrayList<>(dirtyNations));
        deleteAlliancesInDB(ids);
    }

    public void deleteTreaties(Set<Treaty> treaties, Consumer<Event> eventConsumer) {
        for (Treaty treaty : treaties) {
            boolean removed = false;
            synchronized (treatiesByAlliance) {
                removed |= treatiesByAlliance.getOrDefault(treaty.getFromId(), Collections.EMPTY_MAP).remove(treaty.getToId()) != null;
                removed |= treatiesByAlliance.getOrDefault(treaty.getToId(), Collections.EMPTY_MAP).remove(treaty.getFromId()) != null;
            }
            if (removed && eventConsumer != null) {
                if (treaty.getEndTime() <= System.currentTimeMillis()) {
                    eventConsumer.accept(new TreatyExpireEvent(treaty));
                } else {
                    eventConsumer.accept(new TreatyCancelEvent(treaty));
                }
            }
        }
        Set<Long> ids = treaties.stream().map(f -> f.getId()).collect(Collectors.toSet());
        deleteTreatiesInDB(ids);
    }

    private void deleteAlliancesInDB(Set<Integer> ids) {
        if (ids.isEmpty()) return;
        if (ids.size() == 1) {
            int id = ids.iterator().next();
            executeStmt("DELETE FROM ALLIANCES WHERE AllianceId = " + id);
        } else {
            executeStmt("DELETE FROM ALLIANCES WHERE `AllianceId` in " + StringMan.getString(ids));
        }
        deleteAlliancePrivateInDb(ids);
    }

    public void updateActiveTreaties(Consumer<Event> eventConsumer) {
        Map<Integer, List<DBNation>> byAA = getNationsByAlliance(true, true, true, false);
        fetchTreaties(f -> byAA.containsKey(f.getAlliance_id()), eventConsumer);
    }

    public void fetchTreaties(Predicate<DBAlliance> allowAlliance, Consumer<Event> eventConsumer) {
        DnsApi v3 = Locutus.imp().getV3();
        List<AllianceTreaties> treatiesV3 = new ArrayList<>();
        for (DBAlliance alliance : getAlliances()) {
            if (!allowAlliance.test(alliance)) continue;
            DnsQuery<AllianceTreaties> fetched = v3.allianceTreaties(alliance.getId());
            treatiesV3.addAll(fetched.call());
        }
        if (treatiesByAlliance.isEmpty()) eventConsumer = f -> {};
        updateTreaties(treatiesV3, eventConsumer, true);
    }

    public void updateTreaties(List<AllianceTreaties> treatiesV3, Consumer<Event> eventConsumer, boolean deleteMissing) {
        updateTreaties(treatiesV3, eventConsumer, deleteMissing ? f -> true : f -> deleteMissing);
    }

    public void updateTreaties(List<AllianceTreaties> treatiesV3, Consumer<Event> eventConsumer, Predicate<Treaty> deleteMissing) {
        Map<Integer, Map<Integer, Treaty>> treatiesByAACopy = new HashMap<>();
        long now = System.currentTimeMillis();
        for (AllianceTreaties treaty : treatiesV3) {
            Treaty dbTreaty = new Treaty();
            dbTreaty.update(treaty, null);
            if (dbTreaty.getEndTime() <= now) continue;

            DBAlliance fromAA = getAlliance(dbTreaty.getFromId());
            DBAlliance toAA = getAlliance(dbTreaty.getToId());
            if (fromAA == null || toAA == null) continue;
            treatiesByAACopy.computeIfAbsent(fromAA.getId(), f -> new Int2ObjectOpenHashMap<>()).put(toAA.getId(), dbTreaty);
            treatiesByAACopy.computeIfAbsent(toAA.getId(), f -> new Int2ObjectOpenHashMap<>()).put(fromAA.getId(), dbTreaty);
        }

        Set<Integer> allIds = new HashSet<>(treatiesByAACopy.keySet());
        synchronized (treatiesByAlliance) {
            allIds.addAll(treatiesByAlliance.keySet());
        }
        List<Treaty> dirtyTreaties = new ArrayList<>();
        Set<Treaty> toDelete = new LinkedHashSet<>();

        for (int aaId : allIds) {
            Map<Integer, Treaty> previousMap = new HashMap<>(treatiesByAlliance.getOrDefault(aaId, Collections.EMPTY_MAP));
            Map<Integer, Treaty> currentMap = treatiesByAACopy.getOrDefault(aaId, Collections.EMPTY_MAP);

            for (Map.Entry<Integer, Treaty> entry : previousMap.entrySet()) {
                Treaty previous = entry.getValue();
                Treaty current = currentMap.get(entry.getKey());

                int otherId = previous.getFromId() == aaId ? previous.getToId() : previous.getFromId();

                if (current == null) {
                    if (deleteMissing.test(previous)) toDelete.add(previous);
                } else {
                    synchronized (treatiesByAlliance) {
                        treatiesByAlliance.computeIfAbsent(aaId, f -> new Int2ObjectOpenHashMap<>()).put(otherId, current);
                    }
                    if (!current.equals(previous)) {
                        dirtyTreaties.add(current);
                    }
                    if (eventConsumer != null) {
                        if (current.getType() != previous.getType()) {
                            if (current.getType().getStrength() > previous.getType().getStrength()) {
                                eventConsumer.accept(new TreatyUpgradeEvent(previous, current));
                            } else {
                                eventConsumer.accept(new TreatyDowngradeEvent(previous, current));
                            }
                        } else if (current.getEndTime() > previous.getEndTime() + TimeUnit.HOURS.toMillis(1)) {
                            eventConsumer.accept(new TreatyExtendEvent(previous, current));
                        }
                    }
                }
            }

            for (Map.Entry<Integer, Treaty> entry : currentMap.entrySet()) {
                int otherAAId = entry.getKey();
                Treaty treaty = entry.getValue();
                if (!previousMap.containsKey(otherAAId)) {
                    dirtyTreaties.add(treaty);

                    synchronized (treatiesByAlliance) {
                        treatiesByAlliance.computeIfAbsent(aaId, f -> new Int2ObjectOpenHashMap<>()).put(otherAAId, treaty);
                    }
                    // Only run event if it's the from alliance (so you dont double run treaty events)
                    if (eventConsumer != null && treaty.getFromId() == aaId) {
                        eventConsumer.accept(new TreatyCreateEvent(treaty));
                    }
                }
            }
        }
        save(dirtyTreaties);
        if (!toDelete.isEmpty()) deleteTreaties(toDelete, eventConsumer);
    }

    public DBAlliance getAlliance(int id) {
        synchronized (alliancesById) {
            return alliancesById.get(id);
        }
    }

    public DBAlliance getOrCreateAlliance(int id) {
        DBAlliance existing = getAlliance(id);
        if (existing == null) {
            existing = new DBAlliance();
        }
        return existing;
    }

    private final Map<String, Integer> allianceByNameCache = new ConcurrentHashMap<>();

    public String getAllianceName(int aaId) {
        DBAlliance existing = getAlliance(aaId);
        if (existing != null) return existing.getName();

        return "AA:" + aaId;
    }

    public DBAlliance getAllianceByName(String name) {
        Integer aaId = allianceByNameCache.get(name.toLowerCase(Locale.ROOT));
        if (aaId != null) {
            synchronized (alliancesById) {
                DBAlliance aa = alliancesById.get(aaId);
                if (aa != null) return aa;
            }
        }
        return null;
    }

    private int loadTreaties() throws SQLException {
        List<Treaty> treaties = loadAll(new Treaty());
        long now = System.currentTimeMillis();
        Set<Long> treatiesToDelete = treaties.stream().filter(f -> f.getEndTime() < now).map(Treaty::getId).collect(Collectors.toSet());
        List<Event> postAsync = null;
        int total = 0;
        for (Treaty treaty : treaties) {
            if (treatiesToDelete.contains(treaty.getId())) {
                continue;
            }
            DBAlliance from = getAlliance(treaty.getFromId());
            DBAlliance to = getAlliance(treaty.getToId());
            if (from == null && to == null) {
                treatiesToDelete.add(treaty.getId());
                continue;
            }
            treatiesByAlliance.computeIfAbsent(treaty.getFromId(), f -> new Int2ObjectOpenHashMap<>()).put(treaty.getToId(), treaty);
            treatiesByAlliance.computeIfAbsent(treaty.getToId(), f -> new Int2ObjectOpenHashMap<>()).put(treaty.getFromId(), treaty);
            total++;
        }
        deleteTreatiesInDB(treatiesToDelete);
        return total;
    }

    private void loadNations() throws SQLException {
        List<DBNation> nations = loadAll(new DBNation());
        for (DBNation nation : nations) {
            nationsById.put(nation.getNation_id(), nation);
            if (nation.getAlliance_id() != 0) {
                nationsByAlliance.computeIfAbsent(nation.getAlliance_id(),
                        f -> new Int2ObjectOpenHashMap<>()).put(nation.getNation_id(), nation);
            }
        }
    }

    public void updateAllAlliances(Consumer<Event> eventConsumer) {
        Set<Integer> expectedIds;
        synchronized (alliancesById) {
            expectedIds = new IntOpenHashSet(alliancesById.keySet());
        }
        DnsApi v3 = Locutus.imp().getV3();
        List<Alliance> alliances = v3.alliance().call();
        Set<Integer> updated = processUpdatedAlliances(alliances, eventConsumer);
        expectedIds.removeAll(updated);
        deleteAlliances(expectedIds, eventConsumer);
    }

    public Set<Integer> processUpdatedAlliances(List<Alliance> alliances, Consumer<Event> eventConsumer) {
        if (alliances.isEmpty()) return Collections.emptySet();

        List<DBAlliance> dirtyAlliances = new ArrayList<>();

        List<DBAlliance> createdAlliances = new ArrayList<>();
        for (Alliance alliance : alliances) {
            if (alliance.CreationDate != null && alliance.AllianceName != null) { // Essential components of an alliance
                DBAlliance existing;
                synchronized (alliancesById) {
                    existing = alliancesById.get(alliance.AllianceId);
                }
                if (existing == null) {
                    existing = new DBAlliance();
                    existing.update(this, alliance, null);
                    synchronized (alliancesById) {
                        alliancesById.put(existing.getId(), existing);
                        allianceByNameCache.put(existing.getName().toLowerCase(Locale.ROOT), existing.getId());
                    }
                    createdAlliances.add(existing);
                    dirtyAlliances.add(existing);
                } else {
                    String originalName = existing.getName();
                    if (existing.update(this, alliance, eventConsumer)) {
                        dirtyAlliances.add(existing);
                        if (!originalName.equals(existing.getName())) {
                            allianceByNameCache.put(existing.getName().toLowerCase(Locale.ROOT), existing.getId());
                        }
                    }
                }
            }
        }
        if (!createdAlliances.isEmpty() && eventConsumer != null) {
            for (DBAlliance alliance : createdAlliances) {
                eventConsumer.accept(new AllianceCreateEvent(alliance));
            }
        }

        if (!dirtyAlliances.isEmpty()) {
            save(dirtyAlliances);
        }
        return alliances.stream().map(f -> f.AllianceId).collect(Collectors.toSet());
    }

    public void updateAllNations(Consumer<Event> eventConsumer) {
        DnsApi api = Locutus.imp().getV3();
        updateAllNations(api, eventConsumer);
    }

    public void updateAllNations(DnsApi api, Consumer<Event> eventConsumer) {
        long now = System.currentTimeMillis();
        Set<Integer> expectedIds;
        synchronized (nationsById) {
            expectedIds = new IntOpenHashSet(nationsById.keySet());
        }
        List<Nation> nations = api.nation().call();
        if (nations.isEmpty()) {
            System.out.println("No nations fetched");
            return;
        }
        Set<Integer> updated = updateNations(nations, eventConsumer, now);
        expectedIds.removeAll(updated);
        System.out.println("Delete nations " + expectedIds + " | updated " + updated.size());
        deleteNations(expectedIds, eventConsumer);
    }

    public Set<Integer> updateNations(Collection<Nation> nations, Consumer<Event> eventConsumer, long timestamp) {
        List<DBNation> toSave = new ArrayList<>();
        Set<Integer> nationsIdsFetched = new HashSet<>();
        for (Nation nation : nations) {
            if (nation.NationId != null) {
                nationsIdsFetched.add(nation.NationId);
            }
            updateNation(nation, eventConsumer, (a, b) -> toSave.add(b), timestamp);
        }
        System.out.println("Save " + toSave.size());
        save(toSave);
        return nationsIdsFetched;

    }
    /**
     *
     * @param nation the nation
     * @param eventConsumer any nation events to call
     * @param nationsToSave (previous, current)
     */
    private void updateNation(Nation nation, Consumer<Event> eventConsumer, BiConsumer<DBNation, DBNation> nationsToSave, long timestamp) {
        DBNation existing = getNation(nation.NationId);
        Consumer<Event> eventHandler;
        if (existing == null) {
            eventHandler = null;
        } else {
            eventHandler = eventConsumer;
        }
        AtomicBoolean isDirty = new AtomicBoolean();
        DBNation newNation = updateNationInfo(existing, nation, eventHandler, isDirty);
        if (isDirty.get()) {
            nationsToSave.accept(existing, newNation);
        }
        if (existing == null && eventConsumer != null && newNation.getAgeDays() <= 1) {
            eventConsumer.accept(new NationCreateEvent(null, newNation));
        }
    }

    public DBNation getNation(int id) {
        synchronized (nationsById) {
            return nationsById.get(id);
        }
    }

    private DBNation updateNationInfo(DBNation base, Nation nation, Consumer<Event> eventConsumer, AtomicBoolean markDirty) {
        DBNation copyOriginal = base == null ? null : base.copy();
        if (base == null) {
            markDirty.set(true);
            base = new DBNation();
        }
        if (base.update(this, nation, eventConsumer)) {
            markDirty.set(true);
        }
        processNationAllianceChange(copyOriginal, base);
        return base;
    }
    private void processNationAllianceChange(DBNation previous, DBNation current) {
        processNationAllianceChange(previous != null ? previous.getAlliance_id() : null, current);
    }
    private void processNationAllianceChange(Integer previousAA, DBNation current) {
        if (previousAA == null) {
            synchronized (nationsById) {
                nationsById.put(current.getNation_id(), current);
            }
            if (current.getAlliance_id() != 0) {
                synchronized (nationsByAlliance) {
                    nationsByAlliance.computeIfAbsent(current.getAlliance_id(),
                            f -> new Int2ObjectOpenHashMap<>()).put(current.getNation_id(), current);
                }
            }
        } else if (previousAA != current.getAlliance_id()) {
            synchronized (nationsByAlliance) {
                if (previousAA != 0) {
                    nationsByAlliance.getOrDefault(previousAA, Collections.EMPTY_MAP).remove(current.getNation_id());
                }
                if (current.getAlliance_id() != 0) {
                    nationsByAlliance.computeIfAbsent(current.getAlliance_id(),
                            f -> new Int2ObjectOpenHashMap<>()).put(current.getNation_id(), current);
                }
            }
        }
    }

    public void loadAlliances() throws SQLException {
        List<DBAlliance> alliances = loadAll(new DBAlliance());
        for (DBAlliance alliance : alliances) {
            alliancesById.put(alliance.getAlliance_id(), alliance);
            allianceByNameCache.put(alliance.getName().toLowerCase(Locale.ROOT), alliance.getId());
        }
    }

    @Override
    public Set<String> getTablesAllowingDeletion() {
        return Set.of("NATION_META");
    }

    @Override
    public Map<String, String> getTablesToSync() {
        return Map.of("NATION_META", "date_updated");
    }

    private void createKicksTable() {
        String kicks = "CREATE TABLE IF NOT EXISTS `KICKS2` (`nation` INT NOT NULL, `from_aa` INT NOT NULL, `from_rank` INT NOT NULL, `to_aa` INT NOT NULL, `to_rank` INT NOT NULL, `date` BIGINT NOT NULL)";
        executeStmt(kicks);
        executeStmt("CREATE INDEX IF NOT EXISTS index_kicks2_nation ON KICKS2 (nation);");
        executeStmt("CREATE INDEX IF NOT EXISTS index_kicks2_from_aa ON KICKS2 (from_aa,to_aa,date);");
    }

    public DBNation getNation(String nameOrLeader) {
        synchronized (nationsById) {
            for (DBNation nation : nationsById.values()) {
                if (nation.getNation().equalsIgnoreCase(nameOrLeader)) {
                    return nation;
                }
            }
            for (DBNation nation : nationsById.values()) {
                if (nation.getLeader().equalsIgnoreCase(nameOrLeader)) {
                    return nation;
                }
            }
        }
        return null;
    }

    public DBNation getFirstNationMatching(Predicate<DBNation> findIf) {
        synchronized (nationsById) {
            for (DBNation value : nationsById.values()) {
                if (findIf.test(value)) {
                    return value;
                }
            }
        }
        return null;
    }

    public Set<DBNation> getNationsMatching(Predicate<DBNation> findIf) {
        Set<DBNation> result = new LinkedHashSet<>();
        synchronized (nationsById) {
            for (DBNation value : nationsById.values()) {
                if (findIf.test(value)) {
                    result.add(value);
                }
            }
        }
        return result;
    }

    public DBNation getNationByName(String name) {
        return getFirstNationMatching(f -> f.getNation().equalsIgnoreCase(name));
    }

    public DBNation getNationByLeader(String leader) {
        return getFirstNationMatching(f -> f.getLeader().equalsIgnoreCase(leader));
    }

    public Map<Integer, DBNation> getNations() {
        synchronized (nationsById) {
            return new Int2ObjectOpenHashMap<>(nationsById);
        }
    }

    public Set<DBNation> getNations(Set<Integer> alliances) {
        if (alliances.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<DBNation> result;
        if (alliances.contains(0)) {
            if (alliances.size() == 1) {
                int id = alliances.iterator().next();
                return getNationsMatching(f -> f.getAlliance_id() == id);
            }
            return getNationsMatching(f -> alliances.contains(f.getAlliance_id()));
        } else {
            result = new LinkedHashSet<>();
            for (int aaId : alliances) {
                synchronized (nationsByAlliance) {
                    Map<Integer, DBNation> nations = nationsByAlliance.get(aaId);
                    if (nations != null) {
                        result.addAll(nations.values());
                    }
                }
            }
        }
        return result;
    }

    public Set<DBAlliance> getAlliances() {
        synchronized (alliancesById) {
            return new ObjectOpenHashSet<>(alliancesById.values());
        }
    }
    public Set<DBAlliance> getAlliances(boolean removeUntaxable, boolean removeInactive, boolean removeApplicants, int topX) {
        Map<Integer, Double> score = new Int2ObjectOpenHashMap<>();
        synchronized (nationsByAlliance) {
            for (Map.Entry<Integer, Map<Integer, DBNation>> entry : nationsByAlliance.entrySet()) {
                int aaId = entry.getKey();
                Map<Integer, DBNation> nationMap = entry.getValue();
                for (DBNation nation : nationMap.values()) {
                    if (removeApplicants && nation.getPositionEnum().id <= Rank.APPLICANT.id) continue;
                    if (removeUntaxable && (nation.hasProtection())) continue;
                    if (removeInactive && nation.active_m() > 7200) continue;
                    if ((removeUntaxable || removeInactive) && nation.isVacation()) continue;
                    score.merge(aaId, nation.getScore(), Double::sum);
                }
            }
        }
        // Sorted
        score = new SummedMapRankBuilder<>(score).sort().get();
        Set<DBAlliance> result = new LinkedHashSet<>();
        for (int aaId : score.keySet()) {
            DBAlliance alliance = getAlliance(aaId);
            if (alliance != null) result.add(alliance);
            if (result.size() >= topX) break;
        }
        return result;
    }

    public void loadAndPurgeMeta() {
        List<Integer> toDelete = new ArrayList<>();
        try (PreparedStatement stmt = prepareQuery("select * FROM NATION_META")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    int key = rs.getInt("key");
                    byte[] data = rs.getBytes("meta");

                    if (id > 0) {
                        DBNation nation = nationsById.get(id);
                        if (nation != null) {
                            nation.setMetaRaw(key, data);
                        } else {
                            toDelete.add(id);
                        }
                    } else {
                        int idAbs = Math.abs(id);
                        DBAlliance alliance;
                        synchronized (alliancesById) {
                            alliance = alliancesById.get(idAbs);
                        }
                        if (alliance != null) {
                            alliance.setMetaRaw(key, data);
                        } else {
                            toDelete.add(id);
                        }
                    }

                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        update("DELETE FROM NATION_META where id in " + StringMan.getString(toDelete));
    }

    public void setMeta(int nationId, NationMeta key, byte[] value) {
        checkNotNull(key);
        setMeta(nationId, key.ordinal(), value);
    }

    public void setMeta(int nationId, int ordinal, byte[] value) {
        checkNotNull(value);
        long pair = MathMan.pairInt(nationId, ordinal);
        update("INSERT OR REPLACE INTO `NATION_META`(`id`, `key`, `meta`, `date_updated`) VALUES(?, ?, ?, ?)", (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setInt(1, nationId);
            stmt.setInt(2, ordinal);
            stmt.setBytes(3, value);
            stmt.setLong(4, System.currentTimeMillis());
        });
    }

    public void deleteMeta(int nationId, NationMeta key) {
        deleteMeta(nationId, key.ordinal());
    }

    public void deleteMeta(int nationId, int keyId) {
        synchronized (this) {
            logDeletion("NATION_META", System.currentTimeMillis(), new String[]{"id", "key"}, nationId, keyId);
            update("DELETE FROM NATION_META where id = ? AND key = ?", new ThrowingConsumer<PreparedStatement>() {
                @Override
                public void acceptThrows(PreparedStatement stmt) throws Exception {
                    stmt.setInt(1, nationId);
                    stmt.setInt(2, keyId);
                }
            });
        }
    }


    public void deleteMeta(AllianceMeta key) {
        String condition = "key = " + key.ordinal() + " AND id < 0";
        deleteMeta(condition);
    }

    public void deleteMeta(NationMeta key) {
        String condition = "key = " + key.ordinal() + " AND id > 0";
        deleteMeta(condition);
    }

    private void deleteMeta(String condition) {
        synchronized (this) {
            logDeletion("NATION_META", System.currentTimeMillis(), condition, "id", "key");
            update("DELETE FROM NATION_META where " + condition);
        }
    }

    public void deleteBeigeReminder(int attacker, int target) {
        update("DELETE FROM `BEIGE_REMINDERS` WHERE `target` = ? AND `attacker` = ?", new ThrowingConsumer<PreparedStatement>() {
            @Override
            public void acceptThrows(PreparedStatement stmt) throws Exception {
                stmt.setInt(1, target);
                stmt.setInt(2, attacker);
            }
        });
    }

    public void purgeOldBeigeReminders() {
        long minTurn = TimeUtil.getHour() - (14 * 24 + 1);
        String queryStr = "DELETE FROM `BEIGE_REMINDERS` WHERE turn < ?";
        update(queryStr, (ThrowingConsumer<PreparedStatement>) stmt -> stmt.setLong(1, minTurn));
    }

    public void addBeigeReminder(DBNation target, DBNation attacker) {
        String query = "INSERT OR REPLACE INTO `BEIGE_REMINDERS` (`target`, `attacker`, `turn`) values(?,?,?)";
        update(query, new ThrowingConsumer<PreparedStatement>() {
            @Override
            public void acceptThrows(PreparedStatement stmt) throws SQLException {
                stmt.setInt(1, target.getNation_id());
                stmt.setInt(2, attacker.getNation_id());
                stmt.setLong(3, TimeUtil.getHour());
            }
        });
    }

    public Set<DBNation> getBeigeRemindersByTarget(DBNation nation) {
        try (PreparedStatement stmt = prepareQuery("SELECT attacker from BEIGE_REMINDERS where target = ?")) {
            stmt.setInt(1, nation.getNation_id());

            Set<DBNation> result = new HashSet<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int attackerId = rs.getInt(1);
                    DBNation other = DBNation.getById(attackerId);
                    if (other != null) {
                        result.add(other);
                    }
                }
            }
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Set<DBNation> getBeigeRemindersByAttacker(DBNation nation) {
        try (PreparedStatement stmt = prepareQuery("SELECT target from BEIGE_REMINDERS where attacker = ?")) {
            stmt.setInt(1, nation.getNation_id());

            Set<DBNation> result = new HashSet<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int attackerId = rs.getInt(1);
                    DBNation other = DBNation.getById(attackerId);
                    if (other != null) {
                        result.add(other);
                    }
                }
            }
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void addMetric(DnsMetric metric, long turnOrDay, double value) {
        if (!Double.isFinite(value)) {
            return;
        }
        boolean isTurn = metric.isTurn();
        String table = isTurn ? "GAME_METRICS_TURN" : "GAME_METRICS_DAY";
        String turnOrDayCol = isTurn ? "turn" : "day";
        String query = "INSERT OR REPLACE INTO `" + table + "`(`metric`, `" + turnOrDayCol + "`, `value`) VALUES(?, ?, ?)";
        update(query, (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setInt(1, metric.ordinal());
            stmt.setLong(2, turnOrDay);
            stmt.setDouble(3, value);
        });
    }

    public long getLatestMetricTime(boolean isTurn) {
        String tableName = isTurn ? "GAME_METRICS_TURN" : "GAME_METRICS_DAY";
        String turnOrDayCol = isTurn ? "turn" : "day";
        String query = "SELECT MAX(" + turnOrDayCol + ") FROM " + tableName;
        try (PreparedStatement stmt = getConnection().prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public Map<Long, Double> getMetrics(DnsMetric metric, long start, long end) {
        boolean isTurn = metric.isTurn();
        String table = isTurn ? "GAME_METRICS_TURN" : "GAME_METRICS_DAY";
        String turnOrDayCol = isTurn ? "turn" : "day";
        String query = "SELECT * FROM " + table + " WHERE metric = ? AND " + turnOrDayCol + " >= ? AND " + turnOrDayCol + " <= ?";
        Map<Long, Double> result = new Long2DoubleOpenHashMap();
        query(query, (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setInt(1, metric.ordinal());
            stmt.setLong(2, start);
            stmt.setLong(3, end);
        }, (ThrowingConsumer<ResultSet>) rs -> {
            while (rs.next()) {
                long turnOrDay = rs.getLong(turnOrDayCol);
                double value = rs.getDouble("value");
                result.put(turnOrDay, value);
            }
        });
        return result;
    }

    public Map<DnsMetric, Map<Long, Double>> getMetrics(Collection<DnsMetric> metrics, long start, long end) {
        if (metrics.isEmpty()) return new HashMap<>();
        if (metrics.size() == 1) {
            DnsMetric metric = metrics.iterator().next();
            return Collections.singletonMap(metric, getMetrics(metric, start, end));
        }
        Map<DnsMetric, Map<Long, Double>> result = new Object2ObjectOpenHashMap<>();
        result.putAll(getMetrics(metrics, true, start, end));
        result.putAll(getMetrics(metrics, false, start, end));
        return result;
    }

    private Map<DnsMetric, Map<Long, Double>> getMetrics(Collection<DnsMetric> metrics, boolean isTurn, long start, long end) {
        List<Integer> ids = new ArrayList<>(metrics.stream().filter(f -> f.isTurn() == isTurn).map(Enum::ordinal).toList());
        if (isTurn) {
            start = TimeUtil.getHour(start);
            end = end == Long.MAX_VALUE ? end : TimeUtil.getHour(end);
        } else {
            start = TimeUtil.getDay(start);
            end = end == Long.MAX_VALUE ? end : TimeUtil.getDay(end);
        }
        if (ids.isEmpty()) return new HashMap<>();
        if (ids.size() == 1) {
            DnsMetric metric = metrics.iterator().next();
            return Collections.singletonMap(metric, getMetrics(metric, start, end));
        }
        ids.sort(Comparator.naturalOrder());
        String table = isTurn ? "GAME_METRICS_TURN" : "GAME_METRICS_DAY";
        String turnOrDayCol = isTurn ? "turn" : "day";
        String query = "SELECT * FROM " + table + " WHERE metric in " + StringMan.getString(ids) + " AND " + turnOrDayCol + " >= ? AND " + turnOrDayCol + " <= ?";
        Map<DnsMetric, Map<Long, Double>> result = new Object2ObjectOpenHashMap<>();
        long finalStart = start;
        long finalEnd = end;
        query(query, (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setLong(1, finalStart);
            stmt.setLong(2, finalEnd);
        }, (ThrowingConsumer<ResultSet>) rs -> {
            while (rs.next()) {
                int metricId = rs.getInt("metric");
                DnsMetric metric = DnsMetric.values[metricId];
                long turnOrDay = rs.getLong(turnOrDayCol);
                double value = rs.getDouble("value");
                result.computeIfAbsent(metric, f -> new Long2DoubleOpenHashMap()).put(turnOrDay, value);
            }
        });
        return result;
    }

    public void addAllianceMetric(DBAlliance alliance, AllianceMetric metric, long turn, double value, boolean ignore) {
        checkNotNull(metric);
        if (!Double.isFinite(value)) {
            return;
        }
        String query = "INSERT OR " + (ignore ? "IGNORE" : "REPLACE") + " INTO `ALLIANCE_METRICS`(`alliance_id`, `metric`, `turn`, `value`) VALUES(?, ?, ?, ?)";
        update(query, new ThrowingConsumer<PreparedStatement>() {
            @Override
            public void acceptThrows(PreparedStatement stmt) throws SQLException {
                stmt.setInt(1, alliance.getAlliance_id());
                stmt.setInt(2, metric.ordinal());
                stmt.setLong(3, turn);
                stmt.setDouble(4, value);
            }
        });
    }

    public Map<DBAlliance, Map<AllianceMetric, Map<Long, Double>>> getAllianceMetrics(Set<Integer> allianceIds, AllianceMetric metric, long turn) {
        if (allianceIds.isEmpty()) throw new IllegalArgumentException("No metrics provided");
        List<Integer> alliancesSorted = new ArrayList<>(allianceIds);
        alliancesSorted.sort(Comparator.naturalOrder());
        String allianceQueryStr = StringMan.getString(alliancesSorted);

        Map<DBAlliance, Map<AllianceMetric, Map<Long, Double>>> result = new LinkedHashMap<>();

        String query = "SELECT * FROM ALLIANCE_METRICS WHERE alliance_id in " + allianceQueryStr + " AND metric = ? and turn <= ? ORDER BY turn DESC LIMIT " + allianceIds.size();
        query(query, new ThrowingConsumer<PreparedStatement>() {
            @Override
            public void acceptThrows(PreparedStatement stmt) throws Exception {
                stmt.setInt(1, metric.ordinal());
                stmt.setLong(2, turn);
            }
        }, new ThrowingConsumer<ResultSet>() {
            @Override
            public void acceptThrows(ResultSet rs) throws Exception {
                while (rs.next()) {
                    int allianceId = rs.getInt("alliance_id");
                    AllianceMetric metric = AllianceMetric.values[rs.getInt("metric")];
                    long turn = rs.getLong("turn");
                    double value = rs.getDouble("value");

                    DBAlliance alliance = getOrCreateAlliance(allianceId);
                    if (!result.containsKey(alliance)) {
                        result.computeIfAbsent(alliance, f -> new HashMap<>()).computeIfAbsent(metric, f -> new HashMap<>()).put(turn, value);
                    }
                }
            }
        });
        return result;
    }

    public Map<DBAlliance, Map<Long, Double>> getAllianceMetrics(AllianceMetric metric, long startTurn) {
        Map<DBAlliance, Map<Long, Double>> result = new LinkedHashMap<>();
        String query = "SELECT * FROM ALLIANCE_METRICS WHERE metric = ? and turn >= ? ORDER BY turn ASC";
        query(query, new ThrowingConsumer<PreparedStatement>() {
            @Override
            public void acceptThrows(PreparedStatement stmt) throws Exception {
                stmt.setInt(1, metric.ordinal());
                stmt.setLong(2, startTurn);
            }
        }, new ThrowingConsumer<ResultSet>() {
            @Override
            public void acceptThrows(ResultSet rs) throws Exception {
                while (rs.next()) {
                    int allianceId = rs.getInt("alliance_id");
                    long turn = rs.getLong("turn");
                    double value = rs.getDouble("value");
                    DBAlliance alliance = getOrCreateAlliance(allianceId);
                    result.computeIfAbsent(alliance, f -> new Long2DoubleLinkedOpenHashMap()).put(turn, value);
                }
            }
        });
        return result;
    }


    public Map<DBAlliance, Map<AllianceMetric, Map<Long, Double>>> getAllianceMetrics(Set<Integer> allianceIds, AllianceMetric metric, long turnStart, long turnEnd) {
        if (allianceIds.isEmpty()) throw new IllegalArgumentException("No metrics provided");
        String allianceQueryStr = StringMan.getString(allianceIds);
        boolean hasTurnEnd = turnEnd > 0 && turnEnd < Long.MAX_VALUE;

        Map<DBAlliance, Map<AllianceMetric, Map<Long, Double>>> result = new HashMap<>();

        String query = "SELECT * FROM ALLIANCE_METRICS WHERE alliance_id in " + allianceQueryStr + " AND metric = ? and turn >= ?" + (hasTurnEnd ? " and turn <= ?" : "");
        query(query, new ThrowingConsumer<PreparedStatement>() {
            @Override
            public void acceptThrows(PreparedStatement stmt) throws Exception {
                stmt.setInt(1, metric.ordinal());
                stmt.setLong(2, turnStart);
                if (hasTurnEnd) stmt.setLong(3, turnEnd);
            }
        }, new ThrowingConsumer<ResultSet>() {
            @Override
            public void acceptThrows(ResultSet rs) throws Exception {
                while (rs.next()) {
                    int allianceId = rs.getInt("alliance_id");
                    AllianceMetric metric = AllianceMetric.values[rs.getInt("metric")];
                    long turn = rs.getLong("turn");
                    double value = rs.getDouble("value");

                    DBAlliance alliance = getOrCreateAlliance(allianceId);
                    result.computeIfAbsent(alliance, f -> new HashMap<>()).computeIfAbsent(metric, f -> new HashMap<>()).put(turn, value);
                }
            }
        });
        return result;
    }

    public Map<DBAlliance, Map<AllianceMetric, Map<Long, Double>>> getAllianceMetrics(Set<Integer> allianceIds, Collection<AllianceMetric> metrics, long turnStart, long turnEnd) {
        if (metrics.isEmpty()) throw new IllegalArgumentException("No metrics provided");
        if (allianceIds.isEmpty()) throw new IllegalArgumentException("No metrics provided");
        List<Integer> alliancesSorted = new ArrayList<>(allianceIds);
        alliancesSorted.sort(Comparator.naturalOrder());
        String allianceQueryStr = StringMan.getString(alliancesSorted);
        String metricQueryStr = StringMan.getString(metrics.stream().map(Enum::ordinal).collect(Collectors.toList()));
        boolean hasTurnEnd = turnEnd > 0 && turnEnd < Long.MAX_VALUE;

        Map<DBAlliance, Map<AllianceMetric, Map<Long, Double>>> result = new HashMap<>();

        String query = "SELECT * FROM ALLIANCE_METRICS WHERE alliance_id in " + allianceQueryStr + " AND metric in " + metricQueryStr + " and turn >= ?" + (hasTurnEnd ? " and turn <= ?" : "");
        query(query, new ThrowingConsumer<PreparedStatement>() {
            @Override
            public void acceptThrows(PreparedStatement stmt) throws Exception {
                stmt.setLong(1, turnStart);
                if (hasTurnEnd) stmt.setLong(2, turnEnd);
            }
        }, new ThrowingConsumer<ResultSet>() {
            @Override
            public void acceptThrows(ResultSet rs) throws Exception {
                while (rs.next()) {
                    int allianceId = rs.getInt("alliance_id");
                    AllianceMetric metric = AllianceMetric.values[rs.getInt("metric")];
                    long turn = rs.getLong("turn");
                    double value = rs.getDouble("value");

                    DBAlliance alliance = getOrCreateAlliance(allianceId);
                    result.computeIfAbsent(alliance, f -> new HashMap<>()).computeIfAbsent(metric, f -> new HashMap<>()).put(turn, value);
                }
            }
        });
        return result;
    }

    public Set<Treaty> getTreatiesMatching(Predicate<Treaty> filter) {
        Set<Treaty> treaties = new ObjectOpenHashSet<>();
        synchronized (treatiesByAlliance) {
            for (Map<Integer, Treaty> allianceTreaties : treatiesByAlliance.values()) {
                for (Treaty treaty : allianceTreaties.values()) {
                    if (filter.test(treaty)) {
                        treaties.add(treaty);
                    }
                }
            }
        }
        return treaties;
    }

    public Set<Treaty> getTreaties() {
        Set<Treaty> treaties = new ObjectOpenHashSet<>();
        synchronized (treatiesByAlliance) {
            for (Map<Integer, Treaty> allianceTreaties : treatiesByAlliance.values()) {
                treaties.addAll(allianceTreaties.values());
            }
        }
        return treaties;
    }
    public Map<Integer, Treaty> getTreaties(int allianceId, TreatyType... types) {
        Map<Integer, Treaty> treaties = getTreaties(allianceId);
        Set<TreatyType> typesSet = new HashSet<>(Arrays.asList(types));
        treaties.entrySet().removeIf(t -> !typesSet.contains(t.getValue().getType()));
        return treaties;
    }

    public Map<Integer, Treaty> getTreaties(int allianceId) {
        synchronized (treatiesByAlliance) {
            Map<Integer, Treaty> treaties = treatiesByAlliance.get(allianceId);
            return treaties == null || treaties.isEmpty() ? Collections.EMPTY_MAP : new Int2ObjectOpenHashMap<>(treaties);
        }
    }

    private ConcurrentHashMap<Integer, Long> turnActivityCache = new ConcurrentHashMap<>();

    public void setActivity(int nationId, long turn) {
        if (turnActivityCache.computeIfAbsent(nationId, f -> 0L) >= turn) return; // already set (or newer
        update("INSERT OR REPLACE INTO `ACTIVITY` (`nation`, `turn`) VALUES(?, ?)", (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setInt(1, nationId);
            stmt.setLong(2, turn);
        });
    }

    public Set<Integer> getNationsActiveAtTurn(long turn) {
        Set<Integer> result = new IntOpenHashSet();
        try (PreparedStatement stmt = prepareQuery("SELECT nation FROM ACTIVITY WHERE turn = ?")) {
            stmt.setLong(1, turn);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getInt(1));
                }
            }
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Set<Long> getActivity(int nationId) {
        return getActivity(nationId, 0, Long.MAX_VALUE);
    }

    public Map<Integer, Long> getLastActiveTurns(Set<Integer> nationIds, long turn) {
        if (nationIds.isEmpty()) return Collections.emptyMap();
        if (nationIds.size() == 1) {
            int nationId = nationIds.iterator().next();
            return Map.of(nationId, getLastActiveTurn(nationId, turn));
        }

        String query = "SELECT * " +
                "FROM Activity " +
                "WHERE (nation, turn) IN (SELECT nation, MAX(turn) " +
                "FROM Activity " +
                "WHERE turn <= CURRENT_TURN " +
                "AND nation in " + StringMan.getString(nationIds) + " " +
                "GROUP BY nation)";
        Map<Integer, Long> result = new Int2LongOpenHashMap();
        query(query, new ThrowingConsumer<PreparedStatement>() {
            @Override
            public void acceptThrows(PreparedStatement stmt) throws Exception {
            }
        }, new ThrowingConsumer<ResultSet>() {
            @Override
            public void acceptThrows(ResultSet rs) throws Exception {
                while (rs.next()) {
                    int nationId = rs.getInt("nation");
                    long turn = rs.getLong("turn");
                    result.put(nationId, turn);
                }
            }
        });
        return result;
    }

    public long getLastActiveTurn(int nationId, long turn) {
        try (PreparedStatement stmt = prepareQuery("SELECT * FROM Activity WHERE nation = ? AND turn <= ? ORDER BY turn DESC LIMIT 1")) {
            stmt.setInt(1, nationId);
            stmt.setLong(2, turn);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    return rs.getLong("turn");
                }
            }
            return 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Set<Long> getActivityByDay(int nationId, long minHour) {
        Set<Long> result = new LinkedHashSet<>();
        for (long turn : getActivity(nationId, minHour, Long.MAX_VALUE)) {
            result.add(turn / 24);
        }
        return result;
    }

    public Map<Integer, Set<Long>> getActivityByDay(long minDate, long maxDate) {
        return getActivityByDay(minDate, maxDate, null);
    }

    public Map<Integer, Set<Long>> getActivityByDay(long minDate, long maxDate, Predicate<Integer> includeNation) {
        // dates are inclusive
        long minTurn = TimeUtil.getHour(minDate);
        long maxTurn = TimeUtil.getHour(maxDate);
        try (PreparedStatement stmt = prepareQuery("select nation, (`turn`/24) FROM ACTIVITY WHERE turn >= ? AND turn <= ?")) {
            stmt.setLong(1, minTurn);
            stmt.setLong(2, maxTurn);

            Map<Integer, Set<Long>> result = new Int2ObjectOpenHashMap<>();
            BiConsumer<Integer, Long> applyNation = includeNation == null ?
                    (nation, day) -> result.computeIfAbsent(nation, f -> new LongOpenHashSet()).add(day) : (nation, day) -> {
                if (includeNation.test(nation)) {
                    result.computeIfAbsent(nation, f -> new LongOpenHashSet()).add(day);
                }
            };

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt(1);
                    long day = rs.getLong(2);
                    applyNation.accept(id, day);
                }
            }
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Map<Integer, Set<Long>> getActivityByTurn(long minTurn, long maxTurn, Predicate<Integer> includeNation) {
        try (PreparedStatement stmt = prepareQuery("select nation, `turn` FROM ACTIVITY WHERE turn >= ? AND turn <= ?")) {
            stmt.setLong(1, minTurn);
            stmt.setLong(2, maxTurn);

            Map<Integer, Set<Long>> result = new Int2ObjectOpenHashMap<>();
            BiConsumer<Integer, Long> applyNation = includeNation == null ?
                    (nation, turn) -> result.computeIfAbsent(nation, f -> new LongOpenHashSet()).add(turn) : (nation, turn) -> {
                if (includeNation.test(nation)) {
                    result.computeIfAbsent(nation, f -> new LongOpenHashSet()).add(turn);
                }
            };

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt(1);
                    long turn = rs.getLong(2);
                    applyNation.accept(id, turn);
                }
            }
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Map<Long, Set<Integer>> getActivityByDay(long minDate, Predicate<Integer> allowNation) {
        long minTurn = TimeUtil.getHour(minDate);
        try (PreparedStatement stmt = prepareQuery("select nation, (`turn`/24) FROM ACTIVITY WHERE turn > ?")) {
            stmt.setLong(1, minTurn);

            Map<Long, Set<Integer>> result = new Long2ObjectOpenHashMap<>();

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt(1);
                    if (!allowNation.test(id)) continue;
                    long day = rs.getLong(2);
                    result.computeIfAbsent(day, f -> new IntOpenHashSet()).add(id);
                }
            }
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Set<Long> getActivity(int nationId, long minTurn, long maxTurn) {
        String query = "SELECT * FROM ACTIVITY WHERE nation = ? AND turn > ?";
        if (maxTurn != Long.MAX_VALUE) {
            query += " AND turn <= ?";
        }
        query += " ORDER BY turn ASC";
        try (PreparedStatement stmt = prepareQuery(query)) {
            stmt.setInt(1, nationId);
            stmt.setLong(2, minTurn);
            if (maxTurn != Long.MAX_VALUE) stmt.setLong(3, maxTurn);

            Set<Long> set = new LinkedHashSet<>();

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long turn = rs.getLong("turn");
                    set.add(turn);
                }
            }
            return set;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Map<Integer, List<DBNation>> getNationsByAlliance(Collection<DBNation> nationSet, boolean removeUntaxable, boolean removeInactive, boolean removeApplicants, boolean sortByScore) {
        final Int2DoubleMap scoreMap = new Int2DoubleOpenHashMap();
        Int2ObjectOpenHashMap<List<DBNation>> nationsByAllianceFiltered = new Int2ObjectOpenHashMap<>();

        long activeCutoff = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(7200);
        for (DBNation nation : nationSet) {
            int aaId = nation.getAlliance_id();
            if (aaId == 0) continue;
            if (removeApplicants && nation.getPositionEnum().id <= Rank.APPLICANT.id) continue;
            if (removeUntaxable && (nation.hasProtection())) continue;
            if (removeInactive && (nation.lastActiveMs() < activeCutoff)) continue;
            if ((removeUntaxable || removeInactive) && nation.isVacation()) continue;
            nationsByAllianceFiltered.computeIfAbsent(aaId, f -> new ObjectArrayList<>()).add(nation);
            // merge nation.getScore() into scoreMap
            scoreMap.merge(aaId, nation.getScore(), Double::sum);
        }
        if (sortByScore) {
            IntArrayList aaIds = new IntArrayList(scoreMap.keySet());
            aaIds.sort((IntComparator) (id1, id2) -> Double.compare(scoreMap.get(id2), scoreMap.get(id1)));
            Int2ObjectLinkedOpenHashMap<List<DBNation>> sortedMap = new Int2ObjectLinkedOpenHashMap<>(nationsByAllianceFiltered.size());
            for (int aaId : aaIds) {
                sortedMap.put(aaId, nationsByAllianceFiltered.get(aaId));
            }
            return sortedMap;
        } else {
            return nationsByAllianceFiltered;
        }
    }

    public Map<Integer, List<DBNation>> getNationsByAlliance(boolean removeInactive, boolean removeApplicants, boolean removeVM, boolean sortByScore) {
        long activeCutoff = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(7200);
        long turnNow = TimeUtil.getHour();

        Predicate<DBNation> filter = new Predicate<DBNation>() {
            @Override
            public boolean test(DBNation nation) {
                if (removeApplicants && nation.getPositionEnum().id <= Rank.APPLICANT.id) return false;
                if (removeInactive && (nation.lastActiveMs() < activeCutoff)) return false;
                if (removeVM && nation.isVacation()) return false;
                return true;
            }
        };
        return getNationsByAlliance(filter, sortByScore);
    }

    public Map<DBAlliance, Integer> getAllianceRanks(Predicate<DBNation> filter, boolean sortByScore) {
        Map<Integer, List<DBNation>> nations = getNationsByAlliance(filter, sortByScore);
        Map<DBAlliance, Integer> ranks = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<DBNation>> entry : nations.entrySet()) {
            DBAlliance alliance = DBAlliance.getOrCreate(entry.getKey());
            ranks.put(alliance, ranks.size() + 1);
        }
        return ranks;
    }

    public Map<Integer, List<DBNation>> getNationsByAlliance(Predicate<DBNation> filter, boolean sortByScore) {
        final Int2DoubleMap scoreMap = new Int2DoubleOpenHashMap();
        Int2ObjectOpenHashMap<List<DBNation>> nationsByAllianceFiltered = new Int2ObjectOpenHashMap<>();
        synchronized (nationsByAlliance) {
            for (Map.Entry<Integer, Map<Integer, DBNation>> entry : nationsByAlliance.entrySet()) {
                int aaId = entry.getKey();
                Map<Integer, DBNation> nationMap = entry.getValue();
                double score = 0;
                for (DBNation nation : nationMap.values()) {
                    if (filter != null && !filter.test(nation)) continue;
                    score += nation.getScore();
                    nationsByAllianceFiltered.computeIfAbsent(aaId, f -> new ObjectArrayList<>()).add(nation);
                }
                if (score > 0) {
                    scoreMap.put(aaId, score);
                }
            }
        }
        return sortByScore ? sortByScore(scoreMap, nationsByAllianceFiltered) : nationsByAllianceFiltered;
    }

    private Int2ObjectLinkedOpenHashMap<List<DBNation>> sortByScore(Int2DoubleMap scoreMap, Int2ObjectOpenHashMap<List<DBNation>> nationsByAllianceFiltered) {
        IntArrayList aaIds = new IntArrayList(scoreMap.keySet());
        aaIds.sort((IntComparator) (id1, id2) -> Double.compare(scoreMap.get(id2), scoreMap.get(id1)));
        Int2ObjectLinkedOpenHashMap<List<DBNation>> sortedMap = new Int2ObjectLinkedOpenHashMap<>(nationsByAllianceFiltered.size());
        for (int aaId : aaIds) {
            sortedMap.put(aaId, nationsByAllianceFiltered.get(aaId));
        }
        return sortedMap;
    }

    public void addRemove(int nationId, int fromAA, int toAA, Rank fromRank, Rank toRank, long time) {
        update("INSERT INTO `KICKS2`(`nation`, `from_aa`, `to_aa`, `from_rank`, `to_rank`, `date`) VALUES(?, ?, ?, ?, ?, ?)", (ThrowingConsumer<PreparedStatement>) stmt -> {
            stmt.setInt(1, nationId);
            stmt.setInt(2, fromAA);
            stmt.setInt(3, toAA);
            stmt.setInt(4, fromRank.ordinal());
            stmt.setInt(5, toRank.ordinal());
            stmt.setLong(6, time);
        });
    }

    public AllianceChange getPreviousAlliance(int nationId, int currentAA) {
        String query = "select * FROM KICKS2 WHERE nation = ? AND from_aa != 0 AND from_aa != ? ORDER BY date DESC LIMIT 1";
        try (PreparedStatement stmt = prepareQuery(query)) {
            stmt.setInt(1, nationId);
            stmt.setInt(2, currentAA);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    return new AllianceChange(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public long getAllianceApplicantSeniorityTimestamp(DBNation nation, Long snapshotDate) {
        if (nation.getAlliance_id() == 0) return Long.MAX_VALUE;
        try (PreparedStatement stmt = prepareQuery("select * FROM KICKS2 WHERE nation = ? " + (snapshotDate != null ? "AND DATE < " + snapshotDate : "") + " AND from_aa != ? ORDER BY date DESC LIMIT 1")) {
            stmt.setInt(1, nation.getNation_id());
            stmt.setInt(2, nation.getAlliance_id());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    return rs.getLong("date");
                }
            }
            return Long.MAX_VALUE;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public long getAllianceMemberSeniorityTimestamp(DBNation nation, Long snapshotDate) {
        if (nation.getPosition() < Rank.MEMBER.id) return Long.MAX_VALUE;
        try (PreparedStatement stmt = prepareQuery("select * FROM KICKS2 WHERE nation = ? " + (snapshotDate != null ? "AND DATE < " + snapshotDate : "") + " ORDER BY date DESC LIMIT 1")) {
            stmt.setInt(1, nation.getNation_id());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    return rs.getLong("date");
                }
            }
            return Long.MAX_VALUE;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public List<AllianceChange> getRemovesByNation(int nationId) {
        return getRemovesByNation(nationId, null);
    }
    public List<AllianceChange> getRemovesByNation(int nationId, Long date) {
        try (PreparedStatement stmt = prepareQuery("select * FROM KICKS2 WHERE nation = ? " + (date != null && date != 0 ? "AND date > ? " : "") + "ORDER BY date DESC")) {
            stmt.setInt(1, nationId);
            if (date != null) {
                stmt.setLong(2, date);
            }

            List<AllianceChange> list = new ObjectArrayList<>();

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new AllianceChange(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Map<Integer, List<AllianceChange>> getRemovesByAlliances(long cutoff) {
        String query = "SELECT * FROM KICKS2 " + (cutoff > 0 ? "WHERE date > ? " : "") + "ORDER BY date DESC";
        Map<Integer, List<AllianceChange>> resultsByAA = new Int2ObjectOpenHashMap<>();
        try (PreparedStatement stmt = prepareQuery(query)) {
            if (cutoff > 0) {
                stmt.setLong(1, cutoff);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AllianceChange change = new AllianceChange(rs);
                    if (change.getFromId() != 0) {
                        resultsByAA.computeIfAbsent(change.getFromId(), k -> new ObjectArrayList<>()).add(change);
                    }
                    if (change.getToId() != 0) {
                        resultsByAA.computeIfAbsent(change.getToId(), k -> new ObjectArrayList<>()).add(change);
                    }
                }
            }
            return resultsByAA;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    public Map<Integer, List<AllianceChange>> getRemovesByAlliances(Set<Integer> alliances, long cutoff) {
        if (alliances.isEmpty()) return Collections.emptyMap();
        if (alliances.size() == 1) {
            int alliance = alliances.iterator().next();
            List<AllianceChange> result = getRemovesByAlliance(alliance, cutoff);
            return Collections.singletonMap(alliance, result);
        } else {
            Map<Integer, List<AllianceChange>> resultsByAA = new Int2ObjectOpenHashMap<>();

            Set<Integer> fastMap = new IntOpenHashSet(alliances);
            List<Integer> alliancesSorted = new ArrayList<>(alliances);
            alliancesSorted.sort(Comparator.naturalOrder());
            String query = "SELECT * FROM KICKS2 WHERE (from_aa IN " + StringMan.getString(alliancesSorted) + " OR to_aa IN " + StringMan.getString(alliancesSorted) + ")" + (cutoff > 0 ? " AND date > ? " : "") + "ORDER BY date DESC";

            try (PreparedStatement stmt = getConnection().prepareStatement(query)) {
                if (cutoff > 0) {
                    stmt.setLong(1, cutoff);
                }
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {

                    AllianceChange change = new AllianceChange(rs);
                    if (fastMap.contains(change.getFromId())) {
                        resultsByAA.computeIfAbsent(change.getFromId(), k -> new ObjectArrayList<>()).add(change);
                    }
                    if (fastMap.contains(change.getToId())) {
                        resultsByAA.computeIfAbsent(change.getToId(), k -> new ObjectArrayList<>()).add(change);
                    }
                }
                return resultsByAA;
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    public List<AllianceChange> getRemovesByAlliance(int allianceId) {
        return getRemovesByAlliance(allianceId, 0L);
    }

    public List<AllianceChange> getRemovesByAlliance(int allianceId, long cutoff) {
        List<AllianceChange> list = new ObjectArrayList<>();
        try (PreparedStatement stmt = prepareQuery("select * FROM KICKS2 WHERE (from_aa = ? OR to_aa = ?) " + (cutoff > 0 ? "AND date > ? " : "") + "ORDER BY date DESC")) {
            stmt.setInt(1, allianceId);
            stmt.setInt(2, allianceId);
            if (cutoff > 0) {
                stmt.setLong(3, cutoff);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AllianceChange change = new AllianceChange(rs);
                    list.add(change);
                }
            }
            return list;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void deleteNations(Set<Integer> ids, Consumer<Event> eventConsumer) {
        for (int id : new HashSet<>(ids)) {
            DBNation nation;
            synchronized (nationsById) {
                nation = nationsById.get(id);
            }
            if (nation != null) {
                if (nation.getDate() + TimeUnit.MINUTES.toMillis(30) > System.currentTimeMillis()) {
                    // don't delete new nations
                    ids.remove(id);
                    continue;
                }
                synchronized (nationsById) {
                    nationsById.remove(id);
                }
                if (nation.getAlliance_id() != 0) {
                    synchronized (nationsByAlliance) {
                        nationsByAlliance.getOrDefault(nation.getAlliance_id(), Collections.EMPTY_MAP)
                                .remove(id);
                    }
                }
                if (eventConsumer != null) eventConsumer.accept(new NationDeleteEvent(nation));
            } else {
                ids.remove(id);
            }
        }
        if (ids.isEmpty()) return;
        deleteNationsInDB(ids);
    }

    private synchronized void deleteNationPrivateInDb(Set<Integer> ids) {
        if (ids.size() == 1) {
            int id = ids.iterator().next();
            executeStmt("DELETE FROM " + (new NationPrivate().getTableName()) + " WHERE nation_id = " + id);
        } else {
            String idStr = StringMan.getString(ids);
            executeStmt("DELETE FROM " + (new NationPrivate().getTableName()) + " WHERE `nation_id` in " + idStr);
        }
    }

    private synchronized void deleteNationsInDB(Set<Integer> ids) {
        if (ids.size() == 1) {
            int id = ids.iterator().next();
            executeStmt("DELETE FROM nations WHERE nation_id = " + id);
        } else {
            String idStr = StringMan.getString(ids);
            executeStmt("DELETE FROM nations WHERE `nation_id` in " + idStr);
        }
        deleteNationPrivateInDb(ids);
    }

    private synchronized void deleteTreatiesInDB(Set<Long> ids) {
        if (ids.size() == 1) {
            long id = ids.iterator().next();
            executeStmt("DELETE FROM TREATIES WHERE id = " + id);
        } else {
            String query = "DELETE FROM TREATIES WHERE `id` in " + StringMan.getString(ids);
            executeStmt(query);
        }
    }

    public void saveNationPrivate(NationPrivate nationPrivate) {
        save(List.of(nationPrivate));
    }

    public boolean updateNation(DBNation nation, Consumer<Event> eventConsumer) {
        DnsApi api = Locutus.imp().getV3();
        return updateNation(api, nation, eventConsumer);
    }

    public boolean updateNation(DnsApi api, DBNation nation, Consumer<Event> eventConsumer) {
        List<Nation> record = api.nation(nation.getId()).call();
        if (record != null && !record.isEmpty()) {
            Nation nationRecord = record.get(0);
            boolean changed = nation.update(this, nationRecord, eventConsumer);
            save(List.of(nation));
            return changed;
        }
        return false;
    }

    public void saveAlliancePrivate(AlliancePrivate alliancePrivate) {
        save(List.of(alliancePrivate));
    }
}

package link.locutus.discord.db.conflict;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import link.locutus.discord.Locutus;
import link.locutus.discord.commands.manager.v2.binding.annotation.Command;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.DBWar;
import link.locutus.discord.db.entities.conflict.ConflictCategory;
import link.locutus.discord.db.entities.conflict.ConflictColumn;
import link.locutus.discord.db.entities.conflict.ConflictMetric;
import link.locutus.discord.db.entities.conflict.DamageStatGroup;
import link.locutus.discord.pnw.AllianceList;
import link.locutus.discord.util.TimeUtil;
import link.locutus.discord.web.AwsManager;
import link.locutus.discord.web.JteUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Conflict {
    private final int id;
    private final int ordinal;
    private ConflictCategory category;
    private String wiki;
    private String name;
    private long hourStart;
    private long hourEnd;
    private final Map<Integer, Long> startTime = new Int2ObjectOpenHashMap<>();
    private final Map<Integer, Long> endTime = new Int2ObjectOpenHashMap<>();
    private final Map<Integer, Map<Integer, DamageStatGroup>> warsVsAlliance = new Int2ObjectOpenHashMap<>();
    private final CoalitionSide coalition1;
    private final CoalitionSide coalition2;
    private byte[] flatStatsGzip;
    private byte[] graphStatsGzip;
    private volatile boolean dirtyWars = false;
    private volatile boolean dirtyJson = true;
    private String casusBelli = "";
    private String statusDesc = "";
    private long createdByServer;

    public void clearWarData() {
        warsVsAlliance.clear();;
        coalition1.clearWarData();
        coalition2.clearWarData();
        dirtyWars = true;
        dirtyJson = true;
    }

    public Conflict(int id, int ordinal, long createdByServer, ConflictCategory category, String name, String col1, String col2, String wiki, String cb, String status, long hourStart, long hourEnd) {
        this.id = id;
        this.ordinal = ordinal;
        this.createdByServer = createdByServer;
        this.category = category;
        this.name = name;
        this.wiki = wiki == null ? "" : wiki;
        this.casusBelli = cb == null ? "" : cb;
        this.statusDesc = status == null ? "" : status;
        this.hourStart = hourStart;
        this.hourEnd = hourEnd;
        this.coalition1 = new CoalitionSide(this, col1, true);
        this.coalition2 = new CoalitionSide(this, col2, false);
        this.coalition1.setOther(coalition2);
        this.coalition2.setOther(coalition1);
    }

    public void setCasusBelli(String casusBelli) {
        this.casusBelli = casusBelli;
        dirtyJson = true;
        getManager().setCb(id, casusBelli);
    }

    public void setStatus(String status) {
        this.statusDesc = status;
        dirtyJson = true;
        getManager().setStatus(id, status);
    }

    @Command(desc = "The conflict category")
    public ConflictCategory getCategory() {
        return category;
    }

    public void setCategory(ConflictCategory category) {
        this.category = category;
        dirtyJson = true;
        getManager().updateConflictCategory(id, category);
    }

    public void setWiki(String wiki) {
        this.wiki = wiki;
        dirtyJson = true;
        getManager().updateConflictWiki(id, wiki);
    }

    public void setName(String name, boolean isPrimary) {
        if (isPrimary) {
            coalition1.setName(name);
        } else {
            coalition2.setName(name);
        }
        dirtyJson = true;
        getManager().updateConflictName(id, name, isPrimary);
    }

    // TODO FIXME :||remove
//    public void updateGraphsLegacy(ConflictManager manager) throws IOException, ParseException {
//        Set<Integer> nationIds = new IntOpenHashSet();
//        nationIds.addAll(coalition1.getNationIds());
//        nationIds.addAll(coalition2.getNationIds());
//
//        long startMs = TimeUtil.getTimeFromTurn(turnStart);
//        long endMs = turnEnd == Long.MAX_VALUE ? Long.MAX_VALUE : TimeUtil.getTimeFromTurn(turnEnd);
//
//        Map<Long, Map<Integer, Map<MilitaryUnit, Integer>>> milHistory = Locutus.imp().getNationDB().getMilitaryHistoryByTurn(nationIds, startMs, endMs);
//        Map<Long, Map<Integer, DBNation>> nationsByDay = new HashMap<>();
//
//        DataDumpParser parser = Locutus.imp().getDataDumper(true);
//
//        long dayStart = TimeUtil.getDayFromTurn(getStartTurn());
//        long dayEnd = getEndTurn() == Long.MAX_VALUE ? Long.MAX_VALUE : TimeUtil.getDayFromTurn(getEndTurn() + 23);
//
//        parser.iterateAll(day -> {
//            if (day >= dayStart && day <= dayEnd) {
//                return true;
//            }
//            return false;
//        }, null, null,
//                (day, header) -> {
//            int nationId = header.nation_id.get();
//            if (!nationIds.contains(nationId)) {
//                Rank position = header.alliance_position.get();
//                if (position.id <= Rank.APPLICANT.id) return;
//                int allianceId = header.alliance_id.get();
//                if (!coalition1.hasAlliance(allianceId) && !coalition2.hasAlliance(allianceId)) return;
//            }
////            long currentTimeMs = TimeUtil.getTimeFromDay(day);
//            DBNationSnapshot nation = header.getNation(f -> true, f -> true, false, true, true);
//            if (nation != null) {
//                nationsByDay.computeIfAbsent(day, k -> new Int2ObjectOpenHashMap<>()).put(nation.getId(), nation);
//            }
//        }, (day, cityHeader) -> {
//            Map<Integer, DBNation> nationMap = nationsByDay.get(day);
//            if (nationMap == null) return;
//            int nationId = cityHeader.nation_id.get();
//            DBNationSnapshot nation = (DBNationSnapshot) nationMap.get(nationId);
//            if (nation == null) return;
//            DBCity city = cityHeader.getCity();
//            nation.addCity(city);
//        }, null);
//
//        long currentTurn = TimeUtil.getTurn();
//
//        long lastDay = nationsByDay.keySet().stream().max(Long::compareTo).orElse(0L);
//        Map<Integer, DBNation> latest;
//        if (turnEnd == Long.MAX_VALUE || lastDay == 0 || TimeUtil.getTimeFromTurn(turnEnd) > TimeUtil.getTimeFromDay(lastDay)) {
//            latest = new Int2ObjectOpenHashMap<>();
//            for (int id : nationIds) {
//                DBNation nation = DBNation.getById(id);
//                if (nation != null) {
//                    latest.put(id, nation);
//                }
//            }
//            long nextDay = TimeUtil.getDayFromTurn(TimeUtil.getTurn() + 23);
//            nationsByDay.put(nextDay, latest);
//        } else {
//            latest = nationsByDay.get(lastDay);
//        }
//
//        // save day data
//        {
//            for (Map.Entry<Long, Map<Integer, DBNation>> entry : nationsByDay.entrySet()) {
//                long day = entry.getKey();
//                long dayStartTurn = TimeUtil.getTurn(TimeUtil.getTimeFromDay(day));
//                long dayEndTurn = dayStartTurn + 23;
//                Map<Integer, DBNation> nations = entry.getValue();
//                Set<DBNation> col1Nations = new ObjectOpenHashSet<>();
//                Set<DBNation> col2Nations = new ObjectOpenHashSet<>();
//                for (Map.Entry<Integer, DBNation> nationEntry : nations.entrySet()) {
//                    DBNation nation = nationEntry.getValue();
//                    if (nation.isVacation() || nation.getPositionEnum().id <= Rank.APPLICANT.id) continue;
//                    if (nation instanceof DBNationSnapshot snap && !snap.hasCityData()) continue;
//                    int aaId = nation.getAlliance_id();
//                    long startTurn = getStartTurn(aaId);
//                    if (startTurn != turnStart && startTurn > dayEndTurn) continue;
//                    long endTurn = getEndTurn(aaId);
//                    if (endTurn != turnEnd && endTurn < dayStartTurn) continue;
//                    if (coalition1.hasAlliance(aaId)) {
//                        col1Nations.add(nation);
//                    } else if (coalition2.hasAlliance(aaId)) {
//                        col2Nations.add(nation);
//                    }
//                }
//                coalition1.updateDayTierGraph(manager, day, col1Nations, true, false);
//                coalition2.updateDayTierGraph(manager, day, col2Nations, true, false);
//            }
//        }
//
//        {
//            MilitaryUnit[] units = {MilitaryUnit.SOLDIER, MilitaryUnit.TANK, MilitaryUnit.AIRCRAFT, MilitaryUnit.SHIP};
//
//            long latestTurn = Math.min(getEndTurn(), currentTurn);
//            long latestDay = TimeUtil.getDayFromTurn(latestTurn);
//            Map<Integer, Map<MilitaryUnit, Integer>> milCountByNation = new Int2ObjectOpenHashMap<>();
//
//            Consumer<Map<Integer, DBNation>> setMilCount = (nations) -> {
//                for (Map.Entry<Integer, DBNation> entry : nations.entrySet()) {
//                    DBNation nation = entry.getValue();
//                    Map<MilitaryUnit, Integer> counts = new EnumMap<>(MilitaryUnit.class);
//                    for (MilitaryUnit unit : MilitaryUnit.values()) {
//                        int count = nation.getUnits(unit);
//                        if (count > 0) {
//                            counts.put(unit, count);
//                        }
//                    }
//                    milCountByNation.put(entry.getKey(), counts);
//                }
//            };
//            setMilCount.accept(latest);
//
//            for (long turn = latestTurn; turn >= turnStart; turn--) {
//                long newDay = TimeUtil.getDay(TimeUtil.getTimeFromTurn(turn));
//                boolean setViaParser = false;
//                if (turn != latestTurn) {
//                    if (newDay != latestDay) {
//                        latestDay = newDay;
//                        Map<Integer, DBNation> newLatest = nationsByDay.get(latestDay);
//                        if (newLatest != null) {
//                            latest = newLatest;
//                            milCountByNation.clear();
//                            setMilCount.accept(latest);
//                            setViaParser = true;
//                        }
//                    }
//                    if (!setViaParser) {
//                        Map<Integer, Map<MilitaryUnit, Integer>> turnMilHistory = milHistory.get(turn);
//                        if (turnMilHistory != null) {
//                            for (Map.Entry<Integer, Map<MilitaryUnit, Integer>> entry : turnMilHistory.entrySet()) {
//                                int nationId = entry.getKey();
//                                Map<MilitaryUnit, Integer> counts = entry.getValue();
//                                for (Map.Entry<MilitaryUnit, Integer> milEntry : counts.entrySet()) {
//                                    milCountByNation.computeIfAbsent(nationId, k -> new EnumMap<>(MilitaryUnit.class)).put(milEntry.getKey(), milEntry.getValue());
//                                }
//                            }
//                        }
//                    }
//                }
//
//                Map<Integer, Map<Byte, Map<MilitaryUnit, Integer>>> unitsByCityCol1 = new Int2ObjectOpenHashMap<>();
//                Map<Integer, Map<Byte, Map<MilitaryUnit, Integer>>> unitsByCityCol2 = new Int2ObjectOpenHashMap<>();
//
//                long finalTurn = turn;
//                Set<Integer> allowedNations = latest.values().stream().filter(f -> {
//                    if (f.getPositionEnum().id <= Rank.APPLICANT.id) return false;
//                    int allianceId = f.getAlliance_id();
//                    if (!coalition1.hasAlliance(allianceId) && !coalition2.hasAlliance(allianceId)) return false;
//                    if (f.isVacation()) return false;
//                    long turnStart = getStartTurn(allianceId);
//                    long turnEnd = getEndTurn(allianceId);
//                    return finalTurn >= turnStart && finalTurn <= turnEnd;
//                }).map(DBNation::getNation_id).collect(Collectors.toSet());
//
//                for (int id : allowedNations) {
//                    DBNation nation = latest.get(id);
//                    int aaId = nation.getAlliance_id();
//                    boolean isPrimary = coalition1.hasAlliance(aaId);
//                    Map<Byte, Map<MilitaryUnit, Integer>> unitsByCity = (isPrimary ? unitsByCityCol1 : unitsByCityCol2).computeIfAbsent(aaId, f -> new Byte2ObjectOpenHashMap<>());
//                    byte cities = (byte) nation.getCities();
//                    for (MilitaryUnit unit : units) {
//                        int count = nation.getUnits(unit);
//                        unitsByCity.computeIfAbsent(cities, k -> new EnumMap<>(MilitaryUnit.class)).merge(unit, count, Integer::sum);
//                    }
//                }
//
//                coalition1.updateTurnTierGraph(manager, turn, unitsByCityCol1, true, false);
//                coalition2.updateTurnTierGraph(manager, turn, unitsByCityCol2, true, false);
//            }
//        }
//
//        manager.deleteGraphData(getId());
//        List<ConflictMetric.Entry> entries = getGraphEntries();
//        manager.addGraphData(entries);
//    }

    public List<ConflictMetric.Entry> getGraphEntries() {
        List<ConflictMetric.Entry> entries = new ObjectArrayList<>();
        entries.addAll(coalition1.getGraphEntries());
        entries.addAll(coalition2.getGraphEntries());
        return entries;
    }

    public synchronized byte[] getGraphPsonGzip(ConflictManager manager) {
        Map<String, Object> root = new Object2ObjectLinkedOpenHashMap<>();
        root.put("name", getName());
        root.put("start", TimeUtil.getTimeFromHour(hourStart));
        root.put("end", hourEnd == Long.MAX_VALUE ? -1 : TimeUtil.getTimeFromHour(hourEnd));

        List<String> metricNames = new ObjectArrayList<>();

        List<Integer> metricsDay = new IntArrayList();
        List<Integer> metricsTurn = new IntArrayList();

        for (ConflictMetric metric : ConflictMetric.values) {
            (metric.isDay() ? metricsDay : metricsTurn).add(metricNames.size());
            metricNames.add(metric.name().toLowerCase(Locale.ROOT));
        }
        Map<ConflictColumn, Function<DamageStatGroup, Object>> damageHeaders = DamageStatGroup.createRanking();
        List<ConflictColumn> columns = new ObjectArrayList<>(damageHeaders.keySet());
        List<Function<DamageStatGroup, Object>> valueFuncs = columns.stream().map(damageHeaders::get).toList();

        int columnMetricOffset = metricNames.size();

        for (ConflictColumn column : columns) {
            metricsDay.add(metricNames.size());
            String defPrefix = column.isCount() ? "def:" : "loss:";
            metricNames.add(defPrefix + column.getName());
            metricsDay.add(metricNames.size());
            String attPrefix = column.isCount() ? "off:" : "dealt:";
            metricNames.add(attPrefix + column.getName());
        }
        root.put("metric_names", metricNames);
        root.put("metrics_turn", metricsTurn);
        root.put("metrics_day", metricsDay);

        List<Map<String, Object>> coalitions = new ObjectArrayList<>();
        coalitions.add(coalition1.toGraphMap(manager, metricsTurn, metricsDay, valueFuncs, columnMetricOffset));
        coalitions.add(coalition2.toGraphMap(manager, metricsTurn, metricsDay, valueFuncs, columnMetricOffset));

        root.put("coalitions", coalitions);
        return graphStatsGzip = JteUtil.compress(JteUtil.toBinary(root));
    }

    private Map<String, Object> warsVsAllianceJson() {
        Map<ConflictColumn, Function<DamageStatGroup, Object>> combined = DamageStatGroup.createRanking();
        List<Map.Entry<ConflictColumn, Function<DamageStatGroup, Object>>> combinedList = new ObjectArrayList<>(combined.entrySet());

        Map<String, Object> result = new Object2ObjectLinkedOpenHashMap<>();
        // headers
        result.put("headers", combinedList.stream().map(e -> e.getKey().getName()).toList());
        result.put("header_desc", combinedList.stream().map(e -> e.getKey().getDescription()).toList());
        // alliance ids
        List<Integer> aaIdsFull = new IntArrayList();
        aaIdsFull.addAll(coalition1.getAllianceIdsSorted());
        aaIdsFull.addAll(coalition2.getAllianceIdsSorted());

        List<List<List<Long>>> tables = new ArrayList<>();
        for (Map.Entry<ConflictColumn, Function<DamageStatGroup, Object>> entry : combinedList) {
            List<List<Long>> table = new ArrayList<>();
            Function<DamageStatGroup, Object> function = entry.getValue();

            for (int aaId1 : aaIdsFull) {
                List<Long> row = new LongArrayList();
                Map<Integer, DamageStatGroup> statsByAA = warsVsAlliance.get(aaId1);
                if (statsByAA != null) {
                    for (int aaId2 : aaIdsFull) {
                        DamageStatGroup stats = statsByAA.get(aaId2);
                        if (stats == null) {
                            row.add(0L);
                        } else {
                            Object value = function.apply(stats);
                            row.add(((Number) value).longValue());
                        }
                    }
                }
                table.add(row);
            }
            tables.add(table);
        }
        result.put("data", tables);
        return result;
    }

    @Command(desc = "The status of the conflict")
    public String getStatusDesc() {
        return statusDesc;
    }

    @Command(desc = "The casus belli of the conflict")
    public String getCasusBelli() {
        return casusBelli;
    }

    public synchronized byte[] getPsonGzip(ConflictManager manager) {
        if (dirtyWars) {
            // TODO
        }
        if (dirtyJson || flatStatsGzip == null) {
            try {
                Map<String, Object> root = new Object2ObjectLinkedOpenHashMap<>();
                root.put("name", getName());
                root.put("start", TimeUtil.getTimeFromHour(hourStart));
                root.put("end", hourEnd == Long.MAX_VALUE ? -1 : TimeUtil.getTimeFromHour(hourEnd));
                root.put("wiki", wiki);
                root.put("status", statusDesc);
                root.put("cb", casusBelli);

                List<Object> coalitions = new ObjectArrayList<>();
                coalitions.add(coalition1.toMap(manager));
                coalitions.add(coalition2.toMap(manager));
                root.put("coalitions", coalitions);

                Map<ConflictColumn, Function<DamageStatGroup, Object>> damageHeader = DamageStatGroup.createHeader();
                root.put("damage_header", new ObjectArrayList<>(damageHeader.keySet().stream().map(ConflictColumn::getName).toList()));
                root.put("header_desc", new ObjectArrayList<>(damageHeader.keySet().stream().map(ConflictColumn::getDescription).toList()));
                root.put("header_group", new ObjectArrayList<>(damageHeader.keySet().stream().map(f -> f.getType().name()).toList()));
                root.put("header_type", new ObjectArrayList<>(damageHeader.keySet().stream().map(f -> f.isCount() ? 1 : 0).toList()));
                root.put("war_web", warsVsAllianceJson());
                root.put("update_ms", System.currentTimeMillis());
                byte[] compressed = JteUtil.compress(JteUtil.toBinary(root));
                flatStatsGzip = compressed;
                return compressed;
            } finally {
                dirtyJson = false;
            }
        }
        return flatStatsGzip;
    }

    public long getStartHour(int allianceId) {
        return startTime.getOrDefault(allianceId, hourStart);
    }
    public long getEndHour(int allianceId) {
        return endTime.getOrDefault(allianceId, hourEnd);
    }

    private CoalitionSide getCoalition(int aaId1, int aaId2, long hour) {
        if (coalition1.hasAlliance(aaId1)) {
            if (coalition2.hasAlliance(aaId2) &&
                    getStartHour(aaId1) <= hour && getEndHour(aaId1) > hour &&
                    getStartHour(aaId2) <= hour && getEndHour(aaId2) > hour) {
                    return coalition1;
            }
        } else if (coalition2.hasAlliance(aaId1)) {
            if (coalition1.hasAlliance(aaId2) &&
                    getStartHour(aaId1) <= hour && getEndHour(aaId1) > hour &&
                    getStartHour(aaId2) <= hour && getEndHour(aaId2) > hour) {
                return coalition2;
            }
        }
        return null;
    }

    private DamageStatGroup getWarWebEntry(int aaId1, int aaId2) {
        return warsVsAlliance.computeIfAbsent(aaId1, k -> new Int2ObjectOpenHashMap<>()).computeIfAbsent(aaId2, k -> new DamageStatGroup());
    }

    public boolean updateWar(DBWar previous, DBWar current, long hour) {
        CoalitionSide side = getCoalition(current.getAttacker_aa(), current.getDefender_aa(), hour);
        if (side == null) return false;

        CoalitionSide otherSide = side.getOther();
        side.updateWar(previous, current, true);
        otherSide.updateWar(previous, current, false);

        if (previous == null) {
            getWarWebEntry(current.getAttacker_aa(), current.getDefender_aa()).newWar(current, true);
        }

        dirtyJson = true;
        return true;
    }

    private void setParticipantTime(int allianceId, long start, long end) {
        if (start > 0) {
            startTime.put(allianceId, start);
        }
        if (end != Long.MAX_VALUE) {
            endTime.put(allianceId, end);
        }
        dirtyJson = true;
    }

    private ConflictManager getManager() {
        return Locutus.imp().getWarDb().getConflicts();
    }

    public Conflict setName(String name) {
        this.name = name;
        getManager().updateConflictName(id, name);
        dirtyJson = true;
        return this;
    }

    public Conflict setStart(long time) {
        long newTurn = TimeUtil.getHour(time);
        if (newTurn == hourStart) return this;
        this.hourStart = newTurn;
        getManager().updateConflict(this, hourStart, hourEnd);
        dirtyJson = true;
        return this;
    }

    public Conflict setEnd(long time) {
        long newTurn = time == Long.MAX_VALUE ? time : TimeUtil.getHour(time) + 1;
        if (newTurn == hourEnd) return this;
        this.hourEnd = newTurn;
        getManager().updateConflict(this, hourStart, hourEnd);
        dirtyJson = true;
        return this;
    }

    public Conflict addParticipant(int allianceId, boolean side, Long start, Long end) {
        return addParticipant(allianceId, side, true, start, end);
    }

    public Conflict addParticipant(int allianceId, boolean side, boolean save, Long start, Long end) {
        if (start != null && start > 0 && start != Long.MAX_VALUE) {
            startTime.put(allianceId, start);
        } else {
            startTime.remove(allianceId);
        }
        if (end != null && end != Long.MAX_VALUE) {
            endTime.put(allianceId, end);
        } else {
            endTime.remove(allianceId);
        }
        if (side) coalition1.add(allianceId);
        else coalition2.add(allianceId);
        if (save) {
            getManager().addParticipant(this, allianceId, side, startTime.getOrDefault(allianceId, 0L), endTime.getOrDefault(allianceId, Long.MAX_VALUE));
            dirtyWars = true;
        }
        dirtyJson = true;
        return this;
    }

    public Conflict removeParticipant(int allianceId) {
        coalition1.remove(allianceId);
        coalition2.remove(allianceId);
        startTime.remove(allianceId);
        endTime.remove(allianceId);
        getManager().removeParticipant(this, allianceId);
        dirtyWars = true;
        dirtyJson = true;
        return this;
    }

    @Command(desc = "The id of the conflict in the database and website url")
    public int getId() {
        return id;
    }

    @Command(desc = "The url of this conflict (if pushed live)")
    public String getUrl() {
        return Settings.INSTANCE.WEB.S3.SITE + "/conflict?id=" + id;
    }

    @Command(desc = "The ordinal of the conflict (load order)")
    public int getOrdinal() {
        return ordinal;
    }

    @Command(desc = "The name of the conflict")
    public String getName() {
        return name;
    }

    @Command(desc = "The turn the conflict started\n" +
            "Measured in 2h turns from unix epoch")
    public long getStartHour() {
        return hourStart;
    }

    @Command(desc = "The turn the conflict ends\n" +
            "Measured in 2h turns from unix epoch\n" +
            "If the conflict is ongoing, this will be Long.MAX_VALUE")
    public long getEndHour() {
        return hourEnd;
    }

    public Set<Integer> getCoalition1() {
        return coalition1.getAllianceIds();
    }

    public Set<Integer> getCoalition2() {
        return coalition2.getAllianceIds();
    }

    @Command(desc = "If an alliance is a participant in the conflict")
    public boolean isParticipant(DBAlliance alliance) {
        return coalition1.hasAlliance(alliance.getId()) || coalition2.hasAlliance(alliance.getId());
    }

    @Command(desc = "A number representing the side an alliance is on in the conflict\n" +
            "0 = No side, 1 = Primary/Coalition 1, 2 = Secondary/Coalition 2")
    public int getSide(DBAlliance alliance) {
        if (coalition1.hasAlliance(alliance.getId())) return 1;
        if (coalition2.hasAlliance(alliance.getId())) return 2;
        return 0;
    }

    @Command(desc = "The start turn of the conflict in milliseconds since unix epoch")
    public long getStartMS() {
        return TimeUtil.getTimeFromHour(hourStart);
    }

    @Command(desc = "The end turn of the conflict in milliseconds since unix epoch")
    public long getEndMS() {
        return hourEnd == Long.MAX_VALUE ? Long.MAX_VALUE : TimeUtil.getTimeFromHour(hourEnd);
    }

    @Command(desc = "The alliance list of for the first coalition")
    public AllianceList getCol1List() {
        return new AllianceList(getCoalition1());
    }

    @Command(desc = "The alliance list of for the second coalition")
    public AllianceList getCol2List() {
        return new AllianceList(getCoalition2());
    }

    @Command(desc = "The alliance list for ALL participants")
    public AllianceList getAllianceList() {
        return new AllianceList(getAllianceIds());
    }

    public Set<DBAlliance> getCoalition1Obj() {
        return coalition1.getAllianceIds().stream().map(DBAlliance::getOrCreate).collect(Collectors.toSet());
    }

    public Set<DBAlliance> getCoalition2Obj() {
        return coalition2.getAllianceIds().stream().map(DBAlliance::getOrCreate).collect(Collectors.toSet());
    }

    public CoalitionSide getSide(boolean isPrimary) {
        return isPrimary ? coalition1 : coalition2;
    }

    public Boolean isSide(int allianceId) {
        if (coalition1.hasAlliance(allianceId)) return true;
        if (coalition2.hasAlliance(allianceId)) return false;
        return null;
    }

    public Set<Integer> getAllianceIds() {
        Set<Integer> ids = new IntOpenHashSet(coalition1.getAllianceIds().size() + coalition2.getAllianceIds().size());
        ids.addAll(coalition1.getAllianceIds());
        ids.addAll(coalition2.getAllianceIds());
        return ids;
    }

    @Command(desc = "The number of active wars in the conflict")
    public int getActiveWars() {
        return coalition1.getOffensiveStats(null, false).activeWars + coalition2.getOffensiveStats(null, false).activeWars;
    }

    @Command(desc = "The number of total wars in the conflict")
    public int getTotalWars() {
        return coalition1.getOffensiveStats(null, false).totalWars + coalition2.getOffensiveStats(null, false).totalWars;
    }

    @Command(desc = "The current damage dealt between the conflict's participants, using market prices")
    public double getDamageConverted(boolean isPrimary) {
        return (isPrimary ? coalition1 : coalition2).getInflicted().getTotalConverted();
    }

    @Command(desc = "The wiki link for this conflict (or null)")
    public String getWiki() {
        return wiki;
    }

    @Command(desc = "The discord guild that created this conflict (or null)")
    public GuildDB getGuild() {
        return createdByServer <= 0 ? null : Locutus.imp().getGuildDB(createdByServer);
    }

    @Command(desc = "The id of the discord guild that created this conflict")
    public long getGuildId() {
        return createdByServer;
    }

    public boolean isActive() {
        if (this.hourEnd == Long.MAX_VALUE) return true;
        long turn = TimeUtil.getHour();
        return turn <= this.hourEnd + 96;
    }

    /**
     * Pushes the conflict to the AWS S3 bucket
     * @param manager
     * @param webIdOrNull
     * @param includeGraphs
     * @return List of URLs
     */
    public List<String> push(ConflictManager manager, String webIdOrNull, boolean includeGraphs, boolean updateIndex) {
        AwsManager aws = manager.getAws();
        if (webIdOrNull == null) {
            if (getId() == -1) throw new IllegalArgumentException("Conflict has no id");
            webIdOrNull = Integer.toString(getId());
        }
        long ttl = isActive() ? TimeUnit.MINUTES.toSeconds(1) : TimeUnit.MINUTES.toSeconds(5);
        String key = "conflicts/" + webIdOrNull + ".gzip";
        byte[] value = getPsonGzip(manager);
        aws.putObject(key, value, ttl);

        List<String> urls = new ArrayList<>();
        urls.add(aws.getLink(key));

        if (includeGraphs) {
            String graphKey = "conflicts/graphs/" + webIdOrNull + ".gzip";
            byte[] graphValue = getGraphPsonGzip(manager);
            aws.putObject(graphKey, graphValue, ttl);
            urls.add(aws.getLink(graphKey));
        }
        if (updateIndex) {
            manager.pushIndex();
        }
        return urls;
    }

    @Command(desc = "If the conflict has been updated since the last push")
    public boolean isDirty() {
        return this.dirtyJson;
    }

    public void set(Conflict wikiConflict, boolean setName) {
        if (!wikiConflict.getStatusDesc().equalsIgnoreCase(getStatusDesc())) {
            setStatus(wikiConflict.getStatusDesc());
        }
        if (!wikiConflict.getCasusBelli().equalsIgnoreCase(getCasusBelli())) {
            setCasusBelli(wikiConflict.getCasusBelli());
        }
        if (setName && !wikiConflict.getName().equalsIgnoreCase(getName())) {
            setName(wikiConflict.getName());
        }
        if (getCategory() != wikiConflict.getCategory()) {
            setCategory(wikiConflict.getCategory());
        }
        if (getAllianceIds().isEmpty()) {
            for (int aaId : wikiConflict.getCoalition1()) addParticipant(aaId, true, null, null);
            for (int aaId : wikiConflict.getCoalition2()) addParticipant(aaId, false, null, null);
        }
    }

    public void updateGraphsLegacy(ConflictManager manager) {
        // TODO FIXME :||remove
        throw new UnsupportedOperationException("Not implemented");
    }
}
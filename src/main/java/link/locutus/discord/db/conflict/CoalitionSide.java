package link.locutus.discord.db.conflict;

import it.unimi.dsi.fastutil.bytes.Byte2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2LongOpenHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteOpenHashSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import link.locutus.discord.api.types.MilitaryUnit;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.db.entities.DBWar;
import link.locutus.discord.db.entities.conflict.ConflictColumn;
import link.locutus.discord.db.entities.conflict.ConflictMetric;
import link.locutus.discord.db.entities.conflict.DamageStatGroup;
import link.locutus.discord.db.entities.conflict.DayTierGraphData;
import link.locutus.discord.db.entities.conflict.TurnTierGraphData;
import link.locutus.discord.pnw.AllianceList;
import link.locutus.discord.util.TimeUtil;
import link.locutus.discord.web.JteUtil;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CoalitionSide {
    private final Conflict parent;
    private String name;
    private final boolean isPrimary;
    private CoalitionSide otherSide;
    private final Set<Integer> coalition = new IntOpenHashSet();
    private final Map<Integer, Integer> allianceIdByNation = new Int2ObjectOpenHashMap<>();
    private final DamageStatGroup inflictedAndOffensiveStats = new DamageStatGroup();
    private final DamageStatGroup lossesAndDefensiveStats = new DamageStatGroup();
    private final Map<Integer, Map.Entry<DamageStatGroup, DamageStatGroup>> damageByAlliance = new Int2ObjectOpenHashMap<>();
    private final Map<Integer, Map.Entry<DamageStatGroup, DamageStatGroup>> damageByNation = new Int2ObjectOpenHashMap<>();

    public void clearWarData() {
        allianceIdByNation.clear();
        inflictedAndOffensiveStats.clear();
        lossesAndDefensiveStats.clear();
        damageByAlliance.clear();
        damageByNation.clear();
    }

    public Set<Integer> getNationIds() {
        return damageByNation.keySet();
    }

    public List<Integer> getAllianceIdsSorted() {
        List<Integer> allianceIds = new ArrayList<>(coalition);
        Collections.sort(allianceIds);
        return allianceIds;
    }

    public Map<String, Object> toMap(ConflictManager manager) {
        Map<String, Object> root = new Object2ObjectLinkedOpenHashMap<>();
        root.put("name", getName());
        List<Integer> allianceIds = new ArrayList<>(coalition);
        Collections.sort(allianceIds);
        List<String> allianceNames = new ObjectArrayList<>();
        for (int id : allianceIds) {
            String aaName = manager.getAllianceNameOrNull(id);
            if (aaName == null) aaName = "";
            allianceNames.add(aaName);
        }
        root.put("alliance_ids", allianceIds);
        root.put("alliance_names", allianceNames);

        List<Integer> nationIds = new IntArrayList(damageByNation.keySet());
        Collections.sort(nationIds);
        List<String> nationNames = new ObjectArrayList<>();
        for (int id : nationIds) {
            DBNation nation = DBNation.getById(id);
            String name = nation == null ? "" : nation.getName();
            nationNames.add(name);
        }
        List<Integer> nationAAs = nationIds.stream().map(allianceIdByNation::get).collect(Collectors.toList());
        root.put("nation_aa",nationAAs);
        root.put("nation_ids", nationIds);
        root.put("nation_names", nationNames);

        Map<ConflictColumn, Function<DamageStatGroup, Object>> damageHeader = DamageStatGroup.createHeader();
        List<List<Object>> damageData = new ObjectArrayList<>();
        JteUtil.writeArray(damageData, damageHeader.values(), List.of(lossesAndDefensiveStats, inflictedAndOffensiveStats));
        JteUtil.writeArray(damageData, damageHeader.values(), allianceIds, damageByAlliance);
        JteUtil.writeArray(damageData, damageHeader.values(), nationIds, damageByNation);
        root.put("damage", damageData);
        return root;
    }

    public void add(int allianceId) {
        if (allianceId == 0) throw new IllegalArgumentException("Alliance ID cannot be 0. " + this.getName());
        coalition.add(allianceId);
    }

    public void remove(int allianceId) {
        coalition.remove(allianceId);
    }

    public Set<Integer> getAllianceIds() {
        return coalition;
    }

    public String getName() {
        return name;
    }

    public DamageStatGroup getOffensiveStats(Integer id, boolean isAlliance) {
        if (id == null) return inflictedAndOffensiveStats;
        Map.Entry<DamageStatGroup, DamageStatGroup> pair = isAlliance ? damageByAlliance.get(id) : damageByNation.get(id);
        return pair == null ? null : pair.getValue();
    }
    public DamageStatGroup getDefensiveStats(Integer id, boolean isAlliance) {
        if (id == null) return lossesAndDefensiveStats;
        Map.Entry<DamageStatGroup, DamageStatGroup> pair = isAlliance ? damageByAlliance.get(id) : damageByNation.get(id);
        return pair == null ? null : pair.getKey();
    }

    public DamageStatGroup getLosses() {
        return getDamageStats(true, null, false);
    }

    public DamageStatGroup getInflicted() {
        return getDamageStats(false, null, false);
    }

    public DamageStatGroup getDamageStats(boolean isLosses, Integer id, boolean isAlliance) {
        if (id == null) return isLosses ? lossesAndDefensiveStats : inflictedAndOffensiveStats;
        Map<Integer, Map.Entry<DamageStatGroup, DamageStatGroup>> map = isAlliance ? damageByAlliance : damageByNation;
        Map.Entry<DamageStatGroup, DamageStatGroup> pair = map.computeIfAbsent(id, k -> Map.entry(new DamageStatGroup(), new DamageStatGroup()));
        return isLosses ? pair.getKey() : pair.getValue();
    }

    public CoalitionSide(Conflict parent, String name, boolean isPrimary) {
        this.parent = parent;
        this.name = name;
        this.isPrimary = isPrimary;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean hasAlliance(int aaId) {
        return coalition.contains(aaId);
    }

    public CoalitionSide getOther() {
        return otherSide;
    }

    public Conflict getParent() {
        return parent;
    }

    protected void setOther(CoalitionSide coalition) {
        this.otherSide = coalition;
    }

    private void applyAttackerStats(int allianceId, int nationId, int cities, long day, Consumer<DamageStatGroup> onEach) {
        allianceIdByNation.putIfAbsent(nationId, allianceId);
        onEach.accept(getDamageStats(false, allianceId, true));
        onEach.accept(getDamageStats(false, nationId, false));
    }

    private void applyDefenderStats(int allianceId, int nationId, int cities, long day, Consumer<DamageStatGroup> onEach) {
        allianceIdByNation.putIfAbsent(nationId, allianceId);
        onEach.accept(getDamageStats(true, allianceId, true));
        onEach.accept(getDamageStats(true, nationId, false));
    }

    public void updateWar(DBWar previous, DBWar current, boolean isAttacker) {
        int cities;
        long day = TimeUtil.getDay(current.getDate());
        int attackerAA, attackerId;
        if (isAttacker) {
            cities = current.getTier(true);
            attackerAA = current.getAttacker_aa();
            attackerId = current.getAttacker_id();
            if (previous == null) {
                inflictedAndOffensiveStats.newWar(current, true);
                applyAttackerStats(attackerAA, attackerId, cities, day, p -> p.newWar(current, true));
            } else {
                inflictedAndOffensiveStats.updateWar(previous, current, true);
                applyAttackerStats(attackerAA, attackerId, cities, day, p -> p.updateWar(previous, current, true));
            }
        } else {
            attackerAA = current.getDefender_aa();
            attackerId = current.getDefender_id();
            cities = current.getTier(false);
            if (previous == null) {
                lossesAndDefensiveStats.newWar(current, false);
                applyDefenderStats(attackerAA, attackerId, cities, day, p -> p.newWar(current, false));
            } else {
                lossesAndDefensiveStats.updateWar(previous, current, false);
                applyDefenderStats(attackerAA, attackerId, cities, day, p -> p.updateWar(previous, current, false));
            }
        }
    }

    private void trimTimeData(Map<Long, Map<Integer, Map<Integer, Map<Byte, Long>>>> turnData) {
        if (turnData.size() < 2) return;
        // Return a new map that only includes values that are different from the previous turn
        Map<Long, Map<Integer, Map<Integer, Map<Byte, Long>>>> trimmed = new Long2ObjectArrayMap<>();
        List<Long> turnsSorted = new LongArrayList(turnData.keySet());
        turnsSorted.sort(Long::compareTo);
        Map<Integer, Map<Integer, Map<Byte, Long>>> previous = new Int2ObjectOpenHashMap<>();
        for (int i = 0; i < turnsSorted.size(); i++) {
            Long currentTurn = turnsSorted.get(i);

            Map<Integer, Map<Integer, Map<Byte, Long>>> currentData = turnData.get(currentTurn);
            if (currentData == null || currentData.isEmpty()) continue;

            for (Map.Entry<Integer, Map<Integer, Map<Byte, Long>>> entry : currentData.entrySet()) {
                Map<Integer, Map<Byte, Long>> currentByMetric = entry.getValue();
                Map<Integer, Map<Byte, Long>> previousByMetric = previous.get(entry.getKey());
                if (previousByMetric == null) {
                    Map<Integer, Map<Byte, Long>> copy = new Int2ObjectOpenHashMap<>(currentByMetric.size());
                    for (Map.Entry<Integer, Map<Byte, Long>> allianceEntry : currentByMetric.entrySet()) {
                        copy.put(allianceEntry.getKey(), new Byte2LongOpenHashMap(allianceEntry.getValue()));
                    }
                    previous.put(entry.getKey(), copy);
                    trimmed.computeIfAbsent(currentTurn, k -> new Int2ObjectOpenHashMap<>()).put(entry.getKey(), currentByMetric);
                    continue;
                }
                for (Map.Entry<Integer, Map<Byte, Long>> allianceEntry : currentByMetric.entrySet()) {
                    Map<Byte, Long> currentByAlliance = allianceEntry.getValue();
                    Map<Byte, Long> previousByAlliance = previousByMetric.get(allianceEntry.getKey());
                    if (previousByAlliance == null) {
                        previousByMetric.put(allianceEntry.getKey(), new Byte2LongOpenHashMap(currentByAlliance));
                        trimmed.computeIfAbsent(currentTurn, k -> new Int2ObjectOpenHashMap<>())
                                .computeIfAbsent(entry.getKey(), k -> new Int2ObjectOpenHashMap<>())
                                .put(allianceEntry.getKey(), currentByAlliance);
                        continue;
                    }
                    for (Map.Entry<Byte, Long> cityEntry : currentByAlliance.entrySet()) {
                        Long currentValue = cityEntry.getValue();
                        Long previousValue = previousByAlliance.get(cityEntry.getKey());
                        if (currentValue != null && !currentValue.equals(previousValue)) {
                            previousByAlliance.put(cityEntry.getKey(), currentValue);
                            trimmed.computeIfAbsent(currentTurn, k -> new Int2ObjectOpenHashMap<>())
                                    .computeIfAbsent(entry.getKey(), k -> new Int2ObjectOpenHashMap<>())
                                    .put(allianceEntry.getKey(), currentByAlliance);
                        }
                    }
                }
            }
        }

        // Replace the original map with the trimmed one
        turnData.clear();
        turnData.putAll(trimmed);
    }

    private Map<Long, Map<Integer, Map<Integer, Map<Byte, Long>>>> sortTimeMap(Map<Long, Map<Integer, Map<Integer, Map<Byte, Long>>>> data) {
        Map<Long, Map<Integer, Map<Integer, Map<Byte, Long>>>> copy = new Long2ObjectLinkedOpenHashMap<>();
        List<Long> turns = new LongArrayList(data.keySet());
        turns.sort(Long::compareTo);
        for (long turn : turns) {
            Map<Integer, Map<Integer, Map<Byte, Long>>> map = data.get(turn);
            Map<Integer, Map<Integer, Map<Byte, Long>>> sorted = new Int2ObjectLinkedOpenHashMap<>();
            IntArrayList keysSorted = new IntArrayList(map.keySet());
            keysSorted.sort(Integer::compareTo);
            for (int metricId : keysSorted) {
                Map<Integer, Map<Byte, Long>> allianceMap = map.get(metricId);
                Map<Integer, Map<Byte, Long>> allianceSorted = new Int2ObjectLinkedOpenHashMap<>();
                IntArrayList allianceKeySorted = new IntArrayList(allianceMap.keySet());
                allianceKeySorted.sort(Integer::compareTo);
                for (int allianceId : allianceKeySorted) {
                    Map<Byte, Long> cityMap = allianceMap.get(allianceId);
                    Map<Byte, Long> citySorted = new Byte2LongLinkedOpenHashMap();
                    ByteArrayList cityKeySorted = new ByteArrayList(cityMap.keySet());
                    cityKeySorted.sort(Byte::compareTo);
                    for (byte city : cityKeySorted) {
                        Long value = cityMap.get(city);
                        if (value != 0) {
                            citySorted.put(city, value);
                        }
                    }
                    allianceSorted.put(allianceId, citySorted);
                }
                sorted.put(metricId, allianceSorted);
            }
            copy.put(turn, sorted);
        }
        return copy;
    }
}
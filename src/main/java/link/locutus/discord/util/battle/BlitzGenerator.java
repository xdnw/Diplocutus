package link.locutus.discord.util.battle;

import link.locutus.discord.Locutus;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.*;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.discord.DiscordUtil;
import link.locutus.discord.util.MathMan;
import link.locutus.discord.util.math.ArrayUtil;
import link.locutus.discord.util.sheet.SheetUtil;
import link.locutus.discord.util.sheet.SpreadSheet;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BlitzGenerator {
    private static final double AIR_FACTOR = 2;

    private final boolean indefinate;
    private final double attActiveThreshold;
    private final double defActiveThreshold;
    private final Set<Guild> guilds;
    private final int turn;
    private final int maxAttacksPerNation;
    private final double sameAAPriority;
    private final double activityMatchupPriority;
    private final boolean parseExisting;

    private Function<DBNation, Activity> weeklyCache;
    Set<DBNation> colA = new HashSet<>();
    Set<DBNation> colB = new HashSet<>();

    private boolean assignEasy;

    public BlitzGenerator(int turn, int maxAttacksPerNation, double sameAAPriority, double activityMatchupPriority, double attActiveThreshold, double defActiveThreshold, Set<Long> guildsIds, boolean parseExisting) {
        this.sameAAPriority = sameAAPriority;
        this.activityMatchupPriority = activityMatchupPriority;
        this.maxAttacksPerNation = maxAttacksPerNation;
        this.weeklyCache = Activity.createCache(14 * 12);
        this.indefinate = turn == -1;
        this.turn = turn;
        this.attActiveThreshold = attActiveThreshold;
        this.defActiveThreshold = defActiveThreshold;
        this.parseExisting = parseExisting;

        this.guilds = new HashSet<>();

        for (Long discordId : guildsIds) {
            Guild guild = Locutus.imp().getDiscordApi().getGuildById(discordId);
            guilds.add(guild);
        }
    }

    private void setEasy() {
        this.assignEasy = true;
    }

    public static Map<DBNation, Set<DBNation>> reverse(Map<DBNation, Set<DBNation>> targets) {
        Map<DBNation, Set<DBNation>> reversed = new LinkedHashMap<>();
        for (Map.Entry<DBNation, Set<DBNation>> entry : targets.entrySet()) {
            DBNation defender = entry.getKey();
            for (DBNation attacker : entry.getValue()) {
                reversed.computeIfAbsent(attacker, f -> new LinkedHashSet<>()).add(defender);
            }
        }
        return reversed;
    }

    private static void process(DBNation attacker, DBNation defender, double minScoreMultiplier, double maxScoreMultiplier, boolean checkUpdeclare, boolean checkWarSlots, BiConsumer<Map.Entry<DBNation, DBNation>, String> invalidOut, Set<String> outMessages) {
        double minScore = attacker.getScore() * minScoreMultiplier;
        double maxScore = attacker.getScore() * maxScoreMultiplier;

        String response;
        if (defender.getScore() < minScore) {
            double diff = Math.round((minScore - defender.getScore()) * 100) / 100d;
            response = ("`" + defender.getNation() + "` is " + MathMan.format(diff) + "ns below " + "`" + attacker.getNation() + "`");
        } else if (defender.getScore() > maxScore) {
            double diff = Math.round((defender.getScore() - maxScore) * 100) / 100d;
            response = ("`" + defender.getNation() + "` is " + MathMan.format(diff) + "ns above " + "`" + attacker.getNation() + "`");
        } else if (checkUpdeclare && defender.getStrength() > attacker.getStrength() * 1.33) {
            double ratio = defender.getStrength() / attacker.getStrength();
            response = ("`" + defender.getNation() + "` is " + MathMan.format(ratio) + "x stronger than " + "`" + attacker.getNation() + "`");
        } else if (checkWarSlots && defender.getDef() == defender.getMaxDef()) {
            response = ("`" + defender.getNation() + "` is slotted");
        } else {
            return;
        }
        if (outMessages.add(response)) {
            invalidOut.accept(new AbstractMap.SimpleEntry<>(defender, attacker), response);
        }
    }

    public static Map<DBNation, Set<DBNation>> getTargets(SpreadSheet sheet, boolean isLeader, int headerRow) {
        return getTargets(sheet, isLeader, headerRow, f -> Integer.MAX_VALUE, 0, Integer.MAX_VALUE, false, false, f -> true, (a, b) -> {}, a -> {});
    }

    public static Map<DBNation, Set<DBNation>> getTargets(SpreadSheet sheet, boolean isLeader, int headerRow, Function<DBNation, Integer> maxWars, double minScoreMultiplier, double maxScoreMultiplier, boolean checkUpdeclare, boolean checkWarSlotted, Function<DBNation, Boolean> isValidTarget, BiConsumer<Map.Entry<DBNation, DBNation>, String> invalidOut2, Consumer<Map<String, Object>> debugInfo) {
        AtomicInteger numErrors = new AtomicInteger();
        BiConsumer<Map.Entry<DBNation, DBNation>, String> invalidOut = (entry, s) -> {
            numErrors.incrementAndGet();
            invalidOut2.accept(entry, s);
        };
        List<List<Object>> rows = sheet.fetchAll(null);
        List<Object> header = rows.get(headerRow);

        boolean useLeader = isLeader;
        Integer targetI = null;
        Integer allianceI = null;
        Integer attI = null;
        Integer att2 = null;
        Integer att3 = null;
        List<Integer> targetsIndexesRoseFormat = new ArrayList<>();

        Map<Integer, String> duplicateColumns = new LinkedHashMap<>();
        boolean isReverse = false;
        for (int i = 0; i < header.size(); i++) {
            Object obj = header.get(i);
            if (obj == null) continue;
            String title = obj.toString();
            if (title.equalsIgnoreCase("alliance")) {
                if (allianceI != null) {
                    duplicateColumns.put(i, "alliance");
                } else {
                    allianceI = i;
                }
            } else if (title.equalsIgnoreCase("nation") || title.equalsIgnoreCase("nation name") || (title.equalsIgnoreCase("link"))) {
                if (targetI == null) {
                    targetI = i;
                } else {
                    duplicateColumns.put(i, "nation");
                }
            } else if (title.equalsIgnoreCase("leader")) {
                if (targetI == null) {
                    targetI = i;
                    useLeader = true;
                } else {
                    duplicateColumns.put(i, "leader");
                }
            } else if (title.equalsIgnoreCase("att1") || title.equalsIgnoreCase("Fighter #1") || title.equalsIgnoreCase("Attacker 1")) {
                if (attI != null) {
                    duplicateColumns.put(i, "att1");
                }
                attI = i;
            } else if (title.equalsIgnoreCase("att2") || title.equalsIgnoreCase("Fighter #2") || title.equalsIgnoreCase("Attacker 2")) {
                if (att2 != null) {
                    duplicateColumns.put(i, "att2");
                }
                att2 = i;
            } else if (title.equalsIgnoreCase("att3") || title.equalsIgnoreCase("Fighter #3") || title.equalsIgnoreCase("Attacker 3")) {
                if (att3 != null) {
                    duplicateColumns.put(i, "att3");
                }
                att3 = i;
            } else if (title.equalsIgnoreCase("def1")) {
                if (attI != null) {
                    duplicateColumns.put(i, "def1");
                }
                attI = i;
                isReverse = true;
            } else if (title.toLowerCase().startsWith("spy slot ")) {
                if (attI != null) {
                    duplicateColumns.put(i, "spy slot");
                }
                targetsIndexesRoseFormat.add(i);
                targetI = 0;
            }
        }
        if (!duplicateColumns.isEmpty()) {
            Map<String, List<String>> duplicates = new LinkedHashMap<>();
            for (Map.Entry<Integer, String> entry : duplicateColumns.entrySet()) {
                int index = entry.getKey();
                String cell = SheetUtil.getLetter(index) + (headerRow + 1);
                duplicates.computeIfAbsent(entry.getValue(), f -> new ArrayList<>()).add(cell);
            }
            for (Map.Entry<String, List<String>> entry : duplicates.entrySet()) {
                String response = ("Duplicate columns found for: " + entry.getKey() + " at " + String.join(", ", entry.getValue()));
                invalidOut.accept(new AbstractMap.SimpleEntry<>(null, null), response);
            }


        }

        Set<DBNation> allAttackers = new LinkedHashSet<>();
        Set<DBNation> allDefenders = new LinkedHashSet<>();
        Map<DBNation, Set<DBNation>> targets = new LinkedHashMap<>();
        Map<DBNation, Set<DBNation>> offensiveWars = new LinkedHashMap<>();
        Set<String> outMessages = new HashSet<>();

        for (int i = headerRow + 1; i < rows.size(); i++) {
            List<Object> row = rows.get(i);
            if (row == null) continue;
            if (row.isEmpty() || row.size() <= targetI) continue;

            Object cell = row.get(targetI);
            if (cell == null || cell.toString().isEmpty()) continue;
            String nationStr = cell.toString();
            if (nationStr.contains(" / ")) nationStr = nationStr.split(" / ")[0];
            DBNation nation = DiscordUtil.parseNation(nationStr, false, useLeader);

            DBNation attacker = isReverse ? nation : null;
            DBNation defender = !isReverse ? nation : null;

            if (nation == null) {
                String response = ("`" + cell.toString() + "` is an invalid nation");
                if (outMessages.add(response)) {
                    invalidOut.accept(new AbstractMap.SimpleEntry<>(defender, attacker), response);
                }
                continue;
            }

            if (allianceI != null && rows.size() > allianceI) {
                Object aaCell = row.get(allianceI);
                if (aaCell != null) {
                    String allianceStr = aaCell.toString();
                    DBAlliance alliance = Locutus.imp().getNationDB().getAllianceByName(allianceStr);
                    if (alliance != null && nation.getAlliance_id() != alliance.getAlliance_id()) {
                        String response = ("Nation: `" + nationStr + "` is no longer in alliance: `" + allianceStr + "`");
                        if (outMessages.add(response)) {
                            invalidOut.accept(new AbstractMap.SimpleEntry<>(defender, attacker), response);
                        }
                    }
                }
            }

            if (attI != null) {
                boolean finalIsReverse = isReverse;
                DBNation finalDefender = defender;
                DBNation finalAttacker = attacker;
                boolean finalUseLeader = useLeader;
                Consumer<Integer> onRow = new Consumer<>() {
                    @Override
                    public void accept(Integer j) {
                        if (j >= row.size()) return;
                        DBNation defenderMutable = finalDefender;
                        DBNation attackerMutable = finalAttacker;
                        Object cell = row.get(j);
                        if (cell == null || cell.toString().isEmpty()) return;
                        DBNation other = DiscordUtil.parseNation(cell.toString().split("\\|")[0], false, finalUseLeader);

                        if (finalIsReverse) {
                            defenderMutable = other;
                        } else {
                            attackerMutable = other;
                        }

                        if (other == null) {
                            String response = ("`" + cell.toString() + "` is an invalid nation");
                            if (outMessages.add(response)) {
                                invalidOut.accept(new AbstractMap.SimpleEntry<>(defenderMutable, attackerMutable), response);
                            }
                            return;
                        }

                        process(attackerMutable, defenderMutable, minScoreMultiplier, maxScoreMultiplier, checkUpdeclare, checkWarSlotted, invalidOut, outMessages);

                        allAttackers.add(attackerMutable);
                        allDefenders.add(defenderMutable);

                        targets.computeIfAbsent(defenderMutable, f -> new LinkedHashSet<>()).add(attackerMutable);
                        offensiveWars.computeIfAbsent(attackerMutable, f -> new LinkedHashSet<>()).add(defenderMutable);
                    }
                };
                if (att2 == null && att3 == null) {
                    for (int j = attI; j < row.size(); j++) {
                        onRow.accept(j);
                    }
                } else {
                    onRow.accept(attI);
                    onRow.accept(att2);
                    onRow.accept(att3);
                }

            } else if (!targetsIndexesRoseFormat.isEmpty()) {
                for (Integer j : targetsIndexesRoseFormat) {
                    cell = row.get(j);
                    if (cell == null || cell.toString().isEmpty()) continue;
                    DBNation other = DiscordUtil.parseNation(cell.toString().split(" / ")[0], false, useLeader);
                    if (isReverse) {
                        defender = other;
                    } else {
                        attacker = other;
                    }
                    if (other == null) {
                        String response = ("`" + cell.toString() + "` is an invalid nation");
                        if (outMessages.add(response)) {
                            invalidOut.accept(new AbstractMap.SimpleEntry<>(defender, attacker), response);
                        }
                        continue;
                    }
                    DBNation tmp = attacker;
                    attacker = defender;
                    defender = tmp;

                    allAttackers.add(attacker);
                    allDefenders.add(defender);

                    targets.computeIfAbsent(defender, f -> new LinkedHashSet<>()).add(attacker);
                    offensiveWars.computeIfAbsent(attacker, f -> new LinkedHashSet<>()).add(defender);
                }

            } else {
                throw new IllegalArgumentException("No targets found");
            }
        }

        for (Map.Entry<DBNation, Set<DBNation>> entry : offensiveWars.entrySet()) {
            DBNation attacker = entry.getKey();
            Set<DBNation> defenders = entry.getValue();
            if (defenders.size() > maxWars.apply(attacker)) {
                String response = ("`" + attacker.getNation() + "` has " + entry.getValue().size() + " targets");
                if (outMessages.add(response)) {
                    invalidOut.accept(new AbstractMap.SimpleEntry<>(null, attacker), response);
                }
            }
        }

        for (DBNation attacker : allAttackers) {
            String response;
            if (attacker.active_m() > 4880) {
                response = ("Attacker: `" + attacker.getNation() + "` is inactive");
            } else if (attacker.isVacation()) {
                response = ("Attacker: `" + attacker.getNation() + "` is in Vacation Mode");
            } else {
                continue;
            }
            if (outMessages.add(response)) {
                invalidOut.accept(new AbstractMap.SimpleEntry<>(null, attacker), response);
            }
        }

        for (DBNation defender : allDefenders) {
            String response;
            if (!isValidTarget.apply(defender)) {
                response = ("Defender: `" + defender.getNation() + "` is not an enemy");
            } else if (defender.active_m() > TimeUnit.DAYS.toMinutes(8)) {
                response = ("Defender: `" + defender.getNation() + "` is inactive");
            } else if (defender.isVacation()) {
                response = ("Defender: `" + defender.getNation() + "` is in Vacation Mode");
            } else if (defender.hasProtection()) {
                response = ("Defender: `" + defender.getNation() + "` is beige");
            } else {
                continue;
            }
            if (outMessages.add(response)) {
                invalidOut.accept(new AbstractMap.SimpleEntry<>(defender, null), response);
            }
        }

        String attackerStr = isReverse ? "defender" : "attacker";
        String defenderStr = !isReverse ? "defender" : "attacker";
        Map<String, Object> debugInfoMap = new LinkedHashMap<>();
        debugInfoMap.put("sheet_id", sheet.getSpreadsheetId());
        debugInfoMap.put("errors", numErrors.get());
        debugInfoMap.put("header_row", (headerRow + 1));
        debugInfoMap.put("inverse", isReverse ? "true (found `def` column)" : "false");
        debugInfoMap.put("use_leader", useLeader);
        debugInfoMap.put(defenderStr, targetI == null ? "Not Found" : SheetUtil.getLetter(targetI) + (headerRow + 1));
        debugInfoMap.put("alliance", allianceI == null ? "Not Found" : SheetUtil.getLetter(allianceI) + (headerRow + 1));
        debugInfoMap.put(attackerStr + "_1", attI == null ? "Not Found" : SheetUtil.getLetter(attI) + (headerRow + 1));
        debugInfoMap.put(attackerStr + "_2", att2 == null ? "Not Found" : SheetUtil.getLetter(att2) + (headerRow + 1));
        debugInfoMap.put(attackerStr + "_3", att3 == null ? "Not Found" : SheetUtil.getLetter(att3) + (headerRow + 1));

        Map<String, Integer> attackersByAA = ArrayUtil.sortMap(allAttackers.stream().collect(Collectors.groupingBy(DBNation::getAllianceName, Collectors.summingInt(f -> 1))), false);
        Map<String, Integer> defendersByAA = ArrayUtil.sortMap(allDefenders.stream().collect(Collectors.groupingBy(DBNation::getAllianceName, Collectors.summingInt(f -> 1))), false);

        debugInfoMap.put("attacker_alliances", attackersByAA);
        debugInfoMap.put("defender_alliances", defendersByAA);
        debugInfo.accept(debugInfoMap);

        return targets;
    }

    public void addAlliances(Set<Integer> allianceIds, boolean attacker) {
        Set<DBNation> nations = Locutus.imp().getNationDB().getNations(allianceIds);
        addNations(nations, attacker);
    }

    public Set<DBNation> getNations(boolean attacker) {
        return attacker ? colA : colB;
    }

    public void addNations(Collection<DBNation> nations, boolean attacker) {
        getNations(attacker).addAll(nations);
    }

    public void removeInactive(int minutes) {
        colA.removeIf(n -> n.active_m() > minutes);
        colB.removeIf(n -> n.active_m() > minutes);
    }

    public void removeSlotted() {
        colB.removeIf(n -> n.getDef() >= n.getMaxDef() || n.hasProtection() || n.isVacation());
    }

    BiFunction<Double, Double, Integer> attScores;
    BiFunction<Double, Double, Integer> defScores;

    BiFunction<Double, Double, Double> attPlanes;
    BiFunction<Double, Double, Double> defPlanes;

    Function<Double, Double> enemyPlaneRatio;

    private void init() {
        attScores = DNS.getIsNationsInScoreRange(colA);
        defScores = DNS.getIsNationsInScoreRange(colB);

        attPlanes = DNS.getXInRange(colA, n -> Math.pow(n.getStrength(), AIR_FACTOR));
        defPlanes = DNS.getXInRange(colB, n -> Math.pow(n.getStrength(), AIR_FACTOR));

        enemyPlaneRatio = new Function<Double, Double>() {
            @Override
            public Double apply(Double scoreAttacker) {
                double attAmt = attPlanes.apply(scoreAttacker * 0.75, scoreAttacker * 1.25);
                double defAmt = defPlanes.apply(scoreAttacker * 0.75, scoreAttacker * 1.25);
                return defAmt / attAmt;
            }
        };
        // remove defenders that can't counter

        Iterator<DBNation> defIter = colB.iterator();
        while (defIter.hasNext()) {
            DBNation defender = defIter.next();
            double minScore = defender.getScore() * 0.75;
            double maxScore = defender.getScore() / 0.75;
            if (attScores.apply(minScore, maxScore) <= 0) {
                defIter.remove();
            }
        }
    }

    public Map<DBNation, List<DBNation>> assignEasyTargets() {
        return assignEasyTargets(1, 1.8);
    }

    public Map<DBNation, List<DBNation>> assignEasyTargets(double maxStrengthRatio, double maxInfraRatio) {
        init();
//        int airCap = Buildings.HANGAR.perDay() * Buildings.HANGAR.cap(f -> false);
//        colA.removeIf(n -> n.getAircraft() < airCap * n.getCities() * 0.8);

        Map<DBNation, List<DBNation>> attPool = new HashMap<>(); // Pool of nations that could be used as targets
        Map<DBNation, List<DBNation>> defPool = new HashMap<>(); // Pool of nations that could be used as targets

        for (DBNation attacker : colA) {
            double attActive = activityFactor(attacker, true);
            if (attActive < attActiveThreshold) {
                continue;
            }

            for (DBNation defender : colB) {
                if (defender.getStrength() > attacker.getStrength() * maxStrengthRatio) {
                    continue;
                }
                if (defender.getInfra() > attacker.getInfra() * maxInfraRatio) {
                    continue;
                }

                double defActive = activityFactor(defender, false);

                if (defActive < defActiveThreshold) {
                    continue;
                }

                double minScore = attacker.getScore() * DNS.WAR_RANGE_MIN_MODIFIER_ACTIVE;
                double maxScore = attacker.getScore() * DNS.WAR_RANGE_MAX_MODIFIER_ACTIVE;

//                if (enemyPlaneRatio.apply(defender.getScore()) > 1) {
//                    maxScore /= 0.75;
//                }

                if (defender.getScore() >= minScore && defender.getScore() <= maxScore) {
                    attPool.computeIfAbsent(attacker, f -> new ArrayList<>()).add(defender);
                    defPool.computeIfAbsent(defender, f -> new ArrayList<>()).add(attacker);
                }
            }
        }

        ArrayList<DBNation> colAList = new ArrayList<>(colA);
        Collections.sort(colAList, new Comparator<DBNation>() {
            @Override
            public int compare(DBNation o1, DBNation o2) {
                return Double.compare(o2.getStrength(), o1.getStrength());
            }
        });

        // The actual targets
        Map<DBNation, List<DBNation>> attTargets = new LinkedHashMap<>(); // Final assigned targets
        Map<DBNation, List<DBNation>> defTargets = new LinkedHashMap<>(); // Final assigned targets

        Set<DBWar> warsToRemove = new HashSet<>();

        if (parseExisting) {
            Set<Integer> natIds = new HashSet<>();
            for (DBNation nation : attPool.keySet()) natIds.add(nation.getNation_id());
            for (DBNation nation : defPool.keySet()) natIds.add(nation.getNation_id());
            Map<Integer, List<DBWar>> wars = Locutus.imp().getWarDb().getActiveWarsByAttacker(natIds, natIds, WarStatus.ACTIVE, WarStatus.DEFENDER_OFFERED_PEACE, WarStatus.ATTACKER_OFFERED_PEACE);
            for (Map.Entry<Integer, List<DBWar>> entry : wars.entrySet()) {
                for (DBWar war : entry.getValue()) {
                    DBNation attacker = Locutus.imp().getNationDB().getNation(war.getAttacker_id());
                    DBNation defender = Locutus.imp().getNationDB().getNation(war.getDefender_id());
                    if (attacker == null || defender == null) continue;
                    if (!war.isActive()) continue;

                    if (colA.contains(defender) || colB.contains(attacker)) {
                        DBNation tmp = defender;
                        defender = attacker;
                        attacker = tmp;
                    }
                    attTargets.computeIfAbsent(attacker, f -> new ArrayList<>()).add(defender);
                    defTargets.computeIfAbsent(defender, f -> new ArrayList<>()).add(attacker);

                    warsToRemove.add(war);
                }
                removeSlotted();
            }

            removeSlotted();
        }

        for (DBNation attacker : colAList) {
            List<DBNation> defenders = attPool.getOrDefault(attacker, Collections.emptyList());
            Collections.sort(defenders, new Comparator<DBNation>() {
                @Override
                public int compare(DBNation o1, DBNation o2) {
                    return Double.compare(o1.getStrength(), o2.getStrength());
                }
            });

            int maxAttacks = attacker.getNumWars();
            for (DBNation defender : defenders) {
                List<DBNation> existing = defTargets.getOrDefault(defender, Collections.emptyList());
                if (existing.size() >= defender.getMaxDef()) {
                    continue;
                }
                if (maxAttacks++ >= maxAttacksPerNation) break;
//                if (defender.getStrength() >= attacker.getStrength() * (maxGroundRatio + maxAirRatio)) continue;

                attTargets.computeIfAbsent(attacker, f -> new ArrayList<>()).add(defender);
                defTargets.computeIfAbsent(defender, f -> new ArrayList<>()).add(attacker);
            }
        }

        if (!warsToRemove.isEmpty()) {
            for (DBWar war : warsToRemove) {
                DBNation attacker = Locutus.imp().getNationDB().getNation(war.getAttacker_id());
                DBNation defender = Locutus.imp().getNationDB().getNation(war.getDefender_id());
                defTargets.getOrDefault(defender, Collections.emptyList()).remove(attacker);
                defTargets.getOrDefault(attacker, Collections.emptyList()).remove(defender);
            }
            defTargets.entrySet().removeIf(e -> e.getValue().isEmpty());
        }

        return defTargets;

    }

    public Map<DBNation, List<DBNation>> assignTargets(Double maxStrengthRatio) {
        init();

        Map<DBNation, List<DBNation>> attPool = new HashMap<>(); // Pool of nations that could be used as targets
        Map<DBNation, List<DBNation>> defPool = new HashMap<>(); // Pool of nations that could be used as targets

        for (DBNation attacker : colA) {
            double attActive = activityFactor(attacker, true);
            if (attActive < attActiveThreshold) continue;

            int num = 0;
            for (DBNation defender : colB) {
                double defActive = activityFactor(defender, false);

                if (defActive < defActiveThreshold) continue;

                double minScore = attacker.getScore() * DNS.WAR_RANGE_MIN_MODIFIER_ACTIVE;
                double maxScore = attacker.getScore() * DNS.WAR_RANGE_MAX_MODIFIER_ACTIVE;
                if (defender.getScore() >= minScore && defender.getScore() <= maxScore) {
                    attPool.computeIfAbsent(attacker, f -> new ArrayList<>()).add(defender);
                    defPool.computeIfAbsent(defender, f -> new ArrayList<>()).add(attacker);
                }
            }
        }

        List<Map.Entry<DBNation, Double>> targetPriority = new ArrayList<>();

        for (Map.Entry<DBNation, List<DBNation>> entry : defPool.entrySet()) {
            DBNation defender = entry.getKey();
            List<DBNation> attackers = entry.getValue();
            if (attackers.size() == 0) {
                continue;
            }

            double magicStrength = getValue(defender, false, attackers);

            targetPriority.add(new AbstractMap.SimpleEntry<>(defender, magicStrength));
        }

        targetPriority.sort((o1, o2) -> Double.compare(o2.getValue(), o1.getValue()));


        // The actual targets
        Map<DBNation, List<DBNation>> attTargets = new LinkedHashMap<>(); // Final assigned targets
        Map<DBNation, List<DBNation>> defTargets = new LinkedHashMap<>(); // Final assigned targets

        Set<DBWar> warsToRemove = new HashSet<>();

        if (parseExisting) {
            Set<Integer> natIds = new HashSet<>();
            for (DBNation nation : attPool.keySet()) natIds.add(nation.getNation_id());
            for (DBNation nation : defPool.keySet()) natIds.add(nation.getNation_id());
            Map<Integer, List<DBWar>> wars = Locutus.imp().getWarDb().getActiveWarsByAttacker(natIds, natIds, WarStatus.ACTIVE, WarStatus.DEFENDER_OFFERED_PEACE, WarStatus.ATTACKER_OFFERED_PEACE);
            for (Map.Entry<Integer, List<DBWar>> entry : wars.entrySet()) {
                for (DBWar war : entry.getValue()) {
                    DBNation attacker = Locutus.imp().getNationDB().getNation(war.getAttacker_id());
                    DBNation defender = Locutus.imp().getNationDB().getNation(war.getDefender_id());
                    if (attacker == null || defender == null) continue;
                    if (!war.isActive()) continue;

                    if (colA.contains(defender) || colB.contains(attacker)) {
                        DBNation tmp = defender;
                        defender = attacker;
                        attacker = tmp;
                    }
                    attTargets.computeIfAbsent(attacker, f -> new ArrayList<>()).add(defender);
                    defTargets.computeIfAbsent(defender, f -> new ArrayList<>()).add(attacker);

                    warsToRemove.add(war);
                }
                removeSlotted();
            }

            removeSlotted();
        }

        outer:
        while (true) {
            targetPriority.clear();
            HashMap<DBNation, Double> attackerPriorityCache = new HashMap<>();

            // TODO move this to a function to clean up the code
            // get the priority
            Function<DBNation, Double> attackerPriority = new Function<DBNation, Double>() {
                @Override
                public Double apply(DBNation attacker) {
                    if (attackerPriorityCache.containsKey(attacker)) {
                        return attackerPriorityCache.get(attacker);
                    }
                    if (attTargets.getOrDefault(attacker, Collections.emptyList()).size() >= maxAttacksPerNation) {
                        attackerPriorityCache.put(attacker, Double.NEGATIVE_INFINITY);
                    };
                    double attStr = attacker.getStrength();

                    List<DBNation> defenders = attPool.getOrDefault(attacker, Collections.emptyList());
                    // subtract 1/3 strength of defender
                    for (DBNation defender : defenders) {
                        // TODO calculate strength based on current military if not indefinite
                        // TODO use simulator to calculate strength
                        // AND OTHER PLACE WITH SAME FORMULA
                        double defStr = defender.getStrength();
                        defStr -= defStr * 0.25 * defTargets.getOrDefault(defender, Collections.emptyList()).size();
                        attStr -= defStr * 0.15;
                    }

                    attackerPriorityCache.put(attacker, attStr);
                    return attStr;
                }
            };

            for (Map.Entry<DBNation, List<DBNation>> entry : defPool.entrySet()) {
                DBNation defender = entry.getKey();
                List<DBNation> existing = defTargets.getOrDefault(defender, Collections.emptyList());

                if (existing.size() >= defender.getMaxDef()) {
                    continue;
                }

                List<DBNation> attackers = defPool.getOrDefault(defender, Collections.emptyList());

                if (attackers.size() == 0) continue;
                attackers.removeIf(n -> attTargets.getOrDefault(n, Collections.emptyList()).size() >= maxAttacksPerNation);
                if (attackers.size() == 0) continue;
                if (maxStrengthRatio != null) {
                    attackers.removeIf(n -> n.getStrength() * maxStrengthRatio < defender.getStrength());
                }

                // TODO calculate strength based on current military if not indefinite
                // TODO use simulator to calculate strength
                // AND OTHER PLACE WITH SAME FORMULA
                double defStr = getValue(defender, false, attackers);
                defStr -= defStr * 0.25 * defTargets.getOrDefault(defender, Collections.emptyList()).size();

                int freeSlots = defender.getMaxDef() - attackers.size();

                if (freeSlots >= attackers.size()) {
                    defStr *= 20 * (1 + freeSlots - attackers.size());
                }

                targetPriority.add(new AbstractMap.SimpleEntry<>(defender, defStr));
            }

            targetPriority.sort((o1, o2) -> Double.compare(o2.getValue(), o1.getValue()));

            for (Map.Entry<DBNation, Double> entry : targetPriority) {
                DBNation defender = entry.getKey();

                List<DBNation> attackers = defPool.get(defender);

                if (attackers.isEmpty()) continue;

                List<DBNation> existing = defTargets.getOrDefault(defender, Collections.emptyList());

                double totalPlanes = -1;
                Map<Integer, Double> planesByAA = null;

                // TODO limit to same alliance
                double best = Double.NEGATIVE_INFINITY;
                DBNation bestAttacker = null;
                for (DBNation attacker : attackers) {
                    Double attPriority = attackerPriority.apply(attacker);
                    if (attPriority == null) continue;

                    if (sameAAPriority != 0) {
                        boolean sameAA = false;
                        if (!existing.isEmpty()) {
                            for (DBNation nation : existing) {
                                if (nation.getAlliance_id() == attacker.getAlliance_id()) {
                                    sameAA = true;
                                    break;
                                }
                            }
                            if (!sameAA) {
                                attPriority = attPriority * (1 - sameAAPriority);
                            }
                        } else {
                            if (totalPlanes == -1) {
                                planesByAA = new HashMap<>();
                                totalPlanes = 0;
                                for (DBNation other : attackers) {
                                    int id = other.getAlliance_id();
                                    double str = other.getStrength();
                                    totalPlanes += str;
                                    planesByAA.put(id, planesByAA.getOrDefault(id, 0d) + str);
                                }
                            }
                            Double aaPlanes = planesByAA.get(attacker.getAlliance_id());
                            attPriority = attPriority * (1 - sameAAPriority) + attPriority * (aaPlanes / totalPlanes) * (sameAAPriority);
                        }
                    }

                    if (activityMatchupPriority != 0) {
                        if (!existing.isEmpty()) {
                            double[] attActive = weeklyCache.apply(attacker).getByDayTurn().clone();
                            for (DBNation other : existing) {
                                double[] otherActive = weeklyCache.apply(other).getByDayTurn();
                                for (int i = 0; i < attActive.length; i++) {
                                    attActive[i] *= (otherActive[i] + 0.1);
                                }
                            }
                            double activityMatch = 0;
                            for (double v : attActive) activityMatch += v;

                            attPriority = attPriority * (1 - activityMatchupPriority) + attPriority * activityMatch * activityMatchupPriority;
                        }
                    }

                    if (attPriority > best) {
                        best = attPriority;
                        bestAttacker = attacker;
                    }
                }

                if (bestAttacker != null) {
                    attackers.remove(bestAttacker);
                    attTargets.computeIfAbsent(bestAttacker, f -> new ArrayList<>()).add(defender);
                    defTargets.computeIfAbsent(defender, f -> new ArrayList<>()).add(bestAttacker);
                    continue outer;
                }

            }

            break;
        }

        ArrayList<Map.Entry<DBNation, List<DBNation>>> attPoolSorted = new ArrayList<>(attPool.entrySet());
        Collections.sort(attPoolSorted, (o1, o2) -> Double.compare(o2.getKey().getStrength(), o1.getKey().getStrength()));

        outer:
        for (Map.Entry<DBNation, List<DBNation>> entry : attPoolSorted) {
            DBNation attacker = entry.getKey();
            List<DBNation> targets = attTargets.getOrDefault(attacker, Collections.emptyList());

            if (!targets.isEmpty()) continue;

            List<DBNation> options = new ArrayList<>(entry.getValue());
            Collections.sort(options, (o1, o2) -> Double.compare(o1.getStrength(), o2.getStrength()));

            double minStr = Double.MAX_VALUE;
            DBNation minStrNation = null;
            for (DBNation defender : options) {
                double defStr = defender.getStrength();

                int numAttackers = Math.max(0, defTargets.getOrDefault(defender, Collections.emptyList()).size() - defender.getMaxDef());

                if (numAttackers > 6) continue;

                if (defStr < minStr) {
                    minStr = defStr;
                    minStrNation = defender;
                }
            }

            if (minStrNation != null && attacker.getStrength() > minStr) {
                attTargets.computeIfAbsent(attacker, f -> new ArrayList<>()).add(minStrNation);
                defTargets.computeIfAbsent(minStrNation, f -> new ArrayList<>()).add(attacker);
            }
        }


        if (!warsToRemove.isEmpty()) {
            for (DBWar war : warsToRemove) {
                DBNation attacker = Locutus.imp().getNationDB().getNation(war.getAttacker_id());
                DBNation defender = Locutus.imp().getNationDB().getNation(war.getDefender_id());
                defTargets.getOrDefault(defender, Collections.emptyList()).remove(attacker);
                defTargets.getOrDefault(attacker, Collections.emptyList()).remove(defender);
            }
            defTargets.entrySet().removeIf(e -> e.getValue().isEmpty());
        }
        return defTargets;
    }

    public double getValue(DBNation defender, boolean isAttacker, List<DBNation> attackers) {
        return defender.getStrength();
    }

    private Map<DBNation, Double> activityFactorAtt = new HashMap<>();
    private Map<DBNation, Double> activityFactorDef = new HashMap<>();

    public double activityFactor(DBNation nation, boolean isAttacker) {
        if ((!isAttacker && nation.getPosition() > 2) || nation.getOff() > 0) return 1;

        Map<DBNation, Double> cache = isAttacker ? activityFactorAtt : activityFactorDef;
        Double cacheValue = cache.get(nation);
        if (cacheValue != null) return cacheValue;

        GuildDB db = Locutus.imp().getGuildDBByAA(nation.getAlliance_id());
        if (db != null) {
            guilds.add(db.getGuild());
        }

        User user = nation.getUser();
        if (user != null) {
            for (Guild guild : guilds) {
                if (Roles.BLITZ_PARTICIPANT.has(user, guild)) {
                    return 1;
                }

                if (Roles.BLITZ_PARTICIPANT_OPT_OUT.has(user, guild)) {
                    return 0;
                }
            }
        }

        Activity activity = weeklyCache.apply(nation);

        double min = 0;
        if (indefinate) {
            if (!isAttacker) {
                // Use average for defender
                for (double v : activity.getByDay()) {
                    min += v;
                }
                min /= 7;
            } else {
                // Use min for attacker
                min = 1;
                for (double v : activity.getByDay()) {
                    min = Math.min(v, min);
                }
            }
        } else {
            if (isAttacker) {
                min = activity.loginChance(turn, 1, true);
            } else {
                min = activity.loginChance(24, true);
                if (min < 0.8 && nation.getOff() > 0) {
                    min = 0.2 * min + 0.8;
                }
            }
        }
        cache.put(nation, min);
        return min;
    }


}

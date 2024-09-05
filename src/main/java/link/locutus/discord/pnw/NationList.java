package link.locutus.discord.pnw;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import link.locutus.discord.Locutus;
import link.locutus.discord.api.types.Building;
import link.locutus.discord.api.types.Rank;
import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.commands.manager.v2.impl.pw.NationFilter;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.db.entities.DBWar;
import link.locutus.discord.util.MathMan;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.StringMan;
import link.locutus.discord.util.math.ArrayUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface NationList extends NationFilter {
    default AllianceList toAllianceList() {
        return new AllianceList(getAllianceIds());
    }
    @Override
    default String getFilter() {
        return null;
    }

    @Override
    default boolean test(DBNation dbNation){
        return getNations().contains(dbNation);
    }

    Set<DBNation> getNations();

    default Set<Integer> getAllianceIds() {
        return getNations().stream().map(DBNation::getAlliance_id).collect(Collectors.toSet());
    }

    default Set<Integer> getNationIds() {
        return getNations().stream().map(DBNation::getNation_id).collect(Collectors.toSet());
    }

//    default double[] getAverageMMR(boolean update) {
//        double[] total = getTotalMMR(update);
//        double num = total[4];
//        return new double[] {total[0] / num, total[1] / num, total[2] / num, total[3] / num};
//    }

    default  <T> Map<T, NationList> groupBy(Function<DBNation, T> groupBy) {
        Map<T, List<DBNation>> mapList = new HashMap<>();
        for (DBNation nation : getNations()) {
            T group = groupBy.apply(nation);
            mapList.computeIfAbsent(group, f -> new ArrayList<>()).add(nation);
        }
        Map<T, NationList> result = new HashMap<>();
        for (Map.Entry<T, List<DBNation>> entry : mapList.entrySet()) {
            result.put(entry.getKey(), new SimpleNationList(entry.getValue()));
        }
        return result;
    }

    default <T> Stream<T> stream(Function<? super DBNation, ? extends T> map) {
        return getNations().stream().map(map);
    }

    default Stream<DBNation> stream() {
        return getNations().stream();
    }


    default String toMarkdown() {
        return toMarkdown(true, true, true, true, true, true);
    }

    // arguments for controlling which information to include
    default String toMarkdown(boolean includeAlliance, boolean includeRevenue, boolean includeWars, boolean includePositions, boolean includeData, boolean includeMMR) {
        StringBuilder body = new StringBuilder();
        Set<DBNation> nations = getNations();
        body.append("> " + nations.size() + " nations ");
        int countNone = nations.stream().filter(f -> f.getAlliance_id() == 0).collect(Collectors.toSet()).size();
        if (includeAlliance) {
            Set<DBAlliance> alliances = nations.stream().map(DBNation::getAlliance).filter(f -> f != null).collect(Collectors.toSet());
            if (alliances.isEmpty()) {
                body.append("(No alliance)\n");
            } else if (alliances.size() == 1) {
                DBAlliance aa = alliances.iterator().next();
                body.append(" in " + aa.getMarkdownUrl() + "\n");
            } else if (alliances.size() <= 5) {
                body.append(" in\n");
                for (DBAlliance aa : alliances) {
                    int count = (int) nations.stream().filter(f -> f.getAlliance_id() == aa.getId()).count();
                    body.append("> - ").append(aa.getMarkdownUrl()).append("x").append(count).append("\n");
                }
            } else {
                body.append(" in " + alliances.size() + " alliances\n");
            }
            if (countNone > 0 && !alliances.isEmpty()) {
                body.append("(" + countNone + " in None)\n");
            }
        }
        if (includePositions || includeData) {
            body.append("```\n");
            Set<DBNation> members = nations.stream().filter(n -> n.getPosition() > Rank.APPLICANT.id && n.isVacation() == false).collect(Collectors.toSet());
            if (includePositions) {
                // Number of members / applicants (active past day)
                Set<DBNation> activeMembers = members.stream().filter(n -> n.active_m() < 7200).collect(Collectors.toSet());
                Set<DBNation> applicants = nations.stream().filter(n -> n.getPosition() == Rank.APPLICANT.id && n.isVacation() == false).collect(Collectors.toSet());
                Set<DBNation> activeApplicants = applicants.stream().filter(n -> n.active_m() < 7200).collect(Collectors.toSet());
                // 5 members (3 active/2 taxable) | 2 applicants (1 active)
                body.append(members.size()).append(" members (").append(activeMembers.size()).append(" active/");
                if (!applicants.isEmpty()) {
                    body.append(" | ").append(applicants.size()).append(" applicants (").append(activeApplicants.size()).append(" active)");
                }
                body.append("\n");
            }
            if (includeData) {
                // Off, Def, Cities (total/average), Score, Color
                int off = nations.stream().mapToInt(DBNation::getOff).sum();
                int def = nations.stream().mapToInt(DBNation::getDef).sum();
                double score = members.stream().mapToDouble(DBNation::getScore).sum();
                body.append(off).append("\uD83D\uDDE1 | ")
                        .append(def).append("\uD83D\uDEE1 | ")
                        .append(MathMan.format(score)).append("ns");
            }
            body.append("\n```\n");
        }

//        if (includeMMR) {
//            double[] mmrBuild = this.getAverageMMR(false);
//            // Convert to e.g. MMR[Build]=1.5/2.5/1.1/3.0 | MMR[Unit]=1.5/2.5/1.1/3.0
//            // append with each number on newline
//            body.append("\n**MMR[Build]**: `")
//                    .append(MathMan.format(mmrBuild[0])).append("/")
//                    .append(MathMan.format(mmrBuild[1])).append("/")
//                    .append(MathMan.format(mmrBuild[2])).append("/")
//                    .append(MathMan.format(mmrBuild[3])).append("`")
//                    .append("\n**MMR[Unit]**: `")
//                    .append(MathMan.format(mmrUnit[0])).append("/")
//                    .append(MathMan.format(mmrUnit[1])).append("/")
//                    .append(MathMan.format(mmrUnit[2])).append("/")
//                    .append(MathMan.format(mmrUnit[3])).append("`\n");
//        }

        if (includeWars) {
            Map<DBAlliance, Integer> warsByAlliance = new HashMap<>();
            for (DBWar war : getActiveWars()) {
                DBNation attacker = war.getNation(true);
                DBNation defender = war.getNation(false);
                if (attacker == null || attacker.active_m() > 7200) continue;
                if (defender == null || defender.active_m() > 7200) continue;
                int otherAAId = nations.contains(attacker) ? defender.getAlliance_id() : attacker.getAlliance_id();
                if (otherAAId > 0) {
                    DBAlliance otherAA = DBAlliance.getOrCreate(otherAAId);
                    warsByAlliance.put(otherAA, warsByAlliance.getOrDefault(otherAA, 0) + 1);
                }
            }
            if (!warsByAlliance.isEmpty()) {
                List<Map.Entry<DBAlliance, Integer>> sorted = warsByAlliance.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .toList();
                body.append("\n**Alliance Wars:**\n");
                String cappedMsg = null;
                if (sorted.size() > 20) {
                    cappedMsg = "- +" + (sorted.size() - 20) + " more";
                    sorted = sorted.stream().limit(20).collect(Collectors.toList());
                }
                for (Map.Entry<DBAlliance, Integer> entry : sorted) {
                    body.append("- ").append(DNS.getMarkdownUrl(entry.getKey().getId(), true))
                            .append(": ").append(entry.getValue()).append(" wars\n");
                }
                if (cappedMsg != null) {
                    body.append(cappedMsg).append("\n");
                }
            }
        }
        // TODO FIXME :||remove
//        if (includeRevenue) {
//            // Revenue
//            Map<ResourceType, Double> revenue = getRevenue();
//            if (revenue.isEmpty()) {
//                body.append("`No taxable revenue`\n");
//            } else {
//                body.append("\n**Taxable Nation Revenue:**");
//                body.append("`").append(ResourceType.resourcesToString(revenue)).append("`\n");
//                body.append("- worth: `$" + MathMan.format(ResourceType.convertedTotal(revenue)) + "`\n");
//            }
//        }
        return body.toString();
    }

    default Set<DBWar> getActiveWars() {
        Set<DBWar> wars = new ObjectOpenHashSet<>();
        for (DBNation nation : getNations()) {
            wars.addAll(nation.getActiveWars());
        }
        return wars;
    }

    default Set<DBNation> getNationsMatching(Predicate<DBNation> nations) {
        return getNations().stream().filter(nations).collect(Collectors.toSet());
    }

    default double getScore() {
        Collection<DBNation> nations = new ArrayList<>(getNations());
        nations.removeIf(f -> f.getPosition() <= 0 || f.isVacation());
        double total = 0;
        for (DBNation nation : nations) {
            total += nation.getScore();
        }
        return total;
    }

    default boolean contains(DBNation f) {
        return getNations().contains(f);
    }
}

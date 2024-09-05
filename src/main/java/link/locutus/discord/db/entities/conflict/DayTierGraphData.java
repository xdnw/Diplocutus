package link.locutus.discord.db.entities.conflict;

import it.unimi.dsi.fastutil.bytes.Byte2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import link.locutus.discord.db.entities.DBNation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DayTierGraphData extends TierGraphData {
    public void update(Set<DBNation> nations) {
        metricByTier.clear();

        Map<Integer, Map<Byte, Integer>> nationsByAllianceByTier = metricByTier.computeIfAbsent(ConflictMetric.NATION, k -> new Int2ObjectOpenHashMap<>());
        Map<Integer, Map<Byte, Integer>> infraByAllianceByTier = metricByTier.computeIfAbsent(ConflictMetric.INFRA, k -> new Int2ObjectOpenHashMap<>());
        Map<Integer, Map<Byte, Integer>> beigeByAllianceByTier = metricByTier.computeIfAbsent(ConflictMetric.PROTECTION, k -> new Int2ObjectOpenHashMap<>());

        for (DBNation nation : nations) {
            int aaId = nation.getAlliance_id();
            byte cities = (byte) nation.getTier();
            int infra = (int) Math.round(nation.getInfra());
            boolean isBeige = nation.hasProtection();
            nationsByAllianceByTier.computeIfAbsent(aaId, k -> new Byte2IntOpenHashMap()).merge(cities, 1, Integer::sum);
            infraByAllianceByTier.computeIfAbsent(aaId, k -> new Byte2IntOpenHashMap()).merge(cities, infra, Integer::sum);
            if (isBeige) beigeByAllianceByTier.computeIfAbsent(aaId, k -> new Byte2IntOpenHashMap()).merge(cities, 1, Integer::sum);
        }

        List<ConflictMetric> metrics = Arrays.stream(ConflictMetric.values).filter(ConflictMetric::isDay)
                .filter(f -> f != ConflictMetric.NATION && f != ConflictMetric.INFRA && f != ConflictMetric.PROTECTION).toList();

        for (DBNation nation : nations) {
            int aaId = nation.getAlliance_id();
            byte cities = (byte) nation.getTier();
            for (ConflictMetric metric : metrics) {
                int value = metric.get(nation);
                if (value == 0) continue;
                metricByTier.computeIfAbsent(metric, k -> new Int2ObjectOpenHashMap<>())
                        .computeIfAbsent(aaId, k -> new Byte2IntOpenHashMap()).merge(cities, value, Integer::sum);
            }
        }
    }
}

package link.locutus.discord.db.entities.metric;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import link.locutus.discord.Locutus;
import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.api.endpoints.DnsApi;
import link.locutus.discord.db.NationDB;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.util.TimeUtil;
import link.locutus.discord.util.scheduler.ThrowingBiConsumer;

import java.sql.PreparedStatement;
import java.util.List;

public enum DnsMetric {

    TOTAL_NATIONS {
        @Override
        public double update() {
            return Locutus.imp().getNationDB().getNations().size();
        }
    },
    NATIONS_CREATED {
        @Override
        public double update() {
            int created = 0;
            long cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000;
            for (DBNation nation : Locutus.imp().getNationDB().getNations().values()) {
                if (nation.getDate() > cutoff) {
                    created++;
                }
            }
            return created;
        }
    },
    ACTIVE_1_DAY {
        @Override
        public double update() {
            long cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000;
            return Locutus.imp().getNationDB().getNations().values().stream().filter(n -> n.lastActiveMs() > cutoff).count();
        }
    },
    ACTIVE_2_DAYS {
        @Override
        public double update() {
            long cutoff = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000;
            return Locutus.imp().getNationDB().getNations().values().stream().filter(n -> n.lastActiveMs() > cutoff).count();
        }
    },
    ACTIVE_3_DAYS {
        @Override
        public double update() {
            long cutoff = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000;
            return Locutus.imp().getNationDB().getNations().values().stream().filter(n -> n.lastActiveMs() > cutoff).count();
        }
    },
    ACTIVE_1_WEEK {
        @Override
        public double update() {
            long cutoff = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000;
            return Locutus.imp().getNationDB().getNations().values().stream().filter(n -> n.lastActiveMs() > cutoff).count();
        }
    },
    ACTIVE_1_MONTH {
        @Override
        public double update() {
            long cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000;
            return Locutus.imp().getNationDB().getNations().values().stream().filter(n -> n.lastActiveMs() > cutoff).count();
        }
    },

    ;

    public static final DnsMetric[] values = values();

    DnsMetric() {
    }

    public boolean isTurn() {
        return false;
    }

    public abstract double update();

    public static void update(NationDB db) {
        long day = db.getLatestMetricTime(false);
        if (day >= TimeUtil.getDay() - 1) return;

        List<DnsMetricValue> metricValues = new ObjectArrayList<>();
        loadAll(metricValues, day);

        saveAll(metricValues, false, false);
    }

    public static void loadAll(List<DnsMetricValue> values, long day) {
        for (DnsMetric metric : values()) {
            double value = metric.update();
            values.add(new DnsMetricValue(metric, day, value));
        }
    }

    public record DnsMetricValue(DnsMetric metric, long turnOrDay, double value) {}

    private static void saveAll(List<DnsMetricValue> values, boolean isTurn, boolean replace) {
        String table = isTurn ? "GAME_METRICS_TURN" : "GAME_METRICS_DAY";
        String dayOrTurnCol = isTurn ? "turn" : "day";
        int chunkSize = 10000;
        for (int i = 0; i < values.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, values.size());
            List<DnsMetricValue> subList = values.subList(i, end);
            String keyWord = replace ? "REPLACE" : "IGNORE";
            Locutus.imp().getNationDB().executeBatch(subList, "INSERT OR " + keyWord + " INTO `" + table + "`(`metric`, `" + dayOrTurnCol + "`, `value`) VALUES(?, ?, ?)", (ThrowingBiConsumer<DnsMetricValue, PreparedStatement>) (value, stmt) -> {
                stmt.setInt(1, value.metric.ordinal());
                stmt.setLong(2, value.turnOrDay);
                stmt.setDouble(3, value.value);
            });
        }
    }
}

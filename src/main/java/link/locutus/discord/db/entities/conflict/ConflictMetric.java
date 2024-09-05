package link.locutus.discord.db.entities.conflict;

import link.locutus.discord.db.entities.DBNation;

import java.util.Map;

public enum ConflictMetric {
    NATION(false) {
        @Override
        public int get(DBNation nation) {
            return 1;
        }
    },
    PROTECTION(false) {
        @Override
        public int get(DBNation nation) {
            return nation.hasProtection() ? 1 : 0;
        }
    },
    INFRA(false) {
        @Override
        public int get(DBNation nation) {
            return (int) nation.getInfra();
        }
    },
    LAND(false) {
        @Override
        public int get(DBNation nation) {
            return (int) nation.getLand();
        }
    },
    NON_CORE_LAND(false) {
        @Override
        public int get(DBNation nation) {
            return (int) nation.getNonCoreLand();
        }
    },
    POPULATION(false) {
        @Override
        public int get(DBNation nation) {
            return (int) nation.getPopulation();
        }
    },
    WAR_INDEX(false) {
        @Override
        public int get(DBNation nation) {
            return (int) nation.getWarIndex();
        }
    },
    TAX_INCOME(false) {
        @Override
        public int get(DBNation nation) {
            return (int) nation.getTaxIncome();
        }
    },
    OTHER_INCOME(false) {
        @Override
        public int get(DBNation nation) {
            return (int) nation.getOtherIncome();
        }
    },
    CORPORATION_INCOME(false) {
        @Override
        public int get(DBNation nation) {
            return (int) nation.getCorporationIncome();
        }
    },
    MINERAL_INCOME(false) {
        @Override
        public int get(DBNation nation) {
            return (int) nation.getMineralOutput();
        }
    },
    FUEL_INCOME(false) {
        @Override
        public int get(DBNation nation) {
            return (int) nation.getFuelOutput();
        }
    },
    SCORE_INCOME(false) {
        @Override
        public int get(DBNation nation) {
            return (int) nation.getScore();
        }
    },
    ;

    public static final ConflictMetric[] values = values();
    private final boolean isDay;

    ConflictMetric(boolean isDay) {
        this.isDay = isDay;
    }

    public abstract int get(DBNation nation);

    public boolean isDay() {
        return isDay;
    }

    public record Entry(ConflictMetric metric, int conflictId, int allianceId, boolean side, long turnOrDay, int city, int value) {
    }
}

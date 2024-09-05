package link.locutus.discord.api.types.espionage;

public enum CyberOps implements IEspionage {
    PAY_COMMERCE_SYSTEMS(Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0, false),
    PAY_POWER_RANSOM(Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0, false),
    PAY_EDUCATION_RANSOM(Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0, false),
    INFLUENCE_NEWS_NEGATIVELY(2, 0, 1, 12, false),
    INFLUENCE_NEWS_POSITIVELY(1, -1, 1, 24, true),
    HIJACK_COMMERCE_SYSTEMS(4, 3, 4, 24, false),
    RANSOM_COMMERCE_SYSTEMS(4, 1, 5, 24, false),
    RECOVER_COMMERCE_SYSTEMS(4, -1, 6, 12, true, 2),
    HIJACK_POWER_SYSTEMS(4, 3, 7, 24, false),
    RANSOM_POWER_SYSTEMS(4, 1, 8, 24, false),
    RECOVER_POWER_SYSTEMS(4, -1, 9, 12, true, 2),
    HIJACK_EDUCATION_SYSTEMS(4, 3, 10, 24, false),
    RANSOM_EDUCATION_SYSTEMS(4, 1, 11, 24, false),
    RECOVER_EDUCATION_SYSTEMS(4, -1, 12, 12, true, 2),
    BANK_HACK(3, 6, 13, 12, false);

    private final int failure;
    private final int reveal;
    private final int level;
    private final int cost;
    private final boolean self;
    private final int addRansomCost;

    CyberOps(int failure, int reveal, int level, int cost, boolean self) {
        this(failure, reveal, level, cost, self, 0);
    }

    CyberOps(int failure, int reveal, int level, int cost, boolean self, int addRansomCost) {
        this.failure = failure;
        this.reveal = reveal;
        this.level = level;
        this.cost = cost;
        this.self = self;
        this.addRansomCost = addRansomCost;
    }

    @Override
    public int getFailureDifficulty() {
        return failure;
    }
    @Override
    public int getRevealDifficulty() {
        return reveal;
    }
    @Override
    public int getLevel() {
        return level;
    }
    @Override
    public int getCost(boolean ransomed) {
        return cost + (ransomed ? addRansomCost : 0);
    }
    @Override
    public boolean canTargetSelf() {
        return self;
    }
}

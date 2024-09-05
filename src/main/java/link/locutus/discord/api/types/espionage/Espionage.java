package link.locutus.discord.api.types.espionage;

public enum Espionage implements IEspionage {
    GATHER_INTELLIGENCE(1, -1, 1, 4, false),
    PLANT_AGENTS(1, 0, 2, 12, false),
    INVESTIGATE_GOVERNMENT(1, 0, 2, 12, true),
    ASSASSINATE_AGENTS(3, 4, 3, 12, false),
    INFILTRATE_RESEARCH_LABS(4, 1, 5, 12, false),
    COUNTER_ESPIONAGE_OP(0, 0, 6, 12, true),
    BRIBE_POLITICIAN(3, 2, 7, 12, false),
    INVESTIGATE_POLITICIAN(2, -1, 8, 12, true),
    ASSASSINATE_POLITICIAN(5, 3, 9, 12, false),
    DISRUPT_ESPIONAGE_OPERATIONS(3, 4, 10, 12, false),
    BRIBE_SCIENTIST(2, 3, 11, 24, false),
    INVESTIGATE_SCIENTIST(1, -1, 12, 12, true),
    ASSASSINATE_SCIENTIST(4, 3, 13, 12, false),
    INFILTRATE_MILITARY(3, 0, 14, 12, false),
    INVESTIGATE_MILITARY(1, 0, 15, 12, true),
    ASSASSINATE_GENERAL(6, 4, 16, 12, false),
    TERRORIST_ATTACK(4, 6, 17, 12, false);

    private final int failure;
    private final int reveal;
    private final int level;
    private final int cost;
    private final boolean self;

    Espionage(int failure, int reveal, int level, int cost, boolean self) {
        this.failure = failure;
        this.reveal = reveal;
        this.level = level;
        this.cost = cost;
        this.self = self;
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
        return cost;
    }
    @Override
    public boolean canTargetSelf() {
        return self;
    }
}

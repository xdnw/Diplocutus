package link.locutus.discord.api.types.espionage;

public interface IEspionage {
    int getFailureDifficulty();

    int getRevealDifficulty();

    int getLevel();

    int getCost(boolean ransomed);

    boolean canTargetSelf();
}

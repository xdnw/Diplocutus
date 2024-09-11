package link.locutus.discord.api.types;

public class NationModifier {
    public int TECH_OUTPUT;
    public double TECH_OUTPUT_PERCENT;

    public double getTechOutput(double educationIndex) {
        return TECH_OUTPUT * (1 + (TECH_OUTPUT_PERCENT * 0.01)) * ((educationIndex * 0.01));
    }
}

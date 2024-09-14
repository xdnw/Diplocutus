package link.locutus.discord.api.types;

public class NationModifier {
    public int TECH_OUTPUT;
    public double TECH_OUTPUT_PERCENT = 1;

    public double getTechOutput(double educationIndex) {
        System.out.println("TECH " + TECH_OUTPUT_PERCENT + " | " + educationIndex);
        return TECH_OUTPUT * TECH_OUTPUT_PERCENT * ((educationIndex * 0.01));
    }
}

package link.locutus.discord.api.types;

public class TaxRate {
    public int percent;

    public TaxRate(String parse) {
        this.percent = Integer.parseInt(parse.replace("%", ""));
    }

    @Override
    public String toString() {
        return percent + "%";
    }
}

package link.locutus.discord.db.entities;

import link.locutus.discord.util.MathMan;
import link.locutus.discord.util.StringMan;

import java.util.ArrayList;
import java.util.List;

public class MMRDouble {
    public final double[] mmr;

    public MMRDouble(double[] mmr) {
        this.mmr = mmr;
    }

    public static MMRDouble fromString(String input) {
        double[] mmr = new double[3];
        if (input.length() == 3) {
            mmr[0] = (int) (input.charAt(0) - '0');
            mmr[1] = (int) (input.charAt(1) - '0');
            mmr[2] = (int) (input.charAt(2) - '0');
        } else if (input.contains("/") || input.contains(",")) {
            String[] split = input.split("[/,]");
            mmr[0] = Double.parseDouble(split[0]);
            mmr[1] = Double.parseDouble(split[1]);
            mmr[2] = Double.parseDouble(split[2]);
        } else {
            throw new IllegalArgumentException("MMR must be 3 numbers. Provided value: `" + input + "`");
        }
        return new MMRDouble(mmr);
    }

    @Override
    public String toString() {
        List<String> valueStr = new ArrayList<>();
        for (double value : mmr) {
            valueStr.add(MathMan.format(value));
        }
        return StringMan.join(valueStr, "/");
    }

    public double getArmyBases() {
        return mmr[0];
    }

    public double getAirBases() {
        return mmr[1];
    }

    public double getNavalBases() {
        return mmr[2];
    }
}

package link.locutus.discord.api.types;


import java.util.function.BiFunction;
import java.util.function.Function;

public enum WarCostMode {
    DEALT(true, false, true, false),
    NET_DEALT(true, true, true, false),
    PROFIT(false, true, false, false),
    LOSSES(false, true, false, true),
    NET_LOSSES(true, true, false, true),
    ;

    private final boolean includeDealt;
    private final boolean includeReceived;
    private final boolean addDealt;
    private final boolean addReceived;

    WarCostMode(boolean includeDealt, boolean includeReceived, boolean addDealt, boolean addReceived) {
        this.includeDealt = includeDealt;
        this.includeReceived = includeReceived;
        this.addDealt = addDealt;
        this.addReceived = addReceived;
    }

    public boolean includeDealt() {
        return includeDealt;
    }

    public boolean includeReceived() {
        return includeReceived;
    }

    public boolean addDealt() {
        return addDealt;
    }

    public boolean addReceived() {
        return addReceived;
    }

    public BiFunction<Double, Double, Double> getCostFunc() {
        Function<Double, Double> applyDealt;
        Function<Double, Double> applyReceived;
        if (includeDealt()) {
            if (addDealt()) {
                applyDealt = value -> value;
            } else {
                applyDealt = value -> -value;
            }
        } else {
            applyDealt = value -> 0d;
        }
        if (includeReceived()) {
            if (addReceived()) {
                applyReceived = value -> value;
            } else {
                applyReceived = value -> -value;
            }
        } else {
            applyReceived = value -> 0d;
        }
        BiFunction<Double, Double, Double> applyBoth;
        if (applyDealt != null) {
            if (applyReceived != null) {
                applyBoth = (received, dealt) -> applyDealt.apply(dealt) + applyReceived.apply(received);
            } else {
                applyBoth = (received, dealt) -> applyDealt.apply(dealt);
            }
        } else if (applyReceived != null) {
            applyBoth = (received, dealt) -> applyReceived.apply(received);
        } else {
            applyBoth = (received, dealt) -> 0d;
        }
        return applyBoth;
    }


}

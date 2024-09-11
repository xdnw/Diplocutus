package link.locutus.discord.api.types;

import link.locutus.discord.api.generated.AllianceMilitary;

import java.util.Map;
import java.util.function.Function;

public enum MilitaryUnitType {
    INFANTRY(f -> f.InfantryCapacity, f -> f.UsedInfantryCapacity),
    ARMOR(f -> f.ArmourCapacity, f -> f.UsedArmourCapacity),
    ARTILLERY(f -> f.ArtilleryCapacity, f -> f.UsedArtilleryCapacity),
    AIR(f -> f.AirCapacity, f -> f.UsedAirCapacity),
    NAVAL(f -> f.NavalCapacity, f -> f.UsedNavalCapacity);

    ;

    private final Function<AllianceMilitary, Integer> getCapacity;
    private final Function<AllianceMilitary, Integer> getUsedCapacity;

    MilitaryUnitType(Function<AllianceMilitary, Integer> getCapacity, Function<AllianceMilitary, Integer> getUsedCapacity) {
        this.getCapacity = getCapacity;
        this.getUsedCapacity = getUsedCapacity;
    }

    public static final MilitaryUnitType[] values = values();

    public static MilitaryUnitType parse(String input) {
        return valueOf(input.toUpperCase().replace(" ", "_"));
    }

    public int getCapacity(AllianceMilitary military) {
        return getCapacity.apply(military);
    }

    public int getUsedCapacity(AllianceMilitary military) {
        return getUsedCapacity.apply(military);
    }

    public int getUsedCapacity(Map<MilitaryUnit, Integer> military) {
        int used = 0;
        for (Map.Entry<MilitaryUnit, Integer> entry : military.entrySet()) {
            if (entry.getKey().getCapacityType() == this) {
                used += entry.getValue() * entry.getKey().getCapacityUsage();
            }
        }
        return used;
    }
}

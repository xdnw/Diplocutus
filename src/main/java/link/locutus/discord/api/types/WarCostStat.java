package link.locutus.discord.api.types;

import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.db.entities.DBWar;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;

public enum WarCostStat {
    WAR_VALUE,
    LAND,
    DEVASTATION,

    CASH(ResourceType.CASH),
    TECHNOLOGY(ResourceType.TECHNOLOGY),
    PRODUCTION(ResourceType.PRODUCTION),
    MINERALS(ResourceType.MINERALS),
    URANIUM(ResourceType.URANIUM),
    RARE_METALS(ResourceType.RARE_METALS),
    FUEL(ResourceType.FUEL),
    POLITICAL_SUPPORT(ResourceType.POLITICAL_SUPPORT),
    CRYPTO(ResourceType.CRYPTO),

    INFANTRY(MilitaryUnit.INFANTRY),
    SUPPORT_EQUIPMENT(MilitaryUnit.SUPPORT_EQUIPMENT),
    LIGHT_TANKS(MilitaryUnit.LIGHT_TANKS),
    MEDIUM_TANKS(MilitaryUnit.MEDIUM_TANKS),
    HEAVY_TANKS(MilitaryUnit.HEAVY_TANKS),
    LIGHT_MECHS(MilitaryUnit.LIGHT_MECHS),
    HEAVY_MECHS(MilitaryUnit.HEAVY_MECHS),
    PRECURSOR_MECHS(MilitaryUnit.PRECURSOR_MECHS),
    ARTILLERY(MilitaryUnit.ARTILLERY),
    MISSILE_LAUNCHERS(MilitaryUnit.MISSILE_LAUNCHERS),
    FIGHTERS(MilitaryUnit.FIGHTERS),
    BOMBERS(MilitaryUnit.BOMBERS),
    HELICOPTERS(MilitaryUnit.HELICOPTERS),
    DRONES(MilitaryUnit.DRONES),
    STEALTH_FIGHTERS(MilitaryUnit.STEALTH_FIGHTERS),
    STEALTH_BOMBERS(MilitaryUnit.STEALTH_BOMBERS),
    DESTROYERS(MilitaryUnit.DESTROYERS),
    SUBMARINES(MilitaryUnit.SUBMARINES),
    CRUISERS(MilitaryUnit.CRUISERS),
    BATTLESHIPS(MilitaryUnit.BATTLESHIPS),
    CARRIERS(MilitaryUnit.CARRIERS),

    ;

    public static final WarCostStat[] values = values();
    static {
        // check that all resources and units are covered
        Set<MilitaryUnit> units = new HashSet<>(Arrays.asList(MilitaryUnit.values));
        Set<ResourceType> resources = new HashSet<>(Arrays.asList(ResourceType.values));
        for (WarCostStat stat : values) {
            if (stat.unit() != null) {
                units.remove(stat.unit());
            } else if (stat.resource() != null) {
                resources.remove(stat.resource());
            }
        }
        if (!units.isEmpty()) {
            throw new IllegalStateException("Missing units: " + units);
        }
        if (!resources.isEmpty()) {
            throw new IllegalStateException("Missing resources: " + resources);
        }
    }

    private final MilitaryUnit unit;
    private final ResourceType resource;

    WarCostStat() {
        this(null, null);
    }

    WarCostStat(MilitaryUnit unit, ResourceType resource) {
        this.unit = unit;
        this.resource = resource;
    }

    WarCostStat(MilitaryUnit unit) {
        this(unit, null);
    }

    WarCostStat(ResourceType resource) {
        this(null, resource);
    }

    public MilitaryUnit unit() {
        return this.unit;
    }

    public ResourceType resource() {
        return this.resource;
    }

    public final BiFunction<Boolean, DBWar, Double> getFunction(boolean excludeUnits, boolean excludeInfra, boolean excludeConsumption, boolean excludeLoot, boolean excludeBuildings) {
        // TODO FIXME :||remove
        return null;
//        if (unit != null) {
//            return (attacker, war) -> (double) war.getUnitLosses(unit(), attacker);
//        } else if (resource != null) {
//            double[] rssBuffer = ResourceType.getBuffer();
//            Arrays.fill(rssBuffer, 0);
//            return (attacker, war) -> {
//                rssBuffer[resource.ordinal()] = 0;
//                return war.getLosses(rssBuffer, attacker, !excludeUnits, !excludeInfra, !excludeConsumption, !excludeLoot, !excludeBuildings)[resource.ordinal()];
//            };
//        } else {
//            double[] rssBuffer = ResourceType.getBuffer();
//            return (attacker, attack) -> {
//                Arrays.fill(rssBuffer, 0);
//                return attack.getLossesConverted(rssBuffer, attacker, !excludeUnits, !excludeInfra, !excludeConsumption, !excludeLoot, !excludeBuildings);
//            };
//        }
    }
}

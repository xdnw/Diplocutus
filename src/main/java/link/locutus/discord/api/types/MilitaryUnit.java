package link.locutus.discord.api.types;

import link.locutus.discord.api.generated.AllianceMilitary;
import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.commands.manager.v2.binding.annotation.Command;
import link.locutus.discord.util.DNS;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public enum MilitaryUnit {
    // Infantry Equipment	1.25	0.625	0	None
    INFANTRY(CombatType.GROUND, 1, 2, 0.045, 2, MilitaryUnitType.INFANTRY, 2, 0.045, f -> f.Infantry, f -> f.InfantryQuality,
            1.25, 0.625, 0, null, null),
    //Support Equipment	250	125	0	Military Organization lvl 3
    SUPPORT_EQUIPMENT(CombatType.GROUND, 100, 100, 9, 5, MilitaryUnitType.INFANTRY, 1, 0.09, f -> f.SupportVehicles, f -> f.SupportVehiclesQuality,
            250, 125, 0, Map.of(Technology.MILITARY_ORGANIZATION, 3), null),
    //Light Tank	625	312.5	0	Tank Lvl 1
    LIGHT_TANKS(CombatType.GROUND, 1, 200, 12, 10, MilitaryUnitType.ARMOR, 200, 12, f -> f.LightTanks, f -> f.LightTanksQuality,
            625, 312.5, 0, Map.of(Technology.TANK_TECHNOLOGY, 1), null),
    //Medium Tank	1,875	937.5	0	Tank Lvl 6, Armour Lvl 7
    MEDIUM_TANKS(CombatType.GROUND, 2, 450, 30, 5, MilitaryUnitType.ARMOR, 225, 15, f -> f.MediumTanks, f -> f.MediumTanksQuality,
            1875, 937.5, 0, Map.of(Technology.TANK_TECHNOLOGY, 6, Technology.ARMOR_IMPROVEMENT, 7), null),
    //Heavy Tank	3,750	1875	200	Tank Lvl 13, Armour Lvl 14
    HEAVY_TANKS(CombatType.GROUND, 3.3, 900, 60, 2, MilitaryUnitType.ARMOR, 272, 18, f -> f.HeavyTanks, f -> f.HeavyTanksQuality,
            3750, 1875, 200, Map.of(Technology.TANK_TECHNOLOGY, 13, Technology.ARMOR_IMPROVEMENT, 14), null),
    //Light Mech	1,250	625	0	Mech Lvl 1, Infantry Equipment Lvl 14
    LIGHT_MECHS(CombatType.GROUND, 1.3, 300, 21, 10, MilitaryUnitType.ARMOR, 230, 16, f -> f.LightMechs, f -> f.LightTanksQuality,
            1250, 625, 0, Map.of(Technology.MECH_DEVELOPMENT, 1, Technology.INFANTRY_EQUIPMENT, 14), null),
    //Heavy Mech	3,750	1,875	300	Mech Lvl 14, Armour Lvl 16
    HEAVY_MECHS(CombatType.GROUND, 4, 1000, 84, 3, MilitaryUnitType.ARMOR, 250, 21, f -> f.HeavyMechs, f -> f.HeavyMechsQuality,
            3750, 1875, 300, Map.of(Technology.MECH_DEVELOPMENT, 14, Technology.ARMOR_IMPROVEMENT, 16), null),
    //Precursor Mech	12,500	6,250	1,000	Precursor Lvl 14, Armour Lvl 22, Precursor Mech Experimental Replication Project
    PRECURSOR_MECHS(CombatType.GROUND, 10, 3000, 210, 6, MilitaryUnitType.ARMOR, 300, 21, f -> f.PrescusarMech, f -> f.PrescusarMechQuality,
            12500, 6250, 1000, Map.of(Technology.PRECURSOR_TECHNOLOGY, 14, Technology.ARMOR_IMPROVEMENT, 22), Map.of(Project.PRECURSOR_MECH_EXPERIMENTAL_REPLICATION, 1)),
    //Artillery	750	375	0	Ordnance Lvl 1
    ARTILLERY(CombatType.GROUND, 1, 60, 18, 3, MilitaryUnitType.ARTILLERY, 60, 18, f -> f.Artillery, f -> f.ArtilleryQuality,
            750, 375, 0, Map.of(Technology.ORDNANCE_DEVELOPMENT, 1), null),
    //Missile Launcher	2,250	1,125	0	Rocketry Lvl 1
    MISSILE_LAUNCHERS(CombatType.GROUND, 4, 180, 73, 3, MilitaryUnitType.ARTILLERY, 45, 18, f -> f.MissileLaunchers, f -> f.MissileLaunchersQuality,
            2250, 1125, 0, Map.of(Technology.ROCKETRY, 1), null),
    //Fighter	2,500	1,250	0	Aerospace Lvl 1
    FIGHTERS(CombatType.AIR, 1, 1350, 60, 0, MilitaryUnitType.AIR, 1350, 60, f -> f.Fighters, f -> f.FightersQuality,
            2500, 1250, 0, Map.of(Technology.AEROSPACE_DEVELOPMENT, 1), null),
    //Bomber	2,500	1,250	0	Aerospace Lvl 7, Ordinace Lvl 11
    BOMBERS(CombatType.AIR, 1, 1350, 82.5, 0, MilitaryUnitType.AIR, 1350, 82.5, f -> f.Bombers, f -> f.BombersQuality,
            2500, 1250, 0, Map.of(Technology.AEROSPACE_DEVELOPMENT, 7, Technology.ORDNANCE_DEVELOPMENT, 11), null),
    //Helicopter	2,500	1,250	0	Aerospace Lvl 6
    HELICOPTERS(CombatType.AIR, 1, 1350, 49.5, 0, MilitaryUnitType.AIR, 1350, 49.5, f -> f.Helicopters, f -> f.HelicoptersQuality,
            2500, 1250, 0, Map.of(Technology.AEROSPACE_DEVELOPMENT, 6), null),
    //Drone	750	375	0	Aerospace Lvl 12, Robotics Lvl 12
    DRONES(CombatType.AIR, 0.5, 585, 24, 0, MilitaryUnitType.AIR, 1170, 48, f -> f.Drones, f -> f.DronesQuality,
            750, 375, 0, Map.of(Technology.AEROSPACE_DEVELOPMENT, 12, Technology.ROBOTICS, 12), null),
    //Stealth Fighter	5,000	2,500	400	Ordinance Lvl 23, Stealth Lvl 10, Stealth Frame Development Project
    STEALTH_FIGHTERS(CombatType.AIR, 3, 4950, 225, 0, MilitaryUnitType.AIR, 1650, 75, f -> f.StealthFighters, f -> f.StealthFightersQuality,
            5000, 2500, 400, Map.of(Technology.ORDNANCE_DEVELOPMENT, 23, Technology.STEALTH_TECHNOLOGY, 10), Map.of(Project.STEALTH_FRAME_DEVELOPMENT, 1)),
    //Stealth Bomber	5,000	2,500	400	Aerospace Lvl 16, Stealth Lvl 10, Stealth Frame Development Project
    STEALTH_BOMBERS(CombatType.AIR, 3, 4950, 277.5, 0, MilitaryUnitType.AIR, 1650, 112.5, f -> f.StealthBombers, f -> f.StealthBombersQuality,
            5000, 2500, 400, Map.of(Technology.AEROSPACE_DEVELOPMENT, 16, Technology.STEALTH_TECHNOLOGY, 10), Map.of(Project.STEALTH_FRAME_DEVELOPMENT, 1)),
    //Destroyer	37,500	18,750	0	Naval Lvl 1
    DESTROYERS(CombatType.NAVAL, 0.6, 12500, 468, 0, MilitaryUnitType.NAVAL, 20833, 780, f -> f.Destroyers, f -> f.DestroyersQuality,
            37500, 18750, 0, Map.of(Technology.NAVAL_TECHNOLOGY, 1), null),
    //Submarine	37,500	18,750	0	Naval Lvl 5, Stealth Lvl 1
    SUBMARINES(CombatType.NAVAL, 1, 15000, 720, 0, MilitaryUnitType.NAVAL, 15000, 720, f -> f.Subs, f -> f.SubsQuality,
            37500, 18750, 0, Map.of(Technology.NAVAL_TECHNOLOGY, 5, Technology.STEALTH_TECHNOLOGY, 1), null),
    //Cruiser	75,000	37,500	0	Naval Lvl 11, Rocketry Lvl 5
    CRUISERS(CombatType.NAVAL, 1.5, 23000, 1125, 0, MilitaryUnitType.NAVAL, 15333, 750, f -> f.Cruisers, f -> f.CruisersQuality,
            75000, 37500, 0, Map.of(Technology.NAVAL_TECHNOLOGY, 11, Technology.ROCKETRY, 5), null),
    //Battleship	1,250,000	625,000	0	Naval Lvl 7
    BATTLESHIPS(CombatType.NAVAL, 6, 150000, 5200, 0, MilitaryUnitType.NAVAL, 25000, 866, f -> f.Battleships, f -> f.BattleshipsQuality,
            1250000, 625000, 0, Map.of(Technology.NAVAL_TECHNOLOGY, 7), null),
    //Carrier	1,250,000	625,000	0	Naval Lvl 18
    CARRIERS(CombatType.NAVAL, 10, 125000, 2700, 0, MilitaryUnitType.NAVAL, 12500, 270, f -> f.Carriers, f -> f.CarriersQuality,
            1250000, 625000, 0, Map.of(Technology.NAVAL_TECHNOLOGY, 18), null),
    //Cruise Missile	50,000	100,000	10,000	Cruise Missile Program
    //Nuclear Missile	100,000	200,000	100,000 Uranium	Manhattan Project

    ;


    public static final MilitaryUnit[] values = values();
    private final CombatType combatType;
    private final double capacityUsage;
    private final int baseDefense;
    private final double baseAttack;
    private final int baseMobility;
    private final MilitaryUnitType capacityType;
    private final double defensePerCapacity;
    private final double attackPerCapacity;
    private final Function<AllianceMilitary, Integer> getAmount;
    private final Function<AllianceMilitary, Double> getQuality;
    private final double[] cost;

    MilitaryUnit(CombatType combatType, double capacityUsage,
                 int baseDefense, double baseAttack, int baseMobility, MilitaryUnitType capacityType, double defensePerCapacity, double attackPerCapacity,
                 Function<AllianceMilitary, Integer> getAmount, Function<AllianceMilitary, Double> getQuality,
                 double productionCost,
                 double mineralCost,
                 double rareMetalCost,
                 Map<Technology,Integer> requiredTech,
                 Map<Project,Integer> requiredProject) {
        this.combatType = combatType;
        this.capacityUsage = capacityUsage;
        this.baseDefense = baseDefense;
        this.baseAttack = baseAttack;
        this.baseMobility = baseMobility;
        this.capacityType = capacityType;
        this.defensePerCapacity = defensePerCapacity;
        this.attackPerCapacity = attackPerCapacity;
        this.getAmount = getAmount;
        this.getQuality = getQuality;
        this.cost = ResourceType.getBuffer();
        this.cost[ResourceType.PRODUCTION.ordinal()] = productionCost;
        this.cost[ResourceType.MINERALS.ordinal()] = mineralCost;
        this.cost[ResourceType.RARE_METALS.ordinal()] = rareMetalCost;
    }

    public double[] getCostArr() {
        return cost;
    }

    public Map<ResourceType, Double> getCost(int amt) {
        return ResourceType.resourcesToMap(DNS.multiply(cost, amt));
    }

    public static MilitaryUnit parse(String input) {
        String capitalized = input.toUpperCase(Locale.ROOT).replace(" ", "_");
        try {
            return valueOf(capitalized);
        } catch (IllegalArgumentException e) {}
        return switch (capitalized) {
            case "INFANTRY_EQUIPMENT" -> INFANTRY;
            case "SUPPORT_VEHICLES" -> SUPPORT_EQUIPMENT;
//            case "CRUISE_MISSILES" -> null; // Add appropriate enum value if exists
//            case "NUCLEAR_MISSILES" -> null; // Add appropriate enum value if exists
            default -> {
                throw new IllegalArgumentException("Invalid military unit: " + input);
            }
        };
    }

    public static int[] getBuffer() {
        return new int[values.length];
    }

    public static Map<MilitaryUnit, Integer> arrayToMap(int[] unitArr) {
        Map<MilitaryUnit, Integer> map = new EnumMap<>(MilitaryUnit.class);
        for (int i = 0; i < values.length; i++) {
            int amt = unitArr[i];
            if (amt != 0) map.put(values[i], amt);
        }
        return map;
    }

    public static int[] mapToArray(Map<MilitaryUnit, Integer> units) {
        int[] arr = getBuffer();
        for (Map.Entry<MilitaryUnit, Integer> entry : units.entrySet()) {
            arr[entry.getKey().ordinal()] = entry.getValue();
        }
        return arr;
    }

    @Command(desc = "Get the combat type of the military unit")
    public CombatType getCombatType() {
        return combatType;
    }

    @Command(desc = "Get the capacity usage of the military unit")
    public double getCapacityUsage() {
        return capacityUsage;
    }

    @Command(desc = "Get the base defense of the military unit")
    public int getBaseDefense() {
        return baseDefense;
    }

    @Command(desc = "Get the base attack of the military unit")
    public double getBaseAttack() {
        return baseAttack;
    }

    @Command(desc = "Get the base mobility of the military unit")
    public int getBaseMobility() {
        return baseMobility;
    }

    @Command(desc = "Get the type of the military unit")
    public MilitaryUnitType getCapacityType() {
        return capacityType;
    }

    @Command(desc = "Get the defense per capacity of the military unit")
    public double getDefensePerCapacity() {
        return defensePerCapacity;
    }

    @Command(desc = "Get the attack per capacity of the military unit")
    public double getAttackPerCapacity() {
        return attackPerCapacity;
    }

    @Command(desc = "Get the name of the military unit")
    public String getName() {
        return name();
    }

    public int getAmount(AllianceMilitary military) {
        return getAmount.apply(military);
    }

    public double getQuality(AllianceMilitary military) {
        return getQuality.apply(military);
    }
}

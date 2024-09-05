package link.locutus.discord.api.types;

import link.locutus.discord.api.generated.AllianceMilitary;
import link.locutus.discord.commands.manager.v2.binding.annotation.Command;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

public enum MilitaryUnit {

    INFANTRY(CombatType.GROUND, 1, 2, 0.045, 2, MilitaryUnitType.INFANTRY, 2, 0.045, f -> f.Infantry, f -> f.InfantryQuality),
    SUPPORT_EQUIPMENT(CombatType.GROUND, 100, 100, 9, 5, MilitaryUnitType.INFANTRY, 1, 0.09, f -> f.SupportVehicles, f -> f.SupportVehiclesQuality),
    LIGHT_TANKS(CombatType.GROUND, 1, 200, 12, 10, MilitaryUnitType.ARMOR, 200, 12, f -> f.LightTanks, f -> f.LightTanksQuality),
    MEDIUM_TANKS(CombatType.GROUND, 2, 450, 30, 5, MilitaryUnitType.ARMOR, 225, 15, f -> f.MediumTanks, f -> f.MediumTanksQuality),
    HEAVY_TANKS(CombatType.GROUND, 3.3, 900, 60, 2, MilitaryUnitType.ARMOR, 272, 18, f -> f.HeavyTanks, f -> f.HeavyTanksQuality),
    LIGHT_MECHS(CombatType.GROUND, 1.3, 300, 21, 10, MilitaryUnitType.ARMOR, 230, 16, f -> f.LightMechs, f -> f.LightTanksQuality),
    HEAVY_MECHS(CombatType.GROUND, 4, 1000, 84, 3, MilitaryUnitType.ARMOR, 250, 21, f -> f.HeavyMechs, f -> f.HeavyMechsQuality),
    PRECURSOR_MECHS(CombatType.GROUND, 10, 3000, 210, 6, MilitaryUnitType.ARMOR, 300, 21, f -> f.PrescusarMech, f -> f.PrescusarMechQuality),
    ARTILLERY(CombatType.GROUND, 1, 60, 18, 3, MilitaryUnitType.ARTILLERY, 60, 18, f -> f.Artillery, f -> f.ArtilleryQuality),
    MISSILE_LAUNCHERS(CombatType.GROUND, 4, 180, 73, 3, MilitaryUnitType.ARTILLERY, 45, 18, f -> f.MissileLaunchers, f -> f.MissileLaunchersQuality),
    FIGHTERS(CombatType.AIR, 1, 1350, 60, 0, MilitaryUnitType.AIR, 1350, 60, f -> f.Fighters, f -> f.FightersQuality),
    BOMBERS(CombatType.AIR, 1, 1350, 82.5, 0, MilitaryUnitType.AIR, 1350, 82.5, f -> f.Bombers, f -> f.BombersQuality),
    HELICOPTERS(CombatType.AIR, 1, 1350, 49.5, 0, MilitaryUnitType.AIR, 1350, 49.5, f -> f.Helicopters, f -> f.HelicoptersQuality),
    DRONES(CombatType.AIR, 0.5, 585, 24, 0, MilitaryUnitType.AIR, 1170, 48, f -> f.Drones, f -> f.DronesQuality),
    STEALTH_FIGHTERS(CombatType.AIR, 3, 4950, 225, 0, MilitaryUnitType.AIR, 1650, 75, f -> f.StealthFighters, f -> f.StealthFightersQuality),
    STEALTH_BOMBERS(CombatType.AIR, 3, 4950, 277.5, 0, MilitaryUnitType.AIR, 1650, 112.5, f -> f.StealthBombers, f -> f.StealthBombersQuality),
    DESTROYERS(CombatType.NAVAL, 0.6, 12500, 468, 0, MilitaryUnitType.NAVAL, 20833, 780, f -> f.Destroyers, f -> f.DestroyersQuality),
    SUBMARINES(CombatType.NAVAL, 1, 15000, 720, 0, MilitaryUnitType.NAVAL, 15000, 720, f -> f.Subs, f -> f.SubsQuality),
    CRUISERS(CombatType.NAVAL, 1.5, 23000, 1125, 0, MilitaryUnitType.NAVAL, 15333, 750, f -> f.Cruisers, f -> f.CruisersQuality),
    BATTLESHIPS(CombatType.NAVAL, 6, 150000, 5200, 0, MilitaryUnitType.NAVAL, 25000, 866, f -> f.Battleships, f -> f.BattleshipsQuality),
    CARRIERS(CombatType.NAVAL, 10, 125000, 2700, 0, MilitaryUnitType.NAVAL, 12500, 270, f -> f.Carriers, f -> f.CarriersQuality)
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

    MilitaryUnit(CombatType combatType, double capacityUsage,
                 int baseDefense, double baseAttack, int baseMobility, MilitaryUnitType capacityType, double defensePerCapacity, double attackPerCapacity,
                 Function<AllianceMilitary, Integer> getAmount, Function<AllianceMilitary, Double> getQuality) {
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

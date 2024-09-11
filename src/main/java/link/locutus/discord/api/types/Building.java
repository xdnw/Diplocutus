package link.locutus.discord.api.types;

import link.locutus.discord.api.generated.NationBuildings;
import link.locutus.discord.commands.manager.v2.binding.annotation.Command;

import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

public enum Building {
    RESEARCH_CENTERS(f -> f.ResearchCenters, 250000, 0.05, 100, 10000, 50000),
    ANCIENT_RUIN(f -> f.AncientRuin, 1000000, 0.005, 200, 10000, 200000),
    PRECURSOR_MATRIX(f -> f.PrecursorMatrix, 1000000, 0.003, 500, 10000, 400000, () -> Technology.PRECURSOR_TECHNOLOGY, 7),
    UNIVERSITIES(f -> f.Universitys, 500000, 0.01, 50, 10000, 50000, () -> Technology.EDUCATION_TECHNOLOGY, 3),
    SCHOOL_DISTRICTS(f -> f.SchoolDistricts, 250000, 0.05, 0, 5000, 25000),
    TRADE_SCHOOLS(f -> f.TradeSchools, 1000000, 0.05, 0, 10000, 100000, () -> Project.CAPITAL_UNIVERSITY),
    COMMERCIAL_DISTRICTS(f -> f.CommercialDistricts, 250000, 0.05, 0, 15000, 500000),
    TRADITIONAL_POWER_PLANTS(f -> f.TraditionalPowerPlants, 100000, 0.05, 0, 10000, 50000),
    SOLAR_PLANTS(f -> f.SolarPlants, 200000, 0.02, 0, 2500, 25000, () -> Technology.RENEWABLE_ENERGY, 7),
    WIND_PLANTS(f -> f.WindPlants, 300000, 0.01, 0, 5000, 50000, () -> Technology.RENEWABLE_ENERGY, 3),
    NUCLEAR_PLANTS(f -> f.NuclearPlants, 1000000, 0.02, 0, 10000, 50000, () -> Technology.NUCLEAR_TECHNOLOGY, 4),
    PRECURSOR_ZERO_POINT_REACTORS(f -> f.PrecursorZeroPointReactors, 1000000, 0.003, 0, 10000, 400000, () -> Project.PRECURSOR_REACTOR_ACTIVATION),
    FACTORY_DISTRICTS(f -> f.FactoryDistricts, 250000, 0.05, 0, 15000, 150000),
    PRECURSOR_FABRICATORS(f -> f.PrecursorFabricators, 1000000, 0.003, 0, 10000, 400000, () -> Technology.PRECURSOR_TECHNOLOGY, 2),
    MINING_DISTRICTS(f -> f.MiningDistricts, 250000, 0.05, 0, 15000, 125000),
    PRECURSOR_CORE_EXTRACTOR(f -> f.PrecursorCoreExtractors, 1000000, 0.003, 0, 10000, 400000, () -> Technology.PRECURSOR_TECHNOLOGY, 1),
    ENTERTAINMENT_DISTRICTS(f -> f.EntertainmentDistricts, 250000, 0.05, 0, 15000, 175000),
    FUEL_EXTRACTORS(f -> f.FuelExtractors, 250000, 0.5, 0, 10000, 200000),
    RICH_FUEL_FIELD(f -> f.RichFuelField, 1000000, 0.003, 0, 10000, 200000, () -> Technology.FUEL_EXTRACTION_TECHNOLOGY, 18),
    URANIUM_MINES(f -> f.UraniumMines, 1000000, 0.0035, 0, 10000, 200000, () -> Technology.MINING_TECHNOLOGY, 7),
    RARE_METAL_MINES(f -> f.RareMetalMines, 1000000, 0.0035, 0, 10000, 200000, () -> Technology.RARE_METAL_MINING, 1),
    ROADS(f -> f.Roads, 200000, 0.05, 0, 5000, 25000),
    RAIL_NETWORKS(f -> f.RailNetworks, 500000, 0.03, 0, 5000, 75000, () -> Technology.TRANSPORTATION_TECHNOLOGY, 1),
    AIRPORTS(f -> f.Airports, 1500000, 0.01, 0, 10000, 200000, () -> Technology.TRANSPORTATION_TECHNOLOGY, 4),
    PORTS(f -> f.Ports, 1000000, 0.01, 0, 10000, 250000, () -> Technology.TRANSPORTATION_TECHNOLOGY, 3),
    SUBWAY(f -> f.Subways, 1000000, 0.05, 0, 5000, 75000, () -> Project.CAPITAL_SUBWAY_SYSTEM),
    PRECURSOR_TELEPORTATION_HUBS(f -> f.PrecursorTeleportationHubs, 2000000, 0.0025, 0, 10000, 500000, () -> Technology.PRECURSOR_TECHNOLOGY, 5),
    ARMY_BASES(f -> f.ArmyBases, 500000, 0.015, 0, 10000, 50000),
    NAVAL_BASES(f -> f.NavalBases, 2000000, 0.01, 0, 10000, 25000),
    AIR_BASES(f -> f.AirBases, 1000000, 0.01, 0, 10000, 25000),
    RESIDENTIAL_DISTRICTS(f -> f.ResidentialDistricts, 250000, 0.05, 0, 100000, 0),

    // TODO FIXME :||remove missing from wiki
    RICH_MINING_AREA(f -> f.RichMiningArea, 250000, 0.005, 0, 0, 0, () -> Technology.RARE_METAL_MINING, 1),
    ;

    public static Building[] values = Building.values();

    private final int baseCost;
    private final double scaleFactor;
    private final int dailyProductionPerLevel;
    private final int jobsPerLevel;
    private final int corporationIncomePerLevel;
    private final Supplier<Technology> unlockRequirement;
    private final int level;
    private final Supplier<Project> requiresProject;
    private final Function<NationBuildings, Integer> get;

    Building(Function<NationBuildings, Integer> get, int baseCost, double scaleFactor, int dailyProductionPerLevel, int jobsPerLevel, int corporationIncomePerLevel) {
        this(get, baseCost, scaleFactor, dailyProductionPerLevel, jobsPerLevel, corporationIncomePerLevel, () -> null);
    }


    Building(Function<NationBuildings, Integer> get, int baseCost, double scaleFactor, int dailyProductionPerLevel, int jobsPerLevel, int corporationIncomePerLevel, Supplier<Project> requiresProject) {
        this(get, baseCost, scaleFactor, dailyProductionPerLevel, jobsPerLevel, corporationIncomePerLevel, () -> null, 0, requiresProject);
    }

    Building(Function<NationBuildings, Integer> get, int baseCost, double scaleFactor, int dailyProductionPerLevel, int jobsPerLevel, int corporationIncomePerLevel, Supplier<Technology> unlockRequirement, int level) {
        this(get, baseCost, scaleFactor, dailyProductionPerLevel, jobsPerLevel, corporationIncomePerLevel, unlockRequirement, level, () -> null);
    }

    Building(Function<NationBuildings, Integer> get, int baseCost, double scaleFactor, int dailyProductionPerLevel, int jobsPerLevel, int corporationIncomePerLevel, Supplier<Technology> unlockRequirement, int level, Supplier<Project> requiresProject) {
        this.get = get;
        this.baseCost = baseCost;
        this.scaleFactor = scaleFactor;
        this.dailyProductionPerLevel = dailyProductionPerLevel;
        this.jobsPerLevel = jobsPerLevel;
        this.corporationIncomePerLevel = corporationIncomePerLevel;
        this.unlockRequirement = unlockRequirement;
        this.requiresProject = requiresProject;
        this.level = level;
    }

    public static Building parse(String input) {
        for (Building building : values) {
            if (building.name().equalsIgnoreCase(input)) {
                return building;
            }
            if (building.name().replace("_", " ").equalsIgnoreCase(input)) {
                return building;
            }
        }
        return null;
    }

    public int get(NationBuildings buildings) {
        Integer amt = get.apply(buildings);
        return amt == null ? 0 : amt;
    }

    @Command(desc = "Get the name of the building")
    public String getName() {
        return name();
    }

    @Command(desc = "Get the base cost of the building")
    public int getBaseCost() {
        return baseCost;
    }

    @Command(desc = "Get the scale factor of the building")
    public double getScaleFactor() {
        return scaleFactor;
    }

    @Command(desc = "Get the daily production per level of the building")
    public int getDailyProductionPerLevel() {
        return dailyProductionPerLevel;
    }

    @Command(desc = "Get the jobs per level of the building")
    public int getJobsPerLevel() {
        return jobsPerLevel;
    }

    @Command(desc = "Get the corporation income per level of the building")
    public int getCorporationIncomePerLevel() {
        return corporationIncomePerLevel;
    }

    @Command(desc = "Get the unlock requirement of the building")
    public Technology getUnlockRequirement() {
        return unlockRequirement.get();
    }

    @Command(desc = "Get the required project of the building")
    public Project getRequiredProject() {
        return requiresProject.get();
    }

    @Command(desc = "Get the level of the building")
    public int getLevel() {
        return level;
    }
}

package link.locutus.discord.api.types;

import link.locutus.discord.api.generated.NationBuildings;
import link.locutus.discord.api.generated.NationsEffectsSummary;
import link.locutus.discord.commands.manager.v2.binding.annotation.Command;
import link.locutus.discord.db.entities.DBNation;

import java.util.Map;
import java.util.function.*;

public enum Building {
    RESEARCH_CENTERS(f -> f.ResearchCenters, f -> f.researchCenter, 250000, 0.05, 100, 10000, 50000) {
        @Override
        public void apply(NationModifier modifier, int level) {
            modifier.TECH_OUTPUT += 100 * level;
        }
    },
    ANCIENT_RUIN(f -> f.AncientRuin, f -> 0, 1000000, 0.005, 200, 10000, 200000) {
        @Override
        public void apply(NationModifier modifier, int level) {
            modifier.TECH_OUTPUT += 200 * level;
        }
    },
    PRECURSOR_MATRIX(f -> f.PrecursorMatrix, f -> 0, 1000000, 0.003, 500, 10000, 400000, () -> Technology.PRECURSOR_TECHNOLOGY, 7) {
        @Override
        public void apply(NationModifier modifier, int level) {
            modifier.TECH_OUTPUT += 500 * level;
        }
    },
    UNIVERSITIES(f -> f.Universitys, f -> f.university, 500000, 0.01, 50, 10000, 50000, () -> Technology.EDUCATION_TECHNOLOGY, 3) {
        @Override
        public void apply(NationModifier modifier, int level) {
            modifier.TECH_OUTPUT += 50 * level;
        }
    },
    SCHOOL_DISTRICTS(f -> f.SchoolDistricts, f -> f.schoolDistrict, 250000, 0.05, 0, 5000, 25000),
    TRADE_SCHOOLS(f -> f.TradeSchools, f -> 0, 1000000, 0.05, 0, 10000, 100000, () -> Project.CAPITAL_UNIVERSITY),
    COMMERCIAL_DISTRICTS(f -> f.CommercialDistricts, f -> f.commerceDistrict, 250000, 0.05, 0, 15000, 500000),
    TRADITIONAL_POWER_PLANTS(f -> f.TraditionalPowerPlants, f -> f.traditionalPower, 100000, 0.05, 0, 10000, 50000),
    SOLAR_PLANTS(f -> f.SolarPlants, f -> f.solar, 200000, 0.02, 0, 2500, 25000, () -> Technology.RENEWABLE_ENERGY, 7),
    WIND_PLANTS(f -> f.WindPlants, f -> f.wind, 300000, 0.01, 0, 5000, 50000, () -> Technology.RENEWABLE_ENERGY, 3),
    NUCLEAR_PLANTS(f -> f.NuclearPlants, f -> f.nuclear, 1000000, 0.02, 0, 10000, 50000, () -> Technology.NUCLEAR_TECHNOLOGY, 4),
    PRECURSOR_ZERO_POINT_REACTORS(f -> f.PrecursorZeroPointReactors, f -> 0, 1000000, 0.003, 0, 10000, 400000, () -> Project.PRECURSOR_REACTOR_ACTIVATION),
    FACTORY_DISTRICTS(f -> f.FactoryDistricts, f -> f.factoryDistrict, 250000, 0.05, 0, 15000, 150000),
    PRECURSOR_FABRICATORS(f -> f.PrecursorFabricators, f -> 0, 1000000, 0.003, 0, 10000, 400000, () -> Technology.PRECURSOR_TECHNOLOGY, 2),
    MINING_DISTRICTS(f -> f.MiningDistricts, f -> f.miningDistrict, 250000, 0.05, 0, 15000, 125000),
    PRECURSOR_CORE_EXTRACTOR(f -> f.PrecursorCoreExtractors, f -> 0, 1000000, 0.003, 0, 10000, 400000, () -> Technology.PRECURSOR_TECHNOLOGY, 1),
    ENTERTAINMENT_DISTRICTS(f -> f.EntertainmentDistricts, f -> 0, 250000, 0.05, 0, 15000, 175000),
    FUEL_EXTRACTORS(f -> f.FuelExtractors, f -> f.fuelExtractor, 250000, 0.5, 0, 10000, 200000),
    RICH_FUEL_FIELD(f -> f.RichFuelField, f -> 0, 1000000, 0.003, 0, 10000, 200000, () -> Technology.FUEL_EXTRACTION_TECHNOLOGY, 18),
    URANIUM_MINES(f -> f.UraniumMines, f -> f.uraniumMine, 1000000, 0.0035, 0, 10000, 200000, () -> Technology.MINING_TECHNOLOGY, 7),
    RARE_METAL_MINES(f -> f.RareMetalMines, f -> f.rareMetalMine, 1000000, 0.0035, 0, 10000, 200000, () -> Technology.RARE_METAL_MINING, 1),
    ROADS(f -> f.Roads, f -> f.roads, 200000, 0.05, 0, 5000, 25000),
    RAIL_NETWORKS(f -> f.RailNetworks, f -> f.railNetwork, 500000, 0.03, 0, 5000, 75000, () -> Technology.TRANSPORTATION_TECHNOLOGY, 1),
    AIRPORTS(f -> f.Airports, f -> f.airport, 1500000, 0.01, 0, 10000, 200000, () -> Technology.TRANSPORTATION_TECHNOLOGY, 4),
    PORTS(f -> f.Ports, f -> f.port, 1000000, 0.01, 0, 10000, 250000, () -> Technology.TRANSPORTATION_TECHNOLOGY, 3),
    SUBWAY(f -> f.Subways, f -> 0, 100000000, 0.05, 0, 5000, 75000, () -> Project.CAPITAL_SUBWAY_SYSTEM),
    PRECURSOR_TELEPORTATION_HUBS(f -> f.PrecursorTeleportationHubs, f -> 0, 2000000, 0.0025, 0, 10000, 500000, () -> Technology.PRECURSOR_TECHNOLOGY, 5),
    ARMY_BASES(f -> f.ArmyBases, f -> f.armyBase, 500000, 0.015, 0, 10000, 50000),
    NAVAL_BASES(f -> f.NavalBases, f -> f.navalBase, 2000000, 0.01, 0, 10000, 25000),
    AIR_BASES(f -> f.AirBases, f -> f.airBase, 1000000, 0.01, 0, 10000, 25000),
    RESIDENTIAL_DISTRICTS(f -> f.ResidentialDistricts, f -> f.residentialDistrict, 250000, 0.05, 0, 0, 100000),

    // TODO FIXME :||remove missing from wiki
    RICH_MINING_AREA(f -> f.RichMiningArea, f -> 0, 250000, 0.005, 0, 10000, 200000, () -> Technology.RARE_METAL_MINING, 1),
    ;

    public static Building[] values = Building.values();

    private final int baseCost;
    private final double scaleFactor;
    private final int dailyProductionPerLevel;
    private final int jobsPerLevel;
    private final int corporationIncomePerLevel;
    private final Supplier<Technology> unlockRequirement;
    private final int techLevel;
    private final Supplier<Project> requiresProject;
    private final Function<NationBuildings, Integer> get;
    private final Function<NationsEffectsSummary, Integer> getEffect;

    Building(Function<NationBuildings, Integer> get, Function<NationsEffectsSummary, Integer> getEffect, int baseCost, double scaleFactor, int dailyProductionPerLevel, int jobsPerLevel, int corporationIncomePerLevel) {
        this(get, getEffect, baseCost, scaleFactor, dailyProductionPerLevel, jobsPerLevel, corporationIncomePerLevel, () -> null);
    }


    Building(Function<NationBuildings, Integer> get, Function<NationsEffectsSummary, Integer> getEffect, int baseCost, double scaleFactor, int dailyProductionPerLevel, int jobsPerLevel, int corporationIncomePerLevel, Supplier<Project> requiresProject) {
        this(get, getEffect, baseCost, scaleFactor, dailyProductionPerLevel, jobsPerLevel, corporationIncomePerLevel, () -> null, 0, requiresProject);
    }

    Building(Function<NationBuildings, Integer> get, Function<NationsEffectsSummary, Integer> getEffect, int baseCost, double scaleFactor, int dailyProductionPerLevel, int jobsPerLevel, int corporationIncomePerLevel, Supplier<Technology> unlockRequirement, int level) {
        this(get, getEffect, baseCost, scaleFactor, dailyProductionPerLevel, jobsPerLevel, corporationIncomePerLevel, unlockRequirement, level, () -> null);
    }

    Building(Function<NationBuildings, Integer> get, Function<NationsEffectsSummary, Integer> getEffect, int baseCost, double scaleFactor, int dailyProductionPerLevel, int jobsPerLevel, int corporationIncomePerLevel, Supplier<Technology> unlockRequirement, int level, Supplier<Project> requiresProject) {
        this.get = get;
        this.getEffect = getEffect;
        this.baseCost = baseCost;
        this.scaleFactor = scaleFactor;
        this.dailyProductionPerLevel = dailyProductionPerLevel;
        this.jobsPerLevel = jobsPerLevel;
        this.corporationIncomePerLevel = corporationIncomePerLevel;
        this.unlockRequirement = unlockRequirement;
        this.requiresProject = requiresProject;
        this.techLevel = level;
    }

    public double cost(int level, int totalSlots, double costReduction) {
        return Math.floor((baseCost + (0.005 / scaleFactor) * level * baseCost) * Math.max(1, Math.pow((level / (totalSlots * scaleFactor)), 2)) * costReduction);
    }

    public double cost(int levelStart, int levelEnd, int totalSlots, double costReduction) {
        if (levelEnd > 1000) {
            throw new IllegalArgumentException("Level end is too high (max: 1000, provided: " + levelEnd + ")");
        }
        double cost = 0;
        for (int i = levelStart; i < levelEnd; i++) {
            cost += cost(i, totalSlots, costReduction);
        }
        return cost;
    }

    public void apply(NationModifier modifier, int level) {

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

    public static int getJobs(Map<Building, Integer> buildings) {
        return buildings.entrySet().stream().mapToInt(e -> e.getKey().jobsPerLevel * e.getValue()).sum();
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
    public int getTechLevel() {
        return techLevel;
    }

    public int get(NationsEffectsSummary result) {
        return getEffect.apply(result);
    }

    public String getCantBuildReason(Function<Project, Integer> hasProject, Function<Technology, Integer> hasTech) {
        if (requiresProject != null && requiresProject.get() != null) {
            if (hasProject.apply(requiresProject.get()) == 0) {
                return "Requires project " + requiresProject.get().name();
            }
        }
        if (unlockRequirement != null && unlockRequirement.get() != null) {
            int currentLevel = hasTech.apply(unlockRequirement.get());
            if (currentLevel < techLevel) {
                return "Requires " + unlockRequirement.get().name() + " level " + techLevel + " (current: " + currentLevel + ")";
            }
        }
        return null;
    }

    public boolean canBuild(Function<Project, Integer> hasProject, Function<Technology, Integer> hasTech) {
        return getCantBuildReason(hasProject, hasTech) == null;
    }
}

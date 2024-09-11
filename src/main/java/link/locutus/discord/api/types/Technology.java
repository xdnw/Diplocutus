package link.locutus.discord.api.types;

import link.locutus.discord.api.generated.AllianceTech;
import link.locutus.discord.commands.manager.v2.binding.annotation.Command;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public enum Technology {
// Science Technologies
//Scientific Theory
//Initial Cost: 300 tech points
//Requirements: None
//+1 % Tech Cost Reduction per level
//+1% Tech Output per level
//-100 Net Base Tech Cost per level
//+100 technology output per level
//Required for unlocking many different technology's
/* enum */ SCIENTIFIC_THEORY(TechnologyCategory.SCIENCE, 300, f -> f.ScientificTheory) {
    @Override
    public void apply(NationModifier modifier, int level) {
        modifier.TECH_OUTPUT += 100 * level;
        modifier.TECH_OUTPUT_PERCENT += 1 * level;
    }
},
//Espionage
//Initial Cost: 300 tech points
//Requirements: Scientific Theory level 2
//Unlocks new espionage operations
//Increases the success change of espionage operation
//Increases base war intel by 1% per level
//+200 technology output per level
/* enum */ ESPIONAGE(TechnologyCategory.SCIENCE, 300, () -> SCIENTIFIC_THEORY, 2, f -> f.Espionage) {
    @Override
    public void apply(NationModifier modifier, int level) {
        modifier.TECH_OUTPUT += 200 * level;
    }
},
//Counter Intelligence
//Initial Cost: 300 tech points
//Requirements: Espionage level 3
//Decreases the success change of espionage operations targeting your nation
//Reduces hostile base war intel by 1% per level
//+2 stability index per level
/* enum */ COUNTER_INTELLIGENCE(TechnologyCategory.SCIENCE, 300, () -> ESPIONAGE, 3, f -> f.CounterIntelligence),
//Education Technology
//Initial Cost: 1500 tech points
//Requirements: Scientific Theory level 1
//+3% education efficiency by per level
//+3 Education Index per level
/* enum */ EDUCATION_TECHNOLOGY(TechnologyCategory.SCIENCE, 1500, () -> SCIENTIFIC_THEORY, 1, f -> f.EducationTechnologys),
//Entertainment Technology
//Initial Cost: 2500 tech points
//Requirements: Scientific Theory Level 4
//+5% entertainment efficiency by per level
//+1 Commerce Index per level
/* enum */ ENTERTAINMENT_TECHNOLOGY(TechnologyCategory.SCIENCE, 2500, () -> SCIENTIFIC_THEORY, 4, f -> f.Entertainment),
//Computer Technology
//Initial Cost: 5000 tech points
//Requirements: Scientific Theory Level 6
//+5% Research Center Output by per level
//+2 Commerce Index per level
    /* enum */ COMPUTER_TECHNOLOGY(TechnologyCategory.SCIENCE, 5000, () -> SCIENTIFIC_THEORY, 6, f -> f.ComputerTech),
//Virtual Reality
//Initial Cost: 50,000 tech points
//Requirements: Entertainment Technology Level 7 and Computer Technology Level 5
//+1% policy cost reduction per level
//+2 Stability Index per level
//+2 Commerce Index per level
    /* enum */ VIRTUAL_REALITY(TechnologyCategory.SCIENCE, 50000, () -> ENTERTAINMENT_TECHNOLOGY, 7, f -> f.VirtualReality),
//Artificial Intelligence
//Initial Cost: 10,000 tech points
//Requirements: Computer Technology Level 6
//+1% Tech cost reduction per level
//+300 tech output per level
//-100 Net Base Tech Cost per level
    /* enum */ ARTIFICIAL_INTELLIGENCE(TechnologyCategory.SCIENCE, 10000, () -> COMPUTER_TECHNOLOGY, 6, f -> f.ArtificialIntelligence) {
    @Override
    public void apply(NationModifier modifier, int level) {
        modifier.TECH_OUTPUT += 300 * level;
    }
},
//Nuclear Technology
//Initial Cost: 10,000 tech points
//Requirements: Energy Technology Level 6
//+1 Commerce Index per level
//+1 Power Index per level
//+5% nuclear plants efficiency per level
    /* enum */ NUCLEAR_TECHNOLOGY(TechnologyCategory.SCIENCE, 10000, new Supplier<>() {
    @Override
    public Technology get() {
        return ENERGY_TECHNOLOGY;
    }
}, 6, f -> f.Nuclear),
//Rocketry
//Initial Cost: 50,000 tech points
//Requirements: Scientific Theory Level 10
//Unlocks Missile Launcher production at level 1
//+1 Missile Launcher Quality per level
//+0.5 Cruiser Quality per level(This affects rounds down to the nearest integer)
    /* enum */ ROCKETRY(TechnologyCategory.SCIENCE, 50000, () -> SCIENTIFIC_THEORY, 10, f -> f.Rocketry),
//Space Exploration
//Initial Cost: 200,000 tech points
//Requirements: Rocketry Level 3
//+1000 tech output per level
//Will unlock !?!?!??!??! in the future
    /* enum */ SPACE_EXPLORATION(TechnologyCategory.SCIENCE, 200000, () -> ROCKETRY, 3, f -> f.SpaceExploration) {
    @Override
    public void apply(NationModifier modifier, int level) {
        modifier.TECH_OUTPUT += 1000 * level;
    }
},
//Orbital Construction
//Initial Cost: 200,000 tech points
//Requirements: Rocketry Level 7
//+2 Commerce Index per level
//Required for various orbital construction projects
    /* enum */ ORBITAL_CONSTRUCTION(TechnologyCategory.SCIENCE, 200000, () -> ROCKETRY, 7, f -> f.OrbitialConstuction),
//Space Colonization
//Initial Cost: 10,000,000 tech points
//Requirements: Orbital Construction Level 4
//+2 Commerce Index per level
//Required for various orbital construction projects
//Will unlock !?!?!??!??! in the future
    /* enum */ SPACE_COLONIZATION(TechnologyCategory.SCIENCE, 10000000, () -> ORBITAL_CONSTRUCTION, 4, f -> f.SpaceColonoization),
//Precursor Technology
//Initial Cost: 100,000 tech points
//Requirements: Scientific Theory Level 15
//+10% precursor building output per level
//+1 To most indexes per level
    /* enum */ PRECURSOR_TECHNOLOGY(TechnologyCategory.SCIENCE, 100000, () -> SCIENTIFIC_THEORY, 15, f -> f.PrecursorTech),
//Required to use various precursor facilities
//Economy Technologies
//Commercial Technology
//Initial Cost: 300 tech points
//Requirements: Scientific Theory Level 2
//+3 commerce index per level
//+5% commercial efficiency per level
    /* enum */ COMMERCIAL_TECHNOLOGY(TechnologyCategory.ECONOMY, 300, () -> SCIENTIFIC_THEORY, 2, f -> f.Commerce),
//Mining Technology
//Initial Cost: 300 tech points
//Requirements: Scientific Theory Level 3
//+5% mining output per level
    /* enum */ MINING_TECHNOLOGY(TechnologyCategory.ECONOMY, 300, () -> SCIENTIFIC_THEORY, 3, f -> f.Mining),
//Factory Technology
//Initial Cost: 300 tech points
//Requirements: Scientific Theory Level 5
//+5% factory output per level
    /* enum */ FACTORY_TECHNOLOGY(TechnologyCategory.ECONOMY, 300, () -> SCIENTIFIC_THEORY, 5, f -> f.Factory),
//Fuel Extraction Technology
//Initial Cost: 300 tech points
//Requirements: Scientific Theory Level 6
//+5% fuel output output per level
    /* enum */ FUEL_EXTRACTION_TECHNOLOGY(TechnologyCategory.ECONOMY, 300, () -> SCIENTIFIC_THEORY, 6, f -> f.FuelExtraction),
//Energy Technology
//Initial Cost: 500 tech points
//Requirements: Scientific Theory Level 7
//+3 power Index per level
//+3% energy efficiency per level
    /* enum */ ENERGY_TECHNOLOGY(TechnologyCategory.ECONOMY, 500, () -> SCIENTIFIC_THEORY, 7, f -> f.Energy),
//Transportation Technology
//Initial Cost: 2,000 tech points
//Requirements: Scientific Theory Level 4
//+3 transportation Index per level
//+3% energy transportation per level
    /* enum */ TRANSPORTATION_TECHNOLOGY(TechnologyCategory.ECONOMY, 2000, () -> SCIENTIFIC_THEORY, 4, f -> f.Transportation),
//Civil Engineering
//Initial Cost: 2,000 tech points
//Requirements: Scientific Theory Level 4
//Reduces Development cost by 2% per level
//Reduces Building damage by 2% per level
//+2 transportation Index per level
//+2 power Index per level
    /* enum */ CIVIL_ENGINEERING(TechnologyCategory.ECONOMY, 2000, () -> SCIENTIFIC_THEORY, 4, f -> f.CivilEngineering),
//Land Reclamation
//Initial Cost: 10,000 tech points
//Requirements: Civil Engineering Level 5
//Reduces land cost by 2% per level
//+1% building slots per level
    /* enum */ LAND_RECLAMATION(TechnologyCategory.ECONOMY, 10000, () -> CIVIL_ENGINEERING, 5, f -> f.LandReclaimation),
//Skyscraper Technology
//Initial Cost: 50,000 tech points
//Requirements: Civil Engineering Level 6
//+2% building slots per level
    /* enum */ SKYSCRAPER_TECHNOLOGY(TechnologyCategory.ECONOMY, 50000, () -> CIVIL_ENGINEERING, 6, f -> f.SkyscraperDevelopment),
//Robotics
//Initial Cost: 5,000 tech points
//Requirements: Artificial Intelligence Level 2
//+1 employment index per level
//+1 max employment index per level
//+1% income per level
    /* enum */ ROBOTICS(TechnologyCategory.ECONOMY, 5000, () -> ARTIFICIAL_INTELLIGENCE, 2, f -> f.Robotics),
//Urban Planning
//Initial Cost: 10,000 tech points
//Requirements: Energy Technology Level 8
//+2000 population per residential district per level
//Increases population (5 x level of urban planning per development)
//+1 Commerce Index per level
    /* enum */ URBAN_PLANNING(TechnologyCategory.ECONOMY, 10000, () -> ENERGY_TECHNOLOGY, 8, f -> f.UrbanPlanning),
//Renewable Energy
//Initial Cost: 2,000 tech points
//Requirements: Civil Engineering Level 4
//+5% renewable power source output per level
//+2 power Index per level
//Unlocks wind power plants at level 3
//Unlocks solar power plants at level 7
    /* enum */ RENEWABLE_ENERGY(TechnologyCategory.ECONOMY, 2000, () -> CIVIL_ENGINEERING, 4, f -> f.RenewableEnergy),
//Rare Metal Mining
//Initial Cost: 100,000 tech points
//Requirements: Mining Technology Level 12
//+5% rare metal mine output per level
//+5% rare uranium mine output per level
//Unlocks rare metal mines at level 1
    /* enum */ RARE_METAL_MINING(TechnologyCategory.ECONOMY, 100000, () -> MINING_TECHNOLOGY, 12, f -> f.RareMetals),
//Military Technologies
//Military Organization
//Initial Cost: 1,000 tech points
//Requirements: None
//+3% military capacity per level
    /* enum */ MILITARY_ORGANIZATION(TechnologyCategory.MILITARY, 1000, f -> f.MilitaryOrganization),
//Infantry Equipment
//Initial Cost: 300 tech points
//Requirements: Military Organization Level 1
//Increases the quality of Infantry and support Vehicles
//Increases the combat effectiveness of Infantry and support Vehicles
    /* enum */ INFANTRY_EQUIPMENT(TechnologyCategory.MILITARY, 300, () -> MILITARY_ORGANIZATION, 1, f -> f.InfantryEquipment),
//Ordnance Development
//Initial Cost: 1,000 tech points
//Requirements: Military Organization Level 3
//Increases the quality of Battleships with every other level
//Increases the damage output of all military units by 5% per level
    /* enum */ ORDNANCE_DEVELOPMENT(TechnologyCategory.MILITARY, 1000, () -> MILITARY_ORGANIZATION, 3, f -> f.OrdnanceDevolopment),
//Stealth Technology
//Initial Cost: 50,000 tech points
//Requirements: Military Organization Level 14
//Increases the quality of stealth fighters, stealth bombers, and submarines with every other level
//Increases the combat effectiveness of stealth fighters and bombers by 5% per level
//Reduces hostile base war intel by 2% per level
//Effects how much information other nations can see when looking at your nation
    /* enum */ STEALTH_TECHNOLOGY(TechnologyCategory.MILITARY, 50000, () -> MILITARY_ORGANIZATION, 14, f -> f.StealthTechnology),
//Naval Technology
//Initial Cost: 2,500 tech points
//Requirements: Military Organization Level 7
//Increases the quality of all ships with every other level
//Increases the combat effectiveness of all ships by 5% per level
    /* enum */ NAVAL_TECHNOLOGY(TechnologyCategory.MILITARY, 2500, () -> MILITARY_ORGANIZATION, 7, f -> f.NavalTechnology),
//Armor Improvement
//Initial Cost: 2,500 tech points
//Requirements: Military Organization Level 4
//Increases the quality of medium tank, heavy tanks, and heavy mechs with every other level
//Increases the health of all military units by 5% per level
    /* enum */ ARMOR_IMPROVEMENT(TechnologyCategory.MILITARY, 2500, () -> MILITARY_ORGANIZATION, 4, f -> f.ArmourImprovment),
//Tank Technology
//Initial Cost: 5,000 tech points
//Requirements: Military Organization Level 5
//Increases the quality of tanks with every other level
//Increases the combat effectiveness of tanks by 5% per level
    /* enum */ TANK_TECHNOLOGY(TechnologyCategory.MILITARY, 5000, () -> MILITARY_ORGANIZATION, 5, f -> f.TankTechnology),
//Mech Development
//Initial Cost: 5,000 tech points
//Requirements: Military Organization Level 11
//Increases the quality of mechs with every other level
//Increases the combat effectiveness of mechs by 5% per level
    /* enum */ MECH_DEVELOPMENT(TechnologyCategory.MILITARY, 5000, () -> MILITARY_ORGANIZATION, 11, f -> f.MechDevolopment),
//Sensor Technology
//Initial Cost: 10,000 tech points
//Requirements: Military Organization Level 8
//Increases the quality of destroyers with every other level
//Increases base war intel by 1% per level
//Effects how much information you can see when looking at other nations
    /* enum */ SENSOR_TECHNOLOGY(TechnologyCategory.MILITARY, 10000, () -> MILITARY_ORGANIZATION, 8, f -> f.SensorTechnology),
//Aerospace Development
//Initial Cost: 10,000 tech points
//Requirements: Military Organization Level 9
//Increases the quality of air units and carriers with every other level
//Increases the combat effectiveness of air units by 5% per level
    /* enum */ AEROSPACE_DEVELOPMENT(TechnologyCategory.MILITARY, 10000, () -> MILITARY_ORGANIZATION, 9, f -> f.AerospaceDevelopment),
//Cyber Defense
//Initial Cost: 100,000
//Requirements: Computer level 10
//Decreases chance of enemy cyber attacks succeeding
//Adds 2 stability index per level
    /* enum */ CYBER_DEFENSE(TechnologyCategory.MILITARY, 100000, () -> COMPUTER_TECHNOLOGY, 10, f -> f.CyberDefense),
//Electronic Warfare
//Initial Cost: 100,000
//Requirements: Computer level 11
//Increases chance of success when performing cyber operations
//Unlocks new cyber operations
//Increases the effect of the influence news cyber action
    /* enum */ ELECTRONIC_WARFARE(TechnologyCategory.MILITARY, 100000, () -> COMPUTER_TECHNOLOGY, 11, f -> f.ElectronicWarfare),
    ;

    public static Technology[] values = values();

    private final int initialCost;
    private final Supplier<Technology> requiredTech;
    private final int requiredTechLevel;
    private final TechnologyCategory category;
    private final Function<AllianceTech, Integer> get;

    Technology(TechnologyCategory category, int initialCost, Function<AllianceTech, Integer> get) {
        this(category, initialCost, null, 0, get);
    }

    Technology(TechnologyCategory category, int initialCost, Supplier<Technology> requiredTech, int requiredTechLevel, Function<AllianceTech, Integer> get) {
        this.category = category;
        this.initialCost = initialCost;
        this.requiredTech = requiredTech == null ? () -> null : requiredTech;
        this.requiredTechLevel = requiredTechLevel;
        this.get = get;
    }

    public void apply(NationModifier modifier, int level) {

    }

    public final Technology getRequiredTech() {
        return requiredTech.get();
    }

    public final int getRequiredTechLevel() {
        return requiredTechLevel;
    }

    public TechnologyCategory getCategory() {
        return category;
    }

    @Command(desc = "Get the initial cost of the technology")
    public int getInitialCost() {
        return initialCost;
    }

    @Command(desc = "Get the name of the technology")
    public String getName() {
        return name();
    }

    @Command(desc = "Calculate the base tech cost based on acquired technologies, scientific theory level, and artificial intelligence level")
    public int getBaseTechCost(int numberOfAquiredTechnologies, int sciTheoryLevel, int artificialIntelligence) {
        return 100 * numberOfAquiredTechnologies - (sciTheoryLevel + artificialIntelligence) * 200;
    }

    @Command(desc = "Calculate the cost of the technology based on tech cost reduction, base tech cost, and tech level")
    public int getCost(int techCostReduction, int baseTechCost, int techLevel) {
        return techCostReduction * (baseTechCost + initialCost * (int) Math.pow(1.5, techLevel));
    }

    public int get(AllianceTech technology) {
        return get.apply(technology);
    }
}

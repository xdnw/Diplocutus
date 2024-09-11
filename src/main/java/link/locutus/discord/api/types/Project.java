package link.locutus.discord.api.types;

import link.locutus.discord.api.generated.NationProjects;
import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.commands.manager.v2.binding.annotation.Command;

import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public enum Project {
// National Highway System
//Cost: $180 Million, 50,000 Minerals
//Requirements: Transportation Technology Level 6
//Increases your transportation index by 10
//Increases your commerce index by 5
//Increases road output by 25%
    NATIONAL_HIGHWAY_SYSTEM(ProjectCategory.TRANSPORTATION, Map.of(ResourceType.CASH, 180_000_000.0, ResourceType.MINERALS, 50_000.0), f -> f.NationalHighwaySystem),
//Capital Subway System
//Cost: $360 Million, 250,000 Minerals
//Requirements: Transportation Technology Level 2
//Increases your transportation index by 9
//Unlocks building subways
    CAPITAL_SUBWAY_SYSTEM(ProjectCategory.TRANSPORTATION, Map.of(ResourceType.CASH, 360_000_000.0, ResourceType.MINERALS, 250_000.0), f -> f.CapitalSubwaySystem),
//Civil Engineering College
//Cost: $3 Billion, 1,000,000 Tech
//Requirements: Civil Engineering Level 8
//Increases your transportation index by 3
//Increases your commerce index by 3
//Reduces development cost by 5%
    CIVIL_ENGINEERING_COLLEGE(ProjectCategory.TRANSPORTATION, Map.of(ResourceType.CASH, 3_000_000_000.0, ResourceType.TECHNOLOGY, 1_000_000.0), f -> f.CivilEngineeringCollage),
//High Speed Rail Network
//Cost: $5 Billion, 1,000,000 Tech, 500,000 Minerals
//Requirements: Transportation Technology Level 8
//Increases your transportation index by 14
//Increases your commerce index by 6
//Increases rail output by 25%
//Increases the cost of rail networks by 25%
    HIGH_SPEED_RAIL_NETWORK(ProjectCategory.TRANSPORTATION, Map.of(ResourceType.CASH, 5_000_000_000.0, ResourceType.TECHNOLOGY, 1_000_000.0, ResourceType.MINERALS, 500_000.0), f -> f.HighSpeedRailNetwork),
//Hyperloop Network
//Cost: $30 Billion, 2,000,000 Tech, 3,000,000 Minerals
//Requirements: Transportation Technology Level 10
//Increases your transportation index by 4% of your tech index
//Increases rail output by 35%
//Increases the cost of rail networks by 35%
    HYPERLOOP_NETWORK(ProjectCategory.TRANSPORTATION, Map.of(ResourceType.CASH, 30_000_000_000.0, ResourceType.TECHNOLOGY, 2_000_000.0, ResourceType.MINERALS, 3_000_000.0), f -> f.HyperloopNetwork),
//Power Projects
//Major Hydro Dam
//Cost: $180 Million, 50,000 Minerals
//Requirements: Energy Technology Level 8
//Increases your power index by 8
    MAJOR_HYDRO_DAM(ProjectCategory.POWER, Map.of(ResourceType.CASH, 180_000_000.0, ResourceType.MINERALS, 50_000.0), f -> f.MajorHydroDam),
//Green Energy Grid
//Cost: $1.2 Billion, 400,000 Tech, 200,000 Minerals
//Requirements: Energy Technology Level 11
//Increases your power index by 3
//Also increases your power index by 3%
//Increases your commerce index by 3
    GREEN_ENERGY_GRID(ProjectCategory.POWER, Map.of(ResourceType.CASH, 1_200_000_000.0, ResourceType.TECHNOLOGY, 400_000.0, ResourceType.MINERALS, 200_000.0), f -> f.GreenEnergyGrid),
//Energy Efficient Building Standards
//Cost: $2.4 Billion, 600,000 Tech, 200,000 Minerals
//Requirements: Energy Technology Level 12
//Increases your power index by 10
//Increases your commerce index by 5
//Reduces the impact of development on your power index by 5%
    ENERGY_EFFICIENT_BUILDING_STANDARDS(ProjectCategory.POWER, Map.of(ResourceType.CASH, 2_400_000_000.0, ResourceType.TECHNOLOGY, 600_000.0, ResourceType.MINERALS, 200_000.0), f -> f.EnergyEfficentBuildingStandards),
//Power Storage Dam Network
//Cost: $5 Billion, 600,000 Tech, 300,000 Minerals
//Requirements: Renewable Energy Level 5
//Increases your power index by 5
//Increases the power provided by renewable energy sources by 50%
POWER_STORAGE_DAM_NETWORK(ProjectCategory.POWER, Map.of(ResourceType.CASH, 5_000_000_000.0, ResourceType.TECHNOLOGY, 600_000.0, ResourceType.MINERALS, 300_000.0), f -> f.PowerStorageDamNetwork),
//Generation III Reactors
//Cost: $15 Billion, 1,000,000 Tech, 500,000 Minerals
//Requirements: Nuclear Technology Level 10
//Increases your power index by 14
//Increases nuclear power output by 25%
GENERATION_III_REACTORS(ProjectCategory.POWER, Map.of(ResourceType.CASH, 15_000_000_000.0, ResourceType.TECHNOLOGY, 1_000_000.0, ResourceType.MINERALS, 500_000.0), f -> f.ExperimentalFusionReactor),
//Precursor Reactor Activation
//Cost: $30 Billion, 5,000,000 Tech, 2,500,000 Minerals
//Requirements: Precursor Technology Level 6
//Increases your power index by 4% of your tech index
//Allows Precursor Reactors to be activated
PRECURSOR_REACTOR_ACTIVATION(ProjectCategory.POWER, Map.of(ResourceType.CASH, 30_000_000_000.0, ResourceType.TECHNOLOGY, 5_000_000.0, ResourceType.MINERALS, 2_500_000.0), f -> f.ExperimentalPrecursorReactor),
//Education Projects
//Capital University
//Cost: $360 Million, 100,000 Tech, 25,000 Minerals
//Requirements: Education Technology Level 6
//Increases your education index by 4
//Reduces tech cost by 3%
//Reduces the cost of universities by 25%
CAPITAL_UNIVERSITY(ProjectCategory.EDUCATION, Map.of(ResourceType.CASH, 360_000_000.0, ResourceType.TECHNOLOGY, 100_000.0, ResourceType.MINERALS, 25_000.0), f -> f.CapitalUniversity),
//National Education System
//Cost: $1.5 Billion, 100,000 Tech, 100,000 Minerals
//Requirements: Education Technology Level 8
//Increases your education index by 6
//Also increases your education index by 3%
//Increases your tech production by 3%
NATIONAL_EDUCATION_SYSTEM(ProjectCategory.EDUCATION, Map.of(ResourceType.CASH, 1_500_000_000.0, ResourceType.TECHNOLOGY, 100_000.0, ResourceType.MINERALS, 100_000.0), f -> f.NationalEducationSystem) {
    @Override
    public void apply(NationModifier modifier, int level) {
        modifier.TECH_OUTPUT += 3 * level;
    }
},
//Virtual Reality Learning Center
//Cost: $12 Billion, 1,500,000 Tech, 1,500,000 Minerals
//Requirements: Entertainment Technology Level 10 and Virtual Reality Level 3
//Increases your education index by 14
//Increases your commerce index by 6
//Unlocks the VR Propaganda Directive
VIRTUAL_REALITY_LEARNING_CENTER(ProjectCategory.EDUCATION, Map.of(ResourceType.CASH, 12_000_000_000.0, ResourceType.TECHNOLOGY, 1_500_000.0, ResourceType.MINERALS, 1_500_000.0), f -> f.VirtualRealityLearningCenter),
//Orbital Research Station
//Cost: $120 Billion, 20,000,000 Tech, 1,000,000 Minerals
//Requirements: Orbital Construction Level 3
//Increases your education index by 20
//Increases your tech production by 10,000
//Also increases your tech production by 10%
//Reduces tech cost by 5%
ORBITAL_RESEARCH_STATION(ProjectCategory.EDUCATION, Map.of(ResourceType.CASH, 120_000_000_000.0, ResourceType.TECHNOLOGY, 20_000_000.0, ResourceType.MINERALS, 1_000_000.0), f -> f.OribitalResearchStation) {
    @Override
    public void apply(NationModifier modifier, int level) {
        modifier.TECH_OUTPUT += 10_000 * level;
        modifier.TECH_OUTPUT_PERCENT += 10 * level;
    }
},
//Commerce Projects
//Sports Stadium
//Cost: $320 Million, 100,000 Minerals
//Requirements: Entertainment Technology Level 4
//Increases your education index by 3
//Increases your stability index by 6
//Also increases your stability index by 2%
SPORTS_STADIUM(ProjectCategory.COMMERCE, Map.of(ResourceType.CASH, 320_000_000.0, ResourceType.MINERALS, 100_000.0), f -> f.SportsStadium),
//Deepwater Port
//Cost: $1.2 Billion, 250,000 Minerals
//Requirements: Commerce Level 13 and Transportation Level 3
//Increases your education index by 3
//Increases your transportation index by 6
//Also increases your transportation index by 3%
//Reduces the cost of ports by 25%
DEEPWATER_PORT(ProjectCategory.COMMERCE, Map.of(ResourceType.CASH, 1_200_000_000.0, ResourceType.MINERALS, 250_000.0), f -> f.DeepwaterPort),
//International Airport
//Cost: $900 Million, 300,000 Tech 150,000 Minerals
//Requirements: Commerce Level 12 and Transportation Level 4
//Increases your education index by 4
//Increases your transportation index by 5
//Also increases your transportation index by 2%
//Reduces the cost of airports by 25%
INTERNATIONAL_AIRPORT(ProjectCategory.COMMERCE, Map.of(ResourceType.CASH, 900_000_000.0, ResourceType.TECHNOLOGY, 300_000.0, ResourceType.MINERALS, 150_000.0), f -> f.InternationalAirport),
//National Park
//Cost: $1.2 Billion, 200,000 Tech, 100,000 Minerals
//Requirements: Entertainment Technology Level 5
//Increases your education index by 6
//Increases your stability index by 6
//Enables the Expand Nation Parks immediate action
NATIONAL_PARK(ProjectCategory.COMMERCE, Map.of(ResourceType.CASH, 1_200_000_000.0, ResourceType.TECHNOLOGY, 200_000.0, ResourceType.MINERALS, 100_000.0), f -> f.NationalPark),
//Orbital Positioning Satellite
//Cost: $30 Billion, 750,000 Tech
//Requirements: Orbital Construction Level 4
//Increases your commerce index by 15
//Airport Output(+20%)
ORBITAL_POSITIONING_SATELLITE(ProjectCategory.COMMERCE, Map.of(ResourceType.CASH, 30_000_000_000.0, ResourceType.TECHNOLOGY, 750_000.0), f -> f.OrbitalPositioningSatellite),
//Orbital Communication Satellite
//Cost: $60 Billion, 3,000,000 Tech
//Requirements: Orbital Construction Level 9
//Increases your commerce index by 25
//Commerce District Output(+10%)
ORBITAL_COMMUNICATION_SATELLITE(ProjectCategory.COMMERCE, Map.of(ResourceType.CASH, 60_000_000_000.0, ResourceType.TECHNOLOGY, 3_000_000.0), f -> f.OrbitalCommunicationSatellite),
//Orbital Internet Satellite Network
//Cost: $250 Billion, 12,000,000 Tech
//Requirements: Orbital Construction Level 14
//Increases your commerce index by 30
//Increases your commerce index by 7%
ORBITAL_INTERNET_SATELLITE_NETWORK(ProjectCategory.COMMERCE, Map.of(ResourceType.CASH, 250_000_000_000.0, ResourceType.TECHNOLOGY, 12_000_000.0), f -> f.OrbitalInternetSatelliteNetwork),
//Space Projects
//National Observatory
//Cost: $3 Billion, 200,000 Tech, 100,000 Minerals
//Requirements: Scientific Theory Level 9
//Increases your education index by 5
//Increases your tech production by 3,000
//Will unlock ??????? in the future
NATIONAL_OBSERVATORY(ProjectCategory.SPACE, Map.of(ResourceType.CASH, 3_000_000_000.0, ResourceType.TECHNOLOGY, 200_000.0, ResourceType.MINERALS, 100_000.0), f -> f.NationalObservatory) {
    @Override
    public void apply(NationModifier modifier, int level) {
        modifier.TECH_OUTPUT += 3_000 * level;
    }
},
//National Space Agency
//Cost: $9 Billion, 600,000 Tech, 300,000 Minerals
//Requirements: Space Exploration Level 1
//Increases your education index by 10
//Increases your tech production by 3,000
//Will unlock ??????? in the future
NATIONAL_SPACE_AGENCY(ProjectCategory.SPACE, Map.of(ResourceType.CASH, 9_000_000_000.0, ResourceType.TECHNOLOGY, 600_000.0, ResourceType.MINERALS, 300_000.0), f -> f.SpaceResearchCenter) {
    @Override
    public void apply(NationModifier modifier, int level) {
        modifier.TECH_OUTPUT += 3_000 * level;
    }
},
//Orbital Telescope
//Cost: $50 Billion, 2,000,000 Tech, 1,000,000 Minerals
//Requirements: Sensor Technology Level 16
//Increases your education index by 15
//Will double ??????? in the future
ORBITAL_TELESCOPE(ProjectCategory.SPACE, Map.of(ResourceType.CASH, 50_000_000_000.0, ResourceType.TECHNOLOGY, 2_000_000.0, ResourceType.MINERALS, 1_000_000.0), f -> f.OrbitialTelescope),
//Orbital Sensor Array
//Cost: $35 Billion, 5,000,000 Tech, 300,000 Minerals
//Requirements: Sensor Technology Level 16
//Increases your stability index by 15
//Will unlock ??????? in the future
//Will also unlock ??????? in the future
ORBITAL_SENSOR_ARRAY(ProjectCategory.SPACE, Map.of(ResourceType.CASH, 35_000_000_000.0, ResourceType.TECHNOLOGY, 5_000_000.0, ResourceType.MINERALS, 300_000.0), f -> f.OrbitialSensorArray),
//Experimental Fuel Lab
//Cost: $35 Billion, 5,000,000 Tech, 500,000 Minerals
//Requirements: Scientific Theory Level 25
//Increases your education index by 15
//Increases your fuel output by 5%
//Will reduce ??????? in the future
EXPERIMENTAL_FUEL_LAB(ProjectCategory.SPACE, Map.of(ResourceType.CASH, 35_000_000_000.0, ResourceType.TECHNOLOGY, 5_000_000.0, ResourceType.MINERALS, 500_000.0), f -> f.ExperimentalFuelDevelopmentLab),
//Advanced Rocket Fuels
//Cost: $150 Billion, 10,000,000 Tech, 5,000,000 Minerals
//Requirements: Rocketry Level 16
//Increases your transportation index by 15
//Will reduce ??????? in the future
ADVANCED_ROCKET_FUELS(ProjectCategory.SPACE, Map.of(ResourceType.CASH, 150_000_000_000.0, ResourceType.TECHNOLOGY, 10_000_000.0, ResourceType.MINERALS, 5_000_000.0), f -> f.AdvancedRocketFuels),
//Reusable Rocket Program
//Cost: $1 Trillion, 100,000,000 Tech, 100,000,000 Minerals
//Requirements: Rocketry Level 21
//Increases your transportation index by 25
//Will reduce ??????? in the future
REUSABLE_ROCKET_PROGRAM(ProjectCategory.SPACE, Map.of(ResourceType.CASH, 1_000_000_000_000.0, ResourceType.TECHNOLOGY, 100_000_000.0, ResourceType.MINERALS, 100_000_000.0), f -> f.ReuseableRocketProgram),
//Military Projects
//Army Engineer Corps
//Cost: $2 Billion, 750,000 Tech
//Requirements: Civil Engineering Level 7
//Increases your transportation index by 3
//Increases your transportation index by 3%
//Reduces land cost by 5%
//Increases army mobility by 5%
ARMY_ENGINEER_CORPS(ProjectCategory.MILITARY, Map.of(ResourceType.CASH, 2_000_000_000.0, ResourceType.TECHNOLOGY, 750_000.0), f -> f.ArmyEngineerCorps),
//Border Fortifications
//Cost: $5 Billion, 1,000,000 Minerals
//Requirements: None
//Increases entrenchment by 5 at the start of a war
//Unlocks border trade policy
BORDER_FORTIFICATIONS(ProjectCategory.MILITARY, Map.of(ResourceType.CASH, 5_000_000_000.0, ResourceType.MINERALS, 1_000_000.0), f -> f.BorderFortifications),
//Cruise Missile Program
//Cost: $25 Billion, 2,000,000 Tech, 5,000,000 Minerals
//Requirements: Rocketry Technology Level 10
//Unlocks building and using cruise missiles
CRUISE_MISSILE_PROGRAM(ProjectCategory.MILITARY, Map.of(ResourceType.CASH, 25_000_000_000.0, ResourceType.TECHNOLOGY, 2_000_000.0, ResourceType.MINERALS, 5_000_000.0), f -> f.CruiseMissileProgram),
//Missile Defense System
//Cost: $35 Billion, 2,000,000 Tech, 1,000,000 Minerals
//Requirements: Rocketry Technology Level 14
//Unlocks the ability to intercept cruise missiles and nuclear missiles
MISSILE_DEFENSE_SYSTEM(ProjectCategory.MILITARY, Map.of(ResourceType.CASH, 35_000_000_000.0, ResourceType.TECHNOLOGY, 2_000_000.0, ResourceType.MINERALS, 1_000_000.0), f -> f.MissileDefenseSystems),
//Stealth Frame Development
//Cost: $25 Billion, 2,500,000 Tech
//Requirements: Stealth Technology Level 10
//Unlocks the ability to build stealth aircraft
STEALTH_FRAME_DEVELOPMENT(ProjectCategory.MILITARY, Map.of(ResourceType.CASH, 25_000_000_000.0, ResourceType.TECHNOLOGY, 2_500_000.0), f -> f.StealthFrameDevelopmentProject),
//Precursor Mech Experimental Replication
//Cost: $40 Billion, 5,000,000 Tech
//Requirements: Precursor Technology Level 14 and Armor Improvement Level 22
//Unlocks the ability to build Precursor Mechs
PRECURSOR_MECH_EXPERIMENTAL_REPLICATION(ProjectCategory.MILITARY, Map.of(ResourceType.CASH, 40_000_000_000.0, ResourceType.TECHNOLOGY, 5_000_000.0), f -> f.PrecursorMechExperimentalReplication),
//Espionage Projects
//Experimental Stealth Field
//Cost: $10 Billion, 750,000 Tech, 500,000 Minerals
//Requirements: Stealth Technology Level 10
//Reduces hostile intel against your nation(+1 stealth)
//Lowers enemy base war intel against your nation by 5%
EXPERIMENTAL_STEALTH_FIELD(ProjectCategory.ESPIONAGE, Map.of(ResourceType.CASH, 10_000_000_000.0, ResourceType.TECHNOLOGY, 750_000.0, ResourceType.MINERALS, 500_000.0), f -> f.ExperimentalStealthField),
//Spy Satellite
//Cost: $20 Billion, 750,000 Tech, 300,000 Minerals
//Requirements: Sensor Technology Level 14
//Increases how much information you can see when looking at other nations(+1 sensors)
//Increases your base war intel against other nations by 5%
SPY_SATELLITE(ProjectCategory.ESPIONAGE, Map.of(ResourceType.CASH, 20_000_000_000.0, ResourceType.TECHNOLOGY, 750_000.0, ResourceType.MINERALS, 300_000.0), f -> f.SpySatellite),
//Intelligence Agency
//Cost: $10 Billion, 300,000 Minerals
//Requirements: Espionage Technology Level 22
//Increases the chance of success when performing espionage operations(+1 Espionage Attack Strength)
//Unlocks the ability to use the direct operations immediate action
//Unlocks the ability to set an espionage budget
INTELLIGENCE_AGENCY(ProjectCategory.ESPIONAGE, Map.of(ResourceType.CASH, 10_000_000_000.0, ResourceType.MINERALS, 300_000.0), f -> f.IntelligenceAgency),
//National Security Agency
//Cost: $25 Billion, 2,500,000 Minerals
//Requirements: Counter Intelligence Level 23
//Decreases the chance of success when you are the target of espionage operations(+1 Espionage Defense Strength)
//Unlocks the ability to set a national security budget
//Increases your stability index by 20
NATIONAL_SECURITY_AGENCY(ProjectCategory.ESPIONAGE, Map.of(ResourceType.CASH, 25_000_000_000.0, ResourceType.MINERALS, 2_500_000.0), f -> f.NationalSecurityAgency),
//Miscellaneous Projects
//College of Geology
//Cost: $7.5 Billion, 500,000 Tech, 1,000,000 Minerals
//Requirements: Mining Technology Level 21
//Increases your education index by 10
//Increases mineral output by 3%
//Increases Geological Survey Gains by 100%
COLLEGE_OF_GEOLOGY(ProjectCategory.MISCELLANEOUS, Map.of(ResourceType.CASH, 7_500_000_000.0, ResourceType.TECHNOLOGY, 500_000.0, ResourceType.MINERALS, 1_000_000.0), f -> f.CollegeOfGeology),
//College of Archaeology
//Cost: $7.5 Billion, 1,500,000 Tech4
//Requirements: Precursor Technology Level 7
//Increases your education index by 15
//Increases Archaeology Survey Gains by 100%
COLLEGE_OF_ARCHAEOLOGY(ProjectCategory.MISCELLANEOUS, Map.of(ResourceType.CASH, 7_500_000_000.0, ResourceType.TECHNOLOGY, 1_500_000.0), f -> f.CollegeOfArchaeology),
//Department of Geology
//Cost: $25 Billion, 5,000,000 Minerals
//Requirements: Mining Technology Level 27
//Increases budget points by 1
//Increases mineral output by 5%
//Reduces Geological Survey Cost by 50%
DEPARTMENT_OF_GEOLOGY(ProjectCategory.MISCELLANEOUS, Map.of(ResourceType.CASH, 25_000_000_000.0, ResourceType.MINERALS, 5_000_000.0), f -> f.DepartmentOfGeology),
//Department of Archaeology
//Cost: $25 Billion, 5,000,000 Tech
//Requirements: Precursor Technology Level 10
//Increases budget points by 1
//Reduces Archaeology Survey Cost by 50%
DEPARTMENT_OF_ARCHAEOLOGY(ProjectCategory.MISCELLANEOUS, Map.of(ResourceType.CASH, 25_000_000_000.0, ResourceType.TECHNOLOGY, 5_000_000.0), f -> f.DepartmentOfArchaeology),
//National Investments
//National Investments are unique projects that you can invest in multiple times. The base cost for each investment is based on your development level. Additionally, the base cost is increased by level^2 for each investment you have made of that type previously.
//
//Transportation Investment
//Cost: 4000 * (Development + Land/20) * (1+Project Level^2)
//Increases your transportation index by 4 per level
//Increases your transportation index by 1% per level
    TRANSPORTATION_INVESTMENT(ProjectCategory.NATIONAL_INVESTMENT, Map.of(ResourceType.CASH, 4000.0, ResourceType.MINERALS, 1.0), f -> f.TransportationInvestment),
//Power Investment
//Cost: 4000 * (Development + Land/20) * (1+Project Level^2)
//Increases your transportation index by 5 per level
//Increases your transportation index by 1% per level
    POWER_INVESTMENT(ProjectCategory.NATIONAL_INVESTMENT, Map.of(ResourceType.CASH, 4000.0, ResourceType.MINERALS, 1.0), f -> f.PowerInvestment),
//Education Investment
//Cost: 4750 * (Development + Land/20) * (1+Project Level^2)
//Increases your transportation index by 4 per level
//Increases your transportation index by 1% per level
    EDUCATION_INVESTMENT(ProjectCategory.NATIONAL_INVESTMENT, Map.of(ResourceType.CASH, 4750.0, ResourceType.MINERALS, 1.0), f -> f.EducationInvestment),
//Commercial Investment
//Cost: 3500 * (Development + Land/20) * (1+Project Level^2)
//Increases your commercial index by 5 per level
//Increases your commercial index by 1% per level
    COMMERCIAL_INVESTMENT(ProjectCategory.NATIONAL_INVESTMENT, Map.of(ResourceType.CASH, 3500.0, ResourceType.MINERALS, 1.0), f -> f.CommercialInvestment),
//Military Investment
//Cost: 5400 * (Development + Land/20) * (1+Project Level^2)
//Increases your military capacity by 1%
    MILITARY_INVESTMENT(ProjectCategory.NATIONAL_INVESTMENT, Map.of(ResourceType.CASH, 5400.0, ResourceType.MINERALS, 1.0), f -> f.MilitaryInvestment),
//City Investment
//Cost: 2700 * (Development + Land/20) * (1+Project Level^2)
//Increases your building slots by 10
//Increases your building slots by 0.5%
    CITY_INVESTMENT(ProjectCategory.NATIONAL_INVESTMENT, Map.of(ResourceType.CASH, 2700.0, ResourceType.MINERALS, 1.0), f -> f.CityInvestment),
//Residential Investment
//Cost: 1350* (Development + Land/20) * (1+Project Level^2)
//Increases your population by 200,000
//Increases your population by 1%
//Increases your commerce index by 2
    RESIDENTIAL_INVESTMENT(ProjectCategory.NATIONAL_INVESTMENT, Map.of(ResourceType.CASH, 1350.0, ResourceType.MINERALS, 1.0), f -> f.ResidentialInvestment),
//Technology Investment
//Cost: 4000* (Development + Land/20) * (1+Project Level^2)
//Increases your tech production by 300
//Increases your tech production by 1%
//Reduces tech cost by 1%
    TECHNOLOGY_INVESTMENT(ProjectCategory.NATIONAL_INVESTMENT, Map.of(ResourceType.CASH, 4000.0, ResourceType.MINERALS, 1.0), f -> f.TechnologyInvestment) {
    @Override
    public void apply(NationModifier modifier, int level) {
        modifier.TECH_OUTPUT += 300 * level;
        modifier.TECH_OUTPUT_PERCENT += 1d * level;
    }
},
//Healthcare Investment
//Cost: 4000* (Development + Land/20) * (1+Project Level^2)
//Increases your stability index by 5
//Increases your stability index by 1% per level
    HEALTHCARE_INVESTMENT(ProjectCategory.NATIONAL_INVESTMENT, Map.of(ResourceType.CASH, 4000.0, ResourceType.MINERALS, 1.0), f -> f.HealthcareInvestment),
    ;

    public static Project[] values = values();

    private final ProjectCategory category;
    private final Map<ResourceType, Double> baseCost;
    private final Function<NationProjects, Integer> get;

    Project(ProjectCategory category, Map<ResourceType, Double> baseCost, Function<NationProjects, Integer> get) {
        this.category = category;
        this.baseCost = baseCost;
        this.get = get;
    }

    @Command
    public Map<ResourceType, Double> getBaseCost() {
        return baseCost;
    }

    @Command
    public ProjectCategory getCategory() {
        return category;
    }

    public static Project parse(String s) {
        try {
            return valueOf(s.toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void apply(NationModifier modifier, int level) {

    }

    public int get(NationProjects projects) {
        return get.apply(projects);
    }
}

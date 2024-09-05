package link.locutus.discord.api.types;

import link.locutus.discord.api.generated.NationPolicyLastRan;
import link.locutus.discord.commands.manager.v2.binding.annotation.Command;

import java.util.Date;
import java.util.function.Function;

public enum Policy {
    TEMPORARY_TAX_INCREASE(f -> f.TemporaryTaxIncreaseLastRan),
    EMERGENCY_CONSCRIPTION(f -> f.EmergencyConsriptionLastRan),
    FUND_RESEARCH_PROJECT(f -> f.FundResearchProjectLastRan),
    REQUIRE_FACTORY_OVERTIME(f -> f.RequireFactoryOvertimeLastRan),
    REQUIRE_MINE_OVERTIME(f -> f.RequireMineOvertimeLastRan),
    PRIORITIZE_MAJOR_PROJECTS(f -> f.PrioritizeMajorProjectsLastRan),
    PRIORITIZE_NEW_DEVELOPMENT(f -> f.PrioritizeNewDevelopmentLastRan),
    PRIORITIZE_LAND_ACQUISITION(f -> f.PrioritizeLandAquisitionLastRan),
    PRIORITIZE_RECONSTRUCTION(f -> f.PrioritizeReconstructionLastRan),
    FUND_LAND_PURCHASE(f -> f.FundLandPurchaseLastRan),
    FUND_NEW_DEVELOPMENT(f -> f.FundNewDevelopmentLastRan),
    PURCHASE_FOREIGN_TECHNOLOGY(f -> f.PurchaseForeignTechnologyLastRan),
    IMPORT_MINERALS(f -> f.ImportMineralsLastRan),
    CONTRACT_FOREIGN_FACTORIES(f -> f.ContractForeignFactorysLastRan),
    IMPORT_URANIUM(f -> f.ImportUraniumLastRan),
    IMPORT_RARE_METALS(f -> f.ImportRareMetalsLastRan),
    ARCHAEOLOGY_SURVEY(f -> f.ArchaeologySurveyLastRan),
    GEOLOGICAL_SURVEY(f -> f.GeologicalSurveyLastRan),
    EXPEDITE_PROJECTS(f -> f.ExpediteProjectsLastRan),
    RENEGOTIATE_BUDGET(f -> f.RenegotiateBudgetLastRan),
    HOST_PRESS_CONFERENCE(f -> f.HostPressConferenceLastRan),
    BROADCAST_PROPAGANDA(f -> f.BroadcastPropagandaLastRan),
    LEASE_RESOURCE_AREA(f -> f.LeaseResourceAreaLastRan),
    DISMANTLE_PRECURSOR_SITE(f -> f.DismantlePrecusorSiteLastRan),
    FOREIGN_INVESTMENT_AGREEMENT(f -> f.ForeignInvestmentAgreementLastRan),
    IMPORT_FUEL(f -> f.ImportFuelLastRan),
    VR_PROPAGANDA(f -> f.VRPropagandaLastRan),
    EXPAND_NATION_PARKS(f -> f.ExpandNationParksLastRan),
    RESEARCH_BUDGET(f -> f.ResearchBudgetLastRan),
    EDUCATION_BUDGET(f -> f.EducationBudgetLastRan),
    TRANSPORTATION_BUDGET(f -> f.TransportationBudgetLastRan),
    POWER_BUDGET(f -> f.PowerBudgetLastRan),
    WELFARE_BUDGET(f -> f.WelfareBudgetLastRan),
    MILITARY_BUDGET(f -> f.MilitaryBudgetLastRan),
    BUSINESS_SUBSIDIZATION(f -> f.BuisnessSubsidizationLastRan),
    FUEL_EXTRACTION_SUBSIDIZATION(f -> f.FuelExtractionSubsidizationLastRan),
    MINING_SUBSIDIZATION(f -> f.MiningSubsidizationLastRan),
    FACTORY_SUBSIDIZATION(f -> f.FactorySubsidizationLastRan),
    RESEARCH_LAB_SUBSIDIZATION(f -> f.ResearchLabSubsidizationLastRan),
    INFRASTRUCTURE_BUDGET(f -> f.InfrastructureBudgetLastRan),
    LAND_RECLAMATION_BUDGET(f -> f.LandReclaimationBudgetLastRan),
    PROJECT_BUDGET(f -> f.ProjectBudgetLastRan),
    ESPIONAGE_BUDGET(f -> f.EspionageBudgetLastRan),
    DIRECT_OPERATIONS(f -> f.DirectOperationsLastRan),

    ;

    public static Policy[] values = values();
    private final Function<NationPolicyLastRan, Date> get;

    Policy(Function<NationPolicyLastRan, Date> get) {
        this.get = get;
    }

    @Command(desc = "Get the name of the policy")
    public String getName() {
        String name = this.name().toLowerCase();
        String[] parts = name.split("_");
        StringBuilder camelCaseName = new StringBuilder();
        for (String part : parts) {
            camelCaseName.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        return camelCaseName.toString();
    }

    // {\textstyle DailyPoliticalSupport={\Bigl (}{\frac {StabilityIndex}{10}}+BonusDailyPoliticalSupport{\Bigr )}*{\Bigl (}1+0.01*BonusPercentDailyPoliticalSupport{\Bigr )}}

    public static double getDailyPoliticalSupport(double stabilityIndex, double bonusDailyPoliticalSupport, double bonusPercentDailyPoliticalSupport) {
        return ((stabilityIndex / 10) + bonusDailyPoliticalSupport) * (1 + 0.01 * bonusPercentDailyPoliticalSupport);
    }

    public long get(NationPolicyLastRan policies) {
        Date date = get.apply(policies);
        return date == null ? 0 : date.getTime();
    }
}

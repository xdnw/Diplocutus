package link.locutus.discord.api.endpoints;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import link.locutus.discord.api.ApiKeyPool;
import link.locutus.discord.api.generated.*;
import link.locutus.discord.api.types.MilitaryUnit;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DnsApi {
    private final String BASE_URL = "https://diplomacyandstrifeapi.com/api/";

    private static final ObjectMapper MAPPER;
    static {
        MAPPER = Jackson2ObjectMapperBuilder.json()
                .featuresToEnable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .featuresToEnable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .build();
        MAPPER.setDateFormat(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));
        SimpleModule module = new SimpleModule();

        module.addDeserializer(Date.class, new DateDeserializer());

        module.addDeserializer(MilitaryUnit.class, new UpperCaseEnumDeserializer<>(MilitaryUnit.class));
        module.addDeserializer(BankType.class, new UpperCaseEnumDeserializer<>(BankType.class));
        module.addDeserializer(InterestType.class, new UpperCaseEnumDeserializer<>(InterestType.class));
        module.addDeserializer(InventoryType.class, new UpperCaseEnumDeserializer<>(InventoryType.class));
        module.addDeserializer(LoanStatus.class, new UpperCaseEnumDeserializer<>(LoanStatus.class));
        module.addDeserializer(LoanType.class, new UpperCaseEnumDeserializer<>(LoanType.class));
        module.addDeserializer(PaymentDurationType.class, new UpperCaseEnumDeserializer<>(PaymentDurationType.class));
        module.addDeserializer(ResourceType.class, new UpperCaseEnumDeserializer<>(ResourceType.class));
        module.addDeserializer(TreatyType.class, new UpperCaseEnumDeserializer<>(TreatyType.class));
        module.addDeserializer(WarType.class, new UpperCaseEnumDeserializer<>(WarType.class));

        MAPPER.registerModule(module);
    }

    private final ApiKeyPool apiKeyPool;

    public DnsApi(ApiKeyPool apiKeyPool) {
        this.apiKeyPool = apiKeyPool;
    }

    private <T> DnsQuery<T> query(Class<T> clazz, String endpoint, int cost) {
        return new DnsQuery<>(MAPPER, clazz, BASE_URL, endpoint, apiKeyPool, cost);
    }
    //This is a list of all public endpoints that you can use. These calls just require your API code and other information if the call requires it.
    //
    //Nation list
    //nation?APICode={API_CODE}
    //Info: Returns a list of nations with all data publicly available on the leaderboards. The only exception is that it will not return Tech Output.
    //API Cost: 10
    public DnsQuery<Nation> nation() {
        return query(Nation.class, "nation", 10).setPublic();
    }
    //Specific Nation Info
    //nation?APICode={API_CODE}&NationId={NATION_ID}
    //Info: Returns public data for a specific nation
    //API Cost: 1
    public DnsQuery<Nation> nation(int nationId) {
        return query(Nation.class, "nation", 1).add("NationId", nationId).setPublic();
    }
    //Specific Nation Projects
    //NationProjects?APICode={API_CODE}&NationId={NATION_ID}
    //Info: Returns project data for a specific nation
    //API Cost: 1
    public DnsQuery<NationProjects> nationProjects(int nationId) {
        return query(NationProjects.class, "NationProjects", 1).add("NationId", nationId).setPublic();
    }
    //Specific Nation Loan History
    //NationLoanHistory?APICode={API_CODE}&NationId={NATION_ID}
    //Info: Returns the complete loan history for a specific nation.
    //API Cost: 1
    public DnsQuery<LoanHistory> nationLoanHistory(int nationId) {
        return query(LoanHistory.class, "NationLoanHistory", 1).add("NationId", nationId).setPublic();
    }
    //Alliance list
    //alliance?APICode={API_CODE}
    //Info: Returns a list of alliances with all data publicly available on the leaderboards.
    //API Cost: 1
    public DnsQuery<Alliance> alliance() {
        return query(Alliance.class, "alliance", 1).setPublic();
    }
    //Specific Alliance Treaties
    //AllianceTreaties?APICode={API_CODE}&AllianceId={ALLIANCE_ID}
    //Info: Returns active treaties for an alliance
    //API Cost: 1
    public DnsQuery<AllianceTreaties> allianceTreaties(int allianceId) {
        return query(AllianceTreaties.class, "AllianceTreaties", 1).add("AllianceId", allianceId).setPublic();
    }

    // Private Queries //

    //Alliance Member Contribution
    //AllianceMemberContribution?APICode={API_CODE}
    //Info: Returns a list of members of your alliance and their contribution amounts
    //API Cost: 3(use Alliance Member Nation Contribution instead to pull a single nation for an api cost of 1)
    public DnsQuery<Contribution> allianceMemberContribution() {
        return query(Contribution.class, "AllianceMemberContribution", 3);
    }
    //Alliance Member Tech
    //AllianceTech?APICode={API_CODE}
    //Info: Returns a list of members of your alliance and their technology levels
    //API Cost: 1
    public DnsQuery<AllianceTech> allianceTech() {
        return query(AllianceTech.class, "AllianceTech", 1);
    }
    //Alliance Member Military
    //AllianceMilitary?APICode={API_CODE}
    //Info: Returns a list of members of your alliance and their current military numbers
    //API Cost: 1
    public DnsQuery<AllianceMilitary> allianceMilitary() {
        return query(AllianceMilitary.class, "AllianceMilitary", 1);
    }
    //Alliance Member Effects List
    //NationsEffectsSummary?APICode={API_CODE}
    //Info: Returns a list of members of your alliance and their current effect summaries
    //API Cost: 3
    //Note: Not all effects are returned. Technology income is omitted so it can't be used to discover technology bribes
    //Note: All cost reduction is returned in its raw form as this is a summary of effects. This means if this returns a 10 for land cost reduction then the actually effect is 0.99 ^ 10 not 10%.
    //Note: The effects of technology and projects are not included in these numbers as they are handled differently by the backend server
    public DnsQuery<NationsEffectsSummary> nationsEffectsSummary() {
        return query(NationsEffectsSummary.class, "NationsEffectsSummary", 3);
    }
    //Alliance Bank History
    //AllianceBankHistory?APICode={API_CODE}
    //Info: Returns a list of bank transaction values for your own alliance.
    //API Cost: 5
    public DnsQuery<BankHistory> allianceBankHistory() {
        return query(BankHistory.class, "AllianceBankHistory", 5);
    }
    //Alliance Grant History
    //AllianceGrantHistory?APICode={API_CODE}
    //Info: Returns a list of Grants for your own alliance.
    //API Cost: 3
    public DnsQuery<AllianceGrantHistory> allianceGrantHistory() {
        return query(AllianceGrantHistory.class, "AllianceGrantHistory", 3);
    }
    //Alliance Loan History
    //AllianceLoanHistory?APICode={API_CODE}
    //Info: Returns a list of loans for your own alliance.
    //API Cost: 3
    public DnsQuery<LoanHistory> allianceLoanHistory() {
        return query(LoanHistory.class, "AllianceLoanHistory", 3);
    }
    //Alliance Equipment Transaction History
    //AllianceEquipmentTransactionHistory?APICode={API_CODE}
    //Info: Returns a list of equipment transaction values for your own alliance.
    //API Cost: 5
    public DnsQuery<EquipmentTransactionHistory> allianceEquipmentTransactionHistory() {
        return query(EquipmentTransactionHistory.class, "AllianceEquipmentTransactionHistory", 5);
    }
    //Alliance Equipment
    //AllianceInventory?APICode={API_CODE}
    //Info: Returns a list of equipment transaction values for your own alliance.
    //API Cost: 3
    public DnsQuery<AllianceInventory> allianceInventory() {
        return query(AllianceInventory.class, "AllianceInventory", 3);
    }
    //Alliance Bank
    //AllianceBankValues?APICode={API_CODE}
    //Info: Returns the current resources the alliance has in its bank, the current total member deposits, and also the total amount owed to alliance members.
    //API Cost: 1
    public DnsQuery<AllianceBankValues> allianceBankValues() {
        return query(AllianceBankValues.class, "AllianceBankValues", 1);
    }
    //Alliance Tax Income
    //AllianceTaxIncome?APICode={API_CODE}
    //Info: Returns the current alliance daily tax income. Note: The daily tech tax income value is only updated once per day in order to prevent tech bribe detection bots.
    //API Cost: 1
    public DnsQuery<AllianceTaxIncome> allianceTaxIncome() {
        return query(AllianceTaxIncome.class, "AllianceTaxIncome", 1);
    }
    //Alliance Applications
    //AllianceApplication?APICode={API_CODE}
    //Info: Returns a list of active alliance applications
    //API Cost: 1
    public DnsQuery<AllianceApplication> allianceApplication() {
        return query(AllianceApplication.class, "AllianceApplication", 1);
    }
    //Alliance Grant Request
    //AllianceGrantRequest?APICode={API_CODE}
    //Info: Returns a list of active grant request for your own alliance.
    //API Cost: 1
    public DnsQuery<AllianceGrantRequest> allianceGrantRequest() {
        return query(AllianceGrantRequest.class, "AllianceGrantRequest", 1);
    }
    //Alliance Loan Request
    //AllianceLoanRequest?APICode={API_CODE}
    //Info: Returns a list of active loan request for your own alliance.
    //API Cost: 1
    public DnsQuery<AllianceLoanRequest> allianceLoanRequest() {
        return query(AllianceLoanRequest.class, "AllianceLoanRequest", 1);
    }
    //Alliance Member Bank History
    //NationBankHistory?APICode={API_CODE}&NationId={NATION_ID}
    //Info: Returns a list of bank transaction values for a member of your alliance.
    //API Cost: 3
    public DnsQuery<BankHistory> nationBankHistory(int nationId) {
        return query(BankHistory.class, "NationBankHistory", 3).add("NationId", nationId);
    }
    //Alliance Member Equipment Transaction History
    //NationEquipmentTransactionHistory?APICode={API_CODE}&NationId={NATION_ID}
    //Info: Returns a list of equipment transactions for a member of your alliance.
    //API Cost: 3
    public DnsQuery<EquipmentTransactionHistory> nationEquipmentTransactionHistory(int nationId) {
        return query(EquipmentTransactionHistory.class, "NationEquipmentTransactionHistory", 3).add("NationId", nationId);
    }
    //Alliance Member Trade History
    //NationTradeHistory?APICode={API_CODE}&NationId={NATION_ID}
    //Info: Returns a list of trade transaction values for a member of your alliance.
    //API Cost: 3
    public DnsQuery<Void> nationTradeHistory(int nationId) {
        throw new UnsupportedOperationException("Not implemented");
    }
    //Alliance Member Nation Buildings
    //NationBuildings?APICode={API_CODE}&NationId={NATION_ID}
    //Info: Returns a list of buildings for a single member of your alliance.
    //API Cost: 1
    public DnsQuery<NationBuildings> nationBuildings(int nationId) {
        return query(NationBuildings.class, "NationBuildings", 1).add("NationId", nationId);
    }
    //Optional Parameters:
    //ExtraInfo: Adds Development, Land, Core Land, and Non Core Land (&ExtraInfo=true)
    //Alliance Member Policy Last Ran
    //NationPolicyLastRan?APICode={API_CODE}&NationId={NATION_ID}
    //Info: Returns a list of immediate action and budgets and the server time they were last ran for a single nation.
    //API Cost: 1
    public DnsQuery<NationPolicyLastRan> nationPolicyLastRan(int nationId) {
        return query(NationPolicyLastRan.class, "NationPolicyLastRan", 1).add("NationId", nationId);
    }
    //Alliance Member Nation Contribution
    //NationContribution?APICode={API_CODE}&NationId={NATION_ID}
    //Info: Returns the contribution amount for a single member of your alliance.
    //API Cost: 1
    public DnsQuery<Contribution> nationContribution(int nationId) {
        return query(Contribution.class, "NationContribution", 1).add("NationId", nationId);
    }
    //Alliance Member Bank Deposits
    //AllianceMemberBankDeposits?APICode={API_CODE}
    //Info: Returns a list of bank deposits for all members of your alliance.
    //API Cost: 1
    public DnsQuery<AllianceMemberBankDeposits> allianceMemberBankDeposits() {
        return query(AllianceMemberBankDeposits.class, "AllianceMemberBankDeposits", 1);
    }
    //Alliance Member Inventory
    //AllianceMemberInventory?APICode={API_CODE}&NationId={NATION_ID}
    //Info: Returns a list of equipment for single member of your alliance. If a nation in your alliance has disabled Alliance Information Access then this will not return anything for them.
    //Requires you to be a vice leader, or have your alliances permission to approve grants or loans to access this
    //API Cost: 1
    public DnsQuery<AllianceMemberInventory> allianceMemberInventory(int nationId) {
        return query(AllianceMemberInventory.class, "AllianceMemberInventory", 1).add("NationId", nationId);
    }
    //Alliance Member Funds
    //AllianceMemberFunds?APICode={API_CODE}
    //Info: Returns a list of members of your alliance and their current funds levels on their nation. Any nation in your alliance that has disabled Alliance Information Access will not appear in this list.
    //Requires you to be a vice leader, or have your alliances permission to approve grants or loans to access this
    //API Cost: 1
    public DnsQuery<AllianceMemberFunds> allianceMemberFunds() {
        return query(AllianceMemberFunds.class, "AllianceMemberFunds", 1);
    }
    //War History
    //Nation War History
    //NationWarHistory?APICode={API_CODE}&NationId={NATION_ID}
    //Info: Returns a list of wars and corresponding data for a specific nation
    //API Cost: 1
    public DnsQuery<WarHistory> nationWarHistory(Integer nationId) {
        DnsQuery<WarHistory> query = query(WarHistory.class, "NationWarHistory", 1);
        if (nationId != null) {
            query.add("NationId", nationId);
        }
        return query;
    }
    //Optional Parameters:
    //StartDate: any war started on or after this date (year/month/day)
    //StarDateEnd: any war started on or before this date (year/month/day)
    //OnlyActive: only active wars. (OnlyActive=true)
    //OnlyDefensive: only defensive wars. (OnlyDefensive=true)
    //OnlyOffensive: only offensive wars. (OnlyOffensive=true)
    //Nation War Action History
    //WarActionHistory?APICode={API_CODE}&WarId={WAR_ID}
    //Info: Returns a list of actions and units loses per action in a war.
    //API Cost: 1
    public DnsQuery<WarActionHistory> warActionHistory(int warId, Date startDate, Date endDate, Boolean onlyActive, Boolean onlyDefensive, Boolean onlyOffensive) {
        DnsQuery<WarActionHistory> query = query(WarActionHistory.class, "WarActionHistory", 1).add("WarId", warId);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        if (startDate != null) query.add("StartDate", formatter.format(startDate));
        if (endDate != null) query.add("EndDate", formatter.format(endDate));
        if (onlyActive != null) query.add("OnlyActive", onlyActive.toString().toLowerCase(Locale.ROOT));
        if (onlyDefensive != null) query.add("OnlyDefensive", onlyDefensive.toString().toLowerCase(Locale.ROOT));
        if (onlyOffensive != null) query.add("OnlyOffensive", onlyOffensive.toString().toLowerCase(Locale.ROOT));
        return query;
    }
    //WarId can be retrieved from either the nation or alliance war history API
    //Alliance War History
    //AllianceWarHistory?APICode={API_CODE}&AllianceId={ALLIANCE_ID}
    //Info: Returns a list of wars and corresponding data for a specific alliances. Defaults to only active wars.
    //API Cost: 1(10 if pulling more then active wars)
    //Optional Parameters:
    //PullAll: Allows for pulling of inactive wars . Increase API cost to 10 (PullAll=true)
    //StartDate: any war started on or after this date (year/month/day)
    //StarDateEnd: any war started on or before this date (year/month/day)
    //OnlyDefensive: only defensive wars. (OnlyDefensive=true)
    //OnlyOffensive: only offensive wars. (OnlyOffensive=true)
    public DnsQuery<WarHistory> allianceWarHistory(int allianceId, Boolean pullAll, Date startDate, Date endDate, Boolean onlyDefensive, Boolean onlyOffensive) {
        int cost = 0;
        if (pullAll != null) cost = 10;
        DnsQuery<WarHistory> query = query(WarHistory.class, "AllianceWarHistory", cost).add("AllianceId", allianceId);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        if (pullAll != null) query.add("PullAll", pullAll.toString().toLowerCase(Locale.ROOT));
        if (startDate != null) query.add("StartDate", formatter.format(startDate));
        if (endDate != null) query.add("EndDate", formatter.format(endDate));
        if (onlyDefensive != null) query.add("OnlyDefensive", onlyDefensive.toString().toLowerCase(Locale.ROOT));
        if (onlyOffensive != null) query.add("OnlyOffensive", onlyOffensive.toString().toLowerCase(Locale.ROOT));
        return query;
    }
    // Active War History
    //NationWarHistory?APICode={API_CODE}&NationId={NATION_ID}
    //Info: Returns a list of ACTIVE wars for all nations. By defaults only returns basic data. Extra stats can be returned for additional API cost.
    //API Cost: 3(10 with extra stats)
    //Optional Parameters:
    //StartDate: any war started on or after this date (year/month/day)
    //StarDateEnd: any war started on or before this date (year/month/day)
    //pullStats: Pulls war stats. (PullStats=true)
    public DnsQuery<WarHistory> activeWarHistory(int nationId, Date startDate, Date endDate, Boolean pullStats) {
        int cost = 3;
        if (pullStats != null) cost = 10;
        DnsQuery<WarHistory> query = query(WarHistory.class, "NationWarHistory", cost).add("NationId", nationId);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        if (startDate != null) query.add("StartDate", formatter.format(startDate));
        if (endDate != null) query.add("EndDate", formatter.format(endDate));
        if (pullStats != null) query.add("PullStats", pullStats.toString().toLowerCase(Locale.ROOT));
        return query;
    }
}
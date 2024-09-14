package link.locutus.discord.commands.manager.v2.impl.pw.refs;
import link.locutus.discord.commands.manager.v2.command.AutoRegister;
import link.locutus.discord.commands.manager.v2.command.CommandRef;
public class NationCommands {
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="allianceSeniority")
        public static class allianceSeniority extends CommandRef {
            public static final allianceSeniority cmd = new allianceSeniority();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="allianceSeniorityApplicant")
        public static class allianceSeniorityApplicant extends CommandRef {
            public static final allianceSeniorityApplicant cmd = new allianceSeniorityApplicant();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="allianceSeniorityApplicantMs")
        public static class allianceSeniorityApplicantMs extends CommandRef {
            public static final allianceSeniorityApplicantMs cmd = new allianceSeniorityApplicantMs();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="allianceSeniorityMs")
        public static class allianceSeniorityMs extends CommandRef {
            public static final allianceSeniorityMs cmd = new allianceSeniorityMs();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="allianceSeniorityNoneMs")
        public static class allianceSeniorityNoneMs extends CommandRef {
            public static final allianceSeniorityNoneMs cmd = new allianceSeniorityNoneMs();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="avg_daily_login")
        public static class avg_daily_login extends CommandRef {
            public static final avg_daily_login cmd = new avg_daily_login();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="avg_daily_login_turns")
        public static class avg_daily_login_turns extends CommandRef {
            public static final avg_daily_login_turns cmd = new avg_daily_login_turns();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="avg_daily_login_week")
        public static class avg_daily_login_week extends CommandRef {
            public static final avg_daily_login_week cmd = new avg_daily_login_week();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="canBeDeclaredOnByScore")
        public static class canBeDeclaredOnByScore extends CommandRef {
            public static final canBeDeclaredOnByScore cmd = new canBeDeclaredOnByScore();
        public canBeDeclaredOnByScore score(String value) {
            return set("score", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="canBeSpiedByScore")
        public static class canBeSpiedByScore extends CommandRef {
            public static final canBeSpiedByScore cmd = new canBeSpiedByScore();
        public canBeSpiedByScore score(String value) {
            return set("score", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="canDeclareOnScore")
        public static class canDeclareOnScore extends CommandRef {
            public static final canDeclareOnScore cmd = new canDeclareOnScore();
        public canDeclareOnScore score(String value) {
            return set("score", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="canSpyOnScore")
        public static class canSpyOnScore extends CommandRef {
            public static final canSpyOnScore cmd = new canSpyOnScore();
        public canSpyOnScore score(String value) {
            return set("score", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="cellLookup")
        public static class cellLookup extends CommandRef {
            public static final cellLookup cmd = new cellLookup();
        public cellLookup sheet(String value) {
            return set("sheet", value);
        }

        public cellLookup tabName(String value) {
            return set("tabName", value);
        }

        public cellLookup columnSearch(String value) {
            return set("columnSearch", value);
        }

        public cellLookup columnOutput(String value) {
            return set("columnOutput", value);
        }

        public cellLookup search(String value) {
            return set("search", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="correctAllianceMMR")
        public static class correctAllianceMMR extends CommandRef {
            public static final correctAllianceMMR cmd = new correctAllianceMMR();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="countWars")
        public static class countWars extends CommandRef {
            public static final countWars cmd = new countWars();
        public countWars warFilter(String value) {
            return set("warFilter", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="daysSince3ConsecutiveLogins")
        public static class daysSince3ConsecutiveLogins extends CommandRef {
            public static final daysSince3ConsecutiveLogins cmd = new daysSince3ConsecutiveLogins();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="daysSince4ConsecutiveLogins")
        public static class daysSince4ConsecutiveLogins extends CommandRef {
            public static final daysSince4ConsecutiveLogins cmd = new daysSince4ConsecutiveLogins();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="daysSince5ConsecutiveLogins")
        public static class daysSince5ConsecutiveLogins extends CommandRef {
            public static final daysSince5ConsecutiveLogins cmd = new daysSince5ConsecutiveLogins();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="daysSince6ConsecutiveLogins")
        public static class daysSince6ConsecutiveLogins extends CommandRef {
            public static final daysSince6ConsecutiveLogins cmd = new daysSince6ConsecutiveLogins();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="daysSince7ConsecutiveLogins")
        public static class daysSince7ConsecutiveLogins extends CommandRef {
            public static final daysSince7ConsecutiveLogins cmd = new daysSince7ConsecutiveLogins();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="daysSinceConsecutiveLogins")
        public static class daysSinceConsecutiveLogins extends CommandRef {
            public static final daysSinceConsecutiveLogins cmd = new daysSinceConsecutiveLogins();
        public daysSinceConsecutiveLogins checkPastXDays(String value) {
            return set("checkPastXDays", value);
        }

        public daysSinceConsecutiveLogins sequentialDays(String value) {
            return set("sequentialDays", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="daysSinceLastBankDeposit")
        public static class daysSinceLastBankDeposit extends CommandRef {
            public static final daysSinceLastBankDeposit cmd = new daysSinceLastBankDeposit();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="daysSinceLastDefensiveWarLoss")
        public static class daysSinceLastDefensiveWarLoss extends CommandRef {
            public static final daysSinceLastDefensiveWarLoss cmd = new daysSinceLastDefensiveWarLoss();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="daysSinceLastOffensive")
        public static class daysSinceLastOffensive extends CommandRef {
            public static final daysSinceLastOffensive cmd = new daysSinceLastOffensive();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="daysSinceLastSelfWithdrawal")
        public static class daysSinceLastSelfWithdrawal extends CommandRef {
            public static final daysSinceLastSelfWithdrawal cmd = new daysSinceLastSelfWithdrawal();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="daysSinceLastWar")
        public static class daysSinceLastWar extends CommandRef {
            public static final daysSinceLastWar cmd = new daysSinceLastWar();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="equilibriumTaxRate")
        public static class equilibriumTaxRate extends CommandRef {
            public static final equilibriumTaxRate cmd = new equilibriumTaxRate();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getActiveWarsWith")
        public static class getActiveWarsWith extends CommandRef {
            public static final getActiveWarsWith cmd = new getActiveWarsWith();
        public getActiveWarsWith filter(String value) {
            return set("filter", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getAgeDays")
        public static class getAgeDays extends CommandRef {
            public static final getAgeDays cmd = new getAgeDays();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getAllTimeDefensiveWars")
        public static class getAllTimeDefensiveWars extends CommandRef {
            public static final getAllTimeDefensiveWars cmd = new getAllTimeDefensiveWars();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getAllTimeOffDefWars")
        public static class getAllTimeOffDefWars extends CommandRef {
            public static final getAllTimeOffDefWars cmd = new getAllTimeOffDefWars();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getAllTimeOffensiveWars")
        public static class getAllTimeOffensiveWars extends CommandRef {
            public static final getAllTimeOffensiveWars cmd = new getAllTimeOffensiveWars();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getAllTimeWars")
        public static class getAllTimeWars extends CommandRef {
            public static final getAllTimeWars cmd = new getAllTimeWars();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getAlliance")
        public static class getAlliance extends CommandRef {
            public static final getAlliance cmd = new getAlliance();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getAllianceName")
        public static class getAllianceName extends CommandRef {
            public static final getAllianceName cmd = new getAllianceName();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getAllianceRank")
        public static class getAllianceRank extends CommandRef {
            public static final getAllianceRank cmd = new getAllianceRank();
        public getAllianceRank filter(String value) {
            return set("filter", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getAllianceUrl")
        public static class getAllianceUrl extends CommandRef {
            public static final getAllianceUrl cmd = new getAllianceUrl();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getAlliance_id")
        public static class getAlliance_id extends CommandRef {
            public static final getAlliance_id cmd = new getAlliance_id();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getAttacking")
        public static class getAttacking extends CommandRef {
            public static final getAttacking cmd = new getAttacking();
        public getAttacking nations(String value) {
            return set("nations", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getAuditResult")
        public static class getAuditResult extends CommandRef {
            public static final getAuditResult cmd = new getAuditResult();
        public getAuditResult audit(String value) {
            return set("audit", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getAuditResultString")
        public static class getAuditResultString extends CommandRef {
            public static final getAuditResultString cmd = new getAuditResultString();
        public getAuditResultString audit(String value) {
            return set("audit", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getCashOutput")
        public static class getCashOutput extends CommandRef {
            public static final getCashOutput cmd = new getCashOutput();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getCommerceIndex")
        public static class getCommerceIndex extends CommandRef {
            public static final getCommerceIndex cmd = new getCommerceIndex();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getCorporationIncome")
        public static class getCorporationIncome extends CommandRef {
            public static final getCorporationIncome cmd = new getCorporationIncome();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getDate")
        public static class getDate extends CommandRef {
            public static final getDate cmd = new getDate();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getDef")
        public static class getDef extends CommandRef {
            public static final getDef cmd = new getDef();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getDefending")
        public static class getDefending extends CommandRef {
            public static final getDefending cmd = new getDefending();
        public getDefending nations(String value) {
            return set("nations", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getDeposits")
        public static class getDeposits extends CommandRef {
            public static final getDeposits cmd = new getDeposits();
        public getDeposits start(String value) {
            return set("start", value);
        }

        public getDeposits end(String value) {
            return set("end", value);
        }

        public getDeposits ignoreOffsets(String value) {
            return set("ignoreOffsets", value);
        }

        public getDeposits includeExpired(String value) {
            return set("includeExpired", value);
        }

        public getDeposits includeIgnored(String value) {
            return set("includeIgnored", value);
        }

        public getDeposits excludeTypes(String value) {
            return set("excludeTypes", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getDevastation")
        public static class getDevastation extends CommandRef {
            public static final getDevastation cmd = new getDevastation();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getDiscordUser")
        public static class getDiscordUser extends CommandRef {
            public static final getDiscordUser cmd = new getDiscordUser();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getEducationIndex")
        public static class getEducationIndex extends CommandRef {
            public static final getEducationIndex cmd = new getEducationIndex();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getEmploymentIndex")
        public static class getEmploymentIndex extends CommandRef {
            public static final getEmploymentIndex cmd = new getEmploymentIndex();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getEnemies")
        public static class getEnemies extends CommandRef {
            public static final getEnemies cmd = new getEnemies();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getEnemyStrength")
        public static class getEnemyStrength extends CommandRef {
            public static final getEnemyStrength cmd = new getEnemyStrength();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getFighting")
        public static class getFighting extends CommandRef {
            public static final getFighting cmd = new getFighting();
        public getFighting nations(String value) {
            return set("nations", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getFreeOffensiveSlots")
        public static class getFreeOffensiveSlots extends CommandRef {
            public static final getFreeOffensiveSlots cmd = new getFreeOffensiveSlots();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getFuelOutput")
        public static class getFuelOutput extends CommandRef {
            public static final getFuelOutput cmd = new getFuelOutput();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getId")
        public static class getId extends CommandRef {
            public static final getId cmd = new getId();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getInactiveMs")
        public static class getInactiveMs extends CommandRef {
            public static final getInactiveMs cmd = new getInactiveMs();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getInfra")
        public static class getInfra extends CommandRef {
            public static final getInfra cmd = new getInfra();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getLand")
        public static class getLand extends CommandRef {
            public static final getLand cmd = new getLand();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getLeader")
        public static class getLeader extends CommandRef {
            public static final getLeader cmd = new getLeader();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getMaxDef")
        public static class getMaxDef extends CommandRef {
            public static final getMaxDef cmd = new getMaxDef();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getMaxOff")
        public static class getMaxOff extends CommandRef {
            public static final getMaxOff cmd = new getMaxOff();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getMineralOutput")
        public static class getMineralOutput extends CommandRef {
            public static final getMineralOutput cmd = new getMineralOutput();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getName")
        public static class getName extends CommandRef {
            public static final getName cmd = new getName();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getNation")
        public static class getNation extends CommandRef {
            public static final getNation cmd = new getNation();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getNation_id")
        public static class getNation_id extends CommandRef {
            public static final getNation_id cmd = new getNation_id();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getNations")
        public static class getNations extends CommandRef {
            public static final getNations cmd = new getNations();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getNonCoreLand")
        public static class getNonCoreLand extends CommandRef {
            public static final getNonCoreLand cmd = new getNonCoreLand();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getNumDefWarsSince")
        public static class getNumDefWarsSince extends CommandRef {
            public static final getNumDefWarsSince cmd = new getNumDefWarsSince();
        public getNumDefWarsSince date(String value) {
            return set("date", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getNumOffWarsSince")
        public static class getNumOffWarsSince extends CommandRef {
            public static final getNumOffWarsSince cmd = new getNumOffWarsSince();
        public getNumOffWarsSince date(String value) {
            return set("date", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getNumReports")
        public static class getNumReports extends CommandRef {
            public static final getNumReports cmd = new getNumReports();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getNumWars")
        public static class getNumWars extends CommandRef {
            public static final getNumWars cmd = new getNumWars();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getNumWarsAgainstActives")
        public static class getNumWarsAgainstActives extends CommandRef {
            public static final getNumWarsAgainstActives cmd = new getNumWarsAgainstActives();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getNumWarsSince")
        public static class getNumWarsSince extends CommandRef {
            public static final getNumWarsSince cmd = new getNumWarsSince();
        public getNumWarsSince date(String value) {
            return set("date", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getOff")
        public static class getOff extends CommandRef {
            public static final getOff cmd = new getOff();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getOnlineStatus")
        public static class getOnlineStatus extends CommandRef {
            public static final getOnlineStatus cmd = new getOnlineStatus();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getOtherIncome")
        public static class getOtherIncome extends CommandRef {
            public static final getOtherIncome cmd = new getOtherIncome();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getPoliticalPowerOutput")
        public static class getPoliticalPowerOutput extends CommandRef {
            public static final getPoliticalPowerOutput cmd = new getPoliticalPowerOutput();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getPopulation")
        public static class getPopulation extends CommandRef {
            public static final getPopulation cmd = new getPopulation();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getPosition")
        public static class getPosition extends CommandRef {
            public static final getPosition cmd = new getPosition();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getPositionEnum")
        public static class getPositionEnum extends CommandRef {
            public static final getPositionEnum cmd = new getPositionEnum();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getPowerIndex")
        public static class getPowerIndex extends CommandRef {
            public static final getPowerIndex cmd = new getPowerIndex();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getProductionOutput")
        public static class getProductionOutput extends CommandRef {
            public static final getProductionOutput cmd = new getProductionOutput();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getProject")
        public static class getProject extends CommandRef {
            public static final getProject cmd = new getProject();
        public getProject project(String value) {
            return set("project", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getProtectionEnds")
        public static class getProtectionEnds extends CommandRef {
            public static final getProtectionEnds cmd = new getProtectionEnds();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getProtectionRemainingMs")
        public static class getProtectionRemainingMs extends CommandRef {
            public static final getProtectionRemainingMs cmd = new getProtectionRemainingMs();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getRareMetalOutput")
        public static class getRareMetalOutput extends CommandRef {
            public static final getRareMetalOutput cmd = new getRareMetalOutput();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getRelativeStrength")
        public static class getRelativeStrength extends CommandRef {
            public static final getRelativeStrength cmd = new getRelativeStrength();

        }
        @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.binding.DefaultPlaceholders.class,method="getResource")
        public static class getResource extends CommandRef {
            public static final getResource cmd = new getResource();
        public getResource resources(String value) {
            return set("resources", value);
        }

        public getResource resource(String value) {
            return set("resource", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.binding.DefaultPlaceholders.class,method="getResourceValue")
        public static class getResourceValue extends CommandRef {
            public static final getResourceValue cmd = new getResourceValue();
        public getResourceValue resources(String value) {
            return set("resources", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getRevenue")
        public static class getRevenue extends CommandRef {
            public static final getRevenue cmd = new getRevenue();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getRevenueConverted")
        public static class getRevenueConverted extends CommandRef {
            public static final getRevenueConverted cmd = new getRevenueConverted();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getScore")
        public static class getScore extends CommandRef {
            public static final getScore cmd = new getScore();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getStabilityIndex")
        public static class getStabilityIndex extends CommandRef {
            public static final getStabilityIndex cmd = new getStabilityIndex();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getStrongestEnemy")
        public static class getStrongestEnemy extends CommandRef {
            public static final getStrongestEnemy cmd = new getStrongestEnemy();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getStrongestEnemyOfScore")
        public static class getStrongestEnemyOfScore extends CommandRef {
            public static final getStrongestEnemyOfScore cmd = new getStrongestEnemyOfScore();
        public getStrongestEnemyOfScore minScore(String value) {
            return set("minScore", value);
        }

        public getStrongestEnemyOfScore maxScore(String value) {
            return set("maxScore", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getStrongestEnemyRelative")
        public static class getStrongestEnemyRelative extends CommandRef {
            public static final getStrongestEnemyRelative cmd = new getStrongestEnemyRelative();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getStrongestOffEnemyOfScore")
        public static class getStrongestOffEnemyOfScore extends CommandRef {
            public static final getStrongestOffEnemyOfScore cmd = new getStrongestOffEnemyOfScore();
        public getStrongestOffEnemyOfScore minScore(String value) {
            return set("minScore", value);
        }

        public getStrongestOffEnemyOfScore maxScore(String value) {
            return set("maxScore", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getTaxIncome")
        public static class getTaxIncome extends CommandRef {
            public static final getTaxIncome cmd = new getTaxIncome();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getTechIndex")
        public static class getTechIndex extends CommandRef {
            public static final getTechIndex cmd = new getTechIndex();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getTechnology")
        public static class getTechnology extends CommandRef {
            public static final getTechnology cmd = new getTechnology();
        public getTechnology technology(String value) {
            return set("technology", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getTier")
        public static class getTier extends CommandRef {
            public static final getTier cmd = new getTier();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getTransportationIndex")
        public static class getTransportationIndex extends CommandRef {
            public static final getTransportationIndex cmd = new getTransportationIndex();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getUraniumOutput")
        public static class getUraniumOutput extends CommandRef {
            public static final getUraniumOutput cmd = new getUraniumOutput();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getUrl")
        public static class getUrl extends CommandRef {
            public static final getUrl cmd = new getUrl();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getUserAgeDays")
        public static class getUserAgeDays extends CommandRef {
            public static final getUserAgeDays cmd = new getUserAgeDays();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getUserAgeMs")
        public static class getUserAgeMs extends CommandRef {
            public static final getUserAgeMs cmd = new getUserAgeMs();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getUserDiscriminator")
        public static class getUserDiscriminator extends CommandRef {
            public static final getUserDiscriminator cmd = new getUserDiscriminator();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getUserId")
        public static class getUserId extends CommandRef {
            public static final getUserId cmd = new getUserId();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getUserMention")
        public static class getUserMention extends CommandRef {
            public static final getUserMention cmd = new getUserMention();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="getWarIndex")
        public static class getWarIndex extends CommandRef {
            public static final getWarIndex cmd = new getWarIndex();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="hasProtection")
        public static class hasProtection extends CommandRef {
            public static final hasProtection cmd = new hasProtection();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="inactivity_streak")
        public static class inactivity_streak extends CommandRef {
            public static final inactivity_streak cmd = new inactivity_streak();
        public inactivity_streak daysInactive(String value) {
            return set("daysInactive", value);
        }

        public inactivity_streak checkPastXDays(String value) {
            return set("checkPastXDays", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="isAttackingEnemyOfScore")
        public static class isAttackingEnemyOfScore extends CommandRef {
            public static final isAttackingEnemyOfScore cmd = new isAttackingEnemyOfScore();
        public isAttackingEnemyOfScore minScore(String value) {
            return set("minScore", value);
        }

        public isAttackingEnemyOfScore maxScore(String value) {
            return set("maxScore", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="isEnemy")
        public static class isEnemy extends CommandRef {
            public static final isEnemy cmd = new isEnemy();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="isFightingActive")
        public static class isFightingActive extends CommandRef {
            public static final isFightingActive cmd = new isFightingActive();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="isFightingEnemyOfScore")
        public static class isFightingEnemyOfScore extends CommandRef {
            public static final isFightingEnemyOfScore cmd = new isFightingEnemyOfScore();
        public isFightingEnemyOfScore minScore(String value) {
            return set("minScore", value);
        }

        public isFightingEnemyOfScore maxScore(String value) {
            return set("maxScore", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="isIn")
        public static class isIn extends CommandRef {
            public static final isIn cmd = new isIn();
        public isIn nations(String value) {
            return set("nations", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="isInAllianceGuild")
        public static class isInAllianceGuild extends CommandRef {
            public static final isInAllianceGuild cmd = new isInAllianceGuild();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="isInMilcomGuild")
        public static class isInMilcomGuild extends CommandRef {
            public static final isInMilcomGuild cmd = new isInMilcomGuild();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="isInSpyRange")
        public static class isInSpyRange extends CommandRef {
            public static final isInSpyRange cmd = new isInSpyRange();
        public isInSpyRange other(String value) {
            return set("other", value);
        }

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="isInWarRange")
        public static class isInWarRange extends CommandRef {
            public static final isInWarRange cmd = new isInWarRange();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="isInactiveForWar")
        public static class isInactiveForWar extends CommandRef {
            public static final isInactiveForWar cmd = new isInactiveForWar();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="isOnline")
        public static class isOnline extends CommandRef {
            public static final isOnline cmd = new isOnline();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="isReroll")
        public static class isReroll extends CommandRef {
            public static final isReroll cmd = new isReroll();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="isVacation")
        public static class isVacation extends CommandRef {
            public static final isVacation cmd = new isVacation();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="isVerified")
        public static class isVerified extends CommandRef {
            public static final isVerified cmd = new isVerified();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="lastBankDeposit")
        public static class lastBankDeposit extends CommandRef {
            public static final lastBankDeposit cmd = new lastBankDeposit();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="lastSelfWithdrawal")
        public static class lastSelfWithdrawal extends CommandRef {
            public static final lastSelfWithdrawal cmd = new lastSelfWithdrawal();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="login_daychange")
        public static class login_daychange extends CommandRef {
            public static final login_daychange cmd = new login_daychange();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="lostInactiveWar")
        public static class lostInactiveWar extends CommandRef {
            public static final lostInactiveWar cmd = new lostInactiveWar();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="minWarResistance")
        public static class minWarResistance extends CommandRef {
            public static final minWarResistance cmd = new minWarResistance();

        }
        @AutoRegister(clazz=link.locutus.discord.db.entities.DBNation.class,method="passesAudit")
        public static class passesAudit extends CommandRef {
            public static final passesAudit cmd = new passesAudit();
        public passesAudit audit(String value) {
            return set("audit", value);
        }

        }

}

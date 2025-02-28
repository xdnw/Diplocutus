package link.locutus.discord.commands.manager.v2.impl.pw.refs;
import link.locutus.discord.commands.manager.v2.command.AutoRegister;
import link.locutus.discord.commands.manager.v2.command.CommandRef;
public class CM {
        public static class admin{
            public static class alliance{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="runMilitarizationAlerts")
                public static class military_alerts extends CommandRef {
                    public static final military_alerts cmd = new military_alerts();

                }
            }
            public static class command{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="runForNations")
                public static class format_for_nations extends CommandRef {
                    public static final format_for_nations cmd = new format_for_nations();
                public format_for_nations nations(String value) {
                    return set("nations", value);
                }

                public format_for_nations command(String value) {
                    return set("command", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="runMultiple")
                public static class multiple extends CommandRef {
                    public static final multiple cmd = new multiple();
                public multiple commands(String value) {
                    return set("commands", value);
                }

                }
            }
            public static class conflicts{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="checkActiveConflicts")
                public static class check extends CommandRef {
                    public static final check cmd = new check();

                }
            }
            public static class debug{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="guildInfo")
                public static class guild extends CommandRef {
                    public static final guild cmd = new guild();
                public guild guild(String value) {
                    return set("guild", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="nationMeta")
                public static class nation_meta extends CommandRef {
                    public static final nation_meta cmd = new nation_meta();
                public nation_meta nation(String value) {
                    return set("nation", value);
                }

                public nation_meta meta(String value) {
                    return set("meta", value);
                }

                }
            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="dm")
            public static class dm extends CommandRef {
                public static final dm cmd = new dm();
            public dm nations(String value) {
                return set("nations", value);
            }

            public dm message(String value) {
                return set("message", value);
            }

            public dm force(String value) {
                return set("force", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.DiscordCommands.class,method="importEmojis")
            public static class importEmoji extends CommandRef {
                public static final importEmoji cmd = new importEmoji();
            public importEmoji guild(String value) {
                return set("guild", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="importGuildKeys")
            public static class importGuildKeys extends CommandRef {
                public static final importGuildKeys cmd = new importGuildKeys();

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="leaveServer")
            public static class leaveServer extends CommandRef {
                public static final leaveServer cmd = new leaveServer();
            public leaveServer guildId(String value) {
                return set("guildId", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="listAuthenticated")
            public static class listAuthenticated extends CommandRef {
                public static final listAuthenticated cmd = new listAuthenticated();

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="listExpiredGuilds")
            public static class listExpiredGuilds extends CommandRef {
                public static final listExpiredGuilds cmd = new listExpiredGuilds();
            public listExpiredGuilds checkMessages(String value) {
                return set("checkMessages", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="listGuildOwners")
            public static class listGuildOwners extends CommandRef {
                public static final listGuildOwners cmd = new listGuildOwners();

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="msgInfo")
            public static class msgInfo extends CommandRef {
                public static final msgInfo cmd = new msgInfo();
            public msgInfo message(String value) {
                return set("message", value);
            }

            public msgInfo useIds(String value) {
                return set("useIds", value);
            }

            }
            public static class queue{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="showFileQueue")
                public static class file extends CommandRef {
                    public static final file cmd = new file();
                public file timestamp(String value) {
                    return set("timestamp", value);
                }

                public file numResults(String value) {
                    return set("numResults", value);
                }

                }
            }
            public static class settings{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="infoBulk")
                public static class info_servers extends CommandRef {
                    public static final info_servers cmd = new info_servers();
                public info_servers setting(String value) {
                    return set("setting", value);
                }

                public info_servers guilds(String value) {
                    return set("guilds", value);
                }

                public info_servers sheet(String value) {
                    return set("sheet", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="unsetNews")
                public static class subscribe extends CommandRef {
                    public static final subscribe cmd = new subscribe();
                public subscribe setting(String value) {
                    return set("setting", value);
                }

                public subscribe guilds(String value) {
                    return set("guilds", value);
                }

                public subscribe news_channel(String value) {
                    return set("news_channel", value);
                }

                public subscribe unset_on_error(String value) {
                    return set("unset_on_error", value);
                }

                public subscribe force(String value) {
                    return set("force", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="unsetKeys")
                public static class unset extends CommandRef {
                    public static final unset cmd = new unset();
                public unset settings(String value) {
                    return set("settings", value);
                }

                public unset guilds(String value) {
                    return set("guilds", value);
                }

                public unset unset_cant_talk(String value) {
                    return set("unset_cant_talk", value);
                }

                public unset unset_null(String value) {
                    return set("unset_null", value);
                }

                public unset unset_key_no_perms(String value) {
                    return set("unset_key_no_perms", value);
                }

                public unset unset_invalid_aa(String value) {
                    return set("unset_invalid_aa", value);
                }

                public unset unset_all(String value) {
                    return set("unset_all", value);
                }

                public unset unset_validate(String value) {
                    return set("unset_validate", value);
                }

                public unset unsetMessage(String value) {
                    return set("unsetMessage", value);
                }

                public unset force(String value) {
                    return set("force", value);
                }

                }
            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="stop")
            public static class stop extends CommandRef {
                public static final stop cmd = new stop();
            public stop save(String value) {
                return set("save", value);
            }

            }
            public static class sudo{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="sudoNations")
                public static class nations extends CommandRef {
                    public static final nations cmd = new nations();
                public nations nations(String value) {
                    return set("nations", value);
                }

                public nations command(String value) {
                    return set("command", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="sudo")
                public static class user extends CommandRef {
                    public static final user cmd = new user();
                public user command(String value) {
                    return set("command", value);
                }

                public user user(String value) {
                    return set("user", value);
                }

                public user nation(String value) {
                    return set("nation", value);
                }

                }
            }
            public static class sync{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="savePojos")
                public static class pojos extends CommandRef {
                    public static final pojos cmd = new pojos();

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="syncRequests")
                public static class requests extends CommandRef {
                    public static final requests cmd = new requests();
                public requests alliance(String value) {
                    return set("alliance", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="syncBanks")
                public static class syncBanks extends CommandRef {
                    public static final syncBanks cmd = new syncBanks();
                public syncBanks alliance(String value) {
                    return set("alliance", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="syncMetrics")
                public static class syncMetrics extends CommandRef {
                    public static final syncMetrics cmd = new syncMetrics();
                public syncMetrics topX(String value) {
                    return set("topX", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="syncNations")
                public static class syncNations extends CommandRef {
                    public static final syncNations cmd = new syncNations();

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="syncInterviews")
                public static class syncinterviews extends CommandRef {
                    public static final syncinterviews cmd = new syncinterviews();

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="syncTreaties")
                public static class treaties extends CommandRef {
                    public static final treaties cmd = new treaties();

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="syncWarrooms")
                public static class warrooms extends CommandRef {
                    public static final warrooms cmd = new warrooms();
                public warrooms force(String value) {
                    return set("force", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="syncWars")
                public static class wars extends CommandRef {
                    public static final wars cmd = new wars();
                public wars alliance(String value) {
                    return set("alliance", value);
                }

                public wars forceAll(String value) {
                    return set("forceAll", value);
                }

                }
            }
            public static class wiki{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="dumpWiki")
                public static class save extends CommandRef {
                    public static final save cmd = new save();
                public save pathRelative(String value) {
                    return set("pathRelative", value);
                }

                }
            }
        }
        public static class alerts{
            public static class audit{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.PlayerSettingCommands.class,method="auditAlertOptOut")
                public static class optout extends CommandRef {
                    public static final optout cmd = new optout();

                }
            }
            public static class bank{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.PlayerSettingCommands.class,method="bankAlertRequiredValue")
                public static class min_value extends CommandRef {
                    public static final min_value cmd = new min_value();
                public min_value requiredValue(String value) {
                    return set("requiredValue", value);
                }

                }
            }
            public static class beige{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="beigeReminder")
                public static class beigeAlert extends CommandRef {
                    public static final beigeAlert cmd = new beigeAlert();
                public beigeAlert targets(String value) {
                    return set("targets", value);
                }

                public beigeAlert allowOutOfScore(String value) {
                    return set("allowOutOfScore", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="beigeAlertMode")
                public static class beigeAlertMode extends CommandRef {
                    public static final beigeAlertMode cmd = new beigeAlertMode();
                public beigeAlertMode mode(String value) {
                    return set("mode", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="beigeAlertOptOut")
                public static class beigeAlertOptOut extends CommandRef {
                    public static final beigeAlertOptOut cmd = new beigeAlertOptOut();

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="beigeAlertRequiredLoot")
                public static class beigeAlertRequiredLoot extends CommandRef {
                    public static final beigeAlertRequiredLoot cmd = new beigeAlertRequiredLoot();
                public beigeAlertRequiredLoot requiredLoot(String value) {
                    return set("requiredLoot", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="beigeAlertRequiredStatus")
                public static class beigeAlertRequiredStatus extends CommandRef {
                    public static final beigeAlertRequiredStatus cmd = new beigeAlertRequiredStatus();
                public beigeAlertRequiredStatus status(String value) {
                    return set("status", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="beigeReminders")
                public static class beigeReminders extends CommandRef {
                    public static final beigeReminders cmd = new beigeReminders();

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="removeBeigeReminder")
                public static class removeBeigeReminder extends CommandRef {
                    public static final removeBeigeReminder cmd = new removeBeigeReminder();
                public removeBeigeReminder nationsToRemove(String value) {
                    return set("nationsToRemove", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="setBeigeAlertScoreLeeway")
                public static class setBeigeAlertScoreLeeway extends CommandRef {
                    public static final setBeigeAlertScoreLeeway cmd = new setBeigeAlertScoreLeeway();
                public setBeigeAlertScoreLeeway scoreLeeway(String value) {
                    return set("scoreLeeway", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="testBeigeAlertAuto")
                public static class test_auto extends CommandRef {
                    public static final test_auto cmd = new test_auto();

                }
            }
            public static class enemy{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.PlayerSettingCommands.class,method="enemyAlertOptOut")
                public static class optout extends CommandRef {
                    public static final optout cmd = new optout();

                }
            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UnsortedCommands.class,method="loginNotifier")
            public static class login extends CommandRef {
                public static final login cmd = new login();
            public login target(String value) {
                return set("target", value);
            }

            public login doNotRequireWar(String value) {
                return set("doNotRequireWar", value);
            }

            }
        }
        public static class alliance{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UtilityCommands.class,method="allianceCost")
            public static class cost extends CommandRef {
                public static final cost cmd = new cost();
            public cost nations(String value) {
                return set("nations", value);
            }

            public cost update(String value) {
                return set("update", value);
            }

            public cost snapshotDate(String value) {
                return set("snapshotDate", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UnsortedCommands.class,method="leftAA")
            public static class departures extends CommandRef {
                public static final departures cmd = new departures();
            public departures nationOrAlliance(String value) {
                return set("nationOrAlliance", value);
            }

            public departures time(String value) {
                return set("time", value);
            }

            public departures filter(String value) {
                return set("filter", value);
            }

            public departures ignoreInactives(String value) {
                return set("ignoreInactives", value);
            }

            public departures ignoreVM(String value) {
                return set("ignoreVM", value);
            }

            public departures ignoreMembers(String value) {
                return set("ignoreMembers", value);
            }

            public departures listIds(String value) {
                return set("listIds", value);
            }

            public departures sheet(String value) {
                return set("sheet", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UnsortedCommands.class,method="listAllianceMembers")
            public static class listAllianceMembers extends CommandRef {
                public static final listAllianceMembers cmd = new listAllianceMembers();
            public listAllianceMembers page(String value) {
                return set("page", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UnsortedCommands.class,method="revenue")
            public static class revenue extends CommandRef {
                public static final revenue cmd = new revenue();
            public revenue nations(String value) {
                return set("nations", value);
            }

            public revenue snapshotDate(String value) {
                return set("snapshotDate", value);
            }

            }
            public static class stats{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.StatCommands.class,method="allianceAttributeRanking")
                public static class attribute_ranking extends CommandRef {
                    public static final attribute_ranking cmd = new attribute_ranking();
                public attribute_ranking alliances(String value) {
                    return set("alliances", value);
                }

                public attribute_ranking attribute(String value) {
                    return set("attribute", value);
                }

                public attribute_ranking reverseOrder(String value) {
                    return set("reverseOrder", value);
                }

                public attribute_ranking uploadFile(String value) {
                    return set("uploadFile", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.StatCommands.class,method="counterStats")
                public static class counterStats extends CommandRef {
                    public static final counterStats cmd = new counterStats();
                public counterStats alliance(String value) {
                    return set("alliance", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.StatCommands.class,method="allianceMetricsByTurn")
                public static class metric_by_turn extends CommandRef {
                    public static final metric_by_turn cmd = new metric_by_turn();
                public metric_by_turn metric(String value) {
                    return set("metric", value);
                }

                public metric_by_turn coalition(String value) {
                    return set("coalition", value);
                }

                public metric_by_turn time(String value) {
                    return set("time", value);
                }

                public metric_by_turn attachJson(String value) {
                    return set("attachJson", value);
                }

                public metric_by_turn attachCsv(String value) {
                    return set("attachCsv", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.StatCommands.class,method="allianceMetricsAB")
                public static class metrics_ab extends CommandRef {
                    public static final metrics_ab cmd = new metrics_ab();
                public metrics_ab metric(String value) {
                    return set("metric", value);
                }

                public metrics_ab coalition1(String value) {
                    return set("coalition1", value);
                }

                public metrics_ab coalition2(String value) {
                    return set("coalition2", value);
                }

                public metrics_ab time(String value) {
                    return set("time", value);
                }

                public metrics_ab attachJson(String value) {
                    return set("attachJson", value);
                }

                public metrics_ab attachCsv(String value) {
                    return set("attachCsv", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.StatCommands.class,method="allianceMetricsCompareByTurn")
                public static class metrics_by_turn extends CommandRef {
                    public static final metrics_by_turn cmd = new metrics_by_turn();
                public metrics_by_turn metric(String value) {
                    return set("metric", value);
                }

                public metrics_by_turn alliances(String value) {
                    return set("alliances", value);
                }

                public metrics_by_turn time(String value) {
                    return set("time", value);
                }

                public metrics_by_turn attachJson(String value) {
                    return set("attachJson", value);
                }

                public metrics_by_turn attachCsv(String value) {
                    return set("attachCsv", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.StatCommands.class,method="allianceRanking")
                public static class ranking extends CommandRef {
                    public static final ranking cmd = new ranking();
                public ranking alliances(String value) {
                    return set("alliances", value);
                }

                public ranking metric(String value) {
                    return set("metric", value);
                }

                public ranking reverseOrder(String value) {
                    return set("reverseOrder", value);
                }

                public ranking uploadFile(String value) {
                    return set("uploadFile", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.StatCommands.class,method="allianceRankingTime")
                public static class ranking_time extends CommandRef {
                    public static final ranking_time cmd = new ranking_time();
                public ranking_time alliances(String value) {
                    return set("alliances", value);
                }

                public ranking_time metric(String value) {
                    return set("metric", value);
                }

                public ranking_time timeStart(String value) {
                    return set("timeStart", value);
                }

                public ranking_time timeEnd(String value) {
                    return set("timeEnd", value);
                }

                public ranking_time reverseOrder(String value) {
                    return set("reverseOrder", value);
                }

                public ranking_time uploadFile(String value) {
                    return set("uploadFile", value);
                }

                }
            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UnsortedCommands.class,method="stockpile")
            public static class stockpile extends CommandRef {
                public static final stockpile cmd = new stockpile();
            public stockpile nationOrAlliance(String value) {
                return set("nationOrAlliance", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.BankCommands.class,method="allianceTaxIncome")
            public static class tax_income extends CommandRef {
                public static final tax_income cmd = new tax_income();
            public tax_income alliance(String value) {
                return set("alliance", value);
            }

            public tax_income forceUpdate(String value) {
                return set("forceUpdate", value);
            }

            }
            public static class treaty{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.FACommands.class,method="treaties")
                public static class list extends CommandRef {
                    public static final list cmd = new list();
                public list alliances(String value) {
                    return set("alliances", value);
                }

                public list treatyFilter(String value) {
                    return set("treatyFilter", value);
                }

                }
            }
        }
        public static class announcement{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="archiveAnnouncement")
            public static class archive extends CommandRef {
                public static final archive cmd = new archive();
            public archive announcementId(String value) {
                return set("announcementId", value);
            }

            public archive archive(String value) {
                return set("archive", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="announce")
            public static class create extends CommandRef {
                public static final create cmd = new create();
            public create sendTo(String value) {
                return set("sendTo", value);
            }

            public create subject(String value) {
                return set("subject", value);
            }

            public create announcement(String value) {
                return set("announcement", value);
            }

            public create replacements(String value) {
                return set("replacements", value);
            }

            public create channel(String value) {
                return set("channel", value);
            }

            public create bottomText(String value) {
                return set("bottomText", value);
            }

            public create requiredVariation(String value) {
                return set("requiredVariation", value);
            }

            public create requiredDepth(String value) {
                return set("requiredDepth", value);
            }

            public create seed(String value) {
                return set("seed", value);
            }

            public create sendDM(String value) {
                return set("sendDM", value);
            }

            public create force(String value) {
                return set("force", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.EmbedCommands.class,method="announceDocument")
            public static class document extends CommandRef {
                public static final document cmd = new document();
            public document original(String value) {
                return set("original", value);
            }

            public document sendTo(String value) {
                return set("sendTo", value);
            }

            public document replacements(String value) {
                return set("replacements", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="find_announcement")
            public static class find extends CommandRef {
                public static final find cmd = new find();
            public find announcementId(String value) {
                return set("announcementId", value);
            }

            public find message(String value) {
                return set("message", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="find_invite")
            public static class find_invite extends CommandRef {
                public static final find_invite cmd = new find_invite();
            public find_invite invite(String value) {
                return set("invite", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UnsortedCommands.class,method="sendInvite")
            public static class invite extends CommandRef {
                public static final invite cmd = new invite();
            public invite message(String value) {
                return set("message", value);
            }

            public invite inviteTo(String value) {
                return set("inviteTo", value);
            }

            public invite sendTo(String value) {
                return set("sendTo", value);
            }

            public invite expire(String value) {
                return set("expire", value);
            }

            public invite maxUsesEach(String value) {
                return set("maxUsesEach", value);
            }

            public invite sendDM(String value) {
                return set("sendDM", value);
            }

            public invite allowCreation(String value) {
                return set("allowCreation", value);
            }

            public invite force(String value) {
                return set("force", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.TestCommands.class,method="ocr")
            public static class ocr extends CommandRef {
                public static final ocr cmd = new ocr();
            public ocr discordImageUrl(String value) {
                return set("discordImageUrl", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.PlayerSettingCommands.class,method="readAnnouncement")
            public static class read extends CommandRef {
                public static final read cmd = new read();
            public read ann_id(String value) {
                return set("ann_id", value);
            }

            public read markRead(String value) {
                return set("markRead", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.PlayerSettingCommands.class,method="viewAnnouncement")
            public static class view extends CommandRef {
                public static final view cmd = new view();
            public view ann_id(String value) {
                return set("ann_id", value);
            }

            public view document(String value) {
                return set("document", value);
            }

            public view nation(String value) {
                return set("nation", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UtilityCommands.class,method="addWatermark")
            public static class watermark extends CommandRef {
                public static final watermark cmd = new watermark();
            public watermark imageUrl(String value) {
                return set("imageUrl", value);
            }

            public watermark watermarkText(String value) {
                return set("watermarkText", value);
            }

            public watermark color(String value) {
                return set("color", value);
            }

            public watermark opacity(String value) {
                return set("opacity", value);
            }

            public watermark font(String value) {
                return set("font", value);
            }

            public watermark repeat(String value) {
                return set("repeat", value);
            }

            }
        }
        public static class audit{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UnsortedCommands.class,method="checkCities")
            public static class run extends CommandRef {
                public static final run cmd = new run();
            public run nationList(String value) {
                return set("nationList", value);
            }

            public run audits(String value) {
                return set("audits", value);
            }

            public run pingUser(String value) {
                return set("pingUser", value);
            }

            public run postInInterviewChannels(String value) {
                return set("postInInterviewChannels", value);
            }

            public run skipUpdate(String value) {
                return set("skipUpdate", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UnsortedCommands.class,method="auditSheet")
            public static class sheet extends CommandRef {
                public static final sheet cmd = new sheet();
            public sheet nations(String value) {
                return set("nations", value);
            }

            public sheet includeAudits(String value) {
                return set("includeAudits", value);
            }

            public sheet excludeAudits(String value) {
                return set("excludeAudits", value);
            }

            public sheet forceUpdate(String value) {
                return set("forceUpdate", value);
            }

            public sheet verbose(String value) {
                return set("verbose", value);
            }

            public sheet sheet(String value) {
                return set("sheet", value);
            }

            }
        }
        public static class build{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.BuildCommands.class,method="getBuild")
            public static class get extends CommandRef {
                public static final get cmd = new get();
            public get nation(String value) {
                return set("nation", value);
            }

            public get update(String value) {
                return set("update", value);
            }

            }
        }
        public static class building{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.BuildCommands.class,method="buildingCost")
            public static class cost extends CommandRef {
                public static final cost cmd = new cost();
            public cost building(String value) {
                return set("building", value);
            }

            public cost start_amount(String value) {
                return set("start_amount", value);
            }

            public cost end_amount(String value) {
                return set("end_amount", value);
            }

            public cost total_slots(String value) {
                return set("total_slots", value);
            }

            public cost nation(String value) {
                return set("nation", value);
            }

            public cost buildingCostReduction(String value) {
                return set("buildingCostReduction", value);
            }

            public cost force_update(String value) {
                return set("force_update", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.BuildCommands.class,method="buildingCostBulk")
            public static class cost_bulk extends CommandRef {
                public static final cost_bulk cmd = new cost_bulk();
            public cost_bulk buildTo(String value) {
                return set("buildTo", value);
            }

            public cost_bulk total_slots(String value) {
                return set("total_slots", value);
            }

            public cost_bulk nation(String value) {
                return set("nation", value);
            }

            public cost_bulk buildingCostReduction(String value) {
                return set("buildingCostReduction", value);
            }

            public cost_bulk force_update(String value) {
                return set("force_update", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.BuildCommands.class,method="nextBuildingCost")
            public static class costs extends CommandRef {
                public static final costs cmd = new costs();
            public costs nation(String value) {
                return set("nation", value);
            }

            public costs buildingCostReduction(String value) {
                return set("buildingCostReduction", value);
            }

            public costs force_update(String value) {
                return set("force_update", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.BuildCommands.class,method="getBuildSheet")
            public static class sheet extends CommandRef {
                public static final sheet cmd = new sheet();
            public sheet nations(String value) {
                return set("nations", value);
            }

            public sheet sheet(String value) {
                return set("sheet", value);
            }

            public sheet include_effect_buildings(String value) {
                return set("include_effect_buildings", value);
            }

            public sheet update(String value) {
                return set("update", value);
            }

            }
        }
        public static class channel{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="channelMembers")
            public static class channelMembers extends CommandRef {
                public static final channelMembers cmd = new channelMembers();
            public channelMembers channel(String value) {
                return set("channel", value);
            }

            }
            public static class close{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="close")
                public static class current extends CommandRef {
                    public static final current cmd = new current();
                public current forceDelete(String value) {
                    return set("forceDelete", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="closeInactiveChannels")
                public static class inactive extends CommandRef {
                    public static final inactive cmd = new inactive();
                public inactive category(String value) {
                    return set("category", value);
                }

                public inactive age(String value) {
                    return set("age", value);
                }

                public inactive force(String value) {
                    return set("force", value);
                }

                }
            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UtilityCommands.class,method="channelCount")
            public static class count extends CommandRef {
                public static final count cmd = new count();

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.DiscordCommands.class,method="channel")
            public static class create extends CommandRef {
                public static final create cmd = new create();
            public create channelName(String value) {
                return set("channelName", value);
            }

            public create category(String value) {
                return set("category", value);
            }

            public create copypasta(String value) {
                return set("copypasta", value);
            }

            public create addInternalAffairsRole(String value) {
                return set("addInternalAffairsRole", value);
            }

            public create addMilcom(String value) {
                return set("addMilcom", value);
            }

            public create addForeignAffairs(String value) {
                return set("addForeignAffairs", value);
            }

            public create addEcon(String value) {
                return set("addEcon", value);
            }

            public create pingRoles(String value) {
                return set("pingRoles", value);
            }

            public create pingAuthor(String value) {
                return set("pingAuthor", value);
            }

            }
            public static class delete{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.DiscordCommands.class,method="deleteChannel")
                public static class current extends CommandRef {
                    public static final current cmd = new current();
                public current channel(String value) {
                    return set("channel", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="deleteAllInaccessibleChannels")
                public static class inaccessible extends CommandRef {
                    public static final inaccessible cmd = new inaccessible();
                public inaccessible force(String value) {
                    return set("force", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="debugPurgeChannels")
                public static class inactive extends CommandRef {
                    public static final inactive cmd = new inactive();
                public inactive category(String value) {
                    return set("category", value);
                }

                public inactive cutoff(String value) {
                    return set("cutoff", value);
                }

                }
            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="memberChannels")
            public static class memberChannels extends CommandRef {
                public static final memberChannels cmd = new memberChannels();
            public memberChannels member(String value) {
                return set("member", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="allChannelMembers")
            public static class members extends CommandRef {
                public static final members cmd = new members();

            }
            public static class move{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.DiscordCommands.class,method="channelDown")
                public static class Down extends CommandRef {
                    public static final Down cmd = new Down();

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.DiscordCommands.class,method="channelUp")
                public static class Up extends CommandRef {
                    public static final Up cmd = new Up();

                }
            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="open")
            public static class open extends CommandRef {
                public static final open cmd = new open();
            public open category(String value) {
                return set("category", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.DiscordCommands.class,method="channelPermissions")
            public static class permissions extends CommandRef {
                public static final permissions cmd = new permissions();
            public permissions channel(String value) {
                return set("channel", value);
            }

            public permissions nations(String value) {
                return set("nations", value);
            }

            public permissions permission(String value) {
                return set("permission", value);
            }

            public permissions negate(String value) {
                return set("negate", value);
            }

            public permissions removeOthers(String value) {
                return set("removeOthers", value);
            }

            public permissions listChanges(String value) {
                return set("listChanges", value);
            }

            public permissions pingAddedUsers(String value) {
                return set("pingAddedUsers", value);
            }

            }
            public static class rename{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.GPTCommands.class,method="emojifyChannels")
                public static class bulk extends CommandRef {
                    public static final bulk cmd = new bulk();
                public bulk sheet(String value) {
                    return set("sheet", value);
                }

                public bulk excludeCategories(String value) {
                    return set("excludeCategories", value);
                }

                public bulk includeCategories(String value) {
                    return set("includeCategories", value);
                }

                public bulk force(String value) {
                    return set("force", value);
                }

                public bulk popCultureQuotes(String value) {
                    return set("popCultureQuotes", value);
                }

                }
            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.DiscordCommands.class,method="channelCategory")
            public static class setCategory extends CommandRef {
                public static final setCategory cmd = new setCategory();
            public setCategory category(String value) {
                return set("category", value);
            }

            }
            public static class sort{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="sortChannelsName")
                public static class category_filter extends CommandRef {
                    public static final category_filter cmd = new category_filter();
                public category_filter from(String value) {
                    return set("from", value);
                }

                public category_filter categoryPrefix(String value) {
                    return set("categoryPrefix", value);
                }

                public category_filter filter(String value) {
                    return set("filter", value);
                }

                public category_filter warn_on_filter_fail(String value) {
                    return set("warn_on_filter_fail", value);
                }

                public category_filter force(String value) {
                    return set("force", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="sortChannelsSheet")
                public static class sheet extends CommandRef {
                    public static final sheet cmd = new sheet();
                public sheet from(String value) {
                    return set("from", value);
                }

                public sheet sheet(String value) {
                    return set("sheet", value);
                }

                public sheet filter(String value) {
                    return set("filter", value);
                }

                public sheet warn_on_filter_fail(String value) {
                    return set("warn_on_filter_fail", value);
                }

                public sheet force(String value) {
                    return set("force", value);
                }

                }
            }
        }
        public static class chat{
            public static class conversion{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.GPTCommands.class,method="generate_factsheet")
                public static class add_document extends CommandRef {
                    public static final add_document cmd = new add_document();
                public add_document googleDocumentUrl(String value) {
                    return set("googleDocumentUrl", value);
                }

                public add_document document_name(String value) {
                    return set("document_name", value);
                }

                public add_document force(String value) {
                    return set("force", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.GPTCommands.class,method="deleteConversion")
                public static class delete extends CommandRef {
                    public static final delete cmd = new delete();
                public delete source(String value) {
                    return set("source", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.GPTCommands.class,method="showConverting")
                public static class list extends CommandRef {
                    public static final list cmd = new list();
                public list showRoot(String value) {
                    return set("showRoot", value);
                }

                public list showOtherGuilds(String value) {
                    return set("showOtherGuilds", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.GPTCommands.class,method="pauseConversion")
                public static class pause extends CommandRef {
                    public static final pause cmd = new pause();
                public pause source(String value) {
                    return set("source", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.GPTCommands.class,method="resumeConversion")
                public static class resume extends CommandRef {
                    public static final resume cmd = new resume();
                public resume source(String value) {
                    return set("source", value);
                }

                }
            }
            public static class dataset{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.GPTCommands.class,method="delete_document")
                public static class delete extends CommandRef {
                    public static final delete cmd = new delete();
                public delete source(String value) {
                    return set("source", value);
                }

                public delete force(String value) {
                    return set("force", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.GPTCommands.class,method="save_embeddings")
                public static class import_sheet extends CommandRef {
                    public static final import_sheet cmd = new import_sheet();
                public import_sheet sheet(String value) {
                    return set("sheet", value);
                }

                public import_sheet document_name(String value) {
                    return set("document_name", value);
                }

                public import_sheet force(String value) {
                    return set("force", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.GPTCommands.class,method="list_documents")
                public static class list extends CommandRef {
                    public static final list cmd = new list();
                public list listRoot(String value) {
                    return set("listRoot", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.GPTCommands.class,method="embeddingSelect")
                public static class select extends CommandRef {
                    public static final select cmd = new select();
                public select excludeTypes(String value) {
                    return set("excludeTypes", value);
                }

                public select includeWikiCategories(String value) {
                    return set("includeWikiCategories", value);
                }

                public select excludeWikiCategories(String value) {
                    return set("excludeWikiCategories", value);
                }

                public select excludeSources(String value) {
                    return set("excludeSources", value);
                }

                public select addSources(String value) {
                    return set("addSources", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.GPTCommands.class,method="view_document")
                public static class view extends CommandRef {
                    public static final view cmd = new view();
                public view source(String value) {
                    return set("source", value);
                }

                public view getAnswers(String value) {
                    return set("getAnswers", value);
                }

                public view sheet(String value) {
                    return set("sheet", value);
                }

                }
            }
            public static class providers{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.GPTCommands.class,method="chatProviderConfigure")
                public static class configure extends CommandRef {
                    public static final configure cmd = new configure();
                public configure provider(String value) {
                    return set("provider", value);
                }

                public configure options(String value) {
                    return set("options", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.GPTCommands.class,method="listChatProviders")
                public static class list extends CommandRef {
                    public static final list cmd = new list();

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.GPTCommands.class,method="chatPause")
                public static class pause extends CommandRef {
                    public static final pause cmd = new pause();
                public pause provider(String value) {
                    return set("provider", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.GPTCommands.class,method="chatResume")
                public static class resume extends CommandRef {
                    public static final resume cmd = new resume();
                public resume provider(String value) {
                    return set("provider", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.GPTCommands.class,method="setChatProviders")
                public static class set extends CommandRef {
                    public static final set cmd = new set();
                public set providerTypes(String value) {
                    return set("providerTypes", value);
                }

                }
            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.GPTCommands.class,method="unban")
            public static class unban extends CommandRef {
                public static final unban cmd = new unban();
            public unban nation(String value) {
                return set("nation", value);
            }

            public unban force(String value) {
                return set("force", value);
            }

            }
        }
        public static class coalition{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.FACommands.class,method="addCoalition")
            public static class add extends CommandRef {
                public static final add cmd = new add();
            public add alliances(String value) {
                return set("alliances", value);
            }

            public add coalitionName(String value) {
                return set("coalitionName", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.FACommands.class,method="createCoalition")
            public static class create extends CommandRef {
                public static final create cmd = new create();
            public create alliances(String value) {
                return set("alliances", value);
            }

            public create coalitionName(String value) {
                return set("coalitionName", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.FACommands.class,method="deleteCoalition")
            public static class delete extends CommandRef {
                public static final delete cmd = new delete();
            public delete coalitionName(String value) {
                return set("coalitionName", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.FACommands.class,method="generateSphere")
            public static class generate extends CommandRef {
                public static final generate cmd = new generate();
            public generate coalition(String value) {
                return set("coalition", value);
            }

            public generate rootAlliance(String value) {
                return set("rootAlliance", value);
            }

            public generate topX(String value) {
                return set("topX", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.FACommands.class,method="listCoalition")
            public static class list extends CommandRef {
                public static final list cmd = new list();
            public list filter(String value) {
                return set("filter", value);
            }

            public list listIds(String value) {
                return set("listIds", value);
            }

            public list ignoreDeleted(String value) {
                return set("ignoreDeleted", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.FACommands.class,method="removeCoalition")
            public static class remove extends CommandRef {
                public static final remove cmd = new remove();
            public remove alliances(String value) {
                return set("alliances", value);
            }

            public remove coalitionName(String value) {
                return set("coalitionName", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.FACommands.class,method="generateCoalitionSheet")
            public static class sheet extends CommandRef {
                public static final sheet cmd = new sheet();
            public sheet sheet(String value) {
                return set("sheet", value);
            }

            }
        }
        public static class conflict{
            public static class alliance{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ConflictCommands.class,method="addCoalition")
                public static class add extends CommandRef {
                    public static final add cmd = new add();
                public add conflict(String value) {
                    return set("conflict", value);
                }

                public add alliances(String value) {
                    return set("alliances", value);
                }

                public add isCoalition1(String value) {
                    return set("isCoalition1", value);
                }

                public add isCoalition2(String value) {
                    return set("isCoalition2", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ConflictCommands.class,method="removeCoalition")
                public static class remove extends CommandRef {
                    public static final remove cmd = new remove();
                public remove conflict(String value) {
                    return set("conflict", value);
                }

                public remove alliances(String value) {
                    return set("alliances", value);
                }

                }
            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ConflictCommands.class,method="addConflict")
            public static class create extends CommandRef {
                public static final create cmd = new create();
            public create category(String value) {
                return set("category", value);
            }

            public create coalition1(String value) {
                return set("coalition1", value);
            }

            public create coalition2(String value) {
                return set("coalition2", value);
            }

            public create start(String value) {
                return set("start", value);
            }

            public create end(String value) {
                return set("end", value);
            }

            public create conflictName(String value) {
                return set("conflictName", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.VirtualConflictCommands.class,method="createTemporary")
            public static class create_temp extends CommandRef {
                public static final create_temp cmd = new create_temp();
            public create_temp col1(String value) {
                return set("col1", value);
            }

            public create_temp col2(String value) {
                return set("col2", value);
            }

            public create_temp start(String value) {
                return set("start", value);
            }

            public create_temp end(String value) {
                return set("end", value);
            }

            public create_temp includeGraphs(String value) {
                return set("includeGraphs", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ConflictCommands.class,method="deleteConflict")
            public static class delete extends CommandRef {
                public static final delete cmd = new delete();
            public delete conflict(String value) {
                return set("conflict", value);
            }

            public delete force(String value) {
                return set("force", value);
            }

            }
            public static class edit{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ConflictCommands.class,method="setCB")
                public static class casus_belli extends CommandRef {
                    public static final casus_belli cmd = new casus_belli();
                public casus_belli conflict(String value) {
                    return set("conflict", value);
                }

                public casus_belli casus_belli(String value) {
                    return set("casus_belli", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ConflictCommands.class,method="setCategory")
                public static class category extends CommandRef {
                    public static final category cmd = new category();
                public category conflict(String value) {
                    return set("conflict", value);
                }

                public category category(String value) {
                    return set("category", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ConflictCommands.class,method="setConflictEnd")
                public static class end extends CommandRef {
                    public static final end cmd = new end();
                public end conflict(String value) {
                    return set("conflict", value);
                }

                public end time(String value) {
                    return set("time", value);
                }

                public end alliance(String value) {
                    return set("alliance", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ConflictCommands.class,method="setConflictName")
                public static class rename extends CommandRef {
                    public static final rename cmd = new rename();
                public rename conflict(String value) {
                    return set("conflict", value);
                }

                public rename name(String value) {
                    return set("name", value);
                }

                public rename isCoalition1(String value) {
                    return set("isCoalition1", value);
                }

                public rename isCoalition2(String value) {
                    return set("isCoalition2", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ConflictCommands.class,method="setConflictStart")
                public static class start extends CommandRef {
                    public static final start cmd = new start();
                public start conflict(String value) {
                    return set("conflict", value);
                }

                public start time(String value) {
                    return set("time", value);
                }

                public start alliance(String value) {
                    return set("alliance", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ConflictCommands.class,method="setStatus")
                public static class status extends CommandRef {
                    public static final status cmd = new status();
                public status conflict(String value) {
                    return set("conflict", value);
                }

                public status status(String value) {
                    return set("status", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ConflictCommands.class,method="setWiki")
                public static class wiki extends CommandRef {
                    public static final wiki cmd = new wiki();
                public wiki conflict(String value) {
                    return set("conflict", value);
                }

                public wiki url(String value) {
                    return set("url", value);
                }

                }
            }
            public static class featured{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ConflictCommands.class,method="featureConflicts")
                public static class add_rule extends CommandRef {
                    public static final add_rule cmd = new add_rule();
                public add_rule conflicts(String value) {
                    return set("conflicts", value);
                }

                public add_rule guild(String value) {
                    return set("guild", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ConflictCommands.class,method="listFeaturedRuleset")
                public static class list_rules extends CommandRef {
                    public static final list_rules cmd = new list_rules();

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ConflictCommands.class,method="removeFeature")
                public static class remove_rule extends CommandRef {
                    public static final remove_rule cmd = new remove_rule();
                public remove_rule conflicts(String value) {
                    return set("conflicts", value);
                }

                public remove_rule guild(String value) {
                    return set("guild", value);
                }

                }
            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ConflictCommands.class,method="info")
            public static class info extends CommandRef {
                public static final info cmd = new info();
            public info conflict(String value) {
                return set("conflict", value);
            }

            public info showParticipants(String value) {
                return set("showParticipants", value);
            }

            public info hideDeleted(String value) {
                return set("hideDeleted", value);
            }

            public info showIds(String value) {
                return set("showIds", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ConflictCommands.class,method="listConflicts")
            public static class list extends CommandRef {
                public static final list cmd = new list();
            public list includeInactive(String value) {
                return set("includeInactive", value);
            }

            }
            public static class purge{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ConflictCommands.class,method="purgeFeatured")
                public static class featured extends CommandRef {
                    public static final featured cmd = new featured();
                public featured olderThan(String value) {
                    return set("olderThan", value);
                }

                public featured force(String value) {
                    return set("force", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ConflictCommands.class,method="purgeTemporaryConflicts")
                public static class user_generated extends CommandRef {
                    public static final user_generated cmd = new user_generated();
                public user_generated olderThan(String value) {
                    return set("olderThan", value);
                }

                public user_generated force(String value) {
                    return set("force", value);
                }

                }
            }
            public static class sync{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ConflictCommands.class,method="importExternal")
                public static class db_file extends CommandRef {
                    public static final db_file cmd = new db_file();
                public db_file fileLocation(String value) {
                    return set("fileLocation", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ConflictCommands.class,method="recalculateGraphs")
                public static class recalculate_graphs extends CommandRef {
                    public static final recalculate_graphs cmd = new recalculate_graphs();
                public recalculate_graphs conflicts(String value) {
                    return set("conflicts", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ConflictCommands.class,method="recalculateTables")
                public static class recalculate_tables extends CommandRef {
                    public static final recalculate_tables cmd = new recalculate_tables();
                public recalculate_tables conflicts(String value) {
                    return set("conflicts", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ConflictCommands.class,method="syncConflictData")
                public static class website extends CommandRef {
                    public static final website cmd = new website();
                public website conflicts(String value) {
                    return set("conflicts", value);
                }

                public website reinitialize_wars(String value) {
                    return set("reinitialize_wars", value);
                }

                public website reinitialize_graphs(String value) {
                    return set("reinitialize_graphs", value);
                }

                }
            }
        }
        @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UnsortedCommands.class,method="copyPasta")
        public static class copyPasta extends CommandRef {
            public static final copyPasta cmd = new copyPasta();
        public copyPasta key(String value) {
            return set("key", value);
        }

        public copyPasta message(String value) {
            return set("message", value);
        }

        public copyPasta requiredRolesAny(String value) {
            return set("requiredRolesAny", value);
        }

        public copyPasta formatNation(String value) {
            return set("formatNation", value);
        }

        }
        public static class credentials{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UnsortedCommands.class,method="addApiKey")
            public static class addApiKey extends CommandRef {
                public static final addApiKey cmd = new addApiKey();
            public addApiKey nation(String value) {
                return set("nation", value);
            }

            public addApiKey apiKey(String value) {
                return set("apiKey", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UnsortedCommands.class,method="login")
            public static class login extends CommandRef {
                public static final login cmd = new login();
            public login username(String value) {
                return set("username", value);
            }

            public login password(String value) {
                return set("password", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UnsortedCommands.class,method="logout")
            public static class logout extends CommandRef {
                public static final logout cmd = new logout();

            }
        }
        public static class deposits{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UnsortedCommands.class,method="addBalance")
            public static class add extends CommandRef {
                public static final add cmd = new add();
            public add accounts(String value) {
                return set("accounts", value);
            }

            public add amount(String value) {
                return set("amount", value);
            }

            public add note(String value) {
                return set("note", value);
            }

            public add force(String value) {
                return set("force", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UnsortedCommands.class,method="addBalanceSheet")
            public static class addSheet extends CommandRef {
                public static final addSheet cmd = new addSheet();
            public addSheet sheet(String value) {
                return set("sheet", value);
            }

            public addSheet note(String value) {
                return set("note", value);
            }

            public addSheet force(String value) {
                return set("force", value);
            }

            public addSheet negative(String value) {
                return set("negative", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.BankCommands.class,method="deposits")
            public static class check extends CommandRef {
                public static final check cmd = new check();
            public check nationOrAllianceOrGuild(String value) {
                return set("nationOrAllianceOrGuild", value);
            }

            public check offshores(String value) {
                return set("offshores", value);
            }

            public check timeCutoff(String value) {
                return set("timeCutoff", value);
            }

            public check ignoreInternalOffsets(String value) {
                return set("ignoreInternalOffsets", value);
            }

            public check showCategories(String value) {
                return set("showCategories", value);
            }

            public check replyInDMs(String value) {
                return set("replyInDMs", value);
            }

            public check includeExpired(String value) {
                return set("includeExpired", value);
            }

            public check includeIgnored(String value) {
                return set("includeIgnored", value);
            }

            public check allowCheckDeleted(String value) {
                return set("allowCheckDeleted", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.TestCommands.class,method="viewFlow")
            public static class flows extends CommandRef {
                public static final flows cmd = new flows();
            public flows nation(String value) {
                return set("nation", value);
            }

            public flows note(String value) {
                return set("note", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UtilityCommands.class,method="interest")
            public static class interest extends CommandRef {
                public static final interest cmd = new interest();
            public interest nations(String value) {
                return set("nations", value);
            }

            public interest interestPositivePercent(String value) {
                return set("interestPositivePercent", value);
            }

            public interest interestNegativePercent(String value) {
                return set("interestNegativePercent", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.BankCommands.class,method="resetDeposits")
            public static class reset extends CommandRef {
                public static final reset cmd = new reset();
            public reset nations(String value) {
                return set("nations", value);
            }

            public reset ignoreGrants(String value) {
                return set("ignoreGrants", value);
            }

            public reset ignoreLoans(String value) {
                return set("ignoreLoans", value);
            }

            public reset ignoreTaxes(String value) {
                return set("ignoreTaxes", value);
            }

            public reset ignoreBankDeposits(String value) {
                return set("ignoreBankDeposits", value);
            }

            public reset force(String value) {
                return set("force", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.BankCommands.class,method="depositSheet")
            public static class sheet extends CommandRef {
                public static final sheet cmd = new sheet();
            public sheet nations(String value) {
                return set("nations", value);
            }

            public sheet offshores(String value) {
                return set("offshores", value);
            }

            public sheet ignoreOffsets(String value) {
                return set("ignoreOffsets", value);
            }

            public sheet includeExpired(String value) {
                return set("includeExpired", value);
            }

            public sheet includeIgnored(String value) {
                return set("includeIgnored", value);
            }

            public sheet noTaxes(String value) {
                return set("noTaxes", value);
            }

            public sheet noLoans(String value) {
                return set("noLoans", value);
            }

            public sheet noGrants(String value) {
                return set("noGrants", value);
            }

            public sheet noDeposits(String value) {
                return set("noDeposits", value);
            }

            public sheet includePastDepositors(String value) {
                return set("includePastDepositors", value);
            }

            public sheet useFlowNote(String value) {
                return set("useFlowNote", value);
            }

            public sheet force(String value) {
                return set("force", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.BankCommands.class,method="shiftDeposits")
            public static class shift extends CommandRef {
                public static final shift cmd = new shift();
            public shift nation(String value) {
                return set("nation", value);
            }

            public shift from(String value) {
                return set("from", value);
            }

            public shift to(String value) {
                return set("to", value);
            }

            public shift expireTime(String value) {
                return set("expireTime", value);
            }

            public shift decayTime(String value) {
                return set("decayTime", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.TestCommands.class,method="shiftFlow")
            public static class shiftFlow extends CommandRef {
                public static final shiftFlow cmd = new shiftFlow();
            public shiftFlow nation(String value) {
                return set("nation", value);
            }

            public shiftFlow noteFrom(String value) {
                return set("noteFrom", value);
            }

            public shiftFlow flowType(String value) {
                return set("flowType", value);
            }

            public shiftFlow amount(String value) {
                return set("amount", value);
            }

            public shiftFlow noteTo(String value) {
                return set("noteTo", value);
            }

            public shiftFlow alliance(String value) {
                return set("alliance", value);
            }

            public shiftFlow force(String value) {
                return set("force", value);
            }

            }
        }
        public static class development{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.BuildCommands.class,method="devCost")
            public static class cost extends CommandRef {
                public static final cost cmd = new cost();
            public cost buy_up_to(String value) {
                return set("buy_up_to", value);
            }

            public cost nation(String value) {
                return set("nation", value);
            }

            public cost current_dev(String value) {
                return set("current_dev", value);
            }

            public cost current_land(String value) {
                return set("current_land", value);
            }

            public cost dev_cost_reduction(String value) {
                return set("dev_cost_reduction", value);
            }

            public cost force_update(String value) {
                return set("force_update", value);
            }

            }
        }
        @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.FACommands.class,method="embassy")
        public static class embassy extends CommandRef {
            public static final embassy cmd = new embassy();
        public embassy nation(String value) {
            return set("nation", value);
        }

        }
        public static class embed{
            public static class add{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.EmbedCommands.class,method="addButton")
                public static class command extends CommandRef {
                    public static final command cmd = new command();
                public command message(String value) {
                    return set("message", value);
                }

                public command label(String value) {
                    return set("label", value);
                }

                public command behavior(String value) {
                    return set("behavior", value);
                }

                public command command(String value) {
                    return set("command", value);
                }

                public command arguments(String value) {
                    return set("arguments", value);
                }

                public command channel(String value) {
                    return set("channel", value);
                }

                public command force(String value) {
                    return set("force", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.EmbedCommands.class,method="addModal")
                public static class modal extends CommandRef {
                    public static final modal cmd = new modal();
                public modal message(String value) {
                    return set("message", value);
                }

                public modal label(String value) {
                    return set("label", value);
                }

                public modal behavior(String value) {
                    return set("behavior", value);
                }

                public modal command(String value) {
                    return set("command", value);
                }

                public modal arguments(String value) {
                    return set("arguments", value);
                }

                public modal defaults(String value) {
                    return set("defaults", value);
                }

                public modal channel(String value) {
                    return set("channel", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.EmbedCommands.class,method="addButtonRaw")
                public static class raw extends CommandRef {
                    public static final raw cmd = new raw();
                public raw message(String value) {
                    return set("message", value);
                }

                public raw label(String value) {
                    return set("label", value);
                }

                public raw behavior(String value) {
                    return set("behavior", value);
                }

                public raw command(String value) {
                    return set("command", value);
                }

                public raw channel(String value) {
                    return set("channel", value);
                }

                public raw force(String value) {
                    return set("force", value);
                }

                }
            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.DiscordCommands.class,method="card")
            public static class commands extends CommandRef {
                public static final commands cmd = new commands();
            public commands title(String value) {
                return set("title", value);
            }

            public commands body(String value) {
                return set("body", value);
            }

            public commands commands(String value) {
                return set("commands", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.EmbedCommands.class,method="create")
            public static class create extends CommandRef {
                public static final create cmd = new create();
            public create title(String value) {
                return set("title", value);
            }

            public create description(String value) {
                return set("description", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.EmbedCommands.class,method="description")
            public static class description extends CommandRef {
                public static final description cmd = new description();
            public description discMessage(String value) {
                return set("discMessage", value);
            }

            public description description(String value) {
                return set("description", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.DiscordCommands.class,method="embedInfo")
            public static class info extends CommandRef {
                public static final info cmd = new info();
            public info embedMessage(String value) {
                return set("embedMessage", value);
            }

            public info copyToMessage(String value) {
                return set("copyToMessage", value);
            }

            }
            public static class remove{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.EmbedCommands.class,method="removeButton")
                public static class button extends CommandRef {
                    public static final button cmd = new button();
                public button message(String value) {
                    return set("message", value);
                }

                public button labels(String value) {
                    return set("labels", value);
                }

                }
            }
            public static class rename{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.EmbedCommands.class,method="renameButton")
                public static class button extends CommandRef {
                    public static final button cmd = new button();
                public button message(String value) {
                    return set("message", value);
                }

                public button label(String value) {
                    return set("label", value);
                }

                public button rename_to(String value) {
                    return set("rename_to", value);
                }

                }
            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.EmbedCommands.class,method="title")
            public static class title extends CommandRef {
                public static final title cmd = new title();
            public title discMessage(String value) {
                return set("discMessage", value);
            }

            public title title(String value) {
                return set("title", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.DiscordCommands.class,method="updateEmbed")
            public static class update extends CommandRef {
                public static final update cmd = new update();
            public update requiredRole(String value) {
                return set("requiredRole", value);
            }

            public update color(String value) {
                return set("color", value);
            }

            public update title(String value) {
                return set("title", value);
            }

            public update desc(String value) {
                return set("desc", value);
            }

            }
        }
        public static class fun{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.FunCommands.class,method="borg")
            public static class borg extends CommandRef {
                public static final borg cmd = new borg();
            public borg msg(String value) {
                return set("msg", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.FunCommands.class,method="joke")
            public static class joke extends CommandRef {
                public static final joke cmd = new joke();

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.DiscordCommands.class,method="say")
            public static class say extends CommandRef {
                public static final say cmd = new say();
            public say msg(String value) {
                return set("msg", value);
            }

            }
        }
        public static class help{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.HelpCommands.class,method="argument")
            public static class argument extends CommandRef {
                public static final argument cmd = new argument();
            public argument argument(String value) {
                return set("argument", value);
            }

            public argument skipOptionalArgs(String value) {
                return set("skipOptionalArgs", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.HelpCommands.class,method="command")
            public static class command extends CommandRef {
                public static final command cmd = new command();
            public command command(String value) {
                return set("command", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.GPTCommands.class,method="find_argument")
            public static class find_argument extends CommandRef {
                public static final find_argument cmd = new find_argument();
            public find_argument search(String value) {
                return set("search", value);
            }

            public find_argument instructions(String value) {
                return set("instructions", value);
            }

            public find_argument useGPT(String value) {
                return set("useGPT", value);
            }

            public find_argument numResults(String value) {
                return set("numResults", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.GPTCommands.class,method="find_command2")
            public static class find_command extends CommandRef {
                public static final find_command cmd = new find_command();
            public find_command search(String value) {
                return set("search", value);
            }

            public find_command instructions(String value) {
                return set("instructions", value);
            }

            public find_command useGPT(String value) {
                return set("useGPT", value);
            }

            public find_command numResults(String value) {
                return set("numResults", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.GPTCommands.class,method="find_placeholder")
            public static class find_nation_placeholder extends CommandRef {
                public static final find_nation_placeholder cmd = new find_nation_placeholder();
            public find_nation_placeholder search(String value) {
                return set("search", value);
            }

            public find_nation_placeholder instructions(String value) {
                return set("instructions", value);
            }

            public find_nation_placeholder useGPT(String value) {
                return set("useGPT", value);
            }

            public find_nation_placeholder numResults(String value) {
                return set("numResults", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.HelpCommands.class,method="find_setting")
            public static class find_setting extends CommandRef {
                public static final find_setting cmd = new find_setting();
            public find_setting query(String value) {
                return set("query", value);
            }

            public find_setting num_results(String value) {
                return set("num_results", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.HelpCommands.class,method="moderation_check")
            public static class moderation_check extends CommandRef {
                public static final moderation_check cmd = new moderation_check();
            public moderation_check input(String value) {
                return set("input", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.HelpCommands.class,method="nation_placeholder")
            public static class nation_placeholder extends CommandRef {
                public static final nation_placeholder cmd = new nation_placeholder();
            public nation_placeholder command(String value) {
                return set("command", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.HelpCommands.class,method="query")
            public static class query extends CommandRef {
                public static final query cmd = new query();
            public query input(String value) {
                return set("input", value);
            }

            }
        }
        public static class interview{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="adRanking")
            public static class adRanking extends CommandRef {
                public static final adRanking cmd = new adRanking();
            public adRanking uploadFile(String value) {
                return set("uploadFile", value);
            }

            }
            public static class channel{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="renameInterviewChannels")
                public static class auto_rename extends CommandRef {
                    public static final auto_rename cmd = new auto_rename();
                public auto_rename categories(String value) {
                    return set("categories", value);
                }

                public auto_rename allow_non_members(String value) {
                    return set("allow_non_members", value);
                }

                public auto_rename allow_vm(String value) {
                    return set("allow_vm", value);
                }

                public auto_rename list_missing(String value) {
                    return set("list_missing", value);
                }

                public auto_rename force(String value) {
                    return set("force", value);
                }

                }
            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="interview")
            public static class create extends CommandRef {
                public static final create cmd = new create();
            public create user(String value) {
                return set("user", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="iaCat")
            public static class iacat extends CommandRef {
                public static final iacat cmd = new iacat();
            public iacat category(String value) {
                return set("category", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="iachannels")
            public static class iachannels extends CommandRef {
                public static final iachannels cmd = new iachannels();
            public iachannels filter(String value) {
                return set("filter", value);
            }

            public iachannels time(String value) {
                return set("time", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="incentiveRanking")
            public static class incentiveRanking extends CommandRef {
                public static final incentiveRanking cmd = new incentiveRanking();
            public incentiveRanking timestamp(String value) {
                return set("timestamp", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.DiscordCommands.class,method="interviewMessage")
            public static class interviewMessage extends CommandRef {
                public static final interviewMessage cmd = new interviewMessage();
            public interviewMessage nations(String value) {
                return set("nations", value);
            }

            public interviewMessage message(String value) {
                return set("message", value);
            }

            public interviewMessage pingMentee(String value) {
                return set("pingMentee", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="listMentors")
            public static class listMentors extends CommandRef {
                public static final listMentors cmd = new listMentors();
            public listMentors mentors(String value) {
                return set("mentors", value);
            }

            public listMentors mentees(String value) {
                return set("mentees", value);
            }

            public listMentors timediff(String value) {
                return set("timediff", value);
            }

            public listMentors includeAudit(String value) {
                return set("includeAudit", value);
            }

            public listMentors ignoreUnallocatedMembers(String value) {
                return set("ignoreUnallocatedMembers", value);
            }

            public listMentors listIdleMentors(String value) {
                return set("listIdleMentors", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="mentee")
            public static class mentee extends CommandRef {
                public static final mentee cmd = new mentee();
            public mentee mentee(String value) {
                return set("mentee", value);
            }

            public mentee force(String value) {
                return set("force", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="mentor")
            public static class mentor extends CommandRef {
                public static final mentor cmd = new mentor();
            public mentor mentor(String value) {
                return set("mentor", value);
            }

            public mentor mentee(String value) {
                return set("mentee", value);
            }

            public mentor force(String value) {
                return set("force", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="myMentees")
            public static class mymentees extends CommandRef {
                public static final mymentees cmd = new mymentees();
            public mymentees mentees(String value) {
                return set("mentees", value);
            }

            public mymentees timediff(String value) {
                return set("timediff", value);
            }

            }
            public static class questions{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="setInterview")
                public static class set extends CommandRef {
                    public static final set cmd = new set();
                public set message(String value) {
                    return set("message", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="viewInterview")
                public static class view extends CommandRef {
                    public static final view cmd = new view();

                }
            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UtilityCommands.class,method="recruitmentRankings")
            public static class recruitmentRankings extends CommandRef {
                public static final recruitmentRankings cmd = new recruitmentRankings();
            public recruitmentRankings cutoff(String value) {
                return set("cutoff", value);
            }

            public recruitmentRankings topX(String value) {
                return set("topX", value);
            }

            public recruitmentRankings uploadFile(String value) {
                return set("uploadFile", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="setReferrer")
            public static class setReferrer extends CommandRef {
                public static final setReferrer cmd = new setReferrer();
            public setReferrer user(String value) {
                return set("user", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="sortInterviews")
            public static class sortInterviews extends CommandRef {
                public static final sortInterviews cmd = new sortInterviews();
            public sortInterviews sortCategorized(String value) {
                return set("sortCategorized", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="syncInterviews")
            public static class syncInterviews extends CommandRef {
                public static final syncInterviews cmd = new syncInterviews();

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="unassignMentee")
            public static class unassignMentee extends CommandRef {
                public static final unassignMentee cmd = new unassignMentee();
            public unassignMentee mentee(String value) {
                return set("mentee", value);
            }

            }
        }
        @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.DiscordCommands.class,method="invite")
        public static class invite extends CommandRef {
            public static final invite cmd = new invite();

        }
        public static class land{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.BuildCommands.class,method="landCost")
            public static class cost extends CommandRef {
                public static final cost cmd = new cost();
            public cost buy_up_to(String value) {
                return set("buy_up_to", value);
            }

            public cost nation(String value) {
                return set("nation", value);
            }

            public cost current_land(String value) {
                return set("current_land", value);
            }

            public cost land_cost_reduction(String value) {
                return set("land_cost_reduction", value);
            }

            public cost force_update(String value) {
                return set("force_update", value);
            }

            }
        }
        @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UtilityCommands.class,method="me")
        public static class me extends CommandRef {
            public static final me cmd = new me();
        public me snapshotDate(String value) {
            return set("snapshotDate", value);
        }

        }
        public static class message{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="sendCommandOutput")
            public static class command extends CommandRef {
                public static final command cmd = new command();
            public command nations(String value) {
                return set("nations", value);
            }

            public command subject(String value) {
                return set("subject", value);
            }

            public command command(String value) {
                return set("command", value);
            }

            public command body(String value) {
                return set("body", value);
            }

            public command sheet(String value) {
                return set("sheet", value);
            }

            public command sendDM(String value) {
                return set("sendDM", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="dmSheet")
            public static class sheet extends CommandRef {
                public static final sheet cmd = new sheet();
            public sheet sheet(String value) {
                return set("sheet", value);
            }

            public sheet force(String value) {
                return set("force", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="sendTargets")
            public static class targets extends CommandRef {
                public static final targets cmd = new targets();
            public targets blitzSheet(String value) {
                return set("blitzSheet", value);
            }

            public targets spySheet(String value) {
                return set("spySheet", value);
            }

            public targets allowedNations(String value) {
                return set("allowedNations", value);
            }

            public targets allowedEnemies(String value) {
                return set("allowedEnemies", value);
            }

            public targets header(String value) {
                return set("header", value);
            }

            public targets hideDefaultBlurb(String value) {
                return set("hideDefaultBlurb", value);
            }

            public targets force(String value) {
                return set("force", value);
            }

            public targets useLeader(String value) {
                return set("useLeader", value);
            }

            }
        }
        public static class modal{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.TestCommands.class,method="modal")
            public static class create extends CommandRef {
                public static final create cmd = new create();
            public create command(String value) {
                return set("command", value);
            }

            public create arguments(String value) {
                return set("arguments", value);
            }

            public create defaults(String value) {
                return set("defaults", value);
            }

            }
        }
        public static class nation{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UnsortedCommands.class,method="leftAA")
            public static class departures extends CommandRef {
                public static final departures cmd = new departures();
            public departures nationOrAlliance(String value) {
                return set("nationOrAlliance", value);
            }

            public departures time(String value) {
                return set("time", value);
            }

            public departures filter(String value) {
                return set("filter", value);
            }

            public departures ignoreInactives(String value) {
                return set("ignoreInactives", value);
            }

            public departures ignoreVM(String value) {
                return set("ignoreVM", value);
            }

            public departures ignoreMembers(String value) {
                return set("ignoreMembers", value);
            }

            public departures listIds(String value) {
                return set("listIds", value);
            }

            public departures sheet(String value) {
                return set("sheet", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UtilityCommands.class,method="development")
            public static class development extends CommandRef {
                public static final development cmd = new development();
            public development nation(String value) {
                return set("nation", value);
            }

            public development update(String value) {
                return set("update", value);
            }

            public development full_numbers(String value) {
                return set("full_numbers", value);
            }

            }
            public static class list{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.BuildCommands.class,method="getBuild")
                public static class buildings extends CommandRef {
                    public static final buildings cmd = new buildings();
                public buildings nation(String value) {
                    return set("nation", value);
                }

                public buildings update(String value) {
                    return set("update", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="inactive")
                public static class inactive extends CommandRef {
                    public static final inactive cmd = new inactive();
                public inactive nations(String value) {
                    return set("nations", value);
                }

                public inactive days(String value) {
                    return set("days", value);
                }

                public inactive includeApplicants(String value) {
                    return set("includeApplicants", value);
                }

                public inactive includeVacationMode(String value) {
                    return set("includeVacationMode", value);
                }

                public inactive page(String value) {
                    return set("page", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.BuildCommands.class,method="viewProjects")
                public static class projects extends CommandRef {
                    public static final projects cmd = new projects();
                public projects nation(String value) {
                    return set("nation", value);
                }

                public projects force_update(String value) {
                    return set("force_update", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.BuildCommands.class,method="viewTechnologies")
                public static class tech extends CommandRef {
                    public static final tech cmd = new tech();
                public tech nation(String value) {
                    return set("nation", value);
                }

                public tech force_update(String value) {
                    return set("force_update", value);
                }

                }
            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UtilityCommands.class,method="protectionTime")
            public static class protectionTime extends CommandRef {
                public static final protectionTime cmd = new protectionTime();
            public protectionTime nation(String value) {
                return set("nation", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UnsortedCommands.class,method="reroll")
            public static class reroll extends CommandRef {
                public static final reroll cmd = new reroll();
            public reroll nation(String value) {
                return set("nation", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UnsortedCommands.class,method="revenue")
            public static class revenue extends CommandRef {
                public static final revenue cmd = new revenue();
            public revenue nations(String value) {
                return set("nations", value);
            }

            public revenue snapshotDate(String value) {
                return set("snapshotDate", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UtilityCommands.class,method="score")
            public static class score extends CommandRef {
                public static final score cmd = new score();
            public score nation(String value) {
                return set("nation", value);
            }

            public score EducationIndex(String value) {
                return set("EducationIndex", value);
            }

            public score PowerIndex(String value) {
                return set("PowerIndex", value);
            }

            public score EmploymentIndex(String value) {
                return set("EmploymentIndex", value);
            }

            public score TransportationIndex(String value) {
                return set("TransportationIndex", value);
            }

            public score StabilityIndex(String value) {
                return set("StabilityIndex", value);
            }

            public score CommerceIndex(String value) {
                return set("CommerceIndex", value);
            }

            public score Development(String value) {
                return set("Development", value);
            }

            public score Land(String value) {
                return set("Land", value);
            }

            public score Devastation(String value) {
                return set("Devastation", value);
            }

            public score WarIndex(String value) {
                return set("WarIndex", value);
            }

            public score TechIndex(String value) {
                return set("TechIndex", value);
            }

            public score UnfilledJobsPenality(String value) {
                return set("UnfilledJobsPenality", value);
            }

            }
            public static class sheet{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UtilityCommands.class,method="NationSheet")
                public static class NationSheet extends CommandRef {
                    public static final NationSheet cmd = new NationSheet();
                public NationSheet nations(String value) {
                    return set("nations", value);
                }

                public NationSheet columns(String value) {
                    return set("columns", value);
                }

                public NationSheet snapshotTime(String value) {
                    return set("snapshotTime", value);
                }

                public NationSheet sheet(String value) {
                    return set("sheet", value);
                }

                }
            }
            public static class stats{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.StatCommands.class,method="nationRanking")
                public static class ranking extends CommandRef {
                    public static final ranking cmd = new ranking();
                public ranking nations(String value) {
                    return set("nations", value);
                }

                public ranking attribute(String value) {
                    return set("attribute", value);
                }

                public ranking groupByAlliance(String value) {
                    return set("groupByAlliance", value);
                }

                public ranking reverseOrder(String value) {
                    return set("reverseOrder", value);
                }

                public ranking snapshotDate(String value) {
                    return set("snapshotDate", value);
                }

                public ranking total(String value) {
                    return set("total", value);
                }

                }
            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UnsortedCommands.class,method="stockpile")
            public static class stockpile extends CommandRef {
                public static final stockpile cmd = new stockpile();
            public stockpile nationOrAlliance(String value) {
                return set("nationOrAlliance", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="wars")
            public static class wars extends CommandRef {
                public static final wars cmd = new wars();
            public wars nation(String value) {
                return set("nation", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UtilityCommands.class,method="who")
            public static class who extends CommandRef {
                public static final who cmd = new who();
            public who nationOrAlliances(String value) {
                return set("nationOrAlliances", value);
            }

            public who sortBy(String value) {
                return set("sortBy", value);
            }

            public who list(String value) {
                return set("list", value);
            }

            public who listAlliances(String value) {
                return set("listAlliances", value);
            }

            public who listRawUserIds(String value) {
                return set("listRawUserIds", value);
            }

            public who listMentions(String value) {
                return set("listMentions", value);
            }

            public who listInfo(String value) {
                return set("listInfo", value);
            }

            public who listChannels(String value) {
                return set("listChannels", value);
            }

            public who snapshotDate(String value) {
                return set("snapshotDate", value);
            }

            public who page(String value) {
                return set("page", value);
            }

            }
        }
        public static class project{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.BuildCommands.class,method="projectCost")
            public static class cost extends CommandRef {
                public static final cost cmd = new cost();
            public cost project(String value) {
                return set("project", value);
            }

            public cost nation(String value) {
                return set("nation", value);
            }

            public cost start_amount(String value) {
                return set("start_amount", value);
            }

            public cost end_amount(String value) {
                return set("end_amount", value);
            }

            public cost development(String value) {
                return set("development", value);
            }

            public cost land(String value) {
                return set("land", value);
            }

            public cost projectCostReduction(String value) {
                return set("projectCostReduction", value);
            }

            public cost force_update(String value) {
                return set("force_update", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.BankCommands.class,method="ProjectSheet")
            public static class sheet extends CommandRef {
                public static final sheet cmd = new sheet();
            public sheet nations(String value) {
                return set("nations", value);
            }

            public sheet sheet(String value) {
                return set("sheet", value);
            }

            public sheet snapshotTime(String value) {
                return set("snapshotTime", value);
            }

            }
        }
        @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.DiscordCommands.class,method="register")
        public static class register extends CommandRef {
            public static final register cmd = new register();
        public register nation(String value) {
            return set("nation", value);
        }

        public register user(String value) {
            return set("user", value);
        }

        }
        public static class report{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ReportCommands.class,method="createReport")
            public static class add extends CommandRef {
                public static final add cmd = new add();
            public add type(String value) {
                return set("type", value);
            }

            public add message(String value) {
                return set("message", value);
            }

            public add nation(String value) {
                return set("nation", value);
            }

            public add discord_user_id(String value) {
                return set("discord_user_id", value);
            }

            public add imageEvidenceUrl(String value) {
                return set("imageEvidenceUrl", value);
            }

            public add news_post(String value) {
                return set("news_post", value);
            }

            public add updateReport(String value) {
                return set("updateReport", value);
            }

            public add force(String value) {
                return set("force", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ReportCommands.class,method="riskFactors")
            public static class analyze extends CommandRef {
                public static final analyze cmd = new analyze();
            public analyze nation(String value) {
                return set("nation", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ReportCommands.class,method="approveReport")
            public static class approve extends CommandRef {
                public static final approve cmd = new approve();
            public approve report(String value) {
                return set("report", value);
            }

            public approve force(String value) {
                return set("force", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ReportCommands.class,method="ban")
            public static class ban extends CommandRef {
                public static final ban cmd = new ban();
            public ban nation(String value) {
                return set("nation", value);
            }

            public ban timestamp(String value) {
                return set("timestamp", value);
            }

            public ban reason(String value) {
                return set("reason", value);
            }

            public ban force(String value) {
                return set("force", value);
            }

            }
            public static class comment{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ReportCommands.class,method="comment")
                public static class add extends CommandRef {
                    public static final add cmd = new add();
                public add report(String value) {
                    return set("report", value);
                }

                public add comment(String value) {
                    return set("comment", value);
                }

                public add force(String value) {
                    return set("force", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ReportCommands.class,method="removeComment")
                public static class delete extends CommandRef {
                    public static final delete cmd = new delete();
                public delete report(String value) {
                    return set("report", value);
                }

                public delete nationCommenting(String value) {
                    return set("nationCommenting", value);
                }

                public delete force(String value) {
                    return set("force", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ReportCommands.class,method="purgeComments")
                public static class purge extends CommandRef {
                    public static final purge cmd = new purge();
                public purge report(String value) {
                    return set("report", value);
                }

                public purge nation_id(String value) {
                    return set("nation_id", value);
                }

                public purge discord_id(String value) {
                    return set("discord_id", value);
                }

                public purge force(String value) {
                    return set("force", value);
                }

                }
            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ReportCommands.class,method="purgeReports")
            public static class purge extends CommandRef {
                public static final purge cmd = new purge();
            public purge nationIdReported(String value) {
                return set("nationIdReported", value);
            }

            public purge userIdReported(String value) {
                return set("userIdReported", value);
            }

            public purge reportingNation(String value) {
                return set("reportingNation", value);
            }

            public purge reportingUser(String value) {
                return set("reportingUser", value);
            }

            public purge force(String value) {
                return set("force", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ReportCommands.class,method="removeReport")
            public static class remove extends CommandRef {
                public static final remove cmd = new remove();
            public remove report(String value) {
                return set("report", value);
            }

            public remove force(String value) {
                return set("force", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ReportCommands.class,method="searchReports")
            public static class search extends CommandRef {
                public static final search cmd = new search();
            public search nationIdReported(String value) {
                return set("nationIdReported", value);
            }

            public search userIdReported(String value) {
                return set("userIdReported", value);
            }

            public search reportingNation(String value) {
                return set("reportingNation", value);
            }

            public search reportingUser(String value) {
                return set("reportingUser", value);
            }

            }
            public static class sheet{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ReportCommands.class,method="reportSheet")
                public static class generate extends CommandRef {
                    public static final generate cmd = new generate();
                public generate sheet(String value) {
                    return set("sheet", value);
                }

                }
            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ReportCommands.class,method="showReport")
            public static class show extends CommandRef {
                public static final show cmd = new show();
            public show report(String value) {
                return set("report", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ReportCommands.class,method="unban")
            public static class unban extends CommandRef {
                public static final unban cmd = new unban();
            public unban nation(String value) {
                return set("nation", value);
            }

            public unban force(String value) {
                return set("force", value);
            }

            }
            public static class upload{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.ReportCommands.class,method="importLegacyBlacklist")
                public static class legacy_reports extends CommandRef {
                    public static final legacy_reports cmd = new legacy_reports();
                public legacy_reports sheet(String value) {
                    return set("sheet", value);
                }

                }
            }
        }
        public static class role{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="addRoleToAllMembers")
            public static class addRoleToAllMembers extends CommandRef {
                public static final addRoleToAllMembers cmd = new addRoleToAllMembers();
            public addRoleToAllMembers role(String value) {
                return set("role", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UtilityCommands.class,method="autoroleall")
            public static class autoassign extends CommandRef {
                public static final autoassign cmd = new autoassign();
            public autoassign force(String value) {
                return set("force", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UtilityCommands.class,method="autorole")
            public static class autorole extends CommandRef {
                public static final autorole cmd = new autorole();
            public autorole member(String value) {
                return set("member", value);
            }

            public autorole force(String value) {
                return set("force", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UnsortedCommands.class,method="clearAllianceRoles")
            public static class clearAllianceRoles extends CommandRef {
                public static final clearAllianceRoles cmd = new clearAllianceRoles();
            public clearAllianceRoles type(String value) {
                return set("type", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UnsortedCommands.class,method="clearNicks")
            public static class clearNicks extends CommandRef {
                public static final clearNicks cmd = new clearNicks();
            public clearNicks undo(String value) {
                return set("undo", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.DiscordCommands.class,method="hasRole")
            public static class hasRole extends CommandRef {
                public static final hasRole cmd = new hasRole();
            public hasRole user(String value) {
                return set("user", value);
            }

            public hasRole role(String value) {
                return set("role", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="mask")
            public static class mask extends CommandRef {
                public static final mask cmd = new mask();
            public mask members(String value) {
                return set("members", value);
            }

            public mask role(String value) {
                return set("role", value);
            }

            public mask value(String value) {
                return set("value", value);
            }

            public mask toggleMaskFromOthers(String value) {
                return set("toggleMaskFromOthers", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="maskSheet")
            public static class mask_sheet extends CommandRef {
                public static final mask_sheet cmd = new mask_sheet();
            public mask_sheet sheet(String value) {
                return set("sheet", value);
            }

            public mask_sheet removeRoles(String value) {
                return set("removeRoles", value);
            }

            public mask_sheet removeAll(String value) {
                return set("removeAll", value);
            }

            public mask_sheet listMissing(String value) {
                return set("listMissing", value);
            }

            public mask_sheet force(String value) {
                return set("force", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.PlayerSettingCommands.class,method="optOut")
            public static class optOut extends CommandRef {
                public static final optOut cmd = new optOut();
            public optOut optOut(String value) {
                return set("optOut", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="removeAssignableRole")
            public static class removeAssignableRole extends CommandRef {
                public static final removeAssignableRole cmd = new removeAssignableRole();
            public removeAssignableRole requireRole(String value) {
                return set("requireRole", value);
            }

            public removeAssignableRole assignableRoles(String value) {
                return set("assignableRoles", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="aliasRole")
            public static class setAlias extends CommandRef {
                public static final setAlias cmd = new setAlias();
            public setAlias locutusRole(String value) {
                return set("locutusRole", value);
            }

            public setAlias discordRole(String value) {
                return set("discordRole", value);
            }

            public setAlias alliance(String value) {
                return set("alliance", value);
            }

            public setAlias removeRole(String value) {
                return set("removeRole", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="unregisterRole")
            public static class unregister extends CommandRef {
                public static final unregister cmd = new unregister();
            public unregister locutusRole(String value) {
                return set("locutusRole", value);
            }

            public unregister alliance(String value) {
                return set("alliance", value);
            }

            }
        }
        public static class selection_alias{
            public static class add{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addSelectionAlias", field="ALLIANCES")
                public static class alliance extends CommandRef {
                    public static final alliance cmd = new alliance();
                public alliance name(String value) {
                    return set("name", value);
                }

                public alliance alliances(String value) {
                    return set("alliances", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addSelectionAlias", field="AUDIT_TYPES")
                public static class audittype extends CommandRef {
                    public static final audittype cmd = new audittype();
                public audittype name(String value) {
                    return set("name", value);
                }

                public audittype audit_types(String value) {
                    return set("audit_types", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addSelectionAlias", field="BUILDINGS")
                public static class building extends CommandRef {
                    public static final building cmd = new building();
                public building name(String value) {
                    return set("name", value);
                }

                public building Buildings(String value) {
                    return set("Buildings", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addSelectionAlias", field="GUILDS")
                public static class guild extends CommandRef {
                    public static final guild cmd = new guild();
                public guild name(String value) {
                    return set("name", value);
                }

                public guild guilds(String value) {
                    return set("guilds", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addSelectionAlias", field="SETTINGS")
                public static class guildsetting extends CommandRef {
                    public static final guildsetting cmd = new guildsetting();
                public guildsetting name(String value) {
                    return set("name", value);
                }

                public guildsetting settings(String value) {
                    return set("settings", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addSelectionAlias", field="UNITS")
                public static class militaryunit extends CommandRef {
                    public static final militaryunit cmd = new militaryunit();
                public militaryunit name(String value) {
                    return set("name", value);
                }

                public militaryunit military_units(String value) {
                    return set("military_units", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addSelectionAlias", field="NATIONS")
                public static class nation extends CommandRef {
                    public static final nation cmd = new nation();
                public nation name(String value) {
                    return set("name", value);
                }

                public nation nations(String value) {
                    return set("nations", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addSelectionAlias", field="NATION_LIST")
                public static class nationlist extends CommandRef {
                    public static final nationlist cmd = new nationlist();
                public nationlist name(String value) {
                    return set("name", value);
                }

                public nationlist nationlists(String value) {
                    return set("nationlists", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addSelectionAlias", field="NATION_OR_ALLIANCE")
                public static class nationoralliance extends CommandRef {
                    public static final nationoralliance cmd = new nationoralliance();
                public nationoralliance name(String value) {
                    return set("name", value);
                }

                public nationoralliance nationoralliances(String value) {
                    return set("nationoralliances", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addSelectionAlias", field="PROJECTS")
                public static class project extends CommandRef {
                    public static final project cmd = new project();
                public project name(String value) {
                    return set("name", value);
                }

                public project projects(String value) {
                    return set("projects", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addSelectionAlias", field="RESOURCE_TYPES")
                public static class resourcetype extends CommandRef {
                    public static final resourcetype cmd = new resourcetype();
                public resourcetype name(String value) {
                    return set("name", value);
                }

                public resourcetype resources(String value) {
                    return set("resources", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addSelectionAlias", field="TECHNOLOGIES")
                public static class technology extends CommandRef {
                    public static final technology cmd = new technology();
                public technology name(String value) {
                    return set("name", value);
                }

                public technology policies(String value) {
                    return set("policies", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addSelectionAlias", field="POLICIES")
                public static class timedpolicy extends CommandRef {
                    public static final timedpolicy cmd = new timedpolicy();
                public timedpolicy name(String value) {
                    return set("name", value);
                }

                public timedpolicy policies(String value) {
                    return set("policies", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addSelectionAlias", field="TREATIES")
                public static class treaty extends CommandRef {
                    public static final treaty cmd = new treaty();
                public treaty name(String value) {
                    return set("name", value);
                }

                public treaty treaties(String value) {
                    return set("treaties", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addSelectionAlias", field="TREATY_TYPES")
                public static class treatytype extends CommandRef {
                    public static final treatytype cmd = new treatytype();
                public treatytype name(String value) {
                    return set("name", value);
                }

                public treatytype treaty_types(String value) {
                    return set("treaty_types", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addSelectionAlias", field="USERS")
                public static class user extends CommandRef {
                    public static final user cmd = new user();
                public user name(String value) {
                    return set("name", value);
                }

                public user users(String value) {
                    return set("users", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addSelectionAlias", field="WARS")
                public static class war extends CommandRef {
                    public static final war cmd = new war();
                public war name(String value) {
                    return set("name", value);
                }

                public war wars(String value) {
                    return set("wars", value);
                }

                }
            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.CustomSheetCommands.class,method="listSelectionAliases")
            public static class list extends CommandRef {
                public static final list cmd = new list();
            public list type(String value) {
                return set("type", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.CustomSheetCommands.class,method="deleteSelectionAlias")
            public static class remove extends CommandRef {
                public static final remove cmd = new remove();
            public remove selection(String value) {
                return set("selection", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.CustomSheetCommands.class,method="renameSelection")
            public static class rename extends CommandRef {
                public static final rename cmd = new rename();
            public rename sheet(String value) {
                return set("sheet", value);
            }

            public rename name(String value) {
                return set("name", value);
            }

            }
        }
        public static class self{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="addRole")
            public static class add extends CommandRef {
                public static final add cmd = new add();
            public add member(String value) {
                return set("member", value);
            }

            public add addRole(String value) {
                return set("addRole", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="addAssignableRole")
            public static class create extends CommandRef {
                public static final create cmd = new create();
            public create requireRole(String value) {
                return set("requireRole", value);
            }

            public create assignableRoles(String value) {
                return set("assignableRoles", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="listAssignableRoles")
            public static class list extends CommandRef {
                public static final list cmd = new list();

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.IACommands.class,method="removeRole")
            public static class remove extends CommandRef {
                public static final remove cmd = new remove();
            public remove member(String value) {
                return set("member", value);
            }

            public remove addRole(String value) {
                return set("addRole", value);
            }

            }
        }
        public static class settings{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.SettingCommands.class,method="delete")
            public static class delete extends CommandRef {
                public static final delete cmd = new delete();
            public delete key(String value) {
                return set("key", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.SettingCommands.class,method="info")
            public static class info extends CommandRef {
                public static final info cmd = new info();
            public info key(String value) {
                return set("key", value);
            }

            public info value(String value) {
                return set("value", value);
            }

            public info listAll(String value) {
                return set("listAll", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.SettingCommands.class,method="sheets")
            public static class sheets extends CommandRef {
                public static final sheets cmd = new sheets();

            }
        }
        public static class settings_artificial_intelligence{
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="ENABLE_GITHUB_COPILOT", field="ENABLE_GITHUB_COPILOT")
            public static class ENABLE_GITHUB_COPILOT extends CommandRef {
                public static final ENABLE_GITHUB_COPILOT cmd = new ENABLE_GITHUB_COPILOT();
            public ENABLE_GITHUB_COPILOT value(String value) {
                return set("value", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="GPT_USAGE_LIMITS", field="GPT_USAGE_LIMITS")
            public static class GPT_USAGE_LIMITS extends CommandRef {
                public static final GPT_USAGE_LIMITS cmd = new GPT_USAGE_LIMITS();
            public GPT_USAGE_LIMITS userTurnLimit(String value) {
                return set("userTurnLimit", value);
            }

            public GPT_USAGE_LIMITS userDayLimit(String value) {
                return set("userDayLimit", value);
            }

            public GPT_USAGE_LIMITS guildTurnLimit(String value) {
                return set("guildTurnLimit", value);
            }

            public GPT_USAGE_LIMITS guildDayLimit(String value) {
                return set("guildDayLimit", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="register_openai_key", field="OPENAI_MODEL")
            public static class register_openai_key extends CommandRef {
                public static final register_openai_key cmd = new register_openai_key();
            public register_openai_key model(String value) {
                return set("model", value);
            }

            }
        }
        public static class settings_audit{
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="DISABLED_MEMBER_AUDITS", field="DISABLED_MEMBER_AUDITS")
            public static class DISABLED_MEMBER_AUDITS extends CommandRef {
                public static final DISABLED_MEMBER_AUDITS cmd = new DISABLED_MEMBER_AUDITS();
            public DISABLED_MEMBER_AUDITS audits(String value) {
                return set("audits", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="MEMBER_AUDIT_ALERTS", field="MEMBER_AUDIT_ALERTS")
            public static class MEMBER_AUDIT_ALERTS extends CommandRef {
                public static final MEMBER_AUDIT_ALERTS cmd = new MEMBER_AUDIT_ALERTS();
            public MEMBER_AUDIT_ALERTS channel(String value) {
                return set("channel", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="MEMBER_LEAVE_ALERT_CHANNEL", field="MEMBER_LEAVE_ALERT_CHANNEL")
            public static class MEMBER_LEAVE_ALERT_CHANNEL extends CommandRef {
                public static final MEMBER_LEAVE_ALERT_CHANNEL cmd = new MEMBER_LEAVE_ALERT_CHANNEL();
            public MEMBER_LEAVE_ALERT_CHANNEL channel(String value) {
                return set("channel", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="REQUIRED_MMR", field="REQUIRED_MMR")
            public static class REQUIRED_MMR extends CommandRef {
                public static final REQUIRED_MMR cmd = new REQUIRED_MMR();
            public REQUIRED_MMR mmrMap(String value) {
                return set("mmrMap", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="WARCHEST_PER_INFRA", field="WARCHEST_PER_INFRA")
            public static class WARCHEST_PER_INFRA extends CommandRef {
                public static final WARCHEST_PER_INFRA cmd = new WARCHEST_PER_INFRA();
            public WARCHEST_PER_INFRA amount(String value) {
                return set("amount", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="addRequiredMMR", field="REQUIRED_MMR")
            public static class addRequiredMMR extends CommandRef {
                public static final addRequiredMMR cmd = new addRequiredMMR();
            public addRequiredMMR filter(String value) {
                return set("filter", value);
            }

            public addRequiredMMR mmr(String value) {
                return set("mmr", value);
            }

            }
        }
        public static class settings_bank_info{
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="ADDBALANCE_ALERT_CHANNEL", field="ADDBALANCE_ALERT_CHANNEL")
            public static class ADDBALANCE_ALERT_CHANNEL extends CommandRef {
                public static final ADDBALANCE_ALERT_CHANNEL cmd = new ADDBALANCE_ALERT_CHANNEL();
            public ADDBALANCE_ALERT_CHANNEL channel(String value) {
                return set("channel", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="DEPOSIT_ALERT_CHANNEL", field="DEPOSIT_ALERT_CHANNEL")
            public static class DEPOSIT_ALERT_CHANNEL extends CommandRef {
                public static final DEPOSIT_ALERT_CHANNEL cmd = new DEPOSIT_ALERT_CHANNEL();
            public DEPOSIT_ALERT_CHANNEL channel(String value) {
                return set("channel", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="DISPLAY_CONDENSED_DEPOSITS", field="DISPLAY_CONDENSED_DEPOSITS")
            public static class DISPLAY_CONDENSED_DEPOSITS extends CommandRef {
                public static final DISPLAY_CONDENSED_DEPOSITS cmd = new DISPLAY_CONDENSED_DEPOSITS();
            public DISPLAY_CONDENSED_DEPOSITS enabled(String value) {
                return set("enabled", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="DISPLAY_ITEMIZED_DEPOSITS", field="DISPLAY_ITEMIZED_DEPOSITS")
            public static class DISPLAY_ITEMIZED_DEPOSITS extends CommandRef {
                public static final DISPLAY_ITEMIZED_DEPOSITS cmd = new DISPLAY_ITEMIZED_DEPOSITS();
            public DISPLAY_ITEMIZED_DEPOSITS enabled(String value) {
                return set("enabled", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="GRANT_REQUEST_ALERTS", field="GRANT_REQUEST_ALERTS")
            public static class GRANT_REQUEST_ALERTS extends CommandRef {
                public static final GRANT_REQUEST_ALERTS cmd = new GRANT_REQUEST_ALERTS();
            public GRANT_REQUEST_ALERTS channel(String value) {
                return set("channel", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="LOAN_REQUEST_ALERTS", field="LOAN_REQUEST_ALERTS")
            public static class LOAN_REQUEST_ALERTS extends CommandRef {
                public static final LOAN_REQUEST_ALERTS cmd = new LOAN_REQUEST_ALERTS();
            public LOAN_REQUEST_ALERTS channel(String value) {
                return set("channel", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="WITHDRAW_ALERT_CHANNEL", field="WITHDRAW_ALERT_CHANNEL")
            public static class WITHDRAW_ALERT_CHANNEL extends CommandRef {
                public static final WITHDRAW_ALERT_CHANNEL cmd = new WITHDRAW_ALERT_CHANNEL();
            public WITHDRAW_ALERT_CHANNEL channel(String value) {
                return set("channel", value);
            }

            }
        }
        public static class settings_beige_alerts{
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="ENEMY_ALERT_CHANNEL", field="ENEMY_ALERT_CHANNEL")
            public static class ENEMY_ALERT_CHANNEL extends CommandRef {
                public static final ENEMY_ALERT_CHANNEL cmd = new ENEMY_ALERT_CHANNEL();
            public ENEMY_ALERT_CHANNEL channel(String value) {
                return set("channel", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="ENEMY_ALERT_CHANNEL_MODE", field="ENEMY_ALERT_CHANNEL_MODE")
            public static class ENEMY_ALERT_CHANNEL_MODE extends CommandRef {
                public static final ENEMY_ALERT_CHANNEL_MODE cmd = new ENEMY_ALERT_CHANNEL_MODE();
            public ENEMY_ALERT_CHANNEL_MODE mode(String value) {
                return set("mode", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="ENEMY_ALERT_FILTER", field="ENEMY_ALERT_FILTER")
            public static class ENEMY_ALERT_FILTER extends CommandRef {
                public static final ENEMY_ALERT_FILTER cmd = new ENEMY_ALERT_FILTER();
            public ENEMY_ALERT_FILTER filter(String value) {
                return set("filter", value);
            }

            }
        }
        public static class settings_default{
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="DELEGATE_SERVER", field="DELEGATE_SERVER")
            public static class DELEGATE_SERVER extends CommandRef {
                public static final DELEGATE_SERVER cmd = new DELEGATE_SERVER();
            public DELEGATE_SERVER guild(String value) {
                return set("guild", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="registerAlliance", field="ALLIANCE_ID")
            public static class registerAlliance extends CommandRef {
                public static final registerAlliance cmd = new registerAlliance();
            public registerAlliance alliances(String value) {
                return set("alliances", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="registerApiKey", field="API_KEY")
            public static class registerApiKey extends CommandRef {
                public static final registerApiKey cmd = new registerApiKey();
            public registerApiKey apiKeys(String value) {
                return set("apiKeys", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="unregisterAlliance", field="ALLIANCE_ID")
            public static class unregisterAlliance extends CommandRef {
                public static final unregisterAlliance cmd = new unregisterAlliance();
            public unregisterAlliance alliances(String value) {
                return set("alliances", value);
            }

            }
        }
        public static class settings_foreign_affairs{
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="ALLIANCE_CREATE_ALERTS", field="ALLIANCE_CREATE_ALERTS")
            public static class ALLIANCE_CREATE_ALERTS extends CommandRef {
                public static final ALLIANCE_CREATE_ALERTS cmd = new ALLIANCE_CREATE_ALERTS();
            public ALLIANCE_CREATE_ALERTS channel(String value) {
                return set("channel", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="DO_NOT_RAID_TOP_X", field="DO_NOT_RAID_TOP_X")
            public static class DO_NOT_RAID_TOP_X extends CommandRef {
                public static final DO_NOT_RAID_TOP_X cmd = new DO_NOT_RAID_TOP_X();
            public DO_NOT_RAID_TOP_X topAllianceScore(String value) {
                return set("topAllianceScore", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="EMBASSY_CATEGORY", field="EMBASSY_CATEGORY")
            public static class EMBASSY_CATEGORY extends CommandRef {
                public static final EMBASSY_CATEGORY cmd = new EMBASSY_CATEGORY();
            public EMBASSY_CATEGORY category(String value) {
                return set("category", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="FA_SERVER", field="FA_SERVER")
            public static class FA_SERVER extends CommandRef {
                public static final FA_SERVER cmd = new FA_SERVER();
            public FA_SERVER guild(String value) {
                return set("guild", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="TREATY_ALERTS", field="TREATY_ALERTS")
            public static class TREATY_ALERTS extends CommandRef {
                public static final TREATY_ALERTS cmd = new TREATY_ALERTS();
            public TREATY_ALERTS channel(String value) {
                return set("channel", value);
            }

            }
        }
        public static class settings_game_alerts{
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="ACTIVITY_ALERTS", field="ACTIVITY_ALERTS")
            public static class ACTIVITY_ALERTS extends CommandRef {
                public static final ACTIVITY_ALERTS cmd = new ACTIVITY_ALERTS();
            public ACTIVITY_ALERTS channel(String value) {
                return set("channel", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="ALLIANCE_EXODUS_TOP_X", field="ALLIANCE_EXODUS_TOP_X")
            public static class ALLIANCE_EXODUS_TOP_X extends CommandRef {
                public static final ALLIANCE_EXODUS_TOP_X cmd = new ALLIANCE_EXODUS_TOP_X();
            public ALLIANCE_EXODUS_TOP_X rank(String value) {
                return set("rank", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="DELETION_ALERT_CHANNEL", field="DELETION_ALERT_CHANNEL")
            public static class DELETION_ALERT_CHANNEL extends CommandRef {
                public static final DELETION_ALERT_CHANNEL cmd = new DELETION_ALERT_CHANNEL();
            public DELETION_ALERT_CHANNEL channel(String value) {
                return set("channel", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="ESCALATION_ALERTS", field="ESCALATION_ALERTS")
            public static class ESCALATION_ALERTS extends CommandRef {
                public static final ESCALATION_ALERTS cmd = new ESCALATION_ALERTS();
            public ESCALATION_ALERTS channel(String value) {
                return set("channel", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="GAME_ALLIANCE_EXODUS_ALERTS", field="GAME_ALLIANCE_EXODUS_ALERTS")
            public static class GAME_ALLIANCE_EXODUS_ALERTS extends CommandRef {
                public static final GAME_ALLIANCE_EXODUS_ALERTS cmd = new GAME_ALLIANCE_EXODUS_ALERTS();
            public GAME_ALLIANCE_EXODUS_ALERTS channel(String value) {
                return set("channel", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="GAME_LEADER_CHANGE_ALERT", field="GAME_LEADER_CHANGE_ALERT")
            public static class GAME_LEADER_CHANGE_ALERT extends CommandRef {
                public static final GAME_LEADER_CHANGE_ALERT cmd = new GAME_LEADER_CHANGE_ALERT();
            public GAME_LEADER_CHANGE_ALERT channel(String value) {
                return set("channel", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="REPORT_ALERT_CHANNEL", field="REPORT_ALERT_CHANNEL")
            public static class REPORT_ALERT_CHANNEL extends CommandRef {
                public static final REPORT_ALERT_CHANNEL cmd = new REPORT_ALERT_CHANNEL();
            public REPORT_ALERT_CHANNEL channel(String value) {
                return set("channel", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="REROLL_ALERT_CHANNEL", field="REROLL_ALERT_CHANNEL")
            public static class REROLL_ALERT_CHANNEL extends CommandRef {
                public static final REROLL_ALERT_CHANNEL cmd = new REROLL_ALERT_CHANNEL();
            public REROLL_ALERT_CHANNEL channel(String value) {
                return set("channel", value);
            }

            }
        }
        public static class settings_interview{
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="ARCHIVE_CATEGORY", field="ARCHIVE_CATEGORY")
            public static class ARCHIVE_CATEGORY extends CommandRef {
                public static final ARCHIVE_CATEGORY cmd = new ARCHIVE_CATEGORY();
            public ARCHIVE_CATEGORY category(String value) {
                return set("category", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="INTERVIEW_INFO_SPAM", field="INTERVIEW_INFO_SPAM")
            public static class INTERVIEW_INFO_SPAM extends CommandRef {
                public static final INTERVIEW_INFO_SPAM cmd = new INTERVIEW_INFO_SPAM();
            public INTERVIEW_INFO_SPAM channel(String value) {
                return set("channel", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="INTERVIEW_PENDING_ALERTS", field="INTERVIEW_PENDING_ALERTS")
            public static class INTERVIEW_PENDING_ALERTS extends CommandRef {
                public static final INTERVIEW_PENDING_ALERTS cmd = new INTERVIEW_PENDING_ALERTS();
            public INTERVIEW_PENDING_ALERTS channel(String value) {
                return set("channel", value);
            }

            }
        }
        public static class settings_orbis_alerts{
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="ACTIVITY_ALERTS", field="ACTIVITY_ALERTS")
            public static class ACTIVITY_ALERTS extends CommandRef {
                public static final ACTIVITY_ALERTS cmd = new ACTIVITY_ALERTS();
            public ACTIVITY_ALERTS channel(String value) {
                return set("channel", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="ALLIANCE_EXODUS_TOP_X", field="ALLIANCE_EXODUS_TOP_X")
            public static class ALLIANCE_EXODUS_TOP_X extends CommandRef {
                public static final ALLIANCE_EXODUS_TOP_X cmd = new ALLIANCE_EXODUS_TOP_X();
            public ALLIANCE_EXODUS_TOP_X rank(String value) {
                return set("rank", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="DELETION_ALERT_CHANNEL", field="DELETION_ALERT_CHANNEL")
            public static class DELETION_ALERT_CHANNEL extends CommandRef {
                public static final DELETION_ALERT_CHANNEL cmd = new DELETION_ALERT_CHANNEL();
            public DELETION_ALERT_CHANNEL channel(String value) {
                return set("channel", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="ESCALATION_ALERTS", field="ESCALATION_ALERTS")
            public static class ESCALATION_ALERTS extends CommandRef {
                public static final ESCALATION_ALERTS cmd = new ESCALATION_ALERTS();
            public ESCALATION_ALERTS channel(String value) {
                return set("channel", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="REPORT_ALERT_CHANNEL", field="REPORT_ALERT_CHANNEL")
            public static class REPORT_ALERT_CHANNEL extends CommandRef {
                public static final REPORT_ALERT_CHANNEL cmd = new REPORT_ALERT_CHANNEL();
            public REPORT_ALERT_CHANNEL channel(String value) {
                return set("channel", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="REROLL_ALERT_CHANNEL", field="REROLL_ALERT_CHANNEL")
            public static class REROLL_ALERT_CHANNEL extends CommandRef {
                public static final REROLL_ALERT_CHANNEL cmd = new REROLL_ALERT_CHANNEL();
            public REROLL_ALERT_CHANNEL channel(String value) {
                return set("channel", value);
            }

            }
        }
        public static class settings_protection_alerts{
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="ENEMY_ALERT_CHANNEL", field="ENEMY_ALERT_CHANNEL")
            public static class ENEMY_ALERT_CHANNEL extends CommandRef {
                public static final ENEMY_ALERT_CHANNEL cmd = new ENEMY_ALERT_CHANNEL();
            public ENEMY_ALERT_CHANNEL channel(String value) {
                return set("channel", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="ENEMY_ALERT_CHANNEL_MODE", field="ENEMY_ALERT_CHANNEL_MODE")
            public static class ENEMY_ALERT_CHANNEL_MODE extends CommandRef {
                public static final ENEMY_ALERT_CHANNEL_MODE cmd = new ENEMY_ALERT_CHANNEL_MODE();
            public ENEMY_ALERT_CHANNEL_MODE mode(String value) {
                return set("mode", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="ENEMY_ALERT_FILTER", field="ENEMY_ALERT_FILTER")
            public static class ENEMY_ALERT_FILTER extends CommandRef {
                public static final ENEMY_ALERT_FILTER cmd = new ENEMY_ALERT_FILTER();
            public ENEMY_ALERT_FILTER filter(String value) {
                return set("filter", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="ENEMY_PROTECTION_ALERT", field="ENEMY_PROTECTION_ALERT")
            public static class ENEMY_PROTECTION_ALERT extends CommandRef {
                public static final ENEMY_PROTECTION_ALERT cmd = new ENEMY_PROTECTION_ALERT();
            public ENEMY_PROTECTION_ALERT channel(String value) {
                return set("channel", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="PROTECTION_ALERT_CHANNEL", field="PROTECTION_ALERT_CHANNEL")
            public static class PROTECTION_ALERT_CHANNEL extends CommandRef {
                public static final PROTECTION_ALERT_CHANNEL cmd = new PROTECTION_ALERT_CHANNEL();
            public PROTECTION_ALERT_CHANNEL channel(String value) {
                return set("channel", value);
            }

            }
        }
        public static class settings_role{
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="ASSIGNABLE_ROLES", field="ASSIGNABLE_ROLES")
            public static class ASSIGNABLE_ROLES extends CommandRef {
                public static final ASSIGNABLE_ROLES cmd = new ASSIGNABLE_ROLES();
            public ASSIGNABLE_ROLES value(String value) {
                return set("value", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="AUTONICK", field="AUTONICK")
            public static class AUTONICK extends CommandRef {
                public static final AUTONICK cmd = new AUTONICK();
            public AUTONICK mode(String value) {
                return set("mode", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="AUTOROLE_ALLIANCES", field="AUTOROLE_ALLIANCES")
            public static class AUTOROLE_ALLIANCES extends CommandRef {
                public static final AUTOROLE_ALLIANCES cmd = new AUTOROLE_ALLIANCES();
            public AUTOROLE_ALLIANCES mode(String value) {
                return set("mode", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="AUTOROLE_ALLIANCE_RANK", field="AUTOROLE_ALLIANCE_RANK")
            public static class AUTOROLE_ALLIANCE_RANK extends CommandRef {
                public static final AUTOROLE_ALLIANCE_RANK cmd = new AUTOROLE_ALLIANCE_RANK();
            public AUTOROLE_ALLIANCE_RANK allianceRank(String value) {
                return set("allianceRank", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="AUTOROLE_ALLY_GOV", field="AUTOROLE_ALLY_GOV")
            public static class AUTOROLE_ALLY_GOV extends CommandRef {
                public static final AUTOROLE_ALLY_GOV cmd = new AUTOROLE_ALLY_GOV();
            public AUTOROLE_ALLY_GOV enabled(String value) {
                return set("enabled", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="AUTOROLE_ALLY_ROLES", field="AUTOROLE_ALLY_ROLES")
            public static class AUTOROLE_ALLY_ROLES extends CommandRef {
                public static final AUTOROLE_ALLY_ROLES cmd = new AUTOROLE_ALLY_ROLES();
            public AUTOROLE_ALLY_ROLES roles(String value) {
                return set("roles", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="AUTOROLE_MEMBERS", field="AUTOROLE_MEMBERS")
            public static class AUTOROLE_MEMBERS extends CommandRef {
                public static final AUTOROLE_MEMBERS cmd = new AUTOROLE_MEMBERS();
            public AUTOROLE_MEMBERS enabled(String value) {
                return set("enabled", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="AUTOROLE_TOP_X", field="AUTOROLE_TOP_X")
            public static class AUTOROLE_TOP_X extends CommandRef {
                public static final AUTOROLE_TOP_X cmd = new AUTOROLE_TOP_X();
            public AUTOROLE_TOP_X topScoreRank(String value) {
                return set("topScoreRank", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="CONDITIONAL_ROLES", field="CONDITIONAL_ROLES")
            public static class CONDITIONAL_ROLES extends CommandRef {
                public static final CONDITIONAL_ROLES cmd = new CONDITIONAL_ROLES();
            public CONDITIONAL_ROLES roleMap(String value) {
                return set("roleMap", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="addAssignableRole", field="ASSIGNABLE_ROLES")
            public static class addAssignableRole extends CommandRef {
                public static final addAssignableRole cmd = new addAssignableRole();
            public addAssignableRole role(String value) {
                return set("role", value);
            }

            public addAssignableRole roles(String value) {
                return set("roles", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="addConditionalRole", field="CONDITIONAL_ROLES")
            public static class addConditionalRole extends CommandRef {
                public static final addConditionalRole cmd = new addConditionalRole();
            public addConditionalRole filter(String value) {
                return set("filter", value);
            }

            public addConditionalRole role(String value) {
                return set("role", value);
            }

            }
        }
        public static class settings_sheet{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.CustomSheetCommands.class,method="listSheetKeys")
            public static class list extends CommandRef {
                public static final list cmd = new list();

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.CustomSheetCommands.class,method="setSheetKey")
            public static class set extends CommandRef {
                public static final set cmd = new set();
            public set key(String value) {
                return set("key", value);
            }

            public set sheetId(String value) {
                return set("sheetId", value);
            }

            public set tab(String value) {
                return set("tab", value);
            }

            }
        }
        public static class settings_war_alerts{
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="DEFENSE_WAR_CHANNEL", field="DEFENSE_WAR_CHANNEL")
            public static class DEFENSE_WAR_CHANNEL extends CommandRef {
                public static final DEFENSE_WAR_CHANNEL cmd = new DEFENSE_WAR_CHANNEL();
            public DEFENSE_WAR_CHANNEL channel(String value) {
                return set("channel", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="HIDE_APPLICANT_WARS", field="HIDE_APPLICANT_WARS")
            public static class HIDE_APPLICANT_WARS extends CommandRef {
                public static final HIDE_APPLICANT_WARS cmd = new HIDE_APPLICANT_WARS();
            public HIDE_APPLICANT_WARS value(String value) {
                return set("value", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="LOST_WAR_CHANNEL", field="LOST_WAR_CHANNEL")
            public static class LOST_WAR_CHANNEL extends CommandRef {
                public static final LOST_WAR_CHANNEL cmd = new LOST_WAR_CHANNEL();
            public LOST_WAR_CHANNEL channel(String value) {
                return set("channel", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="MENTION_MILCOM_COUNTERS", field="MENTION_MILCOM_COUNTERS")
            public static class MENTION_MILCOM_COUNTERS extends CommandRef {
                public static final MENTION_MILCOM_COUNTERS cmd = new MENTION_MILCOM_COUNTERS();
            public MENTION_MILCOM_COUNTERS value(String value) {
                return set("value", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="MENTION_MILCOM_FILTER", field="MENTION_MILCOM_FILTER")
            public static class MENTION_MILCOM_FILTER extends CommandRef {
                public static final MENTION_MILCOM_FILTER cmd = new MENTION_MILCOM_FILTER();
            public MENTION_MILCOM_FILTER value(String value) {
                return set("value", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="OFFENSIVE_WAR_CHANNEL", field="OFFENSIVE_WAR_CHANNEL")
            public static class OFFENSIVE_WAR_CHANNEL extends CommandRef {
                public static final OFFENSIVE_WAR_CHANNEL cmd = new OFFENSIVE_WAR_CHANNEL();
            public OFFENSIVE_WAR_CHANNEL channel(String value) {
                return set("channel", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="SHOW_ALLY_DEFENSIVE_WARS", field="SHOW_ALLY_DEFENSIVE_WARS")
            public static class SHOW_ALLY_DEFENSIVE_WARS extends CommandRef {
                public static final SHOW_ALLY_DEFENSIVE_WARS cmd = new SHOW_ALLY_DEFENSIVE_WARS();
            public SHOW_ALLY_DEFENSIVE_WARS enabled(String value) {
                return set("enabled", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="SHOW_ALLY_OFFENSIVE_WARS", field="SHOW_ALLY_OFFENSIVE_WARS")
            public static class SHOW_ALLY_OFFENSIVE_WARS extends CommandRef {
                public static final SHOW_ALLY_OFFENSIVE_WARS cmd = new SHOW_ALLY_OFFENSIVE_WARS();
            public SHOW_ALLY_OFFENSIVE_WARS enabled(String value) {
                return set("enabled", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="WAR_ROOM_FILTER", field="WAR_ROOM_FILTER")
            public static class WAR_ROOM_FILTER extends CommandRef {
                public static final WAR_ROOM_FILTER cmd = new WAR_ROOM_FILTER();
            public WAR_ROOM_FILTER value(String value) {
                return set("value", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="WON_WAR_CHANNEL", field="WON_WAR_CHANNEL")
            public static class WON_WAR_CHANNEL extends CommandRef {
                public static final WON_WAR_CHANNEL cmd = new WON_WAR_CHANNEL();
            public WON_WAR_CHANNEL channel(String value) {
                return set("channel", value);
            }

            }
        }
        public static class settings_war_room{
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="ENABLE_WAR_ROOMS", field="ENABLE_WAR_ROOMS")
            public static class ENABLE_WAR_ROOMS extends CommandRef {
                public static final ENABLE_WAR_ROOMS cmd = new ENABLE_WAR_ROOMS();
            public ENABLE_WAR_ROOMS enabled(String value) {
                return set("enabled", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.db.guild.GuildKey.class,method="WAR_SERVER", field="WAR_SERVER")
            public static class WAR_SERVER extends CommandRef {
                public static final WAR_SERVER cmd = new WAR_SERVER();
            public WAR_SERVER guild(String value) {
                return set("guild", value);
            }

            }
        }
        public static class sheet_custom{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.CustomSheetCommands.class,method="addTab")
            public static class add_tab extends CommandRef {
                public static final add_tab cmd = new add_tab();
            public add_tab sheet(String value) {
                return set("sheet", value);
            }

            public add_tab tabName(String value) {
                return set("tabName", value);
            }

            public add_tab select(String value) {
                return set("select", value);
            }

            public add_tab columns(String value) {
                return set("columns", value);
            }

            public add_tab force(String value) {
                return set("force", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.CustomSheetCommands.class,method="auto")
            public static class auto extends CommandRef {
                public static final auto cmd = new auto();
            public auto sheet(String value) {
                return set("sheet", value);
            }

            public auto saveSheet(String value) {
                return set("saveSheet", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.CustomSheetCommands.class,method="listCustomSheets")
            public static class list extends CommandRef {
                public static final list cmd = new list();

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.CustomSheetCommands.class,method="deleteTab")
            public static class remove_tab extends CommandRef {
                public static final remove_tab cmd = new remove_tab();
            public remove_tab sheet(String value) {
                return set("sheet", value);
            }

            public remove_tab tabName(String value) {
                return set("tabName", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.CustomSheetCommands.class,method="updateSheet")
            public static class update extends CommandRef {
                public static final update cmd = new update();
            public update sheet(String value) {
                return set("sheet", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.CustomSheetCommands.class,method="info")
            public static class view extends CommandRef {
                public static final view cmd = new view();
            public view sheet(String value) {
                return set("sheet", value);
            }

            }
        }
        public static class sheet_template{
            public static class add{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addColumns", field="ALLIANCES")
                public static class alliance extends CommandRef {
                    public static final alliance cmd = new alliance();
                public alliance sheet(String value) {
                    return set("sheet", value);
                }

                public alliance a(String value) {
                    return set("a", value);
                }

                public alliance b(String value) {
                    return set("b", value);
                }

                public alliance c(String value) {
                    return set("c", value);
                }

                public alliance d(String value) {
                    return set("d", value);
                }

                public alliance e(String value) {
                    return set("e", value);
                }

                public alliance f(String value) {
                    return set("f", value);
                }

                public alliance g(String value) {
                    return set("g", value);
                }

                public alliance h(String value) {
                    return set("h", value);
                }

                public alliance i(String value) {
                    return set("i", value);
                }

                public alliance j(String value) {
                    return set("j", value);
                }

                public alliance k(String value) {
                    return set("k", value);
                }

                public alliance l(String value) {
                    return set("l", value);
                }

                public alliance m(String value) {
                    return set("m", value);
                }

                public alliance n(String value) {
                    return set("n", value);
                }

                public alliance o(String value) {
                    return set("o", value);
                }

                public alliance p(String value) {
                    return set("p", value);
                }

                public alliance q(String value) {
                    return set("q", value);
                }

                public alliance r(String value) {
                    return set("r", value);
                }

                public alliance s(String value) {
                    return set("s", value);
                }

                public alliance t(String value) {
                    return set("t", value);
                }

                public alliance u(String value) {
                    return set("u", value);
                }

                public alliance v(String value) {
                    return set("v", value);
                }

                public alliance w(String value) {
                    return set("w", value);
                }

                public alliance x(String value) {
                    return set("x", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addColumns", field="AUDIT_TYPES")
                public static class audittype extends CommandRef {
                    public static final audittype cmd = new audittype();
                public audittype sheet(String value) {
                    return set("sheet", value);
                }

                public audittype a(String value) {
                    return set("a", value);
                }

                public audittype b(String value) {
                    return set("b", value);
                }

                public audittype c(String value) {
                    return set("c", value);
                }

                public audittype d(String value) {
                    return set("d", value);
                }

                public audittype e(String value) {
                    return set("e", value);
                }

                public audittype f(String value) {
                    return set("f", value);
                }

                public audittype g(String value) {
                    return set("g", value);
                }

                public audittype h(String value) {
                    return set("h", value);
                }

                public audittype i(String value) {
                    return set("i", value);
                }

                public audittype j(String value) {
                    return set("j", value);
                }

                public audittype k(String value) {
                    return set("k", value);
                }

                public audittype l(String value) {
                    return set("l", value);
                }

                public audittype m(String value) {
                    return set("m", value);
                }

                public audittype n(String value) {
                    return set("n", value);
                }

                public audittype o(String value) {
                    return set("o", value);
                }

                public audittype p(String value) {
                    return set("p", value);
                }

                public audittype q(String value) {
                    return set("q", value);
                }

                public audittype r(String value) {
                    return set("r", value);
                }

                public audittype s(String value) {
                    return set("s", value);
                }

                public audittype t(String value) {
                    return set("t", value);
                }

                public audittype u(String value) {
                    return set("u", value);
                }

                public audittype v(String value) {
                    return set("v", value);
                }

                public audittype w(String value) {
                    return set("w", value);
                }

                public audittype x(String value) {
                    return set("x", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addColumns", field="BUILDINGS")
                public static class building extends CommandRef {
                    public static final building cmd = new building();
                public building sheet(String value) {
                    return set("sheet", value);
                }

                public building a(String value) {
                    return set("a", value);
                }

                public building b(String value) {
                    return set("b", value);
                }

                public building c(String value) {
                    return set("c", value);
                }

                public building d(String value) {
                    return set("d", value);
                }

                public building e(String value) {
                    return set("e", value);
                }

                public building f(String value) {
                    return set("f", value);
                }

                public building g(String value) {
                    return set("g", value);
                }

                public building h(String value) {
                    return set("h", value);
                }

                public building i(String value) {
                    return set("i", value);
                }

                public building j(String value) {
                    return set("j", value);
                }

                public building k(String value) {
                    return set("k", value);
                }

                public building l(String value) {
                    return set("l", value);
                }

                public building m(String value) {
                    return set("m", value);
                }

                public building n(String value) {
                    return set("n", value);
                }

                public building o(String value) {
                    return set("o", value);
                }

                public building p(String value) {
                    return set("p", value);
                }

                public building q(String value) {
                    return set("q", value);
                }

                public building r(String value) {
                    return set("r", value);
                }

                public building s(String value) {
                    return set("s", value);
                }

                public building t(String value) {
                    return set("t", value);
                }

                public building u(String value) {
                    return set("u", value);
                }

                public building v(String value) {
                    return set("v", value);
                }

                public building w(String value) {
                    return set("w", value);
                }

                public building x(String value) {
                    return set("x", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addColumns", field="GUILDS")
                public static class guild extends CommandRef {
                    public static final guild cmd = new guild();
                public guild sheet(String value) {
                    return set("sheet", value);
                }

                public guild a(String value) {
                    return set("a", value);
                }

                public guild b(String value) {
                    return set("b", value);
                }

                public guild c(String value) {
                    return set("c", value);
                }

                public guild d(String value) {
                    return set("d", value);
                }

                public guild e(String value) {
                    return set("e", value);
                }

                public guild f(String value) {
                    return set("f", value);
                }

                public guild g(String value) {
                    return set("g", value);
                }

                public guild h(String value) {
                    return set("h", value);
                }

                public guild i(String value) {
                    return set("i", value);
                }

                public guild j(String value) {
                    return set("j", value);
                }

                public guild k(String value) {
                    return set("k", value);
                }

                public guild l(String value) {
                    return set("l", value);
                }

                public guild m(String value) {
                    return set("m", value);
                }

                public guild n(String value) {
                    return set("n", value);
                }

                public guild o(String value) {
                    return set("o", value);
                }

                public guild p(String value) {
                    return set("p", value);
                }

                public guild q(String value) {
                    return set("q", value);
                }

                public guild r(String value) {
                    return set("r", value);
                }

                public guild s(String value) {
                    return set("s", value);
                }

                public guild t(String value) {
                    return set("t", value);
                }

                public guild u(String value) {
                    return set("u", value);
                }

                public guild v(String value) {
                    return set("v", value);
                }

                public guild w(String value) {
                    return set("w", value);
                }

                public guild x(String value) {
                    return set("x", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addColumns", field="SETTINGS")
                public static class guildsetting extends CommandRef {
                    public static final guildsetting cmd = new guildsetting();
                public guildsetting sheet(String value) {
                    return set("sheet", value);
                }

                public guildsetting a(String value) {
                    return set("a", value);
                }

                public guildsetting b(String value) {
                    return set("b", value);
                }

                public guildsetting c(String value) {
                    return set("c", value);
                }

                public guildsetting d(String value) {
                    return set("d", value);
                }

                public guildsetting e(String value) {
                    return set("e", value);
                }

                public guildsetting f(String value) {
                    return set("f", value);
                }

                public guildsetting g(String value) {
                    return set("g", value);
                }

                public guildsetting h(String value) {
                    return set("h", value);
                }

                public guildsetting i(String value) {
                    return set("i", value);
                }

                public guildsetting j(String value) {
                    return set("j", value);
                }

                public guildsetting k(String value) {
                    return set("k", value);
                }

                public guildsetting l(String value) {
                    return set("l", value);
                }

                public guildsetting m(String value) {
                    return set("m", value);
                }

                public guildsetting n(String value) {
                    return set("n", value);
                }

                public guildsetting o(String value) {
                    return set("o", value);
                }

                public guildsetting p(String value) {
                    return set("p", value);
                }

                public guildsetting q(String value) {
                    return set("q", value);
                }

                public guildsetting r(String value) {
                    return set("r", value);
                }

                public guildsetting s(String value) {
                    return set("s", value);
                }

                public guildsetting t(String value) {
                    return set("t", value);
                }

                public guildsetting u(String value) {
                    return set("u", value);
                }

                public guildsetting v(String value) {
                    return set("v", value);
                }

                public guildsetting w(String value) {
                    return set("w", value);
                }

                public guildsetting x(String value) {
                    return set("x", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addColumns", field="UNITS")
                public static class militaryunit extends CommandRef {
                    public static final militaryunit cmd = new militaryunit();
                public militaryunit sheet(String value) {
                    return set("sheet", value);
                }

                public militaryunit a(String value) {
                    return set("a", value);
                }

                public militaryunit b(String value) {
                    return set("b", value);
                }

                public militaryunit c(String value) {
                    return set("c", value);
                }

                public militaryunit d(String value) {
                    return set("d", value);
                }

                public militaryunit e(String value) {
                    return set("e", value);
                }

                public militaryunit f(String value) {
                    return set("f", value);
                }

                public militaryunit g(String value) {
                    return set("g", value);
                }

                public militaryunit h(String value) {
                    return set("h", value);
                }

                public militaryunit i(String value) {
                    return set("i", value);
                }

                public militaryunit j(String value) {
                    return set("j", value);
                }

                public militaryunit k(String value) {
                    return set("k", value);
                }

                public militaryunit l(String value) {
                    return set("l", value);
                }

                public militaryunit m(String value) {
                    return set("m", value);
                }

                public militaryunit n(String value) {
                    return set("n", value);
                }

                public militaryunit o(String value) {
                    return set("o", value);
                }

                public militaryunit p(String value) {
                    return set("p", value);
                }

                public militaryunit q(String value) {
                    return set("q", value);
                }

                public militaryunit r(String value) {
                    return set("r", value);
                }

                public militaryunit s(String value) {
                    return set("s", value);
                }

                public militaryunit t(String value) {
                    return set("t", value);
                }

                public militaryunit u(String value) {
                    return set("u", value);
                }

                public militaryunit v(String value) {
                    return set("v", value);
                }

                public militaryunit w(String value) {
                    return set("w", value);
                }

                public militaryunit x(String value) {
                    return set("x", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addColumns", field="NATIONS")
                public static class nation extends CommandRef {
                    public static final nation cmd = new nation();
                public nation sheet(String value) {
                    return set("sheet", value);
                }

                public nation a(String value) {
                    return set("a", value);
                }

                public nation b(String value) {
                    return set("b", value);
                }

                public nation c(String value) {
                    return set("c", value);
                }

                public nation d(String value) {
                    return set("d", value);
                }

                public nation e(String value) {
                    return set("e", value);
                }

                public nation f(String value) {
                    return set("f", value);
                }

                public nation g(String value) {
                    return set("g", value);
                }

                public nation h(String value) {
                    return set("h", value);
                }

                public nation i(String value) {
                    return set("i", value);
                }

                public nation j(String value) {
                    return set("j", value);
                }

                public nation k(String value) {
                    return set("k", value);
                }

                public nation l(String value) {
                    return set("l", value);
                }

                public nation m(String value) {
                    return set("m", value);
                }

                public nation n(String value) {
                    return set("n", value);
                }

                public nation o(String value) {
                    return set("o", value);
                }

                public nation p(String value) {
                    return set("p", value);
                }

                public nation q(String value) {
                    return set("q", value);
                }

                public nation r(String value) {
                    return set("r", value);
                }

                public nation s(String value) {
                    return set("s", value);
                }

                public nation t(String value) {
                    return set("t", value);
                }

                public nation u(String value) {
                    return set("u", value);
                }

                public nation v(String value) {
                    return set("v", value);
                }

                public nation w(String value) {
                    return set("w", value);
                }

                public nation x(String value) {
                    return set("x", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addColumns", field="NATION_LIST")
                public static class nationlist extends CommandRef {
                    public static final nationlist cmd = new nationlist();
                public nationlist sheet(String value) {
                    return set("sheet", value);
                }

                public nationlist a(String value) {
                    return set("a", value);
                }

                public nationlist b(String value) {
                    return set("b", value);
                }

                public nationlist c(String value) {
                    return set("c", value);
                }

                public nationlist d(String value) {
                    return set("d", value);
                }

                public nationlist e(String value) {
                    return set("e", value);
                }

                public nationlist f(String value) {
                    return set("f", value);
                }

                public nationlist g(String value) {
                    return set("g", value);
                }

                public nationlist h(String value) {
                    return set("h", value);
                }

                public nationlist i(String value) {
                    return set("i", value);
                }

                public nationlist j(String value) {
                    return set("j", value);
                }

                public nationlist k(String value) {
                    return set("k", value);
                }

                public nationlist l(String value) {
                    return set("l", value);
                }

                public nationlist m(String value) {
                    return set("m", value);
                }

                public nationlist n(String value) {
                    return set("n", value);
                }

                public nationlist o(String value) {
                    return set("o", value);
                }

                public nationlist p(String value) {
                    return set("p", value);
                }

                public nationlist q(String value) {
                    return set("q", value);
                }

                public nationlist r(String value) {
                    return set("r", value);
                }

                public nationlist s(String value) {
                    return set("s", value);
                }

                public nationlist t(String value) {
                    return set("t", value);
                }

                public nationlist u(String value) {
                    return set("u", value);
                }

                public nationlist v(String value) {
                    return set("v", value);
                }

                public nationlist w(String value) {
                    return set("w", value);
                }

                public nationlist x(String value) {
                    return set("x", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addColumns", field="NATION_OR_ALLIANCE")
                public static class nationoralliance extends CommandRef {
                    public static final nationoralliance cmd = new nationoralliance();
                public nationoralliance sheet(String value) {
                    return set("sheet", value);
                }

                public nationoralliance a(String value) {
                    return set("a", value);
                }

                public nationoralliance b(String value) {
                    return set("b", value);
                }

                public nationoralliance c(String value) {
                    return set("c", value);
                }

                public nationoralliance d(String value) {
                    return set("d", value);
                }

                public nationoralliance e(String value) {
                    return set("e", value);
                }

                public nationoralliance f(String value) {
                    return set("f", value);
                }

                public nationoralliance g(String value) {
                    return set("g", value);
                }

                public nationoralliance h(String value) {
                    return set("h", value);
                }

                public nationoralliance i(String value) {
                    return set("i", value);
                }

                public nationoralliance j(String value) {
                    return set("j", value);
                }

                public nationoralliance k(String value) {
                    return set("k", value);
                }

                public nationoralliance l(String value) {
                    return set("l", value);
                }

                public nationoralliance m(String value) {
                    return set("m", value);
                }

                public nationoralliance n(String value) {
                    return set("n", value);
                }

                public nationoralliance o(String value) {
                    return set("o", value);
                }

                public nationoralliance p(String value) {
                    return set("p", value);
                }

                public nationoralliance q(String value) {
                    return set("q", value);
                }

                public nationoralliance r(String value) {
                    return set("r", value);
                }

                public nationoralliance s(String value) {
                    return set("s", value);
                }

                public nationoralliance t(String value) {
                    return set("t", value);
                }

                public nationoralliance u(String value) {
                    return set("u", value);
                }

                public nationoralliance v(String value) {
                    return set("v", value);
                }

                public nationoralliance w(String value) {
                    return set("w", value);
                }

                public nationoralliance x(String value) {
                    return set("x", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addColumns", field="PROJECTS")
                public static class project extends CommandRef {
                    public static final project cmd = new project();
                public project sheet(String value) {
                    return set("sheet", value);
                }

                public project a(String value) {
                    return set("a", value);
                }

                public project b(String value) {
                    return set("b", value);
                }

                public project c(String value) {
                    return set("c", value);
                }

                public project d(String value) {
                    return set("d", value);
                }

                public project e(String value) {
                    return set("e", value);
                }

                public project f(String value) {
                    return set("f", value);
                }

                public project g(String value) {
                    return set("g", value);
                }

                public project h(String value) {
                    return set("h", value);
                }

                public project i(String value) {
                    return set("i", value);
                }

                public project j(String value) {
                    return set("j", value);
                }

                public project k(String value) {
                    return set("k", value);
                }

                public project l(String value) {
                    return set("l", value);
                }

                public project m(String value) {
                    return set("m", value);
                }

                public project n(String value) {
                    return set("n", value);
                }

                public project o(String value) {
                    return set("o", value);
                }

                public project p(String value) {
                    return set("p", value);
                }

                public project q(String value) {
                    return set("q", value);
                }

                public project r(String value) {
                    return set("r", value);
                }

                public project s(String value) {
                    return set("s", value);
                }

                public project t(String value) {
                    return set("t", value);
                }

                public project u(String value) {
                    return set("u", value);
                }

                public project v(String value) {
                    return set("v", value);
                }

                public project w(String value) {
                    return set("w", value);
                }

                public project x(String value) {
                    return set("x", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addColumns", field="RESOURCE_TYPES")
                public static class resourcetype extends CommandRef {
                    public static final resourcetype cmd = new resourcetype();
                public resourcetype sheet(String value) {
                    return set("sheet", value);
                }

                public resourcetype a(String value) {
                    return set("a", value);
                }

                public resourcetype b(String value) {
                    return set("b", value);
                }

                public resourcetype c(String value) {
                    return set("c", value);
                }

                public resourcetype d(String value) {
                    return set("d", value);
                }

                public resourcetype e(String value) {
                    return set("e", value);
                }

                public resourcetype f(String value) {
                    return set("f", value);
                }

                public resourcetype g(String value) {
                    return set("g", value);
                }

                public resourcetype h(String value) {
                    return set("h", value);
                }

                public resourcetype i(String value) {
                    return set("i", value);
                }

                public resourcetype j(String value) {
                    return set("j", value);
                }

                public resourcetype k(String value) {
                    return set("k", value);
                }

                public resourcetype l(String value) {
                    return set("l", value);
                }

                public resourcetype m(String value) {
                    return set("m", value);
                }

                public resourcetype n(String value) {
                    return set("n", value);
                }

                public resourcetype o(String value) {
                    return set("o", value);
                }

                public resourcetype p(String value) {
                    return set("p", value);
                }

                public resourcetype q(String value) {
                    return set("q", value);
                }

                public resourcetype r(String value) {
                    return set("r", value);
                }

                public resourcetype s(String value) {
                    return set("s", value);
                }

                public resourcetype t(String value) {
                    return set("t", value);
                }

                public resourcetype u(String value) {
                    return set("u", value);
                }

                public resourcetype v(String value) {
                    return set("v", value);
                }

                public resourcetype w(String value) {
                    return set("w", value);
                }

                public resourcetype x(String value) {
                    return set("x", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addColumns", field="TECHNOLOGIES")
                public static class technology extends CommandRef {
                    public static final technology cmd = new technology();
                public technology sheet(String value) {
                    return set("sheet", value);
                }

                public technology a(String value) {
                    return set("a", value);
                }

                public technology b(String value) {
                    return set("b", value);
                }

                public technology c(String value) {
                    return set("c", value);
                }

                public technology d(String value) {
                    return set("d", value);
                }

                public technology e(String value) {
                    return set("e", value);
                }

                public technology f(String value) {
                    return set("f", value);
                }

                public technology g(String value) {
                    return set("g", value);
                }

                public technology h(String value) {
                    return set("h", value);
                }

                public technology i(String value) {
                    return set("i", value);
                }

                public technology j(String value) {
                    return set("j", value);
                }

                public technology k(String value) {
                    return set("k", value);
                }

                public technology l(String value) {
                    return set("l", value);
                }

                public technology m(String value) {
                    return set("m", value);
                }

                public technology n(String value) {
                    return set("n", value);
                }

                public technology o(String value) {
                    return set("o", value);
                }

                public technology p(String value) {
                    return set("p", value);
                }

                public technology q(String value) {
                    return set("q", value);
                }

                public technology r(String value) {
                    return set("r", value);
                }

                public technology s(String value) {
                    return set("s", value);
                }

                public technology t(String value) {
                    return set("t", value);
                }

                public technology u(String value) {
                    return set("u", value);
                }

                public technology v(String value) {
                    return set("v", value);
                }

                public technology w(String value) {
                    return set("w", value);
                }

                public technology x(String value) {
                    return set("x", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addColumns", field="POLICIES")
                public static class timedpolicy extends CommandRef {
                    public static final timedpolicy cmd = new timedpolicy();
                public timedpolicy sheet(String value) {
                    return set("sheet", value);
                }

                public timedpolicy a(String value) {
                    return set("a", value);
                }

                public timedpolicy b(String value) {
                    return set("b", value);
                }

                public timedpolicy c(String value) {
                    return set("c", value);
                }

                public timedpolicy d(String value) {
                    return set("d", value);
                }

                public timedpolicy e(String value) {
                    return set("e", value);
                }

                public timedpolicy f(String value) {
                    return set("f", value);
                }

                public timedpolicy g(String value) {
                    return set("g", value);
                }

                public timedpolicy h(String value) {
                    return set("h", value);
                }

                public timedpolicy i(String value) {
                    return set("i", value);
                }

                public timedpolicy j(String value) {
                    return set("j", value);
                }

                public timedpolicy k(String value) {
                    return set("k", value);
                }

                public timedpolicy l(String value) {
                    return set("l", value);
                }

                public timedpolicy m(String value) {
                    return set("m", value);
                }

                public timedpolicy n(String value) {
                    return set("n", value);
                }

                public timedpolicy o(String value) {
                    return set("o", value);
                }

                public timedpolicy p(String value) {
                    return set("p", value);
                }

                public timedpolicy q(String value) {
                    return set("q", value);
                }

                public timedpolicy r(String value) {
                    return set("r", value);
                }

                public timedpolicy s(String value) {
                    return set("s", value);
                }

                public timedpolicy t(String value) {
                    return set("t", value);
                }

                public timedpolicy u(String value) {
                    return set("u", value);
                }

                public timedpolicy v(String value) {
                    return set("v", value);
                }

                public timedpolicy w(String value) {
                    return set("w", value);
                }

                public timedpolicy x(String value) {
                    return set("x", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addColumns", field="TREATIES")
                public static class treaty extends CommandRef {
                    public static final treaty cmd = new treaty();
                public treaty sheet(String value) {
                    return set("sheet", value);
                }

                public treaty a(String value) {
                    return set("a", value);
                }

                public treaty b(String value) {
                    return set("b", value);
                }

                public treaty c(String value) {
                    return set("c", value);
                }

                public treaty d(String value) {
                    return set("d", value);
                }

                public treaty e(String value) {
                    return set("e", value);
                }

                public treaty f(String value) {
                    return set("f", value);
                }

                public treaty g(String value) {
                    return set("g", value);
                }

                public treaty h(String value) {
                    return set("h", value);
                }

                public treaty i(String value) {
                    return set("i", value);
                }

                public treaty j(String value) {
                    return set("j", value);
                }

                public treaty k(String value) {
                    return set("k", value);
                }

                public treaty l(String value) {
                    return set("l", value);
                }

                public treaty m(String value) {
                    return set("m", value);
                }

                public treaty n(String value) {
                    return set("n", value);
                }

                public treaty o(String value) {
                    return set("o", value);
                }

                public treaty p(String value) {
                    return set("p", value);
                }

                public treaty q(String value) {
                    return set("q", value);
                }

                public treaty r(String value) {
                    return set("r", value);
                }

                public treaty s(String value) {
                    return set("s", value);
                }

                public treaty t(String value) {
                    return set("t", value);
                }

                public treaty u(String value) {
                    return set("u", value);
                }

                public treaty v(String value) {
                    return set("v", value);
                }

                public treaty w(String value) {
                    return set("w", value);
                }

                public treaty x(String value) {
                    return set("x", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addColumns", field="TREATY_TYPES")
                public static class treatytype extends CommandRef {
                    public static final treatytype cmd = new treatytype();
                public treatytype sheet(String value) {
                    return set("sheet", value);
                }

                public treatytype a(String value) {
                    return set("a", value);
                }

                public treatytype b(String value) {
                    return set("b", value);
                }

                public treatytype c(String value) {
                    return set("c", value);
                }

                public treatytype d(String value) {
                    return set("d", value);
                }

                public treatytype e(String value) {
                    return set("e", value);
                }

                public treatytype f(String value) {
                    return set("f", value);
                }

                public treatytype g(String value) {
                    return set("g", value);
                }

                public treatytype h(String value) {
                    return set("h", value);
                }

                public treatytype i(String value) {
                    return set("i", value);
                }

                public treatytype j(String value) {
                    return set("j", value);
                }

                public treatytype k(String value) {
                    return set("k", value);
                }

                public treatytype l(String value) {
                    return set("l", value);
                }

                public treatytype m(String value) {
                    return set("m", value);
                }

                public treatytype n(String value) {
                    return set("n", value);
                }

                public treatytype o(String value) {
                    return set("o", value);
                }

                public treatytype p(String value) {
                    return set("p", value);
                }

                public treatytype q(String value) {
                    return set("q", value);
                }

                public treatytype r(String value) {
                    return set("r", value);
                }

                public treatytype s(String value) {
                    return set("s", value);
                }

                public treatytype t(String value) {
                    return set("t", value);
                }

                public treatytype u(String value) {
                    return set("u", value);
                }

                public treatytype v(String value) {
                    return set("v", value);
                }

                public treatytype w(String value) {
                    return set("w", value);
                }

                public treatytype x(String value) {
                    return set("x", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addColumns", field="USERS")
                public static class user extends CommandRef {
                    public static final user cmd = new user();
                public user sheet(String value) {
                    return set("sheet", value);
                }

                public user a(String value) {
                    return set("a", value);
                }

                public user b(String value) {
                    return set("b", value);
                }

                public user c(String value) {
                    return set("c", value);
                }

                public user d(String value) {
                    return set("d", value);
                }

                public user e(String value) {
                    return set("e", value);
                }

                public user f(String value) {
                    return set("f", value);
                }

                public user g(String value) {
                    return set("g", value);
                }

                public user h(String value) {
                    return set("h", value);
                }

                public user i(String value) {
                    return set("i", value);
                }

                public user j(String value) {
                    return set("j", value);
                }

                public user k(String value) {
                    return set("k", value);
                }

                public user l(String value) {
                    return set("l", value);
                }

                public user m(String value) {
                    return set("m", value);
                }

                public user n(String value) {
                    return set("n", value);
                }

                public user o(String value) {
                    return set("o", value);
                }

                public user p(String value) {
                    return set("p", value);
                }

                public user q(String value) {
                    return set("q", value);
                }

                public user r(String value) {
                    return set("r", value);
                }

                public user s(String value) {
                    return set("s", value);
                }

                public user t(String value) {
                    return set("t", value);
                }

                public user u(String value) {
                    return set("u", value);
                }

                public user v(String value) {
                    return set("v", value);
                }

                public user w(String value) {
                    return set("w", value);
                }

                public user x(String value) {
                    return set("x", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.filter.PlaceholdersMap.class,method="addColumns", field="WARS")
                public static class war extends CommandRef {
                    public static final war cmd = new war();
                public war sheet(String value) {
                    return set("sheet", value);
                }

                public war a(String value) {
                    return set("a", value);
                }

                public war b(String value) {
                    return set("b", value);
                }

                public war c(String value) {
                    return set("c", value);
                }

                public war d(String value) {
                    return set("d", value);
                }

                public war e(String value) {
                    return set("e", value);
                }

                public war f(String value) {
                    return set("f", value);
                }

                public war g(String value) {
                    return set("g", value);
                }

                public war h(String value) {
                    return set("h", value);
                }

                public war i(String value) {
                    return set("i", value);
                }

                public war j(String value) {
                    return set("j", value);
                }

                public war k(String value) {
                    return set("k", value);
                }

                public war l(String value) {
                    return set("l", value);
                }

                public war m(String value) {
                    return set("m", value);
                }

                public war n(String value) {
                    return set("n", value);
                }

                public war o(String value) {
                    return set("o", value);
                }

                public war p(String value) {
                    return set("p", value);
                }

                public war q(String value) {
                    return set("q", value);
                }

                public war r(String value) {
                    return set("r", value);
                }

                public war s(String value) {
                    return set("s", value);
                }

                public war t(String value) {
                    return set("t", value);
                }

                public war u(String value) {
                    return set("u", value);
                }

                public war v(String value) {
                    return set("v", value);
                }

                public war w(String value) {
                    return set("w", value);
                }

                public war x(String value) {
                    return set("x", value);
                }

                }
            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.CustomSheetCommands.class,method="listSheetTemplates")
            public static class list extends CommandRef {
                public static final list cmd = new list();
            public list type(String value) {
                return set("type", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.CustomSheetCommands.class,method="deleteTemplate")
            public static class remove extends CommandRef {
                public static final remove cmd = new remove();
            public remove sheet(String value) {
                return set("sheet", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.CustomSheetCommands.class,method="deleteColumns")
            public static class remove_column extends CommandRef {
                public static final remove_column cmd = new remove_column();
            public remove_column sheet(String value) {
                return set("sheet", value);
            }

            public remove_column columns(String value) {
                return set("columns", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.CustomSheetCommands.class,method="renameTemplate")
            public static class rename extends CommandRef {
                public static final rename cmd = new rename();
            public rename sheet(String value) {
                return set("sheet", value);
            }

            public rename name(String value) {
                return set("name", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.CustomSheetCommands.class,method="viewTemplate")
            public static class view extends CommandRef {
                public static final view cmd = new view();
            public view sheet(String value) {
                return set("sheet", value);
            }

            }
        }
        public static class sheets_econ{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.BankCommands.class,method="depositSheet")
            public static class deposits extends CommandRef {
                public static final deposits cmd = new deposits();
            public deposits nations(String value) {
                return set("nations", value);
            }

            public deposits offshores(String value) {
                return set("offshores", value);
            }

            public deposits ignoreOffsets(String value) {
                return set("ignoreOffsets", value);
            }

            public deposits includeExpired(String value) {
                return set("includeExpired", value);
            }

            public deposits includeIgnored(String value) {
                return set("includeIgnored", value);
            }

            public deposits noTaxes(String value) {
                return set("noTaxes", value);
            }

            public deposits noLoans(String value) {
                return set("noLoans", value);
            }

            public deposits noGrants(String value) {
                return set("noGrants", value);
            }

            public deposits noDeposits(String value) {
                return set("noDeposits", value);
            }

            public deposits includePastDepositors(String value) {
                return set("includePastDepositors", value);
            }

            public deposits useFlowNote(String value) {
                return set("useFlowNote", value);
            }

            public deposits force(String value) {
                return set("force", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.BankCommands.class,method="revenueSheet")
            public static class revenue extends CommandRef {
                public static final revenue cmd = new revenue();
            public revenue nations(String value) {
                return set("nations", value);
            }

            public revenue sheet(String value) {
                return set("sheet", value);
            }

            public revenue snapshotTime(String value) {
                return set("snapshotTime", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.BankCommands.class,method="stockpileSheet")
            public static class stockpile extends CommandRef {
                public static final stockpile cmd = new stockpile();
            public stockpile nationFilter(String value) {
                return set("nationFilter", value);
            }

            public stockpile normalize(String value) {
                return set("normalize", value);
            }

            public stockpile forceUpdate(String value) {
                return set("forceUpdate", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.BankCommands.class,method="warchestSheet")
            public static class warchest extends CommandRef {
                public static final warchest cmd = new warchest();
            public warchest nations(String value) {
                return set("nations", value);
            }

            public warchest perInfraWarchest(String value) {
                return set("perInfraWarchest", value);
            }

            public warchest includeGrants(String value) {
                return set("includeGrants", value);
            }

            public warchest doNotNormalizeDeposits(String value) {
                return set("doNotNormalizeDeposits", value);
            }

            public warchest includeDeposits(String value) {
                return set("includeDeposits", value);
            }

            public warchest ignoreStockpileInExcess(String value) {
                return set("ignoreStockpileInExcess", value);
            }

            public warchest includeRevenueDays(String value) {
                return set("includeRevenueDays", value);
            }

            public warchest forceUpdate(String value) {
                return set("forceUpdate", value);
            }

            }
        }
        public static class sheets_ia{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="ActivitySheet")
            public static class ActivitySheet extends CommandRef {
                public static final ActivitySheet cmd = new ActivitySheet();
            public ActivitySheet nations(String value) {
                return set("nations", value);
            }

            public ActivitySheet startTime(String value) {
                return set("startTime", value);
            }

            public ActivitySheet endTime(String value) {
                return set("endTime", value);
            }

            public ActivitySheet sheet(String value) {
                return set("sheet", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="ActivitySheetFromId")
            public static class ActivitySheetFromId extends CommandRef {
                public static final ActivitySheetFromId cmd = new ActivitySheetFromId();
            public ActivitySheetFromId nationId(String value) {
                return set("nationId", value);
            }

            public ActivitySheetFromId startTime(String value) {
                return set("startTime", value);
            }

            public ActivitySheetFromId endTime(String value) {
                return set("endTime", value);
            }

            public ActivitySheetFromId sheet(String value) {
                return set("sheet", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UtilityCommands.class,method="NationSheet")
            public static class NationSheet extends CommandRef {
                public static final NationSheet cmd = new NationSheet();
            public NationSheet nations(String value) {
                return set("nations", value);
            }

            public NationSheet columns(String value) {
                return set("columns", value);
            }

            public NationSheet snapshotTime(String value) {
                return set("snapshotTime", value);
            }

            public NationSheet sheet(String value) {
                return set("sheet", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="ActivitySheetDate")
            public static class activity_date extends CommandRef {
                public static final activity_date cmd = new activity_date();
            public activity_date nations(String value) {
                return set("nations", value);
            }

            public activity_date start_time(String value) {
                return set("start_time", value);
            }

            public activity_date end_time(String value) {
                return set("end_time", value);
            }

            public activity_date by_turn(String value) {
                return set("by_turn", value);
            }

            public activity_date sheet(String value) {
                return set("sheet", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UtilityCommands.class,method="AllianceSheet")
            public static class alliance_sheet extends CommandRef {
                public static final alliance_sheet cmd = new alliance_sheet();
            public alliance_sheet nations(String value) {
                return set("nations", value);
            }

            public alliance_sheet columns(String value) {
                return set("columns", value);
            }

            public alliance_sheet sheet(String value) {
                return set("sheet", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="WarDecSheetDate")
            public static class declares_date extends CommandRef {
                public static final declares_date cmd = new declares_date();
            public declares_date nations(String value) {
                return set("nations", value);
            }

            public declares_date off(String value) {
                return set("off", value);
            }

            public declares_date def(String value) {
                return set("def", value);
            }

            public declares_date start_time(String value) {
                return set("start_time", value);
            }

            public declares_date end_time(String value) {
                return set("end_time", value);
            }

            public declares_date split_off_def(String value) {
                return set("split_off_def", value);
            }

            public declares_date by_turn(String value) {
                return set("by_turn", value);
            }

            public declares_date sheet(String value) {
                return set("sheet", value);
            }

            }
        }
        public static class sheets_milcom{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="MMRSheet")
            public static class MMRSheet extends CommandRef {
                public static final MMRSheet cmd = new MMRSheet();
            public MMRSheet nations(String value) {
                return set("nations", value);
            }

            public MMRSheet sheet(String value) {
                return set("sheet", value);
            }

            public MMRSheet forceUpdate(String value) {
                return set("forceUpdate", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="MilitaryQuality")
            public static class quality extends CommandRef {
                public static final quality cmd = new quality();
            public quality nations(String value) {
                return set("nations", value);
            }

            public quality sheet(String value) {
                return set("sheet", value);
            }

            public quality forceUpdate(String value) {
                return set("forceUpdate", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="warSheet")
            public static class war_sheet extends CommandRef {
                public static final war_sheet cmd = new war_sheet();
            public war_sheet allies(String value) {
                return set("allies", value);
            }

            public war_sheet enemies(String value) {
                return set("enemies", value);
            }

            public war_sheet startTime(String value) {
                return set("startTime", value);
            }

            public war_sheet endTime(String value) {
                return set("endTime", value);
            }

            public war_sheet includeConcludedWars(String value) {
                return set("includeConcludedWars", value);
            }

            public war_sheet sheet(String value) {
                return set("sheet", value);
            }

            }
        }
        public static class stats_other{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UtilityCommands.class,method="recruitmentRankings")
            public static class recruitmentRankings extends CommandRef {
                public static final recruitmentRankings cmd = new recruitmentRankings();
            public recruitmentRankings cutoff(String value) {
                return set("cutoff", value);
            }

            public recruitmentRankings topX(String value) {
                return set("topX", value);
            }

            public recruitmentRankings uploadFile(String value) {
                return set("uploadFile", value);
            }

            }
        }
        public static class stats_tier{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.StatCommands.class,method="attributeTierGraph")
            public static class attribute_tier_graph extends CommandRef {
                public static final attribute_tier_graph cmd = new attribute_tier_graph();
            public attribute_tier_graph metric(String value) {
                return set("metric", value);
            }

            public attribute_tier_graph groupBy(String value) {
                return set("groupBy", value);
            }

            public attribute_tier_graph coalition1(String value) {
                return set("coalition1", value);
            }

            public attribute_tier_graph coalition2(String value) {
                return set("coalition2", value);
            }

            public attribute_tier_graph factor(String value) {
                return set("factor", value);
            }

            public attribute_tier_graph includeInactives(String value) {
                return set("includeInactives", value);
            }

            public attribute_tier_graph includeApplicants(String value) {
                return set("includeApplicants", value);
            }

            public attribute_tier_graph total(String value) {
                return set("total", value);
            }

            public attribute_tier_graph attachJson(String value) {
                return set("attachJson", value);
            }

            public attribute_tier_graph attachCsv(String value) {
                return set("attachCsv", value);
            }

            public attribute_tier_graph snapshotDate(String value) {
                return set("snapshotDate", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.StatCommands.class,method="scoreTierGraph")
            public static class score extends CommandRef {
                public static final score cmd = new score();
            public score coalition1(String value) {
                return set("coalition1", value);
            }

            public score coalition2(String value) {
                return set("coalition2", value);
            }

            public score includeInactives(String value) {
                return set("includeInactives", value);
            }

            public score includeApplicants(String value) {
                return set("includeApplicants", value);
            }

            public score snapshotDate(String value) {
                return set("snapshotDate", value);
            }

            public score attachJson(String value) {
                return set("attachJson", value);
            }

            public score attachCsv(String value) {
                return set("attachCsv", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.StatCommands.class,method="strengthTierGraph")
            public static class strength extends CommandRef {
                public static final strength cmd = new strength();
            public strength coalition1(String value) {
                return set("coalition1", value);
            }

            public strength coalition2(String value) {
                return set("coalition2", value);
            }

            public strength strength_metric(String value) {
                return set("strength_metric", value);
            }

            public strength includeInactives(String value) {
                return set("includeInactives", value);
            }

            public strength includeApplicants(String value) {
                return set("includeApplicants", value);
            }

            public strength snapshotDate(String value) {
                return set("snapshotDate", value);
            }

            public strength attachJson(String value) {
                return set("attachJson", value);
            }

            public strength attachCsv(String value) {
                return set("attachCsv", value);
            }

            }
        }
        public static class stats_war{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.StatCommands.class,method="counterStats")
            public static class counterStats extends CommandRef {
                public static final counterStats cmd = new counterStats();
            public counterStats alliance(String value) {
                return set("alliance", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UtilityCommands.class,method="warRanking")
            public static class warRanking extends CommandRef {
                public static final warRanking cmd = new warRanking();
            public warRanking time(String value) {
                return set("time", value);
            }

            public warRanking attackers(String value) {
                return set("attackers", value);
            }

            public warRanking defenders(String value) {
                return set("defenders", value);
            }

            public warRanking onlyOffensives(String value) {
                return set("onlyOffensives", value);
            }

            public warRanking onlyDefensives(String value) {
                return set("onlyDefensives", value);
            }

            public warRanking normalizePerMember(String value) {
                return set("normalizePerMember", value);
            }

            public warRanking ignore2dInactives(String value) {
                return set("ignore2dInactives", value);
            }

            public warRanking rankByNation(String value) {
                return set("rankByNation", value);
            }

            public warRanking warType(String value) {
                return set("warType", value);
            }

            public warRanking statuses(String value) {
                return set("statuses", value);
            }

            }
        }
        public static class tax{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.BankCommands.class,method="allianceTaxIncome")
            public static class income_alliance extends CommandRef {
                public static final income_alliance cmd = new income_alliance();
            public income_alliance alliance(String value) {
                return set("alliance", value);
            }

            public income_alliance forceUpdate(String value) {
                return set("forceUpdate", value);
            }

            }
        }
        public static class technology{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.BuildCommands.class,method="techCost")
            public static class cost extends CommandRef {
                public static final cost cmd = new cost();
            public cost technology(String value) {
                return set("technology", value);
            }

            public cost nation(String value) {
                return set("nation", value);
            }

            public cost start_level(String value) {
                return set("start_level", value);
            }

            public cost end_level(String value) {
                return set("end_level", value);
            }

            public cost acquired_technologies(String value) {
                return set("acquired_technologies", value);
            }

            public cost sci_level(String value) {
                return set("sci_level", value);
            }

            public cost ai_level(String value) {
                return set("ai_level", value);
            }

            public cost techCostReduction(String value) {
                return set("techCostReduction", value);
            }

            public cost force_update(String value) {
                return set("force_update", value);
            }

            }
        }
        public static class test{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.TestCommands.class,method="dummy")
            public static class dummy extends CommandRef {
                public static final dummy cmd = new dummy();

            }
        }
        public static class trade{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.TradeCommands.class,method="convertedTotal")
            public static class value extends CommandRef {
                public static final value cmd = new value();
            public value resources(String value) {
                return set("resources", value);
            }

            public value normalize(String value) {
                return set("normalize", value);
            }

            public value convertType(String value) {
                return set("convertType", value);
            }

            }
        }
        public static class treaty{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UtilityCommands.class,method="nap")
            public static class gw_nap extends CommandRef {
                public static final gw_nap cmd = new gw_nap();
            public gw_nap listExpired(String value) {
                return set("listExpired", value);
            }

            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.FACommands.class,method="treaties")
            public static class list extends CommandRef {
                public static final list cmd = new list();
            public list alliances(String value) {
                return set("alliances", value);
            }

            public list treatyFilter(String value) {
                return set("treatyFilter", value);
            }

            }
        }
        @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.DiscordCommands.class,method="unregister")
        public static class unregister extends CommandRef {
            public static final unregister cmd = new unregister();
        public unregister nation(String value) {
            return set("nation", value);
        }

        public unregister force(String value) {
            return set("force", value);
        }

        }
        public static class war{
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="warcard")
            public static class card extends CommandRef {
                public static final card cmd = new card();
            public card warId(String value) {
                return set("warId", value);
            }

            }
            public static class counter{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="autocounter")
                public static class auto extends CommandRef {
                    public static final auto cmd = new auto();
                public auto enemy(String value) {
                    return set("enemy", value);
                }

                public auto attackers(String value) {
                    return set("attackers", value);
                }

                public auto max(String value) {
                    return set("max", value);
                }

                public auto pingMembers(String value) {
                    return set("pingMembers", value);
                }

                public auto skipAddMembers(String value) {
                    return set("skipAddMembers", value);
                }

                public auto sendMail(String value) {
                    return set("sendMail", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="counter")
                public static class nation extends CommandRef {
                    public static final nation cmd = new nation();
                public nation target(String value) {
                    return set("target", value);
                }

                public nation counterWith(String value) {
                    return set("counterWith", value);
                }

                public nation allowMaxOffensives(String value) {
                    return set("allowMaxOffensives", value);
                }

                public nation filterWeak(String value) {
                    return set("filterWeak", value);
                }

                public nation onlyOnline(String value) {
                    return set("onlyOnline", value);
                }

                public nation requireDiscord(String value) {
                    return set("requireDiscord", value);
                }

                public nation allowSameAlliance(String value) {
                    return set("allowSameAlliance", value);
                }

                public nation includeInactive(String value) {
                    return set("includeInactive", value);
                }

                public nation includeNonMembers(String value) {
                    return set("includeNonMembers", value);
                }

                public nation ping(String value) {
                    return set("ping", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="counterWar")
                public static class war_id extends CommandRef {
                    public static final war_id cmd = new war_id();
                public war_id war(String value) {
                    return set("war", value);
                }

                public war_id counterWith(String value) {
                    return set("counterWith", value);
                }

                public war_id allowAttackersWithMaxOffensives(String value) {
                    return set("allowAttackersWithMaxOffensives", value);
                }

                public war_id filterWeak(String value) {
                    return set("filterWeak", value);
                }

                public war_id onlyActive(String value) {
                    return set("onlyActive", value);
                }

                public war_id requireDiscord(String value) {
                    return set("requireDiscord", value);
                }

                public war_id allowSameAlliance(String value) {
                    return set("allowSameAlliance", value);
                }

                public war_id includeInactive(String value) {
                    return set("includeInactive", value);
                }

                public war_id includeNonMembers(String value) {
                    return set("includeNonMembers", value);
                }

                public war_id ping(String value) {
                    return set("ping", value);
                }

                }
            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UtilityCommands.class,method="dnr")
            public static class dnr extends CommandRef {
                public static final dnr cmd = new dnr();
            public dnr nation(String value) {
                return set("nation", value);
            }

            }
            public static class find{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="BlitzPractice")
                public static class blitztargets extends CommandRef {
                    public static final blitztargets cmd = new blitztargets();
                public blitztargets topX(String value) {
                    return set("topX", value);
                }

                public blitztargets page(String value) {
                    return set("page", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="war")
                public static class enemy extends CommandRef {
                    public static final enemy cmd = new enemy();
                public enemy targets(String value) {
                    return set("targets", value);
                }

                public enemy numResults(String value) {
                    return set("numResults", value);
                }

                public enemy attackerScore(String value) {
                    return set("attackerScore", value);
                }

                public enemy includeInactives(String value) {
                    return set("includeInactives", value);
                }

                public enemy includeApplicants(String value) {
                    return set("includeApplicants", value);
                }

                public enemy onlyPriority(String value) {
                    return set("onlyPriority", value);
                }

                public enemy onlyWeak(String value) {
                    return set("onlyWeak", value);
                }

                public enemy onlyEasy(String value) {
                    return set("onlyEasy", value);
                }

                public enemy onlyLessDev(String value) {
                    return set("onlyLessDev", value);
                }

                public enemy resultsInDm(String value) {
                    return set("resultsInDm", value);
                }

                public enemy includeStrong(String value) {
                    return set("includeStrong", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="unprotected")
                public static class unprotected extends CommandRef {
                    public static final unprotected cmd = new unprotected();
                public unprotected targets(String value) {
                    return set("targets", value);
                }

                public unprotected numResults(String value) {
                    return set("numResults", value);
                }

                public unprotected ignoreDNR(String value) {
                    return set("ignoreDNR", value);
                }

                public unprotected includeAllies(String value) {
                    return set("includeAllies", value);
                }

                public unprotected nationsToBlitzWith(String value) {
                    return set("nationsToBlitzWith", value);
                }

                public unprotected maxRelativeTargetStrength(String value) {
                    return set("maxRelativeTargetStrength", value);
                }

                public unprotected maxRelativeCounterStrength(String value) {
                    return set("maxRelativeCounterStrength", value);
                }

                public unprotected withinAllAttackersRange(String value) {
                    return set("withinAllAttackersRange", value);
                }

                public unprotected ignoreODP(String value) {
                    return set("ignoreODP", value);
                }

                public unprotected force(String value) {
                    return set("force", value);
                }

                }
            }
            @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="wars")
            public static class info extends CommandRef {
                public static final info cmd = new info();
            public info nation(String value) {
                return set("nation", value);
            }

            }
            public static class room{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="warroom")
                public static class create extends CommandRef {
                    public static final create cmd = new create();
                public create enemy(String value) {
                    return set("enemy", value);
                }

                public create attackers(String value) {
                    return set("attackers", value);
                }

                public create max(String value) {
                    return set("max", value);
                }

                public create force(String value) {
                    return set("force", value);
                }

                public create excludeWeakAttackers(String value) {
                    return set("excludeWeakAttackers", value);
                }

                public create requireDiscord(String value) {
                    return set("requireDiscord", value);
                }

                public create allowAttackersWithMaxOffensives(String value) {
                    return set("allowAttackersWithMaxOffensives", value);
                }

                public create pingMembers(String value) {
                    return set("pingMembers", value);
                }

                public create skipAddMembers(String value) {
                    return set("skipAddMembers", value);
                }

                public create sendMail(String value) {
                    return set("sendMail", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="deleteForEnemies")
                public static class delete_for_enemies extends CommandRef {
                    public static final delete_for_enemies cmd = new delete_for_enemies();
                public delete_for_enemies enemy_rooms(String value) {
                    return set("enemy_rooms", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="deletePlanningChannel")
                public static class delete_planning extends CommandRef {
                    public static final delete_planning cmd = new delete_planning();

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="warRoomSheet")
                public static class from_sheet extends CommandRef {
                    public static final from_sheet cmd = new from_sheet();
                public from_sheet blitzSheet(String value) {
                    return set("blitzSheet", value);
                }

                public from_sheet customMessage(String value) {
                    return set("customMessage", value);
                }

                public from_sheet addCounterMessage(String value) {
                    return set("addCounterMessage", value);
                }

                public from_sheet ping(String value) {
                    return set("ping", value);
                }

                public from_sheet addMember(String value) {
                    return set("addMember", value);
                }

                public from_sheet allowedNations(String value) {
                    return set("allowedNations", value);
                }

                public from_sheet headerRow(String value) {
                    return set("headerRow", value);
                }

                public from_sheet useLeader(String value) {
                    return set("useLeader", value);
                }

                public from_sheet force(String value) {
                    return set("force", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="warRoomList")
                public static class list extends CommandRef {
                    public static final list cmd = new list();
                public list nation(String value) {
                    return set("nation", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="warpin")
                public static class pin extends CommandRef {
                    public static final pin cmd = new pin();

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.AdminCommands.class,method="purgeWarRooms")
                public static class purge extends CommandRef {
                    public static final purge cmd = new purge();
                public purge channel(String value) {
                    return set("channel", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="warcat")
                public static class setCategory extends CommandRef {
                    public static final setCategory cmd = new setCategory();
                public setCategory category(String value) {
                    return set("category", value);
                }

                }
            }
            public static class sheet{
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="blitzSheet")
                public static class blitzsheet extends CommandRef {
                    public static final blitzsheet cmd = new blitzsheet();
                public blitzsheet attNations(String value) {
                    return set("attNations", value);
                }

                public blitzsheet defNations(String value) {
                    return set("defNations", value);
                }

                public blitzsheet maxOff(String value) {
                    return set("maxOff", value);
                }

                public blitzsheet sameAAPriority(String value) {
                    return set("sameAAPriority", value);
                }

                public blitzsheet sameActivityPriority(String value) {
                    return set("sameActivityPriority", value);
                }

                public blitzsheet hour(String value) {
                    return set("hour", value);
                }

                public blitzsheet attActivity(String value) {
                    return set("attActivity", value);
                }

                public blitzsheet defActivity(String value) {
                    return set("defActivity", value);
                }

                public blitzsheet processActiveWars(String value) {
                    return set("processActiveWars", value);
                }

                public blitzsheet onlyEasyTargets(String value) {
                    return set("onlyEasyTargets", value);
                }

                public blitzsheet maxStrengthRatio(String value) {
                    return set("maxStrengthRatio", value);
                }

                public blitzsheet maxDevelopmentRatio(String value) {
                    return set("maxDevelopmentRatio", value);
                }

                public blitzsheet sheet(String value) {
                    return set("sheet", value);
                }

                }
                @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.WarCommands.class,method="ValidateBlitzSheet")
                public static class validate extends CommandRef {
                    public static final validate cmd = new validate();
                public validate sheet(String value) {
                    return set("sheet", value);
                }

                public validate maxWars(String value) {
                    return set("maxWars", value);
                }

                public validate nationsFilter(String value) {
                    return set("nationsFilter", value);
                }

                public validate attackerFilter(String value) {
                    return set("attackerFilter", value);
                }

                public validate useLeader(String value) {
                    return set("useLeader", value);
                }

                public validate headerRow(String value) {
                    return set("headerRow", value);
                }

                }
            }
        }
        @AutoRegister(clazz=link.locutus.discord.commands.manager.v2.impl.pw.commands.UtilityCommands.class,method="who")
        public static class who extends CommandRef {
            public static final who cmd = new who();
        public who nationOrAlliances(String value) {
            return set("nationOrAlliances", value);
        }

        public who sortBy(String value) {
            return set("sortBy", value);
        }

        public who list(String value) {
            return set("list", value);
        }

        public who listAlliances(String value) {
            return set("listAlliances", value);
        }

        public who listRawUserIds(String value) {
            return set("listRawUserIds", value);
        }

        public who listMentions(String value) {
            return set("listMentions", value);
        }

        public who listInfo(String value) {
            return set("listInfo", value);
        }

        public who listChannels(String value) {
            return set("listChannels", value);
        }

        public who snapshotDate(String value) {
            return set("snapshotDate", value);
        }

        public who page(String value) {
            return set("page", value);
        }

        }

}

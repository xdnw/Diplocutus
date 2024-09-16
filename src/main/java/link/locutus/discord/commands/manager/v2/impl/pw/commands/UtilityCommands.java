package link.locutus.discord.commands.manager.v2.impl.pw.commands;

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import link.locutus.discord.Locutus;
import link.locutus.discord.api.generated.WarType;
import link.locutus.discord.api.types.*;
import link.locutus.discord.api.types.tx.Transaction2;
import link.locutus.discord.commands.manager.v2.binding.annotation.*;
import link.locutus.discord.commands.manager.v2.binding.annotation.TextArea;
import link.locutus.discord.commands.manager.v2.binding.annotation.Timestamp;
import link.locutus.discord.commands.manager.v2.binding.bindings.PlaceholderCache;
import link.locutus.discord.commands.manager.v2.command.CommandBehavior;
import link.locutus.discord.commands.manager.v2.command.IMessageBuilder;
import link.locutus.discord.commands.manager.v2.command.IMessageIO;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.IsAlliance;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.RolePermission;
import link.locutus.discord.commands.manager.v2.impl.pw.binding.NationAttributeDouble;
import link.locutus.discord.commands.manager.v2.impl.pw.binding.PWBindings;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.commands.manager.v2.impl.pw.filter.AlliancePlaceholders;
import link.locutus.discord.commands.manager.v2.impl.pw.filter.NationPlaceholders;
import link.locutus.discord.commands.manager.v2.builder.GroupedRankBuilder;
import link.locutus.discord.commands.manager.v2.builder.RankBuilder;
import link.locutus.discord.commands.manager.v2.builder.SummedMapRankBuilder;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.*;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.components.NationPrivate;
import link.locutus.discord.db.entities.metric.AllianceMetric;
import link.locutus.discord.db.guild.GuildKey;
import link.locutus.discord.db.guild.SheetKey;
import link.locutus.discord.pnw.*;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.*;
import link.locutus.discord.util.discord.DiscordUtil;
import link.locutus.discord.util.io.PagePriority;
import link.locutus.discord.util.math.ArrayUtil;
import link.locutus.discord.util.offshore.test.IACategory;
import link.locutus.discord.util.offshore.test.IAChannel;
import link.locutus.discord.util.sheet.SpreadSheet;
import link.locutus.discord.util.task.roles.AutoRoleInfo;
import link.locutus.discord.util.task.roles.IAutoRoleTask;
import link.locutus.discord.api.generated.ResourceType;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.json.JSONObject;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static link.locutus.discord.util.math.ArrayUtil.memorize;

public class UtilityCommands {
    @Command(desc = "list channels")
    @RolePermission(Roles.ADMIN)
    public String channelCount(@Me IMessageIO channel, @Me Guild guild) {
        StringBuilder channelList = new StringBuilder();

        List<Category> categories = guild.getCategories();
        for (Category category : categories) {
            channelList.append(category.getName() + "\n");

            for (GuildChannel catChannel : category.getChannels()) {
                String prefix = "+ ";
                if (catChannel instanceof VoiceChannel) {
                    prefix = "\uD83D\uDD0A ";
                }
                channelList.append(prefix + catChannel.getName() + "\n");
            }

            channelList.append("\n");
        }

        channel.create().file(guild.getChannels().size() + "/500 channels", channelList.toString()).send();
        return null;
    }

//    @Command(desc = "See who was online at the time of a spy op (UTC)")
//    @RolePermission(Roles.MEMBER)
//    @WhitelistPermission
//    public String findSpyOp(@Me DBNation me, String times, int defenderSpies, @Default DBNation defender) {
//        if (defender == null) defender = me;
//
//        Set<Integer> ids = new HashSet<>();
//        Map<DBSpyUpdate, Long> updatesTmp = new HashMap<>();
//        long interval = TimeUnit.MINUTES.toMillis(3);
//
//        for (String timeStr : times.split(",")) {
//            long timestamp = TimeUtil.parseDate(TimeUtil.MMDD_HH_MM_A, timeStr, true);
//            List<DBSpyUpdate> updates = Locutus.imp().getNationDB().getSpyActivity(timestamp, interval);
//            for (DBSpyUpdate update : updates) {
////                nations.put(update.nation_id, nations.getOrDefault(update.nation_id, 0) + 1);
//                DBNation nation = DBNation.getById(update.nation_id);
//                if (nation == null) continue;
//                if (!defender.isInSpyRange(nation)) continue;
//
//                if (ids.contains(update.nation_id)) continue;
//                ids.add(update.nation_id);
//                updatesTmp.put(update, Math.abs(timestamp - update.timestamp));
//
//            }
//        }
//
//        if (updatesTmp.isEmpty()) return "No results (0)";
//
//        Map<DBNation, Map.Entry<Double, String>> allOdds = new HashMap<>();
//
//        for (Map.Entry<DBSpyUpdate, Long> entry : updatesTmp.entrySet()) {
//            DBSpyUpdate update = entry.getKey();
//            long diff = entry.getValue();
////            if (update.spies <= 0) continue;
////            if (update.change == 0) continue;
//
//            DBNation attacker = Locutus.imp().getNationDB().getNation(update.nation_id);
//            if (attacker == null || (defender != null && !attacker.isInSpyRange(defender)) || attacker.getNation_id() == defender.getNation_id()) continue;
//
//            int spiesUsed = update.spies;
//
//
//            int safety = 3;
//            int uncertainty = -1;
//            boolean foundOp = false;
//            boolean spySatellite = Projects.SPY_SATELLITE.hasBit(update.projects);
//            boolean intelligence = Projects.INTELLIGENCE_AGENCY.hasBit(update.projects);
//
//            if (spiesUsed == -1) spiesUsed = attacker.getSpies();
//
//            double odds = SpyCount.getOdds(spiesUsed, defenderSpies, safety, SpyCount.Operation.SPIES, defender);
//            if (spySatellite) odds = Math.min(100, odds * 1.2);
////            if (odds < 10) continue;
//
//            double ratio = odds;
//
//            int numOps = (int) Math.ceil((double) spiesUsed / attacker.getSpies());
//
//            if (!foundOp) {
//                ratio -= ratio * 0.1 * Math.abs((double) diff / interval);
//
//                ratio *= 0.1;
//
//                if (attacker.getPosition() <= 1) ratio *= 0.1;
//            } else {
//                ratio -= 0.1 * ratio * uncertainty;
//
//                if (attacker.getPosition() <= 1) ratio *= 0.5;
//            }
//
//            StringBuilder message = new StringBuilder();
//
//            if (foundOp) message.append("**");
//            message.append(MathMan.format(odds) + "%");
//            if (spySatellite) message.append(" | SAT");
//            if (intelligence) message.append(" | IA");
//            message.append(" | " + spiesUsed + "? spies (" + safety + ")");
//            long diff_m = Math.abs(diff / TimeUnit.MINUTES.toMillis(1));
//            message.append(" | " + diff_m + "m");
//            if (foundOp) message.append("**");
//
//            allOdds.put(attacker, new AbstractMap.SimpleEntry<>(ratio, message.toString()));
//        }
//
//        List<Map.Entry<DBNation, Map.Entry<Double, String>>> sorted = new ArrayList<>(allOdds.entrySet());
//        sorted.sort((o1, o2) -> Double.compare(o2.getValue().getKey(), o1.getValue().getKey()));
//
//        if (sorted.isEmpty()) {
//            return "No results";
//        }
//
//        StringBuilder response = new StringBuilder();
//        for (Map.Entry<DBNation, Map.Entry<Double, String>> entry : sorted) {
//            DBNation att = entry.getKey();
//
//            response.append(att.getNation() + " | " + att.getAllianceName() + "\n");
//            response.append(entry.getValue().getValue()).append("\n\n");
//        }
//
//        return response.toString();
//    }
    @Command(aliases = {"nap", "napdown"})

    public String nap(@Default("false") boolean listExpired) {
        Map<Long, String> naps = new LinkedHashMap<>();
        // TODO FIXME :||remove
        int skippedExpired = 0;

        StringBuilder response = new StringBuilder();
        long now = System.currentTimeMillis();
        for (Map.Entry<Long, String> entry : naps.entrySet()) {
            long timeEnd = entry.getKey();
            if (timeEnd < now && !listExpired) {
                skippedExpired++;
                continue;
            }
            response.append("## " + DiscordUtil.timestamp(timeEnd, null) + ":\n");
            response.append("- " + StringMan.join(entry.getValue().split("\n"), "\n- ") + "\n\n");
        }

        if (response.length() == 0) {
            if (skippedExpired > 0) {
                response.append("Skipped " + skippedExpired + " expired NAPs, set `listExpired: True` to include\n");
            }
            response.append("No active NAPs");
        }
        return response.toString();
    }

    @Command(desc = "Rank the number of wars between two coalitions by nation or alliance\n" +
            "Defaults to alliance ranking")
    public String warRanking(@Me JSONObject command, @Me IMessageIO channel, @Timestamp long time, Set<NationOrAlliance> attackers, Set<NationOrAlliance> defenders,
                             @Arg("Only include offensive wars in the ranking")
                             @Switch("o") boolean onlyOffensives,
                             @Arg("Only include defensive wars in the ranking")
                             @Switch("d") boolean onlyDefensives,
                             @Arg("Rank the average wars per alliance member")
                             @Switch("n") boolean normalizePerMember,
                             @Arg("Ignore inactive nations when determining alliance member counts")
                             @Switch("i") boolean ignore2dInactives,
                             @Arg("Rank by nation instead of alliance")
                             @Switch("a") boolean rankByNation,
                             @Arg("Only rank these war types")
                             @Switch("t") WarType warType,
                             @Arg("Only rank wars with these statuses")
                             @Switch("s") Set<WarStatus> statuses) {
        WarParser parser = WarParser.of(attackers, defenders, time, Long.MAX_VALUE);
        Map<Integer, DBWar> wars = parser.getWars();

        SummedMapRankBuilder<Integer, Double> ranksUnsorted = new RankBuilder<>(wars.values()).group(new BiConsumer<DBWar, GroupedRankBuilder<Integer, DBWar>>() {
            @Override
            public void accept(DBWar dbWar, GroupedRankBuilder<Integer, DBWar> builder) {
                if (warType != null && dbWar.getWarType() != warType) return;
                if (statuses != null && !statuses.contains(dbWar.getStatus())) return;
                if (!rankByNation) {
                    if (dbWar.getAttacker_aa() != 0 && !onlyDefensives) builder.put(dbWar.getAttacker_aa(), dbWar);
                    if (dbWar.getDefender_aa() != 0 && !onlyOffensives) builder.put(dbWar.getDefender_aa(), dbWar);
                } else {
                    if (!onlyDefensives) builder.put(dbWar.getAttacker_id(), dbWar);
                    if (!onlyOffensives) builder.put(dbWar.getDefender_id(), dbWar);
                }
            }
        }).sumValues(f -> 1d);
        if (normalizePerMember && !rankByNation) {
            ranksUnsorted = ranksUnsorted.adapt((aaId, numWars) -> {
                int num = DBAlliance.getOrCreate(aaId).getNations(true, ignore2dInactives ? 2440 : Integer.MAX_VALUE, true).size();
                if (num == 0) return 0d;
                return numWars / (double) num;
            });
        }

        RankBuilder<String> ranks = ranksUnsorted.sort().nameKeys(i -> DNS.getName(i, !rankByNation));
        String offOrDef ="";
        if (onlyOffensives != onlyDefensives) {
            if (!onlyDefensives) offOrDef = "offensive ";
            else offOrDef = "defensive ";
        }

        String title = "Most " + offOrDef + "wars (" + TimeUtil.secToTime(TimeUnit.MILLISECONDS, System.currentTimeMillis() - time) +")";
        if (normalizePerMember) title += "(per " + (ignore2dInactives ? "active " : "") + "nation)";

        ranks.build(channel, command, title, true);

        return null;
    }

    @Command(desc = "Calculate the score of various things. Each argument is option, and can go in any order")
    public String score(@Default DBNation nation,
                        @Default Double EducationIndex,
                        @Default Double PowerIndex,
                        @Default Double EmploymentIndex,
                        @Default Double TransportationIndex,
                        @Default Double StabilityIndex,
                        @Default Double CommerceIndex,
                        @Default Double Development,
                        @Default Double Land,
                        @Default Double Devastation,
                        @Default Double WarIndex,
                        @Default Double TechIndex,
                        @Default Double UnfilledJobsPenality
    ) {
        if (UnfilledJobsPenality == null) UnfilledJobsPenality = nation.estimateUnfilledJobsPenalty();
        else if (UnfilledJobsPenality < 1) throw new IllegalArgumentException("UnfilledJobsPenality must be >= 1");
        if (EducationIndex == null) EducationIndex = nation.getEducationIndex();
        if (PowerIndex == null) PowerIndex = nation.getPowerIndex();
        if (EmploymentIndex == null) EmploymentIndex = nation.getEmploymentIndex();
        if (TransportationIndex == null) TransportationIndex = nation.getTransportationIndex();
        if (StabilityIndex == null) StabilityIndex = nation.getStabilityIndex();
        if (CommerceIndex == null) CommerceIndex = nation.getCommerceIndex();
        if (Development == null) Development = nation.getInfra();
        if (Land == null) Land = nation.getLand();
        if (Devastation == null) Devastation = nation.getDevastation();
        if (WarIndex == null) WarIndex = nation.getWarIndex();
        if (TechIndex == null) TechIndex = nation.getTechIndex();
        double score = DNS.estimateScore(EducationIndex, PowerIndex, EmploymentIndex, TransportationIndex, StabilityIndex, CommerceIndex, Development, Land, Devastation, WarIndex, TechIndex, UnfilledJobsPenality);
        if (score == 0) throw new IllegalArgumentException("No arguments provided");
        return "Score: " + MathMan.format(score) + "\n" +
                "WarRange: " + MathMan.format(score * DNS.WAR_RANGE_MIN_MODIFIER_ACTIVE) + "- " + MathMan.format(score * DNS.WAR_RANGE_MAX_MODIFIER_ACTIVE) + "\n" +
                "Can be Attacked By: " + MathMan.format(score / DNS.WAR_RANGE_MAX_MODIFIER_ACTIVE) + "- " + MathMan.format(score / DNS.WAR_RANGE_MIN_MODIFIER_ACTIVE) + "\n" +
                "Spy range: " + MathMan.format(score * DNS.ESPIONAGE_RANGE_MIN_MODIFIER) + "- " + MathMan.format(score * DNS.ESPIONAGE_RANGE_MAX_MODIFIER);
    }

    @Command(desc = "Add or remove the configured auto roles to all users in this discord guild")
    @RolePermission(Roles.INTERNAL_AFFAIRS)
    public static String autoroleall(@Me GuildDB db, @Me IMessageIO channel, @Me JSONObject command, @Switch("f") boolean force) {
        IAutoRoleTask task = db.getAutoRoleTask();
        task.syncDB();

        AutoRoleInfo result = task.autoRoleAll();
        if (force) {
            channel.send("Please wait...");
            result.execute();
            return result.getChangesAndErrorMessage();
        }

        String body = "`note: Results may differ if settings or users change`\n" +
                result.getSyncDbResult();
        String resultStr = result.toString();
        if (body.length() + resultStr.length() < 2000) {
            body += "\n\n------------\n\n" + resultStr;
        }
        IMessageBuilder msg = channel.create().confirmation("Auto role all", body, command);
        if (body.length() + resultStr.length() >= 2000) {
            msg = msg.file("role_changes.txt", result.toString());
        }

        if (db.hasAlliance()) {
            StringBuilder response = new StringBuilder();
            for (Map.Entry<Member, GuildDB.UnmaskedReason> entry : db.getMaskedNonMembers().entrySet()) {
                User user = entry.getKey().getUser();
                response.append("User: `" + DiscordUtil.getFullUsername(user).replace("_", "\\_") + "`" + " `<@" + user.getIdLong() + ">` ");
                DBNation nation = DiscordUtil.getNation(user);
                if (nation != null) {
                    String active = TimeUtil.secToTime(TimeUnit.MINUTES, nation.active_m());
                    if (nation.active_m() > 10000) active = "**" + active + "**";
                    response.append(nation.getName() + " | <" + nation.getUrl() + "> | " + active + " | " + nation.getPositionEnum() + " in AA:" + nation.getAllianceName());
                }
                response.append("- ").append(entry.getValue());
                response.append("\n");
            }
            if (response.length() > 0) {
                msg.append(response.toString());
            }
        }

        msg.send();
        return null;
    }

    @Command(desc = "Give the configured bot auto roles to a user on discord")
    public static String autorole(@Me GuildDB db, @Me IMessageIO channel, @Me JSONObject command, Member member, @Switch("f") boolean force) {
        IAutoRoleTask task = db.getAutoRoleTask();
        task.syncDB();

        DBNation nation = DiscordUtil.getNation(member.getUser());
        if (nation == null) return "That nation isn't registered: " + CM.register.cmd.toSlashMention();
        AutoRoleInfo result = task.autoRole(member, nation);
        if (force) {
            result.execute();
            return result.getChangesAndErrorMessage();
        }

        String body = "`note: Results may differ if settings or users change`\n" +
                result.getSyncDbResult() + "\n------\n" + result.toString();
        channel.create().confirmation("Auto role " + nation.getNation(), body, command).send();
        return null;
    }

    @RolePermission(value = {Roles.MILCOM, Roles.INTERNAL_AFFAIRS,Roles.ECON,Roles.FOREIGN_AFFAIRS}, any=true)
    @Command(desc = "Create a sheet of alliances with customized columns\n" +
            "See <https://github.com/xdnw/diplocutus/wiki/nation_placeholders> for a list of placeholders")
    @NoFormat
    public String AllianceSheet(AlliancePlaceholders aaPlaceholders, @Me Guild guild, @Me IMessageIO channel, @Me DBNation me, @Me User author, @Me GuildDB db,
                                @Arg("The nations to include in each alliance")
                                Set<DBNation> nations,
                                @Arg("The columns to use in the sheet")
                                @TextArea List<String> columns,
                                @Switch("s") SpreadSheet sheet) throws GeneralSecurityException, IOException {
        if (sheet == null) {
            sheet = SpreadSheet.create(db, SheetKey.ALLIANCES_SHEET);
        }
        List<String> header = new ArrayList<>(columns);
        for (int i = 0; i < header.size(); i++) {
            String arg = header.get(i);
            arg = arg.replace("{", "").replace("}", "").replace("=", "");
            header.set(i, arg);
        }

        Map<Integer, List<DBNation>> nationMap = new RankBuilder<>(nations).group(n -> n.getAlliance_id()).get();
        Set<DBAlliance> alliances = nationMap.keySet().stream().map(DBAlliance::get).filter(Objects::nonNull).collect(Collectors.toSet());

        sheet.setHeader(header);

        PlaceholderCache<DBAlliance> aaCache = new PlaceholderCache<>(alliances);
        List<Function<DBAlliance, String>> formatByColumn = new ArrayList<>();
        for (String column : columns) {
            formatByColumn.add(aaPlaceholders.getFormatFunction(guild, me, author, column, aaCache, true));
        }
        for (DBAlliance alliance : alliances) {
            for (int i = 0; i < columns.size(); i++) {
                Function<DBAlliance, String> formatter = formatByColumn.get(i);
                String formatted = formatter.apply(alliance);
                header.set(i, formatted);
            }
            sheet.addRow(new ArrayList<>(header));
        }

        sheet.updateClearCurrentTab();
        sheet.updateWrite();

        sheet.attach(channel.create(), "alliances").send();
        return null;
    }

    @RolePermission(value = {Roles.MILCOM, Roles.ECON, Roles.INTERNAL_AFFAIRS}, any=true)
    @Command(desc = "A sheet of nations stats with customizable columns\n" +
            "See <https://github.com/xdnw/diplocutus/wiki/nation_placeholders> for a list of placeholders")
    @NoFormat
    public static void NationSheet(NationPlaceholders placeholders, @Me IMessageIO channel, @Me DBNation me, @Me User author, @Me GuildDB db,
                                   NationList nations,
                                   @Arg("A space separated list of columns to use in the sheet\n" +
                                           "Can include NationAttribute as placeholders in columns\n" +
                                           "All NationAttribute placeholders must be surrounded by {} e.g. {nation}")
                                   @TextArea List<String> columns,
                                   @Switch("t") @Timestamp Long snapshotTime, @Switch("s") SpreadSheet sheet) throws GeneralSecurityException, IOException {
        Set<DBNation> nationSet = DNS.getNationsSnapshot(nations.getNations(), nations.getFilter(), snapshotTime, db.getGuild(), true);
        if (sheet == null) {
            sheet = SpreadSheet.create(db, SheetKey.NATION_SHEET);
        }
        List<String> header = new ArrayList<>(columns);
        for (int i = 0; i < header.size(); i++) {
            String arg = header.get(i);
            if (arg.startsWith("=")) arg = "'" + arg;
            header.set(i, arg);
        }

        sheet.setHeader(header);

        PlaceholderCache<DBNation> cache = new PlaceholderCache<>(nationSet);
        List<Function<DBNation, String>> formatFunction = new ArrayList<>();
        for (String arg : columns) {
            formatFunction.add(placeholders.getFormatFunction(db.getGuild(), me, author, arg, cache, true));
        }
        for (DBNation nation : nationSet) {
            for (int i = 0; i < columns.size(); i++) {
                Function<DBNation, String> formatter = formatFunction.get(i);
                String formatted = formatter.apply(nation);

                header.set(i, formatted);
            }

            sheet.addRow(new ArrayList<>(header));
        }

        sheet.updateClearCurrentTab();
        sheet.updateWrite();

        sheet.attach(channel.create(), "nations").send();
    }

    @Command(desc = "Return amount of time a nation has protection for")
    public String protectionTime(DBNation nation) {
        if (!nation.hasProtection()) return "Nation does not have protection time";
        return "Protection ends " + DiscordUtil.timestamp(nation.getProtectionEnds(), null);
    }

    // TODO FIXME :||remove quickest beige
//    @Command(desc = "Return quickest attacks to beige an enemy at a resistance level", aliases = {"fastBeige", "quickestBeige", "quickBeige", "fastestBeige"})
//    public String quickestBeige(@Range(min=1, max=100) int resistance,
//                                @Arg("Don't allow ground attacks")
//                                @Switch("g") boolean noGround,
//                                @Arg("Don't allow naval attacks")
//                                @Switch("s") boolean noShip,
//                                @Arg("Don't allow aircraft attacks")
//                                @Switch("a") boolean noAir,
//                                @Arg("Don't allow missile attacks")
//                                @Switch("m") boolean noMissile,
//                                @Arg("Don't allow nuclear attacks")
//                                @Switch("n") boolean noNuke) {
//        if (resistance > 1000 || resistance < 1) throw new IllegalArgumentException("Resistance must be between 1 and 100");
//        List<AttackType> allowed = new ArrayList<>(List.of(AttackType.values));
//        if (noGround) allowed.removeIf(f -> f == AttackType.GROUND);
//        if (noShip) allowed.removeIf(f -> f.getUnits().length > 0 && f.getUnits()[0] == MilitaryUnit.SHIP);
//        if (noAir) allowed.removeIf(f -> f.getUnits().length > 0 && f.getUnits()[0] == MilitaryUnit.AIRCRAFT);
//        if (noMissile) allowed.removeIf(f -> f.getUnits().length > 0 && f.getUnits()[0] == MilitaryUnit.MISSILE);
//        if (noNuke) allowed.removeIf(f -> f.getUnits().length > 0 && f.getUnits()[0] == MilitaryUnit.NUKE);
//        AttackTypeNode best = AttackTypeNode.findQuickest(allowed, resistance);
//
//        return "Result: " + best.toString() + " MAP: " + best.map + " resistance:" + best.resistance;
//    }

    @Command(desc = "Get info about your own nation")
    public String me(@Me JSONObject command, @Me Guild guild, @Me IMessageIO channel, @Me DBNation me, @Me User author, @Me GuildDB db, @Switch("s") @Timestamp Long snapshotDate) throws IOException {
        return who(command, guild, channel, author, db, me, Collections.singleton(me), null, false, false, false, false, false, false, snapshotDate, null);
    }

    @Command(aliases = {"who", "who", "pw-info", "how", "where", "when", "why", "whois"},
            desc = "Get detailed information about a nation\n" +
                    "Nation argument can be nation name, id, link, or discord tag\n" +
                    "e.g. `{prefix}who @borg`")
    public static String who(@Me JSONObject command, @Me Guild guild, @Me IMessageIO channel, @Me User author, @Me GuildDB db, @Me @Default DBNation me,
                      @Arg("The nations to get info about")
                      Set<NationOrAlliance> nationOrAlliances,
                      @Arg("Sort any listed nations by this attribute")
                      @Default() NationAttributeDouble sortBy,
                      @Arg("List the nations instead of just providing a summary")
                      @Switch("l") boolean list,
                      @Arg("List the alliances of the provided nation")
                      @Switch("a") boolean listAlliances,
                      @Arg("List the discord user ids of each nation")
                      @Switch("r") boolean listRawUserIds,
                      @Arg("List the discord user mentions of each nation")
                      @Switch("m") boolean listMentions,
                      @Arg("List paginated info of each nation")
                      @Switch("i") boolean listInfo,
                      @Arg("List all interview channels of each nation")
                      @Switch("c") boolean listChannels,
                      @Switch("s") @Timestamp Long snapshotDate,
                      @Switch("p") Integer page) throws IOException {
        DBNation myNation = DiscordUtil.getNation(author.getIdLong());
        int perpage = 15;
        StringBuilder response = new StringBuilder();
        String filter = command.has("nationoralliances") ? command.getString("nationoralliances") : null;
        if (filter == null && me != null) filter = me.getQualifiedId();
        final Set<DBNation> nations = DNS.getNationsSnapshot(SimpleNationList.from(nationOrAlliances).getNations(), filter, snapshotDate, db.getGuild(), true);

        String arg0;
        String title;
        if (nationOrAlliances.size() == 1) {
            NationOrAlliance nationOrAA = nationOrAlliances.iterator().next();
            if (nationOrAA.isNation()) {
                DBNation nation = nationOrAA.asNation();
                title = nation.getNation();
                StringBuilder markdown = new StringBuilder(nation.toFullMarkdown());
                IMessageBuilder msg = channel.create().embed(title, markdown.toString());
                msg.send();
            } else {
                if (snapshotDate != null) {
                    throw new IllegalArgumentException("You specified a `snapshotDate`, but alliance snapshots are not currently supported.");
                }
                DBAlliance alliance = nationOrAA.asAlliance();
                title = alliance.getName();
                StringBuilder markdown = new StringBuilder(alliance.toMarkdown() + "\n");
                if (Roles.ADMIN.has(author, db.getGuild()) && myNation != null && myNation.getAlliance_id() == alliance.getId() && db.getAllianceIds().isEmpty()) {
                    markdown.append("\nSet as this guild's alliance: " + CM.settings_default.registerAlliance.cmd.toSlashMention() + "\n");
                }

                IMessageBuilder msg = channel.create().embed(title, markdown.toString());
                msg.send();
            }
        } else {
            NationList nationList = new SimpleNationList(nations);
            int allianceId = -1;
            for (DBNation nation : nations) {
                if (allianceId == -1 || allianceId == nation.getAlliance_id()) {
                    allianceId = nation.getAlliance_id();
                } else {
                    allianceId = -2;
                }
            }
            if (allianceId != -2) {
                String name = DNS.getName(allianceId, true);
                String url = DNS.getUrl(allianceId, true);
                title = "AA: " + name;
                arg0 = MarkupUtil.markdownUrl(name, url);
            } else {
                arg0 = "coalition";
                title = "`" + arg0 + "`";
            }
            if (nations.isEmpty()) return "No nations found";
            title = "(" + nations.size() + " nations) " + title;
            IMessageBuilder msg = channel.create().embed(title, nationList.toMarkdown());

            msg = msg.commandButton(CommandBehavior.EPHEMERAL, command.put("list", "True").put("listMentions", "True"), "List");
            msg.send();
        }
        if (!listInfo && page == null && !response.isEmpty()) {
            channel.create().embed(title, response.toString()).send();
        }

        if (list || listMentions || listRawUserIds || listChannels || listAlliances) {
//            if (perpage == null) perpage = 15;
            if (page == null) page = 0;
            List<String> nationList = new ArrayList<>();

            if (listAlliances) {
                // alliances
                Set<DBAlliance> alliances = nations.stream().map(DBNation::getAlliance).filter(Objects::nonNull).collect(Collectors.toSet());
                List<DBAlliance> alliancesSorted = new ArrayList<>(alliances);
                if (sortBy != null) {
                    alliancesSorted.sort((o1, o2) -> {
                        double v1 = o1.getTotal(sortBy, null);
                        double v2 = o2.getTotal(sortBy, null);
                        return Double.compare(v2, v1);
                    });
                }
                alliancesSorted.forEach(f -> nationList.add(f.getMarkdownUrl()));
            } else {
                IACategory iaCat = listChannels && db != null ? db.getIACategory() : null;
                List<DBNation> nationsSorted = new ArrayList<>(nations);
                if (sortBy != null) {
                    nationsSorted.sort((o1, o2) -> {
                        double v1 = sortBy.apply(o1);
                        double v2 = sortBy.apply(o2);
                        return Double.compare(v2, v1);
                    });
                }

                for (DBNation nation : nationsSorted) {
                    String nationStr = list ? nation.getNationUrlMarkup(true) : "";
                    if (listMentions) {
                        RegisteredUser user = nation.getDBUser();
                        if (user != null) {
                            nationStr += (" <@" + user.getDiscordId() + ">");
                        }
                    }
                    if (listRawUserIds) {
                        RegisteredUser user = nation.getDBUser();
                        if (user != null) {
                            nationStr += (" `<@" + user.getDiscordId() + ">`");
                        }
                    }
                    if (iaCat != null) {
                        IAChannel iaChan = iaCat.get(nation);
                        if (channel != null) {
                            if (listRawUserIds) {
                                nationStr += " `" + iaChan.getChannel().getAsMention() + "`";
                            } else {
                                nationStr += " " + iaChan.getChannel().getAsMention();
                            }
                        }
                    }
                    nationList.add(nationStr);
                }
            }
            int pages = (nations.size() + perpage - 1) / perpage;
            title += "(" + (page + 1) + "/" + pages + ")";

            channel.create().paginate(title, command, page, perpage, nationList).send();
        }
        if (listInfo) {
//            if (perpage == null) perpage = 5;
            perpage = 5;
            ArrayList<DBNation> sorted = new ArrayList<>(nations);

            if (sortBy != null) {
                Collections.sort(sorted, (o1, o2) -> Double.compare(((Number)sortBy.apply(o2)).doubleValue(), ((Number) sortBy.apply(o1)).doubleValue()));
            }

            List<String> results = new ArrayList<>();

            for (DBNation nation : sorted) {
                StringBuilder entry = new StringBuilder();
                entry.append("<" + Settings.INSTANCE.DNS_URL() + "/nation/" + nation.getNation_id() + ">")
                        .append(" | " + String.format("%16s", nation.getNation()))
                        .append(" | " + String.format("%16s", nation.getAllianceName()))
                        .append("\n```")
                        .append(String.format("%5s", (int) nation.getScore())).append(" ns").append(" | ")
                        .append(String.format("%2s", nation.getInfra())).append(" dev").append(" | ")
                        .append(String.format("%2s", nation.getLand())).append(" land").append(" | ")
                        .append(String.format("%2s", nation.getWarIndex())).append(" warIndex").append(" | ")
                        .append(String.format("%1s", nation.getOff())).append(" \uD83D\uDDE1").append(" | ")
                        .append(String.format("%1s", nation.getDef())).append(" \uD83D\uDEE1").append(" | ")
                        ;
//                Activity activity = nation.getActivity(14 * 24);
//                double loginChance = activity.loginChance((int) Math.max(1, (24 - (currentHour % 24))), true);
//                int loginPct = (int) (loginChance * 100);
//                response.append("login=" + loginPct + "%").append(" | ");
                entry.append("```");

                results.add(entry.toString());
            }
            channel.create().paginate("Nations", command, page, perpage, results).send();
        }

        return null;
    }

    private static void printAA(StringBuilder response, DBNation nation, boolean spies) {
        response.append(String.format("%4s", TimeUtil.secToTime(TimeUnit.DAYS, nation.getAgeDays()))).append(" ");
        response.append(nation.toMarkdown(true, false, true, false, false));
        response.append(nation.toMarkdown(true, false, false, true, spies));
    }

    @Command(desc = "Add or subtract interest to a nation's balance based on their current balance")
    @RolePermission(value = Roles.ECON)
    public String interest(@Me IMessageIO channel, @Me GuildDB db, Set<DBNation> nations,
                           @Arg("A percent (out of 100) to apply to POSITIVE resources counts in their account balance")
                           @Range(min=-100, max=100) double interestPositivePercent,
                           @Arg("A percent (out of 100) to apply to NEGATIVE resources counts in their account balance")
                           @Range(min=-100, max=100) double interestNegativePercent) throws IOException, GeneralSecurityException {
        if (nations.isEmpty()) throw new IllegalArgumentException("No nations specified");
        if (nations.size() > 180) throw new IllegalArgumentException("Cannot do intest to that many people");

        interestPositivePercent /= 100d;
        interestNegativePercent /= 100d;

        Map<ResourceType, Double> total = new HashMap<>();
        Map<DBNation, Map<ResourceType, Double>> transfers = new HashMap<>();

        for (DBNation nation : nations) {
            Map<ResourceType, Double> deposits = ResourceType.resourcesToMap(nation.getNetDeposits(db, false));
            deposits = DNS.normalize(deposits);

            Map<ResourceType, Double> toAdd = new LinkedHashMap<>();
            for (Map.Entry<ResourceType, Double> entry : deposits.entrySet()) {
                ResourceType type = entry.getKey();
                double amt = entry.getValue();

                double interest;
                if (amt > 1 && interestPositivePercent > 0) {
                    interest = interestPositivePercent * amt;
                } else if (amt < -1 && interestNegativePercent > 0) {
                    interest = interestNegativePercent * amt;
                } else continue;

                toAdd.put(type, interest);
            }
            total = ResourceType.add(total, toAdd);

            if (toAdd.isEmpty()) continue;
            transfers.put(nation, toAdd);
        }

        SpreadSheet sheet = SpreadSheet.create(db, SheetKey.TRANSFER_SHEET);
        List<String> header = new ArrayList<>(Arrays.asList("nation", "alliance", "score"));
        for (ResourceType value : ResourceType.values) {
            header.add(value.name());
        }
        sheet.setHeader(header);

        for (Map.Entry<DBNation, Map<ResourceType, Double>> entry : transfers.entrySet()) {
            ArrayList<Object> row = new ArrayList<>();
            DBNation nation = entry.getKey();

            row.add(MarkupUtil.sheetUrl(nation.getNation(), nation.getUrl()));
            row.add(MarkupUtil.sheetUrl(nation.getAllianceName(), nation.getAllianceUrl()));
            row.add(nation.getScore());
            Map<ResourceType, Double> transfer = entry.getValue();
            for (ResourceType type : ResourceType.values) {
                double amt = transfer.getOrDefault(type, 0d);
                row.add(amt);
            }

            sheet.addRow(row);
        }
        sheet.updateClearCurrentTab();
        sheet.updateWrite();

        CM.deposits.addSheet cmd = CM.deposits.addSheet.cmd.sheet(sheet.getURL()).note("#deposit");

        IMessageBuilder msg = channel.create();
        StringBuilder result = new StringBuilder();
        sheet.attach(msg, "interest", result, false, 0);

        result.append("Total: `" + ResourceType.resourcesToString(total) + "`" +
                "\nWorth: $" + MathMan.format(ResourceType.convertedTotal(total)));
        result.append("\nOr press \uD83C\uDFE6 to run " + cmd.toSlashCommand() + "");

        String title = "Nation Interest";
        String emoji = "Confirm";

        msg.embed(title, result.toString())
                .commandButton(cmd, emoji)
                .send();

        return null;
    }

    @Command(aliases = {"dnr", "caniraid"}, desc = "Check if declaring war on a nation is allowed by the guild's Do Not Raid (DNR) settings")
    public String dnr(@Me GuildDB db, DBNation nation) {
        Integer dnrTopX = db.getOrNull(GuildKey.DO_NOT_RAID_TOP_X);
        Set<Integer> enemies = db.getCoalition(Coalition.ENEMIES);
        Set<Integer> canRaid = db.getCoalition(Coalition.CAN_RAID);
        Set<Integer> canRaidInactive = db.getCoalition(Coalition.CAN_RAID_INACTIVE);

        String title;
        Boolean dnr = db.getCanRaid().apply(nation);
        if (!dnr) {
            title = ("do NOT raid " + nation.getNation());
        }  else if (nation.getPosition() > 1 && nation.active_m() < 10000) {
            title = ("You CAN raid " + nation.getNation() + " (however they are an active member of an alliance), see also: " + CM.alliance.stats.counterStats.cmd.toSlashMention() + "");
        } else if (nation.getPosition() > 1) {
            title =  "You CAN raid " + nation.getNation() + " (however they are a member of an alliance), see also: " + CM.alliance.stats.counterStats.cmd.toSlashMention() + "";
        } else if (nation.getAlliance_id() != 0) {
            title =  "You CAN raid " + nation.getNation() + ", see also: " + CM.alliance.stats.counterStats.cmd.toSlashMention() + "";
        } else {
            title =  "You CAN raid " + nation.getNation();
        }
        StringBuilder response = new StringBuilder();
        response.append("**" + title + "**");
        if (dnrTopX != null) {
            response.append("\n\n> Do Not Raid Guidelines:");
            response.append("\n> - Avoid members of the top " + dnrTopX + " alliances and members of direct allies (check ingame treaties)");

            Set<String> enemiesStr = enemies.stream().map(f -> Locutus.imp().getNationDB().getAllianceName(f)).filter(Objects::nonNull).collect(Collectors.toSet());
            Set<String> canRaidStr = canRaid.stream().map(f -> Locutus.imp().getNationDB().getAllianceName(f)).filter(Objects::nonNull).collect(Collectors.toSet());
            Set<String> canRaidInactiveStr = canRaidInactive.stream().map(f -> Locutus.imp().getNationDB().getAllianceName(f)).filter(Objects::nonNull).collect(Collectors.toSet());

            if (!enemiesStr.isEmpty()) {
                response.append("\n> - Enemies: " + StringMan.getString(enemiesStr));
            }
            if (!canRaidStr.isEmpty()) {
                response.append("\n> - You CAN raid: " + StringMan.getString(canRaidStr));
            }
            if (!canRaidInactiveStr.isEmpty()) {
                response.append("\n> - You CAN raid inactives (1w) of: " + StringMan.getString(canRaidInactiveStr));
            }
        }
        return response.toString();
    }

    @Command(desc = "Rank alliances by their new members over a timeframe")
    public String recruitmentRankings(@Me User author, @Me IMessageIO channel, @Me JSONObject command,
                                      @Arg("Date to start from")
                                      @Timestamp long cutoff,
                                      @Arg("Top X alliances to show in the ranking")
                                      @Range(min=1, max=150) @Default("80") int topX, @Switch("u") boolean uploadFile) {
        Set<DBAlliance> alliances = Locutus.imp().getNationDB().getAlliances(true, true, true, topX);

        Map<DBAlliance, Integer> rankings = new HashMap<DBAlliance, Integer>();

        Set<Integer> aaIds = alliances.stream().map(f -> f.getAlliance_id()).collect(Collectors.toSet());
        Map<Integer, List<AllianceChange>> removesByAlliance = Locutus.imp().getNationDB().getRemovesByAlliances(aaIds, cutoff);

        for (DBAlliance alliance : alliances) {
            Map<Integer, Long> noneToApp = new Int2LongOpenHashMap();
            Map<Integer, Long> appToMember = new Int2LongOpenHashMap();
            Map<Integer, Long> removed = new Int2LongOpenHashMap();

            List<AllianceChange> rankChanges = removesByAlliance.getOrDefault(alliance.getId(), new ArrayList<>());

            for (AllianceChange change : rankChanges) {
                int nationId = change.getNationId();
                if (change.getFromId() != alliance.getId()) {
                    if (change.getToId() == alliance.getId()) {
                        noneToApp.put(nationId, Math.max(noneToApp.getOrDefault(nationId, 0L), change.getDate()));
                        if (change.getToRank() != Rank.APPLICANT) {
                            appToMember.put(nationId, Math.max(appToMember.getOrDefault(nationId, 0L), change.getDate()));
                        }
                    }
                } else if (change.getToId() != alliance.getId()) {
                    removed.put(nationId, Math.max(removed.getOrDefault(nationId, 0L), change.getDate()));
                } else if (change.getToRank() != Rank.APPLICANT) {
                    appToMember.put(nationId, Math.max(appToMember.getOrDefault(nationId, 0L), change.getDate()));
                }
            }
            noneToApp.entrySet().removeIf(f -> removed.getOrDefault(f.getKey(), 0L) > f.getValue());
            appToMember.entrySet().removeIf(f -> removed.getOrDefault(f.getKey(), 0L) > f.getValue());

            // set where number is in both noneToApp and appToMember
            Set<Integer> both = noneToApp.keySet().stream().filter(appToMember::containsKey).collect(Collectors.toSet());
            int total = both.size();
            if (total > 0) {
                rankings.put(alliance, total);
            }
        }
        if (rankings.isEmpty()) {
            return "No new members found over the specified timeframe. Check your arguments are valid";
        }
        new SummedMapRankBuilder<>(rankings).sort().nameKeys(DBAlliance::getName).build(author, channel, command, "Most new members", uploadFile);
        return null;
    }

//    @Command(desc = "Get the cost of military units and their upkeep")
//    public String unitCost(Map<MilitaryUnit, Long> units,
//                           @Arg("Show the upkeep during war time")
//                           @Default Boolean wartime) {
//        if (wartime == null) wartime = false;
//        StringBuilder response = new StringBuilder();
//
//        response.append("**Units " + StringMan.getString(units) + "**:\n");
//        double[] cost = ResourceType.getBuffer();
//        double[] upkeep = ResourceType.getBuffer();
//
//        for (Map.Entry<MilitaryUnit, Long> entry : units.entrySet()) {
//            MilitaryUnit unit = entry.getKey();
//            Long amt = entry.getValue();
//
//            double[] unitCost = unit.getCost(amt.intValue()).clone();
//            double[] unitUpkeep = unit.getUpkeep(wartime).clone();
//
//            unitUpkeep = DNS.multiply(unitUpkeep, amt);
//
//            ResourceType.add(cost, unitCost);
//            ResourceType.add(upkeep, unitUpkeep);
//        }
//        response.append("Cost:\n```" + ResourceType.resourcesToString(cost) + "``` ");
//        if (ResourceType.resourcesToMap(cost).size() > 1) {
//            response.append("Worth: ~$" + MathMan.format(ResourceType.convertedTotal(cost))).append("\n");
//        }
//        response.append("\nUpkeep:\n```" + ResourceType.resourcesToString(upkeep) + "``` ");
//        if (ResourceType.resourcesToMap(upkeep).size(athMan.for) > 1) {
////            response.append("Worth: ~$" + Mmat(ResourceType.convertedTotal(upkeep))).append("\n");
//        }
//
//        return response.toString();
//    }

    // Helper function to check and update inactivity streaks
    private void checkAndUpdateStreak(Map<Integer, Long> lastActive, Map<Integer, Long> lastNotGray, Map<Integer, Long> lastStreak, Map<Integer, Integer> countMap, int nationId, long day, int daysInactive) {
        long lastActiveDay = lastActive.getOrDefault(nationId, day);
        long lastNotGrayDay = lastNotGray.getOrDefault(nationId, day);
        long lastActiveAdded = lastStreak.getOrDefault(nationId, 0L);
        if (lastActiveAdded == lastActiveDay) return;

        if (day - lastNotGrayDay >= daysInactive) {
            countMap.put(nationId, countMap.getOrDefault(nationId, 0) + 1);
            lastStreak.put(nationId, lastActiveAdded);
        }
    }

    // TODO FIXME :||remove alliance cost
//    @Command(aliases = {"alliancecost", "aacost"}, desc = "Get the value of nations including their cities, projects and units")
//    public String allianceCost(@Me IMessageIO channel, @Me GuildDB db,
//                               NationList nations, @Switch("u") boolean update, @Switch("s") @Timestamp Long snapshotDate) {
//        Set<DBNation> nationSet = DNS.getNationsSnapshot(nations.getNations(), nations.getFilter(), snapshotDate, db.getGuild(), false);
//        double infraCost = 0;
//        double landCost = 0;
//        double cityCost = 0;
//        Map<ResourceType, Double> projectCost = new HashMap<>();
//        Map<ResourceType, Double> militaryCost = new HashMap<>();
//        Map<ResourceType, Double> buildingCost = new HashMap<>();
//        for (DBNation nation : nationSet) {
//            Set<Project> projects = nation.getProjects();
//            for (Project project : projects) {
//                projectCost = ResourceType.addResourcesToA(projectCost, project.cost());
//            }
//            for (MilitaryUnit unit : MilitaryUnit.values) {
//                int units = nation.getUnits(unit);
//                militaryCost = ResourceType.addResourcesToA(militaryCost, ResourceType.resourcesToMap(unit.getCost(units)));
//            }
//            int cities = nation.getCities();
//            for (int i = 1; i <= cities; i++) {
//                boolean manifest = true;
//                boolean cp = i > Projects.URBAN_PLANNING.requiredCities() && projects.contains(Projects.URBAN_PLANNING);
//                boolean acp = i > Projects.ADVANCED_URBAN_PLANNING.requiredCities() && projects.contains(Projects.ADVANCED_URBAN_PLANNING);
//                boolean mp = i > Projects.METROPOLITAN_PLANNING.requiredCities() && projects.contains(Projects.METROPOLITAN_PLANNING);
//                boolean gsa = projects.contains(Projects.GOVERNMENT_SUPPORT_AGENCY);
//                cityCost += DNS.City.nextCityCost(i, manifest, cp, acp, mp, gsa);
//            }
//            Map<Integer, JavaCity> cityMap = nation.getCityMap(update, update,false);
//            for (Map.Entry<Integer, JavaCity> cityEntry : cityMap.entrySet()) {
//                JavaCity city = cityEntry.getValue();
//                {
//                    double landFactor = 1;
//                    double infraFactor = 0.95;
//                    if (projects.contains(Projects.ARABLE_LAND_AGENCY)) landFactor *= 0.95;
//                    if (projects.contains(Projects.CENTER_FOR_CIVIL_ENGINEERING)) infraFactor *= 0.95;
//                    landCost += DNS.City.Land.calculateLand(DNS.City.Land.NEW_CITY_BASE, city.getLand()) * landFactor;
//                    infraCost += DNS.City.Infra.calculateInfra(DNS.City.Infra.NEW_CITY_BASE, city.getInfra()) * infraFactor;
//                }
//                city = cityEntry.getValue();
//                JavaCity empty = new JavaCity();
//                empty.setLand(city.getLand());
//                empty.setInfra(city.getInfra());
//                double[] myBuildingCost = city.calculateCost(empty);
//                buildingCost = ResourceType.addResourcesToA(buildingCost, ResourceType.resourcesToMap(myBuildingCost));
//            }
//        }
//
//        Map<ResourceType, Double> total = new HashMap<>();
//        total.put(ResourceType.MONEY, total.getOrDefault(ResourceType.MONEY, 0d) + infraCost);
//        total.put(ResourceType.MONEY, total.getOrDefault(ResourceType.MONEY, 0d) + landCost);
//        total.put(ResourceType.MONEY, total.getOrDefault(ResourceType.MONEY, 0d) + cityCost);
//        total = ResourceType.add(total, projectCost);
//        total = ResourceType.add(total, militaryCost);
//        total = ResourceType.add(total, buildingCost);
//        double totalConverted = ResourceType.convertedTotal(total);
//        String title = nationSet.size() + " nations worth ~$" + MathMan.format(totalConverted);
//
//        StringBuilder response = new StringBuilder();
//        response.append("**Infra**: $" + MathMan.format(infraCost));
//        response.append("\n").append("**Land**: $" + MathMan.format(landCost));
//        response.append("\n").append("**Cities**: $" + MathMan.format(cityCost));
//        response.append("\n").append("**Projects**: $" + MathMan.format(ResourceType.convertedTotal(projectCost)) + "\n`" + ResourceType.resourcesToString(projectCost) + "`");
//        response.append("\n").append("**Military**: $" + MathMan.format(ResourceType.convertedTotal(militaryCost)) + "\n`" + ResourceType.resourcesToString(militaryCost) + "`");
//        response.append("\n").append("**Buildings**: $" + MathMan.format(ResourceType.convertedTotal(buildingCost)) + "\n`" + ResourceType.resourcesToString(buildingCost) + "`");
//        response.append("\n").append("**Total**: $" + MathMan.format(ResourceType.convertedTotal(total)) + "\n`" + ResourceType.resourcesToString(total) + "`");
//
//        channel.create().embed(title, response.toString()).send();
//        return null;
//    }

    @Command(desc = "Add a watermark to a discord image\n" +
            "Use \\n to add a new line\n" +
            "Default color will be light gray if image is dark and dark gray if image is light\n" +
            "Set `repeat: True` to repeat the watermark down the entire image")
    public String addWatermark(@Me IMessageIO io, String imageUrl, String watermarkText, @Default Color color, @Default("0.05") @Range(min = 0.01, max=1) double opacity, @Default("Arial") Font font, @Switch("r") boolean repeat) {
        float opacityF = (float) opacity;
        // remove anything after ? mark
        String imageStub = imageUrl.split("\\?")[0];

        if (!ImageUtil.isDiscordImage(imageUrl)) {
            throw new IllegalArgumentException("Image must be a discord image, not: `" + imageStub + "`");
        }

        BufferedImage image = ImageUtil.readImage(imageUrl);
        if (color == null) color = ImageUtil.getDefaultWatermarkColor(image);
        byte[] bytes = ImageUtil.addWatermark(image, watermarkText, color, opacityF, font, repeat);
        io.create().image("locutus-watermark.png", bytes).send();
        return null;
    }

    @Command
    @RolePermission(Roles.MEMBER)
    public String development(@Me GuildDB db, @Me User user, @Me DBNation me, DBNation nation, @Switch("u") boolean update, @Switch("n") boolean full_numbers) {
        boolean canViewPrivate = me.getId() == nation.getId() || (Roles.INTERNAL_AFFAIRS.has(user, db.getGuild()) && db.isAllianceId(nation.getAlliance_id()));
        NationPrivate data = canViewPrivate ? nation.getPrivateData() : null;
        long now = System.currentTimeMillis() - (update ? 0 : TimeUnit.HOURS.toMillis(1));

        Map<String, Object> general = new LinkedHashMap<>();
        Map<String, Object> indexes = new LinkedHashMap<>();
        Map<String, Object> daily = new LinkedHashMap<>();
        general.put("Development", nation.getInfra());
        general.put("Land", nation.getLand());
        general.put("Population", nation.getPopulation());

        if (data != null) {
            Map<Building, Integer> buildings = data.getBuildings(now, false);
            Map<Building, Integer> effectBuilding = data.getEffectBuildings(now);

            int slots = data.getTotalSlots(now);
            int openSlots = data.getOpenSlots(now);

            int jobs = Building.getJobs(buildings);
            int effectJobs = Building.getJobs(effectBuilding);
            int totalJobs = jobs + effectJobs;
            general.put("Employment", MathMan.format(totalJobs) + "(" + MathMan.format(effectJobs) + " from effects)");
            long unfilled = Math.round(totalJobs - nation.getPopulation());
            if (unfilled > 0) {
                general.put("Unfilled Jobs", MathMan.format(unfilled));
            } else if (unfilled < 0) {
                general.put("Unemployment", MathMan.format(-unfilled));
            }
            general.put("Total Building Slots", MathMan.format(slots));
            general.put("Open Building Slots", MathMan.format(openSlots));
        }
        indexes.put("Military Index", nation.getWarIndex());
        indexes.put("Stability Index", nation.getStabilityIndex());
        indexes.put("Tech Index", nation.getTechIndex());
        indexes.put("Education Index", nation.getEducationIndex());
        indexes.put("Commerce Index", nation.getCommerceIndex());
        indexes.put("Transportation Index", nation.getTransportationIndex());
        indexes.put("Power Index", nation.getPowerIndex());
        indexes.put("Employment Index", nation.getEmploymentIndex());

        daily.put("Total Cash Income", nation.getCashOutput());
        daily.put("- Corporate Income", nation.getCorporationIncome());
        daily.put("- Other Income", nation.getOtherIncome());
        daily.put("- Tax Income", nation.getTaxIncome());
        daily.put("Minerals Income", nation.getMineralOutput());
        daily.put("Fuel Income", nation.getFuelOutput());
        if (data != null) {
            Map<Building, Integer> buildings = data.getBuildings(now, true);
            Map<Project, Integer> projects = data.getProjects(now);
            Map<Technology, Integer> tech = data.getTechnology(now);
            NationModifier modifier = DNS.getNationModifier(nation.getInfra(), buildings, projects, tech);
            double techOutput = modifier.getTechOutput(nation.getEducationIndex());
            daily.put("Tech Income (no policies)", techOutput);
        }
        daily.put("Production Income", nation.getProductionOutput());
        daily.put("Uranium Income", nation.getUraniumOutput());
        daily.put("Rare Metal Income", nation.getRareMetalOutput());
        daily.put("Political Support Gain", nation.getPoliticalPowerOutput());

        Function<Double, String> formatNum = full_numbers ? MathMan::format : MathMan::formatSig;
        Function<Map.Entry<String, Object>, String> toString = f -> {
            Object value = f.getValue();
            return "" + f.getKey() + ": `" + ((value instanceof Number n) ? (formatNum.apply(n.doubleValue())) : (StringMan.getString(value))) + "`";
        };
        StringBuilder response = new StringBuilder("## " + nation.getMarkdownUrl() + " Development\n");
        response.append("### General Information\n");
        general.forEach((k, v) -> response.append(toString.apply(Map.entry(k, v))).append("\n"));
        response.append("\n### Indexes\n");
        indexes.forEach((k, v) -> response.append(toString.apply(Map.entry(k, v))).append("\n"));
        response.append("\n### Daily Income\n");
        daily.forEach((k, v) -> response.append(toString.apply(Map.entry(k, v))).append("\n"));
        return response.toString();
    }
}
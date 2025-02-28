package link.locutus.discord.commands.manager.v2.impl.pw.commands;

import com.google.gson.JsonObject;
import link.locutus.discord.Locutus;
import link.locutus.discord.api.ApiKeyPool;
import link.locutus.discord.api.types.DepositType;
import link.locutus.discord.api.types.FlowType;
import link.locutus.discord.api.types.Project;
import link.locutus.discord.api.types.tx.Transaction2;
import link.locutus.discord.api.endpoints.DnsApi;
import link.locutus.discord.commands.manager.v2.binding.annotation.*;
import link.locutus.discord.commands.manager.v2.binding.annotation.Timestamp;
import link.locutus.discord.commands.manager.v2.command.CommandBehavior;
import link.locutus.discord.commands.manager.v2.command.CommandRef;
import link.locutus.discord.commands.manager.v2.command.IMessageBuilder;
import link.locutus.discord.commands.manager.v2.command.IMessageIO;
import link.locutus.discord.commands.manager.v2.impl.discord.DiscordChannelIO;
import link.locutus.discord.commands.manager.v2.impl.discord.binding.annotation.NationDepositLimit;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.IsAlliance;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.RolePermission;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.commands.manager.v2.impl.pw.NationFilter;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.BankDB;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.*;
import link.locutus.discord.db.guild.GuildKey;
import link.locutus.discord.db.guild.SheetKey;
import link.locutus.discord.pnw.AllianceList;
import link.locutus.discord.pnw.GuildOrAlliance;
import link.locutus.discord.pnw.NationOrAllianceOrGuild;
import link.locutus.discord.pnw.SimpleNationList;
import link.locutus.discord.util.*;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.pnw.NationList;
import link.locutus.discord.pnw.NationOrAlliance;
import link.locutus.discord.pnw.NationOrAllianceOrGuild;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.discord.DiscordUtil;
import link.locutus.discord.util.math.ArrayUtil;
import link.locutus.discord.util.offshore.Auth;
import link.locutus.discord.util.scheduler.TriConsumer;
import link.locutus.discord.util.scheduler.TriFunction;
import link.locutus.discord.util.sheet.SpreadSheet;
import link.locutus.discord.util.sheet.templates.TransferSheet;
import link.locutus.discord.api.types.Rank;
import link.locutus.discord.api.generated.ResourceType;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static link.locutus.discord.api.generated.ResourceType.convertedTotal;
import static link.locutus.discord.api.generated.ResourceType.resourcesToString;

public class BankCommands {
    @Command(desc = "View alliance tax income")
    @RolePermission(value = {Roles.ECON, Roles.ECON_STAFF})
    public String allianceTaxIncome(@Me IMessageIO io, @Me GuildDB db, DBAlliance alliance, @Switch("f") boolean forceUpdate) {
        if (!db.isAllianceId(alliance.getAlliance_id())) {
            throw new IllegalArgumentException("Alliance is not registered to this guild. See " + CM.register.cmd.toSlashMention());
        }
        long updateAfter = forceUpdate ? Long.MAX_VALUE : System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5);
        Map<ResourceType, Double> taxes = alliance.getPrivateData().getTaxIncome(updateAfter);
        if (taxes == null) {
            return "No tax income data found";
        }
        String out = ResourceType.resourcesToFancyString(taxes);
        io.create().embed(alliance.getName() + " tax income", out).send();
        return null;
    }
//
///*
//> `/deposit resources <amount> <raws-days> <warchest-per-city> <warchest-total> <unit-resources> <note> `
//>
//> `amount` = How many resources to deposit. Optional. Throws error if any of the other arguments (besides note) is set.
//>
//> `raws-days` = Number of days of raws to keep on nation (optional) `double`
//>
//> `keep-warchest-factor` = Number of default warchests to keep per city (per city). Default warchest is is set via the settings command (optional) `double`
//> `keep-per-city` = amount of resources to keep (per city) (optional) `Map<ResourceType,Double>`
//> `keep-total` = amount of resources to keep (total) (optional) `Map<ResourceType,Double>`
//> (only one of warchest-per-city or warchest-total can be set, not both)
//>
//> `unit-resources` = what units to keep funds to buy e.g. for nukes/missiles `Map<MilitaryUnit,Integer>`
//>
//> `note` = the note to use for deposits `String`
//>
//> Which will return a bank url (with the resource values set in the url query, e.g. `d_food=1234&d_coal=5678` (off the top of my head, i cant remember if that's the right parameter to use)`
// */
////    @Command(desc = "Find the ROI for various changes you can make to your nation, with a specified timeframe\n" +
////            "(typically how long you expect the changes, or peacetime to last)\n" +
////            "e.g. `{prefix}ROI @Borg 30`\n" +
////            "Add `-r` to run it recursively for various infra levels")
////    @RolePermission(Roles.MEMBER)
////    public String roi()
//
//    @Command(desc = "Instruct nations to deposit resources into the alliance bank\n" +
//     "If multiple calculation options are set the largest values will be used",
//            groups = {
//                "Mode 1: Using a Sheet",
//                "Mode 2: Specify an Amount",
//                "Mode 3: Calculate an Amount",
//                "Message Settings",
//                "Deposit Via Api",
//            }
//    )
//    @RolePermission(Roles.MEMBER)
//    @IsAlliance
//    public String depositResources(@Me User author, @Me DBNation me, @Me GuildDB db, @Me JSONObject command, @Me IMessageIO io,
//                                   Set<DBNation> nations,
//
//                                   @Arg(value = "A spreadsheet of nations and amounts to deposit\n" +
//                                           "Columns must be named after the resource names", group = 0)
//                                   @Switch("s") TransferSheet sheetAmounts,
//
//                                   @Arg(value = "Exact amount of resources to deposit (capped at resources on nation)\n" +
//                                           "Cannot be used with other deposit modes are set", group = 1)
//                                   @Switch("a") Map<ResourceType, Double> amount,
//
//                                   @Arg(value = "Number of days of city raw resource consumption to keep\n" +
//                                           "Recommended value: 5", group = 2)
//                                       @Range(min = 0)
//                                   @Switch("r") Double rawsDays,
//
//                                   @Arg(value = "Do not keep money above the daily login bonus\n" +
//                                           "Requires `rawsDays` to be set", group = 2) @Switch("d") boolean rawsNoDailyCash,
//                                   @Arg(value = "Do not keep any money\n" +
//                                           "Requires `rawsDays` to be set ", group = 2) @Switch("c") boolean rawsNoCash,
//
//                                   @Arg(value = "Number of default warchests to keep per city\n" +
//                                           "Recommended value: 1\n" +
//                                           "Default warchest is is set via the settings command", group = 2)
//                                   @Switch("wcf") Double keepWarchestFactor,
//                                   @Arg(value = "Amount of resources to keep per city", group = 2)
//                                   @Switch("pc") Map<ResourceType, Double> keepPerCity,
//                                   @Arg(value = "Amount of resources to keep in total", group = 2)
//                                   @Switch("kt") Map<ResourceType, Double> keepTotal,
//                                   @Arg(value = "Keep resources for purchasing specific units", group = 2)
//                                   @Switch("ur") Map<MilitaryUnit, Long> unitResources,
//
//                                   @Arg(value = "Note to add to the deposit\n" +
//                                           "Defaults to deposits", group = 3)
//                                   @Switch("n") DepositType.DepositTypeInfo note,
//
//                                   @Arg(value = "The message to append to the mail or dm message\n" +
//                                           "You must specify either `mailResults` or `dm` if this is set", group = 4)
//                                   @Switch("cm") String customMessage,
//
//                                   @Arg(value = "Send deposit urls to nations via in-game mail", group = 4)
//                                   @Switch("m") boolean mailResults,
//                                   @Arg(value = "Send deposit urls to nations in discord direct messages (dm)", group = 4)
//                                   @Switch("dm") boolean dm,
//
//                                   @Arg(value = "Use the API to do a deposit instead of sending a message", group = 5)
//                                   @Switch("u") boolean useApi,
//
//                                   @Switch("f") boolean force) throws IOException, ExecutionException, InterruptedException, GeneralSecurityException {
//        if (customMessage != null && !dm && !mailResults) {
//            throw new IllegalArgumentException("Cannot specify `customMessage` without specifying `dm` or `mailResults`");
//        }
//        if (amount != null && (rawsDays != null || keepWarchestFactor != null || keepPerCity != null || keepTotal != null || unitResources != null)) {
//            throw new IllegalArgumentException("Cannot specify `amount` to deposit with other deposit modes.");
//        }
//        if (rawsNoDailyCash && rawsDays == null) {
//            throw new IllegalArgumentException("Cannot specify `rawsNoDailyCash` (`-d`) without specifying `rawsDays`");
//        }
//        if (rawsNoCash && rawsDays == null) {
//            throw new IllegalArgumentException("Cannot specify `rawsNoCash` (`-c`) without specifying `rawsDays`");
//        }
//        if (sheetAmounts != null && (rawsDays != null || keepWarchestFactor != null || keepPerCity != null || keepTotal != null || unitResources != null)) {
//            throw new IllegalArgumentException("Cannot specify `sheetAmounts` to deposit with other deposit modes.");
//        }
//        if (nations.isEmpty()) return "No nations found";
//
//        boolean isOther = nations.size() > 1 || me.getNation_id() != nations.iterator().next().getNation_id();
//
//        if (mailResults && !Roles.MAIL.has(author, db.getGuild()) && isOther) {
//            throw new IllegalArgumentException("No permission for `mailResults`. " + Roles.MAIL.toDiscordRoleNameElseInstructions(db.getGuild()));
//        }
//        if (dm && !Roles.MAIL.hasOnRoot(author) && isOther) {
//            throw new IllegalArgumentException("No permission for `dm`. " + Roles.MAIL.toDiscordRoleNameElseInstructions(db.getGuild()));
//        }
//        if (useApi && isOther && !Roles.ECON.has(author, db.getGuild())) {
//            throw new IllegalArgumentException("No permission for `useApi`. " + Roles.ECON.toDiscordRoleNameElseInstructions(db.getGuild()));
//        }
//        Set<Long> allowed = Roles.ECON_STAFF.getAllowedAccounts(author, db);
//        Map<DBNation, OffshoreInstance.TransferStatus> statuses = new LinkedHashMap<>();
//        Map<DBNation, double[]> toKeepMap = new LinkedHashMap<>();
//        Map<DBNation, double[]> toDepositMap = new LinkedHashMap<>();
//
//        if (sheetAmounts != null) {
//            Map<DBNation, Map<ResourceType, Double>> nationTransfers = sheetAmounts.getNationTransfers();
//            nations = new HashSet<>(nations);
//            nations.removeIf(f -> !nationTransfers.containsKey(f));
//            for (Map.Entry<DBNation, Map<ResourceType, Double>> entry : nationTransfers.entrySet()) {
//                toDepositMap.put(entry.getKey(), ResourceType.resourcesToArray(entry.getValue()));
//            }
//        }
//
//        for (DBNation nation : nations) {
//            if (!db.isAllianceId(nation.getAlliance_id())) {
//                statuses.put(nation, OffshoreInstance.TransferStatus.NOT_MEMBER);
//                continue;
//            }
//            if (nation.getPositionEnum().id <= Rank.APPLICANT.id) {
//                statuses.put(nation, OffshoreInstance.TransferStatus.APPLICANT);
//                continue;
//            }
//            if (nation.getNation_id() != me.getNation_id() && !allowed.contains((long) nation.getAlliance_id())) {
//                statuses.put(nation, OffshoreInstance.TransferStatus.AUTHORIZATION);
//                continue;
//            }
//            if (nation.isVacation()) {
//                statuses.put(nation, OffshoreInstance.TransferStatus.VACATION_MODE);
//            }
//            if (nation.isBlockaded()) {
//                statuses.put(nation, OffshoreInstance.TransferStatus.BLOCKADE);
//            }
//        }
//
//        Set<DBNation> remainingNations = nations.stream().filter(f -> !statuses.containsKey(f)).collect(Collectors.toSet());
//
//        AllianceList allianceList = new SimpleNationList(remainingNations).toAllianceList();
//
//        if (rawsDays != null) {
//            if (rawsDays < 1/12d) {
//                throw new IllegalArgumentException("rawsDays must be > 1 turns (1/12 days)");
//            }
//            allianceList.updateCities();
//            Map<DBNation, Map.Entry<OffshoreInstance.TransferStatus, double[]>> funds = allianceList.calculateDisburse(nations, null, rawsDays, false, false, true, rawsNoDailyCash, rawsNoCash, true, false);
//            for (Map.Entry<DBNation, Map.Entry<OffshoreInstance.TransferStatus, double[]>> entry : funds.entrySet()) {
//                DBNation nation = entry.getKey();
//                OffshoreInstance.TransferStatus status = entry.getValue().getKey();
//                double[] rss = entry.getValue().getValue();
//                if (rawsNoDailyCash) {
//                    rss[ResourceType.MONEY.ordinal()] = Math.max(50000 * nation.getCities(), rss[ResourceType.MONEY.ordinal()]);
//                }
//                if (rawsNoCash) {
//                    rss[ResourceType.MONEY.ordinal()] = Math.min(50000 * nation.getCities(), rss[ResourceType.MONEY.ordinal()]);
//                }
//                if (rss != null) {
//                    toKeepMap.put(nation, rss);
//                } else {
//                    statuses.put(nation, status);
//                }
//            }
//        }
//
//        Map<DBNation, Map<ResourceType, Double>> stockpiles = allianceList.getMemberStockpile(remainingNations::contains);
//        for (DBNation nation : remainingNations) {
//            Map<ResourceType, Double> stockpile = stockpiles.get(nation);
//            if (stockpile == null) {
//                statuses.put(nation, OffshoreInstance.TransferStatus.ALLIANCE_ACCESS);
//                continue;
//            }
//            double[] toKeep = toKeepMap.getOrDefault(nation, ResourceType.getBuffer());
//
//            double[] toDeposit = toDepositMap.getOrDefault(nation, ResourceType.getBuffer());
//            if (amount != null) {
//                toDeposit = ResourceType.resourcesToArray(amount);
//            }
//            if (keepWarchestFactor != null) {
//                double[] rss = ResourceType.resourcesToArray(db.getPerCityWarchest(nation));
//                DNS.multiply(rss, nation.getCities());
//                toKeep = ResourceType.max(toKeep, rss);
//            }
//            if (keepPerCity != null) {
//                double[] rss = ResourceType.resourcesToArray(keepPerCity);
//                DNS.multiply(rss, nation.getCities());
//                toKeep = ResourceType.max(toKeep, rss);
//            }
//            if (keepTotal != null) {
//                double[] rss = ResourceType.resourcesToArray(keepTotal);
//                toKeep = ResourceType.max(toKeep, rss);
//            }
//            if (unitResources != null) {
//                double[] rss = ResourceType.getBuffer();
//                for (Map.Entry<MilitaryUnit, Long> entry : unitResources.entrySet()) {
//                    rss = ResourceType.add(rss, entry.getKey().getCost(entry.getValue().intValue()));
//                }
//                toKeep = ResourceType.max(toKeep, rss);
//            }
//            if (!ResourceType.isZero(toKeep)) {
//                // to deposit = stockpile - toKeep, max 0
//                toDeposit = ResourceType.builder().add(stockpile).subtract(toKeep).build();
//            }
//
//            toDeposit = ResourceType.max(toDeposit, ResourceType.getBuffer());
//            toDeposit = ResourceType.min(toDeposit, ResourceType.resourcesToArray(stockpile));
//
//            if (ResourceType.isZero(toDeposit)) {
//                statuses.put(nation, OffshoreInstance.TransferStatus.NOTHING_WITHDRAWN);
//                continue;
//            }
//            toDepositMap.put(nation, toDeposit);
//        }
//        List<String> header = new ArrayList<>(Arrays.asList(
//                "nation",
//                "nation_url",
//                "status",
//                "deposit_raw",
//                "remaining_raw",
//                "deposit_url",
//                "DM",
//                "Mail",
//                "API"
//        ));
//
//        List<List<String>> errorRows = new ArrayList<>();
//        List<List<String>> rows = new ArrayList<>();
//        for (Map.Entry<DBNation, OffshoreInstance.TransferStatus> entry : statuses.entrySet()) {
//            DBNation nation = entry.getKey();
//
//            Map<ResourceType, Double> stockpile = stockpiles.get(nation);
//            String stockpileStr = stockpile == null ? null : ResourceType.resourcesToString(stockpile);
//            List<String> row = Arrays.asList(
//                    nation.getName(),
//                    nation.getUrl(),
//                    entry.getValue().name(),
//                    "{}",
//                    stockpileStr,
//                    "null",
//                    "false",
//                    "false",
//                    "false"
//            );
//            errorRows.add(row);
//        }
//
//        Set<DBNation> finalNations = nations;
//        TriConsumer<IMessageBuilder, List<List<String>>, String> attachErrors = new TriConsumer<IMessageBuilder, List<List<String>>, String>() {
//            @Override
//            public void accept(IMessageBuilder msg, List<List<String>> errors, String title) {
//                if (errors.size() > 0) {
//                    errors = new ArrayList<>(errors);
//                    errors.add(0, header);
//                    if (finalNations.size() > 1) {
//                        String content = errors.stream().map(f -> StringMan.join(f, "\t")).collect(Collectors.joining("\n"));
//                        msg.file(title + ".txt", content);
//                    } else {
//                        String content = errors.stream().map(f -> StringMan.join(f, "\t")).collect(Collectors.joining("\n"));
//                        msg.append("### " + title + "\n");
//                        msg.append("```\n" + content + "\n```");
//                    }
//                }
//            }
//        };
//
//        if (toDepositMap.isEmpty()) {
//            IMessageBuilder msg = io.create().append("Nothing to deposit");
//            attachErrors.accept(msg, errorRows, "errors");
//            msg.send();
//            return null;
//        }
//
//        TriFunction<double[], Integer, String, String> toBankUrl = new TriFunction<>() {
//            @Override
//            public String apply(double[] resources, Integer allianceId, String note) {
//                StringBuilder url = new StringBuilder(Settings.INSTANCE.DNS_URL() + "/alliance/id=" + allianceId + "&display=bank" + (note != null ? "&d_note=" + note : ""));
//                for (ResourceType type : ResourceType.values) {
//                    double amt = resources[type.ordinal()];
//                    if (amt > 0) {
//                        String amtStr = MathMan.format(amt);
//                        url.append("&d_" + type.name().toLowerCase() + "=" + amtStr);
//                    }
//                }
//                return url.toString();
//            }
//        };
//
//        ApiKeyPool key = db.getMailKey();
//        long channelId = io.getIdLong();
//
//        if (!force) {
//            double[] total = ResourceType.getBuffer();
//            double[] remainingTotal = ResourceType.getBuffer();
//            for (Map.Entry<DBNation, double[]> entry : toDepositMap.entrySet()) {
//                total = ResourceType.add(total, entry.getValue());
//                Map<ResourceType, Double> stockpile = stockpiles.get(entry.getKey());
//                if (stockpile != null) {
//                    double[] remaining = ResourceType.max(ResourceType.builder().add(stockpile).subtract(entry.getValue()).build(), ResourceType.getBuffer());
//                    remainingTotal = ResourceType.add(remainingTotal, remaining);
//                }
//            }
//
//            String title = "Deposit worth ~$" + MathMan.format(ResourceType.convertedTotal(total)) + " for ";
//            if (toDepositMap.size() == 1) {
//                title += toDepositMap.keySet().iterator().next().getName();
//            } else {
//                title += toDepositMap.size() + " nations";
//                int numAlliances = new SimpleNationList(toDepositMap.keySet()).getAllianceIds().size();
//                if (numAlliances > 1) {
//                    title += " in " + numAlliances + " alliances";
//                }
//            }
//            IMessageBuilder msg = io.create();
//            StringBuilder body = new StringBuilder();
//            if (useApi) {
//                body.append("`useApi`: True (Will attempt to deposit via api)\n");
//            }
//            if (mailResults) {
//                body.append("`mailResults`: True (Will send results via in-game mail)\n");
//            }
//            if (dm) {
//                body.append("`dmResults`: True (Will send results via discord dm)\n");
//            }
//
//            if (toDepositMap.size() > 1) {
//                SpreadSheet sheet;
//                if (sheetAmounts == null) {
//                    // create sheet
//                    sheet = SpreadSheet.create(db, SheetKey.DEPOSIT_SHEET);
//                    sheet.addRow(header);
//                    for (List<String> errorRow : errorRows) {
//                        sheet.addRow(errorRow);
//                    }
//                    for (Map.Entry<DBNation, double[]> entry : toDepositMap.entrySet()) {
//                        DBNation nation = entry.getKey();
//                        double[] rss = entry.getValue();
//                        // stockpileStr
//                        Map<ResourceType, Double> stockpile = stockpiles.get(nation);
//                        String stockpileStr = stockpile == null ? null : ResourceType.resourcesToString(stockpile);
//
//                        List<String> row = Arrays.asList(
//                                nation.getName(),
//                                nation.getUrl(),
//                                "SEND",
//                                ResourceType.resourcesToString(rss),
//                                stockpileStr,
//                                toBankUrl.apply(rss, nation.getAlliance_id(), note == null ? null : note.toString()),
//                                (dm && nation.getUser() != null) + "",
//                                mailResults + "",
//                                (useApi && nation.getApiKey(false) != null) + ""
//                        );
//                        sheet.addRow(row);
//                    }
//                    sheet.updateClearCurrentTab();
//                    sheet.updateWrite();
//                } else {
//                    sheet = sheetAmounts.getSheet();
//                }
//                sheet.attach(msg, "deposit", body, false, 0);
//            }
//
//            // total / worth
//            body.append("\n**Total:** `" + ResourceType.resourcesToString(total) + "` worth ~$" + MathMan.format(ResourceType.convertedTotal(total)) + "\n");
//            // remaining / worth
//            body.append("\n**Remaining:** `" + ResourceType.resourcesToString(remainingTotal) + "` worth ~$" + MathMan.format(ResourceType.convertedTotal(remainingTotal)) + "\n");
//            // errors
//            if (!errorRows.isEmpty()) {
//                attachErrors.accept(msg, errorRows, "errors");
//            }
//            msg.confirmation(title, body.toString(), command).send();
//            return null;
//        }
//
//        StringBuilder result = new StringBuilder();
//        for (Map.Entry<DBNation, double[]> entry : toDepositMap.entrySet()) {
//            DBNation nation = entry.getKey();
//
//            String sentDmError = null;
//            String sentMailError = null;
//            String sentApiError = null;
//            boolean sentDm = false;
//            boolean sentMail = false;
//            boolean sentApi = false;
//
//            double[] resources = entry.getValue();
//            String url = toBankUrl.apply(resources, nation.getAlliance_id(), note == null ? null : note.toString());
//            result.append("__**# " + nation.getNation() + "**__:\n");
//            //  +"\t" + nation.getNationUrl()).append(" Can deposit: `").append(url).append(">");
//            result.append("- **nation**: <" + nation.getUrl() + ">\n");
//            result.append("- **deposit amount**: `" + ResourceType.resourcesToString(resources) + "`\n");
//            result.append("- **deposit url**: <" + url + ">\n");
//
//            StringBuilder body = new StringBuilder("Excess resources can be deposited in the alliance bank.\n");
//            body.append("Deposit url: " + url + "\n");
//
//            if (customMessage != null) {
//                body.append(customMessage);
//            }
//            if (me.getNation_id() != nation.getNation_id()) {
//                body.append("\n- Sent by: " + me.getNation() + "/" + author.getName());
//            }
//
//            if (mailResults) {
//                String subject = "Deposit Resources/" + channelId;
//                try {
//                    JsonObject mailResult = nation.sendMail(key, subject, body.toString(), true);
//                    result.append("\n- **mail**: ").append("`" + mailResult + "`");
//                    sentMail = true;
//                } catch (Throwable e) {
//                    sentMailError = e.getMessage();
//                    sentMail = false;
//                    result.append("\n- **mail**: Failed to send mail (`" + e.getMessage() + "`)");
//                }
//            }
//
//            if (dm) {
//                User user = nation.getUser();
//                if (user == null) {
//                    sentDmError = "NO DISCORD";
//                    sentDm = false;
//                    result.append("\n- **dm**: No discord user set. See " + CM.register.cmd.toSlashMention());
//                } else {
//                    try {
//                        PrivateChannel channel = RateLimitUtil.complete(user.openPrivateChannel());
//                        RateLimitUtil.queue(channel.sendMessage(body.toString()));
//                        sentDm = true;
//                        result.append("\n- **dm**: Sent dm");
//                    } catch (Throwable e) {
//                        sentDmError = e.getMessage();
//                        sentDm = false;
//                        result.append("\n- **dm**: Failed to send dm (`" + e.getMessage() + "`)");
//                    }
//                }
//            }
//
//            if (useApi) {
//                try {
//                    ApiKeyPool.ApiKey api = nation.getApiKey(false);
//                    if (api == null) {
//                        sentApiError = "NO API";
//                        sentApi = false;
//                        result.append("\n- **api**: No `API_KEY` set. See " + CM.credentials.addApiKey.cmd.toSlashMention());
//                    } else {
//                        String noteStr = note != null ? note.toString() : "#deposit";
//                        new DnsApi(ApiKeyPool.create(api)).depositIntoBank(resources, noteStr);
//                        sentApi = true;
//                        result.append("\n- **api**: Deposited `" + ResourceType.resourcesToString(resources) + "` | `" + noteStr + "` into bank");
//                    }
//                } catch (Throwable e) {
//                    sentApi = false;
//                    sentApiError = e.getMessage();
//                    result.append("\n- **api**: No valid api key set (`" + e.getMessage() + "`). See " + CM.credentials.addApiKey.cmd.toSlashMention());
//                }
//            }
//            result.append("\n");
//
//            String status = "";
//            if (sentDmError != null) status += "DM: " + sentDmError + " ";
//            if (sentMailError != null) status += "Mail: " + sentMailError + " ";
//            if (sentApiError != null) status += "Api: " + sentApiError + " ";
//            if (status.isEmpty()) status = "SUCCESS";
//            Map<ResourceType, Double> stockpile = stockpiles.get(nation);
//            String stockpileStr = stockpile == null ? null : ResourceType.resourcesToString(stockpile);
//
//            List<String> row = Arrays.asList(
//                    nation.getName(),
//                    nation.getUrl(),
//                    status,
//                    ResourceType.resourcesToString(resources),
//                    stockpileStr,
//                    url,
//                    sentDm + "",
//                    sentMail + "",
//                    sentApi + ""
//            );
//            if (sentDmError != null || sentMailError != null) {
//                errorRows.add(row);
//            } else {
//                rows.add(row);
//            }
//            result.append("\n");
//        }
//        IMessageBuilder msg = io.create();
//        if (nations.size() == 1) {
//            attachErrors.accept(msg, errorRows, "errors");
//            if (customMessage != null && !customMessage.isEmpty()) {
//                result.append("> " + StringMan.join(customMessage.split("\n"), "\n> ")).append("\n");
//            }
//
//            return "**Excess resources can be deposited in the alliance bank.**\n" +
//                    "Use the pre-filled link below to deposit the recommended amount:\n" + result.toString();
//        } else {
//            attachErrors.accept(msg, errorRows, "errors");
//            attachErrors.accept(msg, rows, "results");
//            msg.append(result.toString());
//            msg.send();
//        }
//        return null;
//    }
//
//    @Command(desc = "View a nation's taxability, in-game tax rate, and internal tax-rate")
//    @IsAlliance
//    public String taxInfo(@Me IMessageIO io, @Me GuildDB db, @Me DBNation me, @Me User user, DBNation nation) {
//        if (nation == null) nation = me;
//        if (nation.getId() != me.getId()) {
//            if (user == null) throw new IllegalArgumentException("You must be registered to check other nation's tax info: " + CM.register.cmd.toSlashMention());
//            if (db == null) throw new IllegalArgumentException("Checking other nation's tax info must be done within a discord server");
//            if (!Roles.ECON_STAFF.has(user, db.getGuild())) {
//                throw new IllegalArgumentException("No permission to check other nation's tax info. Missing: " + Roles.ECON_STAFF.toDiscordRoleNameElseInstructions(db.getGuild()));
//            }
//        }
//        List<String> responses = new ArrayList<>();
//
//        if (nation.getPositionEnum().id <= Rank.APPLICANT.id) {
//            responses.add("Applicants are not taxed. ");
//        } else {
//            DBAlliance alliance = nation.getAlliance();
//            Map<Integer, TaxBracket> brackets = alliance.getTaxBrackets(TimeUnit.MINUTES.toMillis(5));
//            TaxBracket bracket = brackets.get(nation.getTax_id());
//            if (bracket == null) {
//                bracket = nation.getTaxBracket();
//            }
//            String taxRateStr = bracket.moneyRate >= 0 ? String.format("%d/%d", bracket.moneyRate, bracket.moneyRate) : "Unknown";
//            responses.add(String.format("**Bracket** %s: %s (tax_id:%d)", bracket.getName(), taxRateStr, nation.getTax_id()));
//            if (nation.isGray()) {
//                responses.add("`note: GRAY nations do not pay taxes. ");
//            } else if (nation.isBeige()) {
//                responses.add("`note: BEIGE nations do not pay taxes`");
//            } else if (nation.allianceSeniority() < 3) {
//                responses.add("`note: Nations with <2 days alliance seniority do not pay taxes`");
//            }
//
//            if (db != null) {
//                TaxRate internal = db.getHandler().getInternalTaxrate(nation.getNation_id());
//
//                if (internal.money == -1 && internal.resources == -1) {
//                    responses.add("`note: your taxes do NOT go into your deposits`");
//                } else {
//                    responses.add(String.format("**Internal tax rate:** %d/%d", internal.money, internal.resources));
//                    boolean payAboveMsg = false;
//                    if (internal.money >= bracket.moneyRate) {
//                        responses.add("`note: no $ from taxes goes into your deposits as internal $ rate is higher than your in-game tax $ rate`");
//                    } else {
//                        payAboveMsg = true;
//                    }
//                    if (internal.resources >= bracket.rssRate) {
//                        responses.add("`note: no resources from taxes goes into your deposits as internal resource rate is higher than your in-game tax resources rate`");
//                    } else {
//                        payAboveMsg = true;
//                    }
//                    if (payAboveMsg) {
//                        responses.add("`note: A portion of $/resources above the internal tax rate goes to the alliance`");
//                    }
//                }
//                if (Roles.ECON.has(user, db.getGuild())) {
//                    TaxRate taxBase = db.getOrNull(GuildKey.TAX_BASE);
//                    if (taxBase == null || ((internal.money == -1 && internal.resources == -1))) {
//                        responses.add("`note: set an internal taxrate with " + CM.nation.set.taxinternal.cmd.toSlashMention() + " or globally with " + CM.settings.info.cmd.toSlashMention() + " and key: " + GuildKey.TAX_BASE.name() + "`");
//                    }
//                    responses.add("\nTo view alliance wide bracket tax totals, use: " +
//                        CM.deposits.check.cmd.nationOrAllianceOrGuild("tax_id=" + bracket.taxId).showCategories("true"));
//                }
//            }
//        }
//
//        CM.deposits.check checkCmd = CM.deposits.check.cmd.nationOrAllianceOrGuild(nation.getId() + "").showCategories("true");
//        responses.add("\nTo view a breakdown of your deposits, use: " + checkCmd);
//
//        String title = "Tax info for " + nation.getName();
//        StringBuilder body = new StringBuilder();
//        body.append("**Nation:** ").append(nation.getNationUrlMarkup(true)).append("\n");
//        if (db != null && !db.isAllianceId(nation.getAlliance_id())) {
//            body.append("`note: nation is not in alliances: " + StringMan.getString(db.getAllianceIds()) + "`\n");
//        }
//        body.append(StringMan.join(responses, "\n"));
//
//        io.create().embed(title, body.toString()).send();
//        return null;
//    }
//
//
//    @Command(desc = "Send the funds in the alliance bank to an alliance added to the `offshore` coalition in the bot\n" +
//            "Optionally specify warchest and offshoring account", groups = {
//            "Account Settings",
//            "Resource Amounts",
//    })
//    @RolePermission(value = {Roles.MEMBER, Roles.ECON, Roles.ECON_STAFF}, alliance = true, any=true)
//    @HasOffshore
//    @IsAlliance
//    public static String offshore(@Me User user, @Me GuildDB db, @Me IMessageIO io,
//                                  @Arg(value = "Specify an alternative Offshore alliance to send funds in-game to\n" +
//                                          "Defaults to the currently set offshore coalition", group = 0) @Default DBAlliance to,
//                                  @Arg(value = "Specify an alternative account to offshore with\n" +
//                                          "Defaults to the sender alliance", group = 0) @Default NationOrAllianceOrGuild account,
//                                  @Arg(value = "The amount of resources to keep in the bank\n" +
//                                          "Defaults to keep nothing", group = 1) @Default("{}") Map<ResourceType, Double> keepAmount,
//                                  @Arg(value = "Specify specific resource amounts to offshore\n" +
//                                          "Defaults to all resources\n" +
//                                          "The send amount is auto capped by the resources available and `keepAmount`", group = 1)
//                                  @Default Map<ResourceType, Double> sendAmount) throws IOException {
//        if (account != null && account.isNation()) {
//            throw new IllegalArgumentException("You can't offshore into a nation. You can only offshore into an alliance or guild. Value provided: `Nation:" + account.getName() + "`");
//        }
//        if (account == null) {
//            NationOrAllianceOrGuild defAccount = GuildKey.DEFAULT_OFFSHORE_ACCOUNT.getOrNull(db);
//            if (defAccount != null) {
//                account = defAccount;
//            }
//        }
//        boolean memberCanOffshore = db.getOrNull(GuildKey.MEMBER_CAN_OFFSHORE) == Boolean.TRUE;
//        Roles checkRole = memberCanOffshore && account == null ? Roles.MEMBER : Roles.ECON;
//
//        AllianceList allianceList = checkRole.getAllianceList(user, db);
//        if (allianceList == null || allianceList.isEmpty()) {
//            StringBuilder msg = new StringBuilder("Missing Role: " + checkRole.toDiscordRoleNameElseInstructions(db.getGuild()));
//            if (memberCanOffshore && account != null) {
//                msg.append(". You do not have permission to specify an alternative account.");
//            }
//            if (!memberCanOffshore) {
//                msg.append(". See also: " + CM.settings.info.cmd.toSlashMention() + " with key " + GuildKey.MEMBER_CAN_OFFSHORE.name());
//            }
//            throw new IllegalArgumentException(msg.toString());
//        }
//        Set<Integer> offshores = db.getCoalition(Coalition.OFFSHORE);
//        if (to == null) {
//            OffshoreInstance offshore = db.getOffshore();
//            if (offshore == null) {
//                throw new IllegalArgumentException("No offshore found. See: " + CM.offshore.add.cmd.toSlashMention() + "  or " + CM.coalition.add.cmd.toSlashCommand() + " (for a non automated offshore)");
//            }
//            to = offshore.getAlliance();
//        } else {
//            if (!offshores.contains(to.getAlliance_id())) return "Please add the offshore using " + CM.coalition.add.cmd.alliances(to.getQualifiedId()).coalitionName(Coalition.OFFSHORE.name()) + "";
//        }
//        Set<DBAlliance> alliances = allianceList.getAlliances();
//        if (alliances.size() == 1 && alliances.iterator().next().equals(to)) {
//            throw new IllegalArgumentException("You cannot offshore to yourself");
//        }
//
//        List<TransferResult> results = new ArrayList<>();
//        for (DBAlliance from : alliances) {
//            if (from.getAlliance_id() == to.getAlliance_id()) continue;
//            Map<ResourceType, Double> resources = sendAmount != null ? sendAmount : from.getStockpile(true);
//            Map<ResourceType, Double> stockpile = sendAmount != null ? from.getStockpile(true) : new HashMap<>(resources);
//            Iterator<Map.Entry<ResourceType, Double>> iterator = resources.entrySet().iterator();
//            while (iterator.hasNext()) {
//                Map.Entry<ResourceType, Double> sendAmt = iterator.next();
//                ResourceType type = sendAmt.getKey();
//                double amt = sendAmt.getValue();
//                double stockpileAmt = stockpile.getOrDefault(type, 0d);
//                double maxSend = Math.max(0, stockpileAmt - keepAmount.getOrDefault(type, 0d));
//                if (amt > maxSend) {
//                    sendAmt.setValue(maxSend);
//                }
//            }
//            OffshoreInstance bank = from.getBank();
//            String note;
//            if (account != null) {
//                note = account.isAlliance() ? "#alliance=" + account.getId() : "#guild=" + account.getIdLong();
//            } else {
//                note = "#alliance=" + from.getAlliance_id();
//            }
//            note += " #tx_id=" + UUID.randomUUID().toString();
//            TransferResult response = bank.transferUnsafe2(null, to, resources, note, null);
//            results.add(response);
//        }
//        Map.Entry<String, String> embed = TransferResult.toEmbed(results);
//        io.create().embed(embed.getKey(), embed.getValue()).send();
//        return null;
//    }
//
//    @Command(desc = "Generate csv of war cost by nation between alliances (for reimbursement)\n" +
//            "Filters out wars where nations did not perform actions")
//    @RolePermission(Roles.ADMIN)
//    public String warReimburseByNationCsv(@Arg("The alliances with nations you want to reimburse") Set<DBAlliance> allies,
//                                          @Arg("The enemies during the conflict") Set<DBAlliance> enemies,
//                                          @Arg("Starting time of the conflict") @Timestamp long cutoff, @Arg("If wars with no actions by the defender should NOT be reimbursed") boolean removeWarsWithNoDefenderActions) {
//        Set<Integer> allyIds = allies.stream().map(f -> f.getAlliance_id()).collect(Collectors.toSet());
//        Set<Integer> enemyIds = enemies.stream().map(f -> f.getAlliance_id()).collect(Collectors.toSet());
//
//        Map<Integer, Integer> offensivesByNation = new HashMap<>();
//        Map<Integer, Integer> defensivesByNation = new HashMap<>();
//
//        Set<DBNation> nations = Locutus.imp().getNationDB().getNations(allyIds);
//        nations.removeIf(f -> f.isVacation() || f.active_m() > 10000 || f.getPosition() <= 1);
//        List<DBWar> wars = new ArrayList<>(Locutus.imp().getWarDb().getWarsForNationOrAlliance(null,
//                f -> (allyIds.contains(f) || enemyIds.contains(f)),
//                f -> (allyIds.contains(f.getAttacker_aa()) || allyIds.contains(f.getDefender_aa())) && (enemyIds.contains(f.getAttacker_aa()) || enemyIds.contains(f.getDefender_aa())) && f.getDate() > cutoff).values());
//
//        List<AbstractCursor> allattacks = Locutus.imp().getWarDb().getAttacksByWars(wars);
//        Map<Integer, List<AbstractCursor>> attacksByWar = new HashMap<>();
//        for (AbstractCursor attack : allattacks) {
//            attacksByWar.computeIfAbsent(attack.getWar_id(), f -> new ArrayList<>()).add(attack);
//        }
//
//        if (removeWarsWithNoDefenderActions) {
//            wars.removeIf(f -> {
//                List<AbstractCursor> attacks = attacksByWar.get(f.getWarId());
//                if (attacks == null) return true;
//                boolean att1 = attacks.stream().anyMatch(g -> g.getAttacker_id() == f.getAttacker_id());
//                boolean att2 = attacks.stream().anyMatch(g -> g.getAttacker_id() == f.getDefender_id());
//                return !att1 || !att2;
//            });
//        }
//
//        wars.removeIf(f -> {
//            List<AbstractCursor> attacks = attacksByWar.get(f.getWarId());
//            AttackCost cost = f.toCost(attacks, false, false, false, false, false);
//            boolean primary = allyIds.contains(f.getAttacker_aa());
//            return cost.convertedTotal(primary) <= 0;
//        });
//
//        for (DBWar war : wars) {
//            offensivesByNation.put(war.getAttacker_id(), offensivesByNation.getOrDefault(war.getAttacker_id(), 0) + 1);
//            defensivesByNation.put(war.getDefender_id(), defensivesByNation.getOrDefault(war.getDefender_id(), 0) + 1);
//        }
//
//
//        Map<Integer, double[]> warcostByNation = new HashMap<>();
//
//        for (DBWar war : wars) {
//            List<AbstractCursor> attacks = attacksByWar.get(war.getWarId());
//            AttackCost ac = war.toCost(attacks, false, false, false, false, false);
//            boolean primary = allies.contains(war.getAttacker_aa());
//            double[] units = ResourceType.resourcesToArray(ac.getUnitCost(primary));
//            double[] consume = ResourceType.resourcesToArray(ac.getConsumption(primary));
//
//            double[] cost = ResourceType.add(units, consume);
//
//            double[] warCostTotal = ResourceType.resourcesToArray(ac.getTotal(primary));
//            for (ResourceType type : ResourceType.values) {
//                cost[type.ordinal()] = Math.max(0, Math.min(warCostTotal[type.ordinal()], cost[type.ordinal()]));
//            }
//
//            int nationId = primary ? war.getAttacker_id() : war.getDefender_id();
//            double[] total = warcostByNation.computeIfAbsent(nationId, f -> ResourceType.getBuffer());
//            total = ResourceType.add(total, cost);
//        }
//
//
//        List<String> header = new ArrayList<>(Arrays.asList("nation", "off", "def"));
//        for (ResourceType type : ResourceType.values()) {
//            if (type != ResourceType.CREDITS) header.add(type.name());
//        }
//
//        List<String> lines = new ArrayList<>();
//        lines.add(StringMan.join(header, ","));
//        for (Map.Entry<Integer, double[]> entry : warcostByNation.entrySet()) {
//            int id = entry.getKey();
//            DBNation nation = DBNation.getById(id);
//            if (nation == null || !allies.contains(nation.getAlliance_id()) || nation.getPosition() <= 1 || nation.isVacation() || nation.active_m() > 7200 || nation.getCities() < 10) continue;
//            header.clear();
//            header.add(DNS.getName(id, false));
//            header.add(offensivesByNation.getOrDefault(id, 0) + "");
//            header.add(defensivesByNation.getOrDefault(id, 0) + "");
//            double[] cost = entry.getValue();
//            for (ResourceType type : ResourceType.values()) {
//                if (type != ResourceType.CREDITS) header.add(MathMan.format(cost[type.ordinal()]).replace(",", ""));
//            }
//
//            lines.add(StringMan.join(header, ","));
//        }
//        return StringMan.join(lines, "\n");
//    }
//
//    @Command(desc = "Set the escrow account balances for nation to the values in a spreadshet\n" +
//            "The sheet must have a `nation` column, and then a column for each resource type\n" +
//            "Escrow funds can be withdrawn at a later date by the receiver, such as when a blockade ends\n" +
//            "Use the deposits sheet command to get a spreadsheet of the current escrow balances")
//    @RolePermission(Roles.ECON)
//    @IsAlliance
//    @HasOffshore
//    public String setEscrowSheet(@Me GuildDB db,
//                                 @Me IMessageIO io,
//                                 @Me JSONObject command,
//                                    TransferSheet sheet,
//                                 @Default @Timediff Long expireAfter,
//                                 @Switch("f") boolean force) throws IOException {
//
//        Map<DBAlliance, Map<ResourceType, Double>> aaTransfers = sheet.getAllianceTransfers();
//        if (aaTransfers.isEmpty()) {
//            // cannot escrow for alliance (print the alliance names)
//            List<String> aaNames = aaTransfers.keySet().stream().map(DBAlliance::getName).collect(Collectors.toList());
//            throw new IllegalArgumentException("Alliances cannot have escrow balances: " + StringMan.join(aaNames, ", ") + " please remove them from the sheet and try again");
//        }
//
//        Map<DBNation, Map<ResourceType, Double>> transfers = sheet.getNationTransfers();
//        Map<DBNation, Set<ResourceType>> negativesByNation = new LinkedHashMap<>();
//        for (Map.Entry<DBNation, Map<ResourceType, Double>> entry : transfers.entrySet()) {
//            // ensure no negative values (map of nation: resource1,resource2 etc)
//            Set<ResourceType> negatives = entry.getValue().entrySet().stream().filter(f -> f.getValue() < 0).map(Map.Entry::getKey).collect(Collectors.toSet());
//            if (!negatives.isEmpty()) {
//                negativesByNation.put(entry.getKey(), negatives);
//            }
//        }
//        if (!negativesByNation.isEmpty()) {
//            StringBuilder response = new StringBuilder();
//            response.append("Nations cannot have negative escrow balances:\n");
//            for (Map.Entry<DBNation, Set<ResourceType>> entry : negativesByNation.entrySet()) {
//                response.append("- " + entry.getKey().getName()).append(": ").append(StringMan.join(entry.getValue(), ", ")).append("\n");
//            }
//            throw new IllegalArgumentException(response.toString());
//        }
//
//        Map<DBNation, double[]> transfersArr = new LinkedHashMap<>();
//        for (Map.Entry<DBNation, Map<ResourceType, Double>> entry : transfers.entrySet()) {
//            transfersArr.put(entry.getKey(), ResourceType.resourcesToArray(entry.getValue()));
//        }
//
//        return confirmAddOrSetEscrow(
//                false,
//                io,
//                db,
//                command,
//                new HashMap<>(),
//                "sheet:" + sheet.getSheet().getSpreadsheetId(),
//                transfersArr,
//                expireAfter,
//                force
//        );
//    }
//
//    @Command(desc = "Add funds to the escrow account for a set of nations\n" +
//            "Escrow funds can be withdrawn at a later date by the receiver, such as when a blockade ends\n" +
//            "To transfer funds from a nation's deposits into their escrow, see the transfer command")
//    @RolePermission(Roles.ECON)
//    @IsAlliance
//    @HasOffshore
//    public String addEscrow(@Me GuildDB db, @Me User author, @Me DBNation me,
//                            @Me IMessageIO io,
//                            @Me JSONObject command,
//                            NationList nations,
//                            @Switch("b") @Arg("The base amount of resources to escrow\n" +
//                                    "If per city is set, the highest value of each resource is chosen") Map<ResourceType, Double> amountBase,
//                            @Switch("p") @Arg("Amount of resources to escrow for each city the receiver has\n" +
//                                    "If base is set, the highest value of each resource is chosen\n" +
//                                    "This uses the city count now, not when the funds are withdrawn later") Map<ResourceType, Double> amountPerCity,
//                            @Switch("e") @Arg("Additional resources to escrow\n" +
//                                    "If a base or per city are set, this adds to what is calculated for that") Map<ResourceType, Double> amountExtra,
//                            @Arg("Don't add escrow resources that the nation has in their stockpile") @Switch("s") boolean subtractStockpile,
//                            @Arg("When the nation has these units, don't add the resources equivalent to their cost\n" +
//                                    "Useful to only give resources to those missing units")
//                            @Switch("m") Set<MilitaryUnit> subtractNationsUnits,
//
//                            @Arg("Do not add escrow resources that the nation has in their deposits")
//                            @Switch("d") boolean subtractDeposits,
//
//
//                            @Arg("Delete all receiver escrow after a time period\nRecommended: 5d") @Default @Timediff Long expireAfter,
//                            @Switch("f") boolean force) throws IOException {
//        return addOrSetEscrow(true, db, io, command, nations, amountBase, amountPerCity, amountExtra, subtractStockpile, subtractNationsUnits, subtractDeposits, expireAfter, force);
//    }
//
//    @Command(desc = "Set the escrow account balances for a set of nations\n" +
//            "Escrow funds can be withdrawn at a later date by the receiver, such as when a blockade ends\n" +
//            "To transfer funds from a nation's deposits into their escrow, see the transfer command")
//    @RolePermission(Roles.ECON)
//    @IsAlliance
//    @HasOffshore
//    public String setEscrow(@Me GuildDB db, @Me User author, @Me DBNation me,
//                            @Me IMessageIO io,
//                            @Me JSONObject command,
//                            NationList nations,
//                            @Switch("b") @Arg("The base amount of resources to escrow\n" +
//                                    "If per city is set, the highest value of each resource is chosen") Map<ResourceType, Double> amountBase,
//                            @Switch("p") @Arg("Amount of resources to escrow for each city the receiver has\n" +
//                                    "If base is set, the highest value of each resource is chosen\n" +
//                                    "This uses the city count now, not when the funds are withdrawn later") Map<ResourceType, Double> amountPerCity,
//                            @Switch("e") @Arg("Additional resources to escrow\n" +
//                                    "If a base or per city are set, this adds to what is calculated for that") Map<ResourceType, Double> amountExtra,
//                            @Arg("Don't add escrow resources that the nation has in their stockpile") @Switch("s") boolean subtractStockpile,
//                            @Arg("When the nation has these units, don't add the resources equivalent to their cost\n" +
//                                    "Useful to only give resources to those missing units")
//                            @Switch("m") Set<MilitaryUnit> subtractNationsUnits,
//                            @Arg("Do not add escrow resources that the nation has in their deposits")
//                            @Switch("d") boolean subtractDeposits,
//                            @Arg("Delete all receiver escrow after a time period\nRecommended: 5d") @Default @Timediff Long expireAfter,
//                            @Switch("f") boolean force) throws IOException {
//        return addOrSetEscrow(false, db, io, command, nations, amountBase, amountPerCity, amountExtra, subtractStockpile, subtractNationsUnits, subtractDeposits, expireAfter, force);
//    }
//
//    public String confirmAddOrSetEscrow(boolean isAdd, IMessageIO io, GuildDB db, JSONObject command, Map<DBNation, OffshoreInstance.TransferStatus> errors, String nationsName, Map<DBNation, double[]> amountToSetOrAdd, Long expireAfter, boolean force) throws IOException {
//        long expireEpoch = expireAfter == null ? 0 : System.currentTimeMillis() + expireAfter;
//
//        if (!force || amountToSetOrAdd.isEmpty()) {
//            String title;
//            if (isAdd) {
//                title = "Add to Escrow to " + nationsName;
//            } else {
//                title = "Set escrow for " + nationsName;
//            }
//            List<String> warnings = new ArrayList<>();
//            int blockaded = 0;
//            int unblockaded = 0;
//            for (DBNation nation : amountToSetOrAdd.keySet()) {
//                if (nation.isBlockaded()) {
//                    blockaded++;
//                } else {
//                    unblockaded++;
//                }
//                if (nation.isVacation()) {
//                    warnings.add(nation.getName() + " is in VM mode");
//                    continue;
//                }
//                if (nation.active_m() > 2880) {
//                    warnings.add(nation.getName() + " is inactive for " + TimeUtil.secToTime(TimeUnit.MINUTES, nation.active_m()));
//                    continue;
//                }
//                if (!db.isAllianceId(nation.getAlliance_id())) {
//                    warnings.add(nation.getName() + " is not a member");
//                    continue;
//                }
//                if (nation.getPositionEnum() == Rank.APPLICANT) {
//                    warnings.add(nation.getName() + " is an applicant");
//                    continue;
//                }
//                if (nation.isGray()) {
//                    warnings.add(nation.getName() + " is gray");
//                    continue;
//                }
//            }
//
//            StringBuilder body = new StringBuilder();
//            if (amountToSetOrAdd.size() == 1) {
//                DBNation nation = amountToSetOrAdd.keySet().iterator().next();
//                body.append(nation.getNationUrlMarkup(true) + " | " + nation.getAllianceUrlMarkup(true) + "\n");
//                if (nation.isBlockaded()) {
//                    body.append("`BLOCKADED`\n");
//                } else {
//                    body.append("`NOT BLOCKADED`\n");
//                }
//            } else {
//                SimpleNationList nations = new SimpleNationList(amountToSetOrAdd.keySet());
//                body.append("To: `" + nationsName + "` (" + nations.getNations().size() + " nations in " + nations.getAllianceIds().size() + " alliances)\n");
//                if (blockaded > 0) {
//                    body.append(" | " + blockaded + " blockaded");
//                }
//                if (unblockaded > 0) {
//                    body.append(" | " + unblockaded + " unblockaded");
//                }
//                body.append("\n");
//            }
//            String verb = isAdd ? "adding to Escrow" : "setting Escrow to ";
//            double[] total = ResourceType.getBuffer();
//            for (double[] amount : amountToSetOrAdd.values()) {
//                ResourceType.add(total, amount);
//            }
//            body.append("\nTotal " + verb + ":\n`" + ResourceType.resourcesToString(total) + "` worth: ~$" + MathMan.format(ResourceType.convertedTotal(total)) + "\n");
//
//            if (expireAfter != null) {
//                body.append("\nSetting the expiry for all escrowed to " + DiscordUtil.timestamp(expireEpoch, null)).append("\n");
//            } else {
//                body.append("\nSetting all receiver escrow to not expire. Use `expireAfter` to set an expiry\n");
//            }
//
//            String bodyStr = body.toString();
//
//            if (!warnings.isEmpty()) {
//                body.append("\n**Warnings**:\n");
//                for (String warning : warnings) {
//                    body.append("- " + warning).append("\n");
//                }
//            }
//
//            if (!errors.isEmpty()) {
//                body.append("\n**Errors**:\n");
//                for (Map.Entry<DBNation, OffshoreInstance.TransferStatus> entry : errors.entrySet()) {
//                    body.append("- " + entry.getKey().getName() + ": " + entry.getValue()).append("\n");
//                }
//            }
//
//            IMessageBuilder msg = io.create();
//
//            if (body.length() > bodyStr.length() && body.length() > 4000) {
//                bodyStr += "\n\nWarnings: " + warnings.size() + "\nErrors: " + errors.size() + "\n";
//                bodyStr += "(see attached file)";
//                msg.file("errors.txt", StringMan.join(errors.entrySet(), "\n"));
//                msg.file("warnings.txt", StringMan.join(warnings, "\n"));
//            } else {
//                bodyStr = body.toString();
//            }
//
//            if (amountToSetOrAdd.isEmpty()) {
//                msg.append(body.toString()).send();
//                return null;
//            }
//
//            msg.confirmation(title, body.toString(), command).send();
//            return null;
//        }
//
//        List<String> response = new ArrayList<>();
//        // add all errors
//        for (Map.Entry<DBNation, OffshoreInstance.TransferStatus> entry : errors.entrySet()) {
//            response.add(entry.getKey().getName() + ": " + entry.getValue());
//        }
//        for (Map.Entry<DBNation, double[]> entry : amountToSetOrAdd.entrySet()) {
//            DBNation nation = entry.getKey();
//            double[] amount = entry.getValue();
//            Object lock = OffshoreInstance.NATION_LOCKS.computeIfAbsent(entry.getKey().getId(), k -> new Object());
//            synchronized (lock) {
//                Map.Entry<double[], Long> currentPair = db.getEscrowed(nation);
//                double[] current = currentPair == null ? ResourceType.getBuffer() : currentPair.getKey();
//                long expireEpochNation = expireEpoch;
//                if (expireEpochNation == 0) {
//                    expireEpochNation = currentPair == null ? 0 : currentPair.getValue();
//                }
//                double[] newAmount;
//                if (isAdd) {
//                    newAmount = ResourceType.add(current.clone(), amount);
//                } else {
//                    newAmount = amount;
//                }
//                if (ResourceType.equals(current, newAmount)) {
//                    response.add("No changes for " + nation.getName());
//                    continue;
//                }
//                db.setEscrowed(nation, newAmount, expireEpochNation);
//                response.add(nation.getName() + ": `" + ResourceType.resourcesToString(amount) + "` worth: ~$" + MathMan.format(ResourceType.convertedTotal(amount)) + " added to escrow. New escrowed: `" + ResourceType.resourcesToString(newAmount) + "`");
//            }
//        }
//
//        return StringMan.join(response, "\n") + "\n\nSee also: " + CM.deposits.reset.cmd.toSlashMention();
//    }
//
//    public String addOrSetEscrow(boolean isAdd,
//                            @Me GuildDB db,
//                            @Me IMessageIO io,
//                            @Me JSONObject command,
//                            NationList nations,
//                            @Switch("b") @Arg("The base amount of resources to escrow\n" +
//                                    "If per city is set, the highest value of each resource is chosen") Map<ResourceType, Double> amountBase,
//                            @Switch("p") @Arg("Amount of resources to escrow for each city the receiver has\n" +
//                                    "If base is set, the highest value of each resource is chosen\n" +
//                                    "This uses the city count now, not when the funds are withdrawn later") Map<ResourceType, Double> amountPerCity,
//                            @Switch("e") @Arg("Additional resources to escrow\n" +
//                                    "If a base or per city are set, this adds to what is calculated for that") Map<ResourceType, Double> amountExtra,
//                            @Arg("Don't add escrow resources that the nation has in their stockpile") @Switch("s") boolean subtractStockpile,
//                            @Arg("When the nation has these units, don't add the resources equivalent to their cost\n" +
//                                    "Useful to only give resources to those missing units")
//                            @Switch("m") Set<MilitaryUnit> subtractNationsUnits,
//
//                            @Arg("Do not add escrow resources that the nation has in their deposits")
//                            @Switch("d") boolean subtractDeposits,
//
//
//                            @Arg("Delete all receiver escrow after a time period\nRecommended: 5d") @Default @Timediff Long expireAfter,
//                            @Switch("f") boolean force) throws IOException {
//        if (nations.getNations().size() > 300) {
//            return "Too many nations: " + nations.getNations().size() + " (max: 300)";
//        }
//        if (amountBase == null && amountPerCity == null && amountExtra == null) {
//            return "No amount specified. Please specify at least one of: `amountBase`, `amountPerCity`, `amountExtra`";
//        }
//        if (db.getOrNull(GuildKey.RESOURCE_REQUEST_CHANNEL) == null) {
//            return "No resource request channel set. See " + GuildKey.RESOURCE_REQUEST_CHANNEL.getCommandMention() + "";
//        }
//
//        Map<DBNation, OffshoreInstance.TransferStatus> errors = new LinkedHashMap<>();
//        Map<DBNation, Map<ResourceType, Double>> memberStockpile = null;
//
//        if (subtractStockpile) {
//            for (DBNation nation : nations.getNations()) {
//                if (!db.isAllianceId(nation.getAlliance_id())) {
//                    return "Nation: " + nation.getName() + "(alliance id:" + nation.getAlliance_id() + ") is not a member of this guild's alliances: " + db.getAllianceIds();
//                }
//            }
//            memberStockpile = db.getAllianceList().subList(nations.getNations()).getMemberStockpile();
//        }
//
//        if (!isAdd) {
//            // ensure all are positive amounts
//            if (amountBase != null) {
//                for (Map.Entry<ResourceType, Double> entry : amountBase.entrySet()) {
//                    if (entry.getValue() < 0) {
//                        throw new IllegalArgumentException("`amountBase` is invalid. Cannot set negative escrow amounts: " + entry.getKey() + " = " + MathMan.format(entry.getValue()));
//                    }
//                }
//            }
//            if (amountPerCity != null) {
//                for (Map.Entry<ResourceType, Double> entry : amountPerCity.entrySet()) {
//                    if (entry.getValue() < 0) {
//                        throw new IllegalArgumentException("`amountPerCity` is invalid. Cannot set negative escrow amounts: " + entry.getKey() + " = " + MathMan.format(entry.getValue()));
//                    }
//                }
//            }
//            if (amountExtra != null) {
//                for (Map.Entry<ResourceType, Double> entry : amountExtra.entrySet()) {
//                    if (entry.getValue() < 0) {
//                        throw new IllegalArgumentException("`amountExtra` is invalid. Cannot set negative escrow amounts: " + entry.getKey() + " = " + MathMan.format(entry.getValue()));
//                    }
//                }
//            }
//        }
//
//        Map<DBNation, double[]> amountToSetOrAdd = new LinkedHashMap<>();
//
//        for (DBNation nation : nations.getNations()) {
//            double[] amount = ResourceType.getBuffer();
//            if (amountBase != null) {
//                amount = ResourceType.resourcesToArray(amountBase);
//            }
//            if (amountPerCity != null) {
//                int cities = nation.getCities();
//                double[] perCity = ResourceType.resourcesToArray(amountPerCity);
//                for (int i = 0; i < perCity.length; i++) {
//                    amount[i] = Math.max(perCity[i] * cities, amount[i]);
//                }
//            }
//            if (amountExtra != null) {
//                double[] extra = ResourceType.resourcesToArray(amountExtra);
//                ResourceType.add(amount, extra);
//            }
//
//            if (subtractStockpile) {
//                Map<ResourceType, Double> stockpile = memberStockpile.get(nation);
//                if (stockpile == null) {
//                    errors.put(nation, OffshoreInstance.TransferStatus.ALLIANCE_ACCESS);
//                    continue;
//                }
//                double[] stockpileArr = ResourceType.resourcesToArray(stockpile);
//                for (int i = 0; i < stockpileArr.length; i++) {
//                    amount[i] = Math.max(Math.min(0, amount[i]), amount[i] - stockpileArr[i]);
//                }
//            }
//
//            if (subtractNationsUnits != null && !subtractNationsUnits.isEmpty()) {
//                for (MilitaryUnit unit : subtractNationsUnits) {
//                    int numUnits = nation.getUnits(unit);
//                    double[] cost = unit.getCost(numUnits);
//                    for (int i = 0; i < cost.length; i++) {
//                        amount[i] = Math.max(Math.min(0, amount[i]), amount[i] - cost[i]);
//                    }
//                }
//            }
//
//            if (subtractDeposits) {
//                double[] deposits = nation.getNetDeposits(db, -1L, true);
//                for (int i = 0; i < deposits.length; i++) {
//                    double amt = deposits[i];
//                    if (amt > 0) {
//                        amount[i] = Math.max(Math.min(0, amount[i]), amount[i] - deposits[i]);
//                    }
//                }
//            }
//            ResourceType.max(amount, ResourceType.getBuffer());
//
//            amountToSetOrAdd.put(nation, amount);
//        }
//
//        return confirmAddOrSetEscrow(
//                isAdd,
//                io,
//                db,
//                command,
//                errors,
//                nations.getFilter(),
//                amountToSetOrAdd,
//                expireAfter,
//                force
//        );
//    }
//
//    @Command(desc = "Disburse raw resources needed to operate cities", aliases = {"disburse", "disperse"}, groups = {
//        "Amount Options",
//        "Optional: Bank Note",
//        "Optional: Nation Account",
//        "Optional: Specify Offshore/Bank",
//        "Optional: Tax Bracket Account (pick either or none)"
//    })
//    @RolePermission(value = {Roles.ECON_WITHDRAW_SELF, Roles.ECON}, alliance = true, any = true)
//    @IsAlliance
//    public static String disburse(@Me User author, @Me GuildDB db, @Me IMessageIO io, @Me DBNation me,
//
//                                  @Arg("The nations to send to")
//                                  NationList nationList,
//                                  @Arg(value = "Days of operation to send", group = 0, aliases = "daysdefault") @Range(min=0, max=20) double days,
//                                  @Arg(value = "Do not send money below the daily login bonus", group = 0) @Switch("dc") boolean no_daily_cash,
//                                  @Arg(value = "Do not send ANY money", group = 0) @Switch("c") boolean no_cash,
//
//                                  @Arg(value = "Transfer note\nUse `#IGNORE` to not deduct from deposits", group = 1, aliases = "deposittype") @Default("#tax") DepositType.DepositTypeInfo bank_note,
//                                  @Arg(value = "Have the transfer ignored from nation holdings after a timeframe", group = 1) @Switch("e") @Timediff Long expire,
//                                  @Arg(value = "Have the transfer decrease linearly from balances over a timeframe", group = 1) @Switch("d") @Timediff Long decay,
//                                  @Arg(value = "Have the transfer valued as cash in nation holdings", group = 1, aliases = "converttomoney") @Switch("m") boolean deduct_as_cash,
//
//                           @Arg(value = "The guild's nation account to deduct from\n" +
//                                   "Defaults to None if bulk disburse, else the receivers account", group = 2, aliases = "depositsaccount") @Switch("n") DBNation nation_account,
//                                  @Arg(value = "How to handle the transfer if the receiver is blockaded\n" +
//                                          "Defaults to never escrow", group = 2) @Switch("em") EscrowMode escrow_mode,
//
//                           @Arg(value = "The in-game alliance bank to send from\nDefaults to the offshore set", group = 3, aliases = "usealliancebank") @Switch("a") DBAlliance ingame_bank,
//                           @Arg(value = "The account with the offshore to use\n" +
//                                   "The alliance must be registered to this guild\n" +
//                                   "Defaults to all the alliances of this guild", group = 3, aliases = "useoffshoreaccount") @Switch("o") DBAlliance offshore_account,
//
//                           @Arg(value = "The tax account to deduct from", group = 4) @Switch("t") TaxBracket tax_account,
//                           @Arg(value = "Deduct from the receiver's tax bracket account", group = 4, aliases = "existingtaxaccount") @Switch("ta") boolean use_receiver_tax_account,
//
//
//                           @Arg("Skip checking receiver activity, blockade, VM etc.")
//                           @Switch("b") boolean bypass_checks,
//                           @Switch("f") boolean force) throws GeneralSecurityException, IOException, ExecutionException, InterruptedException {
//        Set<DBNation> nations = new HashSet<>(nationList.getNations());
//
//        AllianceList allianceList = db.getAllianceList();
//        if (allianceList == null) {
//            throw new IllegalArgumentException("This guild is not registered to an alliance. See: " + GuildKey.ALLIANCE_ID.getCommandMention());
//        }
//        for (DBNation nation : nations) {
//            if (!allianceList.contains(nation.getAlliance())) {
//                throw new IllegalArgumentException("This guild is registered to: " + StringMan.getString(allianceList.getIds()) +
//                        " but you are trying to disburse to a nation in " + nation.getAlliance_id() +
//                        "\nConsider using a different list of nations or registering the alliance to this guild (" +
//                        CM.settings.info.cmd.toSlashMention() + " with key `" +  GuildKey.ALLIANCE_ID.name() + "`)");
//            }
//        }
//
//        List<String> output = new ArrayList<>();
//        List<TransferResult> allStatuses = new ArrayList<>();
//
//        if (nations.size() != 1) {
//            int originalSize = nations.size();
//            Iterator<DBNation> iter = nations.iterator();
//            while (iter.hasNext()) {
//                DBNation nation = iter.next();
//                OffshoreInstance.TransferStatus status = OffshoreInstance.TransferStatus.SUCCESS;
//                String debug = "";
//                if (nation.getPosition() <= 1) status = OffshoreInstance.TransferStatus.APPLICANT;
//                else if (nation.isVacation()) status = OffshoreInstance.TransferStatus.VACATION_MODE;
//                else if (!db.isAllianceId(nation.getAlliance_id())) status = OffshoreInstance.TransferStatus.NOT_MEMBER;
//                else if (!bypass_checks) {
//                    if (nation.active_m() > 2880) {
//                        status = OffshoreInstance.TransferStatus.INACTIVE;
//                        debug += " (2+ days)";
//                    }
//                    if (nation.isGray()) status = OffshoreInstance.TransferStatus.GRAY;
//                    if (nation.isBeige() && nation.getCities() <= 4) status = OffshoreInstance.TransferStatus.BEIGE;
//                    if (!status.isSuccess()) debug += " (use the `bypasschecks` parameter to override)";
//                }
//                if (!status.isSuccess()) {
//                    iter.remove();
//                    allStatuses.add(new TransferResult(status, nation, new HashMap<>(), bank_note.toString()).addMessage(status.getMessage() + debug));
//                }
//            }
//            int removed = originalSize - nations.size();
//            if (removed > 0) {
//                output.add("Removed " + removed + " invalid nations (see errors.csv)");
//            }
//        }
//
//        IMessageBuilder msg = io.create();
//
//        if (nations.isEmpty()) {
//            msg.append("No nations found (1)\n" + StringMan.join(output, "\n"));
//            if (!allStatuses.isEmpty()) {
//                msg.file("errors.csv", TransferResult.toFileString(allStatuses));
//                msg.append("\nSummary: `" + TransferResult.count(allStatuses) + "`");
//            }
//            msg.send();
//            return null;
//        }
//
//        Map<DBNation, Map.Entry<OffshoreInstance.TransferStatus, double[]>> funds = allianceList.calculateDisburse(nations, null, days, true, bypass_checks, true, no_daily_cash, no_cash, bypass_checks, force);
//        Map<DBNation, Map<ResourceType, Double>> fundsToSendNations = new LinkedHashMap<>();
//        for (Map.Entry<DBNation, Map.Entry<OffshoreInstance.TransferStatus, double[]>> entry : funds.entrySet()) {
//            DBNation nation = entry.getKey();
//            Map.Entry<OffshoreInstance.TransferStatus, double[]> value = entry.getValue();
//            OffshoreInstance.TransferStatus status = value.getKey();
//            double[] amount = value.getValue();
//            if (status == OffshoreInstance.TransferStatus.SUCCESS) {
//                fundsToSendNations.put(nation, ResourceType.resourcesToMap(amount));
//            } else {
//                allStatuses.add(new TransferResult(status, nation, ResourceType.resourcesToMap(amount), bank_note.toString()).addMessage(status.getMessage()));
//            }
//        }
//
//        if (fundsToSendNations.size() <= 1 || !force) {
//            if (!allStatuses.isEmpty()) {
//                msg.file("errors.csv", TransferResult.toFileString(allStatuses));
//                msg.append("Summary: `" + TransferResult.count(allStatuses) + "`");
//            }
//            if (fundsToSendNations.isEmpty()) {
//                msg.append("\nError. No funds to send.");
//                msg.send();
//                return null;
//            } else {
//                msg.send();
//            }
//        }
//        if (fundsToSendNations.size() == 1) {
//            Map.Entry<DBNation, Map<ResourceType, Double>> entry = fundsToSendNations.entrySet().iterator().next();
//            DBNation nation = entry.getKey();
//            Map<ResourceType, Double> transfer = entry.getValue();
//
//            JSONObject command = CM.transfer.resources.cmd.receiver(
//                    nation.getUrl()).transfer(
//                    ResourceType.resourcesToString(transfer)).depositType(
//                    bank_note.toString()).nationAccount(
//                    nation_account != null ? nation_account.getUrl() : null).senderAlliance(
//                    ingame_bank != null ? ingame_bank.getUrl() : null).allianceAccount(
//                    offshore_account != null ? offshore_account.getUrl() : null).taxAccount(
//                    tax_account != null ? tax_account.getQualifiedId() : null).existingTaxAccount(
//                    use_receiver_tax_account + "").onlyMissingFunds(
//                    Boolean.FALSE.toString()).expire(
//                    expire == null ? null : TimeUtil.secToTime(TimeUnit.MILLISECONDS, expire)).decay(
//                    decay == null ? null : TimeUtil.secToTime(TimeUnit.MILLISECONDS, decay)).convertCash(
//                    String.valueOf(deduct_as_cash)).escrow_mode(
//                    escrow_mode == null ? null : escrow_mode.name()).bypassChecks(
//                    String.valueOf(bypass_checks)).force(
//                    String.valueOf(force)
//            ).toJson();
//
//            return transfer(io, command, author, me, db, nation, transfer, bank_note, nation_account, ingame_bank, offshore_account, tax_account, use_receiver_tax_account, false, expire, decay, null, deduct_as_cash, escrow_mode, bypass_checks, force);
//        } else {
//            UUID key = UUID.randomUUID();
//            TransferSheet sheet = new TransferSheet(db).write(fundsToSendNations, new LinkedHashMap<>()).build();
//            APPROVED_BULK_TRANSFER.put(key, sheet.getTransfers());
//
//            JSONObject command = CM.transfer.bulk.cmd.sheet(
//                    sheet.getSheet().getURL()).depositType(
//                    bank_note.toString()).depositsAccount(
//                    nation_account != null ? nation_account.getUrl() : null).useAllianceBank(
//                    ingame_bank != null ? ingame_bank.getUrl() : null).useOffshoreAccount(
//                    offshore_account != null ? offshore_account.getUrl() : null).taxAccount(
//                    tax_account != null ? tax_account.getQualifiedId() : null).existingTaxAccount(
//                    use_receiver_tax_account + "").expire(
//                    expire == null ? null : TimeUtil.secToTime(TimeUnit.MILLISECONDS, expire)).decay(
//                    decay == null ? null : TimeUtil.secToTime(TimeUnit.MILLISECONDS, decay)).convertToMoney(
//                    Boolean.FALSE.toString()).escrow_mode(
//                    escrow_mode == null ? null : escrow_mode.name()).bypassChecks(
//                    String.valueOf(bypass_checks)).force(
//                    String.valueOf(force)).key(
//                    key.toString()
//            ).toJson();
//            Map errors = force ? new HashMap<>() : TransferResult.toMap(allStatuses);
//            return transferBulkWithErrors(io, command, author, me, db, sheet, bank_note, nation_account, ingame_bank, offshore_account, tax_account, use_receiver_tax_account, expire, decay, deduct_as_cash, escrow_mode, bypass_checks, force, key, errors);
//        }
//    }
//
    @Command(desc = "Get a sheet of members and their revenue (compared to optimal city builds)")
    @RolePermission(value = {Roles.ECON_STAFF, Roles.ECON})
    @IsAlliance
    public String revenueSheet(@Me IMessageIO io, @Me GuildDB db, NationList nations, @Switch("s") SpreadSheet sheet, @Switch("t") @Timestamp Long snapshotTime) throws GeneralSecurityException, IOException, ExecutionException, InterruptedException {
        Set<DBNation> nationSet = DNS.getNationsSnapshot(nations.getNations(), nations.getFilter(), snapshotTime, db.getGuild(), false);
        if (sheet == null) {
            sheet = SpreadSheet.create(db, SheetKey.REVENUE_SHEET);
        }

        Set<Integer> ids = db.getAllianceIds(false);
        int sizeOriginal = nationSet.size();
        nationSet.removeIf(f -> f.getPosition() <= Rank.APPLICANT.id || !ids.contains(f.getAlliance_id()));
        nationSet.removeIf(f -> f.isVacation());
        int numRemoved = sizeOriginal - nationSet.size();
        if (nationSet.isEmpty()) {
            return "No nations to process. " + numRemoved + " nations were removed from the list. Please ensure they are members of your alliance";
        }

        List<String> header = new ArrayList<>(Arrays.asList(
                "nation",
                "infra",
                "land",
                "revenue[converted]"
        ));
        for (ResourceType type : ResourceType.values) {
            header.add(type.name());
        }
        sheet.setHeader(header);

        Function<DBNation, List<String>> addRowTask = nation -> {
            Map<ResourceType, Double> revenue = nation.getRevenue();
            double revenueConverted = ResourceType.convertedTotal(revenue);


            List<String> row = new ArrayList<>(header);
            row.set(0, MarkupUtil.sheetUrl(nation.getNation(), DNS.getUrl(nation.getNation_id(), false)));
            row.set(1, MathMan.format(nation.getInfra()));
            row.set(2, MathMan.format(nation.getLand()));
            row.set(3, MathMan.format(revenueConverted));

            int i = 4;
            for (ResourceType type : ResourceType.values) {
                row.set(i++, MathMan.format(revenue.getOrDefault(type, 0d)));
            }

            return row;
        };

        List<Future<List<String>>> addRowFutures = new ArrayList<>();

        for (DBNation nation : nationSet) {
            Future<List<String>> future = Locutus.imp().getExecutor().submit(() -> addRowTask.apply(nation));
            addRowFutures.add(future);
        }

        for (Future<List<String>> future : addRowFutures) {
            sheet.addRow(future.get());
        }

        sheet.updateClearCurrentTab();
        sheet.updateWrite();

        sheet.attach(io.create(), "revenue").send();
        return null;
    }

    @Command(desc = "Get a sheet of members and their saved up warchest (can include deposits and potential revenue)")
    @RolePermission(value = {Roles.ECON_STAFF, Roles.ECON, Roles.MILCOM, Roles.MILCOM_NO_PINGS})
    @IsAlliance
    public String warchestSheet(@Me GuildDB db, @Me IMessageIO io, Set<DBNation> nations,
                                @Arg("The required warchest per infra. Else uses the guild default") @Switch("c") Map<ResourceType, Double> perInfraWarchest,
                                @Arg("Count current grants against warchest totals") @Switch("g") boolean includeGrants,
                                @Arg("If negative deposits are NOT normalized (to ignore negatives)") @Switch("n") boolean doNotNormalizeDeposits,
                                @Arg("If deposits are included in warchest totals") @Switch("d") boolean includeDeposits,
                                @Arg("Do not count resources above the required amount toward total warchest value") @Switch("e") boolean ignoreStockpileInExcess,
                                @Arg("Include days of potential revenue toward warchest resources")@Switch("r") Integer includeRevenueDays,
                                @Switch("f") boolean forceUpdate) throws IOException, GeneralSecurityException {
        AllianceList alliance = db.getAllianceList();
        if (alliance == null) {
            throw new IllegalArgumentException("This guild is not registered to an alliance. See: " + GuildKey.ALLIANCE_ID.getCommandMention());
        }
        Map<DBNation, Map<ResourceType, Double>> stockpiles = alliance.getMemberStockpile();

        List<String> errors = new ArrayList<>();

        Map<DBNation, double[]> warchestByNation = new HashMap<>();
        Map<DBNation, double[]> warchestLackingByNation = new HashMap<>();
        Map<DBNation, double[]> warchestExcessByNation = new HashMap<>();
        Map<DBNation, double[]> revenueByNation = new HashMap<>();
        Function<DBNation, Map<ResourceType, Double>> wcReqFunc = f -> {
            return perInfraWarchest != null ? perInfraWarchest : db.getPerInfraWarchest(f);
        };

        for (DBNation nation : nations) {
            Map<ResourceType, Double> myStockpile = stockpiles.get(nation);
            if (myStockpile == null) {
                errors.add("Could not fetch stockpile of: " + nation.getNation() + "/" + nation.getNation_id());
                continue;
            }
            double[] stockpileArr2 = ResourceType.resourcesToArray(myStockpile);
            double[] total = stockpileArr2.clone();
            double[] depo = ResourceType.getBuffer();
            if (includeDeposits) {
                depo = nation.getNetDeposits(db, includeGrants, forceUpdate ? 0L : -1L);
                if (!doNotNormalizeDeposits) {
                    depo = DNS.normalize(depo);
                }
                for (int i = 0; i < depo.length; i++) {
                    if (depo[i] > 0) total[i] += depo[i];
                }
            }
            double[] revenue = ResourceType.resourcesToArray(nation.getRevenue());
            revenueByNation.put(nation, revenue);
            if (includeRevenueDays != null) {
                for (int i = 0; i < revenue.length; i++) {
                    total[i] += revenue[i] * includeRevenueDays;
                }
            }

            double[] warchest = ResourceType.getBuffer();
            double[] warchestLacking = ResourceType.getBuffer();
            double[] warchestExcess = ResourceType.getBuffer();

            Map<ResourceType, Double> localPerCityWarchest = wcReqFunc.apply(nation);

            boolean lacking = false;
            boolean excess = false;
            for (Map.Entry<ResourceType, Double> entry : localPerCityWarchest.entrySet()) {
                ResourceType type = entry.getKey();
                double required = entry.getValue() * nation.getInfra();
                double current = Math.max(0, total[type.ordinal()]);
                double net = current - required;

                if (net < -1) {
                    lacking = true;
                    warchestLacking[type.ordinal()] += Math.abs(net);
                } else if (net > 1) {
                    if (ignoreStockpileInExcess) {
                        net -= (stockpileArr2[type.ordinal()] + Math.max(0, depo[type.ordinal()]));
                    }
                    if (net > 1) {
                        warchestExcess[type.ordinal()] += net;
                        excess = true;
                    }
                }

                warchest[type.ordinal()] = total[type.ordinal()];
            }

            warchestByNation.put(nation, warchest);
            if (lacking) {
                warchestLackingByNation.put(nation, warchestLacking);
            }
            if (excess) {
                warchestExcessByNation.put(nation, warchestExcess);
            }
        }

        double[] totalWarchest = ResourceType.getBuffer();
        double[] totalNet = ResourceType.getBuffer();

        SpreadSheet sheet = SpreadSheet.create(db, SheetKey.WARCHEST_SHEET);
        List<String> header = new ArrayList<>(Arrays.asList(
                "nation",
                "infra",
                "land",
                "strength",
                "WC %",
                "WC % (converted)",
                "Revenue Contribution Individual %",
                "Revenue Contribution Aggregate %",
                "Missing WC",
                "Excess WC",
                "WC",
                "Missing WC val",
                "Excess WC val"
        ));

        sheet.setHeader(header);

        double[] empty = ResourceType.getBuffer();
        for (Map.Entry<DBNation, double[]> entry : warchestByNation.entrySet()) {
            DBNation nation = entry.getKey();
            double[] warchest = entry.getValue();
            double[] lacking = warchestLackingByNation.getOrDefault(nation, empty);
            double[] excess = warchestExcessByNation.getOrDefault(nation, empty);
            for (int i = 0; i < warchest.length; i++) {
                totalWarchest[i] += warchest[i];
                totalNet[i] += excess[i];
                totalNet[i] -= lacking[i];
            }
        }

        for (Map.Entry<DBNation, double[]> entry : warchestByNation.entrySet()) {
            DBNation nation = entry.getKey();

            double[] warchest = entry.getValue();
            double[] lacking = warchestLackingByNation.getOrDefault(nation, empty);
            double[] excess = warchestExcessByNation.getOrDefault(nation, empty);
            double[] revenue = revenueByNation.getOrDefault(nation, empty);

            double[] localPerCityWarchest = ResourceType.resourcesToArray(wcReqFunc.apply(nation));
            double requiredValue = ResourceType.convertedTotal(localPerCityWarchest) * nation.getInfra();
            double wcPct = (requiredValue - ResourceType.convertedTotal(lacking)) / requiredValue;
            double wcPctConverted = (requiredValue - ResourceType.convertedTotal(lacking) + ResourceType.convertedTotal(excess)) / requiredValue;

            double revenueIndividualValue = 0;
            double revenueAggregateValue = 0;
            double revenueValue = ResourceType.convertedTotal(revenue);
            for (int i = 0; i < revenue.length; i++) {
                double amt = revenue[i];
                if (amt == 0) continue;;
                if (lacking[i] > 0) {
                    revenueIndividualValue += ResourceType.convertedTotal(ResourceType.values[i], amt);
                }
                if (totalNet[i] < 0) {
                    revenueAggregateValue += ResourceType.convertedTotal(ResourceType.values[i], amt);
                }
            }

            header.set(0, MarkupUtil.sheetUrl(nation.getNation(), DNS.getUrl(nation.getNation_id(), false)));
            header.set(1, nation.getInfra() + "");
            header.set(2, nation.getLand() + "");
            header.set(3, MathMan.format(nation.getStrength()));

            header.set(4, MathMan.format(100 * wcPct));
            header.set(5, MathMan.format(100 * wcPctConverted));

            header.set(6, MathMan.format(100 * (revenueIndividualValue / revenueValue)));
            header.set(7, MathMan.format(100 * (revenueAggregateValue / revenueValue)));
            header.set(8, ResourceType.resourcesToString(lacking));
            header.set(9, ResourceType.resourcesToString(excess));
            header.set(10, ResourceType.resourcesToString(warchest));
            header.set(11, MathMan.format(ResourceType.convertedTotal(lacking)));
            header.set(12, MathMan.format(ResourceType.convertedTotal(excess)));

            sheet.addRow(header);
        }

        sheet.updateClearCurrentTab();
        sheet.updateWrite();

        StringBuilder response = new StringBuilder();
        response.append("Total Warchest: `" + ResourceType.resourcesToString(totalWarchest) + "` worth: ~$" + MathMan.format(ResourceType.convertedTotal(totalWarchest)) + "\n");
        response.append("Net Warchest Req (warchest- requirements): `" + ResourceType.resourcesToString(totalNet) + "` worth: ~$" + MathMan.format(ResourceType.convertedTotal(totalNet)));

        sheet.attach(io.create(), "warchest", response, false, 0).append(response.toString()).send();
        return null;
    }
//
//    public static final Map<UUID, Grant> AUTHORIZED_TRANSFERS = new HashMap<>();
//
//    @Command(desc = "Withdraw from the alliance bank (nation balance)", groups = {
//            "Amount Options",
//            "Optional: Bank Note",
//            "Specify Offshore/Bank",
//            "Optional: Nation Account",
//            "Optional: Tax Bracket Account (pick either or none)",
//    })
//    @RolePermission(value = {Roles.ECON_WITHDRAW_SELF, Roles.ECON}, any=true)
//    public String withdraw(@Me IMessageIO channel, @Me JSONObject command,
//                           @Me User author, @Me DBNation me, @Me GuildDB guildDb,
//
//                           @Arg(value = "Amount to send", group = 0, aliases = "transfer")
//                           @NationDepositLimit Map<ResourceType, Double> amount,
//                           @Arg(value = "Only send funds the receiver is lacking from the amount", aliases = "onlymissingfunds") @Switch("m") boolean only_send_missing,
//
//                           @Arg(value = "Transfer note", group = 1, aliases = "deposittype")
//                           @Default("#deposit") DepositType.DepositTypeInfo bank_note,
//                           @Arg(value = "Have the transfer ignored from nation holdings after a timeframe", group = 1) @Switch("e") @Timediff Long expire,
//                           @Arg(value = "Have the transfer decrease linearly from balances over a timeframe", group = 1) @Switch("d") @Timediff Long decay,
//                           @Arg(value = "Transfer valued at cash equivalent in nation balance", group = 1, aliases = "convertcash") @Switch("c") boolean deduct_as_cash,
//
//                           @Arg(value = "The in-game alliance bank to send from\n" +
//                                   "Defaults to the offshore set", group = 2, aliases = "usealliancebank") @Switch("a") DBAlliance ingame_bank,
//                           @Arg(value = "The account with the offshore to use\n" +
//                                   "The alliance must be registered to this guild\n" +
//                                   "Defaults to all the alliances of this guild", group = 2, aliases = "useoffshoreaccount") @Switch("o") DBAlliance offshore_account,
//
//                           @Arg(value = "The guild's nation account to use\n" +
//                                   "Defaults to your nation", group = 3, aliases = "depositsaccount") @Switch("n") DBNation nation_account,
//                           @Arg(value = "How to handle the transfer if the receiver is blockaded\n" +
//                                   "Defaults to never escrow", group = 3) @Switch("em") EscrowMode escrow_mode,
//
//                           @Arg(value = "The guild's tax account to deduct from\n" +
//                                   "Defaults to None", group = 4) @Switch("t") TaxBracket tax_account,
//                           @Arg(value = "OR deduct from the receiver's tax bracket account\n" +
//                                   "Defaults to false", group = 4, aliases = "existingtaxaccount") @Switch("ta") boolean use_receiver_tax_account,
//
//                           @Arg("Skip checking receiver activity, blockade, VM etc.")
//                           @Switch("b") boolean bypass_checks,
//
//                           @Switch("f") boolean force
//    ) throws IOException {
//        return transfer(channel, command, author, me, guildDb, me, amount, bank_note,
//                nation_account == null ? me : nation_account,
//                ingame_bank,
//                offshore_account,
//                tax_account,
//                use_receiver_tax_account,
//                only_send_missing,
//                expire,
//                decay,
//                null,
//                deduct_as_cash,
//                escrow_mode,
//                bypass_checks,
//                force);
//    }
//
    @Command(desc = "Bulk shift resources in a nations holdings to another note category")
    @RolePermission(Roles.ECON)
    public String shiftDeposits(@Me GuildDB db, @Me IMessageIO io, @Me DBNation me, DBNation nation, @Arg("The note to change FROM") DepositType from, @Arg("The new note to use") DepositType to, @Arg("Make the funds expire") @Default @Timestamp Long expireTime, @Arg("Make the funds decay") @Default @Timestamp Long decayTime) throws IOException {
        if (from == to) throw new IllegalArgumentException("From and to must be a different category.");
        if (expireTime != null && expireTime != 0 && to != DepositType.GRANT) {
            throw new IllegalArgumentException("The grant expiry time is only needed if converted to the grant category");
        }
        if (decayTime != null && decayTime != 0 && to != DepositType.GRANT) {
            throw new IllegalArgumentException("The grant decay time is only needed if converted to the grant category");
        }

        String note = "#" + to.name().toLowerCase(Locale.ROOT);

        if (to == DepositType.GRANT) {
            if ((expireTime == null || expireTime == 0) && (decayTime == null || decayTime == 0)) {
                throw new IllegalArgumentException("You must specify a grant expiry timediff if converting to the grant category. e.g. `60d`");
            } else {
                if (expireTime != null && expireTime != 0) {
                    note += " #expire=timestamp:" + (System.currentTimeMillis() + expireTime);
                }
                if (decayTime != null && decayTime != 0) {
                    note += " #decay=timestamp:" + (System.currentTimeMillis() + decayTime);
                }
            }
        }
        Map<DepositType, double[]> depoByType = nation.getDeposits(db, null, true, 0, 0);

        double[] toAdd = depoByType.get(from);
        if (toAdd == null || ResourceType.isZero(toAdd)) {
            return "Nothing to shift for " + nation.getNation();
        }
        long now = System.currentTimeMillis();
        if (from == DepositType.GRANT) {
            SimpleNationList nationList = new SimpleNationList(Collections.singleton(nation));
            resetDeposits(db, me, io, null, nationList, false, true, true, false, true);
        } else {
            String noteFrom = "#" + from.name().toLowerCase(Locale.ROOT);
            db.subBalance(now, nation, me.getNation_id(), noteFrom, toAdd);
        }
        db.addBalance(now, nation, me.getNation_id(), note, toAdd);
        return "Shifted " + ResourceType.resourcesToString(toAdd) + " from " + from + " to " + to + " for " + nation.getNation();
    }
//
    @Command(desc = "Resets a nations deposits to net zero (of the specific note categories)", groups = {
            "Reset Options"
    })
    @RolePermission(Roles.ECON)
    public String resetDeposits(@Me GuildDB db, @Me DBNation me, @Me IMessageIO io,
                                @Me JSONObject command,
                                NationList nations,
                                @Arg(value = "Do NOT reset grants", group = 0) @Switch("g") boolean ignoreGrants,
                                @Arg(value = "Do NOT reset loans", group = 0) @Switch("l") boolean ignoreLoans,
                                @Arg(value = "Do NOT reset taxes", group = 0) @Switch("t") boolean ignoreTaxes,
                                @Arg(value = "Do NOT reset deposits", group = 0) @Switch("d") boolean ignoreBankDeposits,
//                                @Arg(value = "Do NOT reset escrow", group = 0) @Switch("e") boolean ignoreEscrow,
                                @Switch("f") boolean force) throws IOException {
        if (nations.getNations().size() > 300) {
            throw new IllegalArgumentException("Due to performance issues, you can only reset up to 300 nations at a time");
        }

        long now = System.currentTimeMillis();
        StringBuilder response = new StringBuilder("Resetting deposits for `" + nations.getFilter() + "`\n");

        double[] totalDeposits = ResourceType.getBuffer();
        double[] totalTax = ResourceType.getBuffer();
        double[] totalLoan = ResourceType.getBuffer();
        double[] totalExpire = ResourceType.getBuffer();
        double[] totalEscrow = ResourceType.getBuffer();

        for (DBNation nation : nations.getNations()) {
            Map<DepositType, double[]> depoByType = nation.getDeposits(db, null, true, force ? 0L : -1L, 0);

            double[] deposits = depoByType.get(DepositType.DEPOSIT);
            if (deposits != null && !ignoreBankDeposits && !ResourceType.isZero(deposits)) {
                ResourceType.round(deposits);
                response.append("Subtracting `" + nation.getQualifiedId() + " " + ResourceType.resourcesToString(deposits) + " #deposit`\n");
                ResourceType.subtract(totalDeposits, deposits);
                if (force) db.subBalance(now, nation, me.getNation_id(), "#deposit", deposits);
            }

            double[] tax = depoByType.get(DepositType.TAX);
            if (tax != null && !ignoreTaxes && !ResourceType.isZero(tax)) {
                ResourceType.round(tax);
                response.append("Subtracting `" + nation.getQualifiedId() + " " + ResourceType.resourcesToString(tax) + " #tax`\n");
                ResourceType.subtract(totalTax, tax);
                if (force) db.subBalance(now, nation, me.getNation_id(), "#tax", tax);
            }

            double[] loan = depoByType.get(DepositType.LOAN);
            if (loan != null && !ignoreLoans && !ResourceType.isZero(loan)) {
                ResourceType.round(loan);
                response.append("Subtracting `" + nation.getQualifiedId() + " " + ResourceType.resourcesToString(loan) + " #loan`\n");
                ResourceType.subtract(totalLoan, loan);
                if (force) db.subBalance(now, nation, me.getNation_id(), "#loan", loan);
            }

            double[] grant = depoByType.get(DepositType.GRANT);
            if (grant != null && !ignoreGrants && !ResourceType.isZero(grant)) {
                List<Map.Entry<Integer, Transaction2>> transactions = nation.getTransactions(db, null, true, -1, 0);
                for (Map.Entry<Integer, Transaction2> entry : transactions) {
                    Transaction2 tx = entry.getValue();
                    if (tx.note == null || (!tx.note.contains("#expire") && !tx.note.contains("#decay")) || (tx.receiver_id != nation.getNation_id() && tx.sender_id != nation.getNation_id()))
                        continue;
                    if (tx.sender_id == tx.receiver_id) continue;
                    Map<String, String> notes = DNS.parseTransferHashNotes(tx.note);
                    String expire2 = notes.get("#expire");
                    String decay2 = notes.get("#decay");
                    long expireEpoch = Long.MAX_VALUE;
                    long decayEpoch = Long.MAX_VALUE;
                    if (expire2 != null) {
                        try {
                            expireEpoch = tx.tx_datetime + TimeUtil.timeToSec(expire2) * 1000L;
                        } catch (IllegalArgumentException e) {
                            continue;
                        }
                    }
                    if (decay2 != null) {
                        try {
                            decayEpoch = tx.tx_datetime + TimeUtil.timeToSec(decay2) * 1000L;
                        } catch (IllegalArgumentException e) {
                            continue;
                        }
                    }
                    expireEpoch = Math.min(expireEpoch, decayEpoch);
                    if (expireEpoch > now) {
                        String noteCopy = tx.note
                                .replaceAll("#expire=[a-zA-Z0-9:]+", "")
                                .replaceAll("#decay=[a-zA-Z0-9:]+", "");
                        if (expire2 != null) {
                            noteCopy += " #expire=" + "timestamp:" + expireEpoch;
                        }
                        if (decay2 != null) {
                            noteCopy += " #decay=" + "timestamp:" + decayEpoch;
                        }
                        noteCopy = noteCopy.trim();

                        tx.tx_datetime = System.currentTimeMillis();
                        int sign = entry.getKey();
                        if (sign == 1) {
                            response.append("Subtracting `" + nation.getQualifiedId() + " " + ResourceType.resourcesToString(tx.resources) + " " + noteCopy + "`\n");
                            ResourceType.subtract(totalExpire, tx.resources);
                            if (force) db.subBalance(now, nation, me.getNation_id(), noteCopy, tx.resources);
                        } else if (sign == -1) {
                            response.append("Adding `" + nation.getQualifiedId() + " " + ResourceType.resourcesToString(tx.resources) + " " + noteCopy + "`\n");
                            ResourceType.add(totalExpire, tx.resources);
                            if (force) db.addBalance(now, nation, me.getNation_id(), noteCopy, tx.resources);
                        } else {
                            System.out.println("Invalid sign for deposits reset " + sign);
                        }
                    }
                }
            }

//            if (!ignoreEscrow) {
//                try {
//                    Map.Entry<double[], Long> escrowedPair = db.getEscrowed(nation);
//                    if (escrowedPair != null && !ResourceType.isZero(escrowedPair.getKey())) {
//                        response.append("Subtracting escrow: `" + nation.getQualifiedId() + " " + ResourceType.resourcesToString(escrowedPair.getKey()) + "`\n");
//                        ResourceType.subtract(totalEscrow, escrowedPair.getKey());
//                        if (force) db.setEscrowed(nation, null, 0);
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    response.append("Failed to reset escrow balance: " + e.getMessage() + "\n");
//                }
//            }
        }

        if (!force) {
            String name = nations.getFilter();
            String title = "Reset deposits for " + (name.length() > 100 ? nations.getNations().size() + " nations" : name);
            StringBuilder body = new StringBuilder();
            if (!ResourceType.isZero(totalDeposits)) {
                body.append("Net Adding `" + name + " " + ResourceType.resourcesToString(totalDeposits) + " #deposit`\n");
            }
            if (!ResourceType.isZero(totalTax)) {
                body.append("Net Adding `" + name + " " + ResourceType.resourcesToString(totalTax) + " #tax`\n");
            }
            if (!ResourceType.isZero(totalLoan)) {
                body.append("Net Adding `" + name + " " + ResourceType.resourcesToString(totalLoan) + " #loan`\n");
            }
            if (!ResourceType.isZero(totalExpire)) {
                body.append("Net Adding `" + name + " " + ResourceType.resourcesToString(totalExpire) + " #expire`\n");
            }
            if (!ResourceType.isZero(totalEscrow)) {
                body.append("Deleting Escrow: `" + name + " " + ResourceType.resourcesToString(totalEscrow) + "`\n");
            }

            double[] total = ResourceType.getBuffer();
            total = ResourceType.add(total, totalDeposits);
            total = ResourceType.add(total, totalTax);
            total = ResourceType.add(total, totalLoan);
            total = ResourceType.add(total, totalExpire);
            total = ResourceType.subtract(total, totalEscrow);
            body.append("Total Net: `" + name + " " + ResourceType.resourcesToString(total) + "`\n");
            body.append("\n\nSee attached file for transaction details\n");

            io.create().confirmation(title, body.toString(), command)
                    .file("transaction.txt", response.toString()).send();
            return null;
        }

        return response.toString();
    }
//
//    @Command(desc = "Transfer from the alliance bank (alliance deposits)")
//    @RolePermission(value = {Roles.ECON, Roles.ECON_WITHDRAW_SELF}, any = true)
//    public static String transfer(@Me IMessageIO channel, @Me JSONObject command,
//                                  @Me User author, @Me DBNation me, @Me GuildDB guildDb, NationOrAlliance receiver, @AllianceDepositLimit Map<ResourceType, Double> transfer, DepositType.DepositTypeInfo depositType,
//                                  @Arg("The nation account to deduct from") @Switch("n") DBNation nationAccount,
//                                  @Arg("The alliance bank to send from\nDefaults to the offshore") @Switch("a") DBAlliance senderAlliance,
//                                  @Arg("The alliance account to deduct from\nAlliance must be registered to this guild\nDefaults to all the alliances of this guild") @Switch("o") DBAlliance allianceAccount,
//                                  @Arg("The tax account to deduct from") @Switch("t") TaxBracket taxAccount,
//                                  @Arg("Deduct from the receiver's tax bracket account") @Switch("ta") boolean existingTaxAccount,
//                                  @Arg("Only send funds the receiver is lacking from the amount") @Switch("m") boolean onlyMissingFunds,
//                                  @Arg("Have the transfer ignored from nation holdings after a timeframe") @Switch("e") @Timediff Long expire,
//                                  @Arg("Have the transfer decrease linearly to zero for balances over a timeframe") @Switch("d") @Timediff Long decay,
//                                  @Switch("g") UUID token,
//                                  @Arg("Transfer valued at cash equivalent in nation holdings") @Switch("c") boolean convertCash,
//                                  @Arg("The mode for escrowing funds (e.g. if the receiver is blockaded)\nDefaults to never") @Switch("em") EscrowMode escrow_mode,
//                                  @Switch("b") boolean bypassChecks,
//                                  @Switch("f") boolean force) throws IOException {
//        if (existingTaxAccount) {
//            if (taxAccount != null) throw new IllegalArgumentException("You can't specify both `tax_id` and `existingTaxAccount`");
//            if (!receiver.isNation()) throw new IllegalArgumentException("You can only specify `existingTaxAccount` for a nation");
//            taxAccount = receiver.asNation().getTaxBracket();
//        }
//        if (receiver.isAlliance() && onlyMissingFunds) {
//            return "Option `-o` only applicable for nations";
//        }
//
//        if (receiver.isAlliance() && !receiver.asAlliance().exists()) {
//            throw new IllegalArgumentException("Alliance: " + receiver.getUrl() + " has no receivable nations");
//        }
//        List<String> forceErrors = new ArrayList<>();
//        if (receiver.isNation()) {
//            DBNation nation = receiver.asNation();
//            if (nation.isVacation()) forceErrors.add("Receiver is in Vacation Mode");
//            if (nation.isGray()) forceErrors.add("Receiver is Gray");
//            if (nation.getNumWars() > 0 && receiver.asNation().isBlockaded() && (escrow_mode == null || escrow_mode == EscrowMode.NEVER)) forceErrors.add("Receiver is blockaded");
//            if (nation.active_m() > 10000) forceErrors.add(("!! **WARN**: Receiver is " + TimeUtil.secToTime(TimeUnit.MINUTES, nation.active_m())) + " inactive");
//        } else if (receiver.isAlliance()) {
//            DBAlliance alliance = receiver.asAlliance();
//            if (alliance.getNations(f -> f.getPositionEnum().id > Rank.HEIR.id && f.isVacation() == false && f.active_m() < 10000).size() == 0) {
//                forceErrors.add("Alliance has no active leaders/heirs (are they in vacation mode?)");
//            }
//        }
//        if (!forceErrors.isEmpty() && !bypassChecks) {
//            String title = forceErrors.size() + " **ERRORS**!";
//            String body = StringMan.join(forceErrors, "\n");
//            channel.create().confirmation(title, body, command, "bypassChecks").send();
//            return null;
//        }
//
//        OffshoreInstance offshore;
//        if (senderAlliance != null) {
//            if (!guildDb.isAllianceId(senderAlliance.getAlliance_id())) {
//                return "This guild is not registered to the alliance `" + senderAlliance.getName() + "`";
//            }
//            offshore = senderAlliance.getBank();
//        } else {
//            offshore = guildDb.getOffshore();
//        }
//        if (offshore == null) {
//            return "No offshore is setup. See " + CM.offshore.add.cmd.toSlashMention();
//        }
//        if (nationAccount == null && Roles.ECON.getAllianceList(author, guildDb).isEmpty()) {
//            nationAccount = me;
//        }
//
//        Set<Grant.Requirement> failedRequirements = new HashSet<>();
//        boolean isGrant = false;
//        if (token != null) {
//            Grant authorized = AUTHORIZED_TRANSFERS.get(token);
//            if (authorized == null) return "Invalid token (try again)";
//            if (!receiver.isNation()) return "Receiver is not nation";
//
//            for (Grant.Requirement requirement : authorized.getRequirements()) {
//                if (!requirement.apply(receiver.asNation())) {
//                    failedRequirements.add(requirement);
//                    if (requirement.canOverride()) continue;
//                    else {
//                        return "Failed requirement: " + requirement.getMessage();
//                    }
//                }
//            }
//
//            isGrant = true;
//        }
//
//        // transfer limit
//        long userId = author.getIdLong();
//        if (ResourceType.convertedTotal(transfer) > 5000000000L
//                && userId != Locutus.loader().getAdminUserId()
//                && !Settings.INSTANCE.LEGACY_SETTINGS.WHITELISTED_BANK_USERS.contains(userId)
//                && !isGrant && offshore.getAllianceId() == Settings.INSTANCE.ALLIANCE_ID()
//        ) {
//            return "Transfer too large. Please specify a smaller amount";
//        }
//
//        Map<Long, AccessType> allowedIds = guildDb.getAllowedBankAccountsOrThrow(author, receiver, channel.getIdLong());
//
//        // Filter allowed ids by access type
//
//        if (onlyMissingFunds) {
//            int aaId = receiver.getAlliance_id();
//            if (aaId == 0) {
//                return "Receiver is not in an alliance (cannot determine missing funds)";
//            }
//            AccessType accessType = allowedIds.get((long) aaId);
//            if (accessType == null) {
//                return "You do not have access to the alliance stockpile information for " + DBAlliance.getOrCreate(aaId).getQualifiedId();
//            }
//            if (me.getId() != receiver.getId()) {
//                if (accessType != AccessType.ECON) {
//                    return "You can only access stockpile information for yourself";
//                }
//            }
//            if (!guildDb.isAllianceId(receiver.getAlliance_id())) {
//                return "This guild is not registered to the alliance `" + receiver.getAlliance_id() + "`. See: " + CM.settings_default.registerAlliance.cmd.toSlashMention();
//            }
//            Map<ResourceType, Double> existing = receiver.getStockpile();
//            for (Map.Entry<ResourceType, Double> entry : transfer.entrySet()) {
//                double toSend = Math.max(0, entry.getValue() - existing.getOrDefault(entry.getKey(), 0d));
//                entry.setValue(toSend);
//            }
//        }
//
//        TransferResult result;
//        try {
//            result = offshore.transferFromNationAccountWithRoleChecks(
//                    author,
//                    nationAccount,
//                    allianceAccount,
//                    taxAccount,
//                    guildDb,
//                    channel.getIdLong(),
//                    receiver,
//                    ResourceType.resourcesToArray(transfer),
//                    depositType,
//                    expire,
//                    decay,
//                    null,
//                    convertCash,
//                    escrow_mode,
//                    !force,
//                    bypassChecks
//            );
//        } catch (IllegalArgumentException | IOException e) {
////            result = new AbstractMap.SimpleEntry<>(OffshoreInstance.TransferStatus.OTHER, e.getMessage());
//            result = new TransferResult(OffshoreInstance.TransferStatus.OTHER, receiver, transfer, depositType.toString()).addMessage(e.getMessage());
//        }
//        if (result.getStatus() == OffshoreInstance.TransferStatus.CONFIRMATION) {
//            String worth = "$" + MathMan.format(ResourceType.convertedTotal(transfer));
//            String title = "Send (worth: " + worth + ") to " + receiver.getTypePrefix() + ":" + receiver.getName();
//            if (receiver.isNation()) {
//                title += " | " + receiver.asNation().getAlliance();
//            }
//            channel.create().confirmation(title, result.getMessageJoined(false), command, "force", "Send").cancelButton().send();
//            return null;
//        }
//
//        channel.create().embed(result.toTitleString(), result.toEmbedString()).send();
//        return null;
//    }
//
    @Command(desc = "Sheet of projects each nation has")
    @RolePermission(value = {Roles.ECON, Roles.INTERNAL_AFFAIRS}, any=true)
    public String ProjectSheet(@Me IMessageIO io, @Me GuildDB db, NationList nations, @Switch("s") SpreadSheet sheet, @Switch("t") @Timestamp Long snapshotTime) throws GeneralSecurityException, IOException {
        Set<DBNation> nationSet = DNS.getNationsSnapshot(nations.getNations(), nations.getFilter(), snapshotTime, db.getGuild(), false);
        if (sheet == null) {
            sheet = SpreadSheet.create(db, SheetKey.PROJECT_SHEET);
        }
        List<Object> header = new ArrayList<>(Arrays.asList(
                "nation",
                "alliance",
                "infra",
                "land",
                "score"
        ));

        for (Project value : Project.values) {
            header.add(value.name());
        }

        sheet.setHeader(header);

        for (DBNation nation : nationSet) {

            header.set(0, MarkupUtil.sheetUrl(nation.getNation(), DNS.getUrl(nation.getNation_id(), false)));
            header.set(1, MarkupUtil.sheetUrl(nation.getAllianceName(), DNS.getUrl(nation.getAlliance_id(), true)));
            header.set(2, nation.getInfra());
            header.set(3, nation.getLand());
            header.set(4, nation.getScore());

            for (int i = 0; i < Project.values.length; i++) {
                Project project = Project.values[i];
                header.set(5 + i, nation.getProject(db, project) + "");
            }

            sheet.addRow(header);
        }

        sheet.updateClearCurrentTab();
        sheet.updateWrite();

        sheet.attach(io.create(), "projects").send();
        return null;
    }
//
//    @Command(desc = "Create a google sheet of escrowed resources amounts for a set of nations", groups = {
//            "Optional 1: Specific Nations",
//            "Optional 2: Include past members"
//    })
//    @RolePermission(Roles.ECON)
//    public String escrowSheetCmd(@Me IMessageIO io, @Me GuildDB db, @Me Guild guild,
//                                 @Arg(value = "Specify the nations to include in the sheet\n" +
//                                         "Defaults to current members", group = 0)
//                                 @Default Set<DBNation> nations,
//                                 @Arg(value = "Include all nations that have deposited in the past", group = 1)
//                                 @Switch("p") Set<Integer> includePastDepositors,
//
//                                 @Switch("s") SpreadSheet sheet) throws GeneralSecurityException, IOException {
//        if (nations == null) {
//            Set<Integer> aaIds = db.getAllianceIds();
//            if (!aaIds.isEmpty()) {
//                nations = new LinkedHashSet<>(Locutus.imp().getNationDB().getNations(aaIds));
//                if (includePastDepositors == null || includePastDepositors.isEmpty()) nations.removeIf(n -> n.getPosition() <= 1);
//
//                if (includePastDepositors != null && !includePastDepositors.isEmpty()) {
//                    Set<Integer> ids = Locutus.imp().getBankDB().getReceiverNationIdFromAllianceReceivers(includePastDepositors);
//                    for (int id : ids) {
//                        DBNation nation = Locutus.imp().getNationDB().getNation(id);
//                        if (nation != null) nations.add(nation);
//                    }
//                }
//            } else {
//                if (includePastDepositors != null && !includePastDepositors.isEmpty()) {
//                    throw new IllegalArgumentException("`usePastDepositors` is only for alliances");
//                }
//                Role role = Roles.MEMBER.toRole(guild);
//                if (role == null) throw new IllegalArgumentException("No " + GuildKey.ALLIANCE_ID.getCommandMention() + " set, or " +
//                        "" + CM.role.setAlias.cmd.locutusRole(Roles.MEMBER.name()).discordRole("") + " set");
//                nations = new LinkedHashSet<>();
//                for (Member member : guild.getMembersWithRoles(role)) {
//                    DBNation nation = DiscordUtil.getNation(member.getUser());
//                    if (nation != null) {
//                        nations.add(nation);
//                    }
//                }
//                if (nations.isEmpty()) return "No members found";
//
//            }
//        } else if (includePastDepositors != null && !includePastDepositors.isEmpty()) {
//            throw new IllegalArgumentException("`usePastDepositors` cannot be set when nations are provided");
//        }
//        Map.Entry<SpreadSheet, double[]> sheetPair = escrowSheet(db, nations, sheet);
//        sheet = sheetPair.getKey();
//        double[] totalEscrowed = sheetPair.getValue();
//
//        sheet.updateClearCurrentTab();
//        sheet.updateWrite();
//        // appent resource string and worth
//        sheet.attach(io.create(), "escrow").append("Total Escrowed: `" + ResourceType.resourcesToString(totalEscrowed) + "` | worth: ~$" + MathMan.format(ResourceType.convertedTotal(totalEscrowed))).send();
//        return null;
//    }
//
//    private static Map.Entry<SpreadSheet, double[]> escrowSheet(GuildDB db, Collection<DBNation> nations, SpreadSheet sheetOrNull) throws GeneralSecurityException, IOException {
//        double[] totalEscrowed = ResourceType.getBuffer();
//        List<Object> escrowHeader = new ArrayList<>(Arrays.asList(
//                "nation",
//                "cities",
//                "age",
//                "expires",
//                "value"
//
//        ));
//        for (ResourceType type : ResourceType.values()) {
//            if (type == ResourceType.CREDITS) continue;
//            escrowHeader.add(type.name());
//        }
//        SpreadSheet escrowSheet = sheetOrNull != null ? sheetOrNull : SpreadSheet.create(db, SheetKey.ESCROW_SHEET);
//        escrowSheet.setHeader(escrowHeader);
//
//        for (DBNation nation : nations) {
//            Map.Entry<double[], Long> escrowedPair = db.getEscrowed(nation);
//            if (escrowedPair == null || ResourceType.isZero(escrowedPair.getKey())) continue;
//            ResourceType.add(totalEscrowed, escrowedPair.getKey());
//
//            escrowHeader.set(0, MarkupUtil.sheetUrl(nation.getNation(), DNS.getUrl(nation.getNation_id(), false)));
//            escrowHeader.set(1, nation.getCities());
//            escrowHeader.set(2, nation.getAgeDays());
//
//            long expireEpoch = escrowedPair.getValue();
//            String expires = expireEpoch == 0 ? "never" : TimeUtil.YYYY_MM_DD_HH_MM_SS.format(expireEpoch);
//            escrowHeader.set(3, expires);
//
//            double value = ResourceType.convertedTotal(escrowedPair.getKey());
//            escrowHeader.set(4, MathMan.format(value));
//            int i = 0;
//            for (ResourceType type : ResourceType.values) {
//                if (type == ResourceType.CREDITS) continue;
//                escrowHeader.set(5 + (i++), MathMan.format(escrowedPair.getKey()[type.ordinal()]));
//            }
//
//            escrowSheet.addRow(escrowHeader);
//        }
//
//        return Map.entry(escrowSheet, totalEscrowed);
//    }
//
    @Command(aliases = {"depositSheet", "depositsSheet"}, desc =
            "Get a sheet with member nations and their deposits\n" +
                    "Each nation's safekeep should match the total balance given by deposits command" +
                    "Add `-b` to \n" +
                    "Add `-o` to not include any manual deposit offsets\n" +
                    "Add `-d` to not include deposits\n" +
                    "Add `-t` to not include taxes\n" +
                    "Add `-l` to not include loans\n" +
                    "Add `-g` to not include grants\n" +
                    "Add `-p` to include past depositors\n" +
                    "Add `-f` to force an update"

    )
    @RolePermission(Roles.ECON)
    public static String depositSheet(@Me IMessageIO channel, @Me Guild guild, @Me GuildDB db,
                               @Default Set<DBNation> nations,
                               @Arg("The alliances to track transfers from") @Default Set<DBAlliance> offshores,
                               @Arg("Do NOT include any manual deposit offsets") @Switch("o") boolean ignoreOffsets,
                                      @Arg("Include ALL #expire and #decay transfers") @Switch("ex") boolean includeExpired,
                                      @Arg("Include #ignore transfers") @Switch("i") boolean includeIgnored,
                               @Arg("Do NOT include taxes") @Switch("t") boolean noTaxes,
                               @Arg("Do NOT include loans") @Switch("l") boolean noLoans,
                               @Arg("Do NOT include grants") @Switch("g") boolean noGrants,
                               @Arg("Do NOT include deposits") @Switch("d") boolean noDeposits,
                               @Arg("Include past depositors") @Switch("p") Set<Integer> includePastDepositors,
//                               @Arg("Do NOT include escrow sheet") @Switch("e") boolean noEscrowSheet,
                               @Arg("Only show the flow for this note\n" +
                                       "i.e. To only see funds marked as #TRADE\n" +
                                       "This is for transfer flow breakdown internal, withdrawal, and deposit")
                                  @Switch("n") DepositType useFlowNote,
                               @Switch("f") boolean force

    ) throws GeneralSecurityException, IOException {
        CompletableFuture<IMessageBuilder> msgFuture = channel.send("Please wait...");

        SpreadSheet sheet = SpreadSheet.create(db, SheetKey.DEPOSIT_SHEET);

        List<Object> header = new ArrayList<>(Arrays.asList(
                "nation",
                "infra",
                "age",
                "deposit",
                "tax",
                "loan",
                "grant",
                "total",
                "last_deposit_day",
                "last_self_withdraw_day",
                "flow_internal",
                "flow_withdrawal",
                "flow_deposit"
        ));

        for (ResourceType type : ResourceType.values()) {
            header.add(type.name());
        }

        sheet.setHeader(header);

        boolean useOffset = !ignoreOffsets;

        if (nations == null) {
            Set<Integer> aaIds = db.getAllianceIds();
            if (!aaIds.isEmpty()) {
                nations = new LinkedHashSet<>(Locutus.imp().getNationDB().getNations(aaIds));
                if (includePastDepositors == null || includePastDepositors.isEmpty()) nations.removeIf(n -> n.getPosition() <= 1);

                if (includePastDepositors != null && !includePastDepositors.isEmpty()) {
                    Set<Integer> ids = Locutus.imp().getBankDB().getReceiverNationIdFromAllianceReceivers(includePastDepositors);
                    for (int id : ids) {
                        DBNation nation = Locutus.imp().getNationDB().getNation(id);
                        if (nation != null) nations.add(nation);
                    }
                }
            } else {
                if (includePastDepositors != null && !includePastDepositors.isEmpty()) {
                    throw new IllegalArgumentException("usePastDepositors is only implemented for alliances (ping borg)");
                }
                Role role = Roles.MEMBER.toRole(guild);
                if (role == null) throw new IllegalArgumentException("No " + GuildKey.ALLIANCE_ID.getCommandMention() + " set, or " +
                        "" + CM.role.setAlias.cmd.locutusRole(Roles.MEMBER.name()).discordRole("") + " set");
                nations = new LinkedHashSet<>();
                for (Member member : guild.getMembersWithRoles(role)) {
                    DBNation nation = DiscordUtil.getNation(member.getUser());
                    if (nation != null) {
                        nations.add(nation);
                    }
                }
                if (nations.isEmpty()) return "No members found";

            }
        } else if (includePastDepositors != null && !includePastDepositors.isEmpty()) {
            throw new IllegalArgumentException("usePastDepositors cannot be set when nations are provided");
        }
        Set<Long> tracked = null;
        if (offshores != null) {
            tracked = new LinkedHashSet<>();
            for (DBAlliance aa : offshores) tracked.add((long) aa.getAlliance_id());
            tracked = DNS.expandCoalition(tracked);
        }

        double[] aaTotalPositive = ResourceType.getBuffer();
        double[] aaTotalNet = ResourceType.getBuffer();

        boolean updateBulk = Settings.INSTANCE.TASKS.ALL_BANK_REQUESTS_SECONDS > 0;
        if (updateBulk) {
            for (DBAlliance alliance : db.getAllianceList().getAlliances()) {
                Locutus.imp().runEventsAsync(events -> Locutus.imp().getBankDB().updateBankTransfers(alliance));
            }
        }

        String noteFlowStr = useFlowNote == null ? null : "#" + useFlowNote.name().toLowerCase(Locale.ROOT);

        long last = System.currentTimeMillis();
        for (DBNation nation : nations) {
            if (System.currentTimeMillis() - last > 5000) {
                IMessageBuilder tmp = msgFuture.getNow(null);
                if (tmp != null) msgFuture = tmp.clear().append("calculating for: " + nation.getNation()).send();
                last = System.currentTimeMillis();
            }

            List<Map.Entry<Integer, Transaction2>> transactions = nation.getTransactions(db, tracked, useOffset, (updateBulk && !force) ? -1 : 0L, 0L);
            List<Map.Entry<Integer, Transaction2>> flowTransfers = useFlowNote == null ? transactions : transactions.stream().filter(f -> DNS.parseTransferHashNotes(f.getValue().note).containsKey(noteFlowStr)).collect(Collectors.toList());
            double[] internal = FlowType.INTERNAL.getTotal(flowTransfers, nation.getId());
            double[] withdrawal = FlowType.WITHDRAWAL.getTotal(flowTransfers, nation.getId());
            double[] deposit = FlowType.DEPOSIT.getTotal(flowTransfers, nation.getId());

            Map<DepositType, double[]> deposits = DNS.sumNationTransactions(db, tracked, transactions, includeExpired, includeIgnored, f -> true);
            double[] buffer = ResourceType.getBuffer();

            header.set(0, MarkupUtil.sheetUrl(nation.getNation(), nation.getUrl()));
            header.set(1, nation.getInfra());
            header.set(2, nation.getAgeDays());
            header.set(3, String.format("%.2f", ResourceType.convertedTotal(deposits.getOrDefault(DepositType.DEPOSIT, buffer))));
            header.set(4, String.format("%.2f", ResourceType.convertedTotal(deposits.getOrDefault(DepositType.TAX, buffer))));
            header.set(5, String.format("%.2f", ResourceType.convertedTotal(deposits.getOrDefault(DepositType.LOAN, buffer))));
            header.set(6, String.format("%.2f", ResourceType.convertedTotal(deposits.getOrDefault(DepositType.GRANT, buffer))));
            double[] total = ResourceType.getBuffer();
            for (Map.Entry<DepositType, double[]> entry : deposits.entrySet()) {
                switch (entry.getKey()) {
                    case GRANT:
                        if (noGrants) continue;
                        break;
                    case LOAN:
                        if (noLoans) continue;
                        break;
                    case TAX:
                        if (noTaxes) continue;
                        break;
                    case DEPOSIT:
                        if (noDeposits) continue;
                        break;
                }
                double[] value = entry.getValue();
                total = ArrayUtil.apply(ArrayUtil.DOUBLE_ADD, total, value);
            }
            header.set(7, String.format("%.2f", ResourceType.convertedTotal(total)));
            long lastDeposit = 0;
            long lastSelfWithdrawal = 0;
            for (Map.Entry<Integer, Transaction2> entry : transactions) {
                Transaction2 transaction = entry.getValue();
                if (transaction.sender_id == nation.getNation_id()) {
                    lastDeposit = Math.max(transaction.tx_datetime, lastDeposit);
                }
                if (transaction.isSelfWithdrawal(nation)) {
                    lastSelfWithdrawal = Math.max(transaction.tx_datetime, lastSelfWithdrawal);
                }
            }
            if (lastDeposit == 0) {
                header.set(8, "NEVER");
            } else {
                long days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastDeposit);
                header.set(8, days);
            }
            if (lastSelfWithdrawal == 0) {
                header.set(9, "NEVER");
            } else {
                long days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastSelfWithdrawal);
                header.set(9, days);
            }
            header.set(10, ResourceType.resourcesToString(internal));
            header.set(11, ResourceType.resourcesToString(withdrawal));
            header.set(12, ResourceType.resourcesToString(deposit));

            int i = 13;
            for (ResourceType type : ResourceType.values) {
                header.set((i++), MathMan.format(total[type.ordinal()]));
            }
            double[] normalized = DNS.normalize(total);
            if (ResourceType.convertedTotal(normalized) > 0) {
                aaTotalPositive = ArrayUtil.apply(ArrayUtil.DOUBLE_ADD, aaTotalPositive, normalized);
            }
            aaTotalNet = ArrayUtil.apply(ArrayUtil.DOUBLE_ADD, aaTotalNet, total);
            sheet.addRow(header);
        }

        StringBuilder footer = new StringBuilder();

        sheet.updateClearCurrentTab();
        sheet.updateWrite();

        IMessageBuilder msg = channel.create();
        sheet.attach(msg, "deposits");

//        if (!noEscrowSheet) {
//            Map.Entry<SpreadSheet, double[]> pair = escrowSheet(db, nations, null);
//            if (!ResourceType.isZero(pair.getValue())) {
//                SpreadSheet escrowSheet = pair.getKey();
//                // attach sheet
//                escrowSheet.updateClearCurrentTab();
//                escrowSheet.updateWrite();
//                escrowSheet.attach(msg, "escrow");
//
//                double[] escrowTotal = pair.getValue();
//                aaTotalPositive = ArrayUtil.apply(ArrayUtil.DOUBLE_ADD, aaTotalPositive, escrowTotal);
//                aaTotalNet = ArrayUtil.apply(ArrayUtil.DOUBLE_ADD, aaTotalNet, escrowTotal);
//            } else {
//                noEscrowSheet = true;
//            }
//        }

        footer.append(ResourceType.resourcesToFancyString(aaTotalPositive, "Nation Deposits (" + nations.size() + " nations)"));

        footer.append("\n\nTo adjust member balances: " + CM.deposits.add.cmd.toSlashMention());
//        footer.append("\nTo reset member balances: " + CM.deposits.reset.cmd.toSlashMention()); // TODO CM REF

        String type = "";
//        OffshoreInstance offshore = db.getOffshore();
        double[] aaDeposits;
//        if (offshore != null && offshore.getGuildDB() != db) {
//            type = "offshored";
//            aaDeposits = offshore.getDeposits(db);
//        } else
        if (db.isValidAlliance()){
            type = "bank stockpile";
            long updateAfter = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1);
            aaDeposits = ResourceType.resourcesToArray(db.getAllianceList().getStockpile(updateAfter));
        } else aaDeposits = null;
        if (aaDeposits != null) {
            if (ResourceType.convertedTotal(aaDeposits) > 0) {
                for (int i = 0; i < aaDeposits.length; i++) {
                    aaTotalNet[i] = aaDeposits[i] - aaTotalNet[i];
                    aaTotalPositive[i] = aaDeposits[i] - aaTotalPositive[i];
                }
                String natDepTypes = "balances";//noEscrowSheet ? "balances" : "balances (with escrow)";
                footer.append("\n**Total " + type + "- nation " + natDepTypes + " (without negative balances)**:  Worth: $" + MathMan.format(ResourceType.convertedTotal(aaTotalPositive)) + "\n`" + ResourceType.resourcesToString(aaTotalPositive) + "`");
                footer.append("\n**Total " + type + "- nation " + natDepTypes + "**:  Worth: $" + MathMan.format(ResourceType.convertedTotal(aaTotalNet)) + "\n`" + ResourceType.resourcesToString(aaTotalNet) + "`");
            } else {
                footer.append("\n**No funds are currently " + type + "**");
            }
        }

        msg.embed("Nation Deposits (With Alliance)", footer.toString())
                .send();
        return null;
    }
//
//    @Command(desc = "Set the withdrawal limit (per interval) of a banker", aliases = {"setTransferLimit", "setWithdrawLimit", "setWithdrawalLimit", "setBankLimit"})
//    @RolePermission(Roles.ADMIN)
//    public String setTransferLimit(@Me GuildDB db, Set<DBNation> nations, double limit) {
//        db.getOrThrow(GuildKey.BANKER_WITHDRAW_LIMIT); // requires to be set
//
//        StringBuilder response = new StringBuilder();
//        for (DBNation nation : nations) {
//            db.getHandler().setWithdrawLimit(nation.getNation_id(), limit);
//            response.append("Set withdraw limit of: " + nation.getUrl() + " to $" + MathMan.format(limit) + "\n");
//        }
//        response.append("Done!");
//        return response.toString();
//    }
//
//    @Command(desc = "Set nation's internal taxrate\n" +
//            "See also: `{prefix}nation set taxbracket` and `{prefix}settings key:TAX_BASE`")
//    @RolePermission(value = Roles.ECON)
//    @IsAlliance
//    public String setInternalTaxRate(@Me GuildDB db, Set<DBNation> nations, TaxRate taxRate) {
//        if (taxRate.money < -1 || taxRate.money > 100 || taxRate.resources < -1 || taxRate.resources > 100) throw new IllegalArgumentException("Invalid taxrate: " + taxRate);
//
//        AllianceList aa = db.getAllianceList();
//        if (aa == null || aa.isEmpty()) throw new IllegalArgumentException("This guild is not registered to an alliance");
//
//        StringBuilder response = new StringBuilder();
//
//        for (DBNation nation : nations) {
//            if (!aa.contains(nation.getAlliance_id())) throw new IllegalArgumentException("Nation: " + nation.getUrl() + " is not in alliances: " + StringMan.getString(aa.getIds()));
//            if (nation.getPosition() <= 1) throw new IllegalArgumentException("Nation: " + nation.getUrl() + " is not a member");
//            db.setMeta(nation.getNation_id(), NationMeta.TAX_RATE, new byte[]{(byte) taxRate.money, (byte) taxRate.resources});
//            response.append("Set " + nation.getUrl() + " internal taxrate to " + taxRate + "\n");
//        }
//
//        response.append("Done!");
//        return response.toString();
//    }
//
//    public void largest(Map<ResourceType, Double> total, Map<ResourceType, Double> local) {
//        for (Map.Entry<ResourceType, Double> entry : local.entrySet()) {
//            ResourceType type = entry.getKey();
//            total.put(type, Math.max(entry.getValue(), total.getOrDefault(type, 0d)));
//            if (total.get(type) <= 0) {
//                total.remove(type);
//            }
//        }
//    }
//
//    @Command(desc = "Get a sheet of in-game transfers for nations")
//    @RolePermission(value = Roles.ECON)
//    public String getIngameNationTransfers(@Me IMessageIO channel, @Me GuildDB db, @AllowDeleted Set<NationOrAlliance> senders, @AllowDeleted Set<NationOrAlliance> receivers,
//                                           @Arg("Only transfers after timeframe") @Default("%epoch%") @Timestamp long start_time,
//                                           @Switch("e") @Timestamp Long end_time,
//                                           @Switch("s") SpreadSheet sheet) throws IOException, GeneralSecurityException {
//        if (end_time == null) end_time = Long.MAX_VALUE;
//        if (sheet == null) sheet = SpreadSheet.create(db, SheetKey.BANK_TRANSACTION_SHEET);
//        Set<Long> senderIds = senders.stream().map(NationOrAllianceOrGuild::getIdLong).collect(Collectors.toSet());
//        Set<Long> receiverIds = receivers.stream().map(NationOrAllianceOrGuild::getIdLong).collect(Collectors.toSet());
//        List<Transaction2> transactions = Locutus.imp().getBankDB().getTransactionsByBySenderOrReceiver(senderIds, receiverIds, start_time, end_time);
//        transactions.removeIf(transaction2 -> {
//            NationOrAllianceOrGuild sender = transaction2.getSenderObj();
//            NationOrAllianceOrGuild receiver = transaction2.getReceiverObj();
//            return !senders.contains(sender) || !receivers.contains(receiver);
//        });
//        sheet.addTransactionsList(channel, transactions, true);
//        return null;
//    }
//
//    @Command(desc = "Get a sheet of ingame transfers for nations, filtered by the sender")
//    @RolePermission(value = Roles.ECON)
//    public String IngameNationTransfersBySender(@Me IMessageIO channel, @Me GuildDB db, Set<NationOrAlliance> senders, @Default("%epoch%") @Timestamp long timeframe, @Switch("s") SpreadSheet sheet) throws IOException, GeneralSecurityException {
//        if (sheet == null) sheet = SpreadSheet.create(db, SheetKey.BANK_TRANSACTION_SHEET);
//        Set<Long> senderIds = senders.stream().map(NationOrAllianceOrGuild::getIdLong).collect(Collectors.toSet());
//        List<Transaction2> transactions = Locutus.imp().getBankDB().getTransactionsByBySender(senderIds, timeframe);
//        transactions.removeIf(tx -> !senders.contains(tx.getSenderObj()));
//        sheet.addTransactionsList(channel, transactions, true);
//        return null;
//    }
//
//    @Command(desc = "Get a sheet of ingame transfers for nations, filtered by the receiver", groups = {
//            "Optional: Specify timeframe"
//    })
//    @RolePermission(value = Roles.ECON)
//    public String IngameNationTransfersByReceiver(@Me IMessageIO channel, @Me GuildDB db,
//                                                  Set<NationOrAlliance> receivers, @Arg(value = "Only list transfers after this time", group = 0)
//                                                      @Timestamp @Default Long startTime,
//                                                  @Arg(value = "Only list transfers before this time", group = 0)
//                                                      @Timestamp @Default Long endTime, @Switch("s") SpreadSheet sheet) throws IOException, GeneralSecurityException {
//        if (sheet == null) sheet = SpreadSheet.create(db, SheetKey.BANK_TRANSACTION_SHEET);
//        Set<Long> receiverIds = receivers.stream().map(NationOrAllianceOrGuild::getIdLong).collect(Collectors.toSet());
//        if (startTime == null) startTime = 0L;
//        if (endTime == null) endTime = Long.MAX_VALUE;
//        List<Transaction2> transactions = Locutus.imp().getBankDB().getTransactionsByByReceiver(receiverIds, startTime, endTime);
//        transactions.removeIf(tx -> !receivers.contains(tx.getReceiverObj()));
//        sheet.addTransactionsList(channel, transactions, true);
//        return null;
//    }
//
//    @Command(desc = "Adjust nation's holdings by converting negative resource values of a specific note to a different resource or money",
//    groups = {
//            "Optional: Resources to convert",
//            "Optional (max 1): Specify the deposit notes to convert",
//            "Conversion Options",
//    },
//    groupDescs = {
//            "If no resources are specified, all negative resources are converted to money",
//            "Defaults to all types, except grants"
//    })
//    @RolePermission(value = Roles.ECON)
//    public String convertNegativeDeposits(@Me IMessageIO channel, @Me GuildDB db,
//                                          Set<DBNation> nations,
//
//                                          @Arg(value = "The resources to convert", group = 0)
//                                          @Default("manu,raws,food") Set<ResourceType> negativeResources,
//                                          @Arg(value = "What resource to convert to\n" +
//                                                  "Conversion uses weekly market average prices", group = 0)
//                                          @Default("money") ResourceType convertTo,
//
//
//                                          @Arg(value = "If grants are also converted", group = 1)
//                                          @Switch("g") boolean includeGrants,
//                                          @Arg(value = "Convert transfers of this note category", group = 1)
//                                              @Switch("t") DepositType.DepositTypeInfo depositType,
//
//                                          @Arg(value = "What factor to multiple the converted resources by\n" +
//                                                  "e.g. Use a value below 1.0 to incur a fee", group = 2) @Switch("f")
//                                              Double conversionFactor,
//                                          @Arg(value = "The transfer note to use for the adjustment", group = 2)
//                                              @Default() @Switch("n") String note,
//                                          @Switch("s") SpreadSheet sheet
//                                              ) throws IOException, GeneralSecurityException {
//        if (nations.size() > 500) return "Too many nations > 500";
//        // get deposits of nations
//        // get negatives
//
//        double convertValue = ResourceType.convertedTotal(convertTo, 1);
//        if (conversionFactor != null) convertValue /= conversionFactor;
//
//        Map<NationOrAlliance, double[]> toAddMap = new LinkedHashMap<>();
//        double[] total = ResourceType.getBuffer();
//
//        for (DBNation nation : nations) {
//            double[] depo;
//            if (depositType != null) {
//                Map<DepositType, double[]> depoByCategory = nation.getDeposits(db, null, true, true, -1, 0L, false);
//                depo = depoByCategory.get(depositType.type);
//                if (depo == null) continue;
//            } else {
//                depo = nation.getNetDeposits(db, null, true, true, includeGrants, -1, 0L, false);
//            }
//            double[] amtAdd = ResourceType.getBuffer();
//            boolean add = false;
//            for (ResourceType type : ResourceType.values) {
//                if (!negativeResources.contains(type) || type == convertTo) continue;
//                double currAmt = depo[type.ordinal()];
//                if (currAmt < -0.01) {
//                    add = true;
//                    double newAmt = ResourceType.convertedTotal(type, -currAmt) / convertValue;
//                    amtAdd[type.ordinal()] = -currAmt;
//                    amtAdd[convertTo.ordinal()] += -newAmt;
//                }
//            }
//            if (add) {
//                total = ResourceType.add(total, amtAdd);
//                toAddMap.put(nation, amtAdd);
//            }
//        }
//
//        if (toAddMap.isEmpty()) return "No deposits need to be adjusted (" + nations.size() + " nations checked)";
//        if (sheet == null) sheet = SpreadSheet.create(db, SheetKey.TRANSFER_SHEET);
//
//        TransferSheet txSheet = new TransferSheet(sheet).write(toAddMap).build();
//
//        if (note == null) {
//            if (depositType != null) {
//                note = depositType.toString();
//            } else {
//                note = "#deposit";
//            }
//        }
//
//        CM.deposits.addSheet cmd = CM.deposits.addSheet.cmd.sheet(txSheet.getSheet().getURL()).note(note);
//
//        String title = "Addbalance";
//        String body = "Total: \n`" + ResourceType.resourcesToString(total) + "`\nWorth: ~$" + MathMan.format(ResourceType.convertedTotal(total));
//        String emoji = "Confirm";
//
//
//        channel.create().embed(title, body)
//                .commandButton(cmd, emoji)
//                .send();
//        return null;
//    }
//
//    @Command(desc = "Get a sheet of internal transfers for nations", groups = {
//            "Optional: Specify timeframe"
//    })
//    @RolePermission(value = Roles.ECON)
//    public String getNationsInternalTransfers(@Me IMessageIO channel, @Me GuildDB db,
//                                              @AllowDeleted Set<DBNation> nations,
//                                              @Arg(value = "Only list transfers after this time", group = 0)
//                                              @Timestamp @Default Long startTime,
//                                              @Arg(value = "Only list transfers before this time", group = 0)
//                                              @Timestamp @Default Long endTime,
//                                              @Switch("s") SpreadSheet sheet) throws GeneralSecurityException, IOException {
//        if (startTime == null) startTime = 0L;
//        if (endTime == null) endTime = Long.MAX_VALUE;
//        if (sheet == null) sheet = SpreadSheet.create(db, SheetKey.BANK_TRANSACTION_SHEET);
//        if (nations.size() > 1000) return "Too many nations >1000";
//
//        List<Transaction2> transactions = new ArrayList<>();
//        for (DBNation nation : nations) {
//            List<Transaction2> offsets = db.getDepositOffsetTransactions(nation.getNation_id());
//            transactions.addAll(offsets);
//        }
//        Long finalStartTime = startTime;
//        Long finalEndTime = endTime;
//        transactions.removeIf(f -> f.tx_datetime < finalStartTime || f.tx_datetime > finalEndTime);
//
//        sheet.addTransactionsList(channel, transactions, true);
//        return null;
//    }
//
//    @Command(desc = "Get a sheet of transfers")
//    @RolePermission(value = Roles.ECON, root = true)
//    public String getIngameTransactions(@Me IMessageIO channel, @Me GuildDB db,
//                                        @AllowDeleted @Default NationOrAlliance sender,
//                                        @AllowDeleted @Default NationOrAlliance receiver,
//                                        @AllowDeleted @Default NationOrAlliance banker,
//                                        @Default("%epoch%") @Timestamp long timeframe, @Switch("s") SpreadSheet sheet) throws GeneralSecurityException, IOException {
//        if (sheet == null) sheet = SpreadSheet.create(db, SheetKey.BANK_TRANSACTION_SHEET);
//        List<Transaction2> transactions = Locutus.imp().getBankDB().getAllTransactions(sender, receiver, banker, timeframe, null);
//        if (transactions.size() > 10000) return "Timeframe is too large, please use a shorter period";
//
//        sheet.addTransactionsList(channel, transactions, true);
//        return null;
//    }
//
//    @Command(desc = "Get a sheet of a nation or alliances transactions (excluding taxes)", groups = {
//        "Optional: Specify timeframe",
//        "Display Options",
//    })
//    @RolePermission(value = Roles.ECON)
//    public String transactions(@Me IMessageIO channel, @Me GuildDB db, @Me User user,
//                               @AllowDeleted NationOrAllianceOrGuild nationOrAllianceOrGuild,
//                               @Arg(value = "Only show transactions after this time", group = 0)
//                               @Default("%epoch%") @Timestamp long timeframe,
//                               @Arg(value = "Do NOT include the tax record resources below the internal tax rate\n" +
//                                       "Default: False", group = 1)
//                               @Default("false") boolean useTaxBase,
//                               @Arg(value = "Include balance offset records (i.e. from commands)\n" +
//                                       "Default: True", group = 1)
//                               @Default("true") boolean useOffset,
//                               @Switch("s") SpreadSheet sheet,
//                               @Switch("o") boolean onlyOffshoreTransfers) throws GeneralSecurityException, IOException {
//        if (sheet == null) sheet = SpreadSheet.create(db, SheetKey.BANK_TRANSACTION_SHEET);
//
//        if (onlyOffshoreTransfers && nationOrAllianceOrGuild.isNation()) return "Only Alliance/Guilds can have an offshore account";
//
//        List<Transaction2> transactions = new ArrayList<>();
//        if (nationOrAllianceOrGuild.isNation()) {
//            DBNation nation = nationOrAllianceOrGuild.asNation();
//            List<Map.Entry<Integer, Transaction2>> natTrans = nation.getTransactions(db, null, useTaxBase, useOffset, 0, timeframe, false);
//            for (Map.Entry<Integer, Transaction2> entry : natTrans) {
//                transactions.add(entry.getValue());
//            }
//        } else if (nationOrAllianceOrGuild.isAlliance()) {
//            DBAlliance alliance = nationOrAllianceOrGuild.asAlliance();
//
//            // if this alliance - get the transactions in the offshore
//            Set<Integer> aaIds = db.getAllianceIds();
//            OffshoreInstance offshore = db.getOffshore();
//            if (aaIds.contains(alliance.getAlliance_id()) && offshore != null) {
//                transactions.addAll(offshore.getTransactionsAA(aaIds, true));
//            } else if (!aaIds.isEmpty() && db.getOffshore() != null && aaIds.contains(db.getOffshore().getAllianceId()) && offshore != null) {
//                transactions.addAll(offshore.getTransactionsAA(alliance.getAlliance_id(), true));
//            } else {
//                List<Transaction2> txToAdd = (db.getTransactionsById(alliance.getAlliance_id(), 2));
//                boolean hasAdmin = Roles.ECON.hasOnRoot(user); // TODO or has admin in alliance server ?
//                if (!aaIds.contains(alliance.getAlliance_id()) && !hasAdmin) {
//                    txToAdd.removeIf(f -> f.receiver_type != 1 && f.sender_type != 1);
//                }
//                transactions.addAll(txToAdd);
//            }
//
//            if (onlyOffshoreTransfers) {
//                if (offshore == null) return "This alliance does not have an offshore account";
//                Set<Long> offshoreAAs = db.getOffshore().getOffshoreAAs();
//                transactions.removeIf(f -> f.sender_type == 1 || f.receiver_type == 1);
//                transactions.removeIf(f -> f.tx_id != -1 && f.sender_id != 0 && f.receiver_id != 0 && !offshoreAAs.contains(f.sender_id) && !offshoreAAs.contains(f.receiver_id));
//            }
//
//        } else if (nationOrAllianceOrGuild.isGuild()) {
//            GuildDB otherDB = nationOrAllianceOrGuild.asGuild();
//
//            // if this guild - get the transactions in the offshore
//            Map.Entry<GuildDB, Integer> offshoreEntry = otherDB.getOffshoreDB();
//            GuildDB offshoreDb = offshoreEntry.getKey();
//            if (otherDB.getIdLong() == db.getIdLong() || (offshoreDb == db && onlyOffshoreTransfers)) {
//                OffshoreInstance offshore = db.getOffshore();
//                transactions.addAll(offshore.getTransactionsGuild(otherDB.getIdLong(), true));
//            } else {
//                transactions.addAll(db.getTransactionsById(otherDB.getGuild().getIdLong(), 3));
//            }
//        } else if (nationOrAllianceOrGuild.isTaxid()) {
//
//        }
//
//        sheet.addTransactionsList(channel, transactions, true);
//        return null;
//    }
//
//    public static Map<UUID, Map<NationOrAlliance, Map<ResourceType, Double>>> APPROVED_BULK_TRANSFER = new ConcurrentHashMap<>();
//
//
//
//    @Command(desc = "Send multiple transfers to nations/alliances according to a sheet\n" +
//                    "The transfer sheet columns must be `nations` (which has the nations or alliance name/id/url)\n" +
//                    "and then there must be a column named for each resource type you wish to transfer\n" +
//            "OR use a column called `resources` which has a resource list (e.g. a json object of the resources)")
//    @RolePermission(value = {Roles.ECON_WITHDRAW_SELF, Roles.ECON}, alliance = true, any = true)
//    public static String transferBulk(@Me IMessageIO io, @Me JSONObject command, @Me User user, @Me DBNation me, @Me GuildDB db, TransferSheet sheet, DepositType.DepositTypeInfo depositType,
//
//                                      @Arg("The nation account to deduct from") @Switch("n") DBNation depositsAccount,
//                                      @Arg("The alliance bank to send from\nDefaults to the offshore") @Switch("a") DBAlliance useAllianceBank,
//                                      @Arg("The alliance account to deduct from\nAlliance must be registered to this guild\nDefaults to all the alliances of this guild") @Switch("o") DBAlliance useOffshoreAccount,
//                                      @Arg("The tax account to deduct from") @Switch("t") TaxBracket taxAccount,
//                                      @Arg("Deduct from the receiver's tax bracket account") @Switch("ta") boolean existingTaxAccount,
//                                      @Arg("Have the transfer ignored from nation holdings after a timeframe") @Switch("e") @Timediff Long expire,
//                                      @Arg("Have the transfer decrease linearly over a timeframe") @Switch("d") @Timediff Long decay,
//                                      @Switch("m") boolean convertToMoney,
//                                      @Arg("The mode for escrowing funds (e.g. if the receiver is blockaded)\nDefaults to never") @Switch("em") EscrowMode escrow_mode,
//                                      @Switch("b") boolean bypassChecks,
//                                      @Switch("f") boolean force,
//                                      @Switch("k") UUID key) throws IOException {
//        return transferBulkWithErrors(io, command, user, me, db, sheet, depositType, depositsAccount, useAllianceBank, useOffshoreAccount, taxAccount, existingTaxAccount, expire, decay, convertToMoney, escrow_mode, bypassChecks, force, key, new HashMap<>());
//    }
//
//
//    public static String transferBulkWithErrors(@Me IMessageIO io, @Me JSONObject command, @Me User user, @Me DBNation me, @Me GuildDB db, TransferSheet sheet, DepositType.DepositTypeInfo depositType,
//                                        @Arg("The nation account to deduct from") @Switch("n") DBNation depositsAccount,
//                                        @Arg("The alliance bank to send from\nDefaults to the offshore") @Switch("a") DBAlliance useAllianceBank,
//                                        @Arg("The alliance account to deduct from\nAlliance must be registered to this guild\nDefaults to all the alliances of this guild") @Switch("o") DBAlliance useOffshoreAccount,
//                                        @Arg("The tax account to deduct from") @Switch("t") TaxBracket taxAccount,
//                                        @Arg("Deduct from the receiver's tax bracket account") @Switch("ta") boolean existingTaxAccount,
//                                                @Arg("Have the transfer ignored from nation holdings after a timeframe") @Switch("e") @Timediff Long expire,
//                                                @Arg("Have the transfer decrease linearly to zero for balances over a timeframe") @Switch("d") @Timediff Long decay,
//                                      @Switch("m") boolean convertToMoney,
//                                                @Arg("The mode for escrowing funds (e.g. if the receiver is blockaded)\nDefaults to never") @Switch("em") EscrowMode escrow_mode,
//                                      @Switch("b") boolean bypassChecks,
//                                      @Switch("f") boolean force,
//                                      @Switch("k") UUID key,
//                                                Map<NationOrAlliance, TransferResult> errors) throws IOException {
//        if (existingTaxAccount) {
//            if (taxAccount != null) throw new IllegalArgumentException("You can't specify both `tax_id` and `existingTaxAccount`");
//        }
//        double totalVal = 0;
//
//        int nations = 0;
//        int alliances = 0;
//
//        Set<Integer> memberAlliances = new HashSet<>();
//        Map<NationOrAlliance, Map<ResourceType, Double>> transfers = sheet.getTransfers();
//        Map<ResourceType, Double> totalRss = new LinkedHashMap<>();
//        for (Map.Entry<NationOrAlliance, Map<ResourceType, Double>> entry : transfers.entrySet()) {
//            NationOrAlliance natOrAA = entry.getKey();
//            if (natOrAA.isAlliance()) alliances++;
//            if (natOrAA.isNation()) {
//                nations++;
//                memberAlliances.add(natOrAA.asNation().getAlliance_id());
//            }
//            for (Map.Entry<ResourceType, Double> resourceEntry : entry.getValue().entrySet()) {
//                if (resourceEntry.getValue() > 0) {
//                    ResourceType type = resourceEntry.getKey();
//                    Double amt = resourceEntry.getValue();
//                    totalVal += ResourceType.convertedTotal(type, amt);
//                    totalRss.put(type, totalRss.getOrDefault(type, 0d) + amt);
//                }
//            }
//        }
//
//        if (!force) {
//            String title = "Transfer ~$" + MathMan.format(totalVal) + " to ";
//            if (nations > 0) {
//                title += "(" + nations + " nations";
//                if (memberAlliances.size() > 1) {
//                    title += " in " + memberAlliances.size() + " AAs";
//                }
//                title += ")";
//            }
//            if (alliances > 0) {
//                title += "(" + alliances + " AAs)";
//            }
//
//            StringBuilder desc = new StringBuilder();
//            IMessageBuilder msg = io.create();
//            desc.append("Total: `" + ResourceType.resourcesToString(totalRss) + "`\n");
//            desc.append("Note: `" + depositType + "`\n\n");
//            if (escrow_mode != null && escrow_mode != EscrowMode.NEVER) {
//                desc.append("Escrow Mode: `" + escrow_mode + "`\n");
//            }
//            sheet.getSheet().attach(msg, "transfers", desc, true, desc.length());
//
//            if (!errors.isEmpty()) {
//                desc.append("**Warnings**: `" + TransferResult.count(errors.values()) + "`\n");
//                msg = msg.file("errors.csv", TransferResult.toFileString(errors.values()));
//            }
//
//            key = UUID.randomUUID();
//            APPROVED_BULK_TRANSFER.put(key, transfers);
//            String commandStr = command.put("force", "true").put("key", key).toString();
//            msg.embed(title, desc.toString())
//                    .commandButton(commandStr, "Confirm")
//                    .send();
//            return null;
//        }
//        if (key != null) {
//            Map<NationOrAlliance, Map<ResourceType, Double>> approvedAmounts = APPROVED_BULK_TRANSFER.get(key);
//            if (approvedAmounts == null) return "No amount has been approved for transfer. Please try again";
//            Set<NationOrAlliance> keys = new HashSet<>();
//            keys.addAll(approvedAmounts.keySet());
//            keys.addAll(transfers.keySet());
//            for (NationOrAlliance nationOrAlliance : keys) {
//                Map<ResourceType, Double> amtA = approvedAmounts.getOrDefault(nationOrAlliance, Collections.emptyMap());
//                Map<ResourceType, Double> amtB = transfers.getOrDefault(nationOrAlliance, Collections.emptyMap());
//                if (!ResourceType.equals(amtA, amtB)) {
//                    return "The confirmed amount does not match (For " + nationOrAlliance.getMarkdownUrl() + " `" + ResourceType.resourcesToString(amtA) + "` != `" + ResourceType.resourcesToString(amtB) + "`)\n" +
//                            "Please try again, and avoid confirming multiple transfers at once";
//                }
//            }
//        }
//
//        OffshoreInstance offshore;
//        if (useAllianceBank != null) {
//            if (!db.isAllianceId(useAllianceBank.getAlliance_id())) {
//                return "This guild is not registered to the alliance `" + useAllianceBank.getName() + "`";
//            }
//            offshore = useAllianceBank.getBank();
//        } else {
//            offshore = db.getOffshore();
//        }
//        if (offshore == null) {
//            return "No offshore is setup. See " + CM.offshore.add.cmd.toSlashMention();
//        }
//        if (depositsAccount == null && Roles.ECON.getAllianceList(user, db).isEmpty()) {
//            depositsAccount = me;
//        }
//
//        Map<ResourceType, Double> totalSent = new HashMap<>();
//
//        Map<NationOrAlliance, String> notes = sheet.getNotes();
//        StringBuilder output = new StringBuilder();
//        for (Map.Entry<NationOrAlliance, Map<ResourceType, Double>> entry : transfers.entrySet()) {
//            NationOrAlliance receiver = entry.getKey();
//            double[] amount = ResourceType.resourcesToArray(entry.getValue());
//
//            TransferResult result = null;
//            TaxBracket taxAccountFinal = taxAccount;
//            if (existingTaxAccount) {
//                if (!receiver.isNation()) {
//                    result = new TransferResult(OffshoreInstance.TransferStatus.INVALID_DESTINATION, receiver, amount, depositType.toString()).addMessage("Cannot use `existingTaxAccount` for transfers to alliances");
//                } else {
//                    taxAccountFinal = receiver.asNation().getTaxBracket();
//                }
//            }
//
//            DepositType.DepositTypeInfo depositTypeFinal = depositType.clone();
//            String note = notes.get(receiver);
//            if (note != null) {
//                Map<String, String> parsed = DNS.parseTransferHashNotes(note);
//                depositTypeFinal.applyClassifiers(parsed);
//            }
//
//            if (result == null) {
//                try {
//                    result = offshore.transferFromNationAccountWithRoleChecks(
//                            user,
//                            depositsAccount,
//                            useOffshoreAccount,
//                            taxAccountFinal,
//                            db,
//                            io.getIdLong(),
//                            receiver,
//                            amount,
//                            depositTypeFinal,
//                            expire,
//                            decay,
//                            null,
//                            convertToMoney,
//                            escrow_mode,
//                            false,
//                            bypassChecks
//                    );
//                } catch (IllegalArgumentException | IOException e) {
//                    result = new TransferResult(OffshoreInstance.TransferStatus.OTHER, receiver, amount, depositType.toString()).addMessage(e.getMessage());
//                }
//            }
//
//            output.append(receiver.getUrl() + "\t" + receiver.isAlliance() + "\t" + StringMan.getString(amount) + "\t" + result.getStatus() + "\t" + "\"" + result.getMessageJoined(false).replace("\n", " ") + "\"");
//            output.append("\n");
//            if (result.getStatus().isSuccess()) {
//                totalSent = ResourceType.add(totalSent, ResourceType.resourcesToMap(amount));
//                io.create().embed(result.toTitleString(), result.toEmbedString()).send();
//            } else {
//                errors.put(receiver, result);
//            }
//        }
//        IMessageBuilder msg = io.create();
//        if (!errors.isEmpty()) {
//            for (Map.Entry<NationOrAlliance, TransferResult> entry : errors.entrySet()) {
//                NationOrAlliance receiver = entry.getKey();
//                TransferResult result = entry.getValue();
//                output.append(receiver.getUrl() + "\t" + receiver.isAlliance() + result.getStatus() + "\t" + "\"" + result.getMessageJoined(true) + "\"");
//                output.append("\n");
//            }
//            msg.append("Summary: `" + TransferResult.count(new ArrayList<>(errors.values())) + "`\n");
//        }
//
//        msg.file("transfer-results.csv", output.toString())
//                .append("Done!\nTotal sent: `" + resourcesToString(totalSent) + "` worth: ~$" + MathMan.format(convertedTotal(totalSent)));
//
//        msg.send();
//        return null;
//    }
//
//    @Command(desc = "Unlock transfers for an alliance or guild using this guild as an offshore\n" +
//            "Accounts are automatically locked if there is an error accessing the api, a game captcha, or if an admin of the account is banned in-game\n" +
//            "Only locks from game bans persist across restarts")
//    @RolePermission(value = Roles.ADMIN)
//    public String unlockTransfers(@Me GuildDB db, @Default NationOrAllianceOrGuild nationOrAllianceOrGuild, @Switch("a") boolean unlockAll) {
//        OffshoreInstance offshore = db.getOffshore();
//        if (offshore == null) return "No offshore is set";
//        if (unlockAll) {
//            OffshoreInstance.FROZEN_ESCROW.clear();
//            offshore.disabledNations.clear();
//            offshore.disabledGuilds.clear();
//            return "Done!";
//        } else if (nationOrAllianceOrGuild == null) {
//            return "Please specify `nationOrAllianceOrGuild`";
//        }
//        NationOrAllianceOrGuild alliance = nationOrAllianceOrGuild;
//        if (alliance.isNation()) {
//            return "You can only unlock transfers for an alliance or guild";
//        }
//        if (!alliance.isNation() && offshore.getGuildDB() != db) {
//            if (!OffshoreInstance.FROZEN_ESCROW.containsKey(alliance.getId())) {
//                return "The nation: " + alliance.getUrl() + " is not frozen";
//            }
//            OffshoreInstance.FROZEN_ESCROW.remove(alliance.getId());
//            return "Removed the frozen escrow for " + alliance.getUrl();
//        }
//        Set<Long> coalition = offshore.getGuildDB().getCoalitionRaw(Coalition.FROZEN_FUNDS);
//        if (alliance.isAlliance()) {
//            if (coalition.contains((long) alliance.getAlliance_id())) return "Please use `!removecoalition FROZEN_FUNDS " +  alliance.getAlliance_id() + "`";
//            GuildDB otherDb = alliance.asAlliance().getGuildDB();
//            if (otherDb != null && coalition.contains(otherDb.getIdLong())) return "Please use `!removecoalition FROZEN_FUNDS " + otherDb.getIdLong() + "`";
//        } else if (alliance.isGuild()) {
//            if (coalition.contains((long) alliance.getIdLong())) return "Please use `!removecoalition FROZEN_FUNDS " +  alliance.getIdLong() + "`";
//            for (int aaId : alliance.asGuild().getAllianceIds(true)) {
//                if (coalition.contains((long) aaId))return "Please use `!removecoalition FROZEN_FUNDS " + aaId + "`";
//            }
//        } else if (alliance.isNation()){
//            Long removed = offshore.disabledNations.remove(alliance.getId());
//            if (removed == null) {
//                return "No transfers are locked for " + alliance.getQualifiedId() + ". Valid options: " + offshore.disabledNations.keySet().stream().map(f -> DNS.getMarkdownUrl(f, false));
//            }
//            return "Enabled transfers for " + alliance.getQualifiedId();
//        }
//        if (alliance.isGuild() || alliance.isAlliance()) {
//            GuildDB aaDb = alliance.isGuild() ? alliance.asGuild() : alliance.asAlliance().getGuildDB();
//            if (aaDb == null) {
//                return "No guild found for " + alliance.getMarkdownUrl();
//            }
//            if (offshore.disabledGuilds.remove(aaDb.getIdLong()) == null) {
//                String msg = "No transfers are locked for " + alliance.getQualifiedId() + ".";
//                if (!offshore.disabledGuilds.isEmpty()) {
//                    msg += " Valid options:\n- " + offshore.disabledGuilds.keySet().stream().map(f -> {
//                        Guild guild = Locutus.imp().getDiscordApi().getGuildById(f);
//                        return guild == null ? f + "" : guild.toString();
//                    }).collect(Collectors.joining("\n- "));
//                }
//                return msg;
//            }
//        } else {
//            return alliance + " must be a guild or alliance";
//        }
//        return "Done!";
//    }
//
//    @Command(desc = "Bulk set nation internal taxrates as configured in the guild setting: `REQUIRED_INTERNAL_TAXRATE`")
//    @IsAlliance
//    @RolePermission(Roles.ECON_STAFF)
//    public String setNationInternalTaxRates(@Me GuildDB db, @Arg("The nations to set internal taxrates for\nIf not specified, all nations in the alliance will be used")
//    @Default() Set<DBNation> nations, @Arg("Ping users if their rates are modified") @Switch("p") boolean ping) throws Exception {
//        if (nations == null) {
//            nations = db.getAllianceList().getNations();;
//        }
//        List<String> messages = new ArrayList<>();
//        Map<DBNation, Map.Entry<TaxRate, String>> result = db.getHandler().setNationInternalTaxRate(nations, s -> messages.add(s));
//        if (result.isEmpty()) {
//            return "Done! No changes made to internal tax rates";
//        }
//
//        messages.add("\nResult:");
//        for (Map.Entry<DBNation, Map.Entry<TaxRate, String>> entry : result.entrySet()) {
//            DBNation nation = entry.getKey();
//            Map.Entry<TaxRate, String> bracketInfo = entry.getValue();
//
//            String message = "- " + nation.getNation() + ": moved internal rates to `" + bracketInfo.getKey() + "` for reason: `" + bracketInfo.getValue() + "`";
//            if (ping) {
//                User user = nation.getUser();
//                if (user != null) message += " | " + user.getAsMention();
//            }
//            messages.add(message);
//        }
//
//        messages.add("\n**About Internal Tax Rates**\n" +
//                "- Internal tax rates are NOT ingame tax rates\n" +
//                "- Internal rates determine what money%/resource% of ingame city taxes goes to the alliance\n" +
//                "- The remainder is added to a nation's " + CM.deposits.check.cmd.toSlashMention() + "");
//
//        return StringMan.join(messages, "\n");
//    }
//
//    @Command(desc = "List the assigned taxrate if REQUIRED_TAX_BRACKET or REQUIRED_INTERNAL_TAXRATE are set\n" +
//            "Note: this command does set nations brackets. See: `{prefix}tax setNationBracketAuto` and `{prefix}nation set taxinternalAuto` ")
//    @IsAlliance
//    @RolePermission(Roles.ECON_STAFF)
//    public String listRequiredTaxRates(@Me IMessageIO io, @Me GuildDB db, @Switch("s") SpreadSheet sheet) throws GeneralSecurityException, IOException {
//        if (sheet == null) sheet = SpreadSheet.create(db, SheetKey.TAX_BRACKET_SHEET);
//        List<Object> header = new ArrayList<>(Arrays.asList(
//                "nation",
//                "cities",
//                "assigned_bracket_category",
//                "assigned_bracket_name",
//                "assigned_bracket_id",
//                "assigned_bracket_rate",
//                "assigned_internal_category",
//                "assigned_internal_rate"
//        ));
//
//        AllianceList alliances = db.getAllianceList();
//        Map<Integer, TaxBracket> brackets = alliances.getTaxBrackets(TimeUnit.MINUTES.toMillis(5));
//
//        Set<DBNation> nations = alliances.getNations();
//
//        Map<NationFilter, Integer> requiredBrackets = db.getOrNull(GuildKey.REQUIRED_TAX_BRACKET);
//        Map<NationFilter, TaxRate> requiredInternalRates = db.getOrNull(GuildKey.REQUIRED_INTERNAL_TAXRATE);
//
//        if (requiredBrackets == null) requiredBrackets = Collections.emptyMap();
//        if (requiredInternalRates == null) requiredInternalRates = Collections.emptyMap();
//        requiredBrackets.keySet().forEach(NationFilter::recalculate);
//        requiredInternalRates.keySet().forEach(NationFilter::recalculate);
//
//        sheet.setHeader(header);
//
//        for (DBNation nation : nations) {
//            header.set(0, MarkupUtil.sheetUrl(nation.getNation(), DNS.getUrl(nation.getNation_id(), false)));
//            header.set(1, nation.getCities() + "");
//
//            Map.Entry<NationFilter, Integer> requiredBracket = requiredBrackets.entrySet().stream().filter(f -> f.getKey().test(nation)).findFirst().orElse(null);
//            Map.Entry<NationFilter, TaxRate> requiredInternal = requiredInternalRates.entrySet().stream().filter(f -> f.getKey().test(nation)).findFirst().orElse(null);
//
//            header.set(2, requiredBracket != null ? requiredBracket.getKey().getFilter() : "");
//            int taxId = requiredBracket != null ? requiredBracket.getValue() : -1;
//            TaxBracket bracket = brackets.get(taxId);
//            header.set(3, bracket != null ? bracket.getName() : "");
//            header.set(4, bracket != null ? bracket.taxId : "");
//            header.set(5, bracket != null ? "'" + bracket.getTaxRate().toString() + "'" : "");
//
//            header.set(6, requiredInternal != null ? requiredInternal.getKey().getFilter() : "");
//            header.set(7, requiredInternal != null ? requiredInternal.getValue().toString() : null);
//
//            sheet.addRow(header);
//        }
//
//        sheet.updateClearCurrentTab();
//        sheet.updateWrite();
//        sheet.attach(io.create(), "tax_rates").send();
//        return null;
//    }
//
//
//    @Command(desc = "Bulk set nation tax brackets as configured in the guild setting: `REQUIRED_TAX_BRACKET`")
//    @IsAlliance
//    @RolePermission(Roles.ECON_STAFF)
//    public String setNationTaxBrackets(@Me GuildDB db, @Arg("The nations to set tax brackets for\nIf not specified, all nations in the alliance will be used")
//                                       @Default() Set<DBNation> nations, @Arg("Ping users if their brackets are modified")
//                                       @Switch("p") boolean ping) throws Exception {
//        if (nations == null) {
//            nations = db.getAllianceList().getNations();
//        }
//        List<String> messages = new ArrayList<>();
//        Map<DBNation, Map.Entry<TaxBracket, String>> result = db.getHandler().setNationTaxBrackets(nations, s -> messages.add(s));
//        if (result.isEmpty()) {
//            return "Done! No changes made to ingame brackets";
//        }
//
//        messages.add("\nResult:");
//        for (Map.Entry<DBNation, Map.Entry<TaxBracket, String>> entry : result.entrySet()) {
//            DBNation nation = entry.getKey();
//            Map.Entry<TaxBracket, String> bracketInfo = entry.getValue();
//
//            String message = "- " + nation.getNation() + ": moved city bracket to `" + bracketInfo.getKey() + "` for reason: `" + bracketInfo.getValue() + "`";
//            if (ping) {
//                User user = nation.getUser();
//                if (user != null) message += " | " + user.getAsMention();
//            }
//            messages.add(message);
//        }
//
//        messages.add("\n**About Ingame City Tax Brackets**\n" +
//                "- A tax bracket decides what % of city income (money/resources) goes to the alliance bank\n" +
//                "- Funds from raiding, trading or daily login are never taxed\n" +
//                "- Taxes bypass blockades\n" +
//                "- Your internal tax rate will then determine what portion of city taxes go to your " + CM.deposits.check.cmd.toSlashMention() + "");
//
//        return StringMan.join(messages, "\n");
//    }
//
//    @Command(aliases = {"acceptTrades", "acceptTrade"}, desc = "Deposit your pending trades into your nation's holdings for this guild\n" +
//            "The receiver must be authenticated with the bot and have bank access in an alliance\n" +
//            "Only resources sold for $0 or food bought for cash are accepted")
//    @RolePermission(value = Roles.MEMBER)
//    public String acceptTrades(@Me JSONObject command, @Me IMessageIO io, @Me User user, @Me GuildDB db, @Me DBNation me, DBNation receiver, @Default Map<ResourceType, Double> amount, @Switch("a") boolean useLogin, @Switch("f") boolean force) throws Exception {
//        OffshoreInstance offshore = db.getOffshore();
//        if (offshore == null) return "No offshore is set in this guild: <https://github.com/xdnw/diplocutus/wiki/banking>";
//
//        GuildDB receiverDB = Locutus.imp().getGuildDBByAA(receiver.getAlliance_id());
//        if (receiverDB == null) return "Receiver is not in a guild with locutus";
//
//        User receiverUser = receiver.getUser();
//        if (receiverUser == null) return "Receiver is not verified";
//        Member member = receiverDB.getGuild().getMember(receiverUser);
//        if (receiver.active_m() > 1440) return "Receive is offline for >24 hours";
//        if (!force && receiver.getNumWars() > 0 && (member == null || member.getOnlineStatus() != OnlineStatus.ONLINE)) {
//            String title = "Receiver is not online on discord";
//            StringBuilder body = new StringBuilder();
//            body.append("**Receiver:** " + receiver.getNationUrlMarkup(true) + " | " + receiver.getAllianceUrlMarkup(true)).append("\n");
//            int activeM = receiver.active_m();
//            body.append("**Last active** (in-game): " + (activeM == 0 ? "Now" : TimeUtil.secToTime(TimeUnit.MINUTES, activeM))).append("\n");
//            body.append("**Discord Status:** " + (member == null ? "No Discord" : member.getOnlineStatus())).append("\n");
//            body.append("\n> In case there is a game captcha it is recommended to have the receiver online on discord to solve it so your funds can be safely deposited");
//            io.create().confirmation(title, body.toString(), command).cancelButton().send();
//            return null;
//        }
//
//        Map.Entry<double[], String> result;
//        if (amount != null) {
//            Double money = amount.get(ResourceType.MONEY);
//            if (money != null && money < 100000) {
//                throw new IllegalArgumentException("Minimum amount is $100,000");
//            }
//            result = receiver.tradeAndOffshoreDeposit(db, me, ResourceType.resourcesToArray(amount));
//        } else if (!useLogin) {
//            result = receiver.acceptAndOffshoreTrades(db, me);
//        } else {
//            Auth auth = receiver.getAuth(true);
//            if (auth == null) return "Receiver is not authenticated with Locutus: " + CM.credentials.login.cmd.toSlashMention() + "\n" +
//                    "Alternatively, set `useApi: True`";
//            result = auth.acceptAndOffshoreTrades(db, me.getNation_id());
//        }
//        if (ResourceType.isZero(result.getKey())) {
//            return "__**ERROR: No funds have been added to your account**__\n" +
//                    result.getValue();
//        } else {
//            double[] amt = result.getKey();
//            if (amt[0] > 100000 && amt[ResourceType.FOOD.ordinal()] < -1) {
//                long ppu = Math.round(amt[0] / Math.abs(amt[ResourceType.FOOD.ordinal()]));
//                long created = user.getTimeCreated().toEpochSecond() * 1000L;
//                if (ppu == 100_000 && !db.hasAlliance() && me.getAgeDays() < 30) {
//                    Locutus.imp().getRootDb().addCoalition(db.getIdLong(), Coalition.FROZEN_FUNDS);
//                }
//            }
//            return result.getValue();
//        }
//    }
//
//    @Command(desc = "Get a sheet of a nation tax deposits over a period\n" +
//            "If a tax base is set for the nation or alliance then only the portion within member holdings are included by default")
//    @RolePermission(value = Roles.ECON)
//    @IsAlliance
//    public String taxDeposits(@Me IMessageIO io, @Me GuildDB db, Set<DBNation> nations, @Arg("Set to 0/0 to include all taxes") @Default() TaxRate baseTaxRate, @Default() @Timestamp Long startDate, @Default() @Timestamp Long endDate, @Switch("s") SpreadSheet sheet) throws GeneralSecurityException, IOException {
//        Set<Integer> allianceIds = db.getAllianceIds();
//
//        if (startDate == null) startDate = 0L;
//        if (endDate == null) endDate = Long.MAX_VALUE;
//
//        List<BankDB.TaxDeposit> taxes = new ArrayList<>();
//        for (int allianceId : allianceIds) {
//            taxes.addAll(Locutus.imp().getBankDB().getTaxesByAA(allianceId));
//        }
//        Map<Integer, double[]> totalByNation = new HashMap<>();
//
//        int[] baseArr = baseTaxRate == null ? null : baseTaxRate.toArray();
//        TaxRate aaBase = db.getOrNull(GuildKey.TAX_BASE);
//
//        for (BankDB.TaxDeposit tax : taxes) {
//            if (tax.date < startDate || tax.date > endDate) continue;
//            DBNation nation = DBNation.getById(tax.nationId);
//            if (!nations.contains(nation)) continue;
//
//            int[] internalRate = new int[] {tax.internalMoneyRate, tax.internalResourceRate};
//            if (baseArr != null && baseArr[0] >= 0) internalRate[0] = baseArr[0];
//            if (baseArr != null && baseArr[1] >= 0) internalRate[1] = baseArr[1];
//            if (internalRate[0] < 0) internalRate[0] = aaBase != null && aaBase.money >= 0 ? aaBase.money : 0;
//            if (internalRate[1] < 0) internalRate[1] = aaBase != null && aaBase.resources >= 0 ? aaBase.resources : 0;
//
//            tax.multiplyBase(internalRate);
//            double[] taxDepo = totalByNation.computeIfAbsent(tax.nationId, f -> ResourceType.getBuffer());
//            ArrayUtil.apply(ArrayUtil.DOUBLE_ADD, taxDepo, tax.resources);
//        }
//
//        TransferSheet txSheet;
//        if (sheet == null) {
//            txSheet = new TransferSheet(db);
//        } else {
//            txSheet = new TransferSheet(sheet.getSpreadsheetId());
//        }
//        Map<NationOrAlliance, double[]> transfers = new HashMap<>();
//        for (Map.Entry<Integer, double[]> entry : totalByNation.entrySet()) {
//            transfers.put(DBNation.getById(entry.getKey()), entry.getValue());
//        }
//        txSheet.write(transfers).build();
//
//        txSheet.getSheet().attach(io.create(), "tax_deposits").send();
//        return null;
//    }
//
//    @Command(desc = "Get a sheet of a nation's in-game tax records and full resource amounts over a period\n")
//    @RolePermission(value = Roles.ECON)
//    @IsAlliance
//    public String taxRecords(@Me IMessageIO io, @Me GuildDB db, DBNation nation, @Default() @Timestamp Long startDate, @Default() @Timestamp Long endDate, @Switch("s") SpreadSheet sheet) throws GeneralSecurityException, IOException {
//        Set<Integer> aaIds = db.getAllianceIds();
//
//        if (startDate == null) startDate = 0L;
//        if (endDate == null) endDate = Long.MAX_VALUE;
//
//        List<BankDB.TaxDeposit> taxes = new ArrayList<>();
//        for (int aaId : aaIds) {
//            taxes.addAll(Locutus.imp().getBankDB().getTaxesPaid(nation.getNation_id(), aaId));
//        }
//
//        if (sheet == null) sheet = SpreadSheet.create(db, SheetKey.TAX_RECORD_SHEET);
//
//        List<Object> header = new ArrayList<>(Arrays.asList("nation", "date", "taxrate", "internal_taxrate"));
//        for (ResourceType value : ResourceType.values) {
//            if (value != ResourceType.CREDITS) header.add(value.name());
//        }
//        sheet.setHeader(header);
//
//        for (BankDB.TaxDeposit tax : taxes) {
//            if (tax.date < startDate || tax.date > endDate) continue;
//            header.set(0, MarkupUtil.sheetUrl(nation.getNation(), nation.getUrl()));
//            header.set(1, TimeUtil.YYYY_MM_DD_HH_MM_SS.format(new Date(tax.date)));
//            header.set(2, tax.moneyRate + "/" + tax.resourceRate);
//            header.set(3, tax.internalMoneyRate + "/" + tax.internalResourceRate);
//            int i = 0;
//            for (ResourceType type : ResourceType.values) {
//                if (type != ResourceType.CREDITS) header.set(4 + i++, tax.resources[type.ordinal()]);
//            }
//            sheet.addRow(header);
//        }
//        sheet.updateClearCurrentTab();
//        sheet.updateWrite();
//        sheet.attach(io.create(), "tax_records").send();
//        return null;
//    }
//
//    @Command(desc = "Send from your nation's deposits) to another account (internal transfer", groups = {
//            "Amount to send",
//            "Required: Send To",
//            "Optional: Send From"
//    },
//            groupDescs = {
//                    "",
//                    "Pick a receiver account, nation, or both",
//                    "If there are multiple alliances registered to this guild"
//            })
//    @RolePermission(value = Roles.ECON)
//    public String send(@Me IMessageIO channel, @Me JSONObject command, @Me GuildDB senderDB, @Me User user, @Me DBAlliance alliance, @Me Rank rank, @Me DBNation me,
//                       @Arg(value = "The amount to send", group = 0)
//                       @AllianceDepositLimit Map<ResourceType, Double> amount,
//
//                       @Arg(value = "The offshore alliance or guild account to send to\n" +
//                       "Defaults to this guild", group = 1)
//                       @Default GuildOrAlliance receiver_account,
//                       @Arg(value = "The alliance or guild nation account to send to\n" +
//                       "Defaults to None", group = 1)
//                       @Default DBNation receiver_nation,
//
//                       @Arg(value = "The offshore alliance account to send from\n" +
//                       "Defaults to your alliance (if valid)", group = 2)
//                       @Default DBAlliance sender_alliance,
//
//                       @Switch("f") boolean force) throws IOException {
//        if (OffshoreInstance.DISABLE_TRANSFERS && user.getIdLong() != Locutus.loader().getAdminUserId()) throw new IllegalArgumentException("Error: Maintenance");
//        return sendAA(channel, command, senderDB, user, me, amount, receiver_account, receiver_nation, sender_alliance, me, force);
//    }
//
//    @Command(desc = "Send from your alliance offshore account to another account (internal transfer)", groups = {
//            "Amount to send",
//            "Required: Send To",
//            "Optional: Send From"
//    },
//    groupDescs = {
//            "",
//            "Pick a receiver account, nation, or both",
//            "If using a different account to send from"
//    })
//    @RolePermission(value = Roles.ECON)
//    public String sendAA(@Me IMessageIO channel, @Me JSONObject command, @Me GuildDB senderDB, @Me User user, @Me DBNation me,
//                         @Arg(value = "The amount to send", group = 0)
//                         @AllianceDepositLimit Map<ResourceType, Double> amount,
//                         @Arg(value = "The offshore alliance or guild account to send to\n" +
//                                 "Defaults to this guild", group = 1)
//                         @Default GuildOrAlliance receiver_account,
//                         @Arg(value = "The alliance or guild nation account to send to\n" +
//                                 "Defaults to None", group = 1)
//                         @Default DBNation receiver_nation,
//
//                         @Arg(value = "The offshore alliance account to send from\n" +
//                                 "Defaults to your alliance (if valid)", group = 2)
//                         @Default DBAlliance sender_alliance,
//                         @Arg(value = "The nation to send from\n" +
//                                 "Defaults to your nation", group = 2)
//                         @Default DBNation sender_nation,
//                         @Switch("f") boolean force) throws IOException {
//        if (user.getIdLong() != Locutus.loader().getAdminUserId()) return "WIP";
//        if (OffshoreInstance.DISABLE_TRANSFERS && user.getIdLong() != Locutus.loader().getAdminUserId()) throw new IllegalArgumentException("Error: Maintenance");
//        if (sender_alliance != null && !senderDB.isAllianceId(sender_alliance.getId())) {
//            throw new IllegalArgumentException("Sender alliance is not in this guild");
//        }
//        if (receiver_account == null && receiver_nation == null) {
//            throw new IllegalArgumentException("Please specify a `receiver_account` or `receiver_nation` or both");
//        }
//        boolean hasEcon = Roles.ECON.has(user, senderDB.getGuild());
//        if (!hasEcon) {
//            if (sender_alliance != null && sender_alliance.getId() != me.getAlliance_id()) {
//                throw new IllegalArgumentException("You do not have permission to send from another alliance (only: " + me.getAllianceUrlMarkup(true) + ") " + Roles.ECON.toDiscordRoleNameElseInstructions(senderDB.getGuild()));
//            }
//            if (sender_nation == null) {
//                throw new IllegalArgumentException("You do not have permission to omit `sender_nation` " + Roles.ECON.toDiscordRoleNameElseInstructions(senderDB.getGuild()));
//            } else if (sender_nation.getId() != me.getId()) {
//                throw new IllegalArgumentException("You do not have permission to send from another nation (only: " + me.getNationUrlMarkup(true) + ") " + Roles.ECON.toDiscordRoleNameElseInstructions(senderDB.getGuild()));
//            }
//        }
//
//        double[] amountArr = ResourceType.resourcesToArray(amount);
//        GuildDB receiverDB = receiver_account.isGuild() ? receiver_account.asGuild() : null;
//        DBAlliance receiverAlliance = receiver_account.isAlliance() ? receiver_account.asAlliance() : null;
//        List<TransferResult> results = senderDB.sendInternal(user, me, senderDB, sender_alliance, sender_nation, receiverDB, receiverAlliance, receiver_nation, amountArr, force);
//        if (results.size() == 1) {
//            TransferResult result = results.get(0);
//            if (result.getStatus() == OffshoreInstance.TransferStatus.CONFIRMATION) {
//                String worth = "$" + MathMan.format(ResourceType.convertedTotal(amount));
//                String receiverName;
//                if (receiver_account != null && receiver_nation != null) {
//                    receiverName = receiver_account.getQualifiedName() + " | " + receiver_nation.getNation();
//                } else if (receiver_account != null) {
//                    receiverName = receiver_account.getQualifiedName();
//                } else {
//                    receiverName = receiver_nation.getNation();
//                }
//                String title = "Send (worth: " + worth + ") to " + receiverName;
//                channel.create().confirmation(title, result.getMessageJoined(false), command, "force", "Send").cancelButton().send();
//                return null;
//            }
//        }
//        Map.Entry<String, String> embed = TransferResult.toEmbed(results);
//        channel.create().embed(embed.getKey(), embed.getValue()).send();
//        return null;
//    }
//
    @Command(desc="Displays the account balance for a nation, alliance or guild\n" +
            "Balance info includes deposits, loans, grants, taxes and escrow")
    @RolePermission(Roles.MEMBER)
    public static String deposits(@Me Guild guild, @Me GuildDB db, @Me IMessageIO channel, @Me DBNation me, @Me User author,
                           @AllowDeleted
                           @StarIsGuild
                           @Arg("Account to check holdings for") NationOrAllianceOrGuild nationOrAllianceOrGuild,
                           @Arg("The alliances to check transfers from\nOtherwise the guild configured ones will be used")
                           @Switch("a") Set<DBAlliance> offshores,
                           @Arg("Only include transfers after this time")
                           @Switch("c") @Timestamp Long timeCutoff,
//                            @Arg("Include all taxes in account balance")
//                           @Switch("b") boolean includeBaseTaxes,
                            @Arg("Do NOT include manual offsets in account balance")
                           @Switch("o") boolean ignoreInternalOffsets,
                            @Arg("Show separate sections for taxes and deposits")
                           @Switch("t") Boolean showCategories,
                           @Switch("d") boolean replyInDMs,
                           @Arg("Include expired transfers")
                           @Switch("e") boolean includeExpired,
                           @Arg("Include transfers marked as ignore")
                           @Switch("i") boolean includeIgnored,
                           @Switch("z") boolean allowCheckDeleted
//                           @Arg("Hide the escrow balance ") @Switch("h") boolean hideEscrowed
    ) throws IOException {
        boolean condensedFormat = GuildKey.DISPLAY_CONDENSED_DEPOSITS.getOrNull(db) == Boolean.TRUE;
//        if (!nationOrAllianceOrGuild.isNation() && !nationOrAllianceOrGuild.isTaxid()) {
//            showCategories = false;
//        }
        if (showCategories == null) {
            showCategories = (db.getOrNull(GuildKey.DISPLAY_ITEMIZED_DEPOSITS) == Boolean.TRUE);
        }
        if (timeCutoff == null) timeCutoff = 0L;
        Set<Long> offshoreIds = offshores == null ? null : offshores.stream().map(f -> f.getIdLong()).collect(Collectors.toSet());
        if (offshoreIds != null) offshoreIds = DNS.expandCoalition(offshoreIds);

//        boolean hasAdmin = Roles.ECON.has(author, guild);
//        AllianceList allowed = Roles.ECON.getAllianceList(author, db);

        Map<DepositType, double[]> accountDeposits = new HashMap<>();
//        double[] escrowed = null;
//        long escrowExpire = 0;

        List<String> footers = new ArrayList<>();

        if (nationOrAllianceOrGuild.isAlliance()) {
            DBAlliance alliance = nationOrAllianceOrGuild.asAlliance();

            GuildDB otherDb2 = alliance.getGuildDB();
            if (otherDb2 == null && !allowCheckDeleted) throw new IllegalArgumentException("No guild found for " + alliance);

//            OffshoreInstance offshore = otherDb2 == null && allowCheckDeleted ? db.getOffshore() : otherDb2.getOffshore();
//            if (offshore == null)
            {
                if (otherDb2 == db) {
                    if (!Roles.ECON.has(author, otherDb2.getGuild())) {
                        return "You do not have permisssion to check another alliance's deposits (1)";
                    }
                    long updateAfter = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1);
                    Map<ResourceType, Double> stock = alliance.getStockpile(updateAfter);
                    accountDeposits.put(DepositType.DEPOSIT, ResourceType.resourcesToArray(stock));
                } else {
                    return "The alliance specified is not registered to this discord server.";// + CM.coalition.add.cmd.alliances("AA:" + alliance.getAlliance_id()).coalitionName(Coalition.OFFSHORE.name()) + " and from the offshore server use " + CM.coalition.add.cmd.alliances("AA:" + alliance.getAlliance_id()).coalitionName(Coalition.OFFSHORING.name()) + "";
                }
            }
//            else if (otherDb2 != db && offshore.getGuildDB() != db) {
//                return "You do not have permisssion to check another alliance's deposits (2)";
//            } else {
//                if ((otherDb2 == null || !Roles.ECON.has(author, otherDb2.getGuild())) && (!Roles.ECON.has(author, offshore.getGuildDB().getGuild()))) {
//                    return "You do not have permisssion to check another alliance's deposits (3)";
//                }
//                double[] deposits = ResourceType.resourcesToArray(offshore.getDeposits(alliance.getAlliance_id(), true));
//                accountDeposits.put(DepositType.DEPOSIT, deposits);
//            }
//        } else if (nationOrAllianceOrGuild.isGuild()) {
//            GuildDB otherDb = nationOrAllianceOrGuild.asGuild();
//            OffshoreInstance offshore = otherDb.getOffshore();
//            if (offshore == null) return "No offshore is set. In this server, use " + CM.coalition.add.cmd.alliances(nationOrAllianceOrGuild.getIdLong() + "").coalitionName(Coalition.OFFSHORE.name()) + " and from the offshore server use " + CM.coalition.add.cmd.alliances(nationOrAllianceOrGuild.getIdLong() + "").coalitionName(Coalition.OFFSHORING.name()) + "";
//
//            if (!Roles.ECON.has(author, offshore.getGuildDB().getGuild()) && !Roles.ECON.has(author, otherDb.getGuild())) {
//                return "You do not have permission to check another guild's deposits";
//            }
//            // txList
//            double[] deposits = offshore.getDeposits(otherDb);
//            accountDeposits.put(DepositType.DEPOSIT, deposits);

        } else if (nationOrAllianceOrGuild.isNation()) {
            DBNation nation = nationOrAllianceOrGuild.asNation();
            if (nation != me && !Roles.INTERNAL_AFFAIRS.has(author, guild) && !Roles.INTERNAL_AFFAIRS_STAFF.has(author, guild) && !Roles.ECON.has(author, guild) && !Roles.ECON_STAFF.has(author, guild)) return "You do not have permission to check other nation's deposits";
            // txList
            accountDeposits = nation.getDeposits(db, offshoreIds, !ignoreInternalOffsets, 0L, timeCutoff, includeExpired, includeIgnored, f -> true);
//            if (!hideEscrowed) {
//                Map.Entry<double[], Long> escoredPair = db.getEscrowed(nation);
//                if (escoredPair != null) {
//                    if (escrowed == null) {
//                        escrowed = ResourceType.getBuffer();
//                    }
//                    ResourceType.add(escrowed, escoredPair.getKey());
//                    escrowExpire = escoredPair.getValue();
//                }
//            }
//        } else if (nationOrAllianceOrGuild.isTaxid()) {
//            TaxBracket bracket = nationOrAllianceOrGuild.asBracket();
//            Map<DepositType, double[]> deposits = db.getTaxBracketDeposits(bracket.taxId, timeCutoff, includeExpired, includeIgnored);
//            accountDeposits.putAll(deposits);
//
//            if (showCategories) {
//                footers.add("`#TAX` is for the portion of tax income that does NOT go into member holdings");
//                footers.add("`#DEPOSIT` is for the portion of tax income in member holdings");
//            } else {
//                footers.add("Set `showCategories` for breakdown of tax within member holdings");
//            }
        }

        String title = "Deposits for: " + nationOrAllianceOrGuild.getQualifiedName();
        Map.Entry<double[], String> balanceBody = DNS.createDepositEmbed(db, nationOrAllianceOrGuild, accountDeposits, showCategories, condensedFormat);
        double[] balance = balanceBody.getKey();
        String body = balanceBody.getValue();
        Map<String, Map.Entry<CommandRef, Boolean>> buttons = new LinkedHashMap<>();

        if (me != null && nationOrAllianceOrGuild == me) {
            if (!condensedFormat) {
                footers.add("Funds default to `#deposit` if no other note is used");
            }
//            if (Boolean.TRUE.equals(db.getOrNull(GuildKey.RESOURCE_CONVERSION)) && !condensedFormat) {
//                footers.add("You can sell resources to the alliance by depositing with the note `#cash`");
//            }
//            if (PW.convertedTotal(balance) > 0 && Boolean.TRUE.equals(db.getOrNull(GuildKey.MEMBER_CAN_WITHDRAW))) {
//                if (Roles.ECON_WITHDRAW_SELF.has(author, db.getGuild())) {
//                    footers.add("To withdraw, use: `" + CM.transfer.self.cmd.toSlashMention() + "` ");
//                }
//            }
        }
        boolean econStaff = Roles.ECON_STAFF.has(author, guild);
        boolean econ = Roles.ECON.has(author, guild);

        if (nationOrAllianceOrGuild.isNation()) {
//            if (escrowed != null && !ResourceType.isZero(escrowed)) {
//                if (Roles.ECON_WITHDRAW_SELF.has(author, guild)) {
//                    // add button, add note
//                    buttons.put("withdraw escrow", Map.entry(CM.escrow.withdraw.cmd.receiver(nationOrAllianceOrGuild.getQualifiedId()).amount(""), true));
//                } else if (!condensedFormat) {
//                    footers.add("You do not have permission to withdraw escrowed funds");
//                }
//            }
        } else if (nationOrAllianceOrGuild.isAlliance()) {
            if (econ) {
//                buttons.put("withdraw",
//                        Map.entry(
//                                CM.transfer.resources.cmd.receiver("").transfer("").depositType(DepositType.IGNORE.name())
//                                        .allianceAccount(nationOrAllianceOrGuild.getQualifiedId()),
//                                true));
//                footers.add("To withdraw: " + CM.transfer.resources.cmd.toSlashMention() + " with `#ignore` as note");
            }
//        } else if (nationOrAllianceOrGuild.isTaxid()) {
////            buttons.put("withdraw",
////                    Map.entry(
////                            CM.transfer.resources.cmd.receiver("").transfer("").depositType("").taxAccount(nationOrAllianceOrGuild.getQualifiedName()),
////                            true));
////            footers.add("To withdraw: " + CM.transfer.resources.cmd.toSlashMention() + " with `taxaccount: " + nationOrAllianceOrGuild.getQualifiedName() + "`");
//
//            if (econ) {
//                footers.add("To add balance: " + CM.deposits.add.cmd.toSlashMention() + " with `accounts: " + nationOrAllianceOrGuild.getQualifiedName() + "`");
//            }
        } else if (nationOrAllianceOrGuild.isGuild()) {
            // trade deposit
            if (econ) {
//                buttons.put("withdraw",
//                        Map.entry(
//                                CM.transfer.resources.cmd.receiver("").transfer("").depositType(DepositType.IGNORE.name()),
//                                true));
//                footers.add("To withdraw: " + CM.transfer.resources.cmd.toSlashMention() + " with `#ignore` as note");
//                Map.Entry<GuildDB, Integer> offshore = db.getOffshoreDB();
//                if (offshore != null) {
//                    Set<Integer> aaIds = db.getAllianceIds();
//                    String note = aaIds.isEmpty() ? "#guild=" + guild.getIdLong() : "#alliance=" + aaIds.iterator().next();
//                    footers.add("Send to " + DNS.getMarkdownUrl(offshore.getValue(), true) + " with note `" + note + "` to offshore\n" +
//                            "Or " + MarkupUtil.markdownUrl("send a trade", "https://github.com/xdnw/diplocutus/wiki/banking#for-my-corporation"));
//                }
            }
        }

        if (!showCategories && (nationOrAllianceOrGuild.isNation())) {// || nationOrAllianceOrGuild.isTaxid())) {
            // add footer and button for showing separately
            String itemziedSetting = !econ ? "" : "or " + GuildKey.DISPLAY_ITEMIZED_DEPOSITS.getCommandMention() + " ";
            if (!condensedFormat) {
                footers.add("Use `showCategories: True` " + itemziedSetting + "for a breakdown");
            }
//            buttons.put("breakdown",
//                    Map.entry( // TODO CM REF
//                            CM.deposits.check.cmd.nationOrAllianceOrGuild(
//                                    nationOrAllianceOrGuild.getQualifiedId())
//                                    .offshores(
//                                    offshoreIds == null || offshoreIds.isEmpty() ? null : StringMan.join(offshoreIds, ",")
//                                    ).timeCutoff(
//                                    timeCutoff != null && timeCutoff > 0 ? "timestamp:" + timeCutoff : null
//                                    ).ignoreInternalOffsets(
//                                    ignoreInternalOffsets ? "true" : null
//                                            ).showCategories(
//                                    "true"
//                                            ).replyInDMs(
//                                    replyInDMs ? "true" : null
//                                            ).includeExpired(
//                                    includeExpired ? "true" : null
//                                            ).includeIgnored(
//                                    includeIgnored ? "true" : null
//                            ), false));
        }

//        boolean canOffshore = db.isValidAlliance() && (Boolean.TRUE.equals(GuildKey.MEMBER_CAN_OFFSHORE.getOrNull(db)) || Roles.ECON_STAFF.has(author, guild));
//        if (canOffshore && (nationOrAllianceOrGuild.isNation() || nationOrAllianceOrGuild.isAlliance())) {
//            Map.Entry<GuildDB, Integer> offshore = db.getOffshoreDB();
//            if (offshore != null) {
//                buttons.put("offshore",
//                        Map.entry(
//                                CM.offshore.send.cmd,
//                                false));
//                if (!condensedFormat) {
//                    if (GuildKey.API_KEY.getOrNull(db) != null) {
//                        footers.add("To offshore: " + CM.offshore.send.cmd.toSlashMention() + "");
//                    } else {
//                        footers.add("To offshore, send to " + DNS.getMarkdownUrl(offshore.getValue(), true) + "");
//                    }
//                }
//            }
//        }

        StringBuilder response = new StringBuilder(body);

        if (!footers.isEmpty()) {
            if (condensedFormat) {
                response.append("\n**Tips:**\n");
            } else {
                response.append("\n## Tips:\n");
            }
            for (int i = 0; i < footers.size(); i++) {
                String footer = footers.get(i);
                response.append("- " + footer + "\n");
            }
        }

        IMessageIO output = replyInDMs ? new DiscordChannelIO(RateLimitUtil.complete(author.openPrivateChannel()), null) : channel;

        IMessageBuilder msg = output.create().embed(title, response.toString());
        for (Map.Entry<String, Map.Entry<CommandRef, Boolean>> entry : buttons.entrySet()) {
            String label = entry.getKey();
            CommandRef cmd = entry.getValue().getKey();
            boolean isModal = entry.getValue().getValue();
            if (isModal) {
                msg = msg.modal(CommandBehavior.EPHEMERAL, cmd, label);
            } else {
                msg = msg.commandButton(CommandBehavior.EPHEMERAL, cmd, label);
            }
        }

        CompletableFuture<IMessageBuilder> msgFuture = msg.send();

        if (me != null && nationOrAllianceOrGuild.isNation() && nationOrAllianceOrGuild.asNation().getPosition() > 1 && db.isWhitelisted() && db.getOrNull(GuildKey.API_KEY) != null && db.getAllianceIds(true).contains(nationOrAllianceOrGuild.asNation().getAlliance_id())) {
            DBNation finalNation = nationOrAllianceOrGuild.asNation();
            long updateAfter = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1);
            Locutus.imp().getExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    int initialLength = response.length();
                    Map<ResourceType, Double> stockpile = finalNation.getStockpile(updateAfter);
//                    if (stockpile != null && !stockpile.isEmpty()) {
//                        Map<ResourceType, Double> excess = finalNation.checkExcessResources(db, stockpile);
//                        if (!excess.isEmpty()) {
//                            response.append("- Excess can be deposited: ```" + ResourceType.resourcesToString(excess) + "```\n");
//                        }
//                    }
//                    Map<ResourceType, Double> needed = finalNation.getResourcesNeeded(stockpile, 3, true);
//                    if (!needed.isEmpty()) {
//                        response.append("- Missing resources for the next 3 days: ```" + ResourceType.resourcesToString(needed) + "```\n");
//                    }

//                    if (me != null && me.getNation_id() == finalNation.getNation_id() && Boolean.TRUE.equals(db.getOrNull(GuildKey.MEMBER_CAN_OFFSHORE)) && db.isValidAlliance()) {
//                        AllianceList alliance = db.getAllianceList();
//                        if (alliance != null && !alliance.isEmpty() && alliance.contains(me.getAlliance_id())) {
//                            try {
//                                Map<ResourceType, Double> aaStockpile = me.getAlliance().getStockpile();
//                                if (aaStockpile != null && ResourceType.convertedTotal(aaStockpile) > 5000000) {
//                                    response.append("- The alliance bank has funds to offshore\n");
//                                }
//                            } catch (Throwable ignore) {}
//                        }
//                    }

                    if (response.length() != initialLength) {
                        try {
                            IMessageBuilder msg = msgFuture.get();
                            if (msg != null && msg.getId() > 0) {
                                msg.clearEmbeds().embed(title, response.toString()).send();
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
        }

        return null;
    }
//
//    @Command(desc = "Calculate weekly interest payments for a loan")
//    public String weeklyInterest(@Arg("Principle amount") double amount, @Arg("Percent weekly interest") double pct, @Arg("Number of weeks to loan for") int weeks) {
//        double totalInterest = weeks * (pct / 100d) * amount;
//
//        double weeklyPayments = (totalInterest + amount) / weeks;
//
//        StringBuilder result = new StringBuilder("```");
//        result.append("Principle Amount: $" + MathMan.format(amount)).append("\n");
//        result.append("Loan Interest Rate: " + MathMan.format(pct)).append("%\n");
//        result.append("Total Interest: $" + MathMan.format(totalInterest)).append("\n");
//        result.append("Weekly Payments: $" + MathMan.format(weeklyPayments)).append("\n");
//
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
//        int day = calendar.get(Calendar.DAY_OF_WEEK);
//
//        int dayOffset = 0;
//        switch (day) {
//            case Calendar.MONDAY:
//            case Calendar.TUESDAY:
//                dayOffset = Calendar.FRIDAY - day;
//                break;
//            case Calendar.WEDNESDAY:
//            case Calendar.THURSDAY:
//            case Calendar.FRIDAY:
//            case Calendar.SATURDAY:
//            case Calendar.SUNDAY:
//                dayOffset += 7 + Calendar.FRIDAY - day;
//                break;
//        }
//
//        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
//        ZonedDateTime due = now.plusDays(dayOffset);
//        DateTimeFormatter pattern = DateTimeFormatter.ofPattern("EEEE dd MMMM", Locale.ENGLISH);
//
//        result.append("Today: " + pattern.format(now)).append("\n");
//        String repeating = pattern.format(due) + " and every Friday thereafter for a total of " + weeks + " weeks.";
//        result.append("First Payment Due: " + repeating).append("```");
//
//        return result.toString();
//    }
//
    @Command(desc = "List all nations in the alliance and their in-game resource stockpile", groups = {
            "Optional: Specify Nations",
            "Display Options",
    })
    @RolePermission(any = true, value = {Roles.ECON_STAFF, Roles.ECON})
    @IsAlliance
    public String stockpileSheet(@Me GuildDB db, @Me IMessageIO channel,
                                 @Arg(value = "Only include stockpiles from these nations", group = 0) @Default NationList nationFilter,

                                 @Arg(value = "Divide stockpiles by city count", group = 1) @Switch("n") boolean normalize,
//                                 @Arg(value = "Only show the resources well above warchest and city operation requirements", group = 1) @Switch("e") boolean onlyShowExcess,
                                 @Switch("f") boolean forceUpdate) throws IOException, GeneralSecurityException {
        if (nationFilter != null && !db.getAllianceIds().containsAll(nationFilter.getAllianceIds())) {
            return "You can only view stockpiles for nations in your alliance: (" + db.getAllianceIds() + ")";
        }
        AllianceList alliance = db.getAllianceList();
        if (nationFilter != null) alliance = alliance.subList(nationFilter.getAllianceIds());

        Map<DBNation, Map<ResourceType, Double>> stockpile = alliance.getMemberStockpile();

        List<String> header = new ArrayList<>();
        header.add("nation");
        header.add("discord");
        header.add("infra");
        header.add("land");
        header.add("off|def");
        header.add("strength");

        header.add("marketValue");
        for (ResourceType value : ResourceType.values) {
            header.add(value.name().toLowerCase());
        }

        SpreadSheet sheet = SpreadSheet.create(db, SheetKey.STOCKPILE_SHEET);
        sheet.setHeader(header);

        double[] aaTotal = ResourceType.getBuffer();
        for (Map.Entry<DBNation, Map<ResourceType, Double>> entry : stockpile.entrySet()) {
            List<Object> row = new ArrayList<>();

            DBNation nation = entry.getKey();
            if (nation == null || (nationFilter != null && !nationFilter.getNations().contains(nation))) continue;
            row.add(MarkupUtil.sheetUrl(nation.getNation(), nation.getUrl()));
            row.add(nation.getUserDiscriminator());
            row.add(nation.getInfra());
            row.add(nation.getLand());
            row.add(nation.getOff() +"|" + nation.getDef());
            row.add(nation.getStrength());

            Map<ResourceType, Double> rss = entry.getValue();
//            if (onlyShowExcess) {
//                rss = nation.checkExcessResources(db, rss, false);
//            }

            row.add(ResourceType.convertedTotal(rss));

            for (ResourceType type : ResourceType.values) {
                double amt = rss.getOrDefault(type, 0d);
                if (normalize) amt /= nation.getInfra();
                row.add(amt);

                if (amt > 0) aaTotal[type.ordinal()] += amt;
            }

            sheet.addRow(row);
        }

        sheet.updateClearCurrentTab();
        sheet.updateWrite();

        String totalStr = ResourceType.resourcesToFancyString(aaTotal);
        totalStr += "\n`note:total ignores nations with alliance info disabled`";
        sheet.attach(channel.create().embed("Nation Stockpiles", totalStr), "stockpiles").send();
        return null;
    }
//
//    @Command(desc = "Generate a sheet of member tax brackets and internal tax rates\n" +
//            "`note: internal tax rate is the TAX_BASE and determines what % of their taxes is excluded from deposits`")
//    @RolePermission(any = true, value = {Roles.ECON, Roles.ECON_STAFF})
//    public String taxBracketSheet(@Me IMessageIO io, @Me GuildDB db, @Switch("f") boolean force, @Switch("a") boolean includeApplicants) throws Exception {
//        SpreadSheet sheet = SpreadSheet.create(db, SheetKey.TAX_BRACKET_SHEET);
//        List<Object> header = new ArrayList<>(Arrays.asList(
//                "nation",
//                "position",
//                "cities",
//                "age",
//                "deposits",
//                "tax_id",
//                "tax_rate",
//                "internal"
//        ));
//
//        sheet.setHeader(header);
//
//        boolean failedFetch = true;
//        Map<Integer, TaxBracket> brackets;
//        try {
//            AllianceList alliances = db.getAllianceList();
//            if (alliances == null) {
//                return "Please register an alliance: " + CM.settings_default.registerAlliance.cmd.toSlashMention();
//            }
//            brackets = alliances.getTaxBrackets(TimeUnit.MINUTES.toMillis(5));
//            failedFetch = false;
//        } catch (IllegalArgumentException e) {
//            brackets = new LinkedHashMap<>();
//            Set<Integer> allianceIds = db.getAllianceIds(true);
//            Map<Integer, TaxBracket> allAllianceBrackets = Locutus.imp().getBankDB().getTaxBracketsAndEstimates();
//            for (Map.Entry<Integer, TaxBracket> entry : allAllianceBrackets.entrySet()) {
//                TaxBracket bracket = entry.getValue();
//                if (allianceIds.contains(bracket.getAlliance_id(false))) {
//                    brackets.put(entry.getKey(), bracket);
//                }
//            }
//        }
//        Map<DBNation, TaxBracket> nations = new HashMap<>();
//        for (TaxBracket bracket : brackets.values()) {
//            for (DBNation nation : bracket.getNations()) {
//                nations.put(nation, bracket);
//            }
//        }
//
//        db.getAutoRoleTask().updateTaxRoles(nations);
//
//        long threshold = force ? 0 : Long.MAX_VALUE;
//
//        for (Map.Entry<DBNation, TaxBracket> entry : nations.entrySet()) {
//            TaxBracket bracket = entry.getValue();
//            DBNation nation = entry.getKey();
//
//            if (!includeApplicants && nation.getPosition() <= 1) continue;
//
//            header.set(0, MarkupUtil.sheetUrl(nation.getNation(), nation.getUrl()));
//            header.set(1, nation.getPositionEnum().name());
//            header.set(2, nation.getCities());
//            header.set(3, nation.getAgeDays());
//            header.set(4, String.format("%.2f", nation.getNetDepositsConverted(db, threshold)));
//            header.set(5, bracket.taxId + "");
//            header.set(6, bracket.moneyRate + "/" + bracket.rssRate);
//
//            TaxRate internal = db.getHandler().getInternalTaxrate(nation.getNation_id());
//            header.set(7, internal.toString());
//
//            sheet.addRow(header);
//        }
//
//        sheet.updateClearCurrentTab();
//        sheet.updateWrite();
//
//        StringBuilder response = new StringBuilder();
//        if (failedFetch) response.append("\nnote: Please set an api key with " + CM.credentials.addApiKey.cmd.toSlashMention() + " to view updated tax brackets");
//
//        sheet.attach(io.create(), "tax_brackets", response.toString()).send();
//        return null;
//    }
//
//    @Command(desc = "Set the bot managed offshore for this guild\n" +
//            "The alliance must use a guild with locutus settings `ALLIANCE_ID` and `API_KEY` set, and the coalitions `offshore` and `offshoring` set to include the offshore alliance")
//    @RolePermission(value = Roles.ADMIN)
//    public String addOffshore(@Me IMessageIO io, @Me User user, @Me GuildDB root, @Me DBNation nation, @Me JSONObject command, DBAlliance offshoreAlliance, @Switch("n") boolean newAccount, @Switch("i") boolean importAccount, @Switch("f") boolean force) throws IOException {
//        if (importAccount && newAccount) {
//            throw new IllegalArgumentException("Cannot specify both `newAccount` and `importAccount` (pick one)");
//        }
//        if (root.isDelegateServer()) return "Cannot enable offshoring for delegate server (run this command in the root server)";
//        IMessageBuilder confirmButton = io.create().confirmation(command);
//        GuildDB offshoreDB = offshoreAlliance.getGuildDB();
//
//        if (offshoreDB != null) {
//            if (!offshoreDB.hasAlliance()) {
//                return "Please set the key " + GuildKey.ALLIANCE_ID.getCommandMention() + " in " + offshoreDB.getGuild();
//            }
//            DnsApi api = offshoreAlliance.getApi(AlliancePermission.WITHDRAW_BANK, AlliancePermission.VIEW_BANK);
//            if (api == null) {
//                return "Please set the key " + GuildKey.API_KEY.getCommandMention() + " in " + offshoreDB.getGuild();
//            }
//        }
//
//        if (root.isOffshore(true) && (offshoreDB == null || (offshoreDB == root))) {
//            if (nation.getAlliance_id() != offshoreAlliance.getAlliance_id()) {
//                GuildKey.ALLIANCE_ID.validate(root, user, Set.of(offshoreAlliance.getAlliance_id()));
////                throw new IllegalArgumentException("You must be in the provided alliance: " + offshoreAlliance.getId() + " to set the new ALLIANCE_ID for this offshore");
//            }
//
//            Set<Long> announceChannels = new HashSet<>();
//            Set<Long> serverIds = new HashSet<>();
//            if (nation.getNation_id() == Locutus.loader().getNationId()) {
//                announceChannels.add(Settings.INSTANCE.DISCORD.CHANNEL.ADMIN_ALERTS);
//            }
//
//            outer:
//            for (GuildDB other : Locutus.imp().getGuildDatabases().values()) {
//                if (other.isDelegateServer()) continue outer;
//
//                Set<Long> offshoreIds = other.getCoalitionRaw(Coalition.OFFSHORE);
//                if (offshoreIds.isEmpty()) continue;
//
//                for (int id : root.getAllianceIds()) {
//                    if (offshoreIds.contains((long) id)) {
//                        serverIds.add(other.getIdLong());
//                        continue outer;
//                    }
//                }
//                if (offshoreIds.contains(root.getIdLong())) {
//                    serverIds.add(other.getIdLong());
//                    continue outer;
//                }
//            }
//
//            Set<Integer> aaIds = root.getAllianceIds();
//            Set<Integer> toUnregister = new HashSet<>();
//
//            // check which ids are are set in offshore and offshoring coalition
//            Set<Integer> offshoringIds = root.getCoalition(Coalition.OFFSHORING);
//            Set<Integer> offshoreIds = root.getCoalition(Coalition.OFFSHORE);
//            for (int aaId : aaIds) {
//                if (offshoreIds.contains(aaId) && offshoringIds.contains(aaId)) {
//                    toUnregister.add(aaId);
//                }
//            }
//
//            if (!force) {
//                String title = "Change offshore to: " + offshoreAlliance.getName() + "/" + offshoreAlliance.getId();
//                StringBuilder body = new StringBuilder();
//                body.append("The alliances to this guild will be unregistered: `" + StringMan.getString(toUnregister) + "`\n");
//
//                body.append("The new alliance: `" + offshoreAlliance.getId() + " will be set ` (See: " + GuildKey.ALLIANCE_ID.getCommandMention() + ")\n");
//                body.append("All other guilds using the prior alliance `" + StringMan.getString(toUnregister) + "` will be changed to use the new offshore");
//
//                confirmButton.embed(title, body.toString()).send();
//                return null;
//            }
//
//            Set<Integer> newIds = new HashSet<>(aaIds);
//            newIds.removeAll(toUnregister);
//            newIds.add(offshoreAlliance.getAlliance_id());
//            GuildKey.ALLIANCE_ID.set(root, user, newIds);
//            root.addCoalition(offshoreAlliance.getAlliance_id(), Coalition.OFFSHORE);
//            root.addCoalition(offshoreAlliance.getAlliance_id(), Coalition.OFFSHORING);
//
//            for (Long serverId : serverIds) {
//                GuildDB db = Locutus.imp().getGuildDB(serverId);
//                if (db == null) continue;
//                db.addCoalition(offshoreAlliance.getAlliance_id(), Coalition.OFFSHORE);
//
//                // Find the most stuited channel to post the announcement in
//                MessageChannel channel = db.getResourceChannel(0);
//                if (channel == null) channel = db.getOrNull(GuildKey.ADDBALANCE_ALERT_CHANNEL);
//                if (channel == null) channel = db.getOrNull(GuildKey.DEPOSIT_ALERT_CHANNEL);
//                if (channel == null) channel = db.getOrNull(GuildKey.WITHDRAW_ALERT_CHANNEL);
//                if (channel == null) {
//                    List<NewsChannel> newsChannels = db.getGuild().getNewsChannels();
//                    if (!newsChannels.isEmpty()) channel = newsChannels.get(0);
//                }
//                if (channel == null) channel = (MessageChannel) db.getGuild().getDefaultChannel();
//                if (channel != null) {
//                    announceChannels.add(channel.getIdLong());
//                }
//            }
//
//            String title = "New Offshore";
//            String body = DNS.getMarkdownUrl(offshoreAlliance.getAlliance_id(), true);
//            for (Long announceChannel : announceChannels) {
//                GuildMessageChannel channel = Locutus.imp().getDiscordApi().getGuildChannelById(announceChannel);
//                if (channel != null) {
//                    try {
//                        DiscordUtil.createEmbedCommand(channel, title, body);
//                        Role adminRole = Roles.ECON.toRole(channel.getGuild());
//                        if (adminRole == null) {
//                            adminRole = Roles.ADMIN.toRole(channel.getGuild());
//                        }
//                        Member owner = channel.getGuild().getOwner();
//                        if (adminRole != null) {
//                            RateLimitUtil.queue((channel.sendMessage(adminRole.getAsMention())));
//                        } else if (owner != null) {
//                            RateLimitUtil.queue((channel.sendMessage(owner.getAsMention())));
//                        }
//                    } catch (InsufficientPermissionException ignore) {}
//                }
//            }
//
//            return "Done";
//        }
//
//        if (offshoreDB == null) {
//            return "No guild found for alliance: " + offshoreAlliance.getAlliance_id() + ". To register a guild to an alliance: " + CM.settings_default.registerAlliance.cmd.alliances(offshoreAlliance.getAlliance_id() + "");
//        }
//
//        OffshoreInstance currentOffshore = root.getOffshore();
//        if (currentOffshore != null) {
//            if (currentOffshore.getAllianceId() == offshoreAlliance.getAlliance_id()) {
//                return "That guild is already the offshore for this server";
//            }
//            Set<Integer> aaIds = root.getAllianceIds();
//
//            if (!force) {
//                String idStr = aaIds.isEmpty() ? root.getIdLong() + "" : StringMan.join(aaIds, ",");
//                String title = "Replace current offshore";
//                StringBuilder body = new StringBuilder();
//                body.append("Changing offshores will close the account with your previous offshore provider\n");
//                body.append("Your current offshore is set to: " + currentOffshore.getAllianceId() + "\n");
//                body.append("To check your funds with the current offshore, use " +
//                        CM.deposits.check.cmd.nationOrAllianceOrGuild(idStr));
//                body.append("\nIt is recommended to withdraw all funds from the current offshore before changing, as Locutus may not be able to access the account after closing it`");
//
//                confirmButton.embed(title, body.toString()).send();
//                return null;
//            }
//            for (int aaId : aaIds) {
//                offshoreDB.removeCoalition(aaId, Coalition.OFFSHORING);
//            }
//            offshoreDB.removeCoalition(root.getIdLong(), Coalition.OFFSHORING);
//
//            root.removeCoalition(currentOffshore.getAllianceId(), Coalition.OFFSHORE);
//            root.removeCoalition(currentOffshore.getGuildDB().getIdLong(), Coalition.OFFSHORE);
//        }
//
//        if (offshoreDB == root) {
//            if (!force) {
//                String title = "Designate " + offshoreAlliance.getName() + "/" + offshoreAlliance.getId() + " as the bank";
//                StringBuilder body = new StringBuilder();
//                body.append("Withdraw commands will use this alliance bank\n");
//                body.append("To have another alliance/corporation use this bank as an offshore:\n");
//                body.append("- You must be admin or econ on both discord servers\n");
//                body.append("- On the other guild, use: " + CM.offshore.add.cmd.offshoreAlliance(offshoreAlliance.getAlliance_id() + "") + "\n\n");
//                body.append("If this is an offshore, and you create a new alliance, you may use this command to set the new alliance (all servers offshoring here will be updated)");
//
//                confirmButton.embed(title, body.toString()).send();
//                return null;
//            }
//            root.addCoalition(offshoreAlliance.getAlliance_id(), Coalition.OFFSHORE);
//            root.addCoalition(offshoreAlliance.getAlliance_id(), Coalition.OFFSHORING);
//            return "Done! Set " + offshoreAlliance.getName() + "/" + offshoreAlliance.getId() + " as the designated bank for this server";
//        }
//
//        if (!offshoreDB.isOffshore()) {
//            return "No offshore found for alliance: " + offshoreAlliance.getAlliance_id() + ". Are you sure that's a valid offshore setup with locutus?";
//        }
//        Boolean enabled = offshoreDB.getOrNull(GuildKey.PUBLIC_OFFSHORING);
//        if (enabled != Boolean.TRUE && !Roles.ECON.has(user, offshoreDB.getGuild())) {
//            Role role = Roles.ECON.toRole(offshoreDB);
//            String roleName = role == null ? "ECON" : role.getName();
//            return "You do not have " + roleName + " on " + offshoreDB.getGuild() + ". Alternatively " + GuildKey.PUBLIC_OFFSHORING.getCommandMention() + " is not enabled on that guild.";
//        }
//
//        boolean hasAdmin = Roles.ECON.has(user, offshoreDB.getGuild());
//
//        StringBuilder response = new StringBuilder();
//        // check public offshoring is enabled
//        synchronized (OffshoreInstance.BANK_LOCK) {
//            Set<Integer> aaIds = root.getAllianceIds();
//
//            try {
//                root.addCoalition(offshoreAlliance.getAlliance_id(), Coalition.OFFSHORE);
//                if (aaIds.isEmpty()) {
//                    long id = root.getIdLong();
//                    offshoreDB.addCoalition(id, Coalition.OFFSHORING);
//                } else {
//                    for (int aaId : aaIds) {
//                        offshoreDB.addCoalition(aaId, Coalition.OFFSHORING);
//                    }
//                }
//
//
//                OffshoreInstance offshoreInstance = offshoreDB.getOffshore();
//                Map<NationOrAllianceOrGuild, double[]> depoByAccount = offshoreInstance.getDepositsByAA(root, f -> true, true);
//
//                boolean hasDepoToReset = depoByAccount.values().stream().anyMatch(depo -> !ResourceType.isZero(depo));
//                if (hasDepoToReset && !hasAdmin && importAccount) {
//                    throw new IllegalArgumentException("Missing " + Roles.ADMIN.toDiscordRoleNameElseInstructions(offshoreDB.getGuild()));
//                }
//
//                if (hasDepoToReset && !newAccount && !importAccount) {
//                    String title = "Reset Balance for " + root.getName();
//                    StringBuilder body = new StringBuilder("**__!! WARNING !!__**\n");
//                    if (hasAdmin) {
//                        body.append("Press `New` to make a new account with the offshore\n");
//                        body.append("Press `Import` to import your previous account holdings\n");
//                    } else {
//                        body.append("> This will open a new account and reset your balance with the offshore.\nTo import your previous account holdings, please contact an administrator of the offshore");
//                        body.append("\n\n**Offshore Owner:** <@" + offshoreDB.getGuild().getOwnerId() + ">\n");
//                    }
//                    body.append("**Offshore:** " + offshoreDB.getGuild().toString() + " | " + offshoreAlliance.getMarkdownUrl() + "\n");
//                    for (Map.Entry<NationOrAllianceOrGuild, double[]> entry : depoByAccount.entrySet()) {
//                        NationOrAllianceOrGuild account = entry.getKey();
//                        double[] depo = entry.getValue();
//                        body.append("**Resetting Accounts:**\n");
//                        if (!ResourceType.isZero(depo)) {
//                            body.append("- " + account.getQualifiedId() + " - worth: `~$" + MathMan.format(ResourceType.convertedTotal(depo)) + "`\n");
//                            body.append(" - Balance: `" + ResourceType.resourcesToString(depo) + "`\n");
//                        }
//                    }
//
//                    JSONObject newAccountCmd = new JSONObject(command).put("force", "true");
//
//                    IMessageBuilder msg = io.create().embed(title, body.toString());
//                    msg = msg.commandButton(newAccountCmd.put("newAccount", true), "New");
//                    if (hasAdmin) {
//                        JSONObject importAccountCmd = new JSONObject(command).put("force", "true");
//                        msg = msg.commandButton(importAccountCmd.put("importAccount", true), "Import");
//                    }
//                    msg.send();
//                    return null;
//                }
//
//                for (Map.Entry<NationOrAllianceOrGuild, double[]> entry : depoByAccount.entrySet()) {
//                    NationOrAllianceOrGuild account = entry.getKey();
//                    if (!importAccount) {
//                        double[] depo = entry.getValue();
//                        if (!ResourceType.isZero(depo)) {
//                            long tx_datetime = System.currentTimeMillis();
//                            long receiver_id = 0;
//                            int receiver_type = 0;
//                            int banker = nation.getNation_id();
//
//                            String note = "#deposit";
//                            double[] amount = depo;
//                            for (int i = 0; i < amount.length; i++) amount[i] = -amount[i];
//                            if (!ResourceType.isZero(amount)) {
//                                if (account.isGuild()) {
//                                    offshoreDB.addTransfer(tx_datetime, account.asGuild().getIdLong(), account.getReceiverType(), receiver_id, receiver_type, banker, note, amount);
//                                } else {
//                                    offshoreDB.addTransfer(tx_datetime, account.asAlliance(), receiver_id, receiver_type, banker, note, amount);
//                                }
//                            }
//
//
//                            MessageChannel output = offshoreDB.getResourceChannel(0);
//                            if (output != null) {
//                                String msg = "Added " + ResourceType.resourcesToString(amount) + " to " + account.getTypePrefix() + ":" + account.getName() + "/" + account.getIdLong();
//                                RateLimitUtil.queue(output.sendMessage(msg));
//                                response.append("Reset deposit for " + root.getGuild() + "\n");
//                            }
//                        }
//                    }
//
//                    response.append("Registered " + offshoreAlliance.getQualifiedId() + " as an offshore. See: https://github.com/xdnw/diplocutus/wiki/banking");
//                    if (aaIds.isEmpty()) {
//                        response.append("\n(Your guild id, and the id of your account with the offshore is `" + root.getIdLong() + "`)");
//                    }
//                    if (root.getOrNull(GuildKey.WAR_ALERT_FOR_OFFSHORES) == null) {
//                        if (offshoreDB.getOrNull(GuildKey.PUBLIC_OFFSHORING) == Boolean.TRUE) {
//                            GuildKey.WAR_ALERT_FOR_OFFSHORES.set(root, user, false);
//                            response.append("\nNote: Offshore War alerts are disabled. Enable using: " + GuildKey.WAR_ALERT_FOR_OFFSHORES.getCommandObj(root, true));
//                        } else {
//                            response.append("\nNote: Disable offshore war alerts using: " + GuildKey.WAR_ALERT_FOR_OFFSHORES.getCommandObj(root, false));
//                        }
//                    }
//                }
//
//
//
//            } catch (Throwable e) {
//                root.removeCoalition(offshoreAlliance.getAlliance_id(), Coalition.OFFSHORE);
//                for (int aaId : aaIds) {
//                    offshoreDB.removeCoalition(aaId, Coalition.OFFSHORING);
//                }
//                throw e;
//            }
//        }
//
//        response.append("\nDone!");
//        return response.toString();
//    }
}
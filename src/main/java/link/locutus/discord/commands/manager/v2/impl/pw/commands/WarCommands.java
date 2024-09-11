package link.locutus.discord.commands.manager.v2.impl.pw.commands;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import link.locutus.discord.Locutus;
import link.locutus.discord.api.generated.AllianceMemberInventory;
import link.locutus.discord.api.generated.InventoryType;
import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.api.generated.TreatyType;
import link.locutus.discord.api.types.Building;
import link.locutus.discord.api.types.MilitaryUnit;
import link.locutus.discord.api.types.MilitaryUnitType;
import link.locutus.discord.api.types.tx.Transaction2;
import link.locutus.discord.commands.WarCategory;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.IsAlliance;
import link.locutus.discord.commands.manager.v2.impl.pw.binding.NationAttributeDouble;
import link.locutus.discord.api.ApiKeyPool;
import link.locutus.discord.commands.manager.v2.binding.annotation.Arg;
import link.locutus.discord.commands.manager.v2.command.IMessageBuilder;
import link.locutus.discord.commands.manager.v2.command.IMessageIO;
import link.locutus.discord.commands.manager.v2.impl.discord.DiscordChannelIO;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.commands.manager.v2.binding.annotation.Command;
import link.locutus.discord.commands.manager.v2.binding.annotation.Default;
import link.locutus.discord.commands.manager.v2.binding.annotation.Filter;
import link.locutus.discord.commands.manager.v2.binding.annotation.Me;
import link.locutus.discord.commands.manager.v2.binding.annotation.Range;
import link.locutus.discord.commands.manager.v2.binding.annotation.Switch;
import link.locutus.discord.commands.manager.v2.binding.annotation.TextArea;
import link.locutus.discord.commands.manager.v2.binding.annotation.Timediff;
import link.locutus.discord.commands.manager.v2.binding.annotation.Timestamp;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.CoalitionPermission;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.RolePermission;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.WhitelistPermission;
import link.locutus.discord.config.Messages;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.NationDB;
import link.locutus.discord.db.entities.*;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.components.NationPrivate;
import link.locutus.discord.db.guild.GuildKey;
import link.locutus.discord.db.guild.SheetKey;
import link.locutus.discord.gpt.GPTUtil;
import link.locutus.discord.pnw.*;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.*;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.discord.DiscordUtil;
import link.locutus.discord.util.io.PagePriority;
import link.locutus.discord.util.scheduler.CachedFunction;
import link.locutus.discord.util.sheet.SheetUtil;
import link.locutus.discord.util.sheet.SpreadSheet;
import link.locutus.discord.util.task.war.WarCard;
import link.locutus.discord.api.types.Rank;
import link.locutus.discord.util.update.LeavingBeigeAlert;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Collectors;

public class WarCommands {

    @Command(desc = "Allow receiving automatic beige alerts a certain nation score below your current war range")
    @WhitelistPermission
    @IsAlliance
    public String setBeigeAlertScoreLeeway(@Me DBNation me,
                                           @Range(min=0) double scoreLeeway) {
        me.setMeta(NationMeta.PROTECTION_ALERT_SCORE_LEEWAY, scoreLeeway);
        return "Set beige alert score leeway to " + MathMan.format(scoreLeeway) + "ns" + "\nSee also:" + CM.alerts.beige.test_auto.cmd.toSlashMention();
    }

    @Command(desc = "Set the required amount of loot for automatic beige alerts\n" +
            "Defaults to $15m", aliases = {"beigeAlertRequiredLoot", "setBeigeAlertRequiredLoot"})
    @WhitelistPermission
    public String beigeAlertRequiredLoot(@Me DBNation me,
                                         @Arg("Require the target to have at least this much estimated loot\n" +
                                                 "Resources are valued at weekly market average prices")
                                         double requiredLoot) {
        me.setMeta(NationMeta.PROTECTION_ALERT_REQUIRED_LOOT, requiredLoot);
        return "Set beige alert required loot to $" + MathMan.format(requiredLoot) + "\nSee also:" + CM.alerts.beige.test_auto.cmd.toSlashMention();
    }

    @Command(desc = "Set the types of nations to receive automatic beige alerts for", aliases = {"beigeAlertMode", "setBeigeAlertMode"})
    @WhitelistPermission
    @RolePermission(value = {Roles.PROTECTION_ALERT, Roles.PROTECTION_ALERT_OPT_OUT}, any = true)
    @IsAlliance
    public static String beigeAlertMode(@Me User user, @Me DBNation me, NationMeta.ProtectionAlertMode mode) {
        me.setMeta(NationMeta.PROTECTION_ALERT_MODE, (byte) mode.ordinal());
        if (mode == NationMeta.ProtectionAlertMode.NO_ALERTS) {
            Set<DBNation> reminders = Locutus.imp().getNationDB().getBeigeRemindersByAttacker(me);
            for (DBNation nation : reminders) {
                Locutus.imp().getNationDB().deleteBeigeReminder(me.getNation_id(), nation.getNation_id());
            }
        }
        StringBuilder response = new StringBuilder("Set beige alert mode to " + mode + " via " + CM.alerts.beige.beigeAlertMode.cmd.toSlashMention());
        if (mode != NationMeta.ProtectionAlertMode.NO_ALERTS) {
            for (Guild guild : user.getMutualGuilds()) {
                Role role = Roles.PROTECTION_ALERT_OPT_OUT.toRole(guild);
                Member member = guild.getMember(user);
                if (role != null && member != null && member.getRoles().contains(role)) {
                    try {
                        RateLimitUtil.queue(guild.removeRoleFromMember(user, role));
                        response.append("\nRemoved ").append(role.getName()).append(" from ").append(guild.getName());
                    } catch (Exception e) {
                        response.append("\nFailed to remove ").append(role.getName()).append(" from ").append(guild.getName() + " (" + e.getMessage() + ")");
                        e.printStackTrace();
                    }
                }
            }
        }
        return response.toString() + "\n\nSee also:" + CM.alerts.beige.test_auto.cmd.toSlashMention();
    }

    @Command(desc = "View and test that the current automatic beige alert settings allow for notification")
    @WhitelistPermission
    @RolePermission(value = {Roles.PROTECTION_ALERT, Roles.PROTECTION_ALERT_OPT_OUT}, any = true)
    @IsAlliance
    public String testBeigeAlertAuto(@Me GuildDB db, @Me DBNation me, @Me Member member) {
        NationMeta.ProtectionAlertMode mode = me.getBeigeAlertMode(NationMeta.ProtectionAlertMode.NO_ALERTS);
        if (mode == NationMeta.ProtectionAlertMode.NO_ALERTS) {
            return "Please enable via: " + CM.alerts.beige.beigeAlertMode.cmd.toSlashMention();
        }
        NationMeta.BeigeAlertRequiredStatus requiredStatus = me.getBeigeRequiredStatus(NationMeta.BeigeAlertRequiredStatus.ONLINE);
        if (!requiredStatus.getApplies().test(member)) {
            return "You are not online on discord. You can change this via: " + CM.alerts.beige.beigeAlertRequiredStatus.cmd.toSlashMention();
        }
        Role role = Roles.PROTECTION_ALERT.toRole(db);
        Role optOut = Roles.PROTECTION_ALERT_OPT_OUT.toRole(db);
        Set<Integer> allianceIds = db.getAllianceIds();
        try {
            LeavingBeigeAlert.testBeigeAlertAuto(db, member, role, optOut, allianceIds, true, false);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
        StringBuilder result = new StringBuilder();
        result.append("**Result: Success**\n");
        result.append("- require-role:`").append(role == null ? "N/A" : role.getName()).append("`=").append(member.getRoles().contains(role)).append("\n");
        result.append("- optout:`").append(optOut == null ? "N/A" : optOut.getName()).append("`=").append(member.getRoles().contains(optOut)).append("\n");
        result.append("- mode:`").append(me.getBeigeAlertMode(null)).append("` (default:`NO_ALERTS`)\n");
        result.append("- score-leway:`").append(me.getMeta(NationMeta.PROTECTION_ALERT_SCORE_LEEWAY)).append("` (default:`0ns`)\n");
        result.append("- required-loot:`").append(me.getMeta(NationMeta.PROTECTION_ALERT_REQUIRED_LOOT)).append("` (default:`$15m`)\n");
        result.append("- required-status:`").append(requiredStatus.name()).append("` (default:`ONLINE`)\n");
        return result.toString();
    }

    @Command(desc = "Only get the automatic beige alerts if you have the online status on discord\n" +
            "Note: You will still receive alerts for targets you have subscribed to via `{prefix}alerts beige beigereminders`",
            aliases = {"beigeAlertRequiredStatus", "setBeigeAlertRequiredStatus"})
    @WhitelistPermission
    @IsAlliance
    public String beigeAlertRequiredStatus(@Me DBNation me, NationMeta.BeigeAlertRequiredStatus status) {
        me.setMeta(NationMeta.PROTECTION_ALERT_REQUIRED_STATUS, (byte) status.ordinal());
        return "Set beige alert required status to " + status + "\nSee also:" + CM.alerts.beige.test_auto.cmd.toSlashMention();
    }

    @Command(desc = "List your current beige reminders", aliases = {"beigeReminders", "listBeigeReminders"})
    @WhitelistPermission
    @IsAlliance
    public String beigeReminders(@Me DBNation me) {
        Set<DBNation> reminders = Locutus.imp().getNationDB().getBeigeRemindersByAttacker(me);
        if (reminders.isEmpty()) return "You have no beige reminders set.";

        StringBuilder response = new StringBuilder();
        response.append("**" + me.getNation() + "**").append(me.toMarkdown()).append("\n**Reminders**\n");
        for (DBNation target : reminders) {
            response.append(target.toMarkdown()).append('\n');
        }
        return response.toString();
    }

    @Command(desc = "Remove your beige reminders", aliases = {"removeBeigeReminder", "deleteBeigeReminder"})
    public String removeBeigeReminder(@Me DBNation me, Set<DBNation> nationsToRemove) {
        Set<DBNation> reminders = Locutus.imp().getNationDB().getBeigeRemindersByAttacker(me);
        Set<DBNation> toRemove = new HashSet<>();
        for (DBNation nation : nationsToRemove) {
            if (reminders.contains(nation)) toRemove.add(nation);
        }

        if (toRemove.isEmpty()) return "No nations selected for removal. For a list of your current reminders, use " + CM.alerts.beige.beigeReminders.cmd.toSlashMention() + "";

        StringBuilder response = new StringBuilder();
        for (DBNation nation : toRemove) {
            Locutus.imp().getNationDB().deleteBeigeReminder(me.getNation_id(), nation.getNation_id());
            response.append("Removed reminder for <" + nation.getUrl() + ">\n");
        }
        return response.toString();
    }

    @Command(desc = "Set a reminder for when a nation leaves beige or VM", aliases = {"beigeAlert", "setAlert", "beigeReminder", "setBeigeReminder", "addBeigeReminder"})
    @WhitelistPermission
    @IsAlliance
    @RolePermission(Roles.PROTECTION_ALERT)
    public String beigeReminder(@Me GuildDB db, @Me DBNation me,
                                @Filter("*,#color=beige,#vm_turns=0,#isinwarrange=1|*,#vm_turns>0,#vm_turns<168,#isinwarrange=1") Set<DBNation> targets,
                                @Arg("Allow targets this much ns below your score range")
                                @Switch("s") boolean allowOutOfScore) {
        // Check db can do beige alerts
        LeavingBeigeAlert.testBeigeAlert(db, true);

        Function<DBNation, Boolean> canRaid = db.getCanRaid();

        if (!allowOutOfScore) {
            double score = me.getScore();
            ByteBuffer scoreLeewayBuf = me.getMeta(NationMeta.PROTECTION_ALERT_SCORE_LEEWAY);
            double scoreLeeway = scoreLeewayBuf == null ? 0 : scoreLeewayBuf.getDouble();
            targets.removeIf(f -> {
                double minModifier = f.isInactiveForWar() ? DNS.WAR_RANGE_MIN_MODIFIER_INACTIVE : DNS.WAR_RANGE_MIN_MODIFIER_ACTIVE;
                double maxModifier = f.isInactiveForWar() ? DNS.WAR_RANGE_MAX_MODIFIER_INACTIVE : DNS.WAR_RANGE_MAX_MODIFIER_ACTIVE;
                return f.getScore() < score * minModifier - scoreLeeway || f.getScore() > score * maxModifier;
            });
        }

        targets.removeIf(f -> !canRaid.apply(f));
        targets.removeIf(f -> !f.hasProtection() || f.isVacation());

        if (targets.isEmpty()) {
            return "No suitable targets found. Are you sure you specified a nation you are allowed to raid (see guild DNR) that is currently in beige and within your score range?";
        }

        StringBuilder response = new StringBuilder();

        GuildDB myDB = Locutus.imp().getGuildDBByAA(me.getAlliance_id());
        if (myDB == null) {
            return "Your alliance: " + me.getAllianceName() + " has no guild registered with this bot.";
        }
        if (myDB != db) {
            return "You are not in the same guild as your alliance: " + myDB.getGuild().toString() + " != " + db.getGuild().toString();
        }

        for (DBNation target : targets) {
            long time = target.getProtectionRemainingMs();
            long diff = time - System.currentTimeMillis();
            String diffStr = TimeUtil.secToTime(TimeUnit.MILLISECONDS, diff);

            if (diff < TimeUnit.MINUTES.toMillis(6)) {
                response.append(target.getUrl() + " leaves beige next turn  (in " + diffStr + ")- NO REMINDER SET\n");
                continue;
            }


            Locutus.imp().getNationDB().addBeigeReminder(target, me);
            response.append("Added beige reminder for " + target.getUrl() + " (in " + diffStr + ")\n");
            try {

                LeavingBeigeAlert.testBeigeAlert(db, target, me, null, true, false, false, false);
            } catch (IllegalArgumentException e) {
                response.append("- " + e.getMessage() + ": <" + target.getUrl() + ">)\n");
            }
        }

        if (me.getOff() >= me.getMaxOff()) {
            response.append("`note: You are currently at max offensives and may not receive alerts`\n");
        }

        response.append("\nSee also:\n" +
                "- " + CM.alerts.beige.beigeReminders.cmd.toSlashMention() + "\n" +
                "- " + CM.alerts.beige.removeBeigeReminder.cmd.toSlashMention() + "\n" +
                "- " + CM.alerts.beige.beigeAlertRequiredStatus.cmd.toSlashMention() + "\n" +
                "- " + CM.alerts.beige.beigeAlertMode.cmd.toSlashMention() + "\n" +
                "- " + CM.alerts.beige.beigeAlertRequiredLoot.cmd.toSlashMention() + "\n" +
                "- " + CM.alerts.beige.setBeigeAlertScoreLeeway.cmd.toSlashMention() + "");

        return response.toString();
    }

    // TODO FIXME :||remove raid cmd !!later-important
//    @Command(desc = "Find targets to raid\n" +
//            "Sorted by best nation loot\n" +
//            "Defaults to 7d inactive")
//    @RolePermission(value = {Roles.MEMBER, Roles.APPLICANT}, any=true)
//    public String raid(@Me DBNation me, @Me GuildDB db, @Me Guild guild, @Me User user, @Me IMessageIO channel,
//                       @Default("*") Set<DBNation> targets,
//                       @Switch("r") @Default("5") Integer numResults,
//                       @Switch("a") @Timediff Long activeTimeCutoff,
//                       @Switch("w") boolean weakground,
//                       @Switch("b") Integer beigeTurns,
//                       @Switch("v") Integer vmTurns,
//                       @Switch("n") Double nationScore,
//                       @Switch("s") Integer defensiveSlots,
//                       @Switch("d") boolean ignoreDNR,
//                       @Switch("l") boolean ignoreBankLoot,
//                       @Switch("c") boolean ignoreCityRevenue) throws ExecutionException, InterruptedException {
//
//        DBNation nation = DiscordUtil.getNation(user);
//        if (nation == null) return null;
//
//        boolean dms = false;
//
//        RaidCommand cmd = new RaidCommand();
//        Set<DBNation> allNations = new LinkedHashSet<>(Locutus.imp().getNationDB().getNations().values());
//        if (vmTurns == null) vmTurns = 0;
//        if (defensiveSlots == null) defensiveSlots = -1;
//        boolean active = activeTimeCutoff != null && activeTimeCutoff <= 60;
//        long minutesInactive = activeTimeCutoff == null ? 10000 : TimeUnit.MILLISECONDS.toMinutes(activeTimeCutoff);
//        double score = nationScore == null ? me.getScore() : nationScore;
//
//        if (nationScore != null && !Roles.MILCOM.has(user, guild)) {
//            return "You do not have permission to specify a score";
//        }
//        Set<Integer> ignoreAlliances = new HashSet<>();
//        boolean includeAlliances = false;
//        double minLoot = Double.NEGATIVE_INFINITY;
//        if (numResults == null) numResults = 5;
//        if (beigeTurns == null) beigeTurns = -1;
//
//        String result = cmd.onCommand2(channel, user, db, me, targets, allNations, weakground, dms, vmTurns, defensiveSlots, beigeTurns != null && beigeTurns > 0, !ignoreDNR, ignoreAlliances, includeAlliances, active, minutesInactive, score, minLoot, beigeTurns, ignoreBankLoot, ignoreCityRevenue, numResults);
//        return result;
//    }


    // TODO FIXME :||remove intel
//    private static Map<Integer, Long> alreadySpied = new ConcurrentHashMap<>();
//    @Command(desc = "Find nations to gather intel on (sorted by infra * days since last intel)")
//    @RolePermission(Roles.MEMBER)
//    public String intel(@Me IMessageIO channel, @Me GuildDB db, @Me DBNation me,
//                        @Arg("Exclude nations in the top X alliances (or direct allies)")
//                        @Default Integer dnrTopX,
//                        @Arg("If the alliance Do Not Raid settings are ignore")
//                        @Switch("d") boolean ignoreDNR,
//                        @Arg("The nation gathering intel")
//                        @Switch("n") DBNation attacker,
//                        @Arg("The score range of the nation gathering intel")
//                        @Switch("s") Double score) {
//        DBNation finalNation = attacker == null ? me : attacker;
//        double finalScore = score == null ? finalNation.getScore() : score;
//        if (dnrTopX == null) {
//            dnrTopX = db.getOrNull(GuildKey.DO_NOT_RAID_TOP_X);
//            if (dnrTopX == null) dnrTopX = 0;
//        }
//
//        List<DBNation> enemies = new ArrayList<>(Locutus.imp().getNationDB().getNations().values());
//
//        Set<Integer> allies = db.getAllies(true);
//
//        Function<DBNation, Boolean> raidList = db.getCanRaid(dnrTopX, true);
//        Set<Integer> enemyCoalitions = db.getCoalition("enemies");
//        Set<Integer> targetCoalitions = db.getCoalition("targets");
//
//        if (!ignoreDNR) {
//            enemies.removeIf(f -> !raidList.apply(f));
//        }
//
//        enemies.removeIf(f -> allies.contains(f.getAlliance_id()));
//        enemies.removeIf(f -> f.active_m() < 4320);
//        enemies.removeIf(f -> f.isVacation());
//        enemies.removeIf(f -> f.isBeige());
//        if (finalNation.getCities() > 3) enemies.removeIf(f -> f.getCities() < 4 || f.getScore() < 500);
//        enemies.removeIf(f -> f.getDef() == 3);
//        enemies.removeIf(nation ->
//                nation.active_m() < 12000 &&
//                        nation.getGroundStrength(true, false) > finalNation.getGroundStrength(true, false) &&
//                        nation.getAircraft() > finalNation.getAircraft() &&
//                        nation.getShips() > finalNation.getShips() + 2);
//        long cutoff = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30);
//        enemies.removeIf(f -> alreadySpied.getOrDefault(f.getNation_id(), 0L) > cutoff);
//
//        if (false) {
//            Set<DBNation> myAlliance = Locutus.imp().getNationDB().getNations(Collections.singleton(finalNation.getAlliance_id()));
//            myAlliance.removeIf(f -> f.active_m() > 2440 || f.isVacation());
//            BiFunction<Double, Double, Integer> range = DNS.getIsNationsInScoreRange(myAlliance);
//            enemies.removeIf(f -> range.apply(f.getScore() / DNS.WAR_RANGE_MAX_MODIFIER, f.getScore() / 0.75) <= 0);
//        } else {
//            List<DBNation> tmp = new ArrayList<>(enemies);
//            tmp.removeIf(f -> f.getScore() < finalScore * 0.75 || f.getScore() > finalScore * DNS.WAR_RANGE_MAX_MODIFIER);
//            if (tmp.isEmpty()) {
//                enemies.removeIf(f -> !f.isInSpyRange(finalNation));
//            } else {
//                enemies = tmp;
//            }
//
//        }
//
//        List<Map.Entry<DBNation, Double>> noData = new ArrayList<>();
//        List<Map.Entry<DBNation, Double>> outDated = new ArrayList<>();
//
//        for (DBNation enemy : enemies) {
//            Map.Entry<Double, Boolean> opValue = enemy.getIntelOpValue();
//            if (opValue != null) {
//                List<Map.Entry<DBNation, Double>> list = opValue.getValue() ? outDated : noData;
//                list.add(new AbstractMap.SimpleEntry<>(enemy, opValue.getKey()));
//            }
//        }
//
//        Collections.sort(noData, (o1, o2) -> Double.compare(o2.getValue(), o1.getValue()));
//        Collections.sort(outDated, (o1, o2) -> Double.compare(o2.getValue(), o1.getValue()));
//        noData.addAll(outDated);
//        for (Map.Entry<DBNation, Double> entry : noData) {
//            DBNation nation = entry.getKey();
//            alreadySpied.put(nation.getNation_id(), System.currentTimeMillis());
//
//            String title = "Gather Intelligence for: " + me.getNation();
//            String response = nation.toEmbedString();
//            response += "\n1 spy on extremely covert: ";
//            response += "\n*Please post the result of your spy report here*";
//            response += "\nMore info: https://docs.google.com/document/d/1gEeSOjjSDNBpKhrU9dhO_DN-YM3nYcklYzSYzSqq8k0";
//            channel.create().embed(title, response).send();
//            return null;
//        }
//        return "No results found";
//    }

    public static List<Map.Entry<DBNation, Double>> getCounterChance(GuildDB db, Set<DBNation> targets,
                                                                     @Switch("r") @Default("10") @Range(min=1, max=25) Integer numResults,
                                                                     @Switch("d") boolean ignoreDNR,
                                                                     @Switch("a") boolean includeAllies,
                                                                     @Switch("n") Set<DBNation> nationsToBlitzWith,
                                                                     @Switch("s") @Default("1.2") Double maxRelativeTargetStrength,
                                                                     @Switch("c") @Default("1.2") Double maxRelativeCounterStrength,
                                                                     @Switch("w") boolean withinAllAttackersRange,
                                                                     @Switch("o") boolean ignoreODP,
                                                                     @Switch("f") boolean force) {
        if (ignoreODP && !includeAllies) {
            throw new IllegalArgumentException("Cannot use `ignoreODP` when `includeAllies` is false");
        }
        if (nationsToBlitzWith.stream().anyMatch(f -> f.active_m() > 7200 || f.isVacation()) && !force) {
            throw new IllegalArgumentException("You can't blitz with nations that are inactive or VM. Add `force: True` to bypass");
        }
        BiFunction<Double, Double, Integer> attScores = DNS.getIsNationsInScoreRange(nationsToBlitzWith);

//        double minScore = me.getScore() * 0.75;
//        double maxScore = me.getScore() * PW.WAR_RANGE_MAX_MODIFIER;
        List<DBNation> nations = new ArrayList<>(targets);
        nations.removeIf(f -> f.isVacation());
        nations.removeIf(f -> f.getDef() >= 3);
        nations.removeIf(f -> f.hasProtection());
        if (withinAllAttackersRange) {
            if (nationsToBlitzWith == null) {
                throw new IllegalArgumentException("Please provide a list of nations for `nationsToBlitzWith`");
            }
            double minScore = nationsToBlitzWith.stream().mapToDouble(DBNation::getScore).max().orElse(0) * DNS.WAR_RANGE_MIN_MODIFIER_ACTIVE;
            double maxScore = nationsToBlitzWith.stream().mapToDouble(DBNation::getScore).min().orElse(0) * DNS.WAR_RANGE_MAX_MODIFIER_ACTIVE;
            if (minScore >= maxScore) {
                throw new IllegalArgumentException("Nations `nationsToBlitzWith` do not share a score range.");
            }
            nations.removeIf(f -> f.getScore() < minScore || f.getScore() > maxScore);
        } else {
            nations.removeIf(f -> attScores.apply(f.getScore() / DNS.WAR_RANGE_MAX_MODIFIER_ACTIVE, f.getScore() * DNS.WAR_RANGE_MAX_MODIFIER_ACTIVE) <= 0);
        }

        if (!ignoreDNR) {
            Function<DBNation, Boolean> dnr = db.getCanRaid();
            nations.removeIf(f -> !dnr.apply(f));
        }

        Set<Integer> aaIds = new HashSet<>();
        for (DBNation nation : nations) {
            if (nation.active_m() < 10000 && nation.getPosition() >= Rank.MEMBER.id) {
                aaIds.add(nation.getAlliance_id());
            }
        }

        Map<Integer, List<DBNation>> countersByAlliance = new HashMap<>();

        int maxCounterSize = nationsToBlitzWith.size() * 3;
        for (DBNation nation : nationsToBlitzWith) {
            maxCounterSize -= nation.getDef();
        }
        for (Integer aaId : aaIds) {
            List<DBNation> canCounter = new ArrayList<>();
            DBAlliance alliance = DBAlliance.getOrCreate(aaId);
            Set<DBAlliance> alliances = new HashSet<>(Arrays.asList(alliance));
            if (includeAllies) {
                Set<DBAlliance> allies;
                if (ignoreODP) {
                    allies = alliance.getTreatiedAllies(f -> !f.isOptional() && f.isDefensive());
                } else {
                    allies = alliance.getTreatiedAllies();
                }
                alliances.addAll(allies);
            }
            for (DBAlliance ally : alliances) {
                canCounter.addAll(ally.getNations(true, 10000, true));
            }

            canCounter.removeIf(f -> f.isVacation());
            canCounter.removeIf(f -> f.active_m() > 4880);
            canCounter.removeIf(f -> attScores.apply(f.getScore() * DNS.WAR_RANGE_MIN_MODIFIER_ACTIVE, f.getScore() * DNS.WAR_RANGE_MAX_MODIFIER_ACTIVE) <= 0);
            canCounter.removeIf(f -> f.getOff() >= f.getMaxOff());
            canCounter.removeIf(f -> f.getNumWars() > 0 && f.getRelativeStrength() < 1);

            Collections.sort(canCounter, new Comparator<DBNation>() {
                @Override
                public int compare(DBNation o1, DBNation o2) {
                    return Double.compare(o2.getWarIndex(), o1.getWarIndex());
                }
            });
            if (canCounter.size() > maxCounterSize) canCounter = canCounter.subList(0, maxCounterSize);
            countersByAlliance.put(aaId, canCounter);
        }

        Map<DBNation, Double> strength = new HashMap<>();
        List<Map.Entry<DBNation, Double>> counterChance = new ArrayList<>();
        for (DBNation nation : nations) {
            if (nation.active_m() > 2880) {
                if (nation.lostInactiveWar() || nation.getAlliance_id() == 0) {
                    strength.put(nation, Math.pow(nation.getWarIndex(), 3) * 0.44);
                    continue;
                }
                if (nation.getPosition() == Rank.APPLICANT.id) {
                    strength.put(nation, Math.pow(nation.getWarIndex(), 3) * Math.max(0, 0.8 - 0.1 * nation.active_m() / 1440d));
                    continue;
                }
                strength.put(nation, Math.pow(nation.getWarIndex(), 3) * Math.max(0, 0.8 - 0.1 * nation.active_m() / 1440d));
                continue;
            }
            if (nation.getAlliance_id() == 0) {
                strength.put(nation, Math.pow(nation.getWarIndex(), 3) * 0.66);
                continue;
            }
            if (nation.getDef() > 0 && nation.getRelativeStrength(false) < 1) {
                strength.put(nation, Math.pow(nation.getWarIndex(), 3) * 0.33);
                continue;
            }
            strength.put(nation, Math.pow(nation.getWarIndex(), 3));
        }
         for (DBNation nation : nations) {
             double counterStrength = 0;
             double inactive0 = 0;
             double inactive1 = 0;
             double inactive2 = 0;
             if (nation.getAlliance_id() != 0) {
                 List<DBNation> counters = countersByAlliance.get(nation.getAlliance_id());
                 if (counters != null) {
                     counters = new ArrayList<>(counters);
                     counters.remove(nation);
                     int i = 0;

                     for (DBNation other : counters) {
                         if (other.getId() == nation.getId()) continue;
                         if (i++ >= maxCounterSize) break;
                         if (other.active_m() > 2880) {
                             inactive0 += (1 + ((other.active_m() - 2880d) / 1440d));
                         } else if (other.active_m() > 1440) {
                             inactive1 += (1 + (other.active_m() - 1440d) / 1440d);
                         } else {
                             inactive2 += (1 + (other.active_m()) / 1440d);
                         }
                         counterStrength += Math.pow(other.getWarIndex(), 3);
                     }
                 }
             }
             double logistics = inactive0 * 2 + inactive1 * 1 + inactive2 * 0.5;
             if (logistics > 1) {
                 counterStrength = counterStrength * Math.pow(logistics, 0.95);
             }
             counterStrength += strength.get(nation) * (Math.pow(0.85, Math.min(3, nationsToBlitzWith.size())) / 0.85);
             counterChance.add(new AbstractMap.SimpleEntry<>(nation, counterStrength));
         }

        // nationsToBlitzWith foreach nation.getStrength();
        double myStrength = nationsToBlitzWith.stream().mapToDouble(f -> Math.pow(f.getWarIndex(), 3)).sum();

        if (maxRelativeCounterStrength != null) {
            counterChance.removeIf(f -> f.getKey().getWarIndex() > myStrength * maxRelativeCounterStrength);
            counterChance.removeIf(f -> strength.getOrDefault(f.getKey(), 0d) > myStrength * maxRelativeTargetStrength);
        }

        if (counterChance.isEmpty()) {
            return Collections.emptyList();
        }

        Map<DBNation, Double> valueWeighted = new HashMap<>();
        for (Map.Entry<DBNation, Double> entry : counterChance) {
            valueWeighted.put(entry.getKey(), entry.getValue());
        }
        Collections.sort(counterChance, new Comparator<Map.Entry<DBNation, Double>>() {
            @Override
            public int compare(Map.Entry<DBNation, Double> o1, Map.Entry<DBNation, Double> o2) {
                return Double.compare(valueWeighted.get(o1.getKey()), valueWeighted.get(o2.getKey()));
            }
        });
        return counterChance;
    }

    @Command(desc = "Find nations who aren't protected, or are in an alliance unable to provide suitable counters\n" +
            "Not suitable if you have no military")
    @RolePermission(Roles.MEMBER)
    public String unprotected(@Me GuildDB db, Set<DBNation> targets, @Me DBNation me,
                              @Switch("r") @Default("10") @Range(min=1, max=25) Integer numResults,
                              @Arg("Ignore the configured Do Not Raid list")
                              @Switch("d") boolean ignoreDNR,
                              @Arg("Include allies for finding suitable counters")
                              @Switch("a") boolean includeAllies,
                              @Arg("The nations attacking\n" +
                               "Defaults to your nation")
                              @Switch("n") Set<DBNation> nationsToBlitzWith,
                              @Arg("The maximum allowed military strength of the target nation relative to you")
                              @Switch("s") @Default("1.2") Double maxRelativeTargetStrength,
                              @Arg("The maximum allowed military strength of counters relative to you")
                              @Switch("c") @Default("1.2") Double maxRelativeCounterStrength,
                              @Arg("Only list targets within range of ALL attackers")
                              @Switch("w") boolean withinAllAttackersRange,
                              @Switch("o") boolean ignoreODP,
                              @Switch("f") boolean force
    ) {

        if (nationsToBlitzWith == null) nationsToBlitzWith = Collections.singleton(me);

        List<Map.Entry<DBNation, Double>> counterChance = getCounterChance(db, targets, numResults, ignoreDNR, includeAllies, nationsToBlitzWith, maxRelativeTargetStrength, maxRelativeCounterStrength, withinAllAttackersRange, ignoreODP, force);

        double myStrength = nationsToBlitzWith.stream().mapToDouble(f -> Math.pow(f.getWarIndex(), 3)).sum();

        StringBuilder response = new StringBuilder();
        numResults = Math.min(numResults, 25);

        for (int i = 0; i < Math.min(numResults, counterChance.size()); i++) {
            Map.Entry<DBNation, Double> entry = counterChance.get(i);
            DBNation nation = entry.getKey();
            double counterStrength = entry.getValue();

            response.append('\n')
                    .append("<" + Settings.INSTANCE.DNS_URL() + "/nation/" + nation.getNation_id() + ">")
                    .append(" | " + String.format("%16s", nation.getNation()))
                    .append(" | " + String.format("%16s", nation.getAllianceName()));

            response.append("\n```")
//                            .append(String.format("%5s", (int) nation.getScore())).append(" ns").append(" | ")
                    // TODO FIXME :||remove unprotected
//                    .append(String.format("%2s", nation.getCities())).append(" \uD83C\uDFD9").append(" | ")
////                                .append(String.format("%5s", nation.getAvg_infra())).append(" \uD83C\uDFD7").append(" | ")
//                    .append(String.format("%6s", nation.getSoldiers())).append(" \uD83D\uDC82").append(" | ")
//                    .append(String.format("%5s", nation.getTanks())).append(" \u2699").append(" | ")
//                    .append(String.format("%5s", nation.getAircraft())).append(" \u2708").append(" | ")
//                    .append(String.format("%4s", nation.getShips())).append(" \u26F5").append(" | ")
//                            .append(String.format("%1s", nation.getOff())).append(" \uD83D\uDDE1").append(" | ")
                    .append(String.format("%1s", nation.getDef())).append(" \uD83D\uDEE1");
//                                .append(String.format("%2s", nation.getSpies())).append(" \uD83D\uDD0D");

            if (nation.hasProtection()) {
                response.append(" | ").append("protected=" + DiscordUtil.timestamp(nation.getProtectionRemainingMs(), null));
            }

            Activity activity = nation.getActivity(14 * 24);
            long currentHour = TimeUtil.getHour();
            double loginChance = activity.loginChance((int) Math.max(1, (24 - (currentHour % 24))), true);
            int loginPct = (int) (loginChance * 100);

            response.append(" | log=" + loginPct + "%");
            response.append(" | str=" + MathMan.format(100 * counterStrength / myStrength) + "%");
            response.append("```");
        }

        if (ignoreDNR){
            response.append("\n**WARNING: VIOLATING THE DO NOT RAID LIST IS PROHIBITED**");
        }

        return response.toString();
    }

    // TODO FIXME :||remove war cmd !!important
//    @Command(desc="Find a war target that you can hit\n" +
//            "Defaults to `enemies` coalition")
//    @RolePermission(Roles.MEMBER)
//    public void war(@Me User author, @Me IMessageIO channel, @Me GuildDB db, @Me DBNation me, @Default("~enemies") Set<DBNation> targets, @Default("8") int numResults,
//                      @Arg("Score to search for targets within war range of\n" +
//                              "Defaults to your score")
//                      @Switch("r") Double attackerScore,
//                      @Arg("Include inactive nations in the search\n" +
//                              "Defaults to false")
//                      @Switch("i") boolean includeInactives,
//                        @Arg("Include applicants in the search\n" +
//                                "Defaults to false")
//                      @Switch("a") boolean includeApplicants,
//                      @Arg("Only list targets with offensive wars they are winning")
//                      @Switch("p") boolean onlyPriority,
//                      @Arg("Only list targets weaker than you")
//                      @Switch("w") boolean onlyWeak,
//                      @Arg("Sort by easiest targets")
//                      @Switch("e") boolean onlyEasy,
//                        @Arg("Only list targets with less cities than you")
//                      @Switch("c") boolean onlyLessCities,
//                      @Arg("Return results in direct message")
//                      @Switch("d") boolean resultsInDm,
//                      @Arg("Include nations much stronger than you in the search\n" +
//                              "Defaults to false")
//                      @Switch("s") boolean includeStrong) throws IOException, ExecutionException, InterruptedException {
//        if (resultsInDm) {
//            channel = new DiscordChannelIO(RateLimitUtil.complete(author.openPrivateChannel()), null);
//        }
//        if (attackerScore == null) attackerScore = me.getScore();
//
//        String aa = null;
//
//        if (!includeApplicants) targets.removeIf(f -> f.active_m() > 1440 && f.getPosition() <= 1);
//        if (!includeInactives) targets.removeIf(n -> n.active_m() >= 2440);
//        targets.removeIf(n -> n.isVacation());
////                nations.removeIf(n -> n.isBeige());
//
//        double minScore = attackerScore * 0.75;
//        double maxScore = attackerScore * DNS.WAR_RANGE_MAX_MODIFIER;
//
//        List<DBNation> strong = new ArrayList<>();
//
//        ArrayList<DBNation> targetsStorted = new ArrayList<>();
//        for (DBNation nation : targets) {
//            if (nation.getScore() >= maxScore || nation.getScore() <= minScore) continue;
//            if (nation.active_m() > 2440 && !includeInactives) continue;
//            if (nation.isVacation()) continue;
//            if (nation.getDef() >= 3) continue;
//            if (nation.getCities() >= me.getCities() * 1.5 && !includeStrong && me.getGroundStrength(false, true) > nation.getGroundStrength(true, false) * 2) continue;
//            if (nation.getCities() >= me.getCities() * 1.8 && !includeStrong && nation.active_m() < 2880) continue;
//            targetsStorted.add(nation);
//        }
//
//        if (onlyPriority) {
//            targetsStorted.removeIf(f -> f.getNumWars() == 0);
//            targetsStorted.removeIf(f -> f.getRelativeStrength() <= 1);
//        }
//
//        DBNation finalMe = me;
//        if (onlyWeak) {
//            targetsStorted.removeIf(f -> f.getGroundStrength(true, false) > finalMe.getGroundStrength(true, false));
//            targetsStorted.removeIf(f -> f.getAircraft() > finalMe.getAircraft());
//        }
//        if (onlyLessCities) {
//            targetsStorted.removeIf(f -> f.getCities() > finalMe.getCities());
//        }
//
//        Set<DBWar> wars = me.getActiveWars();
//        for (DBWar war : wars) {
//            targetsStorted.remove(war.getNation(true));
//            targetsStorted.remove(war.getNation(false));
//        }
//
//        int mySoldierRebuy = me.getCities() * Buildings.BARRACKS.getUnitCap() * 5 * 2;
//
//        long currentTurn = TimeUtil.getTurn();
//
//        List<Map.Entry<DBNation, Double>> nationNetValues = new ArrayList<>();
//
//        for (DBNation nation : targetsStorted) {
//            if (nation.isBeige()) continue;
//            double value;
//            if (onlyEasy) {
//                value = BlitzGenerator.getAirStrength(nation, true);
//            } else {
////                        SimulatedWarNode origin = SimulatedWarNode.of(nation, me.getNation_id() + "", nation.getNation_id() + "", "raid");
//                 value = BlitzGenerator.getAirStrength(nation, true);
//                value *= 2 * (nation.getCities() / (double) me.getCities());
//                if (nation.getOff() > 0) value /= 4;
//                if (nation.getShips() > 1 && nation.getOff() > 0 && nation.isBlockader()) value /= 2;
//                if (nation.getDef() <= 1) value /= (1.05 + (0.1 * nation.getDef()));
//                if (nation.active_m() > 1440) value *= 1 + Math.sqrt(nation.active_m() - 1440) / 250;
//                value /= (1 + nation.getOff() * 0.1);
//                if (nation.getScore() > attackerScore * 1.25) value /= 2;
//                if (nation.getOff() > 0) value /= nation.getRelativeStrength();
//            }
//
//            nationNetValues.add(new AbstractMap.SimpleEntry<>(nation, value));
//        }
//
//        Map<DBNation, Integer> beigeTurns = new HashMap<>();
//
//        if (nationNetValues.isEmpty()) {
//            for (DBNation nation : targetsStorted) {
//                if (nation.isBeige()) {
//                    int turns = beigeTurns.computeIfAbsent(nation, f -> f.getBeigeTurns());
//                    nationNetValues.add(new AbstractMap.SimpleEntry<>(nation, (double) turns));
//                }
//            }
//            if (nationNetValues.isEmpty()) {
//                String message;
//                if (onlyPriority) {
//                    message = "No targets found. Try " + CM.war.find.enemy.cmd.toSlashMention() + "";
//                } else {
//                    message = "No targets found:\n" +
//                            "- Add `-i` to include inactives\n" +
//                            "- Add `-a` to include applicants";
//                }
//                channel.send(message);
//                return;
//            }
//        }
//
//        nationNetValues.sort(Comparator.comparingDouble(Map.Entry::getValue));
//
//        StringBuilder response = new StringBuilder("**Results for " + me.getNation() + "**:");
//
//        int count = 0;
//
//        boolean whitelisted = db.isWhitelisted();
//
//        for (Map.Entry<DBNation, Double> nationNetValue : nationNetValues) {
//            if (count++ == numResults) break;
//
//            DBNation nation = nationNetValue.getKey();
//
//            response.append('\n')
//                    .append("<" + Settings.INSTANCE.DNS_URL() + "/nation/id=" + nation.getNation_id() + ">")
//                    .append(" | " + String.format("%16s", nation.getNation()))
//                    .append(" | " + String.format("%16s", nation.getAllianceName()));
//
//            if (whitelisted) {
//                double total = nation.lootTotal();
//                if (total != 0) {
//                    response.append(": $" + MathMan.format(total));
//                }
//            }
//
//            response.append("\n```")
//                    .append(String.format("%2s", nation.getCities())).append(" \uD83C\uDFD9").append(" | ")
//                    .append(String.format("%6s", nation.getSoldiers())).append(" \uD83D\uDC82").append(" | ")
//                    .append(String.format("%5s", nation.getTanks())).append(" \u2699").append(" | ")
//                    .append(String.format("%5s", nation.getAircraft())).append(" \u2708").append(" | ")
//                    .append(String.format("%4s", nation.getShips())).append(" \u26F5").append(" | ")
//                    .append(String.format("%1s", nation.getDef())).append(" \uD83D\uDEE1");
//
//            if (nation.isBeige()) {
//                int turns = beigeTurns.computeIfAbsent(nation, f -> f.getBeigeTurns());
//                if (turns > 0) {
//                    response.append(" | ").append("beige=" + turns);
//                }
//            }
//
//            Activity activity = nation.getActivity(14 * 24);
//            double loginChance = activity.loginChance((int) Math.max(1, (24 - (currentTurn % 24))), true);
//            int loginPct = (int) (loginChance * 100);
//
//            response.append(" | login=" + loginPct + "%");
//            response.append("```");
//        }
//
//        if (count == 0) {
//            channel.send("No results. Please ping a target (advisor)");
//        } else {
//            channel.send(response.toString());
//        }
//    }

    // TODO FIXME :||remove
//    @Command(desc = "Find nations to do a spy op against the specified enemy\n" +
//                    "Op types: (INTEL,NUKE,MISSILE,SHIPS,AIRCRAFT,TANKS,SPIES,SOLDIER) or `*` (for all op types)\n" +
//                    "The alliance argument is optional\n" +
//                    "Use `success>80` to specify a cutoff for spyop success")
//    @RolePermission(Roles.MEMBER)
//    public String Counterspy(@Me IMessageIO channel, @Me GuildDB db,
//                             @Arg("The enemy to spy")
//                             DBNation enemy,
//                             @Arg("The allowed spy operations")
//                             Set<SpyCount.Operation> operations,
//                             @Arg("The nations doing the spy ops on the enemy\n" +
//                              "Defaults to nations in the alliance")
//                             @Default Set<DBNation> counterWith,
//                             @Arg("Required success rate for the spy op")
//                             @Switch("s") @Range(min=0, max=100) Integer minSuccess) throws ExecutionException, InterruptedException, IOException {
//        if (operations.isEmpty()) throw new IllegalArgumentException("Valid operations: " + StringMan.getString(SpyCount.Operation.values()));
//        if (counterWith == null) {
//            counterWith = new HashSet<>(Locutus.imp().getNationDB().getNations(db.getAllianceIds()));
//        }
//        counterWith.removeIf(n -> n.getSpies() == 0 || !n.isInSpyRange(enemy) || n.active_m() > TimeUnit.DAYS.toMinutes(2));
//
//        List<Map.Entry<DBNation, Map.Entry<SpyCount.Operation, Map.Entry<Integer, Double>>>> netDamage = new ArrayList<>();
//
//
//        channel.send("Please wait...");
//
//        Integer enemySpies = enemy.updateSpies(PagePriority.ESPIONAGE_ODDS_SINGLE);
//
//        SpyCount.Operation[] opTypes = operations.toArray(new SpyCount.Operation[0]);
//        for (DBNation nation : counterWith) {
//            Integer mySpies = nation.updateSpies(PagePriority.ESPIONAGE_ODDS_SINGLE);
//
//            if (enemySpies == -1) {
//                return "Unknown enemy spies";
//            }
//            if (opTypes.length == 1 && opTypes[0] == SpyCount.Operation.SPIES && enemySpies == 0) {
//                return "Enemy has no spies";
//            }
//
//            Map.Entry<SpyCount.Operation, Map.Entry<Integer, Double>> best = SpyCount.getBestOp(mySpies, enemy, nation.hasProject(Projects.SPY_SATELLITE), opTypes);
//            if (best != null) {
//                netDamage.add(new AbstractMap.SimpleEntry<>(nation, best));
//            }
//        }
//
//        Collections.sort(netDamage, (o1, o2) -> Double.compare(o2.getValue().getValue().getValue(), o1.getValue().getValue().getValue()));
//
//        if (netDamage.isEmpty()) {
//            return "No nations found";
//        }
//
//        String title = "Recommended ops";
//        StringBuilder body = new StringBuilder();
//
//        int nationCount = 0;
//        for (int i = 0; i < netDamage.size(); i++) {
//            Map.Entry<DBNation, Map.Entry<SpyCount.Operation, Map.Entry<Integer, Double>>> entry = netDamage.get(i);
//
//            Map.Entry<SpyCount.Operation, Map.Entry<Integer, Double>> opinfo = entry.getValue();
//            SpyCount.Operation op = opinfo.getKey();
//            Map.Entry<Integer, Double> safetyDamage = opinfo.getValue();
//
//            DBNation nation = entry.getKey();
//            Integer safety = safetyDamage.getKey();
//            Double damage = safetyDamage.getValue();
//
//            int attacking = entry.getKey().getSpies();
//            int spiesUsed = attacking;
//            if (op != SpyCount.Operation.SPIES) {
//                spiesUsed = SpyCount.getRecommendedSpies(attacking, enemy.getSpies(), safety, op, enemy);
//            }
//
//            double odds = SpyCount.getOdds(spiesUsed, enemySpies, safety, op, enemy);
//            if (minSuccess != null && odds <= minSuccess) continue;
//            if (++nationCount >= 10) break;
//
//            double kills = SpyCount.getKills(spiesUsed, enemy, op, nation.hasProject(Projects.SPY_SATELLITE));
//
//            String nationUrl = DNS.getBBUrl(nation.getNation_id(), false);
//            String allianceUrl = DNS.getBBUrl(nation.getAlliance_id(), true);
//            body.append(nationUrl).append(" | ")
//                    .append(allianceUrl).append("\n");
//
//            String safetyStr = safety == 3 ? "covert" : safety == 2 ? "normal" : "quick";
//
//            body.append(op.name())
//                    .append(" (" + safetyStr + ") with ")
//                    .append(nation.updateSpies(PagePriority.ESPIONAGE_ODDS_SINGLE) + " spies (")
//                    .append(MathMan.format(odds) + "% for $")
//                    .append(MathMan.format(damage) + "net damage)")
//                    .append(" killing " + MathMan.format(kills) + " " + op.unit.getName())
//                    .append("\n")
//            ;
//        }
//
//        body.append("**Enemy:** ")
//                .append(DNS.getBBUrl(enemy.getNation_id(), false))
//                .append(" | ")
//                .append(DNS.getBBUrl(enemy.getAlliance_id(), true))
//                .append("\n**Spies: **").append(enemySpies).append("\n")
//                .append(enemy.toMarkdown(true, true, false, true, false, false))
//                .append(enemy.toMarkdown(true, true, false, false, true, true))
//        ;
//
//        channel.create().embed(title, body.toString()).send();
//        return null;
//    }

    // TODO FIXME :||remove spyops
//    @Command(aliases = {"spyop", "spyops"},
//    desc = "List enemies and spy operation by the highest damage:\n" +
//            "Use `*` for the alliance to target enemies with active wars against allies\n" +
//            "Use `*` for op type to automatically find the best op type\n" +
//            "Use e.g. `80` as the `requiredSuccess` to only list operations above 80% success\n\n" +
//            "e.g. `{prefix}spy find target targets:enemies operations:spies`")
//    @RolePermission(Roles.MEMBER)
//    public String Spyops(@Me User author, @Me IMessageIO channel, @Me GuildDB db, @Me DBNation me,
//                         @Arg("The allowed targets")
//                         Set<DBNation> targets,
//                         @Arg("The allowed operations")
//                         Set<SpyCount.Operation> operations,
//                         @Arg("The required chance of success for an operation")
//                         @Default("40") @Range(min=0,max=100) int requiredSuccess,
//                         @Arg("Return results as a discord direct message")
//                         @Switch("d") boolean directMesssage,
//                         @Arg("Sort by unit kills instead of damage")
//                         @Switch("k") boolean prioritizeKills,
//                         @Arg("The nation doing the spy operation\n" +
//                                 "Defaults to your nation")
//                         @Switch("n") DBNation attacker) throws ExecutionException, InterruptedException, IOException {
//        DBNation finalNation = attacker == null ? me : attacker;
//
//        targets.removeIf(f -> f.active_m() > 2880);
//        targets.removeIf(f -> f.getPosition() <= Rank.APPLICANT.id);
//        String title = "Recommended ops";
//        String body = runSpyOps(finalNation, db, targets, operations, requiredSuccess, prioritizeKills);
//
//        if (directMesssage) {
//            channel = new DiscordChannelIO(RateLimitUtil.complete(author.openPrivateChannel()), null);
//        }
//
//        IMessageBuilder msg = channel.create().embed(title, body);
//
//        String response = ("Use " + CM.nation.spies.cmd.toSlashMention() + " first to ensure the results are up to date");
//        msg.append(response.toString()).send();
//        return null;
//    }
//
//    public String runSpyOps(DBNation me, GuildDB db, Set<DBNation> enemies, Set<SpyCount.Operation> operations, int requiredSuccess, boolean prioritizeKills) throws IOException {
//        double minSuccess = requiredSuccess > 0 ? requiredSuccess : 50;
//
//        if (me == null) {
//            return "Please use " + CM.register.cmd.toSlashMention() + "";
//        }
//
//        boolean findOptimal = true;
//
//        Set<Integer> allies = new HashSet<>();
//        Set<Integer> alliesCoalition = db.getCoalition("allies");
//        if (alliesCoalition != null) allies.addAll(alliesCoalition);
//        if (me.getAlliance_id() != 0) allies.add(me.getAlliance_id());
//        Set<Integer> aaIds = db.getAllianceIds();
//        allies.addAll(aaIds);
//
//        Set<Integer> myEnemies = Locutus.imp().getWarDb().getWarsByNation(me.getNation_id()).stream()
//                .map(dbWar -> dbWar.getAttacker_id() == me.getNation_id() ? dbWar.getDefender_id() : dbWar.getAttacker_id())
//                .collect(Collectors.toSet());
//
//        Function<DBNation, Boolean> isInSpyRange = nation -> me.isInSpyRange(nation) || myEnemies.contains(nation.getNation_id());
//
//        Function<Integer, Boolean> isInvolved = integer -> {
//            if (integer == me.getNation_id()) return true;
//            DBNation nation = Locutus.imp().getNationDB().getNation(integer);
//            return nation != null && allies.contains(nation.getAlliance_id());
//        };
//
//        enemies.removeIf(nation -> {
//            if (!isInSpyRange.apply(nation)) return true;
//            if (nation.isVacation()) return true;
//            if (nation.isEspionageFull()) return true;
//            return false;
//        });
//
//        if (enemies.isEmpty()) {
//            return "No nations found (1)";
//        }
//
//        int mySpies = me.updateSpies(PagePriority.ESPIONAGE_ODDS_SINGLE);
//        long dcTime = TimeUtil.getTimeFromTurn(TimeUtil.getTurn() - (TimeUtil.getTurn() % 24));
//
//        List<Map.Entry<DBNation, Map.Entry<SpyCount.Operation, Map.Entry<Integer, Double>>>> netDamage = new ArrayList<>();
//
//        for (DBNation nation : enemies) {
//            Integer spies = nation.updateSpies(PagePriority.ESPIONAGE_ODDS_SINGLE, false, false);
//            if (spies == null) {
//                continue;
//            }
//            if (spies == -1) {
//                continue;
//            }
//            ArrayList<SpyCount.Operation> opTypesList = new ArrayList<>(operations);
//
//            if (spies == 0) opTypesList.remove(SpyCount.Operation.SPIES);
//            if (nation.getSoldiers() == 0) opTypesList.remove(SpyCount.Operation.SOLDIER);
//            if (nation.getTanks() == 0) opTypesList.remove(SpyCount.Operation.TANKS);
//            if (nation.getAircraft() == 0) opTypesList.remove(SpyCount.Operation.AIRCRAFT);
//            if (nation.getShips() == 0) opTypesList.remove(SpyCount.Operation.SHIPS);
//
//            int maxMissile = MilitaryUnit.MISSILE.getMaxPerDay(nation.getCities(), nation::hasProject);
//            if (opTypesList.contains(SpyCount.Operation.MISSILE) && nation.getMissiles() > 0 && nation.getMissiles() <= maxMissile) {
//                Map<Long, Integer> purchases = nation.getUnitPurchaseHistory(MilitaryUnit.MISSILE, dcTime);
//                if (!purchases.isEmpty()) opTypesList.remove(SpyCount.Operation.MISSILE);
//            }
//
//            int maxNukes = MilitaryUnit.NUKE.getMaxPerDay(nation.getCities(), nation::hasProject);
//            if (opTypesList.contains(SpyCount.Operation.NUKE) && nation.getNukes() > 0 && nation.getNukes() <= maxNukes) {
//                Map<Long, Integer> purchases = nation.getUnitPurchaseHistory(MilitaryUnit.NUKE, dcTime);
//                if (!purchases.isEmpty()) opTypesList.remove(SpyCount.Operation.NUKE);
//            }
//            SpyCount.Operation[] opTypes = opTypesList.toArray(new SpyCount.Operation[0]);
//
//            Map.Entry<SpyCount.Operation, Map.Entry<Integer, Double>> best = SpyCount.getBestOp(!prioritizeKills, mySpies, nation, me.hasProject(Projects.SPY_SATELLITE), opTypes);
//            if (best != null) {
//                double netDamageCost = best.getValue().getValue();
//                if (nation.hasProject(Projects.INTELLIGENCE_AGENCY)) {
//                    netDamageCost *= 2;
//                }
//                if (nation.hasProject(Projects.SPY_SATELLITE)) {
//                    netDamageCost *= 2;
//                }
//                best.getValue().setValue(netDamageCost);
//                netDamage.add(new AbstractMap.SimpleEntry<>(nation, best));
//            }
//        }
//
//        Collections.sort(netDamage, (o1, o2) -> Double.compare(o2.getValue().getValue().getValue(), o1.getValue().getValue().getValue()));
//
//        if (netDamage.isEmpty()) {
//            return "No nations found (2)";
//        }
//
//        StringBuilder body = new StringBuilder("Results for " + me.getNation() + ":\n");
//        int nationCount = 0;
//
//        ArrayList<Map.Entry<DBNation, Runnable>> targets = new ArrayList<>();
//
//        for (int i = 0; i < netDamage.size(); i++) {
//            Map.Entry<DBNation, Map.Entry<SpyCount.Operation, Map.Entry<Integer, Double>>> entry = netDamage.get(i);
//
//            Map.Entry<SpyCount.Operation, Map.Entry<Integer, Double>> opinfo = entry.getValue();
//            SpyCount.Operation op = opinfo.getKey();
//            Map.Entry<Integer, Double> safetyDamage = opinfo.getValue();
//
//            DBNation nation = entry.getKey();
//            Integer safety = safetyDamage.getKey();
//            Double damage = safetyDamage.getValue();
//
//            int spiesUsed = mySpies;
//            if (op != SpyCount.Operation.SPIES) {
//                Integer enemySpies = nation.updateSpies(PagePriority.ESPIONAGE_ODDS_SINGLE, false, false);
//                spiesUsed = SpyCount.getRecommendedSpies(spiesUsed, enemySpies, safety, op, nation);
//            }
//
//            double kills = SpyCount.getKills(spiesUsed, nation, op, me.hasProject(Projects.SPY_SATELLITE));
//
//            Integer enemySpies = nation.getSpies();
//            double odds = SpyCount.getOdds(spiesUsed, enemySpies, safety, op, nation);
//            if (odds <= minSuccess) continue;
//
//            int finalSpiesUsed = spiesUsed;
//            Runnable task = new Runnable() {
//                @Override
//                public void run() {
//                    String nationUrl = DNS.getBBUrl(nation.getNation_id(), false);
//                    String allianceUrl = DNS.getBBUrl(nation.getAlliance_id(), true);
//                    body.append(nationUrl).append(" | ")
//                            .append(allianceUrl).append("\n");
//
//                    body.append("Op: " + op.name()).append("\n")
//                            .append("Safety: " + SpyCount.Safety.byId(safety)).append("\n")
//                            .append("Enemy \uD83D\uDD0E: " + nation.getSpies()).append("\n")
//                            .append("Attacker \uD83D\uDD0E: " + finalSpiesUsed).append("\n")
//                            .append("Dmg: $" + MathMan.format(damage)).append("\n")
//                            .append("Kills: " + MathMan.format(kills)).append("\n")
//                            .append("Success: " + MathMan.format(odds)).append("%\n\n")
//                    ;
//                }
//            };
//            targets.add(new AbstractMap.SimpleEntry<>(nation, task));
//        }
//
//        targets.removeIf(f -> f.getKey().isEspionageFull());
//
//        for (int i = 0; i < Math.min(5, targets.size()); i++) {
//            targets.get(i).getValue().run();
//        }
//        return body.toString();
//    }

    // TODO FIXME :||remove raid sheet
//    @Command(desc = "Generate a sheet of raid targets")
//    @RolePermission(Roles.MILCOM)
//    public String raidSheet(@Me IMessageIO io, @Me GuildDB db, @Me User author,
//                            Set<DBNation> attackers,
//                            Set<DBNation> targets,
//                            @Switch("i") boolean includeInactiveAttackers,
//                            @Switch("a") boolean includeApplicantAttackers,
//                            @Switch("b") boolean includeBeigeAttackers,
//                            @Switch("s") SpreadSheet sheet) throws GeneralSecurityException, IOException {
//        List<String> warnings = new ArrayList<>();
//        if (!includeInactiveAttackers) {
//            int num = attackers.size();
//            attackers.removeIf(f -> f.active_m() > 2440);
//            int numRemoved = num - attackers.size();
//            if (numRemoved > 0) {
//                warnings.add("Removed " + numRemoved + " inactive attackers");
//            }
//        }
//        if (!includeApplicantAttackers) {
//            int num = attackers.size();
//            attackers.removeIf(f -> f.getPositionEnum().id <= Rank.APPLICANT.id);
//            int numRemoved = num - attackers.size();
//            if (numRemoved > 0) {
//                warnings.add("Removed " + numRemoved + " applicant attackers");
//            }
//        }
//        if (!includeBeigeAttackers) {
//            int num = attackers.size();
//            attackers.removeIf(f -> f.isBeige());
//            int numRemoved = num - attackers.size();
//            if (numRemoved > 0) {
//                warnings.add("Removed " + numRemoved + " beige attackers");
//            }
//        }
//        {
//            int num = attackers.size();
//            attackers.removeIf(f -> f.getOff() >= f.getMaxOff() || f.isVacation());
//            int numRemoved = num - attackers.size();
//            if (numRemoved > 0) {
//                warnings.add("Removed " + numRemoved + " attackers with no free offensive slots");
//            }
//        }
//
//        if (attackers.size() > 200) {
//            throw new IllegalArgumentException("Too many attackers: " + attackers.size() + " (max 200)");
//        }
//
//        // Remove unraidable
//        targets.removeIf(f -> f.getDef() >= 3 || f.isBeige() || f.isVacation());
//        // remove DNR
//        Function<DBNation, Boolean> canRaid = db.getCanRaid();
//        targets.removeIf(f -> !canRaid.apply(f));
//        Set<Integer> aaIds = db.getAllianceIds();
//
//        Set<Integer> attackerAAs = attackers.stream().map(f -> f.getAlliance_id()).collect(Collectors.toSet());
//        if (!aaIds.containsAll(attackerAAs)) {
//            throw new IllegalArgumentException("Only attackers from this guild's alliance ids can be used: `" + StringMan.getString(aaIds) + "`. You tried generating targets for attackers in the alliance ids: `" + StringMan.getString(attackerAAs) + "`");
//        }
//
//        Map.Entry<Double, Double> minMax = NationScoreMap.getMinMaxScore(attackers, 0.75, DNS.WAR_RANGE_MAX_MODIFIER);
//        targets.removeIf(f -> f.getScore() < minMax.getKey() || f.getScore() > minMax.getValue());
//        Map.Entry<Double, Double> enemyMinMax = NationScoreMap.getMinMaxScore(targets, 1 / DNS.WAR_RANGE_MAX_MODIFIER, 1 / 0.75);
//        attackers.removeIf(f -> f.getScore() < enemyMinMax.getKey() || f.getScore() > enemyMinMax.getValue());
//
//        if (attackers.isEmpty()) {
//            throw new IllegalArgumentException("No attackers with free offensive slots provided");
//        }
//
//        if (targets.isEmpty()) {
//            throw new IllegalArgumentException("No targets with free slots found");
//        }
//
//        // sort targets by loot
//        List<DBNation> targetsSorted = new ArrayList<>(targets);
//        targetsSorted.sort((o1, o2) -> {
//            double[] loot1 = o1.getLootRevenueTotal();
//            double[] loot2 = o2.getLootRevenueTotal();
//            double lootValue1 = ResourceType.convertedTotal(loot1);
//            double lootValue2 = ResourceType.convertedTotal(loot2);
//            return Double.compare(lootValue2, lootValue1);
//        });
//
//        /*
//        If enemy has more ships and attacker is currently blockaded, reduce loot by 2/5 * activity
//         */
//        NationScoreMap<DBNation> enemyMap = new NationScoreMap<>(targetsSorted, DBNation::getScore, 1/ DNS.WAR_RANGE_MAX_MODIFIER, 1/0.75);
//
//        Map<DBNation, double[]> loots = new HashMap<>();
//
//        Function<DBNation, Double> attActivityFactor = new CachedFunction<>(o1 -> {
//            double lastOffensive1 = o1.daysSinceLastOffensive();
//            if (lastOffensive1 > 7) lastOffensive1 = o1.active_m() / (double) TimeUnit.DAYS.toMinutes(1);
//            lastOffensive1 /= o1.looterModifier(false);
//            return lastOffensive1;
//        });
//        List<DBNation> attackersSorted = new ArrayList<>(attackers);
//        attackersSorted.sort((o1, o2) -> Double.compare(attActivityFactor.apply(o2), attActivityFactor.apply(o1)));
//
//
//        Function<DBNation, Double> getFightChance = new CachedFunction<>(nation -> {
//            if (nation.getAircraft() == 0 && nation.getSoldiers() == 0 && nation.getShips() == 0) {
//                return 0d;
//            }
//            double daysSinceLoss = nation.daysSinceLastDefensiveWarLoss();
//            if (daysSinceLoss <  nation.active_m() / (double) TimeUnit.DAYS.toMinutes(1)) {
//                return 0d;
//            }
//            if (nation.active_m() > 25920) {
//                return 0d;
//            }
//            if (nation.active_m() < 1440) {
//                return 1d;
//            }
//            LoginFactorResult activity = DBNation.getLoginFactorPercents(nation);
//            // average
//            double total = 0;
//            int count = 0;
//            for (Map.Entry<LoginFactor, Double> entry : activity.getResult().entrySet()) {
//                total += (entry.getValue() / 100d);
//                count++;
//            }
//            return total / count;
//        });
//
//        Map<DBNation, List<DBNation>> targetMap = new LinkedHashMap<>();
//        Map<DBNation, List<DBNation>> targetMapInverse = new LinkedHashMap<>();
//
//        for (DBNation attacker : attackersSorted) {
//            int offRemaining = attacker.getMaxOff() - attacker.getOff() + targetMapInverse.getOrDefault(attacker, Collections.emptyList()).size();
//            if (offRemaining <= 0) continue;
//
//            List<DBNation> defenders = enemyMap.get((int) Math.round(attacker.getScore()));
//            List<DBNation> defendersSorted = new ArrayList<>(defenders);
//
//            Function<DBNation, Double> defenderValue = new CachedFunction<>(defender -> {
//                int currDefWars = defender.getDef();
//                int newDefWars = targetMap.getOrDefault(defender, Collections.emptyList()).size();
//                int totalWars = currDefWars + newDefWars;
//                if (totalWars >= 3) return 0d;
//                if (attacker.isBlockaded() && defender.active_m() < 7200 && defender.getShips() > attacker.getShips()) return 0d;
//
//                double fightChance = getFightChance.apply(defender);
//                boolean fightFlag = fightChance > 0.5 || (fightChance > 0.1 && defender.active_m() < 7200);
//
//                boolean canGroundLoot = attacker.getGroundStrength(true, false) > defender.getGroundStrength(true, false);
//                boolean easyGroundLoot = attacker.getGroundStrength(true, false) > defender.getGroundStrength(true, false) * 2.5;
//
//                if (fightFlag && !easyGroundLoot && defender.getShips() > attacker.getShips()) return 0d;
//                if (fightFlag && defender.getAircraft() > attacker.getAircraft() * 0.66 && !easyGroundLoot) return 0d;
//                if (fightFlag && defender.getAircraft() > attacker.getAircraft()) return 0d;
//
//                if (defender.getGroundStrength(true, false) > attacker.getGroundStrength(true, false) && defender.getAircraft() > attacker.getAircraft() * 0.33 && defender.getShips() > attacker.getShips() * 0.33) return 0d;
//
//                double[] loot = loots.computeIfAbsent(defender, DBNation::getLootRevenueTotal);
//                double lootValue = ResourceType.convertedTotal(loot);
//
//                double groundFactor = easyGroundLoot ? 1.1 : canGroundLoot ? 0.8 : 0;
//                double moneyPerGround = Buildings.BARRACKS.getUnitCap() * 5 * defender.getCities() * groundFactor;
//                double moneyRemaining = Math.max(0, ((loot[0] / 0.14) - (moneyPerGround * 10 * totalWars) - 50000 * defender.getCities()) * 0.86);
//
//                int numGround = 10;
//                lootValue += Math.min(moneyRemaining, moneyPerGround * numGround);
//
//                double depositChance = defender.isBlockaded() || defender.getShips() >= attacker.getShips() * 0.33 ? 0.5 * fightChance : 0;
//                lootValue = lootValue * (1 - depositChance);
//
//                if (fightChance > 0 && !canGroundLoot) {
//                    lootValue -= MilitaryUnit.AIRCRAFT.getConvertedCost() * Math.min(attacker.getAircraft(), defender.getAircraft()) +
//                            MilitaryUnit.SHIP.getConvertedCost() * Math.min(attacker.getShips(), defender.getShips()) +
//                            MilitaryUnit.TANK.getConvertedCost() * Math.min(attacker.getTanks(), defender.getTanks());
//                }
//
//                return lootValue;
//            });
//
//            // remove defendersSorted where defenderValue <= 0
//            defendersSorted.removeIf(f -> defenderValue.apply(f) <= 0);
//            // sort descending
//            defendersSorted.sort((o1, o2) -> Double.compare(defenderValue.apply(o2), defenderValue.apply(o1)));
//
//            for (DBNation defender : defendersSorted) {
//                // add to map
//                targetMap.computeIfAbsent(defender, f -> new ArrayList<>()).add(attacker);
//                targetMapInverse.computeIfAbsent(attacker, f -> new ArrayList<>()).add(defender);
//
//                offRemaining--;
//                if (offRemaining <= 0) break;
//            }
//        }
//
//        if (sheet == null) {
//            sheet = SpreadSheet.create(db, SheetKey.RAID_SHEET);
//        }
//
//        SheetUtil.writeTargets(sheet, targetMap, 0);
//
//        StringBuilder header = new StringBuilder();
//        if (!warnings.isEmpty()) {
//            header.append("**" + warnings.size() + " Warnings:**\n");
//            for (String warning : warnings) {
//                header.append("- " + warning).append("\n");
//            }
//        }
//
//        sheet.send(io, header.isEmpty() ? null : header.toString(), author.getAsMention()).send();
//
//        return null;
//
//    }

    // TODO FIXME :||remove intel op sheet
//    @Command(desc = "Generate a list of raidable targets to gather intel on\n" +
//            "`<time>`- filters out nations we have loot intel on in that period\n" +
//            "`<attackers>`- The nations to assign to do the ops (i.e. your alliance link)\n" +
//            "`<ignore-topX>`- filter out top X alliances (e.g. due to DNR), in addition to the set `dnr` coalition\n\n" +
//            "Add `-l` to remove targets with loot history\n" +
//            "Add `-d` to list targets currently on the dnr\n\n" +
//            "e.g. `{prefix}sheets_milcom intelopsheet time:10d attacker:Rose dnrtopx:25`")
//    @RolePermission(Roles.MILCOM)
//    public String IntelOpSheet(@Me IMessageIO io, @Me GuildDB db, @Timestamp long time, Set<DBNation> attackers,
//                               @Arg("Exclude nations in the top X alliances (or direct allies)")
//                               @Default() Integer dnrTopX,
//                               @Arg("If nations with loot history are ignored")
//                               @Switch("l") boolean ignoreWithLootHistory,
//                               @Arg("If the alliance Do Not Raid settings are checked")
//                               @Switch("d") boolean ignoreDNR,
//                               @Switch("s") SpreadSheet sheet) throws GeneralSecurityException, IOException {
//
//        if (ignoreWithLootHistory) time = 0;
//        if (sheet == null) {
//            sheet = SpreadSheet.create(db, SheetKey.SPYOP_SHEET);
//        }
//        int maxOps = 2;
//
//        attackers.removeIf(f -> f.getPosition() <= 1 || f.active_m() > 1440 || f.isVacation());
//        if (dnrTopX == null) dnrTopX = db.getOrNull(GuildKey.DO_NOT_RAID_TOP_X);
//        if (dnrTopX == null) dnrTopX = 0;
//
//        List<DBNation> enemies = new ArrayList<>(Locutus.imp().getNationDB().getNations().values());
//
//
//        Set<Integer> allies = db.getAllies();
//        if (!ignoreDNR) {
//            Function<DBNation, Boolean> canRaid = db.getCanRaid(dnrTopX, true);
//            enemies.removeIf(f -> !canRaid.apply(f));
//        }
//        enemies.removeIf(f -> allies.contains(f.getAlliance_id()));
//        enemies.removeIf(f -> f.active_m() < 4320);
//        enemies.removeIf(f -> f.isVacation());
//        enemies.removeIf(f -> !f.isGray());
////        enemies.removeIf(f -> f.getCities() < 4);
//        enemies.removeIf(f -> f.getAvg_infra() < 300);
//        enemies.removeIf(f -> f.getDef() >= 3);
//
//        Map<DBNation, Double> opValueMap = new HashMap<>();
//
//        Iterator<DBNation> iter = enemies.iterator();
//        while (iter.hasNext()) {
//            DBNation nation = iter.next();
//            Map.Entry<Double, Boolean> opValue = nation.getIntelOpValue(time);
//            if (opValue == null) {
//                iter.remove();
//                continue;
//            }
//            opValueMap.put(nation, opValue.getKey());
//        }
//
//        Collections.sort(enemies, new Comparator<DBNation>() {
//            @Override
//            public int compare(DBNation o1, DBNation o2) {
//                double revenueTime1 = opValueMap.get(o1);
//                double revenueTime2 = opValueMap.get(o2);
//                return Double.compare(revenueTime2, revenueTime1);
//            }
//        });
//
//        enemies.addAll(new ArrayList<>(enemies));
//
//        // nations with big trades
//
//        Map<DBNation, List<Spyop>> targets = new HashMap<>();
//
//        ArrayList<DBNation> attackersList = new ArrayList<>(attackers);
//        Collections.shuffle(attackersList);
//
//        for (DBNation attacker : attackersList) {
//            int numOps = attacker.hasProject(Projects.INTELLIGENCE_AGENCY) ? 2 : 1;
//            numOps = Math.min(numOps, maxOps);
//
//            outer:
//            for (int i = 0; i < numOps; i++) {
//                iter = enemies.iterator();
//                while (iter.hasNext()) {
//                    DBNation enemy = iter.next();
//                    if (!attacker.isInSpyRange(enemy)) continue;
//                    List<Spyop> currentOps = targets.computeIfAbsent(enemy, f -> new ArrayList<>());
//                    if (currentOps.size() > 1) continue;
//                    if (currentOps.size() == 1 && currentOps.get(0).attacker == attacker) continue;
//                    Spyop op = new Spyop(attacker, enemy, 1, SpyCount.Operation.INTEL, 0, 3);
//
//                    currentOps.add(op);
//                    iter.remove();
//                    continue outer;
//                }
//                break;
//            }
//        }
//
//        sheet.updateClearCurrentTab();
//        SpySheet.generateSpySheet(sheet, targets);
//        sheet.updateWrite();
//
//        sheet.attach(io.create(), "spy_intel").send();
//        return null;
//    }

    // TODO FIXME :||remove list spy targets
//    @Command(desc = "Generate a subset of a spy sheet for only certain attackers or defenders")
//    @RolePermission(Roles.MILCOM)
//    public String listSpyTargets(@Me IMessageIO io, @Me User author, @Me GuildDB db,
//                                 @Arg("The current spy sheet")
//                                 SpreadSheet spySheet,
//                                 @Arg("Which attackers to include")
//                                 Set<DBNation> attackers,
//                                 @Arg("Which defenders to include\n" +
//                                         "Default: All")
//                                 @Default("*") Set<DBNation> defenders,
//                                 @Arg("The row the header is on\n" +
//                                         "Default: 1st row")
//                                 @Switch("h") Integer headerRow,
//                                 @Arg("Sheet to put the subset in")
//                                 @Switch("s") SpreadSheet output,
//                                 @Arg("Group the spy operations (left column) by attacker")
//                                 @Switch("a") boolean groupByAttacker) throws GeneralSecurityException, IOException {
//        if (headerRow == null) headerRow = 0;
//        Map<DBNation, Set<Spyop>> spyOps = SpyBlitzGenerator.getTargets(spySheet, headerRow, false);
//
//        if (output == null) {
//            output = SpreadSheet.create(db, SheetKey.SPYOP_SHEET);
//        }
//
//        List<Spyop> allOps = new ArrayList<>();
//        for (Map.Entry<DBNation, Set<Spyop>> entry : spyOps.entrySet()) {
//            for (Spyop spyop : entry.getValue()) {
//                if (attackers.contains(spyop.attacker) && defenders.contains(spyop.defender)) {
//                    allOps.add(spyop);
//                }
//            }
//        }
//
//        Map<DBNation, List<Spyop>> spyOpsFiltered = new LinkedHashMap<>();
//        for (Spyop op : allOps) {
//            if (groupByAttacker) {
//                spyOpsFiltered.computeIfAbsent(op.attacker, f -> new ArrayList<>()).add(op);
//            } else {
//                spyOpsFiltered.computeIfAbsent(op.defender, f -> new ArrayList<>()).add(op);
//            }
//        }
//
//        generateSpySheet(output, spyOpsFiltered, groupByAttacker);
//
//        output.updateClearCurrentTab();
//        output.updateWrite();
//
//        output.send(io, null, author.getAsMention()).send();
//        return null;
//
//
//    }
//
//    @Command(desc = "Generate a spy blitz sheet with the defender on the left and attackers on the right")
//    @RolePermission(Roles.MILCOM)
//    public String SpySheet(@Me IMessageIO io, @Me User author, @Me GuildDB db,
//                           Set<DBNation> attackers,
//                           Set<DBNation> defenders,
//                           @Arg("Allowed spy operations")
//                           @Default("nuke,missile,ships,aircraft,tanks,spies") Set<SpyCount.Operation> allowedTypes,
//                           @Arg("Force an update of all participant spy count")
//                           @Switch("f") boolean forceUpdate,
//                           @Arg("Check the defensive spy slots")
//                           @Switch("e") boolean checkEspionageSlots,
//                           @Arg("Prioritize unit kills instead of damage")
//                           @Switch("k") boolean prioritizeKills,
//                           @Switch("s") SpreadSheet sheet,
//                           @Arg("Max Attackers to assign per defender")
//                               @Range(min=1) @Switch("d") @Default("3") Integer maxDef,
//                           @Arg("Double the offensive spy ops, e.g. two sets of ops at day change\n" +
//                                   "Note: You should also set maxDef to e.g. `6`")
//                               @Switch("o") boolean doubleOps,
//                           @Arg("Remove the available spy ops in another spreadsheet")
//                           @Switch("r") Set<SpreadSheet> removeSheets,
//                           @Arg("Prioritize defenders in these alliances")
//                           @Switch("p") Set<DBAlliance> prioritizeAlliances,
//                           @Arg("Fine grained control over attacker priority\n" +
//                                   "e.g. `(#warpolicy=ARCANE?1.2:1)*(#active_m<1440?1.2:1)*(#hasProject(SS)?1.2:1)`")
//                           @Switch("aw") NationAttributeDouble attackerWeighting,
//                           @Arg("Fine grained control over defender priority")
//                           @Switch("dw") NationAttributeDouble defenderWeighting
//                           ) throws GeneralSecurityException, IOException {
//        if (sheet == null) {
//            sheet = SpreadSheet.create(db, SheetKey.SPYOP_SHEET);
//        }
//
//        SpyBlitzGenerator generator = new SpyBlitzGenerator(attackers, defenders, allowedTypes, forceUpdate, maxDef, checkEspionageSlots, 0, prioritizeKills);
//        if (prioritizeAlliances != null) {
//            for (DBAlliance alliance : prioritizeAlliances) {
//                generator.setAllianceWeighting(alliance, 1.2);
//            }
//            if (attackerWeighting != null) {
//                generator.setAttackerWeighting(attackerWeighting);
//            }
//            if (defenderWeighting != null) {
//                generator.setDefenderWeighting(defenderWeighting);
//            }
//        }
//
//        Map<DBNation, Integer> subtractDefensiveSlots = new HashMap<>();
//        Map<DBNation, Integer> subtractOffensiveSlots = new HashMap<>();
//
//        if (removeSheets != null && !removeSheets.isEmpty()) {
//            for (SpreadSheet subSheet : removeSheets) {
//                Map<DBNation, Set<Spyop>> spyOps = SpyBlitzGenerator.getTargets(subSheet, 0, true);
//                for (Map.Entry<DBNation, Set<Spyop>> entry : spyOps.entrySet()) {
//                    DBNation attacker = entry.getKey();
//                    for (Spyop op : entry.getValue()) {
//                        DBNation defender = op.defender;
//                        subtractOffensiveSlots.merge(attacker, 1, Integer::sum);
//                        subtractDefensiveSlots.merge(defender, 1, Integer::sum);
//                    }
//                }
//
//            }
//        }
//
//        Map<DBNation, List<Spyop>> targets = generator.assignTargets(doubleOps, subtractOffensiveSlots, subtractDefensiveSlots);
//
//        if (sheet == null) {
//            sheet = SpreadSheet.create(db, SheetKey.SPYOP_SHEET);
//        }
//
//        generateSpySheet(sheet, targets);
//
//        sheet.updateClearCurrentTab();
//        sheet.updateWrite();
//
//        sheet.send(io, null, author.getAsMention()).send();
//        return null;
//    }
//
//    private static void generateSpySheet(SpreadSheet sheet, Map<DBNation, List<Spyop>> opsAgainstNations) {
//        generateSpySheet(sheet, opsAgainstNations, false);
//    }
//
//    private static void generateSpySheet(SpreadSheet sheet, Map<DBNation, List<Spyop>> opsAgainstNations, boolean groupByAttacker) {
//        List<Object> header = new ArrayList<>(Arrays.asList(
//                "nation",
//                "alliance",
//                "\uD83C\uDFD9", // cities
//                "\uD83C\uDFD7", // avg_infra
//                "score",
//                "war_policy",
//                "\uD83D\uDD0D",
//                "\uD83D\uDC82",
//                "\u2699",
//                "\u2708",
//                "\u26F5",
//                "\uD83D\uDE80", // rocket
//                "\u2622\uFE0F", // rads
//                "att1",
//                "att2",
//                "att3"
//        ));
//
//        sheet.setHeader(header);
//
//        boolean multipleAAs = false;
//        DBNation prevAttacker = null;
//        for (List<Spyop> spyOpList : opsAgainstNations.values()) {
//            for (Spyop spyop : spyOpList) {
//                DBNation attacker = spyop.attacker;
//                if (prevAttacker != null && prevAttacker.getAlliance_id() != attacker.getAlliance_id()) {
//                    multipleAAs = true;
//                }
//                prevAttacker = attacker;
//            }
//        }
//
//        for (Map.Entry<DBNation, List<Spyop>> entry : opsAgainstNations.entrySet()) {
//            DBNation nation = entry.getKey();
//
//            ArrayList<Object> row = new ArrayList<>();
//            row.add(MarkupUtil.sheetUrl(nation.getNation(), DNS.getUrl(nation.getNation_id(), false)));
//            row.add(MarkupUtil.sheetUrl(nation.getAllianceName(), DNS.getUrl(nation.getAlliance_id(), true)));
//            row.add(nation.getCities());
//            row.add(nation.getAvg_infra());
//            row.add(nation.getScore());
//            row.add(nation.getWarPolicy().name());
//            row.add("" + nation.getSpies());
//
//            row.add(nation.getSoldiers());
//            row.add(nation.getTanks());
//            row.add(nation.getAircraft());
//            row.add(nation.getShips());
//            row.add(nation.getMissiles());
//            row.add(nation.getNukes());
//
//            for (Spyop spyop : entry.getValue()) {
//                DBNation other;
//                if (!groupByAttacker) {
//                    other = spyop.attacker;
//                } else {
//                    other = spyop.defender;
//                }
//                String attStr =other.getNation();
//                String safety = spyop.safety == 3 ? "covert" : spyop.safety == 2 ? "normal" : "quick";
////                attStr += "|" + spyop.operation.name() + "|" + safety + "|" + spyop.spies;
//
//                if (multipleAAs) {
//                    attStr += "|" + spyop.operation.name() + "|" + safety + "|" + spyop.spies + "|" + other.getAllianceName();
//                } else {
//                    attStr += "|" + spyop.operation.name() + "|" + safety + "|" + spyop.spies;
//                }
//                attStr =  MarkupUtil.sheetUrl(attStr, DNS.getUrl(other.getNation_id(), false));
//
//                row.add(attStr);
//            }
//
//            sheet.addRow(row);
//        }
//    }

    // TODO FIXME :||remove deposits sheet
//    @RolePermission(value = {Roles.MILCOM, Roles.INTERNAL_AFFAIRS,Roles.ECON}, any=true)
//    @Command(desc = """
//            Generate a sheet of per nation bank deposit/withdraw activity over a timeframe
//            The columns represent the time unit (either turns or days) when bank transfers occur for each nation
//            A positive value represents a deposit, a negative value represents a withdrawal
//            When both are specified, the net deposit/withdrawal is shown""")
//    public String DepositSheetDate(@Me IMessageIO io, @Me GuildDB db, Set<DBNation> nations,
//                                  boolean deposit,
//                                  boolean withdraw,
//                                  @Arg("Date to start from")
//                                  @Timestamp long start_time,
//                                  @Timestamp long end_time,
//                                  @Switch("d") boolean split_deposit_withdraw,
//                                  @Switch("t") boolean by_turn,
//                                  @Switch("s") SpreadSheet sheet) throws GeneralSecurityException, IOException {
//        if (split_deposit_withdraw && (!deposit || !withdraw)) {
//            throw new IllegalArgumentException("Splitting off and def requires both `off` and `def` to be true");
//        }
//        if (!deposit && !withdraw) {
//            throw new IllegalArgumentException("At least one of `off` or `def` must be true");
//        }
//        long startTurn = TimeUtil.getTurn(start_time);
//        long endTurn = TimeUtil.getTurn(end_time);
//
//        long endDay = TimeUtil.getDay(end_time) + 1;
//        long startDay = TimeUtil.getDay(start_time);
//
//        long numDays = endDay - startDay + 1;
//        if (numDays > 365) {
//            throw new IllegalArgumentException("Too many days: `" + numDays + " (max 365)");
//        }
//        if (endTurn <= startTurn) {
//            throw new IllegalArgumentException("End time must be after start time (2h)");
//        }
//        if (by_turn && endTurn - startTurn > 365) {
//            throw new IllegalArgumentException("Too many turns: `" + (endTurn - startTurn + 1) + " (max 365)");
//        }
//
//        Set<Long> nationIds = new LongOpenHashSet(nations.stream().map(NationOrAllianceOrGuild::getIdLong).collect(Collectors.toSet()));
//        List<Transaction2> records = Locutus.imp().getBankDB().getTransactionsByBySenderOrReceiver(nationIds, nationIds, start_time, end_time);
//        Predicate<Integer> allowNation = f -> nationIds.contains((long) f);
//
//        Map<Integer, Map<Long, Double>> deposited = new Int2ObjectOpenHashMap<>();
//        Map<Integer, Map<Long, Double>> withdrawn = new Int2ObjectOpenHashMap<>();
//        Function<Transaction2, Long> toTime = by_turn ? f -> TimeUtil.getTurn(f.getDate()) : f -> TimeUtil.getDay(f.getDate());
//        for (Transaction2 tx : records) {
//            long time = toTime.apply(tx);
//            if (tx.isSenderNation() && allowNation.test((int) tx.sender_id)) {
//                deposited.computeIfAbsent((int) tx.sender_id, f -> new Long2DoubleOpenHashMap()).merge(time, tx.convertedTotal(), Double::sum);
//            } else if (tx.isReceiverNation() && allowNation.test((int) tx.receiver_id)) {
//                withdrawn.computeIfAbsent((int) tx.receiver_id, f -> new Long2DoubleOpenHashMap()).merge(time, tx.convertedTotal(), Double::sum);
//            }
//        }
//
//        if (sheet == null) {
//            sheet = SpreadSheet.create(db, by_turn ? SheetKey.ACTIVITY_SHEET_TURN : SheetKey.ACTIVITY_SHEET_DAY);
//        }
//
//        long startUnit = by_turn ? startTurn : startDay;
//        long endUnit = by_turn ? endTurn : endDay;
//
//        List<String> header = new ArrayList<>(Arrays.asList("nation", "alliance", "cities"));
//        for (long timeUnit = startUnit; timeUnit <= endUnit; timeUnit++) {
//            long time = by_turn ? TimeUtil.getTimeFromTurn(timeUnit) : TimeUtil.getTimeFromDay(timeUnit);
//            SimpleDateFormat format = by_turn ? TimeUtil.DD_MM_YYYY_HH : TimeUtil.DD_MM_YYYY;
//            header.add(format.format(new Date(time)));
//        }
//
//        sheet.setHeader(header);
//        for (DBNation nation : nations) {
//            Map<Long, Double> depActivity = deposited.getOrDefault(nation.getNation_id(), new Long2DoubleOpenHashMap());
//            Map<Long, Double> withActivity = withdrawn.getOrDefault(nation.getNation_id(), new Long2DoubleOpenHashMap());
//            Function<Long, String> formatFunc;
//            if (split_deposit_withdraw) {
//                formatFunc = f -> {
//                    double depAmt = depActivity.getOrDefault(f, 0d);
//                    double withAmt = withActivity.getOrDefault(f, 0d);
//                    if (depAmt == 0 && withAmt == 0) return "";
//                    return MathMan.format(depAmt) + "/" + MathMan.format(withAmt);
//                };
//            } else {
//                formatFunc = f -> {
//                    double amt = (depActivity.getOrDefault(f, 0d) - withActivity.getOrDefault(f, 0d));
//                    return amt == 0 ? "" : MathMan.format(amt);
//                };
//            }
//            header.set(0, MarkupUtil.sheetUrl(nation.getNation(), nation.getUrl()));
//            header.set(1, MarkupUtil.sheetUrl(nation.getAllianceName(), nation.getAllianceUrl()));
//            header.set(2, nation.getCities() + "");
//            int index = 3;
//            for (long timeUnit = startUnit; timeUnit <= endUnit; timeUnit++) {
//                header.set(index, formatFunc.apply(timeUnit));
//                index++;
//            }
//            sheet.addRow(header);
//        }
//
//        sheet.updateClearCurrentTab();
//        sheet.updateWrite();
//
//        sheet.attach(io.create(), "activity").send();
//        return null;
//    }

    @RolePermission(value = {Roles.MILCOM, Roles.INTERNAL_AFFAIRS,Roles.ECON}, any=true)
    @Command(desc = "Generate a sheet of nation login activity from a nation id over a timeframe\n" +
            "The columns are the 7 days of the week and then turns of the day (12)\n" +
            "Note: use the other activity sheet need info of a deleted nation\n" +
            "Days represent the % of that day a nation logs in (UTC)\n" +
            "Numbers represent the % of that turn a nation logs in")
    public String ActivitySheetFromId(@Me IMessageIO io, @Me GuildDB db, int nationId,
                                      @Arg("Date to start from")
                                      @Default("2w") @Timestamp long startTime,
                                      @Switch("e") @Timestamp Long endTime,
                                      @Switch("s") SpreadSheet sheet) throws GeneralSecurityException, IOException {
        Set<DBNation> nations = Set.of(DBNation.getOrCreate(nationId));
        return activitySheet(io, db, nations, startTime, endTime, sheet);
    }

    private String activitySheet(IMessageIO channel, GuildDB db, Set<DBNation> nations,
                               long startTime,
                               Long endTime,
                               SpreadSheet sheet) throws GeneralSecurityException, IOException {
        if (sheet == null) {
            sheet = SpreadSheet.create(db, SheetKey.ACTIVITY_SHEET);
        }
        List<Object> header = new ArrayList<>(Arrays.asList(
                "nation",
                "alliance",
                "land", // cities
                "infra", // avg_infra
                "score",
                "Mo",
                "Tu",
                "We",
                "Th",
                "Fr",
                "Sa",
                "Su"
        ));
        for (int i = 0; i < 12; i++) {
            header.add((i + 1) + "");
        }

        sheet.setHeader(header);

        for (DBNation nation : nations) {

            header.set(0, MarkupUtil.sheetUrl(nation.getNation(), DNS.getUrl(nation.getNation_id(), false)));
            header.set(1, MarkupUtil.sheetUrl(nation.getAllianceName(), DNS.getUrl(nation.getAlliance_id(), true)));
            header.set(2, nation.getLand());
            header.set(3, nation.getInfra());
            header.set(4, nation.getScore());

            Activity activity = nation.getActivity(14 * 12);
            double[] byDay = activity.getByDay();
            double[] byDayTurn = activity.getByDayTurn();

            for (int i = 0; i < byDay.length; i++) {
                header.set(5 + i, byDay[i] * 100);
            }

            for (int i = 0; i < byDayTurn.length; i++) {
                header.set(5 + byDay.length + i, byDayTurn[i] * 100);
            }

            sheet.addRow(header);
        }

        sheet.updateClearCurrentTab();
        sheet.updateWrite();

        sheet.attach(channel.create(), "activity").send();
        return null;
    }

    @RolePermission(value = {Roles.MILCOM, Roles.INTERNAL_AFFAIRS,Roles.ECON}, any=true)
    @Command(desc = "Generate a sheet of per nation war declare activity over a timeframe\n" +
            "The columns represent the time unit (either turns or days) when wars are declared for each nation")
    public String WarDecSheetDate(@Me IMessageIO io, @Me GuildDB db, Set<DBNation> nations,
                                    boolean off,
                                    boolean def,
                                    @Arg("Date to start from")
                                    @Timestamp long start_time,
                                    @Timestamp long end_time,
                                  @Switch("d") boolean split_off_def,
                                    @Switch("t") boolean by_turn,
                                    @Switch("s") SpreadSheet sheet) throws GeneralSecurityException, IOException {
        if (split_off_def && (!off || !def)) {
            throw new IllegalArgumentException("Splitting off and def requires both `off` and `def` to be true");
        }
        if (!off && !def) {
            throw new IllegalArgumentException("At least one of `off` or `def` must be true");
        }
        long startHour = TimeUtil.getHour(start_time);
        long endHour = TimeUtil.getHour(end_time);

        long endDay = TimeUtil.getDay(end_time) + 1;
        long startDay = TimeUtil.getDay(start_time);

        long numDays = endDay - startDay + 1;
        if (numDays > 365) {
            throw new IllegalArgumentException("Too many days: `" + numDays + " (max 365)");
        }
        if (startHour <= endHour) {
            throw new IllegalArgumentException("End time must be after start time (1h)");
        }
        if (by_turn && endHour - startHour > 365) {
            throw new IllegalArgumentException("Too many hours: `" + (endHour - startHour + 1) + " (max 365)");
        }

        Set<Integer> nationIds = new IntOpenHashSet(nations.stream().map(DBNation::getNation_id).collect(Collectors.toSet()));
        Set<DBWar> wars = Locutus.imp().getWarDb().queryAttacks().withWarsForNationOrAlliance(nationIds::contains, null, null).between(start_time, end_time).getWars();
        Predicate<Integer> allowNation = nationIds::contains;

        Map<Integer, Map<Long, Integer>> offWarsByTime = new Int2ObjectOpenHashMap<>();
        Map<Integer, Map<Long, Integer>> defWarsByTime = new Int2ObjectOpenHashMap<>();
        Function<DBWar, Long> toTime = by_turn ? war -> TimeUtil.getHour(war.getDate()) : war -> TimeUtil.getDay(war.getDate());
        for (DBWar war : wars) {
            long time = toTime.apply(war);
            if (allowNation.test(war.getAttacker_id()) && off) {
                offWarsByTime.computeIfAbsent(war.getAttacker_id(), f -> new Long2IntOpenHashMap()).merge(time, 1, Integer::sum);
            }
            if (allowNation.test(war.getDefender_id()) && def) {
                defWarsByTime.computeIfAbsent(war.getDefender_id(), f -> new Long2IntOpenHashMap()).merge(time, 1, Integer::sum);
            }
        }

        if (sheet == null) {
            sheet = SpreadSheet.create(db, by_turn ? SheetKey.ACTIVITY_SHEET_TURN : SheetKey.ACTIVITY_SHEET_DAY);
        }

        long startUnit = by_turn ? startHour : startDay;
        long endUnit = by_turn ? endHour : endDay;

        List<String> header = new ArrayList<>(Arrays.asList("nation", "alliance", "infra"));
        for (long timeUnit = startUnit; timeUnit <= endUnit; timeUnit++) {
            long time = by_turn ? TimeUtil.getTimeFromHour(timeUnit) : TimeUtil.getTimeFromDay(timeUnit);
            SimpleDateFormat format = by_turn ? TimeUtil.DD_MM_YYYY_HH : TimeUtil.DD_MM_YYYY;
            header.add(format.format(new Date(time)));
        }

        sheet.setHeader(header);
        for (DBNation nation : nations) {
            Map<Long, Integer> offActivity = offWarsByTime.getOrDefault(nation.getNation_id(), new Long2IntOpenHashMap());
            Map<Long, Integer> defActivity = defWarsByTime.getOrDefault(nation.getNation_id(), new Long2IntOpenHashMap());
            Function<Long, String> formatFunc;
            if (split_off_def) {
                formatFunc = f -> {
                    int offAmt = offActivity.getOrDefault(f, 0);
                    int defAmt = defActivity.getOrDefault(f, 0);
                    if (offAmt == 0 && defAmt == 0) return "";
                    return offAmt + "/" + defAmt;
                };
            } else {
                formatFunc = f -> {
                    int amt = (offActivity.getOrDefault(f, 0) + defActivity.getOrDefault(f, 0));
                    return amt == 0 ? "" : amt + "";
                };
            }
            header.set(0, MarkupUtil.sheetUrl(nation.getNation(), nation.getUrl()));
            header.set(1, MarkupUtil.sheetUrl(nation.getAllianceName(), nation.getAllianceUrl()));
            header.set(2, nation.getInfra() + "");
            int index = 3;
            for (long timeUnit = startUnit; timeUnit <= endUnit; timeUnit++) {
                header.set(index, formatFunc.apply(timeUnit));
                index++;
            }
            sheet.addRow(header);
        }

        sheet.updateClearCurrentTab();
        sheet.updateWrite();

        sheet.attach(io.create(), "activity").send();
        return null;
    }

    @RolePermission(value = {Roles.MILCOM, Roles.INTERNAL_AFFAIRS,Roles.ECON}, any=true)
    @Command(desc = "Generate a sheet of nation login activity from a nation id over a timeframe\n" +
            "The sheet columns are the dates with the values being either 1 or 0 for logging in or not")
    public String ActivitySheetDate(@Me IMessageIO io, @Me GuildDB db, Set<DBNation> nations,
                                    @Arg("Date to start from")
                                    @Timestamp long start_time,
                                    @Timestamp long end_time,
                                    @Switch("t") boolean by_turn,
                                    @Switch("s") SpreadSheet sheet) throws GeneralSecurityException, IOException {
        long startHour = TimeUtil.getHour(start_time);
        long endHour = TimeUtil.getHour(end_time);

        long endDay = TimeUtil.getDay(end_time) + 1;
        long startDay = TimeUtil.getDay(start_time);

        long numDays = endDay - startDay + 1;
        if (numDays > 365) {
            throw new IllegalArgumentException("Too many days: `" + numDays + " (max 365)");
        }
        if (endHour <= startHour) {
            throw new IllegalArgumentException("End time must be after start time (2h)");
        }
        if (by_turn && endHour - startHour > 365) {
            throw new IllegalArgumentException("Too many turns: `" + (endHour - startHour + 1) + " (max 365)");
        }

        Set<Integer> nationIds = new IntOpenHashSet(nations.stream().map(DBNation::getNation_id).collect(Collectors.toSet()));
        Predicate<Integer> allowNation = nationIds::contains;

        NationDB natDb = Locutus.imp().getNationDB();
        Map<Integer, Set<Long>> activityByTime;
        if (by_turn) {
            activityByTime = natDb.getActivityByTurn(startHour, endHour, allowNation);
        } else {
            activityByTime = natDb.getActivityByDay(TimeUtil.getTimeFromHour(startHour), TimeUtil.getTimeFromHour(endHour + 23), allowNation);
        }

        if (sheet == null) {
            sheet = SpreadSheet.create(db, by_turn ? SheetKey.ACTIVITY_SHEET_TURN : SheetKey.ACTIVITY_SHEET_DAY);
        }

        long startUnit = by_turn ? startHour : startDay;
        long endUnit = by_turn ? endHour : endDay;

        List<String> header = new ArrayList<>(Arrays.asList("nation", "alliance", "infra"));
        for (long timeUnit = startUnit; timeUnit <= endUnit; timeUnit++) {
            long time = by_turn ? TimeUtil.getTimeFromHour(timeUnit) : TimeUtil.getTimeFromDay(timeUnit);
            SimpleDateFormat format = by_turn ? TimeUtil.DD_MM_YYYY_HH : TimeUtil.DD_MM_YYYY;
            header.add(format.format(new Date(time)));
        }

        sheet.setHeader(header);
        for (DBNation nation : nations) {
            Set<Long> activity = activityByTime.get(nation.getNation_id());
            header.set(0, MarkupUtil.sheetUrl(nation.getNation(), nation.getUrl()));
            header.set(1, MarkupUtil.sheetUrl(nation.getAllianceName(), nation.getAllianceUrl()));
            header.set(2, nation.getInfra() + "");
            int index = 3;
            for (long timeUnit = startUnit; timeUnit <= endUnit; timeUnit++) {
                header.set(index, activity != null && activity.contains(timeUnit) ? "1" : "");
                index++;
            }
            sheet.addRow(header);
        }

        sheet.updateClearCurrentTab();
        sheet.updateWrite();

        sheet.attach(io.create(), "activity").send();
        return null;
    }

    @RolePermission(value = {Roles.MILCOM, Roles.INTERNAL_AFFAIRS,Roles.ECON}, any=true)
    @Command(desc = "Generate a sheet of nation login activity from a nation id over a timeframe\n" +
            "The columns are the 7 days of the week and then turns of the day (12)\n" +
            "Days represent the % of that day a nation logs in (UTC)\n" +
            "Numbers represent the % of that turn a nation logs in")
    public String ActivitySheet(@Me IMessageIO io, @Me GuildDB db, Set<DBNation> nations,
                                @Arg("Date to start from")
                                @Default("2w") @Timestamp long startTime,
                                @Switch("e") @Timestamp Long endTime,
                                @Switch("s") SpreadSheet sheet) throws GeneralSecurityException, IOException {
        if (sheet == null) {
            sheet = SpreadSheet.create(db, SheetKey.ACTIVITY_SHEET);
        }
        List<Object> header = new ArrayList<>(Arrays.asList(
                "nation",
                "alliance",
                "land",
                "infra",
                "score",
                "Mo",
                "Tu",
                "We",
                "Th",
                "Fr",
                "Sa",
                "Su"
        ));
        for (int i = 0; i < 12; i++) {
            header.add((i + 1) + "");
        }

        sheet.setHeader(header);

        for (DBNation nation : nations) {

            header.set(0, MarkupUtil.sheetUrl(nation.getNation(), DNS.getUrl(nation.getNation_id(), false)));
            header.set(1, MarkupUtil.sheetUrl(nation.getAllianceName(), DNS.getUrl(nation.getAlliance_id(), true)));
            header.set(2, nation.getLand());
            header.set(3, nation.getInfra());
            header.set(4, nation.getScore());

            Activity activity;
            if (startTime == 0) {
                activity = nation.getActivity();
            } else {
                long startTurn = TimeUnit.MILLISECONDS.toHours(startTime);
                long endTurn = endTime == null ? Long.MAX_VALUE : TimeUnit.MILLISECONDS.toHours(endTime);
                activity = nation.getActivity(startTurn, endTurn);
            }
            double[] byDay = activity.getByDay();
            double[] byDayTurn = activity.getByDayTurn();

            for (int i = 0; i < byDay.length; i++) {
                header.set(5 + i, byDay[i] * 100);
            }

            for (int i = 0; i < byDayTurn.length; i++) {
                header.set(5 + byDay.length + i, byDayTurn[i] * 100);
            }

            sheet.addRow(header);
        }

        sheet.updateClearCurrentTab();
        sheet.updateWrite();

        sheet.attach(io.create(), "activity").send();
        return null;
    }

    @RolePermission(value = {Roles.MILCOM, Roles.INTERNAL_AFFAIRS,Roles.ECON}, any=true)
    @Command(desc = "Generate a sheet of alliance/nation/city military unit and building counts (MMR)")
    public String MMRSheet(@Me IMessageIO io, @Me GuildDB db, Set<DBNation> nations, @Switch("s") SpreadSheet sheet,
                           @Switch("f") boolean forceUpdate) throws GeneralSecurityException, IOException {
        for (DBNation nation : nations) {
            if (!db.isAllianceId(nation.getAlliance_id())) {
                throw new IllegalArgumentException("Nation " + nation.getMarkdownUrl() + " is not in the guild's alliance: " + db.getAllianceIds());
            }
        }

        if (sheet == null) sheet = SpreadSheet.create(db, SheetKey.MMR_SHEET);
        List<Object> header = new ArrayList<>(Arrays.asList(
                "nation",
                "alliance",
                "infra",
                "land",
                "score",
                "army",
                "air",
                "navy"
        ));
        for (MilitaryUnit unit : MilitaryUnit.values) {
            header.add(unit.name());
        }
        for (MilitaryUnitType type : MilitaryUnitType.values) {
            header.add(type.name());
        }

        sheet.setHeader(header);

        long now = System.currentTimeMillis() - (forceUpdate ? 0 : TimeUnit.HOURS.toMillis(1));
        for (DBNation nation : nations) {
            NationPrivate privateData = nation.getPrivateData();
            System.out.println("Get buildings");
            Map<Building, Integer> buildings = privateData.getBuildings(now);
            Map<MilitaryUnit, Integer> military = privateData.getMilitary(now);
            Map<MilitaryUnit, Double> quality = privateData.getMilitaryQuality(now);
            Map<MilitaryUnitType, Integer> capacity = privateData.getMilitaryCapacity(now);
//            List<AllianceMemberInventory> inventory = privateData.getInventory(now);

            header.set(0, MarkupUtil.sheetUrl(nation.getNation(), DNS.getUrl(nation.getNation_id(), false)));
            header.set(1, MarkupUtil.sheetUrl(nation.getAllianceName(), DNS.getUrl(nation.getAlliance_id(), true)));
            header.set(2, nation.getInfra());
            header.set(3, nation.getLand());
            header.set(4, nation.getScore());
            header.set(5, buildings.getOrDefault(Building.ARMY_BASES, 0) + "");
            header.set(6, buildings.getOrDefault(Building.AIR_BASES, 0) + "");
            header.set(7, buildings.getOrDefault(Building.NAVAL_BASES, 0) + "");

            for (MilitaryUnit unit : MilitaryUnit.values) {
                int amt = military.getOrDefault(unit, 0);
                double qualityVal = quality.getOrDefault(unit, 0d);
                String unitStr = amt + "x" + MathMan.format(qualityVal);
                header.set(8 + unit.ordinal(), unitStr);
            }
            for (MilitaryUnitType type : MilitaryUnitType.values) {
                int capacityCap = capacity.getOrDefault(type, 0);
                int capacityUsed = type.getUsedCapacity(military);
                String capacityStr = capacityUsed + "/" + capacityCap;
                header.set(8 + MilitaryUnit.values.length + type.ordinal(), capacityStr);
            }
            sheet.addRow(header);
        }
        sheet.updateClearCurrentTab();
        sheet.updateWrite();
        String response = "";
        if (!forceUpdate) response += "\nNote: Results may be outdated, add `-f` to update.";
        sheet.attach(io.create(), "mmr", response).send();
        return null;
    }

    // TODO FIXME :||remove validate spy blitz sheet
//    @RolePermission(Roles.MILCOM)
//    @Command(desc = "Run checks on a spy blitz sheet.\n" +
//            "Checks that all nations are in range of their spy blitz targets and that they have no more than the provided number of offensive operations.\n" +
//            "Add `true` for the day-change argument to double the offensive op limit")
//    public String validateSpyBlitzSheet(@Me GuildDB db, @Default SpreadSheet sheet,
//                                        @Arg("If the sheet is for attacks at day change")
//                                        @Default("false") boolean dayChange,
//                                        @Arg("Only allow attacking these nations")
//                                        @Default("*") Set<DBNation> filter,
//                                        @Arg("Parse nation leader instead of nation name") @Switch("l") boolean useLeader) throws GeneralSecurityException, IOException {
//        if (sheet == null) {
//            db.getOrThrow(SheetKey.SPYOP_SHEET);
//            sheet = SpreadSheet.create(db, SheetKey.SPYOP_SHEET);
//        }
//        StringBuilder response = new StringBuilder();
//
//        Function<DBNation, Integer> maxWarsFunc = new Function<DBNation, Integer>() {
//            @Override
//            public Integer apply(DBNation nation) {
//                int offSlots = 1;
//                if (nation.hasProject(Projects.INTELLIGENCE_AGENCY)) offSlots++;
//                if (dayChange) offSlots *= 2;
//                return offSlots;
//            }
//        };
//
//        Function<DBNation, Boolean> isValidTarget = n -> filter.contains(n);
//
//        AtomicBoolean hasErrors = new AtomicBoolean(false);
//        BlitzGenerator.getTargets(sheet, useLeader, 0, maxWarsFunc, 0.4, 2.5, false, false, true, isValidTarget, new BiConsumer<Map.Entry<DBNation, DBNation>, String>() {
//            @Override
//            public void accept(Map.Entry<DBNation, DBNation> dbNationDBNationEntry, String msg) {
//                response.append(msg + "\n");
//                hasErrors.set(true);
//            }
//        }, info -> response.append("```\n" + info.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining("\n")) + "\n```").append("\n"));
//
//        if (!hasErrors.get()) response.append("\n**All checks passed**");
//
//        return response.toString();
//    }

    @Command(desc = "List war rooms for an ally or enemy")
    public String warRoomList(@Me WarCategory warCategory, DBNation nation) {
        Map<Integer, WarCategory.WarRoom> roomMap = warCategory.getWarRoomMap();
        WarCategory.WarRoom room = roomMap.get(nation.getId());
        Function<WarCategory.WarRoom, String> toString = f -> {
            StringBuilder response = new StringBuilder();
            String mention = f.getChannelMention();
            if (mention == null) {
                response.append("Unknown channel for: " + f.target.getMarkdownUrl());
            } else {
                response.append(mention + " - " + f.target.getMarkdownUrl() + " | " + f.target.getAllianceUrlMarkup(true));
                for (DBNation participant : f.getParticipants()) {
                    response.append("\n- " + participant.getMarkdownUrl() + " | " + participant.getAllianceUrlMarkup(true));
                }
            }
            return response.toString();
        };
        if (room != null) {
            return toString.apply(room);
        } else {
            Set<WarCategory.WarRoom> rooms = new LinkedHashSet<>();
            for (Map.Entry<Integer, WarCategory.WarRoom> entry : roomMap.entrySet()) {
                room = entry.getValue();
                if (room.isParticipant(nation, false)) {
                    rooms.add(room);
                }
            }
            if (rooms.isEmpty()) {
                return "No war rooms found for: " + nation.getNationUrlMarkup(true) + " | " + nation.getAllianceUrlMarkup(true);
            }
            StringBuilder response = new StringBuilder();
            for (WarCategory.WarRoom entry : rooms) {
                response.append(toString.apply(entry) + "\n");
            }
            return response.toString();
        }
    }

    // TODO FIXME :||remove war room sheet
//    @Command(desc = "Create war rooms from a blitz sheet")
//    @RolePermission(Roles.MILCOM)
//    public String warRoomSheet(@Me WarCategory warCat, @Me User author, @Me Guild guild, @Me JSONObject command, @Me IMessageIO io,
//                               SpreadSheet blitzSheet,
//                               @Arg("Custom message to send in each created war room")
//                               @Default String customMessage,
//                               @Arg("If the default counter message should be sent")
//                               @Switch("c") boolean addCounterMessage,
//                               @Arg("If the added member should be pinged in the channel")
//                               @Switch("p") boolean ping,
//                               @Arg("If the member should be added to the war room")
//                               @Switch("m") boolean addMember,
//                               @Arg("The nations from the blitz sheet to create war rooms for\n" +
//                                       "Defaults to everyone")
//                               @Switch("a") Set<DBNation> allowedNations,
//                               @Arg("The row the blitz sheet header is one\n" +
//                                       "Defaults to first row")
//                               @Switch("h") Integer headerRow,
//                               @Arg("Parse nation leader instead of nation name") @Switch("l") boolean useLeader,
//                               @Switch("f") boolean force) {
//        if (headerRow == null) headerRow = 0;
//
//        IMessageBuilder msg = io.create();
//
//        StringBuilder response = new StringBuilder();
//        AtomicInteger errors = new AtomicInteger();
//        StringBuilder body = new StringBuilder();
//        Map<DBNation, Set<DBNation>> targets = BlitzGenerator.getTargets(blitzSheet, useLeader, headerRow, f -> 3, 0.75, DNS.WAR_RANGE_MAX_MODIFIER, true, true, false, f -> true,
//                (dbNationDBNationEntry, s) -> {response.append(s).append("\n"); errors.incrementAndGet();},
//                info -> body.append("```\n" + info.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining("\n")) + "\n```").append("\n"));
//        if (!force) {
//            msg = io.create();
//            String title = "Create";
//            if (errors.get() > 0) {
//                title += " with " + errors.get() + " errors";
//                if (response.length() > 2000) {
//                    msg = msg.file(errors.get() + "_errors.txt", response.toString());
//                    body.append("\n\n**See `errors.txt` for details**");
//                } else {
//                    body.append("\n\n__**Errors:**__\n").append(response);
//                }
//            }
//            msg.embed(title, body.toString())
//                    .confirmation(command).send();
//            return null;
//        }
//
//        msg.append("Creating channels...").send();
//
//        if (allowedNations != null) {
//            for (Map.Entry<DBNation, Set<DBNation>> entry : targets.entrySet()) {
//                entry.getValue().removeIf(f -> !allowedNations.contains(f));
//            }
//        }
//        targets.entrySet().removeIf(f -> f.getValue().isEmpty());
//
//        Set<GuildMessageChannel> channels = new LinkedHashSet<>();
//        for (Map.Entry<DBNation, Set<DBNation>> entry : targets.entrySet()) {
//            DBNation target = entry.getKey();
//            Set<DBNation> attackers = entry.getValue();
//
//            WarCategory.WarRoom channel = WarRoom.createChannel(warCat, author, guild, s -> response.append(s).append("\n"), ping, addMember, addCounterMessage, target, attackers);
//
//            try {
//                if (customMessage != null) {
//                    RateLimitUtil.queue(channel.getChannel().sendMessage(customMessage));
//                }
//
//                channels.add(channel.getChannel());
//            } catch (Throwable e) {
//                e.printStackTrace();
//                response.append(e.getMessage());
//            }
//        }
//
//        return "Created " + channels.size() + " for " + targets.size() + " targets";
//    }

    // TODO FIXME :||remove mailtargets !!important
//    @RolePermission(Roles.MILCOM)
//    @Command(desc = "Send spy or war blitz sheets to individual nations\n" +
//            "Blitz Sheet Columns: `nation`, `attacker 1`, `attacker 2`, `attacker 3`")
//    public String sendTargets(@Me GuildDB db, @Me Guild guild, @Me JSONObject command, @Me User author, @Me IMessageIO channel, @Me DBNation me,
//                              @Arg("Url of the war blitz sheet to send")
//                              @Default SpreadSheet blitzSheet,
//                              @Arg("Url of the spy sheet to send")
//                              @Default SpreadSheet spySheet,
//                              @Arg("What attacker nations to send to")
//                              @Default("*") Set<DBNation> allowedNations,
//                              @Arg("What defender nations to include (default: all)")
//                              @Default Set<DBNation> allowedEnemies,
//                              @Arg("Text to prepend to the target instructions being sent")
//                              @Default("") String header,
//                              @Arg("Send from the api key registered to the guild") @Switch("g") boolean sendFromGuildAccount,
//                              @Arg("The api key to use to send the mail") @Switch("a") String apiKey,
//                              @Arg("Hide the default blurb from the message")
//                              @Switch("b") boolean hideDefaultBlurb,
//                              @Switch("f") boolean force,
//                              @Arg("Parse nation leader instead of nation name") @Switch("l") boolean useLeader,
//                              @Arg("Send instructions as direct message on discord")
//                              @Switch("d") boolean dm) throws IOException, GeneralSecurityException {
//        if(header != null) {
//            GPTUtil.checkThrowModeration(header);
//        }
//
//        ApiKeyPool.ApiKey myKey = me.getApiKey(false);
//        ApiKeyPool key = null;
//        if (apiKey != null) {
//            Integer nation = Locutus.imp().getDiscordDB().getNationFromApiKey(apiKey);
//            if (nation == null) return "Invalid API key";
//            key = ApiKeyPool.create(nation, apiKey);
//        }
//        if (key == null) {
//            if ((sendFromGuildAccount || myKey == null)) {
//                key = db.getMailKey();
//            } else {
//                key = ApiKeyPool.builder().addKey(myKey).build();
//            }
//        }
//        if (key == null){
//            return "No api key found. Please use" + GuildKey.API_KEY.getCommandMention() + " or specify `sendFromGuildAccount` or `apiKey` in the command";
//        }
//
//        if (header != null && !header.isEmpty() && !Roles.MAIL.has(author, guild)) {
//            return "You need the MAIL role on discord (see " + CM.role.setAlias.cmd.toSlashMention() + ") to add the custom message: `" + header + "`";
//        }
//        Map<DBNation, Set<DBNation>> warDefAttMap = new HashMap<>();
//        Map<DBNation, Set<DBNation>> spyDefAttMap = new HashMap<>();
//        Map<DBNation, Set<Spyop>> spyOps = new HashMap<>();
//
//        Map<String, Object> blitzSheetSummary = new LinkedHashMap<>();
//        Map<String, Object> spySheetSummary = new LinkedHashMap<>();
//
//        if (dm && !Roles.MAIL.hasOnRoot(author)) return "You do not have permission to dm users";
//
//        if (blitzSheet != null) {
//            warDefAttMap = BlitzGenerator.getTargets(blitzSheet, useLeader, 0, f -> 2, 0.75, DNS.WAR_RANGE_MAX_MODIFIER, true, true, false, f -> true, (a, b) -> {}, (a) -> blitzSheetSummary.putAll(a));
//        }
//
//        if (spySheet != null) {
//            try {
//                spyDefAttMap = BlitzGenerator.getTargets(spySheet, useLeader, 0, f -> 2, 0.4, 2.5, false, false, true, f -> true, (a, b) -> {}, (a) -> spySheetSummary.putAll(a));
//                spyOps = SpyBlitzGenerator.getTargets(spySheet, 0);
//            } catch (NullPointerException e) {
//                spyDefAttMap = BlitzGenerator.getTargets(spySheet, useLeader, 4, f -> 2, 0.4, 2.5, false, false, true, f -> true, (a, b) -> {}, (a) -> spySheetSummary.putAll(a));
//                spyOps = SpyBlitzGenerator.getTargets(spySheet, 4);
//            }
//        }
//
//        if (allowedEnemies != null) {
//            for (Map.Entry<DBNation, Set<DBNation>> entry : warDefAttMap.entrySet()) {
//                entry.getValue().removeIf(f -> !allowedEnemies.contains(f));
//            }
//            for (Map.Entry<DBNation, Set<DBNation>> entry : spyDefAttMap.entrySet()) {
//                entry.getValue().removeIf(f -> !allowedEnemies.contains(f));
//            }
//        }
//
//        Map<DBNation, Set<DBNation>> warAttDefMap = BlitzGenerator.reverse(warDefAttMap);
//        Map<DBNation, Set<DBNation>> spyAttDefMap = BlitzGenerator.reverse(spyDefAttMap);
//        Set<DBNation> allAttackers = new LinkedHashSet<>();
//        allAttackers.addAll(warAttDefMap.keySet());
//        allAttackers.addAll(spyAttDefMap.keySet());
//
//        String date = TimeUtil.YYYY_MM_DD.format(ZonedDateTime.now());
//        String subject = "Targets-" + date + "/" + channel.getIdLong();
//
//        String blurb = "BE ACTIVE ON DISCORD. Additional attack instructions may be in your war room\n" +
//                "\n" +
//                "This is an alliance war, not a counter. The goal is battlefield control:\n" +
//                "1. Try to raid wars just before day change (day change if possible)\n" +
//                "2. If you have ground control, further attacks with tanks kills aircraft\n" +
//                "3. If you have tanks and can get ground control, do ground attacks to kill planes\n" +
//                "4. Get air control to halve enemy tank strength\n" +
//                "5. You can rebuy units inbetween each attack\n" +
//                "6. Do not waste attacks destroying infra or minimal units\n" +
//                "7. Be efficient with your attacks and try NOT to get active enemies to 0 resistance\n" +
//                "8. You can buy more ships when enemy planes are weak, to avoid naval losses\n" +
//                "9. Some wars you may get beiged in, that is OKAY";
//
//        long start = System.currentTimeMillis();
//
//        Map<DBNation, Map.Entry<String, String>> mailTargets = new HashMap<>();
//        int totalSpyTargets = 0;
//        int totalWarTargets = 0;
//
//        int sent = 0;
//        for (DBNation attacker : allAttackers) {
//            if (!allowedNations.contains(attacker)) continue;
//
//            List<DBNation> myAttackOps = new ArrayList<>(warAttDefMap.getOrDefault(attacker, Collections.emptySet()));
//            List<Spyop> mySpyOps = new ArrayList<>(spyOps.getOrDefault(attacker, Collections.emptySet()));
//            if (myAttackOps.isEmpty() && mySpyOps.isEmpty()) continue;
//
//            sent++;
//
//            StringBuilder mail = new StringBuilder();
//            header = header.replace("\\n", "\n");
//            mail.append(header).append("\n");
//
//            if (!myAttackOps.isEmpty()) {
//                if (!hideDefaultBlurb) {
//                    mail.append(blurb + "\n");
//                }
//                mail.append("\n");
//
//                mail.append("Your nation:\n");
//                mail.append(getStrengthInfo(attacker) + "\n");
//                mail.append("\n");
//
//                for (int i = 0; i < myAttackOps.size(); i++) {
//                    totalWarTargets++;
//                    DBNation defender = myAttackOps.get(i);
//                    mail.append((i + 1) + ". War Target: " + MarkupUtil.htmlUrl(defender.getNation(), defender.getUrl()) + "\n");
//                    mail.append(getStrengthInfo(defender) + "\n"); // todo
//
//                    Set<DBNation> others = new LinkedHashSet<>(warDefAttMap.get(defender));
//                    others.remove(attacker);
//                    if (!others.isEmpty()) {
//                        Set<String> allies = new LinkedHashSet<>();
//                        for (DBNation other : others) {
//                            allies.add(other.getNation());
//                        }
//                        mail.append("Joining you: " + StringMan.join(allies, ",") + "\n");
//                    }
//                    mail.append("\n");
//                }
//            }
//
//            if (!mySpyOps.isEmpty()) {
//                int intelOps = 0;
//                int killSpies = 0;
////                int missileNuke = 0;
//                double cost = 0;
//                for (Spyop op : mySpyOps) {
//                    if (op.operation == SpyCount.Operation.INTEL) intelOps++;
//                    if (op.operation == SpyCount.Operation.SPIES) killSpies++;
////                    if (op.operation == SpyCount.Operation.MISSILE && op.defender.getMissiles() <= 4) missileNuke++;
////                    if (op.operation == SpyCount.Operation.NUKE && op.defender.getNukes() <= 4) missileNuke++;
//                    else
//                        cost += SpyCount.opCost(op.spies, op.safety);
//                }
//
//                mail.append("\n");
//                mail.append("Espionage targets: (costs >$" + MathMan.format(cost) + ")\n");
//
//                if (intelOps == 0) {
//                    mail.append("- These are NOT gather intelligence ops. XD\n");
//                    mail.append("- If these targets don't work, reply with the word `more` and i'll send you some more targets\n");
//                }
//                if (killSpies != 0) {
//                    mail.append("- If selecting (but not executing) 1 spy on quick (gather intel) yields >50% odds, it means the enemy has no spies left.\n");
//                    mail.append("- If an enemy has 0 spies, you can use 5|spies|quick (99%) for killing units.\n");
//                }
//
//                if (intelOps != myAttackOps.size()) {
//                    mail.append("- Results may be outdated when you read it, so check they still have units to spy!\n");
//                }
//
//                mail.append(
//                        "- If the op doesn't require it (and it says >50%), you don't have to use more spies or covert\n" +
//                                "- Reply to this message with any spy reports you do against enemies (even if not these targets)\n" +
//                                "- Remember to buy spies every day :D\n\n");
//
//                String baseUrl = "https://diplomacyandstrife.com/nation/";
//                for (int i = 0; i < mySpyOps.size(); i++) {
//                    totalSpyTargets++;
//                    Spyop spyop = mySpyOps.get(i);
//                    String safety = spyop.safety == 3 ? "covert" : spyop.safety == 2 ? "normal" : "quick";
//
//                    String name = spyop.defender.getNation() + " | " + spyop.defender.getAllianceName();
//                    String nationUrl = MarkupUtil.htmlUrl(name, "https://tinyurl.com/y26weu7d/id=" + spyop.defender.getNation_id());
//
//                    String spyUrl = baseUrl + spyop.defender.getNation_id();
//                    String attStr = spyop.operation.name() + "|" + safety + "|" + spyop.spies + "\"";
//                    mail.append((i + 1) + ". " + nationUrl + " | ");
//                    if (spyop.operation != SpyCount.Operation.INTEL) mail.append("kill ");
//                    else mail.append("gather ");
//                    mail.append(spyop.operation.name().toLowerCase() + " using " + spyop.spies + " spies on " + safety);
//
//                    mail.append("\n");
//                }
//            }
//
//            String body = mail.toString().replace("\n","<br>");
//
//            mailTargets.put(attacker, new AbstractMap.SimpleEntry<>(subject, body));
//        }
//
//        if (!force) {
//            String title = totalWarTargets + " wars & " + totalSpyTargets + " spyops";
//
//            Set<Integer> alliances = new LinkedHashSet<>();
//            for (DBNation nation : mailTargets.keySet()) alliances.add(nation.getAlliance_id());
//            String embedTitle = title + " to " + mailTargets.size() + " nations";
//            if (alliances.size() != 1) embedTitle += " in " + alliances.size() + " alliances";
//
//            StringBuilder body = new StringBuilder();
//            body.append("**subject:** `" + subject + "`\n\n");
//            if (!blitzSheetSummary.isEmpty()) {
//                body.append("War Blitz Sheet Summary:\n" + blitzSheetSummary.entrySet().stream().map(e -> "- " + e.getKey() + ": `" + e.getValue() + "`").collect(Collectors.joining("\n")) + "\n");
//            }
//            if (!spySheetSummary.isEmpty()) {
//                body.append("Spy Blitz Sheet Summary:\n" + spySheetSummary.entrySet().stream().map(e -> "- " + e.getKey() + ": `" + e.getValue() + "`").collect(Collectors.joining("\n")) + "\n");
//            }
//
//            channel.create().confirmation(embedTitle, body.toString(), command)
//                            .append(author.getAsMention())
//                                    .send();
//            return null;
//        }
//
//        Map<DBNation, String> mailErrors = new LinkedHashMap<>();
//        Map<DBNation, String> dmErrors = new LinkedHashMap<>();
//        CompletableFuture<IMessageBuilder> msgFuture = channel.send("Sending messages...");
//        for (Map.Entry<DBNation, Map.Entry<String, String>> entry : mailTargets.entrySet()) {
//            DBNation attacker = entry.getKey();
//            subject = entry.getValue().getKey();
//            String body = entry.getValue().getValue();
//
//            try {
//                attacker.sendMail(key, subject, body, true);
//            } catch (Throwable e) {
//                mailErrors.put(attacker, (e.getMessage() + " ").split("\n")[0]);
//            }
//            if (dm) {
//                String markup = MarkupUtil.htmlToMarkdown(body);
//                try {
//                    attacker.sendDM("**" + subject + "**:\n" + markup, new Consumer<String>() {
//                        @Override
//                        public void accept(String string) {
//                            dmErrors.put(attacker, string);
//                        }
//                    });
//                } catch (Throwable e) {
//                    dmErrors.put(attacker, (e.getMessage() + " ").split("\n")[0]);
//                }
//            }
//
//            if (System.currentTimeMillis() - start > 10000) {
//                start = System.currentTimeMillis();
//                if (msgFuture != null) {
//                    IMessageBuilder tmp = msgFuture.getNow(null);
//                    if (tmp != null) msgFuture = tmp.clear().append("Sending to " + attacker.getNation()).send();
//                }
//            }
//        }
//
//        StringBuilder errorMsg = new StringBuilder();
//        if (!mailErrors.isEmpty()) {
//            errorMsg.append("Mail errors: ");
//            errorMsg.append(
//                    mailErrors.keySet()
//                            .stream()
//                            .map(f -> f.getNation_id() + "")
//                            .collect(Collectors.joining(","))
//            );
//            for (Map.Entry<DBNation, String> entry : mailErrors.entrySet()) {
//                errorMsg.append("- " + entry.getKey().getNation_id() + ": " + entry.getValue() + "\n");
//            }
//        }
//
//        if (!dmErrors.isEmpty()) {
//            errorMsg.append("DM errors: ");
//            errorMsg.append(
//                    dmErrors.keySet()
//                            .stream()
//                            .map(f -> f.getNation_id() + "")
//                            .collect(Collectors.joining(","))
//            );
//            for (Map.Entry<DBNation, String> entry : dmErrors.entrySet()) {
//                errorMsg.append("- " + entry.getKey().getNation_id() + ": " + entry.getValue() + "\n");
//            }
//        }
//
//        IMessageBuilder msg = channel.create();
//        if (!errorMsg.isEmpty()) {
//            msg = msg.file("Errors.txt", errorMsg.toString());
//        }
//        msg.append("Done, sent " + sent + " messages" + (errorMsg.isEmpty() ? " (no errors)" : " (with errors)")).send();
//        return null;
//    }

    // TODO FIXME :||remove validate blitz sheet
//    @RolePermission(Roles.MILCOM)
//    @Command(desc="Run checks on nations in a blitz sheet\n" +
//            "- In range of their blitz targets" +
//            "- Still in the alliance" +
//            "- Have no more than the provided number of offensive wars\n" +
//            "Sheet columns: `nation`, `att1`, `att1`, `att3`")
//    public String ValidateBlitzSheet(SpreadSheet sheet,
//                                     @Arg("Max wars per attacker")
//                                     @Default("3") int maxWars,
//                                     @Arg("Only allow attacking these nations")
//                                     @Default("*") Set<DBNation> nationsFilter,
//                                     @Default Set<DBNation> attackerFilter,
//                                     @Arg("Parse nation leader instead of nation name") @Switch("l") boolean useLeader,
//                                     @Arg("Which row of the sheet has the header\n" +
//                                             "Default: 1st row")
//                                     @Switch("h") Integer headerRow) {
//        Function<DBNation, Boolean> isValidTarget = nationsFilter::contains;
//
//        StringBuilder response = new StringBuilder();
//        Integer finalMaxWars = maxWars;
//        if (headerRow == null) headerRow = 0;
//        AtomicBoolean hasErrors = new AtomicBoolean(false);
//        BlitzGenerator.getTargets(sheet, useLeader, headerRow, f -> finalMaxWars, 0.75, DNS.WAR_RANGE_MAX_MODIFIER, true, true, false, isValidTarget, new BiConsumer<Map.Entry<DBNation, DBNation>, String>() {
//            @Override
//            public void accept(Map.Entry<DBNation, DBNation> entry, String msg) {
//                hasErrors.set(true);
//                DBNation defender = entry.getKey();
//                DBNation attacker = entry.getValue();
//                if (attackerFilter != null && attacker != null && !attackerFilter.contains(attacker)) return;
//                response.append(msg + "\n");
//            }
//        }, info -> response.append("```\n" + info.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining("\n")) + "\n```").append("\n"));
//
//        if (!hasErrors.get()) response.append("\n**All checks passed**");
//
//        return response.toString();
//    }
//
//    private String getStrengthInfo(DBNation nation) {
//        String msg = "Ground:" + (int) nation.getGroundStrength(true, false) + ", Air: " + nation.getAircraft() + ", cities:" + nation.getCities();
//
//        if (nation.active_m() > 10000) msg += " (inactive)";
//        else {
//            msg += " (" + ((int) (nation.avg_daily_login() * 100)) + "% active)";
//        }
//
//        return msg;
//    }


    // TODO FIXME :||remove blitz sheet
//    @Command(desc = "Generates a a blitz sheet\n" +
//            "A blitz sheet contains a list of defenders (left column) and auto assigns attackers (right columns)\n" +
//            "Note: If the blitz sheet generated has a lot of city updeclares or unslotted enemies it is recommended to go through and remove low priority defenders\n" +
//            "- Low priority could be enemies without a recent offensive war, inactive, low military, or poor activity\n" +
//            "- Example defNations: `~enemies,#position>1,#active_m<4880,#dayssincelastoffensive>200,#dayssince3consecutivelogins>120,#aircraftpct<0.8,#tankpct<=0.6`" +
//            "Note: To avoid updeclares enable `onlyEasyTargets`")
//    @RolePermission(Roles.MILCOM)
//    public String blitzSheet(@Me IMessageIO io, @Me User author, @Me GuildDB db,
//                             @Arg("Nations that should be used for the attackers\n" +
//                                     "It is recommended to use a google sheet of the attackers available")
//                             NationList attNations,
//
//                             @Arg("Nations that should be used for the defenders\n" +
//                                     "It is recommended to use a google sheet of the priority defenders (unless you are sure you can hit every nation)")
//                             NationList defNations,
//                             @Arg("How many offensive slots a nation can have (defaults to 3)")
//                             @Default("3") @Range(min=1,max=5) int maxOff,
//                             @Arg("Value between 0 and 1 to prioritize assigning a target to nations in the same alliance\n" +
//                                     "Default: 0")
//                             @Default("0") double sameAAPriority,
//                             @Arg("Value between 0 and 1 to prioritize assigning targets to nations with similar activity patterns\n" +
//                                     "Recommended not to use if you know who is attacking")
//                             @Default("0") double sameActivityPriority,
//                             @Arg("The turn in the day (between 0 and 11) when you expect the blitz to happen")
//                             @Default("-1") @Range(min=-1,max=11) int turn,
//                             @Arg("A value between 0 and 1 to filter out attackers below this level of daily activity (default: 0, which is 0%)\n" +
//                                     "Recommend using if you did not provide a sheet of attackers")
//                             @Default("0") double attActivity,
//                             @Arg("A value between 0 and 1 to filter out defenders below this level of activity (default: 0)\n" +
//                                     "Recommend using if you did not provide a sheet of defenders")
//                             @Default("0") double defActivity,
//                             @Arg("Factor in existing wars of attackers and defenders\n" +
//                                     "i.e. To determine slots available and nation strength")
//                             @Switch("w") @Default("true") boolean processActiveWars,
//                             @Arg("Only assign down declares")
//                             @Switch("e") boolean onlyEasyTargets,
//                             @Arg("Maximum ratio of defender cities to attacker\n" +
//                                     "e.g. A value of 1.5 means defenders can have 1.5x more cities than the attacker")
//                             @Switch("c") Double maxCityRatio,
//                             @Arg("Maximum ratio of defender ground strength to attacker\n" +
//                                     "e.g. A value of 1.5 means defenders can have 1.5x more ground strength than the attacker")
//                             @Switch("g") Double maxGroundRatio,
//                             @Arg("Maximum ratio of defender aircraft to attacker\n" +
//                                     "e.g. A value of 1.5 means defenders can have 1.5x more aircraft than the attacker")
//                             @Switch("a") Double maxAirRatio,
//                             @Switch("s") SpreadSheet sheet) throws GeneralSecurityException, IOException {
//        Set<Long> guilds = new HashSet<>();
//
//        BlitzGenerator blitz = new BlitzGenerator(turn, maxOff, sameAAPriority, sameActivityPriority, attActivity, defActivity, guilds, processActiveWars);
//        blitz.addNations(attNations.getNations(), true);
//        blitz.addNations(defNations.getNations(), false);
//        if (processActiveWars) blitz.removeSlotted();
//
//        Map<DBNation, List<DBNation>> targets;
//        if (maxCityRatio != null || maxGroundRatio != null || maxAirRatio != null) {
//            onlyEasyTargets = true;
//        }
//        if (onlyEasyTargets) {
//            if (maxCityRatio == null) maxCityRatio = 1.8;
//            if (maxGroundRatio ==  null) maxGroundRatio = 1d;
//            if (maxAirRatio == null) maxAirRatio = 1.22;
//            targets = blitz.assignEasyTargets(maxCityRatio, maxGroundRatio, maxAirRatio);
//        } else {
//            targets = blitz.assignTargets();
//        }
//
//        if (sheet == null) sheet = SpreadSheet.create(db, SheetKey.ACTIVITY_SHEET);
//
//        SheetUtil.writeTargets(sheet, targets, turn);
//
//        sheet.send(io, null, author.getAsMention()).send();
//        return null;
//    }

    // TODO FIXME :||remove war sheet !!important
//    @Command(desc = "Generate a sheet of active wars between two coalitions (allies, enemies)\n" +
//            "Add `-i` to list concluded wars",
//    groups = {
//            "Additional War Options"
//    })
//    @RolePermission(Roles.MILCOM)
//    public String warSheet(@Me IMessageIO io, @Me GuildDB db,
//                           Set<DBNation> allies,
//                           Set<DBNation> enemies,
//                           @Arg(value = "Cutoff date for wars (default 5 days ago)", group = 0)
//                           @Default("5d") @Timestamp long startTime,
//                           @Switch("e") @Timestamp Long endTime,
//                           @Arg(value = "If concluded wars within the timeframe should be included", group = 0)
//                           @Switch("i") boolean includeConcludedWars,
//                           @Switch("s") SpreadSheet sheet) throws GeneralSecurityException, IOException {
//        if (endTime == null) endTime = System.currentTimeMillis();
//        WarParser parser1 = WarParser.ofAANatobj(null, allies, null, enemies, startTime, endTime);
//
//        Set<DBWar> allWars = new HashSet<>();
//        allWars.addAll(parser1.getWars().values());
//
//        if (!includeConcludedWars) allWars.removeIf(f -> !f.isActive());
//        allWars.removeIf(f -> {
//            DBNation att = f.getNation(true);
//            DBNation def = f.getNation(false);
//            return (!allies.contains(att) && !enemies.contains(att)) || (!allies.contains(def) && !enemies.contains(def));
//        });
//
//        if (sheet == null) {
//            sheet = SpreadSheet.create(db, SheetKey.WAR_SHEET);
//        }
//
//        List<Object> headers = new ArrayList<>(Arrays.asList(
//                "id",
//                "type",
//                "counter",
//                "GS",
//                "AS",
//                "B",
//                "ships",
//                "planes",
//                "tanks",
//                "soldiers",
//                "cities",
//                "MAP",
//                "Resistance",
//                "Attacker",
//                "Att AA",
//                "Turns",
//                "Def AA",
//                "Defender",
//                "Resistance",
//                "MAP",
//                "Cities",
//                "Soldiers",
//                "Tanks",
//                "Planes",
//                "Ships",
//                "GS",
//                "AS",
//                "B"
//        ));
//
//        sheet.setHeader(headers);
//
//        for (DBWar war : allWars) {
//            DBNation att = war.getNation(true);
//            DBNation def = war.getNation(false);
//
//            if (att == null || def == null) continue;
//
//            WarType type = war.getWarType();
//            WarCard card = new WarCard(war, true, false);
//
//
//            headers.set(0, MarkupUtil.sheetUrl(war.getWarId() + "", war.toUrl()));
//            headers.set(1, war.getWarType().name());
//            CounterStat counterStat = card.getCounterStat();
//            headers.set(2, counterStat == null ? "" : counterStat.type.name());
//            headers.set(3, card.groundControl == war.getAttacker_id() ? "Y" : "N");
//            headers.set(4, card.airSuperiority == war.getAttacker_id() ? "Y" : "N");
//            headers.set(5, card.blockaded == war.getAttacker_id() ? "Y" : "N");
//            headers.set(6, att.getShips());
//            headers.set(7, att.getAircraft());
//            headers.set(8, att.getTanks());
//            headers.set(9, att.getSoldiers());
//            headers.set(10, att.getCities());
//            headers.set(11, card.attackerMAP);
//            headers.set(12, card.attackerResistance);
//            headers.set(13, MarkupUtil.sheetUrl(att.getNation(), att.getUrl()));
//            headers.set(14, MarkupUtil.sheetUrl(att.getAllianceName(), att.getAllianceUrl()));
//
//            long turnStart = TimeUtil.getTurn(war.getDate());
//            long turns = 60 - (TimeUtil.getTurn() - turnStart);
//            headers.set(15, turns);
//
//            headers.set(16, MarkupUtil.sheetUrl(def.getAllianceName(), def.getAllianceUrl()));
//            headers.set(17, MarkupUtil.sheetUrl(def.getNation(), def.getUrl()));
//            headers.set(18, card.defenderResistance);
//            headers.set(19, card.defenderMAP);
//            headers.set(20, def.getCities());
//            headers.set(21, def.getSoldiers());
//            headers.set(22, def.getTanks());
//            headers.set(23, def.getAircraft());
//            headers.set(24, def.getShips());
//            headers.set(25, card.groundControl == war.getDefender_id() ? "Y" : "N");
//            headers.set(26, card.airSuperiority == war.getDefender_id() ? "Y" : "N");
//            headers.set(27, card.blockaded == war.getDefender_id() ? "Y" : "N");
//
//            sheet.addRow(headers);
//        }
//
//        sheet.updateClearCurrentTab();
//        sheet.updateWrite();
//
//        sheet.attach(io.create(), "wars").send();
//        return null;
//    }

//    @RolePermission(value = Roles.MILCOM)
//    @Command(desc = "List war rooms")
//    public String listWarRooms(@Me GuildDB db, NationFilter filter) {
//        WarCategory warCat = db.getWarChannel(true);
//        for (Map.Entry<Integer, WarCategory.WarRoom> entry : warCat.getWarRoomMap().entrySet()) {
//            WarCategory.WarRoom room = entry.getValue();
//            DBNation target = room.target;
//            for (DBWar war : target.getActiveWars()) {
//                DBNation other = war.getNation(!war.isAttacker(target));
//            }
//        }
//
//    }

    // TODO FIXME :||remove counter sheet !!important
//    @RolePermission(value = Roles.MILCOM)
//    @Command(desc = "Generate a sheet with a list of nations attacking `allies`\n" +
//            "(Defaults to those attacking allies)\n" +
//            "Please still check the war history in case it is not valid to counter (and add a note to the note column indicating such)\n" +
//            "Add `-a` to filter out applicants\n" +
//            "Add `-i` to filter out inactive members\n" +
//            "Add `-e` to include enemies not attacking")
//    public String counterSheet(@Me IMessageIO io, @Me GuildDB db,
//                               @Arg("Only include these attackers")
//                               @Default() Set<DBNation> enemyFilter,
//                               @Arg("Show attackers against these alliances")
//                               @Default() Set<DBAlliance> allies,
//                               @Arg("Exclude applicants")
//                               @Switch("a") boolean excludeApplicants,
//                               @Arg("Exclude inactive nations (3.4 days)")
//                               @Switch("i") boolean excludeInactives,
//                               @Arg("Include enemies not attacking")
//                               @Switch("e") boolean includeAllEnemies,
//                               @Switch("s") String sheetUrl) throws IOException, GeneralSecurityException {
//        boolean includeProtectorates = true;
//        boolean includeCoalition = true;
//        boolean includeMDP = true;
//        boolean includeODP = true;
//
//        Set<Integer> alliesIds = db.getAllies();
//        Set<Integer> protectorates = new HashSet<>();
//
//        Set<Integer> aaIds = db.getAllianceIds();
//        if (!aaIds.isEmpty()) {
//            for (int aaId : aaIds) {
//                protectorates = Locutus.imp().getNationDB().getTreaties(aaId, TreatyType.PROTECTORATE).keySet();
//                if (includeProtectorates) {
//                    alliesIds.addAll(protectorates);
//                }
//                if (includeMDP) {
//                    alliesIds.addAll(Locutus.imp().getNationDB().getTreaties(aaId, TreatyType.MDP, TreatyType.MDOAP, TreatyType.EXTENSION).keySet());
//                }
//                if (includeODP) {
//                    alliesIds.addAll(Locutus.imp().getNationDB().getTreaties(aaId, TreatyType.ODP, TreatyType.ODOAP).keySet());
//                }
//            }
//        }
//
//        if (allies != null) {
//            for (DBAlliance ally : allies) alliesIds.add(ally.getAlliance_id());
//        }
//
//        Map<DBNation, List<DBWar>> enemies = new HashMap<>();
//        Set<Integer> enemyAAs = db.getCoalition("enemies");
//        Set<DBWar> defWars = Locutus.imp().getWarDb().getActiveWarsByAlliance(null, alliesIds);
//        for (DBWar war : defWars) {
//            if (!war.isActive()) continue;
//            DBNation enemy = Locutus.imp().getNationDB().getNation(war.getAttacker_id());
//            if (enemy == null) continue;
//
//            if (!enemyAAs.contains(enemy.getAlliance_id())) {
//                CounterStat stat = war.getCounterStat();
//                if (stat.type == CounterType.IS_COUNTER || stat.type == CounterType.ESCALATION) continue;
//            }
//
//            DBNation defender = Locutus.imp().getNationDB().getNation(war.getDefender_id());
//            if (defender == null) continue;
//            if (excludeApplicants && defender.getPosition() <= 1) continue;
//            if (excludeInactives && defender.active_m() > 4880) continue;
//            if (!alliesIds.contains(defender.getAlliance_id())) continue;
//
//            enemies.computeIfAbsent(enemy, f -> new ArrayList<>()).add(war);
//        }
//
//        if (includeAllEnemies) {
//            for (DBNation enemy : Locutus.imp().getNationDB().getNations(enemyAAs)) {
//                enemies.putIfAbsent(enemy, new ArrayList<>());
//            }
//        }
//
//        if (enemyFilter != null) {
//            enemies.entrySet().removeIf(f -> !enemyFilter.contains(f.getKey()));
//        }
//
//        SpreadSheet sheet;
//        if (sheetUrl != null) {
//            sheet = SpreadSheet.create(sheetUrl);
//        } else {
//            sheet = SpreadSheet.create(db, SheetKey.COUNTER_SHEET);
//        }
//
//        WarCategory warCat = db.getWarChannel();
//
//        List<Object> header = new ArrayList<>(Arrays.asList(
//                "note",
//                "warroom",
//                "nation",
//                "alliance",
//                "status",
//                "def_position",
//                "att_dd:hh:mm",
//                "def_dd:hh:mm",
//                "\uD83D\uDEE1",
//                "\uD83C\uDFD9", // cities
//                "\uD83C\uDFD7", // avg_infra
//                "score",
//                "\uD83D\uDC82", // soldiers
//                "\u2699", // tanks
//                "\u2708", // air
//                "\u26F5", // navy
//                "def1",
//                "def2",
//                "def3",
//                "def4",
//                "def5"
//        ));
//
//        Map<Integer, String> notes = new HashMap<>();
//        List<List<Object>> rows = sheet.fetchAll(null);
//
//        if (rows != null && !rows.isEmpty()) {
//            for (int i = 1; i < rows.size(); i++) {
//                List<Object> row = rows.get(i);
//                if (row.size() < 3) {
//                    continue;
//                }
//
//                Object note = row.get(0);
//                if (note == null || note.toString().isEmpty()) {
//                    continue;
//                }
//                Object cell = row.get(1);
//                if (cell == null) {
//                    continue;
//                }
//                String nationName = cell + "";
//                if (nationName.isEmpty()) continue;
//
//                DBNation nation = DiscordUtil.parseNation(nationName);
//                if (nation != null) {
//                    notes.put(nation.getNation_id(), note.toString());
//                }
//            }
//        }
//
//        sheet.setHeader(header);
//
//        // sort
//        //
//
//        for (Map.Entry<DBNation, List<DBWar>> entry : enemies.entrySet()) {
//            DBNation enemy = entry.getKey();
//            if (enemy.isBeige() || enemy.getDef() >= 3) continue;
//
//            List<DBWar> wars = entry.getValue();
//
//            int action = 3;
//            String[] actions = {"ATTACKING US", "ATTACKING PROTECTORATE", "ATTACKING ALLY", ""};
//
//            int active_m = Integer.MAX_VALUE;
//            Rank rank = null;
//
//            for (DBWar war : wars) {
//                DBNation defender = Locutus.imp().getNationDB().getNation(war.getDefender_id());
//                if (defender == null) {
//                    continue;
//                }
//                if (rank == null || defender.getPosition() > rank.id) {
//                    int position = defender.getPosition();
//                    rank = Rank.byId(position);
//                }
//
//                active_m = Math.min(active_m, defender.active_m());
//
//                if (aaIds.contains(Integer.valueOf(war.getDefender_aa()))) {
//                    action = Math.min(action, 0);
//                } else if (protectorates.contains(war.getDefender_aa())) {
//                    action = Math.min(action, 1);
//                } else if (alliesIds.contains(war.getDefender_aa())) {
//                    action = Math.min(action, 2);
//                } else {
//                    continue;
//                }
//            }
//
//            String actionStr = actions[action];
//            if (enemyAAs.contains(enemy.getAlliance_id())) {
//                actionStr = ("ENEMY " + actionStr).trim();
//            } else if (wars.isEmpty()) {
//                continue;
//            }
//
//            if (active_m == Integer.MAX_VALUE) active_m = 0;
//
//            ArrayList<Object> row = new ArrayList<>();
//            row.add(notes.getOrDefault(enemy.getNation_id(), ""));
//
//            WarCategory.WarRoom warroom = warCat != null ? warCat.get(enemy, true, false, false) : null;
////            warCat.sync();
//            GuildMessageChannel channel = warroom != null ? warroom.getChannel(false) : null;
//            if (channel != null) {
//                String url = DiscordUtil.getChannelUrl(channel);
//                String name = "#" + enemy.getName();
//                row.add(MarkupUtil.sheetUrl(name, url));
//            } else {
//                row.add("");
//            }
//
//            row.add(MarkupUtil.sheetUrl(enemy.getNation(), DNS.getUrl(enemy.getNation_id(), false)));
//            row.add(MarkupUtil.sheetUrl(enemy.getAllianceName(), DNS.getUrl(enemy.getAlliance_id(), true)));
//            row.add(actionStr);
//            row.add( rank == null ? "" : rank.name());
//
//
//            row.add( DurationFormatUtils.formatDuration(enemy.active_m() * 60L * 1000, "dd:HH:mm"));
//            row.add(DurationFormatUtils.formatDuration(active_m * 60L * 1000, "dd:HH:mm"));
//            row.add(enemy.getDef());
//
//            row.add(enemy.getCities());
//            row.add(enemy.getAvg_infra());
//            row.add(enemy.getScore());
//
//            row.add(enemy.getSoldiers());
//            row.add(enemy.getTanks());
//            row.add(enemy.getAircraft());
//            row.add(enemy.getShips());
//
//            for (int i = 0; i < wars.size(); i++) {
//                DBWar war = wars.get(i);
//                String url = war.toUrl();
//                DBNation defender = Locutus.imp().getNationDB().getNation(war.getDefender_id());
//                String warStr = defender.getNation() + "|" + defender.getAllianceName();
//                row.add(MarkupUtil.sheetUrl(warStr, url));
//            }
//
//            sheet.addRow(row);
//        }
//
//        sheet.updateClearCurrentTab();
//
//        sheet.updateWrite();
//
//        sheet.attach(io.create(), "counter").send();
//        return null;
//    }

    @Command(desc = "Show a war card embed on discord for a war by id")
    public String warcard(@Me IMessageIO channel, int warId) throws IOException {
        new WarCard(warId).embed(channel, true, false);
        return null;
    }

    @Command(desc="Show war info for a nation", aliases = {"wars", "warinfo"})
    public String wars(@Me IMessageIO channel, DBNation nation) {
        Set<DBWar> wars = nation.getActiveWars();
        String title = wars.size() + " wars";
        String body = nation.getWarInfoEmbed();
        channel.create().embed(title, body).send();
        return null;
    }

    // TODO FIXME :||remove counter !!important
//    @RolePermission(value = {Roles.MEMBER, Roles.MILCOM}, any = true)
//    @Command(desc="Get a list of nations to counter a war\n" +
//            "Add `-o` to ignore nations with 5 offensive slots\n" +
//            "Add `-w` to filter out weak attackers\n" +
//            "Add `-a` to only list active nations (past hour)")
//    public String counterWar(@Me DBNation me, @Me GuildDB db,
//                             DBWar war,
//                             @Arg("Nations to counter with\n" +
//                                     "Default: This guild's alliance nations")
//                             @Default Set<DBNation> counterWith,
//                             @Arg("Show counters from nations at max offensive wars\n" +
//                                     "i.e. They can counter when they finish a war")
//                             @Switch("o") boolean allowAttackersWithMaxOffensives,
//                             @Arg("Remove countering nations weaker than the enemy")
//                             @Switch("w") boolean filterWeak,
//                             @Arg("Remove countering nations that are inactive (2 days)")
//                             @Switch("a") boolean onlyActive,
//                             @Arg("Remove countering nations NOT registered with Locutus")
//                             @Switch("d") boolean requireDiscord,
//                             @Arg("Include counters from the same alliance as the defender")
//                             @Switch("s") boolean allowSameAlliance,
//                             @Switch("i") boolean includeInactive,
//                             @Switch("m") boolean includeNonMembers,
//                             @Arg("Include the discord mention of countering nations")
//                             @Switch("p") boolean ping) {
//        Set<Integer> allies = db.getAllies(true);
//        int enemyId = allies.contains(war.getAttacker_aa()) ? war.getDefender_id() : war.getAttacker_id();
//        DBNation enemy = DBNation.getById(enemyId);
//        if (enemy == null) throw new IllegalArgumentException("No nation found for id `" + enemyId + "`");
//        return counter(me, db, enemy, counterWith, allowAttackersWithMaxOffensives, filterWeak, onlyActive, requireDiscord, allowSameAlliance, includeInactive, includeNonMembers, ping);
//    }
//
//    @RolePermission(value = Roles.MILCOM)
//    @Command(desc="Get a list of nations to counter an enemy",
//            groups = {
//                "Counter Options",
//                "Display Info",
//            },
//            groupDescs = {
//                "By default active alliance members with free war slots will be used",
//                ""
//            }
//    )
//    public static String counter(@Me DBNation me, @Me GuildDB db,
//                                 DBNation target,
//                          @Arg(value = "Nations to counter with\n" +
//                                  "Default: This guild's alliance nations", group = 0)
//                          @Default Set<DBNation> counterWith,
//                          @Arg("Show counters from nations at max offensive wars\n" +
//                                  "i.e. They can counter when they finish a war")
//                          @Switch("o") boolean allowMaxOffensives,
//                          @Arg(value = "Remove countering nations weaker than the enemy", group = 0)
//                          @Switch("w") boolean filterWeak,
//                          @Arg(value = "Remove countering nations that are inactive (2 days)", group = 0)
//                          @Switch("a") boolean onlyOnline,
//                          @Arg(value = "Remove countering nations NOT registered with Locutus", group = 0)
//                          @Switch("d") boolean requireDiscord,
//                          @Arg(value = "Don't filter out counters from the same alliance as the defender", group = 0)
//                          @Switch("s") boolean allowSameAlliance,
//                          @Switch("i") boolean includeInactive,
//                          @Switch("m") boolean includeNonMembers,
//                          @Arg(value = "Include the discord mention of countering nations", group = 1)
//                          @Switch("p") boolean ping
//
//
//    ) {
//        if (counterWith == null) {
//            Set<Integer> aaIds = db.getAllianceIds();
//            if (aaIds.isEmpty()) {
//                Set<Integer> allies = db.getAllies(true);
//                if (allies.isEmpty()) {
//                    if (me.getAlliance_id() == 0) {
//                        return "No alliance or allies are set.\n" +
//                                GuildKey.ALLIANCE_ID.getCommandMention() +
//                                "\nOR\n " +
//                                CM.coalition.create.cmd.coalitionName(Coalition.ALLIES.name());
//                    }
//                    aaIds = new HashSet<>(Arrays.asList(me.getAlliance_id()));
//                    counterWith = new HashSet<>(new AllianceList(aaIds).getNations(true, 0, true));
//                } else {
//                    counterWith = new HashSet<>(Locutus.imp().getNationDB().getNations(allies));
//                }
//            } else {
//                counterWith = new HashSet<>(new AllianceList(aaIds).getNations(true, 0, true));
//            }
//        }
//        counterWith.removeIf(nation -> nation.getAlliance_id() == 0);
//        counterWith.removeIf(f -> f.isVacation());
//        double score = target.getScore();
//        double scoreMin = score / DNS.WAR_RANGE_MAX_MODIFIER;
//        double scoreMax = score / 0.75;
//        counterWith.removeIf(nation -> nation.getScore() < scoreMin || nation.getScore() > scoreMax);
//        if (counterWith.isEmpty()) {
//            return "No nations available to counter";
//        }
//
//        int size = counterWith.size();
//        List<String> errors = new ArrayList<>();
//        if (requireDiscord) {
//            counterWith.removeIf(f -> f.getUser() == null);
//            if (size > counterWith.size()) {
//                errors.add("Removed `" + (size - counterWith.size()) + "` nations without discord");
//                size = counterWith.size();
//            }
//        }
//
//        for (DBWar activeWar : target.getActiveWars()) {
//            counterWith.remove(activeWar.getNation(!activeWar.isAttacker(target)));
//        }
//        if (size > counterWith.size()) {
//            errors.add("Removed `" + (size - counterWith.size()) + "` nations in active wars");
//            size = counterWith.size();
//        }
//
//        if (onlyOnline) {
//            counterWith.removeIf(f -> !f.isOnline());
//            if (size > counterWith.size()) {
//                errors.add("Removed `" + (size - counterWith.size()) + "` nations not currently online on discord (or in-game)");
//                size = counterWith.size();
//            }
//        }
//        if (!allowMaxOffensives) {
//            counterWith.removeIf(nation -> nation.getOff() >= nation.getMaxOff());
//            if (size > counterWith.size()) {
//                errors.add("Removed `" + (size - counterWith.size()) + "` nations with max offensives as `allowMaxOffensives` is false");
//                size = counterWith.size();
//            }
//        }
//        if (filterWeak) {
//            counterWith.removeIf(nation -> nation.getWarIndex() < target.getWarIndex());
//            if (size > counterWith.size()) {
//                errors.add("Removed `" + (size - counterWith.size()) + "` nations weaker than the target");
//                size = counterWith.size();
//            }
//        }
//        if (!includeInactive) {
//            counterWith.removeIf(nation -> nation.active_m() > (nation.getCities() < 10 ? 4880 : 10080));
//            if (size > counterWith.size()) {
//                errors.add("Removed `" + (size - counterWith.size()) + "` inactive nations as `includeInactive` is false");
//                size = counterWith.size();
//            }
//        }
//        if (!includeNonMembers) {
//            counterWith.removeIf(nation -> nation.getPosition() <= Rank.APPLICANT.id);
//            if (size > counterWith.size()) {
//                errors.add("Removed `" + (size - counterWith.size()) + "` non-members as `includeNonMembers` is false");
//                size = counterWith.size();
//            }
//        }
//        Set<Integer> counterWithAlliances = counterWith.stream().map(DBNation::getAlliance_id).collect(Collectors.toSet());
//        if (counterWithAlliances.size() == 1 && !allowSameAlliance && counterWithAlliances.contains(target.getAlliance_id())) {
//            return "Please enable `allowSameAlliance` to counter with the same alliance";
//        }
//        if (!allowSameAlliance) {
//            counterWith.removeIf(nation -> nation.getAlliance_id() == target.getAlliance_id());
//            if (size > counterWith.size()) {
//                errors.add("Removed `" + (size - counterWith.size()) + "` nations in the same alliance as `allowSameAlliance` is false");
//                size = counterWith.size();
//            }
//        }
//
//        List<DBNation> attackersSorted = new ArrayList<>(counterWith);
//        if (filterWeak) {
//            attackersSorted = CounterGenerator.generateCounters(db, target, attackersSorted, allowMaxOffensives);
//        } else {
//            attackersSorted = CounterGenerator.generateCounters(db, target, attackersSorted, allowMaxOffensives, false);
//        }
//
//        if (attackersSorted.isEmpty()) {
//            String errorStr = errors.isEmpty() ? "" : "\n- " + StringMan.join(errors, "\n- ");
//            return "No nations available to counter" + errorStr;
//        }
//
//        StringBuilder response = new StringBuilder();
//        response.append("**Enemy: **").append(target.toMarkdown()).append("\n**Counters**\n");
//
//        int count = 0;
//        int maxResults = 25;
//        for (DBNation nation : attackersSorted) {
//            if (count++ == maxResults) break;
//
//            String statusStr = "";
//
//            User user = nation.getUser();
//            if (user != null) {
//                List<Guild> mutual = user.getMutualGuilds();
//                if (!mutual.isEmpty()) {
//                    Guild guild = mutual.get(0);
//                    Member member = guild.getMember(user);
//                    if (member != null) {
//                        OnlineStatus status = member.getOnlineStatus();
//                        if (status != OnlineStatus.OFFLINE && status != OnlineStatus.UNKNOWN) {
//                            statusStr = status.name() + " | ";
//                        }
//                    }
//                }
//            }
//            if (user != null) {
//                response.append(statusStr);
//                response.append(user.getName() + " / ");
//                if (ping) response.append(user.getAsMention());
//                else response.append("`" + user.getAsMention() + "` ");
//            }
//            response.append(nation.toMarkdown()).append('\n');
//        }
//        if (!errors.isEmpty()) {
//            response.append("\n\n__**note:**__\n- ");
//            response.append(StringMan.join(errors, "\n- "));
//        }
//
//        return response.toString();
//    }

    // TODO FIXME :||remove auto counter
//    @RolePermission(value = Roles.MILCOM)
//    @Command(desc = "Auto generate counters\n" +
//            "Add `-p` to ping users that are added\n" +
//            "Add `-a` to skip adding users\n" +
//            "Add `-m` to send standard counter messages")
//    public String autocounter(@Me IMessageIO channel, @Me JSONObject command, @Me WarCategory warCat, @Me DBNation me, @Me User author, @Me GuildDB db,
//                              DBNation enemy,
//                              @Arg("Nations to counter with\n" +
//                                      "Default: This guild's alliance nations")
//                              @Default Set<DBNation> attackers,
//                              @Arg("Max number of nations to counter with")
//                              @Default("3") @Range(min=0) int max,
//                              @Arg("Ping the countering nations on discord")
//                              @Switch("p") boolean pingMembers,
//                              @Arg("Do not add countering nations to a war room for the enemy")
//                              @Switch("a") boolean skipAddMembers,
//                              @Arg("Send counter message ingame to the nations countering")
//                              @Switch("m") boolean sendMail) {
//        if (attackers == null) {
//            AllianceList alliance = db.getAllianceList();
//            if (alliance != null && !alliance.isEmpty()) {
//                attackers = new HashSet<>(alliance.getNations(true, 2440, true));
//            } else {
//                throw new IllegalArgumentException("This guild is not in an alliance, please provide the nations to counter with");
//            }
//        }
//        attackers.removeIf(f -> enemy.getCities() >= f.getCities() * 2);
//        attackers.removeIf(f -> f.getUser() == null || db.getGuild().getMember(f.getUser()) == null);
//        return warroom(channel, command, warCat, me, author, db, enemy, attackers, max, false, true, false, true, pingMembers, skipAddMembers, sendMail);
//    }

//    // TODO FIXME :||remove
//    @RolePermission(value = Roles.MILCOM)
//    @Command(desc = "Sorts the war rooms into the correct discord category\n" +
//            "e.g. `warcat-c1-10`")
//    public String sortWarRooms(@Me WarCategory warCat) {
//        int moved = warCat.sort();
//        return "Done! Moved " + moved + " channels";
//    }

    @RolePermission(value = Roles.MILCOM)
    @Command(desc = "Delete planning war rooms with no participants")
    public String deletePlanningChannel(@Me WarCategory warCat) {
        int count = 0;
        for (Map.Entry<Integer, WarCategory.WarRoom> entry : new HashMap<>(warCat.getWarRoomMap()).entrySet()) {
            WarCategory.WarRoom room = entry.getValue();
            if (room.channel == null) continue;
            if (!room.getParticipants().isEmpty()) continue;
            if (!room.isPlanning()) continue;
            room.addInitialParticipants(false);
            if (!room.getParticipants().isEmpty()) continue;
            room.delete("Manually deleted");
            count++;
        }
        if (count == 0) return "No channels found to delete";
        return "Done. Deleting " + count + " war rooms. Please wait for rooms to finish deleting";
    }

    @RolePermission(value = Roles.MILCOM)
    @Command(desc = "Delete war rooms against the enemies specified")
    public String deleteForEnemies(@Me WarCategory warCat, Set<DBNation> enemy_rooms) {
        int count = 0;
        for (Map.Entry<Integer, WarCategory.WarRoom> entry : new HashMap<>(warCat.getWarRoomMap()).entrySet()) {
            WarCategory.WarRoom room = entry.getValue();
            if (!enemy_rooms.contains(room.target)) continue;
            room.delete("Manually deleted");
            count++;
        }
        if (count == 0) return "No channels found to delete";
        return "Done. Deleting " + count + " war rooms. Please wait for rooms to finish deleting.\n\n" +
                "Note: Rooms will auto create with enemies with active wars, set a filter to specify which enemies rooms are auto created for:\n" +
                "- " + CM.settings_war_alerts.WAR_ROOM_FILTER.cmd.toSlashMention() + "\n" +
                "- " + CM.admin.sync.warrooms.cmd.toSlashMention();
    }

    @RolePermission(value = Roles.MILCOM)
    @Command(desc = "Create a war room\n" +
            "Add `-p` to ping users that are added\n" +
            "Add `-a` to skip adding users\n" +
            "Add `-f` to force create channels (if checks fail)\n" +
            "Add `-m` to send standard counter messages")
    public String warroom(@Me IMessageIO channel, @Me JSONObject command, @Me WarCategory warCat, @Me DBNation me, @Me User author, @Me GuildDB db,
                          DBNation enemy,
                          @Arg("Nations to counter with\n" +
                          "Default: This guild's alliance nations")
                          Set<DBNation> attackers,
                          @Arg("Max number of nations to counter with")
                          @Default("3") @Range(min=0) int max,
                          @Switch("f") boolean force,
                          @Arg("Remove countering nations weaker than the enemy")
                          @Switch("w") boolean excludeWeakAttackers,
                          @Arg("Remove countering nations NOT registered with Locutus")
                          @Switch("d") boolean requireDiscord,
                          @Arg("Show counters from nations at max offensive wars\n" +
                                  "i.e. They can counter when they finish a war")
                          @Switch("o") boolean allowAttackersWithMaxOffensives,
                          @Arg("Ping the countering nations on discord")
                          @Switch("p") boolean pingMembers,
                          @Arg("Do not add countering nations to a war room for the enemy")
                          @Switch("a") boolean skipAddMembers,
                          @Arg("Send counter message ingame to the nations countering")
                          @Switch("m") boolean sendMail) {
        List<DBNation> attackersSorted = new ArrayList<>(attackers);

        if (excludeWeakAttackers) {
            if (requireDiscord) attackersSorted.removeIf(f -> f.getUser() == null);
            attackersSorted = CounterGenerator.generateCounters(db, enemy, attackersSorted, allowAttackersWithMaxOffensives);
            if (attackersSorted.isEmpty()) {
                return "No nations available to counter";
            }
        }

        Set<Integer> tracked = db.getAllies();
        if (!force) {
            for (DBNation attacker : attackersSorted) {
                if (!tracked.contains(attacker.getAlliance_id())) {
                    channel.create().confirmation("Error: Unsuitable counter", attacker.getNationUrlMarkup(true) + " | " + attacker.getAllianceUrlMarkup(true) + " is not an ally.", command).send();
                    return null;
                }
                if (enemy.getScore() < attacker.getScore() * 0.75 || enemy.getScore() > attacker.getScore() * DNS.WAR_RANGE_MAX_MODIFIER_ACTIVE) {
//                    DiscordUtil.pending(channel, message, "Error: Unsuitable counter", attacker.getNationUrlMarkup(true) + " | " + attacker.getAllianceUrlMarkup(true) + " is outside war range (see " + CM.nation.score.cmd.toSlashMention() + "). ", 'f');
                    channel.create().confirmation("Error: Unsuitable counter", attacker.getNationUrlMarkup(true) + " | " +
                            attacker.getAllianceUrlMarkup(true) + " is outside war range (see " + CM.nation.score.cmd.toSlashMention() + "). ", command).send();
                    return null;
                }
                if (attacker.getOff() >= attacker.getMaxOff() && !allowAttackersWithMaxOffensives) {
                    channel.create().confirmation("Error: Unsuitable counter", attacker.getNationUrlMarkup(true) + " | " + attacker.getAllianceUrlMarkup(true) +  " already has max offensives. ", command).send();
                    return null;
                }
                if (attacker.isVacation()) {
                    channel.create().confirmation( "Error: Unsuitable counter", attacker.getNationUrlMarkup(true) + " | " + attacker.getAllianceUrlMarkup(true) + " is in VM. ", command).send();
                    return null;
                }
                if (attacker.active_m() > 7200) {
                    channel.create().confirmation( "Error: Unsuitable counter", attacker.getNationUrlMarkup(true) + " | " + attacker.getAllianceUrlMarkup(true) + " is inactive. ", command).send();
                    return null;
                }
                if (attacker.getNumWars() > 0 && attacker.getRelativeStrength() < 1) {
                    channel.create().confirmation( "Error: Unsuitable counter", attacker.getNationUrlMarkup(true) + " | " + attacker.getAllianceUrlMarkup(true) + " is already involved in heavy conflict.", command).send();
                    return null;
                }
            }
        }

        StringBuilder response = new StringBuilder();
        if (attackersSorted.size() > max) {
            int removed = attackersSorted.size() - max;
            response.append("Skipped adding " + removed + " nations as `max` is set to " + max + ". Provide a higher value to override this\n");
            attackersSorted = attackersSorted.subList(0, max);
        }

        WarCategory.WarRoom warChan = warCat.createChannel(author, new Consumer<String>() {
            @Override
            public void accept(String s) {
                response.append(s + "\n");
            }
        }, pingMembers, !skipAddMembers, sendMail, enemy, attackersSorted);

        response.append(warChan.getChannel().getAsMention());

        me.setMeta(NationMeta.INTERVIEW_WAR_ROOM, (byte) 1);

        if (!sendMail && db.getOrNull(GuildKey.API_KEY) != null) response.append("\n- add `-m` to send standard counter instructions");
        if (!pingMembers && db.getOrNull(GuildKey.API_KEY) != null) response.append("\n- add `-p` to ping users in the war channel");

        if (!skipAddMembers) {
            for (DBNation dbNation : attackersSorted) {
                response.append("\nAdded " + dbNation.toMarkdown(false, false, true, false, true, false));
            }
        }

        return response.toString();
    }

    @RolePermission(value = Roles.MEMBER)
    @Command(desc = "Update the pin in the current war room channel")
    public String warpin(@Me WarCategory.WarRoom warRoom) {
        IMessageBuilder message = warRoom.updatePin(true);
        return "Updated: " + DiscordUtil.getChannelUrl(warRoom.channel) + "/" + message.getId();
    }

    @RolePermission(value = Roles.MILCOM)
    @Command(desc = "Run this command in a war room to assign it to a category\n" +
            "`{prefix}war room setcategory category:raid`")
    public String warcat(@Me WarCategory warCat, @Me WarCategory.WarRoom waRoom, @Me TextChannel channel,
                         @Arg("The category to move this channel to")
                         @Filter("warcat.*") Category category) {
        if (category.equals(channel.getParentCategory())) {
            return "Already in category: " + category.getName();
        }

        RateLimitUtil.complete(channel.getManager().setParent(category));

        return "Set category for " + channel.getAsMention() + " to " + category.getName();
    }

    private Map<Long, List<String>> blitzTargetCache = new HashMap<>();

    @RolePermission(value = Roles.MILCOM)
    @Command(desc = "Generate a list of possible alliance blitz targets (for practice)\n" +
            "Target alliances are sorted by free war slots", aliases = {"blitzpractice","blitztargets"})
    public String BlitzPractice(@Me GuildDB db,
                                @Arg("Exclude the top X alliances (by active nation score)")
                                int topX, @Me IMessageIO channel, @Me JSONObject command, @Switch("p") Integer page) {
        Set<Integer> dnr = db.getCoalition("allies");

        List<String> results = blitzTargetCache.getOrDefault(db.getGuild().getIdLong() + topX, new ArrayList<>());
        if (results.isEmpty()) {

            List<DBAlliance> alliances = new ArrayList<>(Locutus.imp().getNationDB().getAlliances(true, true, true, 1000));
            Set<DBAlliance> top30 = new LinkedHashSet<>(Locutus.imp().getNationDB().getAlliances(true, true, true, topX));

            outer:
            for (DBAlliance alliance : alliances) {
                if (top30.contains(alliance)) continue;
                Set<DBNation> nations = alliance.getNations(true, 5000, true);
                if (nations.size() <= 2) continue;
                for (Map.Entry<Integer, Treaty> entry : alliance.getDefenseTreaties().entrySet()) {
                    if (dnr.contains(entry.getKey())) continue outer;
                    if (top30.contains(DBAlliance.getOrCreate(entry.getKey()))) continue outer;
                }
                int slots = 0;
                for (DBNation nation : nations) {
                    if (nation.hasProtection()) continue;
                    slots += nation.getMaxDef() - nation.getDef();
                }
                if (slots <= 2) continue;

                int myRank = alliances.indexOf(alliance);
                int largestAlly = myRank;
                boolean hasProtection = false;
                for (Map.Entry<Integer, Treaty> entry : alliance.getDefenseTreaties().entrySet()) {
                    DBAlliance other = DBAlliance.getOrCreate(entry.getKey());
                    int min = alliances.indexOf(other);
                    if (min != -1 && min < largestAlly) {
                        largestAlly = min;
                        hasProtection = true;
                    }
                }
                String protectionStr = hasProtection ? " Allied: #" + largestAlly : "";

                List<DBAlliance> sphere = alliance.getSphereRanked();
                String sphereStr = sphere.isEmpty() ? "None" : sphere.get(0).getMarkdownUrl();

                results.add(alliance.getMarkdownUrl() + " #" + myRank + protectionStr + " | sphere?:" + sphereStr + " | members:" + nations.size() + " | slots:" + slots);
            }
            blitzTargetCache.put(db.getGuild().getIdLong() + topX, results);
        }

        int perPage = 10;
        String title = "Blitz targets";

        channel.create().paginate(title, command, page, perPage, results).send();

        return null;
    }
}

package link.locutus.discord.commands.manager.v2.impl.pw.commands;

import link.locutus.discord.Locutus;
import link.locutus.discord.commands.manager.v2.binding.annotation.*;
import link.locutus.discord.commands.manager.v2.command.IMessageIO;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.CoalitionPermission;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.IsAlliance;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.RolePermission;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.WhitelistPermission;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.BankDB;
import link.locutus.discord.db.DiscordDB;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.*;
import link.locutus.discord.db.entities.announce.AnnounceType;
import link.locutus.discord.db.entities.announce.Announcement;
import link.locutus.discord.db.guild.GuildKey;
import link.locutus.discord.pnw.NationOrAlliance;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.*;
import link.locutus.discord.util.discord.DiscordUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PlayerSettingCommands {

    @Command(desc = "View an announcement you have access to")
    @RolePermission(Roles.MEMBER)
    @Ephemeral
    public String viewAnnouncement(@Me IMessageIO io, @Me GuildDB db, @Me DBNation me, @Me User user, int ann_id, @Switch("d") boolean document, @Switch("n") DBNation nation) throws IOException {
        if (nation == null) nation = me;
        if (nation.getId() != me.getId() && !Roles.INTERNAL_AFFAIRS.has(user, db.getGuild())) {
            throw new IllegalArgumentException("Missing role: " + Roles.INTERNAL_AFFAIRS.toDiscordRoleNameElseInstructions(db.getGuild()));
        }
        Announcement parent = db.getAnnouncement(ann_id);
        boolean isInvite = StringUtils.countMatches(parent.replacements, ",") == 2 && !parent.replacements.contains("|") && !parent.replacements.contains("\n");
        AnnounceType type = isInvite ? AnnounceType.INVITE : document ? AnnounceType.DOCUMENT : AnnounceType.MESSAGE;
        if (type == AnnounceType.INVITE) {
            String[] split = parent.replacements.split(",");
            if (!parent.replacements.isEmpty() && split.length > 0 && MathMan.isInteger(split[0])) {
                long serverId = Long.parseLong(split[0]);
                // check user is in guild
                GuildDB otherDb = Locutus.imp().getGuildDB(serverId);
                if (otherDb == null) {
                    throw new IllegalArgumentException("Cannot find server with id: `" + serverId + "`");
                }
                if (otherDb.getGuild().getMember(user) != null) {
                    return "You are already in the server: " + DiscordUtil.getGuildUrl(serverId);
                }
            }
        }

        Announcement.PlayerAnnouncement announcement;
        if (parent.allowCreation) {
            announcement = db.getOrCreatePlayerAnnouncement(ann_id, nation, type);
        } else {
            announcement = db.getPlayerAnnouncement(ann_id, nation.getNation_id());
        }
        String title;
        String message;
        if (announcement == null) {
            if (parent == null) {
                title = "Announcement #" + ann_id + " not found";
                message = "This announcement does not exist";
            } else {
                title = "Announcement #" + ann_id + " was not sent to you";
                message = "This announcement was not sent to you";
            }
        } else {
            title = "[#" + parent.id + "] " + parent.title;
            StringBuilder body = new StringBuilder();
            if (!parent.active) {
                body.append("`Archived`\n");
            }
            String content = announcement.getContent();
            if (document && content.startsWith("https://docs.google.com/document/d/")) {
                content = content.split("\n")[0];
            }
            body.append(">>> " + content);
            body.append("\n\n- Sent by ").append("<@" + parent.sender + ">").append(" ").append(DiscordUtil.timestamp(parent.date, null)).append("\n");
            message = body.toString();

            if (announcement.active) {
                db.setAnnouncementActive(ann_id, nation.getNation_id(), false);
            }
        }

        io.create().append("## " + title + "\n" + message).send();
        return null;
    }


    @Command(desc = "Mark an announcement by the bot as read/unread")
    @RolePermission(Roles.MEMBER)
    public String readAnnouncement(@Me GuildDB db, @Me DBNation nation, int ann_id, @Default Boolean markRead) {
        if (markRead == null) markRead = true;
        db.setAnnouncementActive(ann_id, nation.getNation_id(), !markRead);
        return "Marked announcement #" + ann_id + " as " + (markRead ? "" : "un") + " read";
    }

    @Command(desc = "Opt out of war room relays and ia channel logging")
    public String optOut(@Me User user, DiscordDB db, @Default("true") boolean optOut) {
        byte[] data = new byte[]{(byte) (optOut ? 1 : 0)};
        db.setInfo(DiscordMeta.OPT_OUT, user.getIdLong(), data);
        if (optOut) {
            for (GuildDB guildDB : Locutus.imp().getGuildDatabases().values()) {
                guildDB.deleteInterviewMessages(user.getIdLong());
            }
        }
        return "Set " + DiscordMeta.OPT_OUT + " to " + optOut;
    }

    public static String handleOptOut(Member member, GuildDB db, Roles lcRole, Boolean forceOptOut, Roles... optInRoles) {
        Guild guild = db.getGuild();
        List<Role> optInDiscRoles = new ArrayList<>();
        for (Roles r : optInRoles) {
            Role discR = r.toRole(guild);
            if (discR != null) {
                optInDiscRoles.add(discR);
            }
        }
        List<String> notes = new ArrayList<>();
        if (optInDiscRoles.isEmpty() && optInRoles.length > 0) {
            notes.add("No role `" + Arrays.stream(optInRoles).map(Roles::name).collect(Collectors.joining("`, `")) + "` is set. Have an admin use e.g. "
                    + CM.role.setAlias.cmd.locutusRole(optInRoles[0].name()).discordRole("@someRole")
            );
        }
        Role role = lcRole.toRole(guild);
        if (role == null) {
            // find role by name
            List<Role> roles = db.getGuild().getRolesByName(lcRole.name(), true);
            if (!roles.isEmpty()) {
                role = roles.get(0);
                db.addRole(lcRole, role, 0);
            } else {
                role = RateLimitUtil.complete(guild.createRole().setName(lcRole.name()));
                db.addRole(lcRole, role, 0);
            }
        }

        List<Role> memberRoles = member.getRoles();
        boolean hasAnyOptIn = optInDiscRoles.stream().anyMatch(memberRoles::contains);
        if (memberRoles.contains(role)) {
            if (forceOptOut == Boolean.TRUE) {
                return "You are already opted out of " + lcRole.name() + " alerts";
            }
            RateLimitUtil.complete(guild.removeRoleFromMember(member, role));
            String msg;
            if (!optInDiscRoles.isEmpty() && !hasAnyOptIn) {
                msg = "Your opt out role has been removed (@" + role.getName() + " removed) however you lack a role required to opt back in (@" + Arrays.stream(optInRoles).map(Roles::name).collect(Collectors.joining(", @")) + ")";
            } else {
                msg = "Opted back in to " + lcRole.name() + " alerts (@" + role.getName() + " removed)";
            }
            msg += ". Use the command again to opt out";
            return msg;
        }
        if (forceOptOut == Boolean.FALSE) {
            if (!optInDiscRoles.isEmpty() && !hasAnyOptIn) {
                return "You do not have the opt out role (@" + role.getName() + ") however lack a role to opt in (@" + Arrays.stream(optInRoles).map(Roles::name).collect(Collectors.joining(", @")) + ")";
            }
            return "You are already opted in to " + lcRole.name() + " alerts";
        }
        RateLimitUtil.complete(guild.addRoleToMember(member, role));
        return "Opted out of " + lcRole.name() + " alerts (@" + role.getName() + " added to your user). Use the command again to opt back in";
    }

    @Command(desc = "Toggle your opt out of audit alerts")
    @RolePermission(Roles.MEMBER)
    public String auditAlertOptOut(@Me Member member, @Me DBNation me, @Me Guild guild, @Me GuildDB db) {
        return PlayerSettingCommands.handleOptOut(member, db, Roles.AUDIT_ALERT_OPT_OUT, null);
    }

    @Command(desc = "Toggle your opt out of enemy alerts")
    public String enemyAlertOptOut(@Me GuildDB db, @Me User user, @Me Member member, @Me Guild guild) {
        return PlayerSettingCommands.handleOptOut(member, db, Roles.ENEMY_ALERT_OPT_OUT, null, Roles.ENEMY_ALERT_OFFLINE, Roles.PROTECTION_ALERT);
    }

    @Command(desc = "Set the required transfer market value required for automatic bank alerts\n" +
            "Defaults to $100m, minimum value of 100m")
    @IsAlliance
    public String bankAlertRequiredValue(@Me DBNation me,
                                         @Arg("Require the bank transfer to be worth this much\n" +
                                                 "Resources are valued at weekly market average prices")
                                         double requiredValue) {
        if (requiredValue < 100_000_000) {
            throw new IllegalArgumentException("Minimum value is $100m (you entered: `" + MathMan.format(requiredValue) + "`)");
        }
        me.setMeta(NationMeta.BANK_TRANSFER_REQUIRED_AMOUNT, requiredValue);
        return "Set bank alert required value to $" + MathMan.format(requiredValue);
    }
}

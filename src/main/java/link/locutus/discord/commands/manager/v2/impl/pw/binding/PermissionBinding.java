package link.locutus.discord.commands.manager.v2.impl.pw.binding;

import link.locutus.discord.Locutus;
import link.locutus.discord.commands.manager.v2.binding.BindingHelper;
import link.locutus.discord.commands.manager.v2.binding.annotation.Binding;
import link.locutus.discord.commands.manager.v2.binding.annotation.Me;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.*;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.Coalition;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.db.guild.GuildSetting;
import link.locutus.discord.db.guild.GuildKey;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.DNS;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PermissionBinding extends BindingHelper {

    @Binding(value = "Must be used in a guild registered to a valid in-game alliance")
    @IsAlliance
    public boolean checkAlliance(@Me GuildDB db, IsAlliance perm) {
        if (!db.isValidAlliance()) throw new IllegalArgumentException(db.getGuild() + " is not a valid alliance. See: " + GuildKey.ALLIANCE_ID.getCommandMention() + "");
        return true;
    }

    @Binding(value = "Must be used in a guild with a valid API_KEY configured")
    @HasApi
    public boolean hasApi(@Me GuildDB db, HasApi perm) {
        if (db.getOrNull(GuildKey.API_KEY) == null) throw new IllegalArgumentException("No api key set: " + GuildKey.API_KEY.getCommandMention() + "");
        return true;
    }

    @Binding(value = "Must be run in a guild matching the provided ids")
    @IsGuild
    public boolean checkGuild(@Me Guild guild, IsGuild perm) {
        if (Arrays.stream(perm.value()).noneMatch(f -> f == guild.getIdLong())) {
            throw new IllegalCallerException("Guild does not have permission");
        }
        return true;
    }

    @Binding(value = "Cannot be run in guilds matching the provided ids")
    @NotGuild
    public boolean checkNotGuild(@Me Guild guild, NotGuild perm) {
        if (Arrays.stream(perm.value()).noneMatch(f -> f == guild.getIdLong())) {
            throw new IllegalCallerException("Guild has permission denied");
        }
        return true;
    }

    @Binding(value = "Must be run in a guild that has configured the provided settings")
    @HasKey
    public boolean checkKey(@Me GuildDB db, @Me User author, HasKey perm) {
        if (perm.value() == null || perm.value().length == 0) {
            throw new IllegalArgumentException("No key provided");
        }
        for (String keyName : perm.value()) {
            GuildSetting key = GuildKey.valueOf(keyName.toUpperCase());
            Object value = key.getOrNull(db);
            if (value == null) {
                throw new IllegalArgumentException("Key " + key.name() + " is not set in " + db.getGuild());
            }
            if (perm.checkPermission() && !key.hasPermission(db, author, value)) {
                throw new IllegalCallerException("Key " + key.name() + " does not have permission in " + db.getGuild());
            }
        }
        return true;
    }

    @Binding("Must be run in a guild whitelisted by the bot developer")
    @WhitelistPermission
    public boolean checkWhitelistPermission(@Me GuildDB db, @Me User user, WhitelistPermission perm) {
        if (!db.isWhitelisted()) {
            throw new IllegalCallerException("Guild is not whitelisted");
        }
        if (!Roles.MEMBER.has(user, db.getGuild())) {
            throw new IllegalCallerException("You do not have " + Roles.MEMBER + " " + user.getAsMention() + " see: " + CM.role.setAlias.cmd.toSlashMention());
        }
        return true;
    }

    @Binding("Must be run in a guild added to a coalition by the bot developer")
    @CoalitionPermission(Coalition.ALLIES)
    public boolean checkWhitelistPermission(@Me GuildDB db, CoalitionPermission perm) {
        if (db.getIdLong() == Settings.INSTANCE.ROOT_SERVER) return true;
        Coalition requiredCoalition = perm.value();
        Guild root = Locutus.imp().getServer();
        GuildDB rootDb = Locutus.imp().getGuildDB(root);
        Set<Long> coalitionMembers = rootDb.getCoalitionRaw(requiredCoalition);
        if (coalitionMembers.contains(db.getIdLong())) return true;
        Set<Integer> aaIds = db.getAllianceIds();
        for (int aaId : aaIds) {
            if (coalitionMembers.contains((long) aaId)) return true;
        }
        throw new IllegalCallerException("Guild " + db.getGuild() + " is not in coalition " + requiredCoalition.name());
    }

    @Binding("Deny all use")
    @DenyPermission
    public boolean deny(DenyPermission perm, @Me DBNation nation, @Me User user) {
        throw new IllegalCallerException("Denied by permission: " + nation.getNationUrlMarkup(false) + " | " + user.getAsMention());
    }

    @Binding("Has the aliased roles on discord. \n" +
            "`any` = has any of the roles. \n" +
            "`root` = has role on bot's main guild. \n" +
            "`guild` = has role on that guild. \n" +
            "`alliance` = has role on alliance's guild."
            )
    @RolePermission
    public static boolean checkRole(@Me Guild guild, RolePermission perm, @Me User user) {
        return checkRole(guild, perm, user, null);
    }

    public static boolean checkRole(@Me Guild guild, RolePermission perm, @Me User user, Integer allianceId) {
        if (perm.root()) {
            guild = Locutus.imp().getServer();
        } else if (perm.guild() > 0) {
            guild = Locutus.imp().getDiscordApi().getGuildById(perm.guild());
            if (guild == null) throw new IllegalCallerException("Guild " + perm.guild() + " does not exist" + " " + user.getAsMention() + " (are you sure Locutus is invited?)");
        }
        boolean hasAny = false;
        for (Roles requiredRole : perm.value()) {
            if (allianceId != null && !requiredRole.has(user, guild, allianceId) ||
                    (!requiredRole.has(user, guild) && (!perm.alliance() || requiredRole.getAllowedAccounts(user, guild).isEmpty()))) {
                if (perm.any()) continue;
                throw new IllegalCallerException("You do not have " + requiredRole.name() + " on " + guild + " " + user.getAsMention() + " see: " + CM.role.setAlias.cmd.toSlashMention());
            } else {
                hasAny = true;
            }
        }
        if (!hasAny) {
            // join .name()
            String rolesName = Arrays.asList(perm.value()).stream().map(Roles::name).collect(Collectors.joining(", "));
            throw new IllegalCallerException("You do not have any of `" + rolesName + "` on `" + guild + "` `" + user.getAsMention() + "` see: " + CM.role.setAlias.cmd.toSlashMention());
        }
        return hasAny;
    }
}
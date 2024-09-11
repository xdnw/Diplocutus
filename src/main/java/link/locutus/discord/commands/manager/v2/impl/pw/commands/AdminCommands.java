package link.locutus.discord.commands.manager.v2.impl.pw.commands;

import link.locutus.discord.Logg;
import link.locutus.discord.api.endpoints.DnsQuery;
import link.locutus.discord.api.generated.Nation;
import link.locutus.discord.commands.WarCategory;
import link.locutus.discord.commands.manager.v2.binding.Key;
import link.locutus.discord.commands.manager.v2.binding.LocalValueStore;
import link.locutus.discord.commands.manager.v2.binding.ValueStore;
import link.locutus.discord.commands.manager.v2.binding.bindings.PlaceholderCache;
import link.locutus.discord.commands.manager.v2.command.*;
import link.locutus.discord.commands.manager.v2.impl.pw.filter.NationPlaceholders;
import link.locutus.discord.db.*;
import link.locutus.discord.gpt.GPTUtil;
import link.locutus.discord.util.*;
import link.locutus.discord.web.CommandResult;
import link.locutus.wiki.WikiGenHandler;
import link.locutus.discord.Locutus;
import link.locutus.discord.api.RequestTracker;
import link.locutus.discord.api.ApiKeyPool;
import link.locutus.discord.api.endpoints.DnsApi;
import link.locutus.discord.commands.manager.v2.binding.annotation.*;
import link.locutus.discord.commands.manager.v2.binding.annotation.Timestamp;
import link.locutus.discord.commands.manager.v2.impl.discord.DiscordChannelIO;
import link.locutus.discord.commands.manager.v2.impl.discord.binding.DiscordBindings;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.HasApi;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.RolePermission;
import link.locutus.discord.commands.manager.v2.impl.pw.binding.PWBindings;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.commands.manager.v2.impl.pw.CommandManager2;
import link.locutus.discord.config.Messages;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.entities.*;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.announce.Announcement;
import link.locutus.discord.db.guild.GuildSetting;
import link.locutus.discord.db.guild.GuildKey;
import link.locutus.discord.db.guild.SheetKey;
import link.locutus.discord.event.Event;
import link.locutus.discord.db.entities.metric.AllianceMetric;
import link.locutus.discord.db.entities.Coalition;
import link.locutus.discord.pnw.AllianceList;
import link.locutus.discord.pnw.NationList;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.discord.DiscordUtil;
import link.locutus.discord.util.io.PagePriority;
import link.locutus.discord.util.io.PageRequestQueue;
import link.locutus.discord.util.offshore.Auth;
import link.locutus.discord.util.sheet.SpreadSheet;
import link.locutus.discord.util.task.roles.AutoRoleInfo;
import link.locutus.discord.util.update.AllianceListener;
import com.google.gson.JsonObject;
import link.locutus.discord.api.types.Rank;
import link.locutus.discord.util.update.WarUpdateProcessor;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AdminCommands {

    @Command(desc = "Sync and debug war rooms")
    @RolePermission(Roles.ADMIN)
    public String syncWarrooms(@Me IMessageIO io, @Me JSONObject command, @Me GuildDB db, @Switch("f") boolean force) throws IOException {
        long start = System.currentTimeMillis();

        StringBuilder response = new StringBuilder();
        List<String> errors = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        WarCategory cat = db.getWarChannel(true);
        Guild guild = cat.getGuild();

        Set<Integer> aaIds = cat.getTrackedAllianceIds();
        if (aaIds.isEmpty()) {
            errors.add("No alliances being tracked. Set the `" + Coalition.ALLIES.name() + "` coalition: " + CM.coalition.add.cmd.toSlashMention());
        } else {
            response.append("**Alliances:**: `");
            response.append(aaIds.stream().map(f -> DBAlliance.getOrCreate(f).getMarkdownUrl()).collect(Collectors.joining(",")));
            response.append("`\n");
        }

        if (guild.getChannels().size() >= 500) {
            errors.add("Server at max channels (500)");
        }
        if (guild.getCategories().size() >= 50) {
            errors.add("Server at max categories (50)");
        }

        response.append("**Server:** `" + guild.getName() + "` | `" + guild.getId() + "`");
        if (guild.getIdLong() != db.getIdLong()) response.append(" (WAR_SERVER is set)");
        response.append("\n");

        // list the categories
        Member self = guild.getSelfMember();
        Set<Category> categories = cat.getCategories();

        Map<Category, Set<Permission>> permissionsMissing = new LinkedHashMap<>();

        Permission[] catPerms = cat.getCategoryPermissions();
        for (Category category : categories) {
            EnumSet<Permission> selfPerms = self.getPermissions(category);
            for (Permission perm : catPerms) {
                if (!selfPerms.contains(perm)) {
                    permissionsMissing.computeIfAbsent(category, k -> new LinkedHashSet<>()).add(perm);
                }
            }
        }

        if (categories.isEmpty()) {
            errors.add("No categories found. " +
                    "Create one starting with `warcat`\n" +
                    "Grant the bot the perms: `" + Arrays.stream(catPerms).map(f -> f.getName()).collect(Collectors.joining(", ")) + "`\n");
        } else {
            response.append("**" + categories.size() + " categories:**\n");
            for (Category category : categories) {
                response.append("- " + category.getName());
                Set<Permission> lacking = permissionsMissing.getOrDefault(category, Collections.emptySet());
                if (!lacking.isEmpty()) {
                    response.append(" | missing: `" + lacking.stream().map(f -> f.getName()).collect(Collectors.joining(",")) + "`");
                }
                response.append("\n");
            }
        }

        Map<DBWar, WarCategory.WarCatReason> warsLog = new LinkedHashMap<>();
        Map<DBNation, WarCategory.WarCatReason> inactiveRoomLog = new LinkedHashMap<>();
        Map<DBNation, WarCategory.WarCatReason> activeRoomLog = new LinkedHashMap<>();
        Set<DBNation> toCreate = new LinkedHashSet<>();
        Map<Integer, WarCategory.WarCatReason> toDelete = new LinkedHashMap<>();
        Map<DBNation, TextChannel> toReassign = new LinkedHashMap<>();
        Map<Integer, Set<TextChannel>> duplicates = new LinkedHashMap<>();

        cat.sync(warsLog, inactiveRoomLog, activeRoomLog, toCreate, toDelete, toReassign, duplicates, force);
        if (!warsLog.isEmpty()) {
            response.append("\n**" + warsLog.size() + " wars:**\n");
            for (Map.Entry<DBWar, WarCategory.WarCatReason> entry : warsLog.entrySet()) {
                DBWar war = entry.getKey();
                WarCategory.WarCatReason reason = entry.getValue();
                response.append("- " + war.getWarId() + ": " + reason.name() + " - " + reason.getReason() + "\n");
            }
        }
        if (!inactiveRoomLog.isEmpty()) {
            response.append("\n**" + inactiveRoomLog.size() + " inactive rooms:**\n");
            for (Map.Entry<DBNation, WarCategory.WarCatReason> entry : inactiveRoomLog.entrySet()) {
                DBNation nation = entry.getKey();
                WarCategory.WarCatReason reason = entry.getValue();
                response.append("- " + nation.getNation() + ": " + reason.name() + " - " + reason.getReason() + "\n");
            }
        }
        if (!activeRoomLog.isEmpty()) {
            response.append("\n**" + activeRoomLog.size() + " active rooms:**\n");
            for (Map.Entry<DBNation, WarCategory.WarCatReason> entry : activeRoomLog.entrySet()) {
                DBNation nation = entry.getKey();
                WarCategory.WarCatReason reason = entry.getValue();
                response.append("- " + nation.getNation() + ": " + reason.name() + " - " + reason.getReason() + "\n");
            }
        }
        if (!toCreate.isEmpty()) {
            response.append("\n**" + toCreate.size() + " rooms to create:**\n");
            for (DBNation nation : toCreate) {
                response.append("- " + nation.getMarkdownUrl() + "\n");
            }
        }
        if (!toDelete.isEmpty()) {
            response.append("\n**" + toDelete.size() + " rooms to delete:**\n");
            for (Map.Entry<Integer, WarCategory.WarCatReason> entry : toDelete.entrySet()) {
                int id = entry.getKey();
                WarCategory.WarCatReason reason = entry.getValue();
                response.append("- " + DNS.getMarkdownUrl(id, false) + ": " + reason.name() + " - " + reason.getReason() + "\n");
            }
        }
        if (!toReassign.isEmpty()) {
            response.append("\n**" + toReassign.size() + " rooms to reassign:**\n");
            for (Map.Entry<DBNation, TextChannel> entry : toReassign.entrySet()) {
                DBNation nation = entry.getKey();
                TextChannel channel = entry.getValue();
                response.append("- " + nation.getMarkdownUrl() + " -> " + channel.getAsMention() + "\n");
            }
        }
        if (!duplicates.isEmpty()) {
            response.append("\n**" + duplicates.size() + " duplicate channels:**\n");
            for (Map.Entry<Integer, Set<TextChannel>> entry : duplicates.entrySet()) {
                int id = entry.getKey();
                Set<TextChannel> channels = entry.getValue();
                response.append("- " + DNS.getMarkdownUrl(id, false) + ": " + channels.stream().map(Channel::getAsMention).collect(Collectors.joining(", ")) + "\n");
            }
        }

        StringBuilder full = new StringBuilder();
        if (!errors.isEmpty()) {
            full.append("\n**" + errors.size() + " errors:**\n");
            for (String error : errors) {
                full.append("- " + error + "\n");
            }
        }
        if (!notes.isEmpty()) {
            full.append("\n**" + notes.size() + " notes:**\n");
            for (String note : notes) {
                full.append("- " + note + "\n");
            }
        }
        full.append(response);

        if (!force) {
            String title = "Confirm sync war rooms";
            String body = "See the attached log file for details on room creation, deletion";
            io.create().confirmation(title, body, command).file("warcat.txt", full.toString()).send();
            return null;
        }
        long diff = System.currentTimeMillis() - start;
        io.create().append("Sync war rooms complete. Took: " + diff + "ms\n" +
                "See the attached log file for task output").file("warcat.txt", full.toString()).send();

        return null;
    }

    @Command
    @RolePermission(value = Roles.ADMIN, root = true)
    public String savePojos() throws IOException {
        CommandManager2 manager = Locutus.cmd().getV2();
        manager.getCommands().savePojo(null, CM.class, "CM");
        manager.getNationPlaceholders().getCommands().savePojo(null, CM.class, "NationCommands");
        manager.getAlliancePlaceholders().getCommands().savePojo(null, CM.class, "AllianceCommands");
        return "Done!";
    }
    @Command
    @RolePermission(value = Roles.ADMIN, root = true)
    public String runMilitarizationAlerts() {
        AllianceListener.runMilitarizationAlerts();
        return "Done! (see console)";
    }

    @Command(desc = "Switch a channel to a subscription channel in multiple servers")
    @RolePermission(value = Roles.ADMIN, root = true)
    @Ephemeral
    public String unsetNews(@Me IMessageIO io, @Me JSONObject command,
            GuildSetting setting, Set<GuildDB> guilds, MessageChannel news_channel,
            @Switch("e") boolean unset_on_error,
            @Switch("f") boolean force) {
        if (!(news_channel instanceof NewsChannel)) {
            throw new IllegalArgumentException("Invalid channel type: " + news_channel.getType() + ". Must be a news channel");
        }
        NewsChannel subscribe = (NewsChannel) news_channel;
        GuildDB thisDb = Locutus.imp().getGuildDB(subscribe.getGuild());
        Invite invite = thisDb.getInvite(true);
        // ensure type is instance of Channel
        if (!setting.isChannelType()) {
            return "Invalid setting type: " + setting.getType() + ". Not a channel";
        }

        String errorMessage = "Failed to subscribe to channel: " + subscribe.getAsMention() + "(#" + subscribe.getName() + ") in " + thisDb.getGuild() + " " + invite.getUrl() + "\n" +
                "Please join the server and subscribe manually for future updates";

        List<String> infoMsgs = new ArrayList<>();
        List<String> errorMsgs = new ArrayList<>();

        for (GuildDB otherDb : guilds) {
            if (subscribe.getGuild().getIdLong() == otherDb.getIdLong()) continue;
            String raw = setting.getRaw(otherDb, false);
            if (raw == null) continue;
            Object value = setting.getOrNull(otherDb);
            if (value == null) {
                String msg = otherDb.getGuild().toString() + ": Invalid value `" + raw + "`. See " + CM.admin.settings.unset.cmd.toSlashMention();
                if (unset_on_error) {
                    otherDb.deleteInfo(setting);
                    msg += " (deleted setting)";
                    TextChannel backupChannel = otherDb.getNotifcationChannel();
                    if (backupChannel != null) RateLimitUtil.queue(backupChannel.sendMessage(msg + "\n" + errorMessage));
                }
                errorMsgs.add(msg);
                continue;
            }
            Channel channel = (Channel) value;
            Member self = otherDb.getGuild().getSelfMember();
            if (!(channel instanceof GuildMessageChannel gmc) || !gmc.canTalk()) {
                String msg = otherDb.getGuild().toString() + ": Bot does not have access to " + channel.getAsMention();
                if (force && unset_on_error) {
                    otherDb.deleteInfo(setting);
                    msg += " (deleted setting)";
                    TextChannel backupChannel = otherDb.getNotifcationChannel();
                    if (backupChannel != null) RateLimitUtil.queue(backupChannel.sendMessage(msg + "\n" + errorMessage));
                }
                errorMsgs.add(msg);
                continue;
            }
            if (!(gmc instanceof TextChannel tc)) {
                String msg = otherDb.getGuild().toString() + ": Not set to a Text Channel " + channel.getAsMention();
                if (force && unset_on_error) {
                    otherDb.deleteInfo(setting);
                    msg += " (deleted setting)";
                    TextChannel backupChannel = otherDb.getNotifcationChannel();
                    if (backupChannel != null) RateLimitUtil.queue(backupChannel.sendMessage(msg + "\n" + errorMessage));
                }
                errorMsgs.add(msg);
                continue;
            }
            if (!self.hasPermission(Permission.MANAGE_WEBHOOKS)) {
                String msg = otherDb.getGuild().toString() + ": Bot does not have permission to manage webhooks in " + channel.getAsMention();
                if (force && unset_on_error) {
                    otherDb.deleteInfo(setting);
                    msg += " (deleted setting)";
                    RateLimitUtil.queue(gmc.sendMessage(msg + "\n" + errorMessage));
                }
                errorMsgs.add(msg);
                continue;
            }

            String successMsg = otherDb.getGuild().toString() + ": Unset " + setting.name() + " from " + channel.getAsMention();
            if (!force) {
                infoMsgs.add(successMsg);
                continue;
            }
            try {
                Webhook.WebhookReference result = RateLimitUtil.complete(subscribe.follow(tc));
                otherDb.deleteInfo(setting);
                infoMsgs.add(successMsg + " | " + result);
            } catch (Exception e) {
                String msg = otherDb.getGuild().toString() + ": " + e.getMessage();
                errorMsgs.add(msg);
                if (unset_on_error) {
                    otherDb.deleteInfo(setting);
                    msg += " (deleted setting)";
                    RateLimitUtil.queue(gmc.sendMessage(msg + "\n" + errorMessage));
                }
                continue;

            }
        }
        if (!force) {
            String title = "Confirm unset news";
            StringBuilder body = new StringBuilder("Using news from " + subscribe.getAsMention() + " in " + thisDb.getGuild() + "\n");
            body.append("Unset on error: `" + unset_on_error + "`\n");
            body.append("Servers Subscribed: `" + infoMsgs.size() + "`\n");
            body.append("Errors: `" + errorMsgs.size() + "`\n");
            io.create().confirmation(title, body.toString(), command)
                    .file("info.txt", String.join("\n", infoMsgs))
                    .file("errors.txt", String.join("\n", errorMsgs))
                    .send();
            return null;
        }
        io.create().append("Done! " + infoMsgs.size() + " servers subscribed" +
                " | " + errorMsgs.size() + " errors\n" +
                        "See attached files for details")
                .file("info.txt", String.join("\n", infoMsgs))
                .file("errors.txt", String.join("\n", errorMsgs))
                .send();
        return null;
    }

    @Command(desc = "Bulk unset a guild setting in multiple servers which are invalid based on the provided options")
    @RolePermission(value = Roles.ADMIN, root = true)
    @Ephemeral
    public String unsetKeys(@Me IMessageIO io, @Me JSONObject command,
            Set<GuildSetting> settings, Set<GuildDB> guilds,
                                @Switch("t") boolean unset_cant_talk,
                                @Switch("i") boolean unset_null,
                                @Switch("p") boolean unset_key_no_perms,
                                @Switch("g") boolean unset_invalid_aa,
                                @Switch("a") boolean unset_all,
                                @Switch("v") boolean unset_validate,
                                @Switch("m") String unsetMessage,
                                @Switch("f") boolean force) {
        Map<Guild, Map<GuildSetting, Set<String>>> unsetReasons = new LinkedHashMap<>();

        Set<GuildSetting> channelTypes = new LinkedHashSet<>();
        Set<GuildSetting> nonChanTypes = new LinkedHashSet<>();
        for (GuildSetting setting : settings) {
            Type type = setting.getType().getType();
            if (setting.isChannelType()) {
                channelTypes.add(setting);
            } else {
                nonChanTypes.add(setting);
            }
        }
        for (GuildDB otherDb : guilds) {
            Map<GuildSetting, Boolean> isUnset = new LinkedHashMap<>();
            BiConsumer<GuildSetting, String> unset = (setting, reason) -> {
                if (force) {
                    if (isUnset.getOrDefault(setting, false)) return;
                    isUnset.put(setting, true);
                    String previousValue = setting.getRaw(otherDb, false);
                    Object value = setting.getOrNull(otherDb, false);

                    otherDb.deleteInfo(setting);

                    String message = setting.name() + ": " + reason + ": " + (unsetMessage == null ? "" : unsetMessage) + "\nPrevious value: `" + previousValue + "`";
                    TextChannel sendTo = null;
                    if (value instanceof TextChannel tc && tc.canTalk()) sendTo = tc;
                    if (sendTo == null) sendTo = otherDb.getNotifcationChannel();
                    if (sendTo != null) {
                        try {
                            RateLimitUtil.queue(sendTo.sendMessage(message));
                        } catch (Exception ignore) {
                        }
                    }
                }
            };

            Guild otherGuild = otherDb.getGuild();
            Map<GuildSetting, Set<String>> byGuild = unsetReasons.computeIfAbsent(otherGuild, k -> new LinkedHashMap<>());
            // only channel modes
            for (GuildSetting setting : channelTypes) {
                Channel channel = (Channel) setting.getOrNull(otherDb);
                if (channel == null) continue;
                if (unset_cant_talk) {
                    if (!(channel instanceof GuildMessageChannel gmc) || !gmc.canTalk()) {
                        if (force) unset.accept(setting, "No Talk Permissions in " + channel.getAsMention());
                        byGuild.computeIfAbsent(setting, k -> new LinkedHashSet<>()).add("Can't talk");
                        continue;
                    }
                }
            }
//            // only non channel modes
//            for (GuildSetting setting : nonChanTypes) {
//
//            }
            // all type modes
            for (GuildSetting setting : settings) {
                String raw = setting.getRaw(otherDb, false);
                if (raw == null) continue;
                Object value = setting.getOrNull(otherDb);
                if (unset_null) {
                    if (value == null) {
                        if (force) unset.accept(setting, "Invalid value (null)");
                        byGuild.computeIfAbsent(setting, k -> new LinkedHashSet<>()).add("Null");
                        continue;
                    }
                }
                if (unset_key_no_perms) {
                    String notAllowedReason = "Not allowed";
                    boolean allowed = false;
                    try {
                        allowed = setting.allowed(otherDb, true);
                    } catch (IllegalArgumentException e) {
                        notAllowedReason = e.getMessage();
                    }
                    if (!allowed) {
                        if (force) unset.accept(setting, notAllowedReason);
                        byGuild.computeIfAbsent(setting, k -> new LinkedHashSet<>()).add(notAllowedReason);
                        continue;
                    }
                }
                if (unset_validate) {
                    String validateReason = "Invalid Value (validation error)";
                    boolean valid = false;
                    try {
                        setting.validate(otherDb, null, value);
                        valid = true;
                    } catch (IllegalArgumentException e) {
                        validateReason = e.getMessage();
                    }
                    if (!valid) {
                        if (force) unset.accept(setting, validateReason);
                        byGuild.computeIfAbsent(setting, k -> new LinkedHashSet<>()).add(validateReason);
                        continue;
                    }

                }
                if (unset_invalid_aa) {
                    if (!otherDb.isValidAlliance()) {
//                        if (force) otherDb.deleteInfo(setting);
                        if (force) unset.accept(setting, "No valid Alliance registered");
                        byGuild.computeIfAbsent(setting, k -> new LinkedHashSet<>()).add("Invalid AA");
                        continue;
                    }
                }
                if (unset_all) {
//                    if (force) otherDb.deleteInfo(setting);
                    if (force) unset.accept(setting, "Setting removed by administrator.");
                    byGuild.computeIfAbsent(setting, k -> new LinkedHashSet<>()).add("All");
                }
            }
        }
        StringBuilder msg = new StringBuilder();
        unsetReasons.entrySet().removeIf(e -> e.getValue().isEmpty());
        for (Map.Entry<Guild, Map<GuildSetting, Set<String>>> entry : unsetReasons.entrySet()) {
            Guild guild = entry.getKey();
            Map<GuildSetting, Set<String>> reasons = entry.getValue();
            msg.append(guild.toString()).append(":\n");
            for (Map.Entry<GuildSetting, Set<String>> reason : reasons.entrySet()) {
                GuildSetting setting = reason.getKey();
                Set<String> reasonSet = reason.getValue();
                msg.append("- ").append(setting.name()).append(": ").append(String.join(", ", reasonSet)).append("\n");
            }
        }

        if (!force) {
            String title = "Confirm unset keys";
            StringBuilder body = new StringBuilder("Unset " + settings.size() + " keys for " + guilds.size() + " servers\n");
            body.append("Unset on error: `" + unset_null + "`\n");
            body.append("Servers affected: `" + unsetReasons.size() + "`\n");
            io.create().confirmation(title, body.toString(), command)
                    .file("unset.txt", msg.toString())
                    .send();
            return null;
        }
        io.create().append("Done! " + settings.size() + " keys unset" +
                " | " + unsetReasons.size() + " servers affected\n" +
                "See attached file for details")
                .file("unset.txt", msg.toString())
                .send();
        return null;
    }

    @Command
    @RolePermission(value = Roles.ADMIN, root = true)
    @Ephemeral
    public String infoBulk(@Me GuildDB db, @Me IMessageIO io, GuildSetting setting, Set<GuildDB> guilds, @Switch("s") SpreadSheet sheet) throws GeneralSecurityException, IOException {
        if (sheet == null) {
            sheet = SpreadSheet.create(db, SheetKey.SETTINGS_SERVERS);
        }

        // admin bulk_setting key
        //- include alliances registered column (alliance ids)
        //- have column for coalitions guild is in
        //- have column for isValidAlliance
        //- have column for number of active members 7200 in the alliance

        List<String> header = new ArrayList<>(Arrays.asList(
            "guild",
            "guild_name",
            "owner",
            "owner_id",
            "setting",
            "raw",
            "readable",
            "is_invalid",
            "lack_perms",
            "root_col",
            "alliances",
            "aa_valid",
            "active_aa_members"
        ));

        sheet.setHeader(header);

        GuildDB root = Locutus.imp().getRootDb();


        for (GuildDB otherDb : guilds) {
            String raw = setting.getRaw(otherDb, false);
            if (raw == null) continue;
            Object value = setting.getOrNull(otherDb, false);
            String readable = value == null ? "![NULL]" : setting.toReadableString(db, value);
            boolean noPerms = !setting.allowed(db);

            header.set(0, otherDb.getIdLong() + "");
            header.set(1, otherDb.getGuild().getName());
            long ownerId = otherDb.getGuild().getOwnerIdLong();
            header.set(2, DiscordUtil.getUserName(ownerId));
            header.set(3, ownerId + "");
            header.set(4, setting.name());
            header.set(5, raw);
            header.set(6, readable);
            header.set(7, value == null ? "true" : "false");
            header.set(8, noPerms ? "true" : "false");
            Set<Coalition> hasOnRoot = Arrays.stream(Coalition.values()).filter(otherDb::hasCoalitionPermsOnRoot).collect(Collectors.toSet());
            header.set(9, hasOnRoot.stream().map(Coalition::name).collect(Collectors.joining(",")));
            Set<Integer> aaIds = otherDb.getAllianceIds();
            header.set(10, aaIds == null ? "" : aaIds.toString());
            header.set(11, otherDb.isValidAlliance() ? "true" : "false");
            AllianceList aaList = otherDb.getAllianceList();
            int activeMembers = aaList == null ? -1 : aaList.getNations(true, 7200, true).size();
            header.set(12, activeMembers + "");

            sheet.addRow(header);
        }

        sheet.updateClearCurrentTab();
        sheet.updateWrite();

        sheet.attach(io.create(), "setting_servers").send();
        return null;
    }

    @Command
    @RolePermission(value = Roles.ADMIN, root = true)
    public String checkActiveConflicts() {
        WarUpdateProcessor.checkActiveConflicts();
        return "Done! (see console)";
    }

    @Ephemeral
    @Command(desc = "Dump the URL requests to console")
    @RolePermission(value = Roles.ADMIN, root = true)
    public String showFileQueue(@Me IMessageIO io,
                                @Arg("Specify a timestamp to attach a tallied log of requests over a timeframe\n" +
                                        "Instead of just a summary of current items in the queue")
                                @Default @Timestamp Long timestamp,
                                @Arg("The number of top results to include\n" +
                                        "Default: 25")
                                @Switch("r") Integer numResults) throws URISyntaxException {
        PageRequestQueue handler = FileUtil.getPageRequestQueue();
        List<PageRequestQueue.PageRequestTask<?>> jQueue = handler.getQueue();

        Map<PagePriority, Integer> pagePriorities = new HashMap<>();
        int unknown = 0;
        int size = 0;
        synchronized (jQueue) {
            ArrayList<PageRequestQueue.PageRequestTask<?>> copy = new ArrayList<>(jQueue);
            size = copy.size();
            for (PageRequestQueue.PageRequestTask<?> task : copy) {
                long priority = task.getPriority();
                int ordinal = (int) (priority / Integer.MAX_VALUE);
                if (ordinal >= PagePriority.values.length) unknown++;
                else {
                    PagePriority pagePriority = PagePriority.values[ordinal];
                    pagePriorities.put(pagePriority, pagePriorities.getOrDefault(pagePriority, 0) + 1);
                }
            }
        }
        List<Map.Entry<PagePriority, Integer>> entries = new ArrayList<>(pagePriorities.entrySet());
        // sort
        entries.sort((o1, o2) -> o2.getValue() - o1.getValue());
        if (numResults == null) numResults = 25;

        StringBuilder sb = new StringBuilder();
        sb.append("**File Queue:** " + size + "\n");
        for (Map.Entry<PagePriority, Integer> entry : entries) {
            sb.append(entry.getKey().name()).append(": ").append(entry.getValue()).append("\n");
        }
        if (unknown > 0) {
            sb.append("Unknown: ").append(unknown).append("\n");
        }

        if (timestamp != null) {
            RequestTracker tracker = handler.getTracker();
            Map<String, Integer> byDomain = tracker.getCountByDomain(timestamp);
            Map<String, Integer> byUrl = tracker.getCountByUrl(timestamp);

            sb.append("\n**By Domain:**\n");
            int domainI = 1;
            for (Map.Entry<String, Integer> entry : byDomain.entrySet()) {
                sb.append("- " + entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                if (domainI++ >= numResults) break;
            }

            sb.append("\n**By URL:**\n");
            int urlI = 1;
            for (Map.Entry<String, Integer> entry : byUrl.entrySet()) {
                sb.append("- " + entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                if (urlI++ >= numResults) break;
            }

            RequestTracker v3Tracker = FileUtil.getPageRequestQueue().getTracker();
            Map<String, Integer> v3Request = v3Tracker.getCountByDomain(timestamp);
            if (!v3Request.isEmpty()) {
                sb.append("\n**V3 By Domain:**\n");
                int v3I = 1;
                for (Map.Entry<String, Integer> entry : v3Request.entrySet()) {
                    sb.append("- " + entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                    if (v3I++ >= numResults) break;
                }
            }

        }


        if (numResults > 25) {
            io.create().file("queue.txt", sb.toString()).send();
            return null;
        } else {
            return sb.toString();
        }
    }

    @Command
    @RolePermission(value = Roles.ADMIN, root = true)
    public String dumpWiki(@Default String pathRelative) throws IOException, InvocationTargetException, IllegalAccessException {
        if (pathRelative == null) pathRelative = "../locutus.wiki";
        CommandManager2 manager = Locutus.imp().getCommandManager().getV2();
        WikiGenHandler generator = new WikiGenHandler(pathRelative, manager);
        generator.writeDefaults();

        return "Done!";
    }

    @Command
    @RolePermission(value = Roles.ADMIN, root = true)
    public String syncWars(DBAlliance alliance, boolean forceAll) throws IOException, ParseException {
        Locutus.imp().getWarDb().fetchWars(forceAll, alliance, Event::post);
        return "Done!";
    }

    @Command
    @RolePermission(value = Roles.ADMIN, root = true)
    public String deleteAllInaccessibleChannels(@Switch("f") boolean force) {
        Map<GuildDB, List<GuildSetting>> toUnset = new LinkedHashMap<>();

        for (GuildDB db : Locutus.imp().getGuildDatabases().values()) {
            if (force) {
                List<GuildSetting> keys = db.listInaccessibleChannelKeys();
                if (!keys.isEmpty()) {
                    toUnset.put(db, keys);
                }
            } else {
                db.unsetInaccessibleChannels();
            }
        }

        if (toUnset.isEmpty()) {
            return "No keys to unset";
        }
        StringBuilder response = new StringBuilder();
        for (Map.Entry<GuildDB, List<GuildSetting>> entry : toUnset.entrySet()) {
            response.append(entry.getKey().getGuild().toString() + ":\n");
            List<String> keys = entry.getValue().stream().map(f -> f.name()).collect(Collectors.toList());
            response.append("- " + StringMan.join(keys, "\n- "));
            response.append("\n");
        }
        String footer = "Rerun the command with `-f` to confirm";
        return response + footer;
    }

    @Command
    @RolePermission(value = Roles.ADMIN, root = true)
    public void stop(boolean save) {
        Locutus.imp().stop();
    }

    @Command(desc = "Set the archive status of the bot's announcement")
    @RolePermission(any = true, value = {Roles.INTERNAL_AFFAIRS, Roles.MILCOM, Roles.ADMIN, Roles.FOREIGN_AFFAIRS, Roles.ECON})
    public String archiveAnnouncement(@Me GuildDB db, int announcementId, @Default Boolean archive) {
        if (archive == null) archive = true;
        db.setAnnouncementActive(announcementId, !archive);
        return (archive ? "Archived" : "Unarchived") + " announcement with id: #" + announcementId;
    }

    @Command(desc = "Find the announcement for the closest matching invite")
    @RolePermission(Roles.ADMIN)
    @NoFormat
    public String find_invite(@Me GuildDB db, String invite) throws IOException {
        List<Announcement.PlayerAnnouncement> matches = db.getPlayerAnnouncementsContaining(invite);
        if (matches.isEmpty()) {
            return "No announcements found with content: `" + invite + "`";
        } else {
            return "Found " + matches.size() + " matches:\n- " +
                    matches.stream().map(f -> "{ID:" + f.ann_id + ", receiver:" + f.receiverNation + "}").collect(Collectors.joining("\n- "));
        }
    }

    @Command(desc = "Find the announcement closest matching a message")
    @RolePermission(Roles.ADMIN)
    @NoFormat
    public String find_announcement(@Me GuildDB db, int announcementId, String message) throws IOException {
        List<Announcement.PlayerAnnouncement> announcements = db.getPlayerAnnouncementsByAnnId(announcementId);
        if (announcements.isEmpty()) {
            return "No announcements found with id: #" + announcementId;
        }
        long diffMin = Long.MAX_VALUE;
        List<Announcement.PlayerAnnouncement> matches = new ArrayList<>();
        for (Announcement.PlayerAnnouncement announcement : announcements) {
            String content = announcement.getContent();
            if (message.equalsIgnoreCase(content)) {
                return "Announcement sent to nation id: " + announcement.receiverNation;
            }
            byte[] diff = StringMan.getDiffBytes(message, content);
            if (diff.length < diffMin) {
                diffMin = diff.length;
                matches.clear();
                matches.add(announcement);
            } else if (diff.length == diffMin) {
                matches.add(announcement);
            }
        }

        if (matches.isEmpty()) {
            return "No announcements found with id: #" + announcementId;
        } else if (matches.size() == 1) {
            Announcement.PlayerAnnouncement match = matches.get(0);
            return "Closest match: " + match.receiverNation + " with " + diffMin + " differences:\n```\n" + match.getContent() + "\n```";
        } else {
            StringBuilder response = new StringBuilder();
            response.append(matches.size() + " matches with " + diffMin + " differences:\n");
            for (Announcement.PlayerAnnouncement match : matches) {
                response.append("- " + match.receiverNation + "\n");
                // content in ```
                response.append("```\n" + match.getContent() + "\n```\n");
            }
            return response.toString();
        }
    }

    @Command(desc = "Send an announcement to multiple nations, with random variations for each receiver\n")
    @RolePermission(Roles.ADMIN)
    @HasApi
    @NoFormat
    public String announce(@Me GuildDB db, @Me Guild guild, @Me JSONObject command, @Me IMessageIO currentChannel,
                           @Me User author,
                           NationList sendTo,
                           @Arg("The subject used for sending an in-game mail if a discord direct message fails") String subject,
                           @Arg("The message you want to send") @TextArea String announcement,
                           @Arg("Lines of replacement words or phrases, separated by `|` for each variation\n" +
                                   "Add multiple lines for each replacement you want\n" +
                                   "You can use \\n for newline for discord slash commands") @TextArea String replacements,
                           @Arg("The channel to post the announcement to (must be same server)") @Switch("c") MessageChannel channel,
                           @Arg("The text to post in the channel below the hidden announcement (e.g. mentions)") @Switch("b") String bottomText,
                           @Arg("The required number of differences between each message") @Switch("v") @Default("0") Integer requiredVariation,
                           @Arg("The required depth of changes from the original message") @Switch("r") @Default("0") Integer requiredDepth,
                           @Arg("Variation seed. The same seed will produce the same variations, otherwise results are random") @Switch("s") Long seed,
                           @Arg("If messages are sent via discord direct message") @Switch("d") boolean sendDM,
                           @Switch("f") boolean force) throws IOException {
        // ensure channel is in same server or null
        if (channel != null && ((GuildMessageChannel) channel).getGuild().getIdLong() != guild.getIdLong()) {
            throw new IllegalArgumentException("Channel must be in the same server: " + ((GuildMessageChannel) channel).getGuild() + " != " + guild);
        }
        if (bottomText != null && channel == null) {
            throw new IllegalArgumentException("Bottom text requires a channel");
        }

        Set<Integer> aaIds = db.getAllianceIds();
        if (sendDM) {
            GPTUtil.checkThrowModeration(announcement + "\n" + replacements);
        }

        List<String> errors = new ArrayList<>();
        Collection<DBNation> nations = sendTo.getNations();
        for (DBNation nation : nations) {
            User user = nation.getUser();
            if (user == null) {
                errors.add("Cannot find user for `" + nation.getNation() + "`");
            } else if (guild.getMember(user) == null) {
                errors.add("Cannot find member in guild for `" + nation.getNation() + "` | `" + user.getName() + "`");
            } else {
                continue;
            }
            if (!aaIds.isEmpty() && !aaIds.contains(nation.getAlliance_id())) {
                throw new IllegalArgumentException("Cannot send to nation not in alliance: " + nation.getNation() + " | " + user);
            }
            if (!force) {
                if (nation.active_m() > 20000)
                    return "The " + nations.size() + " receivers includes inactive for >2 weeks. Use `" + sendTo.getFilter() + ",#active_m<20000` or set `force` to confirm";
                if (nation.isVacation())
                    return "The " + nations.size() + " receivers includes vacation mode nations. Use `" + sendTo.getFilter() + ",#vm_turns=0` or set `force` to confirm";
                if (nation.getPosition() < 1) {
                    return "The " + nations.size() + " receivers includes applicants. Use `" + sendTo.getFilter() + ",#position>1` or set `force` to confirm";
                }
            }
        }

        List<String> replacementLines = Announcement.getReplacements(replacements);
        Random random = seed == null ? new Random() : new Random(seed);
        Set<String> results = StringMan.enumerateReplacements(announcement, replacementLines, nations.size() + 1000, requiredVariation, requiredDepth);

        if (results.size() < nations.size()) return "Not enough entropy. Please provide more replacements";

        if (!force) {
            StringBuilder confirmBody = new StringBuilder();
            if (!sendDM) confirmBody.append("**Warning: No ingame or direct message option has been specified**\n");
            confirmBody.append("Send DM (`-d`): " + sendDM).append("\n");
            if (!errors.isEmpty()) {
                confirmBody.append("\n**Errors**:\n- " + StringMan.join(errors, "\n- ")).append("\n");
            }
//            DiscordUtil.createEmbedCommand(currentChannel, "Send to " + nations.size() + " nations", confirmBody + "\nPress to confirm", );
            DiscordUtil.pending(currentChannel, command, "Send to " + nations.size() + " nations", confirmBody + "\nPress to confirm");
            return null;
        }

        currentChannel.send("Please wait...");

        List<String> resultsArray = new ArrayList<>(results);
        Collections.shuffle(resultsArray, random);

        resultsArray = resultsArray.subList(0, nations.size());

        List<Integer> failedToDM = new ArrayList<>();
        List<Integer> failedToMail = new ArrayList<>();

        StringBuilder output = new StringBuilder();

        Map<DBNation, String> sentMessages = new HashMap<>();

        int i = 0;
        for (DBNation nation : nations) {
            String replaced = resultsArray.get(i++);
            String personal = replaced + "\n\n- " + author.getAsMention() + " " + guild.getName();

            boolean result = sendDM && nation.sendDM(personal);
            if (!result && sendDM) {
                failedToDM.add(nation.getNation_id());
            }
            if ((!result && sendDM)) {
                failedToMail.add(nation.getNation_id());
            }

            sentMessages.put(nation, replaced);

            output.append("\n\n```" + replaced + "```" + "^ " + nation.getNation());
        }

        output.append("\n\n------\n");
        if (errors.size() > 0) {
            output.append("Errors:\n- " + StringMan.join(errors, "\n- "));
        }
        if (failedToDM.size() > 0) {
            output.append("\nFailed DM (sent ingame): " + StringMan.getString(failedToDM));
        }
        if (failedToMail.size() > 0) {
            output.append("\nFailed Mail: " + StringMan.getString(failedToMail));
        }

        int annId = db.addAnnouncement(author, subject, announcement, replacements, sendTo.getFilter(), false);
        output.append("\n\nAnnouncement ID: " + annId);
        for (Map.Entry<DBNation, String> entry : sentMessages.entrySet()) {
            byte[] diff = StringMan.getDiffBytes(announcement, entry.getValue());
            db.addPlayerAnnouncement(entry.getKey(), annId, diff);
        }

        if (channel != null) {
            IMessageBuilder msg = new DiscordChannelIO(channel).create();
            StringBuilder body = new StringBuilder();
            body.append("From: " + author.getAsMention() + "\n");
            body.append("To: `" + sendTo.getFilter() + "`\n");

            if (sendDM) {
                body.append("- A copy of this announcement has been sent as a direct message\n");
            }

            body.append("\n\nPress `view` to view the announcement");

            msg = msg.embed("[#" + annId + "] " + subject, body.toString());
            if (bottomText != null && !bottomText.isEmpty()) {
                msg = msg.append(bottomText);
            }

            CM.announcement.view cmd = CM.announcement.view.cmd.ann_id(annId + "");
            msg.commandButton(CommandBehavior.EPHEMERAL, cmd, "view").send();
        }

        return output.toString().trim();
    }

    @Command(desc = "Add or remove a role from a set of members on discord based on a spreadsheet\n" +
            "By default only roles will be added, specify `removeRoles` to remove roles from users not assigned the role in the sheet\n" +
            "Specify `listMissing` to list nations that are not assigned a role in the sheet\n" +
            "Columns:\n" +
            "- `nation`, `leader`, `user`, `member` (at least one)\n" +
            "- `role`, `role1`, `roleN` (multiple, or use comma separated values in one cell)")
    @RolePermission(value = Roles.ADMIN)
    public String maskSheet(@Me IMessageIO io, @Me GuildDB db, @Me Guild guild, @Me JSONObject command,
                            SpreadSheet sheet,
                            @Arg("Remove these roles from users not assigned the role in the sheet")
                            @Switch("u") Set<Role> removeRoles,
                            @Arg("Remove all roles mentioned in the sheet")
                            @Switch("ra") boolean removeAll,
                            @Arg("List nations that are not assigned a role in the sheet")
                            @Switch("ln") Set<DBNation> listMissing,
                            @Switch("f") boolean force) {
        sheet.loadValues(null, true);
        List<Object> nations = sheet.findColumn("nation");
        List<Object> leaders = sheet.findColumn("leader");
        List<Object> users = sheet.findColumn(-1, f -> {
            String lower = f.toLowerCase(Locale.ROOT);
            return lower.startsWith("user") || lower.startsWith("member");
        });
        if (nations == null && leaders == null && users == null) {
            throw new IllegalArgumentException("Expecting column `nation` or `leader` or `user` or `member`");
        }
        Map<String, List<Object>> roles = sheet.findColumn(-1, f -> f.startsWith("role"), true);
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("Expecting at least one column starting with `role`");
        }
        if (removeAll) {
            Set<String> parsed = new LinkedHashSet<>();
            for (Map.Entry<String, List<Object>> entry : roles.entrySet()) {
                String columnName = entry.getKey();
                List<Object> roleValues = entry.getValue();
                if (roleValues == null || roleValues.isEmpty()) {
                    continue;
                }
                for (int i = 0; i < roleValues.size(); i++) {
                    Object roleCell = roleValues.get(i);
                    if (roleCell == null) {
                        continue;
                    }
                    String roleNameList = roleCell.toString();
                    for (String roleName : roleNameList.split(",")) {
                        roleName = roleName.trim();
                        if (parsed.contains(roleName)) continue;
                        parsed.add(roleName);
                        try {
                            Role role = DiscordBindings.role(guild, roleName);
                            if (removeRoles == null) removeRoles = new LinkedHashSet<>();
                            removeRoles.add(role);
                        } catch (IllegalArgumentException e) {
                            continue;
                        }
                    }
                }
            }
        }

        List<String> errors = new ArrayList<>();
        Map<Member, Set<Role>> existingRoles = new LinkedHashMap<>();
        Map<Member, Set<Role>> rolesAllowed = new LinkedHashMap<>();
        Map<Member, Set<Role>> rolesAdded = new LinkedHashMap<>();
        Map<Member, Set<Role>> rolesRemoved = new LinkedHashMap<>();

        Set<DBNation> nationsInSheet = new LinkedHashSet<>();

        int max = Math.max(Math.max(nations == null ? 0 : nations.size(), leaders == null ? 0 : leaders.size()), users == null ? 0 : users.size());
        for (int i = 0; i < max; i++) {
            Object nationObj = nations == null || nations.size() < i ? null : nations.get(i);
            String nationStr = nationObj == null ? null : nationObj.toString();

            Object leaderObj = leaders == null || leaders.size() < i ? null : leaders.get(i);
            String leaderStr = leaderObj == null ? null : leaderObj.toString();

            Object userObj = users == null || users.size() < i ? null : users.get(i);
            String userStr = userObj == null ? null : userObj.toString();

            String input = nationStr == null ? leaderStr == null ? userStr : leaderStr : nationStr;

            User user = null;
            try {
                if (userStr != null) {
                    user = DiscordBindings.user(null, userStr);
                } else if (nationStr != null) {
                    DBNation nation = PWBindings.nation(null, nationStr);
                    if (nation != null) {
                        user = nation.getUser();
                        if (user == null) {
                            errors.add("[Row:" + (i + 2) + "] Nation has no user: " + nation.getMarkdownUrl());
                        }
                    } else {
                        errors.add("[Row:" + (i + 2) + "] Nation not found: `" + nationStr + "`");
                    }
                } else if (leaderStr != null) {
                    DBNation nation = Locutus.imp().getNationDB().getNationByLeader(leaderStr);
                    if (nation != null) {
                        user = nation.getUser();
                        if (user == null) {
                            errors.add("[Row:" + (i + 2) + "] Nation has no user: " + nation.getMarkdownUrl());
                        }
                    } else {
                        errors.add("[Row:" + (i + 2) + "] Nation Leader not found: `" + leaderStr + "`");
                    }
                }
            } catch (IllegalArgumentException e) {
                errors.add("[Row:" + (i + 2) + "] " + e.getMessage());
            }
            if (user == null) continue;
            if (listMissing != null) {
                DBNation nation = DBNation.getByUser(user);
                if (nation != null) {
                    nationsInSheet.add(nation);
                }
            }
            Member member = guild.getMember(user);
            if (member == null) {
                errors.add("[Row:" + (i + 2) + "] User `" + user.getName() + " not found ` in " + guild.toString());
                continue;
            }

            for (Map.Entry<String, List<Object>> entry : roles.entrySet()) {
                String columnName = entry.getKey();
                List<Object> roleValues = entry.getValue();
                if (roleValues == null || roleValues.isEmpty() || roleValues.size() < i) {
                    continue;
                }
                Object roleCell = roleValues.get(i);
                if (roleCell == null) {
                    continue;
                }
                String roleNameList = roleCell.toString();
                for (String roleName : roleNameList.split(",")) {
                    roleName = roleName.trim();
                    try {
                        Role role = DiscordBindings.role(guild, roleName);
                        rolesAllowed.computeIfAbsent(member, f -> new LinkedHashSet<>()).add(role);
                        if (existingRoles.computeIfAbsent(member, f -> new HashSet<>(f.getRoles())).contains(role)) {
                            continue;
                        }
                        rolesAdded.computeIfAbsent(member, f -> new LinkedHashSet<>()).add(role);
                    } catch (IllegalArgumentException e) {
                        errors.add("[Row:" + (i + 2) + ",Column:" + columnName + "] `" + input + "` -> `" + roleName + "`: " + e.getMessage());
                        continue;
                    }
                }
            }
        }

        if (removeRoles != null && !removeRoles.isEmpty()) {
            for (Member member : guild.getMembers()) {
                if (member.getUser().isBot()) continue;
                Set<Role> granted = rolesAllowed.getOrDefault(member, Collections.emptySet());
                for (Role role : removeRoles) {
                    if (!granted.contains(role)) {
                        if (existingRoles.computeIfAbsent(member, f -> new HashSet<>(f.getRoles())).contains(role)) {
                            rolesRemoved.computeIfAbsent(member, f -> new LinkedHashSet<>()).add(role);
                        }
                    }
                }
            }
        }

        StringBuilder body = new StringBuilder();
        body.append("**Sheet**: <" + sheet.getURL() + ">\n");
        IMessageBuilder msg = io.create();

        if (listMissing != null) {
            StringBuilder listMissingMessage = new StringBuilder();
            Set<DBNation> missingNations = listMissing.stream().filter(f -> !nationsInSheet.contains(f)).collect(Collectors.toSet());
            if (!missingNations.isEmpty()) {
                listMissingMessage.append("nation,leader,url,username,user_id\n");
                for (DBNation nation : missingNations) {
                    String name = nation.getName();
                    String leader = nation.getLeader();
                    String url = nation.getUrl();
                    String user = nation.getUserDiscriminator();
                    Long userId = nation.getUserId();
                    listMissingMessage.append(name).append(",")
                            .append(leader).append(",")
                            .append(url).append(",")
                            .append(user == null ? "" : user).append(",")
                            .append(userId == null ? "" : userId).append("\n");
                }
                body.append("**listMissing**: `").append(missingNations.size() + "`\n");
                msg = msg.file("missing_nations.csv", listMissingMessage.toString());
            } else {
                body.append("**listMissing**: No missing nations\n");
            }
        }

        if (removeRoles != null) {
            body.append("**removeRoles**: `").append(removeRoles.stream().map(Role::getName).collect(Collectors.joining(","))).append("`\n");
        }

        if (rolesRemoved.isEmpty() && rolesAdded.isEmpty()) {
            msg.append("\n**Result**: No roles to add or remove").send();
            return null;
        }

        AutoRoleInfo info = new AutoRoleInfo(db, body.toString());
        for (Map.Entry<Member, Set<Role>> entry : rolesAdded.entrySet()) {
            Member member = entry.getKey();
            for (Role role : entry.getValue()) {
                info.addRoleToMember(member, role);
            }
        }
        for (Map.Entry<Member, Set<Role>> entry : rolesRemoved.entrySet()) {
            Member member = entry.getKey();
            for (Role role : entry.getValue()) {
                info.removeRoleFromMember(member, role);
            }
        }
        if (!force) {
            String changeStr = info.toString();
            if (body.length() + changeStr.length() >= 2000) {
                msg = msg.file("role_changes.txt", changeStr);
            } else {
                body.append("\n\n------------\n\n" + changeStr);
            }
            msg.confirmation("Confirm bulk role change", body.toString(), command).send();
            return null;
        }
        io.send("Please wait...");
        info.execute();
        return info.getChangesAndErrorMessage();
    }

    @Command(desc = "Add or remove a role from a set of members on discord")
    @RolePermission(Roles.ADMIN)
    public String mask(@Me Member me, @Me GuildDB db, Set<Member> members, Role role, boolean value, @Arg("If the role should be added or removed from all other members\n" +
            "If `value` is true, the role will be removed, else added") @Switch("r") boolean toggleMaskFromOthers) {
        List<Role> myRoles = me.getRoles();
        List<String> response = new ArrayList<>();
        for (Member member : members) {
            User user = member.getUser();
            List<Role> roles = member.getRoles();
            if (value && roles.contains(role)) {
                response.add(user.getName() + " already has the role: `" + role + "`");
                continue;
            } else if (!value && !roles.contains(role)) {
                response.add(user.getName() + ": does not have the role: `" + role + "`");
                continue;
            }
            if (value) {
                RateLimitUtil.queue(db.getGuild().addRoleToMember(member, role));
                response.add(user.getName() + ": Added role to member");
            } else {
                RateLimitUtil.queue(db.getGuild().removeRoleFromMember(member, role));
                response.add(user.getName() + ": Removed role from member");
            }
        }
        if (toggleMaskFromOthers) {
            for (Member member : db.getGuild().getMembers()) {
                if (members.contains(member)) continue;
                List<Role> memberRoles = member.getRoles();
                if (value) {
                    if (memberRoles.contains(role)) {
                        RateLimitUtil.queue(db.getGuild().removeRoleFromMember(member, role));
                        response.add(member.getUser().getName() + ": Removed role from member");
                    }
                } else {
                    if (!memberRoles.contains(role)) {
                        RateLimitUtil.queue(db.getGuild().addRoleToMember(member, role));
                        response.add(member.getUser().getName() + ": Added role to member");
                    }
                }
            }
        }
        return StringMan.join(response, "\n").trim();
    }

    @Command
    @RolePermission(value = Roles.MAIL, root = true)
    public String dm(@Me User author, @Me Guild guild, @Me IMessageIO io, @Me JSONObject command, Set<DBNation> nations, String message, @Switch("f") boolean force) {
        if (nations.size() > 500) {
            throw new IllegalArgumentException("Too many nations: " + nations.size() + " (max 500)");
        }
        if (!force) {
            String title = "Send " + nations.size() + " messages";
            Set<Integer> alliances = new LinkedHashSet<>();
            for (DBNation nation : nations) alliances.add(nation.getAlliance_id());
            String embedTitle = title + " to nations.";
            if (alliances.size() != 1) embedTitle += " in " + alliances.size() + " alliances.";
            String dmMsg = "content: ```" + message + "```";
            io.create().embed(embedTitle, dmMsg).confirmation(command).send();
            return null;
        }
        boolean hasAdmin = Roles.ADMIN.hasOnRoot(author);
        List<String> errors = new ArrayList<>();
        List<User> users = new ArrayList<>();
        for (DBNation nation : nations) {
            User user = nation.getUser();
            if (user == null) {
                errors.add("No user found for " + nation.getNation());
            } else {
                if (!hasAdmin) {
                    Member member = guild.getMember(user);
                    if (member == null) {
                        errors.add("No member found for " + nation.getNation() + " in guild " + guild);
                        continue;
                    }
                }

                users.add(user);
            }
        }
        if (users.isEmpty()) {
            return "No users found. Are they registered? " + CM.register.cmd.toSlashMention();
        }
        GPTUtil.checkThrowModeration(message);
        CompletableFuture<IMessageBuilder> msgFuture = io.sendMessage("Sending " + users.size() + " with " + errors.size() + " errors\n" + StringMan.join(errors, "\n"));
        for (User mention : users) {
            mention.openPrivateChannel().queue(f -> RateLimitUtil.queue(f.sendMessage(author.getAsMention() + " said: " + message + "\n\n(no reply)")));
        }
        io.sendMessage("Done! Sent " + users.size() + " messages");
        return null;
    }

    @Command(desc = "Remove a discord role the bot uses for command permissions")
    @RolePermission(Roles.ADMIN)
    public String unregisterRole(@Me GuildDB db, Roles locutusRole, @Arg("Only remove a role mapping for this alliance") @Default DBAlliance alliance) {
        return aliasRole(db, locutusRole, null, alliance, true);
    }

    private static String mappingToString(Map<Long, Role> mapping) {
        List<String> response = new ArrayList<>();
        for (Map.Entry<Long, Role> entry : mapping.entrySet()) {
            Role role = entry.getValue();
            long aaId = entry.getKey();
            if (aaId == 0) {
                response.add("*:" + role.getName());
            } else {
                response.add(aaId + ": " + role.getName());
            }
        }
        if (response.isEmpty()) return "";
        return "- " + StringMan.join(response, "\n- ");
    }

    @Command(desc = "Set the discord roles the bot uses for command permissions")
    @RolePermission(Roles.ADMIN)
    public static String aliasRole(@Me GuildDB db, @Default Roles locutusRole, @Default() Role discordRole, @Arg("If the role mapping is only for a specific alliance (WIP)") @Default() DBAlliance alliance, @Arg("Remove the existing mapping instead of setting it") @Switch("r") boolean removeRole) {
        if (alliance != null && !db.isAllianceId(alliance.getAlliance_id())) {
            return "Alliance: " + alliance.getAlliance_id() + " not registered to guild " + db.getGuild() + ". See: " + CM.settings.info.cmd.toSlashMention() + " with key: " + GuildKey.ALLIANCE_ID.name();
        }
        StringBuilder response = new StringBuilder();
        boolean showGlobalMappingInfo = false;

        if (locutusRole == null) {
            if (discordRole != null) {
                List<String> rolesListStr = new ArrayList<>();
                Map<Roles, Map<Long, Long>> allMapping = db.getMappingRaw();
                if (removeRole) {
                    // remove all roles registered to it
                    for (Map.Entry<Roles, Map<Long, Long>> locEntry : allMapping.entrySet()) {
                        for (Map.Entry<Long, Long> discEntry : locEntry.getValue().entrySet()) {
                            long aaId =discEntry.getKey();
                            if (alliance != null && aaId != alliance.getAlliance_id()) continue;
                            if (discEntry.getValue() == discordRole.getIdLong()) {
                                String aaStr =  aaId == 0 ? "*" : DNS.getName(aaId, true);
                                rolesListStr.add("Removed " + locEntry.getKey().name() + " from " + discordRole.getName() + " (AA:" + aaId + ")");
                                db.deleteRole(locEntry.getKey(), aaId);
                            }
                        }
                    }
                    if (rolesListStr.isEmpty()) {
                        return "No aliases found for " + discordRole.getName();
                    }
                    response.append("Removed aliases for " + discordRole.getName() + ":\n- ");
                    response.append(StringMan.join(rolesListStr, "\n- "));
                    response.append("\n\nUse " + CM.role.setAlias.cmd.toSlashMention() + " to view current role aliases");
                    return response.toString();
                }

                for (Map.Entry<Roles, Map<Long, Long>> locEntry : allMapping.entrySet()) {
                    Map<Long, Long> aaToRoleMap = locEntry.getValue();
                    showGlobalMappingInfo |= aaToRoleMap.size() > 1 && aaToRoleMap.containsKey(0L);
                    for (Map.Entry<Long, Long> discEntry : aaToRoleMap.entrySet()) {
                        if (discEntry.getValue() == discordRole.getIdLong()) {
                            Roles role = locEntry.getKey();
                            long aaId = discEntry.getKey();
                            if (aaId == 0) {
                                rolesListStr.add("*:" + role.name());
                            } else {
                                rolesListStr.add(DBAlliance.getOrCreate((int) aaId).getName() + "/" + aaId + ":" + role.name());
                            }
                        }
                    }
                }
                if (rolesListStr.isEmpty()) {
                    return "No aliases found for " + discordRole.getName();
                }
                response.append("Aliases for " + discordRole.getName() + ":\n- ");
                response.append(StringMan.join(rolesListStr, "\n- "));
                if (showGlobalMappingInfo) response.append("\n`note: " + Messages.GLOBAL_ROLE_MAPPING_INFO + "`");
                return response.toString();
            }

            List<String> registeredRoles = new ArrayList<>();
            List<String> unregisteredRoles = new ArrayList<>();
            for (Roles role : Roles.values) {
                Map<Long, Role> mapping = db.getAccountMapping(role);
                if (mapping != null && !mapping.isEmpty()) {
                    registeredRoles.add(role + ":\n" + mappingToString(mapping));
                    continue;
                }
                if (role.getKey() != null && db.getOrNull(role.getKey()) == null) continue;
                unregisteredRoles.add(role + ":\n" + mappingToString(mapping));
            }

            if (!registeredRoles.isEmpty()) {
                response.append("**Registered Roles**:\n" + StringMan.join(registeredRoles, "\n") + "\n");
            }
            if (!unregisteredRoles.isEmpty()) {
                response.append("**Unregistered Roles**:\n" + StringMan.join(unregisteredRoles, "\n") + "\n");
            }
            response.append("Provide a value for `locutusRole` for specific role information.\n" +
                    "Provide a value for `discordRole` to register a role.\n");

            return response.toString();
        }

        if (discordRole == null) {
            if (removeRole) {
                Role alias = db.getRole(locutusRole, alliance != null ? (long) alliance.getAlliance_id() : null);
                if (alias == null) {
                    String allianceStr = alliance != null ? alliance.getName() + "/" + alliance.getAlliance_id() : "*";
                    return "No role alias found for " + allianceStr + ":" + locutusRole.name();
                }
                if (alliance != null) {
                    db.deleteRole(locutusRole, alliance.getAlliance_id());
                } else {
                    db.deleteRole(locutusRole);
                }
                response.append("Removed role alias for " + locutusRole.name() + ":\n");
            }
            Map<Long, Role> mapping = db.getAccountMapping(locutusRole);
            response.append("**" + locutusRole.name() + "**:\n");
            response.append("`" + locutusRole.getDesc() + "`\n");
            if (mapping.isEmpty()) {
                response.append("No value set.");
            } else {
                response.append("```\n" + mappingToString(mapping) + "```\n");
            }
            response.append("Provide a value for `discordRole` to register a role.\n");
            if (mapping.size() > 1 && mapping.containsKey(0L)) {
                response.append("`note: " + Messages.GLOBAL_ROLE_MAPPING_INFO + "`");
            }
            return response.toString().trim();
        }

        if (removeRole) {
            throw new IllegalArgumentException("Cannot remove role alias with this command. Use " + CM.role.unregister.cmd.locutusRole(locutusRole.name()).toSlashCommand() +"");
        }


        int aaId = alliance == null ? 0 : alliance.getAlliance_id();
        String allianceStr = alliance == null ? "*" : alliance.getName() + "/" + aaId;
        db.addRole(locutusRole, discordRole, aaId);
        return "Added role alias: " + locutusRole.name().toLowerCase() + " to " + discordRole.getName() + " for alliance " + allianceStr + "\n" +
                "To unregister, use " + CM.role.unregister.cmd.locutusRole(locutusRole.name()).toSlashCommand() + "";
    }

    @Command(desc = "Import api keys from the guild API_KEY setting, so they can be validated")
    @Ephemeral
    @RolePermission(value = Roles.ADMIN, root = true)
    public String importGuildKeys() {
        StringBuilder response = new StringBuilder();
        for (GuildDB db : Locutus.imp().getGuildDatabases().values()) {
            List<ApiKeyPool.ApiKey> keys = db.getOrNull(GuildKey.API_KEY);
            if (keys == null) return "No keys found for guild " + db.getGuild().getName() + " (" + db.getGuild().getId() + ")";
            for (ApiKeyPool.ApiKey key : keys) {
                try {
                    DnsQuery<Nation> query = new DnsApi(ApiKeyPool.builder().addKeyUnsafe(key.getKey()).build()).nation(Settings.INSTANCE.NATION_ID);
                    query.call();
                    Locutus.imp().getDiscordDB().addApiKey(key.getNation().getId(), key.getKey());
                    response.append(key + ": success" + "\n");
                } catch (Throwable e) {
                    response.append(key + ": " + StringMan.stripApiKey(e.getMessage()) + "\n");
                }
            }
        }
        Logg.text(response.toString());
        return "Done! (see console)";
    }

//    @Command(desc = "Check if current api keys are valid")
//    @RolePermission(value = Roles.ADMIN, root = true)
//    public String validateAPIKeys() {
//        // Validate v3 keys used in the guild db?
//        return "TODO";
//        Set<String> keys = Locutus.imp().getPnwApiV2().getApiKeyUsageStats().keySet();
//        Map<String, String> failed = new LinkedHashMap<>();
//        Map<String, ApiKeyDetails> success = new LinkedHashMap<>();
//        for (String key : keys) {
//            try {
//                ApiKeyDetails stats = new DnsApi(ApiKeyPool.builder().addKeyUnsafe(key).build()).getApiKeyStats();
//                if (stats != null && stats.getNation() != null && stats.getNation().getId() != null) {
//                    success.put(key, stats);
//                } else {
//                    failed.put(key, "Error: null (1)");
//                }
//            } catch (Throwable e) {
//                failed.put(key, e.getMessage());
//            }
//        }
//        StringBuilder response = new StringBuilder();
//        for (Map.Entry<String, String> e : failed.entrySet()) {
//            response.append(e.getKey() + ": " + e.getValue() + "\n");
//        }
//        for (Map.Entry<String, ApiKeyDetails> e : success.entrySet()) {
//            String key = e.getKey();
//            ApiKeyDetails record = e.getValue();
//            int natId = record.getNation().getId();
//            DBNation nation = DBNation.getById(natId);
//            if (nation != null) {
//                response.append(key + ": " + record.toString() + " | " + nation.getNation() + " | " + nation.getAllianceName() + " | " + nation.getPosition() + "\n");
//            } else {
//                response.append(e.getKey() + ": " + e.getValue() + "\n");
//            }
//        }
//        System.out.println(response); // keep
//        return "Done (see console)";
//    }

    @Command(desc = "Purge a category's channels older than the time specified")
    @RolePermission(value = Roles.ADMIN)
    public String debugPurgeChannels(Category category, @Range(min=60) @Timestamp long cutoff) {
        int deleted = 0;
        for (GuildMessageChannel GuildMessageChannel : category.getTextChannels()) {
            if (GuildMessageChannel.getLatestMessageIdLong() > 0) {
                long message = GuildMessageChannel.getLatestMessageIdLong();
                try {
                    long created = net.dv8tion.jda.api.utils.TimeUtil.getTimeCreated(message).toEpochSecond() * 1000L;
                    if (created > cutoff) {
                        continue;
                    }
                } catch (Throwable ignore) {}
            }
            RateLimitUtil.queue(GuildMessageChannel.delete());
            deleted++;
            continue;
        }
        return "Deleted " + deleted + " channels";
    }

    @Command
    @Ephemeral
    @RolePermission(value = Roles.ADMIN, root = true)
    public String listExpiredGuilds(boolean checkMessages) {
        StringBuilder response = new StringBuilder();
        for (GuildDB db : Locutus.imp().getGuildDatabases().values()) {
            Guild guild = db.getGuild();
            Member owner = db.getGuild().getOwner();
            DBNation nation = DiscordUtil.getNation(owner.getUser());

            Set<Integer> aaIds = db.getAllianceIds();

            if (nation != null && nation.active_m() > 30000) {
                response.append(guild + "/" + StringMan.getString(aaIds) + ": owner (nation:" + nation.getNation_id() + ") is inactive " + TimeUtil.secToTime(TimeUnit.MINUTES, nation.active_m()) + "\n");
                continue;
            }
            // In an alliance with inactive leadership (1 month)
            if (!aaIds.isEmpty() && !db.isValidAlliance()) {
                response.append(guild + "/" + StringMan.getString(aaIds) + ": alliance is invalid (nation:" + (nation == null ? "" : nation.getNation_id() + ")\n"));
                continue;
            }

            if (aaIds.isEmpty() && nation == null && checkMessages) {
                long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);
                boolean error = false;
                long last = 0;

                outer:
                for (GuildMessageChannel channel : guild.getTextChannels()) {
                    if (channel.getLatestMessageIdLong() == 0) continue;
                    try {
                        long latestSnowflake = channel.getLatestMessageIdLong();
                        long latestMs = net.dv8tion.jda.api.utils.TimeUtil.getTimeCreated(latestSnowflake).toEpochSecond() * 1000L;
                        if (latestMs > cutoff) {
                            List<Message> messages = RateLimitUtil.complete(channel.getHistory().retrievePast(5));
                            for (Message message : messages) {
                                if (message.getAuthor().isSystem() || message.getAuthor().isBot() || guild.getMember(message.getAuthor()) == null) {
                                    continue;
                                }
                                last = Math.max(last, message.getTimeCreated().toEpochSecond() * 1000L);
                                if (last > cutoff) {
                                    break outer;
                                }
                            }
                        }
                    } catch (Throwable e) {
                        error = true;
                    }
                }
                if (last < cutoff) {
                    response.append(guild + ": has no recent messages\n");
                    continue;
                }
            }
        }
        return response.toString();
    }

    @Command
    @RolePermission(value = Roles.ADMIN, root = true)
    public String leaveServer(long guildId) {
        GuildDB db = Locutus.imp().getGuildDB(guildId);
        if (db == null) return "Server not found " + guildId;
        Guild guild = db.getGuild();
        RateLimitUtil.queue(guild.leave());
        return "Leaving " + guild.getName();
    }

    @Command()
    @RolePermission(value = Roles.ADMIN, root = true)
    public String listGuildOwners() {
        ArrayList<GuildDB> guilds = new ArrayList<>(Locutus.imp().getGuildDatabases().values());
        guilds.sort(new Comparator<GuildDB>() {
            @Override
            public int compare(GuildDB o1, GuildDB o2) {
                return Long.compare(o1.getGuild().getIdLong(), o2.getGuild().getIdLong());
            }
        });
        StringBuilder result = new StringBuilder();
        for (GuildDB value : guilds) {
            Guild guild = value.getGuild();
            User owner = Locutus.imp().getDiscordApi().getUserById(guild.getOwnerIdLong());
            result.append(guild.getIdLong() + " | " + guild.getName() + " | " + owner.getName()).append("\n");
        }
        return result.toString();
    }

    @Command()
    @RolePermission(value = Roles.ADMIN, root = true)
    public String syncMetrics(@Default("80") int topX) throws IOException, ParseException {
        AllianceMetric.update(topX);
        return "Updated metrics for top " + topX + " alliances";
    }

    @Command()
    @RolePermission(value = Roles.ADMIN, root = true)
    public String syncNations(NationDB db) throws IOException, ParseException {
        List<Event> events = new ArrayList<>();
        db.updateAllNations(events::add);
        if (events.size() > 0) {
            Locutus.imp().getExecutor().submit(() -> {
                for (Event event : events) event.post();;
            });
        }
        return "Updated ALL nations. " + events.size() + " changes detected";
    }

    @Command()
    @RolePermission(value = Roles.ADMIN, root = true)
    public String syncBanks(@Me GuildDB db, @Me IMessageIO channel, DBAlliance alliance) throws IOException, ParseException {
        BankDB bankDb = Locutus.imp().getBankDB();
        bankDb.updateBankTransfers(alliance);
        bankDb.updateEquipmentTransfers(alliance);
        bankDb.updateGrantTransfers(alliance);
        bankDb.updateLoanTransfers(alliance);
        return "Done!";
    }

    @Command(desc = "List users in the guild that have provided login credentials to locutus")
    @Ephemeral
    @RolePermission(value = Roles.ADMIN, root = true)
    public String listAuthenticated(@Me GuildDB db) {
        List<Member> members = db.getGuild().getMembers();

        Map<DBNation, Rank> registered = new LinkedHashMap<>();
        Map<DBNation, String> errors = new HashMap<>();

        Set<Integer> alliances = db.getAllianceIds(false);
        for (Member member : members) {
            DBNation nation = DiscordUtil.getNation(member.getUser());
            if (nation != null && (alliances.isEmpty() || alliances.contains(nation.getAlliance_id()))) {
                try {
                    Auth auth = nation.getAuth(true);
                    registered.put(nation, nation.getPositionEnum());
                } catch (IllegalArgumentException ignore) {}
            }
        }

        if (registered.isEmpty()) {
            return "No registered users";
        }
        StringBuilder result = new StringBuilder();
        for (Map.Entry<DBNation, Rank> entry : registered.entrySet()) {
            result.append(entry.getKey().getNation() + "- " + entry.getValue());
            String error = errors.get(entry.getValue());
            if (error != null) {
                result.append(": Could not validate: " + error);
            }
            result.append("\n");
        }
        return result.toString().trim();
    }

    @Command(desc = "Force a fetch and update of war rooms for each guild")
    @RolePermission(value = Roles.MILCOM)
    public String purgeWarRooms( // war room delete_all
            @Me GuildDB db,
            @Me IMessageIO io,
            @Me User user,
            @Arg("Only delete a single channel") @Switch("c") MessageChannel channel) throws IOException {

        WarCategory warCat = db.getWarChannel(true);
        if (channel == null) {
            channel = io instanceof DiscordChannelIO ? ((DiscordChannelIO) io).getChannel() : null;
        }
        if (channel != null) {
            Guild chanGuild = ((GuildMessageChannel) channel).getGuild();
            if (!Roles.MILCOM.has(user, chanGuild)) {
                throw new IllegalArgumentException("Missing " + Roles.MILCOM.toDiscordRoleNameElseInstructions(chanGuild));
            }
        }
        WarCategory.WarRoom room = channel instanceof GuildMessageChannel mC ? WarCategory.getGlobalWarRoom(mC) : null;
        if (channel != null && room == null) {
            throw new IllegalArgumentException("Channel is not a war room");
        }
        if (room != null) {
            room.delete("Deleted by " + DiscordUtil.getFullUsername(user));
            return "Deleted " + channel.getName();
        } else {
            Set<Category> categories = new HashSet<>();
            Iterator<Map.Entry<Integer, WarCategory.WarRoom>> iter = warCat.getWarRoomMap().entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<Integer, WarCategory.WarRoom> entry = iter.next();
                TextChannel guildChan = entry.getValue().getChannel(false);
                if (guildChan != null) {
                    Category category = guildChan.getParentCategory();
                    if (category != null) categories.add(category);
                    RateLimitUtil.queue(guildChan.delete());
                }
                iter.remove();
            }
            for (Category category : categories) {
                if (category.getName().startsWith("warcat-")) {
                    RateLimitUtil.queue(category.delete());
                }
            }
            return "Deleted war rooms! See also: " + CM.admin.sync.warrooms.cmd.toSlashMention();
        }
    }
//    SyncTreaties
    @Command(desc = "Force a fetch and update of treaties from the api")
    @RolePermission(value = Roles.ADMIN, root = true)
    public String syncTreaties() throws IOException {
        Locutus.imp().getNationDB().updateActiveTreaties(Event::post);
        return "Updated treaties!";
    }

    @Command(desc = "View info about a guild with a given id")
    @RolePermission(value = Roles.ADMIN, root = true)
    public String guildInfo(Guild guild) {
        return guild.getName() + "/" + guild.getIdLong() + "\n" +
                "Owner: " + guild.getOwner() + "\n" +
                "Members: " + StringMan.getString(guild.getMembers());
    }

    @Command(desc = "View meta information about a nation in the bot's database")
    @RolePermission(value = Roles.ADMIN, root = true)
    public String nationMeta(DBNation nation, NationMeta meta) {
        ByteBuffer buf = nation.getMeta(meta);
        if (buf == null) return "No value set.";

        byte[] arr = new byte[buf.remaining()];
        buf.get(arr);
        buf = ByteBuffer.wrap(arr);

        switch (arr.length) {
            case 0 -> {
                return "" + (buf.get() & 0xFF);
            }
            case 4 -> {
                return "" + (buf.getInt());
            }
            case 8 -> {
                ByteBuffer buf2 = ByteBuffer.wrap(arr);
                return buf.getLong() + "/" + MathMan.format(buf2.getDouble());
            }
            default -> {
                return new String(arr, StandardCharsets.ISO_8859_1);
            }
        }
    }

    @NoFormat
    @Command(desc = "Run a command as another user")
    @RolePermission(value = Roles.ADMIN, root = true)
    public String sudo(@Me Guild guild, @Me IMessageIO io, String command,
                       @Switch("u") User user,
                       @Switch("n") DBNation nation) {
        if (user == null && nation == null) {
            throw new IllegalArgumentException("Specify a user or nation");
        }
        if (user != null && nation != null) {
            throw new IllegalArgumentException("Specify only a user or nation");
        }
        CommandManager2 v2 = Locutus.cmd().getV2();
        if (user != null) {
            v2.run(guild, io, user, command, false, true);
        } else {
            MessageChannel channel = io instanceof DiscordChannelIO dio ? dio.getChannel() : null;
            Message message = io instanceof DiscordChannelIO dio ? dio.getUserMessage() : null;
            LocalValueStore locals = v2.createLocals(null, guild, channel, null, message, io, null);
            locals.addProvider(Key.of(DBNation.class, Me.class), nation);
            v2.run(locals, io, command, false, true);
        }
        return "Done!";
    }

    @NoFormat
    @Command(desc = "Run multiple commands")
    @RolePermission(value = Roles.ADMIN, root = true)
    public String runMultiple(@Me Guild guild, @Me IMessageIO io, @Me User user, @TextArea String commands) {
        commands = commands.replace("\\n", "\n");
        String[] split = commands.split("\\r?\\n" + "[" + Settings.commandPrefix() + "|" + "/]");

        for (String cmd : split) {
            Locutus.cmd().getV2().run(guild, io, user, cmd, false, true);
        }
        return "Done!";
    }

    @NoFormat
    @Command(desc = "Format a command for each nation, and run it as yourself")
    @RolePermission(value = Roles.ADMIN)
    public String runForNations(@Me GuildDB db, @Me User user, @Me DBNation me, @Me IMessageIO io, NationPlaceholders placeholders, ValueStore store,
                              Set<DBNation> nations, String command) {
        if (!db.hasAlliance()) {
            throw new IllegalArgumentException("No alliance registered to this guild. " + CM.settings_default.registerAlliance.cmd.toSlashMention());
        }
        for (DBNation nation : nations) {
            if (!db.isAllianceId(nation.getAlliance_id())) {
                throw new IllegalArgumentException("Nation " + nation.getMarkdownUrl() + " is not in the alliance/s " + db.getAllianceIds());
            }
        }
        if (nations.size() > 300) {
            throw new IllegalArgumentException("Too many nations to update (max: 300, provided: " + nations.size() + ")");
        }

        PlaceholderCache<DBNation> cache = new PlaceholderCache<>(nations);
        Function<DBNation, String> formatFunc = placeholders.getFormatFunction(store, command, cache, true);
        StringMessageBuilder condensed = new StringMessageBuilder(db.getGuild());
        Runnable sendTask = () -> {
            if (!condensed.isEmpty()) {
                IMessageBuilder msg = io.create();
                condensed.flatten();
                condensed.writeTo(msg);
                msg.send();
                condensed.clear();
            }
        };

        long start = System.currentTimeMillis();
        for (DBNation nation : nations) {
            String formattedCmd = formatFunc.apply(nation);
            try {
                Map.Entry<CommandResult, List<StringMessageBuilder>> response = me.runCommandInternally(db.getGuild(), user, formattedCmd);
                condensed.append("# " + nation.getMarkdownUrl() + ": " + response.getKey() + "\n");
                for (StringMessageBuilder msg : response.getValue()) {
                    msg.writeTo(condensed);
                }
            } catch (Throwable e) {
                condensed.append("# " + nation.getMarkdownUrl() + ": " + StringMan.stripApiKey(e.getMessage()) + "\n");
            }
            if (-start + (start = System.currentTimeMillis()) > 5000) {
                sendTask.run();
            }
        }
        sendTask.run();
        return "Done!";
    }

    @NoFormat
    @Command(desc = "Run a command as multiple nations")
    @RolePermission(value = Roles.ADMIN, root = true)
    public String sudoNations(@Me GuildDB db, @Me IMessageIO io, NationPlaceholders placeholders, ValueStore store,
                              Set<DBNation> nations, String command) {
        PlaceholderCache<DBNation> cache = new PlaceholderCache<>(nations);
        Function<DBNation, String> formatFunc = placeholders.getFormatFunction(store, command, cache, true);

        StringMessageBuilder condensed = new StringMessageBuilder(db.getGuild());
        Runnable sendTask = () -> {
            if (!condensed.isEmpty()) {
                IMessageBuilder msg = io.create();
                condensed.flatten();
                condensed.writeTo(msg);
                msg.send();
                condensed.clear();
            }
        };

        long start = System.currentTimeMillis();
        for (DBNation nation : nations) {
            String formattedCmd = formatFunc.apply(nation);
            User nationUser = nation.getUser();
            try {
                Map.Entry<CommandResult, List<StringMessageBuilder>> response = nation.runCommandInternally(db.getGuild(), nationUser, formattedCmd);
                condensed.append("# " + nation.getMarkdownUrl() + ": " + response.getKey() + "\n");
                for (StringMessageBuilder msg : response.getValue()) {
                    msg.writeTo(condensed);
                }
            } catch (Throwable e) {
                condensed.append("# " + nation.getMarkdownUrl() + ": " + StringMan.stripApiKey(e.getMessage()) + "\n");
            }
            if (-start + (start = System.currentTimeMillis()) > 5000) {
                sendTask.run();
            }

        }
        sendTask.run();
        return "Done!";
    }


}
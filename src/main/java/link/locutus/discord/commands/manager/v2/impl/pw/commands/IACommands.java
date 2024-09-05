package link.locutus.discord.commands.manager.v2.impl.pw.commands;

import link.locutus.discord.Locutus;
import link.locutus.discord.api.ApiKeyPool;
import link.locutus.discord.api.endpoints.DnsApi;
import link.locutus.discord.api.types.tx.BankTransfer;
import link.locutus.discord.api.types.tx.Transaction2;
import link.locutus.discord.commands.manager.v2.binding.ValueStore;
import link.locutus.discord.commands.manager.v2.binding.annotation.*;
import link.locutus.discord.commands.manager.v2.binding.annotation.Timestamp;
import link.locutus.discord.commands.manager.v2.binding.bindings.PlaceholderCache;
import link.locutus.discord.commands.manager.v2.command.IMessageBuilder;
import link.locutus.discord.commands.manager.v2.command.IMessageIO;
import link.locutus.discord.commands.manager.v2.command.StringMessageBuilder;
import link.locutus.discord.commands.manager.v2.impl.discord.DiscordChannelIO;
import link.locutus.discord.commands.manager.v2.impl.discord.binding.DiscordBindings;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.HasApi;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.IsAlliance;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.RolePermission;
import link.locutus.discord.commands.manager.v2.impl.pw.NationFilter;
import link.locutus.discord.commands.manager.v2.impl.pw.binding.PWBindings;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.commands.manager.v2.impl.pw.filter.NationPlaceholders;
import link.locutus.discord.commands.manager.v2.builder.SummedMapRankBuilder;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.*;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.guild.GuildKey;
import link.locutus.discord.db.guild.SheetKey;
import link.locutus.discord.gpt.GPTUtil;
import link.locutus.discord.pnw.AllianceList;
import link.locutus.discord.pnw.NationList;
import link.locutus.discord.pnw.RegisteredUser;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.MarkupUtil;
import link.locutus.discord.util.MathMan;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.RateLimitUtil;
import link.locutus.discord.util.StringMan;
import link.locutus.discord.util.TimeUtil;
import link.locutus.discord.util.discord.DiscordUtil;
import link.locutus.discord.util.offshore.Auth;
import link.locutus.discord.util.offshore.test.IACategory;
import link.locutus.discord.util.offshore.test.IAChannel;
import link.locutus.discord.util.sheet.SpreadSheet;
import link.locutus.discord.util.task.ia.IACheckup;
import link.locutus.discord.web.CommandResult;
import com.google.gson.JsonObject;
import link.locutus.discord.api.types.Rank;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel;
import net.dv8tion.jda.api.entities.channel.attribute.IMemberContainer;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class IACommands {
    @Command(desc = "Rename channels and set their topic (if empty) in a category to match the nation registered to the user added")
    @RolePermission(Roles.INTERNAL_AFFAIRS_STAFF)
    public String renameInterviewChannels(@Me GuildDB db, @Me Guild guild, @Me IMessageIO io, @Me JSONObject command,
                                          Set<Category> categories,
                                          @Switch("m") boolean allow_non_members,
                                          @Switch("v") boolean allow_vm,
                                          @Switch("n") Set<DBNation> list_missing,
                                          @Switch("f") boolean force) {
        for (Category category : categories) {
            if (!category.getGuild().equals(guild)) throw new IllegalArgumentException("Category " + category.getName() + " is not in this guild");
        }
        Map<TextChannel, String> errors = new LinkedHashMap<>();
        Map<TextChannel, String> warnings = new LinkedHashMap<>();
        Map<DBNation, Set<TextChannel>> nationChannels = new HashMap<>();
        Map<User, Set<TextChannel>> userChannels = new HashMap<>();
        List<String> changes = new ArrayList<>();
        for (TextChannel channel : categories.stream().flatMap(f -> f.getTextChannels().stream()).collect(Collectors.toSet())) {
            List<PermissionOverride> overrides = channel.getMemberPermissionOverrides();
            List<Member> members = overrides.stream().map(PermissionOverride::getMember).filter(Objects::nonNull).filter(f -> !f.getUser().isBot()).toList();
            if (members.isEmpty()) {
                errors.put(channel, "No users added to channel");
                continue;
            }
            if (members.size() > 1) {
                errors.put(channel, "Multiple users added to channel");
                continue;
            }
            Member member = members.get(0);
            userChannels.computeIfAbsent(member.getUser(), f -> new HashSet<>()).add(channel);
            if (!channel.canTalk(member)) {
                errors.put(channel, "User does not have permission to talk in channel: `" + DiscordUtil.getFullUsername(member.getUser()) + "` | `" + member.getId() + "`");
                continue;
            }
            DBNation nation = DiscordUtil.getNation(member.getIdLong());
            if (nation == null) {
                errors.put(channel, "User is not registered to a nation: `" + DiscordUtil.getFullUsername(member.getUser()) + "` | `" + member.getId() + "`");
                continue;
            }
            nationChannels.computeIfAbsent(nation, f -> new HashSet<>()).add(channel);
            if (!allow_non_members && !db.isAllianceId(nation.getAlliance_id())) {
                warnings.put(channel, "Nation is not in the alliance: " + nation.getMarkdownUrl() + " (ignore with `allow_non_members: True`");
            } else if (!allow_vm && nation.isVacation()) {
                warnings.put(channel, "Nation is a VM: " + nation.getMarkdownUrl() + " (ignore with `allow_vm: True`");
            }
            String topic = channel.getTopic();
            if (topic == null || channel.getTopic().isEmpty()) {
                String newTopic = nation.getUrl();
                RateLimitUtil.queue(channel.getManager().setTopic(newTopic));
            }
            String newName = nation.getName() + "-" + nation.getId();
            if (channel.getName().replace(" ", "-").equalsIgnoreCase(newName.replace(" ", "-"))) continue;
            changes.add(channel.getAsMention() + ": " + channel.getName() + " -> " + nation.getName() + "-" + nation.getId());
            if (force) {
                try {
                    RateLimitUtil.queue(channel.getManager().setName(newName));
                } catch (PermissionException e) {
                    errors.put(channel, "Error renaming channel: " + e.getMessage());
                }
            }
        }
        // add duplicates to errors
        nationChannels.entrySet().stream().filter(f -> f.getValue().size() > 1).forEach(f -> errors.putIfAbsent(f.getValue().iterator().next(), "Nation has multiple channels"));
        userChannels.entrySet().stream().filter(f -> f.getValue().size() > 1).forEach(f -> errors.putIfAbsent(f.getValue().iterator().next(), "User has multiple channels"));

        StringBuilder errorsStr = new StringBuilder();
        if (!errors.isEmpty()) {
            errorsStr.append("channnel_name\tchannel_id\terror\n");
            for (Map.Entry<TextChannel, String> entry : errors.entrySet()) {
                TextChannel channel = entry.getKey();
                errorsStr.append(channel.getName() + "\t" + channel.getId() + "\t" + entry.getValue() + "\n");
            }
        }
        StringBuilder warningsStr = new StringBuilder();
        if (!warnings.isEmpty()) {
            warningsStr.append("channnel_name\tchannel_id\twarning\n");
            for (Map.Entry<TextChannel, String> entry : warnings.entrySet()) {
                TextChannel channel = entry.getKey();
                warningsStr.append(channel.getName() + "\t" + channel.getId() + "\t" + entry.getValue() + "\n");
            }
        }
        IMessageBuilder msg = io.create();
        StringBuilder append = new StringBuilder();
        if (!errors.isEmpty()) {
            append.append("Errors, see attached `errors.txt` for details");
            msg.file("errors.txt", errorsStr.toString());
        }
        if (!warnings.isEmpty()) {
            append.append("Warnings, see attached `warnings.txt` for details");
            msg.file("warnings.txt", warningsStr.toString());
        }

        if (list_missing != null) {
            Set<DBNation> missing = new LinkedHashSet<>();
            for (DBNation nation : list_missing) {
                if (!nationChannels.containsKey(nation)) {
                    missing.add(nation);
                }
            }
            if (!missing.isEmpty()) {
                List<String> missingRows = new ArrayList<>();
                missingRows.add("nation_id\tnation_name\tdiscord\turl");
                for (DBNation nation : missing) {
                    String userName = nation.getUserDiscriminator();
                    missingRows.add(nation.getId() + "\t" + nation.getName() + "\t" + userName + "\t" + nation.getUrl());
                }
                msg.file("missing.txt", StringMan.join(missingRows, "\n"));
            }
        }

        if (changes.isEmpty()) {
            append.append("No changes to be made");
            msg.append(append.toString()).send();
            return null;
        }
        if (!force) {
            append.append(changes.size() + " changes.");
            msg.confirmation("Confirm rename channels", append.toString() + "\n" +
                    StringMan.join(changes, "\n"), command).send();
        } else {
            msg.append(append.toString() + "\n" + StringMan.join(changes, "\n")).send();
        }
        return null;
    }

    @Command(desc = "Sort the channels in a set of categories into new categories based on the category name\n" +
            "The channels must be in the form `{nation}-{nation_id}`\n" +
            "Receiving categories must resolve to a nation filter: <https://github.com/xdnw/diplocutus/wiki/Nation_placeholders>\n" +
            "Set a category prefix to only send channels to categories starting with a specific word (e.g. `interview-`)\n" +
            "Set a filter to only sort channels for those nations")
    @RolePermission(Roles.INTERNAL_AFFAIRS_STAFF)
    public String sortChannelsName(@Me GuildDB db, @Me Guild guild, @Me User author, @Me DBNation me, @Me IMessageIO io, @Me JSONObject command,
                                   @Arg("Categories to sort from")
                                   Set<Category> from,
                                   @Arg("Category prefix to sort channels into")
                                   String categoryPrefix,
                                   @Arg("Optional filter to only sort channels for those nations")
                                   @Default NationFilter filter,
                                   @Switch("w") boolean warn_on_filter_fail,
                                   @Switch("f") boolean force) {
        for (Category category : from) {
            if (!category.getGuild().equals(guild)) throw new IllegalArgumentException("Category " + category.getName() + " is not in this guild");
        }
        List<String> errors = new ArrayList<>();
        Map<Category, NationFilter> filters = new HashMap<>();

        String prefixLower = categoryPrefix.toLowerCase(Locale.ROOT);
        for (Category category : guild.getCategories()) {
            String name = category.getName();
            if (name.toLowerCase(Locale.ROOT).startsWith(prefixLower)) {
                name = name.substring(prefixLower.length());
                if (name.startsWith("-")) name = name.substring(1);
                try {
                    NationFilter nationFilter = new NationFilterString(name, guild, author, me);
                    filters.put(category, nationFilter);
                } catch (IllegalArgumentException e) {
                    errors.add("[" + category.getName() + "] Error Resolving filter " + e.getMessage());
                    continue;
                }
            } else {
                continue;
            }
        }

        if (filters.isEmpty()) {
            String msg = "No categories found with prefix: `" + categoryPrefix + "` that resolve to a valid category.";
            if (!errors.isEmpty()) {
                msg += "\nCategory Name Errors:\n" + StringMan.join(errors, "\n");
            }
            throw new IllegalArgumentException(msg);
        }

        Function<DBNation, Set<Category>> catFunc = nation -> {
            Set<Category> categories = new HashSet<>();
            for (Map.Entry<Category, NationFilter> entry : filters.entrySet()) {
                if (entry.getValue().test(nation)) {
                    categories.add(entry.getKey());
                }
            }
            return categories.isEmpty() ? null : categories;
        };
        IMessageBuilder msg = sortChannels(db, guild, io, command, from, catFunc, filter, warn_on_filter_fail, force);
        if (!errors.isEmpty()) {
            msg = msg.append("\nCategory Name Errors:\n" + StringMan.join(errors, "\n"));
        }
        msg.send();
        return null;
    }

    @Command(desc = "Sort the channels in a set of categories into new categories based on a spreadsheet\n" +
            "The channels must be in the form `{nation}-{nation_id}`\n" +
            "The sheet must have columns: (`nation` or `leader`) and `category`\n" +
            "Set a filter to only sort channels for those nations")
    @RolePermission(Roles.INTERNAL_AFFAIRS)
    public String sortChannelsSheet(@Me GuildDB db, @Me Guild guild, @Me IMessageIO io, @Me JSONObject command,
                                    @Arg("Categories to sort from")
                                    Set<Category> from,
                                    SpreadSheet sheet,
                                    @Default NationFilter filter,
                                    @Switch("w") boolean warn_on_filter_fail, @Switch("f") boolean force) {
        sheet.loadValues(null, true);
        List<Object> nations = sheet.findColumn("nation");
        List<Object> leaders = sheet.findColumn("leader");
        List<Object> categories = sheet.findColumn("category");
        if (nations == null && leaders == null) {
            throw new IllegalArgumentException("Sheet must have columns: (`nation` or `leader`)");
        }
        if (categories == null) {
            throw new IllegalArgumentException("Sheet must have column: `category`");
        }
        List<String> errors = new ArrayList<>();
        Map<DBNation, Category> categoryMap = new HashMap<>();
        int max = Math.max(nations == null ? 0 : nations.size(), leaders == null ? 0 : leaders.size());
        for (int i = 0; i < max; i++) {
            Object nationObj = nations == null || nations.size() < i ? null : nations.get(i);
            String nationStr = nationObj == null ? null : nationObj.toString();

            Object leaderObj = leaders == null || leaders.size() < i ? null : leaders.get(i);
            String leaderStr = leaderObj == null ? null : leaderObj.toString();

            DBNation nation = null;
            Category category = null;
            try {
                if (nationStr != null && !nationStr.isEmpty()) {
                    nation = PWBindings.nation(null, nationStr);
                } else if (leaderStr != null && !leaderStr.isEmpty()) {
                    nation = Locutus.imp().getNationDB().getNationByLeader(leaderStr);
                    if (nation == null) {
                        errors.add("[Row:" + (i + 2) + "] Nation Leader not found: `" + leaderStr + "`");
                    }
                }
                Object categoryObj = categories.size() < i ? null : categories.get(i);
                String categoryStr = categoryObj == null ? null : categoryObj.toString();
                if (categoryStr != null && !categoryStr.isEmpty()) {
                    category = DiscordBindings.category(guild, categoryStr, null);
                }
            } catch (IllegalArgumentException e) {
                errors.add("[Row:" + (i + 2) + "] " + e.getMessage());
            }
            if (nation != null && category != null) {
                categoryMap.put(nation, category);
            }
        }
        Function<DBNation, Set<Category>> catFunc = nation -> {
            Category category = categoryMap.get(nation);
            if (category == null) {
                return null;
            }
            return Set.of(category);
        };
        IMessageBuilder msg = sortChannels(db, guild, io, command, from, catFunc, filter, warn_on_filter_fail, force);
        if (!errors.isEmpty()) {
            msg = msg.append("\nSheet Errors:\n" + StringMan.join(errors, "\n"));
        }
        msg.send();
        return null;
    }

    public static IMessageBuilder sortChannels(@Me GuildDB db, @Me Guild guild, @Me IMessageIO io, @Me JSONObject command, Set<Category> from, Function<DBNation, Set<Category>> newCategory, @Default NationFilter filter, @Switch("w") boolean warn_on_filter_fail, @Switch("f") boolean force) {
        Set<TextChannel> channels = new LinkedHashSet<>();
        for (Category category : from) {
            channels.addAll(category.getTextChannels());
        }

        Map<TextChannel, String> errors = new LinkedHashMap<>();
        Map<TextChannel, Set<String>> warnings = new LinkedHashMap<>();
        List<String> changes = new ArrayList<>();
        Map<DBNation, Set<TextChannel>> nationChannels = new HashMap<>();

        for (TextChannel channel : channels) {
            String[] split = channel.getName().split("-");
            if (split.length < 2) {
                errors.put(channel, "Invalid channel name: `" + channel.getName() + "` (must be in the form `{nation}-{nation_id}`)");
                continue;
            }
            String idStr = split[split.length - 1];
            if (!MathMan.isInteger(idStr)) {
                errors.put(channel, "Invalid nation id: `" + channel.getName() + "` (must be in the form `{nation}-{nation_id}`)");
                continue;
            }
            long id = Long.parseLong(idStr);
            DBNation nation = DBNation.getById((int) id);
            if (nation == null) {
                errors.put(channel, "Nation not found: `" + id + "`");
                continue;
            }
            nationChannels.computeIfAbsent(nation, f -> new HashSet<>()).add(channel);
            User user = nation.getUser();
            if (user == null) {
                warnings.computeIfAbsent(channel, f -> new HashSet<>()).add("Nation " + nation.getMarkdownUrl() + " is not registered with bot: " + CM.register.cmd.toSlashMention());
            } else {
                Member member = guild.getMember(user);
                if (member == null) {
                    warnings.computeIfAbsent(channel, f -> new HashSet<>()).add("User is not in server: `" + DiscordUtil.getFullUsername(user) + "` | `" + user.getId() + "`");
                } else if (!channel.canTalk(member)) {
                    warnings.computeIfAbsent(channel, f -> new HashSet<>()).add("User does not have permission to talk in channel: `" + DiscordUtil.getFullUsername(user) + "` | `" + user.getId() + "`");
                } else if (!db.hasAlliance()) {
                    Role memberRole = Roles.MEMBER.toRole(db);
                    if (memberRole != null) {
                        if (!member.getRoles().contains(memberRole)) {
                            warnings.computeIfAbsent(channel, f -> new HashSet<>()).add("User is not a member: `" + DiscordUtil.getFullUsername(user) + "` | `" + user.getId() + "`");
                        }
                    }
                }
            }
            if (nation.active_m() > 7200) {
                warnings.computeIfAbsent(channel, f -> new HashSet<>()).add("Nation " + nation.getMarkdownUrl() + " is inactive: " + TimeUtil.secToTime(TimeUnit.MINUTES, nation.active_m()));
            }
            if (db.hasAlliance()) {
                if (!db.isAllianceId(nation.getAlliance_id())) {
                    warnings.computeIfAbsent(channel, f -> new HashSet<>()).add("Nation " + nation.getMarkdownUrl() + " is not a member of this alliance: " + db.getAllianceIds());
                }
            }
            if (filter != null && !filter.test(nation)) {
                if (warn_on_filter_fail) {
                    warnings.computeIfAbsent(channel, f -> new HashSet<>()).add("Nation " + nation.getMarkdownUrl() + " does not match filter: `" + filter.getFilter() + "`");
                }
                continue;
            }

            Set<Category> allowed = newCategory.apply(nation);
            if (allowed == null || allowed.contains(channel.getParentCategory()) || allowed.isEmpty()) continue;
            Category nonMax = null;
            for (Category cat : allowed) {
                if (cat.getChannels().size() > 50) {
                    continue;
                }
                nonMax = cat;
                break;
            }
            if (nonMax == null) {
                errors.put(channel, "Category channel cap reached for " + allowed);
                continue;
            } else {
                Category currCategory = channel.getParentCategory();
                if (force) {
                    try {
                        RateLimitUtil.queue(channel.getManager().setParent(nonMax));
                    } catch (PermissionException e) {
                        errors.put(channel, "Error moving " + nonMax.getName() + ": " + e.getMessage());
                        continue;
                    }
                }
                if (currCategory == null) {
                    changes.add(channel.getAsMention() + " -> " + nonMax.getName());
                } else {
                    changes.add(channel.getAsMention() + ": " + currCategory.getAsMention() + " -> " + nonMax.getName());
                }
            }
        }
        nationChannels.entrySet().stream().filter(f -> f.getValue().size() > 1).forEach(f -> errors.putIfAbsent(f.getValue().iterator().next(), "Nation has multiple channels"));

        Supplier<String> errorsToString = () -> {
            StringBuilder response = new StringBuilder();
            response.append("channel\tcategory\tmention\treason\n");
            for (Map.Entry<TextChannel, String> entry : errors.entrySet()) {
                TextChannel channel = entry.getKey();
                response.append(channel.getName() + "\t" +
                        channel.getParentCategory() + "\t" +
                        entry.getKey().getAsMention() + "\t" +
                        entry.getValue() + "\n");
            }
            return response.toString();
        };
        Supplier<String> warningsToString = () -> {
            StringBuilder response = new StringBuilder();
            response.append("channel\tcategory\tmention\treason\n");
            for (Map.Entry<TextChannel, Set<String>> entry : warnings.entrySet()) {
                TextChannel channel = entry.getKey();
                for (String reason : entry.getValue()) {
                    response.append(channel.getName() + "\t" +
                            channel.getParentCategory() + "\t" +
                            entry.getKey().getAsMention() + "\t" +
                            reason + "\n");
                }
            }
            return response.toString();
        };
        IMessageBuilder msg = io.create();
        if (!errors.isEmpty()) {
            msg.file("errors.txt", errorsToString.get());
        }
        if (!warnings.isEmpty()) {
            msg.file("warnings.txt", warningsToString.get());
        }
        if (changes.isEmpty()) {
            msg.append("No channels will be moved.");
        } else {
            StringBuilder body = new StringBuilder();
            for (String change : changes) {
                body.append(change).append("\n");
            }
            if (force) {
                msg = msg.append(body.toString());
            } else {
                msg = msg.confirmation("Confirm Move " + changes.size() + " channels", body.toString(), command).cancelButton();
            }
        }
        return msg;
    }

    @Command(desc = "Add a discord role to all users in a server")
    @RolePermission(Roles.ADMIN)
    public String addRoleToAllMembers(@Me Guild guild, Role role) {
        int amt = 0;
        for (Member member : guild.getMembers()) {
            if (!member.getRoles().contains(role)) {
                RateLimitUtil.queue(guild.addRoleToMember(member, role));
                amt ++;
            }
        }
        return "Added " + amt + " roles to members (note: it may take a few minutes to update)";
    }
    @Command(desc = "View a list of reactions on a message sent by or mentioning the bot")
    @RolePermission(Roles.ADMIN)
    public String msgInfo(@Me IMessageIO channel, Message message, @Arg("List the ids of users who reacted")@Switch("i") boolean useIds) {
        StringBuilder response = new StringBuilder();

        List<MessageReaction> reactions = message.getReactions();
        Map<User, List<String>> reactionsByUser = new LinkedHashMap<>();
        for (MessageReaction reaction : reactions) {
            String emoji = reaction.getEmoji().asUnicode().getAsCodepoints();
            List<User> users = RateLimitUtil.complete(reaction.retrieveUsers());
            for (User user : users) {
                reactionsByUser.computeIfAbsent(user, f -> new ArrayList<>()).add(emoji);
            }
        }

        String title = "Message " + message.getIdLong();
        response.append("```" + DiscordUtil.trimContent(message.getContentRaw()).replaceAll("`", "\\`") + "```\n\n");

        if (!reactionsByUser.isEmpty()) {
            for (Map.Entry<User, List<String>> entry : reactionsByUser.entrySet()) {
                if (useIds) {
                    response.append(entry.getKey().getIdLong() + "\t" + StringMan.join(entry.getValue(), ","));
                } else {
                    response.append(entry.getKey().getAsMention() + "\t" + StringMan.join(entry.getValue(), ","));
                }
                response.append("\n");
            }
        }

        channel.create().embed(title, response.toString()).send();
        return null;
    }

    @Command(desc = "Close inactive channels in a category")
    @RolePermission(value = {
            Roles.INTERNAL_AFFAIRS_STAFF,
            Roles.INTERNAL_AFFAIRS,
            Roles.ECON_STAFF,
            Roles.ECON,
            Roles.MILCOM,
            Roles.MILCOM_NO_PINGS,
            Roles.FOREIGN_AFFAIRS,
            Roles.FOREIGN_AFFAIRS_STAFF,
    }, any = true)
    public String closeInactiveChannels(@Me GuildDB db, @Me IMessageIO outputChannel, Category category, @Arg("Close channels older than age") @Timediff long age, @Switch("f") boolean force) {
        long cutoff = System.currentTimeMillis() - age;

        IMessageBuilder msg = outputChannel.create();
        for (GuildMessageChannel channel : category.getTextChannels()) {
            String[] split = channel.getName().split("-");
            if (!MathMan.isInteger(split[split.length - 1])) continue;

            if (channel.getLatestMessageIdLong() > 0) {
                long lastId = channel.getLatestMessageIdLong();
                long lastMessageTime = net.dv8tion.jda.api.utils.TimeUtil.getTimeCreated(lastId).toEpochSecond() * 1000L;
                if (lastMessageTime > cutoff) continue;
            }

            String append = close(db, channel, force);
            msg = msg.append(append);
        }
        msg.append("Done!").send();
        return null;
    }

    @Command(desc = "List the self roles that you can assign yourself via the bot")
    public String listAssignableRoles(@Me GuildDB db, @Me Member member) {
        Map<Role, Set<Role>> assignable = db.getOrNull(GuildKey.ASSIGNABLE_ROLES);
        if (assignable == null || assignable.isEmpty()) {
            return "No roles found. See "
                    // TODO FIXME :||remove +  CM.self.create.cmd.toSlashMention()
                    + "";
        }
        assignable = new HashMap<>(assignable);
        if (!Roles.ADMIN.has(member)) {
            Set<Role> myRoles = new HashSet<>(member.getRoles());
            assignable.entrySet().removeIf(f -> !myRoles.contains(f.getValue()));
        }

        if (assignable.isEmpty()) return "You do not have permission to assign any roles";


        StringBuilder response = new StringBuilder();
        for (Map.Entry<Role, Set<Role>> entry : assignable.entrySet()) {
            Role role = entry.getKey();
            response.append("\n" + role.getName() + ":\n- "
                    + StringMan.join(entry.getValue().stream().map(Role::getName).collect(Collectors.toList()), "\n- "));
        }

        return response.toString().trim();
    }

    @Command(desc = "Allow a role to add/remove roles from users")
    @RolePermission(Roles.ADMIN)
    public String addAssignableRole(@Me GuildDB db, @Me User user, @Arg("Require this role in order to use the specified self roles") Role requireRole, Set<Role> assignableRoles) {
        Map<Role, Set<Role>> assignable = db.getOrNull(GuildKey.ASSIGNABLE_ROLES);
        if (assignable == null) assignable = new HashMap<>();

        assignable.computeIfAbsent(requireRole, f -> new HashSet<>()).addAll(assignableRoles);

        StringBuilder result = new StringBuilder();
        result.append(GuildKey.ASSIGNABLE_ROLES.set(db, user, assignable)).append("\n");

        result.append(StringMan.getString(requireRole) + " can now add/remove " + StringMan.getString(assignableRoles) + " via " + CM.self.add.cmd.toSlashMention() + " / " + CM.self.remove.cmd.toSlashMention() + "\n" +
                "- To see a list of current mappings, use "
                // TODO FIXME :||remove + CM.settings.info.cmd.key(GuildKey.ASSIGNABLE_ROLES.name())
                + "");
        return result.toString();
    }

    @Command(desc = "Remove a role from adding/removing specified roles\n" +
            "(having manage roles perm on discord overrides this)")
    @RolePermission(Roles.ADMIN)
    public String removeAssignableRole(@Me GuildDB db, @Me User user, Role requireRole, Set<Role> assignableRoles) {
        Map<Role, Set<Role>> assignable = db.getOrNull(GuildKey.ASSIGNABLE_ROLES);
        if (assignable == null) assignable = new HashMap<>();

        if (!assignable.containsKey(requireRole)) {
            return requireRole + " does not have any roles it can assign";
        }

        StringBuilder response = new StringBuilder();
        Set<Role> current = assignable.get(requireRole);

        for (Role role : assignableRoles) {
            if (current.contains(role)) {
                current.remove(role);
                response.append("\n" + requireRole + " can no longer assign " + role);
            } else {
                response.append("\nUnable to remove " + role + " (no mapping found)");
            }
        }

        response.append("\n" + GuildKey.ASSIGNABLE_ROLES.set(db, user, assignable));

        return response.toString() + "\n" +
                "- To see a list of current mappings, use " + GuildKey.ASSIGNABLE_ROLES.getCommandMention() + "";
    }

    @Command(desc = "Add discord role to a user\n" +
            "See: `{prefix}self list`")
    public String addRole(@Me GuildDB db, @Me Member author, Member member, Role addRole) {
        Map<Role, Set<Role>> assignable = db.getOrNull(GuildKey.ASSIGNABLE_ROLES);
        if (assignable == null) return "`!KeyStore ASSIGNABLE_ROLES` is not set`";
        boolean canAssign = Roles.ADMIN.has(author);
        if (!canAssign) {
            for (Role role : author.getRoles()) {
                if (assignable.getOrDefault(role, Collections.emptySet()).contains(addRole)) {
                    canAssign = true;
                    break;
                }
            }
        }
        if (!canAssign) {
            return "No permission to assign " + addRole + " (see: `listAssignableRoles` | ADMIN: see "
                    // TODO FIXME :||remove +  CM.self.create.cmd.toSlashMention()
                    + ")";
        }
        if (member.getRoles().contains(addRole)) {
            return member + " already has " + addRole;
        }
        RateLimitUtil.queue(db.getGuild().addRoleToMember(member, addRole));
        return "Added " + addRole + " to " + member;
    }

    @Command(desc = "Remove a role to a user\n" +
            "See: `{prefix}self list`")
    @RolePermission(value = {
            Roles.INTERNAL_AFFAIRS_STAFF,
            Roles.INTERNAL_AFFAIRS,
            Roles.ECON_STAFF,
            Roles.ECON,
            Roles.MILCOM,
            Roles.MILCOM_NO_PINGS,
            Roles.FOREIGN_AFFAIRS,
            Roles.FOREIGN_AFFAIRS_STAFF,
    }, any = true)
    public String removeRole(@Me GuildDB db, @Me Member author, Member member, Role addRole) {
        Map<Role, Set<Role>> assignable = db.getOrNull(GuildKey.ASSIGNABLE_ROLES);
        if (assignable == null) return "`!KeyStore ASSIGNABLE_ROLES` is not set`";
        boolean canAssign = Roles.ADMIN.has(author);
        if (!canAssign) {
            for (Role role : author.getRoles()) {
                if (assignable.getOrDefault(role, Collections.emptySet()).contains(addRole)) {
                    canAssign = true;
                    break;
                }
            }
        }
        if (!canAssign) {
            return "No permission to assign " + addRole + " (see: `listAssignableRoles` | ADMIN: see "
                    // TODO FIXME :||remove +  CM.self.create.cmd.toSlashMention()
                    + ")";
        }
        if (!member.getRoles().contains(addRole)) {
            return member + " does not have " + addRole;
        }
        RateLimitUtil.queue(db.getGuild().removeRoleFromMember(member, addRole));
        return "Removed " + addRole + " to " + member;
    }

    @Command(desc = "Opt out of beige alerts")
    @RolePermission(Roles.MEMBER)
    public String beigeAlertOptOut(@Me Member member, @Me DBNation me, @Me Guild guild) {
        Role role = Roles.PROTECTION_ALERT_OPT_OUT.toRole(guild);
        if (role == null) {
            return WarCommands.beigeAlertMode(member.getUser(), me, NationMeta.ProtectionAlertMode.NO_ALERTS);
        }
        if (member.getRoles().contains(role)) {
            // remove role
            RateLimitUtil.complete(guild.removeRoleFromMember(member, role));
            return "Opted in to beige alerts (@" + role.getName() + " role removed). Use the command again to opt out";
        }
        RateLimitUtil.complete(guild.addRoleToMember(member, role));
        return "Opted out of beige alerts (@" + role.getName() + " role added). Use the command again to opt in";
    }

    @Command(desc = "Unassign a mentee from all mentors")
    @RolePermission(Roles.INTERNAL_AFFAIRS_STAFF)
    public String unassignMentee(@Me GuildDB db, @Me DBNation nation, DBNation mentee) {
        ByteBuffer mentorBuf = db.getNationMeta(mentee.getNation_id(), NationMeta.CURRENT_MENTOR);
        DBNation currentMentor = mentorBuf != null ?  DBNation.getById(mentorBuf.getInt()) : null;

        if (currentMentor != null && currentMentor.active_m() < 1440) {
            User currentMentorUser = currentMentor.getUser();
        }

        ByteBuffer buf = ByteBuffer.allocate(Integer.SIZE + Long.SIZE);
        buf.putInt(0);
        buf.putLong(System.currentTimeMillis());

        db.setMeta(mentee.getNation_id(), NationMeta.CURRENT_MENTOR, buf.array());

//        db.deleteMeta(mentee.getNation_id(), NationMeta.CURRENT_MENTOR);

        MessageChannel alertChannel = db.getOrNull(GuildKey.INTERVIEW_PENDING_ALERTS);
        if (alertChannel != null) {
            String message = "Mentor (" + nation.getNation() + " | " + nation.getUserDiscriminator() +
                    ") unassigned Mentee (" + mentee.getNation() + " | " + mentee.getUserDiscriminator() + ")"
                    + (currentMentor != null ? " from Mentor (" + currentMentor.getNation() + " | " + currentMentor.getUserDiscriminator() + ")" : "");
            RateLimitUtil.queue(alertChannel.sendMessage(message));
        }

        return "Set " + mentee.getNation() + "'s mentor to null";
    }

    @Command(desc = "Assign a mentor to a mentee")
    @RolePermission(Roles.INTERNAL_AFFAIRS)
    public String mentor(@Me JSONObject command, @Me IMessageIO io, @Me GuildDB db, DBNation mentor, DBNation mentee, @Switch("f") boolean force) {
        User menteeUser = mentee.getUser();
        if (menteeUser == null) return "Mentee is not registered";

        if (!force) {
            ByteBuffer mentorBuf = db.getNationMeta(mentee.getNation_id(), NationMeta.CURRENT_MENTOR);
            if (mentorBuf != null) {
                DBNation current = DBNation.getById(mentorBuf.getInt());
                if (current != null && current.active_m() < 2880 && current.isVacation() == false) {
                    User currentUser = current.getUser();
                    if (currentUser != null && Roles.MEMBER.has(currentUser, db.getGuild())) {
                        String title = mentee.getNation() + " already has a mentor";
                        StringBuilder body = new StringBuilder();
                        body.append("Current mentor: " + current.getNationUrlMarkup(true));
                        io.create().confirmation(title, body.toString(), command).send();
                        return null;
                    }
                }
            }
        }

        ByteBuffer buf = ByteBuffer.allocate(Integer.SIZE + Long.SIZE);
        buf.putInt(mentor.getNation_id());
        buf.putLong(System.currentTimeMillis());

        // TOOD
        // add mentor to channel (remove any other mentor)
        // add mentor role to channel
        // Move channel to mentor category
        // Remove interviewer role (if mentor role is set)

        db.setMeta(mentee.getNation_id(), NationMeta.CURRENT_MENTOR, buf.array());
        return "Set " + mentee.getNation() + "'s mentor to " + mentor.getNation();
    }

    @Command(desc = "Assign yourself as someone's mentor")
    @RolePermission(Roles.INTERNAL_AFFAIRS_STAFF)
    public String mentee(@Me JSONObject command, @Me IMessageIO io, @Me GuildDB db, @Me DBNation me, DBNation mentee, @Switch("f") boolean force) {
        return mentor(command, io, db, me, mentee, force);
    }

    @Command(desc = "List mentees, grouped by their respective mentors", aliases = {"mymentees"})
    @RolePermission(value=Roles.INTERNAL_AFFAIRS)
    public String myMentees(@Me Guild guild, @Me GuildDB db, @Me DBNation me, @Default("*") Set<DBNation> mentees, @Arg("Activity requirements for mentors") @Default("2w") @Timediff long timediff) throws InterruptedException, ExecutionException, IOException {
        return listMentors(guild, db, Collections.singleton(me), mentees, timediff, db.isWhitelisted(), true, false);
    }

    @Command(desc = "List mentors, grouped by their respective mentees", aliases = {"listMentors", "mentors", "mentees"})
    @RolePermission(value=Roles.INTERNAL_AFFAIRS)
    public String listMentors(@Me Guild guild, @Me GuildDB db, @Default("*") Set<DBNation> mentors, @Default("*") Set<DBNation> mentees,
                              @Arg("Activity requirements for mentors") @Default("2w") @Timediff long timediff,
                              @Arg("Include an audit summary with the list") @Switch("a") boolean includeAudit,
                              @Arg("Do NOT list members without a mentor") @Switch("u") boolean ignoreUnallocatedMembers,
                              @Arg("List mentors without any active mentees") @Switch("i") boolean listIdleMentors) throws IOException, ExecutionException, InterruptedException {
        if (includeAudit && !db.isWhitelisted()) return "No permission to include audits";

        IACategory iaCat = db.getIACategory();
        if (iaCat == null) return "No ia category is enabled";

        IACheckup checkup = includeAudit ? new IACheckup(db, db.getAllianceList(), true) : null;

        Map<DBNation, List<DBNation>> mentorMenteeMap = new HashMap<>();
        Map<DBNation, DBNation> menteeMentorMap = new HashMap<>();
        Map<DBNation, IACategory.SortedCategory> categoryMap = new HashMap<>();
        Map<DBNation, Boolean> passedMap = new HashMap<>();

        for (Map.Entry<DBNation, IAChannel> entry : iaCat.getChannelMap().entrySet()) {
            DBNation mentee = entry.getKey();
            if (!mentees.contains(mentee)) continue;
            User user = mentee.getUser();
            if (user == null) continue;

            boolean graduated = Roles.hasAny(user, guild, Roles.GRADUATED, Roles.INTERNAL_AFFAIRS_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON_STAFF, Roles.MILCOM);

            IAChannel iaChan = iaCat.get(mentee);
            if (iaChan != null) {
                TextChannel myChan = iaChan.getChannel();
                if (myChan != null && myChan.getParentCategory() != null) {
                    IACategory.SortedCategory category = IACategory.SortedCategory.parse(myChan.getParentCategory().getName());
                    if (category != null) {
                        categoryMap.put(mentee, category);
                        if (category == IACategory.SortedCategory.ARCHIVE) {
                            graduated = true;
                        }
                    }
                }
            }
            passedMap.put(mentee, graduated);

            IACategory.AssignedMentor mentor = iaCat.getMentor(mentee, timediff);
            if (mentor != null) {
                mentorMenteeMap.computeIfAbsent(mentor.mentor, f -> new ArrayList<>()).add(mentee);
                menteeMentorMap.put(mentee, mentor.mentee);
            }
        }

        if (mentorMenteeMap.isEmpty()) return "No mentees found";

        List<Map.Entry<DBNation, List<DBNation>>> sorted = new ArrayList<>(mentorMenteeMap.entrySet());
        sorted.sort(new Comparator<Map.Entry<DBNation, List<DBNation>>>() {
            @Override
            public int compare(Map.Entry<DBNation, List<DBNation>> o1, Map.Entry<DBNation, List<DBNation>> o2) {
                return Integer.compare(o2.getValue().size(), o1.getValue().size());
            }
        });

        long requiredMentorActivity = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(20);
        List<BankTransfer> transactions = db.getTransactions(requiredMentorActivity, false);

        StringBuilder response = new StringBuilder();

        for (Map.Entry<DBNation, List<DBNation>> entry : sorted) {
            DBNation mentor = entry.getKey();
            List<DBNation> myMentees = new ArrayList<>(entry.getValue());
            Collections.sort(myMentees, new Comparator<DBNation>() {
                @Override
                public int compare(DBNation o1, DBNation o2) {
                    IACategory.SortedCategory c1 = categoryMap.get(o1);
                    IACategory.SortedCategory c2 = categoryMap.get(o2);
                    if (c1 != null && c2 != null) {
                        return Integer.compare(c1.ordinal(), c2.ordinal());
                    }
                    return Integer.compare(c1 == null ? 1 : 0, c2 == null ? 1 : 0);
                }
            });

            int numPassed = (int) myMentees.stream().filter(f -> passedMap.getOrDefault(f, false)).count();
            myMentees.removeIf(f -> passedMap.getOrDefault(f, false));
            if (myMentees.isEmpty()) {
                if (mentors.size() == 1) {
                    response.append("**No current mentors**");
                }
                continue;
            }

            response.append("\n\n**--- Mentor: " + mentor.getNation()).append("**: " + myMentees.size() + "\n");
            response.append("Graduated: " + numPassed + "\n");

            if (mentor.active_m() > 4880) {
                response.append("**MENTOR IS INACTIVE:** " + DiscordUtil.timestamp(mentor.getInactiveMs(), null)).append("\n");
            }
            if (mentor.isVacation()) {
                response.append("**MENTOR IS VM**").append("\n");
            }
            User mentorUser = mentor.getUser();
            if (mentorUser == null) {
                response.append("**MENTOR IS NOT VERIFIED**").append("\n");
            } else {
                if (!Roles.MEMBER.has(mentorUser, guild)) {
                    response.append("**MENTOR IS NOT MEMBER:** ").append("\n");
                } else if (!Roles.hasAny(mentorUser, guild, Roles.INTERNAL_AFFAIRS_STAFF, Roles.INTERVIEWER)) {
                    response.append("**MENTOR IS NOT IA STAFF:** ").append("\n");
                }
            }

            long latestTX = 0;
            for (Transaction2 transaction : transactions) {
                if (transaction.note == null || !transaction.note.contains("#incentive")) continue;
                if (transaction.sender_id == mentor.getNation_id()) {
                    latestTX = Math.max(latestTX, transaction.tx_datetime);
                    break;
                }
            }

            if (latestTX == 0) {
                response.append("**MENTOR HAS NOT MENTORED**\n");
            } else if (latestTX < System.currentTimeMillis() - TimeUnit.DAYS.toMillis(8)) {
                response.append("**MENTOR LAST INCENTIVE**: " + TimeUtil.secToTime(TimeUnit.MILLISECONDS, System.currentTimeMillis() - latestTX) + "\n");
            }

            for (DBNation myMentee : myMentees) {
                IAChannel myChan = iaCat.get(myMentee);
                IACategory.SortedCategory category = categoryMap.get(myMentee);
                response.append("`" + myMentee.getNation() + "` <" + myMentee.getUrl() + ">\n");
                response.append("- " + category + " | ");
                if (myChan != null && myChan.getChannel() != null) {
                    GuildMessageChannel tc = myChan.getChannel();
                    response.append(" | " + tc.getAsMention());
                    if (tc.getLatestMessageIdLong() > 0) {
                        long lastMessageTime = net.dv8tion.jda.api.utils.TimeUtil.getTimeCreated(tc.getLatestMessageIdLong()).toEpochSecond() * 1000L;
                        response.append(" | " + TimeUtil.secToTime(TimeUnit.MILLISECONDS, System.currentTimeMillis() - lastMessageTime));
                    }
                }
                response.append("\n- Infra" + myMentee.getInfra() + " land=" + myMentee.getLand() + " WarIndex=" + myMentee.getWarIndex() + " off:" + myMentee.getOff());

                if (includeAudit) {
                    Map<IACheckup.AuditType, Map.Entry<Object, String>> checkupResult = checkup.checkup(myMentee, true, true);
                    checkupResult.entrySet().removeIf(f -> f.getValue() == null || f.getValue().getValue() == null);
                    if (!checkupResult.isEmpty()) {
                        response.append("\n- Failed: [" + StringMan.join(checkupResult.keySet(), ", ") + "]");
                    }
                }
                response.append("\n\n");
            }

        }

        if (db.isValidAlliance()) {
            AllianceList alliance = db.getAllianceList();
            Set<DBNation> members = alliance.getNations(true, 2880, true);
            members.removeIf(f -> !mentees.contains(f));

            List<DBNation> membersUnverified = new ArrayList<>();
            List<DBNation> membersNotOnDiscord = new ArrayList<>();
            List<DBNation> nationsNoIAChan = new ArrayList<>();
            List<DBNation> noMentor = new ArrayList<>();
            for (DBNation member : members) {
                User user = member.getUser();
                if (user == null) {
                    membersUnverified.add(member);
                    continue;
                }
                if (guild.getMember(user) == null) {
                    membersNotOnDiscord.add(member);
                    continue;
                }
                if (Roles.hasAny(user, guild, Roles.GRADUATED, Roles.ADMIN, Roles.INTERNAL_AFFAIRS_STAFF, Roles.INTERNAL_AFFAIRS, Roles.INTERVIEWER, Roles.MILCOM, Roles.MILCOM_NO_PINGS, Roles.ECON, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.FOREIGN_AFFAIRS)) {
                    continue;
                }
                if (!passedMap.getOrDefault(member, false)) {
                    if (iaCat.get(member) == null) {
                        nationsNoIAChan.add(member);
                        continue;
                    }
                    if (!menteeMentorMap.containsKey(member)) {
                        noMentor.add(member);
                        continue;
                    }
                }
            }

            if (!ignoreUnallocatedMembers) {
                if (listIdleMentors) {
                    if (mentors.size() > 100) {
                        return "Please provide a list of mentors";
                    }
                    List<DBNation> idleMentors = new ArrayList<>();
                    for (DBNation mentor : mentors) {
                        List<DBNation> myMentees = mentorMenteeMap.getOrDefault(mentor, Collections.emptyList());
                        myMentees.removeIf(f -> f.active_m() > 4880 || f.isVacation() || passedMap.getOrDefault(f, false));
                        if (myMentees.isEmpty()) {
                            idleMentors.add(mentor);
                        }
                    }
                    if (!idleMentors.isEmpty()) {
                        List<String> memberNames = idleMentors.stream().map(DBNation::getNation).collect(Collectors.toList());
                        response.append("\n**Idle mentors**").append("\n- ").append(StringMan.join(memberNames, "\n- "));
                    }
                }

                if (!membersUnverified.isEmpty()) {
                    List<String> memberNames = membersUnverified.stream().map(DBNation::getNation).collect(Collectors.toList());
                    response.append("\n**Unverified members**").append("\n- ").append(StringMan.join(memberNames, "\n- "));
                }
                if (!membersNotOnDiscord.isEmpty()) {
                    List<String> memberNames = membersNotOnDiscord.stream().map(DBNation::getNation).collect(Collectors.toList());
                    response.append("\n**Members left discord**").append("\n- ").append(StringMan.join(memberNames, "\n- "));
                }
                if (!nationsNoIAChan.isEmpty()) {
                    List<String> memberNames = nationsNoIAChan.stream().map(DBNation::getNation).collect(Collectors.toList());
                    response.append("\n**No interview channel**").append("\n- ").append(StringMan.join(memberNames, "\n- "));
                }
            }
            if (!noMentor.isEmpty()) {
                List<String> memberNames = noMentor.stream().map(DBNation::getNation).collect(Collectors.toList());
                response.append("\n**No mentor**").append("\n- ").append(StringMan.join(memberNames, "\n- "));
            }
        }

        response.append("\n\nTo assign a nation as your mentee, use " + CM.interview.mentee.cmd.toSlashMention());
        return response.toString();
    }

    @Command(desc = "Ranking of nations by how many advertisements they have registered (WIP)")
    @RolePermission(value = {Roles.INTERNAL_AFFAIRS,Roles.ECON}, any=true)
    public String adRanking(@Me User author, @Me GuildDB db, @Me IMessageIO io, @Me JSONObject command, @Switch("u") boolean uploadFile) {
        Role role = Roles.MEMBER.toRole(db);
        if (role == null) throw new IllegalArgumentException("No member role is set via " + CM.role.setAlias.cmd.toSlashMention() + "");

        Map<DBNation, Integer> rankings = new HashMap<>();

        for (Member member : db.getGuild().getMembers()) {
            DBNation nation = DiscordUtil.getNation(member.getUser());
            if (nation == null) continue;
            ByteBuffer countBuf = nation.getMeta(NationMeta.RECRUIT_AD_COUNT);
            if (countBuf == null) continue;
            rankings.put(nation, countBuf.getInt());
        }
        if (rankings.isEmpty()) return "No rankings founds";
        new SummedMapRankBuilder<>(rankings).sort().nameKeys(DBNation::getName).build(author, io, command, "Most advertisements", uploadFile);
        return null;
    }

    @Command(desc = "Ranking of nations by how many incentives they have received\n" +
            "Settings: `REWARD_MENTOR` and `REWARD_REFERRAL`")
    @RolePermission(value = {Roles.INTERNAL_AFFAIRS,Roles.ECON}, any=true)
    public String incentiveRanking(@Me GuildDB db, @Me IMessageIO io, @Me JSONObject command, @Timestamp long timestamp) {
        List<BankTransfer> transactions = db.getTransactions(timestamp, false);

        Map<String, Map<DBNation, Integer>> incentivesByGov = new HashMap<>();

        for (Transaction2 transaction : transactions) {
            if (transaction.note == null || !transaction.note.contains("#incentive")) continue;
            Map<String, String> notes = DNS.parseTransferHashNotes(transaction.note);
            String incentive = notes.get("#incentive");
            DBNation gov = DBNation.getById((int) transaction.sender_id);

            if (gov != null) {
                Map<DBNation, Integer> byIncentive = incentivesByGov.computeIfAbsent(incentive, f -> new HashMap<>());
                byIncentive.put(gov, byIncentive.getOrDefault(gov, 0) + 1);
            }
        }

        for (Map.Entry<String, Map<DBNation, Integer>> entry : incentivesByGov.entrySet()) {
            String title = entry.getKey();
            new SummedMapRankBuilder<>(entry.getValue())
                    .sort()
                    .nameKeys(f -> f.getNation())
                    .build(io, command, title, true);
        }
        return null;
    }

    @Command(desc = "Get the top inactive players")
    public void inactive(@Me IMessageIO channel, @Me JSONObject command, Set<DBNation> nations, @Arg("Required days inactive") @Default("7") int days, @Switch("a") boolean includeApplicants, @Switch("v") boolean includeVacationMode, @Switch("p") int page) {
        if (!includeApplicants) nations.removeIf(f -> f.getPosition() <= 1);
        if (!includeVacationMode) nations.removeIf(f -> f.isVacation());
        nations.removeIf(f -> f.active_m() * TimeUnit.DAYS.toMinutes(1) < days);

        List<DBNation> nationList = new ArrayList<>(nations);
        nationList.sort((o1, o2) -> Integer.compare(o2.active_m(), o1.active_m()));

        int perPage = 5;

        String title = "Inactive nations";
        List<String> results = nationList.stream().map(f -> f.toMarkdown()).collect(Collectors.toList());
        channel.create().paginate(title, command, page, perPage, results).send();

    }

    @Command(desc = "Set the discord category for an interview channel", aliases = {"iacat", "interviewcat", "interviewcategory"})
    @RolePermission(value = {Roles.INTERNAL_AFFAIRS, Roles.INTERNAL_AFFAIRS_STAFF}, any=true)
    public String iaCat(@Me IMessageIO channel, @Filter("interview.") Category category) {
        if (!(channel instanceof ICategorizableChannel)) return "This channel cannot be categorized";
        ICategorizableChannel tc = ((ICategorizableChannel) channel);
        RateLimitUtil.queue(tc.getManager().setParent(category));
        return "Moved " + tc.getAsMention() + " to " + category.getName();
    }

    @Command(desc = "Bulk send the result of a bot command to a list of nations in your alliance\n" +
            "The command will run as each user\n" +
            "Nations which are not registered or lack permission to use a command will result in an error\n" +
            "It is recommended to review the output sheet before confirming and sending the results")
    @RolePermission(value=Roles.ADMIN)
    @IsAlliance
    @NoFormat
    public String sendCommandOutput(NationPlaceholders placeholders, ValueStore store, @Me GuildDB db, @Me Guild guild, @Me User author, @Me IMessageIO channel,
                                    @Arg("Nations to mail command results to") Set<DBNation> nations,
                                    String subject,
                                    @Arg("The locutus command to run") String command,
                                    @Arg("Message to send along with the command result")
                                    @TextArea String body, @Switch("s") SpreadSheet sheet,
                                    @Arg("Send as a discord direct message")
                                    @Switch("d") boolean sendDM) throws IOException, GeneralSecurityException {
        if (sheet == null) {
            sheet = SpreadSheet.create(db, SheetKey.MAIL_RESPONSES_SHEET);
        }
        String checkModMsg = command;
        if (subject != null) checkModMsg += subject;
        if (body != null) checkModMsg += body;
        GPTUtil.checkThrowModeration(checkModMsg);

        List<String> header = new ArrayList<>(Arrays.asList(
                "nation",
                "alliance",
                "mail-id",
                "subject",
                "body"
        ));

        sheet.setHeader(header);
        Map<DBNation, Map.Entry<CommandResult, String>> errors = new LinkedHashMap<>();

        if (nations.size() > 300 && !Roles.ADMIN.hasOnRoot(author)) {
            return "Max allowed: 300 nations.";
        }

        List<String> notInAA = new ArrayList<>();
        for (DBNation nation : nations) {
            if (!db.isAllianceId(nation.getAlliance_id())) {
                notInAA.add(nation.getNationUrlMarkup(true));
            }
        }
        if (!notInAA.isEmpty()) {
            return "The following nations are not in the alliance:\n - " + String.join("\n - ", notInAA);
        }

        if (nations.isEmpty()) return "No nations specified";

        Future<IMessageBuilder> msgFuture = channel.send("Please wait...");
        long start = System.currentTimeMillis();

        PlaceholderCache<DBNation> cache = new PlaceholderCache<>(nations);
        Function<DBNation, String> subjectF = placeholders.getFormatFunction(store, subject, cache, true);
        Function<DBNation, String> bodyF = placeholders.getFormatFunction(store, body, cache, true);

        int success = 0;
        for (DBNation nation : nations) {
            if (-start + (start = System.currentTimeMillis()) > 5000) {
                try {
                    msgFuture.get().clear().append("Running for: " + nation.getNation() + "...").send();
                } catch (InterruptedException | ExecutionException e) {
                    // ignore
                }
            }
            User nationUser = nation.getUser();
            String subjectFormat = subjectF.apply(nation);
            String bodyFormat = bodyF.apply(nation);

            Map.Entry<CommandResult, List<StringMessageBuilder>> response = nation.runCommandInternally(guild, nationUser, command);
            CommandResult respType = response.getKey();
            List<StringMessageBuilder> messages = response.getValue();

            if (respType == CommandResult.SUCCESS) {
                if (!bodyFormat.isEmpty()) {
                    bodyFormat = bodyFormat + "\n";
                }

                header.set(0, MarkupUtil.sheetUrl(nation.getNation(), nation.getUrl()));
                header.set(1, MarkupUtil.sheetUrl(nation.getAllianceName(), nation.getAllianceUrl()));
                header.set(2, "");
                header.set(3, subjectFormat);
                header.set(4, IMessageBuilder.toJson(bodyFormat, messages, true, true, true).toString());

                sheet.addRow(header);
                success++;
            } else{
                if (messages == null) {
                    respType = CommandResult.NO_RESPONSE;
                } else if (messages.isEmpty()) {
                    respType = CommandResult.EMPTY_RESPONSE;
                }
                errors.put(nation, new AbstractMap.SimpleEntry<>(respType, IMessageBuilder.toJson("", messages, false, false, false).toString()));
            }
        }

        sheet.updateClearCurrentTab();
        sheet.updateWrite();

        List<String> errorMsgs = new ArrayList<>();
        if (!errors.isEmpty()) {
            for (Map.Entry<DBNation, Map.Entry<CommandResult, String>> entry : errors.entrySet()) {
                DBNation nation = entry.getKey();
                Map.Entry<CommandResult, String> error = entry.getValue();
                errorMsgs.add(nation.getNation() + " -> " + error.getKey() + ": " + error.getValue());
            }
        }

        String title = "Send " + success + " messages";
        StringBuilder embed = new StringBuilder();
        IMessageBuilder msg = channel.create();
        sheet.attach(msg, "mail_command", embed, false, 0);
        embed.append("\nPress `confirm` to confirm");
        // TODO FIXME :||remove
//        CM.mail.sheet cmd = CM.mail.sheet.cmd.sheet(sheet.getURL()).dm(sendDM ? "true" : null).skipMail(skipMail ? "true" : null);
//        msg.confirmation(title, embed.toString(), cmd).send();

        if (errorMsgs.isEmpty()) return null;
        return "Errors\n- " + StringMan.join(errorMsgs, "\n- ");
    }

    @Command(desc = "Bulk send in-game mail from a google sheet\n" +
            "Columns: `nation`, `subject`, `body`\n" +
            "Other bulk mail commands forward to this command")
    @HasApi
    @RolePermission(Roles.ADMIN)
    public String dmSheet(@Me GuildDB db, @Me JSONObject command, @Me IMessageIO io, @Me User author, SpreadSheet sheet, @Switch("f") boolean force) {
        if (!Roles.MAIL.hasOnRoot(author)) {
            throw new IllegalArgumentException("Missing role: " + Roles.MAIL.toDiscordRoleNameElseInstructions(Locutus.imp().getServer()));
        }
        List<List<Object>> data = sheet.fetchAll(null);

        List<Object> nationNames = sheet.findColumn(0, "nation", "id");
        List<Object> subjects = sheet.findColumn("subject");
        List<Object> bodies = sheet.findColumn("message", "body", "content");
        if (nationNames == null || nationNames.size() <= 1) return "No column found: `nation`";
        if (subjects == null) return "No column found: `subject`";
        if (bodies == null) return "No column found: `message`";

        Set<Integer> alliances = new HashSet<>();
        int inactive = 0;
        int vm = 0;
        int noAA = 0;
        int applicants = 0;

        Map<DBNation, Map.Entry<String, String>> messageMap = new LinkedHashMap<>();
        if (force) {
            String messagesJoined = messageMap.values().stream().map(e -> e.getKey() + " " + e.getValue()).collect(Collectors.joining("\n"));
            GPTUtil.checkThrowModeration(messagesJoined);
        }

        for (int i = 1; i < nationNames.size(); i++) {
            Object nationNameObj = nationNames.get(i);
            if (nationNameObj == null) continue;
            String nationNameStr = nationNameObj.toString();
            if (nationNameStr.isEmpty()) continue;

            DBNation nation = DiscordUtil.parseNation(nationNameStr);
            if (nation == null) return "Invalid nation: `" + nationNameStr + "`";

            Object subjectObj = subjects.get(i);
            Object messageObj = bodies.get(i);
            if (subjectObj == null) {
                return "No subject found for: `" + nation.getNation() + "`";
            }
            if (messageObj == null) {
                return "No body found for: `" + nation.getNation() + "`";
            }
            Map.Entry<String, String> msgEntry = new AbstractMap.SimpleEntry<>(
                    subjectObj.toString(), messageObj.toString());

            messageMap.put(nation, msgEntry);

            // metrics
            alliances.add(nation.getAlliance_id());
            if (nation.active_m() > 7200) inactive++;
            if (nation.isVacation()) vm++;
            if (nation.getAlliance_id() == 0) noAA++;
            if (nation.getPosition() <= 1) applicants++;
        }

        if (messageMap.size() > 1000 && !Roles.ADMIN.hasOnRoot(author)) {
            return "Max allowed: 1000 messages.";
        }

        if (!force) {
            String title = "Send " + messageMap.size() + " to nations in " + alliances.size() + " alliances";
            StringBuilder body = new StringBuilder("Messages:");
            IMessageBuilder msg = io.create();
            sheet.attach(msg, "mail", body, false, 0);

            if (inactive > 0) body.append("Inactive Receivers: " + inactive + "\n");
            if (vm > 0) body.append("vm Receivers: " + vm + "\n");
            if (noAA > 0) body.append("No Alliance Receivers: " + noAA + "\n");
            if (applicants > 0) body.append("applicant receivers: " + applicants + "\n");

            body.append("\nPress to confirm");
            msg.confirmation(title, body.toString(), command, "force").send();
            return null;
        }

        io.send("Sending to " + messageMap.size() + " nations in " + alliances.size() + " alliances. Please wait.");
        List<String> response = new ArrayList<>();
        int errors = 0;
        for (Map.Entry<DBNation, Map.Entry<String, String>> entry : messageMap.entrySet()) {
            DBNation nation = entry.getKey();
            Map.Entry<String, String> msgEntry = entry.getValue();
            String subject = msgEntry.getKey();
            String body = msgEntry.getValue();
            StringMessageBuilder richBody = StringMessageBuilder.fromText(body, true, db.getGuild());

            List<String> result = new ArrayList<>();
            User user = nation.getUser();
            if (user == null) {
                result.add("\n- **dm**: No discord user set. See " + CM.register.cmd.toSlashMention());
            } else {
                try {
                    PrivateChannel channel = RateLimitUtil.complete(user.openPrivateChannel());
                    DiscordChannelIO dmIo = new DiscordChannelIO(channel);
                    IMessageBuilder msg = dmIo.create();
                    if (richBody != null) richBody.writeTo(msg);
                    else msg.append(body);
                    msg.send();
                    result.add("\n- **dm**: Sent dm");
                } catch (Throwable e) {
                    e.printStackTrace();
                    result.add("\n- **dm**: Failed to send dm (`" + e.getMessage() + "`)");
                }
            }
            response.add(nation.getUrl() + " -> " + StringMan.getString(result));
        }
        return "Done!\n- " + StringMan.join(response, "\n- ");
    }

    private String channelMemberInfo(Set<Integer> aaIds, Role memberRole, Member member) {
        DBNation nation = DiscordUtil.getNation(member.getUser());

        StringBuilder response = new StringBuilder(DiscordUtil.getFullUsername(member.getUser()));
        response.append(" | `" + member.getAsMention() + "`");
        if (nation != null) {
            response.append(" | N:" + nation.getNation());
            if (!aaIds.isEmpty() && !aaIds.contains(nation.getNation_id())) {
                response.append(" | AA:" + nation.getAllianceName());
            }
            if (nation.getPosition() <= 1) {
                response.append(" | applicant");
            }
            if (nation.isVacation()) {
                response.append(" | VACATION");
            }
            if (nation.active_m() > 10000) {
                response.append(" | inactive=" + DiscordUtil.timestamp(nation.getInactiveMs(), null));
            }
        }
        if (aaIds.isEmpty() && !member.getRoles().contains(memberRole)) {
            response.append(" | No Member Role");
        }
        return response.toString();
    }

    @Command(desc = "List members who can see a discord channel")
    @RolePermission(value = {Roles.INTERNAL_AFFAIRS, Roles.ADMIN}, any = true)
    public String channelMembers(@Me GuildDB db, MessageChannel channel) {
        Set<Integer> aaIds = db.getAllianceIds();
        Role memberRole = Roles.MEMBER.toRole(db.getGuild());

        List<Member> members = (channel instanceof IMemberContainer) ? ((IMemberContainer) channel).getMembers() : Collections.emptyList();
        members.removeIf(f -> f.getUser().isBot() || f.getUser().isSystem());

        List<String> results = new ArrayList<>();
        for (Member member : members) {
            results.add(channelMemberInfo(aaIds, memberRole, member));
        }
        if (results.isEmpty()) return "No users found";
        return StringMan.join(results, "\n");
    }

    @Command(desc = "List all guild channels and what members have access to each")
    @RolePermission(value = {Roles.ADMIN}, any = true)
    public String allChannelMembers(@Me GuildDB db) {
        Set<Integer> aaIds = db.getAllianceIds();
        Role memberRole = Roles.MEMBER.toRole(db.getGuild());

        StringBuilder result = new StringBuilder();

        for (Category category : db.getGuild().getCategories()) {
            result.append("**" + category.getName() + "**\n");
            for (TextChannel GuildMessageChannel : category.getTextChannels()) {
                result.append(GuildMessageChannel.getAsMention() + "\n");

                for (Member member : GuildMessageChannel.getMembers()) {
                    result.append(channelMemberInfo(aaIds, memberRole, member) + "\n");
                }
            }
        }
        return result.toString();
    }

    @Command(desc = "List discord channels a member has access to")
    @RolePermission(value = {Roles.INTERNAL_AFFAIRS, Roles.ADMIN}, any = true)
    public String memberChannels(@Me Guild guild, Member member) {
        List<String> channels = guild.getTextChannels().stream().filter(f -> f.getMembers().contains(member)).map(f -> f.getAsMention()).collect(Collectors.toList());
        User user = member.getUser();
        return DiscordUtil.getFullUsername(user) + " has access to:\n" +
                StringMan.join(channels, "\n");
    }

    @Command(desc = "Move an interview channel from the `interview-archive` category")
    @RolePermission(Roles.MEMBER)
    public String open(@Me User author, @Me Guild guild, @Me IMessageIO channel, @Default Category category) {
        if (!(channel instanceof TextChannel)) {
            return "Not a text channel";
        }
        String closeChar = "\uD83D\uDEAB";
        TextChannel tc = (TextChannel) channel;
        Category channelCategory = tc.getParentCategory();

        if (channelCategory != null && channelCategory.getName().toLowerCase().contains("archive") && category == null) {
            throw new IllegalArgumentException("Please provide a category to move this channel to");
        }
        if (category != null && channelCategory != null && !channelCategory.getName().toLowerCase().contains("archive") && !Roles.INTERNAL_AFFAIRS_STAFF.has(author, guild)) {
            throw new IllegalArgumentException("You do not have permission to move this channel: INTERNAL_AFFAIRS_STAFF");
        }

        if (tc.getName().contains(closeChar)) {
            String newName = tc.getName().replace(closeChar, "");
            RateLimitUtil.queue(tc.getManager().setName(newName));
        }
        if (category != null) {
            RateLimitUtil.queue(tc.getManager().setParent(category));
        }
        return "Reopened channel";
    }

    @Command(desc = "Close a war room, interview or embassy discord channel")
    @RolePermission(value = {Roles.INTERNAL_AFFAIRS, Roles.MILCOM, Roles.ECON, Roles.ECON_STAFF}, any=true)
    public String close(@Me GuildDB db, @Me GuildMessageChannel channel, @Switch("f") boolean forceDelete) {
        if (!(channel instanceof TextChannel)) {
            return "Not a text channel";
        }
        String closeChar = "\uD83D\uDEAB";
        long expireTime = TimeUnit.HOURS.toMillis(24);

        boolean canClose = true;

        TextChannel tc = (TextChannel) channel;

        IACategory iaCat = db.getIACategory();
        if (iaCat != null && iaCat.isInCategory(tc)) {
            canClose = true;
        } else if (tc.getParentCategory() != null && tc.getParentCategory().getName().toLowerCase().contains("warcat")) {
            canClose = true;
        } else {
            String[] split = channel.getName().split("-");
            String id = split[split.length - 1];
            if (split.length >= 2 && MathMan.isInteger(id)) {
                canClose = true;
            }
        }
        if (canClose) {
            Category parent = tc.getParentCategory();
            if (channel.getName().contains(closeChar)) {
                RateLimitUtil.queue(((GuildMessageChannel) channel).delete());
                return null;
            } else if (parent != null && (parent.getName().toLowerCase().startsWith("treasury") || parent.getName().toLowerCase().startsWith("grant"))) {
                int i = 0;
                for (GuildMessageChannel otherChannel : db.getGuild().getTextChannels()) {
                    if (otherChannel.getName().contains(closeChar) && otherChannel.getLatestMessageIdLong() > 0) {
                        String[] split = otherChannel.getName().split("-");
                        if (split.length == 2 && MathMan.isInteger(split[1])) {
                            long id = otherChannel.getLatestMessageIdLong();
                            OffsetDateTime created = net.dv8tion.jda.api.utils.TimeUtil.getTimeCreated(id);
                            long diff = System.currentTimeMillis() - created.toEpochSecond() * 1000L;
                            if (diff > expireTime && ++i < 5) {
                                RateLimitUtil.queue(otherChannel.delete());
                            }
                        }
                    }
                }
                RateLimitUtil.queue(((GuildMessageChannel) channel).getManager().setName(closeChar + channel.getName()));
                return "Marked channel as closed. Auto deletion in >24h. Use " + CM.channel.open.cmd.toSlashMention() + " to reopen. Use " + CM.channel.close.current.cmd.toSlashMention() + " again to force close";
            }

            Category archiveCategory = db.getOrNull(GuildKey.ARCHIVE_CATEGORY);
            if (archiveCategory != null) {
                if (true || archiveCategory.equals(tc.getParentCategory()) || forceDelete) {
                    RateLimitUtil.queue(tc.delete());
                }
                else {
                    long cutoff = System.currentTimeMillis() - expireTime;
                    Locutus.imp().getExecutor().submit(new Runnable() {
                        @Override
                        public void run() {
                            for (GuildMessageChannel toDelete : archiveCategory.getTextChannels()) {
                                try {
                                    long created = net.dv8tion.jda.api.utils.TimeUtil.getTimeCreated(toDelete.getLatestMessageIdLong()).toEpochSecond() * 1000L;
                                    if (created < cutoff) {
                                        RateLimitUtil.queue(toDelete.delete());
                                    }
                                } catch (IllegalStateException ignore) {
                                    ignore.printStackTrace();
                                }
                            }
                        }
                    });
                    RateLimitUtil.queue(tc.getManager().setParent(archiveCategory));
                    for (PermissionOverride perm : tc.getMemberPermissionOverrides()) {
                        RateLimitUtil.queue(tc.upsertPermissionOverride(perm.getMember()).setAllowed(Permission.VIEW_CHANNEL).setDenied(Permission.MESSAGE_SEND));
                    }
                    return "This channel is archived and marked for deletion after 2 days. Do not reply here";
                }
            }
            RateLimitUtil.queue(((GuildMessageChannel)channel).delete());
            return null;
        } else {
            return "You do not have permission to close this channel";
        }
    }

    @Command(desc = "Create an interview channel")
    public static String interview(@Me GuildDB db, @Default("%user%") User user) {
        IACategory iaCat = db.getIACategory(true, true, true);

        if (iaCat.getCategories().isEmpty()) {
            return "No categories found starting with: `interview`";
        }

        GuildMessageChannel channel = iaCat.getOrCreate(user, true);
        if (channel == null) return "Unable to find or create channel (does a category called `interview` exist?)";

        Role applicantRole = Roles.APPLICANT.toRole(db.getGuild());
        if (applicantRole != null) {
            Member member = db.getGuild().getMember(user);
            if (member == null || !member.getRoles().contains(applicantRole)) {
                RateLimitUtil.queue(db.getGuild().addRoleToMember(user, applicantRole));
            }
        }

        return channel.getAsMention();
    }

    @Command(aliases = {"syncInterviews", "syncInterview"}, desc = "Force an update of all interview channels and print the results")
    @RolePermission(value = {Roles.INTERNAL_AFFAIRS, Roles.INTERNAL_AFFAIRS_STAFF}, any=true)
    public String syncInterviews(@Me IMessageIO channel, @Me GuildDB db) {
        IACategory iaCat = db.getIACategory();
        iaCat.load();
        iaCat.purgeUnusedChannels(channel);
        iaCat.alertInvalidChannels(channel);
        return "Done!";
    }

    @Command(desc = "Set yourself as the referrer for a user")
    @RolePermission(value = { Roles.INTERNAL_AFFAIRS, Roles.INTERNAL_AFFAIRS_STAFF, Roles.INTERVIEWER, Roles.MENTOR }, any = true)
    public String setReferrer(@Me GuildDB db, @Me DBNation me, User user) {
        if (!db.isValidAlliance()) return "Note: No alliance registered to guild";
        if (!db.isAllianceId(me.getAlliance_id())) {
            return "Note: You are not in this alliance";
        }
        if (db.getMeta(user.getIdLong(), NationMeta.REFERRER) == null) {
            db.getHandler().setReferrer(user, me);
        }
        return null;
    }

    @Command(aliases = {"sortInterviews", "sortInterview"}, desc = "Sort the interview channels to an audit category\n" +
            "An appropriate discord category must exist in the form: `interview-CATEGORY`\n" +
            "Allowed categories: `INACTIVE,ENTRY,RAIDS,BANK,SPIES,BUILD,COUNTERS,TEST,ARCHIVE`")
    @RolePermission(value = {Roles.INTERNAL_AFFAIRS, Roles.INTERNAL_AFFAIRS_STAFF}, any=true)
    public String sortInterviews(@Me GuildMessageChannel channel, @Me IMessageIO io, @Me GuildDB db, @Arg("Sort channels already in a category") @Default("true") boolean sortCategorized) {
        IACategory iaCat = db.getIACategory();
        iaCat.purgeUnusedChannels(io);
        iaCat.load();
        if (iaCat.isInCategory(channel)) {
            iaCat.sort(io, Collections.singleton((TextChannel) channel), sortCategorized);
        } else {
            iaCat.sort(io, iaCat.getAllChannels(), true);
        }
        return "Done!";
    }

    @Command(desc = "List the interview channels, by category + activity")
    @RolePermission(value = {Roles.INTERNAL_AFFAIRS, Roles.INTERNAL_AFFAIRS_STAFF}, any=true)
    public String iachannels(@Me User author, @Me DBNation me, @Me Guild guild, @Me GuildDB db, String filter, @Arg("Highlight channels inactive for longer than the time specified") @Default("1d") @Timediff long time) throws IOException, GeneralSecurityException {
        try {
            if (!filter.isEmpty()) filter += ",*";
            Set<DBNation> allowedNations = DiscordUtil.parseNations(guild, author, me, filter, false, false);

            Set<Integer> aaIds = db.getAllianceIds();
            if (aaIds.isEmpty()) return "No alliance set " + GuildKey.ALLIANCE_ID.getCommandMention() + "";

            IACategory cat = db.getIACategory();
            if (cat.getCategories().isEmpty()) return "No `interview` categories found";
            cat.load();

            Map<Category, List<IAChannel>> channelsByCategory = new LinkedHashMap<>();

            for (Map.Entry<DBNation, IAChannel> entry : cat.getChannelMap().entrySet()) {
                DBNation nation = entry.getKey();

                if (!allowedNations.contains(nation)) continue;
                if (!aaIds.contains(nation.getAlliance_id()) || nation.active_m() > 10000 || nation.isVacation()) continue;
                User user = nation.getUser();
                if (user == null) continue;


                IAChannel iaChan = entry.getValue();
                TextChannel channel = iaChan.getChannel();
                Category category = channel.getParentCategory();
                String name = category.getName().toLowerCase();
                if (name.endsWith("-archive") || name.endsWith("-inactive")) continue;
                channelsByCategory.computeIfAbsent(category, f -> new ArrayList<>()).add(iaChan);
            }

            List<Category> categories = new ArrayList<>(channelsByCategory.keySet());
            Collections.sort(categories, Comparator.comparingInt(Category::getPosition));

            StringBuilder response = new StringBuilder();

            for (Category category : categories) {
                List<IAChannel> channels = channelsByCategory.get(category);
                List<Map.Entry<IAChannel, Long>> channelsByActivity = new ArrayList<>();
                Map<IAChannel, Map.Entry<Message, Message>> latestMsgs = new LinkedHashMap<>();


                for (IAChannel iaChan : channels) {
                    GuildMessageChannel channel = iaChan.getChannel();
                    DBNation nation = iaChan.getNation();
                    List<Message> messages = RateLimitUtil.complete(channel.getHistory().retrievePast(25));
                    User user = nation.getUser();

                    Message latestMessageUs = null;
                    Message latestMessageThem = null;

                    for (Message message : messages) {
                        User msgAuth = message.getAuthor();
                        if (msgAuth.isBot() || msgAuth.isSystem()) continue;
                        String content = DiscordUtil.trimContent(message.getContentRaw());
                        if (content.startsWith(Settings.commandPrefix() + "")) continue;

                        long msgTime = message.getTimeCreated().toEpochSecond();

                        if (!msgAuth.isBot() && !msgAuth.isSystem()) {


                            if (msgAuth.getIdLong() != user.getIdLong()) {
                                if (latestMessageUs == null || latestMessageUs.getTimeCreated().toEpochSecond() < msgTime) {
                                    latestMessageUs = message;
                                }
                            } else if (latestMessageThem == null || latestMessageThem.getTimeCreated().toEpochSecond() < msgTime) {
                                latestMessageThem = message;
                            }
                        }
                    }


                    long last = 0;
                    if (latestMessageUs != null) last = Math.max(last, latestMessageUs.getTimeCreated().toEpochSecond() * 1000L);
                    if (latestMessageThem != null) last = Math.max(last, latestMessageThem.getTimeCreated().toEpochSecond() * 1000L);
                    long now = System.currentTimeMillis();
                    long diffMsg = now - last;
                    long diffActive = TimeUnit.MINUTES.toMillis(nation.active_m());

                    if (last == 0 || diffMsg > diffActive + time) {
                        long activityValue = diffMsg - diffActive;
                        channelsByActivity.add(new AbstractMap.SimpleEntry<>(iaChan, activityValue));
                        latestMsgs.put(iaChan, new AbstractMap.SimpleEntry<>(latestMessageUs, latestMessageThem));
                    }
                }

                if (!channelsByActivity.isEmpty()) {

                    Collections.sort(channelsByActivity, (o1, o2) -> Long.compare(o2.getValue(), o1.getValue()));

                    String name = category.getName().toLowerCase().replaceAll("interview-", "");

                    response.append("**" + name + "**:\n");
                    for (Map.Entry<IAChannel, Long> channelInfo : channelsByActivity) {
                        IAChannel iaChan = channelInfo.getKey();
                        GuildMessageChannel channel = iaChan.getChannel();

                        DBNation nation = iaChan.getNation();


                        response.append(channel.getAsMention() + " " + "infra:" + nation.getInfra() + " land:" + nation.getLand() + " WarIndex:" + nation.getWarIndex() + " off:" + nation.getOff() + ", " + TimeUtil.secToTime(TimeUnit.MINUTES, nation.active_m()));
                        response.append("\n");

                        Map.Entry<Message, Message> messages = latestMsgs.get(iaChan);
                        List<Message> msgsSorted = new ArrayList<>(2);
                        if (messages.getKey() != null) msgsSorted.add(messages.getKey());
                        if (messages.getValue() != null) msgsSorted.add(messages.getValue());
                        if (msgsSorted.size() == 2 && msgsSorted.get(1).getTimeCreated().toEpochSecond() < msgsSorted.get(0).getTimeCreated().toEpochSecond()) {
                            Collections.reverse(msgsSorted);
                        }

                        if (!msgsSorted.isEmpty()) {
                            for (Message message : msgsSorted) {
                                String msgTrimmed = DiscordUtil.trimContent(message.getContentRaw());
                                if (msgTrimmed.length() > 100) msgTrimmed = msgTrimmed.substring(0, 100) + "...";
                                long epoch = message.getTimeCreated().toEpochSecond() * 1000L;
                                long roundTo = TimeUnit.HOURS.toMillis(1);
                                long diffRounded = System.currentTimeMillis() - epoch;
                                diffRounded = (diffRounded / roundTo) * roundTo;

                                String timeStr = TimeUtil.secToTime(TimeUnit.MILLISECONDS, diffRounded);

                                response.append("- [" + timeStr + "] **" + message.getAuthor().getName() + "**: `" + msgTrimmed + "`");
                                response.append("\n");
                            }

                        }
                    }
                }
            }
            if (response.length() == 0) return "No results found";
            return response.toString() + "\n" + author.getAsMention();
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }


    @Command(desc = "Set the interview message")
    @RolePermission(Roles.INTERNAL_AFFAIRS)
    public String setInterview(@Me GuildDB db, IACategory category, String message) {
        db.setCopyPasta("interview", message.replace("\\n", "\n"));
        return "Set `interview` to:\n```md\n" + message + "```\n\nUse " + CM.interview.questions.view.cmd.toSlashMention() + " to view";
    }

    @Command(desc = "View the interview message")
    @RolePermission(value = {Roles.INTERNAL_AFFAIRS, Roles.INTERNAL_AFFAIRS_STAFF}, any = true)
    public String viewInterview(@Me GuildDB db, IACategory category) {
        String message = db.getCopyPasta("interview", true);
        if (message == null) {
            return "No message set. Set one with " + ""
                    // TODO FIXME :||remove
//                    +  CM.interview.questions.set.cmd.toSlashMention()
                    ;
        }
        return "Interview questions:\n" + message + "";
    }
}
package link.locutus.discord.commands.manager.v2.impl.pw.commands;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import link.locutus.discord.Locutus;
import link.locutus.discord.api.ApiKeyPool;
import link.locutus.discord.api.endpoints.DnsApi;
import link.locutus.discord.api.types.DepositType;
import link.locutus.discord.commands.manager.v2.binding.ValueStore;
import link.locutus.discord.commands.manager.v2.binding.annotation.*;
import link.locutus.discord.commands.manager.v2.command.CommandBehavior;
import link.locutus.discord.commands.manager.v2.command.IMessageBuilder;
import link.locutus.discord.commands.manager.v2.command.IMessageIO;
import link.locutus.discord.commands.manager.v2.impl.discord.DiscordChannelIO;
import link.locutus.discord.commands.manager.v2.impl.discord.DiscordHookIO;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.*;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.commands.manager.v2.impl.pw.filter.NationPlaceholders;
import link.locutus.discord.commands.manager.v2.builder.NumericGroupRankBuilder;
import link.locutus.discord.commands.manager.v2.builder.RankBuilder;
import link.locutus.discord.commands.manager.v2.builder.SummedMapRankBuilder;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.DiscordDB;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.AddBalanceBuilder;
import link.locutus.discord.db.entities.AllianceChange;
import link.locutus.discord.db.entities.Coalition;
import link.locutus.discord.db.entities.DBWar;
import link.locutus.discord.db.entities.NationMeta;
import link.locutus.discord.api.types.tx.Transaction2;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.db.entities.WarParser;
import link.locutus.discord.db.guild.GuildKey;
import link.locutus.discord.db.guild.SheetKey;
import link.locutus.discord.gpt.GPTUtil;
import link.locutus.discord.pnw.AllianceList;
import link.locutus.discord.pnw.NationList;
import link.locutus.discord.pnw.NationOrAlliance;
import link.locutus.discord.pnw.NationOrAllianceOrGuild;
import link.locutus.discord.pnw.RegisteredUser;
import link.locutus.discord.pnw.SimpleNationList;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.*;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.discord.DiscordUtil;
import link.locutus.discord.util.math.ArrayUtil;
import link.locutus.discord.util.offshore.Auth;
import link.locutus.discord.util.offshore.test.IACategory;
import link.locutus.discord.util.offshore.test.IAChannel;
import link.locutus.discord.util.sheet.SpreadSheet;
import link.locutus.discord.api.types.Rank;
import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.util.sheet.templates.TransferSheet;
import link.locutus.discord.util.task.ia.IACheckup;
import link.locutus.discord.util.task.roles.IAutoRoleTask;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.DefaultGuildChannelUnion;
import net.dv8tion.jda.api.requests.restaction.InviteAction;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class UnsortedCommands {

    @Command(desc = "Get an alert on discord when a target logs in within the next 5 days\n" +
            "Useful if you want to know when they might defeat you in war or perform an attack")
    public String loginNotifier(@Me User user, @Me DBNation nation, DBNation target, @Switch("w") boolean doNotRequireWar) {
        // ensure nation is fighting target
        boolean isFighting = false;
        for (DBWar war : target.getActiveWars()) {
            if (war.getAttacker_id() == nation.getId() || war.getDefender_id() == nation.getId()) {
                isFighting = true;
                break;
            }
        }
        if (!isFighting && !doNotRequireWar) {
            return "You are not fighting " + target.getName() + "! Add `doNotRequireWar:True` to ignore this check.";
        }
        synchronized (target) {
            Map<Long, Long> existingMap = target.getLoginNotifyMap();
            if (existingMap == null) existingMap = new LinkedHashMap<>();
            existingMap.put(user.getIdLong(), System.currentTimeMillis());
            target.setLoginNotifyMap(existingMap);
        }
        return "You will be notified when " + target.getName() + " logs in (within the next 5d).";
    }

    @Command(desc ="View the resources in a nation or alliance")
    @RolePermission(Roles.MEMBER)
    @IsAlliance
    public String stockpile(@Me IMessageIO channel, @Me Guild guild, @Me GuildDB db, @Me DBNation me, @Me User author, NationOrAlliance nationOrAlliance) throws IOException {
        Map<ResourceType, Double> totals;

        long cutoff = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1);
        if (nationOrAlliance.isAlliance()) {
            DBAlliance alliance = nationOrAlliance.asAlliance();
            GuildDB otherDb = alliance.getGuildDB();
            if (otherDb == null) return "No guild found for " + alliance;
            if (!Roles.ECON_STAFF.has(author, otherDb.getGuild())) {
                return "You do not have " + Roles.ECON_STAFF + " in " + otherDb;
            }
            totals = alliance.getStockpile(cutoff);
            if (totals == null) {
                return "No stockpile found for " + alliance.getMarkdownUrl() + ". Ensure the api key is set correctly: "
                        + CM.settings.info.cmd.key(GuildKey.API_KEY.name())
                ;
            }
        } else {
            DBNation nation = nationOrAlliance.asNation();
            if (nation.getId() != me.getId()) {
                boolean noPerm = false;
                if (!Roles.ECON.has(author, guild) && !Roles.MILCOM.has(author, guild) && !Roles.INTERNAL_AFFAIRS.has(author, guild) && !Roles.INTERNAL_AFFAIRS_STAFF.has(author, guild)) {
                    noPerm = true;
                } else if (!db.isAllianceId(nation.getAlliance_id())) {
                    noPerm = true;
                }
                if (noPerm) return "You do not have permission to check that account's stockpile!";
            }
            totals = nation.getStockpile(cutoff);
            if (totals == null) {
                return "No stockpile found for " + nation.getMarkdownUrl() + ". Have they disabled alliance information access?";
            }
        }

        String out = ResourceType.resourcesToFancyString(totals);
        channel.create().embed(nationOrAlliance.getName() + " stockpile", out).send();
        return null;
    }

    private void sendIO(StringBuilder out, String selfName, boolean isAlliance, Map<Integer, List<Transaction2>> transferMap, long timestamp, boolean inflow) {
        String URL_BASE = "" + Settings.INSTANCE.DNS_URL() + "/%s/%s";
        long days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - timestamp);
        for (Map.Entry<Integer, List<Transaction2>> entry : transferMap.entrySet()) {
            int id = entry.getKey();

            String typeOther = isAlliance ? "alliance" : "nation";
            String name = DNS.getName(id, isAlliance);
            String url = String.format(URL_BASE, typeOther, id);

            List<Transaction2> transfers = entry.getValue();
            String title = inflow ? name + " > " + selfName : selfName + " > " + name;
//            String followCmd = Settings.commandPrefix(true) + "inflows " + url + " " + timestamp;

            StringBuilder message = new StringBuilder();

            Map<ResourceType, Double> totals = new HashMap<>();
            for (Transaction2 transfer : transfers) {
//                int sign = transfer.getSender() == id ? -1 : 1;
                int sign = 1;

                double[] rss = transfer.resources.clone();
//                rss[0] = 0;
                totals = ResourceType.add(totals, ResourceType.resourcesToMap(rss));
//                totals.put(type, sign * transfer.getAmount() + totals.getOrDefault(type, 0d));
            }

            message.append(ResourceType.resourcesToString(totals));

//            String infoCmd = Settings.commandPrefix(true) + "pw-who " + url;
//            Message msg = PW.createEmbedCommand(channel, title, message.toString(), EMOJI_FOLLOW, followCmd, EMOJI_QUESTION, infoCmd);
            out.append(title + ": " + message).append("\n");
        }
    }

    @Command(desc="Set your api and bot key for the bot\n" +
            "Your API key can be found on the account page: <https://diplomacyandstrife.com/account/>")
    @Ephemeral
    public String addApiKey(@Me IMessageIO io, @Me DBNation me, DBNation nation, String apiKey) {
        if (me.getId() != nation.getId()) {
            throw new IllegalArgumentException("You can only set your own api key");
            }

        apiKey = apiKey.trim();
        // check if string is HEX (case insensitive)
        if (!apiKey.matches("[0-9a-fA-F]+")) {
            return "Invalid API key. Please use the API key found on the account page: <https://diplomacyandstrife.com/account/>";
        }
        try {
            IMessageBuilder msg = io.getMessage();
            if (msg != null) io.delete(msg.getId());
        } catch (Throwable ignore) {}
        ApiKeyPool.ApiKey key = new ApiKeyPool.ApiKey(nation.getId(), apiKey);
        DnsApi api = new DnsApi(ApiKeyPool.builder().addKey(key).build());
        api.nation(nation.getId());

        Locutus.imp().getDiscordDB().addApiKey(nation.getId(), apiKey);

        return "Set api key for " + nation.getMarkdownUrl();
    }

    @Command(desc="Login to allow the bot to run scripts through your account\n" +
            "(Avoid using this if possible)")
    @RankPermission(Rank.LEADER)
    @Ephemeral
    public static String login(@Me IMessageIO io, DiscordDB discordDB, @Me DBNation me,
                               @Arg("Your username (i.e. email) for Diplomacy and Strife")
                               String username,
                               String password) {
        IMessageBuilder msg = io.getMessage();
        try {
            if (msg != null) io.delete(msg.getId());
        } catch (Throwable ignore) {};
        DBAlliance alliance = me.getAlliance();
        Auth existingAuth = alliance.getAuth(true);;

        Auth auth = new Auth(me.getNation_id(), username, password);
        ApiKeyPool.ApiKey key = auth.fetchApiKey();

        discordDB.addApiKey(me.getNation_id(), key.getKey());
        discordDB.addUserPass2(me.getNation_id(), username, password);
        if (existingAuth != null) existingAuth.setValid(false);
        Auth myAuth = me.getAuth(true);
        if (myAuth != null) myAuth.setValid(false);

        return "Login successful.";
    }

    @Command(desc = "Remove your login details from the bot")
    public String logout(@Me DBNation me, @Me User author) {
        if (Locutus.imp().getDiscordDB().getUserPass2(author.getIdLong()) != null || (me != null && Locutus.imp().getDiscordDB().getUserPass2(me.getNation_id()) != null)) {
            Locutus.imp().getDiscordDB().logout(author.getIdLong());
            if (me != null) {
                Locutus.imp().getDiscordDB().logout(me.getNation_id());
                Auth cached = me.auth;
                if (cached != null) {
                    cached.setValid(false);
                }
                me.auth = null;
            }
            return "Logged out";
        }
        return "You are not logged in";
    }

    @Command(desc = "List all in-game alliance members")
    @IsAlliance
    public String listAllianceMembers(@Me IMessageIO channel, @Me JSONObject command, @Me GuildDB db, int page) {
        Set<DBNation> nations = db.getAllianceList().getNations();

        int perPage = 5;

        new RankBuilder<>(nations).adapt(new com.google.common.base.Function<DBNation, String>() {
            @Override
            public String apply(DBNation n) {
                RegisteredUser user = Locutus.imp().getDiscordDB().getUserFromNationId(n.getNation_id());
                String result = "**" + n.getNation() + "**:" +
                        "";

                String active;
                if (n.active_m() < TimeUnit.DAYS.toMinutes(1)) {
                    active = "daily";
                } else if (n.active_m() < TimeUnit.DAYS.toMinutes(7)) {
                    active = "weekly";
                } else {
                    active = "inactive";
                }
                String url = n.getUrl();
                String general = n.toMarkdown(false, true, false);
                String infra = n.toMarkdown(false, false, true);

                StringBuilder response = new StringBuilder();
                response.append(n.getNation()).append(" | ").append(n.getAllianceName()).append(" | ").append(active);
                if (user != null) {
                    response.append('\n').append(user.getDiscordName()).append(" | ").append("`<@!").append(user.getDiscordId()).append(">`");
                }
                response.append('\n').append(url);
                response.append('\n').append(general);
                response.append(infra);

                return response.toString();
            }
        }).page(page - 1, perPage).build(channel, command, getClass().getSimpleName());
        return null;
    }

    public enum ClearRolesEnum {
        UNUSED("Alliance name roles which have no members"),
        ALLIANCE("All alliance name roles"),
        DELETED_ALLIANCES("Alliance name roles with no valid in-game alliance"),
        INACTIVE_ALLIANCES("Alliance name roles with no active members"),
        NOT_ALLOW_LISTED("Alliance name roles not in the allow list (defined by settings:`" + GuildKey.AUTOROLE_ALLIANCES.name() + "," + GuildKey.AUTOROLE_TOP_X.name() + "` and coalition:`" + Coalition.MASKED_ALLIANCES.name() + "`"),

        NON_MEMBERS("Users who are not in the alliance in-game"),
        NON_ALLIES("Users who are not in the alliance, or the `allies` / `offshore` coalition in-game")

        ;

        private final String desc;

        ClearRolesEnum(String s) {
            this.desc = s;
        }

        @Override
        public String toString() {
            return name() + ": `" + desc + "`";
        }
    }

    public String clearAllianceRolesDesc() {
        StringBuilder resposne = new StringBuilder("Clear the bot managed alliance roles on discord\n");
        for (ClearRolesEnum value : ClearRolesEnum.values()) {
            resposne.append(value.toString()).append("\n");
        }
        return resposne.toString();
    }

    @Command(descMethod = "clearAllianceRolesDesc")
    @RolePermission(Roles.ADMIN)
    public String clearAllianceRoles(@Me IMessageIO io, @Me GuildDB db, @Me Guild guild,
                                     @Arg("What role types do you want to remove")
                                     ClearRolesEnum type) throws ExecutionException, InterruptedException {
        List<Future<?>> tasks = new ArrayList<>();
        try {
            switch (type) {
                case UNUSED: {
                    List<String> log = new ArrayList<>();
                    Map<Integer, Role> aaRoles = DiscordUtil.getAARoles(guild.getRoles());
                    for (Map.Entry<Integer, Role> entry : aaRoles.entrySet()) {
                        if (guild.getMembersWithRoles(entry.getValue()).isEmpty()) {
                            tasks.add(RateLimitUtil.queue(entry.getValue().delete()));
                            log.add("Removed " + entry.getValue().getName());
                        }
                    }
                    if (log.isEmpty()) {
                        return "No unused roles found!";
                    }
                    io.create().append("Cleared " + log.size() + " roles").file("role_changes.txt", StringMan.join(log, "\n")).send();
                    return null;
                }
                case NOT_ALLOW_LISTED: {
                    IAutoRoleTask task = db.getAutoRoleTask();
                    task.syncDB();
                    Function<Integer, Boolean> allowed = task.getAllowedAlliances();
                    Map<Integer, Role> aaRoles = DiscordUtil.getAARoles(guild.getRoles());
                    List<String> log = new ArrayList<>();
                    for (Map.Entry<Integer, Role> entry : aaRoles.entrySet()) {
                        if (!allowed.apply(entry.getKey())) {
                            tasks.add(RateLimitUtil.queue(entry.getValue().delete()));
                            log.add("Removed " + entry.getValue().getName());
                        }
                    }
                    if (log.isEmpty()) {
                        return "No roles found!";
                    }
                    io.create().append("Cleared " + log.size() + " roles").file("role_changes.txt", StringMan.join(log, "\n")).send();
                    return null;
                }
                case INACTIVE_ALLIANCES:
                case DELETED_ALLIANCES: {

                    Map<Integer, Role> aaRoles = DiscordUtil.getAARoles(guild.getRoles());
                    List<String> log = new ArrayList<>();
                    for (Map.Entry<Integer, Role> entry : aaRoles.entrySet()) {
                        int aaId = entry.getKey();
                        DBAlliance alliance = DBAlliance.get(aaId);
                        boolean active = alliance != null;
                        if (active && type == ClearRolesEnum.INACTIVE_ALLIANCES) {
                            active = !alliance.getNations(f -> f.getPositionEnum().id >= Rank.APPLICANT.id && f.isVacation() == false && f.active_m() < TimeUnit.DAYS.toMinutes(7)).isEmpty();
                        }
                        if (!active) {
                            tasks.add(RateLimitUtil.queue(entry.getValue().delete()));
                            String reason = alliance != null ? "Inactive" : "Deleted";
                            log.add("Removed " + entry.getValue().getName() + "(" + reason + ")");
                        }
                    }
                    if (log.isEmpty()) {
                        return "No roles found!";
                    }
                    io.create().append("Cleared " + log.size() + " roles").file("role_changes.txt", StringMan.join(log, "\n")).send();
                    return null;
                }
                case NON_ALLIES:
                case NON_MEMBERS: {
                    Map<Long, Role> roles = Roles.MEMBER.toRoleMap(db);
                    List<String> log = new ArrayList<>();
                    List<String> errors = new ArrayList<>();

                    Set<Integer> allIds = new HashSet<>(db.getAllianceIds());
                    if (type == ClearRolesEnum.NON_ALLIES) {
                        allIds.addAll(db.getCoalition(Coalition.ALLIES));
                    }
                    Map<Role, Predicate<DBNation>> allowedRole = new HashMap<>();
                    Map<Role, Long> isAll = new HashMap<>();

                    for (Map.Entry<Long, Role> entry : roles.entrySet()) {
                        long aaId = entry.getKey();
                        Role role = entry.getValue();
                        if (aaId == 0) {
                            isAll.put(role, 0L);
                            allowedRole.put(role, f -> allIds.contains(f.getAlliance_id()));
                        } else {
                            isAll.put(role, aaId);
                            if (allIds.contains((int) aaId)) {
                                allowedRole.put(role, f -> f.getAlliance_id() == aaId);
                            } else {
                                String footer = type == ClearRolesEnum.NON_ALLIES ? " | " + CM.coalition.add.cmd.toSlashMention() + " with `" + Coalition.ALLIES.name() + "`" : "";
                                errors.add("Role " + role.getName() + " is bound to the alliance id: `" + aaId + "` which is not registered to this guild. See: " + CM.role.setAlias.cmd.toSlashMention() + " | " + CM.settings_default.registerAlliance.cmd.toSlashMention() + footer);
                            }
                        }
                    }

                    if (allowedRole.isEmpty()) {
                        return "No member roles found!";
                    }
                    for (Map.Entry<Role, Predicate<DBNation>> entry : allowedRole.entrySet()) {
                        Role role = entry.getKey();
                        Predicate<DBNation> predicate = entry.getValue();
                        for (Member member : guild.getMembersWithRoles(role)) {
                            DBNation nation = DiscordUtil.getNation(member.getIdLong());
                            String reason = null;
                            if (nation == null) {
                                reason = "Not registered to nation";
                            } else if (nation.getAlliance_id() == 0) {
                                reason = "Not in alliance";
                            } else if (nation.getPositionEnum() == Rank.APPLICANT) {
                                reason = "Applicant";
                            } else if (!predicate.test(nation)) {
                                if (isAll.get(role) == 0) {
                                    reason = "Not in alliance";
                                } else {
                                    reason = "Not in alliance id " + isAll.get(role);
                                }
                            }
                            if (reason != null) {
                                tasks.add(RateLimitUtil.queue(db.getGuild().removeRoleFromMember(member, role)));
                                log.add("Removed " + role.getName() + " from " + member.getEffectiveName() + "(" + reason + ")");
                            }
                        }
                    }
                    IMessageBuilder msg = io.create();
                    if (log.isEmpty()) {
                        msg.append("No roles found!");
                    } else {
                        msg.append("Cleared " + log.size() + " roles").file("role_changes.txt", StringMan.join(log, "\n"));
                        // add errors
                        if (!errors.isEmpty()) {
                            msg.append("\n\nErrors:\n- " + StringMan.join(errors, "\n- "));
                        }
                    }
                    msg.send();
                    return null;
                }
                case ALLIANCE: {
                    Map<Integer, Set<Role>> aaRoles = DiscordUtil.getAARolesIncDuplicates(guild.getRoles());
                    for (Map.Entry<Integer, Set<Role>> entry : aaRoles.entrySet()) {
                        for (Role role : entry.getValue()) {
                            tasks.add(RateLimitUtil.queue(role.delete()));
                        }
                    }
                    return "Cleared all AA roles!";
                }
                default:
                    throw new IllegalArgumentException("Unknown type " + type);
            }
        } finally {
            for (Future<?> task : tasks) {
                task.get();
            }
        }
    }

    private Map<Long, String> previous = new HashMap<>();
    private long previousNicksGuild = 0;

    @Command(desc = "Clear all nicknames on discord")
    @RolePermission(Roles.ADMIN)
    public synchronized String clearNicks(@Me Guild guild,
                                          @Arg("Undo the last recent use of this command")
                                          @Default Boolean undo) throws ExecutionException, InterruptedException {
        if (undo == null) undo = false;
        if (previousNicksGuild != guild.getIdLong()) {
            previousNicksGuild = guild.getIdLong();
            previous.clear();
        }
        int failed = 0;
        String msg = null;
        List<Future<?>> tasks = new ArrayList<>();
        for (Member member : guild.getMembers()) {
            if (member.getNickname() != null) {
                try {
                    String nick;
                    if (!undo) {
                        previous.put(member.getIdLong(), member.getNickname());
                        nick = null;
                    } else {
                        nick = previous.get(member.getIdLong());
//                        if (args.get(0).equalsIgnoreCase("*")) {
//                            nick = previous.get(member.getIdLong());
//                        } else {
//                            previous.put(member.getIdLong(), member.getNickname());
//                            nick = DiscordUtil.trimContent(event.getMessage().getContentRaw());
//                            nick = nick.substring(nick.indexOf(' ') + 1);
//                        }
                    }
                    tasks.add(RateLimitUtil.queue(member.modifyNickname(nick)));
                } catch (Throwable e) {
                    msg = e.getMessage();
                    failed++;
                }
            }
        }
        for (Future<?> task : tasks) {
            task.get();
        }
        if (failed != 0) {
            return "Failed to clear " + failed + " nicknames for reason: " + msg;
        }
        return "Cleared all nicknames (that I have permission to clear)!";
    }

    @Command(desc = "Add or subtract from a nation, alliance, guild or tax bracket's account balance\n" +
            "note: Mutated alliance deposits are only valid if your server is a bank/offshore\n" +
            "Use `#expire=30d` to have the amount expire after X days")
    @RolePermission(Roles.ECON)
    public String addBalance(@Me GuildDB db, @Me JSONObject command, @Me IMessageIO channel, @Me Guild guild, @Me User author, @Me DBNation me,
                             @AllowDeleted Set<NationOrAllianceOrGuild> accounts, Map<ResourceType, Double> amount, String note, @Switch("f") boolean force) throws Exception {
        if (note.equalsIgnoreCase("#ignore")) {
            channel.sendMessage("Note: Using `#ignore` will not affect the balance");
        }
        AddBalanceBuilder builder = db.addBalanceBuilder().add(accounts, amount, note);
        if (!force) {
            builder.buildWithConfirmation(channel, command);
            return null;
        }
        boolean hasEcon = Roles.ECON.has(author, guild);
        return builder.buildAndSend(me, hasEcon);
    }

    @Command(desc = "Add account balance using a google sheet of nation's and resource amounts\n" +
            "The google sheet must have a column for nation (name, id or url) and a column named for each in-game resource")
    @RolePermission(Roles.ECON)
    public String addBalanceSheet(@Me GuildDB db, @Me JSONObject command, @Me IMessageIO channel, @Me Guild guild, @Me User author, @Me DBNation me,
                                  SpreadSheet sheet, @Arg("The transaction note to use") String note, @Switch("f") boolean force,
                                  @Arg("Subtract the amounts instead of add") @Switch("n") boolean negative) throws Exception {
        List<String> errors = new ArrayList<>();
        AddBalanceBuilder builder = db.addBalanceBuilder().addSheet(sheet, negative, errors::add, !force, note);
        if (!force) {
            builder.buildWithConfirmation(channel, command);
            return null;
        }
        boolean hasEcon = Roles.ECON.has(author, guild);
        return builder.buildAndSend(me, hasEcon);
    }

    @Command(desc = "Get the revenue of nations or alliances\n" +
            "Equilibrium taxrate is where the value of raws consumed matches the value taxed")
    public String revenue(@Me GuildDB db, @Me IMessageIO channel, @Me DBNation me,
                          NationList nations,
//                          @Switch("c")
//                          @Arg("The amount of time to use to add average DAILY war cost\n" +
//                                  "This includes raid profit")
//                          @Timediff Long includeWarCosts,
                          @Switch("s") @Timestamp Long snapshotDate
                          ) throws Exception {
        Set<DBNation> nationSet = DNS.getNationsSnapshot(nations.getNations(), nations.getFilter(), snapshotDate, db.getGuild(), false);
        ArrayList<DBNation> filtered = new ArrayList<>(nationSet);
        int removed = 0;
        if (filtered.size() == 0) {
            if (removed > 0) {
                throw new IllegalArgumentException("No nations to tax, all " + removed + " nations are untaxable. Use `includeUntaxable` to include them");
            }
            throw new IllegalArgumentException("No nations provided");
        }
        Map<ResourceType, Double> revenue = new HashMap<>();
        if (snapshotDate == null) {
            for (DBNation nation : filtered) {
                if (nation.getAlliance_id() == 0) continue;
            }
        }
        for (DBNation nation : filtered) {
            ResourceType.add(revenue, nation.getRevenue());
        }

        double[] total = ResourceType.builder().add(revenue).build();

        IMessageBuilder response = channel.create();
        double equilibriumTaxrate = 100 * ResourceType.getEquilibrium(total);

        response.append("Daily revenue:")
                .append("```").append(ResourceType.resourcesToString(revenue)).append("```");

        response.append(String.format("Converted total: $" + MathMan.format(ResourceType.convertedTotal(revenue))));
        if (equilibriumTaxrate >= 0) {
            response.append("\nEquilibrium taxrate: `" + MathMan.format(equilibriumTaxrate) + "%`");
        } else {
            response.append("\n`warn: Revenue is not sustainable`");
        }

        if (removed > 0) {
            response.append("\n`warn: " + removed + " untaxable nations removed. Use 'includeUntaxable: True' to include them.`");
        }

        response.send();
        return null;
    }

    @Command(desc = "List the alliance rank changes of a nation or alliance members")
    public static String leftAA(@Me IMessageIO io,
                         NationOrAlliance nationOrAlliance,
                         @Arg("Date to start from")
                         @Default @Timestamp Long time,
                         @Arg("Only include these nations")
                         @Default NationList filter,
                         @Arg("Ignore inactive nations (7 days)")
                         @Switch("a") boolean ignoreInactives,
                         @Arg("Ignore nations in vacation mode")
                         @Switch("v") boolean ignoreVM,
                         @Arg("Ignore nations currently a member of an alliance")
                         @Switch("m") boolean ignoreMembers,
                         @Arg("Attach a list of all nation ids found")
                         @Switch("i") boolean listIds,
                         @Switch("s") SpreadSheet sheet) throws Exception {
        List<AllianceChange> removes;
        Predicate<AllianceChange> isAllowed = f -> true;
        if (filter != null) {
            Set<Integer> nationIds = filter.getNations().stream().map(DBNation::getNation_id).collect(Collectors.toSet());
            isAllowed = f -> nationIds.contains(f.getNationId());
        }
        if (ignoreInactives || ignoreVM || ignoreMembers) {
            Predicate<AllianceChange> finalIsAllowed = isAllowed;
            isAllowed = f -> {
                DBNation nation = Locutus.imp().getNationDB().getNation(f.getNationId());
                if (nation == null) return false;
                if (ignoreInactives && nation.active_m() > 10080) return false;
                if (ignoreVM && nation.isVacation()) return false;
                if (ignoreMembers && nation.getPosition() > 1) return false;
                return finalIsAllowed.test(f);
            };
        }

        if (nationOrAlliance.isNation()) {
            DBNation nation = nationOrAlliance.asNation();
            removes = nation.getAllianceHistory(time);
        } else {
            DBAlliance alliance = nationOrAlliance.asAlliance();
            removes = time != null ? alliance.getRankChanges(time) : alliance.getRankChanges();

            if (removes.isEmpty()) return "No history found";
        }
        int size = removes.size();
        Predicate<AllianceChange> finalIsAllowed1 = isAllowed;
        removes.removeIf(f -> !finalIsAllowed1.test(f));

        if (removes.isEmpty()) {
            String msg = "No history found in the specified timeframe.";
            if (size > 0) {
                msg += " (" + size + " entries removed by filters)";
            }
            throw new IllegalArgumentException(msg);
        }

        IMessageBuilder msg = io.create();

        if (sheet != null) {
            List<String> header = new ArrayList<>(Arrays.asList(
                    "nation",
                    "from_aa",
                    "from_rank",
                    "to_aa",
                    "to_rank",
                    "date"
                    ));
            sheet.setHeader(header);

            for (AllianceChange r : removes) {
                header.set(0, MarkupUtil.sheetUrl(DNS.getName(r.getNationId(), false), DNS.getNationUrl(r.getNationId())));
                header.set(1, MarkupUtil.sheetUrl(DNS.getName(r.getFromId(), true), DNS.getAllianceUrl(r.getFromId())));
                header.set(2, r.getFromRank().name());
                header.set(3, MarkupUtil.sheetUrl(DNS.getName(r.getToId(), true), DNS.getAllianceUrl(r.getToId())));
                header.set(4, r.getToRank().name());
                header.set(5, TimeUtil.YYYY_MM_DD_HH_MM_SS.format(new Date(r.getDate())));
                sheet.addRow(header);
            }

            sheet.updateClearCurrentTab();
            sheet.updateWrite();

            sheet.attach(msg, "departures");
        } else {
            long now = System.currentTimeMillis();
            StringBuilder response = new StringBuilder("Time\tNation\tFrom AA\tRank\tTo AA\tRank\n");
            for (AllianceChange r : removes) {
                long diff = now - r.getDate();
                String natStr = DNS.getMarkdownUrl(r.getNationId(), false);
                String fromStr;
                String toStr;
                if (r.getFromId() != 0) {
                    fromStr = DNS.getMarkdownUrl(r.getFromId(), true) + "\t" + r.getFromRank().name();
                } else {
                    fromStr = "0\tNONE";
                }
                if (r.getToId() != 0) {
                    toStr = DNS.getMarkdownUrl(r.getToId(), true) + "\t" + r.getToRank().name();
                } else {
                    toStr = "0\tNONE";
                }
                String diffStr = TimeUtil.secToTime(TimeUnit.MILLISECONDS, diff);
                response.append(diffStr + "\t" + natStr + "\t" + fromStr + "\t" + toStr + "\n");
            }
            if (response.length() > 2000) {
                msg.file("history.txt", response.toString());
                msg.append("See attached `history.txt`");
            } else {
                msg.append("```csv\n" + response.toString() + "\n```");
            }
        }
        if (listIds) {
            Set<Integer> ids = new LinkedHashSet<>();
            for (AllianceChange r : removes) {
                ids.add(r.getNationId());
            }
            msg.file("ids.txt",  StringMan.join(ids, ","));
        }
        msg.send();
        return null;
    }

    @Command(desc = "Save or paste a stored message")
    @RolePermission(Roles.MEMBER)
    @NoFormat
    public String copyPasta(@Me IMessageIO io, @Me GuildDB db, @Me Guild guild, @Me Member member, @Me User author, @Me DBNation me,
                            @Arg("What to name the saved message")
                            @Default String key,
                            @Default @TextArea String message,
                            @Arg("Require roles to paste the message")
                            @Default Set<Role> requiredRolesAny,
                            @Switch("n") DBNation formatNation,
                            NationPlaceholders placeholders, ValueStore store) throws Exception {
        if (formatNation == null) formatNation = me;
        if (key == null) {

            Map<String, String> copyPastas = db.getCopyPastas(member);
            Set<String> options = copyPastas.keySet().stream().map(f -> f.split("\\.")[0]).collect(Collectors.toSet());

            if (options.size() <= 25) {
                // buttons
                IMessageBuilder msg = io.create().append("Options:");
                for (String option : options) {
                    msg.commandButton(CommandBehavior.DELETE_MESSAGE, CM.copyPasta.cmd.key(option), option);
                }
                msg.send();
                return null;
            }

            // link modals
            return "Options:\n- " + StringMan.join(options, "\n- ");

            // return options
        }
        if (requiredRolesAny != null && !requiredRolesAny.isEmpty()) {
            if (message == null) {
                throw new IllegalArgumentException("requiredRoles can only be used with a message");
            }
            key = requiredRolesAny.stream().map(Role::getId).collect(Collectors.joining(".")) + key;
        }

        if (message == null) {
            Set<String> noRoles = db.getMissingCopypastaPerms(key, member);
            if (!noRoles.isEmpty()) {
                throw new IllegalArgumentException("You do not have the required roles to use this command: `" + StringMan.join(noRoles, ",") + "`");
            }

            String value = db.getCopyPasta(key, true);

            Set<String> missingRoles = null;
            if (value == null) {
                Map<String, String> map = db.getCopyPastas(member);
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    String otherKey = entry.getKey();
                    String[] split = otherKey.split("\\.");
                    if (!split[split.length - 1].equalsIgnoreCase(key)) continue;

                    Set<String> localMissing = db.getMissingCopypastaPerms(otherKey, guild.getMember(author));

                    if (!localMissing.isEmpty()) {
                        missingRoles = localMissing;
                        continue;
                    }

                    value = entry.getValue();
                    missingRoles = null;
                }
            } else {
                missingRoles = db.getMissingCopypastaPerms(key, guild.getMember(author));
            }
            if (missingRoles != null && !missingRoles.isEmpty()) {
                throw new IllegalArgumentException("You do not have the required roles to use this command: `" + StringMan.join(missingRoles, ",") + "`");
            }
            if (value == null) return "No message set for `" + key + "`. Plase use " + CM.copyPasta.cmd.toSlashMention() + "";

            value = placeholders.format2(store, value, formatNation, false);

            return value;
        } else if (!Roles.INTERNAL_AFFAIRS.has(author, guild)) {
            return "Missing role: " + Roles.INTERNAL_AFFAIRS;
        }

        if (!Roles.INTERNAL_AFFAIRS.has(author, guild)) return "No permission.";

        String setKey = key;
        if (requiredRolesAny != null && !requiredRolesAny.isEmpty()) {
            setKey = requiredRolesAny.stream().map(Role::getId).collect(Collectors.joining(".")) + "." + key;
        }

        if (message.isEmpty() || message.equalsIgnoreCase("null")) {
            db.deleteCopyPasta(key);
            db.deleteCopyPasta(setKey);
            return "Deleted message for "
                    + CM.copyPasta.cmd.key(key).toString()
                    ;
        } else {
            db.setCopyPasta(setKey, message);
            return "Added message for "
                     + CM.copyPasta.cmd.key(setKey).toString()
                    + "\n" +
                    "Remove using "
                     + CM.copyPasta.cmd.key(key).message("null").toString()
                    ;
        }


    }

    @Command(desc = "Generate an audit report of a list of nations")
    @RolePermission(Roles.MEMBER)
    @IsAlliance
    public String checkCities(@Me GuildDB db, @Me IMessageIO channel, @Me DBNation me,
                              @Arg("Nations to audit")
                              NationList nationList,
                              @Arg("Only perform these audits (default: all)")
                              @Default Set<IACheckup.AuditType> audits,
                              @Arg("Ping the user on discord with their audit")
                              @Switch("u") boolean pingUser,
                              @Arg("Post the audit in the interview channels (if exists)")
                              @Switch("c") boolean postInInterviewChannels,
                              @Arg("Skip updating nation info from the game")
                              @Switch("s") boolean skipUpdate) throws Exception {
        Collection<DBNation> nations = nationList.getNations();
        Set<Integer> aaIds = nationList.getAllianceIds();

        if (nations.size() > 1) {
            IACategory category = db.getIACategory();
            if (category != null) {
                category.load();
                category.purgeUnusedChannels(channel);
                category.alertInvalidChannels(channel);
            }
        }

        for (DBNation nation : nations) {
            if (!db.isAllianceId(nation.getAlliance_id())) {
                return "Nation `" + nation.getName() + "` is in " + nation.getAlliance().getQualifiedId() + " but this server is registered to: "
                        + StringMan.getString(db.getAllianceIds()) + "\nSee: " + CM.settings.info.cmd.toSlashMention() + " with key `" + GuildKey.ALLIANCE_ID.name() + "`";
            }
        }

        me.setMeta(NationMeta.INTERVIEW_CHECKUP, (byte) 1);

        IACheckup checkup = new IACheckup(db, db.getAllianceList().subList(aaIds), false);

        CompletableFuture<IMessageBuilder> msg = channel.send("Please wait...");

        Map<DBNation, Map<IACheckup.AuditType, Map.Entry<Object, String>>> auditResults = new HashMap<>();

        IACheckup.AuditType[] allowed = audits == null || audits.isEmpty() ? IACheckup.AuditType.values() : audits.toArray(new IACheckup.AuditType[0]);
        for (DBNation nation : nations) {
            StringBuilder output = new StringBuilder();
            int failed = 0;

            Map<IACheckup.AuditType, Map.Entry<Object, String>> auditResult = checkup.checkup(nation, allowed, nations.size() == 1, skipUpdate);
            auditResults.put(nation, auditResult);

            if (auditResult != null) {
                auditResult = IACheckup.simplify(auditResult);
            }

            if (!auditResult.isEmpty()) {
                for (Map.Entry<IACheckup.AuditType, Map.Entry<Object, String>> entry : auditResult.entrySet()) {
                    IACheckup.AuditType type = entry.getKey();
                    Map.Entry<Object, String> info = entry.getValue();
                    if (info == null || info.getValue() == null) continue;
                    failed++;

                    output.append("**").append(type.toString()).append(":** ");
                    output.append(info.getValue()).append("\n\n");
                }
            }
            IMessageBuilder resultMsg = channel.create();
            if (failed > 0) {
                resultMsg.append("**").append(nation.getName()).append("** failed ").append(failed + "").append(" checks:");
                if (pingUser) {
                    User user = nation.getUser();
                    if (user != null) resultMsg.append(user.getAsMention());
                }
                resultMsg.append("\n");
                resultMsg.append(output.toString());
            } else {
                resultMsg.append("All checks passed for " + nation.getNation());
            }
            resultMsg.send();
        }

        if (postInInterviewChannels) {
            if (db.getGuild().getCategoriesByName("interview", true).isEmpty()) {
                return "No `interview` category";
            }

            IACategory category = db.getIACategory();
            if (category.isValid()) {
                category.update(auditResults);
            }
        }

        return null;
    }

    @Command(desc = "Check if a nation is a reroll and print their reroll date")
    public static String reroll(@Me IMessageIO io, DBNation nation) {
        long date = nation.getRerollDate();
        if (date == Long.MAX_VALUE) {
            return "No direct reroll found.";
        }
        String title = "`" + nation.getNation() + "` is a reroll";
        StringBuilder body = new StringBuilder();
        body.append("Reroll date: " + DiscordUtil.timestamp(nation.getDate(), "d") + "\n");
        body.append("Original creation date: " + DiscordUtil.timestamp(date, "d"));
        io.create().embed(title, body.toString()).send();
        return null;
    }

    @Command(desc = "Run audits on member nations and generate a google sheet of the results")
    @IsAlliance
    @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MENTOR, Roles.INTERVIEWER}, any = true)
    public void auditSheet(@Me GuildDB db,
                             @Me IMessageIO io,
                             @Arg("The nations to audit\n" +
                                     "Must be in your alliance") @Default Set<DBNation> nations,
                             @Arg("The audits to include in the sheet\n" +
                                     "Defaults to all audits")@Switch("i") Set<IACheckup.AuditType> includeAudits,
                             @Arg("The audits to exclude from the sheet\n" +
                                     "Defaults to none") @Switch("e") Set<IACheckup.AuditType> excludeAudits,
                             @Arg("Update nation information before running the audit\n" +
                                     "Otherwise the audit will be run on the last fetched info")@Switch("u") boolean forceUpdate,
                             @Arg("Include full descriptions in the audit sheet results\n" +
                                     "Otherwise only raw data will be included")@Switch("v") boolean verbose,
                             @Switch("s") SpreadSheet sheet) throws IOException, ExecutionException, InterruptedException, GeneralSecurityException {
        if (includeAudits == null) {
            includeAudits = new HashSet<>();
            for (IACheckup.AuditType value : IACheckup.AuditType.values()) {
                if (excludeAudits == null || !excludeAudits.contains(value)) {
                    includeAudits.add(value);
                }
            }
        }

        if (nations == null) {
            nations = db.getAllianceList().getNations(true, 0, true);
        }
        AtomicInteger notAllianceRemoved = new AtomicInteger();
        AtomicInteger appRemoved = new AtomicInteger();
        AtomicInteger vmRemoved = new AtomicInteger();
        nations.removeIf(f -> {
            if (!db.isAllianceId(f.getAlliance_id())) {
                notAllianceRemoved.incrementAndGet();
                return true;
            }
            if (f.getPositionEnum().id < Rank.MEMBER.id) {
                appRemoved.incrementAndGet();
                return true;
            }
            if (f.isVacation()) {
                vmRemoved.incrementAndGet();
                return true;
            }
            return false;
        });

        if (nations.isEmpty()) {
            StringBuilder msg = new StringBuilder("No nations found");
            if (notAllianceRemoved.get() > 0) {
                msg.append("\n- Removed " + notAllianceRemoved.get() + " nations were not in the alliance");
            }
            if (appRemoved.get() > 0) {
                msg.append("\n- Removed " + appRemoved.get() + " nations were not members");
            }
            if (vmRemoved.get() > 0) {
                msg.append("\n- Removed " + vmRemoved.get() + " nations were in vacation mode");
            }
            throw new IllegalArgumentException(msg.toString());
        }
        IACheckup checkup = new IACheckup(db, db.getAllianceList().subList(nations), false);
        IACheckup.AuditType[] audits = includeAudits.toArray(new IACheckup.AuditType[0]);
        Map<DBNation, Map<IACheckup.AuditType, Map.Entry<Object, String>>> auditResults = checkup.checkup(nations, null, audits, !forceUpdate);

        if (sheet == null) {
            sheet = SpreadSheet.create(db, SheetKey.IA_SHEET);
        }

        List<String> header = new ArrayList<>(Arrays.asList(
                "nation",
                "interview",
                "username",
                "active_m",
                "land",
                "infra",
                "off"
        ));
        for (IACheckup.AuditType type : audits) {
            header.add(type.name().toLowerCase(Locale.ROOT));
        }
        sheet.setHeader(header);

        IACategory iaCat = db.getIACategory();

        Map<IACheckup.AuditType, Integer> failedCount = new LinkedHashMap<>();
        Map<IACheckup.AuditSeverity, Integer> failedBySeverity = new LinkedHashMap<>();
        Map<IACheckup.AuditSeverity, Integer> nationBySeverity = new LinkedHashMap<>();

        for (Map.Entry<DBNation, Map<IACheckup.AuditType, Map.Entry<Object, String>>> entry : auditResults.entrySet()) {
            DBNation nation = entry.getKey();
            header.set(0, nation.getSheetUrl());

            header.set(1, "");
            if (iaCat != null) {
                IAChannel channel = iaCat.get(nation);
                if (channel != null) {
                    TextChannel text = channel.getChannel();
                    if (text != null) {
                        header.set(1, MarkupUtil.sheetUrl(text.getName(), text.getJumpUrl()));
                    }
                }
            }

            User user = nation.getUser();
            header.set(2, "");
            if (user != null) {
                String url = DiscordUtil.userUrl(user.getIdLong(), false);
                String name = DiscordUtil.getFullUsername(user);
                header.set(2, MarkupUtil.sheetUrl(name, url));
            }

            header.set(3, nation.active_m() + "");
            header.set(4, nation.getLand() + "");
            header.set(5, MathMan.format(nation.getInfra()));
            header.set(6, MathMan.format(nation.getOff()));

            Map<IACheckup.AuditType, Map.Entry<Object, String>> auditMap = entry.getValue();
            IACheckup.AuditSeverity highest = null;

            int i = 7;
            for (IACheckup.AuditType audit : audits) {
                Map.Entry<Object, String> value = auditMap.get(audit);
                if (value == null || value.getValue() == null) {
                    header.set(i, "");
                } else {
                    if (highest == null || highest.ordinal() < audit.severity.ordinal()) {
                        highest = audit.severity;
                    }
                    // use merge
                    failedCount.merge(audit, 1, Integer::sum);
                    failedBySeverity.merge(audit.severity, 1, Integer::sum);
                    String valueStr = verbose ? value.getValue() : StringMan.getString(value.getKey());
                    String escaped = "=\"" + valueStr.replace("\"", "\"\"") + "\"";
                    header.set(i, escaped);
                }
                i++;
            }
            if (highest != null) {
                nationBySeverity.merge(highest, 1, Integer::sum);
            }

            sheet.addRow(header);
        }
        sheet.updateClearCurrentTab();
        sheet.updateWrite();
        IMessageBuilder msg = sheet.attach(io.create(), "audit");

        // sum nationBySeverity
        int auditsTotal = nations.size() * audits.length;
        int auditsPassed = auditsTotal - failedBySeverity.values().stream().mapToInt(Integer::intValue).sum();
        int nationsTotal = nations.size();
        int nationsPassedAll = nationsTotal - nationBySeverity.values().stream().mapToInt(Integer::intValue).sum();

        msg.append("\n## Summary\n")
                        .append("- `" + auditsPassed + "/" + auditsTotal + "` audits passed\n")
                        .append("- `" + nationsPassedAll + "/" + nationsTotal + "` nations passed ALL audits\n");
        if (!failedCount.isEmpty()) {
            failedCount = ArrayUtil.sortMap(failedCount, false);
            msg.append("## By Type\n- `" + StringMan.getString(failedCount) + "`\n");
        }
        if (!failedBySeverity.isEmpty()) {
            failedBySeverity = ArrayUtil.sortMap(failedBySeverity, false);
            msg.append("## \\# Audits By Severity\n- `" + StringMan.getString(failedBySeverity) + "`\n");
        }
        if (!nationBySeverity.isEmpty()) {
            nationBySeverity = ArrayUtil.sortMap(nationBySeverity, false);
            msg.append("## \\# Nations By Severity\n- `" + StringMan.getString(nationBySeverity) + "`\n");
        }
        msg.send();
    }

    @Command(desc = "Create, send and record unique invites to a set of nations\n" +
            "The invite can be sent via discord direct message, mail, viewed from an embed, or command\n" +
            "If `allowCreation` is not enabled, only a single invite will be created per nation; invites may expire and no new invites are permitted.")
    @RolePermission(Roles.ADMIN)
    public String sendInvite(@Me GuildDB db,
                             @Me User author,
                             @Me DBNation me,
                             @Me JSONObject command,
                             @Me IMessageIO currentChannel,
                             String message,
                             Guild inviteTo,
                             @Default NationList sendTo,
                             @Switch("e") @Timediff Long expire,
                             @Switch("u") Integer maxUsesEach,
                             @Arg("Send the invite via discord direct message") @Switch("d") boolean sendDM,
                             @Arg("Allow creating an invite when any nation matches `sendTo`, when they don't already have an invite, or theirs has expired\n" +
                                     "Invites can be created by using viewing the announcement embed or running the announcement view command\n" +
                                     "Defaults to false")
                             @Switch("c") boolean allowCreation,
                             @Switch("f") boolean force) throws IOException {
        DefaultGuildChannelUnion defaultChannel = inviteTo.getDefaultChannel();
        if (defaultChannel == null) {
            throw new IllegalArgumentException("No default channel found for " + inviteTo + ". Please set one in the server settings.");
        }
        if (sendTo == null) {
            SimpleNationList tmp = new SimpleNationList(Collections.singleton(me));
            tmp.setFilter(me.getQualifiedId());
            sendTo = tmp;
        }
        if (sendDM && !Roles.MAIL.hasOnRoot(author)) {
            throw new IllegalArgumentException("You do not have permission to send direct mesages");
        }
        // ensure admin on inviteTo guild
        Member member = inviteTo.getMember(author);
        if (member == null) {
            throw new IllegalArgumentException("You are not a member of " + inviteTo);
        }
        if (!member.hasPermission(Permission.CREATE_INSTANT_INVITE) && !Roles.ADMIN.has(author, inviteTo) && !Roles.INTERNAL_AFFAIRS.has(author, inviteTo)) {
            throw new IllegalArgumentException("You do not have permission to create invites in " + inviteTo);
        }
        if (message != null) {
            GPTUtil.checkThrowModeration(message);
        }

        // dm user instructions find_announcement
        Set<Integer> aaIds = db.getAllianceIds();

        List<String> errors = new ArrayList<>();
        Collection<DBNation> nations = sendTo.getNations();
        List<DBNation> nationsValid = new ArrayList<>();
        for (DBNation nation : nations) {
            User user = nation.getUser();
            if (user == null) {
                errors.add("Cannot find user for `" + nation.getNation() + "`");
                continue;
            } else if (db.getGuild().getMember(user) == null) {
                errors.add("Cannot find member in guild for `" + nation.getNation() + "` | `" + user.getName() + "`");
                continue;
            }
            if (user.getMutualGuilds().contains(inviteTo)) {
                errors.add("Already in the guild: `" + nation.getNation() + "`");
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
            nationsValid.add(nation);
        }

        if (!force) {
            StringBuilder confirmBody = new StringBuilder();
            if (!sendDM) confirmBody.append("**Warning: No direct message option has been specified**\n");
            confirmBody.append("Send DM (`-d`): " + sendDM).append("\n");
            if (!errors.isEmpty() && errors.size() < 15) {
                confirmBody.append("\n**Errors**:\n- " + StringMan.join(errors, "\n- ")).append("\n");
            }
            IMessageBuilder msg = currentChannel.create()
                    .confirmation("Send to " + nationsValid.size() + " nations", confirmBody.toString(), command);

            if (errors.size() >= 15) {
                msg = msg.file("errors.txt", StringMan.join(errors, "\n"));

            }

            msg.send();

            return null;
        }

        currentChannel.send("Please wait...");

        List<Integer> failedToDM = new ArrayList<>();
        List<Integer> failedToMail = new ArrayList<>();

        StringBuilder output = new StringBuilder();

        Map<DBNation, String> sentMessages = new HashMap<>();

        String subject = "Invite to: " + inviteTo.getName();

        String replacementInfo = inviteTo.getId();
        replacementInfo += "," + (expire == null ? 0 : expire);
        replacementInfo += "," + (maxUsesEach == null ? 0 : maxUsesEach);

        if (!allowCreation || sendDM) {
            for (DBNation nation : nationsValid) {
                InviteAction create = defaultChannel.createInvite().setUnique(true);
                if (expire != null) {
                    create = create.setMaxAge((int) (expire / 1000L));
                }
                if (maxUsesEach != null) {
                    create = create.setMaxUses(maxUsesEach);
                }
                Invite invite = RateLimitUtil.complete(create);

                String replaced = message + "\n" + invite.getUrl();
                String personal = replaced + "\n\n- " + author.getAsMention();

                boolean result = sendDM && nation.sendDM(personal);
                if (!result && sendDM) {
                    failedToDM.add(nation.getNation_id());
                }
                if (!result) {
                    failedToMail.add(nation.getNation_id());
                }

                sentMessages.put(nation, replaced);

                output.append("\n\n```" + replaced + "```" + "^ " + nation.getNation());
            }
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

        int annId = db.addAnnouncement(author, subject, message, replacementInfo, sendTo.getFilter(), allowCreation);
        for (Map.Entry<DBNation, String> entry : sentMessages.entrySet()) {
            byte[] diff = StringMan.getDiffBytes(message, entry.getValue());
            db.addPlayerAnnouncement(entry.getKey(), annId, diff);
        }

        MessageChannel channel;
        if (currentChannel instanceof DiscordHookIO hook) {
            channel = hook.getHook().getInteraction().getMessageChannel();
        } else if (currentChannel instanceof DiscordChannelIO channelIO) {
            channel = channelIO.getChannel();
        } else {
            channel = null;
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

            CM.announcement.view cmd = CM.announcement.view.cmd.ann_id(annId + "");
            msg
                    .commandButton(CommandBehavior.EPHEMERAL, cmd, "view")
                    .send();
        }

        return "Done. See " + CM.announcement.find.cmd.toSlashMention() + "\n" + author.getAsMention();
    }
}
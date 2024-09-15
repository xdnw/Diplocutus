package link.locutus.discord.commands.manager.v2.impl.pw.binding;

import link.locutus.discord.Locutus;
import link.locutus.discord.api.ApiKeyPool;
import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.api.generated.TreatyType;
import link.locutus.discord.api.generated.WarType;
import link.locutus.discord.api.types.*;
import link.locutus.discord.commands.WarCategory;
import link.locutus.discord.commands.manager.v2.binding.ValueStore;
import link.locutus.discord.commands.manager.v2.binding.annotation.*;
import link.locutus.discord.commands.manager.v2.binding.bindings.Placeholders;
import link.locutus.discord.commands.manager.v2.binding.bindings.PrimitiveBindings;
import link.locutus.discord.commands.manager.v2.command.CommandCallable;
import link.locutus.discord.commands.manager.v2.command.ICommand;
import link.locutus.discord.commands.manager.v2.command.ICommandGroup;
import link.locutus.discord.commands.manager.v2.command.IMessageIO;
import link.locutus.discord.commands.manager.v2.command.ParameterData;
import link.locutus.discord.commands.manager.v2.impl.discord.binding.DiscordBindings;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.RolePermission;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.commands.manager.v2.impl.pw.NationFilter;
import link.locutus.discord.commands.manager.v2.impl.pw.filter.AlliancePlaceholders;
import link.locutus.discord.db.conflict.Conflict;
import link.locutus.discord.db.conflict.ConflictManager;
import link.locutus.discord.commands.manager.v2.binding.BindingHelper;
import link.locutus.discord.commands.manager.v2.impl.discord.binding.annotation.GuildCoalition;
import link.locutus.discord.commands.manager.v2.impl.discord.binding.annotation.NationDepositLimit;
import link.locutus.discord.commands.manager.v2.binding.bindings.MathOperation;
import link.locutus.discord.commands.manager.v2.command.ParametricCallable;
import link.locutus.discord.commands.manager.v2.impl.pw.commands.UnsortedCommands;
import link.locutus.discord.commands.manager.v2.impl.pw.filter.NationPlaceholders;
import link.locutus.discord.db.*;
import link.locutus.discord.db.entities.*;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.conflict.ConflictCategory;
import link.locutus.discord.db.entities.metric.AllianceMetric;
import link.locutus.discord.db.entities.metric.AllianceMetricMode;
import link.locutus.discord.db.entities.metric.DnsMetric;
import link.locutus.discord.db.guild.GuildSetting;
import link.locutus.discord.db.guild.GuildKey;
import link.locutus.discord.db.guild.GuildSettingCategory;
import link.locutus.discord.pnw.AllianceList;
import link.locutus.discord.pnw.GuildOrAlliance;
import link.locutus.discord.pnw.NationList;
import link.locutus.discord.pnw.NationOrAlliance;
import link.locutus.discord.pnw.NationOrAllianceOrGuild;
import link.locutus.discord.pnw.SimpleNationList;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.AutoAuditType;
import link.locutus.discord.util.MathMan;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.StringMan;
import link.locutus.discord.util.discord.DiscordUtil;
import link.locutus.discord.util.offshore.Auth;
import link.locutus.discord.util.offshore.test.IACategory;
import link.locutus.discord.util.task.ia.IACheckup;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PWBindings extends BindingHelper {

    @Binding(value = "The name of a stored conflict between two coalitions")
    public static Conflict conflict(ConflictManager manager, String nameOrId) {
        Conflict conflict = manager.getConflict(nameOrId);
        if (conflict != null) {
            return conflict;
        }
        if (MathMan.isInteger(nameOrId)) {
            int id = PrimitiveBindings.Integer(nameOrId);
            conflict = manager.getConflictById(id);
            if (conflict != null) return conflict;
        }
        throw new IllegalArgumentException("Unknown conflict: `" + nameOrId + "`. Options: " + StringMan.getString(manager.getConflictNames()));
    }

    @Binding(value = "A war id or url", examples = {"https://diplomacyandstrife.com/war/1234"})
    public static DBWar war(String arg0) {
        if (arg0.contains("/war/")) {
            arg0 = arg0.split("/war/")[1];
        }
        if (!MathMan.isInteger(arg0)) {
            throw new IllegalArgumentException("Not a valid war number: `" + arg0 + "`");
        }
        int warId = Integer.parseInt(arg0);
        DBWar war = Locutus.imp().getWarDb().getWar(warId);
        if (war == null) throw new IllegalArgumentException("No war founds for id: `" + warId + "`");
        return war;
    }

    @Binding(value = "The name of a stored conflict between two coalitions")
    public Set<Conflict> conflicts(ConflictManager manager, ValueStore store, String input) {
        Set<Conflict> result = Locutus.cmd().getV2().getPlaceholders().get(Conflict.class).parseSet(store, input);
        if (result == null || result.isEmpty()) {
            throw new IllegalArgumentException("No conflicts found in: " + input + ". Options: " + manager.getConflictNames());
        }
        return result;
    }

    @Binding(value = "A treaty between two alliances\n" +
            "Link two alliances, separated by a colon")
    public static Treaty treaty(String input) {
        String[] split = input.split("[:><]");
        if (split.length != 2) throw new IllegalArgumentException("Invalid input: `" + input + "` - must be two alliances separated by a comma");
        DBAlliance aa1 = alliance(split[0].trim());
        DBAlliance aa2 = alliance(split[1].trim());
        Treaty treaty = aa1.getTreaties().get(aa2.getId());
        if (treaty == null) {
            throw new IllegalArgumentException("No treaty found between " + aa1.getName() + " and " + aa2.getName());
        }
        return treaty;
    }

    @Binding(value = "The name of a nation attribute\n" +
            "See: <https://github.com/xdnw/diplocutus/wiki/nation_placeholders>", examples = {"color", "war_policy", "continent"},
    webType = "CommandCallable<DBNation>")
    @NationAttributeCallable
    public ParametricCallable nationAttribute(NationPlaceholders placeholders, ValueStore store, String input) {
        List<ParametricCallable> options = placeholders.getParametricCallables();
        ParametricCallable metric = placeholders.get(input);
        if (metric == null) {
            throw new IllegalArgumentException("Invalid attribute: `" + input + "`. See: <https://github.com/xdnw/diplocutus/wiki/nation_placeholders>");
        }
        return metric;
    }

    @Binding(value = "A discord slash command reference for the bot", webType = "CommandCallable")
    public ICommand slashCommand(String input) {
        List<String> split = StringMan.split(input, ' ');
        CommandCallable command = Locutus.imp().getCommandManager().getV2().getCallable(split);
        if (command == null) throw new IllegalArgumentException("No command found for `" + input + "`");
        if (command instanceof ICommandGroup group) {
            String prefix = group.getFullPath();
            if (!prefix.isEmpty()) prefix += " ";
            String optionsStr = "- `" + prefix + String.join("`\n- `" + prefix, group.primarySubCommandIds()) + "`";
            throw new IllegalArgumentException("Command `" + input + "` is a group, not an endpoint. Please specify a sub command:\n" + optionsStr);
        }
        if (!(command instanceof ICommand)) throw new IllegalArgumentException("Command `" + input + "` is not a command endpoint");
        return (ICommand) command;
    }

    @Binding(value = "A comma separated list of beige reasons for defeating an enemy in war")
    public Set<DnsMetric> OrbisMetrics(String input) {
        return emumSet(DnsMetric.class, input);
    }


    @Binding(value = "A comma separated list of deposit types")
    public Set<DepositType> DepositTypes(String input) {
        return emumSet(DepositType.class, input);
    }

    @Binding(value = "The guild setting category")
    public GuildSettingCategory GuildSettingCategory(String input) {
        return emum(GuildSettingCategory.class, input);
    }


    @Binding(value = "The category for a conflict")
    public ConflictCategory ConflictCategory(String input) {
        return emum(ConflictCategory.class, input);
    }


    @Binding(value = "An alert mode for the ENEMY_ALERT_CHANNEL when enemies leave beige")
    public EnemyAlertChannelMode EnemyAlertChannelMode(String input) {
        return emum(EnemyAlertChannelMode.class, input);
    }

    @Binding(value = "A city building type")
    public static Building getBuilding(String input) {
        Building building = Building.parse(input);
        if (building == null) {
            throw new IllegalArgumentException("No building found for `" + input + "`\n" +
                    "Options: `" + Arrays.stream(Building.values()).map(Building::name).collect(Collectors.joining("`, `")) + "`");
        }
        return building;
    }

    @Binding("An string matching for a nation's military buildings (MMR)\n" +
            "In the form `505X` where `X` is any military building")
    public MMRMatcher mmrMatcher(String input) {
        return new MMRMatcher(input);
    }

    @Binding(value = """
            A map of nation filters to MMR
            Use X for any military building
            All nation filters are supported (e.g. roles)
            Priority is first to last (so put defaults at the bottom)""",
            examples = """
            #land<10:505X
            #land>=10:0250""")
    public Map<NationFilter, MMRMatcher> mmrMatcherMap(@Me GuildDB db, String input, @Default @Me User author, @Default @Me DBNation nation) {
        Map<NationFilter, MMRMatcher> filterToMMR = new LinkedHashMap<>();
        for (String line : input.split("\n")) {
            String[] split = line.split("[:]");
            if (split.length != 2) continue;

            String filterStr = split[0].trim();

            boolean containsNation = false;
            for (String arg : filterStr.split(",")) {
                if (!arg.startsWith("#")) containsNation = true;
                if (arg.contains("tax_id=")) containsNation = true;
                if (arg.startsWith("https://docs.google.com/spreadsheets/") || arg.startsWith("sheet:")) containsNation = true;
            }
            if (!containsNation) filterStr += ",*";
            NationFilterString filter = new NationFilterString(filterStr, db.getGuild(), author, nation);
            MMRMatcher mmr = new MMRMatcher(split[1]);
            filterToMMR.put(filter, mmr);
        }

        return filterToMMR;
    }

    @Binding(value = "Auto assign roles based on conditions\n" +
            "See: <https://github.com/xdnw/diplocutus/wiki/nation_placeholders>\n" +
            "Accepts a list of filters to a role.\n" +
            "In the form:\n" +
            "```\n" +
            "#land<10:@someRole\n" +
            "#land>=10:@otherRole\n" +
            "```\n" +
            "Use `*` as the filter to match all nations.\n" +
            "Only alliance members can be given role")
    public Map<NationFilter, Role> conditionalRole(@Me GuildDB db, String input, @Default @Me User author, @Default @Me DBNation nation) {
        Map<NationFilter, Role> filterToRole = new LinkedHashMap<>();
        for (String line : input.split("\n")) {
            int index = line.lastIndexOf(":");
            if (index == -1) {
                continue;
            }
            String part1 = line.substring(0, index);
            String part2 = line.substring(index + 1);
            String filterStr = part1.trim();
            boolean containsNation = false;
            NationFilterString filter = new NationFilterString(filterStr, db.getGuild(), author, nation);
            Role role = DiscordBindings.role(db.getGuild(), part2);
            filterToRole.put(filter, role);
        }
        return filterToRole;
    }

    @Binding(examples = ("#grant #city=1"), value = "A DepositType optionally with a value and a city tag\n" +
            "See: <https://github.com/xdnw/diplocutus/wiki/deposits#transfer-notes>",
    webType = "DepositType")
    public static DepositType.DepositTypeInfo DepositTypeInfo(String input) {
        DepositType type = null;
        long value = 0;
        boolean ignore = false;
        for (String arg : input.split(" ")) {
            if (arg.equalsIgnoreCase("#ignore")) {
                ignore = true;
                continue;
            }
            if (arg.startsWith("#")) arg = arg.substring(1);
            String[] split = arg.split("[=|:]");
            String key = split[0];
            DepositType tmp = StringMan.parseUpper(DepositType.class, key.toUpperCase(Locale.ROOT));
            if (type == null || (type != tmp)) {
                type = tmp;
            } else {
                throw new IllegalArgumentException("Invalid deposit type (duplicate): `" + input + "`");
            }
            if (split.length == 2) {
                value = Long.valueOf(split[1]);
            } else if (split.length != 1) {
                throw new IllegalArgumentException("Invalid deposit type (value): `" + input + "`");
            }
        }
        if (type == null) {
            if (ignore) {
                type = DepositType.IGNORE;
            } else {
                throw new IllegalArgumentException("Invalid deposit type (empty): `" + input + "`");
            }
        }
        return new DepositType.DepositTypeInfo(type, value, ignore);
    }

    @Binding(value = "nation id, name or url", examples = {"Borg", "<@664156861033086987>", "Danzek", "189573", "https://diplomacyandstrife.com/nation/2163"})
    public static DBNation nation(@Default @Me User selfUser, String input) {
        DBNation nation = DiscordUtil.parseNation(input);
        if (nation == null) {
            if (selfUser != null && (input.equalsIgnoreCase("%user%") || input.equalsIgnoreCase("{usermention}"))) {
                nation = DiscordUtil.getNation(selfUser);
            }
            if (nation == null) {
                String error = "No such nation: `" + input + "`";
                if (input.contains(",")) {
                    throw new IllegalArgumentException(error + " (Multiple nations are not accepted for this argument)");
                }
                throw new IllegalArgumentException(error);
            }
        }
        return nation;
    }

    @Binding(value = "4 whole numbers representing barracks,factory,hangar,drydock", examples = {"5553", "0/2/5/0"})
    public MMRInt mmrInt(String input) {
        return MMRInt.fromString(input);
    }

    @Binding(value = "4 decimal numbers representing barracks, factory, hangar, drydock", examples = {"0.0/2.0/5.0/0.0", "5553"})
    public MMRDouble mmrDouble(String input) {
        return MMRDouble.fromString(input);
    }

    public static NationOrAlliance nationOrAlliance(String input) {
        return nationOrAlliance(input, null);
    }

    @Binding(value = "A nation or alliance name, url or id. Prefix with `AA:` or `nation:` to avoid ambiguity if there exists both by the same name or id", examples = {"Borg", "https://diplomacyandstrife.com/alliance/1234", "aa:1234"})
    public static NationOrAlliance nationOrAlliance(String input, @Default ParameterData data) {
        return nationOrAlliance(data, input, false);
    }

    public static NationOrAlliance nationOrAlliance(ParameterData data, String input, boolean forceAllowDeleted) {
        String lower = input.toLowerCase();
        if (lower.startsWith("aa:") || lower.startsWith("alliance:")) {
            return alliance(data, input.split(":", 2)[1]);
        }
        if (lower.contains("alliance/")) {
            return alliance(data, input);
        }
        DBNation nation = DiscordUtil.parseNation(input, forceAllowDeleted || (data != null && data.getAnnotation(AllowDeleted.class) != null));
        if (nation == null) {
            return alliance(data, input);
        }
        return nation;
    }

    @Binding(value = "A guild or alliance name, url or id. Prefix with `AA:` or `guild:` to avoid ambiguity if there exists both by the same name or id", examples = {"guild:216800987002699787", "aa:1234"})
    public static GuildOrAlliance GuildOrAlliance(ParameterData data, String input) {
        String lower = input.toLowerCase();
        if (lower.startsWith("aa:") || lower.startsWith("alliance:")) {
            return alliance(data, input.split(":", 2)[1]);
        }
        if (lower.contains("alliance/")) {
            return alliance(data, input);
        }
        if (lower.startsWith("guild:")) {
            input = input.substring(6);
            if (!MathMan.isInteger(input)) {
                return guild(Long.parseLong(input));
            }
            throw new IllegalArgumentException("Invalid guild id: " + input);
        }
        if (MathMan.isInteger(input)) {
            long id = Long.parseLong(input);
            return guild(id);
        }
        return alliance(data, input);
    }

    @Binding
    public NationPlaceholders placeholders() {
        return Locutus.imp().getCommandManager().getV2().getNationPlaceholders();
    }

    @Binding
    public AlliancePlaceholders aa_placeholders() {
        return Locutus.imp().getCommandManager().getV2().getAlliancePlaceholders();
    }

    @Binding(examples = {"Borg", "alliance/1411", "647252780817448972"}, value = "A nation or alliance name, url or id, or a guild id")
    public static NationOrAllianceOrGuild nationOrAllianceOrGuild(String input, @Default ParameterData data, @Default @Me GuildDB selfDb) {
        if (data != null && input.equals("*")) {
            if (data.getAnnotation(StarIsGuild.class) != null && selfDb != null) {
                return selfDb;
            }
        }
        boolean allowDeleted = data != null && data.getAnnotation(AllowDeleted.class) != null;
        try {
            return nationOrAlliance(data, input, allowDeleted);
        } catch (IllegalArgumentException ignore) {
            if (input.startsWith("guild:")) {
                input = input.substring(6);
                if (!MathMan.isInteger(input)) {
                    for (GuildDB db : Locutus.imp().getGuildDatabases().values()) {
                        if (db.getName().equalsIgnoreCase(input) || db.getGuild().getName().equalsIgnoreCase(input)) {
                            return db;
                        }
                    }
                }
            }
            if (MathMan.isInteger(input)) {
                long id = Long.parseLong(input);
                if (id > Integer.MAX_VALUE) {
                    GuildDB db = Locutus.imp().getGuildDB(id);
                    if (db == null) {
                        if (data != null && data.getAnnotation(AllowDeleted.class) != null) {
                            throw new IllegalArgumentException("Not connected to guild: " + id + " (deleted guilds are not currently supported)");
                        }
                        throw new IllegalArgumentException("Not connected to guild: " + id);
                    }
                    return db;
                }
            }
            for (GuildDB value : Locutus.imp().getGuildDatabases().values()) {
                if (value.getName().equalsIgnoreCase(input)) {
                    return value;
                }
            }
            throw ignore;
        }
    }

    public static NationOrAllianceOrGuild nationOrAllianceOrGuild(String input) {
        return nationOrAllianceOrGuild(input, null, null);
    }

    public static DBAlliance alliance(String input) {
        return alliance(null, input);
    }

    @Binding(examples = {"'Error 404'", "7413", "https://diplomacyandstrife.com/alliance/1411"}, value = "An alliance name id or url")
    public static DBAlliance alliance(ParameterData data, String input) {
        Integer aaId = DNS.parseAllianceId(input);
        if (aaId == null) throw new IllegalArgumentException("Invalid alliance: " + input);
        return DBAlliance.getOrCreate(aaId);
    }

    @Binding(value = "A comma separated list of audit types")
    public Set<IACheckup.AuditType> auditTypes(ValueStore store, String input) {
        Set<IACheckup.AuditType> audits = Locutus.cmd().getV2().getPlaceholders().get(IACheckup.AuditType.class).parseSet(store, input);
        if (audits == null || audits.isEmpty()) {
            throw new IllegalArgumentException("No audit types found in: " + input + ". Options: " + StringMan.getString(IACheckup.AuditType.values()));
        }
        return audits;
    }

    @Binding(value = "An audit type")
    public static IACheckup.AuditType auditType(String input) {
        return emum(IACheckup.AuditType.class, input);
    }

    @Binding(value = "A comma separated list of auto audit types")
    public Set<AutoAuditType> autoAuditType(String input) {
        return emumSet(AutoAuditType.class, input);
    }

    @Binding(value = "A comma separated list of alliance metrics")
    public Set<AllianceMetric> metrics(String input) {
        Set<AllianceMetric> metrics = new HashSet<>();
        for (String type : input.split(",")) {
            AllianceMetric arg = StringMan.parseUpper(AllianceMetric.class, type);
            metrics.add(arg);
        }
        return metrics;
    }

    @Binding(value = "A comma separated list of nation projects")
    public static Set<Project> projects(ValueStore store, String input) {
        Set<Project> result = Locutus.cmd().getV2().getPlaceholders().get(Project.class).parseSet(store, input);
        if (result == null || result.isEmpty()) {
            throw new IllegalArgumentException("No projects found in: " + input + ". Options: " + StringMan.getString(Project.values));
        }
        return result;
    }

    @Binding(value = "A comma separated list of building types")
    public Set<Building> buildings(ValueStore store, String input) {
        Set<Building> result = Locutus.cmd().getV2().getPlaceholders().get(Building.class).parseSet(store, input);
        if (result == null || result.isEmpty()) {
            throw new IllegalArgumentException("No projects found in: " + input + ". Options: " + StringMan.getString(Building.values()));
        }
        return result;
    }

    @Binding(examples = "borg,AA:Cataclysm,#position>1", value = "A comma separated list of nations, alliances and filters")
    public static Set<DBNation> nations(ParameterData data, @Default @Me Guild guild, String input, @Default @Me User author, @Default @Me DBNation me) {
        return nations(data, guild, input, false, author, me);
    }

    public static Set<DBNation> nations(ParameterData data, @Default @Me Guild guild, String input, boolean forceAllowDeleted, @Default @Me User user, @Default @Me DBNation nation) {
        Set<DBNation> nations = DiscordUtil.parseNations(guild, user, nation, input, true, forceAllowDeleted || (data != null && data.getAnnotation(AllowDeleted.class) != null));
        if (nations.isEmpty() && (data == null || data.getAnnotation(AllowEmpty.class) == null)) {
            throw new IllegalArgumentException("No nations found matching: `" + input + "`");
        }
        return nations;
    }

    @Binding(examples = "borg,AA:Cataclysm,#position>1", value = "A comma separated list of nations, alliances and filters",
            webType = "DBNation")
    public static NationList nationList(ParameterData data, @Default @Me Guild guild, String input, @Default @Me User author, @Default @Me DBNation me) {
        return new SimpleNationList(nations(data, guild, input, author, me)).setFilter(input);
    }

    @Binding(examples = "#position>1,#land<=5", value = "A comma separated list of filters (can include nations and alliances)",
    webType = "Predicate<DBNation>")
    public NationFilter nationFilter(@Default @Me User author, @Default @Me DBNation nation, @Default @Me Guild guild, String input) {
        return new NationFilterString(input, guild, author, nation);
    }

    @Binding(examples = "score,soldiers", value = "A comma separated list of numeric nation attributes",
    webType = "Set<TypedFunction<DBNation, Double>>")
    public Set<NationAttributeDouble> nationMetricDoubles(ValueStore store, String input) {
        Set<NationAttributeDouble> metrics = new LinkedHashSet<>();
        for (String arg : StringMan.split(input, ',')) {
            metrics.add(nationMetricDouble(store, arg));
        }
        return metrics;
    }

    @Binding(examples = "warpolicy,color", value = "A comma separated list of nation attributes")
    public Set<NationAttribute> nationMetrics(ValueStore store, String input) {
        Set<NationAttribute> metrics = new LinkedHashSet<>();
        for (String arg : StringMan.split(input, ',')) {
            metrics.add(nationMetric(store, arg));
        }
        return metrics;
    }

    @Binding(examples = "borg,AA:Cataclysm", value = "A comma separated list of nations and alliances")
    public static Set<NationOrAlliance> nationOrAlliance(ParameterData data, @Default @Me Guild guild, String input, @Default @Me User author, @Default @Me DBNation me) {
        Set<NationOrAlliance> result = nationOrAlliance(data, guild, input, false, author, me);
        boolean allowDeleted = data != null && data.getAnnotation(AllowDeleted.class) != null;
        if (!allowDeleted) {
            result.removeIf(n -> !n.isValid());
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("No nations or alliances found matching: `" + input + "`");
        }
        return result;
    }

    public static Set<NationOrAlliance> nationOrAlliance(ParameterData data, @Default @Me Guild guild, String input, boolean forceAllowDeleted, @Default @Me User author, @Default @Me DBNation me) {
        Placeholders<NationOrAlliance> placeholders = Locutus.cmd().getV2().getPlaceholders().get(NationOrAlliance.class);
        return placeholders.parseSet(guild, author, me, input);
    }

    @Binding(examples = "borg,AA:Cataclysm,647252780817448972", value = "A comma separated list of nations, alliances and guild ids")
    public Set<NationOrAllianceOrGuild> nationOrAllianceOrGuilds(ParameterData data, @Default @Me Guild guild, String input, @Default @Me User author, @Default @Me DBNation me) {
        List<String> args = StringMan.split(input, ',');
        Set<NationOrAllianceOrGuild> result = new LinkedHashSet<>();
        List<String> remainder = new ArrayList<>();
        outer:
        for (String arg : args) {
            arg = arg.trim();
            if (arg.startsWith("guild:")) {
                arg = arg.substring(6);
                if (!MathMan.isInteger(arg)) {
                    for (GuildDB db : Locutus.imp().getGuildDatabases().values()) {
                        if (db.getName().equalsIgnoreCase(arg) || db.getGuild().getName().equalsIgnoreCase(arg)) {
                            result.add(db);
                            continue outer;
                        }
                    }
                    throw new IllegalArgumentException("Unknown guild: " + arg);
                }
            }
            if (MathMan.isInteger(arg)) {
                long id = Long.parseLong(arg);
                if (id > Integer.MAX_VALUE) {
                    GuildDB db = Locutus.imp().getGuildDB(id);
                    if (db == null) throw new IllegalArgumentException("Unknown guild: " + id);
                    result.add(db);
                    continue;
                }
            }

            try {
                DBAlliance aa = alliance(data, arg);
                if (aa.exists()) {
                    result.add(aa);
                    continue;
                }
            } catch (IllegalArgumentException ignore) {}
            GuildDB db = guild == null ? null : Locutus.imp().getGuildDB(guild);
            if (db != null) {
                if (arg.charAt(0) == '~') arg = arg.substring(1);
                Set<Integer> coalition = db.getCoalition(arg);
                if (!coalition.isEmpty()) {
                    result.addAll(coalition.stream().map(f -> DBAlliance.getOrCreate(f)).collect(Collectors.toSet()));
                    continue;
                }
            }
            remainder.add(arg);
        }
        if (!remainder.isEmpty()) {
            result.addAll(nations(data, guild, StringMan.join(remainder, ","), author, me));
        }
        if (result.isEmpty()) throw new IllegalArgumentException("Invalid nations or alliances: " + input);
        return result;
    }

    public static Set<DBAlliance> alliances(@Default @Me Guild guild, String input, @Default @Me User author, @Default @Me DBNation me) {
        Set<DBAlliance> alliances = Locutus.cmd().getV2().getAlliancePlaceholders().parseSet(guild, author, me, input);
        if (alliances.isEmpty()) throw new IllegalArgumentException("No alliances found for: `" + input + "`");
        return alliances;
    }

    @Binding(examples = "Cataclysm,790", value = "A comma separated list of alliances")
    public Set<DBAlliance> alliances(AlliancePlaceholders placeholders, ValueStore store, String input) {
        return placeholders.parseSet(store, input);
    }
//
//    @Binding(examples = "Cataclysm,790", value = "A comma separated list of alliances")
//    public static Set<DBAlliance> alliances(ValueStore store, @Default @Me Guild guild, String input, AlliancePlaceholders placeholders) {
//
//        // make function parsing non hash
//
//        for (String arg : StringMan.split(input, ',')) {
//            arg = arg.trim();
//            if (arg.isEmpty()) {
//                throw new IllegalArgumentException("Empty argument. Did you use an extra comma? (input: `" + input + "`)");
//            }
//            char char0 = arg.charAt(0);
//            if (char0 == '#') {
//                Predicate<DBAlliance> filter = placeholders.getFilter(store, arg.substring(1));
//            }
//        }
//    }

    @Binding(examples = "ACTIVE,EXPIRED", value = "A comma separated list of war statuses")
    public static Set<WarStatus> WarStatuses(String input) {
        Set<WarStatus> result = new HashSet<>();
        for (String s : input.split(",")) {
            result.add(WarStatus.parse(s));
        }
        return result;
    }

    @Binding(examples = "ATTRITION,RAID", value = "A comma separated list of war declaration types")
    public static Set<WarType> WarTypes(String input) {
        return emumSet(WarType.class, input);
    }


    @Binding(examples = "SOLDIER,TANK,AIRCRAFT,SHIP,MISSILE,NUKE", value = "A comma separated list of military units")
    public Set<MilitaryUnit> MilitaryUnits(ValueStore store, String input) {
        Set<MilitaryUnit> result = Locutus.cmd().getV2().getPlaceholders().get(MilitaryUnit.class).parseSet(store, input);
        if (result == null || result.isEmpty()) {
            throw new IllegalArgumentException("No projects found in: " + input + ". Options: " + StringMan.getString(MilitaryUnit.values));
        }
        return result;
    }

    @Binding(examples = {"aluminum", "money", "`*`", "manu", "raws", "!food"}, value = "A comma separated list of resource types")
    public static Set<ResourceType> rssTypes(String input) {
        Set<ResourceType> types = new LinkedHashSet<>();
        for (String arg : input.split(",")) {
            boolean remove = arg.startsWith("!");
            if (remove) arg = arg.substring(1);
            List<ResourceType> toAddOrRemove;
            if (arg.equalsIgnoreCase("*")) {
                toAddOrRemove = (Arrays.asList(ResourceType.values()));
            } else {
                toAddOrRemove = Collections.singletonList(ResourceType.parse(arg));
            }
            if (remove) types.removeAll(toAddOrRemove);
            else types.addAll(toAddOrRemove);
        }
        return new LinkedHashSet<>(types);
    }

    @AllianceDepositLimit
    @Binding(examples = {"{money=1.2,food=6}", "food 5,money 3", "5f 3$ 10.5c", "$53"}, value = "A comma separated list of resources and their amounts, which will be restricted by an alliance's account balance")
    public Map<ResourceType, Double> resourcesAA(String resources) {
        return resources(resources);
    }

    @NationDepositLimit
    @Binding(examples = {"{money=1.2,food=6}", "food 5,money 3", "5f 3$ 10.5c", "$53"}, value = "A comma separated list of resources and their amounts, which will be restricted by an nations's account balance")
    public Map<ResourceType, Double> resourcesNation(String resources) {
        return resources(resources);
    }

    @Binding(examples = {"{money=1.2,food=6}", "food 5,money 3", "5f 3$ 10.5c", "$53", "{food=1}*1.5"}, value = "A comma separated list of resources and their amounts")
    public Map<ResourceType, Double> resources(String resources) {
        Map<ResourceType, Double> map = ResourceType.parseResources(resources);
        if (map == null) throw new IllegalArgumentException("Invalid resources: " + resources);
        return map;
    }

    @Binding(examples = {"{soldiers=12,tanks=56}"}, value = "A comma separated list of units and their amounts")
    public Map<MilitaryUnit, Long> units(String input) {
        Map<MilitaryUnit, Long> map = DNS.parseUnits(input);
        if (map == null) throw new IllegalArgumentException("Invalid units: " + input + ". Valid types: " + StringMan.getString(MilitaryUnit.values()) + ". In the form: `{SOLDIERS=1234,TANKS=5678}`");
        return map;
    }

    @Binding(examples = {"money", "aluminum"}, value = "The name of a resource")
    public static ResourceType resource(String resource) {
        return ResourceType.parse(resource);
    }

    @Binding(value = "A note to use for a bank transfer")
    public static DepositType DepositType(String input) {
        if (input.startsWith("#")) input = input.substring(1);
        return StringMan.parseUpper(DepositType.class, input.toUpperCase(Locale.ROOT));
    }

    @Binding(value = "A war declaration type")
    public WarType warType(String warType) {
        return WarType.parse(warType);
    }

    @Binding
    @Me
    public DBNation nationProvided(@Default @Me User user) {
        if (user == null) {
            throw new IllegalStateException("No user provided in command locals");
        }
        DBNation nation = DiscordUtil.getNation(user);
        if (nation == null) throw new IllegalStateException("Please use " + CM.register.cmd.toSlashMention());
        return nation;
    }

    @Binding
    public ConflictManager ConflictManager() {
        Locutus lc = Locutus.imp();
        ConflictManager manager = lc.getWarDb().getConflicts();
        if (manager == null) {
            throw new IllegalStateException("No conflict manager provided in command locals");
        }
        return manager;
    }

    @Binding
    @Me
    public IMessageIO io() {
        throw new IllegalArgumentException("No channel io binding found");
    }

    @Binding
    @Me
    public DBAlliance alliance(@Me DBNation nation) {
        return DBAlliance.getOrCreate(nation.getAlliance_id());
    }

    @Binding
    @Me
    public Map<ResourceType, Double> deposits(@Me GuildDB db, @Me DBNation nation) throws IOException {
        return ResourceType.resourcesToMap(deposits2(db, nation));
    }

    @Binding
    @Me
    public double[] deposits2(@Me GuildDB db, @Me DBNation nation) throws IOException {
        return nation.getNetDeposits(db, false);
    }

    @Binding
    @Me
    public Rank rank(@Me DBNation nation) {
        return nation.getPositionEnum();
    }

    @Binding(value = "A war status")
    public WarStatus status(String input) {
        return WarStatus.parse(input);
    }

    @Binding(value = "Mode for automatically giving discord roles")
    public GuildDB.AutoRoleOption roleOption(String input) {
        return emum(GuildDB.AutoRoleOption.class, input);
    }

    @Binding(value = "Mode for automatically giving discord nicknames")
    public GuildDB.AutoNickOption nickOption(String input) {
        return emum(GuildDB.AutoNickOption.class, input);
    }

    @Binding
    public WarDB warDB() {
        return Locutus.imp().getWarDb();
    }

    @Binding
    public NationDB nationDB() {
        return Locutus.imp().getNationDB();
    }

    @Binding
    public BankDB bankDB() {
        return Locutus.imp().getBankDB();
    }

    @Binding
    public DiscordDB discordDB() {
        return Locutus.imp().getDiscordDB();
    }

    @Binding
    public ReportManager ReportManager() {
        return Locutus.imp().getNationDB().getReportManager();
    }

    @Binding
    @Me
    public GuildDB guildDB(@Me Guild guild) {
        return Locutus.imp().getGuildDB(guild);
    }

    @Binding(examples = "647252780817448972", value = "A discord guild id. See: <https://en.wikipedia.org/wiki/Template:Discord_server#Getting_Guild_ID>")
    public static GuildDB guild(long guildId) {
        GuildDB guild = Locutus.imp().getGuildDB(guildId);
        if (guild == null) throw new IllegalStateException("No guild found for: " + guildId);
        return guild;
    }


    @Binding
    @Me
    public GuildHandler handler(@Me GuildDB db) {
        return db.getHandler();
    }

    @Binding
    public ExecutorService executor() {
        return Locutus.imp().getExecutor();
    }

    @Binding
    public IACategory iaCat(@Me GuildDB db) {
        IACategory iaCat = db.getIACategory();
        if (iaCat == null) throw new IllegalArgumentException("No IA category exists (please see: <TODO document>)");
        return iaCat;
    }

    @Binding
    @Me
    public Auth auth(@Me DBNation nation) {
        return nation.getAuth(true);
    }

    @Binding(value = "One of the default in-game position levels")
    public Rank rank(String rank) {
        return emum(Rank.class, rank);
    }

    @Binding(value = "A nation api key in the form `<nation>:<key>`")
    public ApiKeyPool.ApiKey apiKey(String input) {
        String[] split = input.split("[:]");
        if (split.length != 2) throw new IllegalArgumentException("Invalid api key: " + StringMan.stripApiKey(input) + ". Expected format: `<nation>:<key>`");
        DBNation nation = nation(null, split[0]);
        String keyStr = split[1];
        if (!keyStr.matches("[a-zA-Z0-9]+")) throw new IllegalArgumentException("Invalid api key: " + StringMan.stripApiKey(keyStr) + ". Expected format: `<nation>:<key>`");
        return new ApiKeyPool.ApiKey(nation.getNation_id(), keyStr);
    }

    @Binding(value = "A list of api keys")
    public List<ApiKeyPool.ApiKey> keys(String input) {
        List<ApiKeyPool.ApiKey> keys = new ArrayList<>();
        for (String key : input.split(",")) {
            keys.add(apiKey(key));
        }
        return keys;
    }

    @Binding
    public ReportManager.ReportType reportType(String input) {
        return emum(ReportManager.ReportType.class, input);
    }

    @Binding(value = "Bot guild settings")
    public static GuildSetting key(String input) {
        input = input.replaceAll("_", " ").toLowerCase();
        GuildSetting[] constants = GuildKey.values();
        for (GuildSetting constant : constants) {
            String name = constant.name().replaceAll("_", " ").toLowerCase();
            if (name.equals(input)) return constant;
        }
        List<String> options = Arrays.asList(constants).stream().map(GuildSetting::name).collect(Collectors.toList());
        throw new IllegalArgumentException("Invalid category: `" + input + "`. Options: " + StringMan.getString(options));
    }

    @Binding(value = "Types of users to clear roles of")
    public UnsortedCommands.ClearRolesEnum clearRolesEnum(String input) {
        return emum(UnsortedCommands.ClearRolesEnum.class, input);
    }

    @Binding(value = "The mode for calculating war costs")
    public WarCostMode WarCostMode(String input) {
        return emum(WarCostMode.class, input);
    }

    @Binding(value = "A war attack statistic")
    public WarCostStat WarCostStat(String input) {
        return emum(WarCostStat.class, input);
    }

    @Binding(value = "Bank transaction flow type (internal, withdrawal, depost)")
    public static FlowType FlowType(String input) {
        return emum(FlowType.class, input);
    }

    @Binding(examples = {"@role", "672238503119028224", "roleName"}, value = "A discord role name, mention or id")
    public Roles role(String role) {
        return emum(Roles.class, role);
    }

    @Binding(value = "Military unit name")
    public static MilitaryUnit unit(String unit) {
        return emum(MilitaryUnit.class, unit);
    }

    @Binding(value = "Math comparison operation")
    public MathOperation op(String input) {
        return emum(MathOperation.class, input);
    }

    @Binding(value = "One of the default Bot coalition names")
    public Coalition coalition(String input) {
        return emum(Coalition.class, input);
    }

    @Binding(value = "A name for a default or custom Bot coalition")
    @GuildCoalition
    public String guildCoalition(@Me GuildDB db, String input) {
        input = input.toLowerCase();
        Set<String> coalitions = db.getCoalitionNames();
        for (Coalition value : Coalition.values()) coalitions.add(value.name().toLowerCase());
        if (!coalitions.contains(input)) throw new IllegalArgumentException(
                "No coalition found matching: `" + input +
                        "`. Options: " + StringMan.getString(coalitions) + "\n" +
                        "Create it via " + CM.coalition.create.cmd.toSlashMention()
        );
        return input;
    }

    @Binding
    public AllianceList allianceList(ParameterData param, @Default @Me User user, @Me GuildDB db) {
        AllianceList list = db.getAllianceList();
        if (list == null) {
            throw new IllegalArgumentException("This guild has no registered alliance. See " + CM.settings.info.cmd.toSlashMention() + " with key " + GuildKey.ALLIANCE_ID.name());
        }
        RolePermission perms = param.getAnnotation(RolePermission.class);
        if (perms != null) {
            if (user != null) {
                Set<Integer> allowedIds = new HashSet<>();
                for (int aaId : list.getIds()) {
                    try {
                        PermissionBinding.checkRole(db.getGuild(), perms, user, aaId);
                        allowedIds.add(aaId);
                    } catch (IllegalArgumentException ignore) {
                    }
                }
                if (allowedIds.isEmpty()) {
                    throw new IllegalArgumentException("You are lacking role permissions for the alliance ids: " + StringMan.getString(list.getIds()));
                }
                return new AllianceList(allowedIds);
            }
            throw new IllegalArgumentException("Not registered");
        } else {
            throw new IllegalArgumentException("TODO: disable this error once i verify it works (see console for debug info)");
        }
    }

    @Me
    @Binding
    public WarCategory.WarRoom warRoom(@Me WarCategory warCat, @Me TextChannel channel) {
        WarCategory.WarRoom warroom = warCat.getWarRoom(channel);
        if (warroom == null) throw new IllegalArgumentException("The command was not run in a war room");
        return warroom;
    }
    @Me
    @Binding
    public WarCategory warChannelBinding(@Me GuildDB db) {
        WarCategory warChannel = db.getWarChannel(true);
        if (warChannel == null) throw new IllegalArgumentException("War channels are not enabled. " + GuildKey.ENABLE_WAR_ROOMS.getCommandObj(db, true) + "");
        return warChannel;
    }

    @Binding(value = "A project name. Replace spaces with `_`. See: <https://diplomacy-strife.fandom.com/wiki/Projects>", examples = "NATIONAL_HIGHWAY_SYSTEM")
    public static Project project(String input) {
        Project project = Project.parse(input);
        if (project == null) throw new IllegalArgumentException("Invalid project: `"  + input + "`. Options: " + StringMan.getString(Project.values));
        return project;
    }

    @Binding(value = "A timed policy")
    public static TimedPolicy policy(String input) {
        TimedPolicy policy = TimedPolicy.parse(input);
        if (policy == null) throw new IllegalArgumentException("Invalid policy: `"  + input + "`. Options: " + StringMan.getString(TimedPolicy.values));
        return policy;
    }

    @Binding(value = "A Bot metric for alliances")
    public AllianceMetric AllianceMetric(String input) {
        return StringMan.parseUpper(AllianceMetric.class, input);
    }

    @Binding(value = "A mode for receiving alerts when a nation leaves beige")
    public NationMeta.ProtectionAlertMode BeigeAlertMode(String input) {
        return StringMan.parseUpper(NationMeta.ProtectionAlertMode.class, input);
    }

    @Binding(value = "A discord status for receiving alerts when a nation leaves beige")
    public NationMeta.BeigeAlertRequiredStatus BeigeAlertRequiredStatus(String input) {
        return StringMan.parseUpper(NationMeta.BeigeAlertRequiredStatus.class, input);
    }

    @Binding(value = "A completed nation attribute that accepts no arguments and returns a number\n" +
            "To get the attribute for an attribute with arguments, you must provide a value in brackets\n" +
            "See: <https://github.com/xdnw/diplocutus/wiki/nation_placeholders>", examples = {"score", "ships", "land", "getCitiesSince(5d)"},
    webType = "TypedFunction<DBNation,Double>")
    public NationAttributeDouble nationMetricDouble(ValueStore store, String input) {
        NationPlaceholders placeholders = Locutus.imp().getCommandManager().getV2().getNationPlaceholders();
        NationAttributeDouble metric = placeholders.getMetricDouble(store, input);
        if (metric == null) {
            String optionsStr = StringMan.getString(placeholders.getMetricsDouble(store).stream().map(NationAttribute::getName).collect(Collectors.toList()));
            throw new IllegalArgumentException("Invalid metric: `" + input + "`. Options: " + optionsStr);
        }
        return metric;
    }

    @Binding(value = "A completed nation attribute that accepts no arguments, returns an object, typically a string, number, boolean or enum\n" +
            "To get the attribute for an attribute with arguments, you must provide a value in brackets\n" +
            "See: <https://github.com/xdnw/diplocutus/wiki/nation_placeholders>", examples = {"color", "war_policy", "continent", "city(1)"})
    public NationAttribute nationMetric(ValueStore store, String input) {
        NationPlaceholders placeholders = Locutus.imp().getCommandManager().getV2().getNationPlaceholders();
        NationAttribute metric = placeholders.getMetric(store, input, false);
        if (metric == null) {
            String optionsStr = StringMan.getString(placeholders.getMetrics(store).stream().map(NationAttribute::getName).collect(Collectors.toList()));
            throw new IllegalArgumentException("Invalid metric: `" + input + "`. Options: " + optionsStr);
        }
        return metric;
    }

    @Binding(value = "An in-game treaty type")
    public static TreatyType TreatyType(String input) {
        return TreatyType.parse(input);
    }

    @Binding
    @ReportPerms
    public ReportManager.Report getReport(ReportManager manager, int id) {
        return getReportAll(manager, id);
    }

    @Binding
    public ReportManager.Report getReportAll(ReportManager manager, int id) {
        ReportManager.Report report = manager.getReport(id);
        if (report == null) {
            throw new IllegalArgumentException("No report found with id: `" + id + "`");
        }
        return report;
    }

    @Binding
    public Predicate<DBWar> warFilter(ValueStore store, String input) {
        Placeholders<DBWar> placeholders = Locutus.cmd().getV2().getPlaceholders().get(DBWar.class);
        return placeholders.parseFilter(store, input);
    }

    @Binding
    public AllianceMetricMode AllianceMetricMode(String mode) {
        return emum(AllianceMetricMode.class, mode);
    }

    @Binding
    public NationMeta meta(String input) {
        return emum(NationMeta.class, input);
    }

    @Binding
    public Building building(String input) {
        return emum(Building.class, input);
    }

    @Binding
    public static Technology Technology(String input) {
        return emum(Technology.class, input);
    }

    @Binding
    public WarCostByDayMode WarCostByDayMode(String input) {
        return emum(WarCostByDayMode.class, input);
    }
}
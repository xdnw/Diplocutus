package link.locutus.discord.commands.manager.v2.impl.pw.filter;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import link.locutus.discord.Locutus;
import link.locutus.discord.api.generated.TreatyType;
import link.locutus.discord.api.types.*;
import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.api.types.espionage.CyberOps;
import link.locutus.discord.api.types.espionage.Espionage;
import link.locutus.discord.api.types.tx.*;
import link.locutus.discord.commands.manager.v2.binding.Key;
import link.locutus.discord.commands.manager.v2.binding.ValueStore;
import link.locutus.discord.commands.manager.v2.binding.annotation.*;
import link.locutus.discord.commands.manager.v2.binding.bindings.*;
import link.locutus.discord.commands.manager.v2.binding.validator.ValidatorStore;
import link.locutus.discord.commands.manager.v2.command.IMessageIO;
import link.locutus.discord.commands.manager.v2.impl.discord.binding.DiscordBindings;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.RolePermission;
import link.locutus.discord.commands.manager.v2.impl.pw.binding.PWBindings;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.commands.manager.v2.perm.PermissionHandler;
import link.locutus.discord.db.conflict.Conflict;
import link.locutus.discord.db.conflict.ConflictManager;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.SelectionAlias;
import link.locutus.discord.db.entities.SheetTemplate;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.db.entities.DBWar;
import link.locutus.discord.db.entities.Treaty;
import link.locutus.discord.db.entities.UserWrapper;
import link.locutus.discord.db.guild.GuildKey;
import link.locutus.discord.db.guild.GuildSetting;
import link.locutus.discord.pnw.NationList;
import link.locutus.discord.pnw.NationOrAlliance;
import link.locutus.discord.pnw.SimpleNationList;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.MathMan;
import link.locutus.discord.util.StringMan;
import link.locutus.discord.util.discord.DiscordUtil;
import link.locutus.discord.util.scheduler.ThrowingTriFunction;
import link.locutus.discord.util.sheet.SpreadSheet;
import link.locutus.discord.util.task.ia.IACheckup;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PlaceholdersMap {

    private static PlaceholdersMap INSTANCE;

    public static String getClassName(String simpleName) {
            return simpleName.replace("DB", "").replace("Wrapper", "")
                    .replaceAll("[0-9]", "")
                    .toLowerCase(Locale.ROOT);
    }
    public static String getClassName(Class clazz) {
        return getClassName(clazz.getSimpleName());
    }

    public static Placeholders<DBNation> NATIONS = null;
    public static Placeholders<DBAlliance> ALLIANCES = null;
    public static Placeholders<NationOrAlliance> NATION_OR_ALLIANCE = null;
    public static Placeholders<GuildDB> GUILDS = null;
    public static Placeholders<Treaty> TREATIES = null;
    public static Placeholders<TreatyType> TREATY_TYPES = null;
    public static Placeholders<ResourceType> RESOURCE_TYPES = null;
    public static Placeholders<IACheckup.AuditType> AUDIT_TYPES = null;
    public static Placeholders<NationList> NATION_LIST = null;
    public static Placeholders<UserWrapper> USERS = null;
    public static Placeholders<DBWar> WARS = null;
    public static Placeholders<GuildSetting> SETTINGS = null;
    public static Placeholders<Conflict> CONFLICTS = null;
    public static Placeholders<Espionage> ESPIONAGE = null;
    public static Placeholders<CyberOps> CYBEROPS = null;
    public static Placeholders<Building> BUILDINGS = null;
    public static Placeholders<MilitaryUnit> UNITS = null;
    public static Placeholders<TimedPolicy> POLICIES = null;
    public static Placeholders<Project> PROJECTS = null;
    public static Placeholders<Technology> TECHNOLOGIES = null;
    public static Placeholders<GrantTransfer> GRANTS = null;
    public static Placeholders<LoanTransfer> LOANS = null;
    public static Placeholders<BankTransfer> DEPOSITS = null;
    public static Placeholders<EquipmentTransfer> EQUIPMENT_TRANSFERS = null;



    // --------------------------------------------------------------------


    private final Map<Class<?>, Placeholders<?>> placeholders = new ConcurrentHashMap<>();
    private final ValueStore store;
    private final ValidatorStore validators;
    private final PermissionHandler permisser;

    public Set<Class<?>> getTypes() {
        return placeholders.keySet();
    }

    public PlaceholdersMap(ValueStore store, ValidatorStore validators, PermissionHandler permisser) {
        if (INSTANCE != null) {
            throw new IllegalStateException("Already initialized");
        }
        INSTANCE = this;
        
        this.store = store;
        this.validators = validators;
        this.permisser = permisser;


        this.placeholders.put(TreatyType.class, createTreatyType());
//        Placeholders<DBNation> NATIONS = null;
        this.placeholders.put(DBNation.class, new NationPlaceholders(store, validators, permisser));
        //        Placeholders<DBAlliance> ALLIANCES = null;
        this.placeholders.put(DBAlliance.class, new AlliancePlaceholders(store, validators, permisser));
        //        Placeholders<NationOrAlliance> NATION_OR_ALLIANCE = null;
        this.placeholders.put(NationOrAlliance.class, createNationOrAlliances());
        //        Placeholders<GuildDB> GUILDS = null;
        this.placeholders.put(GuildDB.class, createGuildDB());
        //        Placeholders<Project> PROJECTS = null;
        this.placeholders.put(Project.class, createProjects());
        //        Placeholders<Treaty> TREATIES = null;
        this.placeholders.put(Treaty.class, createTreaty());
        //        Placeholders<ResourceType> RESOURCE_TYPES = null;
        this.placeholders.put(ResourceType.class, createResourceType());
        //        Placeholders<MilitaryUnit> UNITS = null;
        this.placeholders.put(MilitaryUnit.class, createMilitaryUnit());
        //        Placeholders<Building> BUILDINGS = null;
        this.placeholders.put(Building.class, createBuilding());
        //        Placeholders<IACheckup.AuditType> AUDIT_TYPES = null;
        this.placeholders.put(IACheckup.AuditType.class, createAuditType());
        //        Placeholders<NationList> NATION_LIST = null;
        this.placeholders.put(NationList.class, createNationList());
        //        Placeholders<UserWrapper> USERS = null;
        this.placeholders.put(UserWrapper.class, createUsers());
        //        Placeholders<DBWar> WARS = null;
        this.placeholders.put(DBWar.class, createWars());
        //        Placeholders<GuildSetting> SETTINGS = null;
        this.placeholders.put(GuildSetting.class, createGuildSettings());
        //        Placeholders<Conflict> CONFLICTS = null;
        this.placeholders.put(Conflict.class, createConflicts());
//        Placeholders<TimedPolicy> POLICIES = null;
        this.placeholders.put(TimedPolicy.class, createPolicies());
//        Placeholders<Technology> TECHNOLOGIES = null;
        this.placeholders.put(Technology.class, createTechnologies());
        Placeholders<GrantTransfer> GRANTS = null;
//        this.placeholders.put(GrantTransfer.class, createGrantTransfers());
////        Placeholders<LoanTransfer> LOANS = null;
//        this.placeholders.put(LoanTransfer.class, createLoanTransfers());
////        Placeholders<BankTransfer> DEPOSITS = null;
//        this.placeholders.put(BankTransfer.class, createBankTransfers());
////        Placeholders<EquipmentTransfer> EQUIPMENT_TRANSFERS = null;
//        this.placeholders.put(EquipmentTransfer.class, createEquipmentTransfers());

        // TODO FIXME :||remove
        //        Placeholders<Espionage> ESPIONAGE = null;
//        Placeholders<CyberOps> CYBEROPS = null;

        Map<Class, Field> fields = new HashMap<>();
        for (Field field : PlaceholdersMap.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (!Placeholders.class.isAssignableFrom(field.getType())) {
                continue;
            }
            field.setAccessible(true);
            Class enclosedType = (Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            fields.put(enclosedType, field);
        }

        for (Map.Entry<Class<?>, Placeholders<?>> entry : this.placeholders.entrySet()) {
            Class<?> enclosedType = entry.getKey();
            Field field = fields.get(enclosedType);
            if (field == null) {
                throw new IllegalStateException("Missing field for `" + enclosedType + "`. Options:\n- " + StringMan.join(fields.keySet(), "\n- "));
            }
            try {
                field.set(null, entry.getValue());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

//        //-GuildKey
//        this.placeholders.put(GuildSetting.class, createGuildSetting());
//        //- Tax records
//        // - * (within aa)
//        this.placeholders.put(AGrantTemplate.class, createGrantTemplates());
    }

    public <T> Placeholders<T> get(Class<T> type) {
        return (Placeholders<T>) this.placeholders.get(type);
    }

    public <T> Class<T> parseType(String name) {
        name = getClassName(name);
        String finalName = name;
        return (Class<T>) this.placeholders.keySet().stream().filter(f -> getClassName(f).equalsIgnoreCase(finalName)).findFirst().orElse(null);
    }

    public static <T> Set<T> getSelection(Placeholders<T> instance, ValueStore store, String input) {
        return getSelection(instance, store, input, true);
    }
    public static <T> Set<T> getSelection(Placeholders<T> instance, ValueStore store, String input, boolean throwError) {
        Class<T> type = instance.getType();
        boolean isSelection = false;
        String inputAlias = input;
        if (input.startsWith("$") && input.length() > 1) {
            isSelection = true;
            inputAlias = input.substring(1);
        } else if (input.startsWith("select:")) {
            isSelection = true;
            inputAlias = input.substring("select:".length());
        } else if (input.startsWith("selection:")) {
            isSelection = true;
            inputAlias = input.substring("selection:".length());
        } else if (input.startsWith("alias:")) {
            isSelection = true;
            inputAlias = input.substring("alias:".length());
        }
        if (isSelection) {
            GuildDB db = (GuildDB) store.getProvided(Key.of(GuildDB.class, Me.class), false);
            if (db != null) {
                SelectionAlias<T> selection = db.getSheetManager().getSelectionAlias(inputAlias, type);
                if (selection != null) {
                    String query = selection.getSelection();
                    return instance.parseSet(store, query);
                }
                if (throwError) {
                    Map<String, SelectionAlias<T>> options = db.getSheetManager().getSelectionAliases(type);
                    if (options.isEmpty()) {
                        throw new IllegalArgumentException("No selection aliases for type: `" + type.getSimpleName() + "` Create one with `/selection_alias add " + getClassName(type) + "`");
                    }
                    throw new IllegalArgumentException("Invalid selection alias: `" + inputAlias + "`. Options: `" + StringMan.join(options.keySet(), "`, `") + "` (use `$` or `select:` as the prefix). See also: " + CM.selection_alias.list.cmd.toSlashMention());
                }
            }
        }
        return null;
    }

    private Placeholders<NationOrAlliance> createNationOrAlliances() {
        NationPlaceholders nationPlaceholders = (NationPlaceholders) get(DBNation.class);
        AlliancePlaceholders alliancePlaceholders = (AlliancePlaceholders) get(DBAlliance.class);
        return new Placeholders<NationOrAlliance>(NationOrAlliance.class, store, validators, permisser) {
            @Override
            public Set<String> getSheetColumns() {
                return new LinkedHashSet<>(List.of("nation", "alliance"));
            }

            @Override
            public Set<SelectorInfo> getSelectorInfo() {
                Set<SelectorInfo> selectors = new LinkedHashSet<>(NATIONS.getSelectorInfo());
                selectors.addAll(ALLIANCES.getSelectorInfo());
                return selectors;
            }

            @Override
            public String getDescription() {
                return "A nation or alliance";
            }

            @Override
            public String getName(NationOrAlliance o) {
                return o.getName();
            }

            @Binding(value = "A comma separated list of items")
            @Override
            public Set<NationOrAlliance> parseSet(ValueStore store2, String input) {
                if (input.contains("#")) {
                    return (Set) nationPlaceholders.parseSet(store2, input);
                }
                return super.parseSet(store2, input);
            }

            @Override
            protected Set<NationOrAlliance> parseSingleElem(ValueStore store, String input) {
                Set<DBNation> selection2 = getSelection(nationPlaceholders, store, input, false);
                if (selection2 != null) return (Set) selection2;
                Set<DBAlliance> selection3 = getSelection(alliancePlaceholders, store, input, false);
                if (selection3 != null) return (Set) selection3;
                Set<NationOrAlliance> selection = getSelection(this, store, input, true);
                if (selection != null) return selection;
                if (SpreadSheet.isSheet(input)) {
                    return SpreadSheet.parseSheet(input, List.of("nation", "alliance"), true, (type, str) -> {
                        switch (type) {
                            case 0:
                                return PWBindings.nation(null, str);
                            case 1:
                                return PWBindings.alliance(str);
                        }
                        return null;
                    });
                }
                return nationOrAlliancesSingle(store, input, true);
            }

            @Override
            protected Predicate<NationOrAlliance> parseSingleFilter(ValueStore store, String input) {
                if (input.equalsIgnoreCase("*")) {
                    return f -> true;
                }
                Predicate<DBNation> predicate = nationPlaceholders.parseSingleFilter(store, input);
                return new Predicate<NationOrAlliance>() {
                    @Override
                    public boolean test(NationOrAlliance nationOrAlliance) {
                        if (nationOrAlliance.isNation()) {
                            return predicate.test(nationOrAlliance.asNation());
                        }
                        return false;
                    }
                };
            }
            @NoFormat
            @Command(desc = "Add an alias for a selection of NationOrAlliances")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addSelectionAlias(@Me JSONObject command, @Me GuildDB db, String name, Set<NationOrAlliance> nationoralliances) {
                return _addSelectionAlias(this, command, db, name, nationoralliances, "nationoralliances");
            }

            @NoFormat
            @Command(desc = "Add columns to a NationOrAlliance sheet")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addColumns(@Me JSONObject command, @Me GuildDB db, @Me IMessageIO io, @Me User author, @Switch("s") SheetTemplate sheet,
                                     @Default TypedFunction<NationOrAlliance, String> a,
                                     @Default TypedFunction<NationOrAlliance, String> b,
                                     @Default TypedFunction<NationOrAlliance, String> c,
                                     @Default TypedFunction<NationOrAlliance, String> d,
                                     @Default TypedFunction<NationOrAlliance, String> e,
                                     @Default TypedFunction<NationOrAlliance, String> f,
                                     @Default TypedFunction<NationOrAlliance, String> g,
                                     @Default TypedFunction<NationOrAlliance, String> h,
                                     @Default TypedFunction<NationOrAlliance, String> i,
                                     @Default TypedFunction<NationOrAlliance, String> j,
                                     @Default TypedFunction<NationOrAlliance, String> k,
                                     @Default TypedFunction<NationOrAlliance, String> l,
                                     @Default TypedFunction<NationOrAlliance, String> m,
                                     @Default TypedFunction<NationOrAlliance, String> n,
                                     @Default TypedFunction<NationOrAlliance, String> o,
                                     @Default TypedFunction<NationOrAlliance, String> p,
                                     @Default TypedFunction<NationOrAlliance, String> q,
                                     @Default TypedFunction<NationOrAlliance, String> r,
                                     @Default TypedFunction<NationOrAlliance, String> s,
                                     @Default TypedFunction<NationOrAlliance, String> t,
                                     @Default TypedFunction<NationOrAlliance, String> u,
                                     @Default TypedFunction<NationOrAlliance, String> v,
                                     @Default TypedFunction<NationOrAlliance, String> w,
                                     @Default TypedFunction<NationOrAlliance, String> x) throws GeneralSecurityException, IOException {
                return Placeholders._addColumns(this, command,db, io, author, sheet,
                        a, b, c, d, e, f, g, h, i, j,
                        k, l, m, n, o, p, q, r, s, t,
                        u, v, w, x);
            }

        };
    }

    private Set<NationOrAlliance> nationOrAlliancesSingle(ValueStore store, String input, boolean allowStar) {
        GuildDB db = (GuildDB) store.getProvided(Key.of(GuildDB.class, Me.class), false);
        DBNation me = (DBNation) store.getProvided(Key.of(DBNation.class, Me.class), false);
        if (input.equalsIgnoreCase("*") && allowStar) {
            return new ObjectOpenHashSet<>(Locutus.imp().getNationDB().getNations().values());
        }
        if (MathMan.isInteger(input)) {
            long id = Long.parseLong(input);
            if (id < Integer.MAX_VALUE) {
                int idInt = (int) id;
                DBAlliance alliance = DBAlliance.get(idInt);
                if (alliance != null) return Set.of(alliance);
                DBNation nation = DBNation.getById(idInt);
                if (nation != null) return Set.of(nation);
            } else {
                User user = Locutus.imp().getDiscordApi().getUserById(id);
                if (user != null) {
                    DBNation nation = DiscordUtil.getNation(user);
                    if (nation == null) {
                        throw new IllegalArgumentException("User `" + DiscordUtil.getFullUsername(user) + "` is not registered. See "
                                 + CM.register.cmd.toSlashMention()
                        );
                    }
                    return Set.of(nation);
                }
                if (db != null) {
                    Role role = db.getGuild().getRoleById(id);
                    if (role != null) {
                        return (Set) NationPlaceholders.getByRole(db.getGuild(), input, role);
                    }
                } else {
                    DBNation nation = DiscordUtil.getNation(id);
                    if (nation != null) {
                        return Set.of(nation);
                    }
                }
            }
        }
        Integer aaId = DNS.parseAllianceId(input);
        if (aaId != null) {
            return Set.of(DBAlliance.getOrCreate(aaId));
        }
        Integer nationId = DiscordUtil.parseNationId(input);
        if (nationId != null) {
            return Set.of(DBNation.getOrCreate(nationId));
        }
        if (input.charAt(0) == '~') input = input.substring(1);
        if (input.startsWith("coalition:")) input = input.substring("coalition:".length());
        if (input.startsWith("<@&") && db != null) {
            Role role = db.getGuild().getRoleById(input.substring(3, input.length() - 1));
            return (Set) NationPlaceholders.getByRole(db.getGuild(), input, role);
        }
        Set<Integer> coalition = db.getCoalition(input);
        if (!coalition.isEmpty()) {
            return coalition.stream().map(DBAlliance::getOrCreate).collect(Collectors.toSet());
        }
        if (db != null) {
            // get role by name
            String finalInput = input;
            Role role = db.getGuild().getRoles().stream().filter(f -> f.getName().equalsIgnoreCase(finalInput)).findFirst().orElse(null);
            if (role != null) {
                return (Set) NationPlaceholders.getByRole(db.getGuild(), input, role);
            }
            for (Member member : db.getGuild().getMembers()) {
                User user = member.getUser();
                DBNation nation = DiscordUtil.getNation(user);
                if (nation == null) continue;
                if (member.getEffectiveName().equalsIgnoreCase(input) || user.getName().equalsIgnoreCase(input) || input.equalsIgnoreCase(user.getGlobalName())) {
                    return Set.of(nation);
                }
            }
        }
        if (!MathMan.isInteger(input)) {
            String inputLower = input.toLowerCase(Locale.ROOT);
            String best = null;
            double bestScore = Double.MAX_VALUE;
            for (DBNation nation : Locutus.imp().getNationDB().getNations().values()) {
                String name = nation.getName();
                double score = StringMan.distanceWeightedQwertSift4(name.toLowerCase(Locale.ROOT), inputLower);
                if (score < bestScore) {
                    bestScore = score;
                    best = "nation:" + name;
                }
                String leader = nation.getLeader();
                if (leader != null) {
                    score = StringMan.distanceWeightedQwertSift4(leader.toLowerCase(Locale.ROOT), inputLower);
                    if (score < bestScore) {
                        bestScore = score;
                        best = "nation:" + nation.getName();
                    }
                }
            }
            for (DBAlliance alliance : Locutus.imp().getNationDB().getAlliances()) {
                String name = alliance.getName();
                double score = StringMan.distanceWeightedQwertSift4(name.toLowerCase(Locale.ROOT), inputLower);
                if (score < bestScore) {
                    bestScore = score;
                    best = "aa:" + name;
                }
            }
            if (best != null) {
                throw new IllegalArgumentException("Invalid nation or alliance: `" + input + "`. Did you mean: `" + best + "`?");
            }
        }
        throw new IllegalArgumentException("Invalid nation or alliance: `" + input + "`");
    }


    private Placeholders<GuildDB> createGuildDB() {
        return new SimplePlaceholders<GuildDB>(GuildDB.class, store, validators, permisser,
                "A discord guild",
                (ThrowingTriFunction<Placeholders<GuildDB>, ValueStore, String, Set<GuildDB>>) (inst, store, input) -> {
                    Set<GuildDB> selection = getSelection(inst, store, input);
                    if (selection != null) return selection;
                    User user = (User) store.getProvided(Key.of(User.class, Me.class), true);
                    boolean admin = Roles.ADMIN.hasOnRoot(user);
                    if (input.equalsIgnoreCase("*")) {
                        if (admin) {
                            return new HashSet<>(Locutus.imp().getGuildDatabases().values());
                        }
                        List<Guild> guilds = user.getMutualGuilds();
                        Set<GuildDB> dbs = new HashSet<>();
                        for (Guild guild : guilds) {
                            GuildDB db = Locutus.imp().getGuildDB(guild);
                            if (db != null) {
                                dbs.add(db);
                            }
                        }
                        return dbs;
                    }
                    if (SpreadSheet.isSheet(input)) {
                        return SpreadSheet.parseSheet(input, List.of("guild"), true,
                                (type, str) -> PWBindings.guild(PrimitiveBindings.Long(str)));
                    }
                    long id = PrimitiveBindings.Long(input);
                    GuildDB guild = PWBindings.guild(id);
                    if (!admin && guild.getGuild().getMember(user) == null) {
                        throw new IllegalArgumentException("You (" + user + ") are not in the guild with id: `" + id + "`");
                    }
                    return Set.of(guild);
                }, (ThrowingTriFunction<Placeholders<GuildDB>, ValueStore, String, Predicate<GuildDB>>) (inst, store, input) -> {
            if (input.equalsIgnoreCase("*")) {
                return f -> true;
            }
            if (SpreadSheet.isSheet(input)) {
                Set<Long> sheet = SpreadSheet.parseSheet(input, List.of("guild"), true,
                        (type, str) -> PrimitiveBindings.Long(str));
                return f -> sheet.contains(f.getIdLong());
            }
            long id = PrimitiveBindings.Long(input);
            return f -> f.getIdLong() == id;
        }, new Function<GuildDB, String>() {
            @Override
            public String apply(GuildDB guildDB) {
                return guildDB.getGuild().toString();
            }
        }) {
            @Override
            public Set<SelectorInfo> getSelectorInfo() {
                return new LinkedHashSet<>(List.of(
                        new SelectorInfo("GUILD", "123456789012345678", "Guild ID"),
                        new SelectorInfo("*", null, "All shared guilds")
                ));
            }

            @Override
            public Set<String> getSheetColumns() {
                return Set.of("guild");
            }

            @NoFormat
            @Command(desc = "Add an alias for a selection of guilds")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addSelectionAlias(@Me JSONObject command, @Me GuildDB db, String name, Set<GuildDB> guilds) {
                return _addSelectionAlias(this, command, db, name, guilds, "guilds");
            }

            @NoFormat
            @Command(desc = "Add columns to a Guild sheet")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addColumns(@Me JSONObject command, @Me GuildDB db, @Me IMessageIO io, @Me User author, @Switch("s") SheetTemplate sheet,
                                     @Default TypedFunction<GuildDB, String> a,
                                     @Default TypedFunction<GuildDB, String> b,
                                     @Default TypedFunction<GuildDB, String> c,
                                     @Default TypedFunction<GuildDB, String> d,
                                     @Default TypedFunction<GuildDB, String> e,
                                     @Default TypedFunction<GuildDB, String> f,
                                     @Default TypedFunction<GuildDB, String> g,
                                     @Default TypedFunction<GuildDB, String> h,
                                     @Default TypedFunction<GuildDB, String> i,
                                     @Default TypedFunction<GuildDB, String> j,
                                     @Default TypedFunction<GuildDB, String> k,
                                     @Default TypedFunction<GuildDB, String> l,
                                     @Default TypedFunction<GuildDB, String> m,
                                     @Default TypedFunction<GuildDB, String> n,
                                     @Default TypedFunction<GuildDB, String> o,
                                     @Default TypedFunction<GuildDB, String> p,
                                     @Default TypedFunction<GuildDB, String> q,
                                     @Default TypedFunction<GuildDB, String> r,
                                     @Default TypedFunction<GuildDB, String> s,
                                     @Default TypedFunction<GuildDB, String> t,
                                     @Default TypedFunction<GuildDB, String> u,
                                     @Default TypedFunction<GuildDB, String> v,
                                     @Default TypedFunction<GuildDB, String> w,
                                     @Default TypedFunction<GuildDB, String> x) throws GeneralSecurityException, IOException {
                return Placeholders._addColumns(this, command,db, io, author, sheet,
                        a, b, c, d, e, f, g, h, i, j,
                        k, l, m, n, o, p, q, r, s, t,
                        u, v, w, x);
            }
        };
    }

    private Placeholders<NationList> createNationList() {
        return new SimplePlaceholders<NationList>(NationList.class, store, validators, permisser,
                "One or more groups of nations",
                (ThrowingTriFunction<Placeholders<NationList>, ValueStore, String, Set<NationList>>) (inst, store, input) -> {
                    Set<NationList> selection = getSelection(inst, store, input);
                    if (selection != null) return selection;
                    Guild guild = (Guild) store.getProvided(Key.of(Guild.class, Me.class), false);
                    User author = (User) store.getProvided(Key.of(User.class, Me.class), false);
                    DBNation me = (DBNation) store.getProvided(Key.of(DBNation.class, Me.class), false);

                    if (SpreadSheet.isSheet(input)) {
                        Set<String> inputs = SpreadSheet.parseSheet(input, List.of("nations"), true, (type, str) -> str);
                        Set<NationList> lists = new HashSet<>();
                        for (String str : inputs) {
                            Set<DBNation> nations = PWBindings.nations(null, guild, str, author, me);
                            lists.add(new SimpleNationList(nations).setFilter(str));
                        }
                        return lists;
                    }
                    Predicate<DBNation> filter = null;
                    String filterName = "";
                    int index = input.indexOf('[');
                    if (index != -1) {
                        String filterStr = input.substring(index + 1, input.length() - 1);
                        filterName = "[" + filterStr + "]";
                        input = input.substring(0, index);
                        NationPlaceholders placeholders = (NationPlaceholders) get(DBNation.class);
                        filter = placeholders.parseFilter(store, filterStr);
                    }

                    List<NationList> lists = new ArrayList<>();

                    if (input.isEmpty() || input.equalsIgnoreCase("*")) {
                        Set<DBAlliance> alliances = Locutus.imp().getNationDB().getAlliances();
                        for (DBAlliance alliance : alliances) {
                            lists.add(new SimpleNationList(alliance.getNations(filter)).setFilter(filterName));
                        }
                    } else if (input.equalsIgnoreCase("~")) {
                        GuildDB db = guild == null ? null : Locutus.imp().getGuildDB(guild);
                        if (db == null) {
                            db = Locutus.imp().getRootCoalitionServer();
                        }
                        if (db == null) {
                            throw new IllegalArgumentException("No coalition server found, please have the bot owner set one in the `config.yaml`");
                        }
                        for (String coalition : db.getCoalitionNames()) {
                            lists.add(new SimpleNationList(Locutus.imp().getNationDB().getNations(db.getCoalition(coalition))).setFilter(filterName));
                        }
                    } else {
                        NationPlaceholders placeholders = (NationPlaceholders) get(DBNation.class);
                        lists.add(new SimpleNationList(placeholders.parseSet(store, input)).setFilter(filterName));
                    }
                    Set<NationList> result = new HashSet<>();
                    if (filter != null) {
                        for (NationList list : lists) {
                            List<DBNation> newNations = list.getNations().stream().filter(filter).toList();
                            if (!newNations.isEmpty()) {
                                result.add(new SimpleNationList(newNations).setFilter(list.getFilter()));
                            }
                        }
                    } else {
                        result.addAll(lists);
                    }
                    return result;
                }, (ThrowingTriFunction<Placeholders<NationList>, ValueStore, String, Predicate<NationList>>) (inst, store, input) -> {
            if (input.equalsIgnoreCase("*")) return f -> true;
            throw new IllegalArgumentException("NationList predicates other than `*` are unsupported. Please use DBNation instead");
        }, new Function<NationList, String>() {
            @Override
            public String apply(NationList nationList) {
                return nationList.getFilter();
            }
        }) {
            @Override
            public Set<SelectorInfo> getSelectorInfo() {
                return new LinkedHashSet<>(List.of(
                        new SelectorInfo("*", null, "A single list with all nations"),
                        new SelectorInfo("~", null, "A set of nation lists for each coalition in the server"),
                        new SelectorInfo("coalition:COALITION_NAME", "coalition:allies", "A single list with the nations in the coalition"),
                        new SelectorInfo("NATION", "Borg", "Nation name, id, leader, url, user id or mention (see nation type)"),
                        new SelectorInfo("ALLIANCE", "AA:Rose", "Alliance id, name, url or mention (see alliance type)"),
                        new SelectorInfo("SELECTOR[FILTER]", "`*[#land>10]`, `AA:Rose[#position>1,#vm_turns=0]`", "A single nation list based on a selector with an optional filter")
                ));
            }

            @Override
            public Set<String> getSheetColumns() {
                return Set.of("nations");
            }

            @NoFormat
            @Command(desc = "Add an alias for a selection of nationlists")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addSelectionAlias(@Me JSONObject command, @Me GuildDB db, String name, Set<NationList> nationlists) {
                return _addSelectionAlias(this, command, db, name, nationlists, "nationlists");
            }

            @NoFormat
            @Command(desc = "Add columns to a NationList sheet")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addColumns(@Me JSONObject command, @Me GuildDB db, @Me IMessageIO io, @Me User author, @Switch("s") SheetTemplate sheet,
                                     @Default TypedFunction<NationList, String> a,
                                     @Default TypedFunction<NationList, String> b,
                                     @Default TypedFunction<NationList, String> c,
                                     @Default TypedFunction<NationList, String> d,
                                     @Default TypedFunction<NationList, String> e,
                                     @Default TypedFunction<NationList, String> f,
                                     @Default TypedFunction<NationList, String> g,
                                     @Default TypedFunction<NationList, String> h,
                                     @Default TypedFunction<NationList, String> i,
                                     @Default TypedFunction<NationList, String> j,
                                     @Default TypedFunction<NationList, String> k,
                                     @Default TypedFunction<NationList, String> l,
                                     @Default TypedFunction<NationList, String> m,
                                     @Default TypedFunction<NationList, String> n,
                                     @Default TypedFunction<NationList, String> o,
                                     @Default TypedFunction<NationList, String> p,
                                     @Default TypedFunction<NationList, String> q,
                                     @Default TypedFunction<NationList, String> r,
                                     @Default TypedFunction<NationList, String> s,
                                     @Default TypedFunction<NationList, String> t,
                                     @Default TypedFunction<NationList, String> u,
                                     @Default TypedFunction<NationList, String> v,
                                     @Default TypedFunction<NationList, String> w,
                                     @Default TypedFunction<NationList, String> x) throws GeneralSecurityException, IOException {
                return Placeholders._addColumns(this, command,db, io, author, sheet,
                        a, b, c, d, e, f, g, h, i, j,
                        k, l, m, n, o, p, q, r, s, t,
                        u, v, w, x);
            }
        };
    }

    private Set<UserWrapper> parseUserSingle(Guild guild, String input) {
        // *
        if (input.equalsIgnoreCase("*")) {
            return guild.getMembers().stream().map(UserWrapper::new).collect(Collectors.toSet());
        }
        // username
        List<Member> members = guild.getMembersByName(input, true);
        if (!members.isEmpty()) {
            return members.stream().map(UserWrapper::new).collect(Collectors.toSet());
        }
        // user id / mention
        User user = DiscordUtil.getUser(input);
        if (user != null) {
            return new LinkedHashSet<>(List.of(new UserWrapper(guild, user)));
        }
        // Role
        Role role = DiscordUtil.getRole(guild, input);
        if (role != null) {
            return guild.getMembersWithRoles(role).stream().map(UserWrapper::new).collect(Collectors.toSet());
        }
        NationOrAlliance natOrAA = PWBindings.nationOrAlliance(input);
        if (natOrAA.isNation()) {
            user = natOrAA.asNation().getUser();
            if (user == null) {
                throw new IllegalArgumentException("Nation " + natOrAA.getMarkdownUrl() + " is not registered. See: " + CM.register.cmd.toSlashMention());
            }
            return new LinkedHashSet<>(List.of(new UserWrapper(guild, user)));
        }
        return natOrAA.asAlliance().getNations().stream().map(f -> {
            Long id = f.getUserId();
            return id != null ? guild.getMemberById(id) : null;
        }).filter(Objects::nonNull).map(UserWrapper::new).collect(Collectors.toSet());
    }

    private Predicate<UserWrapper> parseUserPredicate(Guild guild, String input) {
        boolean canRole;
        boolean canUser;
        if (input.startsWith("<@&")) {
            canRole = true;
            canUser = false;
            input = input.substring(3, input.length() - 1);
        } else {
            canUser = true;
            if (input.startsWith("<@!") || input.startsWith("<@")) {
                canRole = false;
                input = input.replace("!", "");
                input = input.substring(2, input.length() - 1);
            } else {
                canRole = true;
            }
        }
        if (MathMan.isInteger(input)) {
            long id = Long.parseLong(input);
            if (id > Integer.MAX_VALUE) {
                return f -> {
                    if (canUser && f.getUserId() == id) return true;
                    if (canRole) {
                        Member member = f.getMember();
                        if (member != null) {
                            for (Role role : member.getRoles()) {
                                if (role.getIdLong() == id) return true;
                            }
                        }
                    }
                    return false;
                };
            }
            int intId = (int) id;
            return f -> {
                DBNation nation = f.getNation();
                if (nation != null) {
                    return nation.getId() == intId || nation.getAlliance_id() == intId;
                }
                return false;
            };
        }
        Long id = DiscordUtil.parseUserId(guild, input);
        if (id != null) {
            return f -> f.getUserId() == id;
        }
        Integer nationId = DiscordUtil.parseNationId(input);
        if (nationId != null) {
            return f -> {
                DBNation nation = f.getNation();
                if (nation != null) {
                    return nation.getId() == nationId;
                }
                return false;
            };
        }
        Set<Integer> allianceId = DiscordUtil.parseAllianceIds(guild, input);
        if (allianceId != null && !allianceId.isEmpty()) {
            return f -> {
                DBNation nation = f.getNation();
                if (nation != null) {
                    return allianceId.contains(nation.getAlliance_id());
                }
                return false;
            };
        }
        String finalInput = input;
        return f -> {
            Member member = f.getMember();
            if (member != null) {
                for (Role role : member.getRoles()) {
                    if (role.getName().equalsIgnoreCase(finalInput)) return true;
                }
            }
            return false;
        };
    }

    private Placeholders<UserWrapper> createUsers() {
        return new SimplePlaceholders<UserWrapper>(UserWrapper.class, store, validators, permisser,
                "A discord user",
                (ThrowingTriFunction<Placeholders<UserWrapper>, ValueStore, String, Set<UserWrapper>>) (inst, store, input) -> {
                    Set<UserWrapper> selection = getSelection(inst, store, input);
                    if (selection != null) return selection;
                    GuildDB db = (GuildDB) store.getProvided(Key.of(GuildDB.class, Me.class), true);
                    Guild guild = db.getGuild();
                    if (SpreadSheet.isSheet(input)) {
                        Set<Member> member = SpreadSheet.parseSheet(input, List.of("user"), true, (type, str) -> DiscordBindings.member(guild, null, str));
                        return member.stream().map(UserWrapper::new).collect(Collectors.toSet());
                    }
                    return parseUserSingle(guild, input);
                }, (ThrowingTriFunction<Placeholders<UserWrapper>, ValueStore, String, Predicate<UserWrapper>>) (inst, store, input) -> {
            if (input.equalsIgnoreCase("*")) return f -> true;

            GuildDB db = (GuildDB) store.getProvided(Key.of(GuildDB.class, Me.class), true);
            Guild guild = db.getGuild();

            if (SpreadSheet.isSheet(input)) {
                Set<Long> sheet = SpreadSheet.parseSheet(input, List.of("user"), true,
                        (type, str) -> DiscordUtil.parseUserId(guild, str));
                return f -> sheet.contains(f.getUserId());
            }
            return parseUserPredicate(guild, input);
        }, new Function<UserWrapper, String>() {
            @Override
            public String apply(UserWrapper userWrapper) {
                return userWrapper.getUserName();
            }
        }) {
            @Override
            public Set<SelectorInfo> getSelectorInfo() {
                return new LinkedHashSet<>(List.of(
                        new SelectorInfo("USER", "Borg", "Discord user name"),
                        new SelectorInfo("USER_ID", "123456789012345678", "Discord user id"),
                        new SelectorInfo("@ROLE", "@Member", "All users with a discord role by a given name or mention"),
                        new SelectorInfo("ROLE_ID", "123456789012345678", "All users with the discord role by a given id"),
                        new SelectorInfo("NATION", "Borg", "Nation name, id, leader, url, user id or mention (see nation type) - only if registered with Locutus"),
                        new SelectorInfo("ALLIANCE", "AA:Rose", "Alliance id, name, url or mention (see alliance type), resolves to the users registered with Locutus"),
                        new SelectorInfo("*", null, "All shared users")
                ));
            }

            @Override
            public Set<String> getSheetColumns() {
                return Set.of("user");
            }

            @NoFormat
            @Command(desc = "Add an alias for a selection of users")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addSelectionAlias(@Me JSONObject command, @Me GuildDB db, String name, Set<UserWrapper> users) {
                return _addSelectionAlias(this, command, db, name, users, "users");
            }

            @NoFormat
            @Command(desc = "Add columns to a User sheet")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addColumns(@Me JSONObject command, @Me GuildDB db, @Me IMessageIO io, @Me User author, @Switch("s") SheetTemplate sheet,
                                     @Default TypedFunction<UserWrapper, String> a,
                                     @Default TypedFunction<UserWrapper, String> b,
                                     @Default TypedFunction<UserWrapper, String> c,
                                     @Default TypedFunction<UserWrapper, String> d,
                                     @Default TypedFunction<UserWrapper, String> e,
                                     @Default TypedFunction<UserWrapper, String> f,
                                     @Default TypedFunction<UserWrapper, String> g,
                                     @Default TypedFunction<UserWrapper, String> h,
                                     @Default TypedFunction<UserWrapper, String> i,
                                     @Default TypedFunction<UserWrapper, String> j,
                                     @Default TypedFunction<UserWrapper, String> k,
                                     @Default TypedFunction<UserWrapper, String> l,
                                     @Default TypedFunction<UserWrapper, String> m,
                                     @Default TypedFunction<UserWrapper, String> n,
                                     @Default TypedFunction<UserWrapper, String> o,
                                     @Default TypedFunction<UserWrapper, String> p,
                                     @Default TypedFunction<UserWrapper, String> q,
                                     @Default TypedFunction<UserWrapper, String> r,
                                     @Default TypedFunction<UserWrapper, String> s,
                                     @Default TypedFunction<UserWrapper, String> t,
                                     @Default TypedFunction<UserWrapper, String> u,
                                     @Default TypedFunction<UserWrapper, String> v,
                                     @Default TypedFunction<UserWrapper, String> w,
                                     @Default TypedFunction<UserWrapper, String> x) throws GeneralSecurityException, IOException {
                return Placeholders._addColumns(this, command,db, io, author, sheet,
                        a, b, c, d, e, f, g, h, i, j,
                        k, l, m, n, o, p, q, r, s, t,
                        u, v, w, x);
            }
        };

    }

    private Predicate<BankTransfer> getAllowed(DBNation nation, User user, GuildDB db) {
        Predicate<Integer> allowAlliance;
        if (user != null && db != null) {
            Set<Integer> aaIds = db.getAllianceIds();
            boolean canSee = Roles.hasAny(user, db.getGuild(), Roles.ECON_STAFF, Roles.INTERNAL_AFFAIRS);
            if (canSee) {
                allowAlliance = aaIds::contains;
            } else {
                allowAlliance = f -> false;
            }
        } else {
            allowAlliance = f -> false;
        }
        return f -> {
            if (f.isSenderNation() || f.isReceiverNation()) {
                return false;
            }
            if (nation != null && f.banker_nation == nation.getId()) {
                return false;
            }
            boolean allowSender = allowAlliance.test((int) f.getSender());
            boolean allowReceiver = allowAlliance.test((int) f.getReceiver());
            return allowSender || allowReceiver;
        };
    }

    private Set<BankTransfer> filterTransactions(DBNation nation, User user, GuildDB db, List<BankTransfer> records) {
        Predicate<BankTransfer> filter = getAllowed(nation, user, db);
        Set<BankTransfer> result = new ObjectLinkedOpenHashSet<>(records.size());
        for (BankTransfer record : records) {
            if (filter.test(record)) {
                result.add(record);
            }
        }
        return result;
    }

    public Placeholders<Conflict> createConflicts() {
        return new SimplePlaceholders<Conflict>(Conflict.class, store, validators, permisser,
                "Public and registered alliance conflicts added to the bot\n" +
                        "Unlisted conflicts are not supported by conflict selectors",
                (ThrowingTriFunction<Placeholders<Conflict>, ValueStore, String, Set<Conflict>>) (inst, store, input) -> {
                    Set<Conflict> selection = getSelection(inst, store, input);
                    if (selection != null) return selection;
                    ConflictManager manager = Locutus.imp().getWarDb().getConflicts();
                    if (input.equalsIgnoreCase("*")) {
                        return new LinkedHashSet<>(manager.getConflictMap().values());
                    }
                    if (SpreadSheet.isSheet(input)) {
                        return SpreadSheet.parseSheet(input, List.of("conflict"), true, (type, str) -> PWBindings.conflict(manager, str));
                    }
                    return Set.of(PWBindings.conflict(manager, input));
                }, (ThrowingTriFunction<Placeholders<Conflict>, ValueStore, String, Predicate<Conflict>>) (inst, store, input) -> {
            if (input.equalsIgnoreCase("*")) return f -> true;
            ConflictManager cMan = Locutus.imp().getWarDb().getConflicts();
            if (SpreadSheet.isSheet(input)) {
                Set<Conflict> conflicts = SpreadSheet.parseSheet(input, List.of("conflict"), true, (type, str) -> PWBindings.conflict(cMan, str));
                Set<Integer> ids = conflicts.stream().map(Conflict::getId).collect(Collectors.toSet());
                return f -> ids.contains(f.getId());
            }
            Conflict setting = PWBindings.conflict(cMan, input);
            return f -> f == setting;
        }, new Function<Conflict, String>() {
            @Override
            public String apply(Conflict conflict) {
                return conflict.getId() + "";
            }
        }) {
            @Override
            public Set<SelectorInfo> getSelectorInfo() {
                return new LinkedHashSet<>(List.of(
                        new SelectorInfo("CONFLICT_ID", "12345", "Public Conflict ID"),
                        new SelectorInfo("CONFLICT_NAME", "Duck Hunt", "Public Conflict name, as stored by the bot"),
                        new SelectorInfo("*", null, "All public conflicts")
                ));
            }

            @Override
            public Set<String> getSheetColumns() {
                return Set.of("conflict");
            }
        };
    }

    public Placeholders<GuildSetting> createGuildSettings() {
        return new SimplePlaceholders<GuildSetting>(GuildSetting.class, store, validators, permisser,
                "A bot setting in a guild",
                (ThrowingTriFunction<Placeholders<GuildSetting>, ValueStore, String, Set<GuildSetting>>) (inst, store, input) -> {
                    Set<GuildSetting> selection = getSelection(inst, store, input);
                    if (selection != null) return selection;
                    if (input.equalsIgnoreCase("*")) {
                        return new LinkedHashSet<>(Arrays.asList(GuildKey.values()));
                    }
                    if (SpreadSheet.isSheet(input)) {
                        return SpreadSheet.parseSheet(input, List.of("setting"), true, (type, str) -> PWBindings.key(str));
                    }
                    return Set.of(PWBindings.key(input));
                }, (ThrowingTriFunction<Placeholders<GuildSetting>, ValueStore, String, Predicate<GuildSetting>>) (inst, store, input) -> {
            if (input.equalsIgnoreCase("*")) return f -> true;
            if (SpreadSheet.isSheet(input)) {
                Set<GuildSetting> settings = SpreadSheet.parseSheet(input, List.of("setting"), true, (type, str) -> PWBindings.key(str));
                Set<GuildSetting> ids = new ObjectOpenHashSet<>(settings);
                return ids::contains;
            }
            GuildSetting setting = PWBindings.key(input);
            return f -> f == setting;
        }, new Function<GuildSetting, String>() {
            @Override
            public String apply(GuildSetting setting) {
                return setting.name();
            }
        }) {
            @Override
            public Set<SelectorInfo> getSelectorInfo() {
                return new LinkedHashSet<>(List.of(
                        new SelectorInfo("SETTING", GuildKey.ALLIANCE_ID.name(), "Guild setting name"),
                        new SelectorInfo("*", null, "All guild settings")
                ));
            }

            @Override
            public Set<String> getSheetColumns() {
                return Set.of("setting");
            }

            @NoFormat
            @Command(desc = "Add an alias for a selection of guild settings")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addSelectionAlias(@Me JSONObject command, @Me GuildDB db, String name, Set<GuildSetting> settings) {
                return _addSelectionAlias(this, command, db, name, settings, "settings");
            }

            @NoFormat
            @Command(desc = "Add columns to a Guild Setting sheet")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addColumns(@Me JSONObject command, @Me GuildDB db, @Me IMessageIO io, @Me User author, @Switch("s") SheetTemplate sheet,
                                     @Default TypedFunction<GuildSetting, String> a,
                                     @Default TypedFunction<GuildSetting, String> b,
                                     @Default TypedFunction<GuildSetting, String> c,
                                     @Default TypedFunction<GuildSetting, String> d,
                                     @Default TypedFunction<GuildSetting, String> e,
                                     @Default TypedFunction<GuildSetting, String> f,
                                     @Default TypedFunction<GuildSetting, String> g,
                                     @Default TypedFunction<GuildSetting, String> h,
                                     @Default TypedFunction<GuildSetting, String> i,
                                     @Default TypedFunction<GuildSetting, String> j,
                                     @Default TypedFunction<GuildSetting, String> k,
                                     @Default TypedFunction<GuildSetting, String> l,
                                     @Default TypedFunction<GuildSetting, String> m,
                                     @Default TypedFunction<GuildSetting, String> n,
                                     @Default TypedFunction<GuildSetting, String> o,
                                     @Default TypedFunction<GuildSetting, String> p,
                                     @Default TypedFunction<GuildSetting, String> q,
                                     @Default TypedFunction<GuildSetting, String> r,
                                     @Default TypedFunction<GuildSetting, String> s,
                                     @Default TypedFunction<GuildSetting, String> t,
                                     @Default TypedFunction<GuildSetting, String> u,
                                     @Default TypedFunction<GuildSetting, String> v,
                                     @Default TypedFunction<GuildSetting, String> w,
                                     @Default TypedFunction<GuildSetting, String> x) throws GeneralSecurityException, IOException {
                return Placeholders._addColumns(this, command,db, io, author, sheet,
                        a, b, c, d, e, f, g, h, i, j,
                        k, l, m, n, o, p, q, r, s, t,
                        u, v, w, x);
            }
        };
    }

    public Placeholders<DBWar> createWars() {
        return new SimplePlaceholders<DBWar>(DBWar.class, store, validators, permisser,
                "A war",
                (ThrowingTriFunction<Placeholders<DBWar>, ValueStore, String, Set<DBWar>>) (inst, store, input) -> {
                    Set<DBWar> selection = getSelection(inst, store, input);
                    if (selection != null) return selection;
                    if (SpreadSheet.isSheet(input)) {
                        Set<Integer> warIds = new IntOpenHashSet();
                        Set<Integer> nationIds = new IntOpenHashSet();
                        Set<Integer> allianceIds = new IntOpenHashSet();
                        SpreadSheet.parseSheet(input, List.of("id", "war_id", "nation", "leader", "alliance"), true, (type, str) -> {
                            switch (type) {
                                case 0, 1 -> warIds.add(Integer.parseInt(str));
                                case 2 -> {
                                    DBNation nation = DiscordUtil.parseNation(str, true);
                                    if (nation == null) throw new IllegalArgumentException("Invalid nation: `" + str + "`");
                                    nationIds.add(nation.getId());
                                }
                                case 3 -> {
                                    DBNation nation = Locutus.imp().getNationDB().getNationByLeader(str);
                                    if (nation == null) throw new IllegalArgumentException("Invalid nation leader: `" + str + "`");
                                    nationIds.add(nation.getId());
                                }
                                case 4 -> {
                                    DBAlliance alliance = PWBindings.alliance(str);
                                    if (alliance == null) throw new IllegalArgumentException("Invalid alliance: `" + str + "`");
                                    allianceIds.add(alliance.getId());
                                }
                            }
                            return null;
                        });
                        if (!warIds.isEmpty()) {
                            return Locutus.imp().getWarDb().getWarsById(warIds);
                        }
                    }
                    if (MathMan.isInteger(input)) {
                        int id = Integer.parseInt(input);
                        return Locutus.imp().getWarDb().getWarsById(Set.of(id));
                    }
                    if (input.contains("/war/")) {
                        int warId = Integer.parseInt(input.substring(input.indexOf("/war/") + 5));
                        return Locutus.imp().getWarDb().getWarsById(Set.of(warId));
                    }
                    Set<NationOrAlliance> natOrAA = nationOrAlliancesSingle(store, input, false);
                    Set<Integer> natIds = natOrAA.stream().filter(NationOrAlliance::isNation).map(NationOrAlliance::getId).collect(Collectors.toSet());
                    Set<Integer> aaIds = natOrAA.stream().filter(NationOrAlliance::isAlliance).map(NationOrAlliance::getId).collect(Collectors.toSet());
                    return Locutus.imp().getWarDb().getWarsForNationOrAlliance(natIds, aaIds);
                }, (ThrowingTriFunction<Placeholders<DBWar>, ValueStore, String, Predicate<DBWar>>) (inst, store, input) -> {
            if (input.equalsIgnoreCase("*")) return f -> true;
            if (SpreadSheet.isSheet(input)) {
                Set<Integer> warIds = new IntOpenHashSet();
                SpreadSheet.parseSheet(input, List.of("id", "war_id"), true, (type, str) -> {
                    switch (type) {
                        case 0, 1 -> warIds.add(Integer.parseInt(str));
                    }
                    return null;
                });
                if (!warIds.isEmpty()) {
                    return f -> warIds.contains(f.getWarId());
                }
            }
            if (input.contains("/war/")) {
                int id = Integer.parseInt(input.substring(input.indexOf("/war/") + 5));
                return f -> f.getWarId() == id;
            }
            if (MathMan.isInteger(input)) {
                int id = Integer.parseInt(input);
                return f -> f.getWarId() == id;
            }
            Guild guild = (Guild) store.getProvided(Key.of(Guild.class, Me.class), false);
            User author = (User) store.getProvided(Key.of(User.class, Me.class), false);
            DBNation me = (DBNation) store.getProvided(Key.of(DBNation.class, Me.class), false);
            Set<NationOrAlliance> allowed = PWBindings.nationOrAlliance(null, guild, input, true, author, me);
            return war -> {
                DBNation attacker = DBNation.getOrCreate(war.getAttacker_id());
                DBNation defender = DBNation.getOrCreate(war.getDefender_id());
                if (allowed.contains(attacker) || allowed.contains(defender)) return true;
                DBAlliance attackerAA = war.getAttacker_aa() != 0 ? DBAlliance.getOrCreate(war.getAttacker_aa()) : null;
                if (attackerAA != null && allowed.contains(attackerAA)) return true;
                DBAlliance defenderAA = war.getDefender_aa() != 0 ? DBAlliance.getOrCreate(war.getDefender_aa()) : null;
                if (defenderAA != null && allowed.contains(defenderAA)) return true;
                return false;
            };
        }, new Function<DBWar, String>() {
            @Override
            public String apply(DBWar dbWar) {
                return dbWar.getWarId() + "";
            }
        }) {
            @Override
            public Set<SelectorInfo> getSelectorInfo() {
                return new LinkedHashSet<>(List.of(
                        new SelectorInfo("WAR_ID", "12345", "War ID"),
                        new SelectorInfo("NATION", "Borg", "Nation name, id, leader, url, user id or mention (see nation type)"),
                        new SelectorInfo("ALLIANCE", "AA:Rose", "Alliance id, name, url or mention (see alliance type)"),
                        new SelectorInfo("*", null, "All wars")
                ));
            }

            @Override
            public Set<String> getSheetColumns() {
                return new LinkedHashSet<>(List.of("id", "war_id", "nation", "leader", "alliance"));
            }

            @NoFormat
            @Command(desc = "Add an alias for a selection of wars")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addSelectionAlias(@Me JSONObject command, @Me GuildDB db, String name, Set<DBWar> wars) {
                return _addSelectionAlias(this, command, db, name, wars, "wars");
            }
            @NoFormat
            @Command(desc = "Add columns to a War sheet")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addColumns(@Me JSONObject command, @Me GuildDB db, @Me IMessageIO io, @Me User author, @Switch("s") SheetTemplate sheet,
                                     @Default TypedFunction<DBWar, String> a,
                                     @Default TypedFunction<DBWar, String> b,
                                     @Default TypedFunction<DBWar, String> c,
                                     @Default TypedFunction<DBWar, String> d,
                                     @Default TypedFunction<DBWar, String> e,
                                     @Default TypedFunction<DBWar, String> f,
                                     @Default TypedFunction<DBWar, String> g,
                                     @Default TypedFunction<DBWar, String> h,
                                     @Default TypedFunction<DBWar, String> i,
                                     @Default TypedFunction<DBWar, String> j,
                                     @Default TypedFunction<DBWar, String> k,
                                     @Default TypedFunction<DBWar, String> l,
                                     @Default TypedFunction<DBWar, String> m,
                                     @Default TypedFunction<DBWar, String> n,
                                     @Default TypedFunction<DBWar, String> o,
                                     @Default TypedFunction<DBWar, String> p,
                                     @Default TypedFunction<DBWar, String> q,
                                     @Default TypedFunction<DBWar, String> r,
                                     @Default TypedFunction<DBWar, String> s,
                                     @Default TypedFunction<DBWar, String> t,
                                     @Default TypedFunction<DBWar, String> u,
                                     @Default TypedFunction<DBWar, String> v,
                                     @Default TypedFunction<DBWar, String> w,
                                     @Default TypedFunction<DBWar, String> x) throws GeneralSecurityException, IOException {
                return Placeholders._addColumns(this, command,db, io, author, sheet,
                        a, b, c, d, e, f, g, h, i, j,
                        k, l, m, n, o, p, q, r, s, t,
                        u, v, w, x);
            }
        };
    }

    private Placeholders<Treaty> createTreaty() {
        return new SimplePlaceholders<Treaty>(Treaty.class, store, validators, permisser,
                "A treaty between two alliances",
                (ThrowingTriFunction<Placeholders<Treaty>, ValueStore, String, Set<Treaty>>) (inst, store, input) -> {
                    Set<Treaty> selection = getSelection(inst, store, input);
                    if (selection != null) return selection;
                    if (input.equalsIgnoreCase("*")) {
                        return Locutus.imp().getNationDB().getTreaties();
                    }
                    if (SpreadSheet.isSheet(input)) {
                        return SpreadSheet.parseSheet(input, List.of("treaty"), true, (type, str) -> PWBindings.treaty(str));
                    }
                    Guild guild = (Guild) store.getProvided(Key.of(Guild.class, Me.class), false);
                    GuildDB db = guild == null ? null : Locutus.imp().getGuildDB(guild);
                    List<String> split = StringMan.split(input, (s, index) -> switch (s.charAt(index)) {
                        case ':', '>', '<' -> 1;
                        default -> null;
                    }, 2);
                    if (split.size() != 2) {
                        throw new IllegalArgumentException("Invalid treaty format: `" + input + "`");
                    }
                    Set<Integer> aa1 = DiscordUtil.parseAllianceIds(guild, split.get(0), true);
                    Set<Integer> aa2 = DiscordUtil.parseAllianceIds(guild, split.get(1), true);
                    if (aa1 == null)
                        throw new IllegalArgumentException("Invalid alliance or coalition: `" + split.get(0) + "`");
                    if (aa2 == null)
                        throw new IllegalArgumentException("Invalid alliance or coalition: `" + split.get(1) + "`");
                    return Locutus.imp().getNationDB().getTreatiesMatching(f -> {
                        return (aa1.contains(f.getFromId())) && (aa2.contains(f.getToId())) || (aa1.contains(f.getToId())) && (aa2.contains(f.getFromId()));
                    });
                }, (ThrowingTriFunction<Placeholders<Treaty>, ValueStore, String, Predicate<Treaty>>) (inst, store, input) -> {
            if (input.equalsIgnoreCase("*")) return f -> true;
            if (SpreadSheet.isSheet(input)) {
                Set<Treaty> sheet = SpreadSheet.parseSheet(input, List.of("treaty"), true,
                        (type, str) -> PWBindings.treaty(str));

                Map<Integer, Set<Integer>> treatyIds = new HashMap<>();
                for (Treaty treaty : sheet) {
                    treatyIds.computeIfAbsent(treaty.getFromId(), k -> new HashSet<>()).add(treaty.getToId());
                    treatyIds.computeIfAbsent(treaty.getToId(), k -> new HashSet<>()).add(treaty.getFromId());
                }
                return f -> treatyIds.getOrDefault(f.getFromId(), Collections.emptySet()).contains(f.getToId());
            }
            Guild guild = (Guild) store.getProvided(Key.of(Guild.class, Me.class), false);
            GuildDB db = guild == null ? null : Locutus.imp().getGuildDB(guild);
            List<String> split = StringMan.split(input, (s, index) -> switch (s.charAt(index)) {
                case ':', '>', '<' -> 1;
                default -> null;
            }, 2);
            if (split.size() != 2) {
                throw new IllegalArgumentException("Invalid treaty format: `" + input + "`");
            }
            Set<Integer> aa1 = DiscordUtil.parseAllianceIds(guild, split.get(0), false);
            Long coalitionId1 = aa1 != null || db == null ? null : db.getCoalitionId(split.get(0), true);

            Set<Integer> aa2 = DiscordUtil.parseAllianceIds(guild, split.get(1), false);
            Long coalitionId2 = aa2 != null || db == null ? null : db.getCoalitionId(split.get(1), true);

            if (aa1 == null && coalitionId2 == null) {
                throw new IllegalArgumentException("Invalid treaty alliance or coalition: `" + split.get(0) + "`");
            }
            if (aa2 == null && coalitionId2 == null) {
                throw new IllegalArgumentException("Invalid treaty alliance or coalition: `" + split.get(1) + "`");
            }

            Predicate<Integer> contains1 = f -> {
                if (aa1 != null) {
                    return aa1.contains(f);
                } else {
                    return db.getCoalitionById(coalitionId1).contains(f);
                }
            };
            Predicate<Integer> contains2 = f -> {
                if (aa2 != null) {
                    return aa2.contains(f);
                } else {
                    return db.getCoalitionById(coalitionId2).contains(f);
                }
            };
            return f -> (contains1.test(f.getFromId()) && contains2.test(f.getToId()))
                    || (contains1.test(f.getToId()) && contains2.test(f.getFromId()));
        }, new Function<Treaty, String>() {
            @Override
            public String apply(Treaty treaty) {
                return treaty.toString();
            }
        }) {
            @Override
            public Set<SelectorInfo> getSelectorInfo() {
                return new LinkedHashSet<>(List.of(
                        new SelectorInfo("ALLIANCES:ALLIANCES", "`Rose:Eclipse`, `Rose,Eclipse:~allies`", "A treaty between two sets of alliances or coalitions (direction agnostic)"),
                        new SelectorInfo("ALLIANCES>ALLIANCES", "Rose>Eclipse", "A treaty from one alliance or coalition to another"),
                        new SelectorInfo("ALLIANCES<ALLIANCES", "Rose<Eclipse", "A treaty from one alliance or coalition to another"),
                        new SelectorInfo("*", null, "All treaties")
                ));
            }

            @Override
            public Set<String> getSheetColumns() {
                return Set.of("treaty");
            }

            @NoFormat
            @Command(desc = "Add an alias for a selection of treaties")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addSelectionAlias(@Me JSONObject command, @Me GuildDB db, String name, Set<Treaty> treaties) {
                return _addSelectionAlias(this, command, db, name, treaties, "treaties");
            }

            @NoFormat
            @Command(desc = "Add columns to a Treaty sheet")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addColumns(@Me JSONObject command, @Me GuildDB db, @Me IMessageIO io, @Me User author, @Switch("s") SheetTemplate sheet,
                                     @Default TypedFunction<Treaty, String> a,
                                     @Default TypedFunction<Treaty, String> b,
                                     @Default TypedFunction<Treaty, String> c,
                                     @Default TypedFunction<Treaty, String> d,
                                     @Default TypedFunction<Treaty, String> e,
                                     @Default TypedFunction<Treaty, String> f,
                                     @Default TypedFunction<Treaty, String> g,
                                     @Default TypedFunction<Treaty, String> h,
                                     @Default TypedFunction<Treaty, String> i,
                                     @Default TypedFunction<Treaty, String> j,
                                     @Default TypedFunction<Treaty, String> k,
                                     @Default TypedFunction<Treaty, String> l,
                                     @Default TypedFunction<Treaty, String> m,
                                     @Default TypedFunction<Treaty, String> n,
                                     @Default TypedFunction<Treaty, String> o,
                                     @Default TypedFunction<Treaty, String> p,
                                     @Default TypedFunction<Treaty, String> q,
                                     @Default TypedFunction<Treaty, String> r,
                                     @Default TypedFunction<Treaty, String> s,
                                     @Default TypedFunction<Treaty, String> t,
                                     @Default TypedFunction<Treaty, String> u,
                                     @Default TypedFunction<Treaty, String> v,
                                     @Default TypedFunction<Treaty, String> w,
                                     @Default TypedFunction<Treaty, String> x) throws GeneralSecurityException, IOException {
                return Placeholders._addColumns(this, command,db, io, author, sheet,
                        a, b, c, d, e, f, g, h, i, j,
                        k, l, m, n, o, p, q, r, s, t,
                        u, v, w, x);
            }
        };
    }

    private Placeholders<Project> createProjects() {
        return new StaticPlaceholders<Project>(Project.class, Project::values, store, validators, permisser,
                "A project",
                (ThrowingTriFunction<Placeholders<Project>, ValueStore, String, Set<Project>>) (inst, store, input) -> {
                    Set<Project> selection = getSelection(inst, store, input);
                    if (selection != null) return selection;
                    if (input.equalsIgnoreCase("*")) return new HashSet<>(Arrays.asList(Project.values));
                    if (SpreadSheet.isSheet(input)) {
                        return SpreadSheet.parseSheet(input, List.of("project"), true, (type, str) -> PWBindings.project(str));
                    }
                    Set<Project> result = new HashSet<>();
                    for (String type : input.split(",")) {
                        Project project = Project.parse(type);
                        if (project == null) throw new IllegalArgumentException("Invalid project: `" + type + "`");
                        result.add(project);
                    }
                    return result;
                }) {
            @Override
            public Set<String> getSheetColumns() {
                return Set.of("project");
            }

            @NoFormat
            @Command(desc = "Add an alias for a selection of Projects")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addSelectionAlias(@Me JSONObject command, @Me GuildDB db, String name, Set<Project> projects) {
                return _addSelectionAlias(this, command, db, name, projects, "projects");
            }

            @NoFormat
            @Command(desc = "Add columns to a Project sheet")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addColumns(@Me JSONObject command, @Me GuildDB db, @Me IMessageIO io, @Me User author, @Switch("s") SheetTemplate sheet,
                                     @Default TypedFunction<Project, String> a,
                                     @Default TypedFunction<Project, String> b,
                                     @Default TypedFunction<Project, String> c,
                                     @Default TypedFunction<Project, String> d,
                                     @Default TypedFunction<Project, String> e,
                                     @Default TypedFunction<Project, String> f,
                                     @Default TypedFunction<Project, String> g,
                                     @Default TypedFunction<Project, String> h,
                                     @Default TypedFunction<Project, String> i,
                                     @Default TypedFunction<Project, String> j,
                                     @Default TypedFunction<Project, String> k,
                                     @Default TypedFunction<Project, String> l,
                                     @Default TypedFunction<Project, String> m,
                                     @Default TypedFunction<Project, String> n,
                                     @Default TypedFunction<Project, String> o,
                                     @Default TypedFunction<Project, String> p,
                                     @Default TypedFunction<Project, String> q,
                                     @Default TypedFunction<Project, String> r,
                                     @Default TypedFunction<Project, String> s,
                                     @Default TypedFunction<Project, String> t,
                                     @Default TypedFunction<Project, String> u,
                                     @Default TypedFunction<Project, String> v,
                                     @Default TypedFunction<Project, String> w,
                                     @Default TypedFunction<Project, String> x) throws GeneralSecurityException, IOException {
                return Placeholders._addColumns(this, command,db, io, author, sheet,
                        a, b, c, d, e, f, g, h, i, j,
                        k, l, m, n, o, p, q, r, s, t,
                        u, v, w, x);
            }
        };
    }

    private Placeholders<TimedPolicy> createPolicies() {
        return new StaticPlaceholders<TimedPolicy>(TimedPolicy.class, TimedPolicy::values, store, validators, permisser,
                "A timed policy",
                (ThrowingTriFunction<Placeholders<TimedPolicy>, ValueStore, String, Set<TimedPolicy>>) (inst, store, input) -> {
                    Set<TimedPolicy> selection = getSelection(inst, store, input);
                    if (selection != null) return selection;
                    if (input.equalsIgnoreCase("*")) return new HashSet<>(Arrays.asList(TimedPolicy.values));
                    if (SpreadSheet.isSheet(input)) {
                        return SpreadSheet.parseSheet(input, List.of("policy"), true, (type, str) -> PWBindings.policy(str));
                    }
                    Set<TimedPolicy> result = new HashSet<>();
                    for (String type : input.split(",")) {
                        TimedPolicy policy = TimedPolicy.parse(type);
                        if (policy == null) throw new IllegalArgumentException("Invalid policy: `" + type + "`");
                        result.add(policy);
                    }
                    return result;
                }) {
            @Override
            public Set<String> getSheetColumns() {
                return Set.of("policy");
            }

            @NoFormat
            @Command(desc = "Add an alias for a selection of policy")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addSelectionAlias(@Me JSONObject command, @Me GuildDB db, String name, Set<TimedPolicy> policies) {
                return _addSelectionAlias(this, command, db, name, policies, "policies");
            }

            @NoFormat
            @Command(desc = "Add columns to a Policy sheet")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addColumns(@Me JSONObject command, @Me GuildDB db, @Me IMessageIO io, @Me User author, @Switch("s") SheetTemplate sheet,
                                     @Default TypedFunction<TimedPolicy, String> a,
                                     @Default TypedFunction<TimedPolicy, String> b,
                                     @Default TypedFunction<TimedPolicy, String> c,
                                     @Default TypedFunction<TimedPolicy, String> d,
                                     @Default TypedFunction<TimedPolicy, String> e,
                                     @Default TypedFunction<TimedPolicy, String> f,
                                     @Default TypedFunction<TimedPolicy, String> g,
                                     @Default TypedFunction<TimedPolicy, String> h,
                                     @Default TypedFunction<TimedPolicy, String> i,
                                     @Default TypedFunction<TimedPolicy, String> j,
                                     @Default TypedFunction<TimedPolicy, String> k,
                                     @Default TypedFunction<TimedPolicy, String> l,
                                     @Default TypedFunction<TimedPolicy, String> m,
                                     @Default TypedFunction<TimedPolicy, String> n,
                                     @Default TypedFunction<TimedPolicy, String> o,
                                     @Default TypedFunction<TimedPolicy, String> p,
                                     @Default TypedFunction<TimedPolicy, String> q,
                                     @Default TypedFunction<TimedPolicy, String> r,
                                     @Default TypedFunction<TimedPolicy, String> s,
                                     @Default TypedFunction<TimedPolicy, String> t,
                                     @Default TypedFunction<TimedPolicy, String> u,
                                     @Default TypedFunction<TimedPolicy, String> v,
                                     @Default TypedFunction<TimedPolicy, String> w,
                                     @Default TypedFunction<TimedPolicy, String> x) throws GeneralSecurityException, IOException {
                return Placeholders._addColumns(this, command,db, io, author, sheet,
                        a, b, c, d, e, f, g, h, i, j,
                        k, l, m, n, o, p, q, r, s, t,
                        u, v, w, x);
            }
        };
    }

    private Placeholders<Technology> createTechnologies() {
        return new StaticPlaceholders<Technology>(Technology.class, Technology::values, store, validators, permisser,
                "A Technology",
                (ThrowingTriFunction<Placeholders<Technology>, ValueStore, String, Set<Technology>>) (inst, store, input) -> {
                    Set<Technology> selection = getSelection(inst, store, input);
                    if (selection != null) return selection;
                    if (input.equalsIgnoreCase("*")) return new HashSet<>(Arrays.asList(Technology.values));
                    if (SpreadSheet.isSheet(input)) {
                        return SpreadSheet.parseSheet(input, List.of("technology"), true, (type, str) -> PWBindings.Technology(str));
                    }
                    Set<Technology> result = new HashSet<>();
                    for (String type : input.split(",")) {
                        Technology technology = Technology.parse(type);
                        if (technology == null) throw new IllegalArgumentException("Invalid technology: `" + type + "`");
                        result.add(technology);
                    }
                    return result;
                }) {
            @Override
            public Set<String> getSheetColumns() {
                return Set.of("technology");
            }

            @NoFormat
            @Command(desc = "Add an alias for a selection of technology")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addSelectionAlias(@Me JSONObject command, @Me GuildDB db, String name, Set<Technology> policies) {
                return _addSelectionAlias(this, command, db, name, policies, "policies");
            }

            @NoFormat
            @Command(desc = "Add columns to a Technology sheet")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addColumns(@Me JSONObject command, @Me GuildDB db, @Me IMessageIO io, @Me User author, @Switch("s") SheetTemplate sheet,
                                     @Default TypedFunction<Technology, String> a,
                                     @Default TypedFunction<Technology, String> b,
                                     @Default TypedFunction<Technology, String> c,
                                     @Default TypedFunction<Technology, String> d,
                                     @Default TypedFunction<Technology, String> e,
                                     @Default TypedFunction<Technology, String> f,
                                     @Default TypedFunction<Technology, String> g,
                                     @Default TypedFunction<Technology, String> h,
                                     @Default TypedFunction<Technology, String> i,
                                     @Default TypedFunction<Technology, String> j,
                                     @Default TypedFunction<Technology, String> k,
                                     @Default TypedFunction<Technology, String> l,
                                     @Default TypedFunction<Technology, String> m,
                                     @Default TypedFunction<Technology, String> n,
                                     @Default TypedFunction<Technology, String> o,
                                     @Default TypedFunction<Technology, String> p,
                                     @Default TypedFunction<Technology, String> q,
                                     @Default TypedFunction<Technology, String> r,
                                     @Default TypedFunction<Technology, String> s,
                                     @Default TypedFunction<Technology, String> t,
                                     @Default TypedFunction<Technology, String> u,
                                     @Default TypedFunction<Technology, String> v,
                                     @Default TypedFunction<Technology, String> w,
                                     @Default TypedFunction<Technology, String> x) throws GeneralSecurityException, IOException {
                return Placeholders._addColumns(this, command,db, io, author, sheet,
                        a, b, c, d, e, f, g, h, i, j,
                        k, l, m, n, o, p, q, r, s, t,
                        u, v, w, x);
            }
        };
    }

    private Placeholders<ResourceType> createResourceType() {
        return new StaticPlaceholders<ResourceType>(ResourceType.class, ResourceType::values, store, validators, permisser,
        "A game resource",
        (ThrowingTriFunction<Placeholders<ResourceType>, ValueStore, String, Set<ResourceType>>) (inst, store, input) -> {
            Set<ResourceType> selection = getSelection(inst, store, input);
                    if (selection != null) return selection;
            if (input.equalsIgnoreCase("*")) return new HashSet<>(Arrays.asList(ResourceType.values));
            if (SpreadSheet.isSheet(input)) {
                return SpreadSheet.parseSheet(input, List.of("resource"), true, (type, str) -> PWBindings.resource(str));
            }
            return Set.of(PWBindings.resource(input));
        }) {
            @Override
            public Set<String> getSheetColumns() {
                return Set.of("resource");
            }

            @NoFormat
            @Command(desc = "Add an alias for a selection of resources")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addSelectionAlias(@Me JSONObject command, @Me GuildDB db, String name, Set<ResourceType> resources) {
                return _addSelectionAlias(this, command, db, name, resources, "resources");
            }

            @NoFormat
            @Command(desc = "Add columns to a Resource sheet")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addColumns(@Me JSONObject command, @Me GuildDB db, @Me IMessageIO io, @Me User author, @Switch("s") SheetTemplate sheet,
                                     @Default TypedFunction<ResourceType, String> a,
                                     @Default TypedFunction<ResourceType, String> b,
                                     @Default TypedFunction<ResourceType, String> c,
                                     @Default TypedFunction<ResourceType, String> d,
                                     @Default TypedFunction<ResourceType, String> e,
                                     @Default TypedFunction<ResourceType, String> f,
                                     @Default TypedFunction<ResourceType, String> g,
                                     @Default TypedFunction<ResourceType, String> h,
                                     @Default TypedFunction<ResourceType, String> i,
                                     @Default TypedFunction<ResourceType, String> j,
                                     @Default TypedFunction<ResourceType, String> k,
                                     @Default TypedFunction<ResourceType, String> l,
                                     @Default TypedFunction<ResourceType, String> m,
                                     @Default TypedFunction<ResourceType, String> n,
                                     @Default TypedFunction<ResourceType, String> o,
                                     @Default TypedFunction<ResourceType, String> p,
                                     @Default TypedFunction<ResourceType, String> q,
                                     @Default TypedFunction<ResourceType, String> r,
                                     @Default TypedFunction<ResourceType, String> s,
                                     @Default TypedFunction<ResourceType, String> t,
                                     @Default TypedFunction<ResourceType, String> u,
                                     @Default TypedFunction<ResourceType, String> v,
                                     @Default TypedFunction<ResourceType, String> w,
                                     @Default TypedFunction<ResourceType, String> x) throws GeneralSecurityException, IOException {
                return Placeholders._addColumns(this, command,db, io, author, sheet,
                        a, b, c, d, e, f, g, h, i, j,
                        k, l, m, n, o, p, q, r, s, t,
                        u, v, w, x);
            }
        };
    }

    private Placeholders<MilitaryUnit> createMilitaryUnit() {
        return new StaticPlaceholders<MilitaryUnit>(MilitaryUnit.class, MilitaryUnit::values, store, validators, permisser,
                "A military unit type",
                (ThrowingTriFunction<Placeholders<MilitaryUnit>, ValueStore, String, Set<MilitaryUnit>>) (inst, store, input) -> {
                    Set<MilitaryUnit> selection = getSelection(inst, store, input);
                    if (selection != null) return selection;
                    if (input.equalsIgnoreCase("*")) return new HashSet<>(Arrays.asList(MilitaryUnit.values));
                    if (SpreadSheet.isSheet(input)) {
                        return SpreadSheet.parseSheet(input, List.of("unit"), true, (type, str) -> PWBindings.unit(str));
                    }
                    return Set.of(PWBindings.unit(input));
                }) {
            @Override
            public Set<String> getSheetColumns() {
                return Set.of("unit");
            }

            @NoFormat
            @Command(desc = "Add an alias for a selection of Military Units")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addSelectionAlias(@Me JSONObject command, @Me GuildDB db, String name, Set<MilitaryUnit> military_units) {
                return _addSelectionAlias(this, command, db, name, military_units, "military_units");
            }

            @NoFormat
            @Command(desc = "Add columns to a Military Unit sheet")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addColumns(@Me JSONObject command, @Me GuildDB db, @Me IMessageIO io, @Me User author, @Switch("s") SheetTemplate sheet,
                                     @Default TypedFunction<MilitaryUnit, String> a,
                                     @Default TypedFunction<MilitaryUnit, String> b,
                                     @Default TypedFunction<MilitaryUnit, String> c,
                                     @Default TypedFunction<MilitaryUnit, String> d,
                                     @Default TypedFunction<MilitaryUnit, String> e,
                                     @Default TypedFunction<MilitaryUnit, String> f,
                                     @Default TypedFunction<MilitaryUnit, String> g,
                                     @Default TypedFunction<MilitaryUnit, String> h,
                                     @Default TypedFunction<MilitaryUnit, String> i,
                                     @Default TypedFunction<MilitaryUnit, String> j,
                                     @Default TypedFunction<MilitaryUnit, String> k,
                                     @Default TypedFunction<MilitaryUnit, String> l,
                                     @Default TypedFunction<MilitaryUnit, String> m,
                                     @Default TypedFunction<MilitaryUnit, String> n,
                                     @Default TypedFunction<MilitaryUnit, String> o,
                                     @Default TypedFunction<MilitaryUnit, String> p,
                                     @Default TypedFunction<MilitaryUnit, String> q,
                                     @Default TypedFunction<MilitaryUnit, String> r,
                                     @Default TypedFunction<MilitaryUnit, String> s,
                                     @Default TypedFunction<MilitaryUnit, String> t,
                                     @Default TypedFunction<MilitaryUnit, String> u,
                                     @Default TypedFunction<MilitaryUnit, String> v,
                                     @Default TypedFunction<MilitaryUnit, String> w,
                                     @Default TypedFunction<MilitaryUnit, String> x) throws GeneralSecurityException, IOException {
                return Placeholders._addColumns(this, command,db, io, author, sheet,
                        a, b, c, d, e, f, g, h, i, j,
                        k, l, m, n, o, p, q, r, s, t,
                        u, v, w, x);
            }
        };
    }

    private Placeholders<TreatyType> createTreatyType() {
        return new StaticPlaceholders<TreatyType>(TreatyType.class, TreatyType::values, store, validators, permisser,
                "A treaty type",
                (ThrowingTriFunction<Placeholders<TreatyType>, ValueStore, String, Set<TreatyType>>) (inst, store, input) -> {
                    Set<TreatyType> selection = getSelection(inst, store, input);
                    if (selection != null) return selection;
                    if (input.equalsIgnoreCase("*")) return new HashSet<>(Arrays.asList(TreatyType.values));
                    if (SpreadSheet.isSheet(input)) {
                        return SpreadSheet.parseSheet(input, List.of("treaty_type"), true, (type, str) -> PWBindings.TreatyType(str));
                    }
                    return Set.of(PWBindings.TreatyType(input));
                }) {
            @Override
            public Set<String> getSheetColumns() {
                return Set.of("treaty_type");
            }

            @NoFormat
            @Command(desc = "Add an alias for a selection of Treaty Types")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addSelectionAlias(@Me JSONObject command, @Me GuildDB db, String name, Set<TreatyType> treaty_types) {
                return _addSelectionAlias(this, command, db, name, treaty_types, "treaty_types");
            }

            @NoFormat
            @Command(desc = "Add columns to a TreatyType sheet")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addColumns(@Me JSONObject command, @Me GuildDB db, @Me IMessageIO io, @Me User author, @Switch("s") SheetTemplate sheet,
                                     @Default TypedFunction<TreatyType, String> a,
                                     @Default TypedFunction<TreatyType, String> b,
                                     @Default TypedFunction<TreatyType, String> c,
                                     @Default TypedFunction<TreatyType, String> d,
                                     @Default TypedFunction<TreatyType, String> e,
                                     @Default TypedFunction<TreatyType, String> f,
                                     @Default TypedFunction<TreatyType, String> g,
                                     @Default TypedFunction<TreatyType, String> h,
                                     @Default TypedFunction<TreatyType, String> i,
                                     @Default TypedFunction<TreatyType, String> j,
                                     @Default TypedFunction<TreatyType, String> k,
                                     @Default TypedFunction<TreatyType, String> l,
                                     @Default TypedFunction<TreatyType, String> m,
                                     @Default TypedFunction<TreatyType, String> n,
                                     @Default TypedFunction<TreatyType, String> o,
                                     @Default TypedFunction<TreatyType, String> p,
                                     @Default TypedFunction<TreatyType, String> q,
                                     @Default TypedFunction<TreatyType, String> r,
                                     @Default TypedFunction<TreatyType, String> s,
                                     @Default TypedFunction<TreatyType, String> t,
                                     @Default TypedFunction<TreatyType, String> u,
                                     @Default TypedFunction<TreatyType, String> v,
                                     @Default TypedFunction<TreatyType, String> w,
                                     @Default TypedFunction<TreatyType, String> x) throws GeneralSecurityException, IOException {
                return Placeholders._addColumns(this, command,db, io, author, sheet,
                        a, b, c, d, e, f, g, h, i, j,
                        k, l, m, n, o, p, q, r, s, t,
                        u, v, w, x);
            }
        };
    }

    private Placeholders<IACheckup.AuditType> createAuditType() {
        return new StaticPlaceholders<IACheckup.AuditType>(IACheckup.AuditType.class, IACheckup.AuditType::values, store, validators, permisser,
                "A bot audit type for a nation",
                (ThrowingTriFunction<Placeholders<IACheckup.AuditType>, ValueStore, String, Set<IACheckup.AuditType>>) (inst, store, input) -> {
                    Set<IACheckup.AuditType> selection = getSelection(inst, store, input);
                    if (selection != null) return selection;
                    if (input.equalsIgnoreCase("*")) return new HashSet<>(Arrays.asList(IACheckup.AuditType.values()));
                    if (SpreadSheet.isSheet(input)) {
                        return SpreadSheet.parseSheet(input, List.of("audit"), true, (type, str) -> PWBindings.auditType(str));
                    }
                    return Set.of(PWBindings.auditType(input));
                }) {
            @Override
            public Set<String> getSheetColumns() {
                return Set.of("audit");
            }

            @NoFormat
            @Command(desc = "Add an alias for a selection of Audit Types")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addSelectionAlias(@Me JSONObject command, @Me GuildDB db, String name, Set<IACheckup.AuditType> audit_types) {
                return _addSelectionAlias(this, command, db, name, audit_types, "audit_types");
            }

            @NoFormat
            @Command(desc = "Add columns to a Audit Type sheet")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addColumns(@Me JSONObject command, @Me GuildDB db, @Me IMessageIO io, @Me User author, @Switch("s") SheetTemplate sheet,
                                     @Default TypedFunction<IACheckup.AuditType, String> a,
                                     @Default TypedFunction<IACheckup.AuditType, String> b,
                                     @Default TypedFunction<IACheckup.AuditType, String> c,
                                     @Default TypedFunction<IACheckup.AuditType, String> d,
                                     @Default TypedFunction<IACheckup.AuditType, String> e,
                                     @Default TypedFunction<IACheckup.AuditType, String> f,
                                     @Default TypedFunction<IACheckup.AuditType, String> g,
                                     @Default TypedFunction<IACheckup.AuditType, String> h,
                                     @Default TypedFunction<IACheckup.AuditType, String> i,
                                     @Default TypedFunction<IACheckup.AuditType, String> j,
                                     @Default TypedFunction<IACheckup.AuditType, String> k,
                                     @Default TypedFunction<IACheckup.AuditType, String> l,
                                     @Default TypedFunction<IACheckup.AuditType, String> m,
                                     @Default TypedFunction<IACheckup.AuditType, String> n,
                                     @Default TypedFunction<IACheckup.AuditType, String> o,
                                     @Default TypedFunction<IACheckup.AuditType, String> p,
                                     @Default TypedFunction<IACheckup.AuditType, String> q,
                                     @Default TypedFunction<IACheckup.AuditType, String> r,
                                     @Default TypedFunction<IACheckup.AuditType, String> s,
                                     @Default TypedFunction<IACheckup.AuditType, String> t,
                                     @Default TypedFunction<IACheckup.AuditType, String> u,
                                     @Default TypedFunction<IACheckup.AuditType, String> v,
                                     @Default TypedFunction<IACheckup.AuditType, String> w,
                                     @Default TypedFunction<IACheckup.AuditType, String> x) throws GeneralSecurityException, IOException {
                return Placeholders._addColumns(this, command,db, io, author, sheet,
                        a, b, c, d, e, f, g, h, i, j,
                        k, l, m, n, o, p, q, r, s, t,
                        u, v, w, x);
            }
        };
    }

    private Placeholders<Building> createBuilding() {
        return new StaticPlaceholders<Building>(Building.class, Building::values, store, validators, permisser,
                "A city building type",
                (ThrowingTriFunction<Placeholders<Building>, ValueStore, String, Set<Building>>) (inst, store, input) -> {
                    Set<Building> selection = getSelection(inst, store, input);
                    if (selection != null) return selection;
                    if (input.equalsIgnoreCase("*")) return new HashSet<>(Arrays.asList(Building.values()));
                    if (SpreadSheet.isSheet(input)) {
                        return SpreadSheet.parseSheet(input, List.of("building"), true, (type, str) -> PWBindings.getBuilding(str));
                    }
                    return Set.of(PWBindings.getBuilding(input));
                }) {
            @Override
            public Set<String> getSheetColumns() {
                return Set.of("building");
            }

            @NoFormat
            @Command(desc = "Add an alias for a selection of Buildings")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addSelectionAlias(@Me JSONObject command, @Me GuildDB db, String name, Set<Building> Buildings) {
                return _addSelectionAlias(this, command, db, name, Buildings, "Buildings");
            }

            @NoFormat
            @Command(desc = "Add columns to a Building sheet")
            @RolePermission(value = {Roles.INTERNAL_AFFAIRS_STAFF, Roles.MILCOM, Roles.ECON_STAFF, Roles.FOREIGN_AFFAIRS_STAFF, Roles.ECON, Roles.FOREIGN_AFFAIRS}, any = true)
            public String addColumns(@Me JSONObject command, @Me GuildDB db, @Me IMessageIO io, @Me User author, @Switch("s") SheetTemplate sheet,
                                     @Default TypedFunction<Building, String> a,
                                     @Default TypedFunction<Building, String> b,
                                     @Default TypedFunction<Building, String> c,
                                     @Default TypedFunction<Building, String> d,
                                     @Default TypedFunction<Building, String> e,
                                     @Default TypedFunction<Building, String> f,
                                     @Default TypedFunction<Building, String> g,
                                     @Default TypedFunction<Building, String> h,
                                     @Default TypedFunction<Building, String> i,
                                     @Default TypedFunction<Building, String> j,
                                     @Default TypedFunction<Building, String> k,
                                     @Default TypedFunction<Building, String> l,
                                     @Default TypedFunction<Building, String> m,
                                     @Default TypedFunction<Building, String> n,
                                     @Default TypedFunction<Building, String> o,
                                     @Default TypedFunction<Building, String> p,
                                     @Default TypedFunction<Building, String> q,
                                     @Default TypedFunction<Building, String> r,
                                     @Default TypedFunction<Building, String> s,
                                     @Default TypedFunction<Building, String> t,
                                     @Default TypedFunction<Building, String> u,
                                     @Default TypedFunction<Building, String> v,
                                     @Default TypedFunction<Building, String> w,
                                     @Default TypedFunction<Building, String> x) throws GeneralSecurityException, IOException {
                return Placeholders._addColumns(this, command,db, io, author, sheet,
                        a, b, c, d, e, f, g, h, i, j,
                        k, l, m, n, o, p, q, r, s, t,
                        u, v, w, x);
            }
        };
    }



}

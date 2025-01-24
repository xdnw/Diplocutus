package link.locutus.discord.db.guild;

import com.google.gson.reflect.TypeToken;
import com.knuddels.jtokkit.api.ModelType;
import com.theokanning.openai.service.OpenAiService;
import link.locutus.discord.Locutus;
import link.locutus.discord.api.ApiKeyPool;
import link.locutus.discord.api.endpoints.DnsApi;
import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.api.types.Rank;
import link.locutus.discord.commands.manager.v2.binding.Key;
import link.locutus.discord.commands.manager.v2.binding.annotation.*;
import link.locutus.discord.commands.manager.v2.binding.bindings.PrimitiveBindings;
import link.locutus.discord.commands.manager.v2.impl.discord.binding.DiscordBindings;
import link.locutus.discord.commands.manager.v2.impl.discord.permission.RolePermission;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.commands.manager.v2.impl.pw.NationFilter;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.*;
import link.locutus.discord.gpt.GPTModerator;
import link.locutus.discord.gpt.GPTUtil;
import link.locutus.discord.gpt.ModerationResult;
import link.locutus.discord.gpt.copilot.CopilotDeviceAuthenticationData;
import link.locutus.discord.gpt.imps.CopilotText2Text;
import link.locutus.discord.pnw.GuildOrAlliance;
import link.locutus.discord.pnw.NationOrAllianceOrGuild;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.*;
import link.locutus.discord.util.discord.DiscordUtil;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GuildKey {
    public static final GuildSetting<Set<Integer>> ALLIANCE_ID = new GuildSetting<Set<Integer>>(GuildSettingCategory.DEFAULT, Set.class, Integer.class) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String registerAlliance(@Me GuildDB db, @Me User user, Set<DBAlliance> alliances) {
            Set<Integer> existing = ALLIANCE_ID.getOrNull(db, false);
            existing = existing == null ? new LinkedHashSet<>() : new LinkedHashSet<>(existing);
            Set<Integer> toAdd = alliances.stream().map(DBAlliance::getId).collect(Collectors.toSet());
            for (DBAlliance alliance : alliances) {
                if (existing.contains(alliance.getId())) {
                    throw new IllegalArgumentException("Alliance " + alliance.getName() + " (id: " + alliance.getId() + ") is already registered (registered: " + StringMan.join(existing, ",") + ")\n" +
                            "To set multiple alliances, first delete the currently set alliance ids: " + CM.settings.delete.cmd.key(GuildKey.ALLIANCE_ID.name()));
                }
            }
            toAdd = ALLIANCE_ID.allowedAndValidate(db, user, toAdd);
            existing.addAll(toAdd);
            return ALLIANCE_ID.set(db, user, toAdd);
        }
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String unregisterAlliance(@Me GuildDB db, @Me User user, Set<DBAlliance> alliances) {
            Set<Integer> existing = ALLIANCE_ID.getOrNull(db, false);
            existing = existing == null ? new LinkedHashSet<>() : new LinkedHashSet<>(existing);
            for (DBAlliance alliance : alliances) {
                if (!existing.contains(alliance.getId())) {
                    throw new IllegalArgumentException("Alliance " + alliance.getId() + " is not registered (registered: " + StringMan.join(existing, ",") + ")");
                }
                existing.remove(alliance.getId());
            }
            if (existing.isEmpty()) {
                return ALLIANCE_ID.delete(db, user);
            }
            return ALLIANCE_ID.set(db, user, existing);
        }
        @Override
        public Set<Integer> validate(GuildDB db, User user, Set<Integer> aaIds) {
            if (DELEGATE_SERVER.has(db, false))
                throw new IllegalArgumentException("Cannot set alliance id of delegate server (please unset DELEGATE_SERVER first)");

            if (aaIds.isEmpty()) {
                throw new IllegalArgumentException("No alliance provided");
            }

            for (int aaId : aaIds) {
                if (aaId == 0) {
                    throw new IllegalArgumentException("None alliance (id=0) cannot be registered: " + aaIds);
                }
                DBAlliance alliance = DBAlliance.getOrCreate(aaId);
                GuildDB otherDb = alliance.getGuildDB();
                Member owner = db.getGuild().getOwner();
                if (user == null || user.getIdLong() != Locutus.loader().getAdminUserId()) {
                    DBNation ownerNation = owner != null ? DiscordUtil.getNation(owner.getUser()) : null;
                    DBNation myNation = user != null ? DiscordUtil.getNation(user) : null;
                    if (ownerNation == null && myNation == null) {
                        throw new IllegalArgumentException("No nation found for guild owner or user (owner: " + owner + ", user: " + user + "). Register using " + CM.register.cmd.toSlashMention());
                    }
                    boolean allowed = false;
                    DBNation leader = alliance.getLeader();
                    if (ownerNation != null && leader.equals(ownerNation)) {
                        allowed = true;
                    } else if (myNation != null && leader.equals(myNation)) {
                        allowed = true;
                    }
                    if (!allowed) {
                        throw new IllegalArgumentException("The alliance leader must run the command: " + leader.getMarkdownUrl());
                    }
                }
                if (otherDb != null && otherDb != db) {
                    otherDb.deleteInfo(ALLIANCE_ID);
                    String msg = "Only 1 root server per Alliance is permitted. The ALLIANCE_ID in the other guild: " + otherDb.getGuild() + " has been removed.\n" +
                            "To have multiple servers, set the ALLIANCE_ID on your primary server, and then set " + DELEGATE_SERVER.getCommandMention() + " on your other servers\n" +
                            "The `<guild-id>` for this server is `" + db.getIdLong() + "` and the id for the other server is `" + otherDb.getIdLong() + "`.\n\n" +
                            "Run this command again to confirm and set the ALLIANCE_ID";
                    throw new IllegalArgumentException(msg);
                }
            }
            return aaIds;
        }

        @Override
        public String help() {
            return "Your alliance id";
        }

        @Override
        public String toString(Set<Integer> value) {
            return StringMan.join(value, ",");
        }
    };

    public static final GuildSetting<String> OPENAI_KEY = new GuildStringSetting(GuildSettingCategory.ARTIFICIAL_INTELLIGENCE) {

        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        @Ephemeral
        public String register_openai_key(@Me GuildDB db, @Me User user, String apiKey) {
            return OPENAI_KEY.set(db, user, apiKey);
        }

        @Override
        public String validate(GuildDB db, User user, String apiKey) {
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalArgumentException("Please provide an API key");
            }
            OpenAiService service = new OpenAiService(Settings.INSTANCE.ARTIFICIAL_INTELLIGENCE.OPENAI.API_KEY, Duration.ofSeconds(120));
            GPTModerator moderator = new GPTModerator(service);
            List<ModerationResult> result = moderator.moderate("Hello World");
            if (result.size() == 0) {
                throw new IllegalArgumentException("Invalid API key. No result returned");
            }
            ModerationResult modResult = result.get(0);
            if (modResult.isError()) {
                throw new IllegalArgumentException("Invalid API key. Error returned: " + modResult.getMessage());
            }
            return apiKey;
        }

        @Override
        public String parse(GuildDB db, String input) {
            return input;
        }

        @Override
        public String toReadableString(GuildDB db, String value) {
            if (value != null && value.length() > 7) {
                return value.substring(0, 3) + "..." + value.substring(value.length() - 4);
            }
            return "Invalid key";
        }

        @Override
        public String help() {
            return "OpenAI API key\n" +
                    "Used for chat responses and completion\n" +
                    "Get a key from: <https://platform.openai.com/account/api-keys>";
        }
    }.setupRequirements(f -> f.requireValidAlliance());

    public static final GuildSetting<ModelType> OPENAI_MODEL = new GuildSetting<ModelType>(GuildSettingCategory.ARTIFICIAL_INTELLIGENCE, ModelType.class) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String register_openai_key(@Me GuildDB db, @Me User user, ModelType model) {
            return OPENAI_MODEL.set(db, user, model);
        }

        @Override
        public ModelType validate(GuildDB db, User user, ModelType model) {
            return switch (model) {
                case GPT_4, GPT_4_32K, GPT_3_5_TURBO, GPT_3_5_TURBO_16K -> model;
                default -> throw new IllegalArgumentException("Invalid chat model type: " + model);
            };
        }

        @Override
        public ModelType parse(GuildDB db, String input) {
            return ModelType.valueOf(input);
        }

        @Override
        public String help() {
            return "OpenAI model type\n" +
                    "Used for chat responses and completion\n" +
                    "Valid values: " + StringMan.join(ModelType.values(), ", ");
        }

        @Override
        public String toString(ModelType value) {
            return value.name();
        }
    }.setupRequirements(f -> f.requires(OPENAI_KEY));

    public static GuildSetting<int[]> GPT_USAGE_LIMITS = new GuildSetting<int[]>(GuildSettingCategory.ARTIFICIAL_INTELLIGENCE, int[].class) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String GPT_USAGE_LIMITS(@Me GuildDB db, @Me User user, int userTurnLimit, int userDayLimit, int guildTurnLimit, int guildDayLimit) {
            int[] combined = new int[]{userTurnLimit, userDayLimit, guildTurnLimit, guildDayLimit};
            return GPT_USAGE_LIMITS.set(db, user, combined);
        }

        @Override
        public int[] validate(GuildDB db, User user, int[] limits) {
            // ensure length = 4
            if (limits.length != 4) {
                throw new IllegalArgumentException("Invalid limits. Expected 4 values, got " + limits.length);
            }
            // ensure all > 0
            for (int limit : limits) {
                if (limit < 0) {
                    throw new IllegalArgumentException("Invalid limit. Must be >= 0");
                }
            }
            return limits;
        }

        @Override
        public int[] parse(GuildDB db, String input) {
            // 4 numbers, comma separated
            String[] split = input.split(",");
            if (split.length != 4) {
                throw new IllegalArgumentException("Invalid limits. Expected 4 values, got " + split.length);
            }
            int[] limits = new int[4];
            for (int i = 0; i < 4; i++) {
                try {
                    limits[i] = Integer.parseInt(split[i]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid limit. Must be a number, not: `" + split[i] + "`");
                }
            }
            return limits;
        }

        @Override
        public String help() {
            return "gpt user and guild usage limits, by turn and day\n" +
                    "Used to limit costs incurred from excessive usage" +
                    "Usage is only tracked per session, and is reset each time the bot restarts";
        }

        @Override
        public String toString(int[] value) {
            return StringMan.join(value, ",");
        }
    }.setupRequirements(f -> f.requires(OPENAI_KEY));

    public static GuildSetting<Boolean> ENABLE_GITHUB_COPILOT = new GuildBooleanSetting(GuildSettingCategory.ARTIFICIAL_INTELLIGENCE) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String ENABLE_GITHUB_COPILOT(@Me GuildDB db, @Me User user, boolean value) {
            return ENABLE_GITHUB_COPILOT.setAndValidate(db, user, value);
        }

        @Override
        public Boolean validate(GuildDB db, User user, Boolean value) {
            if (value == Boolean.TRUE) {
                CopilotDeviceAuthenticationData[] authData = new CopilotDeviceAuthenticationData[1];


                CopilotText2Text copilot = new CopilotText2Text("tokens" + File.separator + db.getIdLong(), new Consumer<CopilotDeviceAuthenticationData>() {
                    @Override
                    public void accept(CopilotDeviceAuthenticationData data) {
                        authData[0] = data;
                    }
                });
                try {
                    copilot.generate("Hello W");
                } catch (Throwable e) {
                    if (authData[0] != null) {
                        throw new IllegalArgumentException("Open URL " + authData[0].Url + " to enter the device code: " + authData[0].UserCode);
                    }
                    throw e;
                }
            }
            return value;
        }

        @Override
        public String help() {
            return "Enable GitHub Copilot for generating AI text responses\n" +
                    "See: <https://github.com/features/copilot>\n" +
                    "This is an alternative to an open ai key";
        }
    }.requireValidAlliance();

    public static final GuildSetting<List<ApiKeyPool.ApiKey>> API_KEY = new GuildSetting<List<ApiKeyPool.ApiKey>>(GuildSettingCategory.DEFAULT, List.class, ApiKeyPool.ApiKey.class) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        @Ephemeral
        public String registerApiKey(@Me GuildDB db, @Me User user, List<ApiKeyPool.ApiKey> apiKeys) {
            List<ApiKeyPool.ApiKey> existing = API_KEY.getOrNull(db);
            existing = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
            Map<Integer, String> existingNationIds = new HashMap<>();
            for (ApiKeyPool.ApiKey key : existing) {
                existingNationIds.put(key.getNationId(), key.getKey());
            }

            List<ApiKeyPool.ApiKey> toAdd = API_KEY.allowedAndValidate(db, user, apiKeys);
            Map<Integer, String> toAddNationIds = new HashMap<>();
            for (ApiKeyPool.ApiKey key : toAdd) {
                toAddNationIds.put(key.getNationId(), key.getKey());
            }

            StringBuilder response = new StringBuilder();
            for (ApiKeyPool.ApiKey key : apiKeys) {
                if (existingNationIds.containsKey(key.getNationId())) {
                    response.append("The existing key for nation: " + key.getNationId() + " is already registered\n");
                    existing.removeIf(f -> f.getNationId() == key.getNationId());
                }
                if (existingNationIds.containsValue(key.getKey())) {
                    response.append("The existing key for nation: " + key.getNationId() + " is already registered\n");
                    existing.removeIf(f -> f.getKey().equals(key.getKey()));
                }
                existing.add(key);
            }
            apiKeys = API_KEY.allowedAndValidate(db, user, existing);
            Set<Integer> aaIds = new HashSet<>(db.getAllianceIds());
            for (ApiKeyPool.ApiKey key : existing) {
                int nationId = key.getNationId();
                DBNation nation = DBNation.getById(nationId);
                if (nation != null) {
                    aaIds.remove(nation.getAlliance_id());
                }
            }
            if (!aaIds.isEmpty()) {
                response.append("The following alliance ids are missing from the api keys: " + StringMan.join(aaIds, ",") + "\n");
            }

            response.append(API_KEY.set(db, user, apiKeys));
            return response.toString();
        }

        @Override
        public List<ApiKeyPool.ApiKey> validate(GuildDB db, User user, List<ApiKeyPool.ApiKey> keys) {
            keys = new ArrayList<>(new LinkedHashSet<>(keys));
            Set<Integer> aaIds = db.getAllianceIds();
            if (aaIds.isEmpty()) {
                throw new IllegalArgumentException("Please first use " + GuildKey.ALLIANCE_ID.getCommandMention());
            }
            for (ApiKeyPool.ApiKey key : keys) {
                Integer nationId = key.getNationId();
                if (nationId == null) {
                    throw new IllegalArgumentException("Invalid API key");
                }
                DBNation nation = DBNation.getById(nationId);
                if (nation == null) {
                    throw new IllegalArgumentException("Nation not found for id: " + nationId + "(out of sync?)");
                }
                if (!aaIds.contains(nation.getAlliance_id())) {
                    throw new IllegalArgumentException("Nation " + nation.getName() + " is not in your alliance: " + StringMan.getString(aaIds) + ". Note: Force an update using " + CM.admin.sync.syncNations.cmd.toSlashMention());
                }
                try {
                    new DnsApi(ApiKeyPool.builder().addKey(key).build()).alliance().call();
                } catch (Throwable e) {
                    throw new IllegalArgumentException("Key was rejected: " + StringMan.stripApiKey(e.getMessage()));
                }
            }
            return keys;
        }

        @Override
        public String toString(List<ApiKeyPool.ApiKey> value) {
            value.removeIf(f -> f.getKey().isEmpty() || f.getNation() == null);
            return value.stream().map(f -> f.getNationId() + ":" + f.getKey()).collect(Collectors.joining(","));
        }

        @Override
        public List<ApiKeyPool.ApiKey> parse(GuildDB db, String input) {
            List<ApiKeyPool.ApiKey> keys = new ArrayList<>();
            for (String key : StringMan.split(input, ',')) {
                String[] split = key.split(":");
                if (split.length != 2) {
                    throw new IllegalArgumentException("Invalid key format: " + key + ". Expected format: `<nation>:<key>` e.g. `Borg:abc123`");
                }
                try {
                    DBNation nation = DiscordUtil.parseNation(split[0]);
                    if (nation == null) {
                        throw new IllegalArgumentException("Nation not found: " + split[0]);
                    }
                    keys.add(new ApiKeyPool.ApiKey(nation.getNation_id(), split[1]));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid nation id: " + split[0]);
                }
            }
            return keys;
        }

        @Override
        public String toReadableString(GuildDB db, List<ApiKeyPool.ApiKey> value) {
            List<String> redacted = new ArrayList<>();
            for (ApiKeyPool.ApiKey key : value) {
                int nationId = key.getNationId();
                redacted.add(DNS.getName(nationId, false));
            }
            return StringMan.join(redacted, ",");
        }

        @Override
        public String help() {
            return "API key found at the bottom of: <https://diplomacyandstrife.com/account/>\n" +
                    "Be sure to enable all access you wish the bot to have, and have a sufficient position in-game (e.g. leader)\n" +
                    "Needed for alliance functions and information access, such as calculating resource dispersal, sending mail\n" +
                    "![Api Key Example](https://raw.githubusercontent.com/xdnw/Diplocutus/master/src/main/resources/img/apikey.png)";
        }
    }.setupRequirements(f -> f.requires(ALLIANCE_ID));

    public static GuildSetting<Map<ResourceType, Double>> WARCHEST_PER_INFRA = new GuildResourceSetting(GuildSettingCategory.AUDIT) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String WARCHEST_PER_INFRA(@Me GuildDB db, @Me User user, Map<ResourceType, Double> amount) {
            return WARCHEST_PER_INFRA.setAndValidate(db, user, amount);
        }

        @Override
        public String toReadableString(GuildDB db, Map<ResourceType, Double> value) {
            return ResourceType.resourcesToString(value);
        }

        @Override
        public String help() {
            return "Amount of warchest to recommend per infra in form `{fuel=1.0}`";
        }
    }.setupRequirements(f -> f.requires(ALLIANCE_ID).requireValidAlliance().requires(API_KEY));
    public static GuildSetting<Category> EMBASSY_CATEGORY = new GuildCategorySetting(GuildSettingCategory.FOREIGN_AFFAIRS) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String EMBASSY_CATEGORY(@Me GuildDB db, @Me User user, Category category) {
            return EMBASSY_CATEGORY.setAndValidate(db, user, category);
        }
        @Override
        public String help() {
            return "The name or id of the CATEGORY you would like embassy channels created in (for " + CM.embassy.cmd.toSlashMention() + ")";
        }
    }.setupRequirements(f -> f.requireFunction(new Consumer<GuildDB>() {
        @Override
        public void accept(GuildDB db) {
            if (ALLIANCE_ID.getOrNull(db, true) == null) {
                for (GuildDB otherDb : Locutus.imp().getGuildDatabases().values()) {
                    Guild warServer = WAR_SERVER.getOrNull(otherDb, false);
                    GuildDB faServer = FA_SERVER.getOrNull(otherDb, false);
                    if (faServer != null && faServer.getIdLong() == db.getIdLong()) {
                        return;
                    }
                    if (warServer != null && warServer.getIdLong() == db.getIdLong()) {
                        return;
                    }
                }
                throw new IllegalArgumentException("Missing required setting " + ALLIANCE_ID.name() + " " + ALLIANCE_ID.getCommandMention() + "\n" +
                        "(Or set this server as an " + FA_SERVER.name() + " from another guild)");

            }
        }
    }, "Requires having `" + GuildKey.ALLIANCE_ID.name() + "` set here, or `" + GuildKey.FA_SERVER.name() + "` set to this guild in another guild."));
    public static GuildSetting<Map<Role, Set<Role>>> ASSIGNABLE_ROLES = new GuildSetting<Map<Role, Set<Role>>>(GuildSettingCategory.ROLE, Map.class, Role.class, TypeToken.getParameterized(Set.class, Role.class).getType()) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String addAssignableRole(@Me GuildDB db, @Me User user, Role role, Set<Role> roles) {
            Map<Role, Set<Role>> existing = ASSIGNABLE_ROLES.getOrNull(db, false);
            existing = existing == null ? new HashMap<>() : new LinkedHashMap<>(existing);
            existing.put(role, roles);
            return ASSIGNABLE_ROLES.setAndValidate(db, user, existing);
        }


        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String ASSIGNABLE_ROLES(@Me GuildDB db, @Me User user, Map<Role, Set<Role>> value) {
            return ASSIGNABLE_ROLES.setAndValidate(db, user, value);
        }

        @Override
        public String toReadableString(GuildDB db, Map<Role, Set<Role>> map) {
            List<String> lines = new ArrayList<>();
            for (Map.Entry<Role, Set<Role>> entry : map.entrySet()) {
                String key = entry.getKey().getName();
                List<String> valueStrings = entry.getValue().stream().map(f -> f.getName()).collect(Collectors.toList());
                String value = StringMan.join(valueStrings, ",");

                lines.add(key + ":" + value);
            }
            return StringMan.join(lines, "\n");
        }

        public String toString(Map<Role, Set<Role>> map) {
            List<String> lines = new ArrayList<>();
            for (Map.Entry<Role, Set<Role>> entry : map.entrySet()) {
                String key = entry.getKey().getAsMention();
                List<String> valueStrings = entry.getValue().stream().map(f -> f.getAsMention()).collect(Collectors.toList());
                String value = StringMan.join(valueStrings, ",");

                lines.add(key + ":" + value);
            }
            return StringMan.join(lines, "\n");
        }

        @Override
        public String help() {
            return "Map roles that can be assigned (or removed). See " + CM.self.create.cmd.toSlashMention() + " " + CM.role.removeAssignableRole.cmd.toSlashMention() + " " + CM.self.add.cmd.toSlashMention() + " " + CM.self.remove.cmd.toSlashMention();
        }
    };
    public static GuildSetting<MessageChannel> DEFENSE_WAR_CHANNEL = new GuildChannelSetting(GuildSettingCategory.WAR_ALERTS) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String DEFENSE_WAR_CHANNEL(@Me GuildDB db, @Me User user, MessageChannel channel) {
            return DEFENSE_WAR_CHANNEL.setAndValidate(db, user, channel);
        }

        @Override
        public String help() {
            return "The #channel to receive alerts for defensive wars\n" +
                    "Members and `" + Roles.MILCOM.name() + "` are pinged for defensive wars\n" +
                    "To set the `" + Roles.MILCOM.name() + "` role, see: "
                    + CM.role.setAlias.cmd.locutusRole(Roles.MILCOM.name()).discordRole("")
              ;
        }
    }.setupRequirements(f -> f.requiresAllies().requireActiveGuild().requireValidAlliance());
    public static GuildSetting<Boolean> SHOW_ALLY_DEFENSIVE_WARS = new GuildBooleanSetting(GuildSettingCategory.WAR_ALERTS) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String SHOW_ALLY_DEFENSIVE_WARS(@Me GuildDB db, @Me User user, boolean enabled) {
            return SHOW_ALLY_DEFENSIVE_WARS.setAndValidate(db, user, enabled);
        }
        @Override
        public String help() {
            return "Whether to show defensive war alerts for allies (true/false)";
        }
    }.setupRequirements(f -> f.requires(DEFENSE_WAR_CHANNEL).requiresCoalition(Coalition.ALLIES));
    public static GuildSetting<NationFilter> MENTION_MILCOM_FILTER = new GuildNationFilterSetting(GuildSettingCategory.WAR_ALERTS) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String MENTION_MILCOM_FILTER(@Me GuildDB db, @Me User user, NationFilter value) {
            return MENTION_MILCOM_FILTER.setAndValidate(db, user, value);
        }
        @Override
        public String help() {
            return "A nation filter to apply to limit what wars milcom gets pinged for. ";
        }
    }.setupRequirements(f -> f.requires(DEFENSE_WAR_CHANNEL));

    public static GuildSetting<MessageChannel> OFFENSIVE_WAR_CHANNEL = new GuildChannelSetting(GuildSettingCategory.WAR_ALERTS) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String OFFENSIVE_WAR_CHANNEL(@Me GuildDB db, @Me User user, MessageChannel channel) {
            return OFFENSIVE_WAR_CHANNEL.setAndValidate(db, user, channel);
        }
        @Override
        public String help() {
            return "The #channel to receive alerts for offensive wars\n" +
                    "Members and `" + Roles.FOREIGN_AFFAIRS.name() + "` role are pinged for Do Not Raid (DNR) violations\n" +
                    "To set the `" + Roles.FOREIGN_AFFAIRS.name() + "` role, see: "
                    + CM.role.setAlias.cmd.locutusRole(Roles.FOREIGN_AFFAIRS.name()).discordRole("")
                    + "\n" +
                    "Wars against inactive nones do not create alerts";
        }
    }.setupRequirements(f -> f.requiresAllies().requireActiveGuild().requireValidAlliance());
    public static GuildSetting<Boolean> SHOW_ALLY_OFFENSIVE_WARS = new GuildBooleanSetting(GuildSettingCategory.WAR_ALERTS) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String SHOW_ALLY_OFFENSIVE_WARS(@Me GuildDB db, @Me User user, boolean enabled) {
            return SHOW_ALLY_OFFENSIVE_WARS.setAndValidate(db, user, enabled);
        }
        @Override
        public String help() {
            return "Whether to show offensive war alerts for allies (true/false)";
        }
    }.setupRequirements(f -> f.requires(ALLIANCE_ID).requires(OFFENSIVE_WAR_CHANNEL).requiresCoalition(Coalition.ALLIES));
    public static GuildSetting<Boolean> HIDE_APPLICANT_WARS = new GuildBooleanSetting(GuildSettingCategory.WAR_ALERTS) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String HIDE_APPLICANT_WARS(@Me GuildDB db, @Me User user, boolean value) {
            return HIDE_APPLICANT_WARS.setAndValidate(db, user, value);
        }
        @Override
        public String help() {
            return "Whether to hide war alerts for applicants";
        }
    }.setupRequirements(f -> f.requires(OFFENSIVE_WAR_CHANNEL));
//    public static GuildSetting<MessageChannel> WAR_PEACE_ALERTS = new GuildChannelSetting(GuildSettingCategory.WAR_ALERTS) {
//        @NoFormat
//        @Command(descMethod = "help")
//        @RolePermission(Roles.ADMIN)
//        public String WAR_PEACE_ALERTS(@Me GuildDB db, @Me User user, MessageChannel channel) {
//            return WAR_PEACE_ALERTS.setAndValidate(db, user, channel);
//        }
//        @Override
//        public String help() {
//            return "The #channel to receive alerts for changes to any war peace offers";
//        }
//    }.setupRequirements(f -> f.requires(ALLIANCE_ID));

    public static GuildSetting<Boolean> DISPLAY_ITEMIZED_DEPOSITS = new GuildBooleanSetting(GuildSettingCategory.BANK_INFO) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String DISPLAY_ITEMIZED_DEPOSITS(@Me GuildDB db, @Me User user, boolean enabled) {
            return DISPLAY_ITEMIZED_DEPOSITS.setAndValidate(db, user, enabled);
        }
        @Override
        public String help() {
            return "Whether members's deposits are displayed by default with a breakdown of each category (true/false)";
        }

    }.setupRequirements(f -> f.requiresRole(Roles.MEMBER, true));

    public static GuildSetting<Boolean> DISPLAY_CONDENSED_DEPOSITS = new GuildBooleanSetting(GuildSettingCategory.BANK_INFO) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String DISPLAY_CONDENSED_DEPOSITS(@Me GuildDB db, @Me User user, boolean enabled) {
            return DISPLAY_CONDENSED_DEPOSITS.setAndValidate(db, user, enabled);
        }
        @Override
        public String help() {
            return "Display deposits in a condensed format";
        }
    }.setupRequirements(f -> f.requiresRole(Roles.MEMBER, true));
    public static GuildSetting<MessageChannel> REROLL_ALERT_CHANNEL = new GuildChannelSetting(GuildSettingCategory.GAME_ALERTS) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String REROLL_ALERT_CHANNEL(@Me GuildDB db, @Me User user, MessageChannel channel) {
            return REROLL_ALERT_CHANNEL.setAndValidate(db, user, channel);
        }

        @Override
        public String help() {
            return "The #channel to receive alerts for nation rerolls";
        }
    }.nonPublic().requireActiveGuild();
    public static GuildSetting<MessageChannel> LOST_WAR_CHANNEL = new GuildChannelSetting(GuildSettingCategory.WAR_ALERTS) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String LOST_WAR_CHANNEL(@Me GuildDB db, @Me User user, MessageChannel channel) {
            return LOST_WAR_CHANNEL.setAndValidate(db, user, channel);
        }
        @Override
        public String help() {
            return "The #channel to post wars when our side loses a war";
        }
    }.setupRequirements(f -> f.requires(ALLIANCE_ID));
    public static GuildSetting<MessageChannel> WON_WAR_CHANNEL = new GuildChannelSetting(GuildSettingCategory.WAR_ALERTS) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String WON_WAR_CHANNEL(@Me GuildDB db, @Me User user, MessageChannel channel) {
            return WON_WAR_CHANNEL.setAndValidate(db, user, channel);
        }
        @Override
        public String help() {
            return "The #channel to post wars when our side wins a war (only includes actives)";
        }

    }.setupRequirements(f -> f.requires(ALLIANCE_ID));
    public static GuildSetting<MessageChannel> DEPOSIT_ALERT_CHANNEL = new GuildChannelSetting(GuildSettingCategory.BANK_INFO) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String DEPOSIT_ALERT_CHANNEL(@Me GuildDB db, @Me User user, MessageChannel channel) {
            return DEPOSIT_ALERT_CHANNEL.setAndValidate(db, user, channel);
        }
        @Override
        public String help() {
            return "The #channel to receive alerts when a nation makes a deposit (this will no longer reliably alert)";
        }
    }.setupRequirements(f -> f.requires(ALLIANCE_ID));
    public static GuildSetting<MessageChannel> WITHDRAW_ALERT_CHANNEL = new GuildChannelSetting(GuildSettingCategory.BANK_INFO) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String WITHDRAW_ALERT_CHANNEL(@Me GuildDB db, @Me User user, MessageChannel channel) {
            return WITHDRAW_ALERT_CHANNEL.setAndValidate(db, user, channel);
        }
        @Override
        public String help() {
            return "The #channel to receive alerts when a nation requests a transfer";
        }
    }.setupRequirements(f -> f.requires(ALLIANCE_ID));
    public static GuildSetting<MessageChannel> ADDBALANCE_ALERT_CHANNEL = new GuildChannelSetting(GuildSettingCategory.BANK_INFO) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String ADDBALANCE_ALERT_CHANNEL(@Me GuildDB db, @Me User user, MessageChannel channel) {
            return ADDBALANCE_ALERT_CHANNEL.setAndValidate(db, user, channel);
        }
        @Override
        public String help() {
            return "The #channel to receive alerts when there is a large tranfer in the game or a nation VMs with resources";
        }
    }.setupRequirements(f -> f.requires(ALLIANCE_ID));
    public static GuildSetting<MessageChannel> REPORT_ALERT_CHANNEL = new GuildChannelSetting(GuildSettingCategory.GAME_ALERTS) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String REPORT_ALERT_CHANNEL(@Me GuildDB db, @Me User user, MessageChannel channel) {
            return REPORT_ALERT_CHANNEL.setAndValidate(db, user, channel);
        }
        @Override
        public String help() {
            return "The channel to receive alerts when any nation or user is reported to the bot\n" +
                    "See " + CM.report.add.cmd.toSlashMention();
        }
    }.requireActiveGuild().nonPublic();
    public static GuildSetting<MessageChannel> DELETION_ALERT_CHANNEL = new GuildChannelSetting(GuildSettingCategory.GAME_ALERTS) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String DELETION_ALERT_CHANNEL(@Me GuildDB db, @Me User user, MessageChannel channel) {
            return DELETION_ALERT_CHANNEL.setAndValidate(db, user, channel);
        }
        @Override
        public String help() {
            return "The channel to receive alerts when any nation in the game deletes";
        }
    }.nonPublic().requireActiveGuild();
    public static GuildSetting<GuildDB.AutoNickOption> AUTONICK = new GuildEnumSetting<GuildDB.AutoNickOption>(GuildSettingCategory.ROLE, GuildDB.AutoNickOption.class) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String AUTONICK(@Me GuildDB db, @Me User user, GuildDB.AutoNickOption mode) {
            return AUTONICK.setAndValidate(db, user, mode);
        }
        @Override
        public String help() {
            return "Options: " + StringMan.getString(GuildDB.AutoNickOption.values()) + "\n" +
                    "See also: " + CM.role.clearNicks.cmd.toSlashMention();
        }
    };
    public static GuildSetting<GuildDB.AutoRoleOption> AUTOROLE_ALLIANCES = new GuildEnumSetting<GuildDB.AutoRoleOption>(GuildSettingCategory.ROLE, GuildDB.AutoRoleOption.class) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String AUTOROLE_ALLIANCES(@Me GuildDB db, @Me User user, GuildDB.AutoRoleOption mode) {
            return AUTOROLE_ALLIANCES.setAndValidate(db, user, mode);
        }

        @Override
        public String help() {
            return "Options: " + StringMan.getString(GuildDB.AutoRoleOption.values()) + "\n" +
                    "See also:\n" +
                    "- "
            + CM.coalition.create.cmd.coalitionName(Coalition.MASKED_ALLIANCES.name())
              + "\n" +
                    "- " + CM.role.clearAllianceRoles.cmd.toSlashMention() + "\n" +
                    "- " + AUTOROLE_MEMBERS.getCommandMention() + "\n" +
                    "- " + AUTOROLE_TOP_X.getCommandMention();
        }
    };
    public static GuildSetting<Rank> AUTOROLE_ALLIANCE_RANK = new GuildEnumSetting<Rank>(GuildSettingCategory.ROLE, Rank.class) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String AUTOROLE_ALLIANCE_RANK(@Me GuildDB db, @Me User user, Rank allianceRank) {
            return AUTOROLE_ALLIANCE_RANK.setAndValidate(db, user, allianceRank);
        }
        @Override
        public String help() {
            return "The ingame rank required to get an alliance role.\n" +
                    "Default: member\n" +
                    "Options: " + StringMan.getString(Rank.values());
        }
    }.setupRequirements(f -> f.requires(AUTOROLE_ALLIANCES));
    public static GuildSetting<Boolean> AUTOROLE_MEMBERS = new GuildBooleanSetting(GuildSettingCategory.ROLE) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String AUTOROLE_MEMBERS(@Me GuildDB db, @Me User user, boolean enabled) {
            return AUTOROLE_MEMBERS.setAndValidate(db, user, enabled);
        }

        @Override
        public String help() {
            return "Whether member and applicant roles are automatically added or removed.\n" +
                    "Cannot be used in conjuction with `" + AUTOROLE_ALLY_GOV.name() + "`";
        }
    }.setupRequirements(new Consumer<GuildSetting<Boolean>>() {
        @Override
        public void accept(GuildSetting<Boolean> f) {
            f.requireFunction(d -> {
                d.getOrThrow(GuildKey.ALLIANCE_ID);
            }, "Requires " + GuildKey.ALLIANCE_ID.name() + " to be set").requiresNot(AUTOROLE_ALLY_GOV, false);
        }
    });
    public static GuildSetting<Integer> AUTOROLE_TOP_X = new GuildIntegerSetting(GuildSettingCategory.ROLE) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String AUTOROLE_TOP_X(@Me GuildDB db, @Me User user, Integer topScoreRank) {
            return AUTOROLE_TOP_X.setAndValidate(db, user, topScoreRank);
        }
        @Override
        public String help() {
            return "The number of top alliances to provide roles for, defaults to `0`\n" +
                    "Alliances added to `" + Coalition.MASKED_ALLIANCES + "` are still included outside this range";
        }
    }.setupRequirements(f -> f.requires(AUTOROLE_ALLIANCES));
    public static GuildSetting<Integer> DO_NOT_RAID_TOP_X = new GuildIntegerSetting(GuildSettingCategory.FOREIGN_AFFAIRS) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String DO_NOT_RAID_TOP_X(@Me GuildDB db, @Me User user, Integer topAllianceScore) {
            return DO_NOT_RAID_TOP_X.setAndValidate(db, user, topAllianceScore);
        }
        @Override
        public String help() {
            return "The number of top alliances to include in the Do Not Raid (DNR) list\n" +
                    "Members are not permitted to declare on members of these alliances or their direct allies\n" +
                    "Results in the DNR will be excluded from commands, and will alert Foreign Affairs if violated\n" +
                    "Defaults to `0`";
        }
    }.setupRequirements(f -> f.requires(ALLIANCE_ID));
    public static GuildSetting<Boolean> AUTOROLE_ALLY_GOV = new GuildBooleanSetting(GuildSettingCategory.ROLE) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String AUTOROLE_ALLY_GOV(@Me GuildDB db, @Me User user, boolean enabled) {
            return AUTOROLE_ALLY_GOV.setAndValidate(db, user, enabled);
        }
        @Override
        public String help() {
            return "Whether to give gov/member roles to allies (this is intended for coalition servers), `true` or `false`";
        }
    }.setupRequirements(f -> f.requiresCoalition(Coalition.ALLIES).requiresNot(ALLIANCE_ID, false).requiresNot(AUTOROLE_MEMBERS, false));
    public static GuildSetting<Set<Roles>> AUTOROLE_ALLY_ROLES = new GuildEnumSetSetting<Roles>(GuildSettingCategory.ROLE, Roles.class) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String AUTOROLE_ALLY_ROLES(@Me GuildDB db, @Me User user, Set<Roles> roles) {
            return AUTOROLE_ALLY_ROLES.setAndValidate(db, user, roles);
        }
        @Override
        public String toString(Set<Roles> value) {
            return StringMan.join(value.stream().map(f -> f.name()).collect(Collectors.toList()), ",");
        }

        @Override
        public String help() {
            return "List of roles to autorole from ally servers\n" +
                    "(this is intended for coalition servers to give gov roles to allies)";
        }
    }.setupRequirements(f -> f.requires(AUTOROLE_ALLY_GOV).requiresCoalition(Coalition.ALLIES).requiresNot(ALLIANCE_ID, false));
    public static GuildSetting<Boolean> ENABLE_WAR_ROOMS = new GuildBooleanSetting(GuildSettingCategory.WAR_ROOM) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String ENABLE_WAR_ROOMS(@Me GuildDB db, @Me User user, boolean enabled) {
            return ENABLE_WAR_ROOMS.setAndValidate(db, user, enabled);
        }

        @Override
        public Boolean parse(GuildDB db, String input) {
            db.warChannelInit = false;
            return super.parse(db, input);
        }

        @Override
        public String help() {
            return "If war rooms should be enabled (i.e. auto generate a channel for wars against active nations)\n" +
                    "Note: Defensive war channels must be enabled to have auto war room creation";
        }
    }.setupRequirements(f -> f.requireFunction(d -> {
        d.getOrThrow(GuildKey.ALLIANCE_ID);
    }, "Requires " + GuildKey.ALLIANCE_ID.name() + " to be set"));
    public static GuildSetting<Guild> WAR_SERVER = new GuildSetting<Guild>(GuildSettingCategory.WAR_ROOM, Guild.class) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String WAR_SERVER(@Me GuildDB db, @Me User user, Guild guild) {
            return WAR_SERVER.setAndValidate(db, user, guild);
        }


        @Override
        public String toString(Guild value) {
            return value.getId();
        }

        @Override
        public String toReadableString(GuildDB db, Guild value) {
            return value.toString();
        }

        @Override
        public Guild validate(GuildDB db, User user, Guild guild) {
            GuildDB otherDb = Locutus.imp().getGuildDB(guild);
            if (guild.getIdLong() == db.getGuild().getIdLong())
                throw new IllegalArgumentException("Use "
                        + CM.settings.delete.cmd.key(GuildKey.WAR_SERVER.name())
                        + " to unset the war server");
            if (otherDb.getOrNull(GuildKey.WAR_SERVER, false) != null)
                throw new IllegalArgumentException("Circular reference. The server you have set already defers its war room");
            return guild;
        }

        @Override
        public boolean hasPermission(GuildDB db, User author, Guild guild) {
            if (!super.hasPermission(db, author, guild)) return false;
            if (guild != null && !Roles.ADMIN.has(author, guild))
                throw new IllegalArgumentException("You do not have ADMIN on " + guild);
            return true;
        }

        @Override
        public String help() {
            return "The guild to defer war rooms to";
        }
    }.setupRequirements(f -> f.requires(ENABLE_WAR_ROOMS).requires(ALLIANCE_ID));
    public static GuildSetting<Map.Entry<Integer, Long>> DELEGATE_SERVER = new GuildSetting<Map.Entry<Integer, Long>>(GuildSettingCategory.DEFAULT, Map.class, Integer.class, Long.class) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String DELEGATE_SERVER(@Me GuildDB db, @Me User user, Guild guild) {
            return DELEGATE_SERVER.setAndValidate(db, user, Map.entry(0, guild.getIdLong()));
        }

        @Override
        public Map.Entry<Integer, Long> validate(GuildDB db, User user, Map.Entry<Integer, Long> ids) {
            if (db.getOrNull(ALLIANCE_ID) != null) {
                throw new IllegalArgumentException("You cannot set a delegate a server when this server has `ALLIANCE_ID` set. Remove `DELEGATE_SERVER` or `ALLIANCE_ID` first");
            }
            Guild guild = Locutus.imp().getDiscordApi().getGuildById(ids.getValue());
            if (guild == null)
                throw new IllegalArgumentException("Invalid guild: `" + ids.getValue() + "` (are you sure this Bot is in that server?)");
            GuildDB otherDb = Locutus.imp().getGuildDB(guild);
            if (guild.getIdLong() == db.getIdLong())
                throw new IllegalArgumentException("You cannot set the delegate as this guild");
            if (DELEGATE_SERVER.has(otherDb, false)) {
                throw new IllegalArgumentException("Circular reference. The server you have set already delegates its DELEGATE_SERVER");
            }
            return ids;
        }

        @Override
        public boolean hasPermission(GuildDB db, User author, Map.Entry<Integer, Long> entry) {
            if (!super.hasPermission(db, author, entry)) return false;
            if (entry == null) return true;
            GuildDB otherDB = Locutus.imp().getGuildDB(entry.getValue());
            if (otherDB == null) {
                throw new IllegalArgumentException("Invalid guild: `" + entry.getValue() + "` (are you sure this Bot is in that server?)");
            }
            if (!Roles.ADMIN.has(author, otherDB.getGuild()))
                throw new IllegalArgumentException("You do not have ADMIN on " + otherDB.getGuild());
            return true;
        }

        @Override
        public Map.Entry<Integer, Long> parse(GuildDB db, String input) {
            String[] split2 = input.trim().split("[:|=]", 2);
            Map.Entry<Integer, Long> entry;
            if (split2.length == 2) {
                return Map.entry(Integer.parseInt(split2[0]), Long.parseLong(split2[1]));
            } else {
                return Map.entry(0, Long.parseLong(input));
            }
        }

        @Override
        public String toString(Map.Entry<Integer, Long> value) {
            Map.Entry<Integer, Long> pair = value;
            if (pair.getKey() == 0) return String.valueOf(pair.getValue());
            return pair.getKey() + ":" + pair.getValue();
        }

        @Override
        public String help() {
            return "The guild to delegate unset settings to";
        }
    };
    public static GuildSetting<GuildDB> FA_SERVER = new GuildSetting<GuildDB>(GuildSettingCategory.FOREIGN_AFFAIRS, GuildDB.class) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String FA_SERVER(@Me GuildDB db, @Me User user, Guild guild) {
            GuildDB otherDb = Locutus.imp().getGuildDB(guild);
            return FA_SERVER.setAndValidate(db, user, otherDb);
        }

        @Override
        public GuildDB validate(GuildDB db, User user, GuildDB otherDb) {
            if (otherDb.getIdLong() == db.getGuild().getIdLong())
                throw new IllegalArgumentException("Use "
                         + CM.settings.delete.cmd.key(FA_SERVER.name())
                        + " to unset the FA_SERVER");
            if (FA_SERVER.has(otherDb, false))
                throw new IllegalArgumentException("Circular reference. The server you have set already defers its FA_SERVER");
            return otherDb;
        }

        @Override
        public boolean hasPermission(GuildDB db, User author, GuildDB otherDB) {
            if (!super.hasPermission(db, author, otherDB)) return false;
            if (otherDB != null && !Roles.ADMIN.has(author, otherDB.getGuild()))
                throw new IllegalArgumentException("You do not have ADMIN on " + otherDB.getGuild());
            return true;
        }

        @Override
        public String toString(GuildDB value) {
            return value.getIdLong() + "";
        }

        @Override
        public String toReadableString(GuildDB db, GuildDB value) {
            return value.getName();
        }

        @Override
        public String help() {
            return "The guild to defer coalitions to";
        }
    };
    public static GuildSetting<MessageChannel> PROTECTION_ALERT_CHANNEL = new GuildChannelSetting(GuildSettingCategory.PROTECTION_ALERTS) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String PROTECTION_ALERT_CHANNEL(@Me GuildDB db, @Me User user, MessageChannel channel) {
            return PROTECTION_ALERT_CHANNEL.setAndValidate(db, user, channel);
        }
        @Override
        public String help() {
            return "The #channel to receive alerts when a raid target leaves protection.\n"
                    + CM.role.setAlias.cmd.locutusRole(Roles.PROTECTION_ALERT.name()).discordRole(null)
                    + " must also be set and have members in range";
        }
    }.setupRequirements(f -> f.requireValidAlliance().requires(ALLIANCE_ID).requiresWhitelisted().requireActiveGuild());
    public static GuildSetting<MessageChannel> ENEMY_PROTECTION_ALERT = new GuildChannelSetting(GuildSettingCategory.PROTECTION_ALERTS) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String ENEMY_PROTECTION_ALERT(@Me GuildDB db, @Me User user, MessageChannel channel) {
            return ENEMY_PROTECTION_ALERT.setAndValidate(db, user, channel);
        }
        @Override
        public String help() {
            return "The #channel to receive alerts when an enemy enters protection";
        }
    }.setupRequirements(f -> f.requireValidAlliance().requiresCoalition(Coalition.ENEMIES));
    public static GuildSetting<Map<NationFilter, Role>> CONDITIONAL_ROLES = new GuildSetting<Map<NationFilter, Role>>(GuildSettingCategory.ROLE, Map.class, NationFilter.class, Role.class) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String CONDITIONAL_ROLES(@Me GuildDB db, @Me User user, Map<NationFilter, Role> roleMap) {
            return CONDITIONAL_ROLES.setAndValidate(db, user, roleMap);
        }

        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String addConditionalRole(@Me GuildDB db, @Me User user, NationFilter filter, Role role) {
            Map<NationFilter, Role> existing = CONDITIONAL_ROLES.getOrNull(db, false);
            existing = existing == null ? new HashMap<>() : new LinkedHashMap<>(existing);
            existing.put(filter, role);
            return CONDITIONAL_ROLES.setAndValidate(db, user, existing);
        }

        @Override
        public String toReadableString(GuildDB db, Map<NationFilter, Role> filterToRoles) {
            StringBuilder result = new StringBuilder();
            for (Map.Entry<NationFilter, Role> entry : filterToRoles.entrySet()) {
                result.append(entry.getKey().getFilter() + ":" + entry.getValue().getName() + "\n");
            }
            return result.toString().trim();
        }

        @Override
        public String toString(Map<NationFilter, Role> filterToRoles) {
            StringBuilder result = new StringBuilder();
            for (Map.Entry<NationFilter, Role> entry : filterToRoles.entrySet()) {
                result.append(entry.getKey().getFilter() + ":" + entry.getValue().getId() + "\n");
            }
            return result.toString().trim();
        }

        @Override
        public String help() {
            String response = "Auto assign roles based on conditions\n" +
                    "See: <https://github.com/xdnw/diplocutus/wiki/nation_placeholders>\n" +
                    "Accepts a list of filters to a role.\n" +
                    "In the form:\n" +
                    "```\n" +
                    "#land<10:@someRole\n" +
                    "#land>=10:@otherRole\n" +
                    "```\n" +
                    "Use `*` as the filter to match all nations.\n" +
                    "Only alliance members can be given roles\n" +
                    "Use " + CM.role.autoassign.cmd.toSlashMention() + " to auto assign";

            return response;
        }
    }.setupRequirements(f -> f.requireValidAlliance());

    public static GuildSetting<Map<NationFilter, MMRMatcher>> REQUIRED_MMR = new GuildSetting<Map<NationFilter, MMRMatcher>>(GuildSettingCategory.AUDIT, Map.class, NationFilter.class, MMRMatcher.class) {

        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String REQUIRED_MMR(@Me GuildDB db, @Me User user, Map<NationFilter, MMRMatcher> mmrMap) {
            return REQUIRED_MMR.setAndValidate(db, user, mmrMap);
        }

        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String addRequiredMMR(@Me GuildDB db, @Me User user, NationFilter filter, MMRMatcher mmr) {
            Map<NationFilter, MMRMatcher> existing = REQUIRED_MMR.getOrNull(db, false);
            existing = existing == null ? new HashMap<>() : new LinkedHashMap<>(existing);
            existing.put(filter, mmr);
            return REQUIRED_MMR.setAndValidate(db, user, existing);
        }


        @Override
        public String toString(Map<NationFilter, MMRMatcher> filterToMMR) {
            StringBuilder result = new StringBuilder();
            for (Map.Entry<NationFilter, MMRMatcher> entry : filterToMMR.entrySet()) {
                result.append(entry.getKey().getFilter() + ":" + entry.getValue() + "\n");
            }
            return result.toString().trim();
        }

        @Override
        public String help() {
            String response = "Set the required MMR based on nation criteria\n" +
                    "See: <https://github.com/xdnw/diplocutus/wiki/nation_placeholders>\n" +
                    "Accepts a list of filters to the required MMR.\n" +
                    "In the form:\n" +
                    "```\n" +
                    "#land<60000:30/15/15\n" +
                    "*:60/30/30\n" +
                    "```\n" +
                    "Use `*` as the filter to match all nations.";

            return response;
        }
    }.setupRequirements(f -> f.requires(ALLIANCE_ID));

    public static GuildSetting<MessageChannel> ENEMY_ALERT_CHANNEL = new GuildChannelSetting(GuildSettingCategory.PROTECTION_ALERTS) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String ENEMY_ALERT_CHANNEL(@Me GuildDB db, @Me User user, MessageChannel channel) {
            return ENEMY_ALERT_CHANNEL.setAndValidate(db, user, channel);
        }

        @Override
        public String help() {
            return "The #channel to receive alerts when an enemy nation leaves beige\n" +
                    "Requirements for receiving alerts:\n" +
                    "- Must have the `" + Roles.ENEMY_ALERT.name() + "` or `" + Roles.ENEMY_ALERT_OFFLINE.name() + "` role\n" +
                    "- Be in range (score)\n" +
                    "- active in the past 24h" +
                    "- Have a free offensive war slot\n" +
                    "- Have at least 70% of the target's military\n" +
                    "- Are online, away, or DND on discord, or have the `" + Roles.ENEMY_ALERT_OFFLINE.name() + "` role";
        }
    }.setupRequirements(f -> f.requires(ALLIANCE_ID).requiresCoalition(Coalition.ENEMIES).requireValidAlliance().requireActiveGuild());
    public static GuildSetting<EnemyAlertChannelMode> ENEMY_ALERT_CHANNEL_MODE = new GuildEnumSetting<EnemyAlertChannelMode>(GuildSettingCategory.PROTECTION_ALERTS, EnemyAlertChannelMode.class) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String ENEMY_ALERT_CHANNEL_MODE(@Me GuildDB db, @Me User user, EnemyAlertChannelMode mode) {
            return ENEMY_ALERT_CHANNEL_MODE.setAndValidate(db, user, mode);
        }
        @Override
        public String help() {
            return "The mode for the enemy alert channel to determine what alerts are posted and who is pinged\n" +
                    "Options:\n- " + StringMan.join(EnemyAlertChannelMode.values(), "\n- ");
        }
    }.setupRequirements(f -> f.requires(ENEMY_ALERT_CHANNEL));

    public static GuildSetting<NationFilter> ENEMY_ALERT_FILTER = new GuildSetting<NationFilter>(GuildSettingCategory.PROTECTION_ALERTS, NationFilter.class) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String ENEMY_ALERT_FILTER(@Me GuildDB db, @Me User user, NationFilter filter) {
            return ENEMY_ALERT_FILTER.setAndValidate(db, user, filter);
        }

        @Override
        public String toString(NationFilter value) {
            return value.getFilter();
        }

        @Override
        public String help() {
            return "A filter for enemies to alert on when they leave beige\n" +
                    "Defaults to `#active_m<7200` (active in the past 5 days)";
        }
    }.setupRequirements(f -> f.requires(ENEMY_ALERT_CHANNEL));

    public static GuildSetting<MessageChannel> MEMBER_LEAVE_ALERT_CHANNEL = new GuildChannelSetting(GuildSettingCategory.AUDIT) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String MEMBER_LEAVE_ALERT_CHANNEL(@Me GuildDB db, @Me User user, MessageChannel channel) {
            return MEMBER_LEAVE_ALERT_CHANNEL.setAndValidate(db, user, channel);
        }
        @Override
        public String help() {
            return "The channel to receive alerts when a member leaves";
        }
    }.setupRequirements(f -> f.requireValidAlliance());
    public static GuildSetting<MessageChannel> INTERVIEW_INFO_SPAM = new GuildChannelSetting(GuildSettingCategory.INTERVIEW) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String INTERVIEW_INFO_SPAM(@Me GuildDB db, @Me User user, MessageChannel channel) {
            return INTERVIEW_INFO_SPAM.setAndValidate(db, user, channel);
        }
        @Override
        public String help() {
            return "The channel to receive info spam about expired interview channels";
        }
    }.setupRequirements(f -> f.requireValidAlliance());
    public static GuildSetting<MessageChannel> INTERVIEW_PENDING_ALERTS = new GuildChannelSetting(GuildSettingCategory.INTERVIEW) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String INTERVIEW_PENDING_ALERTS(@Me GuildDB db, @Me User user, MessageChannel channel) {
            return INTERVIEW_PENDING_ALERTS.setAndValidate(db, user, channel);
        }
        @Override
        public String help() {
            return "The channel to receive alerts when a member requests an interview";
        }
    }.setupRequirements(f -> f.requireValidAlliance().requireFunction(db -> {
        Role interviewerRole = Roles.INTERVIEWER.toRole(db.getGuild());
        if (interviewerRole == null) interviewerRole = Roles.MENTOR.toRole(db.getGuild());
        if (interviewerRole == null) interviewerRole = Roles.INTERNAL_AFFAIRS_STAFF.toRole(db.getGuild());
        if (interviewerRole == null) interviewerRole = Roles.INTERNAL_AFFAIRS.toRole(db.getGuild());
        if (interviewerRole == null) {
            throw new IllegalArgumentException("Please use: " + CM.role.setAlias.cmd.toSlashMention() + " to set at least ONE of the following:\n" +
                    StringMan.join(Arrays.asList(Roles.INTERVIEWER, Roles.MENTOR, Roles.INTERNAL_AFFAIRS_STAFF, Roles.INTERNAL_AFFAIRS), ", "));
        }
        // to name
    }, "Please set one of the roles:" + Arrays.asList(Roles.INTERVIEWER, Roles.MENTOR, Roles.INTERNAL_AFFAIRS_STAFF, Roles.INTERNAL_AFFAIRS)
            .stream().map(Enum::name).collect(Collectors.joining(", ")) + " via " + CM.role.setAlias.cmd.toSlashMention()));
    public static GuildSetting<Category> ARCHIVE_CATEGORY = new GuildCategorySetting(GuildSettingCategory.INTERVIEW) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String ARCHIVE_CATEGORY(@Me GuildDB db, @Me User user, Category category) {
            return ARCHIVE_CATEGORY.setAndValidate(db, user, category);
        }
        @Override
        public String help() {
            return "The name or id of the CATEGORY you would like " + CM.channel.close.current.cmd.toSlashMention() + " to move channels to";
        }
    }.setupRequirements(f -> f.requireValidAlliance().requires(INTERVIEW_PENDING_ALERTS));
    public static GuildSetting<MessageChannel> TREATY_ALERTS = new GuildChannelSetting(GuildSettingCategory.FOREIGN_AFFAIRS) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String TREATY_ALERTS(@Me GuildDB db, @Me User user, MessageChannel channel) {
            return TREATY_ALERTS.setAndValidate(db, user, channel);
        }
        @Override
        public String help() {
            return "The #channel to receive alerts for treaty changes";
        }
    }.setupRequirements(f -> f.requireActiveGuild());
    public static GuildSetting<MessageChannel> ALLIANCE_CREATE_ALERTS = new GuildChannelSetting(GuildSettingCategory.FOREIGN_AFFAIRS) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String ALLIANCE_CREATE_ALERTS(@Me GuildDB db, @Me User user, MessageChannel channel) {
            return ALLIANCE_CREATE_ALERTS.setAndValidate(db, user, channel);
        }
        @Override
        public String help() {
            return "The #channel to receive alerts for alliance creation";
        }
    }.setupRequirements(f -> f.requireActiveGuild());
    public static GuildSetting<MessageChannel> GAME_LEADER_CHANGE_ALERT = new GuildChannelSetting(GuildSettingCategory.GAME_ALERTS) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String GAME_LEADER_CHANGE_ALERT(@Me GuildDB db, @Me User user, MessageChannel channel) {
            return GAME_LEADER_CHANGE_ALERT.setAndValidate(db, user, channel);
        }
        @Override
        public String help() {
            return "The #channel to receive alerts when a nation is promoted to leader in an alliance (top 80)";
        }
    }.setupRequirements(f -> f.requireActiveGuild()).nonPublic();
    public static GuildSetting<MessageChannel> GAME_ALLIANCE_EXODUS_ALERTS = new GuildChannelSetting(GuildSettingCategory.GAME_ALERTS) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String GAME_ALLIANCE_EXODUS_ALERTS(@Me GuildDB db, @Me User user, MessageChannel channel) {
            return GAME_ALLIANCE_EXODUS_ALERTS.setAndValidate(db, user, channel);
        }
        @Override
        public String help() {
            return "The #channel to receive alerts when multiple 5+ members leave an alliance\n" +
                    "See also: " + ALLIANCE_EXODUS_TOP_X.getCommandMention();
        }
    }.setupRequirements(f -> f.requireActiveGuild()).nonPublic();
    public static GuildSetting<Integer> ALLIANCE_EXODUS_TOP_X = new GuildIntegerSetting(GuildSettingCategory.GAME_ALERTS) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String ALLIANCE_EXODUS_TOP_X(@Me GuildDB db, @Me User user, int rank) {
            if (rank <= 0) return "Invalid rank: `" + rank + "` (must be > 0)";
            return ALLIANCE_EXODUS_TOP_X.setAndValidate(db, user, rank);
        }
        @Override
        public String help() {
            return "The rank threshold to post exodus alerts for";
        }
    }.setupRequirements(f -> f.requireActiveGuild().requires(GAME_ALLIANCE_EXODUS_ALERTS));
    public static GuildSetting<MessageChannel> ESCALATION_ALERTS = new GuildChannelSetting(GuildSettingCategory.GAME_ALERTS) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String ESCALATION_ALERTS(@Me GuildDB db, @Me User user, MessageChannel channel) {
            return ESCALATION_ALERTS.setAndValidate(db, user, channel);
        }
        @Override
        public String help() {
            return "The #channel to receive alerts for war escalation alerts in orbis";
        }
    }.setupRequirements(f -> f.requireActiveGuild()).nonPublic();
    public static GuildSetting<MessageChannel> ACTIVITY_ALERTS = new GuildChannelSetting(GuildSettingCategory.GAME_ALERTS) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String ACTIVITY_ALERTS(@Me GuildDB db, @Me User user, MessageChannel channel) {
            return ACTIVITY_ALERTS.setAndValidate(db, user, channel);
        }
        @Override
        public String help() {
            return "The #channel to receive alerts for activity (e.g. pre blitz)";
        }
    }.setupRequirements(f -> f.requireActiveGuild()).nonPublic();
    public static GuildSetting<MessageChannel> MEMBER_AUDIT_ALERTS = new GuildChannelSetting(GuildSettingCategory.AUDIT) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String MEMBER_AUDIT_ALERTS(@Me GuildDB db, @Me User user, MessageChannel channel) {
            return MEMBER_AUDIT_ALERTS.setAndValidate(db, user, channel);
        }
        @Override
        public String help() {
            return "The #channel to ping members about audits";
        }
    }.setupRequirements(f -> f.requires(ALLIANCE_ID).requireValidAlliance());
    public static GuildSetting<Set<AutoAuditType>> DISABLED_MEMBER_AUDITS = new GuildEnumSetSetting<AutoAuditType>(GuildSettingCategory.AUDIT, AutoAuditType.class) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String DISABLED_MEMBER_AUDITS(@Me GuildDB db, @Me User user, Set<AutoAuditType> audits) {
            return DISABLED_MEMBER_AUDITS.setAndValidate(db, user, audits);
        }
        @Override
        public String help() {
            return "A comma separated list of audit types to ignore:\n" + StringMan.getString(AutoAuditType.values());
        }
    }.setupRequirements(f -> f.requires(MEMBER_AUDIT_ALERTS).requireValidAlliance());
    public static GuildSetting<NationFilter> WAR_ROOM_FILTER = new GuildNationFilterSetting(GuildSettingCategory.WAR_ALERTS) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String WAR_ROOM_FILTER(@Me GuildDB db, @Me User user, NationFilter value) {
            return WAR_ROOM_FILTER.setAndValidate(db, user, value);
        }
        @Override
        public String help() {
            return "A an additional filter to limit which nations are allowed for a war room.\n" +
                    "War rooms are only created and maintained when attacker and defender match this filter\n";
        }
    }.setupRequirements(f -> f.requires(ENABLE_WAR_ROOMS));
    public static GuildSetting<Boolean> MENTION_MILCOM_COUNTERS = new GuildBooleanSetting(GuildSettingCategory.WAR_ALERTS) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String MENTION_MILCOM_COUNTERS(@Me GuildDB db, @Me User user, boolean value) {
            return MENTION_MILCOM_COUNTERS.setAndValidate(db, user, value);
        }

        @Override
        public String help() {
            return "If the " + Roles.MILCOM.name() + " role is pinged for defensive wars that are counters";
        }
    }.setupRequirements(f -> f.requires(DEFENSE_WAR_CHANNEL));

    public static GuildSetting<MessageChannel> LOAN_REQUEST_ALERTS = new GuildChannelSetting(GuildSettingCategory.BANK_INFO) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String LOAN_REQUEST_ALERTS(@Me GuildDB db, @Me User user, MessageChannel channel) {
            return LOAN_REQUEST_ALERTS.setAndValidate(db, user, channel);
        }

        @Override
        public String help() {
            return "The #channel to receive alerts for loan requests\n" +
                    "`" + Roles.ECON.name() + "` are pinged for loan requests\n"
                    + CM.role.setAlias.cmd.locutusRole(Roles.ECON.name()).discordRole("")
                    ;
        }
    }.setupRequirements(f -> f.requires(API_KEY).requireActiveGuild().requireValidAlliance());

    public static GuildSetting<MessageChannel> GRANT_REQUEST_ALERTS = new GuildChannelSetting(GuildSettingCategory.BANK_INFO) {
        @NoFormat
        @Command(descMethod = "help")
        @RolePermission(Roles.ADMIN)
        public String GRANT_REQUEST_ALERTS(@Me GuildDB db, @Me User user, MessageChannel channel) {
            return GRANT_REQUEST_ALERTS.setAndValidate(db, user, channel);
        }

        @Override
        public String help() {
            return "The #channel to receive alerts for grant requests\n" +
                    "`" + Roles.ECON.name() + "` are pinged for grant requests\n"
                    + CM.role.setAlias.cmd.locutusRole(Roles.ECON.name()).discordRole("")
                    ;
        }
    }.setupRequirements(f -> f.requires(API_KEY).requireActiveGuild().requireValidAlliance());

    private static final Map<String, GuildSetting> BY_NAME = new HashMap<>();

    static {
        // add by field names
        for (Field field : GuildKey.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            if (!GuildSetting.class.isAssignableFrom(field.getType())) continue;
            try {
                GuildSetting setting = (GuildSetting) field.get(null);
                BY_NAME.put(field.getName(), setting);
                setting.setName(field.getName());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public static GuildSetting[] values() {
        return BY_NAME.values().toArray(new GuildSetting[0]);
    }

    public static GuildSetting valueOf(String name) {
        GuildSetting result = BY_NAME.get(name);
        if (result == null) {
            throw new IllegalArgumentException("No such setting: " + name + ". Options:\n- " + StringMan.join(BY_NAME.keySet(), "\n- "));
        }
        return result;
    }
}

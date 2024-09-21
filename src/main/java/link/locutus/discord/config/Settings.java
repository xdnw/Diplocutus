package link.locutus.discord.config;

import link.locutus.discord.Locutus;
import link.locutus.discord.config.yaml.Config;

import java.io.File;
import java.util.*;


public class Settings extends Config {
    @Ignore
    @Final
    public static final Settings INSTANCE = new Settings();

    @Comment({"Override use V2"})
    @Ignore
    @Final
    public static Set<String> WHITELISTED_IPS = new HashSet<>(Arrays.asList("127.0.0.1"));

    @Ignore
    @Final
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36 Edg/107.0.1418.52";

    @Comment({"The discord token of the bot (required)",
            "Found on the bot section of the Discord Developer Portal"})
    public String BOT_TOKEN = "";

    @Comment({"The discord client secret of the bot (required)",
            "Found on the OAuth2 section of the Discord Developer Portal"})
    public String CLIENT_SECRET = "";

    @Comment({"The guild id of the management server for this bot (you should be owner here)",
    "See: https://support.discord.com/hc/en-us/articles/206346498"})
    public long ROOT_SERVER = 0;

    @Comment({"The guild id of the root coalition server (e.g. for spheres)",
            "Defaults to the root-server"})
    public long ROOT_COALITION_SERVER = 0;

    @Comment("Your D&S username (optional)")
    public String USERNAME = "";
    @Comment("Your D&S password (optional)")
    public String PASSWORD = "";

    @Comment({"A map of nation ids to api keys the bot can use for general requests"})
    public Map<Integer, String> API_KEY_POOL = new LinkedHashMap<>();

    @Comment({"The discord id of the bot (generated)",
            "Found in the General Information section of the Discord Developer Portal"})
    public long APPLICATION_ID = 0;

    @Comment("The discord user id of the admin user.")
    public long ADMIN_USER_ID = -1;

    @Comment("The nation id of the admin.")
    public int NATION_ID = 0;


    ////////////////////////////////////////////////////////////////////////////

    @Create
    public ENABLED_COMPONENTS ENABLED_COMPONENTS;
    @Create
    public TASKS TASKS;
    @Create
    public DISCORD DISCORD;
    @Create
    public WEB WEB;
    @Create
    public MODERATION MODERATION;
    @Create
    public LEGACY_SETTINGS LEGACY_SETTINGS;
    @Create
    public ARTIFICIAL_INTELLIGENCE ARTIFICIAL_INTELLIGENCE;
    @Create
    public DATABASE DATABASE;
    @Create
    public BACKUP BACKUP;

    public static String commandPrefix() {
        return Settings.INSTANCE.DISCORD.COMMAND.COMMAND_PREFIX;
    }

    public String DNS_URL() {
        return "https://diplomacyandstrife.com";
    }

    public int ALLIANCE_ID() {
        return Locutus.imp().getNationDB().getNation(Locutus.loader().getNationId()).getAlliance_id();
    }

    public static class ENABLED_COMPONENTS {
        @Comment({"If the discord bot is enabled at all",
                "- Other components require the discord bot to be enabled"})
        public boolean DISCORD_BOT = true;
        @Comment("If slash `/` commands are enabled")
        public boolean SLASH_COMMANDS = true;
        @Comment({"If bot admin only slash commands are registered with discord",
        "If false, you can still use them by mentioning the bot"})
        public boolean REGISTER_ADMIN_SLASH_COMMANDS = true;
        @Comment({"If the web interface is enabled",
                "- If enabled, also configure the web section below"
        })
        public boolean WEB = false;

        @Comment({"Should databases be initialized on startup",
                "false = they are initialized as needed (I havent done much optimization here, so thats probably shortly after startup anyway, lol)"})
        public boolean CREATE_DATABASES_ON_STARTUP = true;

        @Comment({"Should any repeating tasks be enabled",
                "- See the task section to disable/adjust individual tasks"})
        public boolean REPEATING_TASKS = true;

        @Comment("If D&S events should be enabled")
        public boolean EVENTS = true;

        public void disableTasks() {
            CREATE_DATABASES_ON_STARTUP = false;
            REPEATING_TASKS = false;
        }

        public void disableListeners() {
            Settings.INSTANCE.DISCORD.INTENTS.GUILD_MESSAGES = false;
            Settings.INSTANCE.DISCORD.INTENTS.MESSAGE_CONTENT = false;
            Settings.INSTANCE.DISCORD.INTENTS.GUILD_MESSAGE_REACTIONS = false;
            Settings.INSTANCE.DISCORD.INTENTS.DIRECT_MESSAGES = false;
            Settings.INSTANCE.DISCORD.INTENTS.EMOJI = false;
            Settings.INSTANCE.DISCORD.CACHE.MEMBER_OVERRIDES = false;
            Settings.INSTANCE.DISCORD.CACHE.ONLINE_STATUS = false;
            Settings.INSTANCE.DISCORD.CACHE.EMOTE = false;

            Settings.INSTANCE.ENABLED_COMPONENTS.SLASH_COMMANDS = false;

            Settings.INSTANCE.ENABLED_COMPONENTS.WEB = false;
            Settings.INSTANCE.ENABLED_COMPONENTS.EVENTS = false;

            Settings.INSTANCE.ENABLED_COMPONENTS.REPEATING_TASKS = false;

            Settings.INSTANCE.MODERATION.BANNED_ALLIANCES.clear();
            Settings.INSTANCE.MODERATION.BANNED_GUILDS.clear();

            Settings.INSTANCE.ENABLED_COMPONENTS.CREATE_DATABASES_ON_STARTUP = false;
        }
    }

    @Comment({
            "Artificual intelligence is used for features such as natural language responses, search and image processing",
    })
    public static class TASKS {
        @Comment("If any turn related tasks are run (default: true)")
        public boolean ENABLE_TURN_TASKS = true;
//
        @Comment("Fetches most active nations (default 5m)")
        public int ALL_NATION_SECONDS = 5 * 60;

        @Comment("Fetches most active nations (default 5m)")
        public int ALL_ALLIANCE_SECONDS = 5 * 60;

        @Comment("Runs the pre update beige reminders (default: 61 seconds)")
        public int BEIGE_REMINDER_SECONDS = 61;

        @Comment("Fetches alliance treaties (default: 60 minutes)")
        public int TREATY_UPDATE_SECONDS = 60 * 60;

        @Comment("Fetches most active nations (default 10m)")
        public int ALL_BANK_REQUESTS_SECONDS = 10 * 60;

        @Create
        public TURN_TASKS TURN_TASKS;

        public static class TURN_TASKS {
            public boolean ALLIANCE_METRICS = true;
        }
    }

    public static class LEGACY_SETTINGS {
        @Ignore
        @Final
        @Comment("Print the binding types which lack an autocomplete\n" +
                "Disabled by default, as its normal for some types not to have completion")
        public boolean PRINT_MISSING_AUTOCOMPLETE = false;
    }


    @Comment({
            "Prevent users, nations, alliances from using the bot",
            "Indent and then add lines in the form `1234: Your reason`",
            "`banned-bankers` only restricts banking commands for users and nations matching the ids",
    })
    public static class MODERATION {
        /*
        DO NOT ADD HERE FOR PERSONAL REASONS. ONLY IF THEY ARE ABUSING THE BOT
        */
        public Map<Integer, String> BANNED_NATIONS = new LinkedHashMap<>();
        public Map<Long, String> BANNED_USERS = new LinkedHashMap<>();
        public Map<Long, String> BANNED_GUILDS = new LinkedHashMap<>();
        public Map<Integer, String> BANNED_ALLIANCES = new LinkedHashMap<>();
    }

    public static class DISCORD {
        @Create
        public CHANNEL CHANNEL;
        @Create
        public INTENTS INTENTS;
        @Create
        public CACHE CACHE;

        @Create
        public COMMAND COMMAND;

        @Comment({
                "User ids of people who can `!register` other nations",
                "Only give this to trusted people, since it can be abused"
        })
        public List<Long> REGISTER_ANYONE = Arrays.asList();
        @Comment({
                "User ids of people who can `!register` other nations who are applicants in their alliance",
                "Less abusable version of the above, since applicants aren't typically important that impersonation would be too damaging"
        })
        public List<Long> REGISTER_APPLICANTS = Arrays.asList();

        public static class CHANNEL {
            @Comment("The channel id to receive admin alerts in (set to 0 to disable)")
            public long ADMIN_ALERTS = 0;
            @Comment("The channel id to receive error alerts in (set to 0 to disable)")
            public long ERRORS = 0;
        }

        public static class INTENTS {
            @Comment("Can see what members are in a guild")
            public boolean GUILD_MEMBERS = true;
            @Comment({
                    "Can see guild member online status",
                    "Used to limit alerts to online members only",
            })
            public boolean GUILD_PRESENCES = true;
            @Comment("Can see messages sent in guild channels mentioning or replying to this bot")
            public boolean GUILD_MESSAGES = true;
            @Comment({
                    "Can see reactions to messages sent by the bot",
                    "Disabled by default; bot interaction uses buttons now",
                    "The `/embed info` command will not display reaction totals if this is disabled"
            })
            public boolean GUILD_MESSAGE_REACTIONS = false;
            @Comment("Can read direct messages sent to the bot")
            public boolean DIRECT_MESSAGES = true;
            @Comment({"Can see all messages sent in guild channels",
                    "Disabled by default since message content is a whitelisted intent",
                    "Legacy commands require mentioning or replying to the bot"})
            public boolean MESSAGE_CONTENT = false;
            @Comment({
                    "To be able to use custom emojis in embeds as well as the import emoji command",
                    "Disabled by default since it increases discord api usage and is non essential"
            })
            public boolean EMOJI = false;
        }

        public static class CACHE {
            public boolean MEMBER_OVERRIDES = true;
            public boolean ONLINE_STATUS = true;
            public boolean EMOTE = false;
        }

        public static class COMMAND {
            @Comment("The prefix used for message commands (single character)")
            public String COMMAND_PREFIX = "!";
        }
    }


    public static class WEB {
        @Create
        public S3 S3;

        @Comment({"(Optional) Configure AWS S3 bucket for caching war stats",
        "The access key, secret key and region must be set to be enabled",
        "Leave blank to disable"})
        public static final class S3 {
            @Comment("Access key for AWS S3 - for storing binary data")
            public String ACCESS_KEY = "";

            @Comment("Secret Access key for AWS S3 - for storing binary data")
            public String SECRET_ACCESS_KEY = "";

            @Comment("Region of AWS S3 bucket (e.g. `ap-southeast-2`)")
            public String REGION = "";

            @Comment("Name of AWS S3 bucket (e.g. `diplocutus`)")
            public String BUCKET = "";

            @Comment({
                    "The frontend url for war stats",
                    "Not hosted locally, see: <https://github.com/xdnw/lc_stats_svelte/> (github pages)"
            })
            public String SITE = "https://wars.locutus.link";
        }
        @Comment("The port google sheets uses to validate your credentials")
        public int GOOGLE_SHEET_VALIDATION_PORT = 8889;

        @Comment("The port google drive uses to validate your credentials")
        public int GOOGLE_DRIVE_VALIDATION_PORT = 8890;
    }

    @Comment({
            "How often in seconds a task is run (set to 0 to disable)",
            "Note: Diplomacy & Strife is rate limited. You may experience issues if you run tasks too frequently"
    })
    public static class ARTIFICIAL_INTELLIGENCE {

        @Create
        public OCR OCR;

        public static final class OCR {
            @Comment({
                    "The directory of the tesseract files.",
                    "Tesseract is used for optical character recognition (OCR) to read text from images",
                    "To install tesseract, see:",
                    "macOS: `brew install tesseract` and `brew install tesseract-lang`",
                    "linux: `yum install tesseract` and `yum install tesseract-langpack-eng`",
                    "windows: `https://github.com/UB-Mannheim/tesseract/wiki`"
            })
            public String TESSERACT_LOCATION = "src/main/java/tessdata";

            @Comment({"Your API key for <ocr.space> (optional)"})
            public String OCR_SPACE_KEY = "";
        }

        @Create
        public COPILOT COPILOT;

        public static final class COPILOT {
            @Comment({"Allow use of github copilot as on option for chat completions"})
            public boolean ENABLED = false;

            public int USER_TURN_LIMIT = 10;
            public int USER_DAY_LIMIT = 25;
            public int GUILD_TURN_LIMIT = 10;
            public int GUILD_DAY_LIMIT = 25;
        }

        @Create
        public OPENAI OPENAI;

        public static final class OPENAI {
            @Comment({"Your API key from <https://platform.openai.com/account/api-keys> (optional)"})
            public String API_KEY = "";
            public int USER_HOUR_LIMIT = 10;
            public int USER_DAY_LIMIT = 25;
            public int GUILD_HOUR_LIMIT = 10;
            public int GUILD_DAY_LIMIT = 25;
        }

    }

    public static class DATABASE {
        @Create
        public SQLITE SQLITE;

        @Create
        public SYNC SYNC;

        public static final class SQLITE {
            @Comment("Should SQLite be used?")
            public boolean USE = true;
            @Comment("The directory to store the database in")
            public String DIRECTORY = "database";
        }

        public static class SYNC {
            // id of other discord bot
            public long OTHER_BOT_ID = 0;

            // must have message send/delete perms in the channel
            // channel to send sync data in
            public long SYNC_CHANNEL_ID = 0;

            public boolean isEnabled() {
                return OTHER_BOT_ID != 0 && SYNC_CHANNEL_ID != 0;
            }
        }
    }

    public static class BACKUP {
        @Comment({
                "The file location of the backup script to run",
                "Set to empty string to disable backups",
                "e.g. Restic: <https://restic.net/>",
                "Windows Example: <https://gist.github.com/xdnw/a966c4bfe4bf2e1b9fa99ab189d1c41f>",
                "Linux Example: <https://gist.github.com/xdnw/2b3939395961fb4108ab13fe07c43711>",
        })
        public String SCRIPT = "";
        @Comment({"Intervals in turns between backups",
        "Set to 0 to always make a new backup on startup"})
        public int HOURS = 4;
    }

    private File defaultFile = new File("config" + File.separator + "config.yaml");

    public File getDefaultFile() {
        return defaultFile;
    }

    public void reload(File file) {
        this.defaultFile = file;
        load(file);
        save(file);
    }
}
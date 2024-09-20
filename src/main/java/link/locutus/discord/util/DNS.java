package link.locutus.discord.util;

import com.google.common.hash.Hashing;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import link.locutus.discord.Locutus;
import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.api.types.*;
import link.locutus.discord.api.types.tx.Transaction2;
import link.locutus.discord.commands.manager.v2.binding.ValueStore;
import link.locutus.discord.commands.manager.v2.impl.pw.filter.NationPlaceholders;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.NationDB;
import link.locutus.discord.db.entities.*;
import link.locutus.discord.db.guild.GuildKey;
import link.locutus.discord.pnw.NationOrAllianceOrGuild;
import link.locutus.discord.util.discord.DiscordUtil;
import link.locutus.discord.util.offshore.Auth;
import net.dv8tion.jda.api.entities.Guild;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class DNS {
    public static class Development {
        public static double getCost(double costReduction, double development, double land, double amount) {
            if (amount > 1_000_000) {
                throw new IllegalArgumentException("Land being bought is too high (max: 1,000,000, provided: " + MathMan.format(amount) + ")");
            }
            int increment = 50;
            if (amount > increment) {
                double total = 0;
                double finalCostReduction = Land.getCostReductionFromLand(development, land) * costReduction;
                int counter = 0;
                for (int i = 0; i < amount / increment; i++) {
                    double amountCapped = Math.min(increment, amount - i * increment);
                    total += getCostSingle(finalCostReduction, development, amountCapped);
                    development += amountCapped;
                    counter += increment;
                    if (counter >= 10000) {
                        finalCostReduction = Land.getCostReductionFromLand(development, land) * costReduction;
                        counter = 0;
                    }
                }
                return Math.floor(total);
            } else {
                double finalCostReduction = Land.getCostReductionFromLand(development, land) * costReduction;
                return Math.floor(getCostSingle(finalCostReduction, development, amount));
            }
        }

        private static double getCostSingle(double costReduction, double development, double amount) {
            return costReduction * amount * Math.pow((development / 100), 2);
        }
    }

    public static class Land {
        public static double getCostReductionFromLand(double development, double land) {
            return Math.pow(development / land, 2);
        }

        public static double getCost(double costReduction, double totalLand, double amount) {
            if (amount > 1_000_000) {
                throw new IllegalArgumentException("Land being bought is too high (max: 1,000,000, provided: " + MathMan.format(amount) + ")");
            }
            int increment = 50;
            if (amount > increment) {
                double total = 0;
                for (int i = 0; i < amount / increment; i++) {
                    double amountCapped = Math.min(increment, amount - i * increment);
                    total += getCostSingle(costReduction, totalLand, amountCapped);
                    totalLand += amountCapped;
                }
                return Math.floor(total);
            } else {
                return Math.floor(getCostSingle(costReduction, totalLand, amount));
            }
        }

        private static double getCostSingle(double costReduction, double totalLand, double landBeingBought) {
            return costReduction * landBeingBought * Math.pow((totalLand / 100), 2) / 2;
        }
    }
    public static NationModifier getNationModifier(double development, Map<Building, Integer> buildings, Map<Project, Integer> projects, Map<Technology, Integer> tech) {
        NationModifier modifier = new NationModifier();
        modifier.TECH_OUTPUT += (int) Math.floor(development * 0.1);
        for (Map.Entry<Building, Integer> entry : buildings.entrySet()) {
            entry.getKey().apply(modifier, entry.getValue());
        }
        for (Map.Entry<Project, Integer> entry : projects.entrySet()) {
            entry.getKey().apply(modifier, entry.getValue());
        }
        for (Map.Entry<Technology, Integer> entry : tech.entrySet()) {
            entry.getKey().apply(modifier, entry.getValue());
        }
        return modifier;

    }

    public static int getTier(double infra) {
        return (int) ((Math.max(1, infra) + 9_999) / 10_000);
    }
    public static Set<Long> expandCoalition(Collection<Long> coalition) {
        Set<Long> extra = new HashSet<>(coalition);
        for (Long id : coalition) {
            GuildDB other;
            if (id > Integer.MAX_VALUE) {
                other = Locutus.imp().getGuildDB(id);
            } else {
                other = Locutus.imp().getGuildDBByAA(id.intValue());
            }
            if (other != null) {
                for (Integer allianceId : other.getAllianceIds()) {
                    extra.add(allianceId.longValue());
                }
                extra.add(other.getGuild().getIdLong());
            }
        }
        return extra;
    }

    public static String getSphereName(int sphereId) {
        GuildDB db = Locutus.imp().getRootCoalitionServer();
        if (db != null) {
            for (String coalition : db.getCoalitionNames()) {
                Coalition namedCoal = Coalition.getOrNull(coalition);
                if (namedCoal != null) continue;
                Set<Long> ids = db.getCoalitionRaw(coalition);
                if (ids.contains((long) sphereId)) {
                    return coalition;
                }
            }
        }
        return "sphere:" + getName(sphereId, true);
    }

    public static Map<DepositType, double[]> sumNationTransactions(GuildDB guildDB, Set<Long> tracked, List<Map.Entry<Integer, Transaction2>> transactionsEntries) {
        return sumNationTransactions(guildDB, tracked, transactionsEntries, false, false, f -> true);
    }

    /**
     * Sum the nation transactions (assumes all transactions are valid and should be added)
     * @param tracked
     * @param transactionsEntries
     * @return
     */
    public static Map<DepositType, double[]> sumNationTransactions(GuildDB guildDB, Set<Long> tracked, List<Map.Entry<Integer, Transaction2>> transactionsEntries, boolean forceIncludeExpired, boolean forceIncludeIgnored, Predicate<Transaction2> filter) {
        long start = System.currentTimeMillis();
        Map<DepositType, double[]> result = new EnumMap<>(DepositType.class);
        Predicate<Transaction2> allowExpiry = f -> true;
        if (forceIncludeExpired) allowExpiry = f -> false;
        if (tracked == null) {
            tracked = guildDB.getTrackedBanks();
        }

        for (Map.Entry<Integer, Transaction2> entry : transactionsEntries) {
            int sign = entry.getKey();
            Transaction2 record = entry.getValue();
            if (!filter.test(record)) continue;

            boolean isOffshoreSender = (record.sender_type == 2 || record.sender_type == 3) && record.receiver_type == 1;

            boolean allowConversion = record.tx_id != -1 && isOffshoreSender;
            boolean allowArbitraryConversion = record.tx_id != -1 && isOffshoreSender;

            DNS.processDeposit(record, guildDB, tracked, sign, result, record.resources, record.note, record.tx_datetime, allowExpiry, allowConversion, allowArbitraryConversion, true, forceIncludeIgnored);
        }
        long diff = System.currentTimeMillis() - start;
        if (diff > 50) {
            System.out.println("Summed " + transactionsEntries.size() + " transactions in " + diff + "ms");
        }
        return result;
    }

    public static Map<String, String> parseTransferHashNotes(String note) {
        if (note == null || note.isEmpty()) return Collections.emptyMap();

        Map<String, String> result = new LinkedHashMap<>();

        String[] split = note.split("(?=#)");
        for (String filter : split) {
            if (filter.charAt(0) != '#') continue;

            String[] tagSplit = filter.split("[=| ]", 2);
            String tag = tagSplit[0].toLowerCase();
            String value = tagSplit.length == 2 && !tagSplit[1].trim().isEmpty() ? tagSplit[1].split(" ")[0].trim() : null;

            result.put(tag.toLowerCase(), value);
        }
        return result;
    }

    public static void processDeposit(Transaction2 record, GuildDB guildDB, Set<Long> tracked, int sign, Map<DepositType, double[]> result, double[] amount, String note, long date, Predicate<Transaction2> allowExpiry, boolean allowConversion, boolean allowArbitraryConversion, boolean ignoreMarkedDeposits, boolean includeIgnored) {
        /*
        allowConversion sender is nation and alliance has conversion enabled
         */
        if (tracked == null) {
            tracked = guildDB.getTrackedBanks();
        }
        Map<String, String> notes = parseTransferHashNotes(note);
        DepositType type = DepositType.DEPOSIT;
        double decayFactor = 1;

        for (Map.Entry<String, String> entry : notes.entrySet()) {
            String tag = entry.getKey();
            String value = entry.getValue();

            switch (tag) {
                case "#nation":
                case "#alliance":
                case "#guild":
                case "#account":
                    return;
                case "#ignore":
                    if (includeIgnored) {
                        if (value != null && !value.isEmpty() && ignoreMarkedDeposits && MathMan.isInteger(value) && !tracked.contains(Long.parseLong(value))) {
                            return;
                        }
                        continue;
                    }
                    return;
                case "#deposit":
                case "#deposits":
                case "#trade":
                case "#trades":
                case "#trading":
                case "#credits":
                case "#buy":
                case "#sell":
                case "#warchest":
                    if (value != null && !value.isEmpty() && ignoreMarkedDeposits && MathMan.isInteger(value) && !tracked.contains(Long.parseLong(value))) {
                        return;
                    }
                    type = DepositType.DEPOSIT;
                    continue;
                case "#raws":
                case "#raw":
                case "#tax":
                case "#taxes":
                case "#disperse":
                case "#disburse":
                    type = DepositType.TAX;
                    if (value != null && !value.isEmpty() && ignoreMarkedDeposits && MathMan.isInteger(value) && !tracked.contains(Long.parseLong(value))) {
                        return;
                    }
                    continue;
                default:
                    if (!tag.startsWith("#")) continue;
                    continue;
                case "#loan":
                case "#grant":
                    if (value != null && !value.isEmpty() && ignoreMarkedDeposits && MathMan.isInteger(value) && !tracked.contains(Long.parseLong(value))) {
                        return;
                    }
                    if (type == DepositType.DEPOSIT) {
                        type = DepositType.LOAN;
                    }
                    continue;
                case "#decay": {
                    if (allowExpiry.test(record) && value != null && !value.isEmpty()) {
                        try {
                            long now = System.currentTimeMillis();
                            long expire = TimeUtil.timeToSec(value, record.tx_datetime, true) * 1000L;
                            if (now > date + expire) {
                                return;
                            }
                            decayFactor = Math.min(decayFactor, 1 - (now - date) / (double) expire);
                            type = DepositType.GRANT;
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                            type = DepositType.LOAN;
                        }
                    }
                    continue;
                }
                case "#expire": {
                    if (allowExpiry.test(record) && value != null && !value.isEmpty()) {
                        try {
                            long now = System.currentTimeMillis();
                            long expire = TimeUtil.timeToSec(value, record.tx_datetime, true) * 1000L;
                            if (now > date + expire) {
                                return;
                            }
                            type = DepositType.GRANT;
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                            type = DepositType.LOAN;
                        }
                    }
                    continue;
                }
            }
        }
        double[] rss = result.computeIfAbsent(type, f -> ResourceType.getBuffer());
        if (sign == 1 && decayFactor == 1) {
            ResourceType.add(rss, amount);
        } else if (sign == -1 && decayFactor == 1) {
            ResourceType.subtract(rss, amount);
        } else {
            double factor = decayFactor * sign;
            for (int i = 0; i < rss.length; i++) {
                rss[i] += amount[i] * factor;
            }
        }
    }

    public static double WAR_RANGE_MAX_MODIFIER_ACTIVE = 2.00;
    public static double WAR_RANGE_MIN_MODIFIER_ACTIVE = 0.66;

    public static double WAR_RANGE_MAX_MODIFIER_INACTIVE = 4.00;
    public static double WAR_RANGE_MIN_MODIFIER_INACTIVE = 0.25;

    public static double ESPIONAGE_RANGE_MAX_MODIFIER = 4.00;
    public static double ESPIONAGE_RANGE_MIN_MODIFIER = 0.25;

    /**
     * @param offensive (else defensive)
     * @param isWar (else spy)
     * @param isMin (else max)
     * @return
     */
    public static double getAttackRange(boolean offensive, boolean isWar, boolean isMin, boolean isActive, double score) {
        long scoreInt = Math.round(score * 100);
        long range;
        if (offensive) {
            if (isWar) {
                if (isMin) {
                    range = Math.round(scoreInt * (isActive ? WAR_RANGE_MIN_MODIFIER_ACTIVE : WAR_RANGE_MIN_MODIFIER_INACTIVE));
                } else {
                    range = Math.round(scoreInt * (isActive ? WAR_RANGE_MAX_MODIFIER_ACTIVE : WAR_RANGE_MAX_MODIFIER_INACTIVE));
                }
            } else {
                if (isMin) {
                    range = Math.round(scoreInt * ESPIONAGE_RANGE_MIN_MODIFIER);
                } else {
                    range = Math.round(scoreInt * ESPIONAGE_RANGE_MAX_MODIFIER);
                }
            }
        } else {
            if (isWar) {
                if (isMin) {
                    range = Math.round(scoreInt / (isActive ? WAR_RANGE_MAX_MODIFIER_ACTIVE : WAR_RANGE_MAX_MODIFIER_INACTIVE));
                } else {
                    range = Math.round(scoreInt / (isActive ? WAR_RANGE_MIN_MODIFIER_ACTIVE : WAR_RANGE_MIN_MODIFIER_INACTIVE));
                }
            } else {
                if (isMin) {
                    range = Math.round(scoreInt / ESPIONAGE_RANGE_MAX_MODIFIER);
                } else {
                    range = Math.round(scoreInt / ESPIONAGE_RANGE_MIN_MODIFIER);
                }
            }
        }
        return range * 0.01;
    }

    public static Map.Entry<double[], String> createDepositEmbed(GuildDB db, NationOrAllianceOrGuild nationOrAllianceOrGuild, Map<DepositType, double[]> categorized, Boolean showCategories, boolean condenseFormat) {
        StringBuilder response = new StringBuilder();

        List<String> footers = new ArrayList<>();
        if (!condenseFormat) {
            footers.add("value is based on current market prices");
        }

        double[] balance = ResourceType.getBuffer();
        double[] nonBalance = ResourceType.getBuffer();
        List<String> balanceNotes = new ArrayList<>(Arrays.asList("#deposit", "#tax", "#loan", "#grant", "#expire", "#decay"));

        List<String> excluded = new ArrayList<>(Arrays.asList("/escrow"));
        for (Map.Entry<DepositType, double[]> entry : categorized.entrySet()) {
            double[] current = entry.getValue();
            ResourceType.add(nonBalance, current);
        }

        if (showCategories) {
            if (categorized.containsKey(DepositType.DEPOSIT)) {
                response.append("**`#DEPOSIT`** worth $" + MathMan.format(ResourceType.convertedTotal(categorized.get(DepositType.DEPOSIT))));
                response.append("\n```").append(ResourceType.resourcesToString(categorized.get(DepositType.DEPOSIT))).append("```\n");
            }
            if (categorized.containsKey(DepositType.TAX)) {
                response.append("**`#TAX`** worth $" + MathMan.format(ResourceType.convertedTotal(categorized.get(DepositType.TAX))));
                response.append("\n```").append(ResourceType.resourcesToString(categorized.get(DepositType.TAX))).append("```\n");
            } else if (nationOrAllianceOrGuild.isNation()) {
                footers.add("No tax records are added to deposits");
            }
            if (categorized.containsKey(DepositType.LOAN)) {
                response.append("**`#LOAN/#GRANT`** worth $" + MathMan.format(ResourceType.convertedTotal(categorized.get(DepositType.LOAN))));
                response.append("\n```").append(ResourceType.resourcesToString(categorized.get(DepositType.LOAN))).append("```\n");
            }
            if (categorized.containsKey(DepositType.GRANT)) {
                response.append("**`#EXPIRE`** worth $" + MathMan.format(ResourceType.convertedTotal(categorized.get(DepositType.GRANT))));
                response.append("\n```").append(ResourceType.resourcesToString(categorized.get(DepositType.GRANT))).append("```\n");
            }
            if (categorized.size() > 1) {
                response.append("**Balance:** (`" + StringMan.join(balanceNotes, "`|`") + "`) worth: $" + MathMan.format(ResourceType.convertedTotal(balance)) + ")");
                response.append("\n```").append(ResourceType.resourcesToString(balance)).append("``` ");
            }
        } else {
            String prefix = condenseFormat ? "**" : "## ";
            String suffix = condenseFormat ? "**" : "";
            response.append(prefix + "Balance:" + suffix);
            if (condenseFormat) {
                response.append(" worth: `$" + MathMan.format(ResourceType.convertedTotal(balance)) + "`\n");
                response.append("```" + ResourceType.resourcesToString(balance) + "``` ");
            } else {
                response.append("\n").append(ResourceType.resourcesToFancyString(balance)).append("\n");
            }
            response.append("**Includes:** `" + StringMan.join(balanceNotes, "`, `")).append("`\n");
            response.append("**Excludes:** `" + StringMan.join(excluded, "`, `")).append("`\n");

            if (!ResourceType.isZero(nonBalance)) {
                response.append("\n" + prefix + "Expiring Debt:" + suffix + "\n");
                response.append("In addition to your balance, you owe the following:\n");
                response.append("```\n" + ResourceType.resourcesToString(nonBalance)).append("```\n- worth: $" + MathMan.format(ResourceType.convertedTotal(nonBalance)) + "\n");
            }
        }
        return Map.entry(balance, response.toString());
    }

    public static Integer parseAllianceId(String arg) {
        String lower = arg.toLowerCase();
        if (lower.startsWith("aa:")) arg = arg.substring(3);
        else if (lower.startsWith("alliance:")) arg = arg.substring(9);
        if (arg.charAt(0) == '"' && arg.charAt(arg.length() - 1) == '"') {
            arg = arg.substring(1, arg.length() - 1);
        }
        if (arg.equalsIgnoreCase("none")) {
            return 0;
        }
        if (arg.startsWith(Settings.INSTANCE.DNS_URL() + "/alliance/") || arg.startsWith("" + Settings.INSTANCE.DNS_URL() + "/alliance/")) {
            String[] split = arg.split("/alliance/");
            if (split.length == 2) {
                arg = split[1].replaceAll("/", "");
            }
        }
        if (MathMan.isInteger(arg)) {
            try {
                return Integer.parseInt(arg);
            } catch (NumberFormatException e) {}
        }
        {
            DBAlliance alliance = Locutus.imp().getNationDB().getAllianceByName(arg);
            if (alliance != null) {
                return alliance.getAlliance_id();
            }
        }
        if (arg.contains("=HYPERLINK") && arg.contains("alliance/")) {
            String regex = "alliance/([0-9]+)";
            Matcher m = Pattern.compile(regex).matcher(arg);
            m.find();
            arg = m.group(1);
            return Integer.parseInt(arg);
        }
        return null;
    }

    public static double[] normalize(double[] resources) {
        return ResourceType.resourcesToArray(DNS.normalize(ResourceType.resourcesToMap(resources)));
    }

    public static Map<ResourceType, Double> normalize(Map<ResourceType, Double> resources) {
        resources = new LinkedHashMap<>(resources);
        double total = ResourceType.convertedTotal(resources);
        if (total == 0) return new HashMap<>();
        if (total < 0) {
            return new HashMap<>();
        }
        double postiveTotal = ResourceType.convertedTotal(resources);
        double factor = Math.max(0, Math.min(1, total / postiveTotal));
        for (ResourceType type : ResourceType.values()) {
            Double value = resources.get(type);
            if (value == null || value == 0) continue;

            resources.put(type, value * factor);
        }
        return resources;
    }

    public static <T> T withLogin(Callable<T> task, Auth auth) {
        synchronized (auth)
        {
            try {
                auth.login(false);
                return task.call();
            } catch (Exception e) {
                AlertUtil.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
    }

    public static double[] multiply(double[] a, double factor) {
        for (int i = 0; i < a.length; i++) {
            a[i] *= factor;
        }
        return a;
    }

    public static <T extends Number> Map<ResourceType, T> multiply(Map<ResourceType, T> a, T value) {
        HashMap<ResourceType, T> copy = new HashMap<>(a);
        for (Map.Entry<ResourceType, T> entry : copy.entrySet()) {
            entry.setValue(MathMan.multiply(entry.getValue(), value));
        }
        return copy;
    }

    public static String parseDom(Element dom, String clazz) {
        for (Element element : dom.getElementsByClass(clazz)) {
            String text = element.text();
            if (text.startsWith("Player Advertisement by ")) {
                continue;
            }
            return element.text();
        }
        return null;
    }

    public static String getMarkdownUrl(int nationId, boolean isAA) {
        return MarkupUtil.markdownUrl(DNS.getName(nationId, isAA), "<" + DNS.getUrl(nationId, isAA) + ">");
    }

    public static String getName(long nationOrAllianceId, boolean isAA) {
        if (isAA) {
            String name = Locutus.imp().getNationDB().getAllianceName((int) nationOrAllianceId);
            return name != null ? name : nationOrAllianceId + "";
        } else if (Math.abs(nationOrAllianceId) < Integer.MAX_VALUE) {
            DBNation nation = Locutus.imp().getNationDB().getNation((int) nationOrAllianceId);
            return nation != null ? nation.getNation() : nationOrAllianceId + "";
        } else {
            Guild guild = Locutus.imp().getDiscordApi().getGuildById(Math.abs(nationOrAllianceId));
            return guild != null ? guild.getName() : nationOrAllianceId + "";
        }
    }

    public static String getBBUrl(int nationOrAllianceId, boolean isAA) {
        String type;
        String name;
        if (isAA) {
            type = "alliance";
            name = Locutus.imp().getNationDB().getAllianceName(nationOrAllianceId);
            name = name != null ? name : nationOrAllianceId + "";
        } else {
            type = "nation";
            DBNation nation = Locutus.imp().getNationDB().getNation(nationOrAllianceId);
            name = nation != null ? nation.getNation() : nationOrAllianceId + "";
        }
        String url = "" + Settings.INSTANCE.DNS_URL() + "/" + type + "/" + nationOrAllianceId;
        return String.format("[%s](%s)", name, url);
    }

    public static String getUrl(int nationOrAllianceId, boolean isAA) {
        String type;
        String name;
        if (isAA) {
            type = "alliance";
            name = Locutus.imp().getNationDB().getAllianceName(nationOrAllianceId);
            name = name != null ? name : nationOrAllianceId + "";
        } else {
            type = "nation";
            DBNation nation = Locutus.imp().getNationDB().getNation(nationOrAllianceId);
            name = nation != null ? nation.getNation() : nationOrAllianceId + "";
        }
        return "" + Settings.INSTANCE.DNS_URL() + "/" + type + "/" + nationOrAllianceId;
    }

    public static String getNationUrl(int nationId) {
        return "" + Settings.INSTANCE.DNS_URL() + "/nation/" + nationId;
    }

    public static String getAllianceUrl(int cityId) {
        return "" + Settings.INSTANCE.DNS_URL() + "/alliance/" + cityId;
    }

    public static BiFunction<Double, Double, Integer> getIsNationsInScoreRange(Collection<DBNation> attackers) {
        int minScore = Integer.MAX_VALUE;
        int maxScore = 0;
        for (DBNation attacker : attackers) {
            double minModifier = attacker.isInactiveForWar() ? DNS.WAR_RANGE_MIN_MODIFIER_INACTIVE : DNS.WAR_RANGE_MIN_MODIFIER_ACTIVE;
            double maxModifier = attacker.isInactiveForWar() ? DNS.WAR_RANGE_MAX_MODIFIER_INACTIVE : DNS.WAR_RANGE_MAX_MODIFIER_ACTIVE;
            minScore = (int) Math.min(minScore, attacker.getScore() * minModifier);
            maxScore = (int) Math.max(maxScore, attacker.getScore() / minModifier);
        }
        int[] scoreRange = new int[maxScore + 1];
        for (DBNation attacker : attackers) {
            scoreRange[(int) attacker.getScore()]++;
        }
        int total = 0;
        for (int i = 0; i < scoreRange.length; i++) {
            total += scoreRange[i];
            scoreRange[i] = total;
        }
        return new BiFunction<Double, Double, Integer>() {
            @Override
            public Integer apply(Double min, Double max) {
                max = Math.min(scoreRange.length - 1, max);
                min = Math.min(scoreRange.length - 1, min);
                return scoreRange[(int) Math.ceil(max)] - scoreRange[min.intValue()];
            }
        };
    }

    public static BiFunction<Double, Double, Double> getXInRange(Collection<DBNation> attackers, Function<DBNation, Double> valueFunc) {
        int minScore = Integer.MAX_VALUE;
        int maxScore = 0;
        for (DBNation attacker : attackers) {
            double minModifier = attacker.isInactiveForWar() ? DNS.WAR_RANGE_MIN_MODIFIER_INACTIVE : DNS.WAR_RANGE_MIN_MODIFIER_ACTIVE;
            double maxModifier = attacker.isInactiveForWar() ? DNS.WAR_RANGE_MAX_MODIFIER_INACTIVE : DNS.WAR_RANGE_MAX_MODIFIER_ACTIVE;
            minScore = (int) Math.min(minScore, attacker.getScore() * minModifier);
            maxScore = (int) Math.max(maxScore, attacker.getScore() / minModifier);
        }
        double[] scoreRange = new double[maxScore + 1];
        for (DBNation attacker : attackers) {
            scoreRange[(int) attacker.getScore()] += valueFunc.apply(attacker);
        }
        int total = 0;
        for (int i = 0; i < scoreRange.length; i++) {
            total += scoreRange[i];
            scoreRange[i] = total;
        }
        return new BiFunction<Double, Double, Double>() {
            @Override
            public Double apply(Double min, Double max) {
                max = Math.min(scoreRange.length - 1, max);
                return scoreRange[(int) Math.ceil(max)] - scoreRange[min.intValue()];
            }
        };
    }

    public static BiFunction<Double, Double, Integer> getIsNationsInSpyRange(Collection<DBNation> attackers) {
        int minScore = Integer.MAX_VALUE;
        int maxScore = 0;
        for (DBNation attacker : attackers) {
            double minModifier = DNS.ESPIONAGE_RANGE_MIN_MODIFIER;
            double maxModifier = DNS.ESPIONAGE_RANGE_MAX_MODIFIER;
            minScore = (int) Math.min(minScore, attacker.getScore() / maxModifier);
            maxScore = (int) Math.max(maxScore, attacker.getScore() / minModifier);
        }
        int[] scoreRange = new int[maxScore + 1];
        for (DBNation attacker : attackers) {
            scoreRange[(int) attacker.getScore()]++;
        }
        int total = 0;
        for (int i = 0; i < scoreRange.length; i++) {
            total += scoreRange[i];
            scoreRange[i] = total;
        }
        return new BiFunction<Double, Double, Integer>() {
            @Override
            public Integer apply(Double min, Double max) {
                int minVal = min == 0 ? 0 : scoreRange[Math.min(scoreRange.length - 1, min.intValue() - 1)];
                int maxVal = scoreRange[Math.min(scoreRange.length - 1, max.intValue())];
                return maxVal - minVal;
            }
        };
    }

    public static Set<Integer> parseAlliances(GuildDB db, String arg) {
        Set<Integer> aaIds = new LinkedHashSet<>();
        for (String allianceName : arg.split(",")) {
            Set<Integer> coalition = db == null ? Collections.emptySet() : db.getCoalition(allianceName);
            if (!coalition.isEmpty()) aaIds.addAll(coalition);
            else {
                Integer aaId = DNS.parseAllianceId(allianceName);
                if (aaId == null) throw new IllegalArgumentException("Unknown alliance: `" + allianceName + "`");
                aaIds.add(aaId);
            }
        }
        return aaIds;
    }

    public static <E extends Enum<E>, V extends Number> Map<E, V> parseEnumMap(String arg, Class<E> enumClass, Class<V> valueClass) {
        arg = arg.trim();
        if (!arg.contains(":") && !arg.contains("=")) arg = arg.replaceAll("[ ]+", ":");
        arg = arg.replace(" ", "").replace('=', ':').replaceAll("([0-9]),([0-9])", "$1$2").toUpperCase();
        for (E unit : enumClass.getEnumConstants()) {
            String name = unit.name();
            arg = arg.replace(name.toUpperCase() + ":", name + ":");
        }

        double sign = 1;
        if (arg.charAt(0) == '-') {
            sign = -1;
            arg = arg.substring(1);
        }
        int preMultiply = arg.indexOf("*{");
        int postMultiply = arg.indexOf("}*");
        if (preMultiply != -1) {
            String[] split = arg.split("\\*\\{", 2);
            arg = "{" + split[1];
            sign *= Double.parseDouble(split[0]);
        }
        if (postMultiply != -1) {
            String[] split = arg.split("\\}\\*", 2);
            arg = split[0] + "}";
            sign *= Double.parseDouble(split[1]);
        }

        Type type = com.google.gson.reflect.TypeToken.getParameterized(Map.class, enumClass, valueClass).getType();
        if (arg.charAt(0) != '{' && arg.charAt(arg.length() - 1) != '}') {
            arg = "{" + arg + "}";
        }
        Map<E, V> result = new Gson().fromJson(arg, type);
        if (result.containsKey(null)) {
            throw new IllegalArgumentException("Invalid resource type specified in map: `" + arg + "`");
        }
        if (sign != 1) {
            for (Map.Entry<E, V> entry : result.entrySet()) {
                entry.setValue(multiply(entry.getValue(), sign, valueClass));
            }
        }
        return result;
    }

    private static <V extends Number> V multiply(V value, double factor, Class<V> valueClass) {
        if (valueClass == Long.class) {
            return valueClass.cast((long) (value.longValue() * factor));
        } else if (valueClass == Double.class) {
            return valueClass.cast(value.doubleValue() * factor);
        } else if (valueClass == Integer.class) {
            return valueClass.cast((int) (value.intValue() * factor));
        } else {
            throw new IllegalArgumentException("Unsupported value type: " + valueClass);
        }
    }

    public static Map<MilitaryUnit, Long> parseUnits(String arg) {
        return parseEnumMap(arg, MilitaryUnit.class, Long.class);
    }

    public static Map<Building, Integer> parseBuildings(String arg) {
        return parseEnumMap(arg, Building.class, Integer.class);
    }

    public static Map<String, String> parseMap(String arg) {
        if (arg.charAt(0) != '{' && arg.charAt(arg.length() - 1) != '}') {
            arg = arg.trim();
            if (!arg.contains(":") && !arg.contains("=")) arg = arg.replaceAll("[ ]+", ":");
            arg = arg.replace(" ", "").replace('=', ':');
            if (arg.charAt(0) != '{' && arg.charAt(arg.length() - 1) != '}') {
                arg = "{" + arg + "}";
            }
        }
        Type type = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> result = new Gson().fromJson(arg, type);
        if (result.containsKey(null)) {
            throw new IllegalArgumentException("Invalid type specified in map: `" + arg + "`");
        }
        return result;
    }

    public static double estimateScore(double EducationIndex,
                                  double PowerIndex,
                                  double EmploymentIndex,
                                  double TransportationIndex,
                                  double StabilityIndex,
                                  double CommerceIndex,
                                  double Development,
                                  double Land,
                                  double Devastation,
                                  double WarIndex,
                                  double TechIndex,
                                  double UnfilledJobsPenality) {
        double OtherIndex = (EducationIndex + PowerIndex + EmploymentIndex + TransportationIndex + StabilityIndex + CommerceIndex) / (6 * UnfilledJobsPenality);
        double score = (Development + (Land / 25) - (Devastation / 2)) * (WarIndex + TechIndex * 2 + OtherIndex) / 4450;
        return score;
    }

    public static double estimateUnfilledJobsPenalty(double score,
                                    double EducationIndex,
                                    double PowerIndex,
                                    double EmploymentIndex,
                                    double TransportationIndex,
                                    double StabilityIndex,
                                    double CommerceIndex,
                                    double Development,
                                    double Land,
                                    double Devastation,
                                    double WarIndex,
                                  double TechIndex) {
        double OtherIndex = ((score * 4450) / (Development + (Land / 25) - (Devastation / 2))) - WarIndex - TechIndex * 2;
        return (EducationIndex + PowerIndex + EmploymentIndex + TransportationIndex + StabilityIndex + CommerceIndex) / (6 * OtherIndex);
    }

    public static boolean isInScoreRange(double defenderScore, double attackerScore, boolean isWar, boolean isDefenderActive) {
        double min = getAttackRange(false, isWar, true, isDefenderActive, defenderScore);
        double max = getAttackRange(false, isWar, false, isDefenderActive, defenderScore);
        return attackerScore >= min && attackerScore <= max;
    }

    public static Set<DBNation> getNationsSnapshot(Collection<DBNation> nations, String filterStr, Long snapshotDate, Guild guild, boolean includeVM) {
        if (snapshotDate != null) {
            throw new UnsupportedOperationException("Snapshot date is not yet supported");
        }
        return new ObjectOpenHashSet<>(nations);
    }
}
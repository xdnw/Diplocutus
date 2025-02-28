package link.locutus.discord.api.generated;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import it.unimi.dsi.fastutil.io.FastByteArrayInputStream;
import it.unimi.dsi.fastutil.io.FastByteArrayOutputStream;
import link.locutus.discord.Locutus;
import link.locutus.discord.commands.manager.v2.binding.annotation.Command;
import link.locutus.discord.commands.manager.v2.binding.annotation.NoFormat;
import link.locutus.discord.commands.manager.v2.binding.bindings.PrimitiveBindings;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.util.IOUtil;
import link.locutus.discord.util.MathMan;
import link.locutus.discord.util.StringMan;
import link.locutus.discord.util.math.ArrayUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public enum ResourceType {
    CASH(f -> f.Cash, f -> f.AllianceCash, f -> f.AllianceBankCash, f -> f.AllianceTaxIncome),
    TECHNOLOGY(f -> f.Tech, f -> f.AllianceTech, f -> f.AllianceBankTech, f -> f.AllianceTechTaxIncome),
    PRODUCTION(f -> f.Production, f -> 0d, f -> 0d, f -> 0d),
    MINERALS(f -> f.Minerals, f -> f.AllianceMinerals, f -> f.AllianceBankMinerals, f -> f.AllianceMineralTaxIncome),
    URANIUM(f -> f.Uranium, f -> f.AllianceUranium, f -> f.AllianceBankUranium, f -> f.AllianceUraniumTaxIncome),
    RARE_METALS(f -> f.RareMetals, f -> f.AllianceRareMetal, f -> f.AllianceBankRareMetal, f -> f.AllianceRareMetalTaxIncome),
    FUEL(f -> f.Fuel, f -> f.AllianceFuel, f -> f.AllianceBankFuel, f -> f.AllianceFuelTaxIncome),
    POLITICAL_SUPPORT(f -> f.PoliticalPower, f -> 0d, f -> 0d, f -> 0d),
    CRYPTO(f -> 0d, f -> 0d, f -> 0d, f -> 0d)

    ;

    private final Function<AllianceMemberFunds, Double> getNation;
    private final Function<AllianceBankValues, Double> getAlliance;
    private final Function<AllianceBankValues, Double> getMember;
    private final Function<AllianceTaxIncome, Double> getTax;

    ResourceType(Function<AllianceMemberFunds, Double> getNation, Function<AllianceBankValues, Double> getAlliance, Function<AllianceBankValues, Double> getMember, Function<AllianceTaxIncome, Double> getTax) {
        this.getNation = getNation;
        this.getAlliance = getAlliance;
        this.getMember = getMember;
        this.getTax = getTax;
    }

    public static final ResourceType[] values = values();
    public static final List<ResourceType> valuesList = Arrays.asList(values);

    private static final Type RESOURCE_TYPE = new TypeToken<Map<ResourceType, Double>>() {}.getType();
    private static final Gson RESOURCE_GSON = new GsonBuilder()
            .registerTypeAdapter(RESOURCE_TYPE, new DoubleDeserializer())
            .create();

    public double getNation(AllianceMemberFunds stockpile) {
        return getNation.apply(stockpile);
    }

    public double getAlliance(AllianceBankValues stockpile) {
        return getAlliance.apply(stockpile);
    }

    public double getTax(AllianceTaxIncome stockpile) {
        return getTax.apply(stockpile);
    }

    public double getMember(AllianceBankValues stockpile) {
        return getMember.apply(stockpile);
    }

    private static class DoubleDeserializer implements JsonDeserializer<Map<ResourceType, Double>> {
        @Override
        public Map<ResourceType, Double> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            Map<ResourceType, Double> map = new LinkedHashMap<>();
            json.getAsJsonObject().entrySet().forEach(entry -> {
                ResourceType key = ResourceType.valueOf(entry.getKey());
                Double value = PrimitiveBindings.Double(entry.getValue().getAsString());
                map.put(key, value);
            });
            return map;
        }
    }

    @Command(desc = "The name of this resource")
    public String getName() {
        return name().toLowerCase();
    }

    public static final ResourceType parseChar(Character s) {
        if (s == '$') return CASH;
        // ignore MONEY and CREDITS
        for (ResourceType type : values) {
            if (type == PRODUCTION) continue;
            if (Character.toLowerCase(type.getName().charAt(0)) == Character.toLowerCase(s)) {
                return type;
            }
        }
        return null;
    }

    public static Map<ResourceType, Double> roundResources(Map<ResourceType, Double> resources) {
        HashMap<ResourceType, Double> copy = new HashMap<>(resources);
        for (Map.Entry<ResourceType, Double> entry : copy.entrySet()) {
            entry.setValue(Math.round(entry.getValue() * 100.0) / 100.0);
        }
        return copy;
    }

    public static Map<ResourceType, Double> parseResources(String arg) {
        if (arg.endsWith("},")) {
            arg = arg.substring(0, arg.length() - 1);
        }
        boolean allowBodmas = arg.contains("{") && StringMan.containsAny("+-*/^%", arg.replaceAll("\\{[^}]+}", ""));
        return parseResources(arg, allowBodmas);
    }

    public static Map<ResourceType, Double> parseResources(String arg, boolean allowBodmas) {
        if (MathMan.isInteger(arg)) {
            throw new IllegalArgumentException("Please use `$" + arg + "` or `money=" + arg + "` for money, not `" + arg + "`");
        }
        if (arg.contains(" AM ") || arg.contains(" PM ")) {
            arg = arg.replaceAll("([0-9]{1,2}:[0-9]{2})[ ](AM|PM)", "")
                    .replace("\n", " ")
                    .replaceAll("[ ]+", " ");
        }
        if (arg.contains("---+") && arg.contains("-+-")) {
            arg = arg.replace("-+-", "---");
            int start = arg.indexOf("---+");
            arg = arg.substring(start + 4).trim();
            arg = arg.replaceAll("([0-9.]+)[ ]+", "$1,");
            arg = arg.replace("\n", "");
            arg = arg.replaceAll("[ ]+", " ");
            arg = "{" + arg.replace(" | ", ":") + "}";
        }
        if (arg.contains("\t") || arg.contains("    ")) {
            String[] split = arg.split("[\t]");
            if (split.length == 1) split = arg.split("[ ]{4}");
            boolean production = (split.length == values.length);
            if (production || split.length == values.length - 1) {
                ArrayList<ResourceType> types = new ArrayList<>(Arrays.asList(values));
                if (!production) types.remove(PRODUCTION);
                Map<ResourceType, Double> result = new LinkedHashMap<>();
                for (int i = 0; i < types.size(); i++) {
                    result.put(types.get(i), MathMan.parseDouble(split[i].trim()));
                }
                return result;
            }
        }
        arg = arg.trim();
        String original = arg;
        if (!arg.contains(":") && !arg.contains("=")) {
            arg = arg.replaceAll("([-0-9])[ ]([a-zA-Z])", "$1:$2");
            arg = arg.replaceAll("([a-zA-Z])[ ]([-0-9])", "$1:$2");
        }
        arg = arg.replace('=', ':').replaceAll("([0-9]),([0-9])", "$1$2").toUpperCase();
        arg = arg.replaceAll("([0-9.]+):([a-zA-Z]{3,})", "$2:$1");
        arg = arg.replace(" ", "");
        if (arg.startsWith("$") || arg.startsWith("-$")) {
            if (!arg.contains(",")) {
                int sign = 1;
                if (arg.startsWith("-")) {
                    sign = -1;
                    arg = arg.substring(1);
                }
                Map<ResourceType, Double> result = new LinkedHashMap<>();
                result.put(CASH, MathMan.parseDouble(arg) * sign);
                return result;
            }
            arg = arg.replace("$", CASH +":");
        }

        arg = arg.replace("MONEY:", "CASH:");
        arg = arg.replace("TECH:", "TECHNOLOGY:");
        arg = arg.replace("PROD:", "PRODUCTION:");
        arg = arg.replace("MINERAL:", "MINERALS:");
        arg = arg.replace("MIN:", "MINERALS:");
        arg = arg.replace("URA:", "URANIUM:");
        arg = arg.replace("RE:", "RARE_METALS:");
        arg = arg.replace("METALS:", "RARE_METALS:");
        arg = arg.replace("RARE:", "RARE_METALS:");
        arg = arg.replace("GAS:", "FUEL:");
        arg = arg.replace("PS:", "POLITICAL_SUPPORT:");
        arg = arg.replace("POLITICAL:", "POLITICAL_SUPPORT:");
        arg = arg.replace("SUPPORT:", "POLITICAL_SUPPORT:");

        if (!arg.contains("{") && !arg.contains("}")) {
            arg = "{" + arg + "}";
        }
        if (arg.startsWith("-{")) {
            arg = "{}" + arg;
        }
        arg = arg.replace("(-{", "({}-{");

        Map<ResourceType, Double> result;
        try {
            Function<String, Map<ResourceType, Double>> parse = f -> {
                if (f.contains("TRANSACTION_COUNT")) {
                    f = f.replaceAll("\"TRANSACTION_COUNT\":[0-9]+,", "");
                    f = f.replaceAll(",\"TRANSACTION_COUNT\":[0-9]+", "");
                }
                return RESOURCE_GSON.fromJson(f, RESOURCE_TYPE);
            };
            if (allowBodmas) {
                List<ArrayUtil.DoubleArray> resources = (ArrayUtil.calculate(arg, arg1 -> {
                    if (!arg1.contains("{")) {
                        return new ArrayUtil.DoubleArray(PrimitiveBindings.Double(arg1));
                    }
                    Map<ResourceType, Double> map = parse.apply(arg1);
                    double[] arr = resourcesToArray(map);
                    return new ArrayUtil.DoubleArray(arr);
                }));
                result = resourcesToMap(resources.get(0).toArray());
            } else {
                result = parse.apply(arg);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (original.toUpperCase(Locale.ROOT).matches("[0-9]+[ASMGBILUOCF$]([ ][0-9]+[ASMGBILUOCF$])*")) {
                String[] split = original.split(" ");
                result = new LinkedHashMap<>();
                for (String s : split) {
                    Character typeChar = s.charAt(s.length() - 1);
                    ResourceType type1 = parseChar(typeChar);
                    double amount = MathMan.parseDouble(s.substring(0, s.length() - 1));
                    result.put(type1, amount);
                }
            } else {
                return handleResourceError(arg, e);
            }
        }
        if (result.containsKey(null)) {
            return handleResourceError(arg, null);
        }
        return result;
    }

    private static Map<ResourceType, Double> handleResourceError(String arg, Exception e) {
        StringBuilder response = new StringBuilder("Invalid resource amounts: `" + arg + "`\n");
        if (e != null) {
            String msg = e.getMessage();
            if (msg.startsWith("No enum constant")) {
                String rssInput = msg.substring(msg.lastIndexOf(".") + 1);
                List<ResourceType> closest = StringMan.getClosest(rssInput, valuesList, false);

                response.append("You entered `" + rssInput + "` which is not a valid resource.");
                if (closest.size() > 0) {
                    response.append(" Did you mean: `").append(closest.get(0)).append("`");
                }
                response.append("\n");
            } else {
                response.append("Error: `").append(e.getMessage()).append("`\n");
            }
        }
        response.append("Valid resources are: `").append(StringMan.join(values, ", ")).append("`").append("\n");
        response.append("""
                You can enter a single resource like this:
                `minerals=10`
                `$15`
                Use commas for multiple resources:
                `fuel=10,crypto=20,cash=30`
                Use k,m,b,t for thousands, millions, billions, trillions:
                `fuel=10k,crypto=20m,cash=30b`
                Use curly braces for operations:
                `{fuel=3*(2+1),cash=-3}*{fuel=112,cash=2513}*1.5+{cash=1k}^0.5`""");
        throw new IllegalArgumentException(response.toString());
    }

    public static String resourcesToFancyString(double[] resources) {
        return resourcesToFancyString(resourcesToMap(resources));
    }

    public static String resourcesToFancyString(double[] resources, String totalName) {
        return resourcesToFancyString(resourcesToMap(resources), totalName);
    }

    public static String resourcesToFancyString(Map<ResourceType, Double> resources) {
        return resourcesToFancyString(resources, null);
    }

    public static String resourcesToFancyString(Map<ResourceType, Double> resources, String totalName) {
        StringBuilder out = new StringBuilder();
        String leftAlignFormat = "%-10s | %-17s\n";
        out.append("```");
        out.append("Resource   | Amount   \n");
        out.append("-----------+-----------------+\n");
        for (ResourceType type : values) {
            Double amt = resources.get(type);
            if (amt != null) out.append(String.format(leftAlignFormat, type.name(), MathMan.format(amt)));
        }
        out.append("```\n");
        out.append("**Total" + (totalName != null && !totalName.isEmpty() ? " " + totalName : "") + "**: worth ~$" + MathMan.format(convertedTotal(resources)) + "\n```" + resourcesToString(resources) + "``` ");
        return out.toString();
    }

    public static <T extends Number> Map<ResourceType, T> addResourcesToA(Map<ResourceType, T> a, Map<ResourceType, T> b) {
        if (b.isEmpty()) {
            return a;
        }
        for (ResourceType type : values) {
            Number v1 = a.get(type);
            Number v2 = b.get(type);
            Number total = v1 == null ? v2 : (v2 == null ? v1 : MathMan.add(v1, v2));
            if (total != null && total.doubleValue() != 0) {
                a.put(type, (T) total);
            } else {
                a.remove(type);
            }
        }
        return a;
    }

    public static <T extends Number> Map<ResourceType, T> negate(Map<ResourceType, T> b) {
        return subResourcesToA(new LinkedHashMap<>(), b);
    }

    public static <T extends Number> Map<ResourceType, T> subResourcesToA(Map<ResourceType, T> a, Map<ResourceType, T> b) {
        for (ResourceType type : values) {
            Number v1 = a.get(type);
            Number v2 = b.get(type);
            if (v2 == null) continue;
            Number total = MathMan.subtract(v1, v2);
            if (total != null && total.doubleValue() != 0) {
                a.put(type, (T) total);
            }
        }
        return a;
    }

    public static <K, T extends Number> Map<K, T> add(Map<K, T> a, Map<K, T> b) {
        if (a.isEmpty()) {
            return b;
        } else if (b.isEmpty()) {
            return a;
        }
        LinkedHashMap<K, T> copy = new LinkedHashMap<>();
        Set<K> keys = new HashSet<>(a.keySet());
        keys.addAll(b.keySet());
        for (K type : keys) {
            Number v1 = a.get(type);
            Number v2 = b.get(type);
            Number total = v1 == null ? v2 : (v2 == null ? v1 : MathMan.add(v1, v2));
            if (total != null && total.doubleValue() != 0) {
                copy.put(type, (T) total);
            }
        }
        return copy;
    }

    public static Map<ResourceType, Double> resourcesToMap(double[] resources) {
        Map<ResourceType, Double> map = new LinkedHashMap<>();
        for (ResourceType type : values) {
            double value = resources[type.ordinal()];
            if (value != 0) {
                map.put(type, value);
            }
        }
        return map;
    }

    public static double[] resourcesToArray(Map<ResourceType, Double> resources) {
        double[] result = new double[values.length];
        for (Map.Entry<ResourceType, Double> entry : resources.entrySet()) {
            result[entry.getKey().ordinal()] += entry.getValue();
        }
        return result;
    }

    public static String resourcesToString(double[] values) {
        return resourcesToString(resourcesToMap(values));
    }

    public static String resourcesToString(Map<ResourceType, ? extends Number> resources) {
        Map<ResourceType, String> newMap = new LinkedHashMap<>();
        for (ResourceType resourceType : values()) {
            if (resources.containsKey(resourceType)) {
                Number value = resources.get(resourceType);
                if (value.doubleValue() == 0) continue;
                if (value.doubleValue() == value.longValue()) {
                    newMap.put(resourceType, MathMan.format(value.longValue()));
                } else {
                    newMap.put(resourceType, MathMan.format(value.doubleValue()));
                }
            }
        }
        return StringMan.getString(newMap);
    }

    public static double convertedTotal(double[] resources, boolean max) {
        if (max) return convertedTotal(resources);
        double total = 0;
        for (int i = 0; i < resources.length; i++) {
            double amt = resources[i];
            if (amt != 0) {
                total += -convertedTotal(values[i], -amt);
            }
        }
        return total;
    }

    public static double convertedTotal(double[] resources) {
        double total = 0;
        for (int i = 0; i < resources.length; i++) {
            double amt = resources[i];
            if (amt != 0) {
                total += convertedTotal(values[i], amt);
            }
        }
        return total;
    }

    public static double convertedTotal(Map<ResourceType, ? extends Number> resources) {
        double total = 0;
        for (Map.Entry<ResourceType, ? extends Number> entry : resources.entrySet()) {
            total += convertedTotal(entry.getKey(), entry.getValue().doubleValue());
        }
        return total;
    }

    public static double convertedTotalPositive(ResourceType type, double amt) {
        return (type == CASH ? 1 : 10_000) * amt;
    }

    public static double convertedTotalNegative(ResourceType type, double amt) {
        return (type == CASH ? 1 : 10_000) * amt;
    }

    public static double convertedTotal(ResourceType type, double amt) {
        return (type == CASH ? 1 : 10_000) * amt;
    }

    public static double[] max(double[] rss1, double[] rss2) {
        for (int i = 0; i < rss1.length; i++) {
            rss1[i] = Math.max(rss1[i], rss2[i]);
        }
        return rss1;
    }

    public static double[] min(double[] rss1, double[] rss2) {
        for (int i = 0; i < rss1.length; i++) {
            rss1[i] = Math.min(rss1[i], rss2[i]);
        }
        return rss1;
    }

    public String getShorthand() {
        return switch (this) {
            case CASH -> "$";
            case TECHNOLOGY -> "TECH";
            case PRODUCTION -> "PROD";
            case MINERALS -> "MIN";
            case URANIUM -> "URA";
            case RARE_METALS -> "RE";
            case FUEL -> "GAS";
            case POLITICAL_SUPPORT -> "POL";
            case CRYPTO -> "BTC";
        };
    }

    public static final ResourceType parse(String input) {
        String upper = input.toUpperCase().replace(" ", "_");
        switch (upper) {
            // CASH
            case "$":
            case "CA":
            case "CAS":
            case "MONEY":
                return CASH;
            // TECHNOLOGY,
            case "T":
            case "TE":
            case "TEC":
            case "TECH":
            case "TECHN":
            case "TECHNO":
            case "TECHNOL":
            case "TECHNOLO":
            case "TECHNOLOG":
                return TECHNOLOGY;
            // PRODUCTION,
            case "PR":
            case "PRO":
            case "PROD":
            case "PRODU":
            case "PRODUC":
            case "PRODUCT":
            case "PRODUCTI":
            case "PRODUCTIO":
                return PRODUCTION;
            // MINERALS,
            case "M":
            case "MI":
            case "MIN":
            case "MINE":
            case "MINER":
            case "MINERA":
            case "MINERAL":
                return MINERALS;
            // URANIUM,
            case "U":
            case "UR":
            case "URA":
            case "URAN":
            case "URANI":
            case "URANIU":
                return URANIUM;
            // RARE_METALS,
            case "R":
            case "RA":
            case "RAR":
            case "RARE":
            case "RARE_":
            case "RARE_M":
            case "RARE_ME":
            case "RARE_MET":
            case "RARE_META":
            case "RARE_METAL":
                return RARE_METALS;
            // FUEL,
            case "F":
            case "FU":
            case "FUE":
                return FUEL;
            // POLITICAL_SUPPORT,
            case "P":
            case "PO":
            case "POL":
            case "POLI":
            case "POLIT":
            case "POLITI":
            case "POLITIC":
            case "POLITICA":
            case "POLITICAL":
            case "POLITICAL_S":
            case "POLITICAL_SU":
            case "POLITICAL_SUP":
            case "POLITICAL_SUPP":
            case "POLITICAL_SUPPO":
            case "POLITICAL_SUPPOR":
            case "SUPPORT":
            case "PS":
                return POLITICAL_SUPPORT;
            // CRYPTO,
            case "CR":
            case "CRY":
            case "CRYPT":
            case "BTC":
                return CRYPTO;
        }
        return valueOf(upper);
    }

    public static boolean isZero(double[] resources) {
        for (double i : resources) {
            if (i != 0 && (Math.abs(i) >= 0.005)) return false;
        }
        return true;
    }

    public static double[] floor(double[] resources, double min) {
        for (int i = 0; i < resources.length; i++) {
            if (resources[i] < min) resources[i] = min;
        }
        return resources;
    }

    public static double[] ceil(double[] resources, double max) {
        for (int i = 0; i < resources.length; i++) {
            if (resources[i] > max) resources[i] = max;
        }
        return resources;
    }

    public static double[] set(double[] resources, double[] values) {
        for (int i = 0; i < values.length; i++) {
            resources[i] = values[i];
        }
        return resources;
    }

    public static double[] subtract(double[] resources, double[] values) {
        for (int i = 0; i < values.length; i++) {
            double amt = values[i];
            double curr = resources[i];
            resources[i] = (Math.round(curr * 100) - Math.round(amt * 100)) * 0.01;
        }
        return resources;
    }
    public static double[] add(double[] resources, double[] values) {
        for (int i = 0; i < values.length; i++) {
            double amt = values[i];
            double curr = resources[i];
            resources[i] = Math.round(amt * 100) * 0.01 + Math.round(curr * 100) * 0.01;
        }
        return resources;
    }
    public static double[] add(Collection<double[]> values) {
        double[] result = getBuffer();
        for (double[] value : values) {
            add(result, value);
        }
        return result;
    }


    public static double[] round(double[] resources) {
        for (int i = 0; i < resources.length; i++) {
            double amt = resources[i];
            if (amt != 0) {
                resources[i] = Math.round(amt * 100) * 0.01;
            }
        }
        return resources;
    }

    public static double[] negative(double[] resources) {
        for (int i = 0; i < resources.length; i++) {
            resources[i] = -resources[i];
        }
        return resources;
    }

    public static boolean equals(Map<ResourceType, Double> amtA, Map<ResourceType, Double> amtB) {
        for (ResourceType type : ResourceType.values) {
            if (Math.round(100 * (amtA.getOrDefault(type, 0d) - amtB.getOrDefault(type, 0d))) != 0) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(double[] rss1, double[] rss2) {
        for (ResourceType type : ResourceType.values) {
            if (Math.round(100 * (rss1[type.ordinal()] - rss2[type.ordinal()])) != 0) {
                return false;
            }
        }
        return true;
    }

    public static String toString(double[] resources, boolean fancy) {
        return fancy ? resourcesToFancyString(resources) : toString(resources);
    }

    public static String toString(double[] resources) {
        return resourcesToString(resources);
    }

    public double[] toArray(double amt) {
        double[] result = getBuffer();
        result[ordinal()] = amt;
        return result;
    }

    public static double getEquilibrium(double[] total) {
        double consumeCost = 0;
        double taxable = 0;
        for (ResourceType type : values) {
            double amt = total[type.ordinal()];
            if (amt < 0) {
                consumeCost += Math.abs(convertedTotal(type, -amt));
            } else if (amt > 0){
                taxable += Math.abs(convertedTotal(type, amt));
            }
        }
        if (taxable > consumeCost) {
            double requiredTax = consumeCost / taxable;
            return requiredTax;
        }
        return -1;
    }

    public static double[] read(ByteBuffer buf, double[] output) {
        if (output == null) output = getBuffer();
        for (int i = 0; i < output.length; i++) {
            if (!buf.hasRemaining()) break;
            output[i] += buf.getDouble();
        }
        return output;
    }

    public static double[] getBuffer() {
        return new double[ResourceType.values.length];
    }

    public static ResourcesBuilder builder() {
        return new ResourcesBuilder();
    }

    public static ResourcesBuilder builder(double[] amount) {
        return builder().add(amount);
    }

    public static ResourcesBuilder builder(Map<ResourceType, Double> amount) {
        return builder().add(amount);
    }

    public ResourcesBuilder builder(double amt) {
        return builder().add(this, amt);
    }

    public static class ResourcesBuilder {
        private double[] resources = null;

        public String toString() {
            return resourcesToString(build());
        }

        public double convertedTotal() {
            return ResourceType.convertedTotal(build());
        }

        public String convertedStr() {
            return "~$" + MathMan.format(convertedTotal());
        }

        private double[] getResources() {
            if (resources == null) resources = getBuffer();
            return resources;
        }


        public <T> ResourcesBuilder forEach(Iterable<T> iterable, BiConsumer<ResourcesBuilder, T> consumer) {
            for (T t : iterable) {
                consumer.accept(this, t);
            }
            return this;
        }
        public ResourcesBuilder add(ResourceType type, double amt) {
            if (amt != 0) {
                getResources()[type.ordinal()] += amt;
            }
            return this;
        }

        public ResourcesBuilder add(double[] amt) {
            for (ResourceType type : ResourceType.values) {
                add(type, amt[type.ordinal()]);
            }
            return this;
        }

        public ResourcesBuilder add(Map<ResourceType, Double> amt) {
            for (Map.Entry<ResourceType, Double> entry : amt.entrySet()) {
                add(entry.getKey(), entry.getValue());
            }
            return this;
        }

        public ResourcesBuilder addMoney(double amt) {
            return add(ResourceType.CASH, amt);
        }

        public boolean isEmpty() {
            return resources == null || ResourceType.isZero(resources);
        }

        public double[] build() {
            return getResources();
        }

        public ResourcesBuilder subtract(Map<ResourceType, Double> amt) {
            for (Map.Entry<ResourceType, Double> entry : amt.entrySet()) {
                add(entry.getKey(), -entry.getValue());
            }
            return this;
        }

        public ResourcesBuilder subtract(double[] resources) {
            for (int i = 0; i < resources.length; i++) {
                add(ResourceType.values[i], -resources[i]);
            }
            return this;
        }

        public Map<ResourceType, Double> buildMap() {
            return resourcesToMap(build());
        }

        public ResourcesBuilder max(double[] amounts) {
            for (int i = 0; i < amounts.length; i++) {
                this.resources[i] = Math.max(this.resources[i], amounts[i]);
            }
            return this;
        }

        public ResourcesBuilder maxZero() {
            for (int i = 0; i < resources.length; i++) {
                this.resources[i] = Math.max(this.resources[i], 0);
            }
            return this;
        }

        public ResourcesBuilder min(double[] amounts) {
            for (int i = 0; i < amounts.length; i++) {
                this.resources[i] = Math.min(this.resources[i], amounts[i]);
            }
            return this;
        }

        public ResourcesBuilder negative() {
            for (int i = 0; i < resources.length; i++) {
                this.resources[i] = -this.resources[i];
            }
            return this;
        }
    }

    @Command(desc = "The market value of this resource (weekly average)")
    public double getMarketValue() {
        return convertedTotal(this, 1);
    }

    public static interface IResourceArray {
        public double[] get();

        public static IResourceArray create(double[] resources) {
            int numResources = 0;
            boolean noProduction = resources[PRODUCTION.ordinal()] == 0;
            ResourceType latestType = null;
            for (ResourceType type : ResourceType.values) {
                double amt = resources[type.ordinal()];
                if (amt > 0) {
                    numResources++;
                    latestType = type;
                }
            }
            switch (numResources) {
                case 0:
                    return new EmptyResources();
                case 1:
                    return new ResourceAmtCents(latestType, resources[latestType.ordinal()]);
                case 2, 3, 4, 5, 6, 7, 8, 9, 10:
                    return new VarArray(resources);
                default:
                    if (noProduction) {
                        return new ArrayNoProduction(resources);
                    } else {
                        return new VarArray(resources);
                    }
            }
        }
    }

    public static class EmptyResources implements IResourceArray{

        @Override
        public double[] get() {
            return ResourceType.getBuffer();
        }
    }

    public static class ArrayNoProduction implements IResourceArray {
        private final byte[] data;

        public ArrayNoProduction(double[] loot) {
            FastByteArrayOutputStream baos = new FastByteArrayOutputStream();
            for (ResourceType type : ResourceType.values) {
                if (type == ResourceType.PRODUCTION) continue;
                try {
                    IOUtil.writeVarLong(baos, (long) (loot[type.ordinal()] * 100));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            baos.trim();
            this.data = baos.array;
        }
        public double[] get() {
            double[] data = ResourceType.getBuffer();
            FastByteArrayInputStream in = new FastByteArrayInputStream(this.data);
            for (ResourceType type : ResourceType.values) {
                if (type == ResourceType.PRODUCTION) continue;
                try {
                    data[type.ordinal()] = IOUtil.readVarLong(in) / 100d;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return data;
        }
    }

    public static class VarArray implements IResourceArray {
        private final byte[] data;

        public VarArray(double[] loot) {
            FastByteArrayOutputStream baos = new FastByteArrayOutputStream();
            for (ResourceType type : ResourceType.values) {
                long amtCents = (long) (loot[type.ordinal()] * 100);
                if (amtCents == 0) continue;
                try {
                    long pair = type.ordinal() + (amtCents << 4);
                    IOUtil.writeVarLong(baos, pair);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            baos.trim();
            this.data = baos.array;
        }

        public double[] get() {
            double[] data = ResourceType.getBuffer();
            ByteArrayInputStream in = new ByteArrayInputStream(this.data);
            while (in.available() > 0) {
                try {
                    long pair = IOUtil.readVarLong(in);
                    int type = (int) (pair & 0xF);
                    long amtCents = pair >> 4;
                    data[type] = amtCents / 100d;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return data;
        }
    }

    public static class ResourceAmtCents implements IResourceArray {
        private final long data;

        public ResourceAmtCents(ResourceType type, double amount) {
            this.data = type.ordinal() + ((int)(amount * 100d) << 4);
        }

        public ResourceType getType() {
            return ResourceType.values[(int) (data & 0xF)];
        }
        public long getAmountCents() {
            return data >> 4;
        }

        @Override
        public double[] get() {
            return getType().toArray(getAmountCents() / 100d);
        }
    }
}

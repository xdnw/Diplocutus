package link.locutus.discord.util.task.ia;

import link.locutus.discord.Locutus;
import link.locutus.discord.Logg;
import link.locutus.discord.commands.manager.v2.binding.annotation.Command;
import link.locutus.discord.commands.manager.v2.command.IMessageIO;
import link.locutus.discord.commands.manager.v2.impl.pw.refs.CM;
import link.locutus.discord.commands.manager.v2.impl.pw.NationFilter;
import link.locutus.discord.config.Messages;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.*;
import link.locutus.discord.db.guild.GuildKey;
import link.locutus.discord.pnw.AllianceList;
import link.locutus.discord.util.MathMan;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.StringMan;
import link.locutus.discord.util.TimeUtil;
import link.locutus.discord.util.discord.DiscordUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class IACheckup {
    private final GuildDB db;
    private final boolean useCache;
    private final AllianceList alliance;

    public IACheckup(GuildDB db, AllianceList alliance, boolean useCache) throws IOException {
        if (db == null) throw new IllegalStateException("No database found");
        if (alliance == null || alliance.isEmpty()) throw new IllegalStateException("No alliance found");
        this.db = db;
        this.alliance = alliance;
        this.useCache = useCache;
    }

    public AllianceList getAlliance() {
        return alliance;
    }

    public static Map<IACheckup.AuditType, Map.Entry<Object, String>> simplify(Map<IACheckup.AuditType, Map.Entry<Object, String>> auditFinal) {
        Map<AuditType, Map.Entry<Object, String>> audit = new LinkedHashMap<>(auditFinal);
        audit.entrySet().removeIf(f -> {
            Map.Entry<Object, String> value = f.getValue();
            return value == null || value.getValue() == null;
        });

        Map<IACheckup.AuditType, Map.Entry<Object, String>> tmp = new LinkedHashMap<>(audit);
        tmp.entrySet().removeIf(f -> {
            IACheckup.AuditType required = f.getKey().required;
            while (required != null) {
                if (audit.containsKey(required)) return true;
                required = required.required;
            }
            return false;
        });
        return tmp;
    }

    public static void createEmbed(IMessageIO channel, String command, DBNation nation, Map<IACheckup.AuditType, Map.Entry<Object, String>> auditFinal, Integer page) {
        Map<AuditType, Map.Entry<Object, String>> audit = simplify(auditFinal);
        int failed = audit.size();

        boolean newPage = page == null;
        if (page == null) page = 0;
        String title = (page + 1) + "/" + failed + " tips for " + nation.getNation();

        List<String> pages = new ArrayList<>();
        for (Map.Entry<IACheckup.AuditType, Map.Entry<Object, String>> entry : audit.entrySet()) {
            IACheckup.AuditType type = entry.getKey();
            Map.Entry<Object, String> info = entry.getValue();
            if (info == null || info.getValue() == null) continue;

            StringBuilder body = new StringBuilder();

            body.append("**" + type.name() + "**: ").append(type.emoji).append(" ");
            body.append(info.getValue());
            pages.add(body.toString());
        }
        DiscordUtil.paginate(channel, title, command, page, 1, pages, "", true);
    }

    public Map<DBNation, Map<AuditType, Map.Entry<Object, String>>> checkup(Consumer<DBNation> onEach, boolean fast) throws InterruptedException, ExecutionException, IOException {
        List<DBNation> nations = new ArrayList<>(alliance.getNations(true, 0, true));
        return checkup(nations, onEach, fast);
    }

    public Map<DBNation, Map<AuditType, Map.Entry<Object, String>>> checkup(Collection<DBNation> nations, Consumer<DBNation> onEach, boolean fast) throws InterruptedException, ExecutionException, IOException {
        return checkup(nations, onEach, AuditType.values(), fast);
    }

    public Map<DBNation, Map<AuditType, Map.Entry<Object, String>>> checkup(Collection<DBNation> nations, Consumer<DBNation> onEach, AuditType[] auditTypes, boolean fast) throws InterruptedException, ExecutionException, IOException {
        Map<DBNation, Map<AuditType, Map.Entry<Object, String>>> result = new LinkedHashMap<>();
        for (DBNation nation : nations) {
            if (nation.isVacation() || nation.active_m() > 10000) continue;

            if (onEach != null) onEach.accept(nation);

            Map<AuditType, Map.Entry<Object, String>> nationMap = checkup(nation, auditTypes, fast, fast);
            result.put(nation, nationMap);
        }
        return result;
    }

    public Map<AuditType, Map.Entry<Object, String>> checkup(DBNation nation) throws InterruptedException, ExecutionException, IOException {
        return checkup(nation, AuditType.values());
    }

    public Map<AuditType, Map.Entry<Object, String>> checkupSafe(DBNation nation, boolean individual, boolean fast) {
        try {
            Map<AuditType, Map.Entry<Object, String>> result = checkup(nation, individual, fast);
            return result;
        } catch (InterruptedException | ExecutionException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Map<AuditType, Map.Entry<Object, String>> checkup(DBNation nation, boolean individual, boolean fast) throws InterruptedException, ExecutionException, IOException {
        return checkup(nation, AuditType.values(), individual, fast);
    }

    public Map<AuditType, Map.Entry<Object, String>> checkup(DBNation nation, AuditType[] audits) throws InterruptedException, ExecutionException, IOException {
        return checkup(nation, audits, true, false);
    }

    public Map<AuditType, Map.Entry<Object, String>> checkup(DBNation nation, AuditType[] audits, boolean individual, boolean fast) throws InterruptedException, ExecutionException, IOException {
        Map<AuditType, Map.Entry<Object, String>> results = new LinkedHashMap<>();
        for (AuditType type : audits) {
            long start = System.currentTimeMillis();
            audit(type, nation, results, individual, fast);
            long diff = System.currentTimeMillis() - start;
            if (diff > 10) {
                Logg.text("Audit " + type + " took " + diff + " ms");
            }
        }

        long bitMask = 0;
        for (AuditType audit : audits) {
            Map.Entry<Object, String> result = results.get(audit);
            boolean passed = result == null || result.getValue() == null;
            bitMask |= (passed ? 1 : 0) << audit.ordinal();
        }

//        ByteBuffer lastCheckupBuffer = nation.getMeta(NationMeta.CHECKUPS_PASSED);
//        if (lastCheckupBuffer != null) {
//            long lastCheckup = lastCheckupBuffer.getLong();
//        }

//        nation.setMeta(NationMeta.CHECKUPS_PASSED, bitMask);

        results.entrySet().removeIf(f -> f.getValue() == null || f.getValue().getValue() == null);

        results.keySet().stream().filter(f -> f.severity == AuditSeverity.DANGER).count();

        return results;
    }

    private void audit(AuditType type, DBNation nation, Map<AuditType, Map.Entry<Object, String>> results, boolean individual, boolean fast) throws InterruptedException, ExecutionException, IOException {
        if (results.containsKey(type)) {
            return;
        }
        if (type.required != null) {
            if (!results.containsKey(type.required)) {
                audit(type.required, nation, results, individual, fast);
            }
            Map.Entry<Object, String> requiredResult = results.get(type.required);
            if (requiredResult != null) {
                results.put(type, null);
                return;
            }
        }
        Map.Entry<Object, String> value = checkup(type, nation, individual, fast);
        results.put(type, value);
    }

    public enum AuditSeverity {
        INFO,
        WARNING,
        DANGER,
    }

    public enum AuditType {
        INACTIVE("\uD83D\uDCA4", AuditSeverity.DANGER),
        ;

        public final AuditType required;
        public final String emoji;
        public final AuditSeverity severity;

        AuditType(String emoji) {
            this(null, emoji);
        }

        AuditType(String emoji, AuditSeverity severity) {
            this(null, emoji, severity);
        }

        AuditType(AuditType required, String emoji) {
            this(required, emoji, AuditSeverity.INFO);
        }

        AuditType(AuditType required, String emoji, AuditSeverity severity) {
            this.required = required;
            this.emoji = emoji;
            this.severity = severity;
        }

        @Command(desc = "Audit severity")
        public AuditSeverity getSeverity() {
            return severity;
        }

        @Command(desc = "Audit emoji")
        public String getEmoji() {
            return emoji;
        }

        @Command(desc = "The required audit, or null")
        public AuditType getRequired() {
            return required;
        }

        @Command(desc = "Name of the audit")
        public String getName() {
            return name();
        }
    }

    private Map.Entry<Object, String> checkup(AuditType type, DBNation nation, boolean individual, boolean fast) throws InterruptedException, ExecutionException, IOException {
        boolean updateNation = individual && !fast;
        switch (type) {
            case INACTIVE:
                return testIfCacheFails(() -> checkInactive(nation), updateNation);
        }
        throw new IllegalArgumentException("Unsupported: " + type);
    }

    private Map.Entry<Object, String> testIfCacheFails(Supplier<Map.Entry<Object, String>> supplier, boolean test) {
        return supplier.get();
    }

    private Map.Entry<Object, String> checkInactive(DBNation nation) {
        long daysInactive = TimeUnit.MINUTES.toDays(nation.active_m());
        if (daysInactive > 1) {
            String message = "Hasn't logged in for " + daysInactive + " days.";
            return new AbstractMap.SimpleEntry<>(daysInactive, message);
        }
        return null;
    }
}

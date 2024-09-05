package link.locutus.discord.db.entities;

import link.locutus.discord.Locutus;
import link.locutus.discord.api.generated.WarType;
import link.locutus.discord.pnw.NationOrAlliance;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.StringMan;
import link.locutus.discord.util.TimeUtil;
import link.locutus.discord.util.discord.DiscordUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class WarParser {
    private final String nameA;
    private final String nameB;
    private final Function<DBWar, Boolean> isPrimary, isSecondary;
    private final long start;
    private final long end;
    private final Collection<Integer> coal1Alliances;
    private final Collection<Integer> coal1Nations;
    private final Collection<Integer> coal2Alliances;
    private final Collection<Integer> coal2Nations;

    private Map<Integer, DBWar> wars;

    public static WarParser ofAANatobj(Collection<DBAlliance> coal1Alliances, Collection<DBNation> coal1Nations, Collection<DBAlliance> coal2Alliances, Collection<DBNation> coal2Nations, long start, long end) {
        return ofNatObj(coal1Alliances == null ? null : coal1Alliances.stream().map(DBAlliance::getAlliance_id).collect(Collectors.toSet()), coal1Nations, coal2Alliances == null ? null : coal2Alliances.stream().map(DBAlliance::getAlliance_id).collect(Collectors.toSet()), coal2Nations, start, end);
    }

    public static WarParser ofNatObj(Collection<Integer> coal1Alliances, Collection<DBNation> coal1Nations, Collection<Integer> coal2Alliances, Collection<DBNation> coal2Nations, long start, long end) {
        return of(coal1Alliances, coal1Nations == null ? null : coal1Nations.stream().map(DBNation::getNation_id).collect(Collectors.toSet()), coal2Alliances, coal2Nations == null ? null : coal2Nations.stream().map(DBNation::getNation_id).collect(Collectors.toSet()), start, end);
    }

    public static WarParser of(Collection<NationOrAlliance> coal1, Collection<NationOrAlliance> coal2, long start, long end) {
        Collection<Integer> coal1Alliances = coal1 == null ? null : coal1.stream().filter(NationOrAlliance::isAlliance).map(NationOrAlliance::getId).collect(Collectors.toSet());
        Collection<Integer> coal1Nations = coal1 == null ? null : coal1.stream().filter(NationOrAlliance::isNation).map(NationOrAlliance::getId).collect(Collectors.toSet());
        Collection<Integer> coal2Alliances = coal2 == null ? null : coal2.stream().filter(NationOrAlliance::isAlliance).map(NationOrAlliance::getId).collect(Collectors.toSet());
        Collection<Integer> coal2Nations = coal2 == null ? null : coal2.stream().filter(NationOrAlliance::isNation).map(NationOrAlliance::getId).collect(Collectors.toSet());
        return of(coal1Alliances, coal1Nations, coal2Alliances, coal2Nations,  start, end);
    }

    public static WarParser of(Collection<Integer> coal1Alliances, Collection<Integer> coal1Nations, Collection<Integer> coal2Alliances, Collection<Integer> coal2Nations, long start, long end) {
        return new WarParser(coal1Alliances, coal1Nations, coal2Alliances, coal2Nations, start, end);
    }

    private WarParser(Collection<DBWar> wars, Function<DBWar, Boolean> isPrimary) {
        coal1Alliances = Collections.emptySet();
        coal2Alliances = Collections.emptySet();
        coal1Nations = Collections.emptySet();
        coal2Nations = Collections.emptySet();
        this.start = 0;
        this.end = Long.MAX_VALUE;
        nameA = "A";
        nameB = "B";
        this.isPrimary = isPrimary;
        isSecondary = f -> !isPrimary.apply(f);
        this.wars = new HashMap<>(wars.size());
        for (DBWar war : wars) {
            this.wars.put(war.getWarId(), war);
        }
    }

    public static WarParser of(Collection<DBWar> wars, Function<DBWar, Boolean> isPrimary) {
        return new WarParser(wars, isPrimary);
    }

    private WarParser(Collection<Integer> coal1Alliances, Collection<Integer> coal1Nations, Collection<Integer> coal2Alliances, Collection<Integer> coal2Nations, long start, long end) {
        if (coal1Alliances == null && coal1Nations == null && coal2Alliances == null && coal2Nations == null) {
            throw new IllegalArgumentException("At least one coalition must be non-null");
        }
        if (coal1Alliances == null) coal1Alliances = new HashSet<>();
        if (coal1Nations == null) coal1Nations = new HashSet<>();
        if (coal2Alliances == null) coal2Alliances = new HashSet<>();
        if (coal2Nations == null) coal2Nations = new HashSet<>();
        this.coal1Alliances = coal1Alliances;
        this.coal1Nations = coal1Nations;
        this.coal2Alliances = coal2Alliances;
        this.coal2Nations = coal2Nations;
        List<String> coal1Names = new ArrayList<>();
        List<String> coal2Names = new ArrayList<>();
        for (Integer id : coal1Alliances) coal1Names.add("AA:" + DNS.getName(id, true));
        for (Integer id : coal1Nations) coal1Names.add(DNS.getName(id, false));
        for (Integer id : coal2Alliances) coal2Names.add("AA:" + DNS.getName(id, true));
        for (Integer id : coal2Nations) coal2Names.add(DNS.getName(id, false));
        this.nameA = coal1Names.isEmpty() ? "*" : coal1Names.size() > 10 ? "col1" : StringMan.join(coal1Names, ",");
        this.nameB = coal2Names.isEmpty() ? "*" : coal2Names.size() > 10 ? "col1" : StringMan.join(coal2Names, ",");

        Predicate<DBWar> isCol1Attacker;
        Predicate<DBWar> isCol2Attacker;
        Predicate<DBWar> isCol1Defender;
        Predicate<DBWar> isCol2Defender;
        if (coal1Alliances.isEmpty() && coal1Nations.isEmpty()) {
            isCol1Attacker = f -> !this.coal2Alliances.contains(f.getAttacker_aa()) && !this.coal2Nations.contains(f.getAttacker_id());
            isCol1Defender = f -> !this.coal2Alliances.contains(f.getDefender_aa()) && !this.coal2Nations.contains(f.getDefender_id());
        } else {
            isCol1Attacker = f -> this.coal1Alliances.contains(f.getAttacker_aa()) || this.coal1Nations.contains(f.getAttacker_id());
            isCol1Defender = f -> this.coal1Alliances.contains(f.getDefender_aa()) || this.coal1Nations.contains(f.getDefender_id());
        }
        if (coal2Alliances.isEmpty() && coal2Nations.isEmpty()) {
            isCol2Attacker = f -> !this.coal1Alliances.contains(f.getAttacker_aa()) && !this.coal1Nations.contains(f.getAttacker_id());
            isCol2Defender = f -> !this.coal1Alliances.contains(f.getDefender_aa()) && !this.coal1Nations.contains(f.getDefender_id());
        } else {
            isCol2Attacker = f -> this.coal2Alliances.contains(f.getAttacker_aa()) || this.coal2Nations.contains(f.getAttacker_id());
            isCol2Defender = f -> this.coal2Alliances.contains(f.getDefender_aa()) || this.coal2Nations.contains(f.getDefender_id());
        }
        isPrimary = f -> isCol1Attacker.test(f) && isCol2Defender.test(f);
        isSecondary = f -> isCol2Attacker.test(f) && isCol1Defender.test(f);
        this.start = start;
        this.end = end;
    }

    public static WarParser of(Guild guild, User author, DBNation me, String attackers, String defenders, long start) {
        return of(guild, author, me, attackers, defenders, start, Long.MAX_VALUE);
    }

    public static WarParser of(Guild guild, User author, DBNation me, String attackers, String defenders, long start, long end) {
        Set<Integer> coal1Alliances = DiscordUtil.parseAllianceIds(guild, attackers);
        Collection<DBNation> coal1Nations = coal1Alliances != null && !coal1Alliances.isEmpty() ? null : DiscordUtil.parseNations(guild, author, me, attackers, false, true);
        Set<Integer> coal2Alliances = DiscordUtil.parseAllianceIds(guild, defenders);
        Collection<DBNation> coal2Nations = coal2Alliances != null && !coal2Alliances.isEmpty() ? null : DiscordUtil.parseNations(guild, author, me, defenders, false, true);

        return ofNatObj(coal1Alliances, coal1Nations, coal2Alliances, coal2Nations, start, end);
    }

    public WarParser allowedWarTypes(Set<WarType> allowedWarTypes) {
        if (allowedWarTypes != null) getWars().entrySet().removeIf(f -> !allowedWarTypes.contains(f.getValue().getWarType()));
        return this;
    }

    public WarParser allowWarStatuses(Set<WarStatus> statuses) {
        if (statuses != null) getWars().entrySet().removeIf(f -> !statuses.contains(f.getValue().getStatus()));
        return this;
    }

    public Map<Integer, DBWar> getWars() {
        if (this.wars == null) {
            this.wars = Locutus.imp().getWarDb().getWars(coal1Alliances, coal1Nations, coal2Alliances, coal2Nations, start - TimeUnit.DAYS.toMillis(5), end);
        }
        return wars;
    }

    public Function<DBWar, Boolean> getIsPrimary() {
        return isPrimary;
    }

    public Function<DBWar, Boolean> getIsSecondary() {
        return isSecondary;
    }

    public String getNameA() {
        return nameA;
    }
    public String getNameB() {
        return nameB;
    }

    public DBNation getNation(int nationId, DBWar war) {
        return DBNation.getById(nationId);
    }
}

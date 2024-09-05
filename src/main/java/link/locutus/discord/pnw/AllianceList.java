package link.locutus.discord.pnw;

import link.locutus.discord.Locutus;
import link.locutus.discord.api.generated.ResourceType;
import link.locutus.discord.db.GuildDB;
import link.locutus.discord.db.entities.DBAlliance;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.db.entities.Treaty;
import link.locutus.discord.user.Roles;
import link.locutus.discord.util.StringMan;
import net.dv8tion.jda.api.entities.User;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class AllianceList {
    private final Set<Integer> ids;

    public AllianceList(Integer... ids) {
        this(Arrays.asList(ids));
    }

    public <T> AllianceList(Set<Integer> ids) {
        this.ids = ids;
    }

    public AllianceList subList(Roles role, User user, GuildDB db) {
        AllianceList allowed = role.getAllianceList(user, db);
        return this.subList(allowed.getIds());
    }

    public <T> AllianceList(Collection<Integer> ids) {
        this(new LinkedHashSet<>(ids));
    }

    public boolean isInAlliance(DBNation nation) {
        return ids.contains(nation.getNation_id());
    }

    public Set<DBNation> getNations() {
        return Locutus.imp().getNationDB().getNations(ids);
    }

    public Set<DBNation> getNations(boolean removeVM, int removeInactiveM, boolean removeApps) {
        Set<DBNation> nations = getNations();
        if (removeVM) nations.removeIf(f -> f.isVacation());
        if (removeInactiveM > 0) nations.removeIf(f -> f.active_m() > removeInactiveM);
        if (removeApps) nations.removeIf(f -> f.getPosition() <= 1);
        return nations;
    }

    public Set<DBNation> getNations(Predicate<DBNation> filter) {
        Set<DBNation> nations = new HashSet<>();
        for (DBNation nation : getNations()) {
            if (filter.test(nation)) nations.add(nation);
        }
        return nations;
    }

    public Set<DBAlliance> getAlliances() {
        Set<DBAlliance> alliances = new HashSet<>();
        for (int id : ids) {
            DBAlliance alliance = DBAlliance.get(id);
            if (alliance != null) {
                alliances.add(alliance);
            }
        }
        return alliances;
    }

    public AllianceList subList(Set<Integer> aaIds) {
        Set<Integer> copy = new LinkedHashSet<>(ids);
        copy.retainAll(aaIds);
        return new AllianceList(copy);
    }

    public AllianceList subList(Collection<DBNation> nations) {
        Set<Integer> ids = new HashSet<>();
        for (DBNation nation : nations) {
            if (!this.ids.contains(nation.getAlliance_id())) {
                throw new IllegalArgumentException("Nation " + nation.getNation() + " is not in the alliance: " + StringMan.getString(this.ids));
            }
            ids.add(nation.getAlliance_id());
        }
        return new AllianceList(ids);
    }

    public Map<DBNation, Map<ResourceType, Double>> getMemberStockpile() throws IOException {
        return getMemberStockpile(f -> true);
    }

    public Map<DBNation, Map<ResourceType, Double>> getMemberStockpile(Predicate<DBNation> fetchNation) throws IOException {
        Map<DBNation, Map<ResourceType, Double>> result = new LinkedHashMap<>();
        for (DBAlliance alliance : getAlliances()) {
            result.putAll(alliance.getMemberStockpile(fetchNation));
        }
        return result;
    }

    public Set<Integer> getIds() {
        return Collections.unmodifiableSet(ids);
    }

    public Map<Integer, Set<Treaty>> getTreaties() {
        Map<Integer, Set<Treaty>> treatiesBySender = new HashMap<>();
        for (DBAlliance alliance : getAlliances()) {
            for (Map.Entry<Integer, Treaty> entry : alliance.getTreaties().entrySet()) {
                treatiesBySender.computeIfAbsent(entry.getKey(), f -> new HashSet<>()).add(entry.getValue());
            }
        }
        return treatiesBySender;
    }

    public boolean isEmpty() {
        for (int id : ids) {
            if (DBAlliance.get(id) != null) {
                return false;
            }
        }
        return true;
    }

    public boolean contains(DBAlliance to) {
        return ids.contains(to.getAlliance_id());
    }

    public boolean contains(int aaId) {
        return ids.contains(aaId);
    }

    public Map<ResourceType, Double> getStockpile(long timestamp) throws IOException {
        Map<ResourceType, Double> stockpile = new HashMap<>();
        for (DBAlliance alliance : getAlliances()) {
            for (Map.Entry<ResourceType, Double> entry : alliance.getStockpile(timestamp).entrySet()) {
                stockpile.put(entry.getKey(), stockpile.getOrDefault(entry.getKey(), 0.0) + entry.getValue());
            }
        }
        return stockpile;
    }

    public int size() {
        return ids.size();
    }
}

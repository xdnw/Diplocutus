package link.locutus.discord.db.handlers;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import link.locutus.discord.db.WarDB;
import link.locutus.discord.db.entities.DBWar;

import java.util.*;
import java.util.function.Predicate;

public class ActiveWarHandler {
    private final Map<Integer, DBWar[]> activeWars = new Int2ObjectOpenHashMap<>();
    private final WarDB warDB;
    private volatile int numActiveWars = 0;

    public ActiveWarHandler(WarDB warDB) {
        this.warDB = warDB;
    }

    private void makeWarInactive(int nationId, int warId) {
        synchronized (activeWars) {
            DBWar[] wars = activeWars.get(nationId);
            if (wars != null && wars.length > 0) {
                Set<DBWar> newWars = new HashSet<>(Arrays.asList(wars));
                int originalSize = newWars.size();
                if (newWars.removeIf(f -> f.getWarId() == warId)) {
                    if (newWars.isEmpty()) {
                        activeWars.remove(nationId);
                    } else {
                        activeWars.put(nationId, newWars.toArray(new DBWar[0]));
                    }
                    numActiveWars += newWars.size() - originalSize;
                }
            }
        }
    }

    public boolean isEmpty() {
        return activeWars.isEmpty();
    }
    public void makeWarInactive(DBWar war) {
        makeWarInactive(war.getAttacker_id(), war.getWarId());
        makeWarInactive(war.getDefender_id(), war.getWarId());
    }

    public DBWar getWar(int nationId, int warId) {
        synchronized (activeWars) {
            DBWar[] wars = activeWars.get(nationId);
            if (wars != null) {
                for (DBWar war : wars) {
                    if (war.getWarId() == warId) return war;
                }
            }
        }
        return null;
    }

    private void addActiveWar(int nationId, DBWar war) {
        if (!war.isActive()) return;
        synchronized (activeWars) {
            DBWar[] wars = activeWars.get(nationId);
            if (wars == null) wars = new DBWar[0];
            Set<DBWar> newWars = new HashSet<>(Arrays.asList(wars));
            int originalSize = newWars.size();
            newWars.removeIf(f -> f.getWarId() == war.getWarId());
            newWars.add(war);
            activeWars.put(nationId, newWars.toArray(new DBWar[0]));
            numActiveWars += newWars.size() - originalSize;
        }
    }
    public void addActiveWar(DBWar war) {
        addActiveWar(war.getAttacker_id(), war);
        addActiveWar(war.getDefender_id(), war);
    }

    public ObjectOpenHashSet<DBWar> getActiveWarsById() {
        ObjectOpenHashSet<DBWar> result = new ObjectOpenHashSet<>(numActiveWars >> 1);
        synchronized (activeWars) {
            for (DBWar[] nationWars : activeWars.values()) {
                result.addAll(Arrays.asList(nationWars));
            }
        }
        return result;
    }

    public ObjectOpenHashSet<DBWar> getActiveWars(Predicate<Integer> nationId, Predicate<DBWar> warPredicate) {
        ObjectOpenHashSet<DBWar> result = new ObjectOpenHashSet<>();
        synchronized (activeWars) {
            for (Map.Entry<Integer, DBWar[]> entry : activeWars.entrySet()) {
                if (nationId == null || nationId.test(entry.getKey())) {
                    for (DBWar war : entry.getValue()) {
                        if (warPredicate == null || warPredicate.test(war)) {
                            result.add(war);
                        }
                    }
                }
            }
        }
        return result;
    }

    public ObjectOpenHashSet<DBWar> getActiveWars() {
        return getActiveWarsById();
    }

    public Set<DBWar> getActiveWars(int nationId) {
        synchronized (activeWars) {
            DBWar[] wars = activeWars.get(nationId);
            return wars == null ? Collections.emptySet() : new ObjectOpenHashSet<>(wars);
        }
    }

    public DBWar getWar(int warId) {
        synchronized (activeWars) {
            for (DBWar[] nationWars : activeWars.values()) {
                for (DBWar war : nationWars) {
                    if (war.getWarId() == warId) return war;
                }
            }
        }
        return null;
    }
}

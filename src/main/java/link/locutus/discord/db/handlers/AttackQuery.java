package link.locutus.discord.db.handlers;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import link.locutus.discord.Locutus;
import link.locutus.discord.db.WarDB;
import link.locutus.discord.db.entities.DBWar;
import link.locutus.discord.util.TimeUtil;
import link.locutus.discord.util.math.ArrayUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AttackQuery {

    private final WarDB warDb;
    public ObjectOpenHashSet<DBWar> wars;

    public AttackQuery(WarDB warDb) {
        this.warDb = warDb;
    }

    public Set<DBWar> getWars() {
        return wars;
    }

    public WarDB getDb() {
        return warDb;
    }

    public AttackQuery withWars(Collection<DBWar> wars) {
        this.wars = wars instanceof ObjectOpenHashSet ? (ObjectOpenHashSet<DBWar>) wars : new ObjectOpenHashSet<>(wars);
        return this;
    }

    public AttackQuery withWars(long start, long end) {
        if (end == Long.MAX_VALUE) {
            if (start <= 0) {
                return withAllWars();
            } else {
                return withWars(getDb().getWars(f -> f.getDate() >= start));
            }
        } else {
            return withWars(getDb().getWars(f -> f.getDate() >= start && f.getDate() <= end));
        }
    }

    public AttackQuery withWars(Map<Integer, DBWar> wars) {
        this.wars = new ObjectOpenHashSet<>(wars.values());
        return this;
    }

    public AttackQuery withWarSet(ObjectOpenHashSet<DBWar> wars) {
        this.wars = wars;
        return this;
    }

    public AttackQuery withWarSet(Function<WarDB, Set<DBWar>> dbConsumer) {
        return withWars(dbConsumer.apply(getDb()));
    }

    public AttackQuery withWarMap(Function<WarDB, Map<Integer, DBWar>> dbConsumer) {
        return withWars(dbConsumer.apply(getDb()));
    }

    public AttackQuery withWar(DBWar war) {
        this.wars = new ObjectOpenHashSet<>(1);
        this.wars.add(war);
        return this;
    }

    public AttackQuery afterDate(long start) {
        wars.removeIf(e -> e.getDate() < start);
        return this;
    }

    public AttackQuery beforeDate(long end) {
        wars.removeIf(e -> e.getDate() > end);
        return this;

    }

    public AttackQuery between(long start, long end) {
        wars.removeIf(e -> e.getDate() < start || e.getDate() > end);
        return this;
    }

    public AttackQuery withWarsForNationOrAlliance(Predicate<Integer> nations, Predicate<Integer> alliances, Predicate<DBWar> warFilter) {
        withWars(getDb().getWarsForNationOrAlliance(nations, alliances, warFilter));
        return this;
    }

    public AttackQuery withActiveWars(Predicate<Integer> nationId, Predicate<DBWar> warPredicate) {
        wars = getDb().getActiveWars(nationId, warPredicate);
        return this;
    }

    public AttackQuery withWars(Predicate<DBWar> warFilter) {
        withWars(getDb().getWars(warFilter));
        return this;
    }


    public AttackQuery withAllWars() {
        return this.withWarSet(getDb().getWars());
    }

    public AttackQuery withActiveWars() {
        return this.withWars(getDb().getActiveWars());
    }
}


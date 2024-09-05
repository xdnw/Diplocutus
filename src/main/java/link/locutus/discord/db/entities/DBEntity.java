package link.locutus.discord.db.entities;

import link.locutus.discord.api.endpoints.DnsApi;
import link.locutus.discord.api.endpoints.DnsQuery;
import link.locutus.discord.api.types.tx.BankTransfer;
import link.locutus.discord.db.DBMainV2;
import link.locutus.discord.event.Event;
import link.locutus.discord.util.scheduler.ThrowingConsumer;
import link.locutus.discord.util.scheduler.ThrowingFunction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface DBEntity<T, E extends DBEntity<T, E>> {
    String getTableName();

    boolean update(T entity, Consumer<Event> eventConsumer);

    Object[] write();

    void load(Object[] raw);

    Map<String, Class<?>> getTypes();

    E emptyInstance();

    default <U, V> U localCall(long timestamp, AtomicLong cacheMs, U data, Supplier<DnsApi> getApi, Supplier<Integer> getId, BiFunction<DnsApi, Integer, DnsQuery<V>> call, BiConsumer<V, Long> update) {
        return withApi(cacheMs, timestamp, data, () -> {
            DnsApi api = getApi.get();
            if (api != null) {
                long now = System.currentTimeMillis();
                List<V> result = call.apply(api, getId.get()).call();
                if (result != null && !result.isEmpty()) {
                    update.accept(result.get(0), now);
                }
                return true;
            }
            return false;
        });
    }

    default  <U> U withApi(AtomicLong lastUpdated, long timestamp, U result, Supplier<Boolean> update) {
        if (lastUpdated.get() > timestamp) return result;
        synchronized (lastUpdated) {
            if (lastUpdated.get() > timestamp) return result;
            long now = System.currentTimeMillis();
            if (update.get()) {
                lastUpdated.set(now);
            }
            return result;
        }
    }

    default E copy() {
        E copy = emptyInstance();
        copy.load(write());
        return copy;
    }

    default void load(E other) {
        load(other.write());
    }

    default String insert(boolean replace) {
        StringBuilder sb = new StringBuilder("INSERT ");
        if (replace) sb.append("OR REPLACE ");
        else /* ignore */ sb.append("IGNORE ");
        sb.append("INTO ").append(getTableName()).append(" (");
        Map<String, Class<?>> types = getTypes();
        for (String key : types.keySet()) {
            sb.append(key).append(", ");
        }
        sb.setLength(sb.length() - 2);
        sb.append(") VALUES (");
        for (int i = 0; i < types.size(); i++) {
            sb.append("?, ");
        }
        sb.setLength(sb.length() - 2);
        sb.append(");");
        return sb.toString();
    }

    default ThrowingFunction<ResultSet, E> loader() {
        Map<String, Class<?>> types = getTypes();
        return rs -> emptyInstance().load(types, rs);
    }

    default E load(Map<String, Class<?>> types, ResultSet rs) throws SQLException {
        Object[] raw = new Object[types.size()];
        for (int i = 0; i < raw.length; i++) {
            raw[i] = rs.getObject(i + 1);
        }
        load(raw);
        return (E) this;
    }

    default void write(PreparedStatement stmt) throws SQLException {
        Object[] values = write();
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (value == null) {
                stmt.setNull(i + 1, Types.NULL);
            } else {
                stmt.setObject(i + 1, value);
            }
        }
    }
}

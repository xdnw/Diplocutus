package link.locutus.discord.commands.manager.v2.binding.bindings;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import link.locutus.discord.commands.manager.v2.binding.Key;
import link.locutus.discord.commands.manager.v2.binding.ValueStore;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PlaceholderCache<T> {
    protected final List<T> list;
    protected boolean cached = false;
    protected final Map<String, Map<T, Object>> cacheInstance = new Object2ObjectOpenHashMap<>();
    protected final Map<String, Object> cacheGlobal = new Object2ObjectOpenHashMap<>();

    public PlaceholderCache(Collection<T> set) {
        this.list = new ObjectArrayList<>(new ObjectOpenHashSet<>(set));
    }

    public List<T> getList() {
        return list;
    }

    public static <T> ScopedPlaceholderCache<T> getScoped(ValueStore store, Class<T> clazz, String method) {
        PlaceholderCache<T> cache = (PlaceholderCache<T>) store.getProvided(Key.of(PlaceholderCache.class, clazz), false);
        return getScoped(cache, method);
    }

    public static <T> ScopedPlaceholderCache<T> getScoped(PlaceholderCache<T> cache, String method) {
        return new ScopedPlaceholderCache<T>(cache, method);
    }

    public Object get(T object, String id) {
        Map<T, Object> map = cacheInstance.computeIfAbsent(id, o -> new Object2ObjectOpenHashMap<>());
        return map.get(object);
    }

    public boolean has(T object, String id) {
        Map<T, Object> map = cacheInstance.get(id);
        return map != null && map.containsKey(object);
    }

    public void put(T object, String id, Object value) {
        Map<T, Object> map = cacheInstance.computeIfAbsent(id, o -> new Object2ObjectOpenHashMap<>());
        map.put(object, value);
    }

    public Object getGlobal(String id) {
        return cacheGlobal.get(id);
    }

    public void putGlobal(String id, Object value) {
        cacheGlobal.put(id, value);
    }
}

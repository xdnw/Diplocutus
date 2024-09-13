package link.locutus.discord.api;

import link.locutus.discord.Locutus;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.db.guild.GuildKey;

import java.util.*;

public class ApiKeyPool {
    private final List<ApiKey> apiKeyPool;
    private int nextIndex;

    /**
     * Creates a new ApiKeyPool with the given keys.
     * @param keys The keys to use.
     */
    public ApiKeyPool(Collection<ApiKey> keys) {
        this.apiKeyPool = new ArrayList<>(keys);
        this.nextIndex = 0;
        if (apiKeyPool.size() == 0) {
            throw new IllegalArgumentException("No API Key provided, Make sure apiKeyPool array is not empty.");
        }
    }

    public static SimpleBuilder builder() {
        return new SimpleBuilder();
    }

    public static ApiKeyPool create(int nationId, String key) {
        return builder().addKey(nationId, key).build();
    }

    public static ApiKeyPool create(ApiKey key) {
        return builder().addKey(key).build();
    }

    public List<ApiKey> getKeys() {
        return apiKeyPool;
    }

    public synchronized ApiKey getNextApiKey() {
        if (this.nextIndex >= this.apiKeyPool.size()) {
            this.nextIndex = 0;
        }
        for (int i = 0; i < this.apiKeyPool.size(); i++) {
            ApiKey key = this.apiKeyPool.get((this.nextIndex + i) % this.apiKeyPool.size());
            if (key.isValid()) {
                this.nextIndex = (this.nextIndex + i + 1) % this.apiKeyPool.size();
                return key.use();
            }
        }
        throw new IllegalArgumentException("No API key found: " + GuildKey.API_KEY.getCommandMention() + "`)");
    }

    public synchronized void removeKey(ApiKey key) {
        key.setValid(false);
        if (apiKeyPool.size() == 1) throw new IllegalArgumentException("Invalid API key.");
        this.apiKeyPool.removeIf(f -> f.equals(key));
    }

    public int size() {
        return apiKeyPool.size();
    }

    public static class ApiKey {
        private final String key;
        private int nationId;
        private boolean valid;
        private int usage;

        public ApiKey(int nationId, String key) {
            this.nationId = nationId;
            this.key = key;
            this.valid = true;
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public void deleteApiKey() {
            setValid(false);
            Locutus.imp().getDiscordDB().deleteApiKey(key);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ApiKey)) return false;
            return ((ApiKey) obj).key.equalsIgnoreCase(key);
        }

        public ApiKey use() {
            usage++;
            return this;
        }

        public int getUsage() {
            return usage;
        }

        public String getKey() {
            return key;
        }

        public int getNationId() {
            if (nationId == -1) {
                nationId = Locutus.imp().getDiscordDB().getNationFromApiKey(key);
            }
            return nationId;
        }

        public DBNation getNation() {
            return DBNation.getById(nationId);
        }
    }

    public static class SimpleBuilder {
        private final Map<String, ApiKey> keys = new LinkedHashMap<>();

        public boolean isEmpty() {
            return keys.isEmpty();
        }

        @Deprecated
        public SimpleBuilder addKeyUnsafe(String key) {
            return addKey(-1, key);
        }

        @Deprecated
        public SimpleBuilder addKeysUnsafe(String... keys) {
            for (String key : keys) addKeyUnsafe(key);
            return this;
        }

        public SimpleBuilder addKeysUnsafe(List<String> keys) {
            for (String key : keys) addKeyUnsafe(key);
            return this;
        }

        public SimpleBuilder addKeys(List<ApiKey> keys) {
            for (ApiKey key : keys) addKey(key);
            return this;
        }

        public SimpleBuilder addKey(int nationId, String apiKey) {
            ApiKey key = new ApiKey(nationId, apiKey);
            apiKey = apiKey.toLowerCase(Locale.ROOT);
            ApiKey existing = this.keys.get(apiKey);
            if (existing != null) return this;
            this.keys.put(apiKey, key);
            return this;
        }

        public ApiKeyPool build() {
            if (keys.isEmpty()) throw new IllegalArgumentException("No api keys were provided.");
            return new ApiKeyPool(keys.values());
        }

        public SimpleBuilder addKey(ApiKey key) {
            return addKey(key.nationId, key.key);
        }
    }
}

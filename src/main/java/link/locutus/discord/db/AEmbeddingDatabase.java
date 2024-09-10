package link.locutus.discord.db;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import link.locutus.discord.Logg;
import link.locutus.discord.db.entities.EmbeddingSource;
import link.locutus.discord.gpt.IEmbeddingDatabase;
import link.locutus.discord.gpt.imps.ConvertingDocument;
import link.locutus.discord.gpt.imps.DocumentChunk;
import link.locutus.discord.gpt.imps.EmbeddingInfo;
import link.locutus.discord.gpt.pw.GptDatabase;
import link.locutus.discord.util.StringMan;
import link.locutus.discord.util.math.ArrayUtil;
import link.locutus.discord.util.scheduler.ThrowingConsumer;
import link.locutus.discord.util.scheduler.TriConsumer;
import javax.annotation.Nullable;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.SQLDataType;

import java.io.Closeable;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static graphql.com.google.common.base.Preconditions.checkArgument;
import static graphql.com.google.common.base.Preconditions.checkNotNull;

public abstract class AEmbeddingDatabase implements IEmbeddingDatabase, Closeable {

    private volatile boolean loaded = false;
    private final Long2ObjectOpenHashMap<float[]> vectors;
    private final Map<Integer, Set<Long>> textHashBySource;
    private final Map<Integer, Map<Long, Long>> expandedTextHashBySource;
    private final Map<Integer, EmbeddingSource> embeddingSources;
    private final Map<Integer, ConvertingDocument> unconvertedDocuments;
    private final Map<Integer, Map<Integer, DocumentChunk>> documentChunks;
    private final String vectorName;
    private final GptDatabase database;

    public AEmbeddingDatabase(String name, GptDatabase database) throws SQLException, ClassNotFoundException {
        this.database = database;
        this.vectorName = name;
        this.vectors = new Long2ObjectOpenHashMap<>();
        this.textHashBySource = new Int2ObjectOpenHashMap<>();
        this.expandedTextHashBySource = new Int2ObjectOpenHashMap<>();
        this.embeddingSources = new ConcurrentHashMap<>();
        this.unconvertedDocuments = new ConcurrentHashMap<>();
        this.documentChunks = new ConcurrentHashMap<>();
    }

    public <T extends AEmbeddingDatabase> T load() {
        if (loaded) return (T) this;
        loaded = true;
        createTables();
        importLegacyDate();
        loadVectors();
        loadHashesBySource();
        loadSources();
        loadExpandedTextMeta();
        loadUnconvertedDocuments();
        return (T) this;
    }

    public GptDatabase getDatabase() {
        return database;
    }

    @Override
    public void close() {
        database.close();
    }

    @Override
    public void registerHashes(EmbeddingSource source, Set<Long> hashes, boolean deleteAbsent) {
        load();
        checkNotNull(source);
        if (hashes.isEmpty()) {
            if (deleteAbsent) {
                throw new IllegalArgumentException("Cannot delete absent hashes if no hashes are provided");
            }
            return;
        }
        Set<Long> existing = textHashBySource.get(source.source_id);
        if (existing == null) {
            existing = new LongOpenHashSet();
            textHashBySource.put(source.source_id, existing);
            for (long hash : hashes) {
                existing.add(hash);
                saveVectorSources(hash, source.source_id);
            }
        } else {
            Iterator<Long> iter = existing.iterator();
            while (iter.hasNext()) {
                long hash = iter.next();
                if (!hashes.contains(hash)) {
                    iter.remove();
                    if (deleteAbsent) {
                        deleteHash(source.source_id, hash);
                    }
                }
            }
            for (long hash : hashes) {
                if (!existing.contains(hash)) {
                    existing.add(hash);
                    saveVectorSources(hash, source.source_id);
                }
            }
        }
    }

    private DSLContext ctx() {
        return database.ctx();
    }

    public void deleteHash(int source, long hash) {
        ctx().execute("DELETE FROM vector_sources WHERE hash = ? AND source_id = ?", hash, source);
    }

    private void createVectorsTable() {
        String tableName = "vectors_" + vectorName;
        ctx().createTableIfNotExists(tableName)
                .column("hash", SQLDataType.BIGINT.notNull())
                .column("data", SQLDataType.BINARY.notNull())
                .primaryKey("hash")
                .execute();

        // `vectors` exits, import it
        try {
            ctx().execute("INSERT INTO " + tableName + " (hash, data) SELECT hash, data FROM vectors");
            ctx().execute("DROP TABLE vectors");
        } catch (Throwable ignore) {

        }
    }

    public synchronized void saveVector(long hash, float[] vector) {
        load();
        byte[] data = ArrayUtil.toByteArray(vector);
        vectors.put(hash, vector);
        String table = "vectors_" + vectorName;
        ctx().execute("INSERT INTO " + table + " (hash, data) VALUES (?, ?)", hash, data);
    }

    private void createVectorTextTable() {
        ctx().createTableIfNotExists("vector_text")
                .column("hash", SQLDataType.BIGINT.notNull())
                .column("description", SQLDataType.VARCHAR.notNull())
                .primaryKey("hash")
                .execute();
    }

    public synchronized void saveVectorText(long hash, String description) {
        ctx().execute("INSERT INTO vector_text (hash, description) VALUES (?, ?)", hash, description);
    }

    private void createExpandedTextTable() {
        ctx().createTableIfNotExists("expanded_text")
                .column("embedding_hash", SQLDataType.BIGINT.notNull())
                .column("body_hash", SQLDataType.BIGINT.notNull())
                .column("source_id", SQLDataType.BIGINT.notNull())
                .column("body", SQLDataType.VARCHAR.notNull())
                .primaryKey("embedding_hash", "source_id")
                .execute();
    }

    public synchronized boolean saveExpandedText(long embedding_hash, int source_id, String body) {
        load();
        long body_hash = getHash(body);
        Map<Long, Long> hashesBySource = expandedTextHashBySource.computeIfAbsent(source_id, k -> new Long2LongOpenHashMap());
        // if changed
        Long existing = hashesBySource.get(embedding_hash);
        if (existing == null || existing != body_hash) {
            hashesBySource.put(embedding_hash, body_hash);
            ctx().execute("INSERT OR REPLACE INTO expanded_text (embedding_hash, body_hash, source_id, body) VALUES (?, ?, ?, ?)", embedding_hash, body_hash, source_id, body);
            return true;
        }
        return false;
    }

    public boolean hasExpandedText(int source_id, long embedding_hash) {
        load();
        Map<Long, Long> hashesBySource = expandedTextHashBySource.get(source_id);
        if (hashesBySource == null) {
            return false;
        }
        return hashesBySource.containsKey(embedding_hash);
    }

    public String getText(long hash) {
        return ctx().selectFrom("vector_text").where("hash = ?", hash).fetchOne("description", String.class);
    }

    public String getExpandedText(int source_id, long embedding_hash) {
        load();
        Map<Long, Long> hashesBySource = expandedTextHashBySource.get(source_id);
        if (hashesBySource == null) {
            return null;
        }
        Long body_hash = hashesBySource.get(embedding_hash);
        if (body_hash == null) {
            return null;
        }
        return ctx().selectFrom("expanded_text").where("embedding_hash = ? AND source_id = ?", embedding_hash, source_id).fetchOne("body", String.class);
    }

    private void createVectorSourcesTable() {
        ctx().createTableIfNotExists("vector_sources")
                .column("hash", SQLDataType.BIGINT.notNull())
                .column("source_id", SQLDataType.BIGINT.notNull())
                .primaryKey("hash", "source_id")
                .execute();
    }

    public synchronized void saveVectorSources(long hash, int source_id) {
        load();
        textHashBySource.computeIfAbsent(source_id, k -> new LongOpenHashSet()).add(hash);
        ctx().execute("INSERT INTO vector_sources (source_id, hash) VALUES (?, ?)", source_id, hash);
    }

    private void createSourcesTable() {
        ctx().execute("CREATE TABLE IF NOT EXISTS sources (source_id INTEGER PRIMARY KEY AUTOINCREMENT, source_name VARCHAR NOT NULL, date_added BIGINT NOT NULL, hash BIGINT NOT NULL, guild_id BIGINT NOT NULL)");
    }

    private void createDocumentQueueTable() {
        ctx().execute("CREATE TABLE IF NOT EXISTS document_queue (" +
                "source_id INTEGER NOT NULL, " +
                "prompt VARCHAR NOT NULL, " +
                "converted BOOLEAN NOT NULL, " +
                "use_global_context BOOLEAN NOT NULL, " +
                "provider_type INTEGER NOT NULL, " +
                "user BIGINT NOT NULL, " +
                "error VARCHAR, " +
                "date BIGINT NOT NULL, " +
                "hash BIGINT NOT NULL, " +
                "PRIMARY KEY (source_id))");
    }

    private void createChunksTable() {
        ctx().execute("CREATE TABLE IF NOT EXISTS document_chunks (" +
                "source_id INTEGER NOT NULL, " +
                "chunk_index INTEGER NOT NULL, " +
                "converted BOOLEAN NOT NULL, " +
                "text VARCHAR NOT NULL," +
                "output VARCHAR, PRIMARY KEY (source_id, chunk_index))");
    }

    private void loadUnconvertedDocuments() {
        ctx().execute("DELETE FROM document_queue WHERE converted = ?", true);
        ctx().execute("DELETE FROM document_chunks WHERE converted = ?", true);
        // delete where source_id not in sources
        ctx().execute("DELETE FROM document_queue WHERE source_id NOT IN (SELECT source_id FROM sources)");
        ctx().execute("DELETE FROM document_chunks WHERE source_id NOT IN (SELECT source_id FROM document_queue)");

        List<ConvertingDocument> docs = ctx().selectFrom("document_queue").fetchInto(ConvertingDocument.class);
        for (ConvertingDocument doc : docs) {
            unconvertedDocuments.put(doc.source_id, doc);
        }
        List<DocumentChunk> chunks = ctx().selectFrom("document_chunks").fetchInto(DocumentChunk.class);
        for (DocumentChunk chunk : chunks) {
            documentChunks.computeIfAbsent(chunk.source_id, k -> new ConcurrentHashMap<>()).put(chunk.source_id, chunk);
        }
    }

    public List<ConvertingDocument> getUnconvertedDocuments() {
        load();
        return new ArrayList<>(this.unconvertedDocuments.values());
    }

    @Override
    public ConvertingDocument getConvertingDocument(int source_id) {
        load();
        return unconvertedDocuments.get(source_id);
    }

    public void addConvertingDocument(List<ConvertingDocument> documents) {
        load();
        for (ConvertingDocument document : documents) {
            unconvertedDocuments.put(document.source_id, document);
        }
        ctx().transaction((TransactionalRunnable) -> {
            for (ConvertingDocument document : documents) {
                ctx().execute("INSERT OR REPLACE INTO document_queue (source_id, prompt, converted, use_global_context, provider_type, user, error, date, hash) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        document.source_id, document.prompt, document.converted, document.use_global_context, document.provider_type, document.user, document.error, document.date, document.hash);
            }
        });
    }

    public void addChunks(List<DocumentChunk> chunks) {
        load();
        for (DocumentChunk chunk : chunks) {
            documentChunks.computeIfAbsent(chunk.source_id, k -> new ConcurrentHashMap<>()).put(chunk.chunk_index, chunk);
        }
        ctx().transaction((TransactionalRunnable) -> {
        for (DocumentChunk chunk : chunks) {
            ctx().execute("INSERT OR REPLACE INTO document_chunks (source_id, chunk_index, converted, text, output) VALUES (?, ?, ?, ?, ?)",
                    chunk.source_id, chunk.chunk_index, chunk.converted, chunk.text, chunk.output);
        }
        });
    }

    public List<DocumentChunk> getChunks(int source_id) {
        load();
        ArrayList<DocumentChunk> result = new ArrayList<>(documentChunks.getOrDefault(source_id, Collections.emptyMap()).values());
        result.sort(Comparator.comparingInt(o -> o.chunk_index));
        return result;
    }

    @Override
    public void deleteDocumentAndChunks(int sourceId) {
        load();
        documentChunks.remove(sourceId);
        unconvertedDocuments.remove(sourceId);
        ctx().transaction((TransactionalRunnable) -> {
            ctx().execute("DELETE FROM document_queue WHERE source_id = ?", sourceId);
            ctx().execute("DELETE FROM document_chunks WHERE source_id = ?", sourceId);
        });
    }

    @Override
    public EmbeddingSource getEmbeddingSource(int source_id) {
        load();
        return embeddingSources.get(source_id);
    }

    @Override
    public synchronized EmbeddingSource getOrCreateSource(String name, long guild_id) {
        load();
        name = name.toLowerCase();

        EmbeddingSource source = this.getSource(name, guild_id);

        if (source == null) {
            long date_added = System.currentTimeMillis();
            // create
            source = new EmbeddingSource(-1, name, date_added, 0, guild_id);
            ctx().execute("INSERT INTO sources (source_name, date_added, guild_id) VALUES (?, ?, ?)", source.source_name, source.date_added, source.guild_id);
            // set source id
            @Nullable Record result = ctx().fetchOne("SELECT source_id FROM sources WHERE source_name = ? AND date_added = ? AND guild_id = ?", source.source_name, source.date_added, source.guild_id);
            int source_id = (Integer) result.getValue("source_id");
            source = new EmbeddingSource(source_id, source.source_name, source.date_added, 0, source.guild_id);
            // add to map
            embeddingSources.put(source.source_id, source);
            return source;
        } else {
            return source;
        }
    }

    @Override
    public void updateSources(List<EmbeddingSource> sources) {
        load();
        ctx().transaction((TransactionalRunnable) -> {
            for (EmbeddingSource source : sources) {
                ctx().execute("UPDATE sources SET source_name = ?, date_added = ?, guild_id = ? WHERE source_id = ?",
                        source.source_name, source.date_added, source.guild_id, source.source_id);
            }
        });
    }

    private void importLegacyDate() {
        try {
            ctx().select().from("embeddings_2").fetch().forEach(r -> {
                long hash = r.get("hash", Long.class);
                byte[] data = r.get("data", byte[].class);
                String id = r.get("id", String.class);
                String table = "vectors_" + vectorName;
                ctx().execute("INSERT INTO " + table + " (hash, data) VALUES (?, ?)", hash, data);
                ctx().execute("INSERT INTO vector_text (hash, description) VALUES (?, ?)", hash, id);
            });
            ctx().dropTableIfExists("embeddings_2").execute();
        } catch (DataAccessException ignore) {}
    }

    private void loadVectors() {
        String table = "vectors_" + vectorName;
            ctx().select().from(table).fetch().forEach(r -> {
            long hash = r.get("hash", Long.class);
            byte[] data = r.get("data", byte[].class);
            float[] vector = ArrayUtil.toFloatArray(data);
            this.vectors.put(hash, vector);
        });
    }

    private void loadHashesBySource() {
        ctx().select().from("vector_sources").fetch().forEach(r -> {
            long hash = r.get("hash", Long.class);
            int source_id = r.get("source_id", Integer.class);
            Set<Long> hashes = textHashBySource.get(source_id);
            if (hashes == null) {
                hashes = new LongOpenHashSet();
                textHashBySource.put(source_id, hashes);
            }
            hashes.add(hash);
        });
    }

    private void loadSources() {
        ctx().select().from("sources").fetch().forEach(r -> {
            int source_id = r.get("source_id", Integer.class);
            String source_name = r.get("source_name", String.class);
            long date_added = r.get("date_added", Long.class);
            long guild_id = r.get("guild_id", Long.class);
            long hash = r.get("hash", Long.class);

            // embeddingSources is a map of guild_id to set<EmbeddingSource>
            EmbeddingSource source = new EmbeddingSource(source_id, source_name, date_added, hash, guild_id);
            embeddingSources.put(source.source_id, source);
        });
    }

    public synchronized void createTables() {
        // vectors: long hash, byte[] data
        createVectorsTable();
        //        vector_text: long hash, String description
        createVectorTextTable();
        //        expanded_text: long hash, long source_id, String body primary key is (hash, source_id)
        createExpandedTextTable();
//        vector_sources long hash, long source_id
        createVectorSourcesTable();
//        sources: long source_id, String source_name, long date_added, long guild_id
        createSourcesTable();
        // document_queue: source_id, prompt, converted, use_global_context, gpt_provider, user, error, date
        createDocumentQueueTable();
        // document_chunks: source_id, chunk_index, converted, text
        createChunksTable();
    }

    private void loadExpandedTextMeta() {
//        expandedTextHashBySource
        ctx().select().from("expanded_text").fetch().forEach(r -> {
//            .column("embedding_hash", SQLDataType.BIGINT.notNull())
//                    .column("body_hash", SQLDataType.BIGINT.notNull())
            long embedding_hash = r.get("embedding_hash", Long.class);
            long body_hash = r.get("body_hash", Long.class);
            int source_id = r.get("source_id", Integer.class);
            Map<Long, Long> hashes = expandedTextHashBySource.computeIfAbsent(source_id, k -> new Long2LongOpenHashMap());
            hashes.put(embedding_hash, body_hash);
        });
    }

    @Override
    public float[] getEmbedding(long hash) {
        load();
        return vectors.get(hash);
    }

    @Override
    public void deleteSource(EmbeddingSource source) {
        load();
        // delete from expanded_text and sources and vector_sources
        int source_id = source.source_id;
        ctx().execute("DELETE FROM expanded_text WHERE source_id = ?", source_id);
        ctx().execute("DELETE FROM sources WHERE source_id = ?", source_id);
        ctx().execute("DELETE FROM vector_sources WHERE source_id = ?", source_id);
        deleteDocumentAndChunks(source_id);

        embeddingSources.remove(source.source_id);
        // textHashBySource
        textHashBySource.remove(source_id);
        // expandedTextHashBySource
        expandedTextHashBySource.remove(source_id);
        unconvertedDocuments.remove(source_id);
        documentChunks.remove(source_id);
    }

    @Override
    public float[] getEmbedding(String content) {
        return getEmbedding(getHash(content));
    }

    @Override
    public float[] getOrCreateEmbedding(long embeddingHash, String embeddingText, Supplier<String> fullContent, EmbeddingSource source, boolean save, ThrowingConsumer<String> moderate) {
        float[] existing = getEmbedding(embeddingHash);
        if (existing == null) {
            if (moderate != null) {
                moderate.accept(embeddingText);
            }
            // fetch embedding
            existing = fetchEmbedding(embeddingText);
            // store
            if (save) {
                saveVector(embeddingHash, existing);
                saveVectorText(embeddingHash, embeddingText);
            }
        }
        if (save) {
            Set<Long> hashes = textHashBySource.get(source.source_id);
            if (hashes == null || !hashes.contains(embeddingHash)) {
                saveVectorSources(embeddingHash, source.source_id);
            }
            if (fullContent != null) {
                String full = fullContent.get();
                if (full != null && !full.equals(embeddingText)) {
                    saveExpandedText(embeddingHash, source.source_id, full);
                }
            }
        }
        return existing;
    }

    @Override
    public long getHash(String data) {
        BigInteger value = StringMan.hash_fnv1a_64(data.getBytes());
        value = value.add(BigInteger.valueOf(Long.MIN_VALUE));
        return value.longValueExact();
    }

    @Override
    public void iterateVectors(Set<EmbeddingSource> allowedSources, TriConsumer<EmbeddingSource, Long, float[]> source_hash_vector_consumer) {
        for (EmbeddingSource source : allowedSources) {
            Set<Long> hashes = textHashBySource.get(source.source_id);
            if (hashes != null && !hashes.isEmpty()) {
                for (long hash : hashes) {
                    float[] vector = vectors.get(hash);
                    source_hash_vector_consumer.accept(source, hash, vector);
                }
            }
        }
    }

    public Map<Long, String> getContent(Set<Long> hashes) {
        Map<Long, String> result = new Long2ObjectOpenHashMap<>();
        List<Long> hashesSorted = new LongArrayList();
        // sort ascending
        hashesSorted.addAll(hashes);
        hashesSorted.sort(Long::compareTo);
        String query = "SELECT hash, description FROM vector_text WHERE hash IN (" + hashesSorted.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")";
        ctx().fetch(query).forEach(r -> {
            long hash = r.get("hash", Long.class);
            String description = r.get("description", String.class);
            result.put(hash, description);
        });
        return result;
    }

    @Override
    public int countVectors(EmbeddingSource existing) {
        Set<Long> hashes = textHashBySource.get(existing.source_id);
        return hashes == null ? 0 : hashes.size();
    }

    @Override
    public EmbeddingSource getSource(String name, long guild_id) {
        for (EmbeddingSource source : embeddingSources.values()) {
            if (source.guild_id == guild_id && source.source_name.equals(name)) {
                return source;
            }
        }
        return null;
    }

    public Set<EmbeddingSource> getSources(Predicate<Long> guildPredicateOrNull, Predicate<EmbeddingSource> sourcePredicate) {
        Set<EmbeddingSource> result = new LinkedHashSet<>();
        for (EmbeddingSource source : embeddingSources.values()) {
            if (guildPredicateOrNull == null || guildPredicateOrNull.test(source.guild_id)) {
                if (sourcePredicate == null || sourcePredicate.test(source)) {
                    result.add(source);
                }
            }
        }
        return result;
    }

    @Override
    public float[] getEmbedding(EmbeddingSource source, String text, ThrowingConsumer<String> moderate) {
        long hash = getHash(text);
        return getOrCreateEmbedding(hash, text, null, source, true, moderate);
    }

    @Override
    public List<EmbeddingInfo> getClosest(EmbeddingSource inputSource, String input, int top, Set<EmbeddingSource> allowedTypes, BiPredicate<EmbeddingSource, Long> sourceHashPredicate, ThrowingConsumer<String> moderate) {
        checkArgument(top > 0, "top must be > 0");
        PriorityQueue<EmbeddingInfo> largest = new PriorityQueue<>(top, new Comparator<EmbeddingInfo>() {
            @Override
            public int compare(EmbeddingInfo o1, EmbeddingInfo o2) {
                return Double.compare(o1.distance, o2.distance);
            }
        });

        float[] compareTo = getEmbedding(inputSource, input, moderate);

        for (EmbeddingSource source : allowedTypes) {
            Set<Long> hashes = textHashBySource.get(source.source_id);
            if (hashes == null || hashes.isEmpty()) {
                System.out.println("No hashes for " + source.source_name + " | " + source.source_id);
                continue;
            }
            for (long hash : hashes) {
                if (!sourceHashPredicate.test(source, hash)) continue;
                float[] vector = vectors.get(hash);
                double diff = ArrayUtil.cosineSimilarity(vector, compareTo);

                if (largest.size() < top || largest.peek().distance < diff) {
                    if (largest.size() == top)
                        largest.remove();
                    EmbeddingInfo result = new EmbeddingInfo(hash, vector, source, diff);
                    largest.add(result);
                }
            }
        }

        ObjectArrayList<EmbeddingInfo> list = new ObjectArrayList<>(largest.size());
        // poll and add
        while (!largest.isEmpty()) {
            list.add(largest.poll());
        }
        // reverse
        Collections.reverse(list);
        return list;
    }
}

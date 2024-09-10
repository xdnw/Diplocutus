package link.locutus.discord.api.endpoints;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import link.locutus.discord.Locutus;
import link.locutus.discord.api.ApiKeyPool;
import link.locutus.discord.commands.manager.v2.binding.bindings.PlaceholderCache;
import link.locutus.discord.util.FileUtil;
import link.locutus.discord.util.IOUtil;
import link.locutus.discord.util.io.PagePriority;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DnsQuery<T> {
    private final Class<T> clazz;
    private final String url;
    private final Map<String, String> params;
    private final String endpoint;
    private final ObjectMapper mapper;
    private final int cost;
    private final ApiKeyPool pool;
    private boolean publicQuery = false;

    public DnsQuery(ObjectMapper mapper, Class<T> clazz, String url, String endpoint, ApiKeyPool pool, int cost) {
        this.mapper = mapper;
        this.clazz = clazz;
        this.url = url;
        this.endpoint = endpoint;
        this.params = new LinkedHashMap<>();
        this.cost = cost;
        this.pool = pool;
    }

    public DnsQuery<T> setPublic() {
        this.publicQuery = true;
        return this;
    }

    public boolean isPublic() {
        return publicQuery;
    }

    public int getCost() {
        return cost;
    }

    public Class<T> getType() {
        return clazz;
    }

    public String getUrl() {
        return url;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public DnsQuery<T> add(String key, Number value) {
        this.params.put(key, value.toString());
        return this;
    }

    public DnsQuery<T> add(String key, String value) {
        this.params.put(key, value);
        return this;
    }

    public String getUrl(String apiKey) {
        String url = this.url + this.endpoint + "?APICode=" + apiKey;
        for (Map.Entry<String, String> entry : this.params.entrySet()) {
            url += "&" + entry.getKey() + "=" + entry.getValue();
        }
        return url;
    }

    public List<T> call() {
//        if (true) {
//            // testing
//            String filePathInResources = "mock/" + endpoint + ".json";
//            try {
//                System.out.println("Fetch API (mock) " + filePathInResources);
//                URL url = Resources.getResource(filePathInResources);
//                String text = Resources.toString(url, StandardCharsets.UTF_8);
//                if (text == null) {
//                    throw new RuntimeException("Failed to read file: " + filePathInResources);
//                }
//                System.out.println("Read file: " + filePathInResources);
//                return mapper.readerForListOf(clazz).readValue(text);
//            } catch (IOException e) {
//                e.printStackTrace();
//                throw new RuntimeException(e);
//            }
//        }
        ApiKeyPool.ApiKey next = pool.getNextApiKey();
        String url = getUrl(next.getKey());
        try {
            String body = FileUtil.readStringFromURL(PagePriority.ACTIVE_PAGE, url);
            List<T> result = mapper.readerForListOf(clazz).readValue(body);
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            rethrow(e, next, true);
            return null;
        }
    }

    private void rethrow(Throwable e, ApiKeyPool.ApiKey pair, boolean throwRuntime) {
        String msg = e.getMessage();
        msg = msg.replaceAll("(?i)[\\[\\]\"\\n^:\\s,\\.](?=.*[A-Za-z])(?=.*\\d)[0-9A-F]{14,}(?=[\\[\\]\"\\n$:\\s,\\.]|$)", "XXX");
        if (e.getMessage() != null &&
                (StringUtils.containsIgnoreCase(e.getMessage(), pair.getKey()))) {
            msg = StringUtils.replaceIgnoreCase(e.getMessage(), pair.getKey(), "XXX");
            throwRuntime = true;
        }
        if (msg == null) msg = "";
        if (pair.getKey() != null) {
            msg = msg + " (using key from: " + pair.getNationId() + ")";
        }
        if (throwRuntime) throw new RuntimeException(msg);
        if (e instanceof HttpClientErrorException.Unauthorized unauthorized) {
            throw HttpClientErrorException.create(msg, unauthorized.getStatusCode(), unauthorized.getStatusText(), unauthorized.getResponseHeaders(), unauthorized.getResponseBodyAsByteArray(), /* charset utf-8 */ StandardCharsets.UTF_8);
        }
    }
}

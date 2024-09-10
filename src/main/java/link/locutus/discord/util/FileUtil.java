package link.locutus.discord.util;

import link.locutus.discord.Logg;
import link.locutus.discord.config.Settings;
import link.locutus.discord.util.io.PagePriority;
import link.locutus.discord.util.io.PageRequestQueue;
import link.locutus.discord.util.offshore.Auth;
import org.eclipse.jetty.util.UrlEncoded;
import org.springframework.web.client.HttpClientErrorException;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class FileUtil {
    private static final String COOKIES_HEADER = "Set-Cookie";

    public static String readFile(String name) {
        try (InputStream resource = FileUtil.class.getResourceAsStream(name)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = resource.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
            byte[] bytes = buffer.toByteArray();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<String> getResourceFiles(String path) throws IOException {
        List<String> filenames = new ArrayList<>();

        try (
                InputStream in = getResourceAsStream(path);
                BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String resource;

            while ((resource = br.readLine()) != null) {
                filenames.add(resource);
            }
        }

        return filenames;
    }

    public static InputStream getResourceAsStream(String resource) {
        final InputStream in
                = getContextClassLoader().getResourceAsStream(resource);

        return in == null ? FileUtil.class.getResourceAsStream(resource) : in;
    }

    public static ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    public static <T> T submit(int priority, int maxBuffer, int maxDelay, Supplier<T> task, String url) {
        return get(pageRequestQueue.submit(task, getPriority(priority), maxBuffer, maxDelay, url));
    }

    public static <T> T submit(int priority, int maxBuffer, int maxDelay, Supplier<T> task, URI url) {
        return get(pageRequestQueue.submit(task, getPriority(priority), maxBuffer, maxDelay, url));
    }

    public static PageRequestQueue getPageRequestQueue() {
        return pageRequestQueue;
    }

    public static byte[] readBytesFromUrl(PagePriority priority, String urlStr) {
        return submit(priority.ordinal(), priority.getAllowedBufferingMs(), priority.getAllowableDelayMs(), () -> {
            try (InputStream is = new URL(urlStr).openStream()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] byteChunk = new byte[4096]; // Or whatever size you want to read in at a time.
                int n;

                while ( (n = is.read(byteChunk)) > 0 ) {
                    baos.write(byteChunk, 0, n);
                }
                is.close();
                return baos.toByteArray();
            }
            catch (IOException e) {
                e.printStackTrace ();
                return null;
            }
        }, urlStr);
    }

    private static void check409Error(URLConnection connection) throws IOException {
        if (connection instanceof HttpURLConnection http) {
            if (http.getResponseCode() == 429) {
                Integer retryAfter = null;
                String retry = http.getHeaderField("Retry-After");
                if (retry != null && MathMan.isInteger(retry)) {
                    retryAfter = Integer.parseInt(retry);
                }
                throw new TooManyRequests("Too many requests", retryAfter);
            }
        }
    }

    public static class TooManyRequests extends RuntimeException {
        private final Integer retryAfter;

        public TooManyRequests(String message, Integer retryAfter) {
            super(message);
            this.retryAfter = retryAfter;
        }

        public Integer getRetryAfter() {
            return retryAfter;
        }
    }

    public static String readStringFromURL(PagePriority priority, String requestURL) throws IOException {
        return readStringFromURL(priority.ordinal(), priority.getAllowedBufferingMs(), priority.getAllowableDelayMs(), requestURL);
    }

    public static String readStringFromURL(int priority, int maxBuffer, int maxDelay, String requestURL) throws IOException {
        return submit(priority, maxBuffer, maxDelay, () -> {
                try {
                    URL website = new URL(requestURL);
                    URLConnection connection = website.openConnection();
                    try {
                        try (BufferedReader in = new BufferedReader(
                                new InputStreamReader(connection.getInputStream()))) {

                            StringBuilder response = new StringBuilder();
                            String inputLine;

                            while ((inputLine = in.readLine()) != null) {
                                response.append(inputLine);
                            }
                            return response.toString();
                        }
                    } catch (IOException e) {
                        check409Error(connection);
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            },
            requestURL
        );
    }

    public static String encode(String url) throws UnsupportedEncodingException {
        String[] split = url.split("\\?", 2);
        if (split.length == 1) return url;
        return split[0] + "?" + UrlEncoded.encodeString(split[1], StandardCharsets.UTF_8);
    }

    public static CompletableFuture<String> readStringFromURL(PagePriority priority, String urlStr, Map<String, String> arguments) throws IOException {
        return readStringFromURL(priority, urlStr, arguments, null);
    }

    public static CompletableFuture<String> readStringFromURL(PagePriority priority, String urlStr, Map<String, String> arguments, CookieManager msCookieManager) throws IOException {
        return readStringFromURL(priority, urlStr, arguments, true, msCookieManager, i -> {});
    }

    public static CompletableFuture<String> readStringFromURL(PagePriority priority, String urlStr, Map<String, String> arguments, boolean post, CookieManager msCookieManager, Consumer<HttpURLConnection> apply) throws IOException {
        byte[] out = serialize(arguments);
        return readStringFromURL(priority, urlStr, out, post ? RequestType.POST : RequestType.GET, msCookieManager, apply);
    }

    public static byte[] serialize(Map<String, String> arguments) {
        StringJoiner sj = new StringJoiner("&");
        for (Map.Entry<String, String> entry : arguments.entrySet())
            sj.add(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
                    + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        return sj.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static <T> T get(Future<T> myFuture) {
        try {
            return myFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause != null && cause instanceof RuntimeException run) throw run;
            throw new RuntimeException(e);
        }
    }

    public enum RequestType {
        GET,
        POST,
        PUT,
    }

    private static PageRequestQueue pageRequestQueue = new PageRequestQueue(8);
    private static AtomicInteger requestOrder = new AtomicInteger();

    private static long getPriority(int priority) {
        return requestOrder.incrementAndGet() + Integer.MAX_VALUE * (long) priority;
    }

    public static CompletableFuture<String> readStringFromURL(PagePriority priority, String urlStr, byte[] dataBinary, RequestType type, CookieManager msCookieManager, Consumer<HttpURLConnection> apply) {
        Supplier<String> fetch = new Supplier<String>() {
            @Override
            public String get() {
//                System.out.println("Requesting " + urlStr + " at " + now + " with priority " + priority + " ( last: " + (now - lastRead) + " ). Queue size: " + pageRequestQueue.size());
                try {
                    URL url = new URL(urlStr);
                    URLConnection con = url.openConnection();
                    HttpURLConnection http = (HttpURLConnection) con;

                    if (msCookieManager != null && msCookieManager.getCookieStore().getCookies().size() > 0) {
                        for (HttpCookie cookie : msCookieManager.getCookieStore().getCookies()) {
                            http.addRequestProperty("Cookie", cookie.toString());
                        }
                        // While joining the Cookies, use ',' or ';' as needed. Most of the servers are using ';'
//            http.setRequestProperty("Cookie",
//                    StringMan.join(msCookieManager.getCookieStore().getCookies(), ";"));
                    }

                    http.setUseCaches(false);
                    http.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
                    http.setRequestProperty("dnt", "1");
                    http.setRequestProperty("Connection", "keep-alive");
                    http.setRequestProperty("Referer", urlStr);
                    http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                    http.setRequestProperty("User-Agent", Settings.USER_AGENT);
                    if (dataBinary != null && dataBinary.length != 0 && (type == RequestType.POST || type == RequestType.PUT)) {
                        http.setRequestMethod(type.name());
                    } else if ((type != RequestType.POST && type != RequestType.PUT) && dataBinary == null) {
                        http.setRequestMethod("GET");
                    }
                    http.setDoOutput(true);
                    http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                    int length = dataBinary != null ? dataBinary.length : 0;
                    http.setFixedLengthStreamingMode(length);
                    http.setInstanceFollowRedirects(false);

                    if (apply != null) apply.accept(http);

                    http.connect();
                    if (dataBinary != null) {
                        try (OutputStream os = http.getOutputStream()) {
                            os.write(dataBinary);
                        }
                    }

                    try (InputStream is = http.getInputStream()) {
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                        int nRead;
                        byte[] data = new byte[8192];
                        while ((nRead = is.read(data, 0, data.length)) != -1) {
                            buffer.write(data, 0, nRead);
                        }

                        buffer.flush();
                        byte[] bytes = buffer.toByteArray();

                        Map<String, List<String>> headerFields = http.getHeaderFields();
                        List<String> cookiesHeader = headerFields.get(COOKIES_HEADER);
                        if (cookiesHeader != null && msCookieManager != null) {
                            for (String cookie : cookiesHeader) {
                                msCookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
                            }
                        }


                        return new String(bytes, StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        check409Error(http);
                        try (InputStream is = http.getErrorStream()) {
                            if (is != null) {
                                String error = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                                throw new IOException(StringMan.stripApiKey(e.getMessage()) + ":\n" + error);
                            }
                        }
                        Logg.text("Error Reading " + urlStr + "\n " + StringMan.stacktraceToString(e));
                        throw new RuntimeException(StringMan.stripApiKey(e.getMessage()));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(StringMan.stripApiKey(e.getMessage()));
                }
            }
        };
        PageRequestQueue.PageRequestTask<String> task = pageRequestQueue.submit(new Supplier<String>() {
            @Override
            public String get() {
                int backoff = 4000;
                while (true) {
                    try {
                        String result = fetch.get();
                        return result;
                    } catch (RuntimeException e) {
                        Throwable cause = e;
                        while (cause.getCause() != null && cause != cause.getCause()) {
                            cause = cause.getCause();
                        }
                        if (e.getMessage() != null && e.getMessage().contains("Server returned HTTP response code: 429")) {
                            try {
                                Thread.sleep(backoff);
                                backoff += 4000;
                            } catch (InterruptedException ex) {
                                throw new RuntimeException(ex);
                            }
                            continue;
                        }
                        String stripped = StringMan.stripApiKey(e.getMessage());
                        if (!Objects.equals(e.getMessage(), stripped)) {
                            throw new RuntimeException(stripped);
                        }
                        throw e;
                    }
                }
            }
        }, getPriority(priority.ordinal()), priority.getAllowedBufferingMs(), priority.getAllowableDelayMs(), urlStr);
        return task;
    }
}

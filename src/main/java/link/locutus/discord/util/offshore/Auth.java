package link.locutus.discord.util.offshore;

import link.locutus.discord.Locutus;
import link.locutus.discord.api.ApiKeyPool;
import link.locutus.discord.config.Settings;
import link.locutus.discord.db.entities.DBNation;
import link.locutus.discord.util.FileUtil;
import link.locutus.discord.util.DNS;
import link.locutus.discord.util.io.PagePriority;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.CookieManager;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class Auth {
    private final String password;
    private final String username;
    private boolean valid;

    private final int nationId;

    public Auth(int nationId, String username, String password) {
        this.username = username;
        this.password = password;
        this.nationId = nationId;
        this.valid = true;
    }

    private boolean loggedIn = false;

    public void login(boolean force) throws IOException {
        if (!force && loggedIn) return;

        synchronized (this)
        {
            throw new UnsupportedOperationException("Login not implemented");
//            Map<String, String> userPass = new HashMap<>();
//            userPass.put("email", this.getUsername());
//            userPass.put("password", this.getPassword());
//            userPass.put("loginform", "Login");
//            userPass.put("rememberme", "1");
//            String url = "" + Settings.INSTANCE.DNS_URL() + "/login/";
//
//            String loginResult = FileUtil.get(FileUtil.readStringFromURL(PagePriority.LOGIN, url, userPass, this.getCookieManager()));
//            if (!loginResult.contains("Login Successful")) {
//                throw new IllegalArgumentException("Error: " + PW.parseDom(Jsoup.parse(loginResult), "columnheader"));
//            }
//            loggedIn = true;
        }
    }

    public int getNationId() {
        return nationId;
    }

    public DBNation getNation() {
        return Locutus.imp().getNationDB().getNation(nationId);
    }

    public int getAllianceId() {
        DBNation n = getNation();
        return n == null ? 0 : n.getAlliance_id();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public boolean isValid() {
        return valid;
    }

    public ApiKeyPool.ApiKey fetchApiKey() {
        throw new UnsupportedOperationException("TODO FIXME :||remove not implemented yet");
    }
}

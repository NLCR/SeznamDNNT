package cz.inovatika.sdnnt.services.impl.users;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.User;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;

/** Persistent-login tokens. The browser receives the secret; Solr stores only its hash. */
public class RememberMeService {

    static final String COOKIE_NAME = "sdnnt_remember";
    static final String TOKEN_HASH_FIELD = "rememberTokenHash";
    static final String TOKEN_EXPIRATION_FIELD = "rememberTokenExpiration";
    static final String KEEP_LOGGED_IN_FIELD = "rememberKeepLoggedIn";
    private static final int DEFAULT_EXPIRATION_DAYS = 30;
    private static final SecureRandom RANDOM = new SecureRandom();

    public void issue(HttpServletRequest request, HttpServletResponse response, String username,
                      boolean keepLoggedIn) throws Exception {
        String token = newToken();
        Date expiration = new Date(System.currentTimeMillis() + Duration.ofDays(expirationDays()).toMillis());
        updateUser(username, hash(token), expiration, keepLoggedIn);
        setCookie(request, response, token, (int) Duration.ofDays(expirationDays()).getSeconds());
    }

    /** Validates and rotates a token, returning null when it is missing, invalid, or expired. */
    public User consume(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String token = readCookie(request);
        if (token == null) return null;

        try (SolrClient solr = client()) {
            SolrQuery query = new SolrQuery(TOKEN_HASH_FIELD + ":\"" + hash(token) + "\"").setRows(1);
            QueryResponse result = solr.query(DataCollections.users.name(), query);
            if (result.getResults().isEmpty()) {
                clearCookie(request, response);
                return null;
            }
            SolrDocument document = result.getResults().get(0);
            Date expiration = (Date) document.getFieldValue(TOKEN_EXPIRATION_FIELD);
            if (expiration == null || !expiration.after(new Date())) {
                revoke((String) document.getFieldValue(User.USERNAME_KEY));
                clearCookie(request, response);
                return null;
            }
            User user = User.fromSolrDocument(document);
            boolean keepLoggedIn = Boolean.TRUE.equals(document.getFieldValue(KEEP_LOGGED_IN_FIELD));
            request.getSession(true).setAttribute(cz.inovatika.sdnnt.tracking.TrackingFilter.KEEP_LOGGED_IN,
                    keepLoggedIn);
            issue(request, response, user.getUsername(), keepLoggedIn);
            return user;
        }
    }

    public void revoke(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String token = readCookie(request);
        if (token != null) {
            try (SolrClient solr = client()) {
                SolrQuery query = new SolrQuery(TOKEN_HASH_FIELD + ":\"" + hash(token) + "\"").setRows(1);
                QueryResponse result = solr.query(DataCollections.users.name(), query);
                if (!result.getResults().isEmpty()) {
                    revoke((String) result.getResults().get(0).getFieldValue(User.USERNAME_KEY));
                }
            }
        }
        clearCookie(request, response);
    }

    private void revoke(String username) throws Exception {
        updateUser(username, null, null, false);
    }

    private void updateUser(String username, String hash, Date expiration, boolean keepLoggedIn) throws Exception {
        try (SolrClient solr = client()) {
            SolrInputDocument update = new SolrInputDocument();
            update.addField(User.USERNAME_KEY, username);
            update.addField(TOKEN_HASH_FIELD, Collections.singletonMap("set", hash));
            update.addField(TOKEN_EXPIRATION_FIELD, Collections.singletonMap("set", expiration));
            update.addField(KEEP_LOGGED_IN_FIELD,
                    Collections.singletonMap("set", hash == null ? null : keepLoggedIn));
            solr.add(DataCollections.users.name(), update);
            solr.commit(DataCollections.users.name());
        }
    }

    private SolrClient client() {
        return new HttpSolrClient.Builder(Options.getInstance().getString("solr.host")).build();
    }

    private int expirationDays() {
        return Options.getInstance().getInt("rememberMeExpirationDays", DEFAULT_EXPIRATION_DAYS);
    }

    static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        return DigestUtils.sha256Hex(token);
    }

    private String readCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private void setCookie(HttpServletRequest request, HttpServletResponse response, String value, int maxAge) {
        StringBuilder cookie = new StringBuilder(COOKIE_NAME).append('=').append(value)
                .append("; Path=").append(cookiePath(request))
                .append("; Max-Age=").append(maxAge)
                .append("; HttpOnly; SameSite=Lax");
        if (request.isSecure()) cookie.append("; Secure");
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearCookie(HttpServletRequest request, HttpServletResponse response) {
        setCookie(request, response, "", 0);
    }

    private String cookiePath(HttpServletRequest request) {
        String context = request.getContextPath();
        return context == null || context.isEmpty() ? "/" : context + "/";
    }
}

package cz.inovatika.sdnnt.utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

public class SimpleGET {
    
    public static final Logger LOGGER = Logger.getLogger(SimpleGET.class.getName());
    
    public static final int MAXIMUM_ITERATION = 20;

    public static String getFinalURL(String url, int counter) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        try {
            con.setInstanceFollowRedirects(false);
            con.connect();

            if (con.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM || con.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
                String redirectUrl = con.getHeaderField("Location");
                if (counter > MAXIMUM_ITERATION) return redirectUrl;
                return getFinalURL(redirectUrl, counter++);
            }
            return url;
        } finally {
            if (con != null) con.disconnect();
        }
    }

    public static String get(String u) throws IOException {
        URL url = new URL( getFinalURL(u,0) );
        LOGGER.fine("Requesting url :"+url.toString());
        HttpURLConnection conn= (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects( true);
        conn.setRequestProperty("Content-Type", "application/json");
        int responseCode = conn.getResponseCode();

        String s = "";
        try(InputStream is = conn.getInputStream()) {
            s = IOUtils.toString(is, "UTF-8");
        }
        return s;
    }
}

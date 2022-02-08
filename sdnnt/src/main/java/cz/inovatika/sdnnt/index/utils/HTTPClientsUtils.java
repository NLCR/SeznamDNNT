package cz.inovatika.sdnnt.index.utils;

import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HTTPClientsUtils {

    public static final Logger LOGGER = Logger.getLogger(HTTPClientsUtils.class.getName());

    private HTTPClientsUtils() {}

    public static final void quiteClose(CloseableHttpClient client) {
        try {
            client.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,e.getMessage(),e);
        }
    }
}

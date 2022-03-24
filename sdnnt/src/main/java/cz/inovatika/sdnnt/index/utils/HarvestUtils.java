package cz.inovatika.sdnnt.index.utils;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.OAIHarvester;
import cz.inovatika.sdnnt.index.exceptions.MaximumIterationExceedException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;

import java.io.*;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Harvesting utility class
 * @author happy
 */
public class HarvestUtils {

    public static final Logger LOGGER = Logger.getLogger(HarvestUtils.class.getName());

    private HarvestUtils() {}

    /** 
     * Creating debug files for harvest
     * @param type type of harvest
     * @param netStream Data stream
     * @param url Requesting url
     * @return
     */
    public static File debugFile(String type, InputStream netStream, String url) {
        try {
            String harvestFiles = System.getProperty("user.home") + File.separator + ".sdnnt" + File.separator + "harvest" + File.separator + type + "_" + Thread.currentThread().getName();

            File directory = new File(harvestFiles);
            directory.mkdirs();

            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] digest1 = digest.digest(url.getBytes("UTF-8"));

            String s = Base64.getEncoder().encodeToString(digest1);

            s = s.replaceAll("/", "div");
            s = s.replaceAll("\\\\", "slash");
            s = s.replaceAll("\\?", "qm");
            s = s.replaceAll("\\.", "dot");
            s = s.replaceAll("=", "eq");
            s = s.replaceAll("\\+", "pl");

            String prefix = s.substring(0, 2);
            String postfix = s.substring(2);

            // creating structure
            File prefixFolder = new File(directory, prefix);
            prefixFolder.mkdirs();


            File targetFile = new File(prefixFolder, postfix);
            java.nio.file.Files.copy(netStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            LOGGER.info(String.format("URL %s, file %s", url, targetFile.getAbsolutePath()));
            return targetFile;
            //return new FileInputStream(targetFile);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Throttling support. The method tries to download data stream, and if it is not possible, 
     *  waits configured number of iterations.
     *   
     * Throttling situations:
     * <ul>
     *  <li>NotReady html page - information from Aleph </li>
     *  <li>TimeoutException</li>
     * </ul>
     * @param client
     * @param type
     * @param url
     * @return
     * @throws IOException
     * @throws MaximumIterationExceedException
     */
    public static File throttle(CloseableHttpClient client, String type, String url) throws IOException, MaximumIterationExceedException {
        int max_repetion = 3;
        int seconds = 5;

        JSONObject harvest = Options.getInstance().getJSONObject("OAIHavest");
        if (harvest.has("numrepeat")) {
            max_repetion = harvest.getInt("numrepeat");
        }
        if (harvest.has("seconds")) {
            seconds = harvest.getInt("seconds");
        }
        for (int i = 0; i < max_repetion; i++) {
            LOGGER.log(Level.FINE, "Throttling url: "+url+" iteration:"+i);
            HttpGet httpGet = new HttpGet(url);
            try (CloseableHttpResponse response1 = client.execute(httpGet)) {
                final HttpEntity entity = response1.getEntity();
                if (entity != null) {
                    File dFile = null;
                    try (InputStream netStream = entity.getContent()) {
                        dFile = debugFile(type, netStream, url);
                        if (checkNotHTML(dFile)) {
                            return dFile;
                        } else {
                            wait(seconds);
                        }
                    }
                }
            } catch (ConnectTimeoutException | ConnectException | SocketTimeoutException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(),ex);
                wait(seconds);
            }
        }
        throw new MaximumIterationExceedException("Maximum number of waiting exceeed");
    }

    private static void wait(int seconds) {
        try {
            OAIHarvester.LOGGER.log(Level.INFO, "Suspending threads for " + seconds + "seconds ");
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            OAIHarvester.LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public static boolean checkNotHTML(File dfile) throws IOException {
        // read first
        try (FileInputStream fis = new FileInputStream(dfile)) {
            byte[] buffer = new byte[2048];
            int read = IOUtils.read(fis, buffer);
            if (read > 0) {
                String s = new String(buffer, "UTF-8");
                return !s.toLowerCase().contains("<html>");
            } else return false;
        }
    }
    
    
}

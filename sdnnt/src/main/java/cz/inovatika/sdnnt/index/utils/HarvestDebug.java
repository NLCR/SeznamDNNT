package cz.inovatika.sdnnt.index.utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.*;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Logger;

public class HarvestDebug {

    public static final Logger LOGGER = Logger.getLogger(HarvestDebug.class.getName());

    private HarvestDebug()  {}

    public static File debugFile(String type, InputStream netStream, String url) {
        try {
            String harvestFiles = System.getProperty("user.home") + File.separator+".sdnnt"+File.separator+"harvest"+File.separator+type+"_"+Thread.currentThread().getName();

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

}

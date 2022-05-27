package cz.inovatika.sdnnt;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Alberto Hernandez
 */
public class Options {

  public static final Logger LOGGER = Logger.getLogger(Options.class.getName());

  private static Options _sharedInstance = null;
  private final JSONObject client_conf;
  private final JSONObject server_conf;

  public synchronized static Options getInstance() {
    if (_sharedInstance == null) {
      try {
        _sharedInstance = new Options();
      } catch (IOException | JSONException ex) {
        Logger.getLogger(Options.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    return _sharedInstance;
  }

  public synchronized static void resetInstance() {
    _sharedInstance = null;
    LOGGER.log(Level.INFO, "Options reseted");
  }

  public Options() throws IOException, JSONException {
    // v servletu prenastaveno
    File fdef = new File(InitServlet.DEFAULT_CONFIG_FILE);

    //TODO: ?? reading embedded conf ? Read from classloader
    if (fdef.exists()) {
      client_conf = new JSONObject(FileUtils.readFileToString(fdef, "UTF-8"));
    } else {
      client_conf = new JSONObject();
    }
    
    String path = config();

    File fserver = FileUtils.toFile(Options.class.getResource("config.json"));
    String sjson = FileUtils.readFileToString(fserver, "UTF-8");
    server_conf = new JSONObject(sjson);


    //Merge options defined in custom dir
    File f = new File(path);
    LOGGER.info(String.format("Loading configuration files client(%s), server(%s), custom(%s) ", fdef.getAbsolutePath(), fserver.getAbsolutePath(), f.getAbsolutePath()));

    if (f.exists() && f.canRead()) {
      String json = FileUtils.readFileToString(f, "UTF-8");
      JSONObject customClientConf = new JSONObject(json).getJSONObject("client");
      if (customClientConf != null) {
        Iterator keys = customClientConf.keys();
        while (keys.hasNext()) {
          String key = (String) keys.next();
          LOGGER.log(Level.FINE, "key {0} will be overrided", key);
          client_conf.put(key, customClientConf.get(key));
        }
      } else {
          LOGGER.log(Level.SEVERE, String.format("Cannot read from file %s", f.getAbsoluteFile()));
      }

      JSONObject customServerConf = new JSONObject(json).getJSONObject("server");
      if (customServerConf != null) {
        Iterator keys2 = customServerConf.keys();
        while (keys2.hasNext()) {
          String key = (String) keys2.next();
          LOGGER.log(Level.FINE, "key {0} will be overrided", key);
          server_conf.put(key, customServerConf.get(key));
        }
      }
    }
    LOGGER.info("Loaded configuration :"+this.toString());
  }

  // 
  protected String config() {
      return System.getProperty("user.home")+File.separator+".sdnnt"+File.separator+"config.json";
  }

  public JSONObject getClientConf() {
    return client_conf;
  }

  public String getString(String key, String defVal) {
    return server_conf.optString(key, defVal);
  }

  public String getString(String key) {
    return server_conf.optString(key);
  }

  public int getInt(String key, int defVal) {
    return server_conf.optInt(key, defVal);
  }

  public boolean getBoolean(String key, boolean defVal) {
    return server_conf.optBoolean(key, defVal);
  }

  public String[] getStrings(String key) {
    JSONArray arr = server_conf.optJSONArray(key);
    String[] ret = new String[arr.length()];
    for (int i = 0; i < arr.length(); i++) {
      ret[i] = arr.getString(i);
    }
    return ret;
  }

  public JSONArray getJSONArray(String key) {
    return server_conf.optJSONArray(key);
  }

  public JSONObject getJSONObject(String key) {
    return server_conf.optJSONObject(key);
  }

    @Override
    public String toString() {
        JSONObject json = new JSONObject();
        if (this.client_conf != null) {
            json.put("client", this.client_conf);
        }
        if (this.server_conf != null) {
            json.put("server", this.server_conf);
        }
        return json.toString(2);
    }
  
}

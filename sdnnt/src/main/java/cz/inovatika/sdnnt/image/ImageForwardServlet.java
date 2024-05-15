package cz.inovatika.sdnnt.image;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.hadoop.hdfs.util.ByteArrayManager.Conf;
import org.json.JSONObject;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.services.kraminstances.CheckKrameriusConfiguration;
import cz.inovatika.sdnnt.services.kraminstances.InstanceConfiguration;
import cz.inovatika.sdnnt.services.kraminstances.InstanceConfiguration.KramVersion;
import cz.inovatika.sdnnt.utils.PIDUtils;


@WebServlet(value = "/image")
public class ImageForwardServlet extends HttpServlet {
    
    public static final Logger LOGGER = Logger.getLogger(ImageForwardServlet.class.getName());

    public static final String KRAM_URL_KEY = "kramurl";
    
    private void responseHeader(HttpServletResponse response) {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0
        response.setDateHeader("Expires", 0); // Proxies.
    }

    

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        responseHeader(resp);
        try {
            JSONObject checkKamerius = Options.getInstance().getJSONObject("check_kramerius");
            CheckKrameriusConfiguration confs = CheckKrameriusConfiguration.initConfiguration(checkKamerius);
            
            String kramUrl = req.getParameter(KRAM_URL_KEY);
            String pid = PIDUtils.pid(kramUrl);
            if (pid != null) {
                InstanceConfiguration instancConf = confs.match(kramUrl);
                if (instancConf != null) {
                    KramVersion version = instancConf.getVersion();
                    String apiPoint = instancConf.getApiPoint();
                    switch(version) {
                        case V5: 
                            resp.sendRedirect(apiPoint+(apiPoint.endsWith("/") ? "": "/")+"api/v5.0/item/"+pid+"/thumb");
                            return;
                        case V7:
                            resp.sendRedirect(apiPoint+(apiPoint.endsWith("/") ? "": "/")+"api/client/v7.0/items/"+pid+"/image/thumb");
                            return;
                    }
                }
            }
            
        } catch (IOException e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e1.toString());
        } catch (SecurityException e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } catch (Exception e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e1.toString());
        }
    }

    /*
     *         JSONObject checkKamerius = getOptions().getJSONObject("check_kramerius");
        this.checkConf = CheckKrameriusConfiguration.initConfiguration(checkKamerius);

     */
    enum Actions {
    
        REDIRECT_V7 {

            @Override
            public void doPerform(HttpServletRequest req, HttpServletResponse resp) {
                // TODO Auto-generated method stub
                
            }

        };
        
        public abstract void doPerform(HttpServletRequest req, HttpServletResponse resp);
    }
}

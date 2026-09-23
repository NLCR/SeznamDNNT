
package cz.inovatika.sdnnt;

import cz.inovatika.sdnnt.model.User;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.NotificationsService;
import cz.inovatika.sdnnt.services.exceptions.NotificationsException;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;
import cz.inovatika.sdnnt.services.impl.DefaultApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.impl.NotificationServiceImpl;
import cz.inovatika.sdnnt.services.impl.users.UserControlerImpl;
import cz.inovatika.sdnnt.services.impl.users.UsersUtils;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
public class ConfigServlet extends HttpServlet {

  public static final Logger LOGGER = Logger.getLogger(ConfigServlet.class.getName());

  /**
   * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
   * methods.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  protected void processRequest(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    try {

      response.setContentType("application/json;charset=UTF-8");
      
      if (request.getParameter("reset") != null){
        Options.resetInstance();
      }
      PrintWriter out = response.getWriter();
      Options options = Options.getInstance();
      JSONObject js = new JSONObject(options.getClientConf().toString());
      JSONObject krameriusConfig = publicKrameriusConfig(options.getJSONObject("check_kramerius"));
      js.put("kramerius_libraries", krameriusConfig.getJSONObject("libraries"));
      js.put("enabled_kramerius_libraries", krameriusConfig.getJSONArray("enabled"));
      
      ApplicationUserLoginSupport appLoginController = new DefaultApplicationUserLoginSupport(request);
      //UserControler userController = new UserControlerImpl(request);
      User user = appLoginController.getUser();
      if (user != null) {
          NotificationsService service = new NotificationServiceImpl( new UserControlerImpl(request), null);

          try {
              JSONObject userObject = UsersUtils.prepareUserLoggedObject(new UserControlerImpl(request), service, user);
            js.put("user", userObject);

          } catch (UserControlerException |NotificationsException   e) {
            LOGGER.log(Level.SEVERE,e.getMessage(),e);
        }
          
      }

      out.print(js.toString());
    } catch (IOException | JSONException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    } 
  }

  private JSONObject publicKrameriusConfig(JSONObject checkKramerius) {
    JSONObject result = new JSONObject();
    JSONObject libraries = new JSONObject();
    JSONArray enabled = new JSONArray();

    if (checkKramerius == null) {
      result.put("libraries", libraries);
      result.put("enabled", enabled);
      return result;
    }

    JSONObject urls = checkKramerius.optJSONObject("urls");
    JSONObject instances = urls != null ? urls : checkKramerius;
    Iterator<String> keys = instances.keys();
    while (keys.hasNext()) {
      JSONObject item = instances.optJSONObject(keys.next());
      if (item == null) {
        continue;
      }

      JSONArray facetKeys = facetKeys(item);
      for (int i = 0; i < facetKeys.length(); i++) {
        String facetKey = facetKeys.optString(i, "");
        if (!isAnyString(facetKey)) {
          continue;
        }

        boolean skip = item.optBoolean("skip", false);
        JSONObject existing = libraries.optJSONObject(facetKey);
        if (existing != null && (!existing.optBoolean("skip", false) || skip)) {
          continue;
        }

        JSONObject library = new JSONObject();
        library.put("description", item.optString("description", ""));
        library.put("acronym", item.optString("acronym", ""));
        library.put("sigla", item.optString("sigla", ""));
        library.put("skip", skip);
        libraries.put(facetKey, library);

        if (!skip && !contains(enabled, facetKey)) {
          enabled.put(facetKey);
        }
      }
    }

    result.put("libraries", libraries);
    result.put("enabled", enabled);
    return result;
  }

  private JSONArray facetKeys(JSONObject item) {
    JSONArray facetKeys = new JSONArray();
    String sigla = item.optString("sigla", "");
    if (isAnyString(sigla)) {
      facetKeys.put(sigla);
    }

    JSONArray additionalSigla = item.optJSONArray("additional_sigla");
    if (additionalSigla != null) {
      for (int i = 0; i < additionalSigla.length(); i++) {
        String siglaItem = additionalSigla.optString(i, "");
        if (isAnyString(siglaItem)) {
          facetKeys.put(siglaItem);
        }
      }
    }

    String acronym = item.optString("acronym", "");
    if (facetKeys.length() == 0 && isAnyString(acronym)) {
      facetKeys.put(acronym.toUpperCase());
    }
    return facetKeys;
  }

  private boolean isAnyString(String value) {
    return value != null && value.trim().length() > 0;
  }

  private boolean contains(JSONArray array, String value) {
    for (int i = 0; i < array.length(); i++) {
      if (value.equals(array.optString(i, ""))) {
        return true;
      }
    }
    return false;
  }

  // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
  /**
   * Handles the HTTP <code>GET</code> method.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    processRequest(request, response);
  }

  /**
   * Handles the HTTP <code>POST</code> method.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {
    processRequest(request, response);
  }

  /**
   * Returns a short description of the servlet.
   *
   * @return a String containing servlet description
   */
  @Override
  public String getServletInfo() {
    return "Short description";
  }// </editor-fold>

}

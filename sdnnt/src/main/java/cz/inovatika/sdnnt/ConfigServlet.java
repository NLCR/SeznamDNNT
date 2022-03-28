
package cz.inovatika.sdnnt;

import cz.inovatika.sdnnt.model.User;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.NotificationsService;
import cz.inovatika.sdnnt.services.UserController;
import cz.inovatika.sdnnt.services.exceptions.NotificationsException;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;
import cz.inovatika.sdnnt.services.impl.DefaultApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.impl.NotificationServiceImpl;
import cz.inovatika.sdnnt.services.impl.users.UserControlerImpl;
import cz.inovatika.sdnnt.services.impl.users.UsersUtils;

import org.json.JSONException;
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
      JSONObject js = new JSONObject(Options.getInstance().getClientConf().toString());
      
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

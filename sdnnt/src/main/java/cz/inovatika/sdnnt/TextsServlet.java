
package cz.inovatika.sdnnt;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse; 
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
@WebServlet(name = "TextsServlet", urlPatterns = {"/texts/*"})
public class TextsServlet extends HttpServlet {
  
  public static final Logger LOGGER = Logger.getLogger(TextsServlet.class.getName()); 

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
    response.setContentType("text/plain;charset=UTF-8");
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
    response.setHeader("Pragma", "no-cache"); // HTTP 1.0
    response.setDateHeader("Expires", 0); // Proxies.
    PrintWriter out = response.getWriter();
    try {
      String actionNameParam = request.getPathInfo().substring(1);
      if (actionNameParam != null) {
        Actions actionToDo = Actions.valueOf(actionNameParam.toUpperCase());
        out.println(actionToDo.doPerform(request, response));
      } else {

        out.print("actionNameParam -> " + actionNameParam);
      }
    } catch (IOException e1) {
      LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e1.toString());
      out.print(e1.toString());
    } catch (SecurityException e1) {
      LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    } catch (Exception e1) {
      LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e1.toString());
      out.print(e1.toString());
    }
  }
  
  enum Actions {
    READ {
      @Override
      String doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        try {
          String path = InitServlet.CONFIG_DIR + File.separator + Options.getInstance().getString("textsDir") 
                  + File.separator + req.getParameter("id") + "_" + req.getParameter("lang");
          File file = new File(path);
          return FileUtils.readFileToString(file,  "UTF-8");

        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          return ex.toString();
        }
      }
    },
    WRITE {
      @Override
      String doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        try {
          String path = InitServlet.CONFIG_DIR + File.separator + Options.getInstance().getString("textsDir") 
                  + File.separator + req.getParameter("id") + "_" + req.getParameter("lang");
          File file = new File(path);
          FileUtils.writeStringToFile(file, IOUtils.toString(req.getInputStream(), "UTF-8"), "UTF-8");
          return new JSONObject().put("msg", "saved!").toString();
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          return new JSONObject().put("error", ex).toString();
        }
      }
    };

    abstract String doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception;
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

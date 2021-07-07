package cz.inovatika.sdnnt.oai;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author alberto
 */
@WebServlet(name = "OAIServlet", urlPatterns = {"/oai"})
public class OAIServlet extends HttpServlet {

  public static final Logger LOGGER = Logger.getLogger(OAIServlet.class.getName());

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
    response.setContentType("text/xml;charset=UTF-8");
    PrintWriter out = response.getWriter();
    try {
      String actionNameParam = request.getParameter("verb");
      if (actionNameParam != null) {
        Actions actionToDo = Actions.valueOf(actionNameParam);
        String xml = actionToDo.doPerform(request, response);
        out.println(xml);
      } else {
        String xml = OAIRequest.headerOAI() + OAIRequest.responseDateTag()
                + "<request>" + request.getRequestURL() + "</request>"
                + "<error code=\"badVerb\">verb is absent</error>"
                + "</OAI-PMH>";
        out.print(xml);
      }
    } catch (IllegalArgumentException e1) {
      LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
      String xml = OAIRequest.headerOAI() + OAIRequest.responseDateTag()
              + "<request>" + request.getRequestURL() + "</request>"
              + "<error code=\"badVerb\">Illegal OAI verb</error>"
              + "</OAI-PMH>";
      out.print(xml);
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
    Identify {
      @Override
      String doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        return OAIRequest.identify(req);
      }
    },
    ListSets {
      @Override
      String doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        return OAIRequest.listSets(req);
      }
    },
    ListMetadataFormats {
      @Override
      String doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        return OAIRequest.metadataFormats(req);
      }
    },
    GetRecord {
      @Override
      String doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        return OAIRequest.getRecord(req);
      }
    },
    ListIdentifiers {
      @Override
      String doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        return OAIRequest.listRecords(req, true);
      }
    },
    ListRecords {
      @Override
      String doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        return OAIRequest.listRecords(req, false);
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

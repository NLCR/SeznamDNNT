package cz.inovatika.sdnnt;

import cz.inovatika.sdnnt.indexer.models.User;
import cz.inovatika.sdnnt.indexer.models.Zadost;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.util.NamedList;
import org.json.JSONObject;

/**
 *
 * @author alberto
 */
@WebServlet(value = "/account/*")
public class AccountServlet extends HttpServlet {

  public static final Logger LOGGER = Logger.getLogger(AccountServlet.class.getName());

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
    response.setContentType("application/json;charset=UTF-8");
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
    response.setHeader("Pragma", "no-cache"); // HTTP 1.0
    response.setDateHeader("Expires", 0); // Proxies.
    PrintWriter out = response.getWriter();
    try {
      String actionNameParam = request.getPathInfo().substring(1);
      if (actionNameParam != null) {
        Actions actionToDo = Actions.valueOf(actionNameParam.toUpperCase());
        JSONObject json = actionToDo.doPerform(request, response);
        out.println(json.toString(2));
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
    SEARCH {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        JSONObject ret = new JSONObject();
        Options opts = Options.getInstance();
        try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host")).build()) {
          String q = req.getParameter("q");
          if (q == null) {
            q = "*";
          }
          User user = UserController.getUser(req);
          if (user == null) {
            user = UserController.dummy("incad@incad.cz");
//            ret.put("error", "Not logged");
//            return ret;
          }
          SolrQuery query = new SolrQuery(q)
                  .setRows(20)
                  .setParam("df", "fullText")
                  .setFacet(true).addFacetField("typ","state","new_stav")
                  .addFilterQuery("user:" + user.username)
                  .setParam("json.nl", "arrntv")
                  .setFields("*,raw:[json]");
          QueryRequest qreq = new QueryRequest(query);
          NoOpResponseParser rParser = new NoOpResponseParser();
          rParser.setWriterType("json");
          qreq.setResponseParser(rParser);
          NamedList<Object> qresp = solr.request(qreq, "zadost"); 
          solr.close();
          return new JSONObject((String) qresp.get("response"));
        } catch (SolrServerException | IOException ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          ret.put("error", ex);
        }

        return ret; 
      }
    },
    SAVE_ZADOST {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        JSONObject json = new JSONObject();
        User user = UserController.getUser(req);
        try {
          String inputJs;
          if (req.getMethod().equals("POST")) {
            inputJs = IOUtils.toString(req.getInputStream(), "UTF-8");
          } else {
            inputJs = req.getParameter("json");
          }
          if (user == null) {
            json.put("error", "Not logged");
            user = UserController.dummy(new JSONObject(inputJs).getString("user"));
            // user = new JSONObject().put("name", "testUser");
            // return json;
          }
          json = Zadost.save(inputJs, user.username);
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    ADD_ID {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        JSONObject json = new JSONObject();
        User user = UserController.getUser(req);
        if (user == null) {
          json.put("error", "Not logged");
          // user = new JSONObject().put("name", "testUser");
          return json;
        }
        try {
          
          // json = Zadost.save(req.getParameter("id"), req.getParameter("identifier"), user.username);
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    };

    abstract JSONObject doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception;
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

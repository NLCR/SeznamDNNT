package cz.inovatika.sdnnt;

import cz.inovatika.sdnnt.index.Indexer;
import cz.inovatika.sdnnt.indexer.models.User;
import cz.inovatika.sdnnt.indexer.models.Zadost;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cz.inovatika.sdnnt.services.AccountService;
import cz.inovatika.sdnnt.services.impl.AccountServiceImpl;
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

  private AccountService service;


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
      User user = null;  
          if (user == null) {
            user = UserController.dummy(request.getParameter("user"));
            // out.print("{\"error\": \"Not logged\"}");
            // return;
          }
        Actions actionToDo = Actions.valueOf(actionNameParam.toUpperCase());
        JSONObject json = actionToDo.doPerform(this.service, request, response, user);
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

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    this.service = new AccountServiceImpl();
  }

  @Override
  public void init() throws ServletException {
    super.init();
    this.service = new AccountServiceImpl();
  }

  enum Actions {
    // vyhledava zadosti
    SEARCH {
      @Override
      JSONObject doPerform(AccountService service, HttpServletRequest req, HttpServletResponse response, User user) throws Exception {
        String q = req.getParameter("q");
        String state = req.getParameter("state");
        String navrh = req.getParameter("navrh");
        JSONObject ret = new JSONObject();
        try {
          JSONObject qresp = service.search(q, state, navrh, user);
          //ret =  new JSONObject((String) qresp.get("response"));
          ret = service.search(q, state, navrh, user);
          //ret = qresp.getJSONObject("response");
        } catch (SolrServerException | IOException ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          ret.put("error", ex);
        }
        return ret;
      }
    },
    // ziskani konkretni zadosti, vypisou se zaznamy
    GET_ZADOST {
      @Override
      JSONObject doPerform(AccountService service, HttpServletRequest req, HttpServletResponse response, User user) throws Exception {
        JSONObject ret = new JSONObject();
        Options opts = Options.getInstance();
        try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host")).build()) {
          SolrQuery query = new SolrQuery("id:" + req.getParameter("id"))
                  .setRows(1).setFields("*,process:[json]");
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
    // dotazovani do druheho indexu
    GET_ZADOST_RECORDS {
      @Override
      JSONObject doPerform(AccountService service, HttpServletRequest req, HttpServletResponse response, User user) throws Exception {
        JSONObject ret = new JSONObject();
        Options opts = Options.getInstance();
        try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host")).build()) {
          
          // String q = req.getParameter("identifiers").replace("[", "(").replace("]", ")");
          // SolrQuery query = new SolrQuery("identifier:" + q)
          SolrQuery query = new SolrQuery("*:*")
                  .setRows(100)
                  .addFilterQuery("{!join fromIndex=zadost from=identifiers to=identifier} id:" + req.getParameter("id"))
                  .setSort(SolrQuery.SortClause.asc("title_sort"))
                  .setFields("*,raw:[json]");
          QueryRequest qreq = new QueryRequest(query);
          NoOpResponseParser rParser = new NoOpResponseParser();
          rParser.setWriterType("json");
          qreq.setResponseParser(rParser);
          NamedList<Object> qresp = solr.request(qreq, "catalog"); 
          solr.close();
          return new JSONObject((String) qresp.get("response"));
        } catch (SolrServerException | IOException ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          ret.put("error", ex);
        }

        return ret; 
      }
    },
    // ulozeni zadosti
    SAVE_ZADOST {
      @Override
      JSONObject doPerform(AccountService service, HttpServletRequest req, HttpServletResponse response, User user) throws Exception {
        JSONObject json = new JSONObject();
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
          json = service.saveRequest(inputJs, user);
          //json = Zadost.save(inputJs, user.username);
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    // pridava vsechny zaznamy jednoho vyjadreni do zadosti
    // dilo -> vyjadreni -> provedeni
    ADD_FRBR_TO_ZADOST {
      @Override
      JSONObject doPerform(AccountService service, HttpServletRequest req, HttpServletResponse response, User user) throws Exception {
        JSONObject json = new JSONObject();
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
          json = Zadost.saveWithFRBR(inputJs, user.username, req.getParameter("frbr"));
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    // odznaci zadost jako zprocesovanou
    PROCESS_ZADOST {
      @Override
      JSONObject doPerform(AccountService service, HttpServletRequest req, HttpServletResponse response, User user) throws Exception {
        JSONObject json = new JSONObject();
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
          json = Zadost.markAsProcessed(inputJs, user.username);
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    },

    // schvalit navrh - na vyrazeni, na zarazeni - pouze kurator - ne do api
    APPROVE_NAVRH {
      @Override
      JSONObject doPerform(AccountService service, HttpServletRequest req, HttpServletResponse response, User user) throws Exception {
        JSONObject json = new JSONObject();
        try {
          JSONObject inputJs;
          if (req.getMethod().equals("POST")) {
            inputJs = new JSONObject(IOUtils.toString(req.getInputStream(), "UTF-8"));
          } else {
            inputJs = new JSONObject(req.getParameter("json"));
          }
          Indexer.changeStav(inputJs.getString("identifier"), 
                  inputJs.getJSONObject("zadost").getString("navrh"), user.username);
          return Zadost.approve(inputJs.getString("identifier"), inputJs.getJSONObject("zadost").toString(), user.username);
          // json = Zadost.markAsProcessed(inputJs, user.username);
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    // odmitnout navrh - pouze kurator - ne do api
    REJECT_NAVRH {
      @Override
      JSONObject doPerform(AccountService service, HttpServletRequest req, HttpServletResponse response, User user) throws Exception {
        JSONObject json = new JSONObject();
        try {
          JSONObject inputJs;
          if (req.getMethod().equals("POST")) {
            inputJs = new JSONObject(IOUtils.toString(req.getInputStream(), "UTF-8"));
          } else {
            inputJs = new JSONObject(req.getParameter("json"));
          }
          Indexer.changeStav(inputJs.getString("identifier"), 
                  inputJs.getJSONObject("zadost").getString("navrh"), user.username);
          return Zadost.reject(inputJs.getString("identifier"), inputJs.getJSONObject("zadost").toString(), 
                  inputJs.getString("reason"), user.username);
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    APPROVE_NAVRH_IN_IMPORT {
      @Override
      JSONObject doPerform(AccountService service, HttpServletRequest req, HttpServletResponse response, User user) throws Exception {
        JSONObject json = new JSONObject();
        try {
          JSONObject inputJs;
          if (req.getMethod().equals("POST")) {
            inputJs = new JSONObject(IOUtils.toString(req.getInputStream(), "UTF-8"));
          } else {
            inputJs = new JSONObject(req.getParameter("json"));
          }
          return Indexer.approveInImport(inputJs.getString("identifier"), inputJs.getJSONObject("importId").toString(), user.username);
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    },
    ADD_ID {
      @Override
      JSONObject doPerform(AccountService service, HttpServletRequest req, HttpServletResponse response, User user) throws Exception {
        JSONObject json = new JSONObject();
        try {
          
          // json = Zadost.save(req.getParameter("id"), req.getParameter("identifier"), user.username);
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }
        return json;
      }
    };

    abstract JSONObject doPerform(AccountService service, HttpServletRequest request, HttpServletResponse response, User user) throws Exception;
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

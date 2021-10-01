package cz.inovatika.sdnnt;

import cz.inovatika.sdnnt.index.Indexer;
import cz.inovatika.sdnnt.indexer.models.NotificationInterval;
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

import cz.inovatika.sdnnt.rights.RightsResolver;
import cz.inovatika.sdnnt.rights.impl.predicates.MustBeLogged;
import cz.inovatika.sdnnt.rights.impl.predicates.UserMustBeInRole;
import cz.inovatika.sdnnt.services.AccountService;
import cz.inovatika.sdnnt.services.impl.AccountServiceImpl;
import cz.inovatika.sdnnt.services.impl.UserControlerImpl;
import cz.inovatika.sdnnt.utils.ServletsSupport;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.util.NamedList;
import org.json.JSONObject;

import static cz.inovatika.sdnnt.utils.ServletsSupport.*;

import static javax.servlet.http.HttpServletResponse.*;
import static cz.inovatika.sdnnt.rights.Role.*;


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
        JSONObject json = actionToDo.doPerform( request, response);
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
  }

  @Override
  public void init() throws ServletException {
    super.init();
  }

  enum Actions {
    // vyhledava zadosti
    SEARCH {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        if (new RightsResolver(req, new MustBeLogged()).permit()) {


          User user = new UserControlerImpl(req).getUser();
          String q = req.getParameter("q");
          String state = req.getParameter("state");
          String navrh = req.getParameter("navrh");
          String institution = req.getParameter("institution");
          String delegated = req.getParameter("delegated");
          String priority = req.getParameter("priority");

          String page = req.getParameter("page");
          String rows = req.getParameter("rows");

          try {
            AccountService service = new AccountServiceImpl(new UserControlerImpl(req));

            //String priority, String delegated
            return service.search(q, state, navrh, institution, priority, delegated, user, rows != null ? Integer.parseInt(rows): -1, page != null ? Integer.parseInt(page) : -1);
          } catch (SolrServerException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return errorJson(response, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
          }
        } else {
          return errorJson(response, SC_FORBIDDEN, "not allowed");
        }
      }
    },
    // ziskani konkretni zadosti, vypisou se zaznamy
    GET_ZADOST {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        Options opts = Options.getInstance();
        if (new RightsResolver(req, new MustBeLogged()).permit()) {
          try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host")).build()) {
            SolrQuery query = new SolrQuery("id:" + req.getParameter("id"))
                    .setRows(1).setFields("*,process:[json]");
            QueryRequest qreq = new QueryRequest(query);
            NoOpResponseParser rParser = new NoOpResponseParser();
            rParser.setWriterType("json");
            qreq.setResponseParser(rParser);
            NamedList<Object> qresp = solr.request(qreq, "zadost");

            return new JSONObject((String) qresp.get("response"));
          } catch (SolrServerException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return errorJson(response, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
          }
        } else {
          return errorJson(response, SC_FORBIDDEN, "not allowed");
        }
      }
    },
    // dotazovani do druheho indexu
    GET_ZADOST_RECORDS {
      @Override
      JSONObject doPerform( HttpServletRequest req, HttpServletResponse response) throws Exception {

        if (new RightsResolver(req, new MustBeLogged()).permit()) {
          Options opts = Options.getInstance();
          int rows = opts.getClientConf().getInt("rows");
          if (req.getParameter("rows") != null) {
            rows = Integer.parseInt(req.getParameter("rows"));
          }
          int start = 0;
          if (req.getParameter("page") != null) {
            start = Integer.parseInt(req.getParameter("page")) * rows;
          }
          try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host")).build()) {

            SolrQuery query = new SolrQuery("*:*")
                    .setRows(rows)
                    .setStart(start)
                    .addFilterQuery("{!join fromIndex=zadost from=identifiers to=identifier} id:" + req.getParameter("id"))
                    .setSort(SolrQuery.SortClause.asc("title_sort"))
                    .setFields("*,raw:[json]");
            QueryRequest qreq = new QueryRequest(query);
            NoOpResponseParser rParser = new NoOpResponseParser();
            rParser.setWriterType("json");
            qreq.setResponseParser(rParser);
            NamedList<Object> qresp = solr.request(qreq, "catalog");
            return new JSONObject((String) qresp.get("response"));
          } catch (SolrServerException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return errorJson(response, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
          }
        } else {
          return errorJson(response, SC_FORBIDDEN, "not allowed");
        }
      }
    },
    // ulozeni zadosti
    SAVE_ZADOST {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        if (new RightsResolver(req, new MustBeLogged()).permit()) {
          try {
            UserControlerImpl userControler = new UserControlerImpl(req);
            AccountService service = new AccountServiceImpl(userControler);
            return service.saveRequest(readInputJSON(req).toString(), new UserControlerImpl(req).getUser());
          } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return errorJson(response, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
          }
        } else {
          return errorJson(response, SC_FORBIDDEN, "not allowed");
        }
      }
    },
    // pridava vsechny zaznamy jednoho vyjadreni do zadosti
    // dilo -> vyjadreni -> provedeni
    ADD_FRBR_TO_ZADOST {
      @Override
      JSONObject doPerform( HttpServletRequest req, HttpServletResponse response) throws Exception {

        if (new RightsResolver(req, new MustBeLogged()).permit()) {
          try {
            return Zadost.saveWithFRBR(readInputJSON(req).toString(), new UserControlerImpl(req).getUser().username, req.getParameter("frbr"));
          } catch (Exception e) {
            return errorJson(response, SC_INTERNAL_SERVER_ERROR, e.getMessage());
          }
        } else {
          return errorJson(response, SC_FORBIDDEN, "not allowed");
        }
      }
    },
    // odznaci zadost jako zprocesovanou
    PROCESS_ZADOST {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {

        if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {
          try {
            return Zadost.markAsProcessed(readInputJSON(req).toString(), new UserControlerImpl(req).getUser().username);
          } catch (IOException e) {
            return errorJson(response, SC_INTERNAL_SERVER_ERROR, e.getMessage());
          }
        } else {
          return errorJson(response, SC_FORBIDDEN, "not allowed");
        }
      }
    },

    // kurator schvali VVN. Vysledek N
    // O nebo N - > (uživatel/korporace) -> NZN -> (kurátor) -> PA -> (běží lhůta, vstoupi do toho uživatel/korporace ) -> VVN -> (kurátor) -> N
    APPROVE_VVN {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {
          try {
            User user = new UserControlerImpl(req).getUser();
            JSONObject inputJs = ServletsSupport.readInputJSON(req);
            // TODO: Transakce
            Indexer.changeStav(inputJs.getString("identifier"),
                    "VVNtoN", user.username);
            return Zadost.approve(inputJs.getString("identifier"), inputJs.getJSONObject("zadost").toString(),
                    inputJs.getString("reason"), user.username,null);
          } catch (Exception e) {
            return errorJson(response, SC_INTERNAL_SERVER_ERROR, e.getMessage());
          }
        } else {
          return errorJson(response, SC_FORBIDDEN, "not allowed");
        }
      }
    },
    // kurator neschvali VVN. Nemenime dntstav, zustava PA
    // O nebo N - > (uživatel/korporace) -> NZN -> (kurátor) -> PA -> (běží lhůta, vstoupi do toho uživatel/korporace ) -> VVN -> (kurátor, opačné rozhodnutí) -> PA -> (6 měsíců)-> A
    REJECT_VVN {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {
          try {
            User user = new UserControlerImpl(req).getUser();
            JSONObject inputJs = ServletsSupport.readInputJSON(req);
            Indexer.changeStav(inputJs.getString("identifier"),
                    "VVNtoPA", user.username);
            return Zadost.reject(inputJs.getString("identifier"), inputJs.getJSONObject("zadost").toString(),
                    inputJs.getString("reason"), user.username);
          } catch (Exception e) {
            return errorJson(response, SC_INTERNAL_SERVER_ERROR, e.getMessage());
          }
        } else {
          return errorJson(response, SC_FORBIDDEN, "not allowed");
        }
      }
    },

    APPROVE_NAVRH_LIB{
      @Override
      JSONObject doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (new RightsResolver(request, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {
          try {
            User user = new UserControlerImpl(request).getUser();
            JSONObject inputJs = ServletsSupport.readInputJSON(request);
            // todo: transactions (optimistic locking)
            Indexer.reduceVisbilityState(inputJs.getString("identifier"),
                    inputJs.getJSONObject("zadost").getString("navrh"), user.username);
            return Zadost.approve(inputJs.getString("identifier"), inputJs.getJSONObject("zadost").toString(),
                    inputJs.getString("reason"), user.username,"approvedlib");
          } catch (Exception e) {
            return errorJson(response, SC_INTERNAL_SERVER_ERROR, e.getMessage());
          }
        } else {
          return errorJson(response, SC_FORBIDDEN, "not allowed");
        }
      }
    },
    // schvalit navrh - na vyrazeni, na zarazeni - pouze kurator - ne do api
    CHANGE_STAV_DIRECT {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {

        if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {
          try {
            User user = new UserControlerImpl(req).getUser();
            JSONObject inputJs = ServletsSupport.readInputJSON(req);
            // todo: transactions (optimistic locking)
            return Indexer.changeStavDirect(inputJs.getString("identifier"),
                    inputJs.getString("newStav"), 
                    inputJs.getString("poznamka"), 
                    inputJs.getJSONArray("granularity"), 
                    user.username);
          } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return errorJson(response, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
          }
        } else {
          return errorJson(response, SC_FORBIDDEN, "not allowed");
        }
      }
    },
    // schvalit navrh - na vyrazeni, na zarazeni - pouze kurator - ne do api
    APPROVE_NAVRH {
      @Override
      JSONObject doPerform( HttpServletRequest req, HttpServletResponse response) throws Exception {

        if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {
          try {
            User user = new UserControlerImpl(req).getUser();
            JSONObject inputJs = ServletsSupport.readInputJSON(req);
            // todo: transactions (optimistic locking)
            Indexer.changeStav(inputJs.getString("identifier"),
                    inputJs.getJSONObject("zadost").getString("navrh"), user.username);
            return Zadost.approve(inputJs.getString("identifier"), inputJs.getJSONObject("zadost").toString(),
                    inputJs.getString("reason"), user.username,null);
          } catch (Exception ex) {
            return errorJson(response, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
          }
        } else {
          return errorJson(response, SC_FORBIDDEN, "not allowed");
        }
      }
    },
    // odmitnout navrh - pouze kurator - ne do api
    REJECT_NAVRH {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {
          User user = new UserControlerImpl(req).getUser();
          try {
            JSONObject inputJs = ServletsSupport.readInputJSON(req);
            return Zadost.reject(inputJs.getString("identifier"), inputJs.getJSONObject("zadost").toString(),
                    inputJs.getString("reason"), user.username);
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            return errorJson(response, SC_INTERNAL_SERVER_ERROR, e.getMessage());
          }
        } else {
          return errorJson(response, SC_FORBIDDEN, "not allowed");
        }
      }
    },
    APPROVE_NAVRH_IN_IMPORT {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {
          try {
            User user = new UserControlerImpl(req).getUser();
            JSONObject inputJs = ServletsSupport.readInputJSON(req);
            return Indexer.approveInImport(inputJs.getString("identifier"), inputJs.getJSONObject("importId").toString(), user.username);
          } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            return errorJson(response, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
          }
        } else {
          return errorJson(response, SC_FORBIDDEN, "not allowed");
        }
      }
    },
    ADD_ID {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin, user)).permit()) {
          try {
            // TODO: What is it ??
            return new JSONObject();
          } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            return errorJson(response, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
          }
        } else {
          return errorJson(response, SC_FORBIDDEN, "not allowed");
        }
      }
    },
    FOLLOW_RECORD {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        if (new RightsResolver(req, new MustBeLogged()).permit()) {
          try {
            User user = new UserControlerImpl(req).getUser();
            return Indexer.followRecord(req.getParameter("identifier"),  user.username,  NotificationInterval.mesic.valueOf(user.notifikace_interval),  "true".equals(req.getParameter("follow")));
          } catch (Exception e) {
            return errorJson(response, SC_INTERNAL_SERVER_ERROR, e.getMessage());
          }
        } else {
          return errorJson(response, SC_FORBIDDEN, "not allowed");
        }
      }
    };

    abstract JSONObject doPerform( HttpServletRequest request, HttpServletResponse response) throws Exception;
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

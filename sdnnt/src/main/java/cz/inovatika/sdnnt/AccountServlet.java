package cz.inovatika.sdnnt;

import cz.inovatika.sdnnt.index.CatalogSearcher;
import cz.inovatika.sdnnt.index.Indexer;
import cz.inovatika.sdnnt.indexer.models.NotificationInterval;
import cz.inovatika.sdnnt.indexer.models.User;
import cz.inovatika.sdnnt.model.Zadost;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cz.inovatika.sdnnt.rights.RightsResolver;
import cz.inovatika.sdnnt.rights.Role;
import cz.inovatika.sdnnt.rights.impl.predicates.MustBeLogged;
import cz.inovatika.sdnnt.rights.impl.predicates.UserMustBeInRole;
import cz.inovatika.sdnnt.services.AccountService;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.services.impl.AccountServiceImpl;
import cz.inovatika.sdnnt.services.impl.ResourceBundleServiceImpl;
import cz.inovatika.sdnnt.services.impl.UserControlerImpl;
import cz.inovatika.sdnnt.utils.ServletsSupport;
import cz.inovatika.sdnnt.utils.VersionStringCast;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.json.JSONArray;
import org.json.JSONException;
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

          String sort = req.getParameter("sort_account");
          if (sort == null) {
            sort = req.getParameter("user_sort_account");
          }

          String page = req.getParameter("page");
          String rows = req.getParameter("rows");

          try {
            AccountService service = new AccountServiceImpl(new UserControlerImpl(req), new ResourceBundleServiceImpl(req));
            return VersionStringCast.cast(service.search(q, state, Arrays.asList(navrh), institution, priority, delegated, sort, user, rows != null ? Integer.parseInt(rows): -1, page != null ? Integer.parseInt(page) : -1));
          } catch (SolrServerException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return errorJson(response, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
          }
        } else {
          return errorJson(response, SC_FORBIDDEN,"notallowed", "not allowed");
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

            /*
            SolrQuery query = new SolrQuery("id:" + req.getParameter("id"))
                    .setRows(1).setFields("*,process:[json]");

            QueryRequest qreq = new QueryRequest(query);
            NoOpResponseParser rParser = new NoOpResponseParser();
            rParser.setWriterType("json");
            qreq.setResponseParser(rParser);
            NamedList<Object> qresp = solr.request(qreq, "zadost");

            return new JSONObject((String) qresp.get("response"));
            */
            AccountService service = new AccountServiceImpl(new UserControlerImpl(req), new ResourceBundleServiceImpl(req));
            return service.getRequest(req.getParameter("id"));

          } catch (SolrServerException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return errorJson(response, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
          }
        } else {
          return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
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
          try {
            AccountService service = new AccountServiceImpl(new UserControlerImpl(req), new ResourceBundleServiceImpl(req));
            return service.getRecords(req.getParameter("id"),rows, start );
          } catch (SolrServerException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return errorJson(response, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
          }
        } else {
          return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
        }
      }
    },

    // ulozeni zadosti
    SEND_ZADOST {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        if (new RightsResolver(req, new MustBeLogged()).permit()) {
          try {
            UserControlerImpl userControler = new UserControlerImpl(req);
            AccountService service = new AccountServiceImpl(userControler, new ResourceBundleServiceImpl(req));
            return service.userCloseRequest(readInputJSON(req).toString());
          } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return errorJson(response, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
          }
        } else {
          return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
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
            AccountService service = new AccountServiceImpl(userControler, new ResourceBundleServiceImpl(req));
            return service.saveRequest(readInputJSON(req).toString(), new UserControlerImpl(req).getUser(), null);

          } catch(ConflictException cex) {
            LOGGER.log(Level.SEVERE, null, cex);
            return errorJson(response, SC_CONFLICT,cex.getKey(), cex.getMessage());
          } catch(AccountException cex) {
            LOGGER.log(Level.SEVERE, null, cex);
            return errorJson(response, SC_FORBIDDEN,cex.getKey(), cex.getMessage());
          } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return errorJson(response, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
          }
        } else {
          return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
        }
      }
    },
    // ulozeni zadosti
    SAVE_KURATOR_ZADOST {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(kurator, mainKurator)).permit()) {
          try {
            UserControlerImpl userControler = new UserControlerImpl(req);
            AccountService service = new AccountServiceImpl(userControler, new ResourceBundleServiceImpl(req));
            return service.saveCuratorRequest(readInputJSON(req).toString(), null);
          } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return errorJson(response, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
          }
        } else {
          return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
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
          return errorJson(response, SC_FORBIDDEN, "notallowed","not allowed");
        }
      }
    },
    // odznaci zadost jako zprocesovanou
    PROCESS_ZADOST {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
        if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {
          try {
            UserControlerImpl userControler = new UserControlerImpl(req);
            AccountService service = new AccountServiceImpl(userControler, new ResourceBundleServiceImpl(req));
            return service.curatorCloseRequest(readInputJSON(req).toString());
          } catch (IOException e) {
            return errorJson(response, SC_INTERNAL_SERVER_ERROR, e.getMessage());
          }
        } else {
          return errorJson(response, SC_FORBIDDEN, "notallowed","not allowed");
        }
      }
    },

    APPROVE {
      @Override
      JSONObject doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (new RightsResolver(request, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {
          try {
            JSONObject inputJs = ServletsSupport.readInputJSON(request);
            String identifier = inputJs.getString("identifier");
            String zadostJSON = inputJs.getJSONObject("zadost").toString();
            Zadost zadost = Zadost.fromJSON(zadostJSON);

            UserControlerImpl userControler = new UserControlerImpl(request);
            AccountService service = new AccountServiceImpl(userControler, new ResourceBundleServiceImpl(request));
            String alternativeState = request.getParameter("alternative");
            if (alternativeState != null) {
              return service.curatorSwitchAlternativeState(alternativeState, zadost.getId(), identifier, inputJs.getString("reason"));
            } else {
              return service.curatorSwitchState(zadost.getId(), identifier, inputJs.getString("reason"));
            }
          } catch (ConflictException e) {
            return errorJson(response, SC_CONFLICT, e.getKey(), e.getMessage());
          } catch (AccountException e) {
            return errorJson(response, SC_BAD_REQUEST, e.getKey(), e.getMessage());
          } catch (JSONException | SolrServerException e) {
            return errorJson(response, SC_INTERNAL_SERVER_ERROR, e.getMessage());
          }
        } else {
          return errorJson(response, SC_FORBIDDEN, "notallowed","not allowed");
        }
      }
    },

    REJECT {
      @Override
      JSONObject doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (new RightsResolver(request, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {
          JSONObject inputJs = ServletsSupport.readInputJSON(request);
          String identifier = inputJs.getString("identifier");
          String zadostJSON = inputJs.getJSONObject("zadost").toString();
          Zadost zadost = Zadost.fromJSON(zadostJSON);

          UserControlerImpl userControler = new UserControlerImpl(request);
          AccountService service = new AccountServiceImpl(userControler, new ResourceBundleServiceImpl(request));
          return service.curatorRejectSwitchState(zadost.getId(), identifier, inputJs.getString("reason"));

        } else {
          return errorJson(response, SC_FORBIDDEN, "notallowed","not allowed");
        }
      }
    },

    DELETE {
      @Override
      JSONObject doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (new RightsResolver(request, new MustBeLogged(), new UserMustBeInRole(user,knihovna, mainKurator, kurator, admin)).permit()) {
          JSONObject inputJs = ServletsSupport.readInputJSON(request);

          Zadost zadost = Zadost.fromJSON(inputJs.toString());

          UserControlerImpl userControler = new UserControlerImpl(request);
          User user = userControler.getUser();

          List<String> ordinaryUsers = Arrays.asList(Role.user.name(), Role.knihovna.name());
          List<String> kuratorsAndAdmins = Arrays.asList(Role.admin.name(), Role.kurator.name(), Role.mainKurator.name());

          boolean deleteIsPossible = (ordinaryUsers.contains(user.role) && zadost.getState().equals("open")) ||
                  (kuratorsAndAdmins.contains(user.role));

          if (deleteIsPossible) {
            AccountService service = new AccountServiceImpl(userControler, new ResourceBundleServiceImpl(request));
            return service.deleteRequest(zadost.toJSON().toString());
          } else {
            return errorJson(response, SC_FORBIDDEN, "notallowed","not allowed");
          }
        } else {
          return errorJson(response, SC_FORBIDDEN, "notallowed","not allowed");
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
            Indexer.changeStavDirect(inputJs.getString("identifier"),
                    inputJs.getString("newStav"),
                    inputJs.optString("newLicense"),
                    inputJs.getString("poznamka"),
                    inputJs.getJSONArray("granularity"), 
                    user.username);


            CatalogSearcher searcher = new CatalogSearcher();
            return searcher.getById(inputJs.getString("identifier"), user);

          } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return errorJson(response, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
          }
        } else {
          return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
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
          return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
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
          return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
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
          return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
        }
      }
    },


    PREPARE_ZADOST {
      @Override
      JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {

        if (new RightsResolver(req, new MustBeLogged()).permit()) {

          UserControlerImpl userControler = new UserControlerImpl(req);
          AccountService service = new AccountServiceImpl(userControler, new ResourceBundleServiceImpl(req));

          String[] navrhy = req.getParameterValues("navrh");
          if (navrhy != null && navrhy.length >0 ) {
              //String navrh = req.getParameter("navrh");
              JSONObject result = service.search(null, "open", Arrays.asList(navrhy), null, null, null ,null, userControler.getUser(), 100, 0);
              if (result.has("response") && result.getJSONObject("response").has("numFound")) {
                  int numFound = result.getJSONObject("response").getInt("numFound");
                  if (numFound > 0) {
                      JSONArray docs = result.getJSONObject("response").getJSONArray("docs");
                      Zadost zadost = Zadost.fromJSON(docs.getJSONObject(0).toString());
                      return zadost.toJSON();
                  }
              }
              Zadost nZadost = new Zadost(UUID.randomUUID().toString());
              nZadost.setNavrh(navrhy[0]);
              nZadost.setState("open");
              nZadost.setUser(userControler.getUser().username);
              nZadost.setIdentifiers(new ArrayList<>());

              JSONObject jsonZadost = service.saveRequest(nZadost.toJSON().toString(), userControler.getUser(), null);
              return jsonZadost;

          } else {
              return errorJson(response, SC_BAD_REQUEST, "missing navrh parameter");
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

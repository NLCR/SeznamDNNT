package cz.inovatika.sdnnt;

import cz.inovatika.sdnnt.index.AccountIterationSupport;
import cz.inovatika.sdnnt.index.CatalogSearcher;
import cz.inovatika.sdnnt.index.Indexer;
import cz.inovatika.sdnnt.indexer.models.Import;
import cz.inovatika.sdnnt.indexer.models.NotificationInterval;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.workflow.SwitchStateOptions;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cz.inovatika.sdnnt.rights.RightsResolver;
import cz.inovatika.sdnnt.rights.Role;
import cz.inovatika.sdnnt.rights.impl.predicates.MustBeCalledFromLocalhost;
import cz.inovatika.sdnnt.rights.impl.predicates.MustBeLogged;
import cz.inovatika.sdnnt.rights.impl.predicates.UserMustBeInRole;
import cz.inovatika.sdnnt.services.AccountService;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.services.impl.AccountServiceImpl;
import cz.inovatika.sdnnt.services.impl.ResourceBundleServiceImpl;
import cz.inovatika.sdnnt.services.impl.users.UserControlerImpl;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.inovatika.sdnnt.utils.*;

import static cz.inovatika.sdnnt.utils.ServletsSupport.*;

import static javax.servlet.http.HttpServletResponse.*;
import static cz.inovatika.sdnnt.rights.Role.*;


/**
 * @author alberto
 */
@WebServlet(value = "/account/*")
public class AccountServlet extends HttpServlet {

    public static final Logger LOGGER = Logger.getLogger(AccountServlet.class.getName());


    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
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

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    @Override
    public void init() throws ServletException {
        super.init();
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
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


    enum Actions {

        // vyhledava zadosti
        _UPDATE_ZADOST {

            private static final int LIMIT = 1000;

            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeCalledFromLocalhost()).permit()) {
                    long start = System.currentTimeMillis();
                    final AccountIterationSupport support = new AccountIterationSupport();
                    try {
                        JSONArray jsonArray = new JSONArray();
                        AtomicInteger number = new AtomicInteger(0);
                        Map<String, String> reqMap = new HashMap<>();
                        reqMap.put("rows", "" + LIMIT);
                        // all request with null type of request are 
                        updateTypeOfReQuestUSER(support, jsonArray, number, reqMap);
                        
                        updateIDPart(support, jsonArray, number, reqMap);
                        
                        JSONObject object = new JSONObject();
                        object.put("numberOfObjects", number.get());
                        object.put("bulkResults", jsonArray);
                        return object;

                    } catch (Exception ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                        return errorJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.toString());
                    } finally {
                        PureHTTPSolrUtils.commit(support.getCollection());
                        QuartzUtils.printDuration(LOGGER, start);
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
                }
            }


            private void updateIDPart(final AccountIterationSupport support, JSONArray jsonArray,
                    AtomicInteger number, Map<String, String> reqMap) {
                List<String> bulk = new ArrayList<>();
                support.iterate(reqMap, null, Arrays.asList(), Arrays.asList("id_parts:*"), Arrays.asList("id"), (rsp) -> {
                    Object identifier = rsp.getFieldValue("id");

                    bulk.add(identifier.toString());
                    if (bulk.size() >= LIMIT) {
                        number.addAndGet(bulk.size());
                        LOGGER.info(String.format("Bulk update %d", number.get()));
                        
                        JSONObject returnFromPost = PureHTTPSolrUtils.bulkField(bulk,"id", support.getCollection(), 
                            (id) -> {
                                String idParts = ZadostUtils.idParts(id);
                                return "<field name="+"\"id_parts\" update=\"set\">"+idParts+"</field>";
                            }
                        );
                        jsonArray.put(returnFromPost);
                        bulk.clear();
                    }
                }, "id");
                if (!bulk.isEmpty()) {
                    number.addAndGet(bulk.size());
                    JSONObject returnFromPost = PureHTTPSolrUtils.bulkField(bulk,"id", support.getCollection(),
                            (id) -> {
                                String idParts = ZadostUtils.idParts(id);
                                return "<field name="+"\"id_parts\" update=\"set\">"+idParts+"</field>";
                            }
                    );
                    bulk.clear();
                    jsonArray.put(returnFromPost);
                }
            }

            private void updateTypeOfReQuestUSER(final AccountIterationSupport support, JSONArray jsonArray,
                    AtomicInteger number, Map<String, String> reqMap) {
                List<String> bulk = new ArrayList<>();
                support.iterate(reqMap, null, Arrays.asList(), Arrays.asList("type_of_request:*"), Arrays.asList("id"), (rsp) -> {
                    Object identifier = rsp.getFieldValue("id");

                    bulk.add(identifier.toString());
                    if (bulk.size() >= LIMIT) {
                        number.addAndGet(bulk.size());
                        LOGGER.info(String.format("Bulk update %d", number.get()));
                        JSONObject returnFromPost = PureHTTPSolrUtils.bulkField(bulk,"id", support.getCollection(),
                            "<field name="+"\"type_of_request\" update=\"set\">user</field>"
                        );
                        jsonArray.put(returnFromPost);
                        bulk.clear();
                    }
                }, "id");
                if (!bulk.isEmpty()) {
                    number.addAndGet(bulk.size());
                    JSONObject returnFromPost = PureHTTPSolrUtils.bulkField(bulk,"id", support.getCollection(),
                            "<field name="+"\"type_of_request\" update=\"set\">user</field>"
                    );
                    bulk.clear();
                    jsonArray.put(returnFromPost);
                }
            }
        },

        // vyhledava zadosti
        TOUCH {

            private static final int LIMIT = 1000;

            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeCalledFromLocalhost()).permit()) {
                    long start = System.currentTimeMillis();
                    final AccountIterationSupport support = new AccountIterationSupport();
                    try {
                        JSONArray jsonArray = new JSONArray();
                        AtomicInteger number = new AtomicInteger(0);
                        Map<String, String> reqMap = new HashMap<>();
                        reqMap.put("rows", "" + LIMIT);

                        List<String> bulk = new ArrayList<>();
                        support.iterate(reqMap, null, new ArrayList<String>(), new ArrayList<String>(), Arrays.asList("id"), (rsp) -> {
                            Object identifier = rsp.getFieldValue("id");

                            bulk.add(identifier.toString());
                            if (bulk.size() >= LIMIT) {
                                number.addAndGet(bulk.size());
                                LOGGER.info(String.format("Bulk update %d", number.get()));
                                JSONObject returnFromPost = PureHTTPSolrUtils.touchBulk(bulk,"id", support.getCollection());
                                jsonArray.put(returnFromPost);
                                bulk.clear();
                            }
                        }, "id");
                        if (!bulk.isEmpty()) {
                            number.addAndGet(bulk.size());
                            JSONObject returnFromPost = PureHTTPSolrUtils.touchBulk(bulk,"id", support.getCollection());
                            bulk.clear();
                            jsonArray.put(returnFromPost);
                        }

                        JSONObject object = new JSONObject();
                        object.put("numberOfObjects", number.get());
                        object.put("bulkResults", jsonArray);
                        return object;

                    } catch (Exception ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                        return errorJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.toString());
                    } finally {
                        QuartzUtils.printDuration(LOGGER, start);
                        PureHTTPSolrUtils.commit(support.getCollection());
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
                }
            }
        },

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
                    String typeOfReq = req.getParameter("type_of_request");
                    String sort = req.getParameter("sort_account");
                    if (sort == null) {
                        sort = req.getParameter("user_sort_account");
                    }
                    // override q 
                    String prefix = req.getParameter("prefix");
                    if (prefix != null && StringUtils.isAnyString(prefix)) {
                        // oai identifier; exact match
                        boolean exactMatch = prefix.startsWith("oai:aleph-nkp.cz");
                        prefix = prefix.replaceAll("\\:", "\\\\:");
                        if (!exactMatch) {
                            q = String.format("%s*", prefix);
                        } else {
                            q = String.format("%s", prefix);
                        }
                        
                    }
                    String page = req.getParameter("page");
                    String rows = req.getParameter("rows");

                    try {
                        UserControlerImpl uc = new UserControlerImpl(req);
                        AccountService service = new AccountServiceImpl(uc, new ResourceBundleServiceImpl(req));
                        return VersionStringCast.cast(service.search(q, state, Arrays.asList(navrh), institution, priority, delegated, typeOfReq, sort, rows != null ? Integer.parseInt(rows) : -1, page != null ? Integer.parseInt(page) : -1));
                    } catch (SolrServerException | IOException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                        return errorJson(response, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
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

                        UserControlerImpl uc = new UserControlerImpl(req);
                        AccountService service = new AccountServiceImpl(uc, new ResourceBundleServiceImpl(req));
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
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {

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
                        UserControlerImpl uc = new UserControlerImpl(req);
                        AccountService service = new AccountServiceImpl(uc, new ResourceBundleServiceImpl(req));
                        return service.getRecords(req.getParameter("id"), rows, start);
                    } catch (SolrServerException | IOException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                        return errorJson(response, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
                }
            }
        },

        GET_ZADOST_INVALID_RECORDS {

            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {

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
                        UserControlerImpl uc = new UserControlerImpl(req);
                        AccountService service = new AccountServiceImpl(uc, new ResourceBundleServiceImpl(req));
                        return service.getInvalidRecords(req.getParameter("id"));
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
                        return service.saveRequest(readInputJSON(req).toString(), null);

                    } catch (ConflictException cex) {
                        LOGGER.log(Level.SEVERE, null, cex);
                        return errorJson(response, SC_CONFLICT, cex.getKey(), cex.getMessage());
                    } catch (AccountException cex) {
                        LOGGER.log(Level.SEVERE, null, cex);
                        return errorJson(response, SC_FORBIDDEN, cex.getKey(), cex.getMessage());
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
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {

                if (new RightsResolver(req, new MustBeLogged()).permit()) {
                    try {
                        return Zadost.saveWithFRBR(readInputJSON(req).toString(), new UserControlerImpl(req).getUser().getUsername(), req.getParameter("frbr"));
                    } catch (Exception e) {
                        return errorJson(response, SC_INTERNAL_SERVER_ERROR, e.getMessage());
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
                }
            }
        },
        // posune zadost ve workflow
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
                    return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
                }
            }
        },
        
        /** Batch approve */
        APPROVE_BATCH {

            @Override
            JSONObject doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {
                if (new RightsResolver(request, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {
                    try {
                        UserControlerImpl userControler = new UserControlerImpl(request);
                        AccountService service = new AccountServiceImpl(userControler, new ResourceBundleServiceImpl(request));

                        JSONObject inputJs = ServletsSupport.readInputJSON(request);
                        JSONObject zadostJSON = inputJs.getJSONObject("zadost");
                        Zadost zadost = Zadost.fromJSON(zadostJSON.toString());
                        String optionsJSON = inputJs.has("options") && !inputJs.isNull("options") ? inputJs.getString("options") : null;
                        
                        List<String> identifiers = Actions.identifiers(inputJs);
                        Map<String, AccountException> failedIdentifiers = new HashMap<>();
                     
                        zadostJSON = service.curatorSwitchStateBatch(zadostJSON, optionsJSON, identifiers, inputJs.getString("reason"), (f)-> {
                            f.keySet().forEach(k-> {
                               failedIdentifiers.put(k, f.get(k)); 
                            });
                        });
                        
                        service.commit(DataCollections.catalog.name(),DataCollections.zadost.name(), DataCollections.history.name());

                        JSONObject payload = VersionStringCast.cast(service.getRequest(zadost.getId()));
                        if (!failedIdentifiers.isEmpty()) {
                            // TODO: Multiple exceptions
                            List<AccountException> exceptionList = failedIdentifiers.values().stream().collect(Collectors.toList());
                            if (!exceptionList.isEmpty()) {
                                //AccountException ex = exceptionList.get(0);
                                return errorJson(response,SC_BAD_REQUEST, "account.noworkflowstates","account.noworkflowstates", payload);
                            } else {
                                return errorJson(response,SC_BAD_REQUEST, "account.noworkflowstates","account.noworkflowstates", payload);
                            }
                        } else {
                            return payload;
                        }
                    } catch (ConflictException e) {
                        return errorJson(response, SC_CONFLICT, e.getKey(), e.getMessage());
                    } catch (AccountException e) {
                        return errorJson(response, SC_BAD_REQUEST, e.getKey(), e.getMessage());
                    } catch (JSONException | SolrServerException e) {
                        return errorJson(response, SC_INTERNAL_SERVER_ERROR, e.getMessage());
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
                }
            }
            
        },

        APPROVE {
            @Override
            JSONObject doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {
                if (new RightsResolver(request, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {
                    try {
                        UserControlerImpl userControler = new UserControlerImpl(request);
                        AccountService service = new AccountServiceImpl(userControler, new ResourceBundleServiceImpl(request));

                        JSONObject inputJs = ServletsSupport.readInputJSON(request);
                        JSONObject zadostJSON = inputJs.getJSONObject("zadost");
                        Zadost zadost = Zadost.fromJSON(zadostJSON.toString());
                        String optionsJSON = inputJs.has("options") && !inputJs.isNull("options") ? inputJs.getString("options") : null;
                        
                        List<String> identifiers = Actions.identifiers(inputJs);
                        Map<String, AccountException> failedIdentifiers = new HashMap<>();
                        for (String identifier : identifiers) {
                            // pokracovat, pokud identifier nemuze zmenit stav
                            try {
                                String alternativeState = request.getParameter("alternative");
                                if (alternativeState != null) {
                                    zadostJSON = service.curatorSwitchAlternativeState(alternativeState, zadostJSON, optionsJSON, identifier, inputJs.getString("reason"));
                                } else {
                                    zadostJSON = service.curatorSwitchState(zadostJSON, optionsJSON, identifier, inputJs.getString("reason"));
                                }
                            } catch (AccountException e) {
                                failedIdentifiers.put(identifier, e);
                            }
                        }
                        // tady je commit
                        service.commit(DataCollections.catalog.name(),DataCollections.zadost.name(), DataCollections.history.name());
                        JSONObject payload = VersionStringCast.cast(service.getRequest(zadost.getId()));
                        if (!failedIdentifiers.isEmpty()) {
                            // TODO: Multiple exceptions
                            List<AccountException> exceptionList = failedIdentifiers.values().stream().collect(Collectors.toList());
                            if (!exceptionList.isEmpty()) {
                                //AccountException ex = exceptionList.get(0);
                                return errorJson(response,SC_BAD_REQUEST, "account.noworkflowstates","account.noworkflowstates", payload);
                            } else {
                                return errorJson(response,SC_BAD_REQUEST, "account.noworkflowstates","account.noworkflowstates", payload);
                            }
                        } else {
                            return payload;
                        }
                    } catch (ConflictException e) {
                        return errorJson(response, SC_CONFLICT, e.getKey(), e.getMessage());
                    } catch (AccountException e) {
                        return errorJson(response, SC_BAD_REQUEST, e.getKey(), e.getMessage());
                    } catch (JSONException | SolrServerException e) {
                        return errorJson(response, SC_INTERNAL_SERVER_ERROR, e.getMessage());
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
                }
            }
        },

        
        REJECT_BATCH {
            @Override
            JSONObject doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {
                if (new RightsResolver(request, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {
                    UserControlerImpl userControler = new UserControlerImpl(request);
                    AccountService service = new AccountServiceImpl(userControler, new ResourceBundleServiceImpl(request));
                    JSONObject inputJs = ServletsSupport.readInputJSON(request);
                    JSONObject zadostJSON = inputJs.getJSONObject("zadost");
                    Zadost zadost = Zadost.fromJSON(zadostJSON.toString());
                    List<String> identifiers = Actions.identifiers(inputJs);
                    JSONObject retObject = null;
                    zadostJSON = service.curatorRejectStateBatch(zadostJSON, identifiers, inputJs.getString("reason"), (а)->{});
                    service.commit(DataCollections.catalog.name(),DataCollections.zadost.name(),DataCollections.history.name());
                    return VersionStringCast.cast(service.getRequest(zadost.getId()));
                } else {
                    return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
                }
            }
            
        },
        
        REJECT {
            @Override
            JSONObject doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {
                if (new RightsResolver(request, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {
                    UserControlerImpl userControler = new UserControlerImpl(request);
                    AccountService service = new AccountServiceImpl(userControler, new ResourceBundleServiceImpl(request));
                    JSONObject inputJs = ServletsSupport.readInputJSON(request);
                    JSONObject zadostJSON = inputJs.getJSONObject("zadost");
                    Zadost zadost = Zadost.fromJSON(zadostJSON.toString());
                    List<String> identifiers = Actions.identifiers(inputJs);
                    JSONObject retObject = null;
                    for (String identifier : identifiers) {
                        zadostJSON = service.curatorRejectSwitchState(zadostJSON, identifier, inputJs.getString("reason"));
                    }
                    service.commit(DataCollections.catalog.name(),DataCollections.zadost.name(),DataCollections.history.name());
                    return VersionStringCast.cast(service.getRequest(zadost.getId()));
                } else {
                    return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
                }
            }
        },

        DELETE {
            @Override
            JSONObject doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception {
                if (new RightsResolver(request, new MustBeLogged(), new UserMustBeInRole(user, knihovna, mainKurator, kurator, admin)).permit()) {
                    JSONObject inputJs = ServletsSupport.readInputJSON(request);

                    Zadost zadost = Zadost.fromJSON(inputJs.toString());

                    UserControlerImpl userControler = new UserControlerImpl(request);
                    User user = userControler.getUser();

                    List<String> ordinaryUsers = Arrays.asList(Role.user.name(), Role.knihovna.name());
                    List<String> kuratorsAndAdmins = Arrays.asList(Role.admin.name(), Role.kurator.name(), Role.mainKurator.name());

                    boolean deleteIsPossible = (ordinaryUsers.contains(user.getRole()) && zadost.getState().equals("open")) ||
                            (kuratorsAndAdmins.contains(user.getRole()));

                    if (deleteIsPossible) {
                        AccountService service = new AccountServiceImpl(userControler, new ResourceBundleServiceImpl(request));
                        return service.deleteRequest(zadost.toJSON().toString());
                    } else {
                        return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
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
                        // TODO: transactions (optimistic locking)
                        Indexer.changeStavDirect(inputJs.getString("identifier"),
                                inputJs.getString("newStav"),
                                inputJs.optString("newLicense"),
                                inputJs.getString("poznamka"),
                                inputJs.getJSONArray("granularity"),
                                user.getUsername());


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
                        return Indexer.approveInImport(inputJs.getString("identifier"), inputJs.getJSONObject("importId").toString(), user.getUsername());
                    } catch (Exception ex) {
                        LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                        return errorJson(response, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
                }
            }
        },

        IMPORT_DOCUMENT_CONTROLLED {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {
                    try {
                        User user = new UserControlerImpl(req).getUser();
                        JSONObject inputJs = ServletsSupport.readInputJSON(req);
                        // inputJs.put("controlled", true);
                        return Import.setControlled(inputJs.getString("id"), inputJs.getString("controlled_note"), user.getUsername());
                    } catch (Exception ex) {
                        LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                        return errorJson(response, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
                }
            }
        },

        IMPORT_PROCESSED {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {
                    try {
                        User user = new UserControlerImpl(req).getUser();
                        JSONObject inputJs = ServletsSupport.readInputJSON(req);
                        // inputJs.put("controlled", true);
                        return Import.setProcessed(inputJs.getString("id"), user.getUsername());
                    } catch (Exception ex) {
                        LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                        return errorJson(response, SC_INTERNAL_SERVER_ERROR, ex.getMessage());
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "notallowed", "not allowed");
                }
            }
        },

        IMPORT_STAV {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {
                    try {
                        User user = new UserControlerImpl(req).getUser();
                        JSONObject inputJs = ServletsSupport.readInputJSON(req);
                        // inputJs.put("controlled", true);
                        return Import.changeStav(inputJs, user.getUsername());
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
                        return Indexer.followRecord(req.getParameter("identifier"), user.getUsername(), NotificationInterval.mesic.valueOf(user.getNotifikaceInterval()), "true".equals(req.getParameter("follow")));
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
                    if (navrhy != null && navrhy.length > 0) {
                        //String navrh = req.getParameter("navrh");
                        JSONObject result = service.search(null, "open", Arrays.asList(navrhy), null, null, null, null, null, 100, 0);
                        if (result.has("response") && result.getJSONObject("response").has("numFound")) {
                            int numFound = result.getJSONObject("response").getInt("numFound");
                            if (numFound > 0) {
                                JSONArray docs = result.getJSONObject("response").getJSONArray("docs");
                                Zadost zadost = Zadost.fromJSON(docs.getJSONObject(0).toString());
                                return zadost.toJSON();
                            }
                        }
                        String navrh = navrhy[0];
                        return service.prepare(navrh);
                    } else {
                        return errorJson(response, SC_BAD_REQUEST, "missing navrh parameter");
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "not allowed");
                }
            }
        };

        private static List<String> identifiers(JSONObject inputJs) {
            List<String> identifiers = new ArrayList<>();
            //String identifier = inputJs.optString("identifier");
            if (inputJs.has("identifier")) {
                identifiers.add(inputJs.getString("identifier"));
            } else if (inputJs.has("identifiers")) {
                JSONArray identifiersJsonArray = inputJs.getJSONArray("identifiers");
                identifiersJsonArray.forEach(id -> {
                    identifiers.add(id.toString());
                });
            }
            return identifiers;
        }

        abstract JSONObject doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception;
    }

}

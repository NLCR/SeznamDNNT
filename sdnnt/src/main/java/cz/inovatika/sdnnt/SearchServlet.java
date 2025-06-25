package cz.inovatika.sdnnt;

import cz.inovatika.sdnnt.index.CatalogSearcher;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.rights.RightsResolver;
import cz.inovatika.sdnnt.rights.impl.predicates.MustBeLogged;
import cz.inovatika.sdnnt.rights.impl.predicates.UserMustBeInRole;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.StringUtils;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.json.JsonQueryRequest;
import org.apache.solr.client.solrj.request.json.TermsFacetMap;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.util.NamedList;
import org.json.JSONArray;
import org.json.JSONObject;

import static cz.inovatika.sdnnt.rights.Role.*;
import static cz.inovatika.sdnnt.utils.ServletsSupport.errorJson;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;

/**
 * @author alberto
 */
@WebServlet(value = "/search/*")
public class SearchServlet extends HttpServlet {

    public static final Logger LOGGER = Logger.getLogger(SearchServlet.class.getName());

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

    enum Actions {
        DETAILS {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin))
                        .permit()) {
                    CatalogSearcher searcher = new CatalogSearcher();
                    return searcher.details(req);
                } else {
                    return errorJson(response, SC_FORBIDDEN, "not allowed");
                }
            }
        },

        FACET {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                CatalogSearcher searcher = new CatalogSearcher();
                return searcher.facetSearch(req);
            }
        },

        CATALOG {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                CatalogSearcher searcher = new CatalogSearcher();
                return searcher.search(req);
            }
        },

        FRBR {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                CatalogSearcher searcher = new CatalogSearcher();
                return searcher.frbr(req.getParameter("frbr"));
            }
        },
        ACCOUNT {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeLogged()).permit()) {
                    JSONObject ret = new JSONObject();
                    Options opts = Options.getInstance();
                    try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host")).build()) {
                        String q = req.getParameter("q");
                        if (q == null) {
                            q = "*";
                        }
                        JSONObject user = (JSONObject) req.getSession().getAttribute("user");
                        if (user == null) {
                            ret.put("error", "Not logged");
                            return ret;
                        }
                        SolrQuery query = new SolrQuery(q)
                                .setRows(20)
                                .setParam("df", "fullText")
                                .setFacet(true).addFacetField("typ", "old_stav", "navrh")
                                .addFilterQuery("user:" + user.getString("name"))
                                .setParam("json.nl", "arrntv")
                                .setFields("*,raw:[json]");
                        QueryRequest qreq = new QueryRequest(query);
                        NoOpResponseParser rParser = new NoOpResponseParser();
                        rParser.setWriterType("json");
                        qreq.setResponseParser(rParser);
                        NamedList<Object> qresp = solr.request(qreq, "zadost");
                        return new JSONObject((String) qresp.get("response"));
                    } catch (SolrServerException | IOException ex) {
                        LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                        return errorJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.toString());
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "not allowed");
                }
            }
        },
        CATALOG_DOC {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                JSONObject ret = new JSONObject();
                Options opts = Options.getInstance();
                try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host")).build()) {
                    SolrQuery query = new SolrQuery("*")
                            .setRows(100)
                            .addFilterQuery("identifier:\"" + req.getParameter("identifier") + "\"")
                            .setFields("*,raw:[json],granularity:[json],masterlinks:[json],historie_stavu:[json],historie_kurator_stavu:[json]");
                    QueryRequest qreq = new QueryRequest(query);
                    NoOpResponseParser rParser = new NoOpResponseParser();
                    rParser.setWriterType("json");
                    qreq.setResponseParser(rParser);
                    NamedList<Object> qresp = solr.request(qreq, DataCollections.catalog.name());
                    return new JSONObject((String) qresp.get("response"));
                } catch (SolrServerException | IOException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                    return errorJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.toString());
                }
            }
        },
        HISTORY {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                JSONObject ret = new JSONObject();
                Options opts = Options.getInstance();
                try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host")).build()) {
                    SolrQuery query = new SolrQuery("*")
                            .setRows(100)
                            .addFilterQuery("identifier:\"" + req.getParameter("identifier") + "\"")
                            .setFields("*,changes:[json]");
                    QueryRequest qreq = new QueryRequest(query);
                    NoOpResponseParser rParser = new NoOpResponseParser();
                    rParser.setWriterType("json");
                    qreq.setResponseParser(rParser);
                    NamedList<Object> qresp = solr.request(qreq, "history");
                    return new JSONObject((String) qresp.get("response"));
                } catch (SolrServerException | IOException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                    return errorJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.toString());
                }
            }

        },
        STATS_HISTORY {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {

                //facet.range={!tag=r1}datum_stavu&facet.range.gap=+1MONTH&facet.range.start=NOW-1YEAR
                //&facet.range.end=NOW&facet.pivot={!range=r1}dntstav
                JSONObject ret = new JSONObject();
                Options opts = Options.getInstance();
                String interval = req.getParameter("interval");
                try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host")).build()) {
                    SolrQuery query = new SolrQuery("*")
                            .setRows(0)
                            .setFacet(true).setParam("json.nl", "arrntv")
                            .addFacetField("user")
                            .setParam("facet.range", "{!tag=r1}indextime")
                            .setParam("facet.range.gap", "+" + interval)
                            .setParam("facet.range.start", "NOW-1YEAR")
                            .setParam("facet.range.end", "NOW")
                            .setParam("facet.range.other", "all")
                            .setParam("facet.pivot", "{!range=r1}type", "{!range=r1}user");
                    QueryRequest qreq = new QueryRequest(query);
                    NoOpResponseParser rParser = new NoOpResponseParser();
                    rParser.setWriterType("json");
                    qreq.setResponseParser(rParser);
                    NamedList<Object> qresp = solr.request(qreq, "history");
                    return new JSONObject((String) qresp.get("response"));
                } catch (SolrServerException | IOException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                    return errorJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.toString());
                }
            }
        },


        PN_DEADLINE {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {
                    // string identifier
                    Options opts = Options.getInstance();

                    try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host")).build()) {
                        String identifier = req.getParameter("identifier");
                        // jobs
                        // konfigurace -- ??
                        String jobConfig = req.getParameter("job") != null ? req.getParameter("job") : "PNREQ";

                        String unit = "month";
                        long value = 10;
                        JSONObject configObject = Options.getInstance().getJSONObject("jobs").optJSONObject(jobConfig);
                        if (configObject != null) {
                            JSONObject configPN = configObject.optJSONObject("checkPN");
                            unit = configPN.optString("unit","months");
                            value = configPN.optLong("value",10);
                        }

                        SolrQuery query = new SolrQuery("*")
                                .setRows(100)
                                .addFilterQuery("identifier:\"" + identifier + "\"")
                                .setFields(MarcRecordFields.DATUM_KURATOR_STAV_FIELD);
                        QueryRequest qreq = new QueryRequest(query);
                        NoOpResponseParser rParser = new NoOpResponseParser();
                        rParser.setWriterType("json");
                        qreq.setResponseParser(rParser);
                        NamedList<Object> qresp = solr.request(qreq, DataCollections.catalog.name());

                        JSONObject jresp = new JSONObject((String) qresp.get("response")).getJSONObject("response");
                        JSONArray docs = jresp.getJSONArray("docs");
                        if (docs.length() > 0) {
                            JSONObject doc = docs.getJSONObject(0);
                            String datumKuratorStav = doc.getString(MarcRecordFields.DATUM_KURATOR_STAV_FIELD);

                            Instant parsedInstant = Instant.parse(datumKuratorStav);
                            ChronoUnit selected = ChronoUnit.DAYS;
                            ChronoUnit[] values = ChronoUnit.values();
                            for (ChronoUnit chUnit : values) {
                                if (chUnit.name().toLowerCase().equals(unit)) {
                                    selected = chUnit;
                                    break;
                                }
                            }

                            //LocalDateTime deadlineDateTime = LocalDateTime.ofInstant(parsedInstant, ZoneId.systemDefault());

                            ZonedDateTime deadlineDateTime = parsedInstant.atZone(ZoneId.systemDefault());

                            deadlineDateTime  = deadlineDateTime.plus(value, selected);

                            //ZonedDateTime deadlineZonedDateTime = deadlineInstant.atZone(ZoneId.systemDefault());
                            String formattedDeadline = deadlineDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

                            JSONObject object = new JSONObject();
                            object.put("job",jobConfig);
                            object.put("deadline",formattedDeadline);
                            object.put("unit",selected.name());
                            object.put("value",value);
                            return object;
                        } else {
                            return errorJson(response, HttpServletResponse.SC_BAD_REQUEST, "no record");
                        }
                    } catch (SolrServerException | IOException ex) {
                        LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                        return errorJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.toString());
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "not allowed");
                }
            }
        },

        EXPORTS {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {

                    JSONObject ret = new JSONObject();
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
                        SolrQuery query = new SolrQuery("*")
                                .setRows(rows)
                                .setStart(start)
                                .setSort("indextime", SolrQuery.ORDER.desc)
                                .setFacet(true)
                                .addFacetField("processed")
                                .setParam("json.nl", "arrntv")
                                .setFields("*");


                        QueryRequest qreq = new QueryRequest(query);
                        NoOpResponseParser rParser = new NoOpResponseParser();
                        rParser.setWriterType("json");
                        qreq.setResponseParser(rParser);
                        NamedList<Object> qresp = solr.request(qreq, "imports");

                        return new JSONObject((String) qresp.get("response"));
                    } catch (SolrServerException | IOException ex) {
                        LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                        return errorJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.toString());
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "not allowed");
                }

            }

        },


        IMPORT {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {
                    JSONObject ret = new JSONObject();
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
                        SolrQuery query = new SolrQuery("*")
                                .setRows(rows)
                                .setStart(start)
                                .setSort("indextime", SolrQuery.ORDER.desc)
                                .addFilterQuery("id:" + req.getParameter("id"))
                                .setFacet(true)
                                .addFacetField("origin")
                                .addFacetField("processed")
                                .setParam("json.nl", "arrntv")
                                .setFields("*");


                        QueryRequest qreq = new QueryRequest(query);
                        NoOpResponseParser rParser = new NoOpResponseParser();
                        rParser.setWriterType("json");
                        qreq.setResponseParser(rParser);
                        NamedList<Object> qresp = solr.request(qreq, "imports");

                        return new JSONObject((String) qresp.get("response"));
                    } catch (SolrServerException | IOException ex) {
                        LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                        return errorJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.toString());
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "not allowed");
                }


                //return ret;
            }
        },
        IMPORT_NOT_CONTROLLED {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {
                    JSONObject ret = new JSONObject();
                    Options opts = Options.getInstance();
                    try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host")).build()) {
                        SolrQuery query = new SolrQuery("*")
                                .setRows(0)
                                .addFilterQuery("import_id:" + req.getParameter("id"))
                                .addFilterQuery("controlled:false")
                                .addFilterQuery("dntstav:A OR dntstav:PA OR dntstav:NL")
                                .setFields("*,identifiers:[json],catalog:[json],item:[json]");

                        QueryRequest qreq = new QueryRequest(query);
                        NoOpResponseParser rParser = new NoOpResponseParser();
                        rParser.setWriterType("json");
                        qreq.setResponseParser(rParser);
                        NamedList<Object> qresp = solr.request(qreq, "imports_documents");

                        return new JSONObject((String) qresp.get("response"));
                    } catch (SolrServerException | IOException ex) {
                        LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                        return errorJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.toString());
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "not allowed");
                }

            }
        },


        IMPORT_DOCUMENTS {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin))
                        .permit()) {
                    JSONObject ret = new JSONObject();
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
                        SolrQuery query = new SolrQuery("*").setRows(rows).setStart(start)
                                .addSort("hit_type", SolrQuery.ORDER.asc)
                                .addSort("name", SolrQuery.ORDER.asc)
                                .addFilterQuery("import_id:" + req.getParameter("id"))
                                .addFilterQuery("hits_na_vyrazeni:[1 TO *]")
                                .setFacet(true)
                                .addFacetField("{!ex=dntstav}dntstav").addFacetField("{!ex=ct}controlled")
                                .setFacetMinCount(1).setParam("json.nl", "arrntv")
                                .setFields("*,identifiers:[json],catalog:[json],item:[json]");

                        if (req.getParameter("dntstav") != null) {
                            query.addFilterQuery("{!tag=dntstav}dntstav:" + req.getParameter("dntstav"));
                        }
                        if (req.getParameter("controlled") != null) {
                            query.addFilterQuery("{!tag=ct}controlled:" + req.getParameter("controlled"));
                        }

                        QueryRequest qreq = new QueryRequest(query);
                        NoOpResponseParser rParser = new NoOpResponseParser();
                        rParser.setWriterType("json");
                        qreq.setResponseParser(rParser);
                        NamedList<Object> qresp = solr.request(qreq, "imports_documents");

                        final List<String> identsid = new ArrayList<>();

                        JSONObject jsonVal = new JSONObject((String) qresp.get("response"));
                        JSONArray importDocs = jsonVal.getJSONObject("response").getJSONArray("docs");
                        for (int i = 0; i < importDocs.length(); i++) {
                            JSONObject iDoc = importDocs.getJSONObject(i);
                            iDoc.getJSONArray("identifiers").forEach(obj -> {
                                JSONObject jsonObj = (JSONObject) obj;
                                identsid.add(jsonObj.getString("identifier"));
                            });
                        }

                        Map<String, JSONObject> map = new HashMap<>();
                        int batchSize = 40;
                        int number = identsid.size() / batchSize;
                        number = number + ((identsid.size() % batchSize == 0) ? 0 : 1);

                        for (int j = 0; j < number; j++) {
                            int startOffset = j * batchSize;
                            int endOffset = Math.min((j + 1) * batchSize, identsid.size());
                            List<String> sublist = identsid.subList(startOffset, endOffset);
                            String collect = sublist.stream().map(it -> '"' + it + '"').collect(Collectors.joining(" OR "));


                            SolrQuery catalogQuery = new SolrQuery("*")
                                    .setRows(sublist.size())
                                    .setStart(0)
                                    .addFilterQuery("identifier:(" + collect + ")")
                                    .setFields("identifier,"
                                            + "dntstav,"
                                            + "license,"
                                            + "kuratorstav,"
                                            + "historie_stavu,"
                                            + "id_euipo,"
                                            + "historie_kurator_stavu,"
                                            + "granularity:json");

                            QueryRequest catalogReq = new QueryRequest(catalogQuery);
                            NoOpResponseParser catalogRParser = new NoOpResponseParser();
                            catalogRParser.setWriterType("json");
                            catalogReq.setResponseParser(rParser);
                            NamedList<Object> catalogResp = solr.request(catalogReq, DataCollections.catalog.name());
                            JSONObject catalogRespVal = new JSONObject((String) catalogResp.get("response"));

                            JSONArray catalogDocs = catalogRespVal.getJSONObject("response").getJSONArray("docs");
                            for (int i = 0; i < catalogDocs.length(); i++) {
                                JSONObject cDoc = catalogDocs.getJSONObject(i);
                                map.put(cDoc.getString("identifier"), cDoc);
                            }
                        }


                        List<String> fields = Arrays.asList(
                                MarcRecordFields.DNTSTAV_FIELD,
                                MarcRecordFields.LICENSE_FIELD,
                                MarcRecordFields.KURATORSTAV_FIELD,
                                MarcRecordFields.HISTORIE_STAVU_FIELD,
                                MarcRecordFields.ID_EUIPO,
                                MarcRecordFields.GRANULARITY_FIELD);


                        for (int i = 0; i < importDocs.length(); i++) {
                            JSONObject iDoc = importDocs.getJSONObject(i);
                            iDoc.getJSONArray("identifiers").forEach(obj -> {

                                JSONObject jsonObj = (JSONObject) obj;
                                String identifier = jsonObj.getString("identifier");
                                JSONObject catalogDoc = map.get(identifier);
                                if (catalogDoc != null) {

                                    final AtomicBoolean changedInImportFlag = new AtomicBoolean(false);
                                    fields.forEach(f -> {
                                        if (catalogDoc.has(f) && jsonObj.has(f)) {
                                            String importStringRepresentation = jsonObj.get(f).toString();
                                            String catalogStringRepresenation = catalogDoc.get(f).toString();
                                            if (importStringRepresentation != null && catalogStringRepresenation != null) {
                                                if (!importStringRepresentation.equals(catalogStringRepresenation)) {
                                                    jsonObj.put(f, catalogDoc.get(f));
                                                    changedInImportFlag.set(true);
                                                }
                                            } else if (catalogStringRepresenation == null) {
                                                jsonObj.remove(f);
                                            }

                                        } else if (!catalogDoc.has(f) && jsonObj.has(f)) {
                                            jsonObj.remove(f);
                                        } else if (catalogDoc.has(f) && !jsonObj.has(f)) {
                                            jsonObj.put(f, catalogDoc.get(f));
                                        }
                                    });

                                    if (changedInImportFlag.get()) {

                                        jsonObj.put("changedInImport", true);
                                    }
                                } else {
                                    LOGGER.warning(String.format("Identifier '%s' doesnt exist", identifier));
                                }
                            });
                        }


                        return jsonVal;
                    } catch (SolrServerException | IOException ex) {
                        LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                        return errorJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.toString());
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "not allowed");
                }
            }
        },


        IMPORTS {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {

                if (new RightsResolver(req, new MustBeLogged(), new UserMustBeInRole(mainKurator, kurator, admin)).permit()) {
                    Options opts = Options.getInstance();
                    try (SolrClient solr = new HttpSolrClient.Builder(opts.getString("solr.host")).build()) {

                        String page = req.getParameter("page");
                        page = page != null ? page : "0";
                        String rows = req.getParameter("rows");
                        rows = rows != null ? rows : "20";

                        JsonQueryRequest request = new JsonQueryRequest()
                                .setQuery("*:*")
                                .setLimit(Integer.parseInt(rows))
                                .setOffset(Integer.parseInt(page) * Integer.parseInt(rows))
                                .setSort("indextime desc")
                                .returnFields("*");


                        NoOpResponseParser rParser = new NoOpResponseParser();
                        rParser.setWriterType("json");
                        request.setResponseParser(rParser);
                        NamedList<Object> qresp = solr.request(request, "imports");
                        return new JSONObject((String) qresp.get("response"));
                    } catch (SolrServerException | IOException ex) {
                        LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                        return errorJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.toString());
                    }
                } else {
                    return errorJson(response, SC_FORBIDDEN, "not allowed");
                }
            }
        },
        GOOGLEBOOKS {
            @Override
            JSONObject doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception {
                JSONObject ret = new JSONObject();
                try {
                    String url = "https://books.google.com/books?jscmd=viewapi&callback=display_google&bibkeys=" + req.getParameter("id");
                    String jsonp = org.apache.commons.io.IOUtils.toString(new URL(url), "UTF-8");
                    String json = jsonp.substring("display_google(".length(), jsonp.length() - 2);
                    return new JSONObject(json);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                    return errorJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.toString());
                }
            }
        };

        abstract JSONObject doPerform(HttpServletRequest request, HttpServletResponse response) throws Exception;
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">

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

}

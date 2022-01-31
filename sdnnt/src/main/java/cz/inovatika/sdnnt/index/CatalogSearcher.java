/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt.index;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.utils.QueryUtils;
import cz.inovatika.sdnnt.model.User;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.model.workflow.ZadostTypNavrh;
import cz.inovatika.sdnnt.model.workflow.document.DocumentWorkflowFactory;
import cz.inovatika.sdnnt.rights.Role;
import cz.inovatika.sdnnt.services.impl.UserControlerImpl;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.NotificationUtils;
import cz.inovatika.sdnnt.utils.SearchResultsUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.util.NamedList;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author alberto
 */
public class CatalogSearcher {

    public static final Logger LOGGER = Logger.getLogger(CatalogSearcher.class.getName());

    public JSONObject frbr(String id) {
        JSONObject ret = new JSONObject();
        try {
            SolrClient solr = Indexer.getClient();
            SolrQuery query = new SolrQuery("frbr:\"" + id + "\"")
                    .setFields("*,raw:[json],granularity:[json],historie_stavu:[json],historie_kurator_stavu:[json]");

            // 50 is a maximum
            query.setRows(50);
            QueryRequest qreq = new QueryRequest(query);
            NoOpResponseParser rParser = new NoOpResponseParser();
            rParser.setWriterType("json");
            qreq.setResponseParser(rParser);
            NamedList<Object> qresp = solr.request(qreq, "catalog");
            ret = new JSONObject((String) qresp.get("response"));

        } catch (SolrServerException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            ret.put("error", ex);
        }
        return ret;
    }


    public JSONObject search(Map<String, String> req, List<String> filters, User user) {
        JSONObject ret = new JSONObject();
        try {
            SolrClient solr = Indexer.getClient();
            SolrQuery query = doQuery(req, filters, user);
            QueryRequest qreq = new QueryRequest(query);
            NoOpResponseParser rParser = new NoOpResponseParser();
            rParser.setWriterType("json");
            qreq.setResponseParser(rParser);
            NamedList<Object> qresp = solr.request(qreq, "catalog");
            ret = new JSONObject((String) qresp.get("response"));
            if (ret.getJSONObject("response").getInt("numFound") > 0) {

                List<String> ids = SearchResultsUtils.getIdsFromResult(ret);
                JSONArray zadosti = findZadosti(user, ids, "NOT state:processed");
                ret.put("zadosti", zadosti);
                if (user != null) {
                    JSONArray notifications = NotificationUtils.findNotifications(ids, user.getUsername(), Indexer.getClient());
                    ret.put("notifications", notifications);
                }


                Set<String> usedIdentifiers = new LinkedHashSet<>();
                zadosti.forEach(z -> {
                    Zadost zadost = Zadost.fromJSON(z.toString());
                    if (!zadost.getState().equals("processed")) {
                        zadost.getIdentifiers().forEach(usedIdentifiers::add);
                    }
                });

                JSONObject groupActions = new JSONObject();
                SearchResultsUtils.iterateResult(ret, (doc) -> {
                    String key = doc.getString(MarcRecordFields.IDENTIFIER_FIELD);
                    if (!usedIdentifiers.contains(key)) {
                        JSONObject jsonObject = new JSONObject();

                        JSONArray license = doc.optJSONArray(MarcRecordFields.LICENSE_FIELD);
                        JSONArray dntStavyJSONArray = doc.optJSONArray(MarcRecordFields.DNTSTAV_FIELD);
                        JSONArray kuratorStavyJSONArray = doc.optJSONArray(MarcRecordFields.KURATORSTAV_FIELD);

                        if (dntStavyJSONArray != null && kuratorStavyJSONArray != null) {
                            List<String> publicStates = new ArrayList<>();
                            dntStavyJSONArray.forEach(o -> {
                                publicStates.add(o.toString());
                            });

                            List<String> curatorStates = new ArrayList<>();
                            kuratorStavyJSONArray.forEach(o -> {
                                curatorStates.add(o.toString());
                            });

                            List<ZadostTypNavrh> zadostTypNavrhs = DocumentWorkflowFactory.canBePartOfZadost(curatorStates, publicStates, license != null && license.length() > 0 ? license.getString(0) : null);
                            JSONArray actions = new JSONArray();
                            zadostTypNavrhs.stream().map(ZadostTypNavrh::name).forEach(actions::put);
                            jsonObject.put("workflows", actions);
                        }
                        jsonObject.put(MarcRecordFields.DNTSTAV_FIELD, dntStavyJSONArray != null ? dntStavyJSONArray : new JSONArray());
                        groupActions.put(key, jsonObject);
                    }
                });
                ret.put("actions", groupActions);

            }
        } catch (SolrServerException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            ret.put("error", ex);
        }
        // remove public states facet values from curator states
        SearchResultsUtils.removePublicStatesFromCuratorStatesFacets(ret);
        return ret;
    }

    public JSONObject getById(String id, User user) {
        JSONObject ret = new JSONObject();
        try {
            SolrClient solr = Indexer.getClient();
            SolrQuery query = new SolrQuery("identifier:\"" + id + "\"")
                    .setRows(1)
                    .setStart(0)
                    .setSort("identifier", SolrQuery.ORDER.asc)
                    .setFields("*,raw:[json],granularity:[json],historie_stavu:[json],historie_kurator_stavu:[json]");

            QueryRequest qreq = new QueryRequest(query);
            NoOpResponseParser rParser = new NoOpResponseParser();
            rParser.setWriterType("json");
            qreq.setResponseParser(rParser);
            NamedList<Object> qresp = solr.request(qreq, "catalog");
            ret = new JSONObject((String) qresp.get("response"));
            if (ret.getJSONObject("response").getInt("numFound") > 0) {
                List<String> ids = SearchResultsUtils.getIdsFromResult(ret);
                JSONArray zadosti = findZadosti(user, ids);
                ret.put("zadosti", zadosti);
                if (user != null) {
                    JSONArray notifications = NotificationUtils.findNotifications(ids, user.getUsername(), Indexer.getClient());
                    ret.put("notifications", notifications);
                }
            }
        } catch (SolrServerException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            ret.put("error", ex);
        }
        return ret;
    }

    public JSONObject search(HttpServletRequest req) {
        Map<String, String[]> parameterMap = req.getParameterMap();
        Map<String, String> resmap = new HashMap<>();
        parameterMap.entrySet().stream().forEach(stringEntry -> {
            resmap.put(stringEntry.getKey(), stringEntry.getValue()[0]);
        });
        //UserControler controler = new UserControlerImpl(req)
        User user = new UserControlerImpl(req).getUser();
        return search(resmap, new ArrayList<>(), user);
    }

    public JSONObject getA(Map<String, String> req, User user) {
        return getByStav(req, user, Arrays.asList("A"), new ArrayList<>());
    }

    public JSONObject getA(HttpServletRequest req) {
        Map<String, String[]> parameterMap = req.getParameterMap();
        Map<String, String> resmap = new HashMap<>();
        parameterMap.entrySet().stream().forEach(stringEntry -> {
            resmap.put(stringEntry.getKey(), stringEntry.getValue()[0]);
        });
        User user = new UserControlerImpl(req).getUser();
        return getA(resmap, user);
    }

    public JSONObject getPA(Map<String, String> req, User user) {
        return getByStav(req, user, Arrays.asList("PA"), new ArrayList<>());
    }


    private JSONObject getByStav(Map<String, String> req, User user, List<String> stavy, List<String> notStavy) {
        JSONObject ret = new JSONObject();
        try {
            SolrClient solr = Indexer.getClient();
            Options opts = Options.getInstance();
            int rows = opts.getClientConf().getInt("rows");
            if (req.containsKey("rows")) {
                rows = Integer.parseInt(req.get("rows"));
            }
            int start = 0;
            if (req.containsKey("page")) {
                start = Integer.parseInt(req.get("page")) * rows;
            }

            SolrQuery query = new SolrQuery("*")
                    .setRows(rows)
                    .setStart(start)
                    .setSort("identifier", SolrQuery.ORDER.asc)
                    .setFields("identifier,datum_stavu");


            stavy.stream().map(it -> "dntstav:" + it).forEach(query::addFilterQuery);
            notStavy.stream().map(it -> "NOT dntstav:" + it).forEach(query::addFilterQuery);

            QueryRequest qreq = new QueryRequest(query);
            NoOpResponseParser rParser = new NoOpResponseParser();
            rParser.setWriterType("json");
            qreq.setResponseParser(rParser);
            NamedList<Object> qresp = solr.request(qreq, "catalog");
            ret = new JSONObject((String) qresp.get("response"));

        } catch (SolrServerException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            ret.put("error", ex);
        }
        return ret;
    }


    private JSONArray findZadosti(User user, List<String> identifiers, String... additionalFilters) {
        try {
            if (!identifiers.isEmpty()) {
                SolrClient solr = Indexer.getClient();
                String q = identifiers.toString().replace("[", "(").replace("]", ")").replaceAll(",", "");
                SolrQuery query = new SolrQuery("identifiers:" + q)
                        .setFields("*,process:[json]").setRows(100);

                // u uzivatele filtrujeme jenom pro konkrentniho uzivatele
                if (user != null && (user.getRole().equals(Role.user.name()) || user.getRole().equals(Role.knihovna.name()))) {
                    query = query.addFilterQuery("user:\"" + user.getUsername() + "\"");
                }

                for (String filter : additionalFilters) {
                    query = query.addFilterQuery(filter);
                }

                QueryRequest qreq = new QueryRequest(query);
                NoOpResponseParser rParser = new NoOpResponseParser();
                rParser.setWriterType("json");
                qreq.setResponseParser(rParser);
                NamedList<Object> qresp = solr.request(qreq, "zadost");
                return (new JSONObject((String) qresp.get("response"))).getJSONObject("response").getJSONArray("docs");
            } else return new JSONArray();
        } catch (SolrServerException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return null;
        }
    }

    //  "filterFields": ["dntstav", "item_type", "language", "marc_910a", "marc_856a", "nakladatel", "rokvydani"],
    private SolrQuery doQuery(HttpServletRequest req, User user) {
        Map<String, String> map = new HashMap<>();
        Enumeration<String> parameterNames = req.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
            String[] vals = req.getParameterMap().get(name);
            map.put(name, vals.length > 0 ? vals[0] : "");

        }
        return doQuery(map, new ArrayList<>(), user);
    }

    // dat to jinam
    public List<Pair<String, List<String>>> existingCatalogIdentifiersAndStates(List<String> identifiers) {
        try {

            List<Pair<String, List<String>>> retvals = new ArrayList<>();
            SolrClient solr = Indexer.getClient();
            String q = "(" + identifiers.stream().map(it -> "\"" + it + "\"").collect(Collectors.joining(" ")) + ")";

            SolrQuery query = new SolrQuery("*")
                    .addFilterQuery("identifier:" + q)
                    .addFilterQuery("dntstav:*")
                    .addField("identifier").addField("dntstav");

            QueryRequest qreq = new QueryRequest(query);
            NoOpResponseParser rParser = new NoOpResponseParser();
            rParser.setWriterType("json");
            qreq.setResponseParser(rParser);
            NamedList<Object> qresp = solr.request(qreq, "catalog");
            JSONArray jsonArray = (new JSONObject((String) qresp.get("response"))).getJSONObject("response").getJSONArray("docs");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject doc = jsonArray.getJSONObject(i);
                String identifier = doc.getString("identifier");
                List<String> states = new ArrayList<>();

                doc.getJSONArray("dntstav").forEach(obj -> {
                    states.add(obj.toString());
                });

                retvals.add(Pair.of(identifier, states));
            }
            return retvals;
        } catch (SolrServerException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return new ArrayList<>();
        }

    }

    private SolrQuery doQuery(Map<String, String> req, List<String> filters, User user) {
        String q = req.containsKey("q") ? req.get("q") : null;
        if (q == null) {
            q = "*";
        } else {
            q = QueryUtils.query(q);
        }
        Options opts = Options.getInstance();
        int rows = opts.getClientConf().getInt("rows");
        if (req.containsKey("rows")) {
            rows = Integer.parseInt(req.get("rows"));
        }
        int start = 0;
        if (req.containsKey("page")) {
            start = Integer.parseInt(req.get("page")) * rows;
        }
        // select from identifiers;
        // fulltext or id_pid or id_all_identifiers or id_all_identifiers_cuts
        //String modifiedQuery = String.format("fullText:%s OR id_pid:%s OR id_all_identifiers:%s OR id_all_identifiers_cuts:%s", q,q,q,q);
        SolrQuery query = new SolrQuery(q)
                .setRows(rows)
                .setStart(start)
                .setFacet(true).addFacetField("fmt", "language", "dntstav", "kuratorstav", "license", "sigla", "nakladatel")
                .setFacetMinCount(1)
                .setParam("json.nl", "arrntv")
                .setParam("stats", true)
                .setParam("stats.field", "rokvydani")
                .setParam("q.op", "AND")
                .setFields("*,raw:[json],granularity:[json],historie_stavu:[json],historie_kurator_stavu:[json]");


        query.set("defType", "edismax");
        query.set("qf", "title^3 id_pid^4 id_all_identifiers^4 id_all_identifiers_cuts^4 fullText license");


        if (req.containsKey("sort")) {
            if (req.get("sort").startsWith("date1")) {
                String dir = req.get("sort").split(" ")[1];
                query.addSort("date1_int", SolrQuery.ORDER.valueOf(dir));
                query.addSort("date2_int", SolrQuery.ORDER.valueOf(dir));
            } else {
                query.setParam("sort", req.get("sort"));
            }

        }

        // specific filters given from arguments
        filters.stream().forEach(query::addFilterQuery);

        for (Object o : opts.getClientConf().getJSONArray("filterFields")) {
            String field = (String) o;
            if (req.containsKey(field)) {
                if (field.equals("rokvydani")) {
                    query.addFilterQuery(field + ":[" + req.get(field).replace(",", " TO ") + "]");
                } else {
                    query.addFilterQuery(field + ":\"" + req.get(field) + "\"");
                }
            }
        }


        // Vseobecne filtry podle misto vydani (xr ) a roky

        // fq=fmt:BK%20AND%20place_of_pub:"xr%20"%20AND%20date1_int:%5B1910%20TO%202007%5D&fq=marc_338a:svazek&fq=-marc_245h:*&fq=marc_338b:nc&fq=marc_3382:rdacarrier
        int year = Calendar.getInstance().get(Calendar.YEAR);
        int fromYear = opts.getJSONObject("search").getInt("fromYear");
        int yearsBK = opts.getJSONObject("search").getInt("yearsBK");
        String bkDate = "((date1_int:["
                + fromYear
                + " TO "
                + (year - yearsBK)
                + "] AND -date2_int:*"
                + ") OR " + "(date1_int:["
                + fromYear
                + " TO "
                + (year - yearsBK)
                + "] AND date2_int:["
                + fromYear
                + " TO "
                + (year - yearsBK)
                + "]))";

        String bk = "(fmt:BK AND " + bkDate + ")";

        int yearsSE = opts.getJSONObject("search").getInt("yearsSE");
        String seDate = "((date1_int:["
                + fromYear
                + " TO "
                + (year - yearsSE)
                + "] AND date2_int:9999"
                + ") OR " + "date2_int:["
                + fromYear
                + " TO "
                + (year - yearsSE)
                + "])";
        String se = "(fmt:SE AND " + seDate + ")";

        // https://github.com/NLCR/SeznamDNNT/issues/164
        query.addFilterQuery("place_of_pub:\"xr \"");

        query.addFilterQuery(bk + " OR " + se + " OR dntstav:*");


        // Filtry podle role
        if (!"true".equals(req.get("fullCatalog")) || user == null || "user".equals(user.getRole())) {
            query.addFilterQuery("dntstav:*");
        }

        if (user == null || "user".equals(user.getRole())) {
            query.addFilterQuery("-dntstav:X");
        }

        if ("true".equals(req.get("withNotification"))) {
            query.addFilterQuery("{!join fromIndex=notifications from=identifier to=identifier} user:" + user.getUsername());
        }


        return query;
    }

}

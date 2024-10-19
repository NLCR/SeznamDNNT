/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt.index;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.utils.QueryUtils;
import cz.inovatika.sdnnt.indexer.models.notifications.AbstractNotification;
import cz.inovatika.sdnnt.indexer.models.notifications.RuleNotification;
import cz.inovatika.sdnnt.indexer.models.notifications.AbstractNotification.TYPE;
import cz.inovatika.sdnnt.model.DataCollections;
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
import cz.inovatika.sdnnt.services.NotificationsService;
import cz.inovatika.sdnnt.services.exceptions.NotificationsException;
import cz.inovatika.sdnnt.services.impl.NotificationServiceImpl;
import cz.inovatika.sdnnt.services.impl.users.UserControlerImpl;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.NotificationUtils;
import cz.inovatika.sdnnt.utils.SearchResultsUtils;
import cz.inovatika.sdnnt.utils.StringUtils;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.util.NamedList;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author alberto
 */
public class CatalogSearcher {
    
    private static final String DEFAULT_FIELDLIST = "*,raw:[json],masterlinks:[json],granularity:[json],historie_stavu:[json],historie_kurator_stavu:[json],historie_granulovaneho_stavu:[json]";

    public static final String ID_PREFIX = "oai:aleph-nkp.cz:";
    
    public static final Logger LOGGER = Logger.getLogger(CatalogSearcher.class.getName());
    
    
    private String fieldList;
    
    public CatalogSearcher(String fieldList) {
        super();
        this.fieldList = fieldList;
    }


    public CatalogSearcher() {
        super();
    }


    public JSONObject frbr(String id) {
        JSONObject ret = new JSONObject();
        try {
            SolrClient solr = Indexer.getClient();
            SolrQuery query = new SolrQuery("frbr:\"" + id + "\"")
                    .setFields("*,raw:[json],granularity:[json], masterlinks:[json],historie_stavu:[json],historie_kurator_stavu:[json],historie_granulovaneho_stavu:[json]");

            // 50 is a maximum
            query.setRows(50);
            QueryRequest qreq = new QueryRequest(query);
            NoOpResponseParser rParser = new NoOpResponseParser();
            rParser.setWriterType("json");
            qreq.setResponseParser(rParser);
            NamedList<Object> qresp = solr.request(qreq, DataCollections.catalog.name());
            ret = new JSONObject((String) qresp.get("response"));

        } catch (SolrServerException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            ret.put("error", ex);
        }
        return ret;
    }
    

    public JSONObject facetSearch(HttpServletRequest req) {
        Map<String, String[]> parameterMap = req.getParameterMap();
        Map<String, List<String>> resmap = new HashMap<>();
        parameterMap.entrySet().stream().forEach(stringEntry -> {
            List<String> vals = new ArrayList<String>(Arrays.asList(stringEntry.getValue()));
            resmap.put(stringEntry.getKey(), vals);
        });
        User user = new UserControlerImpl(req).getUser();
        String facetSearchField = req.getParameter("facetSearchField");
        String facetSearchOffset = req.getParameter("facetSearchOffset");
        String facetPrefix = req.getParameter("facetSearchPrefix");
        return facetSearch(resmap, new ArrayList<>(), user, facetSearchField, facetSearchOffset,facetPrefix);
    }

    
    public JSONObject facetSearch(Map<String, List<String>> req, List<String> filters, User user, String facetField, String offset, String facetPrefix) {
        JSONObject ret = new JSONObject();
        try {
            SolrClient solr = Indexer.getClient();

            SolrQuery query = doQuery(req, filters, user, (solrQuery)->{
                solrQuery.setFacet(true).addFacetField(facetField);
                solrQuery.setFacetMinCount(1);
                if (StringUtils.isAnyString(facetPrefix)) {
                    solrQuery.setFacetPrefix(facetPrefix);
                }
                solrQuery.setParam(FacetParams.FACET_OFFSET, offset);
            }); 
            
            query.setRows(0).setStart(0);
            QueryRequest qreq = new QueryRequest(query);
            NoOpResponseParser rParser = new NoOpResponseParser();
            rParser.setWriterType("json");
            qreq.setResponseParser(rParser);
            NamedList<Object> qresp = solr.request(qreq, DataCollections.catalog.name());
            ret = new JSONObject((String) qresp.get("response"));
            return ret;
        } catch (SolrServerException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            ret.put("error", ex);
        }
        SearchResultsUtils.removePublicStatesFromCuratorStatesFacets(ret);
        return ret;
    }
    
    
    public JSONObject search(Map<String, List<String>> req, List<String> filters, User user) {
        JSONObject ret = new JSONObject();
        try {
            SolrClient solr = Indexer.getClient();
            SolrQuery query = doQuery(req, filters, user,(solrQuery)-> {
                solrQuery.setFacet(true).addFacetField("fmt", "language", "dntstav", "kuratorstav", "license", "sigla", "nakladatel","digital_libraries", "export","id_euipo_export","c_actions");
                solrQuery.setFacetMinCount(1);
            });
            QueryRequest qreq = new QueryRequest(query);
            NoOpResponseParser rParser = new NoOpResponseParser();
            rParser.setWriterType("json");
            qreq.setResponseParser(rParser);
            NamedList<Object> qresp = solr.request(qreq, DataCollections.catalog.name());
            ret = new JSONObject((String) qresp.get("response"));
            if (ret.getJSONObject("response").getInt("numFound") > 0) {

                List<String> ids = SearchResultsUtils.getIdsFromResult(ret);
                JSONArray zadosti = user != null ? findZadosti(user, ids, "NOT state:processed") : new JSONArray();
                ret.put("zadosti", zadosti);
                
                if (user != null) {
                    JSONArray notifications = NotificationUtils.applySimpleNotifications(ids, user.getUsername(), Indexer.getClient());
                    ret.put("notifications", notifications);

                    JSONArray ruleNotifications = user != null ? NotificationUtils.applyRuleNotifications(user, ids) : new JSONArray();
                    ret.put("rnotifications", ruleNotifications);
                    
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
                        } else {
                            // out of list
                            List<ZadostTypNavrh> zadostTypNavrhs = DocumentWorkflowFactory.canBePartOfZadost(new ArrayList<>(), new ArrayList<>(), null);
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
                    .setFields("*,raw:[json],granularity:[json],masterlinks:[json],historie_stavu:[json],historie_kurator_stavu:[json],historie_granulovaneho_stavu:[json]");

            QueryRequest qreq = new QueryRequest(query);
            NoOpResponseParser rParser = new NoOpResponseParser();
            rParser.setWriterType("json");
            qreq.setResponseParser(rParser);
            NamedList<Object> qresp = solr.request(qreq, DataCollections.catalog.name());
            ret = new JSONObject((String) qresp.get("response"));
            if (ret.getJSONObject("response").getInt("numFound") > 0) {
                List<String> ids = SearchResultsUtils.getIdsFromResult(ret);
                JSONArray zadosti = findZadosti(user, ids);
                ret.put("zadosti", zadosti);
                if (user != null) {
                    JSONArray notifications = NotificationUtils.applySimpleNotifications(ids, user.getUsername(), Indexer.getClient());
                    ret.put("notifications", notifications);
                }
            }
        } catch (SolrServerException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            ret.put("error", ex);
        }
        return ret;
    }
    
    public JSONObject details(HttpServletRequest req) {
        Map<String, String[]> parameterMap = req.getParameterMap();
        Map<String, String> resmap = new HashMap<>();
        parameterMap.entrySet().stream().forEach(stringEntry -> {
            resmap.put(stringEntry.getKey(), stringEntry.getValue()[0]);
        });
        return details(resmap);
    }
    
    public JSONObject details(Map<String, String> req) {
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
            
            String identifiers = req.get("identifiers");
            String q = "("+Arrays.stream(identifiers.split(",")).map(it-> '"'+it+'"').collect(Collectors.joining(" OR "))+")";
            
            SolrQuery query = new SolrQuery("identifier:"+q)
                    .setRows(rows)
                    .setStart(start)
                    .setSort("identifier", SolrQuery.ORDER.asc);
            
            QueryRequest qreq = new QueryRequest(query);
            NoOpResponseParser rParser = new NoOpResponseParser();
            rParser.setWriterType("json");
            qreq.setResponseParser(rParser);
            NamedList<Object> qresp = solr.request(qreq, DataCollections.catalog.name());
            ret = new JSONObject((String) qresp.get("response"));

        } catch (SolrServerException | IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            ret.put("error", ex);
        }
        return ret;
        
    }
    
    public JSONObject search(HttpServletRequest req) {
        Map<String, String[]> parameterMap = req.getParameterMap();
        Map<String, List<String>> resmap = new HashMap<>();
        parameterMap.entrySet().stream().forEach(stringEntry -> {
            List<String> list = new ArrayList<>(Arrays.asList(stringEntry.getValue()));
            resmap.put(stringEntry.getKey(), list);
        });
        //UserControler controler = new UserControlerImpl(req)
        User user = new UserControlerImpl(req).getUser();
        return search(resmap, new ArrayList<>(), user);
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
            NamedList<Object> qresp = solr.request(qreq, DataCollections.catalog.name());
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
    private SolrQuery doQuery(HttpServletRequest req, User user, FacetFieldConfigObject facetConfig) {
        Map<String, List<String>> map = new HashMap<>();
        Enumeration<String> parameterNames = req.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
            String[] vals = req.getParameterMap().get(name);
            map.put(name, new ArrayList<>(Arrays.asList(vals)));
        }
        return doQuery(map, new ArrayList<>(), user, facetConfig);
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
            NamedList<Object> qresp = solr.request(qreq, DataCollections.catalog.name());
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

    /** facet field configuration */
    @FunctionalInterface
    public static interface FacetFieldConfigObject {
        public void configFacets(SolrQuery sQuery);
    }
    
    
    private SolrQuery doQuery(Map<String, List<String>> req, List<String> filters, User user, FacetFieldConfigObject confobject) {
        List<String> qlist = req.containsKey("q") ? req.get("q") : null;
        String q = null;
        if (qlist == null || qlist.size() == 0) {
            q = "*";
        } else {
            q = QueryUtils.query(qlist.get(0));
        }
        Options opts = Options.getInstance();
        int rows = opts.getClientConf().getInt("rows");
        if (req.containsKey("rows")) {
            rows = Integer.parseInt(req.get("rows").get(0));
        }
        int start = 0;
        if (req.containsKey("page")) {
            start = Integer.parseInt(req.get("page").get(0)) * rows;
        }
        SolrQuery query = new SolrQuery(q)
                .setRows(rows)
                .setStart(start)
                .setParam("json.nl", "arrntv")
                .setParam("stats", true)
                
                .setParam("stats.field", "date1_int")
                .setParam("q.op", "AND");

        /** Facet fields configuration */
        if (confobject != null) {
            confobject.configFacets(query);
        }
        
        if (this.fieldList != null) {
            query.setFields(this.fieldList);
        } else {
            query.setFields(DEFAULT_FIELDLIST);
        }
        
        
        query.set("defType", "edismax");
        if (q.startsWith(QueryUtils.IDENTIFIER_PREFIX) ||q.startsWith('"'+QueryUtils.IDENTIFIER_PREFIX)) {
            query.set("qf", "identifier");
        } else {
            query.set("qf", "title^3 id_pid^4 id_all_identifiers^4 id_all_identifiers_cuts^4 fullText license");
        }

        if (req.containsKey("sort")) {
            String sort = req.get("sort").get(0);
            if (sort.startsWith("date1")) {
                String dir = sort.split(" ")[1];
                query.addSort("date1_int", SolrQuery.ORDER.valueOf(dir));
                query.addSort("date2_int", SolrQuery.ORDER.valueOf(dir));
            } else {
                query.setParam("sort", sort);
            }

        }

        // specific filters given from arguments
        filters.stream().forEach(query::addFilterQuery);

        for (Object o : opts.getClientConf().getJSONArray("filterFields")) {
            String field = (String) o;
            if (req.containsKey(field)) {
                if (field.equals("rokvydani")) {
                    String  rokVydani = req.get(field).get(0);
                    String[] limits = rokVydani.split(",");
                    if(limits.length >= 2) {
                        String lowerUI = limits[0];
                        String upperUI = limits[1];
                        
                        String case1= String.format("date1_int:[%s TO %s]", lowerUI,upperUI); // case 1
                        String case2 =  String.format("date2_int:[%s TO %s]", lowerUI,upperUI); //case 2
                        String case3 =  String.format("(date1_int:[* TO %s] AND date2_int:[%s TO *])", upperUI,lowerUI); //case 2
                        String orcase = Arrays.asList(case1,case2,case3).stream().collect(Collectors.joining(" OR "));
                        query.addFilterQuery("("+orcase+")");
                    }
                } else {
                    List<String> vals = req.get(field);
                    if (vals.size() == 1) {
                        query.addFilterQuery(field + ":\"" + vals.get(0) + "\"");
                    } else if (vals.size() > 1){
                        String orCondition =  vals.stream().map(it-> { return '"'+it+'"'; }).collect(Collectors.joining(" OR "));
                        query.addFilterQuery(field + ":(" + orCondition + ")");
                    }
                }
            }
        }


        // Vseobecne filtry podle misto vydani (xr ) a roky
        // dat to mimo
        // fq=fmt:BK%20AND%20place_of_pub:"xr%20"%20AND%20date1_int:%5B1910%20TO%202007%5D&fq=marc_338a:svazek&fq=-marc_245h:*&fq=marc_338b:nc&fq=marc_3382:rdacarrier
        
        // From mail
        int year = Calendar.getInstance().get(Calendar.YEAR);
        int fromYear = opts.getJSONObject("search").getInt("fromYear");
        
        // Issue #510; Only lower bouds is used 
        String bk = QueryUtils.catalogBKFilterQueryPartOnlyLowerBound(opts, year, fromYear);
        String se = QueryUtils.catalogSEFilterQueryPartOnlyLowerBound(opts, year, fromYear);

        // https://github.com/NLCR/SeznamDNNT/issues/164
        query.addFilterQuery("(place_of_pub:\"xr \" OR dntstav:*)");

        query.addFilterQuery(bk + " OR " + se + " OR dntstav:*");

        // full catalog true = bez stavu 
        // user 

        
        
        // Filtry podle role;TODO - Delete it in future
//        if (req.containsKey("fullCatalog")) {
//            if (!"true".equals(req.get("fullCatalog")) || user == null || "user".equals(user.getRole())) {
//                query.addFilterQuery("dntstav:*");
//            }
//        }
        
        if (req.containsKey("fullCatalog")) {
            if ("true".equals(req.get("fullCatalog"))) {
                req.put("catalog", new ArrayList<String>(Arrays.asList("all")));
            }
        }        
        
        // catalog - in_list; outside_list; all
        if (user != null && user.getRole() != null && !user.getRole().equals("user")) {
            String catalog =  req.containsKey("catalog")  ? req.get("catalog").get(0) : "in_list";
            // in list - musi mit stav
            if (catalog.equals("in_list")) {
                query.addFilterQuery("dntstav:*");
            // outside list - nesmi mit stav
            } else  if (catalog.equals("outside_list")) {
                query.addFilterQuery("-dntstav:*");
            } 
        }
        
        // rusi  X D v pripade uzivatele
        ensureUserRoleFilter(user, query);
        // rusi D v pripade knihovny
        ensureKnihovnaRoleFilter(user, query);

        // notifikace
        if ("true".equals(req.get("withNotification"))) {
            // should be in service
            query.addFilterQuery("{!join fromIndex=notifications from=identifier to=identifier} user:" + user.getUsername());
            
        }

        // Notification filter
        if (user != null && req.containsKey("notificationFilter")) {
            try {
                String nFilter = req.get("notificationFilter").get(0);
                if (nFilter.equals("simple")) {
                    // should be in service
                    query.addFilterQuery("{!join fromIndex=notifications from=identifier to=identifier} user:" + user.getUsername());
                } else {
                    NotificationsService notifService = new NotificationServiceImpl(null, null);
                    AbstractNotification anotif = notifService.findNotificationByUserAndId(user.getUsername(),nFilter);
                    if (anotif != null &&  anotif.getType().equals(TYPE.rule.name())) {
                        query.addFilterQuery(((RuleNotification)anotif).provideSearchQueryFilters());
                    }
                }
                
            } catch (NotificationsException e) {
                LOGGER.log(Level.SEVERE,e.getMessage(),e);
            }
        }
        return query;
    }


    private void ensureUserRoleFilter(User user, SolrQuery query) {
        if (user == null || "user".equals(user.getRole())) {

            query.addFilterQuery("dntstav:*");
            
            query.addFilterQuery("-dntstav:X");
            query.addFilterQuery("-dntstav:D");
        }
    }

    private void ensureKnihovnaRoleFilter(User user, SolrQuery query) {
        if (user != null && "knihovna".equals(user.getRole())) {
            query.addFilterQuery("-dntstav:D");
        }
    }

}

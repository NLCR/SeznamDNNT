package cz.inovatika.sdnnt.tools;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CursorMarkParams;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Finds catalog records whose raw MARC JSON contains both data fields 991 and 992.
 */
public class FindPozIstFields {

    private static final String DEFAULT_SOLR_URL = "http://localhost:8983/solr";
    private static final String DEFAULT_COLLECTION = "catalog";
    private static final String DEFAULT_QUERY = "setSpec:\"DNT-ALL\"";
    private static final int DEFAULT_ROWS = 1000;

    public static void main(String[] args) throws Exception {
        Config config = Config.parse(args);
        if (config.help) {
            printHelp();
            return;
        }

        try (SolrClient solr = new HttpSolrClient.Builder(config.solrUrl).build();
             OutputStream output = config.outputFile == null ? System.out : new FileOutputStream(config.outputFile);
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8), true)) {
            run(solr, config, writer);
        }
    }

    private static void run(SolrClient solr, Config config, PrintWriter writer)
            throws SolrServerException, IOException {
        SolrQuery query = new SolrQuery(config.query)
                .setRows(config.rows)
                .setSort("identifier", SolrQuery.ORDER.asc)
                .setFields("identifier", "controlfield_001", "nazev", "followers", "raw");

        SuccessorResolver successorResolver = new SuccessorResolver(solr, config.collection);

        writer.println("identifier\tcontrolfield_001\tnazev\tfollowers\taktualni_naslednik\taktualni_naslednik_dntstav\taktualni_naslednik_kuratorstav\t991_POZ\t992_IST");

        int scanned = 0;
        int matched = 0;
        String cursorMark = CursorMarkParams.CURSOR_MARK_START;
        boolean done = false;
        while (!done) {
            query.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
            QueryResponse response = solr.query(config.collection, query);
            SolrDocumentList docs = response.getResults();
            for (SolrDocument doc : docs) {
                scanned++;
                String raw = asString(doc.getFirstValue("raw"));
                if (raw == null || raw.isEmpty()) {
                    continue;
                }

                JSONObject dataFields = new JSONObject(raw).optJSONObject("dataFields");
                if (dataFields == null || !dataFields.has("991") || !dataFields.has("992")) {
                    continue;
                }

                matched++;
                Collection<Object> followers = doc.getFieldValues("followers");
                List<ResolvedSuccessor> currentSuccessors = successorResolver.resolve(followers);
                writer.println(
                        tsv(asString(doc.getFirstValue("identifier"))) + "\t"
                                + tsv(firstValue(doc, "controlfield_001")) + "\t"
                                + tsv(firstValue(doc, "nazev")) + "\t"
                                + tsv(joinObjects(followers)) + "\t"
                                + tsv(joinSuccessorIdentifiers(currentSuccessors)) + "\t"
                                + tsv(joinSuccessorDntStates(currentSuccessors)) + "\t"
                                + tsv(joinSuccessorCuratorStates(currentSuccessors)) + "\t"
                                + tsv(marcField(dataFields.optJSONArray("991"))) + "\t"
                                + tsv(marcField(dataFields.optJSONArray("992"))));

                if (config.limit > 0 && matched >= config.limit) {
                    System.err.println("Scanned records: " + scanned + ", matched records: " + matched);
                    return;
                }
            }

            String nextCursorMark = response.getNextCursorMark();
            if (cursorMark.equals(nextCursorMark)) {
                done = true;
            }
            cursorMark = nextCursorMark;
        }

        System.err.println("Scanned records: " + scanned + ", matched records: " + matched);
    }

    private static String marcField(JSONArray fields) {
        if (fields == null || fields.length() == 0) {
            return "";
        }

        List<String> formattedFields = new ArrayList<>();
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            if (field == null) {
                continue;
            }
            List<Subfield> subfields = subfields(field.optJSONObject("subFields"));
            StringBuilder builder = new StringBuilder();
            builder.append(field.optString("tag"));
            for (Subfield subfield : subfields) {
                builder.append(" $$").append(subfield.code).append(" ").append(subfield.value);
            }
            formattedFields.add(builder.toString());
        }
        return String.join(" | ", formattedFields);
    }

    private static List<Subfield> subfields(JSONObject subFields) {
        List<Subfield> result = new ArrayList<>();
        if (subFields == null) {
            return result;
        }

        for (String code : subFields.keySet()) {
            JSONArray values = subFields.optJSONArray(code);
            if (values == null) {
                continue;
            }
            for (int i = 0; i < values.length(); i++) {
                JSONObject value = values.optJSONObject(i);
                if (value != null) {
                    result.add(new Subfield(value.optString("code", code), value.optString("value"), value.optInt("index", i)));
                }
            }
        }
        result.sort(Comparator.comparingInt(subfield -> subfield.index));
        return result;
    }

    private static String firstValue(SolrDocument doc, String field) {
        Object value = doc.getFirstValue(field);
        return asString(value);
    }

    private static String joinObjects(Collection<Object> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        List<String> result = new ArrayList<>();
        for (Object value : values) {
            result.add(asString(value));
        }
        return String.join(" | ", result);
    }

    private static String joinSuccessorIdentifiers(List<ResolvedSuccessor> successors) {
        List<String> result = new ArrayList<>();
        for (ResolvedSuccessor successor : successors) {
            result.add(successor.identifier);
        }
        return String.join(" | ", result);
    }

    private static String joinSuccessorDntStates(List<ResolvedSuccessor> successors) {
        List<String> result = new ArrayList<>();
        for (ResolvedSuccessor successor : successors) {
            result.add(successor.dntState);
        }
        return String.join(" | ", result);
    }

    private static String joinSuccessorCuratorStates(List<ResolvedSuccessor> successors) {
        List<String> result = new ArrayList<>();
        for (ResolvedSuccessor successor : successors) {
            result.add(successor.curatorState);
        }
        return String.join(" | ", result);
    }

    private static String asString(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String tsv(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r", " ").replace("\n", " ").replace("\t", " ").trim();
    }

    private static void printHelp() {
        System.out.println("Usage: FindPozIstFields [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --solr <url>          Solr base URL, default: " + DEFAULT_SOLR_URL);
        System.out.println("  --collection <name>   Collection/core, default: " + DEFAULT_COLLECTION);
        System.out.println("  --query <solr-query>  Query to scan, default: " + DEFAULT_QUERY);
        System.out.println("  --rows <n>            Cursor page size, default: " + DEFAULT_ROWS);
        System.out.println("  --limit <n>           Stop after n matching records, default: no limit");
        System.out.println("  --out <file>          Write TSV output to file, default: stdout");
    }

    private static final class Subfield {
        private final String code;
        private final String value;
        private final int index;

        private Subfield(String code, String value, int index) {
            this.code = code;
            this.value = value;
            this.index = index;
        }
    }

    private static final class SuccessorResolver {
        private final SolrClient solr;
        private final String collection;
        private final Map<String, List<ResolvedSuccessor>> cache = new HashMap<>();

        private SuccessorResolver(SolrClient solr, String collection) {
            this.solr = solr;
            this.collection = collection;
        }

        private List<ResolvedSuccessor> resolve(Collection<Object> followers) throws SolrServerException, IOException {
            return resolve(followers, new HashSet<>());
        }

        private List<ResolvedSuccessor> resolve(Collection<Object> followers, Set<String> path)
                throws SolrServerException, IOException {
            List<ResolvedSuccessor> result = new ArrayList<>();
            if (followers == null || followers.isEmpty()) {
                return result;
            }
            for (Object follower : followers) {
                String identifier = asString(follower);
                if (!identifier.isEmpty()) {
                    result.addAll(resolveIdentifier(identifier, path));
                }
            }
            return result;
        }

        private List<ResolvedSuccessor> resolveIdentifier(String identifier, Set<String> path)
                throws SolrServerException, IOException {
            if (cache.containsKey(identifier)) {
                return cache.get(identifier);
            }
            if (path.contains(identifier)) {
                List<ResolvedSuccessor> cycle = new ArrayList<>();
                cycle.add(new ResolvedSuccessor(identifier, "cyklus ve followers", "cyklus ve followers"));
                return cycle;
            }

            path.add(identifier);
            try {
                SolrQuery query = new SolrQuery("*")
                        .setRows(1)
                        .addFilterQuery("identifier:\"" + escapeQueryValue(identifier) + "\"")
                        .setFields("identifier", "dntstav", "kuratorstav", "followers");

                SolrDocumentList docs = solr.query(collection, query).getResults();
                List<ResolvedSuccessor> result = new ArrayList<>();
                if (docs.isEmpty()) {
                    result.add(new ResolvedSuccessor(identifier, "nenalezeno", "nenalezeno"));
                } else {
                    SolrDocument doc = docs.get(0);
                    Collection<Object> states = doc.getFieldValues("dntstav");
                    Collection<Object> curatorStates = doc.getFieldValues("kuratorstav");
                    if (containsStateD(states)) {
                        Collection<Object> followers = doc.getFieldValues("followers");
                        if (followers == null || followers.isEmpty()) {
                            result.add(new ResolvedSuccessor(identifier, "D bez followers", joinObjects(curatorStates)));
                        } else {
                            result.addAll(resolve(followers, path));
                        }
                    } else {
                        result.add(new ResolvedSuccessor(identifier, joinObjects(states), joinObjects(curatorStates)));
                    }
                }
                cache.put(identifier, result);
                return result;
            } finally {
                path.remove(identifier);
            }
        }

        private static boolean containsStateD(Collection<Object> states) {
            if (states == null) {
                return false;
            }
            for (Object state : states) {
                if ("D".equals(asString(state))) {
                    return true;
                }
            }
            return false;
        }

        private static String escapeQueryValue(String value) {
            return value.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }

    private static final class ResolvedSuccessor {
        private final String identifier;
        private final String dntState;
        private final String curatorState;

        private ResolvedSuccessor(String identifier, String dntState, String curatorState) {
            this.identifier = identifier;
            this.dntState = dntState;
            this.curatorState = curatorState;
        }
    }

    private static final class Config {
        private String solrUrl = DEFAULT_SOLR_URL;
        private String collection = DEFAULT_COLLECTION;
        private String query = DEFAULT_QUERY;
        private int rows = DEFAULT_ROWS;
        private int limit = 0;
        private String outputFile;
        private boolean help;

        private static Config parse(String[] args) {
            Config config = new Config();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--help".equals(arg) || "-h".equals(arg)) {
                    config.help = true;
                } else if ("--solr".equals(arg)) {
                    config.solrUrl = requiredValue(args, ++i, arg);
                } else if ("--collection".equals(arg)) {
                    config.collection = requiredValue(args, ++i, arg);
                } else if ("--query".equals(arg)) {
                    config.query = requiredValue(args, ++i, arg);
                } else if ("--rows".equals(arg)) {
                    config.rows = Integer.parseInt(requiredValue(args, ++i, arg));
                } else if ("--limit".equals(arg)) {
                    config.limit = Integer.parseInt(requiredValue(args, ++i, arg));
                } else if ("--out".equals(arg)) {
                    config.outputFile = requiredValue(args, ++i, arg);
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }
            return config;
        }

        private static String requiredValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            return args[index];
        }
    }
}

package cz.inovatika.sdnnt.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
 * Generates Solr atomic update batches. Notes from source MARC 991 are written
 * into historie_stavu and historie_kurator_stavu on the current successor.
 */
public class GeneratePozIstUpdateBatches {

    private static final String DEFAULT_SOLR_URL = "http://localhost:8983/solr";
    private static final String DEFAULT_COLLECTION = "catalog";
    private static final String DEFAULT_QUERY = "setSpec:\"DNT-ALL\"";
    private static final int DEFAULT_ROWS = 1000;
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final String STATE_NZ = "NZ";
    private static final String STATE_PN = "PN";

    public static void main(String[] args) throws Exception {
        Config config = Config.parse(args);
        if (config.help) {
            printHelp();
            return;
        }

        try (SolrClient solr = new HttpSolrClient.Builder(config.solrUrl).build()) {
            run(solr, config);
        }
    }

    private static void run(SolrClient solr, Config config) throws SolrServerException, IOException {
        File outDir = new File(config.outDir);
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new IOException("Cannot create output directory: " + outDir.getAbsolutePath());
        }

        SolrQuery query = new SolrQuery(config.query)
                .setRows(config.rows)
                .setSort("identifier", SolrQuery.ORDER.asc)
                .setFields("identifier", "followers", "raw");

        SuccessorResolver successorResolver = new SuccessorResolver(solr, config.collection);
        BatchWriter batchWriter = new BatchWriter(outDir, config.batchSize);
        File skippedFile = new File(outDir, config.skippedFile);

        int scanned = 0;
        int matched = 0;
        int updates = 0;
        int withoutSuccessor = 0;

        try (PrintWriter skipped = new PrintWriter(new OutputStreamWriter(new FileOutputStream(skippedFile), StandardCharsets.UTF_8))) {
            skipped.println("source_identifier\tfollowers\tresolved_successors\treason\tpoznamka\tist");

            String cursorMark = CursorMarkParams.CURSOR_MARK_START;
            boolean done = false;
            while (!done) {
                query.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
                QueryResponse response = solr.query(config.collection, query);
                SolrDocumentList docs = response.getResults();
                for (SolrDocument doc : docs) {
                    scanned++;
                    String raw = asString(doc.getFirstValue("raw"));
                    if (raw.isEmpty()) {
                        continue;
                    }

                    JSONObject dataFields = new JSONObject(raw).optJSONObject("dataFields");
                    if (dataFields == null || !dataFields.has("991") || !dataFields.has("992")) {
                        continue;
                    }

                    matched++;
                    Collection<Object> followers = doc.getFieldValues("followers");
                    List<ResolvedSuccessor> successors = successorResolver.resolve(followers);
                    List<String> poznamky = marcFieldValues(dataFields.optJSONArray("991"));
                    List<HistoryNote> notes = historyNotes(dataFields.optJSONArray("991"), dataFields.optJSONArray("992"));

                    boolean wroteUpdate = false;
                    String skipReason = null;
                    for (ResolvedSuccessor successor : successors) {
                        if (!successor.usable) {
                            continue;
                        }

                        HistoryUpdate historyUpdate = updateHistory(successor.historieStavu, notes, config.historyField);
                        if (!historyUpdate.updated) {
                            skipReason = historyUpdate.reason;
                            continue;
                        }
                        HistoryUpdate curatorHistoryUpdate = updateHistory(
                                successor.historieKuratorStavu,
                                notes,
                                config.curatorHistoryField);
                        if (!curatorHistoryUpdate.updated) {
                            skipReason = curatorHistoryUpdate.reason;
                            continue;
                        }

                        JSONObject update = new JSONObject();
                        update.put("identifier", successor.identifier);
                        update.put(config.historyField, atomicSet(historyUpdate.historieStavu.toString()));
                        update.put(config.curatorHistoryField, atomicSet(curatorHistoryUpdate.historieStavu.toString()));
                        batchWriter.add(update);
                        updates++;
                        wroteUpdate = true;
                    }
                    if (!wroteUpdate) {
                        withoutSuccessor++;
                        skipped.println(tsv(asString(doc.getFirstValue("identifier"))) + "\t"
                                + tsv(joinObjects(followers)) + "\t"
                                + tsv(joinResolvedSuccessors(successors)) + "\t"
                                + tsv(skipReason != null ? skipReason : skipReason(successors)) + "\t"
                                + tsv(String.join(" | ", poznamky)) + "\t"
                                + tsv(joinHistoryNotes(notes)));
                    }

                    if (config.limit > 0 && matched >= config.limit) {
                        batchWriter.close();
                        printSummary(scanned, matched, updates, withoutSuccessor, skippedFile);
                        return;
                    }
                }

                String nextCursorMark = response.getNextCursorMark();
                if (cursorMark.equals(nextCursorMark)) {
                    done = true;
                }
                cursorMark = nextCursorMark;
            }
        }

        batchWriter.close();
        printSummary(scanned, matched, updates, withoutSuccessor, skippedFile);
    }

    private static JSONObject atomicSet(String value) {
        return new JSONObject().put("set", value);
    }

    private static HistoryUpdate updateHistory(String historieStavuString, List<HistoryNote> notes, String fieldName) {
        if (notes.isEmpty()) {
            return HistoryUpdate.skipped("zdrojovy zaznam nema pouzitelnou poznamku/IST");
        }
        if (historieStavuString == null || historieStavuString.trim().isEmpty()) {
            return HistoryUpdate.skipped("naslednik nema " + fieldName);
        }

        JSONArray history = new JSONArray(historieStavuString);
        boolean updated = false;
        for (HistoryNote note : notes) {
            boolean noteApplied = false;
            for (int i = 0; i < history.length(); i++) {
                JSONObject item = history.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                if (matchesHistory(item, note)) {
                    item.put("comment", note.note);
                    noteApplied = true;
                    updated = true;
                }
            }
            if (!noteApplied) {
                return HistoryUpdate.skipped("nenalezena odpovidajici polozka " + fieldName + " pro " + note);
            }
        }

        return updated ? HistoryUpdate.updated(history) : HistoryUpdate.skipped(fieldName + " nebyla zmenena");
    }

    private static boolean matchesHistory(JSONObject item, HistoryNote note) {
        if (!note.state.isEmpty() && !note.state.equals(item.optString("stav"))) {
            return false;
        }
        if (!note.user.isEmpty() && !note.user.equals(item.optString("user"))) {
            return false;
        }
        return true;
    }

    private static List<String> marcFieldValues(JSONArray fields) {
        List<String> result = new ArrayList<>();
        if (fields == null) {
            return result;
        }

        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            if (field == null) {
                continue;
            }
            List<Subfield> subfields = subfields(field.optJSONObject("subFields"));
            List<String> values = new ArrayList<>();
            for (Subfield subfield : subfields) {
                values.add("$$" + subfield.code + " " + subfield.value);
            }
            if (!values.isEmpty()) {
                result.add(String.join(" ", values));
            }
        }
        return result;
    }

    private static List<HistoryNote> historyNotes(JSONArray pozFields, JSONArray istFields) {
        List<String> notes = marcSubfieldValues(pozFields, "a");
        List<IstField> ist = istFields(istFields);
        List<IstField> targetIst = targetCommentIstFields(ist);
        List<HistoryNote> result = new ArrayList<>();
        if (notes.isEmpty() || targetIst.isEmpty()) {
            return result;
        }

        if (notes.size() == ist.size()) {
            for (int i = 0; i < notes.size(); i++) {
                IstField istField = ist.get(i);
                if (isTargetCommentState(istField.state)) {
                    result.add(new HistoryNote(istField.state, istField.user, notes.get(i)));
                }
            }
        } else if (notes.size() == targetIst.size()) {
            for (int i = 0; i < notes.size(); i++) {
                result.add(new HistoryNote(targetIst.get(i).state, targetIst.get(i).user, notes.get(i)));
            }
        } else if (notes.size() == 1) {
            for (IstField istField : targetIst) {
                result.add(new HistoryNote(istField.state, istField.user, notes.get(0)));
            }
        } else {
            int size = Math.min(notes.size(), targetIst.size());
            for (int i = 0; i < size; i++) {
                result.add(new HistoryNote(targetIst.get(i).state, targetIst.get(i).user, notes.get(i)));
            }
        }
        return result;
    }

    private static List<IstField> targetCommentIstFields(List<IstField> ist) {
        List<IstField> result = new ArrayList<>();
        for (IstField istField : ist) {
            if (isTargetCommentState(istField.state)) {
                result.add(istField);
            }
        }
        return result;
    }

    private static boolean isTargetCommentState(String state) {
        return STATE_NZ.equals(state) || STATE_PN.equals(state);
    }

    private static List<String> marcSubfieldValues(JSONArray fields, String code) {
        List<String> result = new ArrayList<>();
        if (fields == null) {
            return result;
        }
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            if (field == null) {
                continue;
            }
            JSONObject subFields = field.optJSONObject("subFields");
            if (subFields == null) {
                continue;
            }
            JSONArray values = subFields.optJSONArray(code);
            if (values == null) {
                continue;
            }
            for (int j = 0; j < values.length(); j++) {
                JSONObject value = values.optJSONObject(j);
                if (value != null && !value.optString("value").isEmpty()) {
                    result.add(value.optString("value"));
                }
            }
        }
        return result;
    }

    private static List<IstField> istFields(JSONArray fields) {
        List<IstField> result = new ArrayList<>();
        if (fields == null) {
            return result;
        }
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            if (field == null) {
                continue;
            }
            JSONObject subFields = field.optJSONObject("subFields");
            if (subFields == null) {
                continue;
            }
            result.add(new IstField(firstSubfieldValue(subFields, "s"), firstSubfieldValue(subFields, "b")));
        }
        return result;
    }

    private static String firstSubfieldValue(JSONObject subFields, String code) {
        JSONArray values = subFields.optJSONArray(code);
        if (values == null || values.length() == 0) {
            return "";
        }
        JSONObject value = values.optJSONObject(0);
        return value == null ? "" : value.optString("value");
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

    private static void printSummary(int scanned, int matched, int updates, int withoutSuccessor, File skippedFile) {
        System.err.println("Scanned records: " + scanned);
        System.err.println("Matched source records: " + matched);
        System.err.println("Generated successor updates: " + updates);
        System.err.println("Matched records without usable successor: " + withoutSuccessor);
        System.err.println("Skipped records report: " + skippedFile.getAbsolutePath());
    }

    private static String asString(Object value) {
        return value == null ? "" : value.toString();
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

    private static String joinHistoryNotes(List<HistoryNote> notes) {
        List<String> result = new ArrayList<>();
        for (HistoryNote note : notes) {
            result.add(note.toString());
        }
        return String.join(" | ", result);
    }

    private static String joinResolvedSuccessors(List<ResolvedSuccessor> successors) {
        List<String> result = new ArrayList<>();
        for (ResolvedSuccessor successor : successors) {
            result.add(successor.identifier + ":" + successor.reason);
        }
        return String.join(" | ", result);
    }

    private static String skipReason(List<ResolvedSuccessor> successors) {
        if (successors.isEmpty()) {
            return "zdrojovy zaznam nema followers";
        }
        return joinResolvedSuccessors(successors);
    }

    private static String tsv(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r", " ").replace("\n", " ").replace("\t", " ").trim();
    }

    private static void printHelp() {
        System.out.println("Usage: GeneratePozIstUpdateBatches [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --solr <url>                 Solr base URL, default: " + DEFAULT_SOLR_URL);
        System.out.println("  --collection <name>          Collection/core, default: " + DEFAULT_COLLECTION);
        System.out.println("  --query <solr-query>         Query to scan, default: " + DEFAULT_QUERY);
        System.out.println("  --outDir <dir>               Output directory, default: solr-update-batches");
        System.out.println("  --rows <n>                   Cursor page size, default: " + DEFAULT_ROWS);
        System.out.println("  --batchSize <n>              Updates per output file, default: " + DEFAULT_BATCH_SIZE);
        System.out.println("  --limit <n>                  Stop after n matching source records, default: no limit");
        System.out.println("  --historyField <name>        Target history field, default: historie_stavu");
        System.out.println("  --curatorHistoryField <name> Target curator history field, default: historie_kurator_stavu");
        System.out.println("  --skippedFile <file>         TSV report for skipped records, default: skipped-poz-ist.tsv");
    }

    private static final class BatchWriter {
        private final File outDir;
        private final int batchSize;
        private JSONArray current = new JSONArray();
        private int fileIndex = 1;

        private BatchWriter(File outDir, int batchSize) {
            this.outDir = outDir;
            this.batchSize = batchSize;
        }

        private void add(JSONObject update) throws IOException {
            current.put(update);
            if (current.length() >= batchSize) {
                flush();
            }
        }

        private void close() throws IOException {
            flush();
        }

        private void flush() throws IOException {
            if (current.length() == 0) {
                return;
            }
            File file = new File(outDir, String.format("solr-update-%05d.json", fileIndex++));
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                writer.println(current.toString(2));
            }
            current = new JSONArray();
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
                cycle.add(new ResolvedSuccessor(identifier, false, "cyklus ve followers"));
                return cycle;
            }

            path.add(identifier);
            try {
                SolrQuery query = new SolrQuery("*")
                        .setRows(1)
                        .addFilterQuery("identifier:\"" + escapeQueryValue(identifier) + "\"")
                        .setFields("identifier", "dntstav", "followers", "historie_stavu", "historie_kurator_stavu");

                SolrDocumentList docs = solr.query(collection, query).getResults();
                List<ResolvedSuccessor> result = new ArrayList<>();
                if (docs.isEmpty()) {
                    result.add(new ResolvedSuccessor(identifier, false, "naslednik nenalezen"));
                } else {
                    SolrDocument doc = docs.get(0);
                    Collection<Object> states = doc.getFieldValues("dntstav");
                    if (containsStateD(states)) {
                        Collection<Object> followers = doc.getFieldValues("followers");
                        if (followers == null || followers.isEmpty()) {
                            result.add(new ResolvedSuccessor(identifier, false, "naslednik je D a nema followers"));
                        } else {
                            result.addAll(resolve(followers, path));
                        }
                    } else {
                        result.add(new ResolvedSuccessor(
                                identifier,
                                true,
                                "ok",
                                asString(doc.getFirstValue("historie_stavu")),
                                asString(doc.getFirstValue("historie_kurator_stavu"))));
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
        private final boolean usable;
        private final String reason;
        private final String historieStavu;
        private final String historieKuratorStavu;

        private ResolvedSuccessor(String identifier, boolean usable, String reason) {
            this(identifier, usable, reason, "", "");
        }

        private ResolvedSuccessor(
                String identifier,
                boolean usable,
                String reason,
                String historieStavu,
                String historieKuratorStavu) {
            this.identifier = identifier;
            this.usable = usable;
            this.reason = reason;
            this.historieStavu = historieStavu;
            this.historieKuratorStavu = historieKuratorStavu;
        }
    }

    private static final class HistoryUpdate {
        private final boolean updated;
        private final JSONArray historieStavu;
        private final String reason;

        private HistoryUpdate(boolean updated, JSONArray historieStavu, String reason) {
            this.updated = updated;
            this.historieStavu = historieStavu;
            this.reason = reason;
        }

        private static HistoryUpdate updated(JSONArray historieStavu) {
            return new HistoryUpdate(true, historieStavu, "");
        }

        private static HistoryUpdate skipped(String reason) {
            return new HistoryUpdate(false, null, reason);
        }
    }

    private static final class HistoryNote {
        private final String state;
        private final String user;
        private final String note;

        private HistoryNote(String state, String user, String note) {
            this.state = state;
            this.user = user;
            this.note = note;
        }

        @Override
        public String toString() {
            return "stav=" + state + ", user=" + user + ", poznamka=" + note;
        }
    }

    private static final class IstField {
        private final String state;
        private final String user;

        private IstField(String state, String user) {
            this.state = state;
            this.user = user;
        }
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

    private static final class Config {
        private String solrUrl = DEFAULT_SOLR_URL;
        private String collection = DEFAULT_COLLECTION;
        private String query = DEFAULT_QUERY;
        private String outDir = "solr-update-batches";
        private int rows = DEFAULT_ROWS;
        private int batchSize = DEFAULT_BATCH_SIZE;
        private int limit = 0;
        private String historyField = "historie_stavu";
        private String curatorHistoryField = "historie_kurator_stavu";
        private String skippedFile = "skipped-poz-ist.tsv";
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
                } else if ("--outDir".equals(arg)) {
                    config.outDir = requiredValue(args, ++i, arg);
                } else if ("--rows".equals(arg)) {
                    config.rows = Integer.parseInt(requiredValue(args, ++i, arg));
                } else if ("--batchSize".equals(arg)) {
                    config.batchSize = Integer.parseInt(requiredValue(args, ++i, arg));
                } else if ("--limit".equals(arg)) {
                    config.limit = Integer.parseInt(requiredValue(args, ++i, arg));
                } else if ("--historyField".equals(arg)) {
                    config.historyField = requiredValue(args, ++i, arg);
                } else if ("--curatorHistoryField".equals(arg)) {
                    config.curatorHistoryField = requiredValue(args, ++i, arg);
                } else if ("--skippedFile".equals(arg)) {
                    config.skippedFile = requiredValue(args, ++i, arg);
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

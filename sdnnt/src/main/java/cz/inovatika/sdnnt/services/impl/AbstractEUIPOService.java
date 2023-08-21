package cz.inovatika.sdnnt.services.impl;

import java.io.File;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.indexer.models.utils.MarcRecordUtils;
import cz.inovatika.sdnnt.utils.StringUtils;

public class AbstractEUIPOService {
    
    /** Maximum title length */
    public static final int MAX_TITLE_LENGTH = 500;
    /** Maximum author length */
    public static final int MAX_AUTHOR_LENGTH = 100;
    /** Maximum year */
    public static final int MAX_YEAR = 2100;
    /** Minimum year */
    public static final int MIN_YEAR = 1440;

    /** Configuration key for filters; only one for both types **/
    protected static final String FILTERS_KEY = "filters";
    // nonparsable date 
    protected static final String NONPARSABLE_DATES_KEY  ="nonparsabledates";
    /** SE template key */
    protected static final String SE_TEMPLATE_KEY = "setemplate";
    /** BK template  key */
    protected static final String BK_TEMPLATE_KEY = "bktemplate";
    /** Set of iteration states key  */
    protected static final String STATES_KEY = "states";
    /** Maximum rows in spreadsheet */
    protected static final String MAXROWS_KEY = "maxrows";
    /** Maximum rows in spreadsheet */
    protected static final String BATCHROWS_KEY = "batchrows";
    /** Output folder key */
    protected static final String FOLDER_KEY = "folder";
    /** Default output folder  */
    public static final String DEFAULT_OUTPUT_FOLDER = System.getProperty("user.home") + File.separator + ".sdnnt/dump";
    /** Default initial bk filter */
    static final List<String> DEFAULT_INITIAL_BK_FILTER = Arrays.asList( "setSpec:SKC");
    /** Default initial se filter */
    static final List<String> DEFAULT_INITIAL_SE_FILTER = Arrays.asList("setSpec:SKC");
    /** */
    protected static final String MAX_ACCEPTING_BK_YEAR_KEY  ="maxbkyear";

    /**  */
    public static final List<Pattern> DEFAULT_REGULAR_EXPRESSIONS_NONPARSABLE_DATES  = Arrays.asList(
        Pattern.compile("1.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile("200.*", Pattern.CASE_INSENSITIVE)
    );
    
    /** Default solr update batch limit */
    public static final int UPDATE_BATCH_LIMIT = 100;
    /** Default spreadsheet limit; max number of rows per one document */
    public static final int SPREADSHEET_LIMIT = 20000;
    /** Default fetching limit; for reading data from solr */
    public static final int FETCH_LIMIT = 1000;
    /** Default max accepting year */
    public static final int MAX_ACCEPTING_YEAR = 2003;
    
    
    protected String outputFolder = AbstractEUIPOService.DEFAULT_OUTPUT_FOLDER;

    protected String seTemplate = null;
    protected String bkTemplate = null;

    protected int spredsheetLimit = AbstractEUIPOService.SPREADSHEET_LIMIT;
    protected int updateBatchLimit = AbstractEUIPOService.UPDATE_BATCH_LIMIT;
    
    protected List<String> states = null; //DEFAULT_STATES;

    protected List<String> filters = null;// DEFAULT_INITIAL_BK_FILTER;
    protected List<Pattern> compiledPatterns = AbstractEUIPOService.DEFAULT_REGULAR_EXPRESSIONS_NONPARSABLE_DATES;
    
    protected int maxAcceptingBKYear  = MAX_ACCEPTING_YEAR;
    
    
    /**  
     * Cut given title
     * @param maxCharacters Maximum allowed characters 
     * @param input Input text to cut 
     */
    public static Pair<Integer,Integer> maxIndexOfWord(int maxCharacters, String input) {
        List<Pair<Integer,Integer>> pairs = new ArrayList<>();
        
        BreakIterator breakIterator = BreakIterator.getWordInstance();
        breakIterator.setText(input);
    
        // Nalezení začátku prvního slova
        int start = breakIterator.first();
    
        // Vypsání jednotlivých slov
        while (BreakIterator.DONE != breakIterator.next()) {
            int end = breakIterator.current();
            if (end > maxCharacters) {
                break;
            }
            pairs.add(Pair.of(start,end));
    
            start = end;
        }
        
        return !pairs.isEmpty() ?  pairs.get(pairs.size() -1) : null;
    }


    /**
     * Configuration method for result of process. It reads configuration ansd sets folder, number of in sheets and number of records in updating batch
     * @param results Configuration object
     */
    public void iterationResults(JSONObject results) {
        if (results != null) {
            String container = results.optString(AbstractEUIPOService.FOLDER_KEY);
            if (container != null) {
                this.outputFolder = results.optString(AbstractEUIPOService.FOLDER_KEY);
            }
            
            if (results.has(AbstractEUIPOService.MAXROWS_KEY)) {
                this.spredsheetLimit = results.getInt(AbstractEUIPOService.MAXROWS_KEY);
            }
            
            if (results.has(AbstractEUIPOService.BATCHROWS_KEY)) {
                this.updateBatchLimit = results.getInt(AbstractEUIPOService.BATCHROWS_KEY);
            }
            
        }
    }

    /**
     * Configuration method for 
     * @param iteration
     */
    public void iterationConfig(JSONObject iteration) {
        if (iteration != null) {
            if (iteration.has(AbstractEUIPOService.STATES_KEY)) {
                this.states = new ArrayList<>();
                JSONArray iterationOverStates = iteration.optJSONArray(AbstractEUIPOService.STATES_KEY);
                if (iterationOverStates != null) {
                    iterationOverStates.forEach(it -> {
                        states.add(it.toString());
                    });
                }
            }
            
            if (iteration.has(AbstractEUIPOService.BK_TEMPLATE_KEY)) {
                this.bkTemplate = iteration.getString(AbstractEUIPOService.BK_TEMPLATE_KEY);
            }
            if (iteration.has(AbstractEUIPOService.SE_TEMPLATE_KEY)) {
                this.seTemplate = iteration.getString(AbstractEUIPOService.SE_TEMPLATE_KEY);
            }
            
            if (iteration.has(AbstractEUIPOService.FILTERS_KEY)) {
                JSONArray filters = iteration.optJSONArray(AbstractEUIPOService.FILTERS_KEY);
                List<String> listFilters = new ArrayList<>();
                if (filters != null) {
                    filters.forEach(f -> {
                        listFilters.add(f.toString());
                    });
                }
                if (listFilters != null) {
                    this.filters = listFilters;
                }
            }
            if (iteration.has(AbstractEUIPOService.NONPARSABLE_DATES_KEY)) {
                JSONArray nonParsble = iteration.optJSONArray(AbstractEUIPOService.NONPARSABLE_DATES_KEY);
                List<String> listExpressions = new ArrayList<>();
                if (listExpressions != null) {
                    nonParsble.forEach(f -> {
                        listExpressions.add(f.toString());
                    });
                }
                if (listExpressions != null) {
                    this.compiledPatterns = listExpressions.stream().map(Pattern::compile).collect(Collectors.toList());
                }
            }
            
            if (iteration.has(AbstractEUIPOService.MAX_ACCEPTING_BK_YEAR_KEY)) {
                this.maxAcceptingBKYear = iteration.getInt(MAX_ACCEPTING_BK_YEAR_KEY);
            }
        }
    }

    /** Generate author */
    protected List<String> generateAuthor(JSONObject object) {
        List<String> retvals = new ArrayList<>();
        // StringBuilder builder = new StringBuilder();
        List<String> author1 = extractField(object, "100", "a", "b");
        if (author1 != null && !author1.isEmpty())
            retvals.addAll(author1);
        List<String> author2 = extractField(object, "110", "a", "b");
        if (author2 != null && !author2.isEmpty())
            retvals.addAll(author2);
        List<String> author3 = extractField(object, "700", "a", "b");
        if (author3 != null && !author3.isEmpty())
            retvals.addAll(author3);
        return retvals;
    }

    /** Generate title */
    protected List<String> generateTitle(JSONObject object) {
        //return extractField(object, "245", "a", "b", "c", "n", "p");
        return extractField(object, "245", "a", "b",  "n", "p");
    }

    
    protected List<String> generatecCCNB(JSONObject object) {
        return extractField(object, "015", "a");
    }

    protected List<String> generateISBN(JSONObject object) {
        return extractField(object, "020", "a");
    }


    protected List<String> generateISSN(JSONObject object) {
        return extractField(object, "022", "a");
    }


    protected List<String> generateDescription(JSONObject object) {
        return extractField(object, "505", "a");
    }

    protected List<String> generatePublisher(JSONObject object) {
        List<String> gen1 = extractField(object, "260", "b");
        if (gen1 != null && !gen1.isEmpty()) {
            return Arrays.asList(gen1.get(0));
        }
        List<String> gen2 = extractField(object, "264", "b");
        if (gen2 != null && !gen2.isEmpty()) {
            return Arrays.asList(gen2.get(0));
        }

        return new ArrayList<>();
    }

    protected List<String> generateLanguage(JSONObject object) {
        return extractField(object, "041", "a");
    }

    protected List<String> extractField(JSONObject object, String datafieldKey, String... subfields) {
        Map<String, List<String>> mapping = new HashMap<>();
    
        if (object.has("dataFields")) {
            // 245a, 245b, 245n, 245p
            JSONObject dataFields = object.getJSONObject("dataFields");
            if (dataFields != null) {
                if (dataFields.has(datafieldKey)) {
                    JSONArray jsonArray = dataFields.getJSONArray(datafieldKey);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject dataField = jsonArray.getJSONObject(i);
                        if (dataField.has("subFields")) {
                            JSONObject subFields = dataField.getJSONObject("subFields");
                            for (String sf : subfields) {
                                if (subFields.has(sf)) {
                                    List<String> sfvals = new ArrayList<>();
                                    JSONArray sfArray = subFields.getJSONArray(sf);
                                    sfArray.forEach(sfObject -> {
                                        String val = ((JSONObject) sfObject).getString("value");
                                        sfvals.add(val);
                                    });
    
                                    if (mapping.containsKey(datafieldKey + sf)) {
                                        mapping.get(datafieldKey + sf).addAll(sfvals);
                                    } else {
                                        mapping.put(datafieldKey + sf, sfvals);
                                    }
                                    
                                }
                            }
                        }
                    }
                }
            }
        }
        List<String> keys = Arrays.stream(subfields).map(it -> datafieldKey + "" + it).collect(Collectors.toList());
        List<String> vals = new ArrayList<>();
        keys.forEach(key -> {
            if (mapping.containsKey(key)) {
                vals.addAll(mapping.get(key));
            }
        });
    
        return vals;
    }


    protected boolean isValidDate(String string) {
        if (string != null) {
            try {
                int parsed = Integer.parseInt(string);
                return parsed > AbstractEUIPOService.MIN_YEAR && parsed < AbstractEUIPOService.MAX_YEAR;
            } catch (NumberFormatException e) {
                //getLogger().info(String.format("Date omitted: %s", string));
                // ommit 
            }
        }
        return false;
    }


    
    protected Map<String, List<String>> prepareDataRecordForExcel(Object raw, Object date1, Object date2, Object fmt, Object typeOfDate,
            Object controlField008) {
                JSONObject object = new JSONObject(raw.toString());
                Map<String, List<String>> oneRecordValues = new HashMap<>();
            
                // title
                oneRecordValues.put("Title", generateTitle(object));
            
                // author
                oneRecordValues.put("AuthorPerfomer", generateAuthor(object));
            
                // description
                List<String> description = generateDescription(object);
                if (description != null && !description.isEmpty()) {
                    oneRecordValues.put("Description", description);
                }
            
                // publisher
                List<String> publisher = generatePublisher(object);
                if (publisher != null && !publisher.isEmpty()) {
                    oneRecordValues.put("Publisher", publisher);
                }
            
                // language
                List<String> language = generateLanguage(object);
                if (language != null && !language.isEmpty()) {
                    oneRecordValues.put("Language", language);
                } else {
                    String field = controlField008.toString();
                    oneRecordValues.put("Language", Arrays.asList(field.substring(35, 38)));
                }
            
                // specific for bk
                if (fmt.equals("BK")) {
                    // isbn
                    List<String> isbn = generateISBN(object);
                    if (isbn != null && !isbn.isEmpty()) {
                        String isbnText = isbn.get(0);
                        if (StringUtils.isAnyString(isbnText)) {
                            oneRecordValues.put("ISN", isbn);
                            oneRecordValues.put("ISNType", Arrays.asList("ISBN"));
                        }
                    }
                    // TODO: changes
                    if ( date2 != null && !date2.toString().trim().startsWith("99") && !date2.toString().trim().contains("--")
                            && !date2.toString().trim().contains("u") && StringUtils.isAnyString(date2.toString()) && 
                            !typeOfDate.equals("t") ) {
                        
                        
                        if (isValidDate(date1.toString())) {
                            oneRecordValues.put("PublisherDate",
                                    Arrays.asList(date1.toString() + "/" + date2.toString()));
                        }
                    } else {
                        if (isValidDate(date1.toString())) {
                            oneRecordValues.put("PublisherDate", Arrays.asList(date1.toString()));
                        }
                    }
            
                    oneRecordValues.put("Type", Arrays.asList("INDIVIDUAL"));
            
                } else {
                    // specific for SE
                    // issn
                    List<String> isbn = generateISSN(object);
                    if (isbn != null && !isbn.isEmpty()) {
                        String isbnText = isbn.get(0);
                        if (StringUtils.isAnyString(isbnText)) {
                            oneRecordValues.put("ISN", isbn);
                            oneRecordValues.put("ISNType", Arrays.asList("ISSN"));
                        }
                    }
                    // type is set
                    oneRecordValues.put("Type", Arrays.asList("SET"));
                }
            
                oneRecordValues.put("euipo", Arrays.asList(MarcRecordUtils.generateEUIPOIdent()));
                return oneRecordValues;
            }
}

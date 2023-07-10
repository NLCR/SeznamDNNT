package cz.inovatika.sdnnt.index.utils;

import org.apache.solr.client.solrj.util.ClientUtils;
import org.json.JSONArray;

import cz.inovatika.sdnnt.Options;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.stream.Collectors;

public class QueryUtils {
    
    public static final List<String> DEFAULT_QUERY_DATE_PATTERNS = Arrays.asList(
            "19*", "20", "200", "201" , "20uu", "200u", "201u");
    

    public static final String IDENTIFIER_PREFIX = "oai:aleph-nkp.cz:";
    
    private QueryUtils() {}
    
    
    public static String query(String inputQuery) {
        if (inputQuery != null && inputQuery.startsWith(IDENTIFIER_PREFIX)) {
            return "\""+ inputQuery +"\"";
        } else {
            List<String> words = new ArrayList<>();
            Scanner scanner = new Scanner(inputQuery);
            while(scanner.hasNext()) {
                String word = scanner.next();
                if (word != null && !word.trim().equals("")) {
                    if (word !=null && word.startsWith("\"")&&  word.endsWith("\"")) {
                        words.add(word);
                    } else {
                        words.add(ClientUtils.escapeQueryChars(word));
                    }
                }
            }
            return words.stream().collect(Collectors.joining(" "));
        }
    }


    public static String catalogSEFilterQueryPart(Options opts, int year, int fromYear) {
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
        return se;
    }


    
    public static String catalogBKFilterQueryPart(Options opts, int year, int fromYear) {
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
        return bk;
    }

    /** Only lower bound filters; Issue #510 */
    public static String catalogSEFilterQueryPartOnlyLowerBound(Options opts, int year, int fromYear) {
        int yearsSE = opts.getJSONObject("search").getInt("yearsSE");

        JSONArray patternsArray = opts.getJSONObject("search").optJSONArray("yearsSEPatterns");
        List<Object> patterns = new ArrayList<>();

        if (patternsArray != null) {
            patternsArray.forEach(patterns::add);
        } else {
            DEFAULT_QUERY_DATE_PATTERNS.forEach(patterns::add);
        }
        
        String date1Pattern = patterns.stream().map(it-> {
            if (!it.toString().endsWith("*")) {
                return '"'+it.toString()+'"';
            } else return it.toString();
        }).collect(Collectors.joining(" OR "));

        String seDate = "(date1_int:["
                + fromYear
                + " TO "
                + (year - yearsSE)
                +"] OR (NOT date1_int:* AND date1:("+date1Pattern+")))";
        
        String se = "(fmt:SE AND " + seDate + ")";
        return se;
    }

    public static String catalogBKFilterQueryPartOnlyLowerBound(Options opts, int year, int fromYear) {
        int yearsBK = opts.getJSONObject("search").getInt("yearsBK");

        List<Object> patterns = new ArrayList<>();

        JSONArray patternsArray = opts.getJSONObject("search").optJSONArray("yearsBKPatterns" );
        if (patternsArray != null) {
            patternsArray.forEach(patterns::add);
        } else {
            DEFAULT_QUERY_DATE_PATTERNS.forEach(patterns::add);
        }
        
        String date1Pattern = patterns.stream().map(it-> {
            if (!it.toString().endsWith("*")) {
                return '"'+it.toString()+'"';
            } else return it.toString();
        }).collect(Collectors.joining(" OR "));

        String bkDate = "(date1_int:["
                + fromYear
                + " TO "
                + (year - yearsBK)
                + "] OR (NOT date1_int:* AND date1:("+date1Pattern+")))";
    
        String bk = "(fmt:BK AND " + bkDate + ")";
        return bk;
    }
    
}


package cz.inovatika.sdnnt.index.utils;

import org.apache.solr.client.solrj.util.ClientUtils;
import org.json.JSONArray;

import cz.inovatika.sdnnt.Options;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class QueryUtils {
    
    public static final List<String> DEFAULT_QUERY_DATE_PATTERNS = Arrays.asList(
            "19*", "20", "200", "201" , "20uu", "200u", "201u");
    

    public static final String IDENTIFIER_PREFIX = "oai:aleph-nkp.cz:";
    
    private QueryUtils() {}
    
    public static String query(String inputQuery) {
        return query(inputQuery, IDENTIFIER_PREFIX);
    }

    public static String query(String inputQuery, String identifierPrefix) {
        return query(inputQuery, identifierPrefix, null);
    }

    /**
     * Escapes a user query while preserving quoted phrases. When exactField is
     * supplied, quoted phrases are explicitly routed to that field so its
     * analyzer can omit stemming and synonym expansion.
     */
    public static String query(String inputQuery, String identifierPrefix, String exactField) {
        if (inputQuery != null && inputQuery.startsWith(identifierPrefix)) {
            return "\""+ inputQuery +"\"";
        }

        List<String> clauses = new ArrayList<>();
        int offset = 0;
        while (offset < inputQuery.length()) {
            while (offset < inputQuery.length() && Character.isWhitespace(inputQuery.charAt(offset))) {
                offset++;
            }
            if (offset >= inputQuery.length()) {
                break;
            }

            if (inputQuery.charAt(offset) == '"') {
                int closingQuote = inputQuery.indexOf('"', offset + 1);
                if (closingQuote >= 0) {
                    String phrase = inputQuery.substring(offset + 1, closingQuote).trim();
                    if (!phrase.isEmpty()) {
                        String escapedPhrase = Arrays.stream(phrase.split("\\s+"))
                                .map(ClientUtils::escapeQueryChars)
                                .collect(Collectors.joining(" "));
                        String quotedPhrase = "\"" + escapedPhrase + "\"";
                        clauses.add(exactField == null ? quotedPhrase : exactField + ":" + quotedPhrase);
                    }
                    offset = closingQuote + 1;
                    continue;
                }
            }

            int tokenEnd = offset + 1;
            while (tokenEnd < inputQuery.length() && !Character.isWhitespace(inputQuery.charAt(tokenEnd))) {
                tokenEnd++;
            }
            clauses.add(ClientUtils.escapeQueryChars(inputQuery.substring(offset, tokenEnd)));
            offset = tokenEnd;
        }

        return clauses.stream().collect(Collectors.joining(" "));
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

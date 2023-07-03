package cz.inovatika.sdnnt.index.utils;

import org.apache.solr.client.solrj.util.ClientUtils;

import cz.inovatika.sdnnt.Options;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.stream.Collectors;

public class QueryUtils {

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
        String seDate = "(date1_int:["
                + fromYear
                + " TO "
                + (year - yearsSE)
                +"])";
    
        String se = "(fmt:SE AND " + seDate + ")";
        return se;
    }

    public static String catalogBKFilterQueryPartOnlyLowerBound(Options opts, int year, int fromYear) {
        int yearsBK = opts.getJSONObject("search").getInt("yearsBK");
        String bkDate = "(date1_int:["
                + fromYear
                + " TO "
                + (year - yearsBK)
                + "])";
    
        String bk = "(fmt:BK AND " + bkDate + ")";
        return bk;
    }
    
    
}

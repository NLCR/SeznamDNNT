package cz.inovatika.sdnnt.index.utils;

import org.apache.solr.client.solrj.util.ClientUtils;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.stream.Collectors;

public class QueryUtils {

    private QueryUtils() {}

    public static String query(String inputQuery) {
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

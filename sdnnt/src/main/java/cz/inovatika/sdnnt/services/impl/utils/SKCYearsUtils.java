package cz.inovatika.sdnnt.services.impl.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.Pair;

public class SKCYearsUtils {
    
    public static final Logger LOGGER = Logger.getLogger(SKCYearsUtils.class.getName());
    
    private SKCYearsUtils() {}

    
    public static List<Pair<Integer, Integer>> parseYearRanges(String input) throws Exception{
        List<Pair<Integer, Integer>> result = new ArrayList<>();
        String[] ranges = input.split(",");
        Pair<Integer, Integer> lastRange = null;  // Context holding the last parsed range

        for (String range : ranges) {
            if (range.contains("-")) {
                String[] years = range.split("-");
                int startYear = parseYear(years[0], lastRange, true); // Parse start year
                int endYear;

                if (years.length < 2 || years[1].isEmpty()) {
                    endYear = 9999;
                } else {
                    endYear = parseYear(years[1], Pair.of(startYear, startYear), false); // Parse end year
                }
                lastRange = Pair.of(startYear, endYear);
                result.add(lastRange);
            } else {
                int year = parseYear(range, lastRange, true);
                lastRange = Pair.of(year, year);
                result.add(lastRange);
            }
        }
        return result;
    }
    
    
    private static int parseYear(String yearStr, Pair<Integer, Integer> lastRange, boolean isStartYear) {
        StringBuilder yearBuilder = new StringBuilder();
        for (int i = 0; i < yearStr.trim().length(); i++) {
            char ch = yearStr.trim().charAt(i);
            if (Character.isDigit(ch)) {
                yearBuilder.append(ch);
            } else {
                if (isStartYear) {
                    yearBuilder.append("0");
                } else {
                    yearBuilder.append("9");
                }
                
            }
        }
        
        int year = Integer.parseInt(yearBuilder.toString());
        if (yearBuilder.length() == 2) {
            int lastYear = (lastRange != null) ? lastRange.getRight() : 0; // Get the last end year from the context
            int lastCentury = lastYear / 100;
            int potentialYear = lastCentury * 100 + year;
            if (potentialYear <= lastYear) {
                return (lastCentury + 1) * 100 + year;
            } else {
                return potentialYear;
            }
        } else if (yearStr.length() == 1) {
            int lastYear = (lastRange != null) ? lastRange.getRight() : 0;
            if (lastYear >= 2000) {
                return 2000 + year;
            } else if (lastYear >= 1900) {
                return 1900 + year;
            }
        }
        return year;
    }
}

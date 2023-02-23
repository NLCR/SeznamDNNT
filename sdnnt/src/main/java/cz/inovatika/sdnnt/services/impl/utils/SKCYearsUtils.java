package cz.inovatika.sdnnt.services.impl.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

public class SKCYearsUtils {
    
    private SKCYearsUtils() {}

    
    public static List<Pair<Integer, Integer>> skcRange(String pattern) {
        List<Pair<Integer,Integer>> retval = new ArrayList<>();
        String[] values = pattern.split(",");
        for (String val : values) {
            if (val.contains("-")) {
                String[] rangeValue = val.split("-");
                if (rangeValue.length > 1) {
                    int left = normalizeValue(Integer.parseInt(rangeValue[0].trim()));
                    int right = normalizeRightValue(left,Integer.parseInt(rangeValue[1].trim()));
                    retval.add(Pair.of(left, right));
                } else {
                    if (rangeValue.length == 1) {
                        int v = normalizeValue(Integer.parseInt(rangeValue[0].trim()));
                        retval.add(Pair.of(v, 9999));
                    } else {
                        int v = normalizeValue(Integer.parseInt(val));
                        retval.add(Pair.of(v, v));
                    }
                }
            } else {
                int v = normalizeValue(Integer.parseInt(val));
                retval.add(Pair.of(v,v));
            }
        }
        return retval;
    }

    private static int normalizeValue(int val) {
        if (val < 100 && val > 10) {
            return Integer.parseInt("19"+val);
        } else if (val < 10) {
            return Integer.parseInt("190"+val);
        } else return val;
    }
    
    
    private static int normalizeRightValue(int left, int val) {
        // 07  - bud 1907 nebo 2007

        if (val < 10) {
            if (left < 2000) {
                return Integer.parseInt("190"+val);
            } else {
                return Integer.parseInt("200"+val);
            }
        }
        if (val < 100) {
            if (left < 2000) {
                return Integer.parseInt("19"+val);
            } else {
                return Integer.parseInt("20"+val);
            }
        }
        return val;
    }
    
}

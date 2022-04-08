package cz.inovatika.sdnnt.utils;

public class ZadostUtils {
    
    private ZadostUtils() {}

    public static String idParts(String id) {
        if (id != null) {
            if (id.length() > 8) {
                return id.substring(id.length()-8);
            } else return id;
        } else return null;
    }
}

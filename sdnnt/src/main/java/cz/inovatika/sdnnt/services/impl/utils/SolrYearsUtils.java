package cz.inovatika.sdnnt.services.impl.utils;

import java.util.logging.Logger;

public class SolrYearsUtils {
    
    public static final Logger LOGGER = Logger.getLogger(SolrYearsUtils.class.getName());
    
    private SolrYearsUtils() {}

    public static Integer solrDate(String rocnik) {
        int rok = -1;
        if (rocnik != null && rocnik.trim().startsWith("[")) {
            int index = rocnik.indexOf("[");
            rocnik = rocnik.substring(index+1);
        }
        
        if (rocnik != null && rocnik.trim().endsWith("]")) {
            int index = rocnik.indexOf("]");
            rocnik = rocnik.substring(0, index);
        }
        
        
        if (rocnik.contains("-") || rocnik.contains("_") || rocnik.contains("..") ) {
            // expecting range pattern yyyy - yyyyy 
            String[] split = new String[0];
            if (rocnik.contains("-")) {
                split = rocnik.split("-");
            } else if (rocnik.contains("_")){
                split = rocnik.split("_");
            } else {
                split = rocnik.split("..");
            }

            if (split.length > 1) {
                rok = parsingYear(split[1].trim());
            } else if (split.length == 1){
                rok = parsingYear(split[0]);
            } else {
                rok = parsingYear( rocnik);
            }
        } else {
            rok = parsingYear(rocnik);
        }
        return rok;
    }
    
    
    private static int parsingYear(String rocnik) {
        try {
            return Integer.parseInt(rocnik);
        } catch (Exception e) {
            LOGGER.warning(String.format("Input date parsing problem '%s'", rocnik));
            return -1;
        }
    }


}

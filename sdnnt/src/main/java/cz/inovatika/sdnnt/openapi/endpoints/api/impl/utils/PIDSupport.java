package cz.inovatika.sdnnt.openapi.endpoints.api.impl.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PIDSupport {
    
    private static Pattern PATTERN = Pattern.compile("((uuid):([A-Za-z0-9\\-]+))");
    
    private PIDSupport() {}

    public static String pidNormalization(String pid) {
        // orezu od prvniho bileho znaku - SKC a prapodivne postfixy v url 
        if (pid != null) {
            
            Matcher matcher = PATTERN.matcher(pid);
            if (matcher.find()) {
                return matcher.group(0);
                
            }
        }
        return pid;
    }
    
    public static String pidFromLink(String link) {
        int indexOf = link.indexOf("uuid:");
        if (indexOf > 0 ) { 
            return link.substring(indexOf);
        }
        return link;
    }
    
}

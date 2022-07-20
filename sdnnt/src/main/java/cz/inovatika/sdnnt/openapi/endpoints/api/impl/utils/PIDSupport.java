package cz.inovatika.sdnnt.openapi.endpoints.api.impl.utils;

public class PIDSupport {
    
    private PIDSupport() {}

    public static String pidNormalization(String pid) {
        // orezu od prvniho bileho znaku - SKC a prapodivne postfixy v url 
        if (pid != null) {
            char[] charArray = pid.toCharArray();
            for (int i = 0; i < charArray.length; i++) {
                if (Character.isWhitespace(charArray[i])) {
                    return pid.substring(0, i);
                }
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

package cz.inovatika.sdnnt.utils;

public class PIDUtils {

        private PIDUtils() {}

        public static String pid(String surl) {
            if (surl.contains("uuid:")) {
                int start = surl.indexOf("uuid:");
                int end = Math.min(surl.indexOf("uuid:") + 41, surl.length());
                String pid = surl.substring(start, end);
                char[] charArray = pid.toCharArray();
                boolean checkWS = false;
                for (int i = 0; i < charArray.length; i++) {
                    if (Character.isWhitespace(charArray[i])) {
                        checkWS = true;
                    }
                }
                return checkWS ? null : pid;
            } else
                return null;
        }
}

package cz.inovatika.sdnnt.utils;


public class DetectYear {
    
    
    private DetectYear() {}
    
    public static enum Bound {
        UP, DOWN;
    }
    
    public static int detectYear(String minYear, Bound bound) {
        StringBuilder builder = new StringBuilder();
        char[] chars = minYear.toCharArray();
        for (int i = 0; i < Math.min(chars.length, 4); i++) {
            char ch = chars[i];
            if (Character.isDigit(ch)) {
                builder.append(ch);
            } else {
                switch(bound) {
                case UP: 
                    builder.append("9");
                    break;
                case DOWN:
                    builder.append("0");
                    break;
                    
                }
            }
        }
        
        if (builder.length() < 4) {
            for (int i = builder.length(); i < 4; i++) {
                switch(bound) {
                case UP: 
                    builder.append("9");
                    break;
                case DOWN:
                    builder.append("0");
                    break;
                    
                }
            }
        }
        
        return Integer.parseInt(builder.toString());
    }
    
}

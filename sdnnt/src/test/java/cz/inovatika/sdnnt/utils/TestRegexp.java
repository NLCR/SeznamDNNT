package cz.inovatika.sdnnt.utils;

public class TestRegexp {
    
    public static void main(String[] args) {
        
        //String format = testingMethod("member@nkp.cz;employee@nkp.cz", ".*;employee@nkp.cz.*");
        //System.out.println(format);

        //System.out.println(testingMethod("member@nkp.cz;employee@nkp.cz", "(^|.*;\\s*)employee@nkp.cz.*"));
        
       // System.out.println(testingMethod("member@nkp.cz;employee@nkp.cz", ".*employee@nkp\\.cz.*"));
    }

    private static String testingMethod(String header, String regex) {
        //String header = "member@nkp.cz;employee@nkp.cz";
        //headerVal.matches(regexp)
        //String regex = ".*;employee@nkp.cz.*";
        String format = String.format("header %s; pattern %s, result %b",  header, regex, method(header, regex));
        return format;
    }

    private static boolean method(String test, String regex) {
        boolean matches = test.matches(regex);
        return matches;
    }
}

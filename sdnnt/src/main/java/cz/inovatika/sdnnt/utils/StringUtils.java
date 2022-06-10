package cz.inovatika.sdnnt.utils;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;


/**
 * String utilities
 * @author pavels
 */
public class StringUtils {

    public static boolean match(String left, String right) {
        if (isAnyString(left) && isAnyString(right))  {
            return left.equals(right);
        }  else if(left ==null && right == null){
            return true;
        } else return false;
    }
    
    /**
     * Minus operator
     * @param bigger Bigger string
     * @param smaller Smaller string
     * @return result of Bigger - Smaller
     */
    public static String minus(String bigger, String smaller) {
        if (bigger.length() >= smaller.length()) {
            return bigger.replace(smaller, "");
        } else throw new IllegalArgumentException("");
    }
    
    
    /**
     * Returns string with escape sequences 
     * @param rawString Given string 
     * @param escapeChar Escape character
     * @param charsMustBeEscaped All characters that must be escaped
     * @return string with escape sequences
     */
    public static String escape(String rawString, Character escapeChar, Character ... charsMustBeEscaped) {
        StringWriter writer = new StringWriter();
        List<Character> mustBeEscaped = Arrays.asList(charsMustBeEscaped);
        char[] charArray = rawString.toCharArray();
        for (char c : charArray) {
            if (mustBeEscaped.contains(new Character(c))) {
                writer.write('\\');
            }
            writer.write(c);
        }
        return writer.toString();
    }
    
    /**
     * Remove escape sequences from given string 
     * @param rawString Given string
     * @param escapeChar Escape sequnce character
     * @param charsMustBeEscaped All characters that must be escaped
     * @return 
     */
    public static String unescape(String rawString, Character escapeChar, Character ... charsMustBeEscaped) {
        StringWriter writer = new StringWriter();
        List<Character> mustBeEscaped = Arrays.asList(charsMustBeEscaped);
        Stack<Character> stckChars = new Stack<Character>();
        char[] charArray = rawString.toCharArray();
        for (int i = charArray.length-1; i >=0; i--) {
            stckChars.push(new Character(charArray[i]));
        }
        
        while(!stckChars.isEmpty()) {
            Character cChar = stckChars.pop();
            if ((cChar.equals(escapeChar)) && (!stckChars.isEmpty()) && (mustBeEscaped.contains(stckChars.peek()))) {
                writer.write(stckChars.pop());
            } else {
                writer.write(cChar);
            }
        }
        
        return writer.toString();
        
    }
    

    /**
     * Returns true if given input string contains any characters otherwise returns false
     * @param input Input string 
     * @return True if given string contains any character otherwise returns false
     */
    public static boolean isAnyString(String input) {
        return input != null && (!input.trim().equals(""));
    }
}

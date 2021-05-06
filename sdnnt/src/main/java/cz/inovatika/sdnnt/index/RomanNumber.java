/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt.indexer;

/**
 *
 * @author alberto
 */
public class RomanNumber {
    
    String input;
    int intValue;
    boolean isValid;
    
    public RomanNumber(String s){
        this.input = s;
        parse(s);
    }
    
    public int toInt(){
        return intValue;
    }
    
    public boolean isValid(){
        return isValid;
    }

    private void parse(String text) {
        
        int arabicNumber = 0;
        try{
            arabicNumber = Integer.parseInt(text);
            isValid = true;
            intValue = arabicNumber;
        }catch(Exception ex){
            String s = text.toUpperCase();
            while (s.length() > 0) {
                if (s.startsWith("M")) {
                    arabicNumber += 1000;
                    s = s.substring(1);
                } else if (s.startsWith("CM")) {
                    arabicNumber += 900;
                    s = s.substring(2);
                } else if (s.startsWith("D")) {
                    arabicNumber += 500;
                    s = s.substring(1);
                } else if (s.startsWith("CD")) {
                    arabicNumber += 400;
                    s = s.substring(2);
                } else if (s.startsWith("C")) {
                    arabicNumber += 100;
                    s = s.substring(1);
                } else if (s.startsWith("XC")) {
                    arabicNumber += 90;
                    s = s.substring(2);
                } else if (s.startsWith("L")) {
                    arabicNumber += 90;
                    s = s.substring(1);
                } else if (s.startsWith("XL")) {
                    arabicNumber += 40;
                    s = s.substring(2);
                } else if (s.startsWith("X")) {
                    arabicNumber += 10;
                    s = s.substring(1);
                } else if (s.startsWith("IX")) {
                    arabicNumber += 9;
                    s = s.substring(2);
                } else if (s.startsWith("V")) {
                    arabicNumber += 5;
                    s = s.substring(1);
                } else if (s.startsWith("IV")) {
                    arabicNumber += 4;
                    s = s.substring(2);
                } else if (s.startsWith("I")) {
                    arabicNumber += 1;
                    s = s.substring(1);
                } else if (s.startsWith("[") || s.startsWith("]") || s.startsWith("(") || s.startsWith(")")) {
                    // ignore
                    s = s.substring(1);
                } else {
                    s = "";
                    isValid = false;
                    intValue = 0;
                }
            }
            isValid = true;
            intValue = arabicNumber;
        }
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt.index;

import org.apache.commons.validator.routines.ISBNValidator;

/**
 *
 * @author alberto
 */
public class ISBN {

    ISBNValidator val = new ISBNValidator();
    private boolean strict;
    private final char[] SEPARATORS = {'-', ' '};

    public ISBN() { 
        this.strict = false;
    }

    public ISBN(boolean strict) {
        this.strict = strict;
    }
 
    public boolean isValid(String isbn) {
        if (this.strict) {
            return val.isValid(isbn);
        } else {
            if(val.isValid(clean13(isbn))){
                return true;
            }else{
                return val.isValid(clean10(isbn));
            }
            
        }
    }

    private String clean13(String isbn) {
        String ret = removeSeparators(isbn).toUpperCase();
        ret = ret.substring(0, Math.min(13, ret.length()));
        return ret;
    }
    

    private String clean10(String isbn) {
        String ret = removeSeparators(isbn).toUpperCase();
        ret = ret.substring(0, Math.min(10, ret.length()));
        return ret;
    }
    

    private boolean isSeparator(char character) {
        for (char separator : SEPARATORS) {
            if (character == separator) {
                return true;
            }
        }
        return false;
    }
    
    private String removeSeparators(String original) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < original.length(); i++) {
            char character = original.charAt(i);
            if (!isSeparator(character)) {
                result.append(character);
            }
        }
        return result.toString();
    }
}

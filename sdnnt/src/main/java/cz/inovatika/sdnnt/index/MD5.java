/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt.index;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CheckedInputStream;

import java.util.zip.Adler32;
 
import java.io.*;
/**
 * 
 * @author alberto
 */
public class MD5 {

    public static String normalize(String old) {

        String newStr = old;
        char[] o = {'á', 'à', 'č', 'ď', 'ě', 'é', 'í', 'ľ', 'ň', 'ó', 'ř', 'r', 'š', 'ť', 'ů', 'ú', 'u', 'u', 'ý', 'ž', 'Á', 'À', 'Č', 'Ď', 'É', 'Ě', 'Í', 'Ĺ', 'Ň', 'Ó', 'Ř', 'Š', 'Ť', 'Ú', 'Ů', 'Ý', 'Ž'};
        char[] n = {'a', 'a', 'c', 'd', 'e', 'e', 'i', 'l', 'n', 'o', 'r', 'r', 's', 't', 'u', 'u', 'u', 'u', 'y', 'z', 'A', 'A', 'C', 'D', 'E', 'E', 'I', 'L', 'N', 'O', 'R', 'S', 'T', 'U', 'U', 'Y', 'Z'};
        newStr = newStr.replaceAll(" ", "").toLowerCase();
        for (int i = 0; i < o.length; i++) {
            newStr = newStr.replace(o[i], n[i]);
        }
        newStr = newStr.replace(" ", "");
        return newStr;
    }

    /**
     * 
     * @param value
     * @return
     */
    public static String integerMD5(String value){
        //long l = checkSum(generate(value));
        //System.out.println(value + " -> " + l);
        return Long.toString(checkSum(generate(value)));
    }
    
    public static String generate(String[] params) {
        String key = "";
        for(String s: params){
            if(s!=null){
                key += s.replaceAll(" ", "").toLowerCase();
            }
        }
        return generate(normalize(key));
    }

    /**
     * 
     * @param value
     * @return
     */
    public static String generate(String value) {
        String md5val = "";
        MessageDigest algorithm = null;

        try {
            algorithm = MessageDigest.getInstance("MD5");
            // SHA-256
        } catch (NoSuchAlgorithmException nsae) {
            return null;
        }

        byte[] defaultBytes = value.getBytes();
        algorithm.reset();
        algorithm.update(defaultBytes);
        byte messageDigest[] = algorithm.digest();
        StringBuilder hexString = new StringBuilder();

        for (int i = 0; i < messageDigest.length; i++) {
            String hex = Integer.toHexString(0xFF & messageDigest[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        md5val = hexString.toString();
        return md5val;
    }

    /**
     * 
     * @param value
     * @return
     */
    public static long checkSum(String value) {
        try {
            byte buffer[] = value.getBytes();
            ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
            CheckedInputStream cis = new CheckedInputStream(bais, new Adler32());
            byte readBuffer[] = new byte[5];
            if (cis.read(readBuffer) >= 0) {
                return cis.getChecksum().getValue();
            }else{
                return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }
}



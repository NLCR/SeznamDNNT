package cz.inovatika.sdnnt.services.impl;

import java.util.Iterator;

public class SpreadSheetIndexMapper {
        
    // spreadsheet cells
    public static final int A = 0;
    public static final int B = 1;
    public static final int C = 2;
    public static final int D = 3;
    public static final int E = 4;
    public static final int F = 5;
    public static final int G = 6;
    public static final int H = 7;
    public static final int I = 8;
    public static final int J = 9;
    public static final int K = 10;
    public static final int L = 11;
    public static final int M = 12;
    public static final int N = 13;
    public static final int O = 14;
    public static final int P = 15;
    public static final int Q = 16;
    public static final int R = 17;
    public static final int S = 18;
    public static final int T = 19;
    public static final int U = 20;
    public static final int V = 21;
    public static final int W = 22;
    public static final int X = 23;
    public static final int Y = 24;
    public static final int Z = 25;
    public static final int AA = 26;
    public static final int AB = 27;
    public static final int AC = 28;
    public static final int AD = 29;
    public static final int AE = 30;
    public static final int AF = 31;
    public static final int AG = 32;
    public static final int AH = 33;
    public static final int AI = 34;
    public static final int AJ = 35;
    public static final int AK = 36;
    public static final int AL = 37;
    public static final int AM = 38;
    public static final int AN = 39;
    public static final int AO = 40;
    public static final int AP = 41;
    public static final int AQ = 42;
    public static final int AR = 43;
    public static final int AS = 44;
    public static final int AT = 45;
    public static final int AU = 46;
    public static final int AV = 47;
    public static final int AW = 48;
    public static final int AX = 49;
    public static final int AY = 50;
    public static final int AZ = 51;
    public static final int BA = 52;
    public static final int BB = 53;
    public static final int BC = 54;
    public static final int BD = 55;
    public static final int BE = 56;
    public static final int BF = 57;
    public static final int BG = 58;
    public static final int BH = 59;
    public static final int BI = 60;
    public static final int BJ = 61;
    public static final int BK = 62;
    public static final int BL = 63;
    public static final int BM = 64;
    public static final int BN = 65;
    public static final int BO = 66;
    public static final int BP = 67;
    public static final int BQ = 68;
    public static final int BR = 69;
    public static final int BS = 70;
    public static final int BT = 71;
    public static final int BU = 72;
    public static final int BV = 73;
    public static final int BW = 74;
    public static final int BX = 75;
    public static final int BY = 76;
    public static final int BZ = 77;


    public static int addressAfterRange(int start, int step, int count) {
        return (start + (step*count));
    }
    


    private static char toChar(int index) {
        char retchar = (char) (65+index);
        return retchar;
    }

    public static String toColumnAddress(int index) {
        if (index < 26) {
            char oneChar = toChar(index);
            return String.format("%c", oneChar);
        } else {
            char baseChar = toChar((index/26)-1);
            char offsetChar = toChar(index%26);
            return String.format("%c%c", baseChar, offsetChar);
            
        }
    }
    
    
    private static void generateMapping() {
        for (int i = 0; i < 3*26; i++) {
            String address = toColumnAddress(i);
            String statement = String.format("public static final int %s = %d;", address, i);
            System.out.println(statement);
        }
    }
}

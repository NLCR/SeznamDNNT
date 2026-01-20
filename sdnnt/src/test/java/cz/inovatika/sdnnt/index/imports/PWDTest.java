package cz.inovatika.sdnnt.index.imports;

import org.apache.commons.codec.digest.DigestUtils;

public class PWDTest {

    public static void main(String[] args) {
        String test = DigestUtils.sha256Hex("armoAoito1.");
        System.out.println(test);
    }
}

package cz.inovatika.sdnnt.jwt;

import cz.inovatika.sdnnt.UserController;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

public class GeneratePswdTest {

    @Test
    public void testGeneratePSWD() {
        String admin = DigestUtils.sha256Hex("admin");
        System.out.println(admin);
    }
}

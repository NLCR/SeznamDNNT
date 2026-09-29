package cz.inovatika.sdnnt.services.impl.users;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class RememberMeServiceTest {

    @Test
    public void generatesRandomUrlSafeTokensAndStableHashes() {
        String first = RememberMeService.newToken();
        String second = RememberMeService.newToken();

        assertNotEquals(first, second);
        assertTrue(first.matches("[A-Za-z0-9_-]{43}"));
        assertEquals(64, RememberMeService.hash(first).length());
        assertEquals(RememberMeService.hash(first), RememberMeService.hash(first));
        assertNotEquals(RememberMeService.hash(first), RememberMeService.hash(second));
    }
}

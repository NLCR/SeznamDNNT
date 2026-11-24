package cz.inovatika.sdnnt.openapi.endpoints.api;

import static org.junit.Assert.assertEquals;

import java.text.DateFormat;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import org.junit.Test;

public class RFC3339DateFormatTest {

    @Test
    public void preservesUtcMillisecondsAfterClone() throws ParseException {
        DateFormat formatter = (DateFormat) new RFC3339DateFormat().clone();
        Date date = Date.from(Instant.parse("2020-01-02T03:04:05.006Z"));

        assertEquals("2020-01-02T03:04:05.006Z", formatter.format(date));
        assertEquals(date, formatter.parse("2020-01-02T03:04:05.006Z"));
    }
}

package cz.inovatika.sdnnt.model;

import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static cz.inovatika.sdnnt.model.Period.*;


public class PeriodTests {

    public static final SimpleDateFormat SIMPLE_DATE_FORMAT_1 = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT_2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Test
    public void testPeriod_0() throws ParseException {
        // 1.8 deadline
        String format = SIMPLE_DATE_FORMAT_2.format(period_0.defineDeadline(SIMPLE_DATE_FORMAT_1.parse("1993-03-02")));
        Assert.assertEquals("1993-08-01 00:00:00", format);

        format = SIMPLE_DATE_FORMAT_2.format(period_0.defineDeadline(SIMPLE_DATE_FORMAT_1.parse("1993-04-28")));
        Assert.assertEquals("1993-08-01 00:00:00", format);

        format = SIMPLE_DATE_FORMAT_2.format(period_0.defineDeadline(SIMPLE_DATE_FORMAT_1.parse("1993-06-15")));
        Assert.assertEquals("1993-08-01 00:00:00", format);

        format = SIMPLE_DATE_FORMAT_2.format(period_0.defineDeadline(SIMPLE_DATE_FORMAT_1.parse("1993-07-02")));
        Assert.assertEquals("1993-08-01 00:00:00", format);

        format = SIMPLE_DATE_FORMAT_2.format(period_0.defineDeadline(SIMPLE_DATE_FORMAT_1.parse("1993-07-02")));
        Assert.assertEquals("1993-08-01 00:00:00", format);

        //1.2
        format = SIMPLE_DATE_FORMAT_2.format(period_0.defineDeadline(SIMPLE_DATE_FORMAT_1.parse("2020-01-02")));
        Assert.assertEquals("2020-02-01 00:00:00", format);

        format = SIMPLE_DATE_FORMAT_2.format(period_0.defineDeadline(SIMPLE_DATE_FORMAT_1.parse("2019-12-01")));
        Assert.assertEquals("2020-02-01 00:00:00", format);

        format = SIMPLE_DATE_FORMAT_2.format(period_0.defineDeadline(SIMPLE_DATE_FORMAT_1.parse("2020-11-15")));
        Assert.assertEquals("2021-02-01 00:00:00", format);

        format = SIMPLE_DATE_FORMAT_2.format(period_0.defineDeadline(SIMPLE_DATE_FORMAT_1.parse("2020-01-31")));
        Assert.assertEquals("2020-02-01 00:00:00", format);
    }

    @Test
    public void testPeriod_1() throws ParseException {
        // +6 month deadline
        String format = SIMPLE_DATE_FORMAT_2.format(period_1.defineDeadline(SIMPLE_DATE_FORMAT_2.parse("1993-03-02 03:03:03")));
        Assert.assertEquals("1993-09-02 00:00:00", format);

        format = SIMPLE_DATE_FORMAT_2.format(period_1.defineDeadline(SIMPLE_DATE_FORMAT_2.parse("1995-09-02 03:03:03")));
        Assert.assertEquals("1996-03-02 00:00:00", format);

        format = SIMPLE_DATE_FORMAT_2.format(period_1.defineDeadline(SIMPLE_DATE_FORMAT_2.parse("2021-07-02 03:03:03")));
        Assert.assertEquals("2022-01-02 00:00:00", format);
    }

    @Test
    public void testPeriod_2() throws ParseException {
        // +1 month deadline
        String format = SIMPLE_DATE_FORMAT_2.format(period_2.defineDeadline(SIMPLE_DATE_FORMAT_2.parse("1993-03-02 03:03:03")));
        Assert.assertEquals("1993-04-02 00:00:00", format);

        format = SIMPLE_DATE_FORMAT_2.format(period_2.defineDeadline(SIMPLE_DATE_FORMAT_2.parse("1995-01-31 03:03:03")));
        Assert.assertEquals("1995-02-28 00:00:00", format);
    }

    @Test
    public void testPeriod_3() throws ParseException {
        // +5 working days deadline  - no public holidays
        String format = SIMPLE_DATE_FORMAT_2.format(period_3.defineDeadline(SIMPLE_DATE_FORMAT_2.parse("2021-10-31 05:13:03")));
        Assert.assertEquals("2021-11-05 23:59:00",format);

        format = SIMPLE_DATE_FORMAT_2.format(period_3.defineDeadline(SIMPLE_DATE_FORMAT_2.parse("2021-11-05 23:59:00")));
        Assert.assertEquals("2021-11-12 23:59:00",format);

        format = SIMPLE_DATE_FORMAT_2.format(period_3.defineDeadline(SIMPLE_DATE_FORMAT_2.parse("2021-10-06 05:13:03")));
        Assert.assertEquals("2021-10-13 23:59:00",format);
    }

    @Test
    public void testPeriod_3_1() throws ParseException {
        // +5 working days deadline  - no public holidays
        String format = SIMPLE_DATE_FORMAT_2.format(period_3_1.defineDeadline(SIMPLE_DATE_FORMAT_2.parse("2021-10-31 05:13:03")));
        Assert.assertEquals("2021-11-12 23:59:00",format);

        format = SIMPLE_DATE_FORMAT_2.format(period_3_1.defineDeadline(SIMPLE_DATE_FORMAT_2.parse("2021-10-06 05:13:03")));
        Assert.assertEquals("2021-10-20 23:59:00",format);
    }

    @Test
    public void testPeriod_4() throws ParseException {
        // +18 months deadline
        String format = SIMPLE_DATE_FORMAT_2.format(period_4.defineDeadline(SIMPLE_DATE_FORMAT_2.parse("2021-10-31 05:13:03")));
        Assert.assertEquals("2023-04-30 00:00:00",format );

    }

}

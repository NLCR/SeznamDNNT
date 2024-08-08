package cz.inovatika.sdnnt.utils;

import org.junit.Assert;
import org.junit.Test;

import cz.inovatika.sdnnt.utils.DetectYear.Bound;

public class DetectYearTest {
    
    @Test
    public void testDetectYear() {
        Assert.assertTrue(DetectYear.detectYear("1999", Bound.UP) == 1999);
        Assert.assertTrue(DetectYear.detectYear("1999", Bound.DOWN) == 1999);

        Assert.assertTrue(DetectYear.detectYear("199", Bound.UP) == 1999);
        Assert.assertTrue(DetectYear.detectYear("199", Bound.DOWN) == 1990);

        Assert.assertTrue(DetectYear.detectYear("19998", Bound.UP) == 1999);
        Assert.assertTrue(DetectYear.detectYear("1999a", Bound.DOWN) == 1999);

        Assert.assertTrue(DetectYear.detectYear("1uuu", Bound.UP) == 1999);
        Assert.assertTrue(DetectYear.detectYear("1uuu", Bound.DOWN) == 1000);

        Assert.assertTrue(DetectYear.detectYear("1uuuu", Bound.UP) == 1999);
        Assert.assertTrue(DetectYear.detectYear("1uuuu", Bound.DOWN) == 1000);

        Assert.assertTrue(DetectYear.detectYear("uuuuu", Bound.UP) == 9999);
        Assert.assertTrue(DetectYear.detectYear("uuuuu", Bound.DOWN) == 0);

        Assert.assertTrue(DetectYear.detectYear("    ", Bound.DOWN) == 0);
        Assert.assertTrue(DetectYear.detectYear("    ", Bound.UP) == 9999);

    }
}

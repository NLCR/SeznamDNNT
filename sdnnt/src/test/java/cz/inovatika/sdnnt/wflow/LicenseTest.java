package cz.inovatika.sdnnt.wflow;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class LicenseTest {

    @Test
    public void testLicenses() {
        Assert.assertTrue(License.findLincese(Arrays.asList("A", "NZ")).equals(License.dnntt));
        Assert.assertTrue(License.findLincese(Arrays.asList("NZ","A")).equals(License.dnntt));
        Assert.assertTrue(License.findLincese(Arrays.asList("NZ")).equals(License.dnntt));
        Assert.assertTrue(License.findLincese(Arrays.asList("A" )).equals(License.dnnto));

        Assert.assertTrue(License.findLincese(Arrays.asList("N")) == null);
        Assert.assertTrue(License.findLincese(Arrays.asList("PA")) == null);
    }
}

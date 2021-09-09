package cz.inovatika.sdnnt.utils;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

public class IPAddressUtilsTest {

    @Test
    public void testLocalAddress1() {
        final HttpServletRequest reqMock = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(reqMock.getRemoteAddr()).andReturn("localhost");
        EasyMock.expect(reqMock.getHeader("X-Forwarded-For")).andReturn(null);
        EasyMock.replay(reqMock);
        boolean localAddress = IPAddressUtils.isLocalAddress(reqMock);
        Assert.assertTrue(localAddress);
    }

    @Test
    public void testLocalAddress2() {
        final HttpServletRequest reqMock = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(reqMock.getRemoteAddr()).andReturn("127.0.0.1");
        EasyMock.expect(reqMock.getHeader("X-Forwarded-For")).andReturn(null);
        EasyMock.replay(reqMock);
        boolean localAddress = IPAddressUtils.isLocalAddress(reqMock);
        Assert.assertTrue(localAddress);
    }

    @Test
    public void testLocalAddress3() {
        final HttpServletRequest reqMock = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(reqMock.getRemoteAddr()).andReturn("127.0.0.2");
        EasyMock.expect(reqMock.getHeader("X-Forwarded-For")).andReturn(null);
        EasyMock.replay(reqMock);
        boolean localAddress = IPAddressUtils.isLocalAddress(reqMock);
        Assert.assertFalse(localAddress);
    }

}

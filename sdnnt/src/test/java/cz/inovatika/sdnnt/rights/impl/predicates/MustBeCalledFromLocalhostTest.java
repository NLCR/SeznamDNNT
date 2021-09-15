package cz.inovatika.sdnnt.rights.impl.predicates;

import cz.inovatika.sdnnt.utils.IPAddressUtils;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

public class MustBeCalledFromLocalhostTest {

    @Test
    public void testPredicateTrue() {
        Assert.assertTrue(new MustBeCalledFromLocalhost().permit(mockRequest("127.0.0.1")));
    }

    @Test
    public void testPredicateFalse() {
        Assert.assertFalse(new MustBeCalledFromLocalhost().permit(mockRequest("192.1.16.1")));
    }


    private HttpServletRequest mockRequest(String address) {
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(request.getHeader(EasyMock.eq(IPAddressUtils.X_IP_FORWARD))).andReturn(null).anyTimes();
        EasyMock.expect(request.getRemoteAddr()).andReturn(address).times(1);
        EasyMock.replay(request);
        return request;
    }

}

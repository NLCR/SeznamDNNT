package cz.inovatika.sdnnt.rights.impl.predicates;

import cz.inovatika.sdnnt.UserController;
import cz.inovatika.sdnnt.indexer.models.User;
import cz.inovatika.sdnnt.utils.IPAddressUtils;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class MustBeLoggedTest {

    @Test
    public void testMustBeLoggedTrue() {
        Assert.assertTrue(new MustBeLogged().permit(mockRequest(true)));
    }

    @Test
    public void testMustBeLoggedFalse() {
        Assert.assertFalse(new MustBeLogged().permit(mockRequest(false)));
    }

    private HttpServletRequest mockRequest(boolean userInSession) {
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        HttpSession session = EasyMock.createMock(HttpSession.class);
        if (userInSession) {
            User testingUser = new User();
            testingUser.username = "testinguser";

            EasyMock.expect(session.getAttribute(UserController.AUTHENTICATED_USER)).andReturn(testingUser).anyTimes();
            EasyMock.expect(request.getSession(true)).andReturn(session).anyTimes();
        } else {
            EasyMock.expect(session.getAttribute(UserController.AUTHENTICATED_USER)).andReturn(null).anyTimes();
            EasyMock.expect(request.getSession(true)).andReturn(session).anyTimes();
        }
        EasyMock.expect(request.getSession()).andReturn(session).anyTimes();

        EasyMock.replay(request,session);
        return request;
    }

}

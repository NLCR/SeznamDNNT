package cz.inovatika.sdnnt.rights.impl.predicates;

import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.UserControler;
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
            testingUser.setUsername( "testinguser");

            EasyMock.expect(session.getAttribute(ApplicationUserLoginSupport.AUTHENTICATED_USER)).andReturn(testingUser).anyTimes();
            EasyMock.expect(request.getSession(true)).andReturn(session).anyTimes();
        } else {
            EasyMock.expect(session.getAttribute(ApplicationUserLoginSupport.AUTHENTICATED_USER)).andReturn(null).anyTimes();
            EasyMock.expect(request.getSession(true)).andReturn(session).anyTimes();
        }
        EasyMock.expect(request.getSession()).andReturn(session).anyTimes();

        EasyMock.replay(request,session);
        return request;
    }

}

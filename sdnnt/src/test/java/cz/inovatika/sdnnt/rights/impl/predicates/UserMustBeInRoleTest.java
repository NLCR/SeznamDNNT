package cz.inovatika.sdnnt.rights.impl.predicates;

import cz.inovatika.sdnnt.indexer.models.User;
import cz.inovatika.sdnnt.rights.Role;
import cz.inovatika.sdnnt.services.UserControler;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class UserMustBeInRoleTest {

    @Test
    public void testMustBeInRoleTrue() {
        Assert.assertTrue(new UserMustBeInRole(Role.knihovna).permit(mockRequest(true, Role.knihovna)));
    }

    @Test
    public void testMustBeInRoleFalse_BadRole() {
        Assert.assertFalse(new UserMustBeInRole(Role.knihovna).permit(mockRequest(true, Role.user)));
    }

    @Test
    public void testMustBeInRole_NoUser() {
        Assert.assertFalse(new UserMustBeInRole(Role.knihovna).permit(mockRequest(false, null)));

    }

    private HttpServletRequest mockRequest(boolean userInSession, Role role) {
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        HttpSession session = EasyMock.createMock(HttpSession.class);
        if (userInSession) {
            User testingUser = new User();
            testingUser.username = "testinguser";
            testingUser.role = role.name();
            EasyMock.expect(session.getAttribute(UserControler.AUTHENTICATED_USER)).andReturn(testingUser).anyTimes();
            EasyMock.expect(request.getSession(true)).andReturn(session).anyTimes();
        } else {
            EasyMock.expect(session.getAttribute(UserControler.AUTHENTICATED_USER)).andReturn(null).anyTimes();
            EasyMock.expect(request.getSession(true)).andReturn(session).anyTimes();
        }
        EasyMock.expect(request.getSession()).andReturn(session).anyTimes();

        EasyMock.replay(request,session);
        return request;
    }

}

package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.indexer.models.NotificationInterval;
import cz.inovatika.sdnnt.indexer.models.User;
import cz.inovatika.sdnnt.it.EmbeddedServerPrepare;
import cz.inovatika.sdnnt.rights.Role;
import cz.inovatika.sdnnt.rights.exceptions.NotAuthorizedException;
import cz.inovatika.sdnnt.services.MailService;
import cz.inovatika.sdnnt.services.UserControler;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;
import cz.inovatika.sdnnt.tracking.TrackingFilter;
import cz.inovatika.sdnnt.utils.TestServletStream;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.mail.EmailException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.easymock.EasyMock;
import org.json.JSONObject;
import org.junit.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.easymock.EasyMock.*;

public class UserControlerImplTest {

    public static EmbeddedServerPrepare prepare;

    @BeforeClass
    public static void beforeClass() throws Exception {
        prepare = new EmbeddedServerPrepare();
        prepare.setupBeforeClass();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        prepare.tearDownAfterClass();
    }

    @Before
    public void setUpTest() throws Exception {
        prepare.deleteCores("users");
    }

    /** Test register and loging */
    @Test
    public void testRegistrationAndLogin() throws NotAuthorizedException, UserControlerException, IOException, SolrServerException, EmailException {
        // delete solr
        SolrQuery solrQuery = new SolrQuery().setQuery("*:*");
        QueryResponse response = prepare.getClient().query("users", solrQuery);
        long numFound = response.getResults().getNumFound();
        Assert.assertTrue(numFound == 0);

        // registration + mail
        User regUser = testUser("1");

        MailServiceImpl sendRegistrationMail = EasyMock.createMockBuilder(MailServiceImpl.class).withConstructor().
                addMockedMethod("sendRegistrationMail").createMock();

        // storing password available in mail service
        AtomicReference<String> pswd = new AtomicReference<>();
        sendRegistrationMail.sendRegistrationMail(EasyMock.anyObject(User.class), EasyMock.anyObject(), EasyMock.anyObject(String.class), EasyMock.anyObject(String.class));
        EasyMock.expectLastCall().andAnswer(()->{
            Object[] currentArguments = getCurrentArguments();
            pswd.set(currentArguments[2].toString());
            return null;
        });

        registerOneUser(regUser,sendRegistrationMail,  EasyMock.createMock(HttpServletRequest.class));

        // find user
        List<User> users = prepare.getClient().query("users", solrQuery).getBeans(User.class);
        Assert.assertTrue(users.size() == 1);

        // login
        HttpServletRequest loginRequest = EasyMock.createMock(HttpServletRequest.class);
        HttpSession loginSession = EasyMock.createMock(HttpSession.class);
        loginRequest(users.get(0), pswd.get(), loginRequest, loginSession);

        loginSession.setAttribute("user", users.get(0));
        EasyMock.expectLastCall().times(1);

        loginSession.setMaxInactiveInterval(TrackingFilter.MAX_INACTIVE_INTERVAL);
        EasyMock.expectLastCall().times(1);

        loginSession.setAttribute(EasyMock.eq("SESSION_UPDATED_DATE"), anyObject(Date.class));
        EasyMock.expectLastCall().times(1);

        UserControlerImpl userControler = EasyMock.createMockBuilder(UserControlerImpl.class)
                .withConstructor(loginRequest)
                .addMockedMethod("buildClient").createMock();


        EasyMock.expect(userControler.buildClient()).andReturn(prepare.getClient()).anyTimes();
        EasyMock.replay(loginRequest, loginSession, userControler);

        User login = userControler.login();

        Assert.assertTrue(login.username.equals(users.get(0).username));
        Assert.assertTrue(login.pwd == null);

    }

    /** Test get users  by role*/
    @Test
    public void testGetByRole() throws EmailException, IOException, UserControlerException, SolrServerException {

        SolrQuery solrQuery = new SolrQuery().setQuery("*:*");
        QueryResponse response = prepare.getClient().query("users", solrQuery);
        long numFound = response.getResults().getNumFound();
        Assert.assertTrue(numFound == 0);

        registerUsers();

        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        HttpSession session = EasyMock.createMock(HttpSession.class);

        UserControlerImpl userControler = EasyMock.createMockBuilder(UserControlerImpl.class)
                .withConstructor(request)
                .addMockedMethod("buildClient").createMock();

        EasyMock.expect(userControler.buildClient()).andReturn(prepare.getClient()).anyTimes();

        EasyMock.replay(request, session, userControler);

        List<User> usersByRole = userControler.findUsersByRole(Role.user);
        Assert.assertTrue(usersByRole.size() == 3);

        usersByRole.get(0).role = Role.knihovna.name();
        userControler.save(usersByRole.get(0));

        usersByRole.get(1).role = Role.kurator.name();
        userControler.save(usersByRole.get(1));

        usersByRole.get(2).role = Role.user.name();
        userControler.save(usersByRole.get(2));

        Assert.assertTrue(userControler.findUsersByRole(Role.user).size() == 1);
        Assert.assertTrue(userControler.findUsersByRole(Role.knihovna).size() == 1);
        Assert.assertTrue(userControler.findUsersByRole(Role.kurator).size() == 1);

    }

    /** Test get users  by role*/
    @Test
    public void testGetByFindByPrefix() throws EmailException, IOException, UserControlerException, SolrServerException {

        SolrQuery solrQuery = new SolrQuery().setQuery("*:*");
        QueryResponse response = prepare.getClient().query("users", solrQuery);
        long numFound = response.getResults().getNumFound();
        Assert.assertTrue(numFound == 0);

        registerUsers();

        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        HttpSession session = EasyMock.createMock(HttpSession.class);

        UserControlerImpl userControler = EasyMock.createMockBuilder(UserControlerImpl.class)
                .withConstructor(request)
                .addMockedMethod("buildClient").createMock();

        EasyMock.expect(userControler.buildClient()).andReturn(prepare.getClient()).anyTimes();

        EasyMock.replay(request, session, userControler);

        List<User> usersByRole = userControler.findUsersByPrefix("test");
        Assert.assertTrue(usersByRole.size() == 3);

        usersByRole.get(0).role = Role.knihovna.name();
        userControler.save(usersByRole.get(0));

        usersByRole.get(1).role = Role.kurator.name();
        userControler.save(usersByRole.get(1));

        usersByRole.get(2).role = Role.user.name();
        userControler.save(usersByRole.get(2));

        Assert.assertTrue(userControler.findUsersByRole(Role.user).size() == 1);
        Assert.assertTrue(userControler.findUsersByRole(Role.knihovna).size() == 1);
        Assert.assertTrue(userControler.findUsersByRole(Role.kurator).size() == 1);

    }


    /** Test get all users  */
    @Test
    public void testGetAll() throws EmailException, IOException, UserControlerException, SolrServerException {

        SolrQuery solrQuery = new SolrQuery().setQuery("*:*");
        QueryResponse response = prepare.getClient().query("users", solrQuery);
        long numFound = response.getResults().getNumFound();
        Assert.assertTrue(numFound == 0);

        registerUsers();

        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        HttpSession session = EasyMock.createMock(HttpSession.class);

        UserControlerImpl userControler = EasyMock.createMockBuilder(UserControlerImpl.class)
                .withConstructor(request)
                .addMockedMethod("buildClient").createMock();

        EasyMock.expect(userControler.buildClient()).andReturn(prepare.getClient()).anyTimes();

        EasyMock.replay(request, session, userControler);
        List<User> all = userControler.getAll();
        Assert.assertTrue(all.size() == 3);

        Assert.assertTrue(all.get(0).username.equals("test1"));
        Assert.assertTrue(all.get(1).username.equals("test2"));
        Assert.assertTrue(all.get(2).username.equals("test3"));

        Assert.assertTrue(all.get(0).jmeno.equals("TestJmeno_1"));
        Assert.assertTrue(all.get(1).jmeno.equals("TestJmeno_2"));
        Assert.assertTrue(all.get(2).jmeno.equals("TestJmeno_3"));

        Assert.assertTrue(all.get(0).prijmeni.equals("TestPrijmeni_1"));
        Assert.assertTrue(all.get(1).prijmeni.equals("TestPrijmeni_2"));
        Assert.assertTrue(all.get(2).prijmeni.equals("TestPrijmeni_3"));

        Assert.assertTrue(all.get(0).pwd == null);
        Assert.assertTrue(all.get(1).pwd == null);
        Assert.assertTrue(all.get(2).pwd == null);
    }

    /** Test save API key and find by API key */
    @Test
    public void testSaveAndGetByApiKey() throws IOException, SolrServerException, EmailException, UserControlerException {

        SolrQuery solrQuery = new SolrQuery().setQuery("*:*");
        QueryResponse response = prepare.getClient().query("users", solrQuery);
        long numFound = response.getResults().getNumFound();
        Assert.assertTrue(numFound == 0);

        registerUsers();

        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        UserControlerImpl userControler = EasyMock.createMockBuilder(UserControlerImpl.class)
                .withConstructor(request)
                .addMockedMethod("buildClient").createMock();

        EasyMock.expect(userControler.buildClient()).andReturn(prepare.getClient()).anyTimes();

        EasyMock.replay(request,  userControler);


        User u1 = userControler.findUser("test1");
        Assert.assertTrue(u1 != null);

        u1.apikey = "API-KEY-1";
        userControler.save(u1);

        User ur1 = userControler.findUserByApiKey("API-KEY-1");
        Assert.assertEquals(u1, ur1);
    }

    /** Test save by interval and find by interval */
    @Test
    public void testSaveAndGetByInterval() throws IOException, SolrServerException, EmailException, UserControlerException {

        SolrQuery solrQuery = new SolrQuery().setQuery("*:*");
        QueryResponse response = prepare.getClient().query("users", solrQuery);
        long numFound = response.getResults().getNumFound();
        Assert.assertTrue(numFound == 0);

        registerUsers();

        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        UserControlerImpl userControler = EasyMock.createMockBuilder(UserControlerImpl.class)
                .withConstructor(request)
                .addMockedMethod("buildClient").createMock();

        EasyMock.expect(userControler.buildClient()).andReturn(prepare.getClient()).anyTimes();

        EasyMock.replay(request,  userControler);


        User u1 = userControler.findUser("test1");
        Assert.assertTrue(u1 != null);

        User u2 = userControler.findUser("test2");
        Assert.assertTrue(u2 != null);

        u1.notifikace_interval = NotificationInterval.den.name();
        userControler.save(u1);

        u2.notifikace_interval = NotificationInterval.mesic.name();
        userControler.save(u2);

        List<User> den = userControler.findUsersByNotificationInterval(NotificationInterval.den.name());
        Assert.assertTrue(den.size() == 1);

        Assert.assertEquals(den.get(0), u1);

        List<User> mesic = userControler.findUsersByNotificationInterval(NotificationInterval.mesic.name());
        Assert.assertTrue(mesic.size() == 1);

        Assert.assertEquals(mesic.get(0), u2);
    }




    /** Test admin password reset  */
    @Test
    public void testAdminResetPswd() throws IOException, SolrServerException, EmailException, UserControlerException {

        SolrQuery solrQuery = new SolrQuery().setQuery("*:*");
        QueryResponse response = prepare.getClient().query("users", solrQuery);
        long numFound = response.getResults().getNumFound();
        Assert.assertTrue(numFound == 0);

        registerUsers();

        MailService mailService = createMock(MailService.class);

        Pair<String, String> pair = Pair.of("test_1@testovic.cz", "TestJmeno_1 TestPrijmeni_1");

        mailService.sendResetPasswordMail(EasyMock.anyObject(User.class), EasyMock.eq(pair), EasyMock.anyObject(String.class));
        EasyMock.expectLastCall().times(1);

        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        UserControlerImpl userControler = EasyMock.createMockBuilder(UserControlerImpl.class)
                .withConstructor(request, mailService)
                .addMockedMethod("buildClient").createMock();

        EasyMock.expect(userControler.buildClient()).andReturn(prepare.getClient()).anyTimes();

        EasyMock.replay(request,  userControler,mailService);

        User u1 = userControler.findUser("test1");
        Assert.assertTrue(u1 != null);

        userControler.resetPwd(u1.toJSONObject());
        EasyMock.verify(request, userControler, mailService );
    }


    private void registerUsers() throws IOException, EmailException, UserControlerException {
        {

            MailService mailService = createMock(MailService.class);
            mailService.sendRegistrationMail(EasyMock.anyObject(User.class), EasyMock.anyObject(), EasyMock.anyObject(String.class), EasyMock.anyObject(String.class));
            EasyMock.expectLastCall().times(1);

            registerOneUser(testUser("1"), mailService, createMock(HttpServletRequest.class));

        }
        {
            MailService mailService = createMock(MailService.class);
            mailService.sendRegistrationMail(EasyMock.anyObject(User.class), EasyMock.anyObject(), EasyMock.anyObject(String.class), EasyMock.anyObject(String.class));
            EasyMock.expectLastCall().times(1);

            registerOneUser(testUser("2"), mailService, createMock(HttpServletRequest.class));
        }
        {

            MailService mailService = createMock(MailService.class);
            mailService.sendRegistrationMail(EasyMock.anyObject(User.class), EasyMock.anyObject(), EasyMock.anyObject(String.class), EasyMock.anyObject(String.class));
            EasyMock.expectLastCall().times(1);

            registerOneUser(testUser("3"), mailService, createMock(HttpServletRequest.class));
        }
    }





    // register user
    private void registerOneUser(User regUser, MailService mailService, HttpServletRequest request) throws IOException, EmailException, UserControlerException {
        UserControlerImpl userControler = EasyMock.createMockBuilder(UserControlerImpl.class)
                .withConstructor(request, mailService)
                .addMockedMethod("buildClient").createMock();

        EasyMock.expect(userControler.buildClient()).andReturn(prepare.getClient()).anyTimes();
        EasyMock.replay(/*request,session.*/ userControler,mailService);

        userControler.register(regUser.toJSONObject().toString());
        EasyMock.verify(userControler, mailService);
    }


    private User testUser(String postfix) {
        User testUser = new User();
        testUser.username="test"+postfix;
        testUser.jmeno="TestJmeno_"+postfix;
        testUser.prijmeni="TestPrijmeni_"+postfix;
        testUser.role = Role.user.name();
        testUser.email = "test_"+postfix+"@testovic.cz";
        testUser.institution = "institution";
        return testUser;
    }


    private void loginRequest(User user, String pwd ,HttpServletRequest request, HttpSession session) throws IOException {
        JSONObject object = new JSONObject();
        object.put("user", user.username);
        object.put("pwd", pwd);

        EasyMock.expect(session.getAttribute(UserControler.AUTHENTICATED_USER)).andReturn(null).anyTimes();
        EasyMock.expect(request.getSession(true)).andReturn(session).anyTimes();
        EasyMock.expect(request.getSession()).andReturn(session).anyTimes();
        TestServletStream stream = new TestServletStream(new ByteArrayInputStream(object.toString().getBytes()));
        EasyMock.expect(request.getInputStream()).andReturn(stream).times(1);
    }

}

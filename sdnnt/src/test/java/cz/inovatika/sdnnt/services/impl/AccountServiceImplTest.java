package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.indexer.models.User;
import cz.inovatika.sdnnt.indexer.models.Zadost;
import cz.inovatika.sdnnt.it.EmbeddedServerPrepare;
import cz.inovatika.sdnnt.services.AccountService;
import cz.inovatika.sdnnt.services.UserControler;
import cz.inovatika.sdnnt.wflow.NavrhWorklflow;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.easymock.EasyMock;
import org.junit.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class AccountServiceImplTest {


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
        prepare.deleteCores("users","zadost","catalog");
    }


    @Test
    public void testSaveZadost() throws IOException, SolrServerException {
        User user = testUser();
        UserControler controler  = EasyMock.createMock(UserControler.class);

        AccountServiceImpl service = EasyMock.createMockBuilder(AccountServiceImpl.class)
                .withConstructor(controler)
                .addMockedMethod("buildClient").createMock();

        EasyMock.expect(controler.getUser()).andReturn(user).anyTimes();
        EasyMock.expect(service.buildClient()).andReturn(prepare.getClient()).anyTimes();

        EasyMock.replay(controler, service);


        Zadost zadost = new Zadost();
        zadost.identifiers = Arrays.asList("oai:aleph-nkp.cz:DNT01-000057930");
        zadost.id="pokusny11234";
        zadost.institution="NKP";
        zadost.user="pokusny";
        zadost.navrh= NavrhWorklflow.NZN.name();

        service.saveRequest(zadost.toJSON().toString(), user);

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery("*:*");
        QueryResponse catalog = prepare.getClient().query("zadost", solrQuery);
        List<Zadost> beans = catalog.getBeans(Zadost.class);
        Assert.assertTrue(beans.size() == 1);

        Assert.assertTrue(beans.get(0).identifiers.equals(zadost.identifiers));
        Assert.assertTrue(beans.get(0).id.equals(zadost.id));
        Assert.assertTrue(beans.get(0).institution.equals(zadost.institution));
        Assert.assertTrue(beans.get(0).user.equals(zadost.user));
        Assert.assertTrue(beans.get(0).navrh.equals(zadost.navrh));
    }

    private User testUser() {
        User user = new User();
        user.institution="NKP";
        user.username = "pokusny";
        user.jmeno = "Jmeno";
        user.prijmeni = "Prijmeni";
        return user;
    }
}

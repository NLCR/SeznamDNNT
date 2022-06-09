package cz.inovatika.sdnnt.utils;

import java.io.File;
import java.util.List;

import org.easymock.EasyMock;
import org.json.JSONObject;

import cz.inovatika.sdnnt.InitServlet;
import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.services.PXKrameriusService;
import cz.inovatika.sdnnt.services.impl.PXKrameriusServiceImpl;
import cz.inovatika.sdnnt.services.impl.PXYearServiceImpl;
import cz.inovatika.sdnnt.services.impl.hackcerts.HttpsTrustManager;

public class PXKrameriusTest {

    public static void main(String[] args) {
        check();
        //baseURL();
    }

    private static void baseURL() {
        InitServlet.CONFIG_DIR = System.getProperty("user.home")+File.separator+InitServlet.CONFIG_DIR;
        Options instance = Options.getInstance();
        
        JSONObject jsonObject = Options.getInstance().getJSONObject("check_kramerius");

        String optionsConfig="{\"check_kramerius\":{\"urls\":{\"https://www.digitalniknihovna.cz/mzk/\":{\"api\":\"https://kramerius.mzk.cz/search/\"},\"http://www.digitalniknihovna.cz/mlp/\":{\"api\":\"https://kramerius4.mlp.cz/search/\"},\"http://www.digitalniknihovna.cz/mzk/\":{\"api\":\"https://kramerius.mzk.cz/search/\"},\"https://digitalniknihovna.mlp.cz/\":{\"api\":\"https://kramerius4.mlp.cz/search/\"},\"https://kramerius.lib.cas.cz/\":{\"api\":\"https://kramerius.lib.cas.cz/search/\"},\"https://kramerius.techlib.cz/kramerius-web-client/\":{\"api\":\"https://kramerius.techlib.cz/search/\"},\"http://krameriusndk.mzk.cz/search\":{\"api\":\"https://kramerius.mzk.cz/search/\"},\"https://krameriusndk.mzk.cz/search\":{\"api\":\"https://kramerius.mzk.cz/search/\"}}}}";
        JSONObject optionsConfigJSONObject = new JSONObject(optionsConfig);

        String jonbConfig="{\"iteration\":{\"date_range\":\"[* TO 2020]\",\"states\":[\"A\",\"PA\",\"NL\"]},\"results\":{\"state\":\"PX\",\"ctx\":true,\"request\":{\"type\":\"PXN\",\"items\":50}}}";
        JSONObject jobJSONObject = new JSONObject(jonbConfig);

        PXKrameriusServiceImpl service = new PXKrameriusServiceImpl("TEST",jobJSONObject.getJSONObject("iteration"), jobJSONObject.getJSONObject("results"));
        service.initialize();
        String baseUrl = service.baseUrl("http://kramerius5.nkp.cz/uuid/uuid:bbc60d70-8497-11dd-b069-0013d398622b");
        System.out.println(baseUrl);
    }

    private static void check() {
        long start = System.currentTimeMillis();
        
        InitServlet.CONFIG_DIR = System.getProperty("user.home")+File.separator+InitServlet.CONFIG_DIR;
        Options instance = Options.getInstance();
        
        
        JSONObject jsonObject = Options.getInstance().getJSONObject("check_kramerius");

        String optionsConfig="{\"check_kramerius\":{\"urls\":{\"https://www.digitalniknihovna.cz/mzk/\":{\"api\":\"https://kramerius.mzk.cz/search/\"},\"http://www.digitalniknihovna.cz/mlp/\":{\"api\":\"https://kramerius4.mlp.cz/search/\"},\"http://www.digitalniknihovna.cz/mzk/\":{\"api\":\"https://kramerius.mzk.cz/search/\"},\"https://digitalniknihovna.mlp.cz/\":{\"api\":\"https://kramerius4.mlp.cz/search/\"},\"https://kramerius.lib.cas.cz/\":{\"api\":\"https://kramerius.lib.cas.cz/search/\"},\"https://kramerius.techlib.cz/kramerius-web-client/\":{\"api\":\"https://kramerius.techlib.cz/search/\"},\"http://krameriusndk.mzk.cz/search\":{\"api\":\"https://kramerius.mzk.cz/search/\"},\"https://krameriusndk.mzk.cz/search\":{\"api\":\"https://kramerius.mzk.cz/search/\"}}}}";
        JSONObject optionsConfigJSONObject = new JSONObject(optionsConfig);

        String jonbConfig="{\"iteration\":{\"date_range\":\"[* TO 2020]\",\"states\":[\"A\",\"PA\",\"NL\"]},\"results\":{\"state\":\"PX\",\"ctx\":true,\"request\":{\"type\":\"PXN\",\"items\":50}}}";
        JSONObject jobJSONObject = new JSONObject(jonbConfig);

        HttpsTrustManager.allowAllSSL();
        PXKrameriusService service = new PXKrameriusServiceImpl("TEST",jobJSONObject.getJSONObject("iteration"), jobJSONObject.getJSONObject("results"));

        List<String> check = service.check();
        long stop = System.currentTimeMillis();
        System.out.println("It took "+(stop - start)+" ms ");
    }
}

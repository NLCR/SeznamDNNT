package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.services.AccountService;
import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.PXService;
import cz.inovatika.sdnnt.services.UserControler;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.utils.SimpleGET;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CursorMarkParams;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.security.KeyStore;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.*;

public class PXServiceImpl implements PXService {

    public static final Logger LOGGER = Logger.getLogger(PXServiceImpl.class.getName());

    public static final int LIMIT = 1000;
    public static final Map<String, String> MAPPING_HOSTS =  new HashMap<>();
    static {
        MAPPING_HOSTS.put("https://www.digitalniknihovna.cz/mzk/", "https://kramerius.mzk.cz/search/");
        MAPPING_HOSTS.put("http://www.digitalniknihovna.cz/mzk/", "https://kramerius.mzk.cz/search/");
    }


    @Override
    public void check() {
        Map<String,List<String>> mapping = new HashMap<>();
        CatalogIterationSupport support = new CatalogIterationSupport();
        Map<String,String> reqMap = new HashMap<>();
        reqMap.put("rows", ""+LIMIT);
        support.iterate(reqMap, null, Arrays.asList("id_pid:uuid"), Arrays.asList("dntstav:X", "dntstav:PX"), Arrays.asList(
                IDENTIFIER_FIELD,
                SIGLA_FIELD,
                MARC_911_U,
                MARC_956_U
                ), (rsp) -> {
            Object identifier = rsp.getFieldValue("identifier");

            Collection<Object> links1 = rsp.getFieldValues(MARC_911_U);
            Collection<Object> links2 = rsp.getFieldValues(MARC_956_U);

            if (links1 != null && !links1.isEmpty()) {
                List<String> ll = links1.stream().map(Object::toString).collect(Collectors.toList());
                mapping.put(identifier.toString(), ll);
            } else if (links2 != null && !links2.isEmpty()) {
                List<String> ll = links2.stream().map(Object::toString).collect(Collectors.toList());
                mapping.put(identifier.toString(), ll);
            }
        });

        for (String key : mapping.keySet()) {
            List<String> links = mapping.get(key);
            for (String link :  links) {
                try {
                    String pid = pid(link);
                    String checkUrl= baseUrl(link)+"/api/v5.0/item/"+pid;
                    String s = SimpleGET.get(checkUrl);
                    System.out.println(s);
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE,e.getMessage(),e);
                }
            }
        }

    }

    public static String pid(String surl) {
        if (surl.contains("uuid")){
            return surl.substring(surl.indexOf("uuid"));
        } else return null;
    }

    public static String baseUrl(String surl) {
        if (surl.contains("/search/")){
            return surl.substring(0, surl.indexOf("/search")+"/search".length());
        } else {
            if (surl.contains("uuid")){
                String remapping = surl.substring(0, surl.indexOf("uuid"));
                return MAPPING_HOSTS.get(remapping);
            }
            return null;
        }
    }

    public static void main(String[] args) throws AccountException, IOException, ConflictException, SolrServerException {
        String defaultType = KeyStore.getDefaultType();

        User user = new User();
        user.setJmeno("scheduler");
        user.setPrijmeni("scheduler");
        user.setUsername("scheduler");

        ApplicationUserLoginSupport appSupport = new StaticApplicationLoginSupport(user);

        AccountService accountService = new AccountServiceImpl(appSupport, null);
        JSONObject px = accountService.prepare("PXN");
        Zadost zadost = Zadost.fromJSON(px.toString());
        zadost.addIdentifier("oai:aleph-nkp.cz:DNT01-000000094");

        zadost.setState("waiting");
        accountService.schedulerDefinedCloseRequest(zadost.toJSON().toString());
    }
}

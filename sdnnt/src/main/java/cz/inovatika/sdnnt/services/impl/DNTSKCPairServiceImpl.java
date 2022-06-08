package cz.inovatika.sdnnt.services.impl;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.json.JSONObject;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.workflow.duplicate.Case;
import cz.inovatika.sdnnt.model.workflow.duplicate.DuplicateDNTUtils;
import cz.inovatika.sdnnt.services.DNTSKCPairService;
import cz.inovatika.sdnnt.utils.MarcRecordFields;

public class DNTSKCPairServiceImpl extends AbstractCheckDeleteService implements DNTSKCPairService {
    
    protected Logger logger = Logger.getLogger(DNTSKCPairServiceImpl.class.getName());
    
    public DNTSKCPairServiceImpl(String loggerPostfix, JSONObject results) {
        super(loggerPostfix, results);
        if (loggerPostfix != null) {
            this.logger = Logger.getLogger(DNTSKCPairServiceImpl.class.getName()+"."+loggerPostfix);
        }
    }

    @Override
    // musi vracet dle situace
    protected Map<Case, List<Pair<String, List<String>>>> checkUpdate() throws IOException, SolrServerException {
        Map<Case, List<Pair<String, List<String>>>> retvals = new HashMap<>();
        long start = System.currentTimeMillis();
        CatalogIterationSupport support = new CatalogIterationSupport();
        Map<String, String> reqMap = new HashMap<>();
        reqMap.put("rows", "1000" );
        
        AtomicInteger counter = new AtomicInteger();

        try (final SolrClient solrClient = buildClient()) {
            List<String> plusFilter = Arrays.asList(DNTSTAV_FIELD + ":*", SET_SPEC_FIELD + ":\"DNT-ALL\"");
            List<String> minusFilter = Arrays.asList(KURATORSTAV_FIELD + ":D", KURATORSTAV_FIELD + ":DX");
            support.iterate(solrClient, reqMap, null, plusFilter, minusFilter, Arrays.asList(MarcRecordFields.IDENTIFIER_FIELD), (rsp) -> {
                Object identifier = rsp.getFieldValue("identifier");
                try {
                    MarcRecord fromIndex = MarcRecord.fromIndex(solrClient,  identifier.toString());
                    Pair<Case,List<String>> follower = DuplicateDNTUtils.findDNTFollowers(solrClient, fromIndex);
                    if (!retvals.containsKey(follower.getKey())) {
                        retvals.put(follower.getKey(), new  ArrayList());
                    }
                    retvals.get(follower.getKey()).add(Pair.of(fromIndex.identifier, follower.getRight()));
                    int number = counter.incrementAndGet();
                    if (number % 10000 == 0) {
                        long took = System.currentTimeMillis() - start;
                        debugMesage(number, took,getLogger());
                    }
                } catch (SolrServerException | IOException e) {
                    getLogger().log(Level.SEVERE, e.getMessage(), e);
                }
            }, IDENTIFIER_FIELD);
        } catch(IOException e) {
            getLogger().log(Level.SEVERE, e.getMessage(), e);
        }
        return retvals;
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }

    @Override
    protected List<String> checkDelete() throws IOException, SolrServerException {
        return new ArrayList<>();
    }


    protected Options getOptions() {
        return Options.getInstance();
    }

    protected SolrClient buildClient() {
        return new HttpSolrClient.Builder(getOptions().getString("solr.host")).build();
    }
}

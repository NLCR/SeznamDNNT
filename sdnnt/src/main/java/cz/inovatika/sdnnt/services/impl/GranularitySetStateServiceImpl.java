package cz.inovatika.sdnnt.services.impl;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.GRANULARITY_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.KURATORSTAV_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.MARC_856_U;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.MARC_911_U;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.MARC_956_U;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.SIGLA_FIELD;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.mail.search.ReceivedDateTerm;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;import org.apache.commons.math3.ode.SecondOrderIntegrator;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.LBHttpSolrClient.Rsp;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.search.function.EqualFunction;
import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.index.utils.HistoryObjectUtils;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.openapi.endpoints.api.impl.utils.PIDSupport;
import cz.inovatika.sdnnt.services.GranularityService;
import cz.inovatika.sdnnt.services.GranularitySetStateService;
import cz.inovatika.sdnnt.services.impl.granularities.SolrDocChange;
import cz.inovatika.sdnnt.services.impl.utils.MarcUtils;
import cz.inovatika.sdnnt.services.impl.zahorikutils.ZahorikUtils;
import cz.inovatika.sdnnt.utils.JSONUtils;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.QuartzUtils;
import cz.inovatika.sdnnt.utils.SolrJUtilities;

public class GranularitySetStateServiceImpl extends AbstractGranularityService implements GranularitySetStateService{

    private static final int MAX_FETCHED_DOCS = 1000;

    public static final int CHECK_SIZE = 70;

    private Logger logger = Logger.getLogger(GranularityService.class.getName());

    
    private Set<String> changedIdentifiers = new LinkedHashSet<>();
    private Set<String> duplicatesdentifiers = new LinkedHashSet<>();
    
    public GranularitySetStateServiceImpl(String logger) {
        if (logger != null) {
            this.logger = Logger.getLogger(logger);
        }
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }

    protected Options getOptions() {
        return Options.getInstance();
    }

    protected SolrClient buildClient() {
        return new HttpSolrClient.Builder(getOptions().getString("solr.host")).build();
    }

    
    
    
    @Override
    public void setStatesForOneRecord(MarcRecord marcRecord) throws IOException {
        
    }

    @Override
    public void setStates(List<String> testFilters) throws IOException {
        try (final SolrClient solrClient = buildClient()) {
            Map<String, String> reqMap = new HashMap<>();
            reqMap.put("rows", "10000");

            CatalogIterationSupport support = new CatalogIterationSupport();
            List<String> plusFilter = new ArrayList<>(Arrays.asList(
                MarcRecordFields.GRANULARITY_FIELD + ":*",
                "setSpec:SKC"
            ));
            
            
            if (!testFilters.isEmpty()) {
                testFilters.stream().forEach(plusFilter::add);
            }

            AtomicInteger counter = new AtomicInteger();
            
            List<String> minusFilter = Arrays.asList( KURATORSTAV_FIELD + ":D");
            support.iterate(solrClient, reqMap, null, plusFilter, minusFilter,
                    Arrays.asList(IDENTIFIER_FIELD, 
                            SIGLA_FIELD, 
                            MARC_911_U, 
                            MARC_956_U, 
                            MARC_856_U, 
                            GRANULARITY_FIELD,
                            MarcRecordFields.HISTORIE_GRANULOVANEHOSTAVU_FIELD,
                            MarcRecordFields.HISTORIE_STAVU_FIELD,
                            
                            MarcRecordFields.DNTSTAV_FIELD,
                            MarcRecordFields.LICENSE_FIELD,
                            MarcRecordFields.FMT_FIELD,
                            "controlfield_008",
                            MarcRecordFields.RAW_FIELD,
                            MarcRecordFields.FMT_FIELD

                    ), (rsp) -> {

                        counter.incrementAndGet();
                        
                        SolrDocChange sDocChange = new SolrDocChange(rsp, solrClient, logger);
                        
                        JSONArray granJSONArray = sDocChange.getGranularity();
                        List<JSONObject> granularityJSONS = new ArrayList<>();
                        granJSONArray.forEach(it-> { granularityJSONS.add((JSONObject) it);});

                        final Object masterIdentifier = sDocChange.getIdentifier();
                        
                        boolean changedGranularity = sDocChange.processOneDoc( granularityJSONS, masterIdentifier);
                        if (changedGranularity) {
                            sDocChange.atomicChange(granularityJSONS, masterIdentifier, null);
                            this.changedIdentifiers.add(masterIdentifier.toString());
                        }

                }, IDENTIFIER_FIELD);
        }
        
        try (final SolrClient solrClient = buildClient()) {
            SolrJUtilities.quietCommit(solrClient, DataCollections.catalog.name());
        }
        logger.info("Changed states finished. Updated identifiers ("+this.changedIdentifiers.size()+") "+this.changedIdentifiers);
    }



    
    public static void main(String[] args) throws IOException {
        GranularitySetStateServiceImpl service = new GranularitySetStateServiceImpl(null);
        service.setStates(Arrays.asList("identifier:\"oai:aleph-nkp.cz:SKC01-002114585\""));
    }


}

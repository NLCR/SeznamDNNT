package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.services.PXYearService;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.services.utils.ChangeProcessStatesUtility;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.SolrJUtilities;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.*;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;

/**
 * Sluzba, ktera umoznuje vyrazeni dila na zaklade datumu
 * @author happy
 *
 */
public class PXYearServiceImpl extends AbstractPXService implements PXYearService  {

    private String format;

    private Logger logger = Logger.getLogger(PXYearService.class.getName());
    
    public PXYearServiceImpl(String loggerName, JSONObject iteration, JSONObject results) {
        if (loggerName != null) {
            this.logger = Logger.getLogger(PXYearService.class.getName()+"."+loggerName);
        }
        if (iteration != null) {
            super.iterationConfig(iteration);
            this.format =  iteration.optString(FMT_FIELD);
        }
        if (results != null) {
            super.requestsConfig(results);
        }
    }

    @Override
    public List<String> check() {
        List<String> foundCandidates = new ArrayList<>();
        CatalogIterationSupport support = new CatalogIterationSupport();
        Map<String, String> reqMap = new HashMap<>();
        reqMap.put("rows", "" + LIMIT);

        List<String> plusFilter = new ArrayList<>();

        if (this.format != null && !this.format.trim().equals("")) {
            plusFilter.add(FMT_FIELD+":"+this.format);
        }

        if (this.yearConfiguration != null && !this.yearConfiguration.trim().equals("")) {
            plusFilter.add(yearFilter());
        }

        if (!this.states.isEmpty()) {
            String collected = states.stream().collect(Collectors.joining(" OR "));
            plusFilter.add(DNTSTAV_FIELD + ":(" + collected + ")");
        }
        
        logger.info("Current iteration filter " + plusFilter);
 
        try (SolrClient solrClient = buildClient()){
            support.iterate(solrClient, reqMap, null, plusFilter, Arrays.asList(KURATORSTAV_FIELD + ":X", KURATORSTAV_FIELD + ":PX"), Arrays.asList(
                    IDENTIFIER_FIELD,
                    SIGLA_FIELD,
                    MARC_911_U,
                    MARC_956_U,
                    GRANULARITY_FIELD
            ), (rsp) -> {

                Object identifier = rsp.getFieldValue("identifier");
                foundCandidates.add(identifier.toString());
            }, IDENTIFIER_FIELD);
        } catch (Exception e) {
            this.logger.log(Level.SEVERE,e.getMessage(),e);
        }
        
        return foundCandidates;
    }

    @Override
    public void update(List<String> identifiers) throws AccountException, IOException, ConflictException, SolrServerException {

        if (!identifiers.isEmpty()) {
            this.logger.info("Updating identifiers :"+identifiers);
            CuratorItemState cState = null;
            PublicItemState pState = null;
            if (this.destinationState != null) {
                cState = CuratorItemState.valueOf(this.destinationState);
                pState = cState.getPublicItemState(null);
            }

            try (final SolrClient solr = buildClient()) {
                for (String identifier : identifiers) {
                    if (cState != null) {
                        SolrInputDocument sDoc = ChangeProcessStatesUtility.changeProcessState(solr, identifier, cState.name(), "scheduler/yearscheck");
                        solr.add(DataCollections.catalog.name(), sDoc);
                    }
                }
                SolrJUtilities.quietCommit(solr, DataCollections.catalog.name());
            }
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

}

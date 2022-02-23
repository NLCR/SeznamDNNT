package cz.inovatika.sdnnt.services.impl;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.services.PXYearService;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
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
import java.util.stream.Collectors;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.*;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;

public class PXYearServiceImpl extends AbstractPXService implements PXYearService  {

    private String format;

    public PXYearServiceImpl(JSONObject iteration, JSONObject results) {
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

        //List<String> plusFilter = new ArrayList<>(Arrays.asList(FMT_FIELD + ":BK"));
        List<String> plusFilter = new ArrayList<>();

        if (this.format != null && !this.format.trim().equals("")) {
            plusFilter.add(FMT_FIELD+":"+this.format);
        }

        if (this.yearConfiguration != null && !this.yearConfiguration.trim().equals("")) {
            //((date1_int:[1911 TO 2008] AND -date2_int:*) OR (date1_int:[1911 TO 2008] AND date2_int:[1911 TO 2008])))
            String first = String.format("(" + YEAR_OF_PUBLICATION_1 + ":%s AND -" + YEAR_OF_PUBLICATION_2 + ":*)", this.yearConfiguration);
            String second = String.format("(" + YEAR_OF_PUBLICATION_1 + ":%s AND " + YEAR_OF_PUBLICATION_2 + ":%s)", this.yearConfiguration,this.yearConfiguration);
            String fq = String.format("(%s OR %s)", first, second);
            plusFilter.add(fq);
        }

        if (!this.states.isEmpty()) {
            String collected = states.stream().collect(Collectors.joining(" OR "));
            plusFilter.add(DNTSTAV_FIELD + ":(" + collected + ")");
        }
        LOGGER.info("Current iteration filter " + plusFilter);
        support.iterate(buildClient(), reqMap, null, plusFilter, Arrays.asList(DNTSTAV_FIELD + ":X", DNTSTAV_FIELD + ":PX"), Arrays.asList(
                IDENTIFIER_FIELD,
                SIGLA_FIELD,
                MARC_911_U,
                MARC_956_U,
                GRANULARITY_FIELD
        ), (rsp) -> {

            Object identifier = rsp.getFieldValue("identifier");
            foundCandidates.add(identifier.toString());
        }, IDENTIFIER_FIELD);

        return foundCandidates;
    }


    @Override
    public void update(List<String> identifiers) throws AccountException, IOException, ConflictException, SolrServerException {
        if (!identifiers.isEmpty()) {

            CuratorItemState cState = null;
            PublicItemState pState = null;
            if (this.destinationState != null) {
                cState = CuratorItemState.valueOf(this.destinationState);
                pState = cState.getPublicItemState(null);
            }

            try (final SolrClient solr = buildClient()) {
                for (String identifier : identifiers) {
                    SolrInputDocument idoc = new SolrInputDocument();
                    idoc.setField(IDENTIFIER_FIELD, identifier);
                    if (cState != null) {
                        atomicUpdate(idoc, cState.name(), KURATORSTAV_FIELD);
                    }
                    if (pState != null) {
                        atomicUpdate(idoc, pState.name(), DNTSTAV_FIELD);
                    }
                    solr.add(DataCollections.catalog.name(), idoc);
                }
                SolrJUtilities.quietCommit(solr, DataCollections.catalog.name());
            }
        }
    }

    protected Options getOptions() {
        return Options.getInstance();
    }

    protected SolrClient buildClient() {
        return new HttpSolrClient.Builder(getOptions().getString("solr.host")).build();
    }

}

package cz.inovatika.sdnnt.services.impl;

import com.sun.mail.imap.protocol.ID;
import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.model.CuratorItemState;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.services.UpdateAlternativeAlephLinks;
import cz.inovatika.sdnnt.services.exceptions.AccountException;
import cz.inovatika.sdnnt.services.exceptions.ConflictException;
import cz.inovatika.sdnnt.utils.QuartzUtils;
import cz.inovatika.sdnnt.utils.SolrJUtilities;
import cz.inovatika.sdnnt.utils.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CursorMarkParams;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.*;

public class UpdateAlternativeAlephLinksImpl implements UpdateAlternativeAlephLinks {

    public static final Logger LOGGER = Logger.getLogger(UpdateAlternativeAlephLinksImpl.class.getName());
    public static final int LIMIT = 1000;
    public static final int BATCH_SIZE = 100;
    public static final int CHECK_SIZE = 50;

    @Override
    public void updateLinks() {
        
        long start = System.currentTimeMillis();
        
        try(SolrClient client = buildClient()) {
            Map<String, String> reqMap = new HashMap<>();
            reqMap.put("rows", "" + LIMIT);
            CatalogIterationSupport support = new CatalogIterationSupport();

            List<Pair<String, String>> foundCandiates = new ArrayList<>();

            List<String> plusFilter = new ArrayList<>(Arrays.asList(SET_SPEC_FIELD + ":DNT-ALL", ID_CCNB_FIELD+":*"));
            LOGGER.info("Current iteration filter " + plusFilter);
            support.iterate(client, reqMap, null, plusFilter, new ArrayList<>(), Arrays.asList(
                    IDENTIFIER_FIELD,
                    ID_CCNB_FIELD
            ), (rsp) -> {

                Object identifier = rsp.getFieldValue(IDENTIFIER_FIELD);
                Object ccnb = rsp.getFirstValue(ID_CCNB_FIELD);
                Pair<String, String> of = Pair.of(identifier.toString(), ccnb.toString());
                foundCandiates.add(of);

            }, IDENTIFIER_FIELD);

            Map<String, List<String>> ccnbMapping = new HashMap<>();
            foundCandiates.stream().forEach(p-> {
                List<String> list = new ArrayList<>();
                list.add(p.getLeft());
                ccnbMapping.put(p.getRight(),list);
            });

            Map<String,String> linksMapping = new HashMap<>();

            List<String> ccnbs = foundCandiates.stream().map(Pair::getRight).collect(Collectors.toList());
            LOGGER.info(String.format("Size foundCandidates %d, mapping %d, ccnsbs %d", foundCandiates.size(), ccnbMapping.size(), ccnbs.size()));

            int max = foundCandiates.size();
            int numberOfChecks = (max / CHECK_SIZE) + (max % CHECK_SIZE == 0 ? 0 : 1);

            LOGGER.info("Number of checks is "+numberOfChecks);
            for (int i = 0; i < numberOfChecks; i++) {
                int startList = i*CHECK_SIZE;
                int endList = Math.min(startList + CHECK_SIZE,max);
                String query = ccnbs.subList(startList, endList).stream().map(it -> ID_CCNB_FIELD+":\"" + it + "\"").collect(Collectors.joining(" OR "));

                try {
                    SolrQuery q = (new SolrQuery("*")).setRows(CHECK_SIZE);

                    q.addFilterQuery(SET_SPEC_FIELD+":SKC");
                    q.addFilterQuery("("+query+")");
                    q.setFields(IDENTIFIER_FIELD,ID_CCNB_FIELD, "marc_998a");
                    QueryResponse rsp = client.query(DataCollections.catalog.name(),q);
                    for (SolrDocument resultDoc: rsp.getResults()) {
                        Object identifier = resultDoc.getFieldValue(IDENTIFIER_FIELD);
                        Object ccnbField = resultDoc.getFirstValue(ID_CCNB_FIELD);
                        Object link = resultDoc.getFirstValue("marc_998a");
                        if (ccnbField != null) {
                            if (ccnbMapping.containsKey(ccnbField.toString())) {
                                String dntIdentifier = ccnbMapping.get(ccnbField.toString()).get(0);
                                ccnbMapping.get(ccnbField.toString()).add(identifier.toString());
                                linksMapping.put(dntIdentifier, link.toString());
                            }
                        } else {
                            LOGGER.warning(String.format("CCNB not found %s", ccnbField));
                        }
                    }
                    //
                    List<String> identifiers = new ArrayList<>();
                    ccnbs.subList(startList, endList).stream().forEach(ccnb-> {
                        List<String> idfs = ccnbMapping.get(ccnb);
                        if (idfs != null && !idfs.isEmpty()) {
                            identifiers.add(idfs.get(0));
                        }
                    });
                    this.update(client, identifiers, linksMapping);
                } catch (SolrServerException | IOException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(),e);
                }
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(),e);
        } finally {
            QuartzUtils.printDuration(LOGGER, start);
        }
        
    }

    public void update(SolrClient solr, List<String> identifiers, Map<String,String> mapping) throws  IOException,  SolrServerException {
        if (!identifiers.isEmpty()) {
            for (String identifier : identifiers) {
                if (mapping.containsKey(identifier)) {
                    SolrInputDocument idoc = new SolrInputDocument();
                    idoc.setField(IDENTIFIER_FIELD, identifier);
                    atomicUpdate(idoc, mapping.get(identifier), ALTERNATIVE_ALEPH_LINK);
                    solr.add(DataCollections.catalog.name(), idoc);
                }
            }
            LOGGER.info("Updating identifier "+identifiers);
            SolrJUtilities.quietCommit(solr, DataCollections.catalog.name());
        }
    }

    protected void atomicUpdate(SolrInputDocument idoc, Object fValue, String fName) {
        Map<String, Object> modifier = new HashMap<>(1);
        modifier.put("set", fValue);
        idoc.addField(fName, modifier);
    }


    protected Options getOptions() {
        return Options.getInstance();
    }

    protected SolrClient buildClient() {
        return new HttpSolrClient.Builder(getOptions().getString("solr.host")).build();
    }



}

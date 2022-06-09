package cz.inovatika.sdnnt.services.impl;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.DNTSTAV_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;
import static cz.inovatika.sdnnt.utils.MarcRecordFields.KURATORSTAV_FIELD;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.CatalogIterationSupport;
import cz.inovatika.sdnnt.index.OAICheckSKC;
import cz.inovatika.sdnnt.index.exceptions.MaximumIterationExceedException;
import cz.inovatika.sdnnt.model.workflow.duplicate.Case;
import cz.inovatika.sdnnt.services.SKCDeleteService;
import cz.inovatika.sdnnt.services.SKCTypeService;
import cz.inovatika.sdnnt.utils.MarcRecordFields;

public class SKCTypeServiceImpl extends AbstractCheckDeleteService implements SKCTypeService {

    protected Logger logger = Logger.getLogger(SKCTypeServiceImpl.class.getName());

    public SKCTypeServiceImpl(String loggerPostfix, JSONObject results) {
        super(loggerPostfix, results);
        if (loggerPostfix != null) {
            this.logger = Logger.getLogger(SKCTypeServiceImpl.class.getName()+"."+loggerPostfix);
        }
    }
    
    @Override
    protected Map<Case, List<Pair<String, List<String>>>> checkUpdate() throws IOException, SolrServerException {
        Map<Case, List<Pair<String, List<String>>>> retvals = new HashMap<>();
        try {
            OAICheckSKC check = buildCheckOAISKC();
            Pair<List<String>,List<String>> ids = check.iterate();
            retvals.put(Case.SKC_4, new ArrayList<>());
            ids.getValue().stream().forEach(deleted-> {
                retvals.get(Case.SKC_4).add(Pair.of(deleted, new ArrayList<>()));
            });
            List<String> active = ids.getLeft();
            CatalogIterationSupport support = new CatalogIterationSupport();
            Map<String, String> reqMap = new HashMap<>();
            reqMap.put("rows", "1000" );

            try (final SolrClient solrClient = buildClient()) {
                List<String> plusFilter = Arrays.asList(DNTSTAV_FIELD + ":*");
                List<String> minusFilter = Arrays.asList(KURATORSTAV_FIELD + ":D", KURATORSTAV_FIELD + ":DX");
                support.iterate(solrClient, reqMap, null, plusFilter, minusFilter, Arrays.asList(MarcRecordFields.IDENTIFIER_FIELD), (rsp) -> {
                    Object identifier = rsp.getFieldValue("identifier");
                    if (!active.contains(identifier.toString())) {
                        retvals.get(Case.SKC_4).add(Pair.of(identifier.toString(), new ArrayList<>()));
                    }
                }, IDENTIFIER_FIELD);
            } catch(IOException e) {
                getLogger().log(Level.SEVERE, e.getMessage(), e);
            }
            return retvals;

        } catch (IOException | MaximumIterationExceedException | XMLStreamException | ParserConfigurationException | SAXException  e) {
            throw new IOException(e);
        }
    }

    private OAICheckSKC buildCheckOAISKC() {
        return new OAICheckSKC();
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }

    
    protected SolrClient buildClient() {
        return new HttpSolrClient.Builder(getOptions().getString("solr.host")).build();
    }

    
    protected Options getOptions() {
        return Options.getInstance();
    }

    @Override
    protected List<String> checkDelete() throws IOException, SolrServerException {
        return new ArrayList<>();
    }

}

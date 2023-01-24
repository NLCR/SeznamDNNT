package cz.inovatika.sdnnt.model.workflow.duplicate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.openapi.endpoints.api.StringUtil;
import cz.inovatika.sdnnt.utils.StringUtils;

public class DuplicateSKCUtils {

    private DuplicateSKCUtils() {}
    
    public static boolean alreadySetFollower(SolrClient solrClient, MarcRecord origin) throws SolrServerException, IOException {
        SolrQuery idQuery = new SolrQuery(String.format("followers:\"%s\"", origin.identifier)).setRows(100);
        SolrDocumentList results = solrClient.query(DataCollections.catalog.name(), idQuery).getResults();
        return results.getNumFound() > 0;
    }
    
    public static Pair<Case, List<String>> findSKCFollowers(SolrClient solrClient, MarcRecord origin) throws SolrServerException, IOException {
        Set<String> identifiers = new HashSet<>();
        List<Triple<String,String,String>> cartesianRetVals = new ArrayList<>();
        // find by 015 a - live ccnb
        //List<String> retvals = DuplicateUtils.findByMarcField(solrClient, origin, Pair.of("015", "a"));
        List<Triple<String,String,String>> retvals = DuplicateUtils.findByMarcField(solrClient, origin, Pair.of("015", "a"));
        retvals.stream().forEach(m-> { identifiers.add(m.getLeft()); });
        if (retvals.isEmpty()) {
 
            // aktivni - zrusene 
            List<Triple<String,String,String>> azccnnb = DuplicateUtils.findActiveCanceledCCNB(solrClient, origin);
            azccnnb.forEach(zccnb-> {
                if (!identifiers.contains(zccnb.getLeft())) {
                    retvals.add(zccnb); identifiers.add(zccnb.getLeft());
                }
            });
            // aktivni - zrusene 
            List<Triple<String,String,String>> zaccnnb = DuplicateUtils.findCanceledActiveCCNB(solrClient, origin);
            zaccnnb.forEach(azccnb-> {
                if (!identifiers.contains(azccnb.getLeft())) {
                    retvals.add(azccnb); identifiers.add(azccnb.getLeft());
                }
            });
            
            
            // zrusene - zrusene
            List<Triple<String,String,String>> zzccnb = DuplicateUtils.findByMarcField(solrClient, origin, Pair.of("015", "z"));
            zzccnb.forEach(zcb-> {
                if (!identifiers.contains(zcb.getLeft())) {
                    retvals.add(zcb); identifiers.add(zcb.getLeft());
                }
            });
            
        }
        
        
        //TODO: kontrola, zda neexistuje ccnnb
        if (retvals.isEmpty() && !origin.dataFields.containsKey("015")) {
            // nesmi byt SKC_1
            List<Triple<String,String,String>> cartesian = DuplicateUtils.findFollowersBy910ax(solrClient, origin, Pair.of("910","a"), Pair.of("910","x"));
            cartesian.forEach(c-> {
                if (!identifiers.contains(c.getLeft())) {
                    cartesianRetVals.add(c); 
                    identifiers.add(c.getLeft());
                }
            });
        }
        if (!retvals.isEmpty()) {
            if (retvals.size() == 1) {
                String dntstav = retvals.get(0).getMiddle();
                String license = retvals.get(0).getRight();
                if (dntstav == null && license == null) {
                    return Pair.of(Case.SKC_1, Arrays.asList(retvals.get(0).getLeft()));
                    //return Arrays.asList(Pair.of(retvals.get(0).getLeft(), Case.SKC_1));
                } else {
                    if (matchLicenseAndState(origin, dntstav, license)) {
                        return Pair.of(Case.SKC_2a, Arrays.asList(retvals.get(0).getLeft()));
                    } else {
                        return Pair.of(Case.SKC_2b, Arrays.asList(retvals.get(0).getLeft()));
                    }
                }
            } else if (retvals.size() > 1){
                List<String> moreidents = retvals.stream().map(Triple::getLeft).collect(Collectors.toList());
                return Pair.of(Case.SKC_3, moreidents);
            } else  {
                if (checkIfRecordIsValid(origin)) {
                    return Pair.of(Case.SKC_4b, new ArrayList<>());
                } else {
                    return Pair.of(Case.SKC_4a, new ArrayList<>());
                }
            }

        // rozhodnuto na zaklade parovani
        } else if (!cartesianRetVals.isEmpty()) {
            
            String dntstav = cartesianRetVals.get(0).getMiddle();
            String license = cartesianRetVals.get(0).getRight();
            if (cartesianRetVals.size() == 1) {
                if (dntstav == null && license == null) {
                    return Pair.of(Case.SKC_1, Arrays.asList(cartesianRetVals.get(0).getLeft()));
                } else {
                    if (matchLicenseAndState(origin, dntstav, license)) {
                        return Pair.of(Case.SKC_2a, Arrays.asList(cartesianRetVals.get(0).getLeft()));
                    } else {
                        return Pair.of(Case.SKC_2b, Arrays.asList(cartesianRetVals.get(0).getLeft()));
                    }
                }
            } else if (cartesianRetVals.size() > 1){
                List<String> moreidents = cartesianRetVals.stream().map(Triple::getLeft).collect(Collectors.toList());
                return Pair.of(Case.SKC_3, moreidents);
            } else  {
                return Pair.of(Case.SKC_4b, new ArrayList<>());
            }
        } else {
            if (checkIfRecordIsValid(origin)) {
                return Pair.of(Case.SKC_4b, new ArrayList<>());
            } else {
                return Pair.of(Case.SKC_4a, new ArrayList<>());
            }
        }
        
    }

    public static final Logger LOGGER = Logger.getLogger(DuplicateSKCUtils.class.getName());
    
    private static boolean checkIfRecordIsValid(MarcRecord origin) {
      if (origin.fmt != null && Arrays.asList("BK", "SE").contains(origin.fmt.trim().toUpperCase())) {
          Object fieldValue = origin.toSolrDoc().getFieldValue("place_of_pub");
          if (fieldValue != null && fieldValue.toString().trim().toLowerCase().equals("xr")) {
              LOGGER.log(Level.INFO, String.format("Valid record %s. Format %s, place_of_pub %s", origin.identifier, origin.fmt, fieldValue.toString()));
              return true;
          }
      }
      return false;
    }

    static boolean matchLicenseAndState(MarcRecord origin, String dntstav, String license) {
        String originStav = origin.dntstav != null && origin.dntstav.size() > 0 ? origin.dntstav.get(0) : null;
        boolean matchState = StringUtils.match(originStav , dntstav);
        boolean matchLicense = StringUtils.match(origin.license, license);
        return matchState && matchLicense;
    }

}

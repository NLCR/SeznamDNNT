package cz.inovatika.sdnnt.services.impl.kramerius.granularities;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.IDENTIFIER_FIELD;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.model.DataCollections;
import cz.inovatika.sdnnt.model.PublicItemState;
import cz.inovatika.sdnnt.services.impl.kramerius.LinkitemField;
import cz.inovatika.sdnnt.services.impl.kramerius.granularities.GranularityField.TypeOfRec;
import cz.inovatika.sdnnt.services.impl.kramerius.granularities.rules.Marc911Rule;
import cz.inovatika.sdnnt.services.kraminstances.CheckKrameriusConfiguration;
import cz.inovatika.sdnnt.services.kraminstances.InstanceConfiguration;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import cz.inovatika.sdnnt.utils.SolrJUtilities;
import cz.inovatika.sdnnt.utils.StringUtils;

public class Granularity {
    
    public static final Logger LOGGER = Logger.getLogger(Granularity.class.getName());
    
    //private List<String> urls = new ArrayList<>();

    
    private String identifier;
    private PublicItemState state;

    private List<GranularityField> gfields = new ArrayList<>();
    private List<Marc911Rule> rules = new ArrayList<>();
    
    // runtime dirty flag
    private boolean dirty = false;
    
    public Granularity(String identifier) {
        super();
        this.identifier = identifier;
    }
    
    public Granularity(String identifier, PublicItemState stm) {
        super();
        this.identifier = identifier;
        this.state = stm;
    }
    

    
    public void addGranularityField(GranularityField gf) {
        this.gfields.add(gf);
    }
    
    public void removeGranularityField(LinkitemField gf) {
        this.gfields.remove(gf);
    }
    
    public List<GranularityField> getGranularityFields() {
        return  new ArrayList<>(this.gfields);
    }
    
    public String getIdentifier() {
        return identifier;
    }
    
    public LinkitemField findByRootPidAndAcronym(String pid, String acronym) {
        AtomicReference<GranularityField> found = new AtomicReference<>();
        this.gfields.stream().forEach(gf-> {
            if (gf.getAcronym() != null && gf.getAcronym().equals(acronym) && gf.getRootPid() != null && gf.getRootPid().equals(pid)) {
                found.set(gf);
            }
        });
        return found.get();
    }

    public GranularityField findByRootPid(String pid) {
        AtomicReference<GranularityField> found = new AtomicReference<>();
        this.gfields.stream().forEach(gf-> {
            if (gf.getRootPid() != null && gf.getRootPid().equals(pid)) {
                found.set(gf);
            }
        });
        return found.get();
    }

    
    public LinkitemField findByAcronyms(String acronym) {
        AtomicReference<GranularityField> found = new AtomicReference<>();
        this.gfields.stream().forEach(gf-> {
            if (gf.getAcronym() != null && gf.getAcronym().equals(acronym)) {
                found.set(gf);
            }
        });
        return found.get();
    }

    public LinkitemField findByBaseUrl(String baseUrl) {
        AtomicReference<GranularityField> found = new AtomicReference<>();
        this.gfields.stream().forEach(gf-> {
            if (gf.getBaseUrl() != null && gf.getBaseUrl().equals(baseUrl)) {
                found.set(gf);
            }
        });
        return found.get();
        
    }
    

    
    public void addTitleRule(Marc911Rule rule) {
        this.rules.add(rule);
    }

    public void removeTitleRule(Marc911Rule rule) {
        this.rules.remove(rule);
    }
    
    
    public List<Marc911Rule> getTiteRules() {
        return rules;
    }
    
    public boolean acceptByRule(GranularityField f, Logger logger) {
        String acronym = f.getAcronym();
        String rootPid = f.getRootPid();
        for (Marc911Rule rule : this.rules) {
            if (acronym != null &&  acronym.equals(rule.getDlAcronym()) && rootPid != null && rootPid.equals(rule.getPid())) {
                return rule.acceptField(f, logger);
            }
        }
        return true;
    }

    public void merge(CheckKrameriusConfiguration conf) {
        
        List<GranularityField> toDelete = new ArrayList<>();
        List<GranularityField> toMerge = new ArrayList<>();

        List<GranularityField> nfields = new ArrayList<>(gfields);
        for (GranularityField gf : nfields) {
            String baseUrl = gf.getBaseUrl();
            InstanceConfiguration inst = conf.match(baseUrl);
            if (inst != null) {
                if (inst.isShouldSkip()) {
                    this.dirty = true;
                    toDelete.add(gf);
                    this.gfields.remove(gf);
                } else {
                    toMerge.add(gf);
                    this.gfields.remove(gf);
                }
            } else {
                // no instance in configuration; the same as skip=true
                this.dirty = true;
                toDelete.add(gf);
                this.gfields.remove(gf);
            }
        }
        
        if (!toMerge.isEmpty()) {
            Map<String,GranularityField> fromSolr = new HashMap<>();
            Map<String,GranularityField> fromKram = new HashMap<>();
            
            toMerge.stream().forEach(gf-> {
                String pid = gf.getPid();
                String acronym = gf.getAcronym();
                if (gf.getTypeOfRec().equals(TypeOfRec.SDNNT_SOLR)) {
                    fromSolr.put(pid+"_"+acronym, gf);
                } else {
                    fromKram.put(pid+"_"+acronym, gf);
                    
                }
            });
            
            if (!fromKram.isEmpty()) {
                fromKram.keySet().forEach(key-> {
                    GranularityField kramField = fromKram.get(key);
                    GranularityField solrField = fromSolr.get(key);
                    GranularityField merged = mergeField(kramField, solrField);
                    if (merged != null) {
                        this.gfields.add(merged);
                    }
                });
            }
        }
    }
    
    
    
    private GranularityField mergeField(GranularityField kramField, GranularityField solrField) {
        if (kramField != null && solrField != null) {

            solrField.setFetched(kramField.getFetched());
            solrField.setDostupnost(kramField.getDostupnost());
            solrField.setDetails(kramField.getDetails());
            solrField.setModel(kramField.getModel());
            solrField.setRootPid(kramField.getRootPid());
            solrField.setPid(kramField.getPid());
            
            solrField.setLink(kramField.getLink());
            
            String solrCislo = solrField.getCislo();
            String solrDate = solrField.getDate();
            String solrRocnik = solrField.getRocnik();
            
            String kramCislo = kramField.getCislo();
            String kramDate = kramField.getDate();
            String kramRocnik = kramField.getRocnik();
            
            
            solrField.setCislo(kramField.getCislo());
            solrField.setDate(kramField.getDate());
            
            solrField.setKramLicenses(kramField.getKramLicenses());

            solrField.setPidPaths(kramField.getPidPaths());

            // zmixovane 
            
            // pokud se meni cislo nebo datum; zmenit stavy !
            boolean changeDNTStuff = false;
            boolean configurationChangeIsEnabled = false;
            if (configurationChangeIsEnabled) {

                JSONObject jsonObject = Options.getInstance().getJSONObject("granularity");
                if (jsonObject != null) {
                    configurationChangeIsEnabled = jsonObject.optBoolean("refresh.delete_state",false);
                }
                
                if (StringUtils.isAnyString(solrCislo) && StringUtils.isAnyString(kramCislo)) {
                    if (kramCislo !=null) { changeDNTStuff = !kramCislo.equals(solrCislo); }
                    else if (solrCislo != null) { changeDNTStuff = true; } 
                }
                

                if (StringUtils.isAnyString(solrDate) && StringUtils.isAnyString(kramDate)) {
                    if (kramDate !=null) { changeDNTStuff = !kramDate.equals(solrDate); }
                    else if (solrCislo != null) { changeDNTStuff = true; } 
                }
                if (StringUtils.isAnyString(solrRocnik) && StringUtils.isAnyString(kramRocnik)) {
                    if (kramRocnik !=null) { changeDNTStuff = !kramRocnik.equals(solrRocnik); }
                    else if (solrRocnik != null) { changeDNTStuff = true; } 
                }
                
                if (changeDNTStuff) {
                    solrField.setStav(null);
                    solrField.setLicense(null);
                    solrField.setKuratorStav(null);
                    
                    solrField.setCislo(kramCislo);
                    solrField.setDate(kramDate);
                    solrField.setRocnik(kramRocnik);
                }
                
            }

            return solrField;
        } else if (kramField != null && solrField == null) {
            return kramField;
        } 
        
        return null;
    }
    
    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        JSONArray array = new JSONArray();
        this.gfields.stream().map(GranularityField::toJSON).forEach(jsonObject-> {
            array.put(jsonObject);
        });
        obj.put("field", array);
        
        obj.put("identifier", this.identifier);
        obj.put("state", this.state);
        
        JSONArray rules = new JSONArray();
        this.rules.stream().map(Marc911Rule::toJSON).forEach(jsonObject-> {
            rules.put(jsonObject);
        });
        obj.put("rules", rules);
        return obj;
    }
    
    public SolrInputDocument toRomeSolrDocument() {
        SolrInputDocument idoc = new SolrInputDocument();
        idoc.setField(IDENTIFIER_FIELD, identifier);
        SolrJUtilities.atomicSetNull(idoc,  MarcRecordFields.GRANULARITY_FIELD);
        return idoc;
    }    
    
    public SolrInputDocument toSolrDocument() {
        if (!this.gfields.isEmpty()) {
            SolrInputDocument idoc = new SolrInputDocument();
            idoc.setField(IDENTIFIER_FIELD, identifier);
            List<String> collected = this.gfields.stream().map(GranularityField::toSDNNTSolrJson).map(JSONObject::toString).collect(Collectors.toList());
            SolrJUtilities.atomicSet(idoc, collected, MarcRecordFields.GRANULARITY_FIELD);
            return idoc;
        } else return null;
    }


    public void validation(CheckKrameriusConfiguration checkConf) {
        if (this.state != null && this.state == PublicItemState.N) {
            this.repairNStates(checkConf);
        } 
        
        if (this.state != null && this.state == PublicItemState.X) {
            this.repairXStates(checkConf);
        }

        if (this.state != null && this.state == PublicItemState.PA) {
            this.repairNStates(checkConf);
        }
    }

    // nekonzistentni stav N - polozky taky N
    public void repairNStates(CheckKrameriusConfiguration checkConf) {
        List<GranularityField> nfields = new ArrayList<>(gfields);
        for (GranularityField gf : nfields) {
            if (gf.getStav() != null) {
                PublicItemState state = PublicItemState.valueOf(gf.getStav());
                if (state != null && state != PublicItemState.N) {
                    gf.setStav(PublicItemState.N.name());
                    gf.setLicense(null);
                    this.dirty = true;
                }
            }
        }
    }

    public void repairXStates(CheckKrameriusConfiguration checkConf) {
        List<GranularityField> nfields = new ArrayList<>(gfields);
        for (GranularityField gf : nfields) {
            if (gf.getStav() != null) {
                PublicItemState state = PublicItemState.valueOf(gf.getStav());
                if (state != null && state != PublicItemState.X) {
                    gf.setStav(PublicItemState.X.name());
                    gf.setLicense(null);
                    this.dirty = true;
                }
            }
        }
    }
}

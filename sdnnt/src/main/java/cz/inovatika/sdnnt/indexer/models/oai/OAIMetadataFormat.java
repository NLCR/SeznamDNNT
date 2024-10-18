package cz.inovatika.sdnnt.indexer.models.oai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.indexer.models.DataField;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.indexer.models.SubField;

public enum OAIMetadataFormat {
    
    
    marc21("http://www.loc.gov/MARC21/slim",new HashMap<String, String>() {{
        this.put("marc", "http://www.loc.gov/MARC21/slim");
        this.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
    }}, "http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd") {
        @Override
        public String record(MarcRecord mr) {
            return mr.toXml(false, true);
        }
    },

    dilia("https://www.dilia.cz/", new HashMap<String, String>() {{
        //xmlns:marc="http://www.loc.gov/MARC21/slim" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.loc.gov/MARC21/slim http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd"
        this.put("marc", "http://www.loc.gov/MARC21/slim");
        this.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
    }}, null) {
        
        
        private boolean dataField(StringBuilder builder, DataField df, List<String> allowedSubfields) {
            boolean render = false;
            ArrayList<String> keys2 = new ArrayList<String>(df.subFields.keySet());
            ArrayList<SubField> subs = new ArrayList<SubField>();
            for (Object sk : keys2) {
                for (SubField sf : df.subFields.get(sk)) {
                  subs.add(sf);
                }
              }
              Collections.sort(subs, new Comparator<SubField>(){
                  @Override
                  public int compare(
                    SubField o1, SubField o2) {
                      return o1.index - o2.index;
                  }
              });
              for (SubField subField : subs) {
                  if (allowedSubfields.contains(subField.code)) {
                      render = true;
                      break;
                  }
              }

            
            if (render) {
                builder.append("<marc:datafield tag=\"" + df.tag + "\" ind1=\"" + df.ind1 + "\" ind2=\"" + df.ind2 + "\">");
                
                for (SubField sf : subs) {
                    if (allowedSubfields.contains(sf.code)) {
                        builder.append("<marc:subfield code=\"" + sf.code + "\">")
                        .append(StringEscapeUtils.escapeXml(sf.value)).append("</marc:subfield>");
                    }
                }
                builder.append("</marc:datafield>");
            }
            return render;
        }
        
        @Override
        public String record(MarcRecord mr) {
            //namespace https://www.dilia.cz/
            //mr.dataFields.get(dilia);
            StringBuilder builder = new StringBuilder();

            //xmlns:marc="http://www.loc.gov/MARC21/slim" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.loc.gov/MARC21/slim http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd"
            
            List<Pair<String, String>> attrs = new ArrayList<>();
            attrs.add(Pair.of("xmlns",getDefaultNamespace()));

            Set<String> keys = getNamespaces().keySet();
            for (String key : keys) {
                String val = getNamespaces().get(key);
                attrs.add(Pair.of("xmlns:"+key, val));
            }
            
            String attrString = attrs.stream().map(attr-> {
                return attr.getKey()+"=\""+attr.getRight()+"\"";
            }).collect(Collectors.joining(" "));
            
            builder.append(String.format("<metadata %s>", attrString));
            titles(mr, builder);
            isbn(mr,builder);
            authors(mr, builder);
            publishers(mr, builder);
            pids(mr, builder);
            history(mr, builder);
            granularityPids(mr, builder);
            
            builder.append("</metadata>");

            return builder.toString();
        }
        
        private void granularityPids(MarcRecord mr, StringBuilder builder) {
            LinkedHashSet<Pair<String,String>> apids = new LinkedHashSet<>();
            LinkedHashSet<String> npids = new LinkedHashSet<>();
            LinkedHashSet<String> xpids = new LinkedHashSet<>();
            
            LinkedHashSet<String> nostatepids = new LinkedHashSet<>();

            if (mr.granularity != null) {
                JSONArray granularityJSON = mr.granularity;
                for (int i = 0; i < granularityJSON.length(); i++) {
                    JSONObject itemObj = granularityJSON.getJSONObject(i);
                    String pid = itemObj.optString("pid");
                    if (pid != null) {
                        Object stav = itemObj.opt("stav");
                        Object lic = itemObj.opt("license");
                        if (stav != null) {
                            String state = stav.toString();
                            if (stav instanceof JSONArray) {
                                state = ((JSONArray)stav).getString(0);
                            }
                            switch(state) {
                                case "A":
                                    if (lic != null) {
                                        apids.add(Pair.of(pid, lic.toString()));
                                    } else {
                                        apids.add(Pair.of(pid, null));
                                    }
                                break;
                                case "N":
                                    npids.add(pid);
                                break;
                                case "X":
                                    xpids.add(pid);
                                break;
                                default:
                                break;
                            }
                        } else {
                            nostatepids.add(pid);
                        }
                    }
                    
                }
            }
            
            if (apids.size()>0 || npids.size() > 0 || xpids.size() >0 || nostatepids.size() > 0) {
                builder.append("<granularity>");
                
            }
            
            if (apids.size() >0) {
                //String pp = apids.stream().map(pair->{return "<pid>"+pair.getLeft()+"</pid><license>"+pair.getRight()+"</license>";}).collect(Collectors.joining("\n"));
                String pp = apids.stream().map(pair->{return "<pid>"+pair.getLeft()+"</pid>";}).collect(Collectors.joining("\n"));
                builder.append("<a>").append("<pids>").append(pp).append("</pids>").append("</a>");
            }

            if (npids.size() >0) {
                String pp = npids.stream().map(p->{return "<pid>"+p+"</pid>";}).collect(Collectors.joining("\n"));
                builder.append("<n>").append("<pids>").append(pp).append("</pids>").append("</n>");
            }

            if (xpids.size() >0) {
                String pp = xpids.stream().map(p->{return "<pid>"+p+"</pid>";}).collect(Collectors.joining("\n"));
                builder.append("<x>").append("<pids>").append(pp).append("</pids>").append("</x>");
            }
            if (nostatepids.size() >0) {
                String pp = nostatepids.stream().map(p->{return "<pid>"+p+"</pid>";}).collect(Collectors.joining("\n"));
                builder.append("<nostate>").append("<pids>").append(pp).append("</pids>").append("</nostate>");
                
            }

            if (apids.size()>0 || npids.size() > 0 || xpids.size() >0 || nostatepids.size() > 0) {
                builder.append("</granularity>");
                
            }
        }
        
        private void pids(MarcRecord mr, StringBuilder builder) {
            LinkedHashSet<String> pids = new LinkedHashSet<>();
            if (mr.masterLinksDisabled != null && !mr.masterLinksDisabled && mr.masterlinks != null) {
                for (int i = 0; i < mr.masterlinks.length(); i++) {
                    JSONObject obj = mr.masterlinks.getJSONObject(i);
                    String pid = obj.getString("pid");
                    pids.add(pid);
                }
            }
            String pp = pids.stream().map(it-> {
               return "<pid>"+it+"</pid>"; 
            }).collect(Collectors.joining("\n"));
            builder.append("<pids>").append(pp).append("</pids>");
        }
        
        private void history(MarcRecord mr, StringBuilder builder) {
            if (mr.historie_stavu != null) {
                for (int i = 0; i < mr.historie_stavu.length(); i++) {
                    builder.append("<marc:datafield tag=\"992\" ind1=\" \" ind2=\" \">");
                    JSONObject h = mr.historie_stavu.getJSONObject(i);
                    builder.append("<marc:subfield code=\"s\" >").append(h.optString("stav"))
                            .append("</marc:subfield>");
                    builder.append("<marc:subfield code=\"a\" >").append(h.optString("date"))
                            .append("</marc:subfield>");
                    builder.append("<marc:subfield code=\"b\" >").append(h.optString("user"))
                            .append("</marc:subfield>");
                    if (h.has("comment")) {
                        builder.append("<marc:subfield code=\"k\" >").append(h.optString("comment"))
                                .append("</marc:subfield>");
                    }
                    if (h.has("license")) {
                        builder.append("<marc:subfield code=\"l\" >").append(h.optString("license"))
                                .append("</marc:subfield>");
                    }
                    builder.append("</marc:datafield>");
                }

            }
        }        
        
        private void publishers(MarcRecord mr, StringBuilder builder) {
            List<DataField> marc260 = mr.dataFields.get("260");
            boolean additional = false;
            StringBuilder additionanlTitles = new StringBuilder();
            if (marc260 != null) {
                for (DataField df : marc260) {
                    if(dataField(additionanlTitles, df,Arrays.asList("b"))) {
                        additional = true;
                    }
                }
            }

            if (additional) {
                builder.append("<publishers>").append(additionanlTitles).append("</publishers>");
            }
            
        }

        private void authors(MarcRecord mr, StringBuilder builder) {
//            Info o autorech
//            * tag 100
//            * Jmeno autora
//            * subfield a
//            * Role
//            * subfield 4
//            * tag 700
//            * Jmeno autora
//            * subfield a
//            * Role
//            * subfield 4 
            List<DataField> marc100 = mr.dataFields.get("100");
            boolean additional = false;
            StringBuilder additionanlTitles = new StringBuilder();
            if (marc100 != null) {
                for (DataField df : marc100) {
                    if(dataField(additionanlTitles, df,Arrays.asList("a","4"))) {
                        additional = true;
                    }
                }
            }
            List<DataField> marc700 = mr.dataFields.get("700");
            if (marc700 != null) {
                for (DataField df : marc700) {
                    if(dataField(additionanlTitles, df,Arrays.asList("a","4"))) {
                        additional = true;
                    }
                }
            }

            if (additional) {
                builder.append("<authors>").append(additionanlTitles).append("</authors>");
            }
        }

        private void isbn(MarcRecord mr, StringBuilder builder) {
            //  * ISBN
            //    * tag 020 subfield a
            //    * tag 901 subfield b
            List<DataField> marc20 = mr.dataFields.get("020");
            boolean additional = false;
            StringBuilder additionanlTitles = new StringBuilder();
            if (marc20 != null) {
                for (DataField df : marc20) {
                    if(dataField(additionanlTitles, df,Arrays.asList("a"))) {
                        additional = true;
                    }
                }
            }
            List<DataField> marc900 = mr.dataFields.get("020");
            if (marc900 != null) {
                for (DataField df : marc900) {
                    if(dataField(additionanlTitles, df,Arrays.asList("b"))) {
                        additional = true;
                    }
                }
            }

            if (additional) {
                builder.append("<isbn>").append(additionanlTitles).append("</isbn>");
            }
        }

        
//        private void additionalTitles(MarcRecord mr, StringBuilder builder) {
//            // dalsi jmena dila
//            //   * tag 245 subfield n
//            //   * tag 245 subfield p
//            //   * tag 245 subfield b
//            List<DataField> aa245 = mr.dataFields.get("245");
//            boolean additional = false;
//            StringBuilder additionanlTitles = new StringBuilder();
//            if (aa245 != null) {
//                for (DataField df : aa245) {
//                    if(dataField(additionanlTitles, df,Arrays.asList("n","p","b"))) {
//                        additional = true;
//                    }
//                }
//            }
//            if (additional) {
//                builder.append("<additionalTitles>").append(additionanlTitles).append("</additionalTitles>");
//            }
//        }

        private void titles(MarcRecord mr, StringBuilder builder) {
            //  jmeno dila
            //   * tag 222 subfield a
            //   * tag 245 subfield a
            StringBuilder titles = new StringBuilder();
            boolean renderTitle = false;
            List<DataField> a222 = mr.dataFields.get("222");
            if (a222 != null) {
                for (DataField df : a222) {
                    if (dataField(titles, df, Arrays.asList("a"))) {
                        renderTitle = true;
                    }
                }
            }
            List<DataField> a245 = mr.dataFields.get("245");
            if (a245 != null) {
                for (DataField df : a245) {
                    if (dataField(titles, df,Arrays.asList("a","n","p","b"))) {
                        renderTitle = true;
                    }
                }
            }
            if (renderTitle) {
                builder.append("<titles>").append(titles).append("</titles>");
            }
        }
    };
    
    public static OAIMetadataFormat findFormat(String fmtName) {
        OAIMetadataFormat[] formats = OAIMetadataFormat.values();
        for (OAIMetadataFormat fmt : formats) {
            if (fmt.name().equals(fmtName)) {
                return fmt;
            }
        }
        return OAIMetadataFormat.marc21;
    }
    

    private OAIMetadataFormat(String namespace, Map<String,String> namespaces , String schema) {
        this.defaultNamespace = namespace;
        this.schema = schema;
        this.namespaces = namespaces;
    }


    private Map<String,String> namespaces;
    
    protected String defaultNamespace;
    protected String schema;
    
    public String getDefaultNamespace() {
        return defaultNamespace;
    }
    
    public Map<String, String> getNamespaces() {
        return namespaces;
    }
    
    public String getSchema() {
        return schema;
    }
    
    
    public abstract String record(MarcRecord mr);
}

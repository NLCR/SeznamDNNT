package cz.inovatika.sdnnt.index.utils.torefactor;

import cz.inovatika.sdnnt.index.ISBN;
import cz.inovatika.sdnnt.index.MD5;
import cz.inovatika.sdnnt.index.RomanNumber;
import cz.inovatika.sdnnt.indexer.models.DataField;
import cz.inovatika.sdnnt.indexer.models.MarcRecord;
import cz.inovatika.sdnnt.indexer.models.SubField;
import cz.inovatika.sdnnt.model.License;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import org.apache.commons.validator.routines.ISBNValidator;
import org.apache.solr.common.SolrDocumentBase;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static cz.inovatika.sdnnt.utils.MarcRecordFields.*;

// Methods in this class has been moved from MarcRecord and will be rewritten in future
public class MarcRecordUtilsToRefactor {

    public static void fillSolrDoc(SolrInputDocument sdoc, Map<String, List<DataField>> dataFields , List<String> tagsToIndex  ) {
      for (String tag : tagsToIndex) {
        if (dataFields.containsKey(tag)) {
          for (DataField df : dataFields.get(tag)) {
            for (String code : df.getSubFields().keySet()) {
              sdoc.addField("marc_" + tag + code, df.getSubFields().get(code).get(0).getValue());
            }
          }
        }
      }
      addStavFromMarc(sdoc, dataFields);
      addDedup(sdoc, dataFields);
      addFRBR(sdoc, dataFields);
      addEAN(sdoc, dataFields);
    }

    public static void addStavFromMarc(SolrInputDocument sdoc, Map<String, List<DataField>> dataFields) {
      DateFormat dformat = new SimpleDateFormat("yyyyMMdd");
      JSONArray hs = new JSONArray();
          if (dataFields.containsKey("992")) {
            Date datum_stavu = new Date();
            datum_stavu.setTime(0);
            for (DataField df : dataFields.get("992")) {
              JSONObject h = new JSONObject();
              String stav = df.getSubFields().get("s").get(0).getValue();
              List<String> states = df.getSubFields().get("s").stream().map(SubField::getValue).collect(Collectors.toList());
              if (df.getSubFields().containsKey("s")) {
                h.put("stav", stav);
              }
              if (df.getSubFields().containsKey("a")) {
                String ds = df.getSubFields().get("a").get(0).getValue();
                try {
                  Date d = dformat.parse(ds);
                  h.put("date", ds);
                  if (d.after(datum_stavu)) {
                    sdoc.setField("datum_stavu", d);
                    sdoc.setField(DATUM_KURATOR_STAV_FIELD, d);
                    datum_stavu = d;
                  }
                } catch (ParseException pex) {

                }

              }
              if (df.getSubFields().containsKey("b")) {
                h.put("user", df.getSubFields().get("b").get(0).getValue());
              }

              if (isANZCombination(states)) {
                h.put("license", "dnntt");
              } else if (isOnlyACombiation(states) && (!sdoc.containsKey("license") || sdoc.getFieldValue("license") == null) ){
                h.put("license", "dnnto");
              }

              // System.out.println(h);
              hs.put(h);
            }
            sdoc.setField("historie_stavu", hs.toString());
            sdoc.setField(HISTORIE_KURATORSTAVU_FIELD, hs.toString());

          }

          // String dntstav = rec.dataFields.get("990").get(0).subFields.get("a").get(0).value;
          if (dataFields.containsKey("990")) {
            for (DataField df : dataFields.get("990")) {
              //JSONObject h = new JSONObject();
              if (df.getSubFields().containsKey("a")) {
                //String stav = df.getSubFields().get("a").get(0).getValue();
                List<String> states = df.getSubFields().get("a").stream().map(SubField::getValue).collect(Collectors.toList());

                //states.forEach(oneState -> {sdoc.addField("dntstav", oneState);});
                if (isANZCombination(states)) {
                  sdoc.setField("license", "dnntt");
                  sdoc.setField("dntstav", "A");
                  sdoc.setField(KURATORSTAV_FIELD, "A");
                } else if (isOnlyACombiation(states) && (!sdoc.containsKey("license") || sdoc.getFieldValue("license") == null)) {
                  sdoc.setField("license", "dnnto");
                  sdoc.setField("dntstav", "A");
                  sdoc.setField(KURATORSTAV_FIELD, "A");

                } else {
                  states.forEach(oneState -> {sdoc.addField("dntstav", oneState);});
                  states.forEach(oneState -> {sdoc.addField(KURATORSTAV_FIELD, oneState);});
                }

                // history license
                if (states.size() == 1 && states.contains("N")) {
                  List<String> removedLicences = new ArrayList<>();
                  hs.forEach(jsonObj-> {
                    JSONObject json = (JSONObject) jsonObj;
                    if (json.has("license")) {
                      removedLicences.add(json.getString("license"));
                    }
                  });
                  if (!removedLicences.isEmpty()) {
                    removedLicences.stream().forEach(l-> {sdoc.addField("license_history", l);});
                  }
                }
              }
            }
          }

          // Zpracovani pole 956 => granularity
          // 956u: link do dk
          // 9569: stav
          // 956x: cislo
          // 956y: rocnik
          if (dataFields.containsKey("956")) {

            for (DataField df : dataFields.get("956")) {
              JSONObject h = new JSONObject();
              if (df.getSubFields().containsKey("u")) {
                h.put("link", df.getSubFields().get("u").get(0).getValue());
              }
              if (df.getSubFields().containsKey("9")) {
                for (SubField sf: df.getSubFields().get("9")) {
                  h.append("stav", sf.value);
                  h.append(KURATORSTAV_FIELD, sf.value);
                }
                List<Object> ostavy = new ArrayList<>();
                h.getJSONArray("stav").forEach(ostavy::add);
                List<String> stavy = ostavy.stream().map(Object::toString).collect(Collectors.toList());

                if (isOnlyACombiation(stavy)) {
                  h.put(LICENSE_FIELD, License.dnnto.name());
                } else if (isANZCombination(stavy)) {
                  h.put(LICENSE_FIELD, License.dnnto.name());
                }
                //if (h.getJSONArray("stav"))
              }
              if (df.getSubFields().containsKey("x")) {
                h.put("cislo", df.getSubFields().get("x").get(0).getValue());
              }
              if (df.getSubFields().containsKey("y")) {
                h.put("rocnik", df.getSubFields().get("y").get(0).getValue());
              }

              sdoc.addField("granularity", h.toString());
            }

          }
    }

    public static boolean isANZCombination(List<String> states) {
      if (states.size() == 2 ) return states.contains("NZ") && (states.contains("A") || (states.contains("PA")));
      else return states.contains("NZ");
    }

    public static boolean isOnlyACombiation(List<String> states) {
      return states.size() == 1 && (states.get(0).equals("A") || (states.get(0).equals("PA")));
    }

    public static void addEAN(SolrInputDocument sdoc, Map<String, List<DataField>> dataFields ) {
      ISBNValidator isbn = ISBNValidator.getInstance();
      if (sdoc.containsKey("marc_020a")) {
        for (Object s : sdoc.getFieldValues("marc_020a")) {
          String ean = ((String) s);
          ean = isbn.validate(ean);
          if (ean != null) {
            // ean.replaceAll("-", "")
            // ean = ISBN.convertTo13(ean);
            sdoc.addField("ean", ean);
          }
        }
      }
    }

    public static void addDedup(SolrInputDocument sdoc, Map<String, List<DataField>> dataFields) {

      try {

        //ISBN
        String pole = (String) sdoc.getFieldValue("marc_020a");
        ISBN val = new ISBN();

        if (pole != null && !pole.equals("")) {
          //pole = pole.toUpperCase().substring(0, Math.min(13, pole.length()));
          if (!"".equals(pole) && val.isValid(pole)) {
            sdoc.setField("dedup_fields", MD5.generate(new String[]{pole}));
          }
        }

        //ISSN
        pole = (String) sdoc.getFieldValue("marc_022a");
        if (pole != null && !pole.equals("")) {
          //pole = pole.toUpperCase().substring(0, Math.min(13, pole.length()));
          if (!"".equals(pole) && val.isValid(pole)) {
            sdoc.setField("dedup_fields", MD5.generate(new String[]{pole}));
          }
        }

        //ccnb
        pole = (String) sdoc.getFieldValue("marc_015a");
        //logger.log(Level.INFO, "ccnb: {0}", pole);
        if (pole != null && !"".equals(pole)) {
          sdoc.setField("dedup_fields", MD5.generate(new String[]{pole}));
        }

        //Check 245n číslo části
        String f245n = "";
        String f245nraw = (String) sdoc.getFieldValue("marc_245n");
        if (f245nraw != null) {
          RomanNumber rn = new RomanNumber(f245nraw);
          if (rn.isValid()) {
            f245n = Integer.toString(rn.toInt());
          }
        }

        //Pole 250 údaj o vydání (nechat pouze numerické znaky) (jen prvni cislice)
        String f250a = (String) sdoc.getFieldValue("marc_250a");
        if (f250a != null) {
          f250a = onlyLeadNumbers(f250a);
        }

        //Pole 100 autor – osobní jméno (ind1=1 →  prijmeni, jmeno; ind1=0 → jmeno, prijmeni.
        //Obratit v pripade ind1=1, jinak nechat)
        String f100a = (String) sdoc.getFieldValue("marc_100a");
        if (dataFields.containsKey("100") && f100a != null) {
          String ind1 = dataFields.get("100").get(0).ind1;
          if ("1".equals(ind1) && !"".equals(f100a)) {
            String[] split = f100a.split(",", 2);
            if (split.length == 2) {
              f100a = split[1] + split[0];
            }
          }
        }

        if ("".equals(f100a)) {
          f100a = (String) sdoc.getFieldValue("marc_245c");
        }

        //vyber poli
        String uniqueCode = MD5.generate(new String[]{
          (String) sdoc.getFieldValue("marc_245a"), // main title
          (String) sdoc.getFieldValue("marc_245b"), // subtitle
          //map.get("245c"),
          f245n,
          (String) sdoc.getFieldValue("marc_245p"),
          f250a,
          f100a,
          (String) sdoc.getFieldValue("marc_110a"),
          (String) sdoc.getFieldValue("marc_111a"),
          (String) sdoc.getFieldValue("marc_260a"),
          (String) sdoc.getFieldValue("marc_260b"),
          onlyLeadNumbers((String) sdoc.getFieldValue("marc_260c")),
          (String) sdoc.getFieldValue("marc_264a"),
          (String) sdoc.getFieldValue("marc_264b"),
          onlyLeadNumbers((String) sdoc.getFieldValue("marc_264c"))
        });
        sdoc.setField("dedup_fields", uniqueCode);
      } catch (Exception ex) {
        MarcRecord.LOGGER.log(Level.SEVERE, null, ex);
      }

    }

    public static void addFRBR(SolrInputDocument sdoc, Map<String, List<DataField>> dataFields ) {
      // Podle 	Thomas Butler Hickey
      // https://www.oclc.org/content/dam/research/activities/frbralgorithm/2009-08.pdf
      // https://text.nkp.cz/o-knihovne/odborne-cinnosti/zpracovani-fondu/informativni-materialy/bibliograficky-popis-elektronickych-publikaci-v-siti-knihoven-cr
      // strana 42
      // https://www.nkp.cz/soubory/ostatni/vyda_cm26.pdf

      /*
      Pole 130, 240 a 730 pro unifikovaný název a podpole názvových údajů v polích 700, 710 a
  711 umožní po doplnění formátu MARC 21 nebo zavedení nového formátu vygenerovat
  alespoň částečné údaje o díle a vyjádření.
  Nové pole 337 (společně s poli 336 a 338, která jsou součástí Minimálního záznamu)
  nahrazuje dosavadní podpole 245 $h.

      https://is.muni.cz/th/xvt2x/Studie_FRBR.pdf
      strana 100

      FRBR Tool – příklad mapování entit a polí MARC21
  Author: Dreiser, Theodore, 1871 (Field 100) [Work]
  Work: Sister Carrie (Field 240 )
  Form: text - English LDR/06 + 008/35-37) [Expression]
  Edition: 2nd ed. (Field 250) [Manifestation]

       */
      String frbr = "";

      //Ziskame title part
      String title = getFieldPart(sdoc, dataFields, "240", "adgknmpr");
      if (title.isBlank()) {
        title = getFieldPart(sdoc, dataFields,"245", "adgknmpr");
      }
      if (title.isBlank()) {
        title = getFieldPart(sdoc, dataFields,"246", "adgknmpr");
      }
      String authorPart = getAuthorPart(sdoc, dataFields,"100", "bcd") + getAuthorPart(sdoc, dataFields,"110", "bcd") + getAuthorPart(sdoc, dataFields,"111", "bcdnq");

      if (!authorPart.isBlank()) {
        frbr = authorPart + "/" + title;
      } else if (sdoc.containsKey("marc_130a")) {
        //Else if a 130 exists then the title alone is a sufficient key
        frbr = (String) sdoc.getFieldValue("marc_130a");
      } else if (sdoc.containsKey("marc_700a") && !(sdoc.containsKey("marc_700t") || sdoc.containsKey("marc_700k"))) {
        // Else if 7XX (700, 710, 711) fields exist then add the names to the title.
        // Skip 7XX fields with subfields [tk]. Use subfields [abcdq] as the name
        frbr = "/" + title + "/" + getFieldPart(sdoc, dataFields,"700", "abcdq");
      } else if (sdoc.containsKey("marc_710a") && !(sdoc.containsKey("marc_710t") || sdoc.containsKey("marc_710k"))) {
        // Else if 7XX (700, 710, 711) fields exist then add the names to the title.
        // Skip 7XX fields with subfields [tk]. Use subfields [abcdq] as the name
        frbr = "/" + title + "/" + getFieldPart(sdoc, dataFields,"710", "abcdq");
      } else if (sdoc.containsKey("marc_711a") && !(sdoc.containsKey("marc_711t") || sdoc.containsKey("marc_711k"))) {
        // Else if 7XX (700, 710, 711) fields exist then add the names to the title.
        // Skip 7XX fields with subfields [tk]. Use subfields [abcdq] as the name
        frbr = "/" + title + "/" + getFieldPart(sdoc, dataFields,"711", "abcdq");
      } else {
        // Else add the oclc number to the title to make the key unique
        frbr = "/" + title + "/" + (String) sdoc.getFieldValue("controlfield_001");
      }
      sdoc.setField("frbr", MD5.normalize(frbr));
    }

    public static String getAuthorPart(SolrInputDocument sdoc, Map<String, List<DataField>> dataFields , String tag, String codes) {
      String author = "";
      if (sdoc.containsKey("marc_" + tag + "a")) {
        // If an author exists, combine it with a title
        String ind1 = dataFields.get(tag).get(0).ind1;
        String f = dataFields.get(tag).get(0).subFields.get("a").get(0).value;
        if ("1".equals(ind1)) {
          String[] split = f.split(",", 2);
          if (split.length == 2) {
            author = split[1] + split[0];
          }
        } else {
          author = f;
        }
      }
      for (char code : codes.toCharArray()) {
        if (sdoc.containsKey("marc_" + tag + code)) {
          author += "|" + (String) sdoc.getFieldValue("marc_" + tag + code);
        }
      }
      return author;
    }

    public static String getFieldPart(SolrInputDocument sdoc, Map<String, List<DataField>> dataFields , String tag, String codes) {
      String s = "";
      if (dataFields.containsKey(tag)) {
        for (char code : codes.toCharArray()) {
          if (sdoc.containsKey("marc_" + tag + code)) {
            s += "|" + (String) sdoc.getFieldValue("marc_" + tag + code);
          }
        }
      }
      return s;
    }

    public static String onlyLeadNumbers(String s) {
      if (s == null || "".equals(s)) {
        return s;
      }
      String retVal = "";
      int n = 0;
      while (n < s.length() && Character.isDigit(s.charAt(n))) {
        retVal += s.charAt(n);
        n++;
      }
      return retVal;
    }

  // TODO: Rewrite it !! big hack
  public static void syncFromDoc(SolrDocumentBase doc, MarcRecord mr) {
    if (doc.getFieldValues(DNTSTAV_FIELD) != null) {
      mr.dntstav = new ArrayList<>((Collection)doc.getFieldValues(DNTSTAV_FIELD));
    } else {
      mr.dntstav = new ArrayList<>();
    }
    mr.datum_stavu = (Date) doc.getFieldValue(DATUM_STAVU_FIELD);
    if ( doc.getFieldValue(HISTORIE_STAVU_FIELD) != null) {
      mr.historie_stavu = new JSONArray((String) doc.getFieldValue(HISTORIE_STAVU_FIELD));
    } else {
      mr.historie_stavu = new JSONArray();
    }


    if (doc.containsKey(KURATORSTAV_FIELD)) {
      mr.kuratorstav = new ArrayList<>((Collection)doc.getFieldValues(KURATORSTAV_FIELD));
      mr.datum_krator_stavu = (Date) doc.getFieldValue(DATUM_KURATOR_STAV_FIELD);
      mr.historie_kurator_stavu =    new JSONArray((String) doc.getFieldValue(HISTORIE_KURATORSTAVU_FIELD));
    } else {
      mr.kuratorstav = new ArrayList<>(mr.dntstav);
      mr.datum_krator_stavu = mr.datum_stavu;
      if (mr.historie_stavu != null) {
        mr.historie_kurator_stavu = new JSONArray();
        for (int i = 0; i < mr.historie_stavu.length(); i++) {
          mr.historie_kurator_stavu.put(mr.historie_stavu.get(i));
        }
      }
    }


    // license
    Object fieldValue = doc.getFieldValue(LICENSE_FIELD);
    mr.license = (fieldValue instanceof  List  && !((List)fieldValue).isEmpty()) ? ((List) fieldValue).get(0).toString() : (String) fieldValue;
    mr.licenseHistory = (List<String>) doc.getFieldValues(LICENSE_HISTORY_FIELD);

    if (doc.containsKey(GRANULARITY_FIELD)) {
      Collection fieldValues = doc.getFieldValues(GRANULARITY_FIELD);
      JSONArray jsonArray = new JSONArray();
      fieldValues.stream().map(o-> {
        return new JSONObject(o.toString());
      }).forEach(jsonArray::put);
      mr.granularity = jsonArray;
    }
  }

  public static void setFMT(SolrInputDocument sdoc, String type_of_resource, String item_type) {
    // https://knowledge.exlibrisgroup.com/Primo/Product_Documentation/Primo/Technical_Guide/020Working_with_Normalization_Rules/100Validate_UNIMARC_FMT
    // Zmena POZOR. Podle url ai by mel byt BK, ale v alephu vidim SE
    String fmt = "BK";
    switch (type_of_resource) {
      case "a":
        if ("s".equals(item_type) || "i".equals(item_type)) {
          fmt = "SE";
        }
        break;
      case "c":
      case "d":
        fmt = "MU";
        break;
      case "e":
      case "f":
        fmt = "MP";
        break;
      case "g":
      case "k":
      case "r":
        fmt = "VM";
        break;
      case "i":
      case "j":
        fmt = "AM";
        break;
      case "l":
        fmt = "CF";
        break;
      case "m":
        fmt = "MX";
        break;
    }
    sdoc.setField("fmt", fmt);
  }

  public static void setIsProposable(SolrInputDocument sdoc) {

    // Pole podle misto vydani (xr ) a 338 a 245h
    boolean is_proposable = false;

    String place_of_pub = (String) sdoc.getFieldValue("place_of_pub");
    if ("xr ".equals(place_of_pub)) {
      if (sdoc.containsKey("marc_338a")) {
        String marc_338a = (String) sdoc.getFieldValue("marc_338a");
        String marc_338b = (String) sdoc.getFieldValue("marc_338b");
        String marc_3382 = (String) sdoc.getFieldValue("marc_3382");
        is_proposable = "svazek".equals(marc_338a) && "nc".equals(marc_338b) && "rdacarrier".equals(marc_3382);
      } else {
        is_proposable = !sdoc.containsKey("marc_245h");
      }
    }
    sdoc.setField("is_proposable", is_proposable);
  }

  public static void addRokVydani(SolrInputDocument sdoc) {
    if (sdoc.containsKey("marc_260c")) {
      for (Object s : sdoc.getFieldValues("marc_260c")) {
        String val = (String) s;
        val = val.replaceAll("\\[", "").replaceAll("\\]", "").trim();
        try {
          // je to integer. Pridame
          int r = Math.abs(Integer.parseInt(val));
          //Nechame jen 4
          if ((r + "").length() > 3) {
            String v = (r + "").substring(0, 4);
            sdoc.addField("rokvydani", v);
          }

          return;
        } catch (NumberFormatException ex) {

        }
        // casto maji 'c' nebo 'p' na zacatku c2001
        if (val.startsWith("c") || val.startsWith("p")) {
          val = val.substring(1);
          try {
            // je to integer. Pridame
            int r = Integer.parseInt(val);
            sdoc.addField("rokvydani", r);
            return;
          } catch (NumberFormatException ex) {

          }
        }
        // [před r. 1937]
        if (val.startsWith("před r.")) {
          val = val.substring(7).trim();
          try {
            // je to integer. Pridame
            int r = Integer.parseInt(val);
            sdoc.addField("rokvydani", r);
            return;
          } catch (NumberFormatException ex) {

          }
        }

      }
    }
  }
}

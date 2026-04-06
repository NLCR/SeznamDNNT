package cz.inovatika.sdnnt.services.compare;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.common.SolrDocument;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class RecordFingerprint {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(RecordFingerprint.class);
    private final String rawBasicFields;

    private final String basicFieldsHash;
    private final Set<Pair<String,String>> granularity;
    private final Set<Pair<String,String>> masterlinks;
    private final Set<String> identifiers;

    public RecordFingerprint(String rawBasicFields, String basicFieldsHash,
                             Set<Pair<String,String>> granularity,
                             Set<Pair<String,String>> masterlinks,
                             Set<String> identifiers) {
        this.rawBasicFields = rawBasicFields;
        this.basicFieldsHash = basicFieldsHash;
        this.granularity = granularity != null ? Collections.unmodifiableSet(granularity) : Collections.emptySet();
        this.masterlinks = masterlinks != null ? Collections.unmodifiableSet(masterlinks) : Collections.emptySet();
        this.identifiers = identifiers != null ? Collections.unmodifiableSet(identifiers) : Collections.emptySet();
    }

    public String getRawBasicFields() { return rawBasicFields; }

    public String getBasicFieldsHash() { return basicFieldsHash; }
    public Set<Pair<String,String>> getGranularity() { return granularity; }
    public Set<Pair<String,String>> getMasterlinks() { return masterlinks; }
    public Set<String> getIdentifiers() { return identifiers; }


    public String getDiffReportHtml(RecordFingerprint oldFp) {
        if (oldFp == null) {
            return "<p class='new-record'><strong>NOVÝ ZÁZNAM</strong> (v prvním Solru neexistoval)</p>";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<table class='diff-table'>");
        sb.append("<thead><tr><th>Pole</th><th>Původní hodnota</th><th>Nová hodnota</th></tr></thead>");
        sb.append("<tbody>");

        if (!Objects.equals(this.basicFieldsHash, oldFp.basicFieldsHash)) {
            String[] oldParts = oldFp.rawBasicFields.split("\\|", -1);
            String[] newParts = this.rawBasicFields.split("\\|", -1);
            String[] labels = {"dntstav", "kuratorstav", "id_ccnb", "followers",  "ean"};

            for (int i = 0; i < Math.min(oldParts.length, newParts.length); i++) {
                if (!Objects.equals(oldParts[i], newParts[i])) {
                    addTableRow(sb, "[ZÁKLADNÍ] " + labels[i], oldParts[i], newParts[i]);
                }
            }
        }

        renderSetDiff(sb, "Granularity", oldFp.granularity, this.granularity);
        renderSetDiff(sb, "Masterlinks", oldFp.masterlinks, this.masterlinks);
        sb.append("</tbody></table>");

        return sb.length() > 100 ? sb.toString() : "<p>Žádné změny v polích.</p>";
    }

    private void addTableRow(StringBuilder sb, String label, String oldVal, String newVal) {
        sb.append("<tr>");
        sb.append("<td>").append(label).append("</td>");
        sb.append("<td class='old-val'>").append(oldVal == null ? "<i>null</i>" : oldVal).append("</td>");
        sb.append("<td class='new-val'>").append(newVal == null ? "<i>null</i>" : newVal).append("</td>");
        sb.append("</tr>");
    }

    private void renderSetDiff(StringBuilder sb, String category, Set<Pair<String,String>> oldSet, Set<Pair<String,String>> newSet) {
        if (Objects.equals(oldSet, newSet)) return;
        for (Pair<String, String> p : oldSet) {
            if (!newSet.contains(p)) {
                addTableRow(sb, category, p.toString(), "--- CHYBÍ ---");
            }
        }
        for (Pair<String, String> p : newSet) {
            if (!oldSet.contains(p)) {
                addTableRow(sb, category, "--- NOVÉ ---", p.toString());
            }
        }
    }
    public String getDiffReport(RecordFingerprint oldFp) {
        if (oldFp == null) return "NOVÝ ZÁZNAM (v prvním Solru neexistoval)";
        StringBuilder sb = new StringBuilder();

        if (!Objects.equals(this.basicFieldsHash, oldFp.basicFieldsHash)) {
            String[] oldParts = oldFp.rawBasicFields.split("\\|", -1);
            String[] newParts = this.rawBasicFields.split("\\|", -1);

            sb.append("  [ZÁKLADNÍ POLE]:\n");
            if (!Objects.equals(oldParts[0], newParts[0]))
                sb.append(String.format("    - dntstav: %s -> %s\n", oldParts[0], newParts[0]));
            if (!Objects.equals(oldParts[1], newParts[1]))
                sb.append(String.format("    - kuratorstav: %s -> %s\n", oldParts[1], newParts[1]));
            if (!Objects.equals(oldParts[2], newParts[2]))
                sb.append(String.format("    - id_ccnb: %s -> %s\n", oldParts[2], newParts[2]));
        }

        if (!Objects.equals(this.granularity, oldFp.granularity)) {

            Set<Pair<String,String>> diff = diff(oldFp.granularity, granularity);
            sb.append(String.format("  [GRANULARITA]: Změna v datech (Pocet rozdílů %d,  Rozdily: %s)\n", diff.size(),diff));
        }

        if (!Objects.equals(this.masterlinks, oldFp.masterlinks)) {

            Set<Pair<String,String>> diff = diff(oldFp.masterlinks, masterlinks);
            if (diff != null && diff.size() == 0) {
                boolean b = Objects.equals(this.masterlinks, oldFp.masterlinks);
                System.out.println(b);
            }
            sb.append(String.format("  [MASTERLINKS]: Změna v odkazech (Pocet rozdílů %d, rozdily: %s)\n", diff.size(), diff));
        }

        if (!Objects.equals(this.identifiers, oldFp.identifiers)) {
            sb.append(String.format("  [IDENTIFIKÁTORY]: Původní: %s, Nové: %s\n", oldFp.identifiers, this.identifiers));
        }

        return sb.toString();
    }



    private Set<Pair<String, String>> diff(Set<Pair<String,String>> set1, Set<Pair<String,String>> set2) {
        Set<Pair<String,String>> bigger;
        Set<Pair<String,String>> smaller;
        if (set1.size() > set2.size()) {
            bigger = set1;
            smaller = set2;
        } else {
            bigger = set2;
            smaller = set1;
        }
        Set<Pair<String,String>> base = new HashSet<>(bigger);
        Set<Pair<String,String>> rest = new HashSet<>(smaller);
        base.removeAll(rest);
        return base;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecordFingerprint that = (RecordFingerprint) o;
        return Objects.equals(basicFieldsHash, that.basicFieldsHash) &&
                Objects.equals(granularity, that.granularity) &&
                Objects.equals(masterlinks, that.masterlinks) &&
                Objects.equals(identifiers, that.identifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(basicFieldsHash, granularity, masterlinks, identifiers);
    }

    @Override
    public String toString() {
        return "RecordFingerprint{" +
                "basicFieldsHash='" + basicFieldsHash + '\'' +
                ", granularity=" + granularity +
                ", masterlinks=" + masterlinks +
                ", identifiers=" + identifiers +
                '}';
    }

    public static RecordFingerprint loadFromSolrDocument(Logger logger, SolrDocument doc, Set<String> ignoredGranularity, Set<String> ignoredMasterlinks) {
        String basicFieldsRaw = String.format("%s|%s|%s|%s|%s",
                doc.getFieldValue(MarcRecordFields.DNTSTAV_FIELD),
                doc.getFieldValue(MarcRecordFields.KURATORSTAV_FIELD),
                doc.getFieldValue(MarcRecordFields.ID_CCNB_FIELD),
                doc.getFieldValue(MarcRecordFields.FOLLOWERS),
                doc.getFieldValue(MarcRecordFields.EAN_FIELD)
            );

        String basicHash = DigestUtils.sha1Hex(basicFieldsRaw);
        Set<Pair<String,String>> granularityHashes = parseAndHashJsonField(logger,
                doc.getFieldValues("granularity"),
                ignoredGranularity
        );
        Set<Pair<String,String>> masterlinksHashes = parseAndHashJsonField(logger,
                doc.getFieldValues("masterlinks"),
                ignoredMasterlinks
        );
        Collection<Object> idValues = doc.getFieldValues("id_all_identifiers");
        Set<String> identifiers = (idValues == null) ? Collections.emptySet() :
                idValues.stream()
                        .map(Object::toString)
                        .collect(Collectors.toSet());
        return new RecordFingerprint(basicFieldsRaw, basicHash, granularityHashes, masterlinksHashes, identifiers);
    }

    private static Set<Pair<String, String>> parseAndHashJsonField(Logger logger, Collection<Object> jsonStrings, Set<String> keysToIgnore) {
        if (jsonStrings == null || jsonStrings.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Pair<String, String>> hashes = new HashSet<>();

        for (Object obj : jsonStrings) {
            try {
                JSONObject json = new JSONObject(obj.toString());
                for (String key : keysToIgnore) {
                    logger.fine(String.format("\tIgnoring key: %s", key));
                    json.remove(key);
                }
                String stableString = getStableString(json);
                hashes.add(Pair.of(stableString, DigestUtils.md5Hex(stableString)));
            } catch (Exception e) {
                logger.warning(e.getMessage());
            }
        }
        return hashes;
    }

    private static String getStableString(JSONObject json) {
        List<String> keys = new ArrayList<>(json.keySet());
        Collections.sort(keys);

        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            Object value = json.get(key);

            sb.append("\"").append(key).append("\":");
            if (value instanceof JSONArray) {
                List<String> list = new ArrayList<>();
                JSONArray arr = (JSONArray) value;
                for (int j = 0; j < arr.length(); j++) {
                    list.add(arr.get(j).toString());
                }
                Collections.sort(list);
                sb.append(list.toString());
            } else {
                sb.append("\"").append(value.toString()).append("\"");
            }
            if (i < keys.size() - 1) sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }

}
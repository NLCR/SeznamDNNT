package cz.inovatika.sdnnt.services.compare;
import cz.inovatika.sdnnt.utils.MarcRecordFields;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.common.SolrDocument;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

public final class RecordFingerprint {

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

    private static final Set<String> IGNORED_GRANULARITY = Set.of("datestamp", "indextime","fetched", "link", "baseUrl");
    private static final Set<String> IGNORED_MASTERLINKS = Set.of("fetched","link","baseUrl");

    public static RecordFingerprint loadFromSolrDocument(SolrDocument doc, Set<String> ignoredGranularity, Set<String> ignoredMasterlinks) {
        String basicFieldsRaw = String.format("%s|%s|%s",
                doc.getFieldValue(MarcRecordFields.DNTSTAV_FIELD),
                doc.getFieldValue(MarcRecordFields.KURATORSTAV_FIELD),
                doc.getFieldValue(MarcRecordFields.ID_CCNB_FIELD)
        );

        String basicHash = DigestUtils.sha1Hex(basicFieldsRaw);
        Set<Pair<String,String>> granularityHashes = parseAndHashJsonField(
                doc.getFieldValues("granularity"),
                ignoredGranularity != null && !ignoredGranularity.isEmpty() ? ignoredGranularity :  IGNORED_GRANULARITY
        );
        Set<Pair<String,String>> masterlinksHashes = parseAndHashJsonField(
                doc.getFieldValues("masterlinks"),
                ignoredMasterlinks != null && !ignoredMasterlinks.isEmpty() ? ignoredMasterlinks :  IGNORED_MASTERLINKS
        );
        Collection<Object> idValues = doc.getFieldValues("id_all_identifiers");
        Set<String> identifiers = (idValues == null) ? Collections.emptySet() :
                idValues.stream()
                        .map(Object::toString)
                        .collect(Collectors.toSet());
        return new RecordFingerprint(basicFieldsRaw, basicHash, granularityHashes, masterlinksHashes, identifiers);
    }

    private static Set<Pair<String, String>> parseAndHashJsonField(Collection<Object> jsonStrings, Set<String> keysToIgnore) {
        if (jsonStrings == null || jsonStrings.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Pair<String, String>> hashes = new HashSet<>();

        for (Object obj : jsonStrings) {
            try {
                JSONObject json = new JSONObject(obj.toString());
                for (String key : keysToIgnore) {
                    json.remove(key);
                }
                String stableString = getStableString(json);
                hashes.add(Pair.of(stableString, DigestUtils.md5Hex(stableString)));
            } catch (Exception e) {
                System.err.println("Chyba při zpracování JSON: " + e.getMessage());
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
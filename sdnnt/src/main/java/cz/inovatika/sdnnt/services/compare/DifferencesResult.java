package cz.inovatika.sdnnt.services.compare;

import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

public class DifferencesResult {

    private Map<String, Pair<RecordFingerprint, RecordFingerprint>> differences = new HashMap<>();
    private Map<String, RecordFingerprint> missing = new HashMap<>();

    public DifferencesResult(Map<String, Pair<RecordFingerprint, RecordFingerprint>> differences, Map<String, RecordFingerprint> missing) {
        this.differences = differences;
        this.missing = missing;
    }

    public Map<String, Pair<RecordFingerprint, RecordFingerprint>> getDifferences() {
        return differences;
    }

    public Map<String, RecordFingerprint> getMissing() {
        return missing;
    }
}

package cz.inovatika.sdnnt.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class GeneratePozIstUpdateBatchesTest {

    @Test
    public void appliesSinglePoznamkaOnlyToNzAndPnHistoryStates() throws Exception {
        String note = "Omezeni pristupu na zadost nakladatele";
        List<?> notes = historyNotes(
                new JSONArray().put(field("991", "a", note)),
                new JSONArray()
                        .put(ist("PA", "batch"))
                        .put(ist("A", "batch"))
                        .put(ist("NZ", "batch"))
                        .put(ist("PN", "Palmknihy"))
                        .put(ist("A", "batch")));

        Object update = updateHistory(new JSONArray()
                .put(history("PA", "batch"))
                .put(history("A", "batch"))
                .put(history("NZ", "batch"))
                .put(history("PN", "Palmknihy"))
                .put(history("A", "batch"))
                .toString(), notes, "historie_stavu");

        assertTrue(booleanField(update, "updated"));
        JSONArray updated = jsonArrayField(update, "historieStavu");
        assertFalse(updated.getJSONObject(0).has("comment"));
        assertFalse(updated.getJSONObject(1).has("comment"));
        assertEquals(note, updated.getJSONObject(2).getString("comment"));
        assertEquals(note, updated.getJSONObject(3).getString("comment"));
        assertFalse(updated.getJSONObject(4).has("comment"));
    }

    private static List<?> historyNotes(JSONArray pozFields, JSONArray istFields) throws Exception {
        Method method = GeneratePozIstUpdateBatches.class.getDeclaredMethod("historyNotes", JSONArray.class, JSONArray.class);
        method.setAccessible(true);
        return (List<?>) method.invoke(null, pozFields, istFields);
    }

    private static Object updateHistory(String history, List<?> notes, String fieldName) throws Exception {
        Method method = GeneratePozIstUpdateBatches.class.getDeclaredMethod("updateHistory", String.class, List.class, String.class);
        method.setAccessible(true);
        return method.invoke(null, history, notes, fieldName);
    }

    private static boolean booleanField(Object object, String fieldName) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getBoolean(object);
    }

    private static JSONArray jsonArrayField(Object object, String fieldName) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (JSONArray) field.get(object);
    }

    private static JSONObject field(String tag, String code, String value) {
        return new JSONObject()
                .put("tag", tag)
                .put("subFields", new JSONObject()
                        .put(code, new JSONArray().put(new JSONObject()
                                .put("code", code)
                                .put("value", value)
                                .put("index", 0))));
    }

    private static JSONObject ist(String state, String user) {
        return new JSONObject()
                .put("tag", "992")
                .put("subFields", new JSONObject()
                        .put("s", new JSONArray().put(new JSONObject()
                                .put("code", "s")
                                .put("value", state)
                                .put("index", 0)))
                        .put("b", new JSONArray().put(new JSONObject()
                                .put("code", "b")
                                .put("value", user)
                                .put("index", 2))));
    }

    private static JSONObject history(String state, String user) {
        return new JSONObject()
                .put("stav", state)
                .put("date", "20210101")
                .put("user", user);
    }
}

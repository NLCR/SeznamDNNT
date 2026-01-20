package cz.inovatika.sdnnt.index.imports;

import io.netty.util.internal.StringUtil;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class XMLReadSupport {

    private InputStream inputStream;
    private String itemName; //TODO: Delete
    private  LinkedHashSet<String> itemsToSkip;

    public XMLReadSupport(InputStream inputStream, String itemName, LinkedHashSet<String> itemsToSkip) {
        this.inputStream = inputStream;
        this.itemName = itemName;
        this.itemsToSkip = itemsToSkip;
    }

    public void parse(
            Consumer<Map<String, String>> itemProcessor
    ) throws XMLStreamException {

        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        XMLStreamReader reader = factory.createXMLStreamReader(this.inputStream);
        Map<String, Object> currentItem = null;
        String currentElement = null;

        while (reader.hasNext()) {
            int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    currentElement = reader.getLocalName();

                    if (itemName.equals(currentElement)) {
                        currentItem = new HashMap<>();
                    } else if (currentItem != null) {
                        if ("PARAM".equals(currentElement)) {
                            handleParam(reader, currentItem);
                            currentElement = null; // Reset, aby CHARACTERS nic nezachytilo
                        } else if ("DELIVERY".equals(currentElement)) {
                            handleDelivery(reader, currentItem);
                            currentElement = null;
                        }
                    }
                    break;

                case XMLStreamConstants.CHARACTERS:
                    if (currentItem != null && currentElement != null) {
                        String text = reader.getText().trim();
                        if (!text.isEmpty()) {
                            addItemValue(currentItem, currentElement, text);
                        }
                    }
                    break;

                case XMLStreamConstants.END_ELEMENT:
                    String endElement = reader.getLocalName();
                    if (itemName.equals(endElement) && currentItem != null) {
                        String id = getFirstId(currentItem);
                        if (itemsToSkip == null || !itemsToSkip.contains(id)) {
                            Map<String, String> processingItm = new HashMap<>();
                            for (String key: currentItem.keySet()) {
                                Object val = currentItem.get(key);
                                if (val instanceof String) {
                                    processingItm.put(key, (String) val);
                                } else if (val instanceof List) {
                                    List<String> lVal = (List<String>) val;
                                    String strVal = lVal.stream().collect(Collectors.joining(","));
                                    processingItm.put(key, strVal);
                                }
                            }
                            itemProcessor.accept(processingItm);
                        }
                        currentItem = null;
                    }
                    currentElement = null;
                    break;
            }
        }
        reader.close();
    }

    private void addItemValue(Map<String, Object> map, String key, String value) {
        map.compute(key, (k, existing) -> {
            if (existing == null) return value;
            if (existing instanceof List) {
                ((List<String>) existing).add(value);
                return existing;
            }
            List<String> list = new ArrayList<>();
            list.add((String) existing);
            list.add(value);
            return list;
        });
    }

    private void handleParam(XMLStreamReader reader, Map<String, Object> currentItem) throws XMLStreamException {
        String name = null;
        String value = null;
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();
                if ("PARAM_NAME".equals(localName)) name = reader.getElementText();
                else if ("VAL".equals(localName)) value = reader.getElementText();
            } else if (event == XMLStreamConstants.END_ELEMENT && "PARAM".equals(reader.getLocalName())) {
                if (name != null) addItemValue(currentItem, "PARAM_" + name, value);
                break;
            }
        }
    }

    private void handleDelivery(XMLStreamReader reader, Map<String, Object> currentItem) throws XMLStreamException {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.END_ELEMENT && "DELIVERY".equals(reader.getLocalName())) break;
        }
    }

    private String getFirstId(Map<String, Object> item) {
        Object id = item.get("id");
        if (id instanceof List) return ((List<?>) id).get(0).toString();
        return id != null ? id.toString() : null;
    }
}
package cz.inovatika.sdnnt.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.solr.common.SolrDocument;
import org.json.JSONArray;
import org.json.JSONObject;

import cz.inovatika.sdnnt.openapi.endpoints.api.impl.utils.PIDSupport;

public class LinksUtilities {
    
    private LinksUtilities() {}
    
    //TODO:  Osetrit pripad, kdy v marc_911u je link jinam nez do krameria a spravny link do krameria 
    // je v 856u  ? 
    public static  List<String> linksFromDocument(SolrDocument doc) {
        Collection<Object> mlinks911u = doc.getFieldValues("marc_911u");
        Collection<Object> mlinks856u =  doc.getFieldValues("marc_856u");
        Collection<Object> mlinks956u =  doc.getFieldValues("marc_956u");
        
        final List<String> links = new ArrayList<>();
        if (mlinks911u != null && !mlinks911u.isEmpty()) {
            mlinks911u.stream().map(Object::toString).forEach(links::add);
        } else if (mlinks856u != null) {
            mlinks856u.stream().map(Object::toString).forEach(links::add);
        } else if (mlinks956u != null) {
            mlinks956u.stream().map(Object::toString).forEach(links::add);
        }
        return links;
    }
    
    
    
    public static List<String> krameriusLinksFromDocument(SolrDocument doc) {

        Collection<Object> mlinks911u = doc.getFieldValues("marc_911u");
        Collection<Object> mlinks856u =  doc.getFieldValues("marc_856u");
        //Collection<Object> mlinks956u =  doc.getFieldValues("marc_956u");
        List<String> result = new ArrayList<>();
        if (mlinks911u != null) {

            List<String> uuidLinks = mlinks911u.stream().map(ol-> {
                String l = ol.toString();
                int indexof = l.indexOf("uuid:");
                if (indexof >= 0) {
                    return l.substring(indexof);
                } else return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());

            if (!uuidLinks.isEmpty()) {
                mlinks911u.stream().map(Object::toString).forEach(result::add);
            }
        } 
        if (result.isEmpty() && mlinks856u != null) {

            List<String> uuidLinks = mlinks856u.stream().map(ol-> {
                String l = ol.toString();
                int indexof = l.indexOf("uuid:");
                if (indexof >= 0) {
                    return l.substring(indexof);
                } else return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());
            
            if (!uuidLinks.isEmpty()) {
                mlinks856u.stream().map(Object::toString).forEach(result::add);
            }
        }
        
        return result;
    }

    public static List<String> digitalizedKeys(JSONObject digitalized,  final List<String> links) {
        List<String> siglas = new ArrayList<>();
        digitalized.keySet().forEach(key -> {
            JSONArray regexps = digitalized.getJSONObject(key).getJSONArray("regexp");
            for (Object oneRegexp : regexps) {
                // one regexps 
                if(links.stream().anyMatch(l -> {
                        return l.matches(oneRegexp.toString());
                    })) {
                    siglas.add(key);
                }
            }
        });
        return siglas;
    }
    
}

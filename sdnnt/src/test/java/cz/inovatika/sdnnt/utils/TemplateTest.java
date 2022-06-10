package cz.inovatika.sdnnt.utils;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

public class TemplateTest {

    public static StringBuilder BUILDER = new StringBuilder();

    public void testTemplate() {
        List<Map<String, String>> data = Arrays.asList(
                new HashMap<>() {{
                    put("identifier", "oai1");
                }},
                new HashMap<>() {{
                    put("identifier", "oai2");
                }}

        );



        StringWriter stringWriter = new StringWriter();
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile(new StringReader(""), "registration");
        mustache.execute(stringWriter, data);
    }
}

package cz.inovatika.sdnnt;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.services.impl.users.UserControlerImpl;
import cz.inovatika.sdnnt.utils.StringUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

/**
 * The servlet is able to serve consent configuration. 
 * In order to overrride the default use following configuration snippet
 * <br>
 * <pre>
 *  "server": {
 *      "cookieconsent-init_cs":"/path/to/config/json_cs_file",
 *      "cookieconsent-init_en":"/path/to/config/json_en_file"
 *      ....
 *  }
 * </pre>
 * @author happy
 */
public class ConsentConfigurationServlet extends HttpServlet {

    private static final String DEFAULT_LOCALE = "cs";
    private static final List<String> LOCALES = Arrays.asList("cs","en");

    public static final String MIME_TYPE = "application/json;charset=UTF-8";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType(MIME_TYPE);

        Locale locale = req.getLocale();
        String language = LOCALES.contains(locale.getLanguage()) ? locale.getLanguage() : DEFAULT_LOCALE;

        String configured = Options.getInstance().getString("cookieconsent-init_" + language);
        if (configured != null && StringUtils.isAnyString(configured)) {
            File file = new File(configured);
            if (file.exists()) {
                String s = IOUtils.toString(new FileInputStream(file), "UTF-8");
                //JSONObject jsonObject = new JSONObject(s);
                resp.getWriter().write(s.toString());
            }
        } else {

            InputStream defaultStream = this.getClass().getResourceAsStream("default_cookieconsent-init.js");
            String content = IOUtils.toString(defaultStream, "UTF-8");

            String domain = Options.getInstance().getString("domain") != null ? Options.getInstance().getString("domain") : "";

            Map<String,String> scope = new HashMap<>();
            scope.put("domain", domain);
            scope.put("language", language);


            StringWriter stringWriter = new StringWriter();
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(new StringReader(content), "registration");
            mustache.execute(stringWriter, scope);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(stringWriter.toString());
        }
    }
}

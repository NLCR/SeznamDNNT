package cz.inovatika.sdnnt.services.impl.shib;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.model.User;
import cz.inovatika.sdnnt.model.Zadost;
import cz.inovatika.sdnnt.services.ApplicationUserLoginSupport;
import cz.inovatika.sdnnt.services.exceptions.UserControlerException;
import cz.inovatika.sdnnt.services.impl.UserControlerImpl;
import cz.inovatika.sdnnt.services.impl.users.UsersUtils;
import cz.inovatika.sdnnt.tracking.TrackSessionUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ShibbolethFilter implements Filter {

    public static final Logger LOGGER = Logger.getLogger(ShibbolethFilter.class.getName());

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest hReq = (HttpServletRequest) servletRequest;
        HttpSession session = hReq.getSession(false);

        if (session != null && session.getAttribute(ApplicationUserLoginSupport.AUTHENTICATED_USER) == null && ShibbolethUtils.isUnderShibbolethSession(hReq)) {

            List<JSONObject> users = new ArrayList<>();

            List<String> attList = new ArrayList<>();
            Enumeration<String> headerNames = hReq.getHeaderNames();
            while (headerNames.hasMoreElements()) {  attList.add(headerNames.nextElement());  }

            JSONObject shibboleth = Options.getInstance().getJSONObject("shibboleth");
            if (shibboleth.has("mappings")) {
                JSONArray attributes = shibboleth.getJSONArray("mappings");
                for (int i = 0; i < attributes.length(); i++) {
                    JSONObject oneMapping = attributes.getJSONObject(i);
                    if (oneMapping.has("match")) {
                        JSONObject match = oneMapping.getJSONObject("match");

                        String header = match.optString("header","no-value");
                        String regexp = match.optString("regexp","no-value");

                        String headerVal = hReq.getHeader(header);

                        if (headerVal != null && headerVal.matches(regexp)) {
                            if (match.has("user")) {
                                JSONObject userObject = match.getJSONObject("user");
                                users.add(mapping3dUser(hReq, userObject));
                            }
                        }
                    }

                    // merge users

                    JSONObject merged = new JSONObject();
                    users.stream().forEach(o-> {
                        o.keySet().forEach(k-> {
                            merged.put(k, o.get(k));
                        });
                    });

                    User user = User.fromJSON(merged.toString());
                    if (user.getUsername() != null) {
                        user.setThirdPartyUser(true);
                        hReq.getSession(true).setAttribute(ApplicationUserLoginSupport.AUTHENTICATED_USER, user);
                        TrackSessionUtils.touchSession(hReq.getSession());
                    }
                }
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }



    private JSONObject mapping3dUser(HttpServletRequest hReq, JSONObject userObject) {
        JSONObject destination = new JSONObject();
        List<String> keys = Arrays.asList(
                User.USERNAME_KEY,
                User.JMENO_KEY,
                User.PRIJMENI_KEY,
                User.TITUL_KEY,
                User.PSC_KEY,
                User.MESTO_KEY,
                User.ULICE_KEY,
                User.CISLO_KEY,
                User.TELEFON_KEY,
                User.EMAIL_KEY,
                User.POZNAMKA_KEY,
                User.INSTITUTION_KEY,
                User.ROLE_KEY,
                User.APIKEY_KEY,
                User.NOSITEL_KEY,
                User.POZNAMKA_KEY
                );


        keys.stream().forEach(k-> {
            userProperties(hReq, userObject, destination, k);
        });

        return destination;
    }


    private void userProperties(HttpServletRequest hReq, JSONObject source, JSONObject destination, String firstname) {
        if (source.has(firstname)) {
            Object obj = source.get(firstname);
            if (obj instanceof JSONObject) {
                destination.put(firstname, processAttribute(hReq, (JSONObject) obj));
            } else  {
                destination.put(firstname, obj.toString());
            }
        }
    }


    private String processAttribute(HttpServletRequest servletRequest, JSONObject oneAttribute) {
        //oneAttribute.getString("")
        if (oneAttribute.has("headervalue")) {
            return headerValue(servletRequest, oneAttribute);
        } else if (oneAttribute.has("stringvalue")) {
            return stringValue(oneAttribute);
        }
        return null;
    }

    private String headerValue(HttpServletRequest servletRequest, JSONObject oneAttribute) {
        if (oneAttribute.has("headervalue")) {
            String nameOfHeader = oneAttribute.getString("headervalue");
            return  servletRequest.getHeader(nameOfHeader);
        } else return null;
    }

    private String stringValue(JSONObject oneAttribute) {
        if (oneAttribute.has("stringvalue")) {
            String strVal = oneAttribute.getString("stringvalue");
            return strVal;
        } else return null;
    }

    @Override
    public void destroy() {

    }
}

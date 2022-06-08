package cz.inovatika.sdnnt.services.impl.shib.mock;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.Charset;
import java.util.Hashtable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MockShibFilter implements javax.servlet.Filter {
    
    public static final Logger LOGGER = Logger.getLogger(MockShibFilter.class.getName());
    
    public static  String SHIB_KEY = "shib";

    public static Hashtable<String,String> shibTable = new Hashtable<>();
    
    public static Hashtable<String,String> initDefaultShibTable() {
        Hashtable<String,String> sTable = new Hashtable<>();
        sTable.put("shib-session-id", "_dd68cbd66641c9b647b05509ac0241f7");
        sTable.put("shib-session-index", "_36e3755e67acdeaf1b8b6f7ebebecdeb3abd6ddc98");
        sTable.put("shib-session-expires", "1592847906");
        sTable.put("shib-identity-provider", "https://shibboleth.mzk.cz/simplesaml/metadata.xml");
        sTable.put("shib-authentication-method", "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");
        sTable.put("shib-handler", "https://dnnt.mzk.cz/Shibboleth.sso");
        //remote_user = ermak@mzk.cz
        sTable.put("remote_user", "user@mzk.cz");
        sTable.put("affilation","staff@mzk.cz;member@mzk.cz;employee@mzk.cz");
        sTable.put("edupersonuniqueid","user@mzk.cz");
        return sTable;
    }
    
    public static Hashtable<String,String> initFromFile(String path) throws FileNotFoundException, IOException {
        Properties properties = new Properties();
        properties.load(new FileReader(path,Charset.forName("UTF-8")));
        
        Hashtable<String,String> tbl = new Hashtable<>();
        properties.keySet().forEach(key-> {
            tbl.put(key.toString(), properties.getProperty(key.toString()));
        });
        
        return tbl;
    }


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            String initParameter = filterConfig.getInitParameter("shibfile");
            if (initParameter != null) {
                this.shibTable = initFromFile(initParameter);
            } else {
                this.shibTable = initDefaultShibTable();
            }
        } catch ( IOException e) {
            LOGGER.log(Level.SEVERE,e.getMessage(),e);
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        String shib = servletRequest.getParameter(SHIB_KEY);
        HttpSession session = ((HttpServletRequest) servletRequest).getSession(false);
        //Hashtable table = shibTable;
        // string u
        //if (shib != null && shib.equals("default")) {
        if (shib != null && !shib.trim().equals("")) {
            session = ((HttpServletRequest) servletRequest).getSession(true);
            session.setAttribute(SHIB_KEY, true);
        }

        if (session != null &&  session.getAttribute(SHIB_KEY) != null ) {
            HttpServletRequest httpReq = (HttpServletRequest) servletRequest;
            Object o = Proxy.newProxyInstance(servletRequest.getClass().getClassLoader(), new Class[]{HttpServletRequest.class}, new MockHTTPServletInvocationHandler(shibTable, httpReq));
            filterChain.doFilter((ServletRequest) o, servletResponse);
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }

    }

    @Override
    public void destroy() { }

}

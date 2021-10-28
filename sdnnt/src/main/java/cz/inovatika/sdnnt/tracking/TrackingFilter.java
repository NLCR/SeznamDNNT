package cz.inovatika.sdnnt.tracking;

import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.services.UserControler;
import org.json.JSONObject;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * Tracking filter for setting
 */
@WebFilter("/*")
public class TrackingFilter implements javax.servlet.Filter {


    public static final Logger LOGGER = Logger.getLogger(TrackingFilter.class.getName());

    // Inactive interval 10 minutes
    public static final int DEFAULT_MAX_INACTIVE_INTERVAL = 30;
    public static final String KEY = "SESSION_UPDATED_DATE";
    public static final String REMAINING_TIME = "SESSION_REMAINING_TIME";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) servletRequest;
        HttpSession session = httpReq.getSession();
        if (session != null && session.getAttribute(UserControler.AUTHENTICATED_USER) != null) {

            int maxInactiveInterval = DEFAULT_MAX_INACTIVE_INTERVAL;
            JSONObject sessionConfiguration = Options.getInstance().getJSONObject("session");
            if (sessionConfiguration != null && sessionConfiguration.has("maxinactiveinterval")) {
                maxInactiveInterval = sessionConfiguration.getInt("maxinactiveinterval");
            }

            String pathInfo = httpReq.getPathInfo();
            if (pathInfo == null || !pathInfo.endsWith("/ping")) {
                LOGGER.info(String.format("Updating session %s with max inactive interval %d", session.getId(), maxInactiveInterval));
                TrackSessionUtils.touchSession(session);
                List<String> attnames = new ArrayList<>();
                session.getAttributeNames().asIterator().forEachRemaining(attnames::add);

                LOGGER.info("Session attribute names "+attnames);
            } else {

                Date lastTouch = (Date) session.getAttribute(KEY);
                if (lastTouch == null) {
                    lastTouch = (Date) session.getAttribute(TrackSessionListener.KEY);
                }
                if (lastTouch != null) {
                    long diff = (new Date()).getTime() - lastTouch.getTime();
                    int diffInSec = (int)(diff/1000);
                    int rem = maxInactiveInterval - diffInSec;
                    if (rem > 0) {
                        session.setAttribute(REMAINING_TIME, rem);
                        session.setMaxInactiveInterval(rem);
                    } else {
                        session.removeAttribute(REMAINING_TIME);
                    }
                }
            }
        }
        // check path; if it is ping
        //servletRequest.getPathInfo()
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {

    }
    // implements Filter's methods: init(), doFilter() and destroy()
}
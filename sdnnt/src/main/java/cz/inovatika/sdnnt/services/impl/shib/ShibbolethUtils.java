/*
 * Copyright (C) 2010 Pavel Stastny
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.inovatika.sdnnt.services.impl.shib;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.logging.Logger;

/**
 * Only utilities
 * @author pavels
 */
public class ShibbolethUtils {

    public static final Logger LOGGER = Logger.getLogger(ShibbolethUtils.class.getName());

    private static final String SHIB_SESSION_KEY = "Shib-Session-ID";
    private static final String SHIB_PROVIDER_KEY = "Shib-Identity-Provider";
    
    /**
     * Returns true if current user is under shibboleth session
     * @param httpServletRequest
     * @return
     */
    public static boolean isUnderShibbolethSession(HttpServletRequest httpServletRequest) {
        boolean foundIdentityProvider = false;
        Enumeration headerNames = httpServletRequest.getHeaderNames();
        while(headerNames.hasMoreElements()) {
            String hname = (String) headerNames.nextElement();
            if (hname.toLowerCase().contains(SHIB_PROVIDER_KEY.toLowerCase())) {
                String headerValue = httpServletRequest.getHeader(hname);
                if ((headerValue != null) && (!headerValue.trim().equals(""))) {
                    foundIdentityProvider = true;
                }
            }
            LOGGER.fine("header name '"+hname+"' = "+httpServletRequest.getHeader(hname));
        }
        return foundIdentityProvider;
    }

    
    public static boolean validateShibbolethSessionId(HttpServletRequest httpServletRequest) {
        String _id = getShibbolethSessionId(httpServletRequest);
        HttpSession session = httpServletRequest.getSession(true);
        Object _stored_id = session.getAttribute(SHIB_SESSION_KEY);
        if (_id != null) {
            return _id.equals(_stored_id);
        } else {
            return _stored_id  == null;
        }
    }
    
    public static String getShibbolethSessionId(HttpServletRequest httpServletRequest) {
        Enumeration headerNames = httpServletRequest.getHeaderNames();
        while(headerNames.hasMoreElements()) {
            String hname = (String) headerNames.nextElement();
            if (hname.toLowerCase().contains(SHIB_SESSION_KEY.toLowerCase())) {
                String headerValue = httpServletRequest.getHeader(hname);
                if ((headerValue != null) && (!headerValue.trim().equals(""))) {
                    return headerValue;
                }
            }
            LOGGER.fine("header name '"+hname+"' = "+httpServletRequest.getHeader(hname));
        }
        return null;
    }
    
    public static void storeShibbolethSession(HttpServletRequest httpServletRequest) {
        httpServletRequest.getSession(true).setAttribute(SHIB_SESSION_KEY, getShibbolethSessionId(httpServletRequest));
    }

    public static void clearShibbolethSessionValue(HttpServletRequest httpServletRequest) {
        httpServletRequest.getSession().removeAttribute(SHIB_SESSION_KEY);
    }

    public static boolean isShibbolethSessionIsStored(HttpServletRequest httpServletRequest) {
        Object value = httpServletRequest.getSession(true).getAttribute(SHIB_SESSION_KEY);
        return value != null;
    }
}

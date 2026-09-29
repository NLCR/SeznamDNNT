package cz.inovatika.sdnnt.tracking;

import javax.servlet.http.HttpSession;
import java.util.Date;

public class TrackSessionUtils {

    private TrackSessionUtils() {}

    public static void touchSession(HttpSession session) {
        if (Boolean.TRUE.equals(session.getAttribute(TrackingFilter.KEEP_LOGGED_IN))) {
            session.setMaxInactiveInterval(-1);
        } else {
            session.setMaxInactiveInterval(TrackingFilter.DEFAULT_MAX_INACTIVE_INTERVAL);
        }
        session.setAttribute(TrackingFilter.KEY, new Date());
    }
}

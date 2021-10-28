package cz.inovatika.sdnnt.tracking;

import javax.servlet.http.HttpSession;
import java.util.Date;

public class TrackSessionUtils {

    private TrackSessionUtils() {}

    public static void touchSession(HttpSession session) {
        session.setMaxInactiveInterval(TrackingFilter.DEFAULT_MAX_INACTIVE_INTERVAL);
        session.setAttribute(TrackingFilter.KEY, new Date());
    }
}

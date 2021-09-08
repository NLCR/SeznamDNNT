package cz.inovatika.sdnnt.utils;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IPAddressUtils {

    private IPAddressUtils() {}

    public static Logger LOGGER = Logger.getLogger(IPAddressUtils.class.getName());

    public static final String LOCALHOST_DNS = "localhost";

    public static final String X_IP_FORWARD = "X-Forwarded-For";
    public static String[] LOCALHOSTS = {"127.0.0.1","0:0:0:0:0:0:0:1","::1", LOCALHOST_DNS};

    static {
        try {
            IPAddressUtils.LOCALHOSTS = getLocalhostsAddress();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            IPAddressUtils.LOCALHOSTS = new String[] {"127.0.0.1","0:0:0:0:0:0:0:1","::1", LOCALHOST_DNS};
        }
    }
    public static String getRemoteAddress(HttpServletRequest httpReq) {
        String headerFowraded = httpReq.getHeader(X_IP_FORWARD);
        if (StringUtils.isAnyString(headerFowraded)) {
            return headerFowraded;
        } else {
            return httpReq.getRemoteAddr();
        }
    }


    public static boolean isLocalAddress(HttpServletRequest httpReq) {
        String remoteAddress = getRemoteAddress(httpReq);
        return Arrays.asList(LOCALHOSTS).contains(remoteAddress);
    }





    private static String[] getLocalhostsAddress() {
        List<String> alist = new ArrayList<String>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (!iface.isUp())
                    continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    String ip = addr.getHostAddress();
                    LOGGER.fine("local ip address "+ip);
                    String ipaddr = ip;
                    if (ip.contains("%"+iface.getDisplayName())) {
                        LOGGER.fine("removing postfix "+"%"+iface.getDisplayName());
                        ipaddr = StringUtils.minus(ip, "%"+iface.getDisplayName());
                    }
                    alist.add(ipaddr);
                }
            }
        } catch (SocketException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        if (!alist.contains(LOCALHOST_DNS)) {
            alist.add(LOCALHOST_DNS);
        }
        return alist.toArray(new String[alist.size()]);
    }
}

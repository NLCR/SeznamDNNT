package cz.inovatika.sdnnt.openapi;

import org.apache.commons.io.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(value = "/openapi/*")
public class OpenApiDocServlet  extends HttpServlet {

    public static final Logger LOGGER = Logger.getLogger(OpenApiDocServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String spath =  req.getPathInfo().startsWith("/") ? req.getPathInfo().substring(1) : req.getPathInfo();
        InputStream resource = this.getClass().getResourceAsStream(spath);
        if( resource != null) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/x-yaml");
            String s = IOUtils.toString(resource, "UTF-8");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(s);
        } else {
            LOGGER.log(Level.SEVERE, String.format("Cannot find path %s", spath));
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}

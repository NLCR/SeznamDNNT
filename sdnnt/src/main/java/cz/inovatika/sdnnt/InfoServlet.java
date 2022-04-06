package cz.inovatika.sdnnt;

import org.apache.commons.io.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

/**
 * The servlet is able to serve properties file with build info (git hash and version)
 * @author happy
 */
@WebServlet(value = "/info/git")
public class InfoServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream("/git.properties");
        resp.setContentType("application/json");
        if (stream != null) {
            IOUtils.copy(stream, resp.getOutputStream());
        } else {
            IOUtils.copy(new StringReader("{}"), resp.getWriter());
        }
    }
}

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

@WebServlet(value = "/openapi/api.yaml")
public class OpenApiDocServlet  extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        InputStream resource = this.getClass().getResourceAsStream("api.yaml");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/x-yaml");
        String s = IOUtils.toString(resource, "UTF-8");
        resp.getWriter().write(s);
    }
}

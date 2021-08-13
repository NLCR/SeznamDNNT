package cz.inovatika.sdnnt.utils;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/** Utility methods for servlets */
public class ServletsSupport {

    private ServletsSupport() {}

    /** Generic method for reading json object from request */
    public static JSONObject readInputJSON(HttpServletRequest req) throws IOException {
      JSONObject inputJs;
      if (req.getMethod().equals("POST")) {
        inputJs = new JSONObject(IOUtils.toString(req.getInputStream(), "UTF-8"));
      } else {
        inputJs = new JSONObject(req.getParameter("json"));
      }
      return inputJs;
    }
}

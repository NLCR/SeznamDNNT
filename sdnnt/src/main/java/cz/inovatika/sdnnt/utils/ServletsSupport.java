package cz.inovatika.sdnnt.utils;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Utility methods for servlets */
public class ServletsSupport {

    private ServletsSupport() {}

    /** Generic method for reading json object from request */
    public static JSONObject readInputJSON(HttpServletRequest req) throws IOException {
      if (req.getMethod().equals("POST")) {
          String content = IOUtils.toString(req.getInputStream(), "UTF-8");
          return  new JSONObject(content);
      } else {
        return new JSONObject(req.getParameter("json"));
      }
    }

    /** Error json */
    public static JSONObject errorJson(HttpServletResponse response, int statusCode, String errorMessage) {
        if (statusCode != -1) response.setStatus(statusCode);
        JSONObject errorObject = new JSONObject();
        errorObject.put("error", errorMessage);
        return errorObject;
    }



    /** Error json */
    public static JSONObject errorJson(String errorMessage) {
        return errorJson(null, -1, errorMessage);
    }

}

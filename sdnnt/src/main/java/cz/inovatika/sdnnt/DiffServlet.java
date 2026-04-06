package cz.inovatika.sdnnt;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(value = "/diff/*")
public class DiffServlet extends HttpServlet {

    public static Logger LOGGER = Logger.getLogger(DiffServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String pathInfo = req.getPathInfo();
            Actions actionToDo = Actions.LATEST;

            if (pathInfo != null && pathInfo.length() > 1) {
                try {
                    String actionName = pathInfo.substring(1).toUpperCase();
                    actionToDo = Actions.valueOf(actionName);
                } catch (IllegalArgumentException e) {
                    actionToDo = Actions.LIST;
                }
            }
            actionToDo.doPerform(req, resp);
        } catch (IOException e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e1.toString());
        } catch (SecurityException e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } catch (Exception e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e1.toString());
        }

    }

    enum Actions {
        LATEST {
            @Override
            public void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                String fileName = req.getParameter("file");
                String DEFAULT_OUTPUT_FOLDER = System.getProperty("user.home") + File.separator + ".sdnnt/diff";

                File dir = new File(DEFAULT_OUTPUT_FOLDER);
                File[] files = dir.listFiles((d, name) -> name.startsWith("diff_") && name.endsWith(".html"));

                if (files == null || files.length == 0) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Žádné reporty nebyly nalezeny.");
                    return;
                }

                File latest = Arrays.stream(files)
                        .max(Comparator.comparingLong(File::lastModified))
                        .orElse(null);

                if (latest != null) {
                    resp.setContentType("text/html;charset=UTF-8");
                    Files.copy(latest.toPath(), resp.getOutputStream());
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            }
        },

        SHOWFILE {
            @Override
            public void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                String fileName = req.getParameter("file");
                String DEFAULT_OUTPUT_FOLDER = System.getProperty("user.home") + File.separator + ".sdnnt/diff";
                File dir = new File(DEFAULT_OUTPUT_FOLDER);

                if (fileName.contains("..") || !fileName.endsWith(".html")) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Neplatný název souboru.");
                    return;
                }

                File file = new File(dir, fileName);
                if (!file.exists()) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Report nenalezen.");
                    return;
                }

                resp.setContentType("text/html;charset=UTF-8");
                Files.copy(file.toPath(), resp.getOutputStream());

            }
        },
        LIST {
            @Override
            public void doPerform(HttpServletRequest req, HttpServletResponse resp) throws Exception {

                String fileName = req.getParameter("file");
                String DEFAULT_OUTPUT_FOLDER = System.getProperty("user.home") + File.separator + ".sdnnt/diff";
                File dir = new File(DEFAULT_OUTPUT_FOLDER);

                resp.setContentType("text/html;charset=UTF-8");
                StringBuilder sb = new StringBuilder();
                sb.append("<html><head><title>Seznam Diff Reportů</title><style>");
                sb.append("body { font-family: sans-serif; padding: 20px; }");
                sb.append("ul { list-style: none; padding: 0; }");
                sb.append("li { padding: 8px; border-bottom: 1px solid #eee; }");
                sb.append("a { text-decoration: none; color: #0366d6; font-weight: bold; }");
                sb.append("a:hover { text-decoration: underline; }");
                sb.append(".date { color: #666; font-size: 0.9em; margin-left: 10px; }");
                sb.append("</style></head><body>");
                sb.append("<h1>Dostupné Diff Reporty</h1><ul>");

                File[] files = dir.listFiles((d, name) -> name.startsWith("diff_") && name.endsWith(".html"));

                if (files != null && files.length > 0) {
                    Arrays.sort(files, Comparator.comparing(File::getName).reversed());

                    for (File f : files) {
                        sb.append("<li>")
                                .append("<a href='?file=").append(f.getName()).append("'>")
                                .append(f.getName()).append("</a>")
                                .append("<span class='date'>(").append(new java.util.Date(f.lastModified())).append(")</span>")
                                .append("</li>");
                    }
                } else {
                    sb.append("<li>Žádné reporty nebyly nalezeny v: ").append(dir.getAbsolutePath()).append("</li>");
                }

                sb.append("</ul></body></html>");
                resp.getWriter().write(sb.toString());

            }
        };
        public abstract void doPerform(HttpServletRequest req, HttpServletResponse response) throws Exception;

    };


}

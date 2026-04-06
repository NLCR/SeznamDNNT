package cz.inovatika.sdnnt;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

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

@WebServlet(value = "/diff")
public class DiffServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String fileName = req.getParameter("file");
        String DEFAULT_OUTPUT_FOLDER = System.getProperty("user.home") + File.separator + ".sdnnt/diff";
        File dir = new File(DEFAULT_OUTPUT_FOLDER);

        if (StringUtils.isNotEmpty(fileName)) {
            showFile(fileName, dir, resp);
        } else {
            listFiles(dir, resp);
        }
    }

    private void showFile(String fileName, File dir, HttpServletResponse resp) throws IOException {
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

    private void listFiles(File dir, HttpServletResponse resp) throws IOException {
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

}

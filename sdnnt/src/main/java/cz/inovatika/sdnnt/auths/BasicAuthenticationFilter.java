package cz.inovatika.sdnnt.auths;

import cz.inovatika.sdnnt.UserController;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class BasicAuthenticationFilter implements Filter {



    public void basicAuth(FilterChain arg2, HttpServletRequest request, HttpServletResponse response) throws NoSuchAlgorithmException, IOException, ServletException {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String header = httpServletRequest.getHeader("Authorization");
        if (header!=null && header.trim().startsWith("Basic")) {
            String uname = header.trim().substring("Basic".length()).trim();
            byte[] decoded = Base64.getDecoder().decode(uname);
            //byte[] decoded = Base64Coder.decode(uname.toCharArray());
            String fname = new String(decoded, "UTF-8");
            if (fname.contains(":")) {
                String username = fname.substring(0, fname.indexOf(':'));
                String password = fname.substring(fname.indexOf(':')+1);
                UserController.loginByNameAndPassword(httpServletRequest, username, password);
                filterChain.doFilter(request, response);

            } else {
                filterChain.doFilter(request, response);
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {

    }
}

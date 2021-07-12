package cz.inovatika.sdnnt.auths;

import cz.inovatika.sdnnt.UserController;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;

public class ApiKeyAuthenticationFilter implements Filter {

    public static final String API_KEY_HEADER = "X-API-KEY";


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest arg0, ServletResponse arg1, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) arg0;
        HttpSession session = request.getSession();
        if (session != null && session.getAttribute(UserController.AUTHENTICATED_USER) != null) {
            //session with authenticated user
            filterChain.doFilter(arg0, arg1);
        } else  {
            String header = request.getHeader(API_KEY_HEADER);
            if (header != null) {
                UserController.loginByApiKey(request, header);
            }
            filterChain.doFilter(arg0, arg1);
        }
    }



    @Override
    public void destroy() {

    }
}

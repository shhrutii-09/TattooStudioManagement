package mbclient;

import beans.UserSessionBean;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(filterName = "ClientSessionFilter", urlPatterns = "/faces/web/client/*")
public class ClientSessionFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String uri = req.getRequestURI();

        // ----------------------------
        // PUBLIC CLIENT HOME (READ-ONLY)
        // ----------------------------
        if (uri.endsWith("/client/home.xhtml")) {
            chain.doFilter(request, response);
            return;
        }

        UserSessionBean sessionBean = (UserSessionBean)
                req.getSession().getAttribute("userSession");

        // ----------------------------
        // GUEST USER → BACK TO HOME
        // ----------------------------
        if (sessionBean == null || !sessionBean.isLoggedIn()) {
            res.sendRedirect(req.getContextPath() + "/faces/web/client/home.xhtml");
            return;
        }

        // ----------------------------
        // LOGGED IN BUT NOT CLIENT
        // ----------------------------
        if (!sessionBean.isClient()) {
            res.sendRedirect(req.getContextPath() + "/faces/web/client/home.xhtml");
            return;
        }

        // ----------------------------
        // CLIENT ALLOWED
        // ----------------------------
        chain.doFilter(request, response);
    }
}

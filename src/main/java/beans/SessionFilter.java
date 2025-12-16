package beans;

import beans.UserSessionBean;
import jakarta.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(filterName = "SessionFilter", urlPatterns = {"/web/admin/*", "/web/artist/*"})
public class SessionFilter implements Filter {
    
    @Inject
    private UserSessionBean sessionBean;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        
        // Check if user is logged in
        if (!sessionBean.isLoggedIn()) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/web/login.xhtml");
            return;
        }
        
        // Check admin access
        if (path.startsWith("/web/admin/") && !sessionBean.isAdmin()) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/web/access-denied.xhtml");
            return;
        }
        
        // Check artist access
        if (path.startsWith("/web/artist/") && !sessionBean.isArtist()) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/web/access-denied.xhtml");
            return;
        }
        
        // Check artist verification for artist dashboard (but allow pending page)
        if (path.startsWith("/web/artist/") && !path.contains("/pending.xhtml") 
            && sessionBean.isArtist() && !sessionBean.isArtistVerified()) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/web/artist/pending.xhtml");
            return;
        }
        
        chain.doFilter(request, response);
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization code if needed
    }
    
    @Override
    public void destroy() {
        // Cleanup code if needed
    }
}
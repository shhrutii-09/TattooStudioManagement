package filters;

import beans.UserSessionBean;
import jakarta.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(
    filterName = "SecurityFilter",
    urlPatterns = {"/web/admin/*", "/web/artist/*", "/web/client/*"},
    servletNames = {"Faces Servlet"}
)
public class SecurityFilter implements Filter {
    
    @Inject
    private UserSessionBean sessionBean;
    
    // URLs that are accessible without login
    private static final String[] PUBLIC_URLS = {
        "/web/login.xhtml",
        "/web/register.xhtml",
        "/web/forgot-password.xhtml",
        "/web/reset-password.xhtml",
        "/web/verify-email.xhtml",
        "/web/access-denied.xhtml",
        "/web/error.xhtml",
        "/web/client/home.xhtml",
        "/web/client/designs.xhtml",
        "/web/client/artist-list.xhtml",
        "/web/client/artist-profile.xhtml",
        "/web/resources/"  // Allow access to CSS, JS, images
    };
    
    // URLs that require login but not specific role
    private static final String[] AUTHENTICATED_URLS = {
        "/web/client/profile.xhtml",
        "/web/client/appointments.xhtml",
        "/web/client/book-appointment.xhtml",
        "/web/client/my-designs.xhtml"
    };
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String contextPath = httpRequest.getContextPath();
        String requestURI = httpRequest.getRequestURI();
        String path = requestURI.substring(contextPath.length());
        
        // Debug logging (remove in production)
        System.out.println("SecurityFilter checking: " + path);
        System.out.println("User logged in: " + (sessionBean != null && sessionBean.isLoggedIn()));
        if (sessionBean != null && sessionBean.isLoggedIn()) {
            System.out.println("User role: " + sessionBean.getRole());
        }
        
        // Check if it's a resource request (CSS, JS, images)
        if (isResourceRequest(path)) {
            chain.doFilter(request, response);
            return;
        }
        
        // Check if URL is public
        if (isPublicUrl(path)) {
            chain.doFilter(request, response);
            return;
        }
        
        // If sessionBean is null (CDI injection failed), redirect to error
        if (sessionBean == null) {
            System.err.println("SecurityFilter: sessionBean is null - CDI injection failed");
            httpResponse.sendRedirect(contextPath + "/web/error.xhtml");
            return;
        }
        
        // Check if user is logged in for authenticated URLs
        if (!sessionBean.isLoggedIn()) {
            // Store the attempted URL for redirect after login
            String redirectUrl = httpRequest.getRequestURI();
            if (httpRequest.getQueryString() != null) {
                redirectUrl += "?" + httpRequest.getQueryString();
            }
            httpRequest.getSession().setAttribute("redirectAfterLogin", redirectUrl);
            
            // Redirect to login
            httpResponse.sendRedirect(contextPath + "/web/login.xhtml");
            return;
        }
        
        // Check role-based access
        if (!hasAccessToPath(path, sessionBean)) {
            // Log access denied attempt
            System.out.println("Access denied for user " + sessionBean.getUsername() + 
                             " to path: " + path + " (Role: " + sessionBean.getRole() + ")");
            
            httpResponse.sendRedirect(contextPath + "/web/access-denied.xhtml");
            return;
        }
        
        // Check artist verification status
        if (requiresArtistVerification(path, sessionBean)) {
            httpResponse.sendRedirect(contextPath + "/web/artist/pending.xhtml");
            return;
        }
        
        // All checks passed - proceed
        chain.doFilter(request, response);
    }
    
    private boolean isResourceRequest(String path) {
        return path.startsWith("/web/resources/") || 
               path.startsWith("/javax.faces.resource/") ||
               path.contains(".css") || 
               path.contains(".js") || 
               path.contains(".png") || 
               path.contains(".jpg") || 
               path.contains(".gif") ||
               path.contains(".ico") ||
               path.contains(".woff") ||
               path.contains(".woff2") ||
               path.contains(".ttf") ||
               path.contains(".map");
    }
    
    private boolean isPublicUrl(String path) {
        for (String publicUrl : PUBLIC_URLS) {
            if (path.equals(publicUrl) || path.startsWith(publicUrl)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean hasAccessToPath(String path, UserSessionBean session) {
        // Check admin area
        if (path.startsWith("/web/admin/")) {
            return session.isAdmin();
        }
        
        // Check artist area
        if (path.startsWith("/web/artist/")) {
            // Allow access to pending page for unverified artists
            if (path.contains("/pending.xhtml")) {
                return session.isArtist();
            }
            return session.isArtist();
        }
        
        // Check client area - any logged in user can access
        if (path.startsWith("/web/client/")) {
            // These are public URLs already filtered above
            // Remaining client URLs require login but any role
            return true;
        }
        
        // Default: allow access
        return true;
    }
    
    private boolean requiresArtistVerification(String path, UserSessionBean session) {
        // Only check for artists
        if (!session.isArtist()) {
            return false;
        }
        
        // If artist is already verified, no restriction
        if (session.isArtistVerified()) {
            return false;
        }
        
        // Unverified artists can only access:
        // 1. Pending page
        // 2. Logout
        // 3. Profile page (maybe)
        
        if (path.contains("/pending.xhtml") || 
            path.contains("/logout") ||
            path.contains("/profile.xhtml") ||
            path.contains("/login.xhtml")) {
            return false;
        }
        
        // For all other artist pages, require verification
        return path.startsWith("/web/artist/");
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("SecurityFilter initialized");
    }
    
    @Override
    public void destroy() {
        System.out.println("SecurityFilter destroyed");
    }
}
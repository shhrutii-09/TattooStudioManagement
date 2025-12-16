package beans;

import entities.AppUser;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;

@Named
@SessionScoped
public class UserSessionBean implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long userId;
    private String username;
    private String email;
    private String role;
    private boolean artistVerified;
    private boolean loggedIn;
    private AppUser currentUser; // Store the full user object

    public void setUser(AppUser user) {
        this.currentUser = user;
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.role = user.getRole().getRoleName();
        this.loggedIn = true;

        // Only artists have verification status
        if (role.equalsIgnoreCase("ARTIST")) {
            this.artistVerified = user.isIsVerified();
        } else {
            this.artistVerified = true;  // Admin & Client don't need verification
        }
        
        // Store in session map as well for broader access
        FacesContext.getCurrentInstance()
            .getExternalContext()
            .getSessionMap()
            .put("userSession", this);
    }

    public void clear() {
        this.currentUser = null;
        this.userId = null;
        this.username = null;
        this.email = null;
        this.role = null;
        this.artistVerified = false;
        this.loggedIn = false;
        
        // Remove from session map
        FacesContext.getCurrentInstance()
            .getExternalContext()
            .getSessionMap()
            .remove("userSession");
        
        // Invalidate session if needed
        FacesContext.getCurrentInstance()
            .getExternalContext()
            .invalidateSession();
    }
    
    public boolean isUserInRole(String roleName) {
        return loggedIn && role != null && role.equalsIgnoreCase(roleName);
    }
    
    public boolean isAdmin() {
        return isUserInRole("ADMIN");
    }
    
    public boolean isArtist() {
        return isUserInRole("ARTIST");
    }
    
    public boolean isClient() {
        return isUserInRole("CLIENT") || (!isAdmin() && !isArtist() && loggedIn);
    }
    public AppUser getUser() {
    return currentUser;
}


    // Getters
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public boolean isArtistVerified() { return artistVerified; }
    public boolean isLoggedIn() { return loggedIn; }
    public AppUser getCurrentUser() { return currentUser; }
}
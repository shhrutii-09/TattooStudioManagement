package beans;

import entities.AppUser;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;

@Named
@SessionScoped
public class UserSessionBean implements Serializable {

    private Long userId;
    private String username;
    private String role;
    private boolean artistVerified;
    private boolean loggedIn;

    public void setUser(AppUser user) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.role = user.getRole().getRoleName();
        this.loggedIn = true;

        // Only artists have verification status
        if (role.equalsIgnoreCase("ARTIST")) {
            this.artistVerified = user.isIsVerified();
        } else {
            this.artistVerified = true;  // Admin & Client don't need verification
        }
    }

    public void clear() {
        this.userId = null;
        this.username = null;
        this.role = null;
        this.artistVerified = false;
        this.loggedIn = false;
    }

    // Getters
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public boolean isArtistVerified() { return artistVerified; }
    public boolean isLoggedIn() { return loggedIn; }
}

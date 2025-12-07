package beans;

import dto.UserLoginDTO;
import entities.AppUser;
import ejb.AuthEJB;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;

@Named
@SessionScoped
public class LoginBean implements Serializable {

    private UserLoginDTO loginDTO = new UserLoginDTO();

    @EJB
    private AuthEJB authEJB;

    @Inject
    private UserSessionBean sessionBean;

    public String login() {
    try {
        AppUser user = authEJB.authenticateUser(
                loginDTO.getUsername(),
                loginDTO.getPassword()
        );

        if (user == null) {
            addError("Invalid username or password.");
            return null;
        }

        // Artist verification
        if (user.getRole().getRoleName().equalsIgnoreCase("ARTIST") &&
            !user.isIsVerified()) {

            addError("Your artist profile is not verified yet. Please wait for admin approval.");
            return null;
        }

        // Save user
        sessionBean.setUser(user);
        FacesContext.getCurrentInstance()
            .getExternalContext()
            .getSessionMap()
            .put("user", user); // key: "user"

        addInfo("Login successful!");

        
        return navigateByRole(user.getRole().getRoleName());

    } catch (Exception e) {
        addError("Login error: " + e.getMessage());
        return null;
    }
}


    public String logout() {
        sessionBean.clear();
        loginDTO = new UserLoginDTO();
        // Update this to point to the correct location of login.xhtml
        return "/web/login.xhtml?faces-redirect=true"; 
    }

    private String navigateByRole(String role) {
        switch (role.toUpperCase()) {
            case "ADMIN": 
                // Add leading slash "/"
                return "/web/admin/dashboard.xhtml?faces-redirect=true";

            case "ARTIST": 
                // Add leading slash "/"
                return sessionBean.isArtistVerified()
                        ? "/web/artist/dashboard.xhtml?faces-redirect=true"
                        : "/web/artist/pending.xhtml?faces-redirect=true";

            default:
                // Add leading slash "/"
                return "/web/client/home.xhtml?faces-redirect=true";
        }
    }

    private void addError(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }

    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }

    public UserLoginDTO getLoginDTO() {
        return loginDTO;
    }
}

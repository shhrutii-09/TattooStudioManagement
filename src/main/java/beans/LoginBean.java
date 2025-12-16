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
import jakarta.servlet.http.HttpServletRequest;
import java.io.Serializable;

@Named
@SessionScoped
public class LoginBean implements Serializable {

    private static final long serialVersionUID = 1L;
    
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

            // Set user in session bean
            sessionBean.setUser(user);
            
            // Also store user object in session for broader access
            FacesContext.getCurrentInstance()
                .getExternalContext()
                .getSessionMap()
                .put("loggedInUser", user);
            
            // Set session timeout (30 minutes)
            HttpServletRequest request = (HttpServletRequest) 
                FacesContext.getCurrentInstance().getExternalContext().getRequest();
            request.getSession().setMaxInactiveInterval(30 * 60);

            addInfo("Login successful!");
            
            // Redirect based on role
            return navigateByRole(user.getRole().getRoleName());

        } catch (Exception e) {
            addError("Login error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public String logout() {
        try {
            // Clear session bean
            sessionBean.clear();
            
            // Clear login DTO
            loginDTO = new UserLoginDTO();
            
            // Invalidate the session
            FacesContext.getCurrentInstance()
                .getExternalContext()
                .invalidateSession();
            
            addInfo("You have been logged out successfully.");
            
            // Redirect to login page
            return "/web/login.xhtml?faces-redirect=true";
            
        } catch (Exception e) {
            addError("Logout error: " + e.getMessage());
            return null;
        }
    }

    private String navigateByRole(String role) {
        switch (role.toUpperCase()) {
            case "ADMIN": 
                return "/web/admin/dashboard.xhtml?faces-redirect=true";

            case "ARTIST": 
                return sessionBean.isArtistVerified()
                        ? "/web/artist/dashboard.xhtml?faces-redirect=true"
                        : "/web/artist/pending.xhtml?faces-redirect=true";

            case "CLIENT":
                return "/web/client/home.xhtml?faces-redirect=true";

            default:
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
    
    public void setLoginDTO(UserLoginDTO loginDTO) {
        this.loginDTO = loginDTO;
    }
}
package beans;

import dto.AdminProfileDTO;
import ejb.AdminEJBLocal;
import entities.AppUser;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;

@Named
@ViewScoped
public class AdminProfileBean implements Serializable {

    @EJB
    private AdminEJBLocal adminEJB;

    private AdminProfileDTO profile = new AdminProfileDTO();
    private String oldPassword;
    private String newPassword;

   @PostConstruct
public void init() {
    FacesContext fc = FacesContext.getCurrentInstance();
    AppUser user = (AppUser) fc.getExternalContext().getSessionMap().get("user");
    if (user == null) {
        fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
                "Session expired", "Please log in again."));
        return;
    }
    AdminProfileDTO dto = adminEJB.getAdminProfile(user.getUserId());
    if (dto != null) {
        this.profile = dto;
    }
}

    public void saveDetails() {
        boolean ok = adminEJB.updateAdminDetails(profile);
        FacesContext fc = FacesContext.getCurrentInstance();
        if (ok) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Profile updated", "Your details were saved."));
        } else {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Update failed", "Please try again."));
        }
    }

    public void updatePassword() {
        boolean ok = adminEJB.changeAdminPassword(profile.getUserId(), oldPassword, newPassword);
        FacesContext fc = FacesContext.getCurrentInstance();
        if (ok) {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Password changed", "Use your new password next login."));
            oldPassword = "";
            newPassword = "";
        } else {
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Password change failed", "Old password incorrect or operation failed."));
        }
    }

    
    
    // --- Getters/Setters ---
    public AdminProfileDTO getProfile() { return profile; }
    public void setProfile(AdminProfileDTO profile) { this.profile = profile; }

    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
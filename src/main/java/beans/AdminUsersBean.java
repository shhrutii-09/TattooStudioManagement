package beans;

import entities.AppUser;
import ejb.AdminEJBLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named("adminUsersBean")
@ViewScoped
public class AdminUsersBean implements Serializable {

    @EJB
    private AdminEJBLocal adminEJB;

    private List<AppUser> users;
    private AppUser viewingUser;
    private AppUser selectedUser;

    private Long userToDeleteId;

    @PostConstruct
    public void init() {
        loadUsers();
    }

    public void loadUsers() {
        try {
            users = adminEJB.listAllUsers();
        } catch (Exception ex) {
            addError("Failed to load users: " + ex.getMessage());
        }
    }

    public void viewUser(Long id) {
        try {
            viewingUser = adminEJB.getUserById(id);
        } catch (Exception ex) {
            addError(ex.getMessage());
        }
    }

    public void toggleActive(Long userId, boolean deactivate) {
        try {
            adminEJB.deactivateUser(userId, deactivate, deactivate ? "Deactivated by admin" : null);
            addInfo(deactivate ? "User deactivated." : "User activated.");
            loadUsers();
        } catch (Exception ex) {
            addError(ex.getMessage());
        }
    }

    public void verifyArtist(Long artistId, boolean verify) {
        try {
            adminEJB.verifyArtist(artistId, verify, getCurrentAdminId());
            addInfo(verify ? "Artist verified." : "Artist unverified.");
            loadUsers();
        } catch (Exception ex) {
            addError(ex.getMessage());
        }
    }

    public void deleteUser(Long userId) {
    try {
        adminEJB.deleteUser(userId);
        addInfo("User deleted successfully.");
        loadUsers();
    } catch (IllegalArgumentException ex) {
        addError("Failed to delete: " + ex.getMessage());
    } catch (Exception ex) {
        addError("Unexpected error: " + ex.getMessage());
    }
}


    private Long getCurrentAdminId() {
        try {
            UserSessionBean usb = (UserSessionBean) FacesContext.getCurrentInstance()
                    .getApplication()
                    .evaluateExpressionGet(FacesContext.getCurrentInstance(),
                            "#{userSessionBean}", UserSessionBean.class);
            return usb.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }

    private void addError(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }
    
    public void prepareDelete(Long userId) {
    this.userToDeleteId = userId;
}


    // Getters & Setters
    public List<AppUser> getUsers() { return users; }
    public AppUser getViewingUser() { return viewingUser; }
    public AppUser getSelectedUser() { return selectedUser; }
    public void setSelectedUser(AppUser selectedUser) { this.selectedUser = selectedUser; }
    public Long getUserToDeleteId() { return userToDeleteId; }
    public void setUserToDeleteId(Long userToDeleteId) { this.userToDeleteId = userToDeleteId; }
}

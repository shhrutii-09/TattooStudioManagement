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
import java.util.ArrayList;
import java.util.List;

@Named("adminArtistsBean")
@ViewScoped
public class AdminArtistsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private AdminEJBLocal adminEJB;

    private List<AppUser> artists = new ArrayList<>();
    private AppUser viewingArtist;
    private Long artistToDeleteId;

    @PostConstruct
    public void init() {
        loadArtists();
    }

    public void loadArtists() {
        try {
            artists = adminEJB.listArtists();
        } catch (Exception ex) {
            addError("Failed to load artists: " + ex.getMessage());
        }
    }

    public void viewArtist(Long id) {
        try {
            viewingArtist = adminEJB.getUserById(id);
            if (viewingArtist == null) {
                addError("Artist not found.");
            }
        } catch (Exception ex) {
            addError("Error loading artist: " + ex.getMessage());
        }
    }

    public void verifyArtist(Long artistId, boolean verify) {
        try {
            Long adminId = getCurrentAdminId();
            adminEJB.verifyArtist(artistId, verify, adminId);
            addInfo(verify ? "Artist verified." : "Artist unverified.");
            loadArtists();
        } catch (Exception ex) {
            addError("Failed to change verification: " + ex.getMessage());
        }
    }

    public void toggleActive(Long artistId, boolean deactivate) {
        try {
            adminEJB.deactivateUser(artistId, deactivate,
                    deactivate ? "Deactivated by admin" : null);
            addInfo(deactivate ? "Artist deactivated." : "Artist activated.");
            loadArtists();
        } catch (Exception ex) {
            addError("Failed to change active state: " + ex.getMessage());
        }
    }

    public void deleteArtist(Long artistId) {
        try {
            adminEJB.deleteUser(artistId);
            addInfo("Artist deleted.");
            loadArtists();
        } catch (Exception ex) {
            addError("Failed to delete: " + ex.getMessage());
        }
    }

    private Long getCurrentAdminId() {
        try {
            UserSessionBean usb = (UserSessionBean) FacesContext.getCurrentInstance()
                    .getApplication()
                    .evaluateExpressionGet(FacesContext.getCurrentInstance(),
                            "#{userSessionBean}", UserSessionBean.class);
            return usb != null ? usb.getUserId() : null;
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

    // Getters / Setters
    public List<AppUser> getArtists() {
        return artists;
    }

    public AppUser getViewingArtist() {
        return viewingArtist;
    }

    public void setViewingArtist(AppUser viewingArtist) {
        this.viewingArtist = viewingArtist;
    }

    public Long getArtistToDeleteId() {
        return artistToDeleteId;
    }

    public void setArtistToDeleteId(Long artistToDeleteId) {
        this.artistToDeleteId = artistToDeleteId;
    }
}
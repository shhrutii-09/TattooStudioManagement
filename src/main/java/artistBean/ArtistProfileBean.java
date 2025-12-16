/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package artistBean;

import artistDTO.ArtistProfileeeDTO;
import ejb.ArtistEJBLocal;
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
public class ArtistProfileBean implements Serializable {

    @EJB
    private ArtistEJBLocal artistEJB;

    private ArtistProfileeeDTO profile = new ArtistProfileeeDTO();

    private String oldPassword;
    private String newPassword;

    // ---------------- INIT ----------------
    @PostConstruct
    public void init() {
        FacesContext fc = FacesContext.getCurrentInstance();
        AppUser user = (AppUser) fc.getExternalContext()
                                  .getSessionMap()
                                  .get("loggedInUser");

        if (user == null) {
            fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_WARN,
                    "Session expired",
                    "Please login again."
            ));
            return;
        }

        // Pass the session user ID to load the correct profile
        ArtistProfileeeDTO dto = artistEJB.getArtistProfile(user.getUserId());
        if (dto != null) {
            this.profile = dto;
        }
    }

    // ---------------- SAVE PROFILE ----------------
    public void saveDetails() {
        FacesContext fc = FacesContext.getCurrentInstance();

        // The EJB will ignore the ID inside 'profile' and use the session ID
        boolean ok = artistEJB.updateArtistDetails(profile);

        if (ok) {
            fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Profile updated",
                    "Your profile details were saved."
            ));
        } else {
            fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Update failed",
                    "Please try again."
            ));
        }
    }

    // ---------------- PASSWORD CHANGE (Security Fix Here) ----------------
    public void updatePassword() {
        FacesContext fc = FacesContext.getCurrentInstance();
        
        // SECURITY FIX: get the User ID from the Session, NOT the profile DTO.
        // Even better: modify your EJB to not require the ID at all (get it from session internally),
        // but for now, we ensure we send the session ID here.
        AppUser user = (AppUser) fc.getExternalContext().getSessionMap().get("loggedInUser");
        
        if (user == null) {
             fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Session expired"));
             return;
        }

        boolean ok = artistEJB.changeArtistPassword(
                user.getUserId(), // USE SESSION ID, NOT profile.getUserId()
                oldPassword,
                newPassword
        );

        if (ok) {
            fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Password changed",
                    "Please use your new password next login."
            ));
            oldPassword = "";
            newPassword = "";
        } else {
            fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Password change failed",
                    "Old password incorrect."
            ));
        }
    }

    // ---------------- EXPERIENCE ----------------
    public void saveExperience() {
        FacesContext fc = FacesContext.getCurrentInstance();

        // The EJB uses the session ID, so this is safe now
        boolean ok = artistEJB.saveOrUpdateExperience(profile);

        if (ok) {
            fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Experience updated",
                    "Your experience details were saved."
            ));
        } else {
            fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Update failed",
                    "Could not save experience."
            ));
        }
    }

    // ---------------- GETTERS / SETTERS ----------------
    public ArtistProfileeeDTO getProfile() { return profile; }
    public void setProfile(ArtistProfileeeDTO profile) { this.profile = profile; }

    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public ArtistEJBLocal getArtistEJB() {
        return artistEJB;
    }

    public void setArtistEJB(ArtistEJBLocal artistEJB) {
        this.artistEJB = artistEJB;
    }
    
}
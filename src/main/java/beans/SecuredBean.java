package beans;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.Serializable;

public abstract class SecuredBean implements Serializable {
    
    @Inject
    protected UserSessionBean sessionBean;
    
    protected void checkAdminAccess() throws IOException {
        if (!sessionBean.isAdmin()) {
            redirectToHomeWithError("Access denied. Admin privileges required.");
        }
    }
    
    protected void checkArtistAccess() throws IOException {
        if (!sessionBean.isArtist()) {
            redirectToHomeWithError("Access denied. Artist privileges required.");
        }
    }
    
    protected void checkLoggedIn() throws IOException {
        if (!sessionBean.isLoggedIn()) {
            redirectToLogin("Please login to access this page.");
        }
    }
    
    protected void checkArtistVerified() throws IOException {
        if (sessionBean.isArtist() && !sessionBean.isArtistVerified()) {
            redirectToPending("Artist profile not verified yet.");
        }
    }
    
    protected void redirectToHomeWithError(String message) throws IOException {
        FacesContext.getCurrentInstance()
            .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
        FacesContext.getCurrentInstance()
            .getExternalContext()
            .redirect("/web/client/home.xhtml");
    }
    
    protected void redirectToLogin(String message) throws IOException {
        FacesContext.getCurrentInstance()
            .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, message, null));
        FacesContext.getCurrentInstance()
            .getExternalContext()
            .redirect("/web/login.xhtml");
    }
    
    protected void redirectToPending(String message) throws IOException {
        FacesContext.getCurrentInstance()
            .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, message, null));
        FacesContext.getCurrentInstance()
            .getExternalContext()
            .redirect("/web/artist/pending.xhtml");
    }
}
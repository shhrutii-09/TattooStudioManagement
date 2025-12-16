package mbclient;

import beans.UserSessionBean;
import ejb.ClientEJBLocal;
import entities.AppUser;
import entities.TattooDesign;
import entities.DesignComment;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@ViewScoped
public class DesignDetailBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private ClientEJBLocal clientEJB;

    @Inject
    private UserSessionBean userSessionBean;

    // ================= CORE =================
    private Long designId;                 
    private TattooDesign design;
    private AppUser artist;

    // ================= LISTS =================
    private List<TattooDesign> relatedDesigns = new ArrayList<>();
    private List<DesignComment> designComments = new ArrayList<>();

    // ================= LIKE / FAV =================
    private boolean liked;
    private boolean favourited;
    private Long likeCount = 0L;
    private Long commentCount = 0L;

    // ================= ARTIST =================
    private Double artistAverageRating = 0.0;
    private Long artistTotalReviews = 0L;

    // ================= NEW COMMENT =================
    private String newCommentText;

    // =====================================================
    // LOAD DESIGN METHOD
    // =====================================================
    public void loadDesign() {
    if (FacesContext.getCurrentInstance().isPostback()) {
        return;
    }

    if (designId == null) return;

    design = clientEJB.findDesignById(designId);
    if (design == null) return;

    artist = design.getArtist();
    Long clientId = getCurrentClientId();

    relatedDesigns = clientEJB.getRelatedDesigns(design.getStyle(), artist.getUserId());
    designComments = clientEJB.getCommentsForDesign(designId);
    commentCount = (long) designComments.size();

    if (clientId != null) {
        // FIXED: Correct parameter order - clientId first, then designId
        liked = clientEJB.isDesignLikedByClient(clientId, designId);
        favourited = clientEJB.isDesignFavouritedByClient(clientId, designId);
    }

    likeCount = clientEJB.getDesignLikeCount(designId);

    artistAverageRating = clientEJB.getArtistAverageRating(artist.getUserId());
    artistTotalReviews = clientEJB.getArtistTotalReviews(artist.getUserId());
}
    
    // ================= ACTION METHODS =================
 public void toggleLike() {
    Long clientId = getCurrentClientId();
    if (clientId == null) {
        addErrorMessage("Please login to like designs");
        return;
    }

    try {
        // Use the existing toggle method
        liked = clientEJB.toggleDesignLike(clientId, designId);
        
        // Update the count
        likeCount = clientEJB.getDesignLikeCount(designId);
        
        addInfoMessage(liked ? "Design liked" : "Design unliked");
    } catch (Exception e) {
        e.printStackTrace();
        addErrorMessage("Error updating like: " + e.getMessage());
    }
}

public void toggleFavourite() {
    Long clientId = getCurrentClientId();
    if (clientId == null) {
        addErrorMessage("Please login to add favourites");
        return;
    }

    try {
        // Use the existing toggle method
        favourited = clientEJB.toggleFavouriteDesign(clientId, designId);
        
        addInfoMessage(favourited ? "Added to save" : "Removed from save");
    } catch (Exception e) {
        e.printStackTrace();
        addErrorMessage("Error updating favourite: " + e.getMessage());
    }
}
    public void postComment() {
        Long clientId = getCurrentClientId();
        if (clientId == null) {
            addErrorMessage("Please login to post comments");
            return;
        }

        if (newCommentText == null || newCommentText.trim().isEmpty()) {
            addErrorMessage("Please enter a comment");
            return;
        }

        try {
            // Add the comment
            clientEJB.addComment(designId, clientId, newCommentText.trim());
            
            // Refresh comments
            designComments = clientEJB.getCommentsForDesign(designId);
            commentCount = (long) designComments.size();
            
            // Clear form
            newCommentText = null;
            
            addInfoMessage("Comment posted successfully!");
            
        } catch (Exception e) {
            addErrorMessage("Error posting comment: " + e.getMessage());
        }
    }

    // ================= HELPER METHODS =================
    private Long getCurrentClientId() {
        return (userSessionBean != null && userSessionBean.isClient()) ? userSessionBean.getUserId() : null;
    }

    public boolean isClientLoggedIn() {
        return userSessionBean != null && userSessionBean.isClient();
    }

    private void addErrorMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
    }

    private void addInfoMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, message, null));
    }

    // ================= GETTERS & SETTERS =================
    public Long getDesignId() { return designId; }
    public void setDesignId(Long designId) { this.designId = designId; }

    public TattooDesign getDesign() { return design; }
    public boolean isDesignLoaded() { return design != null; }

    public AppUser getArtist() { return artist; }

    public List<TattooDesign> getRelatedDesigns() { return relatedDesigns; }
    public List<DesignComment> getDesignComments() { return designComments; }

    public boolean isLiked() { return liked; }
    public boolean isFavourited() { return favourited; }
    public Long getLikeCount() { return likeCount; }
    public Long getCommentCount() { return commentCount; }

    public Double getArtistAverageRating() { 
        return artistAverageRating != null ? Math.round(artistAverageRating * 10.0) / 10.0 : 0.0; 
    }
    
    public Long getArtistTotalReviews() { return artistTotalReviews; }

    public String getNewCommentText() { return newCommentText; }
    public void setNewCommentText(String newCommentText) { this.newCommentText = newCommentText; }

    public String getDesignImageUrl() {
        if (design != null && design.getImagePath() != null) {
            return design.getImagePath();
        }
        return "/resources/images/default-design.jpg";
    }
}
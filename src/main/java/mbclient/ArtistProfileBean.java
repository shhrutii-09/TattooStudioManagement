package mbclient;

import clientDTO.ArtistProfileDTO;
import clientDTO.ArtistDesignDTO;
import ejb.ClientEJBLocal;
import entities.AppUser;
import entities.TattooDesign;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("clientArtistProfileBean")
@ViewScoped
public class ArtistProfileBean implements Serializable {

    @EJB
    private ClientEJBLocal clientEJB;

    private Long artistId;
    private ArtistProfileDTO artist;
    private boolean loaded;

    // pagination
    private int offset = 0;
    private static final int PAGE_SIZE = 6;
    private boolean hasMoreDesigns = true;

    // review (✔ DOUBLE, matches entity)
    private Double reviewRating;
    private String reviewComment;

    // ================= LOAD ARTIST =================
    public void loadArtist() {

        if (FacesContext.getCurrentInstance().isPostback()) return;
        if (artistId == null) return;

        AppUser a = clientEJB.getArtistInfo(artistId);
        if (a == null || !a.isActive()) return;

        artist = new ArtistProfileDTO();

        artist.setArtistId(a.getUserId());
        artist.setFullName(a.getFullName());
        artist.setEmail(a.getEmail());
        artist.setPhone(a.getPhone());

        if (a.getExperience() != null) {
            artist.setYearsExperience(a.getExperience().getYearsExperience());
        }

        artist.setAverageRating(
            defaultDouble(clientEJB.getArtistAverageRating(artistId))
        );
        artist.setTotalReviews(
            defaultLong(clientEJB.getArtistTotalReviews(artistId))
        );

        loadMoreDesigns();
        loaded = true;
    }

    // ================= LOAD MORE DESIGNS =================
    public void loadMoreDesigns() {

        if (artist == null || !hasMoreDesigns) return;

        List<TattooDesign> designs =
            clientEJB.getDesignsByArtist(artistId, offset, PAGE_SIZE);

        if (designs == null || designs.isEmpty()) {
            hasMoreDesigns = false;
            return;
        }

        if (artist.getDesigns() == null) {
            artist.setDesigns(new ArrayList<>());
        }

        for (TattooDesign d : designs) {
            ArtistDesignDTO dto = new ArtistDesignDTO();
            dto.setDesignId(d.getDesignId());
            dto.setTitle(d.getTitle());
            dto.setImagePath(d.getImagePath());
            dto.setStyle(d.getStyle());
            artist.getDesigns().add(dto);
        }

        offset += PAGE_SIZE;
    }

    // ================= SUBMIT REVIEW =================
    public void submitReview() {

        if (reviewRating == null || reviewRating < 1 || reviewRating > 5) {
            addMessage("Rating must be between 1 and 5");
            return;
        }

        try {
            clientEJB.addReview(
                artistId,
                getLoggedInUserId(),
                reviewRating,
                reviewComment
            );

            addMessage("Review submitted successfully");

            artist.setAverageRating(
                clientEJB.getArtistAverageRating(artistId)
            );
            artist.setTotalReviews(
                clientEJB.getArtistTotalReviews(artistId)
            );

            reviewRating = null;
            reviewComment = null;

        } catch (Exception e) {
            addMessage("You have already reviewed this artist");
        }
    }

    // ================= HELPERS =================
    private Long getLoggedInUserId() {
        return (Long) FacesContext.getCurrentInstance()
                .getExternalContext()
                .getSessionMap()
                .get("userId");
    }

    private Double defaultDouble(Double d) {
        return d != null ? d : 0.0;
    }

    private Long defaultLong(Long l) {
        return l != null ? l : 0L;
    }

    private void addMessage(String msg) {
        FacesContext.getCurrentInstance().addMessage(
            null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null)
        );
    }

    // ================= GETTERS / SETTERS =================
    public Long getArtistId() { return artistId; }
    public void setArtistId(Long artistId) { this.artistId = artistId; }

    public ArtistProfileDTO getArtist() { return artist; }
    public boolean isLoaded() { return loaded; }

    public boolean isHasMoreDesigns() { return hasMoreDesigns; }

    public Double getReviewRating() { return reviewRating; }
    public void setReviewRating(Double reviewRating) {
        this.reviewRating = reviewRating;
    }

    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }
}

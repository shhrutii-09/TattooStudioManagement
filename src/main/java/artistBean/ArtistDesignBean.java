package artistBean;

import artistDTO.DesignCreationDTO;
import artistDTO.DesignListingDTO;
import beans.UserSessionBean;
import ejb.ArtistEJBLocal;
import entities.DesignComment;
import entities.TattooDesign;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.events.Comment;

@Named("artistDesignBean")
@ViewScoped
public class ArtistDesignBean implements Serializable {

    @EJB
    private ArtistEJBLocal artistEJB;

    @Inject
    private UserSessionBean userSession;   // ✅ Inject session bean

    private List<DesignListingDTO> designs;
    private DesignCreationDTO currentDesign;

    private boolean showForm = false;
    private boolean isEditMode = false;

    private Long loggedInArtistId;
    
    // Used in the Reviews Section (Line 147 in design.xhtml)
private List<DesignComment> comments = new ArrayList<>();
// Used in the Related Designs Section (Line 173 in design.xhtml)
private List<TattooDesign> relatedDesigns = new ArrayList<>();

    @PostConstruct
    public void init() {
        // ✅ Get logged-in artist ID from session bean
        if (userSession != null && userSession.isLoggedIn() && userSession.isArtist()) {
            loggedInArtistId = userSession.getUserId();
        } else {
            loggedInArtistId = null;
        }

        designs = new ArrayList<>();
        loadDesigns();
    }

    // ✅ Load only this artist's designs
    public void loadDesigns() {
        designs.clear();

        if (loggedInArtistId == null) {
            return;
        }

        List<TattooDesign> entities = artistEJB.getArtistDesigns(loggedInArtistId, 0, 100);

        for (TattooDesign entity : entities) {
            DesignListingDTO dto = new DesignListingDTO();
            dto.setDesignId(entity.getDesignId());
            dto.setTitle(entity.getTitle());
            dto.setDescription(entity.getDescription());
            dto.setStyle(entity.getStyle());
            dto.setPrice(entity.getPrice());
            dto.setImagePath(entity.getImagePath());

            dto.setBanned(Boolean.TRUE.equals(entity.getIsBanned()));
            dto.setBannedReason(entity.getBannedReason());

            dto.setTotalLikes(entity.getLikes() != null ? entity.getLikes().size() : 0);
            dto.setTotalFavourites(entity.getFavourites() != null ? entity.getFavourites().size() : 0);

            dto.setUploadedAt(entity.getUploadedAt());

            designs.add(dto);
        }
    }

    // ✅ Prepare Create
    public void prepareCreate() {
        this.currentDesign = new DesignCreationDTO();
        this.isEditMode = false;
        this.showForm = true;
    }

    // ✅ Prepare Edit
    public void prepareEdit(DesignListingDTO selected) {
        TattooDesign entity = artistEJB.getDesignById(selected.getDesignId());

        if (entity == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Design not found.");
            return;
        }

        this.currentDesign = new DesignCreationDTO();
        currentDesign.setDesignId(entity.getDesignId());
        currentDesign.setTitle(entity.getTitle());
        currentDesign.setDescription(entity.getDescription());
        currentDesign.setStyle(entity.getStyle());
        currentDesign.setPrice(entity.getPrice());
        currentDesign.setImageUrl(entity.getImagePath()); // ✅ DTO updated

        this.isEditMode = true;
        this.showForm = true;
    }

    // ✅ Save (Create or Update)
    public void saveDesign() {
        try {
            if (isEditMode) {
                // UPDATE
                TattooDesign entity = artistEJB.getDesignById(currentDesign.getDesignId());

                if (entity == null) {
                    addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Design not found.");
                    return;
                }

                entity.setTitle(currentDesign.getTitle());
                entity.setDescription(currentDesign.getDescription());
                entity.setStyle(currentDesign.getStyle());
                entity.setPrice(currentDesign.getPrice());
                entity.setImagePath(currentDesign.getImageUrl()); // ✅ Updated

                artistEJB.updateDesign(entity.getDesignId(), entity);
                addMessage(FacesMessage.SEVERITY_INFO, "Success", "Design updated successfully.");

            } else {
                // CREATE
                TattooDesign newDesign = new TattooDesign();
                newDesign.setTitle(currentDesign.getTitle());
                newDesign.setDescription(currentDesign.getDescription());
                newDesign.setStyle(currentDesign.getStyle());
                newDesign.setPrice(currentDesign.getPrice());
                newDesign.setImagePath(currentDesign.getImageUrl()); // ✅ Updated

                artistEJB.addDesign(loggedInArtistId, newDesign);
                addMessage(FacesMessage.SEVERITY_INFO, "Success", "Design created successfully.");
            }

            loadDesigns();
            cancelForm();

        } catch (Exception e) {
            e.printStackTrace();
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Unexpected error occurred.");
        }
    }
public void cancelForm() {
    this.showForm = false;
    this.currentDesign = null;
    this.isEditMode = false;
}
    // ✅ Soft Delete
    public void deleteDesign(Long designId) {
        try {
            artistEJB.deleteDesign(designId, loggedInArtistId);
            designs.removeIf(d -> d.getDesignId().equals(designId));
            addMessage(FacesMessage.SEVERITY_INFO, "Deleted", "Design removed successfully.");
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Could not delete design.");
        }
    }

    // ✅ Utility
    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, detail));
    }

    // ✅ Getters
    public List<DesignListingDTO> getDesigns() { return designs; }
    public DesignCreationDTO getCurrentDesign() { return currentDesign; }
    public boolean isShowForm() { return showForm; }
    public boolean isEditMode() { return isEditMode; }
    public void setEditMode(boolean editMode) { this.isEditMode = editMode; }
}
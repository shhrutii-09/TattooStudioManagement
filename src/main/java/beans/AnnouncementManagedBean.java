package beans;

import dto.AnnouncementDTO;
import dto.AnnouncementFilterDTO;
import ejb.AdminEJBLocal;
import entities.Announcement;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.primefaces.PrimeFaces;

@Named("announcementMB")
@ViewScoped
public class AnnouncementManagedBean implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(AnnouncementManagedBean.class.getName());

    @EJB
    private AdminEJBLocal adminEJB;

    private List<AnnouncementDTO> announcements;
    private List<AnnouncementDTO> filteredAnnouncements;
    private AnnouncementDTO currentAnnouncement;
    private AnnouncementDTO selectedForDelete;
    private AnnouncementFilterDTO filter = new AnnouncementFilterDTO();

    private boolean creatingNew;
    private boolean editing;

    private final List<String> VALID_ROLES = Arrays.asList("ALL", "CLIENT", "ARTIST");

    @PostConstruct
    public void init() {
        this.announcements = new ArrayList<>();
        loadAnnouncements();
    }

    public void loadAnnouncements() {
        try {
            List<Announcement> entities = adminEJB.listAllAnnouncements();
            this.announcements = entities.stream()
                    .map(this::convertToDTO)
                    .collect(java.util.stream.Collectors.toList());
            this.filteredAnnouncements = null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading announcements.", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to load announcements.");
        }
    }

    public void createNew() {
    this.creatingNew = true;
    this.editing = false;
    this.currentAnnouncement = new AnnouncementDTO();
    this.currentAnnouncement.setTargetRole("ALL"); // default value
}


    public void prepareEdit(AnnouncementDTO ann) {
        if (ann == null || ann.getAnnouncementId() == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Invalid announcement selected for edit.");
            return;
        }
        try {
            this.creatingNew = false;
            this.editing = true;

            // fetch full entity to ensure message and postedBy are available
            Announcement full = adminEJB.getAnnouncementById(ann.getAnnouncementId());
            this.currentAnnouncement = convertToDTO(full);

            // if DB has an unsupported role (e.g. ADMIN) map to ALL
            if (!isValidRole(this.currentAnnouncement.getTargetRole())) {
                this.currentAnnouncement.setTargetRole("ALL");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error preparing edit for announcement id=" + ann.getAnnouncementId(), e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Could not load announcement for edit.");
        }
    }

    public void prepareView(AnnouncementDTO ann) {
        if (ann == null || ann.getAnnouncementId() == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Invalid announcement selected.");
            return;
        }
        try {
            Announcement fullEntity = adminEJB.getAnnouncementById(ann.getAnnouncementId());
            this.currentAnnouncement = convertToDTO(fullEntity);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading announcement for view: " + ann.getAnnouncementId(), e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Could not load announcement details.");
        }
    }

    public void save() {
        try {
            if (currentAnnouncement == null) {
                addMessage(FacesMessage.SEVERITY_WARN, "Validation Error", "Announcement data missing.");
                FacesContext.getCurrentInstance().validationFailed();
                return;
            }
            if (currentAnnouncement.getTitle() == null || currentAnnouncement.getTitle().trim().isEmpty() ||
                currentAnnouncement.getMessage() == null || currentAnnouncement.getMessage().trim().isEmpty() ||
                currentAnnouncement.getTargetRole() == null || currentAnnouncement.getTargetRole().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_WARN, "Validation Error", "Title, Message and Target Audience are required.");
                FacesContext.getCurrentInstance().validationFailed();
                return;
            }

            // normalize role
            if (!isValidRole(currentAnnouncement.getTargetRole())) {
                currentAnnouncement.setTargetRole("ALL");
            }

            if (creatingNew) {
                Long postedByAdminId = getLoggedInAdminId();
                if (postedByAdminId == null) {
                    throw new IllegalStateException("Admin ID not found for creation.");
                }
                adminEJB.createAnnouncement(
                        postedByAdminId,
                        currentAnnouncement.getTitle(),
                        currentAnnouncement.getMessage(),
                        currentAnnouncement.getTargetRole()
                );
                addMessage(FacesMessage.SEVERITY_INFO, "Success", "Announcement created successfully.");
            } else if (editing) {
                if (currentAnnouncement.getAnnouncementId() == null) {
                    addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Cannot update: missing announcement id.");
                    return;
                }
                adminEJB.updateAnnouncement(
                        currentAnnouncement.getAnnouncementId(),
                        currentAnnouncement.getTitle(),
                        currentAnnouncement.getMessage(),
                        currentAnnouncement.getTargetRole()
                );
                addMessage(FacesMessage.SEVERITY_INFO, "Success", "Announcement updated successfully.");
            } else {
                if (currentAnnouncement.getAnnouncementId() != null) {
                    adminEJB.updateAnnouncement(
                            currentAnnouncement.getAnnouncementId(),
                            currentAnnouncement.getTitle(),
                            currentAnnouncement.getMessage(),
                            currentAnnouncement.getTargetRole()
                    );
                    addMessage(FacesMessage.SEVERITY_INFO, "Success", "Announcement updated successfully.");
                } else {
                    addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Unknown save action.");
                }
            }

            // reload and clear state
            // After saving
this.creatingNew = false;
this.editing = false;
this.currentAnnouncement = null;
PrimeFaces.current().ajax().update("announcementsForm:announcementDialog");


        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving announcement", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to save announcement: " + e.getMessage());
            FacesContext.getCurrentInstance().validationFailed();
        }
    }

    // askDelete called from the row -> shows confirm dialog (XHTML triggers the dialog)
    public void askDelete(AnnouncementDTO ann) {
        this.selectedForDelete = ann;
    }

    // confirmDelete called by confirm dialog Yes button
    public void confirmDelete() {
        if (selectedForDelete == null || selectedForDelete.getAnnouncementId() == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "No announcement selected to delete.");
            return;
        }
        try {
            adminEJB.deleteAnnouncement(selectedForDelete.getAnnouncementId());
            addMessage(FacesMessage.SEVERITY_INFO, "Success", "Announcement deleted successfully.");
            loadAnnouncements();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting announcement: " + selectedForDelete.getAnnouncementId(), e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to delete announcement: " + e.getMessage());
        } finally {
            selectedForDelete = null;
        }
    }

    // convenience direct delete (optional) - kept for compatibility
    public void delete(AnnouncementDTO ann) {
        if (ann == null || ann.getAnnouncementId() == null) return;
        try {
            adminEJB.deleteAnnouncement(ann.getAnnouncementId());
            addMessage(FacesMessage.SEVERITY_INFO, "Success", "Announcement deleted.");
            loadAnnouncements();
            PrimeFaces.current().ajax().update("announcementsForm:announcementsTable", "announcementsForm:msgs");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting announcement", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to delete announcement: " + e.getMessage());
        }
    }

    private AnnouncementDTO convertToDTO(Announcement entity) {
        if (entity == null) return null;

        String rawRole = entity.getTargetRole();
        String normalizedRole = isValidRole(rawRole) ? rawRole : "ALL";

        AnnouncementDTO dto = new AnnouncementDTO(
                entity.getAnnouncementId(),
                entity.getTitle(),
                entity.getMessage(),
                normalizedRole,
                entity.getPostedAt(),
                entity.getPostedBy() != null ? entity.getPostedBy().getUserId() : null,
                entity.getPostedBy() != null ? entity.getPostedBy().getFullName() : "System"
        );
        return dto;
    }

    private boolean isValidRole(String role) {
        if (role == null) return false;
        return VALID_ROLES.contains(role.toUpperCase());
    }

    // TODO: replace with real logged-in admin id retrieval
    private Long getLoggedInAdminId() {
        return 1L;
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, detail));
    }

    // Getters / setters
    public List<AnnouncementDTO> getAnnouncements() { return announcements; }
    public void setAnnouncements(List<AnnouncementDTO> announcements) { this.announcements = announcements; }
    public List<AnnouncementDTO> getFilteredAnnouncements() { return filteredAnnouncements; }
    public void setFilteredAnnouncements(List<AnnouncementDTO> filteredAnnouncements) { this.filteredAnnouncements = filteredAnnouncements; }
    public AnnouncementDTO getCurrentAnnouncement() { return currentAnnouncement; }
    public void setCurrentAnnouncement(AnnouncementDTO currentAnnouncement) { this.currentAnnouncement = currentAnnouncement; }
    public AnnouncementDTO getSelectedForDelete() { return selectedForDelete; }
    public void setSelectedForDelete(AnnouncementDTO selectedForDelete) { this.selectedForDelete = selectedForDelete; }
    public AnnouncementFilterDTO getFilter() { return filter; }
    public void setFilter(AnnouncementFilterDTO filter) { this.filter = filter; }
    public boolean isCreatingNew() { return creatingNew; }
    public void setCreatingNew(boolean creatingNew) { this.creatingNew = creatingNew; }
    public boolean isEditing() { return editing; }
    public void setEditing(boolean editing) { this.editing = editing; }
}

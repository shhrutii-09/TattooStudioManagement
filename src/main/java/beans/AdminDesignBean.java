package beans;

import ejb.AdminEJBLocal;
import entities.TattooDesign;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

@Named("adminDesignBean")
@ViewScoped
public class AdminDesignBean implements Serializable {

    @Inject
    private AdminEJBLocal adminEJB;

    private List<TattooDesign> designs;
    private Long selectedDesignId;
    private String banReason;
    private TattooDesign selectedDesign;

    @PostConstruct
    public void init() {
        loadDesigns();
    }

    public void loadDesigns() {
        // Load all designs (including banned and removed)
        designs = adminEJB.listDesigns(0, 500);
    }

    public void banDesign() {
        if (selectedDesignId != null) {
            adminEJB.banDesign(selectedDesignId, banReason, 1L); // replace with logged-in admin ID
            loadDesigns();
            banReason = null;
            selectedDesignId = null;
        }
    }

    public void unbanDesign(Long id) {
        adminEJB.unbanDesign(id, 1L);
        loadDesigns();
    }

    public void deleteDesign(Long id) {
        adminEJB.deleteDesign(id);
        loadDesigns();
    }

    // Getters & Setters
    public List<TattooDesign> getDesigns() { return designs; }
    public Long getSelectedDesignId() { return selectedDesignId; }

    public void setSelectedDesignId(Long selectedDesignId) {
        this.selectedDesignId = selectedDesignId;
        if (selectedDesignId != null && designs != null) {
            this.selectedDesign = designs.stream()
                .filter(d -> d.getDesignId().equals(selectedDesignId))
                .findFirst()
                .orElse(null);
        }
    }

    public String getBanReason() { return banReason; }
    public void setBanReason(String banReason) { this.banReason = banReason; }
    public TattooDesign getSelectedDesign() { return selectedDesign; }
}
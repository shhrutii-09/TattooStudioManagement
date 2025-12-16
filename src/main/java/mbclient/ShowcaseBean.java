package mbclient;

import entities.TattooDesign;
import ejb.ClientEJBLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;

@Named("showcaseBean")
@RequestScoped
public class ShowcaseBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private ClientEJBLocal clientEJB;

    private List<TattooDesign> featuredDesigns = Collections.emptyList();
    private List<TattooDesign> popularDesigns = Collections.emptyList();
    private List<TattooDesign> newDesigns = Collections.emptyList();
    private List<String> availableStyles = Collections.emptyList();
    private int totalDesignCount = 0;

    @PostConstruct
    public void init() {
        try {
            // Fetch all types of designs using new EJB methods
            this.featuredDesigns = clientEJB.getFeaturedDesigns(0, 8);
            this.popularDesigns = clientEJB.getPopularDesigns(0, 6);
            this.newDesigns = clientEJB.getNewDesigns(0, 6);
            this.availableStyles = clientEJB.getAvailableStyles();
            
            // Get total count of active designs
            this.totalDesignCount = clientEJB.listDesigns(0, 1000).size();
            
        } catch (Exception e) {
            System.err.println("Error fetching showcase data: " + e.getMessage());
        }
    }

    // Getter for the view - returns featured designs by default
    public List<TattooDesign> getDesigns() {
        return featuredDesigns;
    }
    
    public List<TattooDesign> getFeaturedDesigns() {
        return featuredDesigns.size() > 6 ? featuredDesigns.subList(0, 6) : featuredDesigns;
    }
    
    public List<TattooDesign> getPopularDesigns() {
        return popularDesigns;
    }
    
    public List<TattooDesign> getNewDesigns() {
        return newDesigns;
    }
    
    public List<String> getAvailableStyles() {
        return availableStyles;
    }
    
    public boolean hasDesigns() {
        return !featuredDesigns.isEmpty();
    }
    
    public int getDesignCount() {
        return totalDesignCount;
    }
    
    public int getFeaturedCount() {
        return Math.min(featuredDesigns.size(), 6);
    }
    
    public int getPopularCount() {
        return Math.min(popularDesigns.size(), 6);
    }
}
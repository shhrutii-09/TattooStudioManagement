package mbclient;

import entities.*;
import ejb.ClientEJBLocal;
import beans.UserSessionBean;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;

@Named("clientHomeBean")
@ViewScoped
public class ClientHomeBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private ClientEJBLocal clientEJB;

    @Inject 
    private UserSessionBean userSessionBean;

    // Dashboard Stats
    private Long totalAppointments = 0L;
    private Long upcomingAppointments = 0L;
    private Long totalFavourites = 0L;
    private Long pendingMedicalForms = 0L;
    
    // Recent Data
    private List<Appointment> recentAppointments = Collections.emptyList();
    private List<TattooDesign> recommendedDesigns = Collections.emptyList();
    private List<AppUser> topArtists = Collections.emptyList();
    private List<TattooDesign> trendingDesigns = Collections.emptyList();
    private List<String> availableStyles = Collections.emptyList();

   @PostConstruct
public void init() {
    if (userSessionBean.isLoggedIn() && userSessionBean.isClient()) {
        fetchDashboardData(userSessionBean.getUserId());
    } else {
        loadPublicData();
    }
}


    private void fetchDashboardData(Long clientId) {
        try {
            // 1. Fetch statistics using new EJB methods
            this.totalAppointments = clientEJB.getClientAppointmentCount(clientId);
            this.upcomingAppointments = clientEJB.getUpcomingAppointmentCount(clientId);
            this.totalFavourites = clientEJB.getClientFavouritesCount(clientId);
            this.pendingMedicalForms = clientEJB.getPendingMedicalFormsCount(clientId);

            // 2. Recent appointments (last 4)
            this.recentAppointments = clientEJB.listClientAppointments(clientId, 0, 4);
            
            // 3. Recommended designs (based on user's favorite styles)
            this.recommendedDesigns = clientEJB.getRecommendedDesigns(clientId);

            // 4. Top artists
            this.topArtists = clientEJB.getTopArtists(0, 3);
            
            // 5. Trending designs
            this.trendingDesigns = clientEJB.getTrendingDesigns(0, 6);
            
        } catch (Exception e) {
            System.err.println("Error fetching client dashboard data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadPublicData() {
        try {
            // Load styles for public view
            this.availableStyles = clientEJB.getAvailableStyles();
            
            // Load trending designs for public view
            this.trendingDesigns = clientEJB.getTrendingDesigns(0, 6);
            
            // Load top artists for public view
            this.topArtists = clientEJB.getTopArtists(0, 3);
            
        } catch (Exception e) {
            System.err.println("Error loading public data: " + e.getMessage());
        }
    }
    
    // Helper method for style colors
    public String getStyleClass(String style) {
        if (style == null) return "badge-minimal";
        
        String styleUpper = style.toUpperCase();
        if (styleUpper.contains("TRADITIONAL")) return "badge-traditional";
        if (styleUpper.contains("GEOMETRIC")) return "badge-geometric";
        if (styleUpper.contains("WATERCOLOR") || styleUpper.contains("WATER COLOUR")) return "badge-watercolor";
        if (styleUpper.contains("JAPANESE") || styleUpper.contains("IREZUMI")) return "badge-japanese";
        if (styleUpper.contains("REALISM") || styleUpper.contains("REALISTIC")) return "badge-realistic";
        if (styleUpper.contains("MINIMAL")) return "badge-minimal";
        if (styleUpper.contains("BLACKWORK")) return "badge-geometric";
        return "badge-minimal";
    }
    
    // Helper method for appointment status colors
    public String getStatusClass(String status) {
        if (status == null) return "status-pending";
        
        String statusUpper = status.toUpperCase();
        if (statusUpper.contains("CONFIRMED")) return "status-confirmed";
        if (statusUpper.contains("PENDING")) return "status-pending";
        if (statusUpper.contains("COMPLETED")) return "status-completed";
        if (statusUpper.contains("CANCELLED")) return "status-cancelled";
        return "status-pending";
    }
    
    // Get upcoming appointments
    public List<Appointment> getUpcomingAppointments() {
        if (!userSessionBean.isLoggedIn() || !userSessionBean.isClient()) {
            return Collections.emptyList();
        }
        
        return clientEJB.getUpcomingAppointments(userSessionBean.getUserId(), 0, 3);
    }
    
    // Check if user has upcoming appointments
    public boolean hasUpcomingAppointments() {
        return !getUpcomingAppointments().isEmpty();
    }
    
    // Get artist rating (for display)
    public Double getArtistRating(Long artistId) {
        try {
            return clientEJB.getArtistAverageRating(artistId);
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    // Format price for display
    public String formatPrice(java.math.BigDecimal price) {
        if (price == null) return "Custom";
        return "$" + price.setScale(2, java.math.RoundingMode.HALF_UP);
    }
    
    // Getters
    public Long getTotalAppointments() { return totalAppointments; }
    public Long getUpcomingAppointmentsCount() { return upcomingAppointments; }
    public Long getTotalFavourites() { return totalFavourites; }
    public Long getPendingMedicalForms() { return pendingMedicalForms; }
    public List<Appointment> getRecentAppointments() { return recentAppointments; }
    public List<TattooDesign> getRecommendedDesigns() { return recommendedDesigns; }
    public List<AppUser> getTopArtists() { return topArtists; }
    public List<TattooDesign> getTrendingDesigns() { return trendingDesigns; }
    public List<String> getAvailableStyles() { return availableStyles; }
    
    public boolean isRecentAppointmentsEmpty() {
        return recentAppointments.isEmpty();
    }
    
    public boolean hasRecommendedDesigns() {
        return recommendedDesigns != null && !recommendedDesigns.isEmpty();
    }
    
    public boolean hasTopArtists() {
        return topArtists != null && !topArtists.isEmpty();
    }
    
    public boolean hasTrendingDesigns() {
        return trendingDesigns != null && !trendingDesigns.isEmpty();
    }
}
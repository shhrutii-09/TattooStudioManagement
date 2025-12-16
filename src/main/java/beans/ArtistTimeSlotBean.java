package beans;

import dto.TimeSlotDTO;
import dto.TimeSlotFilterDTO;
import ejb.AdminEJBLocal;
import entities.AppUser;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Named
@ViewScoped
public class ArtistTimeSlotBean implements Serializable {
    
    @EJB
    private AdminEJBLocal adminEJB;
    
    private Long artistId;
    private String artistName;
    private String artistEmail;
    private List<TimeSlotDTO> timeSlots;
    private TimeSlotDTO selectedSlot;
    
    // Filter properties
    private LocalDate startDate;
    private LocalDate endDate;
    private String selectedStatus = "ALL";
    private String blockReason;
    
    // Statistics
    private long totalSlots;
    private long availableSlots;
    private long bookedSlots;
    private long blockedSlots;
    
    @PostConstruct
    public void init() {
        // Get artist ID from flash scope
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Object artistIdObj = facesContext.getExternalContext().getFlash().get("selectedArtistId");
        
        if (artistIdObj != null) {
            this.artistId = Long.parseLong(artistIdObj.toString());
            loadArtistInfo();
            loadTimeSlots();
        } else {
            // Redirect back if no artist selected
            try {
                facesContext.getExternalContext().redirect("artists.xhtml");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private void loadArtistInfo() {
        try {
            AppUser artist = adminEJB.getUserById(artistId);
            if (artist != null) {
                this.artistName = artist.getFullName();
                this.artistEmail = artist.getEmail();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void loadTimeSlots() {
        try {
            TimeSlotFilterDTO filter = new TimeSlotFilterDTO();
            filter.setArtistId(artistId);
            filter.setStartDate(startDate != null ? startDate : LocalDate.now());
            filter.setEndDate(endDate != null ? endDate : LocalDate.now().plusDays(30));
            if (!"ALL".equals(selectedStatus)) {
                filter.setStatus(selectedStatus);
            }
            
            this.timeSlots = adminEJB.getFilteredTimeSlots(filter);
            calculateStatistics();
            
        } catch (Exception e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                    "Failed to load time slots: " + e.getMessage()));
        }
    }
    
    private void calculateStatistics() {
        totalSlots = timeSlots.size();
        availableSlots = timeSlots.stream().filter(s -> "AVAILABLE".equals(s.getStatus())).count();
        bookedSlots = timeSlots.stream().filter(s -> "BOOKED".equals(s.getStatus())).count();
        blockedSlots = timeSlots.stream().filter(s -> "BLOCKED".equals(s.getStatus())).count();
    }
    
    public void clearFilters() {
        startDate = null;
        endDate = null;
        selectedStatus = "ALL";
        loadTimeSlots();
    }
    
    public void blockSlot() {
        try {
            if (selectedSlot != null && blockReason != null && !blockReason.trim().isEmpty()) {
                // Get current admin ID from session
                Long adminId = getCurrentAdminId();
                
                if (adminId != null) {
                    boolean success = adminEJB.blockTimeSlotWithNotification(
                        selectedSlot.getSlotId(), adminId, blockReason);
                    
                    if (success) {
                        FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", 
                                "Time slot blocked successfully"));
                        loadTimeSlots();
                        blockReason = null;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                    "Failed to block slot: " + e.getMessage()));
        }
    }
    
    public void unblockSlot(Integer slotId) {
        try {
            Long adminId = getCurrentAdminId();
            if (adminId != null) {
                boolean success = adminEJB.unblockTimeSlotWithNotification(slotId, adminId);
                
                if (success) {
                    FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", 
                            "Time slot unblocked successfully"));
                    loadTimeSlots();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                    "Failed to unblock slot: " + e.getMessage()));
        }
    }
    
    public String getStatusColor(String status) {
        switch (status) {
            case "AVAILABLE": return "success";
            case "BOOKED": return "info";
            case "BLOCKED": return "warning";
            default: return "secondary";
        }
    }
    
    private Long getCurrentAdminId() {
        // Get admin ID from session (implement based on your auth system)
        FacesContext context = FacesContext.getCurrentInstance();
        Map<String, Object> sessionMap = context.getExternalContext().getSessionMap();
        Object adminIdObj = sessionMap.get("adminId");
        
        if (adminIdObj instanceof Long) {
            return (Long) adminIdObj;
        } else if (adminIdObj instanceof String) {
            try {
                return Long.parseLong((String) adminIdObj);
            } catch (NumberFormatException e) {
                return 1L; // Default admin ID
            }
        }
        return 1L; // Default admin ID
    }
    
    // Getters and Setters
    public Long getArtistId() { return artistId; }
    public void setArtistId(Long artistId) { this.artistId = artistId; }
    
    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }
    
    public String getArtistEmail() { return artistEmail; }
    public void setArtistEmail(String artistEmail) { this.artistEmail = artistEmail; }
    
    public List<TimeSlotDTO> getTimeSlots() { return timeSlots; }
    public void setTimeSlots(List<TimeSlotDTO> timeSlots) { this.timeSlots = timeSlots; }
    
    public TimeSlotDTO getSelectedSlot() { return selectedSlot; }
    public void setSelectedSlot(TimeSlotDTO selectedSlot) { this.selectedSlot = selectedSlot; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public String getSelectedStatus() { return selectedStatus; }
    public void setSelectedStatus(String selectedStatus) { this.selectedStatus = selectedStatus; }
    
    public String getBlockReason() { return blockReason; }
    public void setBlockReason(String blockReason) { this.blockReason = blockReason; }
    
    public long getTotalSlots() { return totalSlots; }
    public void setTotalSlots(long totalSlots) { this.totalSlots = totalSlots; }
    
    public long getAvailableSlots() { return availableSlots; }
    public void setAvailableSlots(long availableSlots) { this.availableSlots = availableSlots; }
    
    public long getBookedSlots() { return bookedSlots; }
    public void setBookedSlots(long bookedSlots) { this.bookedSlots = bookedSlots; }
    
    public long getBlockedSlots() { return blockedSlots; }
    public void setBlockedSlots(long blockedSlots) { this.blockedSlots = blockedSlots; }
}
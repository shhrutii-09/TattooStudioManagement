// file: managed/TimeSlotManagedBean.java
package beans;

import dto.TimeSlotDTO;
import dto.TimeSlotFilterDTO;
import ejb.AdminEJBLocal;
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
public class TimeSlotBean implements Serializable {
    
    @EJB
    private AdminEJBLocal adminEJB;
    
    private TimeSlotFilterDTO filter = new TimeSlotFilterDTO();
    private List<TimeSlotDTO> timeSlots;
    private Long totalTimeSlots;
    private TimeSlotDTO selectedSlot;
    
    private Integer slotIdToBlock;
    private String blockReason;
    
    // For new slot creation (if needed)
    private Long artistIdForNewSlot;
    private LocalDate slotDate;
    private String startTime;
    private String endTime;
    
    @PostConstruct
    public void init() {
        filter.setStartDate(LocalDate.now());
        filter.setEndDate(LocalDate.now().plusDays(7));
        filter.setStatus("ALL");
        searchTimeSlots();
    }
    
    public void searchTimeSlots() {
    try {
        System.out.println("=== SEARCHING TIME SLOTS ===");
        System.out.println("Filter: " + filter.getArtistId() + 
                         ", Status: " + filter.getStatus() + 
                         ", Start: " + filter.getStartDate() + 
                         ", End: " + filter.getEndDate());
        
        timeSlots = adminEJB.getFilteredTimeSlots(filter);
        System.out.println("Retrieved " + (timeSlots != null ? timeSlots.size() : 0) + " slots");
        
        totalTimeSlots = adminEJB.countFilteredTimeSlots(filter);
        System.out.println("Total count: " + totalTimeSlots);
        
        // Debug: Print first few slots
        if (timeSlots != null && !timeSlots.isEmpty()) {
            for (int i = 0; i < Math.min(timeSlots.size(), 3); i++) {
                TimeSlotDTO slot = timeSlots.get(i);
                System.out.println("Slot[" + i + "]: ID=" + slot.getSlotId() + 
                                 ", Artist=" + slot.getArtistName() + 
                                 ", Status=" + slot.getStatus());
            }
        }
        
    } catch (Exception e) {
        System.err.println("Error in searchTimeSlots: " + e.getMessage());
        e.printStackTrace();
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                "Failed to load time slots: " + e.getMessage()));
    }
}
    
    public void testTimeSlotQuery() {
    try {
        // Simple test with proper messaging
        List<TimeSlotDTO> results = adminEJB.getFilteredTimeSlots(filter);
        
        String message = results.isEmpty() 
            ? "No time slots found with current filters" 
            : "Found " + results.size() + " time slots";
            
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Query Successful", message));
        
    } catch (Exception e) {
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Query Failed", 
                "Error: " + e.getMessage()));
    }
}
    
public void loadAllSlots() {
    try {
        // Load all slots for the next 30 days
        filter.setStartDate(LocalDate.now());
        filter.setEndDate(LocalDate.now().plusDays(30));
        filter.setStatus("ALL");
        searchTimeSlots();
        
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Loaded", 
                "Loaded time slots for the next 30 days"));
        
    } catch (Exception e) {
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                "Failed to load slots: " + e.getMessage()));
    }
}
    
    public void debugDatabase() {
    try {
        String result = adminEJB.debugTimeSlots();
        
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Debug Results", result));
        
        // Also print to console
        System.out.println("=== DEBUG DATABASE RESULTS ===");
        System.out.println(result);
        
    } catch (Exception e) {
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Debug Failed", e.getMessage()));
        e.printStackTrace();
    }
}
    
    public void clearFilters() {
        filter = new TimeSlotFilterDTO();
        filter.setStartDate(LocalDate.now());
        filter.setEndDate(LocalDate.now().plusDays(7));
        filter.setStatus("ALL");
        searchTimeSlots();
    }
    
    public void blockSlot() {
        try {
            // Get current admin ID from session
            Long adminId = getCurrentAdminId();
            
            if (adminId == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Admin not authenticated"));
                return;
            }
            
            boolean success = adminEJB.blockTimeSlotWithNotification(slotIdToBlock, adminId, blockReason);
            
            if (success) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Time slot blocked and artist notified"));
                
                // Clear form
                slotIdToBlock = null;
                blockReason = null;
                
                // Refresh data
                searchTimeSlots();
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to block time slot"));
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to block time slot: " + e.getMessage()));
        }
    }
    
    public void unblockSlot(Integer slotId) {
        try {
            // Get current admin ID from session
            Long adminId = getCurrentAdminId();
            
            if (adminId == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Admin not authenticated"));
                return;
            }
            
            boolean success = adminEJB.unblockTimeSlotWithNotification(slotId, adminId);
            
            if (success) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Time slot unblocked and artist notified"));
                
                // Refresh data
                searchTimeSlots();
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to unblock time slot"));
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to unblock time slot: " + e.getMessage()));
        }
    }
    
    
    
    public void testDebugQuery() {
    try {
        List<TimeSlotDTO> results = adminEJB.debugTimeSlotQuery();
        
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Debug Results", 
                "Found " + results.size() + " time slots in debug query"));
        
        // Update your table with debug results
        this.timeSlots = results;
        this.totalTimeSlots = (long) results.size();
        
    } catch (Exception e) {
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Debug Failed", 
                "Error: " + e.getMessage()));
        e.printStackTrace();
    }
}
    
    public void showBlockDialog(TimeSlotDTO slot) {
        this.selectedSlot = slot;
        this.slotIdToBlock = slot.getSlotId();
        this.blockReason = "";
    }
    
    public String getStatusColor(String status) {
        switch (status) {
            case "AVAILABLE": return "success";
            case "BOOKED": return "info";
            case "BLOCKED": return "warning";
            case "PENDING_APPOINTMENT": return "secondary";
            default: return "default";
        }
    }
    
    public String getStatusIcon(String status) {
        switch (status) {
            case "AVAILABLE": return "pi pi-check-circle";
            case "BOOKED": return "pi pi-calendar";
            case "BLOCKED": return "pi pi-ban";
            case "PENDING_APPOINTMENT": return "pi pi-clock";
            default: return "pi pi-circle";
        }
    }
    
    private Long getCurrentAdminId() {
        // This should get the admin ID from session/security context
        // You'll need to implement this based on your authentication system
        FacesContext context = FacesContext.getCurrentInstance();
        Map<String, Object> sessionMap = context.getExternalContext().getSessionMap();
        Object adminIdObj = sessionMap.get("adminId");
        
        if (adminIdObj instanceof Long) {
            return (Long) adminIdObj;
        } else if (adminIdObj instanceof String) {
            try {
                return Long.parseLong((String) adminIdObj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    // Getters and Setters
    public TimeSlotFilterDTO getFilter() { return filter; }
    public void setFilter(TimeSlotFilterDTO filter) { this.filter = filter; }
    
    public List<TimeSlotDTO> getTimeSlots() { return timeSlots; }
    public void setTimeSlots(List<TimeSlotDTO> timeSlots) { this.timeSlots = timeSlots; }
    
    public Long getTotalTimeSlots() { return totalTimeSlots; }
    public void setTotalTimeSlots(Long totalTimeSlots) { this.totalTimeSlots = totalTimeSlots; }
    
    public TimeSlotDTO getSelectedSlot() { return selectedSlot; }
    public void setSelectedSlot(TimeSlotDTO selectedSlot) { this.selectedSlot = selectedSlot; }
    
    public Integer getSlotIdToBlock() { return slotIdToBlock; }
    public void setSlotIdToBlock(Integer slotIdToBlock) { this.slotIdToBlock = slotIdToBlock; }
    
    public String getBlockReason() { return blockReason; }
    public void setBlockReason(String blockReason) { this.blockReason = blockReason; }
    
    public Long getArtistIdForNewSlot() { return artistIdForNewSlot; }
    public void setArtistIdForNewSlot(Long artistIdForNewSlot) { this.artistIdForNewSlot = artistIdForNewSlot; }
    
    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }
    
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}
// file: managedbeans/AdminAppointmentBean.java - REFACTORED
package beans;

import dto.AppointmentDTO;
import dto.AppointmentFilterDTO;
import ejb.AdminEJBLocal;
import entities.Appointment;
import entities.TimeSlot;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Named
@SessionScoped
public class AdminAppointmentBean implements Serializable {
    
    private static final Logger LOGGER = Logger.getLogger(AdminAppointmentBean.class.getName());
    private static final String INFO_TITLE = "Info";
    private static final String SUCCESS_TITLE = "Success";
    private static final String ERROR_TITLE = "Error";
    private static final String WARNING_TITLE = "Warning";
    
    @EJB
    private AdminEJBLocal adminEJB;
    
    // Data
    private List<AppointmentDTO> appointments;
    private AppointmentDTO selectedAppointment;
    private AppointmentFilterDTO filter;
    private List<TimeSlot> availableSlots;
    
    // Form fields
    private String newStatus;
    private String cancellationReason;
    private Integer selectedSlotId;
    
    // Statistics
    private Map<String, Long> appointmentStats;
    
    // UI State
    private boolean showCancellationDialog = false;
    private boolean showSlotAssignmentDialog = false;
    private boolean showDetailsDialog = false;
    
    // Pagination
    private int currentPage = 0;
    private int pageSize = 10;
    private long totalItems = 0;
    
    @PostConstruct
    public void init() {
        initializeFilter();
        initializeDataStructures();
        loadAllData();
    }
    
private void initializeFilter() {
    filter = new AppointmentFilterDTO();
    filter.setPage(currentPage);
    filter.setSize(pageSize);
    // Don't set dates by default - show ALL appointments regardless of date
    filter.setStartDate(null);
    filter.setEndDate(null);
    filter.setStatus("ALL");
}
    
    private void initializeDataStructures() {
        appointments = new ArrayList<>();
        appointmentStats = new HashMap<>();
        availableSlots = new ArrayList<>();
    }
    
    /**
     * Loads all data: appointments and statistics
     */
    public void loadAllData() {
        loadAppointments();
        loadStatistics();
    }
    
    /**
     * Loads appointments with current filter settings
     */
    public void loadAppointments() {
        try {
            LOGGER.log(Level.INFO, "Loading appointments with filter: {0}", filter);
            
            // Load appointments with pagination
            appointments = adminEJB.getFilteredAppointments(filter);
            totalItems = adminEJB.countFilteredAppointments(filter);
            
            LOGGER.log(Level.INFO, "Loaded {0} appointments, total items: {1}", 
                new Object[]{appointments.size(), totalItems});
            
            if (appointments.isEmpty()) {
                LOGGER.log(Level.WARNING, "No appointments found with current filter");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load appointments", e);
            appointments = new ArrayList<>();
            totalItems = 0;
            showErrorMessage("Failed to load appointments: " + e.getMessage());
        }
    }
    
    /**
     * Loads appointment statistics
     */
    public void loadStatistics() {
        try {
            appointmentStats = adminEJB.getAppointmentStatistics(null, null);
            LOGGER.log(Level.INFO, "Loaded statistics: {0}", appointmentStats);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load statistics", e);
            appointmentStats = new HashMap<>();
        }
    }
    
    /**
     * Performs search with current filter
     */
    public void search() {
        currentPage = 0;
        filter.setPage(currentPage);
        loadAllData();
        showInfoMessage("Found " + totalItems + " appointment(s)");
    }
    
    /**
     * Clears all filters and resets to default
     */
    public void clearFilters() {
        initializeFilter();
        loadAllData();
        showInfoMessage("Filters cleared");
    }
    
    /**
     * Selects an appointment for editing/viewing
     */
    /**
 * Selects an appointment for editing/viewing with null safety
 */
public void selectAppointment(AppointmentDTO appointment) {
    if (appointment == null) {
        this.selectedAppointment = new AppointmentDTO();
        this.newStatus = null;
        this.cancellationReason = null;
        return;
    }
    
    this.selectedAppointment = appointment;
    this.newStatus = appointment.getStatus();
    this.cancellationReason = appointment.getCancellationReason();
    
    // Ensure all fields have defaults if null
    if (this.selectedAppointment.getDesignTitle() == null) {
        this.selectedAppointment.setDesignTitle("No Design Selected");
    }
    if (this.selectedAppointment.getPaymentStatus() == null) {
        this.selectedAppointment.setPaymentStatus("N/A");
    }
}
    
    // In AdminAppointmentBean.java, add this method:
public void testDirectQuery() {
    try {
        System.out.println("=== TEST DIRECT QUERY ===");
        
        // Test 1: Direct entity query
        List<Appointment> directResults = adminEJB.getAllAppointmentsSimple();
        System.out.println("Direct query found: " + directResults.size() + " appointments");
        
        for (int i = 0; i < Math.min(directResults.size(), 5); i++) {
            Appointment a = directResults.get(i);
            System.out.println("Appointment[" + i + "]: ID=" + a.getAppointmentId() + 
                             ", Client=" + (a.getClient() != null ? a.getClient().getFullName() : "null") +
                             ", Status=" + a.getStatus());
        }
        
        // Test 2: DTO query with empty filter
        AppointmentFilterDTO testFilter = new AppointmentFilterDTO();
        testFilter.setPage(0);
        testFilter.setSize(10);
        List<AppointmentDTO> dtoResults = adminEJB.getFilteredAppointments(testFilter);
        System.out.println("DTO query found: " + dtoResults.size() + " appointments");
        
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Test Complete", 
            "Direct: " + directResults.size() + ", DTO: " + dtoResults.size()));
        
    } catch (Exception e) {
        e.printStackTrace();
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Test Failed", e.getMessage()));
    }
}
    
    /**
     * Shows appointment details dialog
     */
    public void showAppointmentDetails(AppointmentDTO appointment) {
    try {
        // Try to get full details
        Appointment fullAppt = adminEJB.getAppointment(appointment.getAppointmentId());
        
        if (fullAppt != null) {
            selectedAppointment = new AppointmentDTO();
            selectedAppointment.setAppointmentId(fullAppt.getAppointmentId());
            selectedAppointment.setStatus(fullAppt.getStatus());
            selectedAppointment.setAppointmentDateTime(fullAppt.getAppointmentDateTime());
            
            if (fullAppt.getClient() != null) {
                selectedAppointment.setClientName(fullAppt.getClient().getFullName());
                selectedAppointment.setClientEmail(fullAppt.getClient().getEmail());
            }
            
            if (fullAppt.getArtist() != null) {
                selectedAppointment.setArtistName(fullAppt.getArtist().getFullName());
            }
            
            if (fullAppt.getDesign() != null) {
                selectedAppointment.setDesignTitle(fullAppt.getDesign().getTitle());
            } else {
                selectedAppointment.setDesignTitle("No Design Selected");
            }
            
            if (fullAppt.getSlot() != null) {
                selectedAppointment.setSlotId(fullAppt.getSlot().getSlotId());
            }
            
            if (fullAppt.getPayment() != null) {
                selectedAppointment.setAmount(fullAppt.getPayment().getAmount());
                selectedAppointment.setPaymentStatus(fullAppt.getPayment().getStatus());
            } else {
                selectedAppointment.setPaymentStatus("N/A");
            }
            
            selectedAppointment.setCancellationReason(fullAppt.getCancellationReason());
            selectedAppointment.setClientNote(fullAppt.getClientNote());
        } else {
            // Fallback
            selectAppointment(appointment);
        }
        
    } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Error loading full details", e);
        selectAppointment(appointment);
    }
    
    showDetailsDialog = true;
}
    
    private AppointmentDTO convertToDTO(Appointment appointment) {
    if (appointment == null) return null;
    
    AppointmentDTO dto = new AppointmentDTO();
    
    // Basic appointment info
    dto.setAppointmentId(appointment.getAppointmentId());
    dto.setStatus(appointment.getStatus());
    dto.setAppointmentDateTime(appointment.getAppointmentDateTime());
    dto.setCancellationReason(appointment.getCancellationReason());
    dto.setClientNote(appointment.getClientNote());
    
    // Client info
    if (appointment.getClient() != null) {
        dto.setClientId(appointment.getClient().getUserId());
        dto.setClientName(appointment.getClient().getFullName());
        dto.setClientEmail(appointment.getClient().getEmail());
    }
    
    // Artist info
    if (appointment.getArtist() != null) {
        dto.setArtistId(appointment.getArtist().getUserId());
        dto.setArtistName(appointment.getArtist().getFullName());
    }
    
    // Design info
    if (appointment.getDesign() != null) {
        dto.setDesignId(appointment.getDesign().getDesignId());
        dto.setDesignTitle(appointment.getDesign().getTitle());
    }
    
    // Slot info
    if (appointment.getSlot() != null) {
        dto.setSlotId(appointment.getSlot().getSlotId());
    }
    
    // Payment info - CRITICAL FIX
    if (appointment.getPayment() != null) {
        dto.setAmount(appointment.getPayment().getAmount());
        dto.setPaymentStatus(appointment.getPayment().getStatus());
    } else {
        dto.setAmount(null);
        dto.setPaymentStatus("N/A");
    }
    
    return dto;
}
    
    /**
     * Shows cancellation dialog for an appointment
     */
    public void showCancelAppointmentDialog(AppointmentDTO appointment) {
        selectAppointment(appointment);
        this.newStatus = "CANCELLED";
        showCancellationDialog = true;
    }
    
    /**
     * Shows slot assignment dialog for an appointment
     */
    public void showAssignSlotDialog(AppointmentDTO appointment) {
        selectAppointment(appointment);
        if (appointment != null && appointment.getArtistId() != null) {
            try {
                availableSlots = adminEJB.getAvailableSlotsForArtist(appointment.getArtistId());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to load available slots", e);
                availableSlots = new ArrayList<>();
                showErrorMessage("Failed to load available time slots");
            }
        }
        showSlotAssignmentDialog = true;
    }
    
    /**
     * Updates appointment status
     */
    public void updateAppointmentStatus() {
        if (selectedAppointment == null || newStatus == null || newStatus.isEmpty()) {
            showErrorMessage("Please select an appointment and status");
            return;
        }
        
        try {
            Long adminId = getCurrentAdminId();
            
            boolean success = adminEJB.updateAppointmentStatus(
                selectedAppointment.getAppointmentId(), 
                newStatus, 
                cancellationReason, 
                adminId
            );
            
            if (success) {
                showSuccessMessage("Appointment #" + selectedAppointment.getAppointmentId() + 
                                 " status changed to " + newStatus);
                resetUpdateForm();
                loadAllData();
            } else {
                showErrorMessage("Failed to update appointment status");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating appointment status", e);
            showErrorMessage("Error updating appointment: " + e.getMessage());
        }
    }
    
    /**
     * Assigns a time slot to appointment
     */
    public void assignSlot() {
        if (selectedAppointment == null || selectedSlotId == null) {
            showErrorMessage("Please select a time slot");
            return;
        }
        
        try {
            adminEJB.assignSlotToAppointment(selectedAppointment.getAppointmentId(), selectedSlotId);
            showSuccessMessage("Time slot assigned to appointment #" + selectedAppointment.getAppointmentId());
            resetSlotForm();
            loadAllData();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to assign slot", e);
            showErrorMessage("Failed to assign slot: " + e.getMessage());
        }
    }
    
    /**
     * Confirms an appointment
     */
    public void confirmAppointment(AppointmentDTO appointment) {
        selectAppointment(appointment);
        newStatus = "CONFIRMED";
        updateAppointmentStatus();
    }
    
    /**
     * Completes an appointment
     */
    public void completeAppointment(AppointmentDTO appointment) {
        selectAppointment(appointment);
        newStatus = "COMPLETED";
        updateAppointmentStatus();
    }
    
    /**
     * Marks appointment as paid
     */
    public void markAsPaid(AppointmentDTO appointment) {
        selectAppointment(appointment);
        newStatus = "PAID";
        updateAppointmentStatus();
    }
    
    /**
     * Changes pagination page
     */
    public void changePage(int page) {
        if (page >= 0 && page < getTotalPages()) {
            currentPage = page;
            filter.setPage(currentPage);
            loadAppointments();
        }
    }
    
    public void debugFilterAndData() {
    try {
        System.out.println("=== DEBUG FILTER AND DATA ===");
        System.out.println("Current Filter:");
        System.out.println("  Status: " + filter.getStatus());
        System.out.println("  Start Date: " + filter.getStartDate());
        System.out.println("  End Date: " + filter.getEndDate());
        
        // Get appointments with current filter
        List<AppointmentDTO> currentResults = adminEJB.getFilteredAppointments(filter);
        System.out.println("Current filter returns: " + currentResults.size() + " appointments");
        
        // Get ALL appointments (no filter)
        AppointmentFilterDTO allFilter = new AppointmentFilterDTO();
        allFilter.setPage(0);
        allFilter.setSize(100);
        allFilter.setStatus("ALL");
        allFilter.setStartDate(null);
        allFilter.setEndDate(null);
        
        List<AppointmentDTO> allResults = adminEJB.getFilteredAppointments(allFilter);
        System.out.println("No filter returns: " + allResults.size() + " appointments");
        
        // Show PENDING appointments
        List<AppointmentDTO> pendingAppts = allResults.stream()
            .filter(a -> "PENDING".equals(a.getStatus()))
            .collect(Collectors.toList());
        
        System.out.println("PENDING appointments in database: " + pendingAppts.size());
        for (AppointmentDTO appt : pendingAppts) {
            System.out.println("  ID: " + appt.getAppointmentId() + 
                             ", Date: " + appt.getAppointmentDateTime() + 
                             ", Client: " + appt.getClientName());
        }
        
        showInfoMessage("Debug: " + pendingAppts.size() + " PENDING appointments found");
        
    } catch (Exception e) {
        e.printStackTrace();
        showErrorMessage("Debug failed: " + e.getMessage());
    }
}
    
    /**
     * Gets current admin ID (implement based on your authentication system)
     */
    private Long getCurrentAdminId() {
        // TODO: Implement actual admin ID retrieval from session/security context
        // For now, return a default or retrieve from user context
        return 1L; 
    }
    
    /**
     * Resets update form fields
     */
    private void resetUpdateForm() {
        selectedAppointment = null;
        newStatus = null;
        cancellationReason = null;
        showCancellationDialog = false;
    }
    
    /**
     * Resets slot assignment form fields
     */
    private void resetSlotForm() {
        selectedAppointment = null;
        selectedSlotId = null;
        showSlotAssignmentDialog = false;
        availableSlots.clear();
    }
    
    // ==================== UI Helper Methods ====================
    
    // In AdminAppointmentBean.java - Replace getStatusColor method:

public String getStatusColor(String status) {
    if (status == null) return "secondary";
    
    String upperStatus = status.toUpperCase();
    
    // Traditional switch statement for Java 11 compatibility
    switch (upperStatus) {
        case "PENDING":
            return "warning";
        case "CONFIRMED":
            return "info";
        case "COMPLETED":
            return "success";
        case "CANCELLED":
            return "danger";
        case "PAID":
            return "primary";
        default:
            return "secondary";
    }
}

// In AdminAppointmentBean.java, add:
public void debugDataLoad() {
    try {
        System.out.println("=== DEBUG DATA LOAD ===");
        System.out.println("Current filter: " + filter);
        System.out.println("Page: " + currentPage + ", Size: " + pageSize);
        
        // Test without filters
        AppointmentFilterDTO testFilter = new AppointmentFilterDTO();
        testFilter.setPage(0);
        testFilter.setSize(100); // Get all
        
        List<AppointmentDTO> allAppointments = adminEJB.getFilteredAppointments(testFilter);
        System.out.println("All appointments count: " + allAppointments.size());
        
        for (AppointmentDTO appt : allAppointments) {
            System.out.println("ID: " + appt.getAppointmentId() + 
                             ", Client: " + appt.getClientName() +
                             ", Date: " + appt.getAppointmentDateTime() +
                             ", Status: " + appt.getStatus());
        }
        
        // Check count
        Long totalCount = adminEJB.countFilteredAppointments(filter);
        System.out.println("Total count with current filter: " + totalCount);
        
        showInfoMessage("Debug: Found " + allAppointments.size() + " total appointments. Filtered: " + totalCount);
        
    } catch (Exception e) {
        e.printStackTrace();
        showErrorMessage("Debug failed: " + e.getMessage());
    }
}
    
    public boolean canAssignSlot(AppointmentDTO appt) {
        return appt != null && appt.isPending() && !appt.hasSlot();
    }
    
    public boolean canConfirm(AppointmentDTO appt) {
        return appt != null && appt.isPending();
    }
    
    public boolean canCancel(AppointmentDTO appt) {
        return appt != null && (appt.isPending() || appt.isConfirmed());
    }
    
    public boolean canComplete(AppointmentDTO appt) {
        return appt != null && (appt.isConfirmed() || appt.isPaid());
    }
    
    public boolean canMarkPaid(AppointmentDTO appt) {
        return appt != null && appt.isConfirmed();
    }
    
    // ==================== Message Helper Methods ====================
    
    private void showInfoMessage(String message) {
        FacesContext.getCurrentInstance()
            .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, INFO_TITLE, message));
    }
    
    private void showSuccessMessage(String message) {
        FacesContext.getCurrentInstance()
            .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, SUCCESS_TITLE, message));
    }
    
    private void showErrorMessage(String message) {
        FacesContext.getCurrentInstance()
            .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, ERROR_TITLE, message));
    }
    
    private void showWarningMessage(String message) {
        FacesContext.getCurrentInstance()
            .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, WARNING_TITLE, message));
    }
    
    // ==================== Getters and Setters ====================
    
    public List<AppointmentDTO> getAppointments() { 
        return appointments; 
    }
    
    public void setAppointments(List<AppointmentDTO> appointments) { 
        this.appointments = appointments; 
    }
    
    public AppointmentDTO getSelectedAppointment() { 
        return selectedAppointment; 
    }
    
    public void setSelectedAppointment(AppointmentDTO selectedAppointment) { 
        this.selectedAppointment = selectedAppointment; 
    }
    
    public AppointmentFilterDTO getFilter() { 
        return filter; 
    }
    
    public void setFilter(AppointmentFilterDTO filter) { 
        this.filter = filter; 
    }
    
    public String getNewStatus() { 
        return newStatus; 
    }
    
    public void setNewStatus(String newStatus) { 
        this.newStatus = newStatus; 
    }
    
    public String getCancellationReason() { 
        return cancellationReason; 
    }
    
    public void setCancellationReason(String cancellationReason) { 
        this.cancellationReason = cancellationReason; 
    }
    
    public Integer getSelectedSlotId() { 
        return selectedSlotId; 
    }
    
    public void setSelectedSlotId(Integer selectedSlotId) { 
        this.selectedSlotId = selectedSlotId; 
    }
    
    public List<TimeSlot> getAvailableSlots() { 
        return availableSlots; 
    }
    
    public void setAvailableSlots(List<TimeSlot> availableSlots) { 
        this.availableSlots = availableSlots; 
    }
    
    public Map<String, Long> getAppointmentStats() { 
        return appointmentStats; 
    }
    
    public void setAppointmentStats(Map<String, Long> appointmentStats) { 
        this.appointmentStats = appointmentStats; 
    }
    
    public boolean isShowCancellationDialog() { 
        return showCancellationDialog; 
    }
    
    public void setShowCancellationDialog(boolean showCancellationDialog) { 
        this.showCancellationDialog = showCancellationDialog; 
    }
    
    public boolean isShowSlotAssignmentDialog() { 
        return showSlotAssignmentDialog; 
    }
    
    public void setShowSlotAssignmentDialog(boolean showSlotAssignmentDialog) { 
        this.showSlotAssignmentDialog = showSlotAssignmentDialog; 
    }
    
    public boolean isShowDetailsDialog() { 
        return showDetailsDialog; 
    }
    
    public void setShowDetailsDialog(boolean showDetailsDialog) { 
        this.showDetailsDialog = showDetailsDialog; 
    }
    
    public int getCurrentPage() { 
        return currentPage; 
    }
    
    public void setCurrentPage(int currentPage) { 
        this.currentPage = currentPage; 
    }
    
    public int getPageSize() { 
        return pageSize; 
    }
    
    public void setPageSize(int pageSize) { 
        this.pageSize = pageSize; 
    }
    
    public long getTotalItems() { 
        return totalItems; 
    }
    
    public void setTotalItems(long totalItems) { 
        this.totalItems = totalItems; 
    }
    
    public int getTotalPages() {
        return pageSize > 0 ? (int) Math.ceil((double) totalItems / pageSize) : 0;
    }
    
    public List<String> getStatusOptions() {
        return List.of("ALL", "PENDING", "CONFIRMED", "COMPLETED", "CANCELLED", "PAID");
    }
    
    // ==================== Debug Methods ====================
    
    /**
     * Debug method to test backend connectivity
     */
    public void debugTest() {
        try {
            LOGGER.info("=== DEBUG TEST START ===");
            
            // Test direct entity fetch
            List<Appointment> directAppointments = adminEJB.getAllAppointmentsSimple();
            LOGGER.log(Level.INFO, "Direct query found: {0} appointments", directAppointments.size());
            
            // Test DTO fetch
            List<AppointmentDTO> dtoAppointments = adminEJB.getFilteredAppointments(filter);
            LOGGER.log(Level.INFO, "DTO query found: {0} appointments", dtoAppointments.size());
            
            // Test statistics
            Map<String, Long> stats = adminEJB.getDashboardStats();
            LOGGER.log(Level.INFO, "Statistics: {0}", stats);
            
            showInfoMessage("Debug complete. Found " + directAppointments.size() + " appointments.");
            LOGGER.info("=== DEBUG TEST END ===");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Debug test failed", e);
            showErrorMessage("Debug failed: " + e.getMessage());
        }
    }
    
    public void testDataLoad() {
    try {
        String result = adminEJB.testDataLoad();
        showInfoMessage("Test Result: " + result);
        System.out.println("Test Data Load Result: " + result);
    } catch (Exception e) {
        showErrorMessage("Test failed: " + e.getMessage());
        e.printStackTrace();
    }
}
}
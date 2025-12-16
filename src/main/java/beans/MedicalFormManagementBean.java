// file: managed/MedicalFormManagementBean.java
package beans;

import dto.MedicalFormDTO;
import dto.MedicalFormFilterDTO;
import ejb.AdminEJBLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Named
@SessionScoped
public class MedicalFormManagementBean implements Serializable {
    
    @EJB
    private AdminEJBLocal adminEJB;
    
    @Inject
    private UserSessionBean userSessionBean; // Add this injection
    
    // Filter properties
    private MedicalFormFilterDTO filter = new MedicalFormFilterDTO();
    private List<MedicalFormDTO> medicalForms = new ArrayList<>();
    private Long totalForms = 0L;
    private int currentPage = 0;
    private int pageSize = 10;
    
    // Selected form for detailed view
    private MedicalFormDTO selectedForm;
    private String rejectionReason;
    
    // Statistics
    private Map<String, Object> statistics = new HashMap<>();
    
    @PostConstruct
    public void init() {
        try {
            // Security check - ensure user is admin
            if (!userSessionBean.isAdmin()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Access denied. Admin privileges required.", null));
                return;
            }
            
            loadMedicalForms();
            loadStatistics();
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error initializing medical forms: " + e.getMessage(), null));
        }
    }
    
    public void loadMedicalForms() {
        try {
            filter.setPage(currentPage);
            filter.setSize(pageSize);
            
            medicalForms = adminEJB.getFilteredMedicalForms(filter);
            totalForms = adminEJB.countFilteredMedicalForms(filter);
            
            if (medicalForms == null) {
                medicalForms = new ArrayList<>();
            }
            
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error loading medical forms: " + e.getMessage(), null));
            medicalForms = new ArrayList<>();
        }
    }
    
    // file: managed/MedicalFormManagementBean.java

public void loadStatistics() {
    try {
        // --- FIX START: Use an empty filter to get ALL forms ---
        MedicalFormFilterDTO statsFilter = new MedicalFormFilterDTO();
        // Do NOT set startDate or endDate
        // Set size to a maximum value to ensure all records are fetched for statistics
        statsFilter.setSize(Integer.MAX_VALUE);
        statsFilter.setPage(0); // Ensure starting from the first page
        
        List<MedicalFormDTO> allForms = adminEJB.getFilteredMedicalForms(statsFilter);
        // --- FIX END ---
        
        if (allForms == null) {
            allForms = new ArrayList<>();
        }
        
        long approvedCount = allForms.stream()
            // Form is approved if isApproved is explicitly TRUE
            .filter(f -> f != null && f.getIsApproved() != null && f.getIsApproved())
            .count();
        
        long pendingCount = allForms.stream()
            // Form is pending if isApproved is NULL or FALSE and it hasn't been explicitly rejected (approvedAt is null)
            // NOTE: The UI logic in getStatusText implies forms are 'Pending' if not approved AND approvedAt is null.
            // However, the statistics logic simply checks for NOT approved (including rejected).
            // Based on the UI card label "Pending Review" and the table status "Pending" and "Approved", let's adjust the bean logic:
            .filter(f -> f != null && (f.getIsApproved() == null || f.getIsApproved().equals(false)))
            .count();
            
        // Critical: The table only shows "Approved" and "Pending." "Pending Review" should arguably count all non-approved.
        // Let's use the simpler logic for now: isApproved == false OR isApproved == null
        
        long totalFormsCount = adminEJB.countFilteredMedicalForms(new MedicalFormFilterDTO()); // Get total count from DB
        
        statistics = new HashMap<>();
        statistics.put("approvedForms", approvedCount);
        statistics.put("pendingForms", totalFormsCount - approvedCount); // Use total from DB - approved count for robustness
        statistics.put("appointmentsWithForms", totalFormsCount);
        statistics.put("approvalRate", totalFormsCount > 0 ? (approvedCount * 100.0 / totalFormsCount) : 0.0);
        
    } catch (Exception e) {
        // ... error handling
    }
}
    
    public void search() {
        currentPage = 0;
        loadMedicalForms();
    }
    
    public void clearFilters() {
        filter = new MedicalFormFilterDTO();
        currentPage = 0;
        loadMedicalForms();
    }
    
    public void nextPage() {
        if (totalForms != null && (currentPage + 1) * pageSize < totalForms) {
            currentPage++;
            loadMedicalForms();
        }
    }
    
    public void prevPage() {
        if (currentPage > 0) {
            currentPage--;
            loadMedicalForms();
        }
    }
    
    public void goToPage(int page) {
        currentPage = page;
        loadMedicalForms();
    }
    
    public void approveForm(Integer formId) {
        try {
            // Security check
            if (!userSessionBean.isAdmin()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Access denied. Admin privileges required.", null));
                return;
            }
            
            // Get admin ID from session bean
            Long adminId = userSessionBean.getUserId();
            
            if (adminId == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Admin not found in session", null));
                return;
            }
            
            adminEJB.approveMedicalForm(formId, adminId);
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Medical form approved successfully!", null));
            
            loadMedicalForms(); // Refresh the list
            
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error approving form: " + e.getMessage(), null));
            e.printStackTrace(); // Add this for debugging
        }
    }
    
    public void rejectForm(Integer formId) {
        try {
            // Security check
            if (!userSessionBean.isAdmin()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Access denied. Admin privileges required.", null));
                return;
            }
            
            // Get admin ID from session bean
            Long adminId = userSessionBean.getUserId();
            
            if (adminId == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Admin not found in session", null));
                return;
            }
            
            // Check if rejection reason is provided
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Please provide a rejection reason", null));
                return;
            }
            
            boolean success = adminEJB.rejectMedicalForm(formId, adminId, rejectionReason);
            
            if (success) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Medical form rejected. Client has been notified.", null));
                
                rejectionReason = null; // Clear the reason
                loadMedicalForms(); // Refresh the list
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Failed to reject medical form", null));
            }
            
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error rejecting form: " + e.getMessage(), null));
            e.printStackTrace(); // Add this for debugging
        }
    }
    
    public void viewFormDetails(Integer formId) {
        try {
            selectedForm = adminEJB.getMedicalFormDetails(formId);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error loading form details: " + e.getMessage(), null));
        }
    }
    
    public void setSelectedFormForRejection(MedicalFormDTO form) {
        this.selectedForm = form;
    }
    
    // Getters and Setters
    public MedicalFormFilterDTO getFilter() { return filter; }
    public void setFilter(MedicalFormFilterDTO filter) { this.filter = filter; }
    
    public List<MedicalFormDTO> getMedicalForms() { 
        if (medicalForms == null) {
            medicalForms = new ArrayList<>();
        }
        return medicalForms; 
    }
    public void setMedicalForms(List<MedicalFormDTO> medicalForms) { 
        this.medicalForms = medicalForms != null ? medicalForms : new ArrayList<>();
    }
    
    public Long getTotalForms() { 
        return totalForms != null ? totalForms : 0L; 
    }
    public void setTotalForms(Long totalForms) { this.totalForms = totalForms; }
    
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
    
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    
    public MedicalFormDTO getSelectedForm() { return selectedForm; }
    public void setSelectedForm(MedicalFormDTO selectedForm) { this.selectedForm = selectedForm; }
    
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    
    public Map<String, Object> getStatistics() { 
        if (statistics == null) {
            statistics = new HashMap<>();
        }
        return statistics; 
    }
    public void setStatistics(Map<String, Object> statistics) { 
        this.statistics = statistics != null ? statistics : new HashMap<>();
    }
    
    // Helper methods for pagination
    public int getTotalPages() {
        if (totalForms == null || pageSize == 0) return 0;
        return (int) Math.ceil((double) totalForms / pageSize);
    }
    
    public List<Integer> getPageNumbers() {
        int totalPages = getTotalPages();
        List<Integer> pages = new ArrayList<>();
        for (int i = 0; i < totalPages; i++) {
            pages.add(i);
        }
        return pages;
    }
    
    // Helper methods for display
    public String getStatusBadge(String status) {
        if (status == null) return "badge-warning";
        
        if ("APPROVED".equalsIgnoreCase(status) || "true".equalsIgnoreCase(status)) {
            return "badge-success";
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            return "badge-danger";
        } else {
            return "badge-warning";
        }
    }
    
    public String getStatusText(MedicalFormDTO form) {
        if (form == null) return "Unknown";
        
        Boolean isApproved = form.getIsApproved();
        if (isApproved != null && isApproved) {
            return "Approved";
        } else if (isApproved != null && !isApproved && form.getApprovedAt() != null) {
            return "Rejected";
        } else {
            return "Pending";
        }
    }
}
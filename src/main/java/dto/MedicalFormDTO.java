// file: dto/MedicalFormDTO.java
package dto;

import java.time.LocalDateTime;

public class MedicalFormDTO {
    private Integer formId;
    private Long clientId;
    private String clientName;
    private String clientEmail;
    private Long appointmentId;
    private LocalDateTime appointmentDateTime;
    private String appointmentStatus;
    
    // Medical details
    private Boolean isMinor;
    private Boolean isPregnant;
    private Boolean diabetes;
    private Boolean heartCondition;
    private Boolean hasAllergies;
    private String allergyDetails;
    
    // Approval details
    private Boolean isApproved;
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    private Long approvedByAdminId;
    private String approvedByAdminName;
    
    // Constructors
    public MedicalFormDTO() {}
    
    public MedicalFormDTO(Integer formId, Long clientId, String clientName, String clientEmail,
                         Long appointmentId, LocalDateTime appointmentDateTime, String appointmentStatus,
                         Boolean isMinor, Boolean isPregnant, Boolean diabetes, Boolean heartCondition,
                         Boolean hasAllergies, String allergyDetails, Boolean isApproved,
                         LocalDateTime submittedAt, LocalDateTime approvedAt,
                         Long approvedByAdminId, String approvedByAdminName) {
        this.formId = formId;
        this.clientId = clientId;
        this.clientName = clientName;
        this.clientEmail = clientEmail;
        this.appointmentId = appointmentId;
        this.appointmentDateTime = appointmentDateTime;
        this.appointmentStatus = appointmentStatus;
        this.isMinor = isMinor;
        this.isPregnant = isPregnant;
        this.diabetes = diabetes;
        this.heartCondition = heartCondition;
        this.hasAllergies = hasAllergies;
        this.allergyDetails = allergyDetails;
        this.isApproved = isApproved;
        this.submittedAt = submittedAt;
        this.approvedAt = approvedAt;
        this.approvedByAdminId = approvedByAdminId;
        this.approvedByAdminName = approvedByAdminName;
    }
    
    // Getters and Setters
    public Integer getFormId() { return formId; }
    public void setFormId(Integer formId) { this.formId = formId; }
    
    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    
    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }
    
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    
    public LocalDateTime getAppointmentDateTime() { return appointmentDateTime; }
    public void setAppointmentDateTime(LocalDateTime appointmentDateTime) { this.appointmentDateTime = appointmentDateTime; }
    
    public String getAppointmentStatus() { return appointmentStatus; }
    public void setAppointmentStatus(String appointmentStatus) { this.appointmentStatus = appointmentStatus; }
    
    public Boolean getIsMinor() { return isMinor; }
    public void setIsMinor(Boolean isMinor) { this.isMinor = isMinor; }
    
    public Boolean getIsPregnant() { return isPregnant; }
    public void setIsPregnant(Boolean isPregnant) { this.isPregnant = isPregnant; }
    
    public Boolean getDiabetes() { return diabetes; }
    public void setDiabetes(Boolean diabetes) { this.diabetes = diabetes; }
    
    public Boolean getHeartCondition() { return heartCondition; }
    public void setHeartCondition(Boolean heartCondition) { this.heartCondition = heartCondition; }
    
    public Boolean getHasAllergies() { return hasAllergies; }
    public void setHasAllergies(Boolean hasAllergies) { this.hasAllergies = hasAllergies; }
    
    public String getAllergyDetails() { return allergyDetails; }
    public void setAllergyDetails(String allergyDetails) { this.allergyDetails = allergyDetails; }
    
    public Boolean getIsApproved() { return isApproved; }
    public void setIsApproved(Boolean isApproved) { this.isApproved = isApproved; }
    
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    
    public Long getApprovedByAdminId() { return approvedByAdminId; }
    public void setApprovedByAdminId(Long approvedByAdminId) { this.approvedByAdminId = approvedByAdminId; }
    
    public String getApprovedByAdminName() { return approvedByAdminName; }
    public void setApprovedByAdminName(String approvedByAdminName) { this.approvedByAdminName = approvedByAdminName; }
}
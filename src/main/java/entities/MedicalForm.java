package entities;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "medical_form")
public class MedicalForm implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FORMID")
    private Integer formId;

    @Column(name = "ALLERGYDETAILS", length = 500)
    private String allergyDetails;

    @Column(name = "DIABETES")
    private Boolean diabetes = false;

    @Column(name = "HASALLERGIES")
    private Boolean hasAllergies = false;

    @Column(name = "HEARTCONDITION")
    private Boolean heartCondition = false;

    @Column(name = "ISMINOR")
    private Boolean isMinor = false;

    @Column(name = "ISPREGNANT")
    private Boolean isPregnant = false;
    
    // FIX: Added missing field and methods to resolve 'cannot find symbol: setIsApproved(boolean)'
    @Column(name = "IS_APPROVED")
    private Boolean isApproved = false; 

    @Column(name = "APPROVED_AT")
private LocalDateTime approvedAt;
    
    @Column(name = "SUBMITTEDAT")
    private LocalDateTime submittedAt;

    @Column(name = "VALIDATED_AT")
    private LocalDateTime validatedAt;
    
     // ADD THIS FIELD FOR REJECTION REASON
    @Column(name = "REJECTION_REASON", length = 1000)
    private String rejectionReason;

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLIENT_ID", nullable = false)
    private AppUser client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VALIDATED_BY")
    private AppUser validatedBy;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "APPOINTMENT_ID", unique = true, nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "APPROVED_BY_ADMIN_ID") // Column name for the admin who approved it
    private AppUser approvedBy;
    public MedicalForm() {}

    // Getters and Setters
    public Integer getFormId() { return formId; }
    public void setFormId(Integer formId) { this.formId = formId; }

    public String getAllergyDetails() { return allergyDetails; }
    public void setAllergyDetails(String allergyDetails) { this.allergyDetails = allergyDetails; }

    public Boolean getDiabetes() { return diabetes; }
    public void setDiabetes(Boolean diabetes) { this.diabetes = diabetes; }

    public Boolean getHasAllergies() { return hasAllergies; }
    public void setHasAllergies(Boolean hasAllergies) { this.hasAllergies = hasAllergies; }

    public Boolean getHeartCondition() { return heartCondition; }
    public void setHeartCondition(Boolean heartCondition) { this.heartCondition = heartCondition; }

    public Boolean getIsMinor() { return isMinor; }
    public void setIsMinor(Boolean isMinor) { this.isMinor = isMinor; }

    public Boolean getIsPregnant() { return isPregnant; }
    public void setIsPregnant(Boolean isPregnant) { this.isPregnant = isPregnant; }
    
    public Boolean getIsApproved() { return isApproved; }
    public void setIsApproved(Boolean isApproved) { this.isApproved = isApproved; } // Setter for isApproved

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public LocalDateTime getValidatedAt() { return validatedAt; }
    public void setValidatedAt(LocalDateTime validatedAt) { this.validatedAt = validatedAt; }

    public AppUser getClient() { return client; }
    public void setClient(AppUser client) { this.client = client; }

    public AppUser getValidatedBy() { return validatedBy; }
    public void setValidatedBy(AppUser validatedBy) { this.validatedBy = validatedBy; }

    @JsonbTransient
    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }
    
    
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public AppUser getApprovedBy() { return approvedBy; }
    public void setApprovedBy(AppUser approvedBy) { this.approvedBy = approvedBy; }
}
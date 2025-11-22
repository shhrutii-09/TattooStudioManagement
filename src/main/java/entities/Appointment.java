package entities;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointment")
public class Appointment implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "APPOINTMENTID")
    private Long appointmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLIENT_ID", nullable = false)
    private AppUser client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARTIST_ID", nullable = false)
    private AppUser artist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DESIGN_ID", nullable = true)
    private TattooDesign design;
    
    // FIX: Added missing link to TimeSlot to resolve 'cannot find symbol: method getSlot()'
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SLOT_ID", unique = true, nullable = false)
    private TimeSlot slot; 

    @Column(name = "APPOINTMENTDATETIME", nullable = false)
    private LocalDateTime appointmentDateTime; // Redundant but useful for query/display

    @Column(name = "STATUS", nullable = false, length = 50)
    private String status; // PENDING, CONFIRMED, COMPLETED, CANCELLED

    // FIX: Added missing field to resolve 'cannot find symbol: setCancellationReason(String)'
    @Column(name = "CANCELLATION_REASON", length = 255)
    private String cancellationReason; 

    
    @Column(name = "CLIENT_NOTE", columnDefinition = "TEXT")
    private String clientNote;

    // One-to-One link to Payment (mapped by Appointment)
    @OneToOne(mappedBy = "appointment", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private Payment payment;

    // One-to-One link to MedicalForm (mapped by Appointment)
    @OneToOne(mappedBy = "appointment", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private MedicalForm medicalForm;
    
    // One-to-One link to Feedback (mapped by Appointment)
    @OneToOne(mappedBy = "appointment", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private Feedback feedback;

    // REMOVED RETOUCH LOGIC as requested by user.
    /*
    @Column(name = "RETOUCH_NOTES", columnDefinition = "TEXT")
    private String retouchNotes;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORIGINAL_APPOINTMENT_ID")
    private Appointment originalAppointment;
    */
    
    // getter
    public String getClientNote() {
        return clientNote;
    }

    // setter
    public void setClientNote(String clientNote) {
        this.clientNote = clientNote;
    }

    public Appointment() {}

    // Getters and Setters
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }

    public AppUser getClient() { return client; }
    public void setClient(AppUser client) { this.client = client; }

    public AppUser getArtist() { return artist; }
    public void setArtist(AppUser artist) { this.artist = artist; }

    public TattooDesign getDesign() { return design; }
    public void setDesign(TattooDesign design) { this.design = design; }
    
    public TimeSlot getSlot() { return slot; } 
    public void setSlot(TimeSlot slot) { this.slot = slot; } // Setter for TimeSlot

    public LocalDateTime getAppointmentDateTime() { return appointmentDateTime; }
    public void setAppointmentDateTime(LocalDateTime appointmentDateTime) { this.appointmentDateTime = appointmentDateTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getCancellationReason() { return cancellationReason; } // Getter/Setter for Cancellation Reason
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    @JsonbTransient
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }

    
    public MedicalForm getMedicalForm() { return medicalForm; }
    public void setMedicalForm(MedicalForm medicalForm) { this.medicalForm = medicalForm; }
    
    public Feedback getFeedback() { return feedback; }
    public void setFeedback(Feedback feedback) { this.feedback = feedback; }

}
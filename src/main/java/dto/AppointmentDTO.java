// file: dto/AppointmentDTO.java - CORRECTED VERSION
package dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AppointmentDTO implements Serializable {
    private Long appointmentId;
    private Long clientId;
    private String clientName;
    private String clientEmail;
    private Long artistId;
    private String artistName;
    private Long designId;
    private String designTitle;
    private Integer slotId;
    private LocalDateTime appointmentDateTime;
    private String status;
    private String cancellationReason;
    private String clientNote;
    private BigDecimal amount;           // CHANGED from Double to BigDecimal
    private String paymentStatus;

    // Default constructor
    public AppointmentDTO() {}
    
    // Constructor with BigDecimal amount
    public AppointmentDTO(Long appointmentId, 
                         Long clientId, String clientName, String clientEmail,
                         Long artistId, String artistName, 
                         Long designId, String designTitle,
                         Integer slotId, LocalDateTime appointmentDateTime,
                         String status, String cancellationReason, 
                         String clientNote,
                         BigDecimal amount, String paymentStatus) {
        this.appointmentId = appointmentId;
        this.clientId = clientId;
        this.clientName = clientName;
        this.clientEmail = clientEmail;
        this.artistId = artistId;
        this.artistName = artistName;
        this.designId = designId;
        this.designTitle = designTitle;
        this.slotId = slotId;
        this.appointmentDateTime = appointmentDateTime;
        this.status = status;
        this.cancellationReason = cancellationReason;
        this.clientNote = clientNote;
        this.amount = amount;
        this.paymentStatus = paymentStatus;
    }

    // Status helper methods
    public boolean isPending() { 
        return "PENDING".equals(status); 
    }
    
    public boolean isConfirmed() { 
        return "CONFIRMED".equals(status); 
    }
    
    public boolean isCompleted() { 
        return "COMPLETED".equals(status); 
    }
    
    public boolean isCancelled() { 
        return "CANCELLED".equals(status); 
    }
    
    public boolean isPaid() { 
        return "PAID".equals(status); 
    }
    
    public boolean hasSlot() {
        return slotId != null && slotId > 0;
    }
    
    // Getters and Setters
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    
    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    
    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }
    
    public Long getArtistId() { return artistId; }
    public void setArtistId(Long artistId) { this.artistId = artistId; }
    
    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }
    
    public Long getDesignId() { return designId; }
    public void setDesignId(Long designId) { this.designId = designId; }
    
    public String getDesignTitle() { return designTitle; }
    public void setDesignTitle(String designTitle) { this.designTitle = designTitle; }
    
    public Integer getSlotId() { return slotId; }
    public void setSlotId(Integer slotId) { this.slotId = slotId; }
    
    public LocalDateTime getAppointmentDateTime() { return appointmentDateTime; }
    public void setAppointmentDateTime(LocalDateTime appointmentDateTime) { 
        this.appointmentDateTime = appointmentDateTime; 
    }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    
    public String getClientNote() { return clientNote; }
    public void setClientNote(String clientNote) { this.clientNote = clientNote; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    // Helper method for Double compatibility if needed in JSF
    public Double getAmountAsDouble() {
        return amount != null ? amount.doubleValue() : null;
    }
    
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    
    // Helper methods
    public String getFormattedDate() {
        if (appointmentDateTime == null) return "No date";
        try {
            java.time.format.DateTimeFormatter formatter = 
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return appointmentDateTime.format(formatter);
        } catch (Exception e) {
            return String.valueOf(appointmentDateTime);
        }
    }
    
    public String getFormattedDateTime() {
        if (appointmentDateTime == null) return "No date";
        try {
            java.time.format.DateTimeFormatter formatter = 
                java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' HH:mm");
            return appointmentDateTime.format(formatter);
        } catch (Exception e) {
            return getFormattedDate();
        }
    }
    
    public String getStatusColor() {
        if (status == null) return "secondary";
        switch (status) {
            case "PENDING": return "warning";
            case "CONFIRMED": return "info";
            case "COMPLETED": return "success";
            case "CANCELLED": return "danger";
            case "PAID": return "primary";
            default: return "secondary";
        }
    }
}
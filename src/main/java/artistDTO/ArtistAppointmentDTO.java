package artistDTO;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ArtistAppointmentDTO implements Serializable {

    private Long appointmentId;

    private String clientName;
    private String clientEmail;

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
    }

    public String getDesignTitle() {
        return designTitle;
    }

    public void setDesignTitle(String designTitle) {
        this.designTitle = designTitle;
    }

    public Integer getSlotId() {
        return slotId;
    }

    public void setSlotId(Integer slotId) {
        this.slotId = slotId;
    }

    public LocalDateTime getAppointmentDateTime() {
        return appointmentDateTime;
    }

    public void setAppointmentDateTime(LocalDateTime appointmentDateTime) {
        this.appointmentDateTime = appointmentDateTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getClientNote() {
        return clientNote;
    }

    public void setClientNote(String clientNote) {
        this.clientNote = clientNote;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    private String designTitle;

    private Integer slotId;
    private LocalDateTime appointmentDateTime;

    private String status;
    private String clientNote;
    private String cancellationReason;

    private BigDecimal amount;
    private String paymentStatus;

    /* ===== Helper flags for UI ===== */

    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public boolean isConfirmed() {
        return "CONFIRMED".equals(status);
    }

    public boolean isPaid() {
        return "PAID".equals(status);
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    public boolean isCancelled() {
        return "CANCELLED".equals(status);
    }

    public boolean hasSlot() {
        return slotId != null;
    }

    
public String getAppointmentDateFormatted() {
    if (this.appointmentDateTime == null) {
        return "";
    }
    return this.appointmentDateTime.format(
        DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
    );
}
}

    

    
    /* ===== Getters / Setters ===== */

    // (generate getters & setters normally)


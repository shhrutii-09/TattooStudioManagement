package dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ArtistPendingEarningDTO {

    private Integer logId;
    private Long artistId;
    private Long adminId;
    private Long paymentId;
    private Long appointmentId;

    private BigDecimal totalAmount;
    private BigDecimal artistShare;
    private BigDecimal adminShare;   // <-- MISSING FIELD (ADD THIS!)
    private BigDecimal premiumBonus;

    private String payoutStatus;
    private LocalDateTime calculatedAt;
    private LocalDateTime payoutAt;

    private Long payoutId;
    private String notes;            // <-- MISSING FIELD (ADD THIS!)

    private String artistName;
    private String adminName;

    // ---------------- Getters & Setters ----------------

    public Integer getLogId() { return logId; }
    public void setLogId(Integer logId) { this.logId = logId; }

    public Long getArtistId() { return artistId; }
    public void setArtistId(Long artistId) { this.artistId = artistId; }

    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getArtistShare() { return artistShare; }
    public void setArtistShare(BigDecimal artistShare) { this.artistShare = artistShare; }

    public BigDecimal getAdminShare() { return adminShare; }      // <-- ADD THIS
    public void setAdminShare(BigDecimal adminShare) { this.adminShare = adminShare; } // <-- ADD THIS

    public BigDecimal getPremiumBonus() { return premiumBonus; }
    public void setPremiumBonus(BigDecimal premiumBonus) { this.premiumBonus = premiumBonus; }

    public String getPayoutStatus() { return payoutStatus; }
    public void setPayoutStatus(String payoutStatus) { this.payoutStatus = payoutStatus; }

    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }

    public LocalDateTime getPayoutAt() { return payoutAt; }
    public void setPayoutAt(LocalDateTime payoutAt) { this.payoutAt = payoutAt; }

    public Long getPayoutId() { return payoutId; }
    public void setPayoutId(Long payoutId) { this.payoutId = payoutId; }

    public String getNotes() { return notes; }                    // <-- ADD THIS
    public void setNotes(String notes) { this.notes = notes; }    // <-- ADD THIS

    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }
}

package entities;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "earning_log")
public class EarningLog implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LOGID")
    private Integer logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARTIST_ID", nullable = false)
    private AppUser artist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ADMIN_ID")
    private AppUser admin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "APPOINTMENT_ID")
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PAYMENT_ID")
    private Payment payment;
    
    // ADDED: Field to store the total transaction amount
    @Column(name = "TOTAL_AMOUNT", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "ARTISTSHARE", precision = 12, scale = 2)
    private BigDecimal artistShare;

    @Column(name = "ADMINSHARE", precision = 12, scale = 2)
    private BigDecimal adminShare;

    @Column(name = "PREMIUMBONUS", precision = 12, scale = 2)
    private BigDecimal premiumBonus;

    @Column(name = "PAYOUTSTATUS", length = 50)
    private String payoutStatus; // e.g., UNPAID, PAID, PROCESSING

    @Column(name = "CALCULATEDAT")
    private LocalDateTime calculatedAt;

    @Column(name = "PAYOUTAT")
    private LocalDateTime payoutAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PAYOUT_ID")
    private ArtistPayout payout;

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
    
@Column(name = "NOTES", columnDefinition = "TEXT")
    private String notes;

    // --- Getters and Setters ---
    public Integer getLogId() { return logId; }
    public void setLogId(Integer logId) { this.logId = logId; }

    public AppUser getArtist() { return artist; }
    public void setArtist(AppUser artist) { this.artist = artist; }

    public AppUser getAdmin() { return admin; }
    public void setAdmin(AppUser admin) { this.admin = admin; }

    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }

    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }

    // ADDED: Getter and Setter for totalAmount
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getArtistShare() { return artistShare; }
    public void setArtistShare(BigDecimal artistShare) { this.artistShare = artistShare; }

    public BigDecimal getAdminShare() { return adminShare; }
    public void setAdminShare(BigDecimal adminShare) { this.adminShare = adminShare; }

    public BigDecimal getPremiumBonus() { return premiumBonus; }
    public void setPremiumBonus(BigDecimal premiumBonus) { this.premiumBonus = premiumBonus; }

    public String getPayoutStatus() { return payoutStatus; }
    public void setPayoutStatus(String payoutStatus) { this.payoutStatus = payoutStatus; }

    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }

    public LocalDateTime getPayoutAt() { return payoutAt; }
    public void setPayoutAt(LocalDateTime payoutAt) { this.payoutAt = payoutAt; }

    public ArtistPayout getPayout() { return payout; }
    public void setPayout(ArtistPayout payout) { this.payout = payout; }
}
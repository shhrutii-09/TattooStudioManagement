package entities;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "artist_payout")
public class ArtistPayout implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PAYOUTID")
    private Long payoutId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARTIST_ID", nullable = false)
    private AppUser artist;

    // FIX: Renamed from TOTAL_AMOUNT to AMOUNT to satisfy setAmount(BigDecimal) error.
    @Column(name = "AMOUNT", precision = 12, scale = 2)
    private BigDecimal amount; 
    
    // FIX: Added missing field to resolve 'cannot find symbol: setNotes(String)'
    @Column(name = "NOTES", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "STATUS", length = 50)
    private String payoutStatus; // PENDING, PAID

    // FIX: Renamed from PAYOUTAT to PAYOUTDATE to satisfy setPayoutDate(LocalDateTime) error.
    @Column(name = "PAYOUTDATE")
    private LocalDateTime payoutDate;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ADMIN_ID")
    private AppUser admin;

    @OneToMany(mappedBy = "payout", fetch = FetchType.LAZY)
    private List<EarningLog> earningLogs;

    public ArtistPayout() { }

    // Getters & setters
    public Long getPayoutId() { return payoutId; }
    public void setPayoutId(Long payoutId) { this.payoutId = payoutId; }

    public AppUser getArtist() { return artist; }
    public void setArtist(AppUser artist) { this.artist = artist; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; } // Setter for Amount

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; } // Setter for Notes

    public String getPayoutStatus() { return payoutStatus; }
    public void setPayoutStatus(String payoutStatus) { this.payoutStatus = payoutStatus; }

    public LocalDateTime getPayoutDate() { return payoutDate; }
    public void setPayoutDate(LocalDateTime payoutDate) { this.payoutDate = payoutDate; } // Setter for PayoutDate

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public AppUser getAdmin() { return admin; }
    public void setAdmin(AppUser admin) { this.admin = admin; }
    
    public List<EarningLog> getEarningLogs() { return earningLogs; }
    public void setEarningLogs(List<EarningLog> earningLogs) { this.earningLogs = earningLogs; }
}
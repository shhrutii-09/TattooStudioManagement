package dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ArtistPayoutDTO {
    private Long payoutId;
    private Long artistId;
    private String artistName;
    private BigDecimal amount;
    private String notes;
    private String payoutStatus;
    private LocalDateTime payoutDate;
    private LocalDateTime createdAt;

    // Getters / setters
    public Long getPayoutId() { return payoutId; }
    public void setPayoutId(Long payoutId) { this.payoutId = payoutId; }

    public Long getArtistId() { return artistId; }
    public void setArtistId(Long artistId) { this.artistId = artistId; }

    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPayoutStatus() { return payoutStatus; }
    public void setPayoutStatus(String payoutStatus) { this.payoutStatus = payoutStatus; }

    public LocalDateTime getPayoutDate() { return payoutDate; }
    public void setPayoutDate(LocalDateTime payoutDate) { this.payoutDate = payoutDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

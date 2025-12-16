package dto;

import java.math.BigDecimal;

public class ArtistEarningSummaryDTO {
    private Long artistId;
    private String artistName;
    private BigDecimal totalPaid = BigDecimal.ZERO;
    private BigDecimal pendingAmount = BigDecimal.ZERO;
    private int pendingCount = 0;
    private String lastPayout; // formatted date or "N/A"

    // Getters / setters
    public Long getArtistId() { return artistId; }
    public void setArtistId(Long artistId) { this.artistId = artistId; }

    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    public BigDecimal getTotalPaid() { return totalPaid; }
    public void setTotalPaid(BigDecimal totalPaid) { this.totalPaid = totalPaid; }

    public BigDecimal getPendingAmount() { return pendingAmount; }
    public void setPendingAmount(BigDecimal pendingAmount) { this.pendingAmount = pendingAmount; }

    public int getPendingCount() { return pendingCount; }
    public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }

    public String getLastPayout() { return lastPayout; }
    public void setLastPayout(String lastPayout) { this.lastPayout = lastPayout; }
}

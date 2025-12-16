package dto;

import java.math.BigDecimal;

public class AdminEarningSummaryDTO {

    // --- Totals Before Payout (Gross Earning View) ---
    private BigDecimal totalGrossRevenue = BigDecimal.ZERO;
    private BigDecimal totalArtistPendingShare = BigDecimal.ZERO;
    private BigDecimal totalAdminPendingShare = BigDecimal.ZERO;

    // --- Totals After Payout (Realized Net Earning View) ---
    private BigDecimal totalAdminNetRealized = BigDecimal.ZERO;
    private BigDecimal totalArtistPaidShare = BigDecimal.ZERO;

    // ========================================================
    // GETTERS & SETTERS
    // ========================================================

    public BigDecimal getTotalGrossRevenue() {
        return totalGrossRevenue;
    }

    public void setTotalGrossRevenue(BigDecimal totalGrossRevenue) {
        this.totalGrossRevenue = totalGrossRevenue;
    }

    public BigDecimal getTotalArtistPendingShare() {
        return totalArtistPendingShare;
    }

    public void setTotalArtistPendingShare(BigDecimal totalArtistPendingShare) {
        this.totalArtistPendingShare = totalArtistPendingShare;
    }

    public BigDecimal getTotalAdminPendingShare() {
        return totalAdminPendingShare;
    }

    public void setTotalAdminPendingShare(BigDecimal totalAdminPendingShare) {
        this.totalAdminPendingShare = totalAdminPendingShare;
    }

    public BigDecimal getTotalAdminNetRealized() {
        return totalAdminNetRealized;
    }

    public void setTotalAdminNetRealized(BigDecimal totalAdminNetRealized) {
        this.totalAdminNetRealized = totalAdminNetRealized;
    }

    public BigDecimal getTotalArtistPaidShare() {
        return totalArtistPaidShare;
    }

    public void setTotalArtistPaidShare(BigDecimal totalArtistPaidShare) {
        this.totalArtistPaidShare = totalArtistPaidShare;
    }
}
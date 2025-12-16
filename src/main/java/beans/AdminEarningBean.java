package beans;

import dto.AdminEarningSummaryDTO;
import dto.ArtistPendingEarningDTO;
import ejb.AdminEJBLocal;
import entities.ArtistPayout;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AdminEarningBean
 * Backing bean for admin-earnings.xhtml page.
 * Calculates and presents the system's (admin's) gross and net earnings.
 */
@Named("adminEarningBean")
@ViewScoped
public class AdminEarningBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private AdminEJBLocal adminEJB;

    private AdminEarningSummaryDTO summary = new AdminEarningSummaryDTO();

    @PostConstruct
    public void init() {
        calculateSummary();
    }

    public void calculateSummary() {
        try {
            // Defensive: fetch raw data from EJB (service layer)
            List<ArtistPendingEarningDTO> unpaidLogs = adminEJB.listAllUnpaidPendingEarnings();
            List<ArtistPendingEarningDTO> paidLogs = adminEJB.listAllPaidPendingEarnings();
            List<ArtistPayout> allPayouts = adminEJB.listAllArtistPayouts();

            // --- Total Gross Revenue (All Appointments) ---
            BigDecimal totalGrossRevenue = unpaidLogs.stream()
                    .map(log -> log.getTotalAmount() == null ? BigDecimal.ZERO : log.getTotalAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .add(paidLogs.stream()
                            .map(log -> log.getTotalAmount() == null ? BigDecimal.ZERO : log.getTotalAmount())
                            .reduce(BigDecimal.ZERO, BigDecimal::add))
                    .setScale(2, RoundingMode.HALF_UP);

            // --- Total Artist Pending Share (Unpaid Liabilities) ---
            BigDecimal totalArtistPendingShare = unpaidLogs.stream()
                    .map(log -> log.getArtistShare() == null ? BigDecimal.ZERO : log.getArtistShare())
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            // --- Total Admin Pending Share (Unrealized Profit) ---
            // Note: This is Total Gross - Artist Pending. Since the calculation ensures balance, this is correct.
            BigDecimal totalAdminPendingShare = totalGrossRevenue.subtract(totalArtistPendingShare);

            // --- Total Artist Paid Share (Total Paid to Artists) ---
            BigDecimal totalArtistPaidShare = allPayouts.stream()
                    .map(payout -> payout.getAmount() == null ? BigDecimal.ZERO : payout.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            // --- Total Admin Net Realized (Realized profit margin) ---
            // CRITICAL FIX: Read the stored Admin Share (the remainder from EarningLog), do not recalculate.
            BigDecimal totalAdminNetRealized = paidLogs.stream()
                    .map(log -> log.getAdminShare() == null ? BigDecimal.ZERO : log.getAdminShare())
                    .filter(x -> x != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            summary.setTotalGrossRevenue(totalGrossRevenue);
            summary.setTotalArtistPendingShare(totalArtistPendingShare);
            summary.setTotalAdminPendingShare(totalAdminPendingShare);
            summary.setTotalArtistPaidShare(totalArtistPaidShare);
            summary.setTotalAdminNetRealized(totalAdminNetRealized);

        } catch (Exception e) {
            addError("Failed to calculate admin earnings: " + e.getMessage());
        }
    }

    public AdminEarningSummaryDTO getSummary() {
        return summary;
    }

    public void refresh() {
        calculateSummary();
    }

    private void addError(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }
}
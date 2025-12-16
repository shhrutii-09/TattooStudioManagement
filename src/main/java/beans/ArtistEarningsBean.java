package beans;

import dto.ArtistEarningSummaryDTO;
import dto.ArtistPendingEarningDTO;
import dto.ArtistPayoutDTO;
import ejb.AdminEJBLocal;
import entities.AppUser;
import entities.ArtistPayout;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Named("artistEarningsBean")
@ViewScoped
public class ArtistEarningsBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @EJB
    private AdminEJBLocal adminEJB;

    // Page data
    private List<AppUser> artists = new ArrayList<>();
    private List<ArtistEarningSummaryDTO> summaries = new ArrayList<>();

    private BigDecimal totalPendingAllArtists = BigDecimal.ZERO;
    private BigDecimal totalPaidAllArtists = BigDecimal.ZERO;
    private long pendingItemsCount = 0L;

    // Selected artist dialog state
    private ArtistEarningSummaryDTO selectedSummary;
    private Long selectedArtistId;
    private List<ArtistPendingEarningDTO> pendingList = new ArrayList<>();
    private List<ArtistPayoutDTO> payoutHistory = new ArrayList<>();
    private String payoutNotes;

    @PostConstruct
    public void init() {
        loadAll();
    }

    public void loadAll() {
        try {
            artists = adminEJB.listArtists();
            buildSummaries();
        } catch (Exception e) {
            addError("Failed to load artists: " + e.getMessage());
        }
    }

    private void buildSummaries() {
        summaries.clear();
        totalPendingAllArtists = BigDecimal.ZERO;
        totalPaidAllArtists = BigDecimal.ZERO;
        pendingItemsCount = 0;

        // Precompute total paid across all payouts for quick dashboard stat
        try {
            // Use EJB helper for total completed payments - fallback if missing
            BigDecimal totalCompletedPayments = BigDecimal.ZERO;
            try {
                totalCompletedPayments = adminEJB.getTotalCompletedPaymentsAmount();
            } catch (Exception ex) {
                totalCompletedPayments = BigDecimal.ZERO;
            }

            // Compute all-artist totalPaid via listAllArtistPayouts if available
            try {
                List<ArtistPayout> allPayouts = adminEJB.listAllArtistPayouts();
                totalPaidAllArtists = allPayouts.stream()
                        .map(ap -> ap.getAmount() == null ? BigDecimal.ZERO : ap.getAmount())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            } catch (Exception ex) {
                totalPaidAllArtists = BigDecimal.ZERO;
            }

            for (AppUser artist : artists) {
                ArtistEarningSummaryDTO s = new ArtistEarningSummaryDTO();
                s.setArtistId(artist.getUserId());
                s.setArtistName(artist.getFullName());

                // Pending logs
                List<ArtistPendingEarningDTO> pending = adminEJB.getPendingEarningsByArtist(artist.getUserId());
                BigDecimal pendingAmount = pending.stream()
                        .map(p -> p.getArtistShare() == null ? BigDecimal.ZERO : p.getArtistShare())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                s.setPendingAmount(pendingAmount != null ? pendingAmount.setScale(2) : BigDecimal.ZERO);
                s.setPendingCount(pending.size());
                totalPendingAllArtists = totalPendingAllArtists.add(s.getPendingAmount());
                pendingItemsCount += pending.size();

                // Total paid for this artist
                List<ArtistPayout> payouts = adminEJB.listArtistPayouts(artist.getUserId(), 0, 100);
                BigDecimal artistPaid = payouts.stream()
                        .map(p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                s.setTotalPaid(artistPaid != null ? artistPaid.setScale(2) : BigDecimal.ZERO);

                // Last payout (formatted)
                Optional<ArtistPayout> last = payouts.stream()
                        .filter(p -> p.getPayoutDate() != null)
                        .sorted(Comparator.comparing(ArtistPayout::getPayoutDate).reversed())
                        .findFirst();
                s.setLastPayout(last.map(ap -> ap.getPayoutDate().format(DATE_FMT)).orElse("N/A"));

                summaries.add(s);
            }

            // sort by pending amount desc
            summaries = summaries.stream()
                    .sorted(Comparator.comparing(ArtistEarningSummaryDTO::getPendingAmount).reversed())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            addError("Failed to build summaries: " + e.getMessage());
        }
    }

    // --- Dialog actions ---

    public void openPendingDialog(ArtistEarningSummaryDTO summary) {
        this.selectedSummary = summary;
        this.selectedArtistId = summary.getArtistId();
        try {
            this.pendingList = adminEJB.getPendingEarningsByArtist(selectedArtistId);
            // Map EJB payout list into DTOs for the view
            List<ArtistPayout> payouts = adminEJB.listArtistPayouts(selectedArtistId, 0, 50);
            payoutHistory = payouts.stream().map(p -> {
                ArtistPayoutDTO dto = new ArtistPayoutDTO();
                dto.setPayoutId(p.getPayoutId());
                dto.setArtistId(p.getArtist() != null ? p.getArtist().getUserId() : null);
                dto.setArtistName(p.getArtist() != null ? p.getArtist().getFullName() : null);
                dto.setAmount(p.getAmount());
                dto.setNotes(p.getNotes());
                dto.setPayoutStatus(p.getPayoutStatus());
                dto.setPayoutDate(p.getPayoutDate());
                dto.setCreatedAt(p.getCreatedAt());
                return dto;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            addError("Failed to load pending list: " + e.getMessage());
            this.pendingList = Collections.emptyList();
            this.payoutHistory = Collections.emptyList();
        }
    }

    public void preparePay(ArtistEarningSummaryDTO summary) {
        this.selectedSummary = summary;
        this.selectedArtistId = summary.getArtistId();
        this.payoutNotes = null;
    }

    public void paySelectedArtist() {
        if (selectedArtistId == null) {
            addError("No artist selected for payout.");
            return;
        }
        if (selectedSummary == null || selectedSummary.getPendingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            addError("No pending amount to pay for selected artist.");
            return;
        }

        try {
            Long adminId = resolveCurrentAdminId();
            Long payoutId = adminEJB.payArtist(selectedArtistId, adminId, payoutNotes);
            addInfo("Payout successful (ID: " + payoutId + ").");
            // refresh data
            loadAll();
            // refresh pending list if dialog open
            if (selectedArtistId != null) openPendingDialog(selectedSummary);
        } catch (Exception e) {
            addError("Payout failed: " + e.getMessage());
        }
    }

    // --- Utilities ---
    private Long resolveCurrentAdminId() {
        try {
            String username = FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal() != null
                    ? FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal().getName()
                    : null;
            if (username != null && !username.isBlank()) {
                return adminEJB.getUserIdByUsername(username);
            }
        } catch (Exception ignored) {}
        return 1L; // fallback
    }

    private void addInfo(String m) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, m, null));
    }
    private void addError(String m) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, m, null));
    }

    // --- Getters / Setters used by XHTML ---
    public List<ArtistEarningSummaryDTO> getSummaries() { return summaries; }
    public BigDecimal getTotalPendingAllArtists() { return totalPendingAllArtists.setScale(2); }
    public BigDecimal getTotalPaidAllArtists() { return totalPaidAllArtists.setScale(2); }
    public long getPendingItemsCount() { return pendingItemsCount; }
    public int getArtistsCount() { return artists == null ? 0 : artists.size(); }

    public ArtistEarningSummaryDTO getSelectedSummary() { return selectedSummary; }
    public List<ArtistPendingEarningDTO> getPendingList() { return pendingList; }
    public List<ArtistPayoutDTO> getPayoutHistory() { return payoutHistory; }

    public String getPayoutNotes() { return payoutNotes; }
    public void setPayoutNotes(String payoutNotes) { this.payoutNotes = payoutNotes; }
}

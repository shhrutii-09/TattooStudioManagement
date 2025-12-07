package ejb;

import dto.AdminProfileDTO;
import entities.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AdminEJBLocal {

    // -------- User Management --------
    List<AppUser> listAllUsers();
    
    AppUser getUserById(Long userId);
    void deactivateUser(Long userId, boolean deactivate, String reason);
    void verifyArtist(Long artistId, boolean verify, Long verifyingAdminId);
 

    Long getUserIdByUsername(String username);
    
    // -------- Announcements --------
    Announcement createAnnouncement(Long postedByAdminId, String title, String message, String targetRole);
    Announcement updateAnnouncement(Long announcementId, String title, String message, String targetRole);
    void deleteAnnouncement(Long announcementId);

    // -------- Tattoo Designs --------
    List<TattooDesign> listDesigns(int offset, int limit);
    void deleteDesign(Long designId);

    // -------- Appointments --------
    List<Appointment> listAllAppointments(int offset, int limit);
    Appointment getAppointment(Long appointmentId);
    // Note: Admin may use this to move from PAID to COMPLETED after service.
    void changeAppointmentStatus(Long appointmentId, String status, String cancellationReason); 
    void assignSlotToAppointment(Long appointmentId, Integer slotId);

    // -------- Time Slots --------
    void blockTimeSlot(Integer slotId, Long adminId, String reason);
    void unblockTimeSlot(Integer slotId, Long adminId);

    // ----------------------------------------------------
    // -------- Payments & Earnings (Feature 7 & 9 - Admin Oversight) --------
    // ----------------------------------------------------
    
    // Feature 7: Admin can view payment logs.
    List<Payment> listPayments(int offset, int limit);
    
    // Admin manually marks payment status (triggers EarningLog creation if COMPLETED).
    void markPaymentStatus(Integer paymentId, String status, Long adminId);

    // Internal system logic: Creates the EarningLog upon successful payment.
    EarningLog logEarningsForPayment(Integer paymentId);

    // Feature 9: Calculates the total net (pending/unpaid) earnings for an artist in a period.
    BigDecimal calculateArtistPendingEarnings(Long artistId, LocalDate fromDate, LocalDate toDate);

    // Feature 9: Simulates the monthly payout and marks the relevant EarningLog entries as PAID.
    Long simulatePayout(Long artistId, LocalDate forMonth, BigDecimal amount, Long adminId);
    
    // Feature 9: Admin view of historical payouts.
    List<ArtistPayout> listArtistPayouts(Long artistId, int offset, int limit); 

    // -------- Feedback & Reports --------
    List<Feedback> listFeedbacks(int offset, int limit);
    Map<String, Object> generateSimpleReports(LocalDate fromDate, LocalDate toDate);

    // -------- Medical Forms --------
    List<MedicalForm> listMedicalForms(int offset, int limit);
    void approveMedicalForm(Integer formId, Long adminId);
    
    
    
    //additionally added
    long countTotalUsers();
long countTotalArtists();
long countTotalClients();
long countTotalBookings();
long countTodaysAppointments();

// Recent activity
List<Appointment> listRecentAppointments(int limit);

// New Method Signature
double calculateTotalEarnings(); // Or use BigDecimal for better currency handling

AdminProfileDTO getAdminProfile(Long adminId);
boolean updateAdminDetails(AdminProfileDTO dto);
boolean changeAdminPassword(Long adminId, String oldPassword, String newPassword);

void deleteUser(Long userId);

List<AppUser> listArtists();

}
// file: ejb/AdminEJBLocal.java - COMPLETE VERSION
package ejb;

import dto.AdminProfileDTO;
import dto.AppointmentDTO;
import dto.AppointmentFilterDTO;
import dto.ArtistPendingEarningDTO;
import dto.MedicalFormDTO;
import dto.MedicalFormFilterDTO;
import dto.TimeSlotDTO;
import dto.TimeSlotFilterDTO;
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
    void deleteUser(Long userId);
    
    // -------- Artist Management --------
    List<AppUser> listArtists();
    
    // -------- Announcements --------
    Announcement createAnnouncement(Long postedByAdminId, String title, String message, String targetRole);
    Announcement updateAnnouncement(Long announcementId, String title, String message, String targetRole);
    void deleteAnnouncement(Long announcementId);
    
    // -------- Tattoo Designs --------
    List<TattooDesign> listDesigns(int offset, int limit);
    void banDesign(Long designId, String reason, Long adminId);
    void unbanDesign(Long designId, Long adminId);
    void deleteDesign(Long designId);
    
    // -------- Appointments --------
    List<Appointment> listAllAppointments(int offset, int limit);
    Appointment getAppointment(Long appointmentId);
//    void changeAppointmentStatus(Long appointmentId, String status, String cancellationReason);
    void assignSlotToAppointment(Long appointmentId, Integer slotId);
    
    // Appointment Management with DTO
    List<AppointmentDTO> getFilteredAppointments(AppointmentFilterDTO filter);
    Long countFilteredAppointments(AppointmentFilterDTO filter);
    boolean updateAppointmentStatus(Long appointmentId, String status, String cancellationReason, Long adminId);
    
    // Test/Debug methods
    List<Appointment> testGetAllAppointments();
    String testDatabaseConnection();
    List<Appointment> getAllAppointmentsSimple();
    List<AppointmentDTO> getAppointmentsNative();
    
    // -------- Time Slots --------
    void blockTimeSlot(Integer slotId, Long adminId, String reason);
    void unblockTimeSlot(Integer slotId, Long adminId);
    List<TimeSlot> listAvailableSlots(LocalDate date);
    List<TimeSlot> getAvailableSlots(Long artistId);
    List<TimeSlot> listAvailableSlotsByArtistAndDate(Long artistId, LocalDate date);
    List<TimeSlot> getAvailableSlotsForArtist(Long artistId);
    
    // -------- Payments & Earnings --------
    List<Payment> listPayments(int offset, int limit);
    void markPaymentStatus(Integer paymentId, String status, Long adminId);
    EarningLog logEarningsForPayment(Integer paymentId);
    BigDecimal calculateArtistPendingEarnings(Long artistId, LocalDate fromDate, LocalDate toDate);
    Long simulatePayout(Long artistId, LocalDate forMonth, BigDecimal amount, Long adminId);
    List<ArtistPayout> listArtistPayouts(Long artistId, int offset, int limit);
    
    // -------- Feedback & Reports --------
    List<Feedback> listFeedbacks(int offset, int limit);
    Map<String, Object> generateSimpleReports(LocalDate fromDate, LocalDate toDate);
    
    // -------- Medical Forms --------
    List<MedicalForm> listMedicalForms(int offset, int limit);
    void approveMedicalForm(Integer formId, Long adminId);

    List<MedicalFormDTO> getFilteredMedicalForms(MedicalFormFilterDTO filter);
    Long countFilteredMedicalForms(MedicalFormFilterDTO filter);
    boolean rejectMedicalForm(Integer formId, Long adminId, String reason);
    MedicalFormDTO getMedicalFormDetails(Integer formId);
    List<Appointment> getAppointmentsWithPendingForms();
    List<Appointment> getAppointmentsWithApprovedForms();
    public Map<String, Object> getMedicalFormsStatistics(LocalDate startDate, LocalDate endDate);
    
    // -------- Dashboard Stats --------
    long countTotalUsers();
    long countTotalArtists();
    long countTotalClients();
    long countTotalBookings();
    long countTodaysAppointments();
    List<Appointment> listRecentAppointments(int limit);
    double calculateTotalEarnings();
    Map<String, Long> getDashboardStats();
    Map<String, Long> getAppointmentStatistics(LocalDate startDate, LocalDate endDate);
    
    // -------- Admin Profile --------
    AdminProfileDTO getAdminProfile(Long adminId);
    boolean updateAdminDetails(AdminProfileDTO dto);
    boolean changeAdminPassword(Long adminId, String oldPassword, String newPassword);


    public String testDatabaseSchema();
    String testDataLoad();
    public Appointment getFullAppointmentDetails(Long appointmentId);

    List<TimeSlotDTO> getFilteredTimeSlots(TimeSlotFilterDTO filter);
Long countFilteredTimeSlots(TimeSlotFilterDTO filter);
boolean blockTimeSlotWithNotification(Integer slotId, Long adminId, String reason);
boolean unblockTimeSlotWithNotification(Integer slotId, Long adminId);
List<TimeSlot> getBlockedSlotsForArtist(Long artistId);
List<TimeSlot> getBookedSlotsForArtist(Long artistId);
public List<TimeSlotDTO> debugTimeSlotQuery();
public String debugTimeSlots();

Long countFilteredPayments(String clientName, String artistName,
                           String paymentMethod, String status,
                           LocalDate startDate, LocalDate endDate);
BigDecimal calculateArtistPendingEarningsSummary(Long artistId);
Map<String, Object> getPaymentStatistics(LocalDate startDate, LocalDate endDate);

List<ArtistPendingEarningDTO> getPendingEarningsByArtist(Long artistId);
Long payArtist(Long artistId, Long adminId, String notes);

// Payments / Payout helpers
//List<dto.ArtistPendingEarningDTO> getPendingEarningsByArtist(Long artistId);
//
//Long payArtist(Long artistId, Long adminId, String notes);

// (optional) fetch pending logs directly if you want raw logs
List<entities.EarningLog> getPendingEarningLogsForArtist(Long artistId);


// 1. Get ALL pending/unpaid logs (to calculate current liabilities)
List<ArtistPendingEarningDTO> listAllUnpaidPendingEarnings(); 

// 2. Get ALL paid/processed logs (to calculate net realized profit)
List<ArtistPendingEarningDTO> listAllPaidPendingEarnings(); 

// 3. Get all artist payouts (to confirm total paid)
List<ArtistPayout> listAllArtistPayouts();
BigDecimal getTotalCompletedPaymentsAmount();

Announcement getAnnouncementById(Long announcementId);
List<Announcement> listAllAnnouncements(); // <-- CRITICAL ADDITION (Fixes the current error)

public void createPendingEarningForPaidAppointment(Long appointmentId, BigDecimal totalAmount) throws Exception;
}

package ejb;

import artistDTO.ArtistAppointmentDTO;
import artistDTO.ArtistAppointmentFilterDTO;
import artistDTO.ArtistProfileeeDTO;
import entities.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.ejb.Local;

@Local
public interface ArtistEJBLocal {

    // ---------- Profile ----------
    AppUser getArtistById(Long artistId);
    AppUser updateArtistProfile(Long artistId, String fullName, String phone, String portfolioLink);
    void initializeArtistCollections(AppUser artist);

    // ---------- Designs Management ----------
    /**
     * Creates a new design with duplicate title validation.
     */
     public void addDesign(Long artistId, TattooDesign newDesign);

    /**
     * Updates an existing design.
     */
    public void updateDesign(Long designId, TattooDesign payload);
public List<TimeSlot> getAvailableTimeSlotsForMyAppointment(LocalDate date);
    /**
     * Soft deletes a design (isRemovedByArtist = true). 
     * Requires artistId to ensure ownership.
     */
    void deleteDesign(Long designId, Long artistId);

    /**
     * Gets designs visible to the artist (excluding removed ones).
     * Populates like/favourite counts.
     */
    List<TattooDesign> getArtistDesigns(Long artistId, int offset, int limit);
    
    TattooDesign getDesignById(Long designId);
    
    /**
     * Lists designs that were banned by Admin (visible to artist so they know).
     */
    List<TattooDesign> listBannedDesigns(Long artistId);

    /**
     * Permanently deletes a design (Only allowed if it was already banned/removed).
     */
    void deleteDesignPermanently(Long designId, Long artistId);

    // ---------- Search (Global) ----------
    List<TattooDesign> searchDesigns(String q, String style, BigDecimal minPrice, BigDecimal maxPrice, int offset, int limit);

    // ---------- Likes / Favourites ----------
    DesignLike likeDesign(Long clientId, Long designId);
    void unlikeDesign(Long clientId, Long designId);
    DesignFavourite favouriteDesign(Long clientId, Long designId);
    void unfavouriteDesign(Long clientId, Long designId);

    // ---------- Time Slots & Schedule ----------
    long countPendingRequestsToday(Long artistId);
    List<ArtistSchedule> listSchedulesForArtist(Long artistId);
    List<TimeSlot> getArtistTimeSlots(Long artistId);
    List<TimeSlot> generateTimeSlotsForArtist(Long artistId, LocalDate startDate, LocalDate endDate, int slotDurationMinutes);
    TimeSlot addTimeSlot(Long artistId, TimeSlot slot);
    ArtistSchedule saveArtistSchedule(Long artistId, ArtistSchedule schedule);
    List<TimeSlot> listAvailableTimeSlots(Long artistId);
    void deleteTimeSlot(Integer slotId);
    TimeSlot updateTimeSlot(Integer slotId, LocalDateTime start, LocalDateTime end, TimeSlot.TimeSlotStatus status);
    
    // Blocking
    void markSlotAsBlocked(Integer slotId, String reason);
    boolean blockOwnSlotWithNotification(Integer slotId, Long artistId, String reason);
    boolean unblockOwnSlotWithNotification(Integer slotId, Long artistId);
    List<TimeSlot> getBlockedSlots(Long artistId);

    // ---------- Appointments ----------
    List<Appointment> listArtistAppointments(Long artistId, int offset, int limit);
    Appointment getAppointment(Long appointmentId);
    Appointment changeAppointmentStatus(Long appointmentId, String status, String cancellationReason);
    Appointment addClientNoteToAppointment(Long appointmentId, String clientNote);

    // ---------- Medical Forms ----------
    MedicalForm getMedicalFormForAppointment(Long appointmentId);
    void acknowledgeMedicalForm(Integer formId, Long artistId, boolean approve);

    // ---------- Feedback & Reviews ----------
    List<Feedback> listFeedbackForArtist(Long artistId, int offset, int limit);
    List<Review> listReviewsForArtist(Long artistId, int offset, int limit);

    // ---------- Financials ----------
    List<Payment> listPaymentsForArtist(Long artistId, int offset, int limit);
    List<EarningLog> listEarningLogsForArtist(Long artistId, int offset, int limit);
    BigDecimal calculateArtistPendingEarnings(Long artistId);
    List<ArtistPayout> listArtistPayouts(Long artistId, int offset, int limit);
    
    // ---------- Dashboard Counts ----------
    long countDesignsByArtist(Long artistId);
    long countTodaysAppointments(Long artistId);
    long countUpcomingAppointments(Long artistId);
    long countCompletedAppointments(Long artistId);
    
    double calculateAverageRating(Long artistId);
    double calculateTotalEarnings(Long artistId);
    List<Appointment> listRecentAppointmentsForArtist(Long artistId, int limit);
    
    // In ArtistEJBLocal.java - Add these methods

// Appointment Management
//Appointment approveAppointment(Long appointmentId, Long artistId, Integer slotId);
//Appointment rejectAppointment(Long appointmentId, Long artistId, String rejectionReason);
//Appointment assignTimeSlot(Long appointmentId, Long artistId, Integer slotId);
//Appointment completeAppointment(Long appointmentId, Long artistId);
public List<TimeSlot> getAvailableTimeSlotsForDate(LocalDate date);
public void blockSlot(Integer slotId, String reason);
//    void assignTimeSlot(Long appointmentId, Integer slotId);

void approveAppointment(Long appointmentId, Integer slotId);

void rejectAppointment(Long appointmentId, String reason);

void completeAppointment(Long appointmentId);

    public List<TimeSlot> getMyTimeSlots();
     public List<ArtistSchedule> getMyWeeklySchedule();
// Query methods
List<TimeSlot> getAvailableTimeSlotsForAppointment(Long artistId, LocalDate date);
List<Appointment> getMyAppointments(String statusFilter, int offset, int limit);

// Automatic processing
public void assignTimeSlot(Long appointmentId, Integer slotId, LocalDateTime newStart, LocalDateTime newEnd);


void autoCompletePaidAppointments(Long artistId);

 // =========================
    // WEEKLY SCHEDULE
    // =========================
    void saveSchedule(ArtistSchedule schedule);

    // =========================
    // TIME SLOTS

    public void generateSlotsNext7Days();
    void unblockSlot(Integer slotId);


    ArtistProfileeeDTO getArtistProfile(Long artistId);
boolean updateArtistDetails(ArtistProfileeeDTO dto);
boolean changeArtistPassword(Long artistId, String oldPassword, String newPassword);
boolean saveOrUpdateExperience(ArtistProfileeeDTO dto);


List<ArtistAppointmentDTO> getAppointmentsForArtist(
        Long artistId,
        ArtistAppointmentFilterDTO filter
    );

    void confirmAppointment(Long appointmentId, Long artistId);

    void cancelAppointment(Long appointmentId, String reason, Long artistId);

    void assignSlotToAppointment(Long appointmentId, Integer slotId, Long artistId);

    List<TimeSlot> getAvailableSlotsForArtist(Long artistId);

}
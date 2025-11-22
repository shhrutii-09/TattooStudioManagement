package ejb;

import entities.*;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ArtistEJBLocal - available operations for Artist role
 */
public interface ArtistEJBLocal {

    // ---------- Profile ----------
    AppUser getArtistById(Long artistId);
    AppUser updateArtistProfile(Long artistId, String fullName, String phone, String portfolioLink);

    // ---------- Designs ----------
    List<TattooDesign> getArtistDesigns(Long artistId, int offset, int limit);
    TattooDesign getDesignById(Long designId);
    TattooDesign addDesign(Long artistId, TattooDesign design);
    TattooDesign updateDesign(Long designId, TattooDesign designPayload);
    void deleteDesign(Long designId);

    // search/browse designs (global, may be used by clients too)
    List<TattooDesign> searchDesigns(String q, String style, BigDecimal minPrice, BigDecimal maxPrice, int offset, int limit);

    // likes / favourites
    DesignLike likeDesign(Long clientId, Long designId);
    void unlikeDesign(Long clientId, Long designId);
    DesignFavourite favouriteDesign(Long clientId, Long designId);
    void unfavouriteDesign(Long clientId, Long designId);

    // ---------- Time slots & schedule ----------
    List<ArtistSchedule> listSchedulesForArtist(Long artistId);
    List<TimeSlot> getArtistTimeSlots(Long artistId);
    List<TimeSlot> generateTimeSlotsForArtist(Long artistId, LocalDate startDate, LocalDate endDate, int slotDurationMinutes);
    TimeSlot addTimeSlot(Long artistId, TimeSlot slot);
    ArtistSchedule saveArtistSchedule(Long artistId, ArtistSchedule schedule);
//    List<TimeSlot> generateTimeSlots(Long artistId, LocalDate date);
//    TimeSlot getTimeSlotById(Integer slotId);
//    List<TimeSlot> listTimeSlots(Long artistId, LocalDate date, TimeSlot.TimeSlotStatus status);
    List<TimeSlot> listAvailableTimeSlots(Long artistId);
    void deleteTimeSlot(Integer slotId);
    TimeSlot updateTimeSlot(Integer slotId, LocalDateTime start, LocalDateTime end, TimeSlot.TimeSlotStatus status);
    // block/unblock by artist for own slots (admin can block separately via AdminEJB)
    void markSlotAsBlocked(Integer slotId, String reason);

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

    // -------------------------------------------------------------------
    // ---------- Payments & Earnings (Artist Financial View - Feature 9) ----------
    // -------------------------------------------------------------------
    
    // Artist view of payments made by clients for their appointments (Gross revenue visibility)
    List<Payment> listPaymentsForArtist(Long artistId, int offset, int limit);

    // Artist view of detailed earning logs (Net revenue per appointment)
    List<EarningLog> listEarningLogsForArtist(Long artistId, int offset, int limit);
    
    // Artist view of total net earnings that are currently pending a payout
    BigDecimal calculateArtistPendingEarnings(Long artistId);

    // Artist view of historical payout records
    List<ArtistPayout> listArtistPayouts(Long artistId, int offset, int limit);

    // ---------- Utility ----------
    void initializeArtistCollections(AppUser artist);
}
package ejb;

import entities.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ClientEJBLocal {

    // Profile
    AppUser getClientById(Long clientId);
    AppUser updateClientProfile(Long clientId, String fullName, String phone);

    // Designs (browse/search)
    List<TattooDesign> listDesigns(int offset, int limit);
    List<TattooDesign> searchDesigns(String q, String style, BigDecimal minPrice, BigDecimal maxPrice, int offset, int limit);

    // Likes & favourites
    DesignLike likeDesign(Long clientId, Long designId);
    void unlikeDesign(Long clientId, Long designId);
    DesignFavourite favouriteDesign(Long clientId, Long designId);
    void unfavouriteDesign(Long clientId, Long designId);
    List<DesignFavourite> listFavourites(Long clientId, int offset, int limit);

    // Booking / appointments
    List<TimeSlot> listAvailableTimeSlots(Long artistId, LocalDate date);
    boolean isSlotAvailable(Integer slotId);
    Long bookAppointment(Long clientId, Long artistId, Long designId, Integer slotId, String clientNote);
    void cancelAppointment(Long appointmentId, String reason);

    List<Appointment> listClientAppointments(Long clientId, int offset, int limit);
    Appointment getAppointment(Long appointmentId);

    // Medical form
    MedicalForm submitMedicalForm(Long clientId, Long appointmentId, MedicalForm form);
    MedicalForm getMedicalFormForAppointment(Long appointmentId);

    // -------------------------------------------------------------------
    // ---------- Payments (Feature 7 - Client Interface) ----------
    // -------------------------------------------------------------------
    /**
     * Creates a payment record and simulates a successful transaction, 
     * which then triggers the Appointment status update (to PAID) via AdminEJB.
     */
    Payment createMockPayment(Long clientId, Long appointmentId, BigDecimal amount, String method);
    void updatePaymentStatus(Integer paymentId, String status);
    // Client view of a specific payment
Payment getPaymentByAppointment(Long appointmentId);    
    // Client view of their payment history / receipts
List<Payment> listClientPayments(Long clientId, int offset, int limit);
    // Feedback
    Feedback submitFeedback(Long clientId, Long appointmentId, Integer rating, String comment);

    // Utility
    void initializeClientCollections(AppUser client);
}
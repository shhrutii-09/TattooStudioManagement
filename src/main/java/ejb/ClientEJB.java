package ejb;

import clientDTO.ArtistCardDTO;
import jakarta.persistence.NoResultException;
import entities.*;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.persistence.*;
//import static jakarta.persistence.GenerationType.UUID;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class ClientEJB implements ClientEJBLocal {

    @PersistenceContext(unitName = "TattooPU")
    private EntityManager em;

    @EJB // Inject the ArtistEJB to call its financial logic
    private ArtistEJBLocal artistEJB;
    
    @EJB // Inject AdminEJB to handle payment status and trigger earning logs/appointment status update
    private AdminEJBLocal adminEJB;
    // -------------------------
    // Profile
    // -------------------------
    @Override
    public AppUser getClientById(Long clientId) {
        AppUser client = em.find(AppUser.class, clientId);
        if (client == null) throw new IllegalArgumentException("Client not found: " + clientId);
        initializeClientCollections(client);
        return client;
    }

    @Override
    @Transactional
    public AppUser updateClientProfile(Long clientId, String fullName, String phone) {
        AppUser client = em.find(AppUser.class, clientId);
        if (client == null) throw new IllegalArgumentException("Client not found: " + clientId);
        if (fullName != null) client.setFullName(fullName);
        if (phone != null) client.setPhone(phone);
        return em.merge(client);
    }

    // -------------------------
    // Designs browse / search
    // -------------------------
    @Override
    public List<TattooDesign> listDesigns(int start, int max) {
        return em.createQuery(
            "SELECT d FROM TattooDesign d " +
            "WHERE (d.isBanned IS NULL OR d.isBanned = false) " +
            "AND (d.isRemovedByArtist IS NULL OR d.isRemovedByArtist = false) " +
            "ORDER BY d.uploadedAt DESC", 
            TattooDesign.class)
            .setFirstResult(start)
            .setMaxResults(max)
            .getResultList();
    }


 @Override
public List<TattooDesign> searchDesigns(
        String qStr,
        String style,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        int offset,
        int limit) {

    StringBuilder ql = new StringBuilder(
        "SELECT d FROM TattooDesign d WHERE " +
        "(d.isBanned IS NULL OR d.isBanned = false) " +
        "AND (d.isRemovedByArtist IS NULL OR d.isRemovedByArtist = false) "
    );

    if (qStr != null && !qStr.isBlank()) {
        ql.append(
            "AND (LOWER(d.title) LIKE :q " +
            "OR LOWER(d.description) LIKE :q " +
            "OR LOWER(d.style) LIKE :q " +
            "OR LOWER(d.artist.fullName) LIKE :q) "
        );
    }

    if (style != null && !style.isBlank()) {
        ql.append("AND LOWER(d.style) LIKE :style ");
    }

    if (minPrice != null) {
        ql.append("AND d.price >= :minPrice ");
    }

    if (maxPrice != null) {
        ql.append("AND d.price <= :maxPrice ");
    }

    ql.append("ORDER BY d.uploadedAt DESC");

    TypedQuery<TattooDesign> q = em.createQuery(ql.toString(), TattooDesign.class);

    if (qStr != null && !qStr.isBlank()) {
        q.setParameter("q", "%" + qStr.toLowerCase() + "%");
    }

    if (style != null && !style.isBlank()) {
        q.setParameter("style", "%" + style.toLowerCase() + "%");
    }

    if (minPrice != null) {
        q.setParameter("minPrice", minPrice);
    }

    if (maxPrice != null) {
        q.setParameter("maxPrice", maxPrice);
    }

    if (offset >= 0) q.setFirstResult(offset);
    if (limit > 0) q.setMaxResults(limit);

    List<TattooDesign> list = q.getResultList();

    // Force lazy init safely
    list.forEach(d -> {
        if (d.getArtist() != null) d.getArtist().getUserId();
        if (d.getLikes() != null) d.getLikes().size();
        if (d.getFavourites() != null) d.getFavourites().size();
    });

    return list;
}



    // -------------------------
    // Likes / favourites
    // -------------------------
    @Override
    @Transactional
    public DesignLike likeDesign(Long clientId, Long designId) {
        AppUser client = em.find(AppUser.class, clientId);
        TattooDesign design = em.find(TattooDesign.class, designId);
        if (client == null) throw new IllegalArgumentException("Client not found");
        if (design == null) throw new IllegalArgumentException("Design not found");

        try {
            DesignLike existing = em.createQuery(
                    "SELECT l FROM DesignLike l WHERE l.client.userId = :cid AND l.design.designId = :did", DesignLike.class)
                    .setParameter("cid", clientId).setParameter("did", designId)
                    .getSingleResult();
            return existing;
        } catch (NoResultException ignored) {}

        DesignLike like = new DesignLike();
        like.setClient(client);
        like.setDesign(design);
        em.persist(like);
        em.flush();
        return like;
    }

    @Override
    @Transactional
    public void unlikeDesign(Long clientId, Long designId) {
        try {
            DesignLike l = em.createQuery(
                    "SELECT l FROM DesignLike l WHERE l.client.userId = :cid AND l.design.designId = :did", DesignLike.class)
                    .setParameter("cid", clientId).setParameter("did", designId)
                    .getSingleResult();
            em.remove(l);
        } catch (NoResultException ignored) {}
    }

    @Override
    @Transactional
    public DesignFavourite favouriteDesign(Long clientId, Long designId) {
        AppUser client = em.find(AppUser.class, clientId);
        TattooDesign design = em.find(TattooDesign.class, designId);
        if (client == null) throw new IllegalArgumentException("Client not found");
        if (design == null) throw new IllegalArgumentException("Design not found");

        try {
            DesignFavourite existing = em.createQuery(
                    "SELECT f FROM DesignFavourite f WHERE f.client.userId = :cid AND f.design.designId = :did", DesignFavourite.class)
                    .setParameter("cid", clientId).setParameter("did", designId)
                    .getSingleResult();
            return existing;
        } catch (NoResultException ignored) {}

        DesignFavourite fav = new DesignFavourite();
        fav.setClient(client);
        fav.setDesign(design);
        em.persist(fav);
        em.flush();
        return fav;
    }

 @Override
@Transactional
public void unfavouriteDesign(Long clientId, Long designId) {
    removeFavourite(designId, clientId); // Reuse the corrected method
}

   @Override
    public List<DesignFavourite> listFavourites(Long clientId, int start, int max) {
        return em.createQuery(
            "SELECT f FROM DesignFavourite f " +
            "WHERE f.client.userId = :clientId " +
            "ORDER BY f.favoritedAt DESC", 
            DesignFavourite.class)
            .setParameter("clientId", clientId)
            .setFirstResult(start)
            .setMaxResults(max)
            .getResultList();
    }

    // -------------------------
    // Booking / appointments
    // -------------------------
    // In ClientEJB.java - Update listAvailableTimeSlots method
@Override
public List<TimeSlot> listAvailableTimeSlots(Long artistId, LocalDate date) {

    LocalDateTime start = date.atStartOfDay();
    LocalDateTime end = date.atTime(23, 59, 59);

    return em.createQuery(
        "SELECT t FROM TimeSlot t " +
        "WHERE t.artist.userId = :artistId " +
        "AND t.startTime BETWEEN :start AND :end " +
        "AND t.status = :status",
        TimeSlot.class
    )
    .setParameter("artistId", artistId)
    .setParameter("start", start)
    .setParameter("end", end)
    .setParameter("status", TimeSlot.TimeSlotStatus.AVAILABLE)
    .getResultList();
}



//    @Override
//@Transactional
//public void cancelAppointment(Long appointmentId, String reason) {
//
//    Appointment appt = em.find(Appointment.class, appointmentId);
//    if (appt == null) {
//        return;
//    }
//
//    // Update appointment status
//    appt.setStatus("CANCELLED");
//    em.merge(appt);
//
//    // Release slot
//    TimeSlot slot = appt.getSlot();
//    if (slot != null && slot.getStatus() != TimeSlot.TimeSlotStatus.BLOCKED) {
//        slot.setStatus(TimeSlot.TimeSlotStatus.AVAILABLE);
//        em.merge(slot);
//    }
//}


   @Override
public boolean isSlotAvailable(Integer slotId) {

    if (slotId == null) {
        return false;
    }

    TimeSlot slot = em.find(TimeSlot.class, slotId);

    return slot != null
            && slot.getStatus() == TimeSlot.TimeSlotStatus.AVAILABLE;
}


//     @Override
//@Transactional
//public Long createAppointment(
//        Long clientId,
//        Long artistId,
//        Long designId,
//        Integer slotId,
//        String clientNote
//) {
//
//    AppUser client = em.find(AppUser.class, clientId);
//    AppUser artist = em.find(AppUser.class, artistId);
//    TattooDesign design = em.find(TattooDesign.class, designId);
//    TimeSlot slot = em.find(TimeSlot.class, slotId);
//
//    // ===== VALIDATION =====
//    if (client == null || artist == null || design == null || slot == null) {
//        throw new IllegalArgumentException("Invalid appointment data");
//    }
//
//    // Slot must belong to artist
//    if (!slot.getArtist().getUserId().equals(artistId)) {
//        throw new IllegalStateException("Slot does not belong to artist");
//    }
//
//    // Slot availability check
//    if (slot.getStatus() != TimeSlot.TimeSlotStatus.AVAILABLE) {
//        throw new IllegalStateException("Slot is not available");
//    }
//
//    // ===== LOCK SLOT =====
//    slot.setStatus(TimeSlot.TimeSlotStatus.PENDING_APPOINTMENT);
//    em.merge(slot);
//
//    // ===== CREATE APPOINTMENT =====
//    Appointment appt = new Appointment();
//    appt.setClient(client);
//    appt.setArtist(artist);
//    appt.setDesign(design);
//    appt.setSlot(slot);
//    appt.setClientNote(clientNote);
//    appt.setStatus("PENDING");
//    appt.setAppointmentDateTime(slot.getStartTime());
//    appt.setCancellationReason(null);
//
//    em.persist(appt);
//    em.flush();
//
//    return appt.getAppointmentId();
//}

    // In ClientEJB.java - Update the bookAppointment method
@Override
@Transactional
public Long bookAppointment(Long clientId, Long artistId, Long designId, String clientNote) {

    AppUser client = em.find(AppUser.class, clientId);
    AppUser artist = em.find(AppUser.class, artistId);
    TattooDesign design = em.find(TattooDesign.class, designId);

    if (client == null || artist == null || design == null) {
        throw new IllegalArgumentException("Invalid booking data");
    }

    Appointment appointment = new Appointment();
    appointment.setClient(client);
    appointment.setArtist(artist);
    appointment.setDesign(design);
    
    // 🔹 ADD THIS LINE - set request time to when client submits the booking
    appointment.setRequestDateTime(LocalDateTime.now());
    
    appointment.setClientNote(clientNote);
    appointment.setStatus("PENDING");

    // NO SLOT, NO DATE - artist will assign later
    appointment.setSlot(null);
    appointment.setAppointmentDateTime(null);

    em.persist(appointment);
    em.flush();

    return appointment.getAppointmentId();
}
@Override
@Transactional
public void cancelAppointment(Long appointmentId) {

    Appointment appt = em.find(Appointment.class, appointmentId);
    if (appt == null) return;

    // Update appointment
    appt.setStatus("CANCELLED");
    em.merge(appt);

    // Free slot correctly
    TimeSlot slot = appt.getSlot();
    if (slot != null) {
        slot.setStatus(TimeSlot.TimeSlotStatus.AVAILABLE);
        em.merge(slot);
    }
}


//      @Override
//    public List<Appointment> listClientAppointments(Long clientId) {
//        return em.createQuery(
//                "SELECT a FROM Appointment a " +
//                "WHERE a.client.userId = :clientId" +
//                "ORDER BY a.createdAt DESC",
//                Appointment.class
//        )
//        .setParameter("clientId", clientId)
//        .getResultList();
//    }


    @Override
public Appointment getAppointment(Long appointmentId) {
    Appointment a = em.find(Appointment.class, appointmentId);
    if (a == null) throw new IllegalArgumentException("Appointment not found");
    if (a.getArtist() != null) a.getArtist().getUserId();
    if (a.getDesign() != null) a.getDesign().getDesignId();
    if (a.getSlot() != null) a.getSlot().getSlotId();
    if (a.getPayment() != null) a.getPayment().getPaymentId();
    return a;
}


    // -------------------------
    // Medical form
    // -------------------------
    @Override
    @Transactional
    public MedicalForm submitMedicalForm(Long clientId, Long appointmentId, MedicalForm form) {
        AppUser client = em.find(AppUser.class, clientId);
        Appointment a = em.find(Appointment.class, appointmentId);
        if (client == null) throw new IllegalArgumentException("Client not found");
        if (a == null) throw new IllegalArgumentException("Appointment not found");

        // ensure appointment belongs to client
        if (!a.getClient().getUserId().equals(clientId)) throw new IllegalArgumentException("Appointment does not belong to client");

        // Ensure only one MedicalForm per appointment (appointment has one-to-one)
        try {
            MedicalForm existing = em.createQuery("SELECT m FROM MedicalForm m WHERE m.appointment.appointmentId = :aid", MedicalForm.class)
                    .setParameter("aid", appointmentId)
                    .getSingleResult();
            // update existing
            existing.setAllergyDetails(form.getAllergyDetails());
            existing.setDiabetes(form.getDiabetes());
            existing.setHasAllergies(form.getHasAllergies());
            existing.setHeartCondition(form.getHeartCondition());
            existing.setIsMinor(form.getIsMinor());
            existing.setIsPregnant(form.getIsPregnant());
            existing.setSubmittedAt(LocalDateTime.now());
            existing.setClient(client);
            return em.merge(existing);
        } catch (NoResultException ignored) {}

        MedicalForm m = new MedicalForm();
        m.setAllergyDetails(form.getAllergyDetails());
        m.setDiabetes(form.getDiabetes());
        m.setHasAllergies(form.getHasAllergies());
        m.setHeartCondition(form.getHeartCondition());
        m.setIsMinor(form.getIsMinor());
        m.setIsPregnant(form.getIsPregnant());
        m.setSubmittedAt(LocalDateTime.now());
        m.setClient(client);
        m.setAppointment(a);
        em.persist(m);

        // link appointment (bidirectional mapping managed by Appointment having mappedBy; ensure appointment gets reference)
        a.setMedicalForm(m);
        em.merge(a);

        em.flush();
        return m;
    }

    @Override
    public MedicalForm getMedicalFormForAppointment(Long appointmentId) {
        try {
            MedicalForm m = em.createQuery("SELECT m FROM MedicalForm m WHERE m.appointment.appointmentId = :aid", MedicalForm.class)
                    .setParameter("aid", appointmentId)
                    .getSingleResult();
            if (m.getClient() != null) m.getClient().getUserId();
            if (m.getValidatedBy() != null) m.getValidatedBy().getUserId();
            return m;
        } catch (NoResultException ex) {
            return null;
        }
    }

    // -------------------------
    // Payments (mock)
    // -------------------------
    @Override
@Transactional
public Payment createMockPayment(Long clientId, Long appointmentId, BigDecimal amount, String method) {
    AppUser client = getClientById(clientId);
    Appointment a = getAppointment(appointmentId);

    if (a.getStatus() == null || !"CONFIRMED".equals(a.getStatus())) {
        throw new IllegalStateException("Appointment must be 'CONFIRMED' to process payment.");
    }
    if (a.getPayment() != null) {
        throw new IllegalStateException("Payment already exists for appointment: " + appointmentId);
    }
    
    // 1. Create and persist Payment entity
    Payment payment = new Payment();
    payment.setClient(client);
    payment.setAppointment(a);
    payment.setAmount(amount);
    payment.setPaymentMethod(method);
    payment.setStatus("COMPLETED"); // Assume success for mock payment
    payment.setTransactionId(java.util.UUID.randomUUID().toString()); // Mock Transaction ID
    
    em.persist(payment);
    em.flush(); // Ensure the Payment ID is generated before calling AdminEJB

    // 2. CRITICAL: Trigger the back-end financial workflow (Post-processing)
    Long internalAdminId = 99L; // Placeholder for system/auditing
    
    try {
        // This call executes the commission logic and updates the Appointment status.
        adminEJB.markPaymentStatus(payment.getPaymentId(), "COMPLETED", internalAdminId); 

    } catch (Exception e) {
        // If AdminEJB fails, the transaction attempts to roll back. We throw the informative error.
        System.err.println("CRITICAL: Failed to update Admin/Earning log after payment completion for ID: " + payment.getPaymentId() + ". Error: " + e.getMessage());
        
        // This is the specific error message the client will see.
        throw new jakarta.ejb.EJBException("Payment was successful but back-end logging failed. Manual review required.", e);
    }
    
    return payment;
}

    @Override
    @Transactional
    public void updatePaymentStatus(Integer paymentId, String status) {
        Payment payment = em.find(Payment.class, paymentId);
        if (payment == null) throw new IllegalArgumentException("Payment not found: " + paymentId);

        String currentStatus = payment.getStatus();
        
        payment.setStatus(status); 
        em.merge(payment);

        // Trigger Earning Log creation ONLY if payment is newly COMPLETED
        if ("COMPLETED".equalsIgnoreCase(status) && !"COMPLETED".equalsIgnoreCase(currentStatus)) {
            // This calls the financial logic in the ArtistEJB
            adminEJB.logEarningsForPayment(paymentId);
        }
    }

    @Override
    public Payment getPaymentByAppointment(Long appointmentId) {
        try {
             return em.createQuery(
                "SELECT p FROM Payment p WHERE p.appointment.appointmentId = :aid", 
                Payment.class
            ).setParameter("aid", appointmentId).getSingleResult();
        } catch (NoResultException e) {
            throw new NoResultException("Payment not found for appointment ID: " + appointmentId);
        }
    }

    @Override
    public List<Payment> listClientPayments(Long clientId, int offset, int limit) {
        TypedQuery<Payment> q = em.createQuery("SELECT p FROM Payment p WHERE p.client.userId = :cid ORDER BY p.paymentDate DESC", Payment.class)
                .setParameter("cid", clientId);
        if (offset >= 0) q.setFirstResult(offset);
        if (limit > 0) q.setMaxResults(limit);
        List<Payment> list = q.getResultList();
        list.forEach(p -> {
            if (p.getAppointment() != null) p.getAppointment().getAppointmentId();
        });
        return list;
    }

    // -------------------------
    // Feedback
    // -------------------------
    @Override
    @Transactional
    public Feedback submitFeedback(Long clientId, Long appointmentId, Integer rating, String comment) {
        AppUser client = em.find(AppUser.class, clientId);
        Appointment a = em.find(Appointment.class, appointmentId);
        if (client == null) throw new IllegalArgumentException("Client not found");
        if (a == null) throw new IllegalArgumentException("Appointment not found");
        if (!a.getClient().getUserId().equals(clientId)) throw new IllegalArgumentException("Appointment does not belong to client");
        if (!"COMPLETED".equalsIgnoreCase(a.getStatus())) throw new IllegalStateException("Feedback allowed only after appointment is COMPLETED");

        // prevent duplicate feedback for same appointment
        try {
            Feedback existing = em.createQuery("SELECT f FROM Feedback f WHERE f.appointment.appointmentId = :aid", Feedback.class)
                    .setParameter("aid", appointmentId)
                    .getSingleResult();
            throw new IllegalStateException("Feedback already submitted for this appointment");
        } catch (NoResultException ignored) {}

        Feedback f = new Feedback();
        f.setClient(client);
        f.setArtist(a.getArtist());
        f.setAppointment(a);
        f.setRating(rating);
        f.setComment(comment);
        f.setCreatedAt(LocalDateTime.now());
        em.persist(f);

        // attach to appointment (bidirectional)
        a.setFeedback(f);
        em.merge(a);
        em.flush();
        return f;
    }

    // -------------------------
    // Utility
    // -------------------------
    @Override
    public void initializeClientCollections(AppUser client) {
        if (client == null) return;
        if (client.getClientAppointments() != null) client.getClientAppointments().size();
        if (client.getDesigns() != null) client.getDesigns().size();
        if (client.getLoginHistory() != null) client.getLoginHistory().size();
        if (client.getTimeSlots() != null) client.getTimeSlots().size();
        if (client.getFeedbacksReceived() != null) client.getFeedbacksReceived().size();
    }
    

    // ========== NEW METHODS FOR DYNAMIC HOME PAGE ==========

   @Override
public List<TattooDesign> listDesignsByStyle(String style, int start, int max) {
    return em.createQuery(
        "SELECT d FROM TattooDesign d " +
        "WHERE (:style IS NULL OR LOWER(d.style) LIKE :style) " +
        "AND (d.isBanned IS NULL OR d.isBanned = false) " +
        "AND (d.isRemovedByArtist IS NULL OR d.isRemovedByArtist = false) " +
        "ORDER BY d.uploadedAt DESC",
        TattooDesign.class)
        .setParameter("style", style == null ? null : "%" + style.toLowerCase() + "%")
        .setFirstResult(start)
        .setMaxResults(max)
        .getResultList();
}


    @Override
    public List<TattooDesign> getPopularDesigns(int start, int max) {
        return em.createQuery(
            "SELECT d FROM TattooDesign d " +
            "WHERE (d.isBanned IS NULL OR d.isBanned = false) " +
            "AND (d.isRemovedByArtist IS NULL OR d.isRemovedByArtist = false) " +
            "ORDER BY SIZE(d.favourites) DESC, SIZE(d.likes) DESC, d.uploadedAt DESC", 
            TattooDesign.class)
            .setFirstResult(start)
            .setMaxResults(max)
            .getResultList();
    }

    @Override
    public List<TattooDesign> getTrendingDesigns(int start, int max) {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        
        return em.createQuery(
            "SELECT d FROM TattooDesign d " +
            "WHERE d.uploadedAt >= :oneMonthAgo " +
            "AND (d.isBanned IS NULL OR d.isBanned = false) " +
            "AND (d.isRemovedByArtist IS NULL OR d.isRemovedByArtist = false) " +
            "ORDER BY SIZE(d.favourites) + SIZE(d.likes) DESC, d.uploadedAt DESC", 
            TattooDesign.class)
            .setParameter("oneMonthAgo", oneMonthAgo)
            .setFirstResult(start)
            .setMaxResults(max)
            .getResultList();
    }

    @Override
    public List<TattooDesign> getNewDesigns(int start, int max) {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        
        return em.createQuery(
            "SELECT d FROM TattooDesign d " +
            "WHERE d.uploadedAt >= :oneWeekAgo " +
            "AND (d.isBanned IS NULL OR d.isBanned = false) " +
            "AND (d.isRemovedByArtist IS NULL OR d.isRemovedByArtist = false) " +
            "ORDER BY d.uploadedAt DESC", 
            TattooDesign.class)
            .setParameter("oneWeekAgo", oneWeekAgo)
            .setFirstResult(start)
            .setMaxResults(max)
            .getResultList();
    }

    @Override
    public List<AppUser> getTopArtists(int start, int max) {
        // Get artists with highest average feedback ratings
        return em.createQuery(
            "SELECT a, COALESCE(AVG(f.rating),0.0) as avgRating, COUNT(f) as reviewCount\n" +
"FROM AppUser a\n" +
"LEFT JOIN Feedback f ON f.artist = a\n" +
"WHERE a.role.roleName = 'ARTIST' AND a.isActive = true\n" +
"GROUP BY a\n" +
"ORDER BY avgRating DESC, reviewCount DESC", 
            Object[].class)
            .setFirstResult(start)
            .setMaxResults(max)
            .getResultStream()
            .map(result -> (AppUser) result[0])
            .collect(Collectors.toList());
    }

    @Override
    public List<TattooDesign> getFeaturedDesigns(int start, int max) {
        // Featured designs could be marked by admin or based on criteria
        return em.createQuery(
            "SELECT d FROM TattooDesign d " +
            "WHERE (d.isBanned IS NULL OR d.isBanned = false) " +
            "AND (d.isRemovedByArtist IS NULL OR d.isRemovedByArtist = false) " +
            "AND d.price > :minPrice " + // Example: featured designs are premium
            "ORDER BY d.uploadedAt DESC", 
            TattooDesign.class)
            .setParameter("minPrice", new BigDecimal("100"))
            .setFirstResult(start)
            .setMaxResults(max)
            .getResultList();
    }

    @Override
public List<String> getAvailableStyles() {
    return em.createQuery(
        "SELECT DISTINCT LOWER(TRIM(d.style)) " +
        "FROM TattooDesign d " +
        "WHERE d.style IS NOT NULL " +
        "AND d.style <> '' " +
        "AND (d.isBanned IS NULL OR d.isBanned = false) " +
        "AND (d.isRemovedByArtist IS NULL OR d.isRemovedByArtist = false) " +
        "ORDER BY LOWER(TRIM(d.style))",
        String.class)
        .getResultList();
}


    @Override
    public Long getClientAppointmentCount(Long clientId) {
        return em.createQuery(
            "SELECT COUNT(a) FROM Appointment a " +
            "WHERE a.client.userId = :clientId", 
            Long.class)
            .setParameter("clientId", clientId)
            .getSingleResult();
    }

    @Override
    public Long getUpcomingAppointmentCount(Long clientId) {
        return em.createQuery(
            "SELECT COUNT(a) FROM Appointment a " +
            "WHERE a.client.userId = :clientId " +
            "AND a.status = 'CONFIRMED' " +
            "AND a.appointmentDateTime > :now", 
            Long.class)
            .setParameter("clientId", clientId)
            .setParameter("now", LocalDateTime.now())
            .getSingleResult();
    }

    @Override
    public Long getClientFavouritesCount(Long clientId) {
        return em.createQuery(
            "SELECT COUNT(f) FROM DesignFavourite f " +
            "WHERE f.client.userId = :clientId", 
            Long.class)
            .setParameter("clientId", clientId)
            .getSingleResult();
    }

    @Override
    public Long getPendingMedicalFormsCount(Long clientId) {
        return em.createQuery(
            "SELECT COUNT(a) FROM Appointment a " +
            "WHERE a.client.userId = :clientId " +
            "AND a.status = 'PENDING' " +
            "AND (a.medicalForm IS NULL OR a.medicalForm.isApproved = false)", 
            Long.class)
            .setParameter("clientId", clientId)
            .getSingleResult();
    }

    @Override
    public List<Appointment> getUpcomingAppointments(Long clientId, int start, int max) {
        return em.createQuery(
            "SELECT a FROM Appointment a " +
            "WHERE a.client.userId = :clientId " +
            "AND a.status = 'CONFIRMED' " +
            "AND a.appointmentDateTime > :now " +
            "ORDER BY a.appointmentDateTime ASC", 
            Appointment.class)
            .setParameter("clientId", clientId)
            .setParameter("now", LocalDateTime.now())
            .setFirstResult(start)
            .setMaxResults(max)
            .getResultList();
    }

    // ========== HELPER METHODS ==========

    @Override
public List<TattooDesign> getDesignsByArtist(Long artistId, int start, int max) {

    return em.createQuery(
        """
        SELECT d
        FROM TattooDesign d
        WHERE d.artist.userId = :artistId
          AND d.isBanned = false
          AND d.isRemovedByArtist = false
        ORDER BY d.uploadedAt DESC
        """,
        TattooDesign.class
    )
    .setParameter("artistId", artistId)
    .setFirstResult(start)
    .setMaxResults(max)
    .getResultList();
}

   @Override
public Double getArtistAverageRating(Long artistId) {

    Double avg = em.createQuery(
        """
        SELECT AVG(r.rating)
        FROM Review r
        WHERE r.artist.userId = :artistId
        """,
        Double.class
    )
    .setParameter("artistId", artistId)
    .getSingleResult();

    return avg != null ? avg : 0.0;
}


    @Override
    public List<AppUser> getAllActiveArtists(int start, int max) {
        return em.createQuery(
            "SELECT a FROM AppUser a " +
            "WHERE a.role.roleName = 'ARTIST' " +
            "AND a.isActive = true " +
            "ORDER BY a.fullName", 
            AppUser.class)
            .setFirstResult(start)
            .setMaxResults(max)
            .getResultList();
    }

    @Override
    public List<TattooDesign> searchDesigns(String keyword, int start, int max) {
        return em.createQuery(
            "SELECT d FROM TattooDesign d " +
            "WHERE (LOWER(d.title) LIKE LOWER(:keyword) " +
            "OR LOWER(d.description) LIKE LOWER(:keyword) " +
            "OR LOWER(d.style) LIKE LOWER(:keyword)) " +
            "AND (d.isBanned IS NULL OR d.isBanned = false) " +
            "AND (d.isRemovedByArtist IS NULL OR d.isRemovedByArtist = false) " +
            "ORDER BY d.uploadedAt DESC", 
            TattooDesign.class)
            .setParameter("keyword", "%" + keyword + "%")
            .setFirstResult(start)
            .setMaxResults(max)
            .getResultList();
    }
    
@Override
@Transactional
public boolean toggleDesignLike(Long clientId, Long designId) {

    try {
        DesignLike l = em.createQuery(
            "SELECT l FROM DesignLike l WHERE l.client.userId = :cid AND l.design.designId = :did",
            DesignLike.class)
            .setParameter("cid", clientId)
            .setParameter("did", designId)
            .getSingleResult();

        // if found → remove it
        em.remove(l);
        return false; // now unliked
    }
    catch (NoResultException e) {
        // if not found → create like
        AppUser c = em.find(AppUser.class, clientId);
        TattooDesign d = em.find(TattooDesign.class, designId);

        DesignLike like = new DesignLike();
        like.setClient(c);
        like.setDesign(d);
        em.persist(like);
        return true; // now liked
    }
}


@Override
public List<Long> listLikedDesignIds(Long clientId) {
    return em.createQuery(
            "SELECT l.design.designId FROM DesignLike l WHERE l.client.userId = :cid",
            Long.class)
        .setParameter("cid", clientId)
        .getResultList();
}

@Override
@Transactional
public boolean toggleFavouriteDesign(Long clientId, Long designId) {
    AppUser client = em.find(AppUser.class, clientId);
    TattooDesign design = em.find(TattooDesign.class, designId);

    if (client == null || design == null) {
        throw new IllegalArgumentException("Client or Design not found.");
    }

    try {
        DesignFavourite existing = em.createQuery(
                "SELECT f FROM DesignFavourite f WHERE f.client.userId = :cid AND f.design.designId = :did",
                DesignFavourite.class)
            .setParameter("cid", clientId)
            .setParameter("did", designId)
            .getSingleResult();

        em.remove(existing);
        return false; // removed from favourite
    } catch (NoResultException e) {
        DesignFavourite fav = new DesignFavourite();
        fav.setClient(client);
        fav.setDesign(design);
        em.persist(fav);
        return true; // added to favourite
    }
}

@Override
public List<Long> listFavouriteDesignIds(Long clientId) {
    return em.createQuery(
            "SELECT f.design.designId FROM DesignFavourite f WHERE f.client.userId = :cid",
            Long.class)
        .setParameter("cid", clientId)
        .getResultList();
}

@Override
public TattooDesign getDesignById(Long id) {
    TattooDesign d = em.find(TattooDesign.class, id);

    if (d == null) return null;
    if (Boolean.TRUE.equals(d.getIsBanned())) return null;
    if (Boolean.TRUE.equals(d.getIsRemovedByArtist())) return null;

    return d;
}

    
@Override
public List<Review> getReviewsForDesign(Long designId) {
    String jpql = "SELECT r FROM Review r WHERE r.design.designId = :designId ORDER BY r.createdAt DESC";
    return em.createQuery(jpql, Review.class)
            .setParameter("designId", designId)
            .getResultList();
}
    @Override
    public List<TattooDesign> getRelatedDesigns(Long designId, Long artistId, String style) {
        try {
            String jpql = "SELECT d FROM TattooDesign d " +
                         "WHERE d.designId <> :designId " +
                         "AND (d.artist.userId = :artistId OR d.style = :style) " +
                         "AND (d.isBanned IS NULL OR d.isBanned = false) " +
                         "AND (d.isRemovedByArtist IS NULL OR d.isRemovedByArtist = false) " +
                         "ORDER BY d.uploadedAt DESC";
            
            return em.createQuery(jpql, TattooDesign.class)
                    .setParameter("designId", designId)
                    .setParameter("artistId", artistId)
                    .setParameter("style", style)
                    .setMaxResults(8)
                    .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
    
@Override
public List<TattooDesign> getRecommendedDesigns(Long clientId) {
    // 1. Get the styles of designs liked by the client
    List<String> likedStyles = em.createQuery(
        "SELECT DISTINCT LOWER(l.design.style) " +
        "FROM DesignLike l " +
        "WHERE l.client.userId = :clientId " +
        "AND l.design.style IS NOT NULL", 
        String.class)
        .setParameter("clientId", clientId)
        .getResultList();

    List<TattooDesign> recommended = Collections.emptyList();

    if (!likedStyles.isEmpty()) {
        // 2. Get designs with matching styles, excluding ones already liked
        recommended = em.createQuery(
            "SELECT DISTINCT d FROM TattooDesign d " +
            "WHERE LOWER(d.style) IN :styles " +
            "AND (d.isBanned IS NULL OR d.isBanned = false) " +
            "AND (d.isRemovedByArtist IS NULL OR d.isRemovedByArtist = false) " +
            "AND d.designId NOT IN (" +
            "   SELECT l.design.designId FROM DesignLike l WHERE l.client.userId = :clientId" +
            ") " +
            "ORDER BY d.uploadedAt DESC", 
            TattooDesign.class)
            .setParameter("styles", likedStyles)
            .setParameter("clientId", clientId)
            .setMaxResults(8)
            .getResultList();
    }

    // 3. If less than 8 designs, fill with trending designs
    if (recommended.size() < 8) {
    List<TattooDesign> trending = getTrendingDesigns(0, 8 - recommended.size());
    for (TattooDesign d : trending) {
        if (!recommended.contains(d)) {
            recommended.add(d);
        }
    }
}
    // 4. Initialize lazy collections to prevent LazyInitializationException
    recommended.forEach(d -> {
        if (d.getArtist() != null) d.getArtist().getUserId();
        if (d.getLikes() != null) d.getLikes().size();
        if (d.getFavourites() != null) d.getFavourites().size();
    });

    return recommended;
}
   
    @Override
public AppUser getArtistInfo(Long artistId) {
    if (artistId == null) return null;

    try {
        return em.createQuery(
            """
            SELECT a
            FROM AppUser a
            WHERE a.userId = :id
              AND a.role.roleName = 'ARTIST'
              AND a.isActive = true
            """,
            AppUser.class
        )
        .setParameter("id", artistId)
        .getSingleResult();

    } catch (NoResultException e) {
        return null;
    }
}


 @Override
    public List<DesignComment> getDesignComments(Long designId) {
        try {
            String jpql = "SELECT c FROM DesignComment c " +
                         "LEFT JOIN FETCH c.client " +
                         "WHERE c.design.designId = :id " +
                         "ORDER BY c.createdAt DESC";
            
            return em.createQuery(jpql, DesignComment.class)
                     .setParameter("id", designId)
                     .getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

@Override
public DesignComment addDesignComment(Long clientId, Long designId, String text) {
    AppUser client = em.find(AppUser.class, clientId);
    TattooDesign design = em.find(TattooDesign.class, designId);

    if (client == null || design == null) return null;

    DesignComment comment = new DesignComment();
    comment.setClient(client);
    comment.setDesign(design);
    comment.setText(text);

    em.persist(comment);
    return comment;
}

@Override
@Transactional
public void addLike(Long designId, Long clientId) {
    System.out.println("DEBUG addLike: designId=" + designId + ", clientId=" + clientId);
    
    try {
        // Check if like already exists
        Long count = em.createQuery(
            "SELECT COUNT(l) FROM DesignLike l WHERE l.client.userId = :clientId AND l.design.designId = :designId", 
            Long.class)
            .setParameter("clientId", clientId)
            .setParameter("designId", designId)
            .getSingleResult();
        
        System.out.println("DEBUG: Existing count = " + count);
        
        if (count > 0) {
            System.out.println("DEBUG: Like already exists, skipping");
            return; // Already liked
        }
        
        // Create new like
        TattooDesign design = em.find(TattooDesign.class, designId);
        AppUser client = em.find(AppUser.class, clientId);
        
        if (design == null) {
            System.err.println("ERROR: Design not found: " + designId);
            throw new IllegalArgumentException("Design not found");
        }
        if (client == null) {
            System.err.println("ERROR: Client not found: " + clientId);
            throw new IllegalArgumentException("Client not found");
        }
        
        DesignLike like = new DesignLike();
        like.setDesign(design);
        like.setClient(client);
        like.setLikedAt(LocalDateTime.now());
        
        System.out.println("DEBUG: Persisting like...");
        em.persist(like);
        em.flush();
        System.out.println("DEBUG: Like persisted successfully");
        
    } catch (Exception e) {
        System.err.println("ERROR in addLike: " + e.getMessage());
        e.printStackTrace();
        throw new RuntimeException("Failed to add like: " + e.getMessage(), e);
    }
}
@Override
@Transactional
public void removeLike(Long designId, Long clientId) {
    try {
        // Find the specific like
        DesignLike like = em.createQuery(
            "SELECT l FROM DesignLike l WHERE l.client.userId = :clientId AND l.design.designId = :designId", 
            DesignLike.class)
            .setParameter("clientId", clientId)
            .setParameter("designId", designId)
            .getSingleResult();
        
        if (like != null) {
            em.remove(like);
            em.flush();
        }
    } catch (NoResultException e) {
        // Like not found, nothing to remove
        System.out.println("Like not found for removal: clientId=" + clientId + ", designId=" + designId);
    } catch (Exception e) {
        throw new RuntimeException("Failed to remove like: " + e.getMessage(), e);
    }
}

@Override
@Transactional
public void addFavourite(Long designId, Long clientId) {
    try {
        // Check if favourite already exists
        Long count = em.createQuery(
            "SELECT COUNT(f) FROM DesignFavourite f WHERE f.client.userId = :clientId AND f.design.designId = :designId", 
            Long.class)
            .setParameter("clientId", clientId)
            .setParameter("designId", designId)
            .getSingleResult();
        
        if (count > 0) {
            return; // Already favourited
        }
        
        // Create new favourite
        TattooDesign design = em.find(TattooDesign.class, designId);
        AppUser client = em.find(AppUser.class, clientId);
        
        if (design == null || client == null) {
            throw new IllegalArgumentException("Design or Client not found");
        }
        
        DesignFavourite favourite = new DesignFavourite();
        favourite.setDesign(design);
        favourite.setClient(client);
        favourite.setFavoritedAt(LocalDateTime.now());
        
        em.persist(favourite);
        em.flush();
        
    } catch (Exception e) {
        throw new RuntimeException("Failed to add favourite: " + e.getMessage(), e);
    }
}

@Override
@Transactional
public void removeFavourite(Long designId, Long clientId) {
    try {
        // Find the specific favourite
        DesignFavourite favourite = em.createQuery(
            "SELECT f FROM DesignFavourite f WHERE f.client.userId = :clientId AND f.design.designId = :designId", 
            DesignFavourite.class)
            .setParameter("clientId", clientId)
            .setParameter("designId", designId)
            .getSingleResult();
        
        if (favourite != null) {
            em.remove(favourite);
            em.flush();
        }
    } catch (NoResultException e) {
        // Favourite not found, nothing to remove
        System.out.println("Favourite not found for removal: clientId=" + clientId + ", designId=" + designId);
    } catch (Exception e) {
        throw new RuntimeException("Failed to remove favourite: " + e.getMessage(), e);
    }
}
@Override
    public void addComment(Long designId, Long clientId, String commentText) {
        // Validate inputs
        if (designId == null || clientId == null) {
            throw new IllegalArgumentException("Design ID and Client ID are required");
        }
        
        if (commentText == null || commentText.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment text cannot be empty");
        }
        
        try {
            // Create new comment
            DesignComment comment = new DesignComment();
            comment.setDesign(em.find(TattooDesign.class, designId));
            comment.setClient(em.find(AppUser.class, clientId));
            comment.setText(commentText.trim());
//            comment.setCreatedAt(new Date());
            
            // Persist the comment
            em.persist(comment);
            em.flush();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to add comment: " + e.getMessage(), e);
        }
    }

   @Override
public List<DesignComment> getCommentsForDesign(Long designId) {
    try {
        return em.createNativeQuery(
            """
            SELECT 
                dc.comment_id AS COMMENTID,
                dc.created_at AS CREATEDAT,
                dc.text AS TEXT,
                dc.client_id AS client_id,
                dc.design_id AS design_id
            FROM design_comments dc
            WHERE dc.design_id = ?
            ORDER BY dc.created_at DESC
            """,
            DesignComment.class
        )
        .setParameter(1, designId)
        .getResultList();

    } catch (Exception e) {
        e.printStackTrace();
        return new ArrayList<>();
    }
}
    
    
public List<Review> getReviewsByArtist(Integer artistId) {
    return em.createQuery(
        "SELECT r FROM Review r WHERE r.artist.userId = :artistId ORDER BY r.reviewDate DESC",
        Review.class
    )
    .setParameter("artistId", artistId)
    .getResultList();
}



@Override
public Long getArtistTotalReviews(Long artistId) {

    return em.createQuery(
        """
        SELECT COUNT(r)
        FROM Review r
        WHERE r.artist.userId = :artistId
        """,
        Long.class
    )
    .setParameter("artistId", artistId)
    .getSingleResult();
}

@Override
public List<Review> getReviewsForArtist(Long artistId) {
    return em.createQuery(
        "SELECT r FROM Review r WHERE r.artist.userId = :artistId ORDER BY r.reviewDate DESC", Review.class)
        .setParameter("artistId", artistId)
        .getResultList();
}

@Override
public Experience getArtistExperience(Long artistId) {
    try {
        // Fetch the Experience entity using the artist's ID
        return em.createQuery(
            "SELECT e FROM Experience e WHERE e.artist.userId = :artistId", Experience.class)
            .setParameter("artistId", artistId)
            .getSingleResult();
    } catch (NoResultException e) {
        // Return null if no experience record is found
        return null; 
    }
}

@Override
public TattooDesign findDesignById(Long designId) {
    return em.createQuery(
        "SELECT d FROM TattooDesign d " +
        "LEFT JOIN FETCH d.artist " +
        "WHERE d.designId = :id " +
        "AND (d.isBanned = false OR d.isBanned IS NULL) " +
        "AND (d.isRemovedByArtist = false OR d.isRemovedByArtist IS NULL)",
        TattooDesign.class
    )
    .setParameter("id", designId)
    .getResultStream()
    .findFirst()
    .orElse(null);
}



 
    @Override
public boolean isDesignLikedByClient(Long clientId, Long designId) {
    try {
        Long count = em.createQuery(
            "SELECT COUNT(l) FROM DesignLike l WHERE l.client.userId = :clientId AND l.design.designId = :designId", 
            Long.class)
            .setParameter("clientId", clientId)
            .setParameter("designId", designId)
            .getSingleResult();
        return count > 0;
    } catch (Exception e) {
        return false;
    }
}

    @Override
public boolean isDesignFavouritedByClient(Long clientId, Long designId) {
    try {
        Long count = em.createQuery(
            "SELECT COUNT(f) FROM DesignFavourite f WHERE f.client.userId = :clientId AND f.design.designId = :designId", 
            Long.class)
            .setParameter("clientId", clientId)
            .setParameter("designId", designId)
            .getSingleResult();
        return count > 0;
    } catch (Exception e) {
        return false;
    }
}

    @Override
public Long getClientAppointmentsCount(Long clientId) {
    try {
        // JPQL query to count all Appointment entities where the client matches the given ID
        return em.createQuery(
            "SELECT COUNT(a) FROM Appointment a WHERE a.client.userId = :clientId", Long.class)
            .setParameter("clientId", clientId)
            .getSingleResult();
    } catch (Exception e) {
        // In a rare case of an execution error, log it and return 0
        System.err.println("Error calculating client appointments count for ID " + clientId + ": " + e.getMessage());
        return 0L; 
    }
}

    @Override
public List<TattooDesign> getRelatedDesigns(String style, Long artistId) {
    if ((style == null || style.isBlank()) && artistId == null) {
        return Collections.emptyList();
    }

    String jpql = "SELECT d FROM TattooDesign d WHERE (d.isBanned IS NULL OR d.isBanned = false) "
                + "AND (d.isRemovedByArtist IS NULL OR d.isRemovedByArtist = false) ";

    if (style != null && !style.isBlank()) {
        jpql += "AND LOWER(d.style) = :style ";
    }
    if (artistId != null) {
        jpql += "AND d.artist.userId = :artistId ";
    }

    jpql += "ORDER BY d.uploadedAt DESC";

    TypedQuery<TattooDesign> query = em.createQuery(jpql, TattooDesign.class);
    if (style != null && !style.isBlank()) {
        query.setParameter("style", style.toLowerCase());
    }
    if (artistId != null) {
        query.setParameter("artistId", artistId);
    }
    query.setMaxResults(8);

    return query.getResultList();
}


//    @Override
//@Transactional
//public MedicalForm submitMedicalForm(Long clientId, Long appointmentId, String data) {
//    AppUser client = em.find(AppUser.class, clientId);
//    Appointment a = em.find(Appointment.class, appointmentId);
//
//    if (client == null) throw new IllegalArgumentException("Client not found");
//    if (a == null) throw new IllegalArgumentException("Appointment not found");
//
//    // Assume 'data' is a JSON string; parse it into MedicalForm fields
//    // You can use a library like Jackson here (or do manual parsing)
//    MedicalForm form = new MedicalForm();
//    form.setClient(client);
//    form.setAppointment(a);
//    form.setSubmittedAt(LocalDateTime.now());
//    form.setIsPregnant(false); // default/fallback
//    form.setIsMinor(false);
//    // TODO: parse JSON `data` to fill other fields
//
//    em.persist(form);
//
//    // link appointment (bidirectional)
//    a.setMedicalForm(form);
//    em.merge(a);
//
//    em.flush();
//    return form;
//}


@Override
@Transactional
public void addComment(Long designId, Long clientId, String commentText, int rating) {
    AppUser client = em.find(AppUser.class, clientId);
    TattooDesign design = em.find(TattooDesign.class, designId);

    if (client == null || design == null) {
        throw new IllegalArgumentException("Client or Design not found");
    }

    // Initialize comments list if null
    if (design.getComments() == null) {
        design.setComments(new ArrayList<>());
    }

    DesignComment comment = new DesignComment();
    comment.setClient(client);
    comment.setDesign(design);
    comment.setText(commentText);
    comment.setCreatedAt(LocalDateTime.now());

    em.persist(comment);
    design.getComments().add(comment);
    em.merge(design);
    em.flush();
}

@Override
public Long getDesignLikeCount(Long designId) {
    return em.createQuery(
            "SELECT COUNT(dl) FROM DesignLike dl WHERE dl.design.designId = :designId", Long.class)
            .setParameter("designId", designId)
            .getSingleResult();
}

 @Override
    public List<TattooDesign> getRecommendedDesignsForClient(Long clientId, int max) {
        // Example logic: return top designs by likes, excluding designs already liked by the client
        return em.createQuery(
                "SELECT d FROM TattooDesign d " +
                "WHERE (d.isBanned IS NULL OR d.isBanned = false) " +
                "AND (d.isRemovedByArtist IS NULL OR d.isRemovedByArtist = false) " +
                "AND d.designId NOT IN (SELECT l.design.designId FROM DesignLike l WHERE l.client.userId = :clientId) " +
                "ORDER BY SIZE(d.likes) DESC", TattooDesign.class)
                .setParameter("clientId", clientId)
                .setMaxResults(max)
                .getResultList();
    }

     @Override
    public boolean hasClientBookedArtistAnyStatus(Long clientId, Long artistId) {
        if (clientId == null || artistId == null) {
            return false;
        }
        
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(a) FROM Appointment a " +
            "WHERE a.client.userId = :clientId " +
            "AND a.artist.userId = :artistId", 
            Long.class
        );
        
        query.setParameter("clientId", clientId);
        query.setParameter("artistId", artistId);
        
        Long count = query.getSingleResult();
        return count != null && count > 0;
    }
    
    @Override
    public Long getCommentCountForDesign(Long designId) {
        if (designId == null) {
            return 0L;
        }
        
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(c) FROM DesignComment c " +
            "WHERE c.design.designId = :designId", 
            Long.class
        );
        
        query.setParameter("designId", designId);
        return query.getSingleResult();
    }
    
    /**
     * Delete a comment
     */
    @Override
    public void deleteComment(Long commentId) {
        if (commentId == null) {
            return;
        }
        
        DesignComment comment = em.find(DesignComment.class, commentId);
        if (comment != null) {
            em.remove(comment);
        }
    }
    
    /**
     * Check if client can comment on design
     * (Optional: Add business logic like requiring booking)
     */
    @Override
    public boolean canClientCommentOnDesign(Long designId, Long clientId) {
        // For now, any logged-in client can comment
        // You can add logic here like requiring booking
        return clientId != null;
    }
    
    // ==================== LIKE METHODS ====================
//
//    @Override
//    public boolean hasClientBookedArtist(Long clientId, Long artistId) {
//        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
//    }
//
//    @Override
//    public void addReview(Long artistId, Long clientId, Integer rating, String comment) {
//        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
//    }
//    
// @Override
//public void addReview(Long artistId, Long clientId, Integer rating, String comment) {
//    // Validate inputs
//    if (artistId == null || clientId == null || rating == null || rating < 1 || rating > 5) {
//        throw new IllegalArgumentException("Invalid review parameters");
//    }
//    
//    if (comment == null || comment.trim().isEmpty()) {
//        throw new IllegalArgumentException("Review comment cannot be empty");
//    }
//    
//    // Check if review already exists
//    Long existing = em.createQuery(
//        "SELECT COUNT(r) FROM Review r WHERE r.artist.userId = :artistId AND r.client.userId = :clientId", 
//        Long.class)
//        .setParameter("artistId", artistId)
//        .setParameter("clientId", clientId)
//        .getSingleResult();
//    
//    if (existing > 0) {
//        throw new IllegalStateException("You have already reviewed this artist");
//    }
//    
//    // Create new review
//    Review review = new Review();
//    review.setArtist(em.find(AppUser.class, artistId));
//    review.setClient(em.find(AppUser.class, clientId));
//    review.setRating(rating);
//    review.setComments(comment.trim());  // CORRECT: Use setComments() not setComment()
//    review.setReviewDate(LocalDateTime.now());  // CORRECT: Use LocalDateTime.now()
//    
//    em.persist(review);
//    em.flush();
//}
@Override
public void addReview(Long artistId, Long clientId, Double rating, String comment) {

    // ---------- validation ----------
    if (artistId == null || clientId == null) {
        throw new IllegalArgumentException("Artist or client missing");
    }

    if (rating == null || rating < 1 || rating > 5) {
        throw new IllegalArgumentException("Rating must be between 1 and 5");
    }

    // ---------- fetch artist ----------
    AppUser artist = em.find(AppUser.class, artistId);
    if (artist == null || !artist.isActive()) {
        throw new IllegalStateException("Artist not found or inactive");
    }

    // ---------- fetch client ----------
    AppUser client = em.find(AppUser.class, clientId);
    if (client == null) {
        throw new IllegalStateException("Client not found");
    }

    // ---------- prevent self-review ----------
    if (artistId.equals(clientId)) {
        throw new IllegalStateException("Artist cannot review themselves");
    }

    // ---------- check duplicate review ----------
    Long count = em.createQuery(
        "SELECT COUNT(r) FROM Review r " +
        "WHERE r.artist.userId = :artistId " +
        "AND r.client.userId = :clientId",
        Long.class
    )
    .setParameter("artistId", artistId)
    .setParameter("clientId", clientId)
    .getSingleResult();

    if (count != null && count > 0) {
        throw new IllegalStateException("You have already reviewed this artist");
    }

    // ---------- create review ----------
    Review review = new Review();
    review.setArtist(artist);
    review.setClient(client);
    review.setRating(rating);
    review.setComments(comment);

    em.persist(review);
}

@Override
public boolean hasClientBookedArtist(Long clientId, Long artistId) {
    if (clientId == null || artistId == null) {
        return false;
    }
    
    TypedQuery<Long> query = em.createQuery(
        "SELECT COUNT(a) FROM Appointment a " +
        "WHERE a.client.userId = :clientId " +
        "AND a.artist.userId = :artistId " +
        "AND a.status IN :validStatuses", 
        Long.class
    );
    
    query.setParameter("clientId", clientId);
    query.setParameter("artistId", artistId);
    query.setParameter("validStatuses", java.util.List.of(
        "COMPLETED", 
        "CONFIRMED",
        "PAID"
    ));
    
    Long count = query.getSingleResult();
    return count != null && count > 0;
}

@Override
public List<Appointment> listClientAppointments(Long clientId, int offset, int limit) {

    TypedQuery<Appointment> query = em.createQuery(
        "SELECT a FROM Appointment a " +
        "WHERE a.client.userId = :clientId " +
        "ORDER BY " +
        "CASE WHEN a.appointmentDateTime IS NULL THEN 1 ELSE 0 END, " +
        "a.appointmentDateTime DESC",
        Appointment.class
    );

    query.setParameter("clientId", clientId);

    if (offset >= 0) query.setFirstResult(offset);
    if (limit > 0) query.setMaxResults(limit);

    return query.getResultList();
}

@Override
public MedicalForm getMedicalForm(Long appointmentId) {
    try {
        MedicalForm form = em.createQuery(
            "SELECT m FROM MedicalForm m " +
            "WHERE m.appointment.appointmentId = :aid",
            MedicalForm.class
        )
        .setParameter("aid", appointmentId)
        .getSingleResult();

        // initialize relations
        if (form.getClient() != null) form.getClient().getUserId();
        if (form.getValidatedBy() != null) form.getValidatedBy().getUserId();

        return form;

    } catch (NoResultException e) {
        return null;
    }
} 
@Override
public List<ArtistCardDTO> getAllArtistsForBrowse() {

    List<AppUser> artists = em.createQuery(
        """
        SELECT a
        FROM AppUser a
        WHERE a.role.roleName = :roleName
          AND a.isActive = true
        ORDER BY a.fullName
        """,
        AppUser.class
    )
    .setParameter("roleName", "ARTIST")
    .getResultList();

    List<ArtistCardDTO> result = new ArrayList<>();

    for (AppUser a : artists) {
        ArtistCardDTO dto = new ArtistCardDTO();
        dto.setArtistId(a.getUserId());
        dto.setFullName(a.getFullName());

        // experience
        if (a.getExperience() != null) {
            dto.setYearsExperience(a.getExperience().getYearsExperience());
        }

        // ratings (USE Review entity)
        Double avgRating = em.createQuery(
            "SELECT AVG(r.rating) FROM Review r WHERE r.artist.userId = :artistId",
            Double.class
        )
        .setParameter("artistId", a.getUserId())
        .getSingleResult();

        Long reviewCount = em.createQuery(
            "SELECT COUNT(r) FROM Review r WHERE r.artist.userId = :artistId",
            Long.class
        )
        .setParameter("artistId", a.getUserId())
        .getSingleResult();

        dto.setAverageRating(avgRating != null ? avgRating : 0.0);
        dto.setTotalReviews(reviewCount);

        result.add(dto);
    }

    return result;
}

//@Override
//public AppUser getArtistInfo(Long artistId) {
//    if (artistId == null) return null;
//
//    try {
//        return em.createQuery(
//            """
//            SELECT a
//            FROM AppUser a
//            WHERE a.userId = :id
//              AND a.role.roleName = 'ARTIST'
//              AND a.isActive = true
//            """,
//            AppUser.class
//        )
//        .setParameter("id", artistId)
//        .getSingleResult();
//
//    } catch (NoResultException e) {
//        return null;
//    }
//}

@Override
    public List<Review> listArtistReviews(Long artistId) { //
        if (artistId == null) {
            return Collections.emptyList();
        }
        
        try {
            return em.createQuery(
                "SELECT r FROM Review r WHERE r.artist.userId = :artistId ORDER BY r.reviewDate DESC",
                Review.class
            )
            .setParameter("artistId", artistId)
            .getResultList();
        } catch (Exception e) {
            System.err.println("Error listing reviews for artist " + artistId + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public boolean hasCompletedAppointment(Long clientId, Long artistId) { //
        if (clientId == null || artistId == null) {
            return false;
        }
        
        try {
            // Business Rule: Check for at least one completed appointment
            Long count = em.createQuery(
                "SELECT COUNT(a) FROM Appointment a WHERE a.client.userId = :clientId " +
                "AND a.artist.userId = :artistId AND a.status = 'COMPLETED'",
                Long.class
            )
            .setParameter("clientId", clientId)
            .setParameter("artistId", artistId)
            .getSingleResult();
            
            return count > 0;
            
        } catch (Exception e) {
            System.err.println("Error checking review eligibility: " + e.getMessage());
            return false;
        }
    }

 @Override
@Transactional
public void submitArtistReview(Long artistId, Long clientId, Double rating, String comments) {
    if (artistId == null || clientId == null || rating == null || rating < 1.0 || rating > 5.0) {
            throw new IllegalArgumentException("Invalid rating or IDs provided.");
        }
        if (comments == null || comments.trim().isEmpty()) {
            throw new IllegalArgumentException("Review comment cannot be empty.");
        }

        try {
            // 1. Attempt to find an existing review
            Review existingReview = em.createQuery(
                    "SELECT r FROM Review r WHERE r.artist.userId = :artistId AND r.client.userId = :clientId", Review.class)
                    .setParameter("artistId", artistId)
                    .setParameter("clientId", clientId)
                    .getSingleResult();

            // 2. If found, update the existing review
            existingReview.setRating(rating);
            existingReview.setComments(comments.trim());
            existingReview.setReviewDate(LocalDateTime.now());
            em.merge(existingReview);

        } catch (NoResultException e) {
            // 3. If no existing review, create a new one
            AppUser artistRef = em.find(AppUser.class, artistId);
            AppUser clientRef = em.find(AppUser.class, clientId);

            if (artistRef == null) throw new IllegalArgumentException("Artist not found.");
            if (clientRef == null) throw new IllegalArgumentException("Client not found.");
            
            Review newReview = new Review();
            newReview.setArtist(artistRef);
            newReview.setClient(clientRef);
            newReview.setRating(rating);
            newReview.setComments(comments.trim());
            newReview.setReviewDate(LocalDateTime.now());

            em.persist(newReview);
            
        } catch (Exception e) {
            // Catch all other exceptions during DB operation and ensure transaction rollback
            // Wrap in EJBException to be handled by the Managed Bean
            throw new EJBException("Failed to submit or update review.", e);
        }
    }
    // In ClientEJB.java
@Override
public boolean hasClientReviewedArtist(Long clientId, Long artistId) {
    try {
        // Query to count reviews where the client and artist IDs match
        Long count = em.createQuery(
            "SELECT COUNT(r) FROM Review r WHERE r.client.userId = :clientId AND r.artist.userId = :artistId",
            Long.class
        )
        .setParameter("clientId", clientId)
        .setParameter("artistId", artistId)
        .getSingleResult();
        
        // Return true if the count is greater than zero
        return count > 0;
    } catch (Exception e) {
        // In a real application, you'd log the error. 
        // For simplicity, we assume an error means no review was found.
        return false; 
    }
}

    @Override
    
    public void addComment(Long designId, Long clientId, String comment, Double rating) {
//        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    


}
    
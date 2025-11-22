package ejb;

import jakarta.persistence.NoResultException;
import entities.*;
import entities.TimeSlot.TimeSlotStatus;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.ejb.Stateless;
import jakarta.persistence.*;
//import static jakarta.persistence.GenerationType.UUID;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
    public List<TattooDesign> listDesigns(int offset, int limit) {
        TypedQuery<TattooDesign> q = em.createQuery("SELECT d FROM TattooDesign d ORDER BY d.uploadedAt DESC", TattooDesign.class);
        if (offset >= 0) q.setFirstResult(offset);
        if (limit > 0) q.setMaxResults(limit);
        List<TattooDesign> list = q.getResultList();
        list.forEach(d -> {
            if (d.getArtist() != null) d.getArtist().getUserId(); // init artist id
            if (d.getLikes() != null) d.getLikes().size();
            if (d.getFavourites() != null) d.getFavourites().size();
        });
        return list;
    }

    @Override
    public List<TattooDesign> searchDesigns(String qStr, String style, BigDecimal minPrice, BigDecimal maxPrice, int offset, int limit) {
        StringBuilder ql = new StringBuilder("SELECT d FROM TattooDesign d WHERE 1=1 ");
        if (qStr != null && !qStr.isBlank()) ql.append("AND (LOWER(d.title) LIKE :q OR LOWER(d.description) LIKE :q) ");
        if (style != null && !style.isBlank()) ql.append("AND LOWER(d.style) = :style ");
        if (minPrice != null) ql.append("AND d.price >= :minPrice ");
        if (maxPrice != null) ql.append("AND d.price <= :maxPrice ");
        ql.append("ORDER BY d.uploadedAt DESC");

        TypedQuery<TattooDesign> q = em.createQuery(ql.toString(), TattooDesign.class);
        if (qStr != null && !qStr.isBlank()) q.setParameter("q", "%" + qStr.toLowerCase() + "%");
        if (style != null && !style.isBlank()) q.setParameter("style", style.toLowerCase());
        if (minPrice != null) q.setParameter("minPrice", minPrice);
        if (maxPrice != null) q.setParameter("maxPrice", maxPrice);
        if (offset >= 0) q.setFirstResult(offset);
        if (limit > 0) q.setMaxResults(limit);

        List<TattooDesign> list = q.getResultList();
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
        try {
            DesignFavourite f = em.createQuery(
                    "SELECT f FROM DesignFavourite f WHERE f.client.userId = :cid AND f.design.designId = :did", DesignFavourite.class)
                    .setParameter("cid", clientId).setParameter("did", designId)
                    .getSingleResult();
            em.remove(f);
        } catch (NoResultException ignored) {}
    }

    @Override
    public List<DesignFavourite> listFavourites(Long clientId, int offset, int limit) {
        TypedQuery<DesignFavourite> q = em.createQuery("SELECT f FROM DesignFavourite f WHERE f.client.userId = :cid ORDER BY f.favoritedAt DESC", DesignFavourite.class)
                .setParameter("cid", clientId);
        if (offset >= 0) q.setFirstResult(offset);
        if (limit > 0) q.setMaxResults(limit);
        List<DesignFavourite> list = q.getResultList();
        list.forEach(f -> {
            if (f.getDesign() != null) f.getDesign().getDesignId();
            if (f.getClient() != null) f.getClient().getUserId();
        });
        return list;
    }

    // -------------------------
    // Booking / appointments
    // -------------------------
    @Override
public List<TimeSlot> listAvailableTimeSlots(Long artistId, LocalDate date) {
    // Assuming you have TimeSlot.TimeSlotStatus imported
    LocalDateTime startOfDay = date.atStartOfDay();
    LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
    
    return em.createQuery(
            "SELECT t FROM TimeSlot t WHERE t.artist.userId = :artistId AND t.status = :status AND t.startTime >= :startOfDay AND t.startTime < :endOfDay ORDER BY t.startTime ASC", TimeSlot.class)
            .setParameter("artistId", artistId)
            .setParameter("status", TimeSlot.TimeSlotStatus.AVAILABLE)
            .setParameter("startOfDay", startOfDay)
            .setParameter("endOfDay", endOfDay)
            .getResultList();
}


    @Override
    public boolean isSlotAvailable(Integer slotId) {
        try {
            TimeSlot slot = em.find(TimeSlot.class, slotId);
            // CRITICAL: Slot must exist and have the AVAILABLE status
            return slot != null && slot.getStatus() == TimeSlotStatus.AVAILABLE;
        } catch (NoResultException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
@Transactional
public Long bookAppointment(Long clientId, Long artistId, Long designId, Integer slotId, String clientNote) {
    AppUser client = em.find(AppUser.class, clientId);
    TattooDesign design = em.find(TattooDesign.class, designId);
    TimeSlot slot = em.find(TimeSlot.class, slotId);

    // 1. Validation Checks (Essential)
    if (client == null) throw new IllegalArgumentException("Client not found.");
    if (design == null) throw new IllegalArgumentException("Design not found.");
    if (slot == null) throw new IllegalArgumentException("Time Slot not found.");
    
    // Ensure the slot belongs to the correct artist (critical security/logic check)
    if (!slot.getArtist().getUserId().equals(artistId)) {
        throw new IllegalArgumentException("Time Slot does not belong to Artist ID: " + artistId);
    }
    
    // 2. Lock the Slot
    if (slot.getStatus() != TimeSlot.TimeSlotStatus.AVAILABLE) {
        // This prevents double booking!
        throw new IllegalStateException("Slot is already " + slot.getStatus().name() + " and cannot be booked.");
    }
    slot.setStatus(TimeSlot.TimeSlotStatus.PENDING_APPOINTMENT); 
    em.merge(slot); // Persist the status change immediately

    // 3. Create Appointment
    Appointment newAppointment = new Appointment();
    newAppointment.setClient(client);
    newAppointment.setArtist(slot.getArtist());
    newAppointment.setDesign(design);
    newAppointment.setSlot(slot); // Link the TimeSlot
    newAppointment.setClientNote(clientNote);
    
    // IMPORTANT: Set the appointment time from the VALIDATED slot!
    newAppointment.setAppointmentDateTime(slot.getStartTime()); 
    
    // The appointment should start as PENDING, NOT CONFIRMED
    newAppointment.setStatus("PENDING"); // <--- Ensure this is NOT "CONFIRMED"

    em.persist(newAppointment);
    em.flush();
    return newAppointment.getAppointmentId();
}
    @Override
    @Transactional
    public void cancelAppointment(Long appointmentId, String reason) {
        Appointment a = em.find(Appointment.class, appointmentId);
        if (a == null) throw new IllegalArgumentException("Appointment not found");
        a.setStatus("CANCELLED");
        a.setCancellationReason(reason);
        // free slot if present
        TimeSlot slot = a.getSlot();
        if (slot != null) {
            slot.setStatus(TimeSlot.TimeSlotStatus.AVAILABLE);
            slot.setBlockReason(null);
            slot.setBlockedBy(null);
            em.merge(slot);
        }
        em.merge(a);
    }

    @Override
    public List<Appointment> listClientAppointments(Long clientId, int offset, int limit) {
        TypedQuery<Appointment> q = em.createQuery("SELECT a FROM Appointment a WHERE a.client.userId = :cid ORDER BY a.appointmentDateTime DESC", Appointment.class)
                .setParameter("cid", clientId);
        if (offset >= 0) q.setFirstResult(offset);
        if (limit > 0) q.setMaxResults(limit);
        List<Appointment> list = q.getResultList();
        list.forEach(a -> {
            if (a.getArtist() != null) a.getArtist().getUserId();
            if (a.getDesign() != null) a.getDesign().getDesignId();
            if (a.getSlot() != null) a.getSlot().getSlotId();
            if (a.getPayment() != null) a.getPayment().getPaymentId();
        });
        return list;
    }

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
}

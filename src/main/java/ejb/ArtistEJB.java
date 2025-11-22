package ejb;

import jakarta.persistence.NoResultException;
import entities.*;
import jakarta.ejb.Stateless;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Stateless
public class ArtistEJB implements ArtistEJBLocal {

    @PersistenceContext(unitName = "TattooPU")
    private EntityManager em;

    private Long getAuthenticatedArtistId() {
        // Placeholder for the ID of the currently logged-in artist
        return 1L; 
    }
    // -------------------------
    // Profile
    // -------------------------
    @Override
    public AppUser getArtistById(Long artistId) {
        AppUser artist = em.find(AppUser.class, artistId);
        if (artist == null) throw new IllegalArgumentException("Artist not found: " + artistId);
        if (artist.getRole() == null || !"ARTIST".equalsIgnoreCase(artist.getRole().getRoleName().trim()))
            throw new IllegalArgumentException("User is not an artist: " + artistId);

        initializeArtistCollections(artist);
        return artist;
    }

    @Override
    @Transactional
    public AppUser updateArtistProfile(Long artistId, String fullName, String phone, String portfolioLink) {
        AppUser artist = em.find(AppUser.class, artistId);
        if (artist == null) throw new IllegalArgumentException("Artist not found: " + artistId);
        if (fullName != null) artist.setFullName(fullName);
        if (phone != null) artist.setPhone(phone);

        // Manage Experience simple link (create or update)
        if (portfolioLink != null) {
            Experience exp = null;
            try {
                exp = em.createQuery("SELECT e FROM Experience e WHERE e.artist.userId = :id", Experience.class)
                        .setParameter("id", artistId)
                        .getSingleResult();
            } catch (NoResultException ignored) {}
            if (exp == null) {
                exp = new Experience();
                exp.setArtist(artist);
                exp.setPortfolioLink(portfolioLink);
                em.persist(exp);
            } else {
                exp.setPortfolioLink(portfolioLink);
                em.merge(exp);
            }
        }

        return em.merge(artist);
    }

    // -------------------------
    // Designs
    // -------------------------
    
    @Override
    @Transactional
    public TattooDesign addDesign(Long artistId, TattooDesign design) {
        AppUser artist = em.find(AppUser.class, artistId);
        if (artist == null) {
            throw new IllegalArgumentException("Artist not found: " + artistId);
        }
        
        // You should enforce non-null checks on design fields here, 
        // e.g., title, style, price (if required).
        if (design.getTitle() == null || design.getTitle().isBlank()) {
            throw new IllegalArgumentException("Design title is mandatory.");
        }

        design.setArtist(artist);
        // Assuming the REST layer handles image saving and sets the imagePath field
        // based on the incoming JSON (or handles file upload separately).
        // For now, we only persist the design entity.
        em.persist(design);
        em.flush();
        return design;
    }
    
    @Override
    @Transactional
    public TattooDesign updateDesign(Long designId, TattooDesign designPayload) {
        TattooDesign existingDesign = em.find(TattooDesign.class, designId);
        
        if (existingDesign == null) {
            throw new IllegalArgumentException("Tattoo design not found with ID: " + designId);
        }

        // Apply updates for every field that was provided in the JSON payload (i.e., is not null)
        
        // Title (String)
        if (designPayload.getTitle() != null) {
            existingDesign.setTitle(designPayload.getTitle());
        }
        
        // Description (String)
        if (designPayload.getDescription() != null) {
            existingDesign.setDescription(designPayload.getDescription());
        }
        
        // Style (String)
        if (designPayload.getStyle() != null) {
            existingDesign.setStyle(designPayload.getStyle());
        }
        
        // Price (BigDecimal)
        if (designPayload.getPrice() != null) { 
            existingDesign.setPrice(designPayload.getPrice());
        }
        
        // ImagePath (String/URL) - This is how the artist updates the image
        if (designPayload.getImagePath() != null) { 
             existingDesign.setImagePath(designPayload.getImagePath());
        }
        
        // Note: 'uploadedAt' and 'artist' cannot be changed after creation.
        
        // The persistence context manages the entity, and changes are synchronized to the DB.
        return em.merge(existingDesign);
    }
    
    @Override
    @Transactional
    public void deleteDesign(Long designId) {
        TattooDesign design = em.find(TattooDesign.class, designId);
        
        if (design == null) {
            // Throw an exception that the REST layer can translate to a 404
            throw new IllegalArgumentException("Tattoo design not found with ID: " + designId);
        }

        // 🛑 STEP 1: Handle Appointments
        // Since Appointment.design is nullable, we must first clear all references 
        // to this design from existing appointments. Without this, the delete will fail.
        
        // This is a bulk update query to set DESIGN_ID to NULL for all appointments 
        // that currently reference this design.
        Query q = em.createQuery("UPDATE Appointment a SET a.design = NULL WHERE a.design.designId = :designId");
        q.setParameter("designId", designId);
        q.executeUpdate();

        // 🛑 STEP 2: Delete the Design
        // Due to the CascadeType.REMOVE set in TattooDesign.java, this single line 
        // will also automatically delete all related DesignLike and DesignFavourite records.
        em.remove(design);
        
        // OPTIONAL: If you store image files on the file system, add logic here to delete the physical file.
    }
    
    @Override
    public List<TattooDesign> getArtistDesigns(Long artistId, int offset, int limit) {
        TypedQuery<TattooDesign> q = em.createQuery(
                "SELECT d FROM TattooDesign d WHERE d.artist.userId = :id ORDER BY d.uploadedAt DESC",
                TattooDesign.class)
                .setParameter("id", artistId);
        if (offset >= 0) q.setFirstResult(offset);
        if (limit > 0) q.setMaxResults(limit);
        List<TattooDesign> list = q.getResultList();
        list.forEach(d -> {
            if (d.getLikes() != null) d.getLikes().size();
            if (d.getFavourites() != null) d.getFavourites().size();
        });
        return list;
    }


    @Override
    public List<TattooDesign> searchDesigns(String q, String style, BigDecimal minPrice, BigDecimal maxPrice, int offset, int limit) {
        
        // 1. Start the base query
        StringBuilder jpql = new StringBuilder("SELECT d FROM TattooDesign d WHERE 1=1 "); // 1=1 allows easy appending of 'AND' clauses
        
        // 2. Prepare parameters map
        TypedQuery<TattooDesign> query;
        
        // 3. Conditionally build the WHERE clause based on non-null parameters
        
        // Keyword Search (q): Checks title and description
        if (q != null && !q.isBlank()) {
            jpql.append("AND (LOWER(d.title) LIKE :q OR LOWER(d.description) LIKE :q) ");
        }
        
        // Style Filter
        if (style != null && !style.isBlank()) {
            jpql.append("AND LOWER(d.style) = :style ");
        }
        
        // Minimum Price Filter
        if (minPrice != null) {
            jpql.append("AND d.price >= :minPrice ");
        }
        
        // Maximum Price Filter
        if (maxPrice != null) {
            jpql.append("AND d.price <= :maxPrice ");
        }

        // 4. Add Ordering for consistency
        jpql.append("ORDER BY d.uploadedAt DESC");
        
        // 5. Create the query and set parameters
        query = em.createQuery(jpql.toString(), TattooDesign.class);

        if (q != null && !q.isBlank()) {
            query.setParameter("q", "%" + q.toLowerCase() + "%");
        }
        if (style != null && !style.isBlank()) {
            query.setParameter("style", style.toLowerCase());
        }
        if (minPrice != null) {
            query.setParameter("minPrice", minPrice);
        }
        if (maxPrice != null) {
            query.setParameter("maxPrice", maxPrice);
        }
        
        // 6. Apply pagination
        query.setFirstResult(offset);
        query.setMaxResults(limit);

        // 7. Execute
        return query.getResultList();
    }

    // likes / favourites
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
            return existing; // already liked
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

    // -------------------------
    // Time slots & schedule
    // -------------------------
    @Override
@Transactional
public ArtistSchedule saveArtistSchedule(Long artistId, ArtistSchedule schedule) {
    AppUser artist = em.find(AppUser.class, artistId);
    if (artist == null) throw new IllegalArgumentException("Artist not found: " + artistId);
    
    // CRITICAL: Check if a schedule for that day already exists to prevent duplicate
    try {
        ArtistSchedule existing = em.createQuery(
            "SELECT s FROM ArtistSchedule s WHERE s.artist.userId = :artistId AND s.dayOfWeek = :day", 
            ArtistSchedule.class)
            .setParameter("artistId", artistId)
            .setParameter("day", schedule.getDayOfWeek())
            .getSingleResult();
            
        // If existing, update it instead of creating new
        existing.setStartTime(schedule.getStartTime());
        existing.setEndTime(schedule.getEndTime());
        existing.setWorking(schedule.isWorking());
        return em.merge(existing);
        
    } catch (NoResultException e) {
        // No existing schedule, proceed to create
    }
    
    schedule.setArtist(artist);
    if (schedule.getId() != null) {
        return em.merge(schedule); // Use merge if ID is present
    } else {
        em.persist(schedule); // Use persist for new entity
        return schedule;
    }
}

@Override
    public List<TimeSlot> listAvailableTimeSlots(Long artistId) {
        // Query the database for TimeSlots belonging to the artist, where STATUS is 'AVAILABLE'
        // We use the TimeSlotStatus Enum for type safety
        try {
            TypedQuery<TimeSlot> query = em.createQuery(
                "SELECT t FROM TimeSlot t WHERE t.artist.userId = :artistId AND t.status = :status ORDER BY t.startTime ASC", 
                TimeSlot.class);
            
            query.setParameter("artistId", artistId);
            query.setParameter("status", TimeSlot.TimeSlotStatus.AVAILABLE);
            
            return query.getResultList();
            
        } catch (Exception e) {
            // Log the exception here
            System.err.println("Error listing available slots: " + e.getMessage());
            return List.of(); // Return an empty list on failure
        }
    }

   @Override
@Transactional
public List<TimeSlot> generateTimeSlotsForArtist(Long artistId, LocalDate startDate, LocalDate endDate, int slotDurationMinutes) {
    AppUser artist = em.find(AppUser.class, artistId);
    if (artist == null) throw new IllegalArgumentException("Artist not found.");
    
    // Fetch all schedules for the artist
    List<ArtistSchedule> schedules = listSchedulesForArtist(artistId); 
    
    // Clear existing slots in the range to prevent duplicates/conflicts (CRITICAL)
    em.createQuery("DELETE FROM TimeSlot t WHERE t.artist.userId = :artistId AND t.startTime >= :start AND t.endTime <= :end")
        .setParameter("artistId", artistId)
        .setParameter("start", startDate.atStartOfDay())
        .setParameter("end", endDate.plusDays(1).atStartOfDay())
        .executeUpdate();
    
    List<TimeSlot> createdSlots = new ArrayList<>();
    
    LocalDate currentDate = startDate;
    while (!currentDate.isAfter(endDate)) {
        DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
        
        // Find the schedule for the current day
        ArtistSchedule schedule = schedules.stream()
                .filter(s -> s.getDayOfWeek() == dayOfWeek && s.isWorking())
                .findFirst().orElse(null);

        if (schedule != null) {
            
            // FIX START: Variables are in the correct scope
            LocalDateTime currentStart = currentDate.atTime(schedule.getStartTime()); // This is in scope for the while loop below
            LocalDateTime scheduleEnd = currentDate.atTime(schedule.getEndTime());
            
            while (currentStart.isBefore(scheduleEnd)) {
                
                LocalDateTime currentEnd = currentStart.plusMinutes(slotDurationMinutes); // This is in scope for the rest of the loop
                
                // IMPORTANT: Ensure the last slot doesn't exceed the scheduled end time
                if (currentEnd.isAfter(scheduleEnd)) {
                    break; // Stop creating slots for this shift
                }

                // FUNCTIONAL FIX: Add check for existing slot to prevent duplicates (even after the DELETE query above, this is safer)
                if (!checkExistingSlot(artistId, currentStart, currentEnd)) {
                    TimeSlot slot = new TimeSlot();
                    slot.setArtist(artist);
                    slot.setStartTime(currentStart);
                    slot.setEndTime(currentEnd); 
                    slot.setStatus(TimeSlot.TimeSlotStatus.AVAILABLE);
                    
                    em.persist(slot);
                    createdSlots.add(slot);
                } 
                // Always advance the start time for the next iteration
                currentStart = currentEnd; 
            }
        }
        currentDate = currentDate.plusDays(1);
    }
    em.flush();
    return createdSlots;
}
    
private boolean checkExistingSlot(Long artistId, LocalDateTime start, LocalDateTime end) {
    try {
        em.createQuery("SELECT t FROM TimeSlot t WHERE t.artist.userId = :artistId AND t.startTime = :start AND t.endTime = :end", TimeSlot.class)
            .setParameter("artistId", artistId)
            .setParameter("start", start)
            .setParameter("end", end)
            .getSingleResult();
        return true;
    } catch (NoResultException e) {
        return false;
    } catch (Exception e) {
        // Log other exceptions
        return true; 
    }
}    

    @Override
    public List<TimeSlot> getArtistTimeSlots(Long artistId) {
        List<TimeSlot> list = em.createQuery("SELECT t FROM TimeSlot t WHERE t.artist.userId = :id ORDER BY t.startTime", TimeSlot.class)
                .setParameter("id", artistId)
                .getResultList();
        list.forEach(t -> {
            if (t.getBlockedBy() != null) t.getBlockedBy().getUserId();
        });
        return list;
    }
    
    @Override
    public List<ArtistSchedule> listSchedulesForArtist(Long artistId) {
        AppUser artist = em.find(AppUser.class, artistId);
        if (artist == null) {
            throw new IllegalArgumentException("Artist not found with ID: " + artistId);
        }
    return em.createQuery(
            "SELECT s FROM ArtistSchedule s WHERE s.artist.userId = :artistId ORDER BY s.dayOfWeek ASC", 
            ArtistSchedule.class)
            .setParameter("artistId", artistId)
            .getResultList();
}

    @Override
    @Transactional
    public TimeSlot addTimeSlot(Long artistId, TimeSlot slot) {
    AppUser artist = em.find(AppUser.class, artistId);
    if (artist == null) throw new IllegalArgumentException("Artist not found: " + artistId);
    if (slot.getStatus() == null) {
        slot.setStatus(TimeSlot.TimeSlotStatus.AVAILABLE);
    }
    slot.setArtist(artist);
    em.persist(slot);
    em.flush();
    return slot;
}
    @Override
    @Transactional
    public TimeSlot updateTimeSlot(Integer slotId, LocalDateTime start, LocalDateTime end, TimeSlot.TimeSlotStatus status) {
        TimeSlot t = em.find(TimeSlot.class, slotId);
        if (t == null) throw new IllegalArgumentException("Slot not found");
        if (start != null) t.setStartTime(start);
        if (end != null) t.setEndTime(end);
        if (status != null) t.setStatus(status);
        return em.merge(t);
    }

    @Override
    @Transactional
    public void deleteTimeSlot(Integer slotId) {
        TimeSlot t = em.find(TimeSlot.class, slotId);
        if (t == null) throw new IllegalArgumentException("Slot not found");
        em.remove(t);
    }

    @Override
    @Transactional
    public void markSlotAsBlocked(Integer slotId, String reason) {
        TimeSlot t = em.find(TimeSlot.class, slotId);
        if (t == null) throw new IllegalArgumentException("Slot not found");
        t.setStatus(TimeSlot.TimeSlotStatus.BLOCKED);
        t.setBlockReason(reason);
        em.merge(t);
    }

    // -------------------------
    // Appointments
    // -------------------------
    @Override
    public List<Appointment> listArtistAppointments(Long artistId, int offset, int limit) {
        TypedQuery<Appointment> q = em.createQuery("SELECT a FROM Appointment a WHERE a.artist.userId = :id ORDER BY a.appointmentDateTime DESC", Appointment.class)
                .setParameter("id", artistId);
        if (offset >= 0) q.setFirstResult(offset);
        if (limit > 0) q.setMaxResults(limit);
        List<Appointment> list = q.getResultList();
        list.forEach(a -> {
            if (a.getSlot() != null) a.getSlot().getSlotId();
            if (a.getDesign() != null) a.getDesign().getDesignId();
            if (a.getClient() != null) a.getClient().getUserId();
            if (a.getMedicalForm() != null) a.getMedicalForm().getFormId();
        });
        return list;
    }

    @Override
    public Appointment getAppointment(Long appointmentId) {
        Appointment a = em.find(Appointment.class, appointmentId);
        if (a == null) throw new IllegalArgumentException("Appointment not found: " + appointmentId);
        if (a.getSlot() != null) a.getSlot().getSlotId();
        if (a.getDesign() != null) a.getDesign().getDesignId();
        if (a.getClient() != null) a.getClient().getUserId();
        if (a.getMedicalForm() != null) a.getMedicalForm().getFormId();
        return a;
    }

    @Override
    @Transactional
    public Appointment changeAppointmentStatus(Long appointmentId, String status, String cancellationReason) {
        Appointment a = em.find(Appointment.class, appointmentId);
        if (a == null) throw new IllegalArgumentException("Appointment not found: " + appointmentId);
        a.setStatus(status);
        if ("CANCELLED".equalsIgnoreCase(status) && cancellationReason != null) a.setCancellationReason(cancellationReason);
        else a.setCancellationReason(null);
        return em.merge(a);
    }

    @Override
    @Transactional
    public Appointment addClientNoteToAppointment(Long appointmentId, String clientNote) {
        Appointment a = em.find(Appointment.class, appointmentId);
        if (a == null) throw new IllegalArgumentException("Appointment not found: " + appointmentId);
        a.setClientNote(clientNote);
        return em.merge(a);
    }

    // -------------------------
    // Medical Forms
    // -------------------------
    @Override
    public MedicalForm getMedicalFormForAppointment(Long appointmentId) {
        try {
            MedicalForm m = em.createQuery("SELECT m FROM MedicalForm m WHERE m.appointment.appointmentId = :aid", MedicalForm.class)
                    .setParameter("aid", appointmentId)
                    .getSingleResult();
            // initialize
            if (m.getClient() != null) m.getClient().getUserId();
            if (m.getValidatedBy() != null) m.getValidatedBy().getUserId();
            return m;
        } catch (NoResultException ex) {
            return null;
        }
    }

    @Override
    @Transactional
    public void acknowledgeMedicalForm(Integer formId, Long artistId, boolean approve) {
        MedicalForm form = em.find(MedicalForm.class, formId);
        if (form == null) throw new IllegalArgumentException("MedicalForm not found");
        AppUser artist = em.find(AppUser.class, artistId);
        if (artist == null) throw new IllegalArgumentException("Artist not found");
        form.setIsApproved(approve);
        form.setValidatedBy(artist);
        form.setValidatedAt(LocalDateTime.now());
        em.merge(form);
    }

    // -------------------------
    // Feedback & Reviews
    // -------------------------
    @Override
    public List<Feedback> listFeedbackForArtist(Long artistId, int offset, int limit) {
        TypedQuery<Feedback> q = em.createQuery("SELECT f FROM Feedback f WHERE f.artist.userId = :id ORDER BY f.createdAt DESC", Feedback.class)
                .setParameter("id", artistId);
        if (offset >= 0) q.setFirstResult(offset);
        if (limit > 0) q.setMaxResults(limit);
        List<Feedback> list = q.getResultList();
        list.forEach(f -> {
            if (f.getClient() != null) f.getClient().getUserId();
            if (f.getAppointment() != null) f.getAppointment().getAppointmentId();
        });
        return list;
    }

    @Override
    public List<Review> listReviewsForArtist(Long artistId, int offset, int limit) {
        TypedQuery<Review> q = em.createQuery("SELECT r FROM Review r WHERE r.artist.userId = :id ORDER BY r.reviewDate DESC", Review.class)
                .setParameter("id", artistId);
        if (offset >= 0) q.setFirstResult(offset);
        if (limit > 0) q.setMaxResults(limit);
        List<Review> list = q.getResultList();
        list.forEach(r -> {
            if (r.getClient() != null) r.getClient().getUserId();
        });
        return list;
    }

    // -------------------------
    // Payments & Earnings
    // -------------------------
    @Override
public List<Payment> listPaymentsForArtist(Long artistId, int offset, int limit) {
    // 1. Validate artist exists
    getArtistById(artistId); 
    
    // 2. Query for payments linked to appointments made with this artist
    TypedQuery<Payment> q = em.createQuery(
            "SELECT p FROM Payment p WHERE p.appointment.artist.userId = :artistId ORDER BY p.paymentDate DESC", 
            Payment.class
        ).setParameter("artistId", artistId);

    if (offset >= 0) q.setFirstResult(offset);
    if (limit > 0) q.setMaxResults(limit);

    // Eagerly initialize appointment for display context
    List<Payment> payments = q.getResultList();
    payments.forEach(p -> {
        if (p.getAppointment() != null) p.getAppointment().getAppointmentId();
    });
    return payments;
}

@Override
public List<EarningLog> listEarningLogsForArtist(Long artistId, int offset, int limit) {
    // 1. Validate artist
    getArtistById(artistId); 

    // 2. Query for detailed earning logs for the artist
    TypedQuery<EarningLog> q = em.createQuery(
        "SELECT e FROM EarningLog e WHERE e.artist.userId = :artistId ORDER BY e.calculatedAt DESC", 
        EarningLog.class
    ).setParameter("artistId", artistId);

    if (offset >= 0) q.setFirstResult(offset);
    if (limit > 0) q.setMaxResults(limit);
    
    // Eagerly initialize fields for safe REST serialization
    List<EarningLog> logs = q.getResultList();
    logs.forEach(e -> {
        if (e.getAppointment() != null) e.getAppointment().getAppointmentId();
        if (e.getPayment() != null) e.getPayment().getPaymentId();
        if (e.getPayout() != null) e.getPayout().getPayoutId();
    });
    return logs;
}

@Override
public BigDecimal calculateArtistPendingEarnings(Long artistId) {
    // 1. Validate artist
    getArtistById(artistId); 

    // 2. Sum the artistShare for all EarningLogs where the payoutStatus is PENDING
    String ql = "SELECT SUM(e.artistShare) FROM EarningLog e " +
                "WHERE e.artist.userId = :artistId " +
                "AND e.payoutStatus = 'PENDING'";

    TypedQuery<BigDecimal> q = em.createQuery(ql, BigDecimal.class)
            .setParameter("artistId", artistId);
            
    // SingleResult is null if no PENDING logs exist
    BigDecimal earnings = q.getSingleResult();
    
    return earnings == null ? BigDecimal.ZERO : earnings.setScale(2, BigDecimal.ROUND_HALF_UP);
}

//    @Override
//    public BigDecimal calculateEarnings(Long artistId, LocalDate fromDate, LocalDate toDate) {
//        String ql = "SELECT SUM(p.amount) FROM Payment p JOIN p.appointment a WHERE a.artist.userId = :artistId AND p.status = :completed";
//        if (fromDate != null && toDate != null) {
//            ql += " AND p.paymentDate BETWEEN :from AND :to";
//        }
//        TypedQuery<BigDecimal> q = em.createQuery(ql, BigDecimal.class);
//        q.setParameter("artistId", artistId);
//        q.setParameter("completed", "COMPLETED");
//        if (fromDate != null && toDate != null) {
//            q.setParameter("from", fromDate.atStartOfDay());
//            q.setParameter("to", toDate.plusDays(1).atStartOfDay());
//        }
//        BigDecimal total = q.getSingleResult();
//        return total == null ? BigDecimal.ZERO : total;
//    }
@Override
public List<ArtistPayout> listArtistPayouts(Long artistId, int offset, int limit) {
    // 1. Validate artist
    getArtistById(artistId);

    // 2. Query for historical payout records
    TypedQuery<ArtistPayout> q = em.createQuery(
        "SELECT ap FROM ArtistPayout ap WHERE ap.artist.userId = :artistId ORDER BY ap.payoutDate DESC",
        ArtistPayout.class
    ).setParameter("artistId", artistId);

    if (offset >= 0) q.setFirstResult(offset);
    if (limit > 0) q.setMaxResults(limit);

    // Eagerly initialize Admin field for audit trail
    List<ArtistPayout> payouts = q.getResultList();
    payouts.forEach(ap -> {
        if (ap.getAdmin() != null) ap.getAdmin().getUserId();
    });
    
    return payouts;
}

//    @Override
//    @Transactional
//    public Long requestPayout(Long artistId, BigDecimal amount, Long requestedByAdminOrArtistId) {
//        AppUser artist = em.find(AppUser.class, artistId);
//        if (artist == null) throw new IllegalArgumentException("Artist not found");
//        ArtistPayout payout = new ArtistPayout();
//        payout.setArtist(artist);
//        payout.setAmount(amount);
//        payout.setPayoutDate(LocalDateTime.now());
//        payout.setPayoutStatus("REQUESTED");
//        payout.setCreatedAt(LocalDateTime.now());
//        if (requestedByAdminOrArtistId != null) payout.setAdmin(em.find(AppUser.class, requestedByAdminOrArtistId));
//        em.persist(payout);
//        em.flush();
//        return payout.getPayoutId();
//    }

    // -------------------------
    // Utility
    // -------------------------
    @Override
    public void initializeArtistCollections(AppUser artist) {
        if (artist == null) return;
        if (artist.getDesigns() != null) artist.getDesigns().size();
        if (artist.getArtistAppointments() != null) artist.getArtistAppointments().size();
        if (artist.getSchedules() != null) artist.getSchedules().size();
        if (artist.getTimeSlots() != null) artist.getTimeSlots().size();
        if (artist.getFeedbacksReceived() != null) artist.getFeedbacksReceived().size();
        if (artist.getReviewsReceived() != null) artist.getReviewsReceived().size();
        if (artist.getLoginHistory() != null) artist.getLoginHistory().size();
    }

    @Override
    public TattooDesign getDesignById(Long designId) {
        TattooDesign design = em.find(TattooDesign.class, designId);
        if (design != null) {
            // Initialize collections for REST response (if needed)
            if (design.getLikes() != null) design.getLikes().size();
            if (design.getFavourites() != null) design.getFavourites().size();
        }
        return design; 
    }

    
    
}

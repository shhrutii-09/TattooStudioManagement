package ejb;

import artistDTO.ArtistAppointmentDTO;
import artistDTO.ArtistAppointmentFilterDTO;
import artistDTO.ArtistProfileeeDTO;
import jakarta.persistence.NoResultException;
import entities.*;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import util.PasswordUtil;

@Stateless
public class ArtistEJB implements ArtistEJBLocal {

    @Inject
private beans.UserSessionBean userSession;

    @PersistenceContext(unitName = "TattooPU")
    private EntityManager em;

  private Long getAuthenticatedArtistId() {
        if (userSession == null || !userSession.isArtist()) {
            throw new SecurityException("Artist not authenticated");
        }
        return userSession.getUserId();
    }

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

    @Override
    public void addDesign(Long artistId, TattooDesign newDesign) {
    AppUser artist = em.find(AppUser.class, artistId);
    if (artist == null) throw new IllegalArgumentException("Artist not found");

    // Check duplicate title
    Long count = em.createQuery(
        "SELECT COUNT(d) FROM TattooDesign d " +
        "WHERE d.artist.userId = :artistId AND d.title = :title AND d.isRemovedByArtist = false", Long.class)
        .setParameter("artistId", artistId)
        .setParameter("title", newDesign.getTitle())
        .getSingleResult();
    if (count > 0) throw new IllegalArgumentException("You already have a design with this title.");

    newDesign.setArtist(artist);
    newDesign.setUploadedAt(LocalDateTime.now());

    em.persist(newDesign);
}
    
    @Override
public void updateDesign(Long designId, TattooDesign payload) {
    TattooDesign existing = em.find(TattooDesign.class, designId);
    if (existing == null || Boolean.TRUE.equals(existing.getIsRemovedByArtist()))
        throw new IllegalArgumentException("Design not found");

    // Duplicate title check
    Long count = em.createQuery(
        "SELECT COUNT(d) FROM TattooDesign d " +
        "WHERE d.artist.userId = :artistId AND d.title = :title AND d.designId != :designId " +
        "AND d.isRemovedByArtist = false", Long.class)
        .setParameter("artistId", existing.getArtist().getUserId())
        .setParameter("title", payload.getTitle())
        .setParameter("designId", designId)
        .getSingleResult();

    if (count > 0) throw new IllegalArgumentException("You already have a design with this title.");

    existing.setTitle(payload.getTitle());
    existing.setDescription(payload.getDescription());
    existing.setPrice(payload.getPrice());
    existing.setStyle(payload.getStyle());
    existing.setImagePath(payload.getImagePath());

    em.merge(existing);
}

        
@Override
    public List<TattooDesign> getArtistDesigns(Long artistId, int offset, int limit) {
        return em.createQuery(
            "SELECT d FROM TattooDesign d " +
            "LEFT JOIN FETCH d.likes " +
            "LEFT JOIN FETCH d.favourites " +
            "WHERE d.artist.userId = :artistId " +
            "AND d.isRemovedByArtist = false " +
            "ORDER BY d.uploadedAt DESC", TattooDesign.class)
            .setParameter("artistId", artistId)
            .setFirstResult(offset)
            .setMaxResults(limit)
            .getResultList();
    }



   @Override
public List<TattooDesign> searchDesigns(String qStr, String style, BigDecimal minPrice, BigDecimal maxPrice, int offset, int limit) {
StringBuilder ql = new StringBuilder(
        "SELECT d FROM TattooDesign d WHERE " +
        " (d.isBanned = false OR d.isBanned IS NULL) " +
        " AND (d.isRemovedByArtist = false OR d.isRemovedByArtist IS NULL) "
    );

    if (qStr != null && !qStr.isBlank())
        ql.append(" AND (LOWER(d.title) LIKE :q OR LOWER(d.description) LIKE :q) ");

    if (style != null && !style.isBlank())
        ql.append(" AND LOWER(d.style) = :style ");

    if (minPrice != null)
        ql.append(" AND d.price >= :minPrice ");

    if (maxPrice != null)
        ql.append(" AND d.price <= :maxPrice ");

    ql.append(" ORDER BY d.uploadedAt DESC");

    TypedQuery<TattooDesign> q = em.createQuery(ql.toString(), TattooDesign.class);

    if (qStr != null && !qStr.isBlank())
        q.setParameter("q", "%" + qStr.toLowerCase() + "%");

    if (style != null && !style.isBlank())
        q.setParameter("style", style.toLowerCase());

    if (minPrice != null)
        q.setParameter("minPrice", minPrice);

    if (maxPrice != null)
        q.setParameter("maxPrice", maxPrice);

    if (offset >= 0)
        q.setFirstResult(offset);

    if (limit > 0)
        q.setMaxResults(limit);

    List<TattooDesign> list = q.getResultList();

    list.forEach(d -> {
        if (d.getArtist() != null) d.getArtist().getUserId();
        if (d.getLikes() != null) d.getLikes().size();
        if (d.getFavourites() != null) d.getFavourites().size();
    });

    return list;
}

// In ArtistEJB.java - Add these methods
@Override
@Transactional
public boolean blockOwnSlotWithNotification(Integer slotId, Long artistId, String reason) {
    try {
        TimeSlot slot = em.find(TimeSlot.class, slotId);
        if (slot == null) {
            throw new IllegalArgumentException("Slot not found: " + slotId);
        }
        
        // Verify artist owns this slot
        if (!slot.getArtist().getUserId().equals(artistId)) {
            throw new IllegalStateException("You can only block your own time slots.");
        }
        
        // Check if slot is booked
        boolean isBooked = false;
        try {
            Appointment appointment = em.createQuery(
                "SELECT a FROM Appointment a WHERE a.slot.slotId = :slotId", 
                Appointment.class)
                .setParameter("slotId", slotId)
                .getSingleResult();
            isBooked = true;
            
            // If booked, cancel the appointment
            appointment.setStatus("CANCELLED");
            appointment.setCancellationReason(
                "Appointment cancelled because time slot was blocked by artist. Reason: " + reason);
            em.merge(appointment);
            
            // Notify client
            createArtistBlockedSlotNotification(appointment.getClient().getUserId(), 
                                              slot, reason, true);
        } catch (NoResultException e) {
            // Slot not booked, that's fine
        }
        
        // Block the slot
        slot.setStatus(TimeSlot.TimeSlotStatus.BLOCKED);
        slot.setBlockReason(reason);
        slot.setBlockedBy(null); // Artist blocking, not admin
        em.merge(slot);
        
        return true;
        
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

private void createArtistBlockedSlotNotification(Long clientId, TimeSlot slot, 
                                                String reason, boolean appointmentCancelled) {
    AppUser artist = slot.getArtist();
    
    // Create announcement for client (if appointment was cancelled)
    if (appointmentCancelled && clientId != null) {
        Announcement announcement = new Announcement();
        announcement.setTitle("Appointment Cancelled - Slot Blocked by Artist");
        announcement.setMessage(String.format(
            "Your appointment with %s on %s has been cancelled because the artist blocked the time slot. " +
            "Reason: %s. Please book another slot.",
            artist.getFullName(),
            slot.getStartTime(),
            reason
        ));
        announcement.setPostedAt(LocalDateTime.now());
        announcement.setPostedBy(artist);
        announcement.setTargetRole("CLIENT");
        em.persist(announcement);
    }
}

@Override
@Transactional
public boolean unblockOwnSlotWithNotification(Integer slotId, Long artistId) {
    try {
        TimeSlot slot = em.find(TimeSlot.class, slotId);
        if (slot == null) {
            throw new IllegalArgumentException("Slot not found: " + slotId);
        }
        
        // Verify artist owns this slot
        if (!slot.getArtist().getUserId().equals(artistId)) {
            throw new IllegalStateException("You can only unblock your own time slots.");
        }
        
        // Only unblock if currently blocked (and not by admin)
        if (slot.getStatus() != TimeSlot.TimeSlotStatus.BLOCKED) {
            throw new IllegalStateException("Slot is not blocked.");
        }
        
        // Check if blocked by admin (admin blocks should only be removed by admin)
        if (slot.getBlockedBy() != null) {
            throw new IllegalStateException("This slot was blocked by admin. Please contact admin to unblock.");
        }
        
        // Unblock the slot
        slot.setStatus(TimeSlot.TimeSlotStatus.AVAILABLE);
        slot.setBlockReason(null);
        em.merge(slot);
        
        return true;
        
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

// In ArtistEJB.java - Add this method
@Override
public List<TimeSlot> getBlockedSlots(Long artistId) {
    return em.createQuery(
        "SELECT t FROM TimeSlot t WHERE " +
        "t.artist.userId = :artistId AND " +
        "t.status = 'BLOCKED' AND " +
        "t.startTime > CURRENT_TIMESTAMP " +  // Future blocked slots only
        "ORDER BY t.startTime ASC", 
        TimeSlot.class)
        .setParameter("artistId", artistId)
        .getResultList();
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


    @Override
@Transactional
public ArtistSchedule saveArtistSchedule(Long artistId, ArtistSchedule schedule) {
    try {
        AppUser artist = em.find(AppUser.class, artistId);
        if (artist == null) throw new IllegalArgumentException("Artist not found: " + artistId);
        
        // Check if schedule exists for this day
        ArtistSchedule existing = null;
        try {
            existing = em.createQuery(
                "SELECT s FROM ArtistSchedule s WHERE s.artist.userId = :artistId AND s.dayOfWeek = :day", 
                ArtistSchedule.class)
                .setParameter("artistId", artistId)
                .setParameter("day", schedule.getDayOfWeek())
                .getSingleResult();
        } catch (NoResultException e) {
            // No existing schedule, that's fine
        }
        
        schedule.setArtist(artist);
        
        if (existing != null) {
            // Update existing
            existing.setStartTime(schedule.getStartTime());
            existing.setEndTime(schedule.getEndTime());
            existing.setWorking(schedule.isWorking());
            return em.merge(existing);
        } else {
            // Create new
            if (schedule.getId() != null) {
                return em.merge(schedule);
            } else {
                em.persist(schedule);
                return schedule;
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
        throw e;
    }
}
    
// In ArtistEJB.java - Update listAvailableTimeSlots method
@Override
public List<TimeSlot> listAvailableTimeSlots(Long artistId) {
    try {
        TypedQuery<TimeSlot> query = em.createQuery(
            "SELECT t FROM TimeSlot t WHERE " +
            "t.artist.userId = :artistId AND " +
            "t.status = :status AND " +
            "t.startTime > CURRENT_TIMESTAMP " +  // Only future slots
            "ORDER BY t.startTime ASC", 
            TimeSlot.class);
        
        query.setParameter("artistId", artistId);
        query.setParameter("status", TimeSlot.TimeSlotStatus.AVAILABLE);
        
        return query.getResultList();
        
    } catch (Exception e) {
        System.err.println("Error listing available slots: " + e.getMessage());
        return List.of(); // Return an empty list on failure
    }
}

   @Override
@Transactional
public List<TimeSlot> generateTimeSlotsForArtist(Long artistId, LocalDate startDate, LocalDate endDate, int slotDurationMinutes) {
    AppUser artist = em.find(AppUser.class, artistId);
    if (artist == null) throw new IllegalArgumentException("Artist not found.");
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
    try {
        List<TimeSlot> list = em.createQuery(
            "SELECT t FROM TimeSlot t WHERE t.artist.userId = :id ORDER BY t.startTime", 
            TimeSlot.class)
            .setParameter("id", artistId)
            .getResultList();
        
        // Initialize relationships
        for (TimeSlot t : list) {
            if (t.getArtist() != null) t.getArtist().getUserId();
        }
        
        return list;
    } catch (Exception e) {
        e.printStackTrace();
        return new ArrayList<>();
    }
}

   @Override
public List<ArtistSchedule> listSchedulesForArtist(Long artistId) {
    try {
        AppUser artist = em.find(AppUser.class, artistId);
        if (artist == null) {
            throw new IllegalArgumentException("Artist not found with ID: " + artistId);
        }
        
        return em.createQuery(
            "SELECT s FROM ArtistSchedule s WHERE s.artist.userId = :artistId ORDER BY s.dayOfWeek ASC", 
            ArtistSchedule.class)
            .setParameter("artistId", artistId)
            .getResultList();
    } catch (Exception e) {
        e.printStackTrace();
        return new ArrayList<>();
    }
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
    try {
        TimeSlot t = em.find(TimeSlot.class, slotId);
        if (t == null) throw new IllegalArgumentException("Slot not found");
        if (start != null) t.setStartTime(start);
        if (end != null) t.setEndTime(end);
        if (status != null) t.setStatus(status);
        return em.merge(t);
    } catch (Exception e) {
        e.printStackTrace();
        throw e;
    }
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
        TypedQuery<Appointment> q = em.createQuery(
            "SELECT a FROM Appointment a WHERE a.artist.userId = :id ORDER BY a.appointmentDateTime DESC",
            Appointment.class
        ).setParameter("id", artistId);
        if (offset >= 0) q.setFirstResult(offset);
        if (limit > 0) q.setMaxResults(limit);
        List<Appointment> list = q.getResultList();
        list.forEach(a -> { if (a.getSlot() != null) a.getSlot().getSlotId(); });
        return list;
    }

   @Override
    public Appointment getAppointment(Long appointmentId) {
        Appointment a = em.find(Appointment.class, appointmentId);
        if (a == null) throw new IllegalArgumentException("Appointment not found: " + appointmentId);
        if (a.getSlot() != null) a.getSlot().getSlotId();
        if (a.getClient() != null) a.getClient().getUserId();
        return a;
    }

     @Override
    @Transactional
    public Appointment changeAppointmentStatus(Long appointmentId, String status, String cancellationReason) {
        Appointment a = em.find(Appointment.class, appointmentId);
        if (a == null) throw new IllegalArgumentException("Appointment not found: " + appointmentId);
        a.setStatus(status);
        a.setCancellationReason("CANCELLED".equalsIgnoreCase(status) ? cancellationReason : null);
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
    getArtistById(artistId); // security check

    return em.createQuery(
        "SELECT e FROM EarningLog e WHERE e.artist.userId = :artistId ORDER BY e.calculatedAt DESC",
        EarningLog.class
    )
    .setParameter("artistId", artistId)
    .setFirstResult(offset)
    .setMaxResults(limit)
    .getResultList();
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
    
    @Override
public List<TattooDesign> listBannedDesigns(Long artistId) {
    return em.createQuery(
        "SELECT d FROM TattooDesign d WHERE d.artist.userId = :id AND d.isBanned = true ORDER BY d.bannedAt DESC",
        TattooDesign.class
    )
    .setParameter("id", artistId)
    .getResultList();
}

@Override
@Transactional
public void deleteDesignPermanently(Long designId, Long artistId) {
    TattooDesign d = em.find(TattooDesign.class, designId);
    if (d == null)
        throw new IllegalArgumentException("Design not found: " + designId);

    if (!d.getArtist().getUserId().equals(artistId))
        throw new IllegalArgumentException("Artist does not own this design.");

    // Must be banned before deletion
    if (!Boolean.TRUE.equals(d.getIsBanned())) {
        throw new IllegalStateException("Design must be banned by admin before artist can remove it.");
    }

    // 1. Clear appointment references
    em.createQuery("UPDATE Appointment a SET a.design = NULL WHERE a.design.designId = :id")
        .setParameter("id", designId)
        .executeUpdate();

    // 2. Remove likes & favourites (cascade handles this automatically)

    // 3. Mark as removed
    d.setIsRemovedByArtist(true);
    d.setRemovedAt(LocalDateTime.now());

    // 4. Mark invisible
    d.setIsBanned(true);   // still banned
    d.setBannedReason("Removed by artist");
    d.setBannedAt(LocalDateTime.now());

    em.merge(d);
}

@Override
public void deleteDesign(Long designId, Long artistId) {
    TattooDesign design = em.find(TattooDesign.class, designId);
    if (design == null) throw new IllegalArgumentException("Design not found");
    if (!design.getArtist().getUserId().equals(artistId))
        throw new IllegalArgumentException("Unauthorized");

    design.setIsRemovedByArtist(true);
    design.setRemovedAt(LocalDateTime.now());

    em.merge(design);
}
    
    
    @Override
    public long countDesignsByArtist(Long artistId) {
        return em.createQuery(
            "SELECT COUNT(d) FROM TattooDesign d WHERE d.artist.userId = :id " +
            "AND (d.isRemovedByArtist = false OR d.isRemovedByArtist IS NULL)", Long.class)
            .setParameter("id", artistId)
            .getSingleResult();
    }

    // In ArtistEJB.java
@Override
public long countTodaysAppointments(Long artistId) {
    // This now counts PENDING requests received TODAY.
    LocalDate today = LocalDate.now();
    LocalDateTime startOfToday = today.atStartOfDay();
    LocalDateTime endOfToday = today.plusDays(1).atStartOfDay();

    return em.createQuery(
        "SELECT COUNT(a) FROM Appointment a WHERE a.artist.userId = :id " +
        // Using the new REQUEST_DATETIME field to filter by the request date
        "AND a.requestDateTime >= :startOfToday " + 
        "AND a.requestDateTime < :endOfToday " +
        // Only counting new PENDING requests, since the scheduled datetime is NULL
        "AND a.status = 'PENDING'", Long.class)
        .setParameter("id", artistId)
        .setParameter("startOfToday", startOfToday)
        .setParameter("endOfToday", endOfToday)
        .getSingleResult();
}
@Override
public long countPendingRequestsToday(Long artistId) {
    LocalDate today = LocalDate.now();
    LocalDateTime startOfToday = today.atStartOfDay();
    LocalDateTime endOfToday = today.plusDays(1).atStartOfDay();

    // Counts PENDING appointments requested today
    return em.createQuery(
        "SELECT COUNT(a) FROM Appointment a WHERE a.artist.userId = :id " +
        "AND a.requestDateTime >= :startOfToday " + // **Check #1: Using requestDateTime**
        "AND a.requestDateTime < :endOfToday " +    // **Check #2: Correct date range**
        "AND a.status = 'PENDING'", Long.class)     // **Check #3: Filtering by status**
        .setParameter("id", artistId)
        .setParameter("startOfToday", startOfToday)
        .setParameter("endOfToday", endOfToday)
        .getSingleResult();
}

    @Override
public long countUpcomingAppointments(Long artistId) {
    return em.createQuery(
        "SELECT COUNT(a) FROM Appointment a WHERE a.artist.userId = :id " +
        "AND a.appointmentDateTime > CURRENT_TIMESTAMP " +
        "AND a.status = 'CONFIRMED'", Long.class)  // Changed from 'SCHEDULED' to 'CONFIRMED'
        .setParameter("id", artistId)
        .getSingleResult();
}

    @Override
    public long countCompletedAppointments(Long artistId) {
        return em.createQuery(
            "SELECT COUNT(a) FROM Appointment a WHERE a.artist.userId = :id " +
            "AND a.status = 'COMPLETED'", Long.class)
            .setParameter("id", artistId)
            .getSingleResult();
    }
    
    // ... inside ArtistEJB class ...

    @Override
    public double calculateAverageRating(Long artistId) {
        try {
            Double avg = em.createQuery(
                "SELECT AVG(r.rating) FROM Review r WHERE r.artist.userId = :id", Double.class)
                .setParameter("id", artistId)
                .getSingleResult();
            return avg != null ? avg : 0.0;
        } catch (NoResultException e) {
            return 0.0;
        }
    }

    @Override
    public double calculateTotalEarnings(Long artistId) {
        try {
            BigDecimal total = em.createQuery(
                "SELECT SUM(e.artistShare) FROM EarningLog e WHERE e.artist.userId = :id", BigDecimal.class)
                .setParameter("id", artistId)
                .getSingleResult();
            return total != null ? total.doubleValue() : 0.0;
        } catch (NoResultException e) {
            return 0.0;
        }
    }

    @Override
    public List<Appointment> listRecentAppointmentsForArtist(Long artistId, int limit) {
        // Returns the most recent appointments (Upcoming first, then by date)
        TypedQuery<Appointment> q = em.createQuery(
            "SELECT a FROM Appointment a WHERE a.artist.userId = :id " +
            "ORDER BY a.appointmentDateTime DESC", Appointment.class)
            .setParameter("id", artistId);
            
        if (limit > 0) q.setMaxResults(limit);
        
        List<Appointment> list = q.getResultList();
        // Initialize relationships to avoid LazyInitializationException in the view
        list.forEach(a -> {
            if(a.getClient() != null) a.getClient().getFullName();
            if(a.getDesign() != null) a.getDesign().getTitle();
        });
        return list;
    }

    
    // In ArtistEJB.java - Add these appointment management methods
@Override
    public void approveAppointment(Long appointmentId, Integer slotId) {
        Long artistId = getAuthenticatedArtistId();
        Appointment appointment = em.find(Appointment.class, appointmentId);
        if (!appointment.getArtist().getUserId().equals(artistId)) throw new SecurityException("Unauthorized");
        TimeSlot slot = em.find(TimeSlot.class, slotId);
        if (slot.getStatus() != TimeSlot.TimeSlotStatus.AVAILABLE) throw new IllegalStateException("Slot not available");
        appointment.setSlot(slot);
        appointment.setStatus("CONFIRMED");
        slot.setStatus(TimeSlot.TimeSlotStatus.BOOKED);
        em.merge(appointment);
        em.merge(slot);
    }

@Override
public List<TimeSlot> getAvailableTimeSlotsForMyAppointment(LocalDate date) {

    Long artistId = getAuthenticatedArtistId(); // 🔐 server-side identity

    return em.createQuery(
            "SELECT ts FROM TimeSlot ts " +
            "WHERE ts.artist.userId = :artistId " +
            "AND ts.status = :status " +
            "AND DATE(ts.startTime) = :date " +
            "ORDER BY ts.startTime",
            TimeSlot.class)
        .setParameter("artistId", artistId)
        .setParameter("status", TimeSlot.TimeSlotStatus.AVAILABLE)
        .setParameter("date", date)
        .getResultList();
}


@Override
    public void rejectAppointment(Long appointmentId, String reason) {
        Long artistId = getAuthenticatedArtistId();
        Appointment appointment = em.find(Appointment.class, appointmentId);
        if (!appointment.getArtist().getUserId().equals(artistId)) throw new SecurityException("Unauthorized");
        appointment.setStatus("REJECTED");
        appointment.setCancellationReason(reason);
        em.merge(appointment);
    }



@Override
    public void completeAppointment(Long appointmentId) {
        Long artistId = getAuthenticatedArtistId();
        Appointment appointment = em.find(Appointment.class, appointmentId);
        if (!appointment.getArtist().getUserId().equals(artistId)) throw new SecurityException("Unauthorized");
        appointment.setStatus("COMPLETED");
        em.merge(appointment);
    }
    
@Override
    public List<TimeSlot> getAvailableTimeSlotsForAppointment(Long artistId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        return em.createQuery(
            "SELECT t FROM TimeSlot t WHERE t.artist.userId = :artistId AND t.startTime >= :start AND t.startTime < :end AND t.status = 'AVAILABLE' ORDER BY t.startTime ASC",
            TimeSlot.class
        ).setParameter("artistId", artistId)
         .setParameter("start", startOfDay)
         .setParameter("end", endOfDay)
         .getResultList();
    }

// Update this in ArtistEJB.java
// In ArtistEJB.java

// In ArtistEJB.java
@Override
    public List<Appointment> getMyAppointments(String statusFilter, int offset, int limit) {
        Long artistId = getAuthenticatedArtistId();
        StringBuilder jpql = new StringBuilder("SELECT a FROM Appointment a WHERE a.artist.userId = :artistId ");
        if (statusFilter != null && !"ALL".equalsIgnoreCase(statusFilter)) jpql.append("AND a.status = :status ");
        jpql.append("ORDER BY a.requestDateTime DESC");
        TypedQuery<Appointment> q = em.createQuery(jpql.toString(), Appointment.class).setParameter("artistId", artistId);
        if (statusFilter != null && !"ALL".equalsIgnoreCase(statusFilter)) q.setParameter("status", statusFilter);
        if (offset >= 0) q.setFirstResult(offset);
        if (limit > 0) q.setMaxResults(limit);
        return q.getResultList();
    }

// File: ArtistEJB.java


 @Override
    @Transactional
    public void autoCompletePaidAppointments(Long artistId) {
        List<Appointment> appointments = em.createQuery(
            "SELECT a FROM Appointment a WHERE a.artist.userId = :artistId AND a.status = 'CONFIRMED' AND a.appointmentDateTime < CURRENT_TIMESTAMP AND a.payment IS NOT NULL AND a.payment.status = 'COMPLETED'",
            Appointment.class
        ).setParameter("artistId", artistId).getResultList();
        appointments.forEach(a -> {
            a.setStatus("COMPLETED");
            em.merge(a);
        });
    }

    
@Override
@Transactional
public void assignTimeSlot(Long appointmentId, Integer slotId, LocalDateTime newStart, LocalDateTime newEnd) {

    Long artistId = getAuthenticatedArtistId(); // 🔐 server decides

    Appointment appointment = em.find(Appointment.class, appointmentId);
    if (appointment == null) throw new IllegalArgumentException("Appointment not found");

    if (!appointment.getArtist().getUserId().equals(artistId)) {
        throw new SecurityException("You can only assign slots to your own appointments");
    }

    TimeSlot slot = em.find(TimeSlot.class, slotId);
    if (slot == null) throw new IllegalArgumentException("Time slot not found");

    if (!slot.getArtist().getUserId().equals(artistId)) {
        throw new SecurityException("You can only use your own time slots");
    }

    if (slot.getStatus() != TimeSlot.TimeSlotStatus.AVAILABLE) {
        throw new IllegalStateException("Time slot is not available");
    }

    // ✅ Update start/end time if provided
    if (newStart != null && newEnd != null) {
        slot.setStartTime(newStart);
        slot.setEndTime(newEnd);
    }

    // ✅ Business update
    appointment.setSlot(slot);
    appointment.setStatus("CONFIRMED");

    slot.setStatus(TimeSlot.TimeSlotStatus.BOOKED);

    em.merge(appointment);
    em.merge(slot);
}
    // =========================
    // WEEKLY SCHEDULE
    // =========================
 public List<ArtistSchedule> getMyWeeklySchedule() {
    Long artistId = getAuthenticatedArtistId();
    return em.createQuery(
            "SELECT s FROM ArtistSchedule s WHERE s.artist.userId = :artistId ORDER BY s.dayOfWeek",
            ArtistSchedule.class)
        .setParameter("artistId", artistId)
        .getResultList();
}

 @Override
@Transactional
public void saveSchedule(ArtistSchedule schedule) {
    AppUser artist = em.find(AppUser.class, getAuthenticatedArtistId());
    if (artist == null) throw new IllegalArgumentException("Artist not found");

    schedule.setArtist(artist);

    if (schedule.getId() == null) {
        em.persist(schedule);
    } else {
        em.merge(schedule);
    }
}

    // =========================
    // TIME SLOTS
    // =========================
    @Override
   public List<TimeSlot> getMyTimeSlots() {
    Long artistId = getAuthenticatedArtistId();
    return em.createQuery(
            "SELECT t FROM TimeSlot t WHERE t.artist.userId = :artistId ORDER BY t.startTime",
            TimeSlot.class)
        .setParameter("artistId", artistId)
        .getResultList();
}
   @Override
    @Transactional
public void generateSlotsNext7Days() {
    Long artistId = getAuthenticatedArtistId();
    List<ArtistSchedule> weeklySchedule = getMyWeeklySchedule();

    LocalDate today = LocalDate.now();
    LocalDate endDate = today.plusDays(7);

    for (LocalDate date = today; !date.isAfter(endDate); date = date.plusDays(1)) {
        DayOfWeek dow = date.getDayOfWeek();

        for (ArtistSchedule schedule : weeklySchedule) {
            if (!schedule.isWorking() || schedule.getDayOfWeek() != dow) continue;

            LocalTime start = schedule.getStartTime();
            LocalTime end = schedule.getEndTime();
            LocalDateTime slotStart = LocalDateTime.of(date, start);

            while (slotStart.toLocalTime().isBefore(end)) {
                LocalDateTime slotEnd = slotStart.plusMinutes(60); // 1-hour slots
                if (slotEnd.toLocalTime().isAfter(end)) break;

                // Avoid duplicate slots
                Long count = em.createQuery(
                        "SELECT COUNT(t) FROM TimeSlot t WHERE t.artist.userId = :artistId AND t.startTime = :start",
                        Long.class)
                    .setParameter("artistId", artistId)
                    .setParameter("start", slotStart)
                    .getSingleResult();

                if (count == 0) {
                    TimeSlot ts = new TimeSlot();
                    ts.setArtist(em.getReference(AppUser.class, artistId));
                    ts.setStartTime(slotStart);
                    ts.setEndTime(slotEnd);
                    ts.setStatus(TimeSlot.TimeSlotStatus.AVAILABLE);
                    em.persist(ts);
                }

                slotStart = slotStart.plusMinutes(60);
            }
        }
    }
}

@Override
    @Transactional
public void blockSlot(Integer slotId, String reason) {
    TimeSlot slot = em.find(TimeSlot.class, slotId);
    if (slot == null) throw new IllegalArgumentException("Slot not found");
    if (!slot.getArtist().getUserId().equals(getAuthenticatedArtistId()))
        throw new SecurityException("Unauthorized");

    slot.setStatus(TimeSlot.TimeSlotStatus.BLOCKED);
    slot.setBlockReason(reason);
    em.merge(slot);
}
@Override
@Transactional
public void unblockSlot(Integer slotId) {
    TimeSlot slot = em.find(TimeSlot.class, slotId);
    if (slot == null) throw new IllegalArgumentException("Slot not found");
    if (!slot.getArtist().getUserId().equals(getAuthenticatedArtistId()))
        throw new SecurityException("Unauthorized");

    if (slot.getStatus() == TimeSlot.TimeSlotStatus.BLOCKED) {
        slot.setStatus(TimeSlot.TimeSlotStatus.AVAILABLE);
        slot.setBlockReason(null);
        em.merge(slot);
    }
}

@Override
public List<TimeSlot> getAvailableTimeSlotsForDate(LocalDate date) {
    Long artistId = getAuthenticatedArtistId();
    LocalDateTime startOfDay = date.atStartOfDay();
    LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

    return em.createQuery(
            "SELECT t FROM TimeSlot t WHERE t.artist.userId = :artistId AND t.status = :status " +
            "AND t.startTime >= :start AND t.startTime < :end ORDER BY t.startTime",
            TimeSlot.class)
        .setParameter("artistId", artistId)
        .setParameter("status", TimeSlot.TimeSlotStatus.AVAILABLE)
        .setParameter("start", startOfDay)
        .setParameter("end", endOfDay)
        .getResultList();
}

@Override
    public ArtistProfileeeDTO getArtistProfile(Long requestedId) {
        // SECURITY: If the logged-in user is an Artist, force the ID to match their session.
        if (userSession.isArtist()) {
            requestedId = userSession.getUserId();
        }

        AppUser artist = em.find(AppUser.class, requestedId);
        if (artist == null) return null;

        // Map AppUser to DTO
        ArtistProfileeeDTO dto = new ArtistProfileeeDTO();
        dto.setUserId(artist.getUserId());
        dto.setUsername(artist.getUsername());
        dto.setFullName(artist.getFullName());
        dto.setEmail(artist.getEmail());
        dto.setPhone(artist.getPhone());
        
        // Fix: Get Role Name string, not the object
        if (artist.getRole() != null) {
            dto.setRole(artist.getRole().getRoleName());
        }
        
        // Fix: Use correct getter from AppUser entity
        dto.setVerified(artist.isIsVerified()); 

        // Fix: Retrieve Portfolio/Experience data from the Experience entity, not AppUser
        Experience exp = artist.getExperience();
        if (exp != null) {
            dto.setExperienceId(exp.getExperienceId()); // Fix: use getExperienceId()
            dto.setPortfolioLink(exp.getPortfolioLink()); // Fix: portfolio is here
            dto.setSpecialties(exp.getSpecialties());
            dto.setYearsExperience(exp.getYearsExperience());
            dto.setStatus(exp.getStatus());
        }

        return dto;
    }    
@Override
    public boolean updateArtistDetails(ArtistProfileeeDTO dto) {
        try {
            // SECURITY: Ignore dto.getUserId(). Use the Session ID.
            Long currentArtistId = userSession.getUserId();

            AppUser artist = em.find(AppUser.class, currentArtistId);
            if (artist == null) return false;

            // Update AppUser fields
            artist.setFullName(dto.getFullName());
            artist.setPhone(dto.getPhone());
            artist.setEmail(dto.getEmail());
            artist.setUsername(dto.getUsername());

            // NOTE: We do NOT update PortfolioLink here because that belongs 
            // to the Experience entity. That is handled in saveOrUpdateExperience.

            em.merge(artist);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
@Override
@Transactional
public boolean changeArtistPassword(Long artistId, String oldPassword, String newPassword) {
    try {
        AppUser artist = em.find(AppUser.class, artistId);
        if (artist == null) return false;

        // 🔐 Verify old password (hashed)
        if (!PasswordUtil.verifyPassword(oldPassword, artist.getPassword())) {
            return false;
        }

        // 🔐 Store hashed password
        String hashed = PasswordUtil.hashPassword(newPassword);
        artist.setPassword(hashed);

        em.merge(artist);
        return true;

    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}
@Override
    public boolean saveOrUpdateExperience(ArtistProfileeeDTO dto) {
        try {
            // SECURITY: Force Session ID
            Long currentArtistId = userSession.getUserId();

            AppUser artist = em.find(AppUser.class, currentArtistId);
            if (artist == null) return false;

            // Fix: Use 'Experience' entity, not 'ArtistExperience'
            Experience exp = artist.getExperience();
            
            if (exp == null) {
                exp = new Experience();
                exp.setArtist(artist);
                // We don't set ID manually, let DB generate it
            }

            // Update fields from DTO
            exp.setPortfolioLink(dto.getPortfolioLink());
            exp.setSpecialties(dto.getSpecialties());
            exp.setYearsExperience(dto.getYearsExperience());
            exp.setStatus(dto.getStatus());

            // Persist or Merge
            if (exp.getExperienceId() == null) {
                em.persist(exp);
                artist.setExperience(exp); // Link it back to artist
                em.merge(artist);
            } else {
                em.merge(exp);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

@Override
    public List<ArtistAppointmentDTO> getAppointmentsForArtist(
            Long artistId,
            ArtistAppointmentFilterDTO filter) {

        StringBuilder jpql = new StringBuilder(
            "SELECT a FROM Appointment a WHERE a.artist.userId = :artistId"
        );

        if (filter != null && filter.hasFilters()) {

            if (filter.getClientName() != null && !filter.getClientName().isBlank()) {
                jpql.append(" AND LOWER(a.client.fullName) LIKE :clientName");
            }

            if (filter.getStatus() != null && !"ALL".equals(filter.getStatus())) {
                jpql.append(" AND a.status = :status");
            }

            if (filter.getStartDate() != null) {
                jpql.append(" AND a.appointmentDateTime >= :startDate");
            }

            if (filter.getEndDate() != null) {
                jpql.append(" AND a.appointmentDateTime <= :endDate");
            }
        }

        jpql.append(" ORDER BY a.requestDateTime DESC");

        TypedQuery<Appointment> query =
                em.createQuery(jpql.toString(), Appointment.class);

        query.setParameter("artistId", artistId);

        if (filter != null && filter.hasFilters()) {

            if (filter.getClientName() != null && !filter.getClientName().isBlank()) {
                query.setParameter(
                    "clientName",
                    "%" + filter.getClientName().toLowerCase() + "%"
                );
            }

            if (filter.getStatus() != null && !"ALL".equals(filter.getStatus())) {
                query.setParameter("status", filter.getStatus());
            }

            if (filter.getStartDate() != null) {
                query.setParameter(
                    "startDate",
                    filter.getStartDate().atStartOfDay()
                );
            }

            if (filter.getEndDate() != null) {
                query.setParameter(
                    "endDate",
                    filter.getEndDate().atTime(23, 59, 59)
                );
            }
        }

        query.setFirstResult(filter.getPage() * filter.getSize());
        query.setMaxResults(filter.getSize());

        List<Appointment> results = query.getResultList();

        return results.stream()
                .map(this::toArtistDTO)
                .collect(Collectors.toList());
    }

    /* =========================================================
       2️⃣ CONFIRM APPOINTMENT (ONLY OWN ARTIST)
       ========================================================= */
    @Override
    public void confirmAppointment(Long appointmentId, Long artistId) {

        Appointment appt = getOwnedAppointment(appointmentId, artistId);

        if (!"PENDING".equals(appt.getStatus())) {
            throw new IllegalStateException("Only PENDING appointments can be confirmed");
        }

        appt.setStatus("CONFIRMED");
        em.merge(appt);
    }

    /* =========================================================
       3️⃣ CANCEL APPOINTMENT (ONLY OWN ARTIST)
       ========================================================= */
    @Override
    public void cancelAppointment(Long appointmentId, String reason, Long artistId) {

        Appointment appt = getOwnedAppointment(appointmentId, artistId);

        if ("COMPLETED".equals(appt.getStatus())) {
            throw new IllegalStateException("Completed appointment cannot be cancelled");
        }

        appt.setStatus("CANCELLED");
        appt.setCancellationReason(reason);

        // Free slot if assigned
        if (appt.getSlot() != null) {
            TimeSlot slot = appt.getSlot();
            slot.setStatus(TimeSlot.TimeSlotStatus.AVAILABLE);
            appt.setSlot(null);
            em.merge(slot);
        }

        em.merge(appt);
    }

    /* =========================================================
       4️⃣ ASSIGN SLOT TO APPOINTMENT (ARTIST OWN SLOT ONLY)
       ========================================================= */
    @Override
    public void assignSlotToAppointment(
            Long appointmentId,
            Integer slotId,
            Long artistId) {

        Appointment appt = getOwnedAppointment(appointmentId, artistId);

        if (!"PENDING".equals(appt.getStatus())) {
            throw new IllegalStateException("Slot can be assigned only to PENDING appointment");
        }

        TimeSlot slot = em.find(TimeSlot.class, slotId);

        if (slot == null) {
            throw new IllegalArgumentException("Time slot not found");
        }

        // 🔒 CRITICAL SECURITY CHECK
        if (!slot.getArtist().getUserId().equals(artistId)) {
            throw new SecurityException("Cannot assign another artist's slot");
        }

        if (slot.getStatus() != TimeSlot.TimeSlotStatus.AVAILABLE) {
            throw new IllegalStateException("Slot is not available");
        }

        slot.setStatus(TimeSlot.TimeSlotStatus.BOOKED);
        appt.setSlot(slot);

        em.merge(slot);
        em.merge(appt);
    }

    /* =========================================================
       🔒 SHARED SECURITY METHOD
       ========================================================= */
    private Appointment getOwnedAppointment(Long appointmentId, Long artistId) {

        Appointment appt = em.find(Appointment.class, appointmentId);

        if (appt == null) {
            throw new IllegalArgumentException("Appointment not found");
        }

        if (!appt.getArtist().getUserId().equals(artistId)) {
            throw new SecurityException("Unauthorized artist access");
        }

        return appt;
    }

    /* =========================================================
       DTO MAPPER (ARTIST SAFE)
       ========================================================= */
    private ArtistAppointmentDTO toArtistDTO(Appointment a) {

        ArtistAppointmentDTO dto = new ArtistAppointmentDTO();

        dto.setAppointmentId(a.getAppointmentId());
        dto.setStatus(a.getStatus());
        dto.setAppointmentDateTime(a.getAppointmentDateTime());
        dto.setClientNote(a.getClientNote());
        dto.setCancellationReason(a.getCancellationReason());

        if (a.getClient() != null) {
            dto.setClientName(a.getClient().getFullName());
            dto.setClientEmail(a.getClient().getEmail());
        }

        if (a.getDesign() != null) {
            dto.setDesignTitle(a.getDesign().getTitle());
        }

        if (a.getSlot() != null) {
            dto.setSlotId(a.getSlot().getSlotId());
        }

        if (a.getPayment() != null) {
            dto.setAmount(a.getPayment().getAmount());
            dto.setPaymentStatus(a.getPayment().getStatus());
        }

        return dto;
    }
   
  @Override
public List<TimeSlot> getAvailableSlotsForArtist(Long artistId) {

    return em.createQuery(
        "SELECT s FROM TimeSlot s " +
        "WHERE s.artist.userId = :artistId " +
        "AND s.status = :status " +
        "ORDER BY s.startTime",
        TimeSlot.class
    )
    .setParameter("artistId", artistId)
    .setParameter("status", TimeSlot.TimeSlotStatus.AVAILABLE)
    .getResultList();
}

 } 

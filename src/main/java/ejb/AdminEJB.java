package ejb;
import jakarta.persistence.NoResultException;
import java.time.LocalDateTime;
import entities.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Stateless
//@RolesAllowed({"ADMIN"})
public class AdminEJB implements AdminEJBLocal {

    @PersistenceContext(unitName = "TattooPU")
    private EntityManager em;

    private static final BigDecimal ARTIST_COMMISSION_RATE = new BigDecimal("0.60"); // 60% Artist share
    private static final BigDecimal ADMIN_CUT_RATE = new BigDecimal("0.40");       // 40% Studio cut
    
    // -----------------------
    // User Management
    // -----------------------
    @Override
    public List<AppUser> listAllUsers() {
        return em.createQuery("SELECT u FROM AppUser u ORDER BY u.createdAt DESC", AppUser.class)
                 .getResultList();
    }

    private AppUser findUser(Long userId) {
    if (userId == null) {
         throw new IllegalArgumentException("User ID must not be null.");
    }
    AppUser u = em.find(AppUser.class, userId);
    if (u == null) {
        throw new IllegalArgumentException("User not found: " + userId);
    }
    return u;
}
    
    @Override
public AppUser getUserById(Long userId) {
    // This is now simplified to use the helper
    return findUser(userId);
}

    @Override
    @Transactional
    public void deactivateUser(Long userId, boolean deactivate, String reason) {
//        AppUser u = em.find(AppUser.class, userId);
//        if (u == null) throw new IllegalArgumentException("User not found: " + userId);
//        u.setActive(!deactivate ? true : false);
//        em.merge(u);
        // optionally log deactivation
        AppUser u = findUser(userId);
        // Set isActive to true if 'deactivate' is false, and vice versa.
        u.setIsActive(!deactivate);
        u.setDeactivationReason(deactivate ? reason : null);

        em.merge(u);
    }

  @Override
@Transactional
public void verifyArtist(Long artistId, boolean verify, Long verifyingAdminId) {
    AppUser artist = findUser(artistId); // Uses helper
    
    findUser(verifyingAdminId); // Validate that the admin performing the action exists
    
    artist.setIsVerified(verify);
    
    em.merge(artist);
}
    // Ensure user is ARTIST
//    GroupMaster role = user.getRole();
//    if (role == null || role.getRoleName() == null 
//        || !role.getRoleName().equalsIgnoreCase("ARTIST")) {
//        throw new IllegalArgumentException("User is not an artist: " + artistId);
//    }
//
//    // perform verification
//    user.setVerified(verify);
//    em.merge(user);
//
//    // optional logging
//    if (verifyingAdminId != null) {
//        AppUser admin = em.find(AppUser.class, verifyingAdminId);
//        if (admin == null)
//            throw new IllegalArgumentException("Admin user not found: " + verifyingAdminId);
//    }
//}


    // -----------------------
    // Announcements
    // -----------------------
    @Override
    @Transactional
    public Announcement createAnnouncement(Long postedByAdminId, String title, String message, String targetRole) {
        AppUser admin = em.find(AppUser.class, postedByAdminId);
        if (admin == null) throw new IllegalArgumentException("Admin not found: " + postedByAdminId);
        Announcement a = new Announcement();
        a.setTitle(title);
        a.setMessage(message);
        a.setPostedAt(LocalDateTime.now());
        a.setTargetRole(targetRole);
        a.setPostedBy(admin);
        em.persist(a);
        return a;
    }

    @Override
    @Transactional
    public Announcement updateAnnouncement(Long announcementId, String title, String message, String targetRole) {
        Announcement a = em.find(Announcement.class, announcementId);
        if (a == null) throw new IllegalArgumentException("Announcement not found: " + announcementId);
        a.setTitle(title);
        a.setMessage(message);
        a.setTargetRole(targetRole);
        a.setPostedAt(LocalDateTime.now());
        return em.merge(a);
    }

    @Override
    @Transactional
    public void deleteAnnouncement(Long announcementId) {
        Announcement a = em.find(Announcement.class, announcementId);
        if (a == null) throw new IllegalArgumentException("Announcement not found: " + announcementId);
        em.remove(a);
    }

    // -----------------------
    // Tattoo Designs
    // -----------------------
    @Override
    public List<TattooDesign> listDesigns(int offset, int limit) {
        TypedQuery<TattooDesign> q = em.createQuery("SELECT d FROM TattooDesign d ORDER BY d.uploadedAt DESC", TattooDesign.class);
        if (offset >= 0) q.setFirstResult(offset);
        if (limit > 0) q.setMaxResults(limit);
        return q.getResultList();
    }

    @Override
    @Transactional
    public void deleteDesign(Long designId) {
        TattooDesign d = em.find(TattooDesign.class, designId);
        if (d == null) throw new IllegalArgumentException("Design not found: " + designId);
        em.remove(d);
    }

    // -----------------------
    // Appointments
    // -----------------------
    @Override
    public List<Appointment> listAllAppointments(int offset, int limit) {
        TypedQuery<Appointment> q = em.createQuery("SELECT a FROM Appointment a ORDER BY a.appointmentDateTime DESC", Appointment.class);
        if (offset >= 0) q.setFirstResult(offset);
        if (limit > 0) q.setMaxResults(limit);
        return q.getResultList();
    }

    @Override
    public Appointment getAppointment(Long appointmentId) {
        Appointment a = em.find(Appointment.class, appointmentId);
        if (a == null) throw new IllegalArgumentException("Appointment not found: " + appointmentId);
        return a;
    }

    @Override
    @Transactional
    public void changeAppointmentStatus(Long appointmentId, String status, String cancellationReason) {
        Appointment a = em.find(Appointment.class, appointmentId);
        if (a == null) throw new IllegalArgumentException("Appointment not found: " + appointmentId);
        a.setStatus(status);
        if ("CANCELLED".equalsIgnoreCase(status) && cancellationReason != null) {
            a.setCancellationReason(cancellationReason);
        } else {
            a.setCancellationReason(null);
        }
        em.merge(a);
    }

    @Override
    @Transactional
    public void assignSlotToAppointment(Long appointmentId, Integer slotId) {
        Appointment a = em.find(Appointment.class, appointmentId);
        if (a == null) throw new IllegalArgumentException("Appointment not found: " + appointmentId);
        TimeSlot slot = em.find(TimeSlot.class, slotId);
        if (slot == null) throw new IllegalArgumentException("Slot not found: " + slotId);
        if (slot.getStatus() == TimeSlot.TimeSlotStatus.BOOKED)
            throw new IllegalStateException("Slot is already booked: " + slotId);

        a.setSlot(slot);
        a.setAppointmentDateTime(slot.getStartTime());
        slot.setStatus(TimeSlot.TimeSlotStatus.BOOKED);
        em.merge(slot);
        em.merge(a);
    }

    // -----------------------
    // Time Slots
    // -----------------------
    @Override
    @Transactional
    public void blockTimeSlot(Integer slotId, Long adminId, String reason) {
        TimeSlot slot = em.find(TimeSlot.class, slotId);
        if (slot == null) throw new IllegalArgumentException("Slot not found: " + slotId);
        AppUser admin = em.find(AppUser.class, adminId);
        if (admin == null) throw new IllegalArgumentException("Admin not found: " + adminId); // **ID Check Added**

        slot.setStatus(TimeSlot.TimeSlotStatus.BLOCKED);
        slot.setBlockReason(reason);
        slot.setBlockedBy(admin);
        em.merge(slot);
    }

    @Override
    @Transactional
    public void unblockTimeSlot(Integer slotId, Long adminId) {
        TimeSlot slot = em.find(TimeSlot.class, slotId);
        if (slot == null) throw new IllegalArgumentException("Slot not found: " + slotId);
        AppUser admin = em.find(AppUser.class, adminId);
        if (admin == null) throw new IllegalArgumentException("Admin not found: " + adminId); // **ID Check Added**
        
        slot.setStatus(TimeSlot.TimeSlotStatus.AVAILABLE);
        slot.setBlockReason(null);
        slot.setBlockedBy(null);
        em.merge(slot);
    }

    // -----------------------
    // Payments & Earnings
    // -----------------------
    @Override
@jakarta.transaction.Transactional
public EarningLog logEarningsForPayment(Integer paymentId) {
    Payment payment = em.find(Payment.class, paymentId);
    if (payment == null) {
        throw new IllegalArgumentException("Payment not found: " + paymentId);
    }
    
    Appointment appointment = payment.getAppointment();
    if (appointment == null) {
        throw new IllegalStateException("Payment ID " + paymentId + " is not linked to an appointment.");
    }
    
    AppUser artist = appointment.getArtist();
    // CRITICAL DATA INTEGRITY CHECK: Prevents NullPointerException
    if (artist == null) {
        throw new IllegalStateException("Appointment ID " + appointment.getAppointmentId() + " is missing an assigned Artist. Cannot log earnings.");
    }
    
    // CRITICAL FIX: Ensure the Admin user (ID 1) exists for the audit trail
    AppUser studioAdmin = findUser(1L); // Assumes AppUser ID 1 is the Studio/Admin account
    if (studioAdmin == null) {
        throw new IllegalStateException("Studio/Admin account (ID 1) not found for financial logging. Cannot complete audit.");
    }

    // --- FINANCIAL CALCULATIONS (60/40 Split) ---
    BigDecimal totalAmount = payment.getAmount();
    BigDecimal artistShare = totalAmount.multiply(ARTIST_COMMISSION_RATE).setScale(2, BigDecimal.ROUND_HALF_UP);
    BigDecimal adminCut = totalAmount.subtract(artistShare); // Calculate the admin share as the remainder to handle rounding precisely
    
    // --- Create a SINGLE EarningLog record (as per your entity structure) ---
    EarningLog log = new EarningLog();
    
    // Set relationships
    log.setPayment(payment);
    log.setAppointment(appointment);
    log.setArtist(artist); 
    log.setAdmin(studioAdmin); // Logged by the system's Admin user
    
    // Set financial data (using the correct field names from EarningLog.java)
    log.setTotalAmount(totalAmount); 
    log.setArtistShare(artistShare); 
    log.setAdminShare(adminCut); 
    log.setPremiumBonus(BigDecimal.ZERO); // Default or apply logic if needed
    
    // Set status and audit date
    log.setPayoutStatus("PENDING"); // Artist's share is pending payout
    log.setCalculatedAt(LocalDateTime.now()); // Date of log creation
    // log.setPayoutAt(null); // Payout date is null until the payout occurs

    em.persist(log);
    
    return log;
}
    
    
    @Override
public List<Payment> listPayments(int offset, int limit) {
    TypedQuery<Payment> q = em.createQuery("SELECT p FROM Payment p ORDER BY p.paymentDate DESC", Payment.class);
    if (offset >= 0) q.setFirstResult(offset);
    if (limit > 0) q.setMaxResults(limit);
    
    // Eagerly initialize related entities for REST serialization
    List<Payment> payments = q.getResultList();
    payments.forEach(p -> {
        if (p.getAppointment() != null) {
            p.getAppointment().getAppointmentId(); 
            if(p.getAppointment().getArtist() != null) p.getAppointment().getArtist().getUserId();
        }
    });
    return payments;
}

    @Override
@jakarta.transaction.Transactional
public void markPaymentStatus(Integer paymentId, String status, Long adminId) {
    Payment payment = em.find(Payment.class, paymentId);
    if (payment == null) {
        throw new IllegalArgumentException("Payment not found: " + paymentId);
    }
    
    String upperStatus = status.toUpperCase().trim();
    String currentStatus = payment.getStatus().toUpperCase().trim();
    
    // Check for previous completion to prevent double logging of earnings
    if ("COMPLETED".equals(upperStatus) && !currentStatus.equals("COMPLETED")) {
        
        // 1. Log Earnings (This is the critical step that was failing)
        // If this call fails (e.g., missing Artist, missing Admin ID 1), it throws an exception
        // that rolls back the entire transaction, including the payment status update in ClientEJB.
        logEarningsForPayment(paymentId); 
        
        // 2. Update Appointment Status to PAID
        Appointment a = payment.getAppointment();
        if (a != null && "CONFIRMED".equals(a.getStatus())) {
            a.setStatus("PAID");
            em.merge(a);
        }
        
        // 3. Update Payment Status in Admin context (Ensures the payment is officially marked)
        payment.setStatus(upperStatus);
        em.merge(payment); 
    } 
}

    @Override
public BigDecimal calculateArtistPendingEarnings(Long artistId, LocalDate fromDate, LocalDate toDate) {
    // Validate artist exists 
    findUser(artistId); 

    // Sum all 'artistShare' from EarningLog entries that are not yet paid out
    String ql = "SELECT SUM(e.artistShare) FROM EarningLog e " +
                "WHERE e.artist.userId = :artistId " +
                "AND e.payoutStatus = 'PENDING' " + 
                "AND e.calculatedAt >= :start AND e.calculatedAt < :end";

    TypedQuery<BigDecimal> q = em.createQuery(ql, BigDecimal.class)
            .setParameter("artistId", artistId)
            .setParameter("start", fromDate.atStartOfDay())
            .setParameter("end", toDate.plusDays(1).atStartOfDay()); 

    BigDecimal earnings = q.getSingleResult();
    
    return earnings == null ? BigDecimal.ZERO : earnings.setScale(2, BigDecimal.ROUND_HALF_UP);
}


    @Override
@Transactional
public Long simulatePayout(Long artistId, LocalDate forMonth, BigDecimal amount, Long adminId) {
    AppUser artist = findUser(artistId);
    AppUser admin = findUser(adminId);
    
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException("Payout amount must be positive.");
    }
    
    // Define the month range for which the payout is being made
    LocalDate startOfMonth = forMonth.withDayOfMonth(1);
    LocalDate endOfMonth = forMonth.plusMonths(1).withDayOfMonth(1).minusDays(1);

    // 1. Create the Payout record
    ArtistPayout payout = new ArtistPayout();
    payout.setArtist(artist);
    payout.setAmount(amount.setScale(2, BigDecimal.ROUND_HALF_UP));
    payout.setAdmin(admin);
    payout.setPayoutStatus("PAID");
    payout.setPayoutDate(LocalDateTime.now());
    payout.setNotes("Simulated payout for " + startOfMonth.getMonth().name() + " " + startOfMonth.getYear() + " (Admin action)");
    payout.setCreatedAt(LocalDateTime.now()); 
    em.persist(payout);
    
    // 2. Find and update relevant PENDING EarningLogs to link them to this Payout
    List<EarningLog> logsToPayout = em.createQuery(
            "SELECT e FROM EarningLog e WHERE e.artist.userId = :artistId " +
            "AND e.payoutStatus = 'PENDING' " +
            "AND e.calculatedAt >= :start AND e.calculatedAt < :end " +
            "ORDER BY e.calculatedAt ASC", EarningLog.class)
            .setParameter("artistId", artistId)
            .setParameter("start", startOfMonth.atStartOfDay())
            .setParameter("end", endOfMonth.plusDays(1).atStartOfDay())
            .getResultList();
    
    for (EarningLog log : logsToPayout) {
        log.setPayoutStatus("PAID");
        log.setPayoutAt(payout.getPayoutDate());
        log.setPayout(payout);
        em.merge(log);
    }
    
    return payout.getPayoutId();
}

    @Override
public List<ArtistPayout> listArtistPayouts(Long artistId, int offset, int limit) {
    findUser(artistId);
    
    TypedQuery<ArtistPayout> q = em.createQuery(
        "SELECT ap FROM ArtistPayout ap WHERE ap.artist.userId = :artistId ORDER BY ap.payoutDate DESC",
        ArtistPayout.class
    ).setParameter("artistId", artistId);

    if (offset >= 0) q.setFirstResult(offset);
    if (limit > 0) q.setMaxResults(limit);

    return q.getResultList();
}

    // -----------------------
    // Feedback & Reports
    // -----------------------
    @Override
    public List<Feedback> listFeedbacks(int offset, int limit) {
        TypedQuery<Feedback> q = em.createQuery("SELECT f FROM Feedback f ORDER BY f.createdAt DESC", Feedback.class);
        if (offset >= 0) q.setFirstResult(offset);
        if (limit > 0) q.setMaxResults(limit);
        return q.getResultList();
    }

// Inside AdminEJB.java

@Override
public Map<String, Object> generateSimpleReports(LocalDate fromDate, LocalDate toDate) {
    Map<String, Object> report = new HashMap<>();

    // 1. Convert LocalDate range to LocalDateTime (Start of 'from' day to End of 'to' day)
    LocalDateTime startDateTime = fromDate.atStartOfDay();
    LocalDateTime endDateTime = toDate.plusDays(1).atStartOfDay(); 
    
    // --- 1. Calculate Total Bookings (Count of Appointments) ---
    // FIX: Using the nested path 'a.timeSlot.startTime'
    Long totalBookings = em.createQuery(
            "SELECT COUNT(a) FROM Appointment a WHERE a.timeSlot.startTime >= :start AND a.timeSlot.startTime < :end AND a.status IN ('PAID', 'COMPLETED')",
            Long.class)
            .setParameter("start", startDateTime)
            .setParameter("end", endDateTime)
            .getSingleResult();
    
    report.put("totalBookings", totalBookings);

    // --- 2. Calculate Total Revenue (Sum of COMPLETED Payments) ---
    // FIX: Using the nested path 'a.timeSlot.startTime'
    BigDecimal totalRevenue = em.createQuery(
            "SELECT SUM(p.amount) FROM Payment p JOIN p.appointment a " +
            "WHERE p.status = 'COMPLETED' AND a.timeSlot.startTime >= :start AND a.timeSlot.startTime < :end",
            BigDecimal.class)
            .setParameter("start", startDateTime)
            .setParameter("end", endDateTime)
            .getSingleResult();
            
    report.put("totalRevenue", totalRevenue == null ? BigDecimal.ZERO : totalRevenue);

    // --- 3. Identify Top 3 Artists by Revenue/Bookings ---
    // FIX: Using the nested path 'a.timeSlot.startTime'
    List<Object[]> topArtistsRaw = em.createQuery(
            "SELECT a.artist.userId, a.artist.fullName, COUNT(a) AS bookingCount " +
            "FROM Appointment a " +
            "WHERE a.status = 'COMPLETED' AND a.timeSlot.startTime >= :start AND a.timeSlot.startTime < :end " +
            "GROUP BY a.artist.userId, a.artist.fullName " +
            "ORDER BY bookingCount DESC",
            Object[].class)
            .setParameter("start", startDateTime)
            .setParameter("end", endDateTime)
            .setMaxResults(3) // Limit to top 3
            .getResultList();

    // The stream logic for type safety remains correct.
    List<Map<String, Object>> topArtists = topArtistsRaw.stream().map(
        arr -> {
            Map<String, Object> artistMap = new HashMap<>();
            artistMap.put("artistId", (Long) arr[0]);
            artistMap.put("fullName", (String) arr[1]);
            artistMap.put("completedBookings", (Long) arr[2]);
            return artistMap;
        }
    ).toList();
    
    report.put("topArtists", topArtists);

    return report;
}

    // -----------------------
    // Medical Forms
    // -----------------------
   @Override
    public List<MedicalForm> listMedicalForms(int offset, int limit) {
        // MedicalForm has 'submittedAt' field (not createdAt). Order by submittedAt.
        TypedQuery<MedicalForm> q = em.createQuery(
            "SELECT m FROM MedicalForm m ORDER BY m.submittedAt DESC",
            MedicalForm.class
        );

        if (offset >= 0) q.setFirstResult(offset);
        if (limit > 0) q.setMaxResults(limit);

        return q.getResultList();
    }



// --- AdminEJB.java: Replace approveMedicalForm (Uses new entity setters) ---
    @Override
    @Transactional
    public void approveMedicalForm(Integer formId, Long adminId) {

        MedicalForm form = em.find(MedicalForm.class, formId);
        if (form == null)
            throw new IllegalArgumentException("Medical form not found: " + formId);

        // Retrieve the admin user who performed the action
        AppUser admin = findUser(adminId); 

        form.setIsApproved(true); 

        // New setters for the required audit trail
        form.setApprovedBy(admin); 
        form.setApprovedAt(LocalDateTime.now());

        em.merge(form);
    }
}
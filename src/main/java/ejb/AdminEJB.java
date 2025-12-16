package ejb;
import dto.AdminProfileDTO;
import dto.AppointmentDTO;
import dto.AppointmentFilterDTO;
import dto.ArtistPendingEarningDTO;
import dto.MedicalFormDTO;
import dto.MedicalFormFilterDTO;
import dto.TimeSlotDTO;
import dto.TimeSlotFilterDTO;
import jakarta.persistence.NoResultException;
import entities.*;
import jakarta.ejb.Stateless;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import util.PasswordUtil;
import jakarta.persistence.EntityNotFoundException;
import java.math.RoundingMode;
import java.util.stream.Collectors;

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
        return em.createQuery(
                "SELECT u FROM AppUser u WHERE u.role.roleName <> 'ADMIN' ORDER BY u.userId ASC",
                AppUser.class
        ).getResultList();
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

    AppUser artist = findUser(artistId);

    // If adminId is provided ensure admin exists
    if (verifyingAdminId != null) {
        findUser(verifyingAdminId);
    }

    // Set verified/unverified
    artist.setIsVerified(verify);

    // Auto-sync active status
    if (verify) {
        artist.setIsActive(true);     // verified → active
        artist.setDeactivationReason(null);
    } else {
        artist.setIsActive(false);    // unverified → inactive
        artist.setDeactivationReason("Artist unverified by admin");
    }

    em.merge(artist);
}

public Long getUserIdByUsername(String username) {
    try {
        TypedQuery<Long> query = em.createQuery(
            "SELECT u.userId FROM AppUser u WHERE u.username = :username", 
            Long.class
        );
        query.setParameter("username", username);
        return query.getSingleResult();
    } catch (NoResultException e) {
        return null;
    }
}


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
    em.flush(); // ensure id assigned

    // initialize postedBy
    a.getPostedBy().getUserId();
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
    // DO NOT overwrite original postedAt on edit — keep original postedAt
    // If you want to record "lastUpdated", add a new field to Announcement entity (recommended).
    Announcement merged = em.merge(a);

    // Initialize postedBy before returning (in case calling code inspects postedBy)
    if (merged.getPostedBy() != null) merged.getPostedBy().getUserId();

    return merged;
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
        TypedQuery<TattooDesign> q = em.createQuery(
            "SELECT d FROM TattooDesign d ORDER BY d.uploadedAt DESC",
            TattooDesign.class
        );
        if (offset >= 0) q.setFirstResult(offset);
        if (limit > 0) q.setMaxResults(limit);
        return q.getResultList();
    }


    @Override
    @Transactional
    public void banDesign(Long designId, String reason, Long adminId) {
        if (designId == null) throw new IllegalArgumentException("Design ID must not be null.");
        TattooDesign d = em.find(TattooDesign.class, designId);
        if (d == null) throw new IllegalArgumentException("Design not found: " + designId);
        AppUser admin = findUser(adminId); // will throw if adminId invalid

        // Mark design as banned
        d.setIsBanned(true);
        d.setBannedReason(reason != null ? reason : "No reason provided");
        d.setBannedAt(LocalDateTime.now());

        em.merge(d);

        // Create an admin announcement for admins (audit)
        Announcement aAdmin = new Announcement();
        aAdmin.setTitle("Design banned: " + d.getTitle());
        aAdmin.setMessage("Admin " + admin.getFullName() + " banned design '" + d.getTitle() + "' (ID " + designId + "). Reason: " + d.getBannedReason());
        aAdmin.setPostedAt(LocalDateTime.now());
        aAdmin.setPostedBy(admin);
        aAdmin.setTargetRole("ADMIN");
        em.persist(aAdmin);

        // Notify the artist (broad announcement targeted at ARTIST role).
        Announcement aArtist = new Announcement();
        aArtist.setTitle("Your design has been banned: " + d.getTitle());
        aArtist.setMessage("Your design '" + d.getTitle() + "' (ID " + designId + ") was banned by admin. Reason: " + d.getBannedReason() + ". Please review and, if required, remove it from your portfolio.");
        aArtist.setPostedAt(LocalDateTime.now());
        aArtist.setPostedBy(admin);
        aArtist.setTargetRole("ARTIST");
        em.persist(aArtist);

        List<Long> affectedClients = em.createQuery(
                "SELECT DISTINCT ap.client.userId FROM Appointment ap WHERE ap.design.designId = :did AND ap.appointmentDateTime >= :now",
                Long.class)
            .setParameter("did", designId)
            .setParameter("now", LocalDateTime.now())
            .getResultList();

        for (Long clientId : affectedClients) {
            // We reuse Announcement but set targetRole = "CLIENT" — your UI can show only announcements for the logged-in client
            Announcement cAnn = new Announcement();
            cAnn.setTitle("Design used in your booking was banned");
            cAnn.setMessage("A design you selected for an upcoming appointment has been banned by admin. Appointment(s) that referenced design ID " + designId + " may require you to choose a new design. Please contact the studio or choose another design.");
            cAnn.setPostedAt(LocalDateTime.now());
            cAnn.setPostedBy(admin);
            cAnn.setTargetRole("CLIENT");
            em.persist(cAnn);
        }
    }


    @Override
    @Transactional
    public void unbanDesign(Long designId, Long adminId) {
        TattooDesign d = em.find(TattooDesign.class, designId);
        if (d == null) throw new IllegalArgumentException("Design not found: " + designId);

        AppUser admin = findUser(adminId);

        d.setIsBanned(false);
        d.setBannedReason(null);
        d.setBannedAt(null);

        // 🔥 Restore if artist removed it earlier
        d.setIsRemovedByArtist(false);
        d.setRemovedAt(null);

        em.merge(d);

        // Announcement for artist + admin
        Announcement aArtist = new Announcement();
        aArtist.setTitle("Design unbanned: " + d.getTitle());
        aArtist.setMessage("Design '" + d.getTitle() + "' (ID " + designId + ") has been unbanned by admin " + admin.getFullName() + ".");
        aArtist.setPostedAt(LocalDateTime.now());
        aArtist.setPostedBy(admin);
        aArtist.setTargetRole("ARTIST");
        em.persist(aArtist);

        Announcement aAdmin = new Announcement();
        aAdmin.setTitle("Design unbanned: " + d.getTitle());
        aAdmin.setMessage("Admin " + admin.getFullName() + " unbanned design '" + d.getTitle() + "' (ID " + designId + ").");
        aAdmin.setPostedAt(LocalDateTime.now());
        aAdmin.setPostedBy(admin);
        aAdmin.setTargetRole("ADMIN");
        em.persist(aAdmin);
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

//    @Override
//    public void changeAppointmentStatus(Long appointmentId, String status, String cancellationReason) {
//        Appointment appt = em.find(Appointment.class, appointmentId);
//        if (appt == null) {
//            throw new IllegalArgumentException("Appointment not found with ID: " + appointmentId);
//        }
//
//        // Set the new status
//        appt.setStatus(status);
//
//        // Handle cancellation reason if the status is CANCELLED
//        if ("CANCELLED".equals(status)) {
//            appt.setCancellationReason(cancellationReason);
//            // OPTIONAL: If the appointment was booked to a slot, free the slot here
//            if (appt.getSlot() != null) {
//                 appt.getSlot().setStatus(TimeSlot.TimeSlotStatus.AVAILABLE);
//                 appt.setSlot(null); // Unlink the slot
//            }
//    }
//    
//    em.merge(appt);
//}
//
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
        log.setPayoutStatus("UNPAID"); // Artist's share is pending payout
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

    String newStatus = status == null ? "" : status.toUpperCase().trim();
    String currentStatus = payment.getStatus() == null ? "" : payment.getStatus().toUpperCase().trim();

    if ("COMPLETED".equals(newStatus) && !currentStatus.equals("COMPLETED")) {
        // create earning log (idempotent)
        createEarningLogForPayment(paymentId, adminId);

        // update appointment to PAID when appropriate
        Appointment appt = payment.getAppointment();
        if (appt != null && "CONFIRMED".equalsIgnoreCase(appt.getStatus())) {
            appt.setStatus("PAID");
            em.merge(appt);
        }

        payment.setStatus("COMPLETED");
        em.merge(payment);
    } else {
        // For other status transitions, just update status
        payment.setStatus(newStatus);
        em.merge(payment);
    }
}


    @Override
    public BigDecimal calculateArtistPendingEarnings(Long artistId, LocalDate fromDate, LocalDate toDate) {
        // Validate artist exists 
        findUser(artistId); 

        String ql = "SELECT SUM(e.artistShare) FROM EarningLog e " +
                    "WHERE e.artist.userId = :artistId " +
                    "AND e.payoutStatus = 'UNPAID' " + 
                    "AND e.calculatedAt >= :start AND e.calculatedAt < :end";

        TypedQuery<BigDecimal> q = em.createQuery(ql, BigDecimal.class)
                .setParameter("artistId", artistId)
                .setParameter("start", fromDate.atStartOfDay())
                .setParameter("end", toDate.plusDays(1).atStartOfDay()); 

        BigDecimal earnings = q.getSingleResult();

        return earnings == null ? BigDecimal.ZERO : earnings.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    @Override
@jakarta.transaction.Transactional
public Long simulatePayout(Long artistId, LocalDate forMonth, BigDecimal amount, Long adminId) {
    AppUser artist = findUser(artistId);
    AppUser admin = findUser(adminId);

    // create payout record as SIMULATED or PAID depending on your choice
    ArtistPayout payout = new ArtistPayout();
    payout.setArtist(artist);
    payout.setAdmin(admin);
    payout.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
    payout.setNotes("Simulated payout for " + forMonth.getMonth().name() + " " + forMonth.getYear());
    payout.setPayoutStatus("SIMULATED");
    payout.setPayoutDate(LocalDateTime.now());
    payout.setCreatedAt(LocalDateTime.now());
    em.persist(payout);
    em.flush();

    // optionally link and mark logs for that month as PAID if you want simulation to behave like real payout:
    // List<EarningLog> logsToPayout = ...
    // for each: set payout/payoutStatus/payoutAt and em.merge(log)

    return payout.getPayoutId();
}


  @Override
public List<ArtistPayout> listArtistPayouts(Long artistId, int offset, int limit) {
    return em.createQuery(
        "SELECT p FROM ArtistPayout p WHERE p.artist.userId = :id ORDER BY p.payoutDate DESC",
        ArtistPayout.class
    )
    .setParameter("id", artistId)
    .setFirstResult(offset)
    .setMaxResults(limit)
    .getResultList();
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
    public void approveMedicalForm(Integer formId, Long adminId) {
    try {
        MedicalForm form = em.find(MedicalForm.class, formId);
        if (form != null) {
            // Find the admin user entity
            AppUser adminUser = em.find(AppUser.class, adminId);
            if (adminUser == null) {
                throw new RuntimeException("Admin user not found with ID: " + adminId);
            }
            
            form.setIsApproved(true);
            form.setApprovedAt(LocalDateTime.now());
            form.setApprovedBy(adminUser); // Use setApprovedBy instead of setApprovedByAdminId
            em.merge(form);
            
            // Flush to ensure changes are persisted
            em.flush();
        } else {
            throw new RuntimeException("Medical form not found with ID: " + formId);
        }
    } catch (Exception e) {
        throw new RuntimeException("Error approving medical form: " + e.getMessage(), e);
    }
}
    
    //additionally added things
    @Override
public long countTotalUsers() {
    return em.createQuery("SELECT COUNT(u) FROM AppUser u", Long.class)
             .getSingleResult();
}

@Override
public long countTotalArtists() {
    return em.createQuery("SELECT COUNT(u) FROM AppUser u WHERE u.role.roleName = 'ARTIST'", Long.class)
             .getSingleResult();
}

@Override
public long countTotalClients() {
    return em.createQuery("SELECT COUNT(u) FROM AppUser u WHERE u.role.roleName = 'CLIENT'", Long.class)
             .getSingleResult();
}

@Override
public long countTotalBookings() {
    return em.createQuery("SELECT COUNT(a) FROM Appointment a", Long.class)
             .getSingleResult();
}

@Override
public long countTodaysAppointments() {
    LocalDate today = LocalDate.now();
    LocalDateTime start = today.atStartOfDay();
    LocalDateTime end = today.plusDays(1).atStartOfDay();

    return em.createQuery(
            "SELECT COUNT(a) FROM Appointment a WHERE a.appointmentDateTime >= :start AND a.appointmentDateTime < :end",
            Long.class)
            .setParameter("start", start)
            .setParameter("end", end)
            .getSingleResult();
}

@Override
public List<Appointment> listRecentAppointments(int limit) {
    return em.createQuery(
            "SELECT a FROM Appointment a ORDER BY a.appointmentDateTime DESC",
            Appointment.class)
            .setMaxResults(limit)
            .getResultList();
}


@Override
public double calculateTotalEarnings() {
    try {
        // Check if your Payment entity has 'amount' or 'paymentAmount' field
        // Change 'paymentAmount' to 'amount' if needed
        Double result = em.createQuery(
                "SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED'",  // Changed to 'amount'
                Double.class)
                .getSingleResult();
        return result != null ? result : 0.0;
    } catch (NoResultException e) {
        return 0.0;
    } catch (Exception e) {
        e.printStackTrace();  // Added stack trace
        return 0.0;
    }
}

// AdminEJB.java
@Override
public AdminProfileDTO getAdminProfile(Long adminId) {
    AppUser admin = em.find(AppUser.class, adminId);
    if (admin == null) return null;

    AdminProfileDTO dto = new AdminProfileDTO();
    dto.setUserId(admin.getUserId());
    dto.setUsername(admin.getUsername());
    dto.setFullName(admin.getFullName());
    dto.setEmail(admin.getEmail());
    dto.setPhone(admin.getPhone());
    if (admin.getRole() != null) dto.setRole(admin.getRole().getRoleName());
    return dto;
}

@Override
public boolean updateAdminDetails(AdminProfileDTO dto) {
    try {
        AppUser admin = em.find(AppUser.class, dto.getUserId());
        if (admin == null) return false;

        admin.setUsername(dto.getUsername());   // add this line
        admin.setFullName(dto.getFullName());
        admin.setEmail(dto.getEmail());
        admin.setPhone(dto.getPhone());
        // role remains unchanged
        em.merge(admin);
        return true;
    } catch (Exception e) {
        // log e
        return false;
    }
}

@Override
public boolean changeAdminPassword(Long adminId, String oldPassword, String newPassword) {
    try {
        AppUser admin = em.find(AppUser.class, adminId);
        if (admin == null) return false;

        // ✅ Verify old password using PasswordUtil
        if (!PasswordUtil.verifyPassword(oldPassword, admin.getPassword())) {
            return false; // old password doesn’t match
        }

        // ✅ Hash new password before saving
        String hashedNewPassword = PasswordUtil.hashPassword(newPassword);
        admin.setPassword(hashedNewPassword);

        em.merge(admin);
        return true;
    } catch (Exception e) {
        // log error
        return false;
    }
}


@Override
@Transactional
public void deleteUser(Long userId) {
    AppUser u = em.find(AppUser.class, userId);
    if (u == null) {
        throw new IllegalArgumentException("User not found: " + userId);
    }
    em.remove(u);
}

// AdminEJB.java

@Override
public List<AppUser> listArtists() {
    return em.createQuery(
        "SELECT u FROM AppUser u WHERE u.role.roleName = 'ARTIST' ORDER BY u.userId ASC",
        AppUser.class
    ).getResultList();
}



@Override
public List<TimeSlot> listAvailableSlots(LocalDate date) {
    // 1. Define the time range
    LocalDateTime start = date.atStartOfDay();
    LocalDateTime end = date.plusDays(1).atStartOfDay();
    
    // Assuming TimeSlots are marked "AVAILABLE"
    final String AVAILABLE_STATUS = "AVAILABLE"; 

    TypedQuery<TimeSlot> query = em.createQuery(
        "SELECT ts FROM TimeSlot ts WHERE ts.startTime >= :start AND ts.startTime < :end AND ts.status = :status ORDER BY ts.startTime ASC",
        TimeSlot.class);

    // 2. CRITICAL FIX: Bind ALL parameters
    query.setParameter("start", start);
    query.setParameter("end", end);
    query.setParameter("status", AVAILABLE_STATUS); // This parameter was likely missing!

    return query.getResultList();
}

// In AdminEJB.java - ensure this method exists
@Override
public Map<String, Long> getDashboardStats() {
    Map<String, Long> stats = new HashMap<>();
    
    // Count appointments by status
    String[] statuses = {"PENDING", "CONFIRMED", "COMPLETED", "CANCELLED", "PAID"};
    
    for (String status : statuses) {
        try {
            Long count = em.createQuery(
                "SELECT COUNT(a) FROM Appointment a WHERE a.status = :status", Long.class)
                .setParameter("status", status)
                .getSingleResult();
            stats.put(status, count != null ? count : 0L);
        } catch (Exception e) {
            stats.put(status, 0L);
        }
    }
    
    return stats;
}

    
    @Override
public List<TimeSlot> getAvailableSlots(Long artistId) {
    return em.createQuery(
            "SELECT t FROM TimeSlot t " +
            "WHERE t.artist.userId = :artistId " +
            "AND t.status = :status " +
            "ORDER BY t.startTime ASC",
            TimeSlot.class)
            .setParameter("artistId", artistId)
            .setParameter("status", TimeSlot.TimeSlotStatus.AVAILABLE)
            .getResultList();
}

// ✅ KEEP THIS BLOCK (It is the correct implementation)
@Override
@Transactional
public void assignSlotToAppointment(Long appointmentId, Integer slotId) {
    if (appointmentId == null || slotId == null) {
        throw new IllegalArgumentException("Appointment ID and Slot ID must be provided.");
    }

    Appointment appt = em.find(Appointment.class, appointmentId);
    TimeSlot slot = em.find(TimeSlot.class, slotId);

    if (appt == null) {
        throw new EntityNotFoundException("Appointment not found with ID: " + appointmentId);
    }
    if (slot == null) {
        throw new EntityNotFoundException("TimeSlot not found with ID: " + slotId);
    }
    
    if (slot.getStatus() == TimeSlot.TimeSlotStatus.BLOCKED) {
        throw new IllegalStateException("Cannot assign appointment to a BLOCKED time slot.");
    }
    
    // Business Logic Check
    if (!"PENDING".equals(appt.getStatus())) {
         throw new IllegalStateException("Cannot assign slot to an appointment that is already " + appt.getStatus() + ".");
    }
    if (slot.getStatus() != TimeSlot.TimeSlotStatus.AVAILABLE) {
         throw new IllegalStateException("Slot is not available: " + slot.getStatus());
    }

    // Set slot status to BOOKED
    slot.setStatus(TimeSlot.TimeSlotStatus.BOOKED); 

    // Link Appointment to Slot
    appt.setSlot(slot);

    // Set appointment datetime from the slot
    appt.setAppointmentDateTime(slot.getStartTime());

    // Update appointment status
    appt.setStatus("CONFIRMED");

    // Persist both in the transaction
    em.merge(slot);
    em.merge(appt);
}


@Override
public List<TimeSlot> listAvailableSlotsByArtistAndDate(Long artistId, LocalDate date) {
    if (artistId == null || date == null) {
        return List.of();
    }
    
    // The query finds available slots for a specific artist on a specific day.
    // Use the artist's ID and check if the slot's start time is within the day.
    LocalDateTime startOfDay = date.atStartOfDay();
    LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

    // Assuming TimeSlot has an artist (AppUser) relationship
    return em.createQuery(
            "SELECT t FROM TimeSlot t WHERE t.artist.userId = :artistId " +
            "AND t.startTime >= :startOfDay AND t.startTime < :endOfDay " +
            "AND t.status = 'AVAILABLE' " + // Check status as a String for safety
            "ORDER BY t.startTime ASC",
            TimeSlot.class)
            .setParameter("artistId", artistId)
            .setParameter("startOfDay", startOfDay)
            .setParameter("endOfDay", endOfDay)
            .getResultList();
}

    // Add these methods to AdminEJB.java (in the appropriate section)

// In AdminEJB.java, update the getFilteredAppointments method:

// In AdminEJB.java, update the getFilteredAppointments method:

// In AdminEJB.java, update the getFilteredAppointments method:

@Override
public List<AppointmentDTO> getFilteredAppointments(AppointmentFilterDTO filter) {
    try {
        System.out.println("=== getFilteredAppointments START ===");
        System.out.println("Filter: clientName=" + filter.getClientName() + 
                         ", artistName=" + filter.getArtistName() + 
                         ", status=" + filter.getStatus() + 
                         ", startDate=" + filter.getStartDate() + 
                         ", endDate=" + filter.getEndDate());
        
        // First, let's verify the database connection by running a simple query
        Long testCount = em.createQuery("SELECT COUNT(a) FROM Appointment a", Long.class)
                          .getSingleResult();
        System.out.println("Total appointments in DB: " + testCount);
        
        // Build the JPQL query with correct field names
        StringBuilder jpql = new StringBuilder(
            "SELECT NEW dto.AppointmentDTO(" +
            "a.appointmentId, " +
            "c.userId, c.fullName, c.email, " +            // client fields
            "art.userId, art.fullName, " +                  // artist fields
            "d.designId, COALESCE(d.title, ''), " +         // design fields
            "ts.slotId, " +                                 // slot field
            "a.appointmentDateTime, " +                     // date field
            "a.status, " +                                  // status
            "COALESCE(a.cancellationReason, ''), " +        // cancellation reason
            "COALESCE(a.clientNote, ''), " +                // client note
            "p.amount, COALESCE(p.status, '')) " +          // payment fields
            "FROM Appointment a " +
            "JOIN a.client c " +
            "JOIN a.artist art " +
            "LEFT JOIN a.design d " +
            "LEFT JOIN a.slot ts " +
            "LEFT JOIN a.payment p " +
            "WHERE 1 = 1"
        );
        
        Map<String, Object> params = new HashMap<>();
        
        // Apply filters
        if (filter.getClientName() != null && !filter.getClientName().trim().isEmpty()) {
            jpql.append(" AND LOWER(c.fullName) LIKE LOWER(:clientName)");
            params.put("clientName", "%" + filter.getClientName().trim() + "%");
        }
        
        if (filter.getArtistName() != null && !filter.getArtistName().trim().isEmpty()) {
            jpql.append(" AND LOWER(art.fullName) LIKE LOWER(:artistName)");
            params.put("artistName", "%" + filter.getArtistName().trim() + "%");
        }
        
        if (filter.getStatus() != null && !filter.getStatus().isEmpty() && !filter.getStatus().equals("ALL")) {
            jpql.append(" AND a.status = :status");
            params.put("status", filter.getStatus());
        }
        
        if (filter.getStartDate() != null) {
            jpql.append(" AND a.appointmentDateTime >= :startDate");
            params.put("startDate", filter.getStartDate().atStartOfDay());
        }
        
        if (filter.getEndDate() != null) {
            jpql.append(" AND a.appointmentDateTime <= :endDate");
            params.put("endDate", filter.getEndDate().atTime(23, 59, 59));
        }
        
        jpql.append(" ORDER BY a.appointmentDateTime DESC");
        
        System.out.println("JPQL Query: " + jpql.toString());
        System.out.println("Parameters: " + params);
        
        TypedQuery<AppointmentDTO> query = em.createQuery(jpql.toString(), AppointmentDTO.class);
        
        // Set parameters
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
            System.out.println("Set param: " + entry.getKey() + " = " + entry.getValue());
        }
        
        // Apply pagination
        int firstResult = filter.getPage() * filter.getSize();
        query.setFirstResult(firstResult);
        query.setMaxResults(filter.getSize());
        
        System.out.println("Pagination - First: " + firstResult + ", Max: " + filter.getSize());
        
        List<AppointmentDTO> results = query.getResultList();
        System.out.println("Query returned " + results.size() + " appointments");
        
        // Debug: print all results
        for (int i = 0; i < results.size(); i++) {
            AppointmentDTO dto = results.get(i);
            System.out.println("Appointment[" + i + "]: ID=" + dto.getAppointmentId() + 
                             ", Client=" + dto.getClientName() + 
                             ", Artist=" + dto.getArtistName() +
                             ", Status=" + dto.getStatus() +
                             ", Date=" + dto.getAppointmentDateTime());
        }
        
        System.out.println("=== getFilteredAppointments END ===");
        return results;
        
    } catch (Exception e) {
        System.err.println("=== ERROR in getFilteredAppointments ===");
        e.printStackTrace();
        
        // Fallback: Try native query
        return getAppointmentsNative();
    }
}

@Override
public Long countFilteredAppointments(AppointmentFilterDTO filter) {
    try {
        System.out.println("=== countFilteredAppointments START ===");
        
        StringBuilder jpql = new StringBuilder(
            "SELECT COUNT(a) FROM Appointment a " +
            "JOIN a.client c " +
            "JOIN a.artist art " +
            "WHERE 1=1"
        );
        
        Map<String, Object> params = new HashMap<>();
        
        // Apply filters
        if (filter.getClientName() != null && !filter.getClientName().trim().isEmpty()) {
            jpql.append(" AND LOWER(c.fullName) LIKE LOWER(:clientName)");
            params.put("clientName", "%" + filter.getClientName().trim() + "%");
        }
        
        if (filter.getArtistName() != null && !filter.getArtistName().trim().isEmpty()) {
            jpql.append(" AND LOWER(art.fullName) LIKE LOWER(:artistName)");
            params.put("artistName", "%" + filter.getArtistName().trim() + "%");
        }
        
        if (filter.getStatus() != null && !filter.getStatus().isEmpty() && !filter.getStatus().equals("ALL")) {
            jpql.append(" AND a.status = :status");
            params.put("status", filter.getStatus());
        }
        
        if (filter.getStartDate() != null) {
            jpql.append(" AND a.appointmentDateTime >= :startDate");
            params.put("startDate", filter.getStartDate().atStartOfDay());
        }
        
        if (filter.getEndDate() != null) {
            jpql.append(" AND a.appointmentDateTime <= :endDate");
            params.put("endDate", filter.getEndDate().atTime(23, 59, 59));
        }
        
        System.out.println("Count JPQL: " + jpql.toString());
        System.out.println("Count Parameters: " + params);
        
        TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);
        
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        
        Long count = query.getSingleResult();
        System.out.println("Total count: " + count);
        System.out.println("=== countFilteredAppointments END ===");
        
        return count != null ? count : 0L;
        
    } catch (Exception e) {
        System.err.println("Error in countFilteredAppointments: " + e.getMessage());
        e.printStackTrace();
        
        // Fallback: Count all appointments
        try {
            Long totalCount = em.createQuery("SELECT COUNT(a) FROM Appointment a", Long.class)
                               .getSingleResult();
            return totalCount != null ? totalCount : 0L;
        } catch (Exception e2) {
            return 0L;
        }
    }
}

@Override
@Transactional
public boolean updateAppointmentStatus(Long appointmentId, String status, String cancellationReason, Long adminId) {
    try {
        Appointment appointment = em.find(Appointment.class, appointmentId);
        if (appointment == null) {
            throw new IllegalArgumentException("Appointment not found: " + appointmentId);
        }
        
        AppUser admin = findUser(adminId);
        if (admin == null) {
            throw new IllegalArgumentException("Admin not found: " + adminId);
        }
        
        String oldStatus = appointment.getStatus();
        appointment.setStatus(status);
        
        // Handle cancellation reason
        if ("CANCELLED".equals(status)) {
            appointment.setCancellationReason(cancellationReason);
            
            // Free the slot if appointment was booked
            if (appointment.getSlot() != null) {
                TimeSlot slot = appointment.getSlot();
                slot.setStatus(TimeSlot.TimeSlotStatus.AVAILABLE);
                em.merge(slot);
            }
        } else {
            appointment.setCancellationReason(null);
        }
        
        // Handle slot status based on appointment status
        if (appointment.getSlot() != null) {
            TimeSlot slot = appointment.getSlot();
            if ("CONFIRMED".equals(status) || "PAID".equals(status)) {
                slot.setStatus(TimeSlot.TimeSlotStatus.BOOKED);
            } else if ("COMPLETED".equals(status)) {
                slot.setStatus(TimeSlot.TimeSlotStatus.BOOKED); // Keep as booked for record
            } else if ("PENDING".equals(status)) {
                slot.setStatus(TimeSlot.TimeSlotStatus.AVAILABLE);
            }
            em.merge(slot);
        }
        
        em.merge(appointment);
        
        // Log the action
        createAnnouncement(adminId, 
            "Appointment Status Updated", 
            String.format("Admin %s changed appointment %d status from %s to %s", 
                admin.getFullName(), appointmentId, oldStatus, status),
            "ADMIN");
        
        return true;
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

// Add this simple test method
public List<Appointment> testGetAllAppointments() {
    try {
        System.out.println("Testing getAllAppointments...");
        return em.createQuery("SELECT a FROM Appointment a", Appointment.class).getResultList();
    } catch (Exception e) {
        e.printStackTrace();
        return new ArrayList<>();
    }
}


// Add to AdminEJB.java
public String testDatabaseConnection() {
    try {
        System.out.println("=== TESTING DATABASE CONNECTION ===");
        
        // Test 1: Count appointments
        Long count = em.createQuery("SELECT COUNT(a) FROM Appointment a", Long.class)
                      .getSingleResult();
        System.out.println("Appointment count: " + count);
        
        // Test 2: Get raw data
        List<Object[]> testData = em.createQuery(
            "SELECT a.appointmentId, a.appointmentDateTime, a.status FROM Appointment a", 
            Object[].class)
            .setMaxResults(3)
            .getResultList();
        
        System.out.println("Test data:");
        for (Object[] row : testData) {
            System.out.println("ID: " + row[0] + 
                             ", Date: " + row[1] + " (Type: " + 
                             (row[1] != null ? row[1].getClass().getName() : "null") + 
                             "), Status: " + row[2]);
        }
        
        return "Database test successful. Found " + count + " appointments.";
    } catch (Exception e) {
        System.err.println("=== DATABASE TEST FAILED ===");
        e.printStackTrace();
        return "Database test failed: " + e.getMessage();
    }
}

@Override
public List<TimeSlot> getAvailableSlotsForArtist(Long artistId) {
    return em.createQuery(
        "SELECT ts FROM TimeSlot ts " +
        "WHERE ts.artist.userId = :artistId " +
        "AND ts.status = 'AVAILABLE' " +
        "AND ts.startTime > CURRENT_TIMESTAMP " +
        "ORDER BY ts.startTime ASC", 
        TimeSlot.class)
        .setParameter("artistId", artistId)
        .getResultList();
}

@Override
public Map<String, Long> getAppointmentStatistics(LocalDate startDate, LocalDate endDate) {
    Map<String, Long> stats = new HashMap<>();
    
    String[] statuses = {"PENDING", "CONFIRMED", "COMPLETED", "CANCELLED", "PAID"};
    
    for (String status : statuses) {
        try {
            StringBuilder jpql = new StringBuilder(
                "SELECT COUNT(a) FROM Appointment a WHERE a.status = :status");
            
            if (startDate != null) {
                jpql.append(" AND a.appointmentDateTime >= :startDate");
            }
            if (endDate != null) {
                jpql.append(" AND a.appointmentDateTime <= :endDate");
            }
            
            TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class)
                .setParameter("status", status);
            
            if (startDate != null) {
                query.setParameter("startDate", startDate.atStartOfDay());
            }
            if (endDate != null) {
                query.setParameter("endDate", endDate.atTime(23, 59, 59));
            }
            
            Long count = query.getSingleResult();
            stats.put(status, count != null ? count : 0L);
        } catch (Exception e) {
            System.err.println("Error counting status " + status + ": " + e.getMessage());
            stats.put(status, 0L);
        }
    }
    return stats;
}

// In AdminEJB.java - Update getAppointmentsNative() method:
// Enhanced version with payment info
public List<AppointmentDTO> getAppointmentsNative() {
    try {
        System.out.println("Using native SQL query...");
        
        String sql = "SELECT " +
                    "a.APPOINTMENTID, " +
                    "c.FULLNAME as clientName, c.EMAIL as clientEmail, " +
                    "art.FULLNAME as artistName, " +
                    "COALESCE(d.TITLE, '') as designTitle, " +
                    "a.SLOT_ID, " +
                    "a.APPOINTMENTDATETIME, " +
                    "a.STATUS, " +
                    "COALESCE(a.CANCELLATION_REASON, '') as cancellationReason, " +
                    "COALESCE(a.CLIENT_NOTE, '') as clientNote, " +
                    "COALESCE(p.amount, 0) as amount, " +
                    "COALESCE(p.status, '') as paymentStatus " +
                    "FROM appointment a " +
                    "JOIN app_user c ON a.CLIENT_ID = c.USER_ID " +
                    "JOIN app_user art ON a.ARTIST_ID = art.USER_ID " +
                    "LEFT JOIN tattoo_design d ON a.DESIGN_ID = d.DESIGNID " +
                    "LEFT JOIN payment p ON a.APPOINTMENTID = p.APPOINTMENT_ID " +
                    "ORDER BY a.APPOINTMENTDATETIME DESC";
        
        Query query = em.createNativeQuery(sql);
        List<Object[]> results = query.getResultList();
        
        List<AppointmentDTO> appointments = new ArrayList<>();
        for (Object[] row : results) {
            AppointmentDTO dto = new AppointmentDTO();
            dto.setAppointmentId(((Number) row[0]).longValue());
            dto.setClientName((String) row[1]);
            dto.setClientEmail((String) row[2]);
            dto.setArtistName((String) row[3]);
            dto.setDesignTitle((String) row[4]);
            dto.setSlotId(row[5] != null ? ((Number) row[5]).intValue() : null);
            
            // Handle date conversion
            if (row[6] != null) {
                if (row[6] instanceof java.sql.Timestamp) {
                    dto.setAppointmentDateTime(((java.sql.Timestamp) row[6]).toLocalDateTime());
                } else if (row[6] instanceof java.util.Date) {
                    dto.setAppointmentDateTime(((java.util.Date) row[6]).toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
                }
            }
            
            dto.setStatus((String) row[7]);
            dto.setCancellationReason((String) row[8]);
            dto.setClientNote((String) row[9]);
            
            // Handle amount - check if Payment entity has amount field
            if (row[10] != null && ((Number) row[10]).doubleValue() > 0) {
                dto.setAmount(new BigDecimal(((Number) row[10]).doubleValue()));
            }
            
            dto.setPaymentStatus((String) row[11]);
            
            appointments.add(dto);
        }
        
        System.out.println("Native query found " + appointments.size() + " appointments");
        return appointments;
        
    } catch (Exception e) {
        System.err.println("=== ERROR in native query ===");
        e.printStackTrace();
        return new ArrayList<>();
    }
}// In AdminEJB.java - Add this method if not already there:

@Override
public List<Appointment> getAllAppointmentsSimple() {
    try {
        System.out.println("=== SIMPLE QUERY TEST ===");
        return em.createQuery(
            "SELECT a FROM Appointment a " +
            "LEFT JOIN FETCH a.client " +
            "LEFT JOIN FETCH a.artist " +
            "LEFT JOIN FETCH a.design " +
            "LEFT JOIN FETCH a.slot " +
            "ORDER BY a.appointmentDateTime DESC", 
            Appointment.class)
            .getResultList();
    } catch (Exception e) {
        e.printStackTrace();
        return new ArrayList<>();
    }
}

// Add this method to AdminEJB.java for database testing:
public String testDatabaseSchema() {
    try {
        System.out.println("=== TESTING DATABASE SCHEMA ===");
        
        // Test 1: Check if tables exist
        String[] tables = {"appointment", "app_user", "tattoo_design", "time_slot", "payment"};
        for (String table : tables) {
            try {
                Long count = (Long) em.createNativeQuery("SELECT COUNT(*) FROM " + table)
                                     .getSingleResult();
                System.out.println("Table " + table + ": " + count + " records");
            } catch (Exception e) {
                System.err.println("Table " + table + " not found or error: " + e.getMessage());
            }
        }
        
        // Test 2: Try a simple join query - FIXED COLUMN NAMES
        String sql = "SELECT " +
                    "a.APPOINTMENTID, " +
                    "c.FULLNAME as client_name, " +
                    "art.FULLNAME as artist_name, " +
                    "a.STATUS " +
                    "FROM appointment a " +
                    "INNER JOIN app_user c ON a.CLIENT_ID = c.USER_ID " +  // ✅ FIXED
                    "INNER JOIN app_user art ON a.ARTIST_ID = art.USER_ID " +  // ✅ FIXED
                    "LIMIT 5";
        
        try {
            List<Object[]> results = em.createNativeQuery(sql).getResultList();
            System.out.println("Simple join query successful. Found " + results.size() + " results:");
            for (Object[] row : results) {
                System.out.println("ID: " + row[0] + ", Client: " + row[1] + ", Artist: " + row[2] + ", Status: " + row[3]);
            }
        } catch (Exception e) {
            System.err.println("Join query failed: " + e.getMessage());
            return "Schema test failed: " + e.getMessage();
        }
        
        return "Schema test completed successfully";
        
    } catch (Exception e) {
        System.err.println("Schema test failed completely: " + e.getMessage());
        e.printStackTrace();
        return "Schema test failed: " + e.getMessage();
    }
}
    
public List<AppointmentDTO> getAllAppointmentsSimpleNative() {
    try {
        System.out.println("=== TEST: Getting all appointments via simple native query ===");
        
        String sql = "SELECT " +
                    "a.APPOINTMENTID, " +
                    "c.FULLNAME as clientName, " +
                    "art.FULLNAME as artistName, " +
                    "a.APPOINTMENTDATETIME, " +
                    "a.STATUS " +
                    "FROM appointment a " +
                    "JOIN app_user c ON a.CLIENT_ID = c.USER_ID " +
                    "JOIN app_user art ON a.ARTIST_ID = art.USER_ID " +
                    "ORDER BY a.APPOINTMENTDATETIME DESC";
        
        Query query = em.createNativeQuery(sql);
        List<Object[]> results = query.getResultList();
        
        System.out.println("Found " + results.size() + " appointments in database");
        
        List<AppointmentDTO> appointments = new ArrayList<>();
        for (Object[] row : results) {
            AppointmentDTO dto = new AppointmentDTO();
            dto.setAppointmentId(((Number) row[0]).longValue());
            dto.setClientName((String) row[1]);
            dto.setArtistName((String) row[2]);
            
            if (row[3] != null) {
                if (row[3] instanceof java.sql.Timestamp) {
                    dto.setAppointmentDateTime(((java.sql.Timestamp) row[3]).toLocalDateTime());
                }
            }
            
            dto.setStatus((String) row[4]);
            
            appointments.add(dto);
            System.out.println("Appt ID: " + dto.getAppointmentId() + 
                             ", Client: " + dto.getClientName() + 
                             ", Artist: " + dto.getArtistName() +
                             ", Status: " + dto.getStatus());
        }
        
        return appointments;
        
    } catch (Exception e) {
        System.err.println("Error in getAllAppointmentsSimpleNative: " + e.getMessage());
        e.printStackTrace();
        return new ArrayList<>();
    }
}
public String testDataLoad() {
    try {
        StringBuilder result = new StringBuilder();
        
        // Test 1: Count total appointments
        Long totalCount = em.createQuery("SELECT COUNT(a) FROM Appointment a", Long.class)
                          .getSingleResult();
        result.append("Total appointments in DB: ").append(totalCount).append("\n");
        
        // Test 2: Check if JPQL works
        try {
            List<Appointment> jpqlResults = em.createQuery(
                "SELECT a FROM Appointment a JOIN a.client c JOIN a.artist art", 
                Appointment.class)
                .setMaxResults(5)
                .getResultList();
            result.append("JPQL query returned: ").append(jpqlResults.size()).append(" results\n");
        } catch (Exception e) {
            result.append("JPQL query failed: ").append(e.getMessage()).append("\n");
        }
        
        // Test 3: Try native query
        try {
            String sql = "SELECT COUNT(*) FROM appointment a " +
                        "JOIN app_user c ON a.CLIENT_ID = c.USER_ID " +
                        "JOIN app_user art ON a.ARTIST_ID = art.USER_ID";
            Long nativeCount = (Long) em.createNativeQuery(sql).getSingleResult();
            result.append("Native join count: ").append(nativeCount).append("\n");
        } catch (Exception e) {
            result.append("Native query failed: ").append(e.getMessage()).append("\n");
        }
        
        return result.toString();
        
    } catch (Exception e) {
        return "Test failed: " + e.getMessage();
    }
}

@Override
public Appointment getFullAppointmentDetails(Long appointmentId) {
    try {
        System.out.println("=== Fetching full appointment details for ID: " + appointmentId);
        
        Appointment appointment = em.find(Appointment.class, appointmentId);
        
        if (appointment != null) {
            // Force initialization of lazy-loaded relationships
            if (appointment.getClient() != null) {
                appointment.getClient().getUserId(); // Initialize
            }
            if (appointment.getArtist() != null) {
                appointment.getArtist().getUserId(); // Initialize
            }
            if (appointment.getDesign() != null) {
                appointment.getDesign().getDesignId(); // Initialize
            }
            if (appointment.getSlot() != null) {
                appointment.getSlot().getSlotId(); // Initialize
            }
            if (appointment.getPayment() != null) {
                appointment.getPayment().getPaymentId(); // Initialize
            }
            
            System.out.println("Found appointment: ID=" + appointment.getAppointmentId() +
                             ", Client=" + (appointment.getClient() != null ? appointment.getClient().getFullName() : "null") +
                             ", Payment=" + (appointment.getPayment() != null ? appointment.getPayment().getAmount() : "null"));
        } else {
            System.out.println("Appointment not found for ID: " + appointmentId);
        }
        
        return appointment;
        
    } catch (Exception e) {
        System.err.println("Error fetching full appointment details: " + e.getMessage());
        e.printStackTrace();
        throw e;
    }
}


// Add these methods to your existing AdminEJB.java:
public List<TimeSlotDTO> debugTimeSlotQuery() {
    try {
        System.out.println("=== DEBUG TIME SLOT QUERY ===");
        
        // Test 1: Check if we can query basic data
        List<Object[]> test1 = em.createQuery(
            "SELECT ts.slotId, ts.artist.userId, ts.startTime, ts.status FROM TimeSlot ts", 
            Object[].class)
            .setMaxResults(5)
            .getResultList();
            
        System.out.println("Test 1 - Basic query: " + test1.size() + " results");
        for (Object[] row : test1) {
            System.out.println("  Slot ID: " + row[0] + 
                             ", Artist ID: " + row[1] + 
                             ", Status: " + row[3] + 
                             ", Status type: " + (row[3] != null ? row[3].getClass().getName() : "null"));
        }
        
        // Test 2: Try the constructor expression with STRING conversion
        try {
            List<TimeSlotDTO> test2 = em.createQuery(
                "SELECT NEW dto.TimeSlotDTO(" +
                "ts.slotId, " +
                "a.userId, " +
                "a.fullName, " +
                "ts.startTime, " +
                "ts.endTime, " +
                "STRING(ts.status), " +  // Using STRING() function
                "COALESCE(ts.blockReason, '')) " +
                "FROM TimeSlot ts " +
                "JOIN ts.artist a " +
                "ORDER BY ts.startTime ASC " +
                "LIMIT 5", 
                TimeSlotDTO.class)
                .getResultList();
                
            System.out.println("Test 2 - Constructor query with STRING(): " + test2.size() + " results");
            return test2;
            
        } catch (Exception e) {
            System.err.println("Test 2 failed: " + e.getMessage());
            e.printStackTrace();
            
            // Test 3: Try without STRING() - maybe your JPA doesn't support it
            try {
                List<TimeSlotDTO> test3 = em.createQuery(
                    "SELECT NEW dto.TimeSlotDTO(" +
                    "ts.slotId, " +
                    "a.userId, " +
                    "a.fullName, " +
                    "ts.startTime, " +
                    "ts.endTime, " +
                    "ts.status.name(), " +  // Using .name() method
                    "COALESCE(ts.blockReason, '')) " +
                    "FROM TimeSlot ts " +
                    "JOIN ts.artist a " +
                    "ORDER BY ts.startTime ASC " +
                    "LIMIT 5", 
                    TimeSlotDTO.class)
                    .getResultList();
                    
                System.out.println("Test 3 - Constructor query with .name(): " + test3.size() + " results");
                return test3;
                
            } catch (Exception e2) {
                System.err.println("Test 3 also failed: " + e2.getMessage());
                throw e2;
            }
        }
        
    } catch (Exception e) {
        System.err.println("All debug queries failed: " + e.getMessage());
        e.printStackTrace();
        return new ArrayList<>();
    }
}
@Override
public List<TimeSlotDTO> getFilteredTimeSlots(TimeSlotFilterDTO filter) {
    try {
        System.out.println("=== GETTING FILTERED TIME SLOTS ===");
        System.out.println("DEBUG: Artist ID filter: " + filter.getArtistId());
        System.out.println("DEBUG: Status filter: " + filter.getStatus());
        System.out.println("DEBUG: Start Date: " + filter.getStartDate());
        System.out.println("DEBUG: End Date: " + filter.getEndDate());
        
        // Build WHERE clause dynamically
        StringBuilder whereClause = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        
        // CRITICAL: Filter by artist ID - THIS IS REQUIRED!
        if (filter.getArtistId() != null) {
            whereClause.append(" AND ARTIST_ID = ?");
            params.add(filter.getArtistId());
        } else {
            System.err.println("ERROR: artistId is null! Cannot filter time slots.");
            return new ArrayList<>();
        }
        
        // Filter by status
        if (filter.getStatus() != null && !filter.getStatus().isEmpty() && !"ALL".equals(filter.getStatus())) {
            whereClause.append(" AND STATUS = ?");
            params.add(filter.getStatus());
        }
        
        // Filter by date range
        if (filter.getStartDate() != null) {
            whereClause.append(" AND STARTTIME >= ?");
            params.add(java.sql.Timestamp.valueOf(filter.getStartDate().atStartOfDay()));
        }
        
        if (filter.getEndDate() != null) {
            whereClause.append(" AND ENDTIME <= ?");
            params.add(java.sql.Timestamp.valueOf(filter.getEndDate().atTime(23, 59, 59)));
        }
        
        String sql = "SELECT " +
            "SLOTID, " +
            "ARTIST_ID, " +
            "STARTTIME, " +
            "ENDTIME, " +
            "STATUS, " +
            "COALESCE(BLOCKREASON, '') as BLOCKREASON, " +
            "BLOCKED_BY_ADMIN_ID " +
            "FROM time_slot " +
            whereClause.toString() +
            " ORDER BY STARTTIME ASC";
        
        System.out.println("DEBUG: SQL: " + sql);
        System.out.println("DEBUG: Params: " + params);
        
        Query query = em.createNativeQuery(sql);
        
        // Set parameters
        for (int i = 0; i < params.size(); i++) {
            query.setParameter(i + 1, params.get(i));
        }
        
        // ========== PAGINATION CODE GOES HERE ==========
        // Apply pagination if needed - using Integer instead of int
        Integer page = filter.getPage();
        Integer size = filter.getSize();
        if (page != null && size != null && page >= 0 && size > 0) {
            int firstResult = page * size;
            query.setFirstResult(firstResult);
            query.setMaxResults(size);
            System.out.println("DEBUG: Pagination - Page: " + page + 
                             ", Size: " + size + 
                             ", First Result: " + firstResult);
        }
        // ========== END PAGINATION ==========
        
        List<Object[]> results = query.getResultList();
        System.out.println("DEBUG: Filtered results: " + results.size() + " slots found for artist " + filter.getArtistId());
        
        List<TimeSlotDTO> dtos = new ArrayList<>();
        
        for (Object[] row : results) {
            try {
                TimeSlotDTO dto = new TimeSlotDTO();
                
                // Basic fields
                if (row[0] != null) dto.setSlotId(((Number) row[0]).intValue());
                if (row[1] != null) dto.setArtistId(((Number) row[1]).longValue());
                
                // Get artist name
                if (dto.getArtistId() != null) {
                    try {
                        String artistSql = "SELECT FULLNAME FROM app_user WHERE USER_ID = ?";
                        String artistName = (String) em.createNativeQuery(artistSql)
                            .setParameter(1, dto.getArtistId())
                            .getSingleResult();
                        dto.setArtistName(artistName);
                    } catch (Exception e) {
                        dto.setArtistName("Artist #" + dto.getArtistId());
                        System.out.println("WARN: Could not fetch name for artist " + dto.getArtistId());
                    }
                }
                
                // Dates
                if (row[2] != null && row[2] instanceof java.sql.Timestamp) {
                    dto.setStartTime(((java.sql.Timestamp) row[2]).toLocalDateTime());
                }
                if (row[3] != null && row[3] instanceof java.sql.Timestamp) {
                    dto.setEndTime(((java.sql.Timestamp) row[3]).toLocalDateTime());
                }
                
                if (row[4] != null) dto.setStatus((String) row[4]);
                if (row[5] != null) dto.setBlockReason((String) row[5]);
                
                // Get admin name if blocked
                if (row[6] != null) {
                    Long adminId = ((Number) row[6]).longValue();
                    try {
                        String adminSql = "SELECT USERNAME FROM app_user WHERE USER_ID = ? AND ROLE = 'ADMIN'";
                        String adminName = (String) em.createNativeQuery(adminSql)
                            .setParameter(1, adminId)
                            .getSingleResult();
                        dto.setBlockedByAdminName(adminName);
                    } catch (Exception e) {
                        dto.setBlockedByAdminName("Admin #" + adminId);
                    }
                }
                
                System.out.println("DEBUG: Slot " + dto.getSlotId() + 
                                 " - Artist: " + dto.getArtistName() + 
                                 " (ID: " + dto.getArtistId() + ")" +
                                 ", Status: " + dto.getStatus() +
                                 ", Start: " + dto.getStartTime());
                
                dtos.add(dto);
                
            } catch (Exception e) {
                System.err.println("ERROR processing row: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("DEBUG: Total DTOs created: " + dtos.size());
        return dtos;
        
    } catch (Exception e) {
        System.err.println("CRITICAL ERROR in getFilteredTimeSlots: " + e.getMessage());
        e.printStackTrace();
        return new ArrayList<>();
    }
}
public String debugTimeSlots() {
    try {
        StringBuilder result = new StringBuilder();
        
        // Test 1: Count time slots
        String countSql = "SELECT COUNT(*) FROM time_slot";
        Number count = (Number) em.createNativeQuery(countSql).getSingleResult();
        result.append("Total time slots in database: ").append(count).append("\n\n");
        
        // Test 2: Get sample data
        String sampleSql = "SELECT SLOTID, ARTIST_ID, STARTTIME, STATUS FROM time_slot LIMIT 5";
        List<Object[]> samples = em.createNativeQuery(sampleSql).getResultList();
        
        result.append("Sample time slots:\n");
        for (Object[] row : samples) {
            result.append("ID: ").append(row[0])
                  .append(", Artist ID: ").append(row[1])
                  .append(", Start: ").append(row[2])
                  .append(", Status: ").append(row[3])
                  .append("\n");
        }
        
        // Test 3: Check if artists exist
        result.append("\nChecking artist with ID 2: ");
        try {
            String artistSql = "SELECT FULLNAME FROM app_user WHERE USER_ID = 2";
            String artistName = (String) em.createNativeQuery(artistSql).getSingleResult();
            result.append("Found artist: ").append(artistName);
        } catch (Exception e) {
            result.append("Artist not found or error: ").append(e.getMessage());
        }
        
        System.out.println("DEBUG TIME SLOTS:\n" + result.toString());
        return result.toString();
        
    } catch (Exception e) {
        String error = "Debug failed: " + e.getMessage();
        System.err.println(error);
        e.printStackTrace();
        return error;
    }
}

@Override
public Long countFilteredTimeSlots(TimeSlotFilterDTO filter) {
    try {
        System.out.println("=== COUNTING FILTERED TIME SLOTS ===");
        
        // Simple count query
        String sql = "SELECT COUNT(*) FROM time_slot";
        
        Query query = em.createNativeQuery(sql);
        Number count = (Number) query.getSingleResult();
        
        System.out.println("DEBUG: Count result: " + count);
        return count != null ? count.longValue() : 0L;
        
    } catch (Exception e) {
        System.err.println("Error in countFilteredTimeSlots: " + e.getMessage());
        e.printStackTrace();
        return 0L;
    }
}
// This should already be in your AdminEJB.java - just ensure it exists
@Override
@Transactional
public boolean blockTimeSlotWithNotification(Integer slotId, Long adminId, String reason) {
    try {
        TimeSlot slot = em.find(TimeSlot.class, slotId);
        if (slot == null) {
            throw new IllegalArgumentException("Slot not found: " + slotId);
        }
        
        AppUser admin = findUser(adminId);
        AppUser artist = slot.getArtist();
        
        // Check if slot has an appointment
        boolean appointmentCancelled = false;
        try {
            Appointment appointment = em.createQuery(
                "SELECT a FROM Appointment a WHERE a.slot.slotId = :slotId", 
                Appointment.class)
                .setParameter("slotId", slotId)
                .getSingleResult();
            
            // Cancel the appointment
            appointment.setStatus("CANCELLED");
            appointment.setCancellationReason(
                "Appointment cancelled because time slot was blocked by admin. Reason: " + reason);
            em.merge(appointment);
            appointmentCancelled = true;
            
            // Notify client
            Announcement clientAnnouncement = new Announcement();
            clientAnnouncement.setTitle("Appointment Cancelled - Slot Blocked by Admin");
            clientAnnouncement.setMessage(String.format(
                "Your appointment with %s on %s has been cancelled because the time slot was blocked by admin. " +
                "Reason: %s. Please book another slot.",
                artist.getFullName(),
                slot.getStartTime(),
                reason
            ));
            clientAnnouncement.setPostedAt(LocalDateTime.now());
            clientAnnouncement.setPostedBy(admin);
            clientAnnouncement.setTargetRole("CLIENT");
            em.persist(clientAnnouncement);
            
        } catch (NoResultException e) {
            // Slot not booked, that's fine
        }
        
        // Block the slot
        slot.setStatus(TimeSlot.TimeSlotStatus.BLOCKED);
        slot.setBlockReason(reason);
        slot.setBlockedBy(admin);
        em.merge(slot);
        
        // Notify artist
        Announcement artistAnnouncement = new Announcement();
        artistAnnouncement.setTitle("Time Slot Blocked by Admin" + 
            (appointmentCancelled ? " - Appointment Cancelled" : ""));
        artistAnnouncement.setMessage(String.format(
            "Admin %s has blocked your time slot from %s to %s. Reason: %s" +
            (appointmentCancelled ? 
                "\nNote: An existing appointment was cancelled due to this block." : ""),
            admin.getFullName(),
            slot.getStartTime(),
            slot.getEndTime(),
            reason
        ));
        artistAnnouncement.setPostedAt(LocalDateTime.now());
        artistAnnouncement.setPostedBy(admin);
        artistAnnouncement.setTargetRole("ARTIST");
        em.persist(artistAnnouncement);
        
        return true;
        
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

@Override
@Transactional
public boolean unblockTimeSlotWithNotification(Integer slotId, Long adminId) {
    try {
        // Unblock the slot using existing method
        unblockTimeSlot(slotId, adminId);
        
        // Get slot details
        TimeSlot slot = em.find(TimeSlot.class, slotId);
        if (slot == null) {
            return false;
        }
        
        // Create notification for artist
        AppUser artist = slot.getArtist();
        AppUser admin = findUser(adminId);
        
        // Create announcement for artist
        Announcement announcement = new Announcement();
        announcement.setTitle("Time Slot Unblocked");
        announcement.setMessage(String.format(
            "Admin %s has unblocked your time slot from %s to %s. The slot is now available.",
            admin.getFullName(),
            slot.getStartTime(),
            slot.getEndTime()
        ));
        announcement.setPostedAt(LocalDateTime.now());
        announcement.setPostedBy(admin);
        announcement.setTargetRole("ARTIST");
        em.persist(announcement);
        
        return true;
        
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

@Override
public List<TimeSlot> getBlockedSlotsForArtist(Long artistId) {
    return em.createQuery(
        "SELECT ts FROM TimeSlot ts " +
        "WHERE ts.artist.userId = :artistId " +
        "AND ts.status = 'BLOCKED' " +
        "AND ts.startTime > CURRENT_TIMESTAMP " +
        "ORDER BY ts.startTime ASC",
        TimeSlot.class)
        .setParameter("artistId", artistId)
        .getResultList();
}

@Override
public List<TimeSlot> getBookedSlotsForArtist(Long artistId) {
    return em.createQuery(
        "SELECT ts FROM TimeSlot ts " +
        "WHERE ts.artist.userId = :artistId " +
        "AND ts.status = 'BOOKED' " +
        "AND ts.startTime > CURRENT_TIMESTAMP " +
        "ORDER BY ts.startTime ASC",
        TimeSlot.class)
        .setParameter("artistId", artistId)
        .getResultList();
}

@Override
public Long countFilteredPayments(String clientName, String artistName, 
                                  String paymentMethod, String status,
                                  LocalDate startDate, LocalDate endDate) {
    try {
        StringBuilder jpql = new StringBuilder(
            "SELECT COUNT(p) FROM Payment p " +
            "JOIN p.appointment a " +
            "JOIN p.client c " +
            "JOIN a.artist art " +
            "WHERE 1 = 1"
        );
        
        Map<String, Object> params = new HashMap<>();
        
        // Apply filters (same as above)
        if (clientName != null && !clientName.trim().isEmpty()) {
            jpql.append(" AND LOWER(c.fullName) LIKE LOWER(:clientName)");
            params.put("clientName", "%" + clientName.trim() + "%");
        }
        
        if (artistName != null && !artistName.trim().isEmpty()) {
            jpql.append(" AND LOWER(art.fullName) LIKE LOWER(:artistName)");
            params.put("artistName", "%" + artistName.trim() + "%");
        }
        
        if (paymentMethod != null && !paymentMethod.isEmpty() && !paymentMethod.equals("ALL")) {
            jpql.append(" AND p.paymentMethod = :paymentMethod");
            params.put("paymentMethod", paymentMethod);
        }
        
        if (status != null && !status.isEmpty() && !status.equals("ALL")) {
            jpql.append(" AND p.status = :status");
            params.put("status", status);
        }
        
        if (startDate != null) {
            jpql.append(" AND p.paymentDate >= :startDate");
            params.put("startDate", startDate.atStartOfDay());
        }
        
        if (endDate != null) {
            jpql.append(" AND p.paymentDate <= :endDate");
            params.put("endDate", endDate.atTime(23, 59, 59));
        }
        
        TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);
        
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        
        Long count = query.getSingleResult();
        return count != null ? count : 0L;
        
    } catch (Exception e) {
        System.err.println("Error in countFilteredPayments: " + e.getMessage());
        return 0L;
    }
}




@Override
public BigDecimal calculateArtistPendingEarningsSummary(Long artistId) {
    try {
        String ql = "SELECT SUM(e.artistShare) FROM EarningLog e " +
                    "WHERE e.artist.userId = :artistId " +
                    "AND e.payoutStatus = 'UNPAID'";
        
        TypedQuery<BigDecimal> q = em.createQuery(ql, BigDecimal.class)
                .setParameter("artistId", artistId);
        
        BigDecimal earnings = q.getSingleResult();
        return earnings == null ? BigDecimal.ZERO : earnings.setScale(2, BigDecimal.ROUND_HALF_UP);
        
    } catch (Exception e) {
        System.err.println("Error in calculateArtistPendingEarningsSummary: " + e.getMessage());
        return BigDecimal.ZERO;
    }
}

@Override
public Map<String, Object> getPaymentStatistics(LocalDate startDate, LocalDate endDate) {
    Map<String, Object> stats = new HashMap<>();
    
    try {
        // Total payments
        Long totalPayments = em.createQuery(
            "SELECT COUNT(p) FROM Payment p WHERE p.paymentDate >= :start AND p.paymentDate <= :end",
            Long.class)
            .setParameter("start", startDate.atStartOfDay())
            .setParameter("end", endDate.atTime(23, 59, 59))
            .getSingleResult();
        
        // Total revenue
        BigDecimal totalRevenue = em.createQuery(
            "SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED' " +
            "AND p.paymentDate >= :start AND p.paymentDate <= :end",
            BigDecimal.class)
            .setParameter("start", startDate.atStartOfDay())
            .setParameter("end", endDate.atTime(23, 59, 59))
            .getSingleResult();
        
        // Payments by status
        String[] statuses = {"COMPLETED", "PENDING", "FAILED"};
        Map<String, Long> paymentsByStatus = new HashMap<>();
        for (String status : statuses) {
            Long count = em.createQuery(
                "SELECT COUNT(p) FROM Payment p WHERE p.status = :status " +
                "AND p.paymentDate >= :start AND p.paymentDate <= :end",
                Long.class)
                .setParameter("status", status)
                .setParameter("start", startDate.atStartOfDay())
                .setParameter("end", endDate.atTime(23, 59, 59))
                .getSingleResult();
            paymentsByStatus.put(status, count != null ? count : 0L);
        }
        
        // Payments by method
        List<Object[]> byMethod = em.createQuery(
            "SELECT p.paymentMethod, COUNT(p) FROM Payment p " +
            "WHERE p.paymentDate >= :start AND p.paymentDate <= :end " +
            "GROUP BY p.paymentMethod",
            Object[].class)
            .setParameter("start", startDate.atStartOfDay())
            .setParameter("end", endDate.atTime(23, 59, 59))
            .getResultList();
        
        Map<String, Long> paymentsByMethod = new HashMap<>();
        for (Object[] row : byMethod) {
            paymentsByMethod.put((String) row[0], (Long) row[1]);
        }
        
        stats.put("totalPayments", totalPayments != null ? totalPayments : 0L);
        stats.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        stats.put("paymentsByStatus", paymentsByStatus);
        stats.put("paymentsByMethod", paymentsByMethod);
        
    } catch (Exception e) {
        System.err.println("Error in getPaymentStatistics: " + e.getMessage());
        // Return empty stats
        stats.put("totalPayments", 0L);
        stats.put("totalRevenue", BigDecimal.ZERO);
        stats.put("paymentsByStatus", new HashMap<>());
        stats.put("paymentsByMethod", new HashMap<>());
    }
    
    return stats;
}

@Override
public List<ArtistPendingEarningDTO> getPendingEarningsByArtist(Long artistId) {
    if (artistId == null) return new ArrayList<>();

    List<EarningLog> logs = em.createQuery(
        "SELECT e FROM EarningLog e " +
        "WHERE e.artist.userId = :artistId " +
        "AND e.payoutStatus = 'UNPAID' " +   // <-- THIS IS THE FIX
        "ORDER BY e.calculatedAt ASC",
        EarningLog.class)
        .setParameter("artistId", artistId)
        .getResultList();

    return logs.stream()
        .map(this::mapToArtistPendingEarningDTO)
        .collect(Collectors.toList());
}



// New/Updated method to handle the admin "Pay Artist" action
@Override
@jakarta.transaction.Transactional
public Long payArtist(Long artistId, Long adminId, String notes) {
    if (artistId == null) throw new IllegalArgumentException("artistId is required");

    AppUser artist = findUser(artistId);
    AppUser admin = adminId != null ? findUser(adminId) : null;

    List<EarningLog> pendingLogs = em.createQuery(
        "SELECT e FROM EarningLog e WHERE e.artist.userId = :artistId AND e.payoutStatus = 'UNPAID' ORDER BY e.calculatedAt ASC",
        EarningLog.class)
        .setParameter("artistId", artistId)
        .getResultList();

    if (pendingLogs.isEmpty()) {
        throw new IllegalStateException("No pending earnings for artist: " + artistId);
    }

    BigDecimal totalArtistShare = pendingLogs.stream()
            .map(EarningLog::getArtistShare)
            .filter(x -> x != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);

    ArtistPayout payout = new ArtistPayout();
    payout.setArtist(artist);
    payout.setAdmin(admin);
    payout.setAmount(totalArtistShare);
    payout.setNotes(notes != null ? notes : ("Payout for " + pendingLogs.size() + " log(s)"));
    payout.setPayoutStatus("PAID");
    payout.setPayoutDate(LocalDateTime.now());
    payout.setCreatedAt(LocalDateTime.now());

    em.persist(payout);
    em.flush();

    for (EarningLog log : pendingLogs) {
        log.setPayoutStatus("PAID");
        log.setPayout(payout);
        log.setPayoutAt(payout.getPayoutDate());
        em.merge(log);
    }

    return payout.getPayoutId();
}


@Override
public List<EarningLog> getPendingEarningLogsForArtist(Long artistId) {
    try {
        return em.createQuery(
                "SELECT e FROM EarningLog e WHERE e.artist.userId = :artistId AND e.payoutStatus = 'UNPAID'", EarningLog.class)
                .setParameter("artistId", artistId)
                .getResultList();
    } catch (NoResultException e) {
        return new ArrayList<>();
    }
}

// Add these three methods to fulfill the AdminEJBLocal interface contract.
@Override
public List<ArtistPendingEarningDTO> listAllUnpaidPendingEarnings() {
    List<EarningLog> logs = em.createQuery(
        "SELECT l FROM EarningLog l WHERE l.payout IS NULL AND l.payoutStatus = 'UNPAID'",
        EarningLog.class
    ).getResultList();
    return logs.stream().map(this::mapToArtistPendingEarningDTO).collect(Collectors.toList());
}

@Override
public List<ArtistPendingEarningDTO> listAllPaidPendingEarnings() {
    List<EarningLog> logs = em.createQuery(
        "SELECT l FROM EarningLog l WHERE l.payout IS NOT NULL AND l.payoutStatus = 'PAID'",
        EarningLog.class
    ).getResultList();
    return logs.stream().map(this::mapToArtistPendingEarningDTO).collect(Collectors.toList());
}

@Override
public List<ArtistPayout> listAllArtistPayouts() {
    return em.createQuery(
        "SELECT p FROM ArtistPayout p WHERE p.payoutStatus = 'PAID' ORDER BY p.payoutDate DESC",
        ArtistPayout.class
    ).getResultList();
}


@Override
public BigDecimal getTotalCompletedPaymentsAmount() {
    BigDecimal total = em.createQuery(
        "SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'COMPLETED'",
        BigDecimal.class
    ).getSingleResult();
    return total != null ? total.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
}


private ArtistPendingEarningDTO mapToArtistPendingEarningDTO(EarningLog log) {
    if (log == null) return null;

    ArtistPendingEarningDTO dto = new ArtistPendingEarningDTO();

    dto.setLogId(log.getLogId());
    dto.setAppointmentId(
        log.getAppointment() != null ? log.getAppointment().getAppointmentId() : null
    );

    dto.setPaymentId(
        log.getPayment() != null ? log.getPayment().getPaymentId().longValue() : null
    );

    dto.setTotalAmount(log.getTotalAmount());
    dto.setArtistShare(log.getArtistShare());
    dto.setAdminShare(log.getAdminShare());
    dto.setPremiumBonus(log.getPremiumBonus());

    dto.setPayoutStatus(log.getPayoutStatus());
    dto.setCalculatedAt(log.getCalculatedAt());
    dto.setPayoutAt(log.getPayoutAt());

    if (log.getArtist() != null) {
        dto.setArtistId(log.getArtist().getUserId());
        dto.setArtistName(log.getArtist().getFullName());
    }

    if (log.getAdmin() != null) {
        dto.setAdminId(log.getAdmin().getUserId());
        dto.setAdminName(log.getAdmin().getFullName());
    }

    dto.setPayoutId(
        log.getPayout() != null ? log.getPayout().getPayoutId() : null
    );
    dto.setNotes(log.getNotes());

    return dto;
}


@jakarta.transaction.Transactional
public EarningLog createEarningLogForPayment(Integer paymentId, Long adminId) {
    Payment p = em.find(Payment.class, paymentId);
    if (p == null) throw new IllegalArgumentException("Payment not found: " + paymentId);
    if (!"COMPLETED".equalsIgnoreCase(p.getStatus())) {
        throw new IllegalStateException("Payment is not completed: " + p.getStatus());
    }

    // Avoid duplicate logs
    List<EarningLog> existing = em.createQuery(
        "SELECT e FROM EarningLog e WHERE e.payment.paymentId = :pid", EarningLog.class)
        .setParameter("pid", paymentId)
        .getResultList();
    if (!existing.isEmpty()) return existing.get(0);

    Appointment appt = p.getAppointment();
    if (appt == null) throw new IllegalStateException("Payment has no appointment linked.");

    AppUser artist = appt.getArtist();
    if (artist == null) throw new IllegalStateException("Appointment has no artist.");

    BigDecimal total = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
    BigDecimal artistShare = total.multiply(ARTIST_COMMISSION_RATE).setScale(2, RoundingMode.HALF_UP);
    BigDecimal adminShare = total.subtract(artistShare).setScale(2, RoundingMode.HALF_UP);

    EarningLog log = new EarningLog();
    log.setPayment(p);
    log.setAppointment(appt);
    log.setArtist(artist);
    log.setAdmin(adminId != null ? em.find(AppUser.class, adminId) : null);
    log.setTotalAmount(total);
    log.setArtistShare(artistShare);
    log.setAdminShare(adminShare);
    log.setPremiumBonus(BigDecimal.ZERO);
    log.setPayoutStatus("UNPAID"); // Option A: UNPAID -> PAID
    log.setCalculatedAt(LocalDateTime.now());

    em.persist(log);
    em.flush();
    return log;
}


private ArtistPendingEarningDTO toArtistPendingEarningDTO(EarningLog log) {
    ArtistPendingEarningDTO dto = new ArtistPendingEarningDTO();
    
    dto.setLogId(log.getLogId());
    
    // Appointment details
    if (log.getAppointment() != null) {
        dto.setAppointmentId(log.getAppointment().getAppointmentId());
    }
    
    // Financial shares
    dto.setTotalAmount(log.getTotalAmount());
    dto.setArtistShare(log.getArtistShare());
    
    dto.setAdminShare(log.getAdminShare()); 
    // --------------------------------------------------------------------------------------------------
    
    dto.setPayoutStatus(log.getPayoutStatus());
    dto.setCalculatedAt(log.getCalculatedAt());
    
    // Artist details
    if (log.getArtist() != null) {
        dto.setArtistId(log.getArtist().getUserId());
        dto.setArtistName(log.getArtist().getFullName());
    }
    
    // Notes/Other
    dto.setNotes(log.getNotes());
    
    return dto;
}


// In AdminEJB.java - Medical Forms Management methods:

public List<MedicalFormDTO> getFilteredMedicalForms(MedicalFormFilterDTO filter) {
    try {
        // Build the query based on filter criteria
        StringBuilder jpql = new StringBuilder(
            "SELECT NEW dto.MedicalFormDTO(" +
            "mf.formId, c.userId, c.username, c.email, " +
            "a.appointmentId, a.appointmentDateTime, a.status, " +
            "mf.isMinor, mf.isPregnant, mf.diabetes, mf.heartCondition, " +
            "mf.hasAllergies, mf.allergyDetails, mf.isApproved, " +
            "mf.submittedAt, mf.approvedAt, " +
            "admin.userId, admin.username) " +
            "FROM MedicalForm mf " +
            "JOIN mf.client c " +
            "JOIN mf.appointment a " +
            "LEFT JOIN mf.approvedBy admin " +
            "WHERE 1=1"
        );
        
        Map<String, Object> parameters = new HashMap<>();
        
        if (filter.getClientName() != null && !filter.getClientName().isEmpty()) {
            jpql.append(" AND (LOWER(c.username) LIKE LOWER(:clientName) OR LOWER(c.email) LIKE LOWER(:clientName))");
            parameters.put("clientName", "%" + filter.getClientName() + "%");
        }
        
        if (filter.getIsApproved() != null) {
            jpql.append(" AND mf.isApproved = :isApproved");
            parameters.put("isApproved", filter.getIsApproved());
        }
        
        if (filter.getAppointmentStatus() != null && !filter.getAppointmentStatus().equals("ALL")) {
            jpql.append(" AND a.status = :appointmentStatus");
            parameters.put("appointmentStatus", filter.getAppointmentStatus());
        }
        
        if (filter.getStartDate() != null) {
            jpql.append(" AND DATE(mf.submittedAt) >= :startDate");
            parameters.put("startDate", filter.getStartDate());
        }
        
        if (filter.getEndDate() != null) {
            jpql.append(" AND DATE(mf.submittedAt) <= :endDate");
            parameters.put("endDate", filter.getEndDate());
        }
        
        jpql.append(" ORDER BY mf.submittedAt DESC");
        
        TypedQuery<MedicalFormDTO> query = em.createQuery(jpql.toString(), MedicalFormDTO.class);
        
        // Set parameters
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        
        // Set pagination
        query.setFirstResult(filter.getPage() * filter.getSize());
        query.setMaxResults(filter.getSize());
        
        return query.getResultList();
        
    } catch (Exception e) {
        throw new RuntimeException("Error fetching filtered medical forms: " + e.getMessage(), e);
    }
}

public Long countFilteredMedicalForms(MedicalFormFilterDTO filter) {
    try {
        StringBuilder jpql = new StringBuilder(
            "SELECT COUNT(mf) FROM MedicalForm mf " +
            "JOIN mf.client c " +
            "JOIN mf.appointment a " +
            "WHERE 1=1"
        );
        
        Map<String, Object> parameters = new HashMap<>();
        
        if (filter.getClientName() != null && !filter.getClientName().isEmpty()) {
            jpql.append(" AND (LOWER(c.username) LIKE LOWER(:clientName) OR LOWER(c.email) LIKE LOWER(:clientName))");
            parameters.put("clientName", "%" + filter.getClientName() + "%");
        }
        
        if (filter.getIsApproved() != null) {
            jpql.append(" AND mf.isApproved = :isApproved");
            parameters.put("isApproved", filter.getIsApproved());
        }
        
        if (filter.getAppointmentStatus() != null && !filter.getAppointmentStatus().equals("ALL")) {
            jpql.append(" AND a.status = :appointmentStatus");
            parameters.put("appointmentStatus", filter.getAppointmentStatus());
        }
        
        if (filter.getStartDate() != null) {
            jpql.append(" AND DATE(mf.submittedAt) >= :startDate");
            parameters.put("startDate", filter.getStartDate());
        }
        
        if (filter.getEndDate() != null) {
            jpql.append(" AND DATE(mf.submittedAt) <= :endDate");
            parameters.put("endDate", filter.getEndDate());
        }
        
        TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);
        
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        
        return query.getSingleResult();
        
    } catch (Exception e) {
        throw new RuntimeException("Error counting filtered medical forms: " + e.getMessage(), e);
    }
}

public boolean rejectMedicalForm(Integer formId, Long adminId, String reason) {
    try {
        MedicalForm form = em.find(MedicalForm.class, formId);
        if (form != null) {
            AppUser adminUser = em.find(AppUser.class, adminId);
            if (adminUser == null) {
                throw new RuntimeException("Admin user not found with ID: " + adminId);
            }
            
            form.setIsApproved(false);
            form.setApprovedAt(LocalDateTime.now());
            form.setApprovedBy(adminUser);
            form.setRejectionReason(reason); // Now this will work
            
            em.merge(form);
            em.flush();
            
            return true;
        }
        return false;
    } catch (Exception e) {
        throw new RuntimeException("Error rejecting medical form: " + e.getMessage(), e);
    }
}

public MedicalFormDTO getMedicalFormDetails(Integer formId) {
    try {
        String jpql = 
            "SELECT NEW dto.MedicalFormDTO(" +
            "mf.formId, c.userId, c.username, c.email, " +
            "a.appointmentId, a.appointmentDateTime, a.status, " +
            "mf.isMinor, mf.isPregnant, mf.diabetes, mf.heartCondition, " +
            "mf.hasAllergies, mf.allergyDetails, mf.isApproved, " +
            "mf.submittedAt, mf.approvedAt, " +
            "admin.userId, admin.username) " +
            "FROM MedicalForm mf " +
            "JOIN mf.client c " +
            "JOIN mf.appointment a " +
            "LEFT JOIN mf.approvedBy admin " +
            "WHERE mf.formId = :formId";
        
        TypedQuery<MedicalFormDTO> query = em.createQuery(jpql, MedicalFormDTO.class);
        query.setParameter("formId", formId);
        
        return query.getSingleResult();
        
    } catch (NoResultException e) {
        return null;
    } catch (Exception e) {
        throw new RuntimeException("Error fetching medical form details: " + e.getMessage(), e);
    }
}

@Override
public List<Appointment> getAppointmentsWithPendingForms() {
    return em.createQuery(
        "SELECT a FROM Appointment a " +
        "WHERE EXISTS (SELECT 1 FROM MedicalForm m WHERE m.appointment = a AND m.isApproved = false) " +
        "AND a.status IN ('PENDING', 'CONFIRMED') " +
        "ORDER BY a.appointmentDateTime ASC",
        Appointment.class)
        .getResultList();
}

@Override
public List<Appointment> getAppointmentsWithApprovedForms() {
    return em.createQuery(
        "SELECT a FROM Appointment a " +
        "WHERE EXISTS (SELECT 1 FROM MedicalForm m WHERE m.appointment = a AND m.isApproved = true) " +
        "ORDER BY a.appointmentDateTime DESC",
        Appointment.class)
        .getResultList();
}

// In AdminEJB.java
@Override
public Map<String, Object> getMedicalFormsStatistics(LocalDate startDate, LocalDate endDate) {
    Map<String, Object> stats = new HashMap<>();
    
    try {
        // Count approved forms
        Long approvedCount = em.createQuery(
            "SELECT COUNT(m) FROM MedicalForm m WHERE m.isApproved = true " +
            "AND m.submittedAt >= :start AND m.submittedAt <= :end", Long.class)
            .setParameter("start", startDate.atStartOfDay())
            .setParameter("end", endDate.atTime(23, 59, 59))
            .getSingleResult();
        
        // Count pending forms
        Long pendingCount = em.createQuery(
            "SELECT COUNT(m) FROM MedicalForm m WHERE m.isApproved = false " +
            "AND m.submittedAt >= :start AND m.submittedAt <= :end", Long.class)
            .setParameter("start", startDate.atStartOfDay())
            .setParameter("end", endDate.atTime(23, 59, 59))
            .getSingleResult();
        
        // Count total appointments with forms
        Long appointmentsWithForms = em.createQuery(
            "SELECT COUNT(DISTINCT a) FROM Appointment a " +
            "WHERE EXISTS (SELECT 1 FROM MedicalForm m WHERE m.appointment = a) " +
            "AND a.appointmentDateTime >= :start AND a.appointmentDateTime <= :end", Long.class)
            .setParameter("start", startDate.atStartOfDay())
            .setParameter("end", endDate.atTime(23, 59, 59))
            .getSingleResult();
        
        stats.put("approvedForms", approvedCount != null ? approvedCount : 0L);
        stats.put("pendingForms", pendingCount != null ? pendingCount : 0L);
        stats.put("appointmentsWithForms", appointmentsWithForms != null ? appointmentsWithForms : 0L);
        stats.put("approvalRate", approvedCount != null && appointmentsWithForms != null && appointmentsWithForms > 0 ?
            (approvedCount * 100.0 / appointmentsWithForms) : 0.0);
        
    } catch (Exception e) {
        System.err.println("Error in getMedicalFormsStatistics: " + e.getMessage());
        // Return empty stats
        stats.put("approvedForms", 0L);
        stats.put("pendingForms", 0L);
        stats.put("appointmentsWithForms", 0L);
        stats.put("approvalRate", 0.0);
    }
    
    return stats;
}

@Override
public Announcement getAnnouncementById(Long announcementId) {
    if (announcementId == null) {
        throw new IllegalArgumentException("Announcement ID must not be null.");
    }
    TypedQuery<Announcement> q = em.createQuery(
        "SELECT a FROM Announcement a LEFT JOIN FETCH a.postedBy pb WHERE a.announcementId = :id",
        Announcement.class
    );
    q.setParameter("id", announcementId);

    List<Announcement> list = q.getResultList();
    if (list.isEmpty()) {
        throw new EntityNotFoundException("Announcement not found: " + announcementId);
    }
    return list.get(0);
}

    
    @Override
public List<Announcement> listAllAnnouncements() {
    // JOIN FETCH to initialize postedBy (so calling code can safely read postedBy fields)
    TypedQuery<Announcement> q = em.createQuery(
        "SELECT a FROM Announcement a LEFT JOIN FETCH a.postedBy pb ORDER BY a.postedAt DESC",
        Announcement.class
    );
    return q.getResultList();
}

    @Override
    public void createPendingEarningForPaidAppointment(Long appointmentId, BigDecimal totalAmount) {
//        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    

}   
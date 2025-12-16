package ejb;

import clientDTO.ArtistCardDTO;
import entities.*;
import jakarta.ejb.Local;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Local
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
//    void unfavouriteDesign(Long clientId, Long designId);
    
    List<DesignFavourite> listFavourites(Long clientId, int offset, int limit);

    // Booking / appointments
    List<TimeSlot> listAvailableTimeSlots(Long artistId, LocalDate date);
    boolean isSlotAvailable(Integer slotId);
//    Long bookAppointment(Long clientId, Long artistId, Long designId, Integer slotId, String clientNote);
    public Long bookAppointment(Long clientId, Long artistId, Long designId, String clientNote);
//    void cancelAppointment(Long appointmentId, String reason);

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
    List<TattooDesign> listDesignsByStyle(String style, int start, int max);
List<TattooDesign> getPopularDesigns(int start, int max);
List<TattooDesign> getNewDesigns(int start, int max);
List<AppUser> getTopArtists(int start, int max);
List<TattooDesign> getTrendingDesigns(int start, int max);

    public List<TattooDesign> getFeaturedDesigns(int start, int max);
    public List<String> getAvailableStyles();
    public Long getClientAppointmentCount(Long clientId);
    public Long getUpcomingAppointmentCount(Long clientId);
    public Long getClientFavouritesCount(Long clientId);
    public Long getPendingMedicalFormsCount(Long clientId);
    public List<Appointment> getUpcomingAppointments(Long clientId, int start, int max);
    public List<TattooDesign> getDesignsByArtist(Long artistId, int start, int max);
    public Double getArtistAverageRating(Long artistId);
    public List<AppUser> getAllActiveArtists(int start, int max);
    public List<TattooDesign> searchDesigns(String keyword, int start, int max);

    public boolean toggleDesignLike(Long clientId, Long designId);
    
    // Likes & favourites
List<Long> listLikedDesignIds(Long clientId);
public boolean toggleFavouriteDesign(Long clientId, Long designId);
public List<Long> listFavouriteDesignIds(Long clientId);
TattooDesign getDesignById(Long designId);



    // --- Reviews ---
   List<Review> getReviewsForDesign(Long designId);

    // --- Related Designs ---
List<TattooDesign> getRelatedDesigns(String style, Long artistId);

    // --- Recommended Designs ---
    List<TattooDesign> getRecommendedDesigns(Long clientId);

    // --- Artist Info ---
    AppUser getArtistInfo(Long artistId);
    
    // --- COMMENTS ---
List<DesignComment> getDesignComments(Long designId);
DesignComment addDesignComment(Long clientId, Long designId, String text);

//boolean toggleDesignLike(Long clientId, Long designId); // you already added this — keep it
public List<Review> getReviewsByArtist(Integer artistId);

    // Medical form
//    MedicalForm submitMedicalForm(Long clientId, Long appointmentId, String data);
    
    // --- Design Detail Page Specific Methods ---
    
    // Design Loading
    TattooDesign findDesignById(Long designId); // Assuming this is the correct EJB name for the Bean call

    // Status checks
    boolean isDesignLikedByClient(Long clientId, Long designId);
    boolean isDesignFavouritedByClient(Long clientId, Long designId);

    // Artist Info & Reviews (NEW)
    Long getArtistTotalReviews(Long artistId); // NEW
    List<Review> getReviewsForArtist(Long artistId); // NEW
    


    // --- Utility/Pagination ---
    // ... (other methods from the previous snippet) ...
    Long getClientAppointmentsCount(Long clientId);
    
    public Experience getArtistExperience(Long artistId);
    
    void addLike(Long designId, Long clientId);           // Adds a like
void removeLike(Long designId, Long clientId);        // Removes a like
void addFavourite(Long designId, Long clientId);      // Adds to favourites
void removeFavourite(Long designId, Long clientId);   // Removes from favourites
void addComment(Long designId, Long clientId, String comment, int rating); 

 public List<TattooDesign> getRelatedDesigns(Long designId, Long artistId, String style);
 
    public List<DesignComment> getCommentsForDesign(Long designId);
void addComment(Long designId, Long clientId, String comment);
Long getDesignLikeCount(Long designId);

    List<TattooDesign> getRecommendedDesignsForClient(Long clientId, int max);

public boolean hasClientBookedArtistAnyStatus(Long clientId, Long artistId);
  public boolean hasClientBookedArtist(Long clientId, Long artistId);
//public void addReview(Long artistId, Long clientId, Double rating, String comment);
void addReview(Long artistId, Long clientId, Double rating, String comment);

public Long getCommentCountForDesign(Long designId);
   public void deleteComment(Long commentId);
   public boolean canClientCommentOnDesign(Long designId, Long clientId);
   
   
   //appointment shits
//   TattooDesign getDesignById(Long designId);
//    AppUser getArtistInfo(Long artistId);
   
//    List<TimeSlot> listAvailableTimeSlots(Long artistId, LocalDate date);
//    boolean isSlotAvailable(Integer slotId);
//    Long createAppointment(
//        Long clientId,
//        Long artistId,
//        Long designId,
//        Integer slotId,
//        String clientNote
//    );
//
//    Appointment getAppointment(Long appointmentId);
//    List<Appointment> listClientAppointments(Long clientId);

//    void cancelAppointment(Long appointmentId, String reason);
    
//    MedicalForm submitMedicalForm(
//        Long clientId,
//        Long appointmentId,
//        MedicalForm form
//    );

    MedicalForm getMedicalForm(Long appointmentId);
//    Payment createMockPayment(
//        Long clientId,
//        Long appointmentId,
//        BigDecimal amount,
//        String method
//    );
//
//    Payment getPaymentByAppointment(Long appointmentId);
List<ArtistCardDTO> getAllArtistsForBrowse();

    public void cancelAppointment(Long appointmentId);
    
    public List<Review> listArtistReviews(Long artistId);
    public boolean hasCompletedAppointment(Long clientId, Long artistId);
//  public void submitArtistReview(Long artistId, Long clientId, Integer rating, String comments);
  public boolean hasClientReviewedArtist(Long clientId, Long artistId);
public void submitArtistReview(Long artistId, Long clientId, Double rating, String comments);
        void addComment(Long designId, Long clientId, String comment, Double rating);
}
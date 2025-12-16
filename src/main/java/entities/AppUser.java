package entities;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "app_user")
public class AppUser implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "USERNAME", nullable = false, unique = true, length = 150)
    private String username;

    @Column(name = "FULLNAME", nullable = false, length = 200)
    private String fullName;

    @Column(name = "EMAIL", nullable = false, unique = true, length = 200)
    private String email;

    @Column(name = "PASSWORD", nullable = false, length = 255)
    private String password;

    @Column(name = "PHONE", length = 50)
    private String phone;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ROLE_ROLEID", nullable = false)
    private GroupMaster role;

    @Column(name = "IS_ACTIVE", nullable = false)
    private boolean isActive = true;

    @Column(name = "IS_VERIFIED", nullable = false)
    private boolean isVerified = false;

     @Column(name = "DEACTIVATION_REASON", length = 500)
    private String deactivationReason;
    
    // --- FIX 2: Added missing field for Artist verific    ation ---
//    @Column(name = "IS_VERIFIED", nullable = false)
//    private Boolean isVerified = false;
    
    // ----------------------
    // relationships
    // ----------------------

    // prevent recursion when serializing user -> designs -> user -> ...
    @JsonbTransient
    @OneToMany(mappedBy = "artist", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<TattooDesign> designs;

    @JsonbTransient
    @OneToMany(mappedBy = "client", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<Appointment> clientAppointments;

    @JsonbTransient
    @OneToMany(mappedBy = "artist", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<Appointment> artistAppointments;

    @JsonbTransient
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LoginHistory> loginHistory;

    @JsonbTransient
    @OneToMany(mappedBy = "artist", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Feedback> feedbacksReceived;

    @JsonbTransient
    @OneToMany(mappedBy = "artist", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Review> reviewsReceived;

    @JsonbTransient
    @OneToMany(mappedBy = "artist", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ArtistSchedule> schedules;

    @JsonbTransient
    @OneToMany(mappedBy = "artist", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TimeSlot> timeSlots;

    @OneToOne(mappedBy = "artist", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Experience experience;
    // --- getters / setters (keep yours) ---

    public Experience getExperience() {
        return experience;
    }

    public void setExperience(Experience experience) {
        this.experience = experience;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    // ... rest of getters and setters unchanged ...
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { this.isVerified = verified; }

    public String getDeactivationReason() { return deactivationReason; }
    public void setDeactivationReason(String deactivationReason) { this.deactivationReason = deactivationReason; }
    
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public GroupMaster getRole() { return role; }
    public void setRole(GroupMaster role) { this.role = role; }

    // ... add remaining getters/setters for fields you left intact ...

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isIsActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public boolean isIsVerified() {
        return isVerified;
    }

    public void setIsVerified(boolean isVerified) {
        this.isVerified = isVerified;
    }

    @JsonbTransient
    public List<TattooDesign> getDesigns() {
        return designs;
    }

    public void setDesigns(List<TattooDesign> designs) {
        this.designs = designs;
    }

    @JsonbTransient
    public List<Appointment> getClientAppointments() {
        return clientAppointments;
    }

    @JsonbTransient
    public void setClientAppointments(List<Appointment> clientAppointments) {
        this.clientAppointments = clientAppointments;
    }

    @JsonbTransient
    public List<Appointment> getArtistAppointments() {
        return artistAppointments;
    }

    @JsonbTransient
    public void setArtistAppointments(List<Appointment> artistAppointments) {
        this.artistAppointments = artistAppointments;
    }

    public List<LoginHistory> getLoginHistory() {
        return loginHistory;
    }

    public void setLoginHistory(List<LoginHistory> loginHistory) {
        this.loginHistory = loginHistory;
    }

    public List<Feedback> getFeedbacksReceived() {
        return feedbacksReceived;
    }

    public void setFeedbacksReceived(List<Feedback> feedbacksReceived) {
        this.feedbacksReceived = feedbacksReceived;
    }

    public List<Review> getReviewsReceived() {
        return reviewsReceived;
    }

    public void setReviewsReceived(List<Review> reviewsReceived) {
        this.reviewsReceived = reviewsReceived;
    }

    public List<ArtistSchedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<ArtistSchedule> schedules) {
        this.schedules = schedules;
    }

    @JsonbTransient
    public List<TimeSlot> getTimeSlots() {
        return timeSlots;
    }

    public void setTimeSlots(List<TimeSlot> timeSlots) {
        this.timeSlots = timeSlots;
    }
    
    @JsonbTransient
@OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
private List<DesignFavourite> favourites;

@JsonbTransient
@OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
private List<DesignLike> likes;

public List<DesignFavourite> getFavourites() {
    return favourites;
}

public void setFavourites(List<DesignFavourite> favourites) {
    this.favourites = favourites;
}

public List<DesignLike> getLikes() {
    return likes;
}

public void setLikes(List<DesignLike> likes) {
    this.likes = likes;
}




}

package clientDTO;

import java.io.Serializable;

public class ArtistCardDTO implements Serializable {

    private Long artistId;
    private String fullName;
    private Integer yearsExperience;
    private Double averageRating;
    private Long totalReviews;

    // Optional (add later if needed)
    private String profileImage;

    // ===== GETTERS & SETTERS =====

    public String getInitial() {
    if (fullName == null || fullName.isEmpty()) {
        return "?";
    }
    return fullName.substring(0, 1).toUpperCase();
}

    public Long getArtistId() {
        return artistId;
    }

    public void setArtistId(Long artistId) {
        this.artistId = artistId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Integer getYearsExperience() {
        return yearsExperience;
    }

    public void setYearsExperience(Integer yearsExperience) {
        this.yearsExperience = yearsExperience;
    }

    public Double getAverageRating() {
        return averageRating != null
                ? Math.round(averageRating * 10.0) / 10.0
                : 0.0;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Long getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(Long totalReviews) {
        this.totalReviews = totalReviews;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}

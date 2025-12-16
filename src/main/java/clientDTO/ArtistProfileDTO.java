package clientDTO;

import clientDTO.ArtistDesignDTO;
import java.io.Serializable;
import java.util.List;

public class ArtistProfileDTO implements Serializable {

    public String getInitial() {
    if (fullName != null && !fullName.isEmpty()) {
        return fullName.substring(0, 1).toUpperCase();
    }
    return "?";
}

    private Long artistId;
    private String fullName;
    private String email;
    private String phone;
    private Integer yearsExperience;

    private Double averageRating;
    private Long totalReviews;

    private List<ArtistDesignDTO> designs;

    // ================= GETTERS & SETTERS =================

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getYearsExperience() {
        return yearsExperience;
    }

    public void setYearsExperience(Integer yearsExperience) {
        this.yearsExperience = yearsExperience;
    }

    public Double getAverageRating() {
        return averageRating;
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

    public List<ArtistDesignDTO> getDesigns() {
        return designs;
    }

    public void setDesigns(List<ArtistDesignDTO> designs) {
        this.designs = designs;
    }
}

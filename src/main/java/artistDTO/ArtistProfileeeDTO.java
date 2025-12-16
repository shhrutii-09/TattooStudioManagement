package artistDTO;

import java.io.Serializable;

public class ArtistProfileeeDTO implements Serializable {

    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String role;        // read-only
    private boolean verified;   // read-only

    // ---- Experience ----
    private Integer experienceId;
    private String portfolioLink;
    private String specialties;
    private Integer yearsExperience;
    private String status;

    // ---------- getters & setters ----------

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public Integer getExperienceId() { return experienceId; }
    public void setExperienceId(Integer experienceId) { this.experienceId = experienceId; }

    public String getPortfolioLink() { return portfolioLink; }
    public void setPortfolioLink(String portfolioLink) { this.portfolioLink = portfolioLink; }

    public String getSpecialties() { return specialties; }
    public void setSpecialties(String specialties) { this.specialties = specialties; }

    public Integer getYearsExperience() { return yearsExperience; }
    public void setYearsExperience(Integer yearsExperience) { this.yearsExperience = yearsExperience; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

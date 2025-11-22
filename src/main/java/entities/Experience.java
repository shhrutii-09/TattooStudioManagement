package entities;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "experience")
public class Experience implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EXPERIENCEID")
    private Integer experienceId;

    @Column(name = "PORTFOLIOLINK", length = 500)
    private String portfolioLink;

    @Column(name = "SPECIALTIES", length = 500)
    private String specialties;

    @Column(name = "STATUS", length = 50)
    private String status;

    @Column(name = "YEARSEXPERIENCE")
    private Integer yearsExperience;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARTIST_ID", nullable = false, unique = true)
    private AppUser artist;

    public Experience() {}

    public Integer getExperienceId() { return experienceId; }
    public void setExperienceId(Integer experienceId) { this.experienceId = experienceId; }

    public String getPortfolioLink() { return portfolioLink; }
    public void setPortfolioLink(String portfolioLink) { this.portfolioLink = portfolioLink; }

    public String getSpecialties() { return specialties; }
    public void setSpecialties(String specialties) { this.specialties = specialties; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getYearsExperience() { return yearsExperience; }
    public void setYearsExperience(Integer yearsExperience) { this.yearsExperience = yearsExperience; }

    public AppUser getArtist() { return artist; }
    public void setArtist(AppUser artist) { this.artist = artist; }
}
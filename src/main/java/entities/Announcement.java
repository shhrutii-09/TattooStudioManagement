package entities;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "announcement")
public class Announcement implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ANNOUNCEMENTID")
    private Long announcementId;

    @Column(name = "MESSAGE", columnDefinition = "TEXT")
    private String message;

    @Column(name = "POSTEDAT")
    private LocalDateTime postedAt;

    @Column(name = "TARGETROLE", nullable = false, length = 100)
    private String targetRole;

    @Column(name = "TITLE", nullable = false, length = 150)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "POSTED_BY", nullable = false)
    private AppUser postedBy;

    public Announcement() { }

    // Getters & setters
    public Long getAnnouncementId() { return announcementId; }
    public void setAnnouncementId(Long announcementId) { this.announcementId = announcementId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getPostedAt() { return postedAt; }
    public void setPostedAt(LocalDateTime postedAt) { this.postedAt = postedAt; }

    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public AppUser getPostedBy() { return postedBy; }
    public void setPostedBy(AppUser postedBy) { this.postedBy = postedBy; }
}
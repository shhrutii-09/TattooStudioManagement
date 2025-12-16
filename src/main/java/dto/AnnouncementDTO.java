// file: dto/AnnouncementDTO.java
package dto;

import java.time.LocalDateTime;

public class AnnouncementDTO {
    private Long announcementId;
    private String title;
    private String message;
    private String targetRole; // "ALL", "CLIENT", "ARTIST", "ADMIN"
    private LocalDateTime postedAt;
    private Long postedById;
    private String postedByName;
    
    // Constructors
    public AnnouncementDTO() {}
    
    public AnnouncementDTO(Long announcementId, String title, String message, 
                          String targetRole, LocalDateTime postedAt, 
                          Long postedById, String postedByName) {
        this.announcementId = announcementId;
        this.title = title;
        this.message = message;
        this.targetRole = targetRole;
        this.postedAt = postedAt;
        this.postedById = postedById;
        this.postedByName = postedByName;
    }
    
    // Getters and Setters
    public Long getAnnouncementId() { return announcementId; }
    public void setAnnouncementId(Long announcementId) { this.announcementId = announcementId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
    
    public LocalDateTime getPostedAt() { return postedAt; }
    public void setPostedAt(LocalDateTime postedAt) { this.postedAt = postedAt; }
    
    public Long getPostedById() { return postedById; }
    public void setPostedById(Long postedById) { this.postedById = postedById; }
    
    public String getPostedByName() { return postedByName; }
    public void setPostedByName(String postedByName) { this.postedByName = postedByName; }
    
    // Helper methods for UI
    public String getPostedDateFormatted() {
        if (postedAt == null) return "";
        return postedAt.toLocalDate().toString();
    }
    
    public String getPostedTimeFormatted() {
        if (postedAt == null) return "";
        return postedAt.toLocalTime().toString().substring(0, 5);
    }
    
    public String getTargetRoleLabel() {
        if (targetRole == null) return "";
        switch (targetRole) {
            case "ALL": return "All Users";
            case "CLIENT": return "Clients";
            case "ARTIST": return "Artists";
            case "ADMIN": return "Admins";
            default: return targetRole;
        }
    }
}
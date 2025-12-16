// file: dto/AnnouncementFilterDTO.java
package dto;

import java.time.LocalDate;

public class AnnouncementFilterDTO {
    private String title;
    private String message;
    private String targetRole;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long postedBy;
    private int page = 0;
    private int size = 10;
    
    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public Long getPostedBy() { return postedBy; }
    public void setPostedBy(Long postedBy) { this.postedBy = postedBy; }
    
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    
    // Helper methods
    public boolean hasFilters() {
        return (title != null && !title.isEmpty()) ||
               (message != null && !message.isEmpty()) ||
               (targetRole != null && !targetRole.isEmpty() && !targetRole.equals("ALL")) ||
               (startDate != null) ||
               (endDate != null) ||
               (postedBy != null);
    }
    
    public void clearFilters() {
        title = null;
        message = null;
        targetRole = null;
        startDate = null;
        endDate = null;
        postedBy = null;
        page = 0;
    }
}
package artistDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DesignListingDTO {
    
    private Long designId;
    private String title;
    private String description;
    private String style;
    private BigDecimal price;
    private String imagePath;
    
    // Status flags
    private boolean isBanned;
    private String bannedReason;
    
    // Calculated Metrics
    private int totalLikes;
    private int totalFavourites;
    
    private LocalDateTime uploadedAt;

    // Getters and Setters
    public Long getDesignId() { return designId; }
    public void setDesignId(Long designId) { this.designId = designId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public boolean isBanned() { return isBanned; }
    public void setBanned(boolean banned) { isBanned = banned; }
    public String getBannedReason() { return bannedReason; }
    public void setBannedReason(String bannedReason) { this.bannedReason = bannedReason; }
    public int getTotalLikes() { return totalLikes; }
    public void setTotalLikes(int totalLikes) { this.totalLikes = totalLikes; }
    public int getTotalFavourites() { return totalFavourites; }
    public void setTotalFavourites(int totalFavourites) { this.totalFavourites = totalFavourites; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
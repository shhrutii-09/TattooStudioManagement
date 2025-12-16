package dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AdminDesignDTO {
    private Long designId;
    private String title;
    private String style;
    private BigDecimal price;
    private String imageUrl;
    private boolean isBanned;
    private boolean isRemovedByArtist;
    private LocalDateTime uploadedAt;

    private Long artistId;
    private String artistName;

    // getters + setters...

    public Long getDesignId() {
        return designId;
    }

    public void setDesignId(Long designId) {
        this.designId = designId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isIsBanned() {
        return isBanned;
    }

    public void setIsBanned(boolean isBanned) {
        this.isBanned = isBanned;
    }

    public boolean isIsRemovedByArtist() {
        return isRemovedByArtist;
    }

    public void setIsRemovedByArtist(boolean isRemovedByArtist) {
        this.isRemovedByArtist = isRemovedByArtist;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Long getArtistId() {
        return artistId;
    }

    public void setArtistId(Long artistId) {
        this.artistId = artistId;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }
}

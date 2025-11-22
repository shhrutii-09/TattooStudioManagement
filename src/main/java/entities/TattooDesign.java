package entities;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tattoo_design")
public class TattooDesign implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DESIGNID")
    private Long designId;

    // avoid recursion: do not attempt to serialize the whole artist here
    @JsonbTransient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARTIST_ID", nullable = false)
    private AppUser artist;

    @Column(name = "TITLE", nullable = false, length = 150)
    private String title;

    @Column(name = "DESCRIPTION", columnDefinition = "TEXT")
    private String description;

    @Column(name = "IMAGEPATH", length = 500)
    private String imagePath; // Stores the file name/relative path

    @Column(name = "STYLE", length = 80)
    private String style;

    @Column(name = "PRICE", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "UPLOADEDAT")
    private LocalDateTime uploadedAt = LocalDateTime.now();
    
//    @Column(name = "image_path")
//    private String imagePath;
//
//public String getImagePath() { return imagePath; }
//public void setImagePath(String imagePath) { this.imagePath = imagePath; }


//    @JsonbTransient
//    @OneToMany(mappedBy = "design")
//    private List<DesignLike> likes;
//
//    
//    @OneToMany(mappedBy = "design")
//    private List<DesignFavourite> favourites;

    @JsonbTransient
    @OneToMany(mappedBy = "design", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DesignLike> likes;
    
    @JsonbTransient
    @OneToMany(mappedBy = "design", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DesignFavourite> favourites;
    
    // --- NEW FIELD FOR FILE TRANSFER ---
    @Transient // JPA ignores this field
    // @JsonbTransient // Optional: if you don't want to send this field back in responses
    private String imageDataBase64; 
    // -----------------------------------

    // Constructors
    public TattooDesign() {}

    // Getters and Setters

    public Long getDesignId() { return designId; }
    public void setDesignId(Long designId) { this.designId = designId; }

    public AppUser getArtist() { return artist; }
    public void setArtist(AppUser artist) { this.artist = artist; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public List<DesignLike> getLikes() { return likes; }
    public void setLikes(List<DesignLike> likes) { this.likes = likes; }

    public List<DesignFavourite> getFavourites() { return favourites; }
    public void setFavourites(List<DesignFavourite> favourites) { this.favourites = favourites; }
    
    // Getter/Setter for new transient field
    public String getImageDataBase64() {
        return imageDataBase64;
    }

    public void setImageDataBase64(String imageDataBase64) {
        this.imageDataBase64 = imageDataBase64;
    }
}
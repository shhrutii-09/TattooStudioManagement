package entities;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "design_favourite",
        uniqueConstraints = @UniqueConstraint(columnNames = {"CLIENT_ID", "DESIGN_ID"}))
public class DesignFavourite implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FAVID")
    private Integer favId;

    @Column(name = "FAVORITEDAT")
    private LocalDateTime favoritedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLIENT_ID", nullable = false)
    private AppUser client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DESIGN_ID", nullable = false)
    private TattooDesign design;

    public DesignFavourite() {}

    public Integer getFavId() { return favId; }
    public void setFavId(Integer favId) { this.favId = favId; }

    public LocalDateTime getFavoritedAt() { return favoritedAt; }
    public void setFavoritedAt(LocalDateTime favoritedAt) { this.favoritedAt = favoritedAt; }

    public AppUser getClient() { return client; }
    public void setClient(AppUser client) { this.client = client; }

    public TattooDesign getDesign() { return design; }
    public void setDesign(TattooDesign design) { this.design = design; }
}
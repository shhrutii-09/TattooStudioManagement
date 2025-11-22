package entities;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "design_like",
        uniqueConstraints = @UniqueConstraint(columnNames = {"CLIENT_ID", "DESIGN_ID"}))
public class DesignLike implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LIKEID")
    private Integer likeId;

    @Column(name = "LIKEDAT")
    private LocalDateTime likedAt = LocalDateTime.now();

    @JsonbTransient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLIENT_ID", nullable = false)
    private AppUser client;

    @JsonbTransient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DESIGN_ID", nullable = false)
    private TattooDesign design;

    public DesignLike() {}

    public Integer getLikeId() { return likeId; }
    public void setLikeId(Integer likeId) { this.likeId = likeId; }

    public LocalDateTime getLikedAt() { return likedAt; }
    public void setLikedAt(LocalDateTime likedAt) { this.likedAt = likedAt; }

    public AppUser getClient() { return client; }
    public void setClient(AppUser client) { this.client = client; }

    public TattooDesign getDesign() { return design; }
    public void setDesign(TattooDesign design) { this.design = design; }
}
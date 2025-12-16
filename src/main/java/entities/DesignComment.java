package entities;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity                     // THIS IS REQUIRED
@Table(name = "design_comments")
public class DesignComment implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long commentId;

    @ManyToOne
    @JoinColumn(name = "design_id")
    private TattooDesign design;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private AppUser client;

    private String text;

    private LocalDateTime createdAt;

    // getters + setters

    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    public TattooDesign getDesign() {
        return design;
    }

    public void setDesign(TattooDesign design) {
        this.design = design;
    }

    public AppUser getClient() {
        return client;
    }

    public void setClient(AppUser client) {
        this.client = client;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

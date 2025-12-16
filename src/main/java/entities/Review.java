package entities;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "review", 
    uniqueConstraints = {
        // CONSTRAINT: A client can only leave one generic review for a specific artist.
        // This is necessary if 'Review' is separate from 'Feedback' (post-appointment)
        @UniqueConstraint(columnNames = {"CLIENT_ID", "ARTIST_ID"}) 
    }
)
public class Review implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REVIEWID")
    private Integer reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLIENT_ID", nullable = false)
    private AppUser client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARTIST_ID", nullable = false)
    private AppUser artist;

    @Column(name = "RATING", nullable = false)
    private Double rating;

    @Column(name = "COMMENTS", length = 500)
    private String comments;

    @Column(name = "REVIEWDATE")
    private LocalDateTime reviewDate = LocalDateTime.now();

    public Review() {}

    public Integer getReviewId() { return reviewId; }
    public void setReviewId(Integer reviewId) { this.reviewId = reviewId; }

    public AppUser getClient() { return client; }
    public void setClient(AppUser client) { this.client = client; }

    public AppUser getArtist() { return artist; }
    public void setArtist(AppUser artist) { this.artist = artist; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public LocalDateTime getReviewDate() { return reviewDate; }
    public void setReviewDate(LocalDateTime reviewDate) { this.reviewDate = reviewDate; }
}
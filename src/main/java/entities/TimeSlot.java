package entities;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "time_slot")
public class TimeSlot implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SLOTID")
    private Integer slotId;

    @JsonbTransient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARTIST_ID", nullable = false)
    private AppUser artist;

    @Column(name = "STARTTIME", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "ENDTIME", nullable = false)
    private LocalDateTime endTime;

    // CRITICAL UPDATE: Status is now an Enum, persisted as a String in the DB
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 50)
    private TimeSlotStatus status; // AVAILABLE, BOOKED, BLOCKED, PENDING_APPOINTMENT

    @Column(name = "BLOCKREASON", length = 255)
    private String blockReason; 
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BLOCKED_BY_ADMIN_ID")
    private AppUser blockedBy;

    public TimeSlot() {}

    // Getters and setters 
    public Integer getSlotId() { return slotId; }
    public void setSlotId(Integer slotId) { this.slotId = slotId; }

    public AppUser getArtist() { return artist; }
    public void setArtist(AppUser artist) { this.artist = artist; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    // Getter/Setter now use TimeSlotStatus Enum
    public TimeSlotStatus getStatus() { return status; }
    public void setStatus(TimeSlotStatus status) { this.status = status; }
    
    public String getBlockReason() { return blockReason; }
    public void setBlockReason(String blockReason) { this.blockReason = blockReason; }

    public AppUser getBlockedBy() { return blockedBy; }
    public void setBlockedBy(AppUser blockedBy) { this.blockedBy = blockedBy; }
    
    public enum TimeSlotStatus {
    AVAILABLE,
    BOOKED,
    BLOCKED,
    PENDING_APPOINTMENT // Useful if an appointment requires artist confirmation
}
}
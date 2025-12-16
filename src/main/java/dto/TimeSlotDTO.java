// file: dto/TimeSlotDTO.java
package dto;

import entities.TimeSlot;
import java.time.LocalDateTime;

public class TimeSlotDTO {
    private Integer slotId;
    private Long artistId;
    private String artistName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status; // "AVAILABLE", "BOOKED", "BLOCKED", "PENDING_APPOINTMENT"
    private String blockReason;
    private Long blockedByAdminId;
    private String blockedByAdminName;
    private boolean isBooked;
    private boolean isBlocked;
    private boolean isAvailable;
    private Long appointmentId; // if booked

    // Constructors
    public TimeSlotDTO() {}

    // In TimeSlotDTO.java - Add this constructor
public TimeSlotDTO(Integer slotId, Long artistId, String artistName, 
                  LocalDateTime startTime, LocalDateTime endTime, 
                  TimeSlot.TimeSlotStatus status, String blockReason) {
    this.slotId = slotId;
    this.artistId = artistId;
    this.artistName = artistName;
    this.startTime = startTime;
    this.endTime = endTime;
    this.status = status != null ? status.name() : null;  // Convert enum to string
    this.blockReason = blockReason;
    this.isBooked = "BOOKED".equals(this.status);
    this.isBlocked = "BLOCKED".equals(this.status);
    this.isAvailable = "AVAILABLE".equals(this.status);
}

    // Getters and Setters
    public Integer getSlotId() { return slotId; }
    public void setSlotId(Integer slotId) { this.slotId = slotId; }

    public Long getArtistId() { return artistId; }
    public void setArtistId(Long artistId) { this.artistId = artistId; }

    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { 
        this.status = status;
        this.isBooked = "BOOKED".equals(status);
        this.isBlocked = "BLOCKED".equals(status);
        this.isAvailable = "AVAILABLE".equals(status);
    }

    public String getBlockReason() { return blockReason; }
    public void setBlockReason(String blockReason) { this.blockReason = blockReason; }

    public Long getBlockedByAdminId() { return blockedByAdminId; }
    public void setBlockedByAdminId(Long blockedByAdminId) { this.blockedByAdminId = blockedByAdminId; }

    public String getBlockedByAdminName() { return blockedByAdminName; }
    public void setBlockedByAdminName(String blockedByAdminName) { this.blockedByAdminName = blockedByAdminName; }

    public boolean isBooked() { return isBooked; }
    public void setBooked(boolean booked) { isBooked = booked; }

    public boolean isBlocked() { return isBlocked; }
    public void setBlocked(boolean blocked) { isBlocked = blocked; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
}
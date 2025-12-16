// file: dto/TimeSlotFilterDTO.java
package dto;

import java.time.LocalDate;

public class TimeSlotFilterDTO {
    private Long artistId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status; // "ALL", "AVAILABLE", "BOOKED", "BLOCKED"
    private Integer  page = 0;
    private Integer  size = 20; 

    // Getters and Setters
    public Long getArtistId() { return artistId; }
    public void setArtistId(Long artistId) { this.artistId = artistId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer  getPage() { return page; }
    public void setPage(Integer  page) { this.page = page; }

    public Integer  getSize() { return size; }
    public void setSize(Integer  size) { this.size = size; }
}
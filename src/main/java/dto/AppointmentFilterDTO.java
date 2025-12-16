// file: dto/AppointmentFilterDTO.java
package dto;

import java.io.Serializable;
import java.time.LocalDate;

public class AppointmentFilterDTO implements Serializable {
    private Long clientId;
    private String clientName;
    private Long artistId;
    private String artistName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status = "ALL"; // Default to show all
    private Integer page = 0;
    private Integer size = 10;
    
    // Constructors
    public AppointmentFilterDTO() {}
    
    // Getters and Setters
    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    
    public Long getArtistId() { return artistId; }
    public void setArtistId(Long artistId) { this.artistId = artistId; }
    
    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
    
    // Helper methods
    // In AppointmentFilterDTO.java
public boolean hasFilters() {
    return (clientId != null) || 
           (clientName != null && !clientName.trim().isEmpty()) ||
           (artistId != null) ||
           (artistName != null && !artistName.trim().isEmpty()) ||
           (startDate != null) ||
           (endDate != null) ||
           (status != null && !status.equals("ALL"));
}
}
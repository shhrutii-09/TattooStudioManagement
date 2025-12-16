// file: dto/SimpleAppointmentDTO.java
package dto;

import java.io.Serializable;

public class SimpleAppointmentDTO implements Serializable {
    private Long appointmentId;
    private String clientName;
    private String artistName;
    private String appointmentDate; // Store as String
    private String status;
    
    public SimpleAppointmentDTO() {}
    
    public SimpleAppointmentDTO(Long appointmentId, String clientName, 
                               String artistName, String appointmentDate, 
                               String status) {
        this.appointmentId = appointmentId;
        this.clientName = clientName;
        this.artistName = artistName;
        this.appointmentDate = appointmentDate;
        this.status = status;
    }
    
    // Getters and Setters
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    
    public String getArtistName() { return artistName; }
    public void setArtistName(String artistName) { this.artistName = artistName; }
    
    public String getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(String appointmentDate) { this.appointmentDate = appointmentDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
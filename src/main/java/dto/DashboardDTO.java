package dto;

import entities.Appointment;
import java.io.Serializable;
import java.util.List;

public class DashboardDTO implements Serializable {

    private long totalUsers;
    private long totalArtists;
    private long totalClients;
    private long todaysAppointments;

    private List<Appointment> recentAppointments;

    private double totalEarnings; // New field

    // New Getter
    public double getTotalEarnings() { 
        return totalEarnings; 
    }

    // New Setter
    public void setTotalEarnings(double totalEarnings) { 
        this.totalEarnings = totalEarnings; 
    }
    
    
    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getTotalArtists() { return totalArtists; }
    public void setTotalArtists(long totalArtists) { this.totalArtists = totalArtists; }

    public long getTotalClients() { return totalClients; }
    public void setTotalClients(long totalClients) { this.totalClients = totalClients; }

    public long getTodaysAppointments() { return todaysAppointments; }
    public void setTodaysAppointments(long todaysAppointments) { this.todaysAppointments = todaysAppointments; }

    public List<Appointment> getRecentAppointments() { return recentAppointments; }
    public void setRecentAppointments(List<Appointment> recentAppointments) { this.recentAppointments = recentAppointments; }
}

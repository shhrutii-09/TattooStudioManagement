package artistDTO;

import entities.Appointment;
import java.io.Serializable;
import java.util.List;

public class ArtistDashboardDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private long totalDesigns;
    private long todaysAppointments;
    private long upcomingAppointments;
    private long completedAppointments;

    private double averageRating;
    private double totalEarnings;

    // ✅ Keep recent appointments INSIDE the DTO
    private List<Appointment> recentAppointments;

    /* ---------------- GETTERS & SETTERS ---------------- */

    public long getTotalDesigns() {
        return totalDesigns;
    }

    public void setTotalDesigns(long totalDesigns) {
        this.totalDesigns = totalDesigns;
    }

    public long getTodaysAppointments() {
        return todaysAppointments;
    }

    public void setTodaysAppointments(long todaysAppointments) {
        this.todaysAppointments = todaysAppointments;
    }

    public long getUpcomingAppointments() {
        return upcomingAppointments;
    }

    public void setUpcomingAppointments(long upcomingAppointments) {
        this.upcomingAppointments = upcomingAppointments;
    }

    public long getCompletedAppointments() {
        return completedAppointments;
    }

    public void setCompletedAppointments(long completedAppointments) {
        this.completedAppointments = completedAppointments;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public double getTotalEarnings() {
        return totalEarnings;
    }

    public void setTotalEarnings(double totalEarnings) {
        this.totalEarnings = totalEarnings;
    }

    public List<Appointment> getRecentAppointments() {
        return recentAppointments;
    }

    public void setRecentAppointments(List<Appointment> recentAppointments) {
        this.recentAppointments = recentAppointments;
    }
}

package beans;

import dto.DashboardDTO;
import ejb.AdminEJBLocal;
import entities.Appointment;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.inject.Named;
import jakarta.faces.view.ViewScoped;

import java.io.Serializable;
import java.util.List;

@Named("adminDashboardBean")
@ViewScoped
public class AdminDashboardBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private AdminEJBLocal adminEJB;

    private DashboardDTO dashboard;
    private Appointment selectedAppointment;

    private boolean loading = true;
    private String error;

    @PostConstruct
    public void init() {
        dashboard = new DashboardDTO();
        try {
            loadStats();
            loadRecentAppointments();
        } catch (Exception ex) {
            error = "Failed to load dashboard: " + ex.getMessage();
        } finally {
            loading = false;
        }
    }

    private void loadStats() {
        dashboard.setTotalUsers(adminEJB.countTotalUsers());
        dashboard.setTotalArtists(adminEJB.countTotalArtists());
        dashboard.setTotalClients(adminEJB.countTotalClients());
        dashboard.setTodaysAppointments(adminEJB.countTodaysAppointments());
        
        dashboard.setTotalEarnings(adminEJB.calculateTotalEarnings());
    }

    private void loadRecentAppointments() {
        List<Appointment> list = adminEJB.listRecentAppointments(5);
        dashboard.setRecentAppointments(list);
    }

    public void refresh() {
        loading = true;
        try {
            loadStats();
            loadRecentAppointments();
            error = null;
        } catch (Exception ex) {
            error = "Refresh failed: " + ex.getMessage();
        } finally {
            loading = false;
        }
    }

    /** --------------------------
     * VIEW APPOINTMENT DETAILS
     * --------------------------*/
    public void viewAppointment(Appointment appt) {
        if (appt == null) return;
        this.selectedAppointment = adminEJB.getAppointment(appt.getAppointmentId());
    }

    // Getters
    public DashboardDTO getDashboard() { return dashboard; }
    public Appointment getSelectedAppointment() { return selectedAppointment; }
    public boolean isLoading() { return loading; }
    public String getError() { return error; }
}

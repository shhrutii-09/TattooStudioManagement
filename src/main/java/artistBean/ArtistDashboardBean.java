package artistBean;

import artistDTO.ArtistDashboardDTO;
import beans.UserSessionBean;
import ejb.ArtistEJBLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;

@Named("artistDashboardBean")
@ViewScoped
public class ArtistDashboardBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private ArtistEJBLocal artistEJB;

    @Inject
    private UserSessionBean userSessionBean;

    private ArtistDashboardDTO dashboard;
    private boolean loading = true;
    private String error;

    private Long artistId; // ✅ store only ID, not entity

    @PostConstruct
    public void init() {
        try {
            // 🔐 Session & role validation
            if (userSessionBean == null ||
                !userSessionBean.isLoggedIn() ||
                !userSessionBean.isArtist()) {

                redirectUnauthorized();
                return;
            }

            artistId = userSessionBean.getUserId();

            if (artistId == null) {
                redirectUnauthorized();
                return;
            }

            dashboard = new ArtistDashboardDTO();
            
            dashboard.setTodaysAppointments(
        // Now calling the new method that counts requests made today
        artistEJB.countPendingRequestsToday(artistId) 
    ); 

    dashboard.setUpcomingAppointments(
        // The existing countUpcomingAppointments should now only count CONFIRMED/PAID appointments after today
        artistEJB.countUpcomingAppointments(artistId)
    );
            loadStats();
            loadRecentAppointments();

        } catch (Exception ex) {
            ex.printStackTrace();
            error = "Failed to load artist dashboard.";
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, error, null));
        } finally {
            loading = false;
        }
    }

    private void loadStats() {
        dashboard.setTotalDesigns(
                artistEJB.countDesignsByArtist(artistId)
        );

        dashboard.setTodaysAppointments(
                artistEJB.countTodaysAppointments(artistId)
        );

        dashboard.setUpcomingAppointments(
                artistEJB.countUpcomingAppointments(artistId)
        );

        dashboard.setCompletedAppointments(
                artistEJB.countCompletedAppointments(artistId)
        );

        dashboard.setAverageRating(
                artistEJB.calculateAverageRating(artistId)
        );

        dashboard.setTotalEarnings(
                artistEJB.calculateTotalEarnings(artistId)
        );
    }

    private void loadRecentAppointments() {
        dashboard.setRecentAppointments(
                artistEJB.listRecentAppointmentsForArtist(artistId, 5)
        );
    }

    private void redirectUnauthorized() {
        try {
            FacesContext.getCurrentInstance()
                .getExternalContext()
                .redirect("/login.xhtml");
        } catch (Exception ignored) {
        }
    }

    /* ---------------- GETTERS ---------------- */

    public ArtistDashboardDTO getDashboard() {
        return dashboard;
    }

    public boolean isLoading() {
        return loading;
    }

    public String getError() {
        return error;
    }
}

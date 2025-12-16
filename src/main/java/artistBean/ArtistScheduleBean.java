package artistBean;

import beans.UserSessionBean;
import ejb.ArtistEJBLocal;
import entities.ArtistSchedule;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Named("artistScheduleBean")
@ViewScoped
public class ArtistScheduleBean implements Serializable {

    @EJB
    private ArtistEJBLocal artistEJB;

    @Inject
    private UserSessionBean userSessionBean;

    private List<ArtistSchedule> schedules;

    // ========================
    // INIT
    // ========================
    @PostConstruct
    public void init() {
        if (!userSessionBean.isLoggedIn() || !userSessionBean.isArtist()) {
            schedules = new ArrayList<>();
            return;
        }

        Long artistId = userSessionBean.getUserId();
        schedules = artistEJB.listSchedulesForArtist(artistId);

        ensureAllDaysExist();
    }

    // ========================
    // GETTERS
    // ========================
    public List<ArtistSchedule> getSchedules() {
        return schedules;
    }

    // ========================
    // ACTIONS
    // ========================
    public void saveSchedule(ArtistSchedule schedule) {

        if (!userSessionBean.isLoggedIn() || !userSessionBean.isArtist()) {
            return;
        }

        // Validation
        if (schedule.isWorking()) {
            if (schedule.getStartTime() == null || schedule.getEndTime() == null ||
                !schedule.getStartTime().isBefore(schedule.getEndTime())) {

                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Invalid Time",
                                "Start time must be before end time"));
                return;
            }
        } else {
            // OFF day
            schedule.setStartTime(null);
            schedule.setEndTime(null);
        }

        artistEJB.saveArtistSchedule(userSessionBean.getUserId(), schedule);

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Saved",
                        schedule.getDayOfWeek() + " schedule updated"));
    }

    // ========================
    // HELPERS
    // ========================
    private void ensureAllDaysExist() {

        if (schedules == null) {
            schedules = new ArrayList<>();
        }

        for (DayOfWeek day : DayOfWeek.values()) {
            boolean exists = schedules.stream()
                    .anyMatch(s -> s.getDayOfWeek() == day);

            if (!exists) {
                ArtistSchedule s = new ArtistSchedule();
                s.setDayOfWeek(day);
                s.setWorking(false);
                s.setStartTime(LocalTime.of(9, 0));
                s.setEndTime(LocalTime.of(18, 0));
                schedules.add(s);
            }
        }

        schedules.sort(Comparator.comparingInt(s -> s.getDayOfWeek().getValue()));
    }
}

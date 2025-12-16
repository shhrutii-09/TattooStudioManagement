package artistBean;

import artistDTO.ArtistAppointmentDTO;
import artistDTO.ArtistAppointmentFilterDTO;
import beans.UserSessionBean;
import ejb.ArtistEJBLocal;
import entities.TimeSlot;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Named("artistAppointmentBean")
@ViewScoped
public class ArtistAppointmentBean implements Serializable {

    @EJB
    private ArtistEJBLocal artistEJB;

    @Inject
    private UserSessionBean userSessionBean;

    private List<ArtistAppointmentDTO> appointments;
    private ArtistAppointmentDTO selectedAppointment;
    private ArtistAppointmentFilterDTO filter;
private LocalDate selectedDate;
    private List<TimeSlot> availableSlots;
    private Long selectedAppointmentId;
    private Integer selectedSlotId;

    private String cancellationReason;

    @PostConstruct
    public void init() {
        filter = new ArtistAppointmentFilterDTO();
        loadAppointments();
    }

    public void loadAppointments() {
        appointments = artistEJB.getAppointmentsForArtist(
            userSessionBean.getUserId(),
            filter
        );
    }

    public void confirmAppointment(ArtistAppointmentDTO dto) {
        artistEJB.confirmAppointment(
            dto.getAppointmentId(),
            userSessionBean.getUserId()
        );
        loadAppointments();
        info("Appointment confirmed");
    }

    public void prepareSlotAssign(ArtistAppointmentDTO dto) {
        if (availableSlots == null || availableSlots.isEmpty()) {
    FacesContext.getCurrentInstance().addMessage(
        null,
        new FacesMessage(
            FacesMessage.SEVERITY_WARN,
            "No Slots",
            "No available time slots found"
        )
    );
}
        this.selectedAppointmentId = dto.getAppointmentId();
        this.selectedSlotId = null;
        this.availableSlots =
            artistEJB.getAvailableSlotsForArtist(userSessionBean.getUserId());
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    // 3. ADD THE PUBLIC SETTER METHOD
    public void setSelectedDate(LocalDate selectedDate) {
        this.selectedDate = selectedDate;
    }
    
    public void assignSlot() {
        artistEJB.assignSlotToAppointment(
            selectedAppointmentId,
            selectedSlotId,
            userSessionBean.getUserId()
        );
        loadAppointments();
        info("Slot assigned");
    }

    public void prepareCancel(ArtistAppointmentDTO dto) {
        this.selectedAppointment = dto;
        this.cancellationReason = null;
    }

    public void cancelAppointment() {
        artistEJB.cancelAppointment(
            selectedAppointment.getAppointmentId(),
            cancellationReason,
            userSessionBean.getUserId()
        );
        loadAppointments();
        info("Appointment cancelled");
    }

    private void info(String msg) {
        FacesContext.getCurrentInstance().addMessage(
            null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Info", msg)
        );
    }

    // getters & setters (unchanged)

    public ArtistEJBLocal getArtistEJB() {
        return artistEJB;
    }

    public void setArtistEJB(ArtistEJBLocal artistEJB) {
        this.artistEJB = artistEJB;
    }

    public UserSessionBean getUserSessionBean() {
        return userSessionBean;
    }

    public void setUserSessionBean(UserSessionBean userSessionBean) {
        this.userSessionBean = userSessionBean;
    }

    public List getAppointments() {
        return appointments;
    }

    public void setAppointments(List appointments) {
        this.appointments = appointments;
    }

    public ArtistAppointmentDTO getSelectedAppointment() {
        return selectedAppointment;
    }

    public void setSelectedAppointment(ArtistAppointmentDTO selectedAppointment) {
        this.selectedAppointment = selectedAppointment;
    }

    public ArtistAppointmentFilterDTO getFilter() {
        return filter;
    }

    public void setFilter(ArtistAppointmentFilterDTO filter) {
        this.filter = filter;
    }

    public List getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(List availableSlots) {
        this.availableSlots = availableSlots;
    }

    public Long getSelectedAppointmentId() {
        return selectedAppointmentId;
    }

    public void setSelectedAppointmentId(Long selectedAppointmentId) {
        this.selectedAppointmentId = selectedAppointmentId;
    }

    public Integer getSelectedSlotId() {
        return selectedSlotId;
    }

    public void setSelectedSlotId(Integer selectedSlotId) {
        this.selectedSlotId = selectedSlotId;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
}

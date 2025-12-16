package mbclient;

import clientDTO.BookAppointmentDTO;
import ejb.ClientEJBLocal;
import entities.MedicalForm;
import entities.TimeSlot;
import beans.UserSessionBean;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Named("bookAppointmentBean")
@ViewScoped
public class BookAppointmentBean implements Serializable {

    @Inject
    private ClientEJBLocal clientEJB;

    @Inject
    private UserSessionBean userSession; // ✅ Inject user session

    private BookAppointmentDTO dto;
    private List<TimeSlot> availableSlots;

    private Long clientId;
//    private LocalDate selectedDate = LocalDate.now(); 

    @PostConstruct
    public void init() {
        if (!userSession.isLoggedIn()) {
            // Not logged in → redirect to login
            redirectToLogin();
            return;
        }

        clientId = userSession.getUserId(); // ✅ get clientId from session bean

        FacesContext ctx = FacesContext.getCurrentInstance();
        Map<String, String> params = ctx.getExternalContext().getRequestParameterMap();

        if (dto == null) {
            dto = new BookAppointmentDTO();
        }

        if (params.containsKey("designId") && params.containsKey("artistId")) {
            Long designId = Long.valueOf(params.get("designId"));
            Long artistId = Long.valueOf(params.get("artistId"));

            dto.setDesignId(designId);
            dto.setArtistId(artistId);

            var design = clientEJB.getDesignById(designId);
            if (design != null) {
                dto.setDesignTitle(design.getTitle());
                dto.setDesignImage(design.getImagePath());
                dto.setPrice(design.getPrice().doubleValue());
            }

            var artist = clientEJB.getArtistInfo(artistId);
            if (artist != null) {
                dto.setArtistName(artist.getFullName());
            }
        }
    }

    // Listener for date selection
//    public void onDateSelect() {
//        if (dto != null && dto.getArtistId() != null && selectedDate != null) {
//            availableSlots = clientEJB.listAvailableTimeSlots(dto.getArtistId(), selectedDate);
//        } else {
//            availableSlots = java.util.Collections.emptyList();
//        }
//    }

    public String submitBooking() {
        if (clientId == null || dto.getArtistId() == null || dto.getDesignId() == null) {
            // Defensive check
            FacesContext.getCurrentInstance().addMessage(null,
                new jakarta.faces.application.FacesMessage(
                    jakarta.faces.application.FacesMessage.SEVERITY_ERROR,
                    "Booking failed: Missing required data",
                    ""
                )
            );
            return null;
        }

        // 1️⃣ Book appointment
        Long appointmentId = clientEJB.bookAppointment(
            clientId,
            dto.getArtistId(),
            dto.getDesignId(),
            dto.getClientNote()
        );

        // 2️⃣ Save medical form
        MedicalForm form = new MedicalForm();
        form.setAllergyDetails(dto.getAllergyDetails());
        form.setHasAllergies(dto.getAllergyDetails() != null && !dto.getAllergyDetails().isBlank());
        form.setDiabetes(dto.getDiabetes());
        form.setHeartCondition(dto.getHeartCondition());
        form.setIsPregnant(dto.getIsPregnant());
        form.setIsMinor(!dto.isAbove18());
        form.setSubmittedAt(java.time.LocalDateTime.now());

        clientEJB.submitMedicalForm(clientId, appointmentId, form);

        // 3️⃣ Redirect to "my-appointments" page
        return "/web/client/my-appointments.xhtml?faces-redirect=true";
    }

    private void redirectToLogin() {
        try {
            FacesContext.getCurrentInstance()
                .getExternalContext()
                .redirect(FacesContext.getCurrentInstance()
                        .getExternalContext().getRequestContextPath() + "/web/login.xhtml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Disable submit if conditions not met
    public boolean isSubmitDisabled() {
        return !dto.isAbove18() || !dto.isConsentGiven();
    }

    // ---------------- Getters & Setters ----------------
    public BookAppointmentDTO getDto() { return dto; }
    public List<TimeSlot> getAvailableSlots() { return availableSlots; }
//    public LocalDate getSelectedDate() { return selectedDate; }
//    public void setSelectedDate(LocalDate selectedDate) { this.selectedDate = selectedDate; }
}

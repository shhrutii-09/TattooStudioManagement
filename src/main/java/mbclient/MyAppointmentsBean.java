package mbclient;

import ejb.ClientEJBLocal;
import entities.Appointment;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import beans.UserSessionBean;
import jakarta.inject.Inject;
import ejb.ClientPaymentEJB;
import jakarta.faces.application.FacesMessage;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import java.io.Serializable;
import java.time.LocalDateTime;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@Named("myAppointmentsBean")
@ViewScoped
public class MyAppointmentsBean implements Serializable {

    private Long clientId;
    private List<Appointment> appointments;
private Appointment lastPaidAppointment;

    
    // Assuming ClientEJBLocal has listClientAppointments and updateAppointment methods
    @EJB
    private ClientEJBLocal clientEJB;

    @EJB
private ClientPaymentEJB paymentEJB;

    @Inject
private UserSessionBean userSession;

//    @Inject
//private InvoiceService invoiceService;

    @PostConstruct
public void init() {

    if (userSession == null || !userSession.isLoggedIn()) {
        System.out.println("User not logged in — skipping appointments load");
        return;
    }

    // Client ID = logged-in user's ID
    clientId = userSession.getUserId();

    System.out.println("DEBUG clientId = " + clientId);

    appointments = clientEJB.listClientAppointments(clientId, 0, 50);

    System.out.println("DEBUG appointments size = " + appointments.size());

    autoCancelPendingPayments();
}

    
    // ----------------------------------------------------
    // Status helpers for UI (MAPS DIRECTLY TO YOUR 5 CASES)
    // ----------------------------------------------------

    /** Case 4: ✅ CONFIRMED + PAID (Status: Paid & Confirmed) */
    public boolean isPaidAndConfirmed(Appointment a) {
        // Status must be CONFIRMED AND a COMPLETED payment entity must exist.
        // We assume the backend process moves the status to CONFIRMED before payment is accepted, 
        // and then payment is recorded in the Payment table with status COMPLETED.
        return "CONFIRMED".equals(a.getStatus()) 
               && a.getPayment() != null 
               && "COMPLETED".equals(a.getPayment().getStatus());
    }
    
    /** Case 2: 🟢 CONFIRMED (Payment not done yet) */
    public boolean isConfirmedButNotPaid(Appointment a) {
        // Status must be CONFIRMED AND NOT paid (i.e., payment is null or not completed).
        return "CONFIRMED".equals(a.getStatus()) 
               && !isPaidAndConfirmed(a);
    }

    /** Case 1: 🟡 PENDING */
    public boolean isPending(Appointment a) {
        return "PENDING".equals(a.getStatus());
    }
    
    /** Case 5: 🏁 COMPLETED */
    public boolean isCompleted(Appointment a) {
        return "COMPLETED".equals(a.getStatus());
    }

    /** Case 3: 🔴 CANCELLED (Auto-cancelled or manually cancelled) */
    public boolean isCancelled(Appointment a) {
        return "CANCELLED".equals(a.getStatus());
    }
    
    // ----------------------------------------------------
    // Action Methods
    // ----------------------------------------------------
    
   public String payNow(Appointment appointment) {

    try {
        BigDecimal amount = calculateAmount(appointment);

        paymentEJB.makeMockPayment(
            appointment,
            appointment.getClient(),
            amount
        );
lastPaidAppointment = appointment;

        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(
                FacesMessage.SEVERITY_INFO,
                "Payment Successful",
                "Your appointment is now paid and confirmed."
            )
        );

    } catch (Exception e) {
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(
                FacesMessage.SEVERITY_ERROR,
                "Payment Failed",
                e.getMessage()
            )
        );
    }

    // Stay on same page
    return null;
}


    public void leaveFeedback(Appointment appointment) {
        // Placeholder for navigating or submitting feedback
        System.out.println("Directing to feedback for Appointment ID: " + appointment.getAppointmentId());
        // You may need to navigate to a specific feedback form or open a dialog
    }
    
    /**
     * Auto-cancel logic (Client-side fallback)
     * If appointment.status == CONFIRMED AND payment not done within X hours (or before slot time)
     * -> appointment.status = CANCELLED
     */
    private void autoCancelPendingPayments() {
        if (appointments == null) return;

        // Define the auto-cancel duration (e.g., 24 hours) or use the appointment time slot
        // For simplicity, we'll check if the appointment time has passed if it's confirmed but unpaid.
        
        for (Appointment a : appointments) {
            // Check Case 2: CONFIRMED but NOT paid
            if (isConfirmedButNotPaid(a)) { 
                
                // 💡 LOGIC IMPLEMENTATION: 
                // We are using the appointment time as the final deadline to avoid booking confusion.
                if (a.getAppointmentDateTime() != null && a.getAppointmentDateTime().isBefore(LocalDateTime.now())) {
                    // Perform the cancellation
                    a.setStatus("CANCELLED");
                    a.setCancellationReason("Auto-cancelled: Payment not completed before session time.");
                    
                    // Persist the change
                    // clientEJB.updateAppointment(a); // Ensure this method exists in your EJB
                }
            }
        }
    }

    public List<Appointment> getPaidAppointments() {
    return appointments.stream()
        .filter(this::isPaidAndConfirmed)
        .toList();
}

public List<Appointment> getUnpaidAppointments() {
    return appointments.stream()
        .filter(a -> !isPaidAndConfirmed(a))
        .toList();
}

    // ----------------------------------------------------
    // Getters & Setters
    // ----------------------------------------------------
    public List<Appointment> getAppointments() {
        return appointments;
    }

    public Long getClientId() {
        return clientId;
    }
 private BigDecimal calculateAmount(Appointment appt) {
    if (appt.getDesign() != null && appt.getDesign().getPrice() != null) {
        return appt.getDesign().getPrice();
    }
    return new BigDecimal("5000");
}
 
 public Appointment getLastPaidAppointment() {
    return lastPaidAppointment;
}

public void setLastPaidAppointment(Appointment lastPaidAppointment) {
    this.lastPaidAppointment = lastPaidAppointment;
}

public void downloadInvoice(Appointment appt) {
    try {
        String receipt =
            "InkFlow Studio - Payment Receipt\n\n" +
            "Appointment ID: " + appt.getAppointmentId() + "\n" +
            "Amount Paid: ₹" + appt.getPayment().getAmount() + "\n" +
            "Transaction ID: " + appt.getPayment().getTransactionId() + "\n" +
            "Status: PAID\n";

        byte[] pdfBytes = receipt.getBytes(StandardCharsets.UTF_8);

        FacesContext fc = FacesContext.getCurrentInstance();
        HttpServletResponse response =
            (HttpServletResponse) fc.getExternalContext().getResponse();

        response.reset();
        response.setContentType("application/pdf");
        response.setHeader(
            "Content-Disposition",
            "attachment; filename=receipt_" + appt.getAppointmentId() + ".pdf"
        );
        response.setContentLength(pdfBytes.length);

        response.getOutputStream().write(pdfBytes);
        response.getOutputStream().flush();
        fc.responseComplete();

    } catch (Exception e) {
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(
                FacesMessage.SEVERITY_ERROR,
                "Download failed",
                "Could not generate receipt"
            )
        );
    }
}
public boolean isPaid(Appointment a) {
    return a.getPayment() != null
           && "COMPLETED".equals(a.getPayment().getStatus());
}

public boolean isUpcomingPaid(Appointment a) {
    return isPaid(a)
        && a.getAppointmentDateTime() != null
        && a.getAppointmentDateTime().isAfter(LocalDateTime.now());
}

public boolean isPastPaid(Appointment a) {
    return isPaid(a)
        && a.getAppointmentDateTime() != null
        && a.getAppointmentDateTime().isBefore(LocalDateTime.now());
}

}
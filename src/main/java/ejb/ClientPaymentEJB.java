package ejb;

import entities.*;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Stateless
public class ClientPaymentEJB {

    @PersistenceContext
    private EntityManager em;

    public Payment makeMockPayment(Appointment appointment,
                                   AppUser client,
                                   BigDecimal amount) {

        // Prevent double payment
        if (appointment.getPayment() != null) {
            throw new IllegalStateException("Payment already done");
        }

        Payment payment = new Payment();
        payment.setAppointment(appointment);
        payment.setClient(client);
        payment.setAmount(amount);
        payment.setPaymentMethod("MOCK_CARD");
        payment.setTransactionId("MOCK-" + UUID.randomUUID());
        payment.setStatus("COMPLETED");
        payment.setPaymentDate(LocalDateTime.now());

        // Link both sides
        appointment.setPayment(payment);
        appointment.setStatus("PAID");

        em.persist(payment);
        em.merge(appointment);

        return payment;
    }
}

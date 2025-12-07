package rest;

import ejb.ClientEJBLocal;
import entities.*;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.math.BigDecimal;
import java.util.*;
import java.time.LocalDate;
import jakarta.annotation.security.RolesAllowed;
import jakarta.annotation.security.PermitAll;

/**
 * ClientREST - complete client API for Tattoo Studio Management
 *
 * Error format: { "error": "message" }
 */
@Path("/client")
@RolesAllowed({"ADMIN", "ARTIST", "CLIENT"})
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ClientRest {

    @Inject
    private ClientEJBLocal clientEJB;

    // -------------------------
    // Designs
    // -------------------------

    /**
     * GET /api/client/designs
     * pagination: offset, limit
     */
    @GET
    @PermitAll
    @Path("/designs")
    public Response listDesigns(@QueryParam("offset") @DefaultValue("0") int offset,
                                @QueryParam("limit") @DefaultValue("50") int limit) {
        try {
            List<TattooDesign> list = clientEJB.listDesigns(offset, limit);
            return Response.ok(list).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * GET /api/client/designs/search
     * optional query params:
     * q (keyword), style, minPrice, maxPrice, offset, limit
     */
    @GET
    @PermitAll
    @Path("/designs/search")
    public Response searchDesigns(@QueryParam("q") String q,
                                  @QueryParam("style") String style,
                                  @QueryParam("minPrice") String minPriceStr,
                                  @QueryParam("maxPrice") String maxPriceStr,
                                  @QueryParam("offset") @DefaultValue("0") int offset,
                                  @QueryParam("limit") @DefaultValue("50") int limit) {
        try {
            BigDecimal minPrice = null, maxPrice = null;
            if (minPriceStr != null && !minPriceStr.isBlank()) minPrice = new BigDecimal(minPriceStr);
            if (maxPriceStr != null && !maxPriceStr.isBlank()) maxPrice = new BigDecimal(maxPriceStr);

            List<TattooDesign> list = clientEJB.searchDesigns(q, style, minPrice, maxPrice, offset, limit);
            return Response.ok(list).build();
        } catch (NumberFormatException nfe) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", "Invalid price format")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * GET /api/client/designs/{id}
     * returns design if found else { "error": "Design not found" }
     *
     * Note: ClientEJBLocal doesn't expose a single getDesignById method,
     * so we fetch list and search — this keeps REST layer independent of EM.
     */
    @GET
    @PermitAll
    @Path("/designs/{id}")
    public Response getDesignById(@PathParam("id") Long id) {
        try {
            List<TattooDesign> list = clientEJB.listDesigns(0, 1000);

            TattooDesign design = list.stream()
                    .filter(d -> d.getDesignId() != null && d.getDesignId().equals(id))
                    .findFirst()
                    .orElse(null);

            if (design == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Design not found"))
                        .build();
            }

            return Response.ok(design).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }


    // -------------------------
    // Likes & Favourites
    // -------------------------

    /**
     * POST /api/client/designs/{designId}/like
     * body: { "clientId": 3 }
     */
    @POST
    @Path("/designs/{designId}/like")
    public Response likeDesign(@PathParam("designId") Long designId, Map<String, Object> body) {
        try {
            Long clientId = (body.get("clientId") != null) ? Long.parseLong(body.get("clientId").toString()) : null;
            if (clientId == null) return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "clientId required")).build();
            DesignLike like = clientEJB.likeDesign(clientId, designId);
            return Response.ok(like).build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", ex.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * POST /api/client/designs/{designId}/unlike
     * body: { "clientId": 3 }
     */
    @POST
    @Path("/designs/{designId}/unlike")
    public Response unlikeDesign(@PathParam("designId") Long designId, Map<String, Object> body) {
        try {
            Long clientId = (body.get("clientId") != null) ? Long.parseLong(body.get("clientId").toString()) : null;
            if (clientId == null) return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "clientId required")).build();
            clientEJB.unlikeDesign(clientId, designId);
            return Response.ok(Map.of("success", true)).build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", ex.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * POST /api/client/designs/{designId}/favourite
     * body: { "clientId": 3 }
     */
    @POST
    @Path("/designs/{designId}/favourite")
    public Response favouriteDesign(@PathParam("designId") Long designId, Map<String, Object> body) {
        try {
            Long clientId = (body.get("clientId") != null) ? Long.parseLong(body.get("clientId").toString()) : null;
            if (clientId == null) return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "clientId required")).build();
            DesignFavourite fav = clientEJB.favouriteDesign(clientId, designId);
            return Response.ok(fav).build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", ex.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * POST /api/client/designs/{designId}/unfavourite
     * body: { "clientId": 3 }
     */
    @POST
    @Path("/designs/{designId}/unfavourite")
    public Response unfavouriteDesign(@PathParam("designId") Long designId, Map<String, Object> body) {
        try {
            Long clientId = (body.get("clientId") != null) ? Long.parseLong(body.get("clientId").toString()) : null;
            if (clientId == null) return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "clientId required")).build();
            clientEJB.unfavouriteDesign(clientId, designId);
            return Response.ok(Map.of("success", true)).build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", ex.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * GET /api/client/favourites/{clientId}
     * list favourites for a client
     */
    @GET
    @Path("/favourites/{clientId}")
    public Response listFavourites(@PathParam("clientId") Long clientId,
                                   @QueryParam("offset") @DefaultValue("0") int offset,
                                   @QueryParam("limit") @DefaultValue("50") int limit) {
        try {
            List<DesignFavourite> list = clientEJB.listFavourites(clientId, offset, limit);
            return Response.ok(list).build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", ex.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    // -------------------------
    // Appointments & Slots
    // -------------------------

    /**
     * GET /api/client/slots/{slotId}/available
     * check availability
     */
    @GET
@Path("/slots/artist/{artistId}")
public Response getAvailableSlots(@PathParam("artistId") Long artistId, 
                                  @QueryParam("date") String dateStr) {
    try {
        LocalDate date = LocalDate.parse(dateStr);
        List<TimeSlot> availableSlots = clientEJB.listAvailableTimeSlots(artistId, date);
        return Response.ok(availableSlots).build();
        
    } catch (Exception ex) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Error fetching slots: " + ex.getMessage()))
                .build();
    }
}

    @GET
    @Path("/slots/{slotId}/available")
    public Response isSlotAvailable(@PathParam("slotId") Integer slotId) {
        try {
            boolean ok = clientEJB.isSlotAvailable(slotId);
            return Response.ok(Map.of("available", ok)).build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", ex.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * POST /api/client/appointments/book
     * body: { "clientId": 3, "artistId": 2, "designId": 5, "slotId": 10, "note": "..." }
     */
    @POST
@Path("/appointments/book")
public Response bookAppointment(Map<String, Object> data) {
    try {
        // Safe parsing of required fields
        Long clientId = Long.parseLong(data.get("clientId").toString());
        Long artistId = Long.parseLong(data.get("artistId").toString());
        Integer slotId = Integer.parseInt(data.get("slotId").toString());
        
        // Optional fields
        Long designId = data.get("designId") != null ? Long.parseLong(data.get("designId").toString()) : null;
        String clientNote = (String) data.getOrDefault("clientNote", "");

        Long appointmentId = clientEJB.bookAppointment(clientId, artistId, designId, slotId, clientNote);

        return Response.status(Response.Status.CREATED)
                .entity(Map.of("appointmentId", appointmentId, "message", "Appointment booked successfully (PENDING)."))
                .build();

    } catch (IllegalStateException | IllegalArgumentException ex) {
        // Catches the CRITICAL double-booking failure from the EJB (IllegalStateException)
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", ex.getMessage()))
                .build();
    } catch (Exception ex) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Failed to book appointment: " + ex.getMessage()))
                .build();
    }
}

    /**
     * POST /api/client/appointments/{id}/cancel
     * body: { "reason": "..." }
     */
    @POST
    @Path("/appointments/{id}/cancel")
    public Response cancelAppointment(@PathParam("id") Long appointmentId, Map<String, Object> body) {
        try {
            String reason = body.getOrDefault("reason", "").toString();
            clientEJB.cancelAppointment(appointmentId, reason);
            return Response.ok(Map.of("success", true)).build();
        } catch (IllegalArgumentException iae) {
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", iae.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * GET /api/client/appointments/{clientId}
     * list appointments for a client
     */
    @GET
    @Path("/appointments/{clientId}")
    public Response listClientAppointments(@PathParam("clientId") Long clientId,
                                           @QueryParam("offset") @DefaultValue("0") int offset,
                                           @QueryParam("limit") @DefaultValue("50") int limit) {
        try {
            List<Appointment> list = clientEJB.listClientAppointments(clientId, offset, limit);
            return Response.ok(list).build();
        } catch (IllegalArgumentException iae) {
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", iae.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * GET /api/client/appointment/{id}
     */
    @GET
    @Path("/appointment/{id}")
    public Response getAppointment(@PathParam("id") Long id) {
        try {
            Appointment a = clientEJB.getAppointment(id);
            return Response.ok(a).build();
        } catch (IllegalArgumentException iae) {
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", iae.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    // -------------------------
    // Medical / Consent
    // -------------------------

    /**
     * POST /api/client/appointments/{appointmentId}/medical
     * body: MedicalForm JSON (all fields supported)
     */
    @POST
    @Path("/appointments/{appointmentId}/medical")
    public Response submitMedicalForm(@PathParam("appointmentId") Long appointmentId, MedicalForm form) {
        try {
            // form must contain client in body? Prefer clientId in nested appointment->client or in request body
            // We expect clientId as form.getClient().getUserId() OR caller will include clientId field in form.client
            Long clientId = (form.getClient() != null) ? form.getClient().getUserId() : null;
            if (clientId == null) return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "clientId required in medical form")).build();

            MedicalForm saved = clientEJB.submitMedicalForm(clientId, appointmentId, form);
            return Response.ok(saved).build();
        } catch (IllegalArgumentException iae) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", iae.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * GET /api/client/appointments/{appointmentId}/medical
     */
    @GET
    @Path("/appointments/{appointmentId}/medical")
    public Response getMedicalForm(@PathParam("appointmentId") Long appointmentId) {
        try {
            MedicalForm m = clientEJB.getMedicalFormForAppointment(appointmentId);
            if (m == null) return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", "Medical form not found")).build();
            return Response.ok(m).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    // -------------------------
    // Payments (mock)
    // -------------------------

    /**
     * POST /api/client/payments
     * body: { "clientId": 3, "appointmentId": 12, "amount": 1500.00, "method": "CASH" }
     */
    @POST
    @Path("/payments")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createPayment(Map<String, Object> paymentPayload) {
        try {
            // Data extraction and validation
            Long clientId = Long.parseLong(paymentPayload.get("clientId").toString());
            Long appointmentId = Long.parseLong(paymentPayload.get("appointmentId").toString());
            BigDecimal amount = new BigDecimal(paymentPayload.get("amount").toString());
            String method = paymentPayload.get("method").toString();
            
            // Call EJB logic
            Payment payment = clientEJB.createMockPayment(clientId, appointmentId, amount, method);
            
            // The response should now succeed due to the @JsonbTransient fix
            return Response.ok(payment).build(); 
        } catch (NumberFormatException | NullPointerException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", "Invalid payment data format: " + ex.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(Map.of("error", "Payment processing failed: " + e.getMessage())).build();
        }
    }
    /**
     * POST /api/client/payments/{paymentId}/status
     * body: { "status": "COMPLETED" }
     */
    @POST
    @Path("/payments/{paymentId}/status")
    public Response updatePaymentStatus(@PathParam("paymentId") Integer paymentId, Map<String, Object> body) {
        try {
            String status = body.get("status") != null ? body.get("status").toString() : null;
            if (status == null) return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "status required")).build();
            clientEJB.updatePaymentStatus(paymentId, status);
            return Response.ok(Map.of("success", true)).build();
        } catch (IllegalArgumentException iae) {
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", iae.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * GET /api/client/payments/appointment/{appointmentId}
     */
    @GET
    @Path("/payments/appointment/{appointmentId}")
    public Response getPaymentByAppointment(@PathParam("appointmentId") Long appointmentId) {
        try {
            Payment p = clientEJB.getPaymentByAppointment(appointmentId);
            if (p == null) return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", "Payment not found")).build();
            return Response.ok(p).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * GET /api/client/payments/{clientId}
     */
    @GET
    @Path("/payments/{clientId}")
    public Response listClientPayments(@PathParam("clientId") Long clientId,
                                       @QueryParam("offset") @DefaultValue("0") int offset,
                                       @QueryParam("limit") @DefaultValue("50") int limit) {
        try {
            List<Payment> list = clientEJB.listClientPayments(clientId, offset, limit);
            return Response.ok(list).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    // -------------------------
    // Feedback & Ratings
    // -------------------------

    /**
     * POST /api/client/appointments/{appointmentId}/feedback
     * body: { "clientId": 3, "rating": 5, "comment": "..." }
     *
     * Per your choice: rating allowed ONLY after appointment is COMPLETED
     */
    @POST
    @Path("/appointments/{appointmentId}/feedback")
    public Response submitFeedback(@PathParam("appointmentId") Long appointmentId, Map<String, Object> body) {
        try {
            Long clientId = body.get("clientId") != null ? Long.parseLong(body.get("clientId").toString()) : null;
            Integer rating = body.get("rating") != null ? Integer.parseInt(body.get("rating").toString()) : null;
            String comment = body.getOrDefault("comment", "").toString();

            if (clientId == null || rating == null)
                return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "clientId and rating required")).build();

            Feedback f = clientEJB.submitFeedback(clientId, appointmentId, rating, comment);
            return Response.ok(f).build();

        } catch (IllegalStateException ise) {
            return Response.status(Response.Status.CONFLICT).entity(Map.of("error", ise.getMessage())).build();
        } catch (IllegalArgumentException iae) {
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", iae.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    // -------------------------
    // Profile & Utility
    // -------------------------

    /**
     * GET /api/client/{clientId}
     */
    @GET
    @Path("/{clientId}")
    public Response getClient(@PathParam("clientId") Long clientId) {
        try {
            AppUser client = clientEJB.getClientById(clientId);
            return Response.ok(client).build();
        } catch (IllegalArgumentException iae) {
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", iae.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * PUT /api/client/{clientId}
     * body: { "fullName": "...", "phone": "..." }
     */
    @PUT
    @Path("/{clientId}")
    public Response updateClientProfile(@PathParam("clientId") Long clientId, Map<String, Object> body) {
        try {
            String fullName = body.getOrDefault("fullName", null) != null ? body.get("fullName").toString() : null;
            String phone = body.getOrDefault("phone", null) != null ? body.get("phone").toString() : null;
            AppUser updated = clientEJB.updateClientProfile(clientId, fullName, phone);
            return Response.ok(updated).build();
        } catch (IllegalArgumentException iae) {
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", iae.getMessage())).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("error", e.getMessage())).build();
        }
    }
}

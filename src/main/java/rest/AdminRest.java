package rest;

import ejb.AdminEJBLocal;
import entities.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import jakarta.annotation.security.RolesAllowed; // <-- Ensure this is imported!
import jakarta.ws.rs.core.SecurityContext;

@Path("/admin")
//@RolesAllowed({"ADMIN"})
@RolesAllowed("ADMIN")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminRest {

    
    
    
    @EJB
    private AdminEJBLocal adminEJB;
    @Context private SecurityContext securityContext;

    // Helper to safely get the original business message from a potentially wrapped exception
    private String getBusinessMessage(Exception e) {
        Throwable cause = e.getCause();
        // Check for EJBException wrapper and return its cause message, otherwise return the current message
        return cause != null && cause.getMessage() != null ? cause.getMessage() : e.getMessage();
    }

    // The getRequiredAdminId helper method has been removed as it was not used/defined.
    
    // -----------------------
    // User Management
    // -----------------------
    @GET
    @Path("/users")
    public Response listAllUsers() {
        try {
            List<AppUser> users = adminEJB.listAllUsers();
            return Response.ok(users).build(); // sends JSON array
        } catch (Exception e) {
            // Catches unexpected internal errors
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(Map.of("message", "Error fetching users: " + getBusinessMessage(e))).build();
        }
    }

    @GET
    @Path("/users/{id}")
    public Response getUser(@PathParam("id") Long userId) {
        try {
            return Response.ok(adminEJB.getUserById(userId)).build();
        } catch (Exception ex) {
            // Catching generic Exception ensures the EJB container wrapper doesn't prevent handling.
            String message = getBusinessMessage(ex);
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("message", message))
                           .build();
        }
    }

    @PUT
    @Path("/users/{id}/deactivate")
    public Response deactivateUser(@PathParam("id") Long userId, 
                                   Map<String, Object> data) { // Removed @HeaderParam
        try {
            // 1. Authenticate and validate the Admin performing the action - REMOVED
//
            // 2. Safely extract required fields from JSON body
            Object deactivateObj = data.get("deactivate");
            String reason = (String) data.get("reason");
            
            if (deactivateObj == null) {
                 return Response.status(Response.Status.BAD_REQUEST)
                                .entity(Map.of("message", "The 'deactivate' field (boolean) is required in the body."))
                                .build();
            }
            
            // Explicitly cast to Boolean (using map.get(key) often returns Boolean for JSON 'true/false')
            Boolean deactivate;
            if (deactivateObj instanceof Boolean) {
                deactivate = (Boolean) deactivateObj;
            } else if (deactivateObj instanceof String) {
                deactivate = Boolean.parseBoolean((String) deactivateObj);
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                               .entity(Map.of("message", "The 'deactivate' field must be a boolean (true or false)."))
                               .build();
            }

            // 3. Execute EJB logic
            adminEJB.deactivateUser(userId, deactivate, reason);
            
            String message = deactivate ? "User deactivated successfully." : "User activated successfully.";
            return Response.ok(Map.of("success", true, "message", message)).build();
            
        } catch (IllegalArgumentException ex) {
            // Handles User Not Found from EJB (404)
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("message", getBusinessMessage(ex)))
                           .build();
        } catch (Exception ex) {
            // Handles unexpected exceptions
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("message", "Invalid request data or unexpected error: " + getBusinessMessage(ex)))
                           .build();
        }
    }
@PUT
@Path("/artists/{id}/verify")
public Response verifyArtist(@PathParam("id") Long artistId, 
                             Map<String, Object> data) { // Removed @HeaderParam
    try {
        // Must extract adminId from body for EJB logic validation
        Long adminId = data.get("adminId") != null ? Long.parseLong(data.get("adminId").toString()) : null;
        
        Boolean verify = (Boolean) data.get("verify");
        if (verify == null) {
             return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("message", "The 'verify' field (boolean) is required in the body."))
                            .build();
        }
        
        adminEJB.verifyArtist(artistId, verify, adminId); // Pass adminId
        
        String message = verify ? "Artist verified successfully." : "Artist unverified successfully.";
        return Response.ok(Map.of("success", true, "message", message)).build();
        
    } catch (IllegalArgumentException ex) {
        return Response.status(Response.Status.NOT_FOUND)
                       .entity(Map.of("message", getBusinessMessage(ex)))
                       .build();
    } catch (Exception ex) {
        return Response.status(Response.Status.BAD_REQUEST)
                       .entity(Map.of("message", "Invalid request data: " + getBusinessMessage(ex)))
                       .build();
    }
}

    // -----------------------
    // Announcements
    // -----------------------
    @POST
    @Path("/announcements")
    public Response createAnnouncement(Map<String, Object> data) {
        try {
            Long adminId = Long.parseLong(data.get("adminId").toString());
            String title = data.get("title").toString();
            String message = data.get("message").toString();
            String targetRole = data.get("targetRole").toString();
            Announcement a = adminEJB.createAnnouncement(adminId, title, message, targetRole);
            return Response.ok(a).build();
        } catch (Exception ex) {
            // Catches parsing errors or 'Admin not found' from EJB (which is wrapped)
            String message = getBusinessMessage(ex);
            return Response.status(message.contains("not found") ? Response.Status.NOT_FOUND : Response.Status.BAD_REQUEST)
                           .entity(Map.of("message", message))
                           .build();
        }
    }

    @PUT
    @Path("/announcements/{id}")
    public Response updateAnnouncement(@PathParam("id") Long id, Map<String, Object> data) {
        try {
            String title = data.get("title").toString();
            String message = data.get("message").toString();
            String targetRole = data.get("targetRole").toString();
            Announcement a = adminEJB.updateAnnouncement(id, title, message, targetRole);
            return Response.ok(a).build();
        } catch (Exception ex) {
            // Catching generic Exception for wrapped IllegalArgumentException (Not Found)
            String message = getBusinessMessage(ex);
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("message", message))
                           .build();
        }
    }

    @DELETE
    @Path("/announcements/{id}")
    public Response deleteAnnouncement(@PathParam("id") Long id) {
        try {
            adminEJB.deleteAnnouncement(id);
            return Response.ok(Map.of("success", true, "message", "Announcement deleted.")).build();
        } catch (Exception ex) {
            // Catching generic Exception for wrapped IllegalArgumentException (Not Found)
            String message = getBusinessMessage(ex);
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("message", message))
                           .build();
        }
    }

    // -----------------------
    // Tattoo Designs
    // -----------------------
    @GET
    @Path("/designs")
    public Response listDesigns(@QueryParam("offset") @DefaultValue("0") int offset,
                                @QueryParam("limit") @DefaultValue("50") int limit) {
        List<TattooDesign> designs = adminEJB.listDesigns(offset, limit);
        return Response.ok(designs).build();
    }

    @DELETE
@Path("/designs/{id}")
public Response deleteDesign(@PathParam("id") Long designId) {
    try {
        adminEJB.deleteDesign(designId); // void
        return Response.ok(Map.of("success", true, "message", "Design deleted successfully.")).build();
    } catch (IllegalArgumentException ex) {
        return Response.status(Response.Status.NOT_FOUND)
                       .entity(Map.of("error", ex.getMessage()))
                       .build();
    } catch (Exception ex) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                       .entity(Map.of("error", ex.getMessage()))
                       .build();
    }
}


    // -----------------------
    // Appointments
    // -----------------------
    @GET
    @Path("/appointments")
    public Response listAppointments(@QueryParam("offset") @DefaultValue("0") int offset,
                                     @QueryParam("limit") @DefaultValue("50") int limit) {
        return Response.ok(adminEJB.listAllAppointments(offset, limit)).build();
    }

    @GET
    @Path("/appointments/{id}")
    public Response getAppointment(@PathParam("id") Long id) {
        try {
            return Response.ok(adminEJB.getAppointment(id)).build();
        } catch (Exception ex) {
            // Catching generic Exception for wrapped IllegalArgumentException (Not Found)
            String message = getBusinessMessage(ex);
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("message", message))
                           .build();
        }
    }

    @POST
    @Path("/appointments/{id}/status")
    public Response changeAppointmentStatus(@PathParam("id") Long id, Map<String, Object> data) {
        String status = data.get("status").toString();
        String reason = data.getOrDefault("reason", "").toString();
        try {
            adminEJB.changeAppointmentStatus(id, status, reason);
            return Response.ok(Map.of("success", true, "message", "Appointment status changed.")).build();
        } catch (Exception ex) {
            // Catching generic Exception for wrapped IllegalArgumentException (Not Found)
            String message = getBusinessMessage(ex);
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("message", message))
                           .build();
        }
    }

    @POST
    @Path("/appointments/{id}/assign-slot")
    public Response assignSlot(@PathParam("id") Long appointmentId, Map<String, Object> data) {
        try {
            // Safely parse Integer from String
            Integer slotId = Integer.parseInt(data.get("slotId").toString());
            adminEJB.assignSlotToAppointment(appointmentId, slotId);
            return Response.ok(Map.of("success", true, "message", "Slot assigned successfully.")).build();
        } catch (Exception ex) {
            String message = getBusinessMessage(ex);
            // Distinguish between Not Found (404, IllegalArgumentException) and Illegal State/Bad Request (400, IllegalStateException)
            if (message.contains("not found")) {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity(Map.of("message", message))
                               .build();
            } else { // Should catch IllegalStateException (Slot is already booked) and other bad requests
                return Response.status(Response.Status.BAD_REQUEST)
                               .entity(Map.of("message", message))
                               .build();
            }
        }
    }

    // -----------------------
    // Time Slots (New endpoints based on EJB)
    // -----------------------
    @PUT
    @Path("/slots/{id}/block")
    public Response blockTimeSlot(@PathParam("id") Integer slotId, Map<String, Object> data) {
        try {
            Long adminId = Long.parseLong(data.get("adminId").toString());
            String reason = data.get("reason").toString();
            adminEJB.blockTimeSlot(slotId, adminId, reason);
            return Response.ok(Map.of("success", true, "message", "Time slot blocked.")).build();
        } catch (Exception ex) {
            String message = getBusinessMessage(ex);
            return Response.status(message.contains("not found") ? Response.Status.NOT_FOUND : Response.Status.BAD_REQUEST)
                           .entity(Map.of("message", message))
                           .build();
        }
    }

    @PUT
    @Path("/slots/{id}/unblock")
    public Response unblockTimeSlot(@PathParam("id") Integer slotId, Map<String, Object> data) {
        try {
            Long adminId = Long.parseLong(data.get("adminId").toString());
            adminEJB.unblockTimeSlot(slotId, adminId);
            return Response.ok(Map.of("success", true, "message", "Time slot unblocked.")).build();
        } catch (Exception ex) {
            String message = getBusinessMessage(ex);
            return Response.status(message.contains("not found") ? Response.Status.NOT_FOUND : Response.Status.BAD_REQUEST)
                           .entity(Map.of("message", message))
                           .build();
        }
    }


    // -----------------------
    // Payments
    // -----------------------
    @GET
    @Path("/payments")
    public Response listPayments(@QueryParam("offset") @DefaultValue("0") int offset,
                                 @QueryParam("limit") @DefaultValue("50") int limit) {
        return Response.ok(adminEJB.listPayments(offset, limit)).build();
    }

    @POST
    @Path("/payments/{id}/status")
    public Response markPaymentStatus(@PathParam("id") Integer paymentId, Map<String, Object> data) {
        String status = data.get("status").toString();
        // adminId must be passed to the EJB for audit/validation
        Long adminId = data.get("adminId") != null ? Long.parseLong(data.get("adminId").toString()) : null;
        try {
            adminEJB.markPaymentStatus(paymentId, status, adminId);
            return Response.ok(Map.of("success", true, "message", "Payment status updated.")).build();
        } catch (Exception ex) {
            // Catching generic Exception for wrapped IllegalArgumentException (Not Found)
            String message = getBusinessMessage(ex);
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("message", message))
                           .build();
        }
    }

    // -----------------------
    // Earnings & Payouts (Updated based on AdminEJBLocal)
    // -----------------------
    
    @GET
    @Path("/artists/{artistId}/pending-earnings")
    public Response calculateArtistPendingEarnings(@PathParam("artistId") Long id,
                                             @QueryParam("from") @DefaultValue("") String fromStr, 
                                             @QueryParam("to") @DefaultValue("") String toStr) {
        try {
            // If dates are missing, use a safe default range (e.g., start of system to today)
            // This is a common and safer way to handle optional date ranges than throwing an exception.
            LocalDate fromDate = fromStr.isEmpty() ? LocalDate.of(2000, 1, 1) : LocalDate.parse(fromStr);
            LocalDate toDate = toStr.isEmpty() ? LocalDate.now() : LocalDate.parse(toStr);
            
            if (fromDate.isAfter(toDate)) {
                 throw new IllegalArgumentException("'from' date cannot be after 'to' date.");
            }

            BigDecimal total = adminEJB.calculateArtistPendingEarnings(id, fromDate, toDate);
            
            return Response.ok(Map.of("artistId", id, "totalPendingEarnings", total)).build();
        } catch (java.time.format.DateTimeParseException dtpe) {
             return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("message", "Invalid date format. Dates must be in YYYY-MM-DD format."))
                           .build();
        } catch (Exception ex) {
            // Handles other exceptions from EJB or date logic
            String message = getBusinessMessage(ex);
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("message", "Invalid date parameters or data: " + message))
                           .build();
        }
    }

    @POST
    @Path("/payouts/simulate")
    public Response simulatePayout(Map<String, Object> data) {
        try {
            Long artistId = Long.parseLong(data.get("artistId").toString());
            BigDecimal amount = new BigDecimal(data.get("amount").toString());
            LocalDate forMonth = LocalDate.parse(data.get("forMonth").toString());
            // adminId is required for EJB logic validation
            Long adminId = Long.parseLong(data.get("adminId").toString());
            
            Long payoutId = adminEJB.simulatePayout(artistId, forMonth, amount, adminId);
            return Response.ok(Map.of("payoutId", payoutId)).build();
        } catch (Exception ex) {
            String message = getBusinessMessage(ex);
             // Distinguish between Not Found (404) and Bad Request (400 - e.g. parsing)
            if (message.contains("not found")) {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity(Map.of("message", message))
                               .build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                               .entity(Map.of("message", "Invalid request data: " + message))
                               .build();
            }
        }
    }
    
    @GET
    @Path("/artists/{artistId}/payouts")
    public Response listArtistPayouts(@PathParam("artistId") Long artistId,
                                     @QueryParam("offset") @DefaultValue("0") int offset,
                                     @QueryParam("limit") @DefaultValue("50") int limit) {
        try {
             return Response.ok(adminEJB.listArtistPayouts(artistId, offset, limit)).build();
        } catch (IllegalArgumentException ex) {
             return Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("message", getBusinessMessage(ex)))
                            .build();
        } catch (Exception ex) {
             return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(Map.of("message", "Error fetching payouts: " + getBusinessMessage(ex)))
                            .build();
        }
    }
    // -----------------------
    // Reports
    // -----------------------
    @GET
@Path("/reports")
public Response generateReports(@QueryParam("from") String fromDateStr, @QueryParam("to") String toDateStr) {
    try {
        LocalDate fromDate = (fromDateStr != null && !fromDateStr.isEmpty()) ? LocalDate.parse(fromDateStr) : null;
        LocalDate toDate = (toDateStr != null && !toDateStr.isEmpty()) ? LocalDate.parse(toDateStr) : null;

        if (fromDate == null || toDate == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("message", "Both 'from' and 'to' date parameters are required and must be in yyyy-MM-dd format."))
                           .build();
        }

        Map<String, Object> report = adminEJB.generateSimpleReports(fromDate, toDate);
        return Response.ok(report).build();
    } catch (Exception ex) {
        // Handles date parsing or other bad requests
        String message = getBusinessMessage(ex);
        return Response.status(Response.Status.BAD_REQUEST)
                       .entity(Map.of("message", "Invalid date parameters or data: " + message))
                       .build();
    }
}

    // -----------------------
    // Medical Forms
    // -----------------------
    @GET
    @Path("/medical-forms")
    public Response listMedicalForms(@QueryParam("offset") @DefaultValue("0") int offset,
                                     @QueryParam("limit") @DefaultValue("50") int limit) {
        return Response.ok(adminEJB.listMedicalForms(offset, limit)).build();
    }

   @POST
    @Path("/medical-forms/{id}/approve")
    public Response approveMedicalForm(@PathParam("id") Integer formId) { 
        try {
            // 1. SECURELY retrieve the authenticated username from the JWT/SecurityContext
            String username = securityContext.getUserPrincipal().getName(); 
            
            // 2. Look up the Admin's ID using the username (The EJB method we need to create)
            Long adminId = adminEJB.getUserIdByUsername(username); 

            adminEJB.approveMedicalForm(formId, adminId);
            return Response.ok(Map.of("success", true, "message", "Medical form approved.")).build();
        } catch (Exception ex) { 
            String message = getBusinessMessage(ex);
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("message", message))
                           .build();
        }
    }
}
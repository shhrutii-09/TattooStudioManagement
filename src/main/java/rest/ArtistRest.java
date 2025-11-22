package rest;

import ejb.ArtistEJBLocal;
import entities.*;
import jakarta.ejb.EJB;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Path("/artist")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ArtistRest {

    @EJB
    private ArtistEJBLocal artistEJB;

    private String getBusinessMessage(Exception e) {
        Throwable cause = e.getCause();
        // Check for EJBException wrapper and return its cause message, otherwise return the current message
        return cause != null && cause.getMessage() != null ? cause.getMessage() : e.getMessage();
    }
    
    // -----------------------
    // Profile
    // -----------------------
    @GET
    @Path("/{id}")
    public Response getArtist(@PathParam("id") Long id) {
        try {
            AppUser artist = artistEJB.getArtistById(id);
            return Response.ok(artist).build();
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

    @PUT
    @Path("/{id}")
    public Response updateProfile(@PathParam("id") Long id, Map<String, Object> data) {
        try {
            String fullName = data.getOrDefault("fullName", null) != null ? data.get("fullName").toString() : null;
            String phone = data.getOrDefault("phone", null) != null ? data.get("phone").toString() : null;
            String portfolioLink = data.getOrDefault("portfolioLink", null) != null ? data.get("portfolioLink").toString() : null;

            AppUser updated = artistEJB.updateArtistProfile(id, fullName, phone, portfolioLink);
            return Response.ok(updated).build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }

    // -----------------------
    // Designs
    // -----------------------
    
    @POST
    @Path("/{id}/designs")
    public Response addDesign(@PathParam("id") Long id, TattooDesign design) {
        try {
            // NOTE: The incoming 'design' object from the client payload 
            // should match the fields in TattooDesign.java (title, style, price, description, etc.).
            // The JSON body you provided in the prompt is suitable.
            TattooDesign created = artistEJB.addDesign(id, design);
            // Return 201 Created for a new resource
            return Response.status(Response.Status.CREATED).entity(created).build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }
    
    @PUT
    @Path("/designs/{designId}")
    public Response updateDesign(@PathParam("designId") Long designId, TattooDesign designPayload) {
        try {
            // The payload can contain any subset of fields (title, price, imagePath, etc.)
            TattooDesign updated = artistEJB.updateDesign(designId, designPayload);
            return Response.ok(updated).build();
        } catch (IllegalArgumentException ex) {
            String message = ex.getMessage();
            if (message.contains("not found")) {
                 return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", message))
                        .build();
            }
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", message))
                    .build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }
    
    @DELETE
@Path("/designs/{designId}")
public Response deleteDesign(@PathParam("designId") Long designId) {
    try {
        artistEJB.deleteDesign(designId);
        // Use 204 No Content for a successful deletion operation
        return Response.status(Response.Status.NO_CONTENT).build();
    } catch (IllegalArgumentException ex) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", ex.getMessage()))
                .build();
    } catch (Exception ex) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Failed to delete design: " + ex.getMessage()))
                .build();
    }
}
    
    @GET
    @Path("/{id}/designs")
    public Response listDesigns(@PathParam("id") Long id,
                                @QueryParam("offset") @DefaultValue("0") int offset,
                                @QueryParam("limit") @DefaultValue("50") int limit) {
        try {
            List<TattooDesign> list = artistEJB.getArtistDesigns(id, offset, limit);
            return Response.ok(list).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/designs/{designId}")
    public Response getDesign(@PathParam("designId") Long designId) {
        try {
            TattooDesign design = artistEJB.getDesignById(designId);
            if (design == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Design not found"))
                        .build();
            }
            return Response.ok(design).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/designs/search")
    public Response searchDesigns(
        @QueryParam("q") String q,
        @QueryParam("style") String style,
        @QueryParam("minPrice") BigDecimal minPrice,
        @QueryParam("maxPrice") BigDecimal maxPrice,
        @DefaultValue("0") @QueryParam("offset") int offset,
        @DefaultValue("10") @QueryParam("limit") int limit) {

        try {
            List<TattooDesign> results = artistEJB.searchDesigns(q, style, minPrice, maxPrice, offset, limit);
            return Response.ok(results).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }

    // -----------------------
    // Likes / Favourites
    // -----------------------
    @POST
    @Path("/designs/{designId}/like")
    public Response likeDesign(@PathParam("designId") Long designId, Map<String, Object> data) {
        try {
            Long clientId = Long.parseLong(data.get("clientId").toString());
            DesignLike like = artistEJB.likeDesign(clientId, designId);
            return Response.ok(like).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/designs/{designId}/unlike")
    public Response unlikeDesign(@PathParam("designId") Long designId, Map<String, Object> data) {
        try {
            Long clientId = Long.parseLong(data.get("clientId").toString());
            artistEJB.unlikeDesign(clientId, designId);
            return Response.ok(Map.of("success", true)).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/designs/{designId}/favourite")
    public Response favouriteDesign(@PathParam("designId") Long designId, Map<String, Object> data) {
        try {
            Long clientId = Long.parseLong(data.get("clientId").toString());
            DesignFavourite fav = artistEJB.favouriteDesign(clientId, designId);
            return Response.ok(fav).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/designs/{designId}/unfavourite")
    public Response unfavouriteDesign(@PathParam("designId") Long designId, Map<String, Object> data) {
        try {
            Long clientId = Long.parseLong(data.get("clientId").toString());
            artistEJB.unfavouriteDesign(clientId, designId);
            return Response.ok(Map.of("success", true)).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }

    // -----------------------
    // Time slots
    // -----------------------
    
   @GET
@Path("/{id}/schedule")
public Response getArtistSchedule(@PathParam("id") Long artistId) {
    try {
        // This line now compiles since the method is in the EJBLocal interface
        List<ArtistSchedule> schedules = artistEJB.listSchedulesForArtist(artistId); 
        return Response.ok(schedules).build();
    } catch (IllegalArgumentException ex) {
        // Catches "Artist not found" from EJB layer
        return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", ex.getMessage()))
                .build();
    } catch (Exception ex) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Error retrieving schedule: " + ex.getMessage()))
                .build();
    }
}
    
@POST
@Path("/{id}/schedule")
@Transactional // Include if your JAX-RS requires explicit transaction handling
@Consumes(MediaType.APPLICATION_JSON) // Redundant but good practice
public Response setArtistSchedule(@PathParam("id") Long artistId, ArtistSchedule schedule) {
    try {
        // CRITICAL: Need to convert the incoming JSON strings (like DayOfWeek, Time) 
        // into proper Java objects. If you pass the raw ArtistSchedule, JAX-RS should 
        // try to map the JSON to the fields.

        // Assuming your JAX-RS provider (Payara) can map the incoming JSON to ArtistSchedule
        // (which requires a default constructor and getters/setters in ArtistSchedule.java).
        
        // Before calling EJB, ensure required fields are present
        if (schedule.getDayOfWeek() == null || schedule.getStartTime() == null || schedule.getEndTime() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "DayOfWeek, StartTime, and EndTime are required."))
                    .build();
        }
        
        ArtistSchedule savedSchedule = artistEJB.saveArtistSchedule(artistId, schedule);
        
        return Response.status(Response.Status.CREATED)
                .entity(savedSchedule)
                .build();

    } catch (IllegalArgumentException ex) {
        return Response.status(Response.Status.NOT_FOUND) // Artist ID not found
                .entity(Map.of("error", ex.getMessage()))
                .build();
    } catch (Exception ex) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Error setting schedule: " + ex.getMessage()))
                .build();
    }
}

@GET
    @Path("/{id}/slots/available") // This maps to the API you are testing
    public Response getAvailableSlots(@PathParam("id") Long artistId) {
        try {
            // Call the EJB method to fetch available slots
            List<TimeSlot> availableSlots = artistEJB.listAvailableTimeSlots(artistId);
            
            // Return the list of slots
            return Response.ok(availableSlots).build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to retrieve available slots: " + ex.getMessage()))
                    .build();
        }
    }

        @POST
        @Path("/{id}/slots/generate") 
        @Consumes(MediaType.APPLICATION_JSON)
        public Response generateSlots(@PathParam("id") Long artistId, Map<String, Object> body) {
            try {
                // 1. Parse required date/duration fields from the JSON body
                String startDateStr = (String) body.get("startDate");
                String endDateStr = (String) body.get("endDate");

                // FIX: Safely retrieve the number and convert to Integer to prevent ClassCastException
                // JAX-RS often deserializes number literals like '60' into BigDecimal or Long.
                Number slotDurationNumber = (Number) body.get("slotDurationMinutes");
                Integer slotDurationMinutes = null;

                // Check for required fields upfront
                if (startDateStr == null || endDateStr == null || slotDurationNumber == null) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", "startDate, endDate, and slotDurationMinutes are required."))
                            .build();
                }

                // Convert to Integer safely
                slotDurationMinutes = slotDurationNumber.intValue(); // The fix is here!

                // 2. Validate and parse dates
                LocalDate startDate = LocalDate.parse(startDateStr);
                LocalDate endDate = LocalDate.parse(endDateStr);

                // CRITICAL LOGIC CHECK: Ensure startDate is not after endDate
                if (startDate.isAfter(endDate)) {
                     throw new IllegalArgumentException("startDate (" + startDateStr + ") cannot be after endDate (" + endDateStr + ").");
                }

                // 3. Call the EJB method (this line now executes without the ClassCastException)
                List<TimeSlot> createdSlots = artistEJB.generateTimeSlotsForArtist(artistId, startDate, endDate, slotDurationMinutes);

                // 4. Return success response
                return Response.ok(createdSlots).build();

            } catch (DateTimeParseException ex) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Invalid date format. Dates must be in YYYY-MM-DD format."))
                        .build();
            } catch (IllegalArgumentException ex) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", ex.getMessage()))
                        .build();
            } catch (ClassCastException ex) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Invalid type for slotDurationMinutes. Must be a whole number."))
                        .build();
            } catch (Exception ex) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(Map.of("error", "An unexpected server error occurred: " + ex.getMessage()))
                        .build();
            }
        }

    @GET
    @Path("/{id}/timeslots")
    public Response getTimeSlots(@PathParam("id") Long id) {
        try {
            List<TimeSlot> list = artistEJB.getArtistTimeSlots(id);
            return Response.ok(list).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/{id}/timeslots")
    public Response addTimeSlot(@PathParam("id") Long id, TimeSlot slot) {
        try {
            TimeSlot created = artistEJB.addTimeSlot(id, slot);
            return Response.ok(created).build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/timeslots/{slotId}")
    public Response updateTimeSlot(@PathParam("slotId") Integer slotId, Map<String, Object> data) {
        try {
            LocalDateTime start = data.get("start") != null ? LocalDateTime.parse(data.get("start").toString()) : null;
            LocalDateTime end = data.get("end") != null ? LocalDateTime.parse(data.get("end").toString()) : null;
            TimeSlot.TimeSlotStatus status = data.get("status") != null ? TimeSlot.TimeSlotStatus.valueOf(data.get("status").toString()) : null;

            TimeSlot updated = artistEJB.updateTimeSlot(slotId, start, end, status);
            return Response.ok(updated).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/timeslots/{slotId}")
    public Response deleteTimeSlot(@PathParam("slotId") Integer slotId) {
        try {
            artistEJB.deleteTimeSlot(slotId);
            return Response.ok(Map.of("success", true, "message", "Time slot deleted successfully")).build();
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

    @POST
    @Path("/timeslots/{slotId}/block")
    public Response blockSlot(@PathParam("slotId") Integer slotId, Map<String, Object> data) {
        try {
            String reason = data.getOrDefault("reason", "").toString();
            artistEJB.markSlotAsBlocked(slotId, reason);
            return Response.ok(Map.of("success", true)).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }

    // -----------------------
    // Appointments
    // -----------------------
    @GET
    @Path("/{id}/appointments")
    public Response listAppointments(@PathParam("id") Long id,
                                     @QueryParam("offset") @DefaultValue("0") int offset,
                                     @QueryParam("limit") @DefaultValue("50") int limit) {
        try {
            List<Appointment> list = artistEJB.listArtistAppointments(id, offset, limit);
            return Response.ok(list).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/appointments/{id}")
    public Response getAppointment(@PathParam("id") Long id) {
        try {
            Appointment appointment = artistEJB.getAppointment(id);
            return Response.ok(appointment).build();
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

    @POST
    @Path("/appointments/{id}/status")
    public Response changeAppointmentStatus(@PathParam("id") Long id, Map<String, Object> data) {
        try {
            String status = data.getOrDefault("status", "").toString();
            String reason = data.getOrDefault("reason", null) != null ? data.get("reason").toString() : null;
            Appointment updated = artistEJB.changeAppointmentStatus(id, status, reason);
            return Response.ok(updated).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/appointments/{id}/note")
    public Response addNote(@PathParam("id") Long id, Map<String, Object> data) {
        try {
            String note = data.getOrDefault("note", "").toString();
            Appointment updated = artistEJB.addClientNoteToAppointment(id, note);
            return Response.ok(updated).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }

    // -----------------------
    // Medical forms
    // -----------------------
    @GET
    @Path("/appointments/{id}/medical-form")
    public Response getMedicalForm(@PathParam("id") Long appointmentId) {
        try {
            MedicalForm form = artistEJB.getMedicalFormForAppointment(appointmentId);
            if (form == null) return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Medical form not found")).build();
            return Response.ok(form).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/medical-forms/{id}/acknowledge")
    public Response acknowledgeForm(@PathParam("id") Integer formId, Map<String, Object> data) {
        try {
            Long artistId = data.get("artistId") != null ? Long.parseLong(data.get("artistId").toString()) : null;
            boolean approve = Boolean.parseBoolean(data.getOrDefault("approve", "false").toString());
            artistEJB.acknowledgeMedicalForm(formId, artistId, approve);
            return Response.ok(Map.of("success", true)).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }

    // -----------------------
    // Feedback & Reviews
    // -----------------------
    @GET
    @Path("/{id}/feedback")
    public Response getFeedback(@PathParam("id") Long id,
                                @QueryParam("offset") @DefaultValue("0") int offset,
                                @QueryParam("limit") @DefaultValue("50") int limit) {
        try {
            List<Feedback> list = artistEJB.listFeedbackForArtist(id, offset, limit);
            return Response.ok(list).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}/reviews")
    public Response getReviews(@PathParam("id") Long id,
                               @QueryParam("offset") @DefaultValue("0") int offset,
                               @QueryParam("limit") @DefaultValue("50") int limit) {
        try {
            List<Review> list = artistEJB.listReviewsForArtist(id, offset, limit);
            return Response.ok(list).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }
    }

    // -----------------------
    // Payments & earnings
    // -----------------------
   @GET
@Path("/{id}/payments") // This path is now unique for listing payments
public Response listPaymentsForArtist(@PathParam("id") Long artistId,
                                      @QueryParam("offset") @DefaultValue("0") int offset,
                                      @QueryParam("limit") @DefaultValue("50") int limit) {
    try {
        // Calls the correct EJB method
        List<Payment> payments = artistEJB.listPaymentsForArtist(artistId, offset, limit);
        return Response.ok(payments).build();
    } catch (Exception ex) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", getBusinessMessage(ex)))
                .build();
    }
}
    
    @GET
    @Path("/{id}/earnings/logs")
    public Response listEarningLogsForArtist(@PathParam("id") Long artistId,
                                              @QueryParam("offset") @DefaultValue("0") int offset,
                                              @QueryParam("limit") @DefaultValue("50") int limit) {
        try {
            List<EarningLog> logs = artistEJB.listEarningLogsForArtist(artistId, offset, limit);
            return Response.ok(logs).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", getBusinessMessage(ex)))
                    .build();
        }
    }
    
    @GET
    @Path("/{id}/earnings/pending") // CORRECTED path and method call
    public Response getPendingEarnings(@PathParam("id") Long id) {
        try {
            // Correct EJB method: calculateArtistPendingEarnings only takes artistId
            BigDecimal total = artistEJB.calculateArtistPendingEarnings(id);
            return Response.ok(Map.of("totalPendingEarnings", total)).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", getBusinessMessage(ex)))
                    .build();
        }
    }
    
//    @GET
//    @Path("/{id}/payments")
//    public Response getPayments(@PathParam("id") Long id,
//                                @QueryParam("offset") @DefaultValue("0") int offset,
//                                @QueryParam("limit") @DefaultValue("50") int limit) {
//        try {
//            List<Payment> list = artistEJB.listPaymentsForArtist(id, offset, limit);
//            return Response.ok(list).build();
//        } catch (Exception ex) {
//            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
//                    .entity(Map.of("error", ex.getMessage()))
//                    .build();
//        }
//    }

    @GET
    @Path("/{id}/payouts")
    public Response listArtistPayouts(@PathParam("id") Long artistId,
                                      @QueryParam("offset") @DefaultValue("0") int offset,
                                      @QueryParam("limit") @DefaultValue("50") int limit) {
        try {
            List<ArtistPayout> payouts = artistEJB.listArtistPayouts(artistId, offset, limit);
            return Response.ok(payouts).build();
        } catch (Exception ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", getBusinessMessage(ex)))
                    .build();
        }
    }
    
//    @GET
//    @Path("/{id}/earnings")
//    public Response getEarnings(@PathParam("id") Long id,
//                                @QueryParam("from") String fromStr,
//                                @QueryParam("to") String toStr) {
//        try {
//            LocalDate from = (fromStr != null && !fromStr.isEmpty()) ? LocalDate.parse(fromStr) : null;
//            LocalDate to = (toStr != null && !toStr.isEmpty()) ? LocalDate.parse(toStr) : null;
//            BigDecimal total = artistEJB.calculateEarnings(id, from, to);
//            return Response.ok(Map.of("totalEarnings", total)).build();
//        } catch (Exception ex) {
//            return Response.status(Response.Status.BAD_REQUEST)
//                    .entity(Map.of("error", ex.getMessage()))
//                    .build();
//        }
//    }

//    @POST
//    @Path("/{id}/payouts/request")
//    public Response requestPayout(@PathParam("id") Long id, Map<String, Object> data) {
//        try {
//            BigDecimal amount = new BigDecimal(data.get("amount").toString());
//            Long requestedBy = data.get("requestedBy") != null ? Long.parseLong(data.get("requestedBy").toString()) : null;
//            Long payoutId = artistEJB.requestPayout(id, amount, requestedBy);
//            return Response.ok(Map.of("payoutId", payoutId)).build();
//        } catch (Exception ex) {
//            return Response.status(Response.Status.BAD_REQUEST)
//                    .entity(Map.of("error", ex.getMessage()))
//                    .build();
//        }
//    }
}

package rest;

import ejb.AuthEJB;
import entities.AppUser;
import jakarta.annotation.security.PermitAll;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import security.JWTUtil;
import util.PasswordUtil; // Import Utility

@Path("/auth")
@PermitAll
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthRest {

    @EJB
    private AuthEJB authBean;

    public static class LoginCredentials {
        public String username;
        public String password;
    }

    public static class RegistrationDTO {
        public String username;
        public String password;
        public String fullName;
        public String email;
        public String phone;
        public String role; 
    }

    @POST
    @Path("/login")
    public Response login(LoginCredentials loginData) {
        if (loginData == null || loginData.username == null || loginData.password == null) {
             return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Username and password are required"))
                    .build();
        }

        // AuthEJB now handles hash verification internally
        AppUser user = authBean.authenticateUser(loginData.username, loginData.password);

        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Invalid Credentials"))
                    .build();
        }

        String roleName = user.getRole().getRoleName().toUpperCase().trim(); 
        String token = JWTUtil.generateToken(user.getUsername(), roleName);

        return Response.ok(Map.of(
            "token", token, 
            "role", roleName, 
            "userId", user.getUserId(),
            "username", user.getUsername()
        )).build();
    }

    @POST
    @Path("/register")
    public Response register(RegistrationDTO regData) {
        try {
            if (regData.username == null || regData.password == null || regData.email == null || regData.role == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Missing required fields."))
                        .build();
            }

            String roleUpper = regData.role.toUpperCase().trim();
            if (!roleUpper.equals("CLIENT") && !roleUpper.equals("ARTIST")) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Invalid Role. Allowed: CLIENT, ARTIST"))
                        .build();
            }

            // HASH PASSWORD HERE
            String hashedPassword = PasswordUtil.hashPassword(regData.password);

            AppUser newUser = new AppUser();
            newUser.setUsername(regData.username);
            newUser.setPassword(hashedPassword); // Set Hash
            newUser.setFullName(regData.fullName);
            newUser.setEmail(regData.email);
            newUser.setPhone(regData.phone);

            authBean.registerUser(newUser, roleUpper);

            return Response.status(Response.Status.CREATED)
                    .entity(Map.of("success", true, "message", "User registered successfully."))
                    .build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Registration failed: " + e.getMessage()))
                    .build();
        }
    }
}
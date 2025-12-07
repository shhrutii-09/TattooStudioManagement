package security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.AuthenticationException;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class JWTAuthenticationMechanism implements HttpAuthenticationMechanism {

    @Override
    public AuthenticationStatus validateRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpMessageContext context) throws AuthenticationException {

        String authHeader = request.getHeader("Authorization");

        // 1. Check for Authorization header (Bearer token)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return context.doNothing(); // Allows public endpoints
        }

        // 2. Extract token
        String token = authHeader.substring("Bearer ".length()).trim();

        // 3. Validate token
        if (!JWTUtil.validateToken(token)) {
            return context.responseUnauthorized(); // Token invalid/expired
        }

        // 4. Extract details
        String username = JWTUtil.getUsernameFromToken(token);
        String role = JWTUtil.getRoleFromToken(token);

        // 5. Notify the container about the authenticated user and their roles
        return context.notifyContainerAboutLogin(
                username,
//                new HashSet<>(List.of(role)));
                Set.of(role));
                }
}
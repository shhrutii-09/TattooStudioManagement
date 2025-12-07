package util;

/**
 * Utility class for password hashing and verification.
 * * NOTE: In a real environment, you MUST use a robust, industry-standard
 * library like jBCrypt, SCrypt, or Argon2 for secure hashing. This simple
 * SHA-256 implementation with a fixed salt is provided here only to
 * demonstrate the required architecture (saving a hash, not plain text)
 * and should be replaced with a proper library in production.
 */
public class PasswordUtil {

    // Simple, fixed salt for demonstration purposes (DO NOT USE IN PRODUCTION)
    private static final String DEMO_SALT = "InkFlowStudioSecureSalt2025"; 

    /**
     * Hashes a plain-text password for storage.
     * @param password The plain-text password.
     * @return The hashed password string.
     */
    public static String hashPassword(String password) {
        // Concatenate password with salt
        return sha256(password + DEMO_SALT);
    }

    /**
     * Verifies a plain-text password against a stored hash during login.
     * @param password The plain-text password input from the user.
     * @param storedHash The hash retrieved from the database.
     * @return true if the passwords match, false otherwise.
     */
    public static boolean verifyPassword(String password, String storedHash) {
        // Hash the input password and compare it to the stored hash
        return storedHash.equals(hashPassword(password));
    }

    /**
     * A simple SHA-256 implementation placeholder (replace with real library).
     * @param base The string to hash.
     * @return The hashed string.
     */
    private static String sha256(String base) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            // In a real application, you would handle this more gracefully.
            throw new RuntimeException("Error hashing password", ex);
        }
    }
}
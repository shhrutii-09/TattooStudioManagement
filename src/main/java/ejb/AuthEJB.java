package ejb;

import entities.AppUser;
import entities.GroupMaster;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import util.PasswordUtil; 

@Stateless
public class AuthEJB {

    @PersistenceContext(unitName = "TattooPU")
    private EntityManager em;

    public AppUser authenticateUser(String username, String password) {
        try {
            // STEP 1: Find user by username ONLY. 
            // We do NOT check the password in the query.
            TypedQuery<AppUser> query = em.createQuery(     "SELECT u FROM AppUser u JOIN FETCH u.role r WHERE (u.username = :user OR u.email = :user) AND u.isActive = TRUE",
    AppUser.class);
query.setParameter("user", username);

            AppUser user = query.getSingleResult();
            
            // STEP 2: Verify the password using your Utility
            if (PasswordUtil.verifyPassword(password, user.getPassword())) {
                if (user.getRole() != null) user.getRole().getRoleName().trim(); 
                return user;
            } else {
                return null;
            }


        } catch (NoResultException e) {
            return null; // Username not found
        }
    }
    
    public void registerUser(AppUser newUser, String roleName) {
        // 1. Check if username or email already exists
        Long count = em.createQuery("SELECT COUNT(u) FROM AppUser u WHERE u.username = :user OR u.email = :email", Long.class)
                .setParameter("user", newUser.getUsername())
                .setParameter("email", newUser.getEmail())
                .getSingleResult();
        
        if (count > 0) {
            throw new IllegalArgumentException("Username or Email already exists.");
        }

        // 2. Find or Create Role
        GroupMaster role;
        try {
            role = em.createQuery("SELECT g FROM GroupMaster g WHERE g.roleName = :role", GroupMaster.class)
                     .setParameter("role", roleName)
                     .getSingleResult();
        } catch (NoResultException e) {
            role = new GroupMaster();
            role.setRoleName(roleName);
            em.persist(role);
        }

        // 3. Setup User
        newUser.setRole(role);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setIsActive(true); 
        newUser.setIsVerified(false); 

        // 4. Save
        em.persist(newUser);
    }
}
package ejb;

import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.EntityManager;

@Startup
@Singleton
public class DatabaseInitializer {

    @PersistenceContext(unitName = "TattooPU")
    private EntityManager em;

    public void init() {
        // This method is called upon initialization.
        // Simply injecting the EntityManager and letting the EJB start
        // is enough to force the JPA provider to execute the 
        // "create" database action defined in persistence.xml.
        System.out.println("TattooPU Persistence Unit Initialized. Schema generation expected to run.");
    }
}

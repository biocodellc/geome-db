package biocode.fims.rest;

/**
 * Jersey Application for Biscicol Resolver Services
 */
public class BiscicolResolverApplication extends FimsApplication {

    public BiscicolResolverApplication() {
        super();
        packages("biocode.fims.rest.services.id");
    }
}

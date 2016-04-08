package biocode.fims.rest;

/**
 * Created by rjewing on 3/18/16.
 */
public class BiscicolResolverApplication extends FimsApplication {

    public BiscicolResolverApplication() {
        super();
        packages("biocode.fims.rest.services.id");
    }
}

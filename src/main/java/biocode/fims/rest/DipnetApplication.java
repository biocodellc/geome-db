package biocode.fims.rest;


import biocode.fims.rest.services.rest.AuthenticationService;

/**
 * * Jersey Application for Dipnet REST Services
 */
public class DipnetApplication extends FimsApplication {

    public DipnetApplication() {
        super();
        register(AuthenticationService.class);
    }
}

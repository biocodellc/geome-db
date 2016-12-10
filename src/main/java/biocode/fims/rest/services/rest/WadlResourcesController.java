package biocode.fims.rest.services.rest;

import biocode.fims.settings.SettingsManager;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * @author RJ Ewing
 */
@Produces({"application/vnd.sun.wadl+xml", "application/xml"})
@Singleton
@Path("fims.wadl")
public class WadlResourcesController extends FimsAbstractWadlResourcesController {

    WadlResourcesController(SettingsManager settingsManager) {
        super(settingsManager);
    }
}

package biocode.fims.rest.services.rest;

import biocode.fims.authorizers.ProjectAuthorizer;
import biocode.fims.service.BcidService;
import biocode.fims.service.ProjectService;
import biocode.fims.settings.SettingsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.Path;

/**
 * @author RJ Ewing
 */
@Controller
@Path("bcids")
public class BcidController extends FimsAbstractBcidController {
    @Autowired
    BcidController(BcidService bcidService,SettingsManager settingsManager, ProjectAuthorizer projectAuthorizer) {
        super(bcidService, settingsManager, projectAuthorizer);
    }
}

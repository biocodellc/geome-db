package biocode.fims.rest.services.rest;

import biocode.fims.service.ExpeditionService;
import biocode.fims.settings.SettingsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.Path;

/**
 * REST interface calls for working with expeditions.
 */
@Controller
@Path("expeditions")
public class ExpeditionController extends FimsAbstractExpeditionController {
    private static Logger logger = LoggerFactory.getLogger(ExpeditionController.class);

    @Autowired
    public ExpeditionController(ExpeditionService expeditionService, SettingsManager settingsManager) {
        super(expeditionService, settingsManager);
    }

}

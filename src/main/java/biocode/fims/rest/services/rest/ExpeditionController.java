package biocode.fims.rest.services.rest;

import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import biocode.fims.settings.SettingsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.Path;

/**
 * REST services dealing with expeditions
 * @resourceTag Expeditions
 */
@Controller
@Path("expeditions")
public class ExpeditionController extends FimsAbstractExpeditionController {

    @Autowired
    public ExpeditionController(ExpeditionService expeditionService, ProjectService projectService, SettingsManager settingsManager) {
        super(expeditionService, projectService, settingsManager);
    }
}

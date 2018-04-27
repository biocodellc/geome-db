package biocode.fims.rest.services.rest;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;

/**
 * project API endpoints
 *
 * @resourceTag Projects
 */
@Controller
@Path("projects")
public class ProjectController extends BaseProjectsController {

    @Autowired
    ProjectController(ExpeditionService expeditionService, FimsProperties props,
                      ProjectService projectService) {
        super(expeditionService, props, projectService);
    }
}

package biocode.fims.rest.services.rest;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.*;
import biocode.fims.fimsExceptions.*;
import biocode.fims.models.Project;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import biocode.fims.service.UserService;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * REST services dealing with projects
 *
 * @resourceTag Projects
 */
@Controller
@Path("projects")
public class ProjectController extends FimsAbstractProjectsController {

    @Autowired
    ProjectController(ExpeditionService expeditionService, FimsProperties props,
                      ProjectService projectService, UserService userService) {
        super(expeditionService, props, projectService);
    }
}

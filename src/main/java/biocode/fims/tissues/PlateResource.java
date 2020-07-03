package biocode.fims.tissues;


import biocode.fims.application.config.FimsProperties;
import biocode.fims.authorizers.ProjectAuthorizer;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.models.Project;
import biocode.fims.models.User;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.service.ProjectService;
import biocode.fims.service.PlateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author rjewing
 */
@Controller
@Produces(MediaType.APPLICATION_JSON)
public class PlateResource extends FimsController {
    private final static Logger logger = LoggerFactory.getLogger(PlateResource.class);

    private final ProjectService projectService;
    private final PlateService plateService;
    private final ProjectAuthorizer projectAuthorizer;

    @Autowired
    public PlateResource(FimsProperties props, ProjectService projectService, PlateService plateService, ProjectAuthorizer projectAuthorizer) {
        super(props);
        this.projectService = projectService;
        this.plateService = plateService;
        this.projectAuthorizer = projectAuthorizer;
    }

    @Path("{projectId: [0-9]+}")
    @GET
    public List<String> getPlates(@PathParam("projectId") Integer projectId) {
        User user = userContext.getUser();
        Project project = projectService.getProject(projectId);

        if (project == null) {
            throw new BadRequestException("Invalid project");
        }

        if (!projectAuthorizer.userHasAccess(user, project)) {
            throw new ForbiddenRequestException("You do not have access to this project");
        }

        return plateService.getPlates(project);
    }

    @Path("{projectId: [0-9]+}/{plateName}")
    @GET
    public Plate get(@PathParam("projectId") Integer projectId,
                     @PathParam("plateName") String plateName) {
        User user = userContext.getUser();
        Project project = projectService.getProject(projectId);

        if (project == null) {
            throw new BadRequestException("Invalid project");
        }

        if (!projectAuthorizer.userHasAccess(user, project)) {
            throw new ForbiddenRequestException("You do not have access to this project");
        }

        Plate plate = plateService.getPlate(project, plateName);

        if (plate == null) {
            throw new BadRequestException("Invalid plate");
        }

        return plate;
    }

    @Authenticated
    @Path("{projectId: [0-9]+}/{plateName}")
    @POST
    public PlateResponse create(@PathParam("projectId") Integer projectId,
                                @PathParam("plateName") String plateName,
                                Plate plate) {
        User user = userContext.getUser();
        Project project = projectService.getProject(projectId);

        if (project == null) {
            throw new BadRequestException("Invalid project");
        }

        plate.name(plateName);

        return plateService.create(user, project, plate);
    }

    @Authenticated
    @Path("{projectId: [0-9]+}/{plateName}")
    @PUT
    public PlateResponse update(@PathParam("projectId") Integer projectId,
                                @PathParam("plateName") String plateName,
                                Plate plate) {
        User user = userContext.getUser();
        Project project = projectService.getProject(projectId);

        if (project == null) {
            throw new BadRequestException("Invalid project");
        }

        plate.name(plateName);

        return plateService.update(user, project, plate);
    }
}

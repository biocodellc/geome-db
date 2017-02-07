package biocode.fims.rest.services.rest;

import biocode.fims.authorizers.ProjectAuthorizer;
import biocode.fims.bcid.BcidMetadataSchema;
import biocode.fims.bcid.Identifier;
import biocode.fims.bcid.Renderer.JSONRenderer;
import biocode.fims.entities.Bcid;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.serializers.Views;
import biocode.fims.service.BcidService;
import biocode.fims.service.ProjectService;
import biocode.fims.settings.SettingsManager;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author RJ Ewing
 */
@Controller
@Path("bcids")
public class BcidController extends FimsAbstractBcidController {
    private final BcidService bcidService;
    private final ProjectService projectService;

    @Autowired
    BcidController(BcidService bcidService, ProjectService projectService, SettingsManager settingsManager) {
        super(bcidService, projectService, settingsManager);
        this.bcidService = bcidService;
        this.projectService = projectService;
    }

    // TODO move this when we refactor the BCID application out of FIMS. This should probably get moved to the FimsAbstractBcidController
    @SuppressWarnings("Duplicates")
    @JsonView(Views.Summary.class)
    @GET
    @Path("metadata/{identifier: .+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response metadata (@PathParam("identifier") String identifierString) {
        Bcid bcid;
        String divider = settingsManager.retrieveValue("divider");
        Identifier identifier = new Identifier(identifierString, divider);

        try {
            bcid = bcidService.getBcid(identifier.getBcidIdentifier());
        } catch (EmptyResultDataAccessException e) {
            throw new BadRequestException("Invalid Identifier");
        }

        ProjectAuthorizer projectAuthorizer = new ProjectAuthorizer(projectService, appRoot);
        BcidMetadataSchema bcidMetadataSchema = new BcidMetadataSchema(bcid, settingsManager, identifier);

        JSONRenderer renderer = new JSONRenderer(
                userContext.getUser(),
                bcid,
                projectAuthorizer,
                bcidService,
                bcidMetadataSchema,
                appRoot
        );

        return Response.ok(renderer.getMetadata()).build();
    }
}

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

    private static Logger logger = LoggerFactory.getLogger(ProjectController.class);

    @Autowired
    ProjectController(ExpeditionService expeditionService, FimsProperties props,
                      ProjectService projectService, UserService userService) {
        super(expeditionService, props, projectService);
    }

    @GET
    @Path("/{projectId}/getLatLongColumns")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLatLongColumns(@PathParam("projectId") int projectId) {
        String decimalLatDefinedBy = "http://rs.tdwg.org/dwc/terms/decimalLatitude";
        String decimalLongDefinedBy = "http://rs.tdwg.org/dwc/terms/decimalLongitude";
        JSONObject response = new JSONObject();

        try {
            File configFile = new ConfigurationFileFetcher(projectId, defaultOutputDirectory(), true).getOutputFile();

            Mapping mapping = new Mapping();
            mapping.addMappingRules(configFile);
            String defaultSheet = mapping.getDefaultSheetName();
            ArrayList<Attribute> attributeList = mapping.getAllAttributes(defaultSheet);

            response.put("data_sheet", defaultSheet);

            for (Attribute attribute : attributeList) {
                // when we find the column corresponding to the definedBy for lat and long, add them to the response
                if (decimalLatDefinedBy.equalsIgnoreCase(attribute.getDefined_by())) {
                    response.put("lat_column", attribute.getColumn());
                } else if (decimalLongDefinedBy.equalsIgnoreCase(attribute.getDefined_by())) {
                    response.put("long_column", attribute.getColumn());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new FimsRuntimeException(500, e);
        }
        return Response.ok(response.toJSONString()).build();
    }

    @GET
    @Path("/{projectId}/filterOptions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFilterOptions(@PathParam("projectId") int projectId) {
        Project project = projectService.getProject(projectId, props.appRoot());

        if (project == null) {
            throw new biocode.fims.fimsExceptions.BadRequestException("Invalid projectId");
        }

        List<String> columns = new ArrayList<>();

        // TODO don't default to first entity
        for (Attribute a: project.getProjectConfig().entities().get(0).getAttributes()) {
            columns.add(a.getColumn());
        }

        return Response.ok(columns).build();
    }

    @GET
    @Path("/{projectId}/uniqueKey")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUniqueKey(@PathParam("projectId") Integer projectId) {
        File configFile = new ConfigurationFileFetcher(projectId, defaultOutputDirectory(), true).getOutputFile();

        Mapping mapping = new Mapping();
        mapping.addMappingRules(configFile);

        return Response.ok("{\"uniqueKey\":\"" + mapping.getDefaultSheetUniqueKey() + "\"}").build();
    }
}

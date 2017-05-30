package biocode.fims.rest.services.rest;

import biocode.fims.bcid.ProjectMinter;
import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.*;
import biocode.fims.fimsExceptions.*;
import biocode.fims.models.Expedition;
import biocode.fims.models.Project;
import biocode.fims.models.User;
import biocode.fims.rest.filters.Admin;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.run.TemplateProcessor;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import biocode.fims.service.UserService;
import biocode.fims.settings.SettingsManager;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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

    private final UserService userService;

    @Autowired
    ProjectController(ExpeditionService expeditionService, SettingsManager settingsManager,
                      ProjectService projectService, UserService userService) {
        super(expeditionService, settingsManager, projectService);
        this.userService = userService;
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
        Project project = projectService.getProject(projectId, settingsManager.retrieveValue("appRoot"));

        if (project == null) {
            throw new biocode.fims.fimsExceptions.BadRequestException("Invalid projectId");
        }

        List<String> columns = new ArrayList<>();

        // TODO don't default to first entity
        for (Attribute a: project.getProjectConfig().getEntities().get(0).getAttributes()) {
            columns.add(a.getColumn());
        }

        return Response.ok(columns).build();
    }

    @GET
    @Authenticated
    @Produces(MediaType.TEXT_HTML)
    @Path("/{projectId}/metadata")
    public Response listMetadataAsTable(@PathParam("projectId") int projectId) {
        ProjectMinter project = new ProjectMinter();
        JSONObject metadata = project.getMetadata(projectId, userContext.getUser().getUsername());
        StringBuilder sb = new StringBuilder();

        sb.append("<table>\n");
        sb.append("\t<tbody>\n");
        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td>Title:</td>\n");
        sb.append("\t\t\t<td>");
        sb.append(metadata.get("title"));
        sb.append("</td>\n");
        sb.append("\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td>Configuration File:</td>\n");
        sb.append("\t\t\t<td>");
        sb.append(metadata.get("validationXml"));
        sb.append("</td>\n");
        sb.append("\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td>Public Project</td>\n");
        sb.append("\t\t\t<td>\n");
        sb.append(metadata.get("public"));
        sb.append("</td>\n");
        sb.append("\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td></td>\n");
        sb.append("\t\t\t<td><a href=\"javascript:void()\" id=\"edit_metadata\">Edit Metadata</a></td>\n");
        sb.append("\t\t</tr>\n");

        sb.append("\t</tbody>\n</table>\n");

        return Response.ok(sb.toString()).build();
    }

    @GET
    @Authenticated
    @Path("/{projectId}/metadataEditor")
    @Produces(MediaType.TEXT_HTML)
    public Response listMetadataEditorAsTable(@PathParam("projectId") int projectId) {
        StringBuilder sb = new StringBuilder();
        ProjectMinter project = new ProjectMinter();
        JSONObject metadata = project.getMetadata(projectId, userContext.getUser().getUsername());

        sb.append("<form id=\"submitForm\" method=\"POST\">\n");
        sb.append("<table>\n");
        sb.append("\t<tbody>\n");
        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td>Title</td>\n");
        sb.append(("\t\t\t<td><input type=\"text\" class=\"project_metadata\" name=\"title\" value=\""));
        sb.append(metadata.get("title"));
        sb.append("\"></td>\n\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td>Configuration File</td>\n");
        sb.append(("\t\t\t<td><input type=\"text\" class=\"project_metadata\" name=\"validationXml\" value=\""));
        sb.append(metadata.get("validationXml"));
        sb.append("\"></td>\n\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td>Public Project</td>\n");
        sb.append("\t\t\t<td><input type=\"checkbox\" name=\"public\"");
        if (metadata.get("public").equals("true")) {
            sb.append(" checked=\"checked\"");
        }
        sb.append("></td>\n\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td></td>\n");
        sb.append("\t\t\t<td class=\"error\" align=\"center\">");
        sb.append("</td>\n\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td></td>\n");
        sb.append(("\t\t\t<td><input id=\"metadataSubmit\" type=\"button\" value=\"Submit\">"));
        sb.append("</td>\n\t\t</tr>\n");
        sb.append("\t</tbody>\n");
        sb.append("</table>\n");
        sb.append("</form>\n");

        return Response.ok(sb.toString()).build();
    }

    @Deprecated
    @GET
    @Authenticated
    @Admin
    @Path("/{projectId}/users")
    @Produces(MediaType.TEXT_HTML)
    public Response listUsersAsTable(@PathParam("projectId") int projectId) {
        ProjectMinter p = new ProjectMinter();

        if (!p.isProjectAdmin(userContext.getUser().getUsername(), projectId)) {
            // only display system users to project admins
            throw new ForbiddenRequestException("You are not an admin to this project");
        }

        JSONObject response = p.getProjectUsers(projectId);
        Project project = projectService.getProjectWithMembers(projectId);
        List<User> projectMembers = project.getProjectMembers();

        List<User> allUsers = userService.getUsers();

        StringBuilder sb = new StringBuilder();

        sb.append("\t<form method=\"POST\">\n");

        sb.append("<table data-projectId=\"" + projectId + "\" data-projectTitle=\"" + response.get("projectTitle") + "\">\n");
        sb.append("\t<tr>\n");

        for (User member : projectMembers) {
            sb.append("\t<tr>\n");
            sb.append("\t\t<td>");
            sb.append(member.getUsername());
            sb.append("</td>\n");
            sb.append("\t\t<td><a id=\"remove_user\" data-userId=\"" + member.getUserId() + "\" data-username=\"" + member.getUsername() + "\" href=\"javascript:void();\">(remove)</a> ");
            sb.append("<a id=\"edit_profile\" data-username=\"" + member.getUsername() + "\" href=\"javascript:void();\">(edit)</a></td>\n");
            sb.append("\t</tr>\n");
        }

        sb.append("\t<tr>\n");
        sb.append("\t\t<td>Add User:</td>\n");
        sb.append("\t\t<td>");
        sb.append("<select name=userId>\n");
        sb.append("\t\t\t<option value=\"0\">Create New User</option>\n");

        for (User user : allUsers) {
            sb.append("\t\t\t<option value=\"" + user.getUserId() + "\">" + user.getUsername() + "</option>\n");
        }

        sb.append("\t\t</select></td>\n");
        sb.append("\t</tr>\n");
        sb.append("\t<tr>\n");
        sb.append("\t\t<td></td>\n");
        sb.append("\t\t<td><div class=\"error\" align=\"center\"></div></td>\n");
        sb.append("\t</tr>\n");
        sb.append("\t<tr>\n");
        sb.append("\t\t<td><input type=\"hidden\" name=\"projectId\" value=\"" + projectId + "\"></td>\n");
        sb.append("\t\t<td><input type=\"button\" value=\"Submit\" onclick=\"projectUserSubmit(\'" +
                ((String) response.get("projectTitle")).replaceAll(" ", "_") + '_' + projectId + "\')\"></td>\n");
        sb.append("\t</tr>\n");

        sb.append("</table>\n");
        sb.append("\t</form>\n");

        return Response.ok(sb.toString()).build();
    }

    @Deprecated
    @GET
    @Authenticated
    @Admin
    @Produces(MediaType.TEXT_HTML)
    @Path("/{projectId}/admin/expeditions/")
    public Response listExpeditionsAsTable(@PathParam("projectId") int projectId) {
        if (!projectService.isProjectAdmin(userContext.getUser(), projectId)) {
            throw new ForbiddenRequestException("You must be this project's admin in order to view its expeditions.");
        }

        Project project = projectService.getProjectWithExpeditions(projectId);

        StringBuilder sb = new StringBuilder();
        sb.append("<form method=\"POST\">\n");
        sb.append("<table>\n");
        sb.append("<tbody>\n");
        sb.append("\t<tr>\n");
        sb.append("\t\t<th>Username</th>\n");
        sb.append("\t\t<th>Expedition Title</th>\n");
        sb.append("\t\t<th>Public</th>\n");
        sb.append("\t</tr>\n");

        for (Expedition expedition : project.getExpeditions()) {
            sb.append("\t<tr>\n");
            sb.append("\t\t<td>");
            sb.append(expedition.getUser().getUsername());
            sb.append("</td>\n");
            sb.append("\t\t<td>");
            sb.append(expedition.getExpeditionTitle());
            sb.append("</td>\n");
            sb.append("\t\t<td><input name=\"");
            sb.append(expedition.getExpeditionId());
            sb.append("\" type=\"checkbox\"");
            if (expedition.isPublic()) {
                sb.append(" checked=\"checked\"");
            }
            sb.append("/></td>\n");
            sb.append("\t</tr>\n");
        }

        sb.append("\t<tr>\n");
        sb.append("\t\t<td></td>\n");
        sb.append("\t\t<td><input type=\"hidden\" name=\"projectId\" value=\"" + projectId + "\" /></td>\n");
        sb.append("\t\t<td><input id=\"expeditionForm\" type=\"button\" value=\"Submit\"></td>\n");
        sb.append("\t</tr>\n");

        sb.append("</tbody>\n");
        sb.append("</table>\n");
        sb.append("</form>\n");
        return Response.ok(sb.toString()).build();
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

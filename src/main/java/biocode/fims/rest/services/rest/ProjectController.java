package biocode.fims.rest.services.rest;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.bcid.ProjectMinter;
import biocode.fims.biscicol.query.BiscicolQueryUtils;
import biocode.fims.config.ConfigurationFileEsMapper;
import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.*;
import biocode.fims.elasticSearch.ElasticSearchIndexer;
import biocode.fims.elasticSearch.query.ElasticSearchFilterField;
import biocode.fims.entities.Expedition;
import biocode.fims.entities.Project;
import biocode.fims.entities.User;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.rest.SpringObjectMapper;
import biocode.fims.rest.filters.Admin;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.run.TemplateProcessor;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import biocode.fims.service.UserService;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.client.Client;
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
    private final Client esClient;

    @Autowired
    ProjectController(ExpeditionService expeditionService, FimsProperties props,
                      ProjectService projectService, UserService userService, Client esClient) {
        super(expeditionService, props, projectService);
        this.userService = userService;
        this.esClient = esClient;
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
        File configFile = new ConfigurationFileFetcher(projectId, defaultOutputDirectory(), true).getOutputFile();

        Mapping mapping = new Mapping();
        mapping.addMappingRules(configFile);

        ArrayNode filters = new SpringObjectMapper().createArrayNode();

        for (ElasticSearchFilterField f : BiscicolQueryUtils.getAvailableFilters(mapping)) {
            ObjectNode filter = filters.addObject();
            filter.put("field", f.getField());
            filter.put("displayName", f.getDisplayName());
        }

        return Response.ok(filters).build();
    }

    @GET
    @Path("/{projectId}/getDefinition/{columnName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDefinitions(@PathParam("projectId") int projectId,
                                   @PathParam("columnName") String columnName) {
        TemplateProcessor t = new TemplateProcessor(projectId, defaultOutputDirectory(), props.naan());
        StringBuilder output = new StringBuilder();

        Iterator attributes = t.getMapping().getAllAttributes(t.getMapping().getDefaultSheetName()).iterator();
        // Get a list of rules for the first digester.Worksheet instance
        Worksheet sheet = t.getValidation().getWorksheets().get(0);

        List<Rule> rules = sheet.getRules();


        while (attributes.hasNext()) {
            Attribute a = (Attribute) attributes.next();
            String column = a.getColumn();
            if (columnName.trim().equals(column.trim())) {
                // The column name
                output.append("<b>Column Name: " + columnName + "</b><p>");

                // URI
                if (a.getUri() != null) {
                    output.append("URI = " +
                            "<a href='" + a.getUri() + "' target='_blank'>" +
                            a.getUri() +
                            "</a><br>\n");
                }
                // Defined_by
                if (a.getDefined_by() != null) {
                    output.append("Defined_by = " +
                            "<a href='" + a.getDefined_by() + "' target='_blank'>" +
                            a.getDefined_by() +
                            "</a><br>\n");
                }

                // Definition
                if (a.getDefinition() != null && !a.getDefinition().trim().equals("")) {
                    output.append("<p>\n" +
                            "<b>Definition:</b>\n" +
                            "<p>" + a.getDefinition() + "\n");
                } else {
                    output.append("<p>\n" +
                            "<b>Definition:</b>\n" +
                            "<p>No custom definition available\n");
                }

                // Synonyms
                if (a.getSynonyms() != null && !a.getSynonyms().trim().equals("")) {
                    output.append("<p>\n" +
                            "<b>Synonyms:</b>\n" +
                            "<p>" + a.getSynonyms() + "\n");
                }

                // Synonyms
                if (a.getDataformat() != null && !a.getDataformat().trim().equals("")) {
                    output.append("<p>\n" +
                            "<b>Data Formatting Instructions:</b>\n" +
                            "<p>" + a.getDataformat() + "\n");
                }

                // Rules
                Iterator it = rules.iterator();
                StringBuilder ruleValidations = new StringBuilder();
                while (it.hasNext()) {

                    Rule r = (Rule) it.next();
                    r.setDigesterWorksheet(sheet);

                    if (r != null) {
                        biocode.fims.digester.List sList = t.getValidation().findList(r.getList());

                        // Convert to native state (without underscores)
                        String ruleColumn = r.getColumn();

                        if (ruleColumn != null) {
                            // Match column names with or without underscores
                            if (ruleColumn.replace("_", " ").equals(column) ||
                                    ruleColumn.equals(column)) {
                                ruleValidations.append(printRuleMetadata(r, sList));
                            }
                        }
                    }
                }
                if (!ruleValidations.toString().equals("")) {
                    output.append("<p>\n" +
                            "<b>Validation Rules:</b>\n<p>");
                    output.append(ruleValidations.toString());
                }

                return Response.ok(output.toString()).build();
            }
        }

        return Response.ok("No definition found for " + columnName).build();
    }

    /**
     * Print ruleMetadata
     *
     * @param sList We pass in a List of fields we want to associate with this rule
     * @return
     */
    private String printRuleMetadata(Rule r, biocode.fims.digester.List sList) {
        StringBuilder output = new StringBuilder();
        output.append("<li>\n");
        //
        if (r.getType().equals("checkInXMLFields")) {
            r.setType("Lookup Value From List");
        }
        // Display the Rule type
        output.append("\t<li>type: " + r.getType() + "</li>\n");
        // Display warning levels
        output.append("\t<li>level: " + r.getLevel() + "</li>\n");
        // Display values
        if (r.getValue() != null) {
            try {
                output.append("\t<li>value: " + URLDecoder.decode(r.getValue(), "utf-8") + "</li>\n");
            } catch (UnsupportedEncodingException e) {
                output.append("\t<li>value: " + r.getValue() + "</li>\n");
                logger.warn("UnsupportedEncodingException", e);
            }
        }
        // Display fields
        // Convert XML Field values to a Stringified list
        java.util.List listFields;
        if (sList != null && sList.getFields().size() > 0) {
            listFields = sList.getFields();
        } else {
            listFields = r.getFields();
        }
        Iterator it;
        try {
            it = listFields.iterator();
        } catch (NullPointerException e) {
            logger.warn("NullPointerException", e);
            return output.toString();
        }
        // One or the other types of list need data
        if (!it.hasNext())
            return output.toString();

        output.append("\t<li>list: \n");

        // Look at the Fields
        output.append("\t\t<ul>\n");

        if (it != null) {
            while (it.hasNext()) {
                String field = ((Field) it.next()).getValue();
                //String field = (String) it.next();
                output.append("\t\t\t<li>" + field + "</li>\n");
            }
        }
        output.append("\t\t</ul>\n");
        output.append("\t</li>\n");

        output.append("</li>\n");
        return output.toString();
    }

    @GET
    @Path("/{projectId}/attributes")
    @Produces(MediaType.TEXT_HTML)
    public Response getAttributes(@PathParam("projectId") int projectId) {
        TemplateProcessor t = new TemplateProcessor(projectId, defaultOutputDirectory(), props.naan());
        LinkedList<String> requiredColumns = t.getRequiredColumns("error");
        LinkedList<String> desiredColumns = t.getRequiredColumns("warning");
        // Use TreeMap for natural sorting of groups
        Map<String, StringBuilder> groups = new TreeMap<String, StringBuilder>();

        //StringBuilder output = new StringBuilder();
        // A list of names we've already added
        ArrayList addedNames = new ArrayList();
        Iterator attributes = t.getMapping().getAllAttributes(t.getMapping().getDefaultSheetName()).iterator();
        while (attributes.hasNext()) {
            Attribute a = (Attribute) attributes.next();

            StringBuilder thisOutput = new StringBuilder();
            // Set the column name
            String column = a.getColumn();
            String group = a.getGroup();
            String uri = a.getUri();

            // Check that this name hasn't been read already.  This is necessary in some situations where
            // column names are repeated for different entities in the configuration file
            if (!addedNames.contains(column)) {
                // Set boolean to tell us if this is a requiredColumn
                Boolean aRequiredColumn = false, aDesiredColumn = false;
                if (requiredColumns == null) {
                    aRequiredColumn = false;
                } else if (requiredColumns.contains(a.getColumn())) {
                    aRequiredColumn = true;
                }
                if (desiredColumns == null) {
                    aDesiredColumn = false;
                } else if (desiredColumns.contains(a.getColumn())) {
                    aDesiredColumn = true;
                }


                // Construct the checkbox text
                thisOutput.append("<input type='checkbox' class='check_boxes' value='" + column + "' data-uri='");
                thisOutput.append(uri);
                thisOutput.append("'");

                // If this is a required column then make it checked (and immutable)
                if (aRequiredColumn)
                    thisOutput.append(" checked disabled");
                else if (aDesiredColumn)
                    thisOutput.append(" checked");

                // Close tag and insert Definition link
                thisOutput.append(">" + column + " \n" +
                        "<a href='#' class='def_link' name='" + column + "'>DEF</a>\n" + "<br>\n");

                // Fetch any existing content for this key
                if (group == null || group.equals("")) {
                    group = "Default Group";
                }
                StringBuilder existing = groups.get(group);

                // Append (not required) or Insert (required) the new content onto any existing in this key
                if (existing == null) {
                    existing = thisOutput;
                } else {
                    if (aRequiredColumn) {
                        existing.insert(0, thisOutput);
                    } else {
                        existing.append(thisOutput);
                    }
                }
                groups.put(group, existing);

                //groups.put(group, existing == null ? thisOutput : existing.append(thisOutput));

            }

            // Now that we've added this to the output, add it to the ArrayList so we don't add it again
            addedNames.add(column);
        }

        // Iterate through any defined groups, which makes the template processor easier to navigate
        Iterator it = groups.entrySet().iterator();
        StringBuilder output = new StringBuilder();
        output.append("<a href='#' id='select_all'>Select ALL</a> | ");
        output.append("<a href='#' id='select_none'>Select NONE</a> | ");
        output.append("<a href='#' onclick='saveTemplateConfig()'>Save</a>");
        output.append("<script>" +
                "$('#select_all').click(function(event) {\n" +
                "      // Iterate each checkbox\n" +
                "      $(':checkbox').each(function() {\n" +
                "          this.checked = true;\n" +
                "      });\n" +
                "  });\n" +
                "$('#select_none').click(function(event) {\n" +
                "    $(':checkbox').each(function() {\n" +
                "       if (!$(this).is(':disabled')) {\n" +
                "          this.checked = false;}\n" +
                "      });\n" +
                "});" +
                "</script>");

        int count = 0;
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            String groupName;

            try {
                groupName = pairs.getKey().toString();
            } catch (NullPointerException e) {
                groupName = "Default Group";
            }
            if (groupName.equals("") || groupName.equals("null")) {
                groupName = "Default Group";
            }

            // Anchors cannot have spaces in the name so we replace them with underscores
            String massagedGroupName = groupName.replaceAll(" ", "_");
            if (!pairs.getValue().toString().equals("")) {
                output.append("<div class=\"panel panel-default\">");
                output.append("<div class=\"panel-heading\"> " +
                        "<h4 class=\"panel-title\"> " +
                        "<a class=\"accordion-toggle\" data-toggle=\"collapse\" data-parent=\"#accordion\" href=\"#" + massagedGroupName + "\">" + groupName + "</a> " +
                        "</h4> " +
                        "</div>");
                output.append("<div id=\"" + massagedGroupName + "\" class=\"panel-collapse collapse");
                // Make the first element open initially
                if (count == 0) {
                    output.append(" in");
                }
                output.append("\">\n" +
                        "                <div class=\"panel-body\">\n" +
                        "                    <div id=\"" + massagedGroupName + "\" class=\"panel-collapse collapse in\">");
                output.append(pairs.getValue().toString());
                output.append("\n</div></div></div></div>");
            }

            it.remove(); // avoids a ConcurrentModificationException
            count++;
        }
        return Response.ok(output.toString()).build();
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

    @POST
    @Path("/createExcel/")
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createExcel(
            @FormParam("fields") List<String> fields,
            @FormParam("projectId") Integer projectId) {

        // Create the template processor which handles all functions related to the template, reading, generation
        TemplateProcessor t = new TemplateProcessor(projectId, defaultOutputDirectory(), props.naan());

        // Set the default sheet-name
        String defaultSheetname = t.getMapping().getDefaultSheetName();

        File file = t.createExcelFile(defaultSheetname, defaultOutputDirectory(), fields);

        // Catch a null file and return 204
        if (file == null)
            return Response.status(204).build();

        // Return response
        Response.ResponseBuilder response = Response.ok(file);
        response.header("Content-Disposition",
                "attachment; filename=" + file.getName());
        return response.build();
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

    @Override
    @GET
    @Path("/{projectId}/config/refreshCache")
    public Response refreshCache(@PathParam("projectId") Integer projectId) {
        File configFile = new ConfigurationFileFetcher(projectId, defaultOutputDirectory(), false).getOutputFile();

        ElasticSearchIndexer indexer = new ElasticSearchIndexer(esClient);
        JSONObject mapping = ConfigurationFileEsMapper.convert(configFile);
        indexer.updateMapping(projectId, mapping);

        return Response.noContent().build();
    }
}

package services;

import biocode.fims.FimsService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * REST services dealing with projects
 */
@Path("projects")
public class Projects extends FimsService {

    @POST
    @Path("/{projectId}/admin/addUser/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addUser(MultivaluedMap params) {
        return fimsConnector.createPOSTConnnection(fimsCoreRoot + "id/projectService/addUser/",
                fimsConnector.getPostParams(params));
    }

    @GET
    @Path("/{projectId}/admin/removeUser/{userId}")
    public Response removeUser(@PathParam("projectId") int projectId,
                               @PathParam("userId") int userId) {
        return fimsConnector.createGETConnection(fimsCoreRoot + "id/projectService/removeUser/" + projectId + "/" + userId);
    }

    @GET
    @Path("/{projectId}/getLatLongColumns")
    public Response getLatLongColumns(@PathParam("projectId") int projectId) {
        return fimsConnector.createGETConnection(fimsCoreRoot + "id/projectService/getLatLongColumns/" + projectId);
    }

    @GET
    @Path("/{projectId}/expeditions")
    public Response listExpeditions(@PathParam("projectId") int projectId) {
        return fimsConnector.createGETConnection(fimsCoreRoot + "id/expeditionService/list/" + projectId);
    }

    @GET
    @Path("/admin/list")
    public Response listAdminProjects() {
        return fimsConnector.createGETConnection(fimsCoreRoot + "id/projectService/admin/list");
    }

    @GET
    @Path("/listUserProjects")
    public Response listUserProjects() {
        return fimsConnector.createGETConnection(fimsCoreRoot + "id/projectService/listUserProjects");
    }

    @POST
    @Path("/{projectId}/metadata/update")
    public Response updateMetadata(@PathParam("projectId") int projectId,
                                   MultivaluedMap<String, String> params) {
        return fimsConnector.createPOSTConnnection(fimsCoreRoot + "id/projectService/updateConfig/" + projectId,
                fimsConnector.getPostParams(params));
    }

    @POST
    @Path("/query/kml")
    public Response queryKML(MultivaluedMap<String, String> params) {
        return fimsConnector.createPOSTConnnection(fimsCoreRoot + "biocode-fims/rest/query/kml",
                fimsConnector.getPostParams(params));
    }

    @POST
    @Path("/query/excel")
    public Response queryExcel(MultivaluedMap<String, String> params) {
        return fimsConnector.createPOSTConnnection(fimsCoreRoot + "biocode-fims/rest/query/excel",
                fimsConnector.getPostParams(params));
    }

    @POST
    @Path("/query/json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response queryJSON(MultivaluedMap<String, String> params) {
        return fimsConnector.createPOSTConnnection(
                fimsCoreRoot + "biocode-fims/rest/query/json/", fimsConnector.getPostParams(params));
    }

    @GET
    @Path("/{projectId}/filterOptions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFilterOptions(@PathParam("projectId") int projectId) {
        return fimsConnector.createGETConnection(fimsCoreRoot + "biocode-fims/rest/mapping/filterOptions/" + projectId);
    }

    @GET
    @Path("/{projectId}/graphs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGraphs(@PathParam("projectId") int projectId) {
        return fimsConnector.createGETConnection(fimsCoreRoot + "id/projectService/graphs/" + projectId);
    }

    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listProjects() {
        return fimsConnector.createGETConnection(fimsCoreRoot + "id/projectService/list/");
    }

    @GET
    @Path("/{projectId}/abstract")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAbstract(@PathParam("projectId") int projectId) {
        return fimsConnector.createGETConnection(
                fimsCoreRoot + "biocode-fims/rest/templates/abstract/" + projectId);
    }

    @GET
    @Path("/{projectId}/removeConfig/{configName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeConfig(@PathParam("configName") String configName,
                                 @PathParam("projectId") int projectId) {
        return fimsConnector.createGETConnection(
                fimsCoreRoot + "id/projectService/removeTemplateConfig/" + projectId + "/" + configName);
    }

    @POST
    @Path("/{projectId}/saveConfig/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveConfig(MultivaluedMap<String, String> params) {
        return fimsConnector.createPOSTConnnection(
                fimsCoreRoot + "id/projectService/saveTemplateConfig/", fimsConnector.getPostParams(params));
    }

    @GET
    @Path("/{projectId}/getConfig/{configName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfig(@PathParam("configName") String configName,
                               @PathParam("projectId") int projectId) {
        return fimsConnector.createGETConnection(
                fimsCoreRoot + "id/projectService/getTemplateConfig/" + projectId + "/" + configName);
    }

    @GET
    @Path("/{projectId}/getConfigs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfigs(@PathParam("projectId") int projectId) {
        return fimsConnector.createGETConnection(
                fimsCoreRoot + "id/projectService/getTemplateConfigs/" + projectId);
    }

    @GET
    @Path("/{projectId}/getDefinition/{columnName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDefinitions(@PathParam("projectId") int projectId,
                                  @PathParam("columnName") String columnName) {
        JSONObject definition = fimsConnector.getJSONObject(
                fimsCoreRoot + "biocode-fims/rest/templates/definition/" + projectId + "/" + columnName);
        StringBuilder sb = new StringBuilder();
        sb.append("<b>Column Name: " + definition.get("columnName") + "</b><p>");
        // URI
        if (!definition.get("uri").equals("")) {
            sb.append("URI = " +
                    "<a href='" + definition.get("uri") + "' target='_blank'>" +
                    definition.get("uri") +
                    "</a><br>\n");
        }
        // Defined_by
        if (!definition.get("defined_by").equals("")) {
            sb.append("Defined_by = " +
                    "<a href='" + definition.get("defined_by") + "' target='_blank'>" +
                    definition.get("defined_by") +
                    "</a><br>\n");
        }

        // Definition
        if (!definition.get("definition").equals("")) {
            sb.append("<p>\n" +
                    "<b>Definition:</b>\n" +
                    "<p>" + definition.get("definition") + "\n");
        } else {
            sb.append("<p>\n" +
                    "<b>Definition:</b>\n" +
                    "<p>No custom definition available\n");
        }

        // Synonyms
        if (!definition.get("synonyms").equals("")) {
            sb.append("<p>\n" +
                    "<b>Synonyms:</b>\n" +
                    "<p>" + definition.get("synonyms") + "\n");
        }

        // Synonyms
        if (!definition.get("dataFormat").equals("")) {
            sb.append("<p>\n" +
                    "<b>Data Formatting Instructions:</b>\n" +
                    "<p>" + definition.get("dataFormat") + "\n");
        }
        JSONArray rules = (JSONArray) definition.get("rules");

        if (!rules.isEmpty()) {
            sb.append("<p>\n" +
                    "<b>Validation Rules:</b>\n<p>");
        }

        for (Object r : rules) {
            JSONObject rule = (JSONObject) r;
            sb.append("<li>\n");
            // Display the Rule type
            sb.append("\t<li>type: " + rule.get("type") + "</li>\n");
            // Display warning levels
            sb.append("\t<li>level: " + rule.get("level") + "</li>\n");
            if (rule.containsKey("value")) {
                sb.append("\t<li>value: " + rule.get("value") + "</li>\n");
            }
            JSONArray list = (JSONArray) rule.get("list");
            if (!list.isEmpty()) {
                sb.append("\t<li>list: \n");

                // Look at the Fields
                sb.append("\t\t<ul>\n");

                for (Object field : list) {
                    sb.append("\t\t\t<li>" + field + "</li>\n");
                }
                sb.append("\t\t</ul>\n");
                sb.append("\t</li>\n");

                sb.append("</li>\n");
            }
        }
        return Response.ok(sb.toString()).build();
    }

    @GET
    @Path("/{projectId}/attributes")
    @Produces(MediaType.TEXT_HTML)
    public Response getAttributes(@PathParam("projectId") int projectId) {
        JSONObject attributes = fimsConnector.getJSONObject(
                fimsCoreRoot + "biocode-fims/rest/templates/attributes/" + projectId);
        StringBuilder sb = new StringBuilder();
        sb.append("<a href='#' id='select_all'>Select ALL</a> | ");
        sb.append("<a href='#' id='select_none'>Select NONE</a> | ");
        sb.append("<a href='#' onclick='saveTemplateConfig()'>Save</a>");
        sb.append("<script>" +
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
        for (Object groupName: attributes.keySet()) {
            String massagedGroupName = groupName.toString().replaceAll(" ", "_");
            JSONArray columns = (JSONArray) attributes.get(groupName);
                sb.append("<div class=\"panel panel-default\">");
                sb.append("<div class=\"panel-heading\"> " +
                        "<h4 class=\"panel-title\"> " +
                        "<a class=\"accordion-toggle\" data-toggle=\"collapse\" data-parent=\"#accordion\" href=\"#" + massagedGroupName + "\">" + groupName + "</a> " +
                        "</h4> " +
                        "</div>");
                sb.append("<div id=\"" + massagedGroupName + "\" class=\"panel-collapse collapse");
                // Make the first element open initially
                if (count == 0) {
                    sb.append(" in");
                }
                sb.append("\">\n" +
                        "                <div class=\"panel-body\">\n" +
                        "                    <div id=\"" + massagedGroupName + "\" class=\"panel-collapse collapse in\">");
            for (Object c: columns) {
                JSONObject columnObject = (JSONObject) c;
                for (Object columnName: columnObject.keySet()) {
                    JSONObject column = (JSONObject) columnObject.get(columnName);
                    sb.append("<input type='checkbox' class='check_boxes' value='" + columnName + "' data-uri='");
                    sb.append(column.get("uri"));
                    sb.append("'");

                    // If this is a required column then make it checked (and immutable)
                    if (column.get("level").equals("required"))
                        sb.append(" checked disabled");
                    else if (column.get("level").equals("desired"))
                        sb.append(" checked");

                    // Close tag and insert Definition link
                    sb.append(">" + columnName + " \n" +
                            "<a href='#' class='def_link' name='" + columnName + "'>DEF</a>\n" + "<br>\n");
                }
            }
            sb.append("\n</div></div></div></div>");
            count++;
        }

        return Response.ok(sb.toString()).build();
    }

    @GET
    @Path("/{projectId}/metadata")
    public Response listMetadataAsTable(@PathParam("projectId") int projectId) {
        JSONObject metadata = fimsConnector.getJSONObject(fimsCoreRoot + "id/projectService/metadata/" + projectId);
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
    @Path("/{projectId}/metadataEditor")
    public Response listMetadataEditorAsTable(@PathParam("projectId") int projectId) {
        StringBuilder sb = new StringBuilder();
        JSONObject metadata = fimsConnector.getJSONObject(fimsCoreRoot + "id/projectService/metadata/" + projectId);

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

    @GET
    @Path("/{projectId}/users")
    public Response listUsersAsTable(@PathParam("projectId") int projectId) {
        JSONObject response = fimsConnector.getJSONObject(fimsCoreRoot + "id/projectService/getUsers/" + projectId);
        JSONArray projectUsers = (JSONArray) response.get("users");
        JSONArray users = fimsConnector.getJSONArray(fimsCoreRoot + "id/userService/list");
        StringBuilder sb = new StringBuilder();

        sb.append("\t<form method=\"POST\">\n");

        sb.append("<table data-projectId=\"" + projectId + "\" data-projectTitle=\"" + response.get("projectTitle") + "\">\n");
        sb.append("\t<tr>\n");

        for (Object u: projectUsers) {
            JSONObject user = (JSONObject) u;
            String username = (String) user.get("username");
            sb.append("\t<tr>\n");
            sb.append("\t\t<td>");
            sb.append(username);
            sb.append("</td>\n");
            sb.append("\t\t<td><a id=\"remove_user\" data-userId=\"" + user.get("userId") + "\" data-username=\"" + username + "\" href=\"javascript:void();\">(remove)</a> ");
            sb.append("<a id=\"edit_profile\" data-username=\"" + username + "\" href=\"javascript:void();\">(edit)</a></td>\n");
            sb.append("\t</tr>\n");
        }

        sb.append("\t<tr>\n");
        sb.append("\t\t<td>Add User:</td>\n");
        sb.append("\t\t<td>");
        sb.append("<select name=userId>\n");
        sb.append("\t\t\t<option value=\"0\">Create New User</option>\n");

        for (Object u: users) {
            JSONObject user = (JSONObject) u;
            sb.append("\t\t\t<option value=\"" + user.get("userId") + "\">" + user.get("username") + "</option>\n");
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

    @GET
    @Path("/{projectId}/admin/expeditions/")
    public Response listExpeditionsAsTable(@PathParam("projectId") int projectId) {
        JSONArray expeditions = fimsConnector.getJSONArray(fimsCoreRoot + "id/expeditionService/admin/list/" + projectId);
        StringBuilder sb = new StringBuilder();
        sb.append("<form method=\"POST\">\n");
        sb.append("<table>\n");
        sb.append("<tbody>\n");
        sb.append("\t<tr>\n");
        sb.append("\t\t<th>Username</th>\n");
        sb.append("\t\t<th>Expedition Title</th>\n");
        sb.append("\t\t<th>Public</th>\n");
        sb.append("\t</tr>\n");

        for (Object e: expeditions) {
            JSONObject expedition = (JSONObject) e;
            sb.append("\t<tr>\n");
            sb.append("\t\t<td>");
            sb.append(expedition.get("username"));
            sb.append("</td>\n");
            sb.append("\t\t<td>");
            sb.append(expedition.get("expeditionTitle"));
            sb.append("</td>\n");
            sb.append("\t\t<td><input name=\"");
            sb.append(expedition.get("expeditionId"));
            sb.append("\" type=\"checkbox\"");
            if (Boolean.valueOf(expedition.get("public").toString())) {
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
}

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

    @GET
    @Path("/{projectId}/expeditions")
    public Response listExpeditions(@PathParam("projectId") int projectId) {
        return fimsConnector.createGETConnection(fimsCoreRoot + "id/expeditionService/list/" + projectId);
    }

    @GET
    @Path("/listUserProjects")
    public Response listUserProjects() {
        return fimsConnector.createGETConnection(fimsCoreRoot + "id/projectService/listUserProjects");
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

    public static void main(String[] args) {
        Projects p = new Projects();
        p.getAttributes(1);
    }
}

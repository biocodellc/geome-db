package services;

import biocode.fims.FimsService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Set;

/**
 * REST services dealing with projects
 */
@Path("projects")
public class Projects extends FimsService {

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
    @Path("/{projectId}/attributes")
    @Produces(MediaType.TEXT_HTML)
    public Response getAttributes(@PathParam("projectId") int projectId) {
        JSONObject attributes = fimsConnector.getJSONObject(
                fimsCoreRoot + "biocode-fims/rest/templates/attributes/" + projectId);


        //TODO need to finish this service to display the attribute checkbox html
        Set groups = attributes.entrySet();
        for (Object g: attributes.keySet()) {
            JSONArray group = (JSONArray) g;
            for (Object c: group) {
                JSONObject column = (JSONObject) c;
            }

        }

        return Response.ok().build();
    }

    public static void main(String[] args) {
        Projects p = new Projects();
        p.getAttributes(1);
    }
}

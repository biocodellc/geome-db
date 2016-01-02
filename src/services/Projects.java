package services;

import biocode.fims.FimsService;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.ServerErrorException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

/**
 * REST services dealing with projects
 */
@Path("projects")
public class Projects extends FimsService {

    @POST
    @Path("/query/kml")
    public Response queryKML(MultivaluedMap<String, String> params) {
        Client client = ClientBuilder.newClient();
        WebTarget t = client.target(fimsCoreRoot).path("biocode-fims/rest/query/kml");
        return t.request().post(Entity.entity(params, MediaType.APPLICATION_FORM_URLENCODED_TYPE), Response.class);
    }

    @POST
    @Path("/query/excel")
    public Response queryExcel(MultivaluedMap<String, String> params) {
        Client client = ClientBuilder.newClient();
        WebTarget t = client.target(fimsCoreRoot).path("biocode-fims/rest/query/excel");
        return t.request().post(Entity.entity(params, MediaType.APPLICATION_FORM_URLENCODED_TYPE), Response.class);
    }

    @POST
    @Path("/query/json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response queryJSON(MultivaluedMap<String, String> params) {
        try {
            String response = fimsConnector.createPOSTConnnection(
                    new URL(fimsCoreRoot + "biocode-fims/rest/query/json/"), fimsConnector.getPostParams(params));
            return Response.status(fimsConnector.getResponseCode()).entity(response).build();
        } catch (MalformedURLException e) {
            throw new ServerErrorException(e);
        }
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
        try {
            String response = fimsConnector.createGETConnection(new URL(
                    fimsCoreRoot + "id/projectService/graphs/" + projectId));
            return Response.status(fimsConnector.getResponseCode()).entity(response).build();
        } catch (MalformedURLException e) {
            throw new ServerErrorException(e);
        }
    }

    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listProjects() {

        try {
            String response = fimsConnector.createGETConnection(new URL(fimsCoreRoot + "id/projectService/list/"));

            return Response.status(fimsConnector.getResponseCode()).entity(response).build();
        } catch (MalformedURLException e) {
            throw new ServerErrorException(e);
        }
    }

    @GET
    @Path("/{projectId}/abstract")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAbstract(@PathParam("projectId") int projectId) {
        try {
            return Response.ok(fimsConnector.createGETConnection(new URL(
                    fimsCoreRoot + "biocode-fims/rest/templates/abstract/" + projectId))).build();
        } catch (MalformedURLException e) {
            throw new ServerErrorException(e);
        }
    }

    @GET
    @Path("/{projectId}/removeConfig/{configName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeConfig(@PathParam("configName") String configName,
                                 @PathParam("projectId") int projectId) {
        try {
            return Response.ok(fimsConnector.createGETConnection(new URL(
                    fimsCoreRoot + "id/projectService/removeTemplateConfig/" + projectId + "/" + configName))).build();
        } catch (MalformedURLException e) {
            throw new ServerErrorException(e);
        }
    }

    @GET
    @Path("/{projectId}/getConfig/{configName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfig(@PathParam("configName") String configName,
                               @PathParam("projectId") int projectId) {
        try {
            return Response.ok(fimsConnector.createGETConnection(new URL(
                    fimsCoreRoot + "id/projectService/getTemplateConfig/" + projectId + "/" + configName))).build();
        } catch (MalformedURLException e) {
            throw new ServerErrorException(e);
        }
    }

    @GET
    @Path("/{projectId}/getConfigs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfigs(@PathParam("projectId") int projectId) {
        try {
            return Response.ok(fimsConnector.createGETConnection(new URL(
                    fimsCoreRoot + "id/projectService/getTemplateConfigs/" + projectId))).build();
        } catch (MalformedURLException e) {
            throw new ServerErrorException(e);
        }
    }

    @GET
    @Path("/{projectId}/attributes")
    @Produces(MediaType.TEXT_HTML)
    public Response getAttributes(@PathParam("projectId") int projectId) {
        try {
            fimsCoreRoot = "http://localhost:8080/";
            JSONObject attributes = (JSONObject) JSONValue.parse(fimsConnector.createGETConnection(new URL(
                    fimsCoreRoot + "biocode-fims/rest/templates/attributes/" + projectId)));

            //TODO need to finish this service to display the attribute checkbox html
            Set groups = attributes.entrySet();
            for (Object g: attributes.keySet()) {
                JSONArray group = (JSONArray) g;
                for (Object c: group) {
                    JSONObject column = (JSONObject) c;
                }

            }

        } catch (MalformedURLException e) {
            throw new ServerErrorException(e);
        }
        return Response.ok().build();
    }

    public static void main(String[] args) {
        Projects p = new Projects();
        p.getAttributes(1);
    }
}

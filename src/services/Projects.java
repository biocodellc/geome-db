package services;

import biocode.fims.FimsConnector;
import biocode.fims.FimsService;

import javax.servlet.ServletException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URL;

/**
 * REST services dealing with projects
 */
@Path("projects")
public class Projects extends FimsService {


    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listProjects()
            throws IOException, ServletException {

        FimsConnector fimsConnector = new FimsConnector(clientId, clientSecret);
        String response = fimsConnector.createGETConnection(new URL(fimsCoreRoot + "id/projectService/list/"));

        return Response.status(fimsConnector.getResponseCode()).entity(response).build();
    }
}

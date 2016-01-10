package services;

import biocode.fims.FimsService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * rest class to validate and upload datasets
 */
@Path("validate")
public class Validate extends FimsService {
    @POST
    @Path("/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validate(MultivaluedMap params) {
        return fimsConnector.createPOSTConnnection(fimsCoreRoot + "biocode-fims/rest/validate",
                fimsConnector.getPostParams(params));
    }

    @GET
    @Path("/continue")
    @Produces(MediaType.APPLICATION_JSON)
    public Response continueUpload(@QueryParam("createExpedition") @DefaultValue("false") Boolean createExpedition) {
        return fimsConnector.createGETConnection(
                fimsCoreRoot + "biocode-fims/rest/validate/continue?createExpedition=" + createExpedition);
    }

    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatus() {
        return fimsConnector.createGETConnection(fimsCoreRoot + "biocode-fims/rest/validate/status");
    }
}

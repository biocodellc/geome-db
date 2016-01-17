package services;

import biocode.fims.bcid.Renderer.RDFRenderer;
import biocode.fims.bcid.Resolver;
import biocode.fims.rest.FimsService;
import utils.HTMLTableRenderer;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST services dealing with projects
 */
@Path("metadata")
public class BcidMetadata extends FimsService {
    @PathParam("scheme") String scheme;

    @GET
    @Path("{scheme}:/{naan}/{shoulderPlusIdentifier}")
    @Produces(MediaType.TEXT_HTML)
    public Response resolver(
            @PathParam("naan") String naan,
            @PathParam("shoulderPlusIdentifier") String shoulderPlusIdentifier,
            @HeaderParam("Accept") String accept) {
        shoulderPlusIdentifier = shoulderPlusIdentifier.trim();

        // Structure the Bcid element from path parameters
        String element = scheme + ":" + "/" + naan + "/" + shoulderPlusIdentifier;

        // Return an appropriate response based on the Accepts header that was passed in.
        Resolver r = new Resolver(element);
        if (accept.equalsIgnoreCase("application/rdf+xml")) {
            // Return RDF when the Accepts header specifies rdf+xml
            String response = r.printMetadata(new RDFRenderer());
            r.close();
            return Response.ok(response).build();
        } else {
            String response = r.printMetadata(new HTMLTableRenderer(userId, r));
            r.close();
            return Response.ok(response).build();
        }
    }
}

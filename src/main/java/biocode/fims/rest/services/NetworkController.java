package biocode.fims.rest.services;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.service.NetworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * network API endpoints
 *
 * @resourceTag Networks
 */
@Controller
@Path("networks")
@Produces({MediaType.APPLICATION_JSON})
public class NetworkController extends BaseNetworksController {

    @Context
    private ServletContext context;

    @Autowired
    NetworkController(NetworkService networkService, FimsProperties props) {
        super(networkService, props);
    }
}

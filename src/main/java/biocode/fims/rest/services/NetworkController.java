package biocode.fims.rest.services;

import biocode.fims.application.config.GeomeProperties;
import biocode.fims.service.NetworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * network API endpoints
 *
 * @resourceTag Network
 */
@Controller
@Path("network")
@Produces({MediaType.APPLICATION_JSON})
public class NetworkController extends BaseNetworksController {

    @Autowired
    NetworkController(GeomeProperties props) {
        super(props);
    }
}


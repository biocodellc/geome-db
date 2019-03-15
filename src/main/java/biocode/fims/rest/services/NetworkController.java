package biocode.fims.rest.services;

import biocode.fims.application.config.GeomeProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * network API endpoints
 *
 * @resourceTag Network
 */
@Controller
@Path("network")
@Produces({MediaType.APPLICATION_JSON})
@Singleton
public class NetworkController extends BaseNetworksController {

    @Autowired
    NetworkController(GeomeProperties props) {
        super(props);
    }
}


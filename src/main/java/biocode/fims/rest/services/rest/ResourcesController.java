package biocode.fims.rest.services.rest;

import biocode.fims.application.config.FimsProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.Path;

/**
 * @author RJ Ewing
 */
@Controller
@Path("resourceTypes")
public class ResourcesController extends FimsAbstractResourceController {

    @Autowired
    ResourcesController(FimsProperties props) {
        super(props);
    }
}

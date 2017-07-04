package biocode.fims.rest.services.rest;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.service.OAuthProviderService;
import biocode.fims.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.Path;

/**
 * @author RJ Ewing
 */
@Controller
@Path("authenticationService")
public class AuthenticationController extends FimsAbstractAuthenticationController {
    @Autowired
    public AuthenticationController(OAuthProviderService oAuthProviderService, UserService userService, FimsProperties props) {
        super(oAuthProviderService, userService, props);
    }
}

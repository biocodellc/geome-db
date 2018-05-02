package biocode.fims.rest.services;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.rest.services.BaseOAuthController;
import biocode.fims.service.OAuthProviderService;
import biocode.fims.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.Path;

/**
 * @author RJ Ewing
 */
@Controller
@Path("oauth")
public class OAuthController extends BaseOAuthController {
    @Autowired
    public OAuthController(OAuthProviderService oAuthProviderService, UserService userService, FimsProperties props) {
        super(oAuthProviderService, userService, props);
    }
}

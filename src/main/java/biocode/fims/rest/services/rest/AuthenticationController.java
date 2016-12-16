package biocode.fims.rest.services.rest;

import biocode.fims.service.OAuthProviderService;
import biocode.fims.service.UserService;
import biocode.fims.settings.SettingsManager;
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
    public AuthenticationController(OAuthProviderService oAuthProviderService, UserService userService, SettingsManager settingsManager) {
        super(oAuthProviderService, userService, settingsManager);
    }
}

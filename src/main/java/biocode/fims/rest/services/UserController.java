package biocode.fims.rest.services;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.service.ProjectService;
import biocode.fims.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.inject.Singleton;
import javax.ws.rs.Path;

/**
 * user API endpoints
 *
 * @resourceTag Users
 */
@Controller
@Path("users")
@Singleton
public class UserController extends BaseUserController {

    @Autowired
    UserController(UserService userService, ProjectService projectService, FimsProperties props) {
        super(userService, projectService, props);
    }
}

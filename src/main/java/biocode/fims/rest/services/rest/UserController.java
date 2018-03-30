package biocode.fims.rest.services.rest;

import biocode.fims.models.User;
import biocode.fims.application.config.FimsProperties;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.service.UserService;
import biocode.fims.utils.EmailUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST services dealing with user management
 *
 * @resourceTag Users
 */
@Controller
@Path("users")
public class UserController extends FimsAbstractUserController {

    @Autowired
    UserController(UserService userService, FimsProperties props) {
        super(userService, props);
    }

    /**
     * Rest service to initiate the reset password process
     *
     * @param username
     * @return
     */
    @GET
    @Path("{user}/sendResetToken")
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendResetToken(@PathParam("user") String username) {
        if (username.isEmpty()) {
            throw new BadRequestException("User not found.", "username is null");
        }
        User user = userService.generateResetToken(username);
        if (user != null) {

            String resetTokenURL = props.appRoot() + "resetPass?resetToken=" +
                    user.getPasswordResetToken();

            String emailBody = "You requested a password reset for your Biocode-Fims account.\n\n" +
                    "Use the following link within the next 24 hrs to reset your password.\n\n" +
                    resetTokenURL + "\n\n" +
                    "Thanks";

            // Send an Email that this completed
            EmailUtils.sendEmail(
                    user.getEmail(),
                    "Reset Password Link",
                    emailBody);
        }

        return Response.ok("{\"success\":\"A password reset token has be sent to your email.\"}").build();
    }
}

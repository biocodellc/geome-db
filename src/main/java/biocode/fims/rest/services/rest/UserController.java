package biocode.fims.rest.services.rest;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.entities.User;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.rest.filters.Admin;
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

    @GET
    @Authenticated
    @Admin
    @Path("/admin/profile/listEditorAsTable/{user}")
    @Produces(MediaType.TEXT_HTML)
    public Response listAdminProfileEditorAsTable(@PathParam("user") String username) {
        User user = userService.getUser(username);
        return Response.ok(getProfileEditor(user, true)).build();
    }

    @GET
    @Authenticated
    @Path("/profile/listEditorAsTable")
    @Produces(MediaType.TEXT_HTML)
    public Response listProfileEditorAsTable() {
        return Response.ok(getProfileEditor(userContext.getUser(), false)).build();
    }

    @GET
    @Authenticated
    @Admin
    @Path("/admin/createUserForm")
    @Produces(MediaType.TEXT_HTML)
    public Response getCreatUserForm() {
        StringBuilder sb = new StringBuilder();
        sb.append("\t<form id=\"submitForm\" method=\"POST\">\n");

        sb.append("<table>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td>Username</td>\n");
        sb.append("\t\t\t<td><input type=\"text\" name=\"username\"></td>\n");
        sb.append("\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td>First Name</td>\n");
        sb.append(("\t\t\t<td><input type=\"text\" name=\"firstName\"></td>\n"));
        sb.append("\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td>Last Name</td>\n");
        sb.append(("\t\t\t<td><input type=\"text\" name=\"lastName\"></td>\n"));
        sb.append("\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td>Email</td>\n");
        sb.append(("\t\t\t<td><input type=\"text\" name=\"email\"></td>\n"));
        sb.append("\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td>Institution</td>\n");
        sb.append(("\t\t\t<td><input type=\"text\" name=\"institution\"></td>\n"));
        sb.append("\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td>Password</td>\n");
        sb.append("\t\t\t<td><input class=\"pwcheck\" type=\"password\" name=\"password\" data-indicator=\"pwindicator\"></td>\n");
        sb.append("\t\t</tr>");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td></td>\n");
        sb.append("\t\t\t<td><div id=\"pwindicator\"><div class=\"label\"></div></div></td>\n");
        sb.append("\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td></td>\n");
        sb.append("\t\t\t<td><div class=\"error\" align=\"center\"></div></td>\n");
        sb.append("\t\t</tr>\n");

        sb.append("\t\t<tr>\n");
        sb.append("\t\t\t<td></td>\n");
        sb.append("\t\t\t<td><input type=\"button\" id=\"createFormButton\" value=\"Submit\"><input type=\"button\" id=\"createFormCancelButton\" value=\"Cancel\"></td>\n");
        sb.append("\t\t</tr>\n");
        sb.append("\t\t<input type=\"hidden\" name=\"projectId\">\n");

        sb.append("</table>\n");
        sb.append("\t</form>\n");


        return Response.ok(sb.toString()).build();
    }

    @GET
    @Authenticated
    @Path("/profile/listAsTable")
    @Produces(MediaType.TEXT_HTML)
    public Response listProfileAsTable() {
        StringBuilder sb = new StringBuilder();

        sb.append("<table id=\"profile\">\n");
        sb.append("\t<tr>\n");
        sb.append("\t\t<td>First Name:</td>\n");
        sb.append("\t\t<td>");
        sb.append(userContext.getUser().getFirstName());
        sb.append("</td>\n");
        sb.append("\t</tr>\n");

        sb.append("\t<tr>\n");
        sb.append("\t\t<td>Last Name:</td>\n");
        sb.append("\t\t<td>");
        sb.append(userContext.getUser().getLastName());
        sb.append("</td>\n");
        sb.append("\t</tr>\n");

        sb.append("\t<tr>\n");
        sb.append("\t\t<td>Email:</td>\n");
        sb.append("\t\t<td>");
        sb.append(userContext.getUser().getEmail());
        sb.append("</td>\n");
        sb.append("\t</tr>\n");

        sb.append("\t<tr>\n");
        sb.append("\t\t<td>Institution:</td>\n");
        sb.append("\t\t<td>");
        sb.append(userContext.getUser().getInstitution());
        sb.append("</td>\n");
        sb.append("\t</tr>\n");

        sb.append("\t<tr>\n");
        sb.append("\t\t<td></td>\n");
        sb.append("\t\t<td><a href=\"javascript:void(0)\">Edit Profile</a></td>\n");
        sb.append("\t</tr>\n");

        sb.append("\t</tr>\n</table>\n");

        return Response.ok(sb.toString()).build();
    }

    private String getProfileEditor(User user, Boolean isAdmin) {
        StringBuilder sb = new StringBuilder();

        sb.append("<form>\n");

        sb.append("<table>\n");
        sb.append("\t<tr>\n");
        sb.append("\t\t<td>First Name</td>\n");
        sb.append(("\t\t<td><input type=\"text\" name=\"firstName\" value=\""));
        sb.append(user.getFirstName());
        sb.append("\"></td>\n\t</tr>");

        sb.append("\t<tr>\n");
        sb.append("\t\t<td>Last Name</td>\n");
        sb.append(("\t\t<td><input type=\"text\" name=\"lastName\" value=\""));
        sb.append(user.getLastName());
        sb.append("\"></td>\n\t</tr>");

        sb.append("\t<tr>\n");
        sb.append("\t\t<td>Email</td>\n");
        sb.append(("\t\t<td><input type=\"text\" name=\"email\" value=\""));
        sb.append(user.getEmail());
        sb.append("\"></td>\n\t</tr>");

        sb.append("\t<tr>\n");
        sb.append("\t\t<td>Institution</td>\n");
        sb.append(("\t\t<td><input type=\"text\" name=\"institution\" value=\""));
        sb.append(user.getInstitution());
        sb.append("\"></td>\n\t</tr>");

        sb.append("\t<tr>\n");
        sb.append("\t\t<td>New Password</td>\n");
        sb.append("\t\t<td><input class=\"pwcheck\" type=\"password\" name=\"newPassword\" data-indicator=\"pwindicator\">");
        sb.append("</td>\n\t</tr>");

        sb.append("\t<tr>\n");
        sb.append("\t\t<td></td>\n");
        sb.append("\t\t<td><div id=\"pwindicator\"><div class=\"label\"></div></div></td>\n");
        sb.append("\t</tr>");

        if (!isAdmin) {
            sb.append("\t<tr>\n");
            sb.append("\t\t<td>Old Password</td>\n");
            sb.append("\t\t<td><input type=\"password\" name=\"oldPassword\">");
            sb.append("</td>\n\t</tr>");
        }

        sb.append("\t<tr>\n");
        sb.append("\t\t<td></td>\n");
        sb.append("<td class=\"error\" align=\"center\">");
        sb.append("</td>\n\t</tr>");

        sb.append("\t<tr>\n");
        sb.append("\t\t<td></td>\n");
        sb.append(("\t\t<td><input id=\"profile_submit\" type=\"button\" value=\"Submit\"><input type=\"button\" id=\"cancelButton\" value=\"Cancel\">"));
        sb.append("</td>\n\t</tr>\n");
        sb.append("</table>\n");
        sb.append("<input type=\"hidden\" name=\"username\" value=\"");
        sb.append(user.getUsername());
        sb.append("\" />");
        sb.append("</form>\n");

        return sb.toString();
    }
}

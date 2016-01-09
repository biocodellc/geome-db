package services;

import biocode.fims.FimsService;
import biocode.fims.SendEmail;
import biocode.fims.fimsExceptions.ServerErrorException;
import org.json.simple.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * REST services dealing with user management
 */
@Path("users")
public class Users extends FimsService {
    /**
     * Rest service to initiate the reset password process
     * @param username
     * @return
     */
    @GET
    @Path("{user}/sendResetToken")
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendResetToken(@PathParam("user") String username) {
        JSONObject resetToken = fimsConnector.postJSONObject(
                fimsCoreRoot + "id/authenticationService/sendResetToken",
                "username=" + username);

        String resetTokenURL = appRoot + "resetPass.jsp?resetToken=" +
                resetToken.get("resetToken");

        String emailBody = "You requested a password reset for your Biocode-Fims account.\n\n" +
                "Use the following link within the next 24 hrs to reset your password.\n\n" +
                resetTokenURL + "\n\n" +
                "Thanks";

        // Send an Email that this completed
        SendEmail sendEmail = new SendEmail(
                sm.retrieveValue("mailUser"),
                sm.retrieveValue("mailPassword"),
                sm.retrieveValue("mailFrom"),
                resetToken.get("email").toString(),
                "Reset Password Link",
                emailBody);
        sendEmail.start();

        return Response.ok("{\"success\":\"A password reset token has be sent to your email.\"}").build();
    }

    /**
     * Rest service to reset a user's password
     * @param resetToken
     * @param password
     * @return
     */
    @POST
    @Path("/resetPassword")
    @Produces(MediaType.TEXT_HTML)
    public Response resetPassword(@FormParam("resetToken") String resetToken,
                                  @FormParam("password") String password) {
        try {
            JSONObject response = fimsConnector.postJSONObject(
                    fimsCoreRoot + "id/authenticationService/reset",
                    "token=" + resetToken + "&password=" + password);
            if (!response.containsKey("success")) {
                throw new ServerErrorException("Server Error", "Something happened while updating user's password");
            }
            return Response.seeOther(new URI(appRoot + "index.jsp")).build();
        } catch (URISyntaxException e) {
            throw new ServerErrorException(e);
        }
    }

    @GET
    @Path("{user}/profile/listEditorAsTable")
    @Produces(MediaType.TEXT_HTML)
    public Response listAdminProfileEditorAsTable(@PathParam("user") String username) {
        JSONObject profile = fimsConnector.getJSONObject(fimsCoreRoot + "id/userService/profile/" + username);
        return Response.ok(getProfileEditor(profile, true)).build();
    }

    @POST
    @Path("/profile/update")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProfile(MultivaluedMap params) {
        return fimsConnector.createPOSTConnnection(fimsCoreRoot + "id/userService/profile/update/",
                fimsConnector.getPostParams(params));
    }

    @GET
    @Path("/profile/listEditorAsTable")
    @Produces(MediaType.TEXT_HTML)
    public Response listProfileEditorAsTable() {
        JSONObject profile = fimsConnector.getJSONObject(fimsCoreRoot + "id/userService/profile/");
        return Response.ok(getProfileEditor(profile, false)).build();
    }

    @POST
    @Path("/admin/create")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createUser(MultivaluedMap params) {
        return fimsConnector.createPOSTConnnection(fimsCoreRoot + "id/userService/create/",
                fimsConnector.getPostParams(params));
    }

    @GET
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
    @Path("/profile/listAsTable")
    @Produces(MediaType.TEXT_HTML)
    public Response listProfileAsTable() {
        JSONObject profile = fimsConnector.getJSONObject(fimsCoreRoot + "id/userService/profile/");
        StringBuilder sb = new StringBuilder();

        sb.append("<table id=\"profile\">\n");
        sb.append("\t<tr>\n");
        sb.append("\t\t<td>First Name:</td>\n");
        sb.append("\t\t<td>");
        sb.append(profile.get("firstName"));
        sb.append("</td>\n");
        sb.append("\t</tr>\n");

        sb.append("\t<tr>\n");
        sb.append("\t\t<td>Last Name:</td>\n");
        sb.append("\t\t<td>");
        sb.append(profile.get("lastName"));
        sb.append("</td>\n");
        sb.append("\t</tr>\n");

        sb.append("\t<tr>\n");
        sb.append("\t\t<td>Email:</td>\n");
        sb.append("\t\t<td>");
        sb.append(profile.get("email"));
        sb.append("</td>\n");
        sb.append("\t</tr>\n");

        sb.append("\t<tr>\n");
        sb.append("\t\t<td>Institution:</td>\n");
        sb.append("\t\t<td>");
        sb.append(profile.get("institution"));
        sb.append("</td>\n");
        sb.append("\t</tr>\n");

        sb.append("\t<tr>\n");
        sb.append("\t\t<td></td>\n");
        sb.append("\t\t<td><a href=\"javascript:void(0)\">Edit Profile</a></td>\n");
        sb.append("\t</tr>\n");

        sb.append("\t</tr>\n</table>\n");

        return Response.ok(sb.toString()).build();
    }

    private String getProfileEditor(JSONObject profile, Boolean isAdmin) {
        StringBuilder sb = new StringBuilder();

        sb.append("<form>\n");

        sb.append("<table>\n");
        sb.append("\t<tr>\n");
        sb.append("\t\t<td>First Name</td>\n");
        sb.append(("\t\t<td><input type=\"text\" name=\"firstName\" value=\""));
        sb.append(profile.get("firstName"));
        sb.append("\"></td>\n\t</tr>");

        sb.append("\t<tr>\n");
        sb.append("\t\t<td>Last Name</td>\n");
        sb.append(("\t\t<td><input type=\"text\" name=\"lastName\" value=\""));
        sb.append(profile.get("lastName"));
        sb.append("\"></td>\n\t</tr>");

        sb.append("\t<tr>\n");
        sb.append("\t\t<td>Email</td>\n");
        sb.append(("\t\t<td><input type=\"text\" name=\"email\" value=\""));
        sb.append(profile.get("email"));
        sb.append("\"></td>\n\t</tr>");

        sb.append("\t<tr>\n");
        sb.append("\t\t<td>Institution</td>\n");
        sb.append(("\t\t<td><input type=\"text\" name=\"institution\" value=\""));
        sb.append(profile.get("institution"));
        sb.append("\"></td>\n\t</tr>");

        sb.append("\t<tr>\n");
        sb.append("\t\t<td>New Password</td>\n");
        sb.append("\t\t<td><input class=\"pwcheck\" type=\"password\" name=\"new_password\" data-indicator=\"pwindicator\">");
        sb.append("</td>\n\t</tr>");

        sb.append("\t<tr>\n");
        sb.append("\t\t<td></td>\n");
        sb.append("\t\t<td><div id=\"pwindicator\"><div class=\"label\"></div></div></td>\n");
        sb.append("\t</tr>");

        if (!isAdmin) {
            sb.append("\t<tr>\n");
            sb.append("\t\t<td>Old Password</td>\n");
            sb.append("\t\t<td><input type=\"password\" name=\"old_password\">");
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
        sb.append(profile.get("username"));
        sb.append("\" />");
        sb.append("</form>\n");

        return sb.toString();
    }
}

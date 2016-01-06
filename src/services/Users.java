package services;

import biocode.fims.FimsService;
import biocode.fims.SendEmail;
import biocode.fims.fimsExceptions.ServerErrorException;
import org.json.simple.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
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
    @Path("{user}/profile/listAsTable")
    @Produces(MediaType.TEXT_HTML)
    public String listProfileAsTable() {
       return "stet";
    }
}

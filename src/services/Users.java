package services;

import biocode.fims.FimsConnector;
import biocode.fims.FimsService;
import biocode.fims.SendEmail;
import biocode.fims.fimsExceptions.ServerErrorException;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URL;

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
        FimsConnector fimsConnector = new FimsConnector(clientId, clientSecret);
        try {
            JSONObject resetToken = (JSONObject) JSONValue.parse(fimsConnector.createPOSTConnnection(
                    new URL(fimsCoreRoot + "id/authenticationService/sendResetToken"),
                    "username=" + username));

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
        } catch (MalformedURLException e) {
            throw new ServerErrorException(e);
        }
    }

    /**
     * Rest service to reset a user's password
     * @param resetToken
     * @param password
     * @return
     */
    @POST
    @Path("/resetPassword")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetPassword(@FormParam("resetToken") String resetToken,
                                  @FormParam("password") String password) {
        FimsConnector fimsConnector = new FimsConnector(clientId, clientSecret);
        try {
            JSONObject response = (JSONObject) JSONValue.parse(fimsConnector.createPOSTConnnection(
                    new URL(fimsCoreRoot + "id/authenticationService/reset"),
                    "token=" + resetToken + "&password=" + password));
            if (!response.containsKey("success")) {
                throw new ServerErrorException("Server Error", "Something happened while updating user's password");
            }
            return Response.ok("{\"success\":\"Your password was successfully updated.\"}").build();
        } catch (MalformedURLException e) {
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

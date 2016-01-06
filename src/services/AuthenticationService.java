package services;

import biocode.fims.FimsService;
import biocode.fims.fimsExceptions.ServerErrorException;
import org.json.simple.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by rjewing on 4/12/14.
 */
@Path("authenticationService")
public class AuthenticationService extends FimsService {

    /**
     * first, exchange the user creditials for an access token. Then use the access token to obtain the user's
     * profile information, and store the username, user id, access token, and refresh token in the session.
     *
     */
    @POST
    @Path("login")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@FormParam("username") String username,
                          @FormParam("password") String password) {

        String params = "username=" + username + "&password=" + password + "&grant_type=password" +
                "&client_id=" + clientId + "&client_secret=" + clientSecret;

        JSONObject tokenJSON = fimsConnector.postJSONObject(fimsCoreRoot +
                "id/authenticationService/oauth/access_token", params);

        accessToken = tokenJSON.get("access_token").toString();
        refreshToken = tokenJSON.get("refresh_token").toString();

        fimsConnector.setRefreshToken(refreshToken);
        fimsConnector.setAccessToken(accessToken);

        JSONObject profileJSON = fimsConnector.getJSONObject(
                fimsCoreRoot + "id/userService/oauth");

        session.setAttribute("user", profileJSON.get("username"));
        session.setAttribute("userId", profileJSON.get("userId"));
        session.setAttribute("accessToken", accessToken);
        session.setAttribute("refreshToken", refreshToken);
        System.out.println("accessToken= " + accessToken);

        //TODO get the Response.seeOther working with ajax call
        // Check if the user has set their own password, if they are just using the temporary password,
        // inform the user to change their password
        if (!Boolean.getBoolean((String) profileJSON.get("hasSetPassword"))) {
            Response.ok("{\"url\":\"" + appRoot + "secure/profile.jsp?error=Update Your Password\"}").build();
        }

//            return Response.seeOther(new URI(appRoot + "index.jsp")).build();
        return Response.ok("{\"url\":\"" + appRoot + "index.jsp\"}").build();
    }

    /**
     * Rest service to log a user out of the fims system
     */
    @GET
    @Path("logout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout() {
        // Invalidate the session
        session.invalidate();
        try {
            return Response.seeOther(new URI(appRoot + "index.jsp")).build();
        } catch (URISyntaxException e) {
            throw new ServerErrorException(e);
        }
    }
}

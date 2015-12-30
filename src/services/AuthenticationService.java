package services;

import biocode.fims.FimsService;
import biocode.fims.fimsExceptions.ServerErrorException;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import biocode.fims.FimsConnector;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

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
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@FormParam("username") String username,
                          @FormParam("password") String password) {

        try {
            FimsConnector fimsConnector = new FimsConnector(clientId, clientSecret);
            String params = "username=" + username + "&password=" + password + "&grant_type=password" +
                    "&client_id=" + clientId + "&client_secret=" + clientSecret;

            JSONObject tokenJSON = (JSONObject) JSONValue.parse(fimsConnector.createPOSTConnnection(new URL(fimsCoreRoot +
                    "id/authenticationService/oauth/access_token"), params));

            accessToken = tokenJSON.get("access_token").toString();
            refreshToken = tokenJSON.get("refresh_token").toString();

            JSONObject profileJSON = (JSONObject) JSONValue.parse(fimsConnector.createGETConnection(
                    new URL(fimsCoreRoot + "id/userService/oauth?access_token=" + accessToken)));

            session.setAttribute("user", profileJSON.get("username"));
            session.setAttribute("userId", profileJSON.get("user_id"));
            session.setAttribute("accessToken", accessToken);
            session.setAttribute("refreshToken", refreshToken);

            //TODO get the Response.seeOther working with ajax call
//            return Response.seeOther(new URI(appRoot + "index.jsp")).build();
            return Response.ok("{\"url\":\"" + appRoot + "index.jsp\"}").build();
        } catch (MalformedURLException e) {
            throw new ServerErrorException(e);
        }
//        } catch (URISyntaxException e) {
//            throw new ServerErrorException(e);
//        }
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

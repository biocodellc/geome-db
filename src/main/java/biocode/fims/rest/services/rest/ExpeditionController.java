package biocode.fims.rest.services.rest;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.bcid.ExpeditionMinter;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

/**
 * REST services dealing with expeditions
 * @resourceTag Expeditions
 */
@Controller
@Path("expeditions")
public class ExpeditionController extends FimsAbstractExpeditionController {

    @Autowired
    public ExpeditionController(ExpeditionService expeditionService, ProjectService projectService, FimsProperties props) {
        super(expeditionService, projectService, props);
    }

    @GET
    @Authenticated
    @Path("{expeditionId}/resourcesAsTable")
    @Produces(MediaType.TEXT_HTML)
    public Response listResourcesAsTable(@PathParam("expeditionId") int expeditionId) {
        ExpeditionMinter e = new ExpeditionMinter();

        ArrayList<JSONObject> resources = e.getResources(expeditionId);

        StringBuilder sb = new StringBuilder();
        sb.append("<table>\n");
        sb.append("\t<tbody>\n");
        sb.append("\t<tr>\n");
        sb.append("\t\t<th>Identifier</th>\n");
        sb.append("\t\t<th>Resource Type</th>\n");
        sb.append("\t</tr>\n");

        for (Object r : resources) {
            JSONObject resource = (JSONObject) r;
            sb.append("\t<tr>\n");
            sb.append("\t\t<td>");
            sb.append("<a href=\"" + props.appRoot() + "lookup?id=");
            sb.append(resource.get("identifier"));
            sb.append("\">");
            sb.append("http://n2t.net/");
            sb.append(resource.get("identifier"));
            sb.append("</a>");
            sb.append("</td>\n");
            sb.append("\t\t<td>");
            // only display a hyperlink if resourceTypeUri is not empty
            if (!resource.get("resourceTypeUri").equals("")) {
                sb.append("<a href=\"" + resource.get("resourceTypeUri") + "\">" + resource.get("resourceType") + "</a>");
            } else {
                sb.append(resource.get("resourceType"));
            }
            sb.append("</td>\n");
            sb.append("\t</tr>\n");
        }

        sb.append("\t</tbody>\n");
        sb.append("</table>\n");
        return Response.ok(sb.toString()).build();
    }

    @GET
    @Authenticated
    @Path("{expeditionId}/datasetsAsTable")
    @Produces(MediaType.TEXT_HTML)
    public Response listDatasetsAsTable(@PathParam("expeditionId") int expeditionId) {
        ExpeditionMinter expeditionMinter = new ExpeditionMinter();

        if (!props.ignoreUser() && !expeditionMinter.userOwnsExpedition(userContext.getUser().getUserId(), expeditionId)) {
            throw new ForbiddenRequestException("You must own this expedition in order to view its datasets.");
        }

        ArrayList<JSONObject> datasets = expeditionMinter.getDatasets(expeditionId);
        StringBuilder sb = new StringBuilder();
        sb.append("<table>\n");
        sb.append("\t<tr>\n");
        sb.append("\t\t<th>Date</th>\n");
        sb.append("\t\t<th>Identifier</th>\n");
        sb.append("\t</tr>\n");

        if (!datasets.isEmpty()) {
            for (Object d : datasets) {
                JSONObject dataset = (JSONObject) d;
                sb.append("\t<tr>\n");
                sb.append("\t\t<td>");
                sb.append(dataset.get("ts"));
                sb.append("\t\t</td>");

                sb.append("\t\t<td>");
                sb.append("<a href=\"" + props.appRoot() + "lookup?id=");
                sb.append(dataset.get("identifier"));
                sb.append("\">");
                sb.append("http://n2t.net/");
                sb.append(dataset.get("identifier"));
                sb.append("</a>");
                sb.append("\t\t</td>");
                sb.append("\t</tr>\n");
            }
        }

        sb.append("</table>\n");
        return Response.ok(sb.toString()).build();
    }

    @GET
    @Authenticated
    @Path("{expeditionId}/metadataAsTable")
    @Produces(MediaType.TEXT_HTML)
    public Response listMetadataAsTable(@PathParam("expeditionId") int expeditionId) {
        ExpeditionMinter e = new ExpeditionMinter();

        if (!props.ignoreUser() && !e.userOwnsExpedition(userContext.getUser().getUserId(), expeditionId)) {
            throw new ForbiddenRequestException("You must own this expedition in order to view its datasets.");
        }

        JSONObject metadata = e.getMetadata(expeditionId);

        StringBuilder sb = new StringBuilder();
        sb.append("<table>\n");
        sb.append("\t<tbody>\n");

        sb.append("\t<tr>\n");
        sb.append("\t\t<td>");
        sb.append("Identifier:");
        sb.append("\t\t</td>\n");
        sb.append("\t\t<td>");
        sb.append("<a href=\"" + props.appRoot() + "lookup?id=");
        sb.append(metadata.get("identifier"));
        sb.append("\">");
        sb.append("http://n2t.net/");
        sb.append(metadata.get("identifier"));
        sb.append("</a>");
        sb.append("\t\t</td>\n");
        sb.append("\t</tr>\n");

        sb.append("\t<tr>\n");
        sb.append("\t\t<td>");
        sb.append("Public Expedition:");
        sb.append("\t\t</td>");
        sb.append("\t\t<td>");
        if (Boolean.valueOf(metadata.get("public").toString())) {
            sb.append("yes");
        } else {
            sb.append("no");
        }
        sb.append("&nbsp;&nbsp;");
        sb.append("<a href='#' onclick=\"editExpedition('");
        sb.append(metadata.get("projectId"));
        sb.append("', '");
        sb.append(metadata.get("expeditionCode"));
        sb.append("', this)\">edit</a>");
        sb.append("\t\t</td>");
        sb.append("\t</tr>\n");

        sb.append("\t</tbody>\n");
        sb.append("</table>\n");
        return Response.ok(sb.toString()).build();
    }
}

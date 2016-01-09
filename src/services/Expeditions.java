package services;

import biocode.fims.FimsService;
import biocode.fims.SettingsManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import utils.Utils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * REST services dealing with expeditions
 */
@Path("expeditions")
public class Expeditions extends FimsService {
    @GET
    @Path("{expeditionId}/resourcesAsTable")
    public Response listResourcesAsTable(@PathParam("expeditionId") int expeditionId) {
        JSONArray resources = fimsConnector.getJSONArray(fimsCoreRoot + "id/expeditionService/resources/" + expeditionId);
        StringBuilder sb = new StringBuilder();
        sb.append("<table>\n");
        sb.append("\t<tbody>\n");
        sb.append("\t<tr>\n");
        sb.append("\t\t<th>Identifier</th>\n");
        sb.append("\t\t<th>Resource Type</th>\n");
        sb.append("\t</tr>\n");

        for (Object r: resources) {
            JSONObject resource = (JSONObject) r;
            sb.append("\t<tr>\n");
            sb.append("\t\t<td>");
            sb.append("<a href=\"" + appRoot + "lookup.jsp?id=");
            sb.append(resource.get("identifier"));
            sb.append("\">");
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
    @Path("{expeditionId}/datasetsAsTable")
    public Response listDatasetsAsTable(@PathParam("expeditionId") int expeditionId) {
        JSONArray datasets = fimsConnector.getJSONArray(fimsCoreRoot + "id/expeditionService/datasets/" + expeditionId);
        StringBuilder sb = new StringBuilder();
        sb.append("<table>\n");
        sb.append("\t<tr>\n");
        sb.append("\t\t<th>Date</th>\n");
        sb.append("\t\t<th>Identifier</th>\n");
        sb.append("\t</tr>\n");

        if (!datasets.isEmpty()) {
            Utils u = new Utils();
            List<JSONObject> sortedDatasets = u.sortDatasets(datasets);
            for (Object d : sortedDatasets) {
                JSONObject dataset = (JSONObject) d;
                sb.append("\t<tr>\n");
                sb.append("\t\t<td>");
                sb.append(dataset.get("ts"));
                sb.append("\t\t</td>");

                sb.append("\t\t<td>");
                sb.append("<a href=\"" + appRoot + "lookup.jsp?id=");
                sb.append(dataset.get("identifier"));
                sb.append("\">");
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
    @Path("{expeditionId}/metadataAsTable")
    public Response listMetadataAsTable(@PathParam("expeditionId") int expeditionId) {
        JSONObject configuration = fimsConnector.getJSONObject(fimsCoreRoot + "id/expeditionService/metadata/" + expeditionId);
        StringBuilder sb = new StringBuilder();
        sb.append("<table>\n");
        sb.append("\t<tbody>\n");

        sb.append("\t<tr>\n");
        sb.append("\t\t<td>");
        sb.append("Identifier:");
        sb.append("\t\t</td>\n");
        sb.append("\t\t<td>");
        sb.append("<a href=\"" + appRoot + "lookup.jsp?id=");
        sb.append(configuration.get("identifier"));
        sb.append("\">");
        sb.append(configuration.get("identifier"));
        sb.append("</a>");
        sb.append("\t\t</td>\n");
        sb.append("\t</tr>\n");

        sb.append("\t<tr>\n");
        sb.append("\t\t<td>");
        sb.append("Public Expedition:");
        sb.append("\t\t</td>");
        sb.append("\t\t<td>");
        if (Boolean.valueOf(configuration.get("public").toString())) {
            sb.append("yes");
        } else {
            sb.append("no");
        }
        sb.append("&nbsp;&nbsp;");
        sb.append("<a href='#' onclick=\"editExpedition('");
        sb.append(configuration.get("projectId"));
        sb.append("', '");
        sb.append(configuration.get("expeditionCode"));
        sb.append("', this)\">edit</a>");
        sb.append("\t\t</td>");
        sb.append("\t</tr>\n");

        sb.append("\t</tbody>\n");
        sb.append("</table>\n");
        return Response.ok(sb.toString()).build();
    }

    @GET
    @Path("updateStatus/{projectId}/{expeditionCode}/{publicStatus}")
    public Response updateStatus(@PathParam("projectId") int projectId,
                                 @PathParam("expeditionCode") String expeditionCode,
                                 @PathParam("publicStatus") Boolean publicStatus) {
        return fimsConnector.createGETConnection(fimsCoreRoot + "id/expeditionService/publicExpedition/"
                + projectId + "/" + expeditionCode + "/" + publicStatus);
    }
}

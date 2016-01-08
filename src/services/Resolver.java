package services;

import biocode.fims.FimsService;
import biocode.fims.fimsExceptions.ServerErrorException;
import org.glassfish.jersey.client.ClientResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * REST services dealing with projects
 */
@Path("{scheme}:")
public class Resolver extends FimsService {
    @PathParam("scheme") String scheme;
    @GET
    @Path("/{naan}/{shoulderPlusIdentifier}")
    public Response resolver(
            @PathParam("naan") String naan,
            @PathParam("shoulderPlusIdentifier") String shoulderPlusIdentifier) {
        shoulderPlusIdentifier = shoulderPlusIdentifier.trim();

        // Structure the Bcid element from path parameters
        String element = scheme + ":" + "/" + naan + "/" + shoulderPlusIdentifier;

        JSONObject metadata = fimsConnector.getJSONObject(fimsCoreRoot + "id/metadata/" + element);

        // If forwardingResolution is true, then forward to the resolutionTarget
        if (((JSONObject) metadata.get("forwardingResolution")).get("value").equals("true")) {
            try {
                URI resolutionTarget = new URI(((JSONObject) metadata.get("resolutionTarget")).get("value").toString());
                return Response.seeOther(resolutionTarget).build();
            } catch (URISyntaxException e) {
                throw new ServerErrorException(e);
            }
        } else {
            return Response.ok(renderTable(metadata)).build();
        }
    }

    /**
     * render an html table of the bcid metadata
     * @param metadata
     * @return
     */
    private String renderTable(JSONObject metadata) {
        StringBuilder outputSB = new StringBuilder();
        if (!metadata.containsKey("identifier")) {
            return "<h1>Unable to find bcid</h1>";
        }
        outputSB.append("<h1>" + ((JSONObject) metadata.get("identifier")).get("value") + " is a <a href=\"" +
                ((JSONObject) metadata.get("rdf:type")).get("value") + "\">" +
                ((JSONObject) metadata.get("rdf:type")).get("shortValue")+ "</a></h1>\n\n");
        outputSB.append("<table>\n");
        outputSB.append("\t<tr>\n" +
                "\t\t<th>Description</th>\n" +
                "\t\t<th>Value</th>\n" +
                "\t\t<th>Definition</th>\n" +
                "\t</tr>\n");

        tableResourceRowAppender((JSONObject) metadata.get("rdf:type"), "rdf:type", outputSB);
        tableResourceRowAppender((JSONObject) metadata.get("rdf:Description"), "rdf:Description", outputSB);
        tableResourceRowAppender((JSONObject) metadata.get("dcterms:mediator"), "dcterms:mediator", outputSB);
        tableResourceRowAppender((JSONObject) metadata.get("dcterms:hasVersion"), "dcterms:hasVersion", outputSB);
        tableResourceRowAppender((JSONObject) metadata.get("dcterms:isReferencedBy"), "dcterms:isReferencedBy", outputSB);
        tableResourceRowAppender((JSONObject) metadata.get("dcterms:rights"), "dcterms:rights", outputSB);
        tableResourceRowAppender((JSONObject) metadata.get("dcterms:isPartOf"), "dcterms:isPartOf", outputSB);
        tablePropertyRowAppender((JSONObject) metadata.get("dc:date"), "dc:date", outputSB);
        tablePropertyRowAppender((JSONObject) metadata.get("dc:creator"), "dc:creator", outputSB);
        tablePropertyRowAppender((JSONObject) metadata.get("dc:title"), "dc:title", outputSB);
        tablePropertyRowAppender((JSONObject) metadata.get("dc:source"), "dc:source", outputSB);
        tablePropertyRowAppender((JSONObject) metadata.get("bsc:suffixPassThrough"), "bsc:suffixPassThrough", outputSB);
        outputSB.append("</table>\n");
        if (metadata.containsKey("download")) {
            appendDataset((JSONObject) metadata.get("download"), outputSB);
        }
        if (metadata.containsKey("datasets")) {
            appendExpeditionDatasets((JSONArray) metadata.get("datasets"), outputSB);
        }
        return outputSB.toString();
    }

    /**
     * append each property
     *
     * @param
     */
    private void tablePropertyRowAppender(JSONObject property, String key, StringBuilder outputSB) {
        if (!((String) property.get("value")).trim().equals("")) {
            outputSB.append("\t<tr>\n" +
                    "\t\t<td>" + property.get("value") + "</td>\n" +
                    "\t\t<td><a href=\"" + property.get("fullKey") + "\">" + key + "</a></td>\n" +
                    "\t\t<td>" + property.get("description") + "</td>\n" +
                    "\t</tr>\n");
        }
    }

    /**
     * append each property
     *
     * @param
     */
    private void tableResourceRowAppender(JSONObject resource, String key, StringBuilder outputSB) {
        if (!((String) resource.get("value")).trim().equals("")) {
            outputSB.append("\t<tr>\n" +
                    "\t\t<td><a href=\"" + resource.get("value") + "\">" + resource.get("value") + "</a></td>\n" +
                    "\t\t<td><a href=\"" + resource.get("fullKey") + "\">" + key + "</a></td>\n" +
                    "\t\t<td>" + resource.get("description") + "</td>\n" +
                    "\t</tr>\n");
        }

    }

    /**
     * creates a html table to display the datasets
     * @param datasets
     * @param outputSB
     */
    private void appendExpeditionDatasets(JSONArray datasets, StringBuilder outputSB) {
        List<JSONObject> sortedDatasets = sortDatasets(datasets);
        outputSB.append("<table>\n");
        outputSB.append("\t<tr>\n");
        outputSB.append("\t\t<th>Date</th>\n");
        outputSB.append("\t\t<th>Identifier</th>\n");
        outputSB.append("\t</tr>\n");
        for (Object d: sortedDatasets) {
            JSONObject dataset = (JSONObject) d;
            outputSB.append("\t<tr>\n");
            outputSB.append("\t\t<td>");
            outputSB.append(dataset.get("ts"));
            outputSB.append("\t\t</td>");

            outputSB.append("\t\t<td>");
            outputSB.append("<a href=\"" + sm.retrieveValue("appRoot") + "lookup.jsp?id=");
            outputSB.append(dataset.get("identifier"));
            outputSB.append("\">");
            outputSB.append(dataset.get("identifier"));
            outputSB.append("</a>");
            outputSB.append("\t\t</td>");
            outputSB.append("\t</tr>\n");
        }
        outputSB.append("</table>\n");
    }

    /**
     * method to sort the datasets by the ts
     * @param datasets
     * @return
     */
    private List<JSONObject>sortDatasets(JSONArray datasets) {
        List<JSONObject> datasetsList = new ArrayList<>();

        for (Object d: datasets) {
            datasetsList.add((JSONObject) d);
        }
        Collections.sort(datasetsList, new Comparator<JSONObject>() {
            //You can change "Name" with "ID" if you want to sort by ID
            private static final String KEY_NAME = "ts";

            @Override
            public int compare(JSONObject a, JSONObject b) {
                String valA = (String) a.get(KEY_NAME);
                String valB = (String) b.get(KEY_NAME);

                return valB.compareTo(valA);
            }
        });

        return datasetsList;
    }

    /**
     * create an html table to display the dataset download links
     * @param download
     * @param outputSB
     */
    private void appendDataset(JSONObject download, StringBuilder outputSB) {
        outputSB.append("<table>\n");
        outputSB.append("\t<tr>\n");
        outputSB.append("\t\t<th>Download:</th>\n");
        // Excel option
        outputSB.append("\t\t<th>");
        outputSB.append("<a href='");
        outputSB.append(download.get("excel"));
        outputSB.append("'>.xlsx</a>");

        outputSB.append("&nbsp;&nbsp;");

        // TAB delimited option
        outputSB.append("<a href='");
        outputSB.append(download.get("tab"));
        outputSB.append("'>.txt</a>");

        outputSB.append("&nbsp;&nbsp;");

        // n3 option
        outputSB.append("<a href='");
        outputSB.append(download.get("n3"));
        outputSB.append("'>n3</a>");

        outputSB.append("&nbsp;&nbsp;");
        outputSB.append("&nbsp;&nbsp;");

        outputSB.append("\t\t</td>");
        outputSB.append("\t</tr>\n");
        outputSB.append("</table>\n");
    }
}

package utils;

import biocode.fims.bcid.*;
import biocode.fims.bcid.Renderer.Renderer;
import biocode.fims.settings.SettingsManager;
import org.json.simple.JSONObject;

import java.util.ArrayList;

/**
 * utils.HTMLTableRenderer renders Identifier results as an HTMLTable
 */
public class HTMLTableRenderer extends Renderer {
    private Integer userId;
    private Resolver resolver;
    private static SettingsManager sm;
    private static String appRoot;

    static {
        sm = SettingsManager.getInstance();
        appRoot = sm.retrieveValue("appRoot");
    }

    /**
     * constructor for displaying private dataset information
     * @param userId
     */
    public HTMLTableRenderer(Integer userId, Resolver resolver) {
        this.userId = userId;
        this.resolver = resolver;
    }

    public void enter() {
        outputSB.append("<h1>" + identifier.getValue() + " is a <a href=\"" +
                resource.getValue() + "\">" +
                resource.getShortValue() + "</a></h1>\n\n");
        outputSB.append("<table>\n");
        outputSB.append("\t<tr>\n" +
                "\t\t<th>Description</th>\n" +
                "\t\t<th>Value</th>\n" +
                "\t\t<th>Definition</th>\n" +
                "\t</tr>\n");
    }

    public void printMetadata() {
        tableResourceRowAppender(resource);
        tableResourceRowAppender(about);
        tableResourceRowAppender(dcMediator);
        tableResourceRowAppender(dcHasVersion);
        tableResourceRowAppender(dcIsReferencedBy);
        tableResourceRowAppender(dcRights);
        tableResourceRowAppender(dcIsPartOf);
        tablePropertyRowAppender(dcDate);
        tablePropertyRowAppender(dcCreator);
        tablePropertyRowAppender(dcTitle);
        tablePropertyRowAppender(dcSource);
        tablePropertyRowAppender(bscSuffixPassthrough);
        tablePropertyRowAppender(isPublic);
        outputSB.append("</table>\n");
        appendExpeditionOrDatasetData(resource);
    }

    public void leave() {
        outputSB.append("</table>\n");
    }

    public boolean validIdentifier() {
        if (identifier == null) {
            outputSB.append("<h2>Unable to find identifier</h2>");
            return false;
        } else {
            return true;
        }
    }

    /**
     * append each property
     *
     * @param map
     */
    private void tablePropertyRowAppender(metadataElement map) {
        if (map != null) {
            if (!map.getValue().trim().equals("")) {
                outputSB.append("\t<tr>\n" +
                        "\t\t<td>" + map.getValue() + "</td>\n" +
                        "\t\t<td><a href=\"" + map.getFullKey() + "\">" + map.getKey() + "</a></td>\n" +
                        "\t\t<td>" + map.getDescription() + "</td>\n" +
                        "\t</tr>\n");
            }
        }
    }

    /**
     * append each property
     *
     * @param map
     */
    private void tableResourceRowAppender(metadataElement map) {
        if (map != null) {
            if (!map.getValue().trim().equals("")) {
                outputSB.append("\t<tr>\n" +
                        "\t\t<td><a href=\"" + map.getValue() + "\">" + map.getValue() + "</a></td>\n" +
                        "\t\t<td><a href=\"" + map.getFullKey() + "\">" + map.getKey() + "</a></td>\n" +
                        "\t\t<td>" + map.getDescription() + "</td>\n" +
                        "\t</tr>\n");
            }
        }

    }

    /**
     * check if the resource is a collection or dataset and append the dataset(s)
     * @param resource
     */
    private void appendExpeditionOrDatasetData(metadataElement resource) {
        ResourceTypes rts = new ResourceTypes();
        ResourceType rt = rts.get(resource.getValue());

        // check if the resource is a dataset or a collection
        if (rts.get(1).equals(rt)) {
            appendDataset();
        } else if (rts.get(38).equals(rt)) {
            appendExpeditionDatasets();
        }
    }

    private void appendExpeditionDatasets() {
        ExpeditionMinter expeditionMinter = new ExpeditionMinter();
        if (displayDatasets()) {
            ArrayList<JSONObject> datasets = expeditionMinter.getDatasets(resolver.getExpeditionId());

            outputSB.append("<table>\n");
            outputSB.append("\t<tr>\n");
            outputSB.append("\t\t<th>Date</th>\n");
            outputSB.append("\t\t<th>Identifier</th>\n");
            outputSB.append("\t</tr>\n");
            if (!datasets.isEmpty()) {
                for (Object d : datasets) {
                    JSONObject dataset = (JSONObject) d;
                    outputSB.append("\t<tr>\n");
                    outputSB.append("\t\t<td>");
                    outputSB.append(dataset.get("ts"));
                    outputSB.append("\t\t</td>");

                    outputSB.append("\t\t<td>");
                    outputSB.append("<a href=\"" + appRoot + "lookup.jsp?id=");
                    outputSB.append(dataset.get("identifier"));
                    outputSB.append("\">");
                    outputSB.append(dataset.get("identifier"));
                    outputSB.append("</a>");
                    outputSB.append("\t\t</td>");
                    outputSB.append("\t</tr>\n");
                }
            }
            outputSB.append("</table>\n");
        }
    }

    private void appendDataset() {
        if (displayDatasets()) {
            String projectId = resolver.getProjectID(bcid.getBcidsId());
            String graph = bcid.getGraph();

            outputSB.append("<table>\n");
            outputSB.append("\t<tr>\n");
            outputSB.append("\t\t<th>Download:</th>\n");
            // Excel option
            outputSB.append("\t\t<th>");
            outputSB.append("<a href='");
            outputSB.append(appRoot);
            outputSB.append("rest/projects/query/excel?graphs=");
            outputSB.append(graph);
            outputSB.append("&projectId=");
            outputSB.append(projectId);
            outputSB.append("'>.xlsx</a>");

            outputSB.append("&nbsp;&nbsp;");

            // TAB delimited option
            outputSB.append("<a href='");
            outputSB.append(appRoot);
            outputSB.append("rest/projects/query/tab?graphs=");
            outputSB.append(graph);
            outputSB.append("&projectId=");
            outputSB.append(projectId);
            outputSB.append("'>.txt</a>");

            outputSB.append("&nbsp;&nbsp;");

            // n3 option
            outputSB.append("<a href='");
            outputSB.append(bcid.getWebAddress());
            outputSB.append("'>n3</a>");

            outputSB.append("&nbsp;&nbsp;");
            outputSB.append("&nbsp;&nbsp;");

            outputSB.append("\t\t</td>");
            outputSB.append("\t</tr>\n");
            outputSB.append("</table>\n");
        }
    }

    private Boolean displayDatasets() {
        Boolean ignoreUser = Boolean.getBoolean(sm.retrieveValue("ignoreUser"));
        Integer projectId = Integer.parseInt(resolver.getProjectID(bcid.getBcidsId()));
        ExpeditionMinter expeditionMinter = new ExpeditionMinter();
        ProjectMinter projectMinter = new ProjectMinter();

        try {
            //if public expedition, return true
            if (expeditionMinter.isPublic(resolver.getExpeditionCode(), projectId)) {
                return true;
            }
            // if ignore_user and user in project, return true
            if (userId != null) {
                if (ignoreUser && projectMinter.userExistsInProject(userId, projectId)) {
                    return true;
                }
                // if !ignore_user and userOwnsExpedition, return true
                else if (!ignoreUser && expeditionMinter.userOwnsExpedition(userId, resolver.getExpeditionCode(), projectId)) {
                    return true;
                }
            }
        } finally {
            expeditionMinter.close();
            projectMinter.close();
        }

        return false;
    }
}

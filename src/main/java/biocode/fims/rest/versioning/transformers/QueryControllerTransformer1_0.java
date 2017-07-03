package biocode.fims.rest.versioning.transformers;

import biocode.fims.entities.BcidTmp;
import biocode.fims.service.BcidService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.*;

/**
 * class to transform requests to {@link biocode.fims.rest.services.rest.QueryController} resource methods from
 * {@link biocode.fims.rest.versioning.APIVersion}v1_0 to APIVersion v1_1, and responses from v1_1 to v1_0.
 */
@Component
public class QueryControllerTransformer1_0 extends FimsAbstractTransformer {
    private final static Logger logger = LoggerFactory.getLogger(QueryControllerTransformer1_0.class);

    @Autowired
    private BcidService bcidService;

    public Object queryJsonResponse(Object returnVal) {
        return transformJsonResponse(returnVal);
    }

    public Object queryJsonAsPostResponse(Object returnVal) {
        return transformJsonResponse(returnVal);
    }

    private Object transformJsonResponse(Object returnVal) {
        if (!(returnVal instanceof Response)) {
            return returnVal;
        }
        Response response = (Response) returnVal;

        if (response.getStatus() == 204) {
            return response;
        }

        Page<ObjectNode> entity;
        try {
            entity = (Page<ObjectNode>) response.getEntity();
        } catch (ClassCastException e) {
            logger.debug("ParseException occurred", e);
            return returnVal;
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode v1_0Response = mapper.createObjectNode();
        ArrayNode headers = mapper.createArrayNode();
        ArrayNode data = mapper.createArrayNode();

        for (ObjectNode resource: entity.getContent()) {

            if (headers.size() == 0) {
                resource.fieldNames().forEachRemaining(key -> headers.add(key));
            }

            ArrayNode row = mapper.createArrayNode();
            for (Iterator<Map.Entry<String, JsonNode>> it = resource.fields(); it.hasNext(); ) {
                row.add(it.next().getValue().asText());
            }

            data.addObject().set("row", row);
        }

        v1_0Response.set("data", data);
        v1_0Response.set("header", headers);


        return Response.fromResponse(response).entity(v1_0Response).build();
    }

    public void queryJsonRequest(LinkedHashMap<String, Object> argMap,
                                 MultivaluedMap<String, String> queryParameters) {
        transformGETRequest(argMap, queryParameters);
    }

    public void queryJsonAsPostRequest(LinkedHashMap<String, Object> argMap,
                                       MultivaluedMap<String, String> queryParameters) {
        transformPOSTRequest(argMap, queryParameters);
    }

    public void queryKmlRequest(LinkedHashMap<String, Object> argMap,
                                MultivaluedMap<String, String> queryParameters) {
        transformGETRequest(argMap, queryParameters);
    }

    public void queryKmlAsPostRequest(LinkedHashMap<String, Object> argMap,
                                       MultivaluedMap<String, String> queryParameters) {
        transformPOSTRequest(argMap, queryParameters);
    }

    public void queryExcelRequest(LinkedHashMap<String, Object> argMap,
                                  MultivaluedMap<String, String> queryParameters) {
        transformGETRequest(argMap, queryParameters);
    }

    public void queryExcelAsPostRequest(LinkedHashMap<String, Object> argMap,
                                      MultivaluedMap<String, String> queryParameters) {
        transformPOSTRequest(argMap, queryParameters);
    }

    public void queryCspaceRequest(LinkedHashMap<String, Object> argMap,
                                   MultivaluedMap<String, String> queryParameters) {
        transformGETRequest(argMap, queryParameters);
    }

    public void queryTabRequest(LinkedHashMap<String, Object> argMap,
                                MultivaluedMap<String, String> queryParameters) {
        transformGETRequest(argMap, queryParameters);
    }

    public void queryTabAsPostRequest(LinkedHashMap<String, Object> argMap,
                                        MultivaluedMap<String, String> queryParameters) {
        transformPOSTRequest(argMap, queryParameters);
    }

    private void transformGETRequest(LinkedHashMap<String, Object> argMap,
                                     MultivaluedMap<String, String> queryParameters) {
        if (queryParameters.containsKey("project_id")) {
            argMap.put("projectId", Integer.parseInt(queryParameters.get("project_id").get(0)));
        }

        if (argMap.containsKey("limit")) {
            argMap.put("limit", Integer.parseInt(queryParameters.getOrDefault("limit", Collections.singletonList("500000")).get(0)));
        }

        List<String> expeditions = null;
        if (argMap.get("expeditionsString") == null) {
             expeditions = new ArrayList<>();
        } else {
            expeditions = Arrays.asList(((String) argMap.get("expeditionsString")).split(","));
        }

        List<String> graphs = new ArrayList<>();
        if (queryParameters.containsKey("graphs")) {
            Collections.addAll(graphs, queryParameters.get("graphs").get(0).split(","));
        }

        transformGraphs(graphs, expeditions);
        argMap.put("expeditionsString", String.join(",", expeditions));
    }

    private void transformPOSTRequest(LinkedHashMap<String, Object> argMap,
                                      MultivaluedMap<String, String> queryParameters) {
        MultivaluedMap<String, String> form = (MultivaluedMap<String, String>) argMap.get("form");

        if (argMap.containsKey("limit")) {
            argMap.put("limit", Integer.parseInt(queryParameters.getOrDefault("limit", Collections.singletonList("500000")).get(0)));
        }

        form.put("projectId", form.remove("project_id"));
        form.put("expeditions", new LinkedList<>());

        transformGraphs(form.remove("graphs"), form.get("expeditions"));
    }

    private void transformGraphs(List<String> graphs, List<String> expeditions) {
        if (graphs != null && graphs.size() > 0) {

            graphs.remove("all");

            if (graphs.size() > 0) {
                for (BcidTmp bcidTmp : bcidService.getBcids(graphs)) {
                    if (bcidTmp.getExpedition() != null) {
                        expeditions.add(bcidTmp.getExpedition().getExpeditionCode());
                    }
                }
            }
        }
    }
}

package biocode.fims.rest.versioning.transformers;

import biocode.fims.entities.Bcid;
import biocode.fims.rest.versioning.Transformer;
import biocode.fims.service.BcidService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * class to transform requests to {@link biocode.fims.rest.services.rest.QueryController} resource methods from
 * {@link biocode.fims.rest.versioning.APIVersion}v1_0 to APIVersion v1_1, and responses from v1_1 to v1_0.
 */
@Component
public class QueryControllerTransformer1_0 implements Transformer {
    private final static Logger logger = LoggerFactory.getLogger(QueryControllerTransformer1_0.class);

    @Autowired
    private BcidService bcidService;

    @Override
    public void updateRequestData(LinkedHashMap<String, Object> argMap, String methodName, MultivaluedMap<String, String> queryParameters) {
        try {
            Method transformMethod = this.getClass().getMethod(methodName + "Request", LinkedHashMap.class, MultivaluedMap.class);
            transformMethod.invoke(this, argMap, queryParameters);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            logger.debug("Problem transforming response for class: " + this.getClass() + " and method: " + methodName + "Response");
        }
    }

    @Override
    public Object updateResponseData(Object returnVal, String methodName) {
        try {
            Method transformMethod = this.getClass().getMethod(methodName + "Response", Object.class);
            return transformMethod.invoke(this, returnVal);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            logger.debug("Problem transforming response for class: " + this.getClass() + " and method: " + methodName + "Response");
        }
        return returnVal;
    }

    public Object queryJsonResponse(Object returnVal) {
        return transformJsonResponse(returnVal);
    }

    public Object queryJsonAsPOSTResponse(Object returnVal) {
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
            entity = (Page<ObjectNode>) new JSONParser().parse(String.valueOf(response.getEntity()));
        } catch (ParseException e) {
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

            data.add(
                    mapper.createObjectNode().put(
                            "row",
                            row
                    )
            );
        }

        v1_0Response.put("data", data);
        v1_0Response.put("header", headers);


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
            argMap.put("projectId", queryParameters.get("project_id").get(0));
        }

        if (!queryParameters.containsKey("limit")) {
            // TODO does this throw an exception if limit is missing?
            queryParameters.addFirst("limit", "10000");
        }

        transformGraphs(argMap, queryParameters.get("graphs"));
    }

    private void transformPOSTRequest(LinkedHashMap<String, Object> argMap,
                                      MultivaluedMap<String, String> queryParameters) {
        MultivaluedMap<String, String> form = (MultivaluedMap<String, String>) argMap.get("form");

        if (!queryParameters.containsKey("limit")) {
            // TODO does this throw an exception if limit is missing?
            queryParameters.addFirst("limit", "10000");
        }

        argMap.put("projectId", form.remove("project_id"));

        transformGraphs(argMap, form.get("graphs"));
    }

    private void transformGraphs(LinkedHashMap<String, Object> argMap, List<String> graphsParam) {
        if (graphsParam != null && graphsParam.size() > 0) {
            List<String> graphs = new ArrayList<>();

            Collections.addAll(graphs, graphsParam.get(0).split(","));

            graphs.remove("all");

            List<String> expeditions = (List<String>) argMap.get("expedition");

            if (graphs.size() > 0) {
                for (Bcid bcid : bcidService.getBcids(graphs)) {
                    if (bcid.getExpedition() != null) {
                        expeditions.add(bcid.getExpedition().getExpeditionCode());
                    }
                }
            }
        }
    }
}

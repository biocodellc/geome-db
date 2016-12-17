package biocode.fims.rest.versioning.transformers;

import biocode.fims.rest.versioning.Transformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * class to transform requests to {@link biocode.fims.rest.services.rest.ProjectController} resource methods from
 * {@link biocode.fims.rest.versioning.APIVersion}v1_0 to APIVersion v1_1, and responses from v1_1 to v1_0.
 */
@Component
public class ProjectControllerTransformer1_0 implements Transformer {
    private final static Logger logger = LoggerFactory.getLogger(ProjectControllerTransformer1_0.class);

    @Override
    public void updateRequestData(LinkedHashMap<String, Object> argMap, String methodName, MultivaluedMap<String, String> queryParameters) {
        try {
            Method transformMethod = this.getClass().getMethod(methodName + "Request", LinkedHashMap.class, MultivaluedMap.class);
            transformMethod.invoke(this, argMap, queryParameters);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            logger.debug("Problem transforming response for class: " + this.getClass() + " and method: " + methodName + "Response\n {}", e);
        } catch (InvocationTargetException e) {
            logger.info("Problem transforming response for class: " + this.getClass() + " and method: " + methodName + "Response\n {}", e);
        }
    }

    @Override
    public Object updateResponseData(Object returnVal, String methodName) {
        try {
            Method transformMethod = this.getClass().getMethod(methodName + "Response", Object.class);
            return transformMethod.invoke(this, returnVal);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            logger.debug("Problem transforming response for class: " + this.getClass() + " and method: " + methodName + "Response\n {}", e);
        } catch (InvocationTargetException e) {
            logger.info("Problem transforming response for class: " + this.getClass() + " and method: " + methodName + "Response\n {}", e);
        }
        return returnVal;
    }

    public Object getFilterOptionsResponse(Object returnVal) {
        if (!(returnVal instanceof Response)) {
            return returnVal;
        }
        Response response = (Response) returnVal;

        ArrayNode entity;
        try {
            entity = (ArrayNode) response.getEntity();
        } catch (ClassCastException e) {
            logger.debug("ParseException occurred", e);
            return returnVal;
        }

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode v1_0Response = mapper.createArrayNode();

        for (JsonNode node : entity) {
            ObjectNode filterOption = (ObjectNode) node;

            ObjectNode tranformedFilterOption = mapper.createObjectNode();

            tranformedFilterOption.set("column", filterOption.path("displayName"));
            tranformedFilterOption.set("uri", filterOption.path("field"));

            v1_0Response.add(tranformedFilterOption);
        }

        return Response.fromResponse(response).entity(v1_0Response).build();
    }
}


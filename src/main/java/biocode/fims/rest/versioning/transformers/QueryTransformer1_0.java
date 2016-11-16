package biocode.fims.rest.versioning.transformers;

import biocode.fims.rest.versioning.Transformer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;

/**
 * class to transform requests to {@link biocode.fims.rest.services.rest.Query} resource methods from
 * {@link biocode.fims.rest.versioning.APIVersion}v1_0 to APIVersion v1_1, and responses from v1_1 to v1_0.
 */
public class QueryTransformer1_0 implements Transformer {
    private final static Logger logger = LoggerFactory.getLogger(QueryTransformer1_0.class);

    @Override
    public void updateRequestData(LinkedHashMap<String, Object> argMap, String methodName) {

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

        JSONArray entity = null;
        try {
            entity = (JSONArray) new JSONParser().parse(String.valueOf(response.getEntity()));
        } catch (ParseException e) {
            logger.debug("ParseException occurred", e);
            return returnVal;
        }

        JSONObject v1_0Response = new JSONObject();
        JSONArray headers = new JSONArray();
        JSONArray data = new JSONArray();

        for (Object obj : entity) {
            JSONObject resource = (JSONObject) obj;

            if (headers.size() == 0) {
                headers.addAll(resource.keySet());
            }

            JSONArray row = new JSONArray();
            for (Object val : resource.values()) {
                row.add(val);
            }

            JSONObject rowObject = new JSONObject();
            rowObject.put("row", row);

            data.add(rowObject);
        }

        v1_0Response.put("data", data);
        v1_0Response.put("header", headers);


        return Response.fromResponse(response).entity(v1_0Response).build();
    }
}

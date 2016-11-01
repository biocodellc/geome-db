package biocode.fims.rest.versioning.transformers;

import biocode.fims.rest.versioning.Transformer;
import biocode.fims.utils.StringGenerator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * class to transform requests to {@link biocode.fims.rest.services.rest.Validate} resource methods from
 * {@link biocode.fims.rest.versioning.APIVersion}v1_0 to APIVersion v1_1, and responses from v1_1 to v1_0.
 */
public class ValidateTransformer1_0 implements Transformer {
    private final static Logger logger = LoggerFactory.getLogger(ValidateTransformer1_0.class);

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

    public Object uploadResponse(Object returnVal) {
        Response response = (Response) returnVal;

        JSONObject entity = null;
        try {
            entity = (JSONObject) new JSONParser().parse(String.valueOf(response.getEntity()));
        } catch (ParseException e) {
            logger.debug("ParseException occurred", e);
            return returnVal;
        }

        JSONObject v1_0Response = entity;

        if (entity.containsKey("done")) {
            JSONObject msgObject = new JSONObject();
            msgObject.put("message", entity.get("done"));
            v1_0Response.put("done", msgObject);
        }
        return Response.fromResponse(response).entity(v1_0Response).build();

    }

    public Object validateResponse(Object returnVal) {
        Response response = (Response) returnVal;

        JSONObject entity = null;
        try {
            entity = (JSONObject) new JSONParser().parse(String.valueOf(response.getEntity()));
        } catch (ParseException e) {
            logger.debug("ParseException occurred", e);
            return returnVal;
        }

        JSONObject v1_0Response = entity;

        JSONObject msg;
        if (entity.containsKey("done")) {
            msg = (JSONObject) entity.get("done");
        } else {
            msg = (JSONObject) entity.get("continue");
        }

        JSONObject worksheets = (JSONObject) msg.get("worksheets");
        JSONArray worksheetArray = new JSONArray();

        Iterator entrySetIt = worksheets.entrySet().iterator();

        while (entrySetIt.hasNext()) {
            Map.Entry entry = (Map.Entry) entrySetIt.next();
            String sheetName = (String) entry.getKey();
            JSONObject warnings = (JSONObject) ((JSONObject) entry.getValue()).get("warnings");
            JSONObject errors = (JSONObject) ((JSONObject) entry.getValue()).get("errors");

            JSONObject msgObj = new JSONObject();

            msgObj.put("warnings", getGroupMsgObjects(warnings));
            msgObj.put("errors", getGroupMsgObjects(errors));

            JSONObject sheetObj = new JSONObject();
            sheetObj.put(sheetName, msgObj);
            worksheetArray.add(sheetObj);
        }

        // overwrite the worksheets object with the worksheet array
        msg.put("worksheets", worksheetArray);

        return Response.fromResponse(response).entity(v1_0Response).build();
    }

    private JSONArray getGroupMsgObjects(JSONObject msgs) {
        JSONArray array = new JSONArray();

        for (Object k: msgs.keySet()) {
            String key = (String) k;
            JSONObject msgGroupObj = new JSONObject();
            msgGroupObj.put(key, msgs.get(key));
            array.add(msgGroupObj);
        }

        return array;
    }
}

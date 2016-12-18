package biocode.fims.rest.versioning.transformers;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import java.util.Iterator;
import java.util.Map;

/**
 * class to transform requests to {@link biocode.fims.rest.services.rest.ValidateController} resource methods from
 * {@link biocode.fims.rest.versioning.APIVersion}v1_0 to APIVersion v1_1, and responses from v1_1 to v1_0.
 */
@Component
public class ValidateControllerTransformer1_0 extends FimsAbstractTransformer {
    private final static Logger logger = LoggerFactory.getLogger(ValidateControllerTransformer1_0.class);

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

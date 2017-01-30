package biocode.fims.rest.versioning.transformers;

import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.MultivaluedMap;
import java.util.LinkedHashMap;

/**
 * class to transform requests to {@link biocode.fims.rest.services.rest.ValidateController} resource methods from
 * {@link biocode.fims.rest.versioning.APIVersion}v1_1 to APIVersion v2, and responses from v2 to v1_1.
 */
@Component
public class ValidateControllerTransformer1_1 extends FimsAbstractTransformer {
    private final static Logger logger = LoggerFactory.getLogger(ValidateControllerTransformer1_1.class);

    public void validateRequest(LinkedHashMap<String, Object> argMap,
                                MultivaluedMap<String, String> queryParameters) {

        FormDataMultiPart multiPart = (FormDataMultiPart) argMap.get("multiPart");
        argMap.put("fimsMetadata", multiPart.getField("dataset"));

        if (multiPart.getField("upload") != null &&
                StringUtils.equalsIgnoreCase(multiPart.getField("upload").getValue(), "on")) {
            argMap.put("upload", true);
        }

    }

}

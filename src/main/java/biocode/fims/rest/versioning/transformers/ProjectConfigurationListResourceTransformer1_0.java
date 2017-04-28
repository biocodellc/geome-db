package biocode.fims.rest.versioning.transformers;

import biocode.fims.digester.Field;
import biocode.fims.rest.versioning.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * class to transform requests to {@link biocode.fims.rest.services.rest.ProjectController} resource methods from
 * {@link biocode.fims.rest.versioning.APIVersion}v1_0 to APIVersion v1_1, and responses from v1_1 to v1_0.
 */
@Component
public class ProjectConfigurationListResourceTransformer1_0 extends FimsAbstractTransformer implements Transformer {
    private final static Logger logger = LoggerFactory.getLogger(ProjectConfigurationListResourceTransformer1_0.class);

    public Object getListFieldsResponse(Object returnVal) {
        if (!(returnVal instanceof List)) {
            return returnVal;
        }

        List<Field> entity;
        try {
            entity = (List<Field>) returnVal;
        } catch (ClassCastException e) {
            logger.debug("ParseException occurred", e);
            return returnVal;
        }

        List<String> v1_0Response = new ArrayList<>();

        for (Field f: entity) {
            v1_0Response.add(f.getValue());
        }

        return v1_0Response;
    }
}


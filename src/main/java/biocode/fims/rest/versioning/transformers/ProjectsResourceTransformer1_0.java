package biocode.fims.rest.versioning.transformers;

import biocode.fims.models.Project;
import biocode.fims.rest.FimsObjectMapper;
import biocode.fims.rest.versioning.Transformer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
public class ProjectsResourceTransformer1_0 extends FimsAbstractTransformer implements Transformer {
    private final static Logger logger = LoggerFactory.getLogger(ProjectsResourceTransformer1_0.class);

    public Object getProjectsResponse(Object returnVal) {
        if (!(returnVal instanceof List)) {
            return returnVal;
        }

        List<Project> entity;
        try {
            entity = (List<Project>) returnVal;
        } catch (ClassCastException e) {
            logger.debug("ParseException occurred", e);
            return returnVal;
        }

        ObjectMapper mapper = new FimsObjectMapper();
        List<ObjectNode> v1_0Response = new ArrayList<>();

        for (Project project: entity) {
            ObjectNode transformedProject = mapper.createObjectNode();

            transformedProject.put("projectId", String.valueOf(project.getProjectId()));
            transformedProject.put("projectCode", project.getProjectCode());
            transformedProject.put("projectTitle", project.getProjectTitle());
            transformedProject.put("validationXml", project.getValidationXml());

            v1_0Response.add(transformedProject);
        }

        return v1_0Response;
    }
}


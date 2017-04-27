package biocode.fims.rest.versioning.transformers;

import biocode.fims.entities.Project;
import biocode.fims.rest.SpringObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * class to transform requests to {@link biocode.fims.rest.services.rest.ProjectController} resource methods from
 * {@link biocode.fims.rest.versioning.APIVersion}v1_0 to APIVersion v1_1, and responses from v1_1 to v1_0.
 */
@Component
public class ProjectControllerTransformer1_0 extends FimsAbstractTransformer {
    private final static Logger logger = LoggerFactory.getLogger(ProjectControllerTransformer1_0.class);


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

    public Object getUserProjectsResponse(Object returnVal) {
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

        ObjectMapper mapper = new SpringObjectMapper();
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


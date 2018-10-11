package biocode.fims.run;

import biocode.fims.config.models.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.UploadCode;
import biocode.fims.models.EntityIdentifier;
import biocode.fims.models.Project;
import biocode.fims.models.User;
import biocode.fims.records.RecordSet;
import biocode.fims.service.ProjectService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
public class GeomeDatasetAuthorizer implements DatasetAuthorizer {

    private final ProjectService projectService;
    private final FimsDatasetAuthorizer fimsDatasetAuthorizer;

    public GeomeDatasetAuthorizer(ProjectService projectService, FimsDatasetAuthorizer fimsDatasetAuthorizer) {
        this.projectService = projectService;
        this.fimsDatasetAuthorizer = fimsDatasetAuthorizer;
    }

    @Override
    public boolean authorize(Dataset dataset, Project project, User user) {
        try {
            fimsDatasetAuthorizer.authorize(dataset, project, user);
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode().equals(UploadCode.USER_NO_OWN_EXPEDITION)) {

                // allow users to create/delete tissues in an expedition that they do not own if they are a member
                List<Entity> uploadEntities = dataset.stream()
                        .filter(RecordSet::hasRecordToPersist)
                        .map(RecordSet::entity)
                        .distinct()
                        .collect(Collectors.toList());

                if (uploadEntities.size() == 1 && uploadEntities.get(0).getConceptAlias().equals("Tissue")
                        && projectService.isUserMemberOfProject(user, project.getProjectId())) {
                    return true;
                }

            }

            throw e;
        }

        return true;
    }

    @Override
    public boolean authorize(EntityIdentifier entityIdentifier, User user) {
        try {
            fimsDatasetAuthorizer.authorize(entityIdentifier, user);
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode().equals(UploadCode.USER_NO_OWN_EXPEDITION)) {

                // allow users to create/delete tissues in an expedition that they do not own if they are a member
                if (entityIdentifier.getConceptAlias().equals("Tissue")
                        && projectService.isUserMemberOfProject(user, entityIdentifier.getExpedition().getProject().getProjectId())) {
                    return true;
                }
            }

            throw e;
        }
        return true;
    }
}

package biocode.fims.rest.services.subResources;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.application.config.PhotosProperties;
import biocode.fims.authorizers.ProjectAuthorizer;
import biocode.fims.config.models.Entity;
import biocode.fims.config.models.PhotoEntity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.models.Expedition;
import biocode.fims.models.Project;
import biocode.fims.models.User;
import biocode.fims.photos.BulkPhotoLoader;
import biocode.fims.photos.BulkPhotoPackage;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.rest.responses.UploadResponse;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import biocode.fims.utils.Flag;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import org.apache.commons.collections.keyvalue.MultiKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipInputStream;

/**
 * @author rjewing
 */
@Controller
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class PhotosResource extends ResumableUploadResource {
    private final static Logger logger = LoggerFactory.getLogger(PhotosResource.class);

    private final ProjectService projectService;
    private final ExpeditionService expeditionService;
    private final ProjectAuthorizer projectAuthorizer;
    private final PhotosProperties photosProps;
    private final RecordRepository recordRepository;
    private final BulkPhotoLoader photoLoader;


    @Autowired
    public PhotosResource(FimsProperties props, ProjectService projectService, ExpeditionService expeditionService,
                          ProjectAuthorizer projectAuthorizer, PhotosProperties photosProps, RecordRepository recordRepository,
                          BulkPhotoLoader photoLoader) {
        super(props);
        this.projectService = projectService;
        this.expeditionService = expeditionService;
        this.projectAuthorizer = projectAuthorizer;
        this.photosProps = photosProps;
        this.recordRepository = recordRepository;
        this.photoLoader = photoLoader;
    }

    @Authenticated
    @Path("{entity: [a-zA-Z0-9_]+}/upload")
    @PUT
    @Consumes({"application/zip", "application/octet-stream"})
    public UploadResponse bulkUpload(@QueryParam("projectId") Integer projectId,
                                     @QueryParam("expeditionCode") String expeditionCode,
                                     @QueryParam("type") UploadType uploadType,
                                     @QueryParam("ignoreId") Flag ignoreId,
                                     @PathParam("entity") String conceptAlias,
                                     InputStream is) {
        clearExpiredUploadEntries();

        User user = userContext.getUser();
        Project project = projectService.getProject(projectId);

        // valid request logic

        if (project == null) {
            String msg = projectId == null ? "Missing required projectId queryParam" : "Invalid projectId queryParam";
            throw new BadRequestException(msg);
        }

        ProjectConfig config = project.getProjectConfig();
        Entity entity = config.entity(conceptAlias);

        if (entity == null) {
            throw new BadRequestException("Invalid entity path param");
        } else if (!(Objects.equals(entity.type(), PhotoEntity.TYPE))) {
            throw new BadRequestException("Specified entity is not a PhotoEntity");
        }

        Expedition expedition;
        if (expeditionCode == null) {
            Entity parentEntity = config.entity(entity.getParentEntity());
            if (!parentEntity.getUniqueAcrossProject()) {
                throw new BadRequestException("The expeditionCode queryParam is missing. however the uniqueKey for parent:\"" + parentEntity.getConceptAlias() + "\" of entity: \"" + conceptAlias + "\" is not uniqueAcrossProject.");
            }

            if (!projectAuthorizer.userHasAccess(user, project)) {
                throw new ForbiddenRequestException("You do not have permission to upload to this project");
            }

        } else if ((expedition = expeditionService.getExpedition(expeditionCode, projectId)) == null) {
            throw new BadRequestException("Invalid expeditionCode queryParam. That expedition does not exist in this project.");
        } else if (!expedition.getUser().equals(user) && !project.getUser().equals(user)) {
            throw new ForbiddenRequestException("You do not have permission to upload to this project or expedition");
        }

        // process file upload

        MultiKey key = getKey(user, projectId, expeditionCode, conceptAlias);
        UploadEntry uploadEntry = getUploadEntry(key, uploadType);

        try {
            // resume or resumable upload
            if (uploadEntry != null) {
                try {
                    is = resumeUpload(is, uploadEntry);
                } catch (EOFException e) {
                    EntityMessages entityMessages = new EntityMessages(conceptAlias);
                    entityMessages.addErrorMessage("Incomplete Upload", new Message("Incomplete file upload"));
                    return new UploadResponse(false, entityMessages);
                }
            }

            // process the bulk upload

            BulkPhotoPackage photoPackage = new BulkPhotoPackage.Builder()
                    .stream(new ZipInputStream(is))
                    .user(user)
                    .project(project)
                    .expeditionCode(expeditionCode)
                    .ignoreId(ignoreId.isPresent())
                    .entity(entity)
                    .photosDir(photosProps.photosDir())
                    .recordRepository(recordRepository)
                    .build();

            // if we are here, we've successfully received the entire file
            UploadResponse uploadResponse = photoLoader.process(photoPackage);

            if (uploadEntry != null) {
                try {
                    uploadEntry.targetFile.delete();
                } catch (Exception e) {
                    // ignore error
                }
            }
            resumableUploads.remove(key);
            return uploadResponse;
        } catch (IOException e) {
            if (!(e.getCause() instanceof TimeoutException)) {
                logger.info("Bulk Upload IOException", e);
            }
            EntityMessages entityMessages = new EntityMessages(conceptAlias);
            entityMessages.addErrorMessage("Incomplete Upload", new Message("Incomplete file upload"));
            return new UploadResponse(false, entityMessages);
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Authenticated
    @GET
    @Path("{entity: [a-zA-Z0-9_]+}/upload/progress")
    public UploadEntry status(@QueryParam("projectId") Integer projectId,
                              @QueryParam("expeditionCode") String expeditionCode,
                              @PathParam("entity") String conceptAlias) {
        User user = userContext.getUser();

        MultiKey key = getKey(user, projectId, expeditionCode, conceptAlias);
        UploadEntry uploadEntry = resumableUploads.get(key);

        if (uploadEntry == null) {
            throw new BadRequestException("Failed to find existing upload for the provided params");
        }

        return uploadEntry;
    }

    private MultiKey getKey(User user, int projectId, String expeditionCode, String conceptAlias) {
        return new MultiKey(user.getUserId(), projectId, expeditionCode, conceptAlias);
    }
}

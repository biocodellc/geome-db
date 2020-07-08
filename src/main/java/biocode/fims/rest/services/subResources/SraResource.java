package biocode.fims.rest.services.subResources;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.application.config.TissueProperties;
import biocode.fims.authorizers.QueryAuthorizer;
import biocode.fims.config.models.Entity;
import biocode.fims.config.models.FastqEntity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.fimsExceptions.errorCodes.GenericErrorCode;
import biocode.fims.fimsExceptions.errorCodes.UploadCode;
import biocode.fims.models.Expedition;
import biocode.fims.models.Project;
import biocode.fims.models.User;
import biocode.fims.ncbi.models.SraSubmissionData;
import biocode.fims.ncbi.sra.submission.*;
import biocode.fims.query.QueryBuilder;
import biocode.fims.query.QueryResults;
import biocode.fims.query.dsl.*;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.repositories.SraSubmissionRepository;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.rest.helpers.SraUploadStore;
import biocode.fims.rest.models.SraUploadMetadata;
import biocode.fims.rest.responses.FileResponse;
import biocode.fims.rest.responses.SraUploadCreateResponse;
import biocode.fims.rest.responses.SraUploadResponse;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import biocode.fims.service.UserService;
import biocode.fims.tools.FileCache;
import biocode.fims.utils.FileUtils;
import org.apache.commons.collections.keyvalue.MultiKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;

/**
 * @author rjewing
 */
@Controller
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class SraResource extends ResumableUploadResource {
    private final static Logger logger = LoggerFactory.getLogger(SraResource.class);

    @Context
    private ServletContext context;

    private final SraUploadStore uploadStore;
    private final ProjectService projectService;
    private final ExpeditionService expeditionService;
    private final UserService userService;
    private final QueryAuthorizer queryAuthorizer;
    private final RecordRepository recordRepository;
    private final FileCache fileCache;
    private final TissueProperties tissueProperties;
    private SraMetadataMapper sraMetadataMapper;
    private BioSampleMapper bioSampleMapperInstance;
    private SraSubmissionRepository sraSubmissionRepository;

    @Autowired
    public SraResource(FimsProperties props, ProjectService projectService, ExpeditionService expeditionService, UserService userService,
                       QueryAuthorizer queryAuthorizer, RecordRepository recordRepository, FileCache fileCache,
                       TissueProperties tissueProperties, SraMetadataMapper sraMetadataMapper,
                       BioSampleMapper bioSampleMapperInstance, SraSubmissionRepository sraSubmissionRepository) {
        super(props);
        this.projectService = projectService;
        this.expeditionService = expeditionService;
        this.userService = userService;
        this.queryAuthorizer = queryAuthorizer;
        this.recordRepository = recordRepository;
        this.fileCache = fileCache;
        this.tissueProperties = tissueProperties;
        this.sraMetadataMapper = sraMetadataMapper;
        this.bioSampleMapperInstance = bioSampleMapperInstance;
        this.sraSubmissionRepository = sraSubmissionRepository;

        this.uploadStore = new SraUploadStore();
    }

    /**
     * TODO Re-write *Mappers to be more robust
     *
     * @param projectId
     * @param expeditionCode
     * @return
     */
    @GET
    @Path("/submissionData")
    public Object getSubmissionData(@QueryParam("projectId") int projectId,
                                    @QueryParam("expeditionCode") String expeditionCode,
                                    @QueryParam("format") @DefaultValue("file") String format) {

        if (projectId == 0 || expeditionCode == null) {
            throw new BadRequestException("both projectId and expeditionCode are required");
        }

        Project project = projectService.getProject(projectId);
        ProjectConfig config = project.getProjectConfig();

        Entity e = config.entities()
                .stream()
                .filter(FastqEntity.class::isInstance)
                .findFirst()
                .orElseThrow((Supplier<RuntimeException>) () -> new BadRequestException("Could not find FastqEntity for provided project"));


        Entity parentEntity = e;
        do {
            parentEntity = config.entity(parentEntity.getParentEntity());
        } while (parentEntity.isChildEntity());

        List<String> entities = config.getEntityRelations(parentEntity, e).stream()
                .flatMap(r -> Stream.of(r.getChildEntity().getConceptAlias(), r.getParentEntity().getConceptAlias()))
                .collect(Collectors.toList());

        Expression exp = new ExpeditionExpression(expeditionCode);
        exp = new LogicalExpression(
                LogicalOperator.AND,
                exp,
                new ProjectExpression(Collections.singletonList(projectId))
        );
        exp = new SelectExpression(
                String.join(",", entities),
                exp
        );

        QueryBuilder qb = new QueryBuilder(config, project.getNetwork().getId(), e.getConceptAlias());
        Query query = new Query(qb, config, exp);

        if (!queryAuthorizer.authorizedQuery(Collections.singletonList(projectId), new ArrayList<>(query.expeditions()), userContext.getUser())) {
            throw new FimsRuntimeException(GenericErrorCode.UNAUTHORIZED, 403);
        }

        QueryResults queryResults = recordRepository.query(query);

        if (queryResults.isEmpty()) return null;

        SraMetadataMapper metadataMapper = sraMetadataMapper.newInstance(config, e, queryResults);
        BioSampleMapper bioSampleMapper = bioSampleMapperInstance.newInstance(config, e, queryResults, props.bcidResolverPrefix());


        if (format.equalsIgnoreCase("json")) {
            return new SraSubmissionData(
                    bioSampleMapper.getBioSamples(),
                    metadataMapper.getResourceMetadataAsMap()
            );
        }

        File bioSampleFile = BioSampleAttributesGenerator.generateFile(bioSampleMapper);
        File sraMetadataFile = SraMetadataGenerator.generateFile(metadataMapper);

        Map<String, File> fileMap = new HashMap<>();
        fileMap.put("bioSample-attributes.tsv", bioSampleFile);
        fileMap.put("sra-metadata.tsv", sraMetadataFile);
        fileMap.put("sra-step-by-step-instructions.pdf", new File(context.getRealPath("docs/sra-step-by-step-instructions.pdf")));

        File zip = FileUtils.zip(fileMap, defaultOutputDirectory());
        String fileId = fileCache.cacheFileForUser(zip, userContext.getUser(), "sra-files.zip");

        return new FileResponse(uriInfo.getBaseUriBuilder(), fileId);
    }


    /**
     * Uploading is a 2 part process.
     * <p>
     * You must first issue a call to this service to store the metadata for the upload.
     * Next you PUT the files to this endpoint
     *
     * @param metadata
     * @return The id of the upload. This is used when uploading the actual Fastq files in order to associate the metadata with the correct files
     */
    @Authenticated
    @Path("/upload")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public SraUploadCreateResponse create(SraUploadMetadata metadata) {
        User user = userContext.getUser();
        Project project = projectService.getProject(metadata.projectId);

        // validate request params

        if (project == null) {
            String msg = metadata.projectId == null ? "Missing required projectId queryParam" : "Invalid projectId queryParam";
            throw new BadRequestException(msg);
        }

        if (!projectService.isUserMemberOfProject(user, project.getProjectId())) {
            throw new FimsRuntimeException(UploadCode.USER_NOT_PROJECT_MEMBER, 400, project.getProjectTitle());
        }

        ProjectConfig config = project.getProjectConfig();
        Entity entity = config.entities().stream()
                .filter(FastqEntity.class::isInstance)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("The specified project does not support Fastq data."));


        Expedition expedition = expeditionService.getExpedition(metadata.expeditionCode, metadata.projectId);
        if (expedition == null) {
            throw new BadRequestException("Invalid expeditionCode. That expedition does not exist in this project.");
        } else if (!project.isEnforceExpeditionAccess() && !expedition.getUser().equals(user) && !project.getUser().equals(user)) {
            throw new ForbiddenRequestException("You do not have permission to upload to this project and/or expedition");
        }
        metadata.project = project;
        metadata.expedition = expedition;

        if (metadata.setUserSraProfile(user)) {
            userService.update(user);
        }

        UUID processId = UUID.randomUUID();
        uploadStore.put(processId, metadata, user.getUserId());

        return new SraUploadCreateResponse(processId);
    }

    @Authenticated
    @Path("/upload")
    @PUT
    @Consumes({"application/zip", "application/octet-stream"})
    public SraUploadResponse upload(@QueryParam("id") UUID uploadId,
                                    @QueryParam("type") UploadType uploadType,
                                    InputStream is) {
        clearExpiredUploadEntries();

        SraUploadMetadata metadata = uploadStore.get(uploadId, userContext.getUser().getUserId());

        if (metadata == null) {
            throw new BadRequestException("Could not find an upload entry with the id \"" + uploadId + "\"." +
                    " upload entries expire after 24 hrs.");
        }

        // process file upload

        MultiKey key = getKey(userContext.getUser(), uploadId);
        UploadEntry uploadEntry = getUploadEntry(key, uploadType);

        try {
            // resume or resumable upload
            if (uploadEntry != null) {
                try {
                    is = resumeUpload(is, uploadEntry);
                } catch (EOFException e) {
                    return new SraUploadResponse(false, "Incomplete file upload");
                }
            }

            // process the upload

            // if we are here, we've successfully received the entire file
            SraSubmissionData sraSubmissionData = (SraSubmissionData) this.getSubmissionData(metadata.projectId, metadata.expeditionCode, "json");

            try {
                return new SraLoader(
                        metadata,
                        new ZipInputStream(is),
                        sraSubmissionData,
                        tissueProperties.sraSubmissionDir(),
                        userContext.getUser(),
                        props.appRoot(),
                        sraSubmissionRepository
                ).upload();
            } finally {
                if (uploadEntry != null) {
                    try {
                        uploadEntry.targetFile.delete();
                    } catch (Exception e) {
                        // ignore error
                        logger.debug("Failed to delete uploadEntry file");
                    }
                }
                resumableUploads.remove(getKey(userContext.getUser(), uploadId));
            }
        } catch (IOException e) {
            logger.debug("Error", e);
            if (!(e.getCause() instanceof TimeoutException)) {
                logger.info("Sra submission IOException", e);
            }
            return new SraUploadResponse(false, "Incomplete file upload");
        } finally {
            try {
                is.close();
            } catch (IOException ignored) {
                logger.debug("Error closing input stream", ignored);
            }
        }
    }

    @Authenticated
    @GET
    @Path("/upload/progress")
    public UploadEntry status(@QueryParam("id") UUID id) {
        User user = userContext.getUser();

        MultiKey key = getKey(user, id);
        UploadEntry uploadEntry = resumableUploads.get(key);

        if (uploadEntry == null) {
            throw new BadRequestException("Failed to find existing upload for the provided params");
        }

        return uploadEntry;
    }

    private MultiKey getKey(User user, UUID id) {
        return new MultiKey(user.getUserId(), id);
    }
}

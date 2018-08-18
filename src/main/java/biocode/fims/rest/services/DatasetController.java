package biocode.fims.rest.services;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.authorizers.QueryAuthorizer;
import biocode.fims.projectConfig.ColumnComparator;
import biocode.fims.projectConfig.models.Entity;
import biocode.fims.fimsExceptions.errorCodes.ProjectCode;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.models.Expedition;
import biocode.fims.models.Project;
import biocode.fims.fimsExceptions.*;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.errorCodes.UploadCode;
import biocode.fims.models.User;
import biocode.fims.models.records.RecordMetadata;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.query.QueryBuilder;
import biocode.fims.query.QueryResult;
import biocode.fims.query.QueryResults;
import biocode.fims.query.dsl.*;
import biocode.fims.query.writers.DelimitedTextQueryWriter;
import biocode.fims.query.writers.QueryWriter;
import biocode.fims.reader.DataConverterFactory;
import biocode.fims.reader.DataReaderFactory;
import biocode.fims.rest.responses.FileResponse;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.responses.ValidationResponse;
import biocode.fims.tools.FileCache;
import biocode.fims.tools.ValidationStore;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.run.DatasetProcessor;
import biocode.fims.run.ProcessorStatus;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import biocode.fims.tools.UploadStore;
import biocode.fims.utils.FileUtils;
import biocode.fims.run.DataSourceMetadata;
import biocode.fims.validation.RecordValidatorFactory;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.sql.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * @resourceTag Data
 * @resourceDescription Validate and load data
 */
@Controller
@Path("data")
@Produces({MediaType.APPLICATION_JSON})
public class DatasetController extends FimsController {
    private final static Logger logger = LoggerFactory.getLogger(DatasetController.class);

    private final ExpeditionService expeditionService;
    private final RecordValidatorFactory validatorFactory;
    private final RecordRepository recordRepository;
    private final ProjectService projectService;
    private final DataReaderFactory readerFactory;
    private final DataConverterFactory dataConverterFactory;
    private final QueryAuthorizer queryAuthorizer;
    private final FileCache fileCache;

    private final UploadStore uploadStore;
    private final ValidationStore validationStore;

    public DatasetController(ExpeditionService expeditionService, DataReaderFactory readerFactory,
                             RecordValidatorFactory validatorFactory, RecordRepository recordRepository,
                             ProjectService projectService, QueryAuthorizer queryAuthorizer, FileCache fileCache,
                             DataConverterFactory dataConverterFactory, FimsProperties props) {
        super(props);
        this.expeditionService = expeditionService;
        this.readerFactory = readerFactory;
        this.validatorFactory = validatorFactory;
        this.recordRepository = recordRepository;
        this.projectService = projectService;
        this.queryAuthorizer = queryAuthorizer;
        this.fileCache = fileCache;
        this.dataConverterFactory = dataConverterFactory;

        this.uploadStore = new UploadStore();
        this.validationStore = new ValidationStore();
    }


    /**
     * Validate a dataset
     *
     * @param projectId
     * @param expeditionCode
     * @param dataSourceMetadata
     * @param dataSourceFiles
     * @param workbookFiles
     * @param upload
     * @param reloadWorkbooks
     * @param waitForCompletion  If false, the request will be processed aschronyously. The response will contain a validation Id
     *                           that can be used to fetch the current status of the validation including the results when finished.
     * @return
     */
    @POST
    @Path("validate")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public ValidationResponse validate(@FormDataParam("projectId") Integer projectId,
                                       @FormDataParam("expeditionCode") String expeditionCode,
                                       @FormDataParam("dataSourceMetadata") List<DataSourceMetadata> dataSourceMetadata,
                                       @FormDataParam("dataSourceFiles") List<FormDataBodyPart> dataSourceFiles,
                                       @FormDataParam("workbooks") List<FormDataBodyPart> workbookFiles,
                                       @FormDataParam("upload") boolean upload,
                                       @FormDataParam("reloadWorkbooks") @DefaultValue("false") boolean reloadWorkbooks,
                                       @QueryParam("waitForCompletion") @DefaultValue("true") boolean waitForCompletion) {

        //TODO need to handle un-authenticated & missing expeditionCode validation cases. We can still attempt to validate, but can't fetch any parent entities that don't exist on the sheet

        if (projectId == null || (workbookFiles == null && dataSourceFiles == null)) {
            throw new BadRequestException("projectId, and either workbooks or dataSourceFiles are required.");
        }

        if (upload && userContext.getUser() == null) {
            throw new UnauthorizedRequestException("You must be logged in to upload.");
        }


        // create a new processorStatus
        ProcessorStatus processorStatus = new ProcessorStatus();

        Project project = projectService.getProject(projectId);

        if (project == null) {
            throw new FimsRuntimeException(ProjectCode.INVALID_PROJECT, 400);
        }

        DatasetProcessor.Builder builder = new DatasetProcessor.Builder(projectId, expeditionCode, processorStatus)
                .user(userContext.getUser())
                .projectConfig(project.getProjectConfig())
                .readerFactory(readerFactory)
                .dataConverterFactory(dataConverterFactory)
                .recordRepository(recordRepository)
                .validatorFactory(validatorFactory)
                .expeditionService(expeditionService)
                .ignoreUser(props.ignoreUser())
                .serverDataDir(props.serverRoot())
                .reloadWorkbooks(reloadWorkbooks);

        UUID processId = UUID.randomUUID();

        // we need to save all files before async processing. If we attempt to save
        // the files in the async code, Jersey will sporadically throw the error
        // No such MIME Part: Part=4:binary
        Map<String, String> workbooks = saveFiles(workbookFiles);
        Map<String, String> dataSources = saveFiles(dataSourceFiles);

        // need to copy params to effectively final variables to using inside lambda function
        List<DataSourceMetadata> finalDataSourceMetadata = dataSourceMetadata == null ? Collections.emptyList() : dataSourceMetadata;
        User user = userContext.getUser();
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();

        // run validation asynchronously
        CompletableFuture<ValidationResponse> future = CompletableFuture.supplyAsync(() -> {
            // update the status
            processorStatus.appendStatus("Initializing...");

            if (workbooks.size() > 0) {
                for (Map.Entry<String, String> e : workbooks.entrySet()) {
                    processorStatus.appendStatus("\nExcel workbook filename = " + e.getKey());
                    builder.workbook(e.getValue());
                }
            }

            if (finalDataSourceMetadata.size() > 0 || dataSources.size() > 0) {

                for (Map.Entry<String, String> e : dataSources.entrySet()) {
                    RecordMetadata recordMetadata = null;

                    processorStatus.appendStatus("\nDataSourceMetadata filename = " + e.getKey());

                    for (DataSourceMetadata metadata : finalDataSourceMetadata) {
                        if (StringUtils.equals(e.getKey(), metadata.getFilename())) {

                            recordMetadata = metadata.toRecordMetadata(readerFactory.getReaderTypes());

                            // remove the metadata so we can later verify that every dataSourceMetadata contains a matching dataSourceFile
                            finalDataSourceMetadata.remove(metadata);
                            break;
                        }
                    }

                    if (recordMetadata == null) {
                        throw new BadRequestException("could not find a matching DataSourceMetadata object for dataSourceFile: " + e.getKey(),
                                "Make sure every dataSourceFile has a corresponding dataSourceMetadata object with the correct DataSourceMetadata.filename");
                    }

                    builder.addDataset(e.getValue(), recordMetadata);
                }

            }

            DatasetProcessor processor = builder.build();

            if (upload) {
                boolean isvalid = processor.validate();

                if (processor.hasError()) {
                    return new ValidationResponse(
                            null,
                            isvalid,
                            true,
                            processor.messages(),
                            null
                    );
                } else {

                    uploadStore.put(processId, processor, user.getUserId());

                    URI uploadUri = uriBuilder
                            .path("data/upload/{id}") //TODO find a better way to do this
                            .build(processId);

                    return new ValidationResponse(
                            processId,
                            isvalid,
                            processor.hasError(),
                            processor.messages(),
                            uploadUri.toString()
                    );
                }

            } else {
                boolean isvalid = processor.validate();

                return new ValidationResponse(
                        null,
                        isvalid,
                        processor.hasError(),
                        processor.messages(),
                        null
                );
            }
        });

        if (waitForCompletion) {
            try {
                // waits for validation to complete
                return future.get();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();

                int status = 500;
                String userMessage = "Server Error";
                String developerMessage = "Server Error";
                if (cause instanceof FimsAbstractException) {
                    status = ((FimsAbstractException) cause).getHttpStatusCode();
                    userMessage = ((FimsAbstractException) cause).getUsrMessage();
                    developerMessage = ((FimsAbstractException) cause).getDeveloperMessage();
                }
                throw new FimsRuntimeException(userMessage, developerMessage, status, cause);
            } catch (InterruptedException e) {
                throw new FimsRuntimeException(500, e);
            }
        }

        int userId = user == null ? 0 : user.getUserId();
        UUID id = validationStore.put(processId, processorStatus, userId);

        future.whenCompleteAsync(((validationResponse, throwable) -> {
            if (throwable != null) logger.error("Exception during dataset validation", throwable);
            validationStore.update(id, validationResponse, throwable);
        }));

        return new ValidationResponse(id);
    }

    /**
     * Get the status/results of a dataset validation.
     * <p>
     * The results are removed after 2hrs
     *
     * @return
     */
    @GET
    @Path("/validate/{id}")
    public ValidationResponse status(@PathParam("id") UUID id) {
        int userId = userContext.getUser() == null ? 0 : userContext.getUser().getUserId();
        ValidationStore.ValidationResult result = validationStore.get(id, userId);

        // if no result is found, we can't do anything
        if (result == null) {
            throw new FimsRuntimeException("Could not find a dataset validation with the id \"" + id + "\"." +
                    " Validation results expire after 2 hrs.", 400);
        }

        if (result.exception() != null) {
            String msg = result.exception() instanceof FimsAbstractException
                    ? ((FimsAbstractException) result.exception()).getUsrMessage()
                    : "Server Error";

            return ValidationResponse.withException(msg);

        }

        if (result.response() != null) {
            return result.response();
        }

        return ValidationResponse.withStatus(result.status().statusHtml());
    }

    /**
     * Service to upload a dataset to an expedition. The validate service must be called before this service.
     *
     * @param id required. The dataset id returned from the validate service
     * @responseType biocode.fims.rest.services.DatasetController.UploadResponse
     */
    @Authenticated
    @PUT
    @Path("/upload/{id}")
    public Response upload(@PathParam("id") UUID id) {
        if (id == null) {
            throw new BadRequestException("id queryParam is required");
        }

        int userId = userContext.getUser() != null ? userContext.getUser().getUserId() : 0;
        DatasetProcessor processor = uploadStore.get(id, userId);

        // if no processor is found, we can't do anything
        if (processor == null) {
            throw new FimsRuntimeException("Could not find a validated dataset with the id \"" + id + "\"." +
                    " Validated datasets expire after 5 mins.", 400);
        }

        try {
            processor.upload();
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == UploadCode.INVALID_EXPEDITION) {
                String message = "The expedition code \"" + processor.expeditionCode() + "\" does not exist.";

                return Response.status(400)
                        .entity(new UploadResponse(false, message))
                        .build();
            } else {
                throw e;
            }
        }

        uploadStore.invalidate(id);
        return Response.ok(new UploadResponse(true, "Successfully Uploaded!")).build();
    }

    /**
     * Export all data for a given expedition
     *
     * @param projectId
     * @param expeditionCode
     * @return
     */
    @GET
    @Path("/export/{projectId}/{expeditionCode}")
    public FileResponse export(@PathParam("projectId") int projectId,
                               @PathParam("expeditionCode") String expeditionCode) {

        if (!queryAuthorizer.authorizedQuery(Collections.singletonList(projectId), Collections.singletonList(expeditionCode), userContext.getUser())) {
            throw new ForbiddenRequestException("You are not authorized to access this expeditions data.");
        }

        Expedition expedition = expeditionService.getExpedition(expeditionCode, projectId);

        if (expedition == null) {
            throw new NotFoundException("could not find expedition");
        }

        ProjectConfig config = expedition.getProject().getProjectConfig();

        List<String> entities = config.entities().stream().map(Entity::getConceptAlias).collect(Collectors.toList());

        ExpeditionExpression expeditionExpression = new ExpeditionExpression(expeditionCode);
        Expression exp = new SelectExpression(
                String.join(",", entities.subList(1, entities.size() - 1)),
                expeditionExpression
        );

        QueryBuilder qb = new QueryBuilder(expedition.getProject(), entities.get(0));
        Query query = new Query(qb, config, exp);

        QueryResults result = recordRepository.query(query);
        QueryWriter queryWriter = new DelimitedTextQueryWriter(result, ",", config);

        File file = null;
        try {
            file = queryWriter.write();
        } catch (FimsRuntimeException e) {
            if (!e.getErrorCode().equals(QueryCode.NO_RESOURCES)) {
                throw e;
            }
        }

        if (file == null) {
            return null;
        }

        String fileId = fileCache.cacheFileForUser(file, userContext.getUser(), expeditionCode + "-export.zip");

        return new FileResponse(uriInfo.getBaseUriBuilder(), fileId);
    }

    private Map<String, String> saveFiles(List<FormDataBodyPart> sources) {
        Map<String, String> files = new HashMap<>();

        if (sources != null) {
            for (FormDataBodyPart data : sources) {
                String fileName = data.getContentDisposition().getFileName();
                InputStream is = data.getEntityAs(InputStream.class);
                String tmpFilename = saveFile(is, fileName, "");
                files.put(fileName, tmpFilename);
            }
        }

        return files;
    }

    private String saveFile(InputStream is, String filename, String defaultExt) {
        String ext = FileUtils.getExtension(filename, defaultExt);
        String tempFilename = FileUtils.saveTempFile(is, ext);
        if (tempFilename == null) {
            throw new FimsRuntimeException("Server error saving file: " + filename, 500);
        }

        return tempFilename;
    }

    private static class UploadResponse {
        private boolean success;
        private String message;

        public UploadResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}



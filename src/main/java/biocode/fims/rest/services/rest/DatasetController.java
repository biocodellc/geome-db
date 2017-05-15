package biocode.fims.rest.services.rest;

import biocode.fims.models.Project;
import biocode.fims.fimsExceptions.*;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.errorCodes.UploadCode;
import biocode.fims.models.records.RecordMetadata;
import biocode.fims.reader.DataReaderFactory;
import biocode.fims.renderers.EntityMessages;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.rest.FimsService;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.run.DatasetProcessor;
import biocode.fims.run.ProcessorStatus;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import biocode.fims.settings.SettingsManager;
import biocode.fims.tools.UploadStore;
import biocode.fims.utils.FileUtils;
import biocode.fims.run.DataSourceMetadata;
import biocode.fims.validation.RecordValidatorFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * @resourceTag Data
 * @resourceDescription Validate and load data
 */
@Controller
@Path("data")
public class DatasetController extends FimsService {

    private final ExpeditionService expeditionService;
    private final RecordValidatorFactory validatorFactory;
    private final RecordRepository recordRepository;
    private final ProjectService projectService;
    private final DataReaderFactory readerFactory;

    private final UploadStore uploadStore;

    public DatasetController(ExpeditionService expeditionService, DataReaderFactory readerFactory,
                             RecordValidatorFactory validatorFactory, RecordRepository recordRepository,
                             ProjectService projectService, SettingsManager settingsManager) {
        super(settingsManager);
        this.expeditionService = expeditionService;
        this.readerFactory = readerFactory;
        this.validatorFactory = validatorFactory;
        this.recordRepository = recordRepository;
        this.projectService = projectService;

        this.uploadStore = new UploadStore();
    }


    @POST
    @Path("validate")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public ValidationResponse validate(@FormDataParam("projectId") Integer projectId,
                                       @FormDataParam("expeditionCode") String expeditionCode,
                                       @FormDataParam("dataSourceMetadata") List<DataSourceMetadata> dataSourceMetadata,
                                       @FormDataParam("dataSourceFiles") List<FormDataBodyPart> dataSourceFiles,
                                       @FormDataParam("workbooks") List<FormDataBodyPart> workbooks,
                                       @FormDataParam("upload") boolean upload,
                                       @FormDataParam("public") @DefaultValue("false") boolean isPublic) {

        Project project = projectService.getProject(projectId, appRoot);

        if (project == null) {
            throw new BadRequestException("Project not found");
        }

        // create a new processorStatus
        ProcessorStatus processorStatus = new ProcessorStatus();

        try {
            // place the processorStatus in the session here so that we can track the status of the validation process
            // by calling biocode.fims.rest/validate/status
            session.setAttribute("processorStatus", processorStatus);

            DatasetProcessor.Builder builder = new DatasetProcessor.Builder(projectId, expeditionCode, processorStatus)
                    .user(userContext.getUser())
                    .projectConfig(project.getProjectConfig())
                    .readerFactory(readerFactory)
                    .recordRepository(recordRepository)
                    .validatorFactory(validatorFactory)
                    .expeditionService(expeditionService)
                    .ignoreUser(Boolean.parseBoolean(settingsManager.retrieveValue("ignoreUser")))
                    .publicStatus(isPublic)
                    .serverDataDir(settingsManager.retrieveValue("serverRoot"))
                    .reloadDataset(false);

            // update the status
            processorStatus.appendStatus("Initializing...");

            if (workbooks != null && workbooks.size() > 0) {
                for (FormDataBodyPart workbookData : workbooks) {
                    String workbookFilename = workbookData.getContentDisposition().getFileName();
                    processorStatus.appendStatus("\nExcel workbook filename = " + workbookFilename);

                    InputStream is = workbookData.getEntityAs(InputStream.class);
                    String tmpFilename = saveFile(is, workbookFilename, "");

                    builder.workbook(tmpFilename);
                }
            }

            if ((dataSourceMetadata != null && dataSourceMetadata.size() > 0)
                    || (dataSourceFiles != null && dataSourceFiles.size() > 0)) {

                for (FormDataBodyPart dataSourceFile : dataSourceFiles) {
                    RecordMetadata recordMetadata = null;

                    String dataSourceFilename = dataSourceFile.getContentDisposition().getFileName();
                    processorStatus.appendStatus("\nDataSourceMetadata filename = " + dataSourceFilename);

                    for (DataSourceMetadata metadata : dataSourceMetadata) {
                        if (StringUtils.equals(dataSourceFilename, metadata.getFilename())) {

                            recordMetadata = metadata.toRecordMetadata(readerFactory.getReaderTypes());

                            // remove the metadata so we can later verify that every dataSourceMetadata contains a matching dataSourceFile
                            dataSourceMetadata.remove(metadata);
                            break;
                        }
                    }

                    if (recordMetadata == null) {
                        throw new BadRequestException("could not find a matching DataSourceMetadata object for dataSourceFile: " + dataSourceFilename,
                                "Make sure every dataSourceFile has a corresponding dataSourceMetadata object with the correct DataSourceMetadata.filename");
                    }

                    InputStream is = dataSourceFile.getEntityAs(InputStream.class);
                    String tmpFilename = saveFile(is, dataSourceFilename, "");

                    builder.addDataset(tmpFilename, recordMetadata);
                }

            }

            DatasetProcessor processor = builder.build();

            if (upload) {
                if (userContext.getUser() == null) {
                    throw new UnauthorizedRequestException("You must be logged in to upload.");
                }

                boolean isvalid = processor.validate();

                UUID id = uploadStore.put(processor, userContext.getUser().getUserId());

                URI uploadUri = UriBuilder
                        .fromMethod(this.getClass(), "upload")
                        .queryParam("id", id)
                        .build();

                return new ValidationResponse(
                        id,
                        isvalid,
                        processor.hasError(),
                        processor.messages(),
                        uploadUri.toString()
                );

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

        } finally {
            session.removeAttribute("processorStatus");
        }
    }

    /**
     * Service to upload a dataset to an expedition. The validate service must be called before this service.
     *
     * @param id               required. The dataset id returned from the validate service
     * @param createExpedition Do you want to create the expedition if it does not exist?
     * @return
     */
    @Authenticated
    @PUT
    @Path("/upload/{id}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public UploadResponse upload(@PathParam("id") UUID id,
                                 @QueryParam("createExpedition") @DefaultValue("false") Boolean createExpedition) {
        if (id == null) {
            throw new BadRequestException("id queryParam is required");
        }

        int userId = userContext.getUser() != null ? userContext.getUser().getUserId() : 0;
        DatasetProcessor processor = uploadStore.get(id, userId);

        // if no processorStatus is found, we can't do anything
        if (processor == null) {
            throw new FimsRuntimeException("Could not find a validated dataset with the id \"" + id + "\"." +
                    " Validated datasets expire after 5 mins.", 400);
        }

        try {
            // place the processorStatus in the session here so that we can track the status of upload process
            session.setAttribute("processorStatus", processor.status());

            try {
                processor.upload(createExpedition);
            } catch (FimsRuntimeException e) {
                if (e.getErrorCode() == UploadCode.EXPEDITION_CREATE) {
                    String message = "The expedition code \"" + processor.expeditionCode() + "\" does not exist.";

                    URI uri = uriInfo.getRequestUriBuilder().queryParam("createExpedition", true).build();
                    return new UploadResponse(false, message, uri.toString());
                } else {
                    throw e;
                }
            }

            uploadStore.invalidate(id);
            return new UploadResponse(true, "Successfully Uploaded!", null);

        } finally {
            session.removeAttribute("processorStatus");
        }
    }

    /**
     * Service used for getting the current status of the dataset validation/upload.
     *
     * @return
     */
    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public String status() {
        ProcessorStatus processorStatus = (ProcessorStatus) session.getAttribute("processorStatus");

        if (processorStatus == null) {
            throw new BadRequestException("No dataset is being validated");
        }

        return "{\"status\": \"" + processorStatus.statusHtml() + "\"}";
    }

    private String saveFile(InputStream is, String filename, String defaultExt) {
        String ext = FileUtils.getExtension(filename, defaultExt);
        String tempFilename = FileUtils.saveTempFile(is, ext);
        if (tempFilename == null) {
            throw new FimsRuntimeException("Server error saving file: " + filename, 500);
        }

        return tempFilename;
    }


    private static class ValidationResponse {
        @JsonProperty
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private UUID id;
        @JsonProperty
        private boolean isValid;
        @JsonProperty
        private boolean hasError;
        @JsonProperty
        private List<EntityMessages> messages;
        @JsonProperty
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String uploadUrl;

        public ValidationResponse(UUID id, boolean isValid, boolean hasError, List<EntityMessages> messages, String uploadUrl) {
            this.id = id;
            this.isValid = isValid;
            this.hasError = hasError;
            this.messages = messages;
            this.uploadUrl = uploadUrl;
        }
    }

    private static class UploadResponse {
        @JsonProperty
        private boolean success;
        @JsonProperty
        private String message;
        @JsonProperty
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String uploadUrl;

        public UploadResponse(boolean success, String message, String uploadUrl) {
            this.success = success;
            this.message = message;
            this.uploadUrl = uploadUrl;
        }
    }
}



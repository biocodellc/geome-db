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
import biocode.fims.run.DatasetProcessor;
import biocode.fims.run.ProcessController;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import biocode.fims.settings.SettingsManager;
import biocode.fims.utils.FileUtils;
import biocode.fims.run.DataSourceMetadata;
import biocode.fims.validation.RecordValidatorFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.simple.JSONObject;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;

/**
 * @resourceTag Data
 * @resourceDescription Validate and load data
 */
@Scope("prototype")
@Controller
@Path("validate")
public class ValidateController extends FimsService {

    private final ExpeditionService expeditionService;
    private final RecordValidatorFactory validatorFactory;
    private final RecordRepository recordRepository;
    private final ProjectService projectService;
    private final DataReaderFactory readerFactory;

    public ValidateController(ExpeditionService expeditionService, DataReaderFactory readerFactory,
                              RecordValidatorFactory validatorFactory, RecordRepository recordRepository,
                              ProjectService projectService, SettingsManager settingsManager) {
        super(settingsManager);
        this.expeditionService = expeditionService;
        this.readerFactory = readerFactory;
        this.validatorFactory = validatorFactory;
        this.recordRepository = recordRepository;
        this.projectService = projectService;
    }

    /**
     * service to validate a dataset against a project's rules
     *
     * @param projectId
     * @param expeditionCode
     * @param upload
     * @return
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public ValidationResponse validate(@FormDataParam("projectId") Integer projectId,
                                       @FormDataParam("expeditionCode") String expeditionCode,
                                       @FormDataParam("dataSourceMetadata") List<DataSourceMetadata> dataSourceMetadata,
                                       @FormDataParam("dataSourceFiles") List<FormDataBodyPart> dataSourceFiles,
                                       @FormDataParam("workbooks") List<FormDataBodyPart> workbooks,
                                       @FormDataParam("upload") boolean upload,
                                       @FormDataParam("public") @DefaultValue("false") boolean isPublic) {
        boolean removeController = true;

        Project project = projectService.getProject(projectId, appRoot);

        if (project == null) {
            throw new BadRequestException("Project not found");
        }

        // create a new processController
        ProcessController processController = new ProcessController(projectId, expeditionCode);
        processController.setOutputFolder(defaultOutputDirectory());

        // place the processController in the session here so that we can track the status of the validation process
        // by calling biocode.fims.rest/validate/status
        session.setAttribute("processController", processController);

        DatasetProcessor.Builder builder = new DatasetProcessor.Builder(processController)
                .projectConfig(project.getProjectConfig())
                .readerFactory(readerFactory)
                .recordRepository(recordRepository)
                .validatorFactory(validatorFactory)
                .reloadDataset(false);


        try {
            // update the status
            processController.appendStatus("Initializing...<br>");

            if (workbooks != null && workbooks.size() > 0) {
                for (FormDataBodyPart workbookData : workbooks) {
                    String workbookFilename = workbookData.getContentDisposition().getFileName();
                    processController.appendStatus("\nExcel workbook filename = " + workbookFilename);

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
                    processController.appendStatus("\nDataSourceMetadata filename = " + dataSourceFilename);

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

            processController.setDatasetProcessor(processor);

            if (upload) {
                if (userContext.getUser() == null) {
                    throw new UnauthorizedRequestException("You must be logged in to upload.");
                }
                processController.setUserId(userContext.getUser().getUserId());
                processController.setPublicStatus(isPublic);

                // don't remove the controller as we will need it later for uploading this file
                removeController = false;

                boolean isvalid = processor.validate();

                return new ValidationResponse(
                        isvalid,
                        processor.hasError(),
                        processor.messages(),
                        null //TODO set this
                );

            } else {
                boolean isvalid = processor.validate();

                return new ValidationResponse(
                        isvalid,
                        processor.hasError(),
                        processor.messages(),
                        null
                );
            }

        } finally {
            if (removeController) {
                session.removeAttribute("processController");
            }

        }
    }

    /**
     * Service to upload a dataset to an expedition. The validate service must be called before this service.
     *
     * @param createExpedition
     * @return
     */
    @GET
    @Path("/continue")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response upload(@QueryParam("createExpedition") @DefaultValue("false") Boolean createExpedition) {
        ProcessController processController = (ProcessController) session.getAttribute("processController");
        JSONObject returnValue = new JSONObject();
        boolean removeProcessController = true;

        // if no processController is found, we can't do anything
        if (processController == null) {
            //TODO throw exception with ErrorCode
            returnValue.put("error", "No process was detected");
            return Response.ok(returnValue).build();
        }

        try {

            // check if user is logged in
            if (processController.getUserId() == 0) {
                returnValue.put("error", "You must be logged in to upload");
                return Response.ok(returnValue).build();
            }


            DatasetProcessor p = processController.getDatasetProcessor();

            if (createExpedition) {
                processController.setExpeditionTitle(processController.getExpeditionCode() + " spreadsheet");
            }

            try {
                p.upload(createExpedition, Boolean.parseBoolean(settingsManager.retrieveValue("ignoreUser")), expeditionService);
            } catch (FimsRuntimeException e) {
                if (e.getErrorCode() == UploadCode.EXPEDITION_CREATE) {
                    JSONObject message = new JSONObject();
                    message.put("message",
                            "The expedition code \"" + processController.getExpeditionCode() +
                                    "\" does not exist.  " +
                                    "Do you wish to create it now?<br><br>" +
                                    "If you choose to continue, your data will be associated with this new expedition code."
                    );
                    returnValue.put("continue", message);
                    removeProcessController = false;
                    return Response.ok(returnValue).build();
                } else {
                    throw e;
                }
            }

            returnValue.put("done", processController.getSuccessMessage());
            return Response.ok(returnValue).build();
        } finally {
            // remove the processController from the session
            if (removeProcessController) {
                session.removeAttribute("processController");
//                processController.getDatasetProcessor().close();
            }
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
        ProcessController processController = (ProcessController) session.getAttribute("processController");
        if (processController == null) {
            return "{\"error\": \"Waiting for validation to process...\"}";
        }

        return "{\"status\": \"" + processController.getStatusSB().toString() + "\"}";
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
        private boolean isValid;
        @JsonProperty
        private boolean hasError;
        @JsonProperty
        private List<EntityMessages> messages;
        @JsonProperty
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private String uploadUrl;

        public ValidationResponse(boolean isValid, boolean hasError, List<EntityMessages> messages, String uploadUrl) {
            this.isValid = isValid;
            this.hasError = hasError;
            this.messages = messages;
            this.uploadUrl = uploadUrl;
        }
    }
}



package biocode.fims.rest.services.rest;

import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.dipnet.entities.DipnetExpedition;
import biocode.fims.dipnet.entities.FastqMetadata;
import biocode.fims.dipnet.services.DipnetExpeditionService;
import biocode.fims.entities.Expedition;
import biocode.fims.fileManagers.AuxilaryFileManager;
import biocode.fims.fileManagers.dataset.DatasetFileManager;
import biocode.fims.fimsExceptions.*;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.rest.FimsService;
import biocode.fims.run.Process;
import biocode.fims.run.ProcessController;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.OAuthProviderService;
import biocode.fims.settings.SettingsManager;
import biocode.fims.utils.FileUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.media.multipart.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@Path("validate")
public class Validate extends FimsService {

    private final ExpeditionService expeditionService;
    private final DipnetExpeditionService dipnetExpeditionService;
    private final List<AuxilaryFileManager> fileManagers;
    private final DatasetFileManager datasetFileManager;

    @Autowired
    Validate(ExpeditionService expeditionService, DipnetExpeditionService dipnetExpeditionService,
             List<AuxilaryFileManager> fileManagers, DatasetFileManager datasetFileManager,
             OAuthProviderService providerService, SettingsManager settingsManager) {
        super(providerService, settingsManager);
        this.expeditionService = expeditionService;
        this.dipnetExpeditionService = dipnetExpeditionService;
        this.fileManagers = fileManagers;
        this.datasetFileManager = datasetFileManager;
    }

    /**
     * service to validate a dataset against a project's rules
     *
     * @param projectId
     * @param fastqMetadata
     * @param isPublic
     * @param upload
     * @param expeditionCode
     * @param dataset
     * @param fasta
     * @param fastqFilenames
     * @return
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response Validate(@FormDataParam("projectId") Integer projectId,
                             @FormDataParam("fastqMetadata") FastqMetadata fastqMetadata,
                             @FormDataParam("public") @DefaultValue("false") boolean isPublic,
                             @FormDataParam("upload") @DefaultValue("false") boolean upload,
                             @FormDataParam("expeditionCode") String expeditionCode,
                             @FormDataParam("dataset") FormDataBodyPart dataset,
                             @FormDataParam("fasta") FormDataBodyPart fasta,
                             @FormDataParam("fastqFilenames") FormDataBodyPart fastqFilenames) {
        Map<String, Map<String, Object>> fmProps = new HashMap<>();
        JSONObject returnValue = new JSONObject();
        boolean closeProcess = true;
        boolean removeController = true;

        // create a new processController
        ProcessController processController = new ProcessController(projectId, expeditionCode);
        processController.setOutputFolder(uploadPath());

        // place the processController in the session here so that we can track the status of the validation process
        // by calling biocode.fims.rest/validate/status
        session.setAttribute("processController", processController);

        try {
            // update the status
            processController.appendStatus("Initializing...");

            if (dataset != null && dataset.getContentDisposition().getFileName() != null) {
                String datasetFilename = dataset.getContentDisposition().getFileName();
                processController.appendStatus("\nDataset filename = " + datasetFilename);

                InputStream is = dataset.getEntityAs(InputStream.class);
                String tempFilename = saveFile(is, datasetFilename, "xls");

                Map<String, Object> props = new HashMap<>();
                props.put("filename", tempFilename);

                fmProps.put("dataset", props);
            }
            if (fasta != null && fasta.getContentDisposition().getFileName() != null) {
                String fastaFilename = fasta.getContentDisposition().getFileName();
                processController.appendStatus("\nFASTA filename = " + fastaFilename);

                InputStream is = fasta.getEntityAs(InputStream.class);
                String tempFilename = saveFile(is, fasta.getContentDisposition().getFileName(), "fasta");

                Map<String, Object> props = new HashMap<>();
                props.put("filename", tempFilename);

                fmProps.put("fasta", props);
            }
            if (fastqMetadata != null || (fastqFilenames != null && fastqFilenames.getContentDisposition().getFileName() != null)) {
                Map<String, Object> props = new HashMap<>();
                if (fastqFilenames != null && fastqFilenames.getContentDisposition().getFileName() != null) {
                    InputStream is = fastqFilenames.getEntityAs(InputStream.class);
                    String tempFilename = saveFile(is, fastqFilenames.getContentDisposition().getFileName(), "fastq");
                    props.put("filename", tempFilename);
                }


                if (fastqMetadata != null) {
                    try {
                        // hack until we get FastqFileManger.upload working
                        ObjectMapper mapper = new ObjectMapper();
                        JSONObject metadata = (JSONObject) new JSONParser().parse(mapper.writeValueAsString(fastqMetadata));

                        processController.setFastqMetadata(metadata);
                        props.put("metadata", metadata);
                    } catch (JsonProcessingException | ParseException e) {
                        throw new ServerErrorException();
                    }
                }

                fmProps.put("fastq", props);
            }

            File configFile = new ConfigurationFileFetcher(projectId, uploadPath(), false).getOutputFile();

            // Create the process object --- this is done each time to orient the application
            Process process = new Process.ProcessBuilder(datasetFileManager, processController)
                    .addFileManagers(fileManagers)
                    .addFmProperties(fmProps)
                    .configFile(configFile)
                    .build();

            processController.setProcess(process);

            if (process.validate() && upload) {
                if (user == null) {
                    throw new UnauthorizedRequestException("You must be logged in to upload.");
                }

                processController.setUserId(user.getUserId());

                // set public status to true in processController if user wants it on
                if (isPublic) {
                    processController.setPublicStatus(true);
                }

                // if there were validation warnings and user would like to upload, we need to ask the user to continue
                if (processController.getHasWarnings()) {
                    returnValue.put("continue", processController.getMessages());

                    // there were no validation warnings and the user would like to upload, so continue
                } else {
                    JSONObject msg = new JSONObject();
                    msg.put("message", "continue");
                    returnValue.put("continue", msg);
                }

                // don't remove the controller or inputFiles as we will need it later for uploading this file
                closeProcess = false;
                removeController = false;

            } else {
                returnValue.put("done", processController.getMessages());
            }


            return Response.ok(returnValue.toJSONString()).build();

        } finally {
            if (closeProcess && processController.getProcess() != null) {
                processController.getProcess().close();
            }
            if (removeController) {
                session.removeAttribute("processController");
            }

        }

    }

    private String saveFile(InputStream is, String filename, String defaultExt) {
        String ext = FileUtils.getExtension(filename, defaultExt);
        String tempFilename = FileUtils.saveTempFile(is, ext);
        if (tempFilename == null) {
            throw new FimsRuntimeException("Server error saving file: " + filename, 500);
        }

        return tempFilename;
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

            Process p = processController.getProcess();

            if (createExpedition) {
                processController.setExpeditionTitle(processController.getExpeditionCode() + " spreadsheet");
            }

            try {
                p.upload(createExpedition, Boolean.parseBoolean(settingsManager.retrieveValue("ignoreUser")), expeditionService);

                // hack until we get FastqFileManger.upload working
                if (processController.getFastqMetadata() != null) {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        FastqMetadata fastqMetadata = mapper.readValue(processController.getFastqMetadata().toJSONString(), FastqMetadata.class);

                        Expedition expedition = expeditionService.getExpedition(processController.getExpeditionCode(), processController.getProjectId());
                        DipnetExpedition dipnetExpedition = dipnetExpeditionService.getDipnetExpedition(expedition.getExpeditionId());

                        if (dipnetExpedition == null) {
                            dipnetExpedition = new DipnetExpedition.DipnetExpeditionBuilder(expedition)
                                    .fastqMetadata(fastqMetadata)
                                    .build();

                            dipnetExpeditionService.create(dipnetExpedition);
                        } else {
                            dipnetExpedition.setFastqMetadata(fastqMetadata);
                            dipnetExpeditionService.update(dipnetExpedition);
                        }
                    } catch (IOException e) {
                        throw new FimsRuntimeException("Failed to convert FastqMetadata object", 500, e);
                    }
                }

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
                processController.getProcess().close();
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
}



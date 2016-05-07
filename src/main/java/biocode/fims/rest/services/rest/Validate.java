package biocode.fims.rest.services.rest;

import biocode.fims.bcid.BcidMinter;
import biocode.fims.bcid.ResourceTypes;
import biocode.fims.config.ConfigurationFileTester;
import biocode.fims.entities.Bcid;
import biocode.fims.entities.Expedition;
import biocode.fims.entities.Project;
import biocode.fims.fasta.FastaManager;
import biocode.fims.fimsExceptions.*;
import biocode.fims.fuseki.Uploader;
import biocode.fims.fuseki.fasta.FusekiFastaManager;
import biocode.fims.fuseki.triplify.Triplifier;
import biocode.fims.rest.FimsService;
import biocode.fims.run.Process;
import biocode.fims.run.ProcessController;
import biocode.fims.service.BcidService;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import biocode.fims.service.UserService;
import biocode.fims.settings.SettingsManager;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaContext;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.ws.rs.*;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.InputStream;
import java.net.URI;

/**
 */
@Path("validate")
public class Validate extends FimsService {

    private final BcidService bcidService;
    private final ExpeditionService expeditionService;
    private final ProjectService projectService;

    private EntityManagerFactory managerFactory;

    @Autowired
    Validate(BcidService bcidService, ExpeditionService expeditionService, ProjectService projectService,
             EntityManagerFactory managerFactory, UserService userService, SettingsManager settingsManager) {
        super(userService, settingsManager);
        this.bcidService = bcidService;
        this.expeditionService = expeditionService;
        this.projectService = projectService;
        this.managerFactory = managerFactory;
    }

    /**
     * service to validate a dataset against a project's rules
     *
     * @param projectId
     * @param expeditionCode
     * @param upload
     * @param datasetIs
     * @param datasetFileData
     *
     * @return
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public String validate(@FormDataParam("projectId") Integer projectId,
                           @FormDataParam("expeditionCode") String expeditionCode,
                           @FormDataParam("upload") String upload,
                           @FormDataParam("public_status") String publicStatus,
                           @FormDataParam("fasta") InputStream fastaIs,
                           @FormDataParam("fasta") FormDataContentDisposition fastaFileData,
                           @FormDataParam("dataset") InputStream datasetIs,
                           @FormDataParam("dataset") FormDataContentDisposition datasetFileData) {
        StringBuilder retVal = new StringBuilder();
        Boolean removeController = true;
        Boolean deleteInputFile = true;
        String inputFile = null;
        String fastaFile = null;
        FastaManager fastaManager = null;

        Project project = projectService.getProject(projectId);
        // create a new processController
        ProcessController processController = new ProcessController(project, expeditionCode);

        // place the processController in the session here so that we can track the status of the validation process
        // by calling biocode.fims.rest/validate/status
        session.setAttribute("processController", processController);


        // update the status
        processController.appendStatus("Initializing...<br>");
        // save the dataset and/or fasta files
        if (datasetFileData.getFileName() != null) {
            processController.appendStatus("inputFilename = " + processController.stringToHTMLJSON(
                    datasetFileData.getFileName()) + "<br>");

            // Save the uploaded dataset file
            String splitArray[] = datasetFileData.getFileName().split("\\.");
            String ext;
            if (splitArray.length == 0) {
                // if no extension is found, then guess
                ext = "xls";
            } else {
                ext = splitArray[splitArray.length - 1];
            }
            inputFile = processController.saveTempFile(datasetIs, ext);
            // if inputFile null, then there was an error saving the file
            if (inputFile == null) {
                throw new FimsRuntimeException("Server error saving dataset file.", 500);
            }
            processController.setInputFilename(inputFile);
        }
        // Create the process object --- this is done each time to orient the application
        Process p = new Process(
                uploadPath(),
                processController,
                expeditionService
        );
        if (fastaFileData != null) {
            if (fastaFileData.getFileName() != null) {
                processController.appendStatus("fastaFilename = " + processController.stringToHTMLJSON(
                        fastaFileData.getFileName()) + "<br>");

                // Save the uploaded fasta file
                String splitArray[] = fastaFileData.getFileName().split("\\.");
                String fastaExt;
                if (splitArray.length == 0) {
                    // if no extension is found, then guess
                    fastaExt = "fasta";
                } else {
                    fastaExt = splitArray[splitArray.length - 1];
                }
                fastaFile = processController.saveTempFile(fastaIs, fastaExt);
                // if inputFile null, then there was an error saving the file
                if (fastaFile == null) {
                    throw new FimsRuntimeException("Server error saving fasta file.", 500);
                }

                fastaManager = new FusekiFastaManager(
                        processController.getMapping().getMetadata().getQueryTarget(), processController, fastaFile);
                processController.setFastaManager(fastaManager);
            }
        }


        // Test the configuration file to see that we're good to go...
        ConfigurationFileTester cFT = new ConfigurationFileTester();
        boolean configurationGood = true;

        cFT.init(p.configFile);

        if (!cFT.checkUniqueKeys()) {
            String message = "<br>CONFIGURATION FILE ERROR...<br>Please talk to your project administrator to fix the following error:<br>\t\n";
            message += cFT.getMessages();
            processController.setHasErrors(true);
            processController.setValidated(false);
            processController.appendStatus(message + "<br>");
            configurationGood = false;
            retVal.append("{\"done\": \"");
            retVal.append(processController.getStatusSB().toString());
            retVal.append("\"}");
        }


        // Run the process only if the configuration is good.
        if (configurationGood) {
            processController.appendStatus("Validating...<br>");

            if (processController.getInputFilename() != null) {
                p.runValidation();
            }
            if (fastaManager != null) {
                fastaManager.validate(uploadPath());
            }

            // if there were validation errors, we can't upload
            if (processController.getHasErrors()) {
                retVal.append("{\"done\": ");
                retVal.append(processController.getMessages().toJSONString());
                retVal.append("}");

            } else if (upload != null && upload.equals("on")) {

                if (username == null) {
                    throw new UnauthorizedRequestException("You must be logged in to upload.");
                }

                processController.setUser(user);

                // set public status to true in processController if user wants it on
                if (publicStatus != null && publicStatus.equals("on")) {
                       processController.setPublicStatus(true);
                }

                // if there were validation warnings and user would like to upload, we need to ask the user to continue
                if (processController.getHasWarnings()) {
                    retVal.append("{\"continue\": ");
                    retVal.append(processController.getMessages().toJSONString());
                    retVal.append("}");

                    // there were no validation warnings and the user would like to upload, so continue
                } else {
                    retVal.append("{\"continue\": {\"message\": \"continue\"}}");
                }

                // don't delete the inputFile because we'll need it for uploading
                deleteInputFile = false;

                // don't remove the controller as we will need it later for uploading this file
                removeController = false;
            } else {
                // User doesn't want to upload. Return the validation results
                retVal.append("{\"done\": ");
                retVal.append(processController.getMessages().toJSONString());
                retVal.append("}");
            }
        }

        if (deleteInputFile) {
            if (inputFile != null) {
                new File(inputFile).delete();
            }
            if (fastaFile != null) {
                new File(fastaFile).delete();
            }
        }
        if (removeController) {
            session.removeAttribute("processController");
        }

        return retVal.toString();
    }

    /**
     * Service to upload a dataset to an expedition. The validate service must be called before this service.
     *
     * @param createExpedition
     *
     * @return
     */
    @GET
    @Path("/continue")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public String upload(@QueryParam("createExpedition") @DefaultValue("false") Boolean createExpedition) {
        FastaManager fastaManager;
        String previousGraph;
        String currentGraph;
        String successMessage = "";
        ProcessController processController = (ProcessController) session.getAttribute("processController");

        // we need to reattach the entities to the jpa session to avoid lazy-initialization exceptions
        EntityManager manager = managerFactory.createEntityManager();
        Session hibernateSession = manager.unwrap(Session.class);
        try {
            hibernateSession.update(processController.getProject());

            // if no processController is found, we can't do anything
            if (processController == null) {
                return "{\"error\": \"No process was detected.\"}";
            }

            // check if user is logged in
            if (processController.getUser() == null) {
                return "{\"error\": \"You must be logged in to upload.\"}";
            }

            // if the process controller was stored in the session, then the user wants to continue, set warning cleared
            processController.setClearedOfWarnings(true);
            processController.setValidated(true);

            fastaManager = processController.getFastaManager();
            // if fastaManager is null, then we are not uploading a dataset. In that case, we need to copy over the sequences
            // from the old graph to the new graph
            if (fastaManager == null) {
                fastaManager = new FusekiFastaManager(processController.getMapping().getMetadata().getQueryTarget(),
                        processController, null);
            }

            // Create the process object --- this is done each time to orient the application
            Process p = new Process(
                    uploadPath(),
                    processController,
                    expeditionService
            );

            String outputPrefix = processController.getExpeditionCode() + "_output";
            // if there is an inputFilename, then there is a dataset to upload
            if (processController.getInputFilename() != null) {

                // create this expedition if the user wants to
                if (createExpedition) {
                    processController.setExpeditionTitle(processController.getExpeditionCode() + " spreadsheet");
                    p.runExpeditionCreate(bcidService);
                }

                if (!processController.isExpeditionAssignedToUserAndExists()) {
                    p.runExpeditionCheck();

                    if (processController.isExpeditionCreateRequired()) {
                        // ask the user if they want to create this expedition
                        return "{\"continue\": {\"message\": \"The expedition code \\\"" + JSONObject.escape(processController.getExpeditionCode()) +
                                "\\\" does not exist.  " +
                                "Do you wish to create it now?<br><br>" +
                                "If you choose to continue, your data will be associated with this new expedition code.\"}}";
                    }

                    if (!processController.isExpeditionAssignedToUserAndExists())
                        throw new biocode.fims.fimsExceptions.BadRequestException("You do not own the expedition: " + processController.getExpeditionCode());
                }

                // fetch the current graph before uploading the new graph. This is needed to copy over the fasta sequences
                previousGraph = fastaManager.fetchGraph();

                // run the triplifier
                Triplifier triplifier = new Triplifier(outputPrefix, uploadPath(), processController);

                boolean runDeepRoots = Boolean.valueOf(settingsManager.retrieveValue("deepRoots"));

                triplifier.run(processController.getValidation().getSqliteFile(), runDeepRoots);

                // upload the dataset
                Uploader uploader = new Uploader(processController.getMapping().getMetadata().getTarget(),
                        new File(triplifier.getTripleOutputFile()));

                uploader.execute();
                currentGraph = uploader.getGraphID();

                // Get the expedition this dataset is associated with
                Expedition expedition = expeditionService.getExpedition(
                        processController.getExpeditionCode(),
                        processController.getProject().getProjectId()
                );

                Bcid bcid = new Bcid.BcidBuilder(user, ResourceTypes.DATASET_RESOURCE_TYPE)
                        .ezidRequest(Boolean.parseBoolean(settingsManager.retrieveValue("ezidRequests")))
                        .title(processController.getExpeditionCode() + " Dataset")
                        .webAddress(URI.create(uploader.getEndpoint()))
                        .graph(currentGraph)
                        .finalCopy(processController.getFinalCopy())
                        .expedition(expedition)
                        .build();

                bcidService.create(bcid);

                successMessage = "Dataset Identifier: http://n2t.net/" + bcid.getIdentifier() + " (wait 15 minutes for resolution to become active)";
                successMessage += "<br>\t" + "Data Elements Root: " + processController.getExpeditionCode();

                // copy over the fasta sequences if this is not the first dataset uploaded, but only if there is no
                // new fasta file to upload
                if (previousGraph != null && fastaManager.getFastaFilename() == null) {
                    fastaManager.copySequences(previousGraph, currentGraph);
                }

                // delete the temporary file now that it has been uploaded
                new File(processController.getInputFilename()).delete();
            } else {
                successMessage += "<br>\t" + "FASTA data added to dataset belonging to Expedition Code: " + processController.getExpeditionCode();
                currentGraph = fastaManager.fetchGraph();
            }

            // if fastaFilename isn't null, then we have a fasta file to upload
            if (fastaManager.getFastaFilename() != null) {
                if (!processController.isExpeditionAssignedToUserAndExists()) {
                    p.runExpeditionCheck();
                    if (processController.isExpeditionCreateRequired()) {
                        throw new BadRequestException("You can only upload fasta files to existing expeditions unless you" +
                                " are simultaneously uploading a new dataset.");
                    }
                    if (!processController.isExpeditionAssignedToUserAndExists())
                        throw new biocode.fims.fimsExceptions.BadRequestException("You do not own the expedition: " + processController.getExpeditionCode());

                }

                if (currentGraph == null) {
                    throw new BadRequestException("No existing dataset was detected. Your fasta file must be " + "" +
                            "associated with an existing dataset.");
                }
                fastaManager.upload(currentGraph, uploadPath(), outputPrefix);
                new BcidMinter().updateBcidTimestamp(currentGraph);

                // delete the temporary file now that it has been uploaded
                new File(fastaManager.getFastaFilename()).delete();
                successMessage += "<br>\t" + "FASTA data added";
            }

            successMessage += "<br><font color=#188B00>Successfully Uploaded!</font><br><br>";
            processController.appendStatus(successMessage);

            // remove the processController from the session
            session.removeAttribute("processController");

            return "{\"done\": {\"message\": \"" + JSONObject.escape(successMessage) + "\"}}";
        } finally {
            hibernateSession.close();
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



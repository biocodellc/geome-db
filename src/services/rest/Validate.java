package services.rest;

import biocode.fims.bcid.Bcid;
import biocode.fims.bcid.BcidMinter;
import biocode.fims.bcid.ExpeditionMinter;
import biocode.fims.config.ConfigurationFileTester;
import biocode.fims.fasta.FastaManager;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.UnauthorizedRequestException;
import biocode.fims.fuseki.Uploader;
import biocode.fims.fuseki.fasta.FusekiFastaManager;
import biocode.fims.fuseki.triplify.Triplifier;
import biocode.fims.rest.FimsService;
import biocode.fims.run.Process;
import biocode.fims.run.ProcessController;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.simple.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.InputStream;

/**
 */
@Path("validate")
public class Validate extends FimsService {

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

        // create a new processController
        ProcessController processController = new ProcessController(projectId, expeditionCode);

        // place the processController in the session here so that we can track the status of the validation process
        // by calling rest/validate/status
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
                processController
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

                processController.setUserId(userId);

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
        String successMessage = null;
        ProcessController processController = (ProcessController) session.getAttribute("processController");

        // if no processController is found, we can't do anything
        if (processController == null) {
            return "{\"error\": \"No process was detected.\"}";
        }

        // check if user is logged in
        if (processController.getUserId() == null) {
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
                processController
        );

        String outputPrefix = processController.getExpeditionCode() + "_output";
        // if there is an inputFilename, then there is a dataset to upload
        if (processController.getInputFilename() != null) {

            // create this expedition if the user wants to
            if (createExpedition) {
                processController.setExpeditionTitle(processController.getExpeditionCode() + " spreadsheet");
                p.runExpeditionCreate();
            }

            if (!processController.isExpeditionAssignedToUserAndExists()) {
                p.runExpeditionCheck();
            }

            if (processController.isExpeditionCreateRequired()) {
                // ask the user if they want to create this expedition
                return "{\"continue\": {\"message\": \"The expedition code \\\"" + JSONObject.escape(processController.getExpeditionCode()) +
                        "\\\" does not exist.  " +
                        "Do you wish to create it now?<br><br>" +
                        "If you choose to continue, your data will be associated with this new expedition code.\"}}";
            }

            // fetch the current graph before uploading the new graph. This is needed to copy over the fasta sequences
            previousGraph = fastaManager.fetchGraph();

            // run the triplifier
            Triplifier triplifier = new Triplifier(outputPrefix, uploadPath(), processController);

            triplifier.run(processController.getValidation().getSqliteFile());

            // upload the dataset
            Uploader uploader = new Uploader(processController.getMapping().getMetadata().getTarget(),
                    new File(triplifier.getTripleOutputFile()));

            uploader.execute();
            currentGraph = uploader.getGraphID();

            // Detect if this is user=demo or not.  If this is "demo" then do not request EZIDs.
            // User account Demo can still create Data Groups, but they just don't get registered and will be purged periodically
            boolean ezidRequest = true;
            if (username.equals("demo") || sm.retrieveValue("ezidRequests").equalsIgnoreCase("false")) {
                ezidRequest = false;
            }

            // Mint the data group
            BcidMinter bcidMinter = new BcidMinter(ezidRequest);
            String identifier = bcidMinter.createEntityBcid(new Bcid(
                    processController.getUserId(),
                    "http://purl.org/dc/dcmitype/Dataset",
                    processController.getExpeditionCode() + " Dataset",
                    uploader.getEndpoint(),
                    currentGraph,
                    null,
                    processController.getFinalCopy(),
                    false));
            bcidMinter.close();
            successMessage = "Dataset Identifier: http://n2t.net/" + identifier + " (wait 15 minutes for resolution to become active)";

            // Associate the expeditionCode with this identifier
            ExpeditionMinter expedition = new ExpeditionMinter();
            expedition.attachReferenceToExpedition(processController.getExpeditionCode(), identifier, processController.getProjectId());
            expedition.close();
            successMessage += "<br>\t" + "Data Elements Root: " + processController.getExpeditionCode();

            // copy over the fasta sequences if this is not the first dataset uploaded, but only if there is no
            // new fasta file to upload
            if (previousGraph != null && fastaManager.getFastaFilename() == null) {
                fastaManager.copySequences(previousGraph, currentGraph);
            }

            // delete the temporary file now that it has been uploaded
            new File(processController.getInputFilename()).delete();
        } else {
            currentGraph = fastaManager.fetchGraph();
        }

        // if fataFilename isn't null, then we have a fasta file to upload
        if (fastaManager.getFastaFilename() != null) {
            if (!processController.isExpeditionAssignedToUserAndExists()) {
                p.runExpeditionCheck();
                if (processController.isExpeditionCreateRequired()) {
                    throw new BadRequestException("You can only upload fasta files to existing expeditions unless you" +
                            " are simultaneously uploading a new dataset.");
                }
            }

            if (currentGraph == null) {
                throw new BadRequestException("No existing dataset was detected. Your fasta file must be " + "" +
                        "associated with an existing dataset.");
            }
            fastaManager.upload(currentGraph, uploadPath(), outputPrefix);

            // delete the temporary file now that it has been uploaded
            new File(fastaManager.getFastaFilename()).delete();
        }

        successMessage += "<br><font color=#188B00>Successfully Uploaded!</font><br><br>";
        processController.appendStatus(successMessage);

        // remove the processController from the session
        session.removeAttribute("processController");

        return "{\"done\": {\"message\": \"" + JSONObject.escape(successMessage) + "\"}}";
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



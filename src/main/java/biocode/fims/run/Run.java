package biocode.fims.run;

import biocode.fims.auth.Authenticator;
import biocode.fims.bcid.Bcid;
import biocode.fims.bcid.BcidDatabase;
import biocode.fims.bcid.BcidMinter;
import biocode.fims.bcid.ExpeditionMinter;
import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.fasta.FastaManager;
import biocode.fims.fuseki.Uploader;
import biocode.fims.fuseki.fasta.FusekiFastaManager;
import biocode.fims.fuseki.query.FimsQueryBuilder;
import biocode.fims.fuseki.triplify.Triplifier;
import biocode.fims.settings.FimsPrinter;
import biocode.fims.settings.SettingsManager;
import biocode.fims.settings.StandardPrinter;
import org.apache.commons.cli.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.ws.rs.BadRequestException;
import java.io.File;

/**
 * Class to upload and triplify via cmd line
 * TODO refactor to remove duplicate code.
 */
public class Run {

    private SettingsManager settingsManager = SettingsManager.getInstance();

    private Process process;
    private ProcessController processController;
    private String username;

    public Run(Process process, ProcessController processController, String username) {
        this.process = process;
        this.processController = processController;
        this.username = username;
    }

    /**
     * runAll method is designed to go through the FIMS process for a local application.  The REST services
     * would handle user input/output differently
     *
     * @param triplifier
     * @param upload
     * @param expeditionCheck -- only set to FALSE for testing and debugging usually, or local triplify usage.
     */
    public void runAllLocally(Boolean triplifier, Boolean upload, Boolean expeditionCheck, Boolean forceAll) {
        String currentGraph;
        boolean isFastaUpload = false;
        String fastaFileName = null;

        // determine if this is a fasta file or not
        if (processController.getInputFilename().endsWith(".fasta")) {
            isFastaUpload = true;
            fastaFileName = processController.getInputFilename();
            // need to set processController.getInputFilename to null
            processController.setInputFilename(null);
        }

        if (isFastaUpload && triplifier && !upload) {
            FimsPrinter.out.println("We don't currently support only triplifying of Fasta files");
            return;
        }

        FastaManager fastaManager = new FusekiFastaManager(processController.getMapping().getMetadata().getQueryTarget(),
                    processController, fastaFileName);

        // Validation Step
        if (isFastaUpload)
            fastaManager.validate(process.outputFolder);
        else
            process.runValidation();

        // If there is errors, tell the user and stop the operation
        if (processController.getHasErrors()) {
            FimsPrinter.out.println(processController.printMessages());
            return;
        }

        if (processController.getHasWarnings()) {
            Boolean continueOperation;
            if (forceAll) {
                continueOperation = true;
            } else {
//                String message = "\tWarnings found on " + processController.getMapping().getDefaultSheetName() + " worksheet.\n" + processController.getCommandLineSB().toString();
                // In LOCAL version convert HTML tags to readable Text
                // the FimsInputter isn't working correctly, just using the StandardInputter for now
                //Boolean continueOperation = FimsInputter.in.continueOperation(message);
                continueOperation = new StandardInputter().continueOperation(processController.printMessages());
            }
            if (!continueOperation) {
                return;
            }
            processController.setClearedOfWarnings(true);
        }

        //

        // We only need to check on assigning Expedition if the user wants to triplify or upload data
        if (triplifier || upload) {

            if (expeditionCheck) {
                // Expedition Check Step
                if (!processController.isExpeditionAssignedToUserAndExists())
                    process.runExpeditionCheck();
                // if an expedition creation is required, get feedback from user
                if (processController.isExpeditionCreateRequired()) {
                    if (isFastaUpload) {
                        throw new BadRequestException("You can only upload fasta files to existing expeditions unless you" +
                                " are simultaneously uploading a new dataset.");
                    }

                    if (forceAll) {
                        process.runExpeditionCreate();
                    } else {
                        String message = "\nThe dataset code \"" + processController.getExpeditionCode() + "\" does not exist.  " +
                                "Do you wish to create it now?" +
                                "\nIf you choose to continue, your data will be associated with this new dataset code.";
                        Boolean continueOperation = FimsInputter.in.continueOperation(message);
                        if (!continueOperation)
                            return;
                        else
                            process.runExpeditionCreate();
                    }

                }

                // Triplify OR Upload -- not ever both
                if (triplifier) {
                    runTriplifier();
                } else if (upload) {
                    // upload the dataset
                    if (!isFastaUpload) {
                        String tripleFile = runTriplifier();

                        // fetch the current graph before uploading the new graph. This is needed to copy over the fasta sequences
                        String previousGraph = fastaManager.fetchGraph();
                        Uploader uploader = new Uploader(processController.getMapping().getMetadata().getTarget(),
                                new File(tripleFile));

                        uploader.execute();
                        currentGraph = uploader.getGraphID();

                        // Detect if this is user=demo or not.  If this is "demo" then do not request EZIDs.
                        // User account Demo can still create Data Groups, but they just don't get registered and will be purged periodically
                        boolean ezidRequest = true;
                        if (username.equals("demo") || settingsManager.retrieveValue("ezidRequests").equalsIgnoreCase("false")) {
                            ezidRequest = false;
                        }

                        // Mint the data group
                        BcidMinter bcidMinter = new BcidMinter(ezidRequest);
                        String identifier = bcidMinter.createEntityBcid(new Bcid(
                                BcidDatabase.getUserId(username),
                                "http://purl.org/dc/dcmitype/Dataset",
                                processController.getExpeditionCode() + " Dataset",
                                uploader.getEndpoint(),
                                uploader.getGraphID(),
                                null,
                                processController.getFinalCopy(),
                                false));
                        FimsPrinter.out.println("Dataset Identifier: http://n2t.net/" + identifier + " (wait 15 minutes for resolution to become active)");

                        // Associate the expeditionCode with this identifier
                        ExpeditionMinter expedition = new ExpeditionMinter();
                        expedition.attachReferenceToExpedition(processController.getExpeditionCode(), identifier, processController.getProjectId());
                        FimsPrinter.out.println("\t" + "Data Elements Root: " + processController.getExpeditionCode());

                        // copy over the fasta sequences if this is not the first dataset uploaded, but only if there is no
                        // new fasta file to upload
                        if (previousGraph != null && fastaManager.getFastaFilename() == null) {
                            fastaManager.copySequences(previousGraph, currentGraph);
                        }
                    } else {
                        currentGraph = fastaManager.fetchGraph();

                        if (currentGraph == null) {
                            throw new BadRequestException("No existing dataset was detected. Your fasta file must be " + "" +
                                    "associated with an existing dataset.");
                        }
                        fastaManager.upload(
                                currentGraph,
                                System.getProperty("user.dir") + File.separator + "tripleOutput",
                                processController.getExpeditionCode() + "_output");
                        new BcidMinter().updateBcidTimestamp(currentGraph);

                        FimsPrinter.out.println("<br>\t" + "FASTA data added");
                    }

                }
                // If we don't Run the expedition check then we DO NOT assign any ARK roots or special expedition information
                // In other, words, this is typically used for local debug & test modes
            } else {
                process.outputPrefix = "test";
                runTriplifier();
            }
        }
    }

    private String runTriplifier() {
        FimsPrinter.out.println("\nTriplifying...");

        // Run the triplifier
        Triplifier t = new Triplifier(process.outputPrefix, process.outputFolder, processController);

        boolean runDeepRoots = Boolean.valueOf(settingsManager.retrieveValue("deepRoots"));
        t.run(processController.getValidation().getSqliteFile(), runDeepRoots);
        FimsPrinter.out.println("\ttriple output file = " + t.getTripleOutputFile());
        return t.getTripleOutputFile();
    }

    /**
     * Run the program from the command-line
     *
     * @param args
     */
    public static void main(String args[])  throws Exception{
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("/applicationContext.xml");
        String defaultOutputDirectory = System.getProperty("user.dir") + File.separator + "tripleOutput";
        String username = "";
        String password = "";
        Integer project_id = 0;
        //System.out.print(defaultOutputDirectory);

        // Test configuration :
        // -d -t -u -i sampledata/Apogon***.xls

        // Direct output using the standardPrinter subClass of fimsPrinter which send to fimsPrinter.out (for command-line usage)
        FimsPrinter.out = new StandardPrinter();


        // Some classes to help us
        CommandLineParser clp = new GnuParser();
        HelpFormatter helpf = new HelpFormatter();
        CommandLine cl;

        // The expedition code corresponds to a expedition recognized by BCID
        String dataset_code = "";
        // The configuration template
        //String configuration = "";
        // The input file
        String input_file = "";
        // The directory that we write all our files to
        String output_directory = "tripleOutput";
        // Write spreadsheet content back to a spreadsheet file, for testing
        Boolean triplify = false;
        Boolean upload = false;
        Boolean local = false;
        Boolean force = false;


        // Define our commandline options
        Options options = new Options();
        options.addOption("h", "help", false, "print this help message and exit");
        options.addOption("q", "query", true, "Run a query and pass in graph UUIDs to look at for this query -- Use this along with options C and S");
        options.addOption("f", "format", true, "excel|html|json|cspace  specifying the return format for the query");
        options.addOption("F", "filter", true, "Filter results based on a keyword search");

        options.addOption("e", "dataset_code", true, "Dataset code.  You will need to obtain a data code before " +
                "loading data");
        options.addOption("o", "output_directory", true, "Output Directory");
        options.addOption("i", "input_file", true, "Input Spreadsheet or FASTA file");
        options.addOption("p", "project_id", true, "Project Identifier.  A numeric integer corresponding to your project");
        options.addOption("configFile", true, "Use a local config file instead of getting from server");

        options.addOption("t", "triplify", false, "Triplify only (upload process triplifies)");
        options.addOption("l", "local", false, "Local option operates purely locally and does not create proper globally unique identifiers.  Running the local option means you don't need a username and password.");

        options.addOption("u", "upload", false, "Upload");

        options.addOption("U", "username", true, "Username (for uploading data)");
        options.addOption("P", "password", true, "Password (for uploading data)");
        options.addOption("y", "yes", false, "Answer 'y' to all questions");


        // Create the commands parser and parse the command line arguments.
        try {
            cl = clp.parse(options, args);
        } catch (UnrecognizedOptionException e) {
            FimsPrinter.out.println("Error: " + e.getMessage());
            return;
        } catch (ParseException e) {
            FimsPrinter.out.println("Error: " + e.getMessage());
            return;
        }

//         Set the input format
        if (cl.hasOption("y")) {
            FimsInputter.in = new ForceInputter();
            force = true;
        } else {
            FimsInputter.in = new StandardInputter();
        }

        // Set username
        if (cl.hasOption("U")) {
            username = cl.getOptionValue("U");
        }

        // Set password
        if (cl.hasOption("P")) {
            password = cl.getOptionValue("P");
        }

        // Check username and password
        if (cl.hasOption("u") && (username.equals("") || password.equals(""))) {
            FimsPrinter.out.println("Must specify a valid username or password for uploading data!");
            return;
        }

        // Query option must also have project_id option
        if (cl.hasOption("q")) {
            if (!cl.hasOption("p")) {
                helpf.printHelp("fims ", options, true);
                return;
            }

        }
        // Help
        else if (cl.hasOption("h")) {
            helpf.printHelp("fims ", options, true);
            return;
        }

        // No options returns help message
        if (cl.getOptions().length < 1) {
            helpf.printHelp("fims ", options, true);
            return;
        }

        // Sanitize project specification
        if (cl.hasOption("p")) {
            try {
                project_id = new Integer(cl.getOptionValue("p"));
            } catch (Exception e) {
                FimsPrinter.out.println("Bad option for project_id");
                helpf.printHelp("fims ", options, true);
                return;
            }
        }

        // Check for project_id when uploading data
        if (cl.hasOption("u") && project_id < 1) {
            FimsPrinter.out.println("Must specify a valid project_id when uploading data");
            return;
        }

        // Set input file
        if (cl.hasOption("i"))
            input_file = cl.getOptionValue("i");

        // Set output directory
        if (cl.hasOption("o"))
            output_directory = cl.getOptionValue("o");

        // Set dataset_code
        if (cl.hasOption("e"))
            dataset_code = cl.getOptionValue("e");

        // Set triplify option
        if (cl.hasOption("t"))
            triplify = true;

        // Set the "local" option
        if (cl.hasOption("l"))
            local = true;

        // Set upload option
        if (cl.hasOption("u"))
            upload = true;

        // Set default output directory if one is not specified
        if (!cl.hasOption("o")) {
            FimsPrinter.out.println("Using default output directory " + defaultOutputDirectory);
            output_directory = defaultOutputDirectory;
        }

        // Check that output directory is writable
        try {
            if (!new File(output_directory).canWrite()) {
                FimsPrinter.out.println("Unable to write to output directory " + output_directory);
                return;
            }
        } catch (Exception e) {
            FimsPrinter.out.println("Unable to write to output directory " + output_directory);
            return;
        }

        // Run the command
        try {
            /*
            Run a query
             */
            if (cl.hasOption("q")) {

                File configFile = new ConfigurationFileFetcher(project_id, output_directory, true).getOutputFile();

                Process p = new Process(
                        project_id,
                        configFile
                );

                //p.query(cl.getOptionValue("q"), cl.getOptionValue("f"), cl.getOptionValue("F"));
                // TODO: construct filter statements from arguments passed in on command-line
                // Build the Query Object by passing this object and an array of graph objects, separated by commas
                FimsQueryBuilder q = new FimsQueryBuilder(p, cl.getOptionValue("q").split(","), output_directory);
                // Run the query, passing in a format and returning the location of the output file
                System.out.println(q.run(cl.getOptionValue("f")));
            }
            /*
           Run the validator
            */
            else {
                ProcessController processController = new ProcessController(project_id, dataset_code);
                processController.setInputFilename(input_file);
                // if we only want to triplify and not upload, then we operate in LOCAL mode
                if (local && triplify) {
                    processController.appendStatus("Triplifying using LOCAL only options, useful for debugging\n");
                    processController.appendStatus("Does not construct GUIDs, use Deep Roots, or connect to project-specific configurationFiles");
                    processController.setInputFilename(input_file);

                    Process p = new Process(output_directory, processController, new File(cl.getOptionValue("configFile")));
                    Run run = new Run(p, processController, username);
                    run.runAllLocally(true, false, false, force);
                    /*p.runValidation();
                    triplifier t = new triplifier("test", output_directory);
                    p.mapping.Run(t, pc);
                    p.mapping.print();  */

                } else {
                    if (triplify || upload) {
                        // log the user in
                        if (username == null || username.equals("") || password == null || password.equals("")) {
                            FimsPrinter.out.println("Need valid username / password for uploading");
                            helpf.printHelp("fims ", options, true);
                            return;
                        } else {
                            Authenticator authenticator = new Authenticator();
                            if (!authenticator.login(username, password)) {
                                FimsPrinter.out.println("Invalid username and/or password");
                                return;
                            }
                            // Check that a dataset code has been entered
                            if (!cl.hasOption("e")) {
                                FimsPrinter.out.println("Need to enter a dataset code before  uploading");
                                helpf.printHelp("fims ", options, true);
                                return;
                            }
                        }
                    }


                    // Now Run the process
                    Process p;
                    // use local configFile if specified
                    if (cl.hasOption("configFile")) {
                        System.out.println("using local config file = " + cl.getOptionValue("configFile").toString());
                        p = new Process(
                                output_directory,
                                processController,
                                new File(cl.getOptionValue("configFile")));
                    } else {
                        p = new Process(
                                output_directory,
                                processController
                        );
                    }

                    FimsPrinter.out.println("Initializing ...");
                    FimsPrinter.out.println("\tinputFilename = " + input_file);

                    // Run the processor
                    Run run = new Run(p, processController, username);
                    run.runAllLocally(triplify, upload, true, force);
                }
            }
        } catch (Exception e) {
            FimsPrinter.out.println("\nError: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }
}

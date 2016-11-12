package biocode.fims.genbank;

import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.Mapping;
import biocode.fims.entities.Bcid;
import biocode.fims.service.BcidService;
import biocode.fims.settings.FimsPrinter;
import biocode.fims.settings.SettingsManager;
import biocode.fims.settings.StandardPrinter;
import org.apache.commons.cli.*;
import org.apache.commons.digester3.Digester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.util.Set;

/**
 * Class to handle all aspects of sequence submission to biocode.fims.genbank
 * see <a href="http://www.ncbi.nlm.nih.gov/genbank/">http://www.ncbi.nlm.nih.gov/genbank/</a>
 */
public class GenbankManager {

    private final SettingsManager settingsManager;
    private final BcidService bcidService;

    @Autowired
    public GenbankManager(SettingsManager settingsManager, BcidService bcidService) {
        this.settingsManager = settingsManager;
        this.bcidService = bcidService;
    }

    public void run(int projectId, String outputDirectory, String templateFile, boolean generateSequin) {
        String divider = settingsManager.retrieveValue("divider");
        Set<Bcid> latestDatasets = bcidService.getLatestDatasets(projectId);

        File configFile = new ConfigurationFileFetcher(projectId, outputDirectory, true).getOutputFile();

        Mapping mapping = new Mapping();
        mapping.addMappingRules(configFile);

        String fusekiService = mapping.getMetadata().getQueryTarget();

        FimsPrinter.out.println("Generating fasta file ...");
        String fastaFile = FastaGenerator.generate(latestDatasets, fusekiService, mapping, outputDirectory, divider);
        FimsPrinter.out.println("\tFasta file generated: " + fastaFile);

        if (generateSequin) {
            FimsPrinter.out.println("Generating sequin file ...");
            SequinGenerator.generate(fastaFile, templateFile, outputDirectory);
            FimsPrinter.out.println("Sequin and validation files generated. Please check all files with extension .val " +
                    "for any errors during the sequin file generation process. All errors should be fixed before " +
                    "submission to genbank. If no errors exist, then you can submit the files with .sqn extension to " +
                    "genbank via http://www.ncbi.nlm.nih.gov/LargeDirSubs/dir_submit.cgi.");

        }
    }

    /**
     * Run the program from the command-line
     *
     * @param args
     */
    public static void main(String args[])  throws Exception {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("/applicationContext.xml");
        GenbankManager manager = applicationContext.getBean(GenbankManager.class);

        String defaultOutputDirectory = System.getProperty("user.dir") + File.separator + "tripleOutput";
        int projectId;
        String outputDirectory;
        String templateFile = null;
        boolean generateSequin = false;

        // Direct output using the standardPrinter subClass of fimsPrinter which send to fimsPrinter.out (for command-line usage)
        FimsPrinter.out = new StandardPrinter();


        // Some classes to help us
        CommandLineParser clp = new GnuParser();
        HelpFormatter helpf = new HelpFormatter();
        CommandLine cl;


        // Define our commandline options
        Options options = new Options();
        options.addOption("h", "help", false, "print this help message and exit");
        options.addOption("f", "fasta", false, "Generate a fasta file to feed to tbl2asn for genbank submission");
        options.addOption("s", "sequin", false, "Generate a sequin file for genbank submission");
        options.addOption("t", "sequin_template", false, "The template file required for sequin generation");
        options.addOption("o", "output_directory", true, "Output Directory");
        options.addOption("p", "project_id", true, "Project Identifier.  A numeric integer corresponding to your project");

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

        // Help
        if (cl.hasOption("h") || cl.getOptions().length < 1) {
            helpf.printHelp("fims ", options, true);
            return;
        }

        // Sanitize project specification
        if (cl.hasOption("p")) {
            try {
                projectId = Integer.parseInt(cl.getOptionValue("p"));
            } catch (Exception e) {
                FimsPrinter.out.println("Bad option for project_id");
                helpf.printHelp("fims ", options, true);
                return;
            }
        } else {
            FimsPrinter.out.println("project_id is required");
            return;
        }

        if (cl.hasOption("o")) {
            outputDirectory = cl.getOptionValue("o");
        } else {
            outputDirectory = defaultOutputDirectory;
            FimsPrinter.out.println("Using default output_directory: " + defaultOutputDirectory);
        }

        if (cl.hasOption("s")) {
            generateSequin = true;
            if (cl.hasOption("t")) {
                templateFile = cl.getOptionValue("t");
            } else {
                FimsPrinter.out.println("Must provide a template file when generating a sequin file");
                return;
            }
        }

        manager.run(projectId, outputDirectory, templateFile, generateSequin);

    }
}

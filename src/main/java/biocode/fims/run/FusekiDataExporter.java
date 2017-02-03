package biocode.fims.run;

import biocode.fims.application.config.BiscicolAppConfig;
import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.Mapping;
import biocode.fims.elasticSearch.ElasticSearchIndexer;
import biocode.fims.entities.Bcid;
import biocode.fims.entities.Expedition;
import biocode.fims.entities.Project;
import biocode.fims.fileManagers.fimsMetadata.FimsMetadataFileManager;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.fuseki.fileManagers.fimsMetadata.FusekiFimsMetadataPersistenceManager;
import biocode.fims.fuseki.query.FimsQueryBuilder;
import biocode.fims.service.BcidService;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import biocode.fims.settings.FimsPrinter;
import biocode.fims.settings.SettingsManager;
import biocode.fims.settings.StandardPrinter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.cli.*;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.client.Client;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Tmp script to help with exporting fuseki data to file system
 * <p>
 * This will fetch the data from fuseki, and write the data to a csv file
 */
public class FusekiDataExporter {
    private ProjectService projectService;
    private BcidService bcidService;
    private Map<Integer, List<String>> failedExports = new LinkedHashMap<>();

    FusekiDataExporter(BcidService bcidService, ProjectService projectService) {
        this.bcidService = bcidService;
        this.projectService = projectService;
    }


    public void start(String outputDirectory, String projectUrl, Integer projectId) {
        List<Project> projectList = projectService.getProjects(projectUrl);

        for (Project project : projectList) {
            if (projectId == null || project.getProjectId() == projectId) {
                try {
                    System.out.println("exporting project: " + project.getProjectTitle());
                    File configFile = new ConfigurationFileFetcher(project.getProjectId(), System.getProperty("java.io.tmpdir"), false).getOutputFile();

                    export(project.getProjectId(), outputDirectory, configFile);
                } catch (Exception e) {
                    failedExports.computeIfAbsent(project.getProjectId(), k -> Collections.singletonList("all"));
                    e.printStackTrace();
                }
            }
        }

        if (!failedExports.isEmpty()) {
            System.out.println("Failed to export the following datasets:");
            MapUtils.debugPrint(System.out, "FAILED DATASETS:", failedExports);
        }
    }

    private void export(int projectId, String outputDirectory, File configFile) {
        Project project = projectService.getProjectWithExpeditions(projectId);

        Mapping mapping = new Mapping();
        mapping.addMappingRules(configFile);

        int totalResource = 0;

        List<Expedition> expeditions = new ArrayList<>();
        expeditions.addAll(project.getExpeditions());
        expeditions.sort(Comparator.comparing(Expedition::getExpeditionCode).reversed());


        // we need to fetch each Expedition individually as the SheetUniqueKey is only unique on the Expedition level
        for (Expedition expedition : expeditions) {
            for (Bcid bcid : bcidService.getDatasets(projectId, expedition.getExpeditionCode())) {
                try {
                    // only update bcids w/o a sourceFile set and graph != null
                    if (StringUtils.isBlank(bcid.getSourceFile()) && bcid.getGraph() != null) {

                        FimsQueryBuilder q = new FimsQueryBuilder(
                                mapping,
                                new String[]{bcid.getGraph()},
                                System.getProperty("java.io.tmpdir"));

                        File sourceFile = new File(q.writeCSV(false));
                        String oFilename = "fims_metadata_bcid_id_" + bcid.getBcidId() + ".csv";
                        File outputFile = new File(outputDirectory + oFilename);

                        Files.copy(sourceFile.toPath(), outputFile.toPath());

                        // update the bcid sourceFile
                        bcid.setSourceFile(outputFile.getName());
                        bcidService.update(bcid);

                    }

                } catch (Exception e) {
                    if (!failedExports.containsKey(projectId)) {
                        failedExports.put(projectId, new ArrayList<>());
                    }

                    failedExports.get(projectId).add(String.valueOf(bcid.getBcidId()));
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(BiscicolAppConfig.class);
        ProjectService projectService = applicationContext.getBean(ProjectService.class);
        BcidService bcidService = applicationContext.getBean(BcidService.class);

        String output_directory = null;
        String projectUrl = null;
        Integer projectId = null;

        // Direct output using the standardPrinter subClass of fimsPrinter which send to fimsPrinter.out (for command-line usage)
        FimsPrinter.out = new StandardPrinter();

        // Some classes to help us
        CommandLineParser clp = new GnuParser();
        HelpFormatter helpf = new HelpFormatter();
        CommandLine cl;

        // Define our commandline options
        Options options = new Options();
        options.addOption("h", "help", false, "print this help message and exit");
        options.addOption("o", "output_directory", true, "The directory where we should save the exported data. Most likely the serverRoot from the props file");
        options.addOption("u", "projectUrl", true, "The url of the projects to export. This will be used to query the projects table.");
        options.addOption("p", "project", true, "project");

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

        if (cl.hasOption("o")) {
            output_directory = cl.getOptionValue("o");
        } else {
            FimsPrinter.out.println("Error: You must specify an output_directory");
            return;
        }
        if (cl.hasOption("u")) {
            projectUrl = cl.getOptionValue("u");
        } else {
            FimsPrinter.out.println("Error: You must specify a projectUrl");
            return;
        }
        if (cl.hasOption("p")) {
            projectId = Integer.parseInt(cl.getOptionValue("p"));
        }

        boolean continueOperation = new StandardInputter().continueOperation("\n\nWarning: You are about to update the BCIDs. " +
                "Make sure you have turned off the bcid.ts auto update.");

        if (!continueOperation) {
            return;
        }
        FusekiDataExporter dataExporter = new FusekiDataExporter(bcidService, projectService);

        dataExporter.start(output_directory, projectUrl, projectId);
    }
}

package biocode.fims.dipnet.run;

import biocode.fims.application.config.DipnetAppConfig;
import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.Mapping;
import biocode.fims.elasticSearch.ElasticSearchIndexer;
import biocode.fims.entities.Expedition;
import biocode.fims.entities.Project;
import biocode.fims.fasta.fileManagers.FastaFileManager;
import biocode.fims.fileManagers.fimsMetadata.FimsMetadataFileManager;
import biocode.fims.fuseki.fileManagers.fimsMetadata.FusekiFimsMetadataPersistenceManager;
import biocode.fims.run.ProcessController;
import biocode.fims.service.BcidService;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import biocode.fims.settings.FimsPrinter;
import biocode.fims.settings.SettingsManager;
import biocode.fims.settings.StandardPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.cli.*;
import org.apache.commons.collections.MapUtils;
import org.elasticsearch.client.Client;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Tmp script to help with the migration from fuseki to ElasticSearch
 * <p>
 * This will fetch the data from fuseki, and insert into elasticsearch, replacing the existing
 * <urn:sequence> properties with the new fastaSequence entity objects
 */
public class DipnetFusekiToESMigrator {
    private ProjectService projectService;
    private Client esClient;
    private ExpeditionService expeditionService;
    private BcidService bcidService;
    private List<String> failedIndexes = new ArrayList<>();
    private LinkedHashMap<String, Integer> totalResources = new LinkedHashMap<>();
    private SettingsManager settingsManager;
    private MessageSource messageSource;

    DipnetFusekiToESMigrator(ExpeditionService expeditionService, BcidService bcidService, ProjectService projectService,
                             Client esClient, SettingsManager settingsManager, MessageSource messageSource) {
        this.expeditionService = expeditionService;
        this.bcidService = bcidService;
        this.projectService = projectService;
        this.esClient = esClient;
        this.settingsManager = settingsManager;
        this.messageSource = messageSource;
    }

    public void migrate(int projectId, String outputDirectory) {
        Project project = projectService.getProjectWithExpeditions(projectId);
        File configFile = new ConfigurationFileFetcher(projectId, outputDirectory, true).getOutputFile();

        Mapping mapping = new Mapping();
        mapping.addMappingRules(configFile);

        int totalResource = 0;

        // we need to fetch each Expedition individually as the SheetUniqueKey is only unique on the Expedition level
        for (Expedition expedition : project.getExpeditions()) {
            try {
                FusekiFimsMetadataPersistenceManager persistenceManager = new FusekiFimsMetadataPersistenceManager(expeditionService, bcidService, settingsManager);
                FimsMetadataFileManager fimsMetadataFileManager = new FimsMetadataFileManager(
                        persistenceManager, SettingsManager.getInstance(), expeditionService, bcidService, messageSource);


                ProcessController processController = new ProcessController(projectId, expedition.getExpeditionCode());
                processController.setOutputFolder(outputDirectory);
                processController.setMapping(mapping);
                fimsMetadataFileManager.setProcessController(processController);


                System.out.println("updating expedition: " + expedition.getExpeditionCode());

                String[] splitTitle = expedition.getExpeditionTitle().split("_");
                String marker;

                // for all original DIPnet data with fasta sequences (none test data) the expedition title was of the format
                // xxx_marker_xxx
                if (splitTitle.length < 3) {
                    marker = "UNKNOWN";
                } else {
                    marker = splitTitle[1];
                }

                System.out.println("marker: " + marker);

                ArrayNode dataset = fimsMetadataFileManager.index();

                for (JsonNode node: dataset) {
                    ObjectNode resource = (ObjectNode) node;

                    String sequence = resource.remove("urn:sequence").asText();

                    if (sequence != null) {
                        ObjectNode fastaSequence = dataset.objectNode();
                        fastaSequence.put("urn:sequence", sequence);
                        fastaSequence.put("urn:marker", marker);

                        ArrayNode fastaSequences = resource.putArray(FastaFileManager.ENTITY_CONCEPT_ALIAS);
                        fastaSequences.add(fastaSequence);
                    }
                }

                System.out.println("\nindexing " + dataset.size() + " resources ....\n");

                ElasticSearchIndexer indexer = new ElasticSearchIndexer(esClient);
                indexer.indexDataset(
                        project.getProjectId(),
                        expedition.getExpeditionCode(),
                        dataset
                );
                totalResources.put(expedition.getExpeditionCode(), dataset.size());
                totalResource += dataset.size();

            } catch (Exception e) {
                failedIndexes.add(expedition.getExpeditionCode());
                e.printStackTrace();
            }
        }
        totalResources.put("project_total", totalResource);
        MapUtils.debugPrint(System.out,"ProjectId, total_resources_indexed MAP:",totalResources);
        System.out.println(project.getExpeditions().size());
    }

    public static void main(String[] args) {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(DipnetAppConfig.class);
        Client esClient = applicationContext.getBean(Client.class);
        ProjectService projectService = applicationContext.getBean(ProjectService.class);
        ExpeditionService expeditionService = applicationContext.getBean(ExpeditionService.class);
        BcidService bcidService = applicationContext.getBean(BcidService.class);
        SettingsManager settingsManager = applicationContext.getBean(SettingsManager.class);
        MessageSource messageSource = applicationContext.getBean(MessageSource.class);

        String output_directory = "tripleOutput/";

        // Direct output using the standardPrinter subClass of fimsPrinter which send to fimsPrinter.out (for command-line usage)
        FimsPrinter.out = new StandardPrinter();

        // Some classes to help us
        CommandLineParser clp = new GnuParser();
        HelpFormatter helpf = new HelpFormatter();
        CommandLine cl;

        // Define our commandline options
        Options options = new Options();
        options.addOption("h", "help", false, "print this help message and exit");
        options.addOption("o", "output_directory", true, "Output Directory");

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
        }

        DipnetFusekiToESMigrator dataMigrator = new DipnetFusekiToESMigrator(expeditionService, bcidService, projectService, esClient, settingsManager, messageSource);

        // also need to run this for dipnet training projectId 1?
        dataMigrator.migrate(25, output_directory);
    }
}

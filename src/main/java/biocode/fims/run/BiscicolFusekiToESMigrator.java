package biocode.fims.run;

import biocode.fims.application.config.BiscicolAppConfig;
import biocode.fims.config.ConfigurationFileEsMapper;
import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.Mapping;
import biocode.fims.elasticSearch.ElasticSearchIndexer;
import biocode.fims.entities.Expedition;
import biocode.fims.entities.Project;
import biocode.fims.fileManagers.fimsMetadata.FimsMetadataFileManager;
import biocode.fims.fuseki.fileManagers.fimsMetadata.FusekiFimsMetadataPersistenceManager;
import biocode.fims.service.BcidService;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import biocode.fims.settings.FimsPrinter;
import biocode.fims.settings.SettingsManager;
import biocode.fims.settings.StandardPrinter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.cli.*;
import org.apache.commons.collections.MapUtils;
import org.elasticsearch.client.Client;
import org.json.simple.JSONObject;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.File;
import java.util.*;

/**
 * Tmp script to help with the migration from fuseki to ElasticSearch
 * <p>
 * This will fetch the data from fuseki, and insert into elasticsearch, replacing the existing
 * <urn:sequence> properties with the new fastaSequence entity objects
 */
public class BiscicolFusekiToESMigrator {
    private ProjectService projectService;
    private Client esClient;
    private ExpeditionService expeditionService;
    private BcidService bcidService;
    private Map<Integer, List<String>> failedIndexes = new LinkedHashMap<>();
    private Map<Integer, LinkedHashMap<String, Integer>> totalResources = new LinkedHashMap<>();
    private MessageSource messageSource;
    private SettingsManager settingsManager;

    BiscicolFusekiToESMigrator(ExpeditionService expeditionService, BcidService bcidService, ProjectService projectService,
                               Client esClient, MessageSource messageSource, SettingsManager settingsManager) {
        this.expeditionService = expeditionService;
        this.bcidService = bcidService;
        this.projectService = projectService;
        this.esClient = esClient;
        this.messageSource = messageSource;
        this.settingsManager = settingsManager;
    }

    /**
     * create the index and update the mapping
     *
     * @param projectId
     * @param configFile
     */
    private void createIndex(int projectId, File configFile) {
        JSONObject mapping = ConfigurationFileEsMapper.convert(configFile);

        ElasticSearchIndexer indexer = new ElasticSearchIndexer(esClient);
        indexer.updateMapping(projectId, mapping);

    }

    public void start(String outputDirectory, Integer projectId) {
        List<Project> projectList = projectService.getProjects(SettingsManager.getInstance().retrieveValue("appRoot"));

        for (Project project : projectList) {
            if (projectId == null || (projectId != null && project.getProjectId() == projectId)) {
                try {
                    System.out.println("updating project: " + project.getProjectTitle());
                    File configFile = new ConfigurationFileFetcher(project.getProjectId(), outputDirectory, false).getOutputFile();

                    createIndex(project.getProjectId(), configFile);
                    migrate(project.getProjectId(), outputDirectory, configFile);
                } catch (Exception e) {
                    failedIndexes.computeIfAbsent(project.getProjectId(), k -> Collections.singletonList("all"));
                    e.printStackTrace();
                }
            }
        }

        if (!failedIndexes.isEmpty()) {
            System.out.println("Failed to index the following expeditions:");
            MapUtils.debugPrint(System.out, "FAILED INDEXES:", failedIndexes);
        }

        MapUtils.debugPrint(System.out, "ProjectId, total_resources_indexed MAP:", totalResources);
    }

    private void migrate(int projectId, String outputDirectory, File configFile) {
        Project project = projectService.getProjectWithExpeditions(projectId);

        Mapping mapping = new Mapping();
        mapping.addMappingRules(configFile);

        int totalResource = 0;

        List<Expedition> expeditions = new ArrayList<>();
        expeditions.addAll(project.getExpeditions());
        expeditions.sort(Comparator.comparing(Expedition::getExpeditionCode).reversed());


        // we need to fetch each Expedition individually as the SheetUniqueKey is only unique on the Expedition level
        for (Expedition expedition : expeditions) {
            try {
                FusekiFimsMetadataPersistenceManager persistenceManager = new FusekiFimsMetadataPersistenceManager(expeditionService, bcidService, settingsManager);
                FimsMetadataFileManager fimsMetadataFileManager = new FimsMetadataFileManager(
                        persistenceManager, SettingsManager.getInstance(), expeditionService, bcidService, messageSource);


                ProcessController processController = new ProcessController(projectId, expedition.getExpeditionCode());
                processController.setOutputFolder(outputDirectory);
                processController.setMapping(mapping);
                fimsMetadataFileManager.setProcessController(processController);


                System.out.println("updating expedition: " + expedition.getExpeditionCode());

                ArrayNode dataset = fimsMetadataFileManager.index();

                System.out.println("\nindexing " + dataset.size() + " resources ....\n");

                ElasticSearchIndexer indexer = new ElasticSearchIndexer(esClient);
                indexer.indexDataset(
                        project.getProjectId(),
                        expedition.getExpeditionCode(),
                        dataset
                );

                totalResources.computeIfAbsent(projectId, k -> new LinkedHashMap<>());
                totalResources.get(projectId).put(expedition.getExpeditionCode(), dataset.size());
                totalResource += dataset.size();
            } catch (Exception e) {
                if (!failedIndexes.containsKey(projectId)) {
                    failedIndexes.put(projectId, new ArrayList<>());
                }

                failedIndexes.get(projectId).add(expedition.getExpeditionCode());
                e.printStackTrace();
            }
        }
        System.out.println("Indexed " + totalResource + " resources");
        totalResources.computeIfAbsent(projectId, k -> new LinkedHashMap<>());
        totalResources.get(projectId).put("project_total", totalResource);
    }

    public static void main(String[] args) {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(BiscicolAppConfig.class);
        Client esClient = applicationContext.getBean(Client.class);
        ProjectService projectService = applicationContext.getBean(ProjectService.class);
        ExpeditionService expeditionService = applicationContext.getBean(ExpeditionService.class);
        BcidService bcidService = applicationContext.getBean(BcidService.class);
        SettingsManager settingsManager = applicationContext.getBean(SettingsManager.class);
        MessageSource messageSource = applicationContext.getBean(MessageSource.class);

        String output_directory = "tripleOutput/";
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
        options.addOption("o", "output_directory", true, "Output Directory");
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
        }
        if (cl.hasOption("p")) {
            projectId = Integer.parseInt(cl.getOptionValue("p"));
        }

        BiscicolFusekiToESMigrator dataMigrator = new BiscicolFusekiToESMigrator(expeditionService, bcidService, projectService, esClient, messageSource, settingsManager);

        // also need to run this for dipnet training projectId 1?
        dataMigrator.start(output_directory, projectId);
    }
}

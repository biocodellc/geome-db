package biocode.fims.run;

import biocode.fims.application.config.BiscicolAppConfig;
import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.Mapping;
import biocode.fims.elasticSearch.ElasticSearchIndexer;
import biocode.fims.elasticSearch.query.ElasticSearchQuerier;
import biocode.fims.elasticSearch.query.ElasticSearchQuery;
import biocode.fims.entities.Expedition;
import biocode.fims.entities.Project;
import biocode.fims.fileManagers.fimsMetadata.FimsMetadataFileManager;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.fuseki.fileManagers.fimsMetadata.FusekiFimsMetadataPersistenceManager;
import biocode.fims.query.JsonFieldTransform;
import biocode.fims.service.BcidService;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import biocode.fims.settings.FimsPrinter;
import biocode.fims.settings.PathManager;
import biocode.fims.settings.SettingsManager;
import biocode.fims.settings.StandardPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.cli.*;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.Page;

import java.io.*;
import java.util.*;

/**
 * Tmp script to help with the migration from fuseki to ElasticSearch
 * <p>
 * This will fetch the data from fuseki, and insert into elasticsearch, replacing the existing
 * <urn:sequence> properties with the new fastaSequence entity objects
 */
@SuppressWarnings("Duplicates")
public class FusekiToESDataReporter {
    private ProjectService projectService;
    private Client esClient;
    private ExpeditionService expeditionService;
    private BcidService bcidService;
    private Map<Integer, List<String>> failedIndexes = new LinkedHashMap<>();
    private Map<Integer, List<DatasetStats>> projectDatasetStats = new HashMap<>();
    private MessageSource messageSource;
    private SettingsManager settingsManager;

    FusekiToESDataReporter(ExpeditionService expeditionService, BcidService bcidService, ProjectService projectService,
                           Client esClient, MessageSource messageSource, SettingsManager settingsManager) {
        this.expeditionService = expeditionService;
        this.bcidService = bcidService;
        this.projectService = projectService;
        this.esClient = esClient;
        this.messageSource = messageSource;
        this.settingsManager = settingsManager;
    }

    public void start(String outputDirectory, Integer projectId) {
        List<Project> projectList = projectService.getProjects();

        for (Project project : projectList) {
            if (projectId == null || project.getProjectId() == projectId) {
                try {
                    System.out.println("updating project: " + project.getProjectTitle());
                    File configFile = new ConfigurationFileFetcher(project.getProjectId(), outputDirectory, false).getOutputFile();

                    getStats(project.getProjectId(), outputDirectory, configFile);
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

        // TODO write projectDatasetStats to file
        writeStatsToFile(outputDirectory);
    }

    private void writeStatsToFile(String outputDirectory) {
        File oFile = PathManager.createUniqueFile("fusekiToEsStats.csv", outputDirectory);
        String delimiter = ",";

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(oFile)))) {

            StringEscapeUtils.escapeCsv(writer, "projectId");
            writer.write(delimiter);
            StringEscapeUtils.escapeCsv(writer, "expeditionCode");
            writer.write(delimiter);
            StringEscapeUtils.escapeCsv(writer, "fusekiResourceCount");
            writer.write(delimiter);
            StringEscapeUtils.escapeCsv(writer, "esResourceCount");
            writer.write(delimiter);

            writer.write("\n");

            for (Map.Entry<Integer, List<DatasetStats>> entry : projectDatasetStats.entrySet()) {
                String projectId = String.valueOf(entry.getKey());

                for (DatasetStats s: entry.getValue()) {
                    StringEscapeUtils.escapeCsv(writer, projectId);
                    writer.write(delimiter);
                    StringEscapeUtils.escapeCsv(writer, s.getExpeditionCode());
                    writer.write(delimiter);
                    StringEscapeUtils.escapeCsv(writer, String.valueOf(s.getNumberOfFusekiResources()));
                    writer.write(delimiter);
                    StringEscapeUtils.escapeCsv(writer, String.valueOf(s.getNumberOfEsResources()));
                    writer.write(delimiter);

                    writer.write("\n");
                }
            }

        } catch (IOException e) {
            throw new FimsRuntimeException(FileCode.WRITE_ERROR, 500);
        }

        System.out.println("\n\nResults file:  " + oFile.getAbsolutePath());

    }

    private void getStats(int projectId, String outputDirectory, File configFile) {
        Project project = projectService.getProjectWithExpeditions(projectId);

        List<DatasetStats> stats = new ArrayList<>();
        Mapping mapping = new Mapping();
        mapping.addMappingRules(configFile);

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


                System.out.println("getting stats for expedition: " + expedition.getExpeditionCode());

                ArrayNode dataset = fimsMetadataFileManager.getDataset();

                QueryBuilder boolQuery = QueryBuilders.boolQuery()
                        .must(
                                QueryBuilders.matchQuery("expedition.expeditionCode.keyword", expedition.getExpeditionCode())
                        );
                ElasticSearchQuery query = new ElasticSearchQuery(boolQuery, new String[]{String.valueOf(projectId)}, new String[]{ElasticSearchIndexer.TYPE});
                ElasticSearchQuerier querier = new ElasticSearchQuerier(esClient, query);
                Page<ObjectNode> pageableResults = querier.getPageableResults();

                int numberOfEsResources = (int) pageableResults.getTotalElements();
                int numberOfFusekiResources = dataset.size();

                DatasetStats datasetStats = new DatasetStats(expedition.getExpeditionCode(), numberOfFusekiResources, numberOfEsResources);
                stats.add(datasetStats);
            } catch (Exception e) {
                if (!failedIndexes.containsKey(projectId)) {
                    failedIndexes.put(projectId, new ArrayList<>());
                }

                failedIndexes.get(projectId).add(expedition.getExpeditionCode());
                e.printStackTrace();
            }
        }
        projectDatasetStats.put(projectId, stats);
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

        FusekiToESDataReporter dataReporter = new FusekiToESDataReporter(expeditionService, bcidService, projectService, esClient, messageSource, settingsManager);

        dataReporter.start(output_directory, projectId);
    }

    private class DatasetStats {
        private final String expeditionCode;
        private final int numberOfFusekiResources;
        private final int numberOfEsResources;

        public DatasetStats(String expeditionCode, int numberOfFusekiResources, int numberOfEsResources) {
            this.expeditionCode = expeditionCode;
            this.numberOfFusekiResources = numberOfFusekiResources;
            this.numberOfEsResources = numberOfEsResources;
        }

        public String getExpeditionCode() {
            return expeditionCode;
        }

        public int getNumberOfFusekiResources() {
            return numberOfFusekiResources;
        }

        public int getNumberOfEsResources() {
            return numberOfEsResources;
        }
    }
}

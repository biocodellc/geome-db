package biocode.fims.run;

import biocode.fims.application.config.GeomeAppConfig;
import biocode.fims.digester.Attribute;
import biocode.fims.digester.Entity;
import biocode.fims.elasticSearch.ElasticSearchIndexer;
import biocode.fims.elasticSearch.query.ElasticSearchQuerier;
import biocode.fims.elasticSearch.query.ElasticSearchQuery;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.models.Expedition;
import biocode.fims.models.Project;
import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.service.ProjectService;
import biocode.fims.settings.FimsPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.cli.*;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
public class FimsPostgresMigrator {
    private final static Logger logger = LoggerFactory.getLogger(FimsPostgresMigrator.class);

    private final ProjectService projectService;
    private final RecordRepository recordRepository;
    private final ProjectConfigConverter configConverter;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final String projectUrl;
    private final Client client;

    FimsPostgresMigrator(ProjectService projectService, RecordRepository recordRepository,
                         ProjectConfigConverter configConverter, String projectUrl, NamedParameterJdbcTemplate jdbcTemplate,
                         Client client) {
        this.projectService = projectService;
        this.recordRepository = recordRepository;
        this.configConverter = configConverter;
        this.projectUrl = projectUrl;
        this.jdbcTemplate = jdbcTemplate;
        this.client = client;
    }

    void migrate() throws IOException {
        configConverter.storeConfigs();

        // create entity tables and migrate data from elasticsearch
        for (Project p : projectService.getProjectsWithExpeditions(projectUrl)) {

            ProjectConfig config = p.getProjectConfig();

            if (config != null && config.entities().size() > 0) {

                migrateData(p, p.getProjectConfig());
            } else {
                logger.error("project id: " + p.getProjectId() + " project_config is null. Not creating schema and entity tables.");
            }

        }
    }

    private void migrateData(Project p, ProjectConfig config) {

        List<Attribute> attributes = new ArrayList<>();
        config.entities().forEach(e -> attributes.addAll(e.getAttributes()));

        int records = 0;
        for (Expedition e : p.getExpeditions()) {
            try {
                insertEntityIdentifiers(e, config);

                QueryBuilder q = QueryBuilders.matchQuery("expedition.expeditionCode.keyword", e.getExpeditionCode());
                ElasticSearchQuery query = new ElasticSearchQuery(q, new String[]{String.valueOf(p.getProjectId())}, new String[]{ElasticSearchIndexer.TYPE});

                ElasticSearchQuerier querier = new ElasticSearchQuerier(client, query);

                Entity entity = p.getProjectConfig().entities().getFirst();
                RecordSet recordSet = new RecordSet(entity, false);

                ArrayNode allResults = querier.getAllResults();
                for (JsonNode node : allResults) {
                    Record record = new GenericRecord();

                    Iterator<String> it = node.fieldNames();
                    while (it.hasNext()) {
                        String field = it.next();
                        // we added bcid and expedition.expeditionCode properties, they are no longer needed
                        if (!field.equals("bcid") && !field.equals("expedition.expeditionCode")) {
                            record.set(field, node.get(field).asText());
                        }
                    }

                    recordSet.add(record);
                }

                logger.info("expedition: " + e.getExpeditionCode() + " resources: " + allResults.size());

                Dataset dataset = new Dataset();
                dataset.add(recordSet);

                recordRepository.saveDataset(dataset, p.getProjectId(), e.getExpeditionId());
                if (recordSet.records().size() != allResults.size()) {
                    throw new Exception("INVALID_SIZES " + dataset.size() + " != " + allResults.size());
                }
                records += recordSet.records().size();

            } catch (Exception exception) {
                if (exception instanceof FimsRuntimeException && ((FimsRuntimeException) exception).getErrorCode() == QueryCode.NO_RESOURCES) {
                    continue;
                }
                logger.error("Error migrating data for projectId: " + p.getProjectId() + " expeditionId: " + e.getExpeditionId(), exception);
            }

        }
        logger.info("Migrated " + records + " for projectId: " + p.getProjectId());

    }

    private void insertEntityIdentifiers(Expedition e, ProjectConfig config) {
        List<String> conceptAliases = config.entities()
                .stream()
                .map(Entity::getConceptAlias)
                .collect(Collectors.toList());

        try {
            String sql = "INSERT INTO entity_identifiers (expedition_id, concept_alias, identifier) " +
                    "select " + e.getExpeditionId() + ", b.title, b.identifier " +
                    "from bcids_tmp b " +
                    "join expedition_bcids eb on b.id = eb.bcid_id " +
                    "where eb.expedition_id = " + e.getExpeditionId() + " AND b.title in ('" + String.join("', '", conceptAliases) + "');";

            int rows = jdbcTemplate.update(sql, new HashMap<>());

            if (rows != conceptAliases.size()) {
                logger.warn("MISSING_ENTITY_IDENTIFIERS\tDidn't find bcids for every entity for expedition: " + e.getExpeditionId());
            }

        } catch (Exception exception) {
            logger.error("ENTITY_IDENTIFIER_ERROR\t Error inserting entity_identifiers", exception);
        }
    }

    public static void main(String[] args) throws IOException {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(GeomeAppConfig.class);
        ProjectService projectService = applicationContext.getBean(ProjectService.class);
        RecordRepository repository = applicationContext.getBean(RecordRepository.class);
        NamedParameterJdbcTemplate jdbcTemplate = applicationContext.getBean(NamedParameterJdbcTemplate.class);
        Client client = applicationContext.getBean(Client.class);

        String projectUrl = null;

        // Some classes to help us
        CommandLineParser clp = new GnuParser();
        HelpFormatter helpf = new HelpFormatter();
        CommandLine cl;

        // Define our commandline options
        Options options = new Options();
        options.addOption("h", "help", false, "print this help message and exit");
        options.addOption("url", true, "project.projectUrl for the projects to migrate to postgresql");

        // Create the commands parser and parse the command line arguments.
        try {
            cl = clp.parse(options, args);
        } catch (ParseException e) {
            FimsPrinter.out.println("Error: " + e.getMessage());
            return;
        }

        // Help
        if (cl.getOptions().length < 1 || cl.hasOption("h")) {
            helpf.printHelp("fims ", options, true);
            return;
        }

        if (cl.hasOption("url")) {
            projectUrl = cl.getOptionValue("url");
        }

        if (projectUrl == null) {
            System.out.println("url is required.");
            return;
        }

        ProjectConfigConverter projectConfigConverter = new ProjectConfigConverter(projectService, projectUrl);

        FimsPostgresMigrator migrator = new FimsPostgresMigrator(projectService, repository, projectConfigConverter, projectUrl, jdbcTemplate, client);
        migrator.migrate();
    }
}

package biocode.fims.run;

import biocode.fims.application.config.*;
import biocode.fims.bcid.BcidBuilder;
import biocode.fims.config.models.Entity;
import biocode.fims.evolution.processing.EvolutionUpdateCreateTask;
import biocode.fims.evolution.service.EvolutionService;
import biocode.fims.models.Project;
import biocode.fims.query.QueryBuilder;
import biocode.fims.query.QueryResult;
import biocode.fims.query.QueryResults;
import biocode.fims.query.dsl.ProjectExpression;
import biocode.fims.query.dsl.Query;
import biocode.fims.query.dsl.SelectExpression;
import biocode.fims.records.RecordSet;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import org.apache.commons.cli.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A script to backload all existing data into Evolution.
 * <p>
 * This script is meant to only run a single time.
 *
 * @author RJ Ewing
 */
public class EvolutionDataLoader {
    private final ProjectService projectService;
    private final RecordRepository recordRepository;
    private final EvolutionService evolutionService;
    private final FimsProperties props;
    private final EvolutionProperties evolutionProperties;
    private ExpeditionService expeditionService;

    public EvolutionDataLoader(ProjectService projectService, RecordRepository recordRepository, EvolutionService evolutionService, ExpeditionService expeditionService, FimsProperties props, EvolutionProperties evolutionProperties) {
        this.projectService = projectService;
        this.recordRepository = recordRepository;
        this.evolutionService = evolutionService;
        this.expeditionService = expeditionService;
        this.props = props;
        this.evolutionProperties = evolutionProperties;
    }


    private void load(List<Integer> projects) {
        String resolverEndpoint = evolutionProperties.resolverEndpoint();

        for (Project project : projectService.getProjects()) {
            if (projects != null && !projects.contains(project.getProjectId())) continue;
            System.out.println("Loading project: " + project.getProjectTitle());
            List<String> entities = project.getProjectConfig()
                    .entities()
                    .stream()
                    .map(Entity::getConceptAlias)
                    .collect(Collectors.toList());

            ProjectExpression e = new ProjectExpression(Collections.singletonList(project.getProjectId()));
            SelectExpression q = new SelectExpression(String.join(",", entities.subList(1, entities.size())), e);

            QueryBuilder qb = new QueryBuilder(project.getProjectConfig(), project.getNetwork().getId(), entities.get(0));
            Query query = new Query(qb, project.getProjectConfig(), q);

            QueryResults queryResults = recordRepository.query(query);

            for (QueryResult result : queryResults) {
                System.out.println("Loading Entity: " + result.entity().getConceptAlias());
                BcidBuilder bcidBuilder = new BcidBuilder(result.entity(), result.entity().isChildEntity() ? project.getProjectConfig().entity(result.entity().getParentEntity()) : null, props.bcidResolverPrefix());
                BcidBuilder parentBcidBuilder = null;
                RecordSet parentRecordSet = null;

                if (result.entity().isChildEntity()) {
                    QueryResult parent = queryResults.getResult(result.entity().getParentEntity());
                    parentRecordSet = new RecordSet(parent.entity(), parent.records(), false);
                    parentBcidBuilder = new BcidBuilder(parent.entity(), parent.entity().isChildEntity() ? queryResults.getResult(parent.entity().getParentEntity()).entity() : null, props.bcidResolverPrefix());
                }

                new EvolutionUpdateCreateTask(
                        evolutionService,
                        expeditionService,
                        bcidBuilder,
                        result.records(),
                        Collections.emptyList(),
                        parentRecordSet,
                        parentBcidBuilder,
                        resolverEndpoint,
                        props.userURIPrefix()
                ).run();
            }
        }
    }

    public static void main(String[] args) {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(GeomeAppConfig.class);
        ProjectService projectService = applicationContext.getBean(ProjectService.class);
        EvolutionService evolutionService = applicationContext.getBean(EvolutionService.class);
        RecordRepository recordRepository = applicationContext.getBean(RecordRepository.class);
        ExpeditionService expeditionService = applicationContext.getBean(ExpeditionService.class);
        FimsProperties props = applicationContext.getBean(FimsProperties.class);
        EvolutionProperties evolutionProps = applicationContext.getBean(EvolutionProperties.class);

        // Some classes to help us
        CommandLineParser clp = new DefaultParser();
        CommandLine cl;

        Options options = new Options();
        options.addOption("p", "projects", true, "The projects to load. Defaults to loading all.");

        try {
            cl = clp.parse(options, args);
        } catch (ParseException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        List<Integer> projects = null;
        if (cl.hasOption("p")) {
            projects = Stream.of(cl.getOptionValues("p")).map(Integer::parseInt).collect(Collectors.toList());
        }

        EvolutionDataLoader dataLoader = new EvolutionDataLoader(projectService, recordRepository, evolutionService, expeditionService, props, evolutionProps);
        dataLoader.load(projects);
    }
}

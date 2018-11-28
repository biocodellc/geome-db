package biocode.fims.run;

import biocode.fims.application.config.GeomeAppConfig;
import biocode.fims.config.EntitySort;
import biocode.fims.config.models.Entity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.models.Project;
import biocode.fims.models.ProjectConfiguration;
import biocode.fims.query.PostgresUtils;
import biocode.fims.query.QueryResult;
import biocode.fims.query.dsl.Query;
import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.rest.FimsObjectMapper;
import biocode.fims.service.ProjectConfigurationService;
import biocode.fims.service.ProjectService;
import biocode.fims.utils.RecordHasher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * class to migrate any record hash values. Originally, the hashes were calculated using all properties
 * on a spreadsheet either empty or present. Now we only calculate based on present properties
 *
 * @author RJ Ewing
 */
public class HashMigrator {
    private final ProjectConfigurationService projectConfigurationService;
    private final ProjectService projectService;
    private final RecordRepository recordRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    private HashMigrator(ProjectConfigurationService projectConfigurationService, ProjectService projectService,
                         RecordRepository recordRepository, NamedParameterJdbcTemplate jdbcTemplate) {
        this.projectConfigurationService = projectConfigurationService;
        this.projectService = projectService;
        this.recordRepository = recordRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    private void migrate() throws JsonProcessingException {
        List<Project> projects = projectService.getProjects();

        List<ProjectConfiguration> hashedEntityConfigs = projectConfigurationService.getProjectConfigurations(null).stream()
                .filter(c -> c.getProjectConfig().entities().stream().anyMatch(Entity::isHashed))
                .collect(Collectors.toList());

        for (ProjectConfiguration c : hashedEntityConfigs) {
            updateProjects(
                    projects.stream()
                            .filter(p -> p.getProjectConfiguration().equals(c))
                            .collect(Collectors.toList())
            );
        }
    }

    private void updateProjects(List<Project> projects) throws JsonProcessingException {
        if (projects.size() == 0) return;

        ObjectMapper mapper = new FimsObjectMapper();

        ProjectConfig config = projects.get(0).getProjectConfig();

        for (Project project : projects) {
            System.out.println(project.getProjectId());
            for (Entity e : config.entities(EntitySort.PARENTS_FIRST)) {
                if (!e.isHashed()) continue;

                List<Entity> childEntities = config.entities().stream()
                        .filter(entity -> entity.isChildEntity() && entity.getParentEntity().equals(e.getConceptAlias()))
                        .collect(Collectors.toList());

                System.out.println("ProjectID = " +
                        project.getProjectId() + " \t entity = " +
                        e.getConceptAlias() + " \t childEntities = " +
                        String.join(",", childEntities.stream().map(Entity::getConceptAlias).collect(Collectors.toList()))
                );

                String parentTable = PostgresUtils.entityTable(project.getNetwork().getId(), e.getConceptAlias());

                int count = 0;
                for (Record r : recordRepository.getRecords(project, e.getConceptAlias(), GenericRecord.class)) {
                    Map<String, String> props = new HashMap<>(r.properties());
                    props.remove(e.getUniqueKeyURI());

                    Record record = new GenericRecord(props);
                    // generate hash w/o uniqueKey as is done on upload
                    String hash = RecordHasher.hash(record);
                    String oldHash = r.get(e.getUniqueKeyURI());

                    if (hash.equals(oldHash)) continue;
                    count++;
                    System.out.println("oldHash = " + oldHash + " \tnewHash = " + hash);

                    r.set(e.getUniqueKeyURI(), hash);

                    String data = mapper.writeValueAsString(r.properties());

                    Map<String, String> params = new HashMap<>();
                    params.put("data", data);
                    params.put("expedition_code", r.expeditionCode());
                    if (e.isChildEntity()) {
                        params.put("parent_identifier", r.get(config.entity(e.getParentEntity()).getUniqueKeyURI()));
                    }

                    jdbcTemplate.update(
                            "INSERT INTO " + parentTable + " (local_identifier, data, expedition_id" + (e.isChildEntity() ? ", parent_identifier" : "") + ") VALUES ('" + hash + "', to_jsonb(:data::jsonb), " +
                                    " (SELECT id from expeditions where expedition_code = :expedition_code and project_id = " + r.projectId() + ") " +
                                    (e.isChildEntity() ? ", :parent_identifier" : "") +
                                    ") ON CONFLICT (local_identifier, expedition_id) DO NOTHING",
                            params
                    );

                    for (Entity c : childEntities) {
                        String qs = e.getUniqueKey() + "=" + oldHash + " and _expeditions_:" + r.expeditionCode() + " and _projects_:" + r.projectId();
                        Query query = Query.factory(project, c.getConceptAlias(), qs);
                        QueryResult queryResult = recordRepository.query(query).getResult(c.getConceptAlias());

                        String childTable = PostgresUtils.entityTable(project.getNetwork().getId(), c.getConceptAlias());

                        List<MapSqlParameterSource> childParams = new ArrayList<>();

                        for (Record childRecord : queryResult.records()) {
                            childRecord.set(e.getUniqueKeyURI(), hash);

                            String childData = mapper.writeValueAsString(childRecord.properties());

                            MapSqlParameterSource cp = new MapSqlParameterSource();
                            cp.addValue("data", childData);
                            cp.addValue("local_identifier", childRecord.get(c.getUniqueKeyURI()));
                            cp.addValue("expedition_code", childRecord.expeditionCode());

                            childParams.add(cp);
                        }

                        jdbcTemplate.batchUpdate(
                                "UPDATE " + childTable + " SET parent_identifier = '" + hash + "', data = to_jsonb(:data::jsonb) where " +
                                        "local_identifier = :local_identifier and expedition_id = (SELECT id from expeditions where expedition_code = :expedition_code and project_id = " + r.projectId() + ")",
                                childParams.toArray(new MapSqlParameterSource[childParams.size()])
                        );
                    }
                    jdbcTemplate.getJdbcOperations().update("DELETE FROM " + parentTable + " WHERE local_identifier = '" + oldHash + "' and expedition_id = (SELECT id from expeditions where expedition_code = '" + r.expeditionCode() + "' and project_id = " + r.projectId() + ")");
                }
                System.out.println("count = " + count);

            }

        }
    }

    public static void main(String[] args) throws IOException {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(GeomeAppConfig.class);
        ProjectService projectService = applicationContext.getBean(ProjectService.class);
        ProjectConfigurationService projectConfigurationService = applicationContext.getBean(ProjectConfigurationService.class);
        RecordRepository recordRepository = applicationContext.getBean(RecordRepository.class);
        NamedParameterJdbcTemplate jdbcTemplate = applicationContext.getBean(NamedParameterJdbcTemplate.class);

        HashMigrator hashMigrator = new HashMigrator(projectConfigurationService, projectService, recordRepository, jdbcTemplate);
        hashMigrator.migrate();
    }
}

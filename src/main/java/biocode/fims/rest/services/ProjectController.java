package biocode.fims.rest.services;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.application.config.GeomeProperties;
import biocode.fims.application.config.GeomeSql;
import biocode.fims.authorizers.QueryAuthorizer;
import biocode.fims.config.models.Entity;
import biocode.fims.config.models.FastqEntity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.GenericErrorCode;
import biocode.fims.geome.sra.GeomeBioSampleMapper;
import biocode.fims.geome.sra.GeomeSraMetadataMapper;
import biocode.fims.models.Network;
import biocode.fims.models.Project;
import biocode.fims.models.User;
import biocode.fims.models.dataTypes.JacksonUtil;
import biocode.fims.ncbi.sra.submission.BioSampleAttributesGenerator;
import biocode.fims.ncbi.sra.submission.BioSampleMapper;
import biocode.fims.ncbi.sra.submission.SraMetadataGenerator;
import biocode.fims.ncbi.sra.submission.SraMetadataMapper;
import biocode.fims.query.PostgresUtils;
import biocode.fims.query.QueryBuilder;
import biocode.fims.query.QueryResults;
import biocode.fims.query.dsl.*;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.rest.FimsObjectMapper;
import biocode.fims.rest.responses.FileResponse;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.NetworkService;
import biocode.fims.service.ProjectService;
import biocode.fims.tools.FileCache;
import biocode.fims.utils.FileUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Controller;

import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * project API endpoints
 *
 * @resourceTag Projects
 */
@Controller
@Path("projects")
@Produces({MediaType.APPLICATION_JSON})
@Singleton
public class ProjectController extends BaseProjectsController {

    @Context
    private ServletContext context;
    private final GeomeSql geomeSql;
    private final QueryAuthorizer queryAuthorizer;
    private final RecordRepository recordRepository;
    private final FileCache fileCache;
    private final NetworkService networkService;
    private final GeomeProperties geomeProps;

    @Autowired
    ProjectController(ExpeditionService expeditionService, FimsProperties props, GeomeSql geomeSql,
                      ProjectService projectService, QueryAuthorizer queryAuthorizer,
                      RecordRepository recordRepository, FileCache fileCache, NetworkService networkService,
                      GeomeProperties geomeProps) {
        super(expeditionService, props, projectService);
        this.geomeSql = geomeSql;
        this.queryAuthorizer = queryAuthorizer;
        this.recordRepository = recordRepository;
        this.fileCache = fileCache;
        this.networkService = networkService;
        this.geomeProps = geomeProps;
    }

    /**
     * TODO find a more suitable place for this. & Re-write *Mappers to be more robust
     *
     * @param projectId
     * @param expeditionCode
     * @return
     */
    @GET
    @Path("/{projectId}/expeditions/{expeditionCode}/generateSraFiles")
    public FileResponse generateSraFiles(@PathParam("projectId") int projectId,
                                         @PathParam("expeditionCode") String expeditionCode) {

        Project project = projectService.getProject(projectId);


        ProjectConfig config = project.getProjectConfig();

        Entity e = config.entities()
                .stream()
                .filter(FastqEntity.class::isInstance)
                .findFirst()
                .orElseThrow((Supplier<RuntimeException>) () -> new BadRequestException("Could not find FastqEntity for provided project"));


        Entity parentEntity = e;
        do {
            parentEntity = config.entity(parentEntity.getParentEntity());
        } while (parentEntity.isChildEntity());

        List<String> entities = config.getEntityRelations(parentEntity, e).stream()
                .flatMap(r -> Stream.of(r.getChildEntity().getConceptAlias(), r.getParentEntity().getConceptAlias()))
                .collect(Collectors.toList());

        Expression exp = new ExpeditionExpression(expeditionCode);
        exp = new LogicalExpression(
                LogicalOperator.AND,
                exp,
                new ProjectExpression(Collections.singletonList(projectId))
        );
        exp = new SelectExpression(
                String.join(",", entities),
                exp
        );

        QueryBuilder qb = new QueryBuilder(config, project.getNetwork().getId(), e.getConceptAlias());
        Query query = new Query(qb, config, exp);

        if (!queryAuthorizer.authorizedQuery(Collections.singletonList(projectId), new ArrayList<>(query.expeditions()), userContext.getUser())) {
            throw new FimsRuntimeException(GenericErrorCode.UNAUTHORIZED, 403);
        }

        QueryResults queryResults = recordRepository.query(query);

        if (queryResults.isEmpty()) return null;

        SraMetadataMapper metadataMapper = new GeomeSraMetadataMapper(config, e, queryResults);
        BioSampleMapper bioSampleMapper = new GeomeBioSampleMapper(config, e, queryResults, props.bcidResolverPrefix());

        File bioSampleFile = BioSampleAttributesGenerator.generateFile(bioSampleMapper);
        File sraMetadataFile = SraMetadataGenerator.generateFile(metadataMapper);

        Map<String, File> fileMap = new HashMap<>();
        fileMap.put("bioSample-attributes.tsv", bioSampleFile);
        fileMap.put("sra-metadata.tsv", sraMetadataFile);
        fileMap.put("sra-step-by-step-instructions.pdf", new File(context.getRealPath("docs/sra-step-by-step-instructions.pdf")));

        File zip = FileUtils.zip(fileMap, defaultOutputDirectory());
        String fileId = fileCache.cacheFileForUser(zip, userContext.getUser(), "sra-files.zip");

        return new FileResponse(uriInfo.getBaseUriBuilder(), fileId);
    }

    /**
     * Fetch an overview of all expeditions in a project.
     * TODO this isn't in the generated swagger docs
     *
     * @return List of json objects containing the following information for each expedition:
     * - expeditionCode
     * - expeditionTitle
     * - expeditionIdentifier
     * - **conceptAlias**Count - # of records for the entity. 1 line for each entity in the project's config.
     */
    @GET
    @Path("/{projectId}/expeditions/stats")
    public List<Map> expeditionStats(@PathParam("projectId") Integer projectId,
                                     @QueryParam("expeditionCode") String expeditionCode) {
        Project project = projectService.getProject(projectId);

        if (project == null) {
            throw new BadRequestException("Project doesn't exist");
        }

        String countsSql = geomeSql.statsEntityCounts();
        String joinsSql = geomeSql.expeditionStatsEntityJoins();
        String sql = expeditionCode == null ? geomeSql.expeditionStats() : geomeSql.singleExpeditionStats();

        StringBuilder entityJoinsSql = new StringBuilder();
        StringBuilder entityCountsSql = new StringBuilder();

        for (Entity e : project.getProjectConfig().entities()) {
            Map<String, String> p = new HashMap<>();
            p.put("table", PostgresUtils.entityTable(project.getNetwork().getId(), e.getConceptAlias()));
            p.put("entity", e.getConceptAlias());

            entityCountsSql.append(", ");
            entityCountsSql.append(StrSubstitutor.replace(countsSql, p));
            entityJoinsSql.append(StrSubstitutor.replace(joinsSql, p));
        }

        Map<String, String> params = new HashMap<>();
        params.put("entityJoins", entityJoinsSql.toString());
        params.put("entityCounts", entityCountsSql.toString());
        params.put("projectId", String.valueOf(projectId));
        params.put("includePrivate", String.valueOf(userContext.getUser() != null));

        return recordRepository.query(
                StrSubstitutor.replace(sql, params),
                expeditionCode == null ? null : new MapSqlParameterSource("expeditionCode", expeditionCode),
                Map.class
        );
    }

    /**
     * Fetch an overview of all project stats.
     *
     * @return List of projects with their stats:
     */
    @GET
    @Path("/stats")
    public List<Map> projectStats(@QueryParam("includePublic") @DefaultValue("true") Boolean includePublic) {
        Network network = networkService.getNetwork(geomeProps.networkId());

        if (!includePublic && userContext.getUser() == null) includePublic = true;

        String countsSql = geomeSql.statsEntityCounts();
        String joinsSql = geomeSql.projectStatsEntityJoins();
        String sql = geomeSql.projectStats();

        StringBuilder entityJoinsSql = new StringBuilder();
        StringBuilder entityCountsSql = new StringBuilder();

        for (Entity e : network.getNetworkConfig().entities()) {
            Map<String, String> p = new HashMap<>();
            p.put("table", PostgresUtils.entityTable(geomeProps.networkId(), e.getConceptAlias()));
            p.put("entity", e.getConceptAlias());

            entityCountsSql.append(", ");
            entityCountsSql.append(StrSubstitutor.replace(countsSql, p));
            entityJoinsSql.append(StrSubstitutor.replace(joinsSql, p));
        }

        Map<String, String> params = new HashMap<>();
        params.put("entityJoins", entityJoinsSql.toString());
        params.put("entityCounts", entityCountsSql.toString());
        params.put("includePublic", includePublic.toString());
        params.put("userId", userContext.getUser() == null ? "0" : String.valueOf(userContext.getUser().getUserId()));

        return recordRepository.query(
                StrSubstitutor.replace(sql, params),
                null,
                (rs, rowNum) -> {
                    Map<String, Object> project = new HashMap<>();
                    Map<String, Object> config = new HashMap<>();
                    Map<String, Object> user = new HashMap<>();
                    Map<String, Integer> entityStats = new HashMap<>();

                    for (Entity e : network.getNetworkConfig().entities()) {
                        String label = e.getConceptAlias() + "Count";
                        entityStats.put(label, rs.getInt(label));
                    }

                    project.put("projectId", rs.getInt("projectId"));
                    project.put("projectTitle", rs.getString("projectTitle"));
                    project.put("description", rs.getString("description"));
                    project.put("public", rs.getBoolean("public"));
                    project.put("user", user);
                    project.put("projectConfiguration", config);
                    project.put("entityStats", entityStats);

                    user.put("userId", rs.getInt("userId"));
                    user.put("username", rs.getString("username"));
                    user.put("email", rs.getString("email"));

                    config.put("id", rs.getInt("configId"));
                    config.put("name", rs.getString("configName"));
                    config.put("description", rs.getString("configDescription"));
                    config.put("networkApproved", rs.getBoolean("configNetworkApproved"));

                    return project;
                }
        );
    }
}

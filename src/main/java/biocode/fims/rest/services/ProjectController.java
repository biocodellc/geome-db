package biocode.fims.rest.services;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.application.config.GeomeProperties;
import biocode.fims.application.config.GeomeSql;
import biocode.fims.config.models.Entity;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.models.Network;
import biocode.fims.models.Project;
import biocode.fims.query.PostgresUtils;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.services.subResources.*;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.NetworkService;
import biocode.fims.service.ProjectService;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.glassfish.jersey.server.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Controller;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API endpoints for working with projects. This includes fetching details associated with projects.
 * Currently, there are no REST services for creating projects, which instead must be added to the Database
 * manually by an administrator
 *
 * @exclude
 */
@Controller
@Path("projects")
@Produces({MediaType.APPLICATION_JSON})
@Singleton
public class ProjectController extends FimsController {
    protected final GeomeSql geomeSql;
    protected final RecordRepository recordRepository;
    protected final NetworkService networkService;
    protected final GeomeProperties geomeProps;
    protected ExpeditionService expeditionService;
    protected ProjectService projectService;

    @Autowired
    ProjectController(ExpeditionService expeditionService, FimsProperties props,
                      ProjectService projectService, GeomeSql geomeSql, RecordRepository recordRepository, NetworkService networkService, GeomeProperties geomeProps) {
        super(props);
        this.expeditionService = expeditionService;
        this.projectService = projectService;
        this.geomeSql = geomeSql;
        this.recordRepository = recordRepository;
        this.networkService = networkService;
        this.geomeProps = geomeProps;
    }


    /**
     * @responseType biocode.fims.rest.services.subResources.ProjectsResource
     */
    @Path("/")
    public Class<ProjectsResource> getProjectsResource() {
        return ProjectsResource.class;
    }

    /**
     * @responseType biocode.fims.rest.services.subResources.ProjectTemplatesResource
     * @resourceTag Templates
     */
    @Path("{projectId}/templates")
    public Class<ProjectTemplatesResource> getTemplatesResource() {
        return ProjectTemplatesResource.class;
    }

    /**
     * @responseType biocode.fims.rest.services.subResources.ExpeditionsResource
     * @resourceTag Expeditions
     */
    @Path("{projectId}/expeditions")
    public Class<ExpeditionsResource> getExpeditionsResource() {
        return ExpeditionsResource.class;
    }

    /**
     * @responseType biocode.fims.rest.services.subResources.ProjectMembersResource
     * @resourceTag Members
     */
    @Path("{projectId}/members")
    public Class<ProjectMembersResource> getProjectMembersResource() {
        return ProjectMembersResource.class;
    }

    /**
     * @responseType biocode.fims.rest.services.subResources.ProjectConfigResource
     */
    @Path("/{projectId}/config")
    public Class<ProjectConfigResource> getProjectConfigResource() {
        return ProjectConfigResource.class;
    }

    /**
     * @responseType biocode.fims.rest.services.subResources.ProjectConfigurationResource
     * @resourceTag Project Configurations
     */
    @Path("/configs")
    public Class<ProjectConfigurationResource> getProjectConfigurationResource() {
        return ProjectConfigurationResource.class;
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

        return recordRepository.query (
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
                    project.put("latestDataModification", rs.getString("latestDataModification"));
                    project.put("description", rs.getString("description"));
                    project.put("public", rs.getBoolean("public"));
                    project.put("user", user);
                    project.put("principalInvestigator", rs.getString("principalInvestigator"));
                    project.put("principalInvestigatorAffiliation", rs.getString("principalInvestigatorAffiliation"));
                    project.put("projectContact", rs.getString("projectContact"));
                    project.put("projectContactEmail", rs.getString("projectContactEmail"));
                    project.put("publicationGuid", rs.getString("publicationGuid"));
                    project.put("projectDataGuid", rs.getString("projectDataGuid"));
                    project.put("recommendedCitation", rs.getString("recommendedCitation"));
                    project.put("license", rs.getString("license"));
                    project.put("discoverable", rs.getBoolean("discoverable"));

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

package biocode.fims.rest.services;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.application.config.GeomeSql;
import biocode.fims.authorizers.QueryAuthorizer;
import biocode.fims.digester.Entity;
import biocode.fims.digester.FastaEntity;
import biocode.fims.digester.FastqEntity;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.fimsExceptions.errorCodes.GenericErrorCode;
import biocode.fims.geome.sra.GeomeBioSampleMapper;
import biocode.fims.geome.sra.GeomeSraMetadataMapper;
import biocode.fims.models.Project;
import biocode.fims.models.records.RecordSet;
import biocode.fims.ncbi.sra.submission.BioSampleAttributesGenerator;
import biocode.fims.ncbi.sra.submission.BioSampleMapper;
import biocode.fims.ncbi.sra.submission.SraMetadataGenerator;
import biocode.fims.ncbi.sra.submission.SraMetadataMapper;
import biocode.fims.query.PostgresUtils;
import biocode.fims.query.QueryResults;
import biocode.fims.query.dsl.Query;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.rest.responses.FileResponse;
import biocode.fims.rest.services.BaseProjectsController;
import biocode.fims.run.Dataset;
import biocode.fims.run.DatasetBuilder;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import biocode.fims.tools.FileCache;
import biocode.fims.utils.FileUtils;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang.text.StrSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.util.*;
import java.util.function.Supplier;

/**
 * project API endpoints
 *
 * @resourceTag Projects
 */
@Controller
@Path("projects")
@Produces({MediaType.APPLICATION_JSON})
public class ProjectController extends BaseProjectsController {

    @Context
    private ServletContext context;
    private final GeomeSql geomeSql;
    private final QueryAuthorizer queryAuthorizer;
    private final RecordRepository recordRepository;
    private final FileCache fileCache;

    @Autowired
    ProjectController(ExpeditionService expeditionService, FimsProperties props, GeomeSql geomeSql,
                      ProjectService projectService, QueryAuthorizer queryAuthorizer, RecordRepository recordRepository, FileCache fileCache) {
        super(expeditionService, props, projectService);
        this.geomeSql = geomeSql;
        this.queryAuthorizer = queryAuthorizer;
        this.recordRepository = recordRepository;
        this.fileCache = fileCache;
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

        Entity e = project.getProjectConfig().entities()
                .stream()
                .filter(FastqEntity.class::isInstance)
                .findFirst()
                .orElseThrow((Supplier<RuntimeException>) () -> new BadRequestException("Could not find FastqEntity for provided project"));

        Entity parentEntity = project.getProjectConfig().entity(e.getParentEntity());

        String q = "_expeditions_:" +
                expeditionCode +
                " _select_:" +
                parentEntity.getConceptAlias();

        Query query = Query.factory(project, e.getConceptAlias(), q);

        if (!queryAuthorizer.authorizedQuery(Collections.singletonList(projectId), new ArrayList<>(query.expeditions()), userContext.getUser())) {
            throw new FimsRuntimeException(GenericErrorCode.UNAUTHORIZED, 403);
        }

        QueryResults queryResults = recordRepository.query(query);

        if (queryResults.isEmpty()) return null;

        RecordSet parentRecordSet = new RecordSet(parentEntity, queryResults.getResult(parentEntity.getConceptAlias()).records(), false);
        RecordSet recordSet = new RecordSet(e, queryResults.getResult(e.getConceptAlias()).records(), false);
        recordSet.setParent(parentRecordSet);

        SraMetadataMapper metadataMapper = new GeomeSraMetadataMapper(queryResults.getResult(e.getConceptAlias()), queryResults.getResult(parentEntity.getConceptAlias()));
        BioSampleMapper bioSampleMapper = new GeomeBioSampleMapper(queryResults.getResult(e.getConceptAlias()), queryResults.getResult(parentEntity.getConceptAlias()));

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
	 *  TODO this isn't in the generated swagger docs
     *
     * @return List of json objects containing the following information for each expedition:
     * - expeditionCode
     * - expeditionTitle
     * - expeditionIdentifier
     * - **conceptAlias**Count - # of records for the entity. 1 line for each entity in the project's config.
     */
    @GET
    @Path("/{projectId}/expeditions/stats")
    public List<Map> expeditionStats(@PathParam("projectId") Integer projectId) {
        Project project = projectService.getProject(projectId);

        if (project == null) {
            throw new BadRequestException("Project doesn't exist");
        }

        String countsSql = geomeSql.expeditionStatsEntityCounts();
        String joinsSql = geomeSql.expeditionStatsEntityJoins();
        String sql = geomeSql.expeditionStats();

        StringBuilder entityJoinsSql = new StringBuilder();
        StringBuilder entityCountsSql = new StringBuilder();

        for (Entity e : project.getProjectConfig().entities()) {
            Map<String, String> p = new HashMap<>();
            p.put("table", PostgresUtils.entityTable(projectId, e.getConceptAlias()));
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
                new HashMap<>(),
                Map.class
        );
    }
}

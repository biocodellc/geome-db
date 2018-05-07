package biocode.fims.rest.services;

import biocode.fims.application.config.FimsProperties;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * project API endpoints
 *
 * @resourceTag Projects
 */
@Controller
@Path("projects")
public class ProjectController extends BaseProjectsController {

    @Context
    private ServletContext context;
    private final QueryAuthorizer queryAuthorizer;
    private final RecordRepository recordRepository;
    private final FileCache fileCache;

    @Autowired
    ProjectController(ExpeditionService expeditionService, FimsProperties props,
                      ProjectService projectService, QueryAuthorizer queryAuthorizer, RecordRepository recordRepository, FileCache fileCache) {
        super(expeditionService, props, projectService);
        this.queryAuthorizer = queryAuthorizer;
        this.recordRepository = recordRepository;
        this.fileCache = fileCache;
    }

    /**
     * TODO find a more suitable place for this. & Re-write *Mappers to be more robust
     * @param projectId
     * @param expeditionCode
     * @return
     */
    @GET
    @Path("/{projectId}/expeditions/{expeditionCode}/generateSraFiles")
    @Produces("application/json")
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

//    /**
//     * Return a JSON representation of the expedition's that a user is a member of. Each expedition
//     * includes information regarding the latest dataset and the number of identifiers and
//     * sequences in the dataset.
//     *
//     * @param projectId
//     * @return
//     */
//    @GET
//    @Path("/{projectId}/expeditions/stats")
//    @Produces({MediaType.APPLICATION_JSON})
//    public Response listExpeditionsWithLatestDatasets(@PathParam("projectId") Integer projectId) {
//        // TODO using terms aggregation may not be accurate, since documents are stored across shards
//        // possibly need to store the sequence count as a field and them sum that
//        // also, aggs can't use pagination, so we are fetching 1000 expeditions. to support pagination, we need to query
//        // the expeditionCodes
//        Project project = projectService.getProject(projectId);
//
//        if (project == null) {
//            throw new BadRequestException("Project doesn't exist");
//        }
//
//        AggregationBuilder aggsBuilder = AggregationBuilders.terms("expeditions").field("expedition.expeditionCode.keyword")
//                .subAggregation(
//                        AggregationBuilders.nested("fastaSequenceCount", "fastaSequence")
//                )
//                .subAggregation(
//                        AggregationBuilders.filter("fastqMetadataCount",
//                                QueryBuilders.existsQuery(FastqFileManager.CONCEPT_ALIAS))
//                )
//                .order(Terms.Order.term(true))
//                .size(1000);
//
//        SearchResponse response = esClient.prepareSearch(String.valueOf(projectId))
//                .setTypes(ElasticSearchIndexer.TYPE)
//                .setSize(0)
//                .addAggregation(aggsBuilder).get();
//
//        Terms terms = response.getAggregations().get("expeditions");
//        List<ObjectNode> buckets = new ArrayList<>();
//
//        List<Expedition> expeditions = expeditionService.getExpeditions(projectId, false);
//
//        for (Terms.Bucket bucket : terms.getBuckets()) {
//            Expedition expedition = expeditions.stream()
//                    .filter(e -> e.getExpeditionCode().equals(bucket.getKeyAsString()))
//                    .findFirst()
//                    .get();
//
//            ObjectNode b = new SpringObjectMapper().createObjectNode();
//            b.put("resourceCount", bucket.getDocCount());
//            b.put("expeditionCode", String.valueOf(bucket.getKey()));
//            b.put("expeditionTitle", expedition != null ? expedition.getExpeditionTitle() : "");
//            b.put("expeditionIdentifier", expedition != null ? String.valueOf(expedition.getExpeditionBcid().getIdentifier()) : "");
//            b.put("fastaSequenceCount", ((Nested) bucket.getAggregations().get("fastaSequenceCount")).getDocCount());
//            b.put("fastqMetadataCount", ((InternalFilter) bucket.getAggregations().get("fastqMetadataCount")).getDocCount());
//
//            buckets.add(b);
//        }
//
//        return Response.ok(buckets).build();
//    }
}

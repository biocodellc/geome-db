package biocode.fims.rest.services;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.rest.services.BaseProjectsController;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;

/**
 * project API endpoints
 *
 * @resourceTag Projects
 */
@Controller
@Path("projects")
public class ProjectController extends BaseProjectsController {

    @Autowired
    ProjectController(ExpeditionService expeditionService, FimsProperties props,
                      ProjectService projectService) {
        super(expeditionService, props, projectService);
    }

//    @GET
//    @Path("/{projectId}/expeditions/{expeditionCode}/generateSraFiles")
//    @Produces("application/zip")
//    public Response generateSraFiles(@PathParam("projectId") int projectId,
//                                     @PathParam("expeditionCode") String expeditionCode) {
//
//        File configFile = new ConfigurationFileFetcher(projectId, defaultOutputDirectory(), true).getOutputFile();
//
//        Mapping mapping = new Mapping();
//        mapping.addMappingRules(configFile);
//
//        ProcessController processController = new ProcessController(projectId, expeditionCode);
//        processController.setOutputFolder(defaultOutputDirectory());
//        processController.setMapping(mapping);
//        fimsMetadataFileManager.setProcessController(processController);
//        ArrayNode dataset = fimsMetadataFileManager.getDataset();
//
//        SraMetadataMapper metadataMapper = new GeomeSraMetadataMapper(dataset);
//        BioSampleMapper bioSampleMapper = new GeomeBioSampleMapper(dataset);
//
//        File bioSampleFile = BioSampleAttributesGenerator.generateFile(bioSampleMapper, defaultOutputDirectory());
//        File sraMetadataFile = SraMetadataGenerator.generateFile(metadataMapper, defaultOutputDirectory());
//
//        Map<String, File> fileMap = new HashMap<>();
//        fileMap.put("bioSample-attributes.tsv", bioSampleFile);
//        fileMap.put("sra-metadata.tsv", sraMetadataFile);
//        fileMap.put("sra-step-by-step-instructions.pdf", new File(context.getRealPath("docs/sra-step-by-step-instructions.pdf")));
//
//        Response.ResponseBuilder response = Response.ok(FileUtils.zip(fileMap, defaultOutputDirectory()), "application/zip");
//        response.header("Content-Disposition",
//                "attachment; filename=sra-files.zip");
//
//        return response.build();
//    }

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

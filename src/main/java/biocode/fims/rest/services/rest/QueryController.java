package biocode.fims.rest.services.rest;

import biocode.fims.authorizers.QueryAuthorizer;
import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.Mapping;
import biocode.fims.dipnet.query.DipnetQueryUtils;
import biocode.fims.elasticSearch.ElasticSearchIndexer;
import biocode.fims.elasticSearch.query.ElasticSearchFilterCondition;
import biocode.fims.elasticSearch.query.ElasticSearchFilterField;
import biocode.fims.elasticSearch.query.ElasticSearchQuery;
import biocode.fims.elasticSearch.query.ElasticSearchQuerier;
import biocode.fims.fasta.FastaJsonWriter;
import biocode.fims.fasta.FastaSequenceJsonFieldFilter;
import biocode.fims.fastq.fileManagers.FastqFileManager;
import biocode.fims.fimsExceptions.*;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.errorCodes.QueryErrorCode;
import biocode.fims.query.*;
import biocode.fims.rest.FimsService;
import biocode.fims.run.TemplateProcessor;
import biocode.fims.service.ExpeditionService;
import biocode.fims.settings.SettingsManager;
import biocode.fims.utils.FileUtils;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.lucene.search.join.ScoreMode;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.*;

/**
 * Query interface for Biocode-fims expedition
 */
@Controller
@Path("/projects/query")
public class QueryController extends FimsService {
    private static final Logger logger = LoggerFactory.getLogger(QueryController.class);

    private static Integer projectId = Integer.parseInt(SettingsManager.getInstance().retrieveValue("projectId"));
    private Mapping mapping;

    private final Client esClient;
    private final QueryAuthorizer queryAuthorizer;
    private final ExpeditionService expeditionService;

    @Autowired
    QueryController(SettingsManager settingsManager, Client esClient, QueryAuthorizer queryAuthorizer,
                    ExpeditionService expeditionService) {
        super(settingsManager);
        this.esClient = esClient;
        this.queryAuthorizer = queryAuthorizer;
        this.expeditionService = expeditionService;
    }

    /**
     * accepts an elastic json query request. note that aggregations are not supported, and the json query object needs
     * to exclude the initial {"query": } that you would send via the elasticsearch rest api
     *
     * @param page
     * @param limit
     * @param esQueryString
     * @return
     */
    @POST
    @Path("/es")
    @Produces(MediaType.APPLICATION_JSON)
    public Response queryElasticSearch(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("limit") @DefaultValue("100") int limit,
            ObjectNode esQueryString) {

        if (!queryAuthorizer.authorizedQuery(Arrays.asList(projectId), esQueryString, userContext.getUser())) {
            throw new ForbiddenRequestException("unauthorized query");
        }

        ElasticSearchQuery query = new ElasticSearchQuery(
                QueryBuilders.wrapperQuery(esQueryString.toString()),
                new String[]{String.valueOf(projectId)},
                new String[]{ElasticSearchIndexer.TYPE}
        );

        return getJsonResults(page, limit, query);
    }

    private Response getJsonResults(int page, int limit, ElasticSearchQuery query) {
        Pageable pageable = new PageRequest(page, limit);

        query.pageable(pageable);

        ElasticSearchQuerier elasticSearchQuerier = new ElasticSearchQuerier(esClient, query);

        Page<ObjectNode> results = elasticSearchQuerier.getPageableResults();

        List<JsonFieldTransform> writerColumns = DipnetQueryUtils.getJsonFieldTransforms(getMapping());
        writerColumns.add(
                new JsonFieldTransform("fastqMetadata", JsonPointer.compile("/" + FastqFileManager.CONCEPT_ALIAS), null)
        );
        Page<ObjectNode> transformedResults = results.map(r -> JsonTransformer.transform(r, writerColumns));

        return Response.ok(transformedResults).build();
    }

    /**
     * Return JSON for a graph query as POST
     * <p/>
     * filter parameters are of the form:
     * name={URI} value={filter value}
     *
     * @return
     */
    @POST
    @Path("/json/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response queryJsonAsPOST(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("limit") @DefaultValue("100") int limit,
            MultivaluedMap<String, String> form) {

        // Build the query, etc..
        try {
            ElasticSearchQuery query = POSTElasticSearchQuery(form);
            return getJsonResults(page, limit, query);
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryErrorCode.NO_RESOURCES) {
                return Response.ok(
                        new PageImpl<String>(null, new PageRequest(page, limit), 0)
                ).build();
            }

            throw e;
        }
    }

    /**
     * Return CSV for a graph query as POST
     * <p/>
     * filter parameters are of the form:
     * name={URI} value={filter value}
     *
     * @return
     */
    @POST
    @Path("/csv/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("text/csv")
    public Response queryCSVAsPOST(
            MultivaluedMap<String, String> form) {

        try {
            // Build the query, etc..
            ElasticSearchQuery query = POSTElasticSearchQuery(form);

            ElasticSearchQuerier elasticSearchQuerier = new ElasticSearchQuerier(esClient, query);

            ArrayNode results = elasticSearchQuerier.getAllResults();

            JsonWriter jsonWriter = new DelimitedTextJsonWriter(results, DipnetQueryUtils.getJsonFieldTransforms(getMapping()), defaultOutputDirectory(), ",");

            Response.ResponseBuilder response = Response.ok(jsonWriter.write());

            response.header("Content-Disposition",
                    "attachment; filename=dipnet-fims-output.csv");

            return response.build();
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryErrorCode.NO_RESOURCES) {
                return Response.noContent().build();
            }

            throw e;
        }
    }

    /**
     * Return KML for a graph query using POST
     * * <p/>
     * filter parameters are of the form:
     * name={URI} value={filter value}
     *
     * @return
     */
    @POST
    @Path("/kml/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/vnd.google-earth.kml+xml")
    public Response queryKml(
            MultivaluedMap<String, String> form) {

        try {
            // Build the query, etc..
            ElasticSearchQuery query = POSTElasticSearchQuery(form);

            ElasticSearchQuerier elasticSearchQuerier = new ElasticSearchQuerier(esClient, query);

            ArrayNode results = elasticSearchQuerier.getAllResults();

            JsonWriter jsonWriter = new KmlJsonWriter.KmlJsonWriterBuilder(results, defaultOutputDirectory(), DipnetQueryUtils.getJsonFieldTransforms(getMapping()))
                    .latPath(DipnetQueryUtils.getLatitudePointer())
                    .longPath(DipnetQueryUtils.getLongitudePointer())
                    .namePath(DipnetQueryUtils.getUniqueKeyPointer())
                    .build();

            Response.ResponseBuilder response = Response.ok(jsonWriter.write());

            response.header("Content-Disposition",
                    "attachment; filename=dipnet-fims-output.kml");

            // Return response
            return response.build();
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryErrorCode.NO_RESOURCES) {
                return Response.noContent().build();
            }

            throw e;
        }
    }

    @POST
    @Path("/fasta/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/zip")
    public Response queryFasta(
            MultivaluedMap<String, String> form) {

        try {
            // Build the query, etc..
            ElasticSearchQuery query = POSTElasticSearchQuery(form);

            ElasticSearchQuerier elasticSearchQuerier = new ElasticSearchQuerier(esClient, query);

            ArrayNode results = elasticSearchQuerier.getAllResults();

            JsonWriter metadataJsonWriter = new DelimitedTextJsonWriter(
                    results,
                    DipnetQueryUtils.getJsonFieldTransforms(getMapping()),
                    defaultOutputDirectory(),
                    ","
            );

            File metadataFile = metadataJsonWriter.write();

            List<FastaSequenceJsonFieldFilter> fastaSequenceFilters = getFastaSequenceFilters(form);

            JsonWriter fastaJsonWriter = new FastaJsonWriter(
                    results,
                    DipnetQueryUtils.getFastaSequenceFields(getMapping()),
                    defaultOutputDirectory(),
                    fastaSequenceFilters);

            File fastaFile = fastaJsonWriter.write();

            Map<String, File> fileMap = new HashMap<>();
            fileMap.put("dipnet-fims-output.csv", metadataFile);

            if (fastaFile.getName().endsWith(".zip")) {
                fileMap.put("dipnet-fims-output-fasta.zip", fastaFile);
            } else {
                fileMap.put("dipnet-fims-output.fasta", fastaFile);
            }

            Response.ResponseBuilder response = Response.ok(FileUtils.zip(fileMap, defaultOutputDirectory()), "application/zip");

            response.header("Content-Disposition",
                    "attachment; filename=dipnet-fims-output.zip");

            // Return response
            if (response == null) {
                return Response.status(204).build();
            } else {
                return response.build();
            }
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryErrorCode.NO_RESOURCES) {
                return Response.noContent().build();
            }

            throw e;
        }
    }

    @POST
    @Path("/fastq/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/zip")
    public Response queryFastq(
            MultivaluedMap<String, String> form) {

        try {
            // Build the query, etc..
            ElasticSearchQuery query = POSTElasticSearchQuery(form);

            ElasticSearchQuerier elasticSearchQuerier = new ElasticSearchQuerier(esClient, query);

            ArrayNode results = elasticSearchQuerier.getAllResults();

            JsonWriter jsonWriter = new DelimitedTextJsonWriter(
                    results,
                    DipnetQueryUtils.getFastqJsonFieldTransforms(getMapping()),
                    defaultOutputDirectory(),
                    ","
            );

            Response.ResponseBuilder response = Response.ok(jsonWriter.write());

            response.header("Content-Disposition",
                    "attachment; filename=dipnet-fims-output-including-fastq-metadata.csv");

            // Return response
            return response.build();
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryErrorCode.NO_RESOURCES) {
                return Response.noContent().build();
            }

            throw e;
        }
    }

    /**
     * Return Excel for a graph query using POST
     * * <p/>
     * filter parameters are of the form:
     * name={URI} value={filter value}
     *
     * @return
     */
    @POST
    @Path("/excel/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/vnd.ms-excel")
    public Response queryExcel(
            MultivaluedMap<String, String> form) {

        if (!form.containsKey("expeditions") || form.get("expeditions").size() != 1) {
            throw new BadRequestException("Invalid Arguments. Only 1 expedition can be specified");
        }
        // Build the query, etc..
        ElasticSearchQuery query = POSTElasticSearchQuery(form);

        ElasticSearchQuerier elasticSearchQuerier = new ElasticSearchQuerier(esClient, query);

        ArrayNode results = elasticSearchQuerier.getAllResults();

        JsonWriter jsonWriter = new ExcelJsonWriter(results, DipnetQueryUtils.getJsonFieldTransforms(getMapping()), getMapping().getDefaultSheetName(), defaultOutputDirectory());

        File file = jsonWriter.write();

        // Here we attach the other components of the excel sheet found with
        XSSFWorkbook justData = null;
        try {
            justData = new XSSFWorkbook(new FileInputStream(file));
        } catch (IOException e) {
            logger.error("failed to open excel file", e);
        }

        TemplateProcessor t = new TemplateProcessor(projectId, defaultOutputDirectory(), justData);
        file = t.createExcelFileFromExistingSources("Samples", defaultOutputDirectory());
        Response.ResponseBuilder response = Response.ok(file);

        response.header("Content-Disposition",
                "attachment; filename=dipnet-fims-output.xlsx");

        // Return response
        if (response == null) {
            return Response.status(204).build();
        } else {
            return response.build();
        }
    }

    /**
     * Get the POST query result as a JSONArray
     *
     * @return
     */
    private ElasticSearchQuery POSTElasticSearchQuery(MultivaluedMap<String, String> form) {
        List<String> expeditionCodes = new ArrayList<>();
        List<ElasticSearchFilterCondition> filterConditions = new ArrayList<>();

        // remove the projectId if present
        form.remove("projectId");

        if (form.containsKey("expeditions")) {
            expeditionCodes.addAll(form.remove("expeditions"));
        }
        if (form.containsKey("expeditions[]")) {
            expeditionCodes.addAll(form.remove("expeditions[]"));
        }

        if (!queryAuthorizer.authorizedQuery(Collections.singletonList(projectId), expeditionCodes, userContext.getUser())) {
            throw new ForbiddenRequestException("unauthorized query.");
        }

        List<ElasticSearchFilterField> filterFields = DipnetQueryUtils.getAvailableFilters(getMapping());
        filterFields.add(DipnetQueryUtils.get_AllFilter());

        for (Map.Entry<String, List<String>> entry : form.entrySet()) {

            // only expect 1 value
            ElasticSearchFilterCondition filterCondition = new ElasticSearchFilterCondition(
                    lookupFilter(entry.getKey(), filterFields),
                    entry.getValue().get(0));

            filterConditions.add(filterCondition);
        }

        // if no expeditions are specified, then we want to only query public expeditions
        if (expeditionCodes.size() == 0) {
            expeditionService.getPublicExpeditions(projectId).forEach(e -> expeditionCodes.add(e.getExpeditionCode()));

            if (expeditionCodes.size() == 0) {
                throw new FimsRuntimeException(QueryErrorCode.NO_RESOURCES, 204);
            }
        }

        return new ElasticSearchQuery(
                getQueryBuilder(filterConditions, expeditionCodes),
                new String[]{String.valueOf(projectId)},
                new String[]{ElasticSearchIndexer.TYPE}
        );
    }

    private List<FastaSequenceJsonFieldFilter> getFastaSequenceFilters(MultivaluedMap<String, String> form) {
        List<FastaSequenceJsonFieldFilter> fastaSequenceFilters = new ArrayList<>();

        List<ElasticSearchFilterField> fastaFilterFields = DipnetQueryUtils.getFastaFilters(getMapping());

        for (Map.Entry<String, List<String>> entry : form.entrySet()) {

            try {
                ElasticSearchFilterField filterField = lookupFilter(entry.getKey(), fastaFilterFields);

                // fastaSequences are nested objects. the filterField getField would look like {nestedPath}.{attribute_uri}
                // since fastaSequenceFilters only filter fastaSequence objects, we need to remove the nestedPath
                String fastaSequenceFilterPath = filterField.getField().replace(filterField.getPath() + ".", "");

                fastaSequenceFilters.add(
                        new FastaSequenceJsonFieldFilter(
                                fastaSequenceFilterPath,
                                entry.getValue().get(0))
                );

            } catch (FimsRuntimeException e) {
                if (e.getErrorCode().equals(QueryErrorCode.UNKNOWN_FILTER)) {
                    // ignore
                } else {
                    throw e;
                }
            }

        }

        return fastaSequenceFilters;
    }

    private ElasticSearchFilterField lookupFilter(String key, List<ElasticSearchFilterField> filters) {
        // if field doesn't contain a ":", then we assume this is the filter displayName
        boolean isDisplayName = !key.contains(":");

        for (ElasticSearchFilterField filter : filters) {
            if (isDisplayName && key.equals(filter.getDisplayName())) {
                return filter;
            } else if (key.equals(filter.getField())) {
                return filter;
            }
        }

        throw new FimsRuntimeException(QueryErrorCode.UNKNOWN_FILTER, "is " + key + " a filterable field?", 400, key);
    }

    private QueryBuilder getQueryBuilder(List<ElasticSearchFilterCondition> filterConditions, List<String> expeditionCodes) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        for (String expedition : expeditionCodes) {
            // query the keyword sub-field for an exact match
            boolQueryBuilder.should(QueryBuilders.matchQuery("expedition.expeditionCode.keyword", expedition));
            boolQueryBuilder.minimumNumberShouldMatch(1);
        }

        for (ElasticSearchFilterCondition filterCondition : filterConditions) {

            if (filterCondition.isNested()) {
                boolQueryBuilder.must(
                        QueryBuilders.nestedQuery(
                                filterCondition.getPath(),
                                QueryBuilders.matchQuery(filterCondition.getField(), filterCondition.getValue()),
                                ScoreMode.None)
                );
            } else {
                boolQueryBuilder.must(QueryBuilders.matchQuery(filterCondition.getField(), filterCondition.getValue()));
            }
        }

        return boolQueryBuilder;
    }

    private Mapping getMapping() {
        if (mapping != null) {
            return mapping;
        }

        File configFile = new ConfigurationFileFetcher(projectId, defaultOutputDirectory(), true).getOutputFile();

        mapping = new Mapping();
        mapping.addMappingRules(configFile);

        return mapping;
    }
}


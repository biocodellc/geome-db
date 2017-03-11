package biocode.fims.rest.services.rest;

import biocode.fims.authorizers.QueryAuthorizer;
import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.Mapping;
import biocode.fims.geome.query.GeomeQueryUtils;
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
import biocode.fims.query.Query;
import biocode.fims.query.QueryCriteria;
import biocode.fims.query.writers.*;
import biocode.fims.rest.Compress;
import biocode.fims.rest.FimsService;
import biocode.fims.run.TemplateProcessor;
import biocode.fims.service.ExpeditionService;
import biocode.fims.settings.SettingsManager;
import biocode.fims.tools.CachedFile;
import biocode.fims.tools.FileCache;
import biocode.fims.utils.FileUtils;
import biocode.fims.utils.StringGenerator;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

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
    private final FileCache fileCache;

    @Autowired
    QueryController(SettingsManager settingsManager, Client esClient, QueryAuthorizer queryAuthorizer,
                    ExpeditionService expeditionService, FileCache fileCache) {
        super(settingsManager);
        this.esClient = esClient;
        this.queryAuthorizer = queryAuthorizer;
        this.expeditionService = expeditionService;
        this.fileCache = fileCache;
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

        List<JsonFieldTransform> writerColumns = GeomeQueryUtils.getJsonFieldTransforms(getMapping());
        writerColumns.add(
                new JsonFieldTransform("fastqMetadata", FastqFileManager.CONCEPT_ALIAS, null)
        );

        List<JsonFieldTransform> filteredWriterColumns = writerColumns.stream()
                .filter(t -> query.getSource().contains(t.getUri()))
                .collect(Collectors.toList());

        Page<ObjectNode> transformedResults = results.map(r -> JsonTransformer.transform(r, filteredWriterColumns));

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
    @Compress
    @POST
    @Path("/json/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response queryJsonAsPOST(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("limit") @DefaultValue("100") int limit,
            Query query) {

        // Build the query, etc..
        try {
            ElasticSearchQuery esQuery = getEsQuery(query);

            if (query.getSource().size() > 0) {
                esQuery.source(query.getSource());
            }

            return getJsonResults(page, limit, esQuery);
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response queryCSVAsPOST(Query query) {

        try {
            // Build the query, etc..
            ElasticSearchQuerier elasticSearchQuerier = new ElasticSearchQuerier(esClient, getEsQuery(query));

            ArrayNode results = elasticSearchQuerier.getAllResults();

            JsonWriter jsonWriter = new DelimitedTextJsonWriter(results, GeomeQueryUtils.getJsonFieldTransforms(getMapping()), defaultOutputDirectory(), ",");

            return returnFileResults(jsonWriter.write(), "geome-fims-output.csv");
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
    public Response queryKml(Query query) {

        try {
            // Build the query, etc..
            ElasticSearchQuerier elasticSearchQuerier = new ElasticSearchQuerier(esClient, getEsQuery(query));

            ArrayNode results = elasticSearchQuerier.getAllResults();

            JsonWriter jsonWriter = new KmlJsonWriter.KmlJsonWriterBuilder(results, defaultOutputDirectory(), GeomeQueryUtils.getJsonFieldTransforms(getMapping()))
                    .latPath(GeomeQueryUtils.getLatitudePointer())
                    .longPath(GeomeQueryUtils.getLongitudePointer())
                    .namePath(GeomeQueryUtils.getUniqueKeyPointer())
                    .build();

            return returnFileResults(jsonWriter.write(), "geome-fims-output.kml");
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
    public Response queryFasta(Query query) {

        try {
            // Build the query, etc..
            ElasticSearchQuerier elasticSearchQuerier = new ElasticSearchQuerier(esClient, getEsQuery(query));

            ArrayNode results = elasticSearchQuerier.getAllResults();

            JsonWriter metadataJsonWriter = new DelimitedTextJsonWriter(
                    results,
                    GeomeQueryUtils.getJsonFieldTransforms(getMapping()),
                    defaultOutputDirectory(),
                    ","
            );

            File metadataFile = metadataJsonWriter.write();

            List<FastaSequenceJsonFieldFilter> fastaSequenceFilters = getFastaSequenceFilters(query.getCriterion());

            JsonWriter fastaJsonWriter = new FastaJsonWriter(
                    results,
                    GeomeQueryUtils.getFastaSequenceFields(getMapping()),
                    defaultOutputDirectory(),
                    fastaSequenceFilters);

            File fastaFile = fastaJsonWriter.write();

            Map<String, File> fileMap = new HashMap<>();
            fileMap.put("geome-db-output.csv", metadataFile);

            if (fastaFile.getName().endsWith(".zip")) {
                fileMap.put("geome-db-output-fasta.zip", fastaFile);
            } else {
                fileMap.put("geome-db-output.fasta", fastaFile);
            }

            return returnFileResults(FileUtils.zip(fileMap, defaultOutputDirectory()), "geome-fims-output.zip");
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
    public Response queryFastq(Query query) {

        try {
            // Build the query, etc..
            ElasticSearchQuerier elasticSearchQuerier = new ElasticSearchQuerier(esClient, getEsQuery(query));

            ArrayNode results = elasticSearchQuerier.getAllResults();

            JsonWriter jsonWriter = new DelimitedTextJsonWriter(
                    results,
                    GeomeQueryUtils.getFastqJsonFieldTransforms(getMapping()),
                    defaultOutputDirectory(),
                    ","
            );

            return returnFileResults(jsonWriter.write(), "geome-fims-output-including-fastq-metadata.csv");
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
    public Response queryExcel(Query query) {

        if (query.getExpeditions().size() != 1) {
            throw new BadRequestException("Invalid Arguments. Only 1 expedition can be specified");
        }
        try {
            // Build the query, etc..
            ElasticSearchQuerier elasticSearchQuerier = new ElasticSearchQuerier(esClient, getEsQuery(query));

            ArrayNode results = elasticSearchQuerier.getAllResults();

            JsonWriter jsonWriter = new ExcelJsonWriter(results, GeomeQueryUtils.getJsonFieldTransforms(getMapping()), getMapping().getDefaultSheetName(), defaultOutputDirectory());

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

            return returnFileResults(file, "geome-fims-output.xlsx");
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryErrorCode.NO_RESOURCES) {
                return Response.noContent().build();
            }

            throw e;
        }

    }

    private Response returnFileResults(File file, String name) {
        int userId = userContext.getUser() != null ? userContext.getUser().getUserId() : 0;
        String fileId = StringGenerator.generateString(20);
        CachedFile cf = new CachedFile(fileId, file.getAbsolutePath(), userId, name);
        fileCache.addFile(cf);

        URI fileURI = uriInfo.getBaseUriBuilder().path(UtilsController.class).path("file").queryParam("id", fileId).build();

        return Response.ok("{\"url\": \"" + fileURI + "\"}").build();
    }


    private ElasticSearchQuery getEsQuery(Query query) {
        List<ElasticSearchFilterCondition> filterConditions = new ArrayList<>();

        List<ElasticSearchFilterField> filterFields = GeomeQueryUtils.getAvailableFilters(getMapping());
        filterFields.add(GeomeQueryUtils.get_AllFilter());

        if (!queryAuthorizer.authorizedQuery(Collections.singletonList(projectId), query.getExpeditions(), userContext.getUser())) {
            throw new ForbiddenRequestException("unauthorized query.");
        }

        // if no expeditions are specified, then we want to only query public expeditions
        if (query.getExpeditions().size() == 0) {
            query.getExpeditions().addAll(getPublicExpeditions());
        }

        for (QueryCriteria criteria : query.getCriterion()) {
            filterConditions.add(
                    new ElasticSearchFilterCondition(
                            lookupFilter(criteria.getKey(), filterFields),
                            criteria.getValue(),
                            criteria.getType()
                    )
            );
        }

        return new ElasticSearchQuery(
                getQueryBuilder(filterConditions, query.getExpeditions()),
                new String[]{String.valueOf(projectId)},
                new String[]{ElasticSearchIndexer.TYPE}
        );
    }

    private List<String> getPublicExpeditions() {
        List<String> expeditionCodes = new ArrayList<>();
        expeditionService.getPublicExpeditions(projectId).forEach(e -> expeditionCodes.add(e.getExpeditionCode()));

        if (expeditionCodes.size() == 0) {
            throw new FimsRuntimeException(QueryErrorCode.NO_RESOURCES, 204);
        }
        return expeditionCodes;
    }

    private List<FastaSequenceJsonFieldFilter> getFastaSequenceFilters(List<QueryCriteria> criterion) {
        List<FastaSequenceJsonFieldFilter> fastaSequenceFilters = new ArrayList<>();

        List<ElasticSearchFilterField> fastaFilterFields = GeomeQueryUtils.getFastaFilters(getMapping());

        for (QueryCriteria criteria : criterion) {

            try {
                ElasticSearchFilterField filterField = lookupFilter(criteria.getKey(), fastaFilterFields);

                // fastaSequences are nested objects. the filterField getField would look like {nestedPath}.{attribute_uri}
                // since fastaSequenceFilters only filter fastaSequence objects, we need to remove the nestedPath
                String fastaSequenceFilterPath = filterField.getField().replace(filterField.getPath() + ".", "");

                fastaSequenceFilters.add(
                        new FastaSequenceJsonFieldFilter(
                                fastaSequenceFilterPath,
                                criteria.getValue())
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
            boolQueryBuilder.must(filterCondition.getQuery());
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


package biocode.fims.rest.services.rest;

import biocode.fims.authorizers.QueryAuthorizer;
import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.Attribute;
import biocode.fims.digester.Mapping;
import biocode.fims.digester.Validation;
import biocode.fims.biscicol.query.BiscicolQueryUtils;
import biocode.fims.elasticSearch.ElasticSearchIndexer;
import biocode.fims.elasticSearch.query.ElasticSearchFilterCondition;
import biocode.fims.elasticSearch.query.ElasticSearchFilterField;
import biocode.fims.elasticSearch.query.ElasticSearchQuerier;
import biocode.fims.elasticSearch.query.ElasticSearchQuery;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.fimsExceptions.errorCodes.QueryErrorCode;
import biocode.fims.query.*;
import biocode.fims.rest.FimsService;
import biocode.fims.run.TemplateProcessor;
import biocode.fims.service.ExpeditionService;
import biocode.fims.settings.SettingsManager;
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
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

/**
 * Query interface for Biocode-fims expedition
 */
@Scope("request")
@Controller
@Path("/projects/query")
public class QueryController extends FimsService {
    private static final Logger logger = LoggerFactory.getLogger(QueryController.class);

    private Integer projectId;
    private Mapping mapping;
    private File configFile;

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

        List<JsonFieldTransform> writerColumns = BiscicolQueryUtils.getJsonFieldTransforms(getMapping(projectId));
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

        try {
            // Build the query, etc..
            ElasticSearchQuery query = POSTElasticSearchQuery(form);

            return getJsonResults(page, limit, query);
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryErrorCode.NO_RESOURCES) {
                return Response.noContent().build();
            }

            throw e;
        }
    }

    /**
     * Return JSON for a graph query.
     */
    @GET
    @Path("/json/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response queryJson(
            @QueryParam("expeditions") List<String> expeditions,
            @QueryParam("projectId") Integer projectId,
            @QueryParam("filter") String filter,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("limit") @DefaultValue("100") int limit) {

        try {
            ElasticSearchQuery query = GETElasticSearchQuery(projectId, expeditions, filter);

            return getJsonResults(page, limit, query);
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryErrorCode.NO_RESOURCES) {
                return Response.noContent().build();
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

            JsonWriter jsonWriter = new DelimitedTextJsonWriter(results, BiscicolQueryUtils.getJsonFieldTransforms(getMapping(projectId)), uploadPath(), ",");

            Response.ResponseBuilder response = Response.ok(jsonWriter.write());

            response.header("Content-Disposition",
                    "attachment; filename=biocode-fims-output.csv");

            return response.build();
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryErrorCode.NO_RESOURCES) {
                return Response.noContent().build();
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
    @GET
    @Path("/csv/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("text/csv")
    public Response queryCSV(
            @QueryParam("expeditions") List<String> expeditions,
            @QueryParam("projectId") Integer projectId,
            @QueryParam("filter") String filter) {

        try {
            // Build the query, etc..
            ElasticSearchQuery query = GETElasticSearchQuery(projectId, expeditions, filter);

            ElasticSearchQuerier elasticSearchQuerier = new ElasticSearchQuerier(esClient, query);

            ArrayNode results = elasticSearchQuerier.getAllResults();

            JsonWriter jsonWriter = new DelimitedTextJsonWriter(results, BiscicolQueryUtils.getJsonFieldTransforms(getMapping(projectId)), uploadPath(), ",");

            Response.ResponseBuilder response = Response.ok(jsonWriter.write());

            response.header("Content-Disposition",
                    "attachment; filename=biocode-fims-output.csv");

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

            Mapping mapping = getMapping(projectId);

            JsonWriter jsonWriter = new KmlJsonWriter.KmlJsonWriterBuilder(results, uploadPath(), BiscicolQueryUtils.getJsonFieldTransforms(mapping))
                    .latPath(BiscicolQueryUtils.getLatitudePointer(mapping))
                    .longPath(BiscicolQueryUtils.getLongitudePointer(mapping))
                    .namePath(BiscicolQueryUtils.getUniqueKeyPointer(mapping))
                    .build();

            Response.ResponseBuilder response = Response.ok(jsonWriter.write());

            response.header("Content-Disposition",
                    "attachment; filename=biocode-fims-output.kml");

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

    /**
     * Return KML for a graph query using POST
     * filter is just a single value to filter the entire dataset
     *
     * @return
     */
    @GET
    @Path("/kml/")
    @Produces("application/vnd.google-earth.kml+xml")
    public Response queryKml(
            @QueryParam("expeditions") List<String> expeditions,
            @QueryParam("projectId") Integer projectId,
            @QueryParam("filter") String filter) {

        try {
            // Build the query, etc..
            ElasticSearchQuery query = GETElasticSearchQuery(projectId, expeditions, filter);

            ElasticSearchQuerier elasticSearchQuerier = new ElasticSearchQuerier(esClient, query);

            ArrayNode results = elasticSearchQuerier.getAllResults();

            Mapping mapping = getMapping(projectId);

            JsonWriter jsonWriter = new KmlJsonWriter.KmlJsonWriterBuilder(results, uploadPath(), BiscicolQueryUtils.getJsonFieldTransforms(mapping))
                    .latPath(BiscicolQueryUtils.getLatitudePointer(mapping))
                    .longPath(BiscicolQueryUtils.getLongitudePointer(mapping))
                    .namePath(BiscicolQueryUtils.getUniqueKeyPointer(mapping))
                    .build();

            Response.ResponseBuilder response = Response.ok(jsonWriter.write());

            response.header("Content-Disposition",
                    "attachment; filename=biocode-fims-output.kml");

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

    /**
     * Return KML for a graph query using POST
     * filter is just a single value to filter the entire dataset
     *
     * @return
     */
    @GET
    @Path("/cspace/")
    @Produces(MediaType.APPLICATION_XML)
    public Response queryCspace(
            @QueryParam("expeditions") List<String> expeditions,
            @QueryParam("projectId") Integer projectId,
            @QueryParam("filter") String filter) {

        try {
            // Build the query, etc..
            ElasticSearchQuery query = GETElasticSearchQuery(projectId, expeditions, filter);

            Mapping mapping = getMapping(projectId);
            Validation validation = new Validation();
            validation.addValidationRules(configFile, mapping);

            ElasticSearchQuerier elasticSearchQuerier = new ElasticSearchQuerier(esClient, query);

            ArrayNode results = elasticSearchQuerier.getAllResults();

            // TODO implement a CspaceJsonWriter
            JsonWriter jsonWriter = null;
            Response.ResponseBuilder response = Response.ok(jsonWriter.write());

            response.header("Content-Disposition",
                    "attachment; filename=biocode-fims-output.xml");

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

    /**
     * Return Tab delimited data for a graph query using POST
     * * <p/>
     * filter parameters are of the form:
     * name={URI} value={filter value}
     *
     * @return
     */
    @POST
    @Path("/tab/")
    @Consumes("application/x-www-form-urlencoded")
    public Response queryTabAsPost(
            MultivaluedMap<String, String> form) {

        try {
            // Build the query, etc..
            ElasticSearchQuery query = POSTElasticSearchQuery(form);

            ElasticSearchQuerier elasticSearchQuerier = new ElasticSearchQuerier(esClient, query);

            ArrayNode results = elasticSearchQuerier.getAllResults();

            JsonWriter jsonWriter = new DelimitedTextJsonWriter(results, BiscicolQueryUtils.getJsonFieldTransforms(getMapping(projectId)), uploadPath(), ",");

            Response.ResponseBuilder response = Response.ok(jsonWriter.write());

            response.header("Content-Disposition",
                    "attachment; filename=biocode-fims-output.txt");

            return response.build();
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryErrorCode.NO_RESOURCES) {
                return Response.noContent().build();
            }

            throw e;
        }
    }

    /**
     * Return Tab delimited data for a graph query.  The GET query runs a simple FILTER query for any term
     *
     * @return
     */
    @GET
    @Path("/tab/")
    public Response queryTab(
            @QueryParam("expeditions") List<String> expeditions,
            @QueryParam("projectId") Integer projectId,
            @QueryParam("filter") String filter) {

        try {
            // Build the query, etc..
            ElasticSearchQuery query = GETElasticSearchQuery(projectId, expeditions, filter);

            ElasticSearchQuerier elasticSearchQuerier = new ElasticSearchQuerier(esClient, query);

            ArrayNode results = elasticSearchQuerier.getAllResults();

            JsonWriter jsonWriter = new DelimitedTextJsonWriter(results, BiscicolQueryUtils.getJsonFieldTransforms(getMapping(projectId)), uploadPath(), ",");

            Response.ResponseBuilder response = Response.ok(jsonWriter.write());

            response.header("Content-Disposition",
                    "attachment; filename=biocode-fims-output.txt");

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

        return writeExcelResponse(results);
    }

    private Response writeExcelResponse(ArrayNode results) {
        JsonWriter jsonWriter = new ExcelJsonWriter(results, BiscicolQueryUtils.getJsonFieldTransforms(getMapping(projectId)), getMapping(projectId).getDefaultSheetName(), uploadPath());

        File file = jsonWriter.write();

        // Here we attach the other components of the excel sheet found with
        XSSFWorkbook justData = null;
        try {
            justData = new XSSFWorkbook(new FileInputStream(file));
        } catch (IOException e) {
            logger.error("failed to open excel file", e);
        }

        TemplateProcessor t = new TemplateProcessor(projectId, uploadPath(), justData);
        file = t.createExcelFileFromExistingSources("Samples", uploadPath());
        Response.ResponseBuilder response = Response.ok(file);

        response.header("Content-Disposition",
                "attachment; filename=biocode-fims-output.xlsx");

        // Return response
        if (response == null) {
            return Response.status(204).build();
        } else {
            return response.build();
        }
    }

    /**
     * Return Excel for a graph query.  The GET query runs a simple FILTER query for any term
     *
     * @return
     */
    @GET
    @Path("/excel/")
    @Produces("application/vnd.ms-excel")
    public Response queryExcel(
            @QueryParam("expeditions") List<String> expeditions,
            @QueryParam("projectId") Integer projectId,
            @QueryParam("filter") String filter) {

        if (expeditions.size() != 1) {
            throw new BadRequestException("Invalid Arguments. Only 1 expedition can be specified");
        }

        // Build the query, etc..
        ElasticSearchQuery query = GETElasticSearchQuery(projectId, expeditions, filter);

        ElasticSearchQuerier elasticSearchQuerier = new ElasticSearchQuerier(esClient, query);

        ArrayNode results = elasticSearchQuerier.getAllResults();

        return writeExcelResponse(results);
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
        if (form.containsKey("projectId")) {
            projectId = Integer.parseInt(String.valueOf(form.remove("projectId").get(0)));
        }

        if (form.containsKey("expeditions")) {
            expeditionCodes.addAll(form.remove("expeditions"));
        }
        if (form.containsKey("expeditions[]")) {
            expeditionCodes.addAll(form.remove("expeditions[]"));
        }

        if (projectId == null) {
            throw new BadRequestException("ERROR: incomplete arguments");
        }

        if (!queryAuthorizer.authorizedQuery(Collections.singletonList(projectId), expeditionCodes, userContext.getUser())) {
            throw new ForbiddenRequestException("unauthorized query.");
        }

        List<ElasticSearchFilterField> filterFields = BiscicolQueryUtils.getAvailableFilters(getMapping(projectId));

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

    /**
     * Get the query result as a file
     *
     * @param projectId
     * @param filter
     * @return
     */
    private ElasticSearchQuery GETElasticSearchQuery(Integer projectId, List<String> expeditions, String filter) {
        // Make sure projectId is set
        if (projectId == null) {
            throw new BadRequestException("ERROR: incomplete arguments");
        }

        Mapping mapping = getMapping(projectId);
        List<Attribute> attributes = mapping.getDefaultSheetAttributes();

        // Parse the GET filter
        List<ElasticSearchFilterCondition> filterConditions = parseGETFilter(filter, attributes, mapping);


        // if no expeditions are specified, then we want to only query public expeditions
        if (expeditions.size() == 0) {
            expeditionService.getPublicExpeditions(projectId).forEach(e -> expeditions.add(e.getExpeditionCode()));

            if (expeditions.size() == 0) {
                throw new FimsRuntimeException(QueryErrorCode.NO_RESOURCES, 204);
            }
        }

        return new ElasticSearchQuery(
                getQueryBuilder(filterConditions, expeditions),
                new String[]{String.valueOf(projectId)},
                new String[]{ElasticSearchIndexer.TYPE});

    }

    /**
     * Parse the GET filter string smartly.  This looks for either column Names or URI Properties, and
     * if it finds a column name maps to a uri.  Values are assumed to be last element past a semicolon ALWAYS.
     *
     * @param filterQueryString
     * @return
     */
    private List<ElasticSearchFilterCondition> parseGETFilter(String filterQueryString, List<Attribute> attributes, Mapping mapping) {
        List<ElasticSearchFilterCondition> filterConditions = new ArrayList<>();

        if (filterQueryString == null) {
            return filterConditions;
        }

        List<ElasticSearchFilterField> filterFields = BiscicolQueryUtils.getAvailableFilters(getMapping(projectId));

        String[] filterSplit = filterQueryString.split("&");

        try {
            for (String filterString : filterSplit) {
                // this regex will split on the last ":". This is need since uri's contain ":", but we want the whole
                // uri as the key
                String[] filter = filterString.split("[:](?=[^:]*$)");

                if (filter.length != 2) {
                    throw new BadRequestException("invalid filterString. couldn't find a key:value for " + filterString);
                }

                String key = filter[0];

                if (!key.contains(":")) {
                    key = mapping.lookupUriForColumn(key, attributes);
                }

                // only expect 1 value
                ElasticSearchFilterCondition filterCondition = new ElasticSearchFilterCondition(
                        lookupFilter(
                                URLDecoder.decode(key, "UTF8"),
                                filterFields
                        ),
                        URLDecoder.decode(filter[1], "UTF8")
                );

                filterConditions.add(filterCondition);
            }

        } catch (UnsupportedEncodingException e) {
            throw new FimsRuntimeException(500, e);
        }

        return filterConditions;
    }

    private ElasticSearchFilterField lookupFilter(String key, List<ElasticSearchFilterField> filters) {
        // _all is a special filter
        if (key.equals("_all")) {
            return BiscicolQueryUtils.get_AllFilter();
        }

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
            boolQueryBuilder.should(QueryBuilders.matchQuery("expedition.expeditionCode", expedition));
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

    private Mapping getMapping(Integer projectId) {
        if (mapping != null) {
            return mapping;
        }

        if (configFile == null) {
            configFile = new ConfigurationFileFetcher(projectId, uploadPath(), true).getOutputFile();
        }

        mapping = new Mapping();
        mapping.addMappingRules(configFile);

        return mapping;
    }
}

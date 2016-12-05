package biocode.fims.rest.services.rest;

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
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.QueryErrorCode;
import biocode.fims.query.*;
import biocode.fims.rest.FimsService;
import biocode.fims.run.TemplateProcessor;
import biocode.fims.service.OAuthProviderService;
import biocode.fims.settings.PathManager;
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
public class Query extends FimsService {
    private static final Logger logger = LoggerFactory.getLogger(Query.class);

    private static Integer projectId = Integer.parseInt(SettingsManager.getInstance().retrieveValue("projectId"));
    private Mapping mapping;

    private final Client esClient;

    @Autowired
    Query(OAuthProviderService providerService, SettingsManager settingsManager, Client esClient) {
        super(providerService, settingsManager);
        this.esClient = esClient;
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

        Pageable pageable = new PageRequest(page, limit);

        // Build the query, etc..
        ElasticSearchQuery query = POSTElasticSearchQuery(form);
        query.pageable(pageable);

        ElasticSearchQuerier elasticSearchQuerier = new ElasticSearchQuerier(esClient, query);

        Page<ObjectNode> results = elasticSearchQuerier.getPageableResults();

        List<JsonFieldTransform> writerColumns = DipnetQueryUtils.getJsonFieldTransforms(getMapping());
        Page<ObjectNode> transformedResults = results.map(r -> JsonTransformer.transform(r, writerColumns));

        return Response.ok(transformedResults).build();
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

        // Build the query, etc..
        ElasticSearchQuery query = POSTElasticSearchQuery(form);

        ElasticSearchQuerier elasticSearchQuerier = new ElasticSearchQuerier(esClient, query);

        ArrayNode results = elasticSearchQuerier.getAllResults();

        JsonWriter jsonWriter = new DelimitedTextJsonWriter(results, DipnetQueryUtils.getJsonFieldTransforms(getMapping()), uploadPath(), ",");

        Response.ResponseBuilder response = Response.ok(jsonWriter.write());

        response.header("Content-Disposition",
                "attachment; filename=dipnet-fims-output.csv");

        return response.build();
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

        // Build the query, etc..
        ElasticSearchQuery query = POSTElasticSearchQuery(form);

        ElasticSearchQuerier elasticSearchQuerier = new ElasticSearchQuerier(esClient, query);

        ArrayNode results = elasticSearchQuerier.getAllResults();

        JsonWriter jsonWriter = new KmlJsonWriter.KmlJsonWriterBuilder(results, uploadPath(), DipnetQueryUtils.getJsonFieldTransforms(getMapping()))
                .latPath(DipnetQueryUtils.getLatitudePointer())
                .longPath(DipnetQueryUtils.getLongitudePointer())
                .namePath(DipnetQueryUtils.getUniqueKeyPointer())
                .build();

        Response.ResponseBuilder response = Response.ok(jsonWriter.write());

        response.header("Content-Disposition",
                "attachment; filename=dipnet-fims-output.kml");

        // Return response
        if (response == null) {
            return Response.status(204).build();
        } else {
            return response.build();
        }
    }

    @POST
    @Path("/fasta/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/zip")
    public Response queryFasta(
            MultivaluedMap<String, String> form) {

        // Build the query, etc..
        ElasticSearchQuery query = POSTElasticSearchQuery(form);

        ElasticSearchQuerier elasticSearchQuerier = new ElasticSearchQuerier(esClient, query);

        ArrayNode results = elasticSearchQuerier.getAllResults();

        JsonWriter metadataJsonWriter = new DelimitedTextJsonWriter(
                results,
                DipnetQueryUtils.getJsonFieldTransforms(getMapping()),
                uploadPath(),
                ","
        );

        File metadataFile = metadataJsonWriter.write();

        List<FastaSequenceJsonFieldFilter> fastaSequenceFilters = getFastaSequenceFilters(form);

        JsonWriter fastaJsonWriter = new FastaJsonWriter(
                results,
                DipnetQueryUtils.getFastaSequenceFields(getMapping()),
                uploadPath(),
                fastaSequenceFilters);

        File fastaFile = fastaJsonWriter.write();

        Map<String, File> fileMap = new HashMap<>();
        fileMap.put("dipnet-fims-output.csv", metadataFile);
        fileMap.put("dipnet-fims-output.fasta", fastaFile);


        Response.ResponseBuilder response = Response.ok(FileUtils.zip(fileMap, uploadPath()), "application/zip");

        response.header("Content-Disposition",
                "attachment; filename=dipnet-fims-output.zip");

        // Return response
        if (response == null) {
            return Response.status(204).build();
        } else {
            return response.build();
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

        // Build the query, etc..
        ElasticSearchQuery query = POSTElasticSearchQuery(form);

        ElasticSearchQuerier elasticSearchQuerier = new ElasticSearchQuerier(esClient, query);

        ArrayNode results = elasticSearchQuerier.getAllResults();

        JsonWriter jsonWriter = new ExcelJsonWriter(results, DipnetQueryUtils.getJsonFieldTransforms(getMapping()), getMapping().getDefaultSheetName(), uploadPath());

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

        List<ElasticSearchFilterField> filterFields = DipnetQueryUtils.getAvailableFilters(getMapping());

        for (Map.Entry<String, List<String>> entry : form.entrySet()) {

            // only expect 1 value
            ElasticSearchFilterCondition filterCondition = new ElasticSearchFilterCondition(
                    lookupFilter(entry.getKey(), filterFields),
                    entry.getValue().get(0));

            filterConditions.add(filterCondition);
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

        for (Map.Entry<String, List<String>> entry: form.entrySet()) {

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
        // _all is a special filter
        if (key.equals("_all")) {
            return DipnetQueryUtils.get_AllFilter();
        }

        // if field doesn't contain a ":", then we assume this is the filter displayName
        boolean isDisplayName = !key.contains(":");

        for (ElasticSearchFilterField filter: filters) {
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

        for (ElasticSearchFilterCondition filterCondition: filterConditions) {

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

        File configFile = new ConfigurationFileFetcher(projectId, uploadPath(), true).getOutputFile();

        mapping = new Mapping();
        mapping.addMappingRules(configFile);

        return mapping;
    }
}


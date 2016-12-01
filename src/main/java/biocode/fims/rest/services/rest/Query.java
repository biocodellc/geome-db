package biocode.fims.rest.services.rest;

import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.Attribute;
import biocode.fims.digester.Mapping;
import biocode.fims.dipnet.query.DipnetQueryUtils;
import biocode.fims.elasticSearch.ElasticSearchIndexer;
import biocode.fims.elasticSearch.query.ElasticSearchQuery;
import biocode.fims.elasticSearch.query.ElasticSearchQuerier;
import biocode.fims.fileManagers.fasta.FastaFileManager;
import biocode.fims.fimsExceptions.*;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.query.*;
import biocode.fims.rest.FimsService;
import biocode.fims.run.TemplateProcessor;
import biocode.fims.service.OAuthProviderService;
import biocode.fims.settings.SettingsManager;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
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
import java.net.URLDecoder;
import java.util.*;

/**
 * Query interface for Biocode-fims expedition
 */
@Controller
@Path("/projects/query")
public class Query extends FimsService {

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
                .latPath(JsonPointer.compile("urn:decimalLatitude"))
                .longPath(JsonPointer.compile("urn:decimalLongitude"))
                .namePath(JsonPointer.compile("urn:materialSampleID"))
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
            System.out.println("failed to open excel file");
//                logger.error("failed to open QueryWriter excelFile", e);
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

        HashMap<String, String> filterMap = new HashMap<String, String>();

        // remove the projectId if present
        form.remove("projectId");

        if (form.containsKey("expeditions")) {
            expeditionCodes.addAll(form.remove("expeditions"));
        }
        if (form.containsKey("expeditions[]")) {
            expeditionCodes.addAll(form.remove("expeditions[]"));
        }

        // Create a process object here so we can look at uri/column values
        Mapping mapping = getMapping();

        List<Attribute> attributes = mapping.getDefaultSheetAttributes();

        for (Map.Entry entry : form.entrySet()) {
            String key = (String) entry.getKey();
            // Values come over as a linked list
            LinkedList value = (LinkedList) entry.getValue();

            // Treat keys with ":" as a uri
            if (!key.contains(":")) {
                key = mapping.lookupUriForColumn(key, attributes);
            }

            String v = (String) value.get(0);// only expect 1 value here
            filterMap.put(key, v);
        }

        return new ElasticSearchQuery(
                getQueryBuilder(filterMap, expeditionCodes),
                new String[]{String.valueOf(projectId)},
                attributes
        )
                .source(getSource(attributes))
                .types(new String[]{ElasticSearchIndexer.TYPE});

    }

    private String[] getSource(List<Attribute> attributes) {
        List<String> source = new ArrayList<>();

        for (Attribute attribute : attributes) {
            source.add(attribute.getUri());
        }

        return source.toArray(new String[0]);
    }

    private QueryBuilder getQueryBuilder(Map<String, String> filters, List<String> expeditionCodes) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        for (String expedition : expeditionCodes) {
            boolQueryBuilder.should(QueryBuilders.matchQuery("expedition.expeditionCode", expedition));
        }

        for (Map.Entry<String, String> filter : filters.entrySet()) {
            boolQueryBuilder.must(QueryBuilders.matchQuery(filter.getKey(), filter.getValue()));
        }

        return boolQueryBuilder;
    }

    private Mapping getMapping() {
        if (mapping != null) {
            return mapping;
        }

        File configFile = new ConfigurationFileFetcher(projectId, uploadPath(), true).getOutputFile();

        // Parse the Mapping object (this object is used extensively in downstream functions!)
        mapping = new Mapping();
        mapping.addMappingRules(configFile);

        return mapping;
    }
}


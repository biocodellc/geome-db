package biocode.fims.rest.services.rest;

import biocode.fims.application.config.DipnetAppConfig;
import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.Attribute;
import biocode.fims.digester.Mapping;
import biocode.fims.elasticSearch.ElasticSearchIndexer;
import biocode.fims.elasticSearch.query.ElasticSearchQuery;
import biocode.fims.elasticSearch.query.EsQuery;
import biocode.fims.fimsExceptions.*;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.rest.FimsService;
import biocode.fims.service.OAuthProviderService;
import biocode.fims.settings.SettingsManager;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
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
    private static Logger logger = LoggerFactory.getLogger(Query.class);
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

        EsQuery esQuery = new EsQuery(esClient, query, uploadPath());

        return Response.ok(esQuery.getJSON()).build();
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

        EsQuery esQuery = new EsQuery(esClient, query, uploadPath());

        // Run the query, passing in a format and returning the location of the output file
        File file = new File(esQuery.writeCsv());

        Response.ResponseBuilder response = Response.ok(file);

        response.header("Content-Disposition",
                "attachment; filename=dipnet-fims-output.csv");

        return response.build();
    }

    /**
     * Return JSON for a graph query.
     *
     * @return
     */
    @GET
    @Path("/json/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response queryJson(
            @QueryParam("expeditions") List<String> expeditions,
            @QueryParam("projectId") Integer projectId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("limit") @DefaultValue("100") int limit,
            @QueryParam("filter") String filter) {
        Pageable pageable = new PageRequest(page, limit);

        ElasticSearchQuery query = GETElasticSearchQuery(expeditions, projectId, filter);
        query.pageable(pageable);

        EsQuery esQuery = new EsQuery(esClient, query, uploadPath());

        return Response.ok(esQuery.getJSON()).build();
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

        // Construct a file
        ElasticSearchQuery query = GETElasticSearchQuery(expeditions, projectId, filter);

        EsQuery esQuery = new EsQuery(esClient, query, uploadPath());

        File file = new File(esQuery.writeKml());

        // Return file to client
        Response.ResponseBuilder response = Response.ok(file);

        response.header("Content-Disposition",
                "attachment; filename=dipnet-fims-output.kml");

        // Return response
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

        EsQuery esQuery = new EsQuery(esClient, query, uploadPath());

        File file = new File(esQuery.writeKml());

        // Return file to client
        Response.ResponseBuilder response = Response.ok(file);
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

        ElasticSearchQuery query = GETElasticSearchQuery(expeditions, projectId, filter);

        EsQuery esQuery = new EsQuery(esClient, query, uploadPath());

        File file = esQuery.writeExcel();

        // Return file to client
        Response.ResponseBuilder response = Response.ok(file);
        response.header("Content-Disposition",
                "attachment; filename=dipnet-fims-output.xlsx");

        // Return response
        return response.build();
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

        EsQuery esQuery = new EsQuery(esClient, query, uploadPath());

        // Run the query, passing in a format and returning the location of the output file
        File file = esQuery.writeExcel();

        // Return file to client
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
        int projectId = 0;
        List<String> expeditionCodes = new ArrayList<>();

        HashMap<String, String> filterMap = new HashMap<String, String>();

        if (form.containsKey("projectId")) {
            projectId = Integer.parseInt(form.remove("projectId").get(0));
        }

        if (form.containsKey("expeditions")) {
            expeditionCodes.addAll(form.remove("expeditions"));
        }
        if (form.containsKey("expeditions[]")) {
            expeditionCodes.addAll(form.remove("expeditions[]"));
        }

        // Make sure projectId is set
        if (projectId == 0) {
            throw new BadRequestException("ERROR: incomplete arguments");
        }

        // Create a process object here so we can look at uri/column values
        Mapping mapping = getMapping(projectId);

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

    /**
     * Get the query result as a file
     *
     * @param projectId
     * @param filter
     * @return
     */
    private ElasticSearchQuery GETElasticSearchQuery(List<String> expeditions, Integer projectId, String filter) {
        // Make sure projectId is set
        if (projectId == null) {
            throw new BadRequestException("ERROR: incomplete arguments");
        }

        Mapping mapping = getMapping(projectId);
        List<Attribute> attributes = mapping.getDefaultSheetAttributes();

        // Parse the GET filter
        Map<String, String> filters = parseGETFilter(filter, attributes, mapping);

        return new ElasticSearchQuery(getQueryBuilder(filters, expeditions), new String[]{String.valueOf(projectId)}, mapping.getDefaultSheetAttributes())
                .source(getSource(attributes))
                .types(new String[]{ElasticSearchIndexer.TYPE});

    }

    private Mapping getMapping(Integer projectId) {
        File configFile = new ConfigurationFileFetcher(projectId, uploadPath(), true).getOutputFile();

        // Parse the Mapping object (this object is used extensively in downstream functions!)
        Mapping mapping = new Mapping();
        mapping.addMappingRules(configFile);

        return mapping;
    }

    /**
     * Parse the GET filter string smartly.  This looks for either column Names or URI Properties, and
     * if it finds a URI maps to a column name.  Values are assumed to be last element past a semicolon ALWAYS.
     *
     * @param filterQueryString
     * @return
     */
    private Map<String, String> parseGETFilter(String filterQueryString, List<Attribute> attributes, Mapping mapping) {
        Map<String, String> filters = new HashMap<>();

        if (filterQueryString == null)
            return filters;

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

                filters.put(
                        URLDecoder.decode(key, "UTF8"),
                        URLDecoder.decode(filter[1], "UTF8")
                );

            }

        } catch (UnsupportedEncodingException e) {
            throw new FimsRuntimeException(500, e);
        }

        return filters;
    }
}


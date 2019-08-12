package biocode.fims.rest.services.subResources;

import biocode.fims.application.config.GeomeProperties;
import biocode.fims.authorizers.QueryAuthorizer;
import biocode.fims.config.Config;
import biocode.fims.config.models.FastaEntity;
import biocode.fims.fimsExceptions.errorCodes.GenericErrorCode;
import biocode.fims.models.Network;
import biocode.fims.models.User;
import biocode.fims.query.QueryResult;
import biocode.fims.records.Record;
import biocode.fims.records.RecordJoiner;
import biocode.fims.records.RecordSources;
import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.Entity;
import biocode.fims.fasta.FastaQueryWriter;
import biocode.fims.fimsExceptions.*;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.models.Project;
import biocode.fims.query.QueryResults;
import biocode.fims.query.dsl.Query;
import biocode.fims.query.writers.*;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.rest.Compress;
import biocode.fims.rest.responses.FileResponse;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.responses.PaginatedResponse;
import biocode.fims.service.NetworkService;
import biocode.fims.service.ProjectService;
import biocode.fims.tools.FileCache;
import biocode.fims.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@Produces(MediaType.APPLICATION_JSON)
@Scope(value = "prototype")
public class QueryController extends FimsController {
    private static final Logger logger = LoggerFactory.getLogger(QueryController.class);

    private final GeomeProperties props;
    private final RecordRepository recordRepository;
    private final QueryAuthorizer queryAuthorizer;
    private final ProjectService projectService;
    private final NetworkService networkService;
    private final FileCache fileCache;

    @PathParam("entity")
    private String entity;
    private Network network;
    private Project project;

    @Autowired
    QueryController(GeomeProperties props, RecordRepository recordRepository, QueryAuthorizer queryAuthorizer,
                    ProjectService projectService, NetworkService networkService, FileCache fileCache) {
        super(props);
        this.props = props;
        this.recordRepository = recordRepository;
        this.queryAuthorizer = queryAuthorizer;
        this.projectService = projectService;
        this.networkService = networkService;
        this.fileCache = fileCache;
    }

    /**
     * @param source      comma separated list of columns to return. If you are selecting seperate entities, you can prefix
     *                    the column with the entity to filter the parent/child entity source. (ex. Sample.materialSampleID, Event.eventID)
     * @param queryString the query to run
     * @param page        the page number to return Ex. If page=0 and limit=10, results 1-10 will be returned. If page=2 and
     *                    limit=10, results 21-30 will be returned
     * @param limit       the number of results to return
     * @implicitParam entity|string|path|true|||||false|the network entity to query
     * @summary Query network resources, returning JSON
     * @responseType org.springframework.data.domain.Page<>
     */
    @Compress
    @POST
    @Path("/json/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public PaginatedResponse<Map<String, List<Map<String, Object>>>> queryJsonAsPost(
            @FormParam("query") String queryString,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("limit") @DefaultValue("100") int limit,
            @QueryParam("source") String s) {
        return json(queryString, page, limit, s);
    }

    /**
     * @param source comma separated list of columns to return
     * @param page   the page number to return Ex. If page=0 and limit=10, results 1-10 will be returned. If page=2 and
     *               limit=10, results 21-30 will be returned
     * @param limit  the number of results to return
     * @implicitParam entity|string|path|true|||||false|the network entity to query
     * @implicitParam q|string|query|true|||||false|the query to run
     * @excludeParams queryString
     * @summary Query network resources, returning JSON
     * @responseType org.springframework.data.domain.Page<>
     */
    @Compress
    @GET
    @Path("/json/")
    public PaginatedResponse<Map<String, List<Map<String, Object>>>> queryJson(
            @QueryParam("q") String queryString,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("limit") @DefaultValue("100") int limit,
            @QueryParam("source") String s) {
        return json(queryString, page, limit, s);
    }

    private PaginatedResponse<Map<String, List<Map<String, Object>>>> json(String queryString, int page, int limit, String s) {
        List<String> sources = s != null ? Arrays.asList(s.split(",")) : Collections.emptyList();

        Query query = buildQuery(queryString, page, limit);
        return recordRepository.query(query, RecordSources.factory(sources, entity), true);
    }

    /**
     * @implicitParam entity|string|path|true|||||false|the network entity to query
     * @implicitParam q|string|form|true|||||false|the query to run
     * @excludeParam queryString
     * @summary Query network resources, returning a CSV file
     * @responseType java.io.File
     */
    @POST
    @Path("/csv/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public FileResponse queryCSVAsPost(@FormParam("q") String queryString) {
        return csv(queryString);
    }

    /**
     * @implicitParam entity|string|path|true|||||false|the network entity to query
     * @implicitParam q|string|query|true|||||false|the query to run
     * @excludeParams queryString
     * @summary Query network resources, returning CSV file
     * @responseType java.io.File
     */
    @GET
    @Path("/csv/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public FileResponse queryCSV(@QueryParam("q") String queryString) {
        return csv(queryString);
    }

    private FileResponse csv(String queryString) {
        QueryResults queryResults = run(queryString);

        try {
            if (project == null) {
                System.out.println("project: null");
            } else {
                System.out.println("project: " + project.getProjectId());
            }
            QueryWriter queryWriter = new DelimitedTextQueryWriter(queryResults, ",", getConfig());

            List<File> files = queryWriter.write();
            return returnFileResults(FileUtils.zip(files), "geome-fims-output-csv.zip");
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryCode.NO_RESOURCES) {
                return null;
            }

            throw e;
        }
    }

    /**
     * @implicitParam entity|string|path|true|||||false|the network entity to query
     * @implicitParam q|string|form|true|||||false|the query to run
     * @excludeParams queryString
     * @summary Query network resources, returning a KML file
     * @responseType java.io.File
     */
    @POST
    @Path("/kml/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public FileResponse queryKMLAsPost(@FormParam("q") String queryString) {
        return kml(queryString);
    }

    /**
     * @implicitParam entity|string|path|true|||||false|the network entity to query
     * @implicitParam q|string|query|true|||||false|the query to run
     * @excludeParams queryString
     * @summary Query network resources, returning KML file
     * @responseType java.io.File
     */
    @GET
    @Path("/kml/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public FileResponse queryKML(@QueryParam("q") String queryString) {
        return kml(queryString);
    }

    private FileResponse kml(String queryString) {
        Query query = buildQuery(queryString);
        QueryResults queryResults = recordRepository.query(query);

        if (queryResults.results().size() > 1) {
            throw new FimsRuntimeException(QueryCode.UNSUPPORTED_QUERY, "Multi-select queries not supported", 400);
        }

        try {
            // TODO centralize this definedBy and point this method and ProjectController.getLatLongColumns to it
            String decimalLongDefinedBy = "http://rs.tdwg.org/dwc/terms/decimalLongitude";
            String decimalLatDefinedBy = "http://rs.tdwg.org/dwc/terms/decimalLatitude";

            Entity entity = query.queryEntity();

            QueryWriter queryWriter = new KmlQueryWriter.Builder(queryResults.results().get(0))
                    .latColumn(getColumn(entity.getAttributes(), decimalLongDefinedBy))
                    .latColumn(getColumn(entity.getAttributes(), decimalLatDefinedBy))
                    .nameColumn(entity.getUniqueKey())
                    .build();

            return returnFileResults(FileUtils.zip(queryWriter.write()), "geome-fims-output-kml.zip");
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryCode.NO_RESOURCES) {
                return null;
            }

            throw e;
        }
    }

    private String getColumn(List<Attribute> attributes, String definedBy) {
        for (Attribute a : attributes) {
            if (a.getDefinedBy().equals(definedBy)) {
                return a.getColumn();
            }
        }

        return null;
    }

    /**
     * @implicitParam entity|string|path|true|||||false|the network entity to query
     * @implicitParam q|string|query|true|||||false|the query to run
     * @excludeParams queryString
     * @summary Query network resources, returning CSPACE file
     * @responseType java.io.File
     */
    @GET
    @Path("/cspace/")
    public FileResponse queryCspace(@QueryParam("q") String queryString) {

        QueryResults queryResults = run(queryString);

        if (queryResults.results().size() > 1) {
            throw new FimsRuntimeException(QueryCode.UNSUPPORTED_QUERY, "Multi-select queries not supported", 400);
        }

        try {
            QueryWriter queryWriter = new CspaceQueryWriter(queryResults.results().get(0), getConfig());

            return returnFileResults(FileUtils.zip(queryWriter.write()), "geome-fims-output-cspace.zip");
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryCode.NO_RESOURCES) {
                return null;
            }

            throw e;
        }

    }

    /**
     * @implicitParam entity|string|path|true|||||false|the network entity to query
     * @implicitParam q|string|query|true|||||false|the query to run
     * @excludeParams queryString
     * @summary Query network resources, returning FASTA file
     * @responseType java.io.File
     */
    @GET
    @Path("/fasta/")
    @Consumes(MediaType.APPLICATION_JSON)
    public FileResponse queryFasta(@QueryParam("q") @DefaultValue("") String queryString) {
        Config config = getConfig();

        Entity e = config.entity(entity);
        if (e == null || !e.type().equals(FastaEntity.TYPE)) {
            throw new BadRequestException("queryEntity is not a FastaEntity");
        }

        Entity parentEntity = e;
        do {
            parentEntity = config.entity(parentEntity.getParentEntity());
        } while (parentEntity.isChildEntity());

        List<String> entities = config.getEntityRelations(parentEntity, e).stream()
                .flatMap(r -> Stream.of(r.getChildEntity().getConceptAlias(), r.getParentEntity().getConceptAlias()))
                .collect(Collectors.toList());

        queryString += " _select_:[" + String.join(",", entities) + "]";

        QueryResults queryResults = run(queryString);
        if (queryResults.isEmpty()) return null;

        try {
            RecordJoiner joiner = new RecordJoiner(getConfig(), e, queryResults);

            LinkedList<Record> tissues = queryResults.getResult(e.getConceptAlias())
                    .records().stream()
                    .map(joiner::joinRecords)
                    .collect(Collectors.toCollection(LinkedList::new));

            // call getConfig() again here b/c it will be set to the ProjectConfig if the query was against a single project
            QueryWriter queryWriter = new FastaQueryWriter(new QueryResult(tissues, e, config.entity(e.getParentEntity()), props.bcidResolverPrefix()), getConfig());

            List<QueryResult> metadataResults = queryResults.results().stream()
                    .filter(r -> !r.entity().equals(e))
                    .collect(Collectors.toList());

            QueryWriter parentQueryWriter = new DelimitedTextQueryWriter(
                    new QueryResults(metadataResults),
                    ",", getConfig()
            );

            List<File> files = queryWriter.write();
            files.addAll(parentQueryWriter.write());

            return returnFileResults(FileUtils.zip(files), "geome-fims-output-fasta.zip");
        } catch (FimsRuntimeException err) {
            if (err.getErrorCode() == QueryCode.NO_RESOURCES) {
                return null;
            }

            throw err;
        }
    }

    /**
     * @implicitParam entity|string|path|true|||||false|the network entity to query
     * @implicitParam q|string|form|true|||||false|the query to run
     * @excludeParam queryString
     * @summary Query network records, returning a TAB delimited text file
     * @responseType java.io.File
     */
    @POST
    @Path("/tab/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public FileResponse queryTABAsPost(@FormParam("q") String queryString) {
        return tsv(queryString);
    }

    /**
     * @implicitParam entity|string|path|true|||||false|the network entity to query
     * @implicitParam q|string|query|true|||||false|the query to run
     * @excludeParams queryString
     * @summary Query network records, returning TAB delimited text file
     * @responseType java.io.File
     */
    @GET
    @Path("/tab/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public FileResponse queryTAB(@QueryParam("q") String queryString) {
        return tsv(queryString);
    }

    private FileResponse tsv(String queryString) {
        QueryResults queryResults = run(queryString);

        try {
            QueryWriter queryWriter = new DelimitedTextQueryWriter(queryResults, "\t", getConfig());

            List<File> files = queryWriter.write();

            return returnFileResults(FileUtils.zip(files), "geome-fims-output-tab.zip");
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryCode.NO_RESOURCES) {
                return null;
            }

            throw e;
        }
    }

    /**
     * @implicitParam entity|string|path|true|||||false|the network entity to query
     * @implicitParam q|string|form|true|||||false|the query to run
     * @excludeParam queryString
     * @summary Query network records, returning a excel workbook
     * @responseType java.io.File
     */
    @POST
    @Path("/excel/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public FileResponse queryExcelAsPost(@FormParam("q") String queryString) {
        return excel(queryString);
    }

    /**
     * @implicitParam entity|string|path|true|||||false|the network entity to query
     * @implicitParam q|string|query|true|||||false|the query to run
     * @excludeParams queryString
     * @summary Query network records, returning excel workbook
     * @responseType java.io.File
     */
    @GET
    @Path("/excel/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public FileResponse queryExcel(@QueryParam("q") String queryString) {
        return excel(queryString);
    }

    private FileResponse excel(String queryString) {
        Query query = buildQuery(queryString);
        QueryResults queryResults = recordRepository.query(query);

        try {
            // If project, then this is a single project query
            QueryWriter queryWriter = (project == null)
                    ? new ExcelQueryWriter(getConfig(), queryResults, props.naan())
                    : new ExcelQueryWriter(project, queryResults, props.naan());
            List<File> files = queryWriter.write();

            // ExcelQueryWriter return single file
            return returnFileResults(files.get(0), "geome-fims-output-excel.xlsx");
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryCode.NO_RESOURCES) {
                return null;
            }

            throw e;
        }
    }

    private FileResponse returnFileResults(File file, String name) {
        if (file == null) return null;

        String fileId = fileCache.cacheFileForUser(file, userContext.getUser(), name);
        return new FileResponse(uriInfo.getBaseUriBuilder(), fileId);
    }

    private QueryResults run(String queryString) {
        return recordRepository.query(
                buildQuery(queryString)
        );
    }

    private Query buildQuery(String queryString) {
        return buildQuery(queryString, null, null);
    }

    private Network getNetwork() {
        if (network == null || props.networkId() != network.getId()) {
            network = networkService.getNetwork(props.networkId());

            if (network == null) {
                throw new FimsRuntimeException(GenericErrorCode.SERVER_ERROR, "Failed to find valid networkId in props file", 500);
            }
        }

        return network;
    }

    private Config getConfig() {
        return (project == null) ? getNetwork().getNetworkConfig() : project.getProjectConfig();

    }

    private Query buildQuery(String queryString, Integer page, Integer limit) {
        network = getNetwork();
        User user = userContext.getUser();

        Query query = Query.build(network, entity, queryString, page, limit);

        List<Integer> projects = query.projects();

        if (projects.isEmpty()) {
            // If no projects are specified, we must restrict the query to public projects & those projects that a user
            // is a member of

            List<Integer> restrictToProjects = projectService.getProjects(user, true).stream()
                    .map(Project::getProjectId)
                    .collect(Collectors.toList());

            query.restrictToProjects(restrictToProjects);
        } else {
            if (projects.size() == 1) {
                project = projectService.getProject(projects.get(0));

                if (project != null) {
                    query.setProjectConfig(project.getProjectConfig());
                }
            }

            if (!queryAuthorizer.authorizedQuery(projects, new ArrayList<>(query.expeditions()), userContext.getUser())) {
                throw new ForbiddenRequestException("unauthorized query.");
            }
        }

        // Only public expeditions are queryable for non-authenticated users
        if (user == null) query.restrictToPublicExpeditions();

        return query;
    }
}


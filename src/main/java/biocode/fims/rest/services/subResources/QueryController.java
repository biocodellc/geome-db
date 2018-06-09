package biocode.fims.rest.services.subResources;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.authorizers.QueryAuthorizer;
import biocode.fims.projectConfig.models.Attribute;
import biocode.fims.projectConfig.models.Entity;
import biocode.fims.fasta.FastaQueryWriter;
import biocode.fims.fimsExceptions.*;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.models.Project;
import biocode.fims.projectConfig.models.FastaEntity;
import biocode.fims.query.QueryResults;
import biocode.fims.query.dsl.Query;
import biocode.fims.query.writers.*;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.rest.Compress;
import biocode.fims.rest.responses.FileResponse;
import biocode.fims.rest.FimsController;
import biocode.fims.service.ProjectService;
import biocode.fims.tools.FileCache;
import biocode.fims.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.util.*;

@Controller
@Produces(MediaType.APPLICATION_JSON)
public class QueryController extends FimsController {
    private static final Logger logger = LoggerFactory.getLogger(QueryController.class);

    private final RecordRepository recordRepository;
    private final QueryAuthorizer queryAuthorizer;
    private final ProjectService projectService;
    private final FileCache fileCache;

    @QueryParam("projectId")
    private Integer projectId;
    @PathParam("entity")
    private String entity;

    @Autowired
    QueryController(FimsProperties props, RecordRepository recordRepository, QueryAuthorizer queryAuthorizer,
                    ProjectService projectService, FileCache fileCache) {
        super(props);
        this.recordRepository = recordRepository;
        this.queryAuthorizer = queryAuthorizer;
        this.projectService = projectService;
        this.fileCache = fileCache;
    }

    /**
     * @implicitParam projectId|integer|query|true|||||false|the project to query
     * @implicitParam entity|string|path|true|||||false|the project entity to query
     * @param queryString the query to run
     * @param page        the page number to return Ex. If page=0 and limit=10, results 1-10 will be returned. If page=2 and
     *                    limit=10, results 21-30 will be returned
     * @param limit       the number of results to return
     * @param source      comma separated list of columns to return
     * @summary Query project resources, returning JSON
     * @responseType org.springframework.data.domain.Page<>
     */
    @Compress
    @POST
    @Path("/json/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Page queryJsonAsPost(
            @FormParam("query") String queryString,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("limit") @DefaultValue("100") int limit,
            @QueryParam("source") String s) {
        return json(queryString, page, limit, s);
    }

    /**
     * @implicitParam projectId|integer|query|true|||||false|the project to query
     * @implicitParam entity|string|path|true|||||false|the project entity to query
     * @implicitParam q|string|query|true|||||false|the query to run
     * @param page      the page number to return Ex. If page=0 and limit=10, results 1-10 will be returned. If page=2 and
     *                  limit=10, results 21-30 will be returned
     * @param limit     the number of results to return
     * @param source    comma separated list of columns to return
     * @excludeParams queryString
     * @summary Query project resources, returning JSON
     * @responseType org.springframework.data.domain.Page<>
     */
    @Compress
    @GET
    @Path("/json/")
    public Page queryJson(
            @QueryParam("q") String queryString,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("limit") @DefaultValue("100") int limit,
            @QueryParam("source") String s) {
        return json(queryString, page, limit, s);
    }

    private Page json(String queryString, int page, int limit, String s) {
        List<String> source = s != null ? Arrays.asList(s.split(",")) : Collections.emptyList();

        Query query = buildQuery(queryString);
        return recordRepository.query(query, page, limit, source, true);
    }

    /**
     * @implicitParam projectId|integer|query|true|||||false|the project to query
     * @implicitParam entity|string|path|true|||||false|the project entity to query
     * @implicitParam q|string|form|true|||||false|the query to run
     * @excludeParam queryString
     * @summary Query project resources, returning a CSV file
     * @responseType java.io.File
     */
    @POST
    @Path("/csv/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public FileResponse queryCSVAsPost(@FormParam("q") String queryString) {
        return csv(queryString);
    }

    /**
     * @implicitParam projectId|integer|query|true|||||false|the project to query
     * @implicitParam entity|string|path|true|||||false|the project entity to query
     * @implicitParam q|string|query|true|||||false|the query to run
     * @excludeParams queryString
     * @summary Query project resources, returning CSV file
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
            QueryWriter queryWriter = new DelimitedTextQueryWriter(queryResults, ",");

            File file = queryWriter.write();

            if (file.getName().endsWith("zip")) {
                return returnFileResults(file, "geome-fims-output.zip");
            }

            return returnFileResults(file, "geome-fims-output.csv");
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryCode.NO_RESOURCES) {
                return null;
            }

            throw e;
        }
    }

    /**
     * @implicitParam entity|string|path|true|||||false|the project entity to query
     * @implicitParam projectId|integer|query|true|||||false|the project to query
     * @implicitParam q|string|form|true|||||false|the query to run
     * @excludeParams queryString
     * @summary Query project resources, returning a KML file
     * @responseType java.io.File
     */
    @POST
    @Path("/kml/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public FileResponse queryKMLAsPost(@FormParam("q") String queryString) {
        return kml(queryString);
    }

    /**
     * @implicitParam entity|string|path|true|||||false|the project entity to query
     * @implicitParam projectId|integer|query|true|||||false|the project to query
     * @implicitParam q|string|query|true|||||false|the query to run
     * @excludeParams queryString
     * @summary Query project resources, returning KML file
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

            return returnFileResults(queryWriter.write(), "geome-fims-output.kml");
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
     * @implicitParam entity|string|path|true|||||false|the project entity to query
     * @implicitParam projectId|integer|query|true|||||false|the project to query
     * @implicitParam q|string|query|true|||||false|the query to run
     * @excludeParams queryString
     * @summary Query project resources, returning CSPACE file
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
            Project project = projectService.getProject(projectId);
            QueryWriter queryWriter = new CspaceQueryWriter(queryResults.results().get(0), project.getProjectConfig());

            return returnFileResults(queryWriter.write(), "geome-fims-output.cspace.xml");
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryCode.NO_RESOURCES) {
                return null;
            }

            throw e;
        }

    }

    /**
     * @implicitParam entity|string|path|true|||||false|the project entity to query
     * @implicitParam projectId|integer|query|true|||||false|the project to query
     * @implicitParam q|string|query|true|||||false|the query to run
     * @excludeParams queryString
     * @summary Query project resources, returning FASTA file
     * @responseType java.io.File
     */
    @GET
    @Path("/fasta/")
    @Consumes(MediaType.APPLICATION_JSON)
    public FileResponse queryFasta(@QueryParam("q") String queryString) {
        // TODO need to fetch parent queryEntity metadata for query results & return zip file
        // of csv metadata output along w/ fasta file

        Project project = projectService.getProject(projectId);

        Entity e = project.getProjectConfig().entity(entity);
        if (!(e instanceof FastaEntity)) {
            throw new BadRequestException("queryEntity is not a FastaEntity");
        }

        Entity parentEntity = project.getProjectConfig().entity(e.getParentEntity());

        queryString += " _select_:" + parentEntity.getConceptAlias();

        QueryResults queryResults = run(queryString);

        try {
            QueryWriter queryWriter = new FastaQueryWriter(queryResults.getResult(e.getConceptAlias()), project.getProjectConfig());
            QueryResults parentQueryResults = new QueryResults(Arrays.asList(queryResults.getResult(e.getParentEntity())));
            QueryWriter parentQueryWriter = new DelimitedTextQueryWriter(parentQueryResults, ",");

            File fastaFile = queryWriter.write();
            File metadataFile = parentQueryWriter.write();

            Map<String, File> fileMap = new HashMap<>();
            fileMap.put("biocode-fims-output.csv", metadataFile);

            if (fastaFile.getName().endsWith(".zip")) {
                fileMap.put("biocode-fims-fasta.zip", fastaFile);
            } else {
                fileMap.put("biocode-fims-output.fasta", fastaFile);
            }

            File file = FileUtils.zip(fileMap, defaultOutputDirectory());
            return returnFileResults(file, "geome-fims-output.zip");
        } catch (FimsRuntimeException err) {
            if (err.getErrorCode() == QueryCode.NO_RESOURCES) {
                return null;
            }

            throw err;
        }
    }

    /**
     * @implicitParam entity|string|path|true|||||false|the project entity to query
     * @implicitParam projectId|integer|query|true|||||false|the project to query
     * @implicitParam q|string|form|true|||||false|the query to run
     * @excludeParam queryString
     * @summary Query project resources, returning a TAB delimited text file
     * @responseType java.io.File
     */
    @POST
    @Path("/tab/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public FileResponse queryTABAsPost(@FormParam("q") String queryString) {
        return tsv(queryString);
    }

    /**
     * @implicitParam entity|string|path|true|||||false|the project entity to query
     * @implicitParam projectId|integer|query|true|||||false|the project to query
     * @implicitParam q|string|query|true|||||false|the query to run
     * @excludeParams queryString
     * @summary Query project resources, returning TAB delimited text file
     * @responseType java.io.File
     */
    @GET
    @Path("/tsv/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public FileResponse queryTAB(@QueryParam("q") String queryString) {
        return tsv(queryString);
    }

    private FileResponse tsv(String queryString) {
        QueryResults queryResults = run(queryString);

        try {
            QueryWriter queryWriter = new DelimitedTextQueryWriter(queryResults, "\t");

            File file = queryWriter.write();

            if (file.getName().endsWith("zip")) {
                return returnFileResults(file, "geome-fims-output.zip");
            }
            return returnFileResults(file, "geome-fims-output.txt");
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryCode.NO_RESOURCES) {
                return null;
            }

            throw e;
        }
    }

    /**
     * @implicitParam entity|string|path|true|||||false|the project entity to query
     * @implicitParam projectId|integer|query|true|||||false|the project to query
     * @implicitParam q|string|form|true|||||false|the query to run
     * @excludeParam queryString
     * @summary Query project resources, returning a excel workbook
     * @responseType java.io.File
     */
    @POST
    @Path("/excel/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public FileResponse queryExcelAsPost(@FormParam("q") String queryString) {
        return excel(queryString);
    }

    /**
     * @implicitParam entity|string|path|true|||||false|the project entity to query
     * @implicitParam projectId|integer|query|true|||||false|the project to query
     * @implicitParam q|string|query|true|||||false|the query to run
     * @excludeParams queryString
     * @summary Query project resources, returning excel workbook
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
            QueryWriter queryWriter = new ExcelQueryWriter(projectService.getProject(projectId), queryResults, props.naan());
            File file = queryWriter.write();

            return returnFileResults(file, "geome-fims-output.xlsx");
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
        // Make sure projectId is set
        if (projectId == null) {
            throw new BadRequestException("ERROR: incomplete arguments");
        }

        Project project = projectService.getProject(projectId);

        Query query = Query.factory(project, entity, queryString);

        // may break biocode-lims plugin
        if (!queryAuthorizer.authorizedQuery(Collections.singletonList(projectId), new ArrayList<>(query.expeditions()), userContext.getUser())) {
            throw new ForbiddenRequestException("unauthorized query.");
        }
        return query;
    }
}


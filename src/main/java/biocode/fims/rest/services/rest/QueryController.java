package biocode.fims.rest.services.rest;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.authorizers.QueryAuthorizer;
import biocode.fims.digester.Attribute;
import biocode.fims.digester.Entity;
import biocode.fims.digester.FastaEntity;
import biocode.fims.fasta.FastaQueryWriter;
import biocode.fims.fimsExceptions.*;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.models.Project;
import biocode.fims.query.QueryBuilder;
import biocode.fims.query.QueryResult;
import biocode.fims.query.dsl.Query;
import biocode.fims.query.dsl.QueryParser;
import biocode.fims.query.writers.*;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.rest.FimsService;
import biocode.fims.run.TemplateProcessor;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import biocode.fims.utils.FileUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.parboiled.Parboiled;
import org.parboiled.errors.ParserRuntimeException;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Query interface for Biocode-fims expedition
 *
 * @resourceDescription Query a project's resources. See <a href='http://fims.readthedocs.io/en/latest/fims/query.html'>Fims Docs</a>
 * for more detailed information regarding queries.
 * @resourceTag Resources
 */
@Controller
@Path("/projects/{projectId}/query/{entity}")
public class QueryController extends FimsService {
    private static final Logger logger = LoggerFactory.getLogger(QueryController.class);

    private final RecordRepository recordRepository;
    private final QueryAuthorizer queryAuthorizer;
    private final ExpeditionService expeditionService;
    private final ProjectService projectService;

    @PathParam("projectId")
    private Integer projectId;
    @PathParam("entity")
    private String entity;

    @Autowired
    QueryController(FimsProperties props, RecordRepository recordRepository, QueryAuthorizer queryAuthorizer,
                    ExpeditionService expeditionService, ProjectService projectService) {
        super(props);
        this.recordRepository = recordRepository;
        this.queryAuthorizer = queryAuthorizer;
        this.expeditionService = expeditionService;
        this.projectService = projectService;
    }

    /**
     * @param projectId   the project to query
     * @param entity      the project entity to query
     * @param queryString the query to run
     * @param page        the page number to return Ex. If page=0 and limit=10, results 1-10 will be returned. If page=2 and
     *                    limit=10, results 21-30 will be returned
     * @param limit       the number of results to return
     * @summary Query project resources, returning JSON
     * @responseType org.springframework.data.domain.Page<>
     */
    @POST
    @Path("/json/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Page queryJsonAsPost(
            @FormParam("query") String queryString,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("limit") @DefaultValue("100") int limit) {
        return json(queryString, page, limit);
    }

    /**
     * @param projectId the project to query
     * @param page      the page number to return Ex. If page=0 and limit=10, results 1-10 will be returned. If page=2 and
     *                  limit=10, results 21-30 will be returned
     * @param limit     the number of results to return
     * @implicitParam q|string|query|true|||||false|the query to run
     * @excludeParams queryString
     * @summary Query project resources, returning JSON
     * @responseType org.springframework.data.domain.Page<>
     */
    @GET
    @Path("/json/")
    @Produces(MediaType.APPLICATION_JSON)
    public Page queryJson(
            @QueryParam("q") String queryString,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("limit") @DefaultValue("100") int limit) {
        return json(queryString, page, limit);
    }

    private Page json(String queryString, int page, int limit) {
        Query query = buildQuery(queryString);
        return recordRepository.query(query, page, limit, true);
    }

    /**
     * @param projectId   the project to query
     * @param queryString the query to run
     * @summary Query project resources, returning a CSV file
     * @responseType java.io.File
     */
    @POST
    @Path("/csv/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("text/csv")
    public Response queryCSVAsPost(@FormParam("query") String queryString) {
        return csv(queryString);
    }

    /**
     * @param projectId the project to query
     * @implicitParam q|string|query|true|||||false|the query to run
     * @excludeParams queryString
     * @summary Query project resources, returning CSV file
     * @responseType java.io.File
     */
    @GET
    @Path("/csv/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("text/csv")
    public Response queryCSV(@QueryParam("q") String queryString) {
        return csv(queryString);
    }

    private Response csv(String queryString) {
        QueryResult queryResult = run(queryString);

        try {
            QueryWriter queryWriter = new DelimitedTextQueryWriter(queryResult, ",");

            Response.ResponseBuilder response = Response.ok(queryWriter.write());

            response.header("Content-Disposition",
                    "attachment; filename=biocode-fims-output.csv");

            return response.build();
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryCode.NO_RESOURCES) {
                return Response.noContent().build();
            }

            throw e;
        }
    }

    /**
     * @param projectId   the project to query
     * @param queryString the query to run
     * @summary Query project resources, returning a KML file
     * @responseType java.io.File
     */
    @POST
    @Path("/kml/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/vnd.google-earth.kml+xml")
    public Response queryKMLAsPost(@FormParam("query") String queryString) {
        return kml(queryString);
    }

    /**
     * @param projectId the project to query
     * @implicitParam q|string|query|true|||||false|the query to run
     * @excludeParams queryString
     * @summary Query project resources, returning KML file
     * @responseType java.io.File
     */
    @GET
    @Path("/kml/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/vnd.google-earth.kml+xml")
    public Response queryKML(@QueryParam("q") String queryString) {
        return kml(queryString);
    }

    private Response kml(String queryString) {
        Query query = buildQuery(queryString);
        QueryResult queryResult = recordRepository.query(query);

        try {
            // TODO centralize this definedBy and point this method and ProjectController.getLatLongColumns to it
            String decimalLongDefinedBy = "http://rs.tdwg.org/dwc/terms/decimalLongitude";
            String decimalLatDefinedBy = "http://rs.tdwg.org/dwc/terms/decimalLatitude";

            Entity entity = query.entity();

            QueryWriter queryWriter = new KmlQueryWriter.Builder(queryResult)
                    .latColumn(getColumn(entity.getAttributes(), decimalLongDefinedBy))
                    .latColumn(getColumn(entity.getAttributes(), decimalLatDefinedBy))
                    .nameColumn(entity.getUniqueKey())
                    .build();

            Response.ResponseBuilder response = Response.ok(queryWriter.write());

            response.header("Content-Disposition",
                    "attachment; filename=biocode-fims-output.kml");

            return response.build();
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryCode.NO_RESOURCES) {
                return Response.noContent().build();
            }

            throw e;
        }
    }

    private String getColumn(List<Attribute> attributes, String definedBy) {
        for (Attribute a : attributes) {
            if (a.getDefined_by().equals(definedBy)) {
                return a.getColumn();
            }
        }

        return null;
    }

    /**
     * @param projectId the project to query
     * @implicitParam q|string|query|true|||||false|the query to run
     * @excludeParams queryString
     * @summary Query project resources, returning CSPACE file
     * @responseType java.io.File
     */
    @GET
    @Path("/cspace/")
    @Produces(MediaType.APPLICATION_XML)
    public Response queryCspace(@QueryParam("q") String queryString) {

        QueryResult queryResult = run(queryString);

        try {
            Project project = projectService.getProject(projectId);
            QueryWriter queryWriter = new CspaceQueryWriter(queryResult, project.getProjectConfig());

            Response.ResponseBuilder response = Response.ok(queryWriter.write());

            response.header("Content-Disposition",
                    "attachment; filename=biocode-fims-output.cspace.xmll");

            return response.build();
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryCode.NO_RESOURCES) {
                return Response.noContent().build();
            }

            throw e;
        }

    }

    /**
     * @param projectId the project to query
     * @implicitParam q|string|query|true|||||false|the query to run
     * @excludeParams queryString
     * @summary Query project resources, returning FASTA file
     * @responseType java.io.File
     */
    @GET
    @Path("/fasta/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response queryFasta(@QueryParam("q") String queryString) {
        // TODO need to fetch parent entity metadata for query results & return zip file
        // of csv metadata output along w/ fasta file

        Project project = projectService.getProject(projectId);

        Entity e = project.getProjectConfig().entity(entity);
        if (!(e instanceof FastaEntity)) {
            throw new BadRequestException("entity is not a FastaEntity");
        }

        QueryResult queryResult = run(queryString);

        Entity parentEntity = project.getProjectConfig().entity(e.getParentEntity());
        List<String> parentIdentifiers = queryResult.get(false).stream()
                .map(r -> r.get(parentEntity.getUniqueKey()))
                .collect(Collectors.toList());
        // Hacky way to fetch parent metadata.
        // TODO improve this.
        // Probably best to be able to return parent entity data in a single query
        //   -- childEntity queries don't have to be directly related, maybe we can specify
        //      the entity to return (parent, grandParent, etc) or return multiple
        // Another option is to implement an InExpression for the query

        StringBuilder parentQueryBuilder = new StringBuilder();
        for (String parentIdentifier : parentIdentifiers) {
            parentQueryBuilder
                    .append(parentEntity.getUniqueKey())
                    .append(" = ")
                    .append(parentIdentifier)
                    .append(" or ");
        }
        String parentQuery = parentQueryBuilder.toString();
        // remote the last " or "
        parentQuery = parentQuery.substring(parentQuery.length() - 4);

        QueryResult parentQueryResult = recordRepository.query(buildQuery(parentQuery));

        try {
            QueryWriter queryWriter = new FastaQueryWriter(queryResult, project.getProjectConfig());
            QueryWriter parentQueryWriter = new DelimitedTextQueryWriter(parentQueryResult, ",");

            File fastaFile = queryWriter.write();
            File metadataFile = parentQueryWriter.write();

            Map<String, File> fileMap = new HashMap<>();
            fileMap.put("biocode-fims-output.csv", metadataFile);

            if (fastaFile.getName().endsWith(".zip")) {
                fileMap.put("biocode-fims-fasta.zip", fastaFile);
            } else {
                fileMap.put("biocode-fims-output.fasta", fastaFile);
            }

            Response.ResponseBuilder response = Response.ok(FileUtils.zip(fileMap, defaultOutputDirectory()));

            response.header("Content-Disposition",
                    "attachment; filename=biocode-fims-output.zip");

            return response.build();
        } catch (FimsRuntimeException err) {
            if (err.getErrorCode() == QueryCode.NO_RESOURCES) {
                return Response.noContent().build();
            }

            throw e;
        }
    }

    /**
     * @param projectId   the project to query
     * @param queryString the query to run
     * @summary Query project resources, returning a TAB delimited text file
     * @responseType java.io.File
     */
    @POST
    @Path("/tab/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("text/txt")
    public Response queryTABAsPost(@FormParam("query") String queryString) {
        return tsv(queryString);
    }

    /**
     * @param projectId the project to query
     * @implicitParam q|string|query|true|||||false|the query to run
     * @excludeParams queryString
     * @summary Query project resources, returning TAB delimited text file
     * @responseType java.io.File
     */
    @GET
    @Path("/tsv/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("text/txt")
    public Response queryTAB(@QueryParam("q") String queryString) {
        return tsv(queryString);
    }

    private Response tsv(String queryString) {
        QueryResult queryResult = run(queryString);

        try {
            QueryWriter queryWriter = new DelimitedTextQueryWriter(queryResult, "\t");

            Response.ResponseBuilder response = Response.ok(queryWriter.write());

            response.header("Content-Disposition",
                    "attachment; filename=biocode-fims-output.txt");

            return response.build();
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryCode.NO_RESOURCES) {
                return Response.noContent().build();
            }

            throw e;
        }
    }

    /**
     * @param projectId   the project to query
     * @param queryString the query to run
     * @summary Query project resources, returning a excel workbook
     * @responseType java.io.File
     */
    @POST
    @Path("/excel/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/vnd.ms-excel")
    public Response queryExcelAsPost(@FormParam("query") String queryString) {
        return excel(queryString);
    }

    /**
     * @param projectId the project to query
     * @implicitParam q|string|query|true|||||false|the query to run
     * @excludeParams queryString
     * @summary Query project resources, returning excel workbook
     * @responseType java.io.File
     */
    @GET
    @Path("/excel/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/vnd.ms-excel")
    public Response queryExcel(@QueryParam("q") String queryString) {
        return excel(queryString);
    }

    private Response excel(String queryString) {
        //TODO verify that only 1 expedition is included
//        throw new BadRequestException("Invalid Arguments. Only 1 expedition can be specified");

        QueryResult queryResult = run(queryString);

        try {
            //TODO refactor the templateProcessor code to be done inside the ExcelQueryWriter
            QueryWriter queryWriter = new ExcelQueryWriter(queryResult);

            File file = queryWriter.write();

            // Here we attach the other components of the excel sheet found with
            XSSFWorkbook justData = null;
            try {
                justData = new XSSFWorkbook(new FileInputStream(file));
            } catch (IOException e) {
                logger.error("failed to open excel file", e);
            }

            TemplateProcessor t = new TemplateProcessor(projectId, defaultOutputDirectory(), justData, props.naan());
            file = t.createExcelFileFromExistingSources(queryResult.entity().getWorksheet(), defaultOutputDirectory());

            Response.ResponseBuilder response = Response.ok(file);

            response.header("Content-Disposition",
                    "attachment; filename=biocode-fims-output.xlsx");

            return response.build();
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryCode.NO_RESOURCES) {
                return Response.noContent().build();
            }

            throw e;
        }
    }

    private QueryResult run(String queryString) {
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

        QueryBuilder queryBuilder = new QueryBuilder(project, entity);

        QueryParser parser = Parboiled.createParser(QueryParser.class, queryBuilder);
        try {
            ParsingResult<Query> result = new ReportingParseRunner<Query>(parser.Parse()).run(queryString);

            if (result.hasErrors() || result.resultValue == null) {
                throw new FimsRuntimeException(QueryCode.INVALID_QUERY, 400, result.parseErrors.toString());
            }

            Query query = result.resultValue;

            // commenting this out for now until biocode-lims releases a new plugin
//            if (!queryAuthorizer.authorizedQuery(Collections.singletonList(projectId), new ArrayList<>(query.expeditions()), userContext.getUser())) {
//                throw new ForbiddenRequestException("unauthorized query.");
//            }

            return query;
        } catch (ParserRuntimeException e) {
            String parsedMsg = e.getMessage().replaceFirst(" action '(.*)'", "");
            throw new FimsRuntimeException(QueryCode.INVALID_QUERY, 400, parsedMsg.substring(0, (parsedMsg.indexOf("^"))));
        }
    }
}


package biocode.fims.rest.services;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.evolution.models.EvolutionRecordReference;
import biocode.fims.evolution.processing.EvolutionRetrievalTask;
import biocode.fims.evolution.processing.EvolutionTaskExecutor;
import biocode.fims.evolution.service.EvolutionService;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.responses.RecordResponse;
import biocode.fims.rest.services.subResources.QueryController;
import biocode.fims.rest.services.subResources.RecordsResource;
import biocode.fims.utils.Flag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.UUID;

/**
 * @resourceTag Records
 */
@Controller
@Path("/records")
@Singleton
public class RecordController extends FimsController {

    private final RecordsResource recordsResource;
    private EvolutionService evolutionService;
    private EvolutionTaskExecutor taskExecutor;

    @Autowired
    RecordController(FimsProperties props, RecordsResource recordsResource, EvolutionService evolutionService, EvolutionTaskExecutor taskExecutor) {
        super(props);
        this.recordsResource = recordsResource;
        this.evolutionService = evolutionService;
        this.taskExecutor = taskExecutor;
    }

    /**
     * @responseType biocode.fims.rest.services.subResources.RecordsResource
     */
    @Path("/")
    public Class<RecordsResource> getRecordsResource() {
        return RecordsResource.class;
    }

    /**
     * Get a Record by ark id
     *
     * @param identifier The ark id of the Record to fetch
     * @responseMessage 400 Invalid request. The provided ark id is missing a suffix `biocode.fims.utils.ErrorInfo
     */
    @GET
    @Path("{identifier: ark:\\/[0-9]{5}\\/.+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public RecordResponse get(@QueryParam("includeChildren") @DefaultValue("false") Flag includeChildren,
                              @QueryParam("includeParent") @DefaultValue("false") Flag includeParent,
                              @PathParam("identifier") String arkID) {
        RecordResponse response = recordsResource.get(includeChildren, includeParent, arkID);
        EvolutionRecordReference reference = new EvolutionRecordReference((String) response.record.get("bcid"), UUID.randomUUID().toString());
        EvolutionRetrievalTask task = new EvolutionRetrievalTask(evolutionService, Collections.singletonList(reference));
        taskExecutor.addTask(task);
        return response;
    }

    /**
     * @responseType biocode.fims.rest.services.subResources.QueryController
     * @resourceDescription Query a project's records. See <a href='http://fims.readthedocs.io/en/latest/fims/query.html'>Fims Docs</a>
     * for more detailed information regarding queries.
     * @resourceTag Query Records
     */
    // note: we use the regex for entity here so the paths don't collide w/ RecordsResource get by arkID
    @Path("{entity: [a-zA-Z0-9_]+}")
    public Class<QueryController> getQueryController() {
        return QueryController.class;
    }
}


package biocode.fims.rest.services;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.bcid.Bcid;
import biocode.fims.plugins.evolution.models.EvolutionRecordReference;
import biocode.fims.plugins.evolution.processing.EvolutionRetrievalTask;
import biocode.fims.plugins.evolution.processing.EvolutionTaskExecutor;
import biocode.fims.plugins.evolution.service.EvolutionService;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.responses.RecordResponse;
import biocode.fims.rest.responses.RootIdentifierResponse;
import biocode.fims.rest.services.subResources.QueryController;
import biocode.fims.rest.services.subResources.RecordsResource;
import biocode.fims.rest.services.subResources.RootIdentifiersResource;
import biocode.fims.service.BcidService;
import biocode.fims.utils.Flag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.UUID;

/**
 * @resourceTag Root Identifier Controller resolves expeditions
 * This currently resolves expeditions but not Entities, which are directed by
 * EZID to /records
 */
@Controller
@Path("/bcids/metadata")
@Singleton
public class RootIdentifierController extends FimsController {
    private BcidService bcidService;

    private final RootIdentifiersResource rootIdentifierResource;

    @Autowired
    RootIdentifierController(FimsProperties props, RootIdentifiersResource rootIdentifierResource) {
        super(props);
        this.rootIdentifierResource = rootIdentifierResource;
    }

    /**
         * @responseType biocode.fims.rest.services.subResources.RecordsResource
         */
        @Path("/")
    public Class<RootIdentifiersResource> getRecordsResource() {
        return RootIdentifiersResource.class;
    }

    /**
     * Get rootIdentifiers by ARKID
     *
     * @param identifier The ark id of the Record to fetch
     * @responseMessage 400 Invalid request. The provided ark id is missing a suffix `biocode.fims.utils.ErrorInfo
     * Chris meyer's example: this is a sample entity
     * @Path("{identifier: ark:\\/[0-9]{5}\\/Caz2}")
     * A real expedition:
     * @Path("{identifier: ark:\\/[0-9]{5}\\/AEq2}")
     */
    @GET
    @Path("{identifier: ark:\\/[0-9]{5}\\/(.+?)[0-9]}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public RootIdentifierResponse get(@PathParam("identifier") String arkID) {
        RootIdentifierResponse response = rootIdentifierResource.get(arkID);
        return response;
    }
}


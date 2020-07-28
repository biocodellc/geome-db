package biocode.fims.rest.services.subResources;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.responses.ConfirmationResponse;
import biocode.fims.rest.responses.RecordResponse;
import biocode.fims.rest.responses.RootIdentifierResponse;
import biocode.fims.service.BcidService;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.RecordService;
import biocode.fims.service.RootIdentifierService;
import biocode.fims.utils.Flag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * @author RJ Ewing
 */
@Controller
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class RootIdentifiersResource extends FimsController {
    private final RootIdentifierService rootIdentifierService;
    @Autowired
    public RootIdentifiersResource(RootIdentifierService rootIdentifierService, FimsProperties props) {
        super(props);
        this.rootIdentifierService = rootIdentifierService;
    }

    /**
     * Get a Record by ark id
     *
     * @param identifier The ark id of the Record to fetch
     * @responseMessage 400 Invalid request. The provided ark id is missing a suffix `biocode.fims.utils.ErrorInfo
     */
    @GET
    @Path("{identifier: ark:\\/[0-9]{5}\\/(.+?)[0-9].+}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public  RootIdentifierResponse get(@PathParam("identifier") String arkID) {
        try {
            return rootIdentifierService.get(arkID);
        } catch (FimsRuntimeException e) {
            if (e.getErrorCode() == QueryCode.NO_RESOURCES) {
                return null;
            }
            throw e;
        }
    }
}

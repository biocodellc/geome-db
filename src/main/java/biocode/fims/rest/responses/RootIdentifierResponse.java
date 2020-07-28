package biocode.fims.rest.responses;

import biocode.fims.bcid.Identifier;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.GenericErrorCode;
import biocode.fims.models.EntityIdentifier;
import biocode.fims.models.Expedition;
import biocode.fims.repositories.EntityIdentifierRepository;
import biocode.fims.service.ExpeditionService;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.math3.analysis.function.Exp;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 * @author RJ Ewing
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RootIdentifierResponse {
    public final String arkID;
    private final Identifier identifier;
    private final ExpeditionService expeditionService;
    private final EntityIdentifierRepository entityIdentifierRepository;
    public final Expedition expedition;
    public final EntityIdentifier entityIdentifier;

    public RootIdentifierResponse(String arkID, ExpeditionService expeditionService, EntityIdentifierRepository entityIdentifierRepository) {
        this.arkID = arkID;
        this.identifier = parseIdentifier(arkID);
        this.expeditionService = expeditionService;
        this.expedition = expeditionService.getExpedition(identifier.getRootIdentifier());
        this.entityIdentifierRepository = entityIdentifierRepository;

        if (this.expedition == null) {
            this.entityIdentifier = getEntityIdentifier(identifier);
        }   else {
            this.entityIdentifier = null;
        }
    }
    private EntityIdentifier getEntityIdentifier(Identifier identifier) {
           EntityIdentifier entityIdentifier;
           try {
               entityIdentifier = entityIdentifierRepository.findByIdentifier(new URI(identifier.getRootIdentifier()));
           } catch (URISyntaxException e) {
               throw new FimsRuntimeException(GenericErrorCode.SERVER_ERROR, 500);
           }      catch (Exception e) {
               throw   new FimsRuntimeException(GenericErrorCode.SERVER_ERROR, 500);
           }
           return entityIdentifier;
       }
       
       private Identifier parseIdentifier(String arkID) {
           Identifier identifier;
           try {
               identifier = new Identifier(arkID);
           } catch (ArrayIndexOutOfBoundsException e) {
               throw new FimsRuntimeException(GenericErrorCode.BAD_REQUEST, 400, "Invalid identifier");
           }

           return identifier;
       }
       
       
}

package biocode.fims.service;

import biocode.fims.authorizers.QueryAuthorizer;
import biocode.fims.bcid.Identifier;
import biocode.fims.config.models.Entity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.fimsExceptions.errorCodes.GenericErrorCode;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.models.EntityIdentifier;
import biocode.fims.models.Project;
import biocode.fims.models.User;
import biocode.fims.query.QueryBuilder;
import biocode.fims.query.QueryResult;
import biocode.fims.query.dsl.*;
import biocode.fims.repositories.EntityIdentifierRepository;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.rest.responses.RecordResponse;
import biocode.fims.rest.responses.RootIdentifierResponse;
import biocode.fims.run.DatasetAuthorizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
@Service
public class RootIdentifierService {
    private final EntityIdentifierRepository entityIdentifierRepository;
    public final ExpeditionService expeditionService;

    @Autowired
    public RootIdentifierService(EntityIdentifierRepository entityIdentifierRepository,ExpeditionService expeditionService) {
        this.entityIdentifierRepository = entityIdentifierRepository;
        this.expeditionService = expeditionService;
    }

    public RootIdentifierResponse get(String arkID) {
        return new RootIdentifierResponse(arkID,expeditionService,entityIdentifierRepository);
    }
}

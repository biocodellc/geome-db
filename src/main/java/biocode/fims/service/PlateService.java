package biocode.fims.service;

import biocode.fims.application.config.GeomeProperties;
import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.Entity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.fimsExceptions.errorCodes.GenericErrorCode;
import biocode.fims.models.Project;
import biocode.fims.query.QueryBuilder;
import biocode.fims.query.QueryResult;
import biocode.fims.query.QueryResults;
import biocode.fims.query.dsl.ComparisonExpression;
import biocode.fims.query.dsl.ComparisonOperator;
import biocode.fims.query.dsl.Query;
import biocode.fims.query.dsl.SelectExpression;
import biocode.fims.records.Record;
import biocode.fims.records.RecordJoiner;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.tissues.Plate;
import biocode.fims.tissues.TissueRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author rjewing
 */
@Service
public class PlateService {

    private final TissueRepository tissueRepository;
    private final RecordRepository recordRepository;
    private final GeomeProperties props;

    public PlateService(TissueRepository tissueRepository, RecordRepository recordRepository, GeomeProperties props) {
        this.tissueRepository = tissueRepository;
        this.recordRepository = recordRepository;
        this.props = props;
    }

    public List<String> getPlates(Project project) {
        if (project == null) throw new ServerErrorException();

        Entity entity = getTissueEntity(project);

        // will throw exception if missing attribute
        entity.getAttributeByUri(props.tissuePlateUri());

        return tissueRepository.getPlates(
                project.getNetwork().getId(),
                project.getProjectId(),
                entity.getConceptAlias(),
                props.tissuePlateUri()
        );
    }

    public Plate getPlate(Project project, String plateName) {
        if (project == null) throw new ServerErrorException();

        ProjectConfig config = project.getProjectConfig();
        Entity entity = getTissueEntity(project);

        Attribute plateAttribute = entity.getAttributeByUri(props.tissuePlateUri());
        ComparisonExpression plateExp = new ComparisonExpression(plateAttribute.getColumn(), plateName, ComparisonOperator.EQUALS);
//        SelectExpression exp = new SelectExpression(entity.getParentEntity(), plateExp);
        QueryBuilder qb = new QueryBuilder(config, project.getNetwork().getId(), entity.getConceptAlias());
        Query query = new Query(qb, config, plateExp);

//        if (!queryAuthorizer.authorizedQuery(Collections.singletonList(projectId), new ArrayList<>(query.expeditions()), userContext.getUser())) {
//            throw new FimsRuntimeException(GenericErrorCode.UNAUTHORIZED, 403);
//        }

        QueryResults queryResults = recordRepository.query(query);

        if (queryResults.isEmpty()) return null;

//        QueryResult parentResults = queryResults.getResult(entity.getConceptAlias());
//        RecordJoiner joiner = new RecordJoiner(config, entity, queryResults);
        QueryResult tissues = queryResults.getResult(entity.getConceptAlias());

        Attribute wellAttribute = entity.getAttributeByUri(props.tissueWellUri());

        return Plate.fromRecords(wellAttribute.getColumn(), tissues.getAsRecord(false));
    }

    private Entity getTissueEntity(Project project) {
        ProjectConfig config = project.getProjectConfig();
        Entity entity = config.entity(props.tissueEntity());

        if (entity == null) {
            throw new BadRequestException("Project does not contain a \"" + props.tissueEntity() + "\" entity");
        }
        return entity;
    }
}

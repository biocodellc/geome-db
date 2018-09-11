package biocode.fims.service;

import biocode.fims.application.config.GeomeProperties;
import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.Entity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.fimsExceptions.errorCodes.ValidationCode;
import biocode.fims.models.Project;
import biocode.fims.models.User;
import biocode.fims.query.QueryBuilder;
import biocode.fims.query.QueryResult;
import biocode.fims.query.QueryResults;
import biocode.fims.query.dsl.*;
import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.run.DatasetProcessor;
import biocode.fims.run.ProcessorStatus;
import biocode.fims.tissues.Plate;
import biocode.fims.tissues.PlateResponse;
import biocode.fims.tissues.PlateRow;
import biocode.fims.tissues.TissueRepository;
import biocode.fims.validation.RecordValidatorFactory;
import biocode.fims.validation.messages.EntityMessages;
import org.apache.commons.collections.keyvalue.MultiKey;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author rjewing
 */
@Service
public class PlateService {

    private final TissueRepository tissueRepository;
    private final RecordRepository recordRepository;
    private final RecordValidatorFactory validatorFactory;
    private final GeomeProperties props;

    public PlateService(TissueRepository tissueRepository, RecordRepository recordRepository, RecordValidatorFactory validatorFactory, GeomeProperties props) {
        this.tissueRepository = tissueRepository;
        this.recordRepository = recordRepository;
        this.validatorFactory = validatorFactory;
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

        QueryResults queryResults = recordRepository.query(query);

        if (queryResults.isEmpty()) return null;

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

    private Entity getTissueParentEntity(Project project) {
        Entity entity = getTissueEntity(project);
        return project.getProjectConfig().entity(entity.getParentEntity());
    }

    public PlateResponse createPlate(User user, Project project, String plateName, Plate plate) {
        if (getPlate(project, plateName) != null) {
            throw new BadRequestException("A plate with that name already exists");
        }

        Entity entity = getTissueEntity(project);
        Entity parentEntity = getTissueParentEntity(project);

        Set<String> parentIdentifiers = new HashSet<>();
        Map<MultiKey, List<Record>> records = new HashMap<>();

        for (Map.Entry<PlateRow, Record[]> e : plate.getRows().entrySet()) {
            PlateRow row = e.getKey();

            int col = 1;
            for (Record r : e.getValue()) {
                if (r != null) {
                    r.set(props.tissuePlateUri(), plateName);
                    r.set(props.tissueWellUri(), row.toString() + col);

                    String parentIdentifier = r.get(parentEntity.getUniqueKey());

                    parentIdentifiers.add(parentIdentifier);

                    MultiKey k = new MultiKey(r.expeditionCode(), parentIdentifier);
                    records.computeIfAbsent(k, key -> new ArrayList<>()).add(r);
                }
                col++;
            }
        }

        if (records.isEmpty()) {
            throw new FimsRuntimeException(ValidationCode.INVALID_DATASET, 400);
        }

        // fetch existing sibling tissues
        Map<MultiKey, List<Record>> existingTissues = new HashMap<>();
        tissueRepository.getTissues(
                project.getNetwork().getId(),
                project.getProjectId(),
                entity.getConceptAlias(),
                new ArrayList<>(parentIdentifiers)
        )
                .forEach(r -> {
                    MultiKey k = new MultiKey(r.expeditionCode(), r.get(parentEntity.getUniqueKeyURI()));
                    existingTissues.computeIfAbsent(k, key -> new ArrayList<>()).add(r);
                });

        Map<String, RecordSet> recordSets = new HashMap<>();
        // generate a uniqueKey for each tissue in the plate
        for (Map.Entry<MultiKey, List<Record>> e : records.entrySet()) {
            MultiKey k = e.getKey();

            String expeditionCode = (String) k.getKey(0);

            RecordSet recordSet = recordSets.computeIfAbsent(expeditionCode, key -> new RecordSet(entity, false));

            List<Record> siblingTissues = existingTissues.getOrDefault(k, new ArrayList<>());
            for (Record r : e.getValue()) {
                r = transformProperties(entity, r);
                recordSet.add(r);
                if (!r.has(entity.getUniqueKeyURI())) {
                    r.set(entity.getUniqueKeyURI(), r.get(parentEntity.getUniqueKeyURI()) + "." + (siblingTissues.size() + 1));
                }
                siblingTissues.add(r);
            }
        }

        DatasetProcessor.Builder builder = new DatasetProcessor.Builder(project, null, new ProcessorStatus())
                .user(user)
                .recordRepository(recordRepository)
                .validatorFactory(validatorFactory)
                .ignoreUser(props.ignoreUser() || project.getUser().equals(user)) // projectAdmin can modify expedition data
                .serverDataDir(props.serverRoot())
                .uploadValid();

        recordSets.values().forEach(builder::addRecordSet);

        DatasetProcessor processor = builder.build();

        boolean isvalid = processor.validate();

        processor.upload();

        Plate p = getPlate(project, plateName);
        EntityMessages entityMessages = null;
        if (!isvalid) {
            entityMessages = processor.messages().stream()
                    .filter(em -> entity.getConceptAlias().equals(em.conceptAlias()))
                    .findFirst()
                    .orElse(null);
        }

        return new PlateResponse(p, entityMessages);
    }

    /**
     * attempts to map any properties using the entity Attributes uri if possible
     *
     * @param entity entity to map the Record properties against
     * @param r
     * @return new Record instance w/ mapped properties
     */
    private Record transformProperties(Entity entity, Record r) {
        Map<String, String> props = new HashMap<>();

        for (String key : r.properties().keySet()) {
            String uri = entity.getAttributeUri(key);
            if (uri == null) {
                props.put(key, r.get(key));
            } else {
                props.put(uri, r.get(key));
            }
        }

        return new GenericRecord(props, r.rootIdentifier(), r.projectId(), r.expeditionCode(), r.persist());
    }
}

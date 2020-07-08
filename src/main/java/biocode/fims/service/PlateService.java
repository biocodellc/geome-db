package biocode.fims.service;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.bcid.BcidBuilder;
import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.Entity;
import biocode.fims.config.models.TissueEntity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.fimsExceptions.errorCodes.ValidationCode;
import biocode.fims.models.Project;
import biocode.fims.models.User;
import biocode.fims.query.QueryBuilder;
import biocode.fims.query.QueryResults;
import biocode.fims.query.dsl.*;
import biocode.fims.reader.DataConverterFactory;
import biocode.fims.records.*;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.repositories.TissueRepository;
import biocode.fims.run.DatasetAction;
import biocode.fims.run.DatasetAuthorizer;
import biocode.fims.run.DatasetProcessor;
import biocode.fims.run.ProcessorStatus;
import biocode.fims.tissues.*;
import biocode.fims.validation.RecordValidatorFactory;
import biocode.fims.validation.messages.EntityMessages;
import org.apache.commons.collections.keyvalue.MultiKey;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
@Service
public class PlateService {
    // TODO replace w/ Tissue EntityProps
    public final static String TISSUE_PLATE_URI = "urn:plateID";
    public final static String TISSUE_WELL_URI = "urn:wellID";
    private final static String TISSUE_ENTITY = "Tissue";

    private final TissueRepository tissueRepository;
    private final RecordRepository recordRepository;
    private final RecordValidatorFactory validatorFactory;
    private final DatasetAuthorizer datasetAuthorizer;
    private final List<DatasetAction> datasetActions;
    private final DataConverterFactory dataConverterFactory;
    private final FimsProperties props;

    public PlateService(TissueRepository tissueRepository, RecordRepository recordRepository, DataConverterFactory dataConverterFactory,
                        RecordValidatorFactory validatorFactory, DatasetAuthorizer datasetAuthorizer, List<DatasetAction> datasetActions,
                        FimsProperties props) {
        this.tissueRepository = tissueRepository;
        this.recordRepository = recordRepository;
        this.dataConverterFactory = dataConverterFactory;
        this.validatorFactory = validatorFactory;
        this.datasetAuthorizer = datasetAuthorizer;
        this.datasetActions = datasetActions;
        this.props = props;
    }

    public List<String> getPlates(Project project) {
        if (project == null) throw new ServerErrorException();

        Entity entity = getTissueEntity(project);

        // will throw exception if missing attribute
        entity.getAttributeByUri(TISSUE_PLATE_URI);

        return tissueRepository.getPlates(
                project.getNetwork().getId(),
                project.getProjectId(),
                entity.getConceptAlias(),
                TISSUE_PLATE_URI
        );
    }

    public Plate getPlate(Project project, String plateName) {
        if (project == null) throw new ServerErrorException();

        ProjectConfig config = project.getProjectConfig();
        Entity entity = getTissueEntity(project);

        Attribute plateAttribute = entity.getAttributeByUri(TISSUE_PLATE_URI);
        ProjectExpression projectExpression = new ProjectExpression(Collections.singletonList(project.getProjectId()));
        ComparisonExpression plateExp = new ComparisonExpression(plateAttribute.getColumn(), plateName, ComparisonOperator.EQUALS);
        LogicalExpression logicalExpression = new LogicalExpression(LogicalOperator.AND, projectExpression, plateExp);
        SelectExpression exp = new SelectExpression(entity.getParentEntity(), logicalExpression);
        QueryBuilder qb = new QueryBuilder(config, project.getNetwork().getId(), entity.getConceptAlias());
        Query query = new Query(qb, config, exp);


        QueryResults queryResults = recordRepository.query(query);

        if (queryResults.isEmpty()) return null;

        Entity parentEntity = getTissueParentEntity(project);
        BcidBuilder bcidBuilder = new BcidBuilder(entity, parentEntity, "");

        ArrayList<Attribute> attributes = new ArrayList<>(entity.getAttributes());
        attributes.addAll(parentEntity.getAttributes());

        RecordMapper recordMapper = new RecordMapper(bcidBuilder, attributes, false);
        RecordJoiner joiner = new RecordJoiner(config, entity, queryResults);

        List<Record> tissues = queryResults.getResult(entity.getConceptAlias())
                .records().stream()
                .map(joiner::joinRecords)
                .map(recordMapper::mapAsRecord)
                .collect(Collectors.toList());

        Attribute wellAttribute = entity.getAttributeByUri(TISSUE_WELL_URI);

        return Plate.fromRecords(wellAttribute.getColumn(), tissues);
    }

    private Entity getTissueEntity(Project project) {
        ProjectConfig config = project.getProjectConfig();
        Entity entity = config.entity(TissueEntity.CONCEPT_ALIAS);

        if (entity == null) {
            throw new BadRequestException("Project does not contain a \"" + TISSUE_ENTITY + "\" entity");
        }
        return entity;
    }

    private Entity getTissueParentEntity(Project project) {
        Entity entity = getTissueEntity(project);
        return project.getProjectConfig().entity(entity.getParentEntity());
    }

    public PlateResponse update(User user, Project project, Plate plate) {
        Plate p = getPlate(project, plate.name());

        if (p == null) {
            throw new BadRequestException("That plate does not exits");
        }

        PlateTissues plateTissues = getPlateTissues(project, plate);

        if (plateTissues.newTissues().isEmpty()) {
            // no tissues to create
            return new PlateResponse(p, null);
        }

        return save(user, plateTissues);
    }

    public PlateResponse create(User user, Project project, Plate plate) {
        if (getPlate(project, plate.name()) != null) {
            throw new BadRequestException("A plate with that name already exists");
        }

        PlateTissues plateTissues = getPlateTissues(project, plate);

        if (plateTissues.newTissues().isEmpty()) {
            throw new FimsRuntimeException(ValidationCode.INVALID_DATASET, 400);
        }

        return save(user, plateTissues);
    }

    private PlateTissues getPlateTissues(Project project, Plate plate) {
        Entity entity = getTissueEntity(project);
        Entity parentEntity = getTissueParentEntity(project);

        return new PlateTissues.Builder()
                .entity(entity)
                .parentEntity(parentEntity)
                .plate(plate)
                .project(project)
                .build();
    }

    private PlateResponse save(User user, PlateTissues plateTissues) {
        Project project = plateTissues.project();

        Map<MultiKey, List<Record>> siblingTissues = getSiblingTissues(plateTissues);
        Map<String, RecordSet> recordSets = plateTissues.createRecordSets(siblingTissues);

        DatasetProcessor.Builder builder = new DatasetProcessor.Builder(project, null, new ProcessorStatus())
                .user(user)
                .recordRepository(recordRepository)
                .validatorFactory(validatorFactory)
                .dataConverterFactory(dataConverterFactory)
                .datasetAuthorizer(datasetAuthorizer)
                .datasetActions(datasetActions)
                .serverDataDir(props.serverRoot())
                .uploadValid();

        recordSets.values().forEach(builder::addRecordSet);

        DatasetProcessor processor = builder.build();

        boolean isvalid = processor.validate();

        processor.upload();

        Plate p = getPlate(project, plateTissues.name());
        EntityMessages entityMessages = null;
        if (!isvalid) {
            entityMessages = processor.messages().stream()
                    .filter(em -> plateTissues.entity().getConceptAlias().equals(em.conceptAlias()))
                    .findFirst()
                    .orElse(null);
        }

        return new PlateResponse(p, entityMessages);
    }

    /**
     * Fetch existing sibling tissues
     *
     * @param plateTissues
     * @return
     */
    private Map<MultiKey, List<Record>> getSiblingTissues(PlateTissues plateTissues) {
        Project project = plateTissues.project();
        Map<MultiKey, List<Record>> existingTissues = new HashMap<>();

        tissueRepository.getTissues(
                project.getNetwork().getId(),
                project.getProjectId(),
                plateTissues.entity().getConceptAlias(),
                new ArrayList<>(plateTissues.parentIdentifiers())
        )
                .forEach(r -> {
                    MultiKey k = new MultiKey(r.expeditionCode(), r.get(plateTissues.parentEntity().getUniqueKeyURI()));
                    existingTissues.computeIfAbsent(k, key -> new ArrayList<>()).add(r);
                });

        return existingTissues;
    }


}

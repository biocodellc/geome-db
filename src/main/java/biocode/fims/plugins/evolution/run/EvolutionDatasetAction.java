package biocode.fims.plugins.evolution.run;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.bcid.BcidBuilder;
import biocode.fims.models.EntityIdentifier;
import biocode.fims.models.Project;
import biocode.fims.plugins.evolution.application.config.EvolutionProperties;
import biocode.fims.plugins.evolution.processing.EvolutionTaskExecutor;
import biocode.fims.plugins.evolution.processing.EvolutionUpdateCreateTask;
import biocode.fims.plugins.evolution.service.EvolutionService;
import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import biocode.fims.repositories.EntityIdentifierRepository;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.run.Dataset;
import biocode.fims.run.DatasetAction;
import biocode.fims.service.ExpeditionService;
import biocode.fims.utils.RecordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
public class EvolutionDatasetAction implements DatasetAction {
    private static final Logger logger = LoggerFactory.getLogger(EvolutionDatasetAction.class);

    private final RecordRepository recordRepository;
    private final EvolutionService evolutionService;
    private final ExpeditionService expeditionService;
    private final EvolutionTaskExecutor taskExecutor;
    private final EntityIdentifierRepository entityIdentifierRepository;
    private final EvolutionProperties evolutionProperties;
    private final FimsProperties props;

    public EvolutionDatasetAction(RecordRepository recordRepository, EvolutionService evolutionService,
                                  ExpeditionService expeditionService, EvolutionTaskExecutor taskExecutor, EntityIdentifierRepository entityIdentifierRepository,
                                  EvolutionProperties evolutionProperties, FimsProperties props) {
        this.recordRepository = recordRepository;
        this.evolutionService = evolutionService;
        this.expeditionService = expeditionService;
        this.taskExecutor = taskExecutor;
        this.entityIdentifierRepository = entityIdentifierRepository;
        this.evolutionProperties = evolutionProperties;
        this.props = props;
    }

    @Override
    public void onSave(Project project, Dataset dataset) {
        String resolverEndpoint = evolutionProperties.resolverEndpoint();
        try {
            dataset.stream()
                    .filter(RecordSet::hasRecordToPersist)
                    .forEach(recordSet -> {
                        Map<String, Record> records = getExistingRecords(project, recordSet);

                        List<Record> newRecords = new ArrayList<>();
                        List<Record> updatedRecords = new ArrayList<>();

                        String uniqueKey = recordSet.entity().getUniqueKeyURI();
                        EntityIdentifier entityIdentifier = entityIdentifierRepository.findByConceptAliasAndExpeditionExpeditionCode(
                                recordSet.conceptAlias(),
                                recordSet.expeditionCode()
                        );

                        recordSet.recordsToPersist().forEach(record -> {
                            String localIdentifier = record.get(uniqueKey);
                            record.setRootIdentifier(entityIdentifier.getIdentifier().toString());

                            if (!records.containsKey(localIdentifier)) {
                                newRecords.add(record);
                                return;
                            }

                            String oldHash = RecordHasher.hash(records.get(localIdentifier));
                            String newHash = RecordHasher.hash(record);

                            // only consider this an update if the Record has changed
                            if (oldHash.equals(newHash)) return;

                            updatedRecords.add(record);
                        });

                        // Submit a task here to communicate with the Evolution API asynchronously.
                        BcidBuilder bcidBuilder = new BcidBuilder(recordSet.entity(), recordSet.hasParent() ? recordSet.parent().entity() : null, props.bcidResolverPrefix());
                        RecordSet parent = recordSet.parent();
                        BcidBuilder parentBcidBuilder = recordSet.hasParent() ? new BcidBuilder(parent.entity(), parent.hasParent() ? parent.parent().entity() : null, props.bcidResolverPrefix()) : null;
                        EvolutionUpdateCreateTask task = new EvolutionUpdateCreateTask(
                                evolutionService,
                                expeditionService,
                                bcidBuilder,
                                newRecords,
                                updatedRecords,
                                recordSet.parent(),
                                parentBcidBuilder,
                                resolverEndpoint,
                                props.userURIPrefix()
                        );
                        taskExecutor.addTask(task);
                    });
        } catch (Exception e) {
            // Never prevent the main upload from happening
            logger.error("EvolutionDatasetAction failed", e);
        }
    }

    private Map<String, Record> getExistingRecords(Project project, RecordSet recordSet) {
        String uniqueKey = recordSet.entity().getUniqueKeyURI();
        List<String> localIdentifiers = recordSet.recordsToPersist().stream()
                .map(record -> record.get(uniqueKey))
                .collect(Collectors.toList());

        Map<String, Record> records = new HashMap<>();

        recordRepository.getRecords(
                project,
                recordSet.expeditionCode(),
                recordSet.entity().getConceptAlias(),
                localIdentifiers,
                GenericRecord.class
        ).forEach(record -> records.put(record.get(uniqueKey), record));

        return records;
    }
}

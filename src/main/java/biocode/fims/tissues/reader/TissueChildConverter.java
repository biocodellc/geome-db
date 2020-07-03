package biocode.fims.tissues.reader;

import biocode.fims.config.models.Entity;
import biocode.fims.config.models.TissueEntity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import biocode.fims.reader.DataConverter;
import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import biocode.fims.tissues.TissueProps;
import biocode.fims.repositories.TissueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * DataConverter for Tissue Children Entities. If the Tissue entity is set to generateId
 * then any child data uploaded will be tied to
 *
 * @author rjewing
 */
public class TissueChildConverter implements DataConverter {
    private final static Logger logger = LoggerFactory.getLogger(TissueChildConverter.class);

    private final TissueRepository tissueRepository;
    protected ProjectConfig config;

    private Map<String, List<Record>> tissuesByParentId;
    private Map<String, Record> tissuesById;

    public TissueChildConverter(TissueRepository tissueRepository) {
        this.tissueRepository = tissueRepository;
    }

    private TissueChildConverter(TissueRepository tissueRepository, ProjectConfig projectConfig) {
        this.tissueRepository = tissueRepository;
        this.config = projectConfig;
    }

    @Override
    public void convertRecordSet(RecordSet recordSet, int networkId) {
        Entity parentEntity = config.entity(recordSet.entity().getParentEntity());

        if (!parentEntity.getConceptAlias().equals(TissueEntity.CONCEPT_ALIAS)) {
            throw new FimsRuntimeException(DataReaderCode.READ_ERROR, 500);
        }

        if (!((TissueEntity) parentEntity).isGenerateID() || recordSet.recordsToPersist().size() == 0) return;

        String parentKey = config.entity(parentEntity.getParentEntity()).getUniqueKeyURI();

        tissuesByParentId = new HashMap<>();
        tissuesById = new HashMap<>();

        RecordSet tissues = recordSet.parent();
        for (Record tissue : tissues.records()) {
            tissuesById.put(tissue.get(TissueProps.IDENTIFIER.uri()), tissue);
            tissuesByParentId.computeIfAbsent(tissue.get(parentKey), k -> new ArrayList<>()).add(tissue);
        }

        updateRecords(recordSet);
    }

    /**
     * Update any Tissue identifiers on the child record if there is only a single child Tissue loaded.
     *
     * @param recordSet
     */
    private void updateRecords(RecordSet recordSet) {
        for (Record child : recordSet.recordsToPersist()) {

            // this could be both the Tissue identifier or the Sample identifier
            String identifier = child.get(TissueProps.IDENTIFIER.uri());

            // If we don't have a tissue by this id, then this identifier maybe the Sample
            // identifier. Since Tissue.generateId = true, then we will update the child
            // record, setting the Tissue identifier = the generated tissue id if possible.
            // This is only possible if there is a single tissue for a given parent. If there
            // are multiple tissues, then we don't know which tissue this child is attached to
            if (identifier != null && !tissuesById.containsKey(identifier)
                    && tissuesByParentId.getOrDefault(identifier, Collections.emptyList()).size() == 1) {
                Record tissue = tissuesByParentId.get(identifier).get(0);

                Record newChild = child.clone();
                newChild.set(TissueProps.IDENTIFIER.uri(), tissue.get(TissueProps.IDENTIFIER.uri()));

                recordSet.remove(child);
                recordSet.add(newChild);
            }
        }
    }

    @Override
    public DataConverter newInstance(ProjectConfig projectConfig) {
        return new TissueChildConverter(tissueRepository, projectConfig);
    }
}

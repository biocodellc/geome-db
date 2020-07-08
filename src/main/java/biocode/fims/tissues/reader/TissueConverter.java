package biocode.fims.tissues.reader;

import biocode.fims.config.models.Entity;
import biocode.fims.config.models.TissueEntity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import biocode.fims.reader.DataConverter;
import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import biocode.fims.tissues.TissueProps;
import biocode.fims.repositories.TissueRepository;
import biocode.fims.utils.RecordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
public class TissueConverter implements DataConverter {
    private final static Logger logger = LoggerFactory.getLogger(TissueConverter.class);

    private final TissueRepository tissueRepository;
    protected ProjectConfig config;

    private String parentKey;
    private Map<String, Integer> existingTissuesByParentId;
    private Map<String, Record> existingTissuesByHash;

    public TissueConverter(TissueRepository tissueRepository) {
        this.tissueRepository = tissueRepository;
    }

    private TissueConverter(TissueRepository tissueRepository, ProjectConfig projectConfig) {
        this.tissueRepository = tissueRepository;
        this.config = projectConfig;
    }

    @Override
    public void convertRecordSet(RecordSet recordSet, int networkId) {
        if (!(recordSet.entity() instanceof TissueEntity)) {
            throw new FimsRuntimeException(DataReaderCode.READ_ERROR, 500);
        }

        TissueEntity tissueEntity = (TissueEntity) recordSet.entity();

        if (!tissueEntity.isGenerateID()) return;

        if (!tissueEntity.isGenerateEmptyTissue()) {
            removeEmptyTissues(recordSet);
        }

        if (recordSet.recordsToPersist().size() == 0) return;

        String parent = tissueEntity.getParentEntity();
        parentKey = config.entity(parent).getUniqueKeyURI();

        existingTissuesByParentId = new HashMap<>();
        existingTissuesByHash = new HashMap<>();
        Map<String, Integer> existingTissuesByParentIdCount = new HashMap<>();

        if (!recordSet.reload()) {
            getExistingRecords(recordSet, networkId, parentKey).stream()
                    .filter(r -> !r.get(TissueProps.IDENTIFIER.uri()).equals(""))
                    .forEach(r -> {
                        String parentID = r.get(parentKey);

                        // we get the max here so we don't create duplicates if a tissue has been deleted
                        // if id is of form parentIdentifier.[0-9] we parse the digit and update max if
                        // necessary
                        int count = existingTissuesByParentIdCount.getOrDefault(parentID, 0);
                        int max = existingTissuesByParentId.getOrDefault(parentID, count);

                        Pattern p = Pattern.compile(parentID + "\\.(\\d+)");
                        Matcher matcher = p.matcher(r.get(TissueProps.IDENTIFIER.uri()));
                        if (matcher.matches()) {
                            Integer i = Integer.parseInt(matcher.group(1));
                            if (i > max) max = i;
                        }

                        if (!recordSet.reload() && count >= max) max = count + 1;

                        existingTissuesByParentIdCount.put(parentID, ++count);
                        existingTissuesByParentId.put(parentID, max);

                        Map<String, Object> props = new HashMap<>(r.properties());
                        props.remove(TissueProps.IDENTIFIER.uri());

                        Record record = new GenericRecord(props);
                        // store record hashes w/o tissueID so we can compare values before generating the tissue
                        existingTissuesByHash.put(RecordHasher.hash(record), r);
                    });
        }

        updateRecords(recordSet);
    }

    /**
     * A Tissue is considered empty if the tissue contains only the Tissue.uniqueKey and/or the Tissue.parentUniqueKey
     *
     * @param recordSet
     * @return
     */
    private void removeEmptyTissues(RecordSet recordSet) {
        Entity entity = recordSet.entity();
        Entity parentEntity = config.entity(entity.getParentEntity());
        boolean generateID = ((TissueEntity) entity).isGenerateID();

        for (Record r : recordSet.recordsToPersist()) {
            boolean isEmpty = true;
            for (Map.Entry<String, Object> entry : r.properties().entrySet()) {

                // don't use ID attributes & empty values to determine if a tissue is empty
                if (entry.getValue().equals("") ||
                        // if generateID = true & entity.getUniqueKey is present, then we
                        // should create this tissue
                        (!generateID && entry.getKey().equals(entity.getUniqueKeyURI())) ||
                        entry.getKey().equals(parentEntity.getUniqueKeyURI())) {
                    continue;
                }

                isEmpty = false;
                break;
            }

            if (isEmpty) recordSet.remove(r);
        }
    }

    /**
     * Generate unique tissueIDs for each record
     *
     * @param recordSet
     */
    private void updateRecords(RecordSet recordSet) {

        Map<String, Boolean> createdTissues = new HashMap<>();

        for (Record r : recordSet.recordsToPersist()) {

            if (r.get(TissueProps.IDENTIFIER.uri()).equals("")) {
                // check the hash of the Tissue w/o the identifier included
                // if we have an existing tissue w/ a matching hash, then this
                // is treated as an update to the existing tissue, otherwise
                // we create a identifier for the new tissue
                String parentID = r.get(parentKey);
                String hash = RecordHasher.hash(r);
                Record existingRecord = existingTissuesByHash.get(hash);
                Record newTissue = null;

                if (existingRecord == null) {
                    int count = existingTissuesByParentId.getOrDefault(parentID, 0);
                    count += 1;

                    newTissue = r.clone();
                    newTissue.set(TissueProps.IDENTIFIER.uri(), parentID + "." + count);

                    existingTissuesByParentId.put(parentID, count);
                    existingTissuesByHash.put(hash, newTissue);
                    createdTissues.put(hash, true);
                } else if (!(createdTissues.containsKey(hash) && r.properties().size() == 1 && !parentID.equals(""))) {
                    // we want to exclude any duplicate tissues which contain only parentID
                    // this will happen if a duplicate sample is placed on a spreadsheet
                    // or if an empty tissue has already been created & the sample is updated
                    // so we remove the new empty tissue
                    recordSet.remove(r);
                }

                if (newTissue != null) {
                    recordSet.remove(r);
                    recordSet.add(newTissue);
                }
            }
        }
    }

    /**
     * fetch any existing records for that are in the given RecordSet
     *
     * @param recordSet
     * @param networkId
     * @param parentKey
     * @return
     */
    private List<Record> getExistingRecords(RecordSet recordSet, int networkId, String parentKey) {
        if (networkId == 0 || recordSet.expeditionCode() == null) {
            throw new FimsRuntimeException(DataReaderCode.READ_ERROR, 500);
        }

        List<String> parentIdentifiers = recordSet.recordsToPersist().stream()
                .map(r -> r.get(parentKey))
                .distinct()
                .collect(Collectors.toList());

        return tissueRepository.getTissues(networkId, recordSet.projectId(), recordSet.conceptAlias(), parentIdentifiers)
                .parallelStream()
                .filter(t -> Objects.equals(recordSet.expeditionCode(), t.expeditionCode()))
                .collect(Collectors.toList());
    }

    @Override
    public DataConverter newInstance(ProjectConfig projectConfig) {
        return new TissueConverter(tissueRepository, projectConfig);
    }
}

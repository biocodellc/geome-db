package biocode.fims.fastq.reader;

import biocode.fims.config.models.FastqEntity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fastq.FastqProps;
import biocode.fims.fastq.FastqRepository;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import biocode.fims.reader.DataConverter;
import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import biocode.fims.utils.RecordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
public class FastqConverter implements DataConverter {
    private final static Logger logger = LoggerFactory.getLogger(FastqConverter.class);

    private final FastqRepository fastqRepository;
    protected ProjectConfig config;

    private String parentKey;
    private Map<String, Integer> existingFastqByParentId;
    private Map<String, Record> existingFastqByHash;

    public FastqConverter(FastqRepository fastqRepository) {
        this.fastqRepository = fastqRepository;
    }

    private FastqConverter(FastqRepository fastqRepository, ProjectConfig projectConfig) {
        this.fastqRepository = fastqRepository;
        this.config = projectConfig;
    }

    @Override
    public void convertRecordSet(RecordSet recordSet, int networkId) {
        if (!(recordSet.entity() instanceof FastqEntity)) {
            throw new FimsRuntimeException(DataReaderCode.READ_ERROR, 500);
        }

        FastqEntity fastqEntity = (FastqEntity) recordSet.entity();

        if (recordSet.recordsToPersist().size() == 0) return;

        String parent = fastqEntity.getParentEntity();
        parentKey = config.entity(parent).getUniqueKeyURI();

        existingFastqByParentId = new HashMap<>();
        existingFastqByHash = new HashMap<>();
        Map<String, Integer> existingFastqByParentIdCount = new HashMap<>();

        if (!recordSet.reload()) {
            getExistingRecords(recordSet, networkId, parentKey).stream()
                    .filter(r -> !r.get(FastqProps.IDENTIFIER.uri()).equals(""))
                    .forEach(r -> {
                        String parentID = r.get(parentKey);

                        // we get the max here so we don't create duplicates if a record has been deleted
                        // if id is of form parentIdentifier.[0-9] we parse the digit and update max if
                        // necessary
                        int count = existingFastqByParentIdCount.getOrDefault(parentID, 0);
                        int max = existingFastqByParentId.getOrDefault(parentID, count);

                        Pattern p = Pattern.compile(parentID + "\\.(\\d+)");
                        Matcher matcher = p.matcher(r.get(FastqProps.IDENTIFIER.uri()));
                        if (matcher.matches()) {
                            Integer i = Integer.parseInt(matcher.group(1));
                            if (i > max) max = i;
                        }

                        if (!recordSet.reload() && count >= max) max = count + 1;

                        existingFastqByParentIdCount.put(parentID, ++count);
                        existingFastqByParentId.put(parentID, max);

                        Map<String, Object> props = new HashMap<>(r.properties());
                        props.remove(FastqProps.IDENTIFIER.uri());

                        Record record = new GenericRecord(props);
                        // store record hashes w/o identifier so we can compare values before generating the new record
                        existingFastqByHash.put(RecordHasher.hash(record), r);
                    });
        }

        updateRecords(recordSet);
    }

    /**
     * Generate unique identifiers for each record
     *
     * @param recordSet
     */
    private void updateRecords(RecordSet recordSet) {

        Map<String, Boolean> createdRecords = new HashMap<>();

        for (Record r : recordSet.recordsToPersist()) {

            if (r.get(FastqProps.IDENTIFIER.uri()).equals("")) {
                // check the hash of the record w/o the identifier included
                // if we have an existing record w/ a matching hash, then this
                // is treated as an update to the existing record, otherwise
                // we create a identifier for the new record
                String parentID = r.get(parentKey);
                String hash = RecordHasher.hash(r);
                Record existingRecord = existingFastqByHash.get(hash);
                Record newRecord = null;

                if (existingRecord == null) {
                    int count = existingFastqByParentId.getOrDefault(parentID, 0);
                    count += 1;

                    newRecord = r.clone();
                    newRecord.set(FastqProps.IDENTIFIER.uri(), parentID + "." + count);

                    existingFastqByParentId.put(parentID, count);
                    existingFastqByHash.put(hash, newRecord);
                    createdRecords.put(hash, true);
                } else if (!(createdRecords.containsKey(hash) && r.properties().size() == 1 && !parentID.equals(""))) {
                    // we want to exclude any duplicate records which contain only parentID
                    // this will happen if a duplicate sample is placed on a spreadsheet
                    newRecord = r.clone();
                    newRecord.set(FastqProps.IDENTIFIER.uri(), existingRecord.get(FastqProps.IDENTIFIER.uri()));
                }

                if (newRecord != null) {
                    recordSet.remove(r);
                    recordSet.add(newRecord);
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

        return fastqRepository.getRecords(networkId, recordSet.projectId(), recordSet.conceptAlias(), parentIdentifiers)
                .parallelStream()
                .filter(f -> Objects.equals(recordSet.expeditionCode(), f.expeditionCode()))
                .collect(Collectors.toList());
    }

    @Override
    public DataConverter newInstance(ProjectConfig projectConfig) {
        return new FastqConverter(fastqRepository, projectConfig);
    }
}

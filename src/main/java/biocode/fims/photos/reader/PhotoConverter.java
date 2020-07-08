package biocode.fims.photos.reader;

import biocode.fims.application.config.PhotosSql;
import biocode.fims.config.models.PhotoEntity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import biocode.fims.records.GenericRecord;
import biocode.fims.records.GenericRecordRowMapper;
import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import biocode.fims.photos.PhotoEntityProps;
import biocode.fims.photos.PhotoRecord;
import biocode.fims.query.PostgresUtils;
import biocode.fims.reader.DataConverter;
import biocode.fims.repositories.RecordRepository;
import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
public class PhotoConverter implements DataConverter {
    private final static Logger logger = LoggerFactory.getLogger(PhotoConverter.class);
    private final PhotosSql photosSql;
    private final RecordRepository recordRepository;
    protected File file;
    protected ProjectConfig config;

    private Map<MultiKey, PhotoRecord> existingRecords;
    private String parentKey;
    private HashMap<String, Integer> existingPhotosByParentId;

    public PhotoConverter(PhotosSql photosSql, RecordRepository recordRepository) {
        this.photosSql = photosSql;
        this.recordRepository = recordRepository;
    }

    public PhotoConverter(PhotosSql photosSql, RecordRepository recordRepository, ProjectConfig projectConfig) {
        this(photosSql, recordRepository);
        this.config = projectConfig;
    }

    @Override
    public void convertRecordSet(RecordSet recordSet, int networkId) {
        String parent = recordSet.entity().getParentEntity();
        parentKey = config.entity(parent).getUniqueKeyURI();

        existingRecords = new HashMap<>();
        existingPhotosByParentId = new HashMap<>();
        Map<String, Integer> existingPhotosByParentIdCount = new HashMap<>();

        Map<MultiKey, Boolean> ids = new HashMap<>();

        recordSet.recordsToPersist().parallelStream()
                .filter(r -> !((PhotoRecord) r).photoID().equals(""))
                .forEach(r -> ids.put(new MultiKey(r.get(parentKey), ((PhotoRecord) r).photoID()), true));

        getExistingRecords(recordSet, networkId, parentKey)
                .forEach(r -> {
                    MultiKey key = new MultiKey(r.get(parentKey), r.photoID());
                    if (ids.containsKey(key)) existingRecords.put(key, r);

                    String parentID = r.get(parentKey);

                    // we get the max here so we don't create duplicates if a tissue has been deleted
                    // if id is of form photo[0-9] we parse the digit and update max if
                    // necessary
                    int count = existingPhotosByParentIdCount.getOrDefault(parentID, 0);
                    int max = existingPhotosByParentId.getOrDefault(parentID, count);

                    Pattern p = Pattern.compile(parentID + "_photo_(\\d+)");
                    Matcher matcher = p.matcher(r.photoID());
                    if (matcher.matches()) {
                        Integer i = Integer.parseInt(matcher.group(1));
                        if (i > max) max = i;
                    }

                    if (!recordSet.reload() && count >= max) max = count + 1;

                    existingPhotosByParentIdCount.put(parentID, ++count);
                    existingPhotosByParentId.put(parentID, max);
                });

        updateRecords(recordSet);
    }

    /**
     * Preserve processing data if the record already exists.
     * <p>
     * This is necessary because we do additional processing on photos and need to preserve some data.
     *
     * @param recordSet
     */
    private void updateRecords(RecordSet recordSet) {
        boolean generateId = ((PhotoEntity) recordSet.entity()).isGenerateID();

        for (Record r : recordSet.recordsToPersist()) {
            PhotoRecord record = (PhotoRecord) r;

            // generateId if necessary
            if (generateId && record.photoID().equals("")) {
                String parentID = r.get(parentKey);
                int count = existingPhotosByParentId.getOrDefault(parentID, 0);
                count += 1;

                Record newPhoto = r.clone();
                newPhoto.set(PhotoEntityProps.PHOTO_ID.uri(), parentID + "_photo_" + count);

                existingPhotosByParentId.put(parentID, count);
                recordSet.remove(r);
                recordSet.add(newPhoto);
                record = (PhotoRecord) newPhoto;
            }

            PhotoRecord existing = existingRecords.get(new MultiKey(record.get(parentKey), record.photoID()));

            if (existing != null) {
                // delete bulk loaded file for existing record if it exists
                if (existing.bulkLoad() && !existing.bulkLoadFile().equals(record.bulkLoadFile())) {
                    // this happens if a file is bulk loaded again before the 1st has processed
                    record.set(PhotoEntityProps.PROCESSED.uri(), "false");
                    try {
                        File img = new File(existing.bulkLoadFile());
                        img.delete();
                    } catch (Exception exp) {
                        logger.debug("Failed to delete bulk loaded img file", exp);
                    }
                } else if (Objects.equals(record.originalUrl(), existing.originalUrl()) && !existing.hasError()) {
                    // if the originalUrl is the same and the existing photo was successfully processed, copy a few existing props
                    // TODO possibly need to persist more data?
                    boolean newBulkLoad = record.bulkLoad();
                    for (PhotoEntityProps p : PhotoEntityProps.values()) {
                        // don't overwrite the BULK_LOAD_FILE or PROCESSED if this is a new bulk load
                        if (newBulkLoad && (p.equals(PhotoEntityProps.BULK_LOAD_FILE) || p.equals(PhotoEntityProps.PROCESSED)))
                            continue;
                        // allow Filename to be changed
                        if (p.equals(PhotoEntityProps.FILENAME)) continue;
                        record.set(p.uri(), existing.get(p.uri()));
                    }
                } else if (record.bulkLoad() || !record.originalUrl().equals("")) {
                    record.set(PhotoEntityProps.PROCESSED.uri(), "false");
                }
            } else {
                record.set(PhotoEntityProps.PROCESSED.uri(), "false");
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
    private List<PhotoRecord> getExistingRecords(RecordSet recordSet, int networkId, String parentKey) {
        if (networkId == 0 || recordSet.expeditionCode() == null) {
            throw new FimsRuntimeException(DataReaderCode.READ_ERROR, 500);
        }

        String sql = photosSql.getRecords();
        Map<String, String> tableMap = new HashMap<>();
        tableMap.put("table", PostgresUtils.entityTable(networkId, recordSet.entity().getConceptAlias()));

        List<String> parentIdentifiers = recordSet.recordsToPersist().stream()
                .map(r -> r.get(parentKey))
                .distinct()
                .collect(Collectors.toList());


        MapSqlParameterSource p = new MapSqlParameterSource();
        p.addValue("idList", parentIdentifiers);
        p.addValue("expeditionCode", recordSet.expeditionCode());

        RowMapper<GenericRecord> rowMapper = new GenericRecordRowMapper();
        return recordRepository.query(
                StrSubstitutor.replace(sql, tableMap),
                p,
                (rs, rowNum) -> new PhotoRecord(rowMapper.mapRow(rs, rowNum).properties()));
    }

    @Override
    public DataConverter newInstance(ProjectConfig projectConfig) {
        return new PhotoConverter(photosSql, recordRepository, projectConfig);
    }
}

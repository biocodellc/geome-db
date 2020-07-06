package biocode.fims.photos;

import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

import static biocode.fims.photos.PhotoEntityProps.*;

/**
 * @author rjewing
 */
public class PhotoRecord extends GenericRecord {

    public PhotoRecord() {
        super();
    }

    public PhotoRecord(Map<String, Object> properties) {
        super(properties);
    }

    private PhotoRecord(Map<String, Object> properties, String rootIdentifier, int projectId, String expeditionCode, boolean shouldPersist) {
        super(properties, rootIdentifier, projectId, expeditionCode, shouldPersist);
    }

    public String originalUrl() {
        return get(ORIGINAL_URL.uri());
    }

    public String photoID() {
        return get(PHOTO_ID.uri());
    }

    public boolean bulkLoad() {
        return StringUtils.isNotBlank(bulkLoadFile());
    }

    public String bulkLoadFile() {
        return get(BULK_LOAD_FILE.uri());
    }

    public boolean hasError() {
        return !get(PROCESSING_ERROR.uri()).equals("");
    }

    @Override
    public Record clone() {
        return new PhotoRecord(properties, rootIdentifier(), projectId(), expeditionCode(), persist);
    }
}


package biocode.fims.config.models;

import biocode.fims.config.Config;
import biocode.fims.photos.PhotoEntityProps;
import biocode.fims.photos.PhotoRecord;
import biocode.fims.validation.rules.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.*;

import static biocode.fims.photos.PhotoEntityProps.*;


/**
 * @author rjewing
 */
@JsonDeserialize(converter = PhotoEntity.PhotoEntitySanitizer.class)
public class PhotoEntity extends PropEntity<PhotoEntityProps> {
    public static final String TYPE = "Photo";

    private static final String CONCEPT_URI = "http://rs.tdwg.org/dwc/terms/associatedMedia";
    private static final String GENERATE_ID_KEY = "generateID";

    private boolean generateID = false;

    private PhotoEntity() { // needed for EntityTypeIdResolver
        super(PhotoEntityProps.class);
    }

    public PhotoEntity(String conceptAlias) {
        super(PhotoEntityProps.class, conceptAlias, CONCEPT_URI);
    }

    @Override
    protected void init() {
        super.init();

        // TODO look into possibility of using photo hashing, but worried about false positives
        setUniqueKey(PHOTO_ID.column());
        getAttribute(PROCESSED.column()).setInternal(true);
        getAttribute(PROCESSING_ERROR.column()).setInternal(true);
        getAttribute(BULK_LOAD_FILE.column()).setInternal(true);
        getAttribute(FILENAME.column()).setInternal(true);
        getAttribute(IMG_128.column()).setInternal(true);
        getAttribute(IMG_512.column()).setInternal(true);
        getAttribute(IMG_1024.column()).setInternal(true);
        recordType = PhotoRecord.class;
    }

    public boolean isGenerateID() {
        return generateID;
    }

    public void setGenerateID(boolean generateID) {
        this.generateID = generateID;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Map<String, Object> additionalProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(GENERATE_ID_KEY, generateID);
        return props;
    }

    @Override
    public void setAdditionalProps(Map<String, Object> props) {
        if (props == null) return;
        generateID = (boolean) props.getOrDefault(GENERATE_ID_KEY, false);
    }

    @Override
    public void addDefaultRules(Config config) {
        super.addDefaultRules(config);
        RequiredValueRule requiredValueRule = getRule(RequiredValueRule.class, RuleLevel.ERROR);

        if (requiredValueRule == null) {
            requiredValueRule = new RequiredValueRule(new LinkedHashSet<>(), RuleLevel.ERROR);
            addRule(requiredValueRule);
        }

        requiredValueRule.addColumn(PHOTO_ID.column());

        addRule(new PhotoFileRule());
    }

    @Override
    public boolean canReload() {
        return true;
    }

    @Override
    public Entity clone() {
        PhotoEntity entity = new PhotoEntity(getConceptAlias());
        entity.generateID = generateID;
        return clone(entity);
    }

    /**
     * class used to verify PhotoEntity data integrity after deserialization. This is necessary
     * so we don't overwrite the default values during deserialization.
     */
    static class PhotoEntitySanitizer extends PropEntitySanitizer<PhotoEntity> {
    }
}


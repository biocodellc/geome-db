package biocode.fims.photos.processing;

import biocode.fims.config.models.Entity;
import biocode.fims.photos.PhotoRecord;

import java.util.Map;

/**
 * @author rjewing
 */
public class UnprocessedPhotoRecord extends PhotoRecord {

    private final int networkId;
    private final Entity entity;
    private final Entity parentEntity;

    public UnprocessedPhotoRecord(Map<String, Object> properties, Entity parentEntity, Entity entity, int networkId, int projectId, String expeditionCode) {
        super(properties);
        this.parentEntity = parentEntity;
        this.entity = entity;
        this.networkId = networkId;
        setProjectId(projectId);
        setExpeditionCode(expeditionCode);
    }

    public int networkId() {
        return networkId;
    }

    public Entity entity() {
        return entity;
    }

    public Entity parentEntity() {
        return parentEntity;
    }
}

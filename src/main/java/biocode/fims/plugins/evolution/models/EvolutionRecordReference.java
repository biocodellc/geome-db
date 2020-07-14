package biocode.fims.plugins.evolution.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author rjewing
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EvolutionRecordReference {
    public String recordGuid;
    public String eventId;
    public String userId;

    public EvolutionRecordReference(String recordGuid, String eventId, String userId) {
        this.recordGuid = recordGuid;
        this.eventId = eventId;
        this.userId = userId;
    }
}

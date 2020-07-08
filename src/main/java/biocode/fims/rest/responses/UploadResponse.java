package biocode.fims.rest.responses;

import biocode.fims.validation.messages.EntityMessages;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author rjewing
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UploadResponse {
    @JsonProperty
    private boolean success;
    @JsonProperty
    private EntityMessages messages;

    public UploadResponse(boolean success, EntityMessages messages) {
        this.success = success;
        this.messages = messages;
    }

    public boolean isSuccess() {
        return success;
    }

    public EntityMessages getMessages() {
        return messages;
    }
}

package biocode.fims.rest.responses;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

/**
 * @author rjewing
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SraUploadCreateResponse {
    public final UUID uploadId;

    public SraUploadCreateResponse(UUID uploadId) {
        this.uploadId = uploadId;
    }
}

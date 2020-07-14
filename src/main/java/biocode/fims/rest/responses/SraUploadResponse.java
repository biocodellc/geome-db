package biocode.fims.rest.responses;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author rjewing
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SraUploadResponse {
    public final boolean success;
    public final String message;

    public SraUploadResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}

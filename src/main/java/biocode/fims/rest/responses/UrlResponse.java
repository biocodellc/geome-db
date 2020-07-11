package biocode.fims.rest.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author RJ Ewing
 */
public class UrlResponse {
    private final String url;

    public UrlResponse(String url) {
        // localhost does not need to serve http:
        if (url.contains("localhost")) {
            this.url = url;
        } else {
            // force returning https: instead of http:
            this.url = url.replaceAll("http:","https:");
        }
    }

    @JsonProperty()
    public String url() {
        return url;
    }
}

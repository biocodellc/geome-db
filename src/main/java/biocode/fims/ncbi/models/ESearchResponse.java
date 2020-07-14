package biocode.fims.ncbi.models;

import biocode.fims.api.services.PaginatedResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author rjewing
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ESearchResponse implements PaginatedResponse {

    private ESearchResult eSearchResult;

    private ESearchResponse() {}

    public ESearchResponse(ESearchResult eSearchResult) {
        this.eSearchResult = eSearchResult;
    }

    @JsonProperty("esearchresult")
    public ESearchResult geteSearchResult() {
        return eSearchResult;
    }

    @JsonProperty("esearchresult")
    public void seteSearchResult(ESearchResult eSearchResult) {
        this.eSearchResult = eSearchResult;
    }

    public boolean hasMoreResults() {
        return eSearchResult.getCount() > lastResultNumber();
    }

    public int lastResultNumber() {
        return (eSearchResult.getRetrievalStart() + 1) * eSearchResult.getRetrievalMax();
    }
}

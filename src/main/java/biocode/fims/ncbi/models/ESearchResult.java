package biocode.fims.ncbi.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author rjewing
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ESearchResult {

    private int count;
    private int retrievalMax;
    private int retrievalStart;
    private List<String> idList;

    private ESearchResult(){}

    public ESearchResult(int count, int retrievalMax, int retrievalStart, List<String> idList) {
        this.count = count;
        this.retrievalMax = retrievalMax;
        this.retrievalStart = retrievalStart;
        this.idList = idList;
    }

    public int getCount() {
        return count;
    }

    @JsonProperty("retmax")
    public int getRetrievalMax() {
        return retrievalMax;
    }

    @JsonProperty("retstart")
    public int getRetrievalStart() {
        return retrievalStart;
    }

    @JsonProperty("idlist")
    public List<String> getIdList() {
        return idList;
    }
}

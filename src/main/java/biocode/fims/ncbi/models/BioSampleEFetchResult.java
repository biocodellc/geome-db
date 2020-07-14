package biocode.fims.ncbi.models;

import biocode.fims.api.services.PaginatedResponse;
import biocode.fims.ncbi.entrez.requests.AbstractEFetchRequest;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @author rjewing
 */
@XmlRootElement(name="BioSampleSet")
@XmlAccessorType(XmlAccessType.FIELD)
public class BioSampleEFetchResult implements PaginatedResponse {

    @XmlElement(name="BioSample")
    private List<BioSample> bioSamples;

    private BioSampleEFetchResult() {}
    public BioSampleEFetchResult(List<BioSample> bioSamples) {
        this.bioSamples = bioSamples;
    }

    public List<BioSample> getBioSamples() {
        return bioSamples;
    }

    @Override
    public boolean hasMoreResults() {
        // NCBI Entrez efetch doesn't return any information regarding pagination
        // however they do return paginated results. Therefore we need to check if there are more results
        // if the bioSamples.size() == efetch request maximum return size
        return bioSamples.size() == AbstractEFetchRequest.RET_MAX;
    }
}

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
@XmlRootElement(name="EXPERIMENT_PACKAGE_SET")
@XmlAccessorType(XmlAccessType.FIELD)
public class SraEFetchResult implements PaginatedResponse {

    @XmlElement(name="EXPERIMENT_PACKAGE")
    private List<SraExperimentPackage> experimentPackages;

    private SraEFetchResult() {}

    public SraEFetchResult(List<SraExperimentPackage> experimentPackages) {
        this.experimentPackages = experimentPackages;
    }

    public List<SraExperimentPackage> getExperimentPackages() {
        return experimentPackages;
    }

    @Override
    public boolean hasMoreResults() {
        // NCBI Entrez efetch doesn't return any information regarding pagination
        // however they do return paginated results. Therefore we need to check if there are more results
        // if the experimentPackages.size() == efetch request maximum return size
        return experimentPackages.size() == AbstractEFetchRequest.RET_MAX;
    }
}

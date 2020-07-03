package biocode.fims.ncbi.entrez;

import biocode.fims.ncbi.models.SraEFetchResult;
import biocode.fims.ncbi.models.SraExperimentPackage;

import java.util.List;

/**
 * @author rjewing
 */
public class FakeSraEFetchResult extends SraEFetchResult {

    FakeSraEFetchResult(List<SraExperimentPackage> experimentPackages) {
        super(experimentPackages);
    }

    @Override
    public boolean hasMoreResults() {
        return getExperimentPackages().size() == FakeBioSampleEFetchRequest.RET_MAX;
    }
}

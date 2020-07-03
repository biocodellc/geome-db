package biocode.fims.ncbi.entrez;

import biocode.fims.ncbi.models.BioSample;
import biocode.fims.ncbi.models.BioSampleEFetchResult;

import java.util.List;

/**
 * @author rjewing
 */
public class FakeBioSampleEFetchResult extends BioSampleEFetchResult {
    public FakeBioSampleEFetchResult(List<BioSample> bioSamples) {
        super(bioSamples);
    }

    @Override
    public boolean hasMoreResults() {
        return getBioSamples().size() == FakeBioSampleEFetchRequest.RET_MAX;
    }
}

package biocode.fims.ncbi.entrez;

import biocode.fims.ncbi.entrez.requests.BioSampleEFetchRequest;
import biocode.fims.ncbi.models.BioSample;
import biocode.fims.ncbi.models.BioSampleEFetchResult;

import java.util.ArrayList;
import java.util.List;

/**
 * @author rjewing
 */
public class FakeBioSampleEFetchRequest implements BioSampleEFetchRequest {
    final static int RET_MAX = 10;

    private final int totalBioSamples;
    private int currentPage = 0;
    private BioSampleEFetchResult expectedResponse;


    FakeBioSampleEFetchRequest(BioSampleEFetchResult expectedResponse, int totalBioSamples) {
        this.expectedResponse = expectedResponse;
        this.totalBioSamples = totalBioSamples;
    }

    @Override
    public BioSampleEFetchResult execute() {
        return expectedResponse;
    }

    @Override
    public BioSampleEFetchResult getMoreResults() {
        currentPage++;

        expectedResponse = new FakeBioSampleEFetchResult(getNextBioSamples());

        return execute();
    }

    private List<BioSample> getNextBioSamples() {
        int totalReturned = getTotalReturned();
        int numberBioSamplesToReturn = totalBioSamples - totalReturned;
        int startingId = currentPage * RET_MAX;

        return generateBioSamples(numberBioSamplesToReturn, startingId);
    }

    static List<BioSample> generateBioSamples(int numberBioSamplesToReturn, int startingId) {
        List<BioSample> bioSamples = new ArrayList<>();


        for (int i = 0; i < numberBioSamplesToReturn; i++) {
            bioSamples.add(
                    new BioSample(String.valueOf(startingId + i), null, null, null, null )
            );
        }

        return bioSamples;
    }

    private int getTotalReturned() {
        if (isFirstPage()) {
            return expectedResponse.getBioSamples().size();
        } else {
            return currentPage * RET_MAX;
        }
    }

    private boolean isFirstPage() {
        return currentPage == 1;
    }
}

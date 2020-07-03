package biocode.fims.ncbi.entrez;

import biocode.fims.ncbi.entrez.requests.SraEFetchRequest;
import biocode.fims.ncbi.models.SraEFetchResult;
import biocode.fims.ncbi.models.SraExperimentPackage;

import java.util.ArrayList;
import java.util.List;

/**
 * @author rjewing
 */
public class FakeSraExperimentPackageRequest implements SraEFetchRequest {
    private final static int RET_MAX = 10;

    private SraEFetchResult expectedResponse;
    private int currentPage = 0;
    private final int totalExperimentPackages;

    FakeSraExperimentPackageRequest(SraEFetchResult expectedResponse, int totalExperimentPackages) {
        this.expectedResponse = expectedResponse;
        this.totalExperimentPackages = totalExperimentPackages;
    }

    @Override
    public SraEFetchResult execute() {
        return expectedResponse;
    }

    @Override
    public SraEFetchResult getMoreResults() {
        currentPage++;

        expectedResponse = new FakeSraEFetchResult(getNextExperimentPackages());

        return execute();
    }

    private List<SraExperimentPackage> getNextExperimentPackages() {
        int numberExperimentPackagesToReturn = totalExperimentPackages - getTotalReturned();
        int startingId = currentPage * RET_MAX;

        return generateExperimentPackages(numberExperimentPackagesToReturn, startingId);
    }

    private int getTotalReturned() {
        if (isFirstPage()) {
            return expectedResponse.getExperimentPackages().size();
        } else {
            return currentPage * RET_MAX;
        }
    }

    private boolean isFirstPage() {
        return currentPage == 1;
    }

    static List<SraExperimentPackage> generateExperimentPackages(int numberExperimentPackagesToReturn, int startingId) {
        List<SraExperimentPackage> experimentPackages = new ArrayList<>();


        for (int i = 0; i < numberExperimentPackagesToReturn; i++) {
            experimentPackages.add(
                    new SraExperimentPackage(null, String.valueOf(startingId + i), null, null)
            );
        }

        return experimentPackages;
    }
}

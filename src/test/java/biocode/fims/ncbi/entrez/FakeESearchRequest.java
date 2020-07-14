package biocode.fims.ncbi.entrez;

import biocode.fims.ncbi.entrez.requests.ESearchRequest;
import biocode.fims.ncbi.models.ESearchResponse;
import biocode.fims.ncbi.models.ESearchResult;

import java.util.ArrayList;
import java.util.List;

/**
 * @author rjewing
 */
class FakeESearchRequest implements ESearchRequest {

    private ESearchResponse expectedResponse;

    FakeESearchRequest(ESearchResponse expectedResponse) {
        this.expectedResponse = expectedResponse;
    }

    @Override
    public ESearchResponse execute() {
        return expectedResponse;
    }

    public ESearchResponse getMoreResults() {
        expectedResponse = new ESearchResponse(getNextESearchResult());
        return execute();
    }

    private ESearchResult getNextESearchResult() {
        ESearchResult currentResult = expectedResponse.geteSearchResult();

        int lastResultNumber = expectedResponse.lastResultNumber();

        int numberResults = (lastResultNumber + currentResult.getRetrievalMax()) > currentResult.getCount() ? currentResult.getCount() - lastResultNumber : currentResult.getRetrievalMax();
        return new ESearchResult(
                currentResult.getCount(),
                currentResult.getRetrievalMax(),
                currentResult.getRetrievalStart() + 1,
                generateStringIds(numberResults)
        );
    }

    static List<String> generateStringIds(int number) {
        List<String> ids = new ArrayList<>();

        for (int i = 0; i < number; i++) {
            ids.add(String.valueOf(i));
        }

        return ids;
    }
}

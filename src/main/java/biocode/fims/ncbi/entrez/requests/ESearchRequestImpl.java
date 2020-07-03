package biocode.fims.ncbi.entrez.requests;

import biocode.fims.ncbi.entrez.EntrezQueryParams;
import biocode.fims.ncbi.models.ESearchResponse;
import org.springframework.util.Assert;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;

/**
 * Make http requests to the NCBI Entrez eSearch endpoint
 *
 * @author rjewing
 */

public class ESearchRequestImpl extends AbstractEntrezRequest<ESearchResponse> implements ESearchRequest {
    private final static String SERVICE_PATH = "esearch.fcgi";
    private final static String RET_MODE = "json";
    public final static int RET_MAX = 100000;

    public ESearchRequestImpl(String db, String term, String apiKey, Client client) {
        super(SERVICE_PATH, apiKey, client, "GET", biocode.fims.ncbi.models.ESearchResponse.class);
        addDefaultQueryParams(db, term);
        setAccepts(MediaType.APPLICATION_JSON);
    }

    private void addDefaultQueryParams(String db, String term) {
        Assert.hasText(db, "Required parameter db must not be empty");
        Assert.hasText(term, "Required parameter term must not be empty");

        addQueryParam(EntrezQueryParams.DB.getName(), db);
        addQueryParam(EntrezQueryParams.TERM.getName(), term);
        addQueryParam(EntrezQueryParams.RETRIEVAL_MODE.getName(), RET_MODE);
        addQueryParam(EntrezQueryParams.RETRIEVAL_START.getName(), currentPage);
        addQueryParam(EntrezQueryParams.RETRIEVAL_MAX.getName(), RET_MAX);
    }
}
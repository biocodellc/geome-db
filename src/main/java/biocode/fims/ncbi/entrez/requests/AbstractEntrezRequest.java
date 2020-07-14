package biocode.fims.ncbi.entrez.requests;

import biocode.fims.api.services.AbstractRequest;
import biocode.fims.ncbi.entrez.EntrezQueryParams;

import javax.ws.rs.client.Client;


/**
 *
 * @author rjewing
 */
public abstract class AbstractEntrezRequest<T> extends AbstractRequest<T> implements EntrezRequest<T> {
    private final static String BASE_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/";

    protected int currentPage;

    protected AbstractEntrezRequest(String path, String apiKey, Client client, String method, Class<T> responseClass) {
        super(method, responseClass, client, path, BASE_URL);
        addQueryParam(EntrezQueryParams.API_KEY.getName(), apiKey);

        currentPage = 0;
    }

    @Override
    public T getMoreResults() {
        currentPage++;

        addQueryParam(EntrezQueryParams.RETRIEVAL_START.getName(), currentPage);

        return execute();
    }
}

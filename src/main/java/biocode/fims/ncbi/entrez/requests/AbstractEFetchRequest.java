package biocode.fims.ncbi.entrez.requests;

import biocode.fims.ncbi.entrez.EntrezQueryParams;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.springframework.util.Assert;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Make a http requests to the NCBI Entrez eFetch endpoint
 *
 * @author rjewing
 */

public class AbstractEFetchRequest<T> extends AbstractEntrezRequest<T> implements EFetchRequest<T> {
    private final static String SERVICE_PATH = "efetch.fcgi";
    private final static String RET_MODE = "xml";
    public final static int RET_MAX = 10000;

    /**
     *
     * @param db
     * @param ids size must be between 1 and {@link AbstractEFetchRequest#RET_MAX}
     * @param client
     * @param responseClass
     */
    AbstractEFetchRequest(String db, List<String> ids, String apiKey, Client client, Class<T> responseClass) {
        super(SERVICE_PATH, apiKey, client, "POST", responseClass);
        registerDefaultClientFeatures(client);

        setDefaultQueryParams(db);
        setHttpEntity(getDefaultHttpEntity(ids));
        setAccepts(MediaType.APPLICATION_ATOM_XML);
    }

    private Entity getDefaultHttpEntity(List<String> ids) {
        Assert.notEmpty(ids, "Required parameter ids must not be empty");
        assert ids.size() <= RET_MAX;

        MultivaluedMap<String, String> formParams = new MultivaluedHashMap<>();
        formParams.addAll("id", ids);

        Form form = new Form(formParams);

        return Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE);
    }

    private void setDefaultQueryParams(String db) {
        Assert.hasText(db, "Required parameter db must not be empty");

        this.addQueryParam(EntrezQueryParams.DB.getName(), db);
        this.addQueryParam(EntrezQueryParams.RETRIEVAL_MODE.getName(), RET_MODE);
        this.addQueryParam(EntrezQueryParams.RETRIEVAL_START.getName(), currentPage);
        this.addQueryParam(EntrezQueryParams.RETRIEVAL_MAX.getName(), RET_MAX);
    }

    private void registerDefaultClientFeatures(Client client) {
        Configuration config = client.getConfiguration();

        if (!config.isRegistered(MoxyXmlFeature.class)) {
            client.register(MoxyXmlFeature.class);
        }
    }
}
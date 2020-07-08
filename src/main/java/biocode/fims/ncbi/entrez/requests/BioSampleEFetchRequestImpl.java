package biocode.fims.ncbi.entrez.requests;

import biocode.fims.ncbi.NCBIDatabase;
import biocode.fims.ncbi.models.BioSampleEFetchResult;

import javax.ws.rs.client.Client;
import java.util.List;

/**
 * Class for fetching BioSample entries from the NCBI Entrez efetch endpoint
 *
 * @author rjewing
 */
public class BioSampleEFetchRequestImpl extends AbstractEFetchRequest<BioSampleEFetchResult> implements BioSampleEFetchRequest {

    public BioSampleEFetchRequestImpl(List<String> ids, String apiKey, Client client) {
        super(NCBIDatabase.BIO_SAMPLE.getName(), ids, apiKey, client, BioSampleEFetchResult.class);
    }
}

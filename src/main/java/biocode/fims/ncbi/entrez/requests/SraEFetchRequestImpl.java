package biocode.fims.ncbi.entrez.requests;

import biocode.fims.ncbi.NCBIDatabase;
import biocode.fims.ncbi.models.SraEFetchResult;

import javax.ws.rs.client.Client;
import java.util.List;

/**
 * Class for fetching SRA entries from the NCBI Entrez efetch endpoint
 *
 * @author rjewing
 */
public class SraEFetchRequestImpl extends AbstractEFetchRequest<SraEFetchResult> implements SraEFetchRequest {

    public SraEFetchRequestImpl(List<String> ids, String apiKey, Client client) {
        super(NCBIDatabase.SRA.getName(), ids, apiKey, client, SraEFetchResult.class);
    }
}

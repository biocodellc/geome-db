package biocode.fims.ncbi.entrez;

import biocode.fims.ncbi.NCBIDatabase;
import biocode.fims.ncbi.entrez.requests.*;

import javax.ws.rs.client.Client;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author rjewing
 */
public class EntrezApiFactoryImpl implements EntrezApiFactory {
    private final String apiKey;
    private final Client client;

    public EntrezApiFactoryImpl(String apiKey, Client client) {
        this.apiKey = apiKey;
        this.client = client;
    }

    /**
     * If startDate is not null, we only fetch bioSample's that were published after that date
     *
     * @param startDate
     * @return
     */
    @Override
    public ESearchRequest getBioSampleESearchRequest(LocalDate startDate) {
        StringBuilder term = new StringBuilder("bcid[Attribute Name]");

        // ex date filter from 04/2018 - present (date can be any portion (2018, 2018/04)):
        //      &term=bcid[Attribute Name] AND ("2018/04"[Publication Date] : "3000"[Publication Date])
        if (startDate != null) {
            term.append(" AND (\"");
            term.append(startDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")));
            term.append("\"[Publication Date] : \"3000\"[Publication Date])");
        }

        return new ESearchRequestImpl(NCBIDatabase.BIO_SAMPLE.getName(), term.toString(), apiKey, client);
    }

    @Override
    public BioSampleEFetchRequest getBioSamplesFromIds(List<String> bioSampleIds) {
        return new BioSampleEFetchRequestImpl(bioSampleIds, apiKey, client);
    }

    @Override
    public ESearchRequest getSraESearchRequest(Set<String> bioProjectIds) {
        StringBuilder term = new StringBuilder();

        Iterator<String> it = bioProjectIds.iterator();

        while (it.hasNext()) {
            term.append(it.next());
            term.append("[BioProject]");

            if (it.hasNext()) {
                term.append(" OR ");
            }
        }

        return new ESearchRequestImpl(NCBIDatabase.SRA.getName(), term.toString(), apiKey, client);
    }

    @Override
    public SraEFetchRequest getSraExperimentsFromIds(List<String> experimentPackageIds) {
        return new SraEFetchRequestImpl(experimentPackageIds, apiKey, client);
    }
}

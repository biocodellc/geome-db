package biocode.fims.ncbi.entrez;

import biocode.fims.ncbi.entrez.requests.BioSampleEFetchRequest;
import biocode.fims.ncbi.entrez.requests.ESearchRequest;
import biocode.fims.ncbi.entrez.requests.SraEFetchRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * @author rjewing
 */
public interface EntrezApiFactory {
    ESearchRequest getBioSampleESearchRequest(LocalDate startDate);

    BioSampleEFetchRequest getBioSamplesFromIds(List<String> bioSampleIds);

    ESearchRequest getSraESearchRequest(Set<String> bioProjectIds);

    SraEFetchRequest getSraExperimentsFromIds(List<String> experimentPackageIds);
}

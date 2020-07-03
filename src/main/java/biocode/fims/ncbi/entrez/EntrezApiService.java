package biocode.fims.ncbi.entrez;

import biocode.fims.ncbi.entrez.requests.BioSampleEFetchRequest;
import biocode.fims.ncbi.entrez.requests.ESearchRequest;
import biocode.fims.ncbi.entrez.requests.SraEFetchRequest;
import biocode.fims.ncbi.models.*;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * @author rjewing
 */
public class EntrezApiService {
    private final int fetchWeeksInPast;
    private EntrezApiFactory apiFactory;

    public EntrezApiService(EntrezApiFactory apiFactory, int fetchWeeksInPast) {
        this.fetchWeeksInPast = fetchWeeksInPast;
        Assert.notNull(apiFactory);
        this.apiFactory = apiFactory;
    }

    /**
     * <p>
     * get a list of {@link biocode.fims.ncbi.models.BioSample#id}s that have a "bcid" attribute from the NCBI BioSample
     * database
     */
    public List<String> getBioSampleIdsWithBcidAttribute() {
        LocalDate fetchStartDate = LocalDate.now().minusWeeks(fetchWeeksInPast);
        ESearchRequest request = apiFactory.getBioSampleESearchRequest(fetchStartDate);
        return getESearchIds(request);
    }

    /**
     * get the {@link biocode.fims.ncbi.models.BioSample}s from the NCBI BioSample database
     */
    public List<BioSample> getBioSamplesFromIds(List<String> bioSampleIds) {
        BioSampleEFetchRequest request = apiFactory.getBioSamplesFromIds(bioSampleIds);
        BioSampleEFetchResult response = request.execute();

        List<BioSample> bioSamples = response.getBioSamples();

        while (response.hasMoreResults()) {
            response = request.getMoreResults();
            bioSamples.addAll(response.getBioSamples());
        }

        return bioSamples;
    }

    /**
     * get the {@link biocode.fims.ncbi.models.SraExperimentPackage}s from the NCBI SRA database
     */
    public List<SraExperimentPackage> getSraExperimentPackagesFromIds(List<String> experimentPackageIds) {
        SraEFetchRequest request = apiFactory.getSraExperimentsFromIds(experimentPackageIds);
        SraEFetchResult response = request.execute();

        List<SraExperimentPackage> experimentPackages = response.getExperimentPackages();

        while (response.hasMoreResults()) {
            response = request.getMoreResults();
            experimentPackages.addAll(response.getExperimentPackages());
        }

        return experimentPackages;
    }

    /**
     * get a list of all SRA Experiment id's for a {@link biocode.fims.ncbi.models.BioSample#bioProjectId}
     * from the NCBI SRA database
     */
    public List<String> getSraExperimentPackageIds(Set<String> bioProjectIds) {
        Assert.notEmpty(bioProjectIds, "Parameter bioProjectIds must not be empty");
        ESearchRequest request = apiFactory.getSraESearchRequest(bioProjectIds);
        return getESearchIds(request);
    }

    private List<String> getESearchIds(ESearchRequest request) {
        ESearchResponse response = request.execute();
        List<String> ids = response.geteSearchResult()
                .getIdList();

        while (response.hasMoreResults()) {
            response = request.getMoreResults();
            ids.addAll(response.geteSearchResult().getIdList());
        }

        return ids;
    }
}

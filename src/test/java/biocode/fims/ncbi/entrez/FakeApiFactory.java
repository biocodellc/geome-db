package biocode.fims.ncbi.entrez;

import biocode.fims.ncbi.entrez.requests.BioSampleEFetchRequest;
import biocode.fims.ncbi.entrez.requests.ESearchRequest;
import biocode.fims.ncbi.entrez.requests.SraEFetchRequest;
import biocode.fims.ncbi.models.BioSampleEFetchResult;
import biocode.fims.ncbi.models.ESearchResponse;
import biocode.fims.ncbi.models.SraEFetchResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * @author rjewing
 */
class FakeApiFactory implements EntrezApiFactory {
    private final BioSampleEFetchResult bioSampleEFetchResponse;
    private final ESearchResponse bioSampleESearchResponse;
    private final int totalBioSamples;
    private final ESearchResponse sraESearchResponse;
    private final int totalExperimentPackages;
    private final SraEFetchResult sraEFetchResponse;

    private FakeApiFactory(Builder builder) {
        this.bioSampleESearchResponse = builder.bioSampleESearchResponse;
        this.bioSampleEFetchResponse = builder.bioSampleEFetchResponse;
        this.totalBioSamples = builder.totalBioSamples;
        this.sraESearchResponse = builder.sraESearchResponse;
        this.sraEFetchResponse = builder.sraEFetchResponse;
        this.totalExperimentPackages = builder.totalExperimentPackages;
    }

    static class Builder {
        ESearchResponse bioSampleESearchResponse = null;
        private BioSampleEFetchResult bioSampleEFetchResponse = null;
        private int totalBioSamples = 0;
        private ESearchResponse sraESearchResponse = null;
        private SraEFetchResult sraEFetchResponse = null;
        private int totalExperimentPackages = 0;

        Builder() {
        }

        Builder setBioSampleIdsExpectedResponse(ESearchResponse expectedResponse) {
            this.bioSampleESearchResponse = expectedResponse;
            return this;
        }

        Builder setBioSamplesExpectedResponse(BioSampleEFetchResult bioSampleEFetchResult) {
            this.bioSampleEFetchResponse = bioSampleEFetchResult;
            return this;
        }

        Builder setTotalBioSamples(int totalBioSamples) {
            this.totalBioSamples = totalBioSamples;
            return this;
        }

        FakeApiFactory build() {
            return new FakeApiFactory(this);
        }

        public Builder setSraExperimentPackageIdsExpectedResponse(ESearchResponse expectedResponse) {
            this.sraESearchResponse = expectedResponse;
            return this;
        }

        Builder setSraExperimentPackageExpectedResponse(SraEFetchResult sraEFetchResponse) {
            this.sraEFetchResponse = sraEFetchResponse;
            return this;
        }

        public Builder setTotalExperimentPackages(int i) {
            this.totalExperimentPackages = i;
            return this;
        }
    }

    @Override
    public ESearchRequest getBioSampleESearchRequest(LocalDate startDate) {
        return new FakeESearchRequest(bioSampleESearchResponse);
    }

    @Override
    public BioSampleEFetchRequest getBioSamplesFromIds(List<String> bioSampleIds) {
        return new FakeBioSampleEFetchRequest(bioSampleEFetchResponse, totalBioSamples);
    }

    @Override
    public ESearchRequest getSraESearchRequest(Set<String> bioProjectIds) {
        return new FakeESearchRequest(sraESearchResponse);
    }

    @Override
    public SraEFetchRequest getSraExperimentsFromIds(List<String> experimentPackageIds) {
        return new FakeSraExperimentPackageRequest(sraEFetchResponse, totalExperimentPackages);
    }
}

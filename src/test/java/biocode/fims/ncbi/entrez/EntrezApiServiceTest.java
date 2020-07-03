package biocode.fims.ncbi.entrez;

import biocode.fims.ncbi.models.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class EntrezApiServiceTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void contructor_fails_fast_when_given_null_EntrezApiFactory_argument() {
        exception.expect(IllegalArgumentException.class);
        new EntrezApiService(null, 2);
    }

    @Test
    public void getBioSampleIdsWithBcidAttribute_returns_all_ids_when_response_contains_all_results() {
        FakeApiFactory fakeApiFactory = new FakeApiFactory.Builder()
                .setBioSampleIdsExpectedResponse(new ESearchResponse(
                        new ESearchResult(5, 10, 0, FakeESearchRequest.generateStringIds(5))
                ))
                .build();

        EntrezApiService apiService = new EntrezApiService(fakeApiFactory, 2);

        List<String> bioSamples = apiService.getBioSampleIdsWithBcidAttribute();
        assertEquals(5, bioSamples.size());
    }

    @Test
    public void getBioSampleIdsWithBcidAttribute_returns_all_ids_when_response_contains_paginated_results() {
        FakeApiFactory fakeApiFactory = new FakeApiFactory.Builder()
                .setBioSampleIdsExpectedResponse(new ESearchResponse(
                        new ESearchResult(15, 10, 0, FakeESearchRequest.generateStringIds(10))
                ))
                .build();

        EntrezApiService apiService = new EntrezApiService(fakeApiFactory, 2);

        List<String> bioSamples = apiService.getBioSampleIdsWithBcidAttribute();
        assertEquals(15, bioSamples.size());
    }

    @Test
    public void getBioSamplesFromIds_returns_all_bioSamples_when_response_contains_all_results() {

        FakeApiFactory fakeApiFactory = new FakeApiFactory.Builder()
                .setBioSamplesExpectedResponse(new BioSampleEFetchResult(
                        FakeBioSampleEFetchRequest.generateBioSamples(5, 0)
                ))
                .setTotalBioSamples(5)
                .build();

        EntrezApiService apiService = new EntrezApiService(fakeApiFactory, 2);

        List<String> irreleventIdList = Collections.emptyList();
        List<BioSample> bioSamples = apiService.getBioSamplesFromIds(irreleventIdList);
        assertEquals(5, bioSamples.size());
    }

    @Test
    public void getBioSamplesFromIds_returns_all_bioSamples_when_response_contains_paginated_results() {

        FakeApiFactory fakeApiFactory = new FakeApiFactory.Builder()
                .setBioSamplesExpectedResponse(new FakeBioSampleEFetchResult(
                        FakeBioSampleEFetchRequest.generateBioSamples(10, 0)
                ))
                .setTotalBioSamples(25)
                .build();

        EntrezApiService apiService = new EntrezApiService(fakeApiFactory, 2);

        List<String> irreleventIdList = Collections.emptyList();
        List<BioSample> bioSamples = apiService.getBioSamplesFromIds(irreleventIdList);
        assertEquals(25, bioSamples.size());
    }

    @Test
    public void getSraExperimentPackagesByBioProject_returns_all_ids_when_response_contains_all_results() {
        FakeApiFactory fakeApiFactory = new FakeApiFactory.Builder()
                .setSraExperimentPackageIdsExpectedResponse(new ESearchResponse(
                        new ESearchResult(5, 10, 0, FakeESearchRequest.generateStringIds(5))
                ))
                .build();

        EntrezApiService apiService = new EntrezApiService(fakeApiFactory, 2);

        List<String> bioSamples = apiService.getSraExperimentPackageIds(new HashSet<>(Collections.singletonList("bioProject1")));
        assertEquals(5, bioSamples.size());
    }

    @Test
    public void getSraExperimentPackagesByBioProject_returns_all_ids_when_response_contains_paginated_results() {
        FakeApiFactory fakeApiFactory = new FakeApiFactory.Builder()
                .setSraExperimentPackageIdsExpectedResponse(new ESearchResponse(
                        new ESearchResult(25, 10, 0, FakeESearchRequest.generateStringIds(10))
                ))
                .build();

        EntrezApiService apiService = new EntrezApiService(fakeApiFactory, 2);

        List<String> bioSamples = apiService.getSraExperimentPackageIds(new HashSet<>(Collections.singletonList("bioProject1")));
        assertEquals(25, bioSamples.size());
    }

    @Test
    public void getSraExperimentPackagesFromIds_returns_all_packages_when_response_contains_all_results() {

        FakeApiFactory fakeApiFactory = new FakeApiFactory.Builder()
                .setSraExperimentPackageExpectedResponse(new SraEFetchResult(
                        FakeSraExperimentPackageRequest.generateExperimentPackages(5, 0)
                ))
                .setTotalExperimentPackages(5)
                .build();

        EntrezApiService apiService = new EntrezApiService(fakeApiFactory, 2);

        List<String> irreleventIdList = Collections.emptyList();
        List<SraExperimentPackage> experimentPackages = apiService.getSraExperimentPackagesFromIds(irreleventIdList);
        assertEquals(5, experimentPackages.size());
    }

    @Test
    public void getSraExperimentPackagesFromIds_returns_all_packages_when_response_contains_paginated_results() {

        FakeApiFactory fakeApiFactory = new FakeApiFactory.Builder()
                .setSraExperimentPackageExpectedResponse(new FakeSraEFetchResult(
                        FakeSraExperimentPackageRequest.generateExperimentPackages(10, 0)
                ))
                .setTotalExperimentPackages(25)
                .build();

        EntrezApiService apiService = new EntrezApiService(fakeApiFactory, 2);

        List<String> irreleventIdList = Collections.emptyList();
        List<SraExperimentPackage> experimentPackages = apiService.getSraExperimentPackagesFromIds(irreleventIdList);
        assertEquals(25, experimentPackages.size());
    }

}
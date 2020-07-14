package biocode.fims.ncbi.entrez;

import biocode.fims.ncbi.models.BioSample;
import biocode.fims.ncbi.models.SraExperimentPackage;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author rjewing
 */
public class BioSampleRepositoryTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void constructor_fails_fast_when_given_null_service_argument() {
        exception.expect(IllegalArgumentException.class);
        new BioSampleRepository(null);
    }

    @Test
    public void getBioSamples_with_empty_bcids_argument_returns_empty_result() {
        BioSampleRepository repository = new RepositoryBuilder()
                .build();

        List<BioSample> bioSamples = repository.getBioSamples(Collections.emptyList());
        assertTrue(bioSamples.isEmpty());
    }

    @Test
    public void getBioSamples_returns_empty_list_when_empty_repository() {
        BioSampleRepository repository = new RepositoryBuilder()
                .build();

        List<BioSample> bioSamples = repository.getBioSamples(Arrays.asList("ark:/99999/r2"));
        assertTrue("empty list returned if no bioSamples found", bioSamples.isEmpty());
    }

    @Test
    public void getBioSamples_returns_empty_list_when_no_bioSample_matches_in_repository() {
        BioSampleRepository repository = new RepositoryBuilder()
                .withProject(bioProject1())
                .build();

        List<String> searchBcids = bioProject2().bcids();

        List<BioSample> bioSamples = repository.getBioSamples(searchBcids);
        assertTrue("no bioSamples returned if bcid not in bcids list", bioSamples.isEmpty());
    }

    @Test
    public void getBioSamples_returns_bioSamples_in_bcids_list_when_bioSamples_in_same_bioProject_and_bioSamples_have_experiment_package() {
        BioProject bioProject = bioProject1();

        BioSampleRepository repository = new RepositoryBuilder()
                .withProject(bioProject)
                .build();

        List<String> bcids = bioProject.bcids();
        List<BioSample> bioSamples = repository.getBioSamples(bcids);

        assertTrue("bioSample not found in bcids list", bcids.contains(bioSamples.get(0).getBcid()));
        assertTrue("bioSample not found in bcids list", bcids.contains(bioSamples.get(1).getBcid()));
        assertTrue("bioSample missing experiment package", bioSamples.get(0).getSraExperimentPackage() != null);
        assertTrue("bioSample missing experiment package", bioSamples.get(1).getSraExperimentPackage() != null);
    }

    @Test
    public void getBioSamples_returns_bioSamples_in_bcids_list_when_bioSamples_in_different_bioProject_and_bioSamples_have_experiment_package() {
        BioProject bioProject1 = bioProject1();
        BioProject bioProject2 = bioProject2();

        BioSampleRepository repository = new RepositoryBuilder()
                .withProject(bioProject1)
                .withProject(bioProject2)
                .build();

        List<String> searchBcids = bioProject1.bcids();
        searchBcids.addAll(bioProject2.bcids());

        List<BioSample> bioSamples = repository.getBioSamples(searchBcids);

        for (BioSample bioSample: bioSamples) {
            assertTrue("bioSample not found in bcids list", searchBcids.contains(bioSample.getBcid()));
            assertTrue("bioSample missing experiment package", bioSample.getSraExperimentPackage() != null);
        }
    }

    @Test
    public void getBioSamples_returns_empty_list_when_bioSample_does_not_have_experiment_package() {
        BioProject bioProject = bioProjectWithBioSampleNoExperiments();

        BioSampleRepository repository = new RepositoryBuilder()
                .withProject(bioProject)
                .build();

        List<String> bcids = bioProject.bcids();
        List<BioSample> bioSamples = repository.getBioSamples(bcids);

        assertTrue("no bioSamples returned if bioSample missing experiment package", bioSamples.isEmpty());
    }

    @Test
    public void getBioSamples_returns_bioSamples_with_the_correct_experiment_package() {
        BioProject bioProject = bioProject1();

        BioSampleRepository repository = new RepositoryBuilder()
                .withProject(bioProject)
                .build();

        List<String> searchBcids = bioProject.bcids();
        List<BioSample> bioSamples = repository.getBioSamples(searchBcids);

        for (BioSample bioSample : bioSamples) {
            assertTrue("wrong sra experiment package for bioSample", bioSample.getSraExperimentPackage().hasBioSampleAccession(bioSample.getAccession()));
        }
    }

    private BioProject bioProjectWithBioSampleNoExperiments() {
        return new BioProject("bioProject1", bioProject1BioSamples(), Collections.emptyList());
    }

    private BioProject bioProject1() {
        List<SraExperimentPackage> experiments = Arrays.asList(bioSample1Experiments(), bioSample2Experiments());
        return new BioProject("bioProject1", bioProject1BioSamples(), experiments);
    }

    private BioProject bioProject2() {
        List<BioSample> bioSamples = Arrays.asList(bioSample3());
        List<SraExperimentPackage> experiments = Arrays.asList(bioSample3Experiments());

        return new BioProject("bioProject2", bioSamples, experiments);
    }

    private List<BioSample> bioProject1BioSamples() {
        return Arrays.asList(bioSample1(), bioSample2());
    }

    private static BioSample bioSample1() {
        return new BioSample(
                "bioSample1",
                "SAMNB1",
                "ark:/99999/r2",
                "bioProject1",
                "PRJN1"
        );
    }

    private static BioSample bioSample2() {
        return new BioSample(
                "bioSample2",
                "SAMNB2",
                "ark:/99999/s2",
                "bioProject1",
                "PRJN1"
        );
    }

    private static BioSample bioSample3() {
        return new BioSample(
                "bioSample3",
                "SAMNB3",
                "ark:/99999/t2",
                "bioProject2",
                "PRJN2"
        );
    }

    private SraExperimentPackage bioSample1Experiments() {
        return new SraExperimentPackage(
                "SRP1",
                "SRX1",
                "SAMNB1",
                Arrays.asList("SRR1")
        );
    }

    private SraExperimentPackage bioSample2Experiments() {
        return new SraExperimentPackage(
                "SRP2",
                "SRX2",
                "SAMNB2",
                Arrays.asList("SRR2")
        );
    }

    private SraExperimentPackage bioSample3Experiments() {
        return new SraExperimentPackage(
                "SRP3",
                "SRX3",
                "SAMNB3",
                Arrays.asList("SRR3")
        );
    }

    private class RepositoryBuilder {
        MockApiServiceBuilder apiServiceBuilder;
        List<BioProject> bioProjects;

        RepositoryBuilder() {
            apiServiceBuilder = new MockApiServiceBuilder();
            bioProjects = new ArrayList<>();
        }

        RepositoryBuilder withProject(BioProject bioProject) {
            this.bioProjects.add(bioProject);
            apiServiceBuilder.experiments(bioProject.getId(), bioProject.getExperiments());
            return this;
        }

        BioSampleRepository build() {
            addBioSamplesToApiService();
            return new BioSampleRepository(apiServiceBuilder.build());
        }

        private void addBioSamplesToApiService() {
            for (BioProject bioProject: bioProjects) {
                apiServiceBuilder.bioSamples(bioProject.getBioSamples());
            }
        }
    }

    private class BioProject {
        private final String id;
        private final List<BioSample> bioSamples;
        private final List<SraExperimentPackage> experiments;

        BioProject(String id, List<BioSample> bioSamples, List<SraExperimentPackage> experiments) {
            this.id = id;
            this.bioSamples = bioSamples;
            this.experiments = experiments;
        }

        public String getId() {
            return id;
        }

        public List<BioSample> getBioSamples() {
            return bioSamples;
        }

        public List<SraExperimentPackage> getExperiments() {
            return experiments;
        }

        public List<String> bcids() {
            List<String> bcids = new ArrayList<>();

            for (BioSample bioSample: this.bioSamples) {
                bcids.add(bioSample.getBcid());
            }

            return bcids;
        }
    }

    private class MockApiServiceBuilder {
        private List<BioSample> bioSamples;
        private Map<String, List<SraExperimentPackage>> experimentPackages;

        MockApiServiceBuilder() {
            this.bioSamples = new ArrayList<>();
            this.experimentPackages = new HashMap<>();
        }


        MockApiServiceBuilder bioSamples(List<BioSample> bioSamples) {
            this.bioSamples.addAll(bioSamples);
            return this;
        }

        MockApiServiceBuilder experiments(String bioProjectId, List<SraExperimentPackage> experiments) {
            this.experimentPackages.put(bioProjectId, experiments);
            return this;
        }

        EntrezApiService build() {
            List<String> bioSampleIds = getBioSampleIds();

            EntrezApiService apiService = mock(EntrezApiService.class);

            when(apiService.getBioSampleIdsWithBcidAttribute()).thenReturn(bioSampleIds);
            when(apiService.getBioSamplesFromIds(bioSampleIds)).thenReturn(bioSamples);

            if (_addExperiemntPackages()) {
                for (Map.Entry<String, List<SraExperimentPackage>> entry : experimentPackages.entrySet()) {

                    _setupExperimentPackageMock(apiService, entry.getKey(), entry.getValue());

                }
            }
            return apiService;
        }

        private List<String> getBioSampleIds() {
            List<String> bioSampleIds = new ArrayList<>();

            for (BioSample bs : bioSamples) {
                bioSampleIds.add(bs.getId());
            }

            return bioSampleIds;
        }

        private void _setupExperimentPackageMock(EntrezApiService apiService, String bioProjectId, List<SraExperimentPackage> experiments) {
            ArrayList<String> experimentIds = new ArrayList<>();

            for (int i = 0; i < experiments.size(); i++) {
                experimentIds.add(bioProjectId + "_" + i);
            }

            when(apiService.getSraExperimentPackageIds(new HashSet<>(Collections.singletonList(bioProjectId)))).thenReturn(experimentIds);
            when(apiService.getSraExperimentPackagesFromIds(experimentIds)).thenReturn(experiments);
        }

        private boolean _addExperiemntPackages() {
            return !experimentPackages.isEmpty();
        }

    }
}
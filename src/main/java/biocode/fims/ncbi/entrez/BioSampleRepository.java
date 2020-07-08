package biocode.fims.ncbi.entrez;

import biocode.fims.ncbi.models.BioSample;
import biocode.fims.ncbi.models.SraExperimentPackage;
import org.springframework.util.Assert;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author rjewing
 */
public class BioSampleRepository {
    private final EntrezApiService entrez;
    private List<BioSample> bioSamples;
    private Instant lastFetched;

    public BioSampleRepository(EntrezApiService entrez) {
        Assert.notNull(entrez);
        this.entrez = entrez;
    }

    /**
     * BioSamples are cached for 1 hour from ncbi
     *
     * @param bcids
     * @return
     */
    public List<BioSample> getBioSamples(List<String> bcids) {
        if (bcids.isEmpty()) {
            return new ArrayList<>();
        }

        return getBioSamplesFromEntrezApi(bcids);
    }

    private List<BioSample> getBioSamplesFromEntrezApi(List<String> bcids) {
        List<BioSample> bioSamples = getBioSamplesForBcids(bcids);

        if (bioSamples.isEmpty()) {
            return bioSamples;
        }

        List<SraExperimentPackage> experiments = getExperimentsFromEntrezApi(bioSamples);

        return filterBioSamplesWithExperiment(bioSamples, experiments);
    }

    private List<BioSample> getBioSamplesForBcids(List<String> bcids) {
        // cache bioSamples for 1 hour
        if (bioSamples == null || Instant.now().minus(1, ChronoUnit.HOURS).isAfter(lastFetched)) {
            List<String> bioSampleIds = entrez.getBioSampleIdsWithBcidAttribute();

            if (bioSampleIds.isEmpty()) {
                bioSamples = new ArrayList<>();
                lastFetched = Instant.now();
                return bioSamples;
            }

            bioSamples = entrez.getBioSamplesFromIds(bioSampleIds);
            lastFetched = Instant.now();

            // avoid being rate limited
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return filterBioSamplesMatchingBcids(bioSamples, bcids);

    }

    private List<BioSample> filterBioSamplesMatchingBcids(List<BioSample> bioSamples, List<String> bcids) {
        List<BioSample> filteredBioSamples = new ArrayList<>();

        for (BioSample bioSample : bioSamples) {
            String bcid = bioSample.getBcid();
            // strip any prefix to the ark id
            bcid = bcid.substring(bcid.indexOf("ark:/"));
            if (bcids.contains(bcid)) {
                filteredBioSamples.add(bioSample);
            }
        }

        return filteredBioSamples;
    }

    private List<SraExperimentPackage> getExperimentsFromEntrezApi(List<BioSample> bioSamples) {
        Set<String> bioProjectIds = getBioProjectIds(bioSamples);

        return getExperimentsForBioProjects(bioProjectIds);
    }

    private Set<String> getBioProjectIds(List<BioSample> bioSamples) {
        Set<String> bioProjectIds = new HashSet<>();

        for (BioSample bioSample : bioSamples) {
            bioProjectIds.add(bioSample.getBioProjectId());
        }
        return bioProjectIds;
    }

    private List<SraExperimentPackage> getExperimentsForBioProjects(Set<String> bioProjectIds) {
        List<String> sraExperimentPackageIds = entrez.getSraExperimentPackageIds(bioProjectIds);
        return entrez.getSraExperimentPackagesFromIds(sraExperimentPackageIds);
    }

    private List<BioSample> filterBioSamplesWithExperiment(List<BioSample> bioSamples, List<SraExperimentPackage> experiments) {
        List<BioSample> bioSamplesWithExperiments = new ArrayList<>();

        for (SraExperimentPackage experiment : experiments) {
            for (BioSample bioSample : bioSamples) {
                if (experiment.hasBioSampleAccession(bioSample.getAccession())) {
                    bioSample.setSraExperimentPackage(experiment);
                    bioSamplesWithExperiments.add(bioSample);
                    break;
                }
            }
        }

        return bioSamplesWithExperiments;
    }
}

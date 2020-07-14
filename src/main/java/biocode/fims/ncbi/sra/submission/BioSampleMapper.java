package biocode.fims.ncbi.sra.submission;

import biocode.fims.config.Config;
import biocode.fims.config.models.Entity;
import biocode.fims.ncbi.models.GeomeBioSample;
import biocode.fims.ncbi.models.SubmittableBioSample;
import biocode.fims.query.QueryResults;

import java.util.List;

/**
 * Interface to handle the mapping of Biocode FIMS project attributes to Sra BioSample attributes
 */
public interface BioSampleMapper {

    boolean hasNextSample();

    List<String> getHeaderValues();

    List<String> getBioSampleAttributes();

    List<GeomeBioSample> getBioSamples();

    BioSampleMapper newInstance(Config config, Entity fastqEntity, QueryResults queryResults, String bcidResolverPrefix);
}

package biocode.fims.ncbi.sra.submission;

import biocode.fims.config.Config;
import biocode.fims.config.models.Entity;
import biocode.fims.ncbi.models.GeomeBioSample;
import biocode.fims.ncbi.models.SubmittableBioSample;
import biocode.fims.query.QueryResults;
import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface to handle the mapping of Biocode FIMS project attributes to Sra BioSample attributes
 */
public interface BioSampleMapper {
    boolean hasNextSample();

    List<String> getHeaderValues();

    @Deprecated
    List<String> getBioSampleAttributes();

    List<GeomeBioSample> getBioSamples();

    Record nextRecord(); // ✅ Return Record (which is your GenericRecord)

    void reset();

    BioSampleMapper newInstance(Config config, Entity fastqEntity, QueryResults queryResults, String bcidResolverPrefix);

    Map<String, String> getLabelToUriMap();
    Set<String> getRequiredHeaders();

}

package biocode.fims.ncbi.sra.submission;

import biocode.fims.config.Config;
import biocode.fims.config.models.Entity;
import biocode.fims.ncbi.models.SraMetadata;
import biocode.fims.query.QueryResults;

import java.util.List;
import java.util.Map;

/**
 * Interface to handle the mapping of Biocode FIMS project attributes to Sra BioSample attributes
 */
public interface SraMetadataMapper {

    boolean hasNextResource();

    List<String> getHeaderValues();

    List<String> getResourceMetadata();

    List<SraMetadata> getResourceMetadataAsMap();

    SraMetadataMapper newInstance(Config config, Entity fastqEntity, QueryResults queryResults);
}

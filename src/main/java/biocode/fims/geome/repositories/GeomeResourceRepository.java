package biocode.fims.geome.repositories;

import biocode.fims.elasticSearch.EsResourceRepository;
import biocode.fims.entities.Resource;
import biocode.fims.fastq.FastqMetadataRepository;
import biocode.fims.repositories.ResourceRepository;
import org.elasticsearch.client.Client;

import java.util.List;

/**
 * @author rjewing
 */
public class GeomeResourceRepository extends EsResourceRepository implements FastqMetadataRepository, ResourceRepository {

    private final FastqMetadataRepository fastqMetadataRepository;

    public GeomeResourceRepository(Client esClient, FastqMetadataRepository fastqMetadataRepository) {
        super(esClient);
        this.fastqMetadataRepository = fastqMetadataRepository;
    }

    @Override
    public List<Resource> getResourcesWithFastqMetadataMissingBioSamples(int projectId) {
        return fastqMetadataRepository.getResourcesWithFastqMetadataMissingBioSamples(projectId);
    }
}

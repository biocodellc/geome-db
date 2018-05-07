package biocode.fims.geome.sra;

import biocode.fims.digester.Entity;
import biocode.fims.exceptions.SraCode;
import biocode.fims.fastq.FastqRecord;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.models.records.Record;
import biocode.fims.ncbi.sra.submission.AbstractSraMetadataMapper;
import biocode.fims.query.QueryResult;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * Class that maps geome project attributes to sra metadata
 */
public class GeomeSraMetadataMapper extends AbstractSraMetadataMapper {


    private final QueryResult parentResults;
    private final Iterator<Record> recordIt;
    private final String parentUniqueKey;
    private final Entity parentEntity;

    public GeomeSraMetadataMapper(QueryResult fastqResults, QueryResult parentResults) {
        this.parentResults = parentResults;

        if (fastqResults.records().size() == 0) {
            throw new FimsRuntimeException(SraCode.MISSING_DATASET, 400);
        }

        this.recordIt = fastqResults.records().iterator();
        this.parentEntity = parentResults.entity();
        this.parentUniqueKey = parentEntity.getUniqueKeyURI();
    }

    @Override
    public boolean hasNextResource() {
        return recordIt.hasNext();
    }

    @Override
    public List<String> getResourceMetadata() {
        FastqRecord record = (FastqRecord) recordIt.next();
        List<String> metadata = new ArrayList<>();

        Record parent = getParentForRecord(record);

        String sampleId = record.get(parentEntity.getUniqueKeyURI());

        String title;
        String species = parent.get("urn:species");
        String genus = parent.get("urn:genus");

        if (!StringUtils.isBlank(genus)) {
            title = record.libraryLayout() + "_" + genus;
            if (!StringUtils.isBlank(species)) {
                title += "_" + species;
            }
        } else {
            title = record.libraryStrategy() + "_" + parent.get("urn:phylum");
        }

        metadata.add(sampleId);
        metadata.add(sampleId);
        metadata.add(title);
        metadata.add(record.libraryStrategy());
        metadata.add(record.librarySource());
        metadata.add(record.librarySelection());
        metadata.add(record.libraryLayout());
        metadata.add(record.platform());
        metadata.add(record.instrumentModel());
        metadata.add(record.designDescription());
        metadata.add("fastq");
        metadata.add(record.filenames().get(0));

        if (record.libraryLayout().equals("paired")) {
            metadata.add(record.filenames().get(1));
        } else {
            metadata.add("");
        }

        return metadata;
    }

    private Record getParentForRecord(FastqRecord record) {
        String parentId = record.get(parentUniqueKey);
        return this.parentResults.records()
                .stream()
                .filter(r -> Objects.equals(r.get(parentUniqueKey), parentId))
                .findFirst()
                .orElse(null);
    }
}

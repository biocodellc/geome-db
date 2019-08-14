package biocode.fims.geome.sra;

import biocode.fims.config.Config;
import biocode.fims.fastq.FastqProps;
import biocode.fims.config.models.Entity;
import biocode.fims.exceptions.SraCode;
import biocode.fims.fastq.FastqRecord;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.records.Record;
import biocode.fims.ncbi.sra.submission.AbstractSraMetadataMapper;
import biocode.fims.query.QueryResult;
import biocode.fims.query.QueryResults;
import biocode.fims.records.RecordJoiner;
import biocode.fims.tissues.TissueProps;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * Class that maps geome project attributes to sra metadata
 */
public class GeomeSraMetadataMapper extends AbstractSraMetadataMapper {

    private final Iterator<Record> recordIt;
    private final RecordJoiner recordJoiner;

    public GeomeSraMetadataMapper(Config config, Entity fastqEntity, QueryResults queryResults) {
        QueryResult fastqResults = queryResults.getResult(fastqEntity.getConceptAlias());

        if (fastqResults.records().size() == 0) {
            throw new FimsRuntimeException(SraCode.MISSING_DATASET, 400);
        }

        this.recordIt = fastqResults.records().iterator();
        this.recordJoiner = new RecordJoiner(config, fastqEntity, queryResults);
    }

    @Override
    public boolean hasNextResource() {
        return recordIt.hasNext();
    }

    @Override
    public List<String> getResourceMetadata() {
        FastqRecord record = (FastqRecord) recordIt.next();
        List<String> metadata = new ArrayList<>();

        Record joinedRecord = recordJoiner.joinRecords(record);

        String title;
        String species = joinedRecord.get("urn:species");
        String genus = joinedRecord.get("urn:genus");

        if (!StringUtils.isBlank(genus)) {
            title = record.libraryLayout() + "_" + genus;
            if (!StringUtils.isBlank(species)) {
                title += "_" + species;
            }
        } else {
            title = record.libraryStrategy() + "_" + joinedRecord.get("urn:phylum");
        }

        metadata.add(joinedRecord.get(TissueProps.IDENTIFIER.uri()));
        metadata.add(joinedRecord.get(FastqProps.IDENTIFIER.uri()));
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
}

package biocode.fims.geome.sra;

import biocode.fims.config.Config;
import biocode.fims.fastq.FastqProps;
import biocode.fims.config.models.Entity;
import biocode.fims.exceptions.SraCode;
import biocode.fims.fastq.FastqRecord;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.ncbi.models.SraMetadata;
import biocode.fims.ncbi.sra.submission.SraMetadataMapper;
import biocode.fims.records.Record;
import biocode.fims.ncbi.sra.submission.AbstractSraMetadataMapper;
import biocode.fims.query.QueryResult;
import biocode.fims.query.QueryResults;
import biocode.fims.records.RecordJoiner;
import biocode.fims.tissues.TissueProps;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that maps geome project attributes to sra metadata
 */
public class GeomeSraMetadataMapper extends AbstractSraMetadataMapper {

    private Iterator<Record> recordIt;
    private RecordJoiner recordJoiner;
    private List<Record> records;

    public GeomeSraMetadataMapper(Config config, Entity fastqEntity, QueryResults queryResults) {
        QueryResult fastqResults = queryResults.getResult(fastqEntity.getConceptAlias());

        if (fastqResults.records().size() == 0) {
            throw new FimsRuntimeException(SraCode.MISSING_DATASET, 400);
        }

        this.records = fastqResults.records();
        this.recordIt = records.iterator();
        this.recordJoiner = new RecordJoiner(config, fastqEntity, queryResults);
    }

    public GeomeSraMetadataMapper() {
    }

    @Override
    public boolean hasNextResource() {
        return recordIt.hasNext();
    }

    @Override
    public List<SraMetadata> getResourceMetadataAsMap() {
        return records.stream().map(this::recordToMetadata).collect(Collectors.toList());
    }

    @Override
    public List<String> getResourceMetadata() {
        return new ArrayList<>(recordToMetadata(recordIt.next()).values());
    }

    private SraMetadata recordToMetadata(Record r) {
        FastqRecord record = (FastqRecord) r;

        SraMetadata metadata = new SraMetadata();
        List<String> headers = getHeaderValues();

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

        int i = 0;
        metadata.put(headers.get(i), joinedRecord.get(TissueProps.IDENTIFIER.uri()));
        i++;
        metadata.put(headers.get(i), joinedRecord.get(FastqProps.IDENTIFIER.uri()));
        i++;
        metadata.put(headers.get(i), title);
        i++;
        metadata.put(headers.get(i), record.libraryStrategy());
        i++;
        metadata.put(headers.get(i), record.librarySource());
        i++;
        metadata.put(headers.get(i), record.librarySelection());
        i++;
        metadata.put(headers.get(i), record.libraryLayout());
        i++;
        metadata.put(headers.get(i), record.platform());
        i++;
        metadata.put(headers.get(i), record.instrumentModel());
        i++;
        metadata.put(headers.get(i), record.designDescription());
        i++;
        metadata.put(headers.get(i), "fastq");
        i++;
        metadata.put(headers.get(i), record.filenames().get(0));
        i++;

        if (record.libraryLayout().equals("paired")) {
            metadata.put(headers.get(i), record.filenames().get(1));
        } else {
            metadata.put(headers.get(i), "");
        }
        i++;

        return metadata;
    }

    @Override
    public SraMetadataMapper newInstance(Config config, Entity fastqEntity, QueryResults queryResults) {
        return new GeomeSraMetadataMapper(config, fastqEntity, queryResults);
    }
}

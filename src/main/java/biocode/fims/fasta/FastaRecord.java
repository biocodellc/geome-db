package biocode.fims.fasta;

import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;
import biocode.fims.records.RecordMetadata;

import java.util.HashMap;
import java.util.Map;


/**
 * @author rjewing
 */
public class FastaRecord extends GenericRecord {

    public FastaRecord(String parentUniqueKeyUri, String parentIdentifier, String sequence, RecordMetadata recordMetadata) {
        super();
        properties.put(FastaProps.SEQUENCE.uri(), sequence);
        properties.put(parentUniqueKeyUri, parentIdentifier);

        for (Map.Entry e : recordMetadata.metadata().entrySet()) {
            properties.put((String) e.getKey(), e.getValue());
        }
    }

    public FastaRecord(Map<String, Object> properties, String rootIdentifier, int projectId, String expeditionCode, boolean shouldPersist) {
        super(properties, rootIdentifier, projectId, expeditionCode, shouldPersist);
    }

    @Override
    public Record clone() {
        return new FastaRecord(new HashMap<>(properties), rootIdentifier(), projectId(), expeditionCode(), persist);
    }

    public static String generateIdentifier(String localIdentifier, String marker) {
        return localIdentifier + "_" + marker;
    }
}


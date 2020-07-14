package biocode.fims.fasta.reader;

import biocode.fims.config.models.FastaEntity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fasta.FastaProps;
import biocode.fims.fasta.FastaRecord;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import biocode.fims.reader.DataConverter;
import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;

/**
 * @author rjewing
 */
public class FastaConverter implements DataConverter {
    protected ProjectConfig config;

    private String parentKey;

    public FastaConverter() {}

    private FastaConverter(ProjectConfig projectConfig) {
        this.config = projectConfig;
    }

    @Override
    public void convertRecordSet(RecordSet recordSet, int networkId) {
        if (!(recordSet.entity() instanceof FastaEntity)) {
            throw new FimsRuntimeException(DataReaderCode.READ_ERROR, 500);
        }

        FastaEntity fastaEntity = (FastaEntity) recordSet.entity();

        if (recordSet.recordsToPersist().size() == 0) return;

        String parent = fastaEntity.getParentEntity();
        parentKey = config.entity(parent).getUniqueKeyURI();

        updateRecords(recordSet);
    }

    /**
     * Generate unique identifiers for each record
     *
     * @param recordSet
     */
    private void updateRecords(RecordSet recordSet) {

        for (Record r : recordSet.recordsToPersist()) {

            if (r.get(FastaProps.IDENTIFIER.uri()).equals("")) {
                String parentID = r.get(parentKey);
                r.set(FastaProps.IDENTIFIER.uri(), FastaRecord.generateIdentifier(parentID, r.get(FastaProps.MARKER.uri())));
            }
        }
    }

    @Override
    public DataConverter newInstance(ProjectConfig projectConfig) {
        return new FastaConverter(projectConfig);
    }
}

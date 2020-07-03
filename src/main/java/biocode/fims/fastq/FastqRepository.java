package biocode.fims.fastq;

import biocode.fims.records.Record;

import java.util.List;

/**
 * @author rjewing
 */
public interface FastqRepository {

    List<Record> getRecords(int networkId, int projectId, String conceptAlias, List<String> parentIdentifiers);
}

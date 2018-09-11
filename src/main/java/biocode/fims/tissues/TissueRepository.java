package biocode.fims.tissues;

import biocode.fims.records.Record;

import java.util.List;

/**
 * @author rjewing
 */
public interface TissueRepository {

    List<String> getPlates(int networkId, int projectId, String conceptAlias, String plateColumn);

    List<Record> getTissues(int networkId, int projectId, String conceptAlias, List<String> parentIdentifiers);
}

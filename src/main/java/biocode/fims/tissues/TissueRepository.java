package biocode.fims.tissues;

import java.util.List;

/**
 * @author rjewing
 */
public interface TissueRepository {

    List<String> getPlates(int networkId, int projectId, String conceptAlias, String plateColumn);
}

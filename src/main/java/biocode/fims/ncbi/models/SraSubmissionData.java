package biocode.fims.ncbi.models;

import java.util.List;

/**
 * @author rjewing
 */
public class SraSubmissionData {
    public List<GeomeBioSample> bioSamples;
    public List<SraMetadata> sraMetadata;

    public SraSubmissionData(List<GeomeBioSample> bioSamples, List<SraMetadata> sraMetadata) {
        this.bioSamples = bioSamples;
        this.sraMetadata = sraMetadata;
    }
}

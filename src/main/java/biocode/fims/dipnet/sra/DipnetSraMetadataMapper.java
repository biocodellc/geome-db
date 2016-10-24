package biocode.fims.dipnet.sra;

import biocode.fims.dipnet.entities.FastqMetadata;
import biocode.fims.exceptions.SraCode;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.sra.AbstractSraMetadataMapper;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class that maps dipnet project attributes to sra metadata
 */
public class DipnetSraMetadataMapper extends AbstractSraMetadataMapper {

    private final Iterator samplesIt;
    private final FastqMetadata fastqMetadata;

    public DipnetSraMetadataMapper(FastqMetadata fastqMetadata, JSONArray samples) {
        this.fastqMetadata = fastqMetadata;

        if (fastqMetadata == null) {
            throw new FimsRuntimeException(SraCode.MISSING_FASTQ_METADATA, 400);
        }

        if (samples == null || samples.size() == 0) {
            throw new FimsRuntimeException(SraCode.MISSING_DATASET, 400);
        }

        this.samplesIt = samples.iterator();
    }

    @Override
    public boolean hasNextSample() {
        return samplesIt.hasNext();
    }

    @Override
    public List<String> getSampleMetadata() {
        JSONObject sample = (JSONObject) samplesIt.next();
        List<String> metadata = new ArrayList<>();
        String sampleId = (String) sample.get("materialSampleID");

        metadata.add(sampleId);
        metadata.add(fastqMetadata.getLibraryStrategy() + "_" + sample.get("species"));
        metadata.add(fastqMetadata.getLibraryStrategy());
        metadata.add(fastqMetadata.getLibrarySource());
        metadata.add(fastqMetadata.getLibrarySelection());
        metadata.add(fastqMetadata.getLibraryLayout());
        metadata.add(fastqMetadata.getPlatform());
        metadata.add(fastqMetadata.getInstrumentModel());
        metadata.add(fastqMetadata.getDesignDescription());
        metadata.add("fastq");
        metadata.add(getFilename(fastqMetadata.getFilenames(), sampleId, false));

        if (StringUtils.equalsIgnoreCase(fastqMetadata.getLibraryLayout(), "paired")) {
            metadata.add(getFilename(fastqMetadata.getFilenames(), sampleId, true));
        } else {
            metadata.add("");
        }

        return metadata;
    }
}

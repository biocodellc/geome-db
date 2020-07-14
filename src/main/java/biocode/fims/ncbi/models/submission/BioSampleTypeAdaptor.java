package biocode.fims.ncbi.models.submission;

import biocode.fims.rest.models.SraUploadMetadata;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * @author rjewing
 */
public class BioSampleTypeAdaptor extends XmlAdapter<String, SraUploadMetadata.BioSampleType> {
    @Override
    public SraUploadMetadata.BioSampleType unmarshal(String v) throws Exception {
        for (SraUploadMetadata.BioSampleType e : SraUploadMetadata.BioSampleType.values()) {
            if (e.toString().equals(v)) return e;
        }
        return null;
    }

    @Override
    public String marshal(SraUploadMetadata.BioSampleType v) throws Exception {
        return v.toString();
    }
}

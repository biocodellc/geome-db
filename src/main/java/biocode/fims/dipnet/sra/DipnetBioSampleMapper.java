package biocode.fims.dipnet.sra;

import biocode.fims.exceptions.SraCode;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.sra.BioSampleMapper;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class that maps dipnet project attributes to sra BioSample attributes
 */
public class DipnetBioSampleMapper implements BioSampleMapper {
    private static final List<String> BIOSAMPLE_HEADERS = new ArrayList<String>() {{
        add("sample_name");
        add("sample_title");
        add("organism");
        add("collection_date");
        add("geo_loc_name");
        add("tissue");
        add("biomaterial_provider");
        add("collected_by");
        add("depth");
        add("dev_stage");
        add("identified_by");
        add("lat_lon");
        add("sex");
        add("bcid");
    }};

    private final String libraryStrategy;
    private final Iterator samplesIt;
    private final String rootBcid;


    public DipnetBioSampleMapper(JSONArray samples, String libraryStrategy, String rootBcid) {
        this.libraryStrategy = libraryStrategy;
        this.rootBcid = rootBcid;

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
    public List<String> getHeaderValues() {
        return BIOSAMPLE_HEADERS;
    }

    @Override
    public List<String> getBioSampleAttributes() {
        JSONObject sample = (JSONObject) samplesIt.next();
        List<String> bioSampleAttributes = new ArrayList<>();

        bioSampleAttributes.add((String) sample.get("materialSampleID"));
        bioSampleAttributes.add(libraryStrategy + "_" + sample.get("species"));
        bioSampleAttributes.add(sample.get("genus") + " " + sample.get("species"));
        bioSampleAttributes.add(getCollectionDate(sample));

        StringBuilder geoLocSb = new StringBuilder();
        geoLocSb.append(sample.get("country"));
        if (!StringUtils.isBlank((String) sample.get("locality"))) {
            geoLocSb.append(": ");
            geoLocSb.append(sample.get("locality"));
        }
        bioSampleAttributes.add(geoLocSb.toString());

        bioSampleAttributes.add((String) sample.get("geneticTissueType"));
        bioSampleAttributes.add((String) sample.get("principalInvestigator"));
        bioSampleAttributes.add((String) sample.get("recordedBy"));

        StringBuilder depthSb = new StringBuilder();
        if (!StringUtils.isBlank((String) sample.get("minimumDepthInMeters"))) {
            depthSb.append(sample.get("minimumDepthInMeters"));

            if (!StringUtils.isBlank((String) sample.get("maximumDepthInMeters"))) {
                depthSb.append(", ");
                depthSb.append(sample.get("maximumDepthInMeters"));
            }
        }
        bioSampleAttributes.add(depthSb.toString());

        bioSampleAttributes.add((String) sample.get("lifeStage"));
        bioSampleAttributes.add((String) sample.get("identifiedBy"));

        StringBuilder latLongSb = new StringBuilder();
        if (!StringUtils.isBlank((String) sample.get("decimalLatitude")) &&
                !StringUtils.isBlank((String) sample.get("decimalLatitude"))) {

            latLongSb.append(sample.get("decimalLatitude"));
            latLongSb.append(" ");
            latLongSb.append(sample.get("decimalLongitude"));
        }
        bioSampleAttributes.add(latLongSb.toString());

        bioSampleAttributes.add((String) sample.get("sex"));
        bioSampleAttributes.add(rootBcid + sample.get("materialSampleID"));

        return bioSampleAttributes;
    }

    private String getCollectionDate(JSONObject sample) {
        StringBuilder collectionDate = new StringBuilder();

        collectionDate.append(sample.get("yearCollected"));

        String monthCollected = (String) sample.get("monthCollected");
        String dayCollected = (String) sample.get("dayCollected");

        if (!StringUtils.isBlank(monthCollected)) {
            collectionDate.append("-");
            collectionDate.append(monthCollected);

            if (!StringUtils.isBlank(dayCollected)) {
                collectionDate.append("-");
                collectionDate.append(dayCollected);
            }
        }

        return collectionDate.toString();
    }
}

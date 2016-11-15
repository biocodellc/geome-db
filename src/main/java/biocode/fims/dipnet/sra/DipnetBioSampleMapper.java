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
    private static final String BLANK_ATTRIBUTE = "missing";
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
        add("breed");
        add("host");
        add("age");
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

        String organism;
        String species = (String) sample.get("species");
        String genus = (String) sample.get("genus");

        if (!StringUtils.isBlank(genus)) {
            organism = genus;
            if (!StringUtils.isBlank(species)) {
                organism += " " + species;
            }
        } else {
            organism = (String) sample.get("phylum");
        }

        bioSampleAttributes.add((String) sample.get("materialSampleID"));
        bioSampleAttributes.add(libraryStrategy + "_" + organism.replace(" ", "_"));
        bioSampleAttributes.add(organism);
        bioSampleAttributes.add(getCollectionDate(sample));

        StringBuilder geoLocSb = new StringBuilder();
        geoLocSb.append(sample.get("country"));
        // must start with a country, otherwise sra validation fails
        if (!StringUtils.isBlank((String) sample.get("locality")) & !StringUtils.isBlank(geoLocSb.toString())){
            geoLocSb.append(": ");
            geoLocSb.append(sample.get("locality"));
        }
        bioSampleAttributes.add(modifyBlankAttribute(geoLocSb.toString()));

        bioSampleAttributes.add(modifyBlankAttribute((String) sample.get("geneticTissueType")));
        bioSampleAttributes.add((String) sample.get("principalInvestigator"));
        bioSampleAttributes.add(modifyBlankAttribute((String) sample.get("recordedBy")));

        StringBuilder depthSb = new StringBuilder();
        if (!StringUtils.isBlank((String) sample.get("minimumDepthInMeters"))) {
            depthSb.append(sample.get("minimumDepthInMeters"));

            if (!StringUtils.isBlank((String) sample.get("maximumDepthInMeters"))) {
                depthSb.append(", ");
                depthSb.append(sample.get("maximumDepthInMeters"));
            }
        }
        bioSampleAttributes.add(modifyBlankAttribute(depthSb.toString()));

        bioSampleAttributes.add(modifyBlankAttribute((String) sample.get("lifeStage")));
        bioSampleAttributes.add(modifyBlankAttribute((String) sample.get("identifiedBy")));

        StringBuilder latLongSb = new StringBuilder();
        if (!StringUtils.isBlank((String) sample.get("decimalLat")) &&
                !StringUtils.isBlank((String) sample.get("decimalLat"))) {

            latLongSb.append(sample.get("decimalLat"));
            latLongSb.append(" ");
            latLongSb.append(sample.get("decimalLong"));
        }
        bioSampleAttributes.add(modifyBlankAttribute(latLongSb.toString()));

        bioSampleAttributes.add(modifyBlankAttribute((String) sample.get("sex")));
        bioSampleAttributes.add(BLANK_ATTRIBUTE);
        bioSampleAttributes.add(BLANK_ATTRIBUTE);
        bioSampleAttributes.add(BLANK_ATTRIBUTE);
        bioSampleAttributes.add(rootBcid + sample.get("materialSampleID"));

        return bioSampleAttributes;
    }

    private String modifyBlankAttribute(String attribute) {
        if (StringUtils.isBlank(attribute)) {
            return BLANK_ATTRIBUTE;
        }

        return attribute;
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

        return modifyBlankAttribute(collectionDate.toString());
    }
}

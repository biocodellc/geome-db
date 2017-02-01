package biocode.fims.dipnet.sra;

import biocode.fims.exceptions.SraCode;
import biocode.fims.fastq.fileManagers.FastqFileManager;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fastq.sra.BioSampleMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang.StringUtils;

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

    private final Iterator<JsonNode> resourcesIt;

    public DipnetBioSampleMapper(ArrayNode resources) {

        if (resources == null || resources.size() == 0) {
            throw new FimsRuntimeException(SraCode.MISSING_DATASET, 400);
        }

        this.resourcesIt = resources.iterator();
    }

    @Override
    public boolean hasNextSample() {
        return resourcesIt.hasNext();
    }

    @Override
    public List<String> getHeaderValues() {
        return BIOSAMPLE_HEADERS;
    }

    @Override
    public List<String> getBioSampleAttributes() {
        ObjectNode sample = (ObjectNode) resourcesIt.next();
        List<String> bioSampleAttributes = new ArrayList<>();

        if (sample.has(FastqFileManager.CONCEPT_ALIAS)) {
            String organism;
            String species = getTextField(sample, "species");
            String genus = getTextField(sample, "genus");

            if (!StringUtils.isBlank(genus)) {
                organism = genus;
                if (!StringUtils.isBlank(species)) {
                    organism += " " + species;
                }
            } else {
                organism = getTextField(sample, "phylum");
            }

            bioSampleAttributes.add(getTextField(sample, "materialSampleID"));
            bioSampleAttributes.add(sample.at("/" + FastqFileManager.CONCEPT_ALIAS + "/libraryStrategy").asText() + "_" + organism.replace(" ", "_"));
            bioSampleAttributes.add(organism);
            bioSampleAttributes.add(getCollectionDate(sample));

            StringBuilder geoLocSb = new StringBuilder();
            geoLocSb.append(getTextField(sample,"country"));
            // must start with a country, otherwise sra validation fails
            if (!StringUtils.isBlank(getTextField(sample, "locality")) & !StringUtils.isBlank(geoLocSb.toString())) {
                geoLocSb.append(": ");
                geoLocSb.append(getTextField(sample, "locality"));
            }
            bioSampleAttributes.add(modifyBlankAttribute(geoLocSb.toString()));

            bioSampleAttributes.add(modifyBlankAttribute(getTextField(sample, "geneticTissueType")));
            bioSampleAttributes.add(getTextField(sample, "principalInvestigator"));
            bioSampleAttributes.add(modifyBlankAttribute(getTextField(sample, "recordedBy")));

            StringBuilder depthSb = new StringBuilder();
            if (!StringUtils.isBlank(getTextField(sample, "minimumDepthInMeters"))) {
                depthSb.append(getTextField(sample,"minimumDepthInMeters"));

                if (!StringUtils.isBlank(getTextField(sample, "maximumDepthInMeters"))) {
                    depthSb.append(", ");
                    depthSb.append(getTextField(sample, "maximumDepthInMeters"));
                }
            }
            bioSampleAttributes.add(modifyBlankAttribute(depthSb.toString()));

            bioSampleAttributes.add(modifyBlankAttribute(getTextField(sample, "lifeStage")));
            bioSampleAttributes.add(modifyBlankAttribute(getTextField(sample, "identifiedBy")));

            StringBuilder latLongSb = new StringBuilder();
            if (!StringUtils.isBlank(getTextField(sample, "decimalLatitude")) &&
                    !StringUtils.isBlank(getTextField(sample, "decimalLatitude"))) {

                latLongSb.append(getTextField(sample, "decimalLatitude"));
                latLongSb.append(" ");
                latLongSb.append(getTextField(sample, "decimalLongitude"));
            }
            bioSampleAttributes.add(modifyBlankAttribute(latLongSb.toString()));

            bioSampleAttributes.add(modifyBlankAttribute(getTextField(sample, "sex")));
            bioSampleAttributes.add(BLANK_ATTRIBUTE);
            bioSampleAttributes.add(BLANK_ATTRIBUTE);
            bioSampleAttributes.add(BLANK_ATTRIBUTE);
            bioSampleAttributes.add(sample.get("bcid").asText());
        }

        return bioSampleAttributes;
    }

    private String modifyBlankAttribute(String attribute) {
        if (StringUtils.isBlank(attribute)) {
            return BLANK_ATTRIBUTE;
        }

        return attribute;
    }

    private String getCollectionDate(ObjectNode sample) {
        StringBuilder collectionDate = new StringBuilder();

        collectionDate.append(getTextField(sample, "yearCollected"));

        String monthCollected = getTextField(sample, "monthCollected");
        String dayCollected = getTextField(sample, "dayCollected");

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

    private String getTextField(ObjectNode sample, String field) {
        if (sample.hasNonNull(field)) {
            return sample.get(field).asText();
        }
        return "";
    }
}

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
            String species = sample.get("species").asText();
            String genus = sample.get("genus").asText();

            if (!StringUtils.isBlank(genus)) {
                organism = genus;
                if (!StringUtils.isBlank(species)) {
                    organism += " " + species;
                }
            } else {
                organism = sample.get("phylum").asText();
            }

            bioSampleAttributes.add(sample.get("materialSampleID").asText());
            bioSampleAttributes.add(sample.at("/" + FastqFileManager.CONCEPT_ALIAS + "/libraryStrategy").asText() + "_" + organism.replace(" ", "_"));
            bioSampleAttributes.add(organism);
            bioSampleAttributes.add(getCollectionDate(sample));

            StringBuilder geoLocSb = new StringBuilder();
            geoLocSb.append(sample.get("country").asText());
            // must start with a country, otherwise sra validation fails
            if (!StringUtils.isBlank(sample.get("locality").asText()) & !StringUtils.isBlank(geoLocSb.toString())) {
                geoLocSb.append(": ");
                geoLocSb.append(sample.get("locality").asText());
            }
            bioSampleAttributes.add(modifyBlankAttribute(geoLocSb.toString()));

            bioSampleAttributes.add(modifyBlankAttribute(sample.get("geneticTissueType").asText()));
            bioSampleAttributes.add(sample.get("principalInvestigator").asText());
            bioSampleAttributes.add(modifyBlankAttribute(sample.get("recordedBy").asText()));

            StringBuilder depthSb = new StringBuilder();
            if (!StringUtils.isBlank(sample.get("minimumDepthInMeters").asText())) {
                depthSb.append(sample.get("minimumDepthInMeters").asText());

                if (!StringUtils.isBlank(sample.get("maximumDepthInMeters").asText())) {
                    depthSb.append(", ");
                    depthSb.append(sample.get("maximumDepthInMeters").asText());
                }
            }
            bioSampleAttributes.add(modifyBlankAttribute(depthSb.toString()));

            bioSampleAttributes.add(modifyBlankAttribute(sample.get("lifeStage").asText()));
            bioSampleAttributes.add(modifyBlankAttribute(sample.get("identifiedBy").asText()));

            StringBuilder latLongSb = new StringBuilder();
            if (!StringUtils.isBlank(sample.get("decimalLatitude").asText()) &&
                    !StringUtils.isBlank(sample.get("decimalLatitude").asText())) {

                latLongSb.append(sample.get("decimalLatitude").asText());
                latLongSb.append(" ");
                latLongSb.append(sample.get("decimalLongitude").asText());
            }
            bioSampleAttributes.add(modifyBlankAttribute(latLongSb.toString()));

            bioSampleAttributes.add(modifyBlankAttribute(sample.get("sex").asText()));
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

        collectionDate.append(sample.get("yearCollected").asText());

        String monthCollected = sample.get("monthCollected").asText();
        String dayCollected = sample.get("dayCollected").asText();

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

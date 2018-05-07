package biocode.fims.geome.sra;

import biocode.fims.digester.Entity;
import biocode.fims.exceptions.SraCode;
import biocode.fims.fastq.FastqRecord;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.models.records.Record;
import biocode.fims.ncbi.sra.submission.BioSampleMapper;
import biocode.fims.query.QueryResult;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * Class that maps geome project attributes to sra BioSample attributes
 */
public class GeomeBioSampleMapper implements BioSampleMapper {
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

    private final Iterator<Record> recordsIt;
    private final QueryResult fastqResults;
    private final QueryResult parentResults;
    private final Entity parentEntity;
    private final String parentUniqueKey;

    public GeomeBioSampleMapper(QueryResult fastqResults, QueryResult parentResults) {
        this.fastqResults = fastqResults;
        this.parentResults = parentResults;

        if (fastqResults.records().size() == 0) {
            throw new FimsRuntimeException(SraCode.MISSING_DATASET, 400);
        }

        this.parentEntity = parentResults.entity();
        this.parentUniqueKey = parentEntity.getUniqueKeyURI();

        this.recordsIt = fastqResults.records().iterator();
    }

    @Override
    public boolean hasNextSample() {
        return recordsIt.hasNext();
    }

    @Override
    public List<String> getHeaderValues() {
        return BIOSAMPLE_HEADERS;
    }

    @Override
    public List<String> getBioSampleAttributes() {
        FastqRecord record = (FastqRecord) recordsIt.next();
        List<String> bioSampleAttributes = new ArrayList<>();

        Record parent = getParentForRecord(record);

        String organism;
        String species = parent.get("urn:species");
        String genus = parent.get("urn:genus");

        if (!genus.equals("")) {
            organism = genus;
            if (!species.equals("")) {
                organism += " " + species;
            }
        } else {
            organism = parent.get("urn:phylum");
        }

        bioSampleAttributes.add(parent.get("urn:materialSampleID"));
        bioSampleAttributes.add(record.libraryStrategy() + "_" + organism.replace(" ", "_"));
        bioSampleAttributes.add(organism);
        bioSampleAttributes.add(getCollectionDate(parent));

        StringBuilder geoLocSb = new StringBuilder();
        geoLocSb.append(parent.get("urn:country"));
        // must start with a country, otherwise sra validation fails
        if (!StringUtils.isBlank(parent.get("urn:locality")) & !StringUtils.isBlank(geoLocSb.toString())) {
            geoLocSb.append(": ");
            geoLocSb.append(parent.get("urn:locality"));
        }
        bioSampleAttributes.add(modifyBlankAttribute(geoLocSb.toString()));

        bioSampleAttributes.add(modifyBlankAttribute(parent.get("urn:geneticTissueType")));
        bioSampleAttributes.add(parent.get("urn:principalInvestigator"));
        bioSampleAttributes.add(modifyBlankAttribute(parent.get("urn:recordedBy")));

        StringBuilder depthSb = new StringBuilder();
        if (!StringUtils.isBlank(parent.get("urn:minimumDepthInMeters"))) {
            depthSb.append(parent.get("urn:minimumDepthInMeters"));

            if (!StringUtils.isBlank(parent.get("urn:maximumDepthInMeters"))) {
                depthSb.append(", ");
                depthSb.append(parent.get("urn:maximumDepthInMeters"));
            }
        }
        bioSampleAttributes.add(modifyBlankAttribute(depthSb.toString()));

        bioSampleAttributes.add(modifyBlankAttribute(parent.get("urn:lifeStage")));
        bioSampleAttributes.add(modifyBlankAttribute(parent.get("urn:identifiedBy")));

        bioSampleAttributes.add(modifyBlankAttribute(getLatLong(parent)));

        bioSampleAttributes.add(modifyBlankAttribute(parent.get("urn:sex")));
        bioSampleAttributes.add(BLANK_ATTRIBUTE);
        bioSampleAttributes.add(BLANK_ATTRIBUTE);
        bioSampleAttributes.add(BLANK_ATTRIBUTE);
        bioSampleAttributes.add(this.fastqResults.rootIdentifier() + parent.get(parentUniqueKey));

        return bioSampleAttributes;
    }

    private String modifyBlankAttribute(String attribute) {
        if (StringUtils.isBlank(attribute)) {
            return BLANK_ATTRIBUTE;
        }

        return attribute;
    }

    private String getCollectionDate(Record record) {
        StringBuilder collectionDate = new StringBuilder();

        collectionDate.append(record.get("urn:yearCollected"));

        String monthCollected = record.get("urn:monthCollected");
        String dayCollected = record.get("urn:dayCollected");

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

    /**
     * if both lat and long are present, return a string containing the abs(decimalDegree) + Compass Direction
     * <p>
     * ex.
     * <p>
     * lat = -8, long = 140 would return "8 S 140 W"
     *
     * @param parent
     */
    private String getLatLong(Record parent) {
        StringBuilder latLongSb = new StringBuilder();

        if (!StringUtils.isBlank(parent.get("decimalLatitude")) &&
                !StringUtils.isBlank(parent.get("decimalLongitude"))) {

            String latText = parent.get("decimalLatitude");
            String lngText = parent.get("decimalLongitude");
            try {
                Double lat = Double.parseDouble(latText);

                if (lat < 0) {
                    latLongSb.append(Math.abs(lat)).append(" S");
                } else {
                    latLongSb.append(lat).append(" N");
                }

                latLongSb.append(" ");

                Double lng = Double.parseDouble(lngText);

                if (lng < 0) {
                    latLongSb.append(Math.abs(lng)).append(" W");
                } else {
                    latLongSb.append(lng).append(" E");
                }
            } catch (NumberFormatException e) {
                latLongSb = new StringBuilder()
                        .append(latText)
                        .append(" ")
                        .append(lngText);
            }
        }

        return latLongSb.toString();
    }

    private Record getParentForRecord(FastqRecord record) {
        String parentId = record.get(parentUniqueKey);
        return this.parentResults.records()
                .stream()
                .filter(r -> Objects.equals(r.get(parentUniqueKey), parentId))
                .findFirst()
                .orElse(null);
    }
}

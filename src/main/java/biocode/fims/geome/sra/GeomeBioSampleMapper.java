package biocode.fims.geome.sra;

import biocode.fims.config.models.Entity;
import biocode.fims.exceptions.SraCode;
import biocode.fims.fastq.FastqRecord;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.records.Record;
import biocode.fims.ncbi.sra.submission.BioSampleMapper;
import biocode.fims.query.QueryResult;
import biocode.fims.query.QueryResults;
import biocode.fims.records.RecordJoiner;
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
    private final RecordJoiner recordJoiner;
    private final String uniqueKeyURI;

    public GeomeBioSampleMapper(Entity fastqEntity, QueryResults queryResults) {
        QueryResult fastqResults = queryResults.getResult(fastqEntity.getConceptAlias());

        if (fastqResults.records().size() == 0) {
            throw new FimsRuntimeException(SraCode.MISSING_DATASET, 400);
        }

        // sort entities so children come first
        queryResults.sort(new QueryResults.ChildrenFirstComparator());

        this.recordsIt = fastqResults.records().iterator();
        this.recordJoiner = new RecordJoiner(fastqEntity, queryResults);
        this.uniqueKeyURI = fastqEntity.getUniqueKeyURI();
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

        Record joinedRecord = recordJoiner.joinRecords(record);

        String organism;
        String species = joinedRecord.get("urn:species");
        String genus = joinedRecord.get("urn:genus");

        if (!genus.equals("")) {
            organism = genus;
            if (!species.equals("")) {
                organism += " " + species;
            }
        } else {
            organism = joinedRecord.get("urn:phylum");
        }

        bioSampleAttributes.add(joinedRecord.get("urn:materialSampleID"));
        bioSampleAttributes.add(record.libraryStrategy() + "_" + organism.replace(" ", "_"));
        bioSampleAttributes.add(organism);
        bioSampleAttributes.add(getCollectionDate(joinedRecord));

        StringBuilder geoLocSb = new StringBuilder();
        geoLocSb.append(joinedRecord.get("urn:country"));
        // must start with a country, otherwise sra validation fails
        if (!StringUtils.isBlank(joinedRecord.get("urn:locality")) & !StringUtils.isBlank(geoLocSb.toString())) {
            geoLocSb.append(": ");
            geoLocSb.append(joinedRecord.get("urn:locality"));
        }
        bioSampleAttributes.add(modifyBlankAttribute(geoLocSb.toString()));

        bioSampleAttributes.add(modifyBlankAttribute(joinedRecord.get("urn:geneticTissueType")));
        bioSampleAttributes.add(joinedRecord.get("urn:principalInvestigator"));
        bioSampleAttributes.add(modifyBlankAttribute(joinedRecord.get("urn:recordedBy")));

        StringBuilder depthSb = new StringBuilder();
        if (!StringUtils.isBlank(joinedRecord.get("urn:minimumDepthInMeters"))) {
            depthSb.append(joinedRecord.get("urn:minimumDepthInMeters"));

            if (!StringUtils.isBlank(joinedRecord.get("urn:maximumDepthInMeters"))) {
                depthSb.append(", ");
                depthSb.append(joinedRecord.get("urn:maximumDepthInMeters"));
            }
        }
        bioSampleAttributes.add(modifyBlankAttribute(depthSb.toString()));

        bioSampleAttributes.add(modifyBlankAttribute(joinedRecord.get("urn:lifeStage")));
        bioSampleAttributes.add(modifyBlankAttribute(joinedRecord.get("urn:identifiedBy")));

        bioSampleAttributes.add(modifyBlankAttribute(getLatLong(joinedRecord)));

        bioSampleAttributes.add(modifyBlankAttribute(joinedRecord.get("urn:sex")));
        bioSampleAttributes.add(BLANK_ATTRIBUTE);
        bioSampleAttributes.add(BLANK_ATTRIBUTE);
        bioSampleAttributes.add(BLANK_ATTRIBUTE);
        // we don't need to entity.buildChildRecord here b/c fastq entity unique key is null
        // and thus only allows a 1-1 mapping to parents
        bioSampleAttributes.add(record.rootIdentifier() + record.get(uniqueKeyURI));

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

        if (!StringUtils.isBlank(parent.get("urn:decimalLatitude")) &&
                !StringUtils.isBlank(parent.get("urn:decimalLongitude"))) {

            String latText = parent.get("urn:decimalLatitude");
            String lngText = parent.get("urn:decimalLongitude");
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
}

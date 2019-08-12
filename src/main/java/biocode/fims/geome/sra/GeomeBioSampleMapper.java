package biocode.fims.geome.sra;

import biocode.fims.bcid.BcidBuilder;
import biocode.fims.config.Config;
import biocode.fims.config.models.Entity;
import biocode.fims.config.models.FastqEntity;
import biocode.fims.exceptions.SraCode;
import biocode.fims.fastq.FastqProps;
import biocode.fims.fastq.FastqRecord;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.records.Record;
import biocode.fims.ncbi.sra.submission.BioSampleMapper;
import biocode.fims.query.QueryResult;
import biocode.fims.query.QueryResults;
import biocode.fims.records.RecordJoiner;
import biocode.fims.tissues.TissueProps;
import org.apache.commons.lang3.StringUtils;

import java.text.DateFormatSymbols;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that maps geome project attributes to sra BioSample attributes
 */
public class GeomeBioSampleMapper implements BioSampleMapper {
    private static final String BLANK_ATTRIBUTE = "missing";
    private static final Map<String, String> BIOSAMPLE_MAPPING = new LinkedHashMap<String, String>() {{
        put("sample_name", TissueProps.IDENTIFIER.uri());
        put("tissue", "urn:geneticTissueType");
        put("biomaterial_provider", "urn:principalInvestigator");
        put("collected_by", "urn:recordedBy");
        put("dev_stage", "urn:lifeStage");
        put("identified_by", "urn:identifiedBy");
        put("sex", "urn:sex");
    }};

    private static final List<String> BIOSAMPLE_HEADERS = new ArrayList<String>() {{
        addAll(BIOSAMPLE_MAPPING.keySet());

        // The following attributes aren't an exact mapping and are typically a combination of multiple attributes
        add("sample_title");
        add("organism");
        add("collection_date");
        add("geo_loc_name");
        add("depth");
        add("lat_lon");
        add("breed");
        add("host");
        add("age");
        add("bcid");
    }};

    private final Iterator<Record> recordsIt;
    //    private final RecordJoiner recordJoiner;
    private final String parentEntity;
    private final BcidBuilder bcidBuilder;
    private final List<Record> records;
    private final RecordJoiner recordJoiner;
    private final QueryResults queryResults;
    private List<String> additionalDataUris = new ArrayList<>();

    public GeomeBioSampleMapper(Config config, Entity fastqEntity, QueryResults queryResults, String bcidResolverPrefix) {
        this.queryResults = queryResults;
        QueryResult fastqResults = queryResults.getResult(fastqEntity.getConceptAlias());

        if (fastqResults.records().size() == 0) {
            throw new FimsRuntimeException(SraCode.MISSING_DATASET, 400);
        }


//        this.recordsIt = fastqResults.records().iterator();
        this.parentEntity = fastqEntity.getParentEntity();
        Entity parent = config.entity(parentEntity);
        this.bcidBuilder = new BcidBuilder(parent, config.entity(parent.getParentEntity()), bcidResolverPrefix);

        this.recordJoiner = new RecordJoiner(config, fastqEntity, queryResults);
        this.records = fastqResults.records().stream().map(recordJoiner::joinRecords).collect(Collectors.toList());
        this.recordsIt = records.iterator();
    }

    @Override
    public boolean hasNextSample() {
        return recordsIt.hasNext();
    }

    @Override
    public List<String> getHeaderValues() {
        Map<String, String> uriToColumns = new HashMap<>();

        // Get all attributes that contain a value, excluding fastqMetadata attributes
        queryResults.stream()
                .filter(qr -> !qr.entity().type().equals(FastqEntity.TYPE))
                .forEach(qr -> {
                            Set<String> uris = new HashSet<>();
                            qr.records()
                                    .forEach(r ->
                                            uris.addAll(r.properties().keySet())
                                    );

                            Entity e = qr.entity();
                            uris.forEach(uri -> uriToColumns.put(uri, e.getAttributeColumn(uri)));
                        }
                );

        List<String> headers = new ArrayList<>(BIOSAMPLE_HEADERS);

        Collection<String> existingValues = BIOSAMPLE_MAPPING.values();
        additionalDataUris = uriToColumns.keySet().stream()
                .filter(uri -> !existingValues.contains(uri))
                .collect(Collectors.toList());
        headers.addAll(additionalDataUris.stream().map(uriToColumns::get).collect(Collectors.toList()));

        return headers;
    }

    /**
     * note: getHeaderValues needs to be called before this method
     *
     * @return
     */
    @Override
    public List<String> getBioSampleAttributes() {
        Record record = recordsIt.next();
        List<String> bioSampleAttributes = new ArrayList<>();

        for (String uri : BIOSAMPLE_MAPPING.values()) {
            bioSampleAttributes.add(modifyBlankAttribute(record.get(uri)));
        }

        String organism;
        String species = record.get("urn:species");
        String genus = record.get("urn:genus");
        String scientificName = record.get("urn:scientificName");

        if (!scientificName.equals("")) {
            organism = scientificName;
        } else if (!genus.equals("")) {
            organism = genus;
            if (!species.equals("")) {
                organism += " " + species;
            }
        } else {
            organism = record.get("urn:phylum");
        }

        bioSampleAttributes.add(record.get(FastqProps.LIBRARY_STRATEGY.uri()) + "_" + organism.replace(" ", "_"));
        bioSampleAttributes.add(organism);
        bioSampleAttributes.add(getCollectionDate(record));

        StringBuilder geoLocSb = new StringBuilder();
        geoLocSb.append(record.get("urn:country"));
        // must start with a country, otherwise sra validation fails
        if (!StringUtils.isBlank(record.get("urn:locality")) & !StringUtils.isBlank(geoLocSb.toString())) {
            geoLocSb.append(": ");
            geoLocSb.append(record.get("urn:locality"));
        }
        bioSampleAttributes.add(modifyBlankAttribute(geoLocSb.toString()));

        StringBuilder depthSb = new StringBuilder();
        if (!StringUtils.isBlank(record.get("urn:minimumDepthInMeters"))) {
            depthSb.append(record.get("urn:minimumDepthInMeters"));

            if (!StringUtils.isBlank(record.get("urn:maximumDepthInMeters"))) {
                depthSb.append(", ");
                depthSb.append(record.get("urn:maximumDepthInMeters"));
            }
        }
        bioSampleAttributes.add(modifyBlankAttribute(depthSb.toString()));

        bioSampleAttributes.add(modifyBlankAttribute(getLatLong(record)));

        bioSampleAttributes.add(BLANK_ATTRIBUTE);
        bioSampleAttributes.add(BLANK_ATTRIBUTE);
        bioSampleAttributes.add(BLANK_ATTRIBUTE);

        // this is the BioSample bcid, which in our case should be the Tissue
        Record tissue = recordJoiner.getParent(this.parentEntity, record);
        bioSampleAttributes.add(bcidBuilder.build(tissue));

        // add all non-empty data
        for (String uri : additionalDataUris) {
            bioSampleAttributes.add(record.get(uri));
        }

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

        String yearCollected = record.get("urn:yearCollected");

        if (StringUtils.isBlank(yearCollected)) return BLANK_ATTRIBUTE;

        String monthCollected = record.get("urn:monthCollected");
        String dayCollected = record.get("urn:dayCollected");

        if (!StringUtils.isBlank(dayCollected) && !StringUtils.isBlank(monthCollected)) {
            collectionDate.append(dayCollected);
            collectionDate.append("-");

            collectionDate.append(new DateFormatSymbols().getMonths()[Integer.parseInt(monthCollected) - 1]);
            collectionDate.append("-");
        } else if (!StringUtils.isBlank(monthCollected)) {
            collectionDate.append(new DateFormatSymbols().getMonths()[Integer.parseInt(monthCollected) - 1]);
            collectionDate.append("-");
        }

        collectionDate.append(yearCollected);

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

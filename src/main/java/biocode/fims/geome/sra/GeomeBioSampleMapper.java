package biocode.fims.geome.sra;

import biocode.fims.bcid.BcidBuilder;
import biocode.fims.config.Config;
import biocode.fims.config.models.Entity;
import biocode.fims.config.models.FastqEntity;
import biocode.fims.exceptions.SraCode;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.ncbi.models.GeomeBioSample;
import biocode.fims.ncbi.sra.submission.BioSampleMapper;
import biocode.fims.query.QueryResult;
import biocode.fims.query.QueryResults;
import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;
import biocode.fims.records.RecordJoiner;
import biocode.fims.tissues.TissueProps;
import org.apache.commons.lang3.StringUtils;

import java.text.DateFormatSymbols;
import java.util.*;
import java.util.stream.Collectors;

public class GeomeBioSampleMapper implements BioSampleMapper {
    private static final String BLANK_ATTRIBUTE = "missing";
    private static final List<String> BIOSAMPLE_HEADERS_TO_EXCLUDE = Collections.singletonList("urn:country");

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

    private Iterator<Record> recordsIt;
    private int recordIndex = 0;
    private String parentEntity;
    private BcidBuilder bcidBuilder;
    private List<Record> records;
    private RecordJoiner recordJoiner;
    private QueryResults queryResults;
    private List<String> additionalDataUris = new ArrayList<>();
    private List<String> headers;
    private Set<String> existingTissues = new HashSet<>();

    public GeomeBioSampleMapper(Config config, Entity fastqEntity, QueryResults queryResults, String bcidResolverPrefix) {
        this.queryResults = queryResults;
        QueryResult fastqResults = queryResults.getResult(fastqEntity.getConceptAlias());

        if (fastqResults.records().size() == 0) {
            throw new FimsRuntimeException(SraCode.MISSING_DATASET, 400);
        }

        this.parentEntity = fastqEntity.getParentEntity();
        Entity parent = config.entity(parentEntity);
        this.bcidBuilder = new BcidBuilder(parent, config.entity(parent.getParentEntity()), bcidResolverPrefix);
        this.recordJoiner = new RecordJoiner(config, fastqEntity, queryResults);
        this.records = queryResults.getResult(parentEntity).records().stream()
            .map(recordJoiner::joinRecords)
            .collect(Collectors.toList());
        System.out.println(">>> GeomeBioSampleMapper initialized");
        System.out.println(">>> records size: " + records.size());

        this.recordIndex = 0; // IMPORTANT

        //this.records = queryResults.getResult(parentEntity).records().stream().map(recordJoiner::joinRecords).collect(Collectors.toList());
        //this.recordsIt = records.iterator();
    }

    public GeomeBioSampleMapper() {}
    public int getRecordCount() {
        return records != null ? records.size() : 0;
    }
    @Override
    public boolean hasNextSample() {
        System.out.println(">>> hasNextSample() called: recordIndex=" + recordIndex + ", records.size()=" + records.size());

        return recordIndex < records.size();
    }
    @Override
    public Set<String> getRequiredHeaders() {
        return new HashSet<>(BIOSAMPLE_HEADERS); // Or keep a cached static copy
    }

    private Map<String, String> labelToUri = new LinkedHashMap<>();

    @Override
    public List<String> getHeaderValues() {
        if (headers != null) return headers;

        headers = new ArrayList<>();
        labelToUri = new LinkedHashMap<>();

        // Step 1: Fixed BIOSAMPLE_MAPPING headers
        for (Map.Entry<String, String> entry : BIOSAMPLE_MAPPING.entrySet()) {
            String label = entry.getKey();
            String uri = entry.getValue();
            headers.add(label);
            labelToUri.put(label, uri);
        }

        // Step 2: Fixed synthetic fields
        headers.add("sample_title");
        headers.add("organism");
        headers.add("collection_date");
        headers.add("geo_loc_name");
        headers.add("depth");
        headers.add("lat_lon");
        headers.add("breed");
        headers.add("host");
        headers.add("age");
        headers.add("bcid");

        // Step 3: Dynamic columns
        Map<String, String> uriToColumns = new HashMap<>();
        queryResults.stream()
            .filter(qr -> !qr.entity().type().equals(FastqEntity.TYPE))
            .forEach(qr -> {
                Entity e = qr.entity();
                for (Record r : qr.records()) {
                    for (String uri : r.properties().keySet()) {
                        uriToColumns.put(uri, e.getAttributeColumn(uri));
                    }
                }
            });

        Collection<String> mappedUris = BIOSAMPLE_MAPPING.values();
        additionalDataUris = uriToColumns.keySet().stream()
            .filter(uri -> !mappedUris.contains(uri) && !BIOSAMPLE_HEADERS_TO_EXCLUDE.contains(uri))
            .sorted()
            .collect(Collectors.toList());

        for (String uri : additionalDataUris) {
            String label = uriToColumns.get(uri);
            if (label != null) {
                headers.add(label);
                labelToUri.put(label, uri);
            }
        }

        return headers;
    }


    public Map<String, String> getLabelToUriMap() {
        return labelToUri;
    }


    @Override
    public List<GeomeBioSample> getBioSamples() {
        return records.stream().map(this::recordToBioSample).collect(Collectors.toList());
    }

    @Override
    public List<String> getBioSampleAttributes() {
          return new ArrayList<>(recordToBioSample(recordsIt.next()).values());
      }

    @Override
    public Record nextRecord() {
        while (recordIndex < records.size()) {
            System.out.println(">>> Processing record at index: " + recordIndex);
            Record original = records.get(recordIndex++);
            GeomeBioSample bioSample = recordToBioSample(original);

            if (bioSample.isEmpty()) {
                System.out.println(">>> Skipping empty biosample.");
                continue;
            }

            return new GenericRecord(
                    new LinkedHashMap<>(bioSample),
                    original.rootIdentifier(),
                    original.projectId(),
                    original.expeditionCode(),
                    true
            );
        }
        System.out.println(">>> nextRecord() returned null (no more records)");
        return null;
    }




    @Override
    public void reset() {
        this.recordIndex = 0;
    }

    public GeomeBioSampleMapper newInstance(Config config, Entity fastqEntity, QueryResults queryResults, String bcidResolverPrefix) {
        return new GeomeBioSampleMapper(config, fastqEntity, queryResults, bcidResolverPrefix);
    }

    private GeomeBioSample recordToBioSample(Record record) {
        if (headers == null) getHeaderValues();

        GeomeBioSample bioSample = new GeomeBioSample();

        // Avoid duplicate tissues
        String tissueID = record.get(TissueProps.IDENTIFIER.uri());
        if (existingTissues.contains(tissueID)) return bioSample;
        existingTissues.add(tissueID);

        // Mapped BIOSAMPLE fields
        for (Map.Entry<String, String> entry : BIOSAMPLE_MAPPING.entrySet()) {
            String label = entry.getKey();
            String uri = entry.getValue();
            String value = record.get(uri);
            bioSample.put(label, StringUtils.isBlank(value) ? "missing" : value);
        }

        // Organism composite
        String species = record.get("urn:species");
        String genus = record.get("urn:genus");
        String scientificName = record.get("urn:scientificName");
        String organism;
        if (!StringUtils.isBlank(scientificName)) {
            organism = scientificName;
        } else if (!StringUtils.isBlank(genus)) {
            organism = StringUtils.isBlank(species) ? genus : genus + " " + species;
        } else {
            organism = record.get("urn:phylum");
        }

        bioSample.put("sample_title", StringUtils.isBlank(organism) ? "missing" : organism.replace(" ", "_"));
        bioSample.put("organism", StringUtils.isBlank(organism) ? "missing" : organism);
        bioSample.put("collection_date", getCollectionDate(record));

        // geo_loc_name
        String country = record.get("urn:country");
        String locality = record.get("urn:locality");
        String geoLoc = "";
        if (!StringUtils.isBlank(country)) {
            geoLoc = country;
            if (!StringUtils.isBlank(locality)) {
                geoLoc += ": " + locality;
            }
        }
        bioSample.put("geo_loc_name", StringUtils.isBlank(geoLoc) ? "missing" : geoLoc);

        // depth
        String min = record.get("urn:minimumDepthInMeters");
        String max = record.get("urn:maximumDepthInMeters");
        String depth = "";
        if (!StringUtils.isBlank(min)) {
            depth = min;
            if (!StringUtils.isBlank(max)) depth += ", " + max;
        }
        bioSample.put("depth", StringUtils.isBlank(depth) ? "missing" : depth);

        // lat_lon
        String latLon = getLatLong(record);
        bioSample.put("lat_lon", StringUtils.isBlank(latLon) ? "missing" : latLon);

        // breed, host, age → hardcoded
        bioSample.put("breed", "missing");
        bioSample.put("host", "missing");
        bioSample.put("age", "missing");

        // bcid
        Record tissue = recordJoiner.getParent(this.parentEntity, record);
        String bcid = bcidBuilder.build(tissue);
        bioSample.put("bcid", StringUtils.isBlank(bcid) ? "missing" : bcid);

        // Dynamic fields
        for (Map.Entry<String, String> entry : labelToUri.entrySet()) {
            String label = entry.getKey();
            String uri = entry.getValue();

            // Skip if already written as part of BIOSAMPLE_MAPPING or fixed fields
            if (bioSample.containsKey(label)) continue;

            String value = record.get(uri);
            bioSample.put(label, value != null ? value : "");
        }

        return bioSample;
    }


    private String getOrganism(Record record) {
        String species = record.get("urn:species");
        String genus = record.get("urn:genus");
        String scientificName = record.get("urn:scientificName");
        if (!StringUtils.isBlank(scientificName)) return scientificName;
        if (!StringUtils.isBlank(genus)) return StringUtils.isBlank(species) ? genus : genus + " " + species;
        return record.get("urn:phylum");
    }

    private String getGeoLoc(Record record) {
        String country = record.get("urn:country");
        String locality = record.get("urn:locality");
        if (StringUtils.isBlank(country)) return BLANK_ATTRIBUTE;
        if (!StringUtils.isBlank(locality)) return country + ": " + locality;
        return country;
    }

    private String getDepth(Record record) {
        String min = record.get("urn:minimumDepthInMeters");
        String max = record.get("urn:maximumDepthInMeters");
        if (StringUtils.isBlank(min)) return BLANK_ATTRIBUTE;
        return StringUtils.isBlank(max) ? min : min + ", " + max;
    }

    private String getCollectionDate(Record record) {
        String yearCollected = record.get("urn:yearCollected");
        if (StringUtils.isBlank(yearCollected)) return BLANK_ATTRIBUTE;

        StringBuilder sb = new StringBuilder();
        String monthCollected = record.get("urn:monthCollected");
        String dayCollected = record.get("urn:dayCollected");

        if (!StringUtils.isBlank(dayCollected) && !StringUtils.isBlank(monthCollected)) {
            sb.append(dayCollected).append("-");
            sb.append(new DateFormatSymbols().getShortMonths()[Integer.parseInt(monthCollected) - 1]).append("-");
        } else if (!StringUtils.isBlank(monthCollected)) {
            sb.append(new DateFormatSymbols().getShortMonths()[Integer.parseInt(monthCollected) - 1]).append("-");
        }

        sb.append(yearCollected);
        return sb.toString();
    }

    private String getLatLong(Record record) {
        String latText = record.get("urn:decimalLatitude");
        String lngText = record.get("urn:decimalLongitude");

        if (!StringUtils.isBlank(latText) && !StringUtils.isBlank(lngText)) {
            try {
                Double lat = Double.parseDouble(latText);
                Double lng = Double.parseDouble(lngText);
                return Math.abs(lat) + (lat < 0 ? " S" : " N") + " " + Math.abs(lng) + (lng < 0 ? " W" : " E");
            } catch (NumberFormatException e) {
                return latText + " " + lngText;
            }
        }

        return BLANK_ATTRIBUTE;
    }
}

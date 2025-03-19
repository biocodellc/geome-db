package biocode.fims.geome.sra;

import biocode.fims.bcid.BcidBuilder;
import biocode.fims.config.Config;
import biocode.fims.config.models.Entity;
import biocode.fims.config.models.FastqEntity;
import biocode.fims.exceptions.SraCode;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.ncbi.models.GeomeBioSample;
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
 * Class that maps geome project attributes to SRA BioSample attributes.
 */
public class GeomeBioSampleMapper implements BioSampleMapper {
    private static final String BLANK_ATTRIBUTE = "missing";
    private static final List<String> BIOSAMPLE_HEADERS_TO_EXCLUDE = Arrays.asList("urn:country");

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
        addAll(Arrays.asList("sample_title", "organism", "collection_date", "geo_loc_name",
                             "depth", "lat_lon", "breed", "host", "age", "bcid"));
    }};

    private Iterator<Record> recordsIt;
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

        if (fastqResults.records().isEmpty()) {
            throw new FimsRuntimeException(SraCode.MISSING_DATASET, 400);
        }

        this.parentEntity = fastqEntity.getParentEntity();
        Entity parent = config.entity(parentEntity);
        this.bcidBuilder = new BcidBuilder(parent, config.entity(parent.getParentEntity()), bcidResolverPrefix);
        this.recordJoiner = new RecordJoiner(config, fastqEntity, queryResults);

        this.records = queryResults.getResult(parentEntity).records().stream()
                .map(recordJoiner::joinRecords)
                .collect(Collectors.toList());
        this.recordsIt = records.iterator();
    }

    public GeomeBioSampleMapper() {}

    @Override
    public boolean hasNextSample() {
        return recordsIt.hasNext();
    }

    @Override
    public List<String> getHeaderValues() {
        if (headers != null) return headers;
        Map<String, String> uriToColumns = new HashMap<>();

        queryResults.stream()
                .filter(qr -> !qr.entity().type().equals(FastqEntity.TYPE))
                .forEach(qr -> {
                    Set<String> uris = new HashSet<>();
                    qr.records().forEach(r -> uris.addAll(r.properties().keySet()));
                    Entity e = qr.entity();
                    uris.forEach(uri -> uriToColumns.put(uri, e.getAttributeColumn(uri)));
                });

        headers = new ArrayList<>(BIOSAMPLE_HEADERS);

        Collection<String> existingValues = BIOSAMPLE_MAPPING.values();
        additionalDataUris = uriToColumns.keySet().stream()
                .filter(uri -> !existingValues.contains(uri) && !BIOSAMPLE_HEADERS_TO_EXCLUDE.contains(uri))
                .sorted()
                .collect(Collectors.toList());
        headers.addAll(additionalDataUris.stream().map(uriToColumns::get).collect(Collectors.toList()));

        return headers;
    }

    @Override
    public List<GeomeBioSample> getBioSamples() {
        return records.stream().map(this::recordToBioSample).collect(Collectors.toList());
    }

    public GeomeBioSampleMapper newInstance(Config config, Entity fastqEntity, QueryResults queryResults, String bcidResolverPrefix) {
         return new GeomeBioSampleMapper(config, fastqEntity, queryResults, bcidResolverPrefix);
     }

    @Override
    public List<String> getBioSampleAttributes() {
        if (!recordsIt.hasNext()) return Collections.emptyList();

        Record record = recordsIt.next();
        GeomeBioSample bioSample = recordToBioSample(record);

        return headers.stream()
                .map(header -> bioSample.getOrDefault(header, BLANK_ATTRIBUTE))
                .collect(Collectors.toList());
    }

    private GeomeBioSample recordToBioSample(Record record) {
        if (headers == null) getHeaderValues();

        GeomeBioSample bioSample = new GeomeBioSample();
        String tissueID = record.get(TissueProps.IDENTIFIER.uri());
        if (existingTissues.contains(tissueID)) return bioSample;
        existingTissues.add(tissueID);

        int i = 0;
        for (String uri : BIOSAMPLE_MAPPING.values()) {
            bioSample.put(headers.get(i++), modifyBlankAttribute(record.get(uri)));
        }

        bioSample.put(headers.get(i++), getOrganism(record));
        bioSample.put(headers.get(i++), getCollectionDate(record));
        bioSample.put(headers.get(i++), modifyBlankAttribute(getGeoLocation(record)));
        bioSample.put(headers.get(i++), modifyBlankAttribute(getDepth(record)));
        bioSample.put(headers.get(i++), modifyBlankAttribute(getLatLong(record)));
        bioSample.put(headers.get(i++), BLANK_ATTRIBUTE);
        bioSample.put(headers.get(i++), BLANK_ATTRIBUTE);
        bioSample.put(headers.get(i++), BLANK_ATTRIBUTE);

        Record tissue = recordJoiner.getParent(this.parentEntity, record);
        bioSample.put(headers.get(i++), bcidBuilder.build(tissue));

        for (String uri : additionalDataUris) {
            bioSample.put(headers.get(i++), modifyBlankAttribute(record.get(uri)));
        }

        return bioSample;
    }

    private String modifyBlankAttribute(String attribute) {
        return (StringUtils.isBlank(attribute)) ? BLANK_ATTRIBUTE : attribute;
    }

    private String getOrganism(Record record) {
        String species = record.get("urn:species");
        String genus = record.get("urn:genus");
        String scientificName = record.get("urn:scientificName");

        if (!scientificName.isEmpty()) return scientificName;
        if (!genus.isEmpty()) return genus + (species.isEmpty() ? "" : " " + species);
        return record.get("urn:phylum");
    }

    private String getCollectionDate(Record record) {
        String year = record.get("urn:yearCollected");
        if (StringUtils.isBlank(year)) return BLANK_ATTRIBUTE;

        String month = record.get("urn:monthCollected");
        String day = record.get("urn:dayCollected");

        if (!StringUtils.isBlank(day) && !StringUtils.isBlank(month)) {
            return day + "-" + new DateFormatSymbols().getShortMonths()[Integer.parseInt(month) - 1] + "-" + year;
        } else if (!StringUtils.isBlank(month)) {
            return new DateFormatSymbols().getShortMonths()[Integer.parseInt(month) - 1] + "-" + year;
        }
        return year;
    }

    private String getGeoLocation(Record record) {
        StringBuilder geoLocSb = new StringBuilder(record.get("urn:country"));

        if (!StringUtils.isBlank(record.get("urn:locality")) && !geoLocSb.toString().isEmpty()) {
            geoLocSb.append(": ").append(record.get("urn:locality"));
        }

        return geoLocSb.toString();
    }

    private String getDepth(Record record) {
        String minDepth = record.get("urn:minimumDepthInMeters");
        String maxDepth = record.get("urn:maximumDepthInMeters");

        if (StringUtils.isBlank(minDepth)) return BLANK_ATTRIBUTE;
        return (StringUtils.isBlank(maxDepth)) ? minDepth : minDepth + ", " + maxDepth;
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

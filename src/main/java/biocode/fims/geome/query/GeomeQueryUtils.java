package biocode.fims.geome.query;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.DataType;
import biocode.fims.digester.Entity;
import biocode.fims.digester.Mapping;
import biocode.fims.elasticSearch.query.ElasticSearchFilterField;
import biocode.fims.fasta.FastaSequenceFields;
import biocode.fims.fasta.fileManagers.FastaFileManager;
import biocode.fims.fastq.fileManagers.FastqFileManager;
import biocode.fims.query.writers.JsonFieldTransform;
import com.fasterxml.jackson.core.JsonPointer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Helper class for obtaining GeOMe specific Query information
 *
 * @author RJ Ewing
 */
public class GeomeQueryUtils {

    private static final List<String> FILTER_COLUMNS = Arrays.asList(
            "materialSampleID",
            "decimalLatitude",
            "decimalLongitude",
            "lifeStage",
            "sex",
            "wormsID",
            "stateProvince",
            "island",
            "preservative",
            "habitat",
            "class",
            "order",
            "family",
            "subSpecies",
            "vernacularName"
    );

    /**
     * get the list of filterable fields to query against
     *
     * @param mapping
     * @return
     */
    public static List<ElasticSearchFilterField> getAvailableFilters(Mapping mapping) {
        List<ElasticSearchFilterField> filters = new ArrayList<>();

        for (Attribute attribute : mapping.getDefaultSheetAttributes()) {
            filters.add(new ElasticSearchFilterField(attribute.getUri(), attribute.getColumn(), attribute.getDatatype()));
        }

        filters.addAll(getFastaFilters(mapping));
        filters.addAll(getFastqFilters());
        return filters;
    }

    private static List<ElasticSearchFilterField> getFastqFilters() {
        // TODO refactor when we refactor fastqMetadata to use configuration file
        List<ElasticSearchFilterField> filters = new ArrayList<>();

        filters.add(
                new ElasticSearchFilterField(
                        "fastqMetadata.bioSample",
                        "fastqMetadata.bioSample",
                        DataType.STRING
                )
        );

        return filters;
    }

    /**
     * get the list of filterable fields for fastaSequence
     *
     * @param mapping
     * @return
     */
    public static List<ElasticSearchFilterField> getFastaFilters(Mapping mapping) {
        List<ElasticSearchFilterField> filters = new ArrayList<>();

        Entity fastaEntity = mapping.findEntity(FastaFileManager.ENTITY_CONCEPT_ALIAS);

        for (Attribute attribute : fastaEntity.getAttributes()) {
            filters.add(
                    new ElasticSearchFilterField(
                            fastaEntity.getConceptAlias() + "." + attribute.getUri(),
                            fastaEntity.getConceptAlias() + "." + attribute.getColumn(),
                            attribute.getDatatype())
                            .nested(fastaEntity.isEsNestedObject())
                            .path(fastaEntity.getConceptAlias())
            );
        }

        return filters;
    }

    /**
     * ElasticSearchFilterField that will search all fields.
     *
     * @return
     */
    public static ElasticSearchFilterField get_AllFilter() {
        return new ElasticSearchFilterField("_all", null, null);
    }

    /**
     * get a list of attributes as JsonFieldTransform objects to be used in transforming
     * the json resource fields into human readable fields.
     *
     * @param mapping
     * @return
     */
    public static List<JsonFieldTransform> getJsonFieldTransforms(Mapping mapping) {
        List<JsonFieldTransform> fieldTransforms = new ArrayList<>();

        for (Attribute a : mapping.getDefaultSheetAttributes()) {
            fieldTransforms.add(
                    new JsonFieldTransform(
                            a.getColumn(),
                            a.getUri(),
                            a.getDatatype()
                    )
            );
        }

        fieldTransforms.add(
                new JsonFieldTransform(
                        "bcid",
                        "bcid",
                        DataType.STRING
                )
        );

        return fieldTransforms;
    }

    /**
     * get a {@link FastaSequenceFields} object representing the project {@link Mapping}
     *
     * @param mapping
     * @return
     */
    public static FastaSequenceFields getFastaSequenceFields(Mapping mapping) {
        List<JsonFieldTransform> metadataFields = new ArrayList<>();

        Entity fastaEntity = mapping.findEntity(FastaFileManager.ENTITY_CONCEPT_ALIAS);
        String sequencePath = FastaFileManager.SEQUENCE_ATTRIBUTE_URI;
        String uniqueKeyPath = null;

        for (Attribute a : fastaEntity.getAttributes()) {

            if (fastaEntity.getUniqueKey().equals(a.getColumn())) {
                uniqueKeyPath = a.getUri();
            }
            if (!a.getUri().equals(FastaFileManager.SEQUENCE_ATTRIBUTE_URI)) {

                metadataFields.add(new JsonFieldTransform(
                        a.getColumn(),
                        a.getUri(),
                        a.getDatatype()
                ));
            }
        }


        FastaSequenceFields fastaSequenceFields = new FastaSequenceFields(
                fastaEntity.getConceptAlias(),
                "bcid",
                sequencePath,
                metadataFields
        );

        fastaSequenceFields.setUniqueKeyPath(uniqueKeyPath);

        return fastaSequenceFields;
    }

    public static JsonPointer getLongitudePointer() {
        return JsonPointer.compile("/urn:decimalLongitude");
    }

    public static JsonPointer getLatitudePointer() {
        return JsonPointer.compile("/urn:decimalLatitude");
    }

    public static JsonPointer getUniqueKeyPointer() {
        return JsonPointer.compile("/urn:materialSampleID");
    }

    public static List<JsonFieldTransform> getFastqJsonFieldTransforms(Mapping mapping) {
        List<JsonFieldTransform> jsonFieldTransforms = getJsonFieldTransforms(mapping);

        jsonFieldTransforms.add(
                new JsonFieldTransform(
                        "fastqLibraryStrategy",
                        FastqFileManager.CONCEPT_ALIAS + ".libraryStrategy",
                        DataType.STRING
                )
        );
        jsonFieldTransforms.add(
                new JsonFieldTransform(
                        "fastqLibrarySource",
                        FastqFileManager.CONCEPT_ALIAS + ".librarySource",
                        DataType.STRING
                )
        );
        jsonFieldTransforms.add(
                new JsonFieldTransform(
                        "fastqLibrarySelection",
                        FastqFileManager.CONCEPT_ALIAS + ".librarySelection",
                        DataType.STRING
                )
        );
        jsonFieldTransforms.add(
                new JsonFieldTransform(
                        "fastqLibraryLayout",
                        FastqFileManager.CONCEPT_ALIAS + ".libraryLayout",
                        DataType.STRING
                )
        );
        jsonFieldTransforms.add(
                new JsonFieldTransform(
                        "fastqPlatform",
                        FastqFileManager.CONCEPT_ALIAS + ".platform",
                        DataType.STRING
                )
        );
        jsonFieldTransforms.add(
                new JsonFieldTransform(
                        "fastqInstrumentModel",
                        FastqFileManager.CONCEPT_ALIAS + ".instrumentModel",
                        DataType.STRING
                )
        );
        jsonFieldTransforms.add(
                new JsonFieldTransform(
                        "fastqDesignDescription",
                        FastqFileManager.CONCEPT_ALIAS + ".designDescription",
                        DataType.STRING
                )
        );
        jsonFieldTransforms.add(
                new JsonFieldTransform(
                        "bioSample Accession",
                        FastqFileManager.CONCEPT_ALIAS + ".bioSample.accession",
                        DataType.STRING
                )
        );
        jsonFieldTransforms.add(
                new JsonFieldTransform(
                        "bioProject Accession",
                        FastqFileManager.CONCEPT_ALIAS + ".bioSample.bioProjectAccession",
                        DataType.STRING
                )
        );
        jsonFieldTransforms.add(
                new JsonFieldTransform(
                        "Experiment Accession",
                        FastqFileManager.CONCEPT_ALIAS + ".bioSample.experiment.experimentAccession",
                        DataType.STRING
                )
        );
        jsonFieldTransforms.add(
                new JsonFieldTransform(
                        "Study Accession",
                        FastqFileManager.CONCEPT_ALIAS + ".bioSample.experiment.studyAccession",
                        DataType.STRING
                )
        );
        jsonFieldTransforms.add(
                // currently only getting the 1st Run Accession
                new JsonFieldTransform(
                        "Run Accessions",
                        FastqFileManager.CONCEPT_ALIAS + "/bioSample/experiment/runAccessions/0",
                        DataType.STRING
                )
        );

        return jsonFieldTransforms;
    }
}

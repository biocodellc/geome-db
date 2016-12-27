package biocode.fims.dipnet.query;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.DataType;
import biocode.fims.digester.Entity;
import biocode.fims.digester.Mapping;
import biocode.fims.elasticSearch.query.ElasticSearchFilterField;
import biocode.fims.fasta.FastaSequenceFields;
import biocode.fims.fileManagers.fasta.FastaFileManager;
import biocode.fims.query.JsonFieldTransform;
import com.fasterxml.jackson.core.JsonPointer;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for obtaining DIPNet specific Query information
 *
 * @author RJ Ewing
 */
public class DipnetQueryUtils {

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
                            JsonPointer.compile("/" + a.getUri()),
                            a.getDatatype()
                    )
            );
        }

        fieldTransforms.add(
                new JsonFieldTransform(
                        "bcid",
                        JsonPointer.compile("/bcid"),
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
                        JsonPointer.compile("/" + a.getUri()),
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
}

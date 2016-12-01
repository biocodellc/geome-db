package biocode.fims.dipnet.query;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.Entity;
import biocode.fims.digester.Mapping;
import biocode.fims.elasticSearch.query.ElasticSearchFilter;
import biocode.fims.fileManagers.fasta.FastaFileManager;
import biocode.fims.query.JsonFieldTransform;
import com.fasterxml.jackson.core.JsonPointer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author RJ Ewing
 */
public class DipnetQueryUtils {

    /**
     * get the list of filterable fields to query against
     *
     * @param mapping
     * @return
     */
    public static List<ElasticSearchFilter> getAvailableFilters(Mapping mapping) {
        List<ElasticSearchFilter> filters = new ArrayList<>();

        for (Attribute attribute : mapping.getDefaultSheetAttributes()) {
            filters.add(new ElasticSearchFilter(attribute.getUri(), attribute.getColumn()));
        }

        Entity fastaEntity = mapping.findEntity(FastaFileManager.ENTITY_CONCEPT_ALIAS);

        for (Attribute attribute : fastaEntity.getAttributes()) {
            filters.add(
                    new ElasticSearchFilter(
                            fastaEntity.getConceptAlias() + "." + attribute.getUri(),
                            fastaEntity.getConceptAlias() + "." + attribute.getColumn())
                            .nested(fastaEntity.isEsNestedObject())
                            .path(fastaEntity.getConceptAlias())
            );
        }

        return filters;
    }

    /**
     * ElasticSearchFilter that will search all fields.
     * @return
     */
    public static ElasticSearchFilter get_AllFilter() {
        return new ElasticSearchFilter("_all", null);
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

        return fieldTransforms;
    }

    /**
     * get a list of attributes as JsonFieldTransform objects to be used in transforming
     * the json fastqSequence fields into human readable fields.
     *
     * @param mapping
     * @return
     */
    public static List<JsonFieldTransform> getFastaJsonFieldTransforms(Mapping mapping) {
        List<JsonFieldTransform> fieldTransforms = new ArrayList<>();

        for (Attribute a : mapping.findEntity(FastaFileManager.ENTITY_CONCEPT_ALIAS).getAttributes()) {
            fieldTransforms.add(
                    new JsonFieldTransform(
                            a.getColumn(),
                            JsonPointer.compile("/" + a.getUri()),
                            a.getDatatype()
                    )
            );
        }

        return fieldTransforms;
    }
}

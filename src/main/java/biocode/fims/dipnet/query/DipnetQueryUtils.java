package biocode.fims.dipnet.query;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.Mapping;
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
     * get the attributes available to filter queries on for Dipnet.
     * @param mapping
     * @return
     */
    public static List<Attribute> getFilterAttributes(Mapping mapping) {
        List<Attribute> attributes = mapping.getDefaultSheetAttributes();

        attributes.addAll(mapping.findEntity(FastaFileManager.ENTITY_CONCEPT_ALIAS).getAttributes());

        return attributes;
    }

    /**
     * get a list of attributes as JsonFieldTransform objects to be used in transforming
     * the json resource fields into human readable fields.
     * @param mapping
     * @return
     */
    public static List<JsonFieldTransform> getJsonFieldTransforms(Mapping mapping) {
        List<JsonFieldTransform> fieldTransforms = new ArrayList<>();

        for (Attribute a: mapping.getDefaultSheetAttributes()) {
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

package biocode.fims.biscicol.query;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.DataType;
import biocode.fims.digester.Mapping;
import biocode.fims.elasticSearch.query.ElasticSearchFilterField;
import biocode.fims.query.writers.JsonFieldTransform;
import com.fasterxml.jackson.core.JsonPointer;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for obtaining biscicol specific Query information
 *
 * @author RJ Ewing
 */
public class BiscicolQueryUtils {

    /**
     * get the list of filterable fields to query against
     *
     * @param mapping
     * @return
     */
    public static List<ElasticSearchFilterField> getAvailableFilters(Mapping mapping) {
        List<ElasticSearchFilterField> filters = new ArrayList<>();

        for (Attribute attribute : mapping.getDefaultSheetAttributes()) {
            filters.add(new ElasticSearchFilterField(attribute.getUri(), attribute.getColumn(), attribute.getDatatype(), attribute.getGroup()));
        }

        return filters;
    }


    /**
     * ElasticSearchFilterField that will search all fields.
     *
     * @return
     */
    public static ElasticSearchFilterField get_AllFilter() {
        return new ElasticSearchFilterField("_all", null, DataType.STRING, "hidden");
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
                            a.getDatatype(),
                            false
                    )
            );
        }

        fieldTransforms.add(
                new JsonFieldTransform(
                        "bcid",
                        "bcid",
                        DataType.STRING,
                        false
                )
        );

        return fieldTransforms;
    }

    public static JsonPointer getLongitudePointer(Mapping mapping) {
        // TODO centralize this definedBy and point this method and ProjectController.getLatLongColumns to it
        String decimalLongDefinedBy = "http://rs.tdwg.org/dwc/terms/decimalLongitude";

        for (Attribute a: mapping.getDefaultSheetAttributes()) {
            if (decimalLongDefinedBy.equals(a.getDefined_by())) {
                return  JsonPointer.compile("/" + a.getUri());
            }
        }

        return JsonPointer.compile("/urn:decimalLongitude");
    }

    public static JsonPointer getLatitudePointer(Mapping mapping) {
        // TODO centralize this definedBy and point this method and ProjectController.getLatLongColumns to it
        String decimalLatDefinedBy = "http://rs.tdwg.org/dwc/terms/decimalLatitude";

        for (Attribute a: mapping.getDefaultSheetAttributes()) {
            if (decimalLatDefinedBy.equals(a.getDefined_by())) {
                return  JsonPointer.compile("/" + a.getUri());
            }
        }
        return JsonPointer.compile("/urn:decimalLatitude");
    }

    public static JsonPointer getUniqueKeyPointer(Mapping mapping) {
        Attribute a = mapping.lookupAttribute(
                mapping.getDefaultSheetUniqueKey(),
                mapping.getDefaultSheetName()
        );

        return JsonPointer.compile("/" + a.getUri());
    }
}

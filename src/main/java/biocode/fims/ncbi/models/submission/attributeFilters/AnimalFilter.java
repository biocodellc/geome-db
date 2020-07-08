package biocode.fims.ncbi.models.submission.attributeFilters;

import java.util.*;

/**
 * @author rjewing
 */
public class AnimalFilter {
    // https://submit.ncbi.nlm.nih.gov/biosample/template/?package=Model.organism.animal.1.0&action=definition
    private static final Map<String, Boolean> required = new HashMap<String, Boolean>() {{
        put("sex", true);
        put("tissue", true);
    }};

//    private static final List<String> required = new ArrayList<String>() {{
//        add("sex");
//        add("tissue");
//    }};

    private static final List<AttributeGroup> requiredGroups = new ArrayList<AttributeGroup>() {{
        add(new AttributeGroup(Arrays.asList("strain", "isolate", "breed", "cultivar", "ecotype")));
        add(new AttributeGroup(Arrays.asList("age", "dev_stage")));
    }};

    public static Map<String, String> updateAttributes(Map<String, String> attributes) {
        Map<String, String> updated = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
//            if (required.getOrDefault(entry.getKey(), false))

        }

        return updated;
    }
}

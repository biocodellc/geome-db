package biocode.fims.ncbi.models.submission.attributeFilters;

import biocode.fims.ncbi.models.submission.Attribute;

import java.util.List;

/**
 * @author rjewing
 */
public class AttributeGroup {
    private List<String> attributes;

    public AttributeGroup(List<String> attributes) {
        this.attributes = attributes;
    }
}

package biocode.fims.ncbi.models.submission;

import biocode.fims.serializers.AttributeSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * @author rjewing
 */
@JsonSerialize(using = AttributeSerializer.class)
public class BioSampleAttribute extends Attribute {
    @XmlAttribute(name = "attribute_name")
    public String name;

    // needed for serialization
    private BioSampleAttribute() {
        super();
    }

    public BioSampleAttribute(String name, String value) {
        super(null, value);
        this.name = name;
    }
}

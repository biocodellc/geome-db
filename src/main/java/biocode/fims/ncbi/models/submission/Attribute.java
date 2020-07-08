package biocode.fims.ncbi.models.submission;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

/**
 * @author rjewing
 */
public class Attribute {
    @XmlAttribute
    public String name;
    @XmlValue
    public String value;

    Attribute() {};

    public Attribute(String name, String value) {
        this.name = name;
        this.value = value;
    }
}

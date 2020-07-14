package biocode.fims.ncbi.models.submission;

import biocode.fims.models.User;
import org.eclipse.persistence.oxm.annotations.XmlPath;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author rjewing
 */
@XmlRootElement
public class Organization {
    @XmlAttribute
    private static String role = "owner";
    @XmlAttribute
    private static String type = "consortium";
    @XmlElement
    private static String Name = "GEOME";
    @XmlAttribute
    private String url;

    private User user;

    Organization() {
    }

    public Organization(User user, String url) {
        this.user = user;
        this.url = url;
    }

    @XmlElement
    @XmlPath("Contact/Name/First/text()")
    public String getFirst() {
        return user.getSraFirstName();
    }

    @XmlElement
    @XmlPath("Contact/Name/Last/text()")
    public String getLast() {
        return user.getSraLastName();
    }

    @XmlPath("Contact/@email")
    public String getEmail() {
        return user.getSraEmail();
    }

}

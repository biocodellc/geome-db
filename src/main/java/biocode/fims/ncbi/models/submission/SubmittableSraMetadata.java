package biocode.fims.ncbi.models.submission;

import biocode.fims.ncbi.models.SPUIDNamespace;
import biocode.fims.ncbi.models.SraMetadata;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.persistence.oxm.annotations.XmlPath;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class maps SraMetadata values to properties so we can control the output ordering of the xml
 *
 * @author rjewing
 */
@XmlType(propOrder = {"files", "attributes", "refs", "identifier"})
public class SubmittableSraMetadata {
    private static ArrayList<String> IGNOREABLE_ATTRIBUTES = new ArrayList<String>() {{
        add("sample_name");
        add("library_ID");
        add("filename");
        add("filename2");
    }};

    @XmlPath("AddFiles/@target_db")
    private static String targetDb = "SRA";
    @XmlPath("AddFiles/Identifier/SPUID/@spuid_namespace")
    private static String spuidNamespace = SPUIDNamespace.value;

    @XmlPath("AddFiles/Attribute")
    private List<Attribute> attributes;

    @XmlElements({
            @XmlElement(name = "AddFiles/File", type = File.class),
    })
    private List<File> files;

    @XmlElements({
            @XmlElement(name = "AddFiles/AttributeRefId", type = AttributeRef.class),
    })
    private List<AttributeRef> refs;

    @XmlPath("AddFiles/Identifier/SPUID/text()")
    private String identifier;

    SubmittableSraMetadata() {
    }

    private SubmittableSraMetadata(String identifier, List<File> files, List<AttributeRef> refs, List<Attribute> attributes) {
        this.identifier = identifier;
        this.files = files;
        this.refs = refs;
        this.attributes = attributes;
    }

    public static SubmittableSraMetadata fromMetadata(SraMetadata m, String bioProjectAccession, String bioProjectId) {

        List<AttributeRef> refs = new ArrayList<>();
        refs.add(new AttributeRef(bioProjectId, bioProjectAccession, "BioProject"));
        refs.add(new AttributeRef(m.get("sample_name"), null, "BioSample"));

        List<Attribute> attributes = m.entrySet().stream()
                .filter(e -> !IGNOREABLE_ATTRIBUTES.contains(e.getKey()))
                .map(e -> new Attribute(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        List<File> files = new ArrayList<>();
        files.add(new File(m.get("filename")));
        if (!StringUtils.isBlank(m.getOrDefault("filename2", ""))) files.add(new File(m.get("filename2")));

        return new SubmittableSraMetadata(m.get("library_ID"), files, refs, attributes);
    }

    private static class AttributeRef {
        @XmlPath("@name")
        private String name;
        @XmlPath("RefId/PrimaryId/@db")
        private String db;
        @XmlPath("RefId/SPUID/text()")
        private String spuid;
        @XmlPath("RefId/PrimaryId/text()")
        private String id;

        AttributeRef() {
        }

        private AttributeRef(String spuid, String id, String name) {
            this.id = id;
            this.name = name;
            if (id != null) {
                this.db = name;
            } else {
                this.spuid = spuid;
            }
        }

        @XmlPath("RefId/SPUID/@spuid_namespace")
        public String getSpuidNamespace() {
            if (StringUtils.isBlank(spuid)) return null;
            return SPUIDNamespace.value;
        }
    }

    private static class File {
        @XmlElement(name = "DataType")
        private static String dataType = "generic-data";
        @XmlAttribute(name = "file_path")
        private String path;

        File() {
        }

        private File(String path) {
            this.path = path;
        }
    }
}

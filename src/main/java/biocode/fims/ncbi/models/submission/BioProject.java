package biocode.fims.ncbi.models.submission;

import biocode.fims.ncbi.models.SPUIDNamespace;
import biocode.fims.rest.models.SraUploadMetadata;
import org.eclipse.persistence.oxm.annotations.XmlPath;

/**
 * @author rjewing
 */
public class BioProject {
    @XmlPath("AddData/@target_db")
    private static String targetDb = "BioProject";
    @XmlPath("AddData/Data/@content_type")
    private static String contentType = "xml";
    @XmlPath("AddData/Data/XmlContent/Project/@schema_version")
    private static String schemaVersion = "2.0";


    @XmlPath("AddData/Data/XmlContent/Project/ProjectID/SPUID/@spuid_namespace")
    private static String spuidNamespace = SPUIDNamespace.value;
    @XmlPath("AddData/Data/XmlContent/Project/ProjectID/SPUID/text()")
    private String expeditionCode;
    @XmlPath("AddData/Data/XmlContent/Project/Descriptor/Title/text()")
    private String title;
    @XmlPath("AddData/Data/XmlContent/Project/Descriptor/Description/p/text()")
    private String description;
    // TODO do these need to be dynamically set?
    @XmlPath("AddData/Data/XmlContent/Project/ProjectType/ProjectTypeSubmission/@sample_scope")
    private String sampleScope = "eMultispecies";
    @XmlPath("AddData/Data/XmlContent/Project/ProjectType/ProjectTypeSubmission/IntendedDataTypeSet/DataType/text()")
    private String dataType = "genome sequencing";

    BioProject() {}

    public BioProject(SraUploadMetadata metadata) {
        this.expeditionCode = metadata.expeditionCode;
        this.description = metadata.bioProjectDescription;
        this.title = metadata.bioProjectTitle;
    }

    @XmlPath("AddData/Identifier/SPUID/@spuid_namespace")
    private static String spuidNamespace2 = SPUIDNamespace.value;
    @XmlPath("AddData/Identifier/SPUID/text()")
    public String getIdentifier() {
        return this.expeditionCode;
    }

}

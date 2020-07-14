package biocode.fims.ncbi.models;

import biocode.fims.ncbi.models.submission.BioSampleAttribute;
import biocode.fims.ncbi.models.submission.BioSampleTypeAdaptor;
import biocode.fims.rest.models.SraUploadMetadata;
import org.eclipse.persistence.oxm.annotations.XmlPath;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author rjewing
 */
@XmlType(propOrder = {"sampleName", "sampleTitle", "organism", "bioProjectAccession", "bioProjectId", "type", "attributes", "identifier", "bioSampleAccession", "identifierIfAccession"})
public class SubmittableBioSample {
    private static ArrayList<String> IGNOREABLE_ATTRIBUTES = new ArrayList<String>() {{
        add("sample_name");
        add("sample_title");
        add("organism");
    }};

    @XmlPath("AddData/@target_db")
    private static String targetDb = "BioSample";
    @XmlPath("AddData/Data/@content_type")
    private static String contentType = "xml";
    @XmlPath("AddData/Data/XmlContent/BioSample/@schema_version")
    private static String schemaVersion = "2.0";

    // TODO implement this
    // The challenge is that we store the BioSample Accession number on the FastqMetadata objectt
    // really, this information should be stored on the BioSample and only store the Sra accessions
    // on the FastqMetadata. A quick hack would be to make another query to pull the bioSample.accession
    // for any fastqMetadata children of a given BioSample
    @XmlPath("AddData/Data/XmlContent/BioSample/SampleId/PrimaryId/text()")
    private String bioSampleAccession;
    @XmlPath("AddData/Data/XmlContent/BioSample/SampleId/SPUID/text()")
    private String sampleName;

    @XmlPath("AddData/Data/XmlContent/BioSample/Descriptor/Title/text()")
    private String sampleTitle;

    @XmlPath("AddData/Data/XmlContent/BioSample/Organism/OrganismName/text()")
    private String organism;

    @XmlPath("AddData/Data/XmlContent/BioSample/BioProject/PrimaryId/text()")
    private String bioProjectAccession;
    @XmlPath("AddData/Data/XmlContent/BioSample/BioProject/SPUID/text()")
    private String bioProjectId;
    @XmlPath("AddData/Data/XmlContent/BioSample/Package/text()")
    @XmlJavaTypeAdapter(BioSampleTypeAdaptor.class)
    private SraUploadMetadata.BioSampleType type;

    @XmlPath("AddData/Data/XmlContent/BioSample/Attributes/Attribute")
    private List<BioSampleAttribute> attributes;

    private SubmittableBioSample() {
    }

    private SubmittableBioSample(String sampleName, String sampleTitle, String organism,
                                 List<BioSampleAttribute> attributes, SraUploadMetadata.BioSampleType bioSampleType,
                                 String bioProjectAccession, String bioProjectId) {
        this.sampleName = sampleName;
        this.sampleTitle = sampleTitle;
        this.organism = organism;
        this.attributes = attributes;
        type = bioSampleType;
        this.bioProjectAccession = bioProjectAccession;
        this.bioProjectId = bioProjectId;
    }


    @XmlPath("AddData/Data/XmlContent/BioSample/BioProject/PrimaryId/@db")
    public String getBioProjectDb() {
        return this.bioProjectAccession == null ? null : "BioProject";
    }

    @XmlPath("AddData/Data/XmlContent/BioSample/BioProject/SPUID/@spuid_namespace")
    private String getSpuidNamespace2() {
        return this.bioProjectId == null ? null : SPUIDNamespace.value;
    }

    @XmlPath("AddData/Data/XmlContent/BioSample/SampleId/SPUID/@spuid_namespace")
    private String getSpuidNamespace() {
        return this.sampleName == null ? null : SPUIDNamespace.value;
    }

    @XmlPath("AddData/Identifier/SPUID/@spuid_namespace")
    private String getSpuidNamespace3() {
        return this.sampleName == null ? null : SPUIDNamespace.value;
    }

    @XmlPath("AddData/Identifier/SPUID/text()")
    public String getIdentifier() {
        return sampleName;
    }

    @XmlPath("AddData/Identifier/PrimaryId/@db")
    public String getIdentifierDbIfAccession() {
        return bioSampleAccession == null ? null : targetDb;
    }

    @XmlPath("AddData/Identifier/PrimaryId/text()")
    public String getIdentifierIfAccession() {
        return bioSampleAccession;
    }

    public static SubmittableBioSample fromBioSample(GeomeBioSample bioSample, String bioProjectAccession, String bioProjectId, SraUploadMetadata.BioSampleType bioSampleType) {
        List<BioSampleAttribute> attributes = bioSample.entrySet().stream()
                .filter(e -> !IGNOREABLE_ATTRIBUTES.contains(e.getKey()))
                .map(e -> new BioSampleAttribute(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        return new SubmittableBioSample(
                bioSample.get("sample_name"),
                bioSample.get("sample_title"),
                bioSample.get("organism"),
                attributes,
                bioSampleType,
                bioProjectAccession,
                bioProjectId
        );
    }
}


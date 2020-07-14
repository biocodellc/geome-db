package biocode.fims.ncbi.models;

import biocode.fims.models.dataTypes.JacksonUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.persistence.oxm.annotations.XmlPath;

import javax.xml.bind.annotation.*;

/**
 * Domain object representing the information we are interested in from the NCBI Entrez eFetch BioSample
 *
 * @author rjewing
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class BioSample {

    @XmlAttribute
    private String id;

    @XmlAttribute
    private String accession;

    @JsonIgnore
    @XmlPath("Attributes/Attribute[@attribute_name='bcid']/text()")
    private String bcid;

    @XmlPath("Links/Link[@target='bioproject']/text()")
    private String bioProjectId;

    @XmlPath("Links/Link[@target='bioproject']/@label")
    private String bioProjectAccession;
    @JsonProperty("experiment")
    private SraExperimentPackage sraExperimentPackage;

    private BioSample() {
    }

    public BioSample(String id, String accession, String bcid, String bioProjectId, String bioProjectAccession) {
        this.id = id;
        this.accession = accession;
        this.bcid = bcid;
        this.bioProjectId = bioProjectId;
        this.bioProjectAccession = bioProjectAccession;
    }

    public String getId() {
        return id;
    }

    public String getAccession() {
        return accession;
    }

    public String getBcid() {
        return bcid;
    }

    public String getBioProjectId() {
        return bioProjectId;
    }

    public String getBioProjectAccession() {
        return bioProjectAccession;
    }

    public SraExperimentPackage getSraExperimentPackage() {
        return sraExperimentPackage;
    }

    public void setSraExperimentPackage(SraExperimentPackage sraExperimentPackage) {
        this.sraExperimentPackage = sraExperimentPackage;
    }

    @Override
    public String toString() {
        return JacksonUtil.toString(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BioSample)) return false;

        BioSample bioSample = (BioSample) o;

        if (getId() != null ? !getId().equals(bioSample.getId()) : bioSample.getId() != null) return false;
        if (getAccession() != null ? !getAccession().equals(bioSample.getAccession()) : bioSample.getAccession() != null)
            return false;
        if (getBcid() != null ? !getBcid().equals(bioSample.getBcid()) : bioSample.getBcid() != null) return false;
        if (getBioProjectId() != null ? !getBioProjectId().equals(bioSample.getBioProjectId()) : bioSample.getBioProjectId() != null)
            return false;
        if (getBioProjectAccession() != null ? !getBioProjectAccession().equals(bioSample.getBioProjectAccession()) : bioSample.getBioProjectAccession() != null)
            return false;
        return getSraExperimentPackage() != null ? getSraExperimentPackage().equals(bioSample.getSraExperimentPackage()) : bioSample.getSraExperimentPackage() == null;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getAccession() != null ? getAccession().hashCode() : 0);
        result = 31 * result + (getBcid() != null ? getBcid().hashCode() : 0);
        result = 31 * result + (getBioProjectId() != null ? getBioProjectId().hashCode() : 0);
        result = 31 * result + (getBioProjectAccession() != null ? getBioProjectAccession().hashCode() : 0);
        result = 31 * result + (getSraExperimentPackage() != null ? getSraExperimentPackage().hashCode() : 0);
        return result;
    }
}

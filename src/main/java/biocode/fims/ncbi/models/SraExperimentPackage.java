package biocode.fims.ncbi.models;

import org.eclipse.persistence.oxm.annotations.XmlPath;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.List;

/**
 * Domain object representing the information we are interested in from the NCBI Entrez eFetch Sra
 *
 * @author rjewing
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class SraExperimentPackage {

    @XmlPath("STUDY/@accession")
    private String studyAccession;

    @XmlPath("EXPERIMENT/@accession")
    private String experimentAccession;

    @XmlPath("SAMPLE/IDENTIFIERS/EXTERNAL_ID[@namespace='BioSample']/text()")
    private String bioSampleAccession;

    @XmlPath("RUN_SET/RUN/@accession")
    private List<String> runAccessions;

    private SraExperimentPackage() {}

    public SraExperimentPackage(String studyAccession, String experimentAccession, String bioSampleAccession, List<String> runAccessions) {
        this.studyAccession = studyAccession;
        this.experimentAccession = experimentAccession;
        this.bioSampleAccession = bioSampleAccession;
        this.runAccessions = runAccessions;
    }

    public String getStudyAccession() {
        return studyAccession;
    }

    public String getExperimentAccession() {
        return experimentAccession;
    }

    public boolean hasBioSampleAccession(String bioSampleAccession) {
        return this.bioSampleAccession.equals(bioSampleAccession);
    }

    public List<String> getRunAccessions() {
        return runAccessions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SraExperimentPackage)) return false;

        SraExperimentPackage that = (SraExperimentPackage) o;

        if (getStudyAccession() != null ? !getStudyAccession().equals(that.getStudyAccession()) : that.getStudyAccession() != null)
            return false;
        if (getExperimentAccession() != null ? !getExperimentAccession().equals(that.getExperimentAccession()) : that.getExperimentAccession() != null)
            return false;
        if (bioSampleAccession != null ? !bioSampleAccession.equals(that.bioSampleAccession) : that.bioSampleAccession != null)
            return false;
        return getRunAccessions() != null ? getRunAccessions().equals(that.getRunAccessions()) : that.getRunAccessions() == null;
    }

    @Override
    public int hashCode() {
        int result = getStudyAccession() != null ? getStudyAccession().hashCode() : 0;
        result = 31 * result + (getExperimentAccession() != null ? getExperimentAccession().hashCode() : 0);
        result = 31 * result + (bioSampleAccession != null ? bioSampleAccession.hashCode() : 0);
        result = 31 * result + (getRunAccessions() != null ? getRunAccessions().hashCode() : 0);
        return result;
    }
}

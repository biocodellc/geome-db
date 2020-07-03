package biocode.fims.ncbi.models.submission;

import biocode.fims.models.User;
import biocode.fims.models.dataTypes.converters.DateAdaptor;
import biocode.fims.ncbi.models.SraSubmissionData;
import biocode.fims.ncbi.models.SubmittableBioSample;
import biocode.fims.rest.models.SraUploadMetadata;
import org.eclipse.persistence.oxm.annotations.XmlPath;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain object representing the information necessary to submit to sra for a new submission
 *
 * @author rjewing
 */
@XmlRootElement(name = "Submission")
public class SraSubmission {

    @XmlPath("Description/Submitter/@user_name")
    private String submitter;
    @XmlPath("Description/Organization")
    public Organization organization;
    @XmlPath("Description/Hold/@release_date")
    @XmlJavaTypeAdapter(DateAdaptor.class)
    private LocalDate releaseDate;
    @XmlElements({
            @XmlElement(name = "Action", type = BioProject.class),
            @XmlElement(name = "Action", type = SubmittableBioSample.class),
            @XmlElement(name = "Action", type = SubmittableSraMetadata.class),
    })
    private List<Object> actions;

    // necessary for jackson/moxy
    public SraSubmission() {
    }

    public SraSubmission(SraSubmissionData data, SraUploadMetadata metadata, User user, String url) {
        organization = new Organization(user, url);
        this.releaseDate = metadata.releaseDate;
        this.submitter = user.getSraUsername();
        this.actions = new ArrayList<>();

        BioProject bioProject = null;
        if (metadata.bioProjectAccession == null) {
            bioProject = new BioProject(metadata);
            this.actions.add(bioProject);
        }

        BioProject finalBioProject = bioProject;
        data.bioSamples.forEach(b -> {
            String bioProjectId = finalBioProject == null ? null : finalBioProject.getIdentifier();
            this.actions.add(
                    SubmittableBioSample.fromBioSample(b, metadata.bioProjectAccession, bioProjectId, metadata.bioSampleType)
            );
        });

        data.sraMetadata.forEach(m -> {
            String bioProjectId = finalBioProject == null ? null : finalBioProject.getIdentifier();
            this.actions.add(
                    SubmittableSraMetadata.fromMetadata(
                            m, metadata.bioProjectAccession, bioProjectId
                    )
            );
        });
    }
}

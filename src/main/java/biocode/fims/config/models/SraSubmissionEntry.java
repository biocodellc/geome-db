package biocode.fims.models;

import biocode.fims.models.dataTypes.converters.PathPersistenceConverter;
import biocode.fims.serializers.Views;
import com.fasterxml.jackson.annotation.JsonView;

import javax.persistence.*;
import java.nio.file.Path;
import java.util.Date;

/**
 * @author rjewing
 */
@Entity
@Table(name = "sra_submissions")
public class SraSubmissionEntry {

    private int id;
    private User user;
    private Project project;
    private Expedition expedition;
    private Path submissionDir;
    private String message;
    private Status status = Status.READY;
    private Date created;
    private Date modified;


    // needed for hibernate
    private SraSubmissionEntry() {}

    public SraSubmissionEntry(Project project, Expedition expedition, User user, Path submissionDir) {
        this.user = user;
        this.project = project;
        this.expedition = expedition;
        this.submissionDir = submissionDir;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getId() {
        return id;
    }

    private void setId(int id) {
        this.id = id;
    }

    @ManyToOne
    @JoinColumn(name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_sra_submission_user_id"),
            referencedColumnName = "id"
    )
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @ManyToOne
    @JoinColumn(name = "project_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_sra_submission_project_id"),
            referencedColumnName = "id"
    )
    public Project getProject() {
        return project;
    }

    private void setProject(Project project) {
        this.project = project;
    }

    @ManyToOne
    @JoinColumn(name = "expedition_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_sra_submission_expedition_id"),
            referencedColumnName = "id"
    )
    public Expedition getExpedition() {
        return expedition;
    }

    private void setExpedition(Expedition expedition) {
        this.expedition = expedition;
    }

    @Column(name = "submission_dir", nullable = false)
    @Convert(converter = PathPersistenceConverter.class)
    public Path getSubmissionDir() {
        return submissionDir;
    }

    public void setSubmissionDir(Path submissionDir) {
        this.submissionDir = submissionDir;
    }

    @Column(name = "message")
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Column(updatable = false)
    @JsonView(Views.Detailed.class)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getCreated() {
        return created;
    }

    private void setCreated(Date created) {
        this.created = created;
    }

    @JsonView(Views.Detailed.class)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getModified() {
        return modified;
    }

    private void setModified(Date modified) {
        this.modified = modified;
    }

    public enum Status {
        READY, SUBMITTED, COMPLETED, FAILED, SUBMISSION_ERROR
    }

}

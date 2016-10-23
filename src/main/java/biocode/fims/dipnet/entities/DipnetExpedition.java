package biocode.fims.dipnet.entities;

import biocode.fims.entities.Expedition;
import org.springframework.util.Assert;

import javax.persistence.*;

/**
 * DipnetExpedition domain object. A DipnetExpedition has a one-to-one unidirectional relationship to
 * a biocode-fims {@link Expedition}.
 */
//@JsonDeserialize(builder = DipnetExpedition.DipnetExpeditionBuilder.class)
@Entity
@Table(name = "dipnetExpeditions")
public class DipnetExpedition {

    private int expeditionId;
    private FastqMetadata fastqMetadata;

    private Expedition expedition;

    //    @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
    public static class DipnetExpeditionBuilder {
        private Expedition expedition;
        private FastqMetadata fastqMetadata;

        public DipnetExpeditionBuilder(Expedition expedition) {
            this.expedition = expedition;
        }

        public DipnetExpeditionBuilder fastqMetadata(FastqMetadata val) {
            this.fastqMetadata = val;
            return this;
        }

        public DipnetExpedition build() {
            Assert.notNull(expedition);
            return new DipnetExpedition(this);
        }
    }

    // needed for hibernate
    DipnetExpedition() {
    }

    private DipnetExpedition(DipnetExpeditionBuilder builder) {
        this.expedition = builder.expedition;
        setFastqMetadata(builder.fastqMetadata);
    }

    @Id
    @Column(nullable = false)
    public int getExpeditionId() {
        return expeditionId;
    }

    public void setExpeditionId(int id) {
        if (expeditionId == 0)
            this.expeditionId = id;
    }

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "dipnetExpedition")
    public FastqMetadata getFastqMetadata() {
        return fastqMetadata;
    }

    public void setFastqMetadata(FastqMetadata fastqMetadata) {
        this.fastqMetadata = fastqMetadata;
        if (fastqMetadata != null) {
            this.fastqMetadata.setDipnetExpedition(this);
        }
    }

    @Transient
    public Expedition getExpedition() {
        return expedition;
    }

    public void setExpedition(Expedition expedition) {
        if (this.expedition == null)
            this.expedition = expedition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DipnetExpedition)) return false;

        DipnetExpedition that = (DipnetExpedition) o;

        if (getExpeditionId() != that.getExpeditionId()) return false;
        if (getFastqMetadata() != null ? !getFastqMetadata().equals(that.getFastqMetadata()) : that.getFastqMetadata() != null)
            return false;
        return getExpedition().equals(that.getExpedition());

    }

    @Override
    public int hashCode() {
        int result = getExpeditionId();
        result = 31 * result + (getFastqMetadata() != null ? getFastqMetadata().hashCode() : 0);
        result = 31 * result + getExpedition().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DipnetExpedition{" +
                "expeditionId=" + expeditionId +
                ", fastqMetadata=" + fastqMetadata +
                ", expedition=" + expedition +
                '}';
    }
}

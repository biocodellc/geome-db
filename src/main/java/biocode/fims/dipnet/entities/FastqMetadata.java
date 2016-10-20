package biocode.fims.dipnet.entities;

import biocode.fims.converters.JSONArrayPersistenceConverter;
import biocode.fims.fimsExceptions.FimsRuntimeException;

import javax.persistence.*;
import java.util.List;

/**
 * FastqMetadata domain object.
 */
@Entity
@Table(name = "fastqMetadata")
public class FastqMetadata {

    private int expeditionId;
    private String libraryStrategy;
    private String librarySource;
    private String librarySelection;
    private String libraryLayout;
    private String platform;
    private String instrumentModel;
    private String designDescription;
    private List<String> filenames;

    private DipnetExpedition dipnetExpedition;

    public static class FASTQMetadataBuilder {
//        private Expedition expedition;
        private String libraryStrategy;
        private String librarySource;
        private String librarySelection;
        private String libraryLayout;
        private String platform;
        private String instrumentModel;
        private String designDescription;
        private List<String> filenames;

        public FASTQMetadataBuilder() {
        }
        
//        public FASTQMetadataBuilder(Expedition expedition) {
//            this.expedition = expedition;
//        }

        public FASTQMetadataBuilder libraryStrategy(String val) {
            this.libraryStrategy = val;
            return this;
        }

        public FASTQMetadataBuilder librarySource(String val) {
            this.librarySource = val;
            return this;
        }

        public FASTQMetadataBuilder librarySelection(String val) {
            this.librarySelection = val;
            return this;
        }

        public FASTQMetadataBuilder libraryLayout(String val) {
            this.libraryLayout = val;
            return this;
        }

        public FASTQMetadataBuilder platform(String val) {
            this.platform = val;
            return this;
        }

        public FASTQMetadataBuilder instrumentModel(String val) {
            this.instrumentModel = val;
            return this;
        }

        public FASTQMetadataBuilder designDescription(String val) {
            this.designDescription = val;
            return this;
        }

        public FASTQMetadataBuilder filenames(List<String> filenames) {
            this.filenames = filenames;
            return this;
        }

        private boolean validFASTQMetadat() {
            return (libraryStrategy != null && librarySource != null && librarySelection != null
                    && libraryLayout != null && platform != null && instrumentModel != null && designDescription != null);
        }

        public FastqMetadata build() {
            if (!validFASTQMetadat())
                throw new FimsRuntimeException("", "Trying to create an invalid expedition. " +
                        "libraryStrategy, librarySource, librarySelection, libraryLayout, platform, instrumentModel, " +
                        "and designDescription must not be null", 500);

            return new FastqMetadata(this);
        }
    }

    private FastqMetadata(FASTQMetadataBuilder builder) {
        this.libraryStrategy = builder.libraryStrategy;
        this.librarySource = builder.librarySource;
        this.librarySelection = builder.librarySelection;
        this.libraryLayout = builder.libraryLayout;
        this.platform = builder.platform;
        this.instrumentModel = builder.instrumentModel;
        this.designDescription = builder.designDescription;
        this.filenames = builder.filenames;
    }

    @Id
    @Column(unique = true, nullable = false)
    public int getExpeditionId() {
        return expeditionId;
    }

    private void setExpeditionId(int expeditionId) {
        this.expeditionId = expeditionId;
    }

    @Column(nullable = false)
    public String getLibraryStrategy() {
        return libraryStrategy;
    }

    public void setLibraryStrategy(String libraryStrategy) {
        this.libraryStrategy = libraryStrategy;
    }

    @Column(nullable = false)
    public String getLibrarySource() {
        return librarySource;
    }

    public void setLibrarySource(String librarySource) {
        this.librarySource = librarySource;
    }

    @Column(nullable = false)
    public String getLibrarySelection() {
        return librarySelection;
    }

    public void setLibrarySelection(String librarySelection) {
        this.librarySelection = librarySelection;
    }

    @Column(nullable = false)
    public String getLibraryLayout() {
        return libraryLayout;
    }

    public void setLibraryLayout(String libraryLayout) {
        this.libraryLayout = libraryLayout;
    }

    @Column(nullable = false)
    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    @Column(nullable = false)
    public String getInstrumentModel() {
        return instrumentModel;
    }

    public void setInstrumentModel(String instrumentModel) {
        this.instrumentModel = instrumentModel;
    }

    @Column(nullable = false)
    public String getDesignDescription() {
        return designDescription;
    }

    public void setDesignDescription(String designDescription) {
        this.designDescription = designDescription;
    }

    @Convert(converter = JSONArrayPersistenceConverter.class)
    @Column(columnDefinition = "json NOT NULL")
    public List<String> getFilenames() {
        return filenames;
    }

    public void setFilenames(List<String> filenames) {
        this.filenames = filenames;
    }

    @MapsId
    @OneToOne
    @JoinColumn(name = "expeditionId")
    private DipnetExpedition getDipnetExpedition() {
        return dipnetExpedition;
    }

    private void setDipnetExpedition(DipnetExpedition dipnetExpedition) {
        this.dipnetExpedition = dipnetExpedition;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FastqMetadata)) return false;

        FastqMetadata that = (FastqMetadata) o;

        if (getExpeditionId() != that.getExpeditionId()) return false;
        if (!getLibraryStrategy().equals(that.getLibraryStrategy())) return false;
        if (!getLibrarySource().equals(that.getLibrarySource())) return false;
        if (!getLibrarySelection().equals(that.getLibrarySelection())) return false;
        if (!getLibraryLayout().equals(that.getLibraryLayout())) return false;
        if (!getPlatform().equals(that.getPlatform())) return false;
        if (!getInstrumentModel().equals(that.getInstrumentModel())) return false;
        if (!getDesignDescription().equals(that.getDesignDescription())) return false;
        if (!getFilenames().equals(that.getFilenames())) return false;
        return getDipnetExpedition() != null ? getDipnetExpedition().equals(that.getDipnetExpedition()) : that.getDipnetExpedition() == null;

    }

    @Override
    public int hashCode() {
        int result = getExpeditionId();
        result = 31 * result + getLibraryStrategy().hashCode();
        result = 31 * result + getLibrarySource().hashCode();
        result = 31 * result + getLibrarySelection().hashCode();
        result = 31 * result + getLibraryLayout().hashCode();
        result = 31 * result + getPlatform().hashCode();
        result = 31 * result + getInstrumentModel().hashCode();
        result = 31 * result + getDesignDescription().hashCode();
        result = 31 * result + getFilenames().hashCode();
        result = 31 * result + (getDipnetExpedition() != null ? getDipnetExpedition().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FastqMetadata{" +
                "id=" + expeditionId +
                ", libraryStrategy='" + libraryStrategy + '\'' +
                ", librarySource='" + librarySource + '\'' +
                ", librarySelection='" + librarySelection + '\'' +
                ", libraryLayout='" + libraryLayout + '\'' +
                ", platform='" + platform + '\'' +
                ", instrumentModel='" + instrumentModel + '\'' +
                ", designDescription='" + designDescription + '\'' +
                ", dipnetExpedition=" + dipnetExpedition +
                '}';
    }
}

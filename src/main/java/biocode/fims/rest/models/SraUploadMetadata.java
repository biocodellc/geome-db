package biocode.fims.rest.models;

import biocode.fims.models.Expedition;
import biocode.fims.models.Project;
import biocode.fims.models.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;

import java.time.LocalDate;
import java.util.List;

/**
 * @author rjewing
 */
public class SraUploadMetadata {

    public enum BioSampleType {
        // https://www.ncbi.nlm.nih.gov/biosample/docs/packages/
        ANIMAL("Model.organism.animal.1.0"),
        INVERTEBRATE("Invertebrate.1.0"),
        PLANT("Plant.1.0"),
        ENVIRONMENTAL("Metagenome.environmental.1.0"),
        //        GENOME,
        VIRUS("Virus.1.0"),
        //        BETALACTAMASE("Beta-lactamase.1.0"),
//        PATHOGEN("Pathogen.cl.1.0"),
        MICROBE("Microbe.1.0"),
        HUMAN("Human.1.0");

        private final String name;

        BioSampleType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

//    public enum BioProjectType {
//        GENOME("genome sequencing"),
//        ;
//
//        private final String name;
//
//        BioProjectType(String name) {
//            this.name = name;
//        }
//
//        @Override
//        public String toString() {
//            return name;
//        }
//    }

    @JsonSetter(nulls = Nulls.FAIL)
    public String expeditionCode;
    @JsonSetter(nulls = Nulls.FAIL)
    public Integer projectId;
    @JsonIgnore
    public Project project;
    @JsonIgnore
    public Expedition expedition;

    public String bioProjectAccession;
    public String bioProjectTitle;
    public String bioProjectDescription;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    public LocalDate releaseDate;

    public String sraUsername;
    public String sraEmail;
    public String sraFirstName;
    public String sraLastName;

    @JsonSetter(nulls = Nulls.FAIL)
    public BioSampleType bioSampleType;
    @JsonSetter(nulls = Nulls.FAIL, contentNulls = Nulls.FAIL)
    public List<String> bioSamples;

    public boolean setUserSraProfile(User user) {
        boolean updated = false;

        if (sraUsername != null && !sraUsername.equals(user.getSraUsername())) {
            user.setSraUsername(sraUsername);
            updated = true;
        }
        if (sraEmail != null && !sraEmail.equals(user.getSraEmail())) {
            user.setSraEmail(sraEmail);
            updated = true;
        }
        if (sraFirstName != null && !sraFirstName.equals(user.getSraFirstName())) {
            user.setSraFirstName(sraFirstName);
            updated = true;
        }
        if (sraLastName != null && !sraLastName.equals(user.getSraLastName())) {
            user.setSraLastName(sraLastName);
            updated = true;
        }

        return updated;
    }
}

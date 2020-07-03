package biocode.fims.config.models;

import biocode.fims.config.Config;
import biocode.fims.fasta.FastaProps;
import biocode.fims.fasta.FastaRecord;
import biocode.fims.models.dataTypes.JacksonUtil;
import biocode.fims.validation.rules.RequiredValueRule;
import biocode.fims.validation.rules.Rule;
import biocode.fims.validation.rules.RuleLevel;
import biocode.fims.validation.rules.UniqueValueRule;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.LinkedHashSet;

/**
 * @author rjewing
 */
@JsonDeserialize(converter = FastaEntity.FastaEntitySanitizer.class)
public class FastaEntity extends PropEntity<FastaProps> {
    private static final String CONCEPT_URI = "urn:fastaSequence";
    public static final String TYPE = "Fasta";


    private FastaEntity() { // needed for EntityTypeIdResolver
        super(FastaProps.class);
    }

    public FastaEntity(String conceptAlias) {
        super(FastaProps.class, conceptAlias, CONCEPT_URI);
        init();
    }

    @Override
    protected void init() {
        super.init();
        getAttribute(FastaProps.IDENTIFIER.column()).setInternal(true);
        setUniqueKey(FastaProps.IDENTIFIER.column());
        getAttribute(FastaProps.SEQUENCE.column()).setInternal(true);
        getAttribute(FastaProps.MARKER.column()).setInternal(true);
        recordType = FastaRecord.class;

        // note: default rules are set in the FastaValidator
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public boolean getUniqueAcrossProject() {
        return false;
    }

    @Override
    public void addDefaultRules(Config config) {
        super.addDefaultRules(config);
        RequiredValueRule requiredValueRule = getRule(RequiredValueRule.class, RuleLevel.ERROR);

        if (requiredValueRule == null) {
            requiredValueRule = new RequiredValueRule(new LinkedHashSet<>(), RuleLevel.ERROR);
            addRule(requiredValueRule);
        }

        requiredValueRule.addColumn(FastaProps.SEQUENCE.column());
        requiredValueRule.addColumn(FastaProps.IDENTIFIER.column());

        UniqueValueRule uniqueValueRule = new UniqueValueRule(FastaProps.IDENTIFIER.column(), getUniqueAcrossProject(), RuleLevel.ERROR);
        addRule(uniqueValueRule);
    }


    @Override
    public Entity clone() {
        return clone(new FastaEntity(getConceptAlias()));
    }

    /**
     * class used to verify FastaEntity data integrity after deserialization. This is necessary
     * so we don't overwrite the default values during deserialization.
     */
    static class FastaEntitySanitizer extends PropEntitySanitizer<FastaEntity> {
    }
}


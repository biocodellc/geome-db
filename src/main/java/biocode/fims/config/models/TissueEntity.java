package biocode.fims.config.models;

import biocode.fims.config.Config;
import biocode.fims.config.network.NetworkConfig;
import biocode.fims.records.GenericRecord;
import biocode.fims.tissues.TissueProps;
import biocode.fims.validation.rules.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * @author rjewing
 */
@JsonDeserialize(converter = TissueEntity.TissueEntitySanitizer.class)
public class TissueEntity extends PropEntity<TissueProps> {
    private static final String CONCEPT_URI = "urn:Tissue";
    public static final String CONCEPT_ALIAS = "Tissue";
    public static final String TYPE = "Tissue";

    private static final String GENERATE_ID_KEY = "generateID";
    private static final String GENERATE_EMPTY_TISSUE_KEY = "generateEmptyTissue";

    private boolean generateID = false;
    // Will always generate a tissue when on the same sheet as samples
    // useful for DIPNet project where there may not be any tissue
    // attributes but the tissue is still needed to tie the fastaSequence
    // and fastqMetadata to the Sample
    private boolean generateEmptyTissue = false;


    public TissueEntity() {
        super(TissueProps.class, CONCEPT_ALIAS, CONCEPT_URI);
        init();
    }

    @Override
    protected void init() {
        super.init();
        getAttribute(TissueProps.IDENTIFIER.column());
        recordType = GenericRecord.class;
    }

    public boolean isGenerateID() {
        return generateID;
    }

    public void setGenerateID(boolean generateID) {
        this.generateID = generateID;
    }

    public boolean isGenerateEmptyTissue() {
        return generateEmptyTissue;
    }

    public void setGenerateEmptyTissue(boolean generateEmptyTissue) {
        this.generateEmptyTissue = generateEmptyTissue;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public boolean isHashed() {
        return false;
    }

    @Override
    public Map<String, Object> additionalProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(GENERATE_ID_KEY, generateID);
        props.put(GENERATE_EMPTY_TISSUE_KEY, generateEmptyTissue);
        return props;
    }

    @Override
    public void setAdditionalProps(Map<String, Object> props) {
        if (props == null) return;
        generateID = (boolean) props.getOrDefault(GENERATE_ID_KEY, false);
        generateEmptyTissue = (boolean) props.getOrDefault(GENERATE_EMPTY_TISSUE_KEY, false);
    }

    @Override
    public void addDefaultRules(Config config) {
        addRule(new ValidDataTypeFormatRule());

        // don't add the following rules to the network config b/c the projects configs
        // may choose to use a different uniqueKey the defined in the network
        if (!(config instanceof NetworkConfig)) {
            addRule(new ValidForURIRule(getUniqueKey(), RuleLevel.ERROR));

            RequiredValueRule requiredValueRule = getRule(RequiredValueRule.class, RuleLevel.ERROR);

            if (requiredValueRule == null) {
                requiredValueRule = new RequiredValueRule(new LinkedHashSet<>(), RuleLevel.ERROR);
                addRule(requiredValueRule);
            }

            Entity parentEntity = config.entity(getParentEntity());
            requiredValueRule.addColumn(parentEntity.getUniqueKey());
            addRule(new UniqueValueRule(getUniqueKey(), getUniqueAcrossProject(), RuleLevel.ERROR));
        }

        addRule(new ValidParentIdentifiersRule());
    }


    @Override
    public Entity clone() {
        TissueEntity entity = (TissueEntity) clone(new TissueEntity());

        entity.setGenerateID(isGenerateID());

        return entity;
    }

    @Override
    public boolean canReload() {
        return true;
    }

    @Override
    public boolean isValid(Config config) {
        if (!super.isValid(config)) return false;

        // only generateID or generateEmptyTissue if entity is on the same worksheet as the parent entity
        if (generateID || generateEmptyTissue) {
            Entity parent = config.entity(getParentEntity());
            if (parent == null || !parent.hasWorksheet() || !parent.getWorksheet().equals(getWorksheet())) {
                return false;
            }
        }

        return true;
    }

    /**
     * class used to verify TissueEntity data integrity after deserialization. This is necessary
     * so we don't overwrite the default values during deserialization.
     */
    static class TissueEntitySanitizer extends PropEntitySanitizer<TissueEntity> {
    }
}


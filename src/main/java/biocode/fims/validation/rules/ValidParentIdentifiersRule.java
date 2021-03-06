package biocode.fims.validation.rules;

import biocode.fims.config.models.Entity;
import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Checks that the each value specified in the parent id column exists in the parent RecordSet
 *
 * @author rjewing
 */
public class ValidParentIdentifiersRule extends AbstractRule {
    private static final String NAME = "ValidParentIdentifiers";
    private static final String GROUP_MESSAGE = "Invalid parent identifier(s)";

    public ValidParentIdentifiersRule() {
        super(RuleLevel.ERROR);
    }

    @Override
    public boolean run(RecordSet recordSet, EntityMessages messages) {
        Assert.notNull(recordSet);

        if (!recordSet.entity().isChildEntity()) {
            return true;
        }

        if (recordSet.parent() == null) {
            throw new IllegalStateException("Entity \"" + recordSet.entity().getConceptAlias() + "\" is a child entity, but the RecordSet.parent() was null");
        }

        String parentIdentifierUri = recordSet.parent().entity().getUniqueKeyURI();

        String expeditionCode = recordSet.expeditionCode();
        Set<String> parentIdentifiers = recordSet.parent().records().stream()
                .filter(r -> Objects.equals(r.expeditionCode(), expeditionCode))
                .map(r -> r.get(parentIdentifierUri))
                .collect(Collectors.toSet());

        String uri = recordSet.entity().getAttributeUri(recordSet.parent().entity().getUniqueKey());
        Set<String> invalidIdentifiers = new LinkedHashSet<>();

        for (Record r : recordSet.recordsToPersist()) {

            String value = r.get(uri);

            if (value.equals("") || !parentIdentifiers.contains(value)) {
                invalidIdentifiers.add(value);
                if (level().equals(RuleLevel.ERROR)) r.setError();
            }

        }

        if (invalidIdentifiers.size() == 0) {
            return true;
        }

        setMessages(invalidIdentifiers, messages, recordSet.entity().getParentEntity());
        setError();
        return false;
    }

    private void setMessages(Set<String> invalidValues, EntityMessages messages, String parentEntityAlias) {
        if (parentEntityAlias.equalsIgnoreCase("Tissue")) {
            messages.addMessage(
                             GROUP_MESSAGE,
                             new Message(
                                     "The following identifiers did not match a \"" + parentEntityAlias + "\"" +
                                             " identifier OR if loading FASTQ metadata, the filename regular expression" +
                                             " is invalid (see user help).  : " +
                                             "[\"" + String.join("\", \"", invalidValues) + "\"]"
                             ),
                             level()
                     );
        }   else {
            messages.addMessage(
                    GROUP_MESSAGE,
                    new Message(
                            "The following identifiers do not exist in the parent entity \"" + parentEntityAlias + "\": [\"" + String.join("\", \"", invalidValues) + "\"]"
                    ),
                    level()
            );
        }

    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean validConfiguration(List<String> messages, Entity entity) {
        return true;
    }

    @Override
    public boolean mergeRule(Rule r) {
        if (!r.getClass().equals(this.getClass())) return false;
        networkRule = networkRule || r.isNetworkRule();
        return true;
    }

    @Override
    public boolean contains(Rule r) {
        return r.getClass().equals(this.getClass());
    }
}

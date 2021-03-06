package biocode.fims.validation.rules;

import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Check a particular column to see if all the values are unique.
 * This rule is strongly encouraged for at least one column in the spreadsheet
 * <p>
 * NOTE: that NULL values are not counted in this rule, so this rule, by itself does not
 * enforce a primary key... it must be combined with a rule requiring some column value
 *
 * @author rjewing
 */
public class UniqueValueRule extends SingleColumnRule {
    private static final String NAME = "UniqueValue";
    private static final String GROUP_MESSAGE = "Unique value constraint did not pass";
    @JsonProperty
    private boolean uniqueAcrossProject = false;

    // needed for RuleTypeIdResolver to dynamically instantiate Rule implementation
    private UniqueValueRule() {
    }

    public UniqueValueRule(String column, boolean uniqueAcrossProject, RuleLevel level) {
        super(column, level);
        this.uniqueAcrossProject = uniqueAcrossProject;
    }

    public UniqueValueRule(String column, boolean uniqueAcrossProject) {
        this(column, uniqueAcrossProject, RuleLevel.WARNING);
    }

    @Override
    public boolean run(RecordSet recordSet, EntityMessages messages) {
        Assert.notNull(recordSet);

        if (!validConfiguration(recordSet, messages)) {
            return false;
        }

        String uri = recordSet.entity().getAttributeUri(column);

        Set<String> existingValues = new HashSet<>();
        List<String> duplicateValues = new ArrayList<>();

        List<Record> recordsToPersist = recordSet.recordsToPersist();

        if (recordsToPersist.size() > 0) {
            if (uniqueAcrossProject) {
                existingValues.addAll(
                        recordSet.records().stream()
                                .filter(r -> !r.persist())
                                .map(r -> r.get(uri))
                                .collect(Collectors.toList())
                );
            } else {
                // can only upload to a single expedition, so we can look at the first record to persist
                String uploadingExpeditionCode = recordSet.expeditionCode();

                existingValues.addAll(
                        recordSet.records().stream()
                                .filter(r -> Objects.equals(r.expeditionCode(), uploadingExpeditionCode) && !r.persist())
                                .map(r -> r.get(uri))
                                .collect(Collectors.toList())
                );
            }
        }

        for (Record r : recordsToPersist) {

            String value = r.get(uri);

            if (!value.equals("") && !existingValues.add(value)) {
                duplicateValues.add(value);
                if (level().equals(RuleLevel.ERROR)) r.setError();
            }

        }

        if (duplicateValues.size() == 0) {
            return true;
        }

        setMessages(duplicateValues, messages);
        setError();
        return false;
    }

    public boolean uniqueAcrossProject() {
        return uniqueAcrossProject;
    }

    private void setMessages(List<String> invalidValues, EntityMessages messages) {
        String msg = "\"" + column + "\" column is defined as unique ";
        if (uniqueAcrossProject) msg += "across the entire project ";
        msg += "but some values used more than once: \"" + String.join("\", \"", invalidValues) + "\"";
        messages.addMessage(
                GROUP_MESSAGE,
                new Message(msg),
                level()
        );
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean mergeRule(Rule r) {
        if (!r.getClass().equals(this.getClass())) return false;

        UniqueValueRule rule = (UniqueValueRule) r;

        if (rule.level().equals(level())
                && rule.column().equals(column)) {
            if (rule.uniqueAcrossProject && rule.networkRule
                    || uniqueAcrossProject && networkRule
                    || (!rule.uniqueAcrossProject && !uniqueAcrossProject) && (networkRule || rule.networkRule)) {
                networkRule = true;
            }
            uniqueAcrossProject = uniqueAcrossProject || rule.uniqueAcrossProject;
            return true;
        }
        return false;
    }

    @Override
    public boolean contains(Rule r) {
        if (!r.getClass().equals(this.getClass())) return false;

        UniqueValueRule rule = (UniqueValueRule) r;

        if (rule.level().equals(level())
                && rule.column().equals(column)) {

            // rule needs tobe as strict or more strict to consider r contained within the rule
            return !rule.uniqueAcrossProject || uniqueAcrossProject;
        }
        return false;
    }
}

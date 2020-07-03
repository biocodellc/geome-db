package biocode.fims.validation.rules;

import biocode.fims.config.models.Entity;
import biocode.fims.fastq.FastqRecord;
import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.List;

/**
 * @author rjewing
 */
public class FastqMetadataRule extends AbstractRule {
    private static final Logger logger = LoggerFactory.getLogger(FastqMetadataRule.class);

    private static final String NAME = "ValidFastqMetadata";
    private static final String GROUP_MESSAGE = "Invalid fastq metadata";

    public FastqMetadataRule() {
        super(RuleLevel.ERROR);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean run(RecordSet recordSet, EntityMessages messages) {
        Assert.notNull(recordSet);

        if (recordSet.parent() == null) {
            throw new IllegalStateException("FastqEntity \"" + recordSet.entity().getConceptAlias() + "\" is a child entity, but the RecordSet.parent() was null");
        }

        String idKey = recordSet.parent().entity().getUniqueKey();

        for (Record r : recordSet.recordsToPersist()) {
            FastqRecord record = (FastqRecord) r;

            if (!inList("libraryStrategy", record.libraryStrategy())) {
                setError();
                if (level().equals(RuleLevel.ERROR)) r.setError();
                messages.addErrorMessage(
                        GROUP_MESSAGE,
                        new Message("\"" + r.get(idKey) + "\" has an invalid value: \"" + record.libraryStrategy() + "\" for \"libraryStrategy\"")
                );
            }
            if (!inList("librarySource", record.librarySource())) {
                setError();
                if (level().equals(RuleLevel.ERROR)) r.setError();
                messages.addErrorMessage(
                        GROUP_MESSAGE,
                        new Message("\"" + r.get(idKey) + "\" has an invalid value: \"" + record.librarySource() + "\" for \"librarySource\"")
                );
            }
            if (!inList("librarySelection", record.librarySelection())) {
                setError();
                if (level().equals(RuleLevel.ERROR)) r.setError();
                messages.addErrorMessage(
                        GROUP_MESSAGE,
                        new Message("\"" + r.get(idKey) + "\" has an invalid value: \"" + record.librarySelection() + "\" for \"librarySelection\"")
                );
            }
            if (!inList("platform", record.platform())) {
                setError();
                if (level().equals(RuleLevel.ERROR)) r.setError();
                messages.addErrorMessage(
                        GROUP_MESSAGE,
                        new Message("\"" + r.get(idKey) + "\" has an invalid value: \"" + record.platform() + "\" for \"platform\"")
                );
            }
            if (!inList(record.platform(), record.instrumentModel())) {
                setError();
                if (level().equals(RuleLevel.ERROR)) r.setError();
                messages.addErrorMessage(
                        GROUP_MESSAGE,
                        new Message("\"" + r.get(idKey) + "\" has an invalid value: \"" + record.instrumentModel() + "\" for \"instrumentModel\"")
                );
            }
        }

        return !hasError();
    }

    @Override
    public boolean validConfiguration(List<String> messages, Entity entity) {
        return true;
    }

    private boolean inList(String listAlias, String value) {
        if (value == null) return false;

        biocode.fims.config.models.List list = config.findList(listAlias);

        // don't throw an exception if the list isn't defined
        if (list == null) {
            logger.debug("Couldn't find List for FastqMetadata key: " + listAlias);
            return true;
        }

        return list.getFields().stream()
                .anyMatch(f -> (list.getCaseInsensitive())
                        ? StringUtils.equalsIgnoreCase(f.getValue(), value)
                        : f.getValue().equals(value));
    }
}

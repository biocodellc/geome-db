package biocode.fims.validation.rules;

import biocode.fims.config.models.Entity;
import biocode.fims.fastq.FastqRecord;
import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;

/**
 * @author rjewing
 */
public class FastqLibraryLayoutRule extends AbstractRule {
    private static final String NAME = "ValidFastqLibraryLayout";
    private static final String GROUP_MESSAGE = "Invalid fastq LibraryLayout";
    private final List<String> VALID_LAYOUTS = Arrays.asList("single", "paired");

    public FastqLibraryLayoutRule() {
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

        for (Record r : recordSet.recordsToPersist()) {
            FastqRecord record = (FastqRecord) r;

            if (!VALID_LAYOUTS.contains(record.libraryLayout())) {
                // All records will have the same layout
                // TODO this may change if we allow modification of single records (not complete datasets)
                messages.addMessage(
                        GROUP_MESSAGE,
                        new Message(
                                "Invalid libraryLayout for FastqRecord. \"" + record.libraryLayout() + "\" is not \"single\" or \"paired\"."
                        ),
                        level()
                );

                if (level().equals(RuleLevel.ERROR)) recordSet.recordsToPersist().forEach(Record::setError);

                setError();
                return false;
            }

        }

        return true;
    }

    @Override
    public boolean validConfiguration(List<String> messages, Entity entity) {
        return true;
    }
}

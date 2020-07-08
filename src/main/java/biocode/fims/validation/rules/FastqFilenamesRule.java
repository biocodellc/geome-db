package biocode.fims.validation.rules;

import biocode.fims.config.models.Entity;
import biocode.fims.fastq.FastqRecord;
import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import org.springframework.util.Assert;

import java.util.List;
import java.util.regex.Pattern;

/**
 * @author rjewing
 */
public class FastqFilenamesRule extends AbstractRule {
    private static final String NAME = "ValidFastqFilenames";
    private static final String MISSING_GROUP_MESSAGE = "Missing fastq filenames";
    private static final String INVALID_GROUP_MESSAGE = "Invalid fastq filenames";

    private static final Pattern PAIRED_FILE1_PATTERN = Pattern.compile("^([a-zA-Z0-9+=:._()~*]+)(-.*)?([.|_|-]+.*[1]+)\\.(fq|fastq)(\\.gz|\\.gzip|\\.bz2)?$");
    private static final Pattern PAIRED_FILE2_PATTERN = Pattern.compile("^([a-zA-Z0-9+=:._()~*]+)(-.*)?([.|_|-]+.*[2]+)\\.(fq|fastq)(\\.gz|\\.gzip|\\.bz2)?$");
    private static final Pattern SINGLE_FILE_PATTERN = Pattern.compile("^([a-zA-Z0-9+=:._()~*]+)(-.*)?\\.(fq|fastq)(\\.gz|\\.gzip|\\.bz2)?$");

    public FastqFilenamesRule() {
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

        String idKey = recordSet.parent().entity().getUniqueKeyURI();

        boolean isValid = true;

        for (Record r : recordSet.recordsToPersist()) {
            FastqRecord record = (FastqRecord) r;

            String id = record.get(idKey);

            boolean isPaired = record.libraryLayout().equals("paired");

            if (!isPaired && record.filenames().size() != 1 || isPaired && record.filenames().size() != 2) {
                isValid = false;
                if (level().equals(RuleLevel.ERROR)) r.setError();
                messages.addErrorMessage(
                        MISSING_GROUP_MESSAGE,
                        new Message("\"" + id + "\" should have " + ((isPaired) ? "2" : "1") + " files, but found " + record.filenames().size())
                );
            }

            boolean found1 = false;
            boolean found2 = false;
            for (String f : record.filenames()) {
                if (isPaired && PAIRED_FILE1_PATTERN.matcher(f).matches()) {
                    found1 = true;
                } else if (isPaired && PAIRED_FILE2_PATTERN.matcher(f).matches()) {
                    found2 = true;
                } else if (isPaired || !isPaired && !SINGLE_FILE_PATTERN.matcher(f).matches()) {
                    isValid = false;
                    if (level().equals(RuleLevel.ERROR)) r.setError();
                    messages.addErrorMessage(
                            INVALID_GROUP_MESSAGE,
                            new Message("\"" + f + "\" does not match the correct naming format")
                    );
                }
            }

            if (isPaired && (!found1 || !found2)) {
                if (level().equals(RuleLevel.ERROR)) r.setError();
                messages.addErrorMessage(
                        INVALID_GROUP_MESSAGE,
                        new Message("\"" + id + "\" is missing 1 or both files")
                );
            }
        }

        if (!isValid) setError();

        return isValid;
    }

    @Override
    public boolean validConfiguration(List<String> messages, Entity entity) {
        return true;
    }
}

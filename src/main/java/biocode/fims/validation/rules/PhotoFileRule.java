package biocode.fims.validation.rules;

import biocode.fims.config.models.Entity;
import biocode.fims.photos.PhotoEntityProps;
import biocode.fims.photos.PhotoRecord;
import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * @author rjewing
 */
public class PhotoFileRule extends AbstractRule {
    private static final String NAME = "ValidPhotoFile";
    private static final String GROUP_MESSAGE = "Missing Photo file";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public RuleLevel level() {
        return RuleLevel.ERROR;
    }

    @Override
    public boolean run(RecordSet recordSet, EntityMessages messages) {
        Assert.notNull(recordSet);

        List<String> photosMissingFile = new ArrayList<>();

        for (Record r : recordSet.recordsToPersist()) {
            PhotoRecord record = (PhotoRecord) r;

            if (record.bulkLoad()) continue;
            else if (!record.originalUrl().equals("")) continue;
            else if (record.get(PhotoEntityProps.PROCESSED.uri()).equalsIgnoreCase("true")) continue;

            photosMissingFile.add(record.photoID());
            if (level().equals(RuleLevel.ERROR)) r.setError();

        }

        if (photosMissingFile.size() == 0) {
            return true;
        }

        setMessages(messages, photosMissingFile);
        setError();
        return false;
    }

    private void setMessages(EntityMessages messages, List<String> photosMissingFile) {

        for (String key : photosMissingFile) {
            messages.addMessage(
                    GROUP_MESSAGE,
                    new Message(
                            "row with " + PhotoEntityProps.PHOTO_ID.column() + "=" + key + " must have already been processed, or " +
                                    "have a \"" + PhotoEntityProps.ORIGINAL_URL.column() + "\"."),
                    level()
            );
        }
    }

    @Override
    public boolean validConfiguration(List<String> messages, Entity entity) {
        return true;
    }

    @Override
    public boolean mergeRule(Rule r) {
        if (!r.getClass().equals(this.getClass())) return false;

        return r.level().equals(level());
    }

}


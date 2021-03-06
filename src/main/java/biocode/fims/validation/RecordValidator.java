package biocode.fims.validation;

import biocode.fims.config.models.Entity;
import biocode.fims.records.RecordSet;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.utils.Timer;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.rules.*;
import org.springframework.util.Assert;

import java.util.*;

/**
 * This class serves as a base for all other RecordSet validators and is meant to be extended.
 * <p>
 * Sub-classes should also create a {@link ValidatorInstantiator} implementation to instantiate a instance. This is
 * used in the RecordValidatorFactory
 * <p>
 * This class will apply any {@link Rule} defined in the {@link Entity} to the {@link RecordSet}
 *
 * @author rjewing
 */
public class RecordValidator {
    protected ProjectConfig config;
    protected EntityMessages messages;
    protected boolean hasError = false;
    protected boolean isValid = true;

    public RecordValidator(ProjectConfig config) {
        Assert.notNull(config);
        this.config = config;
    }

    public boolean validate(RecordSet recordSet, boolean upload) {
        Assert.notNull(recordSet);

        this.messages = new EntityMessages(recordSet.conceptAlias(), recordSet.entity().getWorksheet());

        Set<Rule> rules = recordSet.entity().getRules();

        for (Rule r : rules) {

            // Bypass certain rules for cases where we do are not uploading data
            // This only runs in cases where upload = false
            // 1. the ValidForURI rule checks tissueID
            if ((!upload &&
                    (!r.name().equalsIgnoreCase("ValidForURI")) ||
                    upload)) {

                r.setProjectConfig(config);

                Timer.getInstance().lap("running rule: " + r.name());

                if (!r.run(recordSet, messages)) {

                    if (r.hasError()) {
                        hasError = true;
                    }

                    isValid = false;
                }
                Timer.getInstance().lap("finished running rule: " + r.name());
            }
        }
        return isValid;
    }

    public boolean hasError() {
        return hasError;
    }

    public EntityMessages messages() {
        return messages;
    }

    public static class DefaultValidatorInstantiator implements ValidatorInstantiator {
        @Override
        public RecordValidator newInstance(ProjectConfig config) {
            return new RecordValidator(config);
        }
    }
}

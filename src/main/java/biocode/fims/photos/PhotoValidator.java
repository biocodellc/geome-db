package biocode.fims.photos;

import biocode.fims.config.project.ProjectConfig;
import biocode.fims.records.RecordSet;
import biocode.fims.validation.RecordValidator;
import biocode.fims.validation.ValidatorInstantiator;

/**
 * @author rjewing
 */
public class PhotoValidator extends RecordValidator {

    public PhotoValidator(ProjectConfig config) {
        super(config);
    }

    @Override
    public boolean validate(RecordSet recordSet) {

        return super.validate(recordSet);
    }

    public static class PhotoValidatorInstantiator implements ValidatorInstantiator {
        @Override
        public RecordValidator newInstance(ProjectConfig config) {
            return new PhotoValidator(config);
        }
    }
}

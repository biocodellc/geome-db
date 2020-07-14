package biocode.fims.fastq;

import biocode.fims.config.project.ProjectConfig;
import biocode.fims.validation.RecordValidator;
import biocode.fims.validation.ValidatorInstantiator;

/**
 * @author rjewing
 */
public class FastqValidator extends RecordValidator {

    public FastqValidator(ProjectConfig config) {
        super(config);
    }

    public static class FastqValidatorInstantiator implements ValidatorInstantiator {
        @Override
        public RecordValidator newInstance(ProjectConfig config) {
            return new FastqValidator(config);
        }
    }
}

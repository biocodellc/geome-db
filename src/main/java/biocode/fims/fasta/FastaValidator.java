package biocode.fims.fasta;

import biocode.fims.config.project.ProjectConfig;
import biocode.fims.validation.RecordValidator;
import biocode.fims.validation.ValidatorInstantiator;


/**
 * @author rjewing
 */
public class FastaValidator extends RecordValidator {

    public FastaValidator(ProjectConfig config) {
        super(config);
    }


    public static class FastaValidatorInstantiator implements ValidatorInstantiator {
        @Override
        public RecordValidator newInstance(ProjectConfig config) {
            return new FastaValidator(config);
        }
    }
}

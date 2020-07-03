package biocode.fims.exceptions;

import biocode.fims.fimsExceptions.errorCodes.ErrorCode;

/**
 * Created by rjewing on 10/23/16.
 */
public enum SraCode implements ErrorCode {
    MISSING_DATASET, MISSING_FASTQ_METADATA, SRA_FILES_FAILED, METADATA_FILE_CREATION_FAILED
}

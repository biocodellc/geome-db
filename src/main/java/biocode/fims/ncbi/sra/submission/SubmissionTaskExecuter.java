package biocode.fims.ncbi.sra.submission;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.application.config.TissueProperties;
import biocode.fims.models.SraSubmissionEntry;
import biocode.fims.ncbi.models.submission.SraSubmission;
import biocode.fims.repositories.SraSubmissionRepository;
import biocode.fims.utils.EmailUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author rjewing
 */
public class SubmissionTaskExecuter {
    private static final long ONE_HOUR = 1000 * 60 * 60;
    private static final Logger logger = LoggerFactory.getLogger(SubmissionTaskExecuter.class);

    private final SraSubmissionRepository submissionRepository;
    private final TissueProperties tissueProperties;

    public SubmissionTaskExecuter(SraSubmissionRepository submissionRepository, TissueProperties tissueProperties) {
        this.submissionRepository = submissionRepository;
        this.tissueProperties = tissueProperties;
    }

    @Scheduled(initialDelay = 60 * 1000 * 2, fixedDelay = ONE_HOUR)
    public void scheduleTasks() {
        for (SraSubmissionEntry submission : submissionRepository.getByStatus(SraSubmissionEntry.Status.READY)) {
            submit(submission);
        }
    }

    private void submit(SraSubmissionEntry submission) {
        FTPClient client = new FTPClient();
        FileInputStream fis = null;

        try {
            client.connect(tissueProperties.sraSubmissionUrl());
            client.login(tissueProperties.sraSubmissionUser(), tissueProperties.sraSubmissionPassword());
            client.enterLocalPassiveMode();

            String dirName = tissueProperties.sraSubmissionRootDir() + "/" + submission.getSubmissionDir().getFileName().toString();

            // make the submission directory
            client.makeDirectory(dirName);
            client.changeWorkingDirectory(dirName);

            // upload all files
            File dir = submission.getSubmissionDir().toFile();
            for (File file : dir.listFiles()) {
                fis = new FileInputStream(file);
                client.storeFile(file.getName(), fis);
                fis.close();
                fis = null;
            }

            // signal that all files have been uploaded and are ready for processing
            client.storeFile("submit.ready", new ByteArrayInputStream(new byte[0]));

            submission.setStatus(SraSubmissionEntry.Status.SUBMITTED);

            client.logout();
        } catch (IOException e) {
            logger.error("Failed to submit via FTP to SRA.", e);
            submission.setStatus(SraSubmissionEntry.Status.FAILED);
            submission.setMessage(e.getMessage());
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
                client.disconnect();
            } catch (IOException e) {
                logger.error("Error closing file stream", e);
            }
        }

        this.submissionRepository.save(submission);

        if (submission.getStatus() == SraSubmissionEntry.Status.SUBMITTED) {
            // notify user
            EmailUtils.sendEmail(
                    submission.getUser().getEmail(),
                    "SRA Submission Successful",
                    "Your submission for \"" + submission.getExpedition().getExpeditionTitle() + "\" has been successfully uploaded to the SRA via GEOME.\n\n" +
                            "You will receive another email with a status report once the SRA has processed your submission."
            );
        } else if (submission.getStatus() == SraSubmissionEntry.Status.FAILED) {
            EmailUtils.sendEmail(
                    submission.getUser().getEmail(),
                    "GEOME - SRA Submission Failed",
                    "Your submission for \"" + submission.getExpedition().getExpeditionTitle() + "\" has failed. Please contact GEOME for help."
            );
        }
    }
}

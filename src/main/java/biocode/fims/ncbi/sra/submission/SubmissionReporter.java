package biocode.fims.ncbi.sra.submission;

import biocode.fims.application.config.TissueProperties;
import biocode.fims.models.SraSubmissionEntry;
import biocode.fims.ncbi.models.submission.SraSubmissionReport;
import biocode.fims.repositories.SraSubmissionRepository;
import biocode.fims.utils.EmailUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;

/**
 * @author rjewing
 */
public class SubmissionReporter {
    private static final long FOUR_HOURS = 1000 * 60 * 60 * 4;
    private static final Logger logger = LoggerFactory.getLogger(SubmissionReporter.class);

    private final SraSubmissionRepository submissionRepository;
    private final TissueProperties tissueProperties;

    public SubmissionReporter(SraSubmissionRepository submissionRepository, TissueProperties tissueProperties) {
        this.submissionRepository = submissionRepository;
        this.tissueProperties = tissueProperties;
    }

    @Scheduled(initialDelay = 60 * 1000 * 5, fixedDelay = FOUR_HOURS)
    public void checkSubmissions() throws IOException {
        FTPClient client = new FTPClient();
        client.connect(tissueProperties.sraSubmissionUrl());
        client.login(tissueProperties.sraSubmissionUser(), tissueProperties.sraSubmissionPassword());
        client.enterLocalPassiveMode();

        for (SraSubmissionEntry submission : submissionRepository.getByStatus(SraSubmissionEntry.Status.SUBMITTED)) {
            checkReport(client, submission);
        }

        client.logout();
        client.disconnect();
    }

    private void checkReport(FTPClient client, SraSubmissionEntry submission) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ByteArrayInputStream is = null;

        try {
            String dirName = tissueProperties.sraSubmissionRootDir() + "/" + submission.getSubmissionDir().getFileName().toString();
            String reportFile= dirName + "/report.xml";

            // hasn't started processing yet
            if (client.listFiles(reportFile).length == 0) {
                return;
            }

            client.retrieveFile(reportFile, os);

            is = new ByteArrayInputStream(os.toByteArray());
            JAXBContext jaxbContext = JAXBContext.newInstance(SraSubmissionReport.class);
            Unmarshaller marshaller = jaxbContext.createUnmarshaller();
            SraSubmissionReport report = (SraSubmissionReport) marshaller.unmarshal(is);

            if (report.isProcessing()) return;

            if (report.isSuccess()) {
                submission.setStatus(SraSubmissionEntry.Status.COMPLETED);
                submission.setMessage(null);
            } else if (report.isError()) {
                submission.setStatus(SraSubmissionEntry.Status.SUBMISSION_ERROR);
                submission.setMessage(report.getErrorMessage());
            }
        } catch (JAXBException e) {
            logger.error("Failed to unmarshall report.xml", e);
        } catch (IOException e) {
            logger.error("Failed to read SRA submission report.", e);
        } finally {
            try {
                os.close();
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                logger.error("Error closing input stream", e);
            }
        }

        this.submissionRepository.save(submission);

        // notify user
        if (submission.getStatus().equals(SraSubmissionEntry.Status.COMPLETED)) {
            EmailUtils.sendEmail(
                    submission.getUser().getEmail(),
                    "GEOME - SRA Submission Successful",
                    "Your submission for \"" + submission.getExpedition().getExpeditionTitle() + "\" has been successfully processed by the SRA.\n\n" +
                            "You should have received an email from the SRA asking you to take ownership of your submission. If you did not, please contact geome.help@gmail.com."
            );
            submission.getSubmissionDir().toFile().delete();
        } else if (submission.getStatus().equals(SraSubmissionEntry.Status.SUBMISSION_ERROR)) {
            EmailUtils.sendEmail(
                    submission.getUser().getEmail(),
                    "GEOME - SRA Submission Failed",
                    "Your submission for \"" + submission.getExpedition().getExpeditionTitle() + "\" has failed with the following reason:\n\n\n" + submission.getMessage() + "\n\n\nPlease contact geome.help@geome.com if you need assistance"
            );
        }
    }
}

package biocode.fims.ncbi.models.submission;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.persistence.oxm.annotations.XmlPath;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

/**
 * @author rjewing
 */
@XmlRootElement(name = "SubmissionStatus")
public class SraSubmissionReport {
    private final static String SUBMITTED = "submitted";
    private final static String PROCESSING = "processing";
    private final static String FAILED = "failed";
    private final static String ERROR = "processed-error";

    @XmlPath("@status")
    private String status;
    @XmlPath("Message/text()")
    private String failedMessage;
    @XmlPath("Action")
    private ArrayList<Action> actions;

    public static class Action {
        @XmlPath("@target_db")
        private String target_db;
        @XmlPath("@status")
        private String status;
        @XmlPath("Response/Message/text()")
        private String errorMessage;

        // these are specific to BioSample db
        @XmlPath("Response/Object/@spuid")
        private String sampleName;
        @XmlPath("Response/Object/Details/ExistingSample/text()")
        private String existingSampleName;
    }

    public boolean isProcessing() {
        if (status.equalsIgnoreCase(SUBMITTED)) return true;

        if (status.equalsIgnoreCase(PROCESSING)) {
            // when target_db=sra Action.status is always submitted
            for (Action action : actions) {
                if (action.target_db.equalsIgnoreCase("biosample") || action.target_db.equalsIgnoreCase("bioproject")) {
                    if (action.status.equalsIgnoreCase(SUBMITTED) || action.status.equalsIgnoreCase(PROCESSING))
                        return true;
                }
            }
        }

        return false;
    }

    public boolean isError() {
        return status.equalsIgnoreCase(FAILED) || status.equalsIgnoreCase(ERROR);
    }

    public boolean isSuccess() {
        return !isError() && !isProcessing();
    }

    public String getErrorMessage() {
        StringBuilder msg = new StringBuilder();

        if (StringUtils.isBlank(failedMessage)) {
            actions.stream()
                    .filter(a -> a.status.equalsIgnoreCase(ERROR))
                    .forEach(a -> {
                        msg.append("Error occurred in submitted ");
                        msg.append(a.target_db);
                        msg.append(":\n");

                        msg.append("\tError Message: ");
                        msg.append(a.errorMessage);
                        msg.append("\n");

                        if (a.target_db.equalsIgnoreCase("biosample")) {
                            msg.append("\tSampleName: ");
                            msg.append(a.sampleName);
                            msg.append("\n");
                            msg.append("\tExisting SampleName: ");
                            msg.append(a.existingSampleName);
                            msg.append("\n");
                        }
                        msg.append("\n");
                    });
        } else {
            msg.append(failedMessage);
        }

        return msg.toString();
    }

}

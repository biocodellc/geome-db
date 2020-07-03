package biocode.fims.application.config;

import org.springframework.core.env.Environment;

/**
 * @author rjewing
 */
public class TissueProperties {
    private final Environment env;

    public TissueProperties(Environment env) {
        this.env = env;
    }

    public String sraSubmissionUser() {
        return env.getRequiredProperty("sraSubmissionUser");
    }

    public String sraSubmissionPassword() {
        return env.getRequiredProperty("sraSubmissionPassword");
    }

    public String sraSubmissionUrl() {
        return env.getRequiredProperty("sraSubmissionUrl");
    }

    public String sraSubmissionRootDir() {
        return env.getRequiredProperty("sraSubmissionRootDir");
    }

    public String sraSubmissionDir() {
        String dir = env.getRequiredProperty("sraSubmissionDir");
        if (!dir.endsWith("/")) {
            dir += "/";
        }
        return dir;
    }
}

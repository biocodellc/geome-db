package biocode.fims.run;

import biocode.fims.application.config.GeomeAppConfig;
import biocode.fims.ncbi.sra.submission.SubmissionReporter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;

/**
 * This class only exists because in production (only) we get a ClassNotFounder error when running as
 * a scheduled task. So we use this and run as a cron job.
 *
 * @author rjewing
 */
public class SraSubmissionReporterRunner {

    public static void main(String[] args) throws IOException {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(GeomeAppConfig.class);
        SubmissionReporter reporter = applicationContext.getBean(SubmissionReporter.class);
        reporter.checkSubmissions();
    }
}

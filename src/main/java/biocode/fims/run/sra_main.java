package biocode.fims.ncbi.sra.submission;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.application.config.TissueProperties;
import biocode.fims.authorizers.QueryAuthorizer;
import biocode.fims.models.User;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.repositories.SraSubmissionRepository;
import biocode.fims.rest.UserContext;
import biocode.fims.rest.services.subResources.SraResource;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.ProjectService;
import biocode.fims.service.UserService;
import biocode.fims.tools.FileCache;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import biocode.fims.application.config.GeomeAppConfig;

public class sra_main {

    public static void main(String[] args) {
        // Load Spring application context
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(GeomeAppConfig.class);

        // Fetch dependencies from Spring
        FimsProperties props = context.getBean(FimsProperties.class);
        ProjectService projectService = context.getBean(ProjectService.class);
        ExpeditionService expeditionService = context.getBean(ExpeditionService.class);
        UserService userService = context.getBean(UserService.class);
        QueryAuthorizer queryAuthorizer = context.getBean(QueryAuthorizer.class);
        RecordRepository recordRepository = context.getBean(RecordRepository.class);
        FileCache fileCache = context.getBean(FileCache.class);
        TissueProperties tissueProperties = context.getBean(TissueProperties.class);
        SraMetadataMapper sraMetadataMapper = context.getBean(SraMetadataMapper.class);
        BioSampleMapper bioSampleMapperInstance = context.getBean(BioSampleMapper.class);
        SraSubmissionRepository sraSubmissionRepository = context.getBean(SraSubmissionRepository.class);

        // Create SraResource instance
        SraResource sraResource = new SraResource(
                props, projectService, expeditionService, userService, queryAuthorizer, recordRepository,
                fileCache, tissueProperties, sraMetadataMapper, bioSampleMapperInstance, sraSubmissionRepository
        );

        // Simulate user login
        UserContext userContext = context.getBean(UserContext.class);
        User user = userService.getUser("exampleUser"); // Change this to actual user retrieval logic
        userContext.setUser(user);

        // Define parameters
        int projectId = 446;
        String expeditionCode = "NMFS_FISHES_NOVASEQ-03_Expedition04";
        String format = "file";

        try {
            // Call getSubmissionData
            Object submissionData = sraResource.getSubmissionData(projectId, expeditionCode, format);
            System.out.println("Submission Data: {}" + submissionData);
        } catch (Exception e) {
            System.out.println("Error retrieving submission data" + e);
        } finally {
            context.close();
        }
    }
}

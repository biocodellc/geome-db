package biocode.fims.run;

import biocode.fims.entities.Bcid;
import biocode.fims.ezid.EzidException;
import biocode.fims.ezid.EzidService;
import biocode.fims.ezid.EzidUtils;
import biocode.fims.service.BcidService;
import biocode.fims.settings.SettingsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.HashMap;
import java.util.Set;

/**
 * Created by rjewing on 9/6/16.
 */
public class EzidUpdator {
    private static final Logger logger = LoggerFactory.getLogger(EzidUpdator.class);
    private BcidService bcidService;
    private SettingsManager settingsManager;
    private final EzidUtils ezidUtils;

    @Autowired
    public EzidUpdator(BcidService bcidService, SettingsManager settingsManager, EzidUtils ezidUtils) {
        this.bcidService = bcidService;
        this.settingsManager = settingsManager;
        this.ezidUtils = ezidUtils;
    }

    /**
     * Update EZID Bcid metadata for this particular ID
     */
    private void updateBcidsEZID(EzidService ezidService, Bcid bcid) throws EzidException {
        // Build the hashmap to pass to ezidService
        // Get creator, using any system defined creator to override the default which is based on user data
        HashMap<String, String> map = ezidUtils.getDcMap(bcid);

        try {
            ezidService.setMetadata(String.valueOf(bcid.getIdentifier()), map);
            logger.info("  Updated Metadata for " + bcid.getIdentifier());
        } catch (EzidException e1) {
            // After attempting to set the Metadata, if another exception is thrown then who knows,
            // probably just a permissions issue.
            throw new EzidException("  Exception thrown in attempting to create EZID " + bcid.getIdentifier() + ", likely a permission issue", e1);
        }
    }

    public void run() throws EzidException {
        Set<Bcid> bcidsWithEzidRequest = bcidService.getBcidsWithEzidRequest();
        EzidService ezidService = new EzidService();
        ezidService.login(settingsManager.retrieveValue("eziduser"), settingsManager.retrieveValue("ezidpass"));

        for (Bcid bcid : bcidsWithEzidRequest) {
            try {
                updateBcidsEZID(ezidService, bcid);
                bcid.setEzidMade(true);
                bcidService.update(bcid);
            } catch (EzidException e) {
                System.out.println("Failed to update EZID for bcidId: " + bcid.getBcidId());
            }
        }

    }

    public static void main(String[] args) throws Exception {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("/applicationContext.xml");
        EzidUpdator updator = applicationContext.getBean(EzidUpdator.class);

        updator.run();
    }
}

package biocode.fims.run;

import biocode.fims.bcid.ManageEZID;
import biocode.fims.entities.Bcid;
import biocode.fims.ezid.EzidException;
import biocode.fims.ezid.EzidService;
import biocode.fims.service.BcidService;
import biocode.fims.settings.SettingsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Set;

/**
 * Created by rjewing on 9/6/16.
 */
public class EzidUpdator {
    private BcidService bcidService;
    private SettingsManager settingsManager;

    @Autowired
    public EzidUpdator(BcidService bcidService, SettingsManager settingsManager) {
        this.bcidService = bcidService;
        this.settingsManager = settingsManager;
    }

    public void run() throws EzidException {
        Set<Bcid> bcidsWithEzidRequest = bcidService.getBcidsWithEzidRequest();
        ManageEZID ezidManager = new ManageEZID();
        EzidService ezidService = new EzidService();
        ezidService.login(settingsManager.retrieveValue("eziduser"), settingsManager.retrieveValue("ezidpass"));

        for (Bcid bcid : bcidsWithEzidRequest) {
            try {
                ezidManager.updateBcidsEZID(ezidService, bcid.getBcidId());
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

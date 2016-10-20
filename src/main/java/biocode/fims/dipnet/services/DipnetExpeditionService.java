package biocode.fims.dipnet.services;

import biocode.fims.digester.Mapping;
import biocode.fims.dipnet.entities.DipnetExpedition;
import biocode.fims.dipnet.repositories.DipnetExpeditionRepository;
import biocode.fims.entities.Expedition;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.service.ExpeditionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Service class for handling {@link DipnetExpedition} persistence
 */
@Service
@Transactional("dipnetTransactionManager")
public class DipnetExpeditionService {
    private final ExpeditionService expeditionService;
    private final DipnetExpeditionRepository dipnetExpeditionRepository;

    @Autowired
    public DipnetExpeditionService(DipnetExpeditionRepository dipnetExpeditionRepository, ExpeditionService expeditionService) {
        this.dipnetExpeditionRepository = dipnetExpeditionRepository;
        this.expeditionService = expeditionService;
    }

    /**
     * Create a DipnetExpedition. This involves first creating a Biocode {@link Expedition}, then setting
     * the DipnetExpedition.expeditionId field. Then persisting the {@link DipnetExpedition}
     * @param dipnetExpedition
     */
    public void create(DipnetExpedition dipnetExpedition) {
        // hack until we get FastqFileManger.upload working, but for now, the Expedition should already be created

        if (dipnetExpedition.getExpedition().getExpeditionId() == 0) {
            throw new ServerErrorException("Server Error", "Expedition should have already been created");
        }

        dipnetExpedition.setExpeditionId(dipnetExpedition.getExpedition().getExpeditionId());
        dipnetExpeditionRepository.save(dipnetExpedition);

        /*
        expeditionService.create(dipnetExpedition.getExpedition(), userId, projectId, null, mapping);

        try {
            // now that the Expedition's id has been set
            dipnetExpedition.setExpeditionId(dipnetExpedition.getExpedition().getExpeditionId());
            // need to manually call update as expedition and dipnetExpedition are in different entityManagers/databases
            expeditionService.update(dipnetExpedition.getExpedition());

            dipnetExpeditionRepository.save(dipnetExpedition);
        } catch (Exception e) {
            // on any exception, delete the expedition
            expeditionService.delete(dipnetExpedition.getExpeditionId());
        }
        */
    }


    public void update(DipnetExpedition dipnetExpedition) {
        dipnetExpeditionRepository.save(dipnetExpedition);
        expeditionService.update(dipnetExpedition.getExpedition());
    }

    public DipnetExpedition getDipnetExpedition(int dipnetExpeditionId) {
        Expedition expedition = expeditionService.getExpedition(dipnetExpeditionId);

        DipnetExpedition dipnetExpedition = dipnetExpeditionRepository.findOneByExpeditionId(dipnetExpeditionId);
        dipnetExpedition.setExpedition(expedition);

        return dipnetExpedition;
    }

}

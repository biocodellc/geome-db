package biocode.fims.dipnet.repositories;

import biocode.fims.dipnet.entities.DipnetExpedition;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * This repositories provides CRUD operations for {@link DipnetExpedition} objects
 */
@Transactional("dipnetTransactionManager")
public interface DipnetExpeditionRepository extends Repository<DipnetExpedition, Integer> {

    @Modifying
    void delete(DipnetExpedition dipnetExpedition);

    void save(DipnetExpedition dipnetExpedition);

    DipnetExpedition findOneByExpeditionId(int dipnetExpeditionId);
}
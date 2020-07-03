package biocode.fims.repositories;

import biocode.fims.models.SraSubmissionEntry;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * This repositories provides CRUD operations for {@link SraSubmissionEntry} objects
 */
@Transactional
public interface SraSubmissionRepository extends Repository<SraSubmissionEntry, Integer> {

    void save(SraSubmissionEntry submission);

    SraSubmissionEntry findOneById(int id);

    List<SraSubmissionEntry> getByStatus(SraSubmissionEntry.Status status);
}

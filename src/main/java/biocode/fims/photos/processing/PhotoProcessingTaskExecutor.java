package biocode.fims.photos.processing;

import biocode.fims.photos.PhotoRecord;
import biocode.fims.repositories.RecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * @author rjewing
 */
public class PhotoProcessingTaskExecutor {
    private static final Logger logger = LoggerFactory.getLogger(PhotoProcessingTaskExecutor.class);
    private static final Set<PhotoRecord> PHOTOS_IN_PROCESSING = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final RecordRepository recordRepository;
    private ExecutorService executorService;

    public PhotoProcessingTaskExecutor(RecordRepository recordRepository, ExecutorService executorService) {
        this.recordRepository = recordRepository;
        this.executorService = executorService;
    }

    // TODO maybe need to set a timeout? https://geowarin.github.io/completable-futures-with-spring-async.html
    public void addTask(PhotoProcessingTask task) {
        if (PHOTOS_IN_PROCESSING.add(task.record())) {
            logger.info("submitting task for: " + task.record());

            CompletableFuture.runAsync(task, executorService)
                    .whenComplete((v, err) -> {
                        if (err != null) {
                            logger.error(err.getMessage(), err);
                        }

                        try {
                            // we still want to save if we have an error b/c we update
                            // the record with an error message.
                            UnprocessedPhotoRecord record = task.record();
                            recordRepository.saveChildRecord(
                                    record,
                                    record.networkId(),
                                    record.parentEntity(),
                                    record.entity()
                            );
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }

                        // we sleep here for 2 seconds to reduce the chance of processing
                        // the same photo 2x. Unprocessed photos are queried every 10 mins,
                        // and added to this executor. There is a small chance that a photo
                        // will finish processing after the scheduler queries for unprocessed
                        // photos, and be removed from the PHOTOS_IN_PROCESSING
                        // set before the duplicate is added to the executor. By waiting 2 secs
                        // after saving to remove from the set, we should eliminate this issue.
                        try {
                            Thread.sleep(2 * 1000);
                        } catch (InterruptedException ignored) {
                        }
                        PHOTOS_IN_PROCESSING.remove(task.record());
                    });

        } else {
            logger.info("ignoring duplicate task: " + task.record());
        }
    }
}

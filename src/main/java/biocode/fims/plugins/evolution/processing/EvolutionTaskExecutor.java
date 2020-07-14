package biocode.fims.plugins.evolution.processing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

/**
 * @author rjewing
 */
public class EvolutionTaskExecutor {
    private static final Logger logger = LoggerFactory.getLogger(EvolutionTaskExecutor.class);

    private ExecutorService executorService;

    public EvolutionTaskExecutor(ExecutorService executorService) {
        this.executorService = executorService;
    }

    // TODO maybe need to set a timeout? https://geowarin.github.io/completable-futures-with-spring-async.html
    public void addTask(Runnable task) {
//        CompletableFuture.runAsync(task, executorService)
//                .whenComplete((v, err) -> {
//                    if (err != null) {
//                        logger.error(err.getMessage(), err);
//                    }
                    // TODO log the results here?
//                });

    }
}

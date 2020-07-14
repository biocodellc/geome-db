package biocode.fims.photos.processing;

import biocode.fims.photos.PhotoEntityProps;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.concurrent.*;

import static org.mockito.Mockito.*;
import static junit.framework.Assert.assertEquals;

/**
 * @author rjewing
 */
public class PhotoProcessingTaskExecutorTest {

    @Test
    public void duplicate_queuedPhotos_not_registered_with_executor_service() throws NoSuchFieldException, IllegalAccessException {
        ExecutorService executorService = mock(ExecutorService.class);
        PhotoProcessingTaskExecutor executor = new PhotoProcessingTaskExecutor(null, executorService);

        PhotoProcessingTask processingTask = spy(getPhotoProcessingTask());

        executor.addTask(processingTask);
        executor.addTask(processingTask);

        // the Runnable impl that CompletableFuture passes to the executor service
        // is package-private so we can't access it. Instead, we capture the
        // argument that execute is called with. Then using reflection, we can check
        // if the func is what we expect. May be a bit fragile, but will work for now
        ArgumentCaptor<Runnable> argument = ArgumentCaptor.forClass(Runnable.class);
        // execute should only be called 1 time, as we don't process duplicates
        verify(executorService, times(1)).execute(argument.capture());

        Runnable runnable = argument.getValue();
        Field task = runnable.getClass().getDeclaredField("fn");
        task.setAccessible(true);

        assertEquals(processingTask, task.get(runnable));
    }

    private PhotoProcessingTask getPhotoProcessingTask() {
        UnprocessedPhotoRecord photo = new UnprocessedPhotoRecord(new HashMap<>(), null,null, 0, 0, null);
        photo.set(PhotoEntityProps.PROCESSED.uri(), "false");
        photo.set(PhotoEntityProps.PHOTO_ID.uri(), "Photo1");
        return new PhotoProcessingTask(null, photo);
    }

}
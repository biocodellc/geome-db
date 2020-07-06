package biocode.fims.photos.processing;

import biocode.fims.photos.PhotoEntityProps;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class PhotoProcessingTaskTest {

    @Test
    public void initialize_and_run() {
        UnprocessedPhotoRecord record = getPhotoRecord();
        FakePhotoProcessor photoProcessor = new FakePhotoProcessor();
        PhotoProcessingTask processingTask = new PhotoProcessingTask(photoProcessor, record);

        assertEquals(record, processingTask.record());
        assertFalse(photoProcessor.processed);

        processingTask.run();

        assertTrue(photoProcessor.processed);
    }

    private UnprocessedPhotoRecord getPhotoRecord() {
        UnprocessedPhotoRecord photo = new UnprocessedPhotoRecord(new HashMap<>(), null,null, 0, 0, null);
        photo.set(PhotoEntityProps.PROCESSED.uri(), "false");
        photo.set(PhotoEntityProps.PHOTO_ID.uri(), "Photo1");
        photo.set(PhotoEntityProps.ORIGINAL_URL.uri(), "ftp:///photo/location");
        return photo;
    }

}
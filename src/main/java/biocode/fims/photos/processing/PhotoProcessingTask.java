package biocode.fims.photos.processing;

/**
 * @author rjewing
 */
public class PhotoProcessingTask implements Runnable {

    private final PhotoProcessor photoProcessor;
    private final UnprocessedPhotoRecord record;

    PhotoProcessingTask(PhotoProcessor photoProcessor, UnprocessedPhotoRecord record) {
        this.photoProcessor = photoProcessor;
        this.record = record;
    }

    @Override
    public void run() {
        photoProcessor.process(record);
    }

    public UnprocessedPhotoRecord record() {
        return record;
    }
}

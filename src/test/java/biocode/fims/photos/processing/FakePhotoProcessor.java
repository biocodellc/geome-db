package biocode.fims.photos.processing;

/**
 * @author rjewing
 */
public class FakePhotoProcessor implements PhotoProcessor {
    boolean processed = false;

    @Override
    public void process(UnprocessedPhotoRecord record) {
        processed = true;
    }
}

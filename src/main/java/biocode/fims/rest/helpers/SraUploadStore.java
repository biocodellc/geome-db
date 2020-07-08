package biocode.fims.rest.helpers;

import biocode.fims.rest.models.SraUploadMetadata;
import biocode.fims.rest.models.UploadStore;

/**
 * @author rjewing
 */
public class SraUploadStore extends UploadStore<SraUploadMetadata> {
    private final static int CACHE_EXPIRATION = 24 * 60 * 60 * 1000; // 24 hrs

    @Override
    public int getCacheExpiration() {
        return CACHE_EXPIRATION;
    }
}

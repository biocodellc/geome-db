package biocode.fims.application.config;

import org.springframework.core.env.Environment;

/**
 * @author rjewing
 */
public class PhotosProperties {
    private final Environment env;

    public PhotosProperties(Environment env) {
        this.env = env;
    }

    public String photosDir() {
        String dir = env.getRequiredProperty("photosDir");
        if (!dir.endsWith("/")) {
            dir += "/";
        }
        return dir;
    }

    public String photosRoot() {
        String path = env.getRequiredProperty("photosRoot");

        if (!path.endsWith("/")) {
            path += "/";
        }

        return path;
    }
}

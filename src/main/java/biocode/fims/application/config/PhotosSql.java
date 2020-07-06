package biocode.fims.application.config;

import org.springframework.util.Assert;

import java.util.Properties;

public class PhotosSql {
    private final Properties sql;

    public PhotosSql(Properties sql) {
        this.sql = sql;
    }

    public String getRecords() {
        return get("getRecords");
    }

    public String unprocessedPhotos() {
        return get("unprocessedPhotos");
    }

    private String get(String prop) {
        String result = sql.getProperty(prop);
        Assert.notNull(result, "Missing " + prop + " sql.");
        return result;
    }
}
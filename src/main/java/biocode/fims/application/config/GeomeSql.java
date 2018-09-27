package biocode.fims.application.config;

import org.springframework.util.Assert;

import java.util.Properties;

/**
 * @author rjewing
 */
public class GeomeSql {
    private final Properties sql;

    public GeomeSql(Properties sql) {
        this.sql = sql;
    }

    public String expeditionStats() {
        return get("expeditionStats");
    }

    public String singleExpeditionStats() {
        return get("singleExpeditionStats");
    }

    public String expeditionStatsEntityCounts() {
        return get("expeditionStatsEntityCounts");
    }

    public String expeditionStatsEntityJoins() {
        return get("expeditionStatsEntityJoins");
    }

    private String get(String prop) {
        String result = sql.getProperty(prop);
        Assert.notNull(result, "Missing " + prop + " sql.");
        return result;
    }
}

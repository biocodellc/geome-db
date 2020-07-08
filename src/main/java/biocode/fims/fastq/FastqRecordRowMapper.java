package biocode.fims.fastq;

import biocode.fims.models.dataTypes.JacksonUtil;
import biocode.fims.records.FimsRowMapper;
import biocode.fims.ncbi.models.BioSample;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static biocode.fims.query.QueryConstants.*;

/**
 * @author rjewing
 */
public class FastqRecordRowMapper implements FimsRowMapper<FastqRecord> {
    private final static JavaType TYPE;

    static {
        TYPE = TypeFactory.defaultInstance().constructMapType(HashMap.class, String.class, Object.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public FastqRecord mapRow(ResultSet rs, int rowNum, String labelPrefix) throws SQLException {
        String data = rs.getString(labelPrefix + DATA);
        if (data == null) return null;

        String rootIdentifier = rs.getString(labelPrefix + ROOT_IDENTIFIER);
        String expeditionCode = rs.getString(EXPEDITION_CODE.toString());
        int projectId = rs.getInt(PROJECT_ID.toString());

        try {
            Map<String, Object> properties = (Map<String, Object>) JacksonUtil.fromString(data, TYPE);
            BioSample bioSample = null;
            if (properties.get(FastqProps.BIOSAMPLE.uri()) != null) {
                bioSample = JacksonUtil.fromMap((Map<String, ?>) properties.remove(FastqProps.BIOSAMPLE.uri()), BioSample.class);
            }

            List<String> filenames = (List<String>) properties.remove("filenames");

            FastqRecord r = new FastqRecord(properties, filenames, rootIdentifier, projectId, expeditionCode, false);
            if (bioSample != null)
                r.setBioSample(bioSample);

            return r;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    @Override
    public FastqRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return mapRow(rs, rowNum, "");
    }
}

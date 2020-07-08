package biocode.fims.fastq;

import biocode.fims.query.PostgresUtils;
import biocode.fims.records.GenericRecordRowMapper;
import biocode.fims.records.Record;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @author rjewing
 */
@Transactional
public class PostgresFastqRepository implements FastqRepository {
    private final static Logger logger = LoggerFactory.getLogger(PostgresFastqRepository.class);
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Properties sql;

    public PostgresFastqRepository(NamedParameterJdbcTemplate jdbcTemplate, Properties sql) {
        this.jdbcTemplate = jdbcTemplate;
        this.sql = sql;
    }

    @Override
    public List<Record> getRecords(int networkId, int projectId, String conceptAlias, List<String> parentIdentifiers) {
        Map<String, Object> tableMap = PostgresUtils.getTableMap(networkId, conceptAlias);

        Map<String, Object> sqlParams = new HashMap<>();
        sqlParams.put("projectId", projectId);
        sqlParams.put("parentIdentifiers", parentIdentifiers);

        return new ArrayList<>(jdbcTemplate.query(
                StringSubstitutor.replace(sql.getProperty("getRecords"), tableMap),
                sqlParams,
                new GenericRecordRowMapper()
        ));


    }
}

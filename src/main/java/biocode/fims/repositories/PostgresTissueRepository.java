package biocode.fims.repositories;

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
public class PostgresTissueRepository implements TissueRepository {
    private final static Logger logger = LoggerFactory.getLogger(PostgresTissueRepository.class);
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Properties sql;

    public PostgresTissueRepository(NamedParameterJdbcTemplate jdbcTemplate, Properties sql) {
        this.jdbcTemplate = jdbcTemplate;
        this.sql = sql;
    }

    @Override
    public List<String> getPlates(int networkId, int projectId, String conceptAlias, String plateColumn) {
        Map<String, Object> tableMap = PostgresUtils.getTableMap(networkId, conceptAlias);
        tableMap.put("plateColumn", plateColumn);

        Map<String, Object> sqlParams = new HashMap<>();
        sqlParams.put("projectId", projectId);

        return jdbcTemplate.queryForList(
                StringSubstitutor.replace(sql.getProperty("getPlates"), tableMap),
                sqlParams,
                String.class
        );
    }

    @Override
    public List<Record> getTissues(int networkId, int projectId, String conceptAlias, List<String> parentIdentifiers) {
        Map<String, Object> tableMap = PostgresUtils.getTableMap(networkId, conceptAlias);

        Map<String, Object> sqlParams = new HashMap<>();
        sqlParams.put("projectId", projectId);
        sqlParams.put("parentIdentifiers", parentIdentifiers);

        return new ArrayList<>(jdbcTemplate.query(
                StringSubstitutor.replace(sql.getProperty("getTissues"), tableMap),
                sqlParams,
                new GenericRecordRowMapper()
        ));


    }
}

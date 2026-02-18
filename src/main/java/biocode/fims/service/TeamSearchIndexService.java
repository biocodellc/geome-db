package biocode.fims.service;

import biocode.fims.application.config.TeamIndexProperties;
import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.Entity;
import biocode.fims.models.ProjectConfiguration;
import biocode.fims.query.PostgresUtils;
import biocode.fims.rest.FimsObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Pushes team data (all projects under a project configuration) to Elasticsearch.
 * Indexed fields are derived from the team's own project configuration entities/attributes.
 */
@Service
public class TeamSearchIndexService {
    private static final Logger logger = LoggerFactory.getLogger(TeamSearchIndexService.class);
    private static final int NO_TEAM_RUNNING = -1;
    private static final Pattern SAFE_IDENT = Pattern.compile("[A-Za-z0-9_]+");

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TeamIndexProperties props;
    private final FimsObjectMapper objectMapper;
    private final ProjectConfigurationService projectConfigurationService;
    private final ExecutorService executorService;
    private final AtomicInteger runningTeamId;

    @Autowired
    public TeamSearchIndexService(NamedParameterJdbcTemplate jdbcTemplate,
                                  TeamIndexProperties props,
                                  FimsObjectMapper objectMapper,
                                  ProjectConfigurationService projectConfigurationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.props = props;
        this.objectMapper = objectMapper;
        this.projectConfigurationService = projectConfigurationService;
        this.executorService = Executors.newSingleThreadExecutor();
        this.runningTeamId = new AtomicInteger(NO_TEAM_RUNNING);
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
    }

    public TeamIndexStatus triggerIndex(int teamId, int triggerUserId) {
        ensureTables();

        if (!props.enabled()) {
            TeamIndexStatus status = getStatus(teamId);
            status.setMessage("teamIndex.enabled=false. Enable team indexing in properties before triggering a run.");
            return status;
        }

        synchronized (this) {
            int activeTeam = runningTeamId.get();
            if (activeTeam != NO_TEAM_RUNNING) {
                TeamIndexStatus status = getStatus(teamId);
                status.setMessage("A team index run is already in progress for teamId=" + activeTeam);
                status.setCurrentlyRunningTeamId(activeTeam);
                return status;
            }

            runningTeamId.set(teamId);
            markRunStarted(teamId, triggerUserId);
            executorService.submit(() -> runIndex(teamId, triggerUserId));
        }

        TeamIndexStatus status = getStatus(teamId);
        status.setMessage("Team index run started");
        status.setCurrentlyRunningTeamId(teamId);
        return status;
    }

    public TeamIndexStatus getStatus(int teamId) {
        ensureTables();

        TeamIndexStatus status = queryStatus(teamId);
        if (status == null) {
            status = TeamIndexStatus.neverRun(teamId);
        }

        int activeTeam = runningTeamId.get();
        if (activeTeam != NO_TEAM_RUNNING) {
            status.setCurrentlyRunningTeamId(activeTeam);
            if (activeTeam == teamId) {
                status.setRunning(true);
            }
        }

        return status;
    }

    private void runIndex(int teamId, int triggerUserId) {
        int checkedCount = 0;
        int indexedCount = 0;
        int deletedCount = 0;
        String lastStatus = "SUCCESS";
        String lastError = null;

        try {
            validateRequiredProperties();

            TeamDefinition team = loadTeamDefinition(teamId);
            Map<String, IndexedDocument> currentDocsById = new LinkedHashMap<>();

            for (EntityDefinition entityDef : team.entities) {
                if (!tableExists(team.networkId, entityDef.conceptAlias)) {
                    continue;
                }

                List<Map<String, Object>> rows = loadEntityRows(teamId, team.networkId, entityDef.conceptAlias);
                checkedCount += rows.size();

                for (Map<String, Object> row : rows) {
                    IndexedDocument doc = toIndexedDocument(teamId, entityDef, row);
                    currentDocsById.put(doc.id, doc);
                }
            }

            Map<String, String> existingState = loadExistingState(teamId);
            Set<String> staleIds = new LinkedHashSet<>(existingState.keySet());
            staleIds.removeAll(currentDocsById.keySet());

            List<IndexedDocument> changedDocs = new ArrayList<>();
            for (IndexedDocument doc : currentDocsById.values()) {
                String existingHash = existingState.get(doc.id);
                if (!doc.hash.equals(existingHash)) {
                    changedDocs.add(doc);
                }
            }

            if (!changedDocs.isEmpty() || !staleIds.isEmpty()) {
                sendBulkChanges(props.elasticsearchIndex().trim(), changedDocs, staleIds);
                upsertState(teamId, changedDocs);
                deleteState(teamId, staleIds);
            }

            indexedCount = changedDocs.size();
            deletedCount = staleIds.size();
        } catch (Exception e) {
            lastStatus = "FAILED";
            lastError = e.getMessage();
            logger.error("Team index run failed for teamId={}", teamId, e);
        } finally {
            markRunFinished(teamId, checkedCount, indexedCount, deletedCount, lastStatus, lastError, triggerUserId);
            runningTeamId.compareAndSet(teamId, NO_TEAM_RUNNING);
        }
    }

    private TeamDefinition loadTeamDefinition(int teamId) {
        ProjectConfiguration configuration = projectConfigurationService.getProjectConfiguration(teamId);
        if (configuration == null) {
            throw new IllegalStateException("Invalid teamId/config id: " + teamId);
        }

        TeamDefinition team = new TeamDefinition();
        team.teamId = teamId;
        team.networkId = configuration.getNetwork().getId();

        for (Entity entity : configuration.getProjectConfig().entities()) {
            if (!SAFE_IDENT.matcher(entity.getConceptAlias()).matches()) {
                logger.warn("Skipping entity with unsafe conceptAlias={} for teamId={}", entity.getConceptAlias(), teamId);
                continue;
            }

            EntityDefinition def = new EntityDefinition();
            def.conceptAlias = entity.getConceptAlias();
            for (Attribute attr : entity.getAttributes()) {
                if (attr.getColumn() == null || attr.getColumn().trim().isEmpty()) {
                    continue;
                }
                FieldDefinition f = new FieldDefinition();
                f.column = attr.getColumn();
                def.fields.add(f);
            }
            team.entities.add(def);
        }

        return team;
    }

    private boolean tableExists(int networkId, String conceptAlias) {
        String tableName = PostgresUtils.entityTable(networkId, conceptAlias);
        String sql = "SELECT to_regclass(:tableName) IS NOT NULL";
        Boolean exists = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("tableName", tableName), Boolean.class);
        return exists != null && exists;
    }

    private List<Map<String, Object>> loadEntityRows(int teamId, int networkId, String conceptAlias) {
        String tableName = PostgresUtils.entityTable(networkId, conceptAlias);
        String sql = "SELECT r.local_identifier as \"localIdentifier\", " +
                "r.parent_identifier as \"parentIdentifier\", " +
                "r.data as \"data\", " +
                "e.id as \"expeditionId\", " +
                "e.expedition_code as \"expeditionCode\", " +
                "e.project_id as \"projectId\" " +
                "FROM " + tableName + " AS r " +
                "JOIN expeditions e ON e.id = r.expedition_id " +
                "JOIN projects p ON p.id = e.project_id " +
                "WHERE p.config_id = :teamId";

        return jdbcTemplate.queryForList(sql, new MapSqlParameterSource("teamId", teamId));
    }

    @SuppressWarnings("unchecked")
    private IndexedDocument toIndexedDocument(int teamId, EntityDefinition entityDef, Map<String, Object> row) throws Exception {
        String projectId = stringValue(row.get("projectId"));
        String expeditionId = stringValue(row.get("expeditionId"));
        String expeditionCode = stringValue(row.get("expeditionCode"));
        String localIdentifier = stringValue(row.get("localIdentifier"));
        String parentIdentifier = stringValue(row.get("parentIdentifier"));

        Map<String, Object> dataMap = parseJsonMap(row.get("data"));
        Map<String, Object> fields = new LinkedHashMap<>();
        for (FieldDefinition field : entityDef.fields) {
            if (dataMap.containsKey(field.column)) {
                fields.put(field.column, dataMap.get(field.column));
            }
        }

        String stableLocalId = localIdentifier;
        if (stableLocalId == null || stableLocalId.isEmpty()) {
            stableLocalId = hashDocument(dataMap);
        }

        String docId = "team-" + teamId +
                "-project-" + nvl(projectId) +
                "-expedition-" + nvl(expeditionId) +
                "-entity-" + entityDef.conceptAlias +
                "-record-" + stableLocalId;

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("teamId", teamId);
        doc.put("entity", entityDef.conceptAlias);
        doc.put("projectId", parseInt(projectId));
        doc.put("expeditionId", parseInt(expeditionId));
        doc.put("expeditionCode", expeditionCode);
        doc.put("localIdentifier", localIdentifier);
        doc.put("parentIdentifier", parentIdentifier);
        doc.put("fields", fields);

        String hash = hashDocument(doc);
        return new IndexedDocument(docId, hash, doc);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonMap(Object rawJson) throws Exception {
        if (rawJson == null) {
            return Collections.emptyMap();
        }
        if (rawJson instanceof Map) {
            return (Map<String, Object>) rawJson;
        }
        return objectMapper.readValue(String.valueOf(rawJson), Map.class);
    }

    private String nvl(String val) {
        return val == null ? "null" : val;
    }

    private Integer parseInt(String val) {
        if (val == null || val.isEmpty()) {
            return null;
        }
        return Integer.parseInt(val);
    }

    private String stringValue(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private String hashDocument(Object document) throws Exception {
        byte[] data = objectMapper.writeValueAsBytes(document);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void sendBulkChanges(String indexName, List<IndexedDocument> changedDocs, Set<String> staleIds) throws Exception {
        List<BulkOperation> operations = new ArrayList<>();
        for (IndexedDocument doc : changedDocs) {
            operations.add(BulkOperation.index(indexName, doc.id, doc.document));
        }
        for (String staleId : staleIds) {
            operations.add(BulkOperation.delete(indexName, staleId));
        }

        if (operations.isEmpty()) {
            return;
        }

        int batchSize = Math.max(1, props.bulkBatchSize());
        String bulkEndpoint = buildBulkEndpoint(indexName);

        Client client = ClientBuilder.newClient();
        try {
            for (int i = 0; i < operations.size(); i += batchSize) {
                List<BulkOperation> batch = operations.subList(i, Math.min(i + batchSize, operations.size()));
                String payload = toNdjson(batch);

                javax.ws.rs.client.Invocation.Builder request = client.target(bulkEndpoint).request(MediaType.APPLICATION_JSON_TYPE);
                request.header("Content-Type", "application/x-ndjson");
                addAuthHeader(request);

                Response response = request.post(javax.ws.rs.client.Entity.entity(payload, "application/x-ndjson"));
                String body = response.hasEntity() ? response.readEntity(String.class) : "";

                if (response.getStatus() >= 300) {
                    throw new IllegalStateException("Elasticsearch bulk request failed with status " + response.getStatus() + ": " + body);
                }

                if (body != null && !body.isEmpty()) {
                    JsonNode json = objectMapper.readTree(body);
                    if (json.path("errors").asBoolean(false)) {
                        throw new IllegalStateException("Elasticsearch bulk response contains item errors");
                    }
                }
            }
        } finally {
            try {
                client.close();
            } catch (Exception ignored) {
            }
        }
    }

    private String buildBulkEndpoint(String indexName) {
        String base = props.elasticsearchBaseUrl().trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + indexName + "/_bulk";
    }

    private String toNdjson(List<BulkOperation> operations) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (BulkOperation op : operations) {
            sb.append(objectMapper.writeValueAsString(op.actionMeta)).append('\n');
            if (op.document != null) {
                sb.append(objectMapper.writeValueAsString(op.document)).append('\n');
            }
        }
        return sb.toString();
    }

    private void addAuthHeader(javax.ws.rs.client.Invocation.Builder request) {
        if (notBlank(props.apiKey())) {
            request.header("Authorization", "ApiKey " + props.apiKey().trim());
            return;
        }
        if (notBlank(props.basicUsername()) && notBlank(props.basicPassword())) {
            String raw = props.basicUsername().trim() + ":" + props.basicPassword();
            String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
            request.header("Authorization", "Basic " + encoded);
        }
    }

    private Map<String, String> loadExistingState(int teamId) {
        String sql = "SELECT doc_id as \"docId\", doc_hash as \"docHash\" FROM team_index_state WHERE team_id = :teamId";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, new MapSqlParameterSource("teamId", teamId));

        Map<String, String> existing = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            existing.put(String.valueOf(row.get("docId")), String.valueOf(row.get("docHash")));
        }
        return existing;
    }

    private void upsertState(int teamId, List<IndexedDocument> changedDocs) {
        if (changedDocs.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO team_index_state (team_id, doc_id, doc_hash, updated_at) " +
                "VALUES (:teamId, :docId, :docHash, now()) " +
                "ON CONFLICT (team_id, doc_id) DO UPDATE SET doc_hash = excluded.doc_hash, updated_at = now()";

        MapSqlParameterSource[] params = new MapSqlParameterSource[changedDocs.size()];
        for (int i = 0; i < changedDocs.size(); i++) {
            IndexedDocument doc = changedDocs.get(i);
            params[i] = new MapSqlParameterSource()
                    .addValue("teamId", teamId)
                    .addValue("docId", doc.id)
                    .addValue("docHash", doc.hash);
        }
        jdbcTemplate.batchUpdate(sql, params);
    }

    private void deleteState(int teamId, Set<String> staleIds) {
        if (staleIds.isEmpty()) {
            return;
        }
        String sql = "DELETE FROM team_index_state WHERE team_id = :teamId AND doc_id in (:docIds)";
        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("teamId", teamId)
                .addValue("docIds", staleIds));
    }

    private void markRunStarted(int teamId, int triggerUserId) {
        String sql = "INSERT INTO team_index_runs (team_id, running, started_at, finished_at, last_status, last_error, " +
                "checked_count, indexed_count, deleted_count, triggered_by_user_id, modified) " +
                "VALUES (:teamId, true, now(), null, 'RUNNING', null, 0, 0, 0, :userId, now()) " +
                "ON CONFLICT (team_id) DO UPDATE SET running = true, started_at = now(), finished_at = null, " +
                "last_status = 'RUNNING', last_error = null, checked_count = 0, indexed_count = 0, deleted_count = 0, " +
                "triggered_by_user_id = :userId, modified = now()";

        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("teamId", teamId)
                .addValue("userId", triggerUserId));
    }

    private void markRunFinished(int teamId, int checked, int indexed, int deleted, String status, String error, int triggerUserId) {
        String sql = "UPDATE team_index_runs SET running = false, finished_at = now(), last_status = :status, " +
                "last_error = :error, checked_count = :checkedCount, indexed_count = :indexedCount, deleted_count = :deletedCount, " +
                "triggered_by_user_id = :userId, modified = now() WHERE team_id = :teamId";

        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("teamId", teamId)
                .addValue("status", status)
                .addValue("error", error)
                .addValue("checkedCount", checked)
                .addValue("indexedCount", indexed)
                .addValue("deletedCount", deleted)
                .addValue("userId", triggerUserId));
    }

    private TeamIndexStatus queryStatus(int teamId) {
        String sql = "SELECT team_id as \"teamId\", running as \"running\", started_at as \"startedAt\", " +
                "finished_at as \"finishedAt\", last_status as \"lastStatus\", last_error as \"lastError\", " +
                "checked_count as \"checkedCount\", indexed_count as \"indexedCount\", deleted_count as \"deletedCount\", " +
                "triggered_by_user_id as \"triggeredByUserId\" " +
                "FROM team_index_runs WHERE team_id = :teamId";

        try {
            return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("teamId", teamId), (rs, rowNum) -> {
                TeamIndexStatus status = new TeamIndexStatus();
                status.setTeamId(rs.getInt("teamId"));
                status.setRunning(rs.getBoolean("running"));
                status.setStartedAt(toDate(rs.getTimestamp("startedAt")));
                status.setFinishedAt(toDate(rs.getTimestamp("finishedAt")));
                status.setLastStatus(rs.getString("lastStatus"));
                status.setLastError(rs.getString("lastError"));
                status.setCheckedCount(rs.getInt("checkedCount"));
                status.setIndexedCount(rs.getInt("indexedCount"));
                status.setDeletedCount(rs.getInt("deletedCount"));
                int triggerUserId = rs.getInt("triggeredByUserId");
                if (!rs.wasNull()) {
                    status.setTriggeredByUserId(triggerUserId);
                }
                return status;
            });
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private void ensureTables() {
        jdbcTemplate.getJdbcOperations().execute("CREATE TABLE IF NOT EXISTS team_index_state (" +
                "team_id INTEGER NOT NULL, " +
                "doc_id TEXT NOT NULL, " +
                "doc_hash TEXT NOT NULL, " +
                "updated_at TIMESTAMP NOT NULL DEFAULT now(), " +
                "PRIMARY KEY (team_id, doc_id)" +
                ")");

        jdbcTemplate.getJdbcOperations().execute("CREATE TABLE IF NOT EXISTS team_index_runs (" +
                "team_id INTEGER PRIMARY KEY, " +
                "running BOOLEAN NOT NULL DEFAULT false, " +
                "started_at TIMESTAMP NULL, " +
                "finished_at TIMESTAMP NULL, " +
                "last_status VARCHAR(32) NULL, " +
                "last_error TEXT NULL, " +
                "checked_count INTEGER NOT NULL DEFAULT 0, " +
                "indexed_count INTEGER NOT NULL DEFAULT 0, " +
                "deleted_count INTEGER NOT NULL DEFAULT 0, " +
                "triggered_by_user_id INTEGER NULL, " +
                "modified TIMESTAMP NOT NULL DEFAULT now()" +
                ")");
    }

    private void validateRequiredProperties() {
        if (!props.enabled()) {
            throw new IllegalStateException("teamIndex.enabled=false");
        }
        if (!notBlank(props.elasticsearchBaseUrl())) {
            throw new IllegalStateException("Missing required property teamIndex.elasticsearch.baseUrl");
        }
        if (!notBlank(props.elasticsearchIndex())) {
            throw new IllegalStateException("Missing required property teamIndex.elasticsearch.index");
        }
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Date toDate(Timestamp ts) {
        return ts == null ? null : new Date(ts.getTime());
    }

    private static class TeamDefinition {
        private int teamId;
        private int networkId;
        private final List<EntityDefinition> entities = new ArrayList<>();
    }

    private static class EntityDefinition {
        private String conceptAlias;
        private final List<FieldDefinition> fields = new ArrayList<>();
    }

    private static class FieldDefinition {
        private String column;
    }

    private static class IndexedDocument {
        private final String id;
        private final String hash;
        private final Map<String, Object> document;

        private IndexedDocument(String id, String hash, Map<String, Object> document) {
            this.id = id;
            this.hash = hash;
            this.document = document;
        }
    }

    private static class BulkOperation {
        private final Map<String, Object> actionMeta;
        private final Map<String, Object> document;

        private BulkOperation(Map<String, Object> actionMeta, Map<String, Object> document) {
            this.actionMeta = actionMeta;
            this.document = document;
        }

        private static BulkOperation index(String indexName, String id, Map<String, Object> document) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("_index", indexName);
            details.put("_id", id);

            Map<String, Object> action = new LinkedHashMap<>();
            action.put("index", details);
            return new BulkOperation(action, document);
        }

        private static BulkOperation delete(String indexName, String id) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("_index", indexName);
            details.put("_id", id);

            Map<String, Object> action = new LinkedHashMap<>();
            action.put("delete", details);
            return new BulkOperation(action, null);
        }
    }

    public static class TeamIndexStatus {
        private int teamId;
        private boolean running;
        private Integer currentlyRunningTeamId;
        private Date startedAt;
        private Date finishedAt;
        private String lastStatus;
        private String lastError;
        private int checkedCount;
        private int indexedCount;
        private int deletedCount;
        private Integer triggeredByUserId;
        private String message;

        public static TeamIndexStatus neverRun(int teamId) {
            TeamIndexStatus status = new TeamIndexStatus();
            status.teamId = teamId;
            status.running = false;
            status.lastStatus = "NEVER_RUN";
            return status;
        }

        public int getTeamId() {
            return teamId;
        }

        public void setTeamId(int teamId) {
            this.teamId = teamId;
        }

        public boolean isRunning() {
            return running;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }

        public Integer getCurrentlyRunningTeamId() {
            return currentlyRunningTeamId;
        }

        public void setCurrentlyRunningTeamId(Integer currentlyRunningTeamId) {
            this.currentlyRunningTeamId = currentlyRunningTeamId;
        }

        public Date getStartedAt() {
            return startedAt;
        }

        public void setStartedAt(Date startedAt) {
            this.startedAt = startedAt;
        }

        public Date getFinishedAt() {
            return finishedAt;
        }

        public void setFinishedAt(Date finishedAt) {
            this.finishedAt = finishedAt;
        }

        public String getLastStatus() {
            return lastStatus;
        }

        public void setLastStatus(String lastStatus) {
            this.lastStatus = lastStatus;
        }

        public String getLastError() {
            return lastError;
        }

        public void setLastError(String lastError) {
            this.lastError = lastError;
        }

        public int getCheckedCount() {
            return checkedCount;
        }

        public void setCheckedCount(int checkedCount) {
            this.checkedCount = checkedCount;
        }

        public int getIndexedCount() {
            return indexedCount;
        }

        public void setIndexedCount(int indexedCount) {
            this.indexedCount = indexedCount;
        }

        public int getDeletedCount() {
            return deletedCount;
        }

        public void setDeletedCount(int deletedCount) {
            this.deletedCount = deletedCount;
        }

        public Integer getTriggeredByUserId() {
            return triggeredByUserId;
        }

        public void setTriggeredByUserId(Integer triggeredByUserId) {
            this.triggeredByUserId = triggeredByUserId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}

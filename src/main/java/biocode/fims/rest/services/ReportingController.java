package biocode.fims.rest.services;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.application.config.GeomeProperties;
import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.Entity;
import biocode.fims.config.network.NetworkConfig;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.models.Network;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.rest.FimsController;
import biocode.fims.service.NetworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Controller;

import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Controller
@Path("reports")
@Produces({MediaType.APPLICATION_JSON})
@Singleton
public class ReportingController extends FimsController {
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[A-Za-z][A-Za-z0-9_]*$");

    private static final String METRIC_EVENTS = "events";
    private static final String METRIC_SAMPLES = "samples";
    private static final String METRIC_DIAGNOSTICS = "diagnostics";
    private static final String METRIC_EVENT_PHOTOS = "eventPhotos";
    private static final String METRIC_SAMPLE_PHOTOS = "samplePhotos";
    private static final String METRIC_EXTRACTIONS = "extractions";
    private static final String METRIC_FASTQ = "fastq";
    private static final String METRIC_FASTA = "fasta";

    private static final LinkedHashMap<String, List<String>> METRIC_ENTITY_CANDIDATES = new LinkedHashMap<>();

    static {
        METRIC_ENTITY_CANDIDATES.put(METRIC_EVENTS, Collections.singletonList("Event"));
        METRIC_ENTITY_CANDIDATES.put(METRIC_SAMPLES, Collections.singletonList("Sample"));
        METRIC_ENTITY_CANDIDATES.put(METRIC_DIAGNOSTICS, Collections.singletonList("Diagnostics"));
        METRIC_ENTITY_CANDIDATES.put(METRIC_EVENT_PHOTOS, Arrays.asList("Event_Photo", "EventPhoto"));
        METRIC_ENTITY_CANDIDATES.put(METRIC_SAMPLE_PHOTOS, Arrays.asList("Sample_Photo", "SamplePhoto"));
        METRIC_ENTITY_CANDIDATES.put(METRIC_EXTRACTIONS, Arrays.asList("Extraction", "Tissue"));
        METRIC_ENTITY_CANDIDATES.put(METRIC_FASTQ, Arrays.asList("fastqMetadata", "FastqMetadata", "Fastq"));
        METRIC_ENTITY_CANDIDATES.put(METRIC_FASTA, Arrays.asList("fastaSequence", "FastaSequence", "Fasta"));
    }

    private final RecordRepository recordRepository;
    private final NetworkService networkService;
    private final GeomeProperties geomeProps;

    @Autowired
    ReportingController(FimsProperties props, RecordRepository recordRepository, NetworkService networkService, GeomeProperties geomeProps) {
        super(props);
        this.recordRepository = recordRepository;
        this.networkService = networkService;
        this.geomeProps = geomeProps;
    }

    @GET
    @Path("/summary")
    public Map<String, Object> reportingSummary(
            @QueryParam("includePublic") @DefaultValue("true") Boolean includePublic,
            @QueryParam("teamId") Integer teamId,
            @QueryParam("projectId") Integer projectId,
            @QueryParam("topUsersLimit") @DefaultValue("25") Integer topUsersLimit,
            @QueryParam("fieldLimit") Integer fieldLimit
    ) {
        Network network = networkService.getNetwork(geomeProps.networkId());
        if (network == null) {
            throw new BadRequestException("Network doesn't exist");
        }

        ReportFilters filters = buildFilters(includePublic, teamId, projectId, topUsersLimit, fieldLimit);
        Integer userId = userContext.getUser() == null ? null : userContext.getUser().getUserId();

        ParsedEntities parsedEntities = parseNetworkEntitiesAndFields(network.getNetworkConfig());
        Set<String> existingTables = loadExistingEntityTables(network.getId());
        List<String> availableEntities = parsedEntities.entities.stream()
                .filter(entity -> existingTables.contains(entity.toLowerCase(Locale.US)))
                .collect(Collectors.toList());

        LinkedHashMap<String, String> metricEntities = resolveMetricEntities(availableEntities);

        List<Map<String, Object>> projectSummary = loadProjectSummary(network.getId(), metricEntities, filters, userId);
        List<Map<String, Object>> fieldUsage = loadFieldUsage(network.getId(), availableEntities, parsedEntities.fieldNamesByEntityAndUri, filters, userId);
        List<Map<String, Object>> topUsers = loadTopUsers(network.getId(), metricEntities, filters, userId);

        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        response.put("generatedAt", Instant.now().toString());
        response.put("networkId", network.getId());
        response.put("filters", filters.toMap());
        response.put("metricEntities", metricEntities);
        response.put("projectSummary", projectSummary);
        response.put("fieldUsage", fieldUsage);
        response.put("topUsers", topUsers);

        return response;
    }

    private ReportFilters buildFilters(Boolean includePublic, Integer teamId, Integer projectId, Integer topUsersLimit, Integer fieldLimit) {
        boolean includePublicFlag = includePublic == null || includePublic;
        if (userContext.getUser() == null && !includePublicFlag) {
            includePublicFlag = true;
        }

        Integer normalizedTeamId = positiveOrNull(teamId);
        Integer normalizedProjectId = positiveOrNull(projectId);
        Integer normalizedFieldLimit = positiveOrNull(fieldLimit);

        int limit = topUsersLimit == null || topUsersLimit < 1 ? 25 : topUsersLimit;
        limit = Math.min(limit, 500);

        return new ReportFilters(includePublicFlag, normalizedTeamId, normalizedProjectId, limit, normalizedFieldLimit);
    }

    private Integer positiveOrNull(Integer value) {
        if (value == null || value < 1) {
            return null;
        }
        return value;
    }

    private ParsedEntities parseNetworkEntitiesAndFields(NetworkConfig config) {
        if (config == null || config.entities() == null) {
            return new ParsedEntities(Collections.emptyList(), Collections.emptyMap());
        }

        LinkedHashSet<String> entities = new LinkedHashSet<>();
        Map<String, String> fieldNamesByEntityAndUri = new HashMap<>();

        for (Entity entity : config.entities()) {
            if (entity == null || entity.getConceptAlias() == null) {
                continue;
            }

            String alias = entity.getConceptAlias();
            if (!SAFE_IDENTIFIER.matcher(alias).matches()) {
                continue;
            }

            entities.add(alias);
            for (Attribute attribute : entity.getAttributes()) {
                if (attribute == null || attribute.getUri() == null || attribute.getColumn() == null) {
                    continue;
                }
                String key = alias.toLowerCase(Locale.US) + "|" + attribute.getUri();
                fieldNamesByEntityAndUri.put(key, attribute.getColumn());
            }
        }

        return new ParsedEntities(new ArrayList<>(entities), fieldNamesByEntityAndUri);
    }

    private Set<String> loadExistingEntityTables(int networkId) {
        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = :schema AND table_type = 'BASE TABLE'";
        MapSqlParameterSource params = new MapSqlParameterSource("schema", "network_" + networkId);

        List<String> rows = recordRepository.query(
                sql,
                params,
                (rs, rowNum) -> rs.getString("table_name").toLowerCase(Locale.US)
        );

        return new HashSet<>(rows);
    }

    private LinkedHashMap<String, String> resolveMetricEntities(List<String> availableEntities) {
        Map<String, String> availableByLower = new HashMap<>();
        for (String entity : availableEntities) {
            availableByLower.put(entity.toLowerCase(Locale.US), entity);
        }

        LinkedHashMap<String, String> mapping = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : METRIC_ENTITY_CANDIDATES.entrySet()) {
            String matched = null;
            for (String candidate : entry.getValue()) {
                matched = availableByLower.get(candidate.toLowerCase(Locale.US));
                if (matched != null) {
                    break;
                }
            }
            mapping.put(entry.getKey(), matched);
        }

        return mapping;
    }

    private List<Map<String, Object>> loadProjectSummary(int networkId, LinkedHashMap<String, String> mapping, ReportFilters filters, Integer userId) {
        VisibilitySql visibility = buildVisibleProjectsCte(filters, userId);
        MetricSqlParts metricSql = metricProjectSqlParts(mapping, networkId);

        String sql =
                visibility.cteSql +
                "SELECT " +
                "  p.id AS \"projectId\", " +
                "  p.project_title AS \"projectTitle\", " +
                "  pc.id AS \"teamId\", " +
                "  to_jsonb(pc)->>'name' AS \"teamName\", " +
                "  tu.id AS \"teamOwnerUserId\", " +
                "  tu.username AS \"teamOwnerUsername\", " +
                "  tu.email AS \"teamOwnerEmail\", " +
                   String.join(", ", metricSql.selects) + " " +
                "FROM visible_projects vp " +
                "JOIN projects p ON p.id = vp.id " +
                "JOIN project_configurations pc ON pc.id = p.config_id " +
                "LEFT JOIN users tu ON tu.id = pc.user_id " +
                   String.join(" ", metricSql.joins) + " " +
                "ORDER BY lower(p.project_title)";

        return recordRepository.query(
                sql,
                visibility.params,
                (rs, rowNum) -> {
                    LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                    row.put("projectId", rs.getInt("projectId"));
                    row.put("projectTitle", rs.getString("projectTitle"));

                    LinkedHashMap<String, Object> team = new LinkedHashMap<>();
                    team.put("teamId", rs.getInt("teamId"));
                    team.put("teamName", rs.getString("teamName"));
                    team.put("ownerUserId", getNullableInteger(rs, "teamOwnerUserId"));
                    team.put("ownerUsername", rs.getString("teamOwnerUsername"));
                    team.put("ownerEmail", rs.getString("teamOwnerEmail"));
                    row.put("team", team);

                    LinkedHashMap<String, Object> counts = new LinkedHashMap<>();
                    counts.put("events", rs.getLong("eventsCount"));
                    counts.put("samples", rs.getLong("samplesCount"));
                    counts.put("diagnostics", rs.getLong("diagnosticsCount"));
                    counts.put("eventPhotos", rs.getLong("eventPhotosCount"));
                    counts.put("samplePhotos", rs.getLong("samplePhotosCount"));
                    counts.put("extractions", rs.getLong("extractionsCount"));
                    counts.put("fastq", rs.getLong("fastqCount"));
                    counts.put("fasta", rs.getLong("fastaCount"));
                    row.put("counts", counts);

                    return row;
                }
        );
    }

    private List<Map<String, Object>> loadFieldUsage(
            int networkId,
            List<String> entities,
            Map<String, String> fieldNamesByEntityAndUri,
            ReportFilters filters,
            Integer userId
    ) {
        if (entities.isEmpty()) {
            return Collections.emptyList();
        }

        VisibilitySql visibility = buildVisibleProjectsCte(filters, userId);
        MapSqlParameterSource params = copyParams(visibility.params);

        List<String> subQueries = new ArrayList<>();
        for (String entity : entities) {
            subQueries.add(
                    "SELECT " +
                    "  '" + entity + "'::text AS \"entityAlias\", " +
                    "  kv.key AS \"fieldUri\", " +
                    "  COUNT(*)::bigint AS \"filledCount\" " +
                    "FROM network_" + networkId + "." + entity + " r " +
                    "JOIN expeditions e ON e.id = r.expedition_id " +
                    "JOIN visible_projects vp ON vp.id = e.project_id " +
                    "CROSS JOIN LATERAL jsonb_each_text(r.data) kv " +
                    "WHERE kv.value IS NOT NULL AND btrim(kv.value) <> '' " +
                    "GROUP BY kv.key"
            );
        }

        String limitSql = "";
        if (filters.fieldLimit != null) {
            params.addValue("fieldLimit", filters.fieldLimit);
            limitSql = " LIMIT :fieldLimit";
        }

        String sql =
                visibility.cteSql +
                "SELECT fu.\"entityAlias\", fu.\"fieldUri\", fu.\"filledCount\" " +
                "FROM (" + String.join(" UNION ALL ", subQueries) + ") fu " +
                "ORDER BY fu.\"filledCount\" DESC, fu.\"entityAlias\", fu.\"fieldUri\"" +
                limitSql;

        return recordRepository.query(
                sql,
                params,
                (rs, rowNum) -> {
                    String entity = rs.getString("entityAlias");
                    String uri = rs.getString("fieldUri");
                    String key = entity.toLowerCase(Locale.US) + "|" + uri;

                    LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                    row.put("entity", entity);
                    row.put("fieldUri", uri);
                    row.put("fieldName", fieldNamesByEntityAndUri.get(key));
                    row.put("filledCount", rs.getLong("filledCount"));
                    return row;
                }
        );
    }

    private List<Map<String, Object>> loadTopUsers(int networkId, LinkedHashMap<String, String> mapping, ReportFilters filters, Integer userId) {
        List<String> unionParts = mapping.values().stream()
                .filter(entity -> entity != null && SAFE_IDENTIFIER.matcher(entity).matches())
                .map(entity ->
                        "SELECT e.user_id AS user_id " +
                        "FROM network_" + networkId + "." + entity + " r " +
                        "JOIN expeditions e ON e.id = r.expedition_id " +
                        "JOIN visible_projects vp ON vp.id = e.project_id"
                )
                .collect(Collectors.toList());

        if (unionParts.isEmpty()) {
            return Collections.emptyList();
        }

        VisibilitySql visibility = buildVisibleProjectsCte(filters, userId);
        MetricSqlParts metricSql = metricUserSqlParts(mapping, networkId);
        MapSqlParameterSource params = copyParams(visibility.params);
        params.addValue("topUsersLimit", filters.topUsersLimit);

        String sql =
                visibility.cteSql +
                ", user_totals AS ( " +
                "  SELECT user_id, COUNT(*)::bigint AS total_count " +
                "  FROM (" + String.join(" UNION ALL ", unionParts) + ") x " +
                "  GROUP BY user_id " +
                ") " +
                "SELECT " +
                "  u.id AS \"userId\", " +
                "  u.username AS \"username\", " +
                "  u.email AS \"email\", " +
                "  u.first_name AS \"firstName\", " +
                "  u.last_name AS \"lastName\", " +
                "  u.institution AS \"institution\", " +
                   String.join(", ", metricSql.selects) + ", " +
                "  ut.total_count AS \"totalCount\" " +
                "FROM user_totals ut " +
                "JOIN users u ON u.id = ut.user_id " +
                   String.join(" ", metricSql.joins) + " " +
                "ORDER BY ut.total_count DESC, lower(u.username) " +
                "LIMIT :topUsersLimit";

        return recordRepository.query(
                sql,
                params,
                (rs, rowNum) -> {
                    LinkedHashMap<String, Object> row = new LinkedHashMap<>();

                    LinkedHashMap<String, Object> user = new LinkedHashMap<>();
                    user.put("userId", rs.getInt("userId"));
                    user.put("username", rs.getString("username"));
                    user.put("email", rs.getString("email"));
                    user.put("firstName", rs.getString("firstName"));
                    user.put("lastName", rs.getString("lastName"));
                    user.put("institution", rs.getString("institution"));
                    row.put("user", user);

                    LinkedHashMap<String, Object> counts = new LinkedHashMap<>();
                    counts.put("events", rs.getLong("eventsCount"));
                    counts.put("samples", rs.getLong("samplesCount"));
                    counts.put("diagnostics", rs.getLong("diagnosticsCount"));
                    counts.put("eventPhotos", rs.getLong("eventPhotosCount"));
                    counts.put("samplePhotos", rs.getLong("samplePhotosCount"));
                    counts.put("extractions", rs.getLong("extractionsCount"));
                    counts.put("fastq", rs.getLong("fastqCount"));
                    counts.put("fasta", rs.getLong("fastaCount"));
                    counts.put("total", rs.getLong("totalCount"));
                    row.put("counts", counts);

                    return row;
                }
        );
    }

    private MetricSqlParts metricProjectSqlParts(LinkedHashMap<String, String> mapping, int networkId) {
        List<String> selects = new ArrayList<>();
        List<String> joins = new ArrayList<>();

        addMetricProject(mapping, networkId, selects, joins, METRIC_EVENTS, "eventsCount", "events_ct");
        addMetricProject(mapping, networkId, selects, joins, METRIC_SAMPLES, "samplesCount", "samples_ct");
        addMetricProject(mapping, networkId, selects, joins, METRIC_DIAGNOSTICS, "diagnosticsCount", "diagnostics_ct");
        addMetricProject(mapping, networkId, selects, joins, METRIC_EVENT_PHOTOS, "eventPhotosCount", "event_photos_ct");
        addMetricProject(mapping, networkId, selects, joins, METRIC_SAMPLE_PHOTOS, "samplePhotosCount", "sample_photos_ct");
        addMetricProject(mapping, networkId, selects, joins, METRIC_EXTRACTIONS, "extractionsCount", "extractions_ct");
        addMetricProject(mapping, networkId, selects, joins, METRIC_FASTQ, "fastqCount", "fastq_ct");
        addMetricProject(mapping, networkId, selects, joins, METRIC_FASTA, "fastaCount", "fasta_ct");

        return new MetricSqlParts(selects, joins);
    }

    private MetricSqlParts metricUserSqlParts(LinkedHashMap<String, String> mapping, int networkId) {
        List<String> selects = new ArrayList<>();
        List<String> joins = new ArrayList<>();

        addMetricUser(mapping, networkId, selects, joins, METRIC_EVENTS, "eventsCount", "events_u_ct");
        addMetricUser(mapping, networkId, selects, joins, METRIC_SAMPLES, "samplesCount", "samples_u_ct");
        addMetricUser(mapping, networkId, selects, joins, METRIC_DIAGNOSTICS, "diagnosticsCount", "diagnostics_u_ct");
        addMetricUser(mapping, networkId, selects, joins, METRIC_EVENT_PHOTOS, "eventPhotosCount", "event_photos_u_ct");
        addMetricUser(mapping, networkId, selects, joins, METRIC_SAMPLE_PHOTOS, "samplePhotosCount", "sample_photos_u_ct");
        addMetricUser(mapping, networkId, selects, joins, METRIC_EXTRACTIONS, "extractionsCount", "extractions_u_ct");
        addMetricUser(mapping, networkId, selects, joins, METRIC_FASTQ, "fastqCount", "fastq_u_ct");
        addMetricUser(mapping, networkId, selects, joins, METRIC_FASTA, "fastaCount", "fasta_u_ct");

        return new MetricSqlParts(selects, joins);
    }

    private void addMetricProject(
            LinkedHashMap<String, String> mapping,
            int networkId,
            List<String> selects,
            List<String> joins,
            String metric,
            String outputColumn,
            String joinAlias
    ) {
        String entity = mapping.get(metric);
        if (entity != null && SAFE_IDENTIFIER.matcher(entity).matches()) {
            joins.add(
                    "LEFT JOIN ( " +
                    "  SELECT e.project_id, COUNT(*)::bigint AS ct " +
                    "  FROM network_" + networkId + "." + entity + " r " +
                    "  JOIN expeditions e ON e.id = r.expedition_id " +
                    "  JOIN visible_projects vp ON vp.id = e.project_id " +
                    "  GROUP BY e.project_id " +
                    ") " + joinAlias + " ON " + joinAlias + ".project_id = p.id"
            );
            selects.add("COALESCE(" + joinAlias + ".ct, 0)::bigint AS \"" + outputColumn + "\"");
        } else {
            selects.add("0::bigint AS \"" + outputColumn + "\"");
        }
    }

    private void addMetricUser(
            LinkedHashMap<String, String> mapping,
            int networkId,
            List<String> selects,
            List<String> joins,
            String metric,
            String outputColumn,
            String joinAlias
    ) {
        String entity = mapping.get(metric);
        if (entity != null && SAFE_IDENTIFIER.matcher(entity).matches()) {
            joins.add(
                    "LEFT JOIN ( " +
                    "  SELECT e.user_id, COUNT(*)::bigint AS ct " +
                    "  FROM network_" + networkId + "." + entity + " r " +
                    "  JOIN expeditions e ON e.id = r.expedition_id " +
                    "  JOIN visible_projects vp ON vp.id = e.project_id " +
                    "  GROUP BY e.user_id " +
                    ") " + joinAlias + " ON " + joinAlias + ".user_id = u.id"
            );
            selects.add("COALESCE(" + joinAlias + ".ct, 0)::bigint AS \"" + outputColumn + "\"");
        } else {
            selects.add("0::bigint AS \"" + outputColumn + "\"");
        }
    }

    private VisibilitySql buildVisibleProjectsCte(ReportFilters filters, Integer userId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> clauses = new ArrayList<>();

        if (userId != null) {
            params.addValue("includePublic", filters.includePublic);
            params.addValue("userId", userId);
            clauses.add("(p.discoverable = true OR p.public = :includePublic OR p.user_id = :userId OR p.id IN (SELECT project_id FROM user_projects WHERE user_id = :userId))");
        } else {
            clauses.add("(p.discoverable = true OR p.public = true)");
        }

        if (filters.teamId != null) {
            params.addValue("teamId", filters.teamId);
            clauses.add("p.config_id = :teamId");
        }

        if (filters.projectId != null) {
            params.addValue("projectId", filters.projectId);
            clauses.add("p.id = :projectId");
        }

        String cteSql =
                "WITH visible_projects AS (" +
                "  SELECT p.id, p.project_title, p.config_id " +
                "  FROM projects p " +
                "  WHERE " + String.join(" AND ", clauses) +
                ") ";

        return new VisibilitySql(cteSql, params);
    }

    private MapSqlParameterSource copyParams(MapSqlParameterSource source) {
        MapSqlParameterSource target = new MapSqlParameterSource();
        for (Map.Entry<String, Object> entry : source.getValues().entrySet()) {
            target.addValue(entry.getKey(), entry.getValue());
        }
        return target;
    }

    private Integer getNullableInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        if (rs.wasNull()) {
            return null;
        }
        return value;
    }

    private static class ReportFilters {
        final boolean includePublic;
        final Integer teamId;
        final Integer projectId;
        final Integer topUsersLimit;
        final Integer fieldLimit;

        ReportFilters(boolean includePublic, Integer teamId, Integer projectId, Integer topUsersLimit, Integer fieldLimit) {
            this.includePublic = includePublic;
            this.teamId = teamId;
            this.projectId = projectId;
            this.topUsersLimit = topUsersLimit;
            this.fieldLimit = fieldLimit;
        }

        Map<String, Object> toMap() {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("includePublic", includePublic);
            map.put("teamId", teamId);
            map.put("projectId", projectId);
            map.put("topUsersLimit", topUsersLimit);
            map.put("fieldLimit", fieldLimit);
            return map;
        }
    }

    private static class ParsedEntities {
        final List<String> entities;
        final Map<String, String> fieldNamesByEntityAndUri;

        ParsedEntities(List<String> entities, Map<String, String> fieldNamesByEntityAndUri) {
            this.entities = entities;
            this.fieldNamesByEntityAndUri = fieldNamesByEntityAndUri;
        }
    }

    private static class VisibilitySql {
        final String cteSql;
        final MapSqlParameterSource params;

        VisibilitySql(String cteSql, MapSqlParameterSource params) {
            this.cteSql = cteSql;
            this.params = params;
        }
    }

    private static class MetricSqlParts {
        final List<String> selects;
        final List<String> joins;

        MetricSqlParts(List<String> selects, List<String> joins) {
            this.selects = selects;
            this.joins = joins;
        }
    }
}


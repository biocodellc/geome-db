package biocode.fims.rest.services;

import biocode.fims.application.config.GeomeProperties;
import biocode.fims.application.config.GeomeSql;
import biocode.fims.config.network.NetworkConfig;
import biocode.fims.models.Network;
import biocode.fims.models.Project;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.rest.FimsObjectMapper;
import biocode.fims.rest.UserContext;
import biocode.fims.serializers.Views;
import biocode.fims.service.NetworkService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class ProjectActivityTimestampTest {
    private static final String REPORTED_UPLOAD = "2026-10-20T01:42:38.323247Z";
    private final String serverTimezone;
    private TimeZone originalTimezone;

    public ProjectActivityTimestampTest(String serverTimezone) {
        this.serverTimezone = serverTimezone;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> timezones() {
        return Arrays.asList(new Object[][]{
                {"UTC"}, {"America/New_York"}, {"America/Los_Angeles"}, {"Asia/Tokyo"}
        });
    }

    @Before
    public void setTimezone() {
        originalTimezone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone(serverTimezone));
    }

    @After
    public void restoreTimezone() {
        TimeZone.setDefault(originalTimezone);
    }

    @Test
    public void should_return_project_stats_activity_as_an_explicit_utc_instant() throws Exception {
        String activity = (String) projectStats(Timestamp.from(Instant.parse(REPORTED_UPLOAD)))
                .get(0).get("latestDataModification");

        assertEquals(REPORTED_UPLOAD, activity);
        assertEquals("2026-10-19", Instant.parse(activity).atZone(ZoneId.of("America/New_York"))
                .toLocalDate().toString());
    }

    @Test
    public void should_preserve_null_activity_in_project_stats() throws Exception {
        assertNull(projectStats(null).get(0).get("latestDataModification"));
    }

    @Test
    public void should_serialize_project_activity_as_an_explicit_utc_instant() throws Exception {
        JsonNode project = serializeProject(Date.from(Instant.parse(REPORTED_UPLOAD)));
        String activity = project.get("latestDataModification").asText();

        assertEquals("2026-10-20T01:42:38.323Z", activity);
        assertEquals("2026-10-19", Instant.parse(activity).atZone(ZoneId.of("America/New_York"))
                .toLocalDate().toString());
    }

    @Test
    public void should_preserve_null_activity_when_serializing_a_project() throws Exception {
        assertTrue(serializeProject(null).get("latestDataModification").isNull());
    }

    private JsonNode serializeProject(Date activity) throws Exception {
        Project project = new Project.ProjectBuilder("", "MDBC", null).build();
        project.setLatestDataModification(activity);
        FimsObjectMapper mapper = new FimsObjectMapper();
        return mapper.readTree(mapper.writerWithView(Views.Detailed.class).writeValueAsString(project));
    }

    @SuppressWarnings("unchecked")
    private List<Map> projectStats(Timestamp activity) throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getTimestamp("latestDataModification")).thenReturn(activity);
        // Reproduce the legacy timezone-free API value if the controller regresses to getString().
        when(resultSet.getString("latestDataModification")).thenReturn(
                activity == null ? null : "2026-10-20 01:42:38.323247");

        RecordRepository repository = mock(RecordRepository.class);
        when(repository.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<Map> mapper = (RowMapper<Map>) invocation.getArguments()[2];
                    return Collections.singletonList(mapper.mapRow(resultSet, 0));
                });

        Network network = mock(Network.class);
        when(network.getNetworkConfig()).thenReturn(new NetworkConfig());
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getNetwork(1)).thenReturn(network);
        GeomeProperties properties = mock(GeomeProperties.class);
        when(properties.networkId()).thenReturn(1);
        GeomeSql sql = mock(GeomeSql.class);
        when(sql.projectStats()).thenReturn("SELECT project stats");

        ProjectController controller = new ProjectController(null, null, null, sql, repository, networkService, properties);
        ReflectionTestUtils.setField(controller, "userContext", mock(UserContext.class));
        return controller.projectStats(true);
    }
}

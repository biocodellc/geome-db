package biocode.fims.photos.processing;

import biocode.fims.config.models.Entity;
import biocode.fims.config.models.PhotoEntity;
import biocode.fims.models.Network;
import biocode.fims.models.dataTypes.JacksonUtil;
import biocode.fims.application.config.PhotosProperties;
import biocode.fims.application.config.PhotosSql;
import biocode.fims.query.PostgresUtils;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.service.NetworkService;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.springframework.scheduling.annotation.Scheduled;

import javax.ws.rs.client.Client;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author rjewing
 */
public class PhotoProcessingTaskScheduler {
    private static final long TEN_MINS = 1000 * 60 * 10;

    private final NetworkService networkService;
    private final RecordRepository recordRepository;
    private final PhotosSql photosSql;
    private final PhotoProcessingTaskExecutor processingTaskExecutor;
    private final Client client;
    private final PhotosProperties props;

    public PhotoProcessingTaskScheduler(NetworkService networkService, RecordRepository recordRepository,
                                        PhotosSql photosSql, PhotoProcessingTaskExecutor processingTaskExecutor,
                                        Client client, PhotosProperties props) {
        this.networkService = networkService;
        this.recordRepository = recordRepository;
        this.photosSql = photosSql;
        this.processingTaskExecutor = processingTaskExecutor;
        this.client = client;
        this.props = props;
    }

    @Scheduled(initialDelay = 60 * 1000, fixedDelay = TEN_MINS)
    public void scheduleTasks() {
        PhotoProcessor photoProcessor = new BasicPhotoProcessor(client, props);
        for (Network n: networkService.getNetworks()) {
            for (Entity e : n.getNetworkConfig().entities()) {
                if (!(e.type().equals(PhotoEntity.TYPE))) continue;

                Entity parentEntity = n.getNetworkConfig().entity(e.getParentEntity());

                String sql = photosSql.unprocessedPhotos();
                Map<String, String> tableMap = new HashMap<>();
                tableMap.put("table", PostgresUtils.entityTable(n.getId(), e.getConceptAlias()));

                List<UnprocessedPhotoRecord> records = recordRepository.query(
                        StrSubstitutor.replace(sql, tableMap),
                        null,
                        (rs, rowNum) -> {
                            String data = rs.getString("data");
                            String expeditionCode = rs.getString("expeditionCode");
                            int projectId = rs.getInt("projectId");

                            try {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> properties = JacksonUtil.fromString(data, HashMap.class);
                                return new UnprocessedPhotoRecord(properties, parentEntity, e, n.getId(), projectId, expeditionCode);
                            } catch (Exception ex) {
                                throw new SQLException(ex);
                            }
                        });

                for (UnprocessedPhotoRecord record : records) {
                    PhotoProcessingTask task = new PhotoProcessingTask(photoProcessor, record);
                    processingTaskExecutor.addTask(task);
                }
            }
        }
    }
}

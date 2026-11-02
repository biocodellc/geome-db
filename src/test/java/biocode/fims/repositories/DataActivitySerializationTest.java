package biocode.fims.repositories;
import biocode.fims.models.Expedition;
import biocode.fims.rest.FimsObjectMapper;
import biocode.fims.serializers.Views;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.TimeZone;
import static org.junit.Assert.*;
public class DataActivitySerializationTest {
 @Test public void serializesDateAndActorTogetherAsUtcEvenInPacificJvm() throws Exception {
  TimeZone previous=TimeZone.getDefault();
  try {
   TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
   Expedition expedition=new Expedition.ExpeditionBuilder("PRIVATE").build();
   Field time=Expedition.class.getDeclaredField("latestDataModification"); time.setAccessible(true); time.set(expedition,Date.from(java.time.Instant.parse("2026-10-29T17:33:35Z")));
   Field actor=Expedition.class.getDeclaredField("latestDataModificationUsername"); actor.setAccessible(true); actor.set(expedition,"editor");
   String json=new FimsObjectMapper().writerWithView(Views.Detailed.class).writeValueAsString(expedition);
   assertTrue(json,json.contains("2026-10-29T17:33:35.000Z")); assertTrue(json,json.contains("\"latestDataModificationUsername\":\"editor\""));
  } finally {TimeZone.setDefault(previous);}
 }
}

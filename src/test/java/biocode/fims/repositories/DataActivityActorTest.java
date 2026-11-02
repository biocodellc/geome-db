package biocode.fims.repositories;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.concurrent.*;
public class DataActivityActorTest {
 @Test public void restoresNestedActor() {
  assertNull(DataActivityActor.current());
  DataActivityActor.as("biocode", () -> {
   assertEquals("biocode", DataActivityActor.current());
   DataActivityActor.as("stweedt", () -> { assertEquals("stweedt", DataActivityActor.current()); return null; });
   assertEquals("biocode", DataActivityActor.current()); return null;
  });
  assertNull(DataActivityActor.current());
 }
 @Test public void cleansUpAfterFailure() {
  try { DataActivityActor.as("biocode", () -> { throw new IllegalStateException(); }); fail(); } catch (IllegalStateException expected) {}
  assertNull(DataActivityActor.current());
 }
 @Test public void attributesBackgroundWorkWithoutLeakingToNextTask() throws Exception {
  ExecutorService executor = Executors.newSingleThreadExecutor();
  try {
   assertEquals("biocode", executor.submit(() -> DataActivityActor.as("biocode", () -> DataActivityActor.current())).get());
   assertNull(executor.submit(() -> DataActivityActor.current()).get());
  } finally { executor.shutdownNow(); }
 }
}

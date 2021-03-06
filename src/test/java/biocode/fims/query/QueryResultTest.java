package biocode.fims.query;

import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.DefaultEntity;
import biocode.fims.config.models.Entity;
import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class QueryResultTest {

    @Test
    public void should_build_bcid_without_parent_entity() {

        LinkedList<Record> records = new LinkedList<>();
        Record r1 = new GenericRecord(new HashMap<>(), "ark:/99999/l2", 0, null, false);
        r1.set("urn:eventId", "1");

        records.add(r1);

        QueryResult result = new QueryResult(records, event(), "");

        List<Map<String, String>> expected = new ArrayList<>();
        Map<String, String> expectedR1 = new HashMap<>();
        expectedR1.put("eventId", "1");
        expectedR1.put("bcid", "ark:/99999/l21");
        expected.add(expectedR1);

        assertEquals(expected, result.get(true, false));

    }

    @Test
    public void should_transform_uris_to_columns() {

        LinkedList<Record> records = new LinkedList<>();

        Record r1 = new GenericRecord(new HashMap<>(), "root", 0, null, false);
        r1.set("urn:sampleId", "1");
        r1.set("urn:eventId", "1");
        r1.set("urn:col3", "value");
        r1.set("urn:col4", "another");

        records.add(r1);

        QueryResult result = new QueryResult(records, sample(), event(), "");

        List<Map<String, String>> expected = new ArrayList<>();
        Map<String, String> expectedR1 = new HashMap<>();
        expectedR1.put("sampleId", "1");
        expectedR1.put("eventId", "1");
        expectedR1.put("col3", "value");
        expectedR1.put("col4", "another");
        expectedR1.put("bcid", "root1");
        expected.add(expectedR1);

        assertEquals(expected, result.get(true, false));

    }

    @Test
    public void should_include_all_entity_columns() {

        LinkedList<Record> records = new LinkedList<>();

        Record r1 = new GenericRecord(new HashMap<>(), "root", 0, null, false);
        r1.set("urn:sampleId", "1");
        r1.set("urn:eventId", "1");
        r1.set("urn:col3", "value");

        records.add(r1);

        QueryResult result = new QueryResult(records, sample(), event(), "");

        List<Map<String, String>> expected = new ArrayList<>();
        Map<String, String> expectedR1 = new HashMap<>();
        expectedR1.put("sampleId", "1");
        expectedR1.put("eventId", "1");
        expectedR1.put("col3", "value");
        expectedR1.put("col4", "");
        expectedR1.put("bcid", "root1");
        expected.add(expectedR1);

        assertEquals(expected, result.get(true, false));

    }

    private Entity event() {
        Entity e = new DefaultEntity("event", "uri:event");
        e.setUniqueKey("eventId");
        e.addAttribute(new Attribute("eventId", "urn:eventId"));
        return e;
    }

    private Entity sample() {
        Entity e = new DefaultEntity("sample", "someURI");
        e.setParentEntity("event");
        e.setUniqueKey("sampleId");
        e.addAttribute(new Attribute("sampleId", "urn:sampleId"));
        e.addAttribute(new Attribute("eventId", "urn:eventId"));
        e.addAttribute(new Attribute("col3", "urn:col3"));
        e.addAttribute(new Attribute("col4", "urn:col4"));
        return e;
    }

}
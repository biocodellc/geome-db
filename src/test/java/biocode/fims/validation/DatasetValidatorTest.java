package biocode.fims.validation;

import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.DefaultEntity;
import biocode.fims.config.models.Entity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import biocode.fims.run.Dataset;
import biocode.fims.run.ProcessorStatus;
import biocode.fims.validation.rules.RequiredValueRule;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class DatasetValidatorTest {

    @Test
    public void should_validate_all_recordSets() {
        RecordValidatorFactory validatorFactory = new RecordValidatorFactory(new HashMap<>());
        Dataset dataset = new Dataset();

        RecordSet events = new RecordSet(entity1(), false);
        dataset.add(events);
        RecordSet samples = new RecordSet(entity2(), false);
        samples.setParent(events);
        dataset.add(samples);

        Record e1 = new GenericRecord();
        e1.set("eventId", "event1");
        e1.setExpeditionCode("exp");
        e1.setProjectId(1);
        events.add(e1);

        Record e2 = new GenericRecord();
        e2.set("eventId", "event2");
        e2.set("col1", "someValue");
        e2.setExpeditionCode("exp");
        e2.setProjectId(1);
        events.add(e2);

        Record s1 = new GenericRecord();
        s1.set("sampleId", "1");
        s1.set("eventId", "event1");
        s1.setExpeditionCode("exp");
        s1.setProjectId(1);
        samples.add(s1);

        Record s2 = new GenericRecord();
        s2.set("sampleId", "2");
        s2.set("eventId", "event1");
        s2.setExpeditionCode("exp");
        s2.setProjectId(1);
        samples.add(s2);

        DatasetValidator validator = new DatasetValidator(validatorFactory, dataset, config());

        assertFalse(validator.validate(new ProcessorStatus(),true));
        assertFalse(validator.hasError());

        List<EntityMessages> messages = validator.messages();
        assertEquals(1, messages.size());

        EntityMessages expectedMessages = new EntityMessages("event", "sheet1");
        expectedMessages.addWarningMessage(
                "Missing column(s)",
                new Message("\"col1\" has a missing cell value")
        );
        assertEquals(expectedMessages, messages.get(0));
    }

    @Test
    public void should_validate_if_parent_is_on_multi_entity_sheet_and_recordSet_has_records_with_same_identifier_and_same_values() {
        RecordValidatorFactory validatorFactory = new RecordValidatorFactory(new HashMap<>());
        Dataset dataset = new Dataset();

        RecordSet events = new RecordSet(entity1(), false);
        RecordSet samples = new RecordSet(entity2(), false);
        samples.setParent(events);

        dataset.add(events);
        dataset.add(samples);

        Record e1 = new GenericRecord();
        e1.set("eventId", "event1");
        e1.set("col1", "someValue");
        e1.setProjectId(1);
        e1.setExpeditionCode("exp");
        events.add(e1);

        Record e2 = new GenericRecord();
        e2.set("eventId", "event1");
        e2.set("col1", "someValue");
        e2.setProjectId(1);
        e2.setExpeditionCode("exp");
        events.add(e2);

        Record s1 = new GenericRecord();
        s1.set("sampleId", "1");
        s1.set("eventId", "event1");
        s1.setProjectId(1);
        s1.setExpeditionCode("exp");
        samples.add(s1);


        DatasetValidator validator = new DatasetValidator(validatorFactory, dataset, config());

        assertTrue(validator.validate(new ProcessorStatus(),true));
        assertFalse(validator.hasError());
    }

    @Test
    public void should_not_validate_if_parent_is_on_multi_entity_sheet_and_recordSet_has_records_with_same_identifier_and_different_values() {
        RecordValidatorFactory validatorFactory = new RecordValidatorFactory(new HashMap<>());
        Dataset dataset = new Dataset();

        ProjectConfig config = config();
        RecordSet events = new RecordSet(config.entity("event"), false);
        RecordSet samples = new RecordSet(config.entity("sample"), false);
        samples.setParent(events);

        dataset.add(events);
        dataset.add(samples);

        Record e1 = new GenericRecord();
        e1.set("eventId", "event1");
        e1.set("col1", "differentValue");
        e1.setProjectId(1);
        e1.setExpeditionCode("exp");
        events.add(e1);

        Record e2 = new GenericRecord();
        e2.set("eventId", "event1");
        e2.set("col1", "someValue");
        e2.setProjectId(1);
        e2.setExpeditionCode("exp");
        events.add(e2);

        Record s1 = new GenericRecord();
        s1.set("sampleId", "1");
        s1.set("eventId", "event1");
        s1.setProjectId(1);
        s1.setExpeditionCode("exp");
        samples.add(s1);


        DatasetValidator validator = new DatasetValidator(validatorFactory, dataset, config);

        assertFalse(validator.validate(new ProcessorStatus(),true));
        assertTrue(validator.hasError());

        List<EntityMessages> messages = validator.messages();
        assertEquals(1, messages.size());

        EntityMessages expectedMessages = new EntityMessages("event", "sheet1");
        expectedMessages.addErrorMessage(
                "Unique value constraint did not pass",
                new Message("\"eventId\" column is defined as unique but some values used more than once: \"event1\"")
        );
        expectedMessages.addErrorMessage(
                "Duplicate parent records",
                new Message("Duplicate \"eventId\" values, however the other columns are not the same.")
        );
        assertEquals(expectedMessages, messages.get(0));

    }

    private ProjectConfig config() {
        ProjectConfig config = new ProjectConfig();

        config.addEntity(entity1());
        config.addEntity(entity2());

        config.addDefaultRules();

        return config;
    }

    private Entity entity1() {
        Entity e = new DefaultEntity("event", "someURI");
        e.setUniqueKey("eventId");
        e.setWorksheet("sheet1");

        e.addAttribute(new Attribute("eventId", "eventId"));
        e.addAttribute(new Attribute("col1", "col1"));

        e.addRule(new RequiredValueRule(new LinkedHashSet<>(Collections.singletonList("col1"))));

        return e;
    }

    private Entity entity2() {
        Entity e = new DefaultEntity("sample", "someURI");
        e.setParentEntity("event");
        e.setUniqueKey("sampleId");
        e.setWorksheet("sheet1");

        e.addAttribute(new Attribute("sampleId", "sampleId"));
        e.addAttribute(new Attribute("eventId", "eventId"));

        return e;
    }

}
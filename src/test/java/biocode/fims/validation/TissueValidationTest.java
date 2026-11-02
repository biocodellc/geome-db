package biocode.fims.validation;

import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.DefaultEntity;
import biocode.fims.config.models.TissueEntity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.reader.DataConverter;
import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import biocode.fims.repositories.TissueRepository;
import biocode.fims.tissues.TissueProps;
import biocode.fims.tissues.reader.TissueConverter;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import biocode.fims.validation.rules.CompositeUniqueValueRule;
import biocode.fims.validation.rules.RegExpRule;
import biocode.fims.validation.rules.RuleLevel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(Parameterized.class)
public class TissueValidationTest {
    private final boolean upload;

    public TissueValidationTest(boolean upload) {
        this.upload = upload;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> modes() {
        return Arrays.asList(new Object[][]{{false}, {true}});
    }

    @Test
    public void should_report_missing_tissue_id_before_invalid_well() {
        ProjectConfig config = projectConfig(false);
        RecordSet tissues = tissues(config, tissue(null, "invalid-well"));

        assertMissingTissueId(config, tissues);
    }

    @Test
    public void should_report_blank_tissue_id_before_duplicate_plate_and_well() {
        ProjectConfig config = projectConfig(false);
        Record existing = new GenericRecord(tissue("existing", "A1").properties(), null, 1, "exp", false);
        RecordSet tissues = tissues(config, tissue("", "A1"));
        tissues.add(existing);

        assertMissingTissueId(config, tissues);
    }

    @Test
    public void should_report_missing_tissue_id_with_valid_plate_and_well() {
        ProjectConfig config = projectConfig(false);
        RecordSet tissues = tissues(config, tissue(null, "A1"));

        assertMissingTissueId(config, tissues);
    }

    @Test
    public void should_report_whitespace_tissue_id_as_missing() {
        ProjectConfig config = projectConfig(false);
        RecordSet tissues = tissues(config, tissue("   ", "A1"));

        assertMissingTissueId(config, tissues);
    }

    @Test
    public void should_still_report_invalid_well_when_tissue_id_is_present() {
        ProjectConfig config = projectConfig(false);
        RecordSet tissues = tissues(config, tissue("sample1.1", "invalid-well"));
        RecordValidator validator = new RecordValidator(config);

        assertFalse(validator.validate(tissues, upload));
        assertTrue(validator.hasError());

        EntityMessages expected = new EntityMessages("Tissue", "Tissue");
        expected.addErrorMessage("Value constraint did not pass", new Message(
                "Value \"invalid-well\" in column \"tissueWell\" does not match the pattern \"[A-H]([1-9]|1[0-2])\""));
        assertEquals(expected, validator.messages());
    }

    @Test
    public void should_accept_tissue_with_identifier_and_valid_plate_and_well() {
        ProjectConfig config = projectConfig(false);
        RecordValidator validator = new RecordValidator(config);

        assertTrue(validator.validate(tissues(config, tissue("sample1.1", "A1")), upload));
        assertFalse(validator.hasError());
        assertEquals(new EntityMessages("Tissue", "Tissue"), validator.messages());
    }

    @Test
    public void should_generate_and_validate_tissue_id_on_combined_sample_sheet() {
        ProjectConfig config = projectConfig(true);
        RecordSet tissues = tissues(config, tissue(null, "A1"));
        TissueRepository repository = mock(TissueRepository.class);
        DataConverter converter = new TissueConverter(repository).newInstance(config);

        converter.convertRecordSet(tissues, 1);

        assertEquals("sample1.1", tissues.records().get(0).get(TissueProps.IDENTIFIER.uri()));
        RecordValidator validator = new RecordValidator(config);
        assertTrue(validator.validate(tissues, upload));
        assertFalse(validator.hasError());
        assertEquals(new EntityMessages("Tissue", "Sample"), validator.messages());
        verifyZeroInteractions(repository);
    }

    private void assertMissingTissueId(ProjectConfig config, RecordSet tissues) {
        RecordValidator validator = new RecordValidator(config);
        assertFalse(validator.validate(tissues, upload));
        assertTrue(validator.hasError());

        EntityMessages expected = new EntityMessages("Tissue", "Tissue");
        expected.addErrorMessage("Missing column(s)", new Message("\"tissueID\" has a missing cell value"));
        assertEquals(expected, validator.messages());
        assertTrue(tissues.recordsToPersist().isEmpty());
    }

    private ProjectConfig projectConfig(boolean generateId) {
        ProjectConfig config = new ProjectConfig();
        DefaultEntity sample = new DefaultEntity("Sample", "urn:Sample");
        sample.setUniqueKey("sampleID");
        sample.setWorksheet("Sample");
        sample.addAttribute(new Attribute("sampleID", "urn:sampleID"));
        config.addEntity(sample);

        TissueEntity tissue = new TissueEntity();
        tissue.setUniqueKey("tissueID");
        tissue.setWorksheet(generateId ? "Sample" : "Tissue");
        tissue.setParentEntity("Sample");
        tissue.setGenerateID(generateId);
        tissue.addRule(new RegExpRule("tissueWell", "[A-H]([1-9]|1[0-2])", false, RuleLevel.ERROR));
        tissue.addRule(new CompositeUniqueValueRule(
                new LinkedHashSet<>(Arrays.asList("tissuePlate", "tissueWell")), RuleLevel.ERROR));
        tissue.configure(config);
        config.addEntity(tissue);
        config.addDefaultRules();
        return config;
    }

    private RecordSet tissues(ProjectConfig config, Record tissue) {
        RecordSet samples = new RecordSet(config.entity("Sample"), true);
        Record sample = new GenericRecord();
        sample.set("urn:sampleID", "sample1");
        sample.setExpeditionCode("exp");
        sample.setProjectId(1);
        samples.add(sample);

        RecordSet tissues = new RecordSet(config.entity("Tissue"), true);
        tissues.setParent(samples);
        tissues.add(tissue);
        return tissues;
    }

    private Record tissue(String identifier, String well) {
        Record record = new GenericRecord();
        record.set("urn:sampleID", "sample1");
        if (identifier != null) record.set(TissueProps.IDENTIFIER.uri(), identifier);
        record.set(TissueProps.PLATE.uri(), "plate1");
        record.set(TissueProps.WELL.uri(), well);
        record.setExpeditionCode("exp");
        record.setProjectId(1);
        return record;
    }
}

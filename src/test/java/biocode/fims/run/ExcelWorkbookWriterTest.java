package biocode.fims.run;

import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.DefaultEntity;
import biocode.fims.config.models.Entity;
import biocode.fims.config.models.Field;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.query.writers.WriterWorksheet;
import biocode.fims.validation.rules.ControlledVocabularyRule;
import biocode.fims.validation.rules.RuleLevel;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExcelWorkbookWriterTest {

    @Test
    public void should_use_absolute_references_for_controlled_vocabulary_data_validation() throws Exception {
        ProjectConfig config = configWithControlledVocabulary();
        ExcelWorkbookWriter writer = new ExcelWorkbookWriter(config, 12345);

        File workbook = writer.write(Arrays.asList(new WriterWorksheet("Samples", Arrays.asList("sampleId", "lifeStage"))));

        try {
            String validationXml = worksheetXmlWithDataValidation(workbook);

            assertTrue(validationXml.contains("<formula1>Lists!$A$2:$A$3</formula1>"));
            assertFalse(validationXml.contains("<formula1>Lists!A2:A3</formula1>"));
        } finally {
            workbook.delete();
        }
    }

    private ProjectConfig configWithControlledVocabulary() {
        ProjectConfig config = new ProjectConfig();

        biocode.fims.config.models.List list = new biocode.fims.config.models.List("lifeStageList");
        list.addField(field("adult"));
        list.addField(field("juvenile"));
        config.addList(list);

        Entity entity = new DefaultEntity("Samples", "urn:samples");
        entity.setWorksheet("Samples");
        entity.addAttribute(new Attribute("sampleId", "urn:sampleId"));
        entity.addAttribute(new Attribute("lifeStage", "urn:lifeStage"));
        entity.addRule(new ControlledVocabularyRule("lifeStage", "lifeStageList", config, RuleLevel.ERROR));
        config.addEntity(entity);

        return config;
    }

    private Field field(String value) {
        Field field = new Field();
        field.setValue(value);
        return field;
    }

    private String worksheetXmlWithDataValidation(File workbook) throws IOException {
        try (ZipFile zipFile = new ZipFile(workbook)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.getName().startsWith("xl/worksheets/") || !entry.getName().endsWith(".xml")) {
                    continue;
                }

                String xml = readEntry(zipFile, entry);
                if (xml.contains("<dataValidations")) {
                    return xml;
                }
            }
        }

        throw new AssertionError("Could not find worksheet XML with data validation");
    }

    private String readEntry(ZipFile zipFile, ZipEntry entry) throws IOException {
        try (InputStream inputStream = zipFile.getInputStream(entry);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }

            return outputStream.toString("UTF-8");
        }
    }
}

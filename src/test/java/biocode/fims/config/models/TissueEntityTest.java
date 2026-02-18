package biocode.fims.config.models;

import biocode.fims.config.project.ProjectConfig;
import biocode.fims.validation.rules.RuleLevel;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TissueEntityTest {

    @Test
    public void should_require_tissue_id_when_generate_id_is_false() {
        ProjectConfig config = projectConfig(false);

        Set<String> requiredColumns = config.getRequiredColumns("Tissue", RuleLevel.ERROR);

        assertTrue(requiredColumns.contains("sampleID"));
        assertTrue(requiredColumns.contains("tissueID"));
    }

    @Test
    public void should_not_require_tissue_id_when_generate_id_is_true() {
        ProjectConfig config = projectConfig(true);

        Set<String> requiredColumns = config.getRequiredColumns("Tissue", RuleLevel.ERROR);

        assertTrue(requiredColumns.contains("sampleID"));
        assertFalse(requiredColumns.contains("tissueID"));
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
        tissue.setWorksheet("Tissue");
        tissue.setParentEntity("Sample");
        tissue.setGenerateID(generateId);
        config.addEntity(tissue);

        config.addDefaultRules();
        return config;
    }
}

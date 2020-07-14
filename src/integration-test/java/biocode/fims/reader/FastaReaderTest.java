package biocode.fims.reader;

import biocode.fims.config.models.DefaultEntity;
import biocode.fims.config.models.FastaEntity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fasta.FastaProps;
import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.Entity;
import biocode.fims.exceptions.FastaReaderCode;
import biocode.fims.fasta.reader.FastaDataReaderType;
import biocode.fims.fasta.reader.FastaReader;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.records.Record;
import biocode.fims.records.RecordMetadata;
import biocode.fims.records.RecordSet;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class FastaReaderTest {
    private static String PARENT_UNIQUE_KEY = "parent_key";
    protected ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
    }

    @Test
    public void test_not_null_assertions() {
        try {
            new FastaReader(null, null, null);
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            new FastaReader(new File("test.fasta"), null, null);
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            new FastaReader(new File("test.fasta"), new ProjectConfig(), null);
            fail();
        } catch (IllegalArgumentException e) {
        }

    }

    @Test
    public void should_return_all_records_for_single_entity_mappping() {
        File file = new File(classLoader.getResource("resources/testDataset.fasta").getFile());

        RecordMetadata rm = new RecordMetadata(FastaDataReaderType.READER_TYPE, false);
        rm.add(FastaReader.CONCEPT_ALIAS_KEY, "fastaEntity");
        rm.add(FastaProps.MARKER.uri(), "CO1");
        DataReader reader = new FastaReader(file, getConfig(), rm);

        List<RecordSet> recordSets = reader.getRecordSets();

        assertEquals(1, recordSets.size());

        for (RecordSet set: recordSets) {
            assertEquals("fastaEntity", set.conceptAlias());
            assertEquals(3, set.records().size());

            for (Record r: set.records()) {
                assertNotNull(r.get(PARENT_UNIQUE_KEY));
                assertEquals("CO1", r.get(FastaProps.MARKER.uri()));
                assertFalse("missing sequence", r.get(FastaProps.SEQUENCE.uri()).trim().equals(""));
            }
        }
    }

    @Test
    public void should_throw_exception_if_no_data() {
        File file = new File(classLoader.getResource("resources/emptyDataset.fasta").getFile());

        RecordMetadata rm = new RecordMetadata(FastaDataReaderType.READER_TYPE, false);
        rm.add(FastaReader.CONCEPT_ALIAS_KEY, "fastaEntity");
        rm.add(FastaProps.MARKER.uri(), "CO1");
        DataReader reader = new FastaReader(file, getConfig(), rm);

        try {
            reader.getRecordSets();
            fail();
        } catch (FimsRuntimeException e) {
            assertEquals(FastaReaderCode.NO_DATA, e.getErrorCode());
        }
    }

    private ProjectConfig getConfig() {
        ProjectConfig config = new ProjectConfig();

        Entity parentEntity = new DefaultEntity("parent", "urn:parentEntity");
        parentEntity.setUniqueKey(PARENT_UNIQUE_KEY);
        config.addEntity(parentEntity);
        Entity fastaEntity = new FastaEntity("fastaEntity");
        fastaEntity.setParentEntity("parent");
        config.addEntity(fastaEntity);


        parentEntity.addAttribute(
                new Attribute("materialSampleID", "urn:materialSampleID")
        );

        return config;
    }
}

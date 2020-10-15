package biocode.fims.reader.plugins;

import biocode.fims.config.models.Attribute;
import biocode.fims.config.models.DefaultEntity;
import biocode.fims.config.models.Entity;
import biocode.fims.config.models.FastaEntity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.exceptions.FastaReaderCode;
import biocode.fims.fasta.FastaProps;
import biocode.fims.fasta.reader.FastaDataReaderType;
import biocode.fims.fasta.reader.FastaReader;
import biocode.fims.fastq.reader.FastqReader;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.reader.DataReader;
import biocode.fims.records.Record;
import biocode.fims.records.RecordMetadata;
import biocode.fims.records.RecordSet;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class FastqReaderTest {
    private static final Pattern PAIRED_ID_PATTERN = Pattern.compile("^([a-zA-Z0-9+=:._()~*]+)(-.*)?([.|_|-]+.*[12FR]+)\\.(fq|fastq)(\\.gz|\\.gzip|\\.bz2)?$", Pattern.CASE_INSENSITIVE);
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
            new FastqReader(null, null, null);


            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            new FastqReader(new File("test.fasta"), null, null);
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            new FastqReader(new File("test.fasta"), new ProjectConfig(), null);
            fail();
        } catch (IllegalArgumentException e) {
        }

    }

    /**
     *   Here we check some acceptable paired end filename patterns.  This test suite
     *   is used to look for some common filenaming conventions and to see that they pass
     *   the paired end filename check.
     */
    @Test
    public void checkPairedEndRegexpMatcher() {
        // Our pattern we are checking
        //SINGLE: "^([a-zA-Z0-9+=:._()~*]+)(-.*)?\\.(fq|fastq)(\\.gz|\\.gzip|\\.bz2)?$", Pattern.CASE_INSENSITIVE);
        //PAIRED: "^([a-zA-Z0-9+=:._()~*]+)(-.*)?([.|_|-]+.*[12FR]+)\\.(fq|fastq)(\\.gz|\\.gzip|\\.bz2)?$", Pattern.CASE_INSENSITIVE);

        Pattern pattern = PAIRED_ID_PATTERN;
        assertEquals(
                "test a true pattern for paired ends",
                pattern.matcher("A01-1.fastq.gz").matches(),
                true);
        assertEquals(
                "test a true pattern for paired ends",
                pattern.matcher("A01-F.fastq.gz").matches(),
                true);
        assertEquals(
                "test a true pattern for paired ends",
                pattern.matcher("A01-F1.fastq.gz").matches(),
                true);
        assertEquals(
                "test a false pattern for paired ends",
                pattern.matcher("A01-x.fastq.gz").matches(),
                false);
        assertEquals(
                "test case-insensitive pattern matching",
                pattern.matcher("A01-f.fastq.gz").matches(),
                true);


    }
}

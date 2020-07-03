package biocode.fims.fastq.reader;

import biocode.fims.config.models.Entity;
import biocode.fims.exceptions.FastqReaderCode;
import biocode.fims.fastq.FastqProps;
import biocode.fims.fastq.FastqRecord;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import biocode.fims.records.Record;
import biocode.fims.records.RecordMetadata;
import biocode.fims.records.RecordSet;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.reader.DataReader;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static biocode.fims.fastq.FastqProps.LIBRARY_LAYOUT;

/**
 * DataReader implementation for Fastq metadata files.
 *
 *
 * This Reader expects the following RecordMetadata:
 *
 *     - {@link FastqReader.CONCEPT_ALIAS_KEY}
 *     - {@link FastqProps.LIBRARY_LAYOUT.uri()}
 *
 */
public class FastqReader implements DataReader {
    private static final String CONCEPT_ALIAS_KEY = "conceptAlias";
    private static final List<String> EXTS = Arrays.asList("txt");

    private static final Pattern SINGLE_ID_PATTERN = Pattern.compile("^([a-zA-Z0-9+=:._()~*]+)(-.*)?\\.(fq|fastq)(\\.gz|\\.gzip|\\.bz2)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAIRED_ID_PATTERN = Pattern.compile("^([a-zA-Z0-9+=:._()~*]+)(-.*)?([.|_|-]+.*[12]+)\\.(fq|fastq)(\\.gz|\\.gzip|\\.bz2)?$", Pattern.CASE_INSENSITIVE);

    protected File file;
    protected ProjectConfig config;
    private RecordMetadata recordMetadata;
    private List<RecordSet> recordSets;
    private Map<String, List<String>> filenames;
    private String parentUniqueKeyUri;
    private Pattern pattern;

    /**
     * This is only to be used for passing the class into the DataReaderFactory
     */
    public FastqReader() {
    }

    public FastqReader(File file, ProjectConfig projectConfig, RecordMetadata recordMetadata) {
        Assert.notNull(file);
        Assert.notNull(projectConfig);
        Assert.notNull(recordMetadata);
        this.file = file;
        this.config = projectConfig;
        this.recordMetadata = recordMetadata;

        // so we know which one we are dealing with
        if (!recordMetadata.has(CONCEPT_ALIAS_KEY) ||
                !recordMetadata.has(LIBRARY_LAYOUT.uri())) {
            throw new FimsRuntimeException(DataReaderCode.MISSING_METADATA, 500);
        }

        if (((String) recordMetadata.get(LIBRARY_LAYOUT.uri())).equalsIgnoreCase("single")) {
            this.pattern = SINGLE_ID_PATTERN;
        } else {
            this.pattern = PAIRED_ID_PATTERN;
        }
    }

    @Override
    public boolean handlesExtension(String ext) {
        return EXTS.contains(ext.toLowerCase());
    }

    @Override
    public DataReader newInstance(File file, ProjectConfig projectConfig, RecordMetadata recordMetadata) {
        return new FastqReader(file, projectConfig, recordMetadata);
    }

    @Override
    public List<RecordSet> getRecordSets() {
        if (recordSets == null) {
            Entity entity = this.config.entity((String) recordMetadata.remove(CONCEPT_ALIAS_KEY));
            Entity parentEntity = this.config.entity(entity.getParentEntity());
            this.parentUniqueKeyUri = parentEntity.getUniqueKeyURI();

            List<Record> records = generateRecords();

            if (records.isEmpty()) {
                throw new FimsRuntimeException(FastqReaderCode.NO_DATA, 400);
            }

            recordSets = Collections.singletonList(
                    new RecordSet(entity, records, recordMetadata.reload())
            );
        }

        return recordSets;
    }

    private List<Record> generateRecords() {
        if (filenames == null) {
            filenames = parseFastqFilenames();
        }
        List<Record> fastaRecords = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry: filenames.entrySet()) {
            // TODO what is entry.key() for failed matches -- should be null
            fastaRecords.add(
                    new FastqRecord(parentUniqueKeyUri, entry.getKey(), entry.getValue(), recordMetadata)
            );
        }

        return fastaRecords;
    }

    private Map<String, List<String>> parseFastqFilenames() {
        Map<String, List<String>> recordFilenames = new HashMap<>();

        try {
            FileReader input = new FileReader(this.file);
            BufferedReader br = new BufferedReader(input);
            String line;

            while ((line = br.readLine()) != null) {
                if (line.trim().equals("")) continue;;
                Matcher matcher = pattern.matcher(line);

                if (matcher.matches()) {
                    String id = matcher.group(1);

                    recordFilenames.computeIfAbsent(id, k -> new ArrayList<>()).add(line);
                } else {
                    recordFilenames.computeIfAbsent(line, k -> new ArrayList<>()).add(line);
                }
            }
        } catch (IOException e) {
            throw new ServerErrorException(e);
        }

        return recordFilenames;
    }

    @Override
    public DataReaderType readerType() {
        return FastqDataReaderType.READER_TYPE;
    }
}

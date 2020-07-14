package biocode.fims.fasta.reader;

import biocode.fims.config.project.ProjectConfig;
import biocode.fims.fasta.FastaProps;
import biocode.fims.config.models.Entity;
import biocode.fims.exceptions.FastaReaderCode;
import biocode.fims.fasta.FastaRecord;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import biocode.fims.records.Record;
import biocode.fims.records.RecordMetadata;
import biocode.fims.records.RecordSet;
import biocode.fims.reader.DataReader;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


/**
 * DataReader implementation for Fasta files. Currently only the identifiers and the sequences
 * are read from the file
 * <p>
 * TODO: read in additional metadata provided for a each sequence ex. ">ALC111 organism="new findings" genus="alpha"
 * <p>
 * <p>
 * This Reader expects the following RecordMetadata:
 * <p>
 * - {@link FastaReader.CONCEPT_ALIAS_KEY}
 * - {@link FastaProps.MARKER}
 */
public class FastaReader implements DataReader {
    public static final String CONCEPT_ALIAS_KEY = "conceptAlias";
    public static final List<String> EXTS = Arrays.asList("fasta", "fa", "mpfa", "fna", "fas", "txt");

    protected File file;
    protected ProjectConfig config;
    private RecordMetadata recordMetadata;
    private List<RecordSet> recordSets;
    String parentUniqueKeyUri;

    /**
     * This is only to be used for passing the class into the DataReaderFactory
     */
    public FastaReader() {
    }

    public FastaReader(File file, ProjectConfig projectConfig, RecordMetadata recordMetadata) {
        Assert.notNull(file);
        Assert.notNull(projectConfig);
        Assert.notNull(recordMetadata);
        this.file = file;
        this.config = projectConfig;
        this.recordMetadata = recordMetadata;

        if (!recordMetadata.has(FastaProps.MARKER.uri()) && recordMetadata.has(FastaProps.MARKER.uri())) {
            recordMetadata.add(
                    FastaProps.MARKER.uri(), recordMetadata.remove(FastaProps.MARKER.uri())
            );
        }
        // so we know which one we are dealing with
        if (!recordMetadata.has(CONCEPT_ALIAS_KEY) ||
                !recordMetadata.has(FastaProps.MARKER.uri())) {
            throw new FimsRuntimeException(DataReaderCode.MISSING_METADATA, 500);
        }
    }

    @Override
    public boolean handlesExtension(String ext) {
        return EXTS.contains(ext.toLowerCase());
    }

    @Override
    public DataReader newInstance(File file, ProjectConfig projectConfig, RecordMetadata recordMetadata) {
        return new FastaReader(file, projectConfig, recordMetadata);
    }

    @Override
    public List<RecordSet> getRecordSets() {
        if (recordSets == null) {
            Entity entity = this.config.entity((String) recordMetadata.remove(CONCEPT_ALIAS_KEY));
            Entity parentEntity = this.config.entity(entity.getParentEntity());
            this.parentUniqueKeyUri = parentEntity.getUniqueKeyURI();

            List<Record> records = parseFasta();

            if (records.isEmpty()) {
                throw new FimsRuntimeException(FastaReaderCode.NO_DATA, 400);
            }

            recordSets = Collections.singletonList(
                    new RecordSet(entity, records, recordMetadata.reload())
            );
        }

        return recordSets;
    }

    /**
     * parse the fasta file identifier-sequence pairs, populating the fastaSequences property
     */
    private List<Record> parseFasta() {
        List<Record> fastaRecords = new ArrayList<>();

        try {
            FileReader input = new FileReader(this.file);
            BufferedReader bufRead = new BufferedReader(input);
            String line;
            String identifier = null;
            String sequence = "";

            while ((line = bufRead.readLine()) != null) {

                // > deliminates the next identifier, sequence block in the fasta file
                if (line.startsWith(">")) {

                    if (!sequence.isEmpty() || identifier != null) {
                        fastaRecords.add(
                                new FastaRecord(parentUniqueKeyUri, identifier, sequence, recordMetadata)
                        );

                        // after putting the sequence into the object, reset the sequence
                        sequence = "";

                    }

                    int endIdentifierIndex;

                    if (line.contains(" ")) {
                        endIdentifierIndex = line.indexOf(" ");
                    } else if (line.contains("\n")) {
                        endIdentifierIndex = line.indexOf("\n");
                    } else {
                        endIdentifierIndex = line.length();
                    }

                    // parse the identifier - minus the deliminator
                    identifier = line.substring(1, endIdentifierIndex);

                } else {
                    // if we are here, we are in between 2 identifiers. This means this is all sequence data
                    sequence += line;
                }
            }

            // need to put the last sequence data into the hashmap
            if (identifier != null) {
                fastaRecords.add(
                        new FastaRecord(parentUniqueKeyUri, identifier, sequence, recordMetadata)
                );
            }
        } catch (IOException e) {
            throw new ServerErrorException(e);
        }

        return fastaRecords;
    }

    @Override
    public DataReaderType readerType() {
        return FastaDataReaderType.READER_TYPE;
    }
}

package biocode.fims.fasta.reader;

import biocode.fims.reader.DataReader;

/**
 * @author rjewing
 */
public class FastaDataReaderType {
    public static final String READER_TYPE_STRING = "FASTA";
    public static final DataReader.DataReaderType READER_TYPE = new DataReader.DataReaderType(READER_TYPE_STRING);

    private FastaDataReaderType() {}
}

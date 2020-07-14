package biocode.fims.fastq.reader;

import biocode.fims.reader.DataReader;

/**
 * @author rjewing
 */
public class FastqDataReaderType {
    public static final String READER_TYPE_STRING = "FASTQ";
    public static final DataReader.DataReaderType READER_TYPE = new DataReader.DataReaderType(READER_TYPE_STRING);

    private FastqDataReaderType() {}
}

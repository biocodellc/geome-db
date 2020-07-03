package biocode.fims.ncbi.sra.submission;

import biocode.fims.exceptions.SraCode;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.utils.FileUtils;

import java.io.*;
import java.util.Iterator;
import java.util.List;

/**
 * class to generate a NCBI sra metadata file (https://www.ncbi.nlm.nih.gov/sra/docs/submitmeta/)
 * This takes the FastqMetadata and resource metadata stored in the FIMS system and writes it to a tsv file.
 */
public class SraMetadataGenerator {
    private static final String DELIMITER = "\t";

    /**
     * generates a sra metadata file from the provided {@link SraMetadataMapper}
     * @param mapper
     * @return
     */
    public static File generateFile(SraMetadataMapper mapper) {
        File metadataFile = FileUtils.createUniqueFile("sra-metadata.tsv", System.getProperty("java.io.tmpdir"));

        try (FileWriter fw = new FileWriter(metadataFile)) {
            List<String> headers = mapper.getHeaderValues();

            Iterator<String> it = headers.iterator();
            while (it.hasNext()) {
                fw.write(it.next());
                if (it.hasNext()) fw.write(DELIMITER);
            }
            fw.write("\n");

            while (mapper.hasNextResource()) {
                List<String> resourceMetadata = mapper.getResourceMetadata();

                if (!resourceMetadata.isEmpty()) {
                    it = resourceMetadata.iterator();

                    while (it.hasNext()) {
                        fw.write(it.next());
                        if (it.hasNext()) fw.write(DELIMITER);
                    }
                    fw.write("\n");
                }
            }

        } catch (IOException e) {
            throw new FimsRuntimeException(SraCode.METADATA_FILE_CREATION_FAILED, 500);
        }

        return metadataFile;
    }
}

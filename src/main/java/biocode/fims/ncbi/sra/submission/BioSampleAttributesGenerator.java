package biocode.fims.ncbi.sra.submission;

import biocode.fims.exceptions.SraCode;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.utils.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * class to generate a NCBI sra BioSample attributes file (https://www.ncbi.nlm.nih.gov/biosample/docs/submission/faq/)
 * This takes the sample metadata stored in the FIMS system and writes it to a tsv file. The attributes file is compatible
 * with both invertebrate and "model organism or animal sample" BioSample submissions.
 */
public class BioSampleAttributesGenerator {
    private static final String DELIMITER = "\t";

    /**
     * generates a bioSample attributes file from the provided {@link BioSampleMapper}
     *
     * @param mapper {@link BioSampleMapper} implementation
     * @return
     */


    public static File generateFile(BioSampleMapper mapper) {
        Logger logger = LoggerFactory.getLogger(BioSampleAttributesGenerator.class);

        File attributesFile = FileUtils.createUniqueFile("bioSample-attributes.tsv", System.getProperty("java.io.tmpdir"));
        logger.info("Creating file: {}", attributesFile.getAbsolutePath());

        try (FileWriter fw = new FileWriter(attributesFile)) {
            if (mapper == null) {
                logger.error("BioSampleMapper is null");
                throw new FimsRuntimeException(SraCode.METADATA_FILE_CREATION_FAILED, 500);
            }

            List<String> headers = mapper.getHeaderValues();
            if (headers == null || headers.isEmpty()) {
                logger.warn("Header values are null or empty");
            } else {
                logger.info("Writing {} headers to file", headers.size());
                Iterator<String> it = headers.iterator();
                while (it.hasNext()) {
                    fw.write(it.next());
                    if (it.hasNext()) fw.write(DELIMITER);
                }
                fw.write("\n");
            }

            int sampleCount = 0;
            while (mapper.hasNextSample()) {
                List<String> bioSampleAttributes = mapper.getBioSampleAttributes();

                if (bioSampleAttributes == null) {
                    logger.error("BioSample attributes list is null at sample index {}", sampleCount);
                    continue;
                }

                if (!bioSampleAttributes.isEmpty()) {
                    sampleCount++;
                    Iterator<String> it = bioSampleAttributes.iterator();
                    while (it.hasNext()) {
                        fw.write(it.next());
                        if (it.hasNext()) fw.write(DELIMITER);
                    }
                    fw.write("\n");
                }
            }
            logger.info("Successfully wrote {} samples to file", sampleCount);
        } catch (IOException e) {
            logger.error("IOException occurred while writing BioSample attributes file", e);
            throw new FimsRuntimeException(SraCode.METADATA_FILE_CREATION_FAILED, 500);
        }

        return attributesFile;
    }

}

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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
            logger.info("Original Headers: {}", headers);
    
            // Filter out null or empty headers
            List<String> validHeaders = headers.stream()
                    .filter(h -> h != null && !h.trim().isEmpty())
                    .collect(Collectors.toList());
    
            logger.info("Filtered Headers (non-empty): {}", validHeaders);
    
            // Write valid headers
            try {
                for (int i = 0; i < validHeaders.size(); i++) {
                    fw.write(validHeaders.get(i));
                    if (i < validHeaders.size() - 1) {
                        fw.write(DELIMITER);
                    }
                }
                fw.write("\n");
            } catch (IOException e) {
                logger.error("IOException occurred while writing headers to file: {}", attributesFile.getAbsolutePath(), e);
                throw new FimsRuntimeException(SraCode.METADATA_FILE_CREATION_FAILED, 500);
            }
    
            int sampleCount = 0;
            while (mapper.hasNextSample()) {
                List<String> bioSampleAttributes = mapper.getBioSampleAttributes();
    
                if (bioSampleAttributes == null) {
                    logger.error("BioSample attributes list is null at sample index {}", sampleCount);
                    continue;
                }
    
                // Ensure only non-null corresponding attributes are written
                List<String> filteredAttributes = new ArrayList<>();
                for (int i = 0; i < headers.size(); i++) {
                    if (headers.get(i) != null && !headers.get(i).trim().isEmpty()) {
                        // Ensure attribute exists, or write empty
                        String attribute = (i < bioSampleAttributes.size()) ? bioSampleAttributes.get(i) : "";
                        filteredAttributes.add(attribute != null ? attribute : "");
                    }
                }
    
                if (!filteredAttributes.isEmpty()) {
                    sampleCount++;
                    try {
                        for (int i = 0; i < filteredAttributes.size(); i++) {
                            fw.write(filteredAttributes.get(i));
                            if (i < filteredAttributes.size() - 1) {
                                fw.write(DELIMITER);
                            }
                        }
                        fw.write("\n");
                    } catch (IOException e) {
                        logger.error("IOException occurred while writing sample {} to file", sampleCount, e);
                        throw new FimsRuntimeException(SraCode.METADATA_FILE_CREATION_FAILED, 500);
                    }
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

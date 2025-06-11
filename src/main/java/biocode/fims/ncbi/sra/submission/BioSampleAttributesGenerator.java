package biocode.fims.ncbi.sra.submission;

import biocode.fims.exceptions.SraCode;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.geome.sra.GeomeBioSampleMapper;
import biocode.fims.records.Record;
import biocode.fims.utils.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        System.out.println(">>> BioSampleAttributesGenerator.generateFile() received mapper instance: " + mapper.hashCode());

        File attributesFile = FileUtils.createUniqueFile("bioSample-attributes.tsv", System.getProperty("java.io.tmpdir"));
        logger.info("Creating file: {}", attributesFile.getAbsolutePath());
    
        try (FileWriter fw = new FileWriter(attributesFile)) {
            if (mapper == null) {
                logger.error("BioSampleMapper is null");
                throw new FimsRuntimeException(SraCode.METADATA_FILE_CREATION_FAILED, 500);
            }
    
            List<String> headers = mapper.getHeaderValues(); // Friendly labels
            Map<String, String> labelToUri = mapper.getLabelToUriMap();

            logger.info("Writing headers: {}", headers);

            // Write header row (labels)
            for (int i = 0; i < headers.size(); i++) {
                fw.write(headers.get(i));
                if (i < headers.size() - 1) fw.write(DELIMITER);
            }
            fw.write("\n");

            // Write each row using uri mapping
            int sampleCount = 0;
            Set<String> requiredFields = mapper.getRequiredHeaders();

            System.out.println(">>> Ready to write rows?");
            System.out.println(">>> mapper.hasNextSample(): " + mapper.hasNextSample());
            System.out.println(">>> mapper.getRecordCount(): " + (mapper instanceof GeomeBioSampleMapper ? ((GeomeBioSampleMapper) mapper).getRecordCount() : "unknown"));

            while (mapper.hasNextSample()) {
                Record record = mapper.nextRecord();
                if (record == null) {
                    System.out.println(">>> Skipped null record.");
                    continue;
                }

                List<String> row = new ArrayList<>();

                for (String label : headers) {
                    String value = record.get(label); // <-- use the label directly

                    if (requiredFields.contains(label)) {
                        row.add(StringUtils.isBlank(value) ? "missing" : value);
                    } else {
                        row.add(value != null ? value : "");
                    }
                }


                fw.write(String.join(DELIMITER, row));
                fw.write("\n");
                sampleCount++;
            }


            logger.info("Successfully wrote {} samples to file", sampleCount);
        } catch (IOException e) {
            logger.error("IOException occurred while writing BioSample attributes file", e);
            throw new FimsRuntimeException(SraCode.METADATA_FILE_CREATION_FAILED, 500);
        }
    
        return attributesFile;
    }
   



}

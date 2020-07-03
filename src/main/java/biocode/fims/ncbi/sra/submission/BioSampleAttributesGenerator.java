package biocode.fims.ncbi.sra.submission;

import biocode.fims.exceptions.SraCode;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.utils.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

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
        File attributesFile = FileUtils.createUniqueFile("bioSample-attributes.tsv", System.getProperty("java.io.tmpdir"));

        try (FileWriter fw = new FileWriter(attributesFile)) {

            List<String> headers = mapper.getHeaderValues();

            Iterator<String> it = headers.iterator();
            while (it.hasNext()) {
                fw.write(it.next());
                if (it.hasNext()) fw.write(DELIMITER);
            }
            fw.write("\n");

            while (mapper.hasNextSample()) {
                List<String> bioSampleAttributes = mapper.getBioSampleAttributes();

                if (!bioSampleAttributes.isEmpty()) {
                    it = bioSampleAttributes.iterator();

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

        return attributesFile;


    }
}

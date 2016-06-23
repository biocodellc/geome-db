package biocode.fims.genbank;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;

import java.io.IOException;

/**
 * Class to generate a sequin file to submit to genbank. This class calls the tbl2asn command line app
 * available at <a href="http://www.ncbi.nlm.nih.gov/genbank/tbl2asn2/">http://www.ncbi.nlm.nih.gov/genbank/tbl2asn2/</a>.
 * tbl2asn takes a template file and a fasta file and generates a sequin file.
 */
public class SequinGenerator {

    /**
     *
     * @param fastaFile the absolute file path or the directory containing the fasta files
     * @param templateFile
     * @param outputDirectory
     */
    public static void generate(String fastaFile, String templateFile, String outputDirectory) {
        String fastaDirectory;
        String filename = null;
        String[] splitPath = fastaFile.split("/");

        // if fastaFile ends with .fsa, the this is an absolute path to a file, so we need to parse the filename and directory
        if (splitPath[splitPath.length - 1].endsWith(".fsa")) {
            filename = splitPath[splitPath.length - 1];
            fastaDirectory = fastaFile.substring(0, fastaFile.length() - filename.length());
        } else {
            fastaDirectory = fastaFile;
        }

        String line = "tbl2asn -t " + templateFile + " -p " + fastaDirectory + " -a s -V v -r " + outputDirectory;
        if (filename != null) {
            line += " -i " + filename;
        }

        CommandLine cmdLine = CommandLine.parse(line);
        DefaultExecutor executor = new DefaultExecutor();
        try {
            executor.execute(cmdLine);
        } catch(IOException e) {
            throw new FimsRuntimeException(500, e);
        }
    }
}

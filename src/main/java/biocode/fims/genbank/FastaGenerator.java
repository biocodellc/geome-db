package biocode.fims.genbank;

import biocode.fims.bcid.Identifier;
import biocode.fims.digester.Entity;
import biocode.fims.digester.Mapping;
import biocode.fims.entities.Bcid;
import biocode.fims.fasta.FastaSequence;
import biocode.fims.fasta.FastaUtils;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.settings.PathManager;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Class to facilitate fasta file generation for biocode.fims.genbank submissions
 */
public class FastaGenerator {
    private final static String GENUS_URI = "urn:genus";
    private final static String SPECIES_URI = "urn:species";
    private final static String ACCESSION_URI = "urn:accession";

    /**
     * fetches the triples in the specified project where the ACCESSION_URI doesn't exist, and writes the data
     * necessary to upload the biocode.fims.genbank to a fasta file.
     * @param datasets the set of dataset Bcids to fetch from the fuseki tdb
     * @param fusekiService
     * @param mapping
     * @param outputDirectory
     * @param divider The divider for identifiers, seperating the root identifier from the suffix
     * @return the absolute path of the fasta file
     */
    public static String generate(Set<Bcid> datasets, String fusekiService, Mapping mapping, String outputDirectory, String divider) {
        Entity rootEntity = FastaUtils.getEntityRoot(mapping, FastaSequence.SEQUENCE_URI);

        List<FastaSequence> fastaSequences = getFastaSequences(fusekiService, datasets,
                rootEntity.getConceptURI(), divider);

        return writeSequencesToFile(fastaSequences, outputDirectory);
    }

    private static String writeSequencesToFile(List<FastaSequence> fastaSequences, String outputDirectory) {
        StringBuilder data = new StringBuilder();
        File fastaFile = PathManager.createFile("dipnet.fsa", outputDirectory);

        try {
            FileOutputStream fos = new FileOutputStream(fastaFile);
            for (FastaSequence fastaSequence: fastaSequences) {
                // each entry starts with ">"
                data.append("> ");

                data.append(fastaSequence.getIdentifier());
                data.append(" [organism=");
                data.append(fastaSequence.getOrganism());
                data.append("]\n");

                data.append(fastaSequence.getSequence());
                data.append("\n");
            }

            fos.write(data.toString().getBytes());
            fos.close();
        } catch (IOException ioe) {
            throw new ServerErrorException(ioe);
        }

        return fastaFile.getAbsolutePath();
    }

    private static List<FastaSequence> getFastaSequences(String fusekiService, Set<Bcid> datasets,
                                                  String conceptUri, String divider) {
        ArrayList<FastaSequence> fastaSequences = new ArrayList<>();
        if (datasets.size() > 0) {
            // query fuseki graph
            StringBuilder sparql = new StringBuilder();
            sparql.append("SELECT ?identifier ?genus ?species ?sequence ");

            for (Bcid bcid : datasets) {
                sparql.append("FROM <" + bcid.getGraph() + "> ");
            }
            sparql.append("WHERE { ");
            sparql.append("?identifier a <" + conceptUri + "> . ");
            sparql.append("?identifier <" + GENUS_URI + "> ?genus . ");
            sparql.append("?identifier <" + SPECIES_URI + "> ?species . ");
            sparql.append("?identifier <" + FastaSequence.SEQUENCE_URI + "> ?sequence . ");
            sparql.append("MINUS { ?identifier <" + ACCESSION_URI + "> ?x } ");
            sparql.append("}");

            QueryExecution qexec = QueryExecutionFactory.sparqlService(fusekiService + "/query", sparql.toString());
            com.hp.hpl.jena.query.ResultSet results = qexec.execSelect();

            // loop through results adding identifiers to datasetIds array
            while (results.hasNext()) {
                QuerySolution soln = results.next();
                Identifier identifier = new Identifier(soln.getResource("identifier").getURI(), divider);
                String sequence = soln.getLiteral("sequence").getString();
                String genus = soln.getLiteral("genus").getString();
                String species = soln.getLiteral("species").getString();

                if (!identifier.hasSuffix()) {
                    throw new ServerErrorException("Error parsing Bcid identifier", "No suffix was found.");
                }

                FastaSequence fastaSequence = new FastaSequence(identifier.getSuffix(), sequence);

                fastaSequence.setIdentifier(identifier.getBcidIdentifier());
                fastaSequence.setOrganism(genus + " " + species);

                fastaSequences.add(fastaSequence);
            }
        }
        return fastaSequences;
    }
}

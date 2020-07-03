package biocode.fims.fasta;

import biocode.fims.config.Config;
import biocode.fims.config.models.Entity;
import biocode.fims.config.models.FastaEntity;
import biocode.fims.config.project.ProjectConfig;
import biocode.fims.exceptions.FastaWriteCode;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.fimsExceptions.errorCodes.QueryCode;
import biocode.fims.query.QueryResult;
import biocode.fims.query.writers.QueryWriter;
import biocode.fims.utils.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.*;

/**
 * @author RJ Ewing
 */
public class FastaQueryWriter implements QueryWriter {
    private final QueryResult queryResult;
    private final Entity parentEntity;
    private final String parentUniqueKey;

    public FastaQueryWriter(QueryResult queryResult, Config config) {
        // TODO pass in parentRecords & list of columns to write out as metadata
        if (!(Objects.equals(queryResult.entity().type(), FastaEntity.TYPE))) {
            throw new FimsRuntimeException(FastaWriteCode.INVALID_ENTITY, 500, queryResult.entity().type());
        }
        this.queryResult = queryResult;

        if (queryResult.get(false, true).size() == 0) {
            throw new FimsRuntimeException(QueryCode.NO_RESOURCES, 400);
        }

        parentEntity = config.entity(queryResult.entity().getParentEntity());
        parentUniqueKey = parentEntity.getUniqueKey();
    }

    /**
     * writes the records in QueryResults to a file. If more the 1 marker type is present,
     * a zip file will be returned with a fasta file with records for each unique marker
     *
     * @return
     */
    @Override
    public List<File> write() {
        Map<String, List<Map<String, Object>>> recordsMap = sortByMarker();
        List<File> sequenceFiles = new ArrayList<>();

        for (Map.Entry<String, List<Map<String, Object>>> entry : recordsMap.entrySet()) {
            sequenceFiles.add(
                    writeMarkerFile(entry.getValue(), entry.getKey())
            );
        }

        return sequenceFiles;
    }

    private Map<String, List<Map<String, Object>>> sortByMarker() {
        Map<String, List<Map<String, Object>>> fastaFileMap = new HashMap<>();

        for (Map<String, Object> record : queryResult.get(false, true)) {
            String marker = String.valueOf(record.get(FastaProps.MARKER.uri()));

            fastaFileMap
                    .computeIfAbsent(marker, k -> new ArrayList<>())
                    .add(record);
        }

        return fastaFileMap;
    }

    private File writeMarkerFile(List<Map<String, Object>> records, String marker) {
        String filename = StringUtils.isBlank(marker) ? "output.fasta" : marker + ".fasta";
        File file = FileUtils.createFile(filename, System.getProperty("java.io.tmpdir"));

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file)))) {


            for (Map<String, Object> record : records) {
                writer.write(">");

                String identifier = String.valueOf(record.get("bcid"));
                writer.write(identifier);

                writer.write(" [marker = ");
                writer.write(String.valueOf(record.get(FastaProps.MARKER.uri())));
                writer.write("] [");
                writer.write(parentUniqueKey);
                writer.write(" = ");
                writer.write(String.valueOf(record.getOrDefault(parentUniqueKey, "")));
                writer.write("] [genus = ");
                writer.write(String.valueOf(record.getOrDefault("urn:genus", "")));
                writer.write("] [specificEpithet = ");
                writer.write(String.valueOf(record.getOrDefault("urn:species", "")));
                writer.write("]\n");

                // TODO add more metadata (locality, genus, species) once networks are implemented

                writer.write(String.valueOf(record.get(FastaProps.SEQUENCE.uri())));
                writer.write("\n");
            }

        } catch (IOException e) {
            throw new FimsRuntimeException(FileCode.WRITE_ERROR, 500);
        }

        return file;
    }
}

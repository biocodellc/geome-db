package biocode.fims.ncbi.sra.submission;

import com.sun.xml.bind.v2.runtime.IllegalAnnotationsException;
import biocode.fims.models.SraSubmissionEntry;
import biocode.fims.models.User;
import biocode.fims.ncbi.models.GeomeBioSample;
import biocode.fims.ncbi.models.SraMetadata;
import biocode.fims.ncbi.models.SraSubmissionData;
import biocode.fims.ncbi.models.submission.SraSubmission;
import biocode.fims.repositories.SraSubmissionRepository;
import biocode.fims.rest.models.SraUploadMetadata;
import biocode.fims.rest.responses.SraUploadResponse;
import biocode.fims.utils.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author rjewing
 */
public class SraLoader {
    private final static Logger logger = LoggerFactory.getLogger(SraLoader.class);

    private final SraUploadMetadata metadata;
    private final ZipInputStream is;
    private final SraSubmissionData sraSubmissionData;
    private final String sraSubmissionDir;
    private final User user;

    private final ArrayList<String> invalidFiles;
    private final String url;
    private final SraSubmissionRepository sraSubmissionRepository;
    private Map<String, List<File>> files;
    private Path submissionDir;

    public SraLoader(SraUploadMetadata metadata, ZipInputStream is, SraSubmissionData sraSubmissionData,
                     String sraSubmissionDir, User user, String appUrl, SraSubmissionRepository sraSubmissionRepository) {
        this.metadata = metadata;
        this.is = is;
        this.sraSubmissionData = sraSubmissionData;
        this.sraSubmissionDir = sraSubmissionDir;
        this.user = user;
        this.url = appUrl;
        this.sraSubmissionRepository = sraSubmissionRepository;
        invalidFiles = new ArrayList<>();
        files = new HashMap<>();
    }

    public SraUploadResponse upload() {
        SraSubmissionData filteredSubmissionData = getSraSubmissionData();

        if (filteredSubmissionData.bioSamples.size() != metadata.bioSamples.size()) {
            return new SraUploadResponse(false, "Invalid bioSamples provided");
        }


        try {
            extractFiles();
            logger.debug("Files successfully extracted");
        } catch (IOException e) {
            logger.debug("Error extracting files", e);
            deleteSubmissionDir();
            return new SraUploadResponse(false, "Invalid/corrupt zip file.");
        }

        logger.debug("Checking for missing files");
        // check that all filenames are present
        List<String> missingFiles = checkForMissingFiles(filteredSubmissionData);

        if (missingFiles.size() > 0) {
            logger.debug("Missing files found");
            deleteSubmissionDir();
            return new SraUploadResponse(
                    false,
                    "The following required files are missing: " +
                            String.join("\", \"", missingFiles) + "\".\n " +
                            "Either submit these files, or remove the bioSamples that require these files from this submission."
            );
        }

        logger.debug("No missing files found. Writing submission xml");
        logger.error("testing logger.error");

        try {
            writeSubmissionXml(filteredSubmissionData);
        } catch (Exception e) {
            logger.debug("Exception thrown, deleting submission dir");
            logger.error("Error creating submission.xml", e);
            deleteSubmissionDir();
            logger.debug("submission dir successfully deleted");
            return new SraUploadResponse(false, "Error creating submission.xml file");
        }

        logger.debug("submission.xml file written. Saving to db");

        try {
            sraSubmissionRepository.save(
                    new SraSubmissionEntry(
                            metadata.project,
                            metadata.expedition,
                            user,
                            getSubmissionDirectory()
                    )
            );
            logger.debug("sra submission saved");
        } catch (Exception e) {
            logger.error("Error saving SraSubmission", e);
            deleteSubmissionDir();
            return new SraUploadResponse(false, "Error saving SRA submission");
        }
        logger.debug("sra submission is valid");

        // validate is from
        return new SraUploadResponse(true, null);
    }

    private void writeSubmissionXml(SraSubmissionData filteredSubmissionData) throws JAXBException {
        try {
            logger.debug("creating SraSubmission object");
            SraSubmission submission = new SraSubmission(filteredSubmissionData, metadata, user, url);
            logger.debug("initiating JAXBContext: " + JAXBContext.JAXB_CONTEXT_FACTORY);
            JAXBContext jaxbContext = JAXBContext.newInstance(SraSubmission.class);
            logger.debug("creating marshaller");
            Marshaller marshaller = jaxbContext.createMarshaller();
            logger.debug("opening submission.xml file");
            File file = new File(getSubmissionDirectory().toString(), "submission.xml");
            logger.debug("marshaling submission.xml file");
            marshaller.marshal(submission, file);
        } catch (JAXBException e) {
            logger.error("", e);
            throw e;
        }
    }

    private List<String> checkForMissingFiles(SraSubmissionData filteredSubmissionData) {
        List<String> requiredFiles = filteredSubmissionData.sraMetadata.stream()
                .flatMap(m ->
                        Stream.of(m.get("filename"), m.get("filename2"))
                ).filter(n -> !StringUtils.isBlank(n))
                .collect(Collectors.toList());

        return requiredFiles.stream()
                .filter(name -> !files.containsKey(name))
                .collect(Collectors.toList());
    }

    private SraSubmissionData getSraSubmissionData() {
        List<GeomeBioSample> bioSamples = sraSubmissionData.bioSamples.stream()
                .filter(b -> metadata.bioSamples.contains(b.get("sample_name")))
                .collect(Collectors.toList());
        List<SraMetadata> sraMetadata = sraSubmissionData.sraMetadata.stream()
                .filter(m -> metadata.bioSamples.contains(m.get("sample_name")))
                .collect(Collectors.toList());

        return new SraSubmissionData(bioSamples, sraMetadata);
    }

    private Path getSubmissionDirectory() {
        if (this.submissionDir == null) {
            this.submissionDir = Paths.get(sraSubmissionDir, metadata.expedition.getExpeditionCode() + "_" + new Date().getTime());
            this.submissionDir.toFile().mkdir();
        }
        return this.submissionDir;
    }

    private void extractFiles() throws IOException {
        Path submissionDir = getSubmissionDirectory();

        ZipEntry ze = is.getNextEntry();

        // if root ZipEntry is a directory, extract files from there
        String zipRootDir = "";
        if (ze.isDirectory()) {
            zipRootDir = ze.getName();
            ze = is.getNextEntry();
        }

        // These suffixes are from FastqFilenamesRule
        List<String> fileSuffixes = Arrays.asList("fq", "fastq", "gz", "gzip", "bz2");

        byte[] buffer = new byte[1024];
        while (ze != null) {
            String fileName = ze.getName().replace(zipRootDir, "");
            String ext = FileUtils.getExtension(fileName, "");

            // ignore nested directories & unsupported file extensions
            if (ze.isDirectory() ||
                    fileName.split(File.separator).length > 1 ||
                    !fileSuffixes.contains(ext.toLowerCase())) {
                logger.info("ignoring dir/unsupported file: " + ze.getName());

                // don't report about hidden osx included dir
                if (!ze.getName().startsWith("__MACOSX") && !ze.getName().endsWith(".DS_Store")) {
                    invalidFiles.add(ze.getName());
                }

                if (ze.isDirectory()) {
                    // skip everything in that directory
                    String root = ze.getName();

                    do {
                        ze = is.getNextEntry();
                    }
                    while (ze != null && ze.getName().startsWith(root));

                } else {
                    ze = is.getNextEntry();
                }
                continue;
            }

            File file = new File(submissionDir.toString(), ze.getName());

            logger.debug("unzipping file: " + fileName + " to: " + file.getAbsolutePath());

            try (FileOutputStream fos = new FileOutputStream(file)) {
                int len;
                while ((len = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                files.computeIfAbsent(fileName, k -> new ArrayList<>()).add(file);
            } catch (Exception e) {
                logger.debug("Failed to extract file", e);
                invalidFiles.add(ze.getName());
            }

            ze = is.getNextEntry();
        }
    }

    private void deleteSubmissionDir() {
        File dir = getSubmissionDirectory().toFile();
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        dir.delete();
    }

}

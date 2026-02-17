package biocode.fims.photos;

import biocode.fims.config.models.Entity;
import biocode.fims.config.models.PhotoEntity;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.fimsExceptions.errorCodes.FileCode;
import biocode.fims.fimsExceptions.errorCodes.ValidationCode;
import biocode.fims.models.Project;
import biocode.fims.models.User;
import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.utils.FileUtils;
import biocode.fims.utils.StringGenerator;
import com.opencsv.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author rjewing
 */
public class BulkPhotoPackage {
    private final static String BULK_PHOTO_DIR = "bulk";
    private final static String METADATA_FILE_NAME = "metadata.csv";
    private final static String METADATA_FILE_NAME_COL = "fileName";

    private final static Logger logger = LoggerFactory.getLogger(Builder.class);
    // file pattern is parent_identifier+photoID.ext
    private static final Pattern FILE_PATTERN = Pattern.compile("^([a-zA-Z0-9+=:._()~*]+)\\+([a-zA-Z0-9+=:._()~*]+)\\.(.*)?$", Pattern.CASE_INSENSITIVE);

    private final ZipInputStream stream;
    private final User user;
    private final Project project;
    private final String expeditionCode;
    private final RecordRepository recordRepository;
    private final Entity entity;
    private final Entity parentEntity;
    private final ArrayList<String> invalidFiles;
    private final String photosDir;
    private final boolean ignoreId;

    private Map<String, String> parentExpeditionCodes;
    private Map<String, List<File>> files;

    private BulkPhotoPackage(Builder builder) {
        this.photosDir = builder.photosDir;
        this.stream = builder.stream;
        this.user = builder.user;
        this.project = builder.project;
        this.expeditionCode = builder.expeditionCode;
        this.entity = builder.entity;
        this.parentEntity = project.getProjectConfig().entity(entity.getParentEntity());
        this.recordRepository = builder.recordRepository;
        this.ignoreId = builder.ignoreId;
        this.invalidFiles = new ArrayList<>();
        this.files = new HashMap<>();

    }

    public File metadataFile() {
        extractFiles();

        if (files.isEmpty()) {
            throw new FimsRuntimeException(ValidationCode.EMPTY_DATASET, 400);
        }

        return files.containsKey(METADATA_FILE_NAME)
                ? updateMetadataFile(files.get(METADATA_FILE_NAME).get(0))
                : generateMetadataFile();
    }

    public Project project() {
        return project;
    }

    public String expeditionCode() {
        return expeditionCode;
    }

    public ArrayList<String> invalidFiles() {
        return invalidFiles;
    }

    public User user() {
        return user;
    }

    public Entity entity() {
        return entity;
    }

    /**
     * add expeditionCodes to the metadata file if they aren't present
     *
     * @param metadataFile
     * @return
     */
    private File updateMetadataFile(File metadataFile) {
        try {
            CSVParser parser = new CSVParserBuilder()
                    .withSeparator(',')
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            List<String[]> metadata = new CSVReaderBuilder(new FileReader(metadataFile))
                    .withCSVParser(parser)
                    .build()
                    .readAll();

            List<String> metadataColumns = new ArrayList<>(Arrays.asList(metadata.remove(0)));

            int expeditionCol = metadataColumns.indexOf(Record.EXPEDITION_CODE);
            // if missing parent identifier column, we can't set the expedition
            int parentIdentifierCol = metadataColumns.indexOf(parentEntity.getUniqueKey());

            boolean setExpedition = expeditionCol == -1 &&
                    parentIdentifierCol > -1 &&
                    expeditionCode == null &&
                    parentEntity.getUniqueAcrossProject();

            int fileNameCol = metadataColumns.indexOf(METADATA_FILE_NAME_COL);

            // missing fileName column, nothing we can do
            if (fileNameCol == -1) {
                throw new FimsRuntimeException(ValidationCode.INVALID_DATASET, 400, METADATA_FILE_NAME + " is missing a fileName column");
            }

            int bulkLoadFileCol = metadataColumns.indexOf(PhotoEntityProps.BULK_LOAD_FILE.uri());
            if (bulkLoadFileCol == -1) {
                bulkLoadFileCol = metadataColumns.size();
                metadataColumns.add(PhotoEntityProps.BULK_LOAD_FILE.uri());
            }

            List<String[]> data = new ArrayList<>();

            if (setExpedition) {
                expeditionCol = metadataColumns.size();
                metadataColumns.add(Record.EXPEDITION_CODE);
            }

            data.add(metadataColumns.toArray(new String[metadataColumns.size()]));

            for (String[] row : metadata) {
                if (row.length != metadataColumns.size()) {
                    row = Arrays.copyOf(row, metadataColumns.size());
                }

                if (setExpedition) {
                    row[expeditionCol] = getExpeditionCode(row[parentIdentifierCol]);
                }

                String fileName = row[fileNameCol];
                if (this.files.containsKey(fileName)) {
                    List<File> files = this.files.get(fileName);
                    if (files.size() > 1) {
                        row[bulkLoadFileCol] = files.remove(0).getAbsolutePath();
                    } else {
                        row[bulkLoadFileCol] = files.get(0).getAbsolutePath();
                    }
                }
                data.add(row);
            }

            // delete old metadata file
            metadataFile.delete();

            return writeMetadataFile(data);
        } catch (IOException e) {
            throw new FimsRuntimeException(FileCode.READ_ERROR, 400, METADATA_FILE_NAME);
        }
    }

    private File generateMetadataFile() {
        List<String[]> data = new ArrayList<>();
        Set<String> knownParentIdentifiers = fetchKnownParentIdentifiers();

        data.add(new String[]{
                parentEntity.getUniqueKey(),
                PhotoEntityProps.PHOTO_ID.uri(),
                PhotoEntityProps.BULK_LOAD_FILE.uri(),
                PhotoEntityProps.FILENAME.uri(),
                Record.EXPEDITION_CODE
        });

        for (Map.Entry<String, List<File>> entry : files.entrySet()) {
            Matcher matcher = FILE_PATTERN.matcher(entry.getKey());

            if (matcher.matches()) {
                String parentIdentifier = matcher.group(1);
                if (!knownParentIdentifiers.contains(parentIdentifier)) {
                    invalidFiles.add("Filename \"" + entry.getKey() + "\" does not match a known " +
                            parentEntity.getConceptAlias() + " name");
                    deleteImages(entry.getValue());
                    continue;
                }

                // setting id to empty string here will cause an id to be generated
                String photoId = this.ignoreId && ((PhotoEntity) this.entity).isGenerateID() ? "" : matcher.group(2);

                for (File f : entry.getValue()) {
                    data.add(new String[]{
                            parentIdentifier,
                            photoId,
                            f.getAbsolutePath(),
                            entry.getKey(),
                            getExpeditionCode(parentIdentifier)
                    });
                }

            } else {
                invalidFiles.add("Filename \"" + entry.getKey() + "\" does not follow file naming convention <parentIdentifier>+<photoID>.<ext>");
                deleteImages(entry.getValue());
            }
        }

        return writeMetadataFile(data);
    }

    private File writeMetadataFile(List<String[]> data) {
        CSVParser parser = new CSVParserBuilder()
                .withSeparator(',')
                .withIgnoreLeadingWhiteSpace(true)
                .build();

        File file = FileUtils.createUniqueFile(METADATA_FILE_NAME, System.getProperty("java.io.tmpdir"));

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(file)))) {

            ICSVWriter csvwriter = new CSVWriterBuilder(writer)
                    .withParser(parser)
                    .build();

            csvwriter.writeAll(data);

        } catch (IOException e) {
            throw new FimsRuntimeException(FileCode.WRITE_ERROR, 500);
        }

        return file;
    }

    private String getExpeditionCode(String parentIdentifier) {
        if (expeditionCode != null) {
            return expeditionCode;
        }

        if (parentExpeditionCodes == null) {
            fetchParentRecords();
        }
        return parentExpeditionCodes.getOrDefault(parentIdentifier, "");
    }

    private void fetchParentRecords() {
        parentExpeditionCodes = new HashMap<>();

        recordRepository.getRecords(project, parentEntity.getConceptAlias(), GenericRecord.class)
                .forEach(r -> parentExpeditionCodes.put(r.get(parentEntity.getUniqueKeyURI()), r.expeditionCode()));
    }

    private void extractFiles() {
        Path rootDir = Paths.get(photosDir, BULK_PHOTO_DIR);
        // make rootDir if necessary
        rootDir.toFile().mkdir();

        try {
            ZipEntry ze = stream.getNextEntry();

            String zipRootDir = "";
            List<String> fileSuffixes = Arrays.asList("jpg", "jpeg", "png");

            byte[] buffer = new byte[8192];
            while (ze != null) {
                String entryName = normalizeZipEntryName(ze.getName());

                // Ignore common macOS metadata files/directories generated by Finder.
                if (isMacOsMetadataEntry(entryName)) {
                    ze = stream.getNextEntry();
                    continue;
                }

                // Use the first top-level directory as an optional archive root.
                if (zipRootDir.isEmpty() && ze.isDirectory() && isTopLevelDirectory(entryName)) {
                    zipRootDir = entryName;
                    ze = stream.getNextEntry();
                    continue;
                }

                String fileName = entryName;
                if (!zipRootDir.isEmpty() && fileName.startsWith(zipRootDir)) {
                    fileName = fileName.substring(zipRootDir.length());
                }

                // If no explicit root directory entry exists, infer one from first-level paths.
                if (zipRootDir.isEmpty() && !ze.isDirectory() && fileName.contains("/")) {
                    int firstSlash = fileName.indexOf('/') + 1;
                    String candidateRoot = fileName.substring(0, firstSlash);
                    String remainder = fileName.substring(firstSlash);
                    if (!remainder.contains("/")) {
                        zipRootDir = candidateRoot;
                        fileName = remainder;
                    }
                }

                String ext = FileUtils.getExtension(fileName, "");
                String baseName = fileName.contains("/") ? fileName.substring(fileName.lastIndexOf('/') + 1) : fileName;

                // Ignore nested directories, unsupported extensions, and AppleDouble resource files.
                if (ze.isDirectory() ||
                        fileName.contains("/") ||
                        baseName.startsWith("._") ||
                        (!fileSuffixes.contains(ext.toLowerCase()) && !METADATA_FILE_NAME.equals(fileName))) {
                    logger.info("ignoring dir/unsupported file: " + entryName);

                    if (!baseName.startsWith("._")) {
                        invalidFiles.add(entryName);
                    }

                    ze = stream.getNextEntry();
                    continue;
                }

                File file = FileUtils.createUniqueFile(StringGenerator.generateString(20) + "." + ext, rootDir.toString());

                logger.debug("unzipping file: " + fileName + " to: " + file.getAbsolutePath());

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    int len;
                    while ((len = stream.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }

                    files.computeIfAbsent(fileName, k -> new ArrayList<>()).add(file);
                } catch (Exception e) {
                    logger.debug("Failed to extract file", e);
                    invalidFiles.add(entryName);
                }

                ze = stream.getNextEntry();
            }
        } catch (IOException e) {
            throw new BadRequestException("invalid/corrupt zip file", e);

        }
    }

    private boolean isMacOsMetadataEntry(String entryName) {
        if (entryName.startsWith("__MACOSX/")) return true;
        String baseName = entryName.contains("/") ? entryName.substring(entryName.lastIndexOf('/') + 1) : entryName;
        return ".DS_Store".equals(baseName) || baseName.startsWith("._");
    }

    private boolean isTopLevelDirectory(String entryName) {
        return entryName.endsWith("/") && entryName.indexOf('/') == entryName.length() - 1;
    }

    private String normalizeZipEntryName(String entryName) {
        String normalized = entryName.replace('\\', '/');
        return normalized.startsWith("./") ? normalized.substring(2) : normalized;
    }

    private Set<String> fetchKnownParentIdentifiers() {
        Set<String> identifiers = new HashSet<>();

        List<? extends Record> records;
        if (expeditionCode != null) {
            records = recordRepository.getRecords(
                    project,
                    expeditionCode,
                    parentEntity.getConceptAlias(),
                    GenericRecord.class
            );
        } else {
            records = recordRepository.getRecords(
                    project,
                    parentEntity.getConceptAlias(),
                    GenericRecord.class
            );
        }

        for (Record record : records) {
            String identifier = record.get(parentEntity.getUniqueKeyURI());
            if (identifier != null && !identifier.trim().isEmpty()) {
                identifiers.add(identifier);
            }
        }

        return identifiers;
    }

    private void deleteImages(List<File> images) {
        for (File img : images) {
            try {
                img.delete();
            } catch (Exception exp) {
                logger.debug("Failed to delete bulk loaded img file", exp);
            }
        }
    }

    public static class Builder {
        // required
        private String photosDir;
        private ZipInputStream stream;
        private Project project;
        private User user;
        private Entity entity;
        private RecordRepository recordRepository;

        // optional
        private String expeditionCode;
        private boolean ignoreId = false;

        public Builder photosDir(String photosDir) {
            this.photosDir = photosDir;
            return this;
        }

        public Builder stream(ZipInputStream stream) {
            this.stream = stream;
            return this;
        }

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public Builder project(Project project) {
            this.project = project;
            return this;
        }

        public Builder expeditionCode(String expeditionCode) {
            this.expeditionCode = expeditionCode;
            return this;
        }

        public Builder entity(Entity entity) {
            this.entity = entity;
            return this;
        }

        public Builder recordRepository(RecordRepository recordRepository) {
            this.recordRepository = recordRepository;
            return this;
        }

        public Builder ignoreId(boolean ignoreId) {
            this.ignoreId = ignoreId;
            return this;
        }

        private boolean isValid() {
            return photosDir != null &&
                    stream != null &&
                    recordRepository != null &&
                    entity != null &&
                    user != null &&
                    project != null;
        }

        public BulkPhotoPackage build() {
            if (!isValid()) {
                throw new ServerErrorException("Server Error", "photosDir, stream, recordRepository, entity, user, and project are all required");
            }
            return new BulkPhotoPackage(this);
        }
    }
}

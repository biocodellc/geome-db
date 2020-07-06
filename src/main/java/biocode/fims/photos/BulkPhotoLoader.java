package biocode.fims.photos;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.DataReaderCode;
import biocode.fims.models.Project;
import biocode.fims.reader.DataConverterFactory;
import biocode.fims.reader.DataReaderFactory;
import biocode.fims.reader.TabularDataReaderType;
import biocode.fims.records.RecordMetadata;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.rest.responses.UploadResponse;
import biocode.fims.run.DatasetAction;
import biocode.fims.run.DatasetAuthorizer;
import biocode.fims.run.DatasetProcessor;
import biocode.fims.run.ProcessorStatus;
import biocode.fims.validation.RecordValidatorFactory;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static biocode.fims.fimsExceptions.errorCodes.DataReaderCode.NO_DATA;
import static biocode.fims.reader.plugins.DelimitedTextReader.SHEET_NAME_KEY;

/**
 * @author rjewing
 */
public class BulkPhotoLoader {

    private final DataReaderFactory readerFactory;
    private final RecordValidatorFactory validatorFactory;
    private final RecordRepository recordRepository;
    private final DataConverterFactory dataConverterFactory;
    private final DatasetAuthorizer datasetAuthorizer;
    private final List<DatasetAction> datasetActions;
    private final FimsProperties props;

    @Autowired
    public BulkPhotoLoader(DataReaderFactory readerFactory, RecordValidatorFactory validatorFactory,
                           RecordRepository recordRepository, DataConverterFactory dataConverterFactory,
                           DatasetAuthorizer datasetAuthorizer, List<DatasetAction> datasetActions,
                           FimsProperties props) {
        this.readerFactory = readerFactory;
        this.validatorFactory = validatorFactory;
        this.recordRepository = recordRepository;
        this.dataConverterFactory = dataConverterFactory;
        this.datasetAuthorizer = datasetAuthorizer;
        this.datasetActions = datasetActions;
        this.props = props;
    }

    public UploadResponse process(BulkPhotoPackage photoPackage) {
        File metadataFile = photoPackage.metadataFile();

        Project project = photoPackage.project();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put(SHEET_NAME_KEY, photoPackage.entity().getWorksheet());

        DatasetProcessor processor = new DatasetProcessor.Builder(project, photoPackage.expeditionCode(), new ProcessorStatus())
                .user(photoPackage.user())
                .readerFactory(readerFactory)
                .dataConverterFactory(dataConverterFactory)
                .recordRepository(recordRepository)
                .validatorFactory(validatorFactory)
                .datasetAuthorizer(datasetAuthorizer)
                .serverDataDir(props.serverRoot())
                .datasetActions(datasetActions)
                .uploadValid()
                .addDataset(metadataFile.getAbsolutePath(), new RecordMetadata(TabularDataReaderType.READER_TYPE, false, metadata))
                .build();

        boolean isvalid = false;
        try {
            isvalid = processor.validate();

            processor.upload();
        } catch (FimsRuntimeException e) {
            if (!e.getErrorCode().equals(DataReaderCode.NO_DATA)) {
                throw e;
            }
        }

        ArrayList<String> invalidFiles = photoPackage.invalidFiles();
        EntityMessages entityMessages = new EntityMessages(photoPackage.entity().getConceptAlias());
        if (!isvalid || invalidFiles.size() > 0) {
            String conceptAlias = photoPackage.entity().getConceptAlias();

            entityMessages = processor.messages().stream()
                    .filter(em -> conceptAlias.equals(em.conceptAlias()))
                    .findFirst()
                    .orElse(new EntityMessages(conceptAlias));

            String groupMessage = "Invalid File(s)";
            for (String f : invalidFiles) {
                entityMessages.addErrorMessage(groupMessage, new Message(f));
            }
        }

        return new UploadResponse(true, entityMessages);
    }
}

package biocode.fims.rest.services.rest;

import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.Mapping;
import biocode.fims.dipnet.entities.DipnetExpedition;
import biocode.fims.dipnet.entities.FastqMetadata;
import biocode.fims.dipnet.services.DipnetExpeditionService;
import biocode.fims.dipnet.sra.DipnetBioSampleMapper;
import biocode.fims.dipnet.sra.DipnetSraMetadataMapper;
import biocode.fims.entities.Bcid;
import biocode.fims.fileManagers.dataset.Dataset;
import biocode.fims.fileManagers.dataset.DatasetFileManager;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.rest.FimsService;
import biocode.fims.run.ProcessController;
import biocode.fims.service.ExpeditionService;
import biocode.fims.service.OAuthProviderService;
import biocode.fims.settings.SettingsManager;
import biocode.fims.sra.BioSampleMapper;
import biocode.fims.sra.SraFileGenerator;
import biocode.fims.sra.SraMetadataGenerator;
import biocode.fims.sra.SraMetadataMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.xml.crypto.Data;
import java.io.File;

/**
 * REST interface calls for working with expeditions.
 */
@Controller
@Path("expeditions")
public class Expeditions extends FimsService {

    private static Logger logger = LoggerFactory.getLogger(ExpeditionRestService.class);
    private final DipnetExpeditionService expeditionService;
    private final DatasetFileManager datasetFileManager;

    @Autowired
    public Expeditions(DipnetExpeditionService expeditionService, DatasetFileManager datasetFileManager,
                       OAuthProviderService providerService, SettingsManager settingsManager) {
        super(providerService, settingsManager);
        this.expeditionService = expeditionService;
        this.datasetFileManager = datasetFileManager;
    }
    @GET
    @Path("/{expeditionId}/sra/files")
    @Produces("application/zip")
    public Response getSraFiles(@PathParam("expeditionId") int expeditionId) {
        DipnetExpedition expedition = expeditionService.getDipnetExpedition(expeditionId);

        if (expedition == null || expedition.getFastqMetadata() == null) {
            throw new BadRequestException("Either the dataset and/or fastq metadata do not exist");
        }
        File configFile = new ConfigurationFileFetcher(expedition.getExpedition().getProject().getProjectId(), uploadPath(), false).getOutputFile();

        Mapping mapping = new Mapping();
        mapping.addMappingRules(configFile);

        ProcessController processController = new ProcessController(expedition.getExpedition().getProject().getProjectId(), expedition.getExpedition().getExpeditionCode());
        processController.setOutputFolder(uploadPath());
        processController.setMapping(mapping);
        datasetFileManager.setProcessController(processController);
        Dataset dataset = datasetFileManager.getDataset();

        Bcid entityBcid = expedition.getExpedition().getEntityBcids().get(0);

        SraMetadataMapper metadataMapper = new DipnetSraMetadataMapper(expedition.getFastqMetadata(), dataset.getSamples());
        BioSampleMapper bioSampleMapper = new DipnetBioSampleMapper(
                dataset.getSamples(),
                expedition.getFastqMetadata().getLibraryStrategy(),
                entityBcid.getIdentifier().toString());

        File file = SraFileGenerator.generateFiles(bioSampleMapper, metadataMapper, uploadPath());

        Response.ResponseBuilder response = Response.ok(file, "application/zip");
        response.header("Content-Disposition",
                    "attachment; filename=sra-files.zip");

        return response.build();
    }
}

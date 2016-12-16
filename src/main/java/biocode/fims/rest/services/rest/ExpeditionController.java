package biocode.fims.rest.services.rest;

import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.Mapping;
import biocode.fims.dipnet.entities.DipnetExpedition;
import biocode.fims.dipnet.services.DipnetExpeditionService;
import biocode.fims.dipnet.sra.DipnetBioSampleMapper;
import biocode.fims.dipnet.sra.DipnetSraMetadataMapper;
import biocode.fims.entities.Bcid;
import biocode.fims.fileManagers.fimsMetadata.FimsMetadataFileManager;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.run.ProcessController;
import biocode.fims.service.ExpeditionService;
import biocode.fims.settings.SettingsManager;
import biocode.fims.sra.*;
import biocode.fims.utils.FileUtils;
import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * REST interface calls for working with expeditions.
 */
@Controller
@Path("expeditions")
public class ExpeditionController extends FimsAbstractExpeditionController {
    private static Logger logger = LoggerFactory.getLogger(ExpeditionController.class);

    private final DipnetExpeditionService dipnetExpeditionService;
    private final FimsMetadataFileManager fimsMetadataFileManager;

    @Autowired
    public ExpeditionController(DipnetExpeditionService dipnetExpeditionService, FimsMetadataFileManager fimsMetadataFileManager,
                                ExpeditionService expeditionService, SettingsManager settingsManager) {
        super(expeditionService, settingsManager);
        this.dipnetExpeditionService = dipnetExpeditionService;
        this.fimsMetadataFileManager = fimsMetadataFileManager;
    }
    @GET
    @Path("/{expeditionId}/sra/files")
    @Produces("application/zip")
    public Response getSraFiles(@PathParam("expeditionId") int expeditionId) {
        DipnetExpedition expedition = dipnetExpeditionService.getDipnetExpedition(expeditionId);

        if (expedition == null || expedition.getFastqMetadata() == null) {
            throw new BadRequestException("Either the fims metadata and/or fastq metadata do not exist");
        }
        File configFile = new ConfigurationFileFetcher(expedition.getExpedition().getProject().getProjectId(), uploadPath(), true).getOutputFile();

        Mapping mapping = new Mapping();
        mapping.addMappingRules(configFile);

        ProcessController processController = new ProcessController(expedition.getExpedition().getProject().getProjectId(), expedition.getExpedition().getExpeditionCode());
        processController.setOutputFolder(uploadPath());
        processController.setMapping(mapping);
        fimsMetadataFileManager.setProcessController(processController);
        JSONArray dataset = fimsMetadataFileManager.getDataset();

        Bcid entityBcid = expedition.getExpedition().getEntityBcids().get(0);

        SraMetadataMapper metadataMapper = new DipnetSraMetadataMapper(expedition.getFastqMetadata(), dataset);
        BioSampleMapper bioSampleMapper = new DipnetBioSampleMapper(
                dataset,
                expedition.getFastqMetadata().getLibraryStrategy(),
                entityBcid.getIdentifier().toString());

        File bioSampleFile = BioSampleAttributesGenerator.generateFile(bioSampleMapper, uploadPath());
        File sraMetadataFile = SraMetadataGenerator.generateFile(metadataMapper, uploadPath());

        Map<String, File> fileMap = new HashMap<>();
        fileMap.put("bioSample-attributes.tsv", bioSampleFile);
        fileMap.put("sra-attributes.tsv", sraMetadataFile);

        Response.ResponseBuilder response = Response.ok(FileUtils.zip(fileMap, uploadPath()), "application/zip");
        response.header("Content-Disposition",
                    "attachment; filename=sra-files.zip");

        return response.build();
    }
}

package biocode.fims.utils;

import biocode.fims.entities.Bcid;
import biocode.fims.entities.Expedition;
import biocode.fims.fuseki.query.Query;
import biocode.fims.rest.SpringObjectMapper;
import biocode.fims.service.BcidService;
import biocode.fims.service.ExpeditionService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * Handles dataset business logic
 */
public class DatasetService {

    private final ExpeditionService expeditionService;
    private final BcidService bcidService;

    @Autowired
    public DatasetService(ExpeditionService expeditionService, BcidService bcidService) {
        this.expeditionService = expeditionService;
        this.bcidService = bcidService;
    }

    public List<Map> listExpeditionDatasetsWithCounts(List<Expedition> expeditions, String fusekiQueryTarget) {
        List<String> graphs= new ArrayList<>();
        Set<Bcid> latestDatsets = bcidService.getLatestDatasetsForExpeditions(expeditions);

        for (Bcid bcid: latestDatsets)
            graphs.add(bcid.getGraph());

        Query fusekiQuery = new Query(fusekiQueryTarget);
        Map<String, Map<String, Integer>> counts = fusekiQuery.countIdentifiersAndSequences(graphs);

        List<Map> expeditionMapList = new SpringObjectMapper().convertValue(expeditions, List.class);

        for (Map expedition: expeditionMapList) {
            for (Bcid bcid: latestDatsets) {
                if (bcid.getExpedition().getExpeditionId() == Integer.parseInt(String.valueOf(expedition.get("expeditionId")))) {
                    Map<String, String> datasetMetadata = new HashMap<>();

                    Map<String, Integer> graphCounts = counts.get(bcid.getGraph());

                    datasetMetadata.put("bcidId", String.valueOf(bcid.getBcidId()));
                    datasetMetadata.put("identifierCount", String.valueOf(graphCounts.get("identifiers")));
                    datasetMetadata.put("sequenceCount", String.valueOf(graphCounts.get("sequences")));

                    expedition.put("latestDataset", datasetMetadata);
                    break;
                }
            }
        }

        return expeditionMapList;
    }
}

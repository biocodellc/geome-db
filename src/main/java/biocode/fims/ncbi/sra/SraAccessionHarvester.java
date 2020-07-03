package biocode.fims.ncbi.sra;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.bcid.BcidBuilder;
import biocode.fims.bcid.Identifier;
import biocode.fims.config.Config;
import biocode.fims.config.models.Entity;
import biocode.fims.config.models.FastqEntity;
import biocode.fims.fastq.FastqRecord;
import biocode.fims.fimsExceptions.FimsAbstractException;
import biocode.fims.fimsExceptions.errorCodes.ConfigCode;
import biocode.fims.models.Project;
import biocode.fims.records.GenericRecord;
import biocode.fims.records.Record;
import biocode.fims.records.RecordJoiner;
import biocode.fims.records.RecordSet;
import biocode.fims.ncbi.entrez.BioSampleRepository;
import biocode.fims.ncbi.models.BioSample;
import biocode.fims.query.QueryResult;
import biocode.fims.query.QueryResults;
import biocode.fims.query.dsl.Query;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.run.Dataset;
import biocode.fims.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * class for harvesting accession's for fimMetadata resource sequences submitted the NCBI SRA database
 *
 * @author rjewing
 */
public class SraAccessionHarvester {
    private static final Logger logger = LoggerFactory.getLogger(SraAccessionHarvester.class);

    private final BioSampleRepository bioSampleRepository;
    private final ProjectService projectService;
    private final RecordRepository recordRepository;
    private final FimsProperties props;

    public SraAccessionHarvester(RecordRepository recordRepository, BioSampleRepository bioSampleRepository,
                                 ProjectService projectService, FimsProperties props) {
        this.props = props;
        Assert.notNull(bioSampleRepository);
        Assert.notNull(recordRepository);
        this.projectService = projectService;
        this.recordRepository = recordRepository;
        this.bioSampleRepository = bioSampleRepository;
    }

//    @Scheduled(cron = "${sraCronSetting}")
    public void harvestForAllProjects() {
        for (Project project : projectService.getProjects()) {
            harvest(project);
        }
    }

    private void harvest(Project project) {
        try {
            Config config = project.getProjectConfig();

            config.entities().stream()
                    .filter(e -> e.type().equals(FastqEntity.TYPE))
                    .forEach(e -> {
                        // TODO need to make the select query configurable
                        String q = "_select_:[Sample,Tissue] not _exists_:bioSample and _projects_:" + project.getProjectId();

                        Query query = Query.build(project, e.getConceptAlias(), q);
                        QueryResults queryResults = recordRepository.query(query);

                        if (queryResults.isEmpty()) {
                            return;
                        }

                        Entity parentEntity = config.entity(e.getParentEntity());
                        RecordJoiner joiner = new RecordJoiner(config, e, queryResults);

                        QueryResult result = queryResults.getResult(e.getConceptAlias());
                        // always use an empty prefix as we only match later on from the ark:/ after
                        BcidBuilder bcidBuilder = new BcidBuilder(parentEntity, config.entity(parentEntity.getParentEntity()), "");

                        List<String> bcidsToQuery = result.records().stream()
                                .map(r -> {
                                    Record parent = joiner.getParent(parentEntity.getConceptAlias(), r);
                                    return bcidBuilder.build(parent);
                                })
                                .collect(Collectors.toList());
                        List<BioSample> bioSamples = bioSampleRepository.getBioSamples(bcidsToQuery);

                        if (bioSamples.isEmpty()) {
                            return;
                        }

                        Dataset d = generateUpdateDataset(result, parentEntity, bioSamples);
                        recordRepository.saveDataset(d, project.getNetwork().getId());

                        // avoid being rate limited
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    });
        } catch (FimsAbstractException e) {
            if (e.getErrorCode().equals(ConfigCode.MISSING_ATTRIBUTE)) {
                // skip this project if config is invalid
                logger.error(e.toString());
            } else {
                throw e;
            }
        }
    }

    private Dataset generateUpdateDataset(QueryResult fastqResults, Entity parentEntity, List<BioSample> bioSamples) {
        Map<String, RecordSet> recordSets = new HashMap<>();

        // parent RecordSet is needed so the recordRepository knows the parentIdentifier
        RecordSet parentRecordSet = new RecordSet(parentEntity, false);
        String parentUniqueKeyURI = parentEntity.getUniqueKeyURI();

        Map<String, Record> records = new HashMap<>();

        fastqResults.records().forEach(r -> {
            records.put(r.get(parentUniqueKeyURI), r);

            Map<String, Object> parentProps = new HashMap<>();
            parentProps.put(parentUniqueKeyURI, r.getAsObject(parentUniqueKeyURI));
            parentRecordSet.add(new GenericRecord(parentProps));
        });

        for (BioSample bioSample : bioSamples) {
            String bcid = bioSample.getBcid();
            if (bcid.startsWith(props.bcidResolverPrefix())) {
                bcid = bcid.substring(props.bcidResolverPrefix().length());
            }
            String parentIdentifier = new Identifier(bcid).getSuffix();

            FastqRecord record = (FastqRecord) records.get(parentIdentifier);
            record.setBioSample(bioSample);

            recordSets.computeIfAbsent(
                    record.expeditionCode(),
                    k -> {
                        RecordSet recordSet = new RecordSet(fastqResults.entity(), false);
                        recordSet.setParent(parentRecordSet);
                        return recordSet;
                    }
            ).add(record);
        }

        return new Dataset(new ArrayList<>(recordSets.values()));
    }
}

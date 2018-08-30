package biocode.fims.application.config;

import biocode.fims.config.models.PhotoEntity;
import biocode.fims.fasta.FastaRecord;
import biocode.fims.fasta.FastaValidator;
import biocode.fims.fasta.reader.FastaDataReaderType;
import biocode.fims.fasta.reader.FastaReader;
import biocode.fims.fastq.FastqRecord;
import biocode.fims.fastq.FastqRecordRowMapper;
import biocode.fims.fastq.FastqValidator;
import biocode.fims.fastq.reader.FastqDataReaderType;
import biocode.fims.fastq.reader.FastqReader;
import biocode.fims.records.FimsRowMapper;
import biocode.fims.records.GenericRecord;
import biocode.fims.records.GenericRecordRowMapper;
import biocode.fims.records.Record;
import biocode.fims.photos.PhotoRecord;
import biocode.fims.photos.PhotoValidator;
import biocode.fims.photos.processing.PhotoProcessingTaskExecutor;
import biocode.fims.photos.processing.PhotoProcessingTaskScheduler;
import biocode.fims.photos.reader.PhotoConverter;
import biocode.fims.reader.*;
import biocode.fims.reader.plugins.CSVReader;
import biocode.fims.reader.plugins.ExcelReader;
import biocode.fims.reader.plugins.TabReader;
import biocode.fims.repositories.PostgresRecordRepository;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.service.NetworkService;
import biocode.fims.service.ProjectService;
import biocode.fims.validation.RecordValidator;
import biocode.fims.validation.RecordValidatorFactory;
import biocode.fims.validation.ValidatorInstantiator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.*;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;

import javax.ws.rs.client.ClientBuilder;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Configuration class for and GeOMe-db-Fims application. Including cli and webapps
 */
@Configuration
@Import({FimsAppConfig.class, PhotosAppConfig.class})
// declaring this here allows us to override any properties that are also included in geome-db.props
@PropertySource(value = "classpath:biocode-fims.props", ignoreResourceNotFound = true)
@PropertySource("classpath:geome-db.props")
public class GeomeAppConfig {
    @Autowired
    FimsAppConfig fimsAppConfig;
    @Autowired
    PhotosAppConfig photosAppConfig;

    @Autowired
    NetworkService networkService;
    @Autowired
    PhotosProperties photosProperties;


    @Bean
    public DataReaderFactory dataReaderFactory() {
        Map<DataReader.DataReaderType, List<DataReader>> dataReaders = new HashMap<>();

        dataReaders.put(
                TabularDataReaderType.READER_TYPE,
                Arrays.asList(new CSVReader(), new TabReader(), new ExcelReader())
        );
        dataReaders.put(
                FastaDataReaderType.READER_TYPE,
                Collections.singletonList(new FastaReader())
        );
        dataReaders.put(
                FastqDataReaderType.READER_TYPE,
                Collections.singletonList(new FastqReader())
        );

        return new DataReaderFactory(dataReaders);
    }

    @Bean
    public DataConverterFactory dataConverterFactory() {
        Map<String, DataConverter> dataConverters = new HashMap<>();
        dataConverters.put(PhotoEntity.TYPE, new PhotoConverter(photosAppConfig.photosSql(), recordRepository()));

        return new DataConverterFactory(dataConverters);
    }

    @Bean
    public RecordValidatorFactory recordValidatorFactory() {
        Map<Class<? extends Record>, ValidatorInstantiator> validators = new HashMap<>();

        validators.put(GenericRecord.class, new RecordValidator.DefaultValidatorInstantiator());
        validators.put(FastaRecord.class, new FastaValidator.FastaValidatorInstantiator());
        validators.put(FastqRecord.class, new FastqValidator.FastqValidatorInstantiator());
        validators.put(PhotoRecord.class, new PhotoValidator.PhotoValidatorInstantiator());

        return new RecordValidatorFactory(validators);
    }

    @Bean
    public RecordRepository recordRepository() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("record-repository-sql.yml"));

        Map<Class<? extends Record>, FimsRowMapper<? extends Record>> rowMappers = new HashMap<>();
        rowMappers.put(GenericRecord.class, new GenericRecordRowMapper());
        rowMappers.put(FastqRecord.class, new FastqRecordRowMapper());

        return new PostgresRecordRepository(fimsAppConfig.jdbcTemplate, yaml.getObject(), rowMappers);
    }

    @Bean
    public GeomeSql geomeSql() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("geome-sql.yml"));
        return new GeomeSql(yaml.getObject());
    }

    @Bean
    public PhotoProcessingTaskScheduler photoProcessingTaskScheduler() {
        PhotoProcessingTaskExecutor executor = new PhotoProcessingTaskExecutor(recordRepository(), Executors.newFixedThreadPool(5));
        return new PhotoProcessingTaskScheduler(networkService, recordRepository(), photosAppConfig.photosSql(), executor, ClientBuilder.newClient(), photosProperties);
    }

//    @Bean
//    public SraAccessionHarvester sraAccessionHarvester() {
//        return new SraAccessionHarvester(geomeResourceRepository(), bioSampleRepository(), projectService, geomeProperties());
//    }

    @Primary
    @Bean
    public GeomeProperties geomeProperties(ConfigurableEnvironment env) {
        return new GeomeProperties(env);
    }

}

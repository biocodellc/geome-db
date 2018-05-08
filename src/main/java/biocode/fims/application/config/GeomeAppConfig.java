package biocode.fims.application.config;

import biocode.fims.fasta.FastaRecord;
import biocode.fims.fasta.FastaValidator;
import biocode.fims.fasta.reader.FastaDataReaderType;
import biocode.fims.fasta.reader.FastaReader;
import biocode.fims.fastq.FastqRecord;
import biocode.fims.fastq.FastqRecordRowMapper;
import biocode.fims.fastq.FastqValidator;
import biocode.fims.fastq.reader.FastqDataReaderType;
import biocode.fims.fastq.reader.FastqReader;
import biocode.fims.models.records.FimsRowMapper;
import biocode.fims.models.records.GenericRecord;
import biocode.fims.models.records.GenericRecordRowMapper;
import biocode.fims.models.records.Record;
import biocode.fims.reader.DataReader;
import biocode.fims.reader.DataReaderFactory;
import biocode.fims.reader.TabularDataReaderType;
import biocode.fims.reader.plugins.CSVReader;
import biocode.fims.reader.plugins.ExcelReader;
import biocode.fims.reader.plugins.TabReader;
import biocode.fims.repositories.PostgresRecordRepository;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.service.ProjectService;
import biocode.fims.validation.RecordValidator;
import biocode.fims.validation.RecordValidatorFactory;
import biocode.fims.validation.ValidatorInstantiator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;

import java.util.*;

/**
 * Configuration class for and GeOMe-db-Fims application. Including cli and webapps
 */
@Configuration
@Import({FimsAppConfig.class, ElasticSearchAppConfig.class})
// declaring this here allows us to override any properties that are also included in geome-db.props
@PropertySource(value = "classpath:biocode-fims.props", ignoreResourceNotFound = true)
@PropertySource("classpath:geome-db.props")
public class GeomeAppConfig {
    @Autowired
    FimsAppConfig fimsAppConfig;

    @Autowired
    ProjectService projectService;

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
    public RecordValidatorFactory recordValidatorFactory() {
        Map<Class<? extends Record>, ValidatorInstantiator> validators = new HashMap<>();

        validators.put(GenericRecord.class, new RecordValidator.DefaultValidatorInstantiator());
        validators.put(FastaRecord.class, new FastaValidator.FastaValidatorInstantiator());
        validators.put(FastqRecord.class, new FastqValidator.FastqValidatorInstantiator());

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

//    @Bean
//    public SraAccessionHarvester sraAccessionHarvester() {
//        return new SraAccessionHarvester(geomeResourceRepository(), bioSampleRepository(), projectService, geomeProperties());
//    }

}

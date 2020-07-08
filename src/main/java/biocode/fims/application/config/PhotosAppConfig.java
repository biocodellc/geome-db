package biocode.fims.application.config;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

/**
 * Configuration class for biocode-fims-photos application.
 */
@Configuration
@PropertySource(value = "classpath:biocode-fims-photos.props", ignoreResourceNotFound = true)
@Import({PhotosProperties.class})
public class PhotosAppConfig {
    @Bean
    public PhotosSql photosSql() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("biocode-fims-photos-sql.yml"));
        return new PhotosSql(yaml.getObject());
    }
}

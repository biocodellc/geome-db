package biocode.fims.rest;

import biocode.fims.application.config.SettingsManagerConfig;
import biocode.fims.rest.versioning.VersionTransformer;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.springframework.context.annotation.*;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.Collections;
import java.util.List;

/**
 * @author rjewing
 */
@Configuration
@Import({SettingsManagerConfig.class})
@ComponentScan(basePackages = {"biocode.fims.service", "biocode.fims.rest", VersionTransformer.TRANSFORMER_PACKAGE})
@ImportResource({
        "classpath:test-data-access-config.xml"
})
@EnableAspectJAutoProxy
public class BiscicolFimsRestTestAppConfig extends FimsRestTestAppConfig {

    @Bean
    public WebTargetFactoryBean webTargetFactoryBean() {
        WebTargetFactoryBean webTargetFactoryBean = super.webTargetFactoryBean();
        List<Class<?>> componentClasses = webTargetFactoryBean.getComponentClasses();
        componentClasses.add(MultiPartFeature.class);

        webTargetFactoryBean.setComponentClasses(componentClasses);

        webTargetFactoryBean.setComponentPackages(Collections.singletonList("biocode.fims.rest.services.rest"));
        return  webTargetFactoryBean;
    }
}

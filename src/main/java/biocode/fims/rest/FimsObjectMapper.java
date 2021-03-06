package biocode.fims.rest;

import biocode.fims.serializers.FimsSerializerModifier;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;

/**
 * Jackson ObjectMapper with hibernate support & JsonView
 */
@Component
public class FimsObjectMapper extends ObjectMapper {

    public FimsObjectMapper() {
        Hibernate5Module hm = new Hibernate5Module();
        hm.configure(Hibernate5Module.Feature.USE_TRANSIENT_ANNOTATION, false);
        this.registerModule(hm);
        this.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        this.registerModule(
                new SimpleModule()
                        .setSerializerModifier(new FimsSerializerModifier())
        );
        this.registerModule(new ParameterNamesModule());
        this.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
    }

}

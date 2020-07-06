package biocode.fims.plugins.evolution.application.config;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import org.postgresql.util.Base64;
import org.springframework.core.env.Environment;

import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * @author rjewing
 */
public class EvolutionProperties {
    private final Environment env;

    public EvolutionProperties(Environment env) {
        this.env = env;
    }

    public String api() {
        return env.getRequiredProperty("evolutionApi");
    }

    public String resolverEndpoint() {
        String url = env.getRequiredProperty("evolutionRecordResolverEndpoint");

        if (!url.endsWith("/")) {
            url += "/";
        }

        return url;
    }

    public String clientId() {
        return env.getRequiredProperty("evolutionClientID");
    }

    public ECPrivateKey clientSK() {
        String pemKey = env.getRequiredProperty("evolutionClientSK");

        try {
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.decode(pemKey));
            KeyFactory kf = KeyFactory.getInstance("EC");
            return (ECPrivateKey) kf.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new FimsRuntimeException("Failed to load evolutionClientSK", 500, e);
        }
    }
}

package biocode.fims.plugins.evolution.service;

import biocode.fims.api.services.AbstractRequest;
import biocode.fims.api.services.RateLimiter;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.plugins.evolution.application.config.EvolutionProperties;
import biocode.fims.plugins.evolution.models.EvolutionRecord;
import biocode.fims.plugins.evolution.models.EvolutionRecordReference;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.security.interfaces.ECPrivateKey;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * @author rjewing
 */
public class EvolutionService {
    private static final Logger logger = LoggerFactory.getLogger(EvolutionService.class);

    private final Client client;
    private final EvolutionProperties evolutionProps;
    private String accessToken;
    private boolean triedRefresh;
    private RateLimiter rateLimiter;

    public EvolutionService(Client client, EvolutionProperties evolutionProps) {
        this.client = client;
        this.evolutionProps = evolutionProps;
        this.rateLimiter = RateLimiter.create(8);
    }

    public void create(List<EvolutionRecord> records) {
        if (records.size() == 0) return;

        if (!authenticated() && !authenticate()) {
            throw new FimsRuntimeException("Unable to authenticate with the Evolution API", 500);
        }

        executeRequest(new CreateRequest(records));
    }

    public void update(List<EvolutionRecord> records) {
        if (records.size() == 0) return;

        if (!authenticated() && !authenticate()) {
            throw new FimsRuntimeException("Unable to authenticate with the Evolution API", 500);
        }

        executeRequest(new UpdateRequest(records));
    }

    public void discovery(List<EvolutionRecordReference> records) {
        if (records.size() == 0) return;

        if (!authenticated() && !authenticate()) {
            throw new FimsRuntimeException("Unable to authenticate with the Evolution API", 500);
        }

        executeRequest(new DiscoveryRequest(records));
    }

    public void retrieval(List<EvolutionRecordReference> records) {
        if (records.size() == 0) return;

        if (!authenticated() && !authenticate()) {
            throw new FimsRuntimeException("Unable to authenticate with the Evolution API", 500);
        }

        executeRequest(new RetrievalRequest(records));
    }

    private <T> T executeRequest(AbstractRequest<T> request) {
        try {
            request.addHeader("Authorization", "Bearer " + accessToken);
            this.rateLimiter.consume();
            return request.execute();
        } catch (WebApplicationException e) {
            if (e instanceof NotAuthorizedException && !triedRefresh) {
                this.triedRefresh = true;
                if (authenticate()) {
                    this.triedRefresh = false;
                    return executeRequest(request);
                }
                // always reset to false so we can retry on every request
                this.triedRefresh = false;
                logger.error("Failed to execute request because authentication failed.");
            }
            throw e;
        } catch (InterruptedException e) {
            throw new FimsRuntimeException(500, e);
        }
    }

    private boolean authenticated() {
        return this.accessToken != null;
    }

    private boolean authenticate() {
        try {
            AccessToken result = new AuthenticateRequest().execute();
            this.accessToken = result.accessToken;
        } catch (WebApplicationException e) {
            logger.error("Evolution API authentication error", e);
            return false;
        }

        this.triedRefresh = false;
        return true;
    }

    private final class CreateRequest extends AbstractRequest<List> {
        private static final String path = "/records";

        public CreateRequest(List<EvolutionRecord> records) {
            super("POST", List.class, client, path, evolutionProps.api());

            this.setHttpEntity(Entity.entity(records, MediaType.APPLICATION_JSON));
            setAccepts(MediaType.APPLICATION_JSON);
        }
    }

    private final class UpdateRequest extends AbstractRequest<List> {
        private static final String path = "/records";

        public UpdateRequest(List<EvolutionRecord> records) {
            super("PUT", List.class, client, path, evolutionProps.api());

            this.setHttpEntity(Entity.entity(records, MediaType.APPLICATION_JSON));
            setAccepts(MediaType.APPLICATION_JSON);
        }
    }

    private final class DiscoveryRequest extends AbstractRequest<List> {
        private static final String path = "/discoveries";

        public DiscoveryRequest(List<EvolutionRecordReference> records) {
            super("POST", List.class, client, path, evolutionProps.api());

            this.setHttpEntity(Entity.entity(records, MediaType.APPLICATION_JSON));
            setAccepts(MediaType.APPLICATION_JSON);
        }
    }

    private final class RetrievalRequest extends AbstractRequest<List> {
        private static final String path = "/retrievals";

        public RetrievalRequest(List<EvolutionRecordReference> records) {
            super("POST", List.class, client, path, evolutionProps.api());

            this.setHttpEntity(Entity.entity(records, MediaType.APPLICATION_JSON));
            setAccepts(MediaType.APPLICATION_JSON);
        }
    }

    private final class AuthenticateRequest extends AbstractRequest<AccessToken> {
        private static final String PATH = "/authentication";

        public AuthenticateRequest() {
            super("POST", AccessToken.class, client, PATH, evolutionProps.api());


            Map<String, String> data = new HashMap<>();
            data.put("client_id", evolutionProps.clientId());


            ECPrivateKey privateKey = evolutionProps.clientSK();

            try {
                Algorithm algorithm = Algorithm.ECDSA256(null, privateKey);

                LocalDateTime dateTime = LocalDateTime.now().plus(Duration.of(2, ChronoUnit.MINUTES));
                Date exp = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());

                String assertion = JWT.create()
                        .withIssuer(evolutionProps.clientId())
                        .withExpiresAt(exp)
                        .sign(algorithm);
                data.put("assertion", assertion);

                this.setHttpEntity(Entity.entity(data, MediaType.APPLICATION_JSON));
                setAccepts(MediaType.APPLICATION_JSON);
            } catch (JWTCreationException exception) {
                //Invalid Signing configuration / Couldn't convert Claims.
                logger.error("Failed to authenticate against Evolution API");
            }

        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AccessToken {
        @JsonProperty("accessToken")
        public String accessToken;
    }
}

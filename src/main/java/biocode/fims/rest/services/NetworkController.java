package biocode.fims.rest.services;

import biocode.fims.application.config.GeomeProperties;
import biocode.fims.config.network.NetworkConfig;
import biocode.fims.models.Network;
import biocode.fims.rest.Compress;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.UserEntityGraph;
import biocode.fims.rest.filters.Admin;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.rest.services.subResources.NetworkConfigurationResource;
import biocode.fims.rest.services.subResources.NetworksResource;
import biocode.fims.serializers.Views;
import biocode.fims.service.NetworkService;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * network API endpoints
 *
 * @resourceTag Network
 */
@Controller
@Path("network")
@Produces({MediaType.APPLICATION_JSON})
public class NetworkController extends FimsController {

    private final NetworkService networkService;
    private final GeomeProperties props;
    private NetworksResource networksResource;
    private final NetworkConfigurationResource networkConfigurationResource;

    @Autowired
    NetworkController(NetworkService networkService, GeomeProperties props,
                      NetworksResource networksResource, NetworkConfigurationResource networkConfigurationResource) {
        super(props);
        this.networkService = networkService;
        this.props = props;
        this.networksResource = networksResource;
        this.networkConfigurationResource = networkConfigurationResource;
    }

    /**
     * Fetch network
     */
    @Compress
    @JsonView(Views.DetailedConfig.class)
    @GET
    public Network getNetwork() {
        return networkService.getNetwork(props.networkId());
    }

    /**
     * Update the {@link Network}
     *
     * @param network The updated network object
     * @responseMessage 403 not the network's admin `biocode.fims.utils.ErrorInfo
     */
    @Compress
    @UserEntityGraph("User.withNetworks")
    @JsonView(Views.DetailedConfig.class)
    @PUT
    @Authenticated
    @Admin
    @Consumes(MediaType.APPLICATION_JSON)
    public Network updateNetwork(Network network) {
        return networksResource.updateNetwork(props.networkId(), network);
    }

    /**
     * Get a network config
     *
     * @return
     */
    @Compress
    @GET
    @Path("config")
    @Produces(MediaType.APPLICATION_JSON)
    public NetworkConfig getConfig() {
        return networkConfigurationResource.getConfig(props.networkId());
    }

    /**
     * Update the network config
     *
     * @param config The updated network object
     */
    @Compress
    @PUT
    @Path("config")
    @Authenticated
    @Admin
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(NetworkConfig config) {
        return networkConfigurationResource.update(props.networkId(), config);
    }
}


package org.openhab.binding.jsupla.handler;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.api.ApiClientFactory;
import pl.grzeslowski.jsupla.api.generated.ApiClient;
import pl.grzeslowski.jsupla.api.generated.ApiException;
import pl.grzeslowski.jsupla.api.generated.api.ServerApi;
import pl.grzeslowski.jsupla.api.generated.model.ServerInfo;

import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static org.eclipse.smarthome.core.thing.ThingStatus.OFFLINE;
import static org.eclipse.smarthome.core.thing.ThingStatus.ONLINE;
import static org.eclipse.smarthome.core.thing.ThingStatusDetail.CONFIGURATION_ERROR;
import static org.openhab.binding.jsupla.JSuplaBindingConstants.ADDRESS_CHANNEL_ID;
import static org.openhab.binding.jsupla.JSuplaBindingConstants.API_VERSION_CHANNEL_ID;
import static org.openhab.binding.jsupla.JSuplaBindingConstants.CLOUD_VERSION_CHANNEL_ID;
import static org.openhab.binding.jsupla.JSuplaBindingConstants.O_AUTH_TOKEN;

public class CloudBridgeHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(CloudBridgeHandler.class);
    private ApiClient bridgeApiClient;
    private String oAuthToken;

    public CloudBridgeHandler(final Bridge bridge) {
        super(bridge);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void initialize() {
        try {
            internalInitialize();
        } catch (Exception ex) {
            logger.error("Cannot start server!", ex);
            updateStatus(OFFLINE, CONFIGURATION_ERROR,
                    "Cannot start server! " + ex.getLocalizedMessage());
        }
    }

    private void internalInitialize() throws ApiException {
        // init bridge api client
        final Configuration config = this.getConfig();
        this.oAuthToken = (String) config.get(O_AUTH_TOKEN);
        this.bridgeApiClient = ApiClientFactory.INSTANCE.newApiClient(oAuthToken);
        bridgeApiClient.setDebugging(logger.isDebugEnabled());

        // get server info
        ServerApi serverApi = new ServerApi(bridgeApiClient);
        ServerInfo serverInfo = serverApi.getServerInfo();
        if (serverInfo == null) {
            updateStatus(OFFLINE, CONFIGURATION_ERROR,
                    "Cannot get server info from `" + bridgeApiClient.getBasePath() + "`!");
            return;
        }
        updateState(ADDRESS_CHANNEL_ID, new StringType(serverInfo.getAddress()));
        updateState(API_VERSION_CHANNEL_ID, new StringType(serverInfo.getApiVersion()));
        updateState(CLOUD_VERSION_CHANNEL_ID, new StringType(serverInfo.getCloudVersion()));

        // check if current api is supported
        String apiVersion = ApiClient.API_VERSION;
        List<String> supportedApiVersions = serverInfo.getSupportedApiVersions();
        if (!supportedApiVersions.contains(apiVersion)) {
            updateStatus(OFFLINE, CONFIGURATION_ERROR,
                    "This API version `" + apiVersion + "` is not supported! Supported api versions: [" +
                            String.join(", ", supportedApiVersions) + "].");
            return;
        }

        // done
        updateStatus(ONLINE);
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        // TODO implement refreshing
    }

    public Optional<ApiClient> getBridgeApiClient() {
        return ofNullable(bridgeApiClient);
    }

    public Optional<String> getoAuthToken() {
        return ofNullable(oAuthToken);
    }
}

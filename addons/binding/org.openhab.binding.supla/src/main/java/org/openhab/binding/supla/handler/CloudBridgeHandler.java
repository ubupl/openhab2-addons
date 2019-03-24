package org.openhab.binding.supla.handler;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.common.ThreadPoolManager;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.supla.internal.ReadWriteMonad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.api.generated.ApiClient;
import pl.grzeslowski.jsupla.api.generated.ApiException;
import pl.grzeslowski.jsupla.api.generated.api.ServerApi;
import pl.grzeslowski.jsupla.api.generated.model.ServerInfo;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.eclipse.smarthome.core.thing.ThingStatus.OFFLINE;
import static org.eclipse.smarthome.core.thing.ThingStatus.ONLINE;
import static org.eclipse.smarthome.core.thing.ThingStatusDetail.CONFIGURATION_ERROR;
import static org.openhab.binding.supla.SuplaBindingConstants.ADDRESS_CHANNEL_ID;
import static org.openhab.binding.supla.SuplaBindingConstants.API_VERSION_CHANNEL_ID;
import static org.openhab.binding.supla.SuplaBindingConstants.CLOUD_VERSION_CHANNEL_ID;
import static org.openhab.binding.supla.SuplaBindingConstants.O_AUTH_TOKEN;
import static org.openhab.binding.supla.internal.cloud.ApiClientFactory.FACTORY;

@SuppressWarnings("PackageAccessibility")
public class CloudBridgeHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(CloudBridgeHandler.class);
    private final ReadWriteMonad<Set<CloudDeviceHandler>> cloudDeviceHandlers = new ReadWriteMonad<>(new HashSet<>());
    private String oAuthToken;
    private String address;
    private String apiVersion;
    private String cloudVersion;
    private ScheduledFuture<?> scheduledFuture;

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
            updateStatus(OFFLINE, CONFIGURATION_ERROR, "Cannot start server! " + ex.getLocalizedMessage());
        }
    }

    private void internalInitialize() throws ApiException {
        // init bridge api client
        final Configuration config = this.getConfig();
        this.oAuthToken = (String) config.get(O_AUTH_TOKEN);
        final ApiClient bridgeApiClient = FACTORY.newApiClient(oAuthToken, logger);

        // get server info
        ServerApi serverApi = new ServerApi(bridgeApiClient);
        ServerInfo serverInfo = serverApi.getServerInfo();
        if (serverInfo == null) {
            updateStatus(OFFLINE, CONFIGURATION_ERROR,
                    "Cannot get server info from `" + bridgeApiClient.getBasePath() + "`!");
            return;
        }
        address = serverInfo.getAddress();
        apiVersion = serverInfo.getApiVersion();
        cloudVersion = serverInfo.getCloudVersion();
        updateState(ADDRESS_CHANNEL_ID, new StringType(address));
        updateState(API_VERSION_CHANNEL_ID, new StringType(apiVersion));
        updateState(CLOUD_VERSION_CHANNEL_ID, new StringType(cloudVersion));

        // check if current api is supported
        String apiVersion = ApiClient.API_VERSION;
        List<String> supportedApiVersions = serverInfo.getSupportedApiVersions();
        if (!supportedApiVersions.contains(apiVersion)) {
            updateStatus(OFFLINE, CONFIGURATION_ERROR, "This API version `" + apiVersion
                                                               + "` is not supported! Supported api versions: [" + String.join(", ", supportedApiVersions) + "].");
            return;
        }

        final ScheduledExecutorService scheduledPool = ThreadPoolManager.getScheduledPool("cloud-bridge-refresh-children");
        final int refreshInterval = ((BigDecimal) config.get("refreshInterval")).intValue();
        this.scheduledFuture = scheduledPool.scheduleAtFixedRate(
                this::refreshCloudDevices,
                refreshInterval * 2,
                refreshInterval,
                SECONDS);

        // done
        updateStatus(ONLINE);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        final String channelId = channelUID.getId();
        if (command instanceof RefreshType) {
            if (ADDRESS_CHANNEL_ID.equals(channelId)) {
                updateState(ADDRESS_CHANNEL_ID, new StringType(address));
            } else if (API_VERSION_CHANNEL_ID.equals(channelId)) {
                updateState(API_VERSION_CHANNEL_ID, new StringType(apiVersion));
            } else if (CLOUD_VERSION_CHANNEL_ID.equals(channelId)) {
                updateState(CLOUD_VERSION_CHANNEL_ID, new StringType(cloudVersion));
            }
        }
    }

    public Optional<String> getOAuthToken() {
        return ofNullable(oAuthToken);
    }

    @Override
    public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
        super.childHandlerInitialized(childHandler, childThing);
        if (childHandler instanceof CloudDeviceHandler) {
            logger.trace("Add `{}` to cloudDeviceHandlers", childHandler.getThing().getUID());
            cloudDeviceHandlers.doInWriteLock(
                    cloudDeviceHandlers -> cloudDeviceHandlers.add((CloudDeviceHandler) childHandler));
        }
    }

    @Override
    public void childHandlerDisposed(ThingHandler childHandler, Thing childThing) {
        super.childHandlerDisposed(childHandler, childThing);
        if (childHandler instanceof CloudDeviceHandler) {
            logger.trace("Remove `{}` to cloudDeviceHandlers", childHandler.getThing().getUID());
            cloudDeviceHandlers.doInWriteLock(cloudDeviceHandlers -> cloudDeviceHandlers.remove(childHandler));
        }
    }

    private void refreshCloudDevices() {
        logger.info("Starting to refresh cloud devices");
        cloudDeviceHandlers.doInReadLock(
                cloudDeviceHandlers -> cloudDeviceHandlers.forEach(CloudDeviceHandler::refresh));
    }
}

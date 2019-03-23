package org.openhab.binding.jsupla.handler;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.BridgeHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.api.ApiClientFactory;
import pl.grzeslowski.jsupla.api.generated.ApiClient;
import pl.grzeslowski.jsupla.api.generated.ApiException;
import pl.grzeslowski.jsupla.api.generated.api.IoDevicesApi;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.valueOf;
import static java.util.Collections.singletonList;
import static org.eclipse.smarthome.core.thing.ThingStatus.OFFLINE;
import static org.eclipse.smarthome.core.thing.ThingStatus.ONLINE;
import static org.eclipse.smarthome.core.thing.ThingStatusDetail.BRIDGE_UNINITIALIZED;
import static org.eclipse.smarthome.core.thing.ThingStatusDetail.COMMUNICATION_ERROR;
import static org.eclipse.smarthome.core.thing.ThingStatusDetail.CONFIGURATION_ERROR;
import static org.openhab.binding.jsupla.JSuplaBindingConstants.SUPLA_DEVICE_CLOUD_ID;
import static org.openhab.binding.jsupla.internal.cloud.CloudChannelFactory.FACTORY;

/**
 * This is handler for all Supla devices.
 * <p>
 * Channels are created at runtime after connecting to Supla Cloud
 *
 * @author Martin Grześlowski - initial contributor
 */
public final class CloudDeviceHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(CloudBridgeHandler.class);
    private CloudBridgeHandler cloudBridgeHandler;
    private ApiClient apiClient;

    public CloudDeviceHandler(final Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        @Nullable final Bridge bridge = getBridge();
        if (bridge == null) {
            logger.debug("No bridge for thing with UID {}", thing.getUID());
            updateStatus(OFFLINE, BRIDGE_UNINITIALIZED,
                    "There is no bridge for this thing. Remove it and add it again.");
            return;
        }
        final @Nullable BridgeHandler bridgeHandler = bridge.getHandler();
        if (!(bridgeHandler instanceof CloudBridgeHandler)) {
            logger.debug("Bridge is not instance of {}! Current bridge class {}, Thing UID {}",
                    CloudBridgeHandler.class.getSimpleName(), bridgeHandler.getClass().getSimpleName(), thing.getUID());
            updateStatus(OFFLINE, BRIDGE_UNINITIALIZED, "There is wrong type of bridge for cloud device!");
            return;
        }
        CloudBridgeHandler handler = (CloudBridgeHandler) bridgeHandler;
        final Optional<String> token = handler.getoAuthToken();
        if (!token.isPresent()) {
            updateStatus(OFFLINE, CONFIGURATION_ERROR, "There is no OAuth token in bridge!");
            return;
        }
        initChannels(token.get());

        // done
        updateStatus(ONLINE);
    }

    private void initChannels(final String token) {
        apiClient = ApiClientFactory.INSTANCE.newApiClient(token);
        final IoDevicesApi ioDevicesApi = new IoDevicesApi(apiClient);
        final String cloudIdString = valueOf(getConfig().get(SUPLA_DEVICE_CLOUD_ID));
        final int cloudId;
        try {
            cloudId = Integer.parseInt(cloudIdString);
        } catch (NumberFormatException e) {
            logger.error("Cannot parse cloud ID `{}` to integer!", cloudIdString, e);
            updateStatus(OFFLINE, CONFIGURATION_ERROR, "Cloud ID is incorrect!");
            return;
        }
        try {
            final List<Channel> channels = ioDevicesApi.getIoDevice(cloudId, singletonList("channels"))
                                                   .getChannels()
                                                   .stream()
                                                   .map(channel -> FACTORY.createChannel(channel, thing.getUID()))
                                                   .filter(Optional::isPresent)
                                                   .map(Optional::get)
                                                   .collect(Collectors.toList());
            updateChannels(channels);
        } catch (ApiException e) {
            logger.error("Error when loading IO device from Supla Cloud!", e);
            updateStatus(OFFLINE, COMMUNICATION_ERROR, "Error when loading IO device from Supla Cloud!");
        }
    }

    private void updateChannels(final List<Channel> channels) {
        ThingBuilder thingBuilder = editThing();
        thingBuilder.withChannels(channels);
        updateThing(thingBuilder.build());
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        logger.debug("Handling command {} in channel {} thing", command, channelUID, thing.getUID());
    }
}

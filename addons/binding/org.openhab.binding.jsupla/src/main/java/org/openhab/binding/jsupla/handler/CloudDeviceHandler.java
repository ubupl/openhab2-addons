package org.openhab.binding.jsupla.handler;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.BridgeHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.api.ApiClientFactory;
import pl.grzeslowski.jsupla.api.generated.ApiClient;
import pl.grzeslowski.jsupla.api.generated.ApiException;
import pl.grzeslowski.jsupla.api.generated.api.ChannelsApi;
import pl.grzeslowski.jsupla.api.generated.api.IoDevicesApi;
import pl.grzeslowski.jsupla.api.generated.model.ChannelFunctionEnumNames;
import pl.grzeslowski.jsupla.api.generated.model.ChannelState;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.eclipse.smarthome.core.library.types.OnOffType.OFF;
import static org.eclipse.smarthome.core.library.types.OnOffType.ON;
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
@SuppressWarnings("PackageAccessibility")
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
            cloudId = parseInt(cloudIdString);
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
        final int channelId = parseInt(channelUID.getId());
        if (command instanceof RefreshType) {
            logger.trace("Refreshing channel {}", channelUID);
            final ChannelsApi channelsApi = new ChannelsApi(apiClient);
            try {
                final pl.grzeslowski.jsupla.api.generated.model.Channel channel =
                        channelsApi.getChannel(channelId, asList("supportedFunctions", "channelState"));
                final ChannelState channelState = channel.getState();
                final ChannelFunctionEnumNames name = channel.getFunction().getName();
                findState(channelState, name).ifPresent(state -> updateState(channelUID, state));
            } catch (ApiException e) {
                logger.error("Error occurred when getting status of channel {}", channelUID, e);
            }
        }
    }

    private Optional<State> findState(ChannelState state, ChannelFunctionEnumNames name) {
        switch (name) {
            case CONTROLLINGTHEGATEWAYLOCK:
            case CONTROLLINGTHEGATE:
            case CONTROLLINGTHEGARAGEDOOR:
            case OPENINGSENSOR_GATEWAY:
            case OPENINGSENSOR_GATE:
            case OPENINGSENSOR_GARAGEDOOR:
            case NOLIQUIDSENSOR:
            case CONTROLLINGTHEDOORLOCK:
            case OPENINGSENSOR_DOOR:
            case CONTROLLINGTHEROLLERSHUTTER:
            case OPENINGSENSOR_ROLLERSHUTTER:
            case POWERSWITCH:
            case LIGHTSWITCH:
            case OPENINGSENSOR_WINDOW:
            case MAILSENSOR:
            case STAIRCASETIMER:
                return of(state.getHi()).map(hi -> hi ? ON : OFF);
            case THERMOMETER:
                return of(new DecimalType(state.getTemperature()));
            case HUMIDITY:
                return of(new DecimalType(state.getHumidity()));
            case HUMIDITYANDTEMPERATURE:
                return of(new StringType(state.getTemperature() + " °C" + state.getHumidity() + "%"));
            case DIMMER:
                return of(new DecimalType(state.getBrightness() / 100.0));
            // case RGBLIGHTING: case DIMMERANDRGBLIGHTING: // TODO support color 
            case DEPTHSENSOR:
                return of(new DecimalType(state.getDepth()));
            case DISTANCESENSOR:
                return of(new DecimalType(state.getDistance()));
            default:
                logger.warn("Does not know how to map {} to OpenHAB state", name);
                return Optional.empty();
        }
    }
}

package org.openhab.binding.supla.handler;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StopMoveType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BridgeHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.supla.internal.cloud.ApiClientFactory;
import org.openhab.binding.supla.internal.cloud.HsbTypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.api.generated.ApiClient;
import pl.grzeslowski.jsupla.api.generated.ApiException;
import pl.grzeslowski.jsupla.api.generated.api.ChannelsApi;
import pl.grzeslowski.jsupla.api.generated.api.IoDevicesApi;
import pl.grzeslowski.jsupla.api.generated.model.ChannelExecuteActionRequest;
import pl.grzeslowski.jsupla.api.generated.model.ChannelFunction;
import pl.grzeslowski.jsupla.api.generated.model.ChannelFunctionActionEnum;
import pl.grzeslowski.jsupla.api.generated.model.ChannelState;
import pl.grzeslowski.jsupla.api.generated.model.Device;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.eclipse.smarthome.core.library.types.OnOffType.OFF;
import static org.eclipse.smarthome.core.library.types.OnOffType.ON;
import static org.eclipse.smarthome.core.library.types.UpDownType.UP;
import static org.eclipse.smarthome.core.thing.ThingStatus.OFFLINE;
import static org.eclipse.smarthome.core.thing.ThingStatus.ONLINE;
import static org.eclipse.smarthome.core.thing.ThingStatusDetail.BRIDGE_UNINITIALIZED;
import static org.eclipse.smarthome.core.thing.ThingStatusDetail.COMMUNICATION_ERROR;
import static org.eclipse.smarthome.core.thing.ThingStatusDetail.CONFIGURATION_ERROR;
import static org.eclipse.smarthome.core.thing.ThingStatusDetail.NONE;
import static org.eclipse.smarthome.core.types.RefreshType.REFRESH;
import static org.openhab.binding.supla.SuplaBindingConstants.SUPLA_DEVICE_CLOUD_ID;
import static org.openhab.binding.supla.internal.cloud.CloudChannelFactory.FACTORY;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelFunctionActionEnum.CLOSE;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelFunctionActionEnum.OPEN;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelFunctionActionEnum.REVEAL;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelFunctionActionEnum.REVEAL_PARTIALLY;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelFunctionActionEnum.SET_RGBW_PARAMETERS;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelFunctionActionEnum.SHUT;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelFunctionActionEnum.TURN_OFF;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelFunctionActionEnum.TURN_ON;

/**
 * This is handler for all Supla devices.
 * <p>
 * Channels are created at runtime after connecting to Supla Cloud
 *
 * @author Martin Grześlowski - initial contributor
 */
@SuppressWarnings("PackageAccessibility")
public final class CloudDeviceHandler extends AbstractDeviceHandler {
    private final Logger logger = LoggerFactory.getLogger(CloudBridgeHandler.class);
    private final Map<ChannelUID, HSBType> ledStates = new HashMap<>();
    private ApiClient apiClient;
    private ChannelsApi channelsApi;
    private int cloudId;
    private IoDevicesApi ioDevicesApi;

    public CloudDeviceHandler(final Thing thing) {
        super(thing);
    }

    @Override
    protected void internalInitialize() throws ApiException {
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
        final Optional<String> token = handler.getOAuthToken();
        if (!token.isPresent()) {
            updateStatus(OFFLINE, CONFIGURATION_ERROR, "There is no OAuth token in bridge!");
            return;
        }
        initApi(token.get());

        if (!initCloudApi()) {
            return;
        }

        if (!checkIfIsOnline()) {
            return;
        }

        if (!checkIfIsEnabled()) {
            return;
        }

        initChannels();

        // done
        updateStatus(ONLINE);
    }

    private boolean initCloudApi() {
        final String cloudIdString = valueOf(getConfig().get(SUPLA_DEVICE_CLOUD_ID));
        try {
            this.cloudId = parseInt(cloudIdString);
            return true;
        } catch (NumberFormatException e) {
            logger.error("Cannot parse cloud ID `{}` to integer!", cloudIdString, e);
            updateStatus(OFFLINE, CONFIGURATION_ERROR, "Cloud ID is incorrect!");
            return false;
        }
    }

    private void initApi(final String token) {
        apiClient = ApiClientFactory.FACTORY.newApiClient(token, logger);
        ioDevicesApi = new IoDevicesApi(apiClient);
        channelsApi = new ChannelsApi(apiClient);
    }

    private boolean checkIfIsOnline() throws ApiException {
        final Device device = ioDevicesApi.getIoDevice(cloudId, singletonList("connected"));
        if (device.isConnected() == null || !device.isConnected()) {
            updateStatus(OFFLINE, NONE, "This device is is not connected to Supla Cloud.");
            return false;
        } else {
            return true;
        }
    }

    private boolean checkIfIsEnabled() throws ApiException {
        final Device device = ioDevicesApi.getIoDevice(cloudId, emptyList());
        if (device.isEnabled() == null || !device.isEnabled()) {
            updateStatus(OFFLINE, NONE, "This device is tuned off in Supla Cloud.");
            return false;
        } else {
            return true;
        }
    }

    private void initChannels() {
        try {
            final List<Channel> channels = ioDevicesApi.getIoDevice(cloudId, singletonList("channels"))
                                                   .getChannels()
                                                   .stream()
                                                   .filter(channel -> !channel.isHidden())
                                                   .map(channel -> FACTORY.createChannels(channel, thing.getUID()))
                                                   .flatMap(List::stream)
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
    protected void handleRefreshCommand(final ChannelUID channelUID) throws Exception {
        final int channelId = parseInt(channelUID.getId());
        logger.trace("Refreshing channel `{}`", channelUID);
        final pl.grzeslowski.jsupla.api.generated.model.Channel channel = queryForChannel(channelId);
        Optional<State> foundState = findState(channel);
        if (foundState.isPresent()) {
            final State state = foundState.get();
            logger.trace("Updating state `{}` to `{}`", channelUID, state);
            updateState(channelUID, state);
            if (state instanceof HSBType) {
                ledStates.put(channelUID, (HSBType) state);
            }
        } else {
            logger.warn("There was no found state for channel `{}` channelState={}, function={}",
                    channelUID, channel.getState(), channel.getFunction());
        }
    }

    @Override
    protected void handleOnOffCommand(final ChannelUID channelUID, final OnOffType command) throws Exception {
        final int channelId = parseInt(channelUID.getId());
        final pl.grzeslowski.jsupla.api.generated.model.Channel channel = queryForChannel(channelId);
        switch (channel.getFunction().getName()) {
            case CONTROLLINGTHEGATE:
            case CONTROLLINGTHEGARAGEDOOR:
                handleOneZeroCommand(channelId, command == ON, OPEN, CLOSE);
                return;
            default:
                handleOneZeroCommand(channelId, command == ON, TURN_ON, TURN_OFF);
        }
    }

    @Override
    protected void handleUpDownCommand(final ChannelUID channelUID, final UpDownType command) throws Exception {
        final int channelId = parseInt(channelUID.getId());
        handleOneZeroCommand(channelId, command == UP, REVEAL, SHUT);
    }

    @Override
    protected void handleHsbCommand(final ChannelUID channelUID, final HSBType command) throws ApiException {
        final int channelId = parseInt(channelUID.getId());
        final pl.grzeslowski.jsupla.api.generated.model.Channel channel = queryForChannel(channelId);
        switch (channel.getFunction().getName()) {
            case RGBLIGHTING:
            case DIMMERANDRGBLIGHTING:
                sendNewLedValue(channelUID, channelId, command);
                return;
            default:
                logger.warn("Not handling `{}` ({}) on channel `{}`", command, command.getClass().getSimpleName(), channelUID);
        }
    }

    @Override
    protected void handleOpenClosedCommand(final ChannelUID channelUID, final OpenClosedType command) throws ApiException {
        final int channelId = parseInt(channelUID.getId());
        handleOneZeroCommand(channelId, command == OpenClosedType.OPEN, OPEN, CLOSE);
    }

    @Override
    protected void handlePercentCommand(final ChannelUID channelUID, final PercentType command) throws ApiException {
        final int channelId = parseInt(channelUID.getId());
        final pl.grzeslowski.jsupla.api.generated.model.Channel channel = queryForChannel(channelId);
        switch (channel.getFunction().getName()) {
            case CONTROLLINGTHEROLLERSHUTTER:
                final int shut = command.intValue();
                logger.debug("Channel `{}` is roller shutter; setting shut={}%", channelUID, shut);
                final ChannelExecuteActionRequest action = new ChannelExecuteActionRequest().action(REVEAL_PARTIALLY).percentage(shut);
                channelsApi.executeAction(action, channelId);
                return;
            case RGBLIGHTING:
            case DIMMERANDRGBLIGHTING:
                if (!ledStates.containsKey(channelUID)) {
                    logger.warn("There is no LED state for channel `{}`!", channelUID);
                    return;
                }
                final HSBType hsbType = requireNonNull(ledStates.get(channelUID));
                final HSBType newHsbType = new HSBType(
                        hsbType.getHue(),
                        hsbType.getSaturation(),
                        command
                );
                sendNewLedValue(channelUID, channelId, newHsbType);
                return;
            default:
                logger.warn("Not handling `{}` ({}) on channel `{}`", command, command.getClass().getSimpleName(), channelUID);
        }
    }

    private void sendNewLedValue(final ChannelUID channelUID, int channelId, final HSBType hsbType) throws ApiException {
        final String rgb = HsbTypeConverter.INSTANCE.convert(hsbType);
        final int brightness = hsbType.getBrightness().intValue();
        final int colorBrightness;
        if (brightness > 0) {
            colorBrightness = hsbType.getSaturation().intValue();
        } else {
            logger.trace("Set colorBrightness to 0% because brightness is 0%");
            colorBrightness = 0;
        }
        logger.trace("Changing RGB to {}, color brightness {}%, brightness {}%", rgb, colorBrightness, brightness);
        final ChannelExecuteActionRequest action = new ChannelExecuteActionRequest()
                                                           .action(SET_RGBW_PARAMETERS)
                                                           .color(rgb)
                                                           .colorBrightness(colorBrightness)
                                                           .brightness(brightness);
        channelsApi.executeAction(action, channelId);
        ledStates.put(channelUID, hsbType);
    }

    @Override
    protected void handleDecimalCommand(final ChannelUID channelUID, final DecimalType command) {
// TODO handle this command
        logger.warn("Not handling `{}` ({}) on channel `{}`", command, command.getClass().getSimpleName(), channelUID);
    }

    private void handleOneZeroCommand(final int channelId,
                                      final boolean firstOrSecond,
                                      final ChannelFunctionActionEnum first,
                                      final ChannelFunctionActionEnum second) throws ApiException {
        final ChannelFunctionActionEnum action = firstOrSecond ? first : second;
        logger.trace("Executing 0/1 command `{}`", action);
        channelsApi.executeAction(new ChannelExecuteActionRequest().action(action), channelId);
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    protected void handleStopMoveTypeCommand(final @NonNull ChannelUID channelUID, final @NonNull StopMoveType command) throws ApiException {
        final int channelId = parseInt(channelUID.getId());
        final pl.grzeslowski.jsupla.api.generated.model.Channel channel = queryForChannel(channelId);
        switch (channel.getFunction().getName()) {
            case CONTROLLINGTHEROLLERSHUTTER:
                handleStopMoveTypeCommandOnRollerShutter(channelUID, channel, command);
                return;
            default:
                logger.warn("Not handling `{}` ({}) on channel `{}`", command, command.getClass().getSimpleName(), channelUID);
        }
    }

    private void handleStopMoveTypeCommandOnRollerShutter(
            final ChannelUID channelUID,
            final pl.grzeslowski.jsupla.api.generated.model.Channel channel,
            final StopMoveType command) throws ApiException {
        switch (command) {
            case MOVE:
                logger.trace("Do not know how to handle command `{}` on roller shutter with id `{}`", command, channelUID);
                return;
            case STOP:
                final ChannelFunctionActionEnum action = ChannelFunctionActionEnum.STOP;
                logger.trace("Sending stop action `{}` to channel with UUID `{}`", action, channelUID);
                channelsApi.executeAction(new ChannelExecuteActionRequest().action(action), channel.getId());
        }
    }

    private Optional<State> findState(pl.grzeslowski.jsupla.api.generated.model.Channel channel) {
        final Optional<ChannelState> state = of(channel.getState());
        final ChannelFunction function = channel.getFunction();
        boolean param2Present = channel.getParam2() != null && channel.getParam2() > 0;

        switch (function.getName()) {
            case OPENINGSENSOR_GATEWAY:
            case OPENINGSENSOR_GATE:
            case OPENINGSENSOR_GARAGEDOOR:
            case NOLIQUIDSENSOR:
            case CONTROLLINGTHEDOORLOCK:
            case OPENINGSENSOR_DOOR:
            case OPENINGSENSOR_ROLLERSHUTTER:
            case OPENINGSENSOR_WINDOW:
            case MAILSENSOR:
            case CONTROLLINGTHEGATEWAYLOCK:
            case CONTROLLINGTHEGATE:
            case CONTROLLINGTHEGARAGEDOOR:
                if (param2Present || !channel.getType().isOutput()) {
                    return state.map(ChannelState::getHi).map(hi -> hi ? ON : OFF);
                } else {
                    return empty();
                }
            case POWERSWITCH:
            case STAIRCASETIMER:
            case LIGHTSWITCH:
                return state.map(ChannelState::getOn).map(on -> on ? ON : OFF);
            case DIMMER:
                return state.map(ChannelState::getBrightness)
                               .map(b -> b / 100.0)
                               .map(DecimalType::new);
            case RGBLIGHTING:
                return state.map(s -> HsbTypeConverter.INSTANCE.toHsbType(s.getColor(), s.getColorBrightness()));
            case DIMMERANDRGBLIGHTING:
                return state.map(s -> HsbTypeConverter.INSTANCE.toHsbType(s.getColor(), s.getColorBrightness(), s.getBrightness()));
            case DEPTHSENSOR:
                return state.map(ChannelState::getDepth).map(DecimalType::new);
            case DISTANCESENSOR:
                return state.map(ChannelState::getDistance).map(DecimalType::new);
            case CONTROLLINGTHEROLLERSHUTTER:
                return state.map(ChannelState::getShut).map(PercentType::new);
            case THERMOMETER:
                return state.map(s -> findTemperature(s, channel)).map(DecimalType::new);
            case HUMIDITY:
                return state.map(s -> findHumidity(s, channel)).map(DecimalType::new);
            case HUMIDITYANDTEMPERATURE:
                return state.map(s -> findTemperature(s, channel) + " °C" + findHumidity(s, channel) + "%").map(StringType::new);
            case NONE:
                return empty();
            default:
                logger.warn("Does not know how to map `{}` to OpenHAB state", channel.getState());
                return empty();
        }
    }

    void refresh() {
        logger.debug("Refreshing `{}`", thing.getUID());
        try {
            if (checkIfIsOnline() && checkIfIsEnabled()) {
                updateStatus(ONLINE);
                logger.trace("Thing `{}` is connected & enabled. Refreshing channels", thing.getUID());
                thing.getChannels()
                        .stream()
                        .map(Channel::getUID)
                        .forEach(channelUID -> handleCommand(channelUID, REFRESH));
            }
        } catch (ApiException e) {
            logger.error("Cannot check if device `{}` is online/enabled", thing.getUID(), e);
        }
    }

    private pl.grzeslowski.jsupla.api.generated.model.Channel queryForChannel(final int channelId) throws ApiException {
        return channelsApi.getChannel(channelId, asList("supportedFunctions", "state"));
    }

    private BigDecimal findTemperature(ChannelState channelState,
                                       pl.grzeslowski.jsupla.api.generated.model.Channel channel) {
        return findValueWithAdjustment(channelState.getTemperature(), channel.getParam2());
    }

    private BigDecimal findHumidity(ChannelState channelState,
                                    pl.grzeslowski.jsupla.api.generated.model.Channel channel) {
        return findValueWithAdjustment(channelState.getHumidity(), channel.getParam3());
    }

    private BigDecimal findValueWithAdjustment(BigDecimal value, Integer adjustment) {
        return Optional.ofNullable(adjustment)
                       .map(v -> v / 100)
                       .map(BigDecimal::new)
                       .orElse(BigDecimal.ZERO)
                       .add(value);
    }
}

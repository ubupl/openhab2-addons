package org.openhab.binding.supla.internal.cloud;

import com.google.common.base.Strings;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.openhab.binding.supla.SuplaBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.api.generated.model.ChannelFunction;

import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.valueOf;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.openhab.binding.supla.SuplaBindingConstants.Channels.CONTACT_CHANNEL_ID;
import static org.openhab.binding.supla.SuplaBindingConstants.Channels.DECIMAL_CHANNEL_ID;
import static org.openhab.binding.supla.SuplaBindingConstants.Channels.DIMMER_CHANNEL_ID;
import static org.openhab.binding.supla.SuplaBindingConstants.Channels.HUMIDITY_CHANNEL_ID;
import static org.openhab.binding.supla.SuplaBindingConstants.Channels.LIGHT_CHANNEL_ID;
import static org.openhab.binding.supla.SuplaBindingConstants.Channels.RGB_CHANNEL_ID;
import static org.openhab.binding.supla.SuplaBindingConstants.Channels.ROLLER_SHUTTER_CHANNEL_ID;
import static org.openhab.binding.supla.SuplaBindingConstants.Channels.SWITCH_CHANNEL_ID;
import static org.openhab.binding.supla.SuplaBindingConstants.Channels.SWITCH_CHANNEL_RO_ID;
import static org.openhab.binding.supla.SuplaBindingConstants.Channels.TEMPERATURE_AND_HUMIDITY_CHANNEL_ID;
import static org.openhab.binding.supla.SuplaBindingConstants.Channels.TEMPERATURE_CHANNEL_ID;

public final class CloudChannelFactory {
    public static final CloudChannelFactory FACTORY = new CloudChannelFactory();
    private final Logger logger = LoggerFactory.getLogger(CloudChannelFactory.class);

    public Optional<Channel> createChannel(pl.grzeslowski.jsupla.api.generated.model.Channel channel, ThingUID thingUID) {
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
            case POWERSWITCH: // has on.off
            case STAIRCASETIMER: // has on.off
                if (channel.getType().isOutput()) {
                    return of(createChannel(channel, thingUID, SWITCH_CHANNEL_ID, "Switch"));
                } else {
                    return of(createChannel(channel, thingUID, SWITCH_CHANNEL_RO_ID, "Switch"));
                }

            case LIGHTSWITCH:
                return of(createChannel(channel, thingUID, LIGHT_CHANNEL_ID, "Switch"));
            case DIMMER:
                return of(createChannel(channel, thingUID, DIMMER_CHANNEL_ID, "Dimmer"));
            case RGBLIGHTING:
            case DIMMERANDRGBLIGHTING:
                return of(createChannel(channel, thingUID, RGB_CHANNEL_ID, "Color"));
            case DEPTHSENSOR:
            case DISTANCESENSOR:
                return of(createChannel(channel, thingUID, DECIMAL_CHANNEL_ID, "Number"));
            case CONTROLLINGTHEROLLERSHUTTER:
                return of(createChannel(channel, thingUID, ROLLER_SHUTTER_CHANNEL_ID, "Rollershutter"));
            case CONTROLLINGTHEGATEWAYLOCK:
            case CONTROLLINGTHEGATE:
            case CONTROLLINGTHEGARAGEDOOR:
                if (param2Present) {
                    if (channel.getType().isOutput()) {
                        return of(createChannel(channel, thingUID, SWITCH_CHANNEL_ID, "Switch"));
                    } else {
                        return of(createChannel(channel, thingUID, SWITCH_CHANNEL_RO_ID, "Switch"));
                    }
                } else {
                    logger.debug("Channel with function `{}` has not param2! {}", function.getName(), channel);
                    return empty();
                }
            case THERMOMETER:
                return of(createChannel(channel, thingUID, TEMPERATURE_CHANNEL_ID, "Number"));
            case HUMIDITY:
                return of(createChannel(channel, thingUID, HUMIDITY_CHANNEL_ID, "Number"));
            case HUMIDITYANDTEMPERATURE:
                return of(createChannel(channel, thingUID, TEMPERATURE_AND_HUMIDITY_CHANNEL_ID, "String"));
            case NONE:
                return empty();
            default:
                logger.warn("Does not know type of this `{}` function", function.getName());
                return empty();
        }
    }

    private Channel createChannel(pl.grzeslowski.jsupla.api.generated.model.Channel channel,
                                  ThingUID thingUID,
                                  String id, final String acceptedItemType) {
        final ChannelUID channelUid = new ChannelUID(thingUID, valueOf(channel.getId()));
        final ChannelTypeUID channelTypeUID = new ChannelTypeUID(SuplaBindingConstants.BINDING_ID, id);

        final ChannelBuilder channelBuilder = ChannelBuilder.create(channelUid, acceptedItemType)
                                                      .withType(channelTypeUID);
        
        if(!isNullOrEmpty(channel.getCaption())) {
            channelBuilder.withLabel(channel.getCaption());
        }
        return channelBuilder.build();
    }
}

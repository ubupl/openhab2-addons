package org.openhab.binding.jsupla.internal.cloud;

import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.openhab.binding.jsupla.JSuplaBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.api.generated.model.ChannelFunction;
import pl.grzeslowski.jsupla.api.generated.model.ChannelType;

import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.valueOf;
import static java.util.Optional.empty;
import static org.openhab.binding.jsupla.JSuplaBindingConstants.Channels.DECIMAL_CHANNEL_ID;
import static org.openhab.binding.jsupla.JSuplaBindingConstants.Channels.LIGHT_CHANNEL_ID;
import static org.openhab.binding.jsupla.JSuplaBindingConstants.Channels.RGB_CHANNEL_ID;
import static org.openhab.binding.jsupla.JSuplaBindingConstants.Channels.SWITCH_CHANNEL_ID;
import static org.openhab.binding.jsupla.JSuplaBindingConstants.Channels.TEMPERATURE_AND_HUMIDITY_CHANNEL_ID;
import static org.openhab.binding.jsupla.JSuplaBindingConstants.Channels.TEMPERATURE_CHANNEL_ID;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelFunctionEnumNames.LIGHTSWITCH;

public final class CloudChannelFactory {
    public static final CloudChannelFactory FACTORY = new CloudChannelFactory();
    private static final Set<String> RELAY_TYPES = newHashSet(
            "SENSORNO",
            "SENSORNC",
            "DISTANCESENSOR",
            "CALLBUTTON",
            "RELAYHFD4",
            "RELAYG5LA1A",
            "RELAY2XG5LA1A",
            "RELAY");
    private static final Set<String> TEMPERATURE_TYPES = newHashSet(
            "THERMOMETERDS18B20",
            "DHT11",
            "DHT21",
            "DHT22",
            "AM2301",
            "AM2302",
            "THERMOMETER"
    );
    private static final Set<String> TEMPERATURE_AND_HUMIDITY_TYPES = newHashSet("HUMIDITYANDTEMPSENSOR");
    private static final Set<String> DECIMAL_TYPES = newHashSet(
            // humidity
            "HUMIDITYSENSOR",
            // others
            "WINDSENSOR",
            "PRESSURESENSOR",
            "RAINSENSOR",
            "WEIGHTSENSOR"
    );
    private static final Set<String> COLOR_CHANNEL_TYPES = newHashSet(
            "DIMMER",
            "RGBLEDCONTROLLER",
            "DIMMERANDRGBLED"
    );
    private final Logger logger = LoggerFactory.getLogger(CloudChannelFactory.class);

    public Optional<Channel> createChannel(pl.grzeslowski.jsupla.api.generated.model.Channel channel, ThingUID thingUID) {
        final ChannelType type = channel.getType();
        final String name = type.getName().getValue();
        if (RELAY_TYPES.contains(name)) {
            final ChannelFunction function = channel.getFunction();
            if (function != null && LIGHTSWITCH.equals(function.getName())) {
                return Optional.of(createChannel(channel, thingUID, LIGHT_CHANNEL_ID));
            } else {
                return Optional.of(createChannel(channel, thingUID, SWITCH_CHANNEL_ID));
            }
        }
        if (TEMPERATURE_TYPES.contains(name)) {
            return Optional.of(createChannel(channel, thingUID, TEMPERATURE_CHANNEL_ID));
        }
        if (TEMPERATURE_AND_HUMIDITY_TYPES.contains(name)) {
            return Optional.of(createChannel(channel, thingUID, TEMPERATURE_AND_HUMIDITY_CHANNEL_ID));
        }
        if (DECIMAL_TYPES.contains(name)) {
            return Optional.of(createChannel(channel, thingUID, DECIMAL_CHANNEL_ID));
        }
        if (COLOR_CHANNEL_TYPES.contains(name)) {
            return Optional.of(createChannel(channel, thingUID, RGB_CHANNEL_ID));
        }
        logger.warn("Channel of type {} is not supported!", type);
        return empty();
    }

    private Channel createChannel(pl.grzeslowski.jsupla.api.generated.model.Channel channel,
                                  ThingUID thingUID,
                                  String id) {
        final ChannelUID channelUid = new ChannelUID(thingUID, valueOf(channel.getId()));
        final ChannelTypeUID channelTypeUID = new ChannelTypeUID(JSuplaBindingConstants.BINDING_ID, id);

        return ChannelBuilder.create(channelUid, null) // TODO should it be null?
                       .withType(channelTypeUID)
                       .build();
    }
}

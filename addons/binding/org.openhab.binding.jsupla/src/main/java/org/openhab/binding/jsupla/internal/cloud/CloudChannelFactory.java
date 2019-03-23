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
import static pl.grzeslowski.jsupla.api.generated.model.ChannelType.NameEnum.AM2301;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelType.NameEnum.AM2302;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelType.NameEnum.CALLBUTTON;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelType.NameEnum.DHT11;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelType.NameEnum.DHT21;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelType.NameEnum.DHT22;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelType.NameEnum.DIMMER;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelType.NameEnum.DIMMERANDRGBLED;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelType.NameEnum.DISTANCESENSOR;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelType.NameEnum.HUMIDITYANDTEMPSENSOR;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelType.NameEnum.HUMIDITYSENSOR;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelType.NameEnum.PRESSURESENSOR;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelType.NameEnum.RAINSENSOR;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelType.NameEnum.RELAY;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelType.NameEnum.RELAY2XG5LA1A;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelType.NameEnum.RELAYG5LA1A;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelType.NameEnum.RELAYHFD4;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelType.NameEnum.RGBLEDCONTROLLER;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelType.NameEnum.SENSORNC;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelType.NameEnum.SENSORNO;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelType.NameEnum.THERMOMETER;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelType.NameEnum.THERMOMETERDS18B20;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelType.NameEnum.WEIGHTSENSOR;
import static pl.grzeslowski.jsupla.api.generated.model.ChannelType.NameEnum.WINDSENSOR;

@SuppressWarnings("PackageAccessibility")
public final class CloudChannelFactory {
    public static final CloudChannelFactory FACTORY = new CloudChannelFactory();
    private static final Set<ChannelType.NameEnum> RELAY_TYPES = newHashSet(
            SENSORNO,
            SENSORNC,
            DISTANCESENSOR,
            CALLBUTTON,
            RELAYHFD4,
            RELAYG5LA1A,
            RELAY2XG5LA1A,
            RELAY);
    private static final Set<ChannelType.NameEnum> TEMPERATURE_TYPES = newHashSet(
            THERMOMETERDS18B20,
            DHT11,
            DHT21,
            DHT22,
            AM2301,
            AM2302,
            THERMOMETER
    );
    private static final Set<ChannelType.NameEnum> TEMPERATURE_AND_HUMIDITY_TYPES = newHashSet(HUMIDITYANDTEMPSENSOR);
    private static final Set<ChannelType.NameEnum> DECIMAL_TYPES = newHashSet(
            // humidity
            HUMIDITYSENSOR,
            // others
            WINDSENSOR,
            PRESSURESENSOR,
            RAINSENSOR,
            DIMMER,
            WEIGHTSENSOR
    );
    private static final Set<ChannelType.NameEnum> COLOR_CHANNEL_TYPES = newHashSet(
            RGBLEDCONTROLLER,
            DIMMERANDRGBLED
    );
    private final Logger logger = LoggerFactory.getLogger(CloudChannelFactory.class);

    public Optional<Channel> createChannel(pl.grzeslowski.jsupla.api.generated.model.Channel channel, ThingUID thingUID) {
        final ChannelType type = channel.getType();
        final ChannelType.NameEnum name = type.getName();
        if (RELAY_TYPES.contains(name)) {
            final ChannelFunction function = channel.getFunction();
            if (function != null && LIGHTSWITCH.equals(function.getName())) {
                return Optional.of(createChannel(channel, thingUID, LIGHT_CHANNEL_ID, "Switch"));
            } else {
                return Optional.of(createChannel(channel, thingUID, SWITCH_CHANNEL_ID, "Switch"));
            }
        }
        if (TEMPERATURE_TYPES.contains(name)) {
            return Optional.of(createChannel(channel, thingUID, TEMPERATURE_CHANNEL_ID, "Number"));
        }
        if (TEMPERATURE_AND_HUMIDITY_TYPES.contains(name)) {
            return Optional.of(createChannel(channel, thingUID, TEMPERATURE_AND_HUMIDITY_CHANNEL_ID, "String"));
        }
        if (DECIMAL_TYPES.contains(name)) {
            return Optional.of(createChannel(channel, thingUID, DECIMAL_CHANNEL_ID, "Number"));
        }
        if (COLOR_CHANNEL_TYPES.contains(name)) {
            return Optional.of(createChannel(channel, thingUID, RGB_CHANNEL_ID, "Color"));
        }
        logger.warn("Channel of type {} is not supported!", type);
        return empty();
    }

    private Channel createChannel(pl.grzeslowski.jsupla.api.generated.model.Channel channel,
                                  ThingUID thingUID,
                                  String id, final String acceptedItemType) {
        final ChannelUID channelUid = new ChannelUID(thingUID, valueOf(channel.getId()));
        final ChannelTypeUID channelTypeUID = new ChannelTypeUID(JSuplaBindingConstants.BINDING_ID, id);

        return ChannelBuilder.create(channelUid, acceptedItemType)
                       .withType(channelTypeUID)
                       .build();
    }
}

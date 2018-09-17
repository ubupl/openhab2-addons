package org.openhab.binding.jsupla.internal;

import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.openhab.binding.jsupla.jSuplaBindingConstants;
import pl.grzeslowski.jsupla.protocoljava.api.channels.values.ChannelValueSwitch;
import pl.grzeslowski.jsupla.protocoljava.api.channels.values.DecimalValue;
import pl.grzeslowski.jsupla.protocoljava.api.channels.values.OnOff;
import pl.grzeslowski.jsupla.protocoljava.api.channels.values.OpenClose;
import pl.grzeslowski.jsupla.protocoljava.api.channels.values.PercentValue;
import pl.grzeslowski.jsupla.protocoljava.api.channels.values.RgbValue;
import pl.grzeslowski.jsupla.protocoljava.api.channels.values.StoppableOpenClose;
import pl.grzeslowski.jsupla.protocoljava.api.channels.values.TemperatureAndHumidityValue;
import pl.grzeslowski.jsupla.protocoljava.api.channels.values.TemperatureValue;
import pl.grzeslowski.jsupla.protocoljava.api.channels.values.UnknownValue;

import static java.util.Objects.requireNonNull;
import static org.openhab.binding.jsupla.jSuplaBindingConstants.Channels.DECIMAL_CHANNEL_ID;
import static org.openhab.binding.jsupla.jSuplaBindingConstants.Channels.RGB_CHANNEL_ID;
import static org.openhab.binding.jsupla.jSuplaBindingConstants.Channels.ROLLER_SHUTTER_CHANNEL_ID;
import static org.openhab.binding.jsupla.jSuplaBindingConstants.Channels.SWITCH_CHANNEL_ID;
import static org.openhab.binding.jsupla.jSuplaBindingConstants.Channels.TEMPERATURE_AND_HUMIDITY_CHANNEL_ID;
import static org.openhab.binding.jsupla.jSuplaBindingConstants.Channels.TEMPERATURE_CHANNEL_ID;
import static org.openhab.binding.jsupla.jSuplaBindingConstants.Channels.UNKNOWN_CHANNEL_ID;

public class ChannelCallback implements ChannelValueSwitch.Callback<org.eclipse.smarthome.core.thing.Channel> {
    private final ThingUID thingUID;

    public ChannelCallback(final ThingUID thingUID) {
        this.thingUID = requireNonNull(thingUID);
    }

    private ChannelUID createChannelUid(String id) {
        return new ChannelUID(thingUID, id);
    }

    private ChannelTypeUID createChannelTypeUID(String id) {
        return new ChannelTypeUID(jSuplaBindingConstants.BINDING_ID, id);
    }

    @Override
    public Channel onDecimalValue(final DecimalValue decimalValue) {
        final ChannelUID channelUid = createChannelUid(DECIMAL_CHANNEL_ID);
        final ChannelTypeUID channelTypeUID = createChannelTypeUID(DECIMAL_CHANNEL_ID);

        return ChannelBuilder.create(channelUid, null) // TODO should it be null?
                       .withType(channelTypeUID)
                       .build();
    }

    @Override
    public Channel onOnOff(final OnOff onOff) {
        return switchChannel();
    }

    @Override
    public Channel onOpenClose(final OpenClose openClose) {
        return switchChannel();
    }

    private Channel switchChannel() {
        final ChannelUID channelUid = createChannelUid(SWITCH_CHANNEL_ID);
        final ChannelTypeUID channelTypeUID = createChannelTypeUID(SWITCH_CHANNEL_ID);

        return ChannelBuilder.create(channelUid, "Switch")
                       .withType(channelTypeUID)
                       .build();
    }

    @Override
    public Channel onPercentValue(final PercentValue percentValue) {
        return null;
    }

    @Override
    public Channel onRgbValue(final RgbValue rgbValue) {
        final ChannelUID channelUid = createChannelUid(RGB_CHANNEL_ID);
        final ChannelTypeUID channelTypeUID = createChannelTypeUID(RGB_CHANNEL_ID);

        return ChannelBuilder.create(channelUid, null) // TODO should it be null?
                       .withType(channelTypeUID)
                       .build();
    }

    @Override
    public Channel onStoppableOpenClose(final StoppableOpenClose stoppableOpenClose) {
        final ChannelUID channelUid = createChannelUid(ROLLER_SHUTTER_CHANNEL_ID);
        final ChannelTypeUID channelTypeUID = createChannelTypeUID(ROLLER_SHUTTER_CHANNEL_ID);

        return ChannelBuilder.create(channelUid, null) // TODO should it be null?
                       .withType(channelTypeUID)
                       .build();
    }

    @Override
    public Channel onTemperatureValue(final TemperatureValue temperatureValue) {
        final ChannelUID channelUid = createChannelUid(TEMPERATURE_CHANNEL_ID);
        final ChannelTypeUID channelTypeUID = createChannelTypeUID(TEMPERATURE_CHANNEL_ID);

        return ChannelBuilder.create(channelUid, null) // TODO should it be null?
                       .withType(channelTypeUID)
                       .build();
    }

    @Override
    public Channel onTemperatureAndHumidityValue(final TemperatureAndHumidityValue temperatureAndHumidityValue) {
        final ChannelUID channelUid = createChannelUid(TEMPERATURE_AND_HUMIDITY_CHANNEL_ID);
        final ChannelTypeUID channelTypeUID = createChannelTypeUID(TEMPERATURE_AND_HUMIDITY_CHANNEL_ID);

        return ChannelBuilder.create(channelUid, null) // TODO should it be null?
                       .withType(channelTypeUID)
                       .build();
    }

    @Override
    public Channel onUnknownValue(final UnknownValue unknownValue) {
        final ChannelUID channelUid = createChannelUid(UNKNOWN_CHANNEL_ID);
        final ChannelTypeUID channelTypeUID = createChannelTypeUID(UNKNOWN_CHANNEL_ID);

        return ChannelBuilder.create(channelUid, null) // TODO should it be null?
                       .withType(channelTypeUID)
                       .build();
    }

}

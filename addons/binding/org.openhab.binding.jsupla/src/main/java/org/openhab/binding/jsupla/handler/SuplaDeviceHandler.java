/**
 * Copyright (c) 2014,2018 by the respective copyright holders.
 * <p>
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 * <p>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.jsupla.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.jsupla.internal.ChannelCallback;
import org.openhab.binding.jsupla.internal.JSuplaConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.protocoljava.api.channels.values.ChannelValueSwitch;
import pl.grzeslowski.jsupla.protocoljava.api.entities.ds.DeviceChannel;
import pl.grzeslowski.jsupla.protocoljava.api.entities.ds.DeviceChannels;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The {@link SuplaDeviceHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Grzeslowski - Initial contribution
 */
@NonNullByDefault
public class SuplaDeviceHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(SuplaDeviceHandler.class);

    @Nullable
    private JSuplaConfiguration config;

    public SuplaDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // TODO
    }

    @Override
    public void initialize() {
        config = getConfigAs(JSuplaConfiguration.class);

        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        updateStatus(ThingStatus.ONLINE);

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work
        // as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
    }

    @SuppressWarnings("deprecation")
    public void setChannels(final DeviceChannels deviceChannels) {
        logger.debug("Registering channels {}", deviceChannels);
        final List<Channel> channels = deviceChannels.getChannels()
                                               .stream()
                                               .sorted(Comparator.comparingInt(DeviceChannel::getNumber))
                                               .map(this::createChannel)
                                               .collect(Collectors.toList());
        updateChannels(channels);
    }

    @SuppressWarnings("deprecation")
    private Channel createChannel(final DeviceChannel deviceChannel) {
        final ChannelValueSwitch<Channel> channelValueSwitch = new ChannelValueSwitch<>(new ChannelCallback(getThing().getUID()));
        return channelValueSwitch.doSwitch(deviceChannel.getValue());
    }

//    public void setChannels(final DeviceChannelsB channels) {
//
//    }

    private void updateChannels(final List<Channel> channels) {
        ThingBuilder thingBuilder = editThing();
        thingBuilder.withChannels(channels);
        updateThing(thingBuilder.build());
    }
}

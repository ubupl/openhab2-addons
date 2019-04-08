/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.spotify.internal.handler;

import static org.openhab.binding.spotify.internal.SpotifyBindingConstants.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.PlayPauseType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.spotify.internal.api.SpotifyApi;
import org.openhab.binding.spotify.internal.api.exception.SpotifyException;
import org.openhab.binding.spotify.internal.api.model.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SpotifyDeviceHandler} is responsible for handling commands, which are sent to one of the channels.
 *
 * @author Andreas Stenlund - Initial contribution
 * @author Hilbrand Bouwkamp - Code cleanup, moved channel state to this class, generic stability.
 */
@NonNullByDefault
public class SpotifyDeviceHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(SpotifyDeviceHandler.class);
    private @NonNullByDefault({}) SpotifyHandleCommands commandHandler;
    private @NonNullByDefault({}) SpotifyApi spotifyApi;
    private @NonNullByDefault({}) String deviceId;

    private boolean active;

    /**
     * Constructor.
     *
     * @param thing Thing representing this device.
     */
    public SpotifyDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        try {
            if (commandHandler != null) {
                commandHandler.handleCommand(channelUID, command, active, deviceId);
            }
        } catch (SpotifyException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, e.getMessage());
        }
    }

    @Override
    public void initialize() {
        final SpotifyBridgeHandler bridgeHandler = (SpotifyBridgeHandler) getBridge().getHandler();
        spotifyApi = bridgeHandler.getSpotifyApi();

        final Configuration config = thing.getConfiguration();
        deviceId = (String) config.get(PROPERTY_SPOTIFY_DEVICE_ID);
        commandHandler = new SpotifyHandleCommands(spotifyApi);
        updateStatus(ThingStatus.UNKNOWN);
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        if (bridgeStatusInfo.getStatus() != ThingStatus.ONLINE) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE, "Spotify Bridge Offline");
            logger.debug("SpotifyDevice {}: SpotifyBridge is not online.", getThing().getThingTypeUID(),
                    bridgeStatusInfo.getStatus());
        }
    }

    /**
     * Updates the status if the given device matches with this handler.
     *
     * @param device device with status information
     * @param playing true if the current active device is playing
     * @return returns true if given device matches with this handler
     */
    public boolean updateDeviceStatus(Device device, boolean playing) {
        if (deviceId.equals(device.getId())) {
            logger.debug("Updating status of Thing: {} Device [ {} {}, {} ]", thing.getUID(), device.getId(),
                    device.getName(), device.getType());
            boolean online = setOnlineStatus(device.isRestricted());
            updateChannelState(CHANNEL_DEVICENAME, new StringType(device.getName()));
            updateChannelState(CHANNEL_DEVICETYPE, new StringType(device.getType()));
            updateChannelState(CHANNEL_DEVICEVOLUME,
                    device.getVolumePercent() == null ? UnDefType.UNDEF : new PercentType(device.getVolumePercent()));
            active = device.isActive();
            updateChannelState(CHANNEL_DEVICEACTIVE, OnOffType.from(active));
            updateChannelState(CHANNEL_DEVICEPLAYER,
                    online && active && playing ? PlayPauseType.PLAY : PlayPauseType.PAUSE);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Updates the device as showing status is gone and reset all device status to default.
     */
    public void setStatusGone() {
        logger.debug("Device is gone: {}", thing.getUID());
        getThing().setStatusInfo(
                new ThingStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.GONE, "Device not available on Spotify"));
        updateChannelState(CHANNEL_DEVICERESTRICTED, OnOffType.ON);
        updateChannelState(CHANNEL_DEVICEACTIVE, OnOffType.OFF);
        updateChannelState(CHANNEL_DEVICEPLAYER, PlayPauseType.PAUSE);
    }

    /**
     * Sets the device online status. If the device is restricted it will be set offline.
     *
     * @param restricted true if device is restricted (no access)
     * @return true if device is online
     */
    private boolean setOnlineStatus(boolean restricted) {
        updateChannelState(CHANNEL_DEVICERESTRICTED, OnOffType.from(restricted));
        if (restricted) {
            // Only change status if device is currently online
            if (thing.getStatus() == ThingStatus.ONLINE) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE,
                        "Restricted. No Web API commands will be accepted by this device.");
            }
            return false;
        } else if (thing.getStatus() != ThingStatus.ONLINE) {
            updateStatus(ThingStatus.ONLINE);
        }
        return true;
    }

    /**
     * Convenience method to update the channel state but only if the channel is linked.
     *
     * @param channelId id of the channel to update
     * @param state State to set on the channel
     */
    private void updateChannelState(String channelId, State state) {
        final Channel channel = thing.getChannel(channelId);

        if (channel != null && isLinked(channel.getUID())) {
            updateState(channel.getUID(), state);
        }
    }
}

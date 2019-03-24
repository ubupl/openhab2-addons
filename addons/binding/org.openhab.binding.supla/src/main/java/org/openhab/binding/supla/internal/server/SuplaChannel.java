/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.supla.internal.server;

import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.openhab.binding.supla.handler.SuplaCloudBridgeHandler;
import org.openhab.binding.supla.handler.SuplaDeviceHandler;
import org.openhab.binding.supla.internal.SuplaDeviceRegistry;
import org.openhab.binding.supla.internal.discovery.SuplaDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.protocoljava.api.entities.dcs.PingServer;
import pl.grzeslowski.jsupla.protocoljava.api.entities.dcs.SetActivityTimeout;
import pl.grzeslowski.jsupla.protocoljava.api.entities.ds.DeviceChannelValue;
import pl.grzeslowski.jsupla.protocoljava.api.entities.ds.DeviceChannels;
import pl.grzeslowski.jsupla.protocoljava.api.entities.ds.DeviceChannelsB;
import pl.grzeslowski.jsupla.protocoljava.api.entities.ds.RegisterDevice;
import pl.grzeslowski.jsupla.protocoljava.api.entities.ds.RegisterDeviceC;
import pl.grzeslowski.jsupla.protocoljava.api.entities.ds.RegisterDeviceD;
import pl.grzeslowski.jsupla.protocoljava.api.entities.sd.RegisterDeviceResult;
import pl.grzeslowski.jsupla.protocoljava.api.entities.sdc.PingServerResultClient;
import pl.grzeslowski.jsupla.protocoljava.api.entities.sdc.SetActivityTimeoutResult;
import pl.grzeslowski.jsupla.protocoljava.api.types.ToServerEntity;
import pl.grzeslowski.jsupla.server.api.Channel;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.time.Instant.now;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.eclipse.smarthome.core.thing.ThingStatus.OFFLINE;
import static org.openhab.binding.supla.SuplaBindingConstants.DEVICE_TIMEOUT_SEC;
import static pl.grzeslowski.jsupla.protocol.api.ResultCode.SUPLA_RESULTCODE_TRUE;
import static reactor.core.publisher.Flux.just;

/**
 * @author Grzeslowski - Initial contribution
 */
public final class SuplaChannel {
    private final SuplaDeviceRegistry suplaDeviceRegistry;
    private Logger logger = LoggerFactory.getLogger(SuplaChannel.class);
    private final SuplaCloudBridgeHandler suplaCloudBridgeHandler;

    // Location Authorization
    private final int serverAccessId;
    private final char[] serverAccessIdPassword;

    // Email authorization
    private final String email;
    private final String authKey;

    private final SuplaDiscoveryService suplaDiscoveryService;
    private final Channel channel;
    private final ScheduledExecutorService scheduledPool;
    private boolean authorized;
    private String guid;
    private AtomicReference<ScheduledFuture<?>> pingSchedule = new AtomicReference<>();
    private AtomicLong lastMessageFromDevice = new AtomicLong();
    private SuplaDeviceHandler suplaDeviceHandler;

    public SuplaChannel(final SuplaCloudBridgeHandler suplaCloudBridgeHandler,
                        final int serverAccessId,
                        final char[] serverAccessIdPassword,
                        final SuplaDiscoveryService suplaDiscoveryService,
                        final Channel channel,
                        final ScheduledExecutorService scheduledPool,
                        final SuplaDeviceRegistry suplaDeviceRegistry,
                        final String email,
                        final String authKey) {
        this.suplaCloudBridgeHandler = requireNonNull(suplaCloudBridgeHandler);
        this.serverAccessId = serverAccessId;
        this.serverAccessIdPassword = serverAccessIdPassword;
        this.suplaDiscoveryService = requireNonNull(suplaDiscoveryService);
        this.channel = channel;
        this.scheduledPool = requireNonNull(scheduledPool);
        this.suplaDeviceRegistry = requireNonNull(suplaDeviceRegistry);
        this.email = requireNonNull(email);
        this.authKey = requireNonNull(authKey);
    }

    @SuppressWarnings("deprecation")
    public synchronized void onNext(final ToServerEntity entity) {
        logger.trace("{} -> {}", guid, entity);
        lastMessageFromDevice.set(now().getEpochSecond());
        if (!authorized) {
            final Runnable auth;
            final DeviceChannels channels;
            final String name;
            if (entity instanceof RegisterDevice) {
                final RegisterDevice registerDevice = (RegisterDevice) entity;
                auth = () -> authorizeForLocation(registerDevice.getGuid(), registerDevice.getLocationId(), registerDevice.getLocationPassword());
                this.guid = registerDevice.getGuid();
                channels = registerDevice.getChannels();
                if (registerDevice instanceof RegisterDeviceC) {
                    final RegisterDeviceC registerDeviceC = (RegisterDeviceC) registerDevice;
                    final String serverName = registerDeviceC.getServerName();
                    if (isNullOrEmpty(serverName)) {
                        name = registerDeviceC.getName();
                    } else {
                        name = registerDeviceC.getName() + " " + serverName;
                    }
                } else {
                    name = registerDevice.getName();
                }
            } else if (entity instanceof RegisterDeviceD) {
                RegisterDeviceD registerDevice = (RegisterDeviceD) entity;
                auth = () -> authorizeForEmail(registerDevice.getGuid(), registerDevice.getEmail(), registerDevice.getAuthKey());
                this.guid = registerDevice.getGuid();
                channels = new DeviceChannelsB(registerDevice.getChannels());
                name = registerDevice.getName();
            } else {
                logger.debug("Device in channel is not authorized in but is also not sending RegisterClient entity! {}",
                        entity);
                auth = null;
                channels = null;
                name = null;
            }
            if (auth != null) {
                authorize(auth, channels, name);
            }
        } else if (entity instanceof SetActivityTimeout) {
            setActivityTimeout();
        } else if (entity instanceof PingServer) {
            pingServer((PingServer) entity);
        } else if (entity instanceof DeviceChannelValue) {
            deviceChannelValue((DeviceChannelValue) entity);
        } else {
            logger.debug("Do not handling this command: {}", entity);
        }
    }

    private void authorize(Runnable authorize, final DeviceChannels channels, final String name) {
        logger = LoggerFactory.getLogger(this.getClass().getName() + "." + guid);
        authorize.run();
        if (authorized) {
            suplaDiscoveryService.addSuplaDevice(guid, name);
            sendRegistrationConfirmation();
            bindToThingHandler(channels);
        } else {
            logger.debug("Authorization failed for GUID {}", guid);
        }
    }

    private void authorizeForLocation(final String guid, final int accessId, final char[] accessIdPassword) {
        if (serverAccessId != accessId) {
            logger.debug("Wrong accessId for GUID {}; {} != {}", guid, accessId, serverAccessId);
            authorized = false;
            return;
        }
        if (!isGoodPassword(accessIdPassword)) {
            logger.debug("Wrong accessIdPassword for GUID {}", guid);
            authorized = false;
            return;
        }
        logger.debug("Authorizing GUID {}", guid);
        authorized = true;
    }

    private void authorizeForEmail(final String guid, final String email, final String authKey) {
        if (!this.email.equals(email)) {
            logger.debug("Wrong email for GUID {}; {} != {}", guid, email, this.email);
            authorized = false;
            return;
        }
        if (!this.authKey.equals(authKey)) {
            logger.debug("Wrong auth key for GUID {}; {} != {}", guid, authKey, this.authKey);
            authorized = false;
            return;
        }
        logger.debug("Authorizing GUID {}", guid);
        authorized = true;
    }

    public void onError(final Throwable ex) {
        if (suplaDeviceHandler != null) {
            logger.error("Error occurred in device. ", ex);
            suplaDeviceHandler.updateStatus(OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Error occurred in channel pipe. " + ex.getLocalizedMessage());
        }
    }

    public void onComplete() {
        logger.debug("onComplete() {}", toString());
        this.suplaCloudBridgeHandler.completedChannel();
        if (suplaDeviceHandler != null) {
            suplaDeviceHandler.updateStatus(OFFLINE, ThingStatusDetail.NONE,
                    "Device went offline");
        }
    }

    private void setActivityTimeout() {
        final SetActivityTimeoutResult data = new SetActivityTimeoutResult(
                DEVICE_TIMEOUT_SEC,
                DEVICE_TIMEOUT_SEC - 2,
                DEVICE_TIMEOUT_SEC + 2);
        channel.write(Flux.just(data))
                .subscribe(date -> logger.trace("setActivityTimeout {} {}", data, date.format(ISO_DATE_TIME)));
        final ScheduledFuture<?> pingSchedule = scheduledPool.scheduleWithFixedDelay(
                this::checkIfDeviceIsUp,
                DEVICE_TIMEOUT_SEC * 2,
                DEVICE_TIMEOUT_SEC,
                TimeUnit.SECONDS);
        this.pingSchedule.set(pingSchedule);
    }

    private void checkIfDeviceIsUp() {
        final long now = now().getEpochSecond();
        if (now - lastMessageFromDevice.get() > DEVICE_TIMEOUT_SEC) {
            logger.debug("Device {} is dead. Need to kill it!", guid);
            channel.close();
            this.pingSchedule.get().cancel(false);
            suplaDeviceHandler.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE,
                    "Device do not response on pings.");
        }
    }

    private void pingServer(final PingServer entity) {
        final PingServerResultClient response = new PingServerResultClient(entity.getTimeval());
        channel.write(just(response))
                .subscribe(date -> logger.trace("pingServer {}s {}ms {}",
                        response.getTimeval().getSeconds(),
                        response.getTimeval().getSeconds(),
                        date.format(ISO_DATE_TIME)));
    }

    private void sendRegistrationConfirmation() {
        final RegisterDeviceResult result = new RegisterDeviceResult(SUPLA_RESULTCODE_TRUE.getValue(), 100, 5, 1);
        channel.write(just(result))
                .subscribe(date -> logger.trace("Send register response at {}", date.format(ISO_DATE_TIME)));
    }

    private boolean isGoodPassword(final char[] accessIdPassword) {
        if (serverAccessIdPassword.length > accessIdPassword.length) {
            return false;
        }
        for (int i = 0; i < serverAccessIdPassword.length; i++) {
            if (serverAccessIdPassword[i] != accessIdPassword[i]) {
                return false;
            }
        }
        return true;
    }

    private void bindToThingHandler(final DeviceChannels channels) {
        final Optional<SuplaDeviceHandler> suplaDevice = suplaDeviceRegistry.getSuplaDevice(guid);
        if (suplaDevice.isPresent()) {
            suplaDeviceHandler = suplaDevice.get();
            suplaDeviceHandler.setChannels(channels);
            suplaDeviceHandler.setSuplaChannel(channel);
        } else {
            logger.debug("Thing not found. Binding of channels will happen later...");
            scheduledPool.schedule(
                    () -> bindToThingHandler(channels),
                    DEVICE_TIMEOUT_SEC,
                    SECONDS);
        }
    }

    private void deviceChannelValue(final DeviceChannelValue entity) {
        suplaDeviceHandler.updateStatus(entity.getChannelNumber(), entity.getValue());
    }

    @Override
    public String toString() {
        return "SuplaChannel{" +
                       "authorized=" + authorized +
                       ", guid='" + guid + '\'' +
                       '}';
    }
}

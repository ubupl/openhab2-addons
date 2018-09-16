package org.openhab.binding.jsupla.internal.server;

import org.openhab.binding.jsupla.internal.discovery.JSuplaDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.protocoljava.api.entities.Timeval;
import pl.grzeslowski.jsupla.protocoljava.api.entities.dcs.SetActivityTimeout;
import pl.grzeslowski.jsupla.protocoljava.api.entities.ds.RegisterDevice;
import pl.grzeslowski.jsupla.protocoljava.api.entities.ds.RegisterDeviceC;
import pl.grzeslowski.jsupla.protocoljava.api.entities.sd.RegisterDeviceResult;
import pl.grzeslowski.jsupla.protocoljava.api.entities.sdc.PingServerResultClient;
import pl.grzeslowski.jsupla.protocoljava.api.entities.sdc.SetActivityTimeoutResult;
import pl.grzeslowski.jsupla.protocoljava.api.types.ToServerEntity;
import pl.grzeslowski.jsupla.server.api.Channel;
import reactor.core.publisher.Flux;

import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.util.Objects.requireNonNull;
import static org.openhab.binding.jsupla.jSuplaBindingConstants.DEVICE_TIMEOUT_MS;
import static pl.grzeslowski.jsupla.protocol.api.ResultCode.SUPLA_RESULTCODE_TRUE;
import static reactor.core.publisher.Flux.just;

public final class JSuplaChannel implements Consumer<ToServerEntity> {
    private final long createdTime;
    private Logger logger = LoggerFactory.getLogger(JSuplaChannel.class);
    private final int serverAccessId;
    private final char[] serverAccessIdPassword;
    private final JSuplaDiscoveryService jSuplaDiscoveryService;
    private final Channel channel;
    private final ScheduledExecutorService scheduledPool;
    private boolean authorized;
    private String guid;
    private AtomicReference<ScheduledFuture<?>> pingSchedule = new AtomicReference<>();

    public JSuplaChannel(final int serverAccessId,
                         final char[] serverAccessIdPassword,
                         final JSuplaDiscoveryService jSuplaDiscoveryService,
                         final Channel channel,
                         final ScheduledExecutorService scheduledPool) {
        this.createdTime = new Date().getTime();
        this.serverAccessId = serverAccessId;
        this.serverAccessIdPassword = serverAccessIdPassword;
        this.jSuplaDiscoveryService = requireNonNull(jSuplaDiscoveryService);
        this.channel = channel;
        this.scheduledPool = requireNonNull(scheduledPool);
    }

    @Override
    public synchronized void accept(final ToServerEntity entity) {
        logger.trace("{} -> {}", guid, entity);
        if (!authorized) {
            if (entity instanceof RegisterDevice) {
                final RegisterDevice registerDevice = (RegisterDevice) entity;
                guid = registerDevice.getGuid();
                logger = LoggerFactory.getLogger(this.getClass().getName() + "." + guid);
                authorize(guid, registerDevice.getLocationId(), registerDevice.getLocationPassword());
                if (authorized) {
                    sendDeviceToDiscoveryInbox(registerDevice);
                    sendRegistrationConfirmation();
                } else {
                    logger.debug("Authorization failed for GUID {}", guid);
                }
            } else {
                logger.debug("Device in channel is not authorized in but is also not sending RegisterClient entity! {}",
                        entity);
            }
        } else if (entity instanceof SetActivityTimeout) {
            setActivityTimeout();
        } else {
            logger.debug("Do not handling this command: {}", entity);
        }
    }

    private void setActivityTimeout() {
        final SetActivityTimeoutResult data = new SetActivityTimeoutResult(
                DEVICE_TIMEOUT_MS,
                DEVICE_TIMEOUT_MS - 20,
                DEVICE_TIMEOUT_MS + 20);
        channel.write(Flux.just(data))
                .subscribe(date -> logger.trace("setActivityTimeout {}", date.format(ISO_DATE_TIME)));
        final ScheduledFuture<?> pingSchedule = scheduledPool.scheduleAtFixedRate(
                this::sendPing,
                DEVICE_TIMEOUT_MS,
                DEVICE_TIMEOUT_MS,
                TimeUnit.MILLISECONDS);
        this.pingSchedule.set(pingSchedule);
    }

    private void sendPing() {
        final int seconds = (int) (new Date().getTime() - createdTime);
        final PingServerResultClient response = new PingServerResultClient(new Timeval(seconds, 0));
        channel.write(just(response))
                .subscribe(date -> logger.trace("sendPing {}s {}", seconds, date.format(ISO_DATE_TIME)));
    }

    private void sendRegistrationConfirmation() {
        final RegisterDeviceResult result = new RegisterDeviceResult(SUPLA_RESULTCODE_TRUE.getValue(), 100, 5, 1);
        channel.write(just(result))
                .subscribe(date -> logger.trace("Send register response at {}", date.format(ISO_DATE_TIME)));
    }

    private void authorize(final String guid, final int accessId, final char[] accessIdPassword) {
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

    private void sendDeviceToDiscoveryInbox(final RegisterDevice registerClient) {
        final String name;
        if (registerClient instanceof RegisterDeviceC) {
            final RegisterDeviceC registerDeviceC = (RegisterDeviceC) registerClient;
            final String serverName = registerDeviceC.getServerName();
            if (isNullOrEmpty(serverName)) {
                name = registerDeviceC.getName();
            } else {
                name = registerDeviceC.getName() + " " + serverName;
            }
        } else {
            name = registerClient.getName();
        }
        jSuplaDiscoveryService.addSuplaDevice(registerClient.getGuid(), name);
    }

    @Override
    public String toString() {
        return "JSuplaChannel{" +
                       "authorized=" + authorized +
                       ", guid='" + guid + '\'' +
                       '}';
    }
}

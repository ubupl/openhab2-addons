package org.openhab.binding.jsupla.internal.server;

import org.openhab.binding.jsupla.internal.JSuplaConfiguration;
import org.openhab.binding.jsupla.internal.discovery.JSuplaDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.protocoljava.api.entities.cs.RegisterClient;
import pl.grzeslowski.jsupla.protocoljava.api.entities.cs.RegisterClientB;
import pl.grzeslowski.jsupla.protocoljava.api.types.ToServerEntity;

import java.util.Arrays;
import java.util.function.Consumer;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;

public final class JSuplaChannel implements Consumer<ToServerEntity> {
    private final Logger logger = LoggerFactory.getLogger(JSuplaChannel.class);
    private final JSuplaConfiguration configuration;
    private final JSuplaDiscoveryService jSuplaDiscoveryService;
    private boolean authorized;
    private String guid;

    public JSuplaChannel(JSuplaConfiguration configuration, JSuplaDiscoveryService jSuplaDiscoveryService) {
        this.configuration = requireNonNull(configuration);
        this.jSuplaDiscoveryService = requireNonNull(jSuplaDiscoveryService);
    }

    @Override
    public synchronized void accept(final ToServerEntity entity) {
        if (!authorized) {
            if (entity instanceof RegisterClient) {
                final RegisterClient registerClient = (RegisterClient) entity;
                authorize(registerClient.getGuid(), registerClient.getAccessId(), registerClient.getAccessIdPassword());
                if (authorized) {
                    guid = registerClient.getGuid();
                    sendDeviceToDiscoveryInbox(registerClient);
                }
            } else {
                logger.debug("Device in channel is not authorized in but is also not sending RegisterClient entity! {}",
                        entity);
            }
            return;
        }
    }

    private void authorize(final String guid, final int accessId, final char[] accessIdPassword) {
        final int serverAccessId = configuration.accessId;
        if (serverAccessId != accessId) {
            logger.debug("Wrong accessId for GUID [{}]; {} != {}", guid, accessId, serverAccessId);
            authorized = false;
        }
        final char[] serverAccessIdPassword = configuration.accessIdPassword.toCharArray();
        if (!Arrays.equals(serverAccessIdPassword, accessIdPassword)) {
            logger.debug("Wrong accessIdPassword for GUID [{}]", guid);
            authorized = false;
        }
        logger.debug("Authorizing GUID [{}]", guid);
        authorized = true;
    }

    private void sendDeviceToDiscoveryInbox(final RegisterClient registerClient) {
        final String name;
        if (registerClient instanceof RegisterClientB) {
            final RegisterClientB clientB = (RegisterClientB) registerClient;
            final String serverName = clientB.getServerName();
            if (isNullOrEmpty(serverName)) {
                name = clientB.getName();
            } else {
                name = clientB.getName() + " " + serverName;
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

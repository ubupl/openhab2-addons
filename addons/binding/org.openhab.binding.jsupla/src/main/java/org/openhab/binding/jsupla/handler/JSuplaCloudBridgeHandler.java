package org.openhab.binding.jsupla.handler;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.jsupla.internal.discovery.JSuplaDiscoveryService;
import org.openhab.binding.jsupla.internal.server.JSuplaChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.protocol.impl.calltypes.CallTypeParserImpl;
import pl.grzeslowski.jsupla.protocol.impl.decoders.DecoderFactoryImpl;
import pl.grzeslowski.jsupla.protocol.impl.decoders.PrimitiveDecoderImpl;
import pl.grzeslowski.jsupla.protocoljava.api.parsers.Parser;
import pl.grzeslowski.jsupla.server.api.Channel;
import pl.grzeslowski.jsupla.server.api.Server;
import pl.grzeslowski.jsupla.server.api.ServerFactory;
import pl.grzeslowski.jsupla.server.api.ServerProperties;
import pl.grzeslowski.jsupla.server.netty.api.NettyServerFactory;

import javax.net.ssl.SSLException;
import java.math.BigDecimal;
import java.security.cert.CertificateException;
import java.util.Arrays;

import static org.eclipse.smarthome.core.thing.ThingStatus.OFFLINE;
import static org.eclipse.smarthome.core.thing.ThingStatus.ONLINE;
import static org.eclipse.smarthome.core.thing.ThingStatusDetail.CONFIGURATION_ERROR;
import static org.openhab.binding.jsupla.jSuplaBindingConstants.CONFIG_PORT;
import static org.openhab.binding.jsupla.jSuplaBindingConstants.CONFIG_SERVER_ACCESS_ID;
import static org.openhab.binding.jsupla.jSuplaBindingConstants.CONFIG_SERVER_ACCESS_ID_PASSWORD;
import static pl.grzeslowski.jsupla.protocoljava.api.ProtocolJavaContext.PROTOCOL_JAVA_CONTEXT;
import static pl.grzeslowski.jsupla.server.api.ServerProperties.fromList;
import static pl.grzeslowski.jsupla.server.netty.api.NettyServerFactory.PORT;
import static pl.grzeslowski.jsupla.server.netty.api.NettyServerFactory.SSL_CTX;

public class JSuplaCloudBridgeHandler extends BaseBridgeHandler {
    private static final Logger logger = LoggerFactory.getLogger(JSuplaCloudBridgeHandler.class);
    private Server server;
    private JSuplaDiscoveryService jSuplaDiscoveryService;

    private int port;
    private int serverAccessId;
    private char[] serverAccessIdPassword;

    public JSuplaCloudBridgeHandler(final Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        final ServerFactory factory = buildServerFactory();
        try {
            final Configuration config = this.getConfig();
            serverAccessId = ((BigDecimal) config.get(CONFIG_SERVER_ACCESS_ID)).intValue();
            serverAccessIdPassword = ((String) config.get(CONFIG_SERVER_ACCESS_ID_PASSWORD)).toCharArray();
            port = ((BigDecimal) config.get(CONFIG_PORT)).intValue();
            server = factory.createNewServer(buildServerProperties(port));
            server.getNewChannelsPipe().subscribe(channel -> newChannel(channel, serverAccessId, serverAccessIdPassword), this::errorOccurredInChannel);

            logger.debug("jSuplaServer running on port {}", port);
            updateStatus(ONLINE);
        } catch (CertificateException | SSLException ex) {
            logger.error("Cannot start server!", ex);
            updateStatus(OFFLINE, CONFIGURATION_ERROR,
                    "Cannot start server! " + ex.getLocalizedMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private ServerFactory buildServerFactory() {
        return new NettyServerFactory(
                new CallTypeParserImpl(),
                new DecoderFactoryImpl(new PrimitiveDecoderImpl()),
                PROTOCOL_JAVA_CONTEXT.getService(Parser.class)
        );
    }

    private ServerProperties buildServerProperties(int port)
            throws CertificateException, SSLException {
        return fromList(Arrays.asList(PORT, port, SSL_CTX, buildSslContext()));
    }

    private SslContext buildSslContext() throws CertificateException, SSLException {
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
    }

    private void newChannel(final Channel channel, int serverAccessId, char[] serverAccessIdPassword) {
        logger.debug("New channel {}", channel);
        channel.getMessagePipe().subscribe(
                new JSuplaChannel(serverAccessId, serverAccessIdPassword, jSuplaDiscoveryService),
                ex -> errorOccurredInChannel(channel, ex));
    }

    private void errorOccurredInChannel(Throwable ex) {
        logger.error("Error occurred in server pipe", ex);
        updateStatus(OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                "Error occurred in server pipe. Message: " + ex.getLocalizedMessage());
    }

    // TODO remove this to JSuplaChannel
    private void errorOccurredInChannel(Channel channel, Throwable ex) {
        logger.error("Error occurred in channel {}", channel, ex);
        updateStatus(OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                "Error occurred in channel pipe. " + ex.getLocalizedMessage());
    }

    @Override
    public void dispose() {
        super.dispose();
        try {
            server.close();
        } catch (Exception ex) {
            logger.error("Could not close server!", ex);
            updateStatus(OFFLINE, ThingStatusDetail.NONE,
                    "Could not close server! It's possible that restart of your RPi is required. " + ex.getLocalizedMessage());
        }
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        // no commands in this bridge
    }

    public void setJSuplaDiscoveryService(final JSuplaDiscoveryService jSuplaDiscoveryService) {
        logger.trace("setJSuplaDiscoveryService#{}", jSuplaDiscoveryService.hashCode());
        this.jSuplaDiscoveryService = jSuplaDiscoveryService;
    }
}

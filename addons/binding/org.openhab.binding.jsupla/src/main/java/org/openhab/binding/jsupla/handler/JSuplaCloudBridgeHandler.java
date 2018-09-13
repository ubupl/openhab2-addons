package org.openhab.binding.jsupla.handler;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.jsupla.internal.JSuplaConfiguration;
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
import java.security.cert.CertificateException;
import java.util.Arrays;

import static org.eclipse.smarthome.core.thing.ThingStatus.OFFLINE;
import static org.eclipse.smarthome.core.thing.ThingStatus.ONLINE;
import static org.eclipse.smarthome.core.thing.ThingStatusDetail.CONFIGURATION_ERROR;
import static pl.grzeslowski.jsupla.protocoljava.api.ProtocolJavaContext.PROTOCOL_JAVA_CONTEXT;
import static pl.grzeslowski.jsupla.server.api.ServerProperties.fromList;
import static pl.grzeslowski.jsupla.server.netty.api.NettyServerFactory.PORT;
import static pl.grzeslowski.jsupla.server.netty.api.NettyServerFactory.SSL_CTX;

public class JSuplaCloudBridgeHandler extends BaseBridgeHandler {
    private static final Logger logger = LoggerFactory.getLogger(JSuplaCloudBridgeHandler.class);
    private Server server;
    private JSuplaDiscoveryService jSuplaDiscoveryService;

    public JSuplaCloudBridgeHandler(final Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        final JSuplaConfiguration configuration = getConfigAs(JSuplaConfiguration.class);
        final ServerFactory factory = buildServerFactory();
        try {
            server = factory.createNewServer(buildServerProperties(configuration));
            server.getNewChannelsPipe().subscribe(channel -> newChannel(channel, configuration), this::errorOccurredInChannel);

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

    private ServerProperties buildServerProperties(JSuplaConfiguration jSuplaConfiguration)
            throws CertificateException, SSLException {
        return fromList(Arrays.asList(PORT, jSuplaConfiguration.port, SSL_CTX, buildSslContext()));
    }

    private SslContext buildSslContext() throws CertificateException, SSLException {
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
    }

    private void newChannel(final Channel channel, JSuplaConfiguration configuration) {
        logger.debug("New channel {}", channel);
        channel.getMessagePipe().subscribe(new JSuplaChannel(configuration, jSuplaDiscoveryService), ex -> errorOccurredInChannel(channel, ex));
    }

    private void errorOccurredInChannel(Throwable ex) {
        logger.error("Error occurred in server pipe", ex);
        updateStatus(OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                "Error occurred in server pipe. Message: " + ex.getLocalizedMessage());
    }

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

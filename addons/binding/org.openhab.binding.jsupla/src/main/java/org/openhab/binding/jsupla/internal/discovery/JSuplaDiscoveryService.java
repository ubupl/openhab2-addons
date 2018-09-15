package org.openhab.binding.jsupla.internal.discovery;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.jsupla.handler.JSuplaCloudBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;
import static org.openhab.binding.jsupla.jSuplaBindingConstants.SUPLA_DEVICE_TYPE;
import static org.openhab.binding.jsupla.jSuplaBindingConstants.SUPPORTED_THING_TYPES_UIDS;

public class JSuplaDiscoveryService extends AbstractDiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(JSuplaDiscoveryService.class);
    private final JSuplaCloudBridgeHandler jSuplaCloudBridgeHandler;

    public JSuplaDiscoveryService(final JSuplaCloudBridgeHandler jSuplaCloudBridgeHandler) {
        super(SUPPORTED_THING_TYPES_UIDS, 10, true);
        this.jSuplaCloudBridgeHandler = requireNonNull(jSuplaCloudBridgeHandler);
    }

    @Override
    protected void startScan() {
        logger.trace("Active scan started, but there is no active scan for jSupla ;)");
        stopScan();
    }

    public void addSuplaDevice(String guid, String name) {
        final ThingUID bridgeUID = jSuplaCloudBridgeHandler.getThing().getUID();

        ThingUID thingUID = new ThingUID(SUPLA_DEVICE_TYPE, bridgeUID, String.valueOf(guid));
        final DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
                                                        .withBridge(bridgeUID)
//                                                      .withProperties(properties) // TODO
                                                        .withLabel(String.format("%s (%s)", name, guid))
                                                        .build();
        logger.debug("Discovered thing with GUID [{}]", guid);
        thingDiscovered(discoveryResult);
    }
}

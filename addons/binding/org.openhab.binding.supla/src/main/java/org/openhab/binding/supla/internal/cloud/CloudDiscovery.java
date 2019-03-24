package org.openhab.binding.supla.internal.cloud;

import com.google.common.collect.ImmutableMap;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.supla.handler.CloudBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.api.ApiClientFactory;
import pl.grzeslowski.jsupla.api.generated.ApiClient;
import pl.grzeslowski.jsupla.api.generated.ApiException;
import pl.grzeslowski.jsupla.api.generated.api.IoDevicesApi;
import pl.grzeslowski.jsupla.api.generated.model.Device;

import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.openhab.binding.supla.SuplaBindingConstants.SUPLA_DEVICE_CLOUD_ID;
import static org.openhab.binding.supla.SuplaBindingConstants.SUPLA_DEVICE_GUID;
import static org.openhab.binding.supla.SuplaBindingConstants.SUPLA_DEVICE_TYPE;
import static org.openhab.binding.supla.SuplaBindingConstants.SUPPORTED_THING_TYPES_UIDS;

/**
 * @author Martin Grzeslowski - Initial contribution
 */
public final class CloudDiscovery extends AbstractDiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(CloudDiscovery.class);
    private final CloudBridgeHandler bridgeHandler;

    public CloudDiscovery(final CloudBridgeHandler bridgeHandler) {
        super(SUPPORTED_THING_TYPES_UIDS, 10, true);
        this.bridgeHandler = requireNonNull(bridgeHandler);
    }

    @Override
    protected void startScan() {
        logger.debug("Starting Supla Cloud discovery scan");
        final Optional<String> token = bridgeHandler.getOAuthToken();
        if (!token.isPresent()) {
            logger.warn("There is no OAuth token for bridge {}! Discovery was cancel.",
                    bridgeHandler.getThing().getUID());
            return;
        }
        final ApiClient apiClient = ApiClientFactory.INSTANCE.newApiClient(token.get());
        final IoDevicesApi api = new IoDevicesApi(apiClient);
        try {
            api.getIoDevices(singletonList("channels")).forEach(this::addThing);
        } catch (ApiException e) {
            logger.error("Cannot get IO devices from Supla Cloud `{}`!", apiClient.getBasePath(), e);
        }
        logger.debug("Finished Supla Cloud scan");
    }

    private void addThing(Device device) {
        final ThingUID thingUID = new ThingUID(SUPLA_DEVICE_TYPE, findBridgeUID(), device.getGUIDString());
        final DiscoveryResult discoveryResult = createDiscoveryResult(thingUID, buildThingLabel(device), buildThingProperties(device));
        thingDiscovered(discoveryResult);
    }

    private ThingUID findBridgeUID() {
        return bridgeHandler.getThing().getUID();
    }

    private DiscoveryResult createDiscoveryResult(ThingUID thingUID, String label, Map<String, Object> properties) {
        return DiscoveryResultBuilder.create(thingUID)
                       .withBridge(findBridgeUID())
                       .withProperties(properties)
                       .withLabel(label)
                       .build();
    }

    private String buildThingLabel(Device device) {
        final StringBuilder sb = new StringBuilder();

        final String name = device.getName();
        if (!isNullOrEmpty(name)) {
            sb.append(name);

            // comment cannot appear without name
            final String comment = device.getComment();
            if (!isNullOrEmpty(comment)) {
                sb.append("(").append(comment).append(")");
            }
        }

        final String primaryLabel = sb.toString();
        if (!isNullOrEmpty(primaryLabel)) {
            return primaryLabel;
        } else {
            return device.getGUIDString();
        }
    }

    private Map<String, Object> buildThingProperties(Device device) {
        return ImmutableMap.<String, Object>builder()
                       .put(SUPLA_DEVICE_GUID, device.getGUIDString())
                       .put(SUPLA_DEVICE_CLOUD_ID, device.getId())
                       .build();
    }
}

/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.supla.internal;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.openhab.binding.supla.handler.CloudBridgeHandler;
import org.openhab.binding.supla.handler.CloudDeviceHandler;
import org.openhab.binding.supla.handler.SuplaCloudBridgeHandler;
import org.openhab.binding.supla.handler.SuplaDeviceHandler;
import org.openhab.binding.supla.internal.cloud.CloudDiscovery;
import org.openhab.binding.supla.internal.discovery.SuplaDiscoveryService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;

import static java.util.Objects.requireNonNull;
import static org.openhab.binding.supla.SuplaBindingConstants.SUPLA_CLOUD_SERVER_TYPE;
import static org.openhab.binding.supla.SuplaBindingConstants.SUPLA_DEVICE_TYPE;
import static org.openhab.binding.supla.SuplaBindingConstants.SUPLA_SERVER_TYPE;
import static org.openhab.binding.supla.SuplaBindingConstants.SUPPORTED_THING_TYPES_UIDS;

/**
 * The {@link SuplaHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Grzeslowski - Initial contribution
 */
@Component(service = ThingHandlerFactory.class, immediate = true, configurationPid = "binding.supla")
@NonNullByDefault
public class SuplaHandlerFactory extends BaseThingHandlerFactory {
    private final Logger logger = LoggerFactory.getLogger(SuplaHandlerFactory.class);
    private @Nullable SuplaDeviceRegistry suplaDeviceRegistry;

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        // it's done cause tycho-compile raises possible null error
        final @Nullable SuplaDeviceRegistry suplaDeviceRegistryNonNull = suplaDeviceRegistry;
        if (SUPLA_DEVICE_TYPE.equals(thingTypeUID)) {
            final ThingUID bridgeUID = requireNonNull(thing.getBridgeUID(), "No bridge for " + thing);
            final ThingTypeUID bridgeTypeUID = bridgeUID.getThingTypeUID();
            if (SUPLA_SERVER_TYPE.equals(bridgeTypeUID)) {
                return newSuplaDeviceHandler(thing, suplaDeviceRegistryNonNull);
            } else if (SUPLA_CLOUD_SERVER_TYPE.equals(bridgeTypeUID)) {
                return newCloudDevice(thing);
            }
        } else if (SUPLA_SERVER_TYPE.equals(thingTypeUID)) {
            return newSuplaCloudBridgeHandler((Bridge) thing);
        } else if (SUPLA_CLOUD_SERVER_TYPE.equals(thingTypeUID)) {
            return newSuplaCloudServerThingHandler(thing);
        }

        return null;
    }

    @NonNull
    private ThingHandler newSuplaDeviceHandler(final Thing thing, final @Nullable SuplaDeviceRegistry suplaDeviceRegistryNonNull) {
        final SuplaDeviceHandler suplaDeviceHandler = new SuplaDeviceHandler(thing);
        if (suplaDeviceRegistryNonNull != null) {
            suplaDeviceRegistryNonNull.addSuplaDevice(suplaDeviceHandler);
        } else {
            throw new IllegalStateException("suplaDeviceRegistry is null!");
        }
        return suplaDeviceHandler;
    }

    @NonNull
    private ThingHandler newSuplaCloudBridgeHandler(final Bridge thing) {
        SuplaCloudBridgeHandler bridgeHandler = new SuplaCloudBridgeHandler(thing, suplaDeviceRegistry);
        final SuplaDiscoveryService discovery = new SuplaDiscoveryService(bridgeHandler);
        registerThingDiscovery(discovery);
        bridgeHandler.setSuplaDiscoveryService(discovery);
        return bridgeHandler;
    }

    private ThingHandler newSuplaCloudServerThingHandler(final Thing thing) {
        final CloudBridgeHandler bridgeHandler = new CloudBridgeHandler((Bridge) thing);
        final CloudDiscovery cloudDiscovery = new CloudDiscovery(bridgeHandler);
        registerThingDiscovery(cloudDiscovery);
        return bridgeHandler;
    }

    @NonNull
    private ThingHandler newCloudDevice(final Thing thing) {
        return new CloudDeviceHandler(thing);
    }

    @Reference
    @SuppressWarnings("unused") // used by OSGi
    public void setSuplaDeviceRegistry(final SuplaDeviceRegistry suplaDeviceRegistry) {
        this.suplaDeviceRegistry = suplaDeviceRegistry;
    }

    @SuppressWarnings("unused") // used by OSGi
    public void unsetSuplaDeviceRegistry(final SuplaDeviceRegistry suplaDeviceRegistry) {
        this.suplaDeviceRegistry = null;
    }

    private synchronized void registerThingDiscovery(DiscoveryService discoveryService) {
        logger.trace("Try to register Discovery service on BundleID: {} Service: {}",
                bundleContext.getBundle().getBundleId(), DiscoveryService.class.getName());
        bundleContext.registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<>());
    }
}

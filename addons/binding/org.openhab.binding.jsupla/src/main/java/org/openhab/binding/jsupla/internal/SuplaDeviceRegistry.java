package org.openhab.binding.jsupla.internal;

import org.openhab.binding.jsupla.handler.SuplaDeviceHandler;

import java.util.Optional;

public interface SuplaDeviceRegistry {
    void addSuplaDevice(SuplaDeviceHandler suplaDeviceHandler);

    Optional<SuplaDeviceHandler> getSuplaDevice(String guid);
}



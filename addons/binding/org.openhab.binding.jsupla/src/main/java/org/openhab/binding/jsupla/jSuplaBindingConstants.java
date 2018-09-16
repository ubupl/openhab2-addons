/**
 * Copyright (c) 2014,2018 by the respective copyright holders.
 * <p>
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 * <p>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.jsupla;

import com.google.common.collect.ImmutableSet;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

import java.util.Set;

/**
 * The {@link jSuplaBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Grzeslowski - Initial contribution
 */
@SuppressWarnings("WeakerAccess")
@NonNullByDefault
public class jSuplaBindingConstants {

    private static final String BINDING_ID = "jsupla";

    // List of all Thing Type UIDs
    public static final ThingTypeUID SUPLA_DEVICE_TYPE = new ThingTypeUID(BINDING_ID, "device");
    public static final ThingTypeUID JSUPLA_SERVER_TYPE = new ThingTypeUID(BINDING_ID, "server-bridge");

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = ImmutableSet.of(SUPLA_DEVICE_TYPE, JSUPLA_SERVER_TYPE);

    // List of all Channel ids
    public static final String LIGHT_CHANNEL = "light-channel";
    public static final String SWITCH_CHANNEL = "switch-channel";

    // jSuplaServer constants
    public static final int DEVICE_TIMEOUT_MS = 100;
    public static final int DEFAULT_PORT = 2016;
    public static final String CONFIG_SERVER_ACCESS_ID = "serverAccessId";
    public static final String CONFIG_SERVER_ACCESS_ID_PASSWORD = "serverAccessIdPassword";
    public static final String CONFIG_PORT = "port";
    public static final String CONNECTED_DEVICES_CHANNEL_ID = "server-devices";
}
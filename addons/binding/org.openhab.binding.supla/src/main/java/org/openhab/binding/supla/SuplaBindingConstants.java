/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.supla;

import com.google.common.collect.ImmutableSet;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

import java.util.Set;

/**
 * The {@link SuplaBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Martin Grzeslowski - Initial contribution
 */
public class SuplaBindingConstants {

    private static final String BINDING_ID = "supla";

    // IDs
    public static final String SUPLA_ZAMEL_ROW_01_THING_ID = "zamel-row-1";

    // List of all Thing Type UIDs
    public static final ThingTypeUID BRIDGE_THING_TYPE = new ThingTypeUID(BINDING_ID, "suplaCloudBridge");
    public static final ThingTypeUID SUPLA_ZAMEL_ROW_01_THING_TYPE = new ThingTypeUID(BINDING_ID, SUPLA_ZAMEL_ROW_01_THING_ID);

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = ImmutableSet.of(BRIDGE_THING_TYPE, SUPLA_ZAMEL_ROW_01_THING_TYPE);

    // List of all Channel ids
    public static final String SWITCH_CHANNEL = "switch-1";

    // Supla Consts
    // Channel
    public static final String SUPLA_IO_DEVICE_ID = "supla-io-device-id";
    public static final String RELAY_CHANNEL_TYPE = "TYPE_RELAY";
}

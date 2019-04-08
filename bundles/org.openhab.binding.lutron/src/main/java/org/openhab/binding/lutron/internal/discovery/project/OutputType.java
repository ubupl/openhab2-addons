/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.lutron.internal.discovery.project;

/**
 * Type of output device in a Lutron system.
 *
 * @author Allan Tong - Initial contribution
 * @author Bob Adair - Added additional output types
 */
public enum OutputType {
    AUTO_DETECT,
    CCO_MAINTAINED,
    CCO_PULSED,
    CEILING_FAN_TYPE,
    ECO_SYSTEM_FLUORESCENT,
    FLUORESCENT_DB,
    INC,
    MLV,
    NON_DIM,
    NON_DIM_ELV,
    NON_DIM_INC,
    SHEER_BLIND,
    SYSTEM_SHADE,
    VENETIAN_BLIND,
    ZERO_TO_TEN,
}

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
package org.openhab.binding.rfxcom.internal.messages;

import org.openhab.binding.rfxcom.internal.exceptions.RFXComUnsupportedValueException;

/**
 * An Utility class to handle {@link ByteEnumWrapper} instances
 *
 * @author Martin van Wingerden - Simplify some code in the RFXCOM binding
 */
public class ByteEnumUtil {
    private ByteEnumUtil() {
        // deliberately empty
    }

    public static <T extends ByteEnumWrapper> T fromByte(Class<T> typeClass, int input)
            throws RFXComUnsupportedValueException {
        for (T enumValue : typeClass.getEnumConstants()) {
            if (enumValue.toByte() == input) {
                return enumValue;
            }
        }

        throw new RFXComUnsupportedValueException(typeClass, input);
    }

    public static <T extends ByteEnumWrapper> T convertSubType(Class<T> typeClass, String subType)
            throws RFXComUnsupportedValueException {
        for (T enumValue : typeClass.getEnumConstants()) {
            if (enumValue.toString().equals(subType)) {
                return enumValue;
            }
        }

        try {
            int byteValue = Integer.parseInt(subType);
            return fromByte(typeClass, byteValue);
        } catch (NumberFormatException e) {
            throw new RFXComUnsupportedValueException(typeClass, subType);
        }
    }
}

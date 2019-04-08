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
package org.openhab.binding.icloud.internal;

import org.openhab.binding.icloud.internal.json.response.ICloudAccountDataResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Extracts iCloud device information from a given JSON string
 *
 * @author Patrik Gfeller - Initial Contribution
 *
 */
public class ICloudDeviceInformationParser {
    private final Gson gson = new GsonBuilder().create();

    public ICloudAccountDataResponse parse(String json) {
        return gson.fromJson(json, ICloudAccountDataResponse.class);
    }
}

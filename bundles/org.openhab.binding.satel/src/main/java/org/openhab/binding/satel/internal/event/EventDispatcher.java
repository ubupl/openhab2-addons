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
package org.openhab.binding.satel.internal.event;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows distributing incoming event to all registered listeners. Listeners
 * must implement {@link SatelEventListener} interface.
 *
 * @author Krzysztof Goworek - Initial contribution
 */
public class EventDispatcher {

    private final Logger logger = LoggerFactory.getLogger(EventDispatcher.class);

    private final Set<SatelEventListener> eventListeners = new CopyOnWriteArraySet<>();

    /**
     * Add a listener for Satel events.
     *
     * @param eventListener
     *            the event listener to add.
     */
    public void addEventListener(SatelEventListener eventListener) {
        this.eventListeners.add(eventListener);
    }

    /**
     * Remove a listener for Satel events.
     *
     * @param eventListener
     *            the event listener to remove.
     */
    public void removeEventListener(SatelEventListener eventListener) {
        this.eventListeners.remove(eventListener);
    }

    /**
     * Dispatch incoming event to all listeners.
     *
     * @param event
     *            the event to distribute.
     */
    public void dispatchEvent(SatelEvent event) {
        logger.debug("Distributing event: {}", event);
        for (SatelEventListener listener : eventListeners) {
            logger.trace("Distributing to {}", listener);
            listener.incomingEvent(event);
        }
    }
}

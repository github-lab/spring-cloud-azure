/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.integration.core;

import java.util.function.Consumer;

/**
 * Operations for subscribing to a destination.
 *
 * @author Warren Zhu
 */
public interface SubscribingOperation<D> {

    /**
     * Register a message consumer to a given destination.
     *
     * @return {@code true} if the consumer was subscribed or {@code false} if it
     * was already subscribed.
     */
    boolean subscribe(String destination, Consumer<Iterable<D>> consumer);

    /**
     * Un-register a message consumer.
     *
     * @return {@code true} if the consumer was un-registered, or {@code false}
     * if was not registered.
     */
    void unsubscribe(String destination, Consumer<Iterable<D>> consumer);

    /**
     * Get checkpointer for a given destination
     */
    Checkpointer<D> getCheckpointer(String destination);
}

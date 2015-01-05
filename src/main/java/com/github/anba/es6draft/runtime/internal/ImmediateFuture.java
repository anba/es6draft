/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * {@link Future} with an immediate result
 */
public class ImmediateFuture<V> implements Future<V> {
    private final V result;

    public ImmediateFuture(V result) {
        this.result = result;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public V get() {
        return result;
    }

    @Override
    public V get(long timeout, TimeUnit unit) {
        return result;
    }
}

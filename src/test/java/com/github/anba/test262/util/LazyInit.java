/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.test262.util;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;

/**
 * Initializes an object lazily
 * 
 */
public abstract class LazyInit<T> extends LazyInitializer<T> {
    @Override
    public T get() {
        try {
            return super.get();
        } catch (ConcurrentException e) {
            throw new RuntimeException(e);
        }
    }
}

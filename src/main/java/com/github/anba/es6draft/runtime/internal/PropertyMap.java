/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.LinkedHashMap;

/**
 *
 */
@SuppressWarnings("serial")
public final class PropertyMap<KEY, VALUE> extends LinkedHashMap<KEY, VALUE> {
    public PropertyMap(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public PropertyMap<KEY, VALUE> clone() {
        return (PropertyMap<KEY, VALUE>) super.clone();
    }
}

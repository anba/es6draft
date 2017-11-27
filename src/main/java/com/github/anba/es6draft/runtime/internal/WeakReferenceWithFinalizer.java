/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * 
 */
public final class WeakReferenceWithFinalizer extends WeakReference<ScriptObject> {
    private final Ref<Runnable> finalizer;

    public WeakReferenceWithFinalizer(ScriptObject referent, Runnable finalizer, ReferenceQueue<ScriptObject> queue) {
        super(referent, queue);
        this.finalizer = new Ref<>(finalizer);
    }

    @Override
    public void clear() {
        super.clear();
        this.finalizer.clear();
    }

    public Ref<Runnable> getFinalizer() {
        return this.finalizer;
    }
}
